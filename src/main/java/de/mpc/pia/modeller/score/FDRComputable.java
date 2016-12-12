package de.mpc.pia.modeller.score;

import java.util.regex.Pattern;

import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;
import de.mpc.pia.modeller.score.comparator.ScoreComparable;

/**
 * This interface implements everything needed by to compute the FDR.
 * 
 * @author julian
 *
 */
public interface FDRComputable extends ScoreComparable {
	/**
	 * Returns the score value of the model with the given name.
	 * 
	 * @param scoreName
	 * @return
	 */
	Double getScore(String scoreShortName);
	
	
	/**
	 * Gets the local FDR value.
	 * @return
	 */
	double getFDR();
	
	
	/**
	 * Sets the local FDR value.
	 * @return
	 */
	void setFDR(double fdr);
	
	
	/**
	 * Getter for the qValue.
	 * @return
	 */
	double getQValue();
	
	
	/**
	 * Setter for the qValue.
	 * @return
	 */
	void setQValue(double value);
	
	
	/**
	 * Delete everything left of a prior FDR calculation.
	 */
	void dumpFDRCalculation();
	
	
	/**
	 * Checks whether this item is a decoy or not, given the pattern p, and
	 * updates the decoy status of the item.
	 * 
	 * @return
	 */
	void updateDecoyStatus(DecoyStrategy strategy, Pattern p);
	
	
	/**
	 * Returns true, if the item is a decoy, or false, if not. The state of the
	 * item should be set via updateDecoyStatus.
	 * @return
	 */
	boolean getIsDecoy();
	
	
	/**
	 * Sets, if this item is FDR good (below the threshold) or not.
	 * 
	 * @param isGood
	 */
	void setIsFDRGood(boolean isGood);
}
