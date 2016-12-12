package de.mpc.pia.visualization.graph;

import java.awt.Color;
import java.awt.Paint;
import java.util.Collection;

import org.apache.commons.collections15.Transformer;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Group;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import edu.uci.ics.jung.visualization.picking.PickedState;


/**
 * Transformer for the boder colors of the vertexObjects
 *
 * @author julianu
 *
 */
public class ProteinVertexBorderColorTransformer
        implements Transformer<VertexObject, Paint> {

    /** the graph handler which holds also information about the inferred proteins */
    private final ProteinVisualizationGraphHandler graphHandler;

    /** the currently selected vertex */
    private final PickedState<VertexObject> pickedState;


    // some border colors
    private static final Color DEFAULT_BORDER_COLOR = Color.BLACK;

    private static final Color SELECTED_BORDER_COLOR = new Color(0x00FFFF);


    /**
     * Constructor
     */
    public ProteinVertexBorderColorTransformer(ProteinVisualizationGraphHandler graphHandler, PickedState<VertexObject> pickedState) {
        this.graphHandler = graphHandler;
        this.pickedState = pickedState;
    }


    @Override
    public Paint transform(VertexObject vertex) {
        Object vObject = vertex.getObject();

        if (pickedState.isPicked(vertex)) {
            return SELECTED_BORDER_COLOR;
        }

        if (vObject instanceof Group) {
            return DEFAULT_BORDER_COLOR;
        }

        if (vObject instanceof Collection<?>) {
            vObject = ((Collection<?>)vertex.getObject()).iterator().next();
        }

        VertexRelation relation = graphHandler.getVertexRelation(vertex);

        switch (relation) {
        case IN_SAME_PAG:
        case IN_PARALLEL_PAG:
            return DEFAULT_BORDER_COLOR;

        case IN_SUB_PAG:
            if (vObject instanceof Peptide)  {
                return ProteinVertexFillColorTransformer.PEPTIDE_COLOR_FAINT;
            } else if (vObject instanceof Accession) {
                return ProteinVertexFillColorTransformer.PROTEIN_COLOR_FAINT;
            } else if (vObject instanceof PeptideSpectrumMatch) {
                return ProteinVertexFillColorTransformer.PSM_COLOR_FAINT;
            }

        case IN_SUPER_PAG:
        case IN_UNRELATED_PAG:
            if (vObject instanceof Peptide)  {
                return ProteinVertexFillColorTransformer.PEPTIDE_COLOR;
            } else if (vObject instanceof Accession) {
                return ProteinVertexFillColorTransformer.PROTEIN_COLOR;
            } else if (vObject instanceof PeptideSpectrumMatch) {
                return ProteinVertexFillColorTransformer.PSM_COLOR;
            }

        case IN_NO_PAG:
        default:
            return DEFAULT_BORDER_COLOR;
        }
    }
}