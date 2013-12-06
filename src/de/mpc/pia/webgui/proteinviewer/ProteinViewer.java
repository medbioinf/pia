package de.mpc.pia.webgui.proteinviewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import de.mpc.pia.modeller.ProteinModeller;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.tools.PIAConfigurationProperties;
import de.mpc.pia.webgui.proteinviewer.component.ProteinViewerExportPanel;
import de.mpc.pia.webgui.proteinviewer.component.ProteinViewerFDRPanel;
import de.mpc.pia.webgui.proteinviewer.component.ProteinViewerFilteringPanel;
import de.mpc.pia.webgui.proteinviewer.component.ProteinViewerInferencePanel;
import de.mpc.pia.webgui.proteinviewer.component.ProteinViewerRankingPanel;
import de.mpc.pia.webgui.proteinviewer.component.ProteinViewerScoringPanel;
import de.mpc.pia.webgui.proteinviewer.component.ProteinViewerSortingPanel;


/**
 * This class handles everything around the ProteinViewer.
 * 
 * @author julian
 *
 */
public class ProteinViewer {
	/** the protein modeller */
	private ProteinModeller proteinModeller;
	
	
	/** the handler for filtering and the filter panel*/
	private ProteinViewerFilteringPanel filterHandler;
	
	/** the handler for ranking and the ranking panel*/
	private ProteinViewerRankingPanel rankingHandler;
	
	/** the handler for sorting and the sort panel*/
	private ProteinViewerSortingPanel sortHandler;
	
	/** the handler for scoring and the scoring panel*/
	private ProteinViewerScoringPanel scoringHandler;
	
	/** the handler for inference and the inference panel*/
	private ProteinViewerInferencePanel inferenceHandler;
	
	/** the handler for FDR calculationand the FDR panel*/
	private ProteinViewerFDRPanel fdrHandler;
	
	/** the handler for the export panel */
	private ProteinViewerExportPanel exportHandler;
	
	/** maps from the {@link ReportProtein} ID to whether show the peptides or not */
	private Map<Long, Boolean> showPeptides;
	
	/** maps from the {@link ReportProtein} ID to whether show the proteins or not */
	private Map<Long, Boolean> showSubProteins;
	
	/** the ID of the protein, of which the attribute is changed */
	private Long togglingProtein;
	
	/** the cached filtered ReportPSMSets */
	private List<ReportProtein> filteredReportProteins;
	
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(ProteinViewer.class);
	
	
	
	/**
	 * Basic Constructor
	 * 
	 * @param spectraMap
	 */
	public ProteinViewer(ProteinModeller modeller,
			PIAConfigurationProperties configurationProperties) {
		this.proteinModeller = modeller;
		
		// set up the filter panel
		filterHandler = new ProteinViewerFilteringPanel(proteinModeller);
		
		// set up the sorting panel
		sortHandler = new ProteinViewerSortingPanel();
		
		// set up the ranking panel
		rankingHandler = new ProteinViewerRankingPanel(proteinModeller);
		
		// set up the FDR handler
		fdrHandler = new ProteinViewerFDRPanel(proteinModeller, sortHandler);
		
		// set up the scoring panel
		scoringHandler = new ProteinViewerScoringPanel(proteinModeller);
		
		// set up the inference panel
		inferenceHandler = new ProteinViewerInferencePanel(proteinModeller,
				scoringHandler, sortHandler, configurationProperties);
		
		// set up the export panel
		exportHandler = new ProteinViewerExportPanel(proteinModeller);
		
		// set up the cache for ReportProteins
		filteredReportProteins = null;
		
		
		togglingProtein = null;
		showPeptides = new HashMap<Long, Boolean>();
		showSubProteins = new HashMap<Long, Boolean>();
	}
	
	/**
	 * Applies the general settings and recalculates the protein inference
	 */
	public void applyGeneralSettings() {
		proteinModeller.applyGeneralSettings();
		inferenceHandler.startInference();
	}
	
	
	/**
	 * Checks, whether the inference is already finished or running. If it is
	 * not yet started, it starts the inference and returns false. If the
	 * inference is done or running, true is returned.
	 * 
	 * @return
	 */
	public Boolean checkForInference() {
		if ((proteinModeller.getAppliedProteinInference() == null) &&
				!inferenceHandler.getInferenceRunning()) {
			inferenceHandler.startInference();
			return false;
		} else {
			return true;
		}
	}
	
	
	/**
	 * Returns the ReportProteins for rendering in the viewer.
	 * 
	 * @param ID
	 * @return
	 */
	public List<ReportProtein> getReportProteins() {
		if (reportNeedsRecaching()) {
			if ((proteinModeller.getAppliedProteinInference() == null) ||
					inferenceHandler.getInferenceRunning()) {
				return new ArrayList<ReportProtein>();
			} else {
				filteredReportProteins =
						proteinModeller.getFilteredReportProteins(
								filterHandler.getFilters());
				
				gotCachedReport();
			}
		}
		
		return filteredReportProteins;
	}
	
	
	/**
	 * Returns whether the reported proteins needs new caching since the
	 * last call of {@link #gotCachedDataForFile(Long)}, because something in
	 * the panels was changed in the meantime.<br/>
	 * @param fileID
	 * @return
	 */
	private Boolean reportNeedsRecaching() {
		if (fdrHandler.getNeedsRecaching()) {
			return true;
		}
		
		if (filterHandler.getNeedsRecaching()) {
			return true;
		}
		
		if (inferenceHandler.getNeedsRecaching()) {
			return true;
		}
		
		if (rankingHandler.getNeedsRecaching()) {
			return true;
		}
		
		if (scoringHandler.getNeedsRecaching()) {
			return true;
		}
		
		return false;
	}
	

	/**
	 * Tells all the panels that the data is cached with the current settings.
	 * @param fileID
	 */
	private void gotCachedReport() {
		fdrHandler.gotCachedData();
		filterHandler.gotCachedData();
		inferenceHandler.gotCachedData();
		rankingHandler.gotCachedData();
		scoringHandler.gotCachedData();
	}
	
	
	/**
	 * Whether to show the peptides of this protein or not.
	 * @param proteinID
	 * @return
	 */
	public boolean showProteinsPeptides(Long proteinID) {
		Boolean show = showPeptides.get(proteinID);
		
		if (show != null) {
			return show;
		} else {
			return false;
		}
	}
	
	
	/**
	 * Toggles whether to show the peptides of this protein.
	 * 
	 * @param proteinID
	 * @return
	 */
	public void toggleShowPeptides() {
		Boolean show = showPeptides.get(togglingProtein);
		if (show != null) {
			showPeptides.put(togglingProtein, !show);
		} else {
			showPeptides.put(togglingProtein, true);
		}
	}
	
	
	/**
	 * Returns the peptides of the protein with the given ID.
	 * 
	 * @param proteinID
	 * @return
	 */
	public List<ReportPeptide> getProteinsPeptides(Long proteinID) {
		ReportProtein protein = proteinModeller.getProtein(proteinID);
		
		if ((protein != null) && showProteinsPeptides(proteinID)) {
			return protein.getPeptides();
		} else {
			return new ArrayList<ReportPeptide>(0);
		}
	}
	
	
	/**
	 * Whether to show the subProteins of this protein or not.
	 * @param proteinID
	 * @return
	 */
	public Boolean showProteinsSubProteins(Long proteinID) {
		Boolean show = showSubProteins.get(proteinID);
		if (show != null) {
			return show;
		} else {
			return false;
		}
	}
	
	
	/**
	 * Toggles whether to show the subProteins of this protein.
	 * 
	 * @param proteinID
	 * @return
	 */
	public void toggleShowSubProteins() {
		Boolean show = showSubProteins.get(togglingProtein);
		
		if (show != null) {
			showSubProteins.put(togglingProtein, !show);
		} else {
			showSubProteins.put(togglingProtein, true);
		}
	}
	
	
	/**
	 * Returns the peptides of the protein with the given ID.
	 * 
	 * @param proteinID
	 * @return
	 */
	public List<ReportProtein> getProteinsSubSets(Long proteinID) {
		ReportProtein protein = proteinModeller.getProtein(proteinID);
		
		if ((protein != null) && showProteinsSubProteins(proteinID)) {
			return protein.getSubSets();
		} else {
			return new ArrayList<ReportProtein>(0);
		}
	}
	
	
	/**
	 * Sets the to-be-toggled-protein-ID
	 * @param id
	 */
	public void setToggleProtein(Long id) {
		togglingProtein = id;
	}
	
	
	/**
	 * Gets the to-be-toggled-protein-ID
	 * @param id
	 */
	public Long getToggleProtein() {
		return togglingProtein;
	}
	
	
	/**
	 * Returns a set of all the currently available scoreShortNames
	 * @return
	 */
	public List<String> getAllScoreShortNames() {
		return proteinModeller.getAllScoreShortNames();
	}
	
	
	/**
     * Getter for the filterHandler.
     * @return
     */
    public ProteinViewerFilteringPanel getFilterHandler()  {
    	return filterHandler;
    }
	
	
	/**
     * Getter for the rankingHandler.
     * @return
     */
    public ProteinViewerRankingPanel getRankingHandler()  {
    	return rankingHandler;
    }
	
	
	/**
     * Getter for the fdrHandler.
     * @return
     */
    public ProteinViewerFDRPanel getFDRHandler()  {
    	return fdrHandler;
    }
	
	
	/**
     * Getter for the sortHandler.
     * @return
     */
    public ProteinViewerSortingPanel getSortHandler()  {
    	return sortHandler;
    }
	
	
	/**
     * Getter for the scoringHandler.
     * @return
     */
    public ProteinViewerScoringPanel getScoringHandler()  {
    	return scoringHandler;
    }
	
    
	/**
     * Getter for the inferenceHandler.
     * @return
     */
    public ProteinViewerInferencePanel getInferenceHandler()  {
    	return inferenceHandler;
    }
    
    
    /**
     * Getter for the exportPanel.
     * @return
     */
    public ProteinViewerExportPanel getExportHandler() {
    	return exportHandler;
    }
}
