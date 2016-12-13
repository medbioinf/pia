package de.mpc.pia.modeller.protein.scoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.report.settings.Setting;
import de.mpc.pia.modeller.report.settings.SettingType;
import de.mpc.pia.modeller.score.ScoreModelEnum;


/**
 * This class defines the abstract for all the scoring methods. A scoring method
 * takes the scores of a {@link ReportProtein} (which is selected or fixed) and
 * calculates the protein score with them.
 * 
 * @author julian
 *
 */
public abstract class AbstractScoring {
	
	/** the score used for this scoring method */
	private Setting scoreSetting;
	
	/** which PSMs of the peptide should be used for scoring */
	private Setting psmForScoringSetting;
	
	
	public final static String scoringSettingID = "used_score";
	
	public final static String scoringSpectraSettingID = "used_spectra";
	
	
	/**
	 * Basic constructor, requires a mapping of the available
	 * {@link de.mpc.pia.modeller.score.ScoreModel}s.
	 * 
	 * @param scoreNameMap
	 */
	public AbstractScoring(Map<String, String> scoreNameMap) {
		String initialKey = null;
		
		if ((scoreNameMap != null) && (scoreNameMap.size() > 0)) {
			initialKey = scoreNameMap.keySet().iterator().next();
		}
		
		scoreSetting = new Setting(scoringSettingID, "Score for scoring",
				initialKey, SettingType.SELECT_ONE_RADIO.getShortName(),
				scoreNameMap);
		
		Map<String, String> psmSettingsMap = new HashMap<>();
		for (PSMForScoring psmForScoring : PSMForScoring.values()) {
			psmSettingsMap.put(psmForScoring.getShortName(),
					psmForScoring.getName());
		}
		psmForScoringSetting = new Setting(scoringSpectraSettingID,
				"PSMs used for scoring",
				PSMForScoring.ONLY_BEST.getShortName(),
				SettingType.SELECT_ONE_RADIO.getShortName(),
				psmSettingsMap);
	}
	
	
	/**
	 * Returns a List of the available settings for this scoring.
	 * @return
	 */
	public List<Setting> getSettings() {
		List<Setting> settingsList = new ArrayList<>(2);
		
		settingsList.add(getScoreSetting());
		settingsList.add(psmForScoringSetting);
		
		return settingsList;
	}
	
	
	/**
	 * Sets the value for the given setting. If a value with the settingName was
	 * found, it is returned with the new setting.
	 * 
	 * @return the setting with the new value or null, if no such setting was
	 * found
	 */
	public Setting setSetting(String settingName, String value) {
		if (scoringSettingID.equals(settingName)) {
			scoreSetting.setValue(value);
			return scoreSetting;
		} else if (scoringSpectraSettingID.equals(settingName)) {
			psmForScoringSetting.setValue(value);
			return psmForScoringSetting;
		}
		
		return null;
	}
	
	
	/**
	 * Getter for the name of the Scoring
	 * @return
	 */
	public abstract String getName();
	
	
	/**
	 * Getter for the SHORT_NAME of the Scoring
	 * @return
	 */
	public abstract String getShortName();
	
	
	/**
	 * Returns a descriptive String of the settings
	 * @return
	 */
	public String getDescriptiveSettings() {
		StringBuilder description = new StringBuilder();
		
		description.append(getName());
		description.append(", ");
		description.append(
				ScoreModelEnum.getName(getScoreSetting().getValue()));
		
		for (PSMForScoring pfs : PSMForScoring.values()) {
			if (pfs.getShortName().equals(getPSMForScoringSetting().getValue())) {
				description.append(", ");
				description.append(pfs.getName());
			}
		}
		
		return description.toString();
	}
	
	
	/**
	 * Getter for the score setting of this scoring.
	 * @return
	 */
	public Setting getScoreSetting() {
		return scoreSetting;
	}
	
	
	/**
	 * Getter for the PSM for scoring setting of this scoring.
	 * @return
	 */
	public Setting getPSMForScoringSetting() {
		return psmForScoringSetting;
	}
	
	
	/**
	 * Updates the available {@link de.mpc.pia.modeller.score.ScoreModel}s for this scoring.<br/>
	 * This should be called every time a new {@link de.mpc.pia.modeller.score.ScoreModel} (like e.g. the
	 * combined FDR Score) is added / calculated.
	 * 
	 * @param scoreNameMap map of the {@link de.mpc.pia.modeller.score.ScoreModel}s shortNames to its corresponding name
	 */
	public void updateAvailableScores(Map<String, String> scoreNameMap) {
		scoreSetting.updateParams(scoreNameMap);
	}
	
	
	/**
	 * Calculates the score for the {@link ReportProtein} with the current
	 * settings.
	 * 
	 * @param protein
	 */
	public abstract Double calculateProteinScore(ReportProtein protein);
	
	
	/**
	 * Calculates the scores for each {@link ReportProtein} in the given List
	 * with the current settings and set the them to the proteins. Also the
	 * subProtein's scores are calculated.
	 * 
	 * @param proteinList
	 */
	public final void calculateProteinScores(List<ReportProtein> proteinList) {
		Map<Long, ReportProtein> subProteins = new HashMap<>();
		
		// calculate scores for the reported proteins
		for (ReportProtein protein : proteinList) {
			protein.setScore(calculateProteinScore(protein));
			
			// get the subset proteins
			protein.getSubSets().stream().filter(subProtein -> !subProteins.containsKey(subProtein.getID())).forEach(subProtein -> subProteins.put(subProtein.getID(), subProtein));
		}
		
		// and the scores for the subset proteins
		for (ReportProtein subProtein : subProteins.values()) {
			subProtein.setScore(calculateProteinScore(subProtein));
		}
	}
	
	/**
	 * Creates a copy of this scoring, containing only the selected settings.
	 * @return
	 */
	public final AbstractScoring smallCopy() {
		Map<String, String> scoreNameMap = new HashMap<>(1);
		
		String scoreName = getScoreSetting().getValue();
		scoreNameMap.put(scoreName, ScoreModelEnum.getName(scoreName));
		AbstractScoring scoring = ProteinScoringFactory.getNewInstanceByName(getShortName(), scoreNameMap);
		if(scoring != null)
		   scoring.getPSMForScoringSetting().setValue(getPSMForScoringSetting().getValue());
		
		return scoring;
	}
}
