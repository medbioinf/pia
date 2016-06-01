package de.mpc.pia.modeller.psm;

import java.util.List;
import java.util.Map;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.modeller.IdentificationKeySettings;
import de.mpc.pia.modeller.report.filter.Filterable;
import de.mpc.pia.modeller.score.FDRComputable;
import de.mpc.pia.modeller.score.FDRScoreComputable;
import de.mpc.pia.modeller.score.comparator.Rankable;

/**
 * Interface to specify report items shown by the PSM Viewer.
 *
 * @author julian
 *
 */
public interface PSMReportItem extends FDRComputable, FDRScoreComputable,
Rankable, Filterable {

    /**
     * Returns a identification String for the PSM.
     * @return
     */
    public String getIdentificationKey(Map<String, Boolean> psmSetSettings);


    /**
     * Returns the identification String for peptide inference.
     * @param considerModifications
     * @return
     */
    public String getPeptideStringID(boolean considerModifications);


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
     * Getter for the retention time.
     *
     * @return
     */
    public Double getRetentionTime();


    /**
     * Getter for the missed cleavages
     * @return
     */
    public int getMissedCleavages();


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
     * Getter for the delta mass given in PPM.
     * @return
     */
    public double getDeltaPPM();


    /**
     * Getter for the modifications of this item.
     * @return
     */
    public Map<Integer, Modification> getModifications();


    /**
     * Returns a String which explains the modifications.
     * This is NOT a substitute for the real modifications, but only for
     * building the identification string and easy exporting.
     *
     * @return
     */
    public String getModificationsString();


    /**
     * Getter for all the Accessions in the item
     * @return
     */
    public List<Accession> getAccessions();


    /**
     * Returns the settings, which are available for identification key
     * calculation, i.e. the {@link IdentificationKeySettings} which are
     * available on all spectra in this PSM or PSM set.
     * @return
     */
    public Map<String, Boolean> getAvailableIdentificationKeySettings();

    /**
     * Returns the settings, which are available for identification key
     * calculation, i.e. the {@link IdentificationKeySettings} which are
     * available on all spectra in this PSM or PSM set and are not redundant,
     * i.e. the best minimal set of settings.
     * @return
     */
    public Map<String, Boolean> getNotRedundantIdentificationKeySettings();

    /**
     * Returns a nice name / header for the spectrum in this PSM or PSM set.
     * @return
     */
    public String getNiceSpectrumName();


    /** Returns a representation of the PSM's or PSM set's scores */
    public String getScoresString();


    /**
     * Returns the peptide in which this PSM occurs.
     * @return
     */
    public Peptide getPeptide();
}
