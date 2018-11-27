package de.mpc.pia.modeller;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.mpc.pia.intermediate.compiler.PIACompilerTest;
import de.mpc.pia.modeller.protein.inference.SpectrumExtractorInference;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.protein.scoring.MultiplicativeScoring;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.score.ScoreModelEnum;

public class SerializationTest {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(SerializationTest.class);


    private static File piaFile;


    @BeforeClass
    public static void initialize() {
        piaFile = new File(PIAModellerTest.class.getResource("/55merge_mascot_tandem.pia.xml").getPath());
    }


    @Test
    public void testSerializingDeserializing() throws IOException {

        PIAModeller piaModeller = new PIAModeller(piaFile.getAbsolutePath());

        piaModeller.setCreatePSMSets(true);

        piaModeller.getPSMModeller().setAllDecoyPattern("Rnd.*");
        piaModeller.getPSMModeller().setAllTopIdentifications(1);

        piaModeller.getPSMModeller().addPreferredFDRScore("mascot_expect");

        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        piaModeller.getPSMModeller().addFilter(0L,
                new PSMScoreFilter(FilterComparator.less_equal, false, 0.5, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));

        // protein level
        SpectrumExtractorInference seInference = new SpectrumExtractorInference();

        seInference.addFilter(
                new PSMScoreFilter(FilterComparator.less_equal, false, 0.5, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));

        seInference.setScoring(new MultiplicativeScoring(new HashMap<>()));
        seInference.getScoring().setSetting(AbstractScoring.SCORING_SETTING_ID, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName());
        seInference.getScoring().setSetting(AbstractScoring.SCORING_SPECTRA_SETTING_ID, PSMForScoring.ONLY_BEST.getShortName());

        piaModeller.getProteinModeller().infereProteins(seInference);

        File serialFile = File.createTempFile("pia_serialize", ".pia");
        PIAModeller.serializeToFile(piaModeller, serialFile);


        // reading in the serialized object
        piaModeller = PIAModeller.deSerializeFromFile(serialFile);

        assertEquals("ProjectName is not correct", "testfile", piaModeller.getProjectName());
        assertEquals("number of files in PIA XML is not correct", 2, piaModeller.getFiles().size());

        assertEquals("createPSMSets should be true", true, piaModeller.getCreatePSMSets());

        assertEquals("Decoy pattern wrong", "Rnd.*", piaModeller.getPSMModeller().getFilesFDRData(1L).getDecoyPattern());
        assertEquals("Decoy pattern wrong", "Rnd.*", piaModeller.getPSMModeller().getFilesFDRData(2L).getDecoyPattern());

        assertEquals("number of PSM sets is wrong", 2420, piaModeller.getPSMModeller().getNrReportPSMs(0L));
        assertEquals("number of PSMs in file 1 is wrong", 2308, piaModeller.getPSMModeller().getNrReportPSMs(1L));
        assertEquals("number of PSMs in file 2 is wrong", 170, piaModeller.getPSMModeller().getNrReportPSMs(2L));


        assertEquals("wrong preferred FDR score", "mascot_expect",
                piaModeller.getPSMModeller().getPreferredFDRScores().get(0));

        assertEquals("number of filtered PSM sets is wrong", 8,
                piaModeller.getPSMModeller().getFilteredReportPSMSets(piaModeller.getPSMModeller().getFilters(0L)).size());

        assertEquals("number of filtered protein groups is wrong", 6,
                piaModeller.getProteinModeller().getFilteredReportProteins(null).size());


        assertEquals("Wrong inference method", "Spectrum Extractor",
                piaModeller.getProteinModeller().getAppliedProteinInference().getName());

        assertEquals("Wrong scoring method", MultiplicativeScoring.NAME,
                piaModeller.getProteinModeller().getAppliedProteinInference().getScoring().getName());

        assertEquals("Wrong number of filters", 1,
                piaModeller.getProteinModeller().getAppliedProteinInference().getFilters().size());

        assertEquals("Wrong filter", FilterComparator.less_equal,
                piaModeller.getProteinModeller().getAppliedProteinInference().getFilters().get(0).getFilterComparator());

        serialFile.delete();
    }


    @Ignore
    @Test
    public void testSerializingCustom() throws IOException {
        File mzid55mergeIntermediate = new File(PIACompilerTest.class.getResource("/55merge-tandem_msgf_from_idxml.pia.xml").getPath());


        PIAModeller piaModeller = new PIAModeller(mzid55mergeIntermediate.getAbsolutePath());

        piaModeller.setCreatePSMSets(true);

        piaModeller.getPSMModeller().setAllDecoyPattern("Rnd.*");
        piaModeller.getPSMModeller().setAllTopIdentifications(1);

        piaModeller.getPSMModeller().calculateAllFDR();
        piaModeller.getPSMModeller().calculateCombinedFDRScore();

        piaModeller.getPSMModeller().addFilter(0L,
                new PSMScoreFilter(FilterComparator.less_equal, false, 0.5, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));

        // protein level
        SpectrumExtractorInference seInference = new SpectrumExtractorInference();

        seInference.addFilter(
                new PSMScoreFilter(FilterComparator.less_equal, false, 0.5, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()));

        seInference.setScoring(new MultiplicativeScoring(new HashMap<>()));
        seInference.getScoring().setSetting(AbstractScoring.SCORING_SETTING_ID, ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName());
        seInference.getScoring().setSetting(AbstractScoring.SCORING_SPECTRA_SETTING_ID, PSMForScoring.ONLY_BEST.getShortName());


        piaModeller.getProteinModeller().infereProteins(seInference);

        File serialFile = File.createTempFile("pia_serialize", ".pia");
        /*
        PIAModeller.serializeToFile(piaModeller, serialFile);
        */


        try (FileOutputStream fos = new FileOutputStream(serialFile);
                GZIPOutputStream gzo = new GZIPOutputStream(fos);
                ObjectOutputStream oos = new ObjectOutputStream(gzo)) {

            oos.writeObject(piaModeller);

        }

        /*
        LOGGER.debug("object: "
                + piaModeller.getPSMModeller().getEvilData().get(new Long(14))
                        .getSpectrum().getSequence()
                );
        */

        piaModeller = new PIAModeller();

        PIAModeller readModel;

        try (FileInputStream fin = new FileInputStream(serialFile);
                GZIPInputStream gzi = new GZIPInputStream(fin);
                ObjectInputStream ois = new ObjectInputStream(gzi)) {

                Object readObject = ois.readObject();
                if (readObject instanceof PIAModeller) {
                    LOGGER.debug("     ist richtiges object " + readObject);
                    readModel = ( PIAModeller ) readObject;
                    LOGGER.debug("      " + readModel);
                } else {
                    String msg = "Could not read a PIAModeller from the file " + serialFile.getAbsolutePath();
                    System.err.println(msg);
                    throw new IOException(msg);
                }

        } catch (IOException e) {
            System.err.println("IOException Could not read PIA model from " + serialFile.getAbsolutePath());
            e.printStackTrace();
            throw e;
        } catch (ClassNotFoundException e) {
            String msg = "ClassNotFoundException Could not read PIA model from " + serialFile.getAbsolutePath();
            System.err.println(msg);
            e.printStackTrace();
            throw new IOException(msg, e);
        }


        /*
        // reading in the serialized object
        piaModeller = PIAModeller.deSerializeFromFile(serialFile);

        assertEquals("ProjectName is not correct", "testfile", piaModeller.getProjectName());
        assertEquals("number of files in PIA XML is not correct", 2, piaModeller.getFiles().size());

        assertEquals("createPSMSets should be true", true, piaModeller.getCreatePSMSets());

        assertEquals("Decoy pattern wrong", "Rnd.*", piaModeller.getPSMModeller().getFilesFDRData(1L).getDecoyPattern());
        assertEquals("Decoy pattern wrong", "Rnd.*", piaModeller.getPSMModeller().getFilesFDRData(2L).getDecoyPattern());

        assertEquals("number of PSM sets is wrong", 2426, piaModeller.getPSMModeller().getNrReportPSMs(0L));
        assertEquals("number of PSMs in file 1 is wrong", 2308, piaModeller.getPSMModeller().getNrReportPSMs(1L));
        assertEquals("number of PSMs in file 2 is wrong", 170, piaModeller.getPSMModeller().getNrReportPSMs(2L));


        assertEquals("wrong preferred FDR score", "mascot_expect",
                piaModeller.getPSMModeller().getPreferredFDRScores().get(0));

        assertEquals("number of filtered PSM sets is wrong", 8,
                piaModeller.getPSMModeller().getFilteredReportPSMSets(piaModeller.getPSMModeller().getFilters(0L)).size());

        assertEquals("number of filtered protein groups is wrong", 6,
                piaModeller.getProteinModeller().getFilteredReportProteins(null).size());


        assertEquals("Wrong inference method", "Spectrum Extractor",
                piaModeller.getProteinModeller().getAppliedProteinInference().getName());

        assertEquals("Wrong scoring method", MultiplicativeScoring.NAME,
                piaModeller.getProteinModeller().getAppliedProteinInference().getScoring().getName());

        assertEquals("Wrong number of filters", 1,
                piaModeller.getProteinModeller().getAppliedProteinInference().getFilters().size());

        assertEquals("Wrong filter", FilterComparator.less_equal,
                piaModeller.getProteinModeller().getAppliedProteinInference().getFilters().get(0).getFilterComparator());
        */
    }
}
