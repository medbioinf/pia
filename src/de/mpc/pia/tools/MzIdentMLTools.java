package de.mpc.pia.tools;

import java.util.regex.Pattern;

import uk.ac.ebi.jmzidml.model.mzidml.AbstractParam;
import uk.ac.ebi.jmzidml.model.mzidml.Cv;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.Enzyme;
import uk.ac.ebi.jmzidml.model.mzidml.FileFormat;
import uk.ac.ebi.jmzidml.model.mzidml.Param;
import uk.ac.ebi.jmzidml.model.mzidml.ParamList;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIDFormat;
import uk.ac.ebi.jmzidml.model.mzidml.UserParam;


/**
 * Some additional functions for handling mzIdentML files.
 * 
 * @author julian
 *
 */
public class MzIdentMLTools {
	
	/** get the index from the title, which is the scan number there */
	public static Pattern patternScanInTitle = Pattern.compile("^.*scan=(\\d+).*$");
	
	private static Cv psiMS = new Cv();
	private static Cv unitOntology = new Cv();
	
	// static initialization
	static {
		psiMS.setId("PSI-MS");
		psiMS.setFullName("PSI-MS");
		psiMS.setUri("http://psidev.cvs.sourceforge.net/viewvc/*checkout*/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo");
		psiMS.setVersion("3.74.0");
		
		unitOntology.setId("UO");
		unitOntology.setFullName("PSI-MS");
		unitOntology.setUri("http://unit-ontology.googlecode.com/svn/trunk/unit.obo");
	}
	
	
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
		} else if ((x == null) || (y == null)) {
			return false;
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
	
	
	/**
	 * Adds the correct unit arguments to an {@link AbstractParam}, which is
	 * given from a (parsed) string.
	 * <p>
	 * The abstractParam's value should be set before, as it is used for the
	 * conversion from mmu to Dalton.
	 * 
	 * @param unit
	 * @param abstractParam
	 * @return
	 */
	public static boolean setUnitParameterFromString(String unit, AbstractParam abstractParam) {
		if ((unit == null) || (abstractParam == null)) {
			return false;
		}
		
		unit = unit.trim();
		
		if (unit.equalsIgnoreCase("Da") ||
				unit.equalsIgnoreCase("Dalton") ||
				unit.equalsIgnoreCase("amu") ||
				unit.equalsIgnoreCase("u") ||
				unit.equalsIgnoreCase("unified atomic mass unit")) {
			abstractParam.setUnitAccession("UO:0000221");
			abstractParam.setUnitCv(getUnitOntology());
			abstractParam.setUnitName("dalton");
			return true;
		} else if (unit.equalsIgnoreCase("ppm") ||
				unit.equalsIgnoreCase("parts per million")) {
			abstractParam.setUnitAccession("UO:0000169");
			abstractParam.setUnitCv(getUnitOntology());
			abstractParam.setUnitName("parts per million");
			return true;
		} else if (unit.equalsIgnoreCase("percent") ||
				unit.equalsIgnoreCase("%")) {
			abstractParam.setUnitAccession("UO:0000187");
			abstractParam.setUnitCv(getUnitOntology());
			abstractParam.setUnitName("percent");
			return true;
		} else if (unit.equalsIgnoreCase("mmu")) {
			// one mmu is 0.001 Da -> calculate it to dalton
			try {
				Double value = Double.parseDouble(abstractParam.getValue());
				value *= 0.001;
				abstractParam.setValue(value.toString());
				abstractParam.setUnitAccession("UO:0000221");
				abstractParam.setUnitCv(getUnitOntology());
				abstractParam.setUnitName("dalton");
			} catch (NumberFormatException e) {
				// could not
				abstractParam.setUnitName("mmu");
				return false;
			}
		}
		
		return false;
	}
	
	
	/**
	 * Create an {@link Enzyme} corresponding to the given name
	 * 
	 * @param enzymeString
	 * @return
	 */
	public static Enzyme getEnzymeFromName(String enzymeString) {
		if ((enzymeString == null) || (enzymeString.trim().length() == 0)) {
			return null;
		}
		
		CleavageAgent agent = CleavageAgent.getByName(enzymeString);
		Enzyme enzyme = new Enzyme();
		ParamList paramList = new ParamList();
		
		if (agent != null) {
			CvParam cvParam = new CvParam();
			cvParam.setAccession(agent.getAccession());
			cvParam.setCv(MzIdentMLTools.getCvPSIMS());
			cvParam.setName(agent.getName());
			
			enzyme.setSiteRegexp(agent.getSiteRegexp());
			
			paramList.getCvParam().add(cvParam);
			enzyme.setId("enzyme_" + cvParam.getAccession());
		} else {
			UserParam userParam = new UserParam();
			userParam.setName(enzymeString);
			
			paramList.getUserParam().add(userParam);
			enzyme.setId("enzyme_" + userParam.getName());
		}
		
		enzyme.setEnzymeName(paramList);
		return enzyme;
	}
	
	
	/**
	 * Getter for the PSI-MS controlled vocabulary
	 * @return
	 */
	public static Cv getCvPSIMS() {
		return psiMS;
	}
	
	
	/**
	 * Getter for the UnitOntology controlled vocabulary
	 * @return
	 */
	public static Cv getUnitOntology() {
		return unitOntology;
	}
}
