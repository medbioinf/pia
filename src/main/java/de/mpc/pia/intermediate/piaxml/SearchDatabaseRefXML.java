package de.mpc.pia.intermediate.piaxml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "SearchDatabaseRef")
public class SearchDatabaseRefXML {
	private String searchDatabase_ref;
	
	
	/**
	 * Basic no-arg constructor.
	 */
	public SearchDatabaseRefXML() {
	}
	
	
	/**
	 * Basic no-arg constructor.
	 */
	public SearchDatabaseRefXML(String searchDatabase_ref) {
		this.searchDatabase_ref = searchDatabase_ref;
	}
	
	
	/**
	 * Gets the value of the searchDatabase_ref attribute.
	 * @return
	 */
	@XmlAttribute(required = true)
	public String getSearchDatabase_ref() {
		return searchDatabase_ref;
	}
	
	
	/**
	 * Sets the value of the searchDatabase_ref attribute.
	 */
	public void setSearchDatabase_ref(String searchDatabase_ref) {
		this.searchDatabase_ref = searchDatabase_ref;
	}
}
