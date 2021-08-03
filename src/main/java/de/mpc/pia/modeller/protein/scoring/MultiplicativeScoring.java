package de.mpc.pia.modeller.protein.scoring;

import java.util.List;
import java.util.Map;

import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.score.ScoreModel;


public class MultiplicativeScoring extends AbstractScoring {

    private static final long serialVersionUID = 3805443330101114308L;


    /** the human readable score */
    public static final  String NAME = "Multiplicative Scoring";

    /** the machine readable score */
    public static final String SHORT_NAME = "scoring_multiplicative";


    public MultiplicativeScoring(Map<String, String> scoreNameMap) {
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

        if (scores.isEmpty()) {
            // no scores found -> no scoring possible
            return Double.NaN;
        }

        Double proteinScore = Double.NaN;

        for (ScoreModel score : scores) {
            double signum = 1.0;
            if ((score.getType().higherScoreBetter() != null) &&
                    !score.getType().higherScoreBetter()) {
                signum = -1.0;
            }

            if (!score.getValue().equals(Double.NaN)) {
                if (!proteinScore.equals(Double.NaN)) {
                    proteinScore = addToScore(proteinScore, signum, score.getValue());
                } else {
                    proteinScore = initializeScore(signum, score.getValue());
                }
            }
        }

        return proteinScore;
    }


    /**
     * Initializes the protein score with the given valuescore
     *
     * @param signum
     * @param value
     * @return
     */
    private static Double initializeScore(Double signum, Double value) {
        Double ret = value;

        if (signum < 0) {
            ret = -Math.log10(value);
        }

        return ret;
    }


    /**
     * Adds the given value to the protein score
     *
     * @param score
     * @param signum
     * @param value
     * @return
     */
    private static Double addToScore(Double score, Double signum, Double value) {
        Double ret = score;

        if (signum < 0) {
            ret -= Math.log10(value);
        } else {
            ret *= value;
        }

        return ret;
    }
}


