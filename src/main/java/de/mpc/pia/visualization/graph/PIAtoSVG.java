package de.mpc.pia.visualization.graph;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Group;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.modeller.PIAModeller;


/**
 * This class creates a graphical representation of a PIA tree using
 * dot/graphviz.
 * <p>
 * Graphviz must be installed on the computer running these methods and dot must
 * be executable by the Java instance.
 *
 * @author julian
 *
 */
public class PIAtoSVG {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(PIAtoSVG.class);

    /** name of the graph */
    private String graphName;

    /** the edges */
    private StringBuilder edges;

    /** th enodes */
    private StringBuilder nodes;

    /** will be returned, if dot cannot be called */
    private static final String ERROR_DOT_SVG = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
            "<svg width='400' height='40'>" +
            "<text font-family='Arial' font-style='normal' font-size='22pt' stroke='none' fill='#000000'>" +
            "<tspan x='5' y='5'>Could not run dot!</tspan>" +
            "</text>" +
            "</svg>";

    /** will be returned, if any error occurred */
    private static final String ERROR_SVG = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
            "<svg width='400' height='40'>" +
            "<text font-family='Arial' font-style='normal' font-size='22pt' stroke='none' fill='#000000'>" +
            "<tspan x='5' y='5'>Error while creating SVG!</tspan>" +
            "</text>" +
            "</svg>";



    /**
     * Basic constructor.
     */
    public PIAtoSVG() {
        graphName = "G";
        edges = new StringBuilder();
        nodes = new StringBuilder();
    }


    /**
     * Constructor which takes a PIA {@link Group} element and builds all the
     * commands to create a graphical representation of the PIA tree of the
     * startGroup.
     *
     * @param startGroup
     * @param name name of the returned graph
     */
    public PIAtoSVG(Group startGroup, String name) {
        this(startGroup, name,
                null, null,
                null, null,
                null, null);
    }


    public PIAtoSVG(Group startGroup, String name,
                    Set<Long> thisAccessions, Set<Long> otherAccessions,
                    Set<Long> thisPeptides, Set<Long> otherPeptides,
                    Set<Long> thisSpectra, Set<Long> otherSpectra) {
        this();

        graphName = name;

        if (startGroup == null) {
            return;
        }

        Set<Group> groups = new HashSet<>();

        // get and layer all the groups in the tree
        Set<Group> toAdd = new HashSet<>();
        toAdd.add(startGroup);
        while (!toAdd.isEmpty()) {
            // get a group from the toAdd set
            Group group = toAdd.iterator().next();

            // add group to groups and the graph and remove from toAdd
            groups.add(group);
            toAdd.remove(group);


            // create the node representing the group
            nodes.append("grp").append(group.getID()).append(" [shape=ellipse color=\"#0000FF\" fillcolor=\"#0000FF\" style=filled label=\"\" width=.2 height=.2];\n");

            // create accession nodes and edges
            for (Accession acc : group.getAccessions().values()) {
                nodes.append("acc").append(acc.getID()).append(" [shape=box ");

                if ((thisAccessions != null) && (otherAccessions != null)) {
                    if (thisAccessions.contains(acc.getID())) {
                        nodes.append("color=\"#008000\" fillcolor=\"#008000\" style=filled");
                    } else if (otherAccessions.contains(acc.getID())) {
                        nodes.append("color=\"#008000\"");
                    } else {
                        nodes.append("color=\"#C0C0C0\" fontcolor=\"#C0C0C0\"");
                    }
                } else {
                    nodes.append("color=\"#008000\"");
                }

                nodes.append(" label=\"").append(acc.getAccession()).append("\" width=.5 height=.2 margin=.05];\n");

                edges.append("acc").append(acc.getID()).append(" -> ").append("grp").append(group.getID()).append(";\n");
            }

            // create peptide nodes and edges
            for (Peptide pep : group.getPeptides().values()) {
                nodes.append("pep").append(pep.getID()).append(" [shape=box ");

                if ((thisPeptides != null) && (otherPeptides != null)) {
                    if (thisPeptides.contains(pep.getID())) {
                        nodes.append("color=\"#FFA500\" fillcolor=\"#FFA500\" style=filled");
                    } else if (otherPeptides.contains(pep.getID())) {
                        nodes.append("color=\"#FFA500\"");
                    } else {
                        nodes.append("color=\"#C0C0C0\" fontcolor=\"#C0C0C0\"");
                    }
                } else {
                    nodes.append("color=\"#FFA500\"");

                    if ((thisPeptides != null) &&
                            thisPeptides.contains(pep.getID())) {
                        nodes.append(" fillcolor=\"#FFA500\" style=filled");
                    }
                }

                nodes.append(" label=\"").append(pep.getSequence()).append("\" width=.5 height=.2 margin=.05];\n");

                edges.append("grp").append(group.getID()).append(" -> ").append("pep").append(pep.getID()).append(";\n");

                for (PeptideSpectrumMatch psm : pep.getSpectra()) {
                    StringBuilder label = new StringBuilder();

                    if (psm.getSourceID() != null) {
                        label.append(psm.getSourceID());
                    } else if (psm.getSpectrumTitle() != null) {
                        label.append(psm.getSpectrumTitle());
                    } else {
                        label.append("z=");
                        label.append(psm.getCharge());
                        if (psm.getRetentionTime() != null)  {
                            label.append(", rt=");
                            label.append(psm.getRetentionTime());
                        }
                    }

                    nodes.append("psm").append(psm.getID()).append(" [shape=box ");

                    if ((thisSpectra != null) && (otherSpectra != null)) {
                        if (thisSpectra.contains(psm.getID())) {
                            nodes.append("color=\"#87CEEB\" fillcolor=\"#87CEEB\" style=filled");
                        } else if (otherSpectra.contains(psm.getID())) {
                            nodes.append("color=\"#87CEEB\"");
                        } else {
                            nodes.append("color=\"#C0C0C0\" fontcolor=\"#C0C0C0\"");
                        }
                    } else {
                        nodes.append("color=\"#87CEEB\"");

                        if ((thisSpectra != null) &&
                                thisSpectra.contains(psm.getID())) {
                            nodes.append(" fillcolor=\"#87CEEB\" style=filled");
                        }
                    }

                    nodes.append(" label=\"").append(label).append("\" width=.3 height=.1 margin=.025 fontsize=7];\n");

                    edges.append("pep").append(pep.getID()).append(" -> ").append("psm").append(psm.getID()).append(";\n");
                }
            }


            for (Map.Entry<Long, Group> grIt
                    : group.getChildren().entrySet()) {
                // add children to toAdd, if not in groups
                if (!groups.contains(grIt.getValue())) {
                    toAdd.add(grIt.getValue());
                }

                // add the edges to the child
                edges.append("grp").append(group.getID()).append(" -> ").append("grp").append(grIt.getValue().getID()).append(";\n");
            }

            // add parents to toAdd, if not in groups
            toAdd.addAll(group.getParents().entrySet().stream().filter(grIt -> !groups.contains(grIt.getValue())).map(Map.Entry::getValue).collect(Collectors.toList()));
        }
    }


    /**
     * Writes the PIA tree in SVG format into the given {@link OutputStream}.
     * The SVG is generated by dot, which must be installed and executable by
     * this instance.
     *
     * @return
     */
    public void createSVG(OutputStream os) {
        List<String> params = new ArrayList<>();

        params.add("dot");
        params.add("-Kdot");
        params.add("-Tsvg");
        params.add("-Nfontsize=8");
        params.add("-Nfontname=Arial");

        try {
            Process p = new ProcessBuilder(params).start();

            BufferedWriter graphWriter = new BufferedWriter(
                    new OutputStreamWriter(p.getOutputStream()));

            graphWriter.write("digraph " + graphName + " {\n");
            graphWriter.write(nodes.toString());
            graphWriter.write(edges.toString());
            graphWriter.write("}");

            graphWriter.flush();
            graphWriter.close();


            int n;
            byte[] buffer = new byte[16384];
            while((n = p.getInputStream().read(buffer)) > -1) {
                os.write(buffer, 0, n);
            }

            p.waitFor();
        } catch (IOException e) {
            LOGGER.warn("dot could not be executed, please set it up correctly", e);

            try {
                BufferedWriter bw = new BufferedWriter(
                        new OutputStreamWriter(os));
                bw.write(ERROR_DOT_SVG);
                bw.close();
            } catch (IOException ex) {
                LOGGER.error("dot could not be executed, please set it up correctly", ex);
            }
        } catch (InterruptedException e) {
            LOGGER.warn("Error while executing dot.", e);

            try {
                BufferedWriter bw = new BufferedWriter(
                        new OutputStreamWriter(os));
                bw.write(ERROR_SVG);
                bw.close();
            } catch (IOException ex) {
                LOGGER.error("Error while executing dot.", ex);
            }
            Thread.currentThread().interrupt();
        }
    }


    /**
     * Returns a String containing the PIA tree in SVG format. The SVG is
     * generated by dot, which must be installed and executable by this
     * instance.
     *
     * @return
     */
    public String createSVG() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        createSVG(os);

        return os.toString();
    }




    /**
     * Main for debugging...
     * @param args
     */
    public static void main(String[] args) {

        String fileName = "/mnt/data/uniNOBACKUP/PIA/webpia/data/20130128112250695-Titintest1.pia.xml";

        try {
            PIAModeller modeller = new PIAModeller(fileName);

            Group group = modeller.getGroups().values().iterator().next();
            PIAtoSVG piaToSVG = new PIAtoSVG(group, "PIAGraph");

            LOGGER.info(piaToSVG.createSVG());
        } catch (Exception e) {
            LOGGER.error("something went wrong!", e);
        }
    }
}
