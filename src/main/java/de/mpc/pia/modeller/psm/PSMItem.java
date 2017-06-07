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
    String getSequence();


    /**
     * Returns the charge of the item.
     * @return
     */
    int getCharge();


    /**
     * Getter for the mass to charge value
     * @return
     */
    double getMassToCharge();


    /**
     * Getter for the missed cleavages
     * @return
     */
    int getMissedCleavages();


    /**
     * Getter for the retention time.
     *
     * @return
     */
    Double getRetentionTime();


    /**
     * Getter for the source ID.
     *
     * @return
     */
    String getSourceID();


    /**
     * Getter for the spectrum title.
     *
     * @return
     */
    String getSpectrumTitle();


    /**
     * Getter for the delta mass.
     * @return
     */
    double getDeltaMass();


    /**
     * Getter for the modifications of this item.
     * @return
     */
    Map<Integer, Modification> getModifications();
}
