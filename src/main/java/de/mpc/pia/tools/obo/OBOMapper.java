package de.mpc.pia.tools.obo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.log4j.Logger;
import org.biojava.nbio.ontology.Ontology;
import org.biojava.nbio.ontology.io.OboParser;

import de.mpc.pia.tools.OntologyConstants;


/**
 * Parser for the MS-OBO ontology of the HUPO-PSI.
 *
 * @author julianu
 *
 */
public class OBOMapper extends AbstractOBOMapper {

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(OBOMapper.class);

    /** the actual ontology in the OBO file */
    private Ontology onlineOntology;

    /** the actual ontology in the OBO file */
    private Ontology shippedOntology;

    // some statics
    public static final String OBO_HAS_REGEXP = "has_regexp";
    public static final String OBO_HAS_ORDER_HIGHERSCOREBETTER = "has_order MS:1002108";
    public static final String OBO_HAS_ORDER_LOWERSCOREBETTER = "has_order MS:1002109";


    /**
     * Constructor for the OBOMapper. Uses the online OBO file (if accessible)
     * or a locally shipped file.
     *
     */
    public OBOMapper() {
        this(true);
    }


    /**
     * Creates a new OBOMapper, using the online OBO or the shipped only.
     *
     * @param useOnline whether to use the online OBO
     */
    public OBOMapper(boolean useOnline) {
        try (InputStream inStreamOffline = OBOMapper.class.getResourceAsStream("/de/mpc/pia/psi-ms.obo")) {
            // get the shipped ontology
            OboParser parser = new OboParser();
            BufferedReader oboFile = new BufferedReader(new InputStreamReader(inStreamOffline));

            shippedOntology = parser.parseOBO(oboFile, "PSI-MS", "MS ontology of the HUPO-PSI");

            // get the online ontology
            if (useOnline) {
                try (InputStream inStreamOnline = new URL(OntologyConstants.PSI_MS_OBO_URL).openStream()) {
                    parser = new OboParser();
                    oboFile = new BufferedReader(new InputStreamReader(inStreamOnline));

                    onlineOntology = parser.parseOBO(oboFile, "PSI-MS", "MS ontology of the HUPO-PSI");
                }
            } else {
                onlineOntology = null;
            }
        } catch (IOException e) {
            onlineOntology = null;
            if (useOnline) {
                LOGGER.warn("could not use remote obo file, check internet connection", e);
            }
        } catch (Exception e) {
            LOGGER.error(e);
            throw new AssertionError(e);
        }
    }


    @Override
    public Ontology getCurrentOntology() {
        if (onlineOntology != null) {
            return onlineOntology;
        } else {
            return shippedOntology;
        }
    }
}
