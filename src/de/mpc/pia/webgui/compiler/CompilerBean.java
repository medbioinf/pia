package de.mpc.pia.webgui.compiler;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.richfaces.event.DropEvent;
import org.richfaces.event.FileUploadEvent;
import org.richfaces.model.UploadedFile;

import de.mpc.pia.intermediate.compiler.parser.InputFileParserFactory;
import de.mpc.pia.tools.PIAConfigurationProperties;


@ManagedBean
@SessionScoped
public class CompilerBean implements Serializable {
	
	/** the name of the project */
	private String projectName;
	
	/** the files for this compiler */
	private Map<String, TempCompileFile> compileFiles;
	
	/** true or false, if the compilation was started or not */
	private boolean compilationStarted;
	
	/** the compilation manager of the application */
	@ManagedProperty(value="#{compilationManager}")
    private CompilationManager compilationManager;
	
	/** the direct download from mascot via HTTP */
	private HTTPMascotDownloadThread mascotHTTPDownload;
	
	/** the name of the job on the Mascot server to be fetched */
	private String mascotJobName;
	
	/** the date path on the Mascot server, where the to be fetched file lies */
	private String mascotDatePath;
	
	/** pia configuration */
	@ManagedProperty(value="#{piaConfiguration}")
    private PIAConfigurationProperties configurationProperties;
	
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(CompilerBean.class);
	
	
	
	/**
	 * Basic constructor.
	 */
	public CompilerBean() {
	}
	
	
	/**
	 * An initializer, also called after starting a thread.
	 */
	@PostConstruct
	private void init() {
		compilationStarted = false;
		projectName = null;
		compileFiles = new HashMap<String, TempCompileFile>();
		
		mascotJobName = "";
		mascotDatePath = "";
		mascotHTTPDownload = null;
	}
	
	
	/**
	 * Triggered, if a upload event was fired.
	 * 
	 * @param event
	 */
	public void fileUpload(FileUploadEvent event) {
        UploadedFile item = event.getUploadedFile();
        String originalName = item.getName();
        
        TempUploadedFile tempFile = null;
        
        try {
        	tempFile = new TempUploadedFile(item.getInputStream(),
        			originalName,
        			configurationProperties.getPIAProperty("tmp_path", "/tmp"));
        } catch (IOException ex) {
        	tempFile = new TempUploadedFile(item.getData(),
            		originalName,
            		configurationProperties.getPIAProperty("tmp_path", "/tmp"));
        }
        
    	putNewTempFile(tempFile.getFilePath(), tempFile);
    	
        TempCompileFile compFile = new TempCompileFile(tempFile, originalName);
        compileFiles.put(tempFile.getFilePath(), compFile);
    }
	
	
	/**
	 * Puts the given {@link TempUploadedFile} into the map of files and returns
	 * the same, as a {@link Map#put(Object, Object)}.
	 * 
	 * @param key
	 * @param tempFile
	 * @return
	 */
	private TempUploadedFile putNewTempFile(String key,
			TempUploadedFile tempFile) {
		return compilationManager.putNewTempFile(key, tempFile);
	}
	
	
	/**
	 * Getter for the compile files.
	 * @return
	 */
	public List<TempCompileFile> getCompileFiles() {
		List<TempCompileFile> files =
				new ArrayList<TempCompileFile>(compileFiles.values());
		Collections.sort(files);
		return files;
	}
	
	
	/**
	 * Returns a List of SelectItems representing the available filters for this
	 * file.
	 * 
	 * @return
	 */
	public List<SelectItem> getFileTypeSelectItems() {
		List<SelectItem> types = new ArrayList<SelectItem>();
		
		types.add(new SelectItem(null, "try to guess"));
		
		for (InputFileParserFactory.InputFileTypes type
				: InputFileParserFactory.InputFileTypes.values()) {
			types.add(new SelectItem(type.getFileTypeShort(),
					type.getFileTypeName()));
		}
		
		return types;
	}
	
	
	/**
	 * Removes the file with the given key from the compileFiles, the tempFiles
	 * and the disk.
	 * 
	 * @param key
	 */
	public void removeFile(TempCompileFile compFile) {
		TempUploadedFile rmvFile = compFile.getFile();
		TempUploadedFile additionalFile = compFile.getAdditionalInfoFile();
		
		// remove from the tempFiles
		compileFiles.remove(rmvFile.getFilePath());
		compilationManager.removeFromTempFiles(rmvFile.getFilePath());
		
		if (additionalFile != null) {
			compileFiles.remove(additionalFile.getFilePath());
			compilationManager.removeFromTempFiles(additionalFile.getFilePath());
		}
		
		// ok, now also remove the real file(s)
		if (rmvFile != null) {
			String fileName = rmvFile.getFilePath();
			if (!rmvFile.removeFromDisk()) {
				logger.warn("Could not delete orphaned file '" + fileName + "'");
			}
		}
		
		if (additionalFile != null) {
			String fileName = additionalFile.getFilePath();
			if (!additionalFile.removeFromDisk()) {
				logger.warn("Could not delete orphaned file '" + fileName + "'");
			}
		}
	}
	
	
	/**
	 * Processed after a file is dropped onto another file.
	 * @param event
	 */
	public void processDropFileOnFile(DropEvent event) {
        if (!(event.getDragValue() instanceof TempCompileFile)) {
        	logger.error("Wrong drag object: " + event.getDragValue() + "!");
        	return;
        }
        if (!(event.getDropValue() instanceof TempCompileFile)) {
        	logger.error("Wrong drop object: " + event.getDropValue() + "!");
        	return;
        }
        
		TempCompileFile dragValue = (TempCompileFile)event.getDragValue();
        TempCompileFile dropValue = (TempCompileFile)event.getDropValue();
        
        // if dragValue has additionalInfoFile, move it to the compileFiles
        TempUploadedFile infoFile = dragValue.getAdditionalInfoFile();
        if (infoFile != null) {
        	
        	TempCompileFile compFile = new TempCompileFile(infoFile,
        			infoFile.getOriginalName());
        	
        	compileFiles.put(infoFile.getFilePath(), compFile);
        	
        	dragValue.setAdditionalInfoFile(null);
        }
        
        // if dropValue already has an additionalInfoFile, move it to compileFiles
        infoFile = dropValue.getAdditionalInfoFile();
        if (infoFile != null) {
        	
        	TempCompileFile compFile = new TempCompileFile(infoFile,
        			infoFile.getOriginalName());
        	
        	compileFiles.put(infoFile.getFilePath(), compFile);
        	
        	dropValue.setAdditionalInfoFile(null);
        }
        
        // make dragValue to dropValue's additionalInfoFile
        compileFiles.remove(dragValue.getFile().getFilePath());
        dropValue.setAdditionalInfoFile(dragValue.getFile());
	}
	
	
	/**
	 * Processed if an info file should be released from the parent.
	 * @param event
	 */
	public void processFileRelease(TempCompileFile parent) {
        if (parent!= null) {
        	TempUploadedFile infoFile = parent.getAdditionalInfoFile();
        	TempCompileFile child = new TempCompileFile(infoFile,
        			infoFile.getOriginalName());
        	
        	parent.setAdditionalInfoFile(null);
        	compileFiles.put(infoFile.getFilePath(), child);
        }
	}
	
	
	/**
	 * Setter for the project name
	 * @param name
	 */
	public void setProjectName(String name) {
		this.projectName = (name.trim().length() > 0) ? name : null;
	}
	
	
	/**
	 * Getter for the project name
	 * @return
	 */
	public String getProjectName() {
		return projectName;
	}
	
	
	/**
	 * Setter for the compilation manager
	 * @param manager
	 */
	public void setCompilationManager(CompilationManager manager) {
		this.compilationManager = manager;
	}
	
	
	/**
	 * Start compilation...
	 * 
	 * @return
	 */
	public void startCompile() {
		boolean ok = true;
		FacesContext context = FacesContext.getCurrentInstance();
		
		if (projectName == null) {
			context.addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR,
							"Please give a project name.", null));
			ok = false;
		}
		
		if (compileFiles.size() < 1) {
			context.addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR,
							"There is no file for compilation given.", null));
			ok = false;
		}
		
		logger.info("startCompile ok=" + ok);
		
		if (!ok) {
			// something went wrong
			logger.info("this is NOT ok!");
			compilationStarted = false;
		} else {
			logger.info("calling startCompilationThread");
			compilationManager.startCompilationThread(projectName,
					new ArrayList<TempCompileFile>(compileFiles.values()));
			
			context.addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO,
							"The compilation of '" + projectName +
							"' started with " + compileFiles.size() + " files.",
							null));
			
			init();
			
			compilationStarted = true;
		}
	}
	
	
	/**
	 * Getter for compilationStarted. Only makes sense to call it, after
	 * startCompile was executed.
	 * 
	 * @return
	 */
	public boolean getCompilationStarted() {
		return compilationStarted;
	}
	
	
	/**
	 * Getter for the mascot file name
	 * @return
	 */
	public String getMascotJobName() {
		return mascotJobName;
	}
	
	
	/**
	 * Setter for the mascot file name
	 * @param fileName
	 */
	public void setMascotJobName(String fileName) {
		this.mascotJobName = fileName;
	}
	
	
	/**
	 * Getter for the mascot date path
	 * @return
	 */
	public String getMascotDatePath() {
		return mascotDatePath;
	}
	
	
	/**
	 * Setter for the mascot date path
	 * @param fileName
	 */
	public void setMascotDatePath(String datePath) {
		this.mascotDatePath = datePath;
	}
	
	
	/**
	 * Getter for the progress of the mascot file copy.
	 * @return
	 */
	public Long getMascotProgressValue() {
		if (mascotHTTPDownload != null) {
			return mascotHTTPDownload.getDownloadProgress();
		} else {
			return -1l;
		}
	}
	
	
	/**
	 * Returns a String of the messages from the mascot import
	 * @return
	 */
	public List<String> getMascotMessages() {
		if (mascotHTTPDownload != null) {
			return mascotHTTPDownload.getMessages();
		} else {
			return new ArrayList<String>(0);
		}
	}
	
	
	/**
	 * Imports the file from the mascot server 
	 */
	public void startMascotImport() {
		if (mascotJobName.trim() != "") {
			
			mascotHTTPDownload = new HTTPMascotDownloadThread(
					configurationProperties.getPIAProperty("tmp_path", "/tmp"),
					configurationProperties.getPIAProperty("mascot_server", "mascotmaster"),
					configurationProperties.getPIAProperty("mascot_path", "mascot"));
			
			mascotHTTPDownload.initialiseHTTPDownload(mascotJobName, mascotDatePath);
			mascotHTTPDownload.start();
		}
	}
	
	
	/**
	 * This must be called after the SCP copy is done.
	 * @return
	 */
	public String finishMascotImport() {
		logger.debug("finishMascotImport called");
		
		if (mascotHTTPDownload != null) {
			String filePath = mascotHTTPDownload.getTmpFilePath();
			String originalName = mascotHTTPDownload.getOriginalFileName();
			
			if ((filePath == null) || (originalName == null)) {
				logger.error(
						"Something uncaught went wrong while copying the file.");
			} else {
		        TempUploadedFile tempFile = new TempUploadedFile(filePath, originalName);
		        putNewTempFile(tempFile.getFilePath(), tempFile);
		        
		        TempCompileFile compFile = new TempCompileFile(tempFile, originalName);
		        compileFiles.put(tempFile.getFilePath(), compFile);
			}
			
			mascotHTTPDownload = null;
		}
		
		return null;
	}
	
	
	
	/**
	 * Setter for the PIAConfigurationProperties, called by injection.
	 * 
	 * @param properties
	 */
	public void setConfigurationProperties(PIAConfigurationProperties properties) {
		this.configurationProperties = properties;
	}

}
