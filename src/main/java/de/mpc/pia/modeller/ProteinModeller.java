package de.mpc.pia.modeller;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.Accession;
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
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.modeller.score.comparator.RankCalculator;


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

        this.reportFilters = new ArrayList<>();
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
        List<String> scoreShortNames = new ArrayList<>();

        // get the scores from the files
        for (Long fileID : psmModeller.getFiles().keySet()) {
            psmModeller.getScoreShortNames(fileID).stream().filter(scoreShort -> !scoreShortNames.contains(scoreShort)).forEach(scoreShortNames::add);
        }

        return scoreShortNames;
    }


    /**
     * Returns the Score name, given the scoreShortName.
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
        reportProteins = new ArrayList<>();

        if (proteinInference != null) {
            appliedProteinInference = proteinInference;
            reportProteins = proteinInference.calculateInference(
                    intermediateGroups,
                    psmModeller.getReportPSMSets(),
                    peptideModeller.getConsiderModifications(),
                    psmModeller.getPSMSetSettings(),
                    peptideModeller.getFilteredReportPeptides(0L, peptideModeller.getFilters(0L)));
        } else {
            LOGGER.error("No inference method set!");
            appliedProteinInference = null;
            reportProteins = null;
        }

        this.fdrData = new FDRData(defaultDecoyStrategy, defaultDecoyPattern, defaultFDRThreshold);

        // create the protein map
        reportProteinsMap = new HashMap<>();
        for (ReportProtein protein : reportProteins) {
            reportProteinsMap.put(protein.getID(), protein);

            if (!protein.getSubSets().isEmpty()) {
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
            if (protein.getRank() != null) {
                return true;
            }
        }

        return false;
    }


    /**
     * Calculates the ranking. If the filter List is not null or empty, the
     * Report is filtered before ranking.
     *
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

        RankCalculator.calculateRanking(ScoreModelEnum.PROTEIN_SCORE.getShortName(), FilterFactory.applyFilters(reportProteins, filters), comparator);
    }


    /**
     * Resorts the report with the given sorting parameters
     */
    public void sortReport(List<String> sortOrders,
            Map<String, SortOrder> sortables) {

        List<Comparator<ReportProtein>> compares =
                new ArrayList<>();

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

        this.fdrData = new FDRData(defaultDecoyStrategy, defaultDecoyPattern, defaultFDRThreshold);
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
     */
    public void calculateFDR() {
        // calculate the FDR values
        fdrData.calculateFDR(reportProteins, true);
    }


    /**
     * Returns the report filters.
     * @return
     */
    public List<AbstractFilter> getReportFilters() {
        if (reportFilters == null) {
            reportFilters = new ArrayList<>();
        }

        return reportFilters;
    }


    /**
     * Add a new filter to the report filters.
     */
    public boolean addReportFilter(AbstractFilter newFilter) {
        return newFilter != null && getReportFilters().add(newFilter);
    }


    /**
     * Removes the report filter at the given index.
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
     * @return
     */
    public List<AbstractFilter> getInferenceFilters() {
        if (inferenceFilters == null) {
            inferenceFilters = new ArrayList<>();
        }

        return inferenceFilters;
    }


    /**
     * Add a new filter to the inference filters. These are not the currently
     * set filters, but a list of filters which may be used for inference.
     */
    public boolean addInferenceFilter(AbstractFilter newFilter) {
        return newFilter != null && getInferenceFilters().add(newFilter);
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
     * Processes the command line on the protein level.
     *
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