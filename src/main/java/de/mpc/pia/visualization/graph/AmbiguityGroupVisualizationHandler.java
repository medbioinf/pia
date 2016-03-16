package de.mpc.pia.visualization.graph;

import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.Set;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Group;
import de.mpc.pia.modeller.protein.ReportProtein;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.algorithms.layout.util.Relaxer;
import edu.uci.ics.jung.algorithms.layout.util.VisRunner;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.GraphMouseListener;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.layout.LayoutTransition;
import edu.uci.ics.jung.visualization.picking.PickedState;
import edu.uci.ics.jung.visualization.util.Animator;

/**
 * A panel for the visualization of protein groups in the complete intermediate
 * tree / cluster.
 *
 * @author julian
 *
 */
public class AmbiguityGroupVisualizationHandler implements GraphMouseListener<VertexObject> {

    /** handler for the shown graph */
    private ProteinVisualizationGraphHandler visGraph;

    /** the currently selected vertex */
    private VertexObject selectedVertex;

    /** the default used layout */
    private static final Class<? extends Layout> defaultLayout = ProteinLayout.class;

    /** the viewer for the graphical visualization */
    private VisualizationViewer<VertexObject, String> visualizationViewer;

    /** the picked status of the graph, i.e. which vertex is selected */
    private PickedState<VertexObject> pickedState;

    /** the picked status of the graph, i.e. which vertex is selected */
    private Layout<VertexObject, String> layout;

    /** the mouse handler for the graph */
    private DefaultModalGraphMouse<VertexObject, String> graphMouse;



    public AmbiguityGroupVisualizationHandler(Group group,
            Map<VertexRelation, Set<Long>> relationsAccessions,
            Map<VertexRelation, Set<Long>> relationsPeptides,
            Map<VertexRelation, Set<Long>> relationsSpectra,
            ReportProtein reportProteinGroup) {
        super();

        this.visGraph = new ProteinVisualizationGraphHandler(group,
                relationsAccessions,
                relationsPeptides,
                relationsSpectra,
                reportProteinGroup);

        this.selectedVertex = null;

        setUpVisualizationPane();

        // set the initial picked vertex to the reference vertex
        pickedState.clear();
    }


    public VisualizationViewer<VertexObject, String> getVisualizationViewer() {
        return visualizationViewer;
    }


    /**
     * Sets up the basic settings of the panel.
     */
    private void setUpVisualizationPane() {
        // set up the layout
        layout = new ProteinLayout<String>(visGraph.getGraph());
        Layout<VertexObject, String> staticLayout = new StaticLayout<VertexObject, String>(visGraph.getGraph(), layout, layout.getSize());

        visualizationViewer = new VisualizationViewer<VertexObject,String>(staticLayout, layout.getSize());
        visualizationViewer.setBackground(Color.white);

        // listen to viewer resizing
        visualizationViewer.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                if (!(layout instanceof ProteinLayout)) {
                    layout.setSize(e.getComponent().getSize());
                }
            }
        });

        // listen to mouse clicks
        visualizationViewer.addGraphMouseListener(this);

        // let the pane listen to the vertex-picking
        pickedState = visualizationViewer.getPickedVertexState();

        // set the special vertex and labeller for the nodes
        ProteinVertexLabeller labeller = new ProteinVertexLabeller(visualizationViewer.getRenderContext(), 5);
        ProteinVertexShapeTransformer shaper = new ProteinVertexShapeTransformer(visualizationViewer.getRenderContext(), 5);
        ProteinVertexFillColorTransformer filler = new ProteinVertexFillColorTransformer(visGraph);
        ProteinVertexBorderColorTransformer borderColorizer = new ProteinVertexBorderColorTransformer(visGraph, pickedState);


        visualizationViewer.getRenderContext().setVertexShapeTransformer(shaper);
        visualizationViewer.getRenderContext().setVertexFillPaintTransformer(filler);
        visualizationViewer.getRenderContext().setVertexLabelTransformer(labeller);
        visualizationViewer.getRenderer().setVertexLabelRenderer(labeller);
        visualizationViewer.getRenderContext().setVertexStrokeTransformer(new ProteinVertexStrokeTransformer(visGraph, pickedState));

        // give a selected vertex red edges, otherwise paint it black
        visualizationViewer.getRenderContext().setVertexDrawPaintTransformer(borderColorizer);

        visualizationViewer.setVertexToolTipTransformer(labeller);

        // customize the edges to be straight lines
        visualizationViewer.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<VertexObject, String>());


        // define a manipulation mouse
        graphMouse = new DefaultModalGraphMouse<VertexObject, String>();
        // set PICKING as default mouse behaviour
        graphMouse.setMode(Mode.PICKING);

        visualizationViewer.setGraphMouse(graphMouse);
    }


    /**
     * Recalculates the layout and visualization of the graph for the changed
     * graph topology
     */
    private void recalculateAndAnimateGraphChanges() {
        layout.setGraph(visGraph.getGraph());
        layout.initialize();

        if (layout instanceof IterativeContext) {
            Relaxer relaxer = new VisRunner((IterativeContext)layout);
            relaxer.stop();
            relaxer.prerelax();
        }

        StaticLayout<VertexObject, String> staticLayout =
                new StaticLayout<VertexObject, String>(visGraph.getGraph(), layout, layout.getSize());

        LayoutTransition<VertexObject, String> lt =
                new LayoutTransition<VertexObject, String>(visualizationViewer,
                        visualizationViewer.getGraphLayout(),
                        staticLayout);

        Animator animator = new Animator(lt);
        animator.start();

        visualizationViewer.repaint();
    }


    @Override
    public void graphClicked(VertexObject v, MouseEvent me) {
        if (me.getButton() == MouseEvent.BUTTON1 && me.getClickCount() == 2) {
            boolean changeVisualisation = visGraph.doubleClickedOn(v);

            if (changeVisualisation) {
                pickedState.clear();
                pickedState.pick(selectedVertex, true);
                recalculateAndAnimateGraphChanges();
            }

            if (v.getObject() instanceof Accession) {
                System.err.println("Accession id: " + ((Accession)v.getObject()).getID());
            }
        }
        me.consume();
    }


    @Override
    public void graphPressed(VertexObject v, MouseEvent me) {
        // nothing to do, yet
    }


    @Override
    public void graphReleased(VertexObject v, MouseEvent me) {
        // nothing to do, yet
    }
}
