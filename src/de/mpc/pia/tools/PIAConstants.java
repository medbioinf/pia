package de.mpc.pia.tools;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * This class holds some constants, which are universally used within PIA.
 * 
 * @author julian
 *
 */
public class PIAConstants {
	
	/** the current version of PIA */
	public static String version;
	
	
	/** the mass of one hydrogen ion (H+) */
	public static BigDecimal H_MASS = new BigDecimal("1.007276");
	
	
	/** the mass tolerance for finding a modification by mass in Unimod */
	public static Double unimod_mass_tolerance = 0.001;
	
	
	// prefixes for the IDs from mzIdentML
	public static String software_prefix = "software_";
	public static String identification_protocol_prefix = "identProtocol_";
	public static String spectrum_identification_prefix = "specIdent_";
	public static String databases_prefix = "searchDB_";
	public static String spectra_data_prefix = "spectraData_";
	
	
	// prefixes for mzTab export
	public static String MZTAB_MISSED_CLEAVAGES_COLUMN_NAME = "missed_cleavages";
	public static String MZTAB_NR_PEPTIDES_COLUMN_NAME = "number_of_peptides";
	public static String MZTAB_NR_PSMS_COLUMN_NAME = "number_of_psms";
	public static String MZTAB_NR_SPECTRA_COLUMN_NAME = "number_of_spectra";
	
	
	static { 
		Logger logger = Logger.getLogger(PIAConstants.class);
		
		InputStream inputStream =
				PIAConstants.class.getResourceAsStream("/de/mpc/pia/general.properties");
		Properties properties = new Properties();
		
		if (inputStream != null) {
			try {
				properties.load(inputStream);
			} catch (IOException e) {
				logger.error("Error reading the properties file 'de.mpc.pia.general.properties'! " + e);
			}
		} else {
			logger.error("Could not open the properties file'! ");
		}
		
		version = properties.getProperty("pia_version");
	}
	
	
	
	
	// some information for the used CVs
	/** the label for the PSI-MS CV */
	public static final String CV_PSI_MS_LABEL = "MS";
	
	/** PIAs accession in the PSI-CV*/
	public static final String CV_PIA_ACCESSION = "MS:XXXXX";
	/** PIAs name in the PSI-CV*/
	public static final String CV_PIA_NAME = "PIA";
	
	/** accession for the "PIA protein score" in PSI-MS */
	public static final String CV_PIA_PROTEIN_SCORE_ACCESSION = "MS:XXXXX";
	/** name for the "PIA protein score" in PSI-MS */
	public static final String CV_PIA_PROTEIN_SCORE_NAME = "PIA protein score";
	
	/** accession for "PSM-level FDRScore" in PSI-MS */
	public static final String CV_PSM_LEVEL_FDRSCORE_ACCESSION = "MS:1002355";
	/** name for "PSM-level FDRScore" in PSI-MS */
	public static final String CV_PSM_LEVEL_FDRSCORE_NAME = "PSM-level FDRScore";
	
	/** accession for "PSM-level combined FDRScore" in PSI-MS */
	public static final String CV_PSM_LEVEL_COMBINED_FDRSCORE_ACCESSION = "MS:1002356";
	/** name for "PSM-level combined FDRScore" in PSI-MS */
	public static final String CV_PSM_LEVEL_COMBINED_FDRSCORE_NAME = "PSM-level combined FDRScore";

	
	/**
	 * We don't ever want to instantiate this class
	 */
	private PIAConstants() {
		throw new AssertionError();
	}
}
