package de.mpc.pia.visualization.graph;


/**
 * This class holds the information of a vertex
 * 
 * @author julianu
 *
 */
public class VertexObject {
	
	/** The label of this vertex (not necessary the shown label) */
	private String label;
	
	/** the actual object represented by this vertex */
	private Object object;
	
	
	
	public VertexObject(String label, Object object) {
		this.label = label;
		this.object = object;
	}
	
	
	/**
	 * getter for the label
	 * @return
	 */
	public String getLabel() {
		return label;
	}
	
	
	/**
	 * getter for the actual object which is represented by this vertex
	 * @return
	 */
	public Object getObject() {
		return object;
	}
	
	
	@Override
	public String toString() {
		return label;
	}
}
