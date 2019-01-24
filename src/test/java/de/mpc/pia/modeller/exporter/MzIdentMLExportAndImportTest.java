package de.mpc.pia.modeller.exporter;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.PIACompilerTest;
import de.mpc.pia.intermediate.compiler.PIASimpleCompiler;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.protein.inference.SpectrumExtractorInference;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.protein.scoring.MultiplicativeScoring;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.score.ScoreModelEnum;


public class MzIdentMLExportAndImportTest {

    private File tandemIdXMLResults;
    private String piaIntermediateFileName;

    @Before
    public void setUp() {
        piaIntermediateFileName = "PIACompilerTest.pia.xml";

        tandemIdXMLResults = new File(PIACompilerTest.class.getResource("/merge1-tandem-fdr_filtered-015.idXML").getPath());
    }


    @Test
    public void testMzIdentMLExportAndImport() throws IOException {
        PIACompiler piaCompiler = new PIASimpleCompiler();

        assertTrue(piaCompiler.getDataFromFile("tandem", tandemIdXMLResults.getAbsolutePath(), null, null));

        piaCompiler.buildClusterList();
        piaCompiler.buildIntermediateStructure();

        piaCompiler.setName("testFile");


        // write out the file
        File piaIntermediateFile = File.createTempFile(piaIntermediateFileName, null);
        piaCompiler.writeOutXML(piaIntermediateFile);
        piaCompiler.finish();


        // read in PIA XML file and perform analysis
        PIAModeller piaModeller = new PIAModeller(piaIntermediateFile.getAbsolutePath());

        piaModeller.getPSMModeller().setAllDecoyPattern("Rnd.*");
        piaModeller.getPSMModeller().setAllTopIdentifications(1);

        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        // protein level
        SpectrumExtractorInference seInference = new SpectrumExtractorInference();


        seInference.addFilter(
                new PSMScoreFilter(FilterComparator.less_equal, false, 0.5, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));


        seInference.setScoring(new MultiplicativeScoring(new HashMap<>()));
        seInference.getScoring().setSetting(AbstractScoring.SCORING_SETTING_ID, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName());
        seInference.getScoring().setSetting(AbstractScoring.SCORING_SPECTRA_SETTING_ID, PSMForScoring.ONLY_BEST.getShortName());

        piaModeller.getProteinModeller().infereProteins(seInference);


        // simple exporting to mzIdentML
        MzIdentMLExporter exporter = new MzIdentMLExporter(piaModeller);
        File exportFile = File.createTempFile("pia_testMzIdentML", ".mzid");

        assertTrue(exporter.exportToMzIdentML(0L, exportFile, true, false));


        // try to read it back in PIA compiler
        piaCompiler = new PIASimpleCompiler();

        assertTrue(piaCompiler.getDataFromFile("mzIdentMLfile", exportFile.getAbsolutePath(), null, null));

        piaCompiler.buildClusterList();
        piaCompiler.buildIntermediateStructure();

        piaCompiler.setName("testFileBackIn");

        // write out the file
        piaIntermediateFile = File.createTempFile(piaIntermediateFileName, null);
        piaCompiler.writeOutXML(piaIntermediateFile);
        piaCompiler.finish();
    }
}
