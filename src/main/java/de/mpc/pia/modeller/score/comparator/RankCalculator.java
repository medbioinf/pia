package de.mpc.pia.modeller.score.comparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;



/**
 * This class has the only purpose to calculate the rank on a List of Rankables.
 * 
 * @author julian
 *
 */
public class RankCalculator {
	
	/**
	 * We don't ever want to instantiate this class
	 */
	private RankCalculator() {
		throw new AssertionError();
	}
	
	
	/**
	 * Calculate the ranking for a List of {@link Rankable}s regarding the
	 * {@link ScoreModel} given by the scoreShortName.
	 * 
	 * @param <T>
	 * @param scoreShortName
	 * @param items
	 */
	public static <T extends Rankable> void calculateRanking(String scoreShortName, List<T> items,
			Comparator<T> comparator) {
		if ((items == null) ||
				ScoreModelEnum.getModelByDescription(scoreShortName).equals(ScoreModelEnum.UNKNOWN_SCORE)) {
			// we have a null list or invalid SHORT_NAME
			return;
		}
		
		Collections.sort(items, comparator);
		
		long currRank = 0;
		Double rankScore = Double.NaN;
		int nrRankItems = 1;
		List<T> rankItems = new ArrayList<>();
		
		
		for (T item : items) {
			if (!rankScore.equals(item.getScore(scoreShortName))) {
				// this is a new rank, set the old one
				for (T rankItem : rankItems) {
					rankItem.setRank(currRank);
				}
				
				currRank += nrRankItems;
				nrRankItems = 0;
				rankScore = item.getScore(scoreShortName);
				rankItems = new ArrayList<>();
			}
			
			rankItems.add(item);
			nrRankItems++;
		}
		
		// set the last rankings
		for (T rankItem : rankItems) {
			rankItem.setRank(currRank);
		}
	}
	
}
