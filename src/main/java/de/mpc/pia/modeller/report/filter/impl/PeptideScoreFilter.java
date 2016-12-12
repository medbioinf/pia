package de.mpc.pia.modeller.report.filter.impl;

import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterType;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;

/**
 * Implementation of a filter, which filters by a peptide score of a
 * {@link ReportPeptide}.
 *
 * @author julian
 *
 */
public class PeptideScoreFilter extends AbstractFilter {

    public static final FilterType filterType = FilterType.numerical;

    private Double value;

    private String scoreShortName;

    public static final String prefix = "peptide_score_filter_";


    public PeptideScoreFilter(FilterComparator arg, boolean negate, Double value,
            String scoreShort) {
        super(arg, RegisteredFilters.PEPTIDE_SCORE_FILTER, negate);

        this.value = value;

        scoreShortName = scoreShort;
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

            shortAndName[0] = prefix + scoreShort;
            shortAndName[1] = modelName + " (Peptide)";

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
        if (o instanceof ReportPeptide) {
            return ((ReportPeptide) o).getScore(scoreShortName);
        } else if (o instanceof Number) {
            return o;
        }

        // nothing supported
        return null;
    }

    @Override
    public boolean supportsClass(Object c) {
        return c instanceof ReportPeptide;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(getShortName());

        str.append(" (").append(getScoreShortName()).append(")");
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
