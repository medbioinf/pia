package de.mpc.pia.modeller.report.filter.impl;

import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterType;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;

/**
 * Implementation of a filter, which filters by a PSM score of a
 * {@link PSMReportItem}.
 *
 * @author julian
 *
 */
public class PSMScoreFilter extends AbstractFilter {

    private static final long serialVersionUID = -759288282471703034L;


    public static final FilterType filterType = FilterType.numerical;

    private Double value;

    private String scoreShortName;

    private String modelName;

    public static final String PREFIX = "psm_score_filter_";


    public PSMScoreFilter(FilterComparator arg, boolean negate, Double value,
            String scoreShort) {
        super(arg, RegisteredFilters.PSM_SCORE_FILTER, negate);

        this.value = value;

        scoreShortName = scoreShort;
        modelName = ScoreModelEnum.getName(scoreShortName);
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
    public static String[] getShortAndFilteringName(String scoreShort,
            String defaultName) {
        String modelName = ScoreModelEnum.getName(scoreShort);

        if (scoreShort.equals(modelName)) {
            // there was no good name of the score, so the shortName will be returned
            modelName = defaultName;
        }

        if (modelName != null) {
            String[] shortAndName = new String[2];

            shortAndName[0] = PREFIX + scoreShort;
            shortAndName[1] = modelName + " (PSM)";

            return shortAndName;
        } else {
            return null;
        }
    }


    public String getScoreShortName() {
        return scoreShortName;
    }


    public String getModelName() {
        return modelName;
    }


    @Override
    public Object getFilterValue() {
        return value;
    }


    @Override
    public Object getObjectsValue(Object o) {
        if (o instanceof PSMReportItem) {
            return ((PSMReportItem)o).getScore(scoreShortName);
        } else if (o instanceof Number) {
            return o;
        }

        // nothing supported
        return null;
    }


    @Override
    public boolean supportsClass(Object c) {
        boolean supports = false;

        if (c instanceof PSMReportItem) {
            // it also depends on the score ("average FDR score" and "combined fdr score" is PSMSet, all other are PSM)
            if (c instanceof ReportPSM) {
                supports = !ReportPSMSet.isPSMSetScore(scoreShortName);
            } else if (c instanceof ReportPSMSet) {
                supports = ReportPSMSet.isPSMSetScore(scoreShortName);
            }
        }

        return supports;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(getShortName());

        str.append(" (").append(modelName).append(")");
        if (getFilterNegate()) {
            str.append(" not");
        }

        str.append(" ");
        str.append(getFilterComparator().toString());

        str.append(" ");
        str.append(getFilterValue());

        return str.toString();
    }
}
