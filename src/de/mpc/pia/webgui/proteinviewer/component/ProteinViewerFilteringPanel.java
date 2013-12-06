package de.mpc.pia.webgui.proteinviewer.component;


import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import de.mpc.pia.modeller.ProteinModeller;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.report.filter.peptide.NrPSMsPerPeptideFilter;
import de.mpc.pia.modeller.report.filter.protein.NrPSMsPerProteinFilter;
import de.mpc.pia.modeller.report.filter.protein.NrPeptidesPerProteinFilter;
import de.mpc.pia.modeller.report.filter.protein.NrSpectraPerProteinFilter;
import de.mpc.pia.modeller.report.filter.protein.ProteinAccessionsFilter;
import de.mpc.pia.modeller.report.filter.protein.ProteinModificationsFilter;
import de.mpc.pia.modeller.report.filter.protein.ProteinRankFilter;
import de.mpc.pia.modeller.report.filter.protein.ProteinScoreFilter;
import de.mpc.pia.modeller.report.filter.protein.SequenceListFilter;
import de.mpc.pia.webgui.proteinviewer.ProteinViewer;


/**
 * Helper class to handle the filtering panel and the filtering in a
 * {@link ProteinViewer}.
 * 
 * @author julian
 *
 */
public class ProteinViewerFilteringPanel {
	
	/** the {@link ProteinModeller} of the {@link ProteinViewer} */
	private ProteinModeller proteinModeller;
	
	
	/** short name of the new filter */
	private String filterShort;
	
	/** whether the new filter should be negated */
	private boolean negate;
	
	/** the comparator of the new filter */
	private String comparator;
	
	/** the input value of the new filter */
	private String input;
	
	/** just some output (if the value was not parsable...) */
	private String messageText;
	
	/** index of the filter to be removed */
	private int removingIndex;
	
	/** whether the reported proteins need recaching of the report, null means new filtering is needed. */
	private Boolean needsRecaching;
	
	
	/**
	 * Basic constructor
	 */
	public ProteinViewerFilteringPanel(ProteinModeller modeller) {
		this.proteinModeller = modeller;
		
		filterShort = null;
		comparator = null;
		input = "";
		messageText = "";
		removingIndex = -1;
		
		needsRecaching = true;
	}
	
	
	/**
	 * Setter for the short name of the new filter.
	 * 
	 * @param filterShort
	 */
	public void setFilterShort(String filterShort) {
		this.filterShort = filterShort;
	}
	
	
	/**
	 * Getter for the short name of the new filter.
	 * 
	 * @param filterShort
	 */
	public String getFilterShort() {
		return filterShort;
	}
	
	
	/**
	 * Returns a List of SelectItems representing the available filters for this
	 * file.
	 * 
	 * @return
	 */
	public List<SelectItem> getFilterTypes() {
		List<SelectItem> filters = new ArrayList<SelectItem>();
		
		filters.add(new SelectItem(ProteinAccessionsFilter.shortName(),
				ProteinAccessionsFilter.filteringName()));
		filters.add(new SelectItem(ProteinModificationsFilter.shortName(),
				ProteinModificationsFilter.filteringName()));
		filters.add(new SelectItem(NrSpectraPerProteinFilter.shortName(),
				NrSpectraPerProteinFilter.filteringName()));
		filters.add(new SelectItem(NrPSMsPerProteinFilter.shortName(),
				NrPSMsPerProteinFilter.filteringName()));
		filters.add(new SelectItem(NrPSMsPerPeptideFilter.shortName(),
				NrPSMsPerPeptideFilter.filteringName()));
		filters.add(new SelectItem(NrPeptidesPerProteinFilter.shortName(),
				NrPeptidesPerProteinFilter.filteringName()));
		filters.add(new SelectItem(ProteinRankFilter.shortName(),
				ProteinRankFilter.filteringName()));
		filters.add(new SelectItem(SequenceListFilter.shortName(),
				SequenceListFilter.filteringName()));
		
		filters.add(new SelectItem(ProteinScoreFilter.shortName,
				ProteinScoreFilter.filteringName));
		
		return filters;
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
	public void setComparator(String comparator) {
		this.comparator = comparator;
	}
	
	
	/**
	 * Getter for the comparator of the new filter
	 * @param comparator
	 */
	public String getcomparator() {
		return comparator;
	}
	
	
	/**
	 * Get a List of the available {@link FilterComparator}s for the selected
	 * filter.
	 * 
	 * @return
	 */
	public List<SelectItem> getFilterComparators() {
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
	public void setInput(String input) {
		this.input = input;
	}
	
	
	/**
	 * Getter for the input value of the new filter.
	 * 
	 * @return
	 */
	public String getInput() {
		return input;
	}
	
	
	/**
	 * Adds the new filter to the List of filters for this file.
	 */
	public void addFilter() {
		StringBuffer messageBuffer = new StringBuffer();
		
		AbstractFilter newFilter = FilterFactory.newInstanceOf(filterShort,
				comparator, input, negate, messageBuffer);
		
		if (proteinModeller.addReportFilter(newFilter)) {
			// reset the new-filter settings
			filterShort = null;
			comparator = null;
			input = "";
			needsRecaching = true;
			
			// return some notice
			messageText = "new filter added";
		} else {
			messageText = messageBuffer.toString();
		}
	}
	
	
	/**
	 * Getter for the message text.
	 * @return
	 */
	public String getMessageText() {
		return messageText;
	}
	
	
	/**
	 * Gets the applied filters
	 */
	public List<AbstractFilter> getFilters() {
		return proteinModeller.getReportFilters();
	}
	
	
	/**
	 * Removes the filter given by the removingIndex
	 */
	public void removeFilter() {
		needsRecaching = true;
		proteinModeller.removeReportFilter(removingIndex);
	}
	
	
	/**
	 * Sets the removingIndex (used to remove a filter)
	 * @param idx
	 */
	public void setRemovingIndex(int idx) {
		removingIndex = idx;
	}

	
	/**
	 * Returns whether the protein report needs new caching since the
	 * last call of {@link #gotCachedDataForFile(Long)}, because the filters
	 * were changed in the meantime.<br/>
	 * @param fileID
	 * @return
	 */
	public Boolean getNeedsRecaching() {
		return needsRecaching;
	}
	
	
	/**
	 * Sets for the protein report, that the current data is obtained filtered.
	 * @param fileID
	 */
	public void gotCachedData() {
		needsRecaching = false;
	}
}
