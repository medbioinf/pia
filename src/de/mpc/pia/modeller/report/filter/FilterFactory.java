package de.mpc.pia.modeller.report.filter;

import java.util.ArrayList;
import java.util.List;

import de.mpc.pia.modeller.report.filter.peptide.PeptideScoreFilter;
import de.mpc.pia.modeller.report.filter.protein.ProteinScoreFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMScoreFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMTopIdentificationFilter;



/**
 * This class contains some helper methods to instantiate
 * {@link AbstractFilter}s and their subclasses for filtering the reports.
 * 
 * @author julian
 *
 */
public class FilterFactory {
	
	/**
	 * We don't ever want to instantiate this class
	 */
	private FilterFactory() {
		throw new AssertionError();
	}
	
	
	/**
	 * gets the available comparators for the filter given by the short name
	 * 
	 * @param filterShort
	 * @return
	 */
	public static List<FilterComparator> getAvailableComparators(String filterShort) {
		if (filterShort != null) {
			FilterType type = getFilterType(filterShort);
			if (type != null) {
				return type.getAvailableComparators();
			}
		}
		
		return new ArrayList<FilterComparator>();
	}
	
	
	/**
	 * Builds a new instance of the filter type, given by the short name.
	 * The comparatorName must be a {@link FilterComparator}'s name valid for
	 * this filter type and the input of a valid type.
	 * 
	 * @param filterShort
	 * @param comparatorName
	 * @param input
	 * @param negate
	 * @param messageBuffer
	 * @return
	 */
	public static AbstractFilter newInstanceOf(String filterShort,
			String comparatorName, String input, boolean negate,
			StringBuffer messageBuffer) {
		
		FilterComparator comparator = FilterComparator.getFilterComparatorByName(comparatorName);
		if (comparator == null) {
			// no comparator selected
			messageBuffer.append("Please select a comparator!");
			return null;
		}
		
		return newInstanceOf(filterShort, comparator, input, negate,
				messageBuffer);
	}
	
	
	/**
	 * Builds a new instance of the filter type, given the comparator.
	 * The comparatorName must be a {@link FilterComparator}'s name valid for
	 * this filter type and the input of a valid type.
	 * 
	 * @param filterShort
	 * @param comparatorName
	 * @param input
	 * @param negate
	 * @param messageBuffer
	 * @return
	 */
	public static AbstractFilter newInstanceOf(String filterShort,
			FilterComparator comparator, String input, boolean negate,
			StringBuffer messageBuffer) {
		
		if ((filterShort == null) ||
				(filterShort.equals("null"))) {
			// no filter selected
			messageBuffer.append("Please select a filter!");
			return null;
		}
		
		if (comparator == null) {
			// no comparator selected
			messageBuffer.append("Please select a comparator!");
			return null;
		}
		
		Object value = null;
		switch (FilterFactory.getFilterType(filterShort)) {
		case numerical:
			// try to convert the input into a numerical value, Float should do
			try {
				value = Double.parseDouble(input);
			} catch (NumberFormatException e) {
				messageBuffer.append("please enter a numerical value");
				value = null;
			}
			break;
			
		case bool:
			value = Boolean.parseBoolean(input);
			break;
			
		case literal:
		case literal_list:
		case modification:
			value = input;
			break;
		}
		
		if ((filterShort != null) && (value != null)) {
			if ((filterShort.startsWith(PSMScoreFilter.prefix)) &&
					(value instanceof Number)) {
				return new PSMScoreFilter(comparator,
						((Number)value).doubleValue(), negate,
						filterShort.substring(PSMScoreFilter.prefix.length()));
			} else if ((filterShort.startsWith(PeptideScoreFilter.prefix)) &&
					(value instanceof Number)) {
				return new PeptideScoreFilter(comparator,
						((Number)value).doubleValue(), negate,
						filterShort.substring(PeptideScoreFilter.prefix.length()));
			} else if ((filterShort.equals(ProteinScoreFilter.shortName)) &&
					(value instanceof Number)) {
				return new ProteinScoreFilter(comparator,
						((Number)value).doubleValue(), negate);
			} else if ((filterShort.startsWith(PSMTopIdentificationFilter.prefix)) &&
					(value instanceof Number)) {
				return new PSMTopIdentificationFilter(comparator,
						((Number)value).intValue(), negate,
						filterShort.substring(PSMTopIdentificationFilter.prefix.length()));
			} else {
				for (RegisteredFilters filter : RegisteredFilters.values()) {
					if (filter.getShortName().equals(filterShort) &&
							filter.isCorrectValueInstance(value)) {
						return filter.newInstanceOf(comparator, value, negate);
					}
				}
			}
		}
		
		return null;
	}
	
	
	/**
	 * Returns the FilterType of the given filter.
	 * 
	 * @param filterShort
	 * @return
	 */
	public static FilterType getFilterType(String filterShort) {
		if (filterShort != null) {
			if (filterShort.startsWith(PSMScoreFilter.prefix)) {
				return PSMScoreFilter.filterType;
			} else if (filterShort.startsWith(PeptideScoreFilter.prefix)) {
				return PeptideScoreFilter.filterType;
			} else if (filterShort.equals(ProteinScoreFilter.shortName)) {
				return ProteinScoreFilter.filterType;
			} else if (filterShort.startsWith(PSMTopIdentificationFilter.prefix)) {
				return PSMTopIdentificationFilter.filterType;
			} else {
				for (RegisteredFilters filter : RegisteredFilters.values()) {
					if (filter.getShortName().equals(filterShort)) {
						return filter.getFilterType();
					}
				}
			}
		}
		return null;
	}
	
	
	/**
	 * Applies the filtering given by the filters to the given List of
	 * Filterables and returns the filtered list.
	 * 
	 * @param <T>
	 * @param reportItems
	 * @param filters
	 * @param fileID
	 * @return
	 */
	public static <T extends Filterable> List<T> applyFilters(
			List<T> reportItems, List<AbstractFilter> filters, Long fileID) {
		if ((filters != null) && (filters.size() > 0)) {
			List<T> filteredReportItems = new ArrayList<T>();
			
			if (reportItems != null) {
				for (T item : reportItems) {
					if (satisfiesFilterList(item, fileID, filters)) {
						filteredReportItems.add(item);
					}
				}
			}
			
			return filteredReportItems;
		} else {
			return reportItems;
		}
	}
	
	
	/**
	 * Applies the filtering given by the filters to the given List of
	 * Filterables and returns the filtered list. Uses 0L as fileID.
	 * 
	 * @param <T>
	 * @param reportItems
	 * @param filters
	 * @return
	 */
	public static <T extends Filterable> List<T> applyFilters(List<T> reportItems,
			List<AbstractFilter> filters) {
		return applyFilters(reportItems, filters, 0L);
	}
	

	/**
	 * Checks whether all the inference filters in the given List are satisfied
	 * for the filterable object.
	 * 
	 * @param o
	 * @return
	 */
	public static <T extends Filterable> boolean satisfiesFilterList(
			T item, Long fileID,
			List<AbstractFilter> filters) {
		boolean satisfiesAllFilters = true;
		
		for (AbstractFilter filter : filters) {
			if (filter.supportsClass(item)) {
				satisfiesAllFilters &= filter.satisfiesFilter(item, fileID);
			}
		}
		
		return satisfiesAllFilters;
	}
}
