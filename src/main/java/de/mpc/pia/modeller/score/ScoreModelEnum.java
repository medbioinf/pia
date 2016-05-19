package de.mpc.pia.modeller.score;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.mpc.pia.modeller.psm.PSMReportItemComparator;
import de.mpc.pia.modeller.psm.ReportPSMSet;
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
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public List<String> getValidDescriptors() {
            return new ArrayList<String>();
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
            return "(cvName not set for average_fdr_score)";
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

        @Override
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

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
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

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

        @Override
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());

            return descs;
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
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

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
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

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

        @Override
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());

            return descs;
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

        @Override
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());

            return descs;
        }
    },

    /** Beginning of Mascot Scores  **/

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
            List<String> accessions = new ArrayList<String>();
            accessions.add(OntologyConstants.MASCOT_EXPECTATION_VALUE.getPrideAccession());
            return accessions;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

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
            List<String> accessions = new ArrayList<String>();
            accessions.add(OntologyConstants.MASCOT_SCORE.getPrideAccession());
            return accessions;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return true;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

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

    /** End of Mascot Scores**/

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
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

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
            List<String> accessions = new ArrayList<String>();
            accessions.add(OntologyConstants.SEQUEST_SP.getPrideAccession());
            return accessions;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

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
            List<String> accessions = new ArrayList<String>();
            accessions.add(OntologyConstants.SEQUEST_XCORR.getPrideAccession());
            return accessions;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return true;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

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
            List<String> accessions = new ArrayList<String>();
            accessions.add(OntologyConstants.XTANDEM_EXPECT.getPrideAccession());
            return accessions;
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return true;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

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
            List<String> accessions = new ArrayList<String>();
            accessions.add(OntologyConstants.XTANDEM_HYPERSCORE.getPrideAccession());
            return accessions;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

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

    /** Beginning of MSGF+ Scores **/

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

        @Override
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());

            return descs;
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

        @Override
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

            descs.add(getName());
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
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return true;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

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

        @Override
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());

            return descs;
        }
    },

    /** End of MSGF+ Scores **/

    /** Beginning of MS-Amanda Scores **/

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
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return true;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add("AmandaScore");
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());

            return descs;
        }
    },

    /** End of the MS-Amanda Scores **/

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
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public Boolean isSearchengineMainScore() {
            return true;
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());
            descs.add("MyriMatch_mvh_score");

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
            return "NO_CV_" + getShortName();
        }

        @Override
        public String getCvName() {
            return "(cvName not set for " + getShortName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

        @Override
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

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
            return "NO_CV_" + getShortName();
        }

        @Override
        public String getCvName() {
            return "(cvName not set for " + getShortName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

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
            return "NO_CV_" + getShortName();
        }

        @Override
        public String getCvName() {
            return "(cvName not set for " + getShortName();
        }

        @Override
        public Boolean higherScoreBetter() {
            return false;
        }

        @Override
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

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
            return "(cvName not set for fasta_sequence_count)";
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());

            return descs;
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
            return "(cvName not set for fasta_accession_count)";
        }

        @Override
        public Boolean higherScoreBetter() {
            return true;
        }

        @Override
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());

            return descs;
        }
    },

    /**
     * The score of the protein.
     *
     * TODO: this must be handled differently... there are scores with higherScoreBetter = true and false!
     *
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

        @Override
        public List<String> getAdditionalAccessions(){
            return Collections.emptyList();
        }

        @Override
        public List<String> getValidDescriptors() {
            List<String> descs = new ArrayList<String>();

            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());

            return descs;
        }
    },
    ;


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
    public final static String getName(String scoreModelDescriptor) {
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
    public abstract List<String> getAdditionalAccessions();


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
    public abstract List<String> getValidDescriptors();


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
    public final static ScoreModelEnum getModelByDescription(String desc) {
        for (ScoreModelEnum model : values()) {
            if (!model.equals(UNKNOWN_SCORE) &&
                    model.isValidDescriptor(desc)) {
                return model;
            }
        }

        return UNKNOWN_SCORE;
    }


    /**
     * The scores in this list should not be used for FDR estimation on PSM level
     */
    public final static List<ScoreModelEnum> notForPSMFdrScore = new ArrayList<ScoreModelEnum>(
            Arrays.asList(new ScoreModelEnum[]{
                    AVERAGE_FDR_SCORE,
                    FASTA_ACCESSION_COUNT,
                    FASTA_SEQUENCE_COUNT,
                    PROTEIN_SCORE,
                    PSM_LEVEL_COMBINED_FDR_SCORE,
                    PSM_LEVEL_FDR_SCORE,
                    PSM_LEVEL_Q_VALUE
            }));
}