package de.mpc.pia.modeller.report.filter.psm;

import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterType;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;

public class DeltaPPMFilter extends AbstractFilter {
	
	protected static final String shortName = RegisteredFilters.DELTA_PPM_FILTER.getShortName();
	
	protected static final String name = "dPPM Filter";
	
	protected static final String filteringName = "dPPM (PSM)";
	
	public static final FilterType filterType = FilterType.numerical;
	
	protected static final Class<Number> valueInstanceClass = Number.class;
	
	private Double value;
	
	
	
	public DeltaPPMFilter(FilterComparator arg, Double value, boolean negate) {
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
			return ((PSMReportItem)o).getDeltaPPM();
		} else if (o instanceof Number) {
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
