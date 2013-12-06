package de.mpc.pia.intermediate.piaxml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * Model for score in a spectrumMatch
 * 
 * @author julian
 * 
 */
@XmlRootElement(name = "Modification")
public class ModificationXML {
	private Integer location;
	
	private Double mass;
	
	private String residue;
	
	private String accession;
	
	private String description;
	
	/**
	 * Gets the value of the location property.
	 * @return
	 */
	@XmlAttribute(required = true)
	public Integer getLocation() {
		return location;
	}
	
	
	/**
	 * Sets the value of the location property.
	 * @param fileName
	 */
	public void setLocation(Integer location) {
		this.location = location;
	}
	
	
	/**
	 * Gets the value of the mass property.
	 * @return
	 */
	@XmlAttribute(required = true)
	public Double getMass() {
		return mass;
	}
	
	
	/**
	 * Sets the value of the mass property.
	 * @param fileName
	 */
	public void setMass(Double mass) {
		this.mass = mass;
	}
	
	
	/**
	 * Gets the value of the  residue property.
	 * @return
	 */
	@XmlAttribute(required = true)
	public String getResidue() {
		return residue;
	}
	
	
	/**
	 * Sets the value of the residue property.
	 * @param fileName
	 */
	public void setResidue(String residue) {
		this.residue = residue;
	}
	
	
	/**
	 * Gets the value of the  accession property.
	 * @return
	 */
	@XmlAttribute
	public String getAccession() {
		return accession;
	}
	
	
	/**
	 * Sets the value of the accession property.
	 * @param fileName
	 */
	public void setAccession(String accession) {
		this.accession = accession;
	}
	
	
	/**
	 * Gets the value of the  description property.
	 * @return
	 */
	@XmlAttribute
	public String getDescription() {
		return description;
	}
	
	
	/**
	 * Sets the value of the description property.
	 * @param fileName
	 */
	public void setDescription(String description) {
		this.description = description;
	}
}
