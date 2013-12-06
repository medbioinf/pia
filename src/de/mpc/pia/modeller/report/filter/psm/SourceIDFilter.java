package de.mpc.pia.modeller.report.filter.psm;

import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterType;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;

public class SourceIDFilter extends AbstractFilter {
	
	protected static final String shortName = RegisteredFilters.SOURCE_ID_FILTER.getShortName();
	
	protected static final String name = "Source ID Filter";
	
	protected static final String filteringName = "Source ID (PSM)";
	
	public static final FilterType filterType = FilterType.literal;
	
	protected static final Class<String> valueInstanceClass = String.class;
	
	private String value;
	
	
	
	public SourceIDFilter(FilterComparator arg, String value, boolean negate) {
		this.comparator = arg;
		this.value = value;
		this.negate = negate;
	}
	
	
	@Override
	public String getShortName() {
		return shortName;
	}
	
	
	public static String shortName() {
		return shortName;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	
	public static String name() {
		return name;
	}
	
	@Override
	public String getFilteringName() {
		return filteringName;
	}
	
	
	public static String filteringName() {
		return filteringName;
	}
	
	@Override
	public Object getFilterValue() {
		return value;
	}
	
	@Override
	public FilterType getFilterType() {
		return filterType;
	}
	
	
	public static boolean isCorrectValueInstance(Object value) {
		return valueInstanceClass.isInstance(value);
	}
	
	@Override
	public Object getObjectsValue(Object o) {
		if (o instanceof PSMReportItem) {
			// if we get an ReportItem, return its charge value
			return ((PSMReportItem)o).getSourceID();
		} else if (o instanceof String) {
			// if we get an Number, just return it
			return o;
		} else {
			// nothing supported
			return null;
		}
	}
	
	@Override
	public boolean supportsClass(Object c) {
		if (c instanceof PSMReportItem) {
			return true;
		} else {
			return false;
		}
	}
}
