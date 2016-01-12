package de.mpc.pia.modeller.protein.scoring;

import java.util.List;
import java.util.Map;

import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.score.ScoreModel;


public class MultiplicativeScoring extends AbstractScoring {

    /** the human readable score */
    public static final  String name = "Multiplicative Scoring";

    /** the machine readable score */
    public static final String shortName = "scoring_multiplicative";


    public MultiplicativeScoring(Map<String, String> scoreNameMap) {
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

        for (ScoreModel score : scores) {

            Double signum = 1.0;
            if ((score.getType().higherScoreBetter() != null) &&
                    !score.getType().higherScoreBetter()) {
                signum = -1.0;
            }

            if ((score != null) && !score.getValue().equals(Double.NaN)) {
                if (!proteinScore.equals(Double.NaN)) {
                    if (signum < 0) {
                        proteinScore -= Math.log10(score.getValue());
                    } else {
                        proteinScore *= score.getValue();
                    }
                } else {
                    if (signum < 0) {
                        proteinScore = -Math.log10(score.getValue());
                    } else {
                        proteinScore = score.getValue();
                    }
                }
            }
        }

        return proteinScore;
    }
}
