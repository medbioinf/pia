package de.mpc.pia.tools.openms.jaxb;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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
    private List<UserParamIdXML> userParam;
    @XmlAttribute(required = true)
    private String sequence;
    @XmlAttribute(required = true)
    private BigInteger charge;
    @XmlAttribute(required = true)
    private float score;
    @XmlAttribute(name = "aa_before")
    private String aaBefore;
    @XmlAttribute(name = "aa_after")
    private String aaAfter;
    @XmlAttribute(name = "protein_refs")
    @XmlIDREF
    @XmlSchemaType(name = "IDREFS")
    private List<Object> proteinRefs;

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
     * Returns the index of the specified protein reference
     *
     * @param protRef
     * @return
     */
    private int findProteinHitIndexById(String protRef) {
        ListIterator<Object> iter = proteinRefs.listIterator();
        int idx = -1;
        while (iter.hasNext()) {
            Object proteinObj = iter.next();
            idx++;
            if (!(proteinObj instanceof ProteinHit)) {
                continue;
            }
            if (((ProteinHit)proteinObj).getId().equals(protRef)) {
                return idx;
            }
        }

        return -1;
    }


    /**
     * Gets the value of the amino acid before this peptide for the protein with
     * the specified reference.
     *
     * @param protRef the reference to the protein (one of {@link #getProteinRefs()}
     * @return the AA before the peptideHit in the specified protein or null, if
     * there is no such protein.
     */
    public String getAaBefore(String protRef) {
        int idx = findProteinHitIndexById(protRef);
        return getAaByIndex(idx);
    }

    private String getAaByIndex(int index){
        if (index > -1) {
            String[] aaArr = aaBefore.split(" ");
            if (aaArr.length > 1) {
                return aaAfter.split(" ")[index];
            } else {
                return aaArr[0];
            }
        }
        return null;
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
     * Gets the value of the amino acid after this peptide for the protein with
     * the specified reference.
     *
     * @param protRef the reference to the protein (one of {@link #getProteinRefs()}
     * @return the AA after the peptideHit in the specified protein or null, if
     * there is no such protein.
     */
    public String getAaAfter(String protRef) {
        int idx = findProteinHitIndexById(protRef);
        return getAaByIndex(idx);
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
            proteinRefs = new ArrayList<>();
        }
        return this.proteinRefs;
    }

}