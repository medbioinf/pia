package de.mpc.pia.intermediate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.ebi.jmzidml.model.mzidml.AbstractParam;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;

import de.mpc.pia.modeller.IdentificationKeySettings;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.tools.PIATools;


/**
 * This class represents a peptide spectrum match (PSM).
 * 
 * @author julian
 * 
 */
public class PeptideSpectrumMatch implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/** internal ID of the PSM */
	private long ID;
	
	/** charge of the spectrum */
	private int charge;
	
	/** measured experimental m/z of the PSM (for positive charge: mass+(z*mass_H+) / z) */
	private double massToCharge;
	
	/** delta of mass (NOT m/z) between measured and theoretical value (measured - theoretical) */
	private double deltaMass;
	
	/** retention time (in seconds), null, if not set */
	private Double retentionTime;
	
	/** sequence of the peptide spectrum match */
	private String sequence;
	
	/** missed cleavages of the spectrum match */
	private int missed;
	
	/** the original ID of the spectrum in the file (as in mzIdentML) */
	private String sourceID;
	
	/** the original title of the spectrum in the file */
	private String spectrumTitle;
	
	/** pointer to the file, where this PSM comes from */
	private PIAInputFile pFile;
	
	/** the SpectrumIdentification (as in mzIdentML), actually a reference */
	private SpectrumIdentification spectrumID;
	
	/** whether this PSM is unique (in this dataset) for its protein (i.e. found only in one protein / accession) */
	private Boolean isUnique;
	
	/** whether this PSM is a decoy, only set by some search engines */
	private Boolean isDecoy;
	
	/** the scores of this PSM */
	private List<ScoreModel> scores;
	
	/** the modifications in this PSM (consistent iteration is needed, therefore use of {@link TreeMap})*/
	private TreeMap<Integer, Modification> modifications;
	
	/** the cvParams and userParams of the spectrum (except the ones identified as scores) */
	private List<AbstractParam> paramList;
	
	/** explanation of the modifications as a String */
	private String modificationString;
	
	/** the peptide ID string (with modifications, without would be just the sequence) */
	private String peptideStringID;
	
	/** are the modifications changed since last building the modification string and peptideIDString */
	private boolean modificationChanged;
	
	/** caches the identification keys */
	private Map<String, String> identificationKeys;

	private double theoreticalMz;

	/** the finished peptide, only used after reading in a PIA file */
	private Peptide peptide;
	
	
	/**
	 * Basic constructor
	 */
	public PeptideSpectrumMatch(long id, int charge, double massToCharge,
			double deltaMass, Double rt, String sequence, int missed,
			String sourceID, String title, PIAInputFile file,
			SpectrumIdentification spectrumID) {
		this.ID = id;
		this.charge = charge;
		this.massToCharge = massToCharge;
		this.deltaMass = deltaMass;
		this.retentionTime = rt;
		this.sequence = sequence;
		this.missed = missed;
		this.sourceID = sourceID;
		this.spectrumTitle = title;
		this.pFile = file;
		this.spectrumID = spectrumID;
		this.isUnique = null;
		this.isDecoy = null;
		
		this.scores = new ArrayList<ScoreModel>();
		this.modifications = new TreeMap<Integer, Modification>();
		this.paramList = new ArrayList<AbstractParam>();
		this.modificationChanged = true;
		this.identificationKeys = new HashMap<String, String>(2);
		
		this.peptide = null;
	}

	/**
	 * A new property has been added
	 */
	public PeptideSpectrumMatch(long id, int charge, double massToCharge, double theoreticalMz,
								double deltaMass, Double rt, String sequence, int missed,
								String sourceID, String title, PIAInputFile file,
								SpectrumIdentification spectrumID) {
		this.ID = id;
		this.charge = charge;
		this.massToCharge = massToCharge;
		this.deltaMass = deltaMass;
		this.retentionTime = rt;
		this.sequence = sequence;
		this.missed = missed;
		this.sourceID = sourceID;
		this.spectrumTitle = title;
		this.pFile = file;
		this.spectrumID = spectrumID;
		this.isUnique = null;
		this.isDecoy = null;

		this.scores = new ArrayList<ScoreModel>();
		this.modifications = new TreeMap<Integer, Modification>();
		this.paramList = new ArrayList<AbstractParam>();
		this.modificationChanged = true;
		this.identificationKeys = new HashMap<String, String>(2);
		this.theoreticalMz = theoreticalMz;

		this.peptide = null;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if ( !(obj instanceof PeptideSpectrumMatch) ) {
			return false;
		}
		
		PeptideSpectrumMatch objSpectrum = (PeptideSpectrumMatch)obj;
	    if ((objSpectrum.ID == this.ID) &&
	    		(objSpectrum.scores == this.scores) &&
	    		(objSpectrum.charge == this.charge) &&
	    		(objSpectrum.massToCharge == this.massToCharge) &&
	    		(objSpectrum.deltaMass == this.deltaMass) &&
	    		(((retentionTime == null) && (objSpectrum.retentionTime == null)) ||
	    				retentionTime.equals(objSpectrum.retentionTime)) &&
	    		(objSpectrum.missed == this.missed) &&
	    		objSpectrum.sequence.equals(sequence) &&
	    		objSpectrum.modifications.equals(modifications) &&
	    		objSpectrum.sourceID.equals(sourceID) &&
	    		(((spectrumTitle != null) && spectrumTitle.equals(objSpectrum.spectrumTitle)) ||
	    				((spectrumTitle == null) && (objSpectrum.spectrumTitle == null))) &&
	    		(((pFile != null) && pFile.equals(objSpectrum.pFile)) ||
	    				((pFile == null) && (objSpectrum.pFile == null))) &&
	    		(((spectrumID != null) && spectrumID.equals(objSpectrum.spectrumID)) ||
	    				((spectrumID == null) && (objSpectrum.spectrumID == null))) &&
   	 	    	(((isUnique == null) && (objSpectrum.isUnique == null)) ||
   	 	    			isUnique.equals(objSpectrum.isUnique)) &&
	    		(((isDecoy == null) && (objSpectrum.isDecoy == null)) ||
	    				isDecoy.equals(objSpectrum.isDecoy))
	    		) {
	    	return true;
	    } else {
	    	return false;
	    }
	}
	
	
	@Override
	public int hashCode() {
		int hash = 0;
		
		hash += (new Long(ID)).hashCode();
		hash += (scores != null) ? scores.hashCode() : 0;
		hash += (new Integer(charge)).hashCode();
		hash += (new Double(massToCharge)).hashCode();
		hash += (new Double(deltaMass)).hashCode();
		hash += (retentionTime != null) ? retentionTime.hashCode() : 0;
		hash += (new Integer(missed)).hashCode();
		hash += (sequence != null) ? sequence.hashCode() : 0;
		hash += (modifications != null) ? modifications.hashCode() : 0;
		hash += (sourceID != null) ? sourceID.hashCode() : 0;
		hash += (spectrumTitle != null) ? spectrumTitle.hashCode() : 0;
		hash += (pFile != null) ? pFile.hashCode() : 0;
		hash += (isUnique != null) ? isUnique.hashCode() : 0;
		hash += (spectrumID != null) ? spectrumID.hashCode() : 0;
		
		return hash;
	}
	
	
	/**
	 * Sets the pFile to the given file.
	 * 
	 * @param file
	 */
	public void setFile(PIAInputFile file) {
		pFile = file;
	}
	
	
	/**
	 * Getter for the ID.
	 * 
	 * @return
	 */
	public Long getID() {
		return ID;
	}
	
	
	/**
	 * Adds a score to the list of scores.
	 * 
	 * @param score
	 */
	public void addScore(ScoreModel score) {
		scores.add(score);
	}
	
	
	/**
	 * Getter for the scores.
	 * 
	 * @return
	 */
	public List<ScoreModel> getScores() {
		return scores;
	}
	
	
	/**
	 * returns the score given by scoreName or null, if none is found
	 * 
	 * @return
	 */
	public ScoreModel getScore(String scoreName) {
		for (ScoreModel score : scores) {
			if (!score.getType().equals(ScoreModelEnum.UNKNOWN_SCORE)) {
				if (score.getType().isValidDescriptor(scoreName)) {
					return score;
				}
			} else {
				if (score.getShortName().equals(scoreName)) {
					return score;
				}
			}
		}
		return null;
	}
	
	
	/**
	 * Getter for the peptide mass.
	 * 
	 * @return
	 */
	public double getMassToCharge() {
		return massToCharge;
	}
	
	
	/**
	 * Getter for the delta mass.
	 * 
	 * @return
	 */
	public double getDeltaMass() {
		return deltaMass;
	}
	
	
	/**
	 * Getter for the retention time. null, if none is given.
	 * 
	 * @return
	 */
	public Double getRetentionTime() {
		return retentionTime;
	}
	
	
	/**
	 * Getter for the charge.
	 * 
	 * @return
	 */
	public int getCharge() {
		return charge;
	}
	
	
	/**
	 * Getter for the missed cleavages.
	 * 
	 * @return
	 */
	public int getMissedCleavages() {
		return missed;
	}
	
	
	/**
	 * Getter for the sequence.
	 * 
	 * @return
	 */
	public String getSequence() {
		return sequence;
	}
	
	
	/**
	 * Rebuild the modificationString and the peptideIDString after the
	 * modifications were changed.
	 */
	private void rebuildAfterModificationChange() {
		// rebuild the modification string
		modificationString = getModificationString(modifications);
		
		// rebuild the peptideStringID
		StringBuffer modificationSB = new StringBuffer(sequence);
		for (Map.Entry<Integer, Modification> modIt : modifications.entrySet()) {
			modificationSB.append("(");
			modificationSB.append(modIt.getKey() + ";" +
					modIt.getValue().getMassString() );
			modificationSB.append(")");
		}
		peptideStringID = modificationSB.toString();
		
		identificationKeys = new HashMap<String, String>(2);
		
		modificationChanged = false;
	}
	
	
	/**
	 * Returns a String which explains the modifications.
	 * This is NOT a substitute for the real modifications, but only for
	 * building the identification string.
	 * The description contains only the position and mass delta with four
	 * digit precision.
	 * 
	 * @return
	 */
	public String getModificationString() {
		if (modificationChanged) {
			rebuildAfterModificationChange();
		}
		return modificationString;
	}
	
	
	/**
	 * Returns a String which explains the modifications in the Map.
	 * 
	 * @param modifications
	 * @return
	 */
	public static String getModificationString(
			Map<Integer, Modification> modifications) {
		StringBuffer modSb = new StringBuffer();
		boolean first = true;
		TreeMap<Integer, Modification> treeModMap =
				new TreeMap<Integer, Modification>(modifications);
		
		for (Map.Entry<Integer, Modification> modIt : treeModMap.entrySet()) {
			if (!first) {
				modSb.append('|');
			}
			modSb.append("[" + modIt.getKey() + ",");
			
			modSb.append(modIt.getValue().getMassString());
			modSb.append("]");

			first = false;
		}
		
		return modSb.toString();
	}
	
	
	/**
	 * Returns the peptide identification string, with respect of modifications
	 * or without.
	 * 
	 * @param considerModifications
	 * @return
	 */
	public String getPeptideStringID(boolean considerModifications) {
		if (considerModifications) {
			if (modificationChanged) {
				rebuildAfterModificationChange();
			}
			return peptideStringID;
		} else {
			return sequence;
		}
	}
	
	
	/**
	 * adds the given modification for the position
	 * 
	 * @param pos
	 * @param type
	 */
	public void addModification(int pos, Modification mod) {
		modifications.put(pos, mod);
		modificationChanged = true;
	}
	
	
	/**
	 * Getter for the modifications map.
	 * @return
	 */
	public Map<Integer, Modification> getModifications() {
		return modifications;
	}
	
	
	/**
	 * Getter for the sourceID.
	 * 
	 * @return
	 */
	public String getSourceID() {
		return sourceID;
	}
	
	
	/**
	 * Getter for the spectrumTitle.
	 * 
	 * @return
	 */
	public String getSpectrumTitle() {
		return spectrumTitle;
	}
	
	
	/**
	 * This method returns a concatenation of the fileID and sourceID
	 * 
	 * @return
	 */
	public String getFileSourceID() {
		return pFile.getID() + ";" + 
				sourceID;
	}
	
	
	/**
	 * Getter for the file.
	 * 
	 * @return
	 */
	public PIAInputFile getFile() {
		return pFile;
	}
	
	
	/**
	 * getter for isUnique.
	 * @return
	 */
	public Boolean getIsUnique() {
		return isUnique;
	}
	
	
	/**
	 * setter for isUnique.
	 * @return
	 */
	public void setIsUnique(Boolean unique) {
		this.isUnique = unique;
	}
	
	
	/**
	 * getter for isDecoy.
	 * @return
	 */
	public Boolean getIsDecoy() {
		return isDecoy;
	}
	
	
	/**
	 * setter for isDecoy.
	 * @return
	 */
	public void setIsDecoy(Boolean isDecoy) {
		this.isDecoy = isDecoy;
	}
	
	
	/**
	 * Getter for the protocol.
	 * @return
	 */
	public SpectrumIdentification getSpectrumIdentification() {
		return spectrumID;
	}
	
	
	/**
	 * Adds a param to the list of cvParams and userParams.
	 * @param param
	 */
	public void addParam(AbstractParam param) {
		paramList.add(param);
	}
	
	
	/**
	 * Returns the list of  cvParams and userParams.
	 * @return
	 */
	public List<AbstractParam> getParams() {
		return paramList;
	}
	
	
	/**
	 * Returns a string to identify the spectrum. This is an identificationKey
	 * generated by {@link PeptideSpectrumMatch#getIdentificationKey(Map)}
	 * using only the m/z, RT, sourceID, spectrumTitle and charge values, if
	 * they are given in the maximalKeySettings psmSetSettings. The sourceID and
	 * spectrumTitle would be sufficient, but are not always available.
	 * 
	 * @param maximalKeySettings
	 * @return
	 */
	public String getSpectrumIdentificationKey(Map<String, Boolean> maximalKeySettings) {
		Map<String, Boolean> psmSetSettings =
				new HashMap<String, Boolean>(maximalKeySettings);
		
		// remove the SEQUENCE and MODIFICATIONS, they are not needed for spectrumIdentificationKey
		psmSetSettings.remove(IdentificationKeySettings.SEQUENCE.name());
		psmSetSettings.remove(IdentificationKeySettings.MODIFICATIONS.name());
		
		return getIdentificationKey(psmSetSettings);
	}
	
	
	/**
	 * Returns a string for identifying a PSM.<br/>
	 *This should be unique for each PSM and is used to pre-sort PSMs found by
	 * different searches. These have to be refined later, because valzues with 
	 * m/z-values can not be used, for identification, due to search engine
	 * roundings.
	 * 
	 * @return
	 */
	public static String getIdentificationKey(
			Map<String, Boolean> psmSetSettings, String sequence,
			String modificationString, int charge, Double massToCharge,
			Double rt, String sourceID, String spectrumTitle, Long fileID) {
		
		List<String> usedSettings = new ArrayList<String>();
		for (Map.Entry<String, Boolean> setSetting : psmSetSettings.entrySet()) {
			if (setSetting.getValue()) {
				usedSettings.add(setSetting.getKey());
			}
		}
		Collections.sort(usedSettings);
		
		StringBuffer key = new StringBuffer();
		for (String settingName : usedSettings) {
			String value = null;
			switch(IdentificationKeySettings.getByName(settingName)) {
			case CHARGE:
				value = Integer.toString(charge);
				break;
				
			case FILE_ID:
				value = Long.toString(fileID);
				break;
				
			case MASSTOCHARGE:
				if (massToCharge != null) {
					value = Double.toString(PIATools.round(massToCharge, 4));
				}
				break;
				
			case MODIFICATIONS:
				value = modificationString;
				break;
				
			case RETENTION_TIME:
				if (rt != null) {
					value = Double.toString((int)PIATools.round(rt, 0));
				}
				break;
				
			case SEQUENCE:
				value = sequence;
				break;
				
			case SOURCE_ID:
				value = sourceID;
				break;
				
			case SPECTRUM_TITLE:
				value = spectrumTitle;
				break;
			}
			
			if (value != null) {
				if (key.length() > 0) {
					key.append(":");
				}
				
				key.append(value);
			}
		}
		
		return key.toString();
	}
	
	
	/**
	 * Returns a string for identifying a PSM.<br/>
	 * This should be unique for each PSM and is used to pre-sort PSMs found by
	 * different searches. These have to be refined later, because valzues with 
	 * m/z-values can not be used, for identification, due to search engine
	 * roundings.
	 * 
	 * @param psmSetSettings
	 * @return
	 */
	public String getIdentificationKey(Map<String, Boolean> psmSetSettings) {
		List<String> usedSettings = new ArrayList<String>();
		for (Map.Entry<String, Boolean> setSetting : psmSetSettings.entrySet()) {
			if (setSetting.getValue()) {
				usedSettings.add(setSetting.getKey());
			}
		}
		Collections.sort(usedSettings);
		
		StringBuffer keyKey = new StringBuffer();
		
		for (String key : usedSettings) {
			keyKey.append(key);
		}
		
		String key = identificationKeys.get(keyKey.toString());
		
		if (key == null) {
			key = getIdentificationKey(
					psmSetSettings,
					this.getSequence(),
					this.getModificationString(),
					this.getCharge(),
					this.getMassToCharge(),
					this.getRetentionTime(),
					this.getSourceID(),
					this.getSpectrumTitle(),
					this.getFile().getID());
			identificationKeys.put(keyKey.toString(), key);
		}
		
		return key;
	}
	
	
	/**
	 * Returns a nice name / header for this PSM's spectrum
	 * @return
	 */
	public String getNiceSpectrumName() {
		StringBuffer spectrumName = new StringBuffer();
		
		if (sourceID != null) {
			spectrumName.append(sourceID);
		} else {
			spectrumName.append("[no sourceID]");
		}
		
		spectrumName.append(" (");
		spectrumName.append((int)PIATools.round(massToCharge, 4));
		spectrumName.append(", ");
		if (charge > 0) {
			spectrumName.append("+");
		}
		spectrumName.append(charge);
		
		if (retentionTime != null) {
			spectrumName.append(", ");
			spectrumName.append(PIATools.round(retentionTime, 0));
		}
		
		spectrumName.append(")");
		
		return spectrumName.toString();
	}
	
	
	/**
	 * Setter for the peptide, called while parsing the PIA XML file.
	 * @param pep
	 */
	public void setPeptide(Peptide pep) {
		this.peptide = pep;
	}
	
	
	/**
	 * Getter for the peptide. If the peptide is not set while parsing the XML
	 * file, null is returned.
	 * @return
	 */
	public Peptide getPeptide() {
		return peptide;
	}

	/**
	 * Get the theoretical MZ value of an specific sequence
	 * @return
     */
	public double getTheoreticalMz() {
		return theoreticalMz;
	}

	/**
	 * Set the mz value for the PSM
	 * @param theoreticalMz
     */
	public void setTheoreticalMz(double theoreticalMz) {
		this.theoreticalMz = theoreticalMz;
	}
}