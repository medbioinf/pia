package de.mpc.pia.modeller.exporter;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.protein.inference.SpectrumExtractorInference;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.protein.scoring.MultiplicativeScoring;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;


public class CSVExporterTest {

    private File piaFile;

    @Before
    public void setUp() {
        piaFile = new File(CSVExporterTest.class.getResource("/55merge_mascot_tandem.pia.xml").getPath());
    }


    @Test
    public void testCSVExporter() throws IOException {
        PIAModeller piaModeller = new PIAModeller(piaFile.getAbsolutePath());

        piaModeller.getPSMModeller().setAllDecoyPattern("Rnd.*");
        piaModeller.getPSMModeller().setAllTopIdentifications(1);

        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        // protein level
        SpectrumExtractorInference seInference = new SpectrumExtractorInference();

        seInference.addFilter(
                new PSMScoreFilter(FilterComparator.less_equal, false, 0.75, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));

        seInference.setScoring(new MultiplicativeScoring(new HashMap<>()));
        seInference.getScoring().setSetting(AbstractScoring.SCORING_SETTING_ID, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName());
        seInference.getScoring().setSetting(AbstractScoring.SCORING_SPECTRA_SETTING_ID, PSMForScoring.ONLY_BEST.getShortName());

        piaModeller.getProteinModeller().infereProteins(seInference);

        piaModeller.getProteinModeller().updateFDRData(DecoyStrategy.ACCESSIONPATTERN, "Rnd.*", 0.01);
        piaModeller.getProteinModeller().updateDecoyStates();
        piaModeller.getProteinModeller().calculateFDR();

        // simple exporting
        CSVExporter exporter = new CSVExporter(piaModeller);
        File exportFile = File.createTempFile("pia_testCSV", ".csv");

        assertTrue(exporter.exportToCSV(0L, exportFile,
                true, true, true,
                true));

        exportFile.delete();
    }
}
