package de.mpc.pia.modeller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpc.pia.JsonAnalysis;
import de.mpc.pia.intermediate.Group;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.ReportProteinComparatorFactory;
import de.mpc.pia.modeller.protein.inference.AbstractProteinInference;
import de.mpc.pia.modeller.protein.inference.ProteinInferenceFactory;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.protein.scoring.ProteinScoringFactory;
import de.mpc.pia.modeller.report.SortOrder;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.modeller.score.comparator.RankCalculator;


/**
 * Modeller for protein related stuff.
 *
 * @author julianu
 *
 */
public class ProteinModeller  implements Serializable {

    private static final long serialVersionUID = 7091587185721765287L;


    /** logger for this class */
    private static final Logger LOGGER = LogManager.getLogger();


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
    private static DecoyStrategy defaultDecoyStrategy = FDRData.DecoyStrategy.ACCESSIONPATTERN;
    private static String defaultDecoyPattern = "s.*";
    private static Double defaultFDRThreshold = 0.05;


    /**
     * Basic constructor for the ProteinModeller.<br/>
     * There will be no inference, but only initialization.
     *
     * @param groups
     */
    public ProteinModeller(PSMModeller psmModeller,
            PeptideModeller peptideModeller, Map<Long, Group> groups) {
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

        this.reportFilters = new ArrayList<>();
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
        List<String> scoreShortNames = new ArrayList<>();

        // get the scores from the files
        for (Long fileID : psmModeller.getFiles().keySet()) {
            psmModeller.getScoreShortNames(fileID).stream().filter(scoreShort -> !scoreShortNames.contains(scoreShort)).forEach(scoreShortNames::add);
        }

        return scoreShortNames;
    }


    /**
     * Returns the Score name, given the scoreShortName.
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
        reportProteins = new ArrayList<>();

        if (proteinInference != null) {
            appliedProteinInference = proteinInference;
            reportProteins = proteinInference.calculateInference(
                    intermediateGroups,
                    psmModeller.getReportPSMSets(),
                    peptideModeller.getConsiderModifications(),
                    psmModeller.getPSMSetSettings(),
                    peptideModeller.getFilteredReportPeptides(0L, peptideModeller.getFilters(0L)));
        } else {
            LOGGER.error("No inference method set!");
            appliedProteinInference = null;
            reportProteins = null;
        }

        this.fdrData = new FDRData(defaultDecoyStrategy, defaultDecoyPattern,
                defaultFDRThreshold);

        // create the protein map
        reportProteinsMap = new HashMap<>();
        for (ReportProtein protein : reportProteins) {
            reportProteinsMap.put(protein.getID(), protein);

            if (!protein.getSubSets().isEmpty()) {
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
            if (protein.getRank() != null) {
                return true;
            }
        }

        return false;
    }


    /**
     * Calculates the ranking. If the filter List is not null or empty, the
     * Report is filtered before ranking.
     *
     * @param filters
     */
    public void calculateRanking(List<AbstractFilter> filters) {
        if ((appliedProteinInference == null) ||
                (appliedProteinInference.getScoring() == null)) {
            LOGGER.error("No protein inference set." +
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
                new ArrayList<>();

        for (String sortKey : sortOrders) {
            SortOrder order = sortables.get(sortKey);

            compares.add( ReportProteinComparatorFactory.getComparatorByName(
                    sortKey, order));
        }

        reportProteins.sort(ReportProteinComparatorFactory.getComparator(compares));
    }


    /**
     * Apply the given scoring to the List of {@link ReportProtein}s.
     *
     * @param scoring
     */
    public void applyScoring(AbstractScoring scoring) {
        if (scoring == null) {
            LOGGER.error("No scoring method given.");
            appliedScoringMethod = null;
            return;
        }

        LOGGER.info("applying scoring method: {}", scoring.getName());
        scoring.calculateProteinScores(reportProteins);
        LOGGER.info("scoring done");
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
     * @return
     */
    public Boolean getInternalDecoysExist() {
        for (Long fileID : psmModeller.getFiles().keySet()) {
            if ((fileID > 0) && psmModeller.getFileHasInternalDecoy(fileID).booleanValue()) {
                return true;
            }
        }

        return false;
    }


    /**
     * Updates the {@link FDRData} for the protein FDR
     *
     * @return
     */
    public void updateFDRData(DecoyStrategy decoyStrategy,
            String decoyPattern, Double fdrThreshold) {
        fdrData.setDecoyStrategy(decoyStrategy);
        fdrData.setDecoyPattern(decoyPattern);
        fdrData.setFDRThreshold(fdrThreshold);
        fdrData.setScoreShortName(ScoreModelEnum.PROTEIN_SCORE.getShortName());

        LOGGER.info("Protein FDRData set to: {}, {}, {}, {}",
                fdrData.getDecoyStrategy(), fdrData.getDecoyPattern(),
                fdrData.getFDRThreshold(), fdrData.getScoreShortName());
    }


    /**
     * Updates the decoy states of the Proteins with the current settings from
     * the FDRData.
     */
    public void updateDecoyStates() {
        LOGGER.info("updateDecoyStates");
        Pattern p = Pattern.compile(fdrData.getDecoyPattern());

        if (reportProteins != null) {
            for (ReportProtein protein : reportProteins) {
                // dump all FDR data
                protein.dumpFDRCalculation();
                protein.updateDecoyStatus(fdrData.getDecoyStrategy(), p);
            }
        } else {
            LOGGER.error("Proteins must be inferred before calling updateDecoyStates");
        }
    }


    /**
     * Calculate the protein FDR
     *
     */
    public void calculateFDR() {
        // calculate the FDR values
        fdrData.calculateFDR(reportProteins, true);
    }


    /**
     * Returns the report filters.
     * @return
     */
    public List<AbstractFilter> getReportFilters() {
        if (reportFilters == null) {
            reportFilters = new ArrayList<>();
        }

        return reportFilters;
    }


    /**
     * Add a new filter to the report filters.
     */
    public boolean addReportFilter(AbstractFilter newFilter) {
        return newFilter != null && getReportFilters().add(newFilter);
    }


    /**
     * Removes the report filter at the given index.
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
     * Adds the filters given by the array derived from parsing the json
     * 
     * @param filters
     * @param fileID
     * @return
     */
	public boolean addReportFiltersFromJSONStrings(String[] filters) {
		boolean allOk = true;
		
		for (String filter : filters) {
			StringBuilder messageBuffer = new StringBuilder();
			AbstractFilter newFilter = FilterFactory.createInstanceFromString(filter, messageBuffer);
			
			if (newFilter != null) {
				LOGGER.info("Adding filter: {}", newFilter);
				allOk |= addReportFilter(newFilter);
			} else {
				LOGGER.error("Could not create filter from string '{}': {}", filter, messageBuffer);
				allOk = false;
			}
		}
		
		return allOk;
	}


    /**
     * Returns the list of inference filters. These are not the currently set
     * filters, but a list of filters which may be used for inference.
     * @return
     */
    public List<AbstractFilter> getInferenceFilters() {
        if (inferenceFilters == null) {
            inferenceFilters = new ArrayList<>();
        }

        return inferenceFilters;
    }


    /**
     * Add a new filter to the inference filters. These are not the currently
     * set filters, but a list of filters which may be used for inference.
     */
    public boolean addInferenceFilter(AbstractFilter newFilter) {
        return newFilter != null && getInferenceFilters().add(newFilter);
    }
    
    
    /**
     * Execute analysis on protein level, getting the settings from JSON.
     * <p>
     * If a required setting is not given, the default value is used.
     */
    public boolean executeProteinOperations(JsonAnalysis json) {
    	boolean allOk = true;
    	
    	AbstractProteinInference proteinInference =
                ProteinInferenceFactory.createInstanceOf(json.getInferenceMethod());
    	
    	if (proteinInference == null) {
    		LOGGER.error("Could not create inference method '{}'", json.getInferenceMethod());
    		allOk = false;
    	} else {
    		LOGGER.info("selected inference method: {}", proteinInference.getName());
    	}
    	
    	if (allOk) {
    		allOk = proteinInference.addFiltersFromJSONStrings(json.getInferenceFilters());
    	}
    	
    	AbstractScoring proteinScoring = null;
    	if (allOk) {
        	proteinScoring = ProteinScoringFactory.getNewInstanceByName(
        			json.getScoringMethod(), Collections.emptyMap());
        	
        	if (proteinScoring == null) {
        		LOGGER.error("Could not create scoring method '{}'", json.getScoringMethod());
        		allOk = false;
        	} else {
        		LOGGER.info("selected scoring method: {}", proteinScoring.getName());
        		
        		proteinScoring.setSetting(AbstractScoring.SCORING_SETTING_ID, json.getScoringBaseScore());
                proteinScoring.setSetting(AbstractScoring.SCORING_SPECTRA_SETTING_ID, json.getScoringPSMs());
        		
        		proteinInference.setScoring(proteinScoring);
        	}
    	}
    	
    	if (allOk) {
    		infereProteins(proteinInference);
    		
    		if (json.isCalculateProteinFDR()) {
                DecoyStrategy decoyStrategy = DecoyStrategy.getStrategyByString(json.getDecoyPattern());
                updateFDRData(decoyStrategy, json.getDecoyPattern(), 0.01);
    			
    			updateDecoyStates();
                calculateFDR();
            }
    	}
    	
    	return allOk;
    }
}