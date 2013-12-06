package de.mpc.pia.intermediate.piaxml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.mpc.pia.intermediate.AccessionOccurrence;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;


@XmlRootElement(name = "peptide")
//defines the the order in which the fields are written
@XmlType(propOrder = {
		"sequence",
		"spectrumRefList",
		"occurrences",
	})
public class PeptideXML {
	private Long id;
	
	private String sequence;
	
	private SpectrumRefListXML spectrumRefList;
	
	private OccurrencesXML occurrences;
	
	
	/**
	 * Basic no-arg constructor.
	 */
	public PeptideXML() {
	}
	
	
	/**
	 * Basic constructor setting the data from the given peptide.
	 * @param psm
	 */
	public PeptideXML(Peptide peptide) {
		id = peptide.getID();
		sequence = peptide.getSequence();
		
		spectrumRefList = new SpectrumRefListXML();
		if (peptide.getSpectra() != null) {
			for (PeptideSpectrumMatch psm : peptide.getSpectra()) {
				spectrumRefList.getSpectrumRefs().add(new SpectrumRefXML(psm.getID()));
			}
		}
		
		occurrences = new OccurrencesXML();
		for(AccessionOccurrence occ : peptide.getAccessionOccurrences()) {
			OccurenceXML occurrence = new OccurenceXML();
			occurrence.setAccessionRefID(occ.getAccession().getID());
			occurrence.setStart(occ.getStart());
			occurrence.setEnd(occ.getEnd());
			occurrences.getOccurrences().add(occurrence);
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
	 * Gets the value of the spectrumRefList property.
	 * @return
	 */
	@XmlElement(name = "spectrumRefList")
	public SpectrumRefListXML getSpectrumRefList() {
		return spectrumRefList;
	}
	
	
	/**
	 * Sets the value of the spectrumRefList property.
	 * @param fileName
	 */
	public void setSpectrumRefList(SpectrumRefListXML spectrumRefList) {
		this.spectrumRefList = spectrumRefList;
	}
	
	
	/**
	 * Gets the value of the occurrences property.
	 * @return
	 */
	@XmlElement(name = "occurrences")
	public OccurrencesXML getOccurrences() {
		return occurrences;
	}
	
	
	/**
	 * Sets the value of the occurrences property.
	 * @param fileName
	 */
	public void setOccurrences(OccurrencesXML occurrences) {
		this.occurrences = occurrences;
	}
}
