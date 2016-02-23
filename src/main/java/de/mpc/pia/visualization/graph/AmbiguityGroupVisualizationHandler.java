package de.mpc.pia.visualization.graph;

import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.Set;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Group;
import de.mpc.pia.modeller.protein.ReportProtein;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.picking.MultiPickedState;
import edu.uci.ics.jung.visualization.picking.PickedState;

/**
 * A panel for the visualization of protein groups in the complete intermediate
 * tree / cluster.
 *
 * @author julian
 *
 */
public class AmbiguityGroupVisualizationHandler implements ItemListener {

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

    /** the currently picked protein (group) */
    private PickedState<VertexObject> pickedProtein;

    /** the picked status of the graph, i.e. which vertex is selected */
    private Layout<VertexObject, String> layout;

    /** the mouse handler for the graph */
    private DefaultModalGraphMouse<VertexObject, String> graphMouse;



    public AmbiguityGroupVisualizationHandler(Group group,
            Set<Long> selectedAccessions, Set<Long> otherAccessions,
            Set<Long> selectedPeptides, Set<Long> otherPeptides,
            Set<Long> selectedSpectra, Set<Long> otherSpectra,
            ReportProtein reportProteinGroup) {
        super();

        this.visGraph = new ProteinVisualizationGraphHandler(group,
                selectedAccessions, otherAccessions,
                selectedPeptides, otherPeptides,
                selectedSpectra, otherSpectra,
                reportProteinGroup);

        this.selectedVertex = null;
        pickedProtein = new MultiPickedState<VertexObject>();
        pickedProtein.clear();

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

        // let the pane listen to the vertex-picking
        pickedState = visualizationViewer.getPickedVertexState();
        pickedState.addItemListener(this);

        // set the special vertex and labeller for the nodes
        ProteinVertexLabeller labeller = new ProteinVertexLabeller(visualizationViewer.getRenderContext(), 5);
        ProteinVertexShapeTransformer shaper = new ProteinVertexShapeTransformer(visualizationViewer.getRenderContext(), 5);
        ProteinVertexFillColorTransformer filler = new ProteinVertexFillColorTransformer(visGraph, pickedProtein);
        ProteinVertexBorderColorTransformer borderColorizer = new ProteinVertexBorderColorTransformer(visGraph, pickedState, pickedProtein);


        visualizationViewer.getRenderContext().setVertexShapeTransformer(shaper);
        visualizationViewer.getRenderContext().setVertexFillPaintTransformer(filler);
        visualizationViewer.getRenderContext().setVertexLabelTransformer(labeller);
        visualizationViewer.getRenderer().setVertexLabelRenderer(labeller);
        visualizationViewer.getRenderContext().setVertexStrokeTransformer(new ProteinVertexStrokeTransformer(visGraph, pickedState, pickedProtein));

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


    @Override
    public void itemStateChanged(ItemEvent e) {
        Object subject = e.getItem();

        if (subject instanceof VertexObject) {
            Object vObject = ((VertexObject)subject).getObject();
            if (vObject instanceof Collection) {
                vObject = ((Collection) vObject).iterator().next();
            }

            if (vObject instanceof Accession){
                pickedProtein.clear();
                pickedProtein.pick((VertexObject)subject, true);
            }
        }
    }

}
