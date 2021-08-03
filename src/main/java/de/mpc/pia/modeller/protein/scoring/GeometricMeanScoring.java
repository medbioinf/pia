package de.mpc.pia.modeller.protein.scoring;

import java.util.List;
import java.util.Map;

import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.score.ScoreModel;


public class GeometricMeanScoring extends AbstractScoring {
	
	/** the human readable score */
	protected static String NAME = "Geometric Mean Scoring";
	
	/** the machine readable score */
	protected static String SHORT_NAME = "geometric_mean_scoring";
	
	
	public GeometricMeanScoring(Map<String, String> scoreNameMap) {
		super(scoreNameMap);
	}
	
	@Override
	public String getName() {
		return NAME;
	}
	
	
	@Override
	public String getShortName() {
		return SHORT_NAME;
	}
	
	
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
		int nrScore = 0;
		
		// count the scores
		for (ScoreModel score : scores) {
			if ((score != null) && !score.getValue().equals(Double.NaN)) {
				nrScore++;
			}
		}
		
		if (nrScore > 0) {
			double exp = 1.0 / (double)nrScore;
			
			// calculate the product(scores)^(1/nrScores) respectively -log() of it for higherScoreBetter
			for (ScoreModel score : scores) {
				double signum = 1.0;
				if ((score.getType().higherScoreBetter() != null) &&
						!score.getType().higherScoreBetter()) {
					signum = -1.0;
				}
				
				if (!score.getValue().equals(Double.NaN)) {
					if (!proteinScore.equals(Double.NaN)) {
						if (signum < 0) {
							proteinScore -= Math.log10(Math.pow(score.getValue(), exp));
						} else {
							proteinScore *= Math.pow(score.getValue(), exp);
						}
						
					} else {
						if (signum < 0) {
							proteinScore = -Math.log10(Math.pow(score.getValue(), exp));
						} else {
							proteinScore = Math.pow(score.getValue(), exp);
						}
					}
				}
			}
		}
		
		return proteinScore;
	}
}
