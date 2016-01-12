package de.mpc.pia.modeller.protein.inference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Group;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.ReportProteinComparatorFactory;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.report.filter.impl.PeptideScoreFilter;
import de.mpc.pia.tools.LabelValueContainer;


/**
 * This is a spectrum-centric, respectively PSM set-centric, inference.
 *
 * <ol>
 *   <li>
 *     a list of possible proteins for all groups with accessions gets created
 *   </li>
 *   <li>
 *     for each ReportProtein in possible protein list build the protein:
 *     <ol type="a">
 *       <li>
 *         for each peptide: if peptide is already reported, take the peptide,
 *         else create peptide with possible PSMs.<br/>
 *         a PSM is ok to be used, if it was not in an already reported
 *         ReportProtein's peptides<br/>
 *         if a PSM set score (like CombinedFDRScore) is used, the smallest
 *         entity is the PSM set, so these get sorted into the peptides.
 *       </li>
 *       <li>
 *         for PSMs (PSM score used for scoring):<br>
 *         if a spectrum is in more than one peptide of a protein, score it only
 *         in the peptide, where it has the best score<br/>
 *
 *         if there is more than one peptide, where the spectrum has the same
 *         best score, do the following:<br/>
 *
 *         get all spectra for the affected peptides.
 *         <ol type="i">
 *           <li>
 *             if there are peptides, which have all of the affected spectra,
 *             one of these peptides gets scored by the spectra, all other
 *             peptides are not scored.
 *           </li>
 *           <li>
 *             if not, score the peptides with the not-questionable spectra.
 *             the highest scoring peptide gets all its spectra. if there are
 *             peptides with same score and spectra, only one gets scored. then
 *             score again with remaining spectra, until all spectra are given.
 *           </li>
 *         </ol>
 *         for PSM sets (if e.g. CombinedFDRScore is used): use the PSM sets
 *         instead of spectra and do the same as before mentioned.
 *       </li>
 *     </ol>
 *   </li>
 *   <li>
 *     take the ReportProtein(s) with the highest score
 *     <ol type="a">
 *       <li>
 *         check, if it is a sub-protein (either regarding peptides or PSMs),
 *         sub-protein has to be completely in another protein. if it is in
 *         multiple proteins, the protein is not reported.
 *       </li>
 *       <li>
 *         if it is no sub-protein and has new peptides to report, report it
 *       </li>
 *       <li>
 *         remove the highest scoring protein from list of possible proteins
 *       </li>
 *     </ol>
 *   </li>
 *   <li>
 *     repeat 2 and 3 until list of possible proteins is empty.
 *   </li>
 * </ol>
 *
 *
 * TODO: show, which PSMs are scoring and link a not scoring to it's reason
 * somehow...
 *
 *
 * @author julian
 *
 */
public class SpectrumExtractorInference extends AbstractProteinInference {

    /** the human readable name of this filter */
    protected static final String name = "Spectrum Extractor";

    /** the machine readable name of the filter */
    protected static final String shortName = "inference_spectrum_extractor";

    /** the list iterator used to give the reportProteins to the working threads */
    private ListIterator<ReportProtein> proteinListIt;

    /** the number of all spectra */
    private int nrSpectra;

    /** the currently processed spectra */
    private int nrUsedSpectra;

    /** the number of disjoint splits */
    private int nrSplits;

    /** the number of finished splits */
    private int nrFinishedSplits;


    /** the accessions' IDs, which may have changed by the spectra used during the last iteration */
    private Set<Long> changedAccessions;

    /** to return 101, when everything is done */
    private boolean inferenceDone;

    /** the logger for this class */
    private static final Logger logger= Logger.getLogger(SpectrumExtractorInference.class);



    public SpectrumExtractorInference() {
        super();
        this.nrSpectra = 0;
        this.nrUsedSpectra = 0;
        this.inferenceDone = false;
    }


    @Override
    public List<LabelValueContainer<String>> getFilterTypes() {
        List<LabelValueContainer<String>> filters = new ArrayList<LabelValueContainer<String>>();

        // PSM filters
        filters.add(new LabelValueContainer<String>(null, "--- PSM ---"));
        for (Map.Entry<String, String>  scoreIt
                : getAvailableScoreShorts().entrySet()) {
            String[] filterNames = PSMScoreFilter.getShortAndFilteringName(
                    scoreIt.getKey(), scoreIt.getValue());

            if (filterNames != null) {
                filters.add(new LabelValueContainer<String>(filterNames[0], filterNames[1]));
            }
        }

        for(RegisteredFilters filter : RegisteredFilters.getPSMFilters()) {
            filters.add(new LabelValueContainer<String>(filter.getShortName(),
                    filter.getFilteringListName()));
        }

        // peptide filters
        filters.add(new LabelValueContainer<String>(null, "--- Peptide ---"));
        for (Map.Entry<String, String>  scoreIt
                : getAvailableScoreShorts().entrySet()) {
            String[] filterNames = PeptideScoreFilter.getShortAndFilteringName(
                    scoreIt.getKey(), scoreIt.getValue());
            if (filterNames != null) {
                filters.add(new LabelValueContainer<String>(filterNames[0], filterNames[1]));
            }
        }
        filters.add(new LabelValueContainer<String>(RegisteredFilters.NR_PSMS_PER_PEPTIDE_FILTER.getShortName(),
                RegisteredFilters.NR_PSMS_PER_PEPTIDE_FILTER.getFilteringListName()));
        filters.add(new LabelValueContainer<String>(RegisteredFilters.NR_SPECTRA_PER_PEPTIDE_FILTER.getShortName(),
                RegisteredFilters.NR_SPECTRA_PER_PEPTIDE_FILTER.getFilteringListName()));
        filters.add(new LabelValueContainer<String>(RegisteredFilters.PEPTIDE_FILE_LIST_FILTER.getShortName(),
                RegisteredFilters.PEPTIDE_FILE_LIST_FILTER.getFilteringListName()));

        // protein filters
        filters.add(new LabelValueContainer<String>(null, "--- Protein ---"));
        filters.add(new LabelValueContainer<String>(RegisteredFilters.NR_SPECTRA_PER_PROTEIN_FILTER.getShortName(),
                RegisteredFilters.NR_SPECTRA_PER_PROTEIN_FILTER.getFilteringListName()));
        filters.add(new LabelValueContainer<String>(RegisteredFilters.NR_PSMS_PER_PROTEIN_FILTER.getShortName(),
                RegisteredFilters.NR_PSMS_PER_PROTEIN_FILTER.getFilteringListName()));
        filters.add(new LabelValueContainer<String>(RegisteredFilters.NR_PEPTIDES_PER_PROTEIN_FILTER.getShortName(),
                RegisteredFilters.NR_PEPTIDES_PER_PROTEIN_FILTER.getFilteringListName()));
        filters.add(new LabelValueContainer<String>(RegisteredFilters.PROTEIN_SCORE_FILTER.getShortName(),
                RegisteredFilters.PROTEIN_SCORE_FILTER.getFilteringListName()));
        filters.add(new LabelValueContainer<String>(RegisteredFilters.NR_UNIQUE_PEPTIDES_PER_PROTEIN_FILTER.getShortName(),
                RegisteredFilters.NR_UNIQUE_PEPTIDES_PER_PROTEIN_FILTER.getFilteringListName()));
        filters.add(new LabelValueContainer<String>(RegisteredFilters.NR_GROUP_UNIQUE_PEPTIDES_PER_PROTEIN_FILTER.getShortName(),
                RegisteredFilters.NR_GROUP_UNIQUE_PEPTIDES_PER_PROTEIN_FILTER.getFilteringListName()));

        return filters;
    }


    @Override
    public List<ReportProtein> calculateInference(Map<Long, Group> groupMap,
            Map<String, ReportPSMSet> reportPSMSetMap,
            boolean considerModifications,
            Map<String, Boolean> psmSetSettings) {
        nrSpectra = 0;
        nrUsedSpectra = 0;
        nrFinishedSplits = 0;
        nrSplits = 1;
        logger.info("calculateInference started...");

        StringBuilder filterSB = new StringBuilder();
        for (AbstractFilter filter : filters) {
            if (filterSB.length() > 0) {
                filterSB.append(", ");
            }
            filterSB.append(filter.toString());
        }

        String scoreShort = getScoring().getScoreSetting().getValue();
        logger.info("scoring: " + getScoring().getName() + " with " +
                scoreShort + ", " +
                getScoring().getPSMForScoringSetting().getValue() +
                "\n\tfilters: " + filterSB.toString() +
                "\n\tpsmSetSettings: " + psmSetSettings +

                "\n\tgroupMap: " + groupMap.size() +
                "\n\treportPSMSetMap: " + reportPSMSetMap.size()
                );

        // map from the spectra to the associated accessions' IDs
        Map<String, Set<Long>> spectraAccessions = new HashMap<String, Set<Long>>(reportPSMSetMap.size() / 2);

        // the reportPSMs are needed frequently, map them from the spectrum ID
        Map<Long, ReportPSM> reportPSMMap = new HashMap<Long, ReportPSM>(reportPSMSetMap.size() / 2);

        // list of the spectrumIdentificationKeys of the already used spectra (this set gets filled while reporting proteins)
        Set<String> usedSpectra = new HashSet<String>();

        logger.info("building reportPSMMap...");

        for (ReportPSMSet psmSet : reportPSMSetMap.values()) {
            for (ReportPSM reportPSM : psmSet.getPSMs()) {
                // if this PSM satisfies the filters, cache it
                if (FilterFactory.
                        satisfiesFilterList(reportPSM, 0L, filters)) {
                    String psmIdKey = reportPSM.getSpectrum().
                            getSpectrumIdentificationKey(psmSetSettings);

                    reportPSMMap.put(reportPSM.getSpectrum().getID(), reportPSM);
                    usedSpectra.add(psmIdKey);

                    // populate the spectraAccessions map
                    Set<Long> accessions = spectraAccessions.get(psmIdKey);
                    if (accessions == null) {
                        accessions = new HashSet<Long>();
                        spectraAccessions.put(psmIdKey, accessions);
                    }
                    for (Accession acc : reportPSM.getAccessions()) {
                        accessions.add(acc.getID());
                    }
                }
            }
        }
        logger.info("reportPSMMap build");

        nrSpectra = usedSpectra.size();
        usedSpectra = null;

        logger.info("creating disjoint splits");

        Long splitIDcounter = 0L;
        Map<Long, Set<Long>> splitIdReportPSMid = new HashMap<Long, Set<Long>>();
        Map<Long, Set<Long>> splitIdAccessions = new HashMap<Long, Set<Long>>();

        Map<String, Long> psmIDsplitID = new HashMap<String, Long>();
        Map<Long, Set<String>> splitIdSpectraID = new HashMap<Long, Set<String>>();

        for (Long reportPSMid : reportPSMMap.keySet()) {
            String psmIdKey = reportPSMMap.get(reportPSMid).getSpectrum().getSpectrumIdentificationKey(psmSetSettings);
            Long splitID = psmIDsplitID.get(psmIdKey);

            if (splitID != null) {
                splitIdReportPSMid.get(splitID).add(reportPSMid);
            } else {
                // find split with any accessions
                Set<Long> mergeToReportPSMs = null;
                Set<Long> mergeToAccessions = null;
                Set<String> mergeToSplitIDs = null;

                Iterator<Entry<Long, Set<Long>>> it = splitIdAccessions.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Long, Set<Long>> splitIt = it.next();
                    Set<Long> accessions = new HashSet<Long>(spectraAccessions.get(psmIdKey));

                    if (accessions.removeAll(splitIdAccessions.get(splitIt.getKey()))) {
                        // overlap in accessions
                        if (splitID == null) {
                            // any further split will be merged to this one
                            splitID = splitIt.getKey();
                            mergeToReportPSMs = splitIdReportPSMid.get(splitID);
                            mergeToAccessions = splitIt.getValue();
                            mergeToSplitIDs = splitIdSpectraID.get(splitID);
                        } else {
                            // need to merge
                            mergeToReportPSMs.addAll(splitIdReportPSMid.get(splitIt.getKey()));
                            splitIdReportPSMid.remove(splitIt.getKey());

                            mergeToAccessions.addAll(splitIt.getValue());
                            it.remove();

                            // the psmIDs need to be re-linked
                            Set<String> psmIDs = splitIdSpectraID.get(splitIt.getKey());
                            for (String psmID : psmIDs) {
                                psmIDsplitID.put(psmID, splitID);
                            }

                            mergeToSplitIDs.addAll(psmIDs);
                            splitIdSpectraID.remove(splitIt.getKey());
                        }
                    }
                }

                // add the accessions and reportPSMid to the corresponding split or create new
                if (splitID == null) {
                    // a new split
                    splitIDcounter++;
                    splitID = splitIDcounter;
                    mergeToReportPSMs = new HashSet<Long>();
                    splitIdReportPSMid.put(splitID, mergeToReportPSMs);

                    mergeToAccessions = new HashSet<Long>();
                    splitIdAccessions.put(splitID, mergeToAccessions);

                    mergeToSplitIDs = new HashSet<String>();
                    splitIdSpectraID.put(splitID, mergeToSplitIDs);
                }


                mergeToReportPSMs.add(reportPSMid);
                mergeToAccessions.addAll(spectraAccessions.get(psmIdKey));

                psmIDsplitID.put(psmIdKey, splitID);
                mergeToSplitIDs.add(psmIdKey);
            }
        }
        psmIDsplitID = null;
        splitIdSpectraID = null;

        nrSplits = splitIdAccessions.size();

        logger.debug("number of splits: " + nrSplits);

        // get the number of threads used for the inference
        int nr_threads = getAllowedThreads();
        if (nr_threads < 1) {
            nr_threads = Runtime.getRuntime().availableProcessors();
        }
        logger.debug("used threads: " + nr_threads);

        List<SpectrumExtractorWorkerThread> threads = new ArrayList<SpectrumExtractorWorkerThread>(nr_threads);

        // the proteins of all splits
        List<ReportProtein> completeReportProteinList = new ArrayList<ReportProtein>(groupMap.size());

        // the remaining group IDs, which were not yet processed
        Set<Long> leftGroupIDs = new HashSet<Long>(groupMap.keySet());

        for (Long splitID : splitIdReportPSMid.keySet()) {
            // maps from groupID/proteinID to the peptides, for (re-)scoring...
            Map<Long, Set<Peptide>> groupsPeptides =
                    new HashMap<Long, Set<Peptide>>(groupMap.size());

            // the (remaining) proteins
            List<ReportProtein> proteinList =
                    new ArrayList<ReportProtein>(groupMap.size());

            Set<Long> splitAccessions = splitIdAccessions.get(splitID);


            Iterator<Long> groupIt = leftGroupIDs.iterator();
            while (groupIt.hasNext()) {
                Long grID = groupIt.next();
                Group group = groupMap.get(grID);

                // only groups with accessions in the split are interesting
                if (group.getAccessions().size() > 0) {
                    // create protein, with same ID as groupID
                    ReportProtein repProtein = new ReportProtein(group.getID());

                    // add the accessions
                    Boolean notInSplit = null;
                    for (Accession acc : group.getAccessions().values()) {
                        if (notInSplit == null) {
                            if (!splitAccessions.contains(acc.getID())) {
                                notInSplit = true;
                            } else {
                                notInSplit = false;
                            }
                        }

                        repProtein.addAccession(acc);
                    }
                    if (notInSplit) {
                        continue;
                    }

                    // put this stub-protein in the protein list
                    proteinList.add(repProtein);

                    // prepare peptide cache for protein
                    Set<Peptide> pepSet =
                            new HashSet<Peptide>(group.getAllPeptides().size());
                    groupsPeptides.put(repProtein.getID(), pepSet);

                    for (Peptide pep : group.getAllPeptides().values()) {
                        // put peptide into cache
                        pepSet.add(pep);
                    }
                }

                // remove group ID of group without accession or added to split
                groupIt.remove();
            }

            Map<Long, ReportPSM> splitReportPSMMap = new HashMap<Long, ReportPSM>(splitIdReportPSMid.get(splitID).size());
            for (Long psmID : splitIdReportPSMid.get(splitID)) {
                splitReportPSMMap.put(psmID, reportPSMMap.get(psmID));
            }

            // the PSMSets used by an already used reportPeptide (this map gets filled while reporting proteins)
            Map<String, Set<ReportPSMSet>> peptidesSpectra =
                    new HashMap<String, Set<ReportPSMSet>>();

            // this is the list, that is going to be returned
            List<ReportProtein> reportProteinList =
                    new ArrayList<ReportProtein>(proteinList.size());

            // reset the used spectra
            usedSpectra = new HashSet<String>();

            changedAccessions = new HashSet<Long>();
            boolean iterate = true;
            while (iterate) {
                // now the (remaining) proteins get rebuild (with usable spectra) and scored
                proteinListIt = proteinList.listIterator();

                // initialize and start  the worker threads
                threads.clear();
                for (int i=0; (i < nr_threads); i++) {
                    SpectrumExtractorWorkerThread workerThread =
                            new SpectrumExtractorWorkerThread(i+1, this,
                                    getScoring(), filters, groupsPeptides,
                                    reportPSMSetMap, splitReportPSMMap, peptidesSpectra,
                                    usedSpectra, scoreShort, considerModifications,
                                    psmSetSettings);
                    threads.add(workerThread);
                    workerThread.start();
                }

                // wait for the threads to finish
                for (SpectrumExtractorWorkerThread workerThread : threads) {
                    try {
                        workerThread.join();
                    } catch (InterruptedException e) {
                        // TODO: make better error/exception
                        logger.error("thread got interrupted!");
                        e.printStackTrace();
                    }
                }

                // remove "empty" proteins
                proteinListIt = proteinList.listIterator();
                while (proteinListIt.hasNext()) {
                    if (proteinListIt.next().getNrPeptides() < 1) {
                        proteinListIt.remove();
                    }
                }

                // order the protein list
                Comparator<ReportProtein> comparator =
                        ReportProteinComparatorFactory.CompareType.SCORE_SORT.getNewInstance();
                Collections.sort(proteinList, comparator);


                // take the next protein from the list, that can be reported
                //proteinListIt = proteinList.listIterator();
                proteinListIt = null;


                Double reportScore = null;
                changedAccessions.clear();
                iterate = false;
                while (proteinList.size() > 0 /*proteinListIt.hasNext()*/) {
                    ReportProtein protein = proteinList.get(0) /*proteinListIt.next()*/;

                    // there was a protein reported and the next has another score -> do the next scoring
                    if ((reportScore != null) &&
                            !reportScore.equals(protein.getScore())) {
                        // start next scoring
                        break;
                    }

                    // count the new peptides in this protein
                    int newPeptides = 0;
                    // IDs of the peptides of this protein
                    Set<String> proteinsPeptides = null;
                    // IDs of the spectra of this protein
                    Set<String> proteinsSpectra = null;

                    // combine all high-scoring proteins with the same peptides and spectra as the current protein
                    proteinListIt = proteinList.listIterator();
                    if (proteinListIt.hasNext()) {
                        proteinListIt.next();
                    }
                    while (proteinListIt.hasNext()) {
                        ReportProtein nextProt = proteinListIt.next();

                        if (!protein.getScore().equals(nextProt.getScore())) {
                            // different score -> no further check needed, leave the loop
                            break;
                        } else {
                            if (proteinsPeptides == null) {
                                // get proteins peptides (if not yet  done)
                                proteinsPeptides = new HashSet<String>(protein.getPeptides().size());
                                proteinsSpectra = new HashSet<String>(proteinsPeptides.size());
                                for (ReportPeptide peptide : protein.getPeptides()) {
                                    if (!peptidesSpectra.containsKey(peptide.getStringID())) {
                                        newPeptides++;
                                    }
                                    proteinsPeptides.add(peptide.getStringID());
                                    proteinsSpectra.addAll(peptide.getSpectraIdentificationKeys());
                                }
                            }

                            // get the protein's peptides and spectra
                            Set<String> nextProteinsPeptides = new HashSet<String>();
                            Set<String> nextProteinsSpectra = new HashSet<String>();
                            for (ReportPeptide peptide : nextProt.getPeptides()) {
                                nextProteinsPeptides.add(peptide.getStringID());
                                nextProteinsSpectra.addAll(peptide.getScoringSpectraIdentificationKeys());
                            }

                            if (nextProteinsPeptides.equals(proteinsPeptides) &&
                                    nextProteinsSpectra.equals(proteinsSpectra)) {
                                // add the accessions to lastIt
                                for (Accession acc : nextProt.getAccessions()) {
                                    protein.addAccession(acc);
                                }

                                // remove the next protein from the list
                                proteinListIt.remove();
                            }
                        }
                    }

                    // remove the protein from the proteinList (either it is ok for report now, or it never will be)
                    //proteinListIt.remove();
                    proteinList.remove(0);


                    if (FilterFactory.
                            satisfiesFilterList(protein, 0L, filters)) {
                        // TODO: insert something like "needs X new spectra/PSMs/Peptides per protein". for now it is set to 1 new peptide

                        // check for subprotein
                        if (proteinsPeptides == null) {
                            // get proteins peptides (if not yet  done)
                            proteinsPeptides = new HashSet<String>(protein.getPeptides().size());
                            for (ReportPeptide peptide : protein.getPeptides()) {
                                if (!peptidesSpectra.containsKey(peptide.getStringID())) {
                                    newPeptides++;
                                }
                                proteinsPeptides.add(peptide.getStringID());
                            }
                        }

                        if (newPeptides > 0) {
                            // at least one new peptide, so the protein may be reported
                            // store the used peptides and spectra
                            for (ReportPeptide peptide : protein.getPeptides()) {
                                String peptideKey = peptide.getStringID();

                                if (!peptidesSpectra.containsKey(peptideKey)) {
                                    // peptide is not yet stored
                                    Set<ReportPSMSet> psms = new HashSet<ReportPSMSet>();

                                    for (PSMReportItem psmSet : peptide.getPSMs()) {
                                        if (psmSet instanceof ReportPSMSet) {
                                            psms.add((ReportPSMSet) psmSet);
                                            Set<Long> psmIDs = new HashSet<Long>();

                                            // add the used spectra to the set
                                            for (ReportPSM psm : ((ReportPSMSet) psmSet).getPSMs()) {
                                                String specIdKey = psm.getSpectrum().
                                                        getSpectrumIdentificationKey(psmSetSettings);

                                                // it is not relevant to check, whether the spectrum is scoring,
                                                // because the spectrum is scoring in any of the used peptides
                                                usedSpectra.add(specIdKey);

                                                Set<Long> accIDs = spectraAccessions.get(specIdKey);
                                                if (accIDs != null) {
                                                    changedAccessions.addAll(accIDs);
                                                }

                                                psmIDs.add(psm.getSpectrum().getID());
                                            }

                                            // try to get info/scores on the PSMSet (only possible, if Set is equal to one in reportPSMSetMap)
                                            String key = ((ReportPSMSet) psmSet).getIdentificationKey(psmSetSettings);
                                            ReportPSMSet givenSet = reportPSMSetMap.get(key);
                                            if ((givenSet != null) &&
                                                    (givenSet.getFDRScore() != null)) {
                                                Set<Long> givenPSMids = new HashSet<Long>();
                                                for (ReportPSM psm : givenSet.getPSMs()) {
                                                    givenPSMids.add(psm.getSpectrum().getID());
                                                }

                                                if (psmIDs.equals(givenPSMids)) {
                                                    psmSet.setFDRScore(givenSet.getFDRScore().getValue());
                                                    psmSet.setFDR(givenSet.getFDR());
                                                }
                                            }
                                        } else {
                                            logger.error("not reportPSMSet PSM in peptide");
                                        }
                                    }

                                    nrUsedSpectra = usedSpectra.size();
                                    peptidesSpectra.put(peptideKey, psms);
                                }
                            }

                            // insert the protein in the "to be reported"-list
                            reportProteinList.add(protein);

                            // found a protein to report, get its score
                            reportScore = protein.getScore();

                            if (proteinList.size() > 0) {
                                iterate = true;
                            }
                        } else {
                            // no new peptides, so this protein may be a subSet or same protein as an already reported protein

                            // get all the protein's spectra
                            if (proteinsSpectra == null) {
                                proteinsSpectra = new HashSet<String>(proteinsPeptides.size());
                                for (ReportPeptide peptide : protein.getPeptides()) {
                                    proteinsSpectra.addAll(
                                            peptide.getSpectraIdentificationKeys());
                                }
                            }

                            for (ReportProtein reportProtein : reportProteinList) {
                                // get the spectra and peptides of the reported protein
                                Set<String> reportProteinsSpectra = new HashSet<String>();
                                Set<String> reportProteinsPeptides = new HashSet<String>();
                                for (ReportPeptide peptide : reportProtein.getPeptides()) {
                                    reportProteinsSpectra.addAll(
                                            peptide.getSpectraIdentificationKeys());
                                    reportProteinsPeptides.add(
                                            peptide.getStringID());
                                }

                                if (reportProteinsSpectra.containsAll(proteinsSpectra)) {
                                    // the protein is completely explained by the reportProtein
                                    if (proteinsSpectra.size() ==
                                            reportProteinsSpectra.size()) {
                                        // the protein has the same spectra as another protein
                                        // as it has no new peptides, there must be
                                        // another protein with same peptides
                                        if ((reportProteinsPeptides.size() == proteinsPeptides.size()) &&
                                                reportProteinsPeptides.containsAll(proteinsPeptides)) {
                                            // TODO: this check should be irrelevant, as it is checked before
                                            // also the peptides are the same -> add the accession(s)
                                            for (Accession acc : protein.getAccessions()) {
                                                reportProtein.addAccession(acc);
                                            }
                                        }
                                    } else {
                                        boolean subSetAlreadyThere = false;
                                        // check, if the protein is a sameSet of another subSet
                                        for (ReportProtein subSet : reportProtein.getSubSets()) {
                                            reportProteinsSpectra = new HashSet<String>();
                                            reportProteinsPeptides = new HashSet<String>();
                                            for (ReportPeptide peptide : subSet.getPeptides()) {
                                                reportProteinsSpectra.addAll(
                                                        peptide.getSpectraIdentificationKeys());
                                                reportProteinsPeptides.add(
                                                        peptide.getStringID());
                                            }

                                            if (proteinsPeptides.equals(reportProteinsPeptides) &&
                                                    proteinsSpectra.equals(reportProteinsSpectra)) {
                                                // protein is same as subSet, add the accessions
                                                for (Accession acc : protein.getAccessions()) {
                                                    subSet.addAccession(acc);
                                                }

                                                subSetAlreadyThere = true;
                                                break;
                                            }
                                        }

                                        if (!subSetAlreadyThere) {
                                            // the protein is a new subset of the reportProtein
                                            reportProtein.addToSubsets(protein);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            completeReportProteinList.addAll(reportProteinList);
            nrFinishedSplits++;
            logger.debug("Finished split " + nrFinishedSplits + " / " + nrSplits + " (" +((double)nrFinishedSplits / nrSplits * 100) +"%)" );
        }

        logger.info(name + " calculateInference done, " + completeReportProteinList.size() + " groups inferred");
        inferenceDone = true;
        return completeReportProteinList;
    }


    /**
     * Returns the next protein in the protein list.
     *
     * @return
     */
    public synchronized ReportProtein getNextProteinForRebuild() {
        synchronized (proteinListIt) {
            if (proteinListIt != null) {

                while (proteinListIt.hasNext()) {
                    ReportProtein prot = proteinListIt.next();

                    if (prot.getNrPeptides() > 0) {
                        // protein is already build, check for possible changes in last iteration

                        for (Accession acc : prot.getAccessions()) {
                            if (changedAccessions.contains(acc.getID())) {
                                // protein might have been changed, rebuild it
                                return prot;
                            }
                        }
                    } else {
                        // protein is not yet build, build it
                        return prot;
                    }

                    // the protein is not changed but build, no rebuild necessary
                }

                return null;
            } else {
                // TODO: throw exception or something
                logger.error("The protein iterator is not yet initialized!");
                return null;
            }
        }
    }


    @Override
    public String getName() {
        return name;
    }


    @Override
    public String getShortName() {
        return shortName;
    }


    @Override
    public Long getProgressValue() {
        if (inferenceDone) {
            return Long.valueOf(101);
        } else {
            Long p;
            if ((nrUsedSpectra == 0) || (nrSpectra == 0) || (nrSplits == 0)) {
                p = Long.valueOf(0);
            } else {
                p = (long)(((double)nrFinishedSplits + (double)nrUsedSpectra / (double)nrSpectra) / (double)nrSplits * 100.0);
            }

            return p;
        }
    }
}
