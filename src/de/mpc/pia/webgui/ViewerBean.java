package de.mpc.pia.webgui;


import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Group;
import de.mpc.pia.modeller.IdentificationKeySettings;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.tools.PIAConfigurationProperties;
import de.mpc.pia.visualization.graph.PIAtoSVG;
import de.mpc.pia.webgui.peptideviewer.PeptideViewer;
import de.mpc.pia.webgui.proteinviewer.ProteinViewer;
import de.mpc.pia.webgui.psmviewer.PSMViewer;
import de.mpc.pia.webgui.wizard.Wizard;


@ManagedBean
@SessionScoped
public class ViewerBean {

	/** the modeller */
	private PIAModeller modeller;
	
	
	/** handles the viewing of PSMs */
	private PSMViewer psmViewer;
	
	/** handles the viewing of the peptides */
	private PeptideViewer peptideViewer;
	
	/** handles the viewing of the proteins */
	private ProteinViewer proteinViewer;
	
	/** the original URL, before redirecting to loading screen */
	private String originalURL;
	
	/** the object type, which should be visualized in the PIA tree */
	private String inTreeType;
	
	/** the object id, which should be visualized in the PIA tree */
	private String inTreeID;
	
	/** the PIA configurations, set on initialisation */
	@ManagedProperty(value="#{piaConfiguration}")
    private PIAConfigurationProperties configurationProperties;
	
	/** the thread for file loading */
	private LoadPIAFileThread loadFileThread;
	
	/** the wizard mode handler */
	private Wizard wizard;
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(ViewerBean.class);
	
	
	/**
	 * Basic constructor.
	 */
	public ViewerBean() {
		modeller = new PIAModeller();
		
		psmViewer = null;
		peptideViewer = null;
		proteinViewer = null;
		
		inTreeType = null;
		inTreeID = null;
		
		loadFileThread = null;
		
		wizard = null;
	}
	
	
	/**
	 * Getter for the modeller
	 * @return
	 */
	public PIAModeller getModeller() {
		return modeller;
	}
	
	
	/**
	 * Setter for fileName.
	 * Also initialises the model, if the fileName changed.
	 * 
	 * @param filename
	 */
	public void setFileName(String filename) {
		if (loadFileThread != null) {
			// file loading is in progress
			if (!loadFileThread.isAlive() && loadFileThread.wasSuccessful()) {
				// just finished loading a file
				getDataFromFileloader();
			}
		}
		
		if ((filename != null) &&
				(loadFileThread == null) &&
				((modeller == null) || !filename.equals(modeller.getFileName()))) {
			// we have a new file, so make space for new modellers
			modeller = null;
			peptideViewer = null;
			proteinViewer = null;
			psmViewer = null;
			
			
			// load the new file
			loadFileThread = new LoadPIAFileThread(filename,
					configurationProperties);
			loadFileThread.start();
			
			// delete the wizard
			wizard = null;
		}
	}
	
	
	/**
	 * Getter for fileName
	 * @return
	 */
	public String getFileName() {
		if (loadFileThread != null) {
			return loadFileThread.getFileName();
		} else if (modeller != null) {
			return modeller.getFileName();
		} else {
			return null;
		}
	}
	
	/**
	 * Returns the URL encoded fileName
	 * @return
	 */
	public String getEncodedFileName() {
		String fileName = getFileName();
		if (fileName != null) {
			try {
				return URLEncoder.encode(fileName, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				return null;
			}
		}
		
		return null;
	}
	
	/**
	 * Checks for errors or ongoing file loadings and redirects, if necessary.
	 */
	public void checkAndRedirect(String originalURL) {
		this.originalURL = originalURL;
		
		if (loadFileThread != null) {
			if (!loadFileThread.isAlive()) {
				if (loadFileThread.wasSuccessful()) {
					getDataFromFileloader();
					return;
				} else {
					FacesContext facesContext = FacesContext.getCurrentInstance();
					
					for (String message : loadFileThread.getErrorMessages()) {
						facesContext.addMessage(null,
								new FacesMessage(message));
					}
					
					try {
						facesContext.getExternalContext().redirect("error.jsf");
					} catch (IOException e) {
						facesContext.getApplication().getNavigationHandler().
								handleNavigation(facesContext, null, "error.jsf");
					}
					loadFileThread = null;
					return;
				}
			} else {
				FacesContext facesContext = FacesContext.getCurrentInstance();
				
				try {
					facesContext.getExternalContext().redirect("loading.jsf");
				} catch (IOException e) {
					// if redirect does not work, make navigation
					facesContext.getApplication().getNavigationHandler().
							handleNavigation(facesContext, null, "loading.jsf");
				}
				return;
			}
		} else {
			if (getFileName() == null) {
				FacesContext facesContext = FacesContext.getCurrentInstance();
				
				facesContext.addMessage(null,
						new FacesMessage("no file given"));
				
				try {
					facesContext.getExternalContext().redirect("error.jsf");
				} catch (IOException e) {
					facesContext.getApplication().getNavigationHandler().
							handleNavigation(facesContext, null, "error.jsf");
				}
				
				return;
			}
		}
	}
	
	
	/**
	 * Gets the original URL before redirecting.
	 * @return
	 */
	public String getOriginalURL() {
		return originalURL;
	}
	
	
	/**
	 * Gets the data from the file loader, if finished.
	 */
	private void getDataFromFileloader() {
		modeller = loadFileThread.getModeller();
		psmViewer = loadFileThread.getPSMViewer();
		peptideViewer = loadFileThread.getPeptideViewer();
		proteinViewer = loadFileThread.getProteinViewer();
		
		loadFileThread = null;
	}
	
	
	/**
	 * Returns the progress of the file loading or 0L if no loading.
	 * @return
	 */
	public Long getLoadingProgress() {
		if (loadFileThread != null) {
			return loadFileThread.getProgress();
		}
		return 0L;
	}
	
	
	/**
	 * Returns status of the loading progress or null if no loading.
	 * @return
	 */
	public String getLoadingStatus() {
		if (loadFileThread != null) {
			return loadFileThread.getLoadingStatus();
		}
		return null;
	}
	
	/**
	 * Calls the {@link ProteinViewer#checkForInference()}, if a valid
	 * proteinViewer is generated.
	 */
	public void checkForInference() {
		if (proteinViewer != null) {
			proteinViewer.checkForInference();
		} else {
			return;
		}
	}
	
	
	/**
	 * Returns the project name or null, if none is given or no project loaded.
	 * 
	 * @return
	 */
	public String getProjectName() {
		if (modeller != null) {
			return modeller.getProjectName();
		}
		return null;
	}
	
	
	/**
	 * Returns all valid {@link IdentificationKeySettings}.
	 * @return
	 */
	public IdentificationKeySettings[] getAllPSMSetSettings() {
		return IdentificationKeySettings.values();
	}
	
	
	/**
	 * Returns the current values of the {@link IdentificationKeySettings}.
	 * @return
	 */
	public Map<String, Boolean> getPSMSetSettings() {
		return modeller.getPSMSetSettings();
	}
	
	
	/**
	 * Returns a Map from the {@link IdentificationKeySettings} String representation to a
	 * Set of file IDs, where a warning occurred.
	 * 
	 * @return
	 */
	public Map<String, Set<Long>> getPSMSetSettingsWarnings() {
		return modeller.getPSMSetSettingsWarnings();
	}
	
	
	/**
	 * Getter for considerModifications.
	 * @return
	 */
	public Boolean getConsiderModifications() {
		return modeller.getConsiderModifications();
	}
	
	
	/**
	 * Setter for considerModifications.
	 * @return
	 */
	public void setConsiderModifications(Boolean considerModifications) {
		modeller.setConsiderModifications(considerModifications);
	}
	
	
	/**
	 * Getter for createPSMSets.
	 * @return
	 */
	public Boolean getCreatePSMSets() {
		return modeller.getCreatePSMSets();
	}
	
	
	/**
	 * Setter for createPSMSets.
	 * @param createPSMSets
	 */
	public void setCreatePSMSets(Boolean createPSMSets) {
		modeller.setCreatePSMSets(createPSMSets);
	}
	
	
	/**
	 * apply the general settings and recalculate the reports
	 */
	public String applySettings() {
		psmViewer.applyGeneralSettings(modeller.getCreatePSMSets(),
				modeller.getPSMSetSettings());
		
		peptideViewer.applyGeneralSettings(modeller.getConsiderModifications());
		
		proteinViewer.applyGeneralSettings();
		
		return null;
	}
	
	
	/**
	 * Getter for the psmViewer.
	 * @return
	 */
	public PSMViewer getPsmViewer() {
		return psmViewer;
	}
	
	
	/**
	 * Getter for the peptideViewer.
	 * @return
	 */
	public PeptideViewer getPeptideViewer() {
		return peptideViewer;
	}
	
	
	/**
	 * Getter for the proteinViewer.
	 * @return
	 */
	public ProteinViewer getProteinViewer() {
		return proteinViewer;
	}
	
	
	
	/**
	 * Setter for the PIAConfigurationProperties, used for injection.
	 * 
	 * @param properties
	 */
	public void setConfigurationProperties(PIAConfigurationProperties properties) {
		this.configurationProperties = properties;
	}
	
	
	/**
	 * Returns the property with the given name or null, if it is not given in
	 * the config file.
	 * 
	 * @param name
	 * @return
	 */
	public String getConfigurationProperty(String name) {
		return configurationProperties.getPIAProperty(name, null);
	}
	

	/**
	 * Getter for the type of the object, which should be visualized in its PIA
	 * tree
	 * 
	 * @return
	 */
	public String getInTreeType() {
		return inTreeType;
	}
	
	
	/**
	 * Setter for the type of the object, which should be visualized in its PIA
	 * tree
	 * 
	 * @return
	 */
	public void setInTreeType(String type) {
		inTreeType = type;
	}
	
	
	/**
	 * Getter for the ID of the object, which should be visualized in its PIA
	 * tree
	 * 
	 * @return
	 */
	public String getInTreeID() {
		return inTreeID;
	}
	
	
	/**
	 * Setter for the ID of the object, which should be visualized in its PIA
	 * tree
	 * 
	 * @return
	 */
	public void setInTreeID(String id) {
		inTreeID = id;
	}
	
	
	/**
	 * Paints the object given by {@link #setInTreeType(String)} and
	 * {@link #setInTreeID(String)} in its PIA tree.
	 * 
	 * @param os the stream, the graphic is returned to
	 * @param data not used!
	 * @throws IOException
	 */
	public void paintObjectInTree(OutputStream os, Object data) throws IOException {
		String objectID = getInTreeID();
		
		if ((getInTreeType() == null) || (objectID == null)) {
			return;
		}
		
		PIAtoSVG piaToSVG = null;
		if (getInTreeType().equals("psm")) {
			Long selectedFileNumber = psmViewer.getSelectedFileTabNumber();
			if (selectedFileNumber == 0) {
				for (ReportPSMSet psmSet : psmViewer.getReportPSMSets()) {
					if (objectID.equals(psmSet.getIdentificationKey(
							psmSet.getNotRedundantIdentificationKeySettings()))) {
						Set<Long> thisSetIDs = new HashSet<Long>();
						for (ReportPSM psm : psmSet.getPSMs()) {
							thisSetIDs.add(psm.getSpectrum().getID());
						}
						
						piaToSVG = new PIAtoSVG(psmSet.getPeptide().getGroup(),
								"PSMTree" + psmSet.getPeptide().getGroup().getTreeID(),
								null, null,
								null, null,
								thisSetIDs, null);
						break;
					}
				}
			} else {
				for (ReportPSM psm 
						: psmViewer.getReportPSMs(selectedFileNumber)) {
					if (objectID.equals(psm.getIdentificationKey(
							modeller.getPSMSetSettings()))) {
						Set<Long> thisSetIDs = new HashSet<Long>();
						thisSetIDs.add(psm.getSpectrum().getID());
						
						piaToSVG = new PIAtoSVG(psm.getPeptide().getGroup(),
								"PSMTree" + psm.getPeptide().getGroup().getTreeID(),
								null, null,
								null, null,
								thisSetIDs, null);
						break;
					}
				}
			}
			
		} else if (getInTreeType().equals("peptide")) {
			Long selectedFileNumber = peptideViewer.getSelectedFileTabNumber();
			for (ReportPeptide peptide
					: peptideViewer.getReportPeptides(selectedFileNumber)) {
				if (objectID.equals(peptide.getSequence())) {
					Set<Long> thisPeptideID = new HashSet<Long>();
					thisPeptideID.add(peptide.getPeptide().getID());
					
					piaToSVG = new PIAtoSVG(peptide.getPeptide().getGroup(),
							"PeptideTree" + peptide.getPeptide().getGroup().getTreeID(),
							null, null,
							thisPeptideID, null,
							null, null);
					break;
				}
			}
		} else if (getInTreeType().equals("protein")) {
			Group proteinGroup = null;
			
			Set<Long> thisAccessionIDs = new HashSet<Long>();
			Set<Long> otherAccessionIDs = new HashSet<Long>();
			
			Set<Long> thisPeptideIDs = new HashSet<Long>();
			Set<Long> otherPeptideIDs = new HashSet<Long>();
			
			Set<Long> thisSpectrumIDs = new HashSet<Long>();
			Set<Long> otherSpectrumIDs = new HashSet<Long>();
			
			for (ReportProtein protein : proteinViewer.getReportProteins()) {
				if (protein.getID().toString().equals(getInTreeID())) {
					proteinGroup = protein.getAccessions().get(0).getGroup();
					
					for (Accession acc : protein.getAccessions()) {
						thisAccessionIDs.add(acc.getID());
					}
					
					for (ReportPeptide pep : protein.getPeptides()) {
						thisPeptideIDs.add(pep.getPeptide().getID());
						
						for (PSMReportItem psm : pep.getPSMs()) {
							if (psm instanceof ReportPSM) {
								thisSpectrumIDs.add(
										((ReportPSM) psm).getSpectrum().getID());
							} else if (psm instanceof ReportPSMSet) {
								for (ReportPSM p 
										: ((ReportPSMSet) psm).getPSMs()) {
									thisSpectrumIDs.add(
											p.getSpectrum().getID());
								}
							}
						}
					}
				} else {
					for (Accession acc : protein.getAccessions()) {
						otherAccessionIDs.add(acc.getID());
					}
					
					for (ReportPeptide pep : protein.getPeptides()) {
						otherPeptideIDs.add(pep.getPeptide().getID());
						
						for (PSMReportItem psm : pep.getPSMs()) {
							if (psm instanceof ReportPSM) {
								otherSpectrumIDs.add(
										((ReportPSM) psm).getSpectrum().getID());
							} else if (psm instanceof ReportPSMSet) {
								for (ReportPSM p 
										: ((ReportPSMSet) psm).getPSMs()) {
									otherSpectrumIDs.add(
											p.getSpectrum().getID());
								}
							}
						}
					}
				}
			}
			
			if (proteinGroup != null) {
				piaToSVG = new PIAtoSVG(proteinGroup,
						"ProteinTree" + proteinGroup.getTreeID(),
						thisAccessionIDs, otherAccessionIDs,
						thisPeptideIDs, otherPeptideIDs,
						thisSpectrumIDs, otherSpectrumIDs);
			}
		}
		
		if (piaToSVG != null)  {
			piaToSVG.createSVG(os);
		} else {
			logger.warn("not implemented data type: " + getInTreeType() +
					" with ID: " + getInTreeID());
		}
	}
	

	/**
	 * Getter for the wizard.
	 * @return
	 */
	public Wizard getWizard() {
		if (wizard == null) {
			wizard = new Wizard(modeller);
		}
		
		return wizard;
	}
}
