package de.mpc.pia.tools.openms.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for the SearchParameters tag of IdXML.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "fixedModification",
    "variableModification",
    "userParam"
})
public class SearchParameters {

    @XmlElement(name = "FixedModification")
    protected List<FixedModification> fixedModification;
    @XmlElement(name = "VariableModification")
    protected List<VariableModification> variableModification;
    @XmlElement(name = "UserParam")
    protected List<UserParamIdXML> userParam;
    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;
    @XmlAttribute(required = true)
    protected String db;
    @XmlAttribute(name = "db_version", required = true)
    protected String dbVersion;
    @XmlAttribute
    protected String taxonomy;
    @XmlAttribute(name = "mass_type", required = true)
    protected MassType massType;
    @XmlAttribute(required = true)
    protected String charges;
    @XmlAttribute
    protected DigestionEnzyme enzyme;
    @XmlAttribute(name = "missed_cleavages")
    @XmlSchemaType(name = "unsignedInt")
    protected Long missedCleavages;
    @XmlAttribute(name = "precursor_peak_tolerance", required = true)
    protected float precursorPeakTolerance;
    @XmlAttribute(name = "peak_mass_tolerance", required = true)
    protected float peakMassTolerance;

    /**
     * Gets the value of the fixedModification property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the fixedModification property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFixedModification().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link FixedModification }
     * 
     * 
     */
    public List<FixedModification> getFixedModification() {
        if (fixedModification == null) {
            fixedModification = new ArrayList<>();
        }
        return this.fixedModification;
    }

    /**
     * Gets the value of the variableModification property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the variableModification property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getVariableModification().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link VariableModification }
     * 
     * 
     */
    public List<VariableModification> getVariableModification() {
        if (variableModification == null) {
            variableModification = new ArrayList<>();
        }
        return this.variableModification;
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
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the db property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDb() {
        return db;
    }

    /**
     * Sets the value of the db property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDb(String value) {
        this.db = value;
    }

    /**
     * Gets the value of the dbVersion property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDbVersion() {
        return dbVersion;
    }

    /**
     * Sets the value of the dbVersion property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDbVersion(String value) {
        this.dbVersion = value;
    }

    /**
     * Gets the value of the taxonomy property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTaxonomy() {
        return taxonomy;
    }

    /**
     * Sets the value of the taxonomy property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTaxonomy(String value) {
        this.taxonomy = value;
    }

    /**
     * Gets the value of the massType property.
     * 
     * @return
     *     possible object is
     *     {@link MassType }
     *     
     */
    public MassType getMassType() {
        return massType;
    }

    /**
     * Sets the value of the massType property.
     * 
     * @param value
     *     allowed object is
     *     {@link MassType }
     *     
     */
    public void setMassType(MassType value) {
        this.massType = value;
    }

    /**
     * Gets the value of the charges property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCharges() {
        return charges;
    }

    /**
     * Sets the value of the charges property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCharges(String value) {
        this.charges = value;
    }

    /**
     * Gets the value of the enzyme property.
     * 
     * @return
     *     possible object is
     *     {@link DigestionEnzyme }
     *     
     */
    public DigestionEnzyme getEnzyme() {
        return enzyme;
    }

    /**
     * Sets the value of the enzyme property.
     * 
     * @param value
     *     allowed object is
     *     {@link DigestionEnzyme }
     *     
     */
    public void setEnzyme(DigestionEnzyme value) {
        this.enzyme = value;
    }

    /**
     * Gets the value of the missedCleavages property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getMissedCleavages() {
        return missedCleavages;
    }

    /**
     * Sets the value of the missedCleavages property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setMissedCleavages(Long value) {
        this.missedCleavages = value;
    }

    /**
     * Gets the value of the precursorPeakTolerance property.
     * 
     */
    public float getPrecursorPeakTolerance() {
        return precursorPeakTolerance;
    }

    /**
     * Sets the value of the precursorPeakTolerance property.
     * 
     */
    public void setPrecursorPeakTolerance(float value) {
        this.precursorPeakTolerance = value;
    }

    /**
     * Gets the value of the peakMassTolerance property.
     * 
     */
    public float getPeakMassTolerance() {
        return peakMassTolerance;
    }

    /**
     * Sets the value of the peakMassTolerance property.
     * 
     */
    public void setPeakMassTolerance(float value) {
        this.peakMassTolerance = value;
    }

}