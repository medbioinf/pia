package de.mpc.pia.modeller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpc.pia.intermediate.Group;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.intermediate.xmlhandler.PIAIntermediateJAXBHandler;
import de.mpc.pia.modeller.exporter.CSVExporter;
import de.mpc.pia.modeller.exporter.MzIdentMLExporter;
import de.mpc.pia.modeller.exporter.MzTabExporter;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabase;
import uk.ac.ebi.jmzidml.model.mzidml.SpectraData;


/**
 * The main modeller class for PIA.
 *
 * @author julianu
 *
 */
public class PIAModeller implements Serializable {

    private static final long serialVersionUID = -1215457917976166137L;


    /**
     * the modeller for everything PSM related
     * @serial
     */
    private PSMModeller psmModeller;

    /**
     * the modeller for everything peptide related
     * @serial
     */
    private PeptideModeller peptideModeller;

    /**
     * the modeller for everything protein related
     * @serial
     */
    private ProteinModeller proteinModeller;


    /**
     * name of the used file
     * @serial
     */
    private String fileName;

    /**
     * handler for the intermediate file
     * @serial
     */
    private PIAIntermediateJAXBHandler intermediateHandler;


    /** logger for this class */
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Very basic constructor.
     */
    public PIAModeller() {
        psmModeller = null;
        peptideModeller = null;
        proteinModeller = null;

        fileName = null;
        intermediateHandler = null;
    }


    /**
     * Basic constructor, creates a model for the given file.
     *
     * @param fileName
     */
    public PIAModeller(String fileName) {
        this();

        if (fileName == null) {
            throw new IllegalArgumentException("No file name given.");
        }

        if (!loadFileName(fileName, null)) {
            throw new IllegalArgumentException("Error loading PIA XML file.");
        }
    }


    /**
     * Getter for the {@link PSMModeller} of this modeller.
     * @return
     */
    public PSMModeller getPSMModeller() {
        return this.psmModeller;
    }


    /**
     * Getter for the {@link PeptideModeller} of this modeller.
     * @return
     */
    public PeptideModeller getPeptideModeller() {
        return this.peptideModeller;
    }


    /**
     * Getter for the {@link ProteinModeller} of this modeller.
     * @return
     */
    public ProteinModeller getProteinModeller() {
        return this.proteinModeller;
    }


    /**
     * Setter for fileName, notifies progress on the given object.
     * Also initializes the model, if the fileName changed.
     *
     * @param filename
     * @param progress the first item in the array holds the current progress
     *
     * @return true, if a new file was loaded
     */
    public boolean loadFileName(String filename, Long[] progress) {
        LOGGER.info("start loading file {}", filename);

        boolean loadOk = false;

        if ((filename != null) && !filename.trim().isEmpty()) {
            this.psmModeller = null;
            this.peptideModeller = null;
            this.proteinModeller = null;

            this.fileName = filename;
            this.intermediateHandler = null;

            try {
                parseIntermediate(progress);
                loadOk = true;
            } catch (Exception e) {
                LOGGER.error("Error while loading PIA XML file", e);
                loadOk = false;
            }
        }

        return loadOk;
    }


    /**
     * Getter for fileName
     * @return
     */
    public String getFileName() {
        return fileName;
    }


    /**
     * Returns the project name.
     * @return
     */
    public String getProjectName() {
        if (intermediateHandler != null) {
            return intermediateHandler.getProjectName();
        }
        return null;
    }


    /**
     * Gets whether PSM sets should be created across files
     * @return
     */
    public Boolean getCreatePSMSets() {
        return psmModeller.getCreatePSMSets();
    }


    /**
     * Sets whether PSM sets should be used across files
     * @param createPSMSets
     */
    public void setCreatePSMSets(Boolean createPSMSets) {
        psmModeller.applyGeneralSettings(createPSMSets);
    }


    /**
     * Getter for the {@link IdentificationKeySettings}.
     * @return
     */
    public Map<String, Boolean> getPSMSetSettings() {
        return psmModeller.getPSMSetSettings();
    }


    /**
     * Returns a Map from the {@link IdentificationKeySettings} String representation to a
     * Set of file IDs, where a warning occurred.
     *
     * @return
     */
    public Map<String, Set<Long>> getPSMSetSettingsWarnings() {
        return (intermediateHandler != null) ?
                intermediateHandler.getPSMSetSettingsWarnings() :
                new HashMap<>(1);
    }


    /**
     * Getter for considerModifications
     * @return
     */
    public Boolean getConsiderModifications() {
        return peptideModeller.getConsiderModifications();
    }


    /**
     * Setter for considerModifications
     * @return
     */
    public void setConsiderModifications(boolean considerMods) {
        peptideModeller.setConsiderModifications(considerMods);
    }


    /**
     * Parses in the intermediate structure from the given file.<br/>
     *
     * @param progressMonitor stores the current progress of the parsing, gets increased by 100 by this method
     *
     * @throws IOException
     */
    private void parseIntermediate(Long[] progressMonitor)
            throws IOException {
        LOGGER.info("loadIntermediate started...");

        Long[] progress;
        if ((progressMonitor == null) || (progressMonitor.length < 1) || (progressMonitor[0] == null)) {
            LOGGER.warn("No progress array given, creating one. "
                    + "But no external supervision will be possible.");
            progress = new Long[1];
        } else {
            progress = progressMonitor;
        }

        progress[0] = 0L;

        if (fileName == null) {
            LOGGER.error("no file given!");
            return;
        }

        LOGGER.info("Starting parse...");

        intermediateHandler = new PIAIntermediateJAXBHandler();
        intermediateHandler.parse(fileName, progress);

        LOGGER.info("{} successfully parsed.\n" +
                "\t {} files\n" +
                "\t {} groups\n" +
                "\t {} accessions\n" +
                "\t {} peptides\n" +
                "\t {} peptide spectrum matches\n" +
                "\t {} trees",
                fileName, intermediateHandler.getFiles().size(),
                intermediateHandler.getGroups().size(),
                intermediateHandler.getAccessions().size(),
                intermediateHandler.getPeptides().size(),
                intermediateHandler.getPSMs().size(),
                intermediateHandler.getNrTrees());

        LOGGER.info("loadIntermediate done.");

        // set spectra uniquenesses
        setGroupsSpectraUniquenesses(intermediateHandler.getGroups().values());

        // the PSMModeller needs no global settings
        psmModeller = new PSMModeller(intermediateHandler.getGroups(),
                intermediateHandler.getFiles(),
                fileName,
                intermediateHandler.getPSMSetSettingsWarnings(),
                intermediateHandler.getPSMs().size());

        // the PeptideModeller takes the PSMModeller and considerModifications
        peptideModeller = new PeptideModeller(psmModeller);

        // initialise the ProteinModeller
        proteinModeller = new ProteinModeller(psmModeller,
                peptideModeller,
                getGroups());

        progress[0] += 60;
    }


    /**
     * Set the uniqueness flags of the spectra in all these groups.
     *
     * @param groups
     */
    private static void setGroupsSpectraUniquenesses(Collection<Group> groups) {
        LOGGER.info("setting spectra uniquenesses");
        for (Group group : groups) {
            // set the uniquenesses o spectra to true, if there is only one accession for this group
            setGroupsSpectraUniquenesses(group,
                    group.getAllAccessions().size() == 1);
        }
        LOGGER.info("spectra uniquenesses set.");
    }


    /**
     * set the uniqueness flag to the given boolean in the spectra of this group
     *
     * @param group
     * @param unique
     */
    private static void setGroupsSpectraUniquenesses(Group group, boolean unique) {
        if (group.getPeptides() == null) {
            return;
        }

        for (Map.Entry<String, Peptide> pepIt : group.getPeptides().entrySet()) {
            for (PeptideSpectrumMatch spec : pepIt.getValue().getSpectra()) {
                spec.setIsUnique(unique);
            }
        }
    }


    /**
     * Getter for the {@link PIAInputFile}s in the intermediate file.<br/>
     * If no intermediate file is given, returns an empty map.
     * @return
     */
    public Map<Long, PIAInputFile> getFiles() {
        if (intermediateHandler != null) {
            return intermediateHandler.getFiles();
        } else {
            return new HashMap<>(1);
        }
    }


    /**
     * Getter for the {@link SearchDatabase}s of the intermediate file.<br/>
     * If no intermediate file is given, returns an empty map.
     *
     * @return
     */
    public Map<String, SearchDatabase> getSearchDatabases() {
        if (intermediateHandler != null) {
            return intermediateHandler.getSearchDatabase();
        } else {
            return new HashMap<>(0);
        }
    }


    /**
     * Getter for the {@link AnalysisSoftware}s of the intermediate file.<br/>
     * If no intermediate file is given, returns an empty map.
     *
     * @return
     */
    public Map<String, AnalysisSoftware> getAnalysisSoftwares() {
        if (intermediateHandler != null) {
            return intermediateHandler.getAnalysisSoftware();
        } else {
            return new HashMap<>(0);
        }
    }


    /**
     * Getter for the {@link SpectraData} of the intermediate file.<br/>
     * If no intermediate file is given, returns an empty map.
     *
     * @return
     */
    public Map<String, SpectraData> getSpectraData() {
        if (intermediateHandler != null) {
            return intermediateHandler.getSpectraData();
        } else {
            return new HashMap<>(0);
        }
    }


    /**
     * Getter for the {@link Group}s in the intermediate file.<br/>
     * If no intermediate file is given, returns an empty map.
     * @return
     */
    public Map<Long, Group> getGroups() {
        if (intermediateHandler != null) {
            return intermediateHandler.getGroups();
        } else {
            return new HashMap<>(1);
        }
    }



    /**
     * The main method, which can be called from the command line.
     *
     * @param args
     */
    public static void main(String[] args) {
    	LOGGER.error("This functionality is moved to PIACli!"
    			+ "\nPlease call this directly (it is the default class of the JAR file by now.");
    }

	
	/**
	 * Exports the PSM level to the given file name. If the format is given, this
	 * will be used for export, otherwise it will be guessed from the filename
	 * 
	 * @param exportFilename export filename, extension will be used for format
	 * guessing, if the format is not specifically given.
	 * @param format format for the export
	 * @param fileID the PIA file (0 for overview) for the export
	 */
    public boolean exportPSMLevel(String exportFilename, String format, long fileID) {
    	if (format == null) {
    		format = FilenameUtils.getExtension(exportFilename);
    	}
    	boolean exportOK = true;
    	
		LOGGER.info("Performing PSM export to {} (format: {}, fileID: {})", exportFilename, format, fileID);
    	if ("mzIdentML".equalsIgnoreCase(format) || "mzid".equalsIgnoreCase(format)) {
            MzIdentMLExporter exporter = new MzIdentMLExporter(this);
            exportOK = exporter.exportToMzIdentML(fileID, exportFilename, false, true);
        } else if ("mztab".equalsIgnoreCase(format)) {
            MzTabExporter exporter = new MzTabExporter(this);
            exportOK = exporter.exportToMzTab(fileID, exportFilename, false, false, true);
        } else if ("csv".equalsIgnoreCase(format)) {
            CSVExporter exporter = new CSVExporter(this);
            exportOK = exporter.exportToCSV(fileID, exportFilename, true, false, false, true);
        } else {
        	LOGGER.error("Could not guess export for {} ({})", exportFilename, format);
        	exportOK = false;
        }
    	
    	return exportOK;
    }
    
    
	/**
	 * Exports the peptide level to the given file name.
	 * <p>
	 * The format for now is always CSV.
	 */
    public boolean exportPeptideLevel(String exportFilename, boolean psmLevel, long fileID) {
		LOGGER.info("Performing peptide export to {} (psmLevel: {}, fileID: {})",
				exportFilename, psmLevel, fileID);
    	CSVExporter exporter = new CSVExporter(this);
    	return exporter.exportToCSV(fileID, exportFilename, psmLevel, true, false, true);
    }
    
    
	/**
	 * Exports the protein level to the given file name. If the format is given,
	 * this will be used for export, otherwise it will be guessed from the
	 * filename
	 * 
	 * @param exportFilename export filename, extension will be used for format
	 * guessing, if the format is not specifically given.
	 * @param format format for the export
	 * @param exportPSMs whether to export PSM information (format dependent)
	 * @param exportPeptides whether to export peptide information (format dependent)
	 * @param exportProteinSequences whether to export protein sequences (format dependent)
	 */
    public boolean exportProteinLevel(String exportFilename, String format,
    		boolean exportPSMs, boolean exportPeptides, boolean exportProteinSequences) {
    	if (format == null) {
    		format = FilenameUtils.getExtension(exportFilename);
    	}
    	boolean exportOK = true;

		LOGGER.info("Performing protein export to {} (format: {}, exportPSMs: {}, exportPeptides: {}, exportProteinSequences: {})",
				exportFilename, format, exportPSMs, exportPeptides, exportProteinSequences);
        if ("mzTab".equalsIgnoreCase(format)) {
            MzTabExporter exporter = new MzTabExporter(this);
            exportOK = exporter.exportToMzTab(0L, exportFilename, true, exportPeptides, true, exportProteinSequences);
        } else if ("mzIdentML".equalsIgnoreCase(format) || "mzid".equalsIgnoreCase(format)) {
            MzIdentMLExporter exporter = new MzIdentMLExporter(this);
            exportOK = exporter.exportToMzIdentML(0L, exportFilename, true, true);
        } else if ("csv".equalsIgnoreCase(format)) {
            CSVExporter exporter = new CSVExporter(this);
            exportOK = exporter.exportToCSV(0L, exportFilename, exportPSMs, exportPeptides, true, true);
        } else {
        	LOGGER.error("Could not guess export for {} ({})", exportFilename, format);
        	exportOK = false;
        }

        return exportOK;
    }

    
    /**
     * Writes the complete processed model to the file given by the name.
     *
     * @param piaModeller
     * @param fileName
     * @throws IOException
     */
    public static void serializeToFile(PIAModeller piaModeller, String fileName) throws IOException {
        File file = new File(fileName);
        serializeToFile(piaModeller, file);
    }


    /**
     * Writes the complete processed model to the given file.
     *
     * @param piaModeller
     * @param file
     * @throws IOException
     */
    public static void serializeToFile(PIAModeller piaModeller, File file) throws IOException {
        LOGGER.info("Serializing data to {}", file.getAbsolutePath());
        try (FileOutputStream fos = new FileOutputStream(file);
                GZIPOutputStream gzo = new GZIPOutputStream(fos);
                ObjectOutputStream oos = new ObjectOutputStream(gzo)) {
            oos.writeObject(piaModeller);
        } catch (StackOverflowError se) {
            LOGGER.error("Could not write whole PIA model to {}", file.getAbsolutePath(), se);
            throw new IOException("Could not serialize whole PIA model, too complex.");
        } catch (Exception e) {
            LOGGER.error("Could not write PIA model to {}", file.getAbsolutePath(), e);
            throw new IOException(e);
        }
    }


    /**
     * Reads a modeller from the file given by the name.
     *
     * @param fileName
     * @throws IOException
     *
     * @throws ClassNotFoundException
     */
    public static PIAModeller deSerializeFromFile(String fileName) throws IOException {
        File file = new File(fileName);
        return deSerializeFromFile(file);
    }


    /**
     * Reads a modeller from the given file
     *
     * @param file
     * @throws IOException
     */
    public static PIAModeller deSerializeFromFile(File file) throws IOException {
        LOGGER.info("reading modeller from {}", file.getAbsolutePath());

        PIAModeller piaModeller;

        try (FileInputStream fin = new FileInputStream(file);
                GZIPInputStream gzi = new GZIPInputStream(fin);
                ObjectInputStream ois = new ObjectInputStream(gzi)) {
            Object readObject = ois.readObject();
            if (readObject instanceof PIAModeller) {
                piaModeller = (PIAModeller) readObject;
            } else {
                String msg = "Could not read a PIAModeller from the file " + file.getAbsolutePath();
                LOGGER.error(msg);
                throw new IOException(msg);
            }
        } catch (IOException e) {
            LOGGER.error("Could not read PIA model from {}", file.getAbsolutePath(), e);
            throw e;
        } catch (ClassNotFoundException e) {
            String msg = "Could not read PIA model from " + file.getAbsolutePath();
            LOGGER.error(msg, e);
            throw new IOException(msg, e);
        }

        return piaModeller;
    }
}
