package de.mpc.pia.tools.openms.jaxb;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for the PeptideHit of IdXML
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userParam"
})
public class PeptideHit {

    @XmlElement(name = "UserParam")
    protected List<UserParam> userParam;
    @XmlAttribute(required = true)
    protected String sequence;
    @XmlAttribute(required = true)
    protected BigInteger charge;
    @XmlAttribute(required = true)
    protected float score;
    @XmlAttribute(name = "aa_before")
    protected String aaBefore;
    @XmlAttribute(name = "aa_after")
    protected String aaAfter;
    @XmlAttribute(name = "protein_refs")
    @XmlIDREF
    @XmlSchemaType(name = "IDREFS")
    protected List<Object> proteinRefs;

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
     * {@link UserParam }
     * 
     * 
     */
    public List<UserParam> getUserParam() {
        if (userParam == null) {
            userParam = new ArrayList<UserParam>();
        }
        return this.userParam;
    }

    /**
     * Gets the value of the sequence property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSequence() {
        return sequence;
    }

    /**
     * Sets the value of the sequence property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSequence(String value) {
        this.sequence = value;
    }

    /**
     * Gets the value of the charge property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getCharge() {
        return charge;
    }

    /**
     * Sets the value of the charge property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setCharge(BigInteger value) {
        this.charge = value;
    }

    /**
     * Gets the value of the score property.
     * 
     */
    public float getScore() {
        return score;
    }

    /**
     * Sets the value of the score property.
     * 
     */
    public void setScore(float value) {
        this.score = value;
    }

    /**
     * Gets the value of the aaBefore property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAaBefore() {
        return aaBefore;
    }

    /**
     * Sets the value of the aaBefore property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAaBefore(String value) {
        this.aaBefore = value;
    }

    /**
     * Gets the value of the aaAfter property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAaAfter() {
        return aaAfter;
    }

    /**
     * Sets the value of the aaAfter property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAaAfter(String value) {
        this.aaAfter = value;
    }

    /**
     * Gets the value of the proteinRefs property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the proteinRefs property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getProteinRefs().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     * 
     * 
     */
    public List<Object> getProteinRefs() {
        if (proteinRefs == null) {
            proteinRefs = new ArrayList<Object>();
        }
        return this.proteinRefs;
    }

}