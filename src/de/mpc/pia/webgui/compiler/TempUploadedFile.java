package de.mpc.pia.webgui.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

/**
 * This class handles the temporary uploaded files in general.
 * 
 * @author julian
 *
 */
public class TempUploadedFile {
	
	/** the path to the file */
	private String filePath;
	
	/** the original name of the file */
	private String originalName;
	
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(TempUploadedFile.class);
	
	
	
	/**
	 * Creates an temporary file with the given data. 
	 * 
	 * @param data
	 */
	public TempUploadedFile(byte[] data, String originalName, String tmpPath) {
		this.originalName = originalName;
		
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		String dateStr = sdfDate.format(new Date());
		
		filePath = tmpPath + dateStr + "-" + originalName;
		
		logger.info("uploading " + originalName + " to " + filePath);
		
		FileOutputStream fos = null;
		
		try {
			fos = new FileOutputStream(filePath);
			fos.write(data);
		} catch (FileNotFoundException e) {
			logger.error("Could not create the file '" + filePath + "', " +
					"maybe set correct read/write permission?", e);
		} catch (IOException e) {
			logger.error("Could not write to the file '" + filePath + "', " +
					"maybe set correct read/write permission or disk full?", e);
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					logger.error("Could not close the file '" + filePath + "'",
							e);
					throw new IllegalArgumentException("Temporary file '" +
							filePath + "' could not be uploaded.");
				}
			} else {
				// file was not written, so something went wrong
				throw new IllegalArgumentException("Temporary file '" +
						filePath + "' could not be uploaded.");
			}
		}
		
	}
	
	
	/**
	 * Creates an temporary file with the given data. 
	 * 
	 * @param data
	 */
	public TempUploadedFile(InputStream data, String originalName, String tmpPath) {
		this.originalName = originalName;
		
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		String dateStr = sdfDate.format(new Date());
		
		filePath = tmpPath + dateStr + "-" + originalName;
		
		logger.info("writing " + originalName + " to " + filePath);
		
		FileOutputStream fos = null;
		
		try {
			fos = new FileOutputStream(filePath);
			
			int read = 0;
			byte[] bytes = new byte[1024];
			
			while ((read = data.read(bytes)) != -1) {
				fos.write(bytes, 0, read);
			}
		 
			data.close();
			fos.flush();
		} catch (FileNotFoundException e) {
			logger.error("Could not create the file '" + filePath + "', " +
					"maybe set correct read/write permission?", e);
		} catch (IOException e) {
			logger.error("Could not write to the file '" + filePath + "', " +
					"maybe set correct read/write permission or disk full?", e);
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					logger.error("Could not close the file '" + filePath + "'",
							e);
					throw new IllegalArgumentException("Temporary file '" +
							filePath + "' could not be uploaded.");
				}
			} else {
				// file was not written, so something went wrong
				throw new IllegalArgumentException("Temporary file '" +
						filePath + "' could not be uploaded.");
			}
		}
		
	}
	
	
	/**
	 * Creates a temporary file using the file given by filePath.
	 * 
	 * @param data
	 */
	public TempUploadedFile(String filePath, String originalName) {
		this.filePath = filePath;
		this.originalName = originalName;
	}
	
	
	/**
	 * Gets the original name of the file.
	 * @return
	 */
	public String getOriginalName() {
		return originalName;
	}
	
	
	/**
	 * Gets the path to the temporary file
	 * @return
	 */
	public String getFilePath() {
		return filePath;
	}
	
	
	/**
	 * Removes the file
	 * @return
	 */
	public boolean removeFromDisk() {
		if (getFilePath() != null) {
			File rmv = new File(getFilePath());
			
			if (rmv.exists() && rmv.canWrite() && rmv.isFile()) {
				rmv.delete();
				originalName = null;
				filePath = null;
				return true;
			}
		}
		return false;
	}
}
