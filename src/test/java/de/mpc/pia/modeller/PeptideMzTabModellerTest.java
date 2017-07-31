package de.mpc.pia.modeller;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.PIASimpleCompiler;
import de.mpc.pia.intermediate.compiler.parser.InputFileParserFactory;
import de.mpc.pia.modeller.exporter.MzTabExporter;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.report.filter.impl.PeptideScoreFilter;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


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

        File tempFile = File.createTempFile(mzTabFile.getName(), ".piaXML");
        piaCompiler.writeOutXML(tempFile);
        piaCompiler.finish();
        piaModeller = new PIAModeller(tempFile.getAbsolutePath());
    }

    @Test
    public void testDecoysAndFDR() throws IOException {
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

        fileID = 0L;
        piaModeller.getPeptideModeller().calculateFDR(fileID);

        fdrData = piaModeller.getPeptideModeller().getFilesFDRData(fileID);

        assertEquals("psm_combined_fdr_score", fdrData.getScoreShortName());
        assertEquals(2, fdrData.getNrDecoys().intValue());
        assertEquals(1260, fdrData.getNrItems().intValue());
        assertEquals(1258, fdrData.getNrTargets().intValue());

        piaModeller.getPeptideModeller().addFilter(0L,
                new PeptideScoreFilter(FilterComparator.less_equal, false, 0.01, ScoreModelEnum.PEPTIDE_LEVEL_Q_VALUE.getShortName())
        );

        List<ReportPeptide> peptides = piaModeller.getPeptideModeller().getFilteredReportPeptides(0L, piaModeller.getPeptideModeller().getFilters(0L));

        peptides.forEach(peptide -> System.out.println(peptide.getSequence() + " -> " + peptide.getQValue()));

        MzTabExporter exporter = new MzTabExporter(piaModeller);
        File exportFile = File.createTempFile("control_exo_rep1_high_mol_weight", "mzTab");
        assertTrue(exporter.exportToMzTab(fileID, exportFile, false, true, false, false));

        exportFile.delete();
    }
}
