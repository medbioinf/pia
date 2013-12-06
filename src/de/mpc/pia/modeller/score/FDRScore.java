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
	 * @param scoreShortName the shortName of the Score used for FDR calculation
	 * @param artificialDecoyFDR the FDR value of an artificial decoy at the
	 * end of the list
	 */
	public static <T extends FDRScoreComputable> void calculateFDRScore(
			List<T> reportItems, FDRData fdrData) {
		// set the stepPoints
		ListIterator<T> it;
		FDRScoreComputable item;
		List<Integer> stepPoints = new ArrayList<Integer>();
		
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
		
		sLast = 0;
		qLast = 0;
		
		if (stepIterator.hasNext()) {
			nextStep = stepIterator.next();
			
			sNext = ScoreModelEnum.transformScoreForFDRScore(
					reportItems.get(nextStep).getScore(scoreShortName),
					scoreShortName);
			qNext = reportItems.get(nextStep).getQValue();
		} else {
			// we add an artificial decoy to the end...
			nextStep = reportItems.size();
			
			sNext = ScoreModelEnum.transformScoreForFDRScore(
					reportItems.get(reportItems.size()-1).getScore(scoreShortName),
					scoreShortName);
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
					
					sNext = ScoreModelEnum.transformScoreForFDRScore(
							reportItems.get(nextStep).getScore(scoreShortName),
							scoreShortName);
					qNext = reportItems.get(nextStep).getQValue();
				}
				
				g = (qNext-qLast) / (sNext-sLast);
			}
			
			item.setFDRScore(
					(ScoreModelEnum.transformScoreForFDRScore(item.getScore(scoreShortName), scoreShortName)
							-sLast)*g + qLast
				);
			
		}
	}
	
}
