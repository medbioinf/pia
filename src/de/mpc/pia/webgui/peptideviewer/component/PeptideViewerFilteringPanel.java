package de.mpc.pia.webgui.peptideviewer.component;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import de.mpc.pia.modeller.PeptideModeller;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.report.filter.peptide.NrPSMsPerPeptideFilter;
import de.mpc.pia.modeller.report.filter.peptide.NrSpectraPerPeptideFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideAccessionsFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideDescriptionFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideFileListFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideMissedCleavagesFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideModificationsFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideRankFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideScoreFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideSequenceFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideSourceIDListFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideSpectrumTitleListFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideUniqueFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMScoreFilter;
import de.mpc.pia.webgui.peptideviewer.PeptideViewer;


/**
 * Helper class to handle the filtering panel and the filtering in a
 * {@link PeptideViewer}.
 * 
 * @author julian
 *
 */
public class PeptideViewerFilteringPanel {
	
	/** the {@link PeptideModeller} of the {@link PeptideViewer} */
	private PeptideModeller peptideModeller;
	
	
	/** the ID of the current file */
	private Long fileID;
	
	/** short name of the new filter */
	private String filterShort;
	
	/** whether the new filter should be negated */
	private boolean negate;
	
	/** the comparator of the new filter */
	private String comparator;
	
	/** the input value of the new filter */
	private String input;
	
	/** just some output (if the value was not parseable...) */
	private String messageText;
	
	/** index of the filter to be removed */
	private int removingIndex;
	
	/** whether the file with the given ID needs new filtering, because the filters where changed, null means new filtering is needed. */
	private Map<Long, Boolean> fileNeedsRecaching;
	
	
	
	/**
	 * Basic constructor
	 */
	public PeptideViewerFilteringPanel(PeptideModeller modeller) {
		this.peptideModeller = modeller;
		fileID = 0L;
		
		filterShort = null;
		comparator = null;
		input = "";
		messageText = "";
		removingIndex = -1;
		
		fileNeedsRecaching = new HashMap<Long, Boolean>();
	}
	
	
	/**
	 * Updates the data for the panel with data for the given file.
	 * 
	 * @return
	 */
	public void updateFilterPanel(Long fileID) {
		this.fileID = fileID;
	}
	
	
	/**
	 * returns the name  of the currently selected file
	 * @return
	 */
	public String getName() {
		String name = peptideModeller.getFiles().get(fileID).getName();
		
		if (name == null) {
			name = peptideModeller.getFiles().get(fileID).getFileName();
		}
		
		return name;
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
		
		filters.add(new SelectItem(PeptideAccessionsFilter.shortName(),
				PeptideAccessionsFilter.filteringName()));
		filters.add(new SelectItem(PeptideDescriptionFilter.shortName(),
				PeptideDescriptionFilter.filteringName()));
		filters.add(new SelectItem(PeptideModificationsFilter.shortName(),
				PeptideModificationsFilter.filteringName()));
		filters.add(new SelectItem(NrPSMsPerPeptideFilter.shortName(),
				NrPSMsPerPeptideFilter.filteringName()));
		filters.add(new SelectItem(PeptideRankFilter.shortName(),
				PeptideRankFilter.filteringName()));
		filters.add(new SelectItem(PeptideSequenceFilter.shortName(),
				PeptideSequenceFilter.filteringName()));
		filters.add(new SelectItem(PeptideSourceIDListFilter.shortName(),
				PeptideSourceIDListFilter.filteringName()));
		filters.add(new SelectItem(PeptideSpectrumTitleListFilter.shortName(),
				PeptideSpectrumTitleListFilter.filteringName()));
		filters.add(new SelectItem(NrSpectraPerPeptideFilter.shortName(),
				NrSpectraPerPeptideFilter.filteringName()));
		filters.add(new SelectItem(PeptideUniqueFilter.shortName(),
				PeptideUniqueFilter.filteringName()));
		
		if (fileID > 0) {
			filters.add(new SelectItem(PeptideMissedCleavagesFilter.shortName(),
					PeptideMissedCleavagesFilter.filteringName()));
		} else {
			filters.add(new SelectItem(PeptideFileListFilter.shortName(),
					PeptideFileListFilter.filteringName()));
		}
		
		for (String scoreShort : peptideModeller.getScoreShortNames(fileID)) {
			String[] filterNames = PeptideScoreFilter.getShortAndFilteringName(scoreShort);
			if (filterNames != null) {
				filters.add(new SelectItem(filterNames[0], filterNames[1]));
			}
			
			filterNames = PSMScoreFilter.getShortAndFilteringName(scoreShort);
			if (filterNames != null) {
				filters.add(new SelectItem(filterNames[0], filterNames[1]));
			}
		}
		
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
		
		System.err.println("addFilter " + filterShort);
		
		if (newFilter != null) {
			peptideModeller.addFilter(fileID, newFilter);
			
			filterShort = null;
			comparator = null;
			input = "";
			
			fileNeedsRecaching.put(fileID, true);
			
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
	 * Gets the applied filters for the selectedFileTabNumber.
	 */
	public List<AbstractFilter> getFilters() {
		return peptideModeller.getFilters(fileID);
	}
	
	
	/**
	 * Gets the applied filters for the given fileID.
	 */
	public List<AbstractFilter> getFilters(Long fileID) {
		return peptideModeller.getFilters(fileID);
	}
	
	
	/**
	 * Removes the filter given by the removingIndex
	 */
	public void removeFilter() {
		fileNeedsRecaching.put(fileID, true);
		peptideModeller.removeFilter(fileID, removingIndex);
	}
	
	
	/**
	 * Sets the removingIndex (used to remove a filter)
	 * @param idx
	 */
	public void setRemovingIndex(int idx) {
		removingIndex = idx;
	}
	
	
	/**
	 * Returns whether the file with the given ID needs recaching since the
	 * last call of {@link #gotCachedDataForFile(Long)}, because the filters
	 * were changed in the meantime.<br/>
	 * @param fileID
	 * @return
	 */
	public Boolean getFileNeedsRecaching(Long fileID) {
		if (fileNeedsRecaching.get(fileID) == null) {
			fileNeedsRecaching.put(fileID, true);
		}
		return fileNeedsRecaching.get(fileID);
	}
	
	
	/**
	 * Sets for the given file, that the current data is obtained filtered.
	 * @param fileID
	 */
	public void gotCachedDataForFile(Long fileID) {
		fileNeedsRecaching.put(fileID, false);
	}
}
