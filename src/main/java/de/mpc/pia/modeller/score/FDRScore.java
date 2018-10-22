package de.mpc.pia.modeller.score;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import de.mpc.pia.tools.PIAConstants;

/**
 * This class calculates the FDR Score for a valid list of items.
 *
 * @author julian
 *
 */
public class FDRScore {

    private FDRScore() {
        // never instantiate this
    }

    /**
     * Calculates the FDR score of the report. To do this, the report must have
     * FDR values and be sorted. Both should be done via calling
     * {@link FDRData#calculateFDR(List)} on the List before.
     *
     * @param reportItems the list of items, for which the FDR should be
     * calculated end of the list
     */
    public static <T extends FDRScoreComputable> void calculateFDRScore(
            List<T> reportItems, FDRData fdrData, boolean higherScoreBetter) {
        if (reportItems.size() < 2) {
            // no calculation for empty list
            return;
        }

        // calculate the FDR scores
        String scoreShortName = fdrData.getScoreShortName();
        boolean allScoresEqual = areAllScoresEqual(reportItems, scoreShortName);

        if (allScoresEqual) {
            // all scores are equal (including any decoys) -> set scores to qValue, as nothing useful is possible
            setFdrScoresToSameQValue(reportItems);
        } else {

            calculateFDRScores(reportItems, scoreShortName, higherScoreBetter, fdrData);
        }
    }


    /**
     * Checks whether all scores of the items are equal.
     *
     * @param reportItems
     * @param scoreShortName
     * @return
     */
    private static <T extends FDRScoreComputable> boolean areAllScoresEqual(List<T> reportItems, String scoreShortName) {
        boolean allEqual = true;
        Double scoreval = reportItems.get(0).getScore(scoreShortName);

        for (T item : reportItems) {
            if (!scoreval.equals(item.getScore(scoreShortName))) {
                allEqual = false;
                break;
            }
        }

        return allEqual;
    }


    /**
     * Sets the FDRScore to the QValue of the first entry in the list. This is only useful, if all q-values are equal,
     * which is the case when all scores are equal.
     *
     * @param reportItems
     */
    private static <T extends FDRScoreComputable> void setFdrScoresToSameQValue(List<T> reportItems) {
        Double value = reportItems.get(0).getQValue();

        if (value.equals(0.0)) {
            // the FDRScore is never 0!
            value = PIAConstants.SMALL_FDRSCORE_SUBSTITUTE;
        }

        for (T item : reportItems) {
            item.setFDRScore(value);
        }
    }


    /**
     * A helper class for the slope calculations
     *
     * @author julianu
     *
     */
    private static class SlopeValues {
        double qLast;
        double qNext;
        double sLast;
        double sNext;

        public void setLastNext() {
            sLast = sNext;
            qLast = qNext;
        }

        public double calculateSlope() {
            return (qNext-qLast) / (sNext-sLast);
        }

        public <T extends FDRScoreComputable> void updateNext(T item, String scoreShortName,
                boolean higherScoreBetter, Double bestScore) {
            sNext = item.getScore(scoreShortName);
            if (higherScoreBetter) {
                sNext = bestScore - sNext;
            }

            qNext = item.getQValue();
        }
    }


    /**
     * Actually calculates the FDR Score for each item
     *
     * @param reportItems
     * @param scoreShortName
     * @param higherScoreBetter
     * @param fdrData
     */
    private static <T extends FDRScoreComputable> void calculateFDRScores(List<T> reportItems, String  scoreShortName,
            boolean higherScoreBetter, FDRData fdrData) {
        Double bestScore = getBestScore(reportItems, higherScoreBetter, scoreShortName);

        Integer nextStep;
        SlopeValues sv = new SlopeValues();

        sv.sNext = 0;
        sv.qNext = 0;

        // get the stepPoints
        List<Integer> stepPoints = getStepPoints(reportItems);

        ListIterator<Integer> stepIterator = stepPoints.listIterator();
        if (stepIterator.hasNext()) {
            sv.setLastNext();
            nextStep = stepIterator.next();
            sv.updateNext(reportItems.get(nextStep), scoreShortName, higherScoreBetter, bestScore);
        } else {
            // we add an artificial decoy to the end...
            sv.setLastNext();

            nextStep = reportItems.size();

            sv.sNext = reportItems.get(reportItems.size()-1).getScore(scoreShortName);
            if (higherScoreBetter) {
                sv.sNext = bestScore - sv.sNext;
            }
            sv.qNext = fdrData.getArtificialDecoyFDR();
        }

        // the slope
        double g = sv.calculateSlope();

        // now calculate the fdr score for each item
        ListIterator<T> it = reportItems.listIterator();
        while (it.hasNext()) {
            FDRScoreComputable item = it.next();

            if (nextStep == it.nextIndex()-1) {
                if (stepIterator.hasNext()) {
                    sv.setLastNext();
                    nextStep = stepIterator.next();
                    sv.updateNext(reportItems.get(nextStep), scoreShortName, higherScoreBetter, bestScore);
                }

                g = sv.calculateSlope();
            }

            double fdrScore = calculateSlopedFDRScore(item, scoreShortName, higherScoreBetter, bestScore, g, sv);

            item.setFDRScore(fdrScore);
        }
    }


    /**
     * Calculates the stepPoints for the given list of reportItems
     *
     * @param reportItems
     * @return
     */
    private static <T extends FDRScoreComputable> List<Integer> getStepPoints(List<T> reportItems) {
        ListIterator<T> it;
        FDRScoreComputable item;
        List<Integer> stepPoints = new ArrayList<>();

        it = reportItems.listIterator(reportItems.size());
        double qValue = Double.NaN;

        while (it.hasPrevious()) {
            item = it.previous();

            if ((Double.compare(qValue, Double.NaN) != 0) &&
                    (item.getQValue() < qValue)) {
                stepPoints.add(it.nextIndex()+1);
            }

            qValue = item.getQValue();
        }

        Collections.sort(stepPoints);
        return stepPoints;
    }


    /**
     * Getter for the best score. As this is only used when higherScoreBetter = true, it otherwise returns null.
     *
     * @param reportItems
     * @param higherScoreBetter
     * @param scoreShortName
     * @return
     */
    private static <T extends FDRScoreComputable> Double getBestScore(List<T> reportItems, boolean higherScoreBetter,
            String scoreShortName) {
        Double bestScore = null;
        if (higherScoreBetter) {
            // need to avoid an FDRSCore of 0 for any real item
            bestScore = reportItems.get(0).getScore(scoreShortName);
            Double nextBestScore = null;
            for (int idx = 1; idx < reportItems.size(); idx++) {
                nextBestScore = reportItems.get(idx).getScore(scoreShortName);
                if (!bestScore.equals(nextBestScore)) {
                    break;
                }
            }

            // set the "best score" (which will have FDRSCore=0) to "bestScore + diff to 2nd best score"
            bestScore += bestScore - nextBestScore;
        }

        return bestScore;
    }


    /**
     * Calculates the FDRScore for the given item, using the slope and last stepPoint's values
     *
     * @param item
     * @param scoreShortName
     * @param higherScoreBetter
     * @param bestScore
     * @param g
     * @return
     */
    private static <T extends FDRScoreComputable> double calculateSlopedFDRScore(T item, String scoreShortName,
            boolean higherScoreBetter, Double bestScore, double g, SlopeValues sv) {
        Double fdrScore = item.getScore(scoreShortName);
        if (higherScoreBetter) {
            fdrScore = bestScore - fdrScore;
        }

        fdrScore = (fdrScore - sv.sLast) * g + sv.qLast;

        if (fdrScore.equals(0.0)) {
            fdrScore = PIAConstants.SMALL_FDRSCORE_SUBSTITUTE;
        }

        return fdrScore;
    }
}
