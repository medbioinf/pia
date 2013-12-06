package de.mpc.pia.webgui.peptideviewer.component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.modeller.PeptideModeller;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.peptide.ReportPeptideComparatorFactory;
import de.mpc.pia.modeller.report.SortOrder;
import de.mpc.pia.webgui.peptideviewer.PeptideViewer;


/**
 * Helper class to handle the sorting panel and the sortings in a
 * {@link PeptideViewer}.
 * 
 * @author julian
 *
 */
public class PeptideViewerSortingPanel {
	
	/** the {@link PeptideModeller} of the {@link PeptideViewer}*/
	private PeptideModeller peptideModeller;
	
	
	/** the ID of the current file */
	private Long fileID;
	
	
	/** mapping from each fileID to the available sortings with current settings */
	private Map<Long, Map<String, SortOrder>> fileSortings;
	
	/** mapping from each fileID to the actual order of the sortings */
	private Map<Long, List<String>> fileSortPriorities;
	
	
	/** the prefix before all score tags */
	public final static String score_prefix = ReportPeptideComparatorFactory.score_prefix; 
	
	
	
	
	/**
	 * Basic constructor
	 */
	public PeptideViewerSortingPanel(PeptideModeller modeller) {
		this.peptideModeller = modeller;
		fileID = 0L;
		
		// initialise the sortings
		fileSortings = new HashMap<Long, Map<String,SortOrder>>();
		fileSortPriorities = new HashMap<Long, List<String>>();

		for (PIAInputFile file : peptideModeller.getFiles().values()) {
			Map<String, SortOrder> sortings = ReportPeptideComparatorFactory.getInitialSortOrders();
			for (String score : peptideModeller.getScoreShortNames(fileID)) {
				if (!sortings.containsKey(score_prefix + score)) {
					sortings.put(score_prefix + score, SortOrder.unsorted);
				}
			}
			fileSortings.put(file.getID(), sortings);
			
			fileSortPriorities.put(file.getID(), new ArrayList<String>());
		}
	}

	
	/**
	 * Updates the data for the panel with data for the given file.
	 * 
	 * @return
	 */
	public void updateSortingPanel(Long fileID) {
		this.fileID = fileID;
		
		Map<String, SortOrder> sortings = fileSortings.get(fileID);
		for (String score : peptideModeller.getScoreShortNames(fileID)) {
			if (!sortings.containsKey(score_prefix + score)) {
				sortings.put(score_prefix + score, SortOrder.unsorted);
			}
		}
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
	public List<String> getSortingsStrings() {
		if (fileSortPriorities.get(fileID) != null) {
			List<String> orderedSortOrders = fileSortPriorities.get(fileID);
			List<String> sortings = new ArrayList<String>();
			
			if (orderedSortOrders.size() > 0) {
				for (String order : orderedSortOrders) {
					sortings.add(order+" ("+fileSortings.get(fileID).get(order)+")");
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
	public Comparator<ReportPeptide> getPeptideComparator(String property) {
		Comparator<ReportPeptide> comparator =
				ReportPeptideComparatorFactory.getComparatorByName(property,
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
	public Comparator<ReportPeptide> getScorePeptideComparator(String property) {
		return getPeptideComparator(score_prefix + property); 
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
		SortOrder sorting = fileSortings.get(fileID).get(property);
		
		if (sorting == null) {
			sorting = SortOrder.unsorted;
		}
		
		return sorting.toString();
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
		return getOrder(fileID, score_prefix + property);
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
