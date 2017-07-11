package de.mpc.pia.tools.obo;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.log4j.Logger;
import org.biojava.nbio.ontology.Ontology;
import org.biojava.nbio.ontology.Term;
import org.biojava.nbio.ontology.Triple;

/**
 * Abstract parser for an obo ontology.
 *
 * @author julianu
 *
 */
public abstract class AbstractOBOMapper {

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(AbstractOBOMapper.class);

    // some statics
    public static final String OBO_RELATIONSHIP = "relationship";
    public static final String OBO_IS_A = "is_a";


    /**
     * Returns the currently used ontology. Usual either a shipped file or online version.
     *
     * @return
     */
    public abstract Ontology getCurrentOntology();


    /**
     * Fetch {@link Term} with specified accession
     *
     * @param accession
     * @return
     */
    public Term getTerm(String accession) {
        try {
            return getCurrentOntology().getTerm(accession);
        } catch (NoSuchElementException e) {
            LOGGER.info("No term with accession '" + accession + "' was found", e);
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
            return getCurrentOntology().getTerms();
        } catch (NoSuchElementException e) {
            LOGGER.info("No term found for ontology", e);
            return Collections.emptySet();
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
        return getCurrentOntology().getTriples(subject, object, predicate);
    }


    /**
     * Gets the entry in the OBO with the given name. If none is found, returns
     * null.
     *
     * @param name
     * @return
     */
    public Term getTermByName(String name) {
        Set<Term> keys = getTerms();
        for (Term term : keys) {
            if (name.equals(term.getDescription())) {
                return term;
            }
        }

        return null;
    }
}
