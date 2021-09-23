package de.mpc.pia.modeller.score;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.mpc.pia.modeller.psm.PSMReportItemComparator;
import de.mpc.pia.tools.OntologyConstants;
import de.mpc.pia.tools.PIAConstants;


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
    UNKNOWN_SCORE(null, null, null, null, null, null, false) {
    	@Override
    	protected void setValidDescriptors() {
    		setValidDescriptors(new ArrayList<>());
    	}
    },
    /**
     * This Score implements the Average FDR-Score.
     */
    AVERAGE_FDR_SCORE("Average FDR Score", "average_fdr_score",
    		"(cvAccession not set for average_fdr_score)", PIAConstants.CV_NAME_NOT_SET_PREFIX + "average_fdr_score)",
    		false, true, false) {
    	@Override
    	protected void setValidDescriptors() {
    		List<String> desc = new ArrayList<>();
    		desc.add("Average FDR Score");
    		desc.add("average fdr score");
    		desc.add("Average FDR");
    		desc.add("average fdr");
    		desc.add("average_fdr_score");
    		setValidDescriptors(desc);
    	}
    },
    /**
     * This Score implements the Combined FDR Score on PSM level.
     */
    PSM_LEVEL_COMBINED_FDR_SCORE("PSM Combined FDR Score", "psm_combined_fdr_score",
    		OntologyConstants.PSM_LEVEL_COMBINED_FDRSCORE,
    		false, true, false) {

        @Override
        protected void setValidDescriptors() {
            List<String> descs = new ArrayList<>();
            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add("PSM-level Combined FDR Score");
            descs.add(getShortName());
    		setValidDescriptors(descs);
        }
    },
    /**
     * This Score implements the FDR Score on PSM level.
     */
    PSM_LEVEL_FDR_SCORE("PSM FDRScore", "psm_fdr_score",
    		OntologyConstants.PSM_LEVEL_FDRSCORE,
    		false, false, false),
    /**
     * This Score implements the PSM level q-value.
     */
    PSM_LEVEL_Q_VALUE("PSM q-value", "psm_q_value",
    		OntologyConstants.PSM_LEVEL_QVALUE,
    		false, false, false) {
    	@Override
    	protected void setValidDescriptors() {
    		List<String> descs = new ArrayList<>();
            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add(getShortName());
            descs.add(getCvAccession());
            descs.add(getCvName());
            descs.add("q-value" + PIAConstants.OPENMS_MAINSCORE_PREFIX);
            descs.add("q-value");
            setValidDescriptors(descs);
        }
    },
    /**
     * This Score implements the Combined FDR Score on peptide level.
     */
    PEPTIDE_LEVEL_COMBINED_FDR_SCORE("Peptide Combined FDR Score", "peptide_combined_fdr_score",
    		OntologyConstants.PEPTIDE_LEVEL_COMBINED_FDRSCORE,
    		false, true, false) {
        @Override
        protected void setValidDescriptors() {
            List<String> descs = new ArrayList<>();
            descs.add(getName());
            descs.add(getName().toLowerCase());
            descs.add("Peptide-level Combined FDR Score");
            descs.add(getShortName());
            setValidDescriptors(descs);
        }
    },
    /**
     * This Score implements the FDR Score on peptide level.
     */
    PEPTIDE_LEVEL_FDR_SCORE("Peptide FDRScore", "peptide_fdr_score",
    		OntologyConstants.PEPTIDE_LEVEL_FDRSCORE,
    		false, false, false),
    /**
     * This Score implements the PSM level q-value.
     */
    PEPTIDE_LEVEL_Q_VALUE("PEPTIDE q-value", "peptide_q_value",
    		OntologyConstants.PEPTIDE_LEVEL_QVALUE,
    		false, false, false),

    /**
     *  This Score implements the Mascot expectation value
     */
    MASCOT_EXPECT("Mascot Expect", "mascot_expect",
    		OntologyConstants.MASCOT_EXPECTATION_VALUE,
    		false, false, false) {
        @Override
        protected List<String> getAdditionalAccessions() {
            List<String> accessions = new ArrayList<>();
            accessions.add(OntologyConstants.MASCOT_EXPECTATION_VALUE.getPrideAccession());
            accessions.add("Mascot expect");
            accessions.add("Mascot_EValue");
            return accessions;
        }
    },
    /**
     *  This Score implements the Mascot Ion Score.
     */
    MASCOT_SCORE("Mascot Ion Score", "mascot_score",
    		OntologyConstants.MASCOT_SCORE,
    		true, false, true) {
        @Override
        protected List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add(OntologyConstants.MASCOT_SCORE.getPrideAccession());
            accessions.add("IonScore");
            accessions.add("Mascot Score");
            accessions.add("mascot score");
            accessions.add("Mascot score");
            accessions.add("Mascot" + PIAConstants.OPENMS_MAINSCORE_PREFIX);
            accessions.add("Mascot_Mascot_score");
            return accessions;
        }
    },

    MASCOT_PTM_SITE_CONFIDENT("Mascot:PTM site assignment", "mascot_ptm_siteassignment",
    		OntologyConstants.MASCOT_PTM_SITE_CONFIDENT,
    		true, false, false) {
        @Override
        protected List<String> getAdditionalAccessions() {
            List<String> accessions = new ArrayList<>();
            accessions.add("Mascot PTM site assignment");
            return accessions;
        }
    },

    MASCOT_IDENTITY_THRESHOLD("Mascot:identity threshold", "mascot_identity_threshold",
    		OntologyConstants.MASCOT_IDENTITY_THRESHED,
    		true, false, false) {
    	@Override
    	protected List<String> getAdditionalAccessions() {
    		List<String> accessions = new ArrayList<>();
    		accessions.add("Mascot Identity Threshold");
    		return accessions;
        }
    },

    /**
     * Mascot Homology Threshold.
     */
    MASCOT_HOMOLOGOY_THRESHOLD("Mascot:homology threshold", "mascot_homology_threshold",
    		OntologyConstants.MASCOT_HOMOLOGY_THRESHOLD,
    		false, false, false),

    /**
     *  This Score implements the Sequest Probability.
     */
    SEQUEST_PROBABILITY("Sequest Probability", "sequest_probability",
    		OntologyConstants.SEQUEST_PROBABILITY,
    		false, false, false) {
        @Override
    	protected List<String> getAdditionalAccessions() {
    		List<String> accessions = new ArrayList<>();
            accessions.add("Probability");
            accessions.add("probability");
            return accessions;
        }
    },
    
    /**
     *  This Score implements the Sequest SpScore.
     */
    SEQUEST_SPSCORE("SpScore", "sequest_spscore",
    		OntologyConstants.SEQUEST_SP,
    		true, false, false) {
        @Override
        protected List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add(OntologyConstants.SEQUEST_SP.getPrideAccession());
            accessions.add("Sequest SpScore");
            accessions.add("sequest spscore");
            return accessions;
        }
    },
    
    /**
     *  This Score implements the Sequest XCorr.
     */
    SEQUEST_XCORR("XCorr", "sequest_xcorr",
    		OntologyConstants.SEQUEST_XCORR,
    		true, false, true) {
        @Override
        protected List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add(OntologyConstants.SEQUEST_XCORR.getPrideAccession());
            accessions.add("Sequest XCorr");
            accessions.add("sequest xcorr");
            return accessions;
        }
    },

    SEQUEST_DELTACN("Delta Cn", "sequest_deltacn",
    		OntologyConstants.SEQUEST_DELTA_CN,
    		true, false, false) {
        @Override
        protected List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add(OntologyConstants.SEQUEST_DELTA_CN.getPrideAccession());
            accessions.add("Sequest delta cn");
            accessions.add("Sequest Delta Cn");
            return accessions;
        }
    },

    SEQUEST_PEPTIDE_SP("SEQUEST:PeptideSp", "sequest_peptidesp",
    		OntologyConstants.SEQUEST_PEPTIDE_SP,
    		true, false, false) {
        @Override
        protected List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add("Sequest PeptideSp");
            return accessions;
        }
    },

    SEQUEST_PEPTIDE_RANK_SP("SEQUEST:PeptideRankSp", "sequest_peptide_ranksp",
    		OntologyConstants.SEQUEST_PEPTIDE_RANK_SP,
    		false, false, false) {
        @Override
        protected List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add("Sequest PeptideRankSp");
            return accessions;
        }
    },

    /**
     * The X!Tandem expectation value.
     */
    XTANDEM_EXPECT("X!Tandem Expect", "xtandem_expect",
    		OntologyConstants.XTANDEM_EXPECT,
    		false, false, true) {
        @Override
        protected List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add(OntologyConstants.XTANDEM_EXPECT.getPrideAccession());
            accessions.add("X! Tandem expect");
            accessions.add("Tandem Expect");
            accessions.add("tandem expect");
            accessions.add("XTandem_E-Value");
            return accessions;
        }
    },
    
    /**
     * The X!Tandem hyperscore value.
     */
    XTANDEM_HYPERSCORE("X!Tandem Hyperscore", "xtandem_hyperscore",
    		OntologyConstants.XTANDEM_HYPERSCORE,
    		true, false, false) {
        @Override
        public List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add(OntologyConstants.XTANDEM_HYPERSCORE.getPrideAccession());
            accessions.add("X! Tandem hyperscore");
            accessions.add("Tandem Hyperscore");
            accessions.add("tandem hyperscore");
            accessions.add("Hyperscore");
            accessions.add("hyperscore");
            accessions.add("XTandem" + PIAConstants.OPENMS_MAINSCORE_PREFIX);
            accessions.add("Mascot_XTandem_score");	// weird, but it was somewhere like this...
            return accessions;
        }
    },

    /* Beginning of MSGF+ Scores **/

    /**
     * The MS-GF+ RawScore
     */
    MSGF_RAWSCORE("MS-GF:RawScore", "msgf_rawscore",
    		OntologyConstants.MSGF_RAWSCORE,
    		true, false, false),
    
    /**
     * The MS-GF+ DeNovoScore
     */
    MSGF_DENOVOSCORE("MS-GF:DeNovoScore", "msgf_denovoscore",
    		OntologyConstants.MSGF_DENOVOSCORE,
    		true, false, false),
    
    /**
     * The MS-GF+ SpecEValue
     */
    MSGF_SPECEVALUE("MS-GF:SpecEValue", "msgf_specevalue",
    		OntologyConstants.MSGF_SPECEVALUE,
    		false, false, true) {
        @Override
        public List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add("SpecEValue" + PIAConstants.OPENMS_MAINSCORE_PREFIX);
            accessions.add("MSGFPlus_SpecEValue_score");
            return accessions;
        }
    },
    
    /**
     * The MS-GF+ SpecEValue
     */
    MSGF_EVALUE("MS-GF:EValue", "msgf_evalue",
    		OntologyConstants.MSGF_EVALUE,
    		false, false, false),
    
    /**
     * The MS-GF+ PepQValue
     */
    MSGF_PEPQVALUE("MS-GF:PepQValue", "msgf_pepqvalue",
    		OntologyConstants.MSGF_PEPQVALUE,
    		false, false, false),
    
    /**
     * The MS-GF+ QValue
     */
    MSGF_QVALUE("MS-GF:QValue", "msgf_qvalue",
    		OntologyConstants.MSGF_QVALUE,
    		false, false, false),
    
    /**
     * The Amanda score
     */
    AMANDA_SCORE("Amanda Score", "amanda_score",
    		OntologyConstants.AMANDA_SCORE,
    		true, false, true) {
        @Override
        public List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add("AmandaScore");
            return accessions;
        }
    },

    COMET_XCORR("Comet:xcorr", "comet_xcorr",
    		OntologyConstants.COMET_XCORR,
    		true, false, false) {
        @Override
        public List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add("Comet XCorr");
            accessions.add("Comet xcorr");
            return accessions;
        }
    },
    
    COMET_DELTA_CN("Comet:deltacn", "comet_deltacn",
    		OntologyConstants.COMET_DELTA_CN,
    		true, false, false),
    
    COMET_DELTA_CN_STAR("Comet:deltacnstar", "comet_deltacnstar",
    		OntologyConstants.COMET_DELTA_CN_STAR,
    		true, false, false),
    
    COMET_SPSCORE("Comet:spscore", "comet_spscore",
    		OntologyConstants.COMET_SP,
    		true, false, false),
    
    COMET_SPRANK("Comet:sprank", "comet_sprank",
    		OntologyConstants.COMET_SP_RANK,
    		false, false, false),
    
    COMET_EXPECTATION("Comet:expectation value", "comet_expectation",
    		OntologyConstants.COMET_EXPECTATION,
    		false, false, true) {
        @Override
        public List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add("Comet Expectation");
            accessions.add("Comet expectation");
            accessions.add("Comet" + PIAConstants.OPENMS_MAINSCORE_PREFIX);
            return accessions;
        }
    },

    /**
     * The Peaks Scores
     */
    PEAKS_PEPTIDE_SCORE("PEAKS:peptideScore", "peaks_peptide_score",
    		OntologyConstants.PEAKS_PEPTIDE_SCORE,
    		true, false, true),

    OMSSA_E_VALUE("OMSSA E-value", "omssa_e_value",
    		OntologyConstants.OMSSA_E_VALUE,
    		false, false, true) {
        @Override
        protected List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add(OntologyConstants.OMSSA_E_VALUE.getPrideAccession());
            return accessions;
        }
    },

    OMSSA_P_VALUE("OMSSA P-value", "omssa_p_value",
    		OntologyConstants.OMSSA_P_VALUE,
    		true, false, false) {
        @Override
        protected List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add(OntologyConstants.OMSSA_P_VALUE.getPrideAccession());
            return accessions;
        }

    },
    
    SCAFFOLD_PEPTIDE_PROBABILITY("Scaffold:Peptide Probability", "scaffold_peptide_probability",
    		OntologyConstants.SCAFFOLD_PEPTIDE_PROBABILITY,
    		true, false, true),
    
    /**
     * The OpenMS Posterior Error Probability
     */
    OPENMS_POSTERIOR_ERROR_PROBABILITY("OpenMS Posterior Error Probability", "openms_posterior_error_probability",
    		PIAConstants.NO_CV_PREFIX + "openms_posterior_error_probability", PIAConstants.CV_NAME_NOT_SET_PREFIX + "openms_posterior_error_probability)",
    		false, false, false) {
        @Override
        protected List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add("Posterior Error Probability" + PIAConstants.OPENMS_MAINSCORE_PREFIX);
            accessions.add("Posterior Error Probability_score");
            return accessions;
        }
    },
    
    /**
     * The OpenMS Posterior Probability
     */
    OPENMS_POSTERIOR_PROBABILITY("OpenMS Posterior Probability", "openms_posterior_probability",
    		PIAConstants.NO_CV_PREFIX + "openms_posterior_probability", PIAConstants.CV_NAME_NOT_SET_PREFIX + "openms_posterior_probability)",
    		true, false, false) {
        @Override
        protected List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add("Posterior Probability" + PIAConstants.OPENMS_MAINSCORE_PREFIX);
            accessions.add("Posterior Probability_score");
            return accessions;
        }
    },
    
    OPENMS_CONSENSUS_PEPMATRIX_POSTERIOR_ERROR_PROBABILITY(
    		"OpenMS Consensus PEPMatrix (Posterior Error Probability)", "openms_consensus_pepmatrix_pep",
    		PIAConstants.NO_CV_PREFIX + "openms_consensus_pepmatrix_pep", PIAConstants.CV_NAME_NOT_SET_PREFIX + "openms_consensus_pepmatrix_pep)",
    		false, false, false) {
        @Override
        protected List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add("Consensus_PEPMatrix (Posterior Error Probability)_score");
            accessions.add("Consensus_PEPMatrix (Posterior Error Probability)" + PIAConstants.OPENMS_MAINSCORE_PREFIX);
            return accessions;
        }
    },

    /**
     * The Percolator Posterior Error Probability
     */
    PERCOLATOR_POSTERIOR_ERROR_PROBABILITY("percolator:PEP", "percolator_pep",
    		OntologyConstants.PERCOLATOR_POSTERIOR_ERROR_PROBABILITY,
    		false, false, false),

    /**
     * The Percolator Q-Value
     */
    PERCOLATOR_Q_VALUE("percolator:Q value", "percolator_q_value",
    		OntologyConstants.PERCOLATOR_Q_VALUE,
    		false, false, false),

    PEPTIDESHAKER_PSM_SCORE("PeptideShaker: PSM Score", "peptideshaker_psm_score",
    		OntologyConstants.PEPTIDESHAKER_PSM_SCORE,
    		true, false, true),

    PEPTIDESHAKER_PSM_CONFIDENCE_SCORE("PeptideShaker PSM confidence", "peptideshaker_psm_confidence",
    		OntologyConstants.PEPTIDESHAKER_PSM_CONFIDENCE_SCORE,
    		true, false, false),

    MYRIMATCH_MZFIDELITY("MyriMatch:mzFidelity", "myrimatch_mzfidelity",
    		OntologyConstants.MYRIMATCH_MZFIDELITY_SCORE,
    		true, false, false),

    MYRIMATCH_MVH("MyriMatch:MVH", "myrimatch_mvh",
    		OntologyConstants.MYRIMATCH_MVH,
    		true, false, true) {
        @Override
        protected List<String> getAdditionalAccessions(){
            List<String> accessions = new ArrayList<>();
            accessions.add("MyriMatch_mvh_score");
            return accessions;
        }
    },

    PHENYX_PEPTIDE_Z_SCORE("Phenyx:Pepzscore", "phenyx_pepzscore",
    		OntologyConstants.PHENYX_PEPTIDE_Z_SCORE,
    		true, false, true),

    PHENYX_SCORE("Phenyx:Score", "phenyx_score",
    		OntologyConstants.PHENYX_SCORE,
    		true, false, false),

    PHENYX_P_VALUE("Phenyx:PepPvalue", "phenyx_peppvalue",
    		OntologyConstants.PHENYX_PEPTIDE_P_VALUE,
    		true, false, false),

    BYONIC_BEST_SCORE("Byonic:Best Score", "byonic_best_score",
    		OntologyConstants.BYONIC_BEST_SCORE,
    		true, false, false),

    BYONIC_SCORE("Byonic:Score", "byonic_score",
    		OntologyConstants.BYONIC_SCORE,
    		true, false, true),

    BYONIC_ABSLOGPROB2D_SCORE("Byonic: Peptide AbsLogProb2D", "byonic_peptide_abslogprob2d",
    		OntologyConstants.BYONIC_ABSLOGPROB2D_SCORE,
    		true, false, false),

    BYONIC_DELTA_SCORE("Byonic:Delta Score", "byonic_delta_score",
    		OntologyConstants.BYONIC_DELTA_SCORE,
    		true, false, false),

    BYONIC_DELTA_MOD_SCORE("Byonic:DeltaMod Score", "byonic_delta_mod_score",
    		OntologyConstants.BYONIC_DELTA_MOD_SCORE,
    		true, false, false),

    SPECTRUM_MILL_SCORE("SpectrumMill:Score", "spectrummill_score",
    		OntologyConstants.SPECTRUM_MILL_SCORE,
    		true, false, true),
    
    WATERS_IDENTITYE_SCORE("IdentityE Score", "identitye_score",
    		OntologyConstants.WATERS_IDENTITYE_SCORE,
    		true, false, false),

    NUMBER_OF_MATCHED_PEAKS("number of matched peaks", "number_of_matched_peaks",
    		OntologyConstants.NUMBER_MATCHED_PEAKS,
    		true, false, false),

    /**
     * The FASTA Sequence Count score.
     */
    FASTA_SEQUENCE_COUNT("FASTA Sequence Count", "fasta_sequence_count",
    		"(cvAccession not set for fasta_sequence_count)", PIAConstants.CV_NAME_NOT_SET_PREFIX + "fasta_sequence_count)",
    		true, false, false),
    
    /**
     * The FASTA Accession Count score.
     */
    FASTA_ACCESSION_COUNT("FASTA Accession Count", "fasta_accession_count",
    		"(cvAccession not set for fasta_accession_count)", PIAConstants.CV_NAME_NOT_SET_PREFIX + "fasta_accession_count)",
    		true, false, false),

    /**
     * The score of the protein.
     */
    PROTEIN_SCORE("Protein score", "protein_score",
    		OntologyConstants.PIA_PROTEIN_SCORE,
    		true, false, false),
    ;


    private String name;
    private String shortName;
    private String cvAccession;
    private String cvName;
    private Boolean higherScoreBetter;
    private List<String> validDescriptors;
    private Boolean psmSetScore;
    private boolean searchEngineMainScore;
    

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
     * Creates a new ScoreModelEnum with the given parameters
     * 
     * @param name
     * @param shortName
     * @param cvAccession
     * @param cvName
     * @param higherScoreBetter
     * @param validDescriptors
     */
    private ScoreModelEnum(String name, String shortName, String cvAccession, String cvName,
    		Boolean higherScoreBetter, Boolean psmSetScore, boolean searchEngineMainScore) {
    	this.name = name;
    	this.shortName = shortName;
    	this.cvAccession = cvAccession;
    	this.cvName = cvName;
    	this.higherScoreBetter = higherScoreBetter;
    	this.psmSetScore = psmSetScore;
    	this.searchEngineMainScore = searchEngineMainScore;
    	setValidDescriptors();
    }
    
    
    private ScoreModelEnum(String name, String shortName, OntologyConstants ontology,
    		Boolean higherScoreBetter, Boolean psmSetScore, boolean searchEngineMainScore) {
    	this(name, shortName,
    			ontology.getPsiAccession(), ontology.getPsiName(),
    			higherScoreBetter, psmSetScore, searchEngineMainScore);
    }
    

    /**
     * Returns the human readable name of the score model.
     * @return
     */
    public final String getName() {
    	return name;
    }


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
    public final String getShortName() {
    	return shortName;
    }


    /**
     * this should be overridden by any PSM set scores (like Combined FDR Score)
     * @return
     */
    public final Boolean isPSMSetScore() {
        return psmSetScore;
    }


    /**
     * Returns the CV accession of the score model
     * @return
     */
    public final String getCvAccession() {
    	return cvAccession;
    }


    /**
     * Returns the CV name of the score model
     * @return
     */
    public final String getCvName() {
    	return cvName;
    }


    /**
     * Returns, whether a higher score is better, or null, if not known (for
     * unknown type).
     *
     * @return
     */
    public final Boolean higherScoreBetter() {
    	return higherScoreBetter;
    }

    /**
     * Some of the scores has different synonyms in PRIDE Database or other databases
     * and should be handle in the same way. For example the Mascot score can be found as
     * PRIDE:0000069 and MS:1001171. The additional values will be handle here.
     * @return
     */
    protected List<String> getAdditionalAccessions(){
        return Collections.emptyList();
    }


    /**
     * Indicates, whether the score is the main score of the search engine.
     * These scores are preferably used for FDR calculation.
     */
    public final boolean isSearchengineMainScore() {
        return searchEngineMainScore;
    }


    /**
     * set the default valid descriptors
     */
	protected void setValidDescriptors() {
		this.validDescriptors = new ArrayList<>();
    	this.validDescriptors.add(getName());
    	this.validDescriptors.add(getName().toLowerCase());
    	this.validDescriptors.add(getShortName());
    	this.validDescriptors.add(getCvAccession());
    	this.validDescriptors.add(getCvName());
	}
	
	
    /**
     * set the default valid descriptors
     */
	protected void setValidDescriptors(List<String> validDescriptors) {
		this.validDescriptors = new ArrayList<>(validDescriptors);
	}
	
	
	/**
     * Gets all the valid descriptors for the ScoreModel (i.e. name, shortName
     * or cvAccession, some additional names)
     * @return
     */
    public final List<String> getValidDescriptors(){
        List<String> descs = new ArrayList<>(validDescriptors);
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
     * cvAccession) or UNKNOWN_SCORE, if there is none for the description.
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
     * Returns the model for the given accession or UNKNOWN_SCORE, if there is none for the description.
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
