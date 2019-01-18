package de.mpc.pia.modeller;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.PIASimpleCompiler;
import de.mpc.pia.intermediate.compiler.parser.InputFileParserFactory;
import de.mpc.pia.modeller.exporter.MzTabExporter;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.report.filter.impl.PeptideScoreFilter;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This code is licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * ==Overview==
 *
 * @author ypriverol on 14/01/2019.
 */
public class FilteringTest{

    private PIAModeller piaModeller;
    private PIACompiler piaCompiler;

    @Before
    public void setUp() throws URISyntaxException, IOException {

        URI uri = (FilteringTest.class.getClassLoader().getResource("test-datasets/test-msgf.mzid")).toURI();
        File file1 = new File(uri);

        uri = (FilteringTest.class.getClassLoader().getResource("test-datasets/test-xtandem.xml")).toURI();
        File file2 = new File(uri);

        piaCompiler = new PIASimpleCompiler();

        piaCompiler.getDataFromFile(file1.getName(), file1.getAbsolutePath(),null, InputFileParserFactory.InputFileTypes.MZIDENTML_INPUT.getFileTypeShort());
        piaCompiler.getDataFromFile(file2.getName(), file2.getAbsolutePath(),null, InputFileParserFactory.InputFileTypes.TANDEM_INPUT.getFileTypeShort());

        piaCompiler.buildClusterList();
        piaCompiler.buildIntermediateStructure();

        File tempFile = File.createTempFile(file1.getName(), ".piaXML");
        piaCompiler.writeOutXML(tempFile);
        piaCompiler.finish();
        piaModeller = new PIAModeller(tempFile.getAbsolutePath());
    }

    @Test
    public void testDecoysAndFDR() throws IOException {
        // set everything on PSM level (but don't calculate FDR now)
        piaModeller.setCreatePSMSets(true);

        piaModeller.getPSMModeller().setAllDecoyPattern(".*_REVERSED.*");
        piaModeller.getPSMModeller().setAllTopIdentifications(0);

        // calculate peptide FDR
        Long fileID = 0L;
        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();


        FDRData fdrData = piaModeller.getPSMModeller().getFilesFDRData(fileID);

        assertEquals(FDRData.DecoyStrategy.ACCESSIONPATTERN, fdrData.getDecoyStrategy());
        assertEquals("average_fdr_score", fdrData.getScoreShortName());
        assertEquals(28, fdrData.getNrDecoys().intValue());
        assertEquals(71, fdrData.getNrItems().intValue());
        assertEquals(43, fdrData.getNrTargets().intValue());

        // calculate peptide FDR and filter
        piaModeller.getPSMModeller().addFilter(0L, new PSMScoreFilter(FilterComparator.less_equal, false, 0.1, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));

        List<ReportPSMSet> peptides = piaModeller.getPSMModeller().getFilteredReportPSMSets(
                piaModeller.getPSMModeller().getFilters(0L));


        MzTabExporter exporter = new MzTabExporter(piaModeller);
        File exportFile = File.createTempFile("control_exo_rep1_high_mol_weight", "mzTab");
        assertTrue(exporter.exportToMzTab(fileID, exportFile, false, true, false, false));

        exportFile.delete();
    }

}
