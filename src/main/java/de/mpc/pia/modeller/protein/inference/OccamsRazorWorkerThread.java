package de.mpc.pia.modeller.protein.inference;

import java.util.*;

import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.Group;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterFactory;



public class OccamsRazorWorkerThread extends Thread {

    /** the ID of this worker thread */
    private int ID;

    /** the caller of this thread */
    private OccamsRazorInference parent;

    /** the applied inference filters */
    private List<AbstractFilter> filters;

    /** maps of the ReportPSMSets (build by the PSM Viewer) */
    private Map<String, ReportPSMSet> reportPSMSetMap;

    /** whether modifications are considered while inferring the peptides */
    private boolean considerModifications;

    /** settings for PSMSet creation */
    private Map<String, Boolean> psmSetSettings;

    /** the inferred peptides, may contain peptide level scores and FDR values */
    private Map<String, ReportPeptide> inferredReportPeptides;

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(OccamsRazorWorkerThread.class);


    public OccamsRazorWorkerThread(int ID,
            OccamsRazorInference parent,
            List<AbstractFilter> filters,
            Map<String, ReportPSMSet> reportPSMSetMap,
            boolean considerModifications,
            Map<String, Boolean> psmSetSettings,
            Map<String, ReportPeptide> reportPeptidesMap) {
        this.ID = ID;
        this.parent = parent;
        this.filters = filters;
        this.reportPSMSetMap = reportPSMSetMap;
        this.considerModifications = considerModifications;
        this.psmSetSettings = psmSetSettings;
        this.inferredReportPeptides = reportPeptidesMap;

        this.setName("OccamsRazorWorkerThread-" + this.ID);
    }


    @Override
    public void run() {
        int treeCount = 0;
        Map.Entry<Long, Map<Long, Group>> treeEntry;

        while (null != (treeEntry = parent.getNextTree())) {
            processTree(treeEntry.getValue());
            treeCount++;
        }

        LOGGER.debug("worker " + ID + " finished after " + treeCount);
    }



    private void processTree(Map<Long, Group> groupMap) {
        // get the filtered report peptides mapping from the groups' IDs
        Map<Long, List<ReportPeptide>> reportPeptidesMap =
                parent.createFilteredReportPeptides(groupMap, reportPSMSetMap,
                        considerModifications, psmSetSettings, inferredReportPeptides);

        // the map of actually reported proteins
        Map<Long, ReportProtein> proteins =
                new HashMap<>(reportPeptidesMap.size());

        // maps from the protein/group IDs to the peptide keys
        Map<Long, Set<String>> peptideKeysMap =
                new HashMap<>();

        // maps from the groups ID to the IDs, which have the same peptides
        Map<Long, Set<Long>> sameSetMap =
                new HashMap<>(reportPeptidesMap.size());

        // create for each group, which has at least one peptide and accession, a ReportProtein
        for (Map.Entry<Long, Group> groupIt : groupMap.entrySet()) {
            if ((groupIt.getValue().getAccessions().size() == 0) ||
                    !parent.groupHasReportPeptides(
                            groupIt.getValue(), reportPeptidesMap)) {
                // this group has no peptides, skip it
                continue;
            }

            ReportProtein protein = new ReportProtein(groupIt.getKey());

            // add the accessions
            groupIt.getValue().getAccessions().values().forEach(protein::addAccession);

            // add the peptides
            Set<Long> pepGroupIDs = new HashSet<>();
            Set<String> peptideKeys = new HashSet<>();
            pepGroupIDs.add(groupIt.getKey());
            pepGroupIDs.addAll(groupIt.getValue().getAllPeptideChildren().keySet());
            pepGroupIDs.stream().filter(reportPeptidesMap::containsKey).forEach(pepGroupID -> {
                for (ReportPeptide peptide : reportPeptidesMap.get(pepGroupID)) {
                    if (!peptideKeys.add(peptide.getStringID())) {
                        LOGGER.warn("Peptide already in list of peptides '" + peptide.getStringID() + "'");
                    } else {
                        protein.addPeptide(peptide);
                    }
                }
            });

            // get the proteins with same peptides and subgroups
            Set<Long> sameSet = new HashSet<>();
            peptideKeysMap.entrySet().stream().filter(peptideKeyIt -> peptideKeyIt.getValue().equals(peptideKeys)).forEach(peptideKeyIt -> {
                sameSet.add(peptideKeyIt.getKey());
                sameSetMap.get(peptideKeyIt.getKey()).add(groupIt.getKey());
            });
            sameSetMap.put(groupIt.getKey(), sameSet);

            peptideKeysMap.put(groupIt.getKey(), peptideKeys);

            proteins.put(protein.getID(), protein);
        }

        if (proteins.size() < 1) {
            // no proteins could be created (e.g. due to filters?)
            return;
        }

        // merge proteins with same peptides
        for (Map.Entry<Long, Set<Long>> sameSetIt : sameSetMap.entrySet()) {
            Long protID = sameSetIt.getKey();
            ReportProtein protein = proteins.get(protID);
            if (protein != null) {
                // the protein is not yet deleted due to samesets
                // add the accessions of sameProtein to protein
// and remove the same-protein
// this makes sure, the protein does not get removed, when it is iterated over sameProtein
                sameSetIt.getValue().stream().filter(sameID -> !Objects.equals(sameID, protID)).forEach(sameID -> {
                    ReportProtein sameProtein = proteins.get(sameID);
                    if (sameProtein != null) {
                        // add the accessions of sameProtein to protein
                        sameProtein.getAccessions().forEach(protein::addAccession);

                        // and remove the same-protein
                        proteins.remove(sameID);
                        peptideKeysMap.remove(sameID);

                        // this makes sure, the protein does not get removed, when it is iterated over sameProtein
                        sameSetMap.get(sameID).remove(protID);
                    }
                });
            }
        }
        // the sameSetMap is no longer needed

        // check the proteins whether they satisfy the filters
        Set<Long> removeProteins = new HashSet<>(proteins.size());
        for (ReportProtein protein : proteins.values()) {
            // score the proteins before filtering
            Double protScore =
                    parent.getScoring().calculateProteinScore(protein);
            protein.setScore(protScore);

            if (!FilterFactory.satisfiesFilterList(protein, 0L, filters)) {
                removeProteins.add(protein.getID());
            }
        }
        for (Long rID : removeProteins) {
            proteins.remove(rID);
            peptideKeysMap.remove(rID);
        }

        // this will be the list of reported proteins
        List<ReportProtein> reportProteins = new ArrayList<>();

        // the still unreported proteins
        HashMap<Long, ReportProtein> unreportedProteins =
                new HashMap<>(proteins);

        // check proteins for sub-proteins and intersections. this cannot be
        // done before, because all proteins have to be built beforehand
        Map<Long, Set<Long>> subProteinMap =
                new HashMap<>(reportPeptidesMap.size());
        Map<Long, Set<Long>> intersectingProteinMap =
                new HashMap<>(reportPeptidesMap.size());
        Set<Long> isSubProtein = new HashSet<>();
        Set<String> reportedPeptides = new HashSet<>();
        for (Long proteinID : proteins.keySet()) {
            Set<String> peptideKeys = peptideKeysMap.get(proteinID);

            Set<Long> subProteins = new HashSet<>();
            subProteinMap.put(proteinID, subProteins);

            Set<Long> intersectingProteins = new HashSet<>();
            intersectingProteinMap.put(proteinID, intersectingProteins);

            for (Long subProtID : proteins.keySet()) {
                if (Objects.equals(proteinID, subProtID)) {
                    continue;
                }

                Set<String> intersection = new HashSet<>(
                        peptideKeysMap.get(subProtID));
                intersection.retainAll(peptideKeys);

                if (intersection.size() > 0) {
                    if (intersection.size() ==
                            peptideKeysMap.get(subProtID).size()) {
                        // the complete subProtID is in proteinID
                        subProteins.add(subProtID);
                    } else if (intersection.size() == peptideKeys.size()) {
                        // the complete proteinID is in subProtID
                        isSubProtein.add(proteinID);
                    } else if (intersection.size() != peptideKeys.size()) {
                        // subProtID intersects proteinID somehow
                        intersectingProteins.add(subProtID);
                    }
                }
            }

            if ((intersectingProteins.size() == 0) &&
                    !isSubProtein.contains(proteinID)) {
                // this protein is no subProtein and has no intersections (but
                // maybe subProteins) -> report this protein
                ReportProtein protein = proteins.get(proteinID);

                reportProteins.add(protein);
                reportedPeptides.addAll(peptideKeysMap.get(proteinID));
                unreportedProteins.remove(proteinID);

                // add the subproteins
                for (Long subID : subProteins) {
                    protein.addToSubsets(proteins.get(subID));
                    unreportedProteins.remove(subID);
                }
            }
        }

        // report all the proteins ordered by which explains the most new peptides
        while (unreportedProteins.size() > 0) {
            Set<Long> mostPepsIDs = null;
            Set<String> mostCanReport = null;
            int nrMostPeps = -1;

            for (ReportProtein protein : unreportedProteins.values()) {
                if (isSubProtein.contains(protein.getID())) {
                    // subproteins are reported indirectly
                    continue;
                }
                Set<String> canReport = peptideKeysMap.get(protein.getID());
                canReport.removeAll(reportedPeptides);

                if (canReport.size() > nrMostPeps) {
                    mostPepsIDs = new HashSet<>();
                    mostPepsIDs.add(protein.getID());
                    nrMostPeps = canReport.size();
                    mostCanReport = canReport;
                } else if ((canReport.size() == nrMostPeps) &&
                        canReport.equals(mostCanReport)) {
                    mostPepsIDs.add(protein.getID());
                }
            }

            for (Long protID : mostPepsIDs != null ? mostPepsIDs : null) {
                ReportProtein protein = proteins.get(protID);
                if (nrMostPeps > 0) {
                    // TODO: for now, the proteins which "explain" no more peptides are not reported (this happens sometimes)
                    reportProteins.add(protein);
                    reportedPeptides.addAll(peptideKeysMap.get(protID));
                }
                unreportedProteins.remove(protID);

                // add the subproteins
                for (Long subID : subProteinMap.get(protID)) {
                    protein.addToSubsets(proteins.get(subID));
                    unreportedProteins.remove(subID);
                }
            }
        }

        if (reportProteins.size() > 0) {
            parent.addToReports(reportProteins);
        }
    }
}
