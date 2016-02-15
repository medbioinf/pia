package de.mpc.pia.intermediate.compiler.parser;

import de.mpc.pia.intermediate.*;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.tools.PRIDETools;
import de.mpc.pia.tools.obo.OBOMapper;
import org.apache.log4j.Logger;
import org.biojava.nbio.ontology.Term;
import org.biojava.nbio.ontology.Triple;
import uk.ac.ebi.jmzidml.model.mzidml.*;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.pride.jaxb.model.*;
import uk.ac.ebi.pride.jmztab.model.*;
import uk.ac.ebi.pride.jmztab.model.Contact;
import uk.ac.ebi.pride.jmztab.model.Modification;
import uk.ac.ebi.pride.jmztab.model.Param;
import uk.ac.ebi.pride.jmztab.model.Software;
import uk.ac.ebi.pride.jmztab.utils.MZTabFileParser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * This class read the MzTab Files and Map the to the PIA Intermediate File format
 *
 * @author Yasset Perez-Riverol (ypriverol@gmail.com)
 * @date 08/02/2016
 */
public class MzTabParser {

    /** logger for this class */
    private static final Logger logger = Logger.getLogger(MzTabParser.class);

    /**
     * We don't ever want to instantiate this class
     */
    private MzTabParser() {
        throw new AssertionError();
    }

    /**
     * Parse the PRIDE Xml into a PIA structure, The concept of PSM is not provided in PRIDE Files
     * this is the main reason why we will considered peptides as PSMs here.
     * @param name
     * @param fileName
     * @param compiler
     * @return
     */
    public static boolean getDataFromMzTabLFile(String name, String fileName, PIACompiler compiler){

        // Open the mztab files.

        File mzTabFile = new File(fileName);

        if (!mzTabFile.canRead()) {
            // TODO: better error / exception
            logger.error("could not read '" + fileName + "'.");
            return false;
        }

        Cv psiMS = new Cv();
        psiMS.setId("PSI-MS");
        psiMS.setFullName("PSI-MS");
        psiMS.setUri("http://psidev.cvs.sourceforge.net/viewvc/*checkout*/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo");

        try{

            MZTabFileParser tabParser = new MZTabFileParser(mzTabFile, new FileOutputStream(mzTabFile.getAbsolutePath() + "errors.out"));

            Metadata metadata = tabParser.getMZTabFile().getMetadata();

            SortedMap<Integer, VariableMod> variableMods = metadata.getVariableModMap();

            SortedMap<Integer, FixedMod> fixMods = metadata.getFixedModMap();

            ModificationParams allModMap = retrieveAllMod(metadata);

            Map<String, String> allModStringMap = retrieveStringMod(metadata);

            Map<String, DBSequence> dbSequences = convertDBSequences(tabParser.getMZTabFile().getProteins());

            /**
             * Convert MsRun data information to SpectraData Objects some things needs to be refined to do not loost the original information
             * such as the Fragmentation information for every mzTab file.
             */

            SortedMap<Integer, MsRun> msRuns = metadata.getMsRunMap();

            Map<Integer, SpectraData> spectraDataMap = new HashMap<Integer, SpectraData>();

            //Create all files, for every single msRun

            Map<MsRun, PIAInputFile> inputFileMap = new HashMap<MsRun, PIAInputFile>();
            Map<MsRun, SpectrumIdentification> spectrumIDMap = new HashMap<MsRun, SpectrumIdentification>();


            for(Integer msRunKey: msRuns.keySet()){

                MsRun msRun = msRuns.get(msRunKey);
                PIAInputFile file = compiler.insertNewFile(msRunKey.toString(), msRun.toString(), InputFileParserFactory.InputFileTypes.MZTAB_INPUT.getFileSuffix());
                inputFileMap.put(msRun, file);
                SpectraData newSpectraData = new SpectraData();

                SpectrumIDFormat formatID = new SpectrumIDFormat();
                formatID.setCvParam(PRIDETools.convertCvParam(msRun.getIdFormat()));
                newSpectraData.setSpectrumIDFormat(formatID);

                FileFormat newFileFormat = new FileFormat();
                newFileFormat.setCvParam(PRIDETools.convertCvParam(msRun.getFormat()));
                newSpectraData.setFileFormat(newFileFormat);

                newSpectraData.setLocation(msRun.getLocation().toString());

                spectraDataMap.put(msRunKey, newSpectraData);

                // create the analysis software and add it to the compiler
                AnalysisSoftware piaConversion = new AnalysisSoftware();

                piaConversion.setId("pia");
                piaConversion.setName("PIA");
                piaConversion.setUri("https://github.com/mpc-bioinformatics/pia");

                AbstractParam abstractParam;
                uk.ac.ebi.jmzidml.model.mzidml.Param param = new uk.ac.ebi.jmzidml.model.mzidml.Param();
                abstractParam = new CvParam();
                ((CvParam)abstractParam).setAccession(" MS:1002387");
                ((CvParam)abstractParam).setCv(psiMS);
                abstractParam.setName("PIA");
                param.setParam(abstractParam);
                piaConversion.setSoftwareName(param);

                piaConversion = compiler.putIntoSoftwareMap(piaConversion);

                SpectrumIdentificationProtocol spectrumIDProtocol =
                        new SpectrumIdentificationProtocol();

                spectrumIDProtocol.setId("piaAnalysis");
                spectrumIDProtocol.setAnalysisSoftware(piaConversion);

                // TODO: only supporting "ms-ms search" for now
                param = new uk.ac.ebi.jmzidml.model.mzidml.Param();
                abstractParam = new CvParam();
                ((CvParam)abstractParam).setAccession("MS:1001083");
                ((CvParam)abstractParam).setCv(psiMS);
                abstractParam.setName("ms-ms search");
                param.setParam(abstractParam);
                spectrumIDProtocol.setSearchType(param);


                spectrumIDProtocol.setModificationParams(allModMap);

                SpectrumIdentification spectrumID = new SpectrumIdentification();
                spectrumID.setId("piaAnalysis");
                spectrumID.setSpectrumIdentificationList(null);
                spectrumID.setSpectrumIdentificationProtocol(spectrumIDProtocol);

                spectrumIDMap.put(msRun, spectrumID);


                //Todo: Some information will be missing if we don't use a general SpectraData object like the fragmentation method.

            }

            // maps from the file's ID to the compiler's analysisSoftware
            Map<Integer, AnalysisSoftware> analysisSoftwareRefs = new HashMap<Integer, AnalysisSoftware>();

            for(Integer softwareKey: metadata.getSoftwareMap().keySet()){

                Software software = metadata.getSoftwareMap().get(softwareKey);
                AnalysisSoftware newSoftware = new AnalysisSoftware();

                newSoftware.setSoftwareName(PRIDETools.convertParam(software.getParam()));

                newSoftware.setId(softwareKey.toString());

                analysisSoftwareRefs.put(softwareKey, newSoftware);

            }

            SortedMap<Integer, PSMSearchEngineScore> searchEgineScores = metadata.getPsmSearchEngineScoreMap();
            // maps from the file's ID to the compiler's ID of the SpectrumIdentificationProtocol

            SortedMap<Integer, Assay> assays = metadata.getAssayMap();

            SortedMap<Integer, Contact> contactMap = metadata.getContactMap();

            Map<String, String> colUnitMap = metadata.getColUnitMap();

            String description = metadata.getDescription();

            List<Param> customList = metadata.getCustomList();

            Collection<PSM> psms = tabParser.getMZTabFile().getPSMs();

            // maps from the ID to the DBSequence, this is important and should be filled using the Protein Section

            // maps from the ID to the Protein
            Map<String, uk.ac.ebi.jmzidml.model.mzidml.Peptide> peptides = new HashMap<String, uk.ac.ebi.jmzidml.model.mzidml.Peptide>();

            // maps from the SpectrumIdentificationList IDs to the SpectrumIdentification IDs
            Map<String, String> specIdListIDtoSpecIdID = new HashMap<String, String>();

            // maps from the enzyme ID (from the OBO) to the regex, only used, if no siteRegex param is given
            Map<String, String> enzymesToRegexes = new HashMap<String, String>();

            int accNr = 0;
            int pepNr = 0;
            int specNr = 0;


            // now parse the lines, each line is one PSM
            for (PSM mzTabPSM: psms) {

                PeptideSpectrumMatch psm;
                Peptide peptide;

                Integer charge = mzTabPSM.getCharge();
                charge = (charge != null)? charge : 0;


                Double precursorMZ = mzTabPSM.getExpMassToCharge();
                precursorMZ = (precursorMZ != null)? precursorMZ : Double.NaN;

                Double theoreticalMass = mzTabPSM.getCalcMassToCharge();
                theoreticalMass = (theoreticalMass != null)? theoreticalMass:0;

                // Todo: What is the aim of this variable, do we can to use it in the future.
                double deltaMass = Double.NaN;

                String sequence = mzTabPSM.getSequence();

                Map<Integer, de.mpc.pia.intermediate.Modification> modifications = new HashMap<Integer, de.mpc.pia.intermediate.Modification>();
                if (mzTabPSM.getModifications() != null && !mzTabPSM.getModifications().isEmpty()) {
                    sequence = transformModifications(sequence, mzTabPSM. getModifications(), modifications, allModStringMap, compiler);
                }

                List<ScoreModel> scores = new ArrayList<ScoreModel>();

                //Parse the current Scores to PIA Scores.
                for(Integer searchEngineScoreKey: metadata.getPsmSearchEngineScoreMap().keySet()){
                    PSMSearchEngineScore mzTabScore = metadata.getPsmSearchEngineScoreMap().get(searchEngineScoreKey);
                    ScoreModel piaScore;
                    if(mzTabPSM.getSearchEngineScore(searchEngineScoreKey) != null){
                        ScoreModelEnum scoreType = null;
                        Param param = (mzTabScore.getParam() != null)? mzTabScore.getParam(): null;
                        if(param != null && param.getAccession() != null)
                            scoreType = ScoreModelEnum.getModelByDescription(param.getAccession());
                        else if(param != null && param.getName() != null)
                            scoreType = ScoreModelEnum.getModelByDescription(param.getName());
                        if(param != null && scoreType == null){
                            // get the scores and add them to the PSM
                            String cvAccession = param.getAccession();
                            Term oboTerm = compiler.getOBOMapper().getTerm(cvAccession);
                            if (oboTerm != null) {
                                // the score is in the OBO file, get the relations etc.
                                Set<Triple> tripleSet = compiler.getOBOMapper().getTriples(oboTerm, null, null);
                                for (Triple triple : tripleSet) {
                                    if (triple.getPredicate().getName().equals(OBOMapper.obo_is_a) &&
                                            triple.getObject().getName().equals(OBOMapper.obo_psmScoreID)) {
                                        piaScore = new ScoreModel(mzTabPSM.getSearchEngineScore(searchEngineScoreKey), cvAccession, param.getName());
                                        scores.add(piaScore);
                                    }
                                }
                            }
                        }
                        if(scoreType != null){
                            piaScore = new ScoreModel(mzTabPSM.getSearchEngineScore(searchEngineScoreKey), scoreType);
                            scores.add(piaScore);
                        }
                    }
                }

                int countRTs = 0;

                for(SpectraRef spectraRef: mzTabPSM.getSpectraRef()){

                    PIAInputFile piaFile = inputFileMap.get(spectraRef.getMsRun());

                    String sourceID = spectraRef.getReference();

                    Double rt = null;
                    if(mzTabPSM.getRetentionTime() != null && mzTabPSM.getRetentionTime().size() > countRTs)
                        rt = mzTabPSM.getRetentionTime().get(countRTs);
                    countRTs++;

                    psm = compiler.insertNewSpectrum(
                            charge,
                            precursorMZ,
                            theoreticalMass,
                            deltaMass,
                            rt,
                            sequence,
                            -1,
                            sourceID,
                            null,
                            piaFile,
                            spectrumIDMap.get(spectraRef.getMsRun()));

                    if(mzTabPSM.getOptionColumnValue("cv_MS:1002217_decoy_peptide") != null)
                        if(mzTabPSM.getOptionColumnValue("cv_MS:1002217_decoy_peptide").equalsIgnoreCase("1"))
                            psm.setIsDecoy(true);
                        else
                            psm.setIsDecoy(false);

                    // get the peptide or create it
                    peptide = compiler.getPeptide(sequence);
                    if (peptide == null) {
                        peptide = compiler.insertNewPeptide(sequence);
                        pepNr++;
                    }

                    // add the spectrum to the peptide
                    peptide.addSpectrum(psm);

                    // add the modifications
                    for (Map.Entry<Integer, de.mpc.pia.intermediate.Modification> mod
                            : modifications.entrySet()) {
                        psm.addModification(mod.getKey(), mod.getValue());
                    }

                    for(ScoreModel score: scores)
                        psm.addScore(score);

                    String accession = mzTabPSM.getAccession();
                    Accession acc = compiler.getAccession(accession);
                    if (acc == null) {
                        String sequenceDB = (dbSequences.get(accession) != null)?dbSequences.get(accession).getSeq():null;
                        // optional the information about the sequence
                        acc = compiler.insertNewAccession(accession, sequenceDB);
                        accNr++;
                    }
                    acc.addFile(piaFile.getID());

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

        }catch (IOException e){
            logger.error("Problem to read the mzTab Files'" + fileName + "'.");
        }
       return false;
    }

    /**
     * Some translation to String modifications
     * @param metadata
     * @return
     */
    private static Map<String, String> retrieveStringMod(Metadata metadata) {
       Map<String, String> mods = new HashMap<String, String>();

        if(metadata.getFixedModMap() != null)
            for(FixedMod fixed: metadata.getFixedModMap().values()){
                mods.put(fixed.getParam().getAccession(), fixed.getParam().getName());
            }

        if(metadata.getVariableModMap() != null){
            for(VariableMod variableMod: metadata.getVariableModMap().values()){
                mods.put(variableMod.getParam().getAccession(), variableMod.getParam().getName());
            }
        }
        return mods;
    }

    /**
     * This funtion return the list of proteins in the database.
     * @param proteins
     * @return
     */
    private static Map<String, DBSequence> convertDBSequences(Collection<Protein> proteins) {
        Map<String, DBSequence> dbSequenceMap = new HashMap<String, DBSequence>();
        if(proteins != null && proteins.size() >0){
            for(Protein protein: proteins){
                DBSequence dbSequence = new DBSequence();
                dbSequence.setAccession(protein.getAccession());
                SearchDatabase searchDatabase = new SearchDatabase();
                searchDatabase.setId("DB=" + protein.getDatabase());
                searchDatabase.setVersion(protein.getDatabaseVersion());
                searchDatabase.setName(protein.getDatabase());
                dbSequence.setSearchDatabase(searchDatabase);
                dbSequence.setSeq(protein.getOptionColumnValue(PRIDETools.OPTIONAL_SEQUENCE_COLUMN));
                dbSequenceMap.put(dbSequence.getAccession(), dbSequence);
            }
        }
        return dbSequenceMap;
    }

    /**
     * This funtion compile for mzTab all the modifications present in the file, including variables and
     * fixed modifications.
     *
     * @param metadata
     * @return
     */
    private static ModificationParams retrieveAllMod(Metadata metadata) {
        ModificationParams modifications = new ModificationParams();

        if(metadata.getFixedModMap() != null)
            for(FixedMod fixed: metadata.getFixedModMap().values()){
                SearchModification searchModification = new SearchModification();
                searchModification.setFixedMod(true);
                searchModification.getCvParam().add(PRIDETools.convertCvParam(fixed.getParam()));
                float valueDeltaMass = (fixed.getParam().getValue()!= null)? new Float(fixed.getParam().getValue()): new Float(-1.0) ;
                searchModification.setMassDelta(valueDeltaMass);
                modifications.getSearchModification().add(searchModification);
                //Todo here we don't need to specify the specificity but during the export we should be able to annotate that.
            }

        if(metadata.getVariableModMap() != null){
            for(VariableMod variableMod: metadata.getVariableModMap().values()){
                SearchModification searchModification = new SearchModification();
                searchModification.setFixedMod(false);
                searchModification.getCvParam().add(PRIDETools.convertCvParam(variableMod.getParam()));
                float valueDeltaMass = (variableMod.getParam().getValue()!= null)? new Float(variableMod.getParam().getValue()): new Float(-1.0) ;
                searchModification.setMassDelta(valueDeltaMass);
                modifications.getSearchModification().add(searchModification);
            }
        }

        return modifications;


    }

    /**
     * This file will take a list of mzTab modifications and convert them to intermediate modifications
     * the methods needs as input the list of mztab modifications and the compiler. The metadata is necesary to
     * get the information of the modifications like names, positions, etc.
     *
     * @param mzTabMods
     * @param modifications
     * @param compiler
     * @return
     */
    private static String transformModifications(String sequence,
                                                 SplitList<uk.ac.ebi.pride.jmztab.model.Modification> mzTabMods,
                                                 Map<Integer, de.mpc.pia.intermediate.Modification> modifications,
                                                 Map<String, String> allModsTab,
                                                 PIACompiler compiler) {
        if (modifications == null){
            logger.error("Modifications map not initialized!");
            return null;
        }

        for (uk.ac.ebi.pride.jmztab.model.Modification oldMod: mzTabMods) {
            for(Integer pos: oldMod.getPositionMap().keySet()){
                String oldAccession = (oldMod.getType() == Modification.Type.MOD && !oldMod.getAccession().startsWith("MOD"))? "MOD:" + oldMod.getAccession(): oldMod.getAccession();
                Character charMod = (pos == 0 || pos > sequence.length())?'.':sequence.charAt(pos-1);
                de.mpc.pia.intermediate.Modification mod = new de.mpc.pia.intermediate.Modification(charMod,
                        compiler.getModReader().getPTMbyAccession(oldAccession).getMonoDeltaMass(),
                        allModsTab.get(oldAccession),
                        oldMod.getAccession());
                modifications.put(pos, mod);

            }
        }
        return sequence;
    }

    /**
     * Converting the modification Items to intermediate modifications
     * @param sequence
     * @param modificationItem
     * @return
     */
    private static Map<Integer, de.mpc.pia.intermediate.Modification> transformModifications(String sequence, List<ModificationItem> modificationItem) {
        if(modificationItem != null && !modificationItem.isEmpty()){
            Map<Integer, de.mpc.pia.intermediate.Modification> modificationMap = new HashMap<Integer, de.mpc.pia.intermediate.Modification>();
            for(ModificationItem mod: modificationItem){
                Double mass = null;
                if(mod.getModMonoDelta() != null && mod.getModMonoDelta().get(0) != null)
                    mass = new Double(mod.getModMonoDelta().get(0));
                else if(mod.getModAvgDelta() != null && mod.getModAvgDelta().get(0) != null)
                    mass = new Double(mod.getModAvgDelta().get(0));
                de.mpc.pia.intermediate.Modification modification =
                        new de.mpc.pia.intermediate.Modification(sequence.charAt(mod.getModLocation().intValue() -1),mass,mod.getModDatabase(),mod.getModAccession());
                modificationMap.put(mod.getModLocation().intValue(), modification);
            }
            return modificationMap;
        }
        return Collections.EMPTY_MAP;
    }


    /**
     * Transform protocol from pride xml to core data model. For now we will add the Steps in the
     * Data processing into CVparam in protein indentification.
     *
     * @param rawProt protocol from pride xml.
     * @return Protocol protocol in core data model format.
     */
    public static SpectrumIdentificationProtocol addProtocol(SpectrumIdentificationProtocol protocol, uk.ac.ebi.pride.jaxb.model.Protocol rawProt) {
        return protocol;
        //Todo we need to parse here the rawProtocol and been able to add them to the General Protocol.
    }






}
