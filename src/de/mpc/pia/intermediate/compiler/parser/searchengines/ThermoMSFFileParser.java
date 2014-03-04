package de.mpc.pia.intermediate.compiler.parser.searchengines;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import uk.ac.ebi.jmzidml.model.mzidml.AbstractParam;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.Cv;
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

import com.compomics.thermo_msf_parser.Parser;
import com.compomics.thermo_msf_parser.msf.ProcessingNode;
import com.compomics.thermo_msf_parser.msf.ProcessingNodeParameter;
import com.compomics.thermo_msf_parser.msf.Protein;
import com.compomics.thermo_msf_parser.msf.ScoreType;
import com.compomics.thermo_msf_parser.msf.Spectrum;

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
	 * Parses the data from an mzIdentML file given by its name into the given
	 * {@link PIACompiler}.
	 * 
	 * @param fileName name of the mzTab file
	 */
	public static boolean getDataFromThermoMSFFile(String name, String fileName,
			PIACompiler compiler) {
		int accNr = 0;
		int pepNr = 0;
		int specNr = 0;
		
		Parser thermoParser = null;
		try {
			thermoParser = new Parser(fileName, false);
		} catch (SQLException e) {
			logger.error("Could not parse " + fileName + ".", e);
			return false;
		} catch (ClassNotFoundException e) {
			logger.error("Could not find the SQLite library.", e);
			return false;
		}
		
		
		Cv psiMS = new Cv();
		psiMS.setId("PSI-MS");
		psiMS.setFullName("PSI-MS");
		psiMS.setUri("http://psidev.cvs.sourceforge.net/viewvc/*checkout*/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo");
		
		
		Param param;
		AbstractParam abstractParam;
		
		Map<Integer, SpectrumIdentification> nodeNumbersToIdentifications =
				new HashMap<Integer, SpectrumIdentification>();
		Map<Integer, SpectrumIdentificationProtocol> nodeNumbersToProtocols =
				new HashMap<Integer, SpectrumIdentificationProtocol>();
		Map<Integer, AnalysisSoftware> nodeNumbersToSoftwares =
				new HashMap<Integer, AnalysisSoftware>();
		
		for (ProcessingNode node : thermoParser.getProcessingNodes()) {
			AnalysisSoftware software = createAnalysisSoftware(
					node, psiMS);
			
			if (software != null) {
				// add the software
				nodeNumbersToSoftwares.put(node.getProcessingNodeNumber(),
						compiler.putIntoSoftwareMap(software));
				
				// get all additional data
				SearchDatabase searchDatabase = null;
				Enzyme enzyme = null;
				Integer maxMissedCleavages = null;
				ParamList additionalSearchParams = new ParamList();
				Tolerance fragmentTolerance = null;
				Tolerance peptideTolerance = null;
				ModificationParams modificationParameters =
						new ModificationParams();
				
				for (ProcessingNodeParameter processingParam
						: node.getProcessingNodeParameters()) {
					
					if (processingParam.getParameterName().equals("Database") ||
							processingParam.getParameterName().equals("FastaDatabase")) {
						// database used
						searchDatabase = new SearchDatabase();
						
						searchDatabase.setId(software.getName() +  "DB" +
								node.getProcessingNodeNumber());
						
						searchDatabase.setLocation("PD database");
						searchDatabase.setName(processingParam.getValueDisplayString());
						
						// databaseName
						param = new Param();
						abstractParam = new UserParam();
						abstractParam.setName(processingParam.getValueDisplayString());
						param.setParam(abstractParam);
						searchDatabase.setDatabaseName(param);
						
						//TODO: are these accessible somehow?
						//searchDatabase.setNumDatabaseSequences();
						//searchDatabase.setNumResidues();
						
						// add searchDB to the compiler
						searchDatabase = compiler.putIntoSearchDatabasesMap(searchDatabase);
					} else if (processingParam.getParameterName().
							equals("Enzyme")) {
						
						enzyme = new Enzyme();
						enzyme.setId("enzyme_" +
								node.getProcessingNodeNumber());
						
						ParamList enzymeName = new ParamList();
						
						if (processingParam.getParameterValue().equalsIgnoreCase("Trypsin") ||
								processingParam.getValueDisplayString().equalsIgnoreCase("Trypsin (Full)")) {
							// this is trypsin
							enzyme.setSiteRegexp("(?<=[KR])(?!P)");
							
							abstractParam = new CvParam();
							((CvParam)abstractParam).setAccession(
									PIAConstants.CV_TRYPSIN_ACCESSION);
							((CvParam)abstractParam).setCv(psiMS);
							((CvParam)abstractParam).setName(
									PIAConstants.CV_TRYPSIN_NAME);
							
							enzymeName.getCvParam().add((CvParam)abstractParam);
						} else {
							abstractParam = new UserParam();
							((UserParam)abstractParam).setName(
									processingParam.getValueDisplayString());
							enzymeName.getCvParam().add((CvParam)abstractParam);
						}
						enzyme.setEnzymeName(enzymeName);
					} else if (processingParam.getParameterName().equals("MaxMissedCleavages") ||
							processingParam.getParameterName().equals("MissedCleavages")) {
						// the allowed missed cleavages
						maxMissedCleavages = 
								Integer.parseInt(processingParam.getParameterValue());
					} else if (processingParam.getParameterName().
							equals("UseAveragePrecursorMass")) {
						// precursor mass monoisotopic or average
						abstractParam = new CvParam();
						if (processingParam.getParameterValue().equals("False")) {
				    		((CvParam)abstractParam).setAccession("MS:1001211");
				    		((CvParam)abstractParam).setCv(psiMS);
				    		abstractParam.setName("parent mass type mono");
						} else {
				    		((CvParam)abstractParam).setAccession("MS:1001212");
				    		((CvParam)abstractParam).setCv(psiMS);
				    		abstractParam.setName("parent mass type average");
						}
			    		additionalSearchParams.getCvParam().add(
			    				(CvParam)abstractParam);
					} else if (processingParam.getParameterName().
							equals("UseAverageFragmentMass")) {
						// fragment mass monoisotopic or average
						abstractParam = new CvParam();
						if (processingParam.getParameterValue().equals("False")) {
				    		((CvParam)abstractParam).setAccession("MS:1001256");
				    		((CvParam)abstractParam).setCv(psiMS);
				    		abstractParam.setName("fragment mass type mono");
						} else {
				    		((CvParam)abstractParam).setAccession("MS:1001255");
				    		((CvParam)abstractParam).setCv(psiMS);
				    		abstractParam.setName("fragment mass type average");
						}
			    		additionalSearchParams.getCvParam().add(
			    				(CvParam)abstractParam);
					} else if (processingParam.getParameterName().
							equals("FragmentTolerance")) {
						fragmentTolerance = new Tolerance();
						
						String split[] =
								processingParam.getParameterValue().split(" ");
						
						abstractParam = new CvParam();
						((CvParam)abstractParam).setAccession("MS:1001412");
						((CvParam)abstractParam).setCv(psiMS);
						abstractParam.setName("search tolerance plus value");
						abstractParam.setValue(split[0]);
						if (split.length > 1) {
							abstractParam.setUnitName(split[1]);
						}
						fragmentTolerance.getCvParam().add((CvParam)abstractParam);
						
						abstractParam = new CvParam();
						((CvParam)abstractParam).setAccession("MS:1001413");
						((CvParam)abstractParam).setCv(psiMS);
						abstractParam.setName("search tolerance minus value");
						abstractParam.setValue(split[0]);
						if (split.length > 1) {
							abstractParam.setUnitName(split[1]);
						}
						fragmentTolerance.getCvParam().add((CvParam)abstractParam);
					} else if (processingParam.getParameterName().
							equals("PeptideTolerance")) {
						peptideTolerance = new Tolerance();
						
						String split[] =
								processingParam.getParameterValue().split(" ");
						
						abstractParam = new CvParam();
						((CvParam)abstractParam).setAccession("MS:1001412");
						((CvParam)abstractParam).setCv(psiMS);
						abstractParam.setName("search tolerance plus value");
						abstractParam.setValue(split[0]);
						if (split.length > 1) {
							abstractParam.setUnitName(split[1]);
						}
						peptideTolerance.getCvParam().add(
								(CvParam)abstractParam);
						
						abstractParam = new CvParam();
						((CvParam)abstractParam).setAccession("MS:1001413");
						((CvParam)abstractParam).setCv(psiMS);
						abstractParam.setName("search tolerance minus value");
						abstractParam.setValue(split[0]);
						if (split.length > 1) {
							abstractParam.setUnitName(split[1]);
						}
						peptideTolerance.getCvParam().add(
								(CvParam)abstractParam);
					} else {
						// check software specific values
						if (node.getNodeName().equals("Mascot")) {
							if (processingParam.getParameterName().equals("Instrument")) {
								// mascot instrument
								abstractParam = new CvParam();
								((CvParam)abstractParam).setAccession(
										"MS:1001656");
								((CvParam)abstractParam).setCv(psiMS);
								abstractParam.setName("Mascot:Instrument");
								abstractParam.setValue(
										processingParam.getParameterValue());
								additionalSearchParams.getCvParam().add(
										(CvParam)abstractParam);
							} else if (processingParam.getParameterName().
									startsWith("DynModification_")) {
								// dynamic sequest modification
								SearchModification searchMod = 
										createModification(
												processingParam.getValueDisplayString(),
												false, psiMS);
								
								if (searchMod != null) {
									modificationParameters
										.getSearchModification().add(searchMod);
								}
							} else if (processingParam.getParameterName().
									startsWith("Static_") &&
									!processingParam.getParameterName().equals("Static_X")) {
								// static sequest modification
								SearchModification searchMod = 
										createModification(
												processingParam.getValueDisplayString(),
												true, psiMS);
								
								if (searchMod != null) {
									modificationParameters
										.getSearchModification().add(searchMod);
								}
							}
							
							
						} else if (node.getNodeName().equals("SequestNode")) {
							
							if (processingParam.getParameterName().startsWith("DynMod_") ||
									processingParam.getParameterName().startsWith("DynNTermMod") ||
									processingParam.getParameterName().startsWith("DynCTermMod")) {
								// dynamic sequest modification
								SearchModification searchMod = 
										createModification(
												processingParam.getValueDisplayString(),
												false, psiMS);
								
								if (searchMod != null) {
									modificationParameters
										.getSearchModification().add(searchMod);
								}
							} else if (processingParam.getParameterName().startsWith("StatMod_") ||
									processingParam.getParameterName().startsWith("StatNTermMod") ||
									processingParam.getParameterName().startsWith("StatCTermMod")) {
								// static sequest modification
								SearchModification searchMod = 
										createModification(
												processingParam.getValueDisplayString(),
												true, psiMS);
								
								if (searchMod != null) {
									modificationParameters
										.getSearchModification().add(searchMod);
								}
							}
							
						}
					}
				}
				
				// create the spectrumIDProtocol
				SpectrumIdentificationProtocol spectrumIDProtocol =
						new SpectrumIdentificationProtocol();
				
				spectrumIDProtocol.setId(
						"pdAnalysis_" + node.getProcessingNodeId());
				spectrumIDProtocol.setAnalysisSoftware(software);
				
				// only MS/MS searches are usable for PIA
				param = new Param();
				abstractParam = new CvParam();
				((CvParam)abstractParam).setAccession("MS:1001083");
				((CvParam)abstractParam).setCv(psiMS);
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
				((CvParam)abstractParam).setCv(psiMS);
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
			logger.error("there are no search nodes in the MSF file.");
			return false;
		}
		
		Map<Integer, PIAInputFile> nodeNumbersToInputFiles = 
				new HashMap<Integer, PIAInputFile>();
		for (Map.Entry<Integer, SpectrumIdentification> idIt :
			nodeNumbersToIdentifications.entrySet()) {
			
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
			
			SpectrumIdentificationProtocol protocol = 
					nodeNumbersToProtocols.get(idIt.getKey());
			SpectrumIdentification id = idIt.getValue();
			
			file.addSpectrumIdentificationProtocol(protocol);
			
			id.setSpectrumIdentificationProtocol(protocol);
			file.addSpectrumIdentification(id);
			
			nodeNumbersToInputFiles.put(idIt.getKey(), file);
		}
		
		// mapping from file name to input spectra
		Map<String, SpectraData> spectraDataMap =
				new HashMap<String, SpectraData>();
		
		// mapping from the ID of SpectrumIdentification to IDs of used inputSpectra
		Map<String, Set<String>> spectrumIdToSpectraData =
				new HashMap<String, Set<String>>();
		
		// now go through the spectra
		for (Spectrum spec: thermoParser.getSpectra()) {
			
			int charge = spec.getCharge();
			
			double precursorMZ = (spec.getSinglyChargedMass() +
					charge * PIAConstants.H_MASS.doubleValue() -
					PIAConstants.H_MASS.doubleValue()) / charge;
			precursorMZ = PIATools.round(precursorMZ, 6);
			
			String sourceID = "index=" + (spec.getFirstScan()-1);
			
			for (com.compomics.thermo_msf_parser.msf.Peptide pep
					: spec.getPeptides()) {
				
				SpectrumIdentification spectrumID =
						nodeNumbersToIdentifications.get(pep.getProcessingNodeNumber());
				String sequence = pep.getSequence();
				
				if (spectrumID == null) {
					logger.warn("PSM (" + sourceID + ", " + pep.getSequence() +
							") does not come from a search.");
					continue;
				} else {
					String rawFileName =
							thermoParser.getRawfileNameByFileId(spec.getFileId());
					
					SpectraData spectraData =
							spectraDataMap.get(rawFileName);
					
					if ((spectraData == null) &&
							(rawFileName.endsWith(".mgf") ||
									rawFileName.endsWith(".MGF"))) {
						spectraData = new SpectraData();
						
						spectraData.setId("inputfile_" + rawFileName);
						spectraData.setLocation(rawFileName);
						
						FileFormat fileFormat = new FileFormat();
						
						abstractParam = new CvParam();
						((CvParam)abstractParam).setAccession("MS:1001062");
						((CvParam)abstractParam).setCv(psiMS);
						abstractParam.setName("Mascot MGF file");
						fileFormat.setCvParam((CvParam)abstractParam);
						spectraData.setFileFormat(fileFormat);
						
						SpectrumIDFormat idFormat = new SpectrumIDFormat();
						abstractParam = new CvParam();
						((CvParam)abstractParam).setAccession("MS:1000774");
						((CvParam)abstractParam).setCv(psiMS);
						abstractParam.setName("multiple peak list nativeID format");
						idFormat.setCvParam((CvParam)abstractParam);
						spectraData.setSpectrumIDFormat(idFormat);
						
						spectraData =
								compiler.putIntoSpectraDataMap(spectraData);
						
						spectraDataMap.put(rawFileName, spectraData);
					}
					
					// look, if spectrumID has the needed spectraDaza, if not, add it
					Set<String> spectraDataIDs =
							spectrumIdToSpectraData.get(spectrumID.getId());
					if (spectraDataIDs == null) {
						spectraDataIDs = new HashSet<String>();
						spectrumIdToSpectraData.put(spectrumID.getId(),
								spectraDataIDs);
					}
					if (!spectraDataIDs.contains(spectraData.getId())) {
						InputSpectra inputSpectra = new InputSpectra();
						inputSpectra.setSpectraData(spectraData);
						
						spectrumID.getInputSpectra().add(inputSpectra);
						spectraDataIDs.add(spectraData.getId());
					}
				}
				
				PIAInputFile file = nodeNumbersToInputFiles.get(
						pep.getProcessingNodeNumber());
				
				PeptideSpectrumMatch psm = compiler.insertNewSpectrum(
						charge,
						precursorMZ,
						PIATools.round(spec.getSinglyChargedMass()-pep.getPeptideMassForCharge(1), 6),
						(spec.getRetentionTime()*60),
						sequence,
						pep.getMissedCleavage(),
						sourceID,
						null,
						file,
						spectrumID);
				specNr++;
				
				// get the peptide or create it
				Peptide peptide = compiler.getPeptide(sequence);
				if (peptide == null) {
					peptide = compiler.insertNewPeptide(sequence);
					pepNr++;
				}
				
				// add the spectrum to the peptide
				peptide.addSpectrum(psm);
				
				
				// add the scores
				for (ScoreType scoreType : pep.getScoreTypes()) {
					ScoreModelEnum scoreModel =
							ScoreModelEnum.getModelByDescription(scoreType.getFriendlyName());
					
					ScoreModel score;
					
					if (!scoreModel.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
						score = new ScoreModel(pep.getScoreByScoreType(scoreType),
								scoreModel);
					} else {
						score = new ScoreModel(pep.getScoreByScoreType(scoreType),
								null, scoreType.getFriendlyName());
					}
					
					psm.addScore(score);
				}
				
				// add the modifications
				if (pep.getPeptideModificationPositions().size() > 0) {
					
					for (int nrMod = 0;
							nrMod < pep.getPeptideModificationPositions().size();
							nrMod++) {
						int loc = pep.getPeptideModificationPositions().
								get(nrMod).getPosition() + 1;
						
						com.compomics.thermo_msf_parser.msf.Modification mod =
								pep.getPeptideModifications().get(nrMod);
						
						Character residue = null;
						if ((loc == 0) || (loc > sequence.length())) {
							residue = '.';
						} else {
							residue = sequence.charAt(loc-1);
						}
						
						Modification modification;
						modification = new Modification(
								residue,
								mod.getDeltaMass(),
								mod.getModificationName(),
								null);
						
						// TODO: get the unimod modification code
						psm.addModification(loc, modification);
					}
				}
				
				// add protein infos
				for (Protein protein : pep.getProteins()) {
					FastaHeaderInfos fastaInfo =
							FastaHeaderInfos.parseHeaderInfos(protein.getDescription());
					String proteinSequence = null;
					
					if (fastaInfo == null) {
						logger.error("Could not parse protein '" +
								protein.getDescription() + "'");
						continue;
					}
					
					try {
						proteinSequence = protein.getSequence();
					} catch (SQLException e) {
						logger.warn("could not get sequence for " +
								fastaInfo.getAccession(), e);
					}
					
					// add the Accession to the compiler (if it is not already there)
					Accession acc = compiler.getAccession(fastaInfo.getAccession());
					if (acc == null) {
						acc = compiler.insertNewAccession(
								fastaInfo.getAccession(), proteinSequence);
						accNr++;
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
								logger.warn("Different DBSequences found for same Accession, this is not suported!\n" +
										"\t Accession: " + acc.getAccession() + 
										"\t'" + proteinSequence + "'\n" +
										"\t'" + acc.getDbSequence() + "'");
							}
						}
					}
					
					
					// add the searchDB to the accession
					for (SearchDatabaseRef dbRef
							: nodeNumbersToIdentifications.get(pep.getProcessingNodeNumber()).getSearchDatabaseRef()) {
						// there is only one searchDB per sequest run
						acc.addSearchDatabaseRef(dbRef.getSearchDatabase().getId());
						break;
					}
					
					
					// add the accession occurrence to the peptide
					// have to recalculate the occurrence, not saved in the MSF
					if (proteinSequence != null) {
						int start = proteinSequence.indexOf(sequence);
						
						while (start > -1) {
							peptide.addAccessionOccurrence(acc,
									start + 1,
									start + sequence.length());
							
							start = proteinSequence.indexOf(sequence, start + 1);
						}
					} else {
						// without valid sequence, set a fake occurrence
						peptide.addAccessionOccurrence(acc,
								0, 0);
					}
					
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
		}
		
		logger.info("inserted new: \n\t" +
				pepNr + " peptides\n\t" +
				specNr + " peptide spectrum matches\n\t" +
				accNr + " accessions");
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
	private static AnalysisSoftware createAnalysisSoftware(ProcessingNode node,
			Cv psiMS) {
		AnalysisSoftware software = new AnalysisSoftware();
		
		// TODO: add more software
		if (node.getNodeName().equals("SequestNode")) {
			software.setId("sequest");
			software.setName("SEQUEST");
			
			CvParam cvParam = new CvParam();
			cvParam.setAccession("MS:1001208");
			cvParam.setCv(psiMS);
			cvParam.setName("Sequest");
			
			Param param = new Param();
			param.setParam(cvParam);
			
			software.setSoftwareName(param);
			
			logger.debug("added Sequest as software");
		} else if (node.getNodeName().equals("Mascot")) {
			software.setId("mascot");
			software.setName("mascot");
			software.setUri("http://www.matrixscience.com/");
			
			CvParam cvParam = new CvParam();
			((CvParam)cvParam).setAccession("MS:1001207");
			((CvParam)cvParam).setCv(psiMS);
			cvParam.setName("Mascot");
			
			Param param = new Param();
			param.setParam(cvParam);
			
			software.setSoftwareName(param);
			
			logger.debug("added Mascot as software");
		} else {
			// unknown software: return null
			return null;
		}
		
		return software;
	}
	
	
	
	private static SearchModification createModification(String modString,
			boolean isFixed, Cv psiMS) {
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
				specificity.setCv(psiMS);
				if (res.contains("N-Term")) {
					specificity.setAccession(PIAConstants.CV_MODIFICATION_SPECIFICITY_N_TERM_ACCESSION);
					specificity.setName(PIAConstants.CV_MODIFICATION_SPECIFICITY_N_TERM_NAME);
				} else {
					specificity.setAccession(PIAConstants.CV_MODIFICATION_SPECIFICITY_C_TERM_ACCESSION);
					specificity.setName(PIAConstants.CV_MODIFICATION_SPECIFICITY_C_TERM_NAME);
				}
				SpecificityRules specRules = new SpecificityRules();
				specRules.getCvParam().add(specificity);
				searchMod.getSpecificityRules().add(specRules);
			} else {
				searchMod.getResidues().add(res);
			}
		}
		
		/*
		if (values[1].equals("[") || values[1].equals("]")) {
			searchMod.getResidues().add(".");
			
			CvParam  specificity = new CvParam();
			specificity.setCv(psiMS);
			if (values[1].equals("[")) {
				specificity.setAccession(PIAConstants.CV_MODIFICATION_SPECIFICITY_N_TERM_ACCESSION);
				specificity.setName(PIAConstants.CV_MODIFICATION_SPECIFICITY_N_TERM_NAME);
			} else {
				specificity.setAccession(PIAConstants.CV_MODIFICATION_SPECIFICITY_C_TERM_ACCESSION);
				specificity.setName(PIAConstants.CV_MODIFICATION_SPECIFICITY_C_TERM_NAME);
			}
			SpecificityRules specRules = new SpecificityRules();
			specRules.getCvParam().add(specificity);
			searchMod.getSpecificityRules().add(specRules);
		} else {
			searchMod.getResidues().add(values[1]);
		}
		*/
		
		return searchMod;
	}
}
