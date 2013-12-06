package de.mpc.pia.modeller.protein;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mpc.pia.modeller.report.SortOrder;
import de.mpc.pia.modeller.score.comparator.RankComparator;


/**
 * Handles {@link Comparator}s for {@link ReportProtein}s.
 * 
 * @author julian
 *
 */
public class ReportProteinComparatorFactory {
	
	/**
	 * the types of sorting available for a {@link ReportPeptide}
	 * 
	 * @author julian
	 *
	 */
	public enum CompareType {
		/**
		 * sort the report proteins by their rank
		 */
		RANK_SORT {
			@Override
			public Comparator<ReportProtein> getNewInstance() {
				return new RankComparator<ReportProtein>();
			}
			
			@Override
			public String toString() {
				return "rank";
			}
		},
		
		/**
		 * sort the report proteins by their number of spectra
		 */
		NR_SPECTRA_SORT {
			@Override
			public Comparator<ReportProtein> getNewInstance() {
				return new Comparator<ReportProtein>() {
					@Override
					public int compare(ReportProtein o1, ReportProtein o2) {
						return o1.getNrSpectra().compareTo(o2.getNrSpectra());
					}
				};
			}
			
			@Override
			public String toString() {
				return "nr_spectra";
			}
		},
		/**
		 * sort the report proteins by their number of PSMs
		 */
		NR_PSMS_SORT {
			@Override
			public Comparator<ReportProtein> getNewInstance() {
				return new Comparator<ReportProtein>() {
					@Override
					public int compare(ReportProtein o1, ReportProtein o2) {
						return o1.getNrPSMs().compareTo(o2.getNrPSMs());
					}
				};
			}
			
			@Override
			public String toString() {
				return "nr_psms";
			}
		},
		/**
		 * sort the report proteins by their number of PSMs
		 */
		NR_PEPTIDES_SORT {
			@Override
			public Comparator<ReportProtein> getNewInstance() {
				return new Comparator<ReportProtein>() {
					@Override
					public int compare(ReportProtein o1, ReportProtein o2) {
						return o1.getNrPeptides().compareTo(o2.getNrPeptides());
					}
				};
			}
			
			@Override
			public String toString() {
				return "nr_peptides";
			}
		},
		
		/**
		 * sort by the score with a given name, interpreting a higher score as better
		 */
		SCORE_SORT {
			@Override
			public Comparator<ReportProtein> getNewInstance() {
				return new Comparator<ReportProtein>() {
					@Override
					public int compare(ReportProtein o1, ReportProtein o2) {
						if (o1.getScore().equals(Double.NaN) &&
								o2.getScore().equals(Double.NaN)) {
							return 0;
						} else if (o1.getScore().equals(Double.NaN)) {
							return 1;
						} else if (o2.getScore().equals(Double.NaN)) {
							return -1;
						}
						
						return o1.getScore().compareTo(o2.getScore());
					}
				};
			}
			
			@Override
			public String toString() {
				return "protein_score";
			}
		},
		
		/**
		 * sort by the score with a given name, interpreting a lower score as better
		 */
		SCORE_SORT_HIGHERSCOREBETTER {
			@Override
			public Comparator<ReportProtein> getNewInstance() {
				return new Comparator<ReportProtein>() {
					@Override
					public int compare(ReportProtein o1, ReportProtein o2) {
						if (o1.getScore().equals(Double.NaN) &&
								o2.getScore().equals(Double.NaN)) {
							return 0;
						} else if (o1.getScore().equals(Double.NaN)) {
							return 1;
						} else if (o2.getScore().equals(Double.NaN)) {
							return -1;
						}
						
						return -o1.getScore().compareTo(o2.getScore());
					}
				};
			}
			
			@Override
			public String toString() {
				return "protein_score_lowerscorebetter";
			}
		},
		;
		
		
		/**
		 * Returns a new instance of the given type.
		 * 
		 * @return
		 */
		public abstract Comparator<ReportProtein> getNewInstance();
		
	}
	
	
	/**
	 * invert the ordering
	 * 
	 * @param other
	 * @return
	 */
	public static Comparator<ReportProtein> descending(final Comparator<ReportProtein> other) {
		return new Comparator<ReportProtein>() {
			@Override
			public int compare(ReportProtein o1, ReportProtein o2) {
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
	public static Comparator<ReportProtein> getComparator(final List<Comparator<ReportProtein>> multipleOptions) {
		return new Comparator<ReportProtein>() {
			public int compare(ReportProtein o1, ReportProtein o2) {
				int result;
				// check all options, the first not returning 0 (equal) gets returned
				for (Comparator<ReportProtein> option : multipleOptions) {
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
	public static Comparator<ReportProtein> getComparatorByName(String name, SortOrder order) {
		
		for (ReportProteinComparatorFactory.CompareType comp : CompareType.values()) {
			if (name.equals(comp.toString())) {
				return (order.equals(SortOrder.ascending)) ? 
						comp.getNewInstance() :
						descending(comp.getNewInstance());
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
		
		for (ReportProteinComparatorFactory.CompareType comp : CompareType.values()) {
			orders.put(comp.toString(), SortOrder.unsorted);
		}
		
		return orders;
	}
}
