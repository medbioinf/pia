package de.mpc.pia.modeller.score.comparator;


/**
 * Interface for simple ranking.<br/>
 * 
 * The values of the rank may range from 0 to {@link Long#MAX_VALUE}. A value
 * smaller 0 is interpreted as not ranked.
 * 
 * @author julian
 *
 */
public interface Rankable extends ScoreComparable {
	/**
	 * Returns the score value with the given NAME.
	 * @param scoreName
	 * @return
	 */
	Double getScore(String scoreName);
	
	
	/**
	 * Getter for the rank.
	 * @return
	 */
	Long getRank();
	
	
	/**
	 * Setter for the rank.
	 * @return
	 */
	void setRank(Long rank);
}
