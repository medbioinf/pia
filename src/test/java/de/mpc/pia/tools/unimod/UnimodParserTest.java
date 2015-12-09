package de.mpc.pia.tools.unimod;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class UnimodParserTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }


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


    public void testParser(UnimodParser unimodParser) {
        assertNotNull(unimodParser.getModificationByName("Carbamidomethyl", Arrays.asList("C")));
        assertNotNull(unimodParser.getModificationByName("Oxidation", Arrays.asList("M")));
        assertNull(unimodParser.getModificationByName("sure_not_there", Arrays.asList("K", "F", "C")));
    }

}
