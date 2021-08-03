package de.mpc.pia.intermediate.compiler.parser;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.tools.MzIdentMLTools;
import de.mpc.pia.tools.OntologyConstants;
import de.mpc.pia.tools.obo.AbstractOBOMapper;
import de.mpc.pia.tools.obo.OBOMapper;
import de.mpc.pia.tools.pride.PRIDETools;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.biojava.nbio.ontology.Term;
import org.biojava.nbio.ontology.Triple;
import uk.ac.ebi.jmzidml.model.mzidml.*;
import uk.ac.ebi.jmzidml.xml.io.MzIdentMLUnmarshaller;
import uk.ac.ebi.pride.utilities.pridemod.ModReader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * This class parses the data from a mzIdentML file for a given {@link PIACompiler}.
 *
 * @author julianu
 *
 */
class MzIdentMLFileParser {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(MzIdentMLFileParser.class);


    /** the used PIA compiler */
    private PIACompiler compiler;

    /** the newly added PIAInputFile in the compiler */
    private PIAInputFile file;

    /** the used unmarshaller */
    private MzIdentMLUnmarshaller unmarshaller;

    /** maps from the ID to the peptides */
    private Map<String, uk.ac.ebi.jmzidml.model.mzidml.Peptide> peptides;

    /** maps from the ID to the PeptideEvidence */
    private Map<String, PeptideEvidence> peptideEvidences;

    /** maps from the ID to the DBSequence */
    private Map<String, DBSequence> dbSequences;

    /** maps from the SpectrumIdentificationList IDs to the SpectrumIdentification IDs */
    private Map<String, String> specIdListIDtoSpecIdID;


    /** number of added accessions */
    private int accNr;
    /** number of added peptides */
    private int pepNr;
    /** number of added spectra */
    private int specNr;

    /** unit ontology accession for minutes */
    private static final String UNIT_ACCESSION_MINUTES = "UO:0000031";


    /** the cvParams which are specially parsed and don't need to be passed for the spectrumIdentificationResults */
    private static List<String> parsedSpecIdResultCVParams = Arrays.asList(OntologyConstants.SCAN_NUMBERS.getPsiAccession(), OntologyConstants.SCAN_START_TIME.getPsiAccession());

    // Map modifications when is needed.
    private static ModReader modReader;

    /**
     * We don't ever want to instantiate this class outside
     */
    private MzIdentMLFileParser(PIACompiler compiler) {
        this.compiler = compiler;
    }


    /**
     * Parses the data from an mzIdentML file given by its name into the given
     * {@link PIACompiler}.
     *
     * @param fileName name of the mzIdentML file
     */
    public static boolean getDataFromMzIdentMLFile(String name, String fileName, PIACompiler compiler) {
        MzIdentMLFileParser parser = new MzIdentMLFileParser(compiler);
        return parser.parseFile(name, fileName);
    }



    /**
     * Parses the mzIdentML file into the compiler.
     *
     * @param name
     * @param fileName
     * @return
     */
    private boolean parseFile(String name, String fileName) {

        if (!createUnmarshaller(name, fileName)) {
            return false;
        }
        // TODO: create one PIAInputFile per SIL (and look out for consensus lists!)

        // maps from the file's ID to the compiler's SpectraData
        Map<String, SpectraData> spectraDataRefs = new HashMap<>();

        // maps from the file's ID to the compiler's searchDB
        Map<String, SearchDatabase> searchDBRefs = new HashMap<>();

        // maps from the file's ID to the compiler's analysisSoftware
        Map<String, AnalysisSoftware> analysisSoftwareRefs = new HashMap<>();

        Set<String> neededSpectrumIdentificationProtocols = new HashSet<>();
        Set<String> neededSpectraData = new HashSet<>();
        Set<String> neededSearchDatabases= new HashSet<>();
        Set<String> neededAnalysisSoftwares= new HashSet<>();

        accNr = 0;
        pepNr = 0;
        specNr = 0;

        // maps from the ID to the SpectrumIdentificationList
        Map<String, SpectrumIdentificationList> specIdLists = getSpectrumIdentificationLists();
        LOGGER.error("File has " + specIdLists.size() + " specIdLists");

        specIdListIDtoSpecIdID = new HashMap<>();

        // get the AnalysisCollection:SpectrumIdentification for the SpectrumIdentificationLists
        AnalysisCollection analysisCollection = unmarshaller.unmarshal(AnalysisCollection.class);
        for (SpectrumIdentification si : analysisCollection.getSpectrumIdentification()) {
            if (specIdLists.keySet().contains(si.getSpectrumIdentificationListRef())) {
                // if the SpectrumIdentification's SpectrumIdentificationList is in the file, we need the SpectrumIdentification
                String specIdListID = si.getSpectrumIdentificationListRef();
                String id = file.addSpectrumIdentification(si);

                specIdListIDtoSpecIdID.put(specIdListID, id);

                neededSpectrumIdentificationProtocols.add(si.getSpectrumIdentificationProtocolRef());
                neededSpectraData.addAll(si.getInputSpectra().stream().map(InputSpectra::getSpectraDataRef).collect(Collectors.toList()));
                neededSearchDatabases.addAll(si.getSearchDatabaseRef().stream().map(SearchDatabaseRef::getSearchDatabaseRef).collect(Collectors.toList()));
            } else {
                LOGGER.warn("file contains SpectrumIdentification ("
                        + si.getId() + ") without SpectrumIdentificationList!");
            }
        }

        // parse through the analysisProtocolColection
        parseAnalysisProtocolCollection(neededSpectrumIdentificationProtocols, neededAnalysisSoftwares);

        // get the necessary inputs:SpectraData
        Inputs inputs = unmarshaller.unmarshal(Inputs.class);
        inputs.getSpectraData().stream()
                .filter(spectraData -> neededSpectraData.contains(spectraData.getId()))
                .forEach(spectraData -> {
                    String ref = spectraData.getId();
                    SpectraData sd = compiler.putIntoSpectraDataMap(spectraData);
                    spectraDataRefs.put(ref, sd);
                });

        LOGGER.debug("Number of spectraData in inputs: " + inputs.getSpectraData().size());

        // get the necessary inputs:SearchDBs
        inputs.getSearchDatabase().stream()
                .filter(searchDB -> neededSearchDatabases.contains(searchDB.getId()))
                .forEach(searchDB -> {
                    String ref = searchDB.getId();
                    SearchDatabase sDB = compiler.putIntoSearchDatabasesMap(searchDB);
                    searchDBRefs.put(ref, sDB);
                });

        // get the necessary AnalysisSoftwares
        AnalysisSoftwareList analysisSoftwareList = unmarshaller.unmarshal(AnalysisSoftwareList.class);
        analysisSoftwareList.getAnalysisSoftware().stream()
                .filter(software -> neededAnalysisSoftwares.contains(software.getId()))
                .forEach(software -> {
                    String ref = software.getId();
                    AnalysisSoftware as = compiler.putIntoSoftwareMap(software);
                    analysisSoftwareRefs.put(ref, as);
                });

        // update the PIAFile's references for SpectraData, SearchDBs and AnalysisSoftwares
        file.updateReferences(spectraDataRefs, searchDBRefs, analysisSoftwareRefs);

        // get/hash the SequenceCollection:PeptideEvidences
        SequenceCollection sc = unmarshaller.unmarshal(SequenceCollection.class);
        peptideEvidences = new HashMap<>();
        for (PeptideEvidence pepEvidence : sc.getPeptideEvidence()) {
            peptideEvidences.put(pepEvidence.getId(), pepEvidence);
        }

        // get/hash the SequenceCollection:DBSequences
        dbSequences = new HashMap<>();
        for (DBSequence dbSeq : sc.getDBSequence()) {
            dbSequences.put(dbSeq.getId(), dbSeq);
        }

        // get/hash the SequenceCollection:Peptides
        peptides = new HashMap<>();
        for (uk.ac.ebi.jmzidml.model.mzidml.Peptide peptide: sc.getPeptide()) {
            peptides.put(peptide.getId(), peptide);
        }


        boolean ok = true;

        // go through the SpectrumIdentificationList:SpectrumIdentificationResult:SpectrumIdentificationItem and build the PeptideSpectrumMatches, Accessions and Peptides
        for (SpectrumIdentificationList specIDList : specIdLists.values()) {
            ok = addSpectrumIdentificationList(specIDList);

            if (!ok) {
                break;
            }
        }

        LOGGER.info("inserted new: \n\t" +
                pepNr + " peptides\n\t" +
                specNr + " peptide spectrum matches\n\t" +
                accNr + " accessions");
        return ok;
    }


    /**
     * Checks file readability and adds it to the inputFiles. Then the unmarshaller is created.
     *
     * @param name
     * @param fileName
     * @return
     */
    private boolean createUnmarshaller(String name, String fileName) {
        // Open the input mzIdentML file for parsing
        File mzidFile = new File(fileName);

        if (!mzidFile.canRead()) {
            LOGGER.error("could not read '" + fileName + "'.");
            return false;
        }

        file = compiler.insertNewFile(name, fileName,
                InputFileParserFactory.InputFileTypes.MZIDENTML_INPUT.getFileSuffix());

        unmarshaller = new MzIdentMLUnmarshaller(mzidFile);
        LOGGER.debug("Version of mzIdentML file: " + unmarshaller.getMzIdentMLVersion());
        return true;
    }


    /**
     * Parses the spectrumIdentificationLists
     * @return
     */
    private Map<String, SpectrumIdentificationList> getSpectrumIdentificationLists() {
        Map<String, SpectrumIdentificationList> spectrumIdentificationLists = new HashMap<>();

        AnalysisData analysisData = unmarshaller.unmarshal(AnalysisData.class);
        analysisData.getSpectrumIdentificationList()
                .forEach(specIdList -> spectrumIdentificationLists.put(specIdList.getId(), specIdList));

        return spectrumIdentificationLists;
    }


    /**
     * Parses the analysisProtocolCollection
     */
    private void parseAnalysisProtocolCollection(Set<String> neededSpectrumIdentificationProtocols,
            Set<String> neededAnalysisSoftwares) {
        // get the necessary AnalysisProtocolCollection:SpectrumIdentificationProtocol
        AnalysisProtocolCollection analysisProtocolCollection = unmarshaller.unmarshal(AnalysisProtocolCollection.class);
        // this protocol is needed, add it to the PIAFile

        List<SpectrumIdentificationProtocol> idProtocols =
                analysisProtocolCollection.getSpectrumIdentificationProtocol().stream()
                        .filter(idProtocol -> neededSpectrumIdentificationProtocols.contains(idProtocol.getId()))
                        .collect(Collectors.toList());

        for (SpectrumIdentificationProtocol idProtocol : idProtocols) {
            // this protocol is needed, add it to the PIAFile
            file.addSpectrumIdentificationProtocol(idProtocol);
            neededAnalysisSoftwares.add(idProtocol.getAnalysisSoftwareRef());

            // look through the enzymes and get regexes, when not given
            if ((idProtocol.getEnzymes() != null) && (idProtocol.getEnzymes().getEnzyme() != null)) {

                for (Enzyme enzyme : idProtocol.getEnzymes().getEnzyme()) {
                    checkEnzymeRegEx(enzyme);
                }
            } else {
                LOGGER.warn("No enzymes in mzIdentML, this should not happen!");
            }

        }
    }


    /**
     * Checks whether the given enzyme has a regular expression in the mzIdentML file, otherwise tries to look it up
     * from the OBO file and maps it.
     *
     * @param enzyme
     */
    private void checkEnzymeRegEx(Enzyme enzyme) {
        ParamList enzymeName = enzyme.getEnzymeName();

        if ((enzyme.getSiteRegexp() == null) && (enzymeName != null)) {
            List<AbstractParam> paramGroup = enzymeName.getParamGroup();
            for (AbstractParam param : paramGroup) {
                if (param instanceof CvParam) {
                    String oboID = ((CvParam) param).getAccession();
                    getAndSetEnzymeRegexFromOBO(oboID, enzyme);
                } else {
                    // TODO: parse the enzyme regex from a userParam
                    LOGGER.error("unsupported enzyme: " + param.getName() + " / " + param.getValue());
                }
            }
        }
    }


    /**
     * Get the enzyme's regular expression from the given OBO id and put it into the enzymes' regexes map.
     *
     * @param oboID
     */
    private void getAndSetEnzymeRegexFromOBO(String oboID, Enzyme enzyme) {
        Term oboTerm = compiler.getOBOMapper().getTerm(oboID);
        if (oboTerm != null) {
            Set<Triple> tripleSet = compiler.getOBOMapper().getTriples(oboTerm, null, null);

            // get the regexes
            List<String> regexes = tripleSet.stream()
                    .filter(triple -> triple.getPredicate().getName().equals(AbstractOBOMapper.OBO_RELATIONSHIP)
                            && triple.getObject().getName().startsWith(OBOMapper.OBO_HAS_REGEXP))
                    .map(triple -> {
                        // put filtered regExes into the map
                        String regExpID = triple.getObject().getName().substring(11).trim();
                        Term regExpTerm = compiler.getOBOMapper().getTerm(regExpID);
                        return StringEscapeUtils.unescapeJava(regExpTerm.getDescription());
                    })
                    .collect(Collectors.toList());

            // set the regex
            if (!regexes.isEmpty()) {
                enzyme.setSiteRegexp(regexes.get(0));
            }
        }
    }

    /**
     * Getter for the Pride Mod Reader allowing to retrieve information from
     * UNIMOD and PSI-MOD at the same time.
     *
     * Unimod and PSI-MOD
     *
     * @return
     */
    public static final ModReader getModReader() {
        if (modReader == null) {
            LOGGER.info("Initializing PRIDE ModReader parser...");
            modReader = ModReader.getInstance();
        }
        return modReader;
    }


    /**
     * Add the SpectrumIdentificationList and all its contents to the compiler.
     *
     * @param specIDList
     * @return
     */
    private boolean addSpectrumIdentificationList(SpectrumIdentificationList specIDList) {
        // get some information from the SpectrumIdentification
        Set<String> specIDListsDBRefs = new HashSet<>();
        SpectrumIdentification spectrumID = null;
        Enzymes specIDListsEnzymes = null;

        String analysisSoftwareName = null;

        for (SpectrumIdentification specID : file.getAnalysisCollection().getSpectrumIdentification()) {
            if (specID.getId().equals(specIdListIDtoSpecIdID.get(specIDList.getId()))  ) {
                // this is the SpectrumIdentification for this list
                specIDListsDBRefs.addAll(specID.getSearchDatabaseRef().stream().map(SearchDatabaseRef::getSearchDatabaseRef).collect(Collectors.toList()));
                spectrumID = specID;

                // get the enzymes
                SpectrumIdentificationProtocol idProtocol = specID.getSpectrumIdentificationProtocol();
                specIDListsEnzymes = idProtocol.getEnzymes();

                // get the analysis software
                AnalysisSoftware analysisSoftware = idProtocol.getAnalysisSoftware();
                if (analysisSoftware != null) {
                    analysisSoftwareName = analysisSoftware.getName();
                }
                break;
            }
        }

        // go through all the SpectrumIdentificationResults and build the PSMs
        LOGGER.debug("Processing " + specIDList.getSpectrumIdentificationResult().size() + " specIdResults");
        boolean ok = true;
        for (SpectrumIdentificationResult specIdRes : specIDList.getSpectrumIdentificationResult()) {
            ok = addSpectrumIdentificationResult(specIdRes, spectrumID, specIDListsDBRefs, specIDListsEnzymes,
                    analysisSoftwareName, specIdRes.getSpectraDataRef());
            if (!ok) {
                break;
            }
        }

        return ok;
    }


    /**
     * Add the SpectrumIdentificationResult and all its contents to the compiler.
     *
     * @param specIdResult
     * @param spectrumID
     * @param specIDListsDBRefs
     * @param specIDListsEnzymes
     * @return
     */
    private boolean addSpectrumIdentificationResult(SpectrumIdentificationResult specIdResult,
            SpectrumIdentification spectrumID, Set<String> specIDListsDBRefs, Enzymes specIDListsEnzymes,
            String analysisSoftwareName, String spectraDataRef) {
        String sourceID = parseSourceID(specIdResult);
        String spectrumTitle = parseSpectrumTitle(specIdResult);
        Double retentionTime = parseRetentionTime(specIdResult);

        // stores the not specially parsed cvParams
        ParamList resultParams = new ParamList();
        specIdResult.getParamGroup().stream()
                .filter(param -> !((param instanceof CvParam) && parsedSpecIdResultCVParams.contains(((CvParam)param).getAccession())))
                .forEach(resultParams.getParamGroup()::add);

        boolean ok = true;
        for (SpectrumIdentificationItem specIdItem : specIdResult.getSpectrumIdentificationItem()) {

            ok = processSpectrumIdentificationItem(specIdItem, resultParams, sourceID, spectrumTitle, retentionTime,
                    spectrumID, specIDListsDBRefs, specIDListsEnzymes, analysisSoftwareName, spectraDataRef);
            if (!ok) {
                break;
            }
        }
        return ok;
    }


    /**
     * Parse the source ID, either from the spectrumID or cvParams.
     *
     * @param specIdResult
     * @return
     */
    private static String parseSourceID(SpectrumIdentificationResult specIdResult) {
        // this is the preferred way to pass the spectrum ID or run number
        String sourceID = specIdResult.getSpectrumID();

        if (sourceID != null) {
            // in some files, the spectrumID has a longer description, which needs some parsing
            Matcher matcher = MzIdentMLTools.patternScanInTitle.matcher(sourceID);
            if (matcher.matches()) {
                sourceID = "scan=" + matcher.group(1);
            }
        } else {
            // the sourceID is the same as the scan number in the cvParam
            List<String> scanNumbers = specIdResult.getCvParam().stream()
                    .filter(cvParam -> cvParam.getAccession().equals(OntologyConstants.SCAN_NUMBERS.getPsiAccession()))
                    .map(AbstractParam::getValue)
                    .sorted()
                    .distinct()
                    .collect(Collectors.toList());

            if (!scanNumbers.isEmpty()) {
                sourceID = "index=" + scanNumbers.get(0);
            }
        }

        return sourceID;
    }


    /**
     * Gets the spectrum title from the appropriate cvParam or the spectrumID.
     *
     * @param specIdResult
     * @return
     */
    private static String parseSpectrumTitle(SpectrumIdentificationResult specIdResult) {
        String spectrumTitle = null;

        // get the "scan start time" cvParams
        List<CvParam> spectrumTitleCvParams = specIdResult.getCvParam().stream()
            .filter(cvParam -> cvParam.getAccession().equals(OntologyConstants.SPECTRUM_TITLE.getPsiAccession()))
            .collect(Collectors.toList());

        if (!spectrumTitleCvParams.isEmpty() && spectrumTitleCvParams.get(0).getValue().contains("scan")) {
            spectrumTitle = spectrumTitleCvParams.get(0).getValue();
        }

        if ((spectrumTitle == null) && (specIdResult.getSpectrumID() != null)) {
            // no spectrumTitle but a spectrumID, which can be used
            Matcher matcher = MzIdentMLTools.patternScanInTitle.matcher(specIdResult.getSpectrumID());
            if (matcher.matches()) {
                spectrumTitle = specIdResult.getSpectrumID();
            }
        }

        return spectrumTitle;
    }


    /**
     * Parses the retention time in seconds from the respective cvParam.
     *
     * @param specIdResult
     * @return
     */
    private static Double parseRetentionTime(SpectrumIdentificationResult specIdResult) {
        Double rt = null;

        // get the "scan start time" cvParams
        List<CvParam> scanStartCvParams = specIdResult.getCvParam().stream()
            .filter(cvParam -> cvParam.getAccession().equals(OntologyConstants.SCAN_START_TIME.getPsiAccession()))
            .collect(Collectors.toList());

        for (CvParam cvParam : scanStartCvParams) {
            try {
                rt = Double.parseDouble(cvParam.getValue());

                String unitAccession = cvParam.getUnitAccession();
                if ((UNIT_ACCESSION_MINUTES.equals(unitAccession))) {
                    rt = rt * 60.0;
                }

                break;
            } catch (NumberFormatException ex) {
                rt = null;
            }
        }

        return rt;
    }


    /**
     * Add the SpectrumIdentificationItem and all its contents to the compiler.
     *
     * @param specIdItem
     * @param resultParams
     * @param sourceID
     * @param spectrumTitle
     * @param retentionTime
     * @param spectrumID
     * @param specIDListsDBRefs
     * @param specIDListsEnzymes
     * @return
     */
    private boolean processSpectrumIdentificationItem(SpectrumIdentificationItem specIdItem, ParamList resultParams,
            String sourceID, String spectrumTitle, Double retentionTime, SpectrumIdentification spectrumID,
            Set<String> specIDListsDBRefs, Enzymes specIDListsEnzymes, String analysisSoftwareName, String spectraDataRef) {
        double deltaMass = calculateDeltaMass(specIdItem);
        uk.ac.ebi.jmzidml.model.mzidml.Peptide peptide = peptides.get(specIdItem.getPeptideRef());

        Peptide pep = parseSIIPeptideEvidences(specIdItem.getPeptideEvidenceRef(), peptide, specIDListsDBRefs);

        boolean isDecoy = true;
        for(PeptideEvidenceRef peptideEvidenceRef: specIdItem.getPeptideEvidenceRef()){
            if(peptideEvidenceRef != null && peptideEvidenceRef.getPeptideEvidence() != null &&
                    !peptideEvidenceRef.getPeptideEvidence().isIsDecoy())
                isDecoy = false;

        }
        if (pep == null) {
            return false;
        }
        String sequence = pep.getSequence();

        // calculate the missed cleavages
        // TODO: how do multiple and independent enzymes behave???
        int missed = calculateMissedCleavages(sequence, specIDListsEnzymes);

        // create the PeptideSpectrumMatch object
        PeptideSpectrumMatch psm;
        psm = compiler.createNewPeptideSpectrumMatch(specIdItem.getChargeState(),
                specIdItem.getExperimentalMassToCharge(),
                deltaMass,
                retentionTime,
                sequence,
                missed,
                sourceID,
                spectrumTitle,
                file,
                spectrumID,
                spectraDataRef);
        psm.setIsDecoy(isDecoy);

        AbstractParam cv = new CvParam();
        ((CvParam) cv).setCv(PRIDETools.PrideOntologyConstants.PRIDE_SUBMITTERS_THERSHOLD.getCv());
        ((CvParam) cv).setAccession(PRIDETools.PrideOntologyConstants.PRIDE_SUBMITTERS_THERSHOLD.getAccession());
        cv.setName(PRIDETools.PrideOntologyConstants.PRIDE_SUBMITTERS_THERSHOLD.getName());
        cv.setValue(Boolean.toString(specIdItem.isPassThreshold()));
        psm.addParam(cv);

        pep.addSpectrum(psm);
        specNr++;

        // get the cvParams add them to the PSM
        for (CvParam cvParam : specIdItem.getCvParam()) {
            Term oboTerm = compiler.getOBOMapper().getTerm(cvParam.getAccession());
            ScoreModel score = parseOBOTermAsScore(oboTerm, cvParam.getValue());

            if (score != null) {
                psm.addScore(score);
            } else {
                // add the cvParam to the params of the PSM
                psm.addParam(cvParam);
            }
        }

        // add the userParam to the params of the PSM

        specIdItem.getUserParam().forEach(userParam -> addUserParamToPSM(psm, userParam, analysisSoftwareName));

        // add the params from the specIdResult to the PSM
        resultParams.getParamGroup().forEach(psm::addParam);

        // adding the modifications
        // the modifications are in SequenceCollection:Peptide
        if (peptide != null) {
            for (Modification mod : peptide.getModification()) {
                processModification(mod, sequence, psm);
            }
        } else {
            LOGGER.warn("no peptide for the peptide_ref " + specIdItem.getPeptideRef() +
                    " in the SequenceCollection -> can't get Modifications for it.");
        }

        // the PSM is finished here
        compiler.insertCompletePeptideSpectrumMatch(psm);

        return true;
    }


    /**
     * Calculates the deltaMass of the given spectrumIdentificationItem
     * @param specIdItem
     * @return
     */
    private static double calculateDeltaMass(SpectrumIdentificationItem specIdItem) {
        double deltaMass;
        if (specIdItem.getCalculatedMassToCharge() != null) {
            deltaMass = (specIdItem.getExperimentalMassToCharge() -
                    specIdItem.getCalculatedMassToCharge()) *
                    specIdItem.getChargeState();
        } else {
            deltaMass = Double.NaN;
        }
        return deltaMass;
    }


    /**
     * Processed the peptideEvidences of a SpectrumIdentificationItem.
     *
     * @param peptideEvidenceRefs
     * @param peptide
     * @param specIDListsDBRefs
     * @return
     */
    private Peptide parseSIIPeptideEvidences(List<PeptideEvidenceRef> peptideEvidenceRefs,
            uk.ac.ebi.jmzidml.model.mzidml.Peptide peptide, Set<String> specIDListsDBRefs) {
        Peptide pep = null;
        String sequence = null;
        for (PeptideEvidenceRef pepEvRef : peptideEvidenceRefs) {
            PeptideEvidence pepEvidence = peptideEvidences.get(pepEvRef.getPeptideEvidenceRef());

            if (pepEvidence == null) {
                LOGGER.error("PeptideEvidence " + pepEvRef.getPeptideEvidenceRef() + " not found!");
                return null;
            }

            DBSequence dbSeq = dbSequences.get(pepEvidence.getDBSequenceRef());
            if (dbSeq == null) {
                LOGGER.error("DBSequence " + pepEvidence.getDBSequenceRef() + " not found!");
                return null;
            }

            Integer start = pepEvidence.getStart();
            Integer end = pepEvidence.getEnd();
            String proteinSequence = dbSeq.getSeq();
            String pepEvSequence = getPeptideEvidenceSequence(start, end, peptide, proteinSequence);

            if (sequence == null) {
                // first sequence of peptide evidence will be the PSM sequence
                sequence = pepEvSequence;
            } else {
                if (!sequence.equals(pepEvSequence)) {
                    LOGGER.error("Different sequences found for a PSM: " + sequence + " != " + pepEvSequence);
                    return null;
                }
            }

            Accession acc = addAccessionInformationFromPeptideEvidence(dbSeq, proteinSequence, specIDListsDBRefs);

            // create the peptide or create new connections
            pep = addPeptideInformationFromPeptideEvidence(pep, sequence, acc, start, end);
        }

        return pep;
    }


    /**
     * Get the sequence for this peptide evidence, either from the peptide or the proteinSequence.
     *
     * @param start
     * @param end
     * @param peptide
     * @param proteinSequence
     * @return
     */
    private static String getPeptideEvidenceSequence(Integer start, Integer end,
            uk.ac.ebi.jmzidml.model.mzidml.Peptide peptide, String proteinSequence) {
        String pepEvSequence = null;

        if (peptide != null) {
            pepEvSequence = peptide.getPeptideSequence();
        } else if ((start != null) && (end != null) && (proteinSequence != null)) {
            pepEvSequence = proteinSequence.substring(start-1, end);
        } else {
            LOGGER.error("No peptide sequence found for a peptide!");
        }

       if ((start != null) && (end != null) && (proteinSequence != null) && (peptide != null)
                    && proteinSequence.trim().length() > 0) {

            // some exporters get the start and stop of sequences wrong
            if (start-1 < 0) {
                start++;
            }
            String dbEvSeq = proteinSequence.substring(start-1, end);

            if ((dbEvSeq != null) && !dbEvSeq.equals(pepEvSequence)) {
                LOGGER.warn("PSM sequence fromSearchDB differs to sequence from Peptide: " +
                        dbEvSeq + " != " + pepEvSequence + ". Only sequence from Peptide is used.");
            }
        }

        return pepEvSequence;
    }


    /**
     * Add the accession of the peptideEvidence to the compiler, if not already there.
     * @param dbSeq
     * @param proteinSequence
     * @param specIDListsDBRefs
     * @return
     */
    private Accession addAccessionInformationFromPeptideEvidence(DBSequence dbSeq, String proteinSequence,
            Set<String> specIDListsDBRefs) {
        // add the Accession to the compiler (if it is not already there)
        FastaHeaderInfos accHeader = FastaHeaderInfos.parseHeaderInfos(dbSeq);

        Accession acc = compiler.getAccession(accHeader.getAccession());
        if (acc == null) {
            acc = compiler.insertNewAccession(accHeader.getAccession(), proteinSequence);
            accNr++;
        }

        acc.addFile(file.getID());

        if (accHeader.getDescription() != null) {
            acc.addDescription(file.getID(), accHeader.getDescription());
        }

        acc.addSearchDatabaseRefs(specIDListsDBRefs);

        if (proteinSequence != null) {
            if ((acc.getDbSequence() != null) &&
                    !proteinSequence.equals(acc.getDbSequence())) {
                LOGGER.warn("Different DBSequences found for same Accession, this is not suported!\n" +
                        "\t Accession: " + acc.getAccession() +
                        '\t' + dbSeq.getSeq() + '\n' +
                        '\t' + acc.getDbSequence());
            } else if (acc.getDbSequence() == null) {
                // found a sequence now
                acc.setDbSequence(proteinSequence);
            }
        }

        return acc;
    }


    /**
     * Adds new peptide information or creates the peptide, if necessary
     *
     * @param pep
     * @param sequence
     * @param acc
     * @param start
     * @param end
     * @return
     */
    private Peptide addPeptideInformationFromPeptideEvidence(Peptide pep, String sequence,
            Accession acc, Integer start, Integer end) {
        Peptide peptide = pep;
        if (peptide == null) {
            // add the Peptide to the compiler (if it is not already there)
            peptide = compiler.getPeptide(sequence);
            if (peptide == null) {
                peptide = compiler.insertNewPeptide(sequence);
                pepNr++;
            }
        }

        // add the accession occurrence to the peptide
        if ((start != null) && (end != null)) {
            peptide.addAccessionOccurrence(acc, start, end);
        }

        // now insert the connection between peptide and accession into the compiler
        compiler.addAccessionPeptideConnection(acc, peptide);

        return peptide;
    }


    /**
     * Calculates the number of missed cleavages for the given enzymes
     *
     * @param sequence
     * @param specIDListsEnzymes
     * @return
     */
    private static int calculateMissedCleavages(String sequence, Enzymes specIDListsEnzymes) {
        int missed = 0;
        if (specIDListsEnzymes != null) {
            for (Enzyme enzyme : specIDListsEnzymes.getEnzyme()) {
                String regExp = enzyme.getSiteRegexp();

                if (regExp == null) {
                    // no regexpt found -> set the missed cleavages to -1, because it is not calculable
                    missed = -1;
                    break;
                }

                missed += sequence.split(regExp).length - 1;
            }
        }

        return missed;
    }


    /**
     * Parses the given OBO term as a score. If it is no score, returns null, otherwise the score.
     *
     * @param oboTerm
     * @param value
     * @return
     */
    private ScoreModel parseOBOTermAsScore(Term oboTerm, String value) {
        ScoreModel score = null;

        if (oboTerm != null) {
            // the score is in the OBO file, get the relations etc.
            Set<Triple> tripleSet = compiler.getOBOMapper().getTriples(oboTerm, null, null);

            for (Triple triple : tripleSet) {
                if (triple.getPredicate().getName().equals(AbstractOBOMapper.OBO_IS_A) &&
                        triple.getObject().getName().equals(OntologyConstants.SEARCH_ENGINE_PSM_SCORE.getPsiAccession())) {
                    // subject is a "search engine specific score for PSM"
                    double doubleValue = Double.parseDouble(value);
                    score = new ScoreModel(doubleValue,
                            StringEscapeUtils.unescapeJava(oboTerm.getName()),
                            StringEscapeUtils.unescapeJava(oboTerm.getDescription()));
                }
            }
        }

        return score;
    }


    /**
     * Processed the given modification and adds it to the PSM.
     *
     * @param mod
     * @param sequence
     * @param psm
     */
    private static void processModification(Modification mod, String sequence, PeptideSpectrumMatch psm) {
        if (mod.getLocation() == null) {
            LOGGER.warn("Cannot build modification without location, skipping.");
        }

        de.mpc.pia.intermediate.Modification modification;

        Character residue;
        String description = null;
        String accession = null;
        Double massDelta = mod.getMonoisotopicMassDelta();

        if ((mod.getLocation() == 0) || (mod.getLocation() > sequence.length())) {
            residue = '.';
        } else {
            residue = sequence.charAt(mod.getLocation() - 1);
        }

        for (CvParam param : mod.getCvParam()) {
            // get the cvParam, which maps to UNIMOD, this is the description
            if ("UNIMOD".equals(param.getCvRef())) {
                description = param.getName();
                accession = param.getAccession();
                break;
            }
        }

        if(massDelta == null && accession.contains("UNIMOD")){
            massDelta = getModReader().getPTMbyAccession(accession).getMonoDeltaMass();
        }
        modification = new de.mpc.pia.intermediate.Modification(
                residue, massDelta, description,
                accession);

        psm.addModification(mod.getLocation(), modification);
    }


    /**
     * Adds the given userParam to the given PSM. Makes some checks to convert the userParam to a score or other param.
     *
     * @param psm
     * @param userParam
     */
    private static void addUserParamToPSM(PeptideSpectrumMatch psm, UserParam userParam, String analysisSoftwareName) {
        boolean processed = false;

        if(analysisSoftwareName != null){
            if ("comet".equalsIgnoreCase(analysisSoftwareName.trim())) {
                // this score seems to originate from a Comet identification
                processed = checkParamForCometSpecifics(psm, userParam);
                if (!processed) {
                    processed = checkParamForPercolatorSpecifics(psm, userParam);
                }
            } else if ("mascot".equalsIgnoreCase(analysisSoftwareName.trim())) {
                processed = checkParamForPercolatorSpecifics(psm, userParam);
            }

            if (!processed) {
                psm.addParam(userParam);
            }
        }

    }


    /**
     * Checks and processes any unprocessed or accidentally as userParam marked params for Comet specific params.
     *
     * @param psm
     * @param userParam
     */
    private static boolean checkParamForCometSpecifics(PeptideSpectrumMatch psm, UserParam userParam) {
        String paramName = userParam.getName().trim().toLowerCase();
        boolean isScore;
        OntologyConstants foundParam;

        switch (paramName) {
        case "xcorr":
            isScore = true;
            foundParam = OntologyConstants.COMET_XCORR;
            break;

        case "deltacn":
            isScore = true;
            foundParam = OntologyConstants.COMET_DELTA_CN;
            break;

        case "deltacnstar":
            isScore = true;
            foundParam = OntologyConstants.COMET_DELTA_CN_STAR;
            break;

        case "spscore":
            isScore = true;
            foundParam = OntologyConstants.COMET_SP;
            break;

        case "sprank":
            isScore = true;
            foundParam = OntologyConstants.COMET_SP_RANK;
            break;

        case "expect":
            isScore = true;
            foundParam = OntologyConstants.COMET_EXPECTATION;
            break;

        default:
            isScore = false;
            foundParam = null;
        }

        boolean processed = false;
        if (isScore && (foundParam != null)) {
            processed = addScoreFromParam(psm, userParam, foundParam);
        }

        return processed;
    }


    /**
     * Checks and processes any unprocessed or accidentally as userParam marked params for Percolator specific params.
     *
     * @param psm
     * @param userParam
     */
    private static boolean checkParamForPercolatorSpecifics(PeptideSpectrumMatch psm, UserParam userParam) {
        String paramName = userParam.getName().trim().toLowerCase();
        boolean isScore;
        OntologyConstants foundParam;

        switch (paramName) {
        case "q value":
            isScore = true;
            foundParam = OntologyConstants.PERCOLATOR_Q_VALUE;
            break;

        case "pep":
            isScore = true;
            foundParam = OntologyConstants.PERCOLATOR_POSTERIOR_ERROR_PROBABILITY;
            break;

        default:
            isScore = false;
            foundParam = null;
        }

        boolean processed = false;
        if (isScore && (foundParam != null)) {
            processed = addScoreFromParam(psm, userParam, foundParam);
        }

        return processed;
    }


    /**
     * Adds the score given by the ontology to the PSM using the value from the userParam.
     *
     * @param psm
     * @param userParam
     * @param ontology
     * @return
     */
    private static boolean addScoreFromParam(PeptideSpectrumMatch psm, UserParam userParam, OntologyConstants ontology) {
        boolean processed = false;

        try {
            ScoreModel score = new ScoreModel(Double.NaN,
                    ontology.getPsiAccession(),
                    ontology.getPsiName());
            Double scoreValue = Double.parseDouble(userParam.getValue());
            score.setValue(scoreValue);
            psm.addScore(score);
            processed = true;
        } catch (NumberFormatException e) {
            LOGGER.error("Could not parse score value", e);
        }

        return processed;
    }


    /**
     * Checks, whether the given file looks like an mzIdentML file
     *
     * @param fileName
     * @return
     */
    public static boolean checkFileType(String fileName) {
        boolean isMzIdentMLFile = false;
        LOGGER.debug("checking whether this is an mzIdentML file: " + fileName);

        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            // read in the first 10, not empty lines
            List<String> lines = stream.filter(line -> !line.trim().isEmpty())
                    .limit(10)
                    .collect(Collectors.toList());

            // check, if first lines are ok
            int idx = 0;

            // optional declaration
            if (lines.get(idx).trim().matches("<\\?xml version=\"[0-9.]+\"( encoding=\"[^\"]+\"){0,1}( standalone=\\\"[^\\\"]+\\\"){0,1}\\?>")) {
                LOGGER.debug("file has the XML declaration line:" + lines.get(idx));
                idx++;
            }

            // optional stylesheet declaration
            if (lines.get(idx).trim().matches("<\\?xml-stylesheet.+\\?>")) {
                LOGGER.debug("file has the XML stylesheet line:" + lines.get(idx));
                idx++;
            }

            // now the MzIdentML element must be next
            if (lines.get(idx).trim().matches("<MzIdentML .+")) {
                isMzIdentMLFile = true;
                LOGGER.debug("file has the MzIdentML element: " + lines.get(idx));
            }
        } catch (Exception e) {
            LOGGER.error("Could not check file " + fileName, e);
        }

        return isMzIdentMLFile;
    }
}
