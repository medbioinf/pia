package de.mpc.pia.modeller.protein.scoring;

import java.util.List;
import java.util.Map;

import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.score.ScoreModel;


public class AdditiveScoring extends AbstractScoring {

    private static final long serialVersionUID = -2932765775124060470L;


    /** the human readable score */
    protected static final String NAME = "Additive Scoring";

    /** the machine readable score */
    protected static final String SHORT_NAME = "scoring_additive";


    public AdditiveScoring(Map<String, String> scoreNameMap) {
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


    /**
     * Calculate the protein score by adding the scores.
     */
    @Override
    public Double calculateProteinScore(ReportProtein protein) {
        List<ScoreModel> scores = PSMForScoring.getProteinsScores(
                getPSMForScoringSetting().getValue(), protein,
                getScoreSetting().getValue());

        if (scores.isEmpty()) {
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
