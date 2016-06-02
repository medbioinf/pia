package de.mpc.pia.modeller;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import uk.ac.ebi.jmzidml.model.mzidml.AnalysisCollection;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisProtocolCollection;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.DBSequence;
import uk.ac.ebi.jmzidml.model.mzidml.InputSpectrumIdentifications;
import uk.ac.ebi.jmzidml.model.mzidml.Inputs;
import uk.ac.ebi.jmzidml.model.mzidml.ParamList;
import uk.ac.ebi.jmzidml.model.mzidml.Peptide;
import uk.ac.ebi.jmzidml.model.mzidml.PeptideEvidence;
import uk.ac.ebi.jmzidml.model.mzidml.PeptideHypothesis;
import uk.ac.ebi.jmzidml.model.mzidml.ProteinAmbiguityGroup;
import uk.ac.ebi.jmzidml.model.mzidml.ProteinDetection;
import uk.ac.ebi.jmzidml.model.mzidml.ProteinDetectionHypothesis;
import uk.ac.ebi.jmzidml.model.mzidml.ProteinDetectionList;
import uk.ac.ebi.jmzidml.model.mzidml.ProteinDetectionProtocol;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationItem;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationItemRef;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationList;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationResult;
import uk.ac.ebi.jmzidml.xml.io.MzIdentMLMarshaller;
import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.AccessionOccurrence;
import de.mpc.pia.intermediate.Group;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ProteinExecuteCommands;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.ReportProteinComparatorFactory;
import de.mpc.pia.modeller.protein.inference.AbstractProteinInference;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.SortOrder;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.modeller.score.comparator.RankCalculator;
import de.mpc.pia.tools.MzIdentMLTools;
import de.mpc.pia.tools.OntologyConstants;
import de.mpc.pia.tools.unimod.UnimodParser;


/**
 * Modeller for protein related stuff.
 *
 * @author julian
 *
 */
public class ProteinModeller {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(ProteinModeller.class);


    /** List of the report proteins */
    private List<ReportProtein> reportProteins;

    /** Map of the report proteins, for easier accession */
    private Map<Long, ReportProtein> reportProteinsMap;

    /** the last applied inference filter */
    private AbstractProteinInference appliedProteinInference;

    /** the last applied scoring method */
    private AbstractScoring appliedScoringMethod;


    /** the corresponding {@link PSMModeller} */
    private PSMModeller psmModeller;

    /** the corresponding {@link PSMModeller} */
    private PeptideModeller peptideModeller;

    /** map of the {@link Group}s in the intermediate structure */
    private Map<Long, Group> intermediateGroups;


    /** the FDR settings for the protein FDR */
    private FDRData fdrData;


    /** filters which may be used for a protein inference */
    private List<AbstractFilter> inferenceFilters;

    /** the list of filters applied to the protein report */
    private List<AbstractFilter> reportFilters;


    // TODO: set these defaults in a file
    private static DecoyStrategy defaultDecoyStrategy = FDRData.DecoyStrategy.ACCESSIONPATTERN;
    private static String defaultDecoyPattern = "s.*";
    private static Double defaultFDRThreshold = 0.05;


    /**
     * Basic constructor for the ProteinModeller.<br/>
     * There will be no inference, but only initialization.
     *
     * @param groups
     * @param considerModifications
     */
    public ProteinModeller(PSMModeller psmModeller,
            PeptideModeller peptideModeller, Map<Long, Group> groups) {
        if (psmModeller == null) {
            throw new IllegalArgumentException("The given PSMModeller is null!");
        } else {
            this.psmModeller = psmModeller;

        }

        if (peptideModeller == null) {
            throw new IllegalArgumentException("The given PeptideModeller is null!");
        } else {
            this.peptideModeller = peptideModeller;
        }

        if (groups == null) {
            throw new IllegalArgumentException("The given intermediate Groups is null!");
        } else {
            this.intermediateGroups = groups;
        }

        this.appliedProteinInference = null;
        this.appliedScoringMethod = null;

        this.fdrData = new FDRData(defaultDecoyStrategy, defaultDecoyPattern,
                defaultFDRThreshold);

        this.reportFilters = new ArrayList<AbstractFilter>();
    }


    /**
     * Applies the general settings
     */
    public void applyGeneralSettings() {

    }


    /**
     * Returns whether modifications were considered while building the
     * peptides.
     *
     * @return
     */
    public Boolean getConsiderModifications() {
        return peptideModeller.getConsiderModifications();
    }


    /**
     * Returns a List of all the currently available scoreShortNames
     * @return
     */
    public List<String> getAllScoreShortNames() {
        List<String> scoreShortNames = new ArrayList<String>();

        // get the scores from the files
        for (Long fileID : psmModeller.getFiles().keySet()) {
            for (String scoreShort : psmModeller.getScoreShortNames(fileID)) {
                if (!scoreShortNames.contains(scoreShort)) {
                    scoreShortNames.add(scoreShort);
                }
            }
        }

        return scoreShortNames;
    }


    /**
     * Returns the Score name, given the scoreShortName.
     * @param fileID
     * @param shortName
     * @return
     */
    public String getScoreName(String shortName) {
        return psmModeller.getScoreName(shortName);
    }


    /**
     * Returns the mapping from the shortNames to the nicely readable names.
     *
     * @return
     */
    public Map<String, String> getScoreShortsToScoreNames() {
        return psmModeller.getScoreShortsToScoreNames();
    }


    /**
     * Returns the filtered List of {@link ReportProtein}s or null, if the
     * proteins are not inferred yet.
     *
     * @param filters
     * @return
     */
    public List<ReportProtein> getFilteredReportProteins(
            List<AbstractFilter> filters) {
        if (reportProteins != null) {
            return FilterFactory.applyFilters(reportProteins, filters);
        } else {
            return null;
        }
    }


    /**
     * Returns the protein with the given ID.
     * @param proteinID
     * @return
     */
    public ReportProtein getProtein(Long proteinID) {
        return reportProteinsMap.get(proteinID);
    }


    /**
     * Calculates the reported proteins with the given settings for the
     * inference.
     */
    public void infereProteins(AbstractProteinInference proteinInference) {
        reportProteins = new ArrayList<ReportProtein>();

        if (proteinInference != null) {
            appliedProteinInference = proteinInference;
            reportProteins = proteinInference.calculateInference(
                    intermediateGroups,
                    psmModeller.getReportPSMSets(),
                    peptideModeller.getConsiderModifications(),
                    psmModeller.getPSMSetSettings());
        } else {
            LOGGER.error("No inference method set!");
            appliedProteinInference = null;
            reportProteins = null;
        }

        this.fdrData = new FDRData(defaultDecoyStrategy, defaultDecoyPattern,
                defaultFDRThreshold);

        // create the protein map
        reportProteinsMap = new HashMap<Long, ReportProtein>();
        for (ReportProtein protein : reportProteins) {
            reportProteinsMap.put(protein.getID(), protein);

            if (protein.getSubSets().size() > 0) {
                for (ReportProtein subProtein : protein.getSubSets()) {
                    reportProteinsMap.put(subProtein.getID(), subProtein);
                }
            }
        }
    }


    /**
     * Returns the last applied inference filter.<br/>
     * If there was no filter or an error occurred during the inference, null
     * will be returned.
     *
     * @return
     */
    public AbstractProteinInference getAppliedProteinInference() {
        return appliedProteinInference;
    }


    /**
     * Returns the last applied scoring method.<br/>
     * If there was no scoring yet or an error occurred during the scoring, null
     * will be returned.
     *
     * @return
     */
    public AbstractScoring getAppliedScoringMethod() {
        return appliedScoringMethod;
    }


    /**
     * Returns whether the proteins are ranked.
     * @return
     */
    public Boolean getAreProteinsRanked() {
        for (ReportProtein protein : reportProteins) {
            if ((protein.getRank() != null)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Calculates the ranking. If the filter List is not null or empty, the
     * Report is filtered before ranking.
     *
     * @param fileID
     * @param rankableShortName
     * @param filters
     */
    public void calculateRanking(List<AbstractFilter> filters) {
        if ((appliedProteinInference == null) ||
                (appliedProteinInference.getScoring() == null)) {
            LOGGER.error("No protein inference set." +
                    " Please calculate inference before ranking.");
            return;
        }

        // first, dump all prior ranking
        for (ReportProtein protein : reportProteins) {
            protein.setRank(-1L);
        }

        Comparator<ReportProtein> comparator =
                ReportProteinComparatorFactory.CompareType.SCORE_SORT.getNewInstance();

        RankCalculator.calculateRanking(
                ScoreModelEnum.PROTEIN_SCORE.getShortName(),
                FilterFactory.applyFilters(reportProteins, filters),
                comparator);
    }


    /**
     * Resorts the report with the given sorting parameters
     */
    public void sortReport(List<String> sortOrders,
            Map<String, SortOrder> sortables) {

        List<Comparator<ReportProtein>> compares =
                new ArrayList<Comparator<ReportProtein>>();

        for (String sortKey : sortOrders) {
            SortOrder order = sortables.get(sortKey);

            compares.add( ReportProteinComparatorFactory.getComparatorByName(
                    sortKey, order));
        }

        Collections.sort(reportProteins,
                ReportProteinComparatorFactory.getComparator(compares));
    }


    /**
     * Apply the given scoring to the List of {@link ReportProtein}s.
     *
     * @param scoring
     */
    public void applyScoring(AbstractScoring scoring) {
        if (scoring == null) {
            LOGGER.error("No scoring method given.");
            appliedScoringMethod = null;
            return;
        }

        LOGGER.info("applying scoring method: " + scoring.getName());
        scoring.calculateProteinScores(reportProteins);
        LOGGER.info("scoring done");
        appliedScoringMethod = scoring;

        this.fdrData = new FDRData(defaultDecoyStrategy, defaultDecoyPattern,
                defaultFDRThreshold);
    }


    /**
     * Getter for the protein FDR data
     * @return
     */
    public FDRData getFDRData() {
        return fdrData;
    }


    /**
     * Returns, whether there are any PSMs in the PIA XML file, which are
     * flagged as decoys.
     *
     * @param fileID
     * @return
     */
    public Boolean getInternalDecoysExist() {
        for (Long fileID : psmModeller.getFiles().keySet()) {
            if ((fileID > 0) && psmModeller.getFileHasInternalDecoy(fileID)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Updates the {@link FDRData} for the protein FDR
     *
     * @param fileID
     * @return
     */
    public void updateFDRData(DecoyStrategy decoyStrategy,
            String decoyPattern, Double fdrThreshold) {
        fdrData.setDecoyStrategy(decoyStrategy);
        fdrData.setDecoyPattern(decoyPattern);
        fdrData.setFDRThreshold(fdrThreshold);
        fdrData.setScoreShortName(ScoreModelEnum.PROTEIN_SCORE.getShortName());

        LOGGER.info("Protein FDRData set to: " +
                fdrData.getDecoyStrategy() + ", " +
                fdrData.getDecoyPattern() + ", " +
                fdrData.getFDRThreshold() + ", " +
                fdrData.getScoreShortName());
    }


    /**
     * Updates the decoy states of the Proteins with the current settings from
     * the FDRData.
     */
    public void updateDecoyStates() {
        LOGGER.info("updateDecoyStates");
        Pattern p = Pattern.compile(fdrData.getDecoyPattern());

        if (reportProteins != null) {
            for (ReportProtein protein : reportProteins) {
                // dump all FDR data
                protein.dumpFDRCalculation();
                protein.updateDecoyStatus(fdrData.getDecoyStrategy(), p);
            }
        } else {
            LOGGER.error("Proteins must be inferred before calling updateDecoyStates");
        }
    }


    /**
     * Calculate the protein FDR
     *
     * @param fileID
     */
    public void calculateFDR() {
        // calculate the FDR values
        fdrData.calculateFDR(reportProteins, true);
    }


    /**
     * Returns the report filters.
     * @param fileID
     * @return
     */
    public List<AbstractFilter> getReportFilters() {
        if (reportFilters == null) {
            reportFilters = new ArrayList<AbstractFilter>();
        }

        return reportFilters;
    }


    /**
     * Add a new filter to the report filters.
     */
    public boolean addReportFilter(AbstractFilter newFilter) {
        if (newFilter != null) {
            return getReportFilters().add(newFilter);
        } else {
            return false;
        }
    }


    /**
     * Removes the report filter at the given index.
     * @param fileID
     * @param removingIndex
     * @return
     */
    public AbstractFilter removeReportFilter(int removingIndex) {
        if ((removingIndex >= 0) &&
                (reportFilters != null) &&
                (removingIndex < reportFilters.size())) {
            return reportFilters.remove(removingIndex);
        }

        return null;
    }


    /**
     * Returns the list of inference filters. These are not the currently set
     * filters, but a list of filters which may be used for inference.
     * @param fileID
     * @return
     */
    public List<AbstractFilter> getInferenceFilters() {
        if (inferenceFilters == null) {
            inferenceFilters = new ArrayList<AbstractFilter>();
        }

        return inferenceFilters;
    }


    /**
     * Add a new filter to the inference filters. These are not the currently
     * set filters, but a list of filters which may be used for inference.
     */
    public boolean addInferenceFilter(AbstractFilter newFilter) {
        if (newFilter != null) {
            return getInferenceFilters().add(newFilter);
        } else {
            return false;
        }
    }


    /**
     * Writes the Protein report with the given filters in a loose CSV format.
     * <br/>
     * As the export may or may not also contain peptide-, PSM-set- and
     * PSM-data, each exported line has a specifying tag in the beginning, if
     * more than only the protein data is exported. The tags are PROTEIN,
     * PEPTIDE, PSMSET and PSM for the respective data. Additionally there is a
     * line starting with COLS_[tag], specifying the columns of the respective
     * data.
     *
     * @throws IOException
     */
    public void exportCSV(Writer writer, Boolean filterExport,
            Boolean includePeptides, Boolean includePSMSets,
            Boolean includePSMs, Boolean oneAccessionPerLine) throws IOException {
        List<ReportProtein> report;
        Boolean includes = includePeptides || includePSMSets || includePSMs;
        List<String> scoreShorts = peptideModeller.getScoreShortNames(0L);

        boolean considermodifications =
                peptideModeller.getConsiderModifications();

        String separator = ",";

        if (includes && !oneAccessionPerLine) {
            writer.append(
                    "\"COLS_PROTEIN\"" + separator +
                    "\"accessions\"" + separator +
                    "\"score\"" + separator +
                    "\"#peptides\"" + separator +
                    "\"#PSMs\"" + separator +
                    "\"#spectra\"");

            if (fdrData.getNrItems() != null) {
                writer.append( separator +
                        "\"isDecoy\"" + separator +
                        "\"FDR\"" + separator +
                        "\"q-value\"" + separator);
            }

            writer.append("\n");

            if (includePeptides) {
                writer.append(
                        "\"COLS_PEPTIDE\"" + separator +
                        "\"sequence\"" + separator);

                if (considermodifications) {
                    writer.append("\"modifications\"" + separator);
                }

                writer.append(	"\"accessions\"" + separator +
                        "\"#spectra\"" + separator +
                        "\"#PSMSets\"" + separator +
                        "\"bestScores\"" +
                        "\n"
                        );
            }

            if (includePSMSets) {
                writer.append(
                        "\"COLS_PSMSET\"" + separator +
                        "\"sequence\"" + separator);

                if (considermodifications) {
                    writer.append("\"modifications\"" + separator);
                }

                writer.append("\"#identifications\"" + separator +
                        "\"charge\"" + separator +
                        "\"m/z\"" + separator +
                        "\"dMass\"" + separator +
                        "\"ppm\"" + separator +
                        "\"RT\"" + separator +
                        "\"missed\"" + separator +
                        "\"sourceID\"" + separator +
                        "\"spectrumTitle\"" + separator +
                        "\"scores\"" +
                        "\n"
                        );
            }

            if (includePSMs) {
                writer.append(
                        "\"COLS_PSM\"" + separator +
                        "\"filename\"" + separator +
                        "\"sequence\"" + separator);

                if (considermodifications) {
                    writer.append("\"modifications\"" + separator);
                }

                writer.append("\"charge\"" + separator +
                        "\"m/z\"" + separator +
                        "\"dMass\"" + separator +
                        "\"ppm\"" + separator +
                        "\"RT\"" + separator +
                        "\"missed\"" + separator +
                        "\"sourceID\"" + separator +
                        "\"spectrumTitle\"" + separator +
                        "\"scores\"" +
                        "\n"
                        );
            }

        } else if (!oneAccessionPerLine) {
            // no special includes, no SpectralCounting
            writer.append(
                    "\"accessions\"" + separator +
                    "\"score\"" + separator +
                    "\"#peptides\"" + separator +
                    "\"#PSMs\"" + separator +
                    "\"#spectra\"");

            if (fdrData.getNrItems() != null) {
                writer.append( separator +
                        "\"isDecoy\"" + separator +
                        "\"FDR\"" + separator +
                        "\"q-value\"");
            }

            writer.append("\n");
        } else {
            // oneAccessionPerLine is set, override everything else
            writer.append(
                    "\"accession\"" + separator +
                    "\"filename\"" + separator +
                    "\"sequence\"" + separator);

            if (considermodifications) {
                writer.append("\"modifications\"" + separator);
            }

            writer.append("\"charge\"" + separator +
                    "\"m/z\"" + separator +
                    "\"dMass\"" + separator +
                    "\"ppm\"" + separator +
                    "\"RT\"" + separator +
                    "\"missed\"" + separator +
                    "\"sourceID\"" + separator +
                    "\"spectrumTitle\"" + separator +
                    "\"scores\"" + separator +
                    "\"isUnique\"" +
                    "\n"
                    );
        }

        report = filterExport ? getFilteredReportProteins(getReportFilters()) :
            reportProteins;

        if (report == null) {
            // no inference run?
            LOGGER.warn("The report is empty, probably no inference run?");
            writer.flush();
            return;
        }

        for (ReportProtein protein : report) {
            // Accessions	Score	Coverage	#Peptides	#PSMs	#Spectra

            StringBuilder accSB = new StringBuilder();
            for (Accession accession : protein.getAccessions()) {
                if (accSB.length() > 0) {
                    accSB.append(",");
                }
                accSB.append(accession.getAccession());

                // TODO: if decoys through inherit, mark the decoy accessions
            }

            if (!oneAccessionPerLine) {
                if (includes) {
                    writer.append("\"PROTEIN\"" + separator);
                }

                writer.append("\"" + accSB.toString() + "\"" + separator +
                        "\"" + protein.getScore() + "\"" + separator +
                        "\"" + protein.getNrPeptides() + "\"" + separator +
                        "\"" + protein.getNrPSMs() + "\"" + separator +
                        "\"" + protein.getNrSpectra() + "\""
                        );

                if (fdrData.getNrItems() != null) {
                    writer.append( separator +
                            "\"" + protein.getIsDecoy() + "\"" + separator +
                            "\"" + protein.getFDR() + "\"" + separator +
                            "\"" + protein.getQValue() + "\""
                            );
                }

                writer.append("\n");
            }


            if (includes || oneAccessionPerLine) {
                for (ReportPeptide peptide : protein.getPeptides()) {

                    StringBuilder modStringBuffer = new StringBuilder();
                    if (considermodifications) {
                        for (Map.Entry<Integer, Modification> modIt
                                : peptide.getModifications().entrySet()) {
                            modStringBuffer.append("[" + modIt.getKey() + "," +
                                    modIt.getValue().getMass() + ",");
                            if (modIt.getValue().getDescription() != null) {
                                modStringBuffer.append(
                                        modIt.getValue().getDescription());
                            }
                            modStringBuffer.append("]");
                        }
                    }

                    if (includePeptides && !oneAccessionPerLine) {

                        accSB = new StringBuilder();
                        for (Accession accession : peptide.getAccessions()) {
                            if (accSB.length() > 0) {
                                accSB.append(",");
                            }
                            accSB.append(accession.getAccession());
                        }

                        StringBuilder scoresSB = new StringBuilder();
                        for (String scoreShort : scoreShorts) {
                            ScoreModel model =
                                    peptide.getBestScoreModel(scoreShort);

                            if (model != null) {
                                if (scoresSB.length() > 0) {
                                    scoresSB.append(",");
                                }

                                scoresSB.append(model.getName() + ":" +
                                        model.getValue());
                            }

                        }

                        writer.append(
                                "\"PEPTIDE\"" + separator +
                                "\"" + peptide.getSequence() + "\"" + separator);

                        if (considermodifications) {
                            writer.append("\"" + modStringBuffer.toString() + "\"" + separator);
                        }

                        writer.append("\"" + accSB.toString() + "\"" + separator +
                                "\"" + peptide.getNrSpectra() + "\"" + separator +
                                "\"" + peptide.getNrPSMs() + "\"" + separator +
                                "\"" + scoresSB.toString() + "\"" +
                                "\n"
                                );
                    }


                    if (includePSMSets || includePSMs || oneAccessionPerLine) {
                        for (PSMReportItem psmSet : peptide.getPSMs()) {
                            if (psmSet instanceof ReportPSMSet) {

                                if (includePSMSets && !oneAccessionPerLine) {
                                    String rt;
                                    if (psmSet.getRetentionTime() != null) {
                                        rt = psmSet.getRetentionTime().toString();
                                    } else {
                                        rt = "";
                                    }

                                    String sourceID = psmSet.getSourceID();
                                    if (sourceID == null) {
                                        sourceID = "";
                                    }

                                    String spectrumTitle = psmSet.getSpectrumTitle();
                                    if (spectrumTitle == null) {
                                        spectrumTitle = "";
                                    }

                                    writer.append(
                                            "\"PSMSET\"" + separator +
                                            "\"" + psmSet.getSequence() +"\"" + separator);

                                    if (considermodifications) {
                                        writer.append("\"" + modStringBuffer.toString() + "\"" + separator);
                                    }

                                    writer.append("\"" + ((ReportPSMSet)psmSet).getPSMs().size() + "\"" + separator +
                                            "\"" + psmSet.getCharge() + "\"" + separator +
                                            "\"" + psmSet.getMassToCharge() + "\"" + separator +
                                            "\"" + psmSet.getDeltaMass() + "\"" + separator +
                                            "\"" + psmSet.getDeltaPPM() + "\"" + separator +
                                            "\"" + rt + "\"" + separator +
                                            "\"" + psmSet.getMissedCleavages() + "\"" + separator +
                                            "\"" + sourceID + "\"" + separator +
                                            "\"" + spectrumTitle + "\"" + separator +
                                            "\"" + psmSet.getScoresString() + "\"" +
                                            "\n"
                                            );
                                }

                                if (includePSMs || oneAccessionPerLine) {
                                    for (ReportPSM psm
                                            : ((ReportPSMSet)psmSet).getPSMs()) {

                                        String rt;
                                        if (psm.getRetentionTime() != null) {
                                            rt = psmSet.getRetentionTime().toString();
                                        } else {
                                            rt = "";
                                        }

                                        String sourceID = psm.getSourceID();
                                        if (sourceID == null) {
                                            sourceID = "";
                                        }

                                        String spectrumTitle = psm.getSpectrumTitle();
                                        if (spectrumTitle == null) {
                                            spectrumTitle = "";
                                        }

                                        StringBuilder scoresSB = new StringBuilder();
                                        for (ScoreModel model : psm.getScores()) {
                                            if (scoresSB.length() > 0) {
                                                scoresSB.append(",");
                                            }
                                            scoresSB.append(model.getName() +
                                                    ":" + model.getValue());
                                        }

                                        Boolean uniqueness =
                                                (psm.getSpectrum().getIsUnique() != null) ?
                                                        psm.getSpectrum().getIsUnique() : false;

                                                        if (!oneAccessionPerLine) {
                                                            writer.append(
                                                                    "\"PSM\"" + separator +
                                                                    "\"" + psm.getInputFileName() + "\"" + separator +
                                                                    "\"" + psm.getSequence() + "\"" + separator);

                                                            if (considermodifications) {
                                                                writer.append("\"" + modStringBuffer.toString() + "\"" + separator);
                                                            }

                                                            writer.append("\"" + psm.getCharge() + "\"" + separator +
                                                                    "\"" + psm.getMassToCharge() + "\"" + separator +
                                                                    "\"" + psm.getDeltaMass() + "\"" + separator +
                                                                    "\"" + psm.getDeltaPPM() + "\"" + separator +
                                                                    "\"" + rt + "\"" + separator +
                                                                    "\"" + psm.getMissedCleavages() + "\"" + separator +
                                                                    "\"" + sourceID + "\"" + separator +
                                                                    "\"" + spectrumTitle + "\"" + separator +
                                                                    "\"" + scoresSB.toString() + "\"" +
                                                                    "\n"
                                                                    );
                                                        } else {
                                                            // export for Spectral Counting
                                                            for (Accession acc
                                                                    : protein.getAccessions()) {

                                                                writer.append(
                                                                        "\"" + acc.getAccession() + "\"" + separator +
                                                                        "\"" + psm.getInputFileName() + "\"" + separator +
                                                                        "\"" + psm.getSequence() + "\"" + separator
                                                                        );

                                                                if (considermodifications) {
                                                                    writer.append(
                                                                            "\"" + modStringBuffer.toString() + "\"" + separator);
                                                                }

                                                                writer.append("\"" + psm.getCharge() + "\"" + separator +
                                                                        "\"" + psm.getMassToCharge() + "\"" + separator +
                                                                        "\"" + psm.getDeltaMass() + "\"" + separator +
                                                                        "\"" + psm.getDeltaPPM() + "\"" + separator +
                                                                        "\"" + rt + "\"" + separator +
                                                                        "\"" + psm.getMissedCleavages() + "\"" + separator +
                                                                        "\"" + sourceID + "\"" + separator +
                                                                        "\"" + spectrumTitle + "\"" + separator +
                                                                        "\"" + scoresSB.toString() + "\"" + separator +
                                                                        "\"" + uniqueness  + "\"" +
                                                                        "\n"
                                                                        );
                                                            }
                                                        }

                                    }
                                }

                            } else {
                                // TODO: better error/exception
                                LOGGER.error("PSM in peptide must be PSMSet!");
                            }
                        }
                    }


                }
            }
        }

        writer.flush();
    }


    /**
     * Writes the Protein report into mzIdentML.
     *
     * @throws IOException
     */
    public void exportMzIdentML(Writer writer, Boolean filterExport)
            throws IOException {
        LOGGER.info("start writing mzIdentML file");


        // TODO: this needs a complete overhaul!
/*
      errors on current export:

      <SpectrumIdentificationResult spectrumID="index=18510" spectraData_ref="spectraData_1" id="2:0:595.3292:4685.0:index=18510">
        <SpectrumIdentificationItem chargeState="2" experimentalMassToCharge="595.3291627988281" peptide_ref="PEP_DDLLIMVVQK(6;15.9949)" rank="0" passThreshold="true" id="2:0:595.3292:[6,15.9949]:4685.0:DDLLIMVVQK:index=18510">
          <PeptideEvidenceRef peptideEvidence_ref="PE_DDLLIMVVQK(6;15.9949)-436-445-sS4R2T5"></PeptideEvidenceRef>
          <cvParam cvRef="PSI-MS" accession="MS:1002315" name="consensus result"></cvParam>
          <cvParam cvRef="PSI-MS" accession="MS:1000016" name="scan start time" value="4685.09200000002" unitAccession="UO:0000010" unitName="second" unitCvRef="UO"></cvParam>
          <cvParam cvRef="PSI-MS" accession="MS:1000796" name="spectrum title" value="595.329162597656_4685.09200000002_controllerType=0 controllerNumber=1 scan=21139_OEII12347"></cvParam>
          <cvParam cvRef="PSI-MS" accession="MS:1001975" name="delta m/z" value="0.0013533867187499999" unitAccession="UO:0000221" unitName="dalton" unitCvRef="UO"></cvParam>
          <cvParam cvRef="PSI-MS" accession="MS:1002356" name="PSM-level combined FDRScore" value="1.4464577135661867E-8"></cvParam>
        </SpectrumIdentificationItem>
      </SpectrumIdentificationResult>

      <SpectrumIdentificationResult spectrumID="index=18510" spectraData_ref="spectraData_1" id="2:0:595.3292:index=18510">
        <SpectrumIdentificationItem chargeState="2" experimentalMassToCharge="595.3291625976562" peptide_ref="PEP_KNGKHYGKEK" rank="0" passThreshold="true" id="2:0:595.3292::KNGKHYGKEK:index=18510">
          <PeptideEvidenceRef peptideEvidence_ref="PE_KNGKHYGKEK-266-275-sQ3TQI7"></PeptideEvidenceRef>
          <cvParam cvRef="PSI-MS" accession="MS:1002315" name="consensus result"></cvParam>
          <cvParam cvRef="PSI-MS" accession="MS:1000796" name="spectrum title" value="595.329162597656_4685.09200000002_controllerType=0 controllerNumber=1 scan=21139_OEII12347"></cvParam>
          <cvParam cvRef="PSI-MS" accession="MS:1001975" name="delta m/z" value="1.002685546875" unitAccession="UO:0000221" unitName="dalton" unitCvRef="UO"></cvParam>
          <cvParam cvRef="PSI-MS" accession="MS:1002356" name="PSM-level combined FDRScore" value="1.4464577135661867E-8"></cvParam>
        </SpectrumIdentificationItem>
      </SpectrumIdentificationResult>
*/

        UnimodParser unimodParser = new UnimodParser();
        MzIdentMLMarshaller m = new MzIdentMLMarshaller();

        // XML header
        writer.write(m.createXmlHeader() + "\n");
        writer.write(m.createMzIdentMLStartTag("PIAExport for proteins") + "\n");

        // there are some variables needed for additional tags later
        Map<String, DBSequence> sequenceMap = new HashMap<String, DBSequence>();
        Map<String, Peptide> peptideMap = new HashMap<String, Peptide>();
        Map<String, PeptideEvidence> pepEvidenceMap =
                new HashMap<String, PeptideEvidence>();

        // TODO: if only one SIL is ever reported, get rid of the map here!
        Map<Long, SpectrumIdentificationList> silMap =
                new HashMap<Long, SpectrumIdentificationList>();
        AnalysisSoftware piaAnalysisSoftware = new AnalysisSoftware();
        Inputs inputs = new Inputs();
        AnalysisProtocolCollection analysisProtocolCollection =
                new AnalysisProtocolCollection();
        AnalysisCollection analysisCollection = new AnalysisCollection();

        psmModeller.writeCommonMzIdentMLTags(writer, m, unimodParser,
                sequenceMap, peptideMap, pepEvidenceMap, silMap,
                piaAnalysisSoftware, inputs,
                analysisProtocolCollection, analysisCollection,
                0L, false, true);

        /*
        for (SpectrumIdentificationList sil : silMap.values()) {
            // the "intermediate PSM list" flag, the combined list below is always the final
            CvParam tempCvParam = MzIdentMLTools.createPSICvParam(
                    OntologyConstants.INTERMEDIATE_PSM_LIST,
                    null);
            sil.getCvParam().add(tempCvParam);
        }
        */

        // create the ProteinDetectionProtocol for PIAs protein inference
        ProteinDetectionProtocol proteinDetectionProtocol =
                new ProteinDetectionProtocol();

        proteinDetectionProtocol.setId("PIA_protein_inference_protocol");
        proteinDetectionProtocol.setAnalysisSoftware(
                piaAnalysisSoftware);
        proteinDetectionProtocol.setName("PIA protein inference protocol");

        analysisProtocolCollection.setProteinDetectionProtocol(
                proteinDetectionProtocol);

        // add the inference settings to the AnalysisParams
        proteinDetectionProtocol.setAnalysisParams(new ParamList());

        // TODO: use CVs for all the userParams below

        // the used inference method
        CvParam cvParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.PIA_PROTEIN_INFERENCE,
                getAppliedProteinInference().getShortName());
        proteinDetectionProtocol.getAnalysisParams().getCvParam().add(cvParam);

        // inference filters
        for (AbstractFilter filter
                : getAppliedProteinInference().getFilters()) {
            cvParam = MzIdentMLTools.createPSICvParam(
                    OntologyConstants.PIA_PROTEIN_INFERENCE_FILTER,
                    filter.toString());

            proteinDetectionProtocol.getAnalysisParams().getCvParam().add(cvParam);
        }

        // scoring method
        cvParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.PIA_PROTEIN_INFERENCE_SCORING,
                getAppliedProteinInference().getScoring().getShortName());
        proteinDetectionProtocol.getAnalysisParams().getCvParam().add(cvParam);

        // score used for scoring
        cvParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.PIA_PROTEIN_INFERENCE_USED_SCORE,
                getAppliedProteinInference().getScoring().getScoreSetting().getValue());
        proteinDetectionProtocol.getAnalysisParams().getCvParam().add(cvParam);

        // PSMs used for scoring
        cvParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.PIA_PROTEIN_INFERENCE_USED_PSMS,
                getAppliedProteinInference().getScoring().getPSMForScoringSetting().getValue());
        proteinDetectionProtocol.getAnalysisParams().getCvParam().add(cvParam);

        proteinDetectionProtocol.setThreshold(new ParamList());
        if (filterExport && (getReportFilters().size() > 0)) {
            for (AbstractFilter filter : getReportFilters()) {
                if (RegisteredFilters.PROTEIN_SCORE_FILTER.getShortName().equals(filter.getShortName())) {
                    // if score filters are set, they are the threshold
                    cvParam = MzIdentMLTools.createPSICvParam(
                            OntologyConstants.PIA_PROTEIN_SCORE,
                            filter.getFilterValue().toString());
                    proteinDetectionProtocol.getThreshold().getCvParam().add(cvParam);
                } else {
                    // all other report filters are AnalysisParams
                    cvParam = MzIdentMLTools.createPSICvParam(
                            OntologyConstants.PIA_FILTER,
                            filter.toString());
                    proteinDetectionProtocol.getAnalysisParams().getCvParam().add(cvParam);
                }
            }
        }
        if ((proteinDetectionProtocol.getThreshold().getCvParam().size() < 1) &&
                (proteinDetectionProtocol.getThreshold().getUserParam().size() < 1)) {
            // no threshold was defined
            cvParam = MzIdentMLTools.createPSICvParam(
                    OntologyConstants.NO_THRESHOLD,
                    null);
            proteinDetectionProtocol.getThreshold().getCvParam().add(cvParam);
        }

        // create the proteinDetectionList
        ProteinDetectionList proteinDetectionList = new ProteinDetectionList();
        proteinDetectionList.setId("protein_report");

        Map<String, SpectrumIdentificationItem> combinedSpecIdItemMap =
                new HashMap<String, SpectrumIdentificationItem>();

        Map<String, SpectrumIdentificationResult> combinedSpecIdResMap =
                new HashMap<String, SpectrumIdentificationResult>();

        Integer thresholdPassingPAGcount = 0;
        for (ReportProtein protein : reportProteins) {
            ProteinAmbiguityGroup pag = new ProteinAmbiguityGroup();

            pag.setId("PAG_" + protein.getID());
            proteinDetectionList.getProteinAmbiguityGroup().add(pag);

            Boolean passThreshold = true;
            if (filterExport && (reportFilters.size() > 0)) {
                passThreshold = FilterFactory.satisfiesFilterList(
                        protein, 0L, reportFilters);
            }

            if (passThreshold) {
                thresholdPassingPAGcount++;
            }

            cvParam = MzIdentMLTools.createPSICvParam(
                    OntologyConstants.PROTEIN_GROUP_PASSES_THRESHOLD,
                    passThreshold.toString());
            pag.getCvParam().add(cvParam);

            cvParam = MzIdentMLTools.createPSICvParam(
                    OntologyConstants.CLUSTER_IDENTIFIER,
                    Long.toString(protein.getAccessions().get(0).getGroup().getTreeID()));
            pag.getCvParam().add(cvParam);

            StringBuilder leadingPDHids = new StringBuilder();
            // the reported proteins/accessions are the "main" proteins
            for (Accession acc : protein.getAccessions()) {
                ProteinDetectionHypothesis pdh =
                        createPDH(acc, protein, passThreshold, pag.getId(),
                                sequenceMap, peptideMap, pepEvidenceMap,
                                combinedSpecIdItemMap, combinedSpecIdResMap);

                cvParam = MzIdentMLTools.createPSICvParam(
                        OntologyConstants.LEADING_PROTEIN,
                        null);
                pdh.getCvParam().add(cvParam);

                leadingPDHids.append(pdh.getId());
                leadingPDHids.append(" ");

                pag.getProteinDetectionHypothesis().add(pdh);
            }

            if (pag.getProteinDetectionHypothesis().size() > 1) {
                for (ProteinDetectionHypothesis pdh
                        : pag.getProteinDetectionHypothesis()) {
                    StringBuilder otherPDHs =
                            new StringBuilder(leadingPDHids.length());

                    for (ProteinDetectionHypothesis others
                            : pag.getProteinDetectionHypothesis()) {
                        if (!others.equals(pdh)) {
                            otherPDHs.append(others.getId());
                            otherPDHs.append(" ");
                        }
                    }

                    cvParam = MzIdentMLTools.createPSICvParam(
                            OntologyConstants.SEQUENCE_SAME_SET_PROTEIN,
                            otherPDHs.toString().trim());
                    pdh.getCvParam().add(cvParam);
                }
            }

            // now add the sub-proteins
            for (ReportProtein subProtein : protein.getSubSets()) {
                List<ProteinDetectionHypothesis> samePDHs =
                        new ArrayList<ProteinDetectionHypothesis>();

                for (Accession subAcc : subProtein.getAccessions()) {
                    ProteinDetectionHypothesis pdh =
                            createPDH(subAcc, subProtein,
                                    false, pag.getId(),
                                    sequenceMap, peptideMap, pepEvidenceMap,
                                    combinedSpecIdItemMap, combinedSpecIdResMap);

                    cvParam = MzIdentMLTools.createPSICvParam(
                            OntologyConstants.NON_LEADING_PROTEIN,
                            null);
                    pdh.getCvParam().add(cvParam);

                    cvParam = MzIdentMLTools.createPSICvParam(
                            OntologyConstants.SEQUENCE_SUB_SET_PROTEIN,
                            leadingPDHids.toString().trim());
                    pdh.getCvParam().add(cvParam);

                    pag.getProteinDetectionHypothesis().add(pdh);
                    samePDHs.add(pdh);
                }

                if (samePDHs.size() > 1) {
                    for (ProteinDetectionHypothesis pdh
                            : samePDHs) {
                        StringBuilder otherPDHs =
                                new StringBuilder(leadingPDHids.length());

                        for (ProteinDetectionHypothesis others
                                : samePDHs) {
                            if (!others.equals(pdh)) {
                                otherPDHs.append(others.getId());
                                otherPDHs.append(" ");
                            }
                        }

                        cvParam = MzIdentMLTools.createPSICvParam(
                                OntologyConstants.SEQUENCE_SAME_SET_PROTEIN,
                                otherPDHs.toString().trim());
                        pdh.getCvParam().add(cvParam);
                    }
                }
            }
        }

        cvParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.COUNT_OF_IDENTIFIED_PROTEINS,
                thresholdPassingPAGcount.toString());
        proteinDetectionList.getCvParam().add(cvParam);

        // create the combinedSil
        SpectrumIdentificationList combinedSil = new SpectrumIdentificationList();
        combinedSil.setId("combined_inference_PSMs");
        for (SpectrumIdentificationResult idResult : combinedSpecIdResMap.values()) {
            combinedSil.getSpectrumIdentificationResult().add(idResult);
        }

        // the "final PSM list" flag
        cvParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.FINAL_PSM_LIST,
                null);
        combinedSil.getCvParam().add(cvParam);

        silMap.put(0L, combinedSil);
        combinedSpecIdItemMap = null;
        combinedSpecIdResMap = null;

        // create the protocol and SpectrumIdentification for combined PSMs
        SpectrumIdentification combiningId =
                psmModeller.createCombinedSpectrumIdentification(
                        piaAnalysisSoftware, getAppliedProteinInference().getFilters());

        analysisCollection.getSpectrumIdentification().add(combiningId);
        combiningId.setSpectrumIdentificationList(combinedSil);
        // TODO: set the "maximal/widest" parameters for the combiningId (all mods, widest tolerances... see doc)

        analysisProtocolCollection.getSpectrumIdentificationProtocol()
        .add(combiningId.getSpectrumIdentificationProtocol());

        // create the ProteinDetection for PIAs protein inference
        ProteinDetection proteinDetection = new ProteinDetection();
        analysisCollection.setProteinDetection(proteinDetection);
        proteinDetection.setId("PIA_protein_inference");
        proteinDetection.setName("PIA protein inference");
        proteinDetection.setProteinDetectionList(proteinDetectionList);
        proteinDetection.setProteinDetectionProtocol(proteinDetectionProtocol);
        InputSpectrumIdentifications inputSpecIDs = new InputSpectrumIdentifications();
        inputSpecIDs.setSpectrumIdentificationList(silMap.get(0L));
        proteinDetection.getInputSpectrumIdentifications().add(inputSpecIDs);

        m.marshal(analysisCollection, writer);
        writer.write("\n");

        m.marshal(analysisProtocolCollection, writer);
        writer.write("\n");

        writer.write(m.createDataCollectionStartTag() + "\n");

        m.marshal(inputs, writer);
        writer.write("\n");


        writer.write(m.createAnalysisDataStartTag() + "\n");

        // write out the spectrumIdentificationLists
        // TODO: clean up, only export one final list from now on!
        for (SpectrumIdentificationList siList : silMap.values()) {
            m.marshal(siList, writer);
            writer.write("\n");
        }

        m.marshal(proteinDetectionList, writer);
        writer.write("\n");

        writer.write(m.createAnalysisDataClosingTag() + "\n");

        writer.write(m.createDataCollectionClosingTag() + "\n");

        writer.write(m.createMzIdentMLClosingTag());

        writer.flush();
        LOGGER.info("writing of mzIdentML done");
    }


    /**
     * Creates a {@link ProteinDetectionHypothesis} (PDH) from the given
     * information.
     *
     * @param acc the accession for this PDH of the associated protein
     * @param protein the actual {@link ReportProtein}, which can have multiple
     * {@link Accession}s
     * @param passThreshold whether the PDH's passThreshold value is true or
     * false
     * @param PAGid the ID of the associated {@link ProteinAmbiguityGroup},
     * needed to cunstruct a scope-valid ID
     * @param sequenceMap a map holding the DBSequences, mapping from their IDs
     * @param pepEvidenceMap a map holding the PeptideEvidences, mapping from
     * their IDs
     * @param combinedSiiMap
     * @return
     */
    private ProteinDetectionHypothesis createPDH(Accession acc,
            ReportProtein protein,
            Boolean passThreshold,
            String PAGid,
            Map<String, DBSequence> sequenceMap,
            Map<String, Peptide> peptideMap,
            Map<String, PeptideEvidence> pepEvidenceMap,
            Map<String, SpectrumIdentificationItem> combinedSpecIdItemMap,
            Map<String, SpectrumIdentificationResult> combinedSpecIdResMap) {
        ProteinDetectionHypothesis pdh =
                new ProteinDetectionHypothesis();

        pdh.setId("PDH_" + acc.getAccession() + "_" + PAGid);

        DBSequence dbSequence = sequenceMap.get(acc.getAccession());
        if (dbSequence != null) {
            pdh.setDBSequence(dbSequence);
        }

        pdh.setPassThreshold(passThreshold);

        int nrInputFiles = psmModeller.getFiles().size()-1;

        String scoreShort = appliedProteinInference.getScoring().getScoreSetting().getValue();
        Map<String, PeptideHypothesis> peptideHypotheses = new HashMap<String, PeptideHypothesis>();

        for (ReportPeptide pep : protein.getPeptides()) {
            for (PSMReportItem psmItem : pep.getPSMs()) {
                // sort the PSMs' SpectrumIdentificationItems into the PeptideHypotheses

                Set<String> peptideEvidenceIDs = new HashSet<String>();

                boolean foundOccurrence = false;

                for (AccessionOccurrence occurrence
                        : psmItem.getPeptide().getAccessionOccurrences()) {
                    if (acc.getAccession().equals(
                            occurrence.getAccession().getAccession())) {
                        peptideEvidenceIDs.add(
                                psmModeller.createPeptideEvidenceID(
                                        psmItem.getPeptideStringID(true),
                                        occurrence.getStart(),
                                        occurrence.getEnd(),
                                        acc));

                        foundOccurrence = true;

                        // there might be multiple occurrences per accession, so no loop-break here
                    }
                }

                if (!foundOccurrence) {
                    peptideEvidenceIDs.add(
                            psmModeller.createPeptideEvidenceID(
                                    psmItem.getPeptideStringID(true),
                                    null, null, acc));
                }

                for (String evidenceID : peptideEvidenceIDs) {
                    PeptideHypothesis ph =
                            peptideHypotheses.get(evidenceID);
                    if (ph == null) {
                        ph = new PeptideHypothesis();
                        ph.setPeptideEvidence(
                                pepEvidenceMap.get(evidenceID));

                        if (ph.getPeptideEvidence() == null) {
                            LOGGER.error("could not find peptideEvidence for '" + evidenceID + "'! "
                                    + "This may happen, if you use different accessions in your databases/search engines.");
                            return null;
                        }

                        peptideHypotheses.put(evidenceID, ph);
                        pdh.getPeptideHypothesis().add(ph);
                    }

                    String spectrumItemID;
                    if (!psmModeller.getCreatePSMSets()) {
                        spectrumItemID =
                                psmModeller.getSpectrumIdentificationItemID(
                                        psmItem,
                                        ((ReportPSMSet) psmItem).getPSMs().get(0).getFileID());
                        spectrumItemID += ":set";
                    } else {
                        spectrumItemID =
                                psmModeller.getSpectrumIdentificationItemID(
                                        psmItem, 0L);
                    }

                    SpectrumIdentificationItem specIdItem =
                            combinedSpecIdItemMap.get(spectrumItemID);
                    if (specIdItem == null) {
                        // if the spectrumIdentificationItem is not yet set,
                        // create it (and put it into the SpectrumIdentificationResult)
                        specIdItem = psmModeller.putPsmInSpectrumIdentificationResultMap(
                                        psmItem,
                                        combinedSpecIdResMap,
                                        peptideMap,
                                        pepEvidenceMap,
                                        null,
                                        false);

                        ScoreModel compareScore =
                                psmItem.getCompareScore(scoreShort);
                        if ((compareScore != null) && (nrInputFiles > 1)) {
                            if (!compareScore.getType().equals(ScoreModelEnum.UNKNOWN_SCORE)) {
                                CvParam tempCvParam = new CvParam();
                                tempCvParam.setAccession(compareScore.getAccession());
                                tempCvParam.setCv(MzIdentMLTools.getCvPSIMS());
                                tempCvParam.setName(compareScore.getType().getCvName());
                                tempCvParam.setValue(compareScore.getValue().toString());

                                specIdItem.getCvParam().add(tempCvParam);
                            } else {
                                // TODO: add unknown scores...
                            }
                        }

                        combinedSpecIdItemMap.put(spectrumItemID, specIdItem);
                    }

                    SpectrumIdentificationItemRef ref =
                            new SpectrumIdentificationItemRef();
                    ref.setSpectrumIdentificationItem(specIdItem);
                    ph.getSpectrumIdentificationItemRef().add(ref);
                }
            }
        }

        CvParam cvParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.PIA_PROTEIN_SCORE,
                protein.getScore().toString());
        pdh.getCvParam().add(cvParam);

        return pdh;
    }


    /**
     * Processes the command line on the protein level.
     *
     * @param model
     * @param commands
     * @return
     */
    public static boolean processCLI(ProteinModeller proteinModeller, PIAModeller piaModeller, String[] commands) {
        if (proteinModeller == null) {
            LOGGER.error("No protein modeller given while processing CLI " +
                    "commands");
            return false;
        }

        if (piaModeller == null) {
            LOGGER.error("No PIA modeller given while processing CLI commands");
            return false;
        }

        Pattern pattern = Pattern.compile("^([^=]+)=(.*)");
        Matcher commandParamMatcher;

        for (String command : commands) {
            String[] params = null;
            commandParamMatcher = pattern.matcher(command);

            if (commandParamMatcher.matches()) {
                command = commandParamMatcher.group(1);
                params = commandParamMatcher.group(2).split(",");
            }

            try {
                ProteinExecuteCommands.valueOf(command).execute(proteinModeller, piaModeller, params);
            } catch (IllegalArgumentException e) {
                LOGGER.error("Could not process unknown call to " + command, e);
            }
        }

        return true;
    }
}