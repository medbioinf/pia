package de.mpc.pia.modeller;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.modeller.peptide.PeptideExecuteCommands;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.peptide.ReportPeptideComparatorFactory;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.SortOrder;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.FDRScore;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;
import de.mpc.pia.modeller.score.comparator.RankCalculator;
import de.mpc.pia.modeller.score.comparator.ScoreComparator;


/**
 * Modeller for peptide related stuff.
 *
 * @author julianu
 *
 */
public class PeptideModeller implements Serializable {

    private static final long serialVersionUID = -5899342615041591821L;


    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(PeptideModeller.class);


    /** the used {@link PSMModeller} */
    private final PSMModeller psmModeller;

    /** maps from the file ID to the List of {@link ReportPeptide}s */
    private Map<Long, List<ReportPeptide>> fileReportPeptides;

    /** the list of filters applied to the data, mapped by the file ID */
    private Map<Long, List<AbstractFilter>> fileFiltersMap;

    /** maps from the fileID to whether the peptides need to be inferred on next call (not set means, they have to be inferred) */
    private Map<Long, Boolean> inferePeptides;

    /** maps from the fileID to the corresponding FDR data */
    private Map<Long, FDRData> fileFDRData;

    /** maps from the fileID to whether an FDR is calculated or not */
    private Map<Long, Boolean> fileFDRCalculated;

    /** whether to consider the modifications for building peptides or not */
    private boolean considerModifications;


    /** the default value for considering the modifications
     *  TODO: default value for considerModifications should be loaded from ini-file
     */
    public static final boolean CONSIDER_MODIFICATIONS_DEFAULT = false;




    public PeptideModeller(PSMModeller psmModeller) {
        this.psmModeller = psmModeller;

        if (psmModeller == null) {
            throw new IllegalArgumentException("The given PSMModeller is null!");
        }

        fileReportPeptides = new HashMap<>();
        fileFiltersMap = new HashMap<>();
        inferePeptides = new HashMap<>();

        // initialize the FDR data maps
        fileFDRData = new HashMap<>();
        fileFDRCalculated = new HashMap<>();

        this.considerModifications = CONSIDER_MODIFICATIONS_DEFAULT;
    }


    /**
     * Getter for the files used in the PIA intermediate file, including the
     * pseudo-overview-file.<br/>
     * The files are directly taken from the {@link PSMModeller}.
     *
     * @return
     */
    public Map<Long, PIAInputFile> getFiles() {
        return psmModeller.getFiles();
    }


    /**
     * Getter for the shortNames of all scores of the given file
     *
     * @param fileID
     * @return
     */
    public List<String> getScoreShortNames(Long fileID) {
        LinkedHashSet<String> scoreShortNames = new LinkedHashSet<>();

        if (fileID > 0) {
            scoreShortNames.addAll(psmModeller.getScoreShortNames(fileID));
        } else {
            for (Long file : psmModeller.getFiles().keySet()) {
                scoreShortNames.addAll(psmModeller.getScoreShortNames(file));
            }
        }

        return new ArrayList<>(scoreShortNames);
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
     * Infere the peptides for the file given by its ID with the PSMs taken
     * from the {@link PSMModeller}. This takes any given filtering into
     * account.
     *
     * @param fileID
     */
    private void inferePeptides(Long fileID) {
        LOGGER.info("Inferring peptides for " + fileID  +
                " considerModifications=" + considerModifications);
        // first put the PSMs sorted by their stringID (this defines a peptide) into a Map
        Map<String, ReportPeptide> peptides = new HashMap<>();

        // take the (filtered) PSMs from the psmModeller
        List<PSMReportItem> reportPSMs;
        if (!fileID.equals(0L)) {
            reportPSMs = new ArrayList<>(
                    psmModeller.getFilteredReportPSMs(fileID, getFilters(fileID)));
        } else {
            reportPSMs = new ArrayList<>(
                    psmModeller.getFilteredReportPSMSets(getFilters(fileID)));
        }

        for (PSMReportItem psm : reportPSMs) {
            String idString =
                    ReportPeptide.createStringID(psm, considerModifications);
            ReportPeptide repPeptide = peptides.get(idString);
            if (repPeptide == null) {
                repPeptide = new ReportPeptide(psm.getSequence(), idString,
                        psm.getPeptide());
                peptides.put(idString, repPeptide);
            }
            repPeptide.addPSM(psm);
        }

        // create a List of the Map
        List<ReportPeptide> repList = new ArrayList<>(peptides.size());
        repList.addAll(peptides.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList()));

        // put this new list into the peptides' list
        fileReportPeptides.put(fileID, repList);
        // this file is set
        inferePeptides.put(fileID, false);
        LOGGER.info("Inferred " + repList.size() + " peptides for " + fileID);


        // peptides are changed -> reset the FDR data
        Boolean fileHasFDR = fileFDRCalculated.get(fileID);
        if ((fileHasFDR != null) && fileHasFDR) {
            fileFDRCalculated.put(fileID, false);
            calculateFDR(fileID);
        }
    }


    /**
     * Returns the filters set for the given file.
     * @param fileID
     * @return
     */
    public List<AbstractFilter> getFilters(Long fileID) {
        List<AbstractFilter> filters = fileFiltersMap.get(fileID);
        if (filters == null) {
            filters = new ArrayList<>();
            fileFiltersMap.put(fileID, filters);
        }

        return filters;
    }


    /**
     * Add a new filter for the given file
     */
    public boolean addFilter(Long fileID, AbstractFilter newFilter) {
        if (newFilter != null) {
            boolean ok = getFilters(fileID).add(newFilter);
            if (ok) {
                inferePeptides.put(fileID, true);
            }
            return ok;
        } else {
            return false;
        }
    }


    /**
     * Removes the filter from the given file at the given index.
     * @param fileID
     * @param removingIndex
     * @return
     */
    public AbstractFilter removeFilter(Long fileID, int removingIndex) {
        List<AbstractFilter> filters = getFilters(fileID);

        if ((removingIndex >= 0) &&
                (filters != null) &&
                (removingIndex < filters.size())) {
            AbstractFilter removed = filters.remove(removingIndex);
            if (removed != null) {
                inferePeptides.put(fileID, true);
            }
            return removed;
        }

        return null;
    }


    /**
     * Removes all given filters.
     */
    public void removeAllFilters() {
        fileFiltersMap.clear();

        // everything should be new inferred
        inferePeptides.clear();
    }


    /**
     * Getter for considerModifications
     * @return
     */
    public Boolean getConsiderModifications() {
        return considerModifications;
    }


    /**
     * Setter for considerModifications
     * @return
     */
    public void setConsiderModifications(boolean considerMods) {
        this.considerModifications = considerMods;
    }


    /**
     * Applies the general settings and sets the inference of peptides on next
     * call.
     *
     * @param considerModifications
     */
    public void applyGeneralSettings(boolean considerModifications) {
        if (this.considerModifications != considerModifications) {
            this.considerModifications = considerModifications;
            // clearing the values means, the peptides for all files and the
            // overview should be inferred
            inferePeptides.clear();
        }
    }


    /**
     * Returns a List of {@link ReportPeptide}s for the given fileID filtered
     * by the given filters.
     *
     * @param fileID
     * @param filters
     * @return
     */
    public List<ReportPeptide> getFilteredReportPeptides(Long fileID,
            List<AbstractFilter> filters) {
        Boolean infere = inferePeptides.get(fileID);
        if ((infere == null) || infere) {
            inferePeptides(fileID);
        }

        if (fileReportPeptides.containsKey(fileID)) {
            return FilterFactory.applyFilters(fileReportPeptides.get(fileID),
                    filters, fileID);
        } else {
            LOGGER.error("There are no ReportPeptides for the fileID " + fileID);
            return new ArrayList<>(0);
        }
    }


    /**
     * Resorts the file report with the given sorting parameters
     */
    public void sortReport(Long fileID, List<String> sortOrders,
            Map<String, SortOrder> sortables) {
        List<Comparator<ReportPeptide>> compares =
                sortOrders.stream().map(sortKey -> ReportPeptideComparatorFactory.getComparatorByName(
                        sortKey,
                        sortables.get(sortKey))).collect(Collectors.toList());

        if (fileReportPeptides.get(fileID) != null) {
            Collections.sort(fileReportPeptides.get(fileID),
                    ReportPeptideComparatorFactory.getComparator(compares));
        }
    }


    /**
     * Returns a List of scoreShortNames of available Scores for ranking.<br/>
     * Same as {@link PSMModeller#getFilesAvailableScoreShortsForRanking(Long)}.
     *
     * @param fileID
     * @return
     */
    public List<String> getFilesAvailableScoreShortsForRanking(Long fileID) {
        List<String> rankingScoreNames = getScoreShortNames(fileID);

        if (rankingScoreNames.isEmpty()) {
            LOGGER.error("No scores available for ranking for the file with ID "+fileID);
        }

        return rankingScoreNames;
    }


    /**
     * Calculates the ranking for the given file and rankableShortName. If the
     * filter List is not null or empty, the Report is filtered before ranking.
     */
    public void calculateRanking(Long fileID, String rankableShortName,
            List<AbstractFilter> filters) {
        if ((rankableShortName == null) || rankableShortName.trim().isEmpty()) {
            LOGGER.error("No score SHORT_NAME given for ranking calculation.");
            return;
        }

        // first, dump all prior ranking
        List<ReportPeptide> reports = fileReportPeptides.get(fileID);
        if (reports != null) {
            for (ReportPeptide pep : reports) {
                pep.setRank(-1L);
            }
        }

        // calculate the new ranking
        RankCalculator.calculateRanking(rankableShortName,
                FilterFactory.applyFilters(
                        fileReportPeptides.get(fileID),
                        filters, fileID),
                new ScoreComparator<>(rankableShortName));
    }



    /**
     * Writes the peptide report for the file with the given ID and optionally
     * filtered with the filters of this file in a loose CSV format.
     *
     * @param writer
     * @param fileID
     * @param filterExport
     * @param oneAccessionPerLine
     * @throws IOException
     */
    public void exportCSV(Writer writer, Long fileID, boolean filterExport,
            boolean oneAccessionPerLine, boolean includePSMSets,
            boolean includePSMs) throws IOException {
        boolean includes = includePSMSets || includePSMs;
        List<ReportPeptide> report = getFilteredReportPeptides(fileID,
                filterExport ? getFilters(fileID) : null);
        List<String> scoreShorts = getScoreShortNames(fileID);

        String separator = ",";

        // write header information
        if (includes) {
            writer.append("\"COLS_PEPTIDES\"" + separator);
        }

        writer.append("\"sequence\"" + separator);

        if (considerModifications) {
            writer.append("\"modifications\"" + separator);
        }

        if (oneAccessionPerLine) {
            writer.append("\"accession\"" + separator);
        } else {
            writer.append("\"accessions\"" + separator);
        }

        writer.append(
                "\"#spectra\"" + separator +
                "\"#PSM sets\"" + separator);

        if (fileID > 0) {
            writer.append("\"missed\"" + separator);
        }

        writer.append("\"unique\"");

        for (String scoreShort : scoreShorts) {
            writer.append(separator + "\"best_" + scoreShort + "\"");
        }

        writer.append("\n");

        if (includePSMSets) {
            writer.append(
                    "\"COLS_PSMSET\"" + separator +
                    "\"sequence\"" + separator);

            if (considerModifications) {
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

            if (considerModifications) {
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

        // write out peptide information
        for (ReportPeptide peptide : report) {
            StringBuilder lineFirst = new StringBuilder(64);    // first part of the line, up to the accession(s)
            StringBuilder lineLast = new StringBuilder(64);     // last part of the line, from the accession(s) to end
            if (includes) {
                lineFirst.append("\"PEPTIDE\"" + separator);
            }

            lineFirst.append("\"" + peptide.getSequence() + "\"" + separator);

            if (considerModifications) {
                lineFirst.append("\"" +
                        peptide.getPSMs().get(0).getModificationsString() +
                        "\"" + separator);
            }

            lineLast.append(
                    "\"" + peptide.getNrSpectra() + "\"" + separator +
                    "\"" + peptide.getNrPSMs() + "\"" + separator);


            if (fileID > 0) {
                lineLast.append("\"" + peptide.getMissedCleavages() + "\"" + separator);
            }

            if (peptide.getAccessions().size() > 1) {
                lineLast.append("\"false\"");
            } else {
                lineLast.append("\"true\"");
            }

            for (String scoreShort : scoreShorts) {
                lineLast.append(separator + "\"" + peptide.getBestScore(scoreShort) + "\"");
            }

            // either cumulate the accessions or write out one line per accession
            if (oneAccessionPerLine) {
                for (Accession acc : peptide.getAccessions()) {
                    writer.append(lineFirst);
                    writer.append("\"" + acc.getAccession() + "\"" + separator);
                    writer.append(lineLast);
                    writer.append("\n");
                }
            } else {
                StringBuilder accessionsSB = new StringBuilder(64);
                for (Accession acc : peptide.getAccessions()) {
                    if (accessionsSB.length() > 0) {
                        accessionsSB.append(",");
                    }

                    accessionsSB.append(
                            URLEncoder.encode(acc.getAccession(), "UTF-8").
                            replace("+", "%20"));
                }

                writer.append(lineFirst);
                writer.append("\"" + accessionsSB + "\"" + separator);
                writer.append(lineLast);
                writer.append("\n");
            }

            if (includePSMSets || includePSMs) {

                for (PSMReportItem psm : peptide.getPSMs()) {
                    StringBuilder row = new StringBuilder();

                    row.append("\"" + psm.getSequence() + "\"" + separator);

                    if (considerModifications) {
                        row.append("\""  + psm.getModificationsString() + "\"" + separator);
                    }

                    if (psm instanceof ReportPSMSet) {
                        row.append("\"" +
                                ((ReportPSMSet) psm).getPSMs().size() + "\"" + separator);
                    }

                    row.append("\"" + psm.getCharge() + "\"" + separator +
                            "\"" + psm.getMassToCharge() + "\"" + separator +
                            "\"" + psm.getDeltaMass() + "\"" + separator +
                            "\"" + psm.getDeltaPPM() + "\"" + separator +
                            "\"" + psm.getRetentionTime() + "\"" + separator +
                            "\"" + psm.getMissedCleavages() + "\"" + separator +
                            "\"" + psm.getSourceID() + "\"" + separator +
                            "\"" + psm.getSpectrumTitle() + "\"" + separator +
                            "\"" + psm.getScoresString() + "\"");


                    if (psm instanceof ReportPSM) {
                        // write the input file name before the remaining row
                        if (includePSMs) {
                            writer.append("\"PSM\"" + separator + "\"" +
                                    ((ReportPSM) psm).getInputFileName() + "\"" + separator);
                            writer.append(row.toString());
                            writer.append("\n");
                        }
                    } else if (psm instanceof ReportPSMSet) {
                        if (includePSMSets) {
                            writer.append("\"PSMSET\"" + separator);
                            writer.append(row.toString());
                            writer.append("\n");
                        }

                        // include the PSMs, if set
                        if (includePSMs) {
                            for (ReportPSM p : ((ReportPSMSet) psm).getPSMs()) {
                                writer.append("\"PSM\"" + separator + "\""
                                        + p.getInputFileName() + "\"" + separator
                                        + "\"" + p.getSequence() + "\"" + separator);

                                if (considerModifications) {
                                    writer.append("\""  + p.getModificationsString() + "\"" + separator);
                                }

                                writer.append("\"" + p.getCharge() + "\"" + separator
                                        + "\"" + p.getMassToCharge() + "\"" + separator
                                        + "\"" + p.getDeltaMass() + "\"" + separator
                                        + "\"" + p.getDeltaPPM() + "\"" + separator
                                        + "\"" + p.getRetentionTime() + "\"" + separator
                                        + "\"" + p.getMissedCleavages() + "\"" + separator
                                        + "\"" + p.getSourceID() + "\"" + separator
                                        + "\"" + p.getSpectrumTitle() + "\"" + separator
                                        + "\"" + p.getScoresString() + "\"");
                                writer.append("\n");
                            }
                        }
                    }
                }
            }
        }

        writer.flush();
    }


    /**
     * Processes the command line on the peptide level.
     *
     * @param commands
     * @return
     */
    public static boolean processCLI(PeptideModeller peptideModeller, PIAModeller piaModeller, String[] commands) {
        if (peptideModeller == null) {
            LOGGER.error("No peptide modeller given while processing CLI commands");
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
                PeptideExecuteCommands.valueOf(command).execute(peptideModeller, piaModeller, params);
            } catch (IllegalArgumentException e) {
                LOGGER.error("Could not process unknown call to " + command, e);
            }
        }

        return true;
    }


    /**
     * Calculate the peptide FDR for the file given by fileID. The settings for
     * the calculation of the FDR are taken from the PSM level. If the FDR on
     * the PSM level was calculated, the FDRScore respectively CombinedFDRScore
     * is used as base score for peptide level FDR. Otherwise the currently set
     * score or preferred score is used.
     *
     * @param fileID
     */
    public void calculateFDR(Long fileID) {
        FDRData fdrData = getFDRDataFromPSMLevel(fileID);
        fileFDRData.put(fileID, fdrData);

        fileFDRCalculated.put(fileID, false);


        String baseScoreShort = getBaseFDRScorePSMLevel(fileID);
        if (baseScoreShort == null) {
            LOGGER.error("Could not get a valid score from PSM level!");
            return;
        }

        fdrData.setScoreShortName(baseScoreShort);
        LOGGER.info("set the score for peptide FDR calculation for fileID=" +
                fileID + ": " + fdrData.getScoreShortName());

        // recalculate the decoy status (especially important, if decoy pattern was changed)
        updateDecoyStates(fileID);

        if (fileReportPeptides.get(fileID) == null) {
            LOGGER.error("No peptides found for the file with ID=" + fileID);
            return;
        }

        // create new list of the filters and leave only the PSM level filters
        List<AbstractFilter> filters = new ArrayList<>(getFilters(fileID));
        ListIterator<AbstractFilter> filterIt = filters.listIterator();

        while (filterIt.hasNext()) {
            AbstractFilter filter = filterIt.next();
            RegisteredFilters regFilter = filter.getRegisteredFilter();
            if (!RegisteredFilters.getPSMFilters().contains(regFilter)) {
                filterIt.remove();
            }
        }

        // get a List of the ReportPeptides for FDR calculation
        List<ReportPeptide> listForFDR = new ArrayList<>(
                getFilteredReportPeptides(fileID, filters));

        // get the comparator for the score
        boolean higherScoreBetter = psmModeller.getHigherScoreBetter(fdrData.getScoreShortName());
        Comparator<ReportPeptide> peptideComparator =
                new ScoreComparator<>(fdrData.getScoreShortName(), higherScoreBetter);

        // calculate the FDR values
        fdrData.calculateFDR(listForFDR, peptideComparator);

        // and also calculate the FDR score
        FDRScore.calculateFDRScore(listForFDR, fdrData, higherScoreBetter);

        // the FDR for this file is calculated now
        fileFDRCalculated.put(fileID, true);
    }


    /**
     * Creates a new instance of {@link FDRData} with the same settings, that
     * were either set on the PSM level or already used for FDR calculation on
     * PSM level. If the {@link FDRData} for the given file is not set on PSM
     * level, default settings (defined on PSM level) are used.
     *
     * @param fileID
     * @return
     */
    private FDRData getFDRDataFromPSMLevel(Long fileID) {
        FDRData psmFdrData = psmModeller.getFileFDRData().get(fileID);

        FDRData fdrData;
        if (psmFdrData != null) {
            // take strategy from PSM level
            fdrData = new FDRData(
                    psmFdrData.getDecoyStrategy(),
                    psmFdrData.getDecoyPattern(),
                    psmFdrData.getFDRThreshold());
        } else {
            // create strategy with default settings
            fdrData = new FDRData(
                    DecoyStrategy.getStrategyByString(psmModeller.getDefaultDecoyPattern()),
                    psmModeller.getDefaultDecoyPattern(),
                    psmModeller.getDefaultFDRThreshold());
        }

        return fdrData;
    }


    /**
     * Returns the base score for the FDR score calculation based on the PSM
     * level settings.
     *
     * @param fileID
     * @return
     */
    private String getBaseFDRScorePSMLevel(Long fileID) {
        if ((fileID == 0L) && psmModeller.isCombinedFDRScoreCalculated()) {
            return ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName();
        } else if (psmModeller.isFDRCalculated(fileID)) {
            return ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName();
        }

        return psmModeller.getFilesPreferredFDRScore(fileID);
    }


    /**
     * Returns, whether the peptide FDR is calculated for the given file.
     *
     * @param fileID
     * @return
     */
    public Boolean isFDRCalculated(Long fileID) {
        if (!fileFDRCalculated.containsKey(fileID)) {
            return false;
        }
        return fileFDRCalculated.get(fileID);
    }


    /**
     * Returns the {@link FDRData} for the file with the given ID.
     *
     * @param fileID
     * @return
     */
    public FDRData getFilesFDRData(Long fileID) {
        return fileFDRData.get(fileID);
    }


    /**
     * Updates the decoy states of the peptides with the current settings from
     * the file's FDRData.
     *
     */
    private void updateDecoyStates(Long fileID) {
        FDRData fdrData = fileFDRData.get(fileID);
        LOGGER.debug("updateDecoyStates for peptides on file " + fileID);

        // select either the PSMs from the given file or all and calculate the fdr
        if (fdrData == null) {
            LOGGER.error("No FDR settings given for file with ID=" + fileID
                    + " this function must be called after getFDRDataFromPSMLevel");
        } else {
            Pattern p = Pattern.compile(fdrData.getDecoyPattern());

            List<ReportPeptide> peptidesList = getFilteredReportPeptides(fileID, null);

            // dump all FDR data, as the decoy information was changed
            for (ReportPeptide peptide : peptidesList) {
                peptide.dumpFDRCalculation();
                peptide.updateDecoyStatus(fdrData.getDecoyStrategy(), p);
            }
        }
    }
}