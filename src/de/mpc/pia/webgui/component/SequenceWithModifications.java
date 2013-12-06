package de.mpc.pia.webgui.component;


import java.util.Map;

import javax.faces.component.FacesComponent;

import org.richfaces.component.UIPanel;

import de.mpc.pia.intermediate.Modification;

/**
 * Component class to display the sequence of a spectrum.
 * 
 * @author julian
 *
 */
@FacesComponent("de.mpc.pia.webgui.component.SequenceWithModifications")
public class SequenceWithModifications extends UIPanel {
	public static final String COMPONENT_TYPE = "de.mpc.pia.webgui.component.SequenceWithModifications";
	
	enum PropertyKeys {
		sequence,
		modifications
	}
	
	
	/**
	 * Basic constructor
	 */
	public SequenceWithModifications() {
		setRendererType("de.mpc.pia.webgui.component.SequenceWithModifications");
	}
	
	
	/**
	 * Getter for the spectrum attribute
	 * @return
	 */
	public String getSequence() {
		return (String)getStateHelper().eval(PropertyKeys.sequence, null);
	}
	
	
	/**
	 * Setter for the spectrum attribute
	 * @param spec
	 */
	public void setSequence(String sequence) {
		getStateHelper().put(PropertyKeys.sequence, sequence);
	}
	
	
	/**
	 * Getter for the spectrum attribute
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Map<Integer, Modification> getModifications() {
		return (Map<Integer, Modification>)getStateHelper().eval(PropertyKeys.modifications, null);
	}
	
	
	/**
	 * Setter for the spectrum attribute
	 * @param spec
	 */
	public void setModifications(Map<Integer, Modification> modifications) {
		getStateHelper().put(PropertyKeys.modifications, modifications);
	}
}
