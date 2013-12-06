package de.mpc.pia.modeller.report.filter.psm;

import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterType;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;

public class PSMSequenceFilter extends AbstractFilter {
	
	protected static final String shortName = RegisteredFilters.PSM_SEQUENCE_FILTER.getShortName();
	
	protected static final String name = "Sequence Filter for PSM";
	
	protected static final String filteringName = "Sequence (PSM)";
	
	public static final FilterType filterType = FilterType.literal;
	
	protected static final Class<String> valueInstanceClass = String.class;
	
	private String value;
	
	
	
	public PSMSequenceFilter(FilterComparator arg, String value, boolean negate) {
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
			return ((PSMReportItem)o).getSequence();
		} else if (o instanceof String) {
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
