package de.mpc.pia.tools.openms.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for the PeptideIdentification of IdXML
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "peptideHit",
    "userParam"
})
public class PeptideIdentification {

    @XmlElement(name = "PeptideHit")
    private List<PeptideHit> peptideHit;
    @XmlElement(name = "UserParam")
    private List<UserParamIdXML> userParam;
    @XmlAttribute(name = "score_type", required = true)
    private String scoreType;
    @XmlAttribute(name = "higher_score_better", required = true)
    private boolean higherScoreBetter;
    @XmlAttribute(name = "significance_threshold")
    private Float significanceThreshold;
    @XmlAttribute(name = "spectrum_reference")
    @XmlSchemaType(name = "unsignedInt")
    private Long spectrumReference;
    @XmlAttribute(name = "RT")
    private Float rt;
    @XmlAttribute(name = "MZ")
    private Float mz;

    /**
     * Gets the value of the peptideHit property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the peptideHit property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPeptideHit().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PeptideHit }
     * 
     * 
     */
    public List<PeptideHit> getPeptideHit() {
        if (peptideHit == null) {
            peptideHit = new ArrayList<>();
        }
        return this.peptideHit;
    }

    /**
     * Gets the value of the userParam property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the userParam property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getUserParam().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link UserParamIdXML }
     * 
     * 
     */
    public List<UserParamIdXML> getUserParam() {
        if (userParam == null) {
            userParam = new ArrayList<>();
        }
        return this.userParam;
    }

    /**
     * Gets the value of the scoreType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getScoreType() {
        return scoreType;
    }

    /**
     * Sets the value of the scoreType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setScoreType(String value) {
        this.scoreType = value;
    }

    /**
     * Gets the value of the higherScoreBetter property.
     * 
     */
    public boolean isHigherScoreBetter() {
        return higherScoreBetter;
    }

    /**
     * Sets the value of the higherScoreBetter property.
     * 
     */
    public void setHigherScoreBetter(boolean value) {
        this.higherScoreBetter = value;
    }

    /**
     * Gets the value of the significanceThreshold property.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getSignificanceThreshold() {
        return significanceThreshold;
    }

    /**
     * Sets the value of the significanceThreshold property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     */
    public void setSignificanceThreshold(Float value) {
        this.significanceThreshold = value;
    }

    /**
     * Gets the value of the spectrumReference property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getSpectrumReference() {
        return spectrumReference;
    }

    /**
     * Sets the value of the spectrumReference property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setSpectrumReference(Long value) {
        this.spectrumReference = value;
    }

    /**
     * Gets the value of the rt property.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getRT() {
        return rt;
    }

    /**
     * Sets the value of the rt property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     */
    public void setRT(Float value) {
        this.rt = value;
    }

    /**
     * Gets the value of the mz property.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getMZ() {
        return mz;
    }

    /**
     * Sets the value of the mz property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     */
    public void setMZ(Float value) {
        this.mz = value;
    }

}