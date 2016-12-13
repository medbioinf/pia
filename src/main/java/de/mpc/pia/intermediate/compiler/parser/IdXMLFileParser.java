package de.mpc.pia.intermediate.compiler.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import uk.ac.ebi.jmzidml.model.mzidml.AbstractParam;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.Cv;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.Enzyme;
import uk.ac.ebi.jmzidml.model.mzidml.Enzymes;
import uk.ac.ebi.jmzidml.model.mzidml.ModificationParams;
import uk.ac.ebi.jmzidml.model.mzidml.Param;
import uk.ac.ebi.jmzidml.model.mzidml.ParamList;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabase;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabaseRef;
import uk.ac.ebi.jmzidml.model.mzidml.SearchModification;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationProtocol;
import uk.ac.ebi.jmzidml.model.mzidml.Tolerance;
import uk.ac.ebi.jmzidml.model.mzidml.UserParam;
import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.tools.PIATools;
import de.mpc.pia.tools.openms.IdXMLParser;
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


    /** name of the UserParam indicating the delta mass (shift) */
    private static final String USERPARAM_NAME_DELTA_MASS = "delta";


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

        Cv psiMS = new Cv();
        psiMS.setId("PSI-MS");
        psiMS.setFullName("PSI-MS");
        psiMS.setUri("http://psidev.cvs.sourceforge.net/viewvc/*checkout*/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo");

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
                LOGGER.error("This identification has no protein information, " +
                        "so PIA cannot use it.");
                break;
            }

            // create the analysis software and add it to the compiler
            AnalysisSoftware topp = new AnalysisSoftware();

            topp.setId("topp");
            topp.setName("TOPP software");
            topp.setUri("http://open-ms.sourceforge.net/");

            AbstractParam abstractParam;
            Param param = new Param();
            abstractParam = new CvParam();
            ((CvParam)abstractParam).setAccession("MS:1000752");
            ((CvParam)abstractParam).setCv(psiMS);
            abstractParam.setName("TOPP software");
            param.setParam(abstractParam);
            topp.setSoftwareName(param);

            topp = compiler.putIntoSoftwareMap(topp);

            // define the spectrumIdentificationProtocol
            SearchParameters searchParameters =
                    (SearchParameters)idRun.getSearchParametersRef();

            SpectrumIdentificationProtocol spectrumIDProtocol =
                    new SpectrumIdentificationProtocol();

            spectrumIDProtocol.setId("toppAnalysis");
            spectrumIDProtocol.setAnalysisSoftware(topp);

            // TODO: only supporting "ms-ms search" for now
            param = new Param();
            abstractParam = new CvParam();
            ((CvParam)abstractParam).setAccession("MS:1001083");
            ((CvParam)abstractParam).setCv(psiMS);
            abstractParam.setName("ms-ms search");
            param.setParam(abstractParam);
            spectrumIDProtocol.setSearchType(param);

            spectrumIDProtocol.setAdditionalSearchParams(new ParamList());
            if (searchParameters.getMassType().equals(MassType.MONOISOTOPIC)) {
                abstractParam = new CvParam();
                ((CvParam)abstractParam).setAccession("MS:1001256");
                ((CvParam)abstractParam).setCv(psiMS);
                abstractParam.setName("fragment mass type mono");
                spectrumIDProtocol.getAdditionalSearchParams().getCvParam().add(
                        (CvParam)abstractParam);

                abstractParam = new CvParam();
                ((CvParam)abstractParam).setAccession("MS:1001211");
                ((CvParam)abstractParam).setCv(psiMS);
                abstractParam.setName("parent mass type mono");
                spectrumIDProtocol.getAdditionalSearchParams().getCvParam().add(
                        (CvParam)abstractParam);
            } else {
                abstractParam = new CvParam();
                ((CvParam)abstractParam).setAccession("MS:1001255");
                ((CvParam)abstractParam).setCv(psiMS);
                abstractParam.setName("fragment mass type average");
                spectrumIDProtocol.getAdditionalSearchParams().getCvParam().add(
                        (CvParam)abstractParam);

                abstractParam = new CvParam();
                ((CvParam)abstractParam).setAccession("MS:1001212");
                ((CvParam)abstractParam).setCv(psiMS);
                abstractParam.setName("parent mass type average");
                spectrumIDProtocol.getAdditionalSearchParams().getCvParam().add(
                        (CvParam)abstractParam);
            }

            ModificationParams modParams = new ModificationParams();
            for (VariableModification variableMod
                    : searchParameters.getVariableModification()) {
                SearchModification variableSearchMod = createSearchModification(
                        variableMod.getName(), false, compiler);

                if (variableSearchMod != null) {
                    modParams.getSearchModification().add(variableSearchMod);
                } else {
                    LOGGER.error("Could not parse variable modification: " +
                            variableMod.getName());
                }
            }

            for (FixedModification fixedMod
                    : searchParameters.getFixedModification()) {
                SearchModification fixedSearchMod = createSearchModification(
                        fixedMod.getName(), true, compiler);

                if (fixedSearchMod != null) {
                    modParams.getSearchModification().add(fixedSearchMod);
                } else {
                    LOGGER.error("Could not parse fixed modification: " +
                            fixedMod.getName());
                }
            }
            spectrumIDProtocol.setModificationParams(modParams);


            Enzyme enzyme = new Enzyme();
            enzyme.setId("enzyme");
            if (searchParameters.getMissedCleavages() != null) {
                enzyme.setMissedCleavages(
                        searchParameters.getMissedCleavages().intValue());
            }

            if (searchParameters.getEnzyme() != null) {

                ParamList paramList = new ParamList();
                abstractParam = new CvParam();
                ((CvParam)abstractParam).setCv(psiMS);

                switch (searchParameters.getEnzyme()) {
                case NO_ENZYME:
                    ((CvParam)abstractParam).setAccession("MS:1001955");
                    abstractParam.setName("no cleavage");
                    paramList.getCvParam().add((CvParam)abstractParam);
                    break;

                case CHYMOTRYPSIN:
                    ((CvParam)abstractParam).setAccession("MS:1001306");
                    abstractParam.setName("Chymotrypsin");
                    paramList.getCvParam().add((CvParam)abstractParam);
                    enzyme.setSiteRegexp("(?<=[FYWL])(?!P)");
                    break;

                case PEPSIN_A:
                    ((CvParam)abstractParam).setAccession("MS:1001311");
                    abstractParam.setName("PepsinA");
                    paramList.getCvParam().add((CvParam)abstractParam);
                    enzyme.setSiteRegexp("(?<=[FL])");
                    break;

                case TRYPSIN:
                    ((CvParam)abstractParam).setAccession("MS:1001251");
                    abstractParam.setName("Trypsin");
                    paramList.getCvParam().add((CvParam)abstractParam);
                    enzyme.setSiteRegexp("(?<=[KR])(?!P)");
                    break;

                case PROTEINASE_K:
                case UNKNOWN_ENZYME:
                default:
                    LOGGER.warn("Unknown enzyme specification: " +
                            searchParameters.getEnzyme());
                    break;
                }

                if (!paramList.getCvParam().isEmpty()) {
                    enzyme.setEnzymeName(paramList);
                }
            }
            Enzymes enzymes = new Enzymes();
            enzymes.getEnzyme().add(enzyme);
            spectrumIDProtocol.setEnzymes(enzymes);


            Tolerance tolerance = new Tolerance();
            abstractParam = new CvParam();
            ((CvParam)abstractParam).setAccession("MS:1001412");
            ((CvParam)abstractParam).setCv(psiMS);
            abstractParam.setName("search tolerance plus value");
            abstractParam.setValue(Float.toString(searchParameters.getPeakMassTolerance()));
            tolerance.getCvParam().add((CvParam)abstractParam);

            abstractParam = new CvParam();
            ((CvParam)abstractParam).setAccession("MS:1001413");
            ((CvParam)abstractParam).setCv(psiMS);
            abstractParam.setName("search tolerance minus value");
            abstractParam.setValue(Float.toString(searchParameters.getPeakMassTolerance()));
            tolerance.getCvParam().add((CvParam)abstractParam);

            spectrumIDProtocol.setFragmentTolerance(tolerance);

            tolerance = new Tolerance();
            abstractParam = new CvParam();
            ((CvParam)abstractParam).setAccession("MS:1001412");
            ((CvParam)abstractParam).setCv(psiMS);
            abstractParam.setName("search tolerance plus value");
            abstractParam.setValue(Float.toString(searchParameters.getPrecursorPeakTolerance()));
            tolerance.getCvParam().add((CvParam)abstractParam);

            abstractParam = new CvParam();
            ((CvParam)abstractParam).setAccession("MS:1001413");
            ((CvParam)abstractParam).setCv(psiMS);
            abstractParam.setName("search tolerance minus value");
            abstractParam.setValue(Float.toString(searchParameters.getPrecursorPeakTolerance()));
            tolerance.getCvParam().add((CvParam)abstractParam);

            spectrumIDProtocol.setParentTolerance(tolerance);

            // add the protocol to the file
            file.addSpectrumIdentificationProtocol(spectrumIDProtocol);

            // create the SearchDatabase
            SearchDatabase searchDatabase = new SearchDatabase();
            searchDatabase.setId("toppDB");
            searchDatabase.setLocation(searchParameters.getDbVersion());
            searchDatabase.setName(searchParameters.getDb());
            // databaseName
            param = new Param();
            abstractParam = new UserParam();
            abstractParam.setName(searchParameters.getDb());
            param.setParam(abstractParam);
            searchDatabase.setDatabaseName(param);
            // TODO: add taxonomy information
            //if (searchParameters.getTaxonomy().trim().equals("") || searchParameters.getTaxonomy().trim().equalsIgnoreCase("All Entries")) {}
            // add searchDB to the compiler
            searchDatabase = compiler.putIntoSearchDatabasesMap(searchDatabase);


            // build the SpectrumIdentification
            SpectrumIdentification spectrumID = new SpectrumIdentification();
            spectrumID.setId("mascotIdentification");
            spectrumID.setSpectrumIdentificationList(null);
            spectrumID.setSpectrumIdentificationProtocol(spectrumIDProtocol);

            SearchDatabaseRef searchDBRef = new SearchDatabaseRef();
            searchDBRef.setSearchDatabase(searchDatabase);
            spectrumID.getSearchDatabaseRef().add(searchDBRef);

            file.addSpectrumIdentification(spectrumID);


            // go through the peptide identifications
            for (PeptideIdentification pepID : idRun.getPeptideIdentification()) {

                Double massToCharge = Double.valueOf(pepID.getMZ());
                Double retentionTime = Double.valueOf(pepID.getRT());

                String sourceID = null;
                for (UserParamIdXML userParam : pepID.getUserParam()) {
                    if (userParam.getName().equals("spectrum_id")) {
                        try {
                            sourceID = "index=" + (Integer.parseInt(userParam.getValue())-1);
                        } catch (NumberFormatException e) {
                            LOGGER.warn("could not parse sourceID: " + userParam.getValue());
                            sourceID = null;
                        }
                    }
                }

                for (PeptideHit pepHit : pepID.getPeptideHit()) {
                    if (pepHit.getProteinRefs().isEmpty()) {
                        // identifications without proteins have no value (for now)
                        LOGGER.error("No protein linked to the peptide " +
                                "identification, dropped PeptideHit for " +
                                pepHit.getSequence());
                        continue;
                    }

                    String sequence = pepHit.getSequence();
                    int charge = pepHit.getCharge().intValue();

                    Map<Integer, Modification> modifications =
                            new HashMap<>(5);

                    if (sequence.contains("(")) {
                        sequence = extractModifications(
                                sequence, modifications, compiler);
                    }

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

                    // get the peptide from the compiler or, if need be, add it
                    Peptide peptide;
                    peptide = compiler.getPeptide(sequence);
                    if (peptide == null) {
                        peptide = compiler.insertNewPeptide(sequence);
                        pepNr++;
                    }

                    // add the spectrum to the peptide
                    peptide.addSpectrum(psm);

                    // the first score is the "main" score
                    ScoreModel score;
                    ScoreModelEnum scoreModel =
                            ScoreModelEnum.getModelByDescription(
                                    pepID.getScoreType() + "_openmsmainscore");

                    if (!scoreModel.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
                        score = new ScoreModel(
                                // looks weird, but so the decimals are correct
                                Double.parseDouble(
                                        String.valueOf(pepHit.getScore())),
                                scoreModel);
                        psm.addScore(score);
                    } else {

                        // try another alternative
                        scoreModel = ScoreModelEnum.getModelByDescription(
                                pepID.getScoreType());

                        if (!scoreModel.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
                            score = new ScoreModel(
                                    // looks weird, but so the decimals are correct
                                    Double.parseDouble(
                                            String.valueOf(pepHit.getScore())),
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

                    // add additional userParams Scores
                    for (de.mpc.pia.tools.openms.jaxb.UserParamIdXML userParam
                            : pepHit.getUserParam()) {
                        // test for any other (known) scores
                        if (userParam.getType().equals(UserParamType.FLOAT)) {
                            scoreModel = ScoreModelEnum.getModelByDescription(
                                    idRun.getSearchEngine() + "_" + userParam.getName());

                            if (!scoreModel.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
                                score = new ScoreModel(
                                        Double.parseDouble(userParam.getValue()),
                                        scoreModel);
                                psm.addScore(score);
                            } else if (userParam.getName().contains("Posterior Error Probability") ||
                                    userParam.getName().contains("Posterior Probability") ||
                                    userParam.getName().contains("Consensus_")) {
                                // look for consensus score separately
                                scoreModel = ScoreModelEnum.getModelByDescription(userParam.getName());
                                if (!scoreModel.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
                                    score = new ScoreModel(
                                            Double.parseDouble(userParam.getValue()),
                                            scoreModel);
                                    psm.addScore(score);
                                }
                            }
                        }

                        // if the target / decoy is set
                        if (userParam.getName().equals("target_decoy")) {
                            if (userParam.getValue().equals("target")) {
                                psm.setIsDecoy(false);
                            } else if (userParam.getValue().equals("decoy")) {
                                psm.setIsDecoy(true);
                            }
                        }
                    }

                    // now add the modifications
                    for (Map.Entry<Integer, Modification> modIt
                            : modifications.entrySet()) {
                        psm.addModification(modIt.getKey(), modIt.getValue());
                    }

                    compiler.insertCompletePeptideSpectrumMatch(psm);

                    for (Object protRef : pepHit.getProteinRefs()) {
                        if (!(protRef instanceof ProteinHit)) {
                            LOGGER.warn("ProteinRef is not a " +
                                    ProteinHit.class.getCanonicalName());
                            continue;
                        }

                        ProteinHit protHit = (ProteinHit)protRef;

                        FastaHeaderInfos fastaInfo = FastaHeaderInfos.parseHeaderInfos(protHit.getAccession());
                        if (fastaInfo == null) {
                            LOGGER.error("Could not parse '" +
                                    protHit.getAccession() + "'");
                            continue;
                        }

                        // add the Accession to the compiler (if not already added)
                        Accession acc = compiler.getAccession(fastaInfo.getAccession());

                        if (acc == null) {
                            acc = compiler.insertNewAccession(
                                    fastaInfo.getAccession(),
                                    protHit.getSequence());
                            accNr++;
                        }

                        acc.addFile(file.getID());

                        if ((fastaInfo.getDescription() != null) &&
                                (fastaInfo.getDescription().length() > 0)) {
                            acc.addDescription(file.getID(),
                                    fastaInfo.getDescription());
                        }

                        // add the searchDB to the accession
                        acc.addSearchDatabaseRef(searchDatabase.getId());

                        // get the occurrences of the peptide
                        if ((acc.getDbSequence() != null) && (acc.getDbSequence().trim().length() > 0)) {
                            List<Integer> startSites = getStartSites(sequence, acc.getDbSequence());
                            for (Integer start : startSites) {
                                peptide.addAccessionOccurrence(acc, start, start + sequence.length() - 1);
                            }
                        }

                        // now insert the connection between peptide and accession into the compiler
                        compiler.addAccessionPeptideConnection(acc, peptide);
                    }
                }
            }
        }

        LOGGER.info("inserted new: \n\t" +
                pepNr + " peptides\n\t" +
                specNr + " peptide spectrum matches\n\t" +
                accNr + " accessions");
        return true;
    }



    /**
     * Creates a {@link SearchModification} from the given encoded modification.
     * The correct unimod-modification will be searched and used.
     *
     * @param encodedModification the encoded modification, either in the form
     * "Carbamidomethyl (C)" or "C+57.0215"
     * @param isFixed
     * @return
     */
    private static SearchModification createSearchModification(
            String encodedModification, boolean isFixed, PIACompiler compiler) {
        ModT unimod = null;

        SearchModification searchMod = new SearchModification();
        searchMod.setFixedMod(isFixed);

        Pattern pattern = Pattern.compile("^(.+)\\(([^)]+)\\)$");
        Matcher matcher = pattern.matcher(encodedModification);
        if (matcher.matches()) {
            // the modification is encoded as e.g. "Carbamidomethyl (C)"

            // add the residues
            for (String res : matcher.group(2).split(" ")) {
                if (res.length() > 1) {
                    if (!searchMod.getResidues().contains(".")) {
                        searchMod.getResidues().add(".");
                    }
                } else {
                    if (!searchMod.getResidues().contains(res)) {
                        searchMod.getResidues().add(res);
                    }
                }
            }

            unimod = compiler.getUnimodParser().getModificationByName(
                    matcher.group(1).trim(), searchMod.getResidues());
        } else {
            // the modification is encoded as e.g. "C+57.0215"
            pattern = Pattern.compile("^(.*)([+-]\\d*\\.\\d*)$");
            matcher = pattern.matcher(encodedModification);

            if (matcher.matches()) {
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
            CvParam cvParam = new CvParam();
            cvParam.setAccession("UNIMOD:" + unimod.getRecordId());
            cvParam.setCv(UnimodParser.getCv());
            cvParam.setName(unimod.getTitle());
            searchMod.getCvParam().add(cvParam);

            searchMod.getResidues();

            searchMod.setMassDelta(
                    unimod.getDelta().getMonoMass().floatValue());

            return searchMod;
        } else {
            return null;
        }
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
     * @param modificationsSequence the sequence with encoded modifications
     * @param modifications mapping for the modifications (position to mod)
     * @param compiler
     * @return
     */
    private static String extractModifications(String modificationsSequence,
            Map<Integer, Modification> modifications,
            PIACompiler compiler) {
        if (modifications == null) {
            LOGGER.error("Modifications map not initialized!");
            return null;
        }

        StringBuilder sequence =
                new StringBuilder(modificationsSequence.length());

        int pos;
        while ( -1 < (pos = modificationsSequence.indexOf('('))) {
            sequence.append(modificationsSequence.substring(0, pos));
            modificationsSequence = modificationsSequence.substring(pos);

            String residue;
            if (sequence.length() == 0) {
                // TODO: how are C-terminal modifications encoded in idXML!
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
                    if (openBr < 0) {
                        break;
                    }
                }

                modName.append(c);
            }

            ModT unimod = compiler.getUnimodParser().getModificationByName(
                    modName.toString(), residue);
            if (unimod != null) {
                Modification mod = new Modification(
                        residue.charAt(0),
                        unimod.getDelta().getMonoMass(),
                        unimod.getTitle(),
                        "UNIMOD:" + unimod.getRecordId());

                modifications.put(sequence.length(), mod);
            } else {
                LOGGER.error("Could not get information for " +
                        "modification " + modName + " in " +
                        sequence);
            }

            modificationsSequence =
                    modificationsSequence.substring(modName.length() + 2);
        }
        sequence.append(modificationsSequence);
        return sequence.toString().toUpperCase();
    }

    /**
     * Getter for the start sites of a given peptide in the given protein
     * sequence.
     *
     * @param peptideSeq
     * @param proteinSeq
     * @return
     */
    private static List<Integer> getStartSites(String peptideSeq, String proteinSeq) {
        List<Integer> startSites = new ArrayList<>();
        proteinSeq = proteinSeq.toUpperCase();

        if (peptideSeq.contains("X")) {
            // replace the X by "." for regular expression matching
            peptideSeq = peptideSeq.replaceAll("X", ".");
        }

        Matcher matcher;
        matcher = Pattern.compile(peptideSeq).matcher(proteinSeq);
        while (matcher.find()) {
            startSites.add(matcher.start() + 2);
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
                    if (!startSites.contains(pos)) {
                        startSites.add(pos);
                    }
                }
            }
        }

        if (startSites.isEmpty()) {
            LOGGER.warn("no occurrences for " + peptideSeq + "    dbSeq: " + proteinSeq);
        }

        return startSites;
    }
}
