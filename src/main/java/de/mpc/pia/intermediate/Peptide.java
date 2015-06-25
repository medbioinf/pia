package de.mpc.pia.intermediate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Peptide implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/** ID of the peptide */
	private long ID;
	
	/** Sequence of the peptide */
	private String sequence;
	
	/** List of pointers to the spectra of this peptide */
	private List<PeptideSpectrumMatch> spectra;
	
	/** Pointer to the peptide's group. */
	private Group pGroup;
	
	/** the occurrences of this peptide, mapped from the accession */
	private Set<AccessionOccurrence> occurrences;
	
	
	
	/**
	 * Basic constructor, sets the spectra and pGroup to null and the score to NaN.
	 *  
	 * @param id
	 * @param seq
	 */
	public Peptide(long id, String seq) {
		this.ID = id;
		this.sequence = seq;
		this.spectra = null;
		this.pGroup = null;
		this.occurrences = new HashSet<AccessionOccurrence>();
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if ( !(obj instanceof Peptide) ) {
			return false;
		}
		
		Peptide objPeptide = (Peptide)obj;
	    if ((objPeptide.ID == this.ID) &&
	    		objPeptide.sequence.equals(sequence) &&
	    		(((spectra != null) && spectra.equals(objPeptide.spectra)) ||
	    				((spectra == null) && (objPeptide.spectra == null))) &&
	    		(((pGroup != null) && pGroup.equals(objPeptide.pGroup)) ||
	    				((pGroup == null) && (objPeptide.pGroup == null))) &&
	    		(objPeptide.occurrences.equals(this.occurrences))) {
	    	return true;
	    } else {
	    	return false;
	    }
	}
	
	
	@Override
	public int hashCode() {
		int hash = 0;
		
		hash += (new Long(ID)).hashCode();
		hash += (sequence != null) ? sequence.hashCode() : 0;
		hash += (spectra != null) ? spectra.hashCode() : 0;
		
		// we can't take the groups hash, because it references back to the peptide
		hash += (pGroup != null) ? hash += pGroup.getID() : 0;
		
		hash += occurrences.hashCode();
		
		return hash;
	}
	
	
	/**
	 * Getter for the ID.
	 * 
	 * @return
	 */
	public long getID() {
		return ID;
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
	public boolean addAccessionOccurrence(Accession accession,
			int start, int end) {
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
