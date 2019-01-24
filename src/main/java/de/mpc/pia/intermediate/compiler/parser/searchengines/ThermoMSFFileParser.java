package de.mpc.pia.intermediate.compiler.parser.searchengines;

import java.util.*;

import org.apache.log4j.Logger;

import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.Enzyme;
import uk.ac.ebi.jmzidml.model.mzidml.Enzymes;
import uk.ac.ebi.jmzidml.model.mzidml.FileFormat;
import uk.ac.ebi.jmzidml.model.mzidml.InputSpectra;
import uk.ac.ebi.jmzidml.model.mzidml.ModificationParams;
import uk.ac.ebi.jmzidml.model.mzidml.Param;
import uk.ac.ebi.jmzidml.model.mzidml.ParamList;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabase;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabaseRef;
import uk.ac.ebi.jmzidml.model.mzidml.SearchModification;
import uk.ac.ebi.jmzidml.model.mzidml.SpecificityRules;
import uk.ac.ebi.jmzidml.model.mzidml.SpectraData;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIDFormat;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationProtocol;
import uk.ac.ebi.jmzidml.model.mzidml.Tolerance;
import de.mpc.PD.APeptideScores;
import de.mpc.PD.APeptides;
import de.mpc.PD.APeptidesAminoAcidModifications;
import de.mpc.PD.APeptidesTerminalModifications;
import de.mpc.PD.AminoAcidModifications;
import de.mpc.PD.AminoAcids;
import de.mpc.PD.FastaFiles;
import de.mpc.PD.FileInfos;
import de.mpc.PD.MassPeaks;
import de.mpc.PD.PeptideScores;
import de.mpc.PD.PeptideScores_decoy;
import de.mpc.PD.Peptides;
import de.mpc.PD.PeptidesAminoAcidModifications;
import de.mpc.PD.PeptidesAminoAcidModifications_decoy;
import de.mpc.PD.PeptidesProteins;
import de.mpc.PD.PeptidesProteins_decoy;
import de.mpc.PD.PeptidesTerminalModifications;
import de.mpc.PD.PeptidesTerminalModifications_decoy;
import de.mpc.PD.Peptides_decoy;
import de.mpc.PD.ProcessingNodeParameters;
import de.mpc.PD.ProcessingNodeScores;
import de.mpc.PD.ProcessingNodes;
import de.mpc.PD.ProteinAnnotations;
import de.mpc.PD.Proteins;
import de.mpc.PD.SpectrumHeaders;
import de.mpc.PD.DB.JDBCAccess;
import de.mpc.PD.Params.SimpleProgramParameters;
import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.parser.FastaHeaderInfos;
import de.mpc.pia.intermediate.compiler.parser.InputFileParserFactory;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.tools.MzIdentMLTools;
import de.mpc.pia.tools.OntologyConstants;
import de.mpc.pia.tools.PIAConstants;
import de.mpc.pia.tools.PIATools;


/**
 * This class parses the data from a Thermo MSF file for a given
 * {@link PIACompiler}.<br/>
 *
 * @author julian
 *
 */
public class ThermoMSFFileParser {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(ThermoMSFFileParser.class);


    /**
     * We don't ever want to instantiate this class
     */
    private ThermoMSFFileParser() {
        throw new AssertionError();
    }


    /**
     * Parses the data from an ProteomeDiscoverer's MSF file given by its name
     * into the given {@link PIACompiler}.
     */
    public static boolean getDataFromThermoMSFFile(String name, String fileName,
            PIACompiler compiler) {
        LOGGER.debug("getting data from file: " + fileName);

        SimpleProgramParameters fileConnectionParams;

        // set up the DB connection to the MSF file
        boolean bUseJDBC = true;  // always use the JDBC connection
        fileConnectionParams = new SimpleProgramParameters(fileName, bUseJDBC);
        JDBCAccess jdbc = new JDBCAccess();
        jdbc.connectToExistingDB(fileName);
        fileConnectionParams.setJDBCAccess(jdbc);

        Map<Long, SpectrumIdentification> nodeNumbersToIdentifications = new HashMap<>();
        Map<Long, SpectrumIdentificationProtocol> nodeNumbersToProtocols = new HashMap<>();
        Map<Long, AnalysisSoftware> nodeNumbersToSoftwares = new HashMap<>();

        // iterate through the ProcessingNodes and get the settings etc.
        for (Map.Entry<Object, Object> nodeObjectIt
                : ProcessingNodes.getObjectMap(fileConnectionParams, ProcessingNodes.class).entrySet()) {

            if (!(nodeObjectIt.getValue() instanceof ProcessingNodes)) {
                LOGGER.warn("not a processingNodes " + nodeObjectIt.getValue().getClass().getCanonicalName());
                continue;
            }

            ProcessingNodes node = (ProcessingNodes)nodeObjectIt.getValue();
            AnalysisSoftware software = createAnalysisSoftware(node);

            if (software != null) {
                // add the software
                software = compiler.putIntoSoftwareMap(software);
                nodeNumbersToSoftwares.put(node.getProcessingNodeNumber(), software);

                // get all additional data
                SearchDatabase searchDatabase = null;
                Enzyme enzyme = null;
                Integer maxMissedCleavages = null;
                ParamList additionalSearchParams = new ParamList();
                Tolerance fragmentTolerance = null;
                Tolerance peptideTolerance = null;
                ModificationParams modificationParameters =
                        new ModificationParams();

                List<String> processingParamNames = node.getProcessingNodeParameterNames();
                for (String paramName : processingParamNames) {
                    ProcessingNodeParameters processingNodeParams =
                            new ProcessingNodeParameters(fileConnectionParams, node.getProcessingNodeNumber(), paramName);

                    if ("FastaDatabase".equals(paramName)
                            || "Protein Database".equals(paramName)) {
                        // get database information
                        FastaFiles fastaFiles = processingNodeParams.getFastaFilesObj();
                        if (fastaFiles != null) {
                            // database used
                            searchDatabase = new SearchDatabase();

                            searchDatabase.setId(software.getName() + "DB"
                                    + node.getProcessingNodeNumber());

                            searchDatabase.setLocation("PD database");
                            searchDatabase.setName(fastaFiles.getFileName());

                            // databaseName
                            Param dbParam = new Param();
                            dbParam.setParam(MzIdentMLTools.createUserParam(
                                    "FASTA file name",
                                    fastaFiles.getFileName(),
                                    "string"));
                            searchDatabase.setDatabaseName(dbParam);

                            // this gets the number of taxonomy filtered sequences/residues
                            searchDatabase.setNumDatabaseSequences(fastaFiles.getNumberOfProteins().longValue());
                            searchDatabase.setNumResidues(fastaFiles.getNumberOfAminoAcids().longValue());

                            // add searchDB to the compiler
                            searchDatabase = compiler.putIntoSearchDatabasesMap(searchDatabase);
                        }
                    } else if ("Enzyme".equals(paramName)
                            || "Enzyme Name".equals(paramName)) {
                        enzyme = MzIdentMLTools.getEnzymeFromName(processingNodeParams.getParameterValue());
                    } else if ("MaxMissedCleavages".equals(paramName)
                            || "MissedCleavages".equals(paramName)
                            || "Maximum Missed Cleavage Sites".equals(paramName)
                            || "Max. Missed Cleavage Sites".equals(paramName)) {
                        // the allowed missed cleavages
                        maxMissedCleavages =
                                Integer.parseInt(processingNodeParams.getParameterValue());
                    } else if ("UseAveragePrecursorMass".equals(paramName)
                            || "Use Average Precursor Mass".equals(paramName)) {
                        // precursor mass monoisotopic or average
                        CvParam precursorParam;
                        if ("False".equals(processingNodeParams.getParameterValue())) {
                            // monoisotopic
                            precursorParam = MzIdentMLTools.createPSICvParam(
                                    OntologyConstants.PARENT_MASS_TYPE_MONO, null);
                        } else {
                            // average
                            precursorParam = MzIdentMLTools.createPSICvParam(
                                    OntologyConstants.PARENT_MASS_TYPE_AVERAGE, null);
                        }
                        additionalSearchParams.getCvParam().add(precursorParam);
                    } else if ("UseAverageFragmentMass".equals(paramName)
                            || "Use Average Fragment Masses".equals(paramName)
                            || "Use Average Fragment Mass".equals(paramName)) {
                        // fragment mass monoisotopic or average
                        CvParam fragmentParam;
                        if ("False".equals(processingNodeParams.getParameterValue())) {
                            // monoisotopic
                            fragmentParam = MzIdentMLTools.createPSICvParam(
                                    OntologyConstants.FRAGMENT_MASS_TYPE_MONO, null);
                        } else {
                            // average
                            fragmentParam = MzIdentMLTools.createPSICvParam(
                                    OntologyConstants.FRAGMENT_MASS_TYPE_AVERAGE, null);
                        }
                        additionalSearchParams.getCvParam().add(fragmentParam);
                    } else if ("FragmentTolerance".equals(paramName)
                            || "Fragment Mass Tolerance".equals(paramName)
                            || "MS2Tolerance".equals(paramName)) {
                        fragmentTolerance = new Tolerance();

                        String[] split = processingNodeParams.getParameterValue().split(" ");

                        CvParam tolParam = MzIdentMLTools.createPSICvParam(
                                OntologyConstants.SEARCH_TOLERANCE_PLUS_VALUE, split[0]);
                        MzIdentMLTools.setUnitParameterFromString(split[1], tolParam);
                        fragmentTolerance.getCvParam().add(tolParam);

                        tolParam = MzIdentMLTools.createPSICvParam(
                                OntologyConstants.SEARCH_TOLERANCE_MINUS_VALUE, split[0]);
                        MzIdentMLTools.setUnitParameterFromString(split[1], tolParam);
                        fragmentTolerance.getCvParam().add(tolParam);
                    } else if ("PeptideTolerance".equals(paramName)
                            || "Precursor Mass Tolerance".equals(paramName)
                            || "MS1Tolerance".equals(paramName)) {
                        peptideTolerance = new Tolerance();

                        String[] split = processingNodeParams.getParameterValue().split(" ");

                        CvParam tolParam = MzIdentMLTools.createPSICvParam(
                                OntologyConstants.SEARCH_TOLERANCE_PLUS_VALUE, split[0]);
                        MzIdentMLTools.setUnitParameterFromString(split[1], tolParam);
                        peptideTolerance.getCvParam().add(tolParam);

                        tolParam = MzIdentMLTools.createPSICvParam(
                                OntologyConstants.SEARCH_TOLERANCE_MINUS_VALUE, split[0]);
                        MzIdentMLTools.setUnitParameterFromString(split[1], tolParam);
                        peptideTolerance.getCvParam().add(tolParam);
                    } else if ("MinimumPeptideLength".equals(paramName)) {
                        additionalSearchParams.getCvParam().add(
                                MzIdentMLTools.createPSICvParam(OntologyConstants.PROTEOME_DISCOVERER_MIN_PEPTIDE_LENGTH,
                                        processingNodeParams.getParameterValue()));
                    } else if ("MaximumPeptideLength".equals(paramName)) {
                        additionalSearchParams.getCvParam().add(
                                MzIdentMLTools.createPSICvParam(OntologyConstants.PROTEOME_DISCOVERER_MAX_PEPTIDE_LENGTH,
                                        processingNodeParams.getParameterValue()));
                    } else {
                        // parse additional software specific settings
                        parseSoftwareSpecificSettings(node, processingNodeParams,
                                additionalSearchParams, modificationParameters);
                    }
                }

                // create the spectrumIDProtocol
                SpectrumIdentificationProtocol spectrumIDProtocol =
                        new SpectrumIdentificationProtocol();

                spectrumIDProtocol.setId(
                        "pdAnalysis_" + node.getID());
                spectrumIDProtocol.setAnalysisSoftware(software);

                // only MS/MS searches are usable for PIA
                Param searchTypeParam = new Param();
                searchTypeParam.setParam(MzIdentMLTools.createPSICvParam(OntologyConstants.MS_MS_SEARCH, null));

                spectrumIDProtocol.setSearchType(searchTypeParam);

                if (!additionalSearchParams.getParamGroup().isEmpty()) {
                    spectrumIDProtocol.setAdditionalSearchParams(additionalSearchParams);
                }

                spectrumIDProtocol.setModificationParams(modificationParameters);

                if (enzyme != null) {
                    if (maxMissedCleavages != null) {
                        enzyme.setMissedCleavages(maxMissedCleavages);
                    }

                    Enzymes enzymes = new Enzymes();
                    spectrumIDProtocol.setEnzymes(enzymes);
                    enzymes.getEnzyme().add(enzyme);
                }

                if (fragmentTolerance != null) {
                    spectrumIDProtocol.setFragmentTolerance(fragmentTolerance);
                }

                if (peptideTolerance != null) {
                    spectrumIDProtocol.setParentTolerance(peptideTolerance);
                }

                // no threshold set, take all PSMs from the dat file
                ParamList thrParamList = new ParamList();
                thrParamList.getCvParam().add(
                        MzIdentMLTools.createPSICvParam(OntologyConstants.NO_THRESHOLD, null));
                spectrumIDProtocol.setThreshold(thrParamList);

                nodeNumbersToProtocols.put(node.getProcessingNodeNumber(),
                        spectrumIDProtocol);


                // add the spectrum identification
                SpectrumIdentification spectrumID = new SpectrumIdentification();
                spectrumID.setId("node" + node.getProcessingNodeNumber() + "Identification");
                spectrumID.setSpectrumIdentificationList(null);

                if (searchDatabase != null) {
                    SearchDatabaseRef searchDBRef = new SearchDatabaseRef();
                    searchDBRef.setSearchDatabase(searchDatabase);
                    spectrumID.getSearchDatabaseRef().add(searchDBRef);
                }

                nodeNumbersToIdentifications.put(
                        node.getProcessingNodeNumber(), spectrumID);
            }
        }

        if (nodeNumbersToIdentifications.size() < 1) {
            LOGGER.error("There are no search nodes in the MSF file!");
            return false;
        }

        Map<Long, PIAInputFile> nodeNumbersToInputFiles = new HashMap<>();
        for (Map.Entry<Long, SpectrumIdentification> idIt : nodeNumbersToIdentifications.entrySet()) {
            PIAInputFile file;

            if (nodeNumbersToIdentifications.size() > 1) {
                // more than one identification in the MSF file -> make several PIAInputFiles
                String searchName = name + "_" +
                        nodeNumbersToSoftwares.get(idIt.getKey()).getName() +
                        "_" + idIt.getKey();
                file = compiler.insertNewFile(searchName, fileName,
                        InputFileParserFactory.InputFileTypes.THERMO_MSF_INPUT.getFileSuffix());
            } else {
                // only one identification node in the file
                file = compiler.insertNewFile(name, fileName,
                        InputFileParserFactory.InputFileTypes.THERMO_MSF_INPUT.getFileSuffix());
            }

            SpectrumIdentificationProtocol protocol = nodeNumbersToProtocols.get(idIt.getKey());
            SpectrumIdentification id = idIt.getValue();

            file.addSpectrumIdentificationProtocol(protocol);

            id.setSpectrumIdentificationProtocol(protocol);
            file.addSpectrumIdentification(id);

            nodeNumbersToInputFiles.put(idIt.getKey(), file);
        }


        // get the amino acid information from file
        Map<Character, AminoAcids> aminoAcidMap = getAminoAcids(fileConnectionParams);

        // mapping from fileID to input spectra
        Map<Long, SpectraData> spectraDataMap =
                new HashMap<>();

        // mapping from the ID of SpectrumIdentification to IDs of used inputSpectra
        Map<String, Set<String>> spectrumIdToSpectraData =
                new HashMap<>();

        LOGGER.info("get spectra info...");
        Map<Object, Object> spectraMap = SpectrumHeaders.getObjectMap(fileConnectionParams, SpectrumHeaders.class);
        LOGGER.info("#spectra: " + spectraMap.size());

        LOGGER.info("get peak info...");
        Map<Object, Object> massPeakMap = MassPeaks.getObjectMap(fileConnectionParams, MassPeaks.class);
        LOGGER.info("#peaks: " + massPeakMap.size());

        LOGGER.info("get file info...");
        Map<Object, Object> fileMap = FileInfos.getObjectMap(fileConnectionParams, FileInfos.class);
        LOGGER.info("#files: " + fileMap.size());

        LOGGER.info("get amino acid modifications...");
        Map<Object, Object> modificationsMap = AminoAcidModifications.getObjectMap(fileConnectionParams, AminoAcidModifications.class);
        LOGGER.info("#amino acid modifications: " + modificationsMap.size());

        LOGGER.info("get protein sequences...");
        Map<Long, String> sequencesMap = new HashMap<>();
        for (Object proteinObj : Proteins.getObjectMap(fileConnectionParams, Proteins.class).values()) {
            Proteins protein = (Proteins)proteinObj;
            sequencesMap.put(protein.getProteinID(), protein.getSequence());
        }
        LOGGER.info("#protein sequences: " + sequencesMap.size());

        LOGGER.info("get protein annotations...");
        Map<Long, String> annotationsMap = new HashMap<>();
        for (Object annotationObj : ProteinAnnotations.getObjectMap(fileConnectionParams, ProteinAnnotations.class).values()) {
            ProteinAnnotations annotation = (ProteinAnnotations)annotationObj;
            annotationsMap.put(annotation.getProteinID(), annotation.getDescription());
        }
        LOGGER.info("#protein annotations: " + annotationsMap.size());

        LOGGER.info("get scores...");
        // mapping from scoreID to scoreName
        Map<Long, String> scoresMap = new HashMap<>();
        for (Object scoreObj : ProcessingNodeScores.getObjectMap(fileConnectionParams, ProcessingNodeScores.class).values()) {
            ProcessingNodeScores score = (ProcessingNodeScores)scoreObj;
            scoresMap.put(score.getScoreID(), score.getFriendlyName());
        }
        LOGGER.info("#scores: " + scoresMap.size());


        // parse the peptides
        LOGGER.info("get peptide info...");
        Collection<Object> peptides = Peptides.getObjectMap(fileConnectionParams, Peptides.class).values();
        LOGGER.info("#peptides: " + peptides.size());

        LOGGER.info("get modifications info...");
        // map from peptideID to modifications
        Map<Long, List<APeptidesAminoAcidModifications>> peptidesModifications =
                new HashMap<>();
        for (Object modObj : PeptidesAminoAcidModifications.getObjectMap(fileConnectionParams, PeptidesAminoAcidModifications.class).values()) {
            APeptidesAminoAcidModifications mod = (APeptidesAminoAcidModifications)modObj;

            List<APeptidesAminoAcidModifications> modList = peptidesModifications.computeIfAbsent(mod.getPeptideID(), k -> new ArrayList<>());

            modList.add(mod);
        }
        LOGGER.info("#modified peptides: " + peptidesModifications.size());

        LOGGER.info("get terminal modifications info...");
        // map from peptideID to terminal modifications
        Map<Long, List<AminoAcidModifications>> terminalModifications =
                new HashMap<>();
        for (Object modObj : PeptidesTerminalModifications.getObjectMap(fileConnectionParams, PeptidesTerminalModifications.class).values()) {
            APeptidesTerminalModifications termMod = (APeptidesTerminalModifications)modObj;

            List<AminoAcidModifications> termModList = terminalModifications.computeIfAbsent(termMod.getPeptideID(), k -> new ArrayList<>());

            termModList.add((AminoAcidModifications)modificationsMap.get(termMod.getTerminalModificationID()));
        }
        LOGGER.info("#terminal modified peptides: " + terminalModifications.size());

        LOGGER.info("get peptides/proteins information...");
        //map from peptideID to proteins
        Map<Long, List<Long>> peptidesProteins = new HashMap<>();
        for (Object pepProtObj : PeptidesProteins.getObjectMap(fileConnectionParams, PeptidesProteins.class).values()) {
            PeptidesProteins pepProt = (PeptidesProteins)pepProtObj;

            List<Long> proteinList = peptidesProteins.computeIfAbsent(pepProt.getPeptideID(), k -> new ArrayList<>());

            proteinList.add(pepProt.getProteinID());
        }
        LOGGER.info("#peptides associated to proteins: " + peptidesProteins.size());

        LOGGER.info("get peptides/scores information...");
        // map from peptideID to scores
        Map<Long, List<APeptideScores>> peptidesScores = new HashMap<>();
        for (Object scoreObject : PeptideScores.getObjectMap(fileConnectionParams, PeptideScores.class).values()) {
            PeptideScores score = (PeptideScores)scoreObject;

            List<APeptideScores> scoreList = peptidesScores.computeIfAbsent(score.getPeptideID(), k -> new ArrayList<>());

            scoreList.add(score);
        }
        LOGGER.info("#peptides associated to sores: " + peptidesScores.size());

        long emptyPSMs = 0;
        for (Object peptide : peptides) {
            if (parsePSM(peptide, false, spectraMap, massPeakMap, fileMap,
                    peptidesProteins, peptidesScores, peptidesModifications, terminalModifications,
                    aminoAcidMap, sequencesMap, annotationsMap, scoresMap,
                    compiler,
                    nodeNumbersToIdentifications, nodeNumbersToInputFiles, spectraDataMap, spectrumIdToSpectraData) == null) {
                emptyPSMs++;
            }
        }
        LOGGER.info("target peptides processed");


        // parse the decoy peptides
        LOGGER.info("get decoy peptide info...");
        peptides = Peptides_decoy.getObjectMap(fileConnectionParams, Peptides_decoy.class).values();
        LOGGER.info("#decoy peptides: " + peptides.size());

        if (!peptides.isEmpty()) {
            LOGGER.info("get decoy modifications info...");
            // map from peptideID to modifications
            peptidesModifications = new HashMap<>();
            for (Object modObj : PeptidesAminoAcidModifications_decoy.getObjectMap(fileConnectionParams, PeptidesAminoAcidModifications_decoy.class).values()) {
                APeptidesAminoAcidModifications mod = (APeptidesAminoAcidModifications)modObj;

                List<APeptidesAminoAcidModifications> modList = peptidesModifications.computeIfAbsent(mod.getPeptideID(), k -> new ArrayList<>());
                modList.add(mod);
            }
            LOGGER.info("#modified decoy peptides: " + peptidesModifications.size());

            LOGGER.info("get decoy terminal modifications info...");
            // map from peptideID to terminal modifications
            terminalModifications = new HashMap<>();
            for (Object modObj : PeptidesTerminalModifications_decoy.getObjectMap(fileConnectionParams, PeptidesTerminalModifications_decoy.class).values()) {
                APeptidesTerminalModifications termMod = (APeptidesTerminalModifications)modObj;

                List<AminoAcidModifications> termModList = terminalModifications.computeIfAbsent(termMod.getPeptideID(), k -> new ArrayList<>());

                termModList.add((AminoAcidModifications)modificationsMap.get(termMod.getTerminalModificationID()));
            }
            LOGGER.info("#terminal modified decoy peptides: " + terminalModifications.size());

            LOGGER.info("get decoy peptides/proteins information...");
            // map from peptideID to proteins
            peptidesProteins = new HashMap<>();
            for (Object pepProtObj : PeptidesProteins_decoy.getObjectMap(fileConnectionParams, PeptidesProteins_decoy.class).values()) {
                PeptidesProteins_decoy pepProt = (PeptidesProteins_decoy)pepProtObj;

                List<Long> proteinList = peptidesProteins.computeIfAbsent(pepProt.getPeptideID(), k -> new ArrayList<>());

                proteinList.add(pepProt.getProteinID());
            }
            LOGGER.info("#decoy peptides associated to proteins: " + peptidesProteins.size());

            LOGGER.info("get decoy peptides/scores information...");
            // map from peptideID to scores
            peptidesScores = new HashMap<>();
            for (Object scoreObject : PeptideScores_decoy.getObjectMap(fileConnectionParams, PeptideScores_decoy.class).values()) {
                PeptideScores_decoy score = (PeptideScores_decoy)scoreObject;

                List<APeptideScores> scoreList = peptidesScores.computeIfAbsent(score.getPeptideID(), k -> new ArrayList<>());

                scoreList.add(score);
            }
            LOGGER.info("#decoy peptides associated to sores: " + peptidesScores.size());

            for (Object peptide : peptides) {
                if (parsePSM(peptide, true, spectraMap, massPeakMap, fileMap,
                        peptidesProteins, peptidesScores, peptidesModifications, terminalModifications,
                        aminoAcidMap, sequencesMap, annotationsMap, scoresMap,
                        compiler,
                        nodeNumbersToIdentifications, nodeNumbersToInputFiles, spectraDataMap, spectrumIdToSpectraData) == null) {
                    emptyPSMs++;
                }
            }
            LOGGER.info("decoy peptides processed");
        } else {
            LOGGER.info("no decoy peptides, that's ok");
        }

        LOGGER.info("all peptides processed");

        if (emptyPSMs > 0) {
            LOGGER.info("There were " + emptyPSMs + " PSMs without protein connection, these are rejected!");
        }

        fileConnectionParams.closeDB();
        return true;
    }


    /**
     * Creates the {@link AnalysisSoftware} from the given friendlyName. If the
     * software is not known/implemented, null is returned.
     *
     * @return
     */
    private static AnalysisSoftware createAnalysisSoftware(ProcessingNodes node) {
        AnalysisSoftware software = new AnalysisSoftware();

        if (("SequestNode".equals(node.getNodeName()) && "SEQUEST".equals(node.getFriendlyName())) ||
                ("IseNode".equals(node.getNodeName()) && "Sequest HT".equals(node.getFriendlyName()))) {
            software.setId("sequest");
            software.setName("SEQUEST");

            Param param = new Param();
            param.setParam(MzIdentMLTools.createPSICvParam(OntologyConstants.SEQUEST, null));
            software.setSoftwareName(param);
        } else if ("Mascot".equals(node.getNodeName()) && "Mascot".equals(node.getFriendlyName())) {
            software.setId("mascot");
            software.setName("Mascot");
            software.setUri("http://www.matrixscience.com/");

            Param param = new Param();
            param.setParam(MzIdentMLTools.createPSICvParam(OntologyConstants.MASCOT, null));
            software.setSoftwareName(param);
        } else if ("AmandaPeptideIdentifier".equals(node.getNodeName()) && "MS Amanda".equals(node.getFriendlyName())) {
            software.setId("amanda");
            software.setName("Amanda");

            Param param = new Param();
            param.setParam(MzIdentMLTools.createPSICvParam(OntologyConstants.AMANDA, null));
            software.setSoftwareName(param);
        } else {
            // TODO: add more software
            return null;
        }

        return software;
    }


    /**
     * Parses software specific search settings and ModificationParams.
     */
    private static boolean parseSoftwareSpecificSettings(ProcessingNodes node,
            ProcessingNodeParameters processingNodeParams, ParamList additionalSearchParams,
            ModificationParams modificationParameters) {

        if ("Mascot".equals(node.getNodeName()) && "Mascot".equals(node.getFriendlyName())) {
            // Mascot settings

            if ("Instrument".equals(processingNodeParams.getParameterName())) {
                // mascot instrument
                additionalSearchParams.getCvParam().add(
                        MzIdentMLTools.createPSICvParam(OntologyConstants.MASCOT,
                                processingNodeParams.getParameterValue()));
                return true;
            } else if (processingNodeParams.getParameterName().startsWith("DynModification_")) {
                // dynamic mascot modification
                SearchModification searchMod =
                        parseModification(processingNodeParams.getValueDisplayString(), false);

                if (searchMod != null) {
                    modificationParameters.getSearchModification().add(searchMod);
                }
                return true;
            } else if (processingNodeParams.getParameterName().startsWith("Static_")
                    && !"Static_X".equals(processingNodeParams.getParameterName())) {
                // static mascot modification
                SearchModification searchMod =
                        parseModification(processingNodeParams.getValueDisplayString(), true);

                if (searchMod != null) {
                    modificationParameters.getSearchModification().add(searchMod);
                }
                return true;
            }
        } else if (("SequestNode".equals(node.getNodeName()) && "SEQUEST".equals(node.getFriendlyName())) ||
                ("IseNode".equals(node.getNodeName()) && "Sequest HT".equals(node.getFriendlyName()))) {
            // SEQUEST settings

            if (processingNodeParams.getParameterName().startsWith("DynMod_") ||
                    processingNodeParams.getParameterName().startsWith("DynNTermMod") ||
                    processingNodeParams.getParameterName().startsWith("DynCTermMod") ||
                    processingNodeParams.getParameterName().startsWith("DynamicModification")) {
                // dynamic sequest modification
                SearchModification searchMod =
                        parseModification(processingNodeParams.getValueDisplayString(), false);

                if (searchMod != null) {
                    modificationParameters.getSearchModification().add(searchMod);
                }
                return true;
            } else if (processingNodeParams.getParameterName().startsWith("StatMod_") ||
                    processingNodeParams.getParameterName().startsWith("StatNTermMod") ||
                    processingNodeParams.getParameterName().startsWith("StatCTermMod") ||
                    processingNodeParams.getParameterName().startsWith("StaticModification")) {
                // static sequest modification
                SearchModification searchMod =
                        parseModification(processingNodeParams.getValueDisplayString(), true);

                if (searchMod != null) {
                    modificationParameters.getSearchModification().add(searchMod);
                }
                return true;
            }
        } else if ("AmandaPeptideIdentifier".equals(node.getNodeName()) && "MS Amanda".equals(node.getFriendlyName())) {
            // Amanda settings

            if (processingNodeParams.getParameterName().startsWith("DynMod_")) {
                // dynamic amanda modification
                SearchModification searchMod =
                        parseModification(processingNodeParams.getValueDisplayString(), false);

                if (searchMod != null) {
                    modificationParameters.getSearchModification().add(searchMod);
                }
                return true;
            } else if (processingNodeParams.getParameterName().startsWith("StatMod_")) {
                // static amanda modification
                SearchModification searchMod =
                        parseModification(processingNodeParams.getValueDisplayString(), true);

                if (searchMod != null) {
                    modificationParameters.getSearchModification().add(searchMod);
                }
                return true;

            }
        }

        return false;
    }


    /**
     * Parses the modification from the ProteomeDiscoverer settings in the
     * {@link ProcessingNodeParameters}.
     *
     * @param modString the whole strin
     * @param isFixed fixed or variable modification
     * @return
     */
    private static SearchModification parseModification(String modString, boolean isFixed) {
        String[] split = modString.split("/");
        if (split.length < 2) {
            LOGGER.warn("Modification could not be parsed: "
                    + modString);
            return null;
        }

        split = split[1].split("Da");

        Float massShift;
        try {
            massShift = Float.parseFloat(split[0]);
        } catch (NumberFormatException e) {
            LOGGER.warn("Could not parse massShift " + split[0] + " in " +
                    modString);
            return null;
        }

        SearchModification searchMod = new SearchModification();
        searchMod.setFixedMod(isFixed);
        searchMod.setMassDelta(massShift);

        split = split[1].
                substring(split[1].indexOf('(')+1, split[1].indexOf(')')).
                split(",");

        for (String res : split) {
            if (res.contains("N-Term") || res.contains("C-Term")) {
                searchMod.getResidues().add(".");

                OntologyConstants modConstant;
                if (res.contains("N-Term")) {
                    if (res.contains("Protein")) {
                        modConstant = OntologyConstants.MODIFICATION_SPECIFICITY_PROTEIN_N_TERM;
                    } else {
                        modConstant = OntologyConstants.MODIFICATION_SPECIFICITY_PEP_N_TERM;
                    }
                } else {
                    if (res.contains("Protein")) {
                        modConstant = OntologyConstants.MODIFICATION_SPECIFICITY_PROTEIN_C_TERM;
                    } else {
                        modConstant = OntologyConstants.MODIFICATION_SPECIFICITY_PEP_C_TERM;
                    }
                }

                CvParam specificity = MzIdentMLTools.createPSICvParam(modConstant, null);

                SpecificityRules specRules = new SpecificityRules();
                specRules.getCvParam().add(specificity);
                searchMod.getSpecificityRules().add(specRules);
            } else {
                searchMod.getResidues().add(res);
            }
        }

        return searchMod;
    }


    /**
     * Creates and adds an {@link PeptideSpectrumMatch} from an peptide entry in
     * the MSF file.
     */
    private static PeptideSpectrumMatch parsePSM(Object peptideObject,
            boolean isDecoy,
            Map<Object, Object> spectraMap,
            Map<Object, Object> massPeakMap,
            Map<Object, Object> fileMap,
            Map<Long, List<Long>> peptidesProteins,
            Map<Long, List<APeptideScores>> peptidesScores,
            Map<Long, List<APeptidesAminoAcidModifications>> peptidesModifications,
            Map<Long, List<AminoAcidModifications>> terminalModifications,
            Map<Character, AminoAcids> aminoAcidMap,
            Map<Long, String> sequencesMap,
            Map<Long, String> annotationsMap,
            Map<Long, String> scoresMap,
            PIACompiler compiler,
            Map<Long, SpectrumIdentification> nodeNumbersToIdentifications,
            Map<Long, PIAInputFile> nodeNumbersToInputFiles,
            Map<Long, SpectraData> spectraDataMap,
            Map<String, Set<String>> spectrumIdToSpectraData
            ) {
        APeptides peptide = (APeptides)peptideObject;

        if (!peptidesProteins.containsKey(peptide.getPeptideID())) {
            // there is no protein information for the peptide! PD does these things...
            // for now: do not include these PSMs
            // TODO: find some better solution
            return null;
        }

        // get some spectrum information
        SpectrumHeaders spectrum = (SpectrumHeaders)spectraMap.get(peptide.getSpectrumID());
        MassPeaks massPeak = (MassPeaks)massPeakMap.get(spectrum.getMassPeakID());

        int charge = spectrum.getCharge();
        double precursorMZ = PIATools.round(massPeak.getMass(), 6);
        String sourceID = "index=" + (spectrum.getFirstScan()-1);

        // get the spectrumIdentification, which identified this peptide
        SpectrumIdentification spectrumID =
                nodeNumbersToIdentifications.get(peptide.getProcessingNodeNumber());

        if (spectrumID == null) {
            LOGGER.warn("PSM (" + sourceID + ", " + peptide.getSequence() +") does not originate from a search.");
            return null;
        } else {
            String rawFileName = ((FileInfos)fileMap.get(massPeak.getFileID())).getFileName();

            SpectraData spectraData =
                    spectraDataMap.get(massPeak.getFileID());

            if (spectraData == null) {

                spectraData = new SpectraData();

                spectraData.setId("inputfile_" + rawFileName);
                spectraData.setLocation(rawFileName);

                if (rawFileName.endsWith(".mgf")
                        || rawFileName.endsWith(".MGF")) {
                    FileFormat fileFormat = new FileFormat();

                    fileFormat.setCvParam(MzIdentMLTools.createPSICvParam(
                            OntologyConstants.MASCOT_MGF_FORMAT, null));
                    spectraData.setFileFormat(fileFormat);

                    SpectrumIDFormat idFormat = new SpectrumIDFormat();
                    idFormat.setCvParam(MzIdentMLTools.createPSICvParam(
                            OntologyConstants.MULTIPLE_PEAK_LIST_NATIVEID_FORMAT, null));
                    spectraData.setSpectrumIDFormat(idFormat);
                } else if (rawFileName.endsWith(".raw")
                        || rawFileName.endsWith("RAW")) {
                    FileFormat fileFormat = new FileFormat();
                    fileFormat.setCvParam(MzIdentMLTools.createPSICvParam(
                            OntologyConstants.THERMO_RAW_FORMAT, null));
                    spectraData.setFileFormat(fileFormat);
                }

                spectraData = compiler.putIntoSpectraDataMap(spectraData);

                spectraDataMap.put(massPeak.getFileID(), spectraData);
            }

            // look, if spectrumID has the needed spectraData, if not, add it
            Set<String> spectraDataIDs =
                    spectrumIdToSpectraData.computeIfAbsent(spectrumID.getId(), k -> new HashSet<>());
            if (!spectraDataIDs.contains(spectraData.getId())) {
                InputSpectra inputSpectra = new InputSpectra();
                inputSpectra.setSpectraData(spectraData);

                spectrumID.getInputSpectra().add(inputSpectra);
                spectraDataIDs.add(spectraData.getId());
            }

        }

        String pepSequence = peptide.getSequence();
        PIAInputFile file = nodeNumbersToInputFiles.get(peptide.getProcessingNodeNumber());

        // get the modifications
        Map<Integer, Modification> modifications = new HashMap<>();
        if (peptidesModifications.containsKey(peptide.getPeptideID())) {
            for (APeptidesAminoAcidModifications aaMod : peptidesModifications.get(peptide.getPeptideID())) {
                int loc = (int)aaMod.getPosition() + 1;

                // TODO: get the unimod modification code
                Modification modification = new Modification(
                        pepSequence.charAt(loc-1),
                        aaMod.getAminoAcidModification().getDeltaMass(),
                        aaMod.getAminoAcidModification().getModificationName(),
                        null);

                modifications.put(loc, modification);
            }
        }

        if (terminalModifications.containsKey(peptide.getPeptideID())) {
            for (AminoAcidModifications termMod : terminalModifications.get(peptide.getPeptideID())) {
                int loc;

                switch (termMod.getPositionType()) {
                case 1:
                case 3:
                    loc = 0;
                    break;
                case 2:
                case 4:
                    loc = pepSequence.length() + 1;
                    break;
                default:
                    LOGGER.error("unknown position type for terminal modification: " + termMod.getPositionType());
                    return null;
                }
                if (loc > -1) {
                    // TODO: get the unimod modification code
                    Modification modification = new Modification(
                            '.',
                            termMod.getDeltaMass(),
                            termMod.getModificationName(),
                            null);
                    modifications.put(loc, modification);
                    break;
                }
            }
        }

        PeptideSpectrumMatch psm = compiler.createNewPeptideSpectrumMatch(
                charge,
                precursorMZ,
                PIATools.round(spectrum.getMass() - getPeptideMassForCharge(1, pepSequence, aminoAcidMap, modifications), 6),
                spectrum.getRetentionTime()*60.0,
                pepSequence,
                peptide.getMissedCleavages(),
                sourceID,
                null,
                file,
                spectrumID);

        psm.setIsDecoy(isDecoy);

        // get the peptide or create it
        Peptide piaPeptide = compiler.getPeptide(pepSequence);
        if (piaPeptide == null) {
            piaPeptide = compiler.insertNewPeptide(pepSequence);
        }

        // add the spectrum to the peptide
        piaPeptide.addSpectrum(psm);

        // add the scores
        if (peptidesScores.containsKey(peptide.getPeptideID())) {
            for (APeptideScores pepScore : peptidesScores.get(peptide.getPeptideID())) {

                ScoreModelEnum scoreModel =
                        ScoreModelEnum.getModelByDescription(
                                scoresMap.get(pepScore.getScoreID()));

                ScoreModel score;
                if (!scoreModel.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
                    score = new ScoreModel(pepScore.getScoreValue(),
                            scoreModel);
                } else {
                    score = new ScoreModel(pepScore.getScoreValue(),
                            null, scoresMap.get(pepScore.getScoreID()));
                }
                psm.addScore(score);
            }
        }

        // add the modifications
        for (Map.Entry<Integer, Modification> modIt : modifications.entrySet()) {
            psm.addModification(modIt.getKey(), modIt.getValue());
        }

        // add protein infos
        for (Long proteinID : peptidesProteins.get(peptide.getPeptideID())) {
            FastaHeaderInfos fastaInfo =
                    FastaHeaderInfos.parseHeaderInfos(annotationsMap.get(proteinID));
            if (fastaInfo == null) {
                LOGGER.error("Could not parse protein annotation '" +
                        annotationsMap.get(proteinID) + "'");
                continue;
            }

            String proteinSequence = sequencesMap.get(proteinID);

            // add the Accession to the compiler (if it is not already there)
            Accession acc = compiler.getAccession(fastaInfo.getAccession());
            if (acc == null) {
                acc = compiler.insertNewAccession(
                        fastaInfo.getAccession(), proteinSequence);
            }

            acc.addFile(file.getID());

            if ((fastaInfo.getDescription() != null) &&
                    (fastaInfo.getDescription().length() > 0)) {
                acc.addDescription(file.getID(), fastaInfo.getDescription());
            }

            if ((acc.getDbSequence() == null) &&
                    (proteinSequence != null)) {
                acc.setDbSequence(proteinSequence);
            } else if ((acc.getDbSequence() != null) &&
                    (proteinSequence != null) &&
                    !acc.getDbSequence().equals(proteinSequence)) {
                if (acc.getDbSequence() != null)  {
                    if (!proteinSequence.equals(acc.getDbSequence())) {
                        LOGGER.warn("Different DBSequences found for same Accession, this is not supported!\n" +
                                "\t Accession: " + acc.getAccession() +
                                "\t'" + proteinSequence + "'\n" +
                                "\t'" + acc.getDbSequence() + "'");
                    }
                }
            }

            // add the searchDB to the accession
            for (SearchDatabaseRef dbRef
                    : nodeNumbersToIdentifications.get(peptide.getProcessingNodeNumber()).getSearchDatabaseRef()) {
                acc.addSearchDatabaseRef(dbRef.getSearchDatabase().getId());
            }

            // add the accession occurrence to the peptide
            // have to recalculate the occurrence, because it is not saved in the MSF
            if (proteinSequence != null) {
                int start = proteinSequence.indexOf(pepSequence);

                while (start > -1) {
                    piaPeptide.addAccessionOccurrence(acc, start + 1,
                            start + pepSequence.length());

                    start = proteinSequence.indexOf(pepSequence, start + 1);
                }
            } else {
                // without valid sequence, set a fake occurrence
                piaPeptide.addAccessionOccurrence(acc, 0, 0);
            }

            // now insert the connection between peptide and accession into the compiler
            compiler.addAccessionPeptideConnection(acc, piaPeptide);
        }

        compiler.insertCompletePeptideSpectrumMatch(psm);
        return psm;
    }


    /**
     * Parses the amino acids from the MSF file in a map from oneLetterCode to
     * AminoAcid
     *
     * @param spp
     * @return
     */
    private static Map<Character, AminoAcids> getAminoAcids(SimpleProgramParameters spp) {
        Map<Character, AminoAcids> aminoAcidMap = new HashMap<>(25);

        for (Object aaObj : AminoAcids.getObjectMap(spp, AminoAcids.class).values()) {
            AminoAcids aa = (AminoAcids)aaObj;

            if (aa.getOneLetterCode() != ' ') {
                aminoAcidMap.put(aa.getOneLetterCode(), aa);
            }
        }

        return aminoAcidMap;
    }


    /**
     * This method calculates the mass based on the charge and the amino acid
     * weights parsed in the MSF file
     */
    private static double getPeptideMassForCharge(int charge, String sequence,
            Map<Character, AminoAcids> aminoAcidMap, Map<Integer, Modification> modifications){
        double calculatedMass = 0.0;

        for (Character aa : sequence.toCharArray()) {
            calculatedMass += aminoAcidMap.get(aa).getMonoisotopicMass();
        }

        // check modifications
        for (Modification mod : modifications.values()) {
            calculatedMass += mod.getMass();
        }

        calculatedMass = calculatedMass + 17.002735;    // C-terminal cleavage change
        calculatedMass = calculatedMass + 1.007825;     // N-terminal cleavage change

        calculatedMass = (calculatedMass + (double)charge * PIAConstants.H_MASS.doubleValue()) / (double)charge;

        return calculatedMass;
    }


    /**
     * Checks, whether the given file looks like a Thermo MSF file
     *
     * @param fileName
     * @return
     */
    public static boolean checkFileType(String fileName) {
        boolean isMSFFile = false;
        LOGGER.debug("checking whether this is an MSF file: " + fileName);

        SimpleProgramParameters fileConnectionParams;

        // set up the DB connection to the MSF file
        fileConnectionParams = new SimpleProgramParameters(fileName, true);
        JDBCAccess jdbc = new JDBCAccess();
        jdbc.connectToExistingDB(fileName);
        fileConnectionParams.setJDBCAccess(jdbc);

        try {
            if (!ProcessingNodes.getObjectMap(fileConnectionParams, ProcessingNodes.class).isEmpty()) {
                isMSFFile = true;
            }

            fileConnectionParams.closeDB();
        } catch (Exception e) {
            LOGGER.debug("Cannot read database", e);
        } finally {
            fileConnectionParams.closeDB();
        }

        return isMSFFile;
    }
}
