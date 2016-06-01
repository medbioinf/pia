package de.mpc.pia.intermediate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Peptide implements Serializable {

    private static final long serialVersionUID = 6576568401621880177L;

    /** ID of the peptide */
    private long id;

    /** Sequence of the peptide */
    private String sequence;

    /** List of pointers to the spectra of this peptide */
    private List<PeptideSpectrumMatch> spectra;

    /** Pointer to the peptide's group. */
    private Group pGroup;

    /** the occurrences of this peptide, mapped from the accession */
    private HashSet<AccessionOccurrence> occurrences;



    /**
     * Basic constructor, sets the spectra and pGroup to null and the score to NaN.
     *
     * @param id
     * @param seq
     */
    public Peptide(long id, String seq) {
        this.id = id;
        this.sequence = seq;
        this.spectra = null;
        this.pGroup = null;
        this.occurrences = new HashSet<AccessionOccurrence>();
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Peptide)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        Peptide objPeptide = (Peptide)obj;
        return new EqualsBuilder().
                append(id, objPeptide.id).
                append(sequence, objPeptide.sequence).
                append(spectra, objPeptide.spectra).
                append(pGroup, objPeptide.pGroup).
                append(occurrences, objPeptide.occurrences).
                isEquals();
    }


    @Override
    public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder(23, 31).
                append(id).
                append(sequence);

        if (spectra != null) {
            hcb.append(spectra.hashCode());
        }
        if (pGroup != null) {
            hcb.append(pGroup.getID());
        }

        hcb.append(occurrences);

        return hcb.toHashCode();
    }


    /**
     * Getter for the ID.
     *
     * @return
     */
    public long getID() {
        return id;
    }


    /**
     * Getter for the sequence.
     *
     * @return
     */
    public String getSequence() {
        return sequence;
    }


    /**
     * Setter for the spectra.
     *
     * @param spectra
     */
    public void setSpectra(List<PeptideSpectrumMatch> spectra) {
        this.spectra = spectra;
    }


    /**
     * adds the given spectra to the list of spectra.
     *
     * @param spectrum
     */
    public void addSpectrum(PeptideSpectrumMatch spectrum) {
        if (spectra == null) {
            spectra = new ArrayList<PeptideSpectrumMatch>();
        } else {
            for (PeptideSpectrumMatch s : spectra) {
                if (s.equals(spectrum)) {
                    // spectrum already in peptide
                    return;
                }
            }
        }

        spectra.add(spectrum);
    }


    /**
     * Getter for the spectra.
     *
     * @return
     */
    public List<PeptideSpectrumMatch> getSpectra() {
        return spectra;
    }


    /**
     * Setter for the group.
     *
     * @param group
     */
    public void setGroup(Group group) {
        this.pGroup = group;
    }


    /**
     * Getter for the group.
     *
     * @return
     */
    public Group getGroup() {
        return pGroup;
    }


    /**
     * Adds a new {@link AccessionOccurrence} with the given parameters to the
     * occurences set.
     *
     * @param accession
     * @param start
     * @param end
     * @return true if the set did not already contain this occurrence
     */
    public boolean addAccessionOccurrence(Accession accession, int start, int end) {
        return occurrences.add(new AccessionOccurrence(accession, start, end));
    }


    /**
     * getter for the AccessionOccurrences
     * @return
     */
    public Set<AccessionOccurrence> getAccessionOccurrences() {
        return occurrences;
    }
}
