package de.mpc.pia.intermediate.piaxml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * Model for spectraList in a PIA intermediate file.
 * 
 * @author julian
 *
 */
@XmlRootElement(name = "spectrumRefList")
public class SpectrumRefListXML {
	@XmlElement(name = "spectrumRef")
	List<SpectrumRefXML> spectrumRefs;
	
	
	/**
	 * Gets the value of the spectrumRefs property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list,
	 * not a snapshot. Therefore any modification you make to the
	 * returned list will be present inside the JAXB object.
	 * This is why there is not a <CODE>set</CODE> method for the spectrumIdentification property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * <pre>
	 *    getFilesList().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list
	 * {@link SpectrumRefXML }
	 * 
	 * @return
	 */
	public List<SpectrumRefXML> getSpectrumRefs() {
		if (spectrumRefs == null) {
			spectrumRefs = new ArrayList<SpectrumRefXML>();
		}
		return spectrumRefs;
	}
}
