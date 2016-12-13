package de.mpc.pia.modeller.exporter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.AccessionOccurrence;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.modeller.IdentificationKeySettings;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.inference.AbstractProteinInference;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.tools.CleavageAgent;
import de.mpc.pia.tools.MzIdentMLTools;
import de.mpc.pia.tools.OntologyConstants;
import de.mpc.pia.tools.PIAConstants;
import de.mpc.pia.tools.unimod.UnimodParser;
import de.mpc.pia.tools.unimod.jaxb.ModT;
import uk.ac.ebi.jmzidml.model.mzidml.AbstractParam;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisCollection;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisProtocolCollection;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftwareList;
import uk.ac.ebi.jmzidml.model.mzidml.CvList;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.DBSequence;
import uk.ac.ebi.jmzidml.model.mzidml.FileFormat;
import uk.ac.ebi.jmzidml.model.mzidml.InputSpectra;
import uk.ac.ebi.jmzidml.model.mzidml.InputSpectrumIdentifications;
import uk.ac.ebi.jmzidml.model.mzidml.Inputs;
import uk.ac.ebi.jmzidml.model.mzidml.Param;
import uk.ac.ebi.jmzidml.model.mzidml.ParamList;
import uk.ac.ebi.jmzidml.model.mzidml.Peptide;
import uk.ac.ebi.jmzidml.model.mzidml.PeptideEvidence;
import uk.ac.ebi.jmzidml.model.mzidml.PeptideEvidenceRef;
import uk.ac.ebi.jmzidml.model.mzidml.PeptideHypothesis;
import uk.ac.ebi.jmzidml.model.mzidml.ProteinAmbiguityGroup;
import uk.ac.ebi.jmzidml.model.mzidml.ProteinDetection;
import uk.ac.ebi.jmzidml.model.mzidml.ProteinDetectionHypothesis;
import uk.ac.ebi.jmzidml.model.mzidml.ProteinDetectionList;
import uk.ac.ebi.jmzidml.model.mzidml.ProteinDetectionProtocol;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabase;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabaseRef;
import uk.ac.ebi.jmzidml.model.mzidml.SequenceCollection;
import uk.ac.ebi.jmzidml.model.mzidml.SourceFile;
import uk.ac.ebi.jmzidml.model.mzidml.SpectraData;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationItem;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationItemRef;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationList;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationProtocol;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationResult;
import uk.ac.ebi.jmzidml.model.mzidml.UserParam;
import uk.ac.ebi.jmzidml.xml.io.MzIdentMLMarshaller;

public class MzIdentMLExporter {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(MzIdentMLExporter.class);

    /** the modeller, that should be exported */
    private PIAModeller piaModeller;

    /** the writer used to export the mzTab file */
    private BufferedWriter outWriter;

    /** used unimod parser */
    private UnimodParser unimodParser;

    /** marshaller to write out mzIdentML */
    private MzIdentMLMarshaller mzidMarshaller;

    /** the fileID for the export */
    private Long exportFileID;

    /** PIA as the analysis software */
    private AnalysisSoftware piaAnalysisSoftware;

    /** the exported {@link Inputs} section */
    private Inputs inputs;

    /** the exported {@link AnalysisProtocolCollection} section */
    private AnalysisProtocolCollection analysisProtocolCollection;

    /** the exported {@link AnalysisCollection} section */
    private AnalysisCollection analysisCollection;

    /** the actually exported {@link SpectrumIdentificationList} */
    private SpectrumIdentificationList siList;

    /** the actually exported {@link ProteinDetectionList} */
    private ProteinDetectionList pdList;

    /** the exported {@link DBSequence}s */
    private Map<String, DBSequence> sequenceMap;

    /** the exported {@link Peptide}s */
    private Map<String, Peptide> peptideMap;

    /** the exported {@link PeptideEvidence}s */
    private Map<String, PeptideEvidence> pepEvidenceMap;

    /** the exported {@link SpectrumIdentificationResult}s */
    private Map<String, SpectrumIdentificationResult> sirMap;


    /** prefix for a protein group in the mzIdentML */
    private static final String PROTEIN_AMBIGUITY_GROUP_PREFIX = "PAG_";

    /** prefix for a protein detection hypothesis in the mzIdentML */
    private static final String PROTEIN_DETECTION_HYPOTHESIS_PREFIX = "PDH_";

    /** prefix for a peptide in the mzIdentML */
    private static final String PEPTIDE_PREFIX = "PEP_";

    /** prefix for a peptide evidence in the mzIdentML */
    private static final String PEPTIDE_EVIDENCE_PREFIX = "PE_";

    /** prefix for a DBSequence in the mzIdentML */
    private static final String DBSEQUENCE_PREFIX = "DBSeq_";

    /** the PSMSetSettings for the {@link SpectrumIdentificationResult}s in mzIdentML export */
    private static final Map<String, Boolean> SPECTRUM_IDENTIFICATION_RESULT_PSM_SET_SETTINGS;


    /**
     * static initializations
     */
    static {
        SPECTRUM_IDENTIFICATION_RESULT_PSM_SET_SETTINGS = new HashMap<>();
        SPECTRUM_IDENTIFICATION_RESULT_PSM_SET_SETTINGS.put(IdentificationKeySettings.FILE_ID.toString(), true);
        SPECTRUM_IDENTIFICATION_RESULT_PSM_SET_SETTINGS.put(IdentificationKeySettings.CHARGE.toString(), true);
        SPECTRUM_IDENTIFICATION_RESULT_PSM_SET_SETTINGS.put(IdentificationKeySettings.RETENTION_TIME.toString(), true);
        SPECTRUM_IDENTIFICATION_RESULT_PSM_SET_SETTINGS.put(IdentificationKeySettings.MASSTOCHARGE.toString(), true);
        SPECTRUM_IDENTIFICATION_RESULT_PSM_SET_SETTINGS.put(IdentificationKeySettings.SOURCE_ID.toString(), true);
    }


    /**
     * Basic constructor to export the
     * @param modeller
     */
    public MzIdentMLExporter(PIAModeller modeller) {
        this.piaModeller = modeller;
        this.unimodParser = null;
    }


    public boolean exportToMzIdentML(Long fileID, File exportFile,
            boolean proteinLevel, boolean filterExport) {
        try {
            FileOutputStream fos;
            fos = new FileOutputStream(exportFile);
            return exportToMzIdentML(fileID, fos, proteinLevel, filterExport);
        } catch (IOException ex) {
            LOGGER.error("Error writing  mzIdentML to " + exportFile.getAbsolutePath(), ex);
            return false;
        }
    }


    public boolean exportToMzIdentML(Long fileID, String exportFileName,
            boolean proteinLevel, boolean filterExport) {
        File piaFile = new File(exportFileName);
        return exportToMzIdentML(fileID, piaFile, proteinLevel, filterExport);
    }


    public boolean exportToMzIdentML(Long fileID, OutputStream exportStream,
            boolean proteinLevel, boolean filterExport) {
        boolean exportOK;
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(exportStream));
            exportOK = exportToMzIdentML(fileID, writer, proteinLevel, filterExport);
            writer.close();
        } catch (IOException e) {
            LOGGER.error("Error while exporting to mzIdentML", e);
            exportOK = false;
        }
        return exportOK;
    }


    /**
     * Exports the data of the modeller using the data of the fileID (only
     * relevant if not protein level) to the specified file. If protein level is
     * selected as well, also this will be exported (and accordingly the PSMs of
     * all merged files).
     *
     * @param filterExport whether the export should be filtered (on any level)
     * @return
     */
    public boolean exportToMzIdentML(Long fileID, Writer exportWriter,
            boolean proteinLevel, boolean filterExport) {
        boolean error = false;
        exportFileID = fileID;
        boolean exportProteinLevel = proteinLevel;

        LOGGER.info("start writing mzIdentML file");

        outWriter = new BufferedWriter(exportWriter);
        unimodParser = new UnimodParser();
        mzidMarshaller = new MzIdentMLMarshaller();

        piaAnalysisSoftware = MzIdentMLTools.getPIAAnalysisSoftware();

        try {
            // XML header
            // TODO: set the version of mzIdentML here
            outWriter.write(mzidMarshaller.createXmlHeader() + "\n");
            outWriter.write(mzidMarshaller.createMzIdentMLStartTag("PIAExport for PSMs") + "\n");

            // there are some variables needed for additional tags later
            analysisCollection = new AnalysisCollection();
            analysisProtocolCollection = new AnalysisProtocolCollection();

            sequenceMap = new HashMap<>();
            peptideMap = new HashMap<>();
            pepEvidenceMap = new HashMap<>();

            if (exportProteinLevel
                    && (piaModeller.getProteinModeller().getAppliedProteinInference() == null)) {
                exportProteinLevel = false;
                LOGGER.error("No protein inference was set, cannot export protein level!");
            }

            if (exportProteinLevel && (exportFileID != 0L)) {
                exportFileID = 0L;
                LOGGER.warn("The exported file for protein level information is always 0, will be set automatically.");
            }

            // write common tags
            writeCommonMzIdentMLTags(filterExport, exportProteinLevel);

            // create analysis collection and protocol
            createAnalysisCollectionAndAnalysisProtocolCollection(filterExport);

            // create protein specific tags
            if (exportProteinLevel) {
                createProteinLevelInformation(filterExport);
            }

            // refine some elements, like checking correct URIs
            refineElements();

            // now write out the mzIdentML tags
            mzidMarshaller.marshal(analysisCollection, outWriter);
            outWriter.write("\n");

            mzidMarshaller.marshal(analysisProtocolCollection, outWriter);
            outWriter.write("\n");

            outWriter.write(mzidMarshaller.createDataCollectionStartTag() + "\n");

            mzidMarshaller.marshal(inputs, outWriter);
            outWriter.write("\n");


            outWriter.write(mzidMarshaller.createAnalysisDataStartTag() + "\n");

            // write out the spectrum identification lists
            mzidMarshaller.marshal(siList, outWriter);
            outWriter.write("\n");

            // write out the protein detection list
            if (exportProteinLevel) {
                mzidMarshaller.marshal(pdList, outWriter);
                outWriter.write("\n");
            }

            outWriter.write(mzidMarshaller.createAnalysisDataClosingTag() + "\n");

            outWriter.write(mzidMarshaller.createDataCollectionClosingTag() + "\n");


            outWriter.write(mzidMarshaller.createMzIdentMLClosingTag());

            outWriter.flush();
            outWriter.close();
            LOGGER.info("writing of mzIdentML done");
        } catch (IOException e) {
            LOGGER.error("Error writing mzIdentML file", e);
        } finally {
            try {
                outWriter.close();
            } catch (IOException e) {
                LOGGER.error("Could not close the file while writing mzIdentML", e);
                error = true;
            }
        }

        return !error;
    }


    /**
     * Writes (and creates) the MzIdentML tags which are common for an export
     * with and without the ProteinDetectionList. This includes the
     * {@link SpectrumIdentificationList}.
     * <p>
     * All given parameters need only to be created with "new", they are filled
     * in this procedure.
     * <p>
     * The mzIdentML file will be written up to the {@link SequenceCollection}.
     *
     * @throws IOException
     */
    private void writeCommonMzIdentMLTags(Boolean filterPSMs, Boolean forProteinExport)
            throws IOException {
        // the CV list
        CvList cvList = new CvList();

        cvList.getCv().add(MzIdentMLTools.getCvPSIMS());
        cvList.getCv().add(UnimodParser.getCv());
        cvList.getCv().add(MzIdentMLTools.getUnitOntology());

        mzidMarshaller.marshal(cvList, outWriter);
        outWriter.write("\n");

        // AnalysisSoftware
        AnalysisSoftwareList analysisSoftwareList = new AnalysisSoftwareList();

        analysisSoftwareList.getAnalysisSoftware().add(piaAnalysisSoftware);

        for (AnalysisSoftware software : piaModeller.getAnalysisSoftwares().values()) {
            analysisSoftwareList.getAnalysisSoftware().add(software);
        }

        mzidMarshaller.marshal(analysisSoftwareList, outWriter);
        outWriter.write("\n");

        // create Inputs element
        createInputs();

        // get the input files for this exportFileID (either a single one or all)
        List<PIAInputFile> fileList = getExportedInputFiles();

        // maps from the searchDB to the PIAInputFiles, which use it
        Map<String, Set<Long>> dbsInFiles = getDatabasesInFiles(fileList);

        // get the PSMReportItems for the file, but do not filter for protein export
        List<PSMReportItem> psmItems = getExportFilesPSMItems(filterPSMs && !forProteinExport);

        // TODO: get the "representative score" for score ranking (in SIR)
        String rankScoreShort = null;

        // create the SpectrumIdentificationList
        createSpectrumIdentificationList(psmItems, dbsInFiles, rankScoreShort,
                filterPSMs && !forProteinExport);

        SequenceCollection sequenceCollection = new SequenceCollection();

        // add the DBSequences into their list
        for (DBSequence dbSequence : sequenceMap.values()) {
            sequenceCollection.getDBSequence().add(dbSequence);
        }

        // add the peptides in their list
        for (Peptide peptide : peptideMap.values()) {
            sequenceCollection.getPeptide().add(peptide);
        }

        // add the peptideEvidences into their list
        for (PeptideEvidence pe : pepEvidenceMap.values()) {
            sequenceCollection.getPeptideEvidence().add(pe);
        }

        mzidMarshaller.marshal(sequenceCollection, outWriter);
        outWriter.write("\n");
    }


    /**
     * Creates and populates the {@link Inputs} element for the exported file
     */
    private void createInputs() {
        inputs = new Inputs();

        List<SearchDatabase> databases = new ArrayList<>();
        List<SpectraData> spectraData = new ArrayList<>();

        if (exportFileID > 0) {
            // get spectrumIdentification of the input file
            SpectrumIdentification specID = piaModeller.getFiles().get(exportFileID)
                    .getAnalysisCollection().getSpectrumIdentification().get(0);

            // add the spectraData of the input file
            spectraData.addAll(specID.getInputSpectra().stream().map(inputSpectra -> piaModeller.getSpectraData().get(inputSpectra.getSpectraDataRef())).collect(Collectors.toList()));

            // add the search databases of the input file
            databases.addAll(specID.getSearchDatabaseRef().stream().map(dbRef -> piaModeller.getSearchDatabases().get(dbRef.getSearchDatabaseRef())).collect(Collectors.toList()));
        } else {
            // add all SpectraData and SearchDatabase
            spectraData.addAll(piaModeller.getSpectraData().values());
            databases.addAll(piaModeller.getSearchDatabases().values());
        }

        inputs.getSpectraData().addAll(spectraData);
        inputs.getSearchDatabase().addAll(databases);

        // add the PIA XML file as additional SourceFile
        SourceFile sourceFile = new SourceFile();
        sourceFile.setId("sf_pia_xml");
        sourceFile.setLocation(piaModeller.getFileName());
        sourceFile.setName("PIA-XML-file");
        sourceFile.setExternalFormatDocumentation(PIAConstants.PIA_REPOSITORY_LOCATION);
        FileFormat fileFormat = new FileFormat();
        fileFormat.setCvParam(MzIdentMLTools.createPSICvParam(OntologyConstants.PIA_XML_FORMAT, null));
        sourceFile.setFileFormat(fileFormat);
        inputs.getSourceFile().add(sourceFile);
    }



    /**
     * Getter for the {@link PIAInputFile}s, that are
     * @return
     */
    private List<PIAInputFile> getExportedInputFiles() {
        List<Long> fileIDs;

        if (exportFileID > 0) {
            fileIDs = new ArrayList<>();
            fileIDs.add(exportFileID);
        } else {
            fileIDs = new ArrayList<>(piaModeller.getFiles().keySet());
        }

        List<PIAInputFile> fileList = new ArrayList<>(fileIDs.size());
        fileList.addAll(fileIDs.stream().filter(fileID -> piaModeller.getFiles().containsKey(fileID)).map(fileID -> piaModeller.getFiles().get(fileID)).collect(Collectors.toList()));

        return fileList;
    }


    /**
     * Creates a mapping from the searchDB to the PIAInputFiles (their IDs),
     * which use it
     *
     * @param fileList
     * @return
     */
    private Map<String, Set<Long>> getDatabasesInFiles(List<PIAInputFile> fileList) {
        Map<Long, List<SpectrumIdentification>> filesSpecIDs =
                new HashMap<>(fileList.size());

        for (PIAInputFile file : fileList) {
            AnalysisCollection ac = file.getAnalysisCollection();
            if (ac != null) {
                List<SpectrumIdentification> specIDs = filesSpecIDs.get(file.getID());
                if (specIDs == null) {
                    specIDs = new ArrayList<>();
                    filesSpecIDs.put(file.getID(), specIDs);
                }

                specIDs.addAll(ac.getSpectrumIdentification());
            }
        }

        Map<String, Set<Long>> dbsInFiles = new HashMap<>();

        for (Map.Entry<Long, List<SpectrumIdentification>> specIDsIt : filesSpecIDs.entrySet()) {
            for (SpectrumIdentification specID : specIDsIt.getValue()) {
                for (SearchDatabaseRef ref : specID.getSearchDatabaseRef()) {
                    Set<Long> files = dbsInFiles.get(ref.getSearchDatabaseRef());
                    if (files == null) {
                        files = new HashSet<>();
                        dbsInFiles.put(ref.getSearchDatabaseRef(), files);
                    }
                    files.add(specIDsIt.getKey());
                }

                if ((exportFileID > 0)
                        || (piaModeller.getFiles().size() == 1)) {
                    // export for one single input protocol only
                    analysisCollection.getSpectrumIdentification().add(specID);

                    analysisProtocolCollection.getSpectrumIdentificationProtocol().add(
                            specID.getSpectrumIdentificationProtocol());
                }
            }
        }

        return dbsInFiles;
    }


    /**
     * Create the {@link SpectrumIdentificationList} for the export.
     *
     * @param psmItems
     * @param dbsInFiles
     * @param filterPSMs
     */
    private void createSpectrumIdentificationList(
            List<PSMReportItem> psmItems,
            Map<String, Set<Long>> dbsInFiles,
            String rankScoreShort,
            Boolean filterPSMs) {

        siList = new SpectrumIdentificationList();

        siList.setId("spectrum_identification_list");

        sirMap = new HashMap<>();

        // each PSM is one SpectrumIdentificationItem, iterate over the PSMs
        for (PSMReportItem psm : psmItems) {

            // first build or get the peptide of the PSM
            String pepId = psm.getPeptideStringID(true);
            Peptide peptide = peptideMap.get(pepId);
            if (peptide == null) {
                peptide = createPeptide(psm, PEPTIDE_PREFIX + pepId);
                peptideMap.put(pepId, peptide);
            }

            // then build the peptide evidences
            for (Accession accession : psm.getAccessions()) {
                putIntoPeptideEvidenceMap(accession, peptide, pepId, psm.getIsDecoy(),
                        psm.getPeptide().getAccessionOccurrences(), dbsInFiles);
            }


            putPsmInSpectrumIdentificationResultMap(psm, pepId, rankScoreShort, filterPSMs);
        }

        siList.getSpectrumIdentificationResult().addAll(sirMap.values());
    }


    /**
     * Getter for the {@link PSMReportItem}s of the exported file. If filterPSMs
     * is true, the PSMs are filtered by the filters given for the file.
     * Therefore, this should be false for protein level exports.
     *
     * @param filterPSMs
     * @return
     */
    private List<PSMReportItem> getExportFilesPSMItems(Boolean filterPSMs) {
        List<PSMReportItem> psmItems;
        List<AbstractFilter> filters = null;

        if (filterPSMs) {
            filters = piaModeller.getPSMModeller().getFilters(exportFileID);
        }

        if (exportFileID == 0) {
            psmItems = new ArrayList<>(
                    piaModeller.getPSMModeller().getFilteredReportPSMSets(filters));
        } else {

            psmItems = new ArrayList<>(
                    piaModeller.getPSMModeller().getFilteredReportPSMs(exportFileID, filters));
        }

        return psmItems;
    }


    /**
     * Creates the peptide for mzIdentML connected to the given PSM.
     *
     * @param psm
     * @param peptideId
     * @return
     */
    private Peptide createPeptide(PSMReportItem psm, String peptideId) {
        Peptide peptide = new Peptide();
        peptide.setId(peptideId);

        peptide.setPeptideSequence(psm.getSequence());

        for (Map.Entry<Integer, Modification> modIt
                : psm.getModifications().entrySet()) {
            uk.ac.ebi.jmzidml.model.mzidml.Modification mod;

            ModT uniMod = unimodParser.getModification(
                    modIt.getValue().getAccession(),
                    modIt.getValue().getDescription(),
                    modIt.getValue().getMass(),
                    modIt.getValue().getResidue().toString());

            if (uniMod != null) {
                mod = unimodParser.createModification(uniMod,
                        modIt.getKey(),
                        modIt.getValue().getResidue().toString());
            } else {
                // build an "unknown modification"
                mod = new uk.ac.ebi.jmzidml.model.mzidml.Modification();
                mod.getResidues().add(
                        modIt.getValue().getResidue().toString());
                mod.setLocation(modIt.getKey());

                mod.getCvParam().add(
                        MzIdentMLTools.createPSICvParam(OntologyConstants.UNKNOWN_MODIFICATION, null));
            }

            if (mod.getResidues().contains(".")) {
                // this is an N- or C-terminal modification -> no residue needed
                mod.getResidues().clear();
            }

            peptide.getModification().add(mod);

            // TODO: handle SubstitutionModifications
        }

        return peptide;
    }


    /**
     * Creates a {@link PeptideEvidence} for the given accession and PSM.
     *
     * @param accession
     * @param peptide
     * @param peptideId
     * @param isDecoy
     * @param accessionOccurrences
     * @param dbsInFiles
     */
    private void putIntoPeptideEvidenceMap(Accession accession, Peptide peptide,
            String peptideId, Boolean isDecoy, Set<AccessionOccurrence> accessionOccurrences,
            Map<String, Set<Long>> dbsInFiles) {
        boolean foundOccurrence = false;

        for (AccessionOccurrence occurrence : accessionOccurrences) {
            // look if occurrences are given in the compilation
            if (accession.getAccession().equals(occurrence.getAccession().getAccession())) {
                String evidenceID = createPeptideEvidenceID(
                        peptideId,
                        occurrence.getStart(),
                        occurrence.getEnd(),
                        accession);

                if (!pepEvidenceMap.containsKey(evidenceID)) {
                    PeptideEvidence pepEvi = createPeptideEvidence(evidenceID,
                            occurrence.getStart(),
                            occurrence.getEnd(),
                            isDecoy,
                            peptide,
                            accession,
                            dbsInFiles);

                    pepEvidenceMap.put(evidenceID, pepEvi);
                }
                foundOccurrence = true;
            }
        }

        if (!foundOccurrence) {
            // no occurrence given for this accessione, so create peptideEvidence without position
            String evidenceID = createPeptideEvidenceID(peptideId, null, null, accession);

            if (!pepEvidenceMap.containsKey(evidenceID)) {
                PeptideEvidence pepEvi = createPeptideEvidence(evidenceID,
                        null, null,
                        isDecoy,
                        peptide,
                        accession,
                        dbsInFiles);

                pepEvidenceMap.put(evidenceID, pepEvi);
            }
        }
    }


    /**
     * Creates a String containing the ID of a {@link PeptideEvidence} with the
     * given information.
     *
     * @param peptideStringID the peptideStringID containing the seqeunce and
     * modifications as in {@link PeptideSpectrumMatch#getPeptideStringID(boolean)}
     * @param start the start in the dbSequence (if known)
     * @param end the end in the dbSequence (if known)
     * @param accession the accession of the protein
     * @return
     */
    private static String createPeptideEvidenceID(String peptideStringID,
            Integer start, Integer end, Accession accession) {
        StringBuilder evidenceIDstr = new StringBuilder(PEPTIDE_EVIDENCE_PREFIX);
        evidenceIDstr.append(peptideStringID);
        if ((start != null) && (end != null)) {
            evidenceIDstr.append("-");
            evidenceIDstr.append(start);
            evidenceIDstr.append("-");
            evidenceIDstr.append(end);
        }
        evidenceIDstr.append("-");
        evidenceIDstr.append(accession.getAccession());

        return evidenceIDstr.toString();
    }


    /**
     * Creates a {@link PeptideEvidence} with the given parameters.
     *
     * @param evidenceID
     * @param start
     * @param end
     * @param isDecoy
     * @param peptide
     * @param accession
     * @param dbsInFiles
     * @return
     */
    private PeptideEvidence createPeptideEvidence(String evidenceID,
            Integer start, Integer end, Boolean isDecoy, Peptide peptide,
            Accession accession, Map<String, Set<Long>> dbsInFiles) {
        PeptideEvidence pepEvi = new PeptideEvidence();

        DBSequence dbSequence = sequenceMap.get(accession);
        if (dbSequence == null) {
            // create the dbSequence entry, if it is not yet created
            dbSequence = createDBSequence(accession, dbsInFiles);
        }
        pepEvi.setDBSequence(dbSequence);

        pepEvi.setId(evidenceID);

        pepEvi.setIsDecoy(isDecoy);
        pepEvi.setPeptide(peptide);
        if (start != null) {
            pepEvi.setStart(start);
        }
        if (end != null) {
            pepEvi.setEnd(end);
        }

        return pepEvi;
    }


    /**
     * Create the DBSEquence for the given Accession and put it into the
     * DBSequences Map.
     *
     * @param accession
     * @param dbsInFiles
     * @return
     */
    private DBSequence createDBSequence(Accession accession, Map<String, Set<Long>> dbsInFiles) {
        DBSequence dbSequence = new DBSequence();

        dbSequence.setAccession(accession.getAccession());
        dbSequence.setId(DBSEQUENCE_PREFIX + accession.getAccession());

        if ((accession.getDbSequence() != null) &&
                (accession.getDbSequence().length() > 0)) {
            dbSequence.setLength(accession.getDbSequence().length());
            dbSequence.setSeq(accession.getDbSequence());
        }

        CvParam descCvParam = getLongestDescription(accession, dbsInFiles);

        if (descCvParam.getValue() != null) {
            dbSequence.getCvParam().add(descCvParam);
        }

        // add info for the DBSequence
        dbSequence.getCvParam().add(MzIdentMLTools.createPSICvParam(OntologyConstants.AMINOACID_SEQUENCE, null));

        String dbRef = descCvParam.getUnitName();
        descCvParam.setUnitName(null);
        if (dbRef == null) {
            // no description found -> use any sequenceRef
            dbRef = accession.getSearchDatabaseRefs().iterator().next();
        }

        dbSequence.setSearchDatabase(piaModeller.getSearchDatabases().get(dbRef));

        sequenceMap.put(accession.getAccession(), dbSequence);

        return dbSequence;
    }


    /**
     * Gets the longest available description of the given Accession and codes
     * the file reference into the UnitName of the returned CvParam. Don't
     * forget to set this to null afterwards!
     *
     * @param accession
     * @param dbsInFiles
     * @return
     */
    private static CvParam getLongestDescription(Accession accession, Map<String, Set<Long>> dbsInFiles) {
        CvParam descCvParam = MzIdentMLTools.createPSICvParam(OntologyConstants.PROTEIN_DESCRIPTION, null);

        Map<Long, String> fileIdToDbRef = new HashMap<>();
        for (Map.Entry<String, Set<Long>>  dbsIt : dbsInFiles.entrySet()) {
            Long firstFileId = dbsIt.getValue().iterator().next();
            if (accession.getSearchDatabaseRefs().contains(dbsIt.getKey())) {
                fileIdToDbRef.put(firstFileId, dbsIt.getKey());
            }
        }

        // look for a good description
        // take this description and DBsequence_ref
        accession.getDescriptions().entrySet().stream().filter(descIt -> !descIt.getValue().trim().isEmpty()
                && fileIdToDbRef.containsKey(descIt.getKey())
                && ((descCvParam.getValue() == null)
                || (descIt.getValue().trim().length() > descCvParam.getValue().length()))).forEach(descIt -> {
            // take this description and DBsequence_ref
            descCvParam.setValue(descIt.getValue().trim());
            descCvParam.setUnitName(fileIdToDbRef.get(descIt.getKey()));
        });

        return descCvParam;
    }


    /**
     * Creates a {@link SpectrumIdentificationItem} for the given PSM and puts
     * it into its {@link SpectrumIdentificationResult}, which will be created
     * if necessary.
     *
     * @param psm
     * @param rankScoreShort
     * @param filterPSM
     * @return
     */
    private SpectrumIdentificationItem putPsmInSpectrumIdentificationResultMap(
            PSMReportItem psm,
            String peptideId,
            String rankScoreShort,
            Boolean filterPSM) {

        SpectrumIdentificationResult sir = createOrGetSIR(psm);

        String psmIdentificationKey = psm.getIdentificationKey(piaModeller.getPSMSetSettings());

        SpectrumIdentificationItem sii = sirContainsSII(sir, psmIdentificationKey);
        if (sii != null) {
            // the SII was already created -> return it
            return sii;
        }

        sii = new SpectrumIdentificationItem();
        sir.getSpectrumIdentificationItem().add(sii);
        sii.setId(psmIdentificationKey);

        sii.setChargeState(psm.getCharge());
        sii.setExperimentalMassToCharge(psm.getMassToCharge());

        if (filterPSM) {
            List<AbstractFilter> filters = piaModeller.getPSMModeller().getFilters(exportFileID);
            sii.setPassThreshold(FilterFactory.satisfiesFilterList(psm, exportFileID, filters));
        } else {
            // without filters, always true
            sii.setPassThreshold(true);
        }

        sii.setPeptide(peptideMap.get(peptideId));
        if ((rankScoreShort == null) || (psm instanceof ReportPSMSet)) {
            sii.setRank(0);
        } else {
            Integer rank = ((ReportPSM)psm).getIdentificationRank(rankScoreShort);
            if (rank != null) {
                sii.setRank(rank);
            } else {
                sii.setRank(0);
            }
        }

        // add the peptideEvidences to the SII
        for (Accession accession : psm.getAccessions()) {
            addPeptideEvidenceToSII(sii, accession, psm.getPeptide().getAccessionOccurrences(), peptideId);
        }

        addScoresToSII(sii, psm);

        addAdditionalInformationToSII(sii, psm);

        return sii;
    }


    /**
     * Creates the {@link SpectrumIdentificationResult} for the PSM or gets it
     * from the map, if it was already created before.
     *
     * @param psm
     * @return
     */
    private SpectrumIdentificationResult createOrGetSIR(PSMReportItem psm) {
        // build the SpectrumIdentificationItem into its result
        String psmIdentificationKey = PeptideSpectrumMatch.getIdentificationKey(
                SPECTRUM_IDENTIFICATION_RESULT_PSM_SET_SETTINGS,
                psm.getSequence(), psm.getModificationsString(),
                psm.getCharge(), psm.getMassToCharge(),
                psm.getRetentionTime(), psm.getSourceID(),
                psm.getSpectrumTitle(), exportFileID);

        SpectrumIdentificationResult specIdRes = sirMap.get(psmIdentificationKey);
        if (specIdRes == null) {
            // this spectrum has no identification yet
            specIdRes = new SpectrumIdentificationResult();

            specIdRes.setId(psmIdentificationKey);
            specIdRes.setSpectrumID(psm.getSourceID());
            specIdRes.setSpectraData(getRepresentingSpectraData(psm));


            if (psm.getSpectrumTitle() != null) {
               specIdRes.getCvParam().add(
                       MzIdentMLTools.createPSICvParam(OntologyConstants.SPECTRUM_TITLE,
                               psm.getSpectrumTitle()));
            }

            if (psm.getRetentionTime() != null) {
                CvParam tempCvParam = MzIdentMLTools.createPSICvParam(
                        OntologyConstants.SCAN_START_TIME, psm.getRetentionTime().toString());

                tempCvParam.setUnitCv(MzIdentMLTools.getUnitOntology());
                tempCvParam.setUnitName("second");
                tempCvParam.setUnitAccession("UO:0000010");

                specIdRes.getCvParam().add(tempCvParam);
            }

            sirMap.put(psmIdentificationKey, specIdRes);
        } else {
            // enhance the spectrum with the spectrumID, if available
            if ((specIdRes.getSpectrumID() == null)
                    && (psm.getSourceID() != null)) {
                specIdRes.setSpectrumID(psm.getSourceID());
            }

            // enhance with spectraData, if available
            if (specIdRes.getSpectraData() == null) {
                SpectraData specData = getRepresentingSpectraData(psm);
                if (specData != null) {
                    specIdRes.setSpectraData(specData);
                }
            }
        }

        return specIdRes;
    }


    /**
     * Checks whether the given {@link SpectrumIdentificationResult} contains
     * a {@link SpectrumIdentificationItem} with the given Id and returns it, if
     * it was found.
     *
     * @param sir
     * @return the SII with the given Id or null, if none is in the SIR
     */
    private static SpectrumIdentificationItem sirContainsSII(SpectrumIdentificationResult sir, String id) {
        SpectrumIdentificationItem sii = null;

        for (SpectrumIdentificationItem itemIt : sir.getSpectrumIdentificationItem()) {
            if (itemIt.getId().equals(id)) {
                // SII already created, return it
                sii = itemIt;
                break;
            }
        }

        return sii;
    }


    /**
     * Gets a representative of the {@link SpectraData} for the given
     * {@link PSMReportItem}.
     *
     * @param psm
     * @return
     */
    private SpectraData getRepresentingSpectraData(PSMReportItem psm) {
        List<ReportPSM> psmList = null;
        if (psm instanceof ReportPSM) {
            psmList = new ArrayList<>(1);
            psmList.add((ReportPSM)psm);
        } else if (psm instanceof ReportPSMSet) {
            psmList = ((ReportPSMSet) psm).getPSMs();
        }

        if(psmList != null){
            for (ReportPSM repPSM : psmList) {
                if ((repPSM.getSpectrum().getSpectrumIdentification().getInputSpectra() != null) &&
                        !repPSM.getSpectrum().getSpectrumIdentification().getInputSpectra().isEmpty()) {
                    SpectraData specData = piaModeller.getSpectraData().get(
                            repPSM.getSpectrum().getSpectrumIdentification().getInputSpectra().get(0).getSpectraDataRef());
                    // TODO: make the choice of spectrumID and spectraData more sophisticated
                    if (specData != null) {
                        return specData;
                    }
                }
            }
        }

        return null;
    }


    /**
     * Adds the {@link PeptideEvidence} (which was entered into the map before)
     * to the {@link SpectrumIdentificationItem}.
     *
     * @param sii
     * @param accession
     * @param accessionOccurrences
     * @param peptideId
     */
    private void addPeptideEvidenceToSII(SpectrumIdentificationItem sii, Accession accession,
            Set<AccessionOccurrence> accessionOccurrences, String peptideId) {
        boolean foundOccurrence = false;

        for (AccessionOccurrence occurrence : accessionOccurrences) {
            // look if occurrences are given in the compilation
            if (accession.getAccession().equals(occurrence.getAccession().getAccession())) {
                String evidenceID = createPeptideEvidenceID(
                        peptideId,
                        occurrence.getStart(), occurrence.getEnd(),
                        accession);

                PeptideEvidenceRef pepEvidenceRef = new PeptideEvidenceRef();
                pepEvidenceRef.setPeptideEvidence(pepEvidenceMap.get(evidenceID));

                sii.getPeptideEvidenceRef().add(pepEvidenceRef);
                foundOccurrence = true;
            }
        }

        if (!foundOccurrence) {
            // no occurrence given, so use peptideEvidence without position
            String evidenceID = createPeptideEvidenceID(peptideId, null, null, accession);

            PeptideEvidenceRef pepEvidenceRef = new PeptideEvidenceRef();
            pepEvidenceRef.setPeptideEvidence(pepEvidenceMap.get(evidenceID));

            sii.getPeptideEvidenceRef().add(pepEvidenceRef);
        }
    }


    /**
     * Adds the scores of the PSM to the SpectrumIdentificationItem.
     *
     * @param sii
     * @param psm
     */
    private static void addScoresToSII(SpectrumIdentificationItem sii, PSMReportItem psm) {
        List<ScoreModel> scoreModels;

        if (psm instanceof ReportPSM) {
            scoreModels = ((ReportPSM) psm).getScores();
        } else {
            Set<String> scoreShorts = new HashSet<>();
            for (ReportPSM psmIt : ((ReportPSMSet) psm).getPSMs()) {
                scoreShorts.addAll(psmIt.getScores().stream().map(ScoreModel::getShortName).collect(Collectors.toList()));
            }

            scoreModels = new ArrayList<>(scoreShorts.size() + 1);

            ScoreModel combinedFDRScore = psm.getFDRScore();
            if ((combinedFDRScore != null)
                    && !combinedFDRScore.getValue().equals(Double.NaN)) {
                // there is a valid Combined FDR Score to add
                scoreModels.add(combinedFDRScore);
            }

            scoreModels.addAll(scoreShorts.stream().map(((ReportPSMSet) psm)::getBestScoreModel).collect(Collectors.toList()));

        }

        for (ScoreModel score  : scoreModels) {
            if (!score.getType().equals(ScoreModelEnum.UNKNOWN_SCORE)) {
                CvParam tempCvParam = new CvParam();
                tempCvParam.setAccession(score.getAccession());
                tempCvParam.setCv(MzIdentMLTools.getCvPSIMS());
                tempCvParam.setName(score.getType().getCvName());
                tempCvParam.setValue(score.getValue().toString());

                sii.getCvParam().add(tempCvParam);
            } else {
                // TODO: add unknown scores...
                // TODO: check CV first... if not there, add as userParam
            }
        }
    }


    /**
     * Adds additional information of the PSM to the SpectrumIdentificationItem,
     * like retention time, spectrum title, delta m/z...
     *
     * @param sii
     * @param psm
     */
    private void addAdditionalInformationToSII(SpectrumIdentificationItem sii, PSMReportItem psm) {
        if ((psm instanceof ReportPSMSet) && piaModeller.getCreatePSMSets()) {
            // mark as consensus result
            sii.getCvParam().add(MzIdentMLTools.createPSICvParam(OntologyConstants.CONSENSUS_RESULT, null));
        }

        if (psm.getRetentionTime() != null) {
            CvParam tempCvParam = MzIdentMLTools.createPSICvParam(
                    OntologyConstants.SCAN_START_TIME, psm.getRetentionTime().toString());

            tempCvParam.setUnitCv(MzIdentMLTools.getUnitOntology());
            tempCvParam.setUnitName("second");
            tempCvParam.setUnitAccession("UO:0000010");

            sii.getCvParam().add(tempCvParam);
        }

        if (psm.getSpectrumTitle() != null) {
            sii.getCvParam().add(
                    MzIdentMLTools.createPSICvParam(OntologyConstants.SPECTRUM_TITLE, psm.getSpectrumTitle()));
        }

        if (!Double.isNaN(psm.getDeltaMass())) {
            double expMZ = sii.getExperimentalMassToCharge();
            double theoreticalMZ = expMZ - (psm.getDeltaMass() / psm.getCharge());

            sii.setCalculatedMassToCharge(theoreticalMZ);
        }

        if (((psm instanceof ReportPSM) || !piaModeller.getCreatePSMSets()) ||
                ((exportFileID == 0L) && (piaModeller.getFiles().size() == 1))) {
            ListIterator<AbstractParam> paramIt;

            if (psm instanceof ReportPSM) {
                paramIt = ((ReportPSM)psm).getSpectrum().getParams().listIterator();
            } else {
                // the psm is a ReportPSMSet
                ReportPSM rPSM = ((ReportPSMSet)psm).getPSMs().get(0);
                paramIt = rPSM.getSpectrum().getParams().listIterator();
            }

            // copy all cvParams over
            while (paramIt.hasNext()) {
                AbstractParam param = paramIt.next();

                if (param instanceof CvParam) {
                    sii.getCvParam().add((CvParam)param);
                } else if (param instanceof UserParam) {
                    sii.getUserParam().add((UserParam)param);
                }
            }
        }
    }


    /**
     * Create or fill the analysis collection and protocol.
     *
     */
    private void createAnalysisCollectionAndAnalysisProtocolCollection(Boolean filterPSM) {
        // update and write the analysisCollection and the analysisProtocolCollection

        SpectrumIdentificationProtocol specIdProt;

        if (analysisProtocolCollection.getSpectrumIdentificationProtocol().isEmpty()) {
            // the protocol was not yet created
            specIdProt = createCombinedSpectrumIdentificationProtocol();
        }  else {
            // the protocol was created before, take it
            specIdProt = analysisProtocolCollection.getSpectrumIdentificationProtocol().get(0);
            // now the SIL can be set as well
            analysisCollection.getSpectrumIdentification().get(0).setSpectrumIdentificationList(siList);
        }

        // give enzymes names
        refineEnzymes(specIdProt);

        if (specIdProt.getAdditionalSearchParams() == null) {
            specIdProt.setAdditionalSearchParams(new ParamList());
        }

        if ((exportFileID == 0) && (piaModeller.getFiles().size() > 1) &&
                piaModeller.getPSMModeller().isCombinedFDRScoreCalculated()) {
            // the consensus score is exported and there is more than one file
            specIdProt.getAdditionalSearchParams().getCvParam().add(
                    MzIdentMLTools.createPSICvParam(OntologyConstants.CONSENSUS_SCORING, null));
        } else {
            // only one file was exported or no consensus -> no special processing
            specIdProt.getAdditionalSearchParams().getCvParam().add(
                    MzIdentMLTools.createPSICvParam(OntologyConstants.NO_SPECIAL_PROCESSING, null));
        }

        specIdProt.getAdditionalSearchParams().getCvParam().add(
                MzIdentMLTools.createPSICvParam(
                        OntologyConstants.PIA_PSM_SETS_CREATED,
                        Boolean.toString(piaModeller.getCreatePSMSets())));

        refineModifications(specIdProt);

        addDecoyRegexpToDatabase();
        addFDRStatusToProtocol(specIdProt);

        // add the applied filters
        if (filterPSM) {
            addPSMFiltersToProtocol(specIdProt, piaModeller.getPSMModeller().getFilters(exportFileID));
        }

        // check, if a threshold is set by now
        if ((specIdProt.getThreshold() == null) ||
                specIdProt.getThreshold().getParamGroup().isEmpty()) {
            if (specIdProt.getThreshold() == null) {
                specIdProt.setThreshold(new ParamList());
            }

            specIdProt.getThreshold().getCvParam().add(
                    MzIdentMLTools.createPSICvParam(OntologyConstants.NO_THRESHOLD, null));
        }
    }


    /**
     * Create a general combination protocol, if more than one file was merged
     *
     * @return
     */
    private SpectrumIdentificationProtocol createCombinedSpectrumIdentificationProtocol() {
        // create the SpectrumIdentificationProtocol for general combining
        SpectrumIdentificationProtocol combiningProtocol = new SpectrumIdentificationProtocol();

        combiningProtocol.setId("psm_combination_protocol");
        combiningProtocol.setAnalysisSoftware(piaAnalysisSoftware);
        combiningProtocol.setAdditionalSearchParams(new ParamList());

        // create a general combining SpectrumIdentification
        SpectrumIdentification combiningId = new SpectrumIdentification();

        combiningId.setId("psm_combination");
        combiningId.setSpectrumIdentificationList(siList);
        combiningId.setSpectrumIdentificationProtocol(combiningProtocol);

        for (SpectraData specData : piaModeller.getSpectraData().values()) {
            InputSpectra spectra = new InputSpectra();
            spectra.setSpectraData(specData);
            combiningId.getInputSpectra().add(spectra);
        }

        for (SearchDatabase searchDB : piaModeller.getSearchDatabases().values()) {
            SearchDatabaseRef ref = new SearchDatabaseRef();
            ref.setSearchDatabase(searchDB);
            combiningId.getSearchDatabaseRef().add(ref);
        }

        // add the search type
        Param tempParam = new Param();
        tempParam.setParam(MzIdentMLTools.createPSICvParam(OntologyConstants.MS_MS_SEARCH, null));
        combiningProtocol.setSearchType(tempParam);

        // TODO: add "the widest" parameters from the original protocol and spectrumid

        // add to the collections
        analysisCollection.getSpectrumIdentification().add(combiningId);
        analysisProtocolCollection.getSpectrumIdentificationProtocol().add(combiningProtocol);
        return combiningProtocol;
    }


    /**
     * Refines the enzyme entries of an {@link SpectrumIdentificationProtocol},
     * i.e. set the enzyme names, if the regexp is given only.
     *
     * @param specIdProt
     */
    private static void refineEnzymes(SpectrumIdentificationProtocol specIdProt) {
        if ((specIdProt.getEnzymes() == null)
                || specIdProt.getEnzymes().getEnzyme().isEmpty()) {
            // no enzymes given, delete the entry
            specIdProt.setEnzymes(null);
            return;
        }

        // if there are enzymes, check whether they can be given names by their regexp
        specIdProt.getEnzymes().getEnzyme().stream().filter(enzyme -> (enzyme.getEnzymeName() == null) && (enzyme.getSiteRegexp() != null)).forEach(enzyme -> {
            CleavageAgent agent = CleavageAgent.getBySiteRegexp(enzyme.getSiteRegexp());
            if (agent != null) {
                ParamList enzymeNameList = new ParamList();

                CvParam cvParam = MzIdentMLTools.createCvParam(
                        agent.getAccession(),
                        MzIdentMLTools.getCvPSIMS(),
                        agent.getName(),
                        null);

                enzymeNameList.getCvParam().add(cvParam);
                enzyme.setEnzymeName(enzymeNameList);
            }
        });
    }

    /**
     * Refines the modifications of an {@link SpectrumIdentificationProtocol},
     * i.e. set the {@link CvParam}s, if they can be found.
     *
     * @param specIdProt
     */
    private void refineModifications(SpectrumIdentificationProtocol specIdProt) {
        if ((specIdProt.getModificationParams() == null)
                ||  specIdProt.getModificationParams().getSearchModification().isEmpty()) {
            // no search modifications, nullify the entry
            specIdProt.setModificationParams(null);
            return;
        }

        // the cvParam of the modification is not set, try to do so
        specIdProt.getModificationParams().getSearchModification().stream().filter(mod -> mod.getCvParam().isEmpty()).forEach(mod -> {
            // the cvParam of the modification is not set, try to do so
            ModT unimod = unimodParser.getModificationByMass(
                    Double.valueOf(mod.getMassDelta()),
                    mod.getResidues());

            if (unimod != null) {
                CvParam tempCvParam = MzIdentMLTools.createCvParam(
                        "UNIMOD:" + unimod.getRecordId(),
                        UnimodParser.getCv(),
                        unimod.getTitle(),
                        null);

                mod.getCvParam().add(tempCvParam);
                mod.setMassDelta(unimod.getDelta().getMonoMass().floatValue());
            }
        });
    }


    /**
     * Sets the regexp for the decoys in the SpectrumIdentificationProtocol, if
     * it is the same for all processed input files.
     *
     */
    private void addDecoyRegexpToDatabase() {
        // get files to handle
        List<Long> fileIDs = new ArrayList<>(piaModeller.getFiles().size());
        if (exportFileID > 0) {
            fileIDs.add(exportFileID);
        } else {
            fileIDs.addAll(piaModeller.getFiles().keySet());
        }

        // get FDRData to handle
        List<FDRData> fdrDatas = new ArrayList<>(piaModeller.getFiles().size());
        fdrDatas.addAll(fileIDs.stream().filter(fileId -> piaModeller.getPSMModeller().isFDRCalculated(fileId)).map(fileId -> piaModeller.getPSMModeller().getFileFDRData().get(fileId)).collect(Collectors.toList()));

        // check patterns in the FDRData
        String decoyPattern = null;
        for (FDRData fdrData : fdrDatas) {
            String newPattern;

            if (fdrData.getDecoyStrategy().equals(FDRData.DecoyStrategy.ACCESSIONPATTERN)) {
                newPattern = fdrData.getDecoyPattern();
            } else {
                newPattern = fdrData.getDecoyStrategy().toString();
            }

            if (decoyPattern == null) {
                decoyPattern = newPattern;
            } else if (!decoyPattern.equals(newPattern)) {
                decoyPattern = null;
                break;
            }
        }

        if (decoyPattern != null) {
            CvParam decoyInfoParam;

            if (FDRData.DecoyStrategy.SEARCHENGINE.toString().equals(decoyPattern)) {
                decoyInfoParam = MzIdentMLTools.createPSICvParam(
                        OntologyConstants.QUALITY_ESTIMATION_WITH_IMPL_DECOY_SEQ,
                        null);
            } else {
                decoyInfoParam = MzIdentMLTools.createPSICvParam(
                        OntologyConstants.DECOY_DB_ACCESSION_REGEXP,
                        decoyPattern);
            }

            for (SearchDatabase db : inputs.getSearchDatabase()) {
                db.getCvParam().add(decoyInfoParam);
            }
        }
    }


    /**
     * Sets the FDR status in the SpectrumIdentificationProtocol, if it is the
     * same for all processed input files.
     *
     * @param specIdProt
     */
    private void addFDRStatusToProtocol(SpectrumIdentificationProtocol specIdProt) {
        if ((exportFileID > 0) || (piaModeller.getFiles().size() == 1)) {
            Long fileId = exportFileID;
            if (piaModeller.getFiles().size() == 1) {
                fileId = piaModeller.getFiles().keySet().iterator().next();
            }

            specIdProt.getAdditionalSearchParams().getCvParam().add(
                    MzIdentMLTools.createPSICvParam(
                            OntologyConstants.PIA_FDRSCORE_CALCULATED,
                            piaModeller.getPSMModeller().isFDRCalculated(fileId).toString()));

            specIdProt.getAdditionalSearchParams().getCvParam().add(
                    MzIdentMLTools.createPSICvParam(
                            OntologyConstants.PIA_USED_TOP_IDENTIFICATIONS,
                            piaModeller.getPSMModeller().getFilesTopIdentifications(fileId).toString()));
        }

        if (exportFileID == 0L) {
            specIdProt.getAdditionalSearchParams().getCvParam().add(
                    MzIdentMLTools.createPSICvParam(
                            OntologyConstants.PIA_COMBINED_FDRSCORE_CALCULATED,
                            String.valueOf(piaModeller.getPSMModeller().isCombinedFDRScoreCalculated())));
        }
    }


    /**
     * Adds the file's PSM level filters to the SpectrumIdentificationProtocol
     *
     * @param specIdProt
     * @param filterList
     */
    private static void addPSMFiltersToProtocol(SpectrumIdentificationProtocol specIdProt,
            List<AbstractFilter> filterList) {

        for (AbstractFilter filter : filterList) {
            if (filter instanceof PSMScoreFilter) {
                // if score filters are set, they are the threshold

                ScoreModelEnum scoreModel =
                        ScoreModelEnum.getModelByDescription(((PSMScoreFilter) filter).getScoreShortName());

                if (specIdProt.getThreshold() == null) {
                    specIdProt.setThreshold(new ParamList());
                }

                if (!scoreModel.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
                    CvParam thrCvParam = MzIdentMLTools.createCvParam(
                            scoreModel.getCvAccession(),
                            MzIdentMLTools.getCvPSIMS(),
                            scoreModel.getCvName(),
                            filter.getFilterValue().toString());

                    specIdProt.getThreshold().getCvParam().add(thrCvParam);
                } else {
                    // TODO: also make scores from OBO available
                    UserParam userParam = new UserParam();
                    userParam.setName(((PSMScoreFilter) filter).getModelName());
                    userParam.setValue(filter.getFilterValue().toString());

                    specIdProt.getThreshold().getUserParam().add(userParam);
                }
            } else {
                // all other report filters are AdditionalSearchParams
                specIdProt.getAdditionalSearchParams().getCvParam().add(
                        MzIdentMLTools.createPSICvParam(OntologyConstants.PIA_FILTER, filter.toString()));
            }
        }
    }


    /**
     * Creates all protein level relevant information, including the
     * {@link ProteinDetectionList}.
     *
     * @param filterExport
     */
    private void createProteinLevelInformation(boolean filterExport) {
        // first create the protocol
        ProteinDetectionProtocol proteinDetectionProtocol = createProteinDetectionProtocol(filterExport);

        // create the proteinDetectionList
        pdList = new ProteinDetectionList();
        pdList.setId("protein_report");

        List<AbstractFilter> proteinFilters = null;
        if (filterExport) {
            proteinFilters = piaModeller.getProteinModeller().getReportFilters();
        }

        Integer thresholdPassingPAGcount = 0;
        for (ReportProtein protein : piaModeller.getProteinModeller().getFilteredReportProteins(null)) {
            if (putProteinIntoDetectionList(protein, filterExport, proteinFilters)) {
                thresholdPassingPAGcount++;
            }
        }

        pdList.getCvParam().add(MzIdentMLTools.createPSICvParam(
                OntologyConstants.COUNT_OF_IDENTIFIED_PROTEINS,
                thresholdPassingPAGcount.toString()));

        // create the ProteinDetection for PIAs protein inference
        ProteinDetection proteinDetection = new ProteinDetection();
        analysisCollection.setProteinDetection(proteinDetection);
        proteinDetection.setId("PIA_protein_inference");
        proteinDetection.setName("PIA protein inference");
        proteinDetection.setProteinDetectionList(pdList);
        proteinDetection.setProteinDetectionProtocol(proteinDetectionProtocol);
        InputSpectrumIdentifications inputSpecIDs = new InputSpectrumIdentifications();
        inputSpecIDs.setSpectrumIdentificationList(siList);
        proteinDetection.getInputSpectrumIdentifications().add(inputSpecIDs);
    }


    /**
     * Creates the {@link ProteinDetectionProtocol} with the PIA protein
     * inference settings.
     *
     * @param filterExport
     * @return
     */
    private ProteinDetectionProtocol createProteinDetectionProtocol(boolean filterExport) {
        // create the ProteinDetectionProtocol for PIAs protein inference
        ProteinDetectionProtocol proteinDetectionProtocol = new ProteinDetectionProtocol();

        proteinDetectionProtocol.setId("PIA_protein_inference_protocol");
        proteinDetectionProtocol.setAnalysisSoftware(piaAnalysisSoftware);
        proteinDetectionProtocol.setName("PIA protein inference protocol");

        analysisProtocolCollection.setProteinDetectionProtocol(proteinDetectionProtocol);

        AbstractProteinInference proteinInference =
                piaModeller.getProteinModeller().getAppliedProteinInference();

        // add the inference settings to the AnalysisParams
        proteinDetectionProtocol.setAnalysisParams(new ParamList());

        // the used inference method
        proteinDetectionProtocol.getAnalysisParams().getCvParam().add(
                MzIdentMLTools.createPSICvParam(
                        OntologyConstants.PIA_PROTEIN_INFERENCE,
                        proteinInference.getShortName()));

        // inference filters
        for (AbstractFilter filter : proteinInference.getFilters()) {
            proteinDetectionProtocol.getAnalysisParams().getCvParam().add(
                    MzIdentMLTools.createPSICvParam(
                            OntologyConstants.PIA_PROTEIN_INFERENCE_FILTER,
                            filter.toString()));
        }

        // scoring method
        proteinDetectionProtocol.getAnalysisParams().getCvParam().add(
                MzIdentMLTools.createPSICvParam(
                        OntologyConstants.PIA_PROTEIN_INFERENCE_SCORING,
                        proteinInference.getScoring().getShortName()));

        // score used for scoring
        proteinDetectionProtocol.getAnalysisParams().getCvParam().add(
                MzIdentMLTools.createPSICvParam(
                        OntologyConstants.PIA_PROTEIN_INFERENCE_USED_SCORE,
                        proteinInference.getScoring().getScoreSetting().getValue()));

        // PSMs used for scoring
        proteinDetectionProtocol.getAnalysisParams().getCvParam().add(
                MzIdentMLTools.createPSICvParam(
                        OntologyConstants.PIA_PROTEIN_INFERENCE_USED_PSMS,
                        proteinInference.getScoring().getPSMForScoringSetting().getValue()));

        // filters on protein level
        proteinDetectionProtocol.setThreshold(new ParamList());
        if (filterExport && !piaModeller.getProteinModeller().getReportFilters().isEmpty()) {
            for (AbstractFilter filter : piaModeller.getProteinModeller().getReportFilters()) {
                if (RegisteredFilters.PROTEIN_SCORE_FILTER.getShortName().equals(filter.getShortName())) {
                    // if score filters are set, they are the threshold
                    proteinDetectionProtocol.getThreshold().getCvParam().add(
                            MzIdentMLTools.createPSICvParam(
                                    OntologyConstants.PIA_PROTEIN_SCORE,
                                    filter.getFilterValue().toString()));
                } else {
                    proteinDetectionProtocol.getAnalysisParams().getCvParam().add(
                            MzIdentMLTools.createPSICvParam(
                                    OntologyConstants.PIA_FILTER,
                                    filter.toString()));
                }
            }
        }

        if (proteinDetectionProtocol.getThreshold().getParamGroup().isEmpty()) {
            // no threshold was defined
            proteinDetectionProtocol.getThreshold().getCvParam().add(
                    MzIdentMLTools.createPSICvParam(OntologyConstants.NO_THRESHOLD, null));
        }

        return proteinDetectionProtocol;
    }


    /**
     * Creates a {@link ProteinAmbiguityGroup} for the given protein and puts it
     * into the {@link ProteinDetectionList}.
     *
     * @param protein
     * @param filterExport
     * @param filters
     * @return
     */
    private boolean putProteinIntoDetectionList(ReportProtein protein, boolean filterExport,
            List<AbstractFilter> filters) {
        ProteinAmbiguityGroup pag = new ProteinAmbiguityGroup();

        pag.setId(PROTEIN_AMBIGUITY_GROUP_PREFIX + protein.getID());
        pdList.getProteinAmbiguityGroup().add(pag);

        boolean passThreshold = true;
        if (filterExport) {
            passThreshold = FilterFactory.satisfiesFilterList(protein, 0L, filters);
        }

        pag.getCvParam().add(MzIdentMLTools.createPSICvParam(
                OntologyConstants.PROTEIN_GROUP_PASSES_THRESHOLD,
                Boolean.toString(passThreshold)));

        pag.getCvParam().add(MzIdentMLTools.createPSICvParam(
                OntologyConstants.CLUSTER_IDENTIFIER,
                Long.toString(protein.getAccessions().get(0).getGroup().getTreeID())));

        StringBuilder leadingPDHsSB = new StringBuilder();
        List<String> leadingPDHsList = new ArrayList<>(protein.getAccessions().size());
        // the reported proteins/accessions are the "main" proteins
        for (Accession acc : protein.getAccessions()) {
            ProteinDetectionHypothesis pdh = createPDH(pag.getId(), acc, protein, passThreshold);

            pdh.getCvParam().add(MzIdentMLTools.createPSICvParam(
                    OntologyConstants.LEADING_PROTEIN,
                    null));

            leadingPDHsSB.append(pdh.getId());
            leadingPDHsSB.append(" ");

            leadingPDHsList.add(pdh.getId());

            pag.getProteinDetectionHypothesis().add(pdh);
        }


        if (leadingPDHsList.size() > 1) {
            // set for each PAG the other same-set proteins
            addSameSetProteinDetectionHypotheses(pag.getProteinDetectionHypothesis(),
                    leadingPDHsList);
        }

        // now add the sub-proteins
        for (ReportProtein subProtein : protein.getSubSets()) {
            List<ProteinDetectionHypothesis> samePDHs =
                    new ArrayList<>(subProtein.getAccessions().size());
            List<String> samePDHsIdsList = new ArrayList<>(subProtein.getAccessions().size());

            for (Accession subAcc : subProtein.getAccessions()) {
                ProteinDetectionHypothesis pdh = createPDH(pag.getId(), subAcc, subProtein, false);

                pdh.getCvParam().add(MzIdentMLTools.createPSICvParam(
                        OntologyConstants.NON_LEADING_PROTEIN, null));

                pdh.getCvParam().add(MzIdentMLTools.createPSICvParam(
                        OntologyConstants.SEQUENCE_SUB_SET_PROTEIN,
                        leadingPDHsSB.toString().trim()));

                pag.getProteinDetectionHypothesis().add(pdh);

                samePDHs.add(pdh);
                samePDHsIdsList.add(pdh.getId());
            }

            if (samePDHs.size() > 1) {
                addSameSetProteinDetectionHypotheses(samePDHs, samePDHsIdsList);
            }
        }

        return passThreshold;
    }


    /**
     * Adds the cvParam for the sames-set PDHs to the given
     * ProteinDetectionHypotheses.
     *
     * @param pdhList
     * @param pdhIds
     */
    private static void addSameSetProteinDetectionHypotheses(List<ProteinDetectionHypothesis> pdhList,
            List<String> pdhIds) {
        for (ProteinDetectionHypothesis pdh : pdhList) {
            List<String> otherPDHsList = new ArrayList<>(pdhIds);
            otherPDHsList.remove(pdh.getId());

            StringBuilder otherPDHs = new StringBuilder();

            for (String other : otherPDHsList) {
                otherPDHs.append(other);
                otherPDHs.append(" ");
            }

            pdh.getCvParam().add(MzIdentMLTools.createPSICvParam(
                    OntologyConstants.SEQUENCE_SAME_SET_PROTEIN,
                    otherPDHs.toString().trim()));
        }
    }


    /**
     * Create the {@link ProteinDetectionHypothesis} for the given accession of
     * the protein.
     *
     * @param pagId
     * @param acc
     * @param protein
     * @param passThreshold
     * @return
     */
    private ProteinDetectionHypothesis createPDH(String pagId, Accession acc,
            ReportProtein protein, Boolean passThreshold) {
        ProteinDetectionHypothesis pdh = new ProteinDetectionHypothesis();

        pdh.setId(PROTEIN_DETECTION_HYPOTHESIS_PREFIX + acc.getAccession() + "_" + pagId);

        DBSequence dbSequence = sequenceMap.get(acc.getAccession());
        if (dbSequence != null) {
            pdh.setDBSequence(dbSequence);
        }

        pdh.setPassThreshold(passThreshold);

        Map<String, PeptideHypothesis> peptideHypotheses = new HashMap<>();

        boolean allOk = true;
        for (ReportPeptide pep : protein.getPeptides()) {
            for (PSMReportItem psmItem : pep.getPSMs()) {
                // sort the PSMs' SpectrumIdentificationItems into the PeptideHypotheses
                List<String> peptideEvidenceIDs = getPeptideEvidenceIdsForAccession(psmItem, acc);

                for (String evidenceID : peptideEvidenceIDs) {
                    allOk &= addPSMToPeptideDetectionHypothesis(evidenceID, pdh, peptideHypotheses, psmItem);
                }
            }
        }

        if (!allOk) {
            return null;
        }

        CvParam cvParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.PIA_PROTEIN_SCORE,
                protein.getScore().toString());
        pdh.getCvParam().add(cvParam);

        return pdh;
    }


    /**
     * This method creates a list of the peptideEvidenceIDs for the PSM and the
     * accession.
     *
     * @param psm
     * @param acc
     * @return
     */
    private static List<String> getPeptideEvidenceIdsForAccession(PSMReportItem psm, Accession acc) {
        List<String> peptideEvidenceIDs = new ArrayList<>();
        String peptideId = psm.getPeptideStringID(true);
        boolean foundOccurrence = false;

        for (AccessionOccurrence occurrence : psm.getPeptide().getAccessionOccurrences()) {
            if (acc.getAccession().equals( occurrence.getAccession().getAccession())) {
                peptideEvidenceIDs.add(createPeptideEvidenceID(
                        peptideId,
                        occurrence.getStart(),
                        occurrence.getEnd(),
                        acc));

                foundOccurrence = true;
                // there might be multiple occurrences per accession, therefore no loop-break here
            }
        }

        if (!foundOccurrence) {
            // add generic peptide evidence id
            peptideEvidenceIDs.add(
                    createPeptideEvidenceID(peptideId, null, null, acc));
        }

        return peptideEvidenceIDs;
    }


    /**
     * Links the PSM with its peptide evidences to the PDH.
     *
     * @param pepEvidenceId
     * @param pdh
     * @param peptideHypotheses
     * @param psm
     * @return
     */
    private boolean addPSMToPeptideDetectionHypothesis(String pepEvidenceId,
            ProteinDetectionHypothesis pdh, Map<String, PeptideHypothesis> peptideHypotheses,
            PSMReportItem psm) {
        PeptideHypothesis ph = peptideHypotheses.get(pepEvidenceId);

        if (ph == null) {
            ph = new PeptideHypothesis();
            ph.setPeptideEvidence(pepEvidenceMap.get(pepEvidenceId));

            if (ph.getPeptideEvidence() == null) {
                LOGGER.error("could not find peptideEvidence for '" + pepEvidenceId + "'! "
                        + "This may happen, if you use different accessions in your databases/search engines.");
                return false;
            }

            peptideHypotheses.put(pepEvidenceId, ph);
            pdh.getPeptideHypothesis().add(ph);
        }

        String siiID = psm.getIdentificationKey(piaModeller.getPSMSetSettings());

        SpectrumIdentificationItemRef ref = new SpectrumIdentificationItemRef();
        ref.setSpectrumIdentificationItemRef(siiID);

        ph.getSpectrumIdentificationItemRef().add(ref);

        return true;
    }


    /**
     * Refine some elements before writing out the mzIdentML file. The URIs will
     * be checked.
     */
    private void refineElements() {
        refineInputs();
    }


    /**
     * Refine some elements in the Inputs to ensure compatibility.
     */
    private void refineInputs() {
        for (SourceFile sourceFile : inputs.getSourceFile()) {
            String location = encodeLocation(sourceFile.getLocation());
            sourceFile.setLocation(location);
        }

        for (SearchDatabase searchDB : inputs.getSearchDatabase()) {
            String location = encodeLocation(searchDB.getLocation());
            searchDB.setLocation(location);
        }

        for (SpectraData spectraData : inputs.getSpectraData()) {
            String location = encodeLocation(spectraData.getLocation());
            spectraData.setLocation(location);

            // the name of the "MGF format" entry changed in the OBO -> set to current value
            FileFormat fileFormat = spectraData.getFileFormat();
            if (fileFormat != null) {
                CvParam formatCvParam = fileFormat.getCvParam();
                if ((formatCvParam != null)
                        && (OntologyConstants.MASCOT_MGF_FORMAT.getPsiAccession().equals(formatCvParam.getAccession()))) {
                    formatCvParam.setName(OntologyConstants.MASCOT_MGF_FORMAT.getPsiName());
                }
            }

        }
    }


    /**
     * Replaces special characters in a location.
     *
     * @param location
     * @return
     */
    private static String encodeLocation(String location) {
        return location.replace(" ", "%20");
    }
}