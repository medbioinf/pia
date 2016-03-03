package de.mpc.pia.intermediate.compiler.parser;

import de.mpc.pia.intermediate.*;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.modeller.IdentificationKeySettings;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.tools.MzIdentMLTools;
import de.mpc.pia.tools.pride.PRIDETools;
import de.mpc.pia.tools.pride.PrideSoftwareList;
import de.mpc.pia.tools.unimod.UnimodParser;
import de.mpc.pia.tools.unimod.jaxb.ModT;

import org.apache.log4j.Logger;
import org.biojava.nbio.ontology.Term;

import uk.ac.ebi.jmzidml.model.mzidml.*;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.UserParam;
import uk.ac.ebi.pride.jaxb.model.*;
import uk.ac.ebi.pride.jaxb.xml.PrideXmlReader;

import java.io.File;
import java.util.*;

/**
 * This class read the PRIDE Xml files and map the structure into a
 * @author Yasset Perez-Riverol (ypriverol@gmail.com)
 * @author julianu
 * @date 08/02/2016
 */
public class PrideXMLParser {

    /** logger for this class */
    private static final Logger logger = Logger.getLogger(PrideXMLParser.class);

    private static final Set<String> chargeAccessions = new HashSet<String>(Arrays.asList(new String[]{
            "PSI:1000041",
            "MS:1000041"}));

    private static final Set<String> mzAccessions = new HashSet<String>(Arrays.asList(new String[]{
            "PSI:1000040",
            "MS:1000040",
            "PSI:1000744",
            "MS:1000744"}));

    private static final Set<String> rtAccessions = new HashSet<String>(Arrays.asList(new String[]{
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
     * @param name
     * @param fileName
     * @param compiler
     * @return
     */
    public static boolean getDataFromPrideXMLFile(String fileName,
            PIACompiler compiler) {
        // Open the input mzIdentML file for parsing
        File prideFile = new File(fileName);

        if (!prideFile.canRead()) {
            logger.error("could not read '" + fileName + "'.");
            return false;
        }

        PrideXmlReader prideParser = new PrideXmlReader(prideFile);

        String name = prideParser.getExpShortLabel();
        PIAInputFile file = compiler.insertNewFile(name, fileName,
                InputFileParserFactory.InputFileTypes.PRIDEXML_INPUT.getFileTypeName());


        // add the spectraData, it is the actual PRIDE XML file
        SpectraData spectraData = null;
        spectraData = new SpectraData();
        spectraData.setId("prideFile");
        spectraData.setLocation(prideFile.getAbsolutePath());

        /* TODO: add the fileformat!
        FileFormat fileFormat = new FileFormat();
        AbstractParam abstractParam = new CvParam();
        ((CvParam)abstractParam).setAccession("MS:1001062");
        ((CvParam)abstractParam).setCv(MzIdentMLTools.getCvPSIMS());
        abstractParam.setName("Mascot MGF file");
        fileFormat.setCvParam((CvParam)abstractParam);
        spectraData.setFileFormat(fileFormat);
        */

        SpectrumIDFormat idFormat = new SpectrumIDFormat();
        AbstractParam abstractParam = new CvParam();
        ((CvParam)abstractParam).setAccession("MS:1000774");
        ((CvParam)abstractParam).setCv(MzIdentMLTools.getCvPSIMS());
        abstractParam.setName("multiple peak list nativeID format");
        idFormat.setCvParam((CvParam)abstractParam);
        spectraData.setSpectrumIDFormat(idFormat);

        spectraData = compiler.putIntoSpectraDataMap(spectraData);


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

        parseAdminInformations(prideParser.getAdmin(), additionalInformation);
        parseInstrumentInformations(prideParser.getInstrument(), additionalInformation, compiler);
        parseDataProcessingInformations(prideParser.getDataProcessing(),
                spectrumIDProtocol, additionalInformation, compiler);

        spectrumIDProtocol.setAdditionalSearchParams(additionalInformation);
        file.addSpectrumIdentificationProtocol(spectrumIDProtocol);

        // add the spectrum identification
        SpectrumIdentification spectrumID = new SpectrumIdentification();
        spectrumID.setId("prideIdentification");
        spectrumID.setSpectrumIdentificationList(null);
        spectrumID.setSpectrumIdentificationProtocol(spectrumIDProtocol);

        if (spectraData != null) {
                InputSpectra inputSpectra = new InputSpectra();
                inputSpectra.setSpectraData(spectraData);
                spectrumID.getInputSpectra().add(inputSpectra);
        }

        file.addSpectrumIdentification(spectrumID);

        int accNr = 0;
        int pepNr = 0;
        int specNr = 0;

        boolean decoysFound = false;

        // to check, whether the PSM is already there, we need the psmSetSettings map
        Map<String, Boolean> psmSetSettings = new HashMap<String, Boolean>();
        psmSetSettings.put(IdentificationKeySettings.SOURCE_ID.name(), true);
        psmSetSettings.put(IdentificationKeySettings.SEQUENCE.name(), true);
        psmSetSettings.put(IdentificationKeySettings.MODIFICATIONS.name(), true);
        psmSetSettings.put(IdentificationKeySettings.CHARGE.name(), true);

        // map to store the already created PSMs
        Map<String, PeptideSpectrumMatch> keysToPSMs = new HashMap<String, PeptideSpectrumMatch>();

        // mapping from the enzyme accessions to regular expressions of the enzyme
        Map<String, String> enzymesToRegexes = new HashMap<String, String>();

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
            /* TODO: add database information (only in some PRIDE XMLs)
            identification.getDatabase();
            identification.getDatabaseVersion();

            SearchDatabase sDB =
                    searchDatabaseMap.get(pep.getFastaFilePath());
            if (sDB != null) {
                acc.addSearchDatabaseRef(sDB.getId());
            }
            */

            for (PeptideItem peptideItem : identification.getPeptideItem()) {
                String sequence = peptideItem.getSequence();

                Spectrum spectrum = peptideItem.getSpectrum();
                SpectrumDesc spectrumDesc = spectrum.getSpectrumDesc();

                Integer charge = null;
                String chargeStr = getValueFromSpectrumPrecursor(spectrumDesc, chargeAccessions);
                if (chargeStr != null) {
                    charge = Integer.parseInt(chargeStr);
                }
                String sourceID = "spectrum=" + spectrum.getId();

                Map<Integer, Modification> modifications =
                        transformModifications(sequence, peptideItem.getModificationItem(), compiler.getUnimodParser());

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

                PeptideSpectrumMatch psm;
                Peptide peptide;

                psm = keysToPSMs.get(psmKey);
                if (psm == null) {
                    String mzStr = getValueFromSpectrumPrecursor(spectrumDesc, mzAccessions);
                    Double precursorMZ = null;
                    if (mzStr != null) {
                        precursorMZ = Double.parseDouble(mzStr);
                    } else {
                        precursorMZ = Double.NaN;
                    }

                    // TODO: implement some calculation for the deltaMass
                    double deltaMass = Double.NaN;

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

                    psm = compiler.insertNewSpectrum(
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
                        logger.error("The peptide " + sequence + " was not found in the compiler!");
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

                // now insert the peptide and the accession into the accession peptide map
                Set<Peptide> accsPeptides =
                        compiler.getFromAccPepMap(acc.getAccession());

                if (accsPeptides == null) {
                    accsPeptides = new HashSet<Peptide>();
                    compiler.putIntoAccPepMap(acc.getAccession(), accsPeptides);
                }

                accsPeptides.add(peptide);

                // and also insert them into the peptide accession map
                Set<Accession> pepsAccessions =
                        compiler.getFromPepAccMap(peptide.getSequence());

                if (pepsAccessions == null) {
                    pepsAccessions = new HashSet<Accession>();
                    compiler.putIntoPepAccMap(peptide.getSequence(),
                            pepsAccessions);
                }

                pepsAccessions.add(acc);
            }
        }


        // go through all PSMs and delete decoy information (if none were found)
        if (!decoysFound) {
            logger.debug("resetting all decoy information, because no decoys were found in the file");
            for (PeptideSpectrumMatch psm : keysToPSMs.values()) {
                psm.setIsDecoy(null);
            }
        }

        logger.info("inserted new: \n\t" +
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
        List<ScoreModel> scoreModels = new ArrayList<ScoreModel>();

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
            Map<Integer, Modification> modificationMap = new HashMap<Integer, Modification>();

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
    private static void parseAdminInformations(Admin adminSection, ParamList additionalInformation) {
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

        // contact information
        if ((adminSection.getContact() != null) && !adminSection.getContact().isEmpty()) {
            for (Contact contact : adminSection.getContact()) {
                abstractParam = MzIdentMLTools.createCvParam(
                        "MS:1000586",
                        MzIdentMLTools.getCvPSIMS(),
                        "contact name",
                        contact.getName());
                additionalInformation.getCvParam().add((CvParam)abstractParam);

                abstractParam = MzIdentMLTools.createCvParam(
                        "MS:1000590",
                        MzIdentMLTools.getCvPSIMS(),
                        "contact affiliation",
                        contact.getInstitution());
                additionalInformation.getCvParam().add((CvParam)abstractParam);

                abstractParam = MzIdentMLTools.createCvParam(
                        "MS:1000589",
                        MzIdentMLTools.getCvPSIMS(),
                        "contact email",
                        contact.getContactInfo());
                additionalInformation.getCvParam().add((CvParam)abstractParam);
            }
        }
    }


    /**
     * Parses the instrument section of the PRIDE XML file
     *
     * @param adminSection
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

        /* TODO: add these information, if necessary
        instrumentSection.getSource();
        instrumentSection.getAnalyzerList();
        instrumentSection.getDetector();
        instrumentSection.getAdditional();
        */
    }


    /**
     * Parses the dataProcessing section of the PRIDE XML file
     *
     * @param adminSection
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
            software = compiler.putIntoSoftwareMap(software);
            software.setVersion(dataProcessingSection.getSoftware().getVersion());
        } else {
            logger.warn("Could not parse software! Please contact developer and ask to implement software '"
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
}