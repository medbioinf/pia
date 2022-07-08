package de.mpc.pia.intermediate;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;


public class ModificationTest {

    private Modification oxidationM = null;
    private Modification anotherOxidationM = null;
    private Modification carbamidomethylC = null;

    @Before
    public void setUp() {
        oxidationM = new Modification('M', 15.994915, "Oxidation", "UNIMOD:35");
        anotherOxidationM = new Modification('M', 15.994915, "Oxidation", "UNIMOD:35");
        carbamidomethylC = new Modification('C', 57.021464, "Carbamidomethyl", "UNIMOD:4");
    }

    @Test
    public void testGetters() {
        assertEquals(Character.valueOf('M'), oxidationM.getResidue());
        assertEquals(Double.valueOf(15.994915), oxidationM.getMass());
        assertEquals("15.9949", oxidationM.getMassString());
        assertEquals("Oxidation", oxidationM.getDescription());
        assertEquals("UNIMOD:35", oxidationM.getAccession());
    }

    @Test
    public void testHashCode() {
        assertEquals("Hashes should be equal", oxidationM.hashCode(), anotherOxidationM.hashCode());
        assertNotEquals("Hashes should not equal", oxidationM.hashCode(), carbamidomethylC.hashCode());
    }

    @Test
    public void testEqualsObject() {
        assertEquals("Both oxidations should be equal", oxidationM, anotherOxidationM);
        assertEquals("Both carbamidomethyl should be", carbamidomethylC,
                new Modification('C', 57.021464, "Carbamidomethyl", "UNIMOD:4"));

        assertNotEquals("Half defined carbamidomethyl should not be equal to fully defined",
                carbamidomethylC, new Modification('C', 57.0, null, "UNIMOD:4"));

        assertNotEquals("These should not be equal", oxidationM, carbamidomethylC);
        assertNotEquals("These should not be equal", anotherOxidationM, carbamidomethylC);
    }
}
