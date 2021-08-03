package de.mpc.pia.intermediate.xmlhandler;

import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Logger;

import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftwareList;
import uk.ac.ebi.jmzidml.model.mzidml.Inputs;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabase;
import uk.ac.ebi.jmzidml.model.mzidml.SpectraData;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Group;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.intermediate.piaxml.AccessionRefXML;
import de.mpc.pia.intermediate.piaxml.AccessionXML;
import de.mpc.pia.intermediate.piaxml.AccessionsListXML;
import de.mpc.pia.intermediate.piaxml.ChildRefXML;
import de.mpc.pia.intermediate.piaxml.DescriptionXML;
import de.mpc.pia.intermediate.piaxml.FileRefXML;
import de.mpc.pia.intermediate.piaxml.FilesListXML;
import de.mpc.pia.intermediate.piaxml.GroupXML;
import de.mpc.pia.intermediate.piaxml.GroupsListXML;
import de.mpc.pia.intermediate.piaxml.ModificationXML;
import de.mpc.pia.intermediate.piaxml.OccurenceXML;
import de.mpc.pia.intermediate.piaxml.PIAInputFileXML;
import de.mpc.pia.intermediate.piaxml.PeptideRefXML;
import de.mpc.pia.intermediate.piaxml.PeptideXML;
import de.mpc.pia.intermediate.piaxml.PeptidesListXML;
import de.mpc.pia.intermediate.piaxml.ScoreXML;
import de.mpc.pia.intermediate.piaxml.SearchDatabaseRefXML;
import de.mpc.pia.intermediate.piaxml.SpectraListXML;
import de.mpc.pia.intermediate.piaxml.SpectrumMatchXML;
import de.mpc.pia.intermediate.piaxml.SpectrumRefXML;
import de.mpc.pia.modeller.IdentificationKeySettings;
import de.mpc.pia.modeller.score.ScoreModel;


public class PIAIntermediateJAXBHandler implements Serializable {

    private static final long serialVersionUID = -8416334186918951733L;


    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(PIAIntermediateJAXBHandler.class);


    /** the name of the project */
    private String projectName;

    /** the input files */
    private Map<Long, PIAInputFile> files;

    /** the SpectraData (like in mzIdentML) */
    private Map<String, SpectraData> spectraData;

    /** the SearchDatabases (like in mzIdentML) */
    private Map<String, SearchDatabase> searchDatabases;

    /** the analysis software for identifications (class from mzIdentML) */
    private Map<String, AnalysisSoftware> software;

    /** the PSMs */
    private Map<Long, PeptideSpectrumMatch> psms;

    /** the peptides */
    private Map<Long, Peptide> peptides;

    /** the accessions */
    private Map<Long, Accession> accessions;

    /** the groups */
    private Map<Long, Group> groups;

    /** Maps from the name of an {@link IdentificationKeySettings} to a set, containg the file IDs, which have warnings for this setting */
    private Map<String, Set<Long>> psmSetSettingsWarnings;


    // XML file tag statics for parsing
    private static final String XML_TAG_FILES_LIST = "filesList";
    private static final String XML_TAG_INPUTS = "Inputs";
    private static final String XML_TAG_ANALYSIS_SOFTWARE_LIST = "AnalysisSoftwareList";
    private static final String XML_TAG_SPECTRA_LIST = "spectraList";
    private static final String XML_TAG_ACCESSIONS_LIST = "accessionsList";
    private static final String XML_TAG_PEPTIDES_LIST = "peptidesList";
    private static final String XML_TAG_GROUPS_LIST = "groupsList";



    /**
     * Basic constructor, initializing all the Maps.
     */
    public PIAIntermediateJAXBHandler() {
        projectName = null;
        files = new HashMap<>();
        spectraData = new HashMap<>();
        searchDatabases = new HashMap<>();
        software = new HashMap<>();
        psms = new HashMap<>();
        peptides = new HashMap<>();
        accessions = new HashMap<>();
        groups = new HashMap<>();
        psmSetSettingsWarnings =
                new HashMap<>(IdentificationKeySettings.values().length);
        for (IdentificationKeySettings setting : IdentificationKeySettings.values()) {
            psmSetSettingsWarnings.put(setting.toString(), new HashSet<>());
        }
    }


    /**
     * Parses the file in chunks and thus having a low memory footprint.<br/>
     *
     * @param fileName
     * @param progressArr stores the current progress of the parsing, gets increased by 40 by this method (remaining 60 are in the PIAModeller)
     *
     * @throws IOException
     */
    public void parse(String fileName, Long[] progressArr)
            throws IOException {
        Long[] progress = progressArr;
        projectName = null;
        files = new HashMap<>();
        spectraData = new HashMap<>();
        searchDatabases = new HashMap<>();
        software = new HashMap<>();
        psms = new HashMap<>();
        peptides = new HashMap<>();
        accessions = new HashMap<>();
        groups = new HashMap<>();

        if ((progress == null) || (progressArr.length < 1) || (progressArr[0] == null)) {
            LOGGER.warn("No progress array given, creating one. "
                    + "But no external supervision will be possible.");
            progress = new Long[1];
            progress[0] = 0L;
        }

        parseXMLFile(fileName, progress);

        // the source ID and spectrum title needs to be updated -> deactivate for now
        // TODO: review the source ID and use it constantly as in the mzIdentML document, converting everything to index=XXX does not work!
        psmSetSettingsWarnings.get(IdentificationKeySettings.SOURCE_ID.toString()).add(0L);
        psmSetSettingsWarnings.get(IdentificationKeySettings.SPECTRUM_TITLE.toString()).add(0L);
    }



    /**
     * Actually parses the XML file given by fileName.
     *
     * @param fileName
     * @param progress stores the current progress of the parsing
     * @throws IOException
     */
    private void parseXMLFile(String fileName, Long[] progress)
            throws IOException {
        // set up a StAX reader
        XMLInputFactory xmlif = XMLInputFactory.newInstance();
        xmlif.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        try (FileReader fileReader = new FileReader(fileName)) {
            XMLStreamReader xmlr = xmlif.createXMLStreamReader(fileReader);

            // move to the root element and check its name.
            xmlr.nextTag();
            xmlr.require(XMLStreamConstants.START_ELEMENT, null, "jPiaXML");

            // get project attributes
            for (int attrIdx=0; attrIdx < xmlr.getAttributeCount(); attrIdx++) {
                if ("name".equals(xmlr.getAttributeName(attrIdx).toString())) {
                    projectName = xmlr.getAttributeValue(attrIdx);
                }
            }

            // move to the first not-root element
            xmlr.nextTag();
            while (xmlr.hasNext()) {
                String tag = xmlr.getLocalName();

                Long tagProgress = parseTag(tag, xmlr);

                if (tagProgress > 0) {
                    progress[0] += tagProgress;
                }

                skipWhitespacesInReader(xmlr);

                // check, if end of file reached
                if ("jPiaXML".equalsIgnoreCase(xmlr.getLocalName())
                        && (xmlr.getEventType() == XMLStreamConstants.END_ELEMENT)) {
                    // finished, reached the closing tag of jPiaXML
                    break;
                }
            }
        } catch (IOException | XMLStreamException | JAXBException e) {
            LOGGER.error("Error while parsing PIA XML file", e);
            throw new IOException(e);
        }
    }


    /**
     * Skips the whitespaces between tags
     *
     * @param xmlr
     * @throws XMLStreamException
     */
    private static void skipWhitespacesInReader(XMLStreamReader xmlr) throws XMLStreamException {
        // skip the whitespace between tags
        if (xmlr.getEventType() == XMLStreamConstants.CHARACTERS) {
            xmlr.next();
        }
    }


    /**
     * Parses the currently by the String given in the {@link XMLStreamReader}.
     *
     * @param tag
     * @param xmlr
     * @return
     * @throws JAXBException
     * @throws XMLStreamException
     */
    private Long parseTag(String tag, XMLStreamReader xmlr)
            throws JAXBException, XMLStreamException {
        long progress = 0L;

        if (XML_TAG_FILES_LIST.equalsIgnoreCase(tag)) {
            LOGGER.info(tag);
            // filesList
            parseFilesList(xmlr);
            progress += 1;
        } else if (XML_TAG_INPUTS.equalsIgnoreCase(tag)) {
            LOGGER.info(tag);
            // Inputs
            parseInputs(xmlr);
            progress += 1;
        } else if (XML_TAG_ANALYSIS_SOFTWARE_LIST.equalsIgnoreCase(tag)) {
            LOGGER.info(tag);
            // AnalysisSoftwareList
            parseAnalysisSoftwareList(xmlr);
            progress += 1;
        } else if (XML_TAG_SPECTRA_LIST.equalsIgnoreCase(tag)) {
            LOGGER.info(tag);
            parseSpectraChunked(xmlr);
            progress += 30;
        } else if (XML_TAG_ACCESSIONS_LIST.equalsIgnoreCase(tag)) {
            LOGGER.info(tag);
            parseAccessionsChunked(xmlr);
            progress += 1;
        } else if (XML_TAG_PEPTIDES_LIST.equalsIgnoreCase(tag)) {
            LOGGER.info(tag);
            parsePeptidesChunked(xmlr);
            progress += 5;
        } else if (XML_TAG_GROUPS_LIST.equalsIgnoreCase(tag)) {
            LOGGER.info(tag);
            parseGroupsChunked(xmlr);
            progress += 1;
        } else {
            LOGGER.warn("unknown tag in pia XML: " + xmlr.getLocalName());
        }

        return progress;
    }



    /**
     * Parses the filesList, given the XMLStreamReader at its starting point.
     *
     * @param xmlr
     * @throws JAXBException
     */
    private void parseFilesList(XMLStreamReader xmlr) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(FilesListXML.class);
        Unmarshaller um = jaxbContext.createUnmarshaller();
        FilesListXML filesListXML = (FilesListXML)um.unmarshal(xmlr);

        if (filesListXML == null) {
            return;
        }

        for (PIAInputFileXML fileXML : filesListXML.getFiles()) {
            PIAInputFile file = new PIAInputFile(fileXML.getId(),
                    fileXML.getName(), fileXML.getFileName(),
                    fileXML.getFormat());

            if (fileXML.getAnalysisCollection() != null) {
                fileXML.getAnalysisCollection().getSpectrumIdentification().forEach(file::addSpectrumIdentification);
            }

            if (fileXML.getAnalysisProtocolCollection() != null) {
                fileXML.getAnalysisProtocolCollection().getSpectrumIdentificationProtocol().forEach(file::addSpectrumIdentificationProtocol);
            }

            files.put(file.getID(), file);
        }
    }


    /**
     * Parses the Inputs, given the XMLStreamReader at its starting point.
     *
     * @param xmlr
     * @throws JAXBException
     */
    private void parseInputs(XMLStreamReader xmlr) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Inputs.class);
        Unmarshaller um = jaxbContext.createUnmarshaller();
        JAXBElement<Inputs> umRoot = um.unmarshal(xmlr, Inputs.class);
        Inputs inputs = umRoot.getValue();

        if (inputs != null) {
            // Inputs:SpectraData
            for (SpectraData sd : inputs.getSpectraData()) {
                spectraData.put(sd.getId(), sd);
            }

            // Inputs:SearchDatabase
            for (SearchDatabase db : inputs.getSearchDatabase()) {
                searchDatabases.put(db.getId(), db);
            }
        }
    }


    /**
     * Parses the AnalysisSoftwareList, given the XMLStreamReader at its
     * starting point.
     *
     * @param xmlr
     * @throws JAXBException
     */
    private void parseAnalysisSoftwareList(XMLStreamReader xmlr) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(AnalysisSoftwareList.class);
        Unmarshaller um = jaxbContext.createUnmarshaller();
        JAXBElement<AnalysisSoftwareList> umRoot = um.unmarshal(xmlr, AnalysisSoftwareList.class);
        AnalysisSoftwareList analysisSoftwareList = umRoot.getValue();

        if (analysisSoftwareList != null) {
            for (AnalysisSoftware sw : analysisSoftwareList.getAnalysisSoftware()) {
                software.put(sw.getId(), sw);
            }
        }
    }


    /**
     * Parses the spectra in a chunked matter. It assumes, the given
     * {@link XMLStreamReader} is at the position of a {@link SpectraListXML}.
     *
     * @param xmlr
     * @throws XMLStreamException
     * @throws JAXBException
     */
    private void parseSpectraChunked(XMLStreamReader xmlr)
            throws XMLStreamException, JAXBException {
        xmlr.require(XMLStreamConstants.START_ELEMENT, null, XML_TAG_SPECTRA_LIST);

        JAXBContext jaxbContext = JAXBContext.newInstance(SpectrumMatchXML.class);
        Unmarshaller um = jaxbContext.createUnmarshaller();

        psmSetSettingsWarnings =
                new HashMap<>(IdentificationKeySettings.values().length);
        for (IdentificationKeySettings setting : IdentificationKeySettings.values()) {
            psmSetSettingsWarnings.put(setting.toString(), new HashSet<>());
        }

        // move to the first spectrumMatch element
        xmlr.nextTag();
        while (xmlr.getEventType() == XMLStreamConstants.START_ELEMENT) {
            xmlr.require(XMLStreamConstants.START_ELEMENT, null, "spectrumMatch");

            SpectrumMatchXML psmXML = (SpectrumMatchXML) um.unmarshal(xmlr);
            PeptideSpectrumMatch psm = createPSMfromXML(psmXML);

            // put the PSM into the map
            psms.put(psm.getID(), psm);

            skipWhitespacesInReader(xmlr);
        }

        xmlr.require(XMLStreamConstants.END_ELEMENT, null, XML_TAG_SPECTRA_LIST);
        if (xmlr.hasNext()) {
            xmlr.nextTag();
        }
    }


    /**
     * Create a {@link PeptideSpectrumMatch} from the {@link SpectrumMatchXML}
     * object.
     *
     * @param psmXML
     * @return
     */
    private PeptideSpectrumMatch createPSMfromXML(SpectrumMatchXML psmXML) {
        PeptideSpectrumMatch psm;

        PIAInputFile file = files.get(psmXML.getFileRef());
        SpectrumIdentification spectrumID = null;

        if (file != null) {
            if (psmXML.getSpectrumIdentificationRef() != null) {
                spectrumID = file.getSpectrumIdentification(
                        psmXML.getSpectrumIdentificationRef());

                if (spectrumID == null) {
                    LOGGER.warn("No SpectrumIdentification found for '" +
                            psmXML.getSpectrumIdentificationRef() + '\'');
                }
            }

        } else {
            LOGGER.warn("PSM '" + psmXML.getId() + "' has no valid fileRef '" +
                    psmXML.getFileRef() + "'.");
        }

        psm = new PeptideSpectrumMatch(psmXML.getId(),
                psmXML.getCharge(),
                psmXML.getMassToCharge(),
                psmXML.getDeltaMass(),
                psmXML.getRetentionTime(),
                psmXML.getSequence(),
                psmXML.getMissed(),
                psmXML.getSourceID(),
                psmXML.getTitle(),
                files.get(psmXML.getFileRef()),
                spectrumID,
                null
                );

        // add the scores
        addPSMScoresFromXML(psm, psmXML);

        // add the modifications
        addPSMModificationsFromXML(psm, psmXML);

        // if the PSM has decoy information, set it
        if (psmXML.getIsDecoy() != null) {
            psm.setIsDecoy(psmXML.getIsDecoy());
        }

        // if the PSM has uniqueness information, set it
        if (psmXML.getIsUnique() != null) {
            psm.setIsUnique(psmXML.getIsUnique());
        }

        // the params
        psmXML.getParamList().forEach(psm::addParam);

        // check for PSM set settings warnings
        updatePSMSetSettingsWarnings(psm);

        return psm;
    }


    /**
     * Adds the scores from the {@link SpectrumMatchXML} to the PSM.
     *
     * @param psm
     * @param psmXML
     */
    private static void addPSMScoresFromXML(PeptideSpectrumMatch psm, SpectrumMatchXML psmXML) {
        for (ScoreXML scoreXML : psmXML.getScores()) {
            ScoreModel score = new ScoreModel(scoreXML.getValue(),
                    scoreXML.getCvAccession(),
                    scoreXML.getName());

            psm.addScore(score);
        }
    }


    /**
     * Adds the modifications from the {@link SpectrumMatchXML} to the PSM.
     *
     * @param psm
     * @param psmXML
     */
    private static void addPSMModificationsFromXML(PeptideSpectrumMatch psm, SpectrumMatchXML psmXML) {
        for (ModificationXML modXML : psmXML.getModification()) {
            Modification mod;
            List<ScoreModel> scoreModels = new ArrayList<>();
            if(modXML.getProbability() != null){
                for(ScoreXML oldScoreXML: modXML.getProbability()){
                    ScoreModel scoreModel = new ScoreModel(oldScoreXML.getValue(),
                            oldScoreXML.getCvAccession(), oldScoreXML.getName(), oldScoreXML.getCvLabel());
                    scoreModels.add(scoreModel);
                }
            }
            mod = new Modification(
                    modXML.getResidue().charAt(0),
                    modXML.getMass(),
                    modXML.getDescription(),
                    modXML.getAccession(),
                    modXML.getCvLabel(), scoreModels
                    );

            psm.addModification(modXML.getLocation(), mod);
        }
    }

    /**
     * Update the warnings with information of the new PSM.
     *
     * @param psm
     */
    private void updatePSMSetSettingsWarnings(PeptideSpectrumMatch psm) {
        if (psm.getRetentionTime() == null) {
            psmSetSettingsWarnings.get(IdentificationKeySettings.RETENTION_TIME.toString())
                    .add(psm.getFile().getID());
        }
        if ((psm.getSourceID() == null) || psm.getSourceID().trim().isEmpty()) {
            psmSetSettingsWarnings.get(IdentificationKeySettings.SOURCE_ID.toString())
                    .add(psm.getFile().getID());
        }
        if ((psm.getSpectrumTitle() == null) || psm.getSpectrumTitle().trim().isEmpty()) {
            psmSetSettingsWarnings.get(IdentificationKeySettings.SPECTRUM_TITLE.toString())
                    .add(psm.getFile().getID());
        }
        if ((psm.getSequence() == null) || psm.getSequence().trim().isEmpty()) {
            psmSetSettingsWarnings.get(IdentificationKeySettings.SEQUENCE.toString())
                    .add(psm.getFile().getID());
        }
    }


    /**
     * Parses the accessions in a chunked matter. It assumes, the given
     * {@link XMLStreamReader} is at the position of an
     * {@link AccessionsListXML}.
     *
     * @param xmlr
     * @throws XMLStreamException
     * @throws JAXBException
     */
    private void parseAccessionsChunked(XMLStreamReader xmlr)
            throws XMLStreamException, JAXBException {
        xmlr.require(XMLStreamConstants.START_ELEMENT, null, XML_TAG_ACCESSIONS_LIST);

        JAXBContext jaxbContext = JAXBContext.newInstance(AccessionXML.class);
        Unmarshaller um = jaxbContext.createUnmarshaller();

        // move to the first accession element
        xmlr.nextTag();
        while (xmlr.getEventType() == XMLStreamConstants.START_ELEMENT) {
            xmlr.require(XMLStreamConstants.START_ELEMENT, null, "accession");

            AccessionXML accXML = (AccessionXML) um.unmarshal(xmlr);
            Accession accession;
            Map<Long, String> descriptions = new HashMap<>();

            Set<Long> filesSet = accXML.getFileRefs().stream().map(FileRefXML::getFile_ref).collect(Collectors.toSet());

            for (DescriptionXML descXML : accXML.getDescriptions()) {
                descriptions.put(descXML.getFileRefID(), descXML.getValue());
            }

            Set<String> searchDatabaseRefs = accXML.getSearchDatabaseRefs().stream().map(SearchDatabaseRefXML::getSearchDatabase_ref).collect(Collectors.toSet());

            accession = new Accession(accXML.getId(),
                    accXML.getAcc(),
                    filesSet,
                    descriptions,
                    accXML.getSequence(),
                    searchDatabaseRefs,
                    null);      // group = null, is set later with the groups

            accessions.put(accession.getID(), accession);

            skipWhitespacesInReader(xmlr);
        }

        xmlr.require(XMLStreamConstants.END_ELEMENT, null, XML_TAG_ACCESSIONS_LIST);
        if (xmlr.hasNext()) {
            xmlr.nextTag();
        }
    }


    /**
     * Parses the peptides in a chunked matter. It assumes, the given
     * {@link XMLStreamReader} is at the position of a {@link PeptidesListXML}.
     *
     * @param xmlr
     * @throws XMLStreamException
     * @throws JAXBException
     */
    private void parsePeptidesChunked(XMLStreamReader xmlr)
            throws XMLStreamException, JAXBException {
        xmlr.require(XMLStreamConstants.START_ELEMENT, null, XML_TAG_PEPTIDES_LIST);

        JAXBContext jaxbContext = JAXBContext.newInstance(PeptideXML.class);
        Unmarshaller um = jaxbContext.createUnmarshaller();

        // move to the first peptide element
        xmlr.nextTag();
        while (xmlr.getEventType() == XMLStreamConstants.START_ELEMENT) {
            xmlr.require(XMLStreamConstants.START_ELEMENT, null, "peptide");

            PeptideXML pepXML = (PeptideXML) um.unmarshal(xmlr);
            Peptide peptide;
            List<PeptideSpectrumMatch> psmList = new ArrayList<>();

            peptide = new Peptide(pepXML.getId(), pepXML.getSequence());

            for (SpectrumRefXML spectrumRefXML
                    : pepXML.getSpectrumRefList().getSpectrumRefs()) {
                PeptideSpectrumMatch psm = psms.get(spectrumRefXML.getSpectrumRefID());

                if (psm != null) {
                    psmList.add(psm);
                    // backlink the peptide in the PSM
                    psm.setPeptide(peptide);
                } else {
                    LOGGER.warn("No spectrumMatch found for '" +
                            spectrumRefXML.getSpectrumRefID() + '\'');
                }
            }
            peptide.setSpectra(psmList);

            for (OccurenceXML occXML : pepXML.getOccurrences().getOccurrences()) {
                Accession acc = accessions.get(occXML.getAccessionRefID());

                if (acc != null) {
                    peptide.addAccessionOccurrence(acc, occXML.getStart(),
                            occXML.getEnd());
                } else {
                    LOGGER.warn("No accession found for occurrence '" +
                            occXML.getAccessionRefID() + '\'');
                }
            }

            peptides.put(peptide.getID(), peptide);

            skipWhitespacesInReader(xmlr);
        }

        xmlr.require(XMLStreamConstants.END_ELEMENT, null, XML_TAG_PEPTIDES_LIST);
        if (xmlr.hasNext()) {
            xmlr.nextTag();
        }
    }


    /**
     * Parses the groups in a chunked matter. It assumes, the given
     * {@link XMLStreamReader} is at the position of a {@link GroupsListXML}.
     *
     * @param xmlr
     * @throws XMLStreamException
     * @throws JAXBException
     */
    private void parseGroupsChunked(XMLStreamReader xmlr)
            throws XMLStreamException, JAXBException {
        Map<Long, List<ChildRefXML>> groupsChildren = new HashMap<>();

        xmlr.require(XMLStreamConstants.START_ELEMENT, null, XML_TAG_GROUPS_LIST);

        JAXBContext jaxbContext = JAXBContext.newInstance(GroupXML.class);
        Unmarshaller um = jaxbContext.createUnmarshaller();

        // move to the first peptide element
        xmlr.nextTag();
        while (xmlr.getEventType() == XMLStreamConstants.START_ELEMENT) {
            xmlr.require(XMLStreamConstants.START_ELEMENT, null, "group");

            GroupXML groupXML = (GroupXML) um.unmarshal(xmlr);
            Group group = new Group(groupXML.getId());

            group.setTreeID(groupXML.getTreeId());

            parseGroupsAccessions(groupXML, group);
            parseGroupsPeptides(groupXML, group);

            // to get the "allAccessions" right, children are set in a second round
            if (groupXML.getChildrenRefList() != null) {
                groupsChildren.put(group.getID(), groupXML.getChildrenRefList());
            }

            groups.put(group.getID(), group);

            skipWhitespacesInReader(xmlr);
        }

        xmlr.require(XMLStreamConstants.END_ELEMENT, null, XML_TAG_GROUPS_LIST);
        if (xmlr.hasNext()) {
            xmlr.nextTag();
        }

        // now set the groups' connections
        for (Map.Entry<Long, List<ChildRefXML>> groupChildIt
                : groupsChildren.entrySet()) {
            Group group = groups.get(groupChildIt.getKey());

            for (ChildRefXML childRef : groupChildIt.getValue()) {
                Group child = groups.get(childRef.getChildRefID());

                if (child != null) {
                    group.addChild(child);
                    child.addParent(group);
                } else {
                    LOGGER.warn("No group found for child reference '" + childRef.getChildRefID() + '\'');
                }
            }
        }
    }


    /**
     * Parses the Accessions from the {@link GroupXML} to the {@link Group}.
     *
     * @param groupXML
     * @param group
     */
    private void parseGroupsAccessions(GroupXML groupXML, Group group) {
        for (AccessionRefXML accRef : groupXML.getAccessionsRefList()) {
            Accession accession = accessions.get(accRef.getAccRefID());

            if (accession != null) {
                group.addAccession(accession);
                // now the accession's group can be set
                accession.setGroup(group);
            } else {
                LOGGER.warn("No accession found for groups reference '" +
                        accRef.getAccRefID() + '\'');
            }
        }
    }


    /**
     * Parses the Peptides from the {@link GroupXML} to the {@link Group}.
     *
     * @param groupXML
     * @param group
     */
    private void parseGroupsPeptides(GroupXML groupXML, Group group) {
        for (PeptideRefXML pepRef : groupXML.getPeptidesRefList()) {
            Peptide peptide = peptides.get(pepRef.getPepRefID());

            if (peptide != null) {
                group.addPeptide(peptide);
                // now the peptide's group can be set
                peptide.setGroup(group);
            } else {
                LOGGER.warn("No peptide found for groups reference '" +
                        pepRef.getPepRefID() + '\'');
            }
        }
    }


    public String getProjectName() {
        return projectName;
    }


    public Map<Long, PIAInputFile> getFiles() {
        return files;
    }


    public Map<String, SpectraData> getSpectraData() {
        return spectraData;
    }


    public Map<String, SearchDatabase> getSearchDatabase() {
        return searchDatabases;
    }


    public Map<String, AnalysisSoftware> getAnalysisSoftware() {
        return software;
    }


    public Map<Long, PeptideSpectrumMatch> getPSMs() {
        return psms;
    }


    public Map<Long, Peptide> getPeptides() {
        return peptides;
    }


    public Map<Long, Accession> getAccessions() {
        return accessions;
    }


    public Map<Long, Group> getGroups() {
        return groups;
    }


    public long getNrTrees() {
        long maxTreeID = 0;

        for (Map.Entry<Long, Group> grIt : groups.entrySet()) {
            if (grIt.getValue().getTreeID() > maxTreeID) {
                maxTreeID = grIt.getValue().getTreeID();
            }
        }

        return maxTreeID;
    }


    public Map<String, Set<Long>> getPSMSetSettingsWarnings() {
        return psmSetSettingsWarnings;
    }
}
