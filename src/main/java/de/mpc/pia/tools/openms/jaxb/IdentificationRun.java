package de.mpc.pia.tools.openms.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for the IdentificationRun tag of IdXML
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "proteinIdentification",
    "peptideIdentification"
})
public class IdentificationRun {

    @XmlElement(name = "ProteinIdentification")
    protected ProteinIdentification proteinIdentification;
    @XmlElement(name = "PeptideIdentification")
    protected List<PeptideIdentification> peptideIdentification;
    @XmlAttribute(name = "search_engine", required = true)
    protected String searchEngine;
    @XmlAttribute(name = "search_engine_version", required = true)
    protected String searchEngineVersion;
    @XmlAttribute(required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar date;
    @XmlAttribute(name = "search_parameters_ref", required = true)
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object searchParametersRef;

    /**
     * Gets the value of the proteinIdentification property.
     * 
     * @return
     *     possible object is
     *     {@link ProteinIdentification }
     *     
     */
    public ProteinIdentification getProteinIdentification() {
        return proteinIdentification;
    }

    /**
     * Sets the value of the proteinIdentification property.
     * 
     * @param value
     *     allowed object is
     *     {@link ProteinIdentification }
     *     
     */
    public void setProteinIdentification(ProteinIdentification value) {
        this.proteinIdentification = value;
    }

    /**
     * Gets the value of the peptideIdentification property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the peptideIdentification property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPeptideIdentification().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PeptideIdentification }
     * 
     * 
     */
    public List<PeptideIdentification> getPeptideIdentification() {
        if (peptideIdentification == null) {
            peptideIdentification = new ArrayList<PeptideIdentification>();
        }
        return this.peptideIdentification;
    }

    /**
     * Gets the value of the searchEngine property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSearchEngine() {
        return searchEngine;
    }

    /**
     * Sets the value of the searchEngine property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSearchEngine(String value) {
        this.searchEngine = value;
    }

    /**
     * Gets the value of the searchEngineVersion property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSearchEngineVersion() {
        return searchEngineVersion;
    }

    /**
     * Sets the value of the searchEngineVersion property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSearchEngineVersion(String value) {
        this.searchEngineVersion = value;
    }

    /**
     * Gets the value of the date property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getDate() {
        return date;
    }

    /**
     * Sets the value of the date property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setDate(XMLGregorianCalendar value) {
        this.date = value;
    }

    /**
     * Gets the value of the searchParametersRef property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getSearchParametersRef() {
        return searchParametersRef;
    }

    /**
     * Sets the value of the searchParametersRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setSearchParametersRef(Object value) {
        this.searchParametersRef = value;
    }

}