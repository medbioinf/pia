package de.mpc.pia.modeller.protein.inference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Group;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.report.filter.peptide.NrPSMsPerPeptideFilter;
import de.mpc.pia.modeller.report.filter.protein.NrPeptidesPerProteinFilter;
import de.mpc.pia.modeller.report.filter.psm.NrPSMsPerPSMSetFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMScoreFilter;
import de.mpc.pia.tools.LabelValueContainer;


/**
 * This inference tries to emulate ProteinExtractor's (as in ProteinScape,
 * Bruker) inference algorithm.
 * 
 * @author julian
 *
 */
@Deprecated
public class ProteinExtractorInference extends AbstractProteinInference {
	
	/** the human readable name of this filter */
	protected static final String name = "ProteinExtracter remake";
	
	/** the machine readable name of the filter */
	protected static final String shortName = "report_protein_extractor";
	
	/** the progress of the inference */
	private Double progress;
	
	
	/** the logger for this class */
	private static final Logger logger= Logger.getLogger(ProteinExtractorInference.class);
	
	
	@Override
	public List<LabelValueContainer<String>> getFilterTypes() {
		List<LabelValueContainer<String>> filters = new ArrayList<LabelValueContainer<String>>();
		
		filters.add(new LabelValueContainer<String>(null, "--- PSM ---"));
		for (String scoreShort : getAvailableScoreShorts()) {
			String[] filterNames = PSMScoreFilter.getShortAndFilteringName(scoreShort);
			
			if (filterNames != null) {
				filters.add(new LabelValueContainer<String>(filterNames[0], filterNames[1]));
			}
		}
		filters.add(new LabelValueContainer<String>(NrPSMsPerPSMSetFilter.shortName(),
				NrPSMsPerPSMSetFilter.filteringName()));
		
		
		filters.add(new LabelValueContainer<String>(null, "--- Peptide ---"));
		filters.add(new LabelValueContainer<String>(NrPSMsPerPeptideFilter.shortName(),
				NrPSMsPerPeptideFilter.filteringName()));
		
		
		filters.add(new LabelValueContainer<String>(null, "--- Protein ---"));
		filters.add(new LabelValueContainer<String>(NrPeptidesPerProteinFilter.shortName(),
				NrPeptidesPerProteinFilter.filteringName()));
		
		return filters;
	}
	
	
	@Override
	public List<ReportProtein> calculateInference(Map<Long, Group> groupMap,
			Map<String, ReportPSMSet> reportPSMSetMap,
			boolean considerModifications,
			Map<String, Boolean> psmSetSettings) {
		progress = 0.0;
		logger.info("calculateInference started...");
		
		
		// the reportPSMs are needed frequently, map them from the PSM ID
		Map<Long, ReportPSM> reportPSMMap = new HashMap<Long, ReportPSM>();
		
		// maps from the spectrumIdentificationKeys to the IDs of the reportPSMs (has not in every search engine more than one reportPSM per spectrum)
		Map<String, Set<Long>> spectrumToReportPSMIDs = new HashMap<String, Set<Long>>();
		
		
		// TODO: this COULD be parallelized, if needed...
		logger.info("building reportPSMMap...");
		for (ReportPSMSet psmSet : reportPSMSetMap.values()) {
			for (ReportPSM reportPSM : psmSet.getPSMs()) {
				
				// if this PSM satisfies the filters, cache it
				if (FilterFactory.
						satisfiesFilterList(reportPSM, 0L, filters)) {
					String psmIdKey = reportPSM.getSpectrum().
							getSpectrumIdentificationKey(psmSetSettings);
					
					if (reportPSMMap.put(reportPSM.getId(), reportPSM) != null) {
						logger.error("There was already a reportPSM in the " +
								"map with ID=" + reportPSM.getId());
					}
					
					Set<Long> reportPSMIDs = spectrumToReportPSMIDs.get(psmIdKey);
					if (reportPSMIDs == null) {
						reportPSMIDs = new HashSet<Long>();
						spectrumToReportPSMIDs.put(psmIdKey, reportPSMIDs);
					}
					reportPSMIDs.add(reportPSM.getId());
				}
				
			}
		}
		logger.info("reportPSMMap build");
		
		
		
		// the proteins, which are not yet reported
		List<ReportProtein> proteinList = new ArrayList<ReportProtein>();
		
		// maps from groupID/proteinID to the peptides
		Map<Long, Set<Peptide>> groupsPeptides =
				new HashMap<Long, Set<Peptide>>(groupMap.size());
		
		// build a first protein list, which is essentially empty
		logger.info("building groupsPeptides and first proteinList...");
		for (Group grIt : groupMap.values()) {
			// only groups with accessions are interesting
			if (grIt.getAccessions().size() > 0) {
				// create protein, with same ID as groupID
				ReportProtein repProtein = new ReportProtein(grIt.getID());
				
				// add the accessions
				for (Accession acc : grIt.getAccessions().values()) {
					repProtein.addAccession(acc);
				}
				
				// put this stub-protein in the protein list
				proteinList.add(repProtein);
				
				// prepare peptide cache for protein
				Set<Peptide> pepSet =
						new HashSet<Peptide>(grIt.getAllPeptides().size());
				groupsPeptides.put(repProtein.getID(), pepSet);
				
				for (Peptide pep : grIt.getAllPeptides().values()) {
					// put peptide into cache
					pepSet.add(pep);
				}
			}
		}
		logger.info("groupsPeptides and first proteinList build");
		
		
		
		// maps from the spectrumIdentificationKeys to the used reportPSM
		Map<String, Long> spectrumToReportPSM =
				new HashMap<String, Long>(spectrumToReportPSMIDs.size());
		
		// the PSM used by an peptide 
		Map<String, ReportPSM> peptidesMainSpectra = 
				new HashMap<String, ReportPSM>();
		
		
		boolean iterate = true;
		while (iterate) {
			
			// build the proteins with the already set and still free PSMs
			for (ReportProtein protein : proteinList) {
				
				
			}
			
			
			
			iterate = false;
			
			// check, whether we are still progressing
			//iterate = false;
		}
		
		
		
		
		
		// sort the proteins
		/*
		Comparator<ReportProtein> comparator;
		comparator = ReportProteinComparatorFactory.CompareType.SCORE_SORT.getNewInstance();
		if (getScoring().higherScoreBetter()) {
			comparator = ReportProteinComparatorFactory.descending(comparator);
		}
		Collections.sort(proteinList, comparator);
		*/
		
		
		// now sort the proteins into the reportProteinList
		
		
		List<ReportProtein> reportProteinList = new ArrayList<ReportProtein>();
		
		
		
		logger.info("calculateInference done.");
		progress = 100.0;
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
	
	
	
	// everything below is old and unusable !!!
	
	
	
/*
	@Override
	public Vector<ReportItem> calculateReportability(Map<Long, Group> groups) {
		Vector<ReportItem> report = new Vector<ReportItem>();	// the returned reports
		
		Map<Long, ReportItemGroup> reportItemMap;	// rebuild map for the groups as ReportItemGroups
		Vector<ReportItemGroup> reportItemVector;	// vector for sorting the reportItemMap
		
		Trie<String, Boolean> usedSpectra = new PatriciaTrie<String, Boolean>(StringKeyAnalyzer.CHAR);
		
		// update our variables for global calls
		minPeptideScore = Float.parseFloat(getFilterSetting("minimal peptide score"));
		minPeptideCount = Integer.parseInt(getFilterSetting("minimal peptide count"));
		maxExpValue = Float.parseFloat(getFilterSetting("maximal expectation value"));
		
		// some stupid infos
		System.out.println("minPeptideCount: "+minPeptideCount);
		System.out.println("minPeptideScore: "+minPeptideScore);
		System.out.println("maxExpValue: "+maxExpValue);
		
		// rebuild the groups into reportItems
		System.out.println("building report map...");
		reportItemMap = buildReportItemMap(groups);
		System.out.println("report map build");
		
		
		// first get the scoring done
		// TODO: think about: CAN the score change in the iteration?
		System.out.println("calculate score...");
		calculateScore(reportItemMap);
		System.out.println("scores done");
		
		
		// sort the reportItemMap by score
		System.out.println("creating vector...");
		reportItemVector = new Vector<ReportItemGroup>(reportItemMap.size());
		
		for (Map.Entry<Long, ReportItemGroup> grIt : reportItemMap.entrySet()) {
			if (Float.compare(grIt.getValue().getScore(), Float.NaN) != 0) {
				reportItemVector.add(grIt.getValue());
			}
		}
		
		reportItemMap = null;	// this is no longer needed, so free the memory
		
		System.out.println("sorting vector...");
		Collections.sort(reportItemVector, Collections.reverseOrder(new ReportItemScoreComparator()));
		System.out.println("vector done");
		
		long reportRank = 0;
		int sameRank;
		Vector<ReportItemGroup> scoreGroup = new Vector<ReportItemGroup>();
		float currentScore = reportItemVector.get(0).getScore();
		
		System.out.println("creating report");
		long percent = 0;
		// the reportItemVector is ordered by the score, so just go through it and report
		for (ReportItemGroup reportItem : reportItemVector) {
			//if (reportRank>100) {break;}
			
			percent++;
			if (percent % 1000 == 0) {
				System.out.println( (100 * percent / reportItemVector.size()) +"% " + percent + "/" + reportItemVector.size() + "..."  );
			}
			
			if ((Float.compare(reportItem.getScore(), currentScore) != 0) &&
					(scoreGroup.size() > 0)) {
				// score is not equal to last round, so create a report group
				reportRank++;
				sameRank = 0;
				
				
				for (ReportItem item : reportItemsFromScoreGroup(scoreGroup, reportRank, report.size())) {
					report.add(item);
					sameRank++;
				}
				
				
				if (sameRank > 1) {
					reportRank += sameRank - 1;
				}
				
				// add the spectra of the reported items to the used ones
				for (ReportItemGroup repItem : scoreGroup) {
					for (Map.Entry<String, ReportPeptide> pepIt : repItem.getPeptides().entrySet()) {
						for (PeptideSpectrumMatch spectrum : pepIt.getValue().getSpectra()) {
							if (!usedSpectra.containsKey(spectrum.getFileSourceID())) {
								usedSpectra.put(spectrum.getFileSourceID(), true);
							}
						}
					}
				}
				
				scoreGroup = new Vector<ReportItemGroup>();
			}
			
			
			// TODO: put groups with subsets of spectra / only used spectra into some kind of sub-report-group
			
			// TODO: handle minPeptideCountsScore (min X peptides with score >= Y)
			
			if ((reportItem.getPeptides().size() >=  minPeptideCount) &&
					(countNewSpectra(reportItem, usedSpectra) > 0) && 
					(reportItem.getAccessions().size() > 0)) {
				scoreGroup.add(reportItem);
			}
			
			currentScore = reportItem.getScore();
		}
		
		// add the last reportGroup
		if (scoreGroup.size() > 0) {
			reportRank++;
			for (ReportItem item : reportItemsFromScoreGroup(scoreGroup, reportRank, report.size())) {
				report.add(item);
			}
		}
		System.out.println("report created");
		
		
		return report;
	}
	
	
	/**
	 * Builds a map of groupID->ReportItemGroup from the given groupID->Group map. 
	 * 
	 * @param groups
	 * @return
	 */
/*	private Map<Long, ReportItemGroup> buildReportItemMap(Map<Long, Group> groups) {
		Map<Long, ReportItemGroup> map = new HashMap<Long, ReportItemGroup>();
		
		for (Map.Entry<Long, Group> groupIt : groups.entrySet()) {
			ReportItemGroup reportItemGroup = groupToReportItemGroup(groupIt.getValue());
			map.put(groupIt.getKey(), reportItemGroup);
		}
		
		return map;
	}
*/	
	
	/**
	 * Builds a ReportItemGroup from the group data. Only the peptides can't be
	 * set like this, they have to be scored first.
	 *  
	 * @param group
	 * @return
	 */
/*	private ReportItemGroup groupToReportItemGroup(Group group) {
		ReportItemGroup reportItem = new ReportItemGroup(-1L, 1L);
		
		// set the score to NaN, as invalid value
		reportItem.setScore(Float.NaN);
		
		// add the accessions to the group
		for (Map.Entry<String, Accession> accIt : group.getAccessions().entrySet()) {
			reportItem.addAccession(accIt.getValue());
		}
		
		// set the group
		reportItem.setGroup(group);
		
		// set children
		Vector<Group> children = new Vector<Group>();
		for (Map.Entry<Long, Group> childrenIt : group.getChildren().entrySet()) {
			children.add(childrenIt.getValue());
		}
		reportItem.setChildren(children);
		
		// set allChildren
		children = new Vector<Group>();
		for (Map.Entry<Long, Group> childrenIt : group.getAllChildren().entrySet()) {
			children.add(childrenIt.getValue());
		}
		reportItem.setAllChildren(children);
		
		return reportItem;
	}
*/	
	
	
	/**
	 * Calculates the score for all the groups in the map.
	 * 
	 * @param groups
	 * @param minPeptideScore
	 */
/*	private void calculateScore(Map<Long, ReportItemGroup> groups) {
		
		for (Map.Entry<Long, ReportItemGroup> groupIt : groups.entrySet()) {
			
			// calculate the score for groups, that have no calculated score yet
			if (Float.compare(groupIt.getValue().getScore(), Float.NaN) == 0) {
				calculateGroupScore(groupIt.getValue(), groups);
			}
			
		}
		
	}
*/	
	
	/**
	 * Calculates the group's score
	 * 
	 * @param group
	 */
/*	private void calculateGroupScore(ReportItemGroup group,
			Map<Long, ReportItemGroup> reportItemMap) {
		float score = Float.NaN;
		
		Vector<ReportPeptide> usedPeptides = new Vector<ReportPeptide>();	// the used peptides
		
		// first calculate children's score
		for (Group child : group.getChildren()) {
			ReportItemGroup reportChild = reportItemMap.get(child.getID());
			
			if (reportChild != null) {
				if (Float.compare(reportChild.getScore(), Float.NaN) == 0) {
					calculateGroupScore(reportChild, reportItemMap);
				}
				
				for (Map.Entry<String, ReportPeptide> pepIt : reportChild.getPeptides().entrySet()) {
					usedPeptides.add(pepIt.getValue().clone());
				}
			} else {
				System.out.println("child is not in report map, that's seriously wrong!");
			}
		}
		
		// then create the own peptides, calculate their scores and add them to the usedPeptides vector
		if (group.getGroup().getPeptides() != null) {
			for (Map.Entry<String, Peptide> pepIt : group.getGroup().getPeptides().entrySet()) {
				
				Float pepScore;
				Vector<PeptideSpectrumMatch> spectra = new Vector<PeptideSpectrumMatch>();
				Vector<PeptideSpectrumMatch> scoringSpectra = new Vector<PeptideSpectrumMatch>();
				
				pepScore = calculatePeptideScore(pepIt.getValue(), spectra, scoringSpectra);
				
				if (Float.compare(pepScore, Float.NaN) != 0) {
					ReportPeptide repPeptide = new ReportPeptide(pepScore,
							pepIt.getValue(), spectra, scoringSpectra);
					
					usedPeptides.add(repPeptide);
				}
			}
		}
		
		// sort the used peptides by score, highest score first
		Collections.sort(usedPeptides, Collections.reverseOrder(new ReportPeptideScoreComparator()));
		
		Vector<String> higherScoredPeptidesSpectra = new Vector<String>();
		Vector<ReportPeptide> validPeptides = new Vector<ReportPeptide>();
		
		for (ReportPeptide checkPep : usedPeptides) {
			
			List<PeptideSpectrumMatch> checkPepsSpectra = checkPep.getSpectra();
			long nrSpectra = checkPepsSpectra.size();
			long explainedSpectra = 0;
			
			// look, if the spectra are already in the higher scored peptides
			for (PeptideSpectrumMatch spectrum : checkPepsSpectra) {
				if (higherScoredPeptidesSpectra.contains(spectrum.getFileSourceID())) {
					explainedSpectra++;
				}
			}
			
			if (!((explainedSpectra > 0) && (explainedSpectra == nrSpectra))) {
				// not all spectra of this peptide are already in another peptide...
				validPeptides.add(checkPep);
				
				// add the spectra to used spectra
				for (PeptideSpectrumMatch spectrum : checkPepsSpectra) {
					if (!higherScoredPeptidesSpectra.contains(spectrum.getFileSourceID())) {
						higherScoredPeptidesSpectra.add(spectrum.getFileSourceID());
					}
				}
				
			}
			
		}
		usedPeptides = validPeptides;
		
		// calculate the score from all the used peptides
		for (ReportPeptide repPep : usedPeptides) {
			Float pepScore = repPep.getScore();
			
			if (Float.compare(score, Float.NaN) == 0) {
				score = pepScore;
			} else {
				score += (Float.compare(pepScore, Float.NaN) == 0) ?
						0 : pepScore;
			}
			
			group.addPeptide(repPep);
		}
		
		// and finally set the score
		group.setScore(score);
	}
*/	
	
	/**
	 * Calculate the peptide score. It takes for each modification of the
	 * peptide the spectrum with the highest score and adds up all the 
	 * scores for each modification.
	 */
/*	private Float calculatePeptideScore(Peptide peptide,
			Vector<PeptideSpectrumMatch> spectra,
			Vector<PeptideSpectrumMatch> scoringSpectra) {
		float peptideScore = 0;
		
		Trie<String, PeptideSpectrumMatch> scoringSpectrumSet = new PatriciaTrie<String, PeptideSpectrumMatch>(StringKeyAnalyzer.CHAR);	// maps the modifications to the spectra
		ScoreModel score, expectancy, modScore;
		boolean ok;
		
		// modifications need to be remodeled, because of same modifications on different positions may only count once
		String reMod;
		String regex = "\\(\\d+;";
		
		// get all the spectra of the peptide and fill the spectra vector
		for (PeptideSpectrumMatch spectrum : peptide.getSpectra()) {
			ok = true;
			
			score = spectrum.getScore(ScoreModelEnum.MASCOT_SCORE.getShortName());
			ok &= ((score != null) && (score.getValue() >= minPeptideScore)) || score.getValue().equals(Float.NaN); 
			
			expectancy = spectrum.getScore(ScoreModelEnum.MASCOT_SCORE.getShortName());
			ok &= ((expectancy != null) && (expectancy.getValue() <= maxExpValue)) || expectancy.getValue().equals(Float.NaN); 
			
			if (ok) {
				// put the spectras which count for the scoring into the map
				// TODO: check, if this works as before. if it does, use StringBuffer...
				//reMod = spectrum.getModificationString().replaceAll(regex, "(");
				reMod = "";
				for (Map.Entry<Integer, Modification> modIt : spectrum.getModifications().entrySet()) {
					reMod += modIt.getValue().toString();
				}
				modScore = (scoringSpectrumSet.containsKey(reMod)) ? scoringSpectrumSet.get(reMod).getScore(ScoreModelEnum.MASCOT_SCORE.getShortName()) : null;
				
				if (!scoringSpectrumSet.containsKey(reMod) ||
						(modScore.getValue() <= score.getValue())) {
					scoringSpectrumSet.put(reMod, spectrum);
				}
				
				// put all spectra, which are allowed to score, into the vector
				spectra.add(spectrum);
			}
		}
		
		// add up the modifications' score and fill the scoringSpectra vector
		for (Map.Entry<String, PeptideSpectrumMatch> spectrum : scoringSpectrumSet.entrySet()) {
			peptideScore += spectrum.getValue().getScore(ScoreModelEnum.MASCOT_SCORE.getShortName()).getValue();
			scoringSpectra.add(spectrum.getValue());
		}
		
		if (peptideScore >= minPeptideScore) {
			return peptideScore;
		} else {
			return Float.NaN;
		}
	}
*/	
	
	/**
	 * Counts the number of spectra in spectra used by the peptides of group,
	 * which are not in spectraDone.
	 * 
	 * @param spectra
	 * @param spectraDone
	 * @return
	 */
/*	private int countNewSpectra(ReportItemGroup group,
			Trie<String, Boolean> usedSpectra) {
		int count = 0;
		
		Trie<String, ReportPeptide> peptides = group.getPeptides();
		Vector<PeptideSpectrumMatch> peptidesSpectra;
		
		for (Map.Entry<String, ReportPeptide> pepIt : peptides.entrySet()) {
			
			peptidesSpectra = pepIt.getValue().getScoringSpectra();
			
			if (peptidesSpectra != null) {
				for (PeptideSpectrumMatch spectrum : peptidesSpectra) {
					if (!usedSpectra.containsKey(spectrum.getFileSourceID())) {
						count++;
					}
				}
			}
		}
		
		return count;
	}
*/	
	
	/**
	 * Builds a Vector of ReportGroup elements from the given scoreGroup vector
	 * of ReportItemGroup elements.
	 */
/*	private Vector<ReportItemGroup> reportItemsFromScoreGroup(Vector<ReportItemGroup> scoreGroup,
			Long reportRank, int IdOffset)
	{
		Vector<ReportItemGroup> items = new Vector<ReportItemGroup>();
		ReportItemGroup item;
		Vector<Integer> explainGroupIndizes;
		int explainGroupIdx;
		boolean explained;
		
		for (ReportItemGroup repItem : scoreGroup) {
			explained = false;
			explainGroupIdx = -1;
			explainGroupIndizes = reportItemSpectrallyExplainedByGroup(repItem, items);
			
			if (explainGroupIndizes.size() > 0) {
				
				for (Integer idx : explainGroupIndizes) {
					// check, if all peptides in group are in the explaining group
					explained = true;
					item = items.get(idx);
					
					for (Map.Entry<String, ReportPeptide> pepIt : repItem.getPeptides().entrySet()) {
						if (!item.getPeptides().containsKey(pepIt.getKey())) {
							explained = false;
							break;
						}
					}
					
					if (explained) {
						explainGroupIdx = idx;
					}
				}
			}
			
			if (explained) {
				// if all is explained, just add the accession
				for (Map.Entry<String, Accession> accIt : repItem.getAccessions().entrySet()) {
					items.get(explainGroupIdx).addAccession(accIt.getValue());
				}
			} else {
				// else, add a new item
				item = new ReportItemGroup(new Long(items.size()+IdOffset+1), reportRank);
				item.setGroup(repItem.getGroup());
				item.setChildren(repItem.getChildren());
				item.setAllChildren(repItem.getAllChildren());
				item.setScore(repItem.getScore());
				
				for (Map.Entry<String, Accession> accIt : repItem.getAccessions().entrySet()) {
					item.addAccession(accIt.getValue());
				}
				
				for (Map.Entry<String, ReportPeptide> pepIt : repItem.getPeptides().entrySet()) {
					item.addPeptide(pepIt.getValue());
				}
				
				items.add(item);
			}
		}
		
		return items;
	}
*/	
	
	/**
	 * Checks whether the group's spectra is fully explained by the spectra of
	 * one of the groups in the vector.
	 * 
	 * @param group
	 * @param reportItems
	 * @return the ID of the explaining group or -1, if none explains
	 */
/*	private Vector<Integer> reportItemSpectrallyExplainedByGroup(ReportItemGroup group,
			Vector<ReportItemGroup> reportItems) {
		Vector<PeptideSpectrumMatch> groupsSpectra = getGroupsSpectra(group);
		long nrSpectra = groupsSpectra.size();
		Vector<PeptideSpectrumMatch> compareSpectra;
		ReportItemGroup compareGroup;
		long explainedSpectra;
		boolean explained;
		int idx;
		Vector<Integer> idxVec = new Vector<Integer>();
		
		for (idx = 0; idx < reportItems.size(); idx++) {
			compareGroup = reportItems.get(idx);
			
			explainedSpectra = 0;
			compareSpectra = getGroupsSpectra(compareGroup);
			
			for (PeptideSpectrumMatch compareSpectrum : compareSpectra) {
				// look, if the spectrum's sourceID is also in the spectra of the group
				explained = false;
				
				for (PeptideSpectrumMatch spectrum : groupsSpectra) {
					if (spectrum.getFileSourceID().equals(compareSpectrum.getFileSourceID())) {
						explained = true;
					}
				}
				
				if (explained) {
					explainedSpectra++;
				}
			}
			
			if ((explainedSpectra > 0) && (explainedSpectra == nrSpectra)) {
				idxVec.add(idx);
			}
		}
		
		return idxVec;
	}
	
	
	private Vector<PeptideSpectrumMatch> getGroupsSpectra(ReportItemGroup group) {
		Vector<PeptideSpectrumMatch> groupsSpectra = new Vector<PeptideSpectrumMatch>();
		
		for (Map.Entry<String, ReportPeptide> pepIt : group.getPeptides().entrySet() ) {
			for (PeptideSpectrumMatch spectrum : pepIt.getValue().getSpectra()) {
				groupsSpectra.add(spectrum);
			}
		}
		
		return groupsSpectra;
	}
*/
}
