package de.mpc.pia.tools;


/**
 * This class contains constants from various used Ontologies.
 *
 * @author julian
 *
 */
public enum OntologyConstants {

    PIA("PIA", "MS:1002387"),
    PIA_XML_FORMAT("PIA XML format", "MS:1002388"),
    PIA_WORKFLOW_PARAMETER("PIA workflow parameter", "MS:1002389"),
    PIA_FDRSCORE_CALCULATED("PIA:FDRScore calculated", "MS:1002390"),
    PIA_COMBINED_FDRSCORE_CALCULATED("PIA:Combined FDRScore calculated", "MS:1002391"),
    PIA_PSM_SETS_CREATED("PIA:PSM sets created", "MS:1002392"),
    PIA_USED_TOP_IDENTIFICATIONS("PIA:used top identifications for FDR", "MS:1002393"),
    PIA_PROTEIN_SCORE("PIA:protein score", "MS:1002394"),
    PIA_PROTEIN_INFERENCE("PIA:protein inference", "MS:1002395"),
    PIA_PROTEIN_INFERENCE_FILTER("PIA:protein inference filter", "MS:1002396"),
    PIA_PROTEIN_INFERENCE_SCORING("PIA:protein inference scoring", "MS:1002397"),
    PIA_PROTEIN_INFERENCE_USED_SCORE("PIA:protein inference used score", "MS:1002398"),
    PIA_PROTEIN_INFERENCE_USED_PSMS("PIA:protein inference used PSMs", "MS:1002399"),
    PIA_FILTER("PIA:filter", "MS:1002400"),

    SEARCH_ENGINE_PSM_SCORE("search engine specific score for PSMs", "MS:1001143"),
    SPECTRUM_TITLE("spectrum title", "MS:1000796"),
    SCAN_START_TIME("scan start time", "MS:1000016"),
    RETENTION_TIME("retention time", "MS:1000894"),
    SCAN_NUMBERS("scan number(s)", "MS:1001115"),
    DELTA_MZ("delta m/z", "MS:1001975"),
    CLEAVAGE_AGENT_NAME("cleavage agent name", "MS:1001045"),
    NO_CLEAVAGE("no cleavage", "MS:1001955"),
    SEARCH_TOLERANCE_PLUS_VALUE("search tolerance plus value", "MS:1001412"),
    SEARCH_TOLERANCE_MINUS_VALUE("search tolerance minus value", "MS:1001413"),
    MS_MS_SEARCH("ms-ms search", "MS:1001083"),
    FRAGMENT_MASS_TYPE_MONO("fragment mass type mono", "MS:1001256"),
    FRAGMENT_MASS_TYPE_AVERAGE("fragment mass type average", "MS:1001255"),
    PARENT_MASS_TYPE_MONO("parent mass type mono", "MS:1001211"),
    PARENT_MASS_TYPE_AVERAGE("parent mass type average", "MS:1001212"),

    PROTEIN_DESCRIPTION("protein description", "MS:1001088"),
    FINAL_PSM_LIST("final PSM list", "MS:1002439"),
    INTERMEDIATE_PSM_LIST("intermediate PSM list", "MS:1002440"),
    SEQUENCE_SAME_SET_PROTEIN("sequence same-set protein", "MS:1001594"),
    SEQUENCE_SUB_SET_PROTEIN("sequence sub-set protein", "MS:1001596"),
    LEADING_PROTEIN("leading protein", "MS:1002401"),
    NON_LEADING_PROTEIN("non-leading protein", "MS:1002402"),
    GROUP_REPRESENTATIVE("group representative", "MS:1002403"),
    COUNT_OF_IDENTIFIED_PROTEINS("count of identified proteins", "MS:1002404"),
    CLUSTER_IDENTIFIER("cluster identifier", "MS:1002407"),
    PROTEIN_GROUP_PASSES_THRESHOLD("protein group passes threshold", "MS:1002415"),
    NO_THRESHOLD("no threshold", "MS:1001494"),

    DECOY_DB_ACCESSION_REGEXP("decoy DB accession regexp", "MS:1001283"),
    QUALITY_ESTIMATION_WITH_IMPL_DECOY_SEQ("quality estimation with implicite decoy sequences", "MS:1001454"),
    DECOY_PEPTIDE("decoy peptide", "MS:1002217"),
    PSM_LEVEL_PVALUE("PSM-level p-value", "MS:1002352"),
    PSM_LEVEL_EVALUE("PSM-level e-value", "MS:1002353"),
    PSM_LEVEL_QVALUE("PSM-level q-value", "MS:1002354"),
    PSM_LEVEL_FDRSCORE("PSM-level FDRScore", "MS:1002355"),
    PSM_LEVEL_COMBINED_FDRSCORE("PSM-level combined FDRScore", "MS:1002356"),
    PEPTIDE_LEVEL_QVALUE("distinct peptide-level q-value", "MS:1001868"),
    PEPTIDE_LEVEL_PVALUE("peptide sequence-level p-value", "MS:1001870"),
    PEPTIDE_LEVEL_EVALUE("peptide sequence-level e-value", "MS:1001872"),
    PEPTIDE_LEVEL_FDRSCORE("distinct peptide-level FDRScore", "MS:1002360"),
    PEPTIDE_LEVEL_COMBINED_FDRSCORE("distinct peptide-level combined FDRScore", "MS:1002361"),
    PROTEIN_LEVEL_LOCAL_FDR("protein-level local FDR", "MS:1002364"),
    PROTEIN_GROUP_LEVEL_Q_VALUE("protein group-level q-value", "MS:1002373"),
    NO_SPECIAL_PROCESSING("no special processing", "MS:1002495"),
    CONSENSUS_SCORING("consensus scoring", "MS:1002492"),
    CONSENSUS_RESULT("consensus result", "MS:1002315"),

    NO_FIXED_MODIFICATIONS_SEARCHED("No fixed modifications searched", "MS:1002453"),
    NO_VARIABLE_MODIFICATIONS_SEARCHED("No variable modifications searched", "MS:1002454"),
    UNKNOWN_MODIFICATION("unknown modification", "MS:1001460"),
    MODIFICATION_SPECIFICITY_PEP_N_TERM("modification specificity peptide N-term", "MS:1001189"),
    MODIFICATION_SPECIFICITY_PEP_C_TERM("modification specificity peptide C-term", "MS:1001190"),
    MODIFICATION_SPECIFICITY_PROTEIN_N_TERM("modification specificity protein N-term", "MS:1002057"),
    MODIFICATION_SPECIFICITY_PROTEIN_C_TERM("modification specificity protein C-term", "MS:1002058"),

    CONTACT_ATTRIBUTE("contact attribute", "MS:1000585"),
    CONTACT_NAME("contact name", "MS:1000586"),
    CONTACT_ADDRESS("contact address", "MS:1000587"),
    CONTACT_URL("contact URL", "MS:1000588"),
    CONTACT_EMAIL("contact email", "MS:1000589"),
    CONTACT_AFFILIATION("contact affiliation", "MS:1000590"),

    INSTRUMENT_MODEL("instrument model", "MS:1000031"),
    SAMPLE_NAME("sample name", "MS:1000002"),

    AMINOACID_SEQUENCE("AA sequence", "MS:1001344"),
    FASTA_FORMAT("FASTA format", "MS:1001348"),
    MULTIPLE_PEAK_LIST_NATIVEID_FORMAT("multiple peak list nativeID format", "MS:1000774"),
    MASCOT_MGF_FORMAT("Mascot MGF format", "MS:1001062"),
    THERMO_RAW_FORMAT("Thermo RAW format", "MS:1000563"),


    PRIDE_XML("PRIDE XML", "MS:1002600"),
    MASCOT_QUERY_NUMBER("Mascot query number", "MS:1001528"),
    SINGLE_PEAK_LIST("single peak list nativeID format", "MS:1000775"),

    PROTEOMEXCHANGE_ACCESSION_NUMBER("ProteomeXchange accession number", "MS:1001919", "ProteomExchange project accession number", "PRIDE:0000216"),
    PRIDE_PROJECT_NAME(null, null, "Project", "PRIDE:0000097"),

    TOPP_SOFTWARE("TOPP software", "MS:1000752"),

    // search engines
    AMANDA("Amanda", "MS:1002336"),
    AMANDA_SCORE("Amanda:AmandaScore", "MS:1002319"),

    MASCOT("Mascot", "MS:1001207"),
    MASCOT_DAT_FORMAT("Mascot DAT format", "MS:1001199"),
    MASCOT_SCORE("Mascot:score", "MS:1001171", "Mascot score", "PRIDE:0000069"),
    MASCOT_EXPECTATION_VALUE("Mascot:expectation value", "MS:1001172", "Mascot expect value", "PRIDE:0000212"),
    MASCOT_INSTRUMENT("Mascot:Instrument", "MS:1001656"),
    MASCOT_IDENTITY_THRESHED("Mascot:identity threshold", "MS:1001371"),
    MASCOT_PTM_SITE_CONFIDENT("Mascot:PTM site assignment", "MS:1002012"),
    MASCOT_HOMOLOGY_THRESHOLD("Mascot:homology threshold", "MS:1001370"),

    MSGF_RAWSCORE("MS-GF:RawScore", "MS:1002049"),
    MSGF_DENOVOSCORE("MS-GF:DeNovoScore", "MS:1002050"),
    MSGF_SPECEVALUE("MS-GF:SpecEValue", "MS:1002052"),
    MSGF_EVALUE("MS-GF:EValue", "MS:1002053"),
    MSGF_PEPQVALUE("MS-GF:PepQValue","MS:1002055"),
    MSGF_QVALUE("MS-GF:QValue","MS:1002054"),

    OMSSA_E_VALUE("OMSSA:evalue", "MS:1001328", "OMSSA E-value", "PRIDE:0000185"),
    OMSSA_P_VALUE("OMSSA:pvalue", "MS:1001329", "OMSSA P-value", "PRIDE:0000186"),

    SEQUEST("SEQUEST", "MS:1001208"),
    SEQUEST_PROBABILITY("SEQUEST:probability", "MS:1001154"),
    SEQUEST_XCORR("SEQUEST:xcorr", "MS:1001155", "Sequest score", "PRIDE:0000053"),
    SEQUEST_DELTA_CN("SEQUEST:deltacn", "MS:1001156", "Delta Cn", "PRIDE:0000012"),
    SEQUEST_SP("SEQUEST:sp", "MS:1001157", "Sp", "PRIDE:0000054"),
    SEQUEST_UNIQ("SEQUEST:Uniq", "MS:1001158"),
    SEQUEST_EXPECTATION("SEQUEST:expectation value", "MS:1001159"),
    SEQUEST_SF("SEQUEST:sf", "MS:1001160", "Sf", "PRIDE:0000284"),
    SEQUEST_PEPTIDE_SP("SEQUEST:PeptideSp", "MS:1001215"),
    SEQUEST_PEPTIDE_RANK_SP("SEQUEST:PeptideRankSp", "MS:1001217"),

    SCAFFOLD_PEPTIDE_PROBABILITY("Scaffold:Peptide Probability", "MS:1001568"),

    XTANDEM("X!Tandem", "MS:1001476"),
    XTANDEM_EXPECT("X!Tandem:expect", "MS:1001330", "expect", "PRIDE:0000183"),
    XTANDEM_HYPERSCORE("X!Tandem:hyperscore", "MS:1001331", "X!Tandem Hyperscore", "PRIDE:0000176"),

    COMET_XCORR("Comet:xcorr", "MS:1002252"),
    COMET_DELTA_CN("Comet:deltacn", "MS:1002253"),
    COMET_DELTA_CN_STAR("Comet:deltacnstar", "MS:1002254"),
    COMET_SP("Comet:spscore", "MS:1002255"),
    COMET_SP_RANK("Comet:sprank", "MS:1002256"),
    COMET_EXPECTATION("Comet:expectation value", "MS:1002257"),

    PEAKS_PEPTIDE_SCORE("PEAKS:peptideScore","MS:1001950"),

    PERCOLATOR_POSTERIOR_ERROR_PROBABILITY("percolator:PEP", "MS:1001493"),
    PERCOLATOR_Q_VALUE("percolator:Q value","MS:1001491"),

    PROTEOME_DISCOVERER_MIN_PEPTIDE_LENGTH("ProteomeDiscoverer:min peptide length", "MS:1002322"),
    PROTEOME_DISCOVERER_MAX_PEPTIDE_LENGTH("ProteomeDiscoverer:max peptide length", "MS:1002323"),
    WATERS_IDENTITYE_SCORE("IdentityE Score", "MS:1001569"),

    PEPTIDESHAKER_PSM_SCORE("PeptideShaker: PSM Score", "MS:1002466"),
    PEPTIDESHAKER_PSM_CONFIDENCE_SCORE("PeptideShaker PSM confidence", "MS:1002467"),

    MYRIMATCH_MVH("MyriMatch:MVH", "MS:1001589"),
    MYRIMATCH_MZFIDELITY_SCORE("MyriMatch:mzFidelity", "MS:1001590"),

    PHENYX_PEPTIDE_Z_SCORE("Phenyx:Pepzscore", "MS:1001395"),
    PHENYX_PEPTIDE_P_VALUE("Phenyx:PepPvalue","MS:1001396"),
    PHENYX_SCORE("Phenyx:Score", "MS:1001390"),

    SPECTRUM_MILL_SCORE( "SpectrumMill:Score" , "MS:1001572", "Spectrum Mill peptide score","PRIDE:0000177"),

    BYONIC_BEST_SCORE("Byonic:Best Score",  "MS:1002269"),
    BYONIC_SCORE("Byonic:Score",  "MS:1002262"),
    BYONIC_ABSLOGPROB2D_SCORE("Byonic: Peptide AbsLogProb2D","MS:1002311"),
    BYONIC_DELTA_SCORE("Byonic:Delta Score",  "MS:1002263"),
    BYONIC_DELTA_MOD_SCORE("Byonic:DeltaMod Score", "MS:1002264"),


    NUMBER_MATCHED_PEAKS("number of matched peaks","MS:1001121");


    /** URL to the current psi-ms.obo file */
    public static final String PSI_MS_OBO_URL = "https://raw.githubusercontent.com/HUPO-PSI/psi-ms-CV/master/psi-ms.obo";
    /** URL to the current psi-mod.obo file */
    public static final String PSI_MOD_OBO_URL = "https://raw.githubusercontent.com/HUPO-PSI/psi-mod-CV/master/PSI-MOD.obo";
    /** the label for the PSI-MS ontology */
    public static final String CV_PSI_MS_LABEL = "MS";
    /** current version of psi-ms.obo */
    public static final String PSI_MS_OBO_VERSION = "3.86.2";

    /** the label for the PRIDE CV */
    public static final String PRIDE_CV_LABEL = "PRIDE";
    /** the URL to the PRIDE CV */
    public static final String PRIDE_CV_URL = "https://raw.githubusercontent.com/PRIDE-Utilities/pride-ontology/master/pride_cv.obo";


    /** name in PSI OBO*/
    private String psiName;

    /** accession in PSI OBO */
    private String psiAccession;

    /** the name in the PRIDE ontoogy */
    private String prideName;

    /** the accession in the PRIDE ontology */
    private String prideAccession;


    /**
     * Initializes the ontology entry without pride references
     *
     * @param psiName
     * @param psiAccession
     */
    OntologyConstants(String psiName, String psiAccession) {
        this.psiName = psiName;
        this.psiAccession = psiAccession;
        this.prideName = null;
        this.prideAccession = null;
    }


    /**
     * Initializes the ontology entry with pride references
     *
     * @param psiName
     * @param psiAccession
     * @param prideName
     * @param prideAccession
     */
    OntologyConstants(String psiName, String psiAccession,
            String prideName, String prideAccession) {
        this(psiName, psiAccession);
        this.prideName = prideName;
        this.prideAccession = prideAccession;
    }


    public String getPsiName() {
        return psiName;
    }


    public String getPsiAccession() {
        return psiAccession;
    }


    public String getPrideName() {
        return prideName;
    }


    public String getPrideAccession() {
        return prideAccession;
    }


    /**
     * Returns the {@link OntologyConstants} with the provided accession (PSI or
     * PRIDE) or null, if none is found.
     *
     * @param accession
     * @return
     */
    public static OntologyConstants getByAccession(String accession) {
        if (accession != null) {
            for (OntologyConstants value : values()) {
                if (accession.equals(value.getPsiAccession()) ||
                        accession.equals(value.getPrideAccession())) {
                    return value;
                }
            }
        }
        return null;
    }
}
