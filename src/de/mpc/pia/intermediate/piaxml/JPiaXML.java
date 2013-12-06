package de.mpc.pia.intermediate.piaxml;

import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftwareList;
import uk.ac.ebi.jmzidml.model.mzidml.Inputs;


/**
 * The Java main class for a PIA intermediate XMl file.
 * 
 * @author julian
 *
 */
@XmlRootElement(namespace = "http://www.medizinisches-proteom-center.de/PIA/piaintermediate")
@XmlType(propOrder = {
	    "filesList",
	    "inputs",
	    "analysisSoftwareList",
	    "spectraList",
	    "accessionsList",
	    "peptidesList",
	    "groupsList"
	})
public class JPiaXML {
	
	/** name of the compilation/project */
	private String name;
	
	/** the creation date of the compilation/project */
    protected XMLGregorianCalendar date;
	
	/** the input files (the ones in Inputs can not be used, more information needed) */
	private FilesListXML filesList;
	
	/** searchDatabases and spectraData */
	private Inputs inputs;
	
	/** the used software */
	private AnalysisSoftwareList analysisSoftwareList;
	
	/** the spectra */
	private SpectraListXML spectraList;
	
	/** the peptides */
	private PeptidesListXML peptidesList;
	
	/** the accessions */
	private AccessionsListXML accessionsList;
	
	/** the groups */
	private GroupsListXML groupsList;
	

	
	/**
	 * Gets the value of the sequence property.
	 * @return
	 */
	@XmlAttribute
	public String getName() {
		return name;
	}
	
	
	/**
	 * Sets the value of the name property.
	 * @param fileName
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	
	/**
	 * Gets the value of the date property.
	 * @return
	*/
	@XmlAttribute
	public XMLGregorianCalendar getDate() {
		return date;
	}
	
	
	/**
	 * Sets the value of the date property.
	 * @param value
	 */
	public void setDate(XMLGregorianCalendar value) {
		this.date = value;
	}
	
	
	/**
	 * Sets the value of the date property, given a {@link Date} object.
	 * @param value
	 */
	public void setDate(Date d) {
		GregorianCalendar gc = new GregorianCalendar();
		
		gc.setTimeInMillis(d.getTime());
		try {
			this.date = DatatypeFactory.newInstance().
					newXMLGregorianCalendar(gc);
		} catch (Exception e) {
			this.date = null;
		}
	}
    
    
	/**
	 * Gets the value of the filesList property.
	 */
	public FilesListXML getFilesList() {
	    return filesList;
	}
	
	
	/**
	 * Sets the value of the filesList property.
	 * @param filesList
	 */
	public void setFilesList(FilesListXML filesList) {
	    this.filesList = filesList;
	}
	
	
	/**
	 * Gets the value of the Inputs property.
	 * @return
	 */
	@XmlElement(name = "Inputs")
	public Inputs getInputs() {
		return inputs;
	}
	
	
	/**
	 * Sets the value of the Inputs property.
	 */
	public void setInputs(Inputs inputsList) {
		this.inputs = inputsList;
	}
	
	
	/**
	 * Gets the value of the AnalysisSoftwareList property.
	 * @return
	 */
	@XmlElement(name = "AnalysisSoftwareList")
	public AnalysisSoftwareList getAnalysisSoftwareList() {
		return analysisSoftwareList;
	}
	
	
	/**
	 * Sets the value of the AnalysisSoftwareList property.
	 */
	public void setAnalysisSoftwareList(AnalysisSoftwareList analysisSoftwareList) {
		this.analysisSoftwareList = analysisSoftwareList;
	}

	
	/**
	 * Gets the value of the spectraList property.
	 * @return
	 */
	@XmlElement(name = "spectraList")
	public SpectraListXML getSpectraList() {
		return spectraList;
	}
	
	
	/**
	 * Sets the value of the spectraList property.
	 */
	public void setSpectraList(SpectraListXML spectraList) {
		this.spectraList = spectraList;
	}

	
	/**
	 * Gets the value of the peptidesList property.
	 * @return
	 */
	@XmlElement(name = "peptidesList")
	public PeptidesListXML getPeptidesList() {
		return peptidesList;
	}
	
	
	/**
	 * Sets the value of the peptidesList property.
	 */
	public void setPeptidesList(PeptidesListXML peptidesList) {
		this.peptidesList = peptidesList;
	}

	
	/**
	 * Gets the value of the peptidesList property.
	 * @return
	 */
	@XmlElement(name = "accessionsList")
	public AccessionsListXML getAccessionsList() {
		return accessionsList;
	}
	
	
	/**
	 * Sets the value of the peptidesList property.
	 */
	public void setAccessionsList(AccessionsListXML accessionsList) {
		this.accessionsList = accessionsList;
	}

	
	/**
	 * Gets the value of the groupsList property.
	 * @return
	 */
	@XmlElement(name = "groupsList")
	public GroupsListXML getGroupsList() {
		return groupsList;
	}
	
	
	/**
	 * Sets the value of the groupsList property.
	 */
	public void setGroupsList(GroupsListXML groupsList) {
		this.groupsList = groupsList;
	}
}
