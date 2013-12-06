package de.mpc.pia.webgui.peptideviewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.modeller.PeptideModeller;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.webgui.peptideviewer.component.PeptideViewerExportingPanel;
import de.mpc.pia.webgui.peptideviewer.component.PeptideViewerFilteringPanel;
import de.mpc.pia.webgui.peptideviewer.component.PeptideViewerRankingPanel;
import de.mpc.pia.webgui.peptideviewer.component.PeptideViewerSortingPanel;


/**
 * This class handles everything around the PeptideViewer.
 * 
 * @author julian
 *
 */
public class PeptideViewer {
	/** the used {@link PeptideModeller}  */
	private PeptideModeller peptideModeller;
	
	
	/** name of the selected File */
	private String selectedFileTabName;
	
	/** number of the file tab (i.e. the file ID)*/
	private Long selectedFileTabNumber;
	
	
	/** the handler for sorting and the sort panel*/
	private PeptideViewerSortingPanel sortPanelHandler;
	
	/** the handler for sorting and the sort panel*/
	private PeptideViewerRankingPanel rankHandler;
	
	/** the handler for filtering and the filter panel*/
	private PeptideViewerFilteringPanel filterHandler;
	
	/** the handler for exporting and the export panel*/
	private PeptideViewerExportingPanel exportHandler;
	
	
	/** the cached filtered ReportPeptides */
	private Map<Long, List<ReportPeptide>> fileFilteredReportPeptides;
	
	
	
	/**
	 * Basic Constructor
	 * 
	 * @param spectraMap
	 */
	public PeptideViewer(PeptideModeller modeller) {
		peptideModeller = modeller;
		
		if (peptideModeller == null) {
			throw new IllegalArgumentException("The given PeptideModeller is null!");
		}
		
		// set up the sorting panel
		sortPanelHandler = new PeptideViewerSortingPanel(peptideModeller);
		
		// set up the filter panel
		filterHandler = new PeptideViewerFilteringPanel(peptideModeller);
		
		// set up the ranking panel
		rankHandler = new PeptideViewerRankingPanel(peptideModeller,
				filterHandler, sortPanelHandler);
		
		// set up the export panel
		exportHandler = new PeptideViewerExportingPanel(peptideModeller);
		
		
		// set up the caches for ReportPeptides
		fileFilteredReportPeptides = new HashMap<Long, List<ReportPeptide>>();
		
				
		// set the overview as the selected tab
		selectedFileTabName = "file_0";
		selectedFileTabNumber = new Long(0);
		
	}
	
	
	/**
	 * Getter for a List of {@link PIAInputFile}s used
	 * @return
	 */
	public List<PIAInputFile> getIdentificationFiles() {
		return new ArrayList<PIAInputFile>(peptideModeller.getFiles().values());
	}
	
	
	/**
	 * Get the ReportPSMs for rendering in the viewer.
	 * The PSMs are filtered with the current filters from the filter handler.
	 * 
	 * @param fileID
	 * @return
	 */
	public List<ReportPeptide> getReportPeptides(Long fileID) {
		if (fileNeedsRecaching(fileID)) {
			fileFilteredReportPeptides.put(fileID,
					peptideModeller.getFilteredReportPeptides(fileID,
							peptideModeller.getFilters(fileID)));
			
			gotCachedDataForFile(fileID);
		}
		
		return fileFilteredReportPeptides.get(fileID);
	}
	
	
	/**
	 * Returns whether the file with the given ID needs new caching since the
	 * last call of {@link #gotCachedDataForFile(Long)}, because something in
	 * the panels was changed in the meantime.<br/>
	 * @param fileID
	 * @return
	 */
	private Boolean fileNeedsRecaching(Long fileID) {
		if (filterHandler.getFileNeedsRecaching(fileID)) {
			return true;
		}
		
		if (rankHandler.getFileNeedsRecaching(fileID)) {
			return true;
		}
		
		return false;
	}
	
	
	/**
	 * Tells all the panels that the data is cached with the current settings.
	 * @param fileID
	 */
	private void gotCachedDataForFile(Long fileID) {
		filterHandler.gotCachedDataForFile(fileID);
		rankHandler.gotCachedDataForFile(fileID);
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
	 * Setter for the selected file tab name
	 * 
	 * @param fileTabName
	 */
	public void setSelectedFileTabName(String fileTabName) {
		this.selectedFileTabName = fileTabName;
		selectedFileTabNumber = Long.parseLong(selectedFileTabName.substring(5));
		
		filterHandler.updateFilterPanel(selectedFileTabNumber);
		rankHandler.updateRankingPanel(selectedFileTabNumber);
		sortPanelHandler.updateSortingPanel(selectedFileTabNumber);
		exportHandler.updateExportPanel(selectedFileTabNumber);
	}
	
	
	/**
	 * Getter for the selectedFileTabNumber
	 * 
	 * @return
	 */
	public Long getSelectedFileTabNumber() {
		return selectedFileTabNumber;
	}
	
	
	/**
	 * Apply the given general settings and recalculate the peptide Lists.
	 */
	public void applyGeneralSettings(boolean considerModifications) {
		peptideModeller.applyGeneralSettings(considerModifications);
		
		// sort all the reports
		for (Long fileID : peptideModeller.getFiles().keySet()) {
			peptideModeller.sortReport(fileID,
					sortPanelHandler.getSortPriorities(fileID),
					sortPanelHandler.getSortOrders(fileID));
		}
	}
	
	
	/**
	 * Getter for considerModifications.
	 * @return
	 */
	public boolean getConsiderModifications() {
		return peptideModeller.getConsiderModifications();
	}
	
	
	/**
	 * Returns the number of score fields for this file.
	 * 
	 * @param fileID
	 * @return
	 */
	public int getNrScoreFieldShortNames(Long fileID) {
		return peptideModeller.getScoreShortNames(fileID).size();
	}
	
	
	/**
	 * Getter for the scoreFieldShortNames of the given file
	 * 
	 * @param fileID
	 * @return
	 */
	public List<String> getScoreFieldShortNames(Long fileID) {
		return peptideModeller.getScoreShortNames(fileID);
	}
	
	
	/**
	 * Returns the full name of the score model, given the short name
	 * 
	 * @param fileID
	 * @return
	 */
	public String getScoreName(String shortName) {
		return ScoreModelEnum.getName(shortName);
	}
	
	
    /**
     * Getter for the sortHandler.
     * @return
     */
    public PeptideViewerSortingPanel getSortHandler()  {
    	return sortPanelHandler;
    }
	
	
    /**
     * Getter for the rankHandler.
     * @return
     */
    public PeptideViewerRankingPanel getRankHandler()  {
    	return rankHandler;
    }
    
	
    /**
     * Getter for the filterHandler.
     * @return
     */
    public PeptideViewerFilteringPanel getFilterHandler()  {
    	return filterHandler;
    }
	
	
    /**
     * Getter for the exportHandler.
     * @return
     */
    public PeptideViewerExportingPanel getExportHandler()  {
    	return exportHandler;
    }
}
