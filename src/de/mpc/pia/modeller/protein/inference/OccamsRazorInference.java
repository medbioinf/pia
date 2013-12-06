package de.mpc.pia.modeller.protein.inference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.Group;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.report.filter.peptide.NrPSMsPerPeptideFilter;
import de.mpc.pia.modeller.report.filter.peptide.NrSpectraPerPeptideFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideFileListFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideScoreFilter;
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
 * <br/><br/>
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
	
	
	/** the logger for this class */
	private static final Logger logger= Logger.getLogger(OccamsRazorInference.class);
	
	
	@Override
	public List<LabelValueContainer<String>> getFilterTypes() {
		List<LabelValueContainer<String>> filters = new ArrayList<LabelValueContainer<String>>();
		
		// PSM filters
		filters.add(new LabelValueContainer<String>(null, "--- PSM ---"));
		for (String scoreShort : getAvailableScoreShorts()) {
			String[] filterNames = PSMScoreFilter.getShortAndFilteringName(scoreShort);
			if (filterNames != null) {
				filters.add(new LabelValueContainer<String>(filterNames[0], filterNames[1]));
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
		
		//gehen denn jetzt die filter oder nicht?
		//accessions und scores testen!!!!
		
		
		// peptide filters
		filters.add(new LabelValueContainer<String>(null, "--- Peptide ---"));
		filters.add(new LabelValueContainer<String>(NrPSMsPerPeptideFilter.shortName(),
				NrPSMsPerPeptideFilter.filteringName()));
		filters.add(new LabelValueContainer<String>(NrSpectraPerPeptideFilter.shortName(),
				NrSpectraPerPeptideFilter.filteringName()));
		filters.add(new LabelValueContainer<String>(PeptideFileListFilter.shortName(),
				PeptideFileListFilter.filteringName()));
		
		for (String scoreShort : getAvailableScoreShorts()) {
			String[] filterNames = PeptideScoreFilter.getShortAndFilteringName(scoreShort);
			if (filterNames != null) {
				filters.add(new LabelValueContainer<String>(filterNames[0], filterNames[1]));
			}
		}
		
		// protein filters
		filters.add(new LabelValueContainer<String>(null, "--- Protein ---"));
		filters.add(new LabelValueContainer<String>(NrPeptidesPerProteinFilter.shortName(),
				NrPeptidesPerProteinFilter.filteringName()));
		filters.add(new LabelValueContainer<String>(NrPSMsPerProteinFilter.shortName(),
				NrPSMsPerProteinFilter.filteringName()));
		filters.add(new LabelValueContainer<String>(NrSpectraPerProteinFilter.shortName(),
				NrSpectraPerProteinFilter.filteringName()));
		filters.add(new LabelValueContainer<String>(ProteinFileListFilter.shortName(),
				ProteinFileListFilter.filteringName()));
		filters.add(new LabelValueContainer<String>(ProteinScoreFilter.shortName,
				ProteinScoreFilter.filteringName));
		filters.add(new LabelValueContainer<String>(NrUniquePeptidesPerProteinFilter.shortName(),
				NrUniquePeptidesPerProteinFilter.filteringName()));
		
		return filters;
	}
	
	@Override
	public List<ReportProtein> calculateInference(Map<Long, Group> groupMap,
			Map<String, ReportPSMSet> reportPSMSetMap,
			boolean considerModifications,
			Map<String, Boolean> psmSetSettings) {
		progress = 0.0;
		logger.info("calculateInference started...");
		logger.info("scoring: " + getScoring().getName() + " with " + 
				getScoring().getScoreSetting().getValue() + ", " +
				getScoring().getPSMForScoringSetting().getValue());
		
		// groups with the IDs in this set should be reported
		Set<Long> reportGroupsIDs = new HashSet<Long>();
		
		// all the PSMs of the groups, including the PSMs in groups' children
		Map<Long, Set<String>> groupsAllPeptides = new HashMap<Long, Set<String>>(groupMap.size());
		
		// map from the group IDs to the IDs of subgroups, if any
		Map<Long, Set<Long>> subGroups;
		
		// the peptide children groups, mapped by the groups' IDs
		Map<Long, Map<Long, Group>> groupPepChildren = new HashMap<Long, Map<Long,Group>>(groupMap.size());
		
		// maps from the groups' IDs to the groups' IDs with equal PSMs after filtering
		Map<Long, Set<Long>> sameSets;
		
		// maps from the tree ID to the groupIDs
		Map<Long, Set<Long>> treeMap = new HashMap<Long, Set<Long>>();
		
		
		// maps from the groups' IDs to the reportPeptides
		Map<Long, List<ReportPeptide>> reportPeptidesMap =
			createFilteredReportPeptides(groupMap, reportPSMSetMap,
					considerModifications, psmSetSettings);
		
		Double progressStep = 1.0 / groupMap.size() * 25.0;
		
		// go through all the groups
		for (Map.Entry<Long, Group> gIt : groupMap.entrySet()) {
			// if this group has accessions and filtered ReportPeptides, put
			// these ReportPeptide's stringIDs into the groupsAllPeptides Map
			if ((gIt.getValue().getAccessions().size() > 0) &&
					groupHasReportPeptides(gIt.getValue(), reportPeptidesMap)) {
				Set<String> allPeptidesSet = new HashSet<String>();
				groupsAllPeptides.put(gIt.getKey(), allPeptidesSet);
				
				if (reportPeptidesMap.containsKey(gIt.getKey())) {
					for (ReportPeptide pepIt : reportPeptidesMap.get(gIt.getKey())) {
						allPeptidesSet.add(pepIt.getStringID());
					}
				}
				
				for (Group pepGroupIt : gIt.getValue().getAllPeptideChildren().values()) {
					if (reportPeptidesMap.containsKey(pepGroupIt.getID())) {
						for (ReportPeptide pepIt : reportPeptidesMap.get(pepGroupIt.getID())) {
							allPeptidesSet.add(pepIt.getStringID());
						}
					}
				}
				
				// fill the treeMap
				Set<Long> treeSet = treeMap.get(gIt.getValue().getTreeID());
				if (treeSet == null) {
					treeSet = new HashSet<Long>();
					treeMap.put(gIt.getValue().getTreeID(), treeSet);
				}
				treeSet.add(gIt.getKey());
			}
			
			
			// check, whether the group has no parent groups (constraint 1)
			if ((gIt.getValue().getParents() == null) ||
					(gIt.getValue().getParents().size() == 0) ) {
				if ((groupsAllPeptides.get(gIt.getKey()) != null) &&
						(groupsAllPeptides.get(gIt.getKey()).size() > 0)) {
					// there is at least one ReportPeptide for this group
					// -> constraint 1) fulfilled
					reportGroupsIDs.add(gIt.getKey());
					
					// map of all peptide children (which have at least one 
					// ReportPeptide) for constraint 3 check
					Map<Long, Group> peptideChildren = new HashMap<Long, Group>();
					
					for (Map.Entry<Long, Group> pepChildIt : gIt.getValue().getAllPeptideChildren().entrySet()) {
						if (groupHasDirectReportPeptides(pepChildIt.getValue(), reportPeptidesMap)) {
							peptideChildren.put(pepChildIt.getKey(),
									pepChildIt.getValue());
						}
					}
					
					groupPepChildren.put(gIt.getKey(), peptideChildren);
				}
			}
			
			progress += progressStep;
		}
		
		
		// check for subGroups
		// for this, we have to check the groupsAllPeptides of each group
		// against the groupsAllPeptides of each group in the same tree
		// while doing this, it is also checked, whether groups are equal (due to filtering)
		Set<Long> newReportGroups = new HashSet<Long>(reportGroupsIDs.size());
		
		subGroups = new HashMap<Long, Set<Long>>(groupsAllPeptides.size());
		sameSets = new HashMap<Long, Set<Long>>(groupsAllPeptides.size());
		
		progressStep = 1.0 / groupsAllPeptides.size() * 25.0;
		for (Map.Entry<Long, Set<String>> grIt : groupsAllPeptides.entrySet()) {
			Long treeID = groupMap.get(grIt.getKey()).getTreeID();
			
			// every group with ReportPeptides gets a subGroups- and sameSets-container
			Set<Long> subs = new HashSet<Long>();
			subGroups.put(grIt.getKey(), subs);
			
			Set<Long> sameSet = sameSets.get(grIt.getKey()); 
			if (sameSet == null) {
				sameSet = new HashSet<Long>();
				sameSets.put(grIt.getKey(), sameSet);
			}
			
			// check against the groups in the tree
			for (Long checkID : treeMap.get(treeID)) {
				if (grIt.getKey() == checkID) {
					// don't check against self
					continue;
				}
				
				// check, whether checkID's group is a subset of grIt's
				Set<String> checkSet = groupsAllPeptides.get(checkID);
				
				// only check, if the set is smaller or equal (so it can be a subset)
				if ((checkSet.size() > 0) &&
						(checkSet.size() <= grIt.getValue().size())) {
					int PeptidesLeft = checkSet.size();
					
					for (String pepString : checkSet) {
						if (grIt.getValue().contains(pepString)) {
							PeptidesLeft--;
						}
					}
					
					if (PeptidesLeft == 0) {
						// all the ReportPeptides are in the set, so it is a subSet
						subs.add(checkID);
						
						if (checkSet.size() == grIt.getValue().size()) {
							// ReportPeptides are the same in checkSet and grIt
							sameSet.add(checkID);
							
							// if checkID's group had a sameSet before, merge the sameSets
							Set<Long> checkSameSet = sameSets.get(checkID);
							if (checkSameSet != null) {
								sameSet.addAll(checkSameSet);
							}
							sameSets.put(checkID, sameSet);
						}
					}
				}
			}
			
			if ((filters.size() > 0) && (reportGroupsIDs.contains(grIt.getKey()))) {
				// if there are filters, we may have sameSets now
				// check if any of the groups in this group's sameSet is already in the newReportGroups
				boolean anySameInReportGroups = false;
				
				for (Long sameID : sameSet) {
					if (newReportGroups.contains(sameID)) {
						anySameInReportGroups = true;
						break;
					}
				}
				
				if (!anySameInReportGroups) {
					// no sameGroup in reportGroups yet, put this one in
					newReportGroups.add(grIt.getKey());
					
					Set<Long> subGroup = subGroups.get(grIt.getKey());
					for (Long sameID : sameSet) {
						if (sameID != grIt.getKey()) {
							// delete the sameSet groups from this group's subGroups
							subGroup.remove(sameID);
						}
					}
				}
			}
			
			progress += progressStep;
		}
		
		
		// to reduce samesets of subgroubs (only one group of a sameset should
		// appear per subgroubs), the groups have to be iterated once more
		// this can only happen, if filters are set
		if (filters.size() > 0) {
			progressStep = 1.0 / groupsAllPeptides.size() * 16.0;
			for (Map.Entry<Long, Set<String>> grIt : groupsAllPeptides.entrySet()) {
				
				Set<Long> subs = subGroups.get(grIt.getKey());
				Set<Long> removeSubs = new HashSet<Long>(subGroups.size());
				
				// remove all the sameSets from the subs, these are not needed (same can not be sub)
				subs.removeAll(sameSets.get(grIt.getKey()));
				
				// iterate through this groups subgroups
				for (Long subID : subs) {
					// did we already check a sameGroup of this subGroup?
					if (!removeSubs.contains(subID)) {
						Set<Long> subSame = sameSets.get(subID);
						if (subSame.size() > 0) {
							// ok, this subgroup has sames, put all but the first on remove
							boolean first = true;
							for (Long sameID : subSame) {
								if (!first) {
									removeSubs.add(sameID);
								} else {
									first = false;
								}
							}
						}
					}
				}
				progress += progressStep;
			}
		}
		
		
		if (filters.size() > 0) {
			// the reportGroups may be reset, if there were filters
			reportGroupsIDs = newReportGroups;
		}
		
		progress = 66.0;
		
		// now check for constraints 2) and 3)
		progressStep = 1.0 / groupPepChildren.size() * 25.0;
		for (Map.Entry<Long, Map<Long, Group> > gcIt : groupPepChildren.entrySet()) {
			Group group = groupMap.get(gcIt.getKey());
			boolean hasDirectPeptides = false;
			
			if ((group != null) &&
					groupHasDirectReportPeptides(group, reportPeptidesMap)) {
				// constraint 2 is fulfilled
				// ID is already in the reportGroupsIDs, leave it there
				hasDirectPeptides = true;
			}
			
			if (!hasDirectPeptides) {
				// check for constraint 3)
		        // check if the gcIt's peptide groups are fully explained by another group's
				
				for (Map.Entry<Long, Map<Long, Group>> checkIt : groupPepChildren.entrySet()) {
					Set<Long> checkSameSet = sameSets.get(checkIt.getKey());
					
					// don't check against the same group or if gcIt's group in in the sameSet of checkIt
					if (!checkIt.getKey().equals(gcIt.getKey()) && 
							((checkSameSet == null) || !checkSameSet.contains(gcIt.getKey()))) {
						int count = 0;
						
						for (Map.Entry<Long, Group> cgIt : checkIt.getValue().entrySet()) {
							if (gcIt.getValue().containsKey(cgIt.getKey())) {
								count++;
							}
						}
						
						if (count == gcIt.getValue().size()) {
							// gcIt's group is fully explained by checkIt's group
							// -> remove it from the reportGroupsIDs
							reportGroupsIDs.remove(gcIt.getKey());
						}
					}
					
				}
				
			}
			progress += progressStep;
		}
		
		// now create the proteins from the groups, which are still in reportGroupsIDs
		// the list, that will be returned
		List<ReportProtein> reportProteinList = new ArrayList<ReportProtein>(reportGroupsIDs.size());
		
		// caching the proteins, especially the subSet proteins
		Map<Long, ReportProtein> proteins = new HashMap<Long, ReportProtein>(reportGroupsIDs.size());
		
		progressStep = 1.0 / reportGroupsIDs.size() * 9;;
		for (Long gID : reportGroupsIDs) {
			ReportProtein protein = createProtein(gID, proteins,
					reportPeptidesMap, groupMap, sameSets, subGroups);
			
			if (FilterFactory.
					satisfiesFilterList(protein, 0L, filters)) {
				// if all the filters are satisfied, add the protein to the reportProteinList
				reportProteinList.add(protein);
			}
			
			progress += progressStep;
		}
		
		progress = 100.0;
		logger.info("calculateInference done");
		return reportProteinList;
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
