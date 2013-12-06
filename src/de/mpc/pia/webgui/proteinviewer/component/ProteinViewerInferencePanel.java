package de.mpc.pia.webgui.proteinviewer.component;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import de.mpc.pia.modeller.ProteinModeller;
import de.mpc.pia.modeller.protein.inference.AbstractProteinInference;
import de.mpc.pia.modeller.protein.inference.ProteinInferenceFactory;
import de.mpc.pia.modeller.protein.inference.ProteinInferenceFactory.ProteinInferenceMethod;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.tools.LabelValueContainer;
import de.mpc.pia.tools.PIAConfigurationProperties;
import de.mpc.pia.webgui.proteinviewer.ProteinViewer;


/**
 * Helper class to handle the inference panel and the inference settings in a
 * {@link ProteinViewer}
 * 
 * @author julian
 *
 */
public class ProteinViewerInferencePanel {
	
	/** the {@link ProteinModeller} of the {@link ProteinViewer}*/
	private ProteinModeller proteinModeller;
	
	
	/** the scoring panel */
	private ProteinViewerScoringPanel scoringPanel;
	
	/** the sorting panel */
	private ProteinViewerSortingPanel sortingPanel;
	
	
	/** short name of the currently selected scoring */
	private String formSelectedInference;
	
	/** the inference filters, maps from (filter shortName -> filter) */
	private Map<String, AbstractProteinInference> inferenceMethods;
	
	
	/** short name of the new filter */
	private String filterShort;
	
	/** whether the new filter should be negated */
	private boolean negate;
	
	/** the comparator of the new filter */
	private String filterComparator;
	
	/** the input value of the new filter */
	private String filterInput;
	
	/** just some output (if the value was not parsable...) */
	private String filterMessageText;
	
	/** index of the filter to be removed */
	private int removingIndex;
	
	/** the thread running the inference */
	private Thread inferenceRunnerThread;
	
	/** whether the current inference is running */
	private boolean isRunning;
	
	/** whether the reported proteins need recaching of the report, null means new filtering is needed. */
	private Boolean needsRecaching;
	
	/** the PIA configurations */
    private PIAConfigurationProperties configurationProperties;
    
    
    
	/**
	 * Basic constructor
	 */
	public ProteinViewerInferencePanel(ProteinModeller proteinModeller,
			ProteinViewerScoringPanel scoringPanel,
			ProteinViewerSortingPanel sortingPanel,
			PIAConfigurationProperties configurationProperties) {
		this.proteinModeller = proteinModeller;
		this.scoringPanel = scoringPanel;
		this.sortingPanel = sortingPanel;
		
		this.configurationProperties = configurationProperties;
		
		inferenceMethods = new HashMap<String, AbstractProteinInference>();
		
		filterShort = null;
		filterComparator = null;
		filterInput = "";
		filterMessageText = "";
		removingIndex = -1;
		
		isRunning = false;
		
		inferenceRunnerThread = null;
		
		needsRecaching = true;
	}
	
	
	/**
	 * Getter for the short name of the currently selected inference.
	 * @return
	 */
	public String getSelectedInference() {
		if (formSelectedInference != null) {
			return formSelectedInference;
		} else {
			AbstractProteinInference appliedInference =
					proteinModeller.getAppliedProteinInference();
			
			if (appliedInference != null) {
				return appliedInference.getShortName();
			} else {
				// return Occam's Razor as default inference
				return ProteinInferenceFactory.ProteinInferenceMethod.
						REPORT_SPECTRUM_EXTRACTOR.getShortName();
			}
		}
	}
	
	
	/**
	 * Setter for the short name of the currently selected inference.
	 * @return
	 */
	public void setSelectedInference(String shortName) {
		formSelectedInference = shortName;
	}
	
	
	/**
	 * Getter for the name of the currently selected inference.
	 * @return
	 */
	public String getSelectedInferenceName() {
		ProteinInferenceMethod inference =
			ProteinInferenceFactory.getProteinInferenceByName(formSelectedInference);
		
		return (inference != null) ? inference.getName() : "no inference";
	}
	
	
	/**
	 * Returns a List of SelectItems for the selection of the scoring method.
	 * @return
	 */
	public List<SelectItem> getInferenceSelectItems() {
		List<SelectItem> items = new ArrayList<SelectItem>();
		
		for (Map.Entry<String, String> inferenceIt :
			ProteinInferenceFactory.getAllProteinInferenceNames().entrySet()) {
			items.add( new SelectItem(inferenceIt.getKey(),
					inferenceIt.getValue()) );
		}
		
		return items;
	}
	
	
	/**
	 * Getter for the currently selected inference method.<br />
	 * If no inference was applied yet, return the first possible.
	 * @return
	 */
	public AbstractProteinInference getInferenceMethod() {
		AbstractProteinInference inference = null;
		
		String selectedInference = getSelectedInference();
		
		if (selectedInference != null) {
			inference = inferenceMethods.get(selectedInference);
			
			if (inference == null) {
				// initialize, if the inference method is not yet in the map
				inference = ProteinInferenceFactory.createInstanceOf(selectedInference);
				inferenceMethods.put(selectedInference, inference);
				
				try {
					int nr_threads = Integer.parseInt(
							configurationProperties.getPIAProperty("nr_threads", "0"));
					inference.setAllowedThreads(nr_threads);
				} catch (NumberFormatException e) {
					// TODO: better warning
					e.printStackTrace();
				}

			}
			
			// update the scores...
			inference.setAvailableScoreShorts(
					proteinModeller.getAllScoreShortNames());
			
			// set the scoring
			inference.setScoring(scoringPanel.getScoringMethod());
		}
		
		return inference;
	}
	
	
	/**
	 * Returns a List of SelectItems representing the available filters of the
	 * selected inference method.
	 * 
	 * @return
	 */
	public List<SelectItem> getInferenceFilterTypes() {
		AbstractProteinInference inference = getInferenceMethod();
		
		List<SelectItem> filters = new ArrayList<SelectItem>();
		for (LabelValueContainer<String> container : inference.getFilterTypes()) {
			filters.add(new SelectItem(container.getLabel(),
					container.getValue()));
		}
		
		return filters;
	}
	
	
	/**
	 * Getter for the currently applied inference.
	 * @return
	 */
	public AbstractProteinInference getAppliedInference() {
		AbstractProteinInference appliedInference =
				proteinModeller.getAppliedProteinInference();
		
		return appliedInference;
	}
	
	
	/**
	 * Getter for the currently applied inference filters.
	 * 
	 * @return
	 */
	public List<AbstractFilter> getAppliedInferenceFilters() {
		AbstractProteinInference appliedInference =
				proteinModeller.getAppliedProteinInference();
		
		if (appliedInference != null) {
			return appliedInference.getFilters();
		} else {
			return new ArrayList<AbstractFilter>();
		}
	}
	
	
	/**
	 * Starts the inference in another thread, not waiting on it to finish.<br/>
	 * If a blocking method is needed, use {@link #infereProteins()}.
	 * @return
	 */
	public String startInference() {
		setInferenceRunning(true);
		
		inferenceRunnerThread = new Thread() {
			@Override
			public void run() {
				setInferenceRunning(true);
				
				AbstractProteinInference selectedInference =
						getInferenceMethod();
				
				// make a copy of the currently selected inference, before
				// applying it
				AbstractProteinInference inference =
						ProteinInferenceFactory.getProteinInferenceByName(
								selectedInference.getShortName()).
								createInstanceOf();
				for (AbstractFilter filter : selectedInference.getFilters()) {
					inference.addFilter(filter);
				}
				inference.setScoring(
						selectedInference.getScoring().smallCopy());
				
				
				proteinModeller.infereProteins(inference);
				
				scoringPanel.applyScoring();
				sortingPanel.cleanAfterInference();
				setInferenceRunning(false);
				needsRecaching = true;
			}
		};
		inferenceRunnerThread.start();
		
		return null;
	}
	
	
	/**
	 * Calculate the inference. <br/>
	 * Actually calls {@link #startInference()} and joins the started Thread,
	 * thus the 
	 */
	public void infereProteins() {
		setInferenceRunning(true);
		startInference();
		
		try {
			inferenceRunnerThread.join();
		} catch (InterruptedException e) {
			// TODO: better exception handling
			System.err.println("inferenceRunnerThread got interrupted.");
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Setter for the short name of the new filter setting.
	 * 
	 * @param filterShort
	 */
	public void setFilterShort(String filterShort) {
		this.filterShort = filterShort;
	}
	
	
	/**
	 * Getter for the short name of the new filter setting.
	 * 
	 * @param filterShort
	 */
	public String getFilterShort() {
		return filterShort;
	}
	

	/**
	 * Setter whether the new filter should be negated.
	 * 
	 * @param negate
	 */
	public void setFilterNegate(boolean negate) {
		this.negate = negate;
	}
	
	
	/**
	 * Getter whether the new filter should be negated.
	 * 
	 * @param negate
	 */
	public boolean getFilterNegate() {
		return negate;
	}
	
	
	/**
	 * Setter for the comparator of the new filter
	 * @param argument
	 */
	public void setFilterComparator(String comparator) {
		this.filterComparator = comparator;
	}
	
	
	/**
	 * Getter for the comparator of the new filter
	 * @param filterComparator
	 */
	public String getFilterComparator() {
		return filterComparator;
	}
	
	
	/**
	 * Get a List of the available {@link FilterComparator}s for the selected
	 * filter.
	 * 
	 * @return
	 */
	public List<SelectItem> getAvailableFilterComparators() {
		List<SelectItem> arguments = new ArrayList<SelectItem>();
		
		for (FilterComparator arg : FilterFactory.getAvailableComparators(filterShort)) {
			arguments.add(new SelectItem(arg.getName(), arg.getLabel()));
		}
		
		return arguments;
	}
	
	
	/**
	 * Setter for the input value of the new filter.
	 * 
	 * @param input
	 */
	public void setFilterInput(String input) {
		this.filterInput = input;
	}
	
	
	/**
	 * Getter for the input value of the new filter.
	 * 
	 * @return
	 */
	public String getFilterInput() {
		return filterInput;
	}
	
	
	/**
	 * Adds the new filter to the List of filters for this file.
	 */
	public void addFilter() {
		StringBuffer messageBuffer = new StringBuffer();
		
		AbstractFilter newFilter = FilterFactory.newInstanceOf(filterShort,
				filterComparator, filterInput, negate, messageBuffer);
		
		AbstractProteinInference inference = getInferenceMethod();
		
		if (newFilter != null) {
			// add the new filter to the filters
			inference.addFilter(newFilter);
			
			filterShort = null;
			filterComparator = null;
			filterInput = "";
			filterMessageText = "new filter added";
		} else {
			filterMessageText = messageBuffer.toString();
		}
	}
	
	
	/**
	 * Getter for the message text.
	 * @return
	 */
	public String getFilterMessageText() {
		return filterMessageText;
	}
	
	
	/**
	 * Returns a {@link List} of all filter settings for the current inference
	 * method.
	 * 
	 * @return
	 */
	public List<AbstractFilter> getInferenceFilters() {
		return getInferenceMethod().getFilters();
	}
	
	
	/**
	 * Removes the filter given by the removingIndex
	 */
	public void removeInferenceFilter() {
		getInferenceMethod().removeFilter(removingIndex);
	}
	
	
	/**
	 * Sets the removingIndex (used to remove a filter)
	 * @param idx
	 */
	public void setRemovingIndex(int idx) {
		removingIndex = idx;
	}
	
	
	/**
	 * Sets the Inference procedure to be started (as seen from outside)
	 * @param running
	 */
	private void setInferenceRunning(Boolean running) {
		this.isRunning = running;
	}
	
	
	/**
	 * Returns, whether the inference is running or not.
	 * @return
	 */
	public Boolean getInferenceRunning() {
		return isRunning;
	}
	
	
	/**
	 * Returns the Progress of the current inference method.
	 * @return
	 */
	public Long getProgressValue() {
		Long value;
		
		if (getInferenceRunning()) {
			
			if (getAppliedInference() == null) {
				// protein inference is in starting process, but not yet
				// assigned (this is a rare threading-case)
				value = Long.valueOf(0);
			} else {
				value = getAppliedInference().getProgressValue();
			}
		} else {
			if (getAppliedInference() == null) {
				// no inference applied yet
				value = Long.valueOf(-1);
			} else {
				// inference is done
				value = Long.valueOf(101);
			}
		}
		
		return value;
	}
	
	
	/**
	 * Returns whether the report needs new caching since the
	 * last call of {@link #gotCachedDataForFile(Long)}.
	 * @param fileID
	 * @return
	 */
	public Boolean getNeedsRecaching() {
		return needsRecaching;
	}
	
	
	/**
	 * Sets that the current data is obtained filtered.
	 * @param fileID
	 */
	public void gotCachedData() {
		needsRecaching = false;
	}
}