package de.mpc.pia.modeller.report.filter;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.report.filter.impl.PSMTopIdentificationFilter;
import de.mpc.pia.modeller.score.ScoreModelEnum;

/**
 * test cases for PSM level filters
 * @author julian
 *
 */
public class PSMFiltersTest {

    private PIAModeller piaModeller = null;

    @Before
    public void setUp() {
        File piaFile = new File(PSMFiltersTest.class.getResource("/55merge_mascot_tandem.pia.xml").getPath());

        // load the PIA XML file
        piaModeller = new PIAModeller(piaFile.getAbsolutePath());

        // PSM level settings
        piaModeller.setCreatePSMSets(true);
        assertEquals("createPSMSets should be true", true, piaModeller.getCreatePSMSets());

        piaModeller.getPSMModeller().setAllDecoyPattern("Rnd.*");
        piaModeller.getPSMModeller().setAllTopIdentifications(1);

        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();
    }


    @Test
    public void testChargeFilter() {
        ArrayList<AbstractFilter> filters = new ArrayList<>();

        List<ReportPSM> psmList = piaModeller.getPSMModeller().getFilteredReportPSMs(1L, filters);
        int nrAllPSMs = psmList.size();

        // maximum double charged
        filters.add(RegisteredFilters.CHARGE_FILTER.newInstanceOf(FilterComparator.less_equal, 2, false));
        psmList = piaModeller.getPSMModeller().getFilteredReportPSMs(1L, filters);

        int nrMax2ChargePSMs = psmList.size();
        assertEquals("Number of maximal double charged PSMs not 1388", 1388, nrMax2ChargePSMs);
        for (ReportPSM psm : psmList) {
            assertTrue("Charge of PSM not <= 2", psm.getCharge() <= 2);
        }

        // at least triple charged
        filters.clear();
        filters.add(RegisteredFilters.CHARGE_FILTER.newInstanceOf(FilterComparator.greater, 2, false));
        psmList = piaModeller.getPSMModeller().getFilteredReportPSMs(1L, filters);

        int nrMin3ChargePSMs = psmList.size();
        assertEquals("Number of minimal triple charged PSMs not ", 920, nrMin3ChargePSMs);
        assertEquals("Number of maximal double plus minimal triple charged PSMs not equal to all PSMs (2308)", 2308, nrAllPSMs);
        for (ReportPSM psm : psmList) {
            assertTrue("Charge of PSM not > 2", psm.getCharge() > 2);
        }
    }


    @Test
    public void testPSMAccessionsFilter() {
        ArrayList<AbstractFilter> filters = new ArrayList<>();

        filters.add(RegisteredFilters.PSM_ACCESSIONS_FILTER.newInstanceOf(FilterComparator.contains_only, "psu|NC_LIV_020800", false));
        List<ReportPSM> psmList = piaModeller.getPSMModeller().getFilteredReportPSMs(1L, filters);
        assertEquals("There should be three PSMs for psu|NC_LIV_020800", 3, psmList.size());
        for (ReportPSM psm : psmList) {
            assertEquals("Accession of PSM is not psu|NC_LIV_020800", "psu|NC_LIV_020800", psm.getAccessions().get(0).getAccession());
        }

        filters.clear();
        filters.add(RegisteredFilters.PSM_ACCESSIONS_FILTER.newInstanceOf(FilterComparator.regex, "Rnd1psu.*", false));
        psmList = piaModeller.getPSMModeller().getFilteredReportPSMs(1L, filters);

        for (ReportPSM psm : psmList) {
            boolean hasRegex = false;
            for (Accession acc : psm.getAccessions()) {
                if (acc.getAccession().startsWith("Rnd1psu")) {
                    hasRegex = true;
                    break;
                }
            }
            assertTrue("Accessions of PSM has not the regular expression 'Rnd1psu.*'", hasRegex);
        }

    }


    @Test
    public void testTopIdentificationTests() {
        ArrayList<AbstractFilter> filters = new ArrayList<>();

        try {
            filters.add(RegisteredFilters.PSM_TOP_IDENTIFICATION_FILTER.newInstanceOf(FilterComparator.less_equal, 3, false));
            fail("The PSMTopIdentificationFilter should not be able to be instantiated like this!");
        } catch (Exception e) {
            // this is expected
            filters.clear();
        }

        List<ReportPSM> psmList = piaModeller.getPSMModeller().getFilteredReportPSMs(1L, filters);
        int nrAllPSMs = psmList.size();

        String testedScoreShort = "mascot_score";

        // top identifications
        filters.clear();
        filters.add(new PSMTopIdentificationFilter(FilterComparator.equal, 1, false, testedScoreShort));
        psmList = piaModeller.getPSMModeller().getFilteredReportPSMs(1L, filters);

        int nrTop1 = psmList.size();
        assertTrue("Number of top identification not <= number of all PSMs", nrTop1 <= nrAllPSMs);
        for (ReportPSM psm : psmList) {
            assertEquals("PSM is not top ID for " + testedScoreShort, 1, (int) psm.getIdentificationRank(testedScoreShort));
        }

        // all worse than top ID
        filters.clear();
        filters.add(new PSMTopIdentificationFilter(FilterComparator.greater, 1, false, testedScoreShort));
        psmList = piaModeller.getPSMModeller().getFilteredReportPSMs(1L, filters);

        int nrNotTop1 = psmList.size();
        assertTrue("Number of not top IDs <= number of all PSMs", nrNotTop1 <= nrAllPSMs);
        assertEquals("Number of top IDs and not topIDs not equal to all PSMs", nrNotTop1 + nrTop1, nrAllPSMs);
        for (ReportPSM psm : psmList) {
            assertTrue("PSM is not 'not top ID' for " + testedScoreShort, psm.getIdentificationRank(testedScoreShort) > 1);
        }
    }


    @Test
    public void testPSMScoreFilter() {
        ArrayList<AbstractFilter> filters = new ArrayList<>();

        try {
            filters.add(RegisteredFilters.PSM_SCORE_FILTER.newInstanceOf(FilterComparator.less_equal, 10, false));
            fail("The PSMScoreFilter should not be able to be instantiated like this!");
        } catch (Exception e) {
            // this is expected
            filters.clear();
        }

        List<ReportPSM> psmList = piaModeller.getPSMModeller().getFilteredReportPSMs(1L, filters);
        int nrAllPSMs = psmList.size();

        // mascot score filter
        String testedScoreShort = "mascot_score";
        filters.clear();
        filters.add(new PSMScoreFilter(FilterComparator.greater_equal, false, 10.0, testedScoreShort));
        psmList = piaModeller.getPSMModeller().getFilteredReportPSMs(1L, filters);

        int nrMascotGreaterTen = psmList.size();

        assertTrue("Number of not top IDs <= number of all PSMs", nrMascotGreaterTen < nrAllPSMs);
        assertEquals("Number of PSMs with mascot_score >= 10 not correct", 1031, nrMascotGreaterTen);

        // this file has no mascot_score
        psmList = piaModeller.getPSMModeller().getFilteredReportPSMs(2L, filters);
        assertEquals("Number of PSMs with mascot_score >= 10 must be 0 for this file", 0, psmList.size());


        // q-value filtering
        testedScoreShort = ScoreModelEnum.PSM_LEVEL_Q_VALUE.getShortName();
        filters.clear();
        filters.add(new PSMScoreFilter(FilterComparator.less_equal, false, 2.0, testedScoreShort));
        psmList = piaModeller.getPSMModeller().getFilteredReportPSMs(1L, filters);
        assertEquals("Number of PSMs with q-value filter not correct", 30, psmList.size());

        filters.clear();
        filters.add(new PSMScoreFilter(FilterComparator.less_equal, false, 1.0, testedScoreShort));
        psmList = piaModeller.getPSMModeller().getFilteredReportPSMs(2L, filters);
        assertEquals("Number of PSMs with q-value filter not correct", 10, psmList.size());

        filters.clear();
        filters.add(new PSMScoreFilter(FilterComparator.less_equal, false, 2.0, testedScoreShort));
        List<ReportPSMSet> psmSetList = piaModeller.getPSMModeller().getFilteredReportPSMSets(filters);
        assertEquals("Number of PSM sets with q-value filter not correct", 12, psmSetList.size());


        // FDR Score filtering
        testedScoreShort = ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName();
        filters.clear();
        filters.add(new PSMScoreFilter(FilterComparator.less_equal, false, 1.0, testedScoreShort));
        psmList = piaModeller.getPSMModeller().getFilteredReportPSMs(1L, filters);
        assertEquals("Number of PSMs with FDR Score filter not correct", 8, psmList.size());

        testedScoreShort = ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName();
        filters.clear();
        filters.add(new PSMScoreFilter(FilterComparator.less_equal, false, 1.0, testedScoreShort));
        psmSetList = piaModeller.getPSMModeller().getFilteredReportPSMSets(filters);
        assertEquals("Number of PSM sets with Combined FDR Score filter not correct", 15, psmSetList.size());
    }
    
    

    @Test
    public void testPSMDecoyFilter() {
    	ArrayList<AbstractFilter> filters = new ArrayList<>();

        filters.add(RegisteredFilters.PSM_DECOY_FILTER.newInstanceOf(FilterComparator.equal, true, false));
        List<ReportPSM> psmList = piaModeller.getPSMModeller().getFilteredReportPSMs(1L, filters);
        for (ReportPSM psm : psmList) {
            assertFalse("There should be no decoys left in PSMs", psm.getIsDecoy());
        }
        
        filters.clear();
        filters.add(RegisteredFilters.PSM_DECOY_FILTER.newInstanceOf(FilterComparator.equal, false, false));
        psmList = piaModeller.getPSMModeller().getFilteredReportPSMs(1L, filters);
        for (ReportPSM psm : psmList) {
            assertTrue("There should be only decoys in PSMs", psm.getIsDecoy());
        }
    }
}
