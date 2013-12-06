package de.mpc.pia.webgui.proteinviewer.component;


import java.util.List;

import de.mpc.pia.modeller.ProteinModeller;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.webgui.proteinviewer.ProteinViewer;


/**
 * Helper class to handle the ranking panel and the ranking in a
 * {@link ProteinViewer}.
 * 
 * @author julian
 *
 */
public class ProteinViewerRankingPanel {
	
	/** the {@link ProteinModeller} of the {@link ProteinViewer}*/
	private ProteinModeller proteinModeller;
	
	/** whether ranking is performed after or before filtering */
	private boolean rankAfterFilter;
	
	/** whether the reported proteins need recaching of the report, null means new filtering is needed. */
	private Boolean needsRecaching;
	
	
	/**
	 * Basic constructor
	 */
	public ProteinViewerRankingPanel(ProteinModeller modeller) {
		this.proteinModeller = modeller;
		
		rankAfterFilter = false;
		
		needsRecaching = true;
	}
	
	
	/**
	 * Getter for rankAfterFilter.
	 * @return
	 */
	public boolean getRankAfterFilter() {
		return rankAfterFilter;
	}
	
	
	/**
	 * Setter for rankAfterFilter.
	 * @return
	 */
	public void setRankAfterFilter(boolean rank) {
		rankAfterFilter = rank;
	}
	
	
	/**
	 * Recalculate the ranking for the {@link ReportPeptide}s of the
	 * selectedFileTabNumber.
	 */
	public void calculateRanking() {
		List<AbstractFilter> filters = getRankAfterFilter() ?
				proteinModeller.getReportFilters() : null;
		
		proteinModeller.calculateRanking(filters);
		
		needsRecaching = true;
	}
	
	
	/**
	 * Returns whether the proteins are ranked.
	 * 
	 * @return
	 */
	public Boolean getAreProteinsRanked() {
		return proteinModeller.getAreProteinsRanked();
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
