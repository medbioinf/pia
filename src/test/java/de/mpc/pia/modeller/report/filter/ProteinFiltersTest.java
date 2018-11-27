package de.mpc.pia.modeller.report.filter;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.inference.SpectrumExtractorInference;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.protein.scoring.MultiplicativeScoring;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;

/**
 * test cases for protein level filters
 * @author julian
 *
 */
public class ProteinFiltersTest {

    private PIAModeller piaModeller = null;

    @Before
    public void setUp() {
        File piaFile = new File(ProteinFiltersTest.class.getResource("/55merge_mascot_tandem.pia.xml").getPath());

        // load the PIA XML file
        piaModeller = new PIAModeller(piaFile.getAbsolutePath());

        // PSM level settings
        piaModeller.setCreatePSMSets(true);
        assertEquals("createPSMSets should be true", true, piaModeller.getCreatePSMSets());

        piaModeller.getPSMModeller().setAllDecoyPattern("Rnd.*");
        piaModeller.getPSMModeller().setAllTopIdentifications(1);

        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        piaModeller.setConsiderModifications(false);


        // protein level
        SpectrumExtractorInference seInference = new SpectrumExtractorInference();

        seInference.addFilter(
                new PSMScoreFilter(FilterComparator.less_equal, false, 0.5, ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName()));

        seInference.setScoring(new MultiplicativeScoring(new HashMap<>()));
        seInference.getScoring().setSetting(AbstractScoring.SCORING_SETTING_ID, ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName());
        seInference.getScoring().setSetting(AbstractScoring.SCORING_SPECTRA_SETTING_ID, PSMForScoring.ONLY_BEST.getShortName());

        piaModeller.getProteinModeller().infereProteins(seInference);

        piaModeller.getProteinModeller().updateFDRData(DecoyStrategy.ACCESSIONPATTERN, "Rnd.*", 0.01);
        piaModeller.getProteinModeller().updateDecoyStates();
        piaModeller.getProteinModeller().calculateFDR();
    }

    @Test
    public void testQValueFilter() {

        ArrayList<AbstractFilter> filters = new ArrayList<>();

        filters.add(RegisteredFilters.PROTEIN_Q_VALUE_FILTER.newInstanceOf(FilterComparator.less_equal, 0.01, false));

        List<ReportProtein> proteinList = piaModeller.getProteinModeller().getFilteredReportProteins(null);
        assertEquals(7, proteinList.size());

        proteinList = piaModeller.getProteinModeller().getFilteredReportProteins(filters);
        assertEquals(3, proteinList.size());
    }
}
