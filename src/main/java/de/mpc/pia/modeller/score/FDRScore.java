package de.mpc.pia.modeller.score;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public class FDRScore {
	
	/**
	 * Calculates the FDR score of the report. To do this, the report must have
	 * FDR values and be sorted. Both should be done via calling
	 * {@link FDRData#calculateFDR(List)} on the List before.
	 * 
	 * @param reportItems the list of items, for which the FDR should be
	 * calculated
	 */
	public static <T extends FDRScoreComputable> void calculateFDRScore(
			List<T> reportItems, FDRData fdrData, boolean higherScoreBetter) {
		if (reportItems.size() < 2) {
			// no calculation for empty list
			return;
		}
		
		// set the stepPoints
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
		
		
		// calculate the FDR scores
		double g;
		double qLast, qNext;
		double sLast, sNext;
		String scoreShortName = fdrData.getScoreShortName();
		
		Collections.sort(stepPoints);
		ListIterator<Integer> stepIterator = stepPoints.listIterator();
		Integer nextStep;
		
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
		
		sLast = 0;
		qLast = 0;
		
		if (stepIterator.hasNext()) {
			nextStep = stepIterator.next();
			
			sNext = reportItems.get(nextStep).getScore(scoreShortName);
			if (higherScoreBetter) {
				sNext = bestScore - sNext;
			}
			
			qNext = reportItems.get(nextStep).getQValue();
		} else {
			// we add an artificial decoy to the end...
			nextStep = reportItems.size();
			
			sNext = reportItems.get(reportItems.size()-1).getScore(scoreShortName);
			if (higherScoreBetter) {
				sNext = bestScore - sNext;
			}
			qNext = fdrData.getArtificialDecoyFDR();
		}
		
		g = (qNext-qLast) / (sNext-sLast);
		
		it = reportItems.listIterator();
		
		while (it.hasNext()) {
			item = it.next();
			
			if (nextStep == it.nextIndex()-1) {
				if (stepIterator.hasNext()) {
					sLast = sNext;
					qLast = qNext;
					
					nextStep = stepIterator.next();
					
					sNext = reportItems.get(nextStep).getScore(scoreShortName);
					if (higherScoreBetter) {
						sNext = bestScore - sNext;
					}
					
					qNext = reportItems.get(nextStep).getQValue();
					
				}
				
				g = (qNext-qLast) / (sNext-sLast);
			}
			
			double fdrScore = item.getScore(scoreShortName);
			if (higherScoreBetter) {
				fdrScore = bestScore - fdrScore;
			}
			fdrScore = (fdrScore - sLast) * g + qLast;
			item.setFDRScore(fdrScore);
		}
	}
	
}
