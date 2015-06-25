package de.mpc.pia.intermediate.piaxml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * Model for score in a spectrumMatch
 * 
 * @author julian
 * 
 */
@XmlRootElement(name = "Score")
public class ScoreXML {
	private String cvAccession;
	
	private String name;
	
	private Double value;
	
	
	/**
	 * Gets the value of the cvAccession property.
	 * @return
	 */
	@XmlAttribute(required = true)
	public String getCvAccession() {
		return cvAccession;
	}
	
	
	/**
	 * Sets the value of the cvAccession property.
	 * @param fileName
	 */
	public void setCvAccession(String cvAccession) {
		this.cvAccession = cvAccession;
	}
	
	
	/**
	 * Gets the value of the cvName property.
	 * @return
	 */
	@XmlAttribute(required = true)
	public String getName() {
		return name;
	}
	
	
	/**
	 * Sets the value of the cvName property.
	 * @param fileName
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	
	/**
	 * Gets the value of the value property.
	 * @return
	 */
	@XmlAttribute(required = true)
	public Double getValue() {
		return value;
	}
	
	
	/**
	 * Sets the value of the value property.
	 * @param fileName
	 */
	public void setValue(Double value) {
		this.value = value;
	}
}
