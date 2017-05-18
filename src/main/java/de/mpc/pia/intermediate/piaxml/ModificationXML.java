package de.mpc.pia.intermediate.piaxml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;


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

	private String cvLabel;

    @XmlElement(name = "Score")
	private List<ScoreXML> probability;
	
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
	 */
	public void setDescription(String description) {
		this.description = description;
	}


	public List<ScoreXML> getProbability() {
		return this.probability;
	}

	@XmlAttribute
	public String getCvLabel() {
		return cvLabel;
	}

	public void setCvLabel(String cvLabel) {
		this.cvLabel = cvLabel;
	}

	public void setProbabilities(List<ScoreXML> probabilities) {
		if(probabilities != null && probabilities.size() > 0){
			this.probability = new ArrayList<>();
			for(ScoreXML score: probabilities)
				this.probability.add(score);
		}
	}
}
