package de.mpc.pia.modeller.report.filter.peptide;

import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterType;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;

public class PeptideUniqueFilter extends AbstractFilter {
	
	protected static final String shortName = RegisteredFilters.PEPTIDE_UNIQUE_FILTER.getShortName();
	
	protected static final String name = "Unique Filter for Peptide";
	
	protected static final String filteringName = "Unique (Peptide)";
	
	public static final FilterType filterType = FilterType.bool;
	
	protected static final Class<Boolean> valueInstanceClass = Boolean.class;
	
	private Boolean value;
	
	
	
	public PeptideUniqueFilter(FilterComparator arg, Boolean value, boolean negate) {
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
			Boolean isUnique = null;
			
			PSMReportItem psm = ((ReportPeptide) o).getPSMs().iterator().next();
			if (psm instanceof ReportPSMSet) {
				isUnique = ((ReportPSMSet) psm).getPSMs().iterator().next().
						getSpectrum().getIsUnique();
			} else if (psm instanceof ReportPSM) {
				isUnique = ((ReportPSM) psm).getSpectrum().getIsUnique();
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
		if (c instanceof ReportPeptide) {
			return true;
		} else {
			return false;
		}
	}
}
