package de.mpc.pia.intermediate.compiler.parser;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.tools.MzIdentMLTools;
import de.mpc.pia.tools.OntologyConstants;
import de.mpc.pia.tools.obo.OBOMapper;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.biojava.nbio.ontology.Term;
import org.biojava.nbio.ontology.Triple;
import uk.ac.ebi.jmzidml.model.mzidml.*;
import uk.ac.ebi.jmzidml.xml.io.MzIdentMLUnmarshaller;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;


/**
 * This class parses the data from a mzIdentML file for a given
 * {@link PIACompiler}.<br/>
 *
 * @author julian
 *
 */
class MzIdentMLFileParser {

    /** logger for this class */
    private static final Logger logger = Logger.getLogger(MzIdentMLFileParser.class);


    /**
     * We don't ever want to instantiate this class
     */
    private MzIdentMLFileParser() {
        throw new AssertionError();
    }


    /**
     * Parses the data from an mzIdentML file given by its NAME into the given
     * {@link PIACompiler}.
     *
     * @param fileName NAME of the mzIdentML file
     */
    public static boolean getDataFromMzIdentMLFile(String name, String fileName,
            PIACompiler compiler) {
        // Open the input mzIdentML file for parsing
        File mzidFile = new File(fileName);

        if (!mzidFile.canRead()) {
            // TODO: better error / exception
            logger.error("could not read '" + fileName + "'.");
            return false;
        }

        PIAInputFile file = compiler.insertNewFile(name, fileName,
                InputFileParserFactory.InputFileTypes.MZIDENTML_INPUT.getFileSuffix());

        // TODO: make one PIAInpitFile per SIL (and look out for consensus lists!)

        MzIdentMLUnmarshaller unmarshaller = new MzIdentMLUnmarshaller(mzidFile);

        // maps from the ID to the SpectrumIdentificationList
        Map<String, SpectrumIdentificationList> specIdLists =
                new HashMap<>();

        // maps from the file's ID to the compiler's ID of the SpectrumIdentification
        Map<String, String> spectrumIdentificationRefs = new HashMap<>();

        // maps from the file's ID to the compiler's ID of the SpectrumIdentificationProtocol
        Map<String, String> spectrumIdentificationProtocolRefs = new HashMap<>();

        // maps from the file's ID to the compiler's SpectraData
        Map<String, SpectraData> spectraDataRefs = new HashMap<>();

        // maps from the file's ID to the compiler's searchDB
        Map<String, SearchDatabase> searchDBRefs = new HashMap<>();

        // maps from the file's ID to the compiler's analysisSoftware
        Map<String, AnalysisSoftware> analysisSoftwareRefs = new HashMap<>();

        // maps from the ID to the PeptideEvidence
        Map<String, PeptideEvidence> peptideEvidences = new HashMap<>();

        // maps from the ID to the DBSequence
        Map<String, DBSequence> dbSequences = new HashMap<>();

        // maps from the ID to the Protein
        Map<String, uk.ac.ebi.jmzidml.model.mzidml.Peptide> peptides = new HashMap<>();

        // maps from the SpectrumIdentificationList IDs to the SpectrumIdentification IDs
        Map<String, String> specIdListIDtoSpecIdID = new HashMap<>();

        // maps from the enzyme ID (from the OBO) to the regex, only used, if no siteRegex param is given
        Map<String, String> enzymesToRegexes = new HashMap<>();


        Set<String> neededSpectrumIdentificationProtocols = new HashSet<>();
        Set<String> neededSpectraData = new HashSet<>();
        Set<String> neededSearchDatabases= new HashSet<>();
        Set<String> neededAnalysisSoftwares= new HashSet<>();

        int accNr = 0;
        int pepNr = 0;
        int specNr = 0;

        // get the SpectrumIdentificationLists
        AnalysisData analysisData = unmarshaller.unmarshal(AnalysisData.class);
        for (SpectrumIdentificationList specIdList
                : analysisData.getSpectrumIdentificationList()) {
            specIdLists.put(specIdList.getId(), specIdList);
        }

        // get the AnalysisCollection:SpectrumIdentification for the SpectrumIdentificationLists
        AnalysisCollection analysisCollection =
                unmarshaller.unmarshal(AnalysisCollection.class);
        for (SpectrumIdentification si
                : analysisCollection.getSpectrumIdentification()) {
            if (specIdLists.keySet().contains(si.getSpectrumIdentificationListRef())) {
                // if the SpectrumIdentification's SpectrumIdentificationList is in the file, we need the SpectrumIdentification
                String ref = si.getId();
                String specIdListID = si.getSpectrumIdentificationListRef();
                String id = file.addSpectrumIdentification(si);
                spectrumIdentificationRefs.put(ref, id);

                specIdListIDtoSpecIdID.put(specIdListID, id);

                neededSpectrumIdentificationProtocols.add(si.getSpectrumIdentificationProtocolRef());
                neededSpectraData.addAll(si.getInputSpectra().stream().map(InputSpectra::getSpectraDataRef).collect(Collectors.toList()));
                neededSearchDatabases.addAll(si.getSearchDatabaseRef().stream().map(SearchDatabaseRef::getSearchDatabaseRef).collect(Collectors.toList()));
            } else {
                // TODO: better error / exception
                logger.warn("file contains SpectrumIdentification (" +
                        si.getId() + ") without SpectrumIdentificationList!");
            }
        }

        // get the necessary AnalysisProtocolCollection:SpectrumIdentificationProtocol
        AnalysisProtocolCollection analysisProtocolCollection =
                unmarshaller.unmarshal(AnalysisProtocolCollection.class);
        // this protocol is needed, add it to the PIAFile
// look through the enzymes and get regexes, when not given
// no siteRegexp given, so look for it in the obo file
// we still need the regExp for this enzyme
//Todo: We should use this in some way
        analysisProtocolCollection.getSpectrumIdentificationProtocol().stream().filter(idProtocol -> neededSpectrumIdentificationProtocols.contains(idProtocol.getId())).forEach(idProtocol -> {
            // this protocol is needed, add it to the PIAFile
            String ref = idProtocol.getId();
            String id = file.addSpectrumIdentificationProtocol(idProtocol);
            spectrumIdentificationProtocolRefs.put(ref, id);
            neededAnalysisSoftwares.add(idProtocol.getAnalysisSoftwareRef());

            // look through the enzymes and get regexes, when not given
            if ((idProtocol.getEnzymes() != null) &&
                    (idProtocol.getEnzymes().getEnzyme() != null)) {
                for (Enzyme enzyme : idProtocol.getEnzymes().getEnzyme()) {
                    ParamList enzymeName = enzyme.getEnzymeName();
                    if ((enzyme.getSiteRegexp() == null) &&
                            (enzymeName != null)) {
                        // no siteRegexp given, so look for it in the obo file
                        List<AbstractParam> paramList = enzymeName.getParamGroup();
                        if (paramList.size() > 0) {

                            if (paramList.get(0) instanceof CvParam) {
                                String oboID = ((CvParam) (paramList.get(0))).getAccession();

                                if (enzymesToRegexes.get(oboID) == null) {
                                    // we still need the regExp for this enzyme

                                    Term oboTerm = compiler.getOBOMapper().getTerm(oboID);
                                    if (oboTerm != null) {
                                        Set<Triple> tripleSet = compiler.getOBOMapper().getTriples(oboTerm, null, null);

                                        tripleSet.stream().filter(triple -> triple.getPredicate().getName().equals(OBOMapper.obo_relationship) &&
                                                triple.getObject().getName().startsWith(OBOMapper.obo_has_regexp)).forEach(triple -> {
                                            String regExpID = triple.getObject().getName().substring(11).trim();
                                            Term regExpTerm = compiler.getOBOMapper().getTerm(regExpID);

                                            if (regExpTerm != null) {
                                                enzymesToRegexes.put(oboID, StringEscapeUtils.unescapeJava(regExpTerm.getDescription()));
                                            }
                                        });
                                    }
                                }

                            } else if (paramList.get(0) instanceof UserParam) {
                                //Todo: We should use this in some way
                            }
                        }
                    }
                }
            } else {
                logger.warn("No enzymes in mzIdentML, this should not happen!");
            }

        });

        // get the necessary inputs:SpectraData
        Inputs inputs = unmarshaller.unmarshal(Inputs.class);
        inputs.getSpectraData().stream().filter(spectraData -> neededSpectraData.contains(spectraData.getId())).forEach(spectraData -> {
            String ref = spectraData.getId();
            SpectraData sd = compiler.putIntoSpectraDataMap(spectraData);
            spectraDataRefs.put(ref, sd);
        });

        // get the necessary inputs:SearchDBs
        inputs.getSearchDatabase().stream().filter(searchDB -> neededSearchDatabases.contains(searchDB.getId())).forEach(searchDB -> {
            String ref = searchDB.getId();
            SearchDatabase sDB = compiler.putIntoSearchDatabasesMap(searchDB);
            searchDBRefs.put(ref, sDB);
        });

        // get the necessary AnalysisSoftwares
        AnalysisSoftwareList analysisSoftwareList = unmarshaller.unmarshal(AnalysisSoftwareList.class);
        analysisSoftwareList.getAnalysisSoftware().stream().filter(software -> neededAnalysisSoftwares.contains(software.getId())).forEach(software -> {
            String ref = software.getId();
            AnalysisSoftware as = compiler.putIntoSoftwareMap(software);
            analysisSoftwareRefs.put(ref, as);
        });

        // update the PIAFile's references for SpectraData, SearchDBs and AnalysisSoftwares
        file.updateReferences(spectraDataRefs, searchDBRefs, analysisSoftwareRefs);


        // get/hash the SequenceCollection:PeptideEvidences
        SequenceCollection sc = unmarshaller.unmarshal(SequenceCollection.class);
        for (PeptideEvidence pepEvidence : sc.getPeptideEvidence()) {
            peptideEvidences.put(pepEvidence.getId(), pepEvidence);
        }

        // get/hash the SequenceCollection:DBSequences
        for (DBSequence dbSeq : sc.getDBSequence()) {
            dbSequences.put(dbSeq.getId(), dbSeq);
        }

        // get/hash the SequenceCollection:Peptides
        for (uk.ac.ebi.jmzidml.model.mzidml.Peptide peptide: sc.getPeptide()) {
            peptides.put(peptide.getId(), peptide);
        }


        // go through the SpectrumIdentificationList:SpectrumIdentificationResult:SpectrumIdentificationItem and build the PeptideSpectrumMatches, Accessions and Peptides
        for (SpectrumIdentificationList specIDList : specIdLists.values()) {

            // get some information from the SpectrumIdentification
            Set<String> specIDListsDBRefs = new HashSet<>();
            SpectrumIdentification spectrumID = null;
            Enzymes specIDListsEnzymes = null;
            Set<InputSpectra> inputSpectraSet = new HashSet<>();
            for (SpectrumIdentification specID
                    : file.getAnalysisCollection().getSpectrumIdentification()) {
                if (specID.getId().equals(specIdListIDtoSpecIdID.get(specIDList.getId()))) {
                    // this is the SpectrumIdentification for this list
                    specIDListsDBRefs.addAll(specID.getSearchDatabaseRef().stream().map(SearchDatabaseRef::getSearchDatabaseRef).collect(Collectors.toList()));
                    spectrumID = specID;

                    // get the enzymes
                    specIDListsEnzymes =
                            specID.getSpectrumIdentificationProtocol().getEnzymes();
                    inputSpectraSet.addAll(specID.getInputSpectra());

                    break;
                }
            }




            // go through all the SpectrumIdentificationResults and build the PSMs
            for (SpectrumIdentificationResult specIdResult
                    : specIDList.getSpectrumIdentificationResult()) {
                String spectrumTitle = null;
                ParamList resultParams = new ParamList();

                for (AbstractParam param : specIdResult.getParamGroup()) {
                    if ((param instanceof CvParam) &&
                            ((CvParam)param).getAccession().equals(OntologyConstants.SPECTRUM_TITLE.getPsiAccession())) {
                        // get the spectrum title, if it is given
                        spectrumTitle = param.getValue();
                    } else {
                        // save all other params and pass them to the PSM later
                        resultParams.getParamGroup().add(param);
                    }
                }


                for (SpectrumIdentificationItem specIdItem
                        : specIdResult.getSpectrumIdentificationItem()) {
                    double deltaMass;
                    if (specIdItem.getCalculatedMassToCharge() != null) {
                        deltaMass = ((specIdItem.getExperimentalMassToCharge() -
                                specIdItem.getCalculatedMassToCharge()) *
                                specIdItem.getChargeState());
                    } else {
                        deltaMass = Double.NaN;
                    }
                    String sequence = null;
                    Peptide pep = null;
                    uk.ac.ebi.jmzidml.model.mzidml.Peptide peptide =
                            peptides.get(specIdItem.getPeptideRef());

                    for (PeptideEvidenceRef pepEvRef
                            : specIdItem.getPeptideEvidenceRef()) {
                        PeptideEvidence pepEvidence = peptideEvidences.get(
                                pepEvRef.getPeptideEvidenceRef());

                        if (pepEvidence == null) {
                            // TODO: better error / exception
                            logger.error("PeptideEvidence " +
                                    pepEvRef.getPeptideEvidenceRef() +
                                    " not found!");
                            return false;
                        }

                        DBSequence dbSeq = dbSequences.get(pepEvidence.getDBSequenceRef());

                        if (dbSeq == null) {
                            // TODO: better error / exception
                            logger.error("DBSequence " +
                                    pepEvidence.getDBSequenceRef() +
                                    " not found!");
                            return false;
                        }

                        Integer start = pepEvidence.getStart();
                        Integer end = pepEvidence.getEnd();
                        String proteinSequence = dbSeq.getSeq();
                        String pepEvSequence = null;

                        if ((start != null) && (end != null)) {
                            if (peptide != null) {
                                pepEvSequence = peptide.getPeptideSequence();
                            } else if (proteinSequence != null) {
                                pepEvSequence = proteinSequence.substring(start-1, end);
                            }

                            if ((proteinSequence != null) && (peptide != null)) {
                                String dbEvSeq = proteinSequence.substring(start-1, end);
                                String pepEvSeq = peptide.getPeptideSequence();

                                if ((dbEvSeq != null) && !dbEvSeq.equals(pepEvSeq)) {
                                    logger.warn("PSM (" + specIdItem.getId() +
                                            ") sequence from " +
                                            "SearchDB differs to sequence from Peptide: " +
                                            dbEvSeq + " != " + pepEvSeq + ". " +
                                            "Sequence from Peptide is used.");
                                }
                            }
                        }

                        if (sequence == null) {
                            sequence = pepEvSequence;
                        } else {
                            if (!sequence.equals(pepEvSequence)) {
                                // TODO: better error / exception
                                logger.error("Different sequences found for a PSM: " +
                                        sequence + " != " + pepEvSequence);
                                return false;
                            }
                        }

                        // add the Accession to the compiler (if it is not already there)
                        FastaHeaderInfos accHeader =
                                FastaHeaderInfos.parseHeaderInfos(dbSeq);

                        Accession acc = compiler.getAccession(accHeader.getAccession());
                        if (acc == null) {
                            acc = compiler.insertNewAccession(
                                    accHeader.getAccession(), proteinSequence);
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
                                logger.warn("Different DBSequences found for same Accession, this is not suported!\n" +
                                        "\t Accession: " + acc.getAccession() +
                                        "\t" + dbSeq.getSeq() + "\n" +
                                        "\t" + acc.getDbSequence());
                            } else if (acc.getDbSequence() == null) {
                                // found a sequence now
                                acc.setDbSequence(proteinSequence);
                            }
                        }

                        if (pep == null) {
                            // add the Peptide to the compiler (if it is not already there)
                            pep = compiler.getPeptide(sequence);
                            if (pep == null) {
                                pep = compiler.insertNewPeptide(sequence);
                                pepNr++;
                            }
                        }

                        // add the accession occurrence to the peptide
                        pep.addAccessionOccurrence(acc, start, end);


                        // now insert the connection between peptide and accession into the compiler
                        compiler.addAccessionPeptideConnection(acc, pep);
                    }


                    // calculate the missed cleavages
                    // TODO: how do multiple and independent enzymes behave???
                    int missed = 0;
                    if (specIDListsEnzymes != null) {
                        for (Enzyme enzyme : specIDListsEnzymes.getEnzyme()) {
                            String regExp = enzyme.getSiteRegexp();

                            ParamList enzymeName = enzyme.getEnzymeName();
                            if ((regExp == null) &&
                                    (enzymeName != null)) {
                                // no siteRegexp given, but enzymeName
                                List<AbstractParam> paramList = enzymeName.getParamGroup();
                                if (paramList.size() > 0) {
                                    if (paramList.get(0) instanceof CvParam) {
                                        String oboID = ((CvParam)(paramList.get(0))).getAccession();
                                        regExp = enzymesToRegexes.get(oboID);
                                    }
                                }
                            }

                            if (regExp == null) {
                                // no regexpt found -> set the missed cleavages to -1, because it is not calculable
                                missed = -1;
                                break;
                            }

                            missed += sequence.split(regExp).length - 1;
                        }
                    }

                    String sourceID = specIdResult.getSpectrumID();
                    Matcher matcher = MzIdentMLTools.patternScanInTitle.matcher(sourceID);
                    if (matcher.matches()) {
                        if (spectrumTitle == null) {
                            spectrumTitle = sourceID;
                        }

                        sourceID = "index=" + matcher.group(1);
                    }

                    // create the PeptideSpectrumMatch object
                    PeptideSpectrumMatch psm;
                    psm = compiler.createNewPeptideSpectrumMatch(specIdItem.getChargeState(),
                            specIdItem.getExperimentalMassToCharge(),
                            deltaMass,
                            null,		// TODO: look for a CV with the RT
                            sequence,
                            missed,
                            sourceID,
                            spectrumTitle,
                            file,
                            spectrumID);
                    pep.addSpectrum(psm);
                    specNr++;

                    // get the scores and add them to the PSM
                    for (CvParam cvParam : specIdItem.getCvParam()) {
                        String cvAccession = cvParam.getAccession();

                        Term oboTerm = compiler.getOBOMapper().getTerm(cvAccession);
                        boolean isScore = false;

                        if (oboTerm != null) {
                            // the score is in the OBO file, get the relations etc.
                            Set<Triple> tripleSet = compiler.getOBOMapper().getTriples(oboTerm, null, null);

                            for (Triple triple : tripleSet) {
                                if (triple.getPredicate().getName().equals(OBOMapper.obo_is_a) &&
                                        triple.getObject().getName().equals(OntologyConstants.SEARCH_ENGINE_PSM_SCORE.getPsiAccession())) {
                                    // subject is a "search engine specific score for PSM"
                                    isScore = true;
                                    ScoreModel score;
                                    double value = Double.parseDouble(cvParam.getValue());
                                    score = new ScoreModel(value, cvAccession,
                                            cvParam.getName());

                                    psm.addScore(score);
                                }
                            }
                        }

                        if (!isScore) {
                            // add the cvParam to the params of the PSM
                            psm.addParam(cvParam);
                        }
                    }

                    // add the userParam to the params of the PSM
                    specIdItem.getUserParam().forEach(psm::addParam);

                    // add the params from the specIdResult to the PSM
                    resultParams.getParamGroup().forEach(psm::addParam);

                    // adding the modifications
                    // the modifications are in SequenceCollection:Peptide
                    if (peptide != null) {

                        for (Modification mod : peptide.getModification()) {

                            if (mod.getLocation() == null) {
                                logger.warn("Cannot build modification without location, skipping.");
                            }

                            de.mpc.pia.intermediate.Modification modification;

                            Character residue;
                            String description = null;
                            String accession = null;
                            Double massDelta = (mod.getMonoisotopicMassDelta() != null) ?
                                    mod.getMonoisotopicMassDelta() : null;


                            if ((mod.getLocation() == 0) ||
                                    (mod.getLocation() > sequence.length())) {
                                residue = '.';
                            } else {
                                residue = sequence.charAt(mod.getLocation() - 1);
                            }

                            for (CvParam param : mod.getCvParam()) {
                                // get the cvParam, which maps to UNIMOD, this is the description
                                if (param.getCvRef().equals("UNIMOD")) {
                                    description = param.getName();
                                    accession = param.getAccession();
                                    break;
                                }
                            }

                            modification =
                                    new de.mpc.pia.intermediate.Modification(
                                            residue, massDelta, description,
                                            accession);

                            psm.addModification(mod.getLocation(), modification);
                        }

                    } else {
                        logger.warn("no peptide for the peptide_ref " +
                                specIdItem.getPeptideRef() +
                                " in the SequenceCollection -> can't get Modifications for it.");
                    }

                    // the PSM is finished here
                    compiler.insertCompletePeptideSpectrumMatch(psm);
                }
            }
        }

        logger.info("inserted new: \n\t" +
                pepNr + " peptides\n\t" +
                specNr + " peptide spectrum matches\n\t" +
                accNr + " accessions");
        return true;
    }

}
