package de.mpc.pia.modeller;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.PIASimpleCompiler;
import de.mpc.pia.intermediate.compiler.parser.InputFileParserFactory;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.impl.PeptideScoreFilter;
import org.junit.*;

import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.ScoreModelEnum;


public class PeptideModellerTest {

    private static File piaFile;

    private PIAModeller piaModeller;
    private static final Long MERGE_FILE_ID = 0L;


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

    @Ignore
    @Test
    public void testPRIDESubmission() throws URISyntaxException, IOException {

        URL folderSubmission = PIAModellerTest.class.getClassLoader().getResource("submission");
        File mzTabFolder = new File(folderSubmission.toURI());
        HashMap<String, String> files = new HashMap<>();
        int countAssay = 0;
        if(mzTabFolder.isDirectory() && mzTabFolder.listFiles().length > 0){
            for(File file: mzTabFolder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if(name.endsWith(".mztab"))
                        return true;
                    return false;
                }
            })){
                files.put(file.getAbsolutePath(), Integer.toString(countAssay));
                countAssay++;
            }
        }

        PIAModeller modeller = computeFDRPSMLevel(files, "PXD000001");
        modeller.getPeptideModeller().calculateFDR(MERGE_FILE_ID);
        if (modeller.getPSMModeller().getAllFilesHaveFDRCalculated() && modeller.getPSMModeller().getFilesFDRData(MERGE_FILE_ID).getNrFDRGoodDecoys() > 0) {

            List<AbstractFilter> filters = new ArrayList<>();
            filters.add(new PSMScoreFilter(FilterComparator.less_equal, false, 0.001, ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName()));
            filters.add(new PeptideScoreFilter(FilterComparator.less_equal, false, 0.01, ScoreModelEnum.PEPTIDE_LEVEL_Q_VALUE.getShortName()));
            List<ReportPeptide> peptides = modeller.getPeptideModeller().getFilteredReportPeptides(MERGE_FILE_ID, filters);
            List<ReportPeptide> noDecoyPeptides = new ArrayList<>();
            if (!peptides.isEmpty()) {
                for (ReportPeptide peptide : peptides) {
                    if (!peptide.getIsDecoy()) {
                        noDecoyPeptides.add(peptide);
                    }
                }
            } else {
                System.out.println("There are no peptides at all!");
            }
        }
    }

    /**
     * This functiion returns a {@link PIAModeller} Object containing the necesary structure to estimate
     * the FDR at PSM and Peptide Level during the conversion process.
     * @param mzTabFileMap The mztab Files by Assay, the current Key of the map is mzTafile file and the value is the Assay ID.
     * @param PXProjectIdentifier PX Project accession
     * @return The {@link PIAModeller} object to compute the FDR of PSMs and Peptides.
     * @throws IOException problems computing FDR
     */
    private PIAModeller computeFDRPSMLevel(Map<String, String> mzTabFileMap, String PXProjectIdentifier) throws IOException {
        PIAModeller piaModeller = null;
        PIACompiler piaCompiler = new PIASimpleCompiler();
        if (mzTabFileMap != null && !mzTabFileMap.isEmpty()) {
            for (Map.Entry entry : mzTabFileMap.entrySet()) {
                String assayKey = (String) entry.getValue();
                String fileName = (String) entry.getKey();
                piaCompiler.getDataFromFile(assayKey, fileName, null, InputFileParserFactory.InputFileTypes.MZTAB_INPUT.getFileTypeShort());
            }
        }
        piaCompiler.buildClusterList();
        piaCompiler.buildIntermediateStructure();
        if (piaCompiler.getAllPeptideSpectrumMatcheIDs() != null && !piaCompiler.getAllPeptideSpectrumMatcheIDs().isEmpty()) {
            File inferenceTempFile = File.createTempFile(PXProjectIdentifier, ".tmp");
            piaCompiler.writeOutXML(inferenceTempFile);
            piaCompiler.finish();
            piaModeller = new PIAModeller(inferenceTempFile.getAbsolutePath());
            piaModeller.setCreatePSMSets(true);
            piaModeller.getPSMModeller().setAllDecoyPattern("searchengine");
            piaModeller.getPSMModeller().setAllTopIdentifications(0);
            // calculate FDR on PSM level
            piaModeller.getPSMModeller().calculateAllFDR();
            piaModeller.getPSMModeller().calculateCombinedFDRScore();
            if (inferenceTempFile.exists()) {
                inferenceTempFile.deleteOnExit();
            }
        }
        return piaModeller;
    }
}
