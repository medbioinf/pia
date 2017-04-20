package de.mpc.pia.visualization.spectra;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.modeller.psm.PSMItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import uk.ac.ebi.pride.utilities.data.controller.DataAccessController;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.MzDataControllerImpl;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.MzMLControllerImpl;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.MzXmlControllerImpl;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.PeakControllerImpl;
import uk.ac.ebi.pride.utilities.data.core.Spectrum;

/**
 * This class maps the PSM IDs from PIA to spectra in any spectra file, which
 * can be parsed by the ms-data-core-api. The spectra file is parsed and the
 * spectra can be accessed by the PSM ID after the instantiation of this class.
 *
 * @author julian
 *
 */
public class PiaPsmToSpectrum<P extends PSMItem> {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(PiaPsmToSpectrum.class);

    /** the data access controller for the file containing the MZ data */
    private DataAccessController daController;

    /** mapping from the psm IDs to the spectrum IDs */
    private Map<Long, Comparable> psmToSpectrum;

    /** number of PSMs, which did not match a spectrum */
    private int nrNullMatches;


    /** the allowed delta in the spectrum matching */
    public static final double MATCHING_TOLERANCE = 0.001;

    /** the m/z width for spectrum binning */
    private static final float BIN_WIDTH = 0.05f;


    /**
     * Creates a matcher for the given spectra file and the PSMs.
     *
     * @param spectraFile
     * @param psms
     */
    public PiaPsmToSpectrum(File spectraFile, Collection<P> psms) {
        daController = null;

        if (!initiateController(spectraFile)) {
            throw new AssertionError("Spectrum file could not be read.");
        }

        if (!psms.isEmpty()) {
            P psm = psms.iterator().next();

            if (!((psm instanceof PeptideSpectrumMatch) || (psm instanceof ReportPSM))) {
                throw new AssertionError("Not supported PSM type " + psm.getClass().getCanonicalName()
                        + ". The PSMs must be either " + PeptideSpectrumMatch.class.getCanonicalName()
                        + " or " + ReportPSM.class.getCanonicalName());
            }
        }

        matchPSMsToSpectra(psms);
    }


    /**
     * Finish all up and close the controller.
     */
    public void close() {
        daController.close();
    }


    /**
     * initiates the {@link DataAccessController}
     *
     * @param spectraFile
     * @return true, if it was initiated correctly, otherwise false
     */
    private boolean initiateController(File spectraFile) {
        if (MzMLControllerImpl.isValidFormat(spectraFile)) {
            daController = new MzMLControllerImpl(spectraFile);
        } else if (MzXmlControllerImpl.isValidFormat(spectraFile)) {
            daController = new MzXmlControllerImpl(spectraFile);
        } else if (MzDataControllerImpl.isValidFormat(spectraFile)) {
            daController = new MzDataControllerImpl(spectraFile);
        } else if (PeakControllerImpl.isValidFormat(spectraFile) != null) {
            daController = new PeakControllerImpl(spectraFile);
        }

        return !(daController == null);
    }


    /**
     * tries to match all PSMs to the spectra in the file handled by the
     * {@link DataAccessController}
     *
     * @param psms
     */
    private void matchPSMsToSpectra(Collection<P> psms) {
        LOGGER.info("Indexing PSMs to spectra");

        Map<Integer, List<Comparable>> mzToSpectraBins = preBinSpectra();

        psmToSpectrum = new HashMap<>();

        Iterator<P> psmIter = psms.iterator();
        int count = 0;
        nrNullMatches = 0;
        while (psmIter.hasNext()) {
            P psm = psmIter.next();

            Integer mzBin = (int)(psm.getMassToCharge() / BIN_WIDTH);

            List<Comparable> binsSpectraIDs = new ArrayList<>(mzToSpectraBins.get(mzBin));
            if (mzToSpectraBins.containsKey(mzBin-1)) {
                binsSpectraIDs.addAll(mzToSpectraBins.get(mzBin-1));
            }
            if (mzToSpectraBins.containsKey(mzBin+1)) {
                binsSpectraIDs.addAll(mzToSpectraBins.get(mzBin+1));
            }

            Comparable specID = findMatchingSpectrumId(psm, binsSpectraIDs);

            if (psm instanceof PeptideSpectrumMatch) {
                psmToSpectrum.put(((PeptideSpectrumMatch) psm).getID(), specID);
            } else if (psm instanceof ReportPSM) {
                psmToSpectrum.put(((ReportPSM) psm).getId(), specID);
            } else {
                LOGGER.error("not supported PSM type");
            }

            if (specID == null) {
                nrNullMatches++;
            }

            count++;
            if (count % 1000 == 0) {
                LOGGER.debug(count + " / " + psms.size() + " PSMs");
            }
        }

        LOGGER.info("done " + psmToSpectrum.size() + " (nullmatches=" + nrNullMatches + ")");
    }


    /**
     * Bins the spectra according to their m/z values in bins
     *
     * @return
     */
    private Map<Integer, List<Comparable>> preBinSpectra() {
        LOGGER.debug("pre binning the spectra");
        Map<Integer, List<Comparable>> mzToSpectraBins = new HashMap<>();

        for (Comparable specID : daController.getSpectrumIds()) {
            int mzBin = (int) (daController.getSpectrumPrecursorMz(specID) / BIN_WIDTH);
            List<Comparable> specIDs = mzToSpectraBins.computeIfAbsent(mzBin, k -> new ArrayList<>());

            specIDs.add(specID);
        }

        LOGGER.debug("binned " + daController.getNumberOfSpectra() + " spectra into " + mzToSpectraBins.size() + " bins");
        return mzToSpectraBins;
    }


    /**
     * Finds a spectrumID for a spectrum in the controller, that fits into the
     * MZ range of the given PSM
     *
     * @return
     */
    private Comparable findMatchingSpectrumId(PSMItem psm, Collection<Comparable> spectraIDs) {
        if (spectraIDs == null) {
            return null;
        }

        // get the allowed m/z window
        double minMZ = psm.getMassToCharge() - MATCHING_TOLERANCE;
        double maxMZ = psm.getMassToCharge() + MATCHING_TOLERANCE;
        int psmCharge = psm.getCharge();
        double psmMZ = psm.getMassToCharge();

        Comparable matchingSpecID = null;
        Iterator<Comparable> specIter = spectraIDs.iterator();
        double deltaMZ = Double.POSITIVE_INFINITY;

        while (specIter.hasNext()) {
            Comparable specID = specIter.next();
            Double precMZ = getMatchingPrecursorMZ(specID, psmCharge, minMZ, maxMZ);

            if (precMZ != null) {
                double delta = Math.abs(precMZ - psmMZ);
                if (delta < deltaMZ) {
                    matchingSpecID = specID;
                    deltaMZ = delta;
                }
            }
        }

        return matchingSpecID;
    }


    /**
     * Return the precursor m/z of the spectrum in the controller given by the ID,
     * but only, if it has the correct charge and falls in the m/z region
     *
     * @param specID the spectrum id or null, if it does not match the charge or m/z region
     * @param psmCharge
     * @param minMZ
     * @param maxMZ
     * @return
     */
    private Double getMatchingPrecursorMZ(Comparable specID, int psmCharge, double minMZ, double maxMZ) {
        Integer precCharge = daController.getSpectrumPrecursorCharge(specID);
        if ((precCharge == null) || (precCharge == psmCharge)) {
            Double precMZ = daController.getSpectrumPrecursorMz(specID);

            if ((precMZ >= minMZ) && (precMZ <= maxMZ)) {
                return precMZ;
            }
        }

        return null;
    }


    /**
     * returns the number of PSMs, which could not be matched against a spectrum
     *
     * @return
     */
    public int getNrNullMatches() {
        return nrNullMatches;
    }


    /**
     * returns the matched spectrum for the given PSM
     *
     * @param psm
     * @return the matching spectrum or null, if none matches
     */
    public Spectrum getSpectrumForPSM(P psm) {
        Comparable specID = null;

        if (psm instanceof PeptideSpectrumMatch) {
            specID = psmToSpectrum.get(((PeptideSpectrumMatch) psm).getID());
        } else if (psm instanceof ReportPSM) {
            specID = psmToSpectrum.get(((ReportPSM) psm).getId());
        } else if (psm == null) {
            LOGGER.error("psm is null");
        }

        if (specID != null) {
            return daController.getSpectrumById(specID);
        } else {
            return null;
        }
    }
}
