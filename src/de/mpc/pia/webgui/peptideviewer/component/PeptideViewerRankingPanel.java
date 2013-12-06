package de.mpc.pia.webgui.peptideviewer.component;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mpc.pia.modeller.PeptideModeller;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.webgui.peptideviewer.PeptideViewer;


/**
 * Helper class to handle the ranking panel and the ranking in a
 * {@link PeptideViewer}.
 * 
 * @author julian
 *
 */
public class PeptideViewerRankingPanel {
	
	/** the {@link PeptideModeller} of the {@link PeptideViewer}*/
	private PeptideModeller peptideModeller;
	
	
	/** the ID of the current file */
	private Long fileID;
	
	
	/** the filterPanel, needed to pass filters (if selected) */
	private PeptideViewerFilteringPanel filteringPanel;
	
	
	/** map from the file ID to the shortname of the score used for ranking */
	private Map<Long, String> fileRankScoreName;
	
	/** map from the file ID to whether ranking is performed after or before filtering */
	private Map<Long, Boolean> fileRankAfterFilter;
	
	/** whether the file with the given ID needs new filtering, because the filters where changed, null means new filtering is needed. */
	private Map<Long, Boolean> fileNeedsRecaching;
	
	
	
	
	/**
	 * Basic constructor
	 */
	public PeptideViewerRankingPanel(PeptideModeller modeller,
			PeptideViewerFilteringPanel filteringPanel,
			PeptideViewerSortingPanel sortingPanel) {
		this.peptideModeller = modeller;
		this.filteringPanel = filteringPanel;
		fileID = 0L;
		
		
		fileRankScoreName = new HashMap<Long, String>();
		fileRankAfterFilter = new HashMap<Long, Boolean>();
		
		fileNeedsRecaching = new HashMap<Long, Boolean>();
	}
	
	
	/**
	 * Updates the data for the panel with data for the given file.
	 * 
	 * @return
	 */
	public void updateRankingPanel(Long fileID) {
		this.fileID = fileID;
	}
	
	
	/**
	 * returns the name  of the currently selected file
	 * @return
	 */
	public String getName() {
		String name = peptideModeller.getFiles().get(fileID).getName();
		
		if (name == null) {
			name = peptideModeller.getFiles().get(fileID).getFileName();
		}
		
		return name;
	}
	
	
	/**
	 * Returns the name of the ScoreModel used for ranking for the
	 * selectedFileTabNumber and whether it was with filtering.
	 * 
	 * @param fileID
	 */
	public String getCurrentRankingDescription() {
		if (fileRankScoreName.get(fileID) != null) {
			return ScoreModelEnum.getName(fileRankScoreName.get(fileID)) +
				(((fileRankAfterFilter.get(fileID) != null) && 
						fileRankAfterFilter.get(fileID)) ? " (after filtering)" : "");
		} else {
			return "(no ranking)";
		}
	}
	
	
	/**
	 * Returns the scoreShortNames of available ranking scores
	 * @return
	 */
	public List<String> getAvailableRankings() {
		return peptideModeller.getFilesAvailableScoreShortsForRanking(fileID);
	}
	
	
	/**
	 * Returns the ranking, which is selected in the form. This is actually the
	 * same as {@link #getRankingScore()}, except when this is null. In this
	 * case the first available is returned.
	 * 
	 * @return
	 */
	public String getFormSelectedRanking() {
		String rankScore = getRankingScore();
		
		if (rankScore == null) {
			for (String scoreShort
					: peptideModeller.getFilesAvailableScoreShortsForRanking(fileID)) {
				// take the first available score
				rankScore = scoreShort;
				break;
			}
		}
		
		return rankScore;
	}
	
	
	/**
	 * Sets the ranking score for the current file via the form.
	 * 
	 * @param scoreName
	 */
	public void setFormSelectedRanking(String scoreName) {
		if (!scoreName.trim().equals("")) {
			fileRankScoreName.put(fileID, scoreName);
		} else {
			fileRankScoreName.put(fileID, null);
		}
	}
	
	
	/**
	 * Returns the {@link ScoreModel}'s shortName for ranking of the
	 * selectedFileTabNumber.
	 * 
	 * @return
	 */
	public String getRankingScore() {
		return fileRankScoreName.get(fileID);
	}
	
	
	/**
	 * Sets the {@link ScoreModel}'s shortName for ranking of the
	 * selectedFileTabNumber.
	 * 
	 * @param scoreName
	 */
	public void setRankingScore(String scoreName) {
		if (ScoreModelEnum.getName(scoreName) != null) {
			fileRankScoreName.put(fileID, scoreName);
		}
	}
	
	
	/**
	 * Getter for rankAfterFilter.
	 * @return
	 */
	public boolean getRankAfterFilter() {
		Boolean rankAfterFilter = fileRankAfterFilter.get(fileID);
		if (rankAfterFilter != null) {
			return rankAfterFilter.booleanValue();
		} else {
			return false;
		}
	}
	
	
	/**
	 * Setter for rankAfterFilter.
	 * @return
	 */
	public void setRankAfterFilter(boolean rank) {
		fileRankAfterFilter.put(fileID, rank);
	}
	
	
	/**
	 * Recalculate the ranking for the {@link ReportPeptide}s of the
	 * selectedFileTabNumber.
	 * 
	 * TODO: this has to go into the modeller!
	 */
	public void calculateRanking() {
		List<AbstractFilter> filters = getRankAfterFilter() ?
				filteringPanel.getFilters() : null;
		
		peptideModeller.calculateRanking(fileID, getRankingScore(), filters);
		
		fileNeedsRecaching.put(fileID, true);
	}
	

	/**
	 * Returns whether the file with the given ID needs recaching since the
	 * last call of {@link #gotCachedDataForFile(Long)}, because the filters
	 * were changed in the meantime.<br/>
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
