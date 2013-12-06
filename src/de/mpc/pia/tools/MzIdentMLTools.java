package de.mpc.pia.tools;

import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.FileFormat;
import uk.ac.ebi.jmzidml.model.mzidml.Param;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIDFormat;
import uk.ac.ebi.jmzidml.model.mzidml.UserParam;


/**
 * Some additional functions for handling mzIdentML files.
 * 
 * @author julian
 *
 */
public class MzIdentMLTools {
	
	/**
	 * We don't ever want to instantiate this class
	 */
	private MzIdentMLTools() {
		throw new AssertionError();
	}
	

	/**
	 * Compares the given Params for equality, i.e. all the Strings in it.
	 * Returns also true, if both params are null.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static boolean paramsEqual(Param x, Param y) {
		if ((x == null) && (y == null)) {
			// both are null
			return true;
		}
		
		CvParam cv1 = x.getCvParam();
		CvParam cv2 = y.getCvParam();
		if ((cv1 != null) && (cv2 != null)) {
			// both are cvParams
			return cvParamsEqualOrNull(cv1, cv2);
		} else {
			UserParam up1 = x.getUserParam();
			UserParam up2 = y.getUserParam();
			
			return userParamsEqualOrNull(up1, up2);
		}
	}
	
	
	/**
	 * Checks for the given cvParams, whether both are null are equal.
	 */
	public static boolean cvParamsEqualOrNull(CvParam x, CvParam y) {
		if ((x != null) && (y != null)) {
			// both are not null
			boolean equal = true;
			
			// required
			equal &= x.getAccession().equals(y.getAccession());
			equal &= x.getCvRef().equals(y.getCvRef());
			equal &= x.getName().equals(y.getName());
			
			// optional
			equal &= PIATools.bothNullOrEqual(x.getUnitAccession(),
					y.getUnitAccession());
			equal &= PIATools.bothNullOrEqual(x.getUnitCvRef(),
					y.getUnitCvRef());
			equal &= PIATools.bothNullOrEqual(x.getUnitName(),
					y.getUnitName());
			equal &= PIATools.bothNullOrEqual(x.getValue(),
					y.getValue());
			
			return equal;
		} else {
			// both must be null then
			return (x == null) && (y == null);
		}
	}
	
	
	/**
	 * Checks for the given userParams, whether both are null are equal.
	 */
	public static boolean userParamsEqualOrNull(UserParam x, UserParam y) {
		if ((x != null) && (y != null)) {
			// both are not null
			boolean equal = true;
			
			// required
			equal &= x.getName().equals(y.getName());
			
			// optional
			equal &= PIATools.bothNullOrEqual(x.getType(),
					y.getType());
			equal &= PIATools.bothNullOrEqual(x.getUnitAccession(),
					y.getUnitAccession());
			equal &= PIATools.bothNullOrEqual(x.getUnitCvRef(),
					y.getUnitCvRef());
			equal &= PIATools.bothNullOrEqual(x.getUnitName(),
					y.getUnitName());
			equal &= PIATools.bothNullOrEqual(x.getValue(),
					y.getValue());
			
			
			return equal;
		} else {
			// both must be null then
			return (x == null) && (y == null);
		}
	}
	
	
	/**
	 * Checks for the given fileFormats, whether both are null are equal.
	 */
	public static boolean fileFormatsEqualOrNull(FileFormat x, FileFormat y) {
		if ((x != null) && (y != null)) {
			// both are not null
			return MzIdentMLTools.cvParamsEqualOrNull(x.getCvParam(),
					y.getCvParam());
		} else {
			// both must be null to be equal
			return (x == null) && (y == null);
		}
	}
	
	
	/**
	 * Checks for the given spectrumIDFormats, whether both are null are equal.
	 */
	public static boolean spectrumIDFormatEqualOrNull(SpectrumIDFormat x,
			SpectrumIDFormat y) {
		if ((x != null) && (y != null)) {
			// both are not null
			return MzIdentMLTools.cvParamsEqualOrNull(x.getCvParam(),
					y.getCvParam());
		} else {
			// both must be null to be equal
			return (x == null) && (y == null);
		}
	}

}
