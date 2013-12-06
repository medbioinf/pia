package de.mpc.pia.webgui.psmviewer.component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.modeller.PSMModeller;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.PSMReportItemComparator;
import de.mpc.pia.modeller.report.SortOrder;



/**
 * This little helper class builds the Panel for the sorting information for the
 * PSM Viewer.
 * 
 * @author julian
 *
 */
public class PSMViewerSortingPanel {
	
	/** the PSM modeller */
	private PSMModeller psmModeller;
	
	/** the ID of the current file */
	private long fileID;
	
	
	/** mapping from each fileID to the available sortings with current settings */
	private Map<Long, Map<String, SortOrder>> fileSortings;
	
	/** mapping from each fileID to the actual order of the sortings */
	private Map<Long, List<String>> fileSortPriorities;
	
	
	/** the prefix before all score tags */
	public final static String score_prefix = PSMReportItemComparator.score_prefix; 
	
	
	
	/**
	 * Basic constructor, setting everything to null.
	 */
	public PSMViewerSortingPanel(PSMModeller modeller) {
		this.psmModeller = modeller;
		fileID = 0L;
		
		// reset the sortings
		fileSortings = new HashMap<Long, Map<String, SortOrder>>();
		fileSortPriorities = new HashMap<Long, List<String>>();
		
		for (PIAInputFile file : modeller.getFiles().values()) {
			// initialise the sortings
			fileSortings.put(file.getID(), PSMReportItemComparator.getInitialSortOrders());
			fileSortPriorities.put(file.getID(), new ArrayList<String>());
			
			
			for (String scoreName : modeller.getScoreShortNames(file.getID())) {
				// scores are added with the prefix "score_"
				fileSortings.get(file.getID()).put(score_prefix + scoreName,
						SortOrder.unsorted);
			}
		}
	}
	
	
	/**
	 * updates the panel
	 * 
	 * @return
	 */
	public void updateSortingPanel(long fileID) {
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
     * Gets the sortings for the given file.
     * @return
     */
    public Map<String, SortOrder> getSortOrders(Long fileID) {
    	return fileSortings.get(fileID);
    }
    
    
    /**
     * Gets the priorities of the sorting for the given file.
     * @return
     */
    public List<String> getSortPriorities(Long fileID) {
    	return fileSortPriorities.get(fileID);
    }
    
	
	/**
	 * Returns a list of String representatives for the sortings of the
	 * selectedFileTabNumber in the correct order. This is only for presentation
	 * in the viewer.
	 * 
	 * @return
	 */
	public List<String> getSortingsString() {
		if (fileSortPriorities.get(fileID) != null) {
			List<String> orderedSortOrders = fileSortPriorities.get(fileID);
			List<String> sortings = new ArrayList<String>();
			
			if (orderedSortOrders.size() > 0) {
				for (String order : orderedSortOrders) {
					sortings.add(order + " (" + fileSortings.get(fileID).get(order) + ")");
				}
			} else {
				sortings.add("(no sorting)");
			}
			
			return sortings;
		} else {
			return new ArrayList<String>();
		}
	}
	
	
	/**
	 * Changes the sorting of the property for the given file.<br />
	 * The order of the sorting is unsorted, ascending, descending, unsorted...
	 * 
	 * @param fileID
	 * @param property
	 */
	public void changeSorting(Long fileID, String property) {
		List<String> orderedSortOrders = fileSortPriorities.get(fileID);
		for (Map.Entry<String, SortOrder> entry
				: fileSortings.get(fileID).entrySet()) {
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
			    
			    if (orderedSortOrders.contains(entry.getKey())) {
			    	// the sortings was already there -> remove it
			    	orderedSortOrders.remove(entry.getKey());
			    }
			    
			    if (newOrder != SortOrder.unsorted) {
			    	// the order is now not unsorted -> put it in front of all sortings
			    	orderedSortOrders.add(0, entry.getKey());
			    }
			}
		}
	}
	
	
	/**
	 * Returns a comparator for the given property.
	 * 
	 * @param property
	 * @return
	 */
	public Comparator<PSMReportItem> getPSMComparator(String property) {
		Comparator<PSMReportItem> comparator =
				PSMReportItemComparator.getComparatorByName(property,
						SortOrder.ascending);
		
		return comparator;
    }
	
	
	/**
	 * Returns a comparator for the given property, where the property is the
	 * name of a score without the score prefix.
	 * 
	 * @param property
	 * @return
	 */
	public Comparator<PSMReportItem> getScorePSMComparator(String property) {
		return getPSMComparator(score_prefix + property); 
	}
	
	
	/**
	 * Returns the order of the property for the given file, i.e. unsorted,
	 * ascending or descending.
	 * 
	 * @param fileID
	 * @param property
	 * @return
	 */
	public String getOrder(Long fileID, String property) {
		return fileSortings.get(fileID).get(property).toString();
	}
	
	
	/**
	 * Returns the order of a property, which is a score without the score,
	 * prefix, for the given file. See {@link #getOrder(Long, String)}.
	 * 
	 * @param fileID
	 * @param property
	 * @return
	 */
	public String getScoreOrder(Long fileID, String property) {
		if (!fileSortings.get(fileID).containsKey(score_prefix + property)) {
			// check, whether the score is new and add it, if necessary
			for (String scoreName : psmModeller.getScoreShortNames(fileID)) {
				if (scoreName.equals(property)) {
					fileSortings.get(fileID).put(score_prefix + scoreName,
							SortOrder.unsorted);
				}
			}
		}
		
		return fileSortings.get(fileID).get( score_prefix + property).toString();
	}
	
	
	/**
	 * Returns the current score prefix, which is used to discern scores from
	 * other attributes.
	 * 
	 * @return
	 */
	public String getScorePrefix() {
		return score_prefix;
	}
}
