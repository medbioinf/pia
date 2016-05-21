package de.mpc.pia.intermediate;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * This class describes the occurrence of an Accession's dbSequence of a
 * Peptide.
 *
 * @author julian
 *
 */
public class AccessionOccurrence {

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
    public boolean equals(Object obj) {
        if (!(obj instanceof AccessionOccurrence)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        AccessionOccurrence objAO= (AccessionOccurrence)obj;
        return new EqualsBuilder().
                append(accession.getID(), objAO.accession.getID()).
                append(start, objAO.start).
                append(end, objAO.end).
                isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(23, 31).
                append(accession.getID()).
                append(start).
                append(end).
                toHashCode();
    }
}
