package de.mpc.pia.intermediate;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * A ModificationType describes the type of a modification via its amino acids,
 * mass shift and a modification name. These settings are usually given by a
 * search engine.
 * 
 * @author julian
 *
 */
public class Modification {
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
}
