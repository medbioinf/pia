package de.mpc.pia.modeller.report.filter.peptide;

import java.util.ArrayList;
import java.util.List;

import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterType;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;

public class PeptideModificationsFilter extends AbstractFilter {
	
	protected static final String shortName = RegisteredFilters.PEPTIDE_MODIFICATIONS_FILTER.getShortName();
	
	protected static final String name = "Modifications Filter for Peptide";
	
	protected static final String filteringName = "Modifications (Peptide)";
	
	public static final FilterType filterType = FilterType.modification;
	
	protected static final Class<String> valueInstanceClass = String.class;
	
	private String value;
	
	
	
	public PeptideModificationsFilter(FilterComparator arg, String value, boolean negate) {
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
		if (o instanceof ReportPeptide) {
			return ((ReportPeptide) o).getModificationsList();
		} else if (o instanceof List<?>) {
			List<Modification> modList = new ArrayList<Modification>();
			for (Object obj : (List<?>)o) {
				if (obj instanceof Modification) {
					modList.add((Modification)obj);
				}
			}
			return modList;
		}
		
		// nothing supported
		return null;
	}

	@Override
	public boolean supportsClass(Object c) {
		if (c instanceof ReportPeptide) {
			return true;
		} else {
			return false;
		}
	}
}
