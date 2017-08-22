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

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(PIAConstants.class);


    /** the current version of PIA */
    public static final String version;

    /** the mass of one hydrogen ion (H+) */
    public static final BigDecimal H_MASS = new BigDecimal("1.007276");

    /** the mass of one water, when it is lost */
    public static final BigDecimal DEHYDRATION_MASS = new BigDecimal("18.010565");

    /** the location of the PIA source */
    public static final String PIA_REPOSITORY_LOCATION = "https://github.com/mpc-bioinformatics/pia";

    /** the substitute for an FDR Score of 0 (if there were no decoys) */
    public static final Double SMALL_FDRSCORE_SUBSTITUTE = new Double(1.0E-23);

    /** precision for rounding the retention time (e.g. for PSM set generation) */
    public static final Integer RETENTION_TIME_PRECISION = 0;

    /** precision for rounding the mass to charge (e.g. for PSM set generation) */
    public static final Integer MASS_TO_CHARGE_PRECISION = 3;


    // prefixes for the IDs from mzIdentML
    public static final String software_prefix = "software_";
    public static final String identification_protocol_prefix = "identProtocol_";
    public static final String spectrum_identification_prefix = "specIdent_";
    public static final String databases_prefix = "searchDB_";
    public static final String spectra_data_prefix = "spectraData_";
    public static final String enzyme_prefix = "enzyme_";


    // prefixes for mzTab export
    public static final String MZTAB_MISSED_CLEAVAGES_COLUMN_NAME = "missed_cleavages";
    public static final String MZTAB_NR_PSMS_COLUMN_NAME = "number_of_psms";
    public static final String MZTAB_NR_SPECTRA_COLUMN_NAME = "number_of_spectra";


    /**
     * Set the version from the properties file, which is always set by maven
     */
    static {
        InputStream inputStream =
                PIAConstants.class.getResourceAsStream("/de/mpc/pia/general.properties");
        Properties properties = new Properties();

        if (inputStream != null) {
            try {
                properties.load(inputStream);
            } catch (IOException e) {
                LOGGER.error("Error reading the properties file 'de.mpc.pia.general.properties'! " + e);
            }
        } else {
            LOGGER.error("Could not open the properties file'! ");
        }

        version = properties.getProperty("pia_version");
    }

    /**
     * Don't ever instantiate this class
     */
    private PIAConstants() {
        throw new AssertionError();
    }
}
