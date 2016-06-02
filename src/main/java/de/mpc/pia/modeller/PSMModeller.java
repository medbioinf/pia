package de.mpc.pia.modeller;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.biojava.nbio.ontology.Term;
import org.biojava.nbio.ontology.Triple;

import uk.ac.ebi.jmzidml.model.mzidml.AbstractParam;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisCollection;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisProtocolCollection;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftwareList;
import uk.ac.ebi.jmzidml.model.mzidml.CvList;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.DBSequence;
import uk.ac.ebi.jmzidml.model.mzidml.Enzyme;
import uk.ac.ebi.jmzidml.model.mzidml.FileFormat;
import uk.ac.ebi.jmzidml.model.mzidml.InputSpectra;
import uk.ac.ebi.jmzidml.model.mzidml.Inputs;
import uk.ac.ebi.jmzidml.model.mzidml.Param;
import uk.ac.ebi.jmzidml.model.mzidml.ParamList;
import uk.ac.ebi.jmzidml.model.mzidml.PeptideEvidence;
import uk.ac.ebi.jmzidml.model.mzidml.PeptideEvidenceRef;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabase;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabaseRef;
import uk.ac.ebi.jmzidml.model.mzidml.SearchModification;
import uk.ac.ebi.jmzidml.model.mzidml.SequenceCollection;
import uk.ac.ebi.jmzidml.model.mzidml.SourceFile;
import uk.ac.ebi.jmzidml.model.mzidml.SpectraData;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationItem;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationList;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationProtocol;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationResult;
import uk.ac.ebi.jmzidml.model.mzidml.UserParam;
import uk.ac.ebi.jmzidml.xml.io.MzIdentMLMarshaller;
import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.AccessionOccurrence;
import de.mpc.pia.intermediate.Group;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.intermediate.xmlhandler.PIAIntermediateJAXBHandler;
import de.mpc.pia.modeller.psm.PSMExecuteCommands;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.PSMReportItemComparator;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.SortOrder;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.report.filter.impl.PSMTopIdentificationFilter;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.FDRScore;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;
import de.mpc.pia.modeller.score.comparator.RankCalculator;
import de.mpc.pia.modeller.score.comparator.ScoreComparator;
import de.mpc.pia.tools.CleavageAgent;
import de.mpc.pia.tools.MzIdentMLTools;
import de.mpc.pia.tools.OntologyConstants;
import de.mpc.pia.tools.PIAConstants;
import de.mpc.pia.tools.obo.OBOMapper;
import de.mpc.pia.tools.unimod.UnimodParser;
import de.mpc.pia.tools.unimod.jaxb.ModT;


/**
 * Modeller for the PSM related stuff.
 *
 * @author julian
 *
 */
public class PSMModeller {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(PSMModeller.class);


    /** maps from the fileID to the {@link PIAInputFile}s, they are straight from the intermediateHandler */
    private Map<Long, PIAInputFile> inputFiles;

    /** maps from the string ID to the {@link SearchDatabase}s, they are straight from the intermediateHandler */
    private Map<String, SearchDatabase> searchDatabases;

    /** maps from the string ID to the {@link SpectraData} straight from the intermediateHandler */
    private Map<String, SpectraData> spectraData;

    /** maps from the string ID to the {@link AnalysisSoftware}s, they are straight from the intermediateHandler */
    private Map<String, AnalysisSoftware> analysisSoftware;

    /** the name of the PIA XML file */
    private String fileName;

    /** maps from the spectrum ID in the PIA intermediate file to the report PSM */
    private Map<Long, ReportPSM> spectraPSMs;

    /** maps from the file ID to the List of {@link ReportPSM}s */
    private Map<Long, List<ReportPSM> > fileReportPSMs;

    /** List of the {@link ReportPSMSet}s for the whole file */
    // TODO: make the reportPSMSets and reportPSMSetMap accessible by getter only, create on demand and null them, when the PSMSetSettings or createPSMSets are changed
    private List<ReportPSMSet> reportPSMSets;

    /** map of the ReportPSMSets, for faster access in the other modellers, this is static per PIA XML file and global settings */
    private Map<String, ReportPSMSet> reportPSMSetMap;


    /** maps from the fileID to List of short names  of the scores. Actually only a reference to the same field in the {@link PIAModeller}. */
    private Map<Long, List<String>> fileScoreShortNames;

    /** mapping from fileId to the available sortings */
    private Map<Long, Set<String>> fileSortables;

    /** maps from the fileID to the corresponding FDR data */
    private Map<Long, FDRData> fileFDRData;

    /** maps from the fileID to whether an FDR is calculated or not */
    private Map<Long, Boolean> fileFDRCalculated;

    /** map from the file's id to the used top identifications for FDR calculation */
    private Map<Long, Integer> fileTopIdentifications;

    /** Maps, whether the file has PSMs with decoy information from the searchengine or otherwise in the PIA XML defined decoys or not */
    private Map<Long, Boolean> fileHasInternalDecoy;

    /** The warnings generated by the {@link PIAIntermediateJAXBHandler} for the PSMSetSettings */
    private Map<String, Set<Long>> psmSetSettingsWarnings;

    /** The represented {@link IdentificationKeySettings} in this Set are used for the calculation of PSM sets or not */
    private Map<String, Boolean> psmSetSettings;

    /** whether to create PSM sets at all or use every PSM independently (useful for data sets with several runs and one search engine) */
    private boolean createPSMSets;

    /** a list of score shortnames, representing the preferred score for the FDR calulation (if it is not manually set) */
    private List<String> preferredFDRScores;


    /** the OBO mapper, to get additional data */
    private OBOMapper oboMapper;

    /** maps from the scoreShort to the scoreName */
    private Map<String, String> scoreShortToScoreName;

    /** maps from the scoreShort to the comparator */
    private Map<String, Comparator<PSMReportItem>> scoreShortToComparator;

    /** maps from the score short to whether the higher score is better (probably current setting) */
    private Map<String, Boolean> scoreShortToHigherScoreBetter;

    /** maps from the score short to whether the higher score better is changeable, because it was not hard coded or found in a CV */
    private Map<String, Boolean> scoreShortToHigherScoreBetterChangeable;


    /** default decoy pattern */
    private String defaultDecoyPattern;

    /** default FDR threshold */
    private Double defaultFDRThreshold;

    /** default for the number of highest ranking PSMs (per spectrum) used for FDR calculation */
    private Integer defaultFDRTopIdentifications;


    /** the list of filters applied to the data, mapped by the file ID */
    private Map<Long, List<AbstractFilter>> fileFiltersMap;


    /** the PSMSetSettings for the SpectrumIdentificationResults in mzIdentML export */
    private Map<String, Boolean> mzIdResultPSMSetSettings;

    /** the PSMSetSettings for the SpectrumIdentificationItems in mzIdentML export */
    private Map<String, Boolean> itemPSMSetSettings;


    /**
     * Basic constructor, creates the {@link ReportPSM}s and
     * {@link ReportPSMSet}s from the given {@link Group}s. The {@link Group}s
     * should derive from a {@link PIAInputFile}.
     *
     * @param groups groups of the PIA intermediate file
     * @param inputFiles the used {@link PIAInputFile}s
     * @param fileScoreNames a map, which will be filled with the scoreNames for each file
     * @param fileScoreShortNames a map, which will be filled with the scoreShortNames for each file
     */
    public PSMModeller(Map<Long, Group> groups,
            Map<Long, PIAInputFile> inputFiles,
            Map<String, SearchDatabase> searchDatabases,
            Map<String, SpectraData> spectraData,
            Map<String, AnalysisSoftware> software,
            String fileName,
            Map<String, Set<Long>> psmSetSettingsWarnings,
            int nrPSMs) {

        // create the file mapping and also add the overview file with ID 0
        this.inputFiles = new HashMap<Long, PIAInputFile>(inputFiles.size()+1);
        this.inputFiles.put(0L, new PIAInputFile(0L, "All files",
                "Overview_of_all_files", "none"));
        this.inputFiles.putAll(inputFiles);

        fileFiltersMap = new HashMap<Long, List<AbstractFilter>>(inputFiles.size());

        this.searchDatabases = searchDatabases;
        this.spectraData = spectraData;
        this.analysisSoftware = software;
        this.fileName = fileName;

        this.scoreShortToScoreName = new HashMap<String, String>();

        // TODO: get defaults from ini-file (or something like that)
        defaultDecoyPattern = "s.*";
        defaultFDRThreshold = 0.01;
        defaultFDRTopIdentifications = 0;
        preferredFDRScores = new ArrayList<String>();

        // initialize the used PSM set settings
        this.psmSetSettingsWarnings = psmSetSettingsWarnings;
        this.psmSetSettings = getMaximalPSMSetSettings();

        // remove redundant psmSetSettings (and use only the more failure tolerant ones)
        psmSetSettings = IdentificationKeySettings.noRedundantSettings(psmSetSettings);

        // by default create PSM sets (and therefore remove fileID for set identifications)
        this.createPSMSets = true;
        this.psmSetSettings.remove(IdentificationKeySettings.FILE_ID.toString());

        // no settings are needed for the calculation of the ReportPSMs, but the PSM Set settings are used
        createReportPSMsFromGroups(groups, nrPSMs);
    }


    /**
     * Applies the general settings and recalculates the PSMSets
     */
    public void applyGeneralSettings(boolean createSets) {
        createPSMSets = createSets;

        if (!createPSMSets) {
            // no sets across files needed -> put fileID into settings
            this.psmSetSettings.put(IdentificationKeySettings.FILE_ID.name(), true);
        }

        // rebuild the PSM sets
        List<AbstractFilter> filters = getFilters(0L);

        // map to create the PSMSets
        Map<String, List<ReportPSM>> psmSetsMap =
                new HashMap<String, List<ReportPSM>>();

        // sort the PSMs in sets with their identificationKeys
        for (ReportPSM psm : spectraPSMs.values()) {
            if (FilterFactory.satisfiesFilterList(psm, 0L, filters)) {
                String psmKey = psm.getIdentificationKey(this.psmSetSettings);

                // put the PSM in the psmKey -> ReportPSM map
                if (!psmSetsMap.containsKey(psmKey)) {
                    psmSetsMap.put(psmKey, new ArrayList<ReportPSM>());
                }
                psmSetsMap.get(psmKey).add(psm);
            }
        }

        createReportPSMSets(psmSetsMap);
    }


    /**
     * Gets whether PSM sets should be used across files
     * @return
     */
    public Boolean getCreatePSMSets() {
        return createPSMSets;
    }


    /**
     * Getter for the {@link IdentificationKeySettings}.
     * @return
     */
    public Map<String, Boolean> getPSMSetSettings() {
        return psmSetSettings;
    }


    /**
     * Setter for the {@link IdentificationKeySettings}.
     *
     * @param newSettings A mapping from the
     * @return Returns the settings which were active before changing to the new
     * settings
     */
    public Map<String, Boolean> setPSMSetSettings(Map<String, Boolean> newSettings) {
        Map<String, Boolean> oldSettings = psmSetSettings;
        psmSetSettings = newSettings;

        // if no sets should be created, add the fileID to the settings
        if (!this.createPSMSets) {
            this.psmSetSettings.put(IdentificationKeySettings.FILE_ID.name(), true);
        }

        if (!psmSetSettings.equals(oldSettings)) {
            LOGGER.info("need to re-apply general settings");
            applyGeneralSettings(getCreatePSMSets());
        }

        return oldSettings;
    }


    /**
     * Returns the maximal set of (redundant) PSMSetSettings to combine PSMs of
     * all input files.
     *
     * @return
     */
    public Map<String, Boolean> getMaximalPSMSetSettings() {
        Map<String, Boolean> settings = new HashMap<String, Boolean>(IdentificationKeySettings.values().length);

        for (IdentificationKeySettings setting : IdentificationKeySettings.values()) {
            if ((getPSMSetSettingsWarnings().get(setting.toString()) != null) &&
                    (getPSMSetSettingsWarnings().get(setting.toString()).size() > 0)) {
                settings.put(setting.toString(), false);
            } else {
                settings.put(setting.toString(), true);
            }
        }

        return settings;
    }


    /**
     * Getter for the PSMSetSettingsWarnings
     * @return
     */
    public Map<String, Set<Long>> getPSMSetSettingsWarnings() {
        return psmSetSettingsWarnings;
    }


    /**
     * This method creates for each {@link PeptideSpectrumMatch} in the
     * given Map of {@link Group}s the corresponding {@link ReportPSM} and the
     * List of {@link ReportPSMSet}s for the overview.
     *
     * TODO: this could be run threaded (by PIA clusters)!
     *
     * @return a mapping from the spectrum ID to the ReportPSM
     */
    private void createReportPSMsFromGroups(Map<Long, Group> groups, int nrAllPSMs) {
        LOGGER.info("createReportPSMsFromGroups started...");

        Integer psmsPerFile = nrAllPSMs / (inputFiles.size()-1);

        // reset the PSMs
        spectraPSMs = new HashMap<Long, ReportPSM>();
        fileReportPSMs = new HashMap<Long, List<ReportPSM>>();

        // reset the scores
        fileScoreShortNames = new HashMap<Long, List<String>>();

        // reset the available sortings
        fileSortables = new HashMap<Long, Set<String>>();
        fileSortables.put(0L, new HashSet<String>());

        // reset the FDR data
        fileFDRData = new HashMap<Long, FDRData>();
        fileFDRCalculated = new HashMap<Long, Boolean>();

        // reset the FDR data for the overview
        fileFDRData.put(0L, new FDRData(FDRData.DecoyStrategy.ACCESSIONPATTERN,
                defaultDecoyPattern, defaultFDRThreshold));
        fileFDRCalculated.put(0L, false);

        // reset the value for used top identifications in FDR calculation
        fileTopIdentifications = new HashMap<Long, Integer>();

        // reset internal decoy knowledge
        fileHasInternalDecoy = new HashMap<Long, Boolean>(inputFiles.size());
        for (Long fileID : inputFiles.keySet()) {
            if (fileID > 0) {
                fileHasInternalDecoy.put(fileID, false);
            }
        }

        scoreShortToComparator =
                new HashMap<String, Comparator<PSMReportItem>>();

        scoreShortToHigherScoreBetter = new HashMap<String, Boolean>();

        scoreShortToHigherScoreBetterChangeable = new HashMap<String, Boolean>();

        // map to create the PSMSets
        Map<String, List<ReportPSM>> psmSetsMap =
                new HashMap<String, List<ReportPSM>>();

        // this map is used, to get the identification ranking for each score of a PSMs
        //  fileID    spectrumID  scoreShort        psm
        Map<Long, Map<String, Map<String, ArrayList<ReportPSM>>>> fileToRankings =
                new HashMap<Long, Map<String,Map<String, ArrayList<ReportPSM>>>>();


        // iterate through the groups
        long nrPSMs = 0;
        for (Map.Entry<Long, Group> groupIt : groups.entrySet()) {
            Map<String, Peptide> peptides = groupIt.getValue().getPeptides();

            // only groups with peptides can have PSMs
            if ((peptides != null) && (peptides.size() > 0)) {
                for (Map.Entry<String, Peptide> pepIt : peptides.entrySet()) {
                    List<PeptideSpectrumMatch> spectra = pepIt.getValue().getSpectra();
                    if (spectra != null) {
                        for (PeptideSpectrumMatch spec : spectra) {
                            ReportPSM psm = new ReportPSM(spec.getID(), spec);
                            Long fileID = spec.getFile().getID();
                            String psmKey = spec.getIdentificationKey(psmSetSettings);

                            // add the accessions
                            for (Accession acc
                                    : groupIt.getValue().getAllAccessions().values()) {
                                // only add accession, if it was found in the spectrum's file
                                if (acc.foundInFile(fileID)) {
                                    psm.addAccession(acc);
                                }
                            }

                            if (spectraPSMs.put(spec.getID(), psm) != null) {
                                // TODO: better warning
                                LOGGER.warn("psm with ID '"+spec.getID()+"' already in map");
                            }


                            // put the PSM in the fileID -> ReportPSMs mapping
                            List<ReportPSM> filesPSMList = fileReportPSMs.get(fileID);
                            if (filesPSMList == null) {
                                filesPSMList = new ArrayList<ReportPSM>(psmsPerFile);
                                fileReportPSMs.put(fileID, filesPSMList);

                                // this file is new, so also add the scoreName-Maps and sorting maps
                                fileScoreShortNames.put(fileID, new ArrayList<String>());

                                fileSortables.put(fileID, new HashSet<String>());

                                // also re-initialise the FDR data
                                fileFDRData.put(fileID,
                                        new FDRData(FDRData.DecoyStrategy.ACCESSIONPATTERN,
                                                defaultDecoyPattern,
                                                defaultFDRThreshold));
                                fileFDRCalculated.put(fileID, false);

                                //and the topIdentifications
                                fileTopIdentifications.put(fileID,
                                        defaultFDRTopIdentifications);
                            }
                            filesPSMList.add(psm);

                            // also put the PSM in the psmKey -> ReportPSM map, which is needed for the creation of the ReportPSMSets
                            List<ReportPSM> psmSets = psmSetsMap.get(psmKey);
                            if (psmSets == null) {
                                psmSets = new ArrayList<ReportPSM>();
                                psmSetsMap.put(psmKey, psmSets);
                            }
                            psmSets.add(psm);


                            // record everything needed for the identification ranking
                            Map<String, Map<String, ArrayList<ReportPSM>>> spectraToPSMs =
                                    fileToRankings.get(fileID);
                            if (spectraToPSMs == null) {
                                spectraToPSMs = new HashMap<String, Map<String, ArrayList<ReportPSM>>>(psmsPerFile);
                                fileToRankings.put(fileID, spectraToPSMs);
                            }


                            String psmScoreRankKey = createPSMKeyForScoreRanking(psm);
                            Map<String, ArrayList<ReportPSM>> scoreshortsToPSMs =
                                    spectraToPSMs.get(psmScoreRankKey);
                            if (scoreshortsToPSMs == null) {
                                scoreshortsToPSMs =
                                        new HashMap<String, ArrayList<ReportPSM>>();
                                spectraToPSMs.put(psmScoreRankKey, scoreshortsToPSMs);
                            }

                            for (ScoreModel score : psm.getScores()) {
                                // add the scorenames, if not yet done, and take the values for topIdentificationRanking
                                List<String> scoreShortNames = fileScoreShortNames.get(fileID);
                                if (!scoreShortNames.contains(score.getShortName())) {
                                    scoreShortNames.add(score.getShortName());

                                    if (!scoreShortToScoreName.containsKey(score.getShortName())) {
                                        scoreShortToScoreName.put(score.getShortName(),
                                                score.getName());

                                        LOGGER.debug("Added to scoremap: " + score.getShortName() + " -> " + score.getName());
                                    }

                                    // add score to the available sortings
                                    String scoreSortName =
                                            PSMReportItemComparator.getScoreSortName(score.getShortName());
                                    if (scoreSortName != null) {
                                        fileSortables.get(fileID).add(scoreSortName);
                                    }
                                }

                                // get comparators for all the PSM scores
                                if (!scoreShortToComparator.containsKey(
                                        score.getShortName())) {

                                    String scoreSortName =
                                            PSMReportItemComparator.getScoreSortName(score.getShortName());
                                    Comparator<PSMReportItem> comp;
                                    if (scoreSortName != null) {
                                        // this score is hard coded
                                        comp = PSMReportItemComparator.getComparatorByName(
                                                scoreSortName,
                                                SortOrder.ascending);

                                        scoreShortToHigherScoreBetterChangeable.put(
                                                score.getShortName(), false);

                                        scoreShortToHigherScoreBetter.put(
                                                score.getShortName(),
                                                ScoreModelEnum.getModelByDescription(score.getShortName()).higherScoreBetter());
                                    } else {
                                        Boolean higherscorebetter = null;

                                        Term oboTerm = getOBOMapper().getTerm(score.getAccession());
                                        if (oboTerm != null) {
                                            // the score is in the OBO file, get the relations etc.
                                            Set<Triple> tripleSet = getOBOMapper().getTriples(oboTerm, null, null);

                                            for (Triple triple : tripleSet) {
                                                if (triple.getPredicate().getName().equals(OBOMapper.obo_is_a)) {
                                                    if (triple.getObject().getName().equals("MS:1001868") || // MS:1001868 ! distinct peptide-level q-value
                                                            triple.getObject().getName().equals("MS:1001870") || // MS:1001870 ! distinct peptide-level p-value
                                                            triple.getObject().getName().equals("MS:1001872")) { // MS:1001872 ! distinct peptide-level e-value
                                                        higherscorebetter = false;
                                                    }
                                                } else if (triple.getPredicate().getName().equals(OBOMapper.obo_relationship)) {
                                                    if (triple.getObject().getName().equals(OBOMapper.obo_has_order_higherscorebetter)) {
                                                        higherscorebetter = true;
                                                    } else if (triple.getObject().getName().equals(OBOMapper.obo_has_order_lowerscorebetter)) {
                                                        higherscorebetter = false;
                                                    }
                                                }
                                            }
                                        }

                                        if (higherscorebetter != null) {
                                            // the status of higherScoreBetter is not to be changed by the user
                                            scoreShortToHigherScoreBetterChangeable.put(
                                                    score.getShortName(), false);
                                        } else {
                                            // the status of higherScoreBetter may be changed by the user
                                            scoreShortToHigherScoreBetterChangeable.put(
                                                    score.getShortName(), true);
                                            higherscorebetter = true;
                                        }
                                        scoreShortToHigherScoreBetter.put(
                                                score.getShortName(),
                                                higherscorebetter);
                                        comp = new ScoreComparator<PSMReportItem>(
                                                score.getShortName(),
                                                higherscorebetter);
                                    }

                                    LOGGER.debug("adding score comparator for " + score.getShortName() + ": " + comp);

                                    scoreShortToComparator.put(
                                            score.getShortName(), comp);
                                }

                                ArrayList<ReportPSM> psmsOfSpectrum =
                                        scoreshortsToPSMs.get(score.getShortName());
                                if (psmsOfSpectrum == null) {
                                    psmsOfSpectrum = new ArrayList<ReportPSM>(10);
                                    scoreshortsToPSMs.put(score.getShortName(),
                                            psmsOfSpectrum);
                                }
                                psmsOfSpectrum.add(psm);
                            }


                            if (!fileHasInternalDecoy.get(fileID) &&
                                    (spec.getIsDecoy() != null) &&
                                    spec.getIsDecoy()) {
                                fileHasInternalDecoy.put(fileID, true);
                            }

                            nrPSMs++;
                            if (nrPSMs % 100000 == 0) {
                                LOGGER.info(nrPSMs + " PSMs done");
                            }
                        }
                    }
                }
            }
        }

        // now set ranks to PSMs which have a known ranking
        for (Map<String, Map<String, ArrayList<ReportPSM>>> spectraToPSM
                : fileToRankings.values()) {

            for (Map<String, ArrayList<ReportPSM>> scoreshortsToPSMs
                    : spectraToPSM.values()) {

                for (Map.Entry<String, ArrayList<ReportPSM>> scoreToPSMsIt
                        : scoreshortsToPSMs.entrySet()) {
                    String scoreShort = scoreToPSMsIt.getKey();
                    Comparator<PSMReportItem> comp =
                            scoreShortToComparator.get(scoreShort);

                    // only sort and rank, if we know how
                    if (comp != null) {
                        Collections.sort(scoreToPSMsIt.getValue(), comp);

                        // give the ranks to the PSMs
                        Double lastScore = null;
                        int rank = 0;
                        for (ReportPSM psm : scoreToPSMsIt.getValue()) {
                            Double thisScore =
                                    psm.getScore(scoreShort);

                            if (!thisScore.equals(lastScore)) {
                                rank++;
                            }

                            psm.setIdentificationRank(scoreShort, rank);

                            lastScore = thisScore;
                        }
                    } else {
                        // unrankable get all ranked as -1
                        for (ReportPSM psm : scoreToPSMsIt.getValue()) {
                            psm.setIdentificationRank(scoreShort, -1);
                        }
                    }
                }
            }
        }

        // create and fill the ReportPSMSets for the overview
        createReportPSMSets(psmSetsMap);

        LOGGER.info("createReportPSMsFromGroups done.");
    }


    /**
     * Getter for the oboMapper. Initializes the OBOMapper on the first call.
     * @return
     */
    private OBOMapper getOBOMapper() {
        if (oboMapper == null) {
            oboMapper = new OBOMapper();
        }
        return oboMapper;
    }


    /**
     * Creates the {@link ReportPSMSet}s, given the {@link ReportPSM}s in a
     * mapping from the PSM-identificationKeys
     * @param psmSetsMap
     */
    private void createReportPSMSets(Map<String, List<ReportPSM>> psmSetsMap) {
        reportPSMSetMap = new HashMap<String, ReportPSMSet>(psmSetsMap.size());

        for (Map.Entry<String, List<ReportPSM>> psmSetsIt : psmSetsMap.entrySet()) {
            ReportPSMSet psmSet =
                    new ReportPSMSet(psmSetsIt.getValue(), psmSetSettings);
            reportPSMSetMap.put(psmSetsIt.getKey(), psmSet);
        }

        reportPSMSets = new ArrayList<ReportPSMSet>(
                reportPSMSetMap.values());

        fileFDRData.put(0L,
                new FDRData(fileFDRData.get(0L).getDecoyStrategy(),
                        fileFDRData.get(0L).getDecoyPattern(),
                        fileFDRData.get(0L).getFDRThreshold()));
        LOGGER.info("createReportPSMSets done");
    }


    /**
     * Returns a key by which the PSMs can be grouped for calculation of top
     * rank identifications.
     *
     * @param psm
     * @return
     */
    private static String createPSMKeyForScoreRanking(ReportPSM psm) {
        return psm.getSourceID()
                + ":" + psm.getSpectrum().getSpectrumTitle()
                + ":" + psm.getMassToCharge()
                + ":" + psm.getSpectrum().getRetentionTime();
    }


    /**
     * Getter for the files used in the PIA intermediate file, including the
     * pseudo-overview-file.
     *
     * @return
     */
    public Map<Long, PIAInputFile> getFiles() {
        return inputFiles;
    }


    /**
     * Getter for the filename of the PIA XML file.
     *
     * @return
     */
    public String getFileName() {
        return fileName;
    }


    /**
     * Getter for the default decoy pattern.
     * @return
     */
    public String getDefaultDecoyPattern() {
        return defaultDecoyPattern;
    }


    /**
     * Getter for the default FDR threshold for acceptance.
     * @return
     */
    public Double getDefaultFDRThreshold() {
        return defaultFDRThreshold;
    }


    /**
     * Getter for the default number of highest ranking PSMs (per spectrum) used
     * for FDR calculation.
     * @return
     */
    public Integer getDefaultFDRTopIdentifications() {
        return defaultFDRTopIdentifications;
    }


    /**
     * Reports the mapping from the ReportPSMSet identificationKeys to the
     * {@link ReportPSMSet}s. This map includes all possible, unfiltered PSM
     * sets.
     *
     * @return
     */
    public Map<String, ReportPSMSet> getReportPSMSets() {
        return reportPSMSetMap;
    }


    /**
     * Returns the number of PSMs or PSM sets for the given file ID.
     * @param fileID
     * @return
     */
    public int getNrReportPSMs(Long fileID) {
        if (fileID > 0) {
            if (fileReportPSMs.containsKey(fileID)) {
                return fileReportPSMs.get(fileID).size();
            } else {
                return -1;
            }
        } else {
            return reportPSMSets.size();
        }
    }


    /**
     * Returns the filtered List of {@link ReportPSM}s for the given fileID.
     *
     * @param fileID
     * @param filters
     * @return
     */
    public List<ReportPSM> getFilteredReportPSMs(Long fileID,
            List<AbstractFilter> filters) {
        if (fileReportPSMs.containsKey(fileID)) {
            return FilterFactory.applyFilters(fileReportPSMs.get(fileID),
                    filters, fileID);
        } else {
            LOGGER.error("There are no ReportPSMs for the fileID " + fileID);
            return new ArrayList<ReportPSM>(1);
        }
    }


    /**
     * Returns the filtered List of {@link ReportPSMSet}s for the PSM sets,
     * applying the given filters.
     *
     * @param filters
     * @return
     */
    public List<ReportPSMSet> getFilteredReportPSMSets(
            List<AbstractFilter> filters) {
        List<ReportPSMSet> filteredPSMSets = new ArrayList<ReportPSMSet>();

        // the PSM sets need a special filtering, some of the sets can become empty, due to filters on PSM level
        for (ReportPSMSet psmSet : reportPSMSets) {
            if (FilterFactory.satisfiesFilterList(psmSet, 0L, filters)) {
                List<ReportPSM> psms = FilterFactory.applyFilters(psmSet.getPSMs(), filters);

                if (!psms.isEmpty()) {
                    ReportPSMSet set = new ReportPSMSet(psms, psmSetSettings);
                    set.copyInfo(psmSet);
                    filteredPSMSets.add(set);
                }
            }
        }

        return filteredPSMSets;
    }


    /**
     * Returns the Score name, given the scoreShortName.
     * @param fileID
     * @param shortName
     * @return
     */
    public String getScoreName(String shortName) {
        return scoreShortToScoreName.get(shortName);
    }


    /**
     * Returns, whether the "higherscorebetter" can be changed by the user for
     * this score.
     *
     * @param shortName
     * @return
     */
    public Boolean getHigherScoreBetterChangeable(String scoreShort) {
        return scoreShortToHigherScoreBetterChangeable.get(scoreShort);
    }


    /**
     * Gets whether the higherScoreBetter is true or false for the score.
     *
     * @param scoreShort
     * @return
     */
    public Boolean getHigherScoreBetter(String scoreShort) {
        return scoreShortToHigherScoreBetter.get(scoreShort);
    }


    /**
     * Sets whether the higherScoreBetter is true or false for the score.
     *
     * @param scoreShort
     */
    public void setHigherScoreBetter(String scoreShort,
            Boolean higherScoreBetter) {
        if (scoreShortToHigherScoreBetterChangeable.get(scoreShort)) {
            scoreShortToHigherScoreBetter.put(scoreShort, higherScoreBetter);

            scoreShortToComparator.put(scoreShort,
                    new ScoreComparator<PSMReportItem>(
                            scoreShort, higherScoreBetter));

            LOGGER.debug("setHigherScoreBetter: " + scoreShortToComparator.get(scoreShort));
        } else {
            LOGGER.warn("The comparator for " + scoreShort + "(" +
                    scoreShortToScoreName.get(scoreShort) +
                    ") may not be changed!");
        }
    }


    /**
     * Returns the comparator for the given short.
     *
     * @param scoreShort
     * @return
     */
    public Comparator<PSMReportItem> getScoreComparator(String scoreShort) {
        if (scoreShortToComparator.containsKey(scoreShort)) {
            return scoreShortToComparator.get(scoreShort);
        }

        LOGGER.warn("no comparator found for " + scoreShort);
        return null;
    }


    /**
     * Returns for the given score, whether a higher score is better.
     *
     * @param scoreShort
     * @return
     */
    public Boolean getHigherScoreBetterForScore(String scoreShort) {
        return scoreShortToHigherScoreBetter.get(scoreShort);
    }


    /**
     * Getter for the shortNames of all scores of the given file
     *
     * @param fileID
     * @return
     */
    public List<String> getScoreShortNames(Long fileID) {
        if (fileScoreShortNames.containsKey(fileID)) {
            return fileScoreShortNames.get(fileID);
        } else {
            return new ArrayList<String>(1);
        }
    }


    /**
     * Returns the mapping from the shortNames to the nicely readable names.
     *
     * @return
     */
    public Map<String, String> getScoreShortsToScoreNames() {
        return scoreShortToScoreName;
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
     * Getter for the map of FDR data
     * @return
     */
    public Map<Long, FDRData> getFileFDRData() {
        return fileFDRData;
    }


    /**
     * Returns the set number of top identifications used for the FDR
     * calculation for the given file.
     *
     * @param fileID
     * @return
     */
    public Integer getFilesTopIdentifications(Long fileID) {
        return fileTopIdentifications.get(fileID);
    }


    /**
     * Sets the number of top identifications used for the FDR calculation for
     * the given file.
     *
     * @param fileID
     * @return
     */
    public void setFilesTopIdentifications(Long fileID, Integer topIDs) {
        fileTopIdentifications.put(fileID, topIDs);
    }


    /**
     * Sets the number of top identifications used for the FDR calculation for
     * all files.
     *
     * @param fileID
     * @return
     */
    public void setAllTopIdentifications(Integer topIDs) {
        for (Long fileID : fileTopIdentifications.keySet()) {
            setFilesTopIdentifications(fileID, topIDs);
        }
    }


    /**
     * Sets the given pattern as the decoy pattern for all files' FDR data and
     * sets the FDR decoy strategy to
     * {@link FDRData.DecoyStrategy}.ACCESSIONPATTERN, unless "searchengine" is
     * given as pattern, which will set
     * {@link FDRData.DecoyStrategy}.SEARCHENGINE as decoy strategy.
     *
     * @param pattern
     */
    public void setAllDecoyPattern(String pattern) {
        FDRData.DecoyStrategy decoyStrategy;
        boolean setPattern;

        if (FDRData.DecoyStrategy.SEARCHENGINE.toString().equals(pattern)) {
            decoyStrategy = FDRData.DecoyStrategy.SEARCHENGINE;
            setPattern = false;
        } else {
            decoyStrategy = FDRData.DecoyStrategy.ACCESSIONPATTERN;
            setPattern = true;
        }

        for (FDRData fdrData : fileFDRData.values()) {
            fdrData.setDecoyStrategy(decoyStrategy);
            if (setPattern) {
                fdrData.setDecoyPattern(pattern);
            }
        }
    }


    /**
     * Updates the {@link FDRData} for the given file.
     *
     * @param fileID
     * @return
     */
    public void updateFilesFDRData(Long fileID, DecoyStrategy decoyStrategy,
            String decoyPattern, Double fdrThreshold, String scoreModelShort,
            Integer topIdentifications) {
        FDRData fdrData = fileFDRData.get(fileID);

        if (fdrData != null) {
            fdrData.setDecoyStrategy(decoyStrategy);
            fdrData.setDecoyPattern(decoyPattern);
            fdrData.setFDRThreshold(fdrThreshold);
            fdrData.setScoreShortName(scoreModelShort);

            setFilesTopIdentifications(fileID, topIdentifications);

            LOGGER.info(fileID + "'s FDRData set to: " +
                    fdrData.getDecoyStrategy() + ", " +
                    fdrData.getDecoyPattern() + ", " +
                    fdrData.getFDRThreshold() + ", " +
                    fdrData.getScoreShortName() + ", " +
                    getFilesTopIdentifications(fileID));
        } else {
            LOGGER.error("No FDRData for file with ID " + fileID);
        }
    }


    /**
     * Returns a List of scoreShortNames of available Scores for FDR calculation
     * for the given file.
     *
     * @param fileID
     * @return
     */
    public List<String> getFilesAvailableScoreShortsForFDR(Long fileID) {
        List<String> fdrScoreNames = new ArrayList<String>();

        // the overview is treated separately (it has no available scores, but only the FDRScore
        if (fileID > 0) {
            if (fileScoreShortNames.containsKey(fileID)) {
                List<String> scoreShorts = fileScoreShortNames.get(fileID);
                for (int i=0; i < scoreShorts.size(); i++) {
                    if (!ScoreModelEnum.PSM_LEVEL_FDR_SCORE.isValidDescriptor(scoreShorts.get(i))) {
                        // FDR score is not available for FDR calculation
                        fdrScoreNames.add(scoreShorts.get(i));
                    }
                }
            } else {
                LOGGER.error("No scores available for FDR calculation for the file with ID "+fileID);
            }
        }

        return fdrScoreNames;
    }


    /**
     * Returns a mapping from the file IDs to the scoreNames used for FDR
     * calculation for each file.
     *
     * @return
     */
    public Map<Long, String> getFileIDsToScoreOfFDRCalculation() {
        Map<Long, String> filenameToScoreOfFDRCalculation = new HashMap<Long, String>(inputFiles.size()-1);

        for (PIAInputFile file : inputFiles.values()) {
            if (file.getID() != 0L) {
                String scoreName;
                Boolean fdrCalculated = fileFDRCalculated.get(file.getID());
                if (fdrCalculated) {
                    FDRData fdrData = fileFDRData.get(file.getID());
                    scoreName = ScoreModelEnum.getName(fdrData.getScoreShortName());
                } else {
                    scoreName = "no FDR calculated";
                }

                filenameToScoreOfFDRCalculation.put(file.getID(), scoreName);
            }
        }

        return filenameToScoreOfFDRCalculation;
    }


    /**
     * Returns, whether for the given file an FDR is calculated.
     * @param fileID
     * @return
     */
    public Boolean isFDRCalculated(Long fileID) {
        return fileFDRCalculated.get(fileID);
    }


    /**
     * Returns true, if all files have a calculated FDR.
     * @return
     */
    public Boolean getAllFilesHaveFDRCalculated() {
        for (PIAInputFile file : inputFiles.values()) {
            if ((file.getID() > 0) &&
                    ((fileFDRCalculated.get(file.getID()) == null) ||
                            !fileFDRCalculated.get(file.getID()))) {
                return false;
            }

        }

        return true;
    }


    /**
     * Returns whether the combined FDR Score is calculated.
     * @return
     */
    public boolean isCombinedFDRScoreCalculated() {
        return !reportPSMSets.isEmpty()
                && (fileFDRCalculated.get(0L) != null)
                && fileFDRCalculated.get(0L);
    }


    /**
     * Returns, whether the file with the given ID has internal decoys, i.e.
     * PSMs which are set to be decoys in the PIA XML file.
     *
     * @param fileID
     * @return
     */
    public Boolean getFileHasInternalDecoy(Long fileID) {
        return fileHasInternalDecoy.get(fileID);
    }


    /**
     * Updates the decoy states of the PSMs with the current settings from the
     * file's FDRData.
     *
     */
    public void updateDecoyStates(Long fileID) {
        FDRData fdrData = fileFDRData.get(fileID);

        LOGGER.info("updateDecoyStates " + fileID);

        // select either the PSMs from the given file or all and calculate the fdr
        if (fdrData == null) {
            LOGGER.error("No FDR settings given for file with ID=" + fileID);
            // TODO: throw an exception or something
            return;
        } else {
            Pattern p = Pattern.compile(fdrData.getDecoyPattern());

            if (fileID > 0) {
                // get a List of the ReportPSMs for FDR calculation
                List<ReportPSM> listForFDR = fileReportPSMs.get(fileID);

                if (listForFDR == null) {
                    LOGGER.error("No PSMs found for the file with ID=" + fileID);
                    // TODO: throw an exception
                    return;
                }

                for (ReportPSM psm : listForFDR) {
                    // dump all FDR data
                    psm.dumpFDRCalculation();
                    psm.updateDecoyStatus(fdrData.getDecoyStrategy(), p);
                }
            } else {
                // set decoy information for PSM sets
                for (ReportPSMSet psmSet : reportPSMSets) {
                    psmSet.dumpFDRCalculation();
                    psmSet.updateDecoyStatus(fdrData.getDecoyStrategy(), p);
                }
            }
        }
    }


    /**
     * Calculate the FDR for all files. <br/>
     * If no score for the FDR calculation is given, use a default.
     */
    public void calculateAllFDR() {
        for (Long fileID : fileReportPSMs.keySet()) {
            calculateFDR(fileID);
        }
    }


    /**
     * Calculate the FDR for the file given by fileID
     *
     * @param fileID
     */
    public void calculateFDR(Long fileID) {
        FDRData fdrData = fileFDRData.get(fileID);

        // select either the PSMs from the given file or all and calculate the fdr
        if (fdrData == null) {
            LOGGER.error("No FDR settings given for file with ID=" + fileID);
            // TODO: throw an exception
            return;
        } else {
            fdrData.setScoreShortName(getFilesPreferredFDRScore(fileID));
            LOGGER.info("set the score for FDR calculation for fileID="
                    + fileID + ": " + fdrData.getScoreShortName());

            // recalculate the decoy status (especially important, if decoy pattern was changed)
            updateDecoyStates(fileID);


            if (fileReportPSMs.get(fileID) == null) {
                LOGGER.error("No PSMs found for the file with ID=" + fileID);
                // TODO: throw an exception
                return;
            }

            // get a List of the ReportPSMs for FDR calculation
            List<PSMReportItem> listForFDR = new ArrayList<PSMReportItem>(fileReportPSMs.get(fileID));

            if ((fileTopIdentifications.get(fileID) != null) &&
                    (fileTopIdentifications.get(fileID) > 0)) {

                LOGGER.info("applying topIdentification filter: top " +
                        fileTopIdentifications.get(fileID) + " for " +
                        fdrData.getScoreShortName());

                for (PSMReportItem psm : listForFDR) {
                    // as the used ReportPSMs may change with the filter, clear all prior FDR information
                    psm.dumpFDRCalculation();
                }

                // only the topIdentifications should be used, so a filter is needed
                List<AbstractFilter> topRankFilter = new ArrayList<AbstractFilter>(1);

                topRankFilter.add(new PSMTopIdentificationFilter(
                        FilterComparator.less_equal,
                        fileTopIdentifications.get(fileID),
                        false,
                        fdrData.getScoreShortName()));

                List<PSMReportItem> filteredList = FilterFactory.applyFilters(
                        listForFDR,
                        topRankFilter,
                        fileID);

                listForFDR = filteredList;
            }


            if (scoreShortToComparator.get(fdrData.getScoreShortName()) == null) {
                LOGGER.warn("No comparator for FDR calculation, "
                        + "aborted calculateFDR!");
                return;
            }

            // calculate the FDR values
            fdrData.calculateFDR(listForFDR,
                    scoreShortToComparator.get(fdrData.getScoreShortName()));

            // and also calculate the FDR score
            FDRScore.calculateFDRScore(listForFDR, fdrData,
                    scoreShortToHigherScoreBetter.get(fdrData.getScoreShortName()));


            List<String> scoreShorts = fileScoreShortNames.get(fileID);
            if (!scoreShorts.contains(ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName())) {
                // add the FDR score to scores of this file
                scoreShorts.add(ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName());
                scoreShortToScoreName.put(
                        ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName(),
                        ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getName());
                scoreShortToComparator.put(ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName(),
                        new ScoreComparator<PSMReportItem>(
                                ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName(),
                                false));
                scoreShortToHigherScoreBetter.put(
                        ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName(),
                        false);
                scoreShortToHigherScoreBetterChangeable.put(
                        ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName(),
                        false);

                // and also to the sortable fields
                fileSortables.get(fileID).add(
                        PSMReportItemComparator.getScoreSortName(ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName()) );
            }

            // the FDR for this file is calculated now
            fileFDRCalculated.put(fileID, true);
        }
    }


    /**
     * Clears all preferred FDR scores.
     */
    public void resetPreferredFDRScores() {
        preferredFDRScores.clear();

        // set all FDR data scoreshorts for the regular files to null
        for (Map.Entry<Long, FDRData> fdrIt : fileFDRData.entrySet()) {
            if (!fdrIt.getKey().equals(0L)) {
                fdrIt.getValue().setScoreShortName(null);
            }
        }
    }


    /**
     * Getter for the set of preferred scores for FDR  calculation.
     * @return
     */
    public List<String> getPreferredFDRScores() {
        return preferredFDRScores;
    }


    /**
     * Adds the given scoreShortNames to the preferred FDR scores.
     */
    public void addPreferredFDRScores(List<String> scoreShortNames) {
        for (String scoreShortName : scoreShortNames) {
            addPreferredFDRScore(scoreShortName);
        }
    }


    /**
     * Adds the score with the given short to the preferred FDR scores. If the
     * score does not exist, do nothing.
     *
     * @param scoreShortName
     */
    public void addPreferredFDRScore(String scoreShortName) {
        if (scoreShortName == null) {
            return;
        }

        // get the unique score shortName, if the score is known
        ScoreModelEnum model =
                ScoreModelEnum.getModelByDescription(scoreShortName);
        String shortName = model.getShortName();
        if (shortName == null) {
            // for an unknown score, take the given shortName
            shortName = scoreShortName;
        }

        if (!preferredFDRScores.contains(shortName)) {
            preferredFDRScores.add(shortName);
        }
    }


    /**
     * Returns the scoreShort which will be used for FDR calculation of the
     * given file. This is either the first applicable in the preferred scores
     * of the first one of the file.
     *
     * @param fileID
     * @return
     */
    public String getFilesPreferredFDRScore(Long fileID) {
        // first look in the preferred scores
        for (String scoreShort : preferredFDRScores) {
            if (fileScoreShortNames.get(fileID).contains(scoreShort)) {
                return scoreShort;
            }
        }

        // if no score is set in the preferred, look for searchengine main scores
        if (fileScoreShortNames.containsKey(fileID)) {
            for (String scoreShort : fileScoreShortNames.get(fileID)) {
                if (ScoreModelEnum.getModelByDescription(scoreShort).isSearchengineMainScore()) {
                    return scoreShort;
                }
            }

            // if no score is returned yet, take the first best score
            return fileScoreShortNames.get(fileID).get(0);
        }

        return null;
    }



    /**
     * Calculates the Combined FDR Score for the PSM sets in the overview
     */
    public void calculateCombinedFDRScore() {
        Map<String, List<ReportPSMSet>> fileLists = new HashMap<String, List<ReportPSMSet>>();
        String key;

        updateDecoyStates(0L);

        // first we need the Average FDR Score for each PSM set
        for (ReportPSMSet set : reportPSMSets) {
            set.calculateAverageFDRScore();

            if (!set.getAverageFDRScore().getValue().equals(Double.NaN)) {
                // put the PSM set into the List, which holds the sets identified in the same files
                if (set.getPSMs().size() > 1) {

                    Set<Long> files = new TreeSet<Long>();
                    for (ReportPSM psm : set.getPSMs()) {
                        if ((psm.getFDRScore() != null) &&
                                !psm.getFDRScore().getValue().equals(Double.NaN)) {
                            // the psm has a valid FDR for this file
                            files.add(psm.getFileID());
                        }
                    }

                    StringBuilder sbKey = new StringBuilder("");

                    for (Long file : files) {
                        if (sbKey.length() > 0) {
                            sbKey.append(":");
                        }
                        sbKey.append(file);
                    }
                    key = sbKey.toString();
                } else {
                    key = set.getPSMs().get(0).getFileID().toString();
                }

                if (!fileLists.containsKey(key)) {
                    fileLists.put(key, new ArrayList<ReportPSMSet>());
                }

                fileLists.get(key).add(set);
            } else {
                // this PSM set gets no Combined FDR Score
                set.setFDRScore(Double.NaN);
            }
        }


        // go through the search-engine-sets, sort by AFS and calculate combined FDR Score
        for (Map.Entry<String, List<ReportPSMSet>> seSetIt : fileLists.entrySet()) {
            LOGGER.info("Calculation of Combined FDR Score for " + seSetIt.getKey());

            Collections.sort(seSetIt.getValue(),
                    new ScoreComparator<ReportPSMSet>(ScoreModelEnum.AVERAGE_FDR_SCORE.getShortName()));

            FDRData fdrData = fileFDRData.get(0L);

            fdrData.setScoreShortName(ScoreModelEnum.AVERAGE_FDR_SCORE.getShortName());
            fdrData.calculateFDR(seSetIt.getValue());

            if (seSetIt.getValue().size() > 2) {
                FDRScore.calculateFDRScore(seSetIt.getValue(), fdrData,
                        ScoreModelEnum.AVERAGE_FDR_SCORE.higherScoreBetter());
            } else {
                for (ReportPSMSet set : seSetIt.getValue()) {
                    set.setFDRScore(set.getAverageFDRScore().getValue());
                }
            }
        }


        // add to the sortable fields
        fileSortables.get(0L).add(
                PSMReportItemComparator.getScoreSortName(ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()) );

        // and add to the score fields
        fileScoreShortNames.put(0L, new ArrayList<String>(1));
        fileScoreShortNames.get(0L).add(ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName());
        scoreShortToScoreName.put(
                ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName(),
                ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getName());
        scoreShortToComparator.put(ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName(),
                new ScoreComparator<PSMReportItem>(
                        ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName(),
                        false));
        scoreShortToHigherScoreBetter.put(
                ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName(),
                false);
        scoreShortToHigherScoreBetterChangeable.put(
                ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName(),
                false);

        // correct the numbers of decoys etc.
        int nrDecoys = 0;
        int nrTargets = 0;
        int nrItems = 0;
        int nrFDRGoodDecoys = 0;
        int nrFDRGoodTargets = 0;
        double thr = fileFDRData.get(0L).getFDRThreshold();

        for (ReportPSMSet set : reportPSMSets) {
            if (!set.getFDRScore().getValue().equals(Double.NaN)) {
                nrItems++;
                if (set.getIsDecoy()) {
                    nrDecoys++;
                    if (set.getFDRScore().getValue() <= thr) {
                        nrFDRGoodDecoys++;
                    }
                } else {
                    nrTargets++;
                    if (set.getFDRScore().getValue() <= thr) {
                        nrFDRGoodTargets++;
                    }
                }
            }
        }

        fileFDRData.get(0L).correctNumbers(nrDecoys, nrFDRGoodDecoys,
                nrFDRGoodTargets, nrItems, nrTargets);

        // the combined FDR is calculated now
        fileFDRCalculated.put(0L, true);
    }


    /**
     * Sorts the List of {@link ReportPSM}s of the file given by fileID with the
     * prior specified sorting parameters.
     */
    public void sortReport(Long fileID, List<String> sortOrders,
            Map<String, SortOrder> sortables) {
        List<Comparator<PSMReportItem>> compares =
                new ArrayList<Comparator<PSMReportItem>>();

        for (String sortKey : sortOrders) {
            Comparator<PSMReportItem> comp = getScoreComparator(sortKey);

            if (comp == null) {
                comp = PSMReportItemComparator.getComparatorByName(
                        sortKey,
                        sortables.get(sortKey));
            } else if (sortables.get(sortKey).equals(SortOrder.descending)){
                comp = PSMReportItemComparator.descending(comp);
            }

            if (comp != null) {
                compares.add( comp);
            } else {
                LOGGER.error("no comparator found for " + sortKey);
            }
        }

        if (fileID > 0) {
            Collections.sort(fileReportPSMs.get(fileID),
                    PSMReportItemComparator.getComparator(compares));
        } else {
            Collections.sort(reportPSMSets,
                    PSMReportItemComparator.getComparator(compares));
        }
    }


    /**
     * Returns a List of scoreShortNames of available Scores for ranking.
     *
     * @param fileID
     * @return
     */
    public List<String> getFilesAvailableScoreShortsForRanking(Long fileID) {
        List<String> rankingScoreNames = new ArrayList<String>();

        if (fileScoreShortNames.containsKey(fileID)) {
            for (String scoreShort : fileScoreShortNames.get(fileID)) {
                rankingScoreNames.add(scoreShort);
            }
        }

        if (rankingScoreNames.isEmpty() && (fileID > 0)) {
            LOGGER.error("No scores available for ranking for the file with ID " + fileID);
        }

        return rankingScoreNames;
    }


    /**
     * Calculates the ranking for the given file and scoreShortName. If the
     * filter List is not null or empty, the Report is filtered before ranking.
     */
    public void calculateRanking(Long fileID, String rankableShortName,
            List<AbstractFilter> filters) {
        if ((rankableShortName == null) || rankableShortName.trim().isEmpty()) {
            LOGGER.error("No score shortName given for ranking calculation.");
            return;
        }

        // first, dump all prior ranking
        List<?> reports;
        if (fileID > 0) {
            reports = fileReportPSMs.get(fileID);
        } else {
            reports = reportPSMSets;
        }
        if (reports != null) {
            for (Object obj : reports) {
                if (obj instanceof PSMReportItem) {
                    ((PSMReportItem) obj).setRank(-1L);
                }
            }
        }

        if (fileID > 0) {
            RankCalculator.calculateRanking(rankableShortName,
                    FilterFactory.applyFilters(fileReportPSMs.get(fileID),
                            filters, fileID),
                    new ScoreComparator<ReportPSM>(rankableShortName));
        } else {
            RankCalculator.calculateRanking(rankableShortName,
                    FilterFactory.applyFilters(reportPSMSets,
                            filters, fileID),
                    new ScoreComparator<ReportPSMSet>(rankableShortName));
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
            filters = new ArrayList<AbstractFilter>();
            fileFiltersMap.put(fileID, filters);
        }

        return filters;
    }


    /**
     * Add a new filter for the given file
     */
    public boolean addFilter(Long fileID, AbstractFilter newFilter) {
        if (newFilter != null) {
            return getFilters(fileID).add(newFilter);
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
            return filters.remove(removingIndex);
        }

        return null;
    }



    public double[] getPPMDeviationData(Long fileID, boolean fdrGood) {
        if (fdrGood && !isFDRCalculated(fileID)) {
            return new double[0];
        }

        ArrayList<Double> ppmData;

        // put all PPM shifts into the array
        if (fileID > 0) {
            ppmData = new ArrayList<Double>(fileReportPSMs.get(fileID).size());
            ListIterator<ReportPSM> iter = fileReportPSMs.get(fileID).listIterator();
            while (iter.hasNext()) {
                ReportPSM psm = iter.next();
                if (!fdrGood || (!psm.getIsDecoy() && psm.getIsFDRGood())) {
                    ppmData.add(psm.getDeltaPPM());
                }
            }
        } else {
            ppmData = new ArrayList<Double>(reportPSMSets.size());
            ListIterator<ReportPSMSet> iter = reportPSMSets.listIterator();
            while (iter.hasNext()) {
                ReportPSMSet psm = iter.next();
                if (!fdrGood || (!psm.getIsDecoy() && psm.getIsFDRGood())) {
                    ppmData.add(psm.getDeltaPPM());
                }
            }
        }

        double[] ppmResult = new double[ppmData.size()];
        ListIterator<Double> iter = ppmData.listIterator();
        int idx = 0;
        while (iter.hasNext()) {
            ppmResult[idx] = iter.next();
            idx++;
        }
        return ppmResult;
    }


    /**
     * Calculates the data for a histogram of the distribution of the PPM
     * divergence. If fdrGood is true, only the FDR good target PSM (sets) are
     * taken into account.
     *
     * @param fileID
     * @param fdrGood whether to use only the FDR good target PSM(set)s
     * @return
     */
    public List<List<Integer>> getPPMs(Long fileID, boolean fdrGood) {
        if (fdrGood && !isFDRCalculated(fileID)) {
            List<List<Integer>> labelled = new ArrayList<List<Integer>>();
            labelled.add(new ArrayList<Integer>());
            labelled.add(new ArrayList<Integer>());
            return labelled;
        }

        Map<Integer, Integer> ppmMap = new HashMap<Integer, Integer>();
        int counted = 0;
        int labelMax = 0;
        int labelMin = 0;

        // put the PPMs in 1-PPM bins in the map
        if (fileID > 0) {
            for (ReportPSM psm : fileReportPSMs.get(fileID)) {
                if (!fdrGood
                        || (!psm.getIsDecoy() && psm.getIsFDRGood())) {
                    Integer label = (int)Math.floor(psm.getDeltaPPM() + 0.5d);

                    if (!ppmMap.containsKey(label)) {
                        ppmMap.put(label, 0);
                    }
                    ppmMap.put(label, ppmMap.get(label) + 1);
                    counted++;

                    if (label < labelMin) {
                        labelMin = label;
                    }
                    if (label > labelMax) {
                        labelMax = label;
                    }
                }
            }
        } else {
            for (ReportPSMSet psm : reportPSMSets) {
                if (!fdrGood
                        || (!psm.getIsDecoy() && psm.getIsFDRGood())) {
                    Integer label = (int)Math.floor(psm.getDeltaPPM() + 0.5d);

                    if (!ppmMap.containsKey(label)) {
                        ppmMap.put(label, 0);
                    }
                    ppmMap.put(label, ppmMap.get(label) + 1);
                    counted++;

                    if (label < labelMin) {
                        labelMin = label;
                    }
                    if (label > labelMax) {
                        labelMax = label;
                    }
                }
            }
        }

        // create the PPM counts and labels list from the map
        List<Integer> ppms = new ArrayList<Integer>();
        List<Integer> labels = new ArrayList<Integer>();
        int drawn = 0;

        labels.add(0);
        if (ppmMap.containsKey(0)) {
            ppms.add(ppmMap.get(0));
            drawn += ppmMap.get(0);
        } else {
            ppms.add(0);
        }

        int i;
        for (i=1; (i < 10) && (drawn < 0.995 * counted); i++) {
            labels.add(i);
            if (ppmMap.containsKey(i)) {
                ppms.add(ppmMap.get(i));
                drawn += ppmMap.get(i);
            } else {
                ppms.add(0);
            }

            labels.add(0, -i);
            if (ppmMap.containsKey(-i)) {
                ppms.add(0, ppmMap.get(-i));
                drawn += ppmMap.get(-i);
            } else {
                ppms.add(0, 0);
            }
        }

        // all above
        for (drawn=0; i < labelMax; i++) {
            if (ppmMap.containsKey(i)) {
                drawn += ppmMap.get(i);
            }
        }
        labels.add(null);
        ppms.add(drawn);

        // all below
        for (i=labelMin, drawn=0; i < labels.get(0); i++) {
            if (ppmMap.containsKey(i)) {
                drawn += ppmMap.get(i);
            }
        }
        labels.add(0, null);
        ppms.add(0, drawn);

        List<List<Integer>> labelled = new ArrayList<List<Integer>>();
        labelled.add(ppms);
        labelled.add(labels);
        return labelled;
    }



    /**
     * Returns, how many times a PSM set had how many identifications. The first
     * entry in the list represents one identification, the second two...
     *
     * @param fdrGood
     * @return
     */
    public List<Integer> getNrIdentifications(boolean fdrGood) {
        if (fdrGood && !isCombinedFDRScoreCalculated()) {
            List<Integer> idLIst = new ArrayList<Integer>(1);
            idLIst.add(0);
            return idLIst;
        }

        Map<Integer, Integer> idMap = new HashMap<Integer, Integer>();
        int maxIDs = 0;

        // count the number of identifications
        for (ReportPSMSet psm : reportPSMSets) {
            if (!fdrGood ||
                    (!psm.getIsDecoy() && psm.getIsFDRGood())) {
                Integer ids = psm.getPSMs().size();

                if (!idMap.containsKey(ids)) {
                    idMap.put(ids, 1);
                } else {
                    idMap.put(ids, idMap.get(ids) + 1);
                }

                if (ids > maxIDs) {
                    maxIDs = ids;
                }
            }
        }

        List<Integer> IDs = new ArrayList<Integer>(maxIDs);
        for (int i=1; i <= maxIDs; i++) {
            if (idMap.containsKey(i)) {
                IDs.add(idMap.get(i));
            } else {
                IDs.add(0);
            }
        }

        return IDs;
    }


    /**
     * Writes the PSM report for the file with the given ID and filtered with
     * the given filters in a loose CSV format.<br/>
     * If the export is for spectral counting, the filters are applied for every
     * file instead of the set overview (only important for the overview and).
     *
     * @param writer
     * @param fileID
     * @throws IOException
     */
    public void exportCSV(Writer writer, Long fileID,
            boolean exportForSC, boolean filterExport) throws IOException {
        List<PSMReportItem> report;
        if (fileID > 0) {
            List<ReportPSM> rep = filterExport ?
                    getFilteredReportPSMs(fileID, getFilters(fileID)) :
                        fileReportPSMs.get(fileID);

                    report = new ArrayList<PSMReportItem>(rep.size());
                    report.addAll(rep);
        } else {
            if (!exportForSC) {
                List<ReportPSMSet> rep = filterExport ?
                        getFilteredReportPSMSets(getFilters(fileID)) :
                            reportPSMSets;
                        report = new ArrayList<PSMReportItem>(rep.size());
                        report.addAll(rep);

            } else {
                report = new ArrayList<PSMReportItem>();

                for (Long psmFileID: fileReportPSMs.keySet()) {
                    if (getFilters(0L).size() > 0) {
                        List<ReportPSM> part = filterExport ?
                                getFilteredReportPSMs(psmFileID, getFilters(0L)) :
                                    fileReportPSMs.get(psmFileID);

                                report.addAll(part);
                    } else {
                        report.addAll(fileReportPSMs.get(psmFileID));
                    }
                }

            }
        }

        String separator = ",";

        if (!exportForSC) {
            if (fileID > 0) {
                writer.append(
                        "\"sequence\"" + separator +
                        "\"modifications\"" + separator +
                        "\"charge\"" + separator +
                        "\"m/z\"" + separator +
                        "\"delta mass\"" + separator +
                        "\"delta ppm\"" + separator +
                        "\"retention time\"" + separator +
                        "\"missed\"" + separator +
                        "\"sourceID\"" + separator +
                        "\"accessions\"" + separator +
                        "\"scores\"" + separator +
                        "\"identification ranks\"" + separator +
                        "\"isDecoy\"" + separator +
                        "\"isUnique\"" + separator +
                        "\"isFDRGood\"" +
                        "\n"
                        );
            } else {
                writer.append(
                        "\"sequence\"" + separator +
                        "\"modifications\"" + separator +
                        "\"charge\"" + separator +
                        "\"m/z\"" + separator +
                        "\"delta mass\"" + separator +
                        "\"delta ppm\"" + separator +
                        "\"retention time\"" + separator +
                        "\"missed\"" + separator +
                        "\"sourceID\"" + separator +
                        "\"accessions\"" + separator +
                        "\"scores\"" + separator +
                        "\"identification ranks\"" + separator +
                        "\"isDecoy\"" + separator +
                        "\"isFDRGood\"" + separator +
                        "\n"
                        );
            }
        } else {
            // exportForSC is set
            writer.append(
                    "\"accession\"" + separator +
                    "\"filename\"" + separator +
                    "\"sequence\"" + separator +
                    "\"modifications\"" + separator +
                    "\"charge\"" + separator +
                    "\"m/z\"" + separator +
                    "\"delta mass\"" + separator +
                    "\"delta ppm\"" + separator +
                    "\"retention time\"" + separator +
                    "\"missed\"" + separator +
                    "\"sourceID\"" + separator +
                    "\"spectrumTitle\"" + separator +
                    "\"scores\"" + separator +
                    "\"identification ranks\"" + separator +
                    "\"isDecoy\"" + separator +
                    "\"isUnique\"" +
                    "\n"
                    );
        }

        for (Object item : report) {
            if (!exportForSC) {
                if (item instanceof ReportPSM) {
                    ReportPSM psm = (ReportPSM)item;

                    StringBuilder accessionsSB = new StringBuilder();
                    for (Accession acc : psm.getAccessions()) {
                        if (accessionsSB.length() > 0) {
                            accessionsSB.append(",");
                        }
                        accessionsSB.append(acc.getAccession());
                    }

                    writer.append(
                            "\"" + psm.getSequence() + "\"" + separator +
                            "\"" + psm.getModificationsString() + "\"" + separator +
                            "\"" + psm.getCharge() + "\"" + separator +
                            "\"" + psm.getMassToCharge() + "\"" + separator +
                            "\"" + psm.getDeltaMass() + "\"" + separator +
                            "\"" + psm.getDeltaPPM() + "\"" + separator +
                            "\"" + psm.getRetentionTime() + "\"" + separator +
                            "\"" + psm.getMissedCleavages() + "\"" + separator +
                            "\"" + psm.getSourceID() + "\"" + separator +
                            "\"" + accessionsSB.toString() + "\"" + separator +
                            "\"" + psm.getScores() + "\"" + separator +
                            "\"" + psm.getIdentificationRanks() + "\"" + separator +
                            "\"" + psm.getIsDecoy() + "\"" + separator +
                            "\"" + ((psm.getSpectrum().getIsUnique() != null) ? psm.getSpectrum().getIsUnique() : false) + "\"" + separator +
                            "\"" + psm.getIsFDRGood() + "\"" +
                            "\n"
                            );
                } else if (item instanceof ReportPSMSet) {
                    ReportPSMSet psm = (ReportPSMSet)item;

                    StringBuilder accessionsSB = new StringBuilder();
                    for (Accession acc : psm.getAccessions()) {
                        if (accessionsSB.length() > 0) {
                            accessionsSB.append(",");
                        }
                        accessionsSB.append(acc.getAccession());
                    }

                    StringBuilder scores = new StringBuilder();
                    StringBuilder idRanks = new StringBuilder();

                    if (isCombinedFDRScoreCalculated()) {
                        ScoreModel combinedFDR = psm.getFDRScore();
                        if (!combinedFDR.getValue().equals(Double.NaN)) {
                            scores.append("[");
                            scores.append(combinedFDR);
                            scores.append("]");
                        }
                    }
                    for (ReportPSM rp : psm.getPSMs()) {
                        scores.append(rp.getScores());

                        idRanks.append("[");
                        idRanks.append(rp.getIdentificationRanks());
                        idRanks.append("]");
                    }

                    writer.append(
                            "\"" + psm.getSequence() + "\"" + separator +
                            "\"" + psm.getModificationsString() + "\"" + separator +
                            "\"" + psm.getCharge() + "\"" + separator +
                            "\"" + psm.getMassToCharge() + "\"" + separator +
                            "\"" + psm.getDeltaMass() + "\"" + separator +
                            "\"" + psm.getDeltaPPM() + "\"" + separator +
                            "\"" + psm.getRetentionTime() + "\"" + separator +
                            "\"" + psm.getMissedCleavages() + "\"" + separator +
                            "\"" + psm.getSourceID() + "\"" + separator +
                            "\"" + accessionsSB.toString() + "\"" + separator +
                            "\"" + scores.toString() + "\"" + separator +
                            "\"" + idRanks.toString() + "\"" + separator +
                            "\"" + psm.getIsDecoy() + "\"" + separator +
                            "\"" + psm.getIsFDRGood() + "\"" + separator +
                            "\n"
                            );
                }
            } else {
                // spectral counting export
                if (item instanceof ReportPSM) {
                    ReportPSM psm = (ReportPSM)item;

                    String exportLine =
                            "\"" + psm.getFileName() +  "\"" + separator +
                            "\"" + psm.getSequence() + "\"" + separator +
                            "\"" + psm.getModificationsString() + "\"" + separator +
                            "\"" + psm.getCharge() + "\"" + separator +
                            "\"" + psm.getMassToCharge() + "\"" + separator +
                            "\"" + psm.getDeltaMass() + "\"" + separator +
                            "\"" + psm.getDeltaPPM() + "\"" + separator +
                            "\"" + psm.getRetentionTime() + "\"" + separator +
                            "\"" + psm.getMissedCleavages() + "\"" + separator +
                            "\"" + psm.getSourceID() + "\"" + separator +
                            "\"" + psm.getSpectrumTitle() + "\"" + separator +
                            "\"" + psm.getScores() + "\"" + separator +
                            "\"" + psm.getIdentificationRanks() + "\"" + separator +
                            "\"" + psm.getIsDecoy() + "\"" + separator +
                            "\"" + ((psm.getSpectrum().getIsUnique() != null) ? psm.getSpectrum().getIsUnique() : false) + "\"";

                    for (Accession accession : psm.getAccessions()) {
                        writer.append("\"" + accession.getAccession() + "\"" + separator);
                        writer.append(exportLine);
                        writer.append("\n");
                    }
                }
            }
        }

        writer.flush();
    }


    /**
     * Writes the PSM report into mzIdentML.
     *
     * @throws IOException
     */
    public void exportMzIdentML(Writer writer, Long fileID,
            Boolean filterExport) throws IOException {
        LOGGER.info("start writing mzIdentML file");

        UnimodParser unimodParser = new UnimodParser();
        MzIdentMLMarshaller m = new MzIdentMLMarshaller();

        // XML header
        writer.write(m.createXmlHeader() + "\n");
        writer.write(m.createMzIdentMLStartTag("PIAExport for PSMs") + "\n");


        // there are some variables needed for additional tags later
        Map<String, DBSequence> sequenceMap = new HashMap<String, DBSequence>();
        Map<String, uk.ac.ebi.jmzidml.model.mzidml.Peptide> peptideMap =
                new HashMap<String, uk.ac.ebi.jmzidml.model.mzidml.Peptide>();
        Map<String, PeptideEvidence> pepEvidenceMap =
                new HashMap<String, PeptideEvidence>();
        Map<Long, SpectrumIdentificationList> silMap =
                new HashMap<Long, SpectrumIdentificationList>();
        AnalysisSoftware piaAnalysisSoftware = new AnalysisSoftware();
        Inputs inputs = new Inputs();
        AnalysisProtocolCollection analysisProtocolCollection =
                new AnalysisProtocolCollection();
        AnalysisCollection analysisCollection = new AnalysisCollection();

        // write common tags
        writeCommonMzIdentMLTags(writer, m, unimodParser,
                sequenceMap, peptideMap, pepEvidenceMap, silMap,
                piaAnalysisSoftware, inputs,
                analysisProtocolCollection, analysisCollection,
                fileID, filterExport, false);

        for (SpectrumIdentificationList sil : silMap.values()) {
            if (fileID > 0) {
                // the "final PSM list" flag, if only this PSM list is exported
                CvParam tempCvParam = MzIdentMLTools.createPSICvParam(
                        OntologyConstants.FINAL_PSM_LIST,
                        null);
                sil.getCvParam().add(tempCvParam);
            } else {
                // the "intermediate PSM list" flag, if also the overview is exported
                CvParam tempCvParam = MzIdentMLTools.createPSICvParam(
                        OntologyConstants.INTERMEDIATE_PSM_LIST,
                        null);
                sil.getCvParam().add(tempCvParam);
            }
        }

        // if the export is for the overview, export the PSM sets
        Map<String, SpectrumIdentificationItem> combinedSiiMap =
                new HashMap<String, SpectrumIdentificationItem>();
        if (fileID < 1) {
            // create the spectrumIdentificationList for PSM sets
            SpectrumIdentificationList combinedSil = createSpectrumIdentificationListForOverview(
                    sequenceMap, peptideMap, pepEvidenceMap, combinedSiiMap,filterExport);
            silMap.put(0L, combinedSil);

            // create the protocol and SpectrumIdentification
            SpectrumIdentification combiningId =
                    createCombinedSpectrumIdentification(
                            piaAnalysisSoftware,
                            filterExport ? getFilters(0L) : null );

            analysisCollection.getSpectrumIdentification().add(combiningId);
            combiningId.setSpectrumIdentificationList(combinedSil);

            analysisProtocolCollection.getSpectrumIdentificationProtocol()
            .add(combiningId.getSpectrumIdentificationProtocol());
        }

        // now write out the mzIdentML tags
        m.marshal(analysisCollection, writer);
        writer.write("\n");

        m.marshal(analysisProtocolCollection, writer);
        writer.write("\n");

        writer.write(m.createDataCollectionStartTag() + "\n");

        m.marshal(inputs, writer);
        writer.write("\n");


        writer.write(m.createAnalysisDataStartTag() + "\n");

        // write out the spectrum identification lists
        for (SpectrumIdentificationList siList : silMap.values()) {
            m.marshal(siList, writer);
            writer.write("\n");
        }

        writer.write(m.createAnalysisDataClosingTag() + "\n");

        writer.write(m.createDataCollectionClosingTag() + "\n");

        writer.write(m.createMzIdentMLClosingTag());

        writer.flush();
        LOGGER.info("writing of mzIdentML done");
    }


    /**
     * Writes (and creates) the MzIdentML tags which are common for an export
     * with and without the ProteinDetectionList.
     * <p>
     * All given parameters need only to be created with "new", they are filled
     * in this procedure.
     * <p>
     * The mzIdentML file will be written up to the {@link SequenceCollection}.
     *
     * @param writer the output writer, i.e. where the file goes
     * @param m the used marshaller
     * @param unimodParser the prior initialised unimodParser
     * @param psiCV the PSI CV
     * @param unimodCV the UniMod CV
     * @param unitCV the unit CV
     * @param sequenceMap a map holding the {@link DBSequence}s, mapping from
     * their IDs
     * @param peptideMap a map holding the {@link uk.ac.ebi.jmzidml.model.mzidml.Peptide}s,
     * mapping from their IDs
     * @param pepEvidenceMap a map holding the {@link PeptideEvidence}s, mapping
     * from their IDs
     * @param silMap a map holding the {@link SpectrumIdentificationList}s for
     * each file, mapping from the fileID (0 for the PSM sets)
     * @param combinedSiiMap map containing the {@link SpectrumIdentificationItem}s
     * of the PSM sets
     * @param piaAnalysisSoftware PIA as an {@link AnalysisSoftware}
     * @param inputs the {@link Inputs} data
     * @param analysisProtocolCollection
     * @param analysisCollection
     * @param fileID the ID of the file for the report (0 for overview AND all
     * other together)
     * @param psmFilterMap a map from the file ID to the applied filters, may be
     * null (no filters set or for protein export)
     * @throws IOException
     */
    public void writeCommonMzIdentMLTags(
            Writer writer, MzIdentMLMarshaller m, UnimodParser unimodParser,
            Map<String, DBSequence> sequenceMap,
            Map<String, uk.ac.ebi.jmzidml.model.mzidml.Peptide> peptideMap,
            Map<String, PeptideEvidence> pepEvidenceMap,
            Map<Long, SpectrumIdentificationList> silMap,       // TODO: get rid of the map, if decided to write only one SIL
            AnalysisSoftware piaAnalysisSoftware,
            Inputs inputs,
            AnalysisProtocolCollection analysisProtocolCollection,
            AnalysisCollection analysisCollection,
            Long fileID, Boolean filterPSMs,
            Boolean forProteinExport)   // TODO: this needs massive cleaning!
                    throws IOException {

        // the CV list
        CvList cvList = new CvList();

        cvList.getCv().add(MzIdentMLTools.getCvPSIMS());
        cvList.getCv().add(UnimodParser.getCv());
        cvList.getCv().add(MzIdentMLTools.getUnitOntology());

        m.marshal(cvList, writer);
        writer.write("\n");


        // AnalysisSoftware
        AnalysisSoftwareList analysisSoftwareList = new AnalysisSoftwareList();

        piaAnalysisSoftware.setName("PIA");
        piaAnalysisSoftware.setId("AS_PIA");
        piaAnalysisSoftware.setVersion(PIAConstants.version);

        Param tempParam = new Param();
        CvParam tempCvParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.PIA,
                null);
        tempParam.setParam(tempCvParam);
        piaAnalysisSoftware.setSoftwareName(tempParam);

        analysisSoftwareList.getAnalysisSoftware().add(piaAnalysisSoftware);

        for (AnalysisSoftware software
                : analysisSoftware.values()) {
            analysisSoftwareList.getAnalysisSoftware().add(software);
        }

        m.marshal(analysisSoftwareList, writer);
        writer.write("\n");


        // get the information from PIA XML file
        if (fileID > 0) {
            // get spectrumIdentification of the file
            SpectrumIdentification specID = inputFiles.get(fileID).
                    getAnalysisCollection().getSpectrumIdentification().get(0);

            for (InputSpectra inputSpectra : specID.getInputSpectra()) {
                // add the spectraData
                inputs.getSpectraData().add(
                        spectraData.get(inputSpectra.getSpectraDataRef()));
            }

            for (SearchDatabaseRef dbRef : specID.getSearchDatabaseRef()) {
                // add the search databases
                inputs.getSearchDatabase().add(searchDatabases.get(dbRef.getSearchDatabaseRef()));
            }
        } else {
            inputs.getSearchDatabase().addAll(searchDatabases.values());
            inputs.getSpectraData().addAll(spectraData.values());
        }

        SourceFile sourceFile = new SourceFile();
        sourceFile.setId("SF_pia_xml");
        sourceFile.setLocation(fileName);
        sourceFile.setName("PIA-XML-file");
        sourceFile.setExternalFormatDocumentation(
                PIAConstants.PIA_REPOSITORY_LOCATION);
        FileFormat fileFormat = new FileFormat();
        tempCvParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.PIA_XML_FORMAT,
                null);
        fileFormat.setCvParam(tempCvParam);
        sourceFile.setFileFormat(fileFormat);
        inputs.getSourceFile().add(sourceFile);


        Map<String, SpectrumIdentification> spectrumIdentificationMap =
                new HashMap<String, SpectrumIdentification>();
        // maps from the searchDB to the files, which use it
        Map<String, Set<Long>> dbsInFiles = new HashMap<String, Set<Long>>();

        List<PIAInputFile> fileList;
        if (fileID > 0) {
            fileList = new ArrayList<PIAInputFile>(1);

            if (fileReportPSMs.containsKey(fileID)) {
                fileList.add(inputFiles.get(fileID));
            }
        } else {
            fileList = new ArrayList<PIAInputFile>();

            for (PIAInputFile file : inputFiles.values()) {
                if (fileReportPSMs.containsKey(file.getID())) {
                    fileList.add(file);
                }
            }
        }
        for (PIAInputFile file : fileList) {
            if (file.getAnalysisCollection() != null) {
                for (SpectrumIdentification specID
                        : file.getAnalysisCollection().getSpectrumIdentification()) {
                    for (SearchDatabaseRef ref
                            : specID.getSearchDatabaseRef()) {
                        Set<Long> files =
                                dbsInFiles.get(ref.getSearchDatabaseRef());
                        if (files == null) {
                            files = new HashSet<Long>();
                            dbsInFiles.put(ref.getSearchDatabaseRef(), files);
                        }
                        files.add(file.getID());
                    }

                    if (!forProteinExport) {
                        analysisCollection.getSpectrumIdentification().add(specID);
                        spectrumIdentificationMap.put(specID.getId(), specID);
                    }

                    SpectrumIdentificationList sil =
                            new SpectrumIdentificationList();
                    sil.setId("SIL_" + specID.getId());
                    specID.setSpectrumIdentificationList(sil);

                    silMap.put(file.getID(), sil);
                }
            }
        }

        // build up the DBSequence, Peptide, PeptideEvidence and SpectrumIdentificationLists now
        for (PIAInputFile file : fileList) {
            if (file.getID() > 0) {
                // TODO: the ranking is problematic... what score should be used for ranking?
                // most probable would be the one used for protein scoring or
                // used for FDRScore calculation.
                // if this is not valid, use the search engine main score
                String rankScoreShort = null;

                SpectrumIdentificationList sil =
                        createSpectrumIdentificationListForFile(file.getID(),
                                sequenceMap, peptideMap, pepEvidenceMap,
                                dbsInFiles, unimodParser,
                                rankScoreShort,
                                (fileID > 0) ? filterPSMs : false);

                if ((sil != null) && (silMap.containsKey(file.getID()))) {
                    // use the already set ID
                    sil.setId(silMap.get(file.getID()).getId());
                }

                if (!forProteinExport) {
                    silMap.put(file.getID(), sil);
                }
            }
        }

        SequenceCollection sequenceCollection = new SequenceCollection();

        // add the DBSequences into their list
        for (DBSequence dbSequence : sequenceMap.values()) {
            sequenceCollection.getDBSequence().add(dbSequence);
        }

        // add the peptides in their list
        for (uk.ac.ebi.jmzidml.model.mzidml.Peptide peptide
                : peptideMap.values()) {
            sequenceCollection.getPeptide().add(peptide);
        }

        // add the peptideEvidences into their list
        for (PeptideEvidence pe : pepEvidenceMap.values()) {
            sequenceCollection.getPeptideEvidence().add(pe);
        }

        m.marshal(sequenceCollection, writer);
        writer.write("\n");


        // update and write the analysisCollection and the analysisProtocolCollection
        if (!forProteinExport) {
            for (PIAInputFile file : fileList) {
                if (file.getID() > 0) {
                    // add all needed spectrum identification protocols
                    for (SpectrumIdentificationProtocol specIdProt
                            : file.getAnalysisProtocolCollection().getSpectrumIdentificationProtocol()) {
                        if ((specIdProt.getEnzymes() != null) &&
                                (specIdProt.getEnzymes().getEnzyme().size() < 1)) {
                            // no enzymes given, sad, but possible
                            specIdProt.setEnzymes(null);
                        } else if (specIdProt.getEnzymes() != null) {
                            // if there are enzymes, check whether they can be given names by their regexp
                            for (Enzyme enzyme : specIdProt.getEnzymes().getEnzyme()) {
                                if ((enzyme.getEnzymeName() == null) && (enzyme.getSiteRegexp() != null)) {
                                    CleavageAgent agent = CleavageAgent.getBySiteRegexp(enzyme.getSiteRegexp());
                                    if (agent != null) {
                                        ParamList enzymeNameList = new ParamList();

                                        CvParam cvParam = MzIdentMLTools.createCvParam(
                                                agent.getAccession(),
                                                MzIdentMLTools.getCvPSIMS(),
                                                agent.getName(),
                                                null);

                                        enzymeNameList.getCvParam().add(cvParam);
                                        enzyme.setEnzymeName(enzymeNameList);
                                    }
                                }
                            }
                        }

                        if (specIdProt.getAdditionalSearchParams() == null) {
                            specIdProt.setAdditionalSearchParams(new ParamList());
                        }

                        for (SearchModification mod
                                : specIdProt.getModificationParams().getSearchModification()) {
                            if (mod.getCvParam().size() < 1) {
                                // the cvParam of the modification is not set, try to do so
                                ModT unimod = unimodParser.
                                        getModificationByMass(
                                                Float.valueOf(mod.getMassDelta()).doubleValue(),
                                                mod.getResidues());

                                if (unimod != null) {
                                    tempCvParam = MzIdentMLTools.createCvParam(
                                            "UNIMOD:" + unimod.getRecordId(),
                                            UnimodParser.getCv(),
                                            unimod.getTitle(),
                                            null);

                                    mod.getCvParam().add(tempCvParam);
                                    mod.setMassDelta(
                                            unimod.getDelta().getMonoMass().floatValue());
                                }
                            }
                        }

                        if (isFDRCalculated(file.getID())) {
                            if (getFileFDRData().get(file.getID()).
                                    getDecoyStrategy().equals(FDRData.DecoyStrategy.ACCESSIONPATTERN)) {
                                tempCvParam = new CvParam();
                                tempCvParam.setAccession("MS:1001283");
                                tempCvParam.setCv(MzIdentMLTools.getCvPSIMS());
                                tempCvParam.setName("decoy DB accession regexp");
                                tempCvParam.setValue(getFileFDRData().get(file.getID()).getDecoyPattern());
                            } else {
                                tempCvParam = new CvParam();
                                tempCvParam.setAccession("MS:1001454");
                                tempCvParam.setCv(MzIdentMLTools.getCvPSIMS());
                                tempCvParam.setName("quality estimation with implicite decoy sequences");
                            }

                            specIdProt.getAdditionalSearchParams()
                            .getCvParam().add(tempCvParam);

                            tempCvParam = MzIdentMLTools.createPSICvParam(
                                    OntologyConstants.PIA_USED_TOP_IDENTIFICATIONS,
                                    fileTopIdentifications.get(file.getID()).toString());
                            specIdProt.getAdditionalSearchParams().getCvParam().add(tempCvParam);
                        }

                        tempCvParam = MzIdentMLTools.createPSICvParam(
                                OntologyConstants.PIA_FDRSCORE_CALCULATED,
                                isFDRCalculated(file.getID()).toString());
                        specIdProt.getAdditionalSearchParams().getCvParam().add(tempCvParam);

                        if (fileID.equals(file.getID()) && filterPSMs) {
                            // add the filters
                            for (AbstractFilter filter : getFilters(fileID)) {
                                if (filter instanceof PSMScoreFilter) {
                                    // if score filters are set, they are the threshold

                                    ScoreModelEnum scoreModel =
                                            ScoreModelEnum.getModelByDescription(
                                                    ((PSMScoreFilter) filter).getScoreShortName());

                                    if (specIdProt.getThreshold() == null) {
                                        specIdProt.setThreshold(new ParamList());
                                    }

                                    if (!scoreModel.equals(ScoreModelEnum.UNKNOWN_SCORE)) {

                                        tempCvParam = new CvParam();
                                        tempCvParam.setAccession(scoreModel.getCvAccession());
                                        tempCvParam.setCv(MzIdentMLTools.getCvPSIMS());
                                        tempCvParam.setName(scoreModel.getCvName());
                                        tempCvParam.setValue(filter.getFilterValue().toString());

                                        specIdProt.getThreshold().getCvParam().add(tempCvParam);
                                    } else {
                                        // TODO: also make scores from OBO available
                                        UserParam userParam = new UserParam();
                                        userParam.setName(((PSMScoreFilter) filter).getModelName());
                                        userParam.setValue(filter.getFilterValue().toString());

                                        specIdProt.getThreshold().getUserParam().add(userParam);
                                    }
                                } else {
                                    // all other report filters are AdditionalSearchParams
                                    tempCvParam = MzIdentMLTools.createPSICvParam(
                                            OntologyConstants.PIA_FILTER,
                                            filter.toString());
                                    specIdProt.getAdditionalSearchParams().getCvParam().add(tempCvParam);
                                }
                            }
                        }

                        // check, if the threshold is set by now
                        if ((specIdProt.getThreshold() == null) ||
                                (specIdProt.getThreshold().getParamGroup().size() < 1)) {
                            if (specIdProt.getThreshold() == null) {
                                specIdProt.setThreshold(new ParamList());
                            }

                            tempCvParam = new CvParam();
                            tempCvParam.setAccession("MS:1001494");
                            tempCvParam.setCv(MzIdentMLTools.getCvPSIMS());
                            tempCvParam.setName("no threshold");
                            specIdProt.getThreshold().getCvParam().add(tempCvParam);
                        }

                        // at the moment, PIA does "no special processing" as described in the OBO for
                        tempCvParam = new CvParam();
                        tempCvParam.setAccession("MS:1002495");
                        tempCvParam.setCv(MzIdentMLTools.getCvPSIMS());
                        tempCvParam.setName("no special processing");
                        specIdProt.getAdditionalSearchParams().getCvParam().add(tempCvParam);


                        analysisProtocolCollection.getSpectrumIdentificationProtocol().add(specIdProt);
                    }
                }
            }
        } else {
            silMap.clear();
        }
    }


    /**
     * Creates a {@link SpectrumIdentificationList} for the PSMs in the file
     * given by its ID. This is only for real files, not for the overview  with
     * ID=0.
     * <p>
     * As the sequenceMap, peptideMap and peptideEvidenceMap are filled during
     * the creation of the SpectrumIdentificationList, their contents should be
     * added to the respective lists after this call.
     *
     * @param fileID the file id (>0)
     * @param sequenceMap a map containing the {@link DBSequence} of this and
     * other files, mapping from the accession
     * @param peptideMap a map containing the {@link uk.ac.ebi.jmzidml.model.mzidml.Peptide}s
     * of this and other files, mapping from the IDs
     * @param peptideEvidenceMap a map containing the {@link PeptideEvidence}s
     * of this and other files, mapping from the IDs
     * @param spectraDataMap mapping from the {@link SpectraData} used for the
     * {@link SpectrumIdentification}
     * @param dbsInFiles maps from the ID of a {@link SearchDatabase} to a set
     * of {@link PIAInputFile}s IDs, which use this database
     * @param unimodParser the used unimodParser
     * @param psiCV the PSI controlled vocabulary
     * @param unitCV the used controlled vocabulary for the units
     * @return a {@link SpectrumIdentificationList} for the file's PSMs or null,
     * if the file does not exist or has no PSMs
     */
    public SpectrumIdentificationList createSpectrumIdentificationListForFile(
            Long fileID,
            Map<String, DBSequence> sequenceMap,
            Map<String, uk.ac.ebi.jmzidml.model.mzidml.Peptide> peptideMap,
            Map<String, PeptideEvidence> peptideEvidenceMap,
            Map<String, Set<Long>> dbsInFiles,
            UnimodParser unimodParser,
            String rankScoreShort, Boolean filterPSMs) {
        if ((fileID == 0) || !fileReportPSMs.containsKey(fileID)) {
            LOGGER.warn("invalid file ID " + fileID);
            return null;
        }

        SpectrumIdentificationList sil = new SpectrumIdentificationList();

        Map<String, SpectrumIdentificationResult> specIdResMap =
                new HashMap<String, SpectrumIdentificationResult>();

        // each PSM is one SpectrumIdentificationItem, iterate over the PSMs
        for (ReportPSM psm : fileReportPSMs.get(fileID)) {
            // first build or get the peptide of the PSM
            String pepId = psm.getPeptideStringID(true);
            uk.ac.ebi.jmzidml.model.mzidml.Peptide peptide =
                    peptideMap.get(pepId);
            if (peptide == null) {
                peptide = new uk.ac.ebi.jmzidml.model.mzidml.Peptide();
                peptide.setId("PEP_" + pepId);

                peptide.setPeptideSequence(psm.getSequence());

                for (Map.Entry<Integer, Modification> modIt
                        : psm.getModifications().entrySet()) {
                    uk.ac.ebi.jmzidml.model.mzidml.Modification mod =
                            new uk.ac.ebi.jmzidml.model.mzidml.Modification();

                    ModT uniMod = unimodParser.getModification(
                            modIt.getValue().getAccession(),
                            modIt.getValue().getDescription(),
                            modIt.getValue().getMass(),
                            modIt.getValue().getResidue().toString());

                    if (uniMod != null) {
                        mod = unimodParser.createModification(uniMod,
                                modIt.getKey(),
                                modIt.getValue().getResidue().toString());
                    } else {
                        // build an "unknown modification"
                        mod = new uk.ac.ebi.jmzidml.model.mzidml.Modification();
                        mod.getResidues().add(
                                modIt.getValue().getResidue().toString());
                        mod.setLocation(modIt.getKey());

                        CvParam tempCvParam = new CvParam();
                        tempCvParam.setAccession("MS:1001460");
                        tempCvParam.setCv(MzIdentMLTools.getCvPSIMS());
                        tempCvParam.setName("unknown modification");
                        mod.getCvParam().add(tempCvParam);
                    }

                    if (mod.getResidues().contains(".")) {
                        // this is an N- or C-terminal modification -> give no residue
                        mod.getResidues().clear();
                    }

                    peptide.getModification().add(mod);

                    // TODO: handle SubstitutionModifications
                }

                peptideMap.put(pepId, peptide);
            }


            // then build the peptide evidences
            for (Accession accession : psm.getAccessions()) {
                boolean foundOccurrence = false;

                for (AccessionOccurrence occurrence
                        : psm.getPeptide().getAccessionOccurrences()) {
                    // look if occurrences are given in the compilation
                    if (accession.getAccession().equals(
                            occurrence.getAccession().getAccession())) {
                        String evidenceID = createPeptideEvidenceID(
                                psm.getPeptideStringID(true),
                                occurrence.getStart(), occurrence.getEnd(),
                                accession);
                        if (!peptideEvidenceMap.containsKey(evidenceID)) {
                            PeptideEvidence pepEvi = createPeptideEvidence(evidenceID,
                                    occurrence.getStart(), occurrence.getEnd(),
                                    psm.getIsDecoy(), peptide,
                                    accession,
                                    sequenceMap, dbsInFiles);

                            peptideEvidenceMap.put(evidenceID, pepEvi);
                        }
                        foundOccurrence = true;
                    }
                }

                if (!foundOccurrence) {
                    // no occurrence given, so create peptideEvidence without position
                    String evidenceID = createPeptideEvidenceID(
                            psm.getPeptideStringID(true),
                            null, null, accession);

                    if (!peptideEvidenceMap.containsKey(evidenceID)) {
                        PeptideEvidence pepEvi = createPeptideEvidence(evidenceID,
                                null, null,
                                psm.getIsDecoy(), peptide,
                                accession,
                                sequenceMap, dbsInFiles);

                        peptideEvidenceMap.put(evidenceID, pepEvi);
                    }
                }
            }

            putPsmInSpectrumIdentificationResultMap(psm, specIdResMap,
                    peptideMap, peptideEvidenceMap,
                    rankScoreShort, filterPSMs);
        }

        sil.getSpectrumIdentificationResult().addAll(specIdResMap.values());

        return sil;
    }


    /**
     * Creates a {@link SpectrumIdentificationList} for the PSM sets of the
     * overview.
     * <p>
     * The sequenceMap, peptideMap and peptideEvidenceMap are no longer filled,
     * but all entries should be available from prior calls of
     * {@link #createSpectrumIdentificationListForFile(Long, Map, Map, Map, Map, UnimodParser, Cv, Cv, String)}
     * for all available files.
     *
     * @param sequenceMap
     * @param peptideMap
     * @param peptideEvidenceMap
     * @param specIDMap
     * @param psiCV
     * @param unitCV
     * @param filterPSMs
     * @return
     */
    public SpectrumIdentificationList createSpectrumIdentificationListForOverview(
            Map<String, DBSequence> sequenceMap,
            Map<String, uk.ac.ebi.jmzidml.model.mzidml.Peptide> peptideMap,
            Map<String, PeptideEvidence> peptideEvidenceMap,
            Map<String, SpectrumIdentificationItem> specIDMap,
            Boolean filterPSMs) {
        SpectrumIdentificationList sil = new SpectrumIdentificationList();
        sil.setId("combined_PSMs");

        Map<String, SpectrumIdentificationResult> specIdResMap =
                new HashMap<String, SpectrumIdentificationResult>();

        // each PSM is one SpectrumIdentificationItem, iterate over the PSMs
        for (ReportPSMSet psmSet : reportPSMSets) {
            SpectrumIdentificationItem sii = putPsmInSpectrumIdentificationResultMap(
                    psmSet, specIdResMap, peptideMap, peptideEvidenceMap, null, filterPSMs);

            specIDMap.put(sii.getId(), sii);

            if (isCombinedFDRScoreCalculated()) {
                ScoreModel fdrScore = psmSet.getFDRScore();

                CvParam tempCvParam = new CvParam();
                tempCvParam.setAccession(ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getCvAccession());
                tempCvParam.setCv(MzIdentMLTools.getCvPSIMS());
                tempCvParam.setName(ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getCvName());
                if (fdrScore != null) {
                    tempCvParam.setValue(fdrScore.getValue().toString());
                } else {
                    tempCvParam.setValue(Double.toString(Double.NaN));
                }
                sii.getCvParam().add(tempCvParam);
            }
        }

        sil.getSpectrumIdentificationResult().addAll(specIdResMap.values());

        // the "final PSM list" flag, because the overview is always the final (on PSM level)
        CvParam tempCvParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.FINAL_PSM_LIST,
                null);
        sil.getCvParam().add(tempCvParam);

        return sil;
    }


    /**
     * Creates a {@link PeptideEvidence} with the given parameters.
     *
     * @param evidenceID
     * @param start
     * @param end
     * @param isDecoy
     * @param peptide
     * @param accession
     * @param sequenceMap
     * @param dbsInFiles
     * @param psiCV
     * @return
     */
    private PeptideEvidence createPeptideEvidence(String evidenceID,
            Integer start,
            Integer end,
            Boolean isDecoy,
            uk.ac.ebi.jmzidml.model.mzidml.Peptide peptide,
            Accession accession,
            Map<String, DBSequence> sequenceMap,
            Map<String, Set<Long>> dbsInFiles) {
        PeptideEvidence pepEvi = new PeptideEvidence();

        DBSequence dbSequence = sequenceMap.get(accession);
        if (dbSequence == null) {
            // create the dbSequence entry, if it is not yet created
            dbSequence = new DBSequence();

            dbSequence.setAccession(accession.getAccession());
            dbSequence.setId("DbSeq_" + accession.getAccession());

            if ((accession.getDbSequence() != null) &&
                    (accession.getDbSequence().length() > 0)) {
                dbSequence.setLength(accession.getDbSequence().length());
                dbSequence.setSeq(accession.getDbSequence());
            }

            String dbRef = null;
            CvParam descCvParam = new CvParam();
            descCvParam.setAccession("MS:1001088");
            descCvParam.setName("protein description");
            descCvParam.setCv(MzIdentMLTools.getCvPSIMS());

            // look for a good description
            for (Map.Entry<Long, String> descIt : accession.getDescriptions().entrySet()) {

                if ((descIt.getValue() != null) && (descIt.getValue().trim().length() > 0)) {
                    // take this description and DBsequence_ref
                    if ((descCvParam.getValue() == null) ||
                            (descIt.getValue().trim().length() > descCvParam.getValue().length())) {
                        descCvParam.setValue(descIt.getValue().trim());
                    }

                    for (String ref : accession.getSearchDatabaseRefs()) {
                        if (dbsInFiles.get(ref).contains(descIt.getKey())) {
                            dbRef = ref;
                        }
                    }
                }
            }

            if ((descCvParam.getValue() != null)) {
                dbSequence.getCvParam().add(descCvParam);
            }

            // add info for the DBSequence
            descCvParam = new CvParam();
            descCvParam.setAccession("MS:1001344");
            descCvParam.setName("AA sequence");
            descCvParam.setCv(MzIdentMLTools.getCvPSIMS());
            dbSequence.getCvParam().add(descCvParam);

            if (dbRef == null) {
                // no description found -> use any sequenceRef
                dbRef = accession.getSearchDatabaseRefs().iterator().next();
            }

            dbSequence.setSearchDatabase(searchDatabases.get(dbRef));

            sequenceMap.put(accession.getAccession(), dbSequence);
        }
        pepEvi.setDBSequence(dbSequence);

        pepEvi.setId(evidenceID);

        pepEvi.setIsDecoy(isDecoy);
        pepEvi.setPeptide(peptide);
        if (start != null) {
            pepEvi.setStart(start);
        }
        if (end != null) {
            pepEvi.setEnd(end);
        }

        return pepEvi;
    }


    /**
     * Creates a String containing the ID of a {@link PeptideEvidence} with the
     * given information. This string is used for the MzIdentML export.
     *
     * @param peptideStringID the peptideStringID containing the seqeunce and
     * modifications as in {@link PeptideSpectrumMatch#getPeptideStringID(boolean)}
     * @param start the start in the dbSequence (if known)
     * @param end the end in the dbSequence (if known)
     * @param accession the accession of the protein
     * @return
     */
    public String createPeptideEvidenceID(String peptideStringID,
            Integer start, Integer end, Accession accession) {
        StringBuilder evidenceIDstr = new StringBuilder("PE_");
        evidenceIDstr.append(peptideStringID);
        if ((start != null) && (end != null)) {
            evidenceIDstr.append("-");
            evidenceIDstr.append(start);
            evidenceIDstr.append("-");
            evidenceIDstr.append(end);
        }
        evidenceIDstr.append("-");
        evidenceIDstr.append(accession.getAccession());

        return evidenceIDstr.toString();
    }


    /**
     * Creates a {@link SpectrumIdentificationItem} for the given PSM and puts
     * it into its {@link SpectrumIdentificationResult}, which will be created
     * if necessary.
     *
     * @param psm
     * @param specIdResMap
     * @param peptideMap
     * @param peptideEvidenceMap
     * @param psiCV
     * @param unitCV
     * @param rankScoreShort
     * @return
     */
    public SpectrumIdentificationItem putPsmInSpectrumIdentificationResultMap(
            PSMReportItem psm,
            Map<String, SpectrumIdentificationResult> specIdResMap,
            Map<String, uk.ac.ebi.jmzidml.model.mzidml.Peptide> peptideMap,
            Map<String, PeptideEvidence> peptideEvidenceMap,
            String rankScoreShort,
            Boolean filterPSM) {

        // build the SpectrumIdentificationItem into its result
        Long fileID = (psm instanceof ReportPSM) ? ((ReportPSM)psm).getFileID() : 0L;

        LOGGER.debug("instance of psm: " + psm.getClass().getName());

        String psmIdentificationKey = getSpectrumIdentificationResultID(psm, fileID);

        SpectrumIdentificationResult specIdRes = specIdResMap.get(psmIdentificationKey);
        if (specIdRes == null) {
            // this spectrum has no identification yet
            specIdRes = new SpectrumIdentificationResult();

            specIdRes.setId(psmIdentificationKey);
            specIdRes.setSpectrumID(psm.getSourceID());
            specIdRes.setSpectraData(getRepresentingSpectraData(psm));

            specIdResMap.put(psmIdentificationKey, specIdRes);
        } else {
            // enhance the spectrum with the spectrumID, if available
            if ((specIdRes.getSpectrumID() == null) &&
                    (psm.getSourceID() != null)) {
                specIdRes.setSpectrumID(psm.getSourceID());
            }

            // enhance with spectraData, if available
            if (specIdRes.getSpectraData() == null) {
                SpectraData specData = getRepresentingSpectraData(psm);
                if (specData != null) {
                    specIdRes.setSpectraData(specData);
                }
            }
        }


        if (!getCreatePSMSets() && (psm instanceof ReportPSMSet)) {
            psmIdentificationKey =
                    getSpectrumIdentificationItemID(psm, ((ReportPSMSet) psm).getPSMs().get(0).getFileID());
            psmIdentificationKey += ":set";
        } else {
            psmIdentificationKey = getSpectrumIdentificationItemID(psm, fileID);
        }

        for (SpectrumIdentificationItem itemIt
                : specIdRes.getSpectrumIdentificationItem()) {
            if (itemIt.getId().equals(psmIdentificationKey)) {
                // SII already created, return it
                return itemIt;
            }
        }

        SpectrumIdentificationItem sii = new SpectrumIdentificationItem();
        specIdRes.getSpectrumIdentificationItem().add(sii);
        sii.setId(psmIdentificationKey);

        sii.setChargeState(psm.getCharge());
        sii.setExperimentalMassToCharge(psm.getMassToCharge());

        if (filterPSM) {
            List<AbstractFilter> filters = null;
            filters = getFilters(fileID);

            sii.setPassThreshold(
                    FilterFactory.satisfiesFilterList(psm, fileID, filters));
        } else {
            // without filters, always true
            sii.setPassThreshold(true);
        }

        sii.setPeptide(peptideMap.get(psm.getPeptideStringID(true)));
        if ((rankScoreShort == null) || (psm instanceof ReportPSMSet)) {
            sii.setRank(0);
        } else {
            Integer rank =
                    ((ReportPSM)psm).getIdentificationRank(rankScoreShort);
            if (rank != null) {
                sii.setRank(rank);
            } else {
                sii.setRank(0);
            }
        }

        // add the peptideEvidences
        for (Accession accession : psm.getAccessions()) {
            boolean foundOccurrence = false;

            for (AccessionOccurrence occurrence
                    : psm.getPeptide().getAccessionOccurrences()) {
                // look if occurrences are given in the compilation
                if (accession.getAccession().equals(
                        occurrence.getAccession().getAccession())) {
                    String evidenceID = createPeptideEvidenceID(
                            psm.getPeptideStringID(true),
                            occurrence.getStart(), occurrence.getEnd(),
                            accession);

                    PeptideEvidenceRef pepEvidenceRef =
                            new PeptideEvidenceRef();
                    pepEvidenceRef.setPeptideEvidence(
                            peptideEvidenceMap.get(evidenceID));

                    sii.getPeptideEvidenceRef().add(pepEvidenceRef);
                    foundOccurrence = true;
                }
            }

            if (!foundOccurrence) {
                // no occurrence given, so use peptideEvidence without position
                String evidenceID = createPeptideEvidenceID(
                        psm.getPeptideStringID(true),
                        null, null, accession);

                PeptideEvidenceRef pepEvidenceRef =
                        new PeptideEvidenceRef();
                pepEvidenceRef.setPeptideEvidence(
                        peptideEvidenceMap.get(evidenceID));

                sii.getPeptideEvidenceRef().add(pepEvidenceRef);
            }
        }

        if ((psm instanceof ReportPSMSet) && getCreatePSMSets()) {
            // mark as consensus result
            CvParam tempCvParam = new CvParam();
            tempCvParam.setAccession("MS:1002315");
            tempCvParam.setCv(MzIdentMLTools.getCvPSIMS());
            tempCvParam.setName("consensus result");
            sii.getCvParam().add(tempCvParam);
        }

        if (((psm instanceof ReportPSM) || !getCreatePSMSets()) ||
                ((fileID == 0L) && (inputFiles.size() == 2 /* the overview counts */))) {
            // either a single PSM, the PSM sets are actually single PSMs or there is only one input file

            ListIterator<AbstractParam> paramIt = null;
            ListIterator<ScoreModel> scoreIt = null;

            if (psm instanceof ReportPSM) {
                paramIt = ((ReportPSM)psm).getSpectrum().getParams().listIterator();
                scoreIt = ((ReportPSM) psm).getScores().listIterator();
            } else {
                // the psm is a ReportPSMSet
                ReportPSM rPSM = ((ReportPSMSet)psm).getPSMs().get(0);
                paramIt = rPSM.getSpectrum().getParams().listIterator();
                scoreIt = rPSM.getScores().listIterator();

                if (((ReportPSMSet)psm).getPSMs().size() > 1) {
                    LOGGER.error("There should be only one PSM in each set, as no real sets are created!");
                }
            }

            // copy all cvParams over
            while (paramIt.hasNext()) {
                AbstractParam param = paramIt.next();

                if (param instanceof CvParam) {
                    sii.getCvParam().add((CvParam)param);
                } else if (param instanceof UserParam) {
                    sii.getUserParam().add((UserParam)param);
                }
            }

            // add the scores (as cvParams)
            while (scoreIt.hasNext()) {
                ScoreModel score = scoreIt.next();

                if (!score.getType().equals(ScoreModelEnum.UNKNOWN_SCORE)) {
                    CvParam tempCvParam = new CvParam();
                    tempCvParam.setAccession(score.getAccession());
                    tempCvParam.setCv(MzIdentMLTools.getCvPSIMS());
                    tempCvParam.setName(score.getType().getCvName());
                    tempCvParam.setValue(score.getValue().toString());

                    sii.getCvParam().add(tempCvParam);
                } else {
                    // TODO: add unknown scores...
                    // TODO: check CV first... if not there, add as userParam
                }
            }
        }


        if (psm.getRetentionTime() != null) {
            CvParam tempCvParam = new CvParam();
            tempCvParam.setAccession("MS:1000016");
            tempCvParam.setCv(MzIdentMLTools.getCvPSIMS());
            tempCvParam.setName("scan start time");
            tempCvParam.setValue(psm.getRetentionTime().toString());

            tempCvParam.setUnitCv(MzIdentMLTools.getUnitOntology());
            tempCvParam.setUnitName("second");
            tempCvParam.setUnitAccession("UO:0000010");

            sii.getCvParam().add(tempCvParam);
        }

        if (psm.getSpectrumTitle() != null) {
            CvParam tempCvParam = new CvParam();
            tempCvParam.setAccession("MS:1000796");
            tempCvParam.setCv(MzIdentMLTools.getCvPSIMS());
            tempCvParam.setName("spectrum title");
            tempCvParam.setValue(psm.getSpectrumTitle());

            sii.getCvParam().add(tempCvParam);
        }

        if (psm.getDeltaMass() > -1) {
            CvParam tempCvParam = new CvParam();
            tempCvParam.setAccession("MS:1001975");
            tempCvParam.setCv(MzIdentMLTools.getCvPSIMS());
            tempCvParam.setName("delta m/z");
            tempCvParam.setValue(Double.toString(psm.getDeltaMass()));

            tempCvParam.setUnitCv(MzIdentMLTools.getUnitOntology());
            tempCvParam.setUnitName("dalton");
            tempCvParam.setUnitAccession("UO:0000221");

            sii.getCvParam().add(tempCvParam);
        }

        return sii;
    }


    /**
     * Getter for the {@link AnalysisSoftware} with the given reference id.
     *
     * @param spectraDataRef
     * @return
     */
    public AnalysisSoftware getAnalysisSoftware(String analysisSofwareRef) {
        return analysisSoftware.get(analysisSofwareRef);
    }


    /**
     * Getter for the {@link SpectraData} with the given reference id.
     *
     * @param spectraDataRef
     * @return
     */
    public SpectraData getSpectraData(String spectraDataRef) {
        return spectraData.get(spectraDataRef);
    }


    /**
     * Gets a representative of the {@link SpectraData} for the given
     * {@link PSMReportItem}.
     *
     * @param psm
     * @return
     */
    public SpectraData getRepresentingSpectraData(PSMReportItem psm) {
        List<ReportPSM> psmList = null;
        if (psm instanceof ReportPSM) {
            psmList = new ArrayList<ReportPSM>(1);
            psmList.add((ReportPSM)psm);
        } else if (psm instanceof ReportPSMSet) {
            psmList = ((ReportPSMSet) psm).getPSMs();
        }
        for (ReportPSM repPSM : psmList) {
            if ((repPSM.getSpectrum().getSpectrumIdentification().getInputSpectra() != null) &&
                    (repPSM.getSpectrum().getSpectrumIdentification().getInputSpectra().size() > 0)) {
                SpectraData specData =
                        spectraData.get(
                                repPSM.getSpectrum().getSpectrumIdentification().getInputSpectra().get(0).getSpectraDataRef());
                // TODO: make the choice of spectrumID and spectraData more sophisticated
                if (specData != null) {
                    return specData;
                }
            }
        }

        return null;
    }


    /**
     * Returns the used ID for the {@link SpectrumIdentificationItem} in
     * mzIdentML export.
     *
     * @param psm
     * @return
     */
    public String getSpectrumIdentificationItemID(PSMReportItem psm,
            Long fileID) {
        if (itemPSMSetSettings == null) {
            // initialize the settings on first call
            itemPSMSetSettings = new HashMap<String,Boolean>();
            itemPSMSetSettings.put(
                    IdentificationKeySettings.FILE_ID.toString(), true);
            itemPSMSetSettings.put(
                    IdentificationKeySettings.CHARGE.toString(), true);
            itemPSMSetSettings.put(
                    IdentificationKeySettings.RETENTION_TIME.toString(), true);
            itemPSMSetSettings.put(
                    IdentificationKeySettings.MASSTOCHARGE.toString(), true);
            itemPSMSetSettings.put(
                    IdentificationKeySettings.SOURCE_ID.toString(), true);
            itemPSMSetSettings.put(
                    IdentificationKeySettings.SEQUENCE.toString(), true);
            itemPSMSetSettings.put(
                    IdentificationKeySettings.MODIFICATIONS.toString(), true);
        }

        return PeptideSpectrumMatch.getIdentificationKey(itemPSMSetSettings,
                psm.getSequence(), psm.getModificationsString(),
                psm.getCharge(), psm.getMassToCharge(),
                psm.getRetentionTime(), psm.getSourceID(),
                psm.getSpectrumTitle(), fileID);
    }


    /**
     * Returns the used ID for the {@link SpectrumIdentificationResult} in
     * mzIdentML export.
     *
     * @param psm
     * @return
     */
    public String getSpectrumIdentificationResultID(PSMReportItem psm, Long fileID) {
        if (mzIdResultPSMSetSettings == null) {
            // initialize the settings on first call
            mzIdResultPSMSetSettings = new HashMap<String,Boolean>();
            mzIdResultPSMSetSettings.put(IdentificationKeySettings.FILE_ID.toString(), true);
            mzIdResultPSMSetSettings.put(IdentificationKeySettings.CHARGE.toString(), true);
            mzIdResultPSMSetSettings.put(IdentificationKeySettings.RETENTION_TIME.toString(), true);
            mzIdResultPSMSetSettings.put(IdentificationKeySettings.MASSTOCHARGE.toString(), true);
            mzIdResultPSMSetSettings.put(IdentificationKeySettings.SOURCE_ID.toString(), true);
        }

        /*
        mzIdResultPSMSetSettings = new HashMap<String,Boolean>();
        mzIdResultPSMSetSettings.put(IdentificationKeySettings.FILE_ID.toString(), true);
        mzIdResultPSMSetSettings.put(IdentificationKeySettings.CHARGE.toString(), true);

        if (!psmSetSettingsWarnings.containsKey(IdentificationKeySettings.RETENTION_TIME.toString()) ||
                !psmSetSettingsWarnings.get(IdentificationKeySettings.RETENTION_TIME.toString()).contains(fileID)) {
            mzIdResultPSMSetSettings.put(IdentificationKeySettings.RETENTION_TIME.toString(), true);
        }

        mzIdResultPSMSetSettings.put(IdentificationKeySettings.MASSTOCHARGE.toString(), true);

        if (!psmSetSettingsWarnings.containsKey(IdentificationKeySettings.SOURCE_ID.toString()) ||
                !psmSetSettingsWarnings.get(IdentificationKeySettings.SOURCE_ID.toString()).contains(fileID)) {
            mzIdResultPSMSetSettings.put(IdentificationKeySettings.SOURCE_ID.toString(), true);
        }
        */

        return PeptideSpectrumMatch.getIdentificationKey(mzIdResultPSMSetSettings,
                psm.getSequence(), psm.getModificationsString(),
                psm.getCharge(), psm.getMassToCharge(),
                psm.getRetentionTime(), psm.getSourceID(),
                psm.getSpectrumTitle(), fileID);
    }


    /**
     * Create the {@link SpectrumIdentification} (and the
     * {@link SpectrumIdentificationProtocol}) for the combination of PSMs.
     *
     * @param psiCV
     * @param pia
     * @param appliedFilters
     * @return
     */
    public SpectrumIdentification createCombinedSpectrumIdentification(
            AnalysisSoftware pia,
            List<AbstractFilter> appliedFilters) {

        // create the SpectrumIdentificationProtocol for the PSM sets
        SpectrumIdentificationProtocol combiningProtocol =
                new SpectrumIdentificationProtocol();

        combiningProtocol.setId("psm_combination_protocol");
        combiningProtocol.setAnalysisSoftware(pia);
        combiningProtocol.setAdditionalSearchParams(new ParamList());

        Param tempParam = new Param();
        CvParam tempCvParam = new CvParam();
        tempCvParam.setAccession("MS:1001083");
        tempCvParam.setCv(MzIdentMLTools.getCvPSIMS());
        tempCvParam.setName("ms-ms search");
        // this may change in the future, but now we only use ms/ms search
        tempParam.setParam(tempCvParam);
        combiningProtocol.setSearchType(tempParam);

        tempCvParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.PIA_PSM_SETS_CREATED,
                Boolean.toString(createPSMSets));
        combiningProtocol.getAdditionalSearchParams().getCvParam().add(
                tempCvParam);

        tempCvParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.PIA_COMBINED_FDRSCORE_CALCULATED,
                Boolean.toString(isCombinedFDRScoreCalculated()));
        combiningProtocol.getAdditionalSearchParams().getCvParam().add(tempCvParam);

        if (isCombinedFDRScoreCalculated()) {
            tempCvParam = new CvParam();
            tempCvParam.setAccession("MS:1002492");
            tempCvParam.setCv(MzIdentMLTools.getCvPSIMS());
            tempCvParam.setName("consensus scoring");
            combiningProtocol.getAdditionalSearchParams().getCvParam().add(tempCvParam);
        } else {
            tempCvParam = new CvParam();
            tempCvParam.setAccession("MS:1002495");
            tempCvParam.setCv(MzIdentMLTools.getCvPSIMS());
            tempCvParam.setName("no special processing");
            combiningProtocol.getAdditionalSearchParams().getCvParam().add(tempCvParam);
        }

        if ((appliedFilters != null) && (appliedFilters.size() > 0)) {
            // add the filters
            for (AbstractFilter filter : appliedFilters) {
                if (filter instanceof PSMScoreFilter) {
                    // if score filters are set, they are the threshold

                    ScoreModelEnum scoreModel =
                            ScoreModelEnum.getModelByDescription(
                                    ((PSMScoreFilter) filter).getScoreShortName());

                    if (combiningProtocol.getThreshold() == null) {
                        combiningProtocol.setThreshold(new ParamList());
                    }

                    if (!scoreModel.equals(ScoreModelEnum.UNKNOWN_SCORE)) {

                        tempCvParam = new CvParam();
                        tempCvParam.setAccession(scoreModel.getCvAccession());
                        tempCvParam.setCv(MzIdentMLTools.getCvPSIMS());
                        tempCvParam.setName(scoreModel.getCvName());
                        tempCvParam.setValue(filter.getFilterValue().toString());

                        combiningProtocol.getThreshold()
                        .getCvParam().add(tempCvParam);
                    } else {
                        // TODO: also make scores from OBO available

                        UserParam userParam = new UserParam();
                        userParam.setName(((PSMScoreFilter) filter).getModelName());
                        userParam.setValue(filter.getFilterValue().toString());

                        combiningProtocol.getThreshold()
                        .getUserParam().add(userParam);
                    }
                } else {
                    // all other report filters are AdditionalSearchParams
                    tempCvParam = MzIdentMLTools.createPSICvParam(
                            OntologyConstants.PIA_FILTER,
                            filter.toString());
                    combiningProtocol.getAdditionalSearchParams().getCvParam().add(tempCvParam);
                }
            }
        }

        // check, if the threshold is set by now
        if ((combiningProtocol.getThreshold() == null) ||
                (combiningProtocol.getThreshold().getParamGroup().size() < 1)) {
            if (combiningProtocol.getThreshold() == null) {
                combiningProtocol.setThreshold(new ParamList());
            }

            tempCvParam = new CvParam();
            tempCvParam.setAccession("MS:1001494");
            tempCvParam.setCv(MzIdentMLTools.getCvPSIMS());
            tempCvParam.setName("no threshold");
            combiningProtocol.getThreshold().getCvParam().add(tempCvParam);
        }

        SpectrumIdentification combiningId =
                new SpectrumIdentification();

        combiningId.setId("psm_combination");
        combiningId.setSpectrumIdentificationProtocol(combiningProtocol);

        for (SpectraData specData : spectraData.values()) {
            InputSpectra spectra = new InputSpectra();
            spectra.setSpectraData(specData);
            combiningId.getInputSpectra().add(spectra);
        }

        for (SearchDatabase searchDB : searchDatabases.values()) {
            SearchDatabaseRef ref = new SearchDatabaseRef();
            ref.setSearchDatabase(searchDB);
            combiningId.getSearchDatabaseRef().add(ref);
        }

        return combiningId;
    }



    /**
     * Writes some PSM level information.
     *
     * @throws IOException
     */
    public void writePSMInformation(String fileName, boolean calculate) throws IOException {
        Writer writer = null;
        try {
            writer = new FileWriter(fileName, false);
            writePSMInformation(writer, calculate);
        } catch (IOException e) {
            LOGGER.error("Could not write PSM information to " + fileName, e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    LOGGER.error("Cannot close file " + fileName, e);
                }
            }
        }
    }


    /**
     * Writes some PSM level information.
     *
     * @throws IOException
     */
    public void writePSMInformation(Writer writer, boolean calculate) throws IOException {
        FDRData fdrData = null;
        List<Double> fdrThresholds = Arrays.asList(new Double[] {0.01, 0.03, 0.05});
        Double originalFDRThreshold = null;
        String nl = System.getProperty("line.separator");

        writer.append("PSM information about " + fileName + nl);

        // numbers for each file
        for (Long fileID : inputFiles.keySet()) {
            if (fileID < 1) {
                // skip the overview, combined FDR is added later
                continue;
            }

            writer.append(nl + "PSMs in file #" + fileID + " (" + inputFiles.get(fileID).getName() + ")" + nl);
            writer.append("==============================" + nl);
            writer.append("#PSMs: ");
            writer.append(Integer.toString(getNrReportPSMs(fileID)));
            writer.append(nl);

            Boolean fdrCalculated = isFDRCalculated(fileID);
            if (fdrCalculated == null) {
                writer.append("FDR was not calculated or was not able to be calculated.");
                writer.append(nl);
            } else if (fdrCalculated) {
                fdrData = getFileFDRData().get(fileID);
                originalFDRThreshold = fdrData.getFDRThreshold();

                writer.append(nl);
                writer.append("FDR is calculated with " + getScoreName(fdrData.getScoreShortName()));
                writer.append(" using " + getFilesTopIdentifications(fileID) + " top identifications");
                writer.append(nl);
                if (fdrData.getDecoyStrategy().equals(DecoyStrategy.ACCESSIONPATTERN)) {
                    writer.append("Regular expression used to identify decoys: " + fdrData.getDecoyPattern());
                } else {
                    writer.append("Searchengine internal decoys are used.");
                }
                writer.append(nl);
                writer.append(nl);

                writer.append("#PSMs with FDR: ");
                writer.append(Integer.toString(fdrData.getNrItems()));
                writer.append(nl);
                writer.append("  #targets:     ");
                writer.append(Integer.toString(fdrData.getNrTargets()));
                writer.append(nl);
                writer.append("  #decoys:      ");
                writer.append(Integer.toString(fdrData.getNrDecoys()));
                writer.append(nl);

                if (!calculate ||
                        !fdrThresholds.contains(fdrData.getFDRThreshold())) {
                    writer.append("FDR " + fdrData.getFDRThreshold() + ":" + nl);
                    writer.append("  #targets below " + fdrData.getFDRThreshold() + " threshold: ");
                    writer.append(fdrData.getNrFDRGoodTargets().toString());
                    writer.append(nl);
                    writer.append("  #decoys below " + fdrData.getFDRThreshold() + " threshold:  ");
                    writer.append(fdrData.getNrFDRGoodDecoys().toString());
                    writer.append(nl);
                    writer.append("  score at threshold: ");
                    writer.append("" + fdrData.getScoreAtThreshold());
                    writer.append(nl);
                }

                if (calculate) {
                    //TODO: make this faster, calculating always the FDR is time consuming
                    for (Double thr : fdrThresholds) {
                        if (!thr.equals(fdrData.getFDRThreshold())) {
                            fdrData.setFDRThreshold(thr);
                            calculateFDR(fileID);
                        }

                        writer.append("FDR " + thr + ":" + nl);
                        writer.append("  #targets below " + thr + " threshold: ");
                        writer.append(fdrData.getNrFDRGoodTargets().toString());
                        writer.append(nl);
                        writer.append("  #decoys below " + thr + " threshold:  ");
                        writer.append(fdrData.getNrFDRGoodDecoys().toString());
                        writer.append(nl);
                        writer.append("  score at threshold: ");
                        writer.append("" + fdrData.getScoreAtThreshold());
                        writer.append(nl);
                    }

                    // reset the FDR data
                    fdrData.setFDRThreshold(originalFDRThreshold);
                    calculateFDR(fileID);
                }
            }
        }

        // numbers for PSMSets
        writer.append(nl + "PSM sets (create sets = " + createPSMSets + ")" + nl);
        writer.append("==============================" + nl);
        writer.append("#PSM sets: ");
        writer.append(Integer.toString(getNrReportPSMs(0L)));
        writer.append(nl);



        if (isCombinedFDRScoreCalculated()) {
            writer.append("combined FDR score is calculated" + nl);

            fdrData = getFileFDRData().get(0L);
            originalFDRThreshold = fdrData.getFDRThreshold();

            writer.append("#PSM sets with FDR: ");
            writer.append(Integer.toString(fdrData.getNrItems()));
            writer.append(nl);
            writer.append("  #targets:         ");
            writer.append(Integer.toString(fdrData.getNrTargets()));
            writer.append(nl);
            writer.append("  #decoys:          ");
            writer.append(Integer.toString(fdrData.getNrDecoys()));
            writer.append(nl);

            if (!calculate ||
                    !fdrThresholds.contains(fdrData.getFDRThreshold())) {
                writer.append("FDR " + fdrData.getFDRThreshold() + ":" + nl);
                writer.append("  #targets below " + fdrData.getFDRThreshold() + " threshold: ");
                writer.append(fdrData.getNrFDRGoodTargets().toString());
                writer.append(nl);
                writer.append("  #decoys below " + fdrData.getFDRThreshold() + " threshold:  ");
                writer.append(fdrData.getNrFDRGoodDecoys().toString());
                writer.append(nl);
                writer.append("  score at threshold: ");
                writer.append("" + fdrData.getScoreAtThreshold());
                writer.append(nl);
            }

            if (calculate) {
                //TODO: make this better, calculating always the FDR is time consuming
                for (Double thr : fdrThresholds) {
                    fdrData.setFDRThreshold(thr);
                    calculateCombinedFDRScore();
                    writer.append("FDR " + thr + ":" + nl);
                    writer.append("  #targets below " + thr + " threshold: ");
                    writer.append(fdrData.getNrFDRGoodTargets().toString());
                    writer.append(nl);
                    writer.append("  #decoys below " + thr + " threshold:  ");
                    writer.append(fdrData.getNrFDRGoodDecoys().toString());
                    writer.append(nl);
                    int nrPSMs = 0;
                    writer.append("  #good identifications in sets:" + nl);
                    for (Integer nrIDs : getNrIdentifications(true)) {
                        nrPSMs++;
                        writer.append("    " + nrPSMs + " - " + nrIDs + nl);
                    }
                }

                // reset the FDR data
                fdrData.setFDRThreshold(originalFDRThreshold);
                calculateCombinedFDRScore();
            }
        }

        // TODO: create histogram-data for mass-shifts at the 1%, 3% and 5% FDR value

        writer.flush();
    }


    /**
     * Processes the command line on the PSM level
     * @param model
     * @param commands
     * @return
     */
    public static boolean processCLI(PSMModeller psmModeller, PIAModeller piaModeller, String[] commands) {
        if (psmModeller == null) {
            LOGGER.error("No PSM modeller given while processing CLI commands");
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
                PSMExecuteCommands.valueOf(command).execute(psmModeller, piaModeller, params);
            } catch (IllegalArgumentException e) {
                LOGGER.error("Could not process unknown call to " + command, e);
            }
        }

        return true;
    }
}
