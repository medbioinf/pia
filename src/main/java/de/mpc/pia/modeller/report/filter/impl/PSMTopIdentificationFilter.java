package de.mpc.pia.modeller.report.filter.impl;

import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterType;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;

/**
 * Implementation of a filter, which filters PSMs for the top X identifications
 * of a spectrum on the given score.
 *
 * @author julian
 *
 */
public class PSMTopIdentificationFilter extends AbstractFilter {

    private static final long serialVersionUID = 880481749210410364L;


    protected final String shortName;

    protected final String name;

    protected final String filteringName;

    public static final FilterType filterType = FilterType.numerical;

    private Integer value;

    private final String scoreShortName;

    public static final String PREFIX = "psm_top_identification_filter_";


    public PSMTopIdentificationFilter(FilterComparator arg, Integer value,
            boolean negate, String scoreShort) {
        super(arg, RegisteredFilters.PSM_TOP_IDENTIFICATION_FILTER, negate);

        this.value = value;

        scoreShortName = scoreShort;
        String modelName = ScoreModelEnum.getName(scoreShortName);

        if (modelName != null) {
            name = modelName + " Top Identifications for PSM";
            filteringName = modelName + " (PSM Top Identifications)";
            shortName = PREFIX + scoreShort;
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
    public static String[] getShortAndFilteringName(String scoreShort,
            String defaultName) {
        String modelName = ScoreModelEnum.getName(scoreShort);

        if (scoreShort.equals(modelName)) {
            // there was no good name of the score, so the shortName was returned
            modelName = defaultName;
        }

        String[] shortAndName = null;

        if (modelName != null) {
            shortAndName = new String[2];

            shortAndName[0] = PREFIX + scoreShort;
            shortAndName[1] = modelName + " (PSM Top Identifications)";
        }

        return shortAndName;
    }


    public String getScoreShortName() {
        return scoreShortName;
    }


    @Override
    public Object getFilterValue() {
        return value;
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
        return c instanceof ReportPSM;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(getShortName());

        str.append(" (").append(getScoreShortName()).append(')');
        if (getFilterNegate()) {
            str.append(" not");
        }

        str.append(' ');
        str.append(getFilterComparator().toString());

        str.append(' ');
        str.append(getFilterValue());

        return str.toString();
    }
}
