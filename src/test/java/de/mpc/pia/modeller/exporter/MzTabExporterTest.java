package de.mpc.pia.modeller.exporter;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import de.mpc.pia.modeller.IdentificationKeySettings;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.protein.inference.SpectrumExtractorInference;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.protein.scoring.MultiplicativeScoring;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.ScoreModelEnum;


public class MzTabExporterTest {

    private File piaFile;


    @Before
    public void setUp() {
        piaFile = new File(MzTabExporterTest.class.getResource("/55merge_mascot_tandem.pia.xml").getPath());
        //piaFile = new File("/mnt/data/uniNOBACKUP/PIA/PRD000397/PRD000397.pia.xml");
    }


    @Test
    public void testMzTabExporter() throws IOException {
        PIAModeller piaModeller = new PIAModeller(piaFile.getAbsolutePath());

        Map<String, Boolean> psmSetSettings = piaModeller.getPSMModeller().getMaximalPSMSetSettings();

        // we want to have sets -> remove fileId
        psmSetSettings.remove(IdentificationKeySettings.FILE_ID.toString());

        // they are erronous in PRIDE XML
        psmSetSettings.remove(IdentificationKeySettings.SOURCE_ID.toString());


        // not available in this case
        //psmSetSettings.remove(IdentificationKeySettings.RETENTION_TIME.toString());
        //psmSetSettings.remove(IdentificationKeySettings.SPECTRUM_TITLE.toString());


        piaModeller.getPSMModeller().setPSMSetSettings(psmSetSettings);

        piaModeller.getPSMModeller().applyGeneralSettings(true);

        piaModeller.getPSMModeller().setAllDecoyPattern(FDRData.DecoyStrategy.SEARCHENGINE.toString());
        piaModeller.getPSMModeller().setAllTopIdentifications(0);

        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        // protein level
        SpectrumExtractorInference seInference = new SpectrumExtractorInference();


        seInference.addFilter(
                new PSMScoreFilter(FilterComparator.less_equal, false, 0.5, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));


        seInference.setScoring(new MultiplicativeScoring(new HashMap<String, String>()));
        seInference.getScoring().setSetting(AbstractScoring.scoringSettingID, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName());
        seInference.getScoring().setSetting(AbstractScoring.scoringSpectraSettingID, PSMForScoring.ONLY_BEST.getShortName());

        piaModeller.getProteinModeller().infereProteins(seInference);

        // simple exporting
        MzTabExporter exporter = new MzTabExporter(piaModeller);
        File exportFile = File.createTempFile("pia_testMzTabExporter", ".mzTab");

        assertTrue(exporter.exportToMzTab(0L, exportFile, true, true, false));

        exportFile.delete();
    }


    @Test
    public void testPeptideLevelExport() throws IOException {
        PIAModeller piaModeller = new PIAModeller(piaFile.getAbsolutePath());

        Map<String, Boolean> psmSetSettings = piaModeller.getPSMModeller().getMaximalPSMSetSettings();
        psmSetSettings.remove(IdentificationKeySettings.SOURCE_ID);

        piaModeller.getPSMModeller().applyGeneralSettings(true);

        //piaModeller.getPSMModeller().setAllDecoyPattern(FDRData.DecoyStrategy.SEARCHENGINE.toString());
        piaModeller.getPSMModeller().setAllDecoyPattern("Rnd.*");
        piaModeller.getPSMModeller().setAllTopIdentifications(0);

        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        // calculate peptide FDR
        piaModeller.getPeptideModeller().addFilter(0L,
                new PSMScoreFilter(FilterComparator.less_equal, false, 0.5, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));

        Long fileID = 0L;
        piaModeller.getPeptideModeller().calculateFDR(fileID);

        // export to mzTab file
        MzTabExporter exporter = new MzTabExporter(piaModeller);
        File exportFile = File.createTempFile("pia_testPeptideLevelExport", ".mzTab");
        assertTrue(exporter.exportToMzTab(0L, exportFile, false, true, false));

        exportFile.delete();
    }
}
