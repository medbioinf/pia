package de.mpc.pia.tools.matomo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;

import de.mpc.pia.tools.PIAConstants;


/**
 * This class handles everything needed for the recording of usage statistics of PIA calls using Matomo.
 *
 * @author julian
 *
 */
public class PIAMatomoTracker {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(PIAMatomoTracker.class);

    /** whether to enable the tracking **/
    private static boolean disableUsageStatistics = false;

    /** name of the configuration file */
    private static final String DEFAULT_CONFIG_FILE = "matomo.props";

    /** path to the configuration file */
    private static final String DEFAULT_CONFIG_FILE_PATH = System.getProperty("user.home") + File.separator +".pia";

    private static final String UTF8 = "UTF-8";

    /** random generator for this instance */
    private static SecureRandom random = new SecureRandom();

    /** the visitor CID, read from the PIA config file */
    private static String visitorCid = null;

    /** a unique Id for this static instance */
    private static String sessionId = new BigInteger(64, random).toString(16);


    /** the URL to the tracking page */
    private static final String PIA_MATOMO_TRACKING_URL = "https://mpc-piwik.phc.ruhr-uni-bochum.de/piwik.php";

    /** the ID for Matomo tracking */
    private static final int PIA_MATOMO_TRACKING_SITE_ID = 4;


    /** event category for command line call */
    public static final String PIA_TRACKING_COMMAND_LINE_CATEGORY = "PIA_cli";
    /** event category for KNIME call */
    public static final String PIA_TRACKING_KNIME_CATEGORY = "PIA_knime";

    /** event name regarding the compiler */
    public static final String PIA_TRACKING_COMPILER_NAME = "compiler";
    /** event name regarding the modeller */
    public static final String PIA_TRACKING_MODELLER_NAME = "modeller";
    /** event name regarding the viewer */
    public static final String PIA_TRACKING_VIEWER_NAME = "viewer";

    /** event action compiler started */
    public static final String PIA_TRACKING_COMPILER_STARTED = "compiler_started";
    /** event action compiler finished */
    public static final String PIA_TRACKING_COMPILER_FINISHED = "compiler_finished";
    /** event action compiler aborted */
    public static final String PIA_TRACKING_COMPILER_ERROR = "compiler_error";

    /** event action modeller with XML workflow started */
    public static final String PIA_TRACKING_MODELLER_XML_STARTED = "modeller_XML_started";
    /** event action modeller with commands directly from the CLI started */
    public static final String PIA_TRACKING_MODELLER_CLI_STARTED = "modeller_CLI_started";
    /** event action modeller started from inside KNIME */
    public static final String PIA_TRACKING_MODELLER_KNIME_STARTED = "modeller_KNIME_started";
    /** event action modeller with XML workflow finished */
    public static final String PIA_TRACKING_MODELLER_FINISHED = "modeller_finished";
    /** event action modeller with XML workflow aborted */
    public static final String PIA_TRACKING_MODELLER_ERROR = "modeller_error";

    /** event action viewer of the analysis in KNIME */
    public static final String PIA_TRACKING_VIEWER_KNIME_ANALYSIS = "viewer_KNIME_analysis";
    /** event action viewer of the analysis in KNIME had an error */
    public static final String PIA_TRACKING_VIEWER_KNIME_ANALYSIS_ERROR = "viewer_KNIME_analysis_error";
    /** event action spectrum viewer in KNIME */
    public static final String PIA_TRACKING_VIEWER_KNIME_SPECTRA = "viewer_KNIME_spectra";



    private PIAMatomoTracker() {
        // not to be called, all things static in this class
    }


    /**
     * Disables or enables the global setting for tracking.
     *
     * @param disable
     */
    public static void disableTracking(boolean disable) {
        disableUsageStatistics = disable;
    }


    /**
     * Read in the configuration data from the stored configuration file. This will mainly set the visitor CID.
     * If there were problems reading the CID from file, a new one will be generated. So, after calling this method, a
     * CID is set.
     */
    private static void readConfigFile() {
        File configDir = new File(DEFAULT_CONFIG_FILE_PATH);

        boolean pathOk = configDir.exists() || configDir.mkdirs();

        if (pathOk) {
            String settingsFileString = DEFAULT_CONFIG_FILE_PATH + File.separator + DEFAULT_CONFIG_FILE;
            File settingsFile = new File(settingsFileString);

            try {
                if (!settingsFile.exists()) {
                    createNewConfigFile(settingsFile);
                }

                Properties properties = new Properties();
                properties.load(new FileInputStream(settingsFile));
                visitorCid = properties.getProperty("visitorCid");
            } catch (IOException e) {
                // silently ignore that no properties file was created
                LOGGER.debug("problems with matomo properties file", e);
                // make sure, a visitor CID was created though
                if (visitorCid == null) {
                    createNewVisitorCID();
                }
            }
        }
    }


    /**
     * Tries to create a new config file for PIA.
     *
     * @param configFile
     * @throws IOException
     */
    private static void createNewConfigFile(File configFile) throws IOException {
        if (!configFile.createNewFile()) {
            throw new IOException("Cannot create config file " + configFile.getAbsolutePath());
        }

        if (visitorCid == null) {
            createNewVisitorCID();
        }

        Properties properties = new Properties();
        properties.setProperty("visitorCid", visitorCid);
        properties.store(new FileOutputStream(configFile), "config file for PIA");
    }


    /**
     * Create a new random visitor CID
     */
    private static void createNewVisitorCID() {
        visitorCid = null;
        while ((visitorCid == null) || (visitorCid.length() < 16)) {
            visitorCid = new BigInteger(64, random).toString(16);
        }
    }


    /**
     * Function used to track a PIA event of any kind. If the visitorCid was not set before (or is null), it will be
     * fetched from the configuration file or newly created and stored in a config file in the user's home directory.
     *
     * @param eventCategory the category, like e.g. "PIA_cli" or "PIA_KNIME"
     * @param eventName name of the event, like "compiler", "analysis", "export"
     * @param eventAction action of the event, like "compiler_started", "compiler_finished", "compiler_aborted"
     * @param eventValue an (optional) value for the event, null will remove this and not record it
     */
    public static void trackPIAEvent(String eventCategory, String eventName, String eventAction, Number eventValue) {
        if (disableUsageStatistics) {
            LOGGER.debug("usage statistics disabled");
            return;
        }

        try {
            LOGGER.debug("usage statistics enabled");

            if (visitorCid == null) {
                // get pia tracking data, if it was not set yet
                readConfigFile();
            }

            LOGGER.debug("usage statistics vCID " + visitorCid);

            String getRequest = "idsite=" + PIA_MATOMO_TRACKING_SITE_ID
                    + "&rec=1"
                    + "&_id=" + sessionId
                    + "&rand=" + (new BigInteger(64, random).toString(20))
                    + "&apiv=1"
                    + "&cid=" + visitorCid
                    + "&e_c=" + URLEncoder.encode(eventCategory, UTF8)
                    + "&e_a=" + URLEncoder.encode(eventAction, UTF8)
                    + "&e_n=" + URLEncoder.encode(eventName, UTF8)
                    + "&send_image=0";

            if (eventValue != null) {
                getRequest += "&e_v=" + eventValue;
            }

            // the custom variables are json encoded
            String customVariablesString = "&cvar="
                    + URLEncoder.encode("{"
                            + "\"1\":[\"operating_system\",\"" + System.getProperty("os.name") + "\"],"
                            + "\"2\":[\"pia_version\",\"" + PIAConstants.version + "\"],"
                            + "}", UTF8);
            getRequest += customVariablesString;

            LOGGER.debug("going to track PIA action with Matomo: " + eventCategory + ", " + eventName + ", "
                    + eventAction + ", " + eventValue);

            // make the actual request via GET
            HttpsURLConnection connection = (HttpsURLConnection) new URL(PIA_MATOMO_TRACKING_URL + "?" +
                    getRequest).openConnection();
            connection.setRequestMethod("GET");
            connection.getResponseCode();

            LOGGER.debug("tracked PIA action with Matomo: " + eventCategory + ", " + eventName + ", "
                    + eventAction + ", " + eventValue);
        } catch (Exception e) {
            // silently ignore any exception
            LOGGER.debug("problem during matomo tracking", e);
        }
    }


    /**
     * Function used to track a PIA event of any kind with the given visitorCid.
     *
     * @param eventCategory the category, like e.g. "PIA_cli" or "PIA_KNIME"
     * @param eventName name of the event, like "compiler", "analysis", "export"
     * @param eventAction action of the event, like "compiler_started", "compiler_finished", "compiler_aborted"
     * @param eventValue an (optional) value for the event, null will remove this and not record it
     * @param setVisitorCid sets the visitorCid to the given value (16 character hexadecimal string)
     */
    public static void trackPIAEvent(String eventCategory, String eventName, String eventAction, Number eventValue,
            String setVisitorCid) {
        visitorCid = setVisitorCid;
        trackPIAEvent(eventCategory, eventName, eventAction, eventValue);
    }
}
