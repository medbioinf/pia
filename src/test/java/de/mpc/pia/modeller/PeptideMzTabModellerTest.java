package de.mpc.pia.modeller;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.PIASimpleCompiler;
import de.mpc.pia.intermediate.compiler.parser.InputFileParserFactory;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author ypriverol (ypriverol@gmail.com) on 19/04/2017.
 */
public class PeptideMzTabModellerTest {

    private static File mzTabFile;
    private PIAModeller piaModeller;
    private PIACompiler piaCompiler;

    @Before
    public void setUp() throws URISyntaxException, IOException {

        URI uri = (PeptideMzTabModellerTest.class.getClassLoader().getResource("control_exo_rep1_high_mol_weight.dat-pride.mztab")) != null ? (PeptideMzTabModellerTest.class.getClassLoader().getResource("control_exo_rep1_high_mol_weight.dat-pride.mztab")).toURI(): null;

        mzTabFile = new File(uri);

        piaCompiler = new PIASimpleCompiler();
        piaCompiler.getDataFromFile(mzTabFile.getName(), mzTabFile.getAbsolutePath(),null, InputFileParserFactory.InputFileTypes.MZTAB_INPUT.getFileTypeShort());

        piaCompiler.buildClusterList();
        piaCompiler.buildIntermediateStructure();

        File tempFile = File.createTempFile(mzTabFile.getName(), ".tmp");
        piaCompiler.writeOutXML(tempFile);
        piaCompiler.finish();
        piaModeller = new PIAModeller(tempFile.getAbsolutePath());
    }

    @Test
    public void testDecoysAndFDR() {
        // set everything on PSM level (but don't calculate FDR now)
        piaModeller.setCreatePSMSets(true);

        piaModeller.getPSMModeller().setAllDecoyPattern("searchengine");
        piaModeller.getPSMModeller().setAllTopIdentifications(0);

        // calculate peptide FDR
        Long fileID = 1L;
        piaModeller.getPeptideModeller().calculateFDR(fileID);

        FDRData fdrData = piaModeller.getPeptideModeller().getFilesFDRData(fileID);

        assertEquals(DecoyStrategy.SEARCHENGINE, fdrData.getDecoyStrategy());
        assertEquals("mascot_score", fdrData.getScoreShortName());
        assertEquals(2, fdrData.getNrDecoys().intValue());
        assertEquals(1260, fdrData.getNrItems().intValue());
        assertEquals(1258, fdrData.getNrTargets().intValue());



        // calculate FDR on PSM level
        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        // calculate peptide FDR and filter
        piaModeller.getPeptideModeller().addFilter(0L, new PSMScoreFilter(FilterComparator.less_equal, false, 0.01, ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName()));

        fileID = 1L;
        piaModeller.getPeptideModeller().calculateFDR(fileID);

        fdrData = piaModeller.getPeptideModeller().getFilesFDRData(fileID);

        assertEquals("psm_fdr_score", fdrData.getScoreShortName());
        assertEquals(2, fdrData.getNrDecoys().intValue());
        assertEquals(1260, fdrData.getNrItems().intValue());
        assertEquals(1258, fdrData.getNrTargets().intValue());
    }
}
