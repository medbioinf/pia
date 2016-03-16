package de.mpc.pia.visualization.graph;

/**
 * This enum describes the relativity of a vertex to another considering
 * their PAGs
 * @author julian
 *
 */
public enum VertexRelation {
    // keep the relations in order (to color collections in the ordinal lowest group)
    IN_SAME_PAG,
    IN_PARALLEL_PAG,
    IN_SUPER_PAG,
    IN_SUB_PAG,
    IN_UNRELATED_PAG,
    IN_NO_PAG,
    ;
}