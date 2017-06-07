package de.mpc.pia.tools.obo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.biojava.nbio.ontology.Ontology;
import org.biojava.nbio.ontology.Term;
import org.biojava.nbio.ontology.io.OboParser;
import org.biojava.nbio.ontology.utils.Annotation;

import de.mpc.pia.tools.MzIdentMLTools;
import de.mpc.pia.tools.OntologyConstants;
import de.mpc.pia.tools.unimod.UnimodParser;
import de.mpc.pia.tools.unimod.jaxb.ModT;
import de.mpc.pia.tools.unimod.jaxb.PositionT;
import de.mpc.pia.tools.unimod.jaxb.SpecificityT;
import uk.ac.ebi.jmzidml.model.mzidml.Modification;
import uk.ac.ebi.jmzidml.model.mzidml.SearchModification;
import uk.ac.ebi.jmzidml.model.mzidml.SpecificityRules;


public class PsiModParser extends AbstractOBOMapper {

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(PsiModParser.class);

    /** the actual ontology in the OBO file */
    private Ontology ontology;

    /** pattern to identify the unimod id in a definition */
    private static Pattern unimodInDescription = Pattern.compile(".+\\[.*UniMod:([^, ]+).*\\].*$");


    /**
     * Constructor for the PsiModParser. Uses the online OBO file (if accessible)
     * or a locally shipped file otherwise.
     *
     */
    public PsiModParser() {
        this(true);
    }


    /**
     * Creates a new PsiModParser, using the online OBO or the shipped file only.
     *
     * @param useOnline whether to use the online OBO
     */
    public PsiModParser(boolean useOnline) {
        InputStream inStream;

        try {
            // get the shipped ontology
            inStream = OBOMapper.class.getResourceAsStream("/de/mpc/pia/PSI-MOD.obo");

            OboParser parser = new OboParser();
            BufferedReader oboFile = new BufferedReader(new InputStreamReader(inStream));

            ontology = parser.parseOBO(oboFile, "PSI-MOD", "modifications defined by the HUPO-PSI");
            inStream.close();
        } catch (IOException e) {
            if (!useOnline) {
                LOGGER.warn("could not read local obo file", e);
            }
        } catch (Exception e) {
            if (!useOnline) {
                LOGGER.error(e);
            }
        }

        if (useOnline) {
            try {
                // get the internet ontology
                inStream = new URL(OntologyConstants.PSI_MOD_OBO_URL).openStream();

                OboParser parser = new OboParser();
                BufferedReader oboFile = new BufferedReader(new InputStreamReader(inStream));

                ontology = parser.parseOBO(oboFile, "PSI-MOD", "modifications defined by the HUPO-PSI");
                inStream.close();
            } catch (IOException e) {
                LOGGER.warn("Could not read online obo file, check internet connection.", e);
            } catch (Exception e) {
                LOGGER.error(e);
            }
        }

        if (ontology == null) {
            throw new AssertionError("Could not read ontology file.");
        }
    }


    @Override
    public Ontology getCurrentOntology() {
        return ontology;
    }


    /**
     * Gets and returns the equivalent of the given Term from Unimod.
     *
     * @param term
     * @param unimodParser
     * @return
     */
    public ModT getUnimodEquivalent(Term term, UnimodParser unimodParser) {
        String[] accAndRes = getUnimodAccessionAndResidue(term);
        String accession = accAndRes[0];
        String residue = accAndRes[1];
        Double massdelta = null;

        return unimodParser.getModification(accession, null, massdelta, residue);
    }


    /**
     * Looks for information about the UniMod equivalent in the definition
     *
     * @param term
     * @return an array containing the UniMod ID and, if given, the residue
     */
    private static String[] getUnimodAccessionAndResidue(Term term) {
        String[] split = new String[2];
        Annotation anno = term.getAnnotation();

        if (anno.containsProperty("def")) {
            String definition = anno.getProperty("def").toString();
            Matcher m = unimodInDescription.matcher(definition);
            if (m.matches()) {
                if (m.group(1).contains("#")) {
                    // the residue is provided in PSI-MOD
                    split = m.group(1).split("#", 2);
                } else {
                    split[0] = m.group(1);
                }

            }
        }

        return split;
    }


    /**
     * Creates the mzIdentML conform modification for the given term.
     *
     * @param pos
     * @param residue
     * @param term
     * @param unimodParser
     * @return
     */
    public Modification getUnimodEquivalentModification(Integer pos, String residue, Term term,
            UnimodParser unimodParser) {
        List<String> residues = new ArrayList<>(1);
        residues.add(residue);

        return getUnimodEquivalentModification(pos, residues, term, unimodParser);
    }


    /**
     * Returns the UniMod object for the given term.
     *
     * @param term
     * @param unimodParser
     * @return
     */
    private static ModT getUnimodByTerm(Term term, UnimodParser unimodParser) {
        String[] accAndRes = getUnimodAccessionAndResidue(term);
        String accession = accAndRes[0];
        String residue = accAndRes[1];
        Double massdelta = null;

        return unimodParser.getModification(accession, null, massdelta, residue);
    }


    /**
     * Creates the mzIdentML conform modification for the given term.
     *
     * @param pos
     * @param residues
     * @param term
     * @param unimodParser
     * @return
     */
    public Modification getUnimodEquivalentModification(Integer pos, List<String> residues, Term term,
            UnimodParser unimodParser) {
        ModT unimod = getUnimodByTerm(term, unimodParser);
        Modification mod = null;

        if (unimod != null) {
            mod = unimodParser.createModification(unimod, pos, residues);
        }

        return mod;
    }



    /**
     * Creates a UniMod equivalent set of {@link SearchModification}s for the given PSI-MOD term
     *
     * @param term
     * @param unimodParser
     * @return
     */
    public List<SearchModification> getUnimodEquivalentSearchModifications(Term term, UnimodParser unimodParser) {
        String[] accAndRes = getUnimodAccessionAndResidue(term);
        String residue = accAndRes[1];

        ModT unimod = getUnimodByTerm(term, unimodParser);
        List<SearchModification> mods = Collections.emptyList();

        if (unimod != null) {

            Set<String> residues = new HashSet<>();
            if (residue != null) {
                // set the residue given by PSI-MOD
                residues.add(residue);
            } else {
                // set all allowed residues of unimod
                unimod.getSpecificity().stream().forEach(spec -> residues.add(spec.getSite()));
            }

            if (!residues.isEmpty()) {
                mods = createSearchModificationsForResidues(unimod, residues);
            } else {
                LOGGER.error("No valid sites but a modification found for " + term);
            }
        } else {
            // TODO: implement creation of "unknown modification" with mass shift and residue
            LOGGER.error("Unknown modification not yet supported");
        }

        return mods;
    }



    /**
     * Creates {@link SearchModification}s of the given UniMod element for the selected residues.
     *
     * @param unimod
     * @param residues
     * @return
     */
    public List<SearchModification> createSearchModificationsForResidues(ModT unimod, Collection<String> residues) {
        List<SearchModification> mods = new ArrayList<>();

        for (SpecificityT spec : unimod.getSpecificity()) {
            if (residues.contains(spec.getSite())) {
                SearchModification mod = new SearchModification();

                mod.getCvParam().add(MzIdentMLTools.createCvParam(
                        UnimodParser.getCv().getId() + ":" + unimod.getRecordId(),
                        UnimodParser.getCv(),
                        unimod.getTitle(),
                        null));

                mod.setMassDelta(unimod.getDelta().getMonoMass().floatValue());

                if (!spec.getPosition().equals(PositionT.ANYWHERE)) {
                    // TODO: implement the specificity!!
                    SpecificityRules specRules = new SpecificityRules();
                    LOGGER.warn("specificity of modifications not yet implemented!! (for " + unimod.getFullName() + ")");
                } else {
                    mod.getResidues().add(spec.getSite());
                }

                mods.add(mod);
            }
        }

        return mods;
    }

}
