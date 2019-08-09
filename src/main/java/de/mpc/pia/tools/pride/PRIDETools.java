package de.mpc.pia.tools.pride;

import de.mpc.pia.tools.MzIdentMLTools;
import de.mpc.pia.tools.OntologyConstants;
import uk.ac.ebi.jmzidml.model.mzidml.Cv;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.Enzyme;
import uk.ac.ebi.jmzidml.model.mzidml.Enzymes;
import uk.ac.ebi.pride.jaxb.model.Identification;
import uk.ac.ebi.pride.jaxb.model.Protocol;
import uk.ac.ebi.pride.jmztab.model.Param;
import uk.ac.ebi.pride.jmztab.model.Protein;

import java.util.Arrays;
import java.util.List;

/**
 * @author Yasset Perez-Riverol (ypriverol@gmail.com)
 * @date 09/02/2016
 */
public class PRIDETools {

    /** PRIDE OBO accession for an enzyme */
    public static final String PRIDE_OBO_ENZYME_ACCESSION = "PRIDE:0000160";

    /** SEP OBO accession for an enzyme */
    public static final String SEPERATION_OBO_ENZYME_ACCESSION = "sep:00142";

    // PRIDE CVTerms
    public static final String PRIDE_DECOY_HIT_ACCESSION = "PRIDE:0000303";
    public static final String PRIDE_DECOY_HIT_NAME = "Decoy";

    public static final List<String> PREFIX_PRIDE_DECOY_ENTRIES  = Arrays.asList("DECOY_", "#C#", "#DECOY#", "###REV###", "REV_", "REVERSE_", "##REV", "DECOY_REV", "RANDOM_", "###RND###", "##RND","REV");
    public static final List<String> POSTFIX_PRIDE_DECOY_ENTRIES = Arrays.asList("_REVERSED", "-DECOY", "RANDOM_","|RND");
    public static final List<String> MIDDLE_PRIDE_DECOY_ENTRIES = Arrays.asList("RANDOM_", "REV_", "_REVERSED");



    private static Cv prideCV = new Cv();

    /*
     * static initialization
     */
    static {
        prideCV.setId("PRIDE");
        prideCV.setFullName("PRIDE Controlled Vocabulary");
        prideCV.setUri(OntologyConstants.PRIDE_CV_URL);
    }


    /**
     * Private constructor to avoid construction
     */
    private PRIDETools() {
        // do nothing
    }


    /**
     * getter for the PRIDE CV
     * @return
     */
    public static Cv getPrideCV() {
        return prideCV;
    }


    /**
     * Convert Param from mzTab to mzIdentML format
     * @param param
     * @return
     */
    public static CvParam convertCvParam(Param param) {
        CvParam cvParam = null;

        if (param != null) {
            if (param.getCvLabel() != null) {
                if (MzIdentMLTools.getCvPSIMS().getId().equals(param.getCvLabel())
                            || "MS".equals(param.getCvLabel())) {
                    cvParam = MzIdentMLTools.createCvParam(
                            param.getAccession(),
                            MzIdentMLTools.getCvPSIMS(),
                            param.getName(),
                            param.getValue());
                } else if (prideCV.getId().equals(param.getCvLabel())) {
                    cvParam = MzIdentMLTools.createCvParam(
                            param.getAccession(),
                            prideCV,
                            param.getName(),
                            param.getValue());
                }

            } else {
                cvParam = new CvParam();

                if (param.getCvLabel() != null) {
                    Cv newCv = new Cv();
                    newCv.setId(param.getCvLabel());
                    newCv.setFullName(param.getCvLabel());
                    cvParam.setCv(newCv);
                }
                if (param.getAccession() != null) {
                    cvParam.setAccession(param.getAccession());
                }
                if(param.getName() != null) {
                    cvParam.setName(param.getName());
                }
                if(param.getValue() != null) {
                    cvParam.setValue(param.getValue());
                }
            }
        }
        return cvParam;
    }

    /**
     * This return an mzIdentML Param
     * @param param
     * @return
     */
    public static uk.ac.ebi.jmzidml.model.mzidml.Param convertParam(Param param){
        uk.ac.ebi.jmzidml.model.mzidml.Param newParam = new uk.ac.ebi.jmzidml.model.mzidml.Param();
        CvParam cvParam = convertCvParam(param);
        newParam.setParam(cvParam);
        return newParam;
    }

    /**
     * Checks whether the passed identification object is a decoy hit. This function only checks for
     * the presence of specific cv / user Params.
     *
     * @param identification A PRIDE JAXB Identification object.
     * @return Boolean indicating whether the passed identification is a decoy hit.
     */
    public static boolean isDecoyHit(Identification identification) {
        if (identification.getAdditional() != null) {
            for (uk.ac.ebi.pride.jaxb.model.CvParam param : identification.getAdditional().getCvParam()) {
                if (PRIDE_DECOY_HIT_ACCESSION.equals(param.getAccession())) {
                    return true;
                }
            }

            for (uk.ac.ebi.pride.jaxb.model.UserParam param : identification.getAdditional().getUserParam()) {
                if ("Decoy Hit".equals(param.getName())) {
                    return true;
                }
                if (PRIDE_DECOY_HIT_NAME.equals(param.getName())){
                    return true;
                }
            }
        }

        return isAccessionDecoy(identification);
    }

    public static boolean isAccessionDecoy(Identification identification) {
        String accession = identification.getAccession();
        for(String prefix: PREFIX_PRIDE_DECOY_ENTRIES)
            if(accession.toUpperCase().startsWith(prefix.toUpperCase()))
                return true;
        for(String postfix: POSTFIX_PRIDE_DECOY_ENTRIES)
            if(accession.toUpperCase().endsWith(postfix.toUpperCase()))
                return true;
        for(String middle: MIDDLE_PRIDE_DECOY_ENTRIES)
            if(accession.toUpperCase().contains(middle.toUpperCase()))
                return true;
        return false;
    }


    /**
     * Tries to get enzyme information from the protocolpart of the PRIDE XML
     *
     * @param prideProtocol
     * @return
     */
    public static Enzymes getEnzymesFromProtocol(Protocol prideProtocol) {
        Enzymes enzymes = new Enzymes();

        boolean enzymeFound = false;

        for (uk.ac.ebi.pride.jaxb.model.Param stepDesc : prideProtocol.getProtocolSteps().getStepDescription()) {
            for (uk.ac.ebi.pride.jaxb.model.CvParam cvParam : stepDesc.getCvParam()) {
                Enzyme enzyme = null;

                if (cvParam.getAccession().equals(PRIDE_OBO_ENZYME_ACCESSION) ||
                        cvParam.getAccession().equals(SEPERATION_OBO_ENZYME_ACCESSION) ||
                        cvParam.getName().contains("enzyme") || cvParam.getName().contains("Enzyme")) {
                    // this is an enzyme, get the respective cleavage agent
                    enzyme = MzIdentMLTools.getEnzymeFromName(cvParam.getValue());
                }

                if (enzyme != null) {
                    enzymes.getEnzyme().add(enzyme);
                    enzymeFound = true;
                }
            }
        }

        return enzymeFound ? enzymes : null;
    }
}
