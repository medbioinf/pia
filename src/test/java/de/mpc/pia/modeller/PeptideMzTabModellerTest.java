package de.mpc.pia.modeller;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.PIASimpleCompiler;
import de.mpc.pia.intermediate.compiler.parser.InputFileParserFactory;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

/**
 * This code is licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * ==Overview==
 * <p>
 * This class
 * <p>
 * Created by ypriverol (ypriverol@gmail.com) on 19/04/2017.
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
        // set everything on PSM level (but not calculating the FDR now)
        piaModeller.setCreatePSMSets(true);

        piaModeller.getPSMModeller().setAllInheritedDecoyStrategies();
        //piaModeller.getPSMModeller().setAllTopIdentifications(1);

        // calculate peptide FDR
        Long fileID = 1L;
        piaModeller.getPeptideModeller().calculateFDR(fileID);

        FDRData fdrData = piaModeller.getPeptideModeller().getFilesFDRData(fileID);

        assertEquals("Rnd.*", fdrData.getDecoyPattern());
        assertEquals("mascot_score", fdrData.getScoreShortName());
        assertEquals(1480, fdrData.getNrDecoys().intValue());
        assertEquals(2004, fdrData.getNrItems().intValue());
        assertEquals(524, fdrData.getNrTargets().intValue());

        // calculate FDR on PSM level
        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        // calculate peptide FDR
        piaModeller.getPeptideModeller().addFilter(0L, new PSMScoreFilter(FilterComparator.less_equal, false, 0.5, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));

        fileID = 0L;
        piaModeller.getPeptideModeller().calculateFDR(fileID);

        fdrData = piaModeller.getPeptideModeller().getFilesFDRData(fileID);

        assertEquals("psm_combined_fdr_score", fdrData.getScoreShortName());
        assertEquals(3, fdrData.getNrDecoys().intValue());
        assertEquals(8, fdrData.getNrItems().intValue());
        assertEquals(5, fdrData.getNrTargets().intValue());
    }
}
