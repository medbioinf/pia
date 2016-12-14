package de.mpc.pia.intermediate.piaxml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "FileRef")
public class FileRefXML {
	private Long file_ref;
	
	
	/**
	 * Basic no-arg constructor.
	 */
	public FileRefXML() {
	}
	
	
	public FileRefXML(Long file_ref) {
		this.file_ref = file_ref;
	}
	
	
	/**
	 * Gets the value of the file_ref attribute.
	 * @return
	 */
	@XmlAttribute(required = true)
	public Long getFile_ref() {
		return file_ref;
	}
	
	
	/**
	 * Sets the value of the file_ref attribute.
	 */
	public void setFile_ref(Long file_ref) {
		this.file_ref = file_ref;
	}
}
