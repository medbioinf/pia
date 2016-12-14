package de.mpc.pia.intermediate.piaxml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "accessionRef")
public class AccessionRefXML {
	private Long accRefID;
	
	
	/**
	 * Basic no-arg constructor.
	 */
	public AccessionRefXML() {
	}
	
	
	/**
	 * Basic no-arg constructor.
	 */
	public AccessionRefXML(Long accRefID) {
		this.accRefID = accRefID;
	}
	
	
	/**
	 * Gets the value of the accRefID attribute.
	 * @return
	 */
	@XmlAttribute(required = true)
	public Long getAccRefID() {
		return accRefID;
	}
	
	
	/**
	 * Sets the value of the accRefID attribute.
	 */
	public void setAccRefID(Long accRefID) {
		this.accRefID = accRefID;
	}
}
