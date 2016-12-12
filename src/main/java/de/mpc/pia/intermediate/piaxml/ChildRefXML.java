package de.mpc.pia.intermediate.piaxml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "childRef")
public class ChildRefXML {
	private Long childRefID;
	
	
	/**
	 * Basic no-arg constructor.
	 */
	public ChildRefXML() {
	}
	
	
	/**
	 * Basic no-arg constructor.
	 */
	public ChildRefXML(Long childRefID) {
		this.childRefID = childRefID;
	}
	
	
	/**
	 * Gets the value of the childRefID attribute.
	 * @return
	 */
	@XmlAttribute(required = true)
	public Long getChildRefID() {
		return childRefID;
	}
	
	
	/**
	 * Sets the value of the childRefID attribute.
	 */
	public void setChildRefID(Long childRefID) {
		this.childRefID = childRefID;
	}
}
