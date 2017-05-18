package de.mpc.pia.intermediate;

import java.io.Serializable;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

import de.mpc.pia.intermediate.piaxml.ScoreXML;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * A ModificationType describes the type of a modification via its amino acids,
 * mass shift and a modification name. These settings are usually given by a
 * search engine.
 *
 * @author julian
 *
 */
public class Modification implements Serializable {

    private static final long serialVersionUID = -4903764803663298714L;

    /** amino acid residue of this modification */
    private Character residue;

    /** the mass shift of the modification (monoisotopic) */
    private Double mass;

    /** description of the modification */
    private String description;

    /** the UNIMOD accession (if accessible) */
    private String accession;

    /** the mass shift as formatted string */
    private String massString;

    private String cvLabel = null;

    /** formatter for the mass as string */
    private static final DecimalFormat df;

    private List<ScoreXML> probability;


    // initialize some static variables
    static {
        // we have a four digit formatter
        // TODO: this may be set up somewhere
        df = new DecimalFormat("0.####");
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
        df.setRoundingMode(RoundingMode.HALF_UP);
    }


    /**
     * Basic constructor, sets all values of the modification type.
     *
     * @param residue
     * @param mass
     * @param description
     */
    public Modification(Character residue, Double mass, String description,
            String acc) {
        this.residue = residue;
        this.mass = mass;
        this.description = description;
        this.accession = acc;
        this.massString = df.format(mass);
    }

    /**
     * Constructor including the probability of the Modification
     *
     * @param residue
     * @param mass
     * @param description
     */
    public Modification(Character residue, Double mass, String description,
                        String acc, List<ScoreXML> probability) {
        this.residue = residue;
        this.mass = mass;
        this.description = description;
        this.accession = acc;
        this.massString = df.format(mass);
        this.probability = probability;
    }

    /**
     * This constructor can handle the post-translational modifications
     * @param residue
     * @param mass
     * @param description
     * @param accession
     * @param massString
     * @param cvLabel
     * @param probability
     */
    public Modification(Character residue, Double mass, String description, String accession, String massString,
                        String cvLabel, List<ScoreXML> probability) {
        this.residue = residue;
        this.mass = mass;
        this.description = description;
        this.accession = accession;
        this.massString = massString;
        this.cvLabel = cvLabel;
        this.probability = probability;
    }

    /**
     * Getter for the residue, i.e. the amino acids, where the modification
     * occurs.
     *
     * @return
     */
    public Character getResidue() {
        return residue;
    }


    /**
     * Getter for the mass shift.
     *
     * @return
     */
    public Double getMass() {
        return mass;
    }


    /**
     * Gets the mass as string with four digits precision
     *
     * @return
     */
    public String getMassString() {
        return massString;
    }


    /**
     * Getter for the description.
     *
     * @return
     */
    public String getDescription() {
        return description;
    }


    /**
     * Getter for the UNIMOD accession.
     *
     * @return
     */
    public String getAccession() {
        return accession;
    }


    public List<ScoreXML >getProbability() {
        return probability;
    }

    public void setProbability(List<ScoreXML> probability) {
        this.probability = probability;
    }

    public String getCvLabel() {
        return cvLabel;
    }

    public void setCvLabel(String cvLabel) {
        this.cvLabel = cvLabel;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(23, 31).
                append(accession).
                append(description).
                append(mass).
                append(residue).
                toHashCode();
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Modification)) {
           return false;
       }
       if (obj == this) {
           return true;
       }

       Modification mod = (Modification)obj;
       return new EqualsBuilder().
               append(accession, mod.accession).
               append(description, mod.description).
               append(mass, mod.mass).
               append(residue, mod.residue).
               isEquals();
    }
}
