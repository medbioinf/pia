package de.mpc.pia.webgui.proteinviewer.component;

import org.apache.log4j.Logger;

import de.mpc.pia.modeller.ProteinModeller;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;


/**
 * This little helper class builds the Panel for the FDR settings used by
 * the Protein Viewer.
 * 
 * @author julian
 *
 */
public class ProteinViewerFDRPanel {
	
	/** the {@link ProteinModeller} of the {@link ProteinViewer}*/
	private ProteinModeller proteinModeller;
	
	/** the sorting panel */
	private ProteinViewerSortingPanel sortPanel;
	
	
	/** the used decoy strategy shown and set via the form */
	private String formDecoyStrategy;
	
	/** the decoy pattern shown and set via the form */
	private String formDecoyPattern;
	
	/** the FDR threshold shown and set via the form */
	private Double formFDRThreshold;
	
	/** whether the reported proteins need recaching of the report, null means new filtering is needed. */
	private Boolean needsRecaching;
	
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(ProteinViewerFDRPanel.class);
	
	
	
	/**
	 * basic constructor
	 * @param modeller
	 */
	public ProteinViewerFDRPanel(ProteinModeller modeller,
			ProteinViewerSortingPanel sortPanel) {
		this.proteinModeller = modeller;
		this.sortPanel = sortPanel;
		
		needsRecaching = true;
		
		updateFDRSettingsPanel();
	}
	
	
	/**
	 * Updates the data for the panel with date for the given file.
	 * 
	 * @return
	 */
	public void updateFDRSettingsPanel() {
		// reset all the form data to the ones given by the file's FDRData
		FDRData fdrData = proteinModeller.getFDRData();
		
		formDecoyPattern = fdrData.getDecoyPattern();
		formFDRThreshold = fdrData.getFDRThreshold();
		formDecoyStrategy = fdrData.getDecoyStrategy().toString();
	}
	
	
	/**
	 * Getter for the protein FDR data
	 * @return
	 */
	public FDRData getFDRData() {
		return proteinModeller.getFDRData();
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
	 * Returns, whether there are any PSMs in the PIA XML file, which are
	 * flagged as decoys.
	 * 
	 * @return
	 */
	public Boolean getHasInternalDecoy() {
		return proteinModeller.getInternalDecoysExist();
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
	 * Updates the decoy strategy. If it changes since the last FDR calculation,
	 * it will be reseted.
	 */
	public void updateDecoyStrategy() {
		logger.info("\tformDecoyStrategy " + formDecoyStrategy);
		
		// compare the used strategy for FDRData and reset (if necessary)
		proteinModeller.updateFDRData(
				DecoyStrategy.getStrategyByString(formDecoyStrategy),
				formDecoyPattern, formFDRThreshold);
		
		proteinModeller.updateDecoyStates();
		
		needsRecaching = true;
	}
	
	
	/**
	 * Calculate the FDR with the given parameters.
	 */
	public void calculateFDR() {
		logger.info("calculateFDR");
		
		updateDecoyStrategy();
		
		proteinModeller.calculateFDR();
		
		proteinModeller.sortReport(sortPanel.getSortPriorities(),
				sortPanel.getSortOrders());
		
		needsRecaching = true;
	}
	
	
	/**
	 * Returns, whether a valid FDR calculation is run for the proteins
	 * 
	 * @param fileNumber
	 * @return
	 */
	public boolean getIsFDRCalculated() {
		return (proteinModeller.getFDRData().getNrItems() != null);
	}
	
	
	/**
	 * Returns whether the report needs new caching since the
	 * last call of {@link #gotCachedDataForFile(Long)}.
	 * @param fileID
	 * @return
	 */
	public Boolean getNeedsRecaching() {
		return needsRecaching;
	}
	
	
	/**
	 * Sets that the current data is obtained filtered.
	 * @param fileID
	 */
	public void gotCachedData() {
		needsRecaching = false;
	}
}
