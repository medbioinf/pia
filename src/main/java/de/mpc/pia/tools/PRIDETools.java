package de.mpc.pia.tools;

import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.pride.jaxb.model.Identification;
import uk.ac.ebi.pride.jmztab.model.Param;

/**
 * @author Yasset Perez-Riverol (ypriverol@gmail.com)
 * @date 09/02/2016
 */
public class PRIDETools {

    public static String OPTIONAL_SEQUENCE_COLUMN = "protein_sequence";

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
                if (param.getAccession().equals(PIAConstants.PRIDE_DECOY_HIT)) {
                    return true;
                }
            }

            for (uk.ac.ebi.pride.jaxb.model.UserParam param : identification.getAdditional().getUserParam()) {
                if ("Decoy Hit".equals(param.getName())) {
                    return true;
                }
                if(PIAConstants.PRIDE_DECOY_HIT_DESC.equals(param.getName())){
                    return true;
                }
            }
        }

        return false;
    }



}
