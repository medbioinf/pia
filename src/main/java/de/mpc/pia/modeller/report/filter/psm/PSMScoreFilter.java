package de.mpc.pia.modeller.report.filter.psm;

import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterType;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;

public class PSMScoreFilter extends AbstractFilter {
	
	protected final String shortName;
	
	protected final String name;
	
	protected final String filteringName;
	
	public static final FilterType filterType = FilterType.numerical;
	
	private Double value;
	
	private final String scoreShortName;
	
	private final String modelName;
	
	public static String prefix = "psm_score_filter_";
	
	
	
	public PSMScoreFilter(FilterComparator arg, Double value, boolean negate,
			String scoreShort) {
		this.comparator = arg;
		this.value = value;
		this.negate = negate;
		
		scoreShortName = scoreShort;
		modelName = ScoreModelEnum.getName(scoreShortName);
		
		if (modelName != null) {
			name = modelName + " Filter for PSM";
			filteringName = modelName + " (PSM)";
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
	 * @param defaultName the default name of the score, if it is not hard
	 * programmed in the {@link ScoreModelEnum}
	 * @return an array of two Strings, containing the short and filtering name, or null, if scoreShort is invalid
	 */
	static public String[] getShortAndFilteringName(String scoreShort,
			String defaultName) {
		String modelName = ScoreModelEnum.getName(scoreShort);
		
		if (scoreShort.equals(modelName)) {
			// there was no good name of the score, so the shortName was returned
			modelName = defaultName;
		}
		
		if (modelName != null) {
			String[] shortAndName = new String[2];
			
			shortAndName[0] = prefix + scoreShort;
			shortAndName[1] = modelName + " (PSM)";
			
			return shortAndName;
		} else {
			return null;
		}
	}
	
	
	@Override
	public String getShortName() {
		return shortName;
	}
	
	
	public String getScoreShortName() {
		return scoreShortName;
	}
	
	
	@Override
	public String getName() {
		return name;
	}
	
	
	public String getModelName() {
		return modelName;
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
		if (o instanceof PSMReportItem) {
			return ((PSMReportItem)o).getScore(scoreShortName);
		} else if (o instanceof Number) {
			return o;
		} else {
			// nothing supported
			return null;
		}
	}
	
	
	@Override
	public boolean newInferenceFor(Class c) {
		if (c.equals(ReportPSMSet.class) ||
				c.equals(ReportPeptide.class)) {
			// rebuild of the PSM sets is necessary
			// rebuilding the reported peptides is necessary
			return true;
		}
		
		// it is either a ReportPSM, which can be filtered, or an invalid object
		return false;
	}
	
	
	@Override
	public boolean supportsClass(Object c) {
		boolean supports = false;
		
		if (c instanceof PSMReportItem) {
			// it also depends on the score ("average FDR score" and "combined fdr score" is PSMSet, all other are PSM)
			if (c instanceof ReportPSM) {
				if (!ReportPSMSet.isPSMSetScore(scoreShortName)) {
					supports = true;
				} else {
					supports = false;
				}
			} else if (c instanceof ReportPSMSet) {
				if (ReportPSMSet.isPSMSetScore(scoreShortName)) {
					supports = true;
				} else {
					supports = false;
				}
			}
		}
		
		return supports;
	}
}
