package de.mpc.pia.modeller.exporter;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.mpc.pia.modeller.IdentificationKeySettings;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.protein.inference.AbstractProteinInference;
import de.mpc.pia.modeller.protein.inference.OccamsRazorInference;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.protein.scoring.MultiplicativeScoring;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.report.filter.impl.PeptideScoreFilter;
import de.mpc.pia.modeller.score.ScoreModelEnum;


public class MzTabExporterForPRIDEReanalysisTest {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(MzTabExporterForPRIDEReanalysisTest.class);


    private File piaFile;


    @Before
    public void setUp() {
        //piaFile = new File("/mnt/data/uniNOBACKUP/PIA/PXD001428/PXD001428_0.pia.xml");
        //piaFile = new File("/mnt/data/uniNOBACKUP/PIA/PRD000397/PRD000397.pia.xml");
        piaFile = new File("/mnt/data/uniNOBACKUP/PIA/PXD001428/OR8_130622_TT_Trypsin_Ti-IMAC_Rep1_B1[Node_05].scored.pia.xml");
    }


    @Ignore
    @Test
    public void testPeptideLevelExport() throws IOException {
        PIAModeller piaModeller = new PIAModeller(piaFile.getAbsolutePath());

        Map<String, Boolean> psmSetSettings = piaModeller.getPSMModeller().getMaximalPSMSetSettings();
        psmSetSettings.remove(IdentificationKeySettings.SOURCE_ID);

        piaModeller.getPSMModeller().applyGeneralSettings(false /* no PSM sets */);

        //piaModeller.getPSMModeller().setAllDecoyPattern(FDRData.DecoyStrategy.SEARCHENGINE.toString());
        piaModeller.getPSMModeller().setAllDecoyPattern("^XXX.*");
        piaModeller.getPSMModeller().setAllTopIdentifications(0);

        piaModeller.getPSMModeller().calculateAllFDR();
        //piaModeller.getPSMModeller().calculateCombinedFDRScore();

        // calculate peptide FDR
        // first filter PSMs on 0.01 PSM level FDRScore, then calculate the peptide level FDRScore
        Long fileID = 0L;
        piaModeller.getPeptideModeller().addFilter(0L,
                new PSMScoreFilter(FilterComparator.less_equal, false, 0.01, ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName()));

        piaModeller.getPeptideModeller().calculateFDR(fileID);

        // export to mzTab file
        // and export the peptide level statistics, but no filtering (i.e. peptide level FDR values may be null)
        MzTabExporter exporter = new MzTabExporter(piaModeller);
        File exportFile = new File("/mnt/data/uniNOBACKUP/PIA/PXD001428/PXD001428_0.01_peptide_level_fdr.mzTab");
        assertTrue(exporter.exportToMzTab(0L, exportFile, false, true, false));

        //exportFile.delete();
    }


    @Ignore
    @Test
    public void testPeptideAndProteinLevelExport() throws IOException {
        double fdrThreshold = 0.01;

        PIAModeller piaModeller = new PIAModeller(piaFile.getAbsolutePath());

        Map<String, Boolean> psmSetSettings = piaModeller.getPSMModeller().getMaximalPSMSetSettings();
        psmSetSettings.remove(IdentificationKeySettings.SOURCE_ID);

        piaModeller.getPSMModeller().applyGeneralSettings(false /* no PSM sets */);

        // here the decoy regular expression needs to be set
        piaModeller.getPSMModeller().setAllDecoyPattern("^XXX.*");
        piaModeller.getPSMModeller().setAllTopIdentifications(0);

        piaModeller.getPSMModeller().calculateAllFDR();
        // no CombinedFDRScore needed, as no sets are created

        // calculate peptide FDR
        // first filter PSMs on 0.01 PSM level FDRScore, then calculate the peptide level FDRScore
        Long fileID = 0L;
        piaModeller.getPeptideModeller().addFilter(0L,
                new PSMScoreFilter(FilterComparator.less_equal, false, fdrThreshold, ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName()));

        piaModeller.getPeptideModeller().calculateFDR(fileID);


        LOGGER.info("peptide level done");

        // protein level
        AbstractProteinInference protInference = new OccamsRazorInference();

        // the filters for peptide level must be repeated here, plus additional filters
        protInference.addFilter(
                new PSMScoreFilter(FilterComparator.less_equal, false, fdrThreshold, ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName()));
        protInference.addFilter(
                new PeptideScoreFilter(FilterComparator.less_equal, false, fdrThreshold, ScoreModelEnum.PEPTIDE_LEVEL_Q_VALUE.getShortName()));

        protInference.setScoring(new MultiplicativeScoring(new HashMap<>()));
        protInference.getScoring().setSetting(AbstractScoring.SCORING_SETTING_ID, ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName());
        protInference.getScoring().setSetting(AbstractScoring.SCORING_SPECTRA_SETTING_ID, PSMForScoring.ONLY_BEST.getShortName());

        piaModeller.getProteinModeller().infereProteins(protInference);

        LOGGER.info("inference done");

        // export to mzTab file
        // and export the peptide level statistics, but no filtering (i.e. peptide level FDR values may be null)
        MzTabExporter exporter = new MzTabExporter(piaModeller);
        File exportFile = new File("/tmp/0.01_pep_level_fdr_occams_razor.mzTab");
        assertTrue(exporter.exportToMzTab(0L, exportFile, true, true, false));
    }
}
