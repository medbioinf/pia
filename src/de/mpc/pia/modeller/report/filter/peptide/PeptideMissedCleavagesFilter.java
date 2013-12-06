package de.mpc.pia.modeller.report.filter.peptide;

import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterType;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;

public class PeptideMissedCleavagesFilter extends AbstractFilter {
	
	protected static final String shortName = RegisteredFilters.PEPTIDE_MISSED_CLEAVAGES_FILTER.getShortName();
	
	protected static final String name = "Missed Cleavages Filter for Peptide";
	
	protected static final String filteringName = "Missed Cleavages (Peptide)";
	
	public static final FilterType filterType = FilterType.numerical;
	
	protected static final Class<Number> valueInstanceClass = Number.class;
	
	private Integer value;
	
	
	
	public PeptideMissedCleavagesFilter(FilterComparator arg, Integer value, boolean negate) {
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
			return ((ReportPeptide) o).getMissedCleavages();
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
		if (c instanceof ReportPeptide) {
			return true;
		} else {
			return false;
		}
	}
}
