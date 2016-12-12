package de.mpc.pia.modeller.score;

/**
 * This interface implements everything needed to compute the FDRScore.
 * 
 * @author julian
 *
 */
public interface FDRScoreComputable extends FDRComputable {
	/**
	 * Setter for the FDR score.
	 * @return
	 */
	void setFDRScore(Double score);
	
	/**
	 * Getter for the fdrScore.
	 * @return
	 */
	ScoreModel getFDRScore();
}
