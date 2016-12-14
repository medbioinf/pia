package de.mpc.pia.visualization.spectra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.modeller.psm.PSMItem;
import uk.ac.ebi.pride.toolsuite.mzgraph.SpectrumBrowser;
import uk.ac.ebi.pride.toolsuite.mzgraph.gui.data.ExperimentalFragmentedIonsTableModel;
import uk.ac.ebi.pride.utilities.data.core.Spectrum;
import uk.ac.ebi.pride.utilities.iongen.impl.DefaultPrecursorIon;
import uk.ac.ebi.pride.utilities.iongen.model.PrecursorIon;
import uk.ac.ebi.pride.utilities.mol.PTModification;
import uk.ac.ebi.pride.utilities.mol.Peptide;
import uk.ac.ebi.pride.utilities.mol.ProductIonPair;

/**
 * This class is used to create a {@link SpectrumBrowser}, which shows a
 * spectrum with automatically created annotations for a given peptide.
 *
 * @author julian
 *
 */
public class SpectrumVisualizer {

    /**
     * private constructor
     */
    private SpectrumVisualizer() {
        // do not instantiate this class
    }


    /**
     * Create a {@link SpectrumBrowser} for the given spectrum and PSM. Only the
     * selected ProductIonPair (B/Y, A/Z, X/C) will be annotated.
     *
     * @param spectrum
     * @param psm
     * @param productIonPair
     * @return
     */
    public static SpectrumBrowser createSpectrumBrowser(Spectrum spectrum, PSMItem psm,
            ProductIonPair productIonPair) {
        return createSpectrumBrowser(spectrum.getMassIntensityMap(), psm.getCharge(),
                psm.getSequence(), psm.getModifications(), productIonPair);
    }


    /**
     * Create a {@link SpectrumBrowser} for the given m/z-intensity map, charge,
     * peptide sequence and modifications. Only the selected ProductIonPair
     * (B/Y, A/Z, X/C) will be annotated.
     *
     * @param massIntensityArr
     * @param psmCharge
     * @param sequence
     * @param modifications
     * @param productIonPair
     * @return
     */
    public static SpectrumBrowser createSpectrumBrowser(double[][] massIntensityArr, int psmCharge,
            String sequence, Map<Integer, Modification> modifications, ProductIonPair productIonPair) {
        SpectrumBrowser spectrumBrowser = new SpectrumBrowser();

        double[] mzArr = new double[massIntensityArr.length];
        double[] intentArr = new double[massIntensityArr.length];
        for (int i=0; i < massIntensityArr.length; i++) {
            mzArr[i] = massIntensityArr[i][0];
            intentArr[i] = massIntensityArr[i][1];
        }

        // Set the spectrum peak list
        spectrumBrowser.setPeaks(mzArr, intentArr);

        // convert modifications
        Map<Integer, List<PTModification>> pepsMods = generatePrideModifications(modifications);

        // set the peptide
        Peptide myPeptide = generatePeptide(sequence, pepsMods);

        // this is the maximum fragment charge (which does not work if it is bigger than 2)
        int charge = (psmCharge > 2) ? 2 : psmCharge;

        // create automatically annotations
        PrecursorIon precursorIon = new DefaultPrecursorIon(myPeptide, charge);
        ExperimentalFragmentedIonsTableModel myTableModel =
                new ExperimentalFragmentedIonsTableModel(precursorIon, productIonPair);

        myTableModel.setPeaks(mzArr, intentArr);
        myTableModel.setCalculate(true);
        myTableModel.setShowWaterLoss(true);
        myTableModel.setShowAmmoniaLoss(true);

        spectrumBrowser.addFragmentIons(myTableModel.getAutoAnnotations());

        spectrumBrowser.setAminoAcidAnnotationParameters(sequence.length(), pepsMods);

        return spectrumBrowser;
    }


    /**
     * Create a {@link Peptide} from the given sequence and modifications.
     *
     * @param sequence
     * @param modifications
     * @return
     */
    public static Peptide generatePeptide(String sequence, Map<Integer, List<PTModification>> modifications) {
        Peptide peptide = new Peptide(sequence);

        for (Map.Entry<Integer, List<PTModification>> modIt : modifications.entrySet()) {
            for (PTModification mod : modIt.getValue()) {
                peptide.addModification(modIt.getKey(), mod);
            }
        }

        return peptide;
    }


    /**
     * Creates a PRIDE {@link PTModification}s map from a PIA
     * {@link Modification} map.
     *
     * @param modifications
     * @return
     */
    public static Map<Integer, List<PTModification>> generatePrideModifications(Map<Integer, Modification> modifications) {
        Map<Integer, List<PTModification>> pepsMods = new HashMap<>();

        for (Map.Entry<Integer, Modification> modIt : modifications.entrySet()) {
            List<Double> monoMassDeltas = new ArrayList<>();
            monoMassDeltas.add(modIt.getValue().getMass());
            PTModification mod = new PTModification(null, null, null, monoMassDeltas, null);

            List<PTModification> modList = new ArrayList<>(1);
            modList.add(mod);

            pepsMods.put(modIt.getKey(), modList);
        }

        return pepsMods;
    }
}
