package de.mpc.pia.modeller.report.filter.protein;

import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterType;
import de.mpc.pia.modeller.score.ScoreModelEnum;

public class ProteinScoreFilter extends AbstractFilter {
	
	public static final String shortName = ScoreModelEnum.PROTEIN_SCORE.getShortName() + "_filter";
	
	protected static final String name = "Protein Score Filter";
	
	public static final String filteringName = ScoreModelEnum.PROTEIN_SCORE.getName();
	
	public static final FilterType filterType = FilterType.numerical;
	
	private Double value;
	
	
	
	public ProteinScoreFilter(FilterComparator arg, Double value, boolean negate) {
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
	
	@Override
	public String getFilteringName() {
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
	
	@Override
	public Object getObjectsValue(Object o) {
		if (o instanceof ReportProtein) {
			return ((ReportProtein) o).getScore();
		} else if (o instanceof Number) {
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
