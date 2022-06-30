package de.mpc.pia.modeller.score;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.PIASimpleCompiler;
import de.mpc.pia.intermediate.compiler.parser.InputFileParserFactory;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.report.filter.impl.PeptideScoreFilter;
import de.mpc.pia.tools.PIAConstants;

/**
 * some additional FDR tests, others are in other testsets (PSM, peptide and protein modeller tests)
 * @author julian
 *
 */
public class FDRScoreTest {

    /** the merge ID is always 0 (as long) */
    final static Long MERGE_FILE_ID = 0L;

    final static Double psmThreshold = 0.01;
    final static Double peptideThreshold = 0.01;


    private static File inferenceTempFile;


    @BeforeClass
    public static void setUp() throws IOException {
        File mzTabFile = new File(FDRScoreTest.class.getResource("/snip-fdrtest.mztab").getPath());

        PIACompiler piaCompiler = new PIASimpleCompiler();

        piaCompiler.getDataFromFile("mzTabFile", mzTabFile.getAbsolutePath(), null,
                InputFileParserFactory.InputFileTypes.MZTAB_INPUT.getFileTypeShort());

        piaCompiler.buildClusterList();
        piaCompiler.buildIntermediateStructure();

        inferenceTempFile = File.createTempFile(FDRScoreTest.class.getCanonicalName(), "-test.pia.xml");
        piaCompiler.writeOutXML(inferenceTempFile);
        piaCompiler.finish();
        if (inferenceTempFile.exists()) {
            inferenceTempFile.deleteOnExit();
        }
    }


    /**
     * some tests for the problem, when all PSMs have exactly the same score (regardless of being target or decoy)
     * @throws IOException
     */
    @Test
    public void testAllPSMsSameScoreFDRScore() {
        PIAModeller piaModeller = new PIAModeller(inferenceTempFile.getAbsolutePath());

        // FDR calculation
        piaModeller.setCreatePSMSets(false);
        piaModeller.getPSMModeller().setAllDecoyPattern("searchengine");
        piaModeller.getPSMModeller().setAllTopIdentifications(0);

        // calculate FDR on PSM level
        piaModeller.getPSMModeller().calculateAllFDR();

        // combined FDR Score is only useful when combining multiple search engines
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        assertEquals("There should be 3 detected decoys",
                Integer.valueOf(3), piaModeller.getPSMModeller().getFilesFDRData(1L).getNrDecoys());

        // the PSM level filter should be set before calculating the peptide level FDR
        piaModeller.getPeptideModeller().addFilter(MERGE_FILE_ID,
                new PSMScoreFilter(FilterComparator.less_equal, false, psmThreshold,
                        ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName()));        // no combined FDR was calculated

        piaModeller.getPeptideModeller().calculateFDR(MERGE_FILE_ID);

        assertEquals("There should be XXX detected decoys",
                Integer.valueOf(3), piaModeller.getPeptideModeller().getFilesFDRData(MERGE_FILE_ID).getNrDecoys());

        // setting filter for peptide level filtering
        List<AbstractFilter> filters = new ArrayList<>();
        filters.add(new PeptideScoreFilter(FilterComparator.less_equal, false, peptideThreshold,
                ScoreModelEnum.PEPTIDE_LEVEL_Q_VALUE.getShortName()));

        // get the FDR filtered peptides
        List<ReportPeptide> peptides = piaModeller.getPeptideModeller().getFilteredReportPeptides(MERGE_FILE_ID, filters);

        assertEquals("Wrong number of peptides in final list", 346, peptides.size());

        if (!peptides.isEmpty()) {
            Double sameFDRScore = peptides.get(0).getScore(ScoreModelEnum.PEPTIDE_LEVEL_Q_VALUE.getShortName());

            assertEquals("Wrong FDR Score for peptides", 0.008746355685131196, sameFDRScore, 0.0000001);

            peptides.forEach(pep -> assertEquals("All peptides should have the same FDR (which should also equal the q-value)",
                    sameFDRScore, pep.getScore(ScoreModelEnum.PEPTIDE_LEVEL_FDR_SCORE.getShortName())));
        }
    }


    @Test
    public void testFDRScoreWithoutDecoys() {
        PIAModeller piaModeller = new PIAModeller(inferenceTempFile.getAbsolutePath());

        piaModeller.setCreatePSMSets(true);

        // setting intentionally wrong decoy pattern
        piaModeller.getPSMModeller().setAllDecoyPattern("decoyIam_.*");
        piaModeller.getPSMModeller().setAllTopIdentifications(0);

        // calculate FDR on PSM level
        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();


        assertEquals("There should be no detected decoys",
                Integer.valueOf(0), piaModeller.getPSMModeller().getFilesFDRData(1L).getNrDecoys());

        piaModeller.getPSMModeller().getFilteredReportPSMs(1L, null).forEach(psm -> assertEquals("FDR Scores should be the small score substitute",
                PIAConstants.SMALL_FDRSCORE_SUBSTITUTE, psm.getFDRScore().getValue()));

        piaModeller.getPSMModeller().getFilteredReportPSMSets(null).forEach(psmSet -> assertEquals("FDR Scores should be the small score substitute",
                PIAConstants.SMALL_FDRSCORE_SUBSTITUTE, psmSet.getFDRScore().getValue()));
    }
}
