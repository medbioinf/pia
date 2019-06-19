package de.mpc.pia.modeller.psm;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.modeller.IdentificationKeySettings;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;
import de.mpc.pia.tools.PIAConstants;


/**
 *
 * @author julianu
 *
 */
public class ReportPSM  implements PSMReportItem {

    private static final long serialVersionUID = 4553213161575220358L;


    /** unique ID of the item */
    private Long id;

    /** the associated spectrum */
    private PeptideSpectrumMatch spectrum;

    /** marks, if this PSM is a decoy */
    private boolean isDecoy;

    /** marks, if this item is FDR good */
    private boolean isFDRGood;

    /** all the accessions this PSM occurs in */
    private TreeMap<String, Accession> accessions;

    /** the rank of the PSM */
    private Long rank;

    /** the local fdr of the PSM */
    private Double fdrValue;

    /** the q-value, only available when FDR is calculated */
    private Double qValue;

    /** the FDR Score */
    private ScoreModel fdrScore;

    /** map from the scoreShorts to the identification ranks */
    private Map<String, Integer> identificationRanks;

    /** The maximal set of  {@link IdentificationKeySettings} which are available on this PSM */
    private Map<String, Boolean> maximalSpectraIdentificationSettings;

    /** The maximal set of not redundant {@link IdentificationKeySettings} which are available on this PSM */
    private Map<String, Boolean> maximalNotRedundantSpectraIdentificationSettings;


    /**
     * Basic constructor.
     *
     * @param id
     * @param spectrum
     */
    public ReportPSM(Long id, PeptideSpectrumMatch spectrum) {
        this.id = id;
        this.spectrum = spectrum;
        isDecoy = (spectrum.getIsDecoy() != null) && spectrum.getIsDecoy();
        isFDRGood = false;
        accessions = new TreeMap<>();
        qValue = null;
        fdrScore = null;

        fdrValue = Double.POSITIVE_INFINITY;
        rank = 0L;
        identificationRanks = new HashMap<>(3);

        // set the map to available values
        maximalSpectraIdentificationSettings = new HashMap<>(5);
        maximalSpectraIdentificationSettings.put(
                IdentificationKeySettings.MASSTOCHARGE.name(), true);
        if (spectrum.getRetentionTime() != null) {
            maximalSpectraIdentificationSettings.put(
                    IdentificationKeySettings.RETENTION_TIME.name(), true);
        }
        if (spectrum.getSourceID() != null) {
            maximalSpectraIdentificationSettings.put(
                    IdentificationKeySettings.SOURCE_ID.name(), true);
        }
        if (spectrum.getSpectrumTitle() != null) {
            maximalSpectraIdentificationSettings.put(
                    IdentificationKeySettings.SPECTRUM_TITLE.name(), true);
        }
        maximalSpectraIdentificationSettings.put(
                IdentificationKeySettings.CHARGE.name(), true);

        maximalNotRedundantSpectraIdentificationSettings =
                IdentificationKeySettings.noRedundantSettings(maximalSpectraIdentificationSettings);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReportPSM reportPSM = (ReportPSM) o;

        if (isDecoy != reportPSM.isDecoy) return false;
        if (isFDRGood != reportPSM.isFDRGood) return false;
        if (!id.equals(reportPSM.id)) return false;
        if (spectrum != null ? !spectrum.equals(reportPSM.spectrum) : reportPSM.spectrum != null) return false;
        return accessions != null ? accessions.equals(reportPSM.accessions) : reportPSM.accessions == null;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (spectrum != null ? spectrum.hashCode() : 0);
        result = 31 * result + (isDecoy ? 1 : 0);
        result = 31 * result + (isFDRGood ? 1 : 0);
        result = 31 * result + (accessions != null ? accessions.hashCode() : 0);
        return result;
    }

    /**
     * Getter for the ID
     * @return
     */
    public Long getId() {
        return id;
    }


    @Override
    public String getIdentificationKey(Map<String, Boolean> psmSetSettings) {
        return spectrum.getIdentificationKey(psmSetSettings);
    }


    @Override
    public Map<String, Boolean> getNotRedundantIdentificationKeySettings() {
        return maximalNotRedundantSpectraIdentificationSettings;
    }


    @Override
    public String getPeptideStringID(boolean considerModifications) {
        return spectrum.getPeptideStringID(considerModifications);
    }


    /**
     * Getter for the spectrum.
     * @return
     */
    public PeptideSpectrumMatch getSpectrum() {
        return spectrum;
    }


    /**
     * Getter for isFDRGood
     * @return
     */
    public boolean getIsFDRGood() {
        return isFDRGood;
    }


    /**
     * Setter for isFDRGood
     * @param isGood
     */
    @Override
    public void setIsFDRGood(boolean isGood) {
        isFDRGood = isGood;
    }


    @Override
    public boolean getIsDecoy() {
        return isDecoy;
    }


    /**
     * Setter for isDecoy
     * @param isDecoy
     */
    public void setIsDecoy(boolean isDecoy) {
        this.isDecoy = isDecoy;
    }


    /**
     * Add the given accession into the accessions map/trie.
     * @param accession
     */
    public void addAccession(Accession accession) {
        accessions.put(accession.getAccession(), accession);
    }


    @Override
    public List<Accession> getAccessions() {
        List<Accession> accList = new ArrayList<>(accessions.size());

        // only add the accession, if it was found in the file
        accList.addAll(accessions.entrySet().stream().filter(accIt -> accIt.getValue().foundInFile(spectrum.getFile().getID())).map(Map.Entry::getValue).collect(Collectors.toList()));

        return accList;
    }


    @Override
    public String getSequence() {
        return spectrum.getSequence();
    }


    @Override
    public int getCharge() {
        return spectrum.getCharge();
    }


    @Override
    public String getSourceID() {
        return spectrum.getSourceID();
    }


    @Override
    public String getSpectrumTitle() {
        return spectrum.getSpectrumTitle();
    }


    /**
     * Getter for the file
     * @return
     */
    public PIAInputFile getFile() {
        return spectrum.getFile();
    }


    /**
     * Getter for the name of the {@link PIAInputFile}
     * @return
     */
    public String getInputFileName() {
        String name = spectrum.getFile().getName();

        if (name == null) {
            name = spectrum.getFile().getFileName();
        }

        return name;
    }


    /**
     * Getter for the fileName of the {@link PIAInputFile}
     * @return
     */
    public String getFileName() {
        return spectrum.getFile().getFileName();
    }

    /**
     * Getter for the file id
     * @return
     */
    public Long getFileID() {
        return spectrum.getFile().getID();
    }


    @Override
    public Map<Integer, Modification> getModifications() {
        return spectrum.getModifications();
    }


    @Override
    public String getModificationsString() {
        return spectrum.getModificationString();
    }


    @Override
    public int getMissedCleavages() {
        return spectrum.getMissedCleavages();
    }


    @Override
    public double getMassToCharge() {
        return spectrum.getMassToCharge();
    }


    @Override
    public double getDeltaMass() {
        return spectrum.getDeltaMass();
    }


    @Override
    public Double getRetentionTime() {
        return spectrum.getRetentionTime();
    }


    @Override
    public double getDeltaPPM() {
        double mass = spectrum.getCharge() *
                (spectrum.getMassToCharge() - PIAConstants.H_MASS.doubleValue());

        return (spectrum.getDeltaMass()) / mass * 1000000;
    }


    @Override
    public Double getScore(String scoreName) {
        Double scoreVal = Double.NaN;

        if (ScoreModelEnum.PSM_LEVEL_FDR_SCORE.isValidDescriptor(scoreName)
                && (fdrScore != null)) {
            scoreVal = fdrScore.getValue();
        } else if (ScoreModelEnum.PSM_LEVEL_Q_VALUE.isValidDescriptor(scoreName)
                && (qValue != null)) {
            scoreVal = qValue;
        } else {
            // for all other cases: get score from spectrum
            ScoreModel score = spectrum.getScore(scoreName);
            if (score != null) {
                scoreVal = score.getValue();
            }
        }

        return scoreVal;
    }


    @Override
    public String getScoresString() {
        StringBuilder scoresSB = new StringBuilder();

        for (ScoreModel model : getScores()) {
            if (scoresSB.length() > 0) {
                scoresSB.append(',');
            }
            scoresSB.append(model.getName());
            scoresSB.append(':');
            scoresSB.append(model.getValue());
        }

        return scoresSB.toString();
    }


    /**
     * Returns a list of the score models of this PSM.
     *
     * @return
     */
    public List<ScoreModel> getScores() {

        List<ScoreModel> scores = new ArrayList<>(spectrum.getScores());

        if (fdrScore != null) {
            scores.add(fdrScore);
        }

        return scores;
    }


    @Override
    public double getFDR() {
        if (fdrValue == null) {
            return Double.NaN;
        } else {
            return fdrValue;
        }
    }


    @Override
    public void setFDR(double fdr) {
        this.fdrValue = fdr;
    }


    @Override
    public Long getRank() {
        return rank;
    }


    @Override
    public void setRank(Long rank) {
        this.rank = rank;
    }


    /**
     * Sets the rank for the score given by scoreShort.
     *
     * @param scoreShort
     * @param rank
     */
    public void setIdentificationRank(String scoreShort, Integer rank) {
        identificationRanks.put(scoreShort, rank);
    }


    /**
     * Returns the identification rank for the given score type.<br/>
     * The identification ranks must be calculated, or null will be returned
     * always.
     *
     * @param scoreShort
     * @return
     */
    public Integer getIdentificationRank(String scoreShort) {
        return identificationRanks.get(scoreShort);
    }


    /**
     * Returns all the identificationRanks
     * @return
     */
    public Map<String, Integer> getIdentificationRanks() {
        return identificationRanks;
    }


    @Override
    public void dumpFDRCalculation() {
        isFDRGood = false;
        qValue = null;
        fdrScore = null;
        fdrValue = Double.POSITIVE_INFINITY;
    }


    @Override
    public void updateDecoyStatus(DecoyStrategy strategy, Pattern p) {
        switch (strategy) {
        case ACCESSIONPATTERN:
            this.isDecoy = isDecoyWithPattern(p);
            break;

        case SEARCHENGINE:
            this.isDecoy = isDecoyWithSearchengine();
            break;

        default:
            this.isDecoy = false;
        }
    }


    /**
     * Returns true, if the PSM is a decoy with the given pattern.
     * @param p
     */
    private boolean isDecoyWithPattern(Pattern p) {
        Matcher m;
        boolean decoy = true;

        for (Map.Entry<String, Accession> accIt : accessions.entrySet()) {
            m = p.matcher(accIt.getValue().getAccession());
            decoy &= m.matches();
        }

        return decoy;
    }


    /**
     * Returns true, if the PSM is flagged as a decoy by the searchengine, i.e.
     * it is already set in the spectrum.
     *
     * @return
     */
    private boolean isDecoyWithSearchengine() {
        boolean decoy = false;

        if (spectrum.getIsDecoy() != null) {
            decoy = spectrum.getIsDecoy();
        }

        return decoy;
    }


    @Override
    public double getQValue() {
        if (qValue == null) {
            return Double.NaN;
        } else {
            return qValue;
        }
    }


    @Override
    public void setQValue(double value) {
        this.qValue = value;
    }


    @Override
    public ScoreModel getFDRScore() {
        return fdrScore;
    }


    @Override
    public void setFDRScore(Double score) {
        if (fdrScore != null) {
            fdrScore.setValue(score);
        } else {
            fdrScore = new ScoreModel(score, ScoreModelEnum.PSM_LEVEL_FDR_SCORE);
        }
    }


    @Override
    public ScoreModel getCompareScore(String scoreShortname) {
        if (ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName().equals(scoreShortname)) {
            return fdrScore;
        }
        else {
            return spectrum.getScore(scoreShortname);
        }
    }


    @Override
    public Map<String, Boolean> getAvailableIdentificationKeySettings() {
        return maximalSpectraIdentificationSettings;
    }


    @Override
    public String getNiceSpectrumName() {
        return spectrum.getNiceSpectrumName();
    }


    @Override
    public Peptide getPeptide() {
        return getSpectrum().getPeptide();
    }
}
