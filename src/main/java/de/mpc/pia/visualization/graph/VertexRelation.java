package de.mpc.pia.visualization.graph;

/**
 * This enum describes the relativity of a vertex to another considering
 * their PAGs
 * @author julian
 *
 */
enum VertexRelation {
    IN_NO_PAG,
    IN_UNRELATED_PAG,
    IN_SAME_PAG,
    IN_SUPER_PAG,
    IN_SUB_PAG,
    IN_PARALLEL_PAG,
    ;
}