package de.mpc.pia.modeller.report.filter.protein;

import java.util.ArrayList;
import java.util.List;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterType;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;

public class ProteinModificationsFilter extends AbstractFilter {
	
	protected static final String shortName = RegisteredFilters.PROTEIN_MODIFICATIONS_FILTER.getShortName();
	
	protected static final String name = "Modifications Filter for Protein";
	
	protected static final String filteringName = "Modifications (Protein)";
	
	public static final FilterType filterType = FilterType.modification;
	
	protected static final Class<String> valueInstanceClass = String.class;
	
	private String value;
	
	
	
	public ProteinModificationsFilter(FilterComparator arg, String value, boolean negate) {
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
			List<Modification> modList = new ArrayList<Modification>();
			for (ReportPeptide pep : ((ReportProtein) o).getPeptides()) {
				modList.addAll(pep.getModificationsList());
			}
			return modList;
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
		if (c instanceof ReportProtein) {
			return true;
		} else {
			return false;
		}
	}
}
