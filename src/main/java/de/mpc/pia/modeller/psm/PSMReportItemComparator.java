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

    RANK_SORT(new RankComparator<>(), "rank"),
    SEQUENCE_SORT(null, "sequence") {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            return o1.getSequence().compareTo(o2.getSequence());
        }
    },
    CHARGE_SORT(null, "charge") {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            return Integer.compare(o1.getCharge(), o2.getCharge());
        }
    },
    NR_PSMS_SORT(null, "nr_psms") {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            Integer o1Nr = 1;
            Integer o2Nr = 1;

            if (o1 instanceof ReportPSMSet) {
                o1Nr = ((ReportPSMSet) o1).getPSMs().size();
            }

            if (o2 instanceof ReportPSMSet) {
                o2Nr = ((ReportPSMSet) o2).getPSMs().size();
            }

            return o1Nr.compareTo(o2Nr);
        }
    },
    MASS_TO_CHARGE_SORT(null, "masstocharge") {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            return Double.compare(o1.getMassToCharge(), o2.getMassToCharge());
        }
    },
    MISSED_SORT(null, "missed") {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            return Integer.compare(o1.getMissedCleavages(), o2.getMissedCleavages());
        }
    },
    SOURCE_ID_SORT(null, "source_id") {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            return PIATools.compareProbableNulls(o1.getSourceID(), o2.getSourceID());
        }
    },
    SPECTRUM_TITLE_SORT(null, "spectrum_title") {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            return PIATools.compareProbableNulls(o1.getSpectrumTitle(),
                    o2.getSpectrumTitle());
        }
    },
    DELTA_MASS_SORT(null, "deltamass") {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            return Double.compare(o1.getDeltaMass(), o2.getDeltaMass());
        }
    },
    DELTA_PPM_SORT(null, "deltappm") {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            return Double.compare(o1.getDeltaPPM(), o2.getDeltaPPM());
        }
    },
    RETENTION_TIME_SORT(null, "retention_time") {
        @Override
        public int compare(PSMReportItem o1, PSMReportItem o2) {
            return PIATools.compareProbableNulls(o1.getRetentionTime(), o2.getRetentionTime());
        }
    },

    // the scores
    AMANDA_SCORE_SORT(ScoreModelEnum.AMANDA_SCORE),
    AVERAGE_FDR_SCORE_SORT(ScoreModelEnum.AVERAGE_FDR_SCORE),
    BYONIC_ABSLOGPROB2D_SCORE_SORT(ScoreModelEnum.BYONIC_ABSLOGPROB2D_SCORE),
    BYONIC_BEST_SCORE_SORT(ScoreModelEnum.BYONIC_BEST_SCORE),
    BYONIC_DELTA_MOD_SCORE_SORT(ScoreModelEnum.BYONIC_DELTA_MOD_SCORE),
    BYONIC_DELTA_SCORE_SORT(ScoreModelEnum.BYONIC_DELTA_SCORE),
    BYONIC_SCORE_SORT(ScoreModelEnum.BYONIC_SCORE),
    COMET_DELTA_CN_SORT(ScoreModelEnum.COMET_DELTA_CN),
    COMET_DELTA_CN_STAR_SORT(ScoreModelEnum.COMET_DELTA_CN_STAR),
    COMET_EXPECTATION_SORT(ScoreModelEnum.COMET_EXPECTATION),
    COMET_SPRANK_SORT(ScoreModelEnum.COMET_SPRANK),
    COMET_SPSCORE_SORT(ScoreModelEnum.COMET_SPSCORE),
    COMET_XCORR_SORT(ScoreModelEnum.COMET_XCORR),
    MASCOT_EXPECT_SORT(ScoreModelEnum.MASCOT_EXPECT),
    MASCOT_HOMOLOGOY_THRESHOLD_SORT(ScoreModelEnum.MASCOT_HOMOLOGOY_THRESHOLD),
    MASCOT_IDENTITY_THRESHOLD_SORT(ScoreModelEnum.MASCOT_IDENTITY_THRESHOLD),
    MASCOT_PTM_SITE_CONFIDENT_SORT(ScoreModelEnum.MASCOT_PTM_SITE_CONFIDENT),
    MASCOT_SCORE_SORT(ScoreModelEnum.MASCOT_SCORE),
    MSGF_DENOVOSCORE_SORT(ScoreModelEnum.MSGF_DENOVOSCORE),
    MSGF_EVALUE_SORT(ScoreModelEnum.MSGF_EVALUE),
    MSGF_PEPQVALUE_SORT(ScoreModelEnum.MSGF_PEPQVALUE),
    MSGF_QVALUE_SORT(ScoreModelEnum.MSGF_QVALUE),
    MSGF_RAWSCORE_SORT(ScoreModelEnum.MSGF_RAWSCORE),
    MSGF_SPECEVALUE_SORT(ScoreModelEnum.MSGF_SPECEVALUE),
    MYRIMATCH_MVH_SORT(ScoreModelEnum.MYRIMATCH_MVH),
    MYRIMATCH_MZFIDELITY_SORT(ScoreModelEnum.MYRIMATCH_MZFIDELITY),
    OMSSA_E_VALUE_SORT(ScoreModelEnum.OMSSA_E_VALUE),
    OMSSA_P_VALUE_SORT(ScoreModelEnum.OMSSA_P_VALUE),
    OPENMS_CONSENSUS_PEPMATRIX_PEP_SORT(ScoreModelEnum.OPENMS_CONSENSUS_PEPMATRIX_POSTERIOR_ERROR_PROBABILITY),
    OPENMS_POSTERIOR_ERROR_PROBABILITY_SORT(ScoreModelEnum.OPENMS_POSTERIOR_ERROR_PROBABILITY),
    OPENMS_POSTERIOR_PROBABILITY_SORT(ScoreModelEnum.OPENMS_POSTERIOR_PROBABILITY),
    PEAKS_PEPTIDE_SCORE_SORT(ScoreModelEnum.PEAKS_PEPTIDE_SCORE),
    PEPTIDESHAKER_PSM_CONFIDENCE_SCORE_SORT(ScoreModelEnum.PEPTIDESHAKER_PSM_CONFIDENCE_SCORE),
    PEPTIDESHAKER_PSM_SCORE_SORT(ScoreModelEnum.PEPTIDESHAKER_PSM_SCORE),
    PERCOLATOR_POSTERIOR_ERROR_PROBABILITY_SORT(ScoreModelEnum.PERCOLATOR_POSTERIOR_ERROR_PROBABILITY),
    PERCOLATOR_Q_VALUE_SORT(ScoreModelEnum.PERCOLATOR_Q_VALUE),
    PHENYX_P_VALUE_SORT(ScoreModelEnum.PHENYX_P_VALUE),
    PHENYX_PEPTIDE_Z_SCORE_SORT(ScoreModelEnum.PHENYX_PEPTIDE_Z_SCORE),
    PHENYX_SCORE_SORT(ScoreModelEnum.PHENYX_SCORE),
    PSM_COMBINED_FDR_SCORE_SORT(ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE),
    PSM_FDR_SCORE_SORT(ScoreModelEnum.PSM_LEVEL_FDR_SCORE),
    PSM_Q_VALUE_SCORE_SORT(ScoreModelEnum.PSM_LEVEL_Q_VALUE),
    SEQUEST_DELTACN_SORT(ScoreModelEnum.SEQUEST_DELTACN),
    SEQUEST_PROBABILITY_SORT(ScoreModelEnum.SEQUEST_PROBABILITY),
    SEQUEST_SPSCORE_SORT(ScoreModelEnum.SEQUEST_SPSCORE),
    SEQUEST_XCORR_SORT(ScoreModelEnum.SEQUEST_XCORR),
    SPECTRUM_MILL_SCORE_SORT(ScoreModelEnum.SPECTRUM_MILL_SCORE),
    XTANDEM_EXPECT_SORT(ScoreModelEnum.XTANDEM_EXPECT),
    XTANDEM_HYPERSCORE_SORT(ScoreModelEnum.XTANDEM_HYPERSCORE),

    // special "scores"
    NUMBER_OF_MATCHED_PEAKS_SORT(ScoreModelEnum.NUMBER_OF_MATCHED_PEAKS),

    // fasta "scores"
    FASTA_ACCESSION_COUNT_SCORE_SORT(ScoreModelEnum.FASTA_ACCESSION_COUNT),
    FASTA_SEQUENCE_COUNT_SCORE_SORT(ScoreModelEnum.FASTA_SEQUENCE_COUNT),
    ;

    /** the prefix before all score tags */
    public static final String SCORE_PREFIX = "score_";
    
    /** the default comparator */
    private Comparator<PSMReportItem> comp = null;
    
    /** the toString result */
    private String stringName = null;
    
    
    /**
     * The default constructor
     */
    private PSMReportItemComparator(Comparator<PSMReportItem> comp, String stringName) {
    	this.comp = comp;
    	this.stringName = stringName;
    }
    
    
    /**
     * Constructs the comparator for a score comparison
     *
     * @param psiName
     * @param psiAccession
     */
    private PSMReportItemComparator(ScoreModelEnum scoreModel) {
    	this.comp = new ScoreComparator<>(scoreModel.getShortName());
    	this.stringName = SCORE_PREFIX + scoreModel.getShortName();
    }
    
    
    /**
     * The default is a score comparator. All other compares should be overriden
     * in the  declaration.
     */
    @Override
    public int compare(PSMReportItem o1, PSMReportItem o2) {
        return comp.compare(o1, o2);
    }
    
    
    @Override
    public String toString() {
    	return stringName;
    }
    

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
