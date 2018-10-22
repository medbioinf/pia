package de.mpc.pia.intermediate.compiler.parser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.tools.CleavageAgent;
import de.mpc.pia.tools.MzIdentMLTools;
import de.mpc.pia.tools.OntologyConstants;
import de.mpc.pia.tools.PIATools;
import de.mpc.pia.tools.openms.IdXMLParser;
import de.mpc.pia.tools.openms.jaxb.DigestionEnzyme;
import de.mpc.pia.tools.openms.jaxb.FixedModification;
import de.mpc.pia.tools.openms.jaxb.IdentificationRun;
import de.mpc.pia.tools.openms.jaxb.MassType;
import de.mpc.pia.tools.openms.jaxb.PeptideHit;
import de.mpc.pia.tools.openms.jaxb.PeptideIdentification;
import de.mpc.pia.tools.openms.jaxb.ProteinHit;
import de.mpc.pia.tools.openms.jaxb.SearchParameters;
import de.mpc.pia.tools.openms.jaxb.UserParamIdXML;
import de.mpc.pia.tools.openms.jaxb.UserParamType;
import de.mpc.pia.tools.openms.jaxb.VariableModification;
import de.mpc.pia.tools.unimod.UnimodParser;
import de.mpc.pia.tools.unimod.jaxb.ModT;


/**
 * This class parses the data from an OpenMS idXML file for a given
 * {@link PIACompiler}.<br/>
 *
 * @author julian
 *
 */
public class IdXMLFileParser {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(IdXMLFileParser.class);


    /** name of the UserParam indicating the target_decoy */
    private static final String USERPARAM_NAME_TARGET_DECOY = "target_decoy";

    /** value of the UserParam for target_decoy indicating a target */
    private static final String USERPARAM_TARGET_DECOY_TARGET = "target";

    /** value of the UserParam for target_decoy indicating a decoy */
    private static final String USERPARAM_TARGET_DECOY_DECOY = "decoy";

    /** value of the UserParam for target_decoy indicating a target+decoy */
    private static final String USERPARAM_TARGET_DECOY_TARGET_AND_DECOY = "target+decoy";

    /** name of the UserParam indicating the delta mass (shift) */
    private static final String USERPARAM_NAME_DELTA_MASS = "delta";

    /** name of the UserParam indicating the protein description */
    private static final String USERPARAM_NAME_DESCRIPTION = "Description";


    /** the modification pattern like "Carbamidomethyl (C)" */
    private static final Pattern MODIFICATION_PATTERN_NAME_RESIDUE = Pattern.compile("^(.+)\\(([^)]+)\\)$");

    /** the modification pattern like "C+57.0215" */
    private static final Pattern MODIFICATION_PATTERN_RESIDUE_SHIFT = Pattern.compile("^(.*)([+-]\\d*\\.\\d*)$");


    /**
     * We don't ever want to instantiate this class
     */
    private IdXMLFileParser() {
        throw new AssertionError();
    }


    /**
     * Parses the data from an IdXML file given by its name into the given
     * {@link PIACompiler}.
     *
     * @param fileName name of the parsed file
     */
    public static boolean getDataFromIdXMLFile(String name, String fileName,
            PIACompiler compiler) {

        IdXMLParser idXMLFile;

        try {
            idXMLFile = new IdXMLParser(fileName);
        } catch (Exception e) {
            LOGGER.error("could not read '" + fileName + "'.", e);
            return false;
        }

        int accNr = 0;
        int pepNr = 0;
        int specNr = 0;
        int runCount = 0;
        for (IdentificationRun idRun : idXMLFile.getIdentificationRuns()) {

            runCount++;
            PIAInputFile file;
            if (idXMLFile.getIdentificationRuns().size() > 1) {
                file = compiler.insertNewFile(
                        name + "_run" + runCount,
                        fileName,
                        InputFileParserFactory.InputFileTypes.ID_XML_INPUT.getFileSuffix());
            } else {
                file = compiler.insertNewFile(
                        name,
                        fileName,
                        InputFileParserFactory.InputFileTypes.ID_XML_INPUT.getFileSuffix());
            }

            if (idRun.getProteinIdentification() == null) {
                LOGGER.error("This identification has no protein information, so PIA cannot use it.");
                break;
            }

            // create the analysis software and add it to the compiler
            AnalysisSoftware topp = new AnalysisSoftware();
            topp.setId("topp");
            topp.setName("TOPP software");
            topp.setUri("http://open-ms.sourceforge.net/");

            Param param = new Param();
            param.setParam(MzIdentMLTools.createPSICvParam(OntologyConstants.TOPP_SOFTWARE, null));
            topp.setSoftwareName(param);

            topp = compiler.putIntoSoftwareMap(topp);

            // define the spectrumIdentificationProtocol
            SearchParameters searchParameters = (SearchParameters)idRun.getSearchParametersRef();

            SpectrumIdentificationProtocol spectrumIDProtocol = new SpectrumIdentificationProtocol();
            spectrumIDProtocol.setId("toppAnalysis");
            spectrumIDProtocol.setAnalysisSoftware(topp);

            // only supporting "ms-ms search" for now
            param = new Param();
            param.setParam(MzIdentMLTools.createPSICvParam(OntologyConstants.MS_MS_SEARCH, null));
            spectrumIDProtocol.setSearchType(param);

            spectrumIDProtocol.setAdditionalSearchParams(new ParamList());
            if (searchParameters.getMassType().equals(MassType.MONOISOTOPIC)) {
                spectrumIDProtocol.getAdditionalSearchParams().getCvParam().add(
                        MzIdentMLTools.createPSICvParam(OntologyConstants.FRAGMENT_MASS_TYPE_MONO, null));
                spectrumIDProtocol.getAdditionalSearchParams().getCvParam().add(
                        MzIdentMLTools.createPSICvParam(OntologyConstants.PARENT_MASS_TYPE_MONO, null));
            } else {
                spectrumIDProtocol.getAdditionalSearchParams().getCvParam().add(
                        MzIdentMLTools.createPSICvParam(OntologyConstants.FRAGMENT_MASS_TYPE_AVERAGE, null));
                spectrumIDProtocol.getAdditionalSearchParams().getCvParam().add(
                        MzIdentMLTools.createPSICvParam(OntologyConstants.PARENT_MASS_TYPE_AVERAGE, null));
            }

            // Modifications
            ModificationParams modParams = processModifications(compiler,
                    searchParameters.getVariableModification(),
                    searchParameters.getFixedModification());
            spectrumIDProtocol.setModificationParams(modParams);

            // Enzymes
            Enzyme enzyme = parseEnzyme(searchParameters.getEnzyme(), searchParameters.getMissedCleavages());
            Enzymes enzymes = new Enzymes();
            enzymes.getEnzyme().add(enzyme);
            spectrumIDProtocol.setEnzymes(enzymes);


            // fragment and peptide tolerances
            Tolerance tolerance = new Tolerance();
            tolerance.getCvParam().add(
                    MzIdentMLTools.createPSICvParam(OntologyConstants.SEARCH_TOLERANCE_PLUS_VALUE,
                            Float.toString(searchParameters.getPeakMassTolerance())));
            tolerance.getCvParam().add(
                    MzIdentMLTools.createPSICvParam(OntologyConstants.SEARCH_TOLERANCE_MINUS_VALUE,
                            Float.toString(searchParameters.getPeakMassTolerance())));
            spectrumIDProtocol.setFragmentTolerance(tolerance);

            tolerance = new Tolerance();
            tolerance.getCvParam().add(
                    MzIdentMLTools.createPSICvParam(OntologyConstants.SEARCH_TOLERANCE_PLUS_VALUE,
                            Float.toString(searchParameters.getPrecursorPeakTolerance())));
            tolerance.getCvParam().add(
                    MzIdentMLTools.createPSICvParam(OntologyConstants.SEARCH_TOLERANCE_MINUS_VALUE,
                            Float.toString(searchParameters.getPrecursorPeakTolerance())));
            spectrumIDProtocol.setParentTolerance(tolerance);

            // add the protocol to the file
            file.addSpectrumIdentificationProtocol(spectrumIDProtocol);

            // create the SearchDatabase
            SearchDatabase searchDatabase = new SearchDatabase();
            searchDatabase.setId("toppDB");
            searchDatabase.setLocation(searchParameters.getDbVersion());
            if ((searchParameters.getDb() == null) || searchParameters.getDb().trim().isEmpty()) {
                // sometimes the searchdatabase gets lost in idXMLs
                searchParameters.setDb("unspecified database");
            }
            searchDatabase.setName(searchParameters.getDb());
            // databaseName
            param = new Param();
            param.setParam(MzIdentMLTools.createUserParam(searchParameters.getDb(), null, "string"));
            searchDatabase.setDatabaseName(param);
            // TODO: add taxonomy information
            // add searchDB to the compiler

            searchDatabase = compiler.putIntoSearchDatabasesMap(searchDatabase);


            // build the SpectrumIdentification
            SpectrumIdentification spectrumID = new SpectrumIdentification();
            spectrumID.setId("openmsIdentification");
            spectrumID.setSpectrumIdentificationList(null);
            spectrumID.setSpectrumIdentificationProtocol(spectrumIDProtocol);


            InputSpectra inputSpectra = new InputSpectra();
            inputSpectra.setSpectraData(createFilesSpectradata(compiler, fileName));
            spectrumID.getInputSpectra().add(inputSpectra);


            SearchDatabaseRef searchDBRef = new SearchDatabaseRef();
            searchDBRef.setSearchDatabase(searchDatabase);
            spectrumID.getSearchDatabaseRef().add(searchDBRef);

            file.addSpectrumIdentification(spectrumID);

            // go through the peptide identifications
            for (PeptideIdentification pepID : idRun.getPeptideIdentification()) {
                int[] adds = processPeptideIdentification(pepID, compiler,
                        enzyme, file, spectrumID, idRun,
                        searchDatabase.getId());
                specNr += adds[0];
                pepNr += adds[1];
                accNr += adds[2];
            }
        }

        LOGGER.info("inserted new: \n\t" +
                pepNr + " peptides\n\t" +
                specNr + " peptide spectrum matches\n\t" +
                accNr + " accessions");
        return true;
    }


    /**
     * Creates the spectraData element for the file and adds it to the compiler
     */
    private static SpectraData createFilesSpectradata(PIACompiler compiler, String fileName) {
        // an inputSpectra with spectraData is needed, though not given in idXML
        // --> take the idXML file, though not 100% correct
        SpectraData spectraData = new SpectraData();

        spectraData.setId("idXMLSpectraData");
        spectraData.setLocation(fileName);

        FileFormat fileFormat = new FileFormat();

        fileFormat.setCvParam(MzIdentMLTools.createCvParam(
                "MS:1001369", MzIdentMLTools.getCvPSIMS(), "text format", null));
        spectraData.setFileFormat(fileFormat);

        SpectrumIDFormat idFormat = new SpectrumIDFormat();
        idFormat.setCvParam(MzIdentMLTools.createCvParam(
                "MS:1000824", MzIdentMLTools.getCvPSIMS(), "no nativeID format", null));
        spectraData.setSpectrumIDFormat(idFormat);

        spectraData = compiler.putIntoSpectraDataMap(spectraData);

        return spectraData;
    }


    /**
     * Process one peptideIdentification
     *
     * @param pepID
     * @param compiler
     * @param enzyme
     * @param file
     * @param spectrumID
     * @param idRun
     * @param searchDbId
     * @return
     */
    private static int[] processPeptideIdentification(PeptideIdentification pepID, PIACompiler compiler,
            Enzyme enzyme, PIAInputFile file, SpectrumIdentification spectrumID, IdentificationRun idRun,
            String searchDbId) {
        int specNr = 0;
        int pepNr = 0;
        int accNr = 0;

        Double massToCharge = Double.valueOf(pepID.getMZ());
        Double retentionTime = Double.valueOf(pepID.getRT());

        // get sourceID, if possible
        String sourceID = getSpectrumSourceIDFromUserParams(pepID.getUserParam());
        if (sourceID == null) {
            sourceID = getSpectrumSourceIDFromSpectrumReference(pepID.getSpectrumReference());
        }
        if (sourceID == null) {
            // nothing given in this file, create an ID using M/Z and RT
            sourceID = pepID.getMZ() + "__" + pepID.getRT();
        }

        // the distinct PSMs in this pepID
        List<PeptideSpectrumMatch> hitsPSMs = new ArrayList<>();

        for (PeptideHit pepHit : pepID.getPeptideHit()) {
            if (pepHit.getProteinRefs().isEmpty()) {
                // identifications without proteins have no value (for now)
                LOGGER.error("No protein linked to the peptide identification, dropped PeptideHit for " +
                        pepHit.getSequence());
                continue;
            }

            String sequence = pepHit.getSequence();
            int charge = pepHit.getCharge().intValue();

            Map<Integer, Modification> modifications = new HashMap<>();

            sequence = extractModifications(sequence, modifications, compiler);

            double deltaMass = getDeltaMass(pepHit.getUserParam());

            int missedCleavages;
            if (enzyme.getSiteRegexp() != null) {
                missedCleavages = sequence.split(enzyme.getSiteRegexp()).length - 1;
            } else {
                missedCleavages = -1;
            }

            PeptideSpectrumMatch psm = compiler.createNewPeptideSpectrumMatch(
                    charge,
                    massToCharge,
                    deltaMass,
                    retentionTime,
                    sequence,
                    missedCleavages,
                    sourceID,
                    null,
                    file,
                    spectrumID);
            specNr++;

            // set the main score
            parsePeptideHitMainScore(psm, pepID, pepHit);


            // add additional userParams Scores
            parsePeptideHitUserParams(psm, pepHit, idRun);

            // now add the modifications
            for (Map.Entry<Integer, Modification> modIt : modifications.entrySet()) {
                psm.addModification(modIt.getKey(), modIt.getValue());
            }

            // get the peptide from the compiler or, if need be, add it
            Peptide peptide;
            peptide = compiler.getPeptide(sequence);
            if (peptide == null) {
                peptide = compiler.insertNewPeptide(sequence);
                pepNr++;
            }

            // there is a bug in OpenMS which creates sometimes multiple peptideHits with identical values originating from the same spectra
            if (!listContainsPSM(hitsPSMs, psm)) {
                compiler.insertCompletePeptideSpectrumMatch(psm);
                hitsPSMs.add(psm);

                // add the spectrum to the peptide
                peptide.addSpectrum(psm);
            }

            accNr += connectProteins(pepHit.getProteinRefs(), compiler, peptide, file.getID(), searchDbId);
        }

        return new int[]{specNr, pepNr, accNr};
    }


    /**
     * Tries to get the sourceID from the userParams of a peptideIdentification.
     *
     * @param userParams
     * @return
     */
    private static String getSpectrumSourceIDFromUserParams(List<UserParamIdXML> userParams) {
        String sourceID = null;

        // the userParam with name "spectrum_id" is erronous -> don't used anymore
        List<UserParamIdXML> sourceIdParams = userParams.stream()
                .filter(param -> OntologyConstants.SCAN_NUMBERS.getPsiAccession().equals(param.getName()))
                .collect(Collectors.toList());

        for (UserParamIdXML param : sourceIdParams) {
            try {
                sourceID = "index=" + Long.parseLong(param.getValue());
            } catch (NumberFormatException e) {
                LOGGER.warn("could not parse sourceID: " + param.getValue());
                sourceID = null;
            }
        }

        return sourceID;
    }


    /**
     * Tries to parse the sourceID from the spectrumReference of a peptideIdentification
     *
     * @param spectrumReference
     * @return
     */
    private static String getSpectrumSourceIDFromSpectrumReference(String spectrumReference) {
        String sourceID = null;

        if (spectrumReference != null) {
            Matcher matcher = MzIdentMLTools.patternScanInTitle.matcher(spectrumReference);
            if (matcher.matches()) {
                sourceID = "index=" + matcher.group(1);
            }
        }

        return sourceID;
    }


    /**
     * Parses the main score of the peptideHit. This is the one in the tag.
     * @param psm
     * @param pepID
     * @param pepHit
     */
    private static void parsePeptideHitMainScore(PeptideSpectrumMatch psm, PeptideIdentification pepID,
            PeptideHit pepHit) {
        ScoreModel score;
        ScoreModelEnum scoreModel = ScoreModelEnum.getModelByDescription(pepID.getScoreType() + "_openmsmainscore");

        if (!scoreModel.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
            score = new ScoreModel(
                    // looks weird, but so the decimals are correct
                    Double.parseDouble(String.valueOf(pepHit.getScore())),
                    scoreModel);
            psm.addScore(score);
        } else {
            // try another way
            scoreModel = ScoreModelEnum.getModelByDescription(pepID.getScoreType());

            if (!scoreModel.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
                score = new ScoreModel(
                        // looks weird, but so the decimals are correct
                        Double.parseDouble(String.valueOf(pepHit.getScore())),
                        scoreModel);
                psm.addScore(score);
            } else {
                score = new ScoreModel(
                        Double.parseDouble(String.valueOf(pepHit.getScore())),
                        pepID.getScoreType() + "_openmsmainscore",
                        pepID.getScoreType());
                psm.addScore(score);
            }
        }
    }


    /**
     * Add the userParams to the PSM.
     *
     * @param psm
     * @param pepHit
     * @param idRun
     */
    private static void parsePeptideHitUserParams(PeptideSpectrumMatch psm, PeptideHit pepHit,
            IdentificationRun idRun) {
        for (UserParamIdXML userParam : pepHit.getUserParam()) {
            // test for any (known) scores, these are always floats
            if (userParam.getType().equals(UserParamType.FLOAT)) {
                addFloatValueUserParam(psm, userParam, idRun);
            }

            // if the target / decoy is set, set it for the PSM according
            if (USERPARAM_NAME_TARGET_DECOY.equals(userParam.getName())) {
                if (USERPARAM_TARGET_DECOY_TARGET.equals(userParam.getValue())
                        || USERPARAM_TARGET_DECOY_TARGET_AND_DECOY.equals(userParam.getValue())) {
                    psm.setIsDecoy(false);
                } else if (USERPARAM_TARGET_DECOY_DECOY.equals(userParam.getValue())) {
                    psm.setIsDecoy(true);
                }
            }

            // TODO: add all userParams
        }
    }


    /**
     * Adds a userParam with a float value. These are often scores.
     * @param psm
     * @param userParam
     * @param idRun
     */
    private static boolean addFloatValueUserParam(PeptideSpectrumMatch psm, UserParamIdXML userParam,
            IdentificationRun idRun) {
        boolean paramAdded = false;

        // try to add it as a score
        ScoreModel score;
        ScoreModelEnum scoreModel = ScoreModelEnum.getModelByDescription(
                idRun.getSearchEngine() + "_" + userParam.getName());

        // if the score was not found with this, try another way
        if (scoreModel.equals(ScoreModelEnum.UNKNOWN_SCORE)
                && (userParam.getName().contains("Posterior Error Probability")
                        || userParam.getName().contains("Posterior Probability")
                        || userParam.getName().contains("Consensus_"))) {
            scoreModel = ScoreModelEnum.getModelByDescription(userParam.getName());
        }

        if (!scoreModel.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
            // a valid score was found now
            score = new ScoreModel(
                    Double.parseDouble(userParam.getValue()),
                    scoreModel);
            psm.addScore(score);
            paramAdded = true;
        }

        return paramAdded;
    }


    /**
     * Connect the protein accessions to the peptide. Add the accessions, if necessary.
     *
     * @param proteinRefs
     * @param compiler
     * @param peptide
     * @param fileID
     * @param searchDbID
     * @return
     */
    private static int connectProteins(List<Object> proteinRefs, PIACompiler compiler, Peptide peptide,
            long fileID, String searchDbID) {
        AtomicInteger addedAccs = new AtomicInteger(0);

        // filter for correct references and connect them
        proteinRefs.stream()
                .filter(ref -> ref instanceof ProteinHit)
                .forEach(protHit -> addedAccs.addAndGet(connectProtein((ProteinHit)protHit, compiler, peptide, fileID, searchDbID))
                );

        return addedAccs.intValue();
    }


    /**
     * Connect the single protein accession to the peptide. Add the accessions, if necessary.
     *
     * @param protHit
     * @param compiler
     * @param peptide
     * @param fileID
     * @param searchDbID
     * @return
     */
    private static int connectProtein(ProteinHit protHit, PIACompiler compiler, Peptide peptide,
            long fileID, String searchDbID) {
        FastaHeaderInfos fastaInfo = FastaHeaderInfos.parseHeaderInfos(protHit.getAccession());
        if (fastaInfo == null) {
            LOGGER.error("Could not parse '" + protHit.getAccession() + "'");
            return 0;
        }

        // add the Accession to the compiler (if not already added)
        Accession acc = compiler.getAccession(fastaInfo.getAccession());
        int addedAcc = 0;

        if (acc == null) {
            acc = compiler.insertNewAccession(
                    fastaInfo.getAccession(),
                    protHit.getSequence());
            addedAcc++;
        }

        if (!acc.getFiles().contains(fileID)) {
            addAccessionInformation(acc, protHit, fileID, searchDbID, fastaInfo);
        }

        // get the occurrences of the peptide
        if ((acc.getDbSequence() != null)
                && !acc.getDbSequence().trim().isEmpty()) {
            String sequence = peptide.getSequence();
            for (int start : getStartSites(sequence, acc.getDbSequence())) {
                peptide.addAccessionOccurrence(acc, start, start + sequence.length() - 1);
            }
        }

        // now insert the connection between peptide and accession into the compiler
        compiler.addAccessionPeptideConnection(acc, peptide);

        return addedAcc;
    }


    /**
     * Add additional information to the accession, like the description. Parses the userParams and, alternatively,
     * uses the parsed accession.
     *
     * @param acc
     * @param protHit
     * @param fileID
     * @param searchDbID
     * @param fastaInfo
     */
    private static void addAccessionInformation(Accession acc, ProteinHit protHit, long fileID, String searchDbID,
            FastaHeaderInfos fastaInfo) {
        acc.addFile(fileID);

        // add the searchDB to the accession
        acc.addSearchDatabaseRef(searchDbID);

        // get further protein description from the userParam
        List <UserParamIdXML> descriptionParams = protHit.getUserParam().stream()
                .filter(param -> param.getName().equalsIgnoreCase(USERPARAM_NAME_DESCRIPTION))
                .collect(Collectors.toList());

        if (!descriptionParams.isEmpty()) {
            acc.addDescription(fileID, descriptionParams.get(0).getValue());
        } else {
            // if there is no description, try the FASTA parser
            if ((fastaInfo.getDescription() != null)
                    && (fastaInfo.getDescription().length() > 0)) {
                acc.addDescription(fileID, fastaInfo.getDescription());
            }
        }
    }


    /**
     * Processes the modifications
     *
     * @param compiler
     * @param variableMods
     * @param fixedMods
     * @return
     */
    private static ModificationParams processModifications(PIACompiler compiler,
            List<VariableModification> variableMods, List<FixedModification> fixedMods) {
        ModificationParams modParams = new ModificationParams();
        for (VariableModification variableMod : variableMods) {
            SearchModification variableSearchMod = createSearchModification(variableMod.getName(), false, compiler);

            if (variableSearchMod != null) {
                modParams.getSearchModification().add(variableSearchMod);
            } else {
                LOGGER.error("Could not parse variable modification: " + variableMod.getName());
            }
        }

        for (FixedModification fixedMod : fixedMods) {
            SearchModification fixedSearchMod = createSearchModification(fixedMod.getName(), true, compiler);

            if (fixedSearchMod != null) {
                modParams.getSearchModification().add(fixedSearchMod);
            } else {
                LOGGER.error("Could not parse fixed modification: " + fixedMod.getName());
            }
        }

        return modParams;
    }


    /**
     * Creates a {@link SearchModification} from the given encoded modification.
     * The correct unimod-modification will be searched and used.
     *
     * @param encodedModification the encoded modification, either in the form "Carbamidomethyl (C)" or "C+57.0215"
     * @param isFixed
     * @return
     */
    private static SearchModification createSearchModification(String encodedModification,
            boolean isFixed, PIACompiler compiler) {
        ModT unimod = null;

        SearchModification searchMod = new SearchModification();
        searchMod.setFixedMod(isFixed);

        Matcher matcher = MODIFICATION_PATTERN_NAME_RESIDUE.matcher(encodedModification);
        if (matcher.matches()) {
            // the modification is encoded as e.g. "Carbamidomethyl (C)"

            String residuesString = matcher.group(2);
            List<String> residues;

            if (residuesString.contains("term") || residuesString.contains("Term")) {
                // get terminus, if given
                residues = addModificationSpecificity(searchMod, residuesString);
            } else {
                // get unique residues
                residues = Arrays.asList(matcher.group(2).split(" ")).stream()
                        .distinct()
                        .collect(Collectors.toList());
            }

            for (String res : residues) {
                if (res.trim().isEmpty()) {
                    searchMod.getResidues().add(".");
                } else {
                    searchMod.getResidues().add(res);
                }
            }

            unimod = compiler.getUnimodParser().getModificationByName(
                    matcher.group(1).trim(), searchMod.getResidues());
        } else {
            matcher = MODIFICATION_PATTERN_RESIDUE_SHIFT.matcher(encodedModification);
            if (matcher.matches()) {
                // the modification is encoded as e.g. "C+57.0215"
                Double massShift = Double.parseDouble(matcher.group(2));
                String residue = matcher.group(1);
                if (residue.length() < 1) {
                    residue = ".";
                }
                searchMod.getResidues().add(residue);

                unimod = compiler.getUnimodParser().getModificationByMass(
                        massShift, residue);
            }
        }

        if (unimod != null) {
            CvParam cvParam = MzIdentMLTools.createCvParam(
                    "UNIMOD:" + unimod.getRecordId(),
                    UnimodParser.getCv(),
                    unimod.getTitle(),
                    null);

            searchMod.getCvParam().add(cvParam);
            searchMod.getResidues();
            searchMod.setMassDelta(unimod.getDelta().getMonoMass().floatValue());
        } else {
            searchMod = null;
        }

        return searchMod;
    }


    /**
     * Adds the modification specificity to the searchMod, if it is given in the residueString.
     *
     * @param searchMod to this the specificity will be added
     * @param residuesString a String in the form similar to "N-Term Q"
     * @return the list of residues
     */
    private static List<String> addModificationSpecificity(SearchModification searchMod, String residuesString) {
        OntologyConstants modSpecificityConstant = null;
        if (residuesString.startsWith("Protein N")) {
            modSpecificityConstant = OntologyConstants.MODIFICATION_SPECIFICITY_PROTEIN_N_TERM;
        } else if (residuesString.startsWith("Protein C")) {
            modSpecificityConstant = OntologyConstants.MODIFICATION_SPECIFICITY_PROTEIN_C_TERM;
        } else if (residuesString.startsWith("N-")) {
            modSpecificityConstant = OntologyConstants.MODIFICATION_SPECIFICITY_PEP_N_TERM;
        } else if (residuesString.startsWith("C-")) {
            modSpecificityConstant = OntologyConstants.MODIFICATION_SPECIFICITY_PEP_C_TERM;
        }

        List<String> residues = new ArrayList<>();

        if (modSpecificityConstant != null) {
            CvParam specificity = MzIdentMLTools.createPSICvParam(modSpecificityConstant, null);

            SpecificityRules specRules = new SpecificityRules();
            specRules.getCvParam().add(specificity);
            searchMod.getSpecificityRules().add(specRules);

            String[] residuesSplit = residuesString.split("erm");
            if (residuesSplit.length > 1) {
                residues = Arrays.asList(residuesSplit[1].trim().split(" ")).stream()
                        .distinct()
                        .collect(Collectors.toList());
            } else {
                residues.add(".");
            }
        }

        return residues;
    }


    /**
     * Parses the UserParams for the deltaMass and returns it, if it was found,
     * otherwise, Double.NaN is returned.
     *
     * @param userParams
     * @return
     */
    private static Double getDeltaMass(List<UserParamIdXML> userParams) {
        Double deltaMass = Double.NaN;

        for (de.mpc.pia.tools.openms.jaxb.UserParamIdXML userParam : userParams) {

            if (USERPARAM_NAME_DELTA_MASS.equals(userParam.getName())) {
                try {
                    deltaMass = Double.parseDouble(userParam.getValue());
                } catch (NumberFormatException e) {
                    LOGGER.warn("Could not parse '" + userParam.getValue()
                            + "' as deltaMass", e);
                    deltaMass = Double.NaN;
                }

                if (!deltaMass.equals(Double.NaN)) {
                    break;
                }
            }

        }

        return deltaMass;
    }


    /**
     * Given a sequence with encoded modifications in the style
     * "LDC(Carbamidomethyl)SHA", the modifications will be extracted and put
     * into the given map and the raw sequence is returned.
     *
     * @param modSequence the sequence with encoded modifications
     * @param modifications mapping for the modifications (position to mod)
     * @param compiler
     * @return
     */
    private static String extractModifications(String modSequence, Map<Integer, Modification> modifications,
            PIACompiler compiler) {
        if (modifications == null) {
            LOGGER.error("Modifications map not initialized!");
            return null;
        }

        String modificationsSequence = trimStartAndEndDot(modSequence);
        StringBuilder sequence = new StringBuilder(modificationsSequence.length());

        int pos;
        while ( -1 < (pos = modificationsSequence.indexOf('('))) {
            sequence.append(modificationsSequence.substring(0, pos));
            modificationsSequence = modificationsSequence.substring(pos);

            String residue;
            if (sequence.length() == 0) {
                // N-terminal modification
                residue = ".";
            } else {
                residue = sequence.substring(sequence.length()-1);
            }

            int openBr = 0;
            StringBuilder modName = new StringBuilder();
            for (int p=1; p < modificationsSequence.length(); p++) {
                char c = modificationsSequence.charAt(p);

                if (c == '(') {
                    openBr++;
                } else if (c == ')') {
                    openBr--;
                }

                if (openBr < 0) {
                    break;
                }

                modName.append(c);
            }

            ModT unimod = compiler.getUnimodParser().getModificationByName(modName.toString(), residue);
            if (unimod != null) {
                Modification mod = new Modification(
                        residue.charAt(0),
                        unimod.getDelta().getMonoMass(),
                        unimod.getTitle(),
                        "UNIMOD:" + unimod.getRecordId());

                modifications.put(sequence.length(), mod);
            } else {
                LOGGER.error("Could not get information for modification " + modName + " in " + sequence);
            }

            modificationsSequence =
                    modificationsSequence.substring(modName.length() + 2);
        }
        sequence.append(modificationsSequence);

        return trimStartAndEndDot(sequence.toString().toUpperCase());
    }


    /**
     * Trims the "." character from the start and end of a sequence string.
     *
     * @param sequenceString
     * @return
     */
    private static String trimStartAndEndDot(String sequenceString) {
        String strippedSequence = sequenceString;
        if (strippedSequence.startsWith(".")) {
            strippedSequence = strippedSequence.substring(1);
        }

        if (strippedSequence.trim().endsWith(".")) {
            strippedSequence = strippedSequence.substring(0, strippedSequence.length() - 1);
        }

        return strippedSequence;
    }


    /**
     * Getter for the start sites of a given peptide in the given protein sequence.
     *
     * @param peptide
     * @param protein
     * @return
     */
    private static List<Integer> getStartSites(String peptide, String protein) {
        List<Integer> startSites = new ArrayList<>();

        String peptideSeq = peptide.toUpperCase();
        String proteinSeq = protein.toUpperCase();

        if (peptideSeq.contains("X")) {
            // replace the X by "." for regular expression matching
            peptideSeq = peptideSeq.replaceAll("X", ".");
        }

        Matcher matcher;
        matcher = Pattern.compile(peptideSeq).matcher(proteinSeq);
        while (matcher.find()) {
            startSites.add(matcher.start() + 1);
        }

        if (proteinSeq.contains("X")) {
            // needs to match around the X
            matcher = Pattern.compile("X").matcher(proteinSeq);
            while (matcher.find()) {
                int subStart = Math.max(0, matcher.start() - peptideSeq.length() + 1);
                int subEnd = Math.min(proteinSeq.length(), matcher.end() + peptideSeq.length() - 1);

                String subProteinSeq = proteinSeq.substring(subStart, subEnd);
                Integer pos = PIATools.longestCommonPeptide(peptideSeq, subProteinSeq);

                if (pos > -1) {
                    pos = subStart + pos;
                    startSites.add(pos);
                }
            }

            // clean the startSites
            startSites = startSites.stream().distinct().collect(Collectors.toList());
        }

        if (startSites.isEmpty()) {
            LOGGER.warn("no occurrences for " + peptideSeq + "    dbSeq: " + proteinSeq);
        }

        return startSites;
    }


    /**
     * Check whether the list of PSMs already contains the PSMs, without comparing the IDs
     *
     * @param psmList
     * @param psm
     * @return
     */
    private static boolean listContainsPSM(List<PeptideSpectrumMatch> psmList, PeptideSpectrumMatch psm) {
        int psmHash = psm.hashCodeWithoutID();

        // evaluate for same hashcodes and equality
        return psmList.stream().
                anyMatch(s -> (psmHash == s.hashCodeWithoutID()) && psm.equalsWithoutID(s));
    }



    /**
     * Parses the used enzyme from the OpenMS settings to mzIdentML.
     *
     * @param openMsEnzyme
     * @param missedCleavages
     * @return
     */
    private static Enzyme parseEnzyme(DigestionEnzyme openMsEnzyme, Long missedCleavages) {
        Enzyme enzyme = new Enzyme();
        enzyme.setId("enzyme");
        if (missedCleavages != null) {
            enzyme.setMissedCleavages(missedCleavages.intValue());
        }

        if (openMsEnzyme != null) {
            ParamList enzymeNameParamList = new ParamList();

            if (openMsEnzyme.equals(DigestionEnzyme.NO_ENZYME)) {
                enzymeNameParamList.getCvParam().add(
                        MzIdentMLTools.createPSICvParam(OntologyConstants.NO_CLEAVAGE, null));
            } else {
                CleavageAgent cleavageAgent = CleavageAgent.getByName(openMsEnzyme.name());

                if (cleavageAgent != null) {
                    enzymeNameParamList.getCvParam().add(
                            MzIdentMLTools.createCvParam(
                                    cleavageAgent.getAccession(),
                                    MzIdentMLTools.getCvPSIMS(),
                                    cleavageAgent.getName(),
                                    null));

                    enzyme.setSiteRegexp(cleavageAgent.getSiteRegexp());
                } else {
                    LOGGER.warn("Unknown enzyme specification: " + openMsEnzyme);
                }
            }

            if (!enzymeNameParamList.getParamGroup().isEmpty()) {
                enzyme.setEnzymeName(enzymeNameParamList);
            }
        }

        return enzyme;
    }


    /**
     * Checks, whether the given file looks like an idXML file
     *
     * @param fileName
     * @return
     */
    public static boolean checkFileType(String fileName) {
        boolean isIdXMLFile = false;
        LOGGER.debug("checking whether this is an idXML file: " + fileName);

        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            // read in the first 10, not empty lines
            List<String> lines = stream.filter(line -> !line.trim().isEmpty())
                    .limit(10)
                    .collect(Collectors.toList());

            // check, if first lines are ok
            int idx = 0;

            // optional declaration
            if (lines.get(idx).trim().matches("<\\?xml version=\"[0-9.]+\"( encoding=\"[^\"]+\"){0,1}\\?>")) {
                LOGGER.debug("file has the XML declaration line:" + lines.get(idx));
                idx++;
            }

            // optional stylesheet declaration
            if (lines.get(idx).trim().matches("<\\?xml-stylesheet.+\\?>")) {
                LOGGER.debug("file has the XML stylesheet line:" + lines.get(idx));
                idx++;
            }

            // now the IdXML element must be next
            if (lines.get(idx).trim().matches("<IdXML .+")) {
                isIdXMLFile = true;
                LOGGER.debug("file has the idXML element: " + lines.get(idx));
            }
        } catch (Exception e) {
            LOGGER.debug("Could not check file " + fileName, e);
        }

        return isIdXMLFile;
    }
}
