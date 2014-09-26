package de.mpc.pia.modeller.report.filter.protein;

import java.util.List;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterType;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;

/**
 * This filter checks for protein groups, which have at least the given number
 * of peptides which are unique for the protein group. These peptides are only
 * identified in the proteins of the protein group.
 * 
 * @author julian
 *
 */
public class NrGroupUniquePeptidesPerProteinFilter extends AbstractFilter {
	
	protected static final String shortName = RegisteredFilters.NR_GROUP_UNIQUE_PEPTIDES_PER_PROTEIN_FILTER.getShortName();
	
	protected static final String name = "#Peptides unique for the protein group";
	
	protected static final String filteringName = "#Peptides unique for Protein group";
	
	public static final FilterType filterType = FilterType.numerical;
	
	protected static final Class<Number> valueInstanceClass = Number.class;
	
	private Integer value;
	
	
	
	public NrGroupUniquePeptidesPerProteinFilter(FilterComparator arg, Integer value, boolean negate) {
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
			Integer nrGroupUnique = 0;
			List<Accession> protAccesssions = ((ReportProtein) o).getAccessions();
			
			for (ReportPeptide peptide : ((ReportProtein) o).getPeptides()) {
				if (protAccesssions.equals(peptide.getAccessions())) {
					nrGroupUnique++;
					
					
					System.err.println("proteinlist " + protAccesssions + " == " + peptide.getAccessions());
				} else {
					System.err.println("proteinlist " + protAccesssions + " != " + peptide.getAccessions());
				}
			}
			
			return nrGroupUnique;
			
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
