package de.mpc.pia.intermediate.compiler.parser;

import java.io.File;
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

public class CometTSVParserTest {

    private String piaIntermediateFileName = "CometCSVParserTest.pia.xml";
    private File cometCSVTestFile;
    
    
    @Before
    public void setUp() {
    	cometCSVTestFile = new File(CometTSVParserTest.class.getResource("/comet-tsv-cut.txt").getPath());
    }


    @Test
    public void testCometCSVFile() throws IOException {
        PIACompiler piaCompiler = new PIASimpleCompiler();

        assertEquals("Comet CSV file could not be parsed", true,
                piaCompiler.getDataFromFile("comet", cometCSVTestFile.getAbsolutePath(), null, null));

        piaCompiler.buildClusterList();
        piaCompiler.buildIntermediateStructure();

        piaCompiler.setName("testFile");
        
        File piaIntermediateFile = File.createTempFile(piaIntermediateFileName, null);
        
        // test writing using the file object
        piaCompiler.writeOutXML(piaIntermediateFile);

        String filePath = piaIntermediateFile.getAbsolutePath();

        PIAModeller piaModeller = new PIAModeller(filePath);
        piaModeller.setCreatePSMSets(true);
        piaModeller.getPSMModeller().setAllDecoyPattern("DECOY_.*");
        piaModeller.getPSMModeller().setAllTopIdentifications(1);

        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        piaModeller.getPSMModeller().addFilter(0L,
                new PSMScoreFilter(FilterComparator.less_equal, false, 0.5, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));
        
        assertEquals("number of filtered PSM sets is wrong", 5079, piaModeller.getPSMModeller().getFilteredReportPSMs(1L, null).size());
        
        assertEquals("number of filtered PSM sets is wrong", 1042,
        		piaModeller.getPSMModeller().getFilteredReportPSMSets(piaModeller.getPSMModeller().getFilters(0L)).size());

        piaIntermediateFile.delete();
    }
}
