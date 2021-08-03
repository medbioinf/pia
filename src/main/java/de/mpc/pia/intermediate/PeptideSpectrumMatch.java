package de.mpc.pia.intermediate;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import uk.ac.ebi.jmzidml.model.mzidml.AbstractParam;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;

import de.mpc.pia.modeller.IdentificationKeySettings;
import de.mpc.pia.modeller.psm.PSMItem;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.tools.PIAConstants;
import de.mpc.pia.tools.PIATools;


/**
 * This class represents a peptide spectrum match (PSM).
 *
 * @author julian
 *
 */
public class PeptideSpectrumMatch implements PSMItem, Serializable {

    private static final long serialVersionUID = -7839921462687609103L;


    /** internal ID of the PSM */
    private long id;

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

    /** the finished peptide, only used after reading in a PIA file */
    private Peptide peptide;

    private String spectraDataRef;

    /**
     * Basic constructor
     */
    public PeptideSpectrumMatch(long id, int charge, double massToCharge,
                                double deltaMass, Double rt, String sequence, int missed,
                                String sourceID, String title, PIAInputFile file,
                                SpectrumIdentification spectrumID, String spectraDataRef) {
        this.id = id;
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

        this.scores = new ArrayList<>();
        this.modifications = new TreeMap<>();
        this.paramList = new ArrayList<>();
        this.modificationChanged = true;
        this.identificationKeys = new HashMap<>(2);

        this.peptide = null;
        this.spectraDataRef = spectraDataRef;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        if (this == obj) return true;

        PeptideSpectrumMatch objSpectrum = (PeptideSpectrumMatch)obj;

        if (id != objSpectrum.id) return false;
        return equalsWithoutID(objSpectrum);
    }


    /**
     * Tests whether this PeptideSpectrumMatch equals the given object without comparing the ID
     *
     * @param obj
     * @return
     */
    public boolean equalsWithoutID(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        if (this == obj) return true;

        PeptideSpectrumMatch objSpectrum = (PeptideSpectrumMatch)obj;

        if (!equalsBasics(objSpectrum)) return false;

        if (!Objects.equals(sourceID, objSpectrum.sourceID)) return false;
        if (!Objects.equals(spectrumTitle, objSpectrum.spectrumTitle)) return false;

        if (!scores.equals(objSpectrum.scores)) return false;
        if (!modifications.equals(objSpectrum.modifications)) return false;

        if (!spectrumID.equals(objSpectrum.spectrumID)) return false;
        return (pFile.equals(objSpectrum.pFile));
    }


    /**
     * Tests whether the basic values of this and the given object are equal.
     *
     * @param objSpectrum
     * @return
     */
    private boolean equalsBasics(PeptideSpectrumMatch objSpectrum) {
        if (charge != objSpectrum.charge) return false;
        if (massToCharge != objSpectrum.massToCharge) return false;
        if (deltaMass != objSpectrum.deltaMass) return false;
        if (missed != objSpectrum.missed) return false;

        if (!Objects.equals(isUnique, objSpectrum.isUnique)) return false;
        if (!Objects.equals(isDecoy, objSpectrum.isDecoy)) return false;

        if (!Objects.equals(retentionTime, objSpectrum.retentionTime)) return false;

        return (sequence.equals(objSpectrum.sequence));
    }


    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + hashCodeWithoutID();

        return result;
    }


    /**
     * Calculates the hash code without using the id
     *
     * @return
     */
    public int hashCodeWithoutID() {
        int result = charge;
        result = 31 * result + (scores != null ? scores.hashCode() : 0);
        long bits = Double.doubleToLongBits(massToCharge);
        result = 31 * result + (int)(bits ^ (bits >>> 32));
        bits = Double.doubleToLongBits(deltaMass);
        result = 31 * result + (int)(bits ^ (bits >>> 32));
        if (retentionTime != null) {
            bits = Double.doubleToLongBits(retentionTime);
            result = 31 * result + (int)(bits ^ (bits >>> 32));
        }
        result = 31 * result + missed;
        result = 31 * result + sequence.hashCode();
        result = 31 * result + modifications.hashCode();
        result = 31 * result + (sourceID != null ? sourceID.hashCode() : 0);
        result = 31 * result + (spectrumTitle != null ? spectrumTitle.hashCode() : 0);
        result = 31 * result + pFile.hashCode();
        result = 31 * result + (isUnique != null ? isUnique.hashCode() : 0);
        result = 31 * result + (isDecoy != null ? isDecoy.hashCode() : 0);
        result = 31 * result + (spectrumID != null ? spectrumID.hashCode() : 0);

        return result;
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
        return id;
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


    @Override
    public double getMassToCharge() {
        return massToCharge;
    }


    @Override
    public double getDeltaMass() {
        return deltaMass;
    }


    @Override
    public Double getRetentionTime() {
        return retentionTime;
    }


    @Override
    public int getCharge() {
        return charge;
    }


    @Override
    public int getMissedCleavages() {
        return missed;
    }


    @Override
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
        StringBuilder modificationSB = new StringBuilder(sequence);
        for (Map.Entry<Integer, Modification> modIt : modifications.entrySet()) {
            modificationSB.append('(');
            modificationSB.append(modIt.getKey()).append(';').append(modIt.getValue().getMassString());
            modificationSB.append(')');
        }
        peptideStringID = modificationSB.toString();

        identificationKeys = new HashMap<>(2);

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
    public static String getModificationString(Map<Integer, Modification> modifications) {
        StringBuilder modSb = new StringBuilder();
        boolean first = true;
        TreeMap<Integer, Modification> treeModMap =
                new TreeMap<>(modifications);

        for (Map.Entry<Integer, Modification> modIt : treeModMap.entrySet()) {
            if (!first) {
                modSb.append('|');
            }
            modSb.append('[').append(modIt.getKey()).append(',');

            modSb.append(modIt.getValue().getMassString());
            modSb.append(']');

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
     */
    public void addModification(int pos, Modification mod) {
        modifications.put(pos, mod);
        modificationChanged = true;
    }


    @Override
    public Map<Integer, Modification> getModifications() {
        return modifications;
    }


    @Override
    public String getSourceID() {
        return sourceID;
    }


    @Override
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
                new HashMap<>(maximalKeySettings);

        // remove the SEQUENCE and MODIFICATIONS, they are not needed for spectrumIdentificationKey
        psmSetSettings.remove(IdentificationKeySettings.SEQUENCE.name());
        psmSetSettings.remove(IdentificationKeySettings.MODIFICATIONS.name());

        return getIdentificationKey(psmSetSettings);
    }


    /**
     * Returns a string for identifying a PSM.<br/>
     * This should be unique for each PSM and is used to pre-sort PSMs found by
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

        List<String> usedSettings = psmSetSettings.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());

        StringBuilder key = new StringBuilder();
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
                    value = Double.toString(PIATools.round(massToCharge, PIAConstants.MASS_TO_CHARGE_PRECISION));
                }
                break;

            case MODIFICATIONS:
                value = modificationString;
                break;

            case RETENTION_TIME:
                if (rt != null) {
                    value = Double.toString((int)PIATools.round(rt, PIAConstants.RETENTION_TIME_PRECISION));
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

            default:
                value = null;
                break;
            }

            if (value != null) {
                if (key.length() > 0) {
                    key.append(':');
                }

                key.append(value);
            }
        }

        return key.toString();
    }


    /**
     * Returns a string for identifying a PSM.<br/>
     * This should be unique for each PSM and is used to pre-sort PSMs found by
     * different searches. These have to be refined later, because values with
     * m/z-values can not be used, for identification, due to search engine
     * roundings.
     *
     * @param psmSetSettings
     * @return
     */
    public String getIdentificationKey(Map<String, Boolean> psmSetSettings) {
        List<String> usedSettings = psmSetSettings.entrySet()
                .stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());

        StringBuilder keyKey = new StringBuilder();

        usedSettings.forEach(keyKey::append);

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

            if (key != null) {
                identificationKeys.put(keyKey.toString(), key);
            }
        }

        return key;
    }


    /**
     * Returns a nice name / header for this PSM's spectrum
     * @return
     */
    public String getNiceSpectrumName() {
        StringBuilder spectrumName = new StringBuilder();

        if (sourceID != null) {
            spectrumName.append(sourceID);
        } else {
            spectrumName.append("[no sourceID]");
        }

        spectrumName.append(" (");
        spectrumName.append((int)PIATools.round(massToCharge, PIAConstants.MASS_TO_CHARGE_PRECISION));
        spectrumName.append(", ");
        if (charge > 0) {
            spectrumName.append('+');
        }
        spectrumName.append(charge);

        if (retentionTime != null) {
            spectrumName.append(", ");
            spectrumName.append(PIATools.round(retentionTime, PIAConstants.RETENTION_TIME_PRECISION));
        }

        spectrumName.append(')');

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
     * Add a list of PSMS scores, best performance than one by one.
     * @param scores
     */
    public void addAllScores(List<ScoreModel> scores) {
        if(scores != null){
            if(this.scores == null)
                this.scores = new ArrayList<>();
            this.scores.addAll(scores);
        }
    }

    public String getSpectraDataRef() {
        return spectraDataRef;
    }

    public void setSpectraDataRef(String spectraDataRef) {
        this.spectraDataRef = spectraDataRef;
    }
}