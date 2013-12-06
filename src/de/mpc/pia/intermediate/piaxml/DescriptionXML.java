package de.mpc.pia.intermediate.piaxml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;


@XmlRootElement(name = "Description")
public class DescriptionXML {
	private Long fileRefID;
	
	private String value;
	
	
	/**
	 * Gets the value of the searchDatabase_ref attribute.
	 * @return
	 */
	@XmlAttribute(required = true)
	public Long getFileRefID() {
		return fileRefID;
	}
	
	
	/**
	 * Sets the value of the searchDatabase_ref attribute.
	 * @param id
	 */
	public void setFileRefID(Long fileRefID) {
		this.fileRefID = fileRefID;
	}
	
	
	/**
	 * Gets the value of the value attribute.
	 * @return
	 */
	@XmlValue
	public String getValue() {
		return value;
	}
	
	
	/**
	 * Sets the value of the value attribute.
	 */
	public void setValue(String value) {
		this.value = value;
	}
}
