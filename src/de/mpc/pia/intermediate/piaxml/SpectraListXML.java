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
@XmlRootElement(name = "spectraList")
public class SpectraListXML {
	@XmlElement(name = "spectrumMatch")
	List<SpectrumMatchXML> spectra;
	
	
	/**
	 * Gets the value of the spectraList property.
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
	 * {@link PIAInputFileXML }
	 * 
	 * @return
	 */
	public List<SpectrumMatchXML> getSpectraList() {
		if (spectra == null) {
			spectra = new ArrayList<SpectrumMatchXML>();
		}
		return spectra;
	}
}
