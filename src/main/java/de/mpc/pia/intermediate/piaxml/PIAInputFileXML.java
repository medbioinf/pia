package de.mpc.pia.intermediate.piaxml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import uk.ac.ebi.jmzidml.model.mzidml.AnalysisCollection;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisProtocolCollection;

import de.mpc.pia.intermediate.PIAInputFile;


/**
 * Model for file in a PIA intermediate file.<br/>
 * Wraps around a {@link PIAInputFile}
 * 
 * @author julian
 * 
 */
@XmlRootElement(name = "file")
//defines the the order in which the fields are written
@XmlType(propOrder = {
		"analysisCollection",
		"analysisProtocolCollection"
	})
public class PIAInputFileXML {
	/** id of the {@link PIAInputFile} element */
	private Long id;
	
	/** the NAME of the {@link PIAInputFile} */
	private String name;
	
	/** the fileName of the {@link PIAInputFile} */
	private String fileName;
	
	/** the format type of the {@link PIAInputFile} */
	private String format;
	
	/** the {@link AnalysisCollection} of the {@link PIAInputFile} */
	private AnalysisCollection analysisCollection;
	
	/** the {@link AnalysisProtocolCollection} of the {@link PIAInputFile} */
	private AnalysisProtocolCollection analysisProtocolCollection;
	
	
	/**
	 * Gets the value of the id attribute.
	 * @return
	 */
	@XmlAttribute(required = true)
	public Long getId() {
		return id;
	}
	
	
	/**
	 * Sets the value of the id attribute.
	 * @param id
	 */
	public void setId(Long id) {
		this.id = id;
	}
	
	
	/**
	 * Gets the value of the fileName property.
	 * @return
	 */
	@XmlAttribute
	public String getName() {
		return name;
	}
	
	
	/**
	 * Sets the value of the NAME property.
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	
	/**
	 * Gets the value of the fileName property.
	 * @return
	 */
	@XmlAttribute(required = true)
	public String getFileName() {
		return fileName;
	}
	
	
	/**
	 * Sets the value of the fileName property.
	 * @param fileName
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	
	/**
	 * Gets the value of the format property.
	 * @return
	 */
	@XmlAttribute(required = true)
	public String getFormat() {
		return format;
	}
	
	
	/**
	 * Sets the value of the format property.
	 * @param format
	 */
	public void setFormat(String format) {
		this.format = format;
	}
	
	
	/**
	 * Gets the value of the AnalysisCollection property.
	 * @return
	 */
	@XmlElement(name = "AnalysisCollection")
	public AnalysisCollection getAnalysisCollection() {
		return analysisCollection;
	}
	
	
	/**
	 * Sets the value of the AnalysisCollection property.
	 * @param analysisCollection
	 */
	public void setAnalysisCollection(AnalysisCollection analysisCollection) {
		this.analysisCollection = analysisCollection;
	}
	
	
	/**
	 * Gets the value of the AnalysisProtocolCollection property.
	 * @return
	 */
	@XmlElement(name = "AnalysisProtocolCollection")
	public AnalysisProtocolCollection getAnalysisProtocolCollection() {
		return analysisProtocolCollection;
	}
	
	
	/**
	 * Sets the value of the AnalysisProtocolCollection property.
	 * @param analysisProtocolCollection
	 */
	public void setAnalysisProtocolCollection(
			AnalysisProtocolCollection analysisProtocolCollection) {
		this.analysisProtocolCollection = analysisProtocolCollection;
	}
}
