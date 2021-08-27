package de.mpc.pia.modeller;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.PIASimpleCompiler;
import de.mpc.pia.modeller.exporter.IdXMLExporter;
import de.mpc.pia.modeller.protein.inference.SpectrumExtractorInference;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.protein.scoring.MultiplicativeScoring;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;
import de.mpc.pia.modeller.score.ScoreModelEnum;


public class IdXMLTest {

    private File openMS2_5_0_idXML_1;
    private File openMS2_5_0_idXML_2;
    private File openMS2_5_0_idXML_3;
    private String piaIntermediateFileName = "IdXMLTest.pia.xml";
    

    @Before
    public void setUp() {
        openMS2_5_0_idXML_1 = new File(PIAModellerTest.class.getResource("/test-datasets/lfq_spikein_dilution_1-openms250.idXML").getPath());
        openMS2_5_0_idXML_2 = new File(PIAModellerTest.class.getResource("/test-datasets/lfq_spikein_dilution_2-openms250.idXML").getPath());
        openMS2_5_0_idXML_3 = new File(PIAModellerTest.class.getResource("/test-datasets/lfq_spikein_dilution_3-openms250.idXML").getPath());
    }

    @Test
    public void testIdXMLOpenMS2_5_0() throws IOException {
        PIACompiler piaCompiler = new PIASimpleCompiler();

        assertTrue(piaCompiler.getDataFromFile("openms", openMS2_5_0_idXML_1.getAbsolutePath(), null, null));
        assertTrue(piaCompiler.getDataFromFile("openms", openMS2_5_0_idXML_2.getAbsolutePath(), null, null));
        assertTrue(piaCompiler.getDataFromFile("openms", openMS2_5_0_idXML_3.getAbsolutePath(), null, null));

        piaCompiler.buildClusterList();
        piaCompiler.buildIntermediateStructure();

        piaCompiler.setName("testFile");

        // write out the file
        File piaIntermediateFile = File.createTempFile(piaIntermediateFileName, null);
        piaCompiler.writeOutXML(piaIntermediateFile);
        piaCompiler.finish();
    	
        
        // open the PIA XML file
        PIAModeller piaModeller = new PIAModeller(piaIntermediateFile.getAbsolutePath());

        piaModeller.setCreatePSMSets(true);
        
        String decoyPattern = "DECOY_.*";
        piaModeller.getPSMModeller().setAllDecoyPattern(decoyPattern);
        piaModeller.getPSMModeller().setAllTopIdentifications(1);

        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        // protein level
        SpectrumExtractorInference seInference = new SpectrumExtractorInference();

        seInference.addFilter(
                new PSMScoreFilter(FilterComparator.less_equal, false, 0.01, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));

        seInference.setScoring(new MultiplicativeScoring(new HashMap<>()));
        seInference.getScoring().setSetting(AbstractScoring.SCORING_SETTING_ID, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName());
        seInference.getScoring().setSetting(AbstractScoring.SCORING_SPECTRA_SETTING_ID, PSMForScoring.ONLY_BEST.getShortName());

        piaModeller.getProteinModeller().infereProteins(seInference);
        
        // protein level FDR
        piaModeller.getProteinModeller().updateFDRData(DecoyStrategy.ACCESSIONPATTERN,
                decoyPattern, 0.01);
        piaModeller.getProteinModeller().updateDecoyStates();
        piaModeller.getProteinModeller().calculateFDR();
        
        // simple exporting
        IdXMLExporter exporter = new IdXMLExporter(piaModeller);
        File exportFile = File.createTempFile("pia_idXmlExportTest", ".idXML");

        assertTrue(exporter.exportToIdXML(0L, exportFile, true, true));

        exportFile.delete();
        piaIntermediateFile.delete();
    }
}
