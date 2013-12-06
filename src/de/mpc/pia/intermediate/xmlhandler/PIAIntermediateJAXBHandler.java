package de.mpc.pia.intermediate.xmlhandler;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Logger;

import uk.ac.ebi.jmzidml.model.mzidml.AbstractParam;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftwareList;
import uk.ac.ebi.jmzidml.model.mzidml.Inputs;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabase;
import uk.ac.ebi.jmzidml.model.mzidml.SpectraData;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationProtocol;

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


public class PIAIntermediateJAXBHandler {
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
	
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(PIAIntermediateJAXBHandler.class);
	
	
	/**
	 * Basic constructor, initializing all the Maps.
	 * @param fileName
	 */
	public PIAIntermediateJAXBHandler() {
		projectName = null;
		files = new HashMap<Long, PIAInputFile>();
		spectraData = new HashMap<String, SpectraData>();
		searchDatabases = new HashMap<String, SearchDatabase>();
		software = new HashMap<String, AnalysisSoftware>();
		psms = new HashMap<Long, PeptideSpectrumMatch>();
		peptides = new HashMap<Long, Peptide>();
		accessions = new HashMap<Long, Accession>();
		groups = new HashMap<Long, Group>();
		psmSetSettingsWarnings =
				new HashMap<String, Set<Long>>(IdentificationKeySettings.values().length);
		for (IdentificationKeySettings setting : IdentificationKeySettings.values()) {
			psmSetSettingsWarnings.put(setting.toString(), new HashSet<Long>());
		}
	}
	
	
	/**
	 * Parses the file in chunks and thus having a low memory footprint.<br/>
	 * The progress gets increased by 40 (remaining 60 are in the PIAModeller).
	 * 
	 * @param fileName
	 * @throws XMLStreamException 
	 * @throws FileNotFoundException 
	 * @throws JAXBException 
	 */
	public void parse(String fileName, Long[] progress)
			throws FileNotFoundException, XMLStreamException, JAXBException {
		projectName = null;
		files = new HashMap<Long, PIAInputFile>();
		spectraData = new HashMap<String, SpectraData>();
		searchDatabases = new HashMap<String, SearchDatabase>();
		software = new HashMap<String, AnalysisSoftware>();
		psms = new HashMap<Long, PeptideSpectrumMatch>();
		peptides = new HashMap<Long, Peptide>();
		accessions = new HashMap<Long, Accession>();
		groups = new HashMap<Long, Group>();
		
		if (progress == null) {
			logger.warn("no progress array given, creating one. But no external" +
					"supervision possible");
			progress = new Long[1];
		}
		
		// set up a StAX reader  
		XMLInputFactory xmlif = XMLInputFactory.newInstance();
		XMLStreamReader xmlr =
				xmlif.createXMLStreamReader(new FileReader(fileName));
		
		// move to the root element and check its name.  
		xmlr.nextTag();  
		xmlr.require(XMLStreamConstants.START_ELEMENT, null, "jPiaXML");
		
		// get project attributes
		for (int attrIdx=0; attrIdx < xmlr.getAttributeCount(); attrIdx++) {
			if (xmlr.getAttributeName(attrIdx).toString().equals("name")) {
				projectName = xmlr.getAttributeValue(attrIdx);
			}
		}
		
		// move to the first not-root element
		xmlr.nextTag();
		while (xmlr.hasNext()) {
			String tag = xmlr.getLocalName();
			
			if (tag.equalsIgnoreCase("filesList")) {
				logger.info(tag);
				// filesList
				JAXBContext jaxbContext = JAXBContext.newInstance(FilesListXML.class);
				Unmarshaller um = jaxbContext.createUnmarshaller();
				FilesListXML filesList = (FilesListXML)um.unmarshal(xmlr);
				
				if (filesList != null) {
					parseFilesList(filesList);
				}
				progress[0] += 1;
			} else if (tag.equalsIgnoreCase("Inputs")) {
				logger.info(tag);
				// Inputs
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
				progress[0] += 1;
			} else if (tag.equalsIgnoreCase("AnalysisSoftwareList")) {
				logger.info(tag);
				// AnalysisSoftwareList
				JAXBContext jaxbContext = JAXBContext.newInstance(AnalysisSoftwareList.class);
				Unmarshaller um = jaxbContext.createUnmarshaller();
				JAXBElement<AnalysisSoftwareList> umRoot = um.unmarshal(xmlr, AnalysisSoftwareList.class);
				AnalysisSoftwareList analysisSoftwareList = umRoot.getValue();
				
				if (analysisSoftwareList != null) {
					for (AnalysisSoftware sw : analysisSoftwareList.getAnalysisSoftware()) {
						software.put(sw.getId(), sw);
					}
				}
				progress[0] += 1;
			} else if (tag.equalsIgnoreCase("spectraList")) {
				logger.info(tag);
				parseSpectraChunked(xmlr);
				progress[0] += 30;
			} else if (tag.equalsIgnoreCase("accessionsList")) {
				logger.info(tag);
				parseAccessionsChunked(xmlr);
				progress[0] += 1;
			} else if (tag.equalsIgnoreCase("peptidesList")) {
				logger.info(tag);
				parsePeptidesChunked(xmlr);
				progress[0] += 5;
			} else if (tag.equalsIgnoreCase("groupsList")) {
				logger.info(tag);
				parseGroupsChunked(xmlr);
				progress[0] += 1;
			} else {
				logger.warn("unknown tag in piaXML: " + xmlr.getLocalName());
			}
			
			// skip the whitespace between tags
			if (xmlr.getEventType() == XMLStreamConstants.CHARACTERS) {
				xmlr.next();
			}
			
			// check, if end of file reached
			if (xmlr.getLocalName().equalsIgnoreCase("jPiaXML") && 
					(xmlr.getEventType() == XMLStreamConstants.END_ELEMENT)) {
				// finished, reached the closing tag of jPiaXML
				break;
			}
		}
	}
	
	
	/**
	 * Parses the files list, given the XML object
	 * 
	 * @param filesListXML
	 */
	public void parseFilesList(FilesListXML filesListXML) {
		for (PIAInputFileXML fileXML : filesListXML.getFiles()) {
			PIAInputFile file = new PIAInputFile(fileXML.getId(),
					fileXML.getName(), fileXML.getFileName(),
					fileXML.getFormat());
			
			if (fileXML.getAnalysisCollection() != null) {
				for (SpectrumIdentification si
						: fileXML.getAnalysisCollection().getSpectrumIdentification()) {
					file.addSpectrumIdentification(si);
				}
			}
			
			if (fileXML.getAnalysisProtocolCollection() != null) {
				for (SpectrumIdentificationProtocol sip
						: fileXML.getAnalysisProtocolCollection().getSpectrumIdentificationProtocol()) {
					file.addSpectrumIdentificationProtocol(sip);
				}
			}
			
			files.put(file.getID(), file);
		}
	}
	
	
	/**
	 * Parses the spectra in a chunked matter. It assumes, the given
	 * {@link XMLStreamReader} is at the position of a {@link SpectraListXML}.
	 * 
	 * @param xmlr
	 * @throws JAXBException 
	 * @throws XMLStreamException 
	 */
	public void parseSpectraChunked(XMLStreamReader xmlr)
			throws JAXBException, XMLStreamException {
		xmlr.require(XMLStreamConstants.START_ELEMENT, null, "spectraList");
		
		JAXBContext jaxbContext = JAXBContext.newInstance(SpectrumMatchXML.class);
		Unmarshaller um = jaxbContext.createUnmarshaller();
		
		psmSetSettingsWarnings =
				new HashMap<String, Set<Long>>(IdentificationKeySettings.values().length);
		for (IdentificationKeySettings setting : IdentificationKeySettings.values()) {
			psmSetSettingsWarnings.put(setting.toString(), new HashSet<Long>());
		}
		
		// move to the first spectrumMatch element
		xmlr.nextTag();
		while (xmlr.getEventType() == XMLStreamConstants.START_ELEMENT) {
			xmlr.require(XMLStreamConstants.START_ELEMENT, null, "spectrumMatch");
			
			SpectrumMatchXML psmXML = (SpectrumMatchXML) um.unmarshal(xmlr);
			PeptideSpectrumMatch psm;
			
			PIAInputFile file = files.get(psmXML.getFileRef());
			SpectrumIdentification spectrumID = null;
			
			if (file != null) {
				
				if (psmXML.getSpectrumIdentificationRef() != null) {
					spectrumID = file.getSpectrumIdentification(
							psmXML.getSpectrumIdentificationRef());
					
					if (spectrumID == null) {
						logger.warn("No SpectrumIdentification found for '" +
								psmXML.getSpectrumIdentificationRef() + "'");
					}
				}
				
			} else {
				logger.warn("PSM '" + psmXML.getId() + "' has no valid fileRef '" +
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
					spectrumID
					);
			
			// add the scores
			for (ScoreXML scoreXML : psmXML.getScores()) {
				ScoreModel score = new ScoreModel(scoreXML.getValue(),
						scoreXML.getCvAccession(),
						scoreXML.getName());
				
				psm.addScore(score);
			}
			
			// add the modifications
			for (ModificationXML modXML : psmXML.getModification()) {
				Modification mod;
				
				mod = new Modification(
						modXML.getResidue().charAt(0),
						modXML.getMass(),
						modXML.getDescription(),
						modXML.getAccession()
						);
				
				psm.addModification(modXML.getLocation(), mod);
			}
			
			// if the PSM has decoy information, set it
			if (psmXML.getIsDecoy() != null) {
				psm.setIsDecoy(psmXML.getIsDecoy());
			}
			
			// if the PSM has uniqueness information, set it
			if (psmXML.getIsUnique() != null) {
				psm.setIsUnique(psmXML.getIsUnique());
			}
			
			// the params
			for (AbstractParam param : psmXML.getParamList()) {
				psm.addParam(param);
			}
			
			// check for PSM set settings warnings
			if (psm.getRetentionTime() == null) {
				psmSetSettingsWarnings.
					get(IdentificationKeySettings.RETENTION_TIME.toString()).
					add(psm.getFile().getID());
			}
			if ((psm.getSourceID() == null) ||
					(psm.getSourceID().trim().equals(""))) {
				psmSetSettingsWarnings.
					get(IdentificationKeySettings.SOURCE_ID.toString()).
					add(psm.getFile().getID());
			}
			if ((psm.getSpectrumTitle() == null) ||
					(psm.getSpectrumTitle().trim().equals(""))) {
				psmSetSettingsWarnings.
					get(IdentificationKeySettings.SPECTRUM_TITLE.toString()).
					add(psm.getFile().getID());
			}
			if ((psm.getSequence() == null) ||
					(psm.getSequence().trim().equals(""))) {
				psmSetSettingsWarnings.
					get(IdentificationKeySettings.SEQUENCE.toString()).
					add(psm.getFile().getID());
			}
			
			// put the PSM into the map
			psms.put(psm.getID(), psm);
			
			// skip the whitespace between spectra
			if (xmlr.getEventType() == XMLStreamConstants.CHARACTERS) {
				xmlr.next();
			}
		}
		
		try {
			xmlr.require(XMLStreamConstants.END_ELEMENT, null, "spectraList");
			if (xmlr.hasNext()) {
				xmlr.nextTag();
			}
		} catch (XMLStreamException e) {
			logger.warn("spectraList does not end correctly.");
		}
	}
	
	
	/**
	 * Parses the accessions in a chunked matter. It assumes, the given
	 * {@link XMLStreamReader} is at the position of an
	 * {@link AccessionsListXML}.
	 * 
	 * @param xmlr
	 * @throws JAXBException 
	 * @throws XMLStreamException 
	 */
	public void parseAccessionsChunked(XMLStreamReader xmlr)
			throws JAXBException, XMLStreamException {
		xmlr.require(XMLStreamConstants.START_ELEMENT, null, "accessionsList");
		
		JAXBContext jaxbContext = JAXBContext.newInstance(AccessionXML.class);
		Unmarshaller um = jaxbContext.createUnmarshaller();
		
		// move to the first accession element
		xmlr.nextTag();
		while (xmlr.getEventType() == XMLStreamConstants.START_ELEMENT) {
			xmlr.require(XMLStreamConstants.START_ELEMENT, null, "accession");
			
			AccessionXML accXML = (AccessionXML) um.unmarshal(xmlr);
			Accession accession;
			Set<Long> files = new HashSet<Long>();
			Map<Long, String> descriptions = new HashMap<Long, String>();
			Set<String> searchDatabaseRefs = new HashSet<String>();
			
			for (FileRefXML fileRef : accXML.getFileRefs()) {
				files.add(fileRef.getFile_ref());
			}
			
			for (DescriptionXML descXML : accXML.getDescriptions()) {
				descriptions.put(descXML.getFileRefID(), descXML.getValue());
			}
			
			for (SearchDatabaseRefXML dbRef : accXML.getSearchDatabaseRefs()) {
				searchDatabaseRefs.add(dbRef.getSearchDatabase_ref());
			}
			
			accession = new Accession(accXML.getId(),
					accXML.getAcc(),
					files,
					descriptions, 
					accXML.getSequence(),
					searchDatabaseRefs,
					null);	// group = null, is set later with the groups
			
			accessions.put(accession.getID(), accession);
			
			// skip the whitespaces
			if (xmlr.getEventType() == XMLStreamConstants.CHARACTERS) {
				xmlr.next();
			}
		}
		
		try {
			xmlr.require(XMLStreamConstants.END_ELEMENT, null, "accessionsList");
			if (xmlr.hasNext()) {
				xmlr.nextTag();
			}
		} catch (XMLStreamException e) {
			logger.warn("accessionsList does not end correctly.");
		}
	}
	
	
	/**
	 * Parses the peptides in a chunked matter. It assumes, the given
	 * {@link XMLStreamReader} is at the position of a {@link PeptidesListXML}.
	 * 
	 * @param xmlr
	 * @throws JAXBException 
	 * @throws XMLStreamException 
	 */
	public void parsePeptidesChunked(XMLStreamReader xmlr)
			throws JAXBException, XMLStreamException {
		xmlr.require(XMLStreamConstants.START_ELEMENT, null, "peptidesList");
		
		JAXBContext jaxbContext = JAXBContext.newInstance(PeptideXML.class);
		Unmarshaller um = jaxbContext.createUnmarshaller();
		
		// move to the first peptide element
		xmlr.nextTag();
		while (xmlr.getEventType() == XMLStreamConstants.START_ELEMENT) {
			xmlr.require(XMLStreamConstants.START_ELEMENT, null, "peptide");
			
			PeptideXML pepXML = (PeptideXML) um.unmarshal(xmlr);
			Peptide peptide;
			List<PeptideSpectrumMatch> psmList = new ArrayList<PeptideSpectrumMatch>();
			
			peptide = new Peptide(pepXML.getId(), pepXML.getSequence());
			
			for (SpectrumRefXML spectrumRefXML
					: pepXML.getSpectrumRefList().getSpectrumRefs()) {
				PeptideSpectrumMatch psm = psms.get(spectrumRefXML.getSpectrumRefID());
				
				if (psm != null) {
					psmList.add(psm);
					// backlink the peptide in the PSM
					psm.setPeptide(peptide);
				} else {
					logger.warn("No spectrumMatch found for '" +
								spectrumRefXML.getSpectrumRefID() + "'");
				}
			}
			peptide.setSpectra(psmList);
			
			for (OccurenceXML occXML : pepXML.getOccurrences().getOccurrences()) {
				Accession acc = accessions.get(occXML.getAccessionRefID());
				
				if (acc != null) {
					peptide.addAccessionOccurrence(acc, occXML.getStart(),
							occXML.getEnd());
				} else {
					logger.warn("No accession found for occurrence '" +
							occXML.getAccessionRefID() + "'");
				}
			}
			
			peptides.put(peptide.getID(), peptide);
			
			// skip the whitespaces
			if (xmlr.getEventType() == XMLStreamConstants.CHARACTERS) {
				xmlr.next();
			}
		}
		
		try {
			xmlr.require(XMLStreamConstants.END_ELEMENT, null, "peptidesList");
			if (xmlr.hasNext()) {
				xmlr.nextTag();
			}
		} catch (XMLStreamException e) {
			logger.warn("peptidesList does not end correctly.");
		}
	}
	
	
	/**
	 * Parses the groups in a chunked matter. It assumes, the given
	 * {@link XMLStreamReader} is at the position of a {@link GroupsListXML}.
	 * 
	 * @param xmlr
	 * @throws JAXBException 
	 * @throws XMLStreamException 
	 */
	public void parseGroupsChunked(XMLStreamReader xmlr)
			throws JAXBException, XMLStreamException {
		xmlr.require(XMLStreamConstants.START_ELEMENT, null, "groupsList");
		
		Map<Long, List<ChildRefXML>> groupsChildren =
				new HashMap<Long, List<ChildRefXML>>();
		
		JAXBContext jaxbContext = JAXBContext.newInstance(GroupXML.class);
		Unmarshaller um = jaxbContext.createUnmarshaller();
		
		// move to the first peptide element
		xmlr.nextTag();
		while (xmlr.getEventType() == XMLStreamConstants.START_ELEMENT) {
			xmlr.require(XMLStreamConstants.START_ELEMENT, null, "group");
			
			GroupXML groupXML = (GroupXML) um.unmarshal(xmlr);
			Group group = new Group(groupXML.getId());
			
			group.setTreeID(groupXML.getTreeId());
			
			for (AccessionRefXML accRef : groupXML.getAccessionsRefList()) {
				Accession accession = accessions.get(accRef.getAccRefID());
				
				if (accession != null) {
					group.addAccession(accession);
					// now the accession's group can be set
					accession.setGroup(group);
				} else {
					logger.warn("No accession found for groups reference '" +
							accRef.getAccRefID() + "'");
				}
			}
			
			for (PeptideRefXML pepRef : groupXML.getPeptidesRefList()) {
				Peptide peptide = peptides.get(pepRef.getPepRefID());
				
				if (peptide != null) {
					group.addPeptide(peptide);
					// now the peptide's group can be set
					peptide.setGroup(group);
				} else {
					logger.warn("No peptide found for groups reference '" +
							pepRef.getPepRefID() + "'");
				}
			}
			
			// to get the allAccessions right, children are set in a second round
			if (groupXML.getChildrenRefList() != null) {
				groupsChildren.put(group.getID(), groupXML.getChildrenRefList());
			}
			
			groups.put(group.getID(), group);
			
			// skip the whitespaces
			if (xmlr.getEventType() == XMLStreamConstants.CHARACTERS) {
				xmlr.next();
			}
		}
		
		try {
			xmlr.require(XMLStreamConstants.END_ELEMENT, null, "groupsList");
			if (xmlr.hasNext()) {
				xmlr.nextTag();
			}
		} catch (XMLStreamException e) {
			logger.warn("groupsList does not end correctly.");
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
					logger.warn("No group found for child reference '" +
							childRef.getChildRefID() + "'");
				}
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
	
	
	/**
	 * For testing purposes only.
	 * @param args
	 */
	public static void main(String[] args) {
		PIAIntermediateJAXBHandler intermediateHandler;
		intermediateHandler = new PIAIntermediateJAXBHandler();
		
		try {
			Runtime runtime = Runtime.getRuntime();
			int mb = 1024*1024;
			final long startTime = System.nanoTime();
			final long endTime;
			
			
			System.out.println(args[0] + " successfully parsed.\n" + 
					"\t" + intermediateHandler.getFiles().size() + " files\n" +
					"\t" + intermediateHandler.getSpectraData().size() + " spectra data inputs\n" + 
					"\t" + intermediateHandler.getSearchDatabase().size() + " searchDBs\n" + 
					"\t" + intermediateHandler.getAnalysisSoftware().size() + " softwares\n" + 
					"\t" + intermediateHandler.getGroups().size() + " groups\n" + 
					"\t" + intermediateHandler.getAccessions().size() + " accessions\n" + 
					"\t" + intermediateHandler.getPeptides().size() + " peptides\n" + 
					"\t" + intermediateHandler.getPSMs().size() + " peptide spectrum matches\n" + 
					"\t" + intermediateHandler.getNrTrees() + " trees");
			
			endTime = System.nanoTime();
			
			//Print used memory
			System.out.println("Used Memory: " 
				+ (runtime.totalMemory() - runtime.freeMemory()) / mb);
			//Print free memory
			System.out.println("Free Memory: " 
				+ runtime.freeMemory() / mb);
			//Print total available memory
			System.out.println("Total Memory: " + runtime.totalMemory() / mb);
			//Print Maximum available memory
			System.out.println("Max Memory: " + runtime.maxMemory() / mb);
			//Print the execution time
			System.out.println("Execution time: " + ((endTime - startTime) / 1000000));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
