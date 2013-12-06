package de.mpc.pia.modeller.score.comparator;

import de.mpc.pia.modeller.score.ScoreModel;

public interface ScoreComparable {
	
	/**
	 * returns the score, with which the comparison will be performed.
	 * @param scoreShortname
	 * @return
	 */
	public ScoreModel getCompareScore(String scoreShortname);
}
