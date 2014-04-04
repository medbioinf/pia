package de.mpc.pia.webgui.psmviewer;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.modeller.IdentificationKeySettings;
import de.mpc.pia.modeller.PSMModeller;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.webgui.psmviewer.component.PSMViewerExportPanel;
import de.mpc.pia.webgui.psmviewer.component.PSMViewerFDRPanel;
import de.mpc.pia.webgui.psmviewer.component.PSMViewerFilterPanel;
import de.mpc.pia.webgui.psmviewer.component.PSMViewerRankingPanel;
import de.mpc.pia.webgui.psmviewer.component.PSMViewerSortingPanel;


/**
 * This class handles and backs the whole PSM Viewer
 * 
 * @author julian
 *
 */
public class PSMViewer {
	/** the used {@link PSMModeller} for this viewer */
	private PSMModeller psmModeller;
	
	
	/** name of the selected File */
	private String selectedFileTabName;
	
	/** number of the file tab (i.e. the file ID)*/
	private Long selectedFileTabNumber;
	
	
	/** the output panel containing the fdr settings */ 
	private PSMViewerFDRPanel fdrPanelHandler;
	
	/** the handler for the ranking panel*/ 
	private PSMViewerRankingPanel rankingPanelHandler;
	
	/** the handler for the filter panel */
	private PSMViewerFilterPanel filterPanelHandler;
	
	/** handler for the export panel */
	private PSMViewerExportPanel exportPanelHandler;
	
	/** handler for the sorting panel */
	private PSMViewerSortingPanel sortPanelHandler;
	
	
	/** the cached filtered ReportPSMs */
	private Map<Long, List<ReportPSM>> fileFilteredReportPSMs;
	
	/** the cached filtered ReportPSMSets */
	private List<ReportPSMSet> filteredReportPSMSets;
	
	
	/** the number of digits for the displayed m/z values (and deltaMass) */
	private int numberDigitsMZ = 4;
	
	/** the number of digits for the displayed PPM values */
	private int numberDigitsPPM = 3;
	
	/** the number of digits for the displayed retention time values */
	private int numberDigitsRT = 2;
	
	/** the number of digits for the displayed scores */
	private int numberDigitsScores = 4;
	
	/** decimal format converter for very small scores */
	private DecimalFormat scoreDfVerySmall = new DecimalFormat("0.###E0");
	
	/** decimal format converter for normal scores */
	private DecimalFormat scoreDfNormal = new DecimalFormat("0.#");
	
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(PSMViewer.class);
	
	
	/**
	 * Basic Constructor
	 * 
	 * @param spectraMap
	 */
	public PSMViewer(PSMModeller modeller) {
		psmModeller = modeller;
		
		if (psmModeller == null) {
			throw new IllegalArgumentException("The given PSMModeller is null!");
		}
		
		
		// set the overview as the selected tab
		selectedFileTabName = "file_0";
		selectedFileTabNumber = 0L;
		
		
		// set up the filter panel
		filterPanelHandler = new PSMViewerFilterPanel(psmModeller);
		
		// set up the sort panel handler
		sortPanelHandler = new PSMViewerSortingPanel(psmModeller);
		
		// set up the ranking and sorting panel
		rankingPanelHandler = new PSMViewerRankingPanel(psmModeller,
				filterPanelHandler, sortPanelHandler);
		
		// set up the FDR settings panel
		fdrPanelHandler = new PSMViewerFDRPanel(psmModeller);
		
		// set up the export panel handler
		exportPanelHandler = new PSMViewerExportPanel(psmModeller);
		
		
		// set up the caches for ReportPSMs and ReportPSMSets
		fileFilteredReportPSMs = new HashMap<Long, List<ReportPSM>>();
		filteredReportPSMSets = null;
		
		
		// TODO: make this via an ini file
		setNumberDigitsMZ(4);
		setNumberDigitsPPM(3);
		setNumberDigitsRT(2);
		setNumberDigitsScores(4);
	}
	
	
	/**
	 * Apply the given general settings and recalculate the PSMSets
	 */
	public void applyGeneralSettings(boolean createPSMSets,
			Map<String, Boolean> psmSetSettings) {
		psmModeller.applyGeneralSettings(createPSMSets, psmSetSettings);
	}
	
	
	/**
	 * Returns the current values of the {@link IdentificationKeySettings} of
	 * the {@link PSMModeller}.
	 * 
	 * @return
	 */
	public Map<String, Boolean> getPSMSetSettings() {
		return psmModeller.getPSMSetSettings();
	}
	
	
	/**
	 * Getter for a List of {@link PIAInputFile}s used
	 * @return
	 */
	public List<PIAInputFile> getIdentificationFiles() {
		return new ArrayList<PIAInputFile>(psmModeller.getFiles().values());
	}
	
	
	/**
	 * Getter for the selected file tab name
	 * 
	 * @return
	 */
	public String getSelectedFileTabName() {
		return selectedFileTabName;
	}
	
	
	/**
	 * Getter for the selected file tab number
	 * 
	 * @return
	 */
	public Long getSelectedFileTabNumber() {
		return selectedFileTabNumber;
	}
	
	
	/**
	 * Setter for the selected file tab name
	 * 
	 * @param fileTabName
	 */
	public void setSelectedFileTabName(String fileTabName) {
		this.selectedFileTabName = fileTabName;
		
		selectedFileTabNumber = Long.parseLong(selectedFileTabName.substring(5));
		
		fdrPanelHandler.updateFDRSettingsPanel(selectedFileTabNumber);
		rankingPanelHandler.updateRankSortSettingsPanel(selectedFileTabNumber);
		filterPanelHandler.updateFilterPanel(selectedFileTabNumber);
		exportPanelHandler.updateExportPanel(selectedFileTabNumber);
		sortPanelHandler.updateSortingPanel(selectedFileTabNumber);
	}
	
	
	/**
	 * Getter for the scoreFieldShortNames of the given file
	 * 
	 * @param fileID
	 * @return
	 */
	public List<String> getScoreFieldShortNames(Long fileID) {
		return psmModeller.getScoreShortNames(fileID);
	}
	
	
	/**
	 * Returns the full name of the score model, given the short name
	 * 
	 * @return
	 */
	public String getScoreName(String shortName) {
		return psmModeller.getScoreName(shortName);
	}
	
	
	/**
	 * Returns the number of score fields for this file.
	 * 
	 * @param fileID
	 * @return
	 */
	public int getNrScoreFieldShortNames(Long fileID) {
		return psmModeller.getScoreShortNames(fileID).size();
	}
	
	
	/**
	 * returns the handler for the FDR settings panel
	 * 
	 * @return
	 */
	public PSMViewerFDRPanel getFDRPanelHandler() {
		return fdrPanelHandler;
	}
	
	
	/**
	 * Returns the handler for the rank and sort settings panel
	 * 
	 * @return
	 */
	public PSMViewerRankingPanel getRankingPanelHandler() {
		return rankingPanelHandler;		
	}
	
	
	/**
	 * Getter for the filter panel.
	 * @return
	 */
	public PSMViewerFilterPanel getFilterPanelHandler() {
		return filterPanelHandler;
	}
	
	
	/**
	 * Getter for the export panel.
	 * 
	 * @return
	 */
	public PSMViewerExportPanel getExportPanelHandler() {
		return exportPanelHandler;
	}
	
	
	/**
	 * Getter for the sort panel.
	 * @return
	 */
	public PSMViewerSortingPanel getSortPanelHandler() {
		return sortPanelHandler;
	}
	
	
	/**
	 * Returns the number of digits for the displayed m/z values (and deltaMass)
	 * @return
	 */
	public int getNumberDigitsMZ() {
		return numberDigitsMZ;
    }
	
	
	/**
	 * Setter for the number of digits for the displayed m/z values (and deltaMass)
	 * @return
	 */
	public void setNumberDigitsMZ(int digits) {
		numberDigitsMZ = digits;
    }
	
	
	/**
	 * Returns the number of digits for the displayed PPM values
	 * @return
	 */
	public int getNumberDigitsPPM() {
		return numberDigitsPPM;
    }
	
	
	/**
	 * Setter for the number of digits for the displayed PPM values
	 * @return
	 */
	public void setNumberDigitsPPM(int digits) {
		numberDigitsPPM = digits;
    }
	
	
	/**
	 * Returns the number of digits for the displayed retention time values
	 * @return
	 */
	public int getNumberDigitsRT() {
		return numberDigitsRT;
    }
	
	
	/**
	 * Setter for the number of digits for the displayed retention time values
	 * @return
	 */
	public void setNumberDigitsRT(int digits) {
		numberDigitsRT = digits;
    }
	
	
	/**
	 * Returns the number of digits for the displayed scores
	 * @return
	 */
	public int getNumberDigitsScores() {
		return numberDigitsScores;
	}
	
	
	/**
	 * Sets the number of digits for the displayed scores
	 * @param digits
	 */
	public void setNumberDigitsScores(int digits) {
		numberDigitsScores = digits;
		scoreDfVerySmall.setMaximumFractionDigits(numberDigitsScores);
		scoreDfNormal.setMaximumFractionDigits(numberDigitsScores);
	}
	
	
	/**
	 * Converts the given score to a string, using the settings for
	 * numberDigitsScores.
	 * 
	 * @param score
	 * @return
	 */
	public String convertScore(Double score) {
		if ((score != null) && !score.equals(Double.NaN)) {
			if (!score.equals(0.0) &&
					(Math.abs(score) < Math.pow(10.0, -numberDigitsScores))) {
				return scoreDfVerySmall.format(score);
			} else {
				return scoreDfNormal.format(score);
			}
		} else {
			if (score == null) {
				return null;
			} else if (score.equals(Double.NaN)) {
				return "NaN";
			}
			
			return Double.toString(score);
		}
	}
	
	
	/**
	 * Get the ReportPSMs for rendering in the viewer.
	 * The PSMs are filtered with the current filters from the filter handler.
	 * 
	 * @param fileID
	 * @return
	 */
	public List<ReportPSM> getReportPSMs(Long fileID) {
		if (fileID > 0) {
			if (fileNeedsRecaching(fileID)) {
				fileFilteredReportPSMs.put(fileID,
						psmModeller.getFilteredReportPSMs(fileID,
						filterPanelHandler.getFilters(fileID)));
				
				gotCachedDataForFile(fileID);
			}
			
			return fileFilteredReportPSMs.get(fileID);
		} else {
			logger.error("There are no ReportPSMs for the fileID " + fileID);
			return new ArrayList<ReportPSM>(0);
		}
	}
	
	
	/**
	 * Get the ReportPSMSets for rendering in the viewer.
	 * The PSMs are filtered with the current filters from the filter handler.
	 * 
	 * @return
	 */
	public List<ReportPSMSet> getReportPSMSets() {
		if (fileNeedsRecaching(0L)) {
			filteredReportPSMSets = psmModeller.getFilteredReportPSMSets(
					filterPanelHandler.getFilters(0L));
			
			gotCachedDataForFile(0L);
		}
		return filteredReportPSMSets;
	}
	
	
	/**
	 * Calculates the data for a histogram of the distribution of the PPM
	 * divergence. If fdrGood is true, only the FDR good target PSM (sets) are
	 * taken into account.
	 * 
	 * @param fileID
	 * @param fdrGood whether to use only the FDR good target PSM(set)s
	 * @return
	 */
	public List<List<Integer>> getPPMs(Long fileID, boolean fdrGood) {
		return psmModeller.getPPMs(fileID, fdrGood);
	}
	
	
	/**
	 * Returns whether the file with the given ID needs new caching since the
	 * last call of {@link #gotCachedDataForFile(Long)}, because something in
	 * the panels was changed in the meantime.<br/>
	 * @param fileID
	 * @return
	 */
	private Boolean fileNeedsRecaching(Long fileID) {
		if (fdrPanelHandler.getFileNeedsRecaching(fileID)) {
			return true;
		}
		
		if (filterPanelHandler.getFileNeedsRecaching(fileID)) {
			return true;
		}
		
		if (rankingPanelHandler.getFileNeedsRecaching(fileID)) {
			return true;
		}
		
		return false;
	}
	
	
	/**
	 * Tells all the panels that the data is cached with the current settings.
	 * @param fileID
	 */
	private void gotCachedDataForFile(Long fileID) {
		fdrPanelHandler.gotCachedDataForFile(fileID);
		filterPanelHandler.gotCachedDataForFile(fileID);
		rankingPanelHandler.gotCachedDataForFile(fileID);
	}
}
