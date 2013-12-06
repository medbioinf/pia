package de.mpc.pia.webgui.component;


import java.util.Map;
import java.util.TreeMap;

import javax.faces.component.FacesComponent;

import org.richfaces.component.UIPanel;

import de.mpc.pia.intermediate.Modification;

/**
 * Component class to display the sequence of an accession with the coverage.
 * 
 * @author julian
 *
 */
@FacesComponent("de.mpc.pia.webgui.component.ProteinSequenceWithCoverage")
public class ProteinSequenceWithCoverage extends UIPanel {
	public static final String COMPONENT_TYPE =
			"de.mpc.pia.webgui.component.ProteinSequenceWithCoverage";
	
	enum PropertyKeys {
		sequence,
		coveragemap,
		linewidth
	}
	
	
	/**
	 * Basic constructor
	 */
	public ProteinSequenceWithCoverage() {
		setRendererType(
				"de.mpc.pia.webgui.component.ProteinSequenceWithCoverage");
	}
	
	
	/**
	 * Getter for the sequence attribute
	 * @return
	 */
	public String getSequence() {
		return (String)getStateHelper().eval(PropertyKeys.sequence, null);
	}
	
	
	/**
	 * Setter for the sequence attribute
	 * @param spec
	 */
	public void setSequence(String sequence) {
		getStateHelper().put(PropertyKeys.sequence, sequence);
	}
	
	
	/**
	 * Getter for the coverage map attribute
	 * @return
	 */
	public TreeMap<Integer, Integer> getCoverageMap() {
		return (TreeMap<Integer, Integer>)getStateHelper().eval(PropertyKeys.coveragemap, null);
	}
	
	
	/**
	 * Setter for the coverage map  attribute
	 * @param spec
	 */
	public void setModifications(Map<Integer, Modification> map) {
		getStateHelper().put(PropertyKeys.coveragemap, map);
	}
	
	
	/**
	 * Getter for the linewidth attribute
	 * @return
	 */
	public Long getLineWidth() {
		return (Long)getStateHelper().eval(PropertyKeys.linewidth, 120L);
	}
	
	
	/**
	 * Setter for the sequence attribute
	 * @param spec
	 */
	public void setLineWidth(Long width) {
		getStateHelper().put(PropertyKeys.linewidth, width);
	}
}
