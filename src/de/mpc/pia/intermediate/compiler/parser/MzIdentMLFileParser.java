package de.mpc.pia.intermediate.compiler.parser;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.obo.datamodel.IdentifiedObject;
import org.obo.datamodel.Link;
import org.obo.datamodel.impl.OBOClassImpl;
import org.obo.datamodel.impl.OBORestrictionImpl;

import uk.ac.ebi.jmzidml.model.mzidml.AbstractParam;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisCollection;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisData;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisProtocolCollection;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftwareList;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.DBSequence;
import uk.ac.ebi.jmzidml.model.mzidml.Enzyme;
import uk.ac.ebi.jmzidml.model.mzidml.Enzymes;
import uk.ac.ebi.jmzidml.model.mzidml.InputSpectra;
import uk.ac.ebi.jmzidml.model.mzidml.Inputs;
import uk.ac.ebi.jmzidml.model.mzidml.Modification;
import uk.ac.ebi.jmzidml.model.mzidml.ParamList;
import uk.ac.ebi.jmzidml.model.mzidml.PeptideEvidence;
import uk.ac.ebi.jmzidml.model.mzidml.PeptideEvidenceRef;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabase;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabaseRef;
import uk.ac.ebi.jmzidml.model.mzidml.SequenceCollection;
import uk.ac.ebi.jmzidml.model.mzidml.SpectraData;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationItem;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationList;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationProtocol;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationResult;
import uk.ac.ebi.jmzidml.model.mzidml.UserParam;
import uk.ac.ebi.jmzidml.xml.io.MzIdentMLUnmarshaller;
import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.tools.obo.OBOMapper;


/**
 * This class parses the data from a mzIdentML file for a given
 * {@link PIACompiler}.<br/>
 * 
 * @author julian
 *
 */
public class MzIdentMLFileParser {
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(MzIdentMLFileParser.class);
	
	
	/**
	 * We don't ever want to instantiate this class
	 */
	private MzIdentMLFileParser() {
		throw new AssertionError();
	}
	
	
	/**
	 * Parses the data from an mzIdentML file given by its name into the given
	 * {@link PIACompiler}.
	 * 
	 * @param fileName name of the mzTab file
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
				new HashMap<String, SpectrumIdentificationList>();
		
		// maps from the file's ID to the compiler's ID of the SpectrumIdentification
		Map<String, String> spectrumIdentificationRefs = new HashMap<String, String>();
		
		// maps from the file's ID to the compiler's ID of the SpectrumIdentificationProtocol
		Map<String, String> spectrumIdentificationProtocolRefs = new HashMap<String, String>();
		
		// maps from the file's ID to the compiler's SpectraData
		Map<String, SpectraData> spectraDataRefs = new HashMap<String, SpectraData>();
		
		// maps from the file's ID to the compiler's searchDB
		Map<String, SearchDatabase> searchDBRefs = new HashMap<String, SearchDatabase>();
		
		// maps from the file's ID to the compiler's analysisSoftware
		Map<String, AnalysisSoftware> analysisSoftwareRefs = new HashMap<String, AnalysisSoftware>();
		
		// maps from the ID to the PeptideEvidence
		Map<String, PeptideEvidence> peptideEvidences = new HashMap<String, PeptideEvidence>();
		
		// maps from the ID to the DBSequence
		Map<String, DBSequence> dbSequences = new HashMap<String, DBSequence>();
		
		// maps from the ID to the Protein
		Map<String, uk.ac.ebi.jmzidml.model.mzidml.Peptide> peptides = new HashMap<String, uk.ac.ebi.jmzidml.model.mzidml.Peptide>();
		
		// maps from the SpectrumIdentificationList IDs to the SpectrumIdentification IDs
		Map<String, String> specIdListIDtoSpecIdID = new HashMap<String, String>();
		
		// maps from the enzyme ID (from the OBO) to the regex, only used, if no siteRegex param is given
		Map<String, String> enzymesToRegexes = new HashMap<String, String>();
		
		
		Set<String> neededSpectrumIdentificationProtocols = new HashSet<String>();
		Set<String> neededSpectraData = new HashSet<String>();
		Set<String> neededSearchDatabases= new HashSet<String>();
		Set<String> neededAnalysisSoftwares= new HashSet<String>();
		
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
				for (InputSpectra spectra : si.getInputSpectra()) {
					neededSpectraData.add(spectra.getSpectraDataRef());
				}
				for (SearchDatabaseRef db : si.getSearchDatabaseRef()) {
					neededSearchDatabases.add(db.getSearchDatabaseRef());
				}
			} else {
				// TODO: better error / exception
				logger.warn("file contains SpectrumIdentification (" +
						si.getId() + ") without SpectrumIdentificationList!");
			}
		}
		
		// get the necessary AnalysisProtocolCollection:SpectrumIdentificationProtocol
		AnalysisProtocolCollection analysisProtocolCollection =
				unmarshaller.unmarshal(AnalysisProtocolCollection.class);
		for (SpectrumIdentificationProtocol idProtocol
				: analysisProtocolCollection.getSpectrumIdentificationProtocol()) {
			if (neededSpectrumIdentificationProtocols.contains(idProtocol.getId())) {
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
									String oboID = ((CvParam)(paramList.get(0))).getAccession();
									
									if (enzymesToRegexes.get(oboID) == null) {
										// we still need the regExp for this enzyme
										IdentifiedObject obj =
												compiler.getOBOMapper().getObject(oboID);
										
										if (obj instanceof OBOClassImpl) {
											Collection<Link> links = ((OBOClassImpl) obj).getParents();
											for (Link link : links) {
												if ((link instanceof OBORestrictionImpl) &&
														(link.getType().getID().equals(OBOMapper.has_regexp_relation))) {
													// this is the regExp, put it into the map
													enzymesToRegexes.put(oboID, link.getParent().getName());
												}
											}
										}
									}
									
								} else if (paramList.get(0) instanceof UserParam) {
									// userParam is given, no use here
								}
							}
						}
					}
				} else {
					logger.warn("No enzymes in mzIdentML, this should not happen!");
				}
				
			}
		}
		neededSpectrumIdentificationProtocols = null;
		
		// get the necessary inputs:SpectraData
		Inputs inputs = unmarshaller.unmarshal(Inputs.class);
		for (SpectraData spectraData : inputs.getSpectraData()) {
			if (neededSpectraData.contains(spectraData.getId())) {
				String ref = spectraData.getId();
				SpectraData sd = compiler.putIntoSpectraDataMap(spectraData);
				spectraDataRefs.put(ref, sd);
			}
		}
		neededSpectraData = null;
		
		// get the necessary inputs:SearchDBs
		for (SearchDatabase searchDB : inputs.getSearchDatabase()) {
			if (neededSearchDatabases.contains(searchDB.getId())) {
				String ref = searchDB.getId();
				SearchDatabase sDB = compiler.putIntoSearchDatabasesMap(searchDB);
				searchDBRefs.put(ref, sDB);
			}
		}
		neededSearchDatabases = null;
		
		// get the necessary AnalysisSoftwares
		AnalysisSoftwareList analysisSoftwareList = unmarshaller.unmarshal(AnalysisSoftwareList.class);
		for (AnalysisSoftware software : analysisSoftwareList.getAnalysisSoftware()) {
			if (neededAnalysisSoftwares.contains(software.getId())) {
				String ref = software.getId();
				AnalysisSoftware as = compiler.putIntoSoftwareMap(software);
				analysisSoftwareRefs.put(ref, as);
			}
		}
		neededAnalysisSoftwares = null;
		
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
			Set<String> specIDListsDBRefs = new HashSet<String>();
			SpectrumIdentification spectrumID = null;
			Enzymes specIDListsEnzymes = null;
			Set<InputSpectra> inputSpectraSet = new HashSet<InputSpectra>();
			for (SpectrumIdentification specID 
					: file.getAnalysisCollection().getSpectrumIdentification()) {
				if (specID.getId().equals(specIdListIDtoSpecIdID.get(specIDList.getId()))) {
					// this is the SpectrumIdentification for this list
					for (SearchDatabaseRef dbRef : specID.getSearchDatabaseRef()) {
						specIDListsDBRefs.add(dbRef.getSearchDatabaseRef());
					}
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
							((CvParam)param).getAccession().equals(OBOMapper.spectrumTitleID)) {
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
						deltaMass = -1;
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
						
						
						// now insert the peptide and the accession into the accession peptide map
						Set<Peptide> accsPeptides =
								compiler.getFromAccPepMap(acc.getAccession());
						
						if (accsPeptides == null) {
							accsPeptides = new HashSet<Peptide>();
							compiler.putIntoAccPepMap(acc.getAccession(), accsPeptides);
						}
						
						accsPeptides.add(pep);
						
						// and also insert them into the peptide accession map
						Set<Accession> pepsAccessions =
								compiler.getFromPepAccMap(pep.getSequence());
						
						if (pepsAccessions == null) {
							pepsAccessions = new HashSet<Accession>();
							compiler.putIntoPepAccMap(pep.getSequence(), pepsAccessions);
						}
						
						pepsAccessions.add(acc);
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
					
					// create the PeptideSpectrumMatch object
					PeptideSpectrumMatch psm;
					psm = compiler.insertNewSpectrum(specIdItem.getChargeState(),
							specIdItem.getExperimentalMassToCharge(),
							deltaMass,
							null,		// TODO: look for a CV with the RT
							sequence,
							missed,
							specIdResult.getSpectrumID(),
							spectrumTitle,
							file,
							spectrumID);
					pep.addSpectrum(psm);
					specNr++;
					
					// get the scores and add them to the PSM
					for (CvParam cvParam : specIdItem.getCvParam()) {
						String cvAccession = cvParam.getAccession();
						
						IdentifiedObject obj =
								compiler.getOBOMapper().getObject(cvAccession);
						
						boolean isScore = false;
						
						if (obj instanceof OBOClassImpl) {
							Collection<Link> links = ((OBOClassImpl) obj).getParents();
							for (Link link : links) {
								if ((link instanceof OBORestrictionImpl) &&
										link.getType().getID().equals(OBOMapper.is_a_relation) &&
										link.getParent().getID().equals(OBOMapper.peptideScoreID)) {
									// obj is a "search engine specific score for peptides"
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
					
					for (UserParam userParam : specIdItem.getUserParam()) {
						// add the userParam to the params of the PSM
						psm.addParam(userParam);
					}
					
					for (AbstractParam param : resultParams.getParamGroup()) {
						// add the params from the specIdResult to the PSM
						psm.addParam(param);
					}
					
					// adding the modifications
					// the modifications are in SequenceCollection:Peptide
					if (peptide != null) {
						
						for (Modification mod : peptide.getModification()) {
							
							if (mod.getLocation() == null) {
								logger.warn("Cannot build modification without location, skipping.");
							}
							
							de.mpc.pia.intermediate.Modification modification;
							
							Character residue = null;
							String description = null;
							String accession = null;
							Double massDelta = 
									(mod.getMonoisotopicMassDelta() != null) ? 
											mod.getMonoisotopicMassDelta().doubleValue()
											: null;
							
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
