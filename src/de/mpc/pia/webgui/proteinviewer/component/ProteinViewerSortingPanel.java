package de.mpc.pia.webgui.proteinviewer.component;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.ReportProteinComparatorFactory;
import de.mpc.pia.modeller.report.SortOrder;
import de.mpc.pia.webgui.proteinviewer.ProteinViewer;


/**
 * Helper class to handle the sorting panel and the sortings in a
 * {@link ProteinViewer}
 * 
 * @author julian
 *
 */
public class ProteinViewerSortingPanel {
	
	/** the available sortings with current settings */
	private Map<String, SortOrder> sortings;
	
	/** the actual order of the sortings */
	private List<String> sortPriorities;
	
	
	
	/**
	 * Basic constructor
	 */
	public ProteinViewerSortingPanel() {
		// initialise the sortables
		sortings = ReportProteinComparatorFactory.getInitialSortOrders();
		sortPriorities = new ArrayList<String>();
	}
	

	/**
	 * Gets the sortings.
	 * @return
	 */
	public Map<String, SortOrder> getSortOrders() {
		return sortings;
	}
    
    
	/**
	 * Gets the priorities of the sorting.
	 * @return
	 */
	public List<String> getSortPriorities() {
		return sortPriorities;
	}
	
	
	/**
	 * Returns a list of String representatives for the sortings in the correct
	 * order. This is only for presentation in the viewer.
	 * 
	 * @return
	 */
	public List<String> getSortingsStrings() {
		List<String> sorts = new ArrayList<String>();
		
		if (sortPriorities.size() > 0) {
			for (String order : sortPriorities) {
				sorts.add(order + " (" + sortings.get(order) + ")");
			}
		} else {
			sorts.add("(no sorting)");
		}
		
		return sorts;
	}
	
	
	/**
	 * Changes the sorting of the property.<br />
	 * The order of the sorting is unsorted, ascending, descending, unsorted...
	 * 
	 * @param fileID
	 * @param property
	 */
	public void changeSorting(String property) {
		for (Map.Entry<String, SortOrder> entry : sortings.entrySet()) {
			SortOrder newOrder;
			
			if (entry.getKey().equals(property)) {
			    if (entry.getValue() == SortOrder.ascending) {
			        newOrder = SortOrder.descending;
			    } else if (entry.getValue() == SortOrder.descending) {
			        newOrder = SortOrder.unsorted;
			    } else {
			        newOrder = SortOrder.ascending;
			    }
			    
			    entry.setValue(newOrder);
			    
			    if (sortPriorities.contains(entry.getKey())) {
			    	// the sortings was already there -> remove it
			    	sortPriorities.remove(entry.getKey());
			    }
			    
			    if (newOrder != SortOrder.unsorted) {
			    	// the order is now not unsorted -> put it in front of all sortings
			    	sortPriorities.add(0, entry.getKey());
			    }
			}
		}
	}
	
	
	/**
	 * Changes the sorting of the property.<br />
	 * The order of the sorting is unsorted, ascending, descending, unsorted...<br/>
	 * This is only for convenience of the pia:sortLink as the fileID is always
	 * 0 for the overview.
	 * 
	 * @param fileID
	 * @param property
	 */
	public void changeSorting(Long fileID, String property) {
		changeSorting(property);
	}
	
	
	/**
	 * Returns a comparator for the given property.
	 * 
	 * @param property
	 * @return
	 */
	public Comparator<ReportProtein> getProteinComparator(String property) {
		Comparator<ReportProtein> comparator =
				ReportProteinComparatorFactory.getComparatorByName(property,
						SortOrder.ascending);
		
		return comparator;
    }
	
	
	/**
	 * Returns the order of the property, i.e. unsorted, ascending or
	 * descending.
	 * 
	 * @param property
	 * @return
	 */
	public String getOrder(String property) {
		SortOrder sorting = sortings.get(property);
		
		if (sorting == null) {
			sorting = SortOrder.unsorted;
		}
		
		return sorting.toString();
	}
	
	
	/**
	 * Returns the order of the property for the given file, i.e. unsorted,
	 * ascending or descending.<br/>
	 * This is only for convenience of the pia:sortLink as the fileID is always
	 * 0 for the overview.
	 * 
	 * @param property
	 * @return
	 */
	public String getOrder(Long fileID, String property) {
		return getOrder(property);
	}
	
	
	/**
	 * Removes the sortings, which are lost directly after a new inference, like
	 * e.g. ranking.
	 */
	public void cleanAfterInference() {
		sortPriorities.remove(
				ReportProteinComparatorFactory.CompareType.RANK_SORT.toString());
		sortings.put(
				ReportProteinComparatorFactory.CompareType.RANK_SORT.toString(),
				SortOrder.unsorted);
	}
}
