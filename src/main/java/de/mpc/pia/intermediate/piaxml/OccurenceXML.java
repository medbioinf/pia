package de.mpc.pia.intermediate.piaxml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "occurrence")
public class OccurenceXML {
	private Long accessionRefID;
	
	private Integer start;
	
	private Integer end;
	
	
	/**
	 * Gets the value of the accessionRefID attribute.
	 * @return
	 */
	@XmlAttribute(required = true)
	public Long getAccessionRefID() {
		return accessionRefID;
	}
	
	
	/**
	 * Sets the value of the accessionRefID attribute.
	 */
	public void setAccessionRefID(Long accessionRefID) {
		this.accessionRefID = accessionRefID;
	}
	
	
	/**
	 * Gets the value of the start attribute.
	 * @return
	 */
	@XmlAttribute
	public int getStart() {
		return start;
	}
	
	
	/**
	 * Sets the value of the start attribute.
	 */
	public void setStart(int start) {
		this.start = start;
	}
	
	
	/**
	 * Gets the value of the end attribute.
	 * @return
	 */
	@XmlAttribute
	public int getEnd() {
		return end;
	}
	
	
	/**
	 * Sets the value of the end attribute.
	 */
	public void setEnd(int end) {
		this.end = end;
	}
}
