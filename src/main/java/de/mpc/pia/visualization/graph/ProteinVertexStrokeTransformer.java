package de.mpc.pia.visualization.graph;

import java.awt.BasicStroke;
import java.awt.Stroke;
import java.util.Collection;

import org.apache.commons.collections15.Transformer;

import de.mpc.pia.intermediate.Group;
import edu.uci.ics.jung.visualization.picking.PickedState;


/**
 * Transformer for the colors of the vertexObjects
 *
 * @author julianu
 *
 */
public class ProteinVertexStrokeTransformer
        implements Transformer<VertexObject, Stroke> {

    /** the graph handler which holds also information about the inferred proteins */
    private final ProteinVisualizationGraphHandler graphHandler;

    /** the picking state of the graph */
    private final PickedState<VertexObject> pickedState;


    private final BasicStroke thinStroke = new BasicStroke(1);
    private final BasicStroke thickStroke = new BasicStroke(2);


    /**
     * Constructor
     */
    public ProteinVertexStrokeTransformer(ProteinVisualizationGraphHandler graphHandler, PickedState<VertexObject> pickedState) {
        this.graphHandler = graphHandler;
        this.pickedState = pickedState;
    }



    @Override
    public Stroke transform(VertexObject vertex) {
        // the picked object has always a thick border
        if (pickedState.isPicked(vertex)) {
            return thickStroke;
        }

        Object vObject = vertex.getObject();

        if (vObject instanceof Group) {
            return thinStroke;
        }

        if (vObject instanceof Collection<?>) {
            vObject = ((Collection<?>)vObject).iterator().next();
        }

        VertexRelation relation = graphHandler.getVertexRelation(vertex);

        switch (relation) {
        case IN_PARALLEL_PAG:
        case IN_SAME_PAG:
        case IN_SUB_PAG:
            return thinStroke;

        case IN_SUPER_PAG:
        case IN_UNRELATED_PAG:
            return thickStroke;

        case IN_NO_PAG:
        default:
            return thinStroke;
        }
    }
}