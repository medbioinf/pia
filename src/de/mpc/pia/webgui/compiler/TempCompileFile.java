package de.mpc.pia.webgui.compiler;

import de.mpc.pia.intermediate.compiler.parser.InputFileParserFactory;

/**
 * This class handles files, which are for compilation. It has additional fields
 * to set the type of the file and to link the "additional information"-files.
 * 
 * @author julian
 *
 */
public class TempCompileFile implements Comparable<TempCompileFile> {
	
	/** the uploaded file */
	private TempUploadedFile tempFile;
	
	/** the original file name */
	private String originalName;
	
	/** a name for easier identification */
	private String name;
	
	/** the specified file type */
	private InputFileParserFactory.InputFileTypes fileType;
	
	/** an optional, additional information file*/
	private TempUploadedFile additionalInfoFile;
	
	
	/**
	 * Basic constructor.
	 * @param file
	 */
	public TempCompileFile(TempUploadedFile file, String fileName) {
		this.tempFile = file;
		this.originalName = fileName;
		this.name = fileName;
		
		// guess the file type
		String fileSuffix = fileName.substring(fileName.lastIndexOf('.')+1);
		fileType = InputFileParserFactory.getFileTypeBySuffix(fileSuffix);
		
		additionalInfoFile = null;
	}
	
	
	/**
	 * Getter for the temporary file.
	 * @return
	 */
	public TempUploadedFile getFile() {
		return tempFile;
	}
	
	
	/**
	 * Getter for the original name of the file.
	 * @return
	 */
	public String getOriginalName() {
		return originalName;
	}
	
	
	/**
	 * Getter for the name of the file.
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	
	/**
	 * Setter for the name of the file.
	 * @return
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	
	/**
	 * Sets the file type by given the short name.
	 * @param typeShort
	 */
	public void setTypeShort(String typeShort) {
		fileType = InputFileParserFactory.getFileTypeByShortName(typeShort);
	}
	
	
	/**
	 * Gets the short name of the file type.
	 * @return
	 */
	public String getTypeShort() {
		if (fileType != null) {
			return fileType.getFileTypeShort();
		} else {
			return null;
		}
	}
	
	
	/**
	 * Getter for the additional information file.
	 * @return
	 */
	public TempUploadedFile getAdditionalInfoFile() {
		return additionalInfoFile;
	}
	
	
	/**
	 * Setter for the additional information file.
	 * @param infoFile
	 */
	public void setAdditionalInfoFile(TempUploadedFile infoFile) {
		this.additionalInfoFile = infoFile;
	}
	
	
	@Override
	public int compareTo(TempCompileFile o) {
		// this simple compares the original file-names
		return getOriginalName().compareTo(o.getOriginalName());
	}
}