package de.mpc.pia.webgui.psmviewer.component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.modeller.PSMModeller;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.score.ScoreModelEnum;


/**
 * This little helper class builds the Panel for the ranking settings and
 * sorting information for the PSM Viewer.
 * 
 * @author julian
 *
 */
public class PSMViewerRankingPanel {
	
	/** the PSM modeller */
	private PSMModeller psmModeller;
	
	/** the ID of the current file */
	private long fileID;
	
	/** the filterPanel, needed to pass filters (if selected) */
	private PSMViewerFilterPanel filterPanel;
	
	
	/** map from the file ID to the shortname of the score used for ranking */
	private Map<Long, String> fileRankScoreName;
	
	/** map from the file ID to whether ranking is performed after or before filtering */
	private Map<Long, Boolean> fileRankAfterFilter;
	
	
	/** whether the file with the given ID needs a recaching of the report, null means new filtering is needed. */
	private Map<Long, Boolean> fileNeedsRecaching;
	
	
	
	
	/**
	 * Basic constructor, setting everything to null.
	 */
	public PSMViewerRankingPanel(PSMModeller modeller,
			PSMViewerFilterPanel filterPanel,
			PSMViewerSortingPanel sortPanel) {
		this.psmModeller = modeller;
		this.filterPanel = filterPanel;
		fileID = 0L;
		
		// reset the rankings
		fileRankScoreName = new HashMap<Long, String>(modeller.getFiles().size());
		fileRankAfterFilter = new HashMap<Long, Boolean>(modeller.getFiles().size());
		
		for (PIAInputFile file : modeller.getFiles().values()) {
			// initialise the rankings with null
			fileRankScoreName.put(file.getID(), null);
			fileRankAfterFilter.put(file.getID(), false);
		}
		
		fileNeedsRecaching = new HashMap<Long, Boolean>();
	}
	
	
	/**
	 * updates the rankSortSettingsOutputPanel
	 * 
	 * @return
	 */
	public void updateRankSortSettingsPanel(long fileID) {
		this.fileID = fileID;
	}
	
	
	/**
	 * returns the name  of the currently selected file
	 * @return
	 */
	public String getName() {
		String name = psmModeller.getFiles().get(fileID).getName();
		
		if (name == null) {
			name = psmModeller.getFiles().get(fileID).getFileName();
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
		return psmModeller.getFilesAvailableScoreShortsForRanking(fileID);
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
					: psmModeller.getFilesAvailableScoreShortsForRanking(fileID)) {
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
	 * Gets the ranking of the current file
	 * @param scoreName
	 */
	public String getRankingScore() {
		return fileRankScoreName.get(fileID);
	}
	
	
	/**
	 * setter for "rank after filtering"
	 * @param rank
	 */
	public void setRankAfterFilter(Boolean rank) {
		fileRankAfterFilter.put(fileID, rank);
	}
	
	
	/**
	 * getter for "rank after filtering"
	 * @param rank
	 */
	public Boolean getRankAfterFilter() {
		return fileRankAfterFilter.get(fileID);
	}
	
	
	/**
	 * Calculates the ranking for the current file and scoreShortName.
	 */
	public void calculateRanking() {
		List<AbstractFilter> filters = getRankAfterFilter() ?
				filterPanel.getFilters() : null;
		
		psmModeller.calculateRanking(fileID, getRankingScore(), filters);
		
		fileNeedsRecaching.put(fileID, true);
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
