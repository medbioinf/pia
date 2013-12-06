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
import uk.ac.ebi.jmzidml.model.mzidml.Param;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabase;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabaseRef;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;
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
		Map<Integer, AnalysisSoftware> nodeNumbersToSoftwares =
				new HashMap<Integer, AnalysisSoftware>();
		
		for (ProcessingNode node : thermoParser.getProcessingNodes()) {
			AnalysisSoftware software = createAnalysisSoftware(
					node.getFriendlyName(), psiMS);
			
			if (software != null) {
				// add the software
				nodeNumbersToSoftwares.put(node.getProcessingNodeNumber(),
						compiler.putIntoSoftwareMap(software));
				
				// get all additional data
				SearchDatabase searchDatabase = null;
				
				for (ProcessingNodeParameter processingParam
						: node.getProcessingNodeParameters()) {
					
					if (processingParam.getFriendlyName().equals("Protein Database")) {
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
					}
					
				}
				
				// add the spectrum identification
				SpectrumIdentification spectrumID = new SpectrumIdentification();
				spectrumID.setId("node" + node.getProcessingNodeNumber() + "Identification");
				/*
				TODO: add these information!
				
				spectrumID.setSpectrumIdentificationList(null);
				spectrumID.setSpectrumIdentificationProtocol(spectrumIDProtocol);
				
				InputSpectra inputSpectra = new InputSpectra();
				inputSpectra.setSpectraData(spectraData);
				spectrumID.getInputSpectra().add(inputSpectra);
				*/
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
			
			
			file.addSpectrumIdentification(idIt.getValue());
			
			nodeNumbersToInputFiles.put(idIt.getKey(), file);
		}
	
		/*

		// TODO: add refinement (modifications and all other stuff
		
		InputParams inputParams = xtandemFile.getInputParameters();
		
		// add the spectraData (input file)
		SpectraData spectraData = new SpectraData();
		
		spectraData.setId("tandemInputMGF");
		spectraData.setLocation(inputParams.getSpectrumPath());
		// TODO: for now write MGF, though it could be mzML as well
		fileFormat = new FileFormat();
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
		
		spectraData = compiler.putIntoSpectraDataMap(spectraData);
		
		
		// define the spectrumIdentificationProtocol
		SpectrumIdentificationProtocol spectrumIDProtocol =
				new SpectrumIdentificationProtocol();
		
		spectrumIDProtocol.setId("tandemAnalysis");
		spectrumIDProtocol.setAnalysisSoftware(tandem);
		
		param = new Param();
		// TODO: check, whether tandem can anything else than ms/ms (guess not... hence the name)
		abstractParam = new CvParam();
		((CvParam)abstractParam).setAccession("MS:1001083");
		((CvParam)abstractParam).setCv(psiMS);
		abstractParam.setName("ms-ms search");
		param.setParam(abstractParam);
		spectrumIDProtocol.setSearchType(param);
		
		ParamList paramList = new ParamList();
		abstractParam = new CvParam();
        // does not appear to be a way in Tandem of specifying parent mass is average
		((CvParam)abstractParam).setAccession("MS:1001211");
		((CvParam)abstractParam).setCv(psiMS);
		abstractParam.setName("parent mass type mono");
		paramList.getCvParam().add((CvParam)abstractParam);
		
		abstractParam = new CvParam();
		boolean fragmentMonoisotopic;
		if (inputParams.getSpectrumFragMassType().
				equals("monoisotopic")) {
    		((CvParam)abstractParam).setAccession("MS:1001256");
    		((CvParam)abstractParam).setCv(psiMS);
    		abstractParam.setName("fragment mass type mono");
    		fragmentMonoisotopic = true;
        } else {
    		((CvParam)abstractParam).setAccession("MS:1001255");
    		((CvParam)abstractParam).setCv(psiMS);
    		abstractParam.setName("fragment mass type average");
    		fragmentMonoisotopic = false;
        }
		paramList.getCvParam().add((CvParam)abstractParam);
		
		spectrumIDProtocol.setAdditionalSearchParams(paramList);
		
		
		ModificationParams modParams = new ModificationParams();
		SearchModification searchMod;
		
		strParam = inputParams.getResiduePotModMass();
		if (strParam != null) {
			// variable modifications (w/o refinement)
			String varMods[] = strParam.split(",");
			
			for(String varMod : varMods) {
				if (!varMod.equalsIgnoreCase("None")) {
					
		            String[] values = varMod.split("@");
		            
					searchMod = new SearchModification();
					searchMod.setFixedMod(false);
					searchMod.setMassDelta((float)Float.parseFloat(values[0]));
					searchMod.getResidues().add(values[1]);
					
					// TODO: add the cvParam for the modification
					
					modParams.getSearchModification().add(searchMod);
				}
			}
		}
		// TODO: add fixed modifications
		spectrumIDProtocol.setModificationParams(modParams);
		
		
		Enzymes enzymes = new Enzymes();
		Enzyme enzyme = new Enzyme();
		
		enzyme.setId("enzyme");
		enzyme.setMissedCleavages(inputParams.getScoringMissCleavageSites());
		
		strParam = inputParams.getProteinCleavageSite();
		if (strParam != null) {
			enzyme.setSiteRegexp(strParam);
			
			String cleavages[] = strParam.split(",");
			
			if (cleavages.length > 1) {
				logger.warn("Only one enzyme (cleavage site) implemented yet.");
			}
			
			if (cleavages.length > 0) {
				strParam = cleavages[0];
				
				
				String values[] = strParam.split("\\|");
				String pre = values[0].substring(1, values[0].length() - 1);
				if (pre.equalsIgnoreCase("X")) {
					// X stands for any, make it \S in PCRE for "not whitespace"
					pre = "\\S";
				}
				if (values[0].startsWith("[") && values[0].endsWith("]")) {
					pre = "(?<=" + pre + ")";
				} else if (values[0].startsWith("{") && values[0].endsWith("}")) {
					pre = "(?<!" + pre + ")";
				}
				
				String post = values[1].substring(1, values[1].length() - 1);
				if (post.equalsIgnoreCase("X")) {
					// X stands for any, make it \S in PCRE for "not whitespace"
					post = "\\S";
				}
				if (values[1].startsWith("[") && values[1].endsWith("]")) {
					post = "(?=" + post + ")";
				} else if (values[1].startsWith("{") && values[1].endsWith("}")) {
					post = "(?!" + post + ")";
				}
				
				enzyme.setSiteRegexp(pre + post);
			} else {
				logger.error("No cleavage site found!");
			}
		}
		
		enzymes.getEnzyme().add(enzyme);
		spectrumIDProtocol.setEnzymes(enzymes);
		
		
		Tolerance tolerance = new Tolerance();
		
		Double error;
		String units;
		if (fragmentMonoisotopic) {
			error = inputParams.getSpectrumMonoIsoMassError();
			units = inputParams.getSpectrumMonoIsoMassErrorUnits();
		} else {
			// TODO: implement average fragment mass
			error = null;
			units = null;
		}
		
		abstractParam = new CvParam();
		((CvParam)abstractParam).setAccession("MS:1001412");
		((CvParam)abstractParam).setCv(psiMS);
		abstractParam.setName("search tolerance plus value");
		abstractParam.setValue(error.toString());
		abstractParam.setUnitName(units);
		tolerance.getCvParam().add((CvParam)abstractParam);
		
		abstractParam = new CvParam();
		((CvParam)abstractParam).setAccession("MS:1001413");
		((CvParam)abstractParam).setCv(psiMS);
		abstractParam.setName("search tolerance minus value");
		abstractParam.setValue(error.toString());
		abstractParam.setUnitName(units);
		tolerance.getCvParam().add((CvParam)abstractParam);
		
		spectrumIDProtocol.setFragmentTolerance(tolerance);
		
		
		tolerance = new Tolerance();
		
		abstractParam = new CvParam();
		((CvParam)abstractParam).setAccession("MS:1001412");
		((CvParam)abstractParam).setCv(psiMS);
		abstractParam.setName("search tolerance plus value");
		abstractParam.setValue(
				String.valueOf(inputParams.getSpectrumParentMonoIsoMassErrorPlus()));
		abstractParam.setUnitName(inputParams.getSpectrumParentMonoIsoMassErrorUnits());
		tolerance.getCvParam().add((CvParam)abstractParam);
		
		abstractParam = new CvParam();
		((CvParam)abstractParam).setAccession("MS:1001413");
		((CvParam)abstractParam).setCv(psiMS);
		abstractParam.setName("search tolerance minus value");
		abstractParam.setValue(
				String.valueOf(inputParams.getSpectrumParentMonoIsoMassErrorMinus()));
		abstractParam.setUnitName(inputParams.getSpectrumParentMonoIsoMassErrorUnits());
		tolerance.getCvParam().add((CvParam)abstractParam);
		
		spectrumIDProtocol.setParentTolerance(tolerance);
		
		// no threshold set, take all PSMs from the dat file
		paramList = new ParamList();
		abstractParam = new CvParam();
		((CvParam)abstractParam).setAccession("MS:1001494");
		((CvParam)abstractParam).setCv(psiMS);
		abstractParam.setName("no threshold");
		paramList.getCvParam().add((CvParam)abstractParam);
		spectrumIDProtocol.setThreshold(paramList);
		
		
		file.addSpectrumIdentificationProtocol(spectrumIDProtocol);
		
		
		// add the spectrum identification
		SpectrumIdentification spectrumID = new SpectrumIdentification();
		spectrumID.setId("mascotIdentification");
		spectrumID.setSpectrumIdentificationList(null);
		spectrumID.setSpectrumIdentificationProtocol(spectrumIDProtocol);
		
		InputSpectra inputSpectra = new InputSpectra();
		inputSpectra.setSpectraData(spectraData);
		spectrumID.getInputSpectra().add(inputSpectra);
		
		SearchDatabaseRef searchDBRef;
		for (SearchDatabase sDB : searchDatabaseMap.values()) {
			searchDBRef = new SearchDatabaseRef();
			searchDBRef.setSearchDatabase(sDB);
			spectrumID.getSearchDatabaseRef().add(searchDBRef);
		}
		
		file.addSpectrumIdentification(spectrumID);
		
		// TODO: add heaps of other settings...
		*/
		
		
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
	static AnalysisSoftware createAnalysisSoftware(String friendlyName, Cv psiMS) {
		AnalysisSoftware software = new AnalysisSoftware();
		
		// TODO: add more software
		if (friendlyName.equals("SEQUEST")) {
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
		} else if (friendlyName.equals("Mascot")) {
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
}
