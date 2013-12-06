package de.mpc.pia.webgui.psmviewer.component;

import java.io.IOException;
import java.io.Writer;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import de.mpc.pia.modeller.PSMModeller;
import de.mpc.pia.webgui.psmviewer.PSMViewer;


public class PSMViewerExportPanel {
	
	/** the {@link PSMModeller} of the {@link PSMViewer}*/
	private PSMModeller psmModeller;
	
	/** the ID of the current file */
	private Long fileID;
	
	
	/** should the data be filtered before export */
	private boolean exportFiltered;
	
	/** export one row per Accession for SpectralCounting */
	private Boolean exportForSC;
	
	
	
	public PSMViewerExportPanel(PSMModeller modeller) {
		this.psmModeller = modeller;
		fileID = 0L;
		exportFiltered = false;
		exportForSC = false;
	}
	
	
	/**
	 * Updates the data for the panel with data for the given file.
	 * 
	 * @return
	 */
	public void updateExportPanel(Long fileID) {
		// not much to do here
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
	
	
	public Long getFileID() {
		return fileID;
	}
	
	
	public void setExportFiltered(boolean exportFiltered) {
		this.exportFiltered = exportFiltered;
	}
	
	
	public boolean getExportFiltered() {
		return exportFiltered;
	}
	
	
	public void setExportForSC(Boolean exportForSC) {
		this.exportForSC = exportForSC;
	}
	
	
	public Boolean getExportForSC() {
		return exportForSC;
	}
	
	
	/**
	 * This function writes the export into mzIdentML
	 */
	public void exportToMzIdentML() {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();
		
		externalContext.setResponseContentType("application/xml");
		externalContext.setResponseHeader("Content-Disposition",
				"attachment; filename=\"report-psms.mzid");
		externalContext.setResponseCharacterEncoding("UTF-8");
		
		try {
			Writer writer = externalContext.getResponseOutputWriter();
			
			psmModeller.exportMzIdentML(writer, fileID, exportFiltered);
			
			writer.close();
		} catch (IOException e) {
			// TODO Better error/exception/logging
			e.printStackTrace();
		}
		
		facesContext.responseComplete();
	}
	
	
	/**
	 * This function writes the export into mzTab
	 */
	public void exportToMzTab() {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();
		
		externalContext.setResponseContentType("text/tab-separated-values");
		externalContext.setResponseHeader("Content-Disposition",
				"attachment; filename=\"report-psms.mzTab");
		externalContext.setResponseCharacterEncoding("UTF-8");
		
		try {
			Writer writer = externalContext.getResponseOutputWriter();
			
			psmModeller.exportMzTab(writer, fileID, exportFiltered);
			
			writer.close();
		} catch (IOException e) {
			// TODO Better error/exception/logging
			e.printStackTrace();
		}
		
		facesContext.responseComplete();
	}
	
	
	/**
	 * This function writes the export into CSV
	 */
	public void exportToCSV() {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();
		
		externalContext.setResponseContentType("text/csv");
		externalContext.setResponseHeader("Content-Disposition",
				"attachment; filename=\"psm_export.csv");
		externalContext.setResponseCharacterEncoding("UTF-8");
		
		try {
			Writer writer = externalContext.getResponseOutputWriter();
			
			psmModeller.exportCSV(writer, fileID, exportForSC, exportFiltered);
			
			writer.close();
		} catch (IOException e) {
			// TODO Better error/exception/logging
			e.printStackTrace();
		}
		
		facesContext.responseComplete();
	}
}
