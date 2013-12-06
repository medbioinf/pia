package de.mpc.pia.modeller.report.filter.psm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterType;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;

public class PSMDescriptionFilter extends AbstractFilter {
	
	protected static final String shortName = RegisteredFilters.PSM_DESCRIPTION_FILTER.getShortName();
	
	protected static final String name = "Description Filter for PSM";
	
	protected static final String filteringName = "Description (PSM)";
	
	public static final FilterType filterType = FilterType.literal_list;
	
	protected static final Class<String> valueInstanceClass = String.class;
	
	private String value;
	
	
	
	public PSMDescriptionFilter(FilterComparator arg, String value, boolean negate) {
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
			return ((PSMReportItem) o).getAccessions();
		} else if (o instanceof List<?>) {
			List<String> objList = new ArrayList<String>();
			for (Object obj : (List<?>)o) {
				if (obj instanceof String) {
					objList.add((String)obj);
				}
			}
			return objList;
		}
		
		// nothing supported
		return null;
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
				if (obj instanceof Accession) {
					if ((fileID > 0) && (((Accession)obj).foundInFile(fileID))) {
						strList.add(((Accession)obj).getDescription(fileID));
					} else if (fileID == 0) {
						for (Map.Entry<Long, String> descIt : ((Accession)obj).getDescriptions().entrySet()) {
							strList.add(descIt.getValue());
						}
					}
				} else if (obj instanceof String) {
					strList.add((String)obj);
				}
			}
		}
		
		return strList;
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
