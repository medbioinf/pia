package de.mpc.pia.intermediate.compiler.parser;

import de.mpc.pia.intermediate.*;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.modeller.IdentificationKeySettings;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.tools.MzIdentMLTools;
import de.mpc.pia.tools.OntologyConstants;
import de.mpc.pia.tools.PIAConstants;
import de.mpc.pia.tools.pride.PRIDETools;
import de.mpc.pia.tools.pride.PrideSoftwareList;
import de.mpc.pia.tools.unimod.UnimodParser;
import de.mpc.pia.tools.unimod.jaxb.ModT;

import org.apache.log4j.Logger;
import org.biojava.nbio.ontology.Term;

import uk.ac.ebi.jmzidml.model.mzidml.*;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.Param;
import uk.ac.ebi.jmzidml.model.mzidml.UserParam;
import uk.ac.ebi.pride.jaxb.model.*;
import uk.ac.ebi.pride.jaxb.model.SourceFile;
import uk.ac.ebi.pride.jaxb.xml.PrideXmlReader;
import uk.ac.ebi.pride.utilities.mol.MoleculeUtilities;

import java.io.File;
import java.util.*;

/**
 * This class reads the PRIDE XML files and maps the structure into the PIA
 * intermediate structure
 *
 * @author Yasset Perez-Riverol (ypriverol@gmail.com)
 * @author julianu
 * @date 08/02/2016
 */
public class PrideXMLParser {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(PrideXMLParser.class);

    private static final Set<String> chargeAccessions = new HashSet<>(Arrays.asList(new String[]{
            "PSI:1000041",
            "MS:1000041"}));

    private static final Set<String> mzAccessions = new HashSet<>(Arrays.asList(new String[]{
            "PSI:1000040",
            "MS:1000040",
            "PSI:1000744",
            "MS:1000744"}));

    private static final Set<String> rtAccessions = new HashSet<>(Arrays.asList(new String[]{
            "PRIDE:0000203",
            "PSI:1000894",
            "MS:1000894",
            "PSI:1000016",
            "MS:1000016"}));


    /**
     * We don't ever want to instantiate this class
     */
    private PrideXMLParser() {
        throw new AssertionError();
    }

    /**
     * Parse the PRIDE Xml into a PIA structure, The concept of PSM is not provided in PRIDE Files
     * this is the main reason why we will considered peptides as PSMs here.
     *
     * @param fileName
     * @param compiler
     * @return
     */
    public static boolean getDataFromPrideXMLFile(String fileName,
            PIACompiler compiler) {
        // Open the input mzIdentML file for parsing
        File prideFile = new File(fileName);

        if (!prideFile.canRead()) {
            LOGGER.error("could not read '" + fileName + "' for PRIDE XML parsing.");
            return false;
        }

        PrideXmlReader prideParser = new PrideXmlReader(prideFile);

        String name = prideParser.getExpShortLabel();
        PIAInputFile file = compiler.insertNewFile(name,
                fileName,
                InputFileParserFactory.InputFileTypes.PRIDEXML_INPUT.getFileTypeName());


        // add the spectraData
        // the spectra are from the original search file, e.g. mascot dat
        SpectraData spectraData;
        spectraData = new SpectraData();
        spectraData.setId("sourceFile");

        // define the spectrumIdentificationProtocol
        SpectrumIdentificationProtocol spectrumIDProtocol =
                        new SpectrumIdentificationProtocol();

        // get the cleavage enzyme(s)
        Enzymes enzymes = PRIDETools.getEnzymesFromProtocol(prideParser.getProtocol());
        if (enzymes != null) {
            spectrumIDProtocol.setEnzymes(enzymes);
        }

        // add all additional information
        ParamList additionalInformation = new ParamList();
        StringBuilder sourceIdBuilder = new StringBuilder();

        prideParser.getAdditionalParams();
        additionalInformation.getUserParam().add(
                MzIdentMLTools.createUserParam("PRIDE XML conversion", null, null));

        parseAdminInformations(prideParser.getAdmin(), additionalInformation,
                spectraData, sourceIdBuilder);
        parseInstrumentInformations(prideParser.getInstrument(), additionalInformation, compiler);
        parseDataProcessingInformations(prideParser.getDataProcessing(),
                spectrumIDProtocol, additionalInformation, compiler);
        parseAdditionalInformations(prideParser.getAdditionalParams(), additionalInformation);

        spectrumIDProtocol.setAdditionalSearchParams(additionalInformation);
        file.addSpectrumIdentificationProtocol(spectrumIDProtocol);

        // add the spectrum identification
        SpectrumIdentification spectrumID = new SpectrumIdentification();
        spectrumID.setId("prideIdentification");
        spectrumID.setSpectrumIdentificationList(null);
        spectrumID.setSpectrumIdentificationProtocol(spectrumIDProtocol);

        if ((spectraData.getLocation() != null) && !spectraData.getLocation().isEmpty()) {
            spectraData = compiler.putIntoSpectraDataMap(spectraData);

            InputSpectra inputSpectra = new InputSpectra();
            inputSpectra.setSpectraData(spectraData);
            spectrumID.getInputSpectra().add(inputSpectra);
        }

        file.addSpectrumIdentification(spectrumID);


        String sourceIdBase = sourceIdBuilder.toString();

        int accNr = 0;
        int pepNr = 0;
        int specNr = 0;

        boolean decoysFound = false;

        // to check, whether the PSM is already there, we need the psmSetSettings map
        Map<String, Boolean> psmSetSettings = new HashMap<>();
        psmSetSettings.put(IdentificationKeySettings.SOURCE_ID.name(), true);
        psmSetSettings.put(IdentificationKeySettings.SEQUENCE.name(), true);
        psmSetSettings.put(IdentificationKeySettings.MODIFICATIONS.name(), true);
        psmSetSettings.put(IdentificationKeySettings.CHARGE.name(), true);

        // map to store the already created PSMs
        Map<String, PeptideSpectrumMatch> keysToPSMs = new HashMap<>();

        // mapping from the enzyme accessions to regular expressions of the enzyme
        Map<String, String> enzymesToRegexes = new HashMap<>();

        // stores the modifications
        Set<Modification> foundModifications = new HashSet<>();

        // go through the identifications (they refer to accessions)
        for (String identifier: prideParser.getIdentIds()) {
            Identification identification = prideParser.getIdentById(identifier);

            // add the Accession to the compiler (if it is not already there)
            Accession acc = compiler.getAccession(identification.getAccession());
            if (acc == null) {
                // no sequence information available in the PRIDE XML
                acc = compiler.insertNewAccession(
                        identification.getAccession(), null);
                accNr++;

                uk.ac.ebi.pride.jaxb.model.CvParam descParam =
                        identification.getAdditional().getCvParamByAcc("PRIDE:0000063");
                if (descParam != null) {
                    acc.addDescription(file.getID(), descParam.getValue());
                }
            }
            acc.addFile(file.getID());

            // add the searchDB to the accession
            if ((identification.getDatabase() != null) &&
                    !identification.getDatabase().isEmpty()) {
                SearchDatabase sDB = createSearchDatabase(
                        identification.getDatabase(), identification.getDatabaseVersion());
                sDB = compiler.putIntoSearchDatabasesMap(sDB);
                acc.addSearchDatabaseRef(sDB.getId());
            }

            for (PeptideItem peptideItem : identification.getPeptideItem()) {
                String sequence = peptideItem.getSequence();

                Spectrum spectrum = peptideItem.getSpectrum();
                SpectrumDesc spectrumDesc = spectrum.getSpectrumDesc();

                Integer charge = null;
                String chargeStr = getValueFromSpectrumPrecursor(spectrumDesc, chargeAccessions);
                if (chargeStr != null) {
                    charge = Integer.parseInt(chargeStr);
                }
                String sourceID = sourceIdBase + spectrum.getId();

                Map<Integer, Modification> modifications =
                        transformModifications(sequence, peptideItem.getModificationItem(), compiler.getUnimodParser());

                foundModifications.addAll(modifications.values());

                String psmKey = PeptideSpectrumMatch.getIdentificationKey(
                        psmSetSettings,
                        sequence,
                        PeptideSpectrumMatch.getModificationString(modifications),  // no different rounding in the same file, so this should be safe
                        charge,
                        null,
                        null,
                        sourceID,
                        null,
                        null);

                Peptide peptide;

                PeptideSpectrumMatch psm = keysToPSMs.get(psmKey);
                if (psm == null) {
                    String mzStr = getValueFromSpectrumPrecursor(spectrumDesc, mzAccessions);
                    Double precursorMZ;
                    double deltaMass = Double.NaN;
                    if (mzStr != null) {
                        precursorMZ = Double.parseDouble(mzStr);

                        double theoreticalMass = MoleculeUtilities.calculateTheoreticalMass(sequence,
                                getPtmMassesForTheoreticalMass(modifications));
                        double precursorMass = precursorMZ*charge -
                                charge*PIAConstants.H_MASS.doubleValue();
                        deltaMass = precursorMass - theoreticalMass;
                    } else {
                        precursorMZ = Double.NaN;
                    }

                    Double rt = null;
                    String rtStr = getValueFromSpectrumPrecursor(spectrumDesc, rtAccessions);
                    if (rtStr != null) {
                        if (rtStr.contains("-")) {
                            rtStr = rtStr.split("-")[0].trim();
                        }
                        rt = Double.parseDouble(rtStr);
                    }

                    int missedCleavages = MzIdentMLTools.calculateMissedCleavages(sequence,
                            enzymes, enzymesToRegexes, compiler.getOBOMapper());

                    psm = compiler.createNewPeptideSpectrumMatch(
                            charge,
                            precursorMZ,
                            deltaMass,
                            rt,
                            sequence,
                            missedCleavages,
                            sourceID,
                            null,
                            file,
                            spectrumID);
                    specNr++;
                    keysToPSMs.put(psmKey, psm);

                    // get the peptide or create it
                    peptide = compiler.getPeptide(sequence);
                    if (peptide == null) {
                        peptide = compiler.insertNewPeptide(sequence);
                        pepNr++;
                    }

                    // add the spectrum to the peptide
                    peptide.addSpectrum(psm);

                    // add the modifications
                    for (Map.Entry<Integer, Modification> mod
                            : modifications.entrySet()) {
                        psm.addModification(mod.getKey(), mod.getValue());
                    }
                } else {
                    // if the PSM is already in the compiler, the peptide must be there as well
                    peptide = compiler.getPeptide(sequence);
                    if (peptide == null) {
                        LOGGER.error("The peptide " + sequence + " was not found in the compiler!");
                        continue;
                    }
                }

                // setting of decoy parameter, but only, if it was calculated anywhere
                boolean isDecoy = PRIDETools.isDecoyHit(identification);
                if (isDecoy) {
                    decoysFound = true;
                }
                if ((psm.getIsDecoy() == null) || psm.getIsDecoy()) {
                    // either not set, or it is a decoy (which may become target)
                    psm.setIsDecoy(isDecoy);
                }


                // add the scores (if not already in the PSM)
                List<ScoreModel> scores = transformScoreModels(peptideItem.getAdditional());

                for (ScoreModel score : scores) {
                    if (!psm.getScores().contains(score)) {
                        psm.addScore(score);
                    }
                }

                // add the accession occurrence to the peptide
                peptide.addAccessionOccurrence(acc,
                        peptideItem.getStart().intValue(), peptideItem.getEnd().intValue());

                // now insert the connection between peptide and accession into the compiler
                compiler.addAccessionPeptideConnection(acc, peptide);

                // add the PSM in the compiler (or overwrite it), this might give warning
                compiler.insertCompletePeptideSpectrumMatch(psm);

                // TODO: this should be restructured: first get a list of all peptides, then add them to avoid adding a PSM multiple times
            }
        }

        // if any modifications were found, add them to the spectrumIDProtocol
        if (!foundModifications.isEmpty()) {
            spectrumIDProtocol.setModificationParams(
                    createModificationParams(foundModifications, compiler.getUnimodParser()));
        }

        // go through all PSMs and delete decoy information (if none were found)
        if (!decoysFound) {
            LOGGER.debug("resetting all decoy information, because no decoys were found in the file");
            for (PeptideSpectrumMatch psm : keysToPSMs.values()) {
                psm.setIsDecoy(null);
            }
        }

        LOGGER.info("inserted new: \n\t" +
                pepNr + " peptides\n\t" +
                specNr + " peptide spectrum matches\n\t" +
                accNr + " accessions");
        return true;
    }

    /**
     * Retrieving the scores at PSM level from PRIDE XMLs
     *
     * @param additional
     * @return
     */
    private static List<ScoreModel> transformScoreModels(uk.ac.ebi.pride.jaxb.model.Param additional) {
        List<ScoreModel> scoreModels = new ArrayList<>();

        for (uk.ac.ebi.pride.jaxb.model.CvParam cvParam : additional.getCvParam()) {
            ScoreModelEnum model = ScoreModelEnum.getModelByDescription(cvParam.getAccession());
            if(model == null){
                model = ScoreModelEnum.getModelByDescription(cvParam.getName());
            }
            if ((model != null) && (model != ScoreModelEnum.UNKNOWN_SCORE)) {
                Double value = new Double(cvParam.getValue());
                scoreModels.add(new ScoreModel(value, model));
            }
        }
        return scoreModels;
    }


    /**
     * Converting the modification Items to intermediate modifications
     *
     * @param sequence
     * @param modificationItem
     * @return
     */
    private static Map<Integer, Modification> transformModifications(String sequence,
            List<ModificationItem> modificationItem, UnimodParser unimodParser) {
        if(modificationItem != null && !modificationItem.isEmpty()) {
            Map<Integer, Modification> modificationMap = new HashMap<>();

            for (ModificationItem mod : modificationItem) {
                Double mass = null;
                Integer location = mod.getModLocation().intValue();

                Character residue;
                if ((location == 0) || (location > sequence.length())) {
                    residue = '.';
                } else {
                    residue = sequence.charAt(mod.getModLocation().intValue() - 1);
                }

                if ((mod.getModMonoDelta() != null) && (mod.getModMonoDelta().get(0) != null)) {
                    mass = new Double(mod.getModMonoDelta().get(0));
                }

                Modification modification;
                ModT unimod = unimodParser.getModificationByMass(mass, residue.toString());
                if (unimod != null) {
                    modification = new Modification(
                            residue,
                            unimod.getDelta().getMonoMass(),
                            unimod.getTitle(),
                            "UNIMOD:" + unimod.getRecordId());
                } else {
                    modification = new Modification(
                            residue,
                            mass,
                            null,
                            null);
                }

                modificationMap.put(location, modification);
            }
            return modificationMap;
        }
        return Collections.emptyMap();
    }


    /**
     * Converting the modification Items to intermediate modifications
     *
     * @param modifications
     * @return
     */
    private static double[] getPtmMassesForTheoreticalMass(Map<Integer, Modification> modifications) {
        if(modifications != null && !modifications.isEmpty()) {
            double[] ptmMasses = new double[modifications.size()+1];

            int i=0;
            for (Modification mod : modifications.values()) {
                ptmMasses[i++] = mod.getMass();
            }

            // add the ubiquous water loss
            ptmMasses[i] = PIAConstants.DEHYDRATION_MASS.doubleValue();
            return ptmMasses;
        }
        return new double[]{PIAConstants.DEHYDRATION_MASS.doubleValue()};
    }


    /**
     * Returns the first value in the spectrum precursor's ionSelection params,
     * that has one of the accessions in the given set.
     *
     * @param spectrumDesc
     * @param paramAccessions
     * @return
     */
    private static String getValueFromSpectrumPrecursor(SpectrumDesc spectrumDesc,
            Set<String> paramAccessions) {
        if ((spectrumDesc == null) || (paramAccessions == null)) {
            return null;
        }

        PrecursorList precursorList = spectrumDesc.getPrecursorList();
        if (precursorList != null && precursorList.getCount() > 0) {
            for (Precursor precursor: precursorList.getPrecursor()) {
                uk.ac.ebi.pride.jaxb.model.Param param = precursor.getIonSelection();
                if ((param != null) && (param.getCvParam() != null)) {
                    for (uk.ac.ebi.pride.jaxb.model.CvParam cvParam : param.getCvParam()) {
                        if (paramAccessions.contains(cvParam.getAccession())) {
                            return cvParam.getValue();
                        }
                    }
                }
            }
        }

        return null;
    }


    /**
     * Converts a cvParam from PRIDE to mzIdentML representation
     * @param param
     * @return
     */
    private static CvParam convertCvParam(uk.ac.ebi.pride.jaxb.model.CvParam param) {
        if(param != null){
            CvParam cvParam = new CvParam();

            if ((param.getAccession() != null) && (!param.getAccession().isEmpty())) {
                cvParam.setAccession(param.getAccession());
            }
            if ((param.getName() != null) && (!param.getName().isEmpty())) {
                cvParam.setName(param.getName());
            }
            if ((param.getValue() != null) && (!param.getValue().isEmpty())) {
                cvParam.setValue(param.getValue());
            }
            if ((param.getCvLabel() != null && (!param.getCvLabel().isEmpty()))) {
                cvParam.setUnitAccession(param.getCvLabel());

                if (param.getCvLabel().equals("MS")) {
                    cvParam.setCv(MzIdentMLTools.getCvPSIMS());
                }
            }

            return cvParam;
        }
        return null;
    }


    /**
     * Converts a userParam from PRIDE to mzIdentML representation
     * @param param
     * @return
     */
    private static UserParam convertUserParam(uk.ac.ebi.pride.jaxb.model.UserParam param) {
        if(param != null){
            UserParam userParam = new UserParam();
            if ((param.getName() != null) && (!param.getName().isEmpty())) {
                userParam.setName(param.getName());
            }
            if ((param.getValue() != null) && (!param.getValue().isEmpty())) {
                userParam.setValue(param.getValue());
            }
            return userParam;
        }
        return null;
    }


    /**
     * Parses the admin section of the PRIDE XML file
     *
     * @param adminSection
     * @param additionalInformation
     */
    private static void parseAdminInformations(Admin adminSection,
            ParamList additionalInformation, SpectraData spectraData, StringBuilder sourceIdBase) {
        AbstractParam abstractParam;

        // the sample name
        if (adminSection.getSampleName() != null) {
            abstractParam = MzIdentMLTools.createCvParam(
                    "MS:1000002",
                    MzIdentMLTools.getCvPSIMS(),
                    "sample name",
                    adminSection.getSampleName());
            additionalInformation.getCvParam().add((CvParam)abstractParam);
        }

        // all additional information in there
        for (uk.ac.ebi.pride.jaxb.model.CvParam param : adminSection.getSampleDescription().getCvParam()) {
            abstractParam = convertCvParam(param);
            if (abstractParam != null) {
                additionalInformation.getCvParam().add((CvParam)abstractParam);
            }
        }

        for (uk.ac.ebi.pride.jaxb.model.UserParam param : adminSection.getSampleDescription().getUserParam()) {
            abstractParam = convertUserParam(param);
            if (abstractParam != null) {
                additionalInformation.getUserParam().add((UserParam)abstractParam);
            }
        }

        // the original source file
        if (adminSection.getSourceFile() != null) {

            SpectraData specData = spectraDataFromSourceFile(adminSection.getSourceFile(), sourceIdBase);

            if (specData != null) {
                spectraData.setLocation(adminSection.getSourceFile().getPathToFile());
                spectraData.setName(adminSection.getSourceFile().getNameOfFile());
                spectraData.setSpectrumIDFormat(specData.getSpectrumIDFormat());
                spectraData.setFileFormat(specData.getFileFormat());
            } else {
                // it's no spectra data -> put into additional search params
                abstractParam = MzIdentMLTools.createUserParam(
                        "original file name",
                        adminSection.getSourceFile().getNameOfFile(),
                        "string");
                additionalInformation.getUserParam().add((UserParam)abstractParam);

                abstractParam = MzIdentMLTools.createUserParam(
                        "original file path",
                        adminSection.getSourceFile().getPathToFile(),
                        "string");
                additionalInformation.getUserParam().add((UserParam)abstractParam);

                abstractParam = MzIdentMLTools.createUserParam(
                        "original file type",
                        adminSection.getSourceFile().getFileType(),
                        "string");
                additionalInformation.getUserParam().add((UserParam)abstractParam);
            }
        }

        // contact information
        if ((adminSection.getContact() != null) && !adminSection.getContact().isEmpty()) {
            for (Contact contact : adminSection.getContact()) {
                abstractParam = MzIdentMLTools.createPSICvParam(
                        OntologyConstants.CONTACT_NAME, contact.getName());
                additionalInformation.getCvParam().add((CvParam)abstractParam);

                abstractParam = MzIdentMLTools.createPSICvParam(
                        OntologyConstants.CONTACT_AFFILIATION, contact.getInstitution());
                additionalInformation.getCvParam().add((CvParam)abstractParam);

                abstractParam = MzIdentMLTools.createPSICvParam(
                        OntologyConstants.CONTACT_EMAIL, contact.getContactInfo());
                additionalInformation.getCvParam().add((CvParam)abstractParam);
            }
        }
    }


    /**
     * Checks whether the given sourceFile contains a valid fileType and
     * creates an mzIdentML SpectraData if it is.
     *
     * @return
     */
    private static SpectraData spectraDataFromSourceFile(SourceFile sourceFile,
            StringBuilder sourceIdBase) {
        SpectraData specData = null;

        if (sourceFile.getFileType().equalsIgnoreCase("Mascot dat file")) {
            specData = new SpectraData();

            FileFormat fileFormat = new FileFormat();
            CvParam cvParam = MzIdentMLTools.createPSICvParam(
                    OntologyConstants.MASCOT_DAT_FORMAT,
                    null
                    );
            fileFormat.setCvParam(cvParam);
            specData.setFileFormat(fileFormat);

            SpectrumIDFormat idFormat = new SpectrumIDFormat();
            cvParam = MzIdentMLTools.createPSICvParam(
                    OntologyConstants.MASCOT_QUERY_NUMBER,
                    null
                    );
            idFormat.setCvParam(cvParam);
            specData.setSpectrumIDFormat(idFormat);

            if (sourceIdBase != null) {
                sourceIdBase.delete(0, sourceIdBase.length());
                sourceIdBase.append("query=");
            }
        }

        return specData;
    }


    /**
     * Parses the instrument section of the PRIDE XML file
     *
     * @param additionalInformation
     */
    private static void parseInstrumentInformations(Instrument instrumentSection,
            ParamList additionalInformation, PIACompiler compiler) {
        AbstractParam abstractParam;

        String instrumentName = instrumentSection.getInstrumentName();
        if ((instrumentName != null) && !instrumentName.isEmpty()) {
            Term instrumentTerm = compiler.getOBOMapper().getTermByName(instrumentName);
            if (instrumentTerm != null) {
                abstractParam = MzIdentMLTools.createCvParam(
                        instrumentTerm.getName(),
                        MzIdentMLTools.getCvPSIMS(),
                        instrumentTerm.getDescription(),
                        null);
            } else {
                abstractParam = MzIdentMLTools.createCvParam(
                        "MS:1000031",
                        MzIdentMLTools.getCvPSIMS(),
                        "instrument model",
                        instrumentName);
            }
            additionalInformation.getCvParam().add((CvParam)abstractParam);
        }
    }


    /**
     * Parses the dataProcessing section of the PRIDE XML file
     *
     * @param additionalInformation
     */
    private static void parseDataProcessingInformations(DataProcessing dataProcessingSection,
            SpectrumIdentificationProtocol spectrumIdProtocol, ParamList additionalInformation,
            PIACompiler compiler) {
        AbstractParam abstractParam;

        // add the software
        PrideSoftwareList prideSoftware =
                PrideSoftwareList.getByPrideName(dataProcessingSection.getSoftware().getName());
        AnalysisSoftware software = null;

        if (prideSoftware != null) {
            software = prideSoftware.getAnalysisSoftwareRepresentation();
            software.setVersion(dataProcessingSection.getSoftware().getVersion());
            software = compiler.putIntoSoftwareMap(software);
        } else {
            LOGGER.warn("Could not parse software! Please contact developer and ask to implement software '"
                    + dataProcessingSection.getSoftware().getName() + "'");
        }

        if (software != null) {
            spectrumIdProtocol.setAnalysisSoftware(software);
        }

        // go through all other processing methods
        for (uk.ac.ebi.pride.jaxb.model.CvParam param
                : dataProcessingSection.getProcessingMethod().getCvParam()) {
            if (param.getAccession().equals("PRIDE:0000161")) {
                // "Fragment mass tolerance setting"
                String splitString[] = param.getValue().split(" ");
                Tolerance tolerance = MzIdentMLTools.createSearchTolerance(
                        splitString[0], splitString[1]);

                if (tolerance != null) {
                    spectrumIdProtocol.setFragmentTolerance(tolerance);
                }
            } else if (param.getAccession().equals("PRIDE:0000078")) {
                // "Peptide mass tolerance setting"
                String splitString[] = param.getValue().split(" ");
                Tolerance tolerance = MzIdentMLTools.createSearchTolerance(
                        splitString[0], splitString[1]);

                if (tolerance != null) {
                    spectrumIdProtocol.setParentTolerance(tolerance);
                }
            } else if (param.getAccession().equals("PRIDE:0000162")) {
                // "Allowed missed cleavages"
                int missed = Integer.parseInt(param.getValue());
                for (Enzyme enzyme : spectrumIdProtocol.getEnzymes().getEnzyme()) {
                    enzyme.setMissedCleavages(missed);
                }
            } else {
                // needs no further processing (or unknown) -> add to additional
                abstractParam = convertCvParam(param);
                if (abstractParam != null) {
                    additionalInformation.getCvParam().add((CvParam)abstractParam);
                }
            }
        }

        for (uk.ac.ebi.pride.jaxb.model.UserParam param
                : dataProcessingSection.getProcessingMethod().getUserParam()) {
            abstractParam = convertUserParam(param);
            if (abstractParam != null) {
                additionalInformation.getUserParam().add((UserParam)abstractParam);
            }
        }
    }


    /**
     * Parses the additional section of the PRIDE XML file
     *
     * @param additionalInformation
     */
    private static void parseAdditionalInformations(uk.ac.ebi.pride.jaxb.model.Param additionalParams,
            ParamList additionalInformation) {

        String pxdAccession = null;
        //<cvParam cvLabel="MS" accession="MS:1001919" name="ProteomeXchange accession number" value="PXD000218"></cvParam>
        String projectName = null;
        //<cvParam cvLabel="PRIDE" accession="PRIDE:0000097" name="Project" value="System-level analysis of cancer and stomal cell specific proteomes reveals extensive reprogramming of phosphorylation networks by tumor microenvironment"></cvParam>

        for (uk.ac.ebi.pride.jaxb.model.CvParam param : additionalParams.getCvParam()) {
            if (param.getAccession().equals(OntologyConstants.PROTEOMEXCHANGE_ACCESSION_NUMBER.getPsiAccession()) ||
                    param.getAccession().equals(OntologyConstants.PROTEOMEXCHANGE_ACCESSION_NUMBER.getPrideAccession())) {
                pxdAccession = param.getValue();
            } else if (param.getAccession().equals(OntologyConstants.PRIDE_PROJECT_NAME.getPrideAccession())) {
                projectName = param.getValue();
            }
        }

        if (pxdAccession != null) {
            additionalInformation.getCvParam().add(MzIdentMLTools.createPSICvParam(
                    OntologyConstants.PROTEOMEXCHANGE_ACCESSION_NUMBER,
                    pxdAccession));
        }
        if (projectName != null) {
            additionalInformation.getCvParam().add(
                    MzIdentMLTools.createCvParam(
                            OntologyConstants.PRIDE_PROJECT_NAME.getPrideAccession(),
                            PRIDETools.getPrideCV(),
                            OntologyConstants.PRIDE_PROJECT_NAME.getPrideName(),
                            projectName));
        }
    }



    /**
     * Creates the {@link ModificationParams} for the
     * SpectrumIdentificationProtocol using the identified modifications.
     *
     * @param modifications
     * @param unimodParser
     * @return
     */
    private static ModificationParams createModificationParams(Set<Modification> modifications,
            UnimodParser unimodParser) {
        ModificationParams modParams = new ModificationParams();
        List<SearchModification> modList = modParams.getSearchModification();

        for (Modification mod : modifications) {
            SearchModification searchMod = new SearchModification();

            searchMod.setFixedMod(false);
            searchMod.setMassDelta(mod.getMass().floatValue());
            searchMod.getResidues().add(mod.getResidue().toString());

            ModT unimod = unimodParser.getModificationByNameAndMass(
                    mod.getDescription(),
                    mod.getMass(),
                    searchMod.getResidues());
            if (unimod != null) {
                CvParam cvParam = new CvParam();
                cvParam.setAccession("UNIMOD:" + unimod.getRecordId());
                cvParam.setCv(UnimodParser.getCv());
                cvParam.setName(unimod.getTitle());
                searchMod.getCvParam().add(cvParam);
            }

            modList.add(searchMod);
        }

        return modParams;
    }


    private static SearchDatabase createSearchDatabase(String name, String version) {
        SearchDatabase sDB = new SearchDatabase();
        sDB.setId("prideDB");
        sDB.setLocation(version);
        sDB.setName(name);
        sDB.setVersion(version);

        if (name.contains(".fasta") || version.contains(".fasta")) {
            // fileformat
            FileFormat fileFormat = new FileFormat();
            fileFormat.setCvParam(
                    MzIdentMLTools.createPSICvParam(OntologyConstants.FASTA_FORMAT,
                            null));
            sDB.setFileFormat(fileFormat);
        }

        // databaseName
        Param param = new Param();
        param.setParam(MzIdentMLTools.createUserParam(name, null, "string"));
        sDB.setDatabaseName(param);

        return sDB;
    }
}