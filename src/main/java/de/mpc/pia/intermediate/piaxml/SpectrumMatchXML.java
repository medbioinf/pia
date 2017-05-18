package de.mpc.pia.intermediate.piaxml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import uk.ac.ebi.jmzidml.model.mzidml.AbstractParam;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.UserParam;

import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.modeller.score.ScoreModel;


/**
 * Model for file in a PIA intermediate file.<br/>
 * Wraps around a {@link PIAInputFile}
 * 
 * @author julian
 * 
 */
@XmlRootElement(name = "spectrumMatch")
//defines the the order in which the fields are written
@XmlType(propOrder = {
		"sourceID",
		"title",
		"scores",
		"modifications",
		"paramList"
	})
public class SpectrumMatchXML {
	/** id of the {@link PeptideSpectrumMatch} element */
	private Long id;
	
	/** charge of the spectrum */
	private int charge;
	
	/** measured experimental m/z of the PSM (for positive charge: mass+(z*mass_H+) / z) */
	private double massToCharge;
	
	/** delta of mass between measured and theoretical value (measured - theoretical) */
	private double deltaMass;
	
	/** retention time (in seconds), null, if not set */
	private Double retentionTime;
	
	/** sequence of the peptide spectrum match */
	private String sequence;
	
	/** missed cleavages of the spectrum match */
	private int missed;
	
	/** the original ID of the spectrum in the file (as in mzIdentML) */
	private String sourceID;
	
	/** pointer to the file, where this PSM comes from */
	private Long fileRef;
	
	/** the SpectrumIdentification (as in mzIdentML), actually a reference */
	private String spectrumIdentificationRef;
	
	/** whether this PSM is unique (in this dataset) for its protein (i.e. found only in one protein / accession) */
	private Boolean isUnique;
	
	/** whether this PSM is a decoy, only set by some search engines */
	private Boolean isDecoy;
	
	
	/** the original title of the spectrum in the file */
	private String title;
	
	/** the scores of this PSM */
	@XmlElement(name = "Score")
	private List<ScoreXML> scores;
	
	/** the modifications in this PSM */
	@XmlElement(name = "Modification")
	private List<ModificationXML> modifications;
	
	/** the cvParams and userParams of the spectrum (except the ones identified as scores) */
	@XmlElements({
		@XmlElement(name = "cvParam", type = CvParam.class),
		@XmlElement(name = "userParam", type = UserParam.class)
	})
	private List<AbstractParam> paramList;
	
	
	/**
	 * Basic no-arg constructor.
	 */
	public SpectrumMatchXML() {
	}
	
	
	/**
	 * Basic constructor setting the data from the given PSM.
	 * @param psm
	 */
	public SpectrumMatchXML(PeptideSpectrumMatch psm) {
		this.id = psm.getID();
		this.charge = psm.getCharge();
		this.massToCharge = psm.getMassToCharge();
		this.deltaMass = psm.getDeltaMass();
		this.retentionTime = psm.getRetentionTime();
		this.sequence = psm.getSequence();
		this.missed = psm.getMissedCleavages();
		this.sourceID = psm.getSourceID();
		if (psm.getFile() != null) {
			this.fileRef = psm.getFile().getID();
		} else {
			// TODO: throw an error
			this.fileRef = null;
		}
		if (psm.getSpectrumIdentification() != null) {
			this.spectrumIdentificationRef = psm.getSpectrumIdentification().getId();
		} else {
			// TODO: throw an error
			this.spectrumIdentificationRef = null;
		}
		this.isUnique = psm.getIsUnique();
		this.isDecoy = psm.getIsDecoy();
		this.title = psm.getSpectrumTitle();
		
		scores = new ArrayList<>(psm.getScores().size());
		for (ScoreModel score : psm.getScores()) {
			ScoreXML scoreXML = new ScoreXML();
			scoreXML.setCvAccession(score.getAccession());
			scoreXML.setName(score.getName());
			scoreXML.setValue(score.getValue());
			scores.add(scoreXML);
		}
		
		modifications = new ArrayList<>(psm.getModifications().size());
		for (Map.Entry<Integer, Modification> modIt : psm.getModifications().entrySet()) {
			ModificationXML modXML = new ModificationXML();
			modXML.setLocation(modIt.getKey());
			modXML.setMass(modIt.getValue().getMass());
			modXML.setResidue(modIt.getValue().getResidue().toString());
			modXML.setAccession(modIt.getValue().getAccession());
			modXML.setDescription(modIt.getValue().getDescription());
			modXML.setCvLabel(modIt.getValue().getCvLabel());
			modXML.setProbabilities(modIt.getValue().getProbability());
			modifications.add(modXML);
		}
		
		this.paramList = psm.getParams();
	}
	
	
	/**
	 * Gets the value of the id attribute.
	 * @return
	 */
	@XmlAttribute(required = true)
	public Long getId() {
		return id;
	}
	
	
	/**
	 * Sets the value of the id attribute.
	 * @param id
	 */
	public void setId(Long id) {
		this.id = id;
	}
	
	
	/**
	 * Gets the value of the charge property.
	 * @return
	 */
	@XmlAttribute(required = true)
	public int getCharge() {
		return charge;
	}
	
	
	/**
	 * Sets the value of the charge property.
	 */
	public void setCharge(int charge) {
		this.charge = charge;
	}
	
	
	/**
	 * Gets the value of the massToCharge property.
	 * @return
	 */
	@XmlAttribute(required = true)
	public double getMassToCharge() {
		return massToCharge;
	}
	
	
	/**
	 * Sets the value of the massToCharge property.
	 */
	public void setMassToCharge(double massToCharge) {
		this.massToCharge = massToCharge;
	}
	
	
	/**
	 * Gets the value of the deltaMass property.
	 * @return
	 */
	@XmlAttribute
	public double getDeltaMass() {
		return deltaMass;
	}
	
	
	/**
	 * Sets the value of the deltaMass property.
	 */
	public void setDeltaMass(double deltaMass) {
		this.deltaMass = deltaMass;
	}
	
	
	/**
	 * Gets the value of the retentionTime property.
	 * @return
	 */
	@XmlAttribute
	public Double getRetentionTime() {
		return retentionTime;
	}
	
	
	/**
	 * Sets the value of the retentionTime property.
	 */
	public void setRetentionTime(Double retentionTime) {
		this.retentionTime = retentionTime;
	}
	
	
	/**
	 * Gets the value of the sequence property.
	 * @return
	 */
	@XmlAttribute(required = true)
	public String getSequence() {
		return sequence;
	}
	
	
	/**
	 * Sets the value of the sequence property.
	 */
	public void setSequence(String sequence) {
		this.sequence = sequence;
	}
	
	
	/**
	 * Gets the value of the missed property.
	 * @return
	 */
	@XmlAttribute(required = true)
	public int getMissed() {
		return missed;
	}
	
	
	/**
	 * Sets the value of the missed property.
	 */
	public void setMissed(int missed) {
		this.missed = missed;
	}
	
	
	/**
	 * Gets the value of the sourceID property.
	 * @return
	 */
	public String getSourceID() {
		return sourceID;
	}
	
	
	/**
	 * Sets the value of the sourceID property.
	 */
	public void setSourceID(String sourceID) {
		this.sourceID = sourceID;
	}
	
	
	/**
	 * Gets the value of the fileRefID property.
	 * @return
	 */
	@XmlAttribute(required = true)
	public Long getFileRef() {
		return fileRef;
	}
	
	
	/**
	 * Sets the value of the fileRefID property.
	 */
	public void setFileRef(Long fileRef) {
		this.fileRef = fileRef;
	}

	
	/**
	 * Gets the value of the spectrumIdentificationRef property.
	 * @return
	 */
	@XmlAttribute(required = true)
	public String getSpectrumIdentificationRef() {
		return spectrumIdentificationRef;
	}
	
	
	/**
	 * Sets the value of the spectrumIdentificationRef property.
	 */
	public void setSpectrumIdentificationRef(String spectrumIdentificationRef) {
		this.spectrumIdentificationRef = spectrumIdentificationRef;
	}
	
	
	/**
	 * Gets the value of the isUnique property.
	 * @return
	 */
	@XmlAttribute
	public Boolean getIsUnique() {
		return isUnique;
	}
	
	
	/**
	 * Sets the value of the isUnique property.
	 */
	public void setIsUnique(Boolean isUnique) {
		this.isUnique = isUnique;
	}
	
	
	/**
	 * Gets the value of the isDecoy property.
	 * @return
	 */
	@XmlAttribute
	public Boolean getIsDecoy() {
		return isDecoy;
	}
	
	
	/**
	 * Sets the value of the isDecoy property.
	 */
	public void setIsDecoy(Boolean isDecoy) {
		this.isDecoy = isDecoy;
	}
	
	
	/**
	 * Gets the value of the spectrumTitle property.
	 * @return
	 */
	@XmlElement(name = "Title")
	public String getTitle() {
		return title;
	}
	
	
	/**
	 * Sets the value of the spectrumTitle property.
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	
	
	/**
	 * Gets the List of scores.
	 * @return
	 */
	public List<ScoreXML> getScores() {
		if (scores == null) {
			scores = new ArrayList<>();
		}
		return scores;
	}
	
	
	/**
	 * Gets the List of modification.
	 * @return
	 */
	public List<ModificationXML> getModification() {
		if (modifications == null) {
			modifications = new ArrayList<>();
		}
		return modifications;
	}
	
	
	/**
	 * Gets the List of cvParams and userParams (except scores and
	 * modifications).
	 * @return
	 */
	public List<AbstractParam> getParamList() {
		if (paramList == null) {
			paramList = new ArrayList<>();
		}
		return paramList;
	}
}
