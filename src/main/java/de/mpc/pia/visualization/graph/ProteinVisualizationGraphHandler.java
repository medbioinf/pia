package de.mpc.pia.visualization.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Group;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.modeller.protein.ReportProtein;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;


/**
 * Handler class for a graph which visualizes a protein ambiguity group.
 *
 * @author julian
 *
 */
public class ProteinVisualizationGraphHandler {

    /** the protein-group-peptides-PSMs graph */
    private DirectedGraph<VertexObject, String> graph;


    /** mapping from the group's label to whether its accessions are shown */
    private Map<String, Boolean> expandedAccessionsMap;

    /** mapping from the group's label to whether its peptides are shown */
    private Map<String, Boolean> expandedPeptidesMap;

    /** mapping from the peptide's label to whether its spectra a shown */
    private Map<String, Boolean> showPSMsMap;

    /** mapping from the group ID to the vertex in the graph */
    private Map<Long, VertexObject> groupVertices;


    /** the protein ambiguity group, for which relations should be calculated (can be null) */
    private ReportProtein reportProteinGroup;


    /** IDs of accessions, grouped by relations to selected PAG */
    private Map<VertexRelation, Set<Long>> relationsAccessions;
    /** IDs of peptides, grouped by relations to selected PAG */
    private Map<VertexRelation, Set<Long>> relationsPeptides;
    /** IDs of spectra, grouped by relations to selected PAG */
    private Map<VertexRelation, Set<Long>> relationsSpectra;



    private static final String PROTEINS_OF_PREFIX = "proteins_of_";
    private static final String PEPTIDES_OF_PREFIX = "peptides_of_";


    public ProteinVisualizationGraphHandler(Group startGroup,
            Map<VertexRelation, Set<Long>> relationsAccessions,
            Map<VertexRelation, Set<Long>> relationsPeptides,
            Map<VertexRelation, Set<Long>> relationsSpectra,
            ReportProtein reportProteinGroup) {

        this.reportProteinGroup = reportProteinGroup;

        this.relationsAccessions = relationsAccessions;
        this.relationsPeptides = relationsPeptides;
        this.relationsSpectra = relationsSpectra;

        this.expandedAccessionsMap = new HashMap<String, Boolean>();
        this.expandedPeptidesMap = new HashMap<String, Boolean>();
        this.showPSMsMap = new HashMap<String, Boolean>();

        createGraphFromIntermediateStructure(startGroup);
    }


    /**
     * getter for the graph
     * @return
     */
    public DirectedGraph<VertexObject, String> getGraph() {
        return graph;
    }


    /**
     * returns whether the accessions of the given group vertex are currently
     * shown/expanded
     */
    public boolean isExpandedAccessions(VertexObject vertex) {
        return (expandedAccessionsMap.containsKey(vertex.getLabel())) ?
                expandedAccessionsMap.get(vertex.getLabel()) : false;
    }


    /**
     * returns whether the peptides of the given group vertex are currently
     * shown/expanded
     */
    public Boolean isExpandedPeptides(VertexObject vertex) {
        return (expandedPeptidesMap.containsKey(vertex.getLabel())) ?
                expandedPeptidesMap.get(vertex.getLabel()) : false;
    }


    /**
     * returns whether the PSMs of the given peptide vertex are currently
     * shown/expanded
     */
    public Boolean isExpandedPSMs(VertexObject vertex) {
        return (showPSMsMap.containsKey(vertex.getLabel())) ?
                showPSMsMap.get(vertex.getLabel()) : false;
    }


    /**
     * creates the graph from the intermediate structure, also clusters using
     * the given settings
     */
    private void createGraphFromIntermediateStructure(Group startGroup) {
        // initialize the graph to be a directed sparse graph
        graph = new DirectedSparseGraph<VertexObject, String>();

        groupVertices = new HashMap<Long, VertexObject>();

        Set<Group> groups = new HashSet<Group>();

        // get and layer all the groups in the tree
        Set<Group> toAdd =  new HashSet<Group>();
        toAdd.add(startGroup);
        while (toAdd.size() > 0) {
            // get a group from the toAdd set
            Group group = toAdd.iterator().next();

            // add group to groups and the graph and remove from toAdd
            groups.add(group);
            toAdd.remove(group);

            VertexObject groupV = addGroupVertex(group);

            // connect to the child-groups
            if (group.getChildren() != null) {
                for (Group child : group.getChildren().values()) {
                    VertexObject childV = addGroupVertex(child);
                    String edgeName = "groupGroup_" + groupV.getLabel() + "_" + childV.getLabel();
                    graph.addEdge(edgeName, groupV, childV);
                }
            }

            // add the proteins collapsed
            if ((group.getAccessions() != null) && (group.getAccessions().size() > 0)) {
                addAccessionVertices(groupV, true);
            }

            // add the peptides
            if ((group.getPeptides() != null) && (group.getPeptides().size() > 0)) {
                addPeptideVertices(groupV, true, false);
            }

            // add children to toAdd, if not in groups
            for (Group childGroup : group.getChildren().values()) {
                if (!groups.contains(childGroup)) {
                    toAdd.add(childGroup);
                }
            }

            // add parents to toAdd, if not in groups
            for (Group parentGroup : group.getParents().values()) {
                if (!groups.contains(parentGroup)) {
                    toAdd.add(parentGroup);
                }
            }
        }
    }


    /**
     * Adds the proteins of the given group to the graph, either collapsed or
     * uncollapsed. If the location is not null, set the proteins' position to
     * the given location.
     */
    private Collection<VertexObject> addAccessionVertices(VertexObject groupV, Boolean collapsed) {
        List<VertexObject> proteins = new ArrayList<VertexObject>();
        Group group = (Group)groupV.getObject();

        if (collapsed && (group.getAccessions().size() > 1)) {
            // show the proteins collapsed
            String proteinLabel = PROTEINS_OF_PREFIX + groupV.getLabel();
            VertexObject proteinsV =
                    new VertexObject(proteinLabel, group.getAccessions().values());

            graph.addVertex(proteinsV);
            proteins.add(proteinsV);

            String edgeName = "proteinGroup_" + proteinLabel + "_" + groupV.getLabel();
            graph.addEdge(edgeName, proteinsV, groupV);

            expandedAccessionsMap.put(groupV.getLabel(), false);

            // TODO: check if it contains the reference protein
        } else {
            for (Accession acc : group.getAccessions().values()) {
                String proteinLabel = acc.getAccession();
                VertexObject proteinV = new VertexObject(proteinLabel, acc);

                graph.addVertex(proteinV);
                proteins.add(proteinV);

                String edgeName = "proteinGroup_" + proteinLabel + "_" + groupV.getLabel();
                graph.addEdge(edgeName, proteinV, groupV);

                // TODO: check if this is the reference protein
            }
            expandedAccessionsMap.put(groupV.getLabel(), true);
        }

        return proteins;
    }


    /**
     * Adds the peptides of the given group to the graph, either collapsed or
     * uncollapsed. If the location is not null, set the peptides' position to
     * the given location.
     */
    private Collection<VertexObject> addPeptideVertices(VertexObject groupV, Boolean collapsed,
            Boolean showPSMs) {
        List<VertexObject> peptides = new ArrayList<VertexObject>();
        Group group = (Group)groupV.getObject();

        if (collapsed && (group.getPeptides().size() > 1)) {
            // show the peptides collapsed
            String peptidesLabel = PEPTIDES_OF_PREFIX + groupV.getLabel();
            VertexObject peptidesV = new VertexObject(peptidesLabel, group.getPeptides().values());

            graph.addVertex(peptidesV);
            peptides.add(peptidesV);

            String edgeName = "groupPeptide_" + groupV.getLabel() + "_" + peptidesLabel;
            graph.addEdge(edgeName, groupV, peptidesV);

            expandedPeptidesMap.put(groupV.getLabel(), false);
        } else {
            // uncollapsed peptides
            for (Peptide peptide : group.getPeptides().values()) {
                String peptideLabel = peptide.getSequence();
                VertexObject peptideV = new VertexObject(peptideLabel, peptide);

                graph.addVertex(peptideV);
                peptides.add(peptideV);

                String edgeName = "groupPeptide_" + groupV.getLabel() + "_" + peptideLabel;
                graph.addEdge(edgeName, groupV, peptideV);

                showPSMsMap.put(peptideLabel, false);

                if (showPSMs && !collapsed) {
                    showPSMs(peptideV);
                }
            }
            expandedPeptidesMap.put(groupV.getLabel(), true);
        }

        return peptides;
    }


    /**
     * Adds the PSMs of the given peptide to the graph. If the location is not
     * null, set the peptides' position to the given location.
     */
    private Collection<VertexObject> addPSMVertices(VertexObject peptideV) {
        List<VertexObject> psms = new ArrayList<VertexObject>();
        Peptide peptide = (Peptide)peptideV.getObject();

        // add the PSMs
        for (PeptideSpectrumMatch psm : peptide.getSpectra()) {
            String psmLabel = psm.getID().toString();
            VertexObject psmV = new VertexObject(psmLabel, psm);

            graph.addVertex(psmV);
            psms.add(psmV);

            String psmEdgeName = "peptidePSM_" + peptideV.getLabel() + "_" + psmLabel;
            graph.addEdge(psmEdgeName, peptideV, psmV);
        }

        showPSMsMap.put(peptideV.getLabel(), true);
        return psms;
    }


    /**
     * adds a group vertex to the graph, if not a vertex for this group is
     * already added
     *
     * @param group
     * @return group's VertexObject (either newly created or already from the graph)
     */
    private VertexObject addGroupVertex(Group group) {
        String groupLabel = String.valueOf(group.getID());
        VertexObject groupV = groupVertices.get(group.getID());
        if (groupV == null) {
            groupV = new VertexObject(groupLabel, group);
            graph.addVertex(groupV);

            groupVertices.put(group.getID(), groupV);
        }

        return groupV;
    }


    /**
     * Uncollapses the proteins of the given {@link VertexObject}, which should
     * be an {@link Group} representative
     * @param groupV
     * @return returns the uncollapsed vertices
     */
    public Collection<VertexObject> uncollapseAccessions(VertexObject groupV) {
        if ((groupV == null) ||
                !(groupV.getObject() instanceof Group) ||
                ((expandedAccessionsMap.get(groupV.getLabel()) != null) && expandedAccessionsMap.get(groupV.getLabel()))) {
            return new ArrayList<VertexObject>();
        }

        // remove the collapsed proteins
        Iterator<String> edgeIt = graph.getIncidentEdges(groupV).iterator();
        while (edgeIt.hasNext()) {
            String edge = edgeIt.next();
            VertexObject proteinsV = graph.getOpposite(groupV, edge);
            if (proteinsV.getLabel().equals(PROTEINS_OF_PREFIX + groupV.getLabel())) {
                graph.removeVertex(proteinsV);
                break;
            }
        }

        // add the proteins uncollapsed
        return addAccessionVertices(groupV, false);
    }


    /**
     * Collapses the proteins of the given {@link VertexObject}, which should be
     * an {@link Group} representative
     *
     * @param groupV
     */
    public Collection<VertexObject> collapseAccessions(VertexObject groupV) {
        if ((groupV == null) ||
                !(groupV.getObject() instanceof Group) ||
                (expandedAccessionsMap.get(groupV.getLabel()) == null) ||
                !expandedAccessionsMap.get(groupV.getLabel()) ||
                (((Group)groupV.getObject()).getAccessions() == null) ||
                (((Group)groupV.getObject()).getAccessions().size() < 2)) {
            return new ArrayList<VertexObject>();
        }

        // remove all the protein vertices
        Iterator<String> edgeIt = graph.getIncidentEdges(groupV).iterator();
        while (edgeIt.hasNext()) {
            String edge = edgeIt.next();
            VertexObject proteinV = graph.getOpposite(groupV, edge);
            if (proteinV.getObject() instanceof Accession) {
                graph.removeVertex(proteinV);
            }
        }

        // add the proteins collapsed
        return addAccessionVertices(groupV, true);
    }


    /**
     * Uncollapses the peptides of the given {@link VertexObject}, which should
     * be an {@link Group} representative
     * @param groupV
     */
    public Collection<VertexObject> uncollapsePeptides(VertexObject groupV) {
        if ((groupV == null) ||
                !(groupV.getObject() instanceof Group) ||
                ((expandedPeptidesMap.get(groupV.getLabel()) != null) && expandedPeptidesMap.get(groupV.getLabel()))) {
            return new ArrayList<VertexObject>();
        }

        // remove the collapsed peptides
        Iterator<String> edgeIt = graph.getIncidentEdges(groupV).iterator();
        while (edgeIt.hasNext()) {
            String edge = edgeIt.next();
            VertexObject peptidesV = graph.getOpposite(groupV, edge);
            if (peptidesV.getLabel().equals(PEPTIDES_OF_PREFIX + groupV.getLabel())) {
                graph.removeVertex(peptidesV);
                break;
            }
        }

        // add the peptides uncollapsed
        return addPeptideVertices(groupV, false, false);
    }


    /**
     * Collapses the peptides of the given {@link VertexObject}, which should
     * be an {@link Group} representative
     * @param groupV
     */
    public Collection<VertexObject> collapsePeptides(VertexObject groupV) {
        if ((groupV == null) ||
                !(groupV.getObject() instanceof Group) ||
                (expandedPeptidesMap.get(groupV.getLabel()) == null) ||
                !expandedPeptidesMap.get(groupV.getLabel()) ||
                (((Group)groupV.getObject()).getPeptides() == null) ||
                (((Group)groupV.getObject()).getPeptides().size() < 2)) {
            return new ArrayList<VertexObject>();
        }

        // remove all the peptide and PSM vertices
        Iterator<String> edgeIt = graph.getIncidentEdges(groupV).iterator();
        while (edgeIt.hasNext()) {
            String edge = edgeIt.next();
            VertexObject peptideV = graph.getOpposite(groupV, edge);
            if (peptideV.getObject() instanceof Peptide) {
                if (isExpandedPSMs(peptideV)) {
                    hidePSMs(peptideV);
                }
                graph.removeVertex(peptideV);
            }
        }

        // add the peptides collapsed
        return addPeptideVertices(groupV, true, false);
    }


    /**
     * Shows the PSMs of the given {@link VertexObject}, which should
     * be an {@link Peptide} representative
     * @param peptideV
     */
    public Collection<VertexObject> showPSMs(VertexObject peptideV) {
        if ((peptideV == null) ||
                !(peptideV.getObject() instanceof Peptide) ||
                ((showPSMsMap.get(peptideV.getLabel()) != null) && showPSMsMap.get(peptideV.getLabel()))) {
            return new ArrayList<VertexObject>();
        }

        return addPSMVertices(peptideV);
    }


    /**
     * Hides the PSMs of the given {@link VertexObject}, which should
     * be an {@link Peptide} representative
     * @param peptideV
     */
    public void hidePSMs(VertexObject peptideV) {
        if ((peptideV == null) ||
                !(peptideV.getObject() instanceof Peptide) ||
                (showPSMsMap.get(peptideV.getLabel()) == null) ||
                !showPSMsMap.get(peptideV.getLabel())) {
            return;
        }

        // remove the PSMs from the graph
        Iterator<String> edgeIt = graph.getIncidentEdges(peptideV).iterator();
        while (edgeIt.hasNext()) {
            String edge = edgeIt.next();
            VertexObject psmV = graph.getOpposite(peptideV, edge);
            if (psmV.getObject() instanceof PeptideSpectrumMatch) {
                graph.removeVertex(psmV);
            }
        }

        showPSMsMap.put(peptideV.getLabel(), false);
    }


    /**
     * Gets the relation of the given proteinVertex to the selected PAG.
     *
     * @param relatedVertex
     * @return
     */
    public VertexRelation getVertexRelation(VertexObject relatedVertex) {
        if (reportProteinGroup == null) {
            // there is no information -> everything is in same PAG
            return VertexRelation.IN_SAME_PAG;
        }

        VertexRelation relation = VertexRelation.IN_NO_PAG;
        Object obj = relatedVertex.getObject();

        if (obj instanceof Collection<?>) {
            Iterator<?> iter = ((Collection<?>)obj).iterator();

            while (iter.hasNext()) {
                VertexRelation newRelation = getObjectRelation(iter.next());

                if (newRelation.ordinal() < relation.ordinal()) {
                    relation = newRelation;
                }
                if (relation.ordinal() == 0) {
                    return relation;
                }
            }

        } else {
            relation =  getObjectRelation(obj);
        }

        return relation;
    }


    /**
     * Gets the relation of the given Object, which should be the value of a
     * vertex.
     *
     * @param obj
     * @return
     */
    private VertexRelation getObjectRelation(Object obj) {
        Long vertexId = null;
        Iterator<Map.Entry<VertexRelation, Set<Long>>> relIt = null;

        if (obj instanceof Accession) {
            vertexId = ((Accession) obj).getID();
            if (relationsAccessions != null) {
                relIt = relationsAccessions.entrySet().iterator();
            }
        } else if (obj instanceof Peptide) {
            vertexId = ((Peptide) obj).getID();
            if (relationsPeptides != null) {
                relIt = relationsPeptides.entrySet().iterator();
            }
        } else if (obj instanceof PeptideSpectrumMatch) {
            vertexId = ((PeptideSpectrumMatch) obj).getID();
            if (relationsSpectra != null) {
                relIt = relationsSpectra.entrySet().iterator();
            }
        }

        if ((relIt != null) && (vertexId != null)) {
            while (relIt.hasNext()) {
                Map.Entry<VertexRelation, Set<Long>> mapping = relIt.next();
                if (mapping.getValue().contains(vertexId)) {
                    return mapping.getKey();
                }
            }
        }

        return VertexRelation.IN_NO_PAG;
    }


    /**
     * A mouse doubleclick was detectedon the given vertex.
     *
     * @param v
     */
    public boolean doubleClickedOn(VertexObject v) {

        VertexObject groupVertex = getGroupOf(v);

        if (v.getLabel().startsWith(PEPTIDES_OF_PREFIX)) {
            // double click on peptide collection ->  uncollapse
            if (groupVertex != null) {
                uncollapsePeptides(groupVertex);
                return true;
            }
        } else if (v.getLabel().startsWith(PROTEINS_OF_PREFIX)) {
            // double click on accessions collection ->  uncollapse
            if (groupVertex != null) {
                uncollapseAccessions(groupVertex);
                return true;
            }
        } else if (v.getObject() instanceof Peptide) {
            // clicked on a peptide -> show or hide PSMs
            if (isExpandedPSMs(v)) {
                hidePSMs(v);
                return false;
            } else {
                showPSMs(v);
                return true;
            }
        } else if (v.getObject() instanceof Group) {
            // clicked on a group -> collapse peptides and accessions

            int accessionCount = 0;
            int peptideCount = 0;

            for (VertexObject vertex : graph.getPredecessors(v)) {
                if (vertex.getObject() instanceof Accession) {
                    accessionCount++;
                }
            }
            for (VertexObject vertex : graph.getSuccessors(v)) {
                if (vertex.getObject() instanceof Peptide) {
                    peptideCount++;
                }
            }

            if (peptideCount > 1) {
                collapsePeptides(v);
            }
            if (accessionCount > 1) {
                collapseAccessions(v);
            }
            if ((peptideCount > 1) || (accessionCount > 1)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Returns the closest {@link VertexObject} representing a group for the
     * given vertex.
     *
     * @param v
     * @return
     */
    private VertexObject getGroupOf(VertexObject v) {

        boolean lookInSuccessors = false;

        if (v.getObject() instanceof Collection<?>) {
            if (v.getLabel().startsWith(PEPTIDES_OF_PREFIX)) {
                lookInSuccessors = false;
            } else if (v.getLabel().startsWith(PROTEINS_OF_PREFIX)) {
                lookInSuccessors = true;
            }
        } else if (v.getObject() instanceof Accession) {
            lookInSuccessors = true;
        } else if (v.getObject() instanceof Peptide) {
            lookInSuccessors = false;
        } else if (v.getObject() instanceof PeptideSpectrumMatch) {
            for (VertexObject vertex : graph.getSuccessors(v)) {
                if (vertex.getObject() instanceof Peptide) {
                    v = vertex;
                    break;
                }
            }
            lookInSuccessors = false;
        }

        Collection<VertexObject> lookIn;
        if (lookInSuccessors) {
            lookIn = graph.getSuccessors(v);
        } else {
            lookIn = graph.getPredecessors(v);
        }

        for (VertexObject vertex : lookIn) {
            if (vertex.getObject() instanceof Group) {
                return  vertex;
            }
        }

        return null;
    }
}