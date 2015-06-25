package de.mpc.pia.modeller.report.filter.psm;

import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterType;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;

public class PSMUniqueFilter extends AbstractFilter {
	
	protected static final String shortName = RegisteredFilters.PSM_UNIQUE_FILTER.getShortName();
	
	protected static final String name = "Unique Filter for PSM";
	
	protected static final String filteringName = "Unique (PSM)";
	
	public static final FilterType filterType = FilterType.bool;
	
	protected static final Class<Boolean> valueInstanceClass = Boolean.class;
	
	private Boolean value;
	
	
	
	public PSMUniqueFilter(FilterComparator arg, Boolean value, boolean negate) {
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
			Boolean isUnique = null;
			if (o instanceof ReportPSMSet) {
				isUnique = ((ReportPSMSet) o).getPSMs().get(0).getSpectrum().getIsUnique();
			} else if (o instanceof ReportPSM) {
				isUnique = ((ReportPSM) o).getSpectrum().getIsUnique();
			}
			
			if (isUnique != null) {
				return isUnique;
			} else {
				return new Boolean(false);
			}
		} else if (o instanceof Boolean) {
			return o;
		}
		
		// nothing supported
		return null;
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
