package de.mpc.pia.intermediate.compiler.parser;

import java.io.IOException;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.obo.dataadapter.DefaultOBOParser;
import org.obo.dataadapter.OBOParseEngine;
import org.obo.dataadapter.OBOParseException;
import org.obo.datamodel.IdentifiedObject;
import org.obo.datamodel.OBOSession;


public class OBOMapper {
	
	/** the logger for this class */
	private static final Logger logger = Logger.getLogger(OBOMapper.class);
	
	private OBOSession oboSession;
	
	public static final String cleavageAgentNameID = "MS:1001045";
	public static final String is_a_relation = "OBO_REL:is_a";
	public static final String has_regexp_relation = "has_regexp";
	public static final String has_order_relation = "OBO_REL:has_order";
	public static final String spectrumTitleID = "MS:1000796";
	public static final String peptideScoreID = "MS:1001143";
	
	/**
	 * Constructor for the OBOMapper. Uses the given file. If this is null,
	 * tries to get the PSI-MS obo file from SourceForge.
	 * 
	 * @param pathToFile
	 */
	public OBOMapper(String pathToFile) {
		if (pathToFile == null) {
			pathToFile = "http://psidev.cvs.sourceforge.net/viewvc/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo";
		}
		
		logger.info("OBO file: "+pathToFile);
		
        try {
			openSession(pathToFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OBOParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	
	private void openSession(String pathToFile) throws IOException, OBOParseException {
        DefaultOBOParser parser = new DefaultOBOParser();
        OBOParseEngine engine = new OBOParseEngine(parser);
        
        engine.setPath(pathToFile);
        
        engine.parse();
        oboSession = parser.getSession();
    }
	
	
	public IdentifiedObject getObject(String object) {
		return oboSession.getObject(object);
	}
	
	
	public Collection<IdentifiedObject> getObjects() {
		return oboSession.getObjects();
	}
}
