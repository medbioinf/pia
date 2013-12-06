package de.mpc.pia.webgui;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;

import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.tools.PIAConfigurationProperties;
import de.mpc.pia.webgui.peptideviewer.PeptideViewer;
import de.mpc.pia.webgui.proteinviewer.ProteinViewer;
import de.mpc.pia.webgui.psmviewer.PSMViewer;


/**
 * This class handles the loading and parsing of a PIA XML file as thread.
 * 
 * @author julian
 *
 */
public class LoadPIAFileThread extends Thread {
	
	/** the name of the file, which should get parsed */
	private String fileName;
	
	/** the PIA configurations */
	private PIAConfigurationProperties configurationProperties;
	
	/** the modeller */
	private PIAModeller modeller;
	
	/** handles the viewing of PSMs */
	private PSMViewer psmViewer;
	
	/** handles the viewing of the peptides */
	private PeptideViewer peptideViewer;
	
	/** handles the viewing of the proteins */
	private ProteinViewer proteinViewer;
	
	/** a list of messages */
	private List<String> errorMessages;
	
	/** whether the thread finished successfully */
	private boolean success;
	
	/** the progress of loading in percent */
	private Long[] progress;
	
	/** parsing is complete */
	private boolean parsed;
	
	/** a status string of the loading progress */
	private String loadingStatus;
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(LoadPIAFileThread.class);
	
	
	public LoadPIAFileThread(String fileName,
			PIAConfigurationProperties configurationProperties) {
		this.fileName = fileName;
		this.configurationProperties = configurationProperties;
		
		errorMessages = new ArrayList<String>();
		success = false; 
		
		modeller = null;
		psmViewer = null;
		peptideViewer = null;
		proteinViewer = null;
		
		progress = new Long[1];
		progress[0] = 0L;
		parsed = false;
		
		loadingStatus = "initialising...";
	}
	
	
	@Override
	public void run() {
		if (fileName == null) {
			errorMessages.add("No file given.");
			logger.error("No file given.");
			return;
		}
		
		try {
			modeller = new PIAModeller();
			progress[0] = 1L;
			
			loadingStatus = "started parsing...";
			if (modeller.loadFileName(fileName, progress)) {
				parsed = true;
				progress[0] = 95L;
				if (modeller != null) {
					loadingStatus = "building PSM viewer...";
					psmViewer = new PSMViewer(modeller.getPSMModeller());
					progress[0] = 96L;
					loadingStatus = "building peptide viewer...";
					peptideViewer = new PeptideViewer(modeller.getPeptideModeller());
					progress[0] = 97L;
					loadingStatus = "building protein viewer...";
					proteinViewer = new ProteinViewer(modeller.getProteinModeller(),
							configurationProperties);
					progress[0] = 99L;
				} else {
					errorMessages.add("Could not generate modeller.");
					logger.error("Could not generate modeller.");
					success = false;
					progress[0] = -1L;
					return;
				}
			}
			
			success = true;
		} catch (FileNotFoundException e) {
			errorMessages.add("File '" + fileName + "' not found.");
			logger.error("File '" + fileName + "' not found.", e);
			success = false;
			progress[0] = -1L;
		} catch (JAXBException e) {
			errorMessages.add("File '" + fileName + "' could not be parsed.");
			logger.error("File '" + fileName + "' could not be parsed.", e);
			success = false;
			progress[0] = -1L;
		} catch (XMLStreamException e) {
			errorMessages.add("File '" + fileName + "' could not be parsed.");
			logger.error("File '" + fileName + "' could not be parsed.", e);
			success = false;
			progress[0] = -1L;
		}
		
		progress[0] = 101L;
	}
	
	
	/**
	 * Returns true, if the run is successfully finished.
	 * @return
	 */
	public boolean wasSuccessful() {
		return success;
	}
	
	
	/**
	 * Returns the filename, which is tried to be loaded.
	 * @return
	 */
	public String getFileName() {
		return fileName;
	}
	
	
	/**
	 * Returns the list of error messages. If the list is empty, the parsing was
	 * without any error.
	 * 
	 * @return
	 */
	public List<String> getErrorMessages() {
		return errorMessages;
	}
	
	
	/**
	 * Getter for the modeller.<br/>
	 * Returns null, as long as the parsing is in process or if an error
	 * occurred.
	 * @return
	 */
	public PIAModeller getModeller() {
		if (!success) {
			return null;
		} else {
			return modeller;
		}
	}
	
	
	/**
	 * Getter for the PSMViewer.<br/>
	 * Returns null, as long as the parsing is in process or if an error
	 * occurred.
	 * @return
	 */
	public PSMViewer getPSMViewer() {
		if (!success) {
			return null;
		} else {
			return psmViewer;
		}
	}
	
	
	/**
	 * Getter for the PeptideViewer.<br/>
	 * Returns null, as long as the parsing is in process or if an error
	 * occurred.
	 * @return
	 */
	public PeptideViewer getPeptideViewer() {
		if (!success) {
			return null;
		} else {
			return peptideViewer;
		}
	}
	
	
	/**
	 * Getter for the ProteinViewer.<br/>
	 * Returns null, as long as the parsing is in process or if an error
	 * occurred.
	 * @return
	 */
	public ProteinViewer getProteinViewer() {
		if (!success) {
			return null;
		} else {
			return proteinViewer;
		}
	}
	
	
	/**
	 * The progress of the file loading.
	 * @return
	 */
	public Long getProgress() {
		if (progress != null) {
			
			if (parsed) {
				return progress[0];
			} else {
				return (long)(0.95 * progress[0]);
			}
			
		} else {
			return -1L;
		}
	}
	
	
	/**
	 * The status of the loading progress.
	 * @return
	 */
	public String getLoadingStatus() {
		return loadingStatus;
	}
}
