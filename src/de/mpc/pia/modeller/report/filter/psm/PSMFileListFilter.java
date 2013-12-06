package de.mpc.pia.modeller.report.filter.psm;

import java.util.ArrayList;
import java.util.List;

import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterType;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;

public class PSMFileListFilter extends AbstractFilter {
	
	protected static final String shortName = RegisteredFilters.PSM_FILE_LIST_FILTER.getShortName();
	
	protected static final String name = "File List Filter for PSM";
	
	protected static final String filteringName = "File List (PSM)";
	
	public static final FilterType filterType = FilterType.literal_list;
	
	protected static final Class<String> valueInstanceClass = String.class;
	
	private String value;
	
	
	
	public PSMFileListFilter(FilterComparator arg, String value, boolean negate) {
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
		if (o instanceof ReportPSMSet) {
			// if we get an ReportPSM, return its PSMs
			return ((ReportPSMSet)o).getPSMs();
		} else if (o instanceof List<?>) {
			List<ReportPSM> objList = new ArrayList<ReportPSM>();
			for (Object obj : (List<?>)o) {
				if (obj instanceof ReportPSM) {
					objList.add((ReportPSM)obj);
				}
			}
			return objList;
		} else {
			// nothing supported
			return null;
		}
	}
	
	@Override
	public boolean valueNeedsFileRefinement() {
		return true;
	}
	
	
	@Override
	public Object doFileRefinement(Long fileID, Object o) {
		List<String> strList = new ArrayList<String>();
		
		if (o instanceof List<?>) {
			for (Object obj : (List<?>)o) {
				if (obj instanceof ReportPSM) {
					strList.add(((ReportPSM)obj).getFile().getName());
				} else if (obj instanceof String) {
					strList.add((String)obj);
				}
			}
		}
		
		return strList;
	}
	
	@Override
	public boolean supportsClass(Object c) {
		if (c instanceof ReportPSMSet) {
			return true;
		} else {
			return false;
		}
	}
}
