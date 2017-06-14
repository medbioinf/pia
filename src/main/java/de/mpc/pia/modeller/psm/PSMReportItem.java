package de.mpc.pia.modeller.psm;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import de.mpc.pia.intermediate.Accession;
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
public interface PSMReportItem extends PSMItem, FDRComputable, FDRScoreComputable, Rankable, Filterable, Serializable {

    /**
     * Returns a identification String for the PSM.
     * @return
     */
    String getIdentificationKey(Map<String, Boolean> psmSetSettings);


    /**
     * Returns the identification String for peptide inference.
     * @param considerModifications
     * @return
     */
    String getPeptideStringID(boolean considerModifications);


    /**
     * Getter for the delta mass given in PPM.
     * @return
     */
    double getDeltaPPM();


    /**
     * Returns a String which explains the modifications.
     * This is NOT a substitute for the real modifications, but only for
     * building the identification string and easy exporting.
     *
     * @return
     */
    String getModificationsString();


    /**
     * Getter for all the Accessions in the item
     * @return
     */
    List<Accession> getAccessions();


    /**
     * Returns the settings, which are available for identification key
     * calculation, i.e. the {@link IdentificationKeySettings} which are
     * available on all spectra in this PSM or PSM set.
     * @return
     */
    Map<String, Boolean> getAvailableIdentificationKeySettings();

    /**
     * Returns the settings, which are available for identification key
     * calculation, i.e. the {@link IdentificationKeySettings} which are
     * available on all spectra in this PSM or PSM set and are not redundant,
     * i.e. the best minimal set of settings.
     * @return
     */
    Map<String, Boolean> getNotRedundantIdentificationKeySettings();

    /**
     * Returns a nice name / header for the spectrum in this PSM or PSM set.
     * @return
     */
    String getNiceSpectrumName();


    /** Returns a representation of the PSM's or PSM set's scores */
    String getScoresString();


    /**
     * Returns the peptide in which this PSM occurs.
     * @return
     */
    Peptide getPeptide();
}
