package de.mpc.pia.modeller.score;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;

import de.mpc.pia.modeller.score.comparator.ScoreComparator;


/**
 * This class represents the FDR calculation data of a file.
 * 
 * @author julian
 *
 */
public class FDRData {
	
	/** teh used strategy to identify decoys */
	private DecoyStrategy decoyStrategy;
	
	/** the pattern to identify a decoy */
	private String decoyPattern;
	
	/** FDR threshold, beneath which an item is FDR good (e.g. 0.01) */
	private Double fdrThreshold;
	
	/** number of all Items */
	private Integer nrItems;
	
	/** number of FDR good target items */
	private Integer nrFDRGoodTargets;
	
	/** number of FDR good decoy items */
	private Integer nrFDRGoodDecoys;
	
	/** number of target items */
	private Integer nrTargets;
	
	/** number of decoy items */
	private Integer nrDecoys;
	
	/** the score at the FDR threshold */
	private Double scoreAtThreshold;
	
	/** the short NAME of the used score */
	private String scoreShortName;
	
	/** the FDR of an artificially added decoy at the last position in the list */
	private Double artificialDecoyFDR;
	
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(FDRData.class);
	
	
	/**
	 * The strategies for decoy identification.
	 */
	public enum DecoyStrategy {
		/**
		 * Decoys are identified by a pattern in the protein-accession, like
		 * "s.*" for "accession starting with an s".
		 */
		ACCESSIONPATTERN {
			@Override
			public String toString() {
				return "accessionpattern";
			}
		},
		/**
		 * The decoys were defined by the search engine (like e.g. Mascot with
		 * decoy search turned on) and the isDecoy argument in the PIA XML file
		 * is used.
		 */
		SEARCHENGINE {
			@Override
			public String toString() {
				return "searchengine";
			}
		},
		/**
		 * The decoys are defined by any inherited item (protein, peptide) or
		 * are given by the search engine (PSM)
		 */
		INHERIT {
			@Override
			public String toString() {
				return "inherit";
			}
		}
		;
		
		
		/**
		 * Gets the {@link DecoyStrategy} which equals the given String. If no
		 * valid description is found, {@link DecoyStrategy#ACCESSIONPATTERN}
		 * will be set as default.
		 * 
		 * @param strategyString
		 * @return
		 */
		public static DecoyStrategy getStrategyByString(String strategyString) {
			for (DecoyStrategy strategy : DecoyStrategy.values()) {
				if (strategy.toString().equals(strategyString)) {
					return strategy;
				}
			}
			
			return ACCESSIONPATTERN;
		}
	}
	
	
	public FDRData(DecoyStrategy strategy, String pattern, Double threshold) {
		this.decoyStrategy = strategy;
		this.decoyPattern = pattern;
		this.fdrThreshold = threshold;
		
		scoreShortName = null;
		
		nullAll();
	}
	
	
	/**
	 * Nulls all the non-set-able values, to indicate, that the FDR must be
	 * recalculated.
	 */
	private void nullAll() {
		nrItems = null;
		nrFDRGoodTargets = null;
		nrFDRGoodDecoys = null;
		nrTargets = null;
		nrDecoys = null;
		scoreAtThreshold = null;
		artificialDecoyFDR = null;
	}
	
	
	/**
	 * Set the decoyStrategy by the decoy String
	 * @param strategyString
	 */
	public void setDecoyStrategy(DecoyStrategy strategy) {
		if ((strategy == null) || (strategy != decoyStrategy)) {
			nullAll();
		}
		
		this.decoyStrategy = strategy;
	}
	
	
	/**
	 * getter for the decoy strategy
	 * @return
	 */
	public DecoyStrategy getDecoyStrategy() {
		return decoyStrategy;
	}
	
	
	/**
	 * Setter for the decoyPattern.
	 * If the pattern changes, delete all the results.
	 * 
	 * @param pattern
	 */
	public void setDecoyPattern(String pattern) {
		if ((pattern == null) || !this.decoyPattern.equals(pattern)) {
			nullAll();
		}
		
		this.decoyPattern = pattern;
	}
	
	
	/**
	 * Getter for the decoyPattern.
	 * @return
	 */
	public String getDecoyPattern() {
		return decoyPattern;
	}
	
	
	/**
	 * Setter for the fdrThreshold.
	 * If the threshold changes, delete all the results.
	 * 
	 * @param pattern
	 */
	public void setFDRThreshold(Double threshold) {
		if (!this.fdrThreshold.equals(threshold)) {
			nullAll();
		}
		
		this.fdrThreshold = threshold;
	}
	
	
	/**
	 * Getter for the fdrThreshold.
	 * @return
	 */
	public Double getFDRThreshold() {
		return fdrThreshold;
	}
	
	
	/**
	 * Getter for the nrItems.
	 * @return
	 */
	public Integer getNrItems() {
		return nrItems;
	}
	
	
	/**
	 * Getter for the number of FDR good target items.
	 * @return
	 */
	public Integer getNrFDRGoodTargets() {
		return nrFDRGoodTargets;
	}
	
	
	/**
	 * Getter for the number of FDR good decoy items.
	 * @return
	 */
	public Integer getNrFDRGoodDecoys() {
		return nrFDRGoodDecoys;
	}
	
	
	/**
	 * Getter for the number of FDR good target plus decoy items.
	 * @return
	 */
	public Integer getNrFDRGoodItems() {
		if ((nrFDRGoodTargets != null) && (nrFDRGoodDecoys != null)) {
			return nrFDRGoodTargets + nrFDRGoodDecoys;
		} else {
			return null;
		}
	}
	
	
	/**
	 * Getter for the number of targets.
	 * @return
	 */
	public Integer getNrTargets() {
		return nrTargets;
	}
	
	
	/**
	 * Getter for the nrDecoys.
	 * @return
	 */
	public Integer getNrDecoys() {
		return nrDecoys;
	}
	
	
	/**
	 * Correct the numbers of decoys and targets, e.g. after CombinedFDRScore
	 * calculation.
	 * 
	 * @param nrDecoys
	 * @param nrFDRGoodDecoys
	 * @param nrFDRGoodTargets
	 * @param nrItems
	 * @param nrTargets
	 */
	public void correctNumbers(int nrDecoys, int nrFDRGoodDecoys,
			int nrFDRGoodTargets, int nrItems, int nrTargets) {
		this.nrDecoys = nrDecoys;
		this.nrFDRGoodDecoys = nrFDRGoodDecoys;
		this.nrFDRGoodTargets = nrFDRGoodTargets;
		this.nrItems = nrItems;
		this.nrTargets = nrTargets;
	}
	
	
	/**
	 * Getter for the scoreAtThreshold.
	 * @return
	 */
	public Double getScoreAtThreshold() {
		return scoreAtThreshold;
	}
	
	
	/**
	 * Setter for the scoreShortName.
	 * @return
	 */
	public void setScoreShortName(String score) {
		if ((score == null) || !score.equals(this.scoreShortName)) {
			nullAll();
		}
		
		scoreShortName = score;
	}
	
	
	/**
	 * Getter for the scoreShortName.
	 * @return
	 */
	public String getScoreShortName() {
		return scoreShortName;
	}
	
	
	/**
	 * Getter for the FDR value of an artificial decoy at the last position in
	 * the list.
	 * 
	 * @return
	 */
	public Double getArtificialDecoyFDR() {
		return artificialDecoyFDR;
	}
	
	
	/**
	 * Calculate the FDR on the given List of comparable objects, using the
	 * ordering given by the score set by the {@link #scoreShortName}.
	 * @param reportItems
	 */
	public <T extends FDRComputable> void calculateFDR(List<T> reportItems) {
		calculateFDR(reportItems,
				new ScoreComparator<>(scoreShortName));
	}
	
	
	/**
	 * Calculate the FDR on the given List of comparable objects, explicitly
	 * setting, if a higher score is better than a lower.
	 * @param reportItems
	 */
	public <T extends FDRComputable> void calculateFDR(List<T> reportItems,
			Boolean higherScoreBetter) {
		
		Comparator<T> comp;
		
		if (higherScoreBetter == null) {
			comp = new ScoreComparator<>(scoreShortName);
		} else {
			comp = new ScoreComparator<>(scoreShortName,
					higherScoreBetter);
		}
		
		calculateFDR(reportItems, comp);
	}
	
	
	/**
	 * Calculate the FDR on the given List of comparable objects, with the
	 * given comparator.
	 * 
	 * @param reportItems
	 */
	public <T extends FDRComputable> void calculateFDR(List<T> reportItems,
			Comparator<T> comparator) {
		if (scoreShortName == null)  {
			// if we don't have a score, abort here
			// TODO: warn at least
			logger.warn("No score set for FDR calculation!");
			return;
		}
		
		if (comparator == null)  {
			logger.warn("No comparator for FDR calculation!");
			return;
		}
		
		logger.info("calculating FDR in FDRData with " + scoreShortName +
				"\n\tstrategy " + decoyStrategy + 
				"\n\tpattern " + decoyPattern + 
				"\n\tfdrThreshold " + fdrThreshold);
		
		double fdr;
		T lastGoodScoreItem;
		
		Double rankScore;
		List<T> rankItems;
		
		// sort the items with the given comparator
		Collections.sort(reportItems, comparator);
		
		nrTargets = 0;
		nrDecoys = 0;
		
		rankScore = Double.NaN;
		lastGoodScoreItem = null;
		rankItems = new ArrayList<>();
		
		for (T item : reportItems) {
			if (!rankScore.equals(item.getScore(scoreShortName))) {
				// this is a new rank, calculate FDR
				if (!rankScore.equals(Double.NaN) && (nrTargets < 1)) {
					// only decoys until now -> set FDR to infinity
					fdr = Double.POSITIVE_INFINITY;
				} else {
					fdr = (double)nrDecoys / nrTargets;
				}
				
				if (fdr <= fdrThreshold) {
					lastGoodScoreItem = item;
				}
				
				for (FDRComputable rankItem : rankItems) {
					rankItem.setFDR(fdr);
				}
				
				rankScore = item.getScore(scoreShortName);
				rankItems = new ArrayList<>();
			}
			
			// check for decoy
			if (item.getIsDecoy()) {
				nrDecoys++;
			} else {
				nrTargets++;
			}
			
			rankItems.add(item);
		}
		
		// calculate the last rank
		if (nrTargets < 1) {
			// only decoys until now -> set FDR to infinity
			fdr = Double.POSITIVE_INFINITY;
			artificialDecoyFDR = Double.POSITIVE_INFINITY;
		} else {
			fdr = (double)nrDecoys / nrTargets;
			artificialDecoyFDR = (double)(nrDecoys + 1) / nrTargets;
		}
		if (fdr <= fdrThreshold) {
			lastGoodScoreItem = reportItems.get(reportItems.size()-1);
		}
		
		for (FDRComputable rankItem : rankItems) {
			rankItem.setFDR(fdr);
		}
		
		// iterate again to set FDR-good flags
		nrFDRGoodTargets = 0;
		nrFDRGoodDecoys = 0;
		for (T item : reportItems) {
			Integer comp;
			
			comp = comparator.compare(item, lastGoodScoreItem);
			
			if (comp <= 0) {
				item.setIsFDRGood(true);
				if (!item.getIsDecoy()) {
					nrFDRGoodTargets++;
				} else {
					nrFDRGoodDecoys++;
				}
			} else {
				item.setIsFDRGood(false);
			}
			
		}
		
		nrItems = reportItems.size();
		if (lastGoodScoreItem != null) {
			scoreAtThreshold =
					lastGoodScoreItem.getScore(scoreShortName);
		}
		
		// at last calculate the q-values
		// for this, iterate backwards through the list
		ListIterator<T> it  = reportItems.listIterator(reportItems.size());
		FDRComputable item;
		Double qValue = Double.NaN;
		
		while (it.hasPrevious()) {
			item = it.previous();
			
			if ((qValue.compareTo(Double.NaN) == 0) ||
					(item.getFDR() < qValue)) {
				qValue = item.getFDR();
			}
			
			item.setQValue(qValue);
		}
	}
}
