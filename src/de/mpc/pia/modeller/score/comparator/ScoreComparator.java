package de.mpc.pia.modeller.score.comparator;

import java.util.Comparator;

import de.mpc.pia.modeller.score.ScoreModel;


public class ScoreComparator<T extends ScoreComparable> implements Comparator<T> {
	
	/** the index of the compared score model of the spectrum */
	private String scoreModelName;
	
	/** whether a higher score is better or not, if not set, the {@link ScoreComparable} must decide */
	private Boolean higherScoreBetter;
	
	
	public ScoreComparator() {
		super();
		this.scoreModelName = "";
		this.higherScoreBetter = null;
	}
	
	
	public ScoreComparator(String modelName) {
		super();
		this.scoreModelName = modelName;
		this.higherScoreBetter = null;
	}
	
	
	public ScoreComparator(String modelName, boolean higherScoreBetter) {
		this(modelName);
		this.higherScoreBetter = higherScoreBetter;
	}
	
	
	/**
	 * Sets the index of the scores of a spectrum, which will be compared.
	 */
	public void setComparedScoreModel(String modelName) {
		this.scoreModelName = modelName;
	}
	
	
	@Override
	public int compare(T o1, T o2) {
		ScoreModel score1, score2;
		
		score1 = o1.getCompareScore(scoreModelName);
		score2 = o2.getCompareScore(scoreModelName);
		
		if ((score1 == null) && 
				(score2 == null)) {
			// both PSMs don't have the score with given index
			return 0;
		} else if ((score1 == null) && 
				(score2 != null)) {
			// score1 does not have the score with given index
			return 1;
		} else if ((score1 != null) && 
				(score2 == null)) {
			// score2 does not have the score with given index
			return -1;
		} else {
			// both have the score model with given index
			Integer ret;
			
			if (higherScoreBetter != null) {
				ret = score1.compareTo(score2, higherScoreBetter);
			} else {
				ret = score1.compareTo(score2);
			}
			
			// if we don't have a defined score model, just order by highest double
			return (ret != null) ? ret : 
				Double.compare(score1.getValue(), score2.getValue());
		}
	}
	
	@Override
	public String toString() {
		return scoreModelName + ":" + higherScoreBetter;
	}
}
