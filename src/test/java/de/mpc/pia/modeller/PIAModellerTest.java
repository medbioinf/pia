package de.mpc.pia.modeller;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.junit.BeforeClass;
import org.junit.Test;

import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.score.ScoreModelEnum;


public class PIAModellerTest {

    public static File piaFile;


    @BeforeClass
    public static void initialize() {
        piaFile = new File(PIAModellerTest.class.getResource("/55merge_mascot_tandem.pia.xml").getPath());
    }


    @Test
    public void testBasicFileLoading()
            throws JAXBException, XMLStreamException, IOException {

        PIAModeller piaModeller = new PIAModeller(piaFile.getAbsolutePath());

        assertEquals("ProjectName is not correct", "testfile", piaModeller.getProjectName());
        assertEquals("number of files in PIA XML is not correct", 2, piaModeller.getFiles().size());

        piaModeller.setCreatePSMSets(true);
        assertEquals("createPSMSets should be true", true, piaModeller.getCreatePSMSets());

        assertEquals("number of PSM sets is wrong", 2426, piaModeller.getPSMModeller().getNrReportPSMs(0L));
        assertEquals("number of PSMs in file 1 is wrong", 2308, piaModeller.getPSMModeller().getNrReportPSMs(1L));
        assertEquals("number of PSMs in file 2 is wrong", 170, piaModeller.getPSMModeller().getNrReportPSMs(2L));

        piaModeller.getPSMModeller().setAllDecoyPattern("Rnd.*");
        piaModeller.getPSMModeller().setAllTopIdentifications(1);

        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        piaModeller.getPSMModeller().addFilter(0L,
                new PSMScoreFilter(FilterComparator.less_equal, false, 0.5, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));

        assertEquals("number of filtered PSM sets is wrong", 9,
                piaModeller.getPSMModeller().getFilteredReportPSMSets(piaModeller.getPSMModeller().getFilters(0L)).size());
    }
}
