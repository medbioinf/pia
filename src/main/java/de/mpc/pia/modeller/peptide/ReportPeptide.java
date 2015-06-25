package de.mpc.pia.modeller.peptide;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.Filterable;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.comparator.Rankable;


/**
 * This class holds the information of a peptide, as it will be reported in the
 * {@link PeptideViewer}
 * 
 * @author julian
 *
 */
public class ReportPeptide implements Rankable, Filterable {
	
	/** identifier for the peptide*/
	private String stringID;
	
	/** the actual sequence of the peptide */
	private String sequence;
	
	/** the rank of this peptide (if calculated) */
	private Long rank;
	
	/** the PSMs in this peptide */
	private List<PSMReportItem> psmList;
	
	/** list of ReportPSM IDs, which do not score in this peptide */
	private Set<Long> nonScoringPSMIDs;
	
	/** list of spectrum Identification keys, which do not score in this peptide */
	private Set<String> nonScoringSpectraIDKeys;
	
	/** the intermediate peptide of this report peptide */
	private Peptide peptide;
	
	
	/**
	 * Basic constructor
	 * 
	 * @param sequence
	 * @param stringID
	 */
	public ReportPeptide(String sequence, String stringID, Peptide peptide) {
		this.sequence = sequence;
		this.stringID = stringID;
		this.peptide = peptide;
		rank = null;
		psmList = new ArrayList<PSMReportItem>();
		nonScoringPSMIDs = new HashSet<Long>();
		nonScoringSpectraIDKeys = new HashSet<String>();
	}
	
	
	/**
	 * Returns the identifier for this peptide.<br/>
	 * This would either simply be the sequence or the sequence and
	 * modification information.
	 * 
	 * @return
	 */
	public String getStringID() {
		return stringID;
	}
	
	
	/**
	 * Getter for the sequence.
	 * @return
	 */
	public String getSequence() {
		return sequence;
	}
	
	
	/**
	 * Returns the {@link Peptide} this {@link ReportPeptide} based on.
	 * @return
	 */
	public Peptide getPeptide() {
		return peptide;
	}
	
	
	@Override
	public void setRank(Long rank) {
		this.rank = rank;
	}
	
	
	@Override
	public Long getRank() {
		return rank;
	}
	
	
	/**
	 * Getter for the missed cleavages.<br/>
	 * Returns the missed cleavages of the first available PSM.<br/>
	 * WARNING: this may make no sense for an overview look, but is intended to
	 * be used for single files only! Different files may have different
	 * protease settings.
	 * 
	 * @return
	 */
	public int getMissedCleavages() {
		if (psmList.size() > 0) {
			return psmList.get(0).getMissedCleavages();
		}
		
		return -1;
	}
	
	
	/**
	 * Getter for the accessions
	 * 
	 * @return
	 */
	public List<Accession> getAccessions() {
		Set<String> accSet = new HashSet<String>();
		List<Accession> accList = new ArrayList<Accession>();
		
		for (PSMReportItem psm : psmList) {
			for (Accession acc : psm.getAccessions()) {
				if (!accSet.contains(acc.getAccession())) {
					accSet.add(acc.getAccession());
					accList.add(acc);
				}
			}
		}
		
		return accList;
	}
	
	
	/**
	 * Adds a single PSM or PSM set to the map of PSMs.
	 * 
	 * @param psm
	 */
	public void addPSM(PSMReportItem psm) {
		psmList.add(psm);
	}
	
	
	/**
	 * Adds the PSM with the given ID to the non scoring PSMs of this peptide.
	 * @param ID
	 */
	public void addToNonScoringPSMs(Long ID) {
		nonScoringPSMIDs.add(ID);
	}
	
	
	/**
	 * Removes the PSM with the given ID from the non scoring PSMs of this
	 * peptide.
	 * @param ID
	 * @return true, if the ID was in the set
	 */
	public boolean removeFromNonScoringPSMs(Long ID) {
		return nonScoringPSMIDs.remove(ID);
	}
	
	
	/**
	 * Returns the set of IDs from non scoring PSMs in this peptide.
	 * @return
	 */
	public Set<Long> getNonScoringPSMIDs() {
		return nonScoringPSMIDs;
	}
	
	
	/**
	 * Removes all elements from the non scoring PSMs set.
	 */
	public void clearNonScoringPSMIDs() {
		nonScoringPSMIDs.clear();
	}
	
	
	/**
	 * Adds the spectrum with the given idKey to the non scoring spectra of this
	 * peptide.
	 * @param idKey
	 */
	public void addToNonScoringSpectra(String idKey) {
		nonScoringSpectraIDKeys.add(idKey);
	}
	
	
	/**
	 * Removes the spectrum with the given idKey from the non scoring spectra of
	 * this peptide.
	 * 
	 * @param idKey
	 * @return true, if the idKey was in the set
	 */
	public boolean removeFromNonScoringSpectra(String idKey) {
		return nonScoringSpectraIDKeys.remove(idKey);
	}
	
	
	/**
	 * Returns the set of idKeys from non scoring spectra in this peptide.
	 * @return
	 */
	public Set<String> getNonScoringSpectraIDKeys() {
		return nonScoringSpectraIDKeys;
	}
	
	
	/**
	 * Removes all elements from the non scoring spectra set.
	 */
	public void clearNonScoringSpectraIDKeys() {
		nonScoringSpectraIDKeys.clear();
	}
	
	
	/**
	 * Removes the ReportPSMSet with the given key from the PSM List.
	 * 
	 * @param psmKey
	 * @return the removed PSM, or null if none is removed
	 */
	public ReportPSMSet removeReportPSMSet(ReportPSMSet remSet,
			Map<String, Boolean> psmSetSettings) {
		Iterator<PSMReportItem> psmIter = psmList.iterator();
		
		String remIdKey = remSet.getIdentificationKey(psmSetSettings);
		
		while (psmIter.hasNext()) {
			PSMReportItem psm = psmIter.next();
			
			if (psm instanceof ReportPSMSet) {
				if (remIdKey.equals(
						((ReportPSMSet) psm).
								getIdentificationKey(psmSetSettings))) {
					// the PSM is found, remove it from List and return it
					psmIter.remove();
					return (ReportPSMSet)psm;
				}
			}
		}
		
		return null;
	}
	
	
	/**
	 * Gets all the identification keys of the {@link ReportPSMSet}s in this
	 * peptide.
	 * 
	 * @return
	 */
	public List<String> getPSMsIdentificationKeys(
			Map<String, Boolean> psmSetSettings) {
		Set<String> idKeySet = new HashSet<String>();
		
		for (PSMReportItem psm : psmList) {
			idKeySet.add(psm.getIdentificationKey(psmSetSettings));
		}
		
		return new ArrayList<String>(idKeySet);
	}
	
	
	/**
	 * Gets all the fileNames of the PSMs.
	 * @return
	 */
	public List<String> getFileNames() {
		Set<String> fileNameSet = new HashSet<String>();
		
		for (PSMReportItem psm : psmList) {
			if (psm instanceof ReportPSM) {
				fileNameSet.add(((ReportPSM) psm).getFileName());
			} else if (psm instanceof ReportPSMSet) {
				for (ReportPSM repPSM : ((ReportPSMSet) psm).getPSMs()) {
					fileNameSet.add(repPSM.getFileName());
				}
			}
		}
		
		return new ArrayList<String>(fileNameSet);
	}
	
	
	/**
	 * Returns a list with the PSMs of this ReportPeptide.
	 * @return
	 */
	public List<PSMReportItem> getPSMs() {
		return new ArrayList<PSMReportItem>(psmList);
	}
	
	
	/**
	 * Gets the list of PSMs for the given psmKey (created by a PSM) from the
	 * PSMs map. This should be only one, either a ReportPSM or a ReportPSMSet,
	 * per peptide with the same key.
	 * 
	 * @return
	 */
	public List<PSMReportItem> getPSMsByIdentificationKey(String psmKey,
			Map<String, Boolean> psmSetSettings) {
		List<PSMReportItem> list = new ArrayList<PSMReportItem>();
		
		for (PSMReportItem psm : psmList) {
			if (psm.getIdentificationKey(psmSetSettings).equals(psmKey)) {
				list.add(psm);
			}
		}
		
		return list;
	}
	
	
	/**
	 * Gets the list of PSMs for the given spectrumKey. This may be an arbitrary
	 * number of PSMs or PSMSets.
	 * 
	 * @return
	 */
	public List<PSMReportItem> getPSMsBySpectrumIdentificationKey(
			String spectrumKey) {
		List<PSMReportItem> list = new ArrayList<PSMReportItem>();
		
		for (PSMReportItem psm : psmList) {
			if (psm instanceof ReportPSM) {
				if (((ReportPSM) psm).getSpectrum().
						getSpectrumIdentificationKey(
								psm.getAvailableIdentificationKeySettings()).
						equals(spectrumKey)) {
					list.add(psm);
				}
			} else if (psm instanceof ReportPSMSet) {
				
				for (ReportPSM reportPSM : ((ReportPSMSet) psm).getPSMs()) {
					if (reportPSM.getSpectrum().
							getSpectrumIdentificationKey(
									psm.getAvailableIdentificationKeySettings()).
							equals(spectrumKey)) {
						list.add(psm);
						// the PSMSet must be added only once
						break;
					}
				}
			}
		}
		
		return list;
	}
	
	
	/**
	 * Returns the best score value with the given name.
	 */
	@Override
	public Double getScore(String scoreName) {
		return getBestScore(scoreName);
	}
	
	
	/**
	 * Returns the best score of all the PSMs for the ScoreModel given by the
	 * scoreName.
	 *  
	 * @param scoreName
	 * @return
	 */
	public Double getBestScore(String scoreName) {
		ScoreModel bestScoreModel = getBestScoreModel(scoreName);
		
		return (bestScoreModel == null) ?
				Double.NaN :
				bestScoreModel.getValue();
	}
	
	
	/**
	 * Returns the ScoreModel with the best score value of all the PSMs for the
	 * ScoreModel given by the scoreName.
	 *  
	 * @param scoreName
	 * @return
	 */
	public ScoreModel getBestScoreModel(String scoreName) {
		ScoreModel bestScoreModel = null;
		
		// get the best of the scores out of the list
		for (PSMReportItem psm : psmList) {
			ScoreModel newScoreModel = null;
			
			if ((psm instanceof ReportPSM) &&
					!nonScoringPSMIDs.contains(((ReportPSM)psm).getId()) && 
					!nonScoringSpectraIDKeys.contains(((ReportPSM)psm).getSpectrum().getSpectrumIdentificationKey(psm.getAvailableIdentificationKeySettings()))) {
				newScoreModel = psm.getCompareScore(scoreName);
			} else if (psm instanceof ReportPSMSet) {
				newScoreModel =((ReportPSMSet)psm).getCompareScore(scoreName,
						nonScoringPSMIDs, nonScoringSpectraIDKeys);
			}
			
			if ((newScoreModel != null) &&
					((bestScoreModel == null) ||
						(newScoreModel.compareTo(bestScoreModel) < 0))) {
					bestScoreModel = newScoreModel;
			}
		}
		
		// no score found
		return bestScoreModel;
	}
	
	
	
	
	/**
	 * Returns the score, with which the comparison will be performed.
	 * For this, the ScoreModel given by the scoreShortName with the highest
	 * score of this peptide will be returned.
	 */
	@Override
	public ScoreModel getCompareScore(String scoreShortname) {
		return getBestScoreModel(scoreShortname);
	}
	
	
	/**
	 * Returns a List of all the {@link Modification}s occurring in the PSMs.
	 * 
	 * @return
	 */
	public List<Modification> getModificationsList() {
		List<Modification> modList = new ArrayList<Modification>();
		
		for (PSMReportItem psm : psmList) {
			for(Map.Entry<Integer, Modification> modIt : psm.getModifications().entrySet()) {
				modList.add(modIt.getValue());
			}
		}
		
		return modList;
	}
	
	
	/**
	 * Getter for the modifications.<br/>
	 * Returns the modifications of the first PSM.<br/>
	 * WARNING: this makes no sense, if the considerModifications is false in
	 * the {@link PeptideViewer}.
	 * 
	 * @return
	 */
	public Map<Integer, Modification> getModifications() {
		for (PSMReportItem psm : psmList) {
			return psm.getModifications();
		}
		
		return new HashMap<Integer, Modification>(1);
	}
	
	
	
	/**
	 * Generates the idString for a peptide for the given PSM.<br/>
	 * This takes the considerModifications into account and calls
	 * {@link ReportPeptide#getIdString(String, Map, boolean)}
	 * 
	 * @param psm
	 * @return
	 */
	public static String createStringID(PSMReportItem psm, boolean considerModifications) {
		return psm.getPeptideStringID(considerModifications);
	}
	
	
	/**
	 * returns a set containing the IDs of all the used Spectra, not only the
	 * scoring spectra.
	 * 
	 * @return
	 */
	public List<String> getSpectraIdentificationKeys() {
		Set<String> spectraKeySet = new HashSet<String>();
		
		for (PSMReportItem psm : psmList) {
			if (psm instanceof ReportPSM) {
				spectraKeySet.add(((ReportPSM) psm).getSpectrum().
						getSpectrumIdentificationKey(
								psm.getNotRedundantIdentificationKeySettings()));
			} else if (psm instanceof ReportPSMSet) {
				for (ReportPSM reportPSM : ((ReportPSMSet) psm).getPSMs()) {
					spectraKeySet.add(reportPSM.getSpectrum().
							getSpectrumIdentificationKey(
									psm.getNotRedundantIdentificationKeySettings()));
				}
			}
			
		}
		
		return new ArrayList<String>(spectraKeySet);
	}
	
	
	/**
	 * Getter for the number of spectra (not only scoring, but all).
	 * @return
	 */
	public Integer getNrSpectra() {
		return getSpectraIdentificationKeys().size();
	}
	
	
	/**
	 * returns a set containing the ID keys of the scoring Spectra.
	 * @return
	 */
	public List<String> getScoringSpectraIdentificationKeys() {
		Set<String> spectraKeySet = new HashSet<String>();
		
		for (PSMReportItem psm : psmList) {
			if (psm instanceof ReportPSM) {
				if (!nonScoringPSMIDs.contains(((ReportPSM) psm).getId())) {
					spectraKeySet.add(((ReportPSM) psm).getSpectrum().
							getSpectrumIdentificationKey(
									psm.getNotRedundantIdentificationKeySettings()));
				}
			} else if (psm instanceof ReportPSMSet) {
				for (ReportPSM reportPSM : ((ReportPSMSet) psm).getPSMs()) {
					if (!nonScoringPSMIDs.contains(reportPSM.getId())) {
						spectraKeySet.add(reportPSM.getSpectrum().
								getSpectrumIdentificationKey(
										psm.getNotRedundantIdentificationKeySettings()));
					}
				}
			}
		}
		
		// now remove the ID keys from the non scoring set
		spectraKeySet.removeAll(nonScoringSpectraIDKeys);
		return new ArrayList<String>(spectraKeySet);
	}
	
	
	/**
	 * Getter for the number of PSMs.
	 * @return
	 */
	public Integer getNrPSMs() {
		// the psmList should be consistent with the number of PSMs, as the
		// peptides are rebuild, if the psmSetSettings are changed
		return psmList.size();
	}
	
	
	/**
	 * Returns a List of all the sourceIDs in this peptide.
	 * @return
	 */
	public List<String> getSourceIDs() {
		Set<String> sourcesSet = new HashSet<String>();
		
		for (PSMReportItem psm : psmList) {
			if (psm.getSourceID() != null) {
				sourcesSet.add(psm.getSourceID());
			}
		}
		
		return new ArrayList<String>(sourcesSet);
	}
	
	
	/**
	 * Returns a List of all the spectrum titles in this peptide.
	 * @return
	 */
	public List<String> getSpectrumTitles() {
		Set<String> titlesSet = new HashSet<String>();
		
		for (PSMReportItem psm : psmList) {
			if (psm.getSpectrumTitle() != null) {
				titlesSet.add(psm.getSpectrumTitle());
			}
		}
		
		return new ArrayList<String>(titlesSet);
	}
	
	
	/**
	 * Returns a nice name / header for the spectrum in the PSM or PSM set
	 * specified by the spectrumKey.
	 * 
	 * @param spectrumKey
	 * @return
	 */
	public String getNiceSpectrumName(String spectrumKey) {
		String name = null;
		List<PSMReportItem> psms =
				getPSMsBySpectrumIdentificationKey(spectrumKey);
		for (PSMReportItem psm : psms) {
			name = psm.getNiceSpectrumName();
		}
		
		if ((psms.size() > 1) && (name != null)) {
			name += " [" + psms.size() + "]";
		}
		
		return name;
	}
}
