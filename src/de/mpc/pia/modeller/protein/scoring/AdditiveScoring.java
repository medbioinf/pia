package de.mpc.pia.modeller.protein.scoring;

import java.util.List;
import java.util.Map;

import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.score.ScoreModel;


public class AdditiveScoring extends AbstractScoring {
	
	/** the human readable score */
	protected static String name = "Additive Scoring";
	
	/** the machine readable score */
	protected static String shortName = "scoring_additive";
	
	
	public AdditiveScoring(Map<String, String> scoreNameMap) {
		super(scoreNameMap);
	}


	@Override
	public String getName() {
		return name;
	}
	
	
	@Override
	public String getShortName() {
		return shortName;
	}
	
	
	@Override
	public Boolean higherScoreBetter() {
		// TODO: make this changeable?
		return true;
	}
	
	
	/**
	 * Calculate the protein score by adding the scores.
	 */
	@Override
	public Double calculateProteinScore(ReportProtein protein) {
		List<ScoreModel> scores = PSMForScoring.getProteinsScores(
				getPSMForScoringSetting().getValue(), protein,
				getScoreSetting().getValue());
		
		if (scores.size() < 1) {
			// no scores found -> no scoring possible
			return Double.NaN;
		}
		
		Double proteinScore = Double.NaN;
		
		for (ScoreModel score : scores) {
			if ((score != null) && !score.getValue().equals(Double.NaN)) {
				if (!proteinScore.equals(Double.NaN)) {
					proteinScore += score.getValue();
				} else {
					proteinScore = score.getValue();
				}
			}
		}
		
		return proteinScore;
	}
}
