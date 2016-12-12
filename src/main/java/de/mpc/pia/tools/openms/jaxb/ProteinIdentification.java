package de.mpc.pia.tools.openms.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ProteinIdentification in IdXML
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "proteinHit",
    "userParam"
})
public class ProteinIdentification {

    @XmlElement(name = "ProteinHit")
    protected List<ProteinHit> proteinHit;
    @XmlElement(name = "UserParam")
    protected List<UserParamIdXML> userParam;
    @XmlAttribute(name = "score_type", required = true)
    protected String scoreType;
    @XmlAttribute(name = "higher_score_better", required = true)
    protected boolean higherScoreBetter;
    @XmlAttribute(name = "significance_threshold")
    protected Float significanceThreshold;

    /**
     * Gets the value of the proteinHit property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the proteinHit property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getProteinHit().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ProteinHit }
     * 
     * 
     */
    public List<ProteinHit> getProteinHit() {
        if (proteinHit == null) {
            proteinHit = new ArrayList<>();
        }
        return this.proteinHit;
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

}