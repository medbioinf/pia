package de.mpc.pia.visualization.graph;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.map.LazyMap;

import de.mpc.pia.intermediate.Accession;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Graph;


/**
 * A layout for the visualization of tree-like structures used in the PIA
 * intermediate structure.
 * <p>
 * This layout should only be used for graphs generated for the visualization of
 * these structures. Its behaviour on other data is unknown (and might not work
 * at all).
 * <p>
 * This code was adepted from the TreeLayout class in Jung2 from Karlheinz Toni
 * and Tom Nelson
 *
 * @author julian
 */
public class ProteinLayout<E> implements Layout<VertexObject, E> {

    /** the graph for this layout */
    protected DirectedGraph<VertexObject, E> graph;

    /** dimensions of the layout */
    protected Dimension size;

    /** locations of the VertexObjects */
    protected Map<VertexObject, Point2D> locations = LazyMap.decorate(
            new HashMap<VertexObject, Point2D>(), new Transformer<VertexObject, Point2D>() {
                @Override
                public Point2D transform(VertexObject vertex) {
                    return new Point2D.Double();
                }
            });

    /** The default horizontal vertex spacing. Initialized to 50. */
    private static final int DEFAULT_DISTX = 100;

    /** The default vertical vertex spacing. Initialized to 50. */
    private static final int DEFAULT_DISTY = 75;

    /** The horizontal vertex spacing. Defaults to {@code DEFAULT_XDIST}. */
    private int distX = DEFAULT_DISTX;

    /** The vertical vertex spacing. Defaults to {@code DEFAULT_YDIST}. */
    private int distY = DEFAULT_DISTY;

    /** label for the pseudo root node */
    private static final String PSEUDO_LABEL = "___pseudoVertex___";


    /** the overall maximal height of the vertices */
    private Integer maxHeight;

    /** the overall maximal y-grid position of the vertices */
    private Integer maxWidth;

    /** the minimal height needed for the vertices (for alreadyDone vertices: the set height) */
    private Map<VertexObject, Integer> minimalHeight;

    /** the vertices, which arwe already positioned */
    private Set<VertexObject> alreadyDone;

    /** te sizes of the trees underneath this vertex */
    private Map<VertexObject, Integer> treeSize;



    /**
     * Creates an instance for the specified graph with default X and Y
     * distances.
     */
    public ProteinLayout(DirectedGraph<VertexObject, E> g) {
        this(g, DEFAULT_DISTX, DEFAULT_DISTY);
    }


    /**
     * Creates an instance for the specified graph and X distance with default Y
     * distance.
     */
    public ProteinLayout(DirectedGraph<VertexObject, E> g, int distx) {
        this(g, distx, DEFAULT_DISTY);
    }


    /**
     * Creates an instance for the specified graph, X distance, and Y distance.
     */
    public ProteinLayout(DirectedGraph<VertexObject, E> g, int distx, int disty) {
        if (g == null) {
            throw new IllegalArgumentException("Graph must be non-null");
        }
        if (distx < 1 || disty < 1) {
            throw new IllegalArgumentException("X and Y distances must each be positive");
        }

        this.size = new Dimension(600, 600);

        this.graph = g;
        this.distX = distx;
        this.distY = disty;

        buildTree();
    }


    /**
     * Build the tree-like layout
     */
    private void buildTree() {
        this.minimalHeight = new HashMap<VertexObject, Integer>(graph.getVertexCount());
        this.alreadyDone = new HashSet<VertexObject>(graph.getVertexCount());
        this.treeSize = new HashMap<VertexObject, Integer>(graph.getVertexCount());
        maxHeight = 0;
        maxWidth = 0;

        // list which maps from the depth of the protein-vertices to the vertex
        TreeMap<Integer, List<VertexObject>> proteinObjects = new TreeMap<Integer, List<VertexObject>>();

        for (VertexObject vertex : graph.getVertices()) {
            if ((vertex.getObject() instanceof Accession) ||
                    ((vertex.getObject() instanceof Collection) &&
                            (((Collection<?>)vertex.getObject()).iterator().next() instanceof Accession))) {
                int depth = calculateDeepestWay(vertex);

                if (!proteinObjects.containsKey(depth)) {
                    proteinObjects.put(depth, new ArrayList<VertexObject>());
                }
                proteinObjects.get(depth).add(vertex);

                if (depth > maxHeight) {
                    maxHeight = depth;
                }
            }
        }

        // create the "pseudo root node" with list of protein-vertices as object
        List<VertexObject> proteins = new ArrayList<VertexObject>();

        for (Map.Entry<Integer, List<VertexObject>> verticesIt : proteinObjects.descendingMap().entrySet()) {
            for (VertexObject vertex : verticesIt.getValue()) {
                int depth = verticesIt.getKey();
                while (depth < maxHeight) {
                    // add pseudo-vertices until the deepest depth is reached
                    List<VertexObject> interProteins = new ArrayList<VertexObject>();
                    interProteins.add(vertex);
                    VertexObject interVertex = new VertexObject(PSEUDO_LABEL, interProteins);

                    vertex = interVertex;
                    depth++;
                    minimalHeight.put(vertex, depth);
                }

                proteins.add(vertex);
            }
        }
        VertexObject root = new VertexObject(PSEUDO_LABEL, proteins);
        minimalHeight.put(root, ++maxHeight);

        calculateDimensionX(root);
        buildTree(root, this.treeSize.get(root) / 2 + this.distX);

        // finally update the size
        size = new Dimension(maxWidth + distX, maxHeight * distY);
    }


    /**
     * calculate positions for the tree underneath the given vertex, including
     * the vertex
     *
     * @param vertex
     * @param xPos
     */
    private void buildTree(VertexObject vertex, int xPos) {
        if (!alreadyDone.contains(vertex)) {
            // go one level further down
            updateMinimalHeight(vertex);

            int yPos = this.distY * (maxHeight - minimalHeight.get(vertex));
            maxWidth = Math.max(maxWidth, xPos);

            locations.get(vertex).setLocation(new Point(xPos, yPos));

            alreadyDone.add(vertex);

            int sizeXofCurrent = treeSize.get(vertex);

            int lastX = xPos - sizeXofCurrent / 2;

            int sizeXofChild;
            int startXofChild;
            if (!PSEUDO_LABEL.equals(vertex.getLabel())) {
                for (VertexObject element : graph.getSuccessors(vertex)) {
                    calculateDimensionX(element);

                    sizeXofChild = this.treeSize.get(element);
                    startXofChild = lastX + sizeXofChild / 2;
                    buildTree(element, startXofChild);
                    lastX = lastX + sizeXofChild + distX;
                }
            } else {
                for (Object proteinO : (List<?>)vertex.getObject()) {
                    VertexObject protein = (VertexObject)proteinO;
                    calculateDimensionX(protein);

                    sizeXofChild = this.treeSize.get(protein);
                    startXofChild = lastX + sizeXofChild / 2;
                    buildTree(protein, startXofChild);
                    lastX = lastX + sizeXofChild + distX;
                }
            }
        }
    }


    /**
     * Calculates the deepest possible route from the vertex to another vertex
     * (which will be a peptide or PSM) and puts the depth in the given map
     */
    private int calculateDeepestWay(VertexObject vertex) {
        if (minimalHeight.containsKey(vertex)) {
            return minimalHeight.get(vertex);
        }

        int deepest = 0;
        for (VertexObject outBound : graph.getSuccessors(vertex)) {
            int depth = calculateDeepestWay(outBound);
            if (depth > deepest) {
                deepest = depth;
            }
        }

        deepest++;
        minimalHeight.put(vertex, deepest);
        return deepest;
    }


    /**
     * Checks whether a child has a heigher depth and updates the minimal depth
     * of the vertex accordingly
     *
     * @param vertex
     */
    private void updateMinimalHeight(VertexObject vertex) {
        if (alreadyDone.contains(vertex)) {
            return;
        }

        int height = minimalHeight.get(vertex);

        // look one up and set height to only one deeper, if the predecessor is already done
        if (graph.getPredecessors(vertex) != null) {
            for (VertexObject predecessor : graph.getPredecessors(vertex)) {
                if (alreadyDone.contains(predecessor)
                        &&  (height < minimalHeight.get(predecessor) - 1)) {
                    height = minimalHeight.get(predecessor) - 1;
                }
            }
        }

        // now update the successors, if it is not already done
        if (graph.getSuccessors(vertex) != null) {
            for (VertexObject successor : graph.getSuccessors(vertex)) {
                updateMinimalHeight(successor);

                // set height to be at least one above the highest successor
                if (height < minimalHeight.get(successor) + 1) {
                    height = minimalHeight.get(successor) + 1;
                }
            }
        }

        minimalHeight.put(vertex, height);
    }


    /**
     * calculates the size needed to show any direct connected proteins and the
     * graph beneath the given vertex
     */
    private int calculateDimensionX(VertexObject vertex) {
        int calcSize = 0;

        if (!PSEUDO_LABEL.equals(vertex.getLabel())) {
            for (VertexObject child : graph.getSuccessors(vertex)) {
                if (!alreadyDone.contains(child)) {
                    calcSize += calculateDimensionX(child) + distX;
                }
            }
        } else {
            for (Object proteinO : (List<?>)vertex.getObject()) {
                calcSize += calculateDimensionX((VertexObject)proteinO) + distX;
            }
        }

        calcSize = Math.max(0, calcSize - distX);
        treeSize.put(vertex, calcSize);

        return calcSize;
    }


    /**
     * This method is not supported by this class. The size of the layout is
     * determined by the topology of the graph, and by the horizontal and
     * vertical spacing (optionally set by the constructor).
     */
    @Override
    public void setSize(Dimension size) {
        throw new UnsupportedOperationException("Size of ProteinLayout is set by vertex spacing in constructor");
    }


    @Override
    public Graph<VertexObject, E> getGraph() {
        return graph;
    }

    @Override
    public Dimension getSize() {
        return size;
    }

    @Override
    public void initialize() {
        buildTree();
    }

    @Override
    public boolean isLocked(VertexObject v) {
        return false;
    }

    @Override
    public void lock(VertexObject v, boolean state) {
        // not yet implemented and not needed
    }

    @Override
    public void reset() {
        // not yet implemented and not needed
    }

    @Override
    public void setGraph(Graph<VertexObject, E> graph) {
        if (graph instanceof DirectedGraph) {
            this.graph = (DirectedGraph<VertexObject, E>)graph;
            buildTree();
        } else {
            throw new IllegalArgumentException("graph must be a DirectedGraph");
        }
    }

    @Override
    public void setInitializer(Transformer<VertexObject, Point2D> initializer) {
        // not yet implemented and not needed
    }

    /**
     * Returns the center of this layout's area.
     */
    public Point2D getCenter() {
        return new Point2D.Double(size.getWidth() / 2, size.getHeight() / 2);
    }

    @Override
    public void setLocation(VertexObject v, Point2D location) {
        locations.get(v).setLocation(location);
    }

    @Override
    public Point2D transform(VertexObject v) {
        return locations.get(v);
    }
}
