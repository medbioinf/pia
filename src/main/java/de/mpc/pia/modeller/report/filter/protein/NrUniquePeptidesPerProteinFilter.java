package de.mpc.pia.modeller.report.filter.protein;

import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterType;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;

public class NrUniquePeptidesPerProteinFilter extends AbstractFilter {
	
	protected static final String shortName = RegisteredFilters.NR_UNIQUE_PEPTIDES_PER_PROTEIN_FILTER.getShortName();
	
	protected static final String name = "#Unique peptides per Protein Filter";
	
	protected static final String filteringName = "#Unique peptides per Protein";
	
	public static final FilterType filterType = FilterType.numerical;
	
	protected static final Class<Number> valueInstanceClass = Number.class;
	
	private Integer value;
	
	
	
	public NrUniquePeptidesPerProteinFilter(FilterComparator arg, Integer value, boolean negate) {
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
		if (o instanceof ReportProtein) {
			Integer nrUnique = 0;
			
			for (ReportPeptide peptide : ((ReportProtein) o).getPeptides()) {
				for (PSMReportItem psmItem : peptide.getPSMs()) {
					Boolean isUnique = null;
					
					if (psmItem instanceof ReportPSMSet) {
						for (ReportPSM psm : ((ReportPSMSet) psmItem).getPSMs()) {
							isUnique = psm.getSpectrum().getIsUnique();
							if (isUnique != null) {
								// one definitely set PSM is enough
								break;
							}
						}
					} else if (psmItem instanceof ReportPSM) {
						isUnique = ((ReportPSM) psmItem).getSpectrum().getIsUnique(); 
					}
					
					if ((isUnique != null) && isUnique) {
						// peptide is unique: increase and go to next peptide
						nrUnique++;
						break;
					}
				}
			}
			
			return nrUnique;
			
		} else if (o instanceof Number) {
			// if we get a Number, just return it
			return o;
		} else {
			// nothing supported
			return null;
		}
	}
	
	@Override
	public boolean supportsClass(Object c) {
		if (c instanceof ReportProtein) {
			return true;
		} else {
			return false;
		}
	}
}
