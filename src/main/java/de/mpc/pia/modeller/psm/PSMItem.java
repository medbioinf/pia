package de.mpc.pia.modeller.psm;

import java.util.Map;

import de.mpc.pia.intermediate.Modification;

/**
 * A general PSM class
 *
 * @author julian
 *
 */
public interface PSMItem {
    /**
     * Returns the shown sequence (without modifications).
     *
     * @return
     */
    public String getSequence();


    /**
     * Returns the charge of the item.
     * @return
     */
    public int getCharge();


    /**
     * Getter for the mass to charge value
     * @return
     */
    public double getMassToCharge();


    /**
     * Getter for the missed cleavages
     * @return
     */
    public int getMissedCleavages();


    /**
     * Getter for the retention time.
     *
     * @return
     */
    public Double getRetentionTime();


    /**
     * Getter for the source ID.
     *
     * @return
     */
    public String getSourceID();


    /**
     * Getter for the spectrum title.
     *
     * @return
     */
    public String getSpectrumTitle();


    /**
     * Getter for the delta mass.
     * @return
     */
    public double getDeltaMass();


    /**
     * Getter for the modifications of this item.
     * @return
     */
    public Map<Integer, Modification> getModifications();
}
