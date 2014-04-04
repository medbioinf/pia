package de.mpc.pia.webgui.wizard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.inference.AbstractProteinInference;
import de.mpc.pia.modeller.protein.inference.ProteinInferenceFactory;
import de.mpc.pia.modeller.protein.inference.ProteinInferenceFactory.ProteinInferenceMethod;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.protein.scoring.ProteinScoringFactory;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.psm.PSMScoreFilter;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;

public class Wizard {
	
	/** the used PIA modeller */
	private PIAModeller modeller;
	
	/** the currently active accordion tab*/
	private String activePSMFDRAccordionTab;
	
	
	/** the FDR threshold to use for PSM and peptide statistics */
	private Double fdrThreshold;
	
	
	/** the selected decoy strategies for the individual files */
	private Map<Long, String> decoyStrategies;
	
	/** the selected decoy patterns for the individual files */
	private Map<Long, String> decoyPatterns;
	
	/** the number of top identifications to use for the individual files */
	private Map<Long, Integer> fdrTopIdentifications;
	
	/** the list of possible FDR scores for the individual files */
	private Map<Long, List<ScoreModel>> possibleFDRScores;
	
	/** the ordered list of preferred FDR scores for all files settings */
	private List<String> preferredFDRScores;
	
	/** the chosen scores for FDR calculation for individual file settings */
	private Map<Long, String> selectedFDRScores;
	
	
	/** this map holds the maximum number of peptides possible with the chosen value for considerModifications */
	private Map<Long, Integer> maximumPeptides;
	
	/** this map holds the number of peptides with the chosen value for considerModifications and an FDR filter */
	private Map<Long, Integer> nrPeptides;
	
	/** this map holds the number of identifications per peptide for each file */
	private Map<Long, List<Integer>> nrPeptideIdentifications;
	
	/** this map holds the labels for the {@link #nrPeptideIdentifications} */
	private Map<Long, List<Integer>> nrPeptideIdentificationsLabels;
	
	
	/** this represents the selected protein inference's shortName */
	private String inferenceMethod;
	
	/** this represents the selected score for protein inference */
	private String inferenceScore;
	
	/** the actual running or used Protein Inference */
	private AbstractProteinInference proteinInference;
	
	/** whether the current inference is running */
	private boolean inferenceRunning;
	
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(Wizard.class);
	
	
	/**
	 * Basic constructor, which initializes the Wizard to start the first step.
	 * @param modeller
	 */
	public Wizard(PIAModeller modeller) {
		this.modeller = modeller;
		
		proteinInference = null;
		inferenceRunning = false;
		
		startPSMsFDRStep();
	}
	
	
	/**
	 * Getter for the modeller
	 * @return
	 */
	public PIAModeller getModeller() {
		return modeller;
	}
	
	
	/**
	 * Initializes everything for the PSM FDR calculation step.
	 */
	public void startPSMsFDRStep() {
		activePSMFDRAccordionTab = "all_files";
		
		fdrThreshold = modeller.getPSMModeller().getDefaultFDRThreshold();
				
		decoyStrategies = new HashMap<Long, String>();
		decoyPatterns = new HashMap<Long, String>();
		fdrTopIdentifications = new HashMap<Long, Integer>();
		possibleFDRScores = new HashMap<Long, List<ScoreModel>>();
		selectedFDRScores = new HashMap<Long, String>();
		
		for (PIAInputFile file
				: modeller.getPSMModeller().getFiles().values()) {
			decoyPatterns.put(file.getID(),
					modeller.getPSMModeller().getDefaultDecoyPattern());
			
			fdrTopIdentifications.put(file.getID(), 1);
			
			if (file.getID().equals(0L)) {
				// set the parameters for all 
				if (getAllowAllFilesStrategySearchengine()) {
					decoyStrategies.put(0L,
							FDRData.DecoyStrategy.SEARCHENGINE.toString());
				} else {
					decoyStrategies.put(0L,
							FDRData.DecoyStrategy.ACCESSIONPATTERN.toString());
				}
				
				List<ScoreModel> possible = new ArrayList<ScoreModel>();
				possibleFDRScores.put(0L, possible);
				preferredFDRScores = new ArrayList<String>();
				for (Long fileID : modeller.getFiles().keySet()) {
					for (String scoreShort : 
						modeller.getPSMModeller().getFilesAvailableScoreShortsForFDR(fileID)) {
						
						ScoreModelEnum modelType =
								ScoreModelEnum.getModelByDescription(scoreShort);
						ScoreModel model;
						
						if (modelType.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
							model = new ScoreModel(0.0, scoreShort,
									modeller.getPSMModeller().getScoreName(scoreShort));
						} else {
							model = new ScoreModel(0.0, modelType);
						}
						
						if (!possible.contains(model)) {
							possible.add(model);
							if (modelType.isSearchengineMainScore()) {
								preferredFDRScores.add(model.getShortName());
							}
						}
					}
				}
			} else  {
				if (allowStrategySearchengine(file.getID())) {
					decoyStrategies.put(file.getID(),
							FDRData.DecoyStrategy.SEARCHENGINE.toString());
				} else {
					decoyStrategies.put(file.getID(),
							FDRData.DecoyStrategy.ACCESSIONPATTERN.toString());
				}
				
				List<ScoreModel> possible = new ArrayList<ScoreModel>();
				possibleFDRScores.put(file.getID(), possible);
				for (String scoreShort : 
					modeller.getPSMModeller().getFilesAvailableScoreShortsForFDR(file.getID())) {
					
					ScoreModelEnum modelType =
							ScoreModelEnum.getModelByDescription(scoreShort);
					ScoreModel model;
					
					if (modelType.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
						model = new ScoreModel(0.0, scoreShort,
								modeller.getPSMModeller().getScoreName(scoreShort));
					} else {
						model = new ScoreModel(0.0, modelType);
					}
					
					if (!possible.contains(model)) {
						possible.add(model);
					}
					
					if (selectedFDRScores.get(file.getID()) == null) {
						// make the first possible the selected value
						selectedFDRScores.put(file.getID(),
								model.getShortName());
					} else if (modelType.isSearchengineMainScore()) {
						// if another is the main score, take that one
						selectedFDRScores.put(file.getID(),
								model.getShortName());
					}
				}
			}
		}
	}
	
	
	/**
	 * Getter for whether PSM sets should be created.
	 * 
	 * @return
	 */
	public Boolean getCreatePSMSets() {
		return this.modeller.getCreatePSMSets();
	}
	
	
	/**
	 * Setter for whether PSM sets should be created.
	 * 
	 * @return
	 */
	public void setCreatePSMSets(Boolean create) {
		modeller.getPSMModeller().applyGeneralSettings(create,
				modeller.getPSMModeller().getPSMSetSettings());
	}
	
	
	/**
	 * Getter for the used FDR threshold
	 * @return
	 */
	public Double getFDRThreshold() {
		return fdrThreshold;
	}
	
	
	/**
	 * Setter for the used FDR threshold
	 * @return
	 */
	public void setFDRThreshold(Double thr) {
		this.fdrThreshold = thr;
	}
	
	
	/**
	 * Returns whether the "searchengine" strategy is allowed for the given
	 * file or not.
	 * 
	 * @param fileID
	 * @return
	 */
	public boolean allowStrategySearchengine(Long fileID) {
		return modeller.getPSMModeller().getFileHasInternalDecoy(fileID);
	}
	
	
	/**
	 * Returns whether it is allowed to select "by searchengine" as decoy
	 * strategy for all files.
	 * 
	 * @return
	 */
	public boolean getAllowAllFilesStrategySearchengine() {
		for (Long fileID : modeller.getFiles().keySet()) {
			if (!allowStrategySearchengine(fileID)) {
				return false;
			}
		}
		
		return true;
	}
	
	
	/**
	 * Getter for the currently active accordion tab
	 * @return
	 */
	public String getActivePSMFDRAccordionTab() {
		return activePSMFDRAccordionTab;
	}
	
	
	/**
	 * Setter for the currently active accordion tab
	 * @return
	 */
	public void setActivePSMFDRAccordionTab(String tabName) {
		this.activePSMFDRAccordionTab = tabName;
	}
	
	
	/**
	 * Getter for the decoy strategy for all files
	 * @return
	 */
	public String getAllFilesDecoyStrategy() {
		return decoyStrategies.get(0L);
	}
	
	
	/**
	 * Setter for the decoy strategy for all files
	 * @return
	 */
	public void setAllFilesDecoyStrategy(String strategy) {
		decoyStrategies.put(0L, strategy);
	}
	
	
	/**
	 * Getter for the decoy pattern for all files
	 * @return
	 */
	public String getAllFilesDecoyPattern() {
		return decoyPatterns.get(0L);
	}
	
	
	/**
	 * Setter for the decoy pattern for all files
	 * @return
	 */
	public void setAllFilesDecoyPattern(String pattern) {
		decoyPatterns.put(0L, pattern);
	}
	
	
	/**
	 * Getter for the number of top identifications to use for all files
	 * @return
	 */
	public Integer getAllFilesFDRTopIdentifications() {
		return fdrTopIdentifications.get(0L);
	}
	
	
	/**
	 * Setter for the number of top identifications to use for all files
	 * @return
	 */
	public void setAllFilesFDRTopIdentifications(Integer top) {
		fdrTopIdentifications.put(0L, top);
	}
	
	
	/**
	 * Getter for all possible scores for FDR calculation for all files
	 * @return
	 */
	public List<ScoreModel> getAllFilesPossibleFDRScores() {
		return possibleFDRScores.get(0L);
	}
	
	
	/**
	 * Getter for the preferred scores for FDR calculation for all files
	 * @return
	 */
	public List<String> getAllFilesPreferredFDRScores() {
		
		logger.debug("getAllFilesPreferredFDRScores " + preferredFDRScores);
		
		return preferredFDRScores;
	}
	
	
	/**
	 * Setter for the preferred scores for FDR calculation for all files
	 * @return
	 */
	public void setAllFilesPreferredFDRScores(List<String> scores) {
		preferredFDRScores = scores;
		
		logger.debug("setAllFilesPreferredFDRScores " + preferredFDRScores);
	}
	
	
	/**
	 * Getter for the decoy strategies for for the individual files
	 * @return
	 */
	public Map<Long, String> getDecoyStrategies() {
		return decoyStrategies;
	}
	
	
	/**
	 * Setter for the decoy strategies for for the individual files
	 * @return
	 */
	public void setDecoyStrategies(Map<Long, String> strategies) {
		this.decoyStrategies = strategies;
	}
	
	
	/**
	 * Getter for the decoy patterns for for the individual files
	 * @return
	 */
	public Map<Long, String> getDecoyPatterns() {
		return decoyPatterns;
	}
	
	
	/**
	 * Setter for the decoy patterns for for the individual files
	 * @return
	 */
	public void setDecoyPatterns(Map<Long, String> patterns) {
		this.decoyPatterns = patterns;
	}
	
	
	/**
	 * Getter for the number of top identifications to use for individual files
	 * @return
	 */
	public Map<Long, Integer> getFDRTopIdentifications() {
		return fdrTopIdentifications;
	}
	
	
	/**
	 * Setter for the number of top identifications to use for individual files
	 * @return
	 */
	public void setFDRTopIdentifications(Map<Long, Integer> topList) {
		fdrTopIdentifications = topList;
	}
	
	
	/**
	 * Getter for all possible scores for FDR calculation for individual files
	 * @return
	 */
	public Map<Long, List<ScoreModel>> getPossibleFDRScores() {
		return possibleFDRScores;
	}
	
	
	/**
	 * Getter for the selected scores for FDR calculation for individual files
	 * @return
	 */
	public Map<Long,String> getSelectedFDRScores() {
		return selectedFDRScores;
	}
	
	
	/**
	 * Setter for the selected scores for FDR calculation for individual files
	 * @return
	 */
	public void setSelectedFDRScores(Map<Long, String> scores) {
		this.selectedFDRScores = scores;
	}
	
	
	/**
	 * Finishes the FDR calculation for PSMs step and starts the FDR calculation
	 * and afterwards the combined FDR calculation.
	 */
	public void finishPSMsFDRStep() {
		if (activePSMFDRAccordionTab.equals("all_files")) {
			// set the decoy strategy and pattern
			if (getAllFilesDecoyStrategy().equals(
					FDRData.DecoyStrategy.SEARCHENGINE.toString())) {
				// set the strategy to searchengine
				modeller.getPSMModeller().setAllDecoyPattern(
						getAllFilesDecoyStrategy());
			} else {
				modeller.getPSMModeller().setAllDecoyPattern(
						getAllFilesDecoyPattern());
			}
			
			for (FDRData fdrData 
					: modeller.getPSMModeller().getFileFDRData().values()) {
				fdrData.setFDRThreshold(fdrThreshold);
			}
			
			// set the top identifications
			modeller.getPSMModeller().setAllTopIdentifications(
					getAllFilesFDRTopIdentifications());
			
			// set the prefered scores for FDR calculation
			modeller.getPSMModeller().resetPreferredFDRScores();
			
			for (String scoreShort : getAllFilesPreferredFDRScores()) {
				modeller.getPSMModeller().addPreferredFDRScore(
						scoreShort);
			}
		} else if (activePSMFDRAccordionTab.equals("per_file")) {
			for (Long fileID : modeller.getFiles().keySet()) {
				modeller.getPSMModeller().updateFilesFDRData(fileID,
						DecoyStrategy.getStrategyByString(decoyStrategies.get(fileID)),
						decoyPatterns.get(fileID),
						fdrThreshold,
						selectedFDRScores.get(fileID),
						fdrTopIdentifications.get(fileID));
			}
		}
		
		modeller.getPSMModeller().calculateAllFDR();
		if (getCreatePSMSets()) {
			modeller.getPSMModeller().calculateCombinedFDRScore();
		}
	}
	
	
	/**
	 * Finishes the PSM review and thus gets some numbers for peptides.
	 */
	public void finishPSMsReviewStep() {
		logger.debug("finishPSMsReviewStep");
		
		Map<Long, List<ReportPeptide>> reportPeptideLists =
				new HashMap<Long, List<ReportPeptide>>();
		
		// get the maximal peptide numbers
		modeller.getPeptideModeller().removeAllFilters();
		
		maximumPeptides = new HashMap<Long, Integer>();
		reportPeptideLists.put(0L,
				modeller.getPeptideModeller().getFilteredReportPeptides(0L, null));
		maximumPeptides.put(0L, reportPeptideLists.get(0L).size());
		
		for (Long fileID : modeller.getFiles().keySet()) {
			reportPeptideLists.put(fileID,
					modeller.getPeptideModeller().getFilteredReportPeptides(fileID, null));
			maximumPeptides.put(fileID, reportPeptideLists.get(fileID).size());
		}
		
		// now set some FDR filters and infer the peptides again
		nrPeptides = new HashMap<Long, Integer>();
		List<AbstractFilter> filters = new ArrayList<AbstractFilter>(1);
		if (modeller.getPSMModeller().isCombinedFDRScoreCalculated()) {
			filters.add(new PSMScoreFilter(
					FilterComparator.less_equal,
					fdrThreshold,
					false,
					ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));
			
			modeller.getPeptideModeller().addFilter(0L, filters.get(0));
			
			
			reportPeptideLists.put(0L,
					modeller.getPeptideModeller().
							getFilteredReportPeptides(0L,filters));
			nrPeptides.put(0L, reportPeptideLists.get(0L).size());
		}
		
		filters.clear();
		filters.add(new PSMScoreFilter(
				FilterComparator.less_equal,
				fdrThreshold,
				false,
				ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName()));
		
		for (Long fileID : modeller.getFiles().keySet()) {
			if (modeller.getPSMModeller().isFDRCalculated(fileID)) {
				
				modeller.getPeptideModeller().addFilter(fileID, filters.get(0));
				
				reportPeptideLists.put(fileID,
						modeller.getPeptideModeller().
								getFilteredReportPeptides(fileID, filters));
				nrPeptides.put(fileID, reportPeptideLists.get(fileID).size());
			}
		}
		
		
		// get the number of identifications per peptide (either of filtered or normal)
		nrPeptideIdentifications = new HashMap<Long, List<Integer>>();
		nrPeptideIdentificationsLabels = new HashMap<Long, List<Integer>>();
		for (Map.Entry<Long, List<ReportPeptide>> reportListIt
				: reportPeptideLists.entrySet()) {
			Map<Integer, Integer> idMap = new HashMap<Integer, Integer>();
			for (ReportPeptide peptide : reportListIt.getValue()) {
				int ids =  peptide.getPSMs().size();
				if (!idMap.containsKey(ids)) {
					idMap.put(ids, 0);
				}
				idMap.put(ids, idMap.get(ids) + 1);
			}
			
			List<Integer> labels = new ArrayList<Integer>(idMap.keySet());
			Collections.sort(labels);
			
			List<Integer> ids = new ArrayList<Integer>(labels.size());
			for (Integer label : labels) {
				ids.add(idMap.get(label));
			}
			
			nrPeptideIdentifications.put(reportListIt.getKey(), ids);
			nrPeptideIdentificationsLabels.put(reportListIt.getKey(), labels);
		}
	}
	
	
	/**
	 * Getter for whether modifications should be considered to differentiate
	 * peptides.
	 * 
	 * @return
	 */
	public Boolean getConsiderModifications() {
		return this.modeller.getConsiderModifications();
	}
	
	
	/**
	 * Setter for whether modifications should be considered to differentiate
	 * peptides.
	 * 
	 * @return
	 */
	public void setConsiderModifications(Boolean consider) {
		modeller.getPeptideModeller().applyGeneralSettings(consider);
	}
	
	
	/**
	 * Gets the maximal number of peptides for each file.
	 * @return
	 */
	public Map<Long, Integer> getMaximalNrPeptides() {
		return maximumPeptides;
	}
	
	
	/**
	 * Gets the number of peptides for each file with FDR filter.
	 * @return
	 */
	public Map<Long, Integer> getNrPeptides() {
		return nrPeptides;
	}
	
	
	/**
	 * Gets the number of peptide identifications for each file
	 * @return
	 */
	public Map<Long, List<Integer>> getNrPeptideIdentifications() {
		return nrPeptideIdentifications;
	}
	
	
	/**
	 * Gets the labels for the number of peptide identifications for each file
	 * @return
	 */
	public Map<Long, List<Integer>> getNrPeptideIdentificationsLabel() {
		return nrPeptideIdentificationsLabels;
	}
	
	
	/**
	 * 
	 */
	public void startProteinInferenceStep() {
		inferenceMethod =
				ProteinInferenceMethod.REPORT_SPECTRUM_EXTRACTOR.getShortName();
		
		if (modeller.getPSMModeller().isCombinedFDRScoreCalculated() &&
				getCreatePSMSets()) {
			// combined FDR score was calculated and sets are build, use the score preferable
			inferenceScore = ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName();
		} else if (
				modeller.getProteinModeller().getAllScoreShortNames().contains(
						ScoreModelEnum.PSM_LEVEL_FDR_SCORE)) {
			// at least the FDR Score is calculated, prefer this
			inferenceScore = ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName();
		} else {
			// then just take the first available score
			inferenceScore =
					modeller.getProteinModeller().getAllScoreShortNames().get(0);
		}
		
		logger.debug("startProteinInferenceStep " + 
				"\n\t" + inferenceMethod +
				"\n\t" + inferenceScore);
	}
	
	
	/**
	 * Getter for the selected inference method
	 * @return
	 */
	public String getInferenceMethod() {
		logger.debug("getInferenceMethod " + inferenceMethod);
		return inferenceMethod;
	}
	
	
	/**
	 * Setter for the selected inference method
	 * @return
	 */
	public void setInferenceMethod(String method) {
		this.inferenceMethod = method;
		logger.debug("setInferenceMethod " + inferenceMethod);
	}
	
	
	/**
	 * Getter for the possible inference methods
	 * @return
	 */
	public List<ProteinInferenceMethod> getPossibleInferenceMethods() {
		List<ProteinInferenceMethod> methods =
				new ArrayList<ProteinInferenceMethod>();
		Collections.addAll(methods, ProteinInferenceMethod.values());
		return methods;
	}
	
	
	/**
	 * Getter for the selected inference score
	 * @return
	 */
	public String getInferenceScore() {
		logger.debug("getInferenceScore " + inferenceScore);
		return inferenceScore;
	}
	
	
	/**
	 * Setter for the selected inference score
	 * @return
	 */
	public void setInferenceScore(String score) {
		this.inferenceScore = score;
		logger.debug("setInferenceScore " + inferenceScore);
	}
	
	
	/**
	 * Getter for the possible inference scores
	 * @return
	 */
	public List<String> getPossibleInferenceScores() {
		return modeller.getProteinModeller().getAllScoreShortNames();
	}
	
	
	/**
	 * Getter for the scoreName, given the scoreShort.
	 * @param scoreShort
	 * @return
	 */
	public String getScoreName(String scoreShort) {
		return modeller.getPSMModeller().getScoreName(scoreShort);
	}
	
	
	/**
	 * Use the given data, guess some defaults and infer the proteins.
	 */
	public void infereProteins() {
		inferenceRunning = true;
		
		proteinInference =
				ProteinInferenceFactory.createInstanceOf(inferenceMethod);
		
		if (inferenceScore.equals(ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName())) {
			// combined FDR score was calculated and sets are build, add a filter
			proteinInference.addFilter(
					new PSMScoreFilter(
							FilterComparator.less_equal,
							getFDRThreshold(),
							false,
							ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));
		} else if (inferenceScore.equals(ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName())) {
			// at least the FDR Score is calculated, so filter for it
			proteinInference.addFilter(
					new PSMScoreFilter(
							FilterComparator.less_equal,
							getFDRThreshold(),
							false,
							ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName()));
		}
		
		// set the scoring
		AbstractScoring scoring;
		/*ScoreModelEnum scoreModel =
				ScoreModelEnum.getModelByDescription(inferenceScore);*/
		
		if (modeller.getPSMModeller().getHigherScoreBetterForScore(inferenceScore)) {
			scoring = ProteinScoringFactory.getNewInstanceByName(
					ProteinScoringFactory.ScoringType.ADDITIVE_SCORING.getShortName(),
					new HashMap<String, String>());
		} else {
			scoring = ProteinScoringFactory.getNewInstanceByName(
					ProteinScoringFactory.ScoringType.MULTIPLICATIVE_SCORING.getShortName(),
					new HashMap<String, String>());
		}
		
		// set the scoring settings
		scoring.setSetting("used_score", inferenceScore);
		scoring.setSetting("used_spectra", PSMForScoring.ONLY_BEST.getShortName());
		
		proteinInference.setScoring(scoring);
		
		startInference();
	}
	
	
	/**
	 * Starts the inference in another thread, not waiting on it to finish.<br/>
	 * If a blocking method is needed, use {@link #infereProteins()}.
	 * @return
	 */
	public String startInference() {
		inferenceRunning = true;
		
		Thread inferenceRunnerThread = new Thread() {
			@Override
			public void run() {
				modeller.getProteinModeller().infereProteins(proteinInference);
				inferenceRunning = false;
			}
		};
		inferenceRunnerThread.start();
		
		return null;
	}
	
	
	/**
	 * Returns true, if the inference is running.
	 * @return
	 */
	public Boolean getInferenceRunning() {
		return inferenceRunning;
	}
	
	
	/**
	 * Returns the progress of the inference.
	 * @return
	 */
	public Long getInferenceProgress() {
		Long progress;
		
		if (inferenceRunning) {
			if (proteinInference == null) {
				// probably not yet initialized
				progress = Long.valueOf(0);
			} else {
				progress = proteinInference.getProgressValue();
			}
		} else {
			if (proteinInference == null) {
				progress = Long.valueOf(-1);
			} else {
				progress = Long.valueOf(101);
			}
		}
		
		return progress;
	}
}
