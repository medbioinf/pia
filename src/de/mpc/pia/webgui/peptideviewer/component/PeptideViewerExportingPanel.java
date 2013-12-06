package de.mpc.pia.webgui.peptideviewer.component;



import java.io.IOException;
import java.io.Writer;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import de.mpc.pia.modeller.PeptideModeller;
import de.mpc.pia.webgui.peptideviewer.PeptideViewer;


/**
 * Helper class to handle the filtering panel and the filtering in a
 * {@link PeptideViewer}.
 * 
 * @author julian
 *
 */
public class PeptideViewerExportingPanel {
	
	/** the {@link PeptideModeller} of the {@link PeptideViewer}*/
	private PeptideModeller peptideModeller;
	
	/** the ID of the current file */
	private Long fileID;
	
	/** whether the exported data should be filtered */
	private boolean exportFiltered;
	
	/** whether the exported data should have only one accession per line */
	private boolean oneAccessionPerLine;
	
	/** whether the PSM sets should be included in the export*/
	private boolean includePSMSets;
	
	/** whether the PSMs should be included in the export*/
	private boolean includePSMs;
	
	
	
	
	/**
	 * Basic constructor
	 */
	public PeptideViewerExportingPanel(PeptideModeller modeller) {
		this.peptideModeller = modeller;
		fileID = 0L;
		exportFiltered = false;
		oneAccessionPerLine = false;
		includePSMSets = false;
		includePSMs = false;
	}
	
	
	/**
	 * Updates the data for the panel with data for the given file.
	 * 
	 * @return
	 */
	public void updateExportPanel(Long fileID) {
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
	 * Setter whether the new filter should be negated.
	 * 
	 * @param negate
	 */
	public void setExportFiltered(boolean exportFiltered) {
		this.exportFiltered = exportFiltered;
	}
	
	
	/**
	 * Getter whether the new filter should be negated.
	 * 
	 * @param negate
	 */
	public boolean getExportFiltered() {
		return exportFiltered;
	}
	
	
	public void setOneAccessionPerLine(boolean oneAccessionPerLine) {
		this.oneAccessionPerLine = oneAccessionPerLine;
	}
	
	
	public boolean getOneAccessionPerLine() {
		return oneAccessionPerLine;
	}
	
	
	public void setIncludePSMSets(boolean includePSMSets) {
		this.includePSMSets = includePSMSets;
	}
	
	
	public boolean getIncludePSMSets() {
		return includePSMSets;
	}
	
	
	public void setIncludePSMs(boolean includePSMs) {
		this.includePSMs = includePSMs;
	}
	
	
	public boolean getIncludePSMs() {
		return includePSMs;
	}
	
	
	/**
	 * this function does the actual export into CSV
	 * 
	 * 
	 * @param report
	 */
	public void exportToCSV() {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();
		
		externalContext.setResponseContentType("text/csv");
		externalContext.setResponseHeader("Content-Disposition",
				"attachment; filename=\"peptide_export.csv");
		externalContext.setResponseCharacterEncoding("UTF-8");
		
		try {
			Writer writer = externalContext.getResponseOutputWriter();
			
			peptideModeller.exportCSV(writer, fileID, exportFiltered,
					oneAccessionPerLine, includePSMSets, includePSMs);
			
			writer.close();
		} catch (IOException e) {
			// TODO Better error/exception/logging
			e.printStackTrace();
		}
		
		facesContext.responseComplete();
	}
}
