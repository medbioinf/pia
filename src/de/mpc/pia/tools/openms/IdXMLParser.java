package de.mpc.pia.tools.openms;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;

import de.mpc.pia.tools.openms.jaxb.IdXML;
import de.mpc.pia.tools.openms.jaxb.IdentificationRun;
import de.mpc.pia.tools.openms.jaxb.SearchParameters;

/**
 * This is a very basic parser for IdXML.
 * <p>
 * It is mainly build using JAXB and the provided schema file of OpenMS's 
 * IdXML format.
 * 
 * @author julian
 *
 */
public class IdXMLParser {
	
	/** the {@link IdentificationRun} from the IdXML file */
	private List<IdentificationRun> identificationRuns;
	
	/** the {@link SearchParameters} from the IdXML file*/
	private List<SearchParameters> searchParameters;
	
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(IdXMLParser.class);
	
	
	/**
	 * Basic constructor, reads in the IdXML file
	 * 
	 * @param idXMLFileName
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public IdXMLParser(String idXMLFileName)
			throws JAXBException, FileNotFoundException {
		JAXBContext context = JAXBContext.newInstance(IdXML.class);
		Unmarshaller um = context.createUnmarshaller();
		
		IdXML idXML = (IdXML)um.unmarshal(new FileReader(idXMLFileName));
		
		if (idXML.getVersion() < 1.2) {
			logger.error("Reading in probable incompatible idXML version " +
					idXML.getVersion());
		}
		
		searchParameters =  idXML.getSearchParameters();
		identificationRuns = idXML.getIdentificationRun();
    }
	
	
	/**
	 * Returns the List of {@link SearchParameters}
	 * @return
	 */
	public List<SearchParameters> getSearchParameters() {
		return searchParameters;
	}
	
	
	/**
	 * Returns the List of {@link IdentificationRun}s
	 * @return
	 */
	public List<IdentificationRun> getIdentificationRuns() {
		return identificationRuns;
	}
}