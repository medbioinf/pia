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
    CLEAVAGE_AGENT_NAME("cleavage agent name", "MS:1001045"),

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

    PSM_LEVEL_QVALUE("PSM-level q-value", "MS:1002354"),
    PSM_LEVEL_FDRSCORE("PSM-level FDRScore", "MS:1002355"),
    PSM_LEVEL_COMBINED_FDRSCORE("PSM-level combined FDRScore", "MS:1002356"),
    PROTEIN_LEVEL_LOCAL_FDR("protein-level local FDR", "MS:1002364"),
    PROTEIN_GROUP_LEVEL_Q_VALUE("protein group-level q-value", "MS:1002373"),

    SEARCH_TOLERANCE_PLUS_VALUE("search tolerance plus value", "MS:1001412"),
    SEARCH_TOLERANCE_MINUS_VALUE("search tolerance minus valu", "MS:1001413"),
    UNKNOWN_MODIFICATION("unknown modification", "MS:1001460"),
    MODIFICATION_SPECIFICITY_PEP_N_TERM("modification specificity peptide N-term", "MS:1001189"),
    MODIFICATION_SPECIFICITY_PEP_C_TERM("modification specificity peptide C-term", "MS:1001190"),
    MODIFICATION_SPECIFICITY_PROTEIN_N_TERM("modification specificity protein N-term", "MS:1002057"),
    MODIFICATION_SPECIFICITY_PROTEIN_C_TERM("modification specificity protein C-term", "MS:1002058"),

    // search engines
    AMANDA_SCORE("Amanda:AmandaScore", "MS:1002319"),

    MASCOT_SCORE("Mascot:score", "MS:1001171", "Mascot score", "PRIDE:0000069"),
    MASCOT_EXPECTATION_VALUE("Mascot:expectation value", "MS:1001172", "Mascot expect value", "PRIDE:0000212"),

    MSGF_RAWSCORE("MS-GF:RawScore", "MS:1002049"),
    MSGF_DENOVOSCORE("MS-GF:DeNovoScore", "MS:1002050"),
    MSGF_SPECEVALUE("MS-GF:SpecEValu", "MS:1002052"),
    MSGF_EVALUE("MS-GF:EValue", "MS:1002053"),

    MYRIMATCH_MVH("MyriMatch:MVH", "MS:1001589"),

    OMSSA_E_VALUE("OMSSA:evalue", "MS:1001328", "OMSSA E-value", "PRIDE:0000185"),
    OMSSA_P_VALUE("OMSSA:pvalue", "MS:1001329", "OMSSA P-value", "PRIDE:0000186"),

    SEQUEST_PROBABILITY("SEQUEST:probability", "MS:1001154"),
    SEQUEST_XCORR("SEQUEST:xcorr", "MS:1001157", "Sequest score", "PRIDE:0000053"),
    SEQUEST_SP("SEQUEST:sp", "MS:1001155", "Sp", "PRIDE:0000054"),

    XTANDEM_EXPECT("X!Tandem:expect", "MS:1001330", "expect", "PRIDE:0000183"),
    XTANDEM_HYPERSCORE("X!Tandem:hyperscore", "MS:1001331", "X!Tandem Hyperscore", "PRIDE:0000176"),
    ;

    /** URL to the current psi-ms.obo file */
    public static final String PSI_MS_OBO_URL = "https://raw.githubusercontent.com/HUPO-PSI/psi-ms-CV/master/psi-ms.obo";
    /** current version of psi-ms.obo */
    public static final String PSI_MS_OBO_VERSION = "3.86.2";
    /** the label for the PSI-MS ontology */
    public static final String CV_PSI_MS_LABEL = "MS";
    /** the label for the PRIDE ontology */
    public static final String CV_PRIDE_LABEL = "PRIDE";


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
     * @param psiaccession
     */
    private OntologyConstants(String psiName, String psiAccession) {
        this.psiName = psiName;
        this.psiAccession = psiAccession;
        this.prideName = null;
        this.prideAccession = null;
    }


    /**
     * Initializes the ontology entry with pride references
     *
     * @param psiName
     * @param psiaccession
     * @param prideName
     * @param prideAccession
     */
    private OntologyConstants(String psiName, String psiAccession,
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
}