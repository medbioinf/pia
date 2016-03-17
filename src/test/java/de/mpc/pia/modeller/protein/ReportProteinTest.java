package de.mpc.pia.modeller.protein;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.inference.SpectrumExtractorInference;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.protein.scoring.MultiplicativeScoring;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;

public class ReportProteinTest {

    private File piaFile;
    private File expectedFile;
    private double scoreDelta = 0.000001;


    @Before
    public void setUp() throws URISyntaxException {
        piaFile = new File(ReportProteinTest.class.getClassLoader().getResource("yeast-gold-015-filtered.pia.xml").toURI());
        expectedFile = new File(ReportProteinTest.class.getClassLoader().getResource("yeast-gold-015-filtered-proteins.csv").toURI());
    }


    @After
    public void tearDown() throws Exception {


    }


    @Test
    public void testReportProteinValues() throws JAXBException, XMLStreamException, IOException, URISyntaxException {

        PIAModeller piaModeller = new PIAModeller(piaFile.getAbsolutePath());

        piaModeller.setCreatePSMSets(true);

        piaModeller.getPSMModeller().setAllDecoyPattern("s.*");
        piaModeller.getPSMModeller().setAllTopIdentifications(0);

        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        piaModeller.setConsiderModifications(false);

        // protein level
        SpectrumExtractorInference seInference = new SpectrumExtractorInference();

        seInference.addFilter(
                new PSMScoreFilter(FilterComparator.less_equal, false, 0.01, ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName()));

        seInference.setScoring(new MultiplicativeScoring(new HashMap<String, String>()));
        seInference.getScoring().setSetting(AbstractScoring.scoringSettingID, ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName());
        seInference.getScoring().setSetting(AbstractScoring.scoringSpectraSettingID, PSMForScoring.ONLY_BEST.getShortName());

        piaModeller.getProteinModeller().infereProteins(seInference);

        piaModeller.getProteinModeller().updateFDRData(DecoyStrategy.ACCESSIONPATTERN, "s.*", 0.01);
        piaModeller.getProteinModeller().updateDecoyStates();
        piaModeller.getProteinModeller().calculateFDR();


        List<AbstractFilter> filters = new ArrayList<AbstractFilter>();
        filters.add(RegisteredFilters.NR_GROUP_UNIQUE_PEPTIDES_PER_PROTEIN_FILTER.newInstanceOf(
                        FilterComparator.greater_equal, 2, false));


        // get the expected values
        Map<String, List<Object>> expectedValues = new HashMap<String, List<Object>>(426);

        BufferedReader br = new BufferedReader(new FileReader(expectedFile));
        String line;
        int nrLine = 0;
        while ((line = br.readLine()) != null) {
            if (nrLine++ < 1) {
                continue;
            }

            String split[] = line.split("\",\"");
            List<Object> entries = new ArrayList<Object>();

            entries.add(Double.parseDouble(split[1]));
            entries.add(Integer.parseInt(split[2]));
            entries.add(Integer.parseInt(split[3]));
            entries.add(Integer.parseInt(split[4]));
            entries.add(Boolean.parseBoolean(split[5]));
            entries.add(Double.parseDouble(split[6]));
            entries.add(Double.parseDouble(split[7].substring(0, split[7].length()-1)));

            expectedValues.put(split[0].substring(1), entries);
        }
        br.close();

        // compare the values
        for (ReportProtein prot : piaModeller.getProteinModeller().getFilteredReportProteins(filters)) {
            StringBuffer accSb = new StringBuffer();
            for (Accession acc : prot.getAccessions()) {
                accSb.append(acc.getAccession());
                accSb.append(",");
            }
            accSb.deleteCharAt(accSb.length()-1);

            List<Object> values = expectedValues.get(accSb.toString());
            assertNotNull("These accessions are not expected: " + accSb.toString(), values);

            assertEquals("Wrong score for " + accSb.toString(), (Double)(values.get(0)), prot.getScore(), scoreDelta);
            assertEquals("Wrong number of peptides for " + accSb.toString(), (Integer)(values.get(1)), prot.getNrPeptides());
            assertEquals("Wrong number of PSMs for " + accSb.toString(), (Integer)(values.get(2)), prot.getNrPSMs());
            assertEquals("Wrong number of spectra for " + accSb.toString(), (Integer)(values.get(3)), prot.getNrSpectra());
            assertEquals("Wrong decoy state for " + accSb.toString(), (Boolean)(values.get(4)), prot.getIsDecoy());
            assertEquals("Wrong FDR for " + accSb.toString(), (Double)(values.get(5)), prot.getFDR(), scoreDelta);
            assertEquals("Wrong q-value for " + accSb.toString(), (Double)(values.get(6)), prot.getQValue(), scoreDelta);
        }
    }
}
