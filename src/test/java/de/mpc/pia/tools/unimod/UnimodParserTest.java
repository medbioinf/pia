package de.mpc.pia.tools.unimod;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;


public class UnimodParserTest {


    @Test
    public void testUnimodOffline() {
        UnimodParser unimodParser = new UnimodParser(false);
        // first, test the offline reader
        testParser(unimodParser);
    }


    @Test
    public void testUnimodOnline() {
        UnimodParser unimodParser = new UnimodParser(true);
        // test the choosing mapper
        testParser(unimodParser);
    }


    private void testParser(UnimodParser unimodParser) {
        assertNotNull(unimodParser.getModificationByName("Carbamidomethyl", Collections.singletonList("C")));
        assertNotNull(unimodParser.getModificationByName("Oxidation", Collections.singletonList("M")));
        assertNull(unimodParser.getModificationByName("sure_not_there", Arrays.asList("K", "F", "C")));
    }

}
