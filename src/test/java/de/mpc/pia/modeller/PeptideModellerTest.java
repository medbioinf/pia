package de.mpc.pia.modeller;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpc.pia.modeller.exporter.IdXMLExporter;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.inference.SpectrumExtractorInference;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.protein.scoring.MultiplicativeScoring;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;
import de.mpc.pia.modeller.score.ScoreModelEnum;

public class PeptideModellerTest {

    private static File piaFile;

    private PIAModeller piaModeller;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        piaFile = new File(PIAModellerTest.class.getResource("/55merge_mascot_tandem.pia.xml").getPath());
    }

    @Before
    public void setUp() throws Exception {
        piaModeller = new PIAModeller(piaFile.getAbsolutePath());
    }

    @Test
    public void testDecoysAndFDR() {
        // set everything on PSM level (but not calculating the FDR now)
        piaModeller.setCreatePSMSets(true);

        piaModeller.getPSMModeller().setAllDecoyPattern("Rnd.*");
        piaModeller.getPSMModeller().setAllTopIdentifications(1);

        // calculate peptide FDR
        Long fileID = 1L;
        piaModeller.getPeptideModeller().calculateFDR(fileID);

        FDRData fdrData = piaModeller.getPeptideModeller().getFilesFDRData(fileID);
        System.err.println("decoyPattern " + fdrData.getDecoyPattern()
                + "\nscoreShort " + fdrData.getScoreShortName()
                + "\nartificialDecoyFDR " + fdrData.getArtificialDecoyFDR()
                + "\nfdrThreshold " + fdrData.getFDRThreshold()
                + "\nnrDecoys " + fdrData.getNrDecoys()
                + "\nfdrGoodDecoys " + fdrData.getNrFDRGoodDecoys()
                + "\nfdrGoodItems " + fdrData.getNrFDRGoodItems()
                + "\nfdrGoodTargets " + fdrData.getNrFDRGoodTargets()
                + "\nnrItems " + fdrData.getNrItems()
                + "\nnrTargets " + fdrData.getNrTargets()
                + "\nscoreAtThreshold " + fdrData.getScoreAtThreshold()
                + "\ndecoyStrategy " + fdrData.getDecoyStrategy()
                );

        assertEquals("Rnd.*", fdrData.getDecoyPattern());
        assertEquals("mascot_score", fdrData.getScoreShortName());
        assertEquals(1480, fdrData.getNrDecoys().intValue());
        assertEquals(2004, fdrData.getNrItems().intValue());
        assertEquals(524, fdrData.getNrTargets().intValue());

        // calculate FDR on PSM level
        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        // calculate peptide FDR
        piaModeller.getPeptideModeller().addFilter(0L,
                new PSMScoreFilter(FilterComparator.less_equal, false, 0.5, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));

        fileID = 0L;
        piaModeller.getPeptideModeller().calculateFDR(fileID);

        fdrData = piaModeller.getPeptideModeller().getFilesFDRData(fileID);

        assertEquals("psm_combined_fdr_score", fdrData.getScoreShortName());
        assertEquals(3, fdrData.getNrDecoys().intValue());
        assertEquals(8, fdrData.getNrItems().intValue());
        assertEquals(5, fdrData.getNrTargets().intValue());
    }

}
