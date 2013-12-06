package de.mpc.pia.intermediate.piaxml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.mpc.pia.intermediate.Accession;


@XmlRootElement(name = "accession")
//defines the the order in which the fields are written
@XmlType(propOrder = {
		"sequence",
		"fileRefs",
		"searchDatabaseRefs",
		"descriptions"
	})
public class AccessionXML {
	
	private Long id;
	
	private String acc;
	
	
	private String sequence;
	
	@XmlElement(name = "FileRef")
	List<FileRefXML> fileRefs;
	
	@XmlElement(name = "SearchDatabaseRef")
	List<SearchDatabaseRefXML> searchDatabaseRefs;
	
	@XmlElement(name = "Description")
	List<DescriptionXML> descriptions;
	
	
	/**
	 * Basic no-arg constructor.
	 */
	public AccessionXML() {
	}
	
	
	/**
	 * Basic constructor setting the data from the given peptide.
	 * @param psm
	 */
	public AccessionXML(Accession accession) {
		id = accession.getID();
		acc = accession.getAccession();
		
		sequence = accession.getDbSequence();
		
		fileRefs = new ArrayList<FileRefXML>(accession.getFiles().size());
		for (Long fileID : accession.getFiles()) {
			fileRefs.add(new FileRefXML(fileID));
		}
		
		searchDatabaseRefs = new ArrayList<SearchDatabaseRefXML>(
				accession.getSearchDatabaseRefs().size());
		for (String ref : accession.getSearchDatabaseRefs()) {
			searchDatabaseRefs.add(new SearchDatabaseRefXML(ref));
		}
		
		descriptions = new ArrayList<DescriptionXML>(
				accession.getDescriptions().size());
		for (Map.Entry<Long, String> descIt : accession.getDescriptions().entrySet()) {
			DescriptionXML desc = new DescriptionXML();
			
			desc.setFileRefID(descIt.getKey());
			desc.setValue(descIt.getValue());
			
			descriptions.add(desc);
		}
	}
	
	
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
	 * Gets the value of the acc attribute.
	 * @return
	 */
	@XmlAttribute
	public String getAcc() {
		return acc;
	}
	
	
	/**
	 * Sets the value of the acc attribute.
	 */
	public void setAcc(String acc) {
		this.acc = acc;
	}
	
	
	/**
	 * Gets the value of the sequence property.
	 * @return
	 */
	@XmlElement(name = "Sequence")
	public String getSequence() {
		return sequence;
	}
	
	
	/**
	 * Sets the value of the sequence property.
	 * @param fileName
	 */
	public void setSequence(String sequence) {
		this.sequence = sequence;
	}
	
	
	/**
	 * Getter for the fileRefs.
	 * @return
	 */
	public List<FileRefXML> getFileRefs() {
		if (fileRefs == null) {
			fileRefs = new ArrayList<FileRefXML>();
		}
		return fileRefs;
	}
	
	
	/**
	 * Getter for the searchDatabaseRefs.
	 * @return
	 */
	public List<DescriptionXML> getDescriptions() {
		if (descriptions == null) {
			descriptions = new ArrayList<DescriptionXML>();
		}
		return descriptions;
	}
	
	
	/**
	 * Getter for the searchDatabaseRefs.
	 * @return
	 */
	public List<SearchDatabaseRefXML> getSearchDatabaseRefs() {
		if (searchDatabaseRefs == null) {
			searchDatabaseRefs = new ArrayList<SearchDatabaseRefXML>();
		}
		return searchDatabaseRefs;
	}
}
