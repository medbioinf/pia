package de.mpc.pia.tools.obo;

import java.io.IOException;
import java.net.URL;
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
	 * Constructor for the OBOMapper. Uses the online OBO file (if accessible)
	 * or a local file.
	 * 
	 * @param pathToFile
	 */
	public OBOMapper() {
		String pathToFile;
		
		pathToFile = "http://psidev.cvs.sourceforge.net/viewvc/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo";
		
		// TODO: read properties (in the pia properties class) and get the path to the obo-file from there
		
		/*
		// properties reader
        Properties props = new Properties();

        InputStream inputStream = null;
        try {
            URL pathURL = IOUtilities.getFullPath(PrideInspectorBootstrap.class, "config/config.props");
            File file = IOUtilities.convertURLToFile(pathURL);
            // input stream of the property file
            inputStream = new FileInputStream(file);
            props.load(inputStream);
        } catch (IOException e) {
            logger.error("Failed to load config/config.props file", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.error("Failed to close InputStream while reading config.props file", e);
                }
            }
        }
        // done reader
        */
		
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
