package de.mpc.pia.modeller;

import java.util.HashMap;
import java.util.Map;

/**
 * This enumeration lists the possible values from a Spectrum, PSM or Peptide,
 * which can be used to create an identification key.
 *
 * @author julian
 *
 */
public enum IdentificationKeySettings {

    MASSTOCHARGE {
        @Override
        public String getShortDescription() {
            return "m/z";
        }

        @Override
        public Boolean getDefault() {
            return true;
        }
    },

    RETENTION_TIME {
        @Override
        public String getShortDescription() {
            return "retention time";
        }

        @Override
        public Boolean getDefault() {
            return true;
        }
    },

    SOURCE_ID {
        @Override
        public String getShortDescription() {
            return "source ID";
        }

        @Override
        public Boolean getDefault() {
            return false;
        }
    },

    SPECTRUM_TITLE {
        @Override
        public String getShortDescription() {
            return "spectrum title";
        }

        @Override
        public Boolean getDefault() {
            return false;
        }
    },

    SEQUENCE {
        @Override
        public String getShortDescription() {
            return "sequence";
        }

        @Override
        public Boolean getDefault() {
            return true;
        }
    },

    MODIFICATIONS {
        @Override
        public String getShortDescription() {
            return "modifications";
        }

        @Override
        public Boolean getDefault() {
            return true;
        }
    },

    CHARGE {
        @Override
        public String getShortDescription() {
            return "charge";
        }

        @Override
        public Boolean getDefault() {
            return true;
        }
    },

    /**
     * Only use FILE_ID, if you not intend to merge results from different search engines
     */
    FILE_ID {
        @Override
        public String getShortDescription() {
            return "file_id";
        }

        @Override
        public Boolean getDefault() {
            return false;
        }
    },
    ;


    /**
     * Returns the description as shown in the GUI
     * @return
     */
    public abstract String getShortDescription();


    /**
     * Returns the default value
     * @return
     */
    public abstract Boolean getDefault();


    /**
     * Returns the {@link IdentificationKeySettings} given by the name or null, if
     * none exists with the name.
     *
     * @param name
     * @return
     */
    public static IdentificationKeySettings getByName(String name) {
        for (IdentificationKeySettings setting : values()) {
            if (setting.name().equals(name)) {
                return setting;
            }
        }

        return null;
    }


    /**
     * Remove redundant psmSetSettings and return a map containing only the more
     * failure tolerant ones.
     */
    public static Map<String, Boolean> noRedundantSettings(Map<String, Boolean> psmSetSettings) {
        Map<String, Boolean> settings =         new HashMap<String, Boolean>(psmSetSettings);

        if (settings.containsKey(IdentificationKeySettings.SOURCE_ID.toString()) &&
                settings.get(IdentificationKeySettings.SOURCE_ID.toString())) {
            // with SOURCE_ID given, m/z, RT and spectrumTitle are unnecessary (or even failure bearing)
            // these are removed
            settings.remove(IdentificationKeySettings.MASSTOCHARGE.toString());
            settings.remove(IdentificationKeySettings.RETENTION_TIME.toString());
            settings.remove(IdentificationKeySettings.SPECTRUM_TITLE.toString());
        }

        return settings;
    }

}
