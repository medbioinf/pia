package de.mpc.pia.modeller.exporter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.PIASimpleCompiler;


public class MzIdentML12Test {

    @Test
    @Ignore("Unfortunately, loading Unmarshaller for mzid 1.1 and mzid 1.2 in same test suite breaks everything")
    public void testMzIdentMLv1_2_0Import() {
        PIACompiler piaCompiler = new PIASimpleCompiler();

        File cometMzid12Results = new File(MzIdentML12Test.class.getResource("/comet_mzid12.mzid").getPath());
        assertTrue(piaCompiler.getDataFromFile("mzid", cometMzid12Results.getAbsolutePath(), null, null));

        piaCompiler.buildClusterList();
        piaCompiler.buildIntermediateStructure();

        piaCompiler.setName("testFile");

        assertEquals("Wrong number of PIA Input files", 1, piaCompiler.getAllFileIDs().size());
        assertEquals("Wrong number of imported peptides", 600, piaCompiler.getNrPeptides());
        assertEquals("Wrong number of imported PSMs", 610, piaCompiler.getNrPeptideSpectrumMatches());
    }
}
