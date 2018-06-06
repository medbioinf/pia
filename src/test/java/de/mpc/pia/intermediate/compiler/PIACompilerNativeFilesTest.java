package de.mpc.pia.intermediate.compiler;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class PIACompilerNativeFilesTest {

    public static File mascotFile;
    public static File tandemFile;

    private String piaIntermediateFileName = "PIACompilerNativeFilesTest.pia.xml";

    @Before
    public void setUp() {
        mascotFile = new File(PIACompilerNativeFilesTest.class.getResource("/Set1_A1.mascot.dat").getPath());
        tandemFile = new File(PIACompilerNativeFilesTest.class.getResource("/Set1_A1.tandem.xml").getPath());
    }


    @Test
    public void testPIACompilerNativeFiles() throws IOException {
        PIACompiler piaCompiler = new PIASimpleCompiler();

        assertEquals("Mascot file could not be parsed", true,
                piaCompiler.getDataFromFile("mascot", mascotFile.getAbsolutePath(), null, "mascot"));

        assertEquals("X!TAndem file could not be parsed", true,
                piaCompiler.getDataFromFile("tandem", tandemFile.getAbsolutePath(), null, "tandem"));

        piaCompiler.buildClusterList();
        piaCompiler.buildIntermediateStructure();

        piaCompiler.setName("testFile");

        File piaIntermediateFile = File.createTempFile(piaIntermediateFileName, null);

        // test writing using the file
        piaCompiler.writeOutXML(piaIntermediateFile);
        piaIntermediateFile.delete();

        // test writing using the file's name
        piaCompiler.writeOutXML(piaIntermediateFile.getAbsolutePath());
        piaIntermediateFile.delete();

        piaCompiler.finish();
    }

}
