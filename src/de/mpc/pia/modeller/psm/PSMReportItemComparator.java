package de.mpc.pia.modeller.psm;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mpc.pia.modeller.report.SortOrder;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.modeller.score.comparator.RankComparator;
import de.mpc.pia.modeller.score.comparator.ScoreComparator;
import de.mpc.pia.tools.PIATools;


public enum PSMReportItemComparator implements Comparator<PSMReportItem> {
	
	RANK_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			RankComparator<PSMReportItem> comparator =
					new RankComparator<PSMReportItem>();
			
			return comparator.compare(o1, o2);
		}
		
		public String toString() {
			return "rank";
		}
	},
	SEQUENCE_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			return o1.getSequence().compareTo(o2.getSequence());
		}
		
		public String toString() {
			return "sequence";
		}
	},
	CHARGE_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			return Integer.valueOf(o1.getCharge()).compareTo(o2.getCharge());
		}
		
		public String toString() {
			return "charge";
		}
	},
	NR_PSMS_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			Integer o1_nr = 1;
			Integer o2_nr = 1;
			
			if (o1 instanceof ReportPSMSet) {
				o1_nr = ((ReportPSMSet) o1).getPSMs().size();
			}
			
			if (o2 instanceof ReportPSMSet) {
				o2_nr = ((ReportPSMSet) o2).getPSMs().size();
			}
			
			return o1_nr.compareTo(o2_nr);
		}
		
		public String toString() {
			return "nr_psms";
		}
	},
	MASS_TO_CHARGE_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			return Double.valueOf(o1.getMassToCharge()).compareTo(o2.getMassToCharge());
		}
		
		public String toString() {
			return "masstocharge";
		}
	},
	MISSED_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			return Integer.valueOf(o1.getMissedCleavages()).compareTo(o2.getMissedCleavages());
		}
		
		public String toString() {
			return "missed";
		}
	},
	SOURCE_ID_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			return PIATools.CompareProbableNulls(o1.getSourceID(),
					o2.getSourceID());
		}
		
		public String toString() {
			return "source_id";
		}
	},
	SPECTRUM_TITLE_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			return PIATools.CompareProbableNulls(o1.getSpectrumTitle(),
					o2.getSpectrumTitle());
		}
		
		public String toString() {
			return "spectrum_title";
		}
	},
	DELTA_MASS_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			return Double.valueOf(o1.getDeltaMass()).compareTo(o2.getDeltaMass());
		}
		
		public String toString() {
			return "deltamass";
		}
	},
	DELTA_PPM_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			return Double.valueOf(o1.getDeltaPPM()).compareTo(o2.getDeltaPPM());
		}
		
		public String toString() {
			return "deltappm";
		}
	},
	RETENTION_TIME_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			return PIATools.CompareProbableNulls(o1.getRetentionTime(),
					o2.getRetentionTime());
		}
		
		public String toString() {
			return "retention_time";
		}
	},
	
	// the scores
	PSM_FDR_SCORE_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName();
		}
	},
	PSM_Q_VALUE_SCORE_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.PSM_LEVEL_Q_VALUE.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.PSM_LEVEL_Q_VALUE.getShortName();
		}
	},
	AVERAGE_FDR_SCORE_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.AVERAGE_FDR_SCORE.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.AVERAGE_FDR_SCORE.getShortName();
		}
	},
	COMBINED_FDR_SCORE_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName();
		}
	},
	MASCOT_SCORE_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.MASCOT_SCORE.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.MASCOT_SCORE.getShortName();
		}
	},
	MASCOT_EXPECT_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.MASCOT_EXPECT.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.MASCOT_EXPECT.getShortName();
		}
	},
	SEQUEST_PROBABILITY_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.SEQUEST_PROBABILITY.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.SEQUEST_PROBABILITY.getShortName();
		}
	},
	SEQUEST_SPSCORE_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.SEQUEST_SPSCORE.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.SEQUEST_SPSCORE.getShortName();
		}
	},
	SEQUEST_XCORR_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.SEQUEST_XCORR.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.SEQUEST_XCORR.getShortName();
		}
	},
	XTANDEM_EXPECT_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.XTANDEM_EXPECT.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.XTANDEM_EXPECT.getShortName();
		}
	},
	XTANDEM_HYPERSCORE_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.XTANDEM_HYPERSCORE.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.XTANDEM_HYPERSCORE.getShortName();
		}
	},
	MSGF_RAWSCORE_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.MSGF_RAWSCORE.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.MSGF_RAWSCORE.getShortName();
		}
	},
	MSGF_DENOVOSCORE_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.MSGF_DENOVOSCORE.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.MSGF_DENOVOSCORE.getShortName();
		}
	},
	MSGF_SPECEVALUE_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.MSGF_SPECEVALUE.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.MSGF_SPECEVALUE.getShortName();
		}
	},
	MSGF_EVALUE_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.MSGF_EVALUE.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.MSGF_EVALUE.getShortName();
		}
	},
	AMANDA_SCORE_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.AMANDA_SCORE.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.AMANDA_SCORE.getShortName();
		}
	},
	OPENMS_POSTERIOR_ERROR_PROBABILITY_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.OPENMS_POSTERIOR_ERROR_PROBABILITY.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.OPENMS_POSTERIOR_ERROR_PROBABILITY.getShortName();
		}
	},
	OPENMS_POSTERIOR_PROBABILITY_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.OPENMS_POSTERIOR_PROBABILITY.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.OPENMS_POSTERIOR_PROBABILITY.getShortName();
		}
	},
	OPENMS_CONSENSUS_PEPMATRIX_PEP_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.OPENMS_CONSENSUS_PEPMATRIX_POSTERIOR_ERROR_PROBABILITY.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.OPENMS_CONSENSUS_PEPMATRIX_POSTERIOR_ERROR_PROBABILITY.getShortName();
		}
	},
	
	
	FASTA_SEQUENCE_COUNT_SCORE_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.FASTA_SEQUENCE_COUNT.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.FASTA_SEQUENCE_COUNT.getShortName();
		}
	},
	FASTA_ACCESSION_COUNT_SCORE_SORT {
		public int compare(PSMReportItem o1, PSMReportItem o2) {
			ScoreComparator<PSMReportItem> comp =
					new ScoreComparator<PSMReportItem>(ScoreModelEnum.FASTA_ACCESSION_COUNT.getShortName());
			return comp.compare(o1, o2);
		}
		
		public String toString() {
			return score_prefix + ScoreModelEnum.FASTA_ACCESSION_COUNT.getShortName();
		}
	},
	;
	
	
	/** the prefix before all score tags */
	public final static String score_prefix = "score_";
	
	
	/**
	 * invert the ordering
	 * 
	 * @param other
	 * @return
	 */
	public static Comparator<PSMReportItem> descending(final Comparator<PSMReportItem> other) {
		return new Comparator<PSMReportItem>() {
			@Override
			public int compare(PSMReportItem o1, PSMReportItem o2) {
				return -1 * other.compare(o1, o2);
			}
		};
    }
	
	
	/**
	 * returns a Comparator for multiple options.
	 * 
	 * @param multipleOptions
	 * @return
	 */
	public static Comparator<PSMReportItem> getComparator(final List<Comparator<PSMReportItem>> multipleOptions) {
		return new Comparator<PSMReportItem>() {
			public int compare(PSMReportItem o1, PSMReportItem o2) {
				int result;
				// check all options, the first not returning 0 (equal) gets returned
				for (Comparator<PSMReportItem> option : multipleOptions) {
					result = option.compare(o1, o2);
					if (result != 0) {
						return result;
					}
				}
				return 0;
			}
		};
	}
	
	
	/**
	 * returns the comparator given by its name using the given order.
	 * 
	 * @param name
	 * @param order
	 * @return
	 */
	public static Comparator<PSMReportItem> getComparatorByName(String name, SortOrder order) {
		for (PSMReportItemComparator comp : values()) {
			if (name.equals(comp.toString())) {
				return (order.equals(SortOrder.ascending)) ? comp : descending(comp);
			}
		}
		
		return null;
	}
	
	
	/**
	 * returns a mapping from the description strings of all the available
	 * sortings to SortOrder.unsorted, except for the scores
	 * @return
	 */
	public static Map<String, SortOrder> getInitialSortOrders() {
		Map<String, SortOrder> orders = new HashMap<String, SortOrder>();
		
		for (PSMReportItemComparator comp : values()) {
			if (!comp.toString().startsWith(score_prefix)) {
				orders.put(comp.toString(), SortOrder.unsorted);
			}
		}
		
		return orders;
	}
	
	
	/**
	 * Returns the name of the score with the given shortName.<br/>
	 * The shortName has the score prefix, to distinguish it from a normal
	 * sorting field.
	 * @return
	 */
	public static String getScoreSortName(String shortName) {
		for (PSMReportItemComparator comp : values()) {
			if (comp.toString().equals(score_prefix + shortName)) {
				return comp.toString();
			}
		}
		
		return null;
	}
}
