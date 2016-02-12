package de.mpc.pia.tools;

import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.pride.jmztab.model.Param;

/**
 * @author Yasset Perez-Riverol (ypriverol@gmail.com)
 * @date 09/02/2016
 */
public class MzTabTools {

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



}
