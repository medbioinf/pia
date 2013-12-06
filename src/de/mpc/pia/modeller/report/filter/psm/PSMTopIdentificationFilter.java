package de.mpc.pia.modeller.report.filter.psm;

import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterType;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;

public class PSMTopIdentificationFilter extends AbstractFilter {
	
	protected final String shortName;
	
	protected final String name;
	
	protected final String filteringName;
	
	public static final FilterType filterType = FilterType.numerical;
	
	private Integer value;
	
	private final String scoreShortName;
	
	public static String prefix = "psm_top_identification_filter_";
	
	
	
	public PSMTopIdentificationFilter(FilterComparator arg, Integer value,
			boolean negate, String scoreShort) {
		this.comparator = arg;
		this.value = value;
		this.negate = negate;
		
		scoreShortName = scoreShort;
		String modelName = ScoreModelEnum.getName(scoreShortName);
		
		if (modelName != null) {
			name = modelName + " Top Identifications for PSM";
			filteringName = modelName + " (PSM Top Identifications)";
			shortName = prefix + scoreShort;
		} else {
			name = null;
			filteringName = null;
			shortName = null;
		}
	}
	
	
	/**
	 * Returns the short name and the filtering name for the score filter of the
	 * {@link ScoreModel} given by the scoreShort.
	 * 
	 * @param scoreShort
	 * @return an array of two Strings, containing the short and filtering name, or null, if scoreShort is invalid
	 */
	static public String[] getShortAndFilteringName(String scoreShort) {
		String modelName = ScoreModelEnum.getName(scoreShort);
		
		if (modelName != null) {
			String[] shortAndName = new String[2];
			
			shortAndName[0] = prefix + scoreShort;
			shortAndName[1] = modelName + " (PSM Top Identifications)";
			
			return shortAndName;
		} else {
			return null;
		}
	}
	
	
	@Override
	public String getShortName() {
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
		if (o instanceof ReportPSM) {
			return ((ReportPSM) o).getIdentificationRank(scoreShortName);
		} else if (o instanceof Number) {
			return o;
		} else {
			// nothing supported
			return null;
		}
	}
	
	@Override
	public boolean supportsClass(Object c) {
		if (c instanceof ReportPSM) {
			return true;
		} else {
			return false;
		}
	}
}
