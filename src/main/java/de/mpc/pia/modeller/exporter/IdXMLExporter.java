package de.mpc.pia.modeller.exporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.inference.AbstractProteinInference;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.settings.Setting;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.tools.CleavageAgent;
import de.mpc.pia.tools.OntologyConstants;
import de.mpc.pia.tools.PIAConstants;
import de.mpc.pia.tools.openms.jaxb.DigestionEnzyme;
import de.mpc.pia.tools.openms.jaxb.MassType;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.Enzyme;

public class IdXMLExporter {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(IdXMLExporter.class);


    /** the modeller, that should be exported */
    private PIAModeller piaModeller;


    /** type for userParam "string" */
    private static final String STRING_TYPE = "string";



    public IdXMLExporter(PIAModeller modeller) {
        this.piaModeller = modeller;
    }


    public boolean exportPSMLevel(Long fileID, String fileName) {
        File exportFile = new File(fileName);
        return exportPSMLevel(fileID, exportFile);
    }


    public boolean exportPSMLevel(Long fileID, File exportFile) {
        return exportToIdXML(fileID, exportFile, false);
    }


    public boolean exportToIdXML(Long fileID, File exportFile, boolean proteinLevel) {
        OutputStream out = null;
        boolean error = false;

        try {
            out = new FileOutputStream(exportFile, false);
            // create an XMLOutputFactory
            XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter streamWriter = new IndentingXMLStreamWriter(outputFactory.createXMLStreamWriter(out));

            // write common idXML header
            streamWriter.writeStartDocument("utf-8", "1.0");
            streamWriter.writeProcessingInstruction("xml-stylesheet",
                    "type=\"text/xsl\" href=\"http://open-ms.sourceforge.net/XSL/IdXML.xsl\"");
            streamWriter.writeCharacters("\n");

            streamWriter.writeStartElement("IdXML");


            // <SearchParameters                one for each IdentificationRun

            Map<Long, String> inputFileIDToSearchParameter = new HashMap<>();
            if (proteinLevel || (fileID < 1)) {
                // use basic settings for this case
                // TODO: get the real settings

                inputFileIDToSearchParameter.put(0L, "SP_0");
                writeSearchParameters(streamWriter,
                        "SP_0", "", "", "", MassType.MONOISOTOPIC, "", DigestionEnzyme.UNKNOWN_ENZYME,
                        0, 0.0, false, 0.0, false);
            } else {
                inputFileIDToSearchParameter.put(fileID, "SP_0");

                writeSearchParameters(streamWriter,
                        "SP_0",
                        getSearchDatabase(fileID),
                        "",             // TODO: get the db version right
                        "",             // TODO: get the taxonomy right
                        getMassType(fileID),
                        "",             // TODO: get the charges right
                        getDigestionEnzyme(fileID),
                        0,              // TODO: get the missed cleavages
                        0.0, false,     // TODO: get the precursor tolerances
                        0.0, false);    // TODO: get the peak mass tolerances
            }

            writeIdentificationRun(streamWriter, fileID, inputFileIDToSearchParameter.get(fileID), proteinLevel);

            // close the idXML and the XML
            streamWriter.writeEndElement();
            streamWriter.writeEndDocument();

            streamWriter.flush();
            streamWriter.close();
        } catch (IOException e) {
            LOGGER.error("Error while trying to write to " + exportFile.getAbsolutePath(), e);
            error = true;
        } catch (XMLStreamException e) {
            LOGGER.error("Error while writing XML to " + exportFile.getAbsolutePath(), e);
            error = true;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    LOGGER.error("Error while trying to close " + exportFile.getAbsolutePath(), e);
                    error = true;
                }
            }
        }

        return !error;
    }


    /**
     * Write the SearchParameters tag
     *
     * @param streamWriter
     * @param id
     * @param db
     * @param db_version
     * @param taxonomy
     * @param mass_type
     * @param charges
     * @param enzyme
     * @param missed_cleavages
     * @param precursor_peak_tolerance
     * @param precursor_peak_tolerance_ppm
     * @param peak_mass_tolerance
     * @param peak_mass_tolerance_ppm
     * @throws XMLStreamException
     */
    private void writeSearchParameters(XMLStreamWriter streamWriter,
            String id, String db, String db_version, String taxonomy, MassType mass_type,
            String charges, DigestionEnzyme enzyme, Integer missed_cleavages,
            Double precursor_peak_tolerance, Boolean precursor_peak_tolerance_ppm,
            Double peak_mass_tolerance, Boolean peak_mass_tolerance_ppm)
            throws XMLStreamException {
        streamWriter.writeStartElement("SearchParameters");

        streamWriter.writeAttribute("id", id);
        streamWriter.writeAttribute("db", db);
        streamWriter.writeAttribute("db_version", db_version);
        streamWriter.writeAttribute("taxonomy", taxonomy);
        streamWriter.writeAttribute("mass_type", mass_type.value());
        streamWriter.writeAttribute("charges", charges);
        streamWriter.writeAttribute("enzyme", enzyme.value());
        streamWriter.writeAttribute("missed_cleavages", missed_cleavages.toString());
        streamWriter.writeAttribute("precursor_peak_tolerance", precursor_peak_tolerance.toString());
        streamWriter.writeAttribute("precursor_peak_tolerance_ppm", precursor_peak_tolerance_ppm.toString());
        streamWriter.writeAttribute("peak_mass_tolerance", peak_mass_tolerance.toString());
        streamWriter.writeAttribute("peak_mass_tolerance_ppm", peak_mass_tolerance_ppm.toString());

        streamWriter.writeEndElement();
    }


    /**
     * Writes the IdentificationRun to the XML file
     * @param streamWriter
     * @param fileID
     * @param spId
     * @param proteinLevel
     * @throws XMLStreamException
     */
    private void writeIdentificationRun(XMLStreamWriter streamWriter, Long fileID,
            String spId, boolean proteinLevel)
            throws XMLStreamException {
        streamWriter.writeStartElement("IdentificationRun");

        Date now = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'hh.mm.ss");
        String date = formatter.format(now);

        String searchEngine = "";
        String searchEngineVersion = "";
        String proteinScore = "";
        Boolean proteinScoreHigherBetter = null;
        Double proteinScoreSignificanceThreshold = null;
        if (proteinLevel || (fileID < 1)) {
            // only one IdentificationRun will be written (for the consensus)
            searchEngine = "PIA";
            searchEngineVersion = PIAConstants.version;

            proteinScore = OntologyConstants.PIA_PROTEIN_SCORE.getPsiName();
            proteinScoreHigherBetter = true;
            proteinScoreSignificanceThreshold = 0.0;    // TODO: this might be set by filters
        } else {
            // only the IdentificationRun for the selected file will be written
            // TODO: set the searchEngine and version
        }

        streamWriter.writeAttribute("date", date);
        streamWriter.writeAttribute("search_engine", searchEngine);
        streamWriter.writeAttribute("search_engine_version", searchEngineVersion);
        streamWriter.writeAttribute("search_parameters_ref", spId);



        // no filtering at the moment
        List<AbstractFilter> filters = new ArrayList<>();

        // mapping from the spectra to the identifications / PSMs
        Map<String, List<PSMReportItem>> peptideIdentifications =
                getPSMsForPeptideIdentifications(proteinLevel, fileID, filters);


        // ---- Protein Identifications ----
        streamWriter.writeStartElement("ProteinIdentification");
        //<ProteinIdentification score_type="Mascot" higher_score_better="true" significance_threshold="0" >

        streamWriter.writeAttribute("score_type", proteinScore);
        if (proteinScoreHigherBetter != null) {
            streamWriter.writeAttribute("higher_score_better", proteinScoreHigherBetter.toString());
        }
        if (proteinScoreSignificanceThreshold != null) {
            streamWriter.writeAttribute("significance_threshold", proteinScoreSignificanceThreshold.toString());
        }

        Map<String, String> accessionToPH = new HashMap<>();

        List<Object[]> indistinguishableList = new ArrayList<>();

        if (proteinLevel) {

            for (ReportProtein protein : piaModeller.getProteinModeller().getFilteredReportProteins(filters)) {
                Double qvalue = null;

                List<String> phIDs = writeAccessionsToXML(streamWriter,
                        protein.getAccessions(), protein.getScore(), qvalue, fileID, accessionToPH);

                Object[] indisObject = new Object[protein.getAccessions().size() + 1];

                indisObject[0] = protein.getScore();
                for (int idx = 0; idx < phIDs.size(); idx++) {
                    indisObject[idx + 1] = phIDs.get(idx);
                }

                indistinguishableList.add(indisObject);
            }
        }

        Iterator<List<PSMReportItem>> peptideHitIter = peptideIdentifications.values().iterator();
        while (peptideHitIter.hasNext()) {

            for (PSMReportItem psmReportItem : peptideHitIter.next()) {
                writeAccessionsToXML(streamWriter, psmReportItem.getAccessions(), 0.0,
                        null, fileID, accessionToPH);
            }
        }

        // TODO: write PIA inference params
        /*
        <UserParam type="float" name="Fido_prob_protein" value="0.9"/>
        <UserParam type="float" name="Fido_prob_peptide" value="0.09"/>
        <UserParam type="float" name="Fido_prob_spurious" value="0"/>
        */

        for (int idx=0; idx < indistinguishableList.size(); idx++) {
            StringBuilder indisValue = new StringBuilder();
            for (Object value : indistinguishableList.get(idx)) {
                if (indisValue.length() > 0) {
                    indisValue.append(',');
                }
                indisValue.append(value);
            }

            writeUserParam(streamWriter, "indistinguishable_proteins_" + idx, STRING_TYPE, indisValue.toString(),
                    null, null, null);
        }

        streamWriter.writeEndElement(); // ProteinIdentification

        // ---- Peptide Identifications ----

        List<String> scoreShortList = piaModeller.getPSMModeller().getScoreShortNames(fileID);

        String mainScore = null;
        String mainScoreShort = null;
        Boolean mainScoreHigherBetter = false;
        if (proteinLevel || (fileID < 1)) {
            if (proteinLevel) {
                AbstractProteinInference protInference = piaModeller.getProteinModeller().getAppliedProteinInference();
                for (Setting setting : protInference.getScoring().getSettings()) {
                    if (setting.getShortName().equals(AbstractScoring.scoringSettingID)) {
                        mainScoreShort = setting.getValue();
                        mainScore = piaModeller.getPSMModeller().getScoreName(mainScoreShort);
                        mainScoreHigherBetter = piaModeller.getPSMModeller().getHigherScoreBetter(mainScoreShort);
                    }
                }

            } else {
                if (piaModeller.getPSMModeller().isCombinedFDRScoreCalculated()) {
                    mainScore = ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getName();
                    mainScoreShort = ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName();
                    mainScoreHigherBetter = false;
                }
            }
        } else {
            // get the main score of the exported file or overview
            if (piaModeller.getPSMModeller().isFDRCalculated(fileID)) {
                // if the FDR is calculated, use PSM_LEVEL_FDR_SCORE
                mainScore = ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getName();
                mainScoreShort = ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName();
                mainScoreHigherBetter = false;
            } else {
                for (String scoreShort : scoreShortList) {
                    ScoreModelEnum scoreModel = ScoreModelEnum.getModelByDescription(scoreShort);
                    if ((mainScore == null) ||
                            ((scoreModel != null) && scoreModel.isSearchengineMainScore())) {
                        // use the mainScore of the searchengine, or the first one in the list
                        mainScore = scoreModel.equals(ScoreModelEnum.UNKNOWN_SCORE) ? scoreShort : scoreModel.getName();
                        mainScoreShort = scoreShort;
                        mainScoreHigherBetter = scoreModel.higherScoreBetter();
                    }
                }
            }
        }

        peptideHitIter = peptideIdentifications.values().iterator();
        while (peptideHitIter.hasNext()) {
            List<PSMReportItem> peptideHits = peptideHitIter.next();

            streamWriter.writeStartElement("PeptideIdentification");

            streamWriter.writeAttribute("score_type", mainScore);
            streamWriter.writeAttribute("higher_score_better", (mainScoreHigherBetter != null) ? mainScoreHigherBetter.toString() : null);
            // TODO: the significance_threshold might be given by a filter
            streamWriter.writeAttribute("significance_threshold", "0");


            ListIterator<PSMReportItem> psmIter = peptideHits.listIterator();

            while (psmIter.hasNext()) {
                PSMReportItem psm = psmIter.next();

                if (psmIter.previousIndex() == 0) {
                    // this is the first, add RT and MZ to PeptideIdentification
                    streamWriter.writeAttribute("MZ", Double.toString(psm.getMassToCharge()));
                    Double rt = psm.getRetentionTime();
                    if (rt != null) {
                        streamWriter.writeAttribute("RT", rt.toString());
                    }
                }

                streamWriter.writeStartElement("PeptideHit");

                if (mainScoreShort != null) {
                    streamWriter.writeAttribute("score", psm.getScore(mainScoreShort).toString());
                }

                streamWriter.writeAttribute("sequence",
                        exportSequenceWithModifications(psm.getSequence(), psm.getModifications()));
                streamWriter.writeAttribute("charge", Integer.toString(psm.getCharge()));
                // TODO: add aa_before and aa_after

                StringBuilder sbProteinRefs = new StringBuilder();
                psm.getAccessions().stream().filter(acc -> accessionToPH.containsKey(acc.getAccession())).forEach(acc -> {
                    sbProteinRefs.append(accessionToPH.get(acc.getAccession()));
                    sbProteinRefs.append(" ");
                });
                if (sbProteinRefs.length() > 0) {
                    streamWriter.writeAttribute("protein_refs", sbProteinRefs.toString().trim());
                }

                // if there is decoy information, write it out
                boolean writeDecoyInfo = false;
                if (fileID > 0) {
                    if (piaModeller.getPSMModeller().getFileHasInternalDecoy(fileID) || // file has internal decoy info
                            piaModeller.getPSMModeller().isFDRCalculated(fileID)) {     // FDR is calculated for the file
                        writeDecoyInfo = true;
                    }
                } else {
                    if (piaModeller.getPSMModeller().isCombinedFDRScoreCalculated()) {
                        writeDecoyInfo = true;
                    }
                }
                if (writeDecoyInfo) {
                    writeUserParam(streamWriter, "target_decoy", STRING_TYPE,
                            psm.getIsDecoy() ? "decoy" : "target", null, null, null);
                }

                // write additional scores
                for (String scoreShort : scoreShortList) {
                    if (scoreShort.equals(mainScoreShort)) {
                        continue;
                    }

                    String scoreName = piaModeller.getPSMModeller().getScoreName(scoreShort);
                    writeUserParam(streamWriter, scoreName, "float", psm.getScore(scoreShort).toString(),
                            null, null, null);
                }

                streamWriter.writeEndElement();
            }

            streamWriter.writeEndElement(); // PeptideIdentification
        }


        LOGGER.debug("peptides: " + peptideIdentifications.size());

        streamWriter.writeEndElement(); // IdentificationRun
    }


    /**
     * Returns the name or location of the used searchdatabase for th egiven
     * file.
     *
     * @param fileID
     * @return
     */
    private String getSearchDatabase(Long fileID) {
        String db = null;
        try {
            String sdbRef = piaModeller.getFiles().get(fileID).getAnalysisCollection().getSpectrumIdentification().get(0).getSearchDatabaseRef().get(0).getSearchDatabaseRef();
            db = piaModeller.getSearchDatabases().get(sdbRef).getLocation();
            if (db == null) {
                db = piaModeller.getSearchDatabases().get(sdbRef).getName();
            }
        } catch (NullPointerException e) {
            LOGGER.warn("could not get searchDatabase for file " + fileID, e);
            db = null;
        }

        if (db == null) {
            db = "";
        } else if (db.startsWith("file:")) {
            URI dbUri;
            try {
                dbUri = new URI(db);
                db = dbUri.getPath();
            } catch (URISyntaxException e) {
                LOGGER.warn("could not get searchDatabase for file " + fileID, e);
            }
        }

        return db;
    }


    /**
     * Returns the mass type (monoisotopic or average) for the given file.
     *
     * @param fileID
     * @return
     */
    private MassType getMassType(Long fileID) {
        for (CvParam cvParam : piaModeller.getFiles().get(fileID).getAnalysisProtocolCollection().getSpectrumIdentificationProtocol().get(0).getAdditionalSearchParams().getCvParam()) {
            if ("MS:1001255".equals(cvParam.getAccession()) ||          // name: fragment mass type average
                    "MS:1001212".equals(cvParam.getAccession())) {      // name: parent mass type average
                return MassType.AVERAGE;
            }
        }

        // default is monoisotopic
        return MassType.MONOISOTOPIC;
    }


    /**
     * Returns the enzyme for the given file.
     * @param fileID
     * @return
     */
    private DigestionEnzyme getDigestionEnzyme(Long fileID) {
        try {
            Enzyme enzyme = piaModeller.getFiles().get(fileID).getAnalysisProtocolCollection().getSpectrumIdentificationProtocol().get(0).getEnzymes().getEnzyme().get(0);

            CleavageAgent agent = CleavageAgent.getBySiteRegexp(enzyme.getSiteRegexp());
            if (agent == null) {
                agent = CleavageAgent.getByName(enzyme.getName());
            }

            return DigestionEnzyme.getFromCleavageAgent(agent);
        } catch (NullPointerException e) {
            LOGGER.warn("Problem getting enzyme for " + fileID, e);
        }

        return DigestionEnzyme.UNKNOWN_ENZYME;
    }


    /**
     * sorts the PSMs by their spectra to use them for the export to idXML'x
     * PeptideIdentifications.
     *
     * @param proteinLevel
     * @param fileID
     * @param filters
     * @return
     */
    private Map<String, List<PSMReportItem>> getPSMsForPeptideIdentifications(
            boolean proteinLevel, Long fileID, List<AbstractFilter> filters) {
        Map<String, List<PSMReportItem>> peptideIdentifications = new HashMap<>();

        Map<String, Boolean> psmSetSettings = piaModeller.getPSMModeller().getPSMSetSettings();

        ListIterator<?> psmIter;
        if (proteinLevel || (fileID < 1)) {
            psmIter = piaModeller.getPSMModeller().getFilteredReportPSMSets(filters).listIterator();
        } else {
            psmIter = piaModeller.getPSMModeller().getFilteredReportPSMs(fileID, filters).listIterator();
        }

        while (psmIter.hasNext()) {
            PSMReportItem psm = (PSMReportItem)psmIter.next();

            // get the identifier of the spectrum (NOT the PSM)
            String spectrumIdKey = null;
            if (psm instanceof ReportPSM) {
                spectrumIdKey = ((ReportPSM) psm).getSpectrum().getSpectrumIdentificationKey(psmSetSettings);
            } else if (psm instanceof ReportPSMSet) {
                spectrumIdKey = ((ReportPSMSet) psm).getPSMs().get(0).getSpectrum().getSpectrumIdentificationKey(psmSetSettings);
            }

            if (!peptideIdentifications.containsKey(spectrumIdKey)) {
                peptideIdentifications.put(spectrumIdKey, new ArrayList<>());
            }

            peptideIdentifications.get(spectrumIdKey).add(psm);
        }

        return peptideIdentifications;
    }


    /**
     * Writes a UserParam in the idXML file.
     *
     * @param streamWriter
     * @param name
     * @param type
     * @param value
     * @param unitAccession
     * @param unitCvRef
     * @param unitName
     * @throws XMLStreamException
     */
    private static void writeUserParam(XMLStreamWriter streamWriter, String name, String type, String value,
            String unitAccession, String unitCvRef, String unitName) throws XMLStreamException {
        streamWriter.writeStartElement("UserParam");

        streamWriter.writeAttribute("type", type);
        streamWriter.writeAttribute("name", name);
        streamWriter.writeAttribute("value", value);

        if (unitAccession != null) {
            streamWriter.writeAttribute("unitAccession", unitAccession);
        }
        if (unitCvRef != null) {
            streamWriter.writeAttribute("unitCvRef", unitCvRef);
        }
        if (unitName != null) {
            streamWriter.writeAttribute("unitName", unitName);
        }
        streamWriter.writeEndElement();
    }


    /**
     * Writes the given accessions as a ProteinIdentification to the idXML file.
     * All given accessions are processed equally, they should be an idXML's
     * "indistinguishable_protein_". Only writes the ProteinIdentification, if
     * it is not yet in the accessionToPH mapping.
     *
     * @param streamWriter
     * @param accessionsList the accessions, which should be written to the idXML
     * @param score score of the protein inference
     * @param qvalue if calculated, the q-value (or null, if not calculated)
     * @param fileID
     * @param accessionToPH mapping from PIA accession to idXML's "PH_XXX", will be filled
     * @return
     * @throws XMLStreamException
     */
    private static List<String> writeAccessionsToXML(XMLStreamWriter streamWriter, List<Accession> accessionsList,
            Double score, Double qvalue, Long fileID, Map<String, String> accessionToPH)
            throws XMLStreamException {
        ListIterator<Accession> accIt = accessionsList.listIterator();
        List<String> writtenPHs = new ArrayList<>(accessionsList.size());
        while (accIt.hasNext()) {
            Accession acc = accIt.next();
            String accStr = acc.getAccession();

            if (!accessionToPH.containsKey(accStr)) {
                String proteinHitID = "PH_" + accessionToPH.size();
                accessionToPH.put(accStr, proteinHitID);

                streamWriter.writeStartElement("ProteinHit");

                streamWriter.writeAttribute("id", proteinHitID);
                streamWriter.writeAttribute("accession", accStr);

                streamWriter.writeAttribute("score", score.toString());

                if ((acc.getDbSequence() != null) && (acc.getDbSequence().trim().length() > 0)) {
                    streamWriter.writeAttribute("sequence", acc.getDbSequence());
                }
                String description = acc.getDescription(fileID);
                if (description != null) {
                    writeUserParam(streamWriter, "Description", STRING_TYPE, description,
                            null, null, null);
                }

                if (qvalue != null) {
                    writeUserParam(streamWriter, "q-value_score", "float", qvalue.toString(),
                            null, null, null);
                }

                // TODO: add decoy information, if it was set on the protein level

                streamWriter.writeEndElement();

                writtenPHs.add(proteinHitID);
            }
        }

        return writtenPHs;
    }


    /**
     * Exports the sequence together with the modifications in the OpenMS style.
     *
     * @param sequence
     * @param modifications
     * @return
     */
    public static String exportSequenceWithModifications(String sequence, Map<Integer, Modification> modifications) {
        StringBuilder modSequence = new StringBuilder(sequence.length());
        int lastPos = 0;

        for (Map.Entry<Integer, Modification> modIt : modifications.entrySet()) {
            int pos = modIt.getKey();

            // first add the unmodified residues from last to here
            if (pos - lastPos >= 1) {
                modSequence.append(sequence.substring(lastPos, pos));
            }

            modSequence.append('(');
            if ((modIt.getValue().getDescription() != null) && (modIt.getValue().getDescription().trim().length() > 0)) {
                modSequence.append(modIt.getValue().getDescription());
            } else {
                modSequence.append(modIt.getValue().getMassString());
            }
            modSequence.append(')');

            lastPos = pos;
        }

        // add the remaining residues
        if (lastPos <= sequence.length()) {
            modSequence.append(sequence.substring(lastPos));
        }

        return modSequence.toString();
    }
}
