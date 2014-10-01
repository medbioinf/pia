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
import de.mpc.pia.modeller.report.filter.peptide.NrPSMsPerPeptideFilter;
import de.mpc.pia.modeller.report.filter.peptide.NrSpectraPerPeptideFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideFileListFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideScoreFilter;
import de.mpc.pia.modeller.report.filter.protein.NrGroupUniquePeptidesPerProteinFilter;
import de.mpc.pia.modeller.report.filter.protein.NrPSMsPerProteinFilter;
import de.mpc.pia.modeller.report.filter.protein.NrPeptidesPerProteinFilter;
import de.mpc.pia.modeller.report.filter.protein.NrSpectraPerProteinFilter;
import de.mpc.pia.modeller.report.filter.protein.NrUniquePeptidesPerProteinFilter;
import de.mpc.pia.modeller.report.filter.protein.ProteinFileListFilter;
import de.mpc.pia.modeller.report.filter.protein.ProteinScoreFilter;
import de.mpc.pia.modeller.report.filter.psm.NrAccessionsPerPSMFilter;
import de.mpc.pia.modeller.report.filter.psm.NrPSMsPerPSMSetFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMAccessionsFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMFileListFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMModificationsFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMScoreFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMUniqueFilter;
import de.mpc.pia.tools.LabelValueContainer;


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
	protected static final String name = "Occam's Razor";
	
	/** the machine readable name of the filter */
	protected static final String shortName = "inference_occams_razor";
	
	/** the progress of the inference */
	private Double progress;
	
	
	/** this iterator iterates over the mapping from the tree ID to its groups*/
	private Iterator<Map.Entry<Long,Map<Long,Group>>> treeGroupsIterator;
	
	/** this list holds the reported proteins */
	private List<ReportProtein> reportProteins;
	
	/** the logger for this class */
	private static final Logger logger= Logger.getLogger(OccamsRazorInference.class);
	
	
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
				filters.add(new LabelValueContainer<String>(
						filterNames[0], filterNames[1]));
			}
		}
		filters.add(new LabelValueContainer<String>(NrPSMsPerPSMSetFilter.shortName(),
				NrPSMsPerPSMSetFilter.filteringName()));
		filters.add(new LabelValueContainer<String>(PSMUniqueFilter.shortName(),
				PSMUniqueFilter.filteringName()));
		filters.add(new LabelValueContainer<String>(PSMAccessionsFilter.shortName(),
				PSMAccessionsFilter.filteringName()));
		filters.add(new LabelValueContainer<String>(NrAccessionsPerPSMFilter.shortName(),
				NrAccessionsPerPSMFilter.filteringName()));
		filters.add(new LabelValueContainer<String>(PSMFileListFilter.shortName(),
				PSMFileListFilter.filteringName()));
		filters.add(new LabelValueContainer<String>(PSMModificationsFilter.shortName(),
				PSMModificationsFilter.filteringName()));
		
		//TODO: accessions filter testen!!!!
		
		
		// peptide filters
		filters.add(new LabelValueContainer<String>(null, "--- Peptide ---"));
		filters.add(new LabelValueContainer<String>(NrPSMsPerPeptideFilter.shortName(),
				NrPSMsPerPeptideFilter.filteringName()));
		filters.add(new LabelValueContainer<String>(NrSpectraPerPeptideFilter.shortName(),
				NrSpectraPerPeptideFilter.filteringName()));
		filters.add(new LabelValueContainer<String>(PeptideFileListFilter.shortName(),
				PeptideFileListFilter.filteringName()));
		
		for (Map.Entry<String, String> scoreIt
				: getAvailableScoreShorts().entrySet()) {
			String[] filterNames = PeptideScoreFilter.getShortAndFilteringName(
					scoreIt.getKey(), scoreIt.getValue());
			if (filterNames != null) {
				filters.add(new LabelValueContainer<String>(
						filterNames[0], filterNames[1]));
			}
		}
		
		// protein filters
		filters.add(new LabelValueContainer<String>(null, "--- Protein ---"));
		filters.add(new LabelValueContainer<String>(NrSpectraPerProteinFilter.shortName(),
				NrSpectraPerProteinFilter.filteringName()));
		filters.add(new LabelValueContainer<String>(NrPSMsPerProteinFilter.shortName(),
				NrPSMsPerProteinFilter.filteringName()));
		filters.add(new LabelValueContainer<String>(NrPeptidesPerProteinFilter.shortName(),
				NrPeptidesPerProteinFilter.filteringName()));
		filters.add(new LabelValueContainer<String>(ProteinFileListFilter.shortName(),
				ProteinFileListFilter.filteringName()));
		filters.add(new LabelValueContainer<String>(ProteinScoreFilter.shortName,
				ProteinScoreFilter.filteringName));
		filters.add(new LabelValueContainer<String>(NrUniquePeptidesPerProteinFilter.shortName(),
				NrUniquePeptidesPerProteinFilter.filteringName()));
		filters.add(new LabelValueContainer<String>(NrGroupUniquePeptidesPerProteinFilter.shortName(),
				NrGroupUniquePeptidesPerProteinFilter.filteringName()));
		
		return filters;
	}
	
	@Override
	public List<ReportProtein> calculateInference(Map<Long, Group> groupMap,
			Map<String, ReportPSMSet> reportPSMSetMap,
			boolean considerModifications,
			Map<String, Boolean> psmSetSettings) {
		progress = 0.0;
		logger.info(name + " calculateInference started...");
		logger.info("scoring: " + getScoring().getName() + " with " + 
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
		
		logger.info("PIA trees sorted, " + treeGroupMap.size() + " trees");
		
		// initialize the reported list
		reportProteins = new ArrayList<ReportProtein>();
		
		// the number of threads used for the inference
		int nr_threads = getAllowedThreads();
		if (nr_threads < 1) {
			nr_threads = Runtime.getRuntime().availableProcessors();
		}
		logger.debug("used threads: " + nr_threads);
		
		List<OccamsRazorWorkerThread> threads =
				new ArrayList<OccamsRazorWorkerThread>(nr_threads);
		
		// initialize and start  the worker threads
		threads.clear();
		for (int i=0; (i < nr_threads); i++) {
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
				// TODO: make better error/exception
				logger.error("thread got interrupted!");
				e.printStackTrace();
			}
		}
		
		progress = 100.0;
		logger.info(name + " calculateInference done, " + reportProteins.size() + " groups inferred");
		return reportProteins;
	}
	
	
	/**
	 * Returns the next tree in the map or null, if no more tree is available.
	 * 
	 * @return
	 */
	public synchronized Map.Entry<Long, Map<Long, Group>> getNextTree() {
		if (treeGroupsIterator != null) {
			if (treeGroupsIterator.hasNext()) {
				return treeGroupsIterator.next();
			}
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
		return name;
	}
	
	
	@Override
	public String getShortName() {
		return shortName;
	}
	
	
	@Override
	public Long getProgressValue() {
		return progress.longValue();
	}
}
