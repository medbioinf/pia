package de.mpc.pia.intermediate;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpc.pia.intermediate.xmlhandler.PIAIntermediateJAXBHandler;
import de.mpc.pia.modeller.PIAModellerTest;

public class IntermediateJAXBTest {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(IntermediateJAXBTest.class);

    public static File piaFile;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        piaFile = new File(PIAModellerTest.class.getResource("/55merge_mascot_tandem.pia.xml").getPath());
    }


    @Test
    public void testIntermediateJAXB() {
        PIAIntermediateJAXBHandler intermediateHandler;
        intermediateHandler = new PIAIntermediateJAXBHandler();

        Runtime runtime = Runtime.getRuntime();
        double mb = 1024*1024;
        final long startTime = System.nanoTime();
        final long endTime;

        try {
            intermediateHandler.parse(piaFile.getAbsolutePath(), null);
        } catch (IOException e) {
            LOGGER.error(e);
        }

        endTime = System.nanoTime();

        assertEquals(2, intermediateHandler.getFiles().size());
        assertEquals(2, intermediateHandler.getSpectraData().size());
        assertEquals(2, intermediateHandler.getSearchDatabase().size());
        assertEquals(2, intermediateHandler.getAnalysisSoftware().size());
        assertEquals(1941, intermediateHandler.getGroups().size());
        assertEquals(2131, intermediateHandler.getAccessions().size());
        assertEquals(2113, intermediateHandler.getPeptides().size());
        assertEquals(2478, intermediateHandler.getPSMs().size());
        assertEquals(1856, intermediateHandler.getNrTrees());

        LOGGER.info("Total Memory: " + runtime.totalMemory() / mb + " MB");
        LOGGER.info("Used Memory: " + (runtime.totalMemory() - runtime.freeMemory()) / mb + " MB");
        LOGGER.info("Free Memory: " + runtime.freeMemory() / mb + " MB");
        LOGGER.info("Max Memory: " + runtime.maxMemory() / mb + " MB");
        LOGGER.info("Execution time: " + ((endTime - startTime) / 1000000000.0) + " s");
    }
}
