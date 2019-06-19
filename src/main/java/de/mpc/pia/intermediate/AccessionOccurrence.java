package de.mpc.pia.intermediate;

import java.io.Serializable;

/**
 * This class describes the occurrence of an Accession's dbSequence of a
 * Peptide.
 *
 * @author julian
 *
 */
public class AccessionOccurrence implements Serializable {

    private static final long serialVersionUID = 1738822043859868668L;

    /** the Accession */
    Accession accession;

    /** start in the DBSequence for this occurrence */
    Integer start;

    /** end in the DBSequence for this occurrence */
    Integer end;


    /**
     * Simple constructor
     * @param accession
     * @param start
     * @param end
     */
    public AccessionOccurrence(Accession accession, Integer start, Integer end) {
        this.accession = accession;
        this.start = start;
        this.end = end;
    }


    public Accession getAccession() {
        return accession;
    }


    public Integer getStart() {
        return start;
    }


    public Integer getEnd() {
        return end;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AccessionOccurrence that = (AccessionOccurrence) o;

        if (accession != null && accession.getID() != null ? !accession.getID().equals(that.accession.getID()) : that.accession != null && that.accession.getID() != null) return false;
        if (start != null ? !start.equals(that.start) : that.start != null) return false;
        return end != null ? end.equals(that.end) : that.end == null;
    }

    @Override
    public int hashCode() {
        int result = accession != null && accession.getID() != null ? accession.getID().hashCode() : 0;
        result = 31 * result + (start != null ? start.hashCode() : 0);
        result = 31 * result + (end != null ? end.hashCode() : 0);
        return result;
    }
}
