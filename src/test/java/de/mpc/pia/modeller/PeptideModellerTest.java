package de.mpc.pia.modeller;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.*;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.inference.SpectrumExtractorInference;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.protein.scoring.MultiplicativeScoring;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;



public class PeptideModellerTest {

    private static File piaFile;

    private PIAModeller piaModeller;


    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(PeptideModellerTest.class);


    @BeforeClass
    public static void setUpBeforeClass() {
        piaFile = new File(PIAModellerTest.class.getResource("/55merge_mascot_tandem.pia.xml").getPath());
    }

    @Before
    public void setUp() {
        piaModeller = new PIAModeller(piaFile.getAbsolutePath());
    }

    @Test
    public void testDecoysAndFDR() {
        // set everything on PSM level (but not calculating the FDR now)
        piaModeller.setCreatePSMSets(true);

        piaModeller.getPSMModeller().setAllDecoyPattern("Rnd.*");
        piaModeller.getPSMModeller().setAllTopIdentifications(1);

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


    @Test
    public void testProteinInferenceAndRetrieveProteinsPeptidesPSMs() {
        // set everything on PSM level (but not calculating the FDR now)
        piaModeller.setCreatePSMSets(true);

        piaModeller.getPSMModeller().setAllDecoyPattern("Rnd.*");
        piaModeller.getPSMModeller().setAllTopIdentifications(1);

        // calculate FDR on PSM level
        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        // calculate peptide FDR
        piaModeller.getPeptideModeller().addFilter(0L, new PSMScoreFilter(FilterComparator.less_equal, false, 0.5, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));
        Long fileID = 0L;
        piaModeller.getPeptideModeller().calculateFDR(fileID);


        // perform protein inference
        SpectrumExtractorInference seInference = new SpectrumExtractorInference();

        seInference.addFilter(
                new PSMScoreFilter(FilterComparator.less_equal, false, 0.5, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));

        seInference.setScoring(new MultiplicativeScoring(new HashMap<>()));
        seInference.getScoring().setSetting(AbstractScoring.SCORING_SETTING_ID, ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName());
        seInference.getScoring().setSetting(AbstractScoring.SCORING_SPECTRA_SETTING_ID, PSMForScoring.ONLY_BEST.getShortName());

        piaModeller.getProteinModeller().infereProteins(seInference);

        piaModeller.getProteinModeller().updateFDRData(DecoyStrategy.ACCESSIONPATTERN, "s.*", 0.01);
        piaModeller.getProteinModeller().updateDecoyStates();
        piaModeller.getProteinModeller().calculateFDR();



        List<ReportProtein> proteins = piaModeller.getProteinModeller().getFilteredReportProteins(null);
        assertEquals("Wrong number of total proteins", 7, proteins.size());

        Map<String, ReportPeptide> rawPeptides = new HashMap<>();
        for (ReportPeptide repPep : piaModeller.getPeptideModeller().getFilteredReportPeptides(0L,
                piaModeller.getPeptideModeller().getFilters(0L))) {
            rawPeptides.put(repPep.getStringID(), repPep);
        }

        assertEquals("Wrong number of total peptides", 8, rawPeptides.size());

        // list the proteins, peptides (with FDR data!!) and PSMs
        for (ReportProtein protein : proteins) {
            LOGGER.debug("PROTEIN");
            for (Accession acc : protein.getAccessions()) {
                LOGGER.debug("\t" + acc.getAccession());
            }

            LOGGER.debug("\tPEPTIDES");
            for (ReportPeptide peptide : protein.getPeptides()) {
                // this is a hack to get the "raw" peptide with it's statistics
                String peptideId = peptide.getPSMs().get(0).getPeptideStringID(piaModeller.getConsiderModifications());
                ReportPeptide rawPeptide = rawPeptides.get(peptideId);
                LOGGER.debug("\t\tFDRScore:" + rawPeptide.getFDRScore() + "\t" + rawPeptide.getSequence());

                for (PSMReportItem psm : peptide.getPSMs()) {
                    LOGGER.debug("\t\t\tFDRScore:" + psm.getFDRScore() + "\t" + psm.toString());
                }
            }
        }
    }


}
