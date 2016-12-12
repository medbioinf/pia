package de.mpc.pia.modeller;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class PSMModellerTest {

    private static File piaFile;
    private static PIAModeller piaModeller;
    private static PSMModeller psmModeller;

    @Before
    public void setUp() {
        piaFile = new File(PIAModellerTest.class.getResource("/55merge_mascot_tandem.pia.xml").getPath());
        piaModeller = new PIAModeller(piaFile.getAbsolutePath());
        psmModeller = piaModeller.getPSMModeller();
    }


    @Test
    public void testGetAndSetPSMSetSettings() {
        Map<String, Boolean> psmSetSettings = new HashMap<>();
        psmSetSettings.put(IdentificationKeySettings.SEQUENCE.toString(), true);
        psmSetSettings.put(IdentificationKeySettings.SOURCE_ID.toString(), true);
        psmSetSettings.put(IdentificationKeySettings.MODIFICATIONS.toString(), true);
        psmSetSettings.put(IdentificationKeySettings.CHARGE.toString(), true);
        psmModeller.setPSMSetSettings(psmSetSettings);

        psmSetSettings = new HashMap<>();
        psmSetSettings.put(IdentificationKeySettings.FILE_ID.toString(), true);
        psmSetSettings.put(IdentificationKeySettings.SOURCE_ID.toString(), false);


        Map<String, Boolean> oldSettings = psmModeller.setPSMSetSettings(psmSetSettings);
        psmSetSettings = psmModeller.getPSMSetSettings();

        assertTrue(oldSettings.get(IdentificationKeySettings.SEQUENCE.toString()));
        assertTrue(oldSettings.get(IdentificationKeySettings.SOURCE_ID.toString()));
        assertTrue(oldSettings.get(IdentificationKeySettings.MODIFICATIONS.toString()));
        assertTrue(oldSettings.get(IdentificationKeySettings.CHARGE.toString()));

        assertFalse(psmSetSettings.get(IdentificationKeySettings.SOURCE_ID.toString()));
        assertTrue(psmSetSettings.get(IdentificationKeySettings.FILE_ID.toString()));
    }
}
