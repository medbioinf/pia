package de.mpc.pia.intermediate;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    private static final long serialVersionUID = -5611387583481669510L;


    /** ID of the file */
    private long id;

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
        this.id = id;
        this.name = name;
        this.fileName = filename;
        this.format = format;
        this.analysisCollection = new AnalysisCollection();
        this.analysisProtocolCollection = new AnalysisProtocolCollection();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PIAInputFile that = (PIAInputFile) o;

        if (id != that.id) return false;
        if (!Objects.equals(fileName, that.fileName)) return false;
        return Objects.equals(format, that.format);
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
        result = 31 * result + (format != null ? format.hashCode() : 0);
        return result;
    }

    /**
     * Getter for the ID.
     * @return
     */
    public Long getID() {
        return id;
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
        String strID = PIAConstants.spectrum_identification_prefix + this.id +
                '_' + (analysisCollection.getSpectrumIdentification().size() + 1L);
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
        String strID = PIAConstants.identification_protocol_prefix + this.id +
                '_' + (analysisProtocolCollection.getSpectrumIdentificationProtocol().size() + 1L);
        String ref = sip.getId();
        sip.setId(strID);
        analysisProtocolCollection.getSpectrumIdentificationProtocol().add(sip);

        // go through the analysisCollection an re-reference the SpectrumIdentificationProtocol
        analysisCollection.getSpectrumIdentification().stream().filter(spectrumId -> spectrumId.getSpectrumIdentificationProtocolRef().equals(ref)).forEach(spectrumId -> spectrumId.setSpectrumIdentificationProtocol(sip));

        // uniquify the enzymes' IDs in the SpectrumIdentificationProtocol
        int idx = 1;
        if (sip.getEnzymes() != null) {
            for (Enzyme enzyme : sip.getEnzymes().getEnzyme()) {
                enzyme.setId(PIAConstants.enzyme_prefix + this.id + '_' + idx);
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
            Set<SpectraData> newSpectraData = si.getInputSpectra().stream().map(spectra -> spectraDataRefs.get(spectra.getSpectraDataRef())).collect(Collectors.toSet());

            // clear the old InputSpectra
            si.getInputSpectra().clear();
            // and add the new spectra
            for (SpectraData spectra : newSpectraData) {
                InputSpectra inputSpectra = new InputSpectra();
                inputSpectra.setSpectraData(spectra);
                si.getInputSpectra().add(inputSpectra);
            }


            // update the SearchDatabaseRefs
            Set<SearchDatabase> newSearchDBs = si.getSearchDatabaseRef().stream().map(searchDBRef -> searchDBRefs.get(searchDBRef.getSearchDatabaseRef())).collect(Collectors.toSet());
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
