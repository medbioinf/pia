package de.mpc.pia.intermediate.compiler.parser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import uk.ac.ebi.jmzidml.model.mzidml.AbstractParam;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.Cv;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.Enzyme;
import uk.ac.ebi.jmzidml.model.mzidml.Enzymes;
import uk.ac.ebi.jmzidml.model.mzidml.ModificationParams;
import uk.ac.ebi.jmzidml.model.mzidml.Param;
import uk.ac.ebi.jmzidml.model.mzidml.ParamList;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabase;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabaseRef;
import uk.ac.ebi.jmzidml.model.mzidml.SearchModification;
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
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.tools.openms.IdXMLParser;
import de.mpc.pia.tools.openms.jaxb.FixedModification;
import de.mpc.pia.tools.openms.jaxb.IdentificationRun;
import de.mpc.pia.tools.openms.jaxb.MassType;
import de.mpc.pia.tools.openms.jaxb.PeptideHit;
import de.mpc.pia.tools.openms.jaxb.PeptideIdentification;
import de.mpc.pia.tools.openms.jaxb.ProteinHit;
import de.mpc.pia.tools.openms.jaxb.SearchParameters;
import de.mpc.pia.tools.openms.jaxb.UserParamType;
import de.mpc.pia.tools.openms.jaxb.VariableModification;
import de.mpc.pia.tools.unimod.UnimodParser;
import de.mpc.pia.tools.unimod.jaxb.ModT;


/**
 * This class parses the data from an OpenMS idXML file for a given
 * {@link PIACompiler}.<br/>
 * 
 * @author julian
 *
 */
public class IdXMLFileParser {
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(IdXMLFileParser.class);
	
	
	/**
	 * We don't ever want to instantiate this class
	 */
	private IdXMLFileParser() {
		throw new AssertionError();
	}
	
	
	/**
	 * Parses the data from an IdXML file given by its name into the given
	 * {@link PIACompiler}.
	 * 
	 * @param fileName name of the parsed file
	 */
	public static boolean getDataFromIdXMLFile(String name, String fileName,
			PIACompiler compiler) {
		
		IdXMLParser idXMLFile;
		
		try {
			idXMLFile = new IdXMLParser(fileName);
		} catch (Exception e) {
			logger.error("could not read '" + fileName + "'.", e);
			return false;
		}
		
		int accNr = 0;
		int pepNr = 0;
		int specNr = 0;
		
		Cv psiMS = new Cv();
		psiMS.setId("PSI-MS");
		psiMS.setFullName("PSI-MS");
		psiMS.setUri("http://psidev.cvs.sourceforge.net/viewvc/*checkout*/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo");
		
		int runCount = 0;
		for (IdentificationRun idRun : idXMLFile.getIdentificationRuns()) {
			
			runCount++;
			PIAInputFile file;
			if (idXMLFile.getIdentificationRuns().size() > 1) {
				file = compiler.insertNewFile(
						name + "_run" + runCount,
						fileName,
						InputFileParserFactory.InputFileTypes.ID_XML_INPUT.getFileSuffix());
			} else {
				file = compiler.insertNewFile(
						name,
						fileName,
						InputFileParserFactory.InputFileTypes.ID_XML_INPUT.getFileSuffix());
			}
			
			if (idRun.getProteinIdentification() == null) {
				logger.error("This identification has no protein information, " +
						"so PIA cannot use it.");
				break;
			}
			
			// create the analysis software and add it to the compiler
			AnalysisSoftware topp = new AnalysisSoftware();
			
			topp.setId("topp");
			topp.setName("TOPP software");
			topp.setUri("http://open-ms.sourceforge.net/");
			
			AbstractParam abstractParam; 
			Param param = new Param();
			abstractParam = new CvParam();
			((CvParam)abstractParam).setAccession("MS:1000752");
			((CvParam)abstractParam).setCv(psiMS);
			abstractParam.setName("TOPP software");
			param.setParam(abstractParam);
			topp.setSoftwareName(param);
			
			compiler.putIntoSoftwareMap(topp);
			
			// define the spectrumIdentificationProtocol
			SearchParameters searchParameters =
					(SearchParameters)idRun.getSearchParametersRef();
			
			SpectrumIdentificationProtocol spectrumIDProtocol =
					new SpectrumIdentificationProtocol();
			
			spectrumIDProtocol.setId("toppAnalysis");
			spectrumIDProtocol.setAnalysisSoftware(topp);
			
			// TODO: only supporting "ms-ms search" for now
			param = new Param();
			abstractParam = new CvParam();
			((CvParam)abstractParam).setAccession("MS:1001083");
			((CvParam)abstractParam).setCv(psiMS);
			abstractParam.setName("ms-ms search");
			param.setParam(abstractParam);
			spectrumIDProtocol.setSearchType(param);
			
			spectrumIDProtocol.setAdditionalSearchParams(new ParamList());
			if (searchParameters.getMassType().equals(MassType.MONOISOTOPIC)) {
				abstractParam = new CvParam();
				((CvParam)abstractParam).setAccession("MS:1001256");
				((CvParam)abstractParam).setCv(psiMS);
				abstractParam.setName("fragment mass type mono");
	    		spectrumIDProtocol.getAdditionalSearchParams().getCvParam().add(
	    				(CvParam)abstractParam);
				
				abstractParam = new CvParam();
	    		((CvParam)abstractParam).setAccession("MS:1001211");
	    		((CvParam)abstractParam).setCv(psiMS);
	    		abstractParam.setName("parent mass type mono");
	    		spectrumIDProtocol.getAdditionalSearchParams().getCvParam().add(
	    				(CvParam)abstractParam);
	        } else {
				abstractParam = new CvParam();
				((CvParam)abstractParam).setAccession("MS:1001255");
				((CvParam)abstractParam).setCv(psiMS);
				abstractParam.setName("fragment mass type average");
	    		spectrumIDProtocol.getAdditionalSearchParams().getCvParam().add(
	    				(CvParam)abstractParam);
				
				abstractParam = new CvParam();
				((CvParam)abstractParam).setAccession("MS:1001212");
				((CvParam)abstractParam).setCv(psiMS);
				abstractParam.setName("parent mass type average");
	    		spectrumIDProtocol.getAdditionalSearchParams().getCvParam().add(
	    				(CvParam)abstractParam);
	        }
			
			ModificationParams modParams = new ModificationParams();
			for (VariableModification variableMod
					: searchParameters.getVariableModification()) {
				Pattern pattern = Pattern.compile("^(.+)\\(([^)]+)\\)$");
				Matcher matcher = pattern.matcher(variableMod.getName());
				
				if (matcher.matches()) {
					SearchModification searchMod = new SearchModification();
					searchMod.setFixedMod(false);
					// add the residues
					for (String res : matcher.group(2).split(" ")) {
						if (res.length() > 1) {
							if (!searchMod.getResidues().contains(".")) {
								searchMod.getResidues().add(".");
							}
						} else {
							if (!searchMod.getResidues().contains(res)) {
								searchMod.getResidues().add(res);
							}
						}
					}
					
					ModT unimod =
							compiler.getUnimodParser().getModificationByName(
									matcher.group(1).trim(),
									searchMod.getResidues());
					
					if (unimod != null) {
						CvParam cvParam = new CvParam();
						cvParam.setAccession("UNIMOD:" + unimod.getRecordId());
						cvParam.setCv(UnimodParser.getCv());
						cvParam.setName(unimod.getTitle());
						searchMod.getCvParam().add(cvParam);
						
						searchMod.setMassDelta(
								unimod.getDelta().getMonoMass().floatValue());
						
						modParams.getSearchModification().add(searchMod);
					}
				} else {
					logger.error("Could not parse modification: " +
							variableMod.getName());
				}
			}
			for (FixedModification fixedMod
					: searchParameters.getFixedModification()) {
				Pattern pattern = Pattern.compile("^(.+)\\(([^)]+)\\)$");
				Matcher matcher = pattern.matcher(fixedMod.getName());
				
				if (matcher.matches()) {
					SearchModification searchMod = new SearchModification();
					searchMod.setFixedMod(true);
					// add the residues
					for (String res : matcher.group(2).split(" ")) {
						if (res.length() > 1) {
							if (!searchMod.getResidues().contains(".")) {
								searchMod.getResidues().add(".");
							}
						} else {
							if (!searchMod.getResidues().contains(res)) {
								searchMod.getResidues().add(res);
							}
						}
					}
					
					ModT unimod =
							compiler.getUnimodParser().getModificationByName(
									matcher.group(1).trim(),
									searchMod.getResidues());
					
					if (unimod != null) {
						CvParam cvParam = new CvParam();
						cvParam.setAccession("UNIMOD:" + unimod.getRecordId());
						cvParam.setCv(UnimodParser.getCv());
						cvParam.setName(unimod.getTitle());
						searchMod.getCvParam().add(cvParam);
						
						searchMod.setMassDelta(
								unimod.getDelta().getMonoMass().floatValue());
						
						modParams.getSearchModification().add(searchMod);
					}
				} else {
					logger.error("Could not parse modification: " +
							fixedMod.getName());
				}
			}
			spectrumIDProtocol.setModificationParams(modParams);
			
			
			Enzyme enzyme = new Enzyme();
			enzyme.setId("enzyme");
			if (searchParameters.getMissedCleavages() != null) {
				enzyme.setMissedCleavages(
						searchParameters.getMissedCleavages().intValue());
			}
			
			if (searchParameters.getEnzyme() != null) {
				
				ParamList paramList = new ParamList();
				abstractParam = new CvParam();
				((CvParam)abstractParam).setCv(psiMS);
				
				switch (searchParameters.getEnzyme()) {
				case NO_ENZYME:
					((CvParam)abstractParam).setAccession("MS:1001955");
					abstractParam.setName("no cleavage");
					paramList.getCvParam().add((CvParam)abstractParam);
					break;
					
				case CHYMOTRYPSIN:
					((CvParam)abstractParam).setAccession("MS:1001306");
					abstractParam.setName("Chymotrypsin");
					paramList.getCvParam().add((CvParam)abstractParam);
					enzyme.setSiteRegexp("(?<=[FYWL])(?!P)");
					break;
				
				case PEPSIN_A:
					((CvParam)abstractParam).setAccession("MS:1001311");
					abstractParam.setName("PepsinA");
					paramList.getCvParam().add((CvParam)abstractParam);
					enzyme.setSiteRegexp("(?<=[FL])");
					break;
					
				case TRYPSIN:
					((CvParam)abstractParam).setAccession("MS:1001251");
					abstractParam.setName("Trypsin");
					paramList.getCvParam().add((CvParam)abstractParam);
					enzyme.setSiteRegexp("(?<=[KR])(?!P)");
					break;
				
				case PROTEINASE_K:
				case UNKNOWN_ENZYME:
				default:
					break;
				}
				
				if (paramList.getCvParam().size() > 0) {
					enzyme.setEnzymeName(paramList);
				}
			}
			Enzymes enzymes = new Enzymes();
			enzymes.getEnzyme().add(enzyme);
			spectrumIDProtocol.setEnzymes(enzymes);
			
			
			Tolerance tolerance = new Tolerance();
			abstractParam = new CvParam();
			((CvParam)abstractParam).setAccession("MS:1001412");
			((CvParam)abstractParam).setCv(psiMS);
			abstractParam.setName("search tolerance plus value");
			abstractParam.setValue(Float.toString(searchParameters.getPeakMassTolerance()));
			tolerance.getCvParam().add((CvParam)abstractParam);
			
			abstractParam = new CvParam();
			((CvParam)abstractParam).setAccession("MS:1001413");
			((CvParam)abstractParam).setCv(psiMS);
			abstractParam.setName("search tolerance minus value");
			abstractParam.setValue(Float.toString(searchParameters.getPeakMassTolerance()));
			tolerance.getCvParam().add((CvParam)abstractParam);
			
			spectrumIDProtocol.setFragmentTolerance(tolerance);
			
			tolerance = new Tolerance();
			abstractParam = new CvParam();
			((CvParam)abstractParam).setAccession("MS:1001412");
			((CvParam)abstractParam).setCv(psiMS);
			abstractParam.setName("search tolerance plus value");
			abstractParam.setValue(Float.toString(searchParameters.getPrecursorPeakTolerance()));
			tolerance.getCvParam().add((CvParam)abstractParam);
			
			abstractParam = new CvParam();
			((CvParam)abstractParam).setAccession("MS:1001413");
			((CvParam)abstractParam).setCv(psiMS);
			abstractParam.setName("search tolerance minus value");
			abstractParam.setValue(Float.toString(searchParameters.getPrecursorPeakTolerance()));
			tolerance.getCvParam().add((CvParam)abstractParam);
			
			spectrumIDProtocol.setParentTolerance(tolerance);
			
			// add the protocol to the file
			file.addSpectrumIdentificationProtocol(spectrumIDProtocol);
			
			// create the SearchDatabase
			SearchDatabase searchDatabase = new SearchDatabase();
			searchDatabase.setId("toppDB");
			searchDatabase.setLocation(searchParameters.getDbVersion());
			searchDatabase.setName(searchParameters.getDb());
			// databaseName
			param = new Param();
			abstractParam = new UserParam();
			abstractParam.setName(searchParameters.getDb());
			param.setParam(abstractParam);
			searchDatabase.setDatabaseName(param);
			// TODO: add taxonomy information
			//if (searchParameters.getTaxonomy().trim().equals("") || searchParameters.getTaxonomy().trim().equalsIgnoreCase("All Entries")) {}
			// add searchDB to the compiler
			searchDatabase = compiler.putIntoSearchDatabasesMap(searchDatabase);
			
			
			// build the SpectrumIdentification
			SpectrumIdentification spectrumID = new SpectrumIdentification();
			spectrumID.setId("mascotIdentification");
			spectrumID.setSpectrumIdentificationList(null);
			spectrumID.setSpectrumIdentificationProtocol(spectrumIDProtocol);
			
			SearchDatabaseRef searchDBRef = new SearchDatabaseRef();
			searchDBRef.setSearchDatabase(searchDatabase);
			spectrumID.getSearchDatabaseRef().add(searchDBRef);
			
			file.addSpectrumIdentification(spectrumID);
			
			
			// go through the peptide identifications
			for (PeptideIdentification pepID : idRun.getPeptideIdentification()) {
				
				Double massToCharge = Double.valueOf(pepID.getMZ());
				Double retentionTime = Double.valueOf(pepID.getRT());
				
				// TODO: spectrum reference (not set inIdXML files) => sourceID
				//pepID.getSpectrumReference();
				
				for (PeptideHit pepHit : pepID.getPeptideHit()) {
					if (pepHit.getProteinRefs().size() < 1) {
						// identifications without proteins have no value (for now)
						logger.error("No protein linked to the peptide " +
								"identification, dropped PeptideHit for " +
								pepHit.getSequence());
						continue;
					}
					
					String sequence = pepHit.getSequence();
					int charge = pepHit.getCharge().intValue();
					
					Map<Integer, Modification> modifications =
							new HashMap<Integer, Modification>(5);
					if (sequence.contains("(")) {
						// get the modifications from the string
						Matcher matcher;
						Pattern pattern = Pattern.compile("\\(([^)]*)\\)");
						StringBuilder extractedSeq = new StringBuilder();
						int start = 0;
						
						matcher = pattern.matcher(sequence);
						
						while (matcher.find()) {
							// found a modification
							extractedSeq.append(sequence.substring(start, matcher.start()));
							start = matcher.end();
							
							String modName = matcher.group(1);
							
							String residue = null;
							// TODO: how are C-terminal modifications encoded in idXML!
							if (matcher.start() == 0) {
								// N-terminal modification
								residue = ".";
							} else {
								residue = sequence.substring(matcher.start()-1,
										matcher.start());
							}
							
							ModT unimod =
									compiler.getUnimodParser().getModificationByName(
											modName, residue);
							if (unimod != null) {
								Modification modification = new Modification(
										residue.charAt(0),
										unimod.getDelta().getMonoMass(),
										modName,
										"UNIMOD:" + unimod.getRecordId());
								
								modifications.put(extractedSeq.length(),
										modification);
							} else {
								logger.error("Could not get information for " +
										"modification " + modName + " in " +
										sequence);
							}
						}
						
						extractedSeq.append(sequence.substring(start));
						sequence = extractedSeq.toString();
					}
					
					// TODO: implement the delta mass, but not set in idXML
					double deltaMass = Double.NaN;
					
					int missedCleavages;
					if ((enzyme.getSiteRegexp() != null)) {
						missedCleavages =
								sequence.split(enzyme.getSiteRegexp()).length - 1;
					} else {
						missedCleavages = -1;
					}
					
					PeptideSpectrumMatch psm;
					psm = compiler.insertNewSpectrum(
							charge,
							massToCharge,
							deltaMass,
							retentionTime,
							sequence,
							missedCleavages,
							null,
							null,
							file,
							spectrumID);
					specNr++;
					
					// get the peptide from the compiler or, if need be, add it 
					Peptide peptide;
					peptide = compiler.getPeptide(sequence);
					if (peptide == null) {
						peptide = compiler.insertNewPeptide(sequence);
						pepNr++;
					}
					
					// add the spectrum to the peptide
					peptide.addSpectrum(psm);
					
					// the first score is the "main" score
					ScoreModel score;
					ScoreModelEnum scoreModel =
							ScoreModelEnum.getModelByDescription(
									pepID.getScoreType() + "_openmsmainscore");
					
					if (!scoreModel.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
						score = new ScoreModel(
								// looks weird, but so i don't get borked decimals
								Double.parseDouble(
										String.valueOf(pepHit.getScore())),
								scoreModel);
						psm.addScore(score);
					} else {
						score = new ScoreModel(
								Double.parseDouble(String.valueOf(pepHit.getScore())),
								pepID.getScoreType() + "_openmsmainscore",
								pepID.getScoreType());
					}
					
					// add additional userParams
					for (de.mpc.pia.tools.openms.jaxb.UserParam userParam
							: pepHit.getUserParam()) {
						// test for any other (known) scores
						if (userParam.getType().equals(UserParamType.FLOAT)) {
							scoreModel = ScoreModelEnum.getModelByDescription(
									pepID.getScoreType() + "_" + userParam.getName());
							
							if (!scoreModel.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
								score = new ScoreModel(
										Double.parseDouble(userParam.getValue()),
										scoreModel);
								psm.addScore(score);
							}
						}
						
						// if the target / decoy is set
						if (userParam.getName().equals("target_decoy")) {
							if (userParam.getValue().equals("target")) {
								psm.setIsDecoy(false);
							} else if (userParam.getValue().equals("decoy")) {
								psm.setIsDecoy(true);
							}
						}
					}
					
					// now add the modifications
					for (Map.Entry<Integer, Modification> modIt
							: modifications.entrySet()) {
						psm.addModification(modIt.getKey(), modIt.getValue());
					}
					
					for (Object protRef : pepHit.getProteinRefs()) {
						
						if (!(protRef instanceof ProteinHit)) {
							logger.warn("ProteinRef is not a " +
									ProteinHit.class.getCanonicalName());
							continue;
						}
						
						ProteinHit protHit = (ProteinHit)protRef;
						
						FastaHeaderInfos fastaInfo =
								FastaHeaderInfos.parseHeaderInfos(
										protHit.getAccession());
						if (fastaInfo == null) {
							logger.error("Could not parse '" +
									protHit.getAccession() + "'");
							continue;
						}
						
						// add the Accession to the compiler (if not already added)
						Accession acc = compiler.getAccession(
								fastaInfo.getAccession());
						
						if (acc == null) {
							acc = compiler.insertNewAccession(
									fastaInfo.getAccession(),
									protHit.getSequence());
							accNr++;
						}
						
						acc.addFile(file.getID());
						
						if ((fastaInfo.getDescription() != null) &&
								(fastaInfo.getDescription().length() > 0)) {
							acc.addDescription(file.getID(),
									fastaInfo.getDescription());
						}
						
						// add the searchDB to the accession
						acc.addSearchDatabaseRef(searchDatabase.getId());
						
						
						// get and add the accession occurrence to the peptide
						StringBuilder fragSeq =
								new StringBuilder(sequence.length()+2);
						
						logger.error(pepHit.getSequence() + " aa before" + pepHit.getAaBefore());
						
						if ((pepHit.getAaBefore() != null) &&
								(!(pepHit.getAaBefore().equals("[") ||
										pepHit.getAaBefore().equals("-") ||
										pepHit.getAaBefore().trim().equals("")))) {
							fragSeq.append(pepHit.getAaBefore());
						}
						fragSeq.append(sequence);
						if ((pepHit.getAaAfter() != null) &&
								(!(pepHit.getAaAfter().equals("]") ||
										pepHit.getAaAfter().equals("-") ||
										pepHit.getAaAfter().trim().equals("")))) {
							fragSeq.append(pepHit.getAaAfter());
						}
						
						Matcher matcher;
						matcher = Pattern.compile(fragSeq.toString()).
								matcher(acc.getDbSequence());
						while (matcher.find()) {
							int start;
							
							if ((pepHit.getAaBefore() == null) ||
									pepHit.getAaBefore().equals("[") ||
									pepHit.getAaBefore().equals("-") ||
									pepHit.getAaBefore().trim().equals("")) {
								start = matcher.start() + 1;
							} else {
								start = matcher.start() + 2;
							}
							
							peptide.addAccessionOccurrence(acc,
									start,
									start + sequence.length() - 1);
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
		}
		
		logger.info("inserted new: \n\t" +
				pepNr + " peptides\n\t" +
				specNr + " peptide spectrum matches\n\t" +
				accNr + " accessions");
		return true;
	}
	
}
