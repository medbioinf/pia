package de.mpc.pia.intermediate.compiler.parser;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.PIASimpleCompiler;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.pride.jaxb.xml.PrideXmlReader;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * @author Yasset Perez-Riverol (ypriverol@gmail.com)
 */
public class PrideXMLParserTest {

    /** logger for this class */
    private static final Logger logger = Logger.getLogger(PrideXMLParser.class);

    private File prideXMLFile;

    private PrideXmlReader reader;

    private final String piaIntermediateFileName = "PrideParser.pia.xml";

    @Before
    public void setUp() throws Exception {
        URI uri = (PrideXMLParserTest.class.getClassLoader().getResource("PRIDE_Example.xml")) != null ? (PrideXMLParserTest.class.getClassLoader().getResource("PRIDE_Example.xml")).toURI(): null;

        if(uri == null)
            throw new IOException("File not found");

        prideXMLFile = new File(uri);
        reader = new PrideXmlReader(uri.toURL());


    }

    @Test
    public void getDataFromPrideXMLFileTest() throws IOException {
        PIACompiler compiler = new PIASimpleCompiler();

        compiler.getDataFromFile(prideXMLFile.getName(),
                prideXMLFile.getAbsolutePath(),
                null,
                InputFileParserFactory.InputFileTypes.PRIDEXML_INPUT.getFileTypeShort());

        compiler.buildClusterList();

        compiler.buildIntermediateStructure();

        File piaIntermediateFile = File.createTempFile(piaIntermediateFileName, null);

        compiler.writeOutXML(piaIntermediateFile);
        compiler.finish();

        /*
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

        Assert.assertTrue(reader.getIdentIds().size() - 1 == proteins.size());
        */
    }

}