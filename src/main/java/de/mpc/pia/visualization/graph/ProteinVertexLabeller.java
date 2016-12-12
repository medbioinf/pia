package de.mpc.pia.visualization.graph;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;
import org.apache.commons.collections15.Transformer;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Group;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;

import java.awt.*;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.Locale;


/**
 * Labeller and transformer for the group visualization
 *
 * @author julianu
 *
 */
public class ProteinVertexLabeller
        implements Renderer.VertexLabel<VertexObject, String>, Transformer<VertexObject, String> {

    private final RenderContext<VertexObject, String> rc;

    /** formatter to show m/z values in PSM labels */
    final static DecimalFormat decimalFormatter;

    static {
        DecimalFormatSymbols decimalSymbols = new DecimalFormatSymbols(Locale.US);
        decimalFormatter = new DecimalFormat("0.####");
        decimalFormatter.setDecimalFormatSymbols(decimalSymbols);
    }


    /**
     * Constructor
     *
     * @param rc
     * @param margin
     */
    public ProteinVertexLabeller(RenderContext<VertexObject, String> rc, int margin) {
        this.rc = rc;
    }


    /**
     * Prepares the rendering and returns an component, to w.g. calculate the
     * preferred component size.
     *
     * @param vertex
     * @return
     */
    public Component prepareRenderer(VertexObject vertex) {
        return rc.getVertexLabelRenderer().getVertexLabelRendererComponent(
                rc.getScreenDevice(),
                rc.getVertexLabelTransformer().transform(vertex),
                rc.getVertexFontTransformer().transform(vertex),
                rc.getPickedVertexState().isPicked(vertex),
                vertex);
    }


    @Override
    public void labelVertex(RenderContext<VertexObject, String> rc, Layout<VertexObject, String> layout, VertexObject v, String label) {
        Graph<VertexObject, String> graph = layout.getGraph();

        if (!rc.getVertexIncludePredicate().evaluate(Context.getInstance(graph, v))) {
            return;
        }

        GraphicsDecorator g = rc.getGraphicsContext();

        Component component = prepareRenderer(v);
        Dimension d = new Dimension(
                (int)component.getPreferredSize().getWidth(),
                (int)component.getPreferredSize().getHeight());

        Point2D p = layout.transform(v);
        p = rc.getMultiLayerTransformer().transform(Layer.LAYOUT, p);

        int x = (int)p.getX();
        int y = (int)p.getY();

        g.draw(component, rc.getRendererPane(), x - d.width / 2, y - d.height / 2, d.width, d.height, true);
    }


    @Override
    public String transform(VertexObject vertex) {
        Object vObject = vertex.getObject();

        if (vObject instanceof Group) {
            return "";
        } else if (vObject instanceof PeptideSpectrumMatch) {
            StringBuilder lblSb = new StringBuilder();
            lblSb.append(decimalFormatter.format(
                    ((PeptideSpectrumMatch) vObject).getMassToCharge()));

            if (((PeptideSpectrumMatch) vObject).getCharge() != 0)  {
                lblSb.append(", ");
                if (((PeptideSpectrumMatch) vObject).getCharge() > 0) {
                    lblSb.append("+");
                } else {
                    lblSb.append("-");
                }
                lblSb.append(((PeptideSpectrumMatch) vObject).getCharge());
            }

            return lblSb.toString();
        } else if (vObject instanceof Collection<?>)  {
            vObject = ((Collection<?>)vertex.getObject()).iterator().next();
            StringBuilder lblSb = new StringBuilder();

            if (vObject instanceof Peptide)  {
                lblSb.append("peptides (#");
            } else if (vObject instanceof Accession) {
                lblSb.append("proteins (#");
            }

            lblSb.append(((Collection<?>)vertex.getObject()).size());
            lblSb.append(")");
            return lblSb.toString();
        }

        // nothing of the above: show the vertex label
        return vertex.getLabel();
    }


    @Override
    public Position getPosition() {
        // the label is always centered
        return Position.CNTR;
    }


    @Override
    public Positioner getPositioner() {
        return (x, y, d) -> Position.CNTR;
    }


    @Override
    public void setPosition(Position position) {
        // TODO Auto-generated method stub
    }


    @Override
    public void setPositioner(Positioner positioner) {
        // TODO Auto-generated method stub
    }
}