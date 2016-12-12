package de.mpc.pia.modeller.peptide;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mpc.pia.modeller.report.SortOrder;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.modeller.score.comparator.RankComparator;
import de.mpc.pia.modeller.score.comparator.ScoreComparator;


/**
 * Handles {@link Comparator}s for {@link ReportPeptide}s.
 * 
 * @author julian
 *
 */
public class ReportPeptideComparatorFactory {
	
	/**
	 * the types of sorting available for a {@link ReportPeptide}
	 * 
	 * @author julian
	 *
	 */
	private enum CompareType {
		/**
		 * sort the report peptides by their sequences
		 */
		SEQUENCE_SORT {
			@Override
			public Comparator<ReportPeptide> getNewInstance() {
				return (o1, o2) -> o1.getSequence().compareTo(o2.getSequence());
			}

			@Override
			public Comparator<ReportPeptide> getNewInstance(String value) {
				return null;
			}
			
			@Override
			public String toString() {
				return "sequence";
			}
		},
		/**
		 * sort the report peptides by their missed cleavages (only use on single
		 * file!)
		 */
		MISSED_SORT {
			@Override
			public Comparator<ReportPeptide> getNewInstance() {
				return (o1, o2) -> Integer.valueOf(o1.getMissedCleavages()).compareTo(o2.getMissedCleavages());
			}

			@Override
			public Comparator<ReportPeptide> getNewInstance(String value) {
				return null;
			}
			
			@Override
			public String toString() {
				return "missed";
			}
		},
		/**
		 * sort the report peptides by their rank
		 */
		RANK_SORT {
			@Override
			public Comparator<ReportPeptide> getNewInstance() {
				return new RankComparator<>();
			}

			@Override
			public Comparator<ReportPeptide> getNewInstance(String value) {
				return null;
			}
			
			@Override
			public String toString() {
				return "rank";
			}
		},
		/**
		 * sort the report peptides by their number of PSMs
		 */
		NR_PSMS_SORT {
			@Override
			public Comparator<ReportPeptide> getNewInstance() {
				return (o1, o2) -> o1.getNrPSMs().compareTo(o2.getNrPSMs());
			}

			@Override
			public Comparator<ReportPeptide> getNewInstance(String value) {
				return null;
			}
			
			@Override
			public String toString() {
				return "nr_psms";
			}
		},
		/**
		 * sort the report peptides by their number of PSMs
		 */
		NR_SPECTRA_SORT {
			@Override
			public Comparator<ReportPeptide> getNewInstance() {
				return (o1, o2) -> o1.getNrSpectra().compareTo(o2.getNrSpectra());
			}

			@Override
			public Comparator<ReportPeptide> getNewInstance(String value) {
				return null;
			}
			
			@Override
			public String toString() {
				return "nr_spectra";
			}
		},
		
		/**
		 * sort by the score with a given name
		 */
		SCORE_SORT {
			@Override
			public Comparator<ReportPeptide> getNewInstance() {
				return null;
			}
			
			@Override
			public Comparator<ReportPeptide> getNewInstance(final String scoreName) {
				return new ScoreComparator<>(scoreName);
			}
			
			@Override
			public String toString() {
				return "score_comparator";
			}
		},
		;
		
		
		/**
		 * Returns a new instance of the given type.
		 * 
		 * @return
		 */
		public abstract Comparator<ReportPeptide> getNewInstance();
		
		
		/**
		 * Returns a new instance of the given type and uses the value for
		 * initialization.
		 *  
		 * @param value
		 * @return
		 */
		public abstract Comparator<ReportPeptide> getNewInstance(final String value);
	}
	
	
	/** the prefix before all score tags */
	public final static String score_prefix = "score_";
	
	
	/**
	 * invert the ordering
	 * 
	 * @param other
	 * @return
	 */
	public static Comparator<ReportPeptide> descending(final Comparator<ReportPeptide> other) {
		return (o1, o2) -> -1 * other.compare(o1, o2);
    }
	
	
	/**
	 * returns a Comparator for multiple options.
	 * 
	 * @param multipleOptions
	 * @return
	 */
	public static Comparator<ReportPeptide> getComparator(final List<Comparator<ReportPeptide>> multipleOptions) {
		return (o1, o2) -> {
            int result;
            // check all options, the first not returning 0 (equal) gets returned
            for (Comparator<ReportPeptide> option : multipleOptions) {
                result = option.compare(o1, o2);
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        };
	}
	
	
	/**
	 * returns the comparator given by its name using the given order.
	 * 
	 * @param name
	 * @param order
	 * @return
	 */
	public static Comparator<ReportPeptide> getComparatorByName(String name, SortOrder order) {
		
		for (ReportPeptideComparatorFactory.CompareType comp : CompareType.values()) {
			if (name.equals(comp.toString())) {
				return (order.equals(SortOrder.ascending)) ? 
						comp.getNewInstance() :
						descending(comp.getNewInstance());
			}
		}
		
		// it still may be a score comparator
		if (name.startsWith(score_prefix)) {
			String scoreName = name.substring(6);
			Comparator<ReportPeptide> comp = ReportPeptideComparatorFactory.CompareType.SCORE_SORT.getNewInstance(scoreName);
			
			return (order.equals(SortOrder.ascending)) ?
					comp :
					descending(comp);
		}
		
		return null;
	}
	
	
	/**
	 * returns a mapping from the description strings of all the available
	 * sortings to SortOrder.unsorted, except for the scores
	 * @return
	 */
	public static Map<String, SortOrder> getInitialSortOrders() {
		Map<String, SortOrder> orders = new HashMap<>();
		
		for (ReportPeptideComparatorFactory.CompareType comp : CompareType.values()) {
			if (!comp.toString().startsWith(score_prefix)) {
				orders.put(comp.toString(), SortOrder.unsorted);
			}
		}
		
		return orders;
	}
	
	
	/**
	 * Returns the sorting name name of the score with the given shortName.
	 * If no score with the given name exists, null is returned.
	 * @return
	 */
	public static String getScoreSortName(String shortName) {
		if (ScoreModelEnum.getName(shortName) != null) {
			return score_prefix + shortName;
		} else {
			return null;
		}
	}
}
