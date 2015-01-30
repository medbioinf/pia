package de.mpc.pia.intermediate.compiler.parser.searchengines;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import uk.ac.ebi.jmzidml.model.mzidml.AbstractParam;
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
import uk.ac.ebi.jmzidml.model.mzidml.UserParam;
import de.mpc.PD.APeptideScores;
import de.mpc.PD.APeptides;
import de.mpc.PD.APeptidesAminoAcidModifications;
import de.mpc.PD.APeptidesTerminalModifications;
import de.mpc.PD.AminoAcidModifications;
import de.mpc.PD.AminoAcids;
import de.mpc.PD.FastaFiles;
import de.mpc.PD.FileInfos;
import de.mpc.PD.MassPeaks;
import de.mpc.PD.PeptideScores;
import de.mpc.PD.PeptideScores_decoy;
import de.mpc.PD.Peptides;
import de.mpc.PD.PeptidesAminoAcidModifications;
import de.mpc.PD.PeptidesAminoAcidModifications_decoy;
import de.mpc.PD.PeptidesProteins;
import de.mpc.PD.PeptidesProteins_decoy;
import de.mpc.PD.PeptidesTerminalModifications;
import de.mpc.PD.PeptidesTerminalModifications_decoy;
import de.mpc.PD.Peptides_decoy;
import de.mpc.PD.ProcessingNodeParameters;
import de.mpc.PD.ProcessingNodeScores;
import de.mpc.PD.ProcessingNodes;
import de.mpc.PD.ProteinAnnotations;
import de.mpc.PD.Proteins;
import de.mpc.PD.SpectrumHeaders;
import de.mpc.PD.DB.JDBCAccess;
import de.mpc.PD.Params.SimpleProgramParameters;
import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.parser.FastaHeaderInfos;
import de.mpc.pia.intermediate.compiler.parser.InputFileParserFactory;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.tools.MzIdentMLTools;
import de.mpc.pia.tools.PIAConstants;
import de.mpc.pia.tools.PIATools;


/**
 * This class parses the data from a Thermo MSF file for a given
 * {@link PIACompiler}.<br/>
 * 
 * @author julian
 *
 */
public class ThermoMSFFileParser {
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(ThermoMSFFileParser.class);
	
	
	/**
	 * We don't ever want to instantiate this class
	 */
	private ThermoMSFFileParser() {
		throw new AssertionError();
	}
	
	
	/**
	 * Parses the data from an ProteomeDiscoverer's MSF file given by its name
	 * into the given {@link PIACompiler}.
	 */
	public static boolean getDataFromThermoMSFFile(String name, String fileName,
			PIACompiler compiler) {
		logger.debug("getting data from file: " + fileName);
		
		SimpleProgramParameters fileConnectionParams = null;
		
		// set up the DB connection to the MSF file
		boolean bUseJDBC = true;  // always use the JDBC connection
		fileConnectionParams = new SimpleProgramParameters(fileName, bUseJDBC);
		JDBCAccess jdbc = new JDBCAccess();
		jdbc.connectToExistingDB(fileName);
		fileConnectionParams.setJDBCAccess(jdbc);
		
		Param param;
		AbstractParam abstractParam;
		
		Map<Long, SpectrumIdentification> nodeNumbersToIdentifications =
				new HashMap<Long, SpectrumIdentification>();
		Map<Long, SpectrumIdentificationProtocol> nodeNumbersToProtocols =
				new HashMap<Long, SpectrumIdentificationProtocol>();
		Map<Long, AnalysisSoftware> nodeNumbersToSoftwares =
				new HashMap<Long, AnalysisSoftware>();
		
		// iterate through the ProcessingNodes and get the settings etc.
		for (Map.Entry<Object, Object> nodeObjectIt
				: ProcessingNodes.getObjectMap(fileConnectionParams, ProcessingNodes.class).entrySet()) {
			
			if (!(nodeObjectIt.getValue() instanceof ProcessingNodes)) {
				logger.warn("not a processingNodes " + nodeObjectIt.getValue().getClass().getCanonicalName());
				continue;
			}
			
			ProcessingNodes node = (ProcessingNodes)nodeObjectIt.getValue();
			AnalysisSoftware software = createAnalysisSoftware(node);
			
			if (software != null) {
				// add the software
				software = compiler.putIntoSoftwareMap(software);
				nodeNumbersToSoftwares.put(node.getProcessingNodeNumber(), software);
				
				// get all additional data
				SearchDatabase searchDatabase = null;
				Enzyme enzyme = null;
				Integer maxMissedCleavages = null;
				ParamList additionalSearchParams = new ParamList();
				Tolerance fragmentTolerance = null;
				Tolerance peptideTolerance = null;
				ModificationParams modificationParameters =
						new ModificationParams();
				
				List<String> processingParamNames = node.getProcessingNodeParameterNames();
		        for (String paramName : processingParamNames) {
					ProcessingNodeParameters processingNodeParams =
		        			new ProcessingNodeParameters(fileConnectionParams, node.getProcessingNodeNumber(), paramName);
					
		        	if (paramName.equals("FastaDatabase") ||
		        			paramName.equals("Protein Database")) {
						// get database information
						FastaFiles fastaFiles = processingNodeParams.getFastaFilesObj();
						if (fastaFiles != null) {
							// database used
							searchDatabase = new SearchDatabase();
							
							searchDatabase.setId(software.getName() +  "DB" +
									node.getProcessingNodeNumber());
							
							searchDatabase.setLocation("PD database");
							searchDatabase.setName(fastaFiles.getFileName());
							
							// databaseName
							param = new Param();
							abstractParam = new UserParam();
							abstractParam.setName(fastaFiles.getFileName());
							param.setParam(abstractParam);
							searchDatabase.setDatabaseName(param);
							
							// this gets the number of taxonomy filtered sequences/residues
							searchDatabase.setNumDatabaseSequences(fastaFiles.getNumberOfProteins().longValue());
							searchDatabase.setNumResidues(fastaFiles.getNumberOfAminoAcids().longValue());
							
							// add searchDB to the compiler
							searchDatabase = compiler.putIntoSearchDatabasesMap(searchDatabase);
						}
		        	} else if (paramName.equals("Enzyme") ||
		        			paramName.equals("Enzyme Name")) {
		        		enzyme = MzIdentMLTools.getEnzymeFromName(processingNodeParams.getParameterValue());
		        	} else if (paramName.equals("MaxMissedCleavages") ||
		        			paramName.equals("MissedCleavages") ||
		        			paramName.equals("Maximum Missed Cleavage Sites") ||
		        			paramName.equals("Max. Missed Cleavage Sites")) {
						// the allowed missed cleavages
						maxMissedCleavages = 
								Integer.parseInt(processingNodeParams.getParameterValue());
					} else if (paramName.equals("UseAveragePrecursorMass") ||
							paramName.equals("Use Average Precursor Mass")) {
						// precursor mass monoisotopic or average
						abstractParam = new CvParam();
						if (processingNodeParams.getParameterValue().equals("False")) {
							// monoisotopic
				    		((CvParam)abstractParam).setAccession("MS:1001211");
				    		((CvParam)abstractParam).setCv(MzIdentMLTools.getCvPSIMS());
				    		abstractParam.setName("parent mass type mono");
						} else {
							// average
				    		((CvParam)abstractParam).setAccession("MS:1001212");
				    		((CvParam)abstractParam).setCv(MzIdentMLTools.getCvPSIMS());
				    		abstractParam.setName("parent mass type average");
						}
			    		additionalSearchParams.getCvParam().add(
			    				(CvParam)abstractParam);
					} else if (paramName.equals("UseAverageFragmentMass") ||
							paramName.equals("Use Average Fragment Masses") ||
							paramName.equals("Use Average Fragment Mass")) {
						// fragment mass monoisotopic or average
						abstractParam = new CvParam();
						if (processingNodeParams.getParameterValue().equals("False")) {
							// monoisotopic
				    		((CvParam)abstractParam).setAccession("MS:1001256");
				    		((CvParam)abstractParam).setCv(MzIdentMLTools.getCvPSIMS());
				    		abstractParam.setName("fragment mass type mono");
						} else {
							// average
				    		((CvParam)abstractParam).setAccession("MS:1001255");
				    		((CvParam)abstractParam).setCv(MzIdentMLTools.getCvPSIMS());
				    		abstractParam.setName("fragment mass type average");
						}
			    		additionalSearchParams.getCvParam().add(
			    				(CvParam)abstractParam);
					} else if (paramName.equals("FragmentTolerance") ||
							paramName.equals("Fragment Mass Tolerance") ||
							paramName.equals("MS2Tolerance")) {
						fragmentTolerance = new Tolerance();
						
						String split[] =
								processingNodeParams.getParameterValue().split(" ");
						
						abstractParam = new CvParam();
						((CvParam)abstractParam).setAccession("MS:1001412");
						((CvParam)abstractParam).setCv(MzIdentMLTools.getCvPSIMS());
						abstractParam.setName("search tolerance plus value");
						abstractParam.setValue(split[0]);
						MzIdentMLTools.setUnitParameterFromString(split[1], abstractParam);
						fragmentTolerance.getCvParam().add((CvParam)abstractParam);
						
						abstractParam = new CvParam();
						((CvParam)abstractParam).setAccession("MS:1001413");
						((CvParam)abstractParam).setCv(MzIdentMLTools.getCvPSIMS());
						abstractParam.setName("search tolerance minus value");
						abstractParam.setValue(split[0]);
						MzIdentMLTools.setUnitParameterFromString(split[1], abstractParam);
						fragmentTolerance.getCvParam().add((CvParam)abstractParam);
					} else if (paramName.equals("PeptideTolerance") ||
							paramName.equals("Precursor Mass Tolerance") ||
							paramName.equals("MS1Tolerance")) {
						peptideTolerance = new Tolerance();
						
						String split[] =
								processingNodeParams.getParameterValue().split(" ");
						
						abstractParam = new CvParam();
						((CvParam)abstractParam).setAccession("MS:1001412");
						((CvParam)abstractParam).setCv(MzIdentMLTools.getCvPSIMS());
						abstractParam.setName("search tolerance plus value");
						abstractParam.setValue(split[0]);
						MzIdentMLTools.setUnitParameterFromString(split[1], abstractParam);
						peptideTolerance.getCvParam().add((CvParam)abstractParam);
						
						abstractParam = new CvParam();
						((CvParam)abstractParam).setAccession("MS:1001413");
						((CvParam)abstractParam).setCv(MzIdentMLTools.getCvPSIMS());
						abstractParam.setName("search tolerance minus value");
						abstractParam.setValue(split[0]);
						MzIdentMLTools.setUnitParameterFromString(split[1], abstractParam);
						peptideTolerance.getCvParam().add((CvParam)abstractParam);
					} else if (paramName.equals("MinimumPeptideLength")) {
						abstractParam = new CvParam();
						((CvParam)abstractParam).setAccession("MS:1002322");
						((CvParam)abstractParam).setCv(MzIdentMLTools.getCvPSIMS());
						abstractParam.setName("ProteomeDiscoverer:min peptide length");
						abstractParam.setValue(processingNodeParams.getParameterValue());
						additionalSearchParams.getCvParam().add((CvParam)abstractParam);
					} else if (paramName.equals("MaximumPeptideLength")) {
						abstractParam = new CvParam();
						((CvParam)abstractParam).setAccession("MS:1002323");
						((CvParam)abstractParam).setCv(MzIdentMLTools.getCvPSIMS());
						abstractParam.setName("ProteomeDiscoverer:max peptide length");
						abstractParam.setValue(processingNodeParams.getParameterValue());
						additionalSearchParams.getCvParam().add((CvParam)abstractParam);
					} else {
						// parse addiional software specific settings
						parseSoftwareSpecificSettings(node, processingNodeParams,
								additionalSearchParams, modificationParameters);
					}
				}
				
				// create the spectrumIDProtocol
				SpectrumIdentificationProtocol spectrumIDProtocol =
						new SpectrumIdentificationProtocol();
				
				spectrumIDProtocol.setId(
						"pdAnalysis_" + node.getID());
				spectrumIDProtocol.setAnalysisSoftware(software);
				
				// only MS/MS searches are usable for PIA
				param = new Param();
				abstractParam = new CvParam();
				((CvParam)abstractParam).setAccession("MS:1001083");
				((CvParam)abstractParam).setCv(MzIdentMLTools.getCvPSIMS());
				abstractParam.setName("ms-ms search");
				param.setParam(abstractParam);
				
				spectrumIDProtocol.setSearchType(param);
				
				if (additionalSearchParams.getCvParam().size() > 0) {
					spectrumIDProtocol.setAdditionalSearchParams(
							additionalSearchParams);
				}
				
				spectrumIDProtocol.setModificationParams(modificationParameters);
				
				if (enzyme != null) {
					if (maxMissedCleavages != null) {
						enzyme.setMissedCleavages(maxMissedCleavages);
					}
					
					Enzymes enzymes = new Enzymes();
					spectrumIDProtocol.setEnzymes(enzymes);
					enzymes.getEnzyme().add(enzyme);
				}
				
				if (fragmentTolerance != null) {
					spectrumIDProtocol.setFragmentTolerance(fragmentTolerance);
				}
				
				if (peptideTolerance != null) {
					spectrumIDProtocol.setParentTolerance(peptideTolerance);
				}
				
				// no threshold set, take all PSMs from the dat file
				ParamList paramList = new ParamList();
				abstractParam = new CvParam();
				((CvParam)abstractParam).setAccession("MS:1001494");
				((CvParam)abstractParam).setCv(MzIdentMLTools.getCvPSIMS());
				abstractParam.setName("no threshold");
				paramList.getCvParam().add((CvParam)abstractParam);
				spectrumIDProtocol.setThreshold(paramList);
				
				nodeNumbersToProtocols.put(node.getProcessingNodeNumber(),
						spectrumIDProtocol);
				
				
				// add the spectrum identification
				SpectrumIdentification spectrumID = new SpectrumIdentification();
				spectrumID.setId("node" + node.getProcessingNodeNumber() + "Identification");
				spectrumID.setSpectrumIdentificationList(null);
				
				if (searchDatabase != null) {
					SearchDatabaseRef searchDBRef = new SearchDatabaseRef();
					searchDBRef.setSearchDatabase(searchDatabase);
					spectrumID.getSearchDatabaseRef().add(searchDBRef);
				}
				
				nodeNumbersToIdentifications.put(
						node.getProcessingNodeNumber(), spectrumID);
			}
		}
		
		if (nodeNumbersToIdentifications.size() < 1) {
			logger.error("There are no search nodes in the MSF file!");
			return false;
		}
		
		Map<Long, PIAInputFile> nodeNumbersToInputFiles = 
				new HashMap<Long, PIAInputFile>();
		for (Map.Entry<Long, SpectrumIdentification> idIt : nodeNumbersToIdentifications.entrySet()) {
			PIAInputFile file;
			
			if (nodeNumbersToIdentifications.size() > 1) {
				// more than one identification in the MSF file -> make several PIAInputFiles
				String searchName = name + "_" +
						nodeNumbersToSoftwares.get(idIt.getKey()).getName() +
						"_" + idIt.getKey();
				file = compiler.insertNewFile(searchName, fileName,
						InputFileParserFactory.InputFileTypes.THERMO_MSF_INPUT.getFileSuffix());
			} else {
				// only one identification node in the file
				file = compiler.insertNewFile(name, fileName,
						InputFileParserFactory.InputFileTypes.THERMO_MSF_INPUT.getFileSuffix());
			}
			
			SpectrumIdentificationProtocol protocol = nodeNumbersToProtocols.get(idIt.getKey());
			SpectrumIdentification id = idIt.getValue();
			
			file.addSpectrumIdentificationProtocol(protocol);
			
			id.setSpectrumIdentificationProtocol(protocol);
			file.addSpectrumIdentification(id);
			
			nodeNumbersToInputFiles.put(idIt.getKey(), file);
		}
		
		
		// get the amino acid information from file
		Map<Character, AminoAcids> aminoAcidMap = getAminoAcids(fileConnectionParams);
		
		// mapping from fileID to input spectra
		Map<Long, SpectraData> spectraDataMap =
				new HashMap<Long, SpectraData>();
		
		// mapping from the ID of SpectrumIdentification to IDs of used inputSpectra
		Map<String, Set<String>> spectrumIdToSpectraData =
				new HashMap<String, Set<String>>();
		
		logger.info("get spectra info...");
		Map<Object, Object> spectraMap = SpectrumHeaders.getObjectMap(fileConnectionParams, SpectrumHeaders.class);
		logger.info("#spectra: " + spectraMap.size());
		
		logger.info("get peak info...");
		Map<Object, Object> massPeakMap = MassPeaks.getObjectMap(fileConnectionParams, MassPeaks.class);
		logger.info("#peaks: " + massPeakMap.size());
		
		logger.info("get file info...");
		Map<Object, Object> fileMap = FileInfos.getObjectMap(fileConnectionParams, FileInfos.class);
		logger.info("#files: " + fileMap.size());
		
		logger.info("get amino acid modifications...");
		Map<Object, Object> modificationsMap = AminoAcidModifications.getObjectMap(fileConnectionParams, AminoAcidModifications.class);
		logger.info("#amino acid modifications: " + modificationsMap.size());
		
		logger.info("get protein sequences...");
		Map<Long, String> sequencesMap = new HashMap<Long, String>();
		for (Object proteinObj : Proteins.getObjectMap(fileConnectionParams, Proteins.class).values()) {
			Proteins protein = (Proteins)proteinObj;
			sequencesMap.put(protein.getProteinID(), protein.getSequence());
		}
		logger.info("#protein sequences: " + sequencesMap.size());
		
		logger.info("get protein annotations...");
		Map<Long, String> annotationsMap = new HashMap<Long, String>();
		for (Object annotationObj : ProteinAnnotations.getObjectMap(fileConnectionParams, ProteinAnnotations.class).values()) {
			ProteinAnnotations annotation = (ProteinAnnotations)annotationObj;
			annotationsMap.put(annotation.getProteinID(), annotation.getDescription());
		}
		logger.info("#protein annotations: " + annotationsMap.size());
		
		logger.info("get scores...");
		// mapping from scoreID to scoreName
		Map<Long, String> scoresMap = new HashMap<Long, String>();
		for (Object scoreObj : ProcessingNodeScores.getObjectMap(fileConnectionParams, ProcessingNodeScores.class).values()) {
			ProcessingNodeScores score = (ProcessingNodeScores)scoreObj;
			scoresMap.put(score.getScoreID(), score.getFriendlyName());
		}
		logger.info("#scores: " + scoresMap.size());
		
		
		// parse the peptides
		logger.info("get peptide info...");
		Collection<Object> peptides = Peptides.getObjectMap(fileConnectionParams, Peptides.class).values();
		logger.info("#peptides: " + peptides.size());
		
		logger.info("get modifications info...");
		// map from peptideID to modifications
		Map<Long, List<APeptidesAminoAcidModifications>> peptidesModifications =
				new HashMap<Long, List<APeptidesAminoAcidModifications>>();
		for (Object modObj : PeptidesAminoAcidModifications.getObjectMap(fileConnectionParams, PeptidesAminoAcidModifications.class).values()) {
			APeptidesAminoAcidModifications mod = (APeptidesAminoAcidModifications)modObj;
			
			List<APeptidesAminoAcidModifications> modList = peptidesModifications.get(mod.getPeptideID());
			if (modList == null) {
				modList = new ArrayList<APeptidesAminoAcidModifications>();
				peptidesModifications.put(mod.getPeptideID(), modList);
			}
			
			modList.add(mod);
		}
		logger.info("#modified peptides: " + peptidesModifications.size());
		
		logger.info("get terminal modifications info...");
		// map from peptideID to terminal modifications
		Map<Long, List<AminoAcidModifications>> terminalModifications =
				new HashMap<Long, List<AminoAcidModifications>>();
		for (Object modObj : PeptidesTerminalModifications.getObjectMap(fileConnectionParams, PeptidesTerminalModifications.class).values()) {
			APeptidesTerminalModifications termMod = (APeptidesTerminalModifications)modObj;
			
			List<AminoAcidModifications> termModList = terminalModifications.get(termMod.getPeptideID());
			if (termModList == null) {
				termModList = new ArrayList<AminoAcidModifications>();
				terminalModifications.put(termMod.getPeptideID(), termModList);
			}
			
			termModList.add((AminoAcidModifications)modificationsMap.get(termMod.getTerminalModificationID()));
		}
		logger.info("#terminal modified peptides: " + terminalModifications.size());
		
		logger.info("get peptides/proteins information...");
		//map from peptideID to proteins
		Map<Long, List<Long>> peptidesProteins = new HashMap<Long, List<Long>>();
		for (Object pepProtObj : PeptidesProteins.getObjectMap(fileConnectionParams, PeptidesProteins.class).values()) {
			PeptidesProteins pepProt = (PeptidesProteins)pepProtObj;
			
			List<Long> proteinList = peptidesProteins.get(pepProt.getPeptideID());
			if (proteinList == null) {
				proteinList = new ArrayList<Long>();
				peptidesProteins.put(pepProt.getPeptideID(), proteinList);
			}
			
			proteinList.add(pepProt.getProteinID());
		}
		logger.info("#peptides associated to proteins: " + peptidesProteins.size());
		
		logger.info("get peptides/scores information...");
		// map from peptideID to scores
		Map<Long, List<APeptideScores>> peptidesScores = new HashMap<Long, List<APeptideScores>>();
		for (Object scoreObject : PeptideScores.getObjectMap(fileConnectionParams, PeptideScores.class).values()) {
			PeptideScores score = (PeptideScores)scoreObject;
			
			List<APeptideScores> scoreList = peptidesScores.get(score.getPeptideID());
			if (scoreList == null) {
				scoreList = new ArrayList<APeptideScores>();
				peptidesScores.put(score.getPeptideID(), scoreList);
			}
			
			scoreList.add(score);
		}
		logger.info("#peptides associated to sores: " + peptidesScores.size());
		
		long emptyPSMs = 0;
		for (Object peptide : peptides) {
			if (parsePSM(peptide, false, spectraMap, massPeakMap, fileMap,
					peptidesProteins, peptidesScores, peptidesModifications, terminalModifications,
					aminoAcidMap, sequencesMap, annotationsMap, scoresMap,
					compiler,
					nodeNumbersToIdentifications, nodeNumbersToInputFiles, spectraDataMap, spectrumIdToSpectraData) == null) {
				emptyPSMs++;
			}
		}
		logger.info("target peptides processed");
		
		
		// parse the decoy peptides
		logger.info("get decoy peptide info...");
		peptides = Peptides_decoy.getObjectMap(fileConnectionParams, Peptides_decoy.class).values();
		logger.info("#decoy peptides: " + peptides.size());
		
		if (peptides.size() > 0) {
			logger.info("get decoy modifications info...");
			// map from peptideID to modifications
			peptidesModifications = new HashMap<Long, List<APeptidesAminoAcidModifications>>();
			for (Object modObj : PeptidesAminoAcidModifications_decoy.getObjectMap(fileConnectionParams, PeptidesAminoAcidModifications_decoy.class).values()) {
				APeptidesAminoAcidModifications mod = (APeptidesAminoAcidModifications)modObj;
				
				List<APeptidesAminoAcidModifications> modList = peptidesModifications.get(mod.getPeptideID());
				if (modList == null) {
					modList = new ArrayList<APeptidesAminoAcidModifications>();
					peptidesModifications.put(mod.getPeptideID(), modList);
				}
				modList.add(mod);
			}
			logger.info("#modified decoy peptides: " + peptidesModifications.size());
			
			logger.info("get decoy terminal modifications info...");
			// map from peptideID to terminal modifications
			terminalModifications = new HashMap<Long, List<AminoAcidModifications>>();
			for (Object modObj : PeptidesTerminalModifications_decoy.getObjectMap(fileConnectionParams, PeptidesTerminalModifications_decoy.class).values()) {
				APeptidesTerminalModifications termMod = (APeptidesTerminalModifications)modObj;
				
				List<AminoAcidModifications> termModList = terminalModifications.get(termMod.getPeptideID());
				if (termModList == null) {
					termModList = new ArrayList<AminoAcidModifications>();
					terminalModifications.put(termMod.getPeptideID(), termModList);
				}
				
				termModList.add((AminoAcidModifications)modificationsMap.get(termMod.getTerminalModificationID()));
			}
			logger.info("#terminal modified decoy peptides: " + terminalModifications.size());
			
			logger.info("get decoy peptides/proteins information...");
			// map from peptideID to proteins
			peptidesProteins = new HashMap<Long, List<Long>>();
			for (Object pepProtObj : PeptidesProteins_decoy.getObjectMap(fileConnectionParams, PeptidesProteins_decoy.class).values()) {
				PeptidesProteins_decoy pepProt = (PeptidesProteins_decoy)pepProtObj;
				
				List<Long> proteinList = peptidesProteins.get(pepProt.getPeptideID());
				if (proteinList == null) {
					proteinList = new ArrayList<Long>();
					peptidesProteins.put(pepProt.getPeptideID(), proteinList);
				}
				
				proteinList.add(pepProt.getProteinID());
			}
			logger.info("#decoy peptides associated to proteins: " + peptidesProteins.size());
			
			logger.info("get decoy peptides/scores information...");
			// map from peptideID to scores
			peptidesScores = new HashMap<Long, List<APeptideScores>>();
			for (Object scoreObject : PeptideScores_decoy.getObjectMap(fileConnectionParams, PeptideScores_decoy.class).values()) {
				PeptideScores_decoy score = (PeptideScores_decoy)scoreObject;
				
				List<APeptideScores> scoreList = peptidesScores.get(score.getPeptideID());
				if (scoreList == null) {
					scoreList = new ArrayList<APeptideScores>();
					peptidesScores.put(score.getPeptideID(), scoreList);
				}
				
				scoreList.add(score);
			}
			logger.info("#decoy peptides associated to sores: " + peptidesScores.size());
			
			for (Object peptide : peptides) {
				if (parsePSM(peptide, true, spectraMap, massPeakMap, fileMap,
						peptidesProteins, peptidesScores, peptidesModifications, terminalModifications,
						aminoAcidMap, sequencesMap, annotationsMap, scoresMap,
						compiler,
						nodeNumbersToIdentifications, nodeNumbersToInputFiles, spectraDataMap, spectrumIdToSpectraData) == null) {
					emptyPSMs++;
				}
			}
			logger.info("decoy peptides processed");
		} else {
			logger.info("no decoy peptides, that's ok");
		}
		
		logger.info("all peptides processed");
		
		if (emptyPSMs > 0) {
			logger.info("There were " + emptyPSMs + " PSMs without protein connection, these are rejected!");
		}
		
		fileConnectionParams.closeDB();
		return true;
	}
	
	
	/**
	 * Creates the {@link AnalysisSoftware} from the given friendlyName. If the
	 * software is not known/implemented, null is returned.
	 * 
	 * @param friendlyName
	 * @param psiMS
	 * @return
	 */
	private static AnalysisSoftware createAnalysisSoftware(ProcessingNodes node) {
		AnalysisSoftware software = new AnalysisSoftware();
		
		if ((node.getNodeName().equals("SequestNode") && node.getFriendlyName().equals("SEQUEST")) ||
				(node.getNodeName().equals("IseNode") && node.getFriendlyName().equals("Sequest HT"))) {
			software.setId("sequest");
			software.setName("SEQUEST");
			
			CvParam cvParam = new CvParam();
			cvParam.setAccession("MS:1001208");
			cvParam.setCv(MzIdentMLTools.getCvPSIMS());
			cvParam.setName("Sequest");
			
			Param param = new Param();
			param.setParam(cvParam);
			
			software.setSoftwareName(param);
		} else if (node.getNodeName().equals("Mascot") && node.getFriendlyName().equals("Mascot")) {
			software.setId("mascot");
			software.setName("Mascot");
			software.setUri("http://www.matrixscience.com/");
			
			CvParam cvParam = new CvParam();
			((CvParam)cvParam).setAccession("MS:1001207");
			((CvParam)cvParam).setCv(MzIdentMLTools.getCvPSIMS());
			cvParam.setName("Mascot");
			
			Param param = new Param();
			param.setParam(cvParam);
			software.setSoftwareName(param);
		} else if (node.getNodeName().equals("AmandaPeptideIdentifier") && node.getFriendlyName().equals("MS Amanda")) {
			software.setId("amanda");
			software.setName("Amanda");
			
			CvParam cvParam = new CvParam();
			((CvParam)cvParam).setAccession("MS:1002336");
			((CvParam)cvParam).setCv(MzIdentMLTools.getCvPSIMS());
			cvParam.setName("Amanda");
			
			Param param = new Param();
			param.setParam(cvParam);
			software.setSoftwareName(param);
		} else {
			// TODO: add more software
			return null;
		}
		
		return software;
	}
	
	
	/**
	 * Parses software specific search settings and ModificationParams.
	 */
	private static boolean parseSoftwareSpecificSettings(ProcessingNodes node,
			ProcessingNodeParameters processingNodeParams, ParamList additionalSearchParams,
			ModificationParams modificationParameters) {
		
		if (node.getNodeName().equals("Mascot") && node.getFriendlyName().equals("Mascot")) {
			// Mascot settings
			
			if (processingNodeParams.getParameterName().equals("Instrument")) {
				// mascot instrument
				CvParam cvParam = new CvParam();
				cvParam.setAccession("MS:1001656");
				cvParam.setCv(MzIdentMLTools.getCvPSIMS());
				cvParam.setName("Mascot:Instrument");
				cvParam.setValue(processingNodeParams.getParameterValue());
				additionalSearchParams.getCvParam().add(cvParam);
				return true;
			} else if (processingNodeParams.getParameterName().startsWith("DynModification_")) {
				// dynamic mascot modification
				SearchModification searchMod = 
						parseModification(processingNodeParams.getValueDisplayString(), false);
				
				if (searchMod != null) {
					modificationParameters.getSearchModification().add(searchMod);
				}
				return true;
			} else if (processingNodeParams.getParameterName().startsWith("Static_") &&
					!processingNodeParams.getParameterName().equals("Static_X")) {
				// static mascot modification
				SearchModification searchMod = 
						parseModification(processingNodeParams.getValueDisplayString(), true);
				
				if (searchMod != null) {
					modificationParameters.getSearchModification().add(searchMod);
				}
				return true;
			}
		} else if ((node.getNodeName().equals("SequestNode") && node.getFriendlyName().equals("SEQUEST")) ||
				(node.getNodeName().equals("IseNode") && node.getFriendlyName().equals("Sequest HT"))) {
			// SEQUEST settings
			
			if (processingNodeParams.getParameterName().startsWith("DynMod_") ||
					processingNodeParams.getParameterName().startsWith("DynNTermMod") ||
					processingNodeParams.getParameterName().startsWith("DynCTermMod") ||
					processingNodeParams.getParameterName().startsWith("DynamicModification")) {
				// dynamic sequest modification
				SearchModification searchMod = 
						parseModification(processingNodeParams.getValueDisplayString(), false);
				
				if (searchMod != null) {
					modificationParameters.getSearchModification().add(searchMod);
				}
				return true;
			} else if (processingNodeParams.getParameterName().startsWith("StatMod_") ||
					processingNodeParams.getParameterName().startsWith("StatNTermMod") ||
					processingNodeParams.getParameterName().startsWith("StatCTermMod") ||
					processingNodeParams.getParameterName().startsWith("StaticModification")) {
				// static sequest modification
				SearchModification searchMod = 
						parseModification(processingNodeParams.getValueDisplayString(), true);
				
				if (searchMod != null) {
					modificationParameters.getSearchModification().add(searchMod);
				}
				return true;
			}
		} else if (node.getNodeName().equals("AmandaPeptideIdentifier") && node.getFriendlyName().equals("MS Amanda")) {
			// Amanda settings
			
			if (processingNodeParams.getParameterName().startsWith("DynMod_")) {
				// dynamic amanda modification
				SearchModification searchMod = 
						parseModification(processingNodeParams.getValueDisplayString(), false);
				
				if (searchMod != null) {
					modificationParameters.getSearchModification().add(searchMod);
				}
				return true;
			} else if (processingNodeParams.getParameterName().startsWith("StatMod_")) {
				// static amanda modification
				SearchModification searchMod = 
						parseModification(processingNodeParams.getValueDisplayString(), true);
				
				if (searchMod != null) {
					modificationParameters.getSearchModification().add(searchMod);
				}
				return true;
				
			}
		}
		
		return false;
	}
	
	
	/**
	 * Parses the modification from the ProteomeDiscoverer settings in the
	 * {@link ProcessingNodeParameters}.
	 * 
	 * @param modString the whole strin
	 * @param isFixed fixed or variable modification
	 * @return
	 */
	private static SearchModification parseModification(String modString, boolean isFixed) {
		String split[] = modString.split("/");
		if (split.length < 2) {
			logger.warn("Modification could not be parsed: "
					+ modString);
			return null;
		}
		
		split = split[1].split("Da");
		
		Float massShift = null;
		try {
			massShift = Float.parseFloat(split[0]);
		} catch (NumberFormatException e) {
			logger.warn("Could not parse massShift " + split[0] + " in " +
					modString);
			return null;
		}
		
		SearchModification searchMod = new SearchModification();
		searchMod.setFixedMod(isFixed);
		searchMod.setMassDelta(massShift);
		
		split = split[1].
				substring(split[1].indexOf("(")+1, split[1].indexOf(")")).
				split(",");
		
		for (String res : split) {
			if (res.contains("N-Term") || res.contains("C-Term")) {
				searchMod.getResidues().add(".");
				
				CvParam  specificity = new CvParam();
				specificity.setCv(MzIdentMLTools.getCvPSIMS());
				if (res.contains("N-Term")) {
					if (res.contains("Protein")) {
						specificity.setAccession(PIAConstants.CV_MODIFICATION_SPECIFICITY_PROTEIN_N_TERM_ACCESSION);
						specificity.setName(PIAConstants.CV_MODIFICATION_SPECIFICITY_PROTEIN_N_TERM_NAME);
					} else {
						specificity.setAccession(PIAConstants.CV_MODIFICATION_SPECIFICITY_PEP_N_TERM_ACCESSION);
						specificity.setName(PIAConstants.CV_MODIFICATION_SPECIFICITY_PEP_N_TERM_NAME);
					}
				} else {
					if (res.contains("Protein")) {
						specificity.setAccession(PIAConstants.CV_MODIFICATION_SPECIFICITY_PROTEIN_C_TERM_ACCESSION);
						specificity.setName(PIAConstants.CV_MODIFICATION_SPECIFICITY_PROTEIN_C_TERM_NAME);
					} else {
						specificity.setAccession(PIAConstants.CV_MODIFICATION_SPECIFICITY_PEP_C_TERM_ACCESSION);
						specificity.setName(PIAConstants.CV_MODIFICATION_SPECIFICITY_PEP_C_TERM_NAME);
					}
				}
				
				SpecificityRules specRules = new SpecificityRules();
				specRules.getCvParam().add(specificity);
				searchMod.getSpecificityRules().add(specRules);
			} else {
				searchMod.getResidues().add(res);
			}
		}
		
		return searchMod;
	}
	
	
	/**
	 * Creates and adds an {@link PeptideSpectrumMatch} from an peptide entry in
	 * the MSF file. 
	 */
	private static PeptideSpectrumMatch parsePSM(Object peptideObject,
			boolean isDecoy,
			Map<Object, Object> spectraMap,
			Map<Object, Object> massPeakMap,
			Map<Object, Object> fileMap,
			Map<Long, List<Long>> peptidesProteins,
			Map<Long, List<APeptideScores>> peptidesScores,
			Map<Long, List<APeptidesAminoAcidModifications>> peptidesModifications,
			Map<Long, List<AminoAcidModifications>> terminalModifications,
			Map<Character, AminoAcids> aminoAcidMap,
			Map<Long, String> sequencesMap,
			Map<Long, String> annotationsMap,
			Map<Long, String> scoresMap,
			PIACompiler compiler,
			Map<Long, SpectrumIdentification> nodeNumbersToIdentifications,
			Map<Long, PIAInputFile> nodeNumbersToInputFiles,
			Map<Long, SpectraData> spectraDataMap,
			Map<String, Set<String>> spectrumIdToSpectraData
			) {
		APeptides peptide = (APeptides)peptideObject;
		
		if (!peptidesProteins.containsKey(peptide.getPeptideID())) {
			// there is no protein information for the peptide! PD does these things...
			// for now: do not include these PSMs
			// TODO: find some better solution 
			return null;
		}
		
		// get some spectrum information
		SpectrumHeaders spectrum = (SpectrumHeaders)spectraMap.get(peptide.getSpectrumID());
		MassPeaks massPeak = (MassPeaks)massPeakMap.get(spectrum.getMassPeakID());
		
		int charge = spectrum.getCharge();
		double precursorMZ = PIATools.round(massPeak.getMass(), 6);
		String sourceID = "index=" + (spectrum.getFirstScan()-1);
		
		// get the spectrumIdentification, which identified this peptide
		SpectrumIdentification spectrumID =
				nodeNumbersToIdentifications.get(peptide.getProcessingNodeNumber());
		
		if (spectrumID == null) {
			logger.warn("PSM (" + sourceID + ", " + peptide.getSequence() +") does not come from a search.");
			return null;
		} else {
			String rawFileName = ((FileInfos)fileMap.get(massPeak.getFileID())).getFileName();
			
			SpectraData spectraData =
					spectraDataMap.get(massPeak.getFileID());
			
			if ((spectraData == null)) {
				
				spectraData = new SpectraData();
				
				spectraData.setId("inputfile_" + rawFileName);
				spectraData.setLocation(rawFileName);
				
				if ((rawFileName.endsWith(".mgf") ||
						rawFileName.endsWith(".MGF"))) {
					FileFormat fileFormat = new FileFormat();
					
					CvParam cvParam = new CvParam();
					cvParam.setAccession("MS:1001062");
					cvParam.setCv(MzIdentMLTools.getCvPSIMS());
					cvParam.setName("Mascot MGF file");
					fileFormat.setCvParam(cvParam);
					spectraData.setFileFormat(fileFormat);
					
					SpectrumIDFormat idFormat = new SpectrumIDFormat();
					cvParam = new CvParam();
					cvParam.setAccession("MS:1000774");
					cvParam.setCv(MzIdentMLTools.getCvPSIMS());
					cvParam.setName("multiple peak list nativeID format");
					idFormat.setCvParam(cvParam);
					spectraData.setSpectrumIDFormat(idFormat);
				} else if ((rawFileName.endsWith(".raw") ||
						rawFileName.endsWith("RAW"))) {
					FileFormat fileFormat = new FileFormat();
					
					CvParam cvParam = new CvParam();
					cvParam.setAccession("MS:1000577");
					cvParam.setCv(MzIdentMLTools.getCvPSIMS());
					cvParam.setName("raw data file");
					fileFormat.setCvParam(cvParam);
					spectraData.setFileFormat(fileFormat);
				}
				
				spectraData = compiler.putIntoSpectraDataMap(spectraData);
				
				spectraDataMap.put(massPeak.getFileID(), spectraData);
			} 
			
			// look, if spectrumID has the needed spectraData, if not, add it
			Set<String> spectraDataIDs =
					spectrumIdToSpectraData.get(spectrumID.getId());
			if (spectraDataIDs == null) {
				spectraDataIDs = new HashSet<String>();
				spectrumIdToSpectraData.put(spectrumID.getId(), spectraDataIDs);
			}
			if (!spectraDataIDs.contains(spectraData.getId())) {
				InputSpectra inputSpectra = new InputSpectra();
				inputSpectra.setSpectraData(spectraData);
				
				spectrumID.getInputSpectra().add(inputSpectra);
				spectraDataIDs.add(spectraData.getId());
			}
			
		}
		
		String pepSequence = peptide.getSequence();
		PIAInputFile file = nodeNumbersToInputFiles.get(peptide.getProcessingNodeNumber());
		
		// get the modifications
		Map<Integer, Modification> modifications = new HashMap<Integer, Modification>();
		if (peptidesModifications.containsKey(peptide.getPeptideID())) {
			for (APeptidesAminoAcidModifications aaMod : peptidesModifications.get(peptide.getPeptideID())) {
				int loc = (int)aaMod.getPosition() + 1;
				
				// TODO: get the unimod modification code
				Modification modification = new Modification(
						pepSequence.charAt(loc-1),
						aaMod.getAminoAcidModification().getDeltaMass(),
						aaMod.getAminoAcidModification().getModificationName(),
						null);
				
				modifications.put(loc, modification);
			}
		}
		
		if (terminalModifications.containsKey(peptide.getPeptideID())) {
			for (AminoAcidModifications termMod : terminalModifications.get(peptide.getPeptideID())) {
				int loc = -1;
				
				switch (termMod.getPositionType()) {
				case 1:
				case 3:
					loc = 0;
					break;
				case 2:
				case 4:
					loc = pepSequence.length() + 1;
					break;
				default:
					logger.error("unknown position type for terminal modification: " + termMod.getPositionType());
					return null;
				}
				if (loc > -1) {
					// TODO: get the unimod modification code
					Modification modification = new Modification(
							'.',
							termMod.getDeltaMass(),
							termMod.getModificationName(),
							null);
					modifications.put(loc, modification);
					break;
				}
			}
		}
		
		PeptideSpectrumMatch psm = compiler.insertNewSpectrum(
				charge,
				precursorMZ,
				PIATools.round(spectrum.getMass() - getPeptideMassForCharge(1, pepSequence, aminoAcidMap, modifications), 6),
				(spectrum.getRetentionTime()*60),
				pepSequence,
				peptide.getMissedCleavages(),
				sourceID,
				null,
				file,
				spectrumID);
		
		psm.setIsDecoy(isDecoy);
		
		// get the peptide or create it
		Peptide piaPeptide = compiler.getPeptide(pepSequence);
		if (piaPeptide == null) {
			piaPeptide = compiler.insertNewPeptide(pepSequence);
		}
		
		// add the spectrum to the peptide
		piaPeptide.addSpectrum(psm);
		
		// add the scores
		if (peptidesScores.containsKey(peptide.getPeptideID())) {
			for (APeptideScores pepScore : peptidesScores.get(peptide.getPeptideID())) {
				
				ScoreModelEnum scoreModel =
						ScoreModelEnum.getModelByDescription(
								scoresMap.get(pepScore.getScoreID()));
				
				ScoreModel score;
				if (!scoreModel.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
					score = new ScoreModel(pepScore.getScoreValue(),
							scoreModel);
				} else {
					score = new ScoreModel(pepScore.getScoreValue(),
							null, scoresMap.get(pepScore.getScoreID()));
				}
				psm.addScore(score);
			}
		}
		
		// add the modifications
		for (Map.Entry<Integer, Modification> modIt : modifications.entrySet()) { 
			psm.addModification(modIt.getKey(), modIt.getValue());
		}
		
		// add protein infos
		for (Long proteinID : peptidesProteins.get(peptide.getPeptideID())) {
			FastaHeaderInfos fastaInfo =
					FastaHeaderInfos.parseHeaderInfos(annotationsMap.get(proteinID));
			if (fastaInfo == null) {
				logger.error("Could not parse protein annotation '" +
						annotationsMap.get(proteinID) + "'");
				continue;
			}
			
			String proteinSequence = sequencesMap.get(proteinID);
			
			// add the Accession to the compiler (if it is not already there)
			Accession acc = compiler.getAccession(fastaInfo.getAccession());
			if (acc == null) {
				acc = compiler.insertNewAccession(
						fastaInfo.getAccession(), proteinSequence);
			}
			
			acc.addFile(file.getID());
			
			if ((fastaInfo.getDescription() != null) &&
					(fastaInfo.getDescription().length() > 0)) {
				acc.addDescription(file.getID(), fastaInfo.getDescription());
			}
			
			if ((acc.getDbSequence() == null) &&
					(proteinSequence != null)) {
				acc.setDbSequence(proteinSequence);
			} else if ((acc.getDbSequence() != null) &&
					(proteinSequence != null) &&
					!acc.getDbSequence().equals(proteinSequence)) {
				if (acc.getDbSequence() != null)  {
					if (!proteinSequence.equals(acc.getDbSequence())) {
						logger.warn("Different DBSequences found for same Accession, this is not supported!\n" +
								"\t Accession: " + acc.getAccession() + 
								"\t'" + proteinSequence + "'\n" +
								"\t'" + acc.getDbSequence() + "'");
					}
				}
			}
			
			// add the searchDB to the accession
			for (SearchDatabaseRef dbRef
					: nodeNumbersToIdentifications.get(peptide.getProcessingNodeNumber()).getSearchDatabaseRef()) {
				acc.addSearchDatabaseRef(dbRef.getSearchDatabase().getId());
			}
			
			// add the accession occurrence to the peptide
			// have to recalculate the occurrence, because it is not saved in the MSF
			if (proteinSequence != null) {
				int start = proteinSequence.indexOf(pepSequence);
				
				while (start > -1) {
					piaPeptide.addAccessionOccurrence(acc, start + 1,
							start + pepSequence.length());
					
					start = proteinSequence.indexOf(pepSequence, start + 1);
				}
			} else {
				// without valid sequence, set a fake occurrence
				piaPeptide.addAccessionOccurrence(acc, 0, 0);
			}
			
			// now insert the peptide and the accession into the accession peptide map
			Set<Peptide> accsPeptides =
					compiler.getFromAccPepMap(acc.getAccession());
			
			if (accsPeptides == null) {
				accsPeptides = new HashSet<Peptide>();
				compiler.putIntoAccPepMap(acc.getAccession(), accsPeptides);
			}
			
			accsPeptides.add(piaPeptide);
			
			// and also insert them into the peptide accession map
			Set<Accession> pepsAccessions =
					compiler.getFromPepAccMap(piaPeptide.getSequence());
			
			if (pepsAccessions == null) {
				pepsAccessions = new HashSet<Accession>();
				compiler.putIntoPepAccMap(piaPeptide.getSequence(),
						pepsAccessions);
			}
			
			pepsAccessions.add(acc);
		}
		
		return psm;
	}
	
	
	/**
	 * Parses the amino acids from the MSF file in a map from oneLetterCode to
	 * AminoAcid
	 * 
	 * @param spp
	 * @return
	 */
	private static Map<Character, AminoAcids> getAminoAcids(SimpleProgramParameters spp) {
		Map<Character, AminoAcids> aminoAcidMap = new HashMap<Character, AminoAcids>(25);
		
		for (Object aaObj : AminoAcids.getObjectMap(spp, AminoAcids.class).values()) {
			AminoAcids aa = (AminoAcids)aaObj;
			
			if (aa.getOneLetterCode() != ' ') {
				aminoAcidMap.put(aa.getOneLetterCode(), aa);
			}
		}
		
		return aminoAcidMap;
	}
	
	
	/**
	 * This method calculates the mass based on the charge and the amino acid
	 * weights parsed in the MSF file
	 */
	private static double getPeptideMassForCharge(int charge, String sequence,
			Map<Character, AminoAcids> aminoAcidMap, Map<Integer, Modification> modifications){
		double calculatedMass = 0.0;
		
		for (Character aa : sequence.toCharArray()) {
			calculatedMass += aminoAcidMap.get(aa).getMonoisotopicMass();
		}
		
		// check modifications
		for (Modification mod : modifications.values()) {
			calculatedMass += mod.getMass();
		}
		
		calculatedMass = calculatedMass + 17.002735;	// C-terminal cleavage change
		calculatedMass = calculatedMass + 1.007825;		// N-terminal cleavage change
		
		calculatedMass = (calculatedMass + (double)charge * PIAConstants.H_MASS.doubleValue()) / (double)charge;
		
		return calculatedMass;
    }
}
