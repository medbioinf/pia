package de.mpc.pia.modeller.protein.scoring;

import java.util.List;
import java.util.Map;

import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.score.ScoreModel;


public class GeometricMeanScoring extends AbstractScoring {
	
	/** the human readable score */
	protected static String name = "Geometric Mean Scoring";
	
	/** the machine readable score */
	protected static String shortName = "geometric_mean_scoring";
	
	
	public GeometricMeanScoring(Map<String, String> scoreNameMap) {
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
			
			// calculate the -log( product(scores)^(1/nrScores) )
			for (ScoreModel score : scores) {
				if ((score != null) && !score.getValue().equals(Double.NaN)) {
					if (!proteinScore.equals(Double.NaN)) {
						proteinScore -= Math.log10(Math.pow(score.getValue(), exp));
					} else {
						proteinScore = -Math.log10(Math.pow(score.getValue(), exp));
					}
				}
			}
		}
		
		return proteinScore;
	}
}
