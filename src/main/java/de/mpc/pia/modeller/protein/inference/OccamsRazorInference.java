package de.mpc.pia.modeller.protein.inference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.Group;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;


/**
 * This inference filter reports all the PIA {@link Group}s as protein, which
 * together fulfill the Occam's Razor constraints. I.e. the minimal set of
 * groups is reported, which contain all peptides and each protein is contained,
 * at least as subset of peptides.<br/>
 *
 * There are 3 constraints for a Group, to be reported:<br/>
 * 1) it has no parent-Groups (but implicit accessions, there cannot be a Group
 * without parents and without accessions)<br/>
 * 2) Group fulfills 1) and has any direct peptides -> report it<br/>
 * 3) Group fulfills 1) but not 2): get the peptide children groups (i.e. the
 * Groups, which have the peptides). If the pepChildGroups are not fully
 * explained by any other Group fulfilling 1), report the Group. (If it is
 * explained by any other, set it as a subGroup of it).
 *
 * <p>
 *
 * TODO: to make this even faster it would be possible to thread the method by
 * the tree IDs
 *
 * @author julian
 *
 */
public class OccamsRazorInference extends AbstractProteinInference {

    /** the human readable name of this filter */
    protected static final String NAME = "Occam's Razor";

    /** the machine readable name of the filter */
    protected static final String SHORT_NAME= "inference_occams_razor";

    /** the progress of the inference */
    private Double progress;


    /** this iterator iterates over the mapping from the tree ID to its groups*/
    private Iterator<Map.Entry<Long,Map<Long,Group>>> treeGroupsIterator;

    /** this list holds the reported proteins */
    private List<ReportProtein> reportProteins;

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(OccamsRazorInference.class);


    @Override
    public List<RegisteredFilters> getAvailablePSMFilters() {
        List<RegisteredFilters> filters = new ArrayList<RegisteredFilters>();

        filters.add(RegisteredFilters.NR_PSMS_PER_PSM_SET_FILTER);
        filters.add(RegisteredFilters.PSM_UNIQUE_FILTER);
        filters.add(RegisteredFilters.PSM_ACCESSIONS_FILTER);
        filters.add(RegisteredFilters.NR_ACCESSIONS_PER_PSM_FILTER);
        filters.add(RegisteredFilters.PSM_FILE_LIST_FILTER);
        filters.add(RegisteredFilters.PSM_MODIFICATIONS_FILTER);

        filters.add(RegisteredFilters.PSM_SCORE_FILTER);

        return filters;
    }

    @Override
    public List<RegisteredFilters> getAvailablePeptideFilters() {
        List<RegisteredFilters> filters = new ArrayList<RegisteredFilters>();

        filters.add(RegisteredFilters.NR_PSMS_PER_PEPTIDE_FILTER);
        filters.add(RegisteredFilters.NR_SPECTRA_PER_PEPTIDE_FILTER);
        filters.add(RegisteredFilters.PEPTIDE_FILE_LIST_FILTER);

        filters.add(RegisteredFilters.PEPTIDE_SCORE_FILTER);

        return filters;
    }

    @Override
    public List<RegisteredFilters> getAvailableProteinFilters() {
        List<RegisteredFilters> filters = new ArrayList<RegisteredFilters>();

        filters.add(RegisteredFilters.NR_SPECTRA_PER_PROTEIN_FILTER);
        filters.add(RegisteredFilters.NR_PSMS_PER_PROTEIN_FILTER);
        filters.add(RegisteredFilters.NR_PEPTIDES_PER_PROTEIN_FILTER);
        filters.add(RegisteredFilters.PROTEIN_FILE_LIST_FILTER);
        filters.add(RegisteredFilters.NR_UNIQUE_PEPTIDES_PER_PROTEIN_FILTER);
        filters.add(RegisteredFilters.NR_GROUP_UNIQUE_PEPTIDES_PER_PROTEIN_FILTER);

        filters.add(RegisteredFilters.PROTEIN_SCORE_FILTER);

        return filters;
    }


    @Override
    public List<ReportProtein> calculateInference(Map<Long, Group> groupMap,
            Map<String, ReportPSMSet> reportPSMSetMap,
            boolean considerModifications,
            Map<String, Boolean> psmSetSettings) {
        progress = 0.0;
        LOGGER.info(NAME + " calculateInference started...");
        LOGGER.info("scoring: " + getScoring().getName() + " with " +
                getScoring().getScoreSetting().getValue() + ", " +
                getScoring().getPSMForScoringSetting().getValue() +
                "\n\tpsmSetSettings: " + psmSetSettings);

        Map<Long, Map<Long, Group>> treeGroupMap =
                new HashMap<Long, Map<Long,Group>>();
        // get the clusters/trees
        for (Map.Entry<Long, Group> groupIt : groupMap.entrySet()) {
            Map<Long, Group> treeGroups =
                    treeGroupMap.get(groupIt.getValue().getTreeID());

            if (treeGroups == null) {
                treeGroups = new HashMap<Long, Group>();
                treeGroupMap.put(groupIt.getValue().getTreeID(), treeGroups);
            }

            treeGroups.put(groupIt.getKey(), groupIt.getValue());
        }
        treeGroupsIterator = treeGroupMap.entrySet().iterator();

        LOGGER.info("PIA trees sorted, " + treeGroupMap.size() + " trees");

        // initialize the reported list
        reportProteins = new ArrayList<ReportProtein>();

        // the number of threads used for the inference
        int nrThreads = getAllowedThreads();
        if (nrThreads < 1) {
            nrThreads = Runtime.getRuntime().availableProcessors();
        }
        LOGGER.debug("used threads: " + nrThreads);

        List<OccamsRazorWorkerThread> threads =
                new ArrayList<OccamsRazorWorkerThread>(nrThreads);

        // initialize and start  the worker threads
        threads.clear();
        for (int i=0; i < nrThreads; i++) {
            OccamsRazorWorkerThread workerThread =
                    new OccamsRazorWorkerThread(i+1,
                            this,
                            filters,
                            reportPSMSetMap,
                            considerModifications,
                            psmSetSettings);
            threads.add(workerThread);
            workerThread.start();
        }

        // wait for the threads to finish
        for (OccamsRazorWorkerThread workerThread : threads) {
            try {
                workerThread.join();
            } catch (InterruptedException e) {
                LOGGER.error("thread got interrupted!", e);
            }
        }

        progress = 100.0;
        LOGGER.info(NAME + " calculateInference done, " + reportProteins.size() + " groups inferred");
        return reportProteins;
    }


    /**
     * Returns the next tree in the map or null, if no more tree is available.
     *
     * @return
     */
    public synchronized Map.Entry<Long, Map<Long, Group>> getNextTree() {
        if ((treeGroupsIterator != null)
                && treeGroupsIterator.hasNext()) {
            return treeGroupsIterator.next();
        }

        return null;
    }


    /**
     * Adds the newProteins to the list of reported {@link ReportProtein}s.
     *
     * @param newProteins
     */
    public synchronized void addToReports(List<ReportProtein> newProteins) {
        reportProteins.addAll(newProteins);
    }


    @Override
    public String getName() {
        return NAME;
    }


    @Override
    public String getShortName() {
        return SHORT_NAME;
    }


    @Override
    public Long getProgressValue() {
        return progress.longValue();
    }
}
