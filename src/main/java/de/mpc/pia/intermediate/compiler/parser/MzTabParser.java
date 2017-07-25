package de.mpc.pia.intermediate.compiler.parser;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.intermediate.compiler.PIACachedCompiler;
import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.tools.PIAConstants;
import de.mpc.pia.tools.PIATools;
import de.mpc.pia.tools.pride.PRIDETools;

import org.apache.log4j.Logger;
import org.biojava.nbio.ontology.Term;

import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.FileFormat;
import uk.ac.ebi.jmzidml.model.mzidml.InputSpectra;
import uk.ac.ebi.jmzidml.model.mzidml.ModificationParams;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabase;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabaseRef;
import uk.ac.ebi.jmzidml.model.mzidml.SearchModification;
import uk.ac.ebi.jmzidml.model.mzidml.SpectraData;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIDFormat;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationProtocol;
import uk.ac.ebi.pride.jmztab.model.CVParam;
import uk.ac.ebi.pride.jmztab.model.FixedMod;
import uk.ac.ebi.pride.jmztab.model.Metadata;
import uk.ac.ebi.pride.jmztab.model.Mod;
import uk.ac.ebi.pride.jmztab.model.Modification;
import uk.ac.ebi.pride.jmztab.model.MsRun;
import uk.ac.ebi.pride.jmztab.model.PSM;
import uk.ac.ebi.pride.jmztab.model.PSMSearchEngineScore;
import uk.ac.ebi.pride.jmztab.model.Param;
import uk.ac.ebi.pride.jmztab.model.Protein;
import uk.ac.ebi.pride.jmztab.model.Software;
import uk.ac.ebi.pride.jmztab.model.SpectraRef;
import uk.ac.ebi.pride.jmztab.model.SplitList;
import uk.ac.ebi.pride.jmztab.model.VariableMod;
import uk.ac.ebi.pride.jmztab.utils.MZTabFileParser;
import uk.ac.ebi.pride.utilities.pridemod.model.PTM;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class read the MzTab Files and map the data to the PIA Intermediate data
 * structure
 *
 * @author julianu
 * @author Yasset Perez-Riverol (ypriverol@gmail.com)
 *
 * @date 08/02/2016
 */
public class MzTabParser {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(MzTabParser.class);


    /** the PIA compiler */
    private PIACompiler compiler;

    /** filename of the imported mzTab file */
    private String fileName;

    /** the mzTab file parser */
    private MZTabFileParser tabParser;

    /** mapping from PRIDE modification accession to modification name */
    private Map<String, String> prideModAccToName;

    /** mapping for the accessions to the parsed proteins, which contain descriptions and sequences */
    private Map<String, Protein> proteinsCache;

    /** mapping from the mzTab software id to the software in teh compiler */
    private Map<Integer, AnalysisSoftware> analysisSoftwareMap;

    /** mapping from the msRun id to the corresponding inputFile in the compiler */
    private Map<Integer, PIAInputFile> inputFileMap;

    /** mapping from the msRun id to the corresponding specIdProtocol compiler */
    private Map<Integer, SpectrumIdentificationProtocol> spectrumIdentificationProtocolMap;

    /** mapping from the msRun id to the corresponding SpectrumIdentification in the compiler */
    private Map<Integer, SpectrumIdentification> spectrumIdentificationMap;

    /** mapping from the msRun id to the corresponding spectraData in the compiler */
    private Map<Integer, SpectraData> spectraDataMap;

    /** mapping from the searchDatabases' names and version to the database in the compiler */
    private Map<String, SearchDatabase> searchDatabaseMap;

    /** maps from the msRun ids to the IDs of teh searchDatabases */
    private Map<Integer, Set<SearchDatabase>> runsToSearchDatabases;

    /** mapping for the search engine scores */
    private Map<Integer, PSMSearchEngineScore> searchEngineScores;

    /** maps from mzTab's searchEngineScore params to PIA's ScoreModels */
    private Map<Param, ScoreModel> searchEngineParamsToScoreModels;

    /** mapping from mzTabs PSM ID to the created PSM s for PIA */
    private Map<String, PeptideSpectrumMatch> psmMap;

    /** all modifications in the file */
    private ModificationParams allModificationParams;

    /** mapping from the accession given in the mzTab file to  possible SearchModifications */
    private Map<String, List<SearchModification>> mzTabaccessionToSearchModifications;


    /** logical position of the PSM decoy state column */
    private String psmDecoyStateLogicalPosition;

    /** logical position of the protein sequence column */
    private String proteinSequenceLogicalPosition;


    /** number of added accessions */
    private int accNr;

    /** number of added peptides */
    private int pepNr;

    /** number of added PSMs */
    private int psmNr;


    /** optional column header for protein sequences */
    public static final String OPTIONAL_SEQUENCE_COLUMN = "protein_sequence";

    /** optional column header for peptide decoy state */
    public static final String OPTIONAL_PEPTIDE_DECOY_COLUMN = "cv_MS:1002217_decoy_peptide";

    /** Cv label for PSI-MOD */
    private static final String CV_LABEL_PSI_MOD = "MOD";


    /**
     * We don't ever want to instantiate this class
     */
    private MzTabParser(PIACompiler compiler, String fileName) {
        this.compiler = compiler;
        this.fileName = fileName;
        this.tabParser = null;

        this.psmDecoyStateLogicalPosition = null;
        this.proteinSequenceLogicalPosition = null;
    }


    /**
     * Parse the mzTab into a PIA structure.
     *
     * @param fileName
     * @param compiler
     * @return
     */
    public static boolean getDataFromMzTabFile(String name, String fileName, PIACompiler compiler) {
        boolean retOk;

        if (compiler instanceof PIACachedCompiler) {
            LOGGER.error("Parsing of mzTab files does not work with this compiler, as the complete PSM cannot be inserted at once.");
            return false;
        }

        MzTabParser parser = new MzTabParser(compiler, fileName);

        retOk = parser.initializeParser();

        if (retOk) {
            retOk = parser.parseFile(name);
        }

        return retOk;
    }


    /**
     * Checks the given file for readability and initializes the parser
     * @return
     */
    private boolean initializeParser() {
        File mzTabFile = new File(fileName);
        boolean retOk = true;

        if (!mzTabFile.canRead()) {
            LOGGER.error("could not read '" + fileName + "'.");
            retOk = false;
        }

        if (retOk) {
            try {
                tabParser = new MZTabFileParser(mzTabFile,
                        new FileOutputStream(mzTabFile.getAbsolutePath() + "errors.out"));
            } catch (IOException e) {
                LOGGER.error("Could not create mzTab file reader", e);
                retOk = false;
            }
        }

        return retOk;
    }


    /**
     * Parses the mzTab file
     *
     * @param name the base name of the file
     * @return
     */
    private boolean parseFile(String name) {
        Metadata metadata = tabParser.getMZTabFile().getMetadata();

        parseMetadataInformation(metadata, name);

        searchDatabaseMap = new HashMap<>();
        runsToSearchDatabases = new HashMap<>();

        cacheProteins();

        accNr = 0;
        pepNr = 0;
        psmNr = 0;

        parsePSMs();

        // add the searchDatabase references for each msRun
        runsToSearchDatabases.forEach((id, searchDBs) -> {
            for (SearchDatabase searchDB : searchDBs) {
                SearchDatabaseRef searchDBRef = new SearchDatabaseRef();
                searchDBRef.setSearchDatabase(searchDB);

                spectrumIdentificationMap.get(id).getSearchDatabaseRef().add(searchDBRef);
            }
        });

        LOGGER.info("inserted new: \n\t" +
                pepNr + " peptides\n\t" +
                psmNr + " peptide spectrum matches\n\t" +
                accNr + " accessions");

        return true;
    }


    /**
     * Parses through the proteins and caches them.
     *
     * @return
     */
    private void cacheProteins() {
        proteinsCache = new  HashMap<>();
        Collection<Protein> proteins = tabParser.getMZTabFile().getProteins();

        tabParser.getMZTabFile().getProteinColumnFactory().getColumnMapping().forEach((key, value) -> {
            if (value.getHeader().endsWith(OPTIONAL_SEQUENCE_COLUMN)) {
                proteinSequenceLogicalPosition = value.getLogicPosition();
            }
        });

        if (proteins != null) {
            proteins.forEach(protein -> proteinsCache.put(protein.getAccession(), protein));
        }
    }


    /**
     * Parses all metadata from and adds its information to the compiler
     *
     * @param metadata
     * @param name the base name of the imported file
     */
    private void parseMetadataInformation(Metadata metadata, String name) {
        prideModAccToName = retrieveStringMod(metadata);
        allModificationParams = retrieveAllMods(metadata);

        // add the softwares
        analysisSoftwareMap = new HashMap<>();
        metadata.getSoftwareMap().forEach((k,v) -> addSoftwareToCompiler(v));

        // process the msRuns
        inputFileMap = new HashMap<>();
        spectraDataMap = new HashMap<>();
        spectrumIdentificationProtocolMap = new HashMap<>();
        spectrumIdentificationMap = new HashMap<>();
        metadata.getMsRunMap().forEach((k,v) -> {
            String inputFileName = name;
            if (metadata.getMsRunMap().size() > 1) {
                inputFileName += "_" + k;
            }
            addMsRunToCompiler(v, inputFileName);
        });


        searchEngineScores = metadata.getPsmSearchEngineScoreMap();
        searchEngineParamsToScoreModels = new HashMap<>();

        // TODO: add contact information
    }


    /**
     * This function compiles for mzTab all the modifications present in the file into one {@link ModificationParams},
     * including all variable and all fixed modifications.
     *
     * @param metadata
     * @return
     */
    private ModificationParams retrieveAllMods(Metadata metadata) {
        ModificationParams modifications = new ModificationParams();
        mzTabaccessionToSearchModifications = new HashMap<>();

        if (metadata.getFixedModMap() != null) {
            metadata.getFixedModMap().forEach((key, mod) -> createModificationListFromMzTabParamMod(mod).forEach(modifications.getSearchModification()::add));
        }

        if (metadata.getVariableModMap() != null) {
            metadata.getVariableModMap().forEach((key, mod) -> createModificationListFromMzTabParamMod(mod).forEach(modifications.getSearchModification()::add));
        }

        return modifications;
    }


    /**
     * Creates a list of {@link SearchModification}s from the params in the mzTab metadata. The created list is added
     * to the mzTabaccessionToSearchModifications mapping.
     *
     * @param mod Modifications
     * @return
     */
    private List<SearchModification> createModificationListFromMzTabParamMod(Mod mod) {
        Param modParam = mod.getParam();
        List<SearchModification> searchModificationList;

        if (modParam.getAccession().startsWith(CV_LABEL_PSI_MOD)) {
            Term psiModTerm = compiler.getPsiModParser().getTerm(modParam.getAccession());
            searchModificationList =
                    compiler.getPsiModParser().getUnimodEquivalentSearchModifications(psiModTerm,
                            compiler.getUnimodParser());
        } else {
            // try the PRIDE conversions
            searchModificationList = new ArrayList<>();

            SearchModification searchModification = new SearchModification();
            searchModification.setFixedMod(false);

            searchModification.getCvParam().add(PRIDETools.convertCvParam(modParam));

            float valueDeltaMass = (modParam.getValue() != null) ?
                    new Float(modParam.getValue()) : new Float(-1.0) ;
            searchModification.setMassDelta(valueDeltaMass);

            searchModificationList.add(searchModification);
        }

        if (!searchModificationList.isEmpty()) {
            mzTabaccessionToSearchModifications.put(modParam.getAccession(), searchModificationList);
        }

        return searchModificationList;
    }


    /**
     * Some translation to String modifications
     * @param metadata
     * @return
     */
    private static Map<String, String> retrieveStringMod(Metadata metadata) {
        Map<String, String> mods = new HashMap<>();

        if (metadata.getFixedModMap() != null) {
            for (FixedMod fixed : metadata.getFixedModMap().values()){
                mods.put(fixed.getParam().getAccession(), fixed.getParam().getName());
            }
        }

        if(metadata.getVariableModMap() != null){
            for (VariableMod variableMod : metadata.getVariableModMap().values()){
                mods.put(variableMod.getParam().getAccession(), variableMod.getParam().getName());
            }
        }
        return mods;
    }


    /**
     * Adds the software from the metadata to the compiler
     *
     * @param software
     * @return
     */
    private AnalysisSoftware addSoftwareToCompiler(Software software) {
        AnalysisSoftware analysisSoftware = new AnalysisSoftware();
        Param swParam = software.getParam();

        if ((swParam.getAccession() != null) && !swParam.getAccession().trim().isEmpty()) {
            analysisSoftware.setId(swParam.getAccession());
        } else {
            analysisSoftware.setId(software.getId().toString());
        }

        analysisSoftware.setName(swParam.getName());

        // set the name
        analysisSoftware.setSoftwareName(PRIDETools.convertParam(software.getParam()));

        analysisSoftware = compiler.putIntoSoftwareMap(analysisSoftware);
        analysisSoftwareMap.put(software.getId(), analysisSoftware);
        return analysisSoftware;
    }


    /**
     * Adds information from an msRun to the compiler.
     *
     * @param msRun
     * @param name the name given for the file
     */
    private void addMsRunToCompiler(MsRun msRun, String name) {
        // create the input file for the msRun
        PIAInputFile file = compiler.insertNewFile(name, fileName,
                InputFileParserFactory.InputFileTypes.MZTAB_INPUT.getFileSuffix());
        inputFileMap.put(msRun.getId(), file);

        SpectrumIdentificationProtocol spectrumIDProtocol = new SpectrumIdentificationProtocol();
        spectrumIDProtocol.setId("spectrumIdentificationProtocol");
        file.addSpectrumIdentificationProtocol(spectrumIDProtocol);
        spectrumIdentificationProtocolMap.put(msRun.getId(), spectrumIDProtocol);

        // add all modification information to all protocols for now
        spectrumIDProtocol.setModificationParams(allModificationParams);

        // if there is only one software, add it to the protocol
        if (analysisSoftwareMap.size() == 1) {
            analysisSoftwareMap.forEach((k,v) -> spectrumIDProtocol.setAnalysisSoftware(v));
        }

        SpectrumIdentification spectrumID = new SpectrumIdentification();
        spectrumID.setId("spectrumIdentification");
        spectrumID.setSpectrumIdentificationProtocol(spectrumIDProtocol);
        file.addSpectrumIdentification(spectrumID);
        spectrumIdentificationMap.put(msRun.getId(), spectrumID);

        // create spectraData information
        SpectraData newSpectraData = convertMsRunSpectraData(msRun);
        newSpectraData = compiler.putIntoSpectraDataMap(newSpectraData);
        spectraDataMap.put(msRun.getId(), newSpectraData);

        InputSpectra inputSpectra = new InputSpectra();
        inputSpectra.setSpectraData(newSpectraData);
        spectrumID.getInputSpectra().add(inputSpectra);
    }


    /**
     * Converts the MsRun to SpectraData
     *
     * @param msRun representation of spectraData in mzTab
     * @return SpectraData represented in PIA and mzIdentML
     */
    public SpectraData convertMsRunSpectraData(MsRun msRun){

        SpectraData newSpectraData = new SpectraData();
        newSpectraData.setLocation(msRun.getLocation().toString());

        if (msRun.getFormat() != null) {
            SpectrumIDFormat formatID = new SpectrumIDFormat();
            formatID.setCvParam(PRIDETools.convertCvParam(msRun.getIdFormat()));
            newSpectraData.setSpectrumIDFormat(formatID);

            FileFormat newFileFormat = new FileFormat();
            newFileFormat.setCvParam(PRIDETools.convertCvParam(msRun.getFormat()));
            newSpectraData.setFileFormat(newFileFormat);
            newSpectraData.setId(msRun.getId().toString());
            newSpectraData.setName(msRun.getReference());
        }
        return newSpectraData;
    }


    /**
     * Parse the PSMs of the mzTab file.
     */
    private void parsePSMs() {
        psmMap = new HashMap<>();

        tabParser.getMZTabFile().getPsmColumnFactory().getColumnMapping().forEach((key, value) -> {
            if (value.getHeader().endsWith(OPTIONAL_PEPTIDE_DECOY_COLUMN)) {
                psmDecoyStateLogicalPosition = value.getLogicPosition();
            }
        });

        for (PSM mzTabPSM : tabParser.getMZTabFile().getPSMs()) {
            parsePSM(mzTabPSM);
        }

    }


    /**
     * Parses a single PSM line of teh file.
     *
     * @param mzTabPSM
     */
    private void parsePSM(PSM mzTabPSM) {
        Integer charge = mzTabPSM.getCharge();

        Double precursorMZ = mzTabPSM.getExpMassToCharge();
        precursorMZ = (precursorMZ != null) ? precursorMZ : Double.NaN;

        // get the delta mass (not delta m/z)
        double deltaMass;
        if (!precursorMZ.equals(Double.NaN) && (charge != 0)
                && (mzTabPSM.getCalcMassToCharge() != null)) {
            deltaMass= (precursorMZ - mzTabPSM.getCalcMassToCharge()) * charge;
        } else {
            deltaMass = Double.NaN;
        }

        String sequence = mzTabPSM.getSequence();

        Map<Integer, de.mpc.pia.intermediate.Modification> modifications =
                transformModifications(sequence, mzTabPSM.getModifications());
        // TODO: if more than one position in the modification is encoded: generate multiple PSMs
        // TODO: add parsing of the search engines: actually only one search engine per PSM can be added to the specIdProtocol in PIA...

        List<ScoreModel> scores = parsePSMScores(mzTabPSM);

        Double rt = null;
        if ((mzTabPSM.getRetentionTime() != null) && !mzTabPSM.getRetentionTime().isEmpty()) {
            // just take the first value of the RTs, PIA only supports one RT per PSM at the moment
            rt = mzTabPSM.getRetentionTime().get(0);
        }

        // get the peptide or create it
        Peptide peptide = compiler.getPeptide(sequence);
        if (peptide == null) {
            peptide = compiler.insertNewPeptide(sequence);
            pepNr++;
        }


        for (SpectraRef spectraRef : mzTabPSM.getSpectraRef()) {
            parsePSMsSpectra(mzTabPSM, spectraRef, peptide,
                    charge, precursorMZ, deltaMass, rt, sequence,
                    scores, modifications);
        }
    }


    /**
     * This file will take a list of mzTab modifications and convert them to intermediate modifications
     * the methods needs as input the list of mztab modifications and the compiler. The metadata is necessary to
     * get the information of the modifications like names, positions, etc.
     *
     * The new version also include the probability that this modification is present.
     *
     * @param sequence
     * @param mzTabMods
     * @return
     */
    private Map<Integer, de.mpc.pia.intermediate.Modification> transformModifications(String sequence,
            SplitList<uk.ac.ebi.pride.jmztab.model.Modification> mzTabMods) {
        Map<Integer, de.mpc.pia.intermediate.Modification> modifications = new HashMap<>();

        for (uk.ac.ebi.pride.jmztab.model.Modification oldMod : mzTabMods) {
            for(Integer pos : oldMod.getPositionMap().keySet()) {
                String oldAccession = (oldMod.getType() == Modification.Type.MOD
                        && !oldMod.getAccession().startsWith("MOD")) ? "MOD:" + oldMod.getAccession(): oldMod.getAccession();

                Character charMod = (pos == 0 || pos > sequence.length()) ? '.' : sequence.charAt(pos-1);

                PTM oldPTM = compiler.getModReader().getPTMbyAccession(oldAccession);
                de.mpc.pia.intermediate.Modification mod;

                if (mzTabaccessionToSearchModifications.containsKey(oldAccession)) {
                    // there are multiple searchMods (with residues), but the needed values should be equal for all
                    SearchModification searchMod = mzTabaccessionToSearchModifications.get(oldAccession).iterator().next();
                    CvParam cvParam = searchMod.getCvParam().iterator().next();

                    BigDecimal bd = new BigDecimal(Float.toString(searchMod.getMassDelta()));
                    mod = new de.mpc.pia.intermediate.Modification(
                            charMod,
                            bd.doubleValue(),
                            cvParam.getName(),
                            cvParam.getAccession(), oldPTM.getCvLabel(),
                            transformScore(oldMod.getPositionMap().get(pos)));

                } else {
                    mod = new de.mpc.pia.intermediate.Modification(
                            charMod,
                            compiler.getModReader().getPTMbyAccession(oldAccession).getMonoDeltaMass(),
                            prideModAccToName.get(oldAccession),
                            oldAccession, oldPTM.getCvLabel(),
                            transformScore(oldMod.getPositionMap().get(pos)));
                }
                modifications.put(pos, mod);
            }
        }
        return modifications;
    }

    private List<ScoreModel> transformScore(CVParam cvParam) {
        List<ScoreModel> scores = new ArrayList<>();
        if(cvParam != null){
            ScoreModel score = new ScoreModel(Double.parseDouble(cvParam.getValue()),cvParam.getAccession(), cvParam.getName(),cvParam.getCvLabel());
            //Todo: We have some risk here for scores that are not double based.
            scores.add(score);
        }
        return scores;
    }


    /**
     * Creates a list of PIA ScoreModels for all scores of the mzTabPSM.
     *
     * @param mzTabPSM
     * @return
     */
    private List<ScoreModel> parsePSMScores(PSM mzTabPSM) {
        List<ScoreModel> scores = new ArrayList<>();

        searchEngineScores.forEach((id, mzTabScore) -> {
            ScoreModel piaScore = parsePSMScore(mzTabPSM, mzTabScore);

            if (piaScore != null) {
                scores.add(piaScore);
            }
        });

        return scores;
    }


    /**
     * Parses the given searchEngineScore of the mzTabPSM.
     *
     * @param mzTabPSM
     * @param mzTabScore
     * @return
     */
    private ScoreModel parsePSMScore(PSM mzTabPSM, PSMSearchEngineScore mzTabScore) {
        ScoreModel score = null;

        Double scoreValue = mzTabPSM.getSearchEngineScore(mzTabScore.getId());
        if (scoreValue != null) {
            Param param = mzTabScore.getParam();

            score = getBasicScoreModelForParam(param);
            score.setValue(scoreValue);
        }

        return score;
    }


    /**
     * Returns a score with value = null for the given search engine score param from mzTab
     *
     * @param searchEngineScoreParam
     * @return
     */
    private ScoreModel getBasicScoreModelForParam(Param searchEngineScoreParam) {
        if (!searchEngineParamsToScoreModels.containsKey(searchEngineScoreParam)) {
            // put a score for this param in the map
            ScoreModel score;

            ScoreModelEnum scoreType = ScoreModelEnum.getModelByDescription(searchEngineScoreParam.getAccession());

            if (ScoreModelEnum.UNKNOWN_SCORE.equals(scoreType)) {
                // still unknown -> try name of param
                scoreType = ScoreModelEnum.getModelByDescription(searchEngineScoreParam.getName());
            }

            if ((scoreType == null) || ScoreModelEnum.UNKNOWN_SCORE.equals(scoreType)) {
                throw new IllegalArgumentException("Type must not be null or of " +
                        "type UNKNOWN_SCORE: " + searchEngineScoreParam.toString());
            }

            score = new ScoreModel(null, scoreType);

            searchEngineParamsToScoreModels.put(searchEngineScoreParam, score);
        }

        ScoreModel score = searchEngineParamsToScoreModels.get(searchEngineScoreParam);
        if (ScoreModelEnum.UNKNOWN_SCORE.equals(score.getType())) {
            score = new ScoreModel(null, score.getAccession(), score.getName());
        } else {
            score = new ScoreModel(null, score.getType());
        }

        return score;
    }




    /**
     * Parses the spectra of a PSM line in the file
     *
     * @param mzTabPSM
     * @param spectraRef
     * @param peptide
     * @param charge
     * @param precursorMZ
     * @param deltaMass
     * @param rt
     * @param sequence
     * @param scores
     * @param modifications
     */
    private void parsePSMsSpectra(PSM mzTabPSM, SpectraRef spectraRef, Peptide peptide,
            int charge, double precursorMZ, double deltaMass, Double rt, String sequence,
            List<ScoreModel> scores, Map<Integer, de.mpc.pia.intermediate.Modification> modifications) {

        MsRun msRun = spectraRef.getMsRun();
        PIAInputFile piaFile = inputFileMap.get(msRun.getId());

        String psmID = createPSMKey(mzTabPSM.getPSM_ID(), modifications, charge, sequence, precursorMZ, rt);

        String sourceID = spectraRef.getReference();
        String spectraTitle = spectraRef.toString();

        PeptideSpectrumMatch psm;
        if (!psmMap.containsKey(psmID)) {
            psm = compiler.createNewPeptideSpectrumMatch(
                    charge,
                    precursorMZ,
                    deltaMass,
                    rt,
                    sequence,
                    -1,             // no way to calculate the missed cleavages w/o sequences and enzymes
                    sourceID,
                    spectraTitle,
                    piaFile,
                    spectrumIdentificationMap.get(msRun.getId()));

            compiler.insertCompletePeptideSpectrumMatch(psm);
            psmNr++;
            psmMap.put(psmID, psm);
        } else {
            psm = psmMap.get(psmID);
        }

        // update decoy state
        updatePSMsDecoyState(psm, mzTabPSM);

        // add the PSM to the peptide
        peptide.addSpectrum(psm);

        // add the modifications
        for (Map.Entry<Integer, de.mpc.pia.intermediate.Modification> mod : modifications.entrySet()) {
            psm.addModification(mod.getKey(), mod.getValue());
        }

        // add the scores
        scores.forEach(score -> {
            if (!psm.getScores().contains(score)) {
                psm.addScore(score);
            }
        });

        Accession acc = parsePSMsAccession(mzTabPSM, msRun, piaFile.getID());

        // now insert the connection between peptide and accession into the compiler
        compiler.addAccessionPeptideConnection(acc, peptide);
    }


    /**
     * Generate a key for to identify the PSM. This is necesary, as the PSM_ID is not used correctly in many mzTab
     * files.
     *
     * @param psmID
     * @param modifications
     * @param charge
     * @param sequence
     * @param precursorMZ
     * @param rt
     * @return
     */
    private static String createPSMKey(String psmID, Map<Integer, de.mpc.pia.intermediate.Modification> modifications,
            int charge, String sequence, double precursorMZ, Double rt) {
        // PSM_ID with same mods, charge, sequence (and RT and M/Z)
        String idSB = psmID + ":" +
                PeptideSpectrumMatch.getModificationString(modifications) +
                ':' +
                charge +
                ':' +
                sequence +
                ':' +
                Double.toString(PIATools.round(precursorMZ, PIAConstants.MASS_TO_CHARGE_PRECISION)) +
                ':' +
                ((rt != null) ? Double.toString((int) PIATools.round(rt, PIAConstants.RETENTION_TIME_PRECISION)) : null);

        return idSB;
    }


    /**
     * Updates the decoy state of the PSM, according to the information in the mzTab PSM line
     *
     * @param psm
     * @param mzTabPSM
     */
    private void updatePSMsDecoyState(PeptideSpectrumMatch psm, PSM mzTabPSM) {
        if (psmDecoyStateLogicalPosition != null) {
            boolean mzTabState = "1".equals(mzTabPSM.getValue(psmDecoyStateLogicalPosition).toString());

            if (psm.getIsDecoy() == null) {
                psm.setIsDecoy(mzTabState);
            } else {
                // as soon as it is no decoy, it never becomes it again
                psm.setIsDecoy(mzTabState && psm.getIsDecoy());
            }
        }
    }


    /**
     * Parses the accession information of the PSM line in the file
     *
     * @param mzTabPSM
     * @param msRun
     * @param fileID
     * @return
     */
    private Accession parsePSMsAccession(PSM mzTabPSM, MsRun msRun, Long fileID) {
        String accession = mzTabPSM.getAccession();
        Accession acc = compiler.getAccession(accession);
        if (acc == null) {
            // optional the information about the sequence
            acc = compiler.insertNewAccession(accession, null);

            SearchDatabase searchDB =
                    getOrAddSearchDatabase(mzTabPSM.getDatabase(), mzTabPSM.getDatabaseVersion());
            acc.addSearchDatabaseRef(searchDB.getId());


            runsToSearchDatabases.computeIfAbsent(msRun.getId(), k -> new HashSet<>())
                    .add(searchDB);

            accNr++;
        }

        if (proteinsCache.containsKey(accession)) {
            Protein mzTabProtein = proteinsCache.get(accession);

            if ((acc.getDbSequence() == null) && (proteinSequenceLogicalPosition != null)) {
                acc.setDbSequence(mzTabProtein.getValue(proteinSequenceLogicalPosition).toString());
            }

            if (!acc.getDescriptions().containsKey(fileID)) {
                acc.addDescription(fileID, mzTabProtein.getDescription());
            }
        }

        acc.addFile(fileID);
        return acc;
    }



    /**
     * Gets the {@link SearchDatabase} defined in the compiler and given by the name and version. If not already in the
     * compiler, it is created.
     *
     * @param databaseName
     * @param databaseVersion
     * @return
     */
    private SearchDatabase getOrAddSearchDatabase(String databaseName, String databaseVersion) {
        String dbKey = databaseName + "----" + databaseVersion;

        // add the searchDatabase if necessary
        searchDatabaseMap.computeIfAbsent(dbKey, key -> {
            SearchDatabase searchDatabase = new SearchDatabase();
            searchDatabase.setId(databaseName);
            searchDatabase.setVersion(databaseVersion);
            searchDatabase.setName(databaseName);

            return compiler.putIntoSearchDatabasesMap(searchDatabase);
        });

        return searchDatabaseMap.get(dbKey);
    }

}
