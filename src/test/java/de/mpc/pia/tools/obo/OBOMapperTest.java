package de.mpc.pia.tools.obo;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.biojava.nbio.ontology.Term;
import org.junit.Test;


public class OBOMapperTest {

    @Test
    public void testMapperOffline() {
        OBOMapper oboMapper = new OBOMapper(false);
        // first, test the offline reader
        testMapper(oboMapper);
    }


    @Test
    public void testMapperOnline() {
        OBOMapper oboMapper = new OBOMapper(true);
        // test the choosing mapper
        testMapper(oboMapper);
    }


    private void testMapper(OBOMapper oboMapper) {
        boolean foundTrypsin = false;
        boolean foundMascotScore = false;
        boolean foundMSGF = false;

        Set<Term> keys = oboMapper.getTerms();
        for (Term term : keys) {
            if (term.getName().equals("MS:1001176")) {
                foundTrypsin = true;
            }

            if ((term.getDescription() != null) &&
                    (term.getDescription().equals("Mascot:score"))) {
                foundMascotScore = true;
            }

            if (term.getName().equals("MS:1002048")) {
                foundMSGF = true;
            }
        }

        assertEquals("trypsin (MS:1001176) should be found in the obo", true, foundTrypsin);
        assertEquals("Mascot:score should be found in the obo", true, foundMascotScore);
        assertEquals("MS/GF+ (MS:1002048) should be found in the obo", true, foundMSGF);
    }

}