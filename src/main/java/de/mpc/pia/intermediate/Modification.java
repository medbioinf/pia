package de.mpc.pia.intermediate;

import java.io.Serializable;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.mpc.pia.modeller.score.ScoreModel;

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

    /** formatter for the mass as string */
    private static final DecimalFormat df;

    private String cvLabel;

    private List<ScoreModel> probability;


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
        this.cvLabel = null;
        this.probability = null;
    }

    /**
     * Constructor including the probability of the Modification
     *
     * @param residue
     * @param mass
     * @param description
     */
    public Modification(Character residue, Double mass, String description,
                        String acc, List<ScoreModel> probability) {
        this.residue = residue;
        this.mass = mass;
        this.description = description;
        this.accession = acc;
        this.massString = df.format(mass);
        this.cvLabel = null;
        this.probability = probability;
    }

    /**
     * This constructor can handle the post-translational modifications
     * @param residue
     * @param mass
     * @param description
     * @param accession
     * @param cvLabel
     * @param probability
     */
    public Modification(Character residue, Double mass, String description, String accession,
                        String cvLabel, List<ScoreModel> probability) {
        this.residue = residue;
        this.mass = mass;
        this.description = description;
        this.accession = accession;
        this.massString = df.format(mass);
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


    public List<ScoreModel> getProbability() {
        return probability;
    }

    public void setProbability(List<ScoreModel> probability) {
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
        int result = 31;

        result = 31 * result + ((accession!=null)?accession.hashCode():0);
        result = 31 * result + ((description!=null)?description.hashCode():0);
        long doubleFieldBits = Double.doubleToLongBits(mass);
        result = 31 * result + (int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
        result = 31 * result + ((residue!=null)?residue:0);
        result = 31 * result + ((cvLabel!=null)?cvLabel.hashCode():0);
        result = 31 * result + ((probability!=null)?probability.hashCode():0);
        return result;
    }

    /**
     * Fast implementation of equals because it is not using reflection and
     * object creation.
     *
     * @param o Modification to be compare.
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Modification that = (Modification) o;

        if (!Objects.equals(residue, that.residue)) return false;
        if (!Objects.equals(mass, that.mass)) return false;
        if (!Objects.equals(description, that.description)) return false;
        if (!Objects.equals(accession, that.accession)) return false;
        if (!Objects.equals(cvLabel, that.cvLabel)) return false;
        return Objects.equals(probability, that.probability);
    }

    @Override
    public String toString() {
        return "Modification{" +
                "residue=" + residue +
                ", mass=" + mass +
                ", description='" + description + '\'' +
                ", accession='" + accession + '\'' +
                ", massString='" + massString + '\'' +
                ", cvLabel='" + cvLabel + '\'' +
                ", probability=" + probability +
                '}';
    }
}
