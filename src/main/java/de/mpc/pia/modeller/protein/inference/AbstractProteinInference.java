package de.mpc.pia.modeller.protein.inference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Group;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.report.filter.impl.PeptideScoreFilter;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.tools.LabelValueContainer;


/**
 * An {@link AbstractProteinInference} calculates for the PIA groups,
 * which are to be reported and in which way.
 *
 * @author julian
 *
 */
public abstract class AbstractProteinInference {

    /** list of the settings. */
    protected List<AbstractFilter> filters;

    /** the available scores for the inference, maps from the scoreShort to the shown name */
    private Map<String, String> availableScoreShorts;

    /** the currently set scoring */
    private AbstractScoring currentScoring;

    /** the number of allowed threads (smaller 1 = all available)*/
    private int allowedThreads;

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(AbstractProteinInference.class);


    public AbstractProteinInference() {
        filters = new ArrayList<AbstractFilter>();
        currentScoring = null;
        allowedThreads = 0;
    }


    /**
     * Get the human readable name of the filter.
     * @return
     */
    public abstract String getName();


    /**
     * Get the unique machine readable short name of the filter.
     * @return
     */
    public abstract String getShortName();


    /**
     * Calculates the inference and thus create the List of
     * {@link ReportProtein}s for the given Map of PIA {@link Group}s.
     *
     * @param groupMap all groups, which should be used for the inference
     * @param reportPSMSetMap the already created PSM sets, which may contain
     * PSMSet scores
     * @param considerModifications whether to consider modifications when
     * infering peptides
     * @param psmSetSettings settings to create PSM sets
     * @param reportPeptideMap the infered peptides for the overview (may or may
     * not be used by the protein inference)
     * @return
     */
    public abstract List<ReportProtein> calculateInference(
            Map<Long, Group> groupMap,
            Map<String, ReportPSMSet> reportPSMSetMap,
            boolean considerModifications,
            Map<String, Boolean> psmSetSettings,
            Collection<ReportPeptide> reportPeptides);


    /**
     * Returns a List of allowed {@link RegisteredFilters} on PSM level for the
     * inference
     *
     * @return
     */
    public abstract List<RegisteredFilters> getAvailablePSMFilters();


    /**
     * Returns a List of allowed {@link RegisteredFilters} on peptide level for
     * the inference
     *
     * @return
     */
    public abstract List<RegisteredFilters> getAvailablePeptideFilters();


    /**
     * Returns a List of allowed {@link RegisteredFilters} on protein level for
     * the inference
     *
     * @return
     */
    public abstract List<RegisteredFilters> getAvailableProteinFilters();


    /**
     * Returns a List of all allowed {@link RegisteredFilters} for the
     * inference. This is a convenience function returning the results of
     * {@link #getAvailablePSMFilters()}, {@link #getAvailablePeptideFilters()}
     * and {@link #getAvailableProteinFilters()}.
     *
     * @return
     */
    public final List<RegisteredFilters> getAllAvailableFilters() {
        List<RegisteredFilters> availableFilters = new ArrayList<RegisteredFilters>();
        availableFilters.addAll(getAvailablePSMFilters());
        availableFilters.addAll(getAvailablePeptideFilters());
        availableFilters.addAll(getAvailableProteinFilters());

        return availableFilters;
    }


    /**
     * Returns a List of SelectItems representing the available filters for this
     * inference.
     *
     * TODO: remove this, if necessary move into the web-interface
     *
     * @return
     */
    @Deprecated
    public final List<LabelValueContainer<String>> getFilterTypes() {
        List<LabelValueContainer<String>> availableFilters = new ArrayList<LabelValueContainer<String>>();

        // PSM filters
        availableFilters.add(new LabelValueContainer<String>(null, "--- PSM ---"));

        for (RegisteredFilters filter : getAvailablePSMFilters()) {
            if (!filter.equals(RegisteredFilters.PSM_SCORE_FILTER)) {
                availableFilters.add(new LabelValueContainer<String>(
                        filter.getShortName(), filter.getFilteringListName()));
            } else {
                for (Map.Entry<String, String>  scoreIt : getAvailableScoreShorts().entrySet()) {
                    String[] filterNames = PSMScoreFilter.getShortAndFilteringName(
                            scoreIt.getKey(), scoreIt.getValue());
                    if (filterNames != null) {
                        availableFilters.add(new LabelValueContainer<String>(
                                filterNames[0], filterNames[1]));
                    }
                }
            }
        }

        // peptide filters
        availableFilters.add(new LabelValueContainer<String>(null, "--- Peptide ---"));

        for (RegisteredFilters filter : getAvailablePeptideFilters()) {
            if (!filter.equals(RegisteredFilters.PEPTIDE_SCORE_FILTER)) {
                availableFilters.add(new LabelValueContainer<String>(
                        filter.getShortName(), filter.getFilteringListName()));
            } else {
                for (Map.Entry<String, String> scoreIt : getAvailableScoreShorts().entrySet()) {
                    String[] filterNames = PeptideScoreFilter.getShortAndFilteringName(
                            scoreIt.getKey(), scoreIt.getValue());
                    if (filterNames != null) {
                        availableFilters.add(new LabelValueContainer<String>(
                                filterNames[0], filterNames[1]));
                    }
                }
            }
        }

        // protein filters
        availableFilters.add(new LabelValueContainer<String>(null, "--- Protein ---"));

        for (RegisteredFilters filter : getAvailableProteinFilters()) {
            availableFilters.add(new LabelValueContainer<String>(
                    filter.getShortName(), filter.getFilteringListName()));
        }

        return availableFilters;
    }


    /**
     * adds a new filter to the inference filters.
     * @param newFilter
     * @return
     */
    public boolean addFilter(AbstractFilter newFilter) {
        return filters.add(newFilter);
    }


    /**
     * Returns a {@link List} of all filter settings for this inference filter.
     * @return
     */
    public List<AbstractFilter> getFilters() {
        return filters;
    }


    /**
     * Removes the filter given by the index
     */
    public AbstractFilter removeFilter(int index) {
        if ((index >= 0) &&
                (index < filters.size())) {
            return filters.remove(index);
        }

        return null;
    }


    /**
     * Sets the available scores to the given Set of {@link ScoreModel}s.
     *
     * @param scores
     */
    public void setAvailableScoreShorts(Map<String, String> scores) {
        availableScoreShorts = scores;
    }


    /**
     * Getter for the available {@link ScoreModel}s.
     * @return
     */
    public Map<String, String> getAvailableScoreShorts() {
        return availableScoreShorts;
    }


    /**
     * Setter for the currently set scoring.
     * @param scoring
     */
    public void setScoring(AbstractScoring scoring) {
        this.currentScoring = scoring;
    }


    /**
     * Getter for the currently set scoring.
     * @return
     */
    public AbstractScoring getScoring() {
        return currentScoring;
    }


    /**
     * Sets the allowed number of threads, smaller 1 means all available.
     * @param threads
     */
    public void setAllowedThreads(int threads) {
        allowedThreads = threads;
    }


    /**
     * Getter for the allowed number of threads, smaller 1 means all available.
     * @param threads
     */
    public int getAllowedThreads() {
        return allowedThreads;
    }


    /**
     * This method creates a Map from the groups' IDs to the associated
     * {@link ReportPeptide}s, which can be build and satisfy the currently set
     * filters.
     *
     * @param groupMap
     * @param considerModifications
     * @return
     */
    public Map<Long, List<ReportPeptide>> createFilteredReportPeptides(Map<Long, Group> groupMap,
            Map<String, ReportPSMSet> reportPSMSetMap, boolean considerModifications,
            Map<String, Boolean> psmSetSettings, Map<String, ReportPeptide> reportPeptideMap) {
        Map<Long, List<ReportPeptide>> peptidesMap = new HashMap<Long, List<ReportPeptide>>(groupMap.size() / 2);

        for (Map.Entry<Long, Group> gIt : groupMap.entrySet()) {
            Map<String, ReportPeptide> gPepsMap = new HashMap<String, ReportPeptide>();

            if (gIt.getValue().getPeptides() == null) {
                // no peptides in the group -> go on
                continue;
            }

            for (Peptide pep :  gIt.getValue().getPeptides().values()) {

                for (PeptideSpectrumMatch psm : pep.getSpectra()) {
                    // get the ReportPSM for each PeptideSpectrumMatch

                    ReportPSMSet repSet =
                            reportPSMSetMap.get(
                                    psm.getIdentificationKey(psmSetSettings));
                    if (repSet == null) {
                        // TODO: better error
                        LOGGER.warn("no PSMSet found for " +
                                psm.getIdentificationKey(psmSetSettings) +
                                "! createFilteredReportPeptides");
                        continue;
                    }

                    ReportPSM reportPSM = null;
                    for (ReportPSM repPSM : repSet.getPSMs()) {
                        if (repPSM.getSpectrum().equals(psm)) {
                            reportPSM = repPSM;
                        }
                    }
                    if (reportPSM == null) {
                        // TODO: better error
                        LOGGER.warn("no PSM found for " +
                                psm.getIdentificationKey(psmSetSettings) +
                                "! createFilteredReportPeptides");
                        continue;
                    }

                    if (FilterFactory.satisfiesFilterList(reportPSM, 0L, filters)) {
                        // all filters on PSM level are satisfied -> use this PSM
                        String pepStringID = ReportPeptide.createStringID(reportPSM, considerModifications);

                        // get the peptide of this PSM
                        ReportPeptide peptide = gPepsMap.get(pepStringID);
                        if (peptide == null) {
                            // no peptide for the pepStringID in the map yet
                            peptide = new ReportPeptide(reportPSM.getSequence(),
                                    pepStringID, pep);
                            gPepsMap.put(pepStringID, peptide);
                        }

                        // get ReportPSMSet from the peptide
                        ReportPSMSet reportPSMSet = null;
                        List<PSMReportItem> setList = peptide.getPSMsByIdentificationKey(
                                reportPSM.getIdentificationKey(psmSetSettings),
                                psmSetSettings);

                        if (setList != null) {
                            if (setList.size() > 1) {
                                // TODO: better error
                                LOGGER.warn("more than one ReportPSMSet in setList for "
                                        + reportPSM.getSourceID() + "!");
                            }

                            for (PSMReportItem psmItem : setList) {
                                if (psmItem instanceof ReportPSMSet) {
                                    reportPSMSet = (ReportPSMSet)psmItem;
                                    break;
                                } else {
                                    // TODO: better error
                                    LOGGER.warn("psmItem is not a ReportPSMSet! ");
                                }
                            }
                        }

                        if (reportPSMSet == null) {
                            reportPSMSet = new ReportPSMSet(psmSetSettings);
                            peptide.addPSM(reportPSMSet);
                        }

                        reportPSMSet.addReportPSM(reportPSM);
                    }
                }
            }

            // in the following, peptides can become PSM-less or don't satisfy the filters, keep only these few
            List<ReportPeptide> keepPeptides = new ArrayList<>(gPepsMap.size());

            // if a psmSet has the same PSMs as the associated one in
            // reportPSMSetMap, set all the FDR variables
            for (ReportPeptide pepIt : gPepsMap.values()) {
                for (String psmKey : pepIt.getPSMsIdentificationKeys(psmSetSettings)) {

                    for (PSMReportItem psm
                            : pepIt.getPSMsByIdentificationKey(psmKey, psmSetSettings)) {
                        if (psm instanceof ReportPSMSet) {
                            ReportPSMSet checkSet = reportPSMSetMap.get(psmKey);

                            if (((ReportPSMSet) psm).getPSMs().size() == checkSet.getPSMs().size()) {
                                // same size of PSMs
                                boolean samePSMs = true;

                                for (ReportPSM p : ((ReportPSMSet) psm).getPSMs()) {
                                    boolean found = false;

                                    for (ReportPSM q : checkSet.getPSMs()) {
                                        if (p.getId() == q.getId()) {
                                            found = true;
                                            break;
                                        }
                                    }

                                    if (!found) {
                                        samePSMs = false;
                                        break;
                                    }
                                }

                                if (samePSMs) {
                                    // same PSMs in both sets -> set FDR scores and so one
                                    if (checkSet.getFDRScore() != null) {
                                        psm.setFDR(checkSet.getFDR());
                                        psm.setFDRScore(checkSet.getFDRScore().getValue());
                                        psm.setIsFDRGood(checkSet.getIsFDRGood());
                                        psm.setQValue(checkSet.getQValue());
                                        psm.setRank(checkSet.getRank());
                                    }
                                }
                            }

                            if (!FilterFactory.satisfiesFilterList(psm, 0L, filters)) {
                                // if the ReportPSMSet does not satisfy the filters, remove it
                                pepIt.removeReportPSMSet((ReportPSMSet) psm,
                                        psmSetSettings);
                            }
                        } else {
                            // TODO: better error
                            LOGGER.warn("psm is not a ReportPSMSet! " +
                                    "createFilteredReportPeptides");
                        }
                    }
                }

                if (pepIt.getNrPSMs() > 0) {
                    ReportPeptide repPeptide = checkAndGetPeptideFromMap(pepIt, reportPeptideMap);

                    if (FilterFactory.satisfiesFilterList(repPeptide, 0L, filters)) {
                        // the peptide has PSMs and satisfies the filters
                        keepPeptides.add(repPeptide);
                    }
                }
            }

            if (!keepPeptides.isEmpty()) {
                peptidesMap.put(gIt.getKey(), keepPeptides);
            }
        }

        return peptidesMap;
    }


    /**
     * Sorts the peptides from a collection into the Map needed by
     * {@link #createFilteredReportPeptides(Map, Map, boolean, Map, Map)}.
     *
     * @param reportPeptides
     * @return
     */
    public Map<String, ReportPeptide> sortPeptidesInMap(Collection<ReportPeptide> reportPeptides) {
        Map<String, ReportPeptide> peptideMap = new HashMap<>();

        for (ReportPeptide peptide : reportPeptides) {
            if (peptideMap.put(peptide.getStringID(), peptide) != null) {
                LOGGER.warn("Added a peptide with identical ID into the map: " + peptide.getStringID());
            }
        }

        return peptideMap;
    }


    /**
     * Checks, whether a peptide with the same PSMs etc. exists in the given map
     * and returns this peptide. If there is no such peptide, return the given
     * peptide.
     *
     * @param peptide
     * @param reportPeptideMap
     * @return
     */
    private static ReportPeptide checkAndGetPeptideFromMap(ReportPeptide peptide, Map<String, ReportPeptide> reportPeptideMap) {
        ReportPeptide returnPeptide = peptide;
        ReportPeptide mapPeptide = reportPeptideMap.get(peptide.getStringID());

        if (mapPeptide != null) {
            if (peptide.getPeptide().equals(mapPeptide.getPeptide())) {
                // the referenced Peptides are equal, compare the PSM sets

                Set pepSet = new HashSet<>(peptide.getPSMs());
                Set mapSet = new HashSet<>(mapPeptide.getPSMs());

                if (pepSet.equals(mapSet)) {
                    // all equal, return the mapPeptide
                    returnPeptide = mapPeptide;
                }
            }
        }

        return returnPeptide;
    }


    /**
     * Tests for the given group, if it has any direct {@link ReportPeptide}s,
     * in the given Map. This Map should by created by
     * {@link AbstractProteinInference#createFilteredReportPeptides(Map, Map, boolean)}
     * prior to calling this function.
     *
     * @param group
     * @param reportPeptidesMap
     * @return
     */
    public boolean groupHasDirectReportPeptides(Group group,
            Map<Long, List<ReportPeptide>> reportPeptidesMap) {
        List<ReportPeptide> pepList = reportPeptidesMap.get(group.getID());
        return (pepList != null) && !pepList.isEmpty();
    }


    /**
     * Tests for the given group, if it has any {@link ReportPeptide}s, whether
     * direct or in the peptideChildren, in the given Map. This Map should be
     * created by {@link AbstractProteinInference#createFilteredReportPeptides(Map, Map, boolean)}
     * prior to calling this function.
     *
     * @param group
     * @param reportPeptidesMap
     * @return
     */
    public boolean groupHasReportPeptides(Group group,
            Map<Long, List<ReportPeptide>> reportPeptidesMap) {
        // check for direct ReportPeptides
        if (groupHasDirectReportPeptides(group, reportPeptidesMap)) {
            return true;
        }

        // check for ReportPeptides of the children
        for (Group gIt : group.getAllPeptideChildren().values()) {
            if (groupHasDirectReportPeptides(gIt, reportPeptidesMap)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Creates a {@link ReportProtein} with for the given ID and puts it into
     * the proteins Map. For the {@link ReportPeptide}s of the protein, the
     * corresponding peptides in the <code>reportPeptidesMap</code> are used.
     *
     * @param id ID of the protein (group in the groupMap)
     * @param proteins the final Map for the {@link ReportProtein}s
     * @param reportPeptidesMap maps from the protein / group ID to the peptides
     * @param groupMap all the groups (the PIA intermediate structure)
     * @param sameSets maps from the ID to all groups, which are the same protein
     * @param subGroups maps from the ID to all the groups, which are subgroups
     * @return
     */
    protected ReportProtein createProtein(Long id, Map<Long, ReportProtein> proteins,
            Map<Long, List<ReportPeptide>> reportPeptidesMap,
            Map<Long, Group> groupMap, Map<Long, Set<Long>> sameSets,
            Map<Long, Set<Long>> subGroups) {
        ReportProtein protein = new ReportProtein(id);
        if (proteins.put(id, protein) != null) {
            LOGGER.warn("protein " + id + " was already in the map! "
                    + protein.getAccessions());
        }

        // add direct peptides
        if (reportPeptidesMap.containsKey(id)) {
            for (ReportPeptide pep : reportPeptidesMap.get(id)) {
                protein.addPeptide(pep);
            }
        }

        // add children's peptides
        for (Group child : groupMap.get(id).getAllPeptideChildren().values()) {
            if (reportPeptidesMap.containsKey(child.getID())) {
                for (ReportPeptide pep : reportPeptidesMap.get(child.getID())) {
                    protein.addPeptide(pep);
                }
            }
        }

        // add the direct accessions
        for (Accession acc : groupMap.get(id).getAccessions().values()) {
            protein.addAccession(acc);
        }

        // add all the accessions from the sameSets
        if ((sameSets != null) &&  sameSets.containsKey(id)) {
            for (Long sameID : sameSets.get(id)) {
                if (!sameID.equals(id)) {
                    for (Accession acc : groupMap.get(sameID).getAccessions().values()) {
                        protein.addAccession(acc);
                    }
                }
            }
        }

        // add the subProteins
        if ((subGroups != null) && subGroups.containsKey(id)) {
            for (Long subID : subGroups.get(id)) {
                ReportProtein subProtein = proteins.get(subID);
                if (subProtein == null) {
                    subProtein = createProtein(subID, proteins, reportPeptidesMap,
                            groupMap, sameSets, subGroups);
                    proteins.put(subID, subProtein);
                }

                protein.addToSubsets(subProtein);
            }
        }

        if (currentScoring != null) {
            protein.setScore(currentScoring.calculateProteinScore(protein));
        }

        return protein;
    }


    /**
     * If polling of inference is enabled, return the current state of the
     * progress (between 0 and 100 percent).
     * @return
     */
    public Long getProgressValue() {
        return Long.valueOf(101);
    }
}
