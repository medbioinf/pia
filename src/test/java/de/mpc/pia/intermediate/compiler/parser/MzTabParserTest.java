package de.mpc.pia.intermediate.compiler.parser;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.inference.SpectrumExtractorInference;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.protein.scoring.MultiplicativeScoring;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.apache.log4j.Logger;
import uk.ac.ebi.pride.jmztab.utils.MZTabFileParser;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Yasset Perez-Riverol (ypriverol@gmail.com)
 * @date 15/02/2016
 */
public class MzTabParserTest {

    /** logger for this class */
    private static final Logger logger = Logger.getLogger(MzTabParserTest.class);

    PIACompiler compiler;

    File mzTabFile;

    MZTabFileParser reader;

    String piaIntermediateFileName = "PiaIntermediateFile.xml";


    @Before
    public void setUp() throws Exception {

        compiler = new PIACompiler();

        java.net.URI uri = PrideXMLParserTest.class.getClassLoader().getResource("PXD000764_34937_combined_fdr.mztab").toURI();

        mzTabFile = new File(uri);

        reader = new MZTabFileParser(new File(uri), System.out);

    }

    @Test
    public void testGetDataFromMzTabLFile() throws Exception {

        compiler.getDataFromFile(mzTabFile.getName(), mzTabFile.getAbsolutePath(), null,InputFileParserFactory.InputFileTypes.MZTAB_INPUT.getFileTypeShort());

        compiler.buildClusterList();

        compiler.buildIntermediateStructure();

        File piaIntermediateFile = File.createTempFile(piaIntermediateFileName, null);

        compiler.writeOutXML(piaIntermediateFile);

        PIAModeller piaModeller = new PIAModeller(piaIntermediateFile.getAbsolutePath());

        // PSM level
        piaModeller.setCreatePSMSets(true);
        assertEquals("createPSMSets should be true", true, piaModeller.getCreatePSMSets());

        piaModeller.getPSMModeller().setAllDecoyPattern(FDRData.DecoyStrategy.SEARCHENGINE.toString());

        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        SpectrumExtractorInference seInference = new SpectrumExtractorInference();

        seInference.addFilter(new PSMScoreFilter(FilterComparator.less_equal, false, 0.01, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));

        seInference.setScoring(new MultiplicativeScoring(new HashMap<String, String>()));
        seInference.getScoring().setSetting(AbstractScoring.scoringSettingID, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName());
        seInference.getScoring().setSetting(AbstractScoring.scoringSpectraSettingID, PSMForScoring.ONLY_BEST.getShortName());

        piaModeller.getProteinModeller().infereProteins(seInference);

        List<ReportProtein> proteins = piaModeller.getProteinModeller().getFilteredReportProteins(null);

        Assert.assertTrue(reader.getMZTabFile().getProteins().size() - 1 == proteins.size());

    }

    @After
    public void tearDown() throws Exception {


    }
}