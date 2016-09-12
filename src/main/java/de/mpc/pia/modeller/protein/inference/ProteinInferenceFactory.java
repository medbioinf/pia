package de.mpc.pia.modeller.protein.inference;

import java.util.HashMap;
import java.util.Map;


/**
 * This class helps with the creation of inference filters.
 *
 * @author julian
 *
 */
public class ProteinInferenceFactory {

    /**
     * Here all the inference filters must be registered.
     *
     * @author julian
     *
     */
    public enum ProteinInferenceMethod {
        REPORT_OCCAMS_RAZOR {
            @Override
            public String getName() {
                return OccamsRazorInference.NAME;
            }

            @Override
            public String getShortName() {
                return OccamsRazorInference.SHORT_NAME;
            }

            @Override
            public OccamsRazorInference createInstanceOf() {
                return new OccamsRazorInference();
            }
        },

        REPORT_SPECTRUM_EXTRACTOR {
            @Override
            public String getName() {
                return SpectrumExtractorInference.NAME;
            }

            @Override
            public String getShortName() {
                return SpectrumExtractorInference.SHORT_NAME;
            }

            @Override
            public SpectrumExtractorInference createInstanceOf() {
                return new SpectrumExtractorInference();
            }
        },
        REPORT_ALL {
            @Override
            public String getName() {
                return ReportAllInference.NAME;
            }

            @Override
            public String getShortName() {
                return ReportAllInference.SHORT_NAME;
            }

            @Override
            public ReportAllInference createInstanceOf() {
                return new ReportAllInference();
            }
        },
        ;

        /**
         * Get the name of the filter.
         * @return
         */
        public abstract String getName();

        /**
         * Get the unique machine readable short name of the filter.
         * @return
         */
        public abstract String getShortName();

        /**
         * Returns a new instance of the filter.
         * @return
         */
        public abstract AbstractProteinInference createInstanceOf();
    }


    /**
     * We don't ever want to instantiate this class
     */
    private ProteinInferenceFactory() {
        throw new AssertionError();
    }


    /**
     * Returns a new instance of the inference filter given by the shortName or
     * null, if no filter with this shortName exists.
     *
     * @param shortName
     * @return
     */
    public static AbstractProteinInference createInstanceOf(String shortName) {
        for (ProteinInferenceMethod filter : ProteinInferenceMethod.values()) {
            if (filter.getShortName().equals(shortName)) {
                return filter.createInstanceOf();
            }
        }

        return null;
    }


    /**
     * Returns a map from each inference filter's shortName to the human
     * readable name of the filter.
     *
     * @return
     */
    public static Map<String, String> getAllProteinInferenceNames() {
        Map<String, String> inferenceMap = new HashMap<String, String>(ProteinInferenceMethod.values().length);

        for (ProteinInferenceMethod filter : ProteinInferenceMethod.values()) {
            inferenceMap.put(filter.getShortName(), filter.getName());
        }

        return inferenceMap;
    }


    /**
     * Returns the inference filter with the given shortName.<br/>
     * If no inference filter with this name is found, returns <code>null</code>.
     *
     * @param shortName
     * @return
     */
    public static ProteinInferenceMethod getProteinInferenceByName(String shortName) {
        for (ProteinInferenceMethod filter : ProteinInferenceMethod.values()) {
            if (filter.getShortName().equals(shortName)) {
                return filter;
            }
        }

        return null;
    }
}
