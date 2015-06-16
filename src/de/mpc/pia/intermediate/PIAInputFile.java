package de.mpc.pia.intermediate;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.mpc.pia.tools.PIAConstants;

import uk.ac.ebi.jmzidml.model.mzidml.AnalysisCollection;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisProtocolCollection;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.Enzyme;
import uk.ac.ebi.jmzidml.model.mzidml.InputSpectra;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabase;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabaseRef;
import uk.ac.ebi.jmzidml.model.mzidml.SpectraData;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationProtocol;


/**
 * File containing input for the intermediate structure.
 * 
 * @author julian
 */
public class PIAInputFile implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/** ID of the file */
	private long ID;
	
	/** a name for easier identification */
	private String name;
	
	/** the path to the file */
	private String fileName;
	
	/** format of the file */
	private String format;
	
	/** the collection of SpectrumIdentifications (same as in mzIdentML) */
	private AnalysisCollection analysisCollection;
	
	/** the AnalysisProtocolCollection (same as in mzIdentML) */
	private AnalysisProtocolCollection analysisProtocolCollection;
	
	
	
	public PIAInputFile(long id, String name, String filename, String format) {
		this.ID = id;
		this.name = name;
		this.fileName = filename;
		this.format = format;
		this.analysisCollection = new AnalysisCollection();
		this.analysisProtocolCollection = new AnalysisProtocolCollection();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if ( !(obj instanceof PIAInputFile) ) {
			return false;
		}
		
		PIAInputFile objFile = (PIAInputFile)obj;
	    if ((objFile.ID == this.ID) &&
	    		objFile.fileName.equals(fileName) &&
	    		objFile.format.equals(format)) {
	    	return true;
	    } else {
	    	return false;
	    }
	}
	
	
	@Override
	public int hashCode() {
		int hash = 0;
		
		hash += (new Long(ID)).hashCode();
		hash += fileName.hashCode();
		
		hash += format.hashCode();
		
		return hash;
	}
	
	
	/**
	 * Getter for the ID.
	 * @return
	 */
	public Long getID() {
		return ID;
	}
	
	
	/**
	 * Getter for the name.
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	
	/**
	 * Getter for the filename.
	 * @return
	 */
	public String getFileName() {
		return fileName;
	}
	
	
	/**
	 * Getter for the file format of the file.
	 * @return
	 */
	public String getFormat() {
		return format;
	}
	
	
	/**
	 * Adds the given {@link SpectrumIdentification} to the analysisCollection.
	 */
	public String addSpectrumIdentification(SpectrumIdentification si) {
		String strID = PIAConstants.spectrum_identification_prefix + this.ID +
				"_" + (analysisCollection.getSpectrumIdentification().size() + 1L);
		si.setId(strID);
		// we don't have any SpectrumIdentificationList in the PIA file
		si.setSpectrumIdentificationList(null);
		analysisCollection.getSpectrumIdentification().add(si);
		return strID;
	}
	
	
	/**
	 * Gets the {@link SpectrumIdentification} with the given ID or null, if
	 * none is found.
	 * 
	 * @param id
	 * @return
	 */
	public SpectrumIdentification getSpectrumIdentification(String id) {
		for (SpectrumIdentification si
				: analysisCollection.getSpectrumIdentification()) {
			if (si.getId().equals(id)) {
				return si; 
			}
		}
		
		return null;
	}
	
	
	/**
	 * Getter for the analysisCollection.
	 * @return
	 */
	public AnalysisCollection getAnalysisCollection() {
		return analysisCollection;
	}
	
	/**
	 * Adds the given {@link SpectrumIdentificationProtocol} to the analysisProtocolCollection.
	 * @param sip
	 */
	public String addSpectrumIdentificationProtocol(SpectrumIdentificationProtocol sip) {
		String strID = PIAConstants.identification_protocol_prefix + this.ID +
				"_" + (analysisProtocolCollection.getSpectrumIdentificationProtocol().size() + 1L);
		String ref = sip.getId();
		sip.setId(strID);
		analysisProtocolCollection.getSpectrumIdentificationProtocol().add(sip);
		
		// go through the analysisCollection an re-reference the SpectrumIdentificationProtocol
		for (SpectrumIdentification spectrumId
				: analysisCollection.getSpectrumIdentification()) {
			if (spectrumId.getSpectrumIdentificationProtocolRef().equals(ref)) {
				spectrumId.setSpectrumIdentificationProtocol(sip);
			}
		}
		
		// uniquify the enzymes' IDs in the SpectrumIdentificationProtocol
		int idx = 1;
		if (sip.getEnzymes() != null) {
			for (Enzyme enzyme : sip.getEnzymes().getEnzyme()) {
				enzyme.setId(PIAConstants.enzyme_prefix + this.ID + "_" + idx);
				idx++;
			}
		}
		
		return strID;
	}
	
	
	/**
	 * 
	 */
	public void updateReferences(Map<String, SpectraData> spectraDataRefs,
			Map<String, SearchDatabase> searchDBRefs,
			Map<String, AnalysisSoftware> analysisSoftwareRefs) {
		
		for (SpectrumIdentification si
				: analysisCollection.getSpectrumIdentification()) {
			// update the SpectraDataRefs
			Set<SpectraData> newSpectraData = new HashSet<SpectraData>();
			for (InputSpectra spectra : si.getInputSpectra()) {
				newSpectraData.add(
						spectraDataRefs.get(spectra.getSpectraDataRef()) );
			}
			
			// clear the old InputSpectra
			si.getInputSpectra().clear();
			// and add the new spectra
			for (SpectraData spectra : newSpectraData) {
				InputSpectra inputSpectra = new InputSpectra();
				inputSpectra.setSpectraData(spectra);
				si.getInputSpectra().add(inputSpectra);
			}
			
			
			// update the SearchDatabaseRefs
			Set<SearchDatabase> newSearchDBs = new HashSet<SearchDatabase>();
			for (SearchDatabaseRef searchDBRef : si.getSearchDatabaseRef()) {
				newSearchDBs.add(
						searchDBRefs.get(searchDBRef.getSearchDatabaseRef()) );
			}
			// clear old searchDBRefs
			si.getSearchDatabaseRef().clear();
			// add the new searchDBs
			for (SearchDatabase sDB : newSearchDBs) {
				SearchDatabaseRef sDBRef = new SearchDatabaseRef();
				sDBRef.setSearchDatabase(sDB);
				si.getSearchDatabaseRef().add(sDBRef);
			}
		}
		
		for (SpectrumIdentificationProtocol protocol :
			// update the AnalysisSoftware in the protocols
			analysisProtocolCollection.getSpectrumIdentificationProtocol()) {
			AnalysisSoftware software =
					analysisSoftwareRefs.get(protocol.getAnalysisSoftwareRef());
			protocol.setAnalysisSoftware(software);
		}
	}
	
	
	/**
	 * Getter for the analysisProtocolCollection.
	 * @return
	 */
	public AnalysisProtocolCollection getAnalysisProtocolCollection() {
		return analysisProtocolCollection;
	}
}
