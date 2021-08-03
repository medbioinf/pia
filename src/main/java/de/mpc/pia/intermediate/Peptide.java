package de.mpc.pia.intermediate;

import java.io.Serializable;
import java.util.*;


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
        this.occurrences = new HashSet<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Peptide peptide = (Peptide) o;

        if (id != peptide.id) return false;
        if (!sequence.equals(peptide.sequence)) return false;
        if (!Objects.equals(spectra, peptide.spectra)) return false;
        if (pGroup != null ? pGroup.getID() != peptide.pGroup.getID() : peptide.pGroup != null) return false;
        return occurrences.equals(peptide.occurrences);
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + sequence.hashCode();
        result = 31 * result + (spectra != null ? spectra.hashCode() : 0);
        result = 31 * result + (pGroup != null ? (int) (pGroup.getID() ^ (pGroup.getID() >>>32)) : 0);
        result = 31 * result + occurrences.hashCode();
        return result;
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
            spectra = new ArrayList<>();
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
