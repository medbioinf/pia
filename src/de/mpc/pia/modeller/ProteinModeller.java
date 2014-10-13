package de.mpc.pia.modeller;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;

import uk.ac.ebi.jmzidml.model.mzidml.AnalysisCollection;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisProtocolCollection;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.Cv;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.DBSequence;
import uk.ac.ebi.jmzidml.model.mzidml.InputSpectrumIdentifications;
import uk.ac.ebi.jmzidml.model.mzidml.Inputs;
import uk.ac.ebi.jmzidml.model.mzidml.ParamList;
import uk.ac.ebi.jmzidml.model.mzidml.Peptide;
import uk.ac.ebi.jmzidml.model.mzidml.PeptideEvidence;
import uk.ac.ebi.jmzidml.model.mzidml.PeptideHypothesis;
import uk.ac.ebi.jmzidml.model.mzidml.ProteinAmbiguityGroup;
import uk.ac.ebi.jmzidml.model.mzidml.ProteinDetection;
import uk.ac.ebi.jmzidml.model.mzidml.ProteinDetectionHypothesis;
import uk.ac.ebi.jmzidml.model.mzidml.ProteinDetectionList;
import uk.ac.ebi.jmzidml.model.mzidml.ProteinDetectionProtocol;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabase;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationItem;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationItemRef;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationList;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationResult;
import uk.ac.ebi.jmzidml.xml.io.MzIdentMLMarshaller;
import uk.ac.ebi.pride.jmztab.model.CVParam;
import uk.ac.ebi.pride.jmztab.model.MZTabColumnFactory;
import uk.ac.ebi.pride.jmztab.model.MZTabConstants;
import uk.ac.ebi.pride.jmztab.model.MZTabDescription;
import uk.ac.ebi.pride.jmztab.model.Metadata;
import uk.ac.ebi.pride.jmztab.model.MsRun;
import uk.ac.ebi.pride.jmztab.model.Protein;
import uk.ac.ebi.pride.jmztab.model.ProteinColumn;
import uk.ac.ebi.pride.jmztab.model.Reliability;
import uk.ac.ebi.pride.jmztab.model.Section;
import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.AccessionOccurrence;
import de.mpc.pia.intermediate.Group;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ProteinExecuteCommands;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.ReportProteinComparatorFactory;
import de.mpc.pia.modeller.protein.inference.AbstractProteinInference;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.SortOrder;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.report.filter.protein.ProteinScoreFilter;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.modeller.score.comparator.RankCalculator;
import de.mpc.pia.tools.PIAConstants;
import de.mpc.pia.tools.unimod.UnimodParser;


/**
 * Modeller for protein related stuff.
 * 
 * @author julian
 *
 */
public class ProteinModeller {
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(ProteinModeller.class);
	
	
	/** maps from the string ID to the {@link SearchDatabase}s, they are straight from the intermediateHandler */
	private Map<String, SearchDatabase> searchDatabases;
	
	
	
	/** List of the report proteins */
	private List<ReportProtein> reportProteins;
	
	/** Map of the report proteins, for easier accession */
	private Map<Long, ReportProtein> reportProteinsMap;
	
	/** the last applied inference filter */
	private AbstractProteinInference appliedProteinInference;
	
	/** the last applied scoring method */
	private AbstractScoring appliedScoringMethod;
	
	
	/** the corresponding {@link PSMModeller} */
	private PSMModeller psmModeller;
	
	/** the corresponding {@link PSMModeller} */
	private PeptideModeller peptideModeller;
	
	/** map of the {@link Group}s in the intermediate structure */
	private Map<Long, Group> intermediateGroups;
	
	
	/** the FDR settings for the protein FDR */
	private FDRData fdrData;
	
	
	/** filters which may be used for a protein inference */
	private List<AbstractFilter> inferenceFilters;
	
	/** the list of filters applied to the protein report */
	private List<AbstractFilter> reportFilters;
	
	
	// TODO: set these defaults in a file
	private static DecoyStrategy defaultDecoyStrategy = 
			FDRData.DecoyStrategy.ACCESSIONPATTERN;
	private static String defaultDecoyPattern = "s.*";
	private static Double defaultFDRThreshold = 0.05;
	
	
	/**
	 * Basic constructor for the ProteinModeller.<br/>
	 * There will be no inference, but only initialization.
	 * 
	 * @param groups
	 * @param considerModifications
	 */
	public ProteinModeller(PSMModeller psmModeller,
			PeptideModeller peptideModeller, Map<Long, Group> groups,
			Map<String, SearchDatabase> searchDatabases) {
		if (psmModeller == null) {
			throw new IllegalArgumentException("The given PSMModeller is null!");
		} else {
			this.psmModeller = psmModeller;
			
		}
		
		if (peptideModeller == null) {
			throw new IllegalArgumentException("The given PeptideModeller is null!");
		} else {
			this.peptideModeller = peptideModeller;
		}
		
		if (groups == null) {
			throw new IllegalArgumentException("The given intermediate Groups is null!");
		} else {
			this.intermediateGroups = groups;
		}
		
		this.appliedProteinInference = null;
		this.appliedScoringMethod = null;
		
		this.fdrData = new FDRData(defaultDecoyStrategy, defaultDecoyPattern,
				defaultFDRThreshold);
		
		this.reportFilters = new ArrayList<AbstractFilter>();
		
		this.searchDatabases = searchDatabases;
	}
	
	
	/**
	 * Applies the general settings
	 */
	public void applyGeneralSettings() {
		
	}
	
	
	/**
	 * Returns whether modifications were considered while building the
	 * peptides.
	 * 
	 * @return
	 */
	public Boolean getConsiderModifications() {
		return peptideModeller.getConsiderModifications();
	}
	
	
	/**
	 * Returns a List of all the currently available scoreShortNames
	 * @return
	 */
	public List<String> getAllScoreShortNames() {
		List<String> scoreShortNames = new ArrayList<String>();
		
		// get the scores from the files
		for (Long fileID : psmModeller.getFiles().keySet()) {
			for (String scoreShort : psmModeller.getScoreShortNames(fileID)) {
				if (!scoreShortNames.contains(scoreShort)) {
					scoreShortNames.add(scoreShort);
				}
			}
		}
		
		return scoreShortNames;
	}
	
	
	/**
	 * Returns the Score name, given the scoreShortName.
	 * @param fileID
	 * @param shortName
	 * @return
	 */
	public String getScoreName(String shortName) {
		return psmModeller.getScoreName(shortName);
	}
	
	
	/**
	 * Returns the mapping from the shortNames to the nicely readable names.
	 * 
	 * @return
	 */
	public Map<String, String> getScoreShortsToScoreNames() {
		return psmModeller.getScoreShortsToScoreNames();
	}
	
	
	/**
	 * Returns the filtered List of {@link ReportProtein}s or null, if the
	 * proteins are not inferred yet.
	 *  
	 * @param filters
	 * @return
	 */
	public List<ReportProtein> getFilteredReportProteins(
			List<AbstractFilter> filters) {
		if (reportProteins != null) {
			return FilterFactory.applyFilters(reportProteins, filters);
		} else {
			return null;
		}
	}
	
	
	/**
	 * Returns the protein with the given ID.
	 * @param proteinID
	 * @return
	 */
	public ReportProtein getProtein(Long proteinID) {
		return reportProteinsMap.get(proteinID);
	}
	
	
	/**
	 * Calculates the reported proteins with the given settings for the
	 * inference.
	 */
	public void infereProteins(AbstractProteinInference proteinInference) {
		reportProteins = new ArrayList<ReportProtein>();
		
		if (proteinInference != null) {
			appliedProteinInference = proteinInference;
			reportProteins = proteinInference.calculateInference(
					intermediateGroups,
					psmModeller.getReportPSMSets(),
					peptideModeller.getConsiderModifications(),
					psmModeller.getPSMSetSettings());
		} else {
			logger.error("No inference method set!");
			appliedProteinInference = null;
			reportProteins = null;
		}
		
		this.fdrData = new FDRData(defaultDecoyStrategy, defaultDecoyPattern,
				defaultFDRThreshold);
		
		// create the protein map
		reportProteinsMap = new HashMap<Long, ReportProtein>();
		for (ReportProtein protein : reportProteins) {
			reportProteinsMap.put(protein.getID(), protein);
			
			if (protein.getSubSets().size() > 0) {
				for (ReportProtein subProtein : protein.getSubSets()) {
					reportProteinsMap.put(subProtein.getID(), subProtein);
				}
			}
		}
	}
	
	
	/**
	 * Returns the last applied inference filter.<br/>
	 * If there was no filter or an error occurred during the inference, null
	 * will be returned.
	 * 
	 * @return
	 */
	public AbstractProteinInference getAppliedProteinInference() {
		return appliedProteinInference;
	}
	
	
	/**
	 * Returns the last applied scoring method.<br/>
	 * If there was no scoring yet or an error occurred during the scoring, null
	 * will be returned.
	 * 
	 * @return
	 */
	public AbstractScoring getAppliedScoringMethod() {
		return appliedScoringMethod;
	}
	
	
	/**
	 * Returns whether the proteins are ranked.
	 * @return
	 */
	public Boolean getAreProteinsRanked() {
		for (ReportProtein protein : reportProteins) {
			if ((protein.getRank() != null)) {
				return true;
			}
		}
		
		return false;
	}
	
	
	/**
	 * Calculates the ranking. If the filter List is not null or empty, the
	 * Report is filtered before ranking.
	 * 
	 * @param fileID
	 * @param rankableShortName
	 * @param filters
	 */
	public void calculateRanking(List<AbstractFilter> filters) {
		if ((appliedProteinInference == null) ||
				(appliedProteinInference.getScoring() == null)) {
			logger.error("No protein inference set." +
					" Please calculate inference before ranking.");
			return;
		}
		
		// first, dump all prior ranking
		for (ReportProtein protein : reportProteins) {
			protein.setRank(-1L);
		}
		
		Comparator<ReportProtein> comparator =
				ReportProteinComparatorFactory.CompareType.SCORE_SORT.getNewInstance();
		
		RankCalculator.calculateRanking(
				ScoreModelEnum.PROTEIN_SCORE.getShortName(),
				FilterFactory.applyFilters(reportProteins, filters),
				comparator);
	}
	
	
	/**
	 * Resorts the report with the given sorting parameters
	 */
	public void sortReport(List<String> sortOrders,
			Map<String, SortOrder> sortables) {
		
		List<Comparator<ReportProtein>> compares =
				new ArrayList<Comparator<ReportProtein>>();
		
		for (String sortKey : sortOrders) {
			SortOrder order = sortables.get(sortKey);
			
			compares.add( ReportProteinComparatorFactory.getComparatorByName(
					sortKey, order));
		}
		
		Collections.sort(reportProteins,
				ReportProteinComparatorFactory.getComparator(compares));
	}
	
	
	/**
	 * Apply the given scoring to the List of {@link ReportProtein}s.
	 * 
	 * @param scoring
	 */
	public void applyScoring(AbstractScoring scoring) {
		if (scoring == null) {
			logger.error("No scoring method given.");
			appliedScoringMethod = null;
			return;
		}
		
		logger.info("applying scoring method: " + scoring.getName());
		scoring.calculateProteinScores(reportProteins);
		logger.info("scoring done");
		appliedScoringMethod = scoring;
		
		this.fdrData = new FDRData(defaultDecoyStrategy, defaultDecoyPattern,
				defaultFDRThreshold);
	}
	
	
	/**
	 * Getter for the protein FDR data
	 * @return
	 */
	public FDRData getFDRData() {
		return fdrData;
	}
	
	
	/**
	 * Returns, whether there are any PSMs in the PIA XML file, which are
	 * flagged as decoys.
	 * 
	 * @param fileID
	 * @return
	 */
	public Boolean getInternalDecoysExist() {
		for (Long fileID : psmModeller.getFiles().keySet()) {
			if ((fileID > 0) && psmModeller.getFileHasInternalDecoy(fileID)) {
				return true;
			}
		}
		
		return false;
	}
	
	
	/**
	 * Updates the {@link FDRData} for the protein FDR
	 * 
	 * @param fileID
	 * @return
	 */
	public void updateFDRData(DecoyStrategy decoyStrategy,
			String decoyPattern, Double fdrThreshold) {
		fdrData.setDecoyStrategy(decoyStrategy);
		fdrData.setDecoyPattern(decoyPattern);
		fdrData.setFDRThreshold(fdrThreshold);
		fdrData.setScoreShortName(ScoreModelEnum.PROTEIN_SCORE.getShortName());
		
		logger.info("Protein FDRData set to: " +
				fdrData.getDecoyStrategy() + ", " +
				fdrData.getDecoyPattern() + ", " +
				fdrData.getFDRThreshold() + ", " +
				fdrData.getScoreShortName());
	}
	
	
	/**
	 * Updates the decoy states of the Proteins with the current settings from
	 * the FDRData.
	 */
	public void updateDecoyStates() {
		logger.info("updateDecoyStates ");
		Pattern p = Pattern.compile(fdrData.getDecoyPattern());
		
		for (ReportProtein protein : reportProteins) {
			// dump all FDR data
			protein.dumpFDRCalculation();
			protein.updateDecoyStatus(fdrData.getDecoyStrategy(), p);
		}
	}
	
	
	/**
	 * Calculate the protein FDR
	 * 
	 * @param fileID
	 */
	public void calculateFDR() {
		// calculate the FDR values
		fdrData.calculateFDR(reportProteins, true);
	}
	
	
	/**
	 * Returns the report filters.
	 * @param fileID
	 * @return
	 */
	public List<AbstractFilter> getReportFilters() {
		if (reportFilters == null) {
			reportFilters = new ArrayList<AbstractFilter>();
		}
		
		return reportFilters;
	}
	
	
	/**
	 * Add a new filter to the report filters.
	 */
	public boolean addReportFilter(AbstractFilter newFilter) {
		if (newFilter != null) {
			return getReportFilters().add(newFilter);
		} else {
			return false;
		}
	}
	
	
	/**
	 * Removes the report filter at the given index.
	 * @param fileID
	 * @param removingIndex
	 * @return
	 */
	public AbstractFilter removeReportFilter(int removingIndex) {
		if ((removingIndex >= 0) &&
				(reportFilters != null) &&
				(removingIndex < reportFilters.size())) {
			return reportFilters.remove(removingIndex);
		}
		
		return null;
	}
	
	
	/**
	 * Returns the list of inference filters. These are not the currently set
	 * filters, but a list of filters which may be used for inference. 
	 * @param fileID
	 * @return
	 */
	public List<AbstractFilter> getInferenceFilters() {
		if (inferenceFilters == null) {
			inferenceFilters = new ArrayList<AbstractFilter>();
		}
		
		return inferenceFilters;
	}
	
	
	/**
	 * Add a new filter to the inference filters. These are not the currently
	 * set filters, but a list of filters which may be used for inference. 
	 */
	public boolean addInferenceFilter(AbstractFilter newFilter) {
		if (newFilter != null) {
			return getInferenceFilters().add(newFilter);
		} else {
			return false;
		}
	}
	
	
	/**
     * Writes the Protein report with the given filters in a loose CSV format.
     * <br/>
     * As the export may or may not also contain peptide-, PSM-set- and
     * PSM-data, each exported line has a specifying tag in the beginning, if
     * more than only the protein data is exported. The tags are PROTEIN,
     * PEPTIDE, PSMSET and PSM for the respective data. Additionally there is a
     * line starting with COLS_[tag], specifying the columns of the respective
     * data.
     * 
     * @throws IOException
     */
	public void exportCSV(Writer writer, Boolean filterExport,
			Boolean includePeptides, Boolean includePSMSets,
			Boolean includePSMs, Boolean oneAccessionPerLine) throws IOException {
		List<ReportProtein> report;
		Boolean includes = includePeptides || includePSMSets || includePSMs;
		List<String> scoreShorts = peptideModeller.getScoreShortNames(0L);
		
		boolean considermodifications =
				peptideModeller.getConsiderModifications();
		
		if (includes && !oneAccessionPerLine) {
			writer.append(
					"\"COLS_PROTEIN\";"+
					"\"accessions\";" +
					"\"score\";" +
					"\"#peptides\";" +
					"\"#PSMs\";" +
					"\"#spectra\";");
			
			if (fdrData.getNrItems() != null) {
				writer.append(
						"\"isDecoy\";" +
						"\"FDR\";" +
						"\"q-value\";");
			}
			
			writer.append("\n");
			
			if (includePeptides) {
				writer.append(
						"\"COLS_PEPTIDE\";"+
						"\"sequence\";");
				
				if (considermodifications) {
					writer.append("\"modifications\";");
				}
				
				writer.append(	"\"accessions\";" +
						"\"#spectra\";" +
						"\"#PSMSets\";" +
						"\"bestScores\";" +
						"\n"
						);
			}
			
			if (includePSMSets) {
				writer.append(
						"\"COLS_PSMSET\";"+
						"\"sequence\";");
				
				if (considermodifications) {
					writer.append("\"modifications\";");
				}
				
				writer.append("\"#identifications\";" +
						"\"charge\";" +
						"\"m/z\";" +
						"\"dMass\";" +
						"\"ppm\";" +
						"\"RT\";" +
						"\"missed\";" +
						"\"sourceID\";" +
						"\"spectrumTitle\";" +
						"\n"
						);
			}
			
			if (includePSMs) {
				writer.append(
						"\"COLS_PSM\";"+
						"\"filename\";" +
						"\"sequence\";");
				
				if (considermodifications) {
					writer.append("\"modifications\";");
				}
				
				writer.append("\"charge\";" +
						"\"m/z\";" +
						"\"dMass\";" +
						"\"ppm\";" +
						"\"RT\";" +
						"\"missed\";" +
						"\"sourceID\";" +
						"\"spectrumTitle\";" +
						"\"scores\";" +
						"\n"
						);
			}
			
		} else if (!oneAccessionPerLine) {
			// no special includes, no SpectralCounting
			writer.append(
					"\"accessions\";" +
					"\"score\";" +
					"\"#peptides\";" +
					"\"#PSMs\";" +
					"\"#spectra\";");
			
			if (fdrData.getNrItems() != null) {
				writer.append(
						"\"isDecoy\";" +
						"\"FDR\";" +
						"\"q-value\";");
			}
			
			writer.append("\n");
		} else {
			// oneAccessionPerLine is set, override everything else
			writer.append(
					"\"accession\";" +
					"\"filename\";" +
					"\"sequence\";");
			
			if (considermodifications) {
				writer.append("\"modifications\";");
			}
			
			writer.append("\"charge\";" +
					"\"m/z\";" +
					"\"dMass\";" +
					"\"ppm\";" +
					"\"RT\";" +
					"\"missed\";" +
					"\"sourceID\";" +
					"\"spectrumTitle\";" +
					"\"scores\";" +
					"\"isUnique\";" +
					"\n"
					);
		}
		
		report = filterExport ? getFilteredReportProteins(getReportFilters()) :
			reportProteins;
		
		if (report == null) {
			// no inference run?
			logger.warn("The report is empty, probably no inference run?");
			writer.flush();
			return;
		}
		
		for (ReportProtein protein : report) {
			// Accessions	Score	Coverage	#Peptides	#PSMs	#Spectra
			
			StringBuffer accSB = new StringBuffer();
			for (Accession accession : protein.getAccessions()) {
				if (accSB.length() > 0) {
					accSB.append(",");
				}
				accSB.append(accession.getAccession());
				
				// TODO: if decoys through inherit, mark the decoy accessions
			}
			
			if (!oneAccessionPerLine) {
				if (includes) {
					writer.append("\"PROTEIN\";");
				}
				
				writer.append("\"" + accSB.toString() + "\";" +
						"\"" + protein.getScore() + "\";" +
						"\"" + protein.getNrPeptides() + "\";" +
						"\"" + protein.getNrPSMs() + "\";" +
						"\"" + protein.getNrSpectra() + "\";"
						);
				
				if (fdrData.getNrItems() != null) {
					writer.append(
							"\"" + protein.getIsDecoy() + "\";" +
							"\"" + protein.getFDR() + "\";" +
							"\"" + protein.getQValue() + "\";"
							);
				}
				
				writer.append("\n");
			}
			
			
			if (includes || oneAccessionPerLine) {
				for (ReportPeptide peptide : protein.getPeptides()) {
					
					StringBuffer modStringBuffer = new StringBuffer();
					if (considermodifications) {
						for (Map.Entry<Integer, Modification> modIt
								: peptide.getModifications().entrySet()) {
							modStringBuffer.append("[" + modIt.getKey() + ";" +
									modIt.getValue().getMass() + ";");
							if (modIt.getValue().getDescription() != null) {
								modStringBuffer.append(
										modIt.getValue().getDescription());
							}
							modStringBuffer.append("]");
						}
					}
					
					if (includePeptides && !oneAccessionPerLine) {
						
						accSB = new StringBuffer();
						for (Accession accession : peptide.getAccessions()) {
							if (accSB.length() > 0) {
								accSB.append(",");
							}
							accSB.append(accession.getAccession());
						}
						
						StringBuffer scoresSB = new StringBuffer();
						for (String scoreShort : scoreShorts) {
							ScoreModel model = 
									peptide.getBestScoreModel(scoreShort);
							
							if (model != null) {
								if (scoresSB.length() > 0) {
									scoresSB.append(",");
								}
								
								scoresSB.append(model.getName() + ":" +
										model.getValue());
							}
							
						}
						
						writer.append(
								"\"PEPTIDE\";"+
								"\"" + peptide.getSequence() + "\";");
						
						if (considermodifications) {
							writer.append(
									"\"" + modStringBuffer.toString() + "\";");
						}
						
						writer.append("\"" + accSB.toString() + "\";" +
								"\"" + peptide.getNrSpectra() + "\";" +
								"\"" + peptide.getNrPSMs() + "\";" +
								"\"" + scoresSB.toString() + "\";" +
								"\n"
								);
					}
					
					
					if (includePSMSets || includePSMs || oneAccessionPerLine) {
						for (PSMReportItem psmSet : peptide.getPSMs()) {
							if (psmSet instanceof ReportPSMSet) {
								
								if (includePSMSets && !oneAccessionPerLine) {
									String rt;
									if (psmSet.getRetentionTime() != null) {
										rt = psmSet.getRetentionTime().toString();
									} else {
										rt = "";
									}
									
									String sourceID = psmSet.getSourceID();
									if (sourceID == null) {
										sourceID = "";
									}
									
									String spectrumTitle = psmSet.getSpectrumTitle();
									if (spectrumTitle == null) {
										spectrumTitle = "";
									}
									
									writer.append(
											"\"PSMSET\";"+
											"\"" + psmSet.getSequence() +"\";");
									
									if (considermodifications) {
										writer.append(
												"\"" + modStringBuffer.toString() + "\";");
									}
									
									writer.append("\"" + ((ReportPSMSet)psmSet).getPSMs().size() + "\";" +
											"\"" + psmSet.getCharge() + "\";" +
											"\"" + psmSet.getMassToCharge() + "\";" +
											"\"" + psmSet.getDeltaMass() + "\";" +
											"\"" + psmSet.getDeltaPPM() + "\";" +
											"\"" + rt + "\";" +
											"\"" + psmSet.getMissedCleavages() + "\";" +
											"\"" + sourceID + "\";" +
											"\"" + spectrumTitle + "\";" +
											"\n"
											);
								}
								
								if (includePSMs || oneAccessionPerLine) {
									for (ReportPSM psm
											: ((ReportPSMSet)psmSet).getPSMs()) {
										
										String rt;
										if (psm.getRetentionTime() != null) {
											rt = psmSet.getRetentionTime().toString();
										} else {
											rt = "";
										}
										
										String sourceID = psm.getSourceID();
										if (sourceID == null) {
											sourceID = "";
										}
										
										String spectrumTitle = psm.getSpectrumTitle();
										if (spectrumTitle == null) {
											spectrumTitle = "";
										}
										
										StringBuffer scoresSB = new StringBuffer();
										for (ScoreModel model : psm.getScores()) {
											if (scoresSB.length() > 0) {
												scoresSB.append(",");
											}
											scoresSB.append(model.getName() +
													":" + model.getValue());
										}
										
										Boolean uniqueness =
												(psm.getSpectrum().getIsUnique() != null) ?
														psm.getSpectrum().getIsUnique() : false;
										
										if (!oneAccessionPerLine) {
											writer.append(
													"\"PSM\";" +
													"\"" + psm.getInputFileName() + "\";" +
													"\"" + psm.getSequence() + "\";");
											
											if (considermodifications) {
												writer.append(
														"\"" + modStringBuffer.toString() + "\";");
											}
											
											writer.append("\"" + psm.getCharge() + "\";" +
													"\"" + psm.getMassToCharge() + "\";" +
													"\"" + psm.getDeltaMass() + "\";" +
													"\"" + psm.getDeltaPPM() + "\";" +
													"\"" + rt + "\";" +
													"\"" + psm.getMissedCleavages() + "\";" +
													"\"" + sourceID + "\";" +
													"\"" + spectrumTitle + "\";" +
													"\"" + scoresSB.toString() + "\";" +
													"\n"
													);
										} else {
											// export for Spectral Counting
											for (Accession acc
													: protein.getAccessions()) {
												
												writer.append(
														"\"" + acc.getAccession() + "\";" +
														"\"" + psm.getInputFileName() + "\";" +
														"\"" + psm.getSequence() + "\";"
														);
												
												if (considermodifications) {
													writer.append(
															"\"" + modStringBuffer.toString() + "\";");
												}
												
												writer.append("\"" + psm.getCharge() + "\";" +
														"\"" + psm.getMassToCharge() + "\";" +
														"\"" + psm.getDeltaMass() + "\";" +
														"\"" + psm.getDeltaPPM() + "\";" +
														"\"" + rt + "\";" +
														"\"" + psm.getMissedCleavages() + "\";" +
														"\"" + sourceID + "\";" +
														"\"" + spectrumTitle + "\";" +
														"\"" + scoresSB.toString() + "\";" +
														"\"" + uniqueness  + "\";" +
														"\n"
														);
											}
										}
										
									}
								}
								
							} else {
								// TODO: better error/exception
								logger.error("PSM in peptide must be PSMSet!");
							}
						}
					}
					
					
				}
			}
		}
		
		writer.flush();
	}
	
	
	/**
     * Writes the Protein report into mzIdentML.
     * 
     * @throws IOException
     */
	public void exportMzIdentML(Writer writer, Boolean filterExport) 
			throws IOException {
		logger.info("start writing mzIdentML file");
		
		UnimodParser unimodParser;
		try {
			unimodParser = new UnimodParser();
		} catch (JAXBException e) {
			logger.error("Could not initialize the UnimodParser.", e);
			writer.flush();
			return;
		}
		
		MzIdentMLMarshaller m = new MzIdentMLMarshaller();
		
		// XML header
		writer.write(m.createXmlHeader() + "\n");
		writer.write(m.createMzIdentMLStartTag("PIAExport for proteins") + "\n");
		
		
		// there are some variables needed for additional tags later
		Cv psiCV = new Cv();
		Cv unimodCV = new Cv();
		Cv unitCV = new Cv();
		Map<String, DBSequence> sequenceMap = new HashMap<String, DBSequence>();
		Map<String, Peptide> peptideMap = new HashMap<String, Peptide>();
		Map<String, PeptideEvidence> pepEvidenceMap =
				new HashMap<String, PeptideEvidence>();
		Map<Long, SpectrumIdentificationList> silMap =
				new HashMap<Long, SpectrumIdentificationList>();
		AnalysisSoftware piaAnalysisSoftware = new AnalysisSoftware();
		Inputs inputs = new Inputs();
		AnalysisProtocolCollection analysisProtocolCollection =
				new AnalysisProtocolCollection();
		AnalysisCollection analysisCollection = new AnalysisCollection();
		
		psmModeller.writeCommonMzIdentMLTags(writer, m, unimodParser,
				psiCV, unimodCV, unitCV,
				sequenceMap, peptideMap, pepEvidenceMap, silMap,
				piaAnalysisSoftware, inputs,
				analysisProtocolCollection, analysisCollection,
				0L, false);
		
		for (SpectrumIdentificationList sil : silMap.values()) {
			// the "intermediate PSM list" flag, the combined list below is always the final
			CvParam tempCvParam = new CvParam();
			tempCvParam.setAccession(PIAConstants.CV_INTERMEDIATE_PSM_LIST_ACCESSION);
			tempCvParam.setCv(psiCV);
			tempCvParam.setName(PIAConstants.CV_INTERMEDIATE_PSM_LIST_NAME);
			sil.getCvParam().add(tempCvParam);
		}
		
		// create the ProteinDetectionProtocol for PIAs protein inference
		ProteinDetectionProtocol proteinDetectionProtocol =
				new ProteinDetectionProtocol();
		
		proteinDetectionProtocol.setId("PIA_protein_inference_protocol");
		proteinDetectionProtocol.setAnalysisSoftware(
				piaAnalysisSoftware);
		proteinDetectionProtocol.setName("PIA protein inference protocol");
		
		analysisProtocolCollection.setProteinDetectionProtocol(
				proteinDetectionProtocol);
		
		// add the inference settings to the AnalysisParams
		proteinDetectionProtocol.setAnalysisParams(new ParamList());
		
		// TODO: use CVs for all the userParams below
		
		// the used inference method
		CvParam cvParam = new CvParam();
		cvParam.setAccession(PIAConstants.CV_PIA_PROTEIN_INFERENCE_ACCESSION);
		cvParam.setCv(psiCV);
		cvParam.setName(PIAConstants.CV_PIA_PROTEIN_INFERENCE_NAME);
		cvParam.setValue(getAppliedProteinInference().getShortName());
		proteinDetectionProtocol.getAnalysisParams().getCvParam().add(cvParam);
		
		// inference filters
		for (AbstractFilter filter 
				: getAppliedProteinInference().getFilters()) {
			cvParam = new CvParam();
			cvParam.setAccession(
					PIAConstants.CV_PIA_PROTEIN_INFERENCE_FILTER_ACCESSION);
			cvParam.setCv(psiCV);
			cvParam.setName(PIAConstants.CV_PIA_PROTEIN_INFERENCE_FILTER_NAME);
			cvParam.setValue(filter.toString());
			proteinDetectionProtocol.getAnalysisParams().getCvParam().add(
					cvParam);
		}
		
		// scoring method
		cvParam = new CvParam();
		cvParam.setAccession(
				PIAConstants.CV_PIA_PROTEIN_INFERENCE_SCORING_ACCESSION);
		cvParam.setCv(psiCV);
		cvParam.setName(PIAConstants.CV_PIA_PROTEIN_INFERENCE_SCORING_NAME);
		cvParam.setValue(
				getAppliedProteinInference().getScoring().getShortName());
		proteinDetectionProtocol.getAnalysisParams().getCvParam().add(cvParam);
		
		// score used for scoring
		cvParam = new CvParam();
		cvParam.setAccession(
				PIAConstants.CV_PIA_PROTEIN_INFERENCE_USED_SCORE_ACCESSION);
		cvParam.setCv(psiCV);
		cvParam.setName(PIAConstants.CV_PIA_PROTEIN_INFERENCE_USED_SCORE_NAME);
		cvParam.setValue(
				getAppliedProteinInference().getScoring().getScoreSetting()
				.getValue());
		proteinDetectionProtocol.getAnalysisParams().getCvParam().add(cvParam);
		
		// PSMs used for scoring
		cvParam = new CvParam();
		cvParam.setAccession(
				PIAConstants.CV_PIA_PROTEIN_INFERENCE_USED_PSMS_ACCESSION);
		cvParam.setCv(psiCV);
		cvParam.setName(PIAConstants.CV_PIA_PROTEIN_INFERENCE_USED_PSMS_NAME);
		cvParam.setValue(getAppliedProteinInference().getScoring()
						.getPSMForScoringSetting().getValue());
		proteinDetectionProtocol.getAnalysisParams().getCvParam().add(cvParam);
		
		proteinDetectionProtocol.setThreshold(new ParamList());
		if (filterExport && (getReportFilters().size() > 0)) {
			for (AbstractFilter filter : getReportFilters()) {
				if (ProteinScoreFilter.shortName().equals(filter.getShortName())) {
					// if score filters are set, they are the threshold
					cvParam = new CvParam();
					cvParam.setAccession(
							PIAConstants.CV_PIA_PROTEIN_SCORE_ACCESSION);
					cvParam.setCv(psiCV);
					cvParam.setName(PIAConstants.CV_PIA_PROTEIN_SCORE_NAME);
					cvParam.setValue(filter.getFilterValue().toString());
					proteinDetectionProtocol.getThreshold()
							.getCvParam().add(cvParam);
				} else {
					// all other report filters are AnalysisParams
					
					cvParam = new CvParam();
					cvParam.setAccession(PIAConstants.CV_PIA_FILTER_ACCESSION);
					cvParam.setCv(psiCV);
					cvParam.setName(PIAConstants.CV_PIA_FILTER_NAME);
					cvParam.setValue(filter.toString());
					proteinDetectionProtocol.getAnalysisParams().getCvParam()
							.add(cvParam);
				}
			}
		}
		if ((proteinDetectionProtocol.getThreshold().getCvParam().size() < 1) && 
				(proteinDetectionProtocol.getThreshold().getUserParam().size() < 1)) {
			cvParam = new CvParam();
			cvParam.setAccession("MS:1001494");
			cvParam.setCv(psiCV);
			cvParam.setName("no threshold");
			proteinDetectionProtocol.getThreshold().getCvParam().add(cvParam);
		}
		
		// create the proteinDetectionList
		ProteinDetectionList proteinDetectionList = new ProteinDetectionList();
		proteinDetectionList.setId("protein_report");
		
		Map<String, SpectrumIdentificationItem> combinedSpecIdItemMap =
				new HashMap<String, SpectrumIdentificationItem>();
		
		Map<String, SpectrumIdentificationResult> combinedSpecIdResMap =
				new HashMap<String, SpectrumIdentificationResult>();
		
		Integer thresholdPassingPAGcount = 0;
		for (ReportProtein protein : reportProteins) {
			ProteinAmbiguityGroup pag = new ProteinAmbiguityGroup();
			
			pag.setId("PAG_" + protein.getID());
			proteinDetectionList.getProteinAmbiguityGroup().add(pag);
			
			Boolean passThreshold = true;
			if (filterExport && (reportFilters.size() > 0)) {
				passThreshold = FilterFactory.satisfiesFilterList(
						protein, 0L, reportFilters);
			}
			
			if (passThreshold) {
				thresholdPassingPAGcount++;
			}
			
			cvParam = new CvParam();
			cvParam.setAccession(PIAConstants.CV_PROTEIN_GROUP_PASSES_THRESHOLD_ACCESSION);
			cvParam.setCv(psiCV);
			cvParam.setName(PIAConstants.CV_PROTEIN_GROUP_PASSES_THRESHOLD_NAME);
			cvParam.setValue(passThreshold.toString());
			pag.getCvParam().add(cvParam);
			
			cvParam = new CvParam();
			cvParam.setAccession(PIAConstants.CV_CLUSTER_IDENTIFIER_ACCESSION);
			cvParam.setCv(psiCV);
			cvParam.setName(PIAConstants.CV_CLUSTER_IDENTIFIER_NAME);
			cvParam.setValue(
					Long.toString(
							protein.getAccessions().get(0).getGroup().getTreeID()));
			pag.getCvParam().add(cvParam);
			
			StringBuilder leadingPDHids = new StringBuilder();
			// the reported proteins/accessions are the "main" proteins
			for (Accession acc : protein.getAccessions()) {
				ProteinDetectionHypothesis pdh =
						createPDH(acc, protein, passThreshold, pag.getId(),
								sequenceMap, peptideMap, pepEvidenceMap,
								combinedSpecIdItemMap, combinedSpecIdResMap,
								psiCV, unitCV);
				
				cvParam = new CvParam();
				cvParam.setAccession(PIAConstants.CV_LEADING_PROTEIN_ACCESSION);
				cvParam.setCv(psiCV);
				cvParam.setName(PIAConstants.CV_LEADING_PROTEIN_NAME);
				pdh.getCvParam().add(cvParam);
				
				leadingPDHids.append(pdh.getId());
				leadingPDHids.append(" ");
				
				pag.getProteinDetectionHypothesis().add(pdh);
			}
			
			if (pag.getProteinDetectionHypothesis().size() > 1) {
				for (ProteinDetectionHypothesis pdh
						: pag.getProteinDetectionHypothesis()) {
					StringBuilder otherPDHs =
							new StringBuilder(leadingPDHids.length());
					
					for (ProteinDetectionHypothesis others
							: pag.getProteinDetectionHypothesis()) {
						if (!others.equals(pdh)) {
							otherPDHs.append(others.getId());
							otherPDHs.append(" ");
						}
					}
					
					cvParam = new CvParam();
					cvParam.setAccession(
							PIAConstants.CV_SEQUENCE_SAME_SET_PROTEIN_ACCESSION);
					cvParam.setCv(psiCV);
					cvParam.setName(
							PIAConstants.CV_SEQUENCE_SAME_SET_PROTEIN_NAME);
					cvParam.setValue(otherPDHs.toString().trim());
					pdh.getCvParam().add(cvParam);
				}
			}
			
			// now add the sub-proteins
			for (ReportProtein subProtein : protein.getSubSets()) {
				List<ProteinDetectionHypothesis> samePDHs =
						new ArrayList<ProteinDetectionHypothesis>();
				
				for (Accession subAcc : subProtein.getAccessions()) {
					ProteinDetectionHypothesis pdh =
							createPDH(subAcc, subProtein,
									false, pag.getId(),
									sequenceMap, peptideMap, pepEvidenceMap,
									combinedSpecIdItemMap, combinedSpecIdResMap,
									psiCV, unitCV);
					
					cvParam = new CvParam();
					cvParam.setAccession(
							PIAConstants.CV_NON_LEADING_PROTEIN_ACCESSION);
					cvParam.setCv(psiCV);
					cvParam.setName(PIAConstants.CV_NON_LEADING_PROTEIN_NAME);
					pdh.getCvParam().add(cvParam);
					
					cvParam = new CvParam();
					cvParam.setAccession(
							PIAConstants.CV_SEQUENCE_SUB_SET_PROTEIN_ACCESSION);
					cvParam.setCv(psiCV);
					cvParam.setName(
							PIAConstants.CV_SEQUENCE_SUB_SET_PROTEIN_NAME);
					cvParam.setValue(leadingPDHids.toString().trim());
					pdh.getCvParam().add(cvParam);
					
					pag.getProteinDetectionHypothesis().add(pdh);
					samePDHs.add(pdh);
				}
				
				if (samePDHs.size() > 1) {
					for (ProteinDetectionHypothesis pdh
							: samePDHs) {
						StringBuilder otherPDHs =
								new StringBuilder(leadingPDHids.length());
						
						for (ProteinDetectionHypothesis others
								: samePDHs) {
							if (!others.equals(pdh)) {
								otherPDHs.append(others.getId());
								otherPDHs.append(" ");
							}
						}
						
						cvParam = new CvParam();
						cvParam.setAccession(
								PIAConstants.CV_SEQUENCE_SAME_SET_PROTEIN_ACCESSION);
						cvParam.setCv(psiCV);
						cvParam.setName(
								PIAConstants.CV_SEQUENCE_SAME_SET_PROTEIN_NAME);
						cvParam.setValue(otherPDHs.toString().trim());
						pdh.getCvParam().add(cvParam);
					}
				}
			}
		}
		
		cvParam = new CvParam();
		cvParam.setAccession(PIAConstants.CV_COUNT_OF_IDENTIFIED_PROTEINS_ACCESSION);
		cvParam.setCv(psiCV);
		cvParam.setName(PIAConstants.CV_COUNT_OF_IDENTIFIED_PROTEINS_NAME);
		cvParam.setValue(thresholdPassingPAGcount.toString());
		proteinDetectionList.getCvParam().add(cvParam);
		
		// create the combinedSil
		SpectrumIdentificationList combinedSil = new SpectrumIdentificationList();
		combinedSil.setId("combined_inference_PSMs");
		for (SpectrumIdentificationResult idResult
				: combinedSpecIdResMap.values()) {
			combinedSil.getSpectrumIdentificationResult().add(idResult);
		}
		
		// the "final PSM list" flag
		CvParam tempCvParam = new CvParam();
		tempCvParam.setAccession(PIAConstants.CV_FINAL_PSM_LIST_ACCESSION);
		tempCvParam.setCv(psiCV);
		tempCvParam.setName(PIAConstants.CV_FINAL_PSM_LIST_NAME);
		combinedSil.getCvParam().add(tempCvParam);
		
		silMap.put(0L, combinedSil);
		combinedSpecIdItemMap = null;
		combinedSpecIdResMap = null;
		
		// create the protocol and SpectrumIdentification for combined PSMs
		SpectrumIdentification combiningId =
				psmModeller.createCombinedSpectrumIdentification(
						psiCV, piaAnalysisSoftware,
						getAppliedProteinInference().getFilters());
		
		analysisCollection.getSpectrumIdentification().add(combiningId);
		combiningId.setSpectrumIdentificationList(combinedSil);
		
		analysisProtocolCollection.getSpectrumIdentificationProtocol()
				.add(combiningId.getSpectrumIdentificationProtocol());
					
		// create the ProteinDetection for PIAs protein inference
		ProteinDetection proteinDetection = new ProteinDetection();
		analysisCollection.setProteinDetection(proteinDetection);
		proteinDetection.setId("PIA_protein_inference");
		proteinDetection.setName("PIA protein inference");
		proteinDetection.setProteinDetectionList(proteinDetectionList);
		proteinDetection.setProteinDetectionProtocol(proteinDetectionProtocol);
		InputSpectrumIdentifications inputSpecIDs = new InputSpectrumIdentifications();
		inputSpecIDs.setSpectrumIdentificationList(silMap.get(0L));
		proteinDetection.getInputSpectrumIdentifications().add(inputSpecIDs);
		
		m.marshal(analysisCollection, writer);
		writer.write("\n");
		
		m.marshal(analysisProtocolCollection, writer);
		writer.write("\n");
		
		writer.write(m.createDataCollectionStartTag() + "\n");
		
		m.marshal(inputs, writer);
		writer.write("\n");
		
		
		writer.write(m.createAnalysisDataStartTag() + "\n");
		
		// write out the spectrumIdentificationLists
		for (SpectrumIdentificationList siList : silMap.values()) {
			m.marshal(siList, writer);
			writer.write("\n");
		}
		
		m.marshal(proteinDetectionList, writer);
		writer.write("\n");
		
		writer.write(m.createAnalysisDataClosingTag() + "\n");
		
		writer.write(m.createDataCollectionClosingTag() + "\n");
		
		writer.write(m.createMzIdentMLClosingTag());
		
		writer.flush();
		logger.info("writing of mzIdentML done");
	}
	
	
	/**
	 * Creates a {@link ProteinDetectionHypothesis} (PDH) from the given
	 * information.
	 * 
	 * @param acc the accession for this PDH of the associated protein
	 * @param protein the actual {@link ReportProtein}, which can have multiple
	 * {@link Accession}s 
	 * @param passThreshold whether the PDH's passThreshold value is true or
	 * false
	 * @param PAGid the ID of the associated {@link ProteinAmbiguityGroup},
	 * needed to cunstruct a scope-valid ID
	 * @param sequenceMap a map holding the DBSequences, mapping from their IDs
	 * @param pepEvidenceMap a map holding the PeptideEvidences, mapping from
	 * their IDs
	 * @param combinedSiiMap
	 * @return
	 */
	private ProteinDetectionHypothesis createPDH(Accession acc,
			ReportProtein protein,
			Boolean passThreshold,
			String PAGid,
			Map<String, DBSequence> sequenceMap,
			Map<String, Peptide> peptideMap,
			Map<String, PeptideEvidence> pepEvidenceMap,
			Map<String, SpectrumIdentificationItem> combinedSpecIdItemMap,
			Map<String, SpectrumIdentificationResult> combinedSpecIdResMap,
			Cv psiCV,
			Cv unitCV) {
		ProteinDetectionHypothesis pdh =
				new ProteinDetectionHypothesis();
		
		pdh.setId("PDH_" + acc.getAccession() + "_" + PAGid);
		
		DBSequence dbSequence = sequenceMap.get(acc.getAccession());
		if (dbSequence != null) {
			pdh.setDBSequence(dbSequence);
		}
		
		pdh.setPassThreshold(passThreshold);
		
		String scoreShort =
				appliedProteinInference.getScoring().getScoreSetting().getValue();
		
		Map<String, PeptideHypothesis> peptideHypotheses =
				new HashMap<String, PeptideHypothesis>();
		
		for (ReportPeptide pep : protein.getPeptides()) {
			for (PSMReportItem psmItem : pep.getPSMs()) {
				// sort the PSMs' SpectrumIdentificationItems into the PeptideHypotheses
				
				Set<String> peptideEvidenceIDs = new HashSet<String>();
				
				boolean foundOccurrence = false;
				
				for (AccessionOccurrence occurrence 
						: psmItem.getPeptide().getAccessionOccurrences()) {
					if (acc.getAccession().equals(
							occurrence.getAccession().getAccession())) {
						peptideEvidenceIDs.add(
								psmModeller.createPeptideEvidenceID(
										psmItem.getPeptideStringID(true),
										occurrence.getStart(),
										occurrence.getEnd(),
										acc));
						
						foundOccurrence = true;
						
						// there might be multiple occurrences per accession, so no loop-break here
					}
				}
				
				if (!foundOccurrence) {
					peptideEvidenceIDs.add(
							psmModeller.createPeptideEvidenceID(
									psmItem.getPeptideStringID(true),
									null, null, acc));
				}
				
				for (String evidenceID : peptideEvidenceIDs) {
					PeptideHypothesis ph =
							peptideHypotheses.get(evidenceID);
					if (ph == null) {
						ph = new PeptideHypothesis();
						ph.setPeptideEvidence(
								pepEvidenceMap.get(evidenceID));
						
						
						
						
						peptideHypotheses.put(evidenceID, ph);
						pdh.getPeptideHypothesis().add(ph);
					}
					
					String spectrumItemID;
					if (!psmModeller.getCreatePSMSets()) {
						spectrumItemID =
								psmModeller.getSpectrumIdentificationItemID(
										psmItem,
										((ReportPSMSet) psmItem).getPSMs().get(0).getFileID());
						spectrumItemID += ":set";
					} else {
						spectrumItemID =
								psmModeller.getSpectrumIdentificationItemID(
										psmItem, 0L);
					}
					
					SpectrumIdentificationItem specIdItem =
							combinedSpecIdItemMap.get(spectrumItemID);
					if (specIdItem == null) {
						// if the spectrumIdentificationItem is not yet set,
						// create it (and put it into the SpectrumIdentificationResult)
						specIdItem =
								psmModeller.putPsmInSpectrumIdentificationResultMap(
										psmItem,
										combinedSpecIdResMap,
										peptideMap,
										pepEvidenceMap,
										psiCV,
										unitCV,
										null,
										false);
						
						ScoreModel compareScore =
								psmItem.getCompareScore(scoreShort);
						if (compareScore != null) {
							if (!compareScore.getType().
									equals(ScoreModelEnum.UNKNOWN_SCORE)) {
								CvParam tempCvParam = new CvParam();
								tempCvParam.setAccession(compareScore.getAccession());
								tempCvParam.setCv(psiCV);
								tempCvParam.setName(compareScore.getType().getCvName());
								tempCvParam.setValue(compareScore.getValue().toString());
								
								specIdItem.getCvParam().add(tempCvParam);
							} else {
								// TODO: add unknown scores...
							}
						}
						
						combinedSpecIdItemMap.put(spectrumItemID, specIdItem);
					}
					
					SpectrumIdentificationItemRef ref =
							new SpectrumIdentificationItemRef();
					ref.setSpectrumIdentificationItem(specIdItem);
					ph.getSpectrumIdentificationItemRef().add(ref);
				}
			}
		}
		
		CvParam cvParam = new CvParam();
		cvParam.setAccession(PIAConstants.CV_PIA_PROTEIN_SCORE_ACCESSION);
		cvParam.setCv(psiCV);
		cvParam.setName(PIAConstants.CV_PIA_PROTEIN_SCORE_NAME);
		cvParam.setValue(protein.getScore().toString());
		pdh.getCvParam().add(cvParam);
		
		return pdh;
	}
	
	
	/**
     * Writes the Protein report into mzTab.
     * 
     * @throws IOException
     */
	public void exportMzTab(Writer writer, Boolean filterExport,
			Boolean includePSMSets) throws IOException {
		logger.info("start writing mzTab file");
		
		UnimodParser unimodParser; 
		try {
			unimodParser = new UnimodParser();
		} catch (JAXBException e) {
			logger.error("Could not initialize the UnimodParser.", e);
			writer.flush();
			return;
		}
		
		// Setting version, mode, and type in MZTabDescription 
		MZTabDescription tabDescription;
		// PIA cannot give a "Complete" protein level export, as the protein
		// scores of the used search engines is not known
		tabDescription = new MZTabDescription(
				MZTabDescription.Mode.Summary,
				MZTabDescription.Type.Identification);
		
		Map<String, List<MsRun>> specIdRefToMsRuns =
				new HashMap<String, List<MsRun>>();
		
		Map<String, Integer> psmScoreShortToId = new HashMap<String, Integer>();
		
		Metadata mtd = psmModeller.createMetadataForMzTab(0L, unimodParser,
				tabDescription, specIdRefToMsRuns, psmScoreShortToId);
		
		// add PIA to the list of used softwares
		int piaSoftwareNr = mtd.getSoftwareMap().size() + 1;
		CVParam piaParam = new CVParam("MS",
				PIAConstants.CV_PIA_ACCESSION,
				PIAConstants.CV_PIA_NAME,
				PIAConstants.version);
		mtd.addSoftwareParam(piaSoftwareNr, piaParam);
		
		mtd.addSoftwareSetting(piaSoftwareNr, "modifications are " + 
				(getConsiderModifications() ? "" : "NOT ") +
				"taken into account for peptide distinction");
		
		if (psmModeller.isCombinedFDRScoreCalculated()) {
			mtd.addSoftwareSetting(piaSoftwareNr,
					PIAConstants.CV_PSM_LEVEL_COMBINED_FDRSCORE_NAME + 
					" was calculated");
			
			for (Map.Entry<Long, FDRData> fdrIt
					: psmModeller.getFileFDRData().entrySet()) {
				if (fdrIt.getKey() > 0) {
					mtd.addSoftwareSetting(piaSoftwareNr,
							"base score for FDR calculation for file " +
							fdrIt.getKey() + " = " +
							fdrIt.getValue().getScoreShortName());
				}
			}
		}
		
		if (filterExport) {
			for (AbstractFilter filter : getReportFilters()) {
				mtd.addSoftwareSetting(piaSoftwareNr, "applied filter " + 
						filter.toString());
			}
		}
		
		// add the PIA protein score
		mtd.addProteinSearchEngineScoreParam(1, new CVParam("MS",
				PIAConstants.CV_PIA_PROTEIN_SCORE_ACCESSION,
				PIAConstants.CV_PIA_PROTEIN_SCORE_NAME,
				null));
		
		// write out the header
		writer.append(mtd.toString());
		
		// get the report proteins
		List<ReportProtein> proteinList;
		proteinList = getFilteredReportProteins(
				filterExport ? getReportFilters() : null);
		
		if (proteinList == null) {
			logger.warn("No report protein list, probably inference was not run.");
		} else {
			Map<String, PSMReportItem> reportPSMs =
					new HashMap<String, PSMReportItem>();
			// write out the proteins
			writer.append(MZTabConstants.NEW_LINE);
			writeProteinsForMzTab(mtd, proteinList, specIdRefToMsRuns,
					reportPSMs, piaParam, writer, unimodParser);
			
			// write out the PSMs
			writer.append(MZTabConstants.NEW_LINE);
			psmModeller.writePSMsForMzTab(mtd, 
					new ArrayList<PSMReportItem>(reportPSMs.values()),
					specIdRefToMsRuns,
					psmScoreShortToId,
					psmModeller.isCombinedFDRScoreCalculated(), writer,
					unimodParser);
		}
		
		writer.flush();
		logger.info("writing of mzTab done");
	}
	
	
	/**
	 * Writes the protein section into an mzTab file
	 * 
	 * @param metadata
	 * @param report
	 * @param specIDRefToMsRuns
	 * @param reportPSMs
	 * @param writer
	 * @param unimodParser
	 * @throws IOException
	 */
	public void writeProteinsForMzTab(Metadata metadata,
			List<ReportProtein> report,
			Map<String, List<MsRun>> specIDRefToMsRuns,
			Map<String, PSMReportItem> reportPSMs,
			CVParam piaParam,
			Writer writer,
			UnimodParser unimodParser) throws IOException {
		// initialize the columns
		MZTabColumnFactory columnFactory =
				MZTabColumnFactory.getInstance(Section.Protein_Header);
		
		Map<String, Boolean> psmSetSettings = psmModeller.getPSMSetSettings();
		
		// cache the msRuns
		Map<Integer, MsRun> msRunMap = new HashMap<Integer, MsRun>();
		for (List<MsRun> msRunList : specIDRefToMsRuns.values()) {
			for (MsRun msRun : msRunList) {
				msRunMap.put(msRun.getId(), msRun);
			}
		}
		
		// add msRun specific information, where possible
		for (Map.Entry<Integer, MsRun> msRunIt : msRunMap.entrySet()) {
			columnFactory.addOptionalColumn(ProteinColumn.NUM_PSMS,
					msRunIt.getValue());
			
			columnFactory.addOptionalColumn(ProteinColumn.NUM_PEPTIDES_DISTINCT,
					msRunIt.getValue());
		}
		
		// add the PIA score 
		CVParam proteinScoreColumnParam =
				new CVParam(PIAConstants.CV_PSI_MS_LABEL,
						PIAConstants.CV_PIA_PROTEIN_SCORE_ACCESSION,
						PIAConstants.CV_PIA_PROTEIN_SCORE_NAME,
						null);
		columnFactory.addOptionalColumn(proteinScoreColumnParam, Double.class);
		
		// add FDR value, if calculated
		CVParam fdrColumnParam = null;
		CVParam qvalueColumnParam = null;
		if (fdrData.getNrItems() != null) {
			columnFactory.addReliabilityOptionalColumn();
			
			fdrColumnParam = new CVParam(PIAConstants.CV_PSI_MS_LABEL,
					"MS:1002364",
					"protein-level local FDR",
					null);
			columnFactory.addOptionalColumn(fdrColumnParam, Double.class);
			
			qvalueColumnParam = new CVParam(PIAConstants.CV_PSI_MS_LABEL,
					"MS:1002373",
					"protein group-level q-value",
					null);
			columnFactory.addOptionalColumn(qvalueColumnParam, Double.class);
		}
		
		// add custom column for nr_peptides
		CVParam nrPeptidesColumnParam =
				new CVParam("MS", "MS:1001097", "distinct peptide sequences", null);
		columnFactory.addOptionalColumn(nrPeptidesColumnParam, Integer.class);
		
		// add custom column for nr_psms
		columnFactory.addOptionalColumn(
				PIAConstants.MZTAB_NR_PSMS_COLUMN_NAME, Integer.class);
		
		// add custom column for nr_spectra
		columnFactory.addOptionalColumn(
				PIAConstants.MZTAB_NR_SPECTRA_COLUMN_NAME, Integer.class);
		
		writer.append(columnFactory.toString());
		writer.append(MZTabConstants.NEW_LINE);
		
		// cache the databaseRefs to an array with name and version
		Map<String, String[]> dbRefToDbNameAndVersion =
				new HashMap<String, String[]>();
		
		for (ReportProtein reportProtein : report) {
			Protein mzTabProtein = new Protein(columnFactory);
			
			Accession representative = reportProtein.getRepresentative();
			mzTabProtein.setAccession(representative.getAccession());
			
			// just take one description
			for (String desc : representative.getDescriptions().values()) {
				if (desc.trim().length() > 0) {
					mzTabProtein.setDescription(desc);
					break;
				}
			}
			
			// taxID and species is (for now) not known in PIA
	        //mzTabProtein.setTaxid(null);
	        //mzTabProtein.setSpecies(null);
			
			// set the first available dbName and dbVersion of the representative
			for (String dbRef :	representative.getSearchDatabaseRefs()) {
				String[] nameAndVersion =
						dbRefToDbNameAndVersion.get(dbRef);
				// cache the name and version of databases
				if (nameAndVersion == null) {
					SearchDatabase sDB = searchDatabases.get(dbRef);
					
					if (sDB.getDatabaseName() != null) {
						nameAndVersion = new String[2];
						if (sDB.getDatabaseName().getCvParam() != null) {
							nameAndVersion[0] = 
									sDB.getDatabaseName().getCvParam().getName();
						} else if (sDB.getDatabaseName().getUserParam() != null) {
							nameAndVersion[0] = 
									sDB.getDatabaseName().getUserParam().getName();
						}
						nameAndVersion[1] = sDB.getVersion();
						
					} else if (sDB.getName() != null) {
						nameAndVersion = new String[2];
						nameAndVersion[0] = sDB.getName();
						nameAndVersion[1] = sDB.getVersion();
					} else {
						nameAndVersion = new String[1];
						nameAndVersion[0] = null;
					}
					
					dbRefToDbNameAndVersion.put(dbRef, nameAndVersion);
				}
				
				if (nameAndVersion[0] != null) {
					mzTabProtein.setDatabase(nameAndVersion[0]);
					mzTabProtein.setDatabaseVersion(nameAndVersion[1]);
				}
			}
			
			Set<String> usedSoftwareRefs = new HashSet<String>();
			
			Map<Integer, Integer> msRunIdToNumPSMs =
					new HashMap<Integer, Integer>();
			
			Map<Integer, Set<String>> msRunIdToDistinctPeptides =
					new HashMap<Integer, Set<String>>();
			
			// go through each peptide and PSM of the protein and collect information
			for (ReportPeptide reportPeptide : reportProtein.getPeptides()) {
				for (PSMReportItem reportItem : reportPeptide.getPSMs()) {
					if (reportItem instanceof ReportPSMSet) {
						
						for (ReportPSM reportPSM
								: ((ReportPSMSet) reportItem).getPSMs()) {
							// get the used search engine for this PSM
							usedSoftwareRefs.add(reportPSM.getFile().
									getAnalysisProtocolCollection().
									getSpectrumIdentificationProtocol().get(0).
									getAnalysisSoftwareRef());
							
							String specIdRef = reportPSM.getSpectrum().
									getSpectrumIdentification().getId();
							
							if (specIDRefToMsRuns.get(specIdRef) != null) {
								// increase the numPSMs of the the msRun(s)
								for (MsRun msRun
										: specIDRefToMsRuns.get(specIdRef)) {
									Integer id = msRun.getId();
									
									if (msRunIdToNumPSMs.containsKey(id)) {
										msRunIdToNumPSMs.put(id,
												msRunIdToNumPSMs.get(id) + 1);
									} else {
										msRunIdToNumPSMs.put(id, 0);
									}
									
									Set<String> disctinctPeptides =
											msRunIdToDistinctPeptides.get(msRun.getId());
									if (disctinctPeptides == null) {
										disctinctPeptides = new HashSet<String>(
												reportProtein.getNrPeptides());
										msRunIdToDistinctPeptides.put(
												msRun.getId(), disctinctPeptides);
									}
									disctinctPeptides.add(reportPSM.getSequence());
								}
							}
						}
						
						// add the PSM set to the PSM map
						String psmKey = reportItem.getIdentificationKey(psmSetSettings);
						if (!reportPSMs.containsKey(psmKey)) {
							reportPSMs.put(psmKey, reportItem);
						}
					} else {
						logger.error(
								"item in ReportPeptide should NOT be ReportPSM");
					}
				}
			}
			
			// add the search engines, which is always PIA
			mzTabProtein.addSearchEngineParam(piaParam);
			
			// set the PIA protein score
			mzTabProtein.setOptionColumnValue(proteinScoreColumnParam, reportProtein.getScore());
			
			// the protein score for each search engine's identification is not collected
			//mzTabProtein.addSearchEngineScoreParam(msRun, param)
			
			// get the protein FDR, if calculated, and set the reliability
			if (fdrColumnParam != null) {
				if (reportProtein.getQValue() <= 0.01) {
					mzTabProtein.setReliability(Reliability.High);
				} else if (reportProtein.getQValue() <= 0.05) {
					mzTabProtein.setReliability(Reliability.Medium);
				} else {
					mzTabProtein.setReliability(Reliability.Poor);
				}
				
				mzTabProtein.setOptionColumnValue(fdrColumnParam, reportProtein.getFDR());
				mzTabProtein.setOptionColumnValue(qvalueColumnParam, reportProtein.getQValue());
			}
			
			for (Map.Entry<Integer, MsRun> msRunIt : msRunMap.entrySet()) {
				Integer msRunID = msRunIt.getValue().getId();
				// set the num_psms_ms_run
				if (msRunIdToNumPSMs.containsKey(msRunID)) {
					mzTabProtein.setNumPSMs(msRunIt.getValue(),
							msRunIdToNumPSMs.get(msRunID));
				} else {
					mzTabProtein.setNumPSMs(msRunIt.getValue(), 0);
				}
				
				if (msRunIdToDistinctPeptides.containsKey(msRunID)) {
					mzTabProtein.setNumPeptidesDistinct(msRunIt.getValue(),
							msRunIdToDistinctPeptides.get(msRunID).size());
				} else {
					mzTabProtein.setNumPeptidesDistinct(msRunIt.getValue(), 0);
				}
			}
			
			for (Accession ambiguityMember : reportProtein.getAccessions()) {
				if (!ambiguityMember.equals(representative)) {
					mzTabProtein.addAmbiguityMembers(
							ambiguityMember.getAccession());
				}
			}
			
			
			// TODO: perhaps add modifications
			//mzTabProtein.addModification(modification);
			
			// there is no URI for the proteins
			//mzTabProtein.setURI(null);
			
			// there is no GO term annotation in PIA yet
			//mzTabProtein.addGOTerm(null)
			
			Double coverage = 
					reportProtein.getCoverage(representative.getAccession());
			if (!coverage.equals(Double.NaN)) {
				mzTabProtein.setProteinConverage(coverage);
			}
			
			mzTabProtein.setOptionColumnValue(nrPeptidesColumnParam,
					reportProtein.getNrPeptides());
			
			mzTabProtein.setOptionColumnValue(
					PIAConstants.MZTAB_NR_PSMS_COLUMN_NAME,
					reportProtein.getNrPSMs());
			
			mzTabProtein.setOptionColumnValue(
					PIAConstants.MZTAB_NR_SPECTRA_COLUMN_NAME,
					reportProtein.getNrSpectra());
			
			writer.append(mzTabProtein.toString());
			writer.append(MZTabConstants.NEW_LINE);
		}
	}
	
	
	/**
	 * Processes the command line on the protein level.
	 * 
	 * @param model
	 * @param commands
	 * @return
	 */
	public static boolean processCLI(ProteinModeller model, String[] commands) {
		if (model == null) {
			logger.error("No protein modeller given while processing CLI " +
					"commands");
			return false;
		}
		
		Pattern pattern = Pattern.compile("^([^=]+)=(.*)");
		Matcher commandParamMatcher;
		
		for (String command : commands) {
			String[] params = null;
			commandParamMatcher = pattern.matcher(command);
			
			if (commandParamMatcher.matches()) {
				command = commandParamMatcher.group(1);
				params = commandParamMatcher.group(2).split(",");
			}
			
			try {
				ProteinExecuteCommands.valueOf(command).execute(model, params);
			} catch (IllegalArgumentException e) {
				logger.error("Could not process unknown call to " +
						command);
			}
		}
		
		return true;
	}
}