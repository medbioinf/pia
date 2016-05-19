package de.mpc.pia.intermediate.compiler;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

public class PIACompilerTest {

    //public static File mascotFile;
    //public static File tandemFile;

    public static File mzid55mergeTandem;
    public static File mzid55mergeOmssa;


    private String piaIntermediateFileName = "PIACompilerTest.pia.xml";


    @BeforeClass
    public static void initialize() {
        //mascotFile = new File(PIACompilerTest.class.getResource("/07-12_MW_58-F008265.dat").getPath());
        //tandemFile = new File(PIACompilerTest.class.getResource("/07-12_MW_58.tandem.xml").getPath());

        mzid55mergeTandem = new File(PIACompilerTest.class.getResource("/55merge_tandem.mzid").getPath());
        mzid55mergeOmssa = new File(PIACompilerTest.class.getResource("/55merge_omssa.mzid").getPath());
    }


    /*
    @Test
    public void testPIACompilerNativeFiles() throws IOException {
        PIACompiler piaCompiler = new PIACompiler();

        assertEquals("Mascot file could not be parsed", true,
                piaCompiler.getDataFromFile("mascot", mascotFile.getAbsolutePath(), null, null));

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
    }
    */

    @Test
    public void testPIACompilerMzidFiles() throws IOException {
        PIACompiler piaCompiler = new PIACompiler();

        assertEquals("X!TAndem file could not be parsed", true,
                piaCompiler.getDataFromFile("tandem", mzid55mergeTandem.getAbsolutePath(), null, null));

        assertEquals("OMSSA file could not be parsed", true,
                piaCompiler.getDataFromFile("tandem", mzid55mergeOmssa.getAbsolutePath(), null, null));

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
    }
}
