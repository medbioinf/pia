package de.mpc.pia.webgui.compiler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.compiler.PIACompiler;

/**
 * This is an actual running compilation.
 * 
 * @author julian
 *
 */
public class CompilationThread extends Thread {
	
	/** the ID of the thread */
	private Long ID;
	
	/** monitor to notify, if we are finished */
	private Object parentMonitor;
	
	/** the name of the project */
	private String projectName;
	
	/** the files for compilation */
	private List<TempCompileFile> files;
	
	/** the number of allowed threads */
	private int nrThreads;
	
	/** the output path for the data */
	private String dataPath;
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(CompilationThread.class);
	
	
	/**
	 * Basic constructor.
	 * 
	 * @param ID
	 * @param projectName
	 * @param files
	 */
	public CompilationThread(Long ID, String projectName,
			List<TempCompileFile> files, int nrThreads, String dataPath,
			Object parentMonitor) {
		this.ID = ID;
		this.parentMonitor = parentMonitor;
		this.projectName = projectName;
		this.files = files;
		this.nrThreads = nrThreads;
		this.dataPath = dataPath;
		
		this.setName("PIA-CompilationThread-" + ID);
	}
	
	
	/**
	 * Returns the internal ID of the CompilationThread.
	 * @return
	 */
	public Long getCompilationID() {
		return ID;
	}
	
	/**
	 * Getter for the projectName
	 * @return
	 */
	public String getProjectName() {
		return projectName;
	}
	
	
	@Override
	public void run() {
		logger.info(getName() + " started to run for '" + projectName + "'");
		
		PIACompiler piaCompiler = new PIACompiler();
		piaCompiler.setNrThreads(nrThreads);
		piaCompiler.setName(projectName);
		
		boolean ok = true;
		
		for (TempCompileFile file : files) {
			
			String fileName = file.getFile().getFilePath();
			String name = file.getName();
			String additionalInfoFileName = null;
			String typeShort = file.getTypeShort();
			
			if (file.getAdditionalInfoFile() != null) {
				additionalInfoFileName =
						file.getAdditionalInfoFile().getFilePath();
			}
			
			logger.info("compilation file:" + fileName +
					"\n\tname: " + name + 
					"\n\tadd: " + additionalInfoFileName + 
					"\n\ttype: " + typeShort);
			
			try {
				if (!piaCompiler.getDataFromFile(name, fileName,
						additionalInfoFileName, typeShort)) {
					logger.error("Something went wrong while getting data from " +
							fileName);
				}
			} catch (Exception e) {
				logger.error("Something went wrong while getting data from " +
						fileName, e);
				ok = false;
				break;
			}
		}
		
		if (ok) {
			piaCompiler.buildClusterList();
			piaCompiler.buildIntermediateStructure();
			
			SimpleDateFormat sdfDate = new SimpleDateFormat("/yyyyMMddHHmmssSSS");
			String dateStr = sdfDate.format(new Date());
			String outName = 
					dataPath +
					dateStr + "-" + 
					projectName.trim().replaceAll("\\s", "_") + ".pia.xml";
			
			piaCompiler.writeOutXML(outName);
			
			logger.info(getName() + " finished.");
		} else {
			logger.info(getName() + " failed.");
		}
		
		// clean up (delete the files)
		for (TempCompileFile file : files) {
			TempUploadedFile rmvFile = file.getFile();
			TempUploadedFile additionalFile = file.getAdditionalInfoFile();
			
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
		
		synchronized (parentMonitor) {
			parentMonitor.notifyAll();
		}
	}
}
