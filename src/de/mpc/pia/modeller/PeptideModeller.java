package de.mpc.pia.modeller;

import java.io.IOException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.modeller.peptide.PeptideExecuteCommands;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.peptide.ReportPeptideComparatorFactory;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.SortOrder;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.score.comparator.RankCalculator;
import de.mpc.pia.modeller.score.comparator.ScoreComparator;


/**
 * Modeller for peptide related stuff.
 * 
 * @author julian
 *
 */
public class PeptideModeller {
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(PeptideModeller.class);
	
	
	/** the used {@link PSMModeller} */
	private PSMModeller psmModeller;
	
	/** maps from the file ID to the List of {@link ReportPeptide}s */
	private Map<Long, List<ReportPeptide>> fileReportPeptides;
	
	/** the list of filters applied to the data, mapped by the file ID */
	private Map<Long, List<AbstractFilter>> fileFiltersMap;
	
	/** maps from the fileID to whether the peptides need to be inferred on next call (not set means, they have to be inferred) */
	private Map<Long, Boolean> inferePeptides;
	
	
	/** whether to consider the modifications for building peptides or not */
	private boolean considerModifications;
	
	
	/** the default value for considering the modifications */
	public static final boolean considerModificationsDefault = false;
	
	
	
	
	public PeptideModeller(PSMModeller psmModeller) {
		this.psmModeller = psmModeller;
		
		if (psmModeller == null) {
			throw new IllegalArgumentException("The given PSMModeller is null!");
		}
		
		fileReportPeptides = new HashMap<Long, List<ReportPeptide>>();
		fileFiltersMap = new HashMap<Long, List<AbstractFilter>>();
		inferePeptides = new HashMap<Long, Boolean>();
		
		// TODO: default value for considerModifications should be loaded from ini-file
		this.considerModifications = considerModificationsDefault;
	}
	
	
	/**
	 * Getter for the files used in the PIA intermediate file, including the
	 * pseudo-overview-file.<br/>
	 * The files are directly taken from the {@link PSMModeller}.
	 * 
	 * @return
	 */
	public Map<Long, PIAInputFile> getFiles() {
		return psmModeller.getFiles();
	}
	
	
	/**
	 * Getter for the shortNames of all scores of the given file
	 * 
	 * @param fileID
	 * @return
	 */
	public List<String> getScoreShortNames(Long fileID) {
		List<String> scoreShortNames = new ArrayList<String>();
		
		if (fileID > 0) {
			for (String scoreShort : psmModeller.getScoreShortNames(fileID)) {
				if (!scoreShortNames.contains(scoreShort)) {
					scoreShortNames.add(scoreShort);
				}
			}
		} else {
			for (Long file : psmModeller.getFiles().keySet()) {
				for (String scoreShort : psmModeller.getScoreShortNames(file)) {
					if (!scoreShortNames.contains(scoreShort)) {
						scoreShortNames.add(scoreShort);
					}
				}
			}
		}
		
		return scoreShortNames;
	}
	
	
	/**
	 * Infere the peptides for the file given by its ID with the PSMs taken
	 * from the {@link PSMModeller}. This takes any given filtering into
	 * account.
	 * 
	 * @param fileID
	 */
	private void inferePeptides(Long fileID) {
		logger.info("Inferring peptides for " + fileID  +
				" considerModifications=" + considerModifications);
		// first put the PSMs sorted by their stringID (this defines a peptide) into a Map 
		Map<String, ReportPeptide> peptides = new HashMap<String, ReportPeptide>();
		
		// take the (filtered) PSMs from the psmModeller
		List<PSMReportItem> reportPSMs;
		if (!fileID.equals(0L)) {
			reportPSMs = new ArrayList<PSMReportItem>(
					psmModeller.getFilteredReportPSMs(fileID, getFilters(fileID)));
		} else {
			reportPSMs = new ArrayList<PSMReportItem>(
					psmModeller.getFilteredReportPSMSets(getFilters(fileID)));
		}
		
		for (PSMReportItem psm : reportPSMs) {
			String idString =
					ReportPeptide.createStringID(psm, considerModifications);
			ReportPeptide repPeptide = peptides.get(idString);
			if (repPeptide == null) {
				repPeptide = new ReportPeptide(psm.getSequence(), idString,
						psm.getPeptide());
				peptides.put(idString, repPeptide);
			}
			repPeptide.addPSM(psm);
		}
		
		// create a List of the Map
		List<ReportPeptide> repList = new ArrayList<ReportPeptide>(peptides.size());
		for (Map.Entry<String, ReportPeptide> repIt : peptides.entrySet()) {
			repList.add(repIt.getValue());
		}
		
		// put this new list into the peptides' list
		fileReportPeptides.put(fileID, repList);
		// this file is set
		inferePeptides.put(fileID, false);
		logger.info("Inferred " + repList.size() + " peptides for " + fileID);
	}
	
	
	/**
	 * Returns the filters set for the given file.
	 * @param fileID
	 * @return
	 */
	public List<AbstractFilter> getFilters(Long fileID) {
		List<AbstractFilter> filters = fileFiltersMap.get(fileID);
		if (filters == null) {
			filters = new ArrayList<AbstractFilter>();
			fileFiltersMap.put(fileID, filters);
		}
		
		return filters;
	}
	
	
	/**
	 * Add a new filter for the given file
	 */
	public boolean addFilter(Long fileID, AbstractFilter newFilter) {
		if (newFilter != null) {
			boolean ok = getFilters(fileID).add(newFilter);
			if (ok) {
				inferePeptides.put(fileID, true);
			}
			return ok;
		} else {
			return false;
		}
	}
	
	
	/**
	 * Removes the filter from the given file at the given index.
	 * @param fileID
	 * @param removingIndex
	 * @return
	 */
	public AbstractFilter removeFilter(Long fileID, int removingIndex) {
		List<AbstractFilter> filters = getFilters(fileID);
		
		if ((removingIndex >= 0) &&
				(filters != null) &&
				(removingIndex < filters.size())) {
			AbstractFilter removed = filters.remove(removingIndex);
			if (removed != null) {
				inferePeptides.put(fileID, true);
			}
			return removed;
		}
		
		return null;
	}
	
	
	/**
	 * Removes all given filters.
	 */
	public void removeAllFilters() {
		fileFiltersMap.clear();
		
		// everything should be new inferred
		inferePeptides.clear();
	}
	
	
	/**
	 * Getter for considerModifications
	 * @return
	 */
	public Boolean getConsiderModifications() {
		return considerModifications;
	}
	
	
	/**
	 * Setter for considerModifications
	 * @return
	 */
	public void setConsiderModifications(boolean considerMods) {
		this.considerModifications = considerMods;
	}
	
	
	/**
	 * Applies the general settings and sets the inference of peptides on next
	 * call.
	 * 
	 * @param considerModifications
	 */
	public void applyGeneralSettings(boolean considerModifications) {
		if (this.considerModifications != considerModifications) {
			this.considerModifications = considerModifications;
			// clearing the values means, the peptides for all files and the 
			// overview should be inferred
			inferePeptides.clear();
		}
	}
	
	
	/**
	 * Returns a List of {@link ReportPeptide}s for the given fileID filtered
	 * by the given filters.
	 * 
	 * @param fileID
	 * @param filters
	 * @return
	 */
	public List<ReportPeptide> getFilteredReportPeptides(Long fileID,
			List<AbstractFilter> filters) {
		Boolean infere = inferePeptides.get(fileID);
		if ((infere == null) || infere) {
			inferePeptides(fileID);
		}
		
		if (fileReportPeptides.containsKey(fileID)) {
			return FilterFactory.applyFilters(fileReportPeptides.get(fileID),
					filters, fileID);
		} else {
			logger.error("There are no ReportPeptides for the fileID " + fileID);
			return new ArrayList<ReportPeptide>(0);
		}
	}
	
	
	/**
	 * Resorts the file report with the given sorting parameters
	 */
	public void sortReport(Long fileID, List<String> sortOrders,
			Map<String, SortOrder> sortables) {
		List<Comparator<ReportPeptide>> compares =
				new ArrayList<Comparator<ReportPeptide>>();
		
		for (String sortKey : sortOrders) {
			compares.add( ReportPeptideComparatorFactory.getComparatorByName(
					sortKey,
					sortables.get(sortKey))
				);
		}
		
		if (fileReportPeptides.get(fileID) != null) {
			Collections.sort(fileReportPeptides.get(fileID),
					ReportPeptideComparatorFactory.getComparator(compares));
		}
	}
	
	
	/**
	 * Returns a List of scoreShortNames of available Scores for ranking.<br/>
	 * Same as {@link PSMModeller#getFilesAvailableScoreShortsForRanking(Long)}.
	 * 
	 * @param fileID
	 * @return
	 */
	public List<String> getFilesAvailableScoreShortsForRanking(Long fileID) {
		List<String> rankingScoreNames = getScoreShortNames(fileID);
		
		if (rankingScoreNames.size() < 1) {
			logger.error("No scores available for ranking for the file with ID "+fileID);
		}
		
		return rankingScoreNames;
	}
	
	
	/**
	 * Calculates the ranking for the given file and rankableShortName. If the
	 * filter List is not null or empty, the Report is filtered before ranking.
	 */
	public void calculateRanking(Long fileID, String rankableShortName,
			List<AbstractFilter> filters) {
		if ((rankableShortName == null) || rankableShortName.trim().equals("")) {
			logger.error("No score shortName given for ranking calculation.");
			return;
		}
		
		// first, dump all prior ranking
		List<ReportPeptide> reports = fileReportPeptides.get(fileID);
		if (reports != null) {
			for (ReportPeptide pep : reports) {
				pep.setRank(-1L);
			}
		}
		
		// calculate the new ranking
		RankCalculator.calculateRanking(rankableShortName,
				FilterFactory.applyFilters(
						fileReportPeptides.get(fileID),
						filters, fileID),
				new ScoreComparator<ReportPeptide>(rankableShortName));
	}
	
	
	
    /**
     * Writes the peptide report for the file with the given ID and optionally
     * filtered with the filters of this file in a loose CSV format.
     * 
     * @param writer
     * @param fileID
     * @param filterExport
     * @param oneAccessionPerLine
     * @throws IOException
     */
	public void exportCSV(Writer writer, Long fileID, boolean filterExport,
			boolean oneAccessionPerLine, boolean includePSMSets,
			boolean includePSMs) throws IOException {
		boolean includes = includePSMSets || includePSMs;
		List<ReportPeptide> report = getFilteredReportPeptides(fileID,
				filterExport ? getFilters(fileID) : null);
		List<String> scoreShorts = getScoreShortNames(fileID);
		
		// write header information
		if (includes) {
			writer.append("\"COLS_PEPTIDES\";");
		}
		
		writer.append("\"sequence\";");
		
		if (considerModifications) {
			writer.append("\"modifications\";");
		}
		
		if (oneAccessionPerLine) {
			writer.append("\"accession\";");
		} else {
			writer.append("\"accessions\";");
		}
		
		writer.append(
				"\"#spectra\";" +
				"\"#PSM sets\";");
		
		if (fileID > 0) {
			writer.append("\"missed\";");
		}
		
		writer.append("\"unique\";");
		
		for (String scoreShort : scoreShorts) {
			writer.append("\"best_" + scoreShort + "\";");
		}
		
		writer.append("\n");
		
		if (includePSMSets) {
			writer.append(
					"\"COLS_PSMSET\";"+
					"\"sequence\";");
			
			if (considerModifications) {
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
					"\"scores\";" +
					"\n"
					);
		}
		
		if (includePSMs) {
			writer.append(
					"\"COLS_PSM\";"+
					"\"filename\";" +
					"\"sequence\";");
			
			if (considerModifications) {
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

		// write out peptide information
		for (ReportPeptide peptide : report) {
			StringBuilder lineFirst = new StringBuilder(64);	// first part of the line, up to the accession(s)
			StringBuilder lineLast = new StringBuilder(64);		// last part of the line, from the accession(s) to end
			if (includes) {
				lineFirst.append("\"PEPTIDE\";");
			}
			
			lineFirst.append("\"" + peptide.getSequence() + "\";");
			
			if (considerModifications) {
				lineFirst.append("\"" +
						peptide.getPSMs().get(0).getModificationsString() +
						"\";");
			}
			
			lineLast.append(
					"\"" + peptide.getNrSpectra() + "\";" +
					"\"" + peptide.getNrPSMs() + "\";");
			
			
			if (fileID > 0) {
				lineLast.append("\"" + peptide.getMissedCleavages() + "\";");
			}
			
			if (peptide.getAccessions().size() > 1) {
				lineLast.append("\"false\";");
			} else {
				lineLast.append("\"true\";");
			}
			
			for (String scoreShort : scoreShorts) {
				lineLast.append("\"" + peptide.getBestScore(scoreShort) + "\";");
			}
			
			// either cumulate the accessions or write out one line per accession
			if (oneAccessionPerLine) {
				for (Accession acc : peptide.getAccessions()) {
					writer.append(lineFirst);
					writer.append("\"" + acc.getAccession() + "\";");
					writer.append(lineLast);
					writer.append("\n");
				}
			} else {
				StringBuilder accessionsSB = new StringBuilder(64);
				for (Accession acc : peptide.getAccessions()) {
					if (accessionsSB.length() > 0) {
						accessionsSB.append(",");
					}
					
					accessionsSB.append(
							URLEncoder.encode(acc.getAccession(), "UTF-8").
								replace("+", "%20"));
				}
				
				writer.append(lineFirst);
				writer.append("\"" + accessionsSB + "\";");
				writer.append(lineLast);
				writer.append("\n");
			}
			
			if (includePSMSets || includePSMs) {
				
				for (PSMReportItem psm : peptide.getPSMs()) {
					StringBuilder row = new StringBuilder();
					
					row.append("\"" + psm.getSequence() + "\";");
					
					if (considerModifications) {
						row.append("\""  + psm.getModificationsString() + "\";");
					}
					
					if (psm instanceof ReportPSMSet) {
						row.append("\"" +
								((ReportPSMSet) psm).getPSMs().size() + "\";"); 
					}
					
					row.append("\"" + psm.getCharge() + "\";" +
							"\"" + psm.getMassToCharge() + "\";" +
							"\"" + psm.getDeltaMass() + "\";" +
							"\"" + psm.getDeltaPPM() + "\";" +
							"\"" + psm.getRetentionTime() + "\";" +
							"\"" + psm.getMissedCleavages() + "\";" +
							"\"" + psm.getSourceID() + "\";" +
							"\"" + psm.getSpectrumTitle() + "\";" +
							"\"" + psm.getScoresString() + "\";");
					
					
					if (psm instanceof ReportPSM) {
						// write the input file name before the remaining row
						if (includePSMs) {
							writer.append("\"PSM\";\"" +
									((ReportPSM) psm).getInputFileName() + "\";");
							writer.append(row.toString());
							writer.append("\n");
						}
					} else if (psm instanceof ReportPSMSet) {
						if (includePSMSets) {
							writer.append("\"PSMSET\";");
							writer.append(row.toString());
							writer.append("\n");
						}
						
						// include the PSMs, if set
						if (includePSMs) {
							for (ReportPSM p : ((ReportPSMSet) psm).getPSMs()) {
								writer.append("\"PSM\";\"" +
										((ReportPSM) p).getInputFileName() + "\";" +
										"\"" + p.getSequence() + "\";");
								
								if (considerModifications) {
									writer.append("\""  + p.getModificationsString() + "\";");
								}
								
								writer.append("\"" + p.getCharge() + "\";" +
										"\"" + p.getMassToCharge() + "\";" +
										"\"" + p.getDeltaMass() + "\";" +
										"\"" + p.getDeltaPPM() + "\";" +
										"\"" + p.getRetentionTime() + "\";" +
										"\"" + p.getMissedCleavages() + "\";" +
										"\"" + p.getSourceID() + "\";" +
										"\"" + p.getSpectrumTitle() + "\";" +
										"\"" + p.getScoresString() + "\";");
								writer.append("\n");
							}
						}
					}
					
				}
				
			}
		}
		
		writer.flush();
	}
	
	
	/**
	 * Processes the command line on the peptide level.
	 * 
	 * @param model
	 * @param commands
	 * @return
	 */
	public static boolean processCLI(PeptideModeller model, String[] commands) {
		if (model == null) {
			logger.error("No peptide modeller given while processing CLI " +
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
				PeptideExecuteCommands.valueOf(command).execute(model, params);
			} catch (IllegalArgumentException e) {
				logger.error("Could not process unknown call to " +
						command);
			}
		}
		
		return true;
	}
}
