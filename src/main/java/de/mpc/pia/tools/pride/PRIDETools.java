package de.mpc.pia.tools.pride;

import de.mpc.pia.tools.MzIdentMLTools;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.Enzyme;
import uk.ac.ebi.jmzidml.model.mzidml.Enzymes;
import uk.ac.ebi.pride.jaxb.model.Identification;
import uk.ac.ebi.pride.jaxb.model.Protocol;
import uk.ac.ebi.pride.jmztab.model.Param;

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


    /**
     * Convert Param from mzTab to mzIdentML format
     * @param param
     * @return
     */
    public static CvParam convertCvParam(Param param) {
        if(param != null){
            CvParam cvParam = new CvParam();
            if(param.getAccession() != null) cvParam.setAccession(param.getAccession());
            if(param.getName() != null) cvParam.setName(param.getName());
            if(param.getValue() != null) cvParam.setValue(param.getValue());
            if(param.getCvLabel() != null) cvParam.setUnitAccession(param.getCvLabel());
            return cvParam;
        }
        return null;
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
