package de.mpc.pia.modeller.report.filter;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.mpc.pia.modeller.psm.ReportPSMSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.PIASimpleCompiler;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.score.ScoreModelEnum;

import static org.junit.Assert.*;


public class SearchEngineFilterTest {

    private File idXMLtandemFile;
    private File idXMLmsgfFile;

    private String piaIntermediateFileName = "SearchEngineFilterTest.pia.xml";


    @Before
    public void setUp() {
        idXMLtandemFile = new File(SearchEngineFilterTest.class.getResource("/merge1-tandem-fdr_filtered-015.idXML").getPath());
        idXMLmsgfFile = new File(SearchEngineFilterTest.class.getResource("/merge1-msgf-fdr_filtered-015.idXML").getPath());
    }


    @Test
    public void testPIASearchEngineFilterAndAnalysis() throws IOException {
        PIACompiler piaCompiler = new PIASimpleCompiler();

        assertTrue(piaCompiler.getDataFromFile("tandem", idXMLtandemFile.getAbsolutePath(), null, null));
        assertTrue(piaCompiler.getDataFromFile("msgf", idXMLmsgfFile.getAbsolutePath(), null, null));

        piaCompiler.buildClusterList();
        piaCompiler.buildIntermediateStructure();

        piaCompiler.setName("testFile");


        // write out the file
        File piaIntermediateFile = File.createTempFile(piaIntermediateFileName, null);
        piaCompiler.writeOutXML(piaIntermediateFile);
        piaCompiler.finish();


        // now make an analysis
        PIAModeller piaModeller = new PIAModeller(piaIntermediateFile.getAbsolutePath());

        piaModeller.setCreatePSMSets(true);

        piaModeller.getPSMModeller().setAllDecoyPattern("s.*");
        piaModeller.getPSMModeller().setAllTopIdentifications(0);

        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        piaModeller.setConsiderModifications(false);

        /* add all nonNativeScores to be needed */
        for (String scoreShort : piaModeller.getPSMModeller().getScoreShortsToScoreNames().keySet()) {
            if (!ScoreModelEnum.getNonNativeScoreModels().contains(ScoreModelEnum.getModelByDescription(scoreShort)) ) {
                piaModeller.getPSMModeller().addFilter(0L,
                        RegisteredFilters.PSM_SET_CONTAINS_SCORE_FILTER.newInstanceOf(FilterComparator.contains, scoreShort, false)
                );
            }
        }

        // also requires FDR scores (which were calculated before)
        piaModeller.getPSMModeller().addFilter(0L,
                RegisteredFilters.PSM_SET_CONTAINS_SCORE_FILTER.newInstanceOf(FilterComparator.contains, ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName(), false)
                );

        piaModeller.getPSMModeller().addFilter(0L,
                RegisteredFilters.PSM_SET_CONTAINS_SCORE_FILTER.newInstanceOf(FilterComparator.contains, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName(), false)
                );


        List<ReportPSMSet> psmSets = piaModeller.getPSMModeller().getFilteredReportPSMSets(
                piaModeller.getPSMModeller().getFilters(0L)
                );

        Assert.assertEquals(4510, psmSets.size());


        /*
        // protein level
        SpectrumExtractorInference seInference = new SpectrumExtractorInference();

        seInference.setScoring(new MultiplicativeScoring(new HashMap<>()));
        seInference.getScoring().setSetting(AbstractScoring.SCORING_SETTING_ID, ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName());
        seInference.getScoring().setSetting(AbstractScoring.SCORING_SPECTRA_SETTING_ID, PSMForScoring.ONLY_BEST.getShortName());

        piaModeller.getProteinModeller().infereProteins(seInference);

        piaModeller.getProteinModeller().updateFDRData(DecoyStrategy.ACCESSIONPATTERN, "s.*", 0.01);
        piaModeller.getProteinModeller().updateDecoyStates();
        piaModeller.getProteinModeller().calculateFDR();
        */

        piaIntermediateFile.delete();
    }

}
