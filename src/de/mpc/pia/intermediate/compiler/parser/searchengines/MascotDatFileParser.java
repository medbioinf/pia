package de.mpc.pia.intermediate.compiler.parser.searchengines;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.compomics.mascotdatfile.util.interfaces.MascotDatfileInf;
import com.compomics.mascotdatfile.util.interfaces.QueryToPeptideMapInf;
import com.compomics.mascotdatfile.util.mascot.FixedModification;
import com.compomics.mascotdatfile.util.mascot.PeptideHit;
import com.compomics.mascotdatfile.util.mascot.ProteinHit;
import com.compomics.mascotdatfile.util.mascot.ProteinMap;
import com.compomics.mascotdatfile.util.mascot.Query;
import com.compomics.mascotdatfile.util.mascot.VariableModification;
import com.compomics.mascotdatfile.util.mascot.enumeration.MascotDatfileType;
import com.compomics.mascotdatfile.util.mascot.factory.MascotDatfileFactory;
import com.compomics.mascotdatfile.util.mascot.iterator.QueryEnumerator;

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
import de.mpc.pia.tools.unimod.UnimodParser;
import de.mpc.pia.tools.unimod.jaxb.ModT;


/**
 * This class parses the data from a Mascot DAT file for a given
 * {@link PIACompiler}.<br/>
 * 
 * @author julian
 *
 */
public class MascotDatFileParser {
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(MascotDatFileParser.class);
	
	
	/**
	 * We don't ever want to instantiate this class
	 */
	private MascotDatFileParser() {
		throw new AssertionError();
	}
	
	
	/**
	 * Parses the data from an mzIdentML file given by its name into the given
	 * {@link PIACompiler}.
	 * 
	 * @param fileName name of the mzTab file
	 */
	public static boolean getDataFromMascotDatFile(String name, String fileName,
			PIACompiler compiler) {
		
		// need to parse through the file, as mascotdatfile (3.2.11) does not support
		//   - the "index" variable of the queries
		//   - the "fastafile"
		//   - no good information for enzyme
		Map<String, String> queryIndexMap = new HashMap<String, String>();
		String fastaFile = null;
		
		String enzymeCleavage = null;
		String enzymeRestrict = null;
		
		try {
			BufferedReader rd;
			rd = new BufferedReader(new FileReader(fileName));
			String line;
			
			boolean inQuery = false;
			boolean inEnzyme = false;
			String queryName = null;
			
			while ((line = rd.readLine()) != null) {
				if (!inQuery) {
					if (line.startsWith("Content-Type: application/x-Mascot; name=\"query")) {
						queryName = line.substring(42, line.length()-1);
						inQuery = true;
					} else if ((fastaFile == null) &&
							line.startsWith("fastafile")) {
						fastaFile = line.substring(10);
					}
				} else if (inQuery &&
						line.startsWith("index=")) {
					queryIndexMap.put(queryName, line);
					inQuery = false;
				}
				
				if (!inEnzyme) {
					if (((enzymeCleavage == null) || (enzymeRestrict == null)) &&
							line.startsWith("Content-Type: application/x-Mascot; name=\"enzyme\"")) {
						inEnzyme = true;
					}
				} else {
					if (line.startsWith("Cleavage:")) {
						enzymeCleavage = line.substring(9).trim();
					} else if (line.startsWith("Restrict:")) {
						enzymeRestrict = line.substring(9).trim();
					} else if (line.startsWith("Content-Type:")) {
						inEnzyme = false;
					}
				}
			}
			
			rd.close();
		} catch (IOException e) {
			logger.error("could not read '" + fileName + "' for index parsing.");
			return false;
		}
		
		
		MascotDatfileInf mascotFile = 
				MascotDatfileFactory.create(fileName, MascotDatfileType.MEMORY);
		
		if (mascotFile == null) {
			// TODO: better error / exception
			logger.error("could not read '" + fileName + "'.");
			return false;
		}
		
		PIAInputFile file = compiler.insertNewFile(name, fileName,
				InputFileParserFactory.InputFileTypes.MASCOT_DAT_INPUT.getFileSuffix());
		
		Cv psiMS = new Cv();
		psiMS.setId("PSI-MS");
		psiMS.setFullName("PSI-MS");
		psiMS.setUri("http://psidev.cvs.sourceforge.net/viewvc/*checkout*/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo");
		
		// create the analysis software and add it to the compiler
		AnalysisSoftware mascot = new AnalysisSoftware();
		
		mascot.setId("mascot");
		mascot.setName("mascot");
		mascot.setUri("http://www.matrixscience.com/");
		mascot.setVersion(mascotFile.getHeaderSection().getVersion());
		
		AbstractParam abstractParam; 
		Param param = new Param();
		abstractParam = new CvParam();
		((CvParam)abstractParam).setAccession("MS:1001207");
		((CvParam)abstractParam).setCv(psiMS);
		abstractParam.setName("Mascot");
		param.setParam(abstractParam);
		mascot.setSoftwareName(param);
		
		compiler.putIntoSoftwareMap(mascot);
		
		
		// create the searchDatabase and add it to the compiler
		SearchDatabase searchDatabase = new SearchDatabase();
		
		// required
		searchDatabase.setId("mascotDB");
		searchDatabase.setLocation(fastaFile);
		// optional
		searchDatabase.setName(mascotFile.getParametersSection().getDatabase());
		searchDatabase.setNumDatabaseSequences(mascotFile.getHeaderSection().getSequences());
		searchDatabase.setNumResidues(mascotFile.getHeaderSection().getResidues());
		
		// fileformat
		FileFormat fileFormat = new FileFormat();
		abstractParam = new CvParam();
		((CvParam)abstractParam).setAccession("MS:1001348");
		((CvParam)abstractParam).setCv(psiMS);
		abstractParam.setName("FASTA format");
		fileFormat.setCvParam((CvParam)abstractParam);
		searchDatabase.setFileFormat(fileFormat);
		// databaseName
		param = new Param();
		abstractParam = new UserParam();
		abstractParam.setName(mascotFile.getHeaderSection().getRelease());
		param.setParam(abstractParam);
		searchDatabase.setDatabaseName(param);
		
		// add searchDB to the compiler
		searchDatabase = compiler.putIntoSearchDatabasesMap(searchDatabase);
		
		
		// add the spectraData (input file)
		SpectraData spectraData = null;
		if ((mascotFile.getParametersSection().getFile() != null) &&
				(mascotFile.getParametersSection().getFile().trim().length() > 0)) {
			spectraData = new SpectraData();
			
			spectraData.setId("mascotInput");
			spectraData.setLocation(mascotFile.getParametersSection().getFile());
			
			if ((mascotFile.getParametersSection().getFormat() != null) &&
					mascotFile.getParametersSection().getFormat().equals("Mascot generic")) {
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
			}
			
			spectraData = compiler.putIntoSpectraDataMap(spectraData);
		} else {
			logger.warn("The source file (MGF) was not recorded in the file!");
		}
		
		// define the spectrumIdentificationProtocol
		SpectrumIdentificationProtocol spectrumIDProtocol =
				new SpectrumIdentificationProtocol();
		
		spectrumIDProtocol.setId("mascotAnalysis");
		spectrumIDProtocol.setAnalysisSoftware(mascot);
		
		param = new Param();
		if (mascotFile.getParametersSection().getSearch().equals("MIS")) {
			abstractParam = new CvParam();
			((CvParam)abstractParam).setAccession("MS:1001083");
			((CvParam)abstractParam).setCv(psiMS);
			abstractParam.setName("ms-ms search");
			param.setParam(abstractParam);
		}
		// TODO: add error on PMF query (not usable for PIA)
		// TODO: and sequence query
		spectrumIDProtocol.setSearchType(param);
		
		ParamList paramList = new ParamList();
		abstractParam = new CvParam();
		((CvParam)abstractParam).setAccession("MS:1001656");
		((CvParam)abstractParam).setCv(psiMS);
		abstractParam.setName("Mascot:Instrument");
		abstractParam.setValue(mascotFile.getParametersSection().getInstrument());
		paramList.getCvParam().add((CvParam)abstractParam);
		
		abstractParam = new UserParam();
		abstractParam.setName("Mascot User Comment");
		abstractParam.setValue(mascotFile.getParametersSection().getCom());
		paramList.getUserParam().add((UserParam)abstractParam);
		
		if (mascotFile.getParametersSection().getMass().
				equalsIgnoreCase("Monoisotopic")) {
			abstractParam = new CvParam();
    		((CvParam)abstractParam).setAccession("MS:1001256");
    		((CvParam)abstractParam).setCv(psiMS);
    		abstractParam.setName("fragment mass type mono");
    		paramList.getCvParam().add((CvParam)abstractParam);
    		
			abstractParam = new CvParam();
    		((CvParam)abstractParam).setAccession("MS:1001211");
    		((CvParam)abstractParam).setCv(psiMS);
    		abstractParam.setName("parent mass type mono");
    		paramList.getCvParam().add((CvParam)abstractParam);
        } else {
    		abstractParam = new CvParam();
    		((CvParam)abstractParam).setAccession("MS:1001255");
    		((CvParam)abstractParam).setCv(psiMS);
    		abstractParam.setName("fragment mass type average");
    		paramList.getCvParam().add((CvParam)abstractParam);
    		
			abstractParam = new CvParam();
    		((CvParam)abstractParam).setAccession("MS:1001212");
    		((CvParam)abstractParam).setCv(psiMS);
    		abstractParam.setName("parent mass type average");
    		paramList.getCvParam().add((CvParam)abstractParam);
        }
		
		spectrumIDProtocol.setAdditionalSearchParams(paramList);
		
		
		ModificationParams modParams = new ModificationParams();
		SearchModification searchMod;
		for (Object objMod : mascotFile.getModificationList().getVariableModifications()) {
			if (objMod instanceof VariableModification) {
				VariableModification mod = (VariableModification)objMod;
				
				searchMod = new SearchModification();
				searchMod.setFixedMod(false);
				
				if (mod.getLocation().equalsIgnoreCase("N-Term") || mod.getLocation().equalsIgnoreCase("Protein N-Term") || 
						mod.getLocation().equalsIgnoreCase("C-Term") || mod.getLocation().equalsIgnoreCase("Protein C-Term")) {
					CvParam  specificity = new CvParam();
					specificity.setCv(psiMS);
					
					if (mod.getLocation().equalsIgnoreCase("N-Term")) {
						specificity.setAccession(PIAConstants.CV_MODIFICATION_SPECIFICITY_PEP_N_TERM_ACCESSION);
						specificity.setName(PIAConstants.CV_MODIFICATION_SPECIFICITY_PEP_N_TERM_NAME);
					} else if (mod.getLocation().equalsIgnoreCase("Protein N-Term")) {
						specificity.setAccession(PIAConstants.CV_MODIFICATION_SPECIFICITY_PROTEIN_N_TERM_ACCESSION);
						specificity.setName(PIAConstants.CV_MODIFICATION_SPECIFICITY_PROTEIN_N_TERM_NAME);
					} else if (mod.getLocation().equalsIgnoreCase("C-Term")) {
						specificity.setAccession(PIAConstants.CV_MODIFICATION_SPECIFICITY_PEP_C_TERM_ACCESSION);
						specificity.setName(PIAConstants.CV_MODIFICATION_SPECIFICITY_PEP_C_TERM_NAME);
					} else if (mod.getLocation().equalsIgnoreCase("Protein C-Term")) {
						specificity.setAccession(PIAConstants.CV_MODIFICATION_SPECIFICITY_PROTEIN_C_TERM_ACCESSION);
						specificity.setName(PIAConstants.CV_MODIFICATION_SPECIFICITY_PROTEIN_C_TERM_NAME);
					}
					
					SpecificityRules specRules = new SpecificityRules();
					specRules.getCvParam().add(specificity);
					searchMod.getSpecificityRules().add(specRules);
					
					searchMod.getResidues().add(".");
				} else {
					for (Character residue : mod.getLocation().toCharArray()) {
						searchMod.getResidues().add(residue.toString());
					}
				}
				searchMod.setMassDelta((float)mod.getMass());
				
				ModT unimod =
						compiler.getUnimodParser().getModificationByNameAndMass(
								mod.getType(),
								mod.getMass(),
								searchMod.getResidues());
				if (unimod != null) {
					CvParam cvParam = new CvParam();
					cvParam.setAccession("UNIMOD:" + unimod.getRecordId());
					cvParam.setCv(UnimodParser.getCv());
					cvParam.setName(unimod.getTitle());
					searchMod.getCvParam().add(cvParam);
				}
				
				modParams.getSearchModification().add(searchMod);
			}
		}
		for (Object objMod : mascotFile.getModificationList().getFixedModifications()) {
			if (objMod instanceof FixedModification) {
				FixedModification mod = (FixedModification)objMod;
				
				searchMod = new SearchModification();
				searchMod.setFixedMod(true);
				
				if (mod.getLocation().equalsIgnoreCase("N-Term") || mod.getLocation().equalsIgnoreCase("Protein N-Term") || 
						mod.getLocation().equalsIgnoreCase("C-Term") || mod.getLocation().equalsIgnoreCase("Protein C-Term")) {
					CvParam  specificity = new CvParam();
					specificity.setCv(psiMS);
					
					if (mod.getLocation().equalsIgnoreCase("N-Term")) {
						specificity.setAccession(PIAConstants.CV_MODIFICATION_SPECIFICITY_PEP_N_TERM_ACCESSION);
						specificity.setName(PIAConstants.CV_MODIFICATION_SPECIFICITY_PEP_N_TERM_NAME);
					} else if (mod.getLocation().equalsIgnoreCase("Protein N-Term")) {
						specificity.setAccession(PIAConstants.CV_MODIFICATION_SPECIFICITY_PROTEIN_N_TERM_ACCESSION);
						specificity.setName(PIAConstants.CV_MODIFICATION_SPECIFICITY_PROTEIN_N_TERM_NAME);
					} else if (mod.getLocation().equalsIgnoreCase("C-Term")) {
						specificity.setAccession(PIAConstants.CV_MODIFICATION_SPECIFICITY_PEP_C_TERM_ACCESSION);
						specificity.setName(PIAConstants.CV_MODIFICATION_SPECIFICITY_PEP_C_TERM_NAME);
					} else if (mod.getLocation().equalsIgnoreCase("Protein C-Term")) {
						specificity.setAccession(PIAConstants.CV_MODIFICATION_SPECIFICITY_PROTEIN_C_TERM_ACCESSION);
						specificity.setName(PIAConstants.CV_MODIFICATION_SPECIFICITY_PROTEIN_C_TERM_NAME);
					}
					
					SpecificityRules specRules = new SpecificityRules();
					specRules.getCvParam().add(specificity);
					searchMod.getSpecificityRules().add(specRules);
					
					searchMod.getResidues().add(".");
				} else {
					for (Character residue : mod.getLocation().toCharArray()) {
						searchMod.getResidues().add(residue.toString());
					}
				}
				
				searchMod.setMassDelta((float)mod.getMass());
				
				ModT unimod =
						compiler.getUnimodParser().getModificationByNameAndMass(
								mod.getType(),
								mod.getMass(),
								searchMod.getResidues());
				if (unimod != null) {
					CvParam cvParam = new CvParam();
					cvParam.setAccession("UNIMOD:" + unimod.getRecordId());
					cvParam.setCv(UnimodParser.getCv());
					cvParam.setName(unimod.getTitle());
					searchMod.getCvParam().add(cvParam);
				}
				
				modParams.getSearchModification().add(searchMod);
			}
		}
		spectrumIDProtocol.setModificationParams(modParams);
		
		Enzymes enzymes = new Enzymes();
		spectrumIDProtocol.setEnzymes(enzymes);
		if (enzymeCleavage != null) {
			Enzyme enzyme = new Enzyme();
			
			enzyme.setId("enzyme");
			enzyme.setMissedCleavages(
					Integer.parseInt(
							mascotFile.getParametersSection().getPFA()));
			
			StringBuilder regExp = new StringBuilder();
			if (enzymeRestrict == null) {
				regExp.append("(?=[");
				regExp.append(enzymeCleavage);
				regExp.append("])");
			} else {
				regExp.append("(?<=[");
				regExp.append(enzymeCleavage);
				regExp.append("])(?!");
				regExp.append(enzymeRestrict);
				regExp.append(")");
			}
			enzyme.setSiteRegexp(regExp.toString());
			
			enzymes.getEnzyme().add(enzyme);
		}
		
		Tolerance tolerance = new Tolerance();
		
		abstractParam = new CvParam();
		((CvParam)abstractParam).setAccession("MS:1001412");
		((CvParam)abstractParam).setCv(psiMS);
		abstractParam.setName("search tolerance plus value");
		abstractParam.setValue(mascotFile.getParametersSection().getITOL());
		MzIdentMLTools.setUnitParameterFromString(
				mascotFile.getParametersSection().getITOLU(), abstractParam);
		tolerance.getCvParam().add((CvParam)abstractParam);
		
		abstractParam = new CvParam();
		((CvParam)abstractParam).setAccession("MS:1001413");
		((CvParam)abstractParam).setCv(psiMS);
		abstractParam.setName("search tolerance minus value");
		abstractParam.setValue(mascotFile.getParametersSection().getITOL());
		MzIdentMLTools.setUnitParameterFromString(
				mascotFile.getParametersSection().getITOLU(), abstractParam);
		tolerance.getCvParam().add((CvParam)abstractParam);
		
		spectrumIDProtocol.setFragmentTolerance(tolerance);
		
		
		tolerance = new Tolerance();
		
		abstractParam = new CvParam();
		((CvParam)abstractParam).setAccession("MS:1001412");
		((CvParam)abstractParam).setCv(psiMS);
		abstractParam.setName("search tolerance plus value");
		abstractParam.setValue(mascotFile.getParametersSection().getTOL());
		MzIdentMLTools.setUnitParameterFromString(
				mascotFile.getParametersSection().getTOLU(), abstractParam);
		tolerance.getCvParam().add((CvParam)abstractParam);
		
		abstractParam = new CvParam();
		((CvParam)abstractParam).setAccession("MS:1001413");
		((CvParam)abstractParam).setCv(psiMS);
		abstractParam.setName("search tolerance minus value");
		abstractParam.setValue(mascotFile.getParametersSection().getTOL());
		MzIdentMLTools.setUnitParameterFromString(
				mascotFile.getParametersSection().getTOLU(), abstractParam);
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
		
		if (spectraData != null) {
			InputSpectra inputSpectra = new InputSpectra();
			inputSpectra.setSpectraData(spectraData);
			spectrumID.getInputSpectra().add(inputSpectra);
		}
		
		SearchDatabaseRef searchDBRef = new SearchDatabaseRef();
		searchDBRef.setSearchDatabase(searchDatabase);
		spectrumID.getSearchDatabaseRef().add(searchDBRef);
		
		file.addSpectrumIdentification(spectrumID);
		
		
		// get the mappings
		QueryEnumerator queryEnumerator = mascotFile.getQueryEnumerator();
		QueryToPeptideMapInf queryToPeptideMap = mascotFile.getQueryToPeptideMap();
		QueryToPeptideMapInf decoyQueryToPeptideMap = mascotFile.getDecoyQueryToPeptideMap();
		ProteinMap proteinMap = mascotFile.getProteinMap();
		ProteinMap decoyProteinMap = mascotFile.getDecoyProteinMap();
		
		// one query is one spectrum, so go through the queries
		Query currQuery;
		while (queryEnumerator.hasMoreElements()) {
			currQuery = queryEnumerator.nextElement();
			
			int charge;
			if (currQuery.getChargeString() == null) {
				charge = 0;
			} else if (currQuery.getChargeString().contains("-")) {
				charge = -Integer.parseInt(currQuery.getChargeString().replace("-", ""));
			} else {
				// we assume, it is positively charged
				charge = Integer.parseInt(currQuery.getChargeString().replace("+", ""));
			}
			
			double precursorMZ = currQuery.getPrecursorMZ();
			
			Double retentionTime;
			if (currQuery.getRetentionTimeInSeconds() != null) {
				retentionTime =	Double.parseDouble(currQuery.getRetentionTimeInSeconds());
			} else {
				retentionTime = null;
			}
			
			String spectrumTitle = currQuery.getTitle();
			String index = queryIndexMap.get("query"+currQuery.getQueryNumber());
			
			// add the target identifications
			if (queryToPeptideMap != null) {
				@SuppressWarnings("unchecked")
				Vector<PeptideHit> peptideHits =
						queryToPeptideMap.getAllPeptideHits(currQuery.getQueryNumber());
				insertPeptideHitsIntoCompiler(compiler, peptideHits, proteinMap,
						searchDatabase, charge, precursorMZ, retentionTime,
						index, spectrumTitle, file, spectrumID, false);
			}
			
			// add the decoy identifications
			if (decoyQueryToPeptideMap != null) {
				@SuppressWarnings("unchecked")
				Vector<PeptideHit> peptideHits =
						decoyQueryToPeptideMap.getAllPeptideHits(currQuery.getQueryNumber());
				insertPeptideHitsIntoCompiler(compiler, peptideHits,
						decoyProteinMap, searchDatabase, charge, precursorMZ,
						retentionTime, index, spectrumTitle, file, spectrumID,
						true);
			}
		}
		
		return true;
	}
	
	
	private static int insertPeptideHitsIntoCompiler(PIACompiler compiler,
			Vector<PeptideHit> peptideHits, ProteinMap proteinMap,
			SearchDatabase searchDatabase, int charge, Double precursorMZ,
			Double retentionTime, String index, String spectrumTitle,
			PIAInputFile file, SpectrumIdentification spectrumID,
			boolean isDecoy) {
		if (peptideHits == null) {
			return 0;
		}
		
		int nrPepHits = 0;
		
		// the peptideHits are the SpectrumPeptideMatches
		for (int i=0; i < peptideHits.size(); i++) {
			PeptideHit peptideHit = peptideHits.get(i);
			
			PeptideSpectrumMatch psm;
			psm = compiler.insertNewSpectrum(
					charge,
					precursorMZ,
					peptideHit.getDeltaMass(),
					retentionTime,
					peptideHit.getSequence(),
					peptideHit.getMissedCleavages(),
					index,
					spectrumTitle,
					file,
					spectrumID);
			
			psm.setIsDecoy(isDecoy);
			
			// get the peptide from the compiler or, if need be, add it 
			Peptide peptide;
			peptide = compiler.getPeptide(peptideHit.getSequence());
			if (peptide == null) {
				peptide = compiler.insertNewPeptide(peptideHit.getSequence());
			}
			
			// add the spectrum to the peptide
			peptide.addSpectrum(psm);
			
			// go through the protein hits
			@SuppressWarnings("unchecked")
			List<ProteinHit> proteins = peptideHit.getProteinHits();
			for (ProteinHit proteinHit : proteins) {
				
				FastaHeaderInfos fastaInfo =
						FastaHeaderInfos.parseHeaderInfos(proteinHit.getAccession());
				
				if (fastaInfo == null) {
					// might be already parsed, take the mascot infos
					fastaInfo = new FastaHeaderInfos(null,
							proteinHit.getAccession(),
							proteinMap.getProteinID(proteinHit.getAccession()).getDescription());
					continue;
				} else {
					// if there was a protein description different to the now parsed one, take the original from mascot
					String proteinDescription = proteinMap.
							getProteinID(proteinHit.getAccession()).
							getDescription();
					if ((proteinDescription != null) &&
							(proteinDescription.trim().length() > 0) && 
							!proteinDescription.equals(fastaInfo.getDescription())) {
						fastaInfo = new FastaHeaderInfos(null,
								fastaInfo.getAccession(),
								proteinDescription);
					}
				}
				
				// add the Accession to the compiler (if it is not already there)
				Accession acc = compiler.getAccession(fastaInfo.getAccession());
				if (acc == null) {
					// unfortunately, the sequence is not stored in the dat file
					acc = compiler.insertNewAccession(
							fastaInfo.getAccession(), null);
				}
				
				acc.addFile(file.getID());
				
				if ((fastaInfo.getDescription() != null) &&
						(fastaInfo.getDescription().length() > 0)) {
					acc.addDescription(file.getID(),
							fastaInfo.getDescription());
				}
				
				acc.addSearchDatabaseRef(searchDatabase.getId());
				
				// add the accession occurrence to the peptide
				peptide.addAccessionOccurrence(acc,
						proteinHit.getStart(), proteinHit.getStop());
				
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
			
			
			// add the scores
			ScoreModel score;
			
			score = new ScoreModel(peptideHit.getIonsScore(),
					ScoreModelEnum.MASCOT_SCORE);
			psm.addScore(score);
			
			score = new ScoreModel(peptideHit.getExpectancy(),
					ScoreModelEnum.MASCOT_EXPECT);
			psm.addScore(score);
			
			// add the modifications
			com.compomics.mascotdatfile.util.interfaces.Modification
					mods[] = peptideHit.getModifications();
			for (int loc=0; loc < mods.length; loc++) {
				if (mods[loc] != null) {
					Modification modification;
					
					Character residue = null;
					if ((loc == 0) || (loc > psm.getSequence().length())) {
						residue = '.';
					} else {
						residue = psm.getSequence().charAt(loc-1);
					}
					
					modification = new Modification(
							residue,
							mods[loc].getMass(),
							mods[loc].getType(),
							null);
					// TODO: get the unimod modification code
					psm.addModification(loc, modification);
				}
			}
			
			nrPepHits++;
		}
		
		return nrPepHits;
	}
}
