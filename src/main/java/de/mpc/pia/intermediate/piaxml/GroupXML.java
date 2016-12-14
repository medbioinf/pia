package de.mpc.pia.intermediate.piaxml;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.mpc.pia.intermediate.Group;


@XmlRootElement(name = "group")
//defines the the order in which the fields are written
@XmlType(propOrder = {
		"accessionsRefList",
		"peptidesRefList",
		"childrenRefList"
	})
public class GroupXML {
	private Long id;
	
	private Long treeId;
	
	
	@XmlElementWrapper(name = "accessionsRefList")
	@XmlElement(name = "accessionRef")
	private List<AccessionRefXML> accessionsRefList;
	
	@XmlElementWrapper(name = "peptidesRefList")
	@XmlElement(name = "peptideRef")
	private List<PeptideRefXML> peptidesRefList;
	
	@XmlElementWrapper(name = "childrenRefList")
	@XmlElement(name = "childRef")
	private List<ChildRefXML> childrenRefList;
	
	
	/**
	 * Basic no-arg constructor.
	 */
	public GroupXML() {
	}
	
	
	/**
	 * Basic constructor setting the data from the given peptide.
	 */
	public GroupXML(Group group) {
		id = group.getID();
		treeId = group.getTreeID();
		
		if (group.getAccessions().size() > 0) {
			accessionsRefList = new ArrayList<>();
			accessionsRefList.addAll(group.getAccessions().values().stream().map(accession -> new AccessionRefXML(accession.getID())).collect(Collectors.toList()));
		}
		
		if (group.getPeptides() != null) {
			peptidesRefList = new ArrayList<>();
			peptidesRefList.addAll(group.getPeptides().values().stream().map(pep -> new PeptideRefXML(pep.getID())).collect(Collectors.toList()));
		}
		
		if (group.getChildren().size() > 0) {
			childrenRefList = new ArrayList<>();
			childrenRefList.addAll(group.getChildren().keySet().stream().map(ChildRefXML::new).collect(Collectors.toList()));
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
	 * Gets the value of the treeId attribute.
	 * @return
	 */
	@XmlAttribute(required = true)
	public Long getTreeId() {
		return treeId;
	}
	
	
	/**
	 * Sets the value of the treeId attribute.
	 */
	public void setTreeId(Long treeId) {
		this.treeId = treeId;
	}
	
	
	/**
	 * Getter for the accessionsRefList
	 * @return
	 */
	public List<AccessionRefXML> getAccessionsRefList() {
		if (accessionsRefList == null) {
			accessionsRefList = new ArrayList<>();
		}
		return accessionsRefList;
	}
	
	
	/**
	 * Getter for the peptidesRefList
	 * @return
	 */
	public List<PeptideRefXML> getPeptidesRefList() {
		if (peptidesRefList == null) {
			peptidesRefList = new ArrayList<>();
		}
		return peptidesRefList;
	}
	
	
	/**
	 * Getter for the childrenRefList
	 * @return
	 */
	public List<ChildRefXML> getChildrenRefList() {
		if (childrenRefList == null) {
			childrenRefList = new ArrayList<>();
		}
		return childrenRefList;
	}
	
}
