package de.mpc.pia.tools.obo;

import static org.junit.Assert.*;

import org.biojava.nbio.ontology.Term;
import org.junit.Test;

import de.mpc.pia.tools.unimod.UnimodParser;


public class PsiModParserTest {

    @Test
    public void testMapperOffline() {
        PsiModParser oboMapper = new PsiModParser(false);

        // first, test the offline reader
        testParser(oboMapper);
    }


    @Test
    public void testMapperOnline() {
        PsiModParser oboMapper = new PsiModParser(true);
        // test the choosing mapper
        testParser(oboMapper);
    }


    private void testParser(PsiModParser oboMapper) {
        Term deamido = oboMapper.getTerm("MOD:00400");
        assertNotNull("MOD:00400 should be found by ID in the obo", deamido);

        assertNotNull("MOD:01090 should be found by name in the obo",
                oboMapper.getTermByName("iodoacetamide derivatized amino-terminal residue"));

        assertNotNull("MOD:00425 should be found by ID in the obo", oboMapper.getTerm("MOD:00425"));

        Term hydroxyD = oboMapper.getTerm("MOD:00036");
        assertNotNull("MOD:00036 should be found by ID in the obo", hydroxyD);

        Term hydroxyDvaline = oboMapper.getTerm("MOD:00756");
        assertNotNull("MOD:00756 should be found by ID in the obo", hydroxyDvaline);


        UnimodParser unimodParser = new UnimodParser(false);
        assertNotNull("could not find the unimod equivalent for deamidation",
                oboMapper.getUnimodEquivalent(deamido, unimodParser));

        assertNotNull("could not find the unimod equivalent cvTerm for (2S,3R)-3-hydroxyaspartic acid [MOD:00036]",
                oboMapper.getUnimodEquivalentSearchModifications(hydroxyD, unimodParser));

        Term modif = oboMapper.getTerm("MOD:00075");
        assertNotNull("could not find the unimod equivalent cvTerm for N,N-dimethyl-L-proline [MOD:00075]",
                oboMapper.getUnimodEquivalentSearchModifications(modif, unimodParser));

        // TODO: check for residues and specifications
    }

}