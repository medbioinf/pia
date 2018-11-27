package de.mpc.pia.modeller;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.PIASimpleCompiler;
import de.mpc.pia.intermediate.compiler.parser.InputFileParserFactory;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.report.filter.impl.PeptideScoreFilter;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This Test allows to compute the FDR at Peptide level for a PRIDE dataset based on mzTab files.
 *
 * Created by ypriverol (ypriverol@gmail.com) on 23/08/2017.
 */
public class PIAPRIDEModellerTest {

    private static final Long MERGE_FILE_ID = 0L;

    org.apache.log4j.Logger logger = Logger.getLogger(PeptideModellerTest.class);

    @Test
    public void testPRIDESubmission() throws URISyntaxException, IOException {

        URL folderSubmission = PIAModellerTest.class.getClassLoader().getResource("submission");
        File mzTabFolder = new File(folderSubmission.toURI());
        HashMap<String, String> files = new HashMap<>();
        int countAssay = 0;
        if(mzTabFolder.isDirectory() && mzTabFolder.listFiles().length > 0){
            for(File file: mzTabFolder.listFiles((dir, name) -> {
                if(name.endsWith(".mztab"))
                    return true;
                return false;
            })){
                files.put(file.getAbsolutePath(), Integer.toString(countAssay));
                countAssay++;
            }
        }

        PIAModeller modeller = computeFDRPSMLevel(files, "PXD000001");

        // TODO: @yasset check, whether you need this or not... you should only trust data containing FDR PSMs though
        // reportPSMSets still exist (even without set creation), but are effectively single PSMs for the overview
        // need to update the decoy states of the overview, as it was never done before (because no combined fdr score was calculated)
        long nrDecoys = modeller.getPSMModeller().getReportPSMSets().entrySet().stream()
                .filter(entry -> entry.getValue().getIsDecoy())
                .count();

        logger.debug("decoys calculated in all files: " + modeller.getPSMModeller().getAllFilesHaveFDRCalculated() );
        logger.debug("decoys in all files: " + nrDecoys);

        // check, whether the FDR is calculated and whether there are any decoys in all files
        if (modeller.getPSMModeller().getAllFilesHaveFDRCalculated()
                && nrDecoys > 0) {

            // the PSM level filter should be set before calculating the peptide level FDR
            modeller.getPeptideModeller().addFilter(MERGE_FILE_ID,
                    new PSMScoreFilter(FilterComparator.less_equal, false, 0.01,
                            ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName()));        // as combinedFDRScore was not calculated, use "normal" fdr score

            // calculate the peptide FDR using the PSM filter
            modeller.getPeptideModeller().calculateFDR(MERGE_FILE_ID);

            // setting filter for peptide level filtering
            List<AbstractFilter> filters = new ArrayList<>();
            filters.add(new PeptideScoreFilter(FilterComparator.less_equal, false, 0.01,
                    ScoreModelEnum.PEPTIDE_LEVEL_Q_VALUE.getShortName()));              // you can also use fdr score here

            // get the FDR filtered peptides
            List<ReportPeptide> peptides = modeller.getPeptideModeller().getFilteredReportPeptides(MERGE_FILE_ID, filters);

            List<ReportPeptide> noDecoyPeptides = new ArrayList<>();
            if (!peptides.isEmpty()) {
                for (ReportPeptide peptide : peptides) {
                    if (!peptide.getIsDecoy()) {
                        noDecoyPeptides.add(peptide);
                        logger.debug("Peptide Sequence: " + peptide.getSequence() + " q-value: " + peptide.getQValue());
                    }
                }
            } else {
                logger.error("There are no peptides at all!");
            }

            Assert.assertEquals(9409, noDecoyPeptides.size());

            logger.info("number of FDR 0.01 filtered target peptides: " + noDecoyPeptides.size() + " / " + peptides.size());
        } else {
            logger.info("no decoys in the data!");
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
            for (Map.Entry<String, String> entry : mzTabFileMap.entrySet()) {
                String assayKey = (String) entry.getValue();
                String fileName = (String) entry.getKey();
                piaCompiler.getDataFromFile(assayKey, fileName, null,
                        InputFileParserFactory.InputFileTypes.MZTAB_INPUT.getFileTypeShort());
            }
        }
        piaCompiler.buildClusterList();
        piaCompiler.buildIntermediateStructure();
        if (piaCompiler.getAllPeptideSpectrumMatcheIDs() != null
                && !piaCompiler.getAllPeptideSpectrumMatcheIDs().isEmpty()) {
            File inferenceTempFile = File.createTempFile(PXProjectIdentifier, ".tmp");
            piaCompiler.writeOutXML(inferenceTempFile);
            piaCompiler.finish();
            piaModeller = new PIAModeller(inferenceTempFile.getAbsolutePath());

            // creation of PSM sets is only useful, when combining multiple search engines
            piaModeller.setCreatePSMSets(false);
            piaModeller.getPSMModeller().setAllDecoyPattern("searchengine");
            piaModeller.getPSMModeller().setAllTopIdentifications(0);

            // calculate FDR on PSM level
            piaModeller.getPSMModeller().calculateAllFDR();

            // combined FDR Score is mainly used for combining search engine results, but can also be used without.
            // if used without creation of PSM sets, the Combined FDR Score is equal to the "normal" FDR Score
            //piaModeller.getPSMModeller().calculateCombinedFDRScore();
            // updating the decoy states is necessary, if CombinedFDRScore was not calculated
            piaModeller.getPSMModeller().updateDecoyStates(MERGE_FILE_ID);

            if (inferenceTempFile.exists()) {
                inferenceTempFile.deleteOnExit();
            }
        }
        return piaModeller;
    }

}
