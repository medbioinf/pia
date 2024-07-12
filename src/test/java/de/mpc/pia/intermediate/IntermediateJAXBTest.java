package de.mpc.pia.intermediate;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import de.mpc.pia.intermediate.xmlhandler.PIAIntermediateJAXBHandler;

public class IntermediateJAXBTest {

    /** logger for this class */
    private static final Logger LOGGER = LogManager.getLogger();

    @Test
    public void testIntermediateJAXB() throws IOException{
        PIAIntermediateJAXBHandler intermediateHandler;
        intermediateHandler = new PIAIntermediateJAXBHandler();

        File piaFile = new File(IntermediateJAXBTest.class.getResource("/55merge_mascot_tandem.pia.xml").getPath());
        intermediateHandler.parse(piaFile.getAbsolutePath(), null);

        assertEquals("Number of Files differ", 2, intermediateHandler.getFiles().size());
        assertEquals("Number of SpectraData differ", 2, intermediateHandler.getSpectraData().size());
        assertEquals("Number of SearchDatabases differ", 2, intermediateHandler.getSearchDatabase().size());
        assertEquals("Number of AnalysisSoftware differ", 2, intermediateHandler.getAnalysisSoftware().size());
        assertEquals("Number of Groups differ", 1941, intermediateHandler.getGroups().size());
        assertEquals("Number of Accessions differ", 2131, intermediateHandler.getAccessions().size());
        assertEquals("Number of Peptides differ", 2113, intermediateHandler.getPeptides().size());
        assertEquals("Number of PSMs differ", 2478, intermediateHandler.getPSMs().size());
        assertEquals("Number of NrTrees differ", 1856, intermediateHandler.getNrTrees());
    }
}
