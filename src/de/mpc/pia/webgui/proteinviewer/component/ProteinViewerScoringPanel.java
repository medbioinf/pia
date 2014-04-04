package de.mpc.pia.webgui.proteinviewer.component;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import de.mpc.pia.modeller.ProteinModeller;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.protein.scoring.ProteinScoringFactory;
import de.mpc.pia.webgui.proteinviewer.ProteinViewer;


/**
 * Helper class to handle the scoring panel and the scorings in a
 * {@link ProteinViewer}
 * 
 * @author julian
 *
 */
public class ProteinViewerScoringPanel {
	
	/** the {@link ProteinModeller} of the {@link ProteinViewer}*/
	private ProteinModeller proteinModeller;
	
	
	/** short name of the currently selected scoring, changes every time another score is selected by ajax calls*/
	private String formSelectedScoring;
	
	/** the scoring methods, maps from (method shortName -> method) */
	private Map<String, AbstractScoring> scoringMethods;
	
	/** some messages */
	private String messages;
	
	/** whether the reported proteins need recaching of the report, null means new filtering is needed. */
	private Boolean needsRecaching;

	
	
	
	/**
	 * Basic constructor
	 */
	public ProteinViewerScoringPanel(ProteinModeller proteinModeller) {
		this.proteinModeller = proteinModeller;
		
		scoringMethods = new HashMap<String, AbstractScoring>();
		messages = null;
		
		formSelectedScoring = ProteinScoringFactory.ScoringType.
				ADDITIVE_SCORING.getShortName();
		
		needsRecaching = true;
	}
	
	
	/**
	 * Getter for the short name of the currently selected scoring.
	 * @return
	 */
	public String getFormSelectedScoring() {
		if (formSelectedScoring != null) {
			return formSelectedScoring;
		} else {
			if (proteinModeller.getAppliedScoringMethod() != null) {
				return proteinModeller.getAppliedScoringMethod().getShortName();
			} else {
				String scoring = null;
				
				for (SelectItem item : getScoringSelectItems()) {
					scoring = item.getValue().toString();
					break;
				}
				
				return scoring;
			}
		}
	}
	
	
	/**
	 * Setter for the short name of the currently selected scoring.
	 * @return
	 */
	public void setFormSelectedScoring(String shortName) {
		formSelectedScoring = shortName;
	}
	
	
	/**
	 * Getter for the human readable name of the currently selected scoring.
	 * @return
	 */
	public String getSelectedScoringName() {
		if (proteinModeller.getAppliedScoringMethod() != null) {
			return proteinModeller.getAppliedScoringMethod().getName();
		} else {
			return "no scoring";
		}
	}
	
	
	/**
	 * Returns a List of SelectItems for the selection of the scoring method.
	 * @return
	 */
	public List<SelectItem> getScoringSelectItems() {
		List<SelectItem> items = new ArrayList<SelectItem>();
		
		for (Map.Entry<String, String> scoringIt
				: ProteinScoringFactory.getAllScoringNames().entrySet()) {
			items.add( new SelectItem(scoringIt.getKey(),
					scoringIt.getValue()) );
		}
		
		return items;
	}
	
	
	/**
	 * Getter for the current scoring method.
	 * @return
	 */
	public AbstractScoring getScoringMethod() {
		AbstractScoring scoring = null;
		
		scoring = scoringMethods.get(getFormSelectedScoring());
		
		Map<String, String> scoreNameMap = new HashMap<String, String>();
		
		for (Map.Entry<String, String> scoringIt 
				: proteinModeller.getScoreShortsToScoreNames().entrySet()) {
			scoreNameMap.put(scoringIt.getKey(), scoringIt.getValue());
		}
		
		if (scoring == null) {
			// initialize, if the scoring is not yet in the map
			scoring = ProteinScoringFactory.getNewInstanceByName(
					formSelectedScoring, scoreNameMap);
			
			if (scoring != null) {
				scoringMethods.put(formSelectedScoring, scoring);
			}
		} else {
			// update the available scores for the scoring
			scoring.updateAvailableScores(scoreNameMap);
		}
		
		return scoring;
	}
	
	
	/**
	 * Getter for the messages.
	 * @return
	 */
	public String getMessages() {
		return messages;
	}
	
	
	/**
	 * Apply the selected scoring to the List of {@link ReportProtein}s from
	 * the {@link ProteinViewer}.
	 */
	public void applyScoring() {
		AbstractScoring scoring = scoringMethods.get(formSelectedScoring);
		
		proteinModeller.applyScoring(scoring);
		
		needsRecaching = true;
	}
	
	
	/**
	 * Returns whether a score is calculated for the {@link ReportProtein}s.
	 * @return
	 */
	public boolean isScoreCalculated() {
		return (proteinModeller.getAppliedScoringMethod() != null);
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
