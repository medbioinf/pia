package de.mpc.pia.modeller.psm;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mpc.pia.modeller.report.SortOrder;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.modeller.score.comparator.RankComparator;
import de.mpc.pia.modeller.score.comparator.ScoreComparator;
import de.mpc.pia.tools.PIATools;


public enum PSMReportItemComparator implements Comparator<PSMReportItem>, Serializable {

    RANK_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            RankComparator<PSMReportItem> comparator = new RankComparator<>();

            return comparator.compare(o1, o2);
        }

        @Override
        public String toString() {
            return "rank";
        }
    },
    SEQUENCE_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            return o1.getSequence().compareTo(o2.getSequence());
        }

        @Override
        public String toString() {
            return "sequence";
        }
    },
    CHARGE_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            return Integer.compare(o1.getCharge(), o2.getCharge());
        }

        @Override
        public String toString() {
            return "charge";
        }
    },
    NR_PSMS_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            Integer o1Nr = 1;
            int o2Nr = 1;

            if (o1 instanceof ReportPSMSet) {
                o1Nr = ((ReportPSMSet) o1).getPSMs().size();
            }

            if (o2 instanceof ReportPSMSet) {
                o2Nr = ((ReportPSMSet) o2).getPSMs().size();
            }

            return o1Nr.compareTo(o2Nr);
        }

        @Override
        public String toString() {
            return "nr_psms";
        }
    },
    MASS_TO_CHARGE_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            return Double.compare(o1.getMassToCharge(), o2.getMassToCharge());
        }

        @Override
        public String toString() {
            return "masstocharge";
        }
    },
    MISSED_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            return Integer.compare(o1.getMissedCleavages(), o2.getMissedCleavages());
        }

        @Override
        public String toString() {
            return "missed";
        }
    },
    SOURCE_ID_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            return PIATools.CompareProbableNulls(o1.getSourceID(),
                    o2.getSourceID());
        }

        @Override
        public String toString() {
            return "source_id";
        }
    },
    SPECTRUM_TITLE_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            return PIATools.CompareProbableNulls(o1.getSpectrumTitle(),
                    o2.getSpectrumTitle());
        }

        @Override
        public String toString() {
            return "spectrum_title";
        }
    },
    DELTA_MASS_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            return Double.compare(o1.getDeltaMass(), o2.getDeltaMass());
        }

        @Override
        public String toString() {
            return "deltamass";
        }
    },
    DELTA_PPM_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            return Double.compare(o1.getDeltaPPM(), o2.getDeltaPPM());
        }

        @Override
        public String toString() {
            return "deltappm";
        }
    },
    RETENTION_TIME_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            return PIATools.CompareProbableNulls(o1.getRetentionTime(),
                    o2.getRetentionTime());
        }

        @Override
        public String toString() {
            return "retention_time";
        }
    },

    // the scores
    PSM_FDR_SCORE_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName();
        }
    },
    PSM_Q_VALUE_SCORE_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.PSM_LEVEL_Q_VALUE.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.PSM_LEVEL_Q_VALUE.getShortName();
        }
    },
    AVERAGE_FDR_SCORE_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.AVERAGE_FDR_SCORE.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.AVERAGE_FDR_SCORE.getShortName();
        }
    },
    COMBINED_FDR_SCORE_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName();
        }
    },
    MASCOT_SCORE_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.MASCOT_SCORE.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.MASCOT_SCORE.getShortName();
        }
    },
    MASCOT_EXPECT_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.MASCOT_EXPECT.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.MASCOT_EXPECT.getShortName();
        }
    },
    SEQUEST_PROBABILITY_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.SEQUEST_PROBABILITY.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.SEQUEST_PROBABILITY.getShortName();
        }
    },
    SEQUEST_SPSCORE_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.SEQUEST_SPSCORE.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.SEQUEST_SPSCORE.getShortName();
        }
    },
    SEQUEST_XCORR_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.SEQUEST_XCORR.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.SEQUEST_XCORR.getShortName();
        }
    },
    XTANDEM_EXPECT_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.XTANDEM_EXPECT.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.XTANDEM_EXPECT.getShortName();
        }
    },
    XTANDEM_HYPERSCORE_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.XTANDEM_HYPERSCORE.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.XTANDEM_HYPERSCORE.getShortName();
        }
    },
    MSGF_RAWSCORE_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.MSGF_RAWSCORE.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.MSGF_RAWSCORE.getShortName();
        }
    },
    MSGF_DENOVOSCORE_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.MSGF_DENOVOSCORE.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.MSGF_DENOVOSCORE.getShortName();
        }
    },
    MSGF_SPECEVALUE_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.MSGF_SPECEVALUE.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.MSGF_SPECEVALUE.getShortName();
        }
    },
    MSGF_EVALUE_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.MSGF_EVALUE.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.MSGF_EVALUE.getShortName();
        }
    },
    MYRIMATCH_MVH_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.MYRIMATCH_MVH.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.MYRIMATCH_MVH.getShortName();
        }
    },
    AMANDA_SCORE_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.AMANDA_SCORE.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.AMANDA_SCORE.getShortName();
        }
    },
    OPENMS_POSTERIOR_ERROR_PROBABILITY_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.OPENMS_POSTERIOR_ERROR_PROBABILITY.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.OPENMS_POSTERIOR_ERROR_PROBABILITY.getShortName();
        }
    },
    OPENMS_POSTERIOR_PROBABILITY_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.OPENMS_POSTERIOR_PROBABILITY.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.OPENMS_POSTERIOR_PROBABILITY.getShortName();
        }
    },
    OPENMS_CONSENSUS_PEPMATRIX_PEP_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.OPENMS_CONSENSUS_PEPMATRIX_POSTERIOR_ERROR_PROBABILITY.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.OPENMS_CONSENSUS_PEPMATRIX_POSTERIOR_ERROR_PROBABILITY.getShortName();
        }
    },


    FASTA_SEQUENCE_COUNT_SCORE_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.FASTA_SEQUENCE_COUNT.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.FASTA_SEQUENCE_COUNT.getShortName();
        }
    },
    FASTA_ACCESSION_COUNT_SCORE_SORT {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            ScoreComparator<PSMReportItem> comp =
                    new ScoreComparator<>(ScoreModelEnum.FASTA_ACCESSION_COUNT.getShortName());
            return comp.compare(o1, o2);
        }

        @Override
        public String toString() {
            return SCORE_PREFIX + ScoreModelEnum.FASTA_ACCESSION_COUNT.getShortName();
        }
    },
    ;


    /** the prefix before all score tags */
    public static final String SCORE_PREFIX = "score_";


    /**
     * invert the ordering
     *
     * @param other
     * @return
     */
    public static Comparator<PSMReportItem> descending(final Comparator<PSMReportItem> other) {
        return (o1, o2) -> -1 * other.compare(o1, o2);
    }


    /**
     * returns a Comparator for multiple options.
     *
     * @param multipleOptions
     * @return
     */
    public static Comparator<PSMReportItem> getComparator(final List<Comparator<PSMReportItem>> multipleOptions) {
        return (o1, o2) -> {
            int result;
            // check all options, the first not returning 0 (equal) gets returned
            for (Comparator<PSMReportItem> option : multipleOptions) {
                result = option.compare(o1, o2);
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        };
    }


    /**
     * returns the comparator given by its name using the given order.
     *
     * @param name
     * @param order
     * @return
     */
    public static Comparator<PSMReportItem> getComparatorByName(String name, SortOrder order) {
        for (PSMReportItemComparator comp : values()) {
            if (name.equals(comp.toString())) {
                return order.equals(SortOrder.ascending) ? comp : descending(comp);
            }
        }

        return null;
    }


    /**
     * returns a mapping from the description strings of all the available
     * sortings to SortOrder.unsorted, except for the scores
     * @return
     */
    public static Map<String, SortOrder> getInitialSortOrders() {
        Map<String, SortOrder> orders = new HashMap<>();

        for (PSMReportItemComparator comp : values()) {
            if (!comp.toString().startsWith(SCORE_PREFIX)) {
                orders.put(comp.toString(), SortOrder.unsorted);
            }
        }

        return orders;
    }


    /**
     * Returns the name of the score with the given shortName.<br/>
     * The shortName has the score prefix, to distinguish it from a normal
     * sorting field.
     * @return
     */
    public static String getScoreSortName(String shortName) {
        for (PSMReportItemComparator comp : values()) {
            if (comp.toString().equals(SCORE_PREFIX + shortName)) {
                return comp.toString();
            }
        }

        return null;
    }
}
