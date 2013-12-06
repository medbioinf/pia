package de.mpc.pia.intermediate.piaxml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "spectrumRef")
public class SpectrumRefXML {
	private Long spectrumRefID;
	
	
	/**
	 * Basic no-arg constructor.
	 */
	public SpectrumRefXML() {
	}
	
	
	/**
	 * Basic no-arg constructor.
	 */
	public SpectrumRefXML(Long spectrumRefID) {
		this.spectrumRefID = spectrumRefID;
	}
	
	
	/**
	 * Gets the value of the spectrumRefID attribute.
	 * @return
	 */
	@XmlAttribute(required = true)
	public Long getSpectrumRefID() {
		return spectrumRefID;
	}
	
	
	/**
	 * Sets the value of the spectrumRefID attribute.
	 * @param id
	 */
	public void setSpectrumRefID(Long spectrumRefID) {
		this.spectrumRefID = spectrumRefID;
	}
}
