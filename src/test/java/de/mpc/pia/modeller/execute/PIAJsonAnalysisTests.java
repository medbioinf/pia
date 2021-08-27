package de.mpc.pia.modeller.execute;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.JsonParseException;

import de.mpc.pia.JsonAnalysis;
import de.mpc.pia.PIACli;
import de.mpc.pia.modeller.PIAModellerTest;

public class PIAJsonAnalysisTests {
    
	private static File defaultJsonAnalysisFile;
	private static File piaXMLFile;


    @BeforeClass
    public static void setUpBeforeClass() {
    	defaultJsonAnalysisFile = new File(PIAModellerTest.class.getResource("/yeast-gold-015_analysis.json").getPath());
    	piaXMLFile = new File(PIAModellerTest.class.getResource("/yeast-gold-015-filtered.pia.xml").getPath());
    }
    
    @Test
    public void testReadDefaultAnalysisSettingsFromJson() throws JsonParseException {
    	JsonAnalysis jsonAnalysis = JsonAnalysis.readFromFile(defaultJsonAnalysisFile);
    	
    	assertFalse(jsonAnalysis.isConsiderModifications());
    	assertTrue(jsonAnalysis.isCreatePSMsets());
    }

    @Test
    public void testSimulateCLIAnalysis() throws JsonParseException {
    	assertTrue(PIACli.processPIAAnalysis(defaultJsonAnalysisFile.getAbsolutePath(),
    			piaXMLFile.getAbsolutePath()));
    }

}
