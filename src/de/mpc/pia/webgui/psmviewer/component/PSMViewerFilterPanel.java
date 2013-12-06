package de.mpc.pia.webgui.psmviewer.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import de.mpc.pia.modeller.PSMModeller;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.report.filter.psm.ChargeFilter;
import de.mpc.pia.modeller.report.filter.psm.DeltaMassFilter;
import de.mpc.pia.modeller.report.filter.psm.DeltaPPMFilter;
import de.mpc.pia.modeller.report.filter.psm.MZFilter;
import de.mpc.pia.modeller.report.filter.psm.NrAccessionsPerPSMFilter;
import de.mpc.pia.modeller.report.filter.psm.NrPSMsPerPSMSetFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMAccessionsFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMDescriptionFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMFileListFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMMissedCleavagesFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMModificationsFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMRankFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMScoreFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMSequenceFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMTopIdentificationFilter;
import de.mpc.pia.modeller.report.filter.psm.SourceIDFilter;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.webgui.psmviewer.PSMViewer;



public class PSMViewerFilterPanel {
	
	/** the {@link PSMModeller} of the {@link PSMViewer}*/
	private PSMModeller psmModeller;
	
	/** the number of the current file */
	private Long fileID;
	
	
	/** short name of the new filter */
	private String filterShort;
	
	/** should this new filter be a negated filter or not */
	private boolean negate;
	
	/** the new comparator value */
	private String comparator;
	
	/** the new input value*/
	private String input;
	
	/** just some output (if the value was not parseable...) */
	private String validationText;
	
	/** index of the filter to be removed */
	private int removingIndex;
	
	/** whether the file with the given ID needs a recaching of the report, null means new filtering is needed. */
	private Map<Long, Boolean> fileNeedsRecaching;
	
	
	
	/**
	 * Basic constructor.
	 * @param psmModeller
	 */
	public PSMViewerFilterPanel(PSMModeller psmModeller) {
		this.psmModeller = psmModeller;
		fileID = 0L;
		
		filterShort = null;
		comparator = null;
		input = "";
		validationText = "";
		removingIndex = -1;
		
		fileNeedsRecaching = new HashMap<Long, Boolean>();
	}
	
	
	/**
	 * Updates the data for the panel with date for the given file.
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
		String name = psmModeller.getFiles().get(fileID).getName();
		
		if (name == null) {
			name = psmModeller.getFiles().get(fileID).getFileName();
		}
		
		return name;
	}
	
	
	/**
	 * Setter for the shortName of a new filter.
	 * @param filterShort
	 */
	public void setFilterShort(String filterShort) {
		this.filterShort = filterShort;
	}
	
	
	/**
	 * Getter for the shortName of a new filter.
	 * @param filterShort
	 */
	public String getFilterShort() {
		return filterShort;
	}
	
	
	/**
	 * Setter for the negate of a new filter.
	 * @param filterShort
	 */
	public void setFilterNegate(boolean negate) {
		this.negate = negate;
	}
	
	
	/**
	 * Getter for the negate of a new filter.
	 * @param filterShort
	 */
	public boolean getFilterNegate() {
		return negate;
	}
	
	
	/**
	 * Getter for the available filter comparators of a new filter.
	 * @param filterShort
	 */
	public List<SelectItem> getFilterComparators() {
		List<SelectItem> arguments = new ArrayList<SelectItem>();
		
		for (FilterComparator arg
				: FilterFactory.getAvailableComparators(getFilterShort())) {
			arguments.add(new SelectItem(arg.getName(), arg.getLabel()));
		}
		
		return arguments;
	}
	
	
	/**
	 * Setter for the comparator of a new filter.
	 * @param filterShort
	 */
	public void setComparator(String argument) {
		this.comparator = argument;
	}
	
	
	/**
	 * Getter for the comparator of a new filter.
	 * @param filterShort
	 */
	public String getComparator() {
		return comparator;
	}
	
	
	/**
	 * Setter for the input value of a new filter.
	 * @param filterShort
	 */
	public void setInput(String input) {
		this.input = input;
	}
	
	
	/**
	 * Getter for the input value of a new filter.
	 * @param filterShort
	 */
	public String getInput() {
		return input;
	}
	
	
	/**
	 * Add a new filter for the current file with
	 */
	public void addFilter() {
		StringBuffer messageBuffer = new StringBuffer();
		
		// we have a valid value, so go on
		AbstractFilter newFilter = FilterFactory.newInstanceOf(filterShort,
				comparator, input, negate, messageBuffer);
		
		if (psmModeller.addFilter(fileID, newFilter)) {
			// reset the new-filter settings
			filterShort = null;
			comparator = null;
			input = "";
			
			fileNeedsRecaching.put(fileID, true);
			
			// give some notice
			validationText = "new filter added";
		} else {
			validationText = messageBuffer.toString();
		}
	}
	
	
	/**
	 * Getter for the filters of the current file
	 * @return
	 */
	public List<AbstractFilter> getFilters() {
		return getFilters(fileID);
	}
	
	
	/**
	 * Getter for the filters of the file given by the fileID
	 * @return
	 */
	public List<AbstractFilter> getFilters(Long fileID) {
		return psmModeller.getFilters(fileID);
	}
	
	
	/**
	 * Getter for the messages / validation texts
	 * @return
	 */
	public String getValidationText() {
		return validationText;
	}
	
	
	/**
	 * Remove the filter with the current index from the filters of the current
	 * file.
	 */
	public void removeFilter() {
		psmModeller.removeFilter(fileID, removingIndex);
		fileNeedsRecaching.put(fileID, true);
	}
	
	
	/**
	 * Sets the index of the filter, which should be removed.
	 * @param idx
	 */
	public void setRemovingIndex(int idx) {
		removingIndex = idx;
	}
	
	
	/**
	 * Returns a List of SelectItems representing the available filters for this
	 * file.
	 * 
	 * @return
	 */
	public List<SelectItem> getFilterTypes() {
		List<SelectItem> filters = new ArrayList<SelectItem>();
		
		filters.add(new SelectItem(PSMAccessionsFilter.shortName(), PSMAccessionsFilter.filteringName()));
		filters.add(new SelectItem(ChargeFilter.shortName(), ChargeFilter.filteringName()));
		filters.add(new SelectItem(DeltaMassFilter.shortName(), DeltaMassFilter.filteringName()));
		filters.add(new SelectItem(DeltaPPMFilter.shortName(), DeltaPPMFilter.filteringName()));
		filters.add(new SelectItem(PSMDescriptionFilter.shortName(), PSMDescriptionFilter.filteringName()));
		filters.add(new SelectItem(PSMMissedCleavagesFilter.shortName(), PSMMissedCleavagesFilter.filteringName()));
		filters.add(new SelectItem(PSMModificationsFilter.shortName(), PSMModificationsFilter.filteringName()));
		filters.add(new SelectItem(MZFilter.shortName(), MZFilter.filteringName()));
		filters.add(new SelectItem(PSMRankFilter.shortName(), PSMRankFilter.filteringName()));
		filters.add(new SelectItem(PSMSequenceFilter.shortName(), PSMSequenceFilter.filteringName()));
		filters.add(new SelectItem(SourceIDFilter.shortName(), SourceIDFilter.filteringName()));
		filters.add(new SelectItem(NrAccessionsPerPSMFilter.shortName(), NrAccessionsPerPSMFilter.filteringName()));
		
		if (fileID > 0) {
			for (String scoreShort : psmModeller.getScoreShortNames(fileID)) {
				// add the score filter
				String[] filterNames = PSMScoreFilter.getShortAndFilteringName(scoreShort);
				if (filterNames != null) {
					filters.add(new SelectItem(filterNames[0], filterNames[1]));
				}
				
				// add the top identifications filter, but not for FDR scores
				if (!scoreShort.equals(ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName()) &&
						!scoreShort.equals(ScoreModelEnum.AVERAGE_FDR_SCORE.getShortName()) &&
						!scoreShort.equals(ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName())) {
					filterNames = PSMTopIdentificationFilter.getShortAndFilteringName(scoreShort);
					if (filterNames != null) {
						filters.add(new SelectItem(filterNames[0], filterNames[1]));
					}
				}
			}
		} else {
			filters.add(new SelectItem(PSMFileListFilter.shortName(), PSMFileListFilter.filteringName()));
			filters.add(new SelectItem(NrPSMsPerPSMSetFilter.shortName(), NrPSMsPerPSMSetFilter.filteringName()));
			
			if (psmModeller.isCombinedFDRScoreCalculated()) {
				filters.add(new SelectItem(PSMScoreFilter.prefix+ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName(),
						ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getName()));
			}
			
			// TODO: add the PSM scores, the filtering should work now
			filters.add(new SelectItem(PSMScoreFilter.prefix+ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName(),
					ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getName() + " (PSM level)"));
		}
		
		return filters;
	}
	
	
	/**
	 * Returns whether the file with the given ID needs new caching since the
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
