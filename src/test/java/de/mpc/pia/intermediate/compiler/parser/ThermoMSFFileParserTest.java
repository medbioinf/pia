package de.mpc.pia.intermediate.compiler.parser;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.PIASimpleCompiler;
import de.mpc.pia.modeller.PIAModeller;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;

import static org.junit.Assert.*;

/**
 * @author julianu
 * @date 05/12/2017
 */
public class ThermoMSFFileParserTest {

    private File msfFile;
    private String piaIntermediateFileName = "PiaIntermediateFile.xml";


    @Before
    public void setUp() throws Exception {

        URI uri = ThermoMSFFileParserTest.class.getClassLoader().getResource("QExHF04458.msf").toURI();

        msfFile = new File(uri);
    }

    @Test
    public void testCreatePIAIntermediateFromMSFFile() throws Exception {
        PIACompiler compiler = new PIASimpleCompiler();

        boolean ok = compiler.getDataFromFile(msfFile.getName(), msfFile.getAbsolutePath(), null,
                InputFileParserFactory.InputFileTypes.THERMO_MSF_INPUT.getFileTypeShort());
        assertTrue("Could not get data from MSF file", ok);
        compiler.buildClusterList();
        compiler.buildIntermediateStructure();

        File piaIntermediateFile = File.createTempFile(piaIntermediateFileName, null);
        compiler.writeOutXML(piaIntermediateFile);
        compiler.finish();
        compiler = null;

        // try to read file
        PIAModeller piaModeller = new PIAModeller(piaIntermediateFile.getAbsolutePath());
        assertEquals("Wrong number of groups in file", 16, piaModeller.getGroups().size());
        piaModeller = null;

        piaIntermediateFile.delete();
    }
}