package de.mpc.pia.webgui.psmviewer.component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import de.mpc.pia.modeller.PSMModeller;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;
import de.mpc.pia.webgui.psmviewer.PSMViewer;


/**
 * This little helper class builds the Panel for the FDR settings used by
 * the PSM Viewer.
 * 
 * @author julian
 *
 */
public class PSMViewerFDRPanel {
	
	/** the {@link PSMModeller} of the {@link PSMViewer}*/
	private PSMModeller psmModeller;
	
	/** the number of the current file */
	private Long fileNumber;
	
	
	/** the used decoy strategy shown and set via the form */
	private String formDecoyStrategy;
	
	/** the decoy pattern shown and set via the form */
	private String formDecoyPattern;
	
	/** the fdr threshold shown and set via the form */
	private Double formFDRThreshold;
	
	/** the topIdentifications used for the FDR shown and set via the form */
	private Integer formFDRTopIdentifications;
	
	/** the FDR score model shown and set via the form */
	private String formFDRScoreModelShort; 
	
	/** whether the file with the given ID needs a recaching of the report, null means new filtering is needed. */
	private Map<Long, Boolean> fileNeedsRecaching;
	
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(PSMViewerFDRPanel.class);
	
	
	
	/**
	 * basic constructor
	 * @param modeller
	 */
	public PSMViewerFDRPanel(PSMModeller modeller) {
		this.psmModeller = modeller;
		
		fileNumber = 0L;
		updateFDRSettingsPanel(fileNumber);
		
		fileNeedsRecaching = new HashMap<Long, Boolean>();
	}
	
	
	/**
	 * Updates the data for the panel with date for the given file.
	 * 
	 * @return
	 */
	public void updateFDRSettingsPanel(Long fileNumber) {
		this.fileNumber = fileNumber;
		
		// reset all the form data to the ones given by the file's FDRData
		FDRData fdrData = psmModeller.getFilesFDRData(fileNumber);
		
		formDecoyPattern = fdrData.getDecoyPattern();
		formFDRThreshold = fdrData.getFDRThreshold();
		formFDRTopIdentifications =
				psmModeller.getFilesTopIdentifications(fileNumber);
		formDecoyStrategy = fdrData.getDecoyStrategy().toString();
		
		if (fdrData.getScoreShortName() != null) {
			formFDRScoreModelShort = fdrData.getScoreShortName();
		} else {
			List<String> scores = 
					psmModeller.getFilesAvailableScoreShortsForFDR(fileNumber);
			if (scores.size() > 0) {
				formFDRScoreModelShort = scores.get(0);
			} else {
				formFDRScoreModelShort = ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName();
			}
		}
	}
	
	
	/**
	 * Getter for the name of the current file.
	 * 
	 * @return
	 */
	public String getName() {
		return getName(fileNumber);
	}
	
	
	/**
	 * Getter for the name of the file, specified by the file ID.
	 * 
	 * @param fileId
	 * @return
	 */
	public String getName(Long fileId) {
		String name = psmModeller.getFiles().get(fileId).getName();
		
		if (name == null) {
			name = psmModeller.getFiles().get(fileId).getFileName();
		}
		
		return name;
	}
	
	
	public Long getFileNumber() {
		return fileNumber;
	}
	
	
	public List<String> getFDRScoreShortNames() {
		return psmModeller.getFilesAvailableScoreShortsForFDR(fileNumber);
	}
	
	
	public FDRData getFDRData() {
		return psmModeller.getFilesFDRData(fileNumber);
	}
	
	
	public Integer getTopIdentifications() {
		return psmModeller.getFilesTopIdentifications(fileNumber);
	}
	
	
	public Map<String, String> getFileFDRs() {
		Map<String, String> namesToScores = new HashMap<String, String>();
		
		for (Map.Entry<Long, String> idToName
				: psmModeller.getFileIDsToScoreOfFDRCalculation().entrySet()) {
			
			namesToScores.put(getName(idToName.getKey()), idToName.getValue());
			
		}
		
		return namesToScores;
	}
	
	
	public Boolean getAllFilesHaveFDRCalculated() {
		return psmModeller.getAllFilesHaveFDRCalculated();
	}
	
	
	/**
	 * getter for decoyStrategy.
	 * @return
	 */
	public String getDecoyStrategy() {
		return formDecoyStrategy;
	}
	
	
	/**
	 * setter for decoyStrategy.
	 * @return
	 */
	public void setDecoyStrategy(String strategy) {
		formDecoyStrategy = strategy;
	}
	
	
	/**
	 * getter for decoyPattern.
	 * @return
	 */
	public String getDecoyPattern() {
		return formDecoyPattern;
	}
	
	
	/**
	 * setter for decoyPattern.
	 * @return
	 */
	public void setDecoyPattern(String pattern) {
		formDecoyPattern = pattern;
	}
	
	
	/**
	 * Returns, whether the current file has internal decoys, i.e. PSMs which
	 * are set to be decoys in the PIA XML file.
	 * 
	 * @return
	 */
	public Boolean getHasInternalDecoy() {
		return psmModeller.getFileHasInternalDecoy(fileNumber);
	}
	
	
	/**
	 * getter for fdrThreshold.
	 * @return
	 */
	public Double getFDRThreshold() {
		return formFDRThreshold;
	}
	
	
	/**
	 * setter for fdrThreshold.
	 * @return
	 */
	public void setFDRThreshold(Double thr) {
		formFDRThreshold = thr;
	}

	
	/**
	 * getter for formFDRTopIdentifications.
	 * @return
	 */
	public Integer getFDRTopIdentifications() {
		return formFDRTopIdentifications;
	}
	
	
	/**
	 * setter for topIdentifications used in FDR calculation.
	 * @return
	 */
	public void setFDRTopIdentifications(Integer top) {
		formFDRTopIdentifications = top;
	}
	
	
	/**
	 * getter for fdrScoreModel.
	 * @return
	 */
	public String getModelForFDRCalculation() {
		FDRData fdrData = psmModeller.getFilesFDRData(fileNumber);
		
		if ((fdrData != null) && (fdrData.getScoreShortName() != null)) {
			return fdrData.getScoreShortName();
		} else {
			if (formFDRScoreModelShort == null) {
				if (fileNumber != 0L) {
					return getFDRScoreShortNames().get(0);
				} else {
					return ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName();
				}
			} else {
				return formFDRScoreModelShort;
			}
		}
	}
	
	
	/**
	 * setter for fdrScoreModel.
	 * @return
	 */
	public void setModelForFDRCalculation(String score) {
		formFDRScoreModelShort = score;
	}
	
	
	/**
	 * Updates the decoy strategy. If it changes since the last FDR calculation,
	 * it will be reseted.
	 */
	public void updateDecoyStrategy() {
		System.err.println("updateDecoyStrategy");
		System.err.println("\tformDecoyStrategy " + formDecoyStrategy);
		
		// compare the used strategy for FDRData and reset (if necessary)
		psmModeller.updateFilesFDRData(fileNumber,
				DecoyStrategy.getStrategyByString(formDecoyStrategy),
				formDecoyPattern, formFDRThreshold, formFDRScoreModelShort,
				formFDRTopIdentifications);
		
		psmModeller.updateDecoyStates(fileNumber);
		
		fileNeedsRecaching.put(fileNumber, true);
	}
	
	
	/**
	 * Calculate the FDR with the given parameters.
	 */
	public void calculateFDR() {
		logger.info("calculateFDR " + fileNumber);
		
		psmModeller.updateFilesFDRData(fileNumber,
				DecoyStrategy.getStrategyByString(formDecoyStrategy),
				formDecoyPattern, formFDRThreshold, formFDRScoreModelShort,
				formFDRTopIdentifications);
		
		psmModeller.calculateFDR(fileNumber);
		
		fileNeedsRecaching.put(fileNumber, true);
	}
	
	
	/**
	 * Calculates the Combined FDR Score for the PSM sets in the overview 
	 */
	public void calculateCombinedFDRScore() {
		logger.info("calculateCombinedFDRScore");
		
		// the average FDR takes always the FDR score for calculation and all PSMs, which have an FDR score (so no topRankIdentification settings)
		psmModeller.updateFilesFDRData(0L,
				DecoyStrategy.getStrategyByString(formDecoyStrategy),
				formDecoyPattern,
				formFDRThreshold,
				ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName(),
				0);
		
		psmModeller.calculateCombinedFDRScore();
		
		fileNeedsRecaching.put(0L, true);
	}
	
	
	/**
	 * returns for the given file number, whether the FDR is calculated.
	 * 
	 * @param fileNumber
	 * @return
	 */
	public boolean getIsFDRCalculated() {
		if (fileNumber > 0) {
			return psmModeller.isFDRCalculated(fileNumber);
		} else {
			return false;
		}
	}
	
	
	/**
	 * Returns whether the combined FDR Score is calculated. 
	 * @return
	 */
	public boolean getIsCombinedFDRScoreCalculated() {
		return psmModeller.isCombinedFDRScoreCalculated();
	}
	
	
	/**
	 * Returns whether the file with the given ID needs new caching since the
	 * last call of {@link #gotCachedDataForFile(Long)}.
	 * @param fileID
	 * @return
	 */
	public Boolean getFileNeedsRecaching(Long fileID) {
		if (fileNeedsRecaching.get(fileID) == null) {
			fileNeedsRecaching.put(fileID, true);
		}
		return fileNeedsRecaching.get(fileID);
	}
	
	
	/**
	 * Sets for the given file, that the current data is obtained filtered.
	 * @param fileID
	 */
	public void gotCachedDataForFile(Long fileID) {
		fileNeedsRecaching.put(fileID, false);
	}
}
