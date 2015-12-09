package de.mpc.pia.tools.obo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.log4j.Logger;
import org.biojava.nbio.ontology.Ontology;
import org.biojava.nbio.ontology.Term;
import org.biojava.nbio.ontology.Triple;
import org.biojava.nbio.ontology.io.OboParser;


public class OBOMapper {

    /** the logger for this class */
    private static final Logger logger = Logger.getLogger(OBOMapper.class);

    /** the actual ontology in the OBO file */
    private Ontology onlineOntology;

    /** the actual ontology in the OBO file */
    private Ontology shippedOntology;

    /** the URL to the current OBO file */
    public final String urlToOBO = "https://raw.githubusercontent.com/HUPO-PSI/mzML/master/cv/psi-ms.obo";

    public static final String obo_cleavageAgentNameID = "MS:1001045";
    public static final String obo_relationship = "relationship";
    public static final String obo_is_a = "is_a";
    public static final String obo_has_regexp = "has_regexp";
    public static final String obo_has_order_higherscorebetter = "has_order MS:1002108";
    public static final String obo_has_order_lowerscorebetter = "has_order MS:1002109";
    public static final String obo_spectrumTitleID = "MS:1000796";
    public static final String obo_psmScoreID = "MS:1001143";

    /**
     * Constructor for the OBOMapper. Uses the online OBO file (if accessible)
     * or a locally shipped file.
     *
     * @param pathToFile
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
        InputStream inStream;

        try {
            // get the shipped ontology
            inStream = OBOMapper.class.getResourceAsStream("/de/mpc/pia/psi-ms.obo");

            OboParser parser = new OboParser();
            BufferedReader oboFile = new BufferedReader(new InputStreamReader(inStream));

            shippedOntology = parser.parseOBO(oboFile, "PSI-MS", "MS ontology of the HUPO-PSI");
            inStream.close();

            // get the internet ontology
            if (useOnline) {
                inStream = new URL(urlToOBO).openStream();

                parser = new OboParser();
                oboFile = new BufferedReader(new InputStreamReader(inStream));

                onlineOntology = parser.parseOBO(oboFile, "PSI-MS", "MS ontology of the HUPO-PSI");
                inStream.close();
            } else {
                onlineOntology = null;
            }
        } catch (IOException e) {
            onlineOntology = null;
            if (useOnline) {
                logger.warn("could not use remote obo file, check internet connection");
            }
        } catch (Exception e) {
            logger.error(e);
            throw new AssertionError(e);
        }
    }


    /**
     * Fetch {@link Term} with specified name
     *
     * @param name
     * @return
     */
    public Term getTerm(String name) {
        try {
            if (onlineOntology != null) {
                return onlineOntology.getTerm(name);
            } else {
                return shippedOntology.getTerm(name);
            }
        } catch (NoSuchElementException e) {
            logger.warn(e);
            return null;
        }
    }


    /**
     * Fetch all {@link Term}s of the ontology
     *
     * @return
     */
    public Set<Term> getTerms() {
        try {
            if (onlineOntology != null) {
                return onlineOntology.getTerms();
            } else {
                return shippedOntology.getTerms();
            }
        } catch (NoSuchElementException e) {
            logger.warn(e);
            return null;
        }
    }


    /**
     * Return all triples from this ontology which match the supplied pattern.
     * If any of the parameters of this method are null, they are treated as
     * wildcards.
     *
     * @param subject
     * @param object
     * @param predicate
     * @return
     */
    public Set<Triple> getTriples(Term subject, Term object, Term predicate) {
        if (onlineOntology != null) {
            return onlineOntology.getTriples(subject, object, predicate);
        } else {
            return shippedOntology.getTriples(subject, object, predicate);
        }
    }
}
