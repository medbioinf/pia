package de.mpc.pia.intermediate.piaxml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "peptideRef")
public class PeptideRefXML {
	private Long pepRefID;
	
	
	/**
	 * Basic no-arg constructor.
	 */
	public PeptideRefXML() {
	}
	
	
	/**
	 * Basic no-arg constructor.
	 */
	public PeptideRefXML(Long pepRefID) {
		this.pepRefID = pepRefID;
	}
	
	
	/**
	 * Gets the value of the pepRefID attribute.
	 * @return
	 */
	@XmlAttribute(required = true)
	public Long getPepRefID() {
		return pepRefID;
	}
	
	
	/**
	 * Sets the value of the pepRefID attribute.
	 * @param id
	 */
	public void setPepRefID(Long pepRefID) {
		this.pepRefID = pepRefID;
	}
}
