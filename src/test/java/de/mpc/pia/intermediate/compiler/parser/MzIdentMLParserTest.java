package de.mpc.pia.intermediate.compiler.parser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.PIASimpleCompiler;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.score.ScoreModelEnum;

public class MzIdentMLParserTest {

    private File mzid55mergeTandem;
    private File mzid55mergeOmssa;

    private String piaIntermediateFileName = "MzIdentMLParserTest.pia.xml";


    @Before
    public void setUp() {
        mzid55mergeTandem = new File(MzIdentMLParserTest.class.getResource("/55merge_tandem.mzid").getPath());
        mzid55mergeOmssa = new File(MzIdentMLParserTest.class.getResource("/55merge_omssa.mzid").getPath());
    }


    @Test
    public void testPIACompilerMzidFDRNative() throws IOException {
        PIACompiler piaCompiler = new PIASimpleCompiler();

        assertEquals("X!TAndem file could not be parsed", true,
                piaCompiler.getDataFromFile("tandem", mzid55mergeTandem.getAbsolutePath(), null, null));

        piaCompiler.buildClusterList();
        piaCompiler.buildIntermediateStructure();

        piaCompiler.setName("testFile");

        File piaIntermediateFile = File.createTempFile(piaIntermediateFileName, null);

        // test writing using the file's name
        piaCompiler.writeOutXML(piaIntermediateFile.getAbsolutePath());
        piaIntermediateFile.delete();

        // test writing using the file object
        piaCompiler.writeOutXML(piaIntermediateFile);

        String filePath = piaIntermediateFile.getAbsolutePath();

        PIAModeller piaModeller = new PIAModeller(filePath);
        piaModeller.setCreatePSMSets(true);
        piaModeller.getPSMModeller().setAllDecoyPattern("searchengine");
        piaModeller.getPSMModeller().setAllTopIdentifications(1);

        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        piaModeller.getPSMModeller().addFilter(0L,
                new PSMScoreFilter(FilterComparator.less_equal, false, 0.5, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));

        assertEquals("number of filtered PSM sets is wrong", 7,
                piaModeller.getPSMModeller().getFilteredReportPSMSets(piaModeller.getPSMModeller().getFilters(0L)).size());

        piaIntermediateFile.delete();

        // test writing using the file stream
        try (FileOutputStream fos = new FileOutputStream(piaIntermediateFile)) {
            piaCompiler.writeOutXML(fos);
            piaIntermediateFile.delete();
        }

        piaIntermediateFile.delete();
    }


    @Test
    public void testPIACompilerMzidFiles() throws IOException {
        PIACompiler piaCompiler = new PIASimpleCompiler();

        assertEquals("X!TAndem file could not be parsed", true,
                piaCompiler.getDataFromFile("tandem", mzid55mergeTandem.getAbsolutePath(), null, null));

        assertEquals("OMSSA file could not be parsed", true,
                piaCompiler.getDataFromFile("tandem", mzid55mergeOmssa.getAbsolutePath(), null, null));

        piaCompiler.buildClusterList();
        piaCompiler.buildIntermediateStructure();

        piaCompiler.setName("testFile");

        File piaIntermediateFile = File.createTempFile(piaIntermediateFileName, null);

        // test writing using the file's name
        piaCompiler.writeOutXML(piaIntermediateFile.getAbsolutePath());
        piaIntermediateFile.delete();

        // test writing using the file object
        piaCompiler.writeOutXML(piaIntermediateFile);
        piaIntermediateFile.delete();

        // test writing using the file stream
        try (FileOutputStream fos = new FileOutputStream(piaIntermediateFile)) {
            piaCompiler.writeOutXML(fos);
            piaIntermediateFile.delete();
        }

        piaIntermediateFile.delete();
    }
}
