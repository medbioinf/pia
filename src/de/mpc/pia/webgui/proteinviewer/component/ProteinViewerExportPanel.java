package de.mpc.pia.webgui.proteinviewer.component;

import java.io.IOException;
import java.io.Writer;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import de.mpc.pia.modeller.ProteinModeller;
import de.mpc.pia.webgui.proteinviewer.ProteinViewer;


public class ProteinViewerExportPanel {
	
	/** the {@link ProteinModeller} of the {@link ProteinViewer}*/
	private ProteinModeller proteinModeller;
	
	/** should the data be filtered before export */
	private Boolean exportFiltered;
	
	/** should the Peptides be included into the export */
	private Boolean includePeptides;
	
	/** should the PSMs be included into the export */
	private Boolean includePSMSets;
	
	/** should the Spectra be included into the export */
	private Boolean includePSMs;
	
	/** export one row per Accession for SpectralCounting */
	private Boolean exportForSC;
	
	
	
	public ProteinViewerExportPanel(ProteinModeller modeller) {
		this.proteinModeller = modeller;
		exportFiltered = false;
		includePeptides = false;
		includePSMSets = false;
		includePSMs = false;
		exportForSC = false;
	}
	
	
	public void setExportFiltered(Boolean exportFiltered) {
		this.exportFiltered = exportFiltered;
	}
	
	
	public Boolean getExportFiltered() {
		return exportFiltered;
	}
	
	
	public void setIncludePeptides(Boolean includePeptides) {
		this.includePeptides = includePeptides;
	}
	
	
	public Boolean getIncludePeptides() {
		return includePeptides;
	}
	
	
	public void setIncludePSMSets(Boolean includePSMSets) {
		this.includePSMSets = includePSMSets;
	}
	
	
	public Boolean getIncludePSMSets() {
		return includePSMSets;
	}
	
	
	public void setIncludePSMs(Boolean includePSMs) {
		this.includePSMs = includePSMs;
	}
	
	
	public Boolean getIncludePSMs() {
		return includePSMs;
	}
	
	
	public void setExportForSC(Boolean exportForSC) {
		this.exportForSC = exportForSC;
	}
	
	
	public Boolean getExportForSC() {
		return exportForSC;
	}
	
	
	/**
	 * This function exports into mzIdentML
	 */
	public void exportToMzIdentML() {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();
		
		externalContext.setResponseContentType("application/xml");
		externalContext.setResponseHeader("Content-Disposition",
				"attachment; filename=\"report-proteins.mzid");
		externalContext.setResponseCharacterEncoding("UTF-8");
		
		try {
			Writer writer = externalContext.getResponseOutputWriter();
			proteinModeller.exportMzIdentML(writer, exportFiltered);
			writer.close();
		} catch (IOException e) {
			// TODO Better error/exception/logging
			e.printStackTrace();
		}
		
		facesContext.responseComplete();
	}
	
	
	/**
	 * This function exports into CSV
	 */
	public void exportToCSV() {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();
		
		externalContext.setResponseContentType("text/csv");
		externalContext.setResponseHeader("Content-Disposition",
				"attachment; filename=\"report-proteins.csv");
		externalContext.setResponseCharacterEncoding("UTF-8");
		
		try {
			Writer writer = externalContext.getResponseOutputWriter();
			
			proteinModeller.exportCSV(writer, exportFiltered, includePeptides,
					includePSMSets, includePSMs, exportForSC);
			
			writer.close();
		} catch (IOException e) {
			// TODO Better error/exception/logging
			e.printStackTrace();
		}
		
		facesContext.responseComplete();
	}
}
