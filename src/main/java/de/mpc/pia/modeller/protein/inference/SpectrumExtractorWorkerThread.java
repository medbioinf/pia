package de.mpc.pia.modeller.protein.inference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.ReportProteinComparatorFactory;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;



public class SpectrumExtractorWorkerThread extends Thread {

    /** the ID of this worker thread */
    private int id;

    /** the caller of this thread */
    private SpectrumExtractorInference parent;

    /** the used scoring */
    private AbstractScoring scoring;

    /** the applied inference filters */
    private List<AbstractFilter> filters;

    /** maps from groupID/proteinID to the peptides */
    private Map<Long, Set<Peptide>> groupsPeptides;

    /** maps of the ReportPSMSets (build by the PSM Viewer) */
    private Map<String, ReportPSMSet> reportPSMSetMap;

    /** map them from the PSM ID to the reportPSMs */
    private Map<Long, ReportPSM> reportPSMMap;

    /** maps from the peptideKey to the reportPSMSets used by the reportPeptide */
    private Map<String, Set<ReportPSMSet>> peptidesSpectra;

    /** list of the spectrumIdentificationKeys of the already used spectra (this map gets filled while reporting proteins) */
    private Set<String> usedSpectra;

    /** shortName of the used protein scoring */
    private String scoreShort;

    /** whether modifications are considered while inferring the peptides */
    private boolean considerModifications;

    /** settings for PSMSet creation */
    private Map<String, Boolean> psmSetSettings;


    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(SpectrumExtractorWorkerThread.class);


    /**
     * Initializes the worker thread.
     *
     * @param id the ID of the thread
     * @param parent the caller of this thread
     * @param scoring the applied scoring
     * @param filters the applied inference filters
     * @param groupsPeptides maps from groupID/proteinID to the peptides
     * @param reportPSMMap map them from the PSM ID to the reportPSMs
     * @param peptidesSpectra maps from the peptideKey to the reportPSMSets used
     * by the reportPeptide
     * @param usedSpectra list of the spectrumIdentificationKeys of the already
     * used spectra (this map gets filled while reporting proteins)
     * @param scoreShort shortName of the used protein scoring
     * @param considerModifications whether modifications are considered while
     * inferring the peptides
     * @param psmSetSettings settings for PSMSet creation
     */
    public SpectrumExtractorWorkerThread(int id,
            SpectrumExtractorInference parent,
            AbstractScoring scoring,
            List<AbstractFilter> filters,
            Map<Long, Set<Peptide>> groupsPeptides,
            Map<String, ReportPSMSet> reportPSMSetMap,
            Map<Long, ReportPSM> reportPSMMap,
            Map<String, Set<ReportPSMSet>> peptidesSpectra,
            Set<String> usedSpectra,
            String scoreShort,
            boolean considerModifications,
            Map<String, Boolean> psmSetSettings) {
        this.id = id;
        this.parent = parent;
        this.scoring = scoring;
        this.filters = filters;
        this.groupsPeptides = groupsPeptides;
        this.reportPSMSetMap = reportPSMSetMap;
        this.reportPSMMap = reportPSMMap;
        this.peptidesSpectra = peptidesSpectra;
        this.usedSpectra = usedSpectra;
        this.scoreShort = scoreShort;
        this.considerModifications = considerModifications;
        this.psmSetSettings = psmSetSettings;

        this.setName("ProteinExtractorWorker-" + this.id);
    }


    @Override
    public void run() {
        ReportProtein protein;

        // get the next available protein from the parent
        protein = parent.getNextProteinForRebuild();
        while (protein != null) {
            // rebuild the protein and get the next
            rebuildProtein(protein);
            protein = parent.getNextProteinForRebuild();
        }
    }


    /**
     * @param protein the protein, which will be rebuild and rescored
     */
    private void rebuildProtein(ReportProtein protein) {
        // first, clear all the peptides from the protein
        protein.clearPeptides();

        // maps from the peptideKeys to the reportPeptides of the protein
        Map<String, ReportPeptide> peptideMap =
                new HashMap<>();

        // the peptides in this list where blacklisted by filter settings
        Set<String> peptideBlacklist = new HashSet<>();
        boolean iterate = true;

        ScoreModelEnum scoreModel =
                ScoreModelEnum.getModelByDescription(
                        scoring.getScoreSetting().getValue());
        if (scoreModel == null) {
            LOGGER.error("no scoring given");
            return;
        }

        while (iterate) {
            // clear the peptide map from any prior runs
            peptideMap.clear();

            // sort the PSMs or PSMSets (if COMBINED_FDR_SCORE is used) into the peptides
            // all possible peptides with their PSMs are given for the protein
            for (Peptide peptide : groupsPeptides.get(protein.getID())) {
                // holds the getPeptideStringIDs, which are done
                Set<String> modificationsDone = new HashSet<>();

                for (PeptideSpectrumMatch psm : peptide.getSpectra()) {
                    String peptideKey = psm.getPeptideStringID(considerModifications);
                    // check, whether the peptide is on the blacklist
                    if (!peptideBlacklist.contains(peptideKey)) {
                        // check, if this PSM is already used
                        if (peptidesSpectra.containsKey(peptideKey)) {
                            // the peptide is already set with some PSMSets, so take
                            // these fixed PSMSets and this peptide (with modifications)
                            // is done
                            if (!modificationsDone.contains(peptideKey)) {
                                ReportPeptide reportPeptide = new ReportPeptide(psm.getSequence(), peptideKey, peptide);

                                if (peptideMap.put(peptideKey, reportPeptide) != null) {
                                    LOGGER.error("There was already another peptide for " +
                                                    peptideKey + " in the peptideMap!");
                                }

                                peptidesSpectra.get(peptideKey).forEach(reportPeptide::addPSM);

                                // this peptide is done
                                modificationsDone.add(peptideKey);
                            }
                        } else {
                            // sort the PSM into the peptide
                            if (scoreModel.isPSMSetScore()) {
                                sortPSMSetIntoReportPeptide(psm, peptideKey, peptideMap);
                            } else {
                                sortPSMIntoReportPeptide(psm, peptideKey, peptideMap);
                            }
                        }
                    }
                }
            }

            if (peptideMap.size() < 1) {
                // no peptides in the protein, done
                protein.setScore(Double.NaN);
                return;
            }

            // maps from the spectrumIdentificationKeys to
            //   [0] the keys of the peptides it occurs in (this may well be only one peptide)
            //   [1] the best score (as ScoreModel)
            //   [2] the key of (one of) the peptide(s) with the highest score
            Map<String, Object[]> spectraToPeptides =
                    new HashMap<>();

                    Iterator<ReportPeptide> pepIt = peptideMap.values().iterator();
                    while (pepIt.hasNext()) {
                        ReportPeptide peptide = pepIt.next();

                        if (!scoreModel.isPSMSetScore()) {
                            // if not a PSM set score is used, get additional information for the
                            // PSMSets now and filter the sets

                            // try to get FDRScore values (if they are not yet set)
// get the ReportPSMSet, which was build by the PSM Viewer
// add the used spectra to the set
// FDR is calculated, so get the scores for it
// remove the PSMSet from the peptide
// psmSet can only be ReportPSMSet
                            peptide.getPSMs().stream().filter(psmSet -> psmSet instanceof ReportPSMSet).forEach(psmSet -> {

                                // try to get FDRScore values (if they are not yet set)
                                if (psmSet.getFDRScore() == null) {
                                    // get the ReportPSMSet, which was build by the PSM Viewer
                                    String key = psmSet.getIdentificationKey(psmSetSettings);
                                    ReportPSMSet givenSet = reportPSMSetMap.get(key);
                                    if ((givenSet != null) &&
                                            (givenSet.getFDRScore() != null)) {
                                        // add the used spectra to the set
                                        Set<Long> psmIDs = new HashSet<>(((ReportPSMSet) psmSet)
                                                .getPSMs().stream()
                                                .map(psm -> psm.getSpectrum().getID())
                                                .collect(Collectors.toList()));

                                        // FDR is calculated, so get the scores for it
                                        Set<Long> givenPSMids = givenSet.getPSMs().stream().map(psm -> psm.getSpectrum().getID()).collect(Collectors.toSet());

                                        if (psmIDs.equals(givenPSMids)) {
                                            psmSet.setFDRScore(givenSet.getFDRScore().getValue());
                                            psmSet.setFDR(givenSet.getFDR());
                                        }
                                    }
                                }

                                if (!FilterFactory.satisfiesFilterList(
                                        psmSet, 0L, filters)) {
                                    // remove the PSMSet from the peptide
                                    peptide.removeReportPSMSet((ReportPSMSet) psmSet,
                                            psmSetSettings);
                                }
                            } );
                        }

                        // if the peptide became empty, remove it
                        if (peptide.getNrPSMs() < 1) {
                            pepIt.remove();
                        } else {
                            // cache the spectra (for the check of a spectrum existing
                            // in multiply peptides)
                            for (PSMReportItem psmSet : peptide.getPSMs()) {
                                if (psmSet instanceof ReportPSMSet) {
                                    String peptideKey =
                                            psmSet.getPeptideStringID(considerModifications);
                                    ScoreModel score = null;

                                    // if a PSM set score is used, then the score is the
                                    // same for the whole set
                                    if (scoreModel.isPSMSetScore()) {
                                        score = psmSet.getCompareScore(scoreShort);
                                    }

                                    for (ReportPSM repPSM : ((ReportPSMSet) psmSet).getPSMs()) {
                                        String specIDKey =
                                                repPSM.getSpectrum().getSpectrumIdentificationKey(psmSetSettings);

                                        if (!scoreModel.isPSMSetScore()) {
                                            // get the score, if not a PSM set scoring
                                            score = repPSM.getCompareScore(scoreShort);
                                        }

                                        Object[] pepData =
                                                spectraToPeptides.get(specIDKey);
                                        if (pepData == null) {
                                            pepData = new Object[3];
                                            // the peptide IDs
                                            pepData[0] = new HashSet<String>();
                                            // the best score
                                            pepData[1] = score;
                                            // (one of the) best peptides
                                            pepData[2] = peptideKey;

                                            spectraToPeptides.put(specIDKey, pepData);
                                        }

                                        @SuppressWarnings("unchecked")
                                        Set<String> set = (Set<String>)pepData[0];
                                        set.add(peptideKey);

                                        if ((score != null) &&
                                                (score.compareTo((ScoreModel)pepData[1]) < 0)) {
                                            // this score is better than the best score until now
                                            pepData[1] = score;
                                            pepData[2] = peptideKey;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // the set of spectra with the corresponding peptides
                    // Object[] = [Set<spectraIDs>, Set<peptideKeys>]
                    Set<Object[]> spectraAndPeptides = new HashSet<>();

                    // check, if a spectrum is in multiple peptides of the protein and
                    // remove any no longer questionable spectra from the map
                    for (Iterator<Map.Entry<String, Object[]>> spIt = spectraToPeptides.entrySet().iterator();
                            spIt.hasNext(); ) {
                        Map.Entry<String, Object[]> spEntry = spIt.next();

                        String specIDKey = spEntry.getKey();
                        @SuppressWarnings("unchecked")
                        Set<String> peptideIDs = (Set<String>)spEntry.getValue()[0];

                        if (peptideIDs.size() > 1) {
                            // the spectrum is in more than one peptide
                            // remove from the peptides, where it has not the best score

                            // the best scoreModel
                            ScoreModel score = (ScoreModel)spEntry.getValue()[1];

                            // the peptides in this set can be removed from the peptideIDs,
                            // as the spectrum does no longer score in them
                            Set<String> peptidesToRemove = new HashSet<>();

                            if ((score != null) &&
                                    !score.getValue().equals(Double.NaN)) {
                                // there is a valid "best score" -> erase the reportPSMs,
                                // which have no best score from their peptides

                                for (String peptideKey : peptideIDs) {
                                    ReportPeptide peptide = peptideMap.get(peptideKey);

                                    for (PSMReportItem repPSM
                                            : peptide.getPSMsBySpectrumIdentificationKey(specIDKey)) {
                                        if (repPSM instanceof ReportPSMSet) {
                                            if (score.compareTo(repPSM.getCompareScore(scoreShort)) != 0) {
                                                // this is not the best score -> PSMs in set should not score in peptide
                                                for (ReportPSM psm
                                                        : ((ReportPSMSet) repPSM).getPSMs()) {
                                                    peptide.addToNonScoringPSMs(psm.getId());
                                                }
                                                peptidesToRemove.add(peptideKey);
                                            }
                                        } else {
                                            LOGGER.error("Not a PSM set in peptide! " +
                                                    repPSM.getSourceID());
                                        }
                                    }
                                }
                            }

                            // remove the no longer scoring peptides
                            peptideIDs.removeAll(peptidesToRemove);
                        }

                        if (peptideIDs.size() == 1) {
                            // this spectrum has only one peptide (now) -> remove any not or
                            // no longer questionable spectra
                            spIt.remove();
                        } else {
                            // the spectrum is still in multiple peptides, sort it into
                            // the disjoint sets
                            sortInSpectrumAndPeptides(specIDKey, peptideIDs,
                                    spectraAndPeptides);
                        }
                    }

                    // go through the disjoint sets of spectra and corresponding peptides
                    for (Object[] tuple : spectraAndPeptides) {
                        if (tuple[0] == null) {
                            // this tuple is removed (while sorting)
                            continue;
                        }

                        // the questionable spectra
                        @SuppressWarnings("unchecked")
                        Set<String> spectra = (Set<String>) tuple[0];
                        // the peptides of these spectra
                        @SuppressWarnings("unchecked")
                        Set<String> peptides = (Set<String>) tuple[1];

                        // get also the other, fixed scoring spectra for the peptides
                        Set<String> allSpectra = new HashSet<>(spectra);
                        for (String peptideKey : peptides) {
                            ReportPeptide peptide = peptideMap.get(peptideKey);

                            peptide.getPSMs().stream().filter(repPSM -> repPSM instanceof ReportPSMSet).forEach(repPSM -> allSpectra.addAll(((ReportPSMSet) repPSM).getPSMs().stream().filter(psm -> !peptide.getNonScoringPSMIDs().contains(
                                    psm.getId())).map(psm -> psm.getSpectrum().
                                    getSpectrumIdentificationKey(
                                            psmSetSettings)).collect(Collectors.toList())));
                        }

                        // check for all peptides, if they have all spectra
                        Set<String> peptidesWithAllSpectra = new HashSet<>();
                        for (String peptideKey : peptides) {
                            ReportPeptide peptide = peptideMap.get(peptideKey);

                            if (peptide.getScoringSpectraIdentificationKeys().
                                    containsAll(allSpectra)) {
                                // the peptide has all the relevant spectra
                                peptidesWithAllSpectra.add(peptideKey);
                            }
                        }

                        if (!peptidesWithAllSpectra.isEmpty()) {
                            // 1) there are peptides, which have all the spectra
                            //    -> one of them gets scored
                            scoreOnePeptide(peptidesWithAllSpectra.iterator().next(),
                                    peptides, spectra, peptideMap);
                        } else {
                            // 2) complicated case

                            // get all the peptides and make a fake-protein for each one.
                            // these proteins are needed, as there is no such thing as a
                            // peptide score. these peptides should for now only use
                            // spectra NOT in the questionable set
                            List<ReportProtein> reportProteins =
                                    new ArrayList<>(peptides.size());
                            Long fakeId = 1L;
                            for (String peptideKey : peptides) {
                                ReportProtein fakeProtein = new ReportProtein(fakeId);
                                protein.getAccessions().forEach(fakeProtein::addAccession);
                                ReportPeptide fakePeptide = peptideMap.get(peptideKey);
                                spectra.forEach(fakePeptide::addToNonScoringSpectra);
                                fakeProtein.addPeptide(fakePeptide);
                                fakeProtein.setScore(
                                        scoring.calculateProteinScore(fakeProtein));
                                reportProteins.add(fakeProtein);
                                fakeId++;
                            }

                            Set<String> alreadyScoringSpectra =
                                    new HashSet<>(spectra.size());

                            Comparator<ReportProtein> comparator =
                                    ReportProteinComparatorFactory.CompareType.SCORE_SORT.getNewInstance();
                            reportProteins.sort(comparator);

                            Set<ReportPeptide> sameScorePeptides =
                                    new HashSet<>();
                            Double sameScore = reportProteins.iterator().next().getScore();

                            while (!reportProteins.isEmpty()) {
                                // stores all the combinations of spectra for this score's
                                // peptides
                                Map<String, List<String>> peptidesScoringSpectraKeys =
                                        new HashMap<>();

                                // get all peptides / fake proteins with the same score
                                Iterator<ReportProtein> proteinIt =
                                        reportProteins.listIterator();
                                while (proteinIt.hasNext()) {
                                    ReportProtein fakeProtein = proteinIt.next();

                                    if (!sameScore.equals(fakeProtein.getScore())) {
                                        break;
                                    } else {
                                        ReportPeptide fakePeptide =
                                                fakeProtein.getPeptides().iterator().next();
                                        sameScorePeptides.add(fakePeptide);

                                        peptidesScoringSpectraKeys.put(
                                                fakePeptide.getStringID(),
                                                fakePeptide.getScoringSpectraIdentificationKeys());

                                        proteinIt.remove();
                                    }
                                }

                                // all peptides with the score are assembled
                                Set<String> newScoringSpectra =
                                        new HashSet<>(spectra.size());
                                Set<List<String>> sameScoringSpectraKeys =
                                        new HashSet<>();
                                for (ReportPeptide peptide : sameScorePeptides) {
                                    boolean scoreNone = false;
                                    List<String> scoringSpectraKeys =
                                            peptide.getScoringSpectraIdentificationKeys();

                                    // if this peptide's spectra are a (sub)-set of another
                                    // peptide's spectra with this score, this peptide gets
                                    // not scored
                                    for (Map.Entry<String, List<String>> spectraKeyIt
                                            : peptidesScoringSpectraKeys.entrySet()) {

                                        if (!spectraKeyIt.getKey().equals(peptide.getStringID()) &&
                                                (spectraKeyIt.getValue().size() < scoringSpectraKeys.size()) &&
                                                spectraKeyIt.getValue().containsAll(scoringSpectraKeys)) { // it must be a sub-set
                                            scoreNone = true;
                                            break;
                                        }
                                    }

                                    if (!sameScoringSpectraKeys.add(scoringSpectraKeys)) {
                                        // there was already another peptide with same score
                                        // and scoring spectra -> don't score this one
                                        scoreNone = true;
                                    }

                                    // go through the PSMs and set their scoring flags
                                    for (PSMReportItem psmItem : peptide.getPSMs()) {
                                        if (psmItem instanceof ReportPSMSet) {
                                            // this is used for PSM set scoring, to
                                            // invalidate a PSM set
                                            boolean removePSMSet = false;

                                            for (ReportPSM psm : ((ReportPSMSet) psmItem).getPSMs()) {
                                                Long psmID = psm.getId();
                                                String specIDKey = psm.getSpectrum().
                                                        getSpectrumIdentificationKey(
                                                                psmSetSettings);

                                                if (spectra.contains(specIDKey)) {
                                                    // it is a questionable spectrum
                                                    if (scoreNone ||
                                                            alreadyScoringSpectra.contains(specIDKey)) {
                                                        // it is already scoring, remove here
                                                        peptide.addToNonScoringPSMs(psmID);
                                                        if (scoreModel.isPSMSetScore()) {
                                                            // invalidate the whole set,
                                                            // if set scoring is active
                                                            removePSMSet = true;
                                                        }
                                                    } else {
                                                        // it may be used here, but removed in the next score set
                                                        newScoringSpectra.add(specIDKey);
                                                    }
                                                }
                                            }

                                            if (removePSMSet) {
                                                for (ReportPSM psm
                                                        : ((ReportPSMSet) psmItem).getPSMs()) {
                                                    peptide.addToNonScoringPSMs(
                                                            psm.getId());
                                                }
                                            }
                                        }
                                    }

                                    // remove the non-scoring flag from the spectra, as they
                                    // were only used for this scoring here
                                    peptide.clearNonScoringSpectraIDKeys();
                                }

                                alreadyScoringSpectra.addAll(newScoringSpectra);

                                // re-score and order the fake proteins
                                if (!reportProteins.isEmpty()) {
                                    scoring.calculateProteinScores(reportProteins);
                                    reportProteins.sort(comparator);
                                    sameScore =
                                            reportProteins.iterator().next().getScore();
                                    sameScorePeptides.clear();
                                }
                            }
                        }
                    }

                    iterate = false;
                    // check, if a peptide is not valid after filtering
                    for (ReportPeptide peptide : peptideMap.values()) {
                        if (!FilterFactory.
                                satisfiesFilterList(peptide, 0L, filters)) {
                            peptideBlacklist.add(peptide.getStringID());
                            iterate = true;
                        }
                    }
        }

        if (peptideMap.size() > 0) {
            // now add the (remaining) peptides from peptideMap to the protein, if they satisfy the filters
            peptideMap.values().stream().filter(peptide -> FilterFactory.
                    satisfiesFilterList(peptide, 0L, filters)).forEach(protein::addPeptide);

            // and finally score the protein
            protein.setScore(scoring.calculateProteinScore(protein));
        } else {
            // no more valid peptides in the protein
            protein.setScore(Double.NaN);
        }
    }


    /**
     * Sorts the given {@link PeptideSpectrumMatch} into the Map of
     * {@link ReportPeptide}s. If there is no peptide for the PSM in the Map
     * yet, the peptide will be generated.
     *
     * @param psm the {@link PeptideSpectrumMatch}, which should be sorted into
     * the peptide
     * @param peptideMap the Map of {@link ReportPeptide}s, in which the
     * {@link PeptideSpectrumMatch} should be sorted
     */
    private void sortPSMIntoReportPeptide(PeptideSpectrumMatch psm,
            String peptideKey, Map<String, ReportPeptide> peptideMap) {
        // get the reportPSM, which contains the PSM (it is still in the map,
        // if it passed the filtering
        ReportPSM reportPSM = reportPSMMap.get(psm.getID());

        if (reportPSM != null) {
            // this PSM satisfied the filters, because it is still in the map
            String specIDKey =
                    psm.getSpectrumIdentificationKey(psmSetSettings);

            if (!usedSpectra.contains(specIDKey)) {
                // this spectrum can still be used for this peptide
                ReportPeptide reportPeptide =
                        peptideMap.computeIfAbsent(peptideKey, k -> new ReportPeptide(psm.getSequence(),
                                peptideKey, psm.getPeptide()));

                // get the PSMSet for this reportPSM
                ReportPSMSet psmSet = null;
                String psmKey = reportPSM.getIdentificationKey(psmSetSettings);

                // as the peptide should have PSMSets (no PSMs), there should be
                // only one set for the psmKey
                List<PSMReportItem> reportPSMSets =
                        reportPeptide.getPSMsByIdentificationKey(psmKey,
                                psmSetSettings);

                if (!reportPSMSets.isEmpty()) {
                    PSMReportItem psmItem = reportPSMSets.get(0);
                    if (psmItem instanceof ReportPSMSet) {
                        psmSet = (ReportPSMSet)psmItem;
                    } else {
                        LOGGER.error("Not a ReportPSMSet-instance in peptide for "
                                + psmKey + "!");
                    }
                    if (reportPSMSets.size() > 1) {
                        LOGGER.error("More than one PSMReportItem in peptide for "
                                + psmKey + "!");
                    }
                } else {
                    // no set yet, create this
                    psmSet = new ReportPSMSet(psmSetSettings);

                    // add the psmSet to the peptide
                    reportPeptide.addPSM(psmSet);
                }

                // add this reportPSM to the set
                if (psmSet != null) {
                    psmSet.addReportPSM(reportPSM);
                } else {
                    LOGGER.error("Error while sorting in PSM, psm is NULL!");
                }
            }
        }
    }


    /**
     * Sorts the given {@link PeptideSpectrumMatch} into the Map of
     * {@link ReportPeptide}s. If there is no peptide for the PSM in the Map
     * yet, the peptide will be generated.
     *
     * @param psm the {@link PeptideSpectrumMatch}, which should be sorted into
     * the peptide
     * @param peptideMap the Map of {@link ReportPeptide}s, in which the
     * {@link PeptideSpectrumMatch} should be sorted
     */
    private void sortPSMSetIntoReportPeptide(PeptideSpectrumMatch psm,
            String peptideKey, Map<String, ReportPeptide> peptideMap) {
        // get the reportPSMSet, which contains the PSM
        ReportPSMSet reportPSMSet = reportPSMSetMap.get(
                psm.getIdentificationKey(psmSetSettings));

        if ((reportPSMSet != null) &&
                (FilterFactory.satisfiesFilterList(
                        reportPSMSet, 0L, filters))) {
            // the reportPSMSet passes the filters
            String specIDKey =
                    psm.getSpectrumIdentificationKey(psmSetSettings);

            if (!usedSpectra.contains(specIDKey)) {
                // this spectrum can still be used for this peptide
                ReportPeptide reportPeptide =
                        peptideMap.computeIfAbsent(peptideKey, k -> new ReportPeptide(psm.getSequence(),
                                peptideKey, psm.getPeptide()));

                if (!reportPeptide.getSpectraIdentificationKeys().contains(
                        specIDKey)) {
                    // the needed PSMSet is not yet in the peptide
                    // it could be alredy in there, if e.g. multiple searches are combined
                    reportPeptide.addPSM(reportPSMSet);
                }
            }
        }
    }


    /**
     * Sorts the given spectrumIdentificationKey and the corresponding peptide
     * keys into the tuple in the spectraAndPeptides.
     *
     * @param specIDKey
     * @param peptideIDs
     * @param spectraAndPeptides
     */
    private void sortInSpectrumAndPeptides(String specIDKey,
            Set<String> peptideIDs, Set<Object[]> spectraAndPeptides ) {
        // the set of spectra with the corresponding peptides
        //   [0] Set<spectraIDs>    (is null, if no longer needed/merged)
        //   [1] Set<peptideKeys>   (is null, if no longer needed/merged)
        Object[] spectrumTuple = null;

        // go through the peptideIDs
        for (String peptideKey : peptideIDs) {
            boolean peptideSortedIn = false;

            // look for the peptide in the spectrumTuple
            if (spectrumTuple != null) {
                @SuppressWarnings("unchecked")
                Set<String> peptides = (Set<String>)spectrumTuple[1];
                if (peptides.contains(peptideKey)) {
                    // spectrum and peptide are in here already
                    peptideSortedIn = true;
                }
            }

            if (!peptideSortedIn) {
                // look, if the peptide is already in another tuple
                for (Object[] tuple : spectraAndPeptides) {
                    if (tuple[0] != null) {
                        @SuppressWarnings("unchecked")
                        Set<String> peptides = (Set<String>) tuple[1];

                        if (peptides.contains(peptideKey)) {
                            if (spectrumTuple == null) {
                                // nothing found for the spectrum before
                                //   -> sort it in here
                                @SuppressWarnings("unchecked")
                                Set<String> spectra = (Set<String>)tuple[0];
                                spectra.add(specIDKey);
                                spectrumTuple = tuple;
                            } else {
                                // the spectrum had a tuple before
                                // -> merge the tuples
                                @SuppressWarnings("unchecked")
                                Set<String> oldSpectra =
                                (Set<String>)spectrumTuple[0];
                                @SuppressWarnings("unchecked")
                                Set<String> oldPeptides =
                                (Set<String>)spectrumTuple[1];
                                @SuppressWarnings("unchecked")
                                Set<String> spectra =
                                (Set<String>)tuple[0];

                                // copy everything into the found tuple
                                spectra.addAll(oldSpectra);
                                peptides.addAll(oldPeptides);

                                // set the old tuple to null values
                                spectrumTuple[0] = null;
                                spectrumTuple[1] = null;

                                // this is our tuple now
                                spectrumTuple = tuple;
                            }

                            peptideSortedIn = true;
                            break;	// peptide may be only in one tuple, done
                        }
                    }
                }
            }

            if (!peptideSortedIn) {
                // no tuple found for the peptide
                if (spectrumTuple != null) {
                    // there is already a tuple for the spectrum
                    // -> sort the peptide in here
                    @SuppressWarnings("unchecked")
                    Set<String> peptides =
                    (Set<String>)spectrumTuple[1];
                    peptides.add(peptideKey);
                } else {
                    // generate a new tuple for the spectrum
                    spectrumTuple = new Object[2];

                    Set<String> spectra = new HashSet<>();
                    spectra.add(specIDKey);
                    spectrumTuple[0] = spectra;

                    Set<String> peptides = new HashSet<>();
                    peptides.add(peptideKey);
                    spectrumTuple[1] = peptides;

                    spectraAndPeptides.add(spectrumTuple);
                }
            }
        }
    }


    /**
     * Sets all the questionableSpectra in the peptides given by the strings in
     * allPeptides and the peptideMap to nonScoring, except for the spectra in
     * the peptide given by scoringPeptideKey.
     *
     * @param scoringPeptideKey
     * @param allPeptides
     * @param questionableSpectra
     * @param peptideMap
     */
    private void scoreOnePeptide(String scoringPeptideKey,
            Set<String> allPeptides, Set<String> questionableSpectra,
            Map<String, ReportPeptide> peptideMap) {

        // remove the questionable spectra from scoring
        allPeptides.stream().filter(peptideKey -> !peptideKey.equals(scoringPeptideKey)).forEach(peptideKey -> {
            // remove the questionable spectra from scoring
            ReportPeptide peptide = peptideMap.get(peptideKey);

            peptide.getPSMs().stream().filter(reportPSM -> reportPSM instanceof ReportPSMSet).forEach(reportPSM -> ((ReportPSMSet) reportPSM).getPSMs().stream().filter(psm -> questionableSpectra.contains(
                    psm.getSpectrum().
                            getSpectrumIdentificationKey(
                                    psmSetSettings))).forEach(psm -> peptide.addToNonScoringPSMs(psm.getId())));
        });
    }
}
