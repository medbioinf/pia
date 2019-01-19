package de.mpc.pia.modeller.score;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.mpc.pia.modeller.psm.PSMReportItemComparator;
import de.mpc.pia.tools.OntologyConstants;


/**
 * This enumeration registers all {@link ScoreModel}s.
 * <p>
 * Also set the {@link PSMReportItemComparator}, if adding a new score!!
 *
 * @author julian
 *
 */
public enum ScoreModelEnum {

    /**
     * The score type is not further known, so actually nothing is known for it.
     */
    UNKNOWN_SCORE {
        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getShortName() {
            return null;
        }

        @Override
        public String getCvAccession() {
            return null;
        }

        @Override
        public String getCvName() {
            return null;
        }

        @Override
        public Boolean higherScoreBetter() {
            return null;
        }

        @Override
        public List<String> getValidDescriptors() {
            return new ArrayList<>();
        }
    },
    /**
     * This Score implements the Average FDR-Score.
     */
    AVERAGE_FDR_SCORE {
        @Override
        public String getName() {
            return "Average FDR Score";
        }

        @Override
        public String getShortName() {
            return "average_fdr_score";
        }

        @Override
        public String getCvAccession() {
            return "(cvAccession not set for average_fdr_score)";
        }

        @Override
        public String getCvName() {
            return CV_NAME_NOT_SET_PREFIX + getShortName() + ")";
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add("Average FDR");
            descs.add("average fdr");
            descs.add(getShortName());

            return descs;
        }

        @Override
        public Boolean isPSMSetScore() {
            return true;
        }
    },
    /**
     * This Score implements the Combined FDR Score on PSM level.
     */
    PSM_LEVEL_COMBINED_FDR_SCORE {
        @Override
        public String getName() {
            return "PSM Combined FDR Score";
        }

        @Override
        public String getShortName() {
            return "psm_combined_fdr_score";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.PSM_LEVEL_COMBINED_FDRSCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.PSM_LEVEL_COMBINED_FDRSCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add("PSM-level Combined FDR Score");
            descs.add(getShortName());

            return descs;
        }

        @Override
        public Boolean isPSMSetScore() {
            return true;
        }
    },
    /**
     * This Score implements the FDR Score on PSM level.
     */
    PSM_LEVEL_FDR_SCORE {
        @Override
        public String getName() {
            return "PSM FDRScore";
        }

        @Override
        public String getShortName() {
            return "psm_fdr_score";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.PSM_LEVEL_FDRSCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.PSM_LEVEL_FDRSCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

    },
    /**
     * This Score implements the PSM level q-value.
     */
    PSM_LEVEL_Q_VALUE {
        @Override
        public String getName() {
            return "PSM q-value";
        }

        @Override
        public String getShortName() {
            return "psm_q_value";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.PSM_LEVEL_QVALUE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.PSM_LEVEL_QVALUE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());
            descs.add("q-value_openmsmainscore");
            descs.add("q-value");

            return descs;
        }
    },

    /**
     * This Score implements the Combined FDR Score on peptide level.
     */
    PEPTIDE_LEVEL_COMBINED_FDR_SCORE {
        @Override
        public String getName() {
            return "Peptide Combined FDR Score";
        }

        @Override
        public String getShortName() {
            return "peptide_combined_fdr_score";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.PEPTIDE_LEVEL_COMBINED_FDRSCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.PEPTIDE_LEVEL_COMBINED_FDRSCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add("Peptide-level Combined FDR Score");
            descs.add(getShortName());

            return descs;
        }

        @Override
        public Boolean isPSMSetScore() {
            return true;
        }
    },
    /**
     * This Score implements the FDR Score on peptide level.
     */
    PEPTIDE_LEVEL_FDR_SCORE {
        @Override
        public String getName() {
            return "Peptide FDRScore";
        }

        @Override
        public String getShortName() {
            return "peptide_fdr_score";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.PEPTIDE_LEVEL_FDRSCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.PEPTIDE_LEVEL_FDRSCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

    },
    /**
     * This Score implements the PSM level q-value.
     */
    PEPTIDE_LEVEL_Q_VALUE {
        @Override
        public String getName() {
            return "PEPTIDE q-value";
        }

        @Override
        public String getShortName() {
            return "peptide_q_value";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.PEPTIDE_LEVEL_QVALUE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.PEPTIDE_LEVEL_QVALUE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }
    },

    /* Beginning of Mascot Scores  **/

    /**
     *  This Score implements the Mascot expectation value
     */
    MASCOT_EXPECT {
        @Override
        public String getName() {
            return "Mascot Expect";
        }

        @Override
        public String getShortName() {
            return "mascot_expect";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.MASCOT_EXPECTATION_VALUE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.MASCOT_EXPECTATION_VALUE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

        @Override
        public List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add(OntologyConstants.MASCOT_EXPECTATION_VALUE.getPrideAccession());
            return accessions;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();
            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());
            descs.add("Mascot expect");
            descs.add("Mascot_EValue");
            descs.addAll(getAdditionalAccessions());

            return descs;
        }
    },
    /**
     *  This Score implements the Mascot Ion Score.
     */
    MASCOT_SCORE {
        @Override
        public String getName() {
            return "Mascot Ion Score";
        }

        @Override
        public String getShortName() {
            return "mascot_score";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.MASCOT_SCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.MASCOT_SCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add(OntologyConstants.MASCOT_SCORE.getPrideAccession());
            return accessions;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return true;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add("IonScore");
            descs.add("Mascot Score");
            descs.add("mascot score");
            descs.add("Mascot score");
            descs.add("Mascot_openmsmainscore");
            descs.add("Mascot_Mascot_score");
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());
            descs.addAll(getAdditionalAccessions());

            return descs;
        }
    },

    MASCOT_PTM_SITE_CONFIDENT {
        @Override
        public String getName() {
            return "Mascot:PTM site assignment";
        }

        @Override
        public String getShortName() {
            return "mascot_ptm_siteassignment";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.MASCOT_PTM_SITE_CONFIDENT.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.MASCOT_PTM_SITE_CONFIDENT.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return false;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();
            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add("Mascot PTM site assignment");
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());
            descs.addAll(getAdditionalAccessions());

            return descs;
        }
    },

    MASCOT_IDENTITY_THRESHOLD {
        @Override
        public String getName() {
            return "Mascot:identity threshold";
        }

        @Override
        public String getShortName() {
            return "mascot_identity_threshold";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.MASCOT_IDENTITY_THRESHED.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.MASCOT_IDENTITY_THRESHED.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return false;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add("Mascot Identity Threshold");
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());
            descs.addAll(getAdditionalAccessions());

            return descs;
        }
    },

    /**
     * Mascot Homology Threshold.
     */
    MASCOT_HOMOLOGOY_THERHOLD{

        @Override
        public String getName() {
            return "Mascot:homology threshold";
        }

        @Override
        public String getShortName() {
            return "mascot_homology_threshold";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.MASCOT_HOMOLOGY_THRESHOLD.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.MASCOT_HOMOLOGY_THRESHOLD.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return false;
        }

    },

    /* End of Mascot Scores**/

    /**
     *  This Score implements the Sequest Probability.
     */
    SEQUEST_PROBABILITY {
        @Override
        public String getName() {
            return "Sequest Probability";
        }

        @Override
        public String getShortName() {
            return "sequest_probability";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.SEQUEST_PROBABILITY.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.SEQUEST_PROBABILITY.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            // no transformation needed
            return false;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add("Probability");
            descs.add("probability");
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());

            return descs;
        }
    },
    /**
     *  This Score implements the Sequest SpScore.
     */
    SEQUEST_SPSCORE {
        @Override
        public String getName() {
            return "SpScore";
        }

        @Override
        public String getShortName() {
            return "sequest_spscore";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.SEQUEST_SP.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.SEQUEST_SP.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add(OntologyConstants.SEQUEST_SP.getPrideAccession());
            return accessions;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add("Sequest SpScore");
            descs.add("sequest spscore");
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());
            descs.addAll(getAdditionalAccessions());

            return descs;
        }
    },
    /**
     *  This Score implements the Sequest XCorr.
     */
    SEQUEST_XCORR {
        @Override
        public String getName() {
            return "XCorr";
        }

        @Override
        public String getShortName() {
            return "sequest_xcorr";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.SEQUEST_XCORR.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.SEQUEST_XCORR.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add(OntologyConstants.SEQUEST_XCORR.getPrideAccession());
            return accessions;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return true;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add("Sequest XCorr");
            descs.add("sequest xcorr");
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());
            descs.addAll(getAdditionalAccessions());

            return descs;
        }
    },

    SEQUEST_DELTACN {
        @Override
        public String getName() {
            return "Delta Cn";
        }

        @Override
        public String getShortName() {
            return "sequest_deltacn";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.SEQUEST_DELTA_CN.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.SEQUEST_DELTA_CN.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add(OntologyConstants.SEQUEST_DELTA_CN.getPrideAccession());
            return accessions;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return false;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add("Delta Cn");
            descs.add("delta cn");
            descs.add("Sequest delta cn");
            descs.add("Sequest Delta Cn");
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());
            descs.addAll(getAdditionalAccessions());

            return descs;
        }
    },

    SEQUEST_PEPTIDE_SP {
        @Override
        public String getName() {
            return "SEQUEST:PeptideSp";
        }

        @Override
        public String getShortName() {
            return "sequest_peptidesp";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.SEQUEST_PEPTIDE_SP.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.SEQUEST_PEPTIDE_SP.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return false;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add("SEQUEST PeptideSp");
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());
            descs.addAll(getAdditionalAccessions());

            return descs;
        }
    },

    SEQUEST_PEPTIDE_RANK_SP {
        @Override
        public String getName() {
            return "SEQUEST:PeptideRankSp";
        }

        @Override
        public String getShortName() {
            return "sequest_peptide_ranksp";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.SEQUEST_PEPTIDE_RANK_SP.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.SEQUEST_PEPTIDE_RANK_SP.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return false;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add("SEQUEST PeptideRankSp");
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());
            descs.addAll(getAdditionalAccessions());

            return descs;
        }
    },

    /**
     * The X!Tandem expectation value.
     */
    XTANDEM_EXPECT {
        @Override
        public String getName() {
            return "X!Tandem Expect";
        }

        @Override
        public String getShortName() {
            return "xtandem_expect";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.XTANDEM_EXPECT.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.XTANDEM_EXPECT.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

        @Override
        public List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add(OntologyConstants.XTANDEM_EXPECT.getPrideAccession());
            return accessions;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return true;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add("X! Tandem expect");
            descs.add("Tandem Expect");
            descs.add("tandem expect");
            descs.add("XTandem_E-Value");
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());
            descs.addAll(getAdditionalAccessions());

            return descs;
        }
    },
    /**
     * The X!Tandem expectation value.
     */
    XTANDEM_HYPERSCORE {
        @Override
        public String getName() {
            return "X!Tandem Hyperscore";
        }

        @Override
        public String getShortName() {
            return "xtandem_hyperscore";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.XTANDEM_HYPERSCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.XTANDEM_HYPERSCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add(OntologyConstants.XTANDEM_HYPERSCORE.getPrideAccession());
            return accessions;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add("X! Tandem hyperscore");
            descs.add("Tandem Hyperscore");
            descs.add("tandem hyperscore");
            descs.add("Hyperscore");
            descs.add("hyperscore");
            descs.add("XTandem_openmsmainscore");
            descs.add("Mascot_XTandem_score");
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());
            descs.addAll(getAdditionalAccessions());

            return descs;
        }
    },

    /* Beginning of MSGF+ Scores **/

    /**
     * The MS-GF+ RawScore
     */
    MSGF_RAWSCORE {
        @Override
        public String getName() {
            return "MS-GF:RawScore";
        }

        @Override
        public String getShortName() {
            return "msgf_rawscore";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.MSGF_RAWSCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.MSGF_RAWSCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

    },
    /**
     * The MS-GF+ DeNovoScore
     */
    MSGF_DENOVOSCORE {
        @Override
        public String getName() {
            return "MS-GF:DeNovoScore";
        }

        @Override
        public String getShortName() {
            return "msgf_denovoscore";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.MSGF_DENOVOSCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.MSGF_DENOVOSCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

    },
    /**
     * The MS-GF+ SpecEValue
     */
    MSGF_SPECEVALUE {
        @Override
        public String getName() {
            return "MS-GF:SpecEValue";
        }

        @Override
        public String getShortName() {
            return "msgf_specevalue";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.MSGF_SPECEVALUE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.MSGF_SPECEVALUE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return true;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add(getName());
            descs.add("SpecEValue_openmsmainscore");
            descs.add("MSGFPlus_SpecEValue_score");
            descs.add(getName().toLowerCase());
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());

            return descs;
        }
    },
    /**
     * The MS-GF+ SpecEValue
     */
    MSGF_EVALUE {
        @Override
        public String getName() {
            return "MS-GF:EValue";
        }

        @Override
        public String getShortName() {
            return "msgf_evalue";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.MSGF_EVALUE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.MSGF_EVALUE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

    },

    /**
     * The MS-GF+ PepQValue
     */
    MSGF_PEPQVALUE {
        @Override
        public String getName() {
            return "MS-GF:PepQValue";
        }

        @Override
        public String getShortName() {
            return "msgf_pepqvalue";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.MSGF_PEPQVALUE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.MSGF_PEPQVALUE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

    },

    /**
     * The MS-GF+ QValue
     */
    MSGF_QVALUE {
        @Override
        public String getName() {
            return "MS-GF:QValue";
        }

        @Override
        public String getShortName() {
            return "msgf_qvalue";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.MSGF_QVALUE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.MSGF_QVALUE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

    },

    /* End of MSGF+ Scores **/

    /* Beginning of MS-Amanda Scores **/

    /**
     * The Amanda score
     */
    AMANDA_SCORE {
        @Override
        public String getName() {
            return "Amanda Score";
        }

        @Override
        public String getShortName() {
            return "amanda_score";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.AMANDA_SCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.AMANDA_SCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return true;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add("AmandaScore");
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());

            return descs;
        }
    },

    /* End of the MS-Amanda Scores **/


    /**
     * The Comet Scores
     */
    COMET_DELTA_CN {
        @Override
        public String getName() {
            return "Comet:deltacn";
        }

        @Override
        public String getShortName() {
            return "comet_deltacn";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.COMET_DELTA_CN.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.COMET_DELTA_CN.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

    },

    COMET_XCORR {
        @Override
        public String getName() {
            return "Comet:xcorr";
        }

        @Override
        public String getShortName() {
            return "comet_xcorr";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.COMET_XCORR.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.COMET_XCORR.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return true;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add("Comet XCorr");
            descs.add("Comet xcorr");
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());
            descs.addAll(getAdditionalAccessions());

            return descs;
        }
    },

    /* End of COMET Scores **/

    /**
     * The Peaks Scores
     */
    PEAKS_PEPTIDE_SCORE {
        @Override
        public String getName() {
            return "PEAKS:peptideScore";
        }

        @Override
        public String getShortName() {
            return "peaks_peptide_score";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.PEAKS_PEPTIDE_SCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.PEAKS_PEPTIDE_SCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return true;
        }
    },

    /* End of Peaks Scores **/

    /** OMSSA Scores ***/

    OMSSA_E_VALUE {
        @Override
        public String getName() {
            return "OMSSA E-value";
        }

        @Override
        public String getShortName() {
            return "omssa_e_value";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.OMSSA_E_VALUE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.OMSSA_E_VALUE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

        @Override
        public List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add(OntologyConstants.OMSSA_E_VALUE.getPrideAccession());
            return accessions;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return true;
        }
    },

    OMSSA_P_VALUE {
        @Override
        public String getName() {
            return "OMSSA P-value";
        }

        @Override
        public String getShortName() {
            return "omssa_p_value";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.OMSSA_P_VALUE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.OMSSA_P_VALUE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add(OntologyConstants.OMSSA_P_VALUE.getPrideAccession());
            return accessions;
        }

    },

    /* End OMSSA Scores **/


    /**
     * The SCAFFOLD SCORES
     */
    SCAFFOLD_PEPTIDE_PROBABILITY {
        @Override
        public String getName() {
            return "Scaffold:Peptide Probability";
        }

        @Override
        public String getShortName() {
            return "scaffold_peptide_probability";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.SCAFFOLD_PEPTIDE_PROBABILITY.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.SCAFFOLD_PEPTIDE_PROBABILITY.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return true;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());
            descs.add("scaffold_peptide_probability");

            return descs;
        }
    },

    /**
     * The OpenMS Posterior Error Probability
     */
    OPENMS_POSTERIOR_ERROR_PROBABILITY {
        @Override
        public String getName() {
            return "OpenMS Posterior Error Probability";
        }

        @Override
        public String getShortName() {
            return "openms_posterior_error_probability";
        }

        @Override
        public String getCvAccession() {
            return NO_CV_PREFIX + getShortName();
        }

        @Override
        public String getCvName() {
            return CV_NAME_NOT_SET_PREFIX + getShortName() + ")";
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add("Posterior Error Probability_openmsmainscore");
            descs.add("Posterior Error Probability_score");
            descs.add(getCvAccession());
            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add(getShortName());

            return descs;
        }
    },
    /**
     * The OpenMS Posterior Probability
     */
    OPENMS_POSTERIOR_PROBABILITY {
        @Override
        public String getName() {
            return "OpenMS Posterior Probability";
        }

        @Override
        public String getShortName() {
            return "openms_posterior_probability";
        }

        @Override
        public String getCvAccession() {
            return NO_CV_PREFIX + getShortName();
        }

        @Override
        public String getCvName() {
            return CV_NAME_NOT_SET_PREFIX + getShortName() + ")";
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add("Posterior Probability_openmsmainscore");
            descs.add("Posterior Probability_score");
            descs.add(getCvAccession());
            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add(getShortName());

            return descs;
        }
    },
    /**
     * The OpenMS Posterior Probability
     */
    OPENMS_CONSENSUS_PEPMATRIX_POSTERIOR_ERROR_PROBABILITY {
        @Override
        public String getName() {
            return "OpenMS Consensus PEPMatrix (Posterior Error Probability)";
        }

        @Override
        public String getShortName() {
            return "openms_consensus_pepmatrix_pep";
        }

        @Override
        public String getCvAccession() {
            return NO_CV_PREFIX + getShortName();
        }

        @Override
        public String getCvName() {
            return CV_NAME_NOT_SET_PREFIX + getShortName() + ")";
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add("Consensus_PEPMatrix (Posterior Error Probability)_score");
            descs.add("Consensus_PEPMatrix (Posterior Error Probability)_openmsmainscore");
            descs.add(getCvAccession());
            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add(getShortName());

            return descs;
        }
    },

    /**
     * The Percolator Posterior Error Probability
     */
    PERCOLATOR_POSTERIOR_ERROR_PROBABILITY {
        @Override
        public String getName() {
            return "percolator:PEP";
        }

        @Override
        public String getShortName() {
            return "percolator_pep";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.PERCOLATOR_POSTERIOR_ERROR_PROBABILITY.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.PERCOLATOR_POSTERIOR_ERROR_PROBABILITY.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

    },

    /**
     * The Percolator Q-Value
     */
    PERCOLATOR_Q_VALUE {
        @Override
        public String getName() {
            return "percolator:Q value";
        }

        @Override
        public String getShortName() {
            return "percolator_q_value";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.PERCOLATOR_Q_VALUE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.PERCOLATOR_Q_VALUE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

    },

    /* Percolator scores END**/

    /** PeptideShaker Scores**/

    PEPTIDESHAKER_PSM_SCORE{

        @Override
        public String getName() {
            return "PeptideShaker: PSM Score";
        }

        @Override
        public String getShortName() {
            return "peptideshaker_psm_score";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.PEPTIDESHAKER_PSM_SCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.PEPTIDESHAKER_PSM_SCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return true;
        }

    },

    PEPTIDESHAKER_PSM_CONFIDENCE_SCORE{

        @Override
        public String getName() {
            return "PeptideShaker PSM confidence";
        }

        @Override
        public String getShortName() {
            return "peptideshaker_psm_confidence";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.PEPTIDESHAKER_PSM_CONFIDENCE_SCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.PEPTIDESHAKER_PSM_CONFIDENCE_SCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

    },

    /* End of PeptideShaker Scores **/


    /** MyriMatch Scores **/

    MYRIMATCH_MZFIDELITY{

        @Override
        public String getName() {
            return "MyriMatch:mzFidelity";
        }

        @Override
        public String getShortName() {
            return "myrimatch_mzfidelity";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.MYRIMATCH_MZFIDELITY_SCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.MYRIMATCH_MZFIDELITY_SCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

    },

    /**
     * The MyriMatch MVH
     */
    MYRIMATCH_MVH {
        @Override
        public String getName() {
            return "MyriMatch:MVH";
        }

        @Override
        public String getShortName() {
            return "myrimatch_mvh";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.MYRIMATCH_MVH.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.MYRIMATCH_MVH.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return true;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());
            descs.add("MyriMatch_mvh_score");

            return descs;
        }
    },

    /** End MyriMatch Scores

    /** Phenyx Score **/

    PHENYX_PEPTIDE_Z_SCORE{

        @Override
        public String getName() {
            return "Phenyx:Pepzscore";
        }

        @Override
        public String getShortName() {
            return "phenyx_pepzscore";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.PHENYX_PEPTIDE_Z_SCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.PHENYX_PEPTIDE_Z_SCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return true;
        }

    },

    PHENYX_SCORE{
        @Override
        public String getName() {
            return "Phenyx:Score";
        }

        @Override
        public String getShortName() {
            return "phenyx_score";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.PHENYX_SCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.PHENYX_SCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

    },

    PHENYX_P_VALUE{
        @Override
        public String getName() {
            return "Phenyx:PepPvalue";
        }

        @Override
        public String getShortName() {
            return "phenyx_peppvalue";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.PHENYX_PEPTIDE_P_VALUE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.PHENYX_PEPTIDE_P_VALUE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

    },



    /* End Phenyx Score **/


    /**
     * Byonic Scores
     */
    BYONIC_BEST_SCORE{

        @Override
        public String getName() {
            return "Byonic:Best Score";
        }

        @Override
        public String getShortName() {
            return "byonic_best_score";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.BYONIC_BEST_SCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.BYONIC_BEST_SCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }
    },

    BYONIC_SCORE{

        @Override
        public String getName() {
            return "Byonic:Score";
        }

        @Override
        public String getShortName() {
            return "byonic_score";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.BYONIC_SCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.BYONIC_SCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return true;
        }
    },

    BYONIC_ABSLOGPROB2D_SCORE{

        @Override
        public String getName() {
            return "Byonic: Peptide AbsLogProb2D";
        }

        @Override
        public String getShortName() {
            return "byonic_peptide_abslogprob2d";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.BYONIC_ABSLOGPROB2D_SCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.BYONIC_ABSLOGPROB2D_SCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }
    },

    BYONIC_DELTA_SCORE{

        @Override
        public String getName() {
            return "Byonic:Delta Score";
        }

        @Override
        public String getShortName() {
            return "byonic_delta_score";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.BYONIC_DELTA_SCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.BYONIC_DELTA_SCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }
    },

    BYONIC_DELTA_MOD_SCORE{

        @Override
        public String getName() {
            return "Byonic:DeltaMod Score";
        }

        @Override
        public String getShortName() {
            return "byonic_delta_mod_score";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.BYONIC_DELTA_MOD_SCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.BYONIC_DELTA_MOD_SCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }
    },


    /* End of Byonic Scores **/


    /** Spectrum Mill Score **/

    SPECTRUM_MILL_SCORE{

        @Override
        public String getName() {
            return "SpectrumMill:Score";
        }

        @Override
        public String getShortName() {
            return "spectrummill_score";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.SPECTRUM_MILL_SCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.SPECTRUM_MILL_SCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return true;
        }

    },

    /* End Spectrum Mill Score **/

    /**
     * The FASTA Sequence Count score.
     */
    FASTA_SEQUENCE_COUNT {
        @Override
        public String getName() {
            return "FASTA Sequence Count";
        }

        @Override
        public String getShortName() {
            return "fasta_sequence_count";
        }

        @Override
        public String getCvAccession() {
            return "(cvAccession not set for fasta_sequence_count)";
        }

        @Override
        public String getCvName() {
            return CV_NAME_NOT_SET_PREFIX + getShortName() + ")";
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

    },
    /**
     * The FASTA Accession Count score.
     */
    FASTA_ACCESSION_COUNT {
        @Override
        public String getName() {
            return "FASTA Accession Count";
        }

        @Override
        public String getShortName() {
            return "fasta_accession_count";
        }

        @Override
        public String getCvAccession() {
            return "(cvAccession not set for fasta_accession_count)";
        }

        @Override
        public String getCvName() {
            return CV_NAME_NOT_SET_PREFIX + getShortName() + ")";
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

    },

    /**
     * The score of the protein.
     */
    PROTEIN_SCORE {
        @Override
        public String getName() {
            return "Protein score";
        }

        @Override
        public String getShortName() {
            return "protein_score";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.PIA_PROTEIN_SCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.PIA_PROTEIN_SCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

    },

    /**
     * Some other scores
     */

    WATERS_IDENTITYE_SCORE{
        @Override
        public String getName() {
            return "IdentityE Score";
        }

        @Override
        public String getShortName() {
            return "identitye_score";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.WATERS_IDENTITYE_SCORE.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.WATERS_IDENTITYE_SCORE.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

    },

    NUMBER_OF_MATCHEED_PEAKS{

        @Override
        public String getName() {
            return "number of matched peaks";
        }

        @Override
        public String getShortName() {
            return "number_of_matched_peaks";
        }

        @Override
        public String getCvAccession() {
            return OntologyConstants.NUMBER_MATCHED_PEAKS.getPsiAccession();
        }

        @Override
        public String getCvName() {
            return OntologyConstants.NUMBER_MATCHED_PEAKS.getPsiName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }
    };


    // some statics
    private static final String NO_CV_PREFIX = "NO_CV_";
    private static final String CV_NAME_NOT_SET_PREFIX = "(cvName not set for ";


    /**
     * These scores are not native searchengine results, but are in some way calculated
     */
    protected static final List<ScoreModelEnum> nonNativeScoreModels = new ArrayList<>(
          Arrays.asList(
                  AVERAGE_FDR_SCORE,
                  PSM_LEVEL_COMBINED_FDR_SCORE,
                  PSM_LEVEL_FDR_SCORE,
                  PSM_LEVEL_Q_VALUE,
                  PEPTIDE_LEVEL_COMBINED_FDR_SCORE,
                  PEPTIDE_LEVEL_FDR_SCORE,
                  PEPTIDE_LEVEL_Q_VALUE
                  )
          );


    /**
     * The scores in this list should not be used for FDR estimation on PSM level
     */
    protected static final List<ScoreModelEnum> notForPSMFdrScore = new ArrayList<>(
            Arrays.asList(
                    AVERAGE_FDR_SCORE,
                    FASTA_ACCESSION_COUNT,
                    FASTA_SEQUENCE_COUNT,
                    PROTEIN_SCORE,
                    PSM_LEVEL_COMBINED_FDR_SCORE,
                    PSM_LEVEL_FDR_SCORE,
                    PSM_LEVEL_Q_VALUE
                    )
            );


    /**
     * Returns the human readable name of the score model.
     * @return
     */
    public abstract String getName();


    /**
     * Returns the human readable name of the score model given by the
     * description (i.e. name, shortName or cvAccession). If the model is
     * {@link ScoreModelEnum#UNKNOWN_SCORE}, return the description.
     *
     * @return
     */
    public static String getName(String scoreModelDescriptor) {
        ScoreModelEnum model = getModelByDescription(scoreModelDescriptor);

        if (model.equals(UNKNOWN_SCORE)) {
            return scoreModelDescriptor;
        } else {
            return model.getName();
        }
    }


    /**
     * Returns the machine readable name of the score model
     * @return
     */
    public abstract String getShortName();


    /**
     * this should be overridden by any PSM set scores (like Combined FDR Score)
     * @return
     */
    public Boolean isPSMSetScore() {
        return false;
    }


    /**
     * Returns the CV accession of the score model
     * @return
     */
    public abstract String getCvAccession();


    /**
     * Returns the CV name of the score model
     * @return
     */
    public abstract String getCvName();


    /**
     * Returns, whether a higher score is better, or null, if not known (for
     * unknown type).
     *
     * @return
     */
    public abstract Boolean higherScoreBetter();

    /**
     * Some of the scores has different synonyms in PRIDE Database or other databases
     * and should be handle in the same way. For example the Mascot score can be found as
     * PRIDE:0000069 and MS:1001171. The additional values will be handle here.
     * @return
     */
    public List<String> getAdditionalAccessions(){
        return Collections.emptyList();
    }


    /**
     * Indicates, whether the score is the main score of the search engine.
     * These scores are preferably used for FDR calculation.
     */
    public Boolean isSearchengineMainScore() {
        return false;
    }


    /**
     * Gets all the valid descriptors for the ScoreModel (i.e. name, shortName
     * or cvAccession, some additional names)
     * @return
     */
    public List<String> getValidDescriptors(){
        List<String> descs = new ArrayList<>();
        descs.add(getName());
        descs.add(getName().toLowerCase());
        descs.add(getShortName());
        descs.add(getCvAccession());
        descs.add(getCvName());
        descs.addAll(getAdditionalAccessions());
        return descs;
    }


    /**
     * Returns true, if the given description is a valid descriptor (i.e. name,
     * shortName or cvAccession) of the score model.
     * @return
     */
    public final boolean isValidDescriptor(String desc) {
        return getValidDescriptors().contains(desc);
    }


    /**
     * Returns the model for the given description (i.e. name, shortName or
     * cvAccession)or UNKNOWN_SCORE, if there is none for the description.
     *
     * @param desc
     * @return
     */
    public static final ScoreModelEnum getModelByDescription(String desc) {
        for (ScoreModelEnum model : values()) {
            if (!model.equals(UNKNOWN_SCORE) &&
                    model.isValidDescriptor(desc)) {
                return model;
            }
        }

        return UNKNOWN_SCORE;
    }

    /**
     * Returns the model for the given description (i.e. name, shortName or
     * cvAccession)or UNKNOWN_SCORE, if there is none for the description.
     *
     * @param accession
     * @return
     */
    public static final ScoreModelEnum getModelByAccession(String accession) {
        for (ScoreModelEnum model : values()) {
            if (model != UNKNOWN_SCORE && model.getCvAccession().equals(accession)) {
                return model;
            }
        }

        return UNKNOWN_SCORE;
    }


    /**
     * Returns the scores which should not be used for FDR estimation on PSM level.
     * @return
     */
    public static final List<ScoreModelEnum> getNotForPSMFdrScore() {
        return notForPSMFdrScore;
    }


    /**
     * Returns the scores which are not native but somehow calculated.
     * @return
     */
    public static final List<ScoreModelEnum> getNonNativeScoreModels() {
        return nonNativeScoreModels;
    }
}