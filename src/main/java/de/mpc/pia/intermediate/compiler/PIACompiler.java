package de.mpc.pia.intermediate.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftwareList;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.Inputs;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabase;
import uk.ac.ebi.jmzidml.model.mzidml.SpectraData;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;
import uk.ac.ebi.jmzidml.model.utils.ModelConstants;
import uk.ac.ebi.pride.utilities.pridemod.ModReader;
import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Group;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.intermediate.compiler.parser.InputFileParserFactory;
import de.mpc.pia.intermediate.piaxml.AccessionXML;
import de.mpc.pia.intermediate.piaxml.PIAInputFileXML;
import de.mpc.pia.intermediate.piaxml.FilesListXML;
import de.mpc.pia.intermediate.piaxml.GroupXML;
import de.mpc.pia.intermediate.piaxml.PeptideXML;
import de.mpc.pia.intermediate.piaxml.SpectrumMatchXML;
import de.mpc.pia.tools.MzIdentMLTools;
import de.mpc.pia.tools.PIAConstants;
import de.mpc.pia.tools.PIATools;
import de.mpc.pia.tools.matomo.PIAMatomoTracker;
import de.mpc.pia.tools.obo.OBOMapper;
import de.mpc.pia.tools.obo.PsiModParser;
import de.mpc.pia.tools.unimod.UnimodParser;

/**
 * This class is used to read in one or several input files and compile them
 * into one PIA XML intermediate file.
 *
 * @author julianu
 *
 */
public abstract class PIACompiler {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(PIACompiler.class);


    /** just a name for the compilation */
    private String compilationName;

    /** the date, when the compiler was initialised */
    private Date startDate;

    /** map of the used files, maps from file ID to PIAFile */
    private Map<Long, PIAInputFile> files;

    /**
     * list of  maps from the peptide ID to the accession IDs, used for building
     * up the intermediate structure. each map represents one cluster and can be
     * processed as thread.
     */
    private List<Map<Long, Collection<Long>>> clusteredPepAccMap;

    /** the iterator over the clusters (maps from peptide IDs to the accession IDs */
    private ListIterator<Map<Long, Collection<Long>>> clusterIterator;

    /** how many clusters are processed until now */
    private Long buildProgress;

    /** offset for the merged clusters */
    private Long clusterOffset;

    /** the SpectraData (like in mzIdentML) */
    private Map<String, SpectraData> spectraDataMap;

    /** the Generated SpectraData ID to Original SpectraData ID (like in mzIdentML) */
    private Map<String, String> spectraDataIDToOriginalIdMap;

    /** the SearchDatabases (like in mzIdentML) */
    private Map<String, SearchDatabase> searchDatabasesMap;

    /** the analysis software for identifications (class from mzIdentML) */
    private Map<String, AnalysisSoftware> softwareMap;

    /** UnimodParser to get additional information from the UniMod */
    private UnimodParser unimodParser;

    /** PsiModParser to get additional information from the PSI-MOD and map to UniMod */
    private PsiModParser psiModParser;

    /** gets modificatiosn in PRIDE style **/
    private ModReader modReader;

    /** the OBO mapper, to get additional data */
    private OBOMapper oboMapper;

    /** the number of used threads */
    private int numThreads;


    /** map of the groups */
    private Map<Long, Group> groups;


    /** the default name for a compilation */
    public static final String DEFAULT_PIA_COMPILATION_NAME = "PIA compilation";

    /** namespace declaration for jPiaXML */
    private static String nsjPiaXML = "http://www.medizinisches-proteom-center.de/PIA/piaintermediate";

    /** prefixdeclaration for jPiaXML */
    private static String prefixjPiaXML = "ns3";

    /** namespace declaration for mzIdentML */
    private static String nsMzIdentML = "http://psidev.info/psi/pi/mzIdentML/1.1";

    /** prefix declaration for mzIdentML */
    private static String prefixMzIdentML = "ns2";

    /** encoding specification */
    private static String encoding = "UTF-8";


    /** helper description */
    private static final String HELP_DESCRIPTION =
            "PIACompiler is used to compile (multiple) search engine results "
            + "into one PIA XML file. Use a high enough amount of memory (e.g. "
            + "use the Java setting -Xmx8G).";


    /**
     * Basic constructor
     */
    public PIACompiler() {
        compilationName = DEFAULT_PIA_COMPILATION_NAME;
        startDate = new Date();

        files = new HashMap<>();

        spectraDataMap = new HashMap<>();
        searchDatabasesMap = new HashMap<>();
        softwareMap = new HashMap<>();
        spectraDataIDToOriginalIdMap = new HashMap<>();

        oboMapper = null;

        unimodParser = null;
        psiModParser = null;
        modReader = null;

        numThreads = 0;
    }


    /**
     * Getter for the oboMapper. Initializes the OBOMapper on the first call.
     * @return
     */
    public final OBOMapper getOBOMapper() {
        if (oboMapper == null) {
            oboMapper = new OBOMapper();
        }
        return oboMapper;
    }


    /**
     * Getter for the UnimodParser. Initializes the UnimodParser on the first
     * call.
     *
     * @return
     */
    public final UnimodParser getUnimodParser() {
        if (unimodParser == null) {
            LOGGER.info("Initializing unimod parser...");
            unimodParser = new UnimodParser();
            LOGGER.info("unimod parser initialized...");
        }
        return unimodParser;
    }


    /**
     * Getter for the PsiModParser. Initializes the parser on the first call.
     * @return
     */
    public final PsiModParser getPsiModParser() {
        if (psiModParser == null) {
            psiModParser = new PsiModParser();
        }
        return psiModParser;
    }


    /**
     * Getter for the Pride Mod Reader allowing to retrieve information from
     * UNIMOD and PSI-MOD at the same time.
     *
     * Unimod and PSI-MOD
     *
     * @return
     */
    public final ModReader getModReader() {
        if (modReader == null) {
            LOGGER.info("Initializing PRIDE ModReader parser...");
            modReader = ModReader.getInstance();
        }
        return modReader;
    }


    /**
     * Parses the data from the file, given by the fileName.
     *
     * @param name just a name for easier identification
     * @param fileName the path to the file
     * @param additionalInfoFileName an additional information file for the
     * search engine results (like RT for Tandem)
     * @param inputFileType the type of the search engine result fil
     * @return
     */
    public final boolean getDataFromFile(String name, String fileName,
            String additionalInfoFileName, String inputFileType) {
        boolean fileParsed;

        fileParsed = InputFileParserFactory.getDataFromFile(name, fileName,
                this, additionalInfoFileName, inputFileType);

        if (!fileParsed) {
            LOGGER.error("Error parsing the file "+fileName);
        } else {
            LOGGER.info("have now: \n\t"
                    + getNrPeptides() + " peptides\n\t"
                    + getNrPeptideSpectrumMatches() + " peptide spectrum matches\n\t"
                    + getNrAccessions() + " accessions");
        }

        return fileParsed;
    }


    /**
     * Inserts a new file into the map of file and return a reference to it.
     *
     * @param name
     * @param fileName
     * @return
     */
    public final PIAInputFile insertNewFile(String name, String fileName,
            String format) {
        PIAInputFile file;
        Long id = (long)files.size()+1;

        file = new PIAInputFile(id, name, fileName, format);
        files.put(id, file);

        return file;
    }


    /**
     * Returns the {@link PIAInputFile} given by the id
     *
     * @return
     */
    private PIAInputFile getFile(Long fileId) {
        return files.get(fileId);
    }


    /**
     * Returns all {@link PIAInputFile} IDs in the compilation
     *
     * @return
     */
    public final Set<Long> getAllFileIDs() {
        return files.keySet();
    }


    /**
     * Returns the {@link Accession} given by the string of the accession
     *
     * @param acc
     * @return
     */
    public abstract Accession getAccession(String acc);


    /**
     * Returns the {@link Accession} given by the accession ID
     *
     * @param accID
     * @return
     */
    public abstract Accession getAccession(Long accID);


    /**
     * Inserts a new accession into the map of accessions.
     * @param accession new protein accession
     * @param dbSequence sequence
     * @return
     */
    public abstract Accession insertNewAccession(String accession, String dbSequence);


    /**
     * Returns the number of accessions
     *
     * @return
     */
    protected abstract int getNrAccessions();


    /**
     * Returns all accession IDs
     *
     * @return
     */
    protected abstract Collection<Long> getAllAccessionIDs();


    /**
     * Returns the peptide given by its sequence
     *
     * @param sequence
     * @return
     */
    public abstract Peptide getPeptide(String sequence);


    /**
     * Returns the peptide given by the peptide ID
     *
     * @return
     */
    public abstract Peptide getPeptide(Long peptideID);


    /**
     * Inserts a new peptide into the map of peptides.
     *
     * @param sequence
     * @return
     */
    public abstract Peptide insertNewPeptide(String sequence);


    /**
     * Returns the number of peptides
     *
     * @return
     */
    public abstract int getNrPeptides();


    /**
     * Returns all peptide IDs
     *
     * @return
     */
    public abstract Collection<Long> getAllPeptideIDs();


    /**
     * Returns the {@link SpectrumMatch} given by the ID
     *
     * @return
     */
    public abstract PeptideSpectrumMatch getPeptideSpectrumMatch(Long psmId);


    /**
     * Creates a new PSM with the given data, but don't add it into the into the
     * spectra set.
     *
     * @return the newly created PSM
     */
    public abstract PeptideSpectrumMatch createNewPeptideSpectrumMatch(Integer charge,
            double massToCharge, double deltaMass, Double rt, String sequence,
            int missed, String sourceID, String spectrumTitle,
            PIAInputFile file, SpectrumIdentification spectrumID, String spectraDataRef);


    /**
     * Inserts the now completed PSM into the compiler. All further changes in
     * the PSM might not be valid anymore!
     *
     * @param psm
     */
    public abstract void insertCompletePeptideSpectrumMatch(PeptideSpectrumMatch psm);


    /**
     * Returns the number of PSMs
     *
     * @return
     */
    public abstract int getNrPeptideSpectrumMatches();


    /**
     * Returns all psm IDs
     *
     * @return
     */
    public abstract Collection<Long> getAllPeptideSpectrumMatcheIDs();


    /**
     * Returns the Set of {@link Peptide}s from the connection map for the given
     * accession.
     *
     * @param acc
     * @return
     */
    public abstract Set<Peptide> getPeptidesFromConnectionMap(String acc);


    /**
     * Returns the Set of {@link Accession}s from the connection map for the
     * given peptide sequence.
     *
     * @param pep
     * @return
     */
    public abstract Set<Accession> getAccessionsFromConnectionMap(String pep);


    /**
     * Returns the List of {@link Peptide} IDs from the connection map for the
     * given accession ID.
     *
     * @param accId
     * @return
     */
    public abstract Collection<Long> getPepIDsFromConnectionMap(Long accId);


    /**
     * Returns the List of {@link Accession} IDs from the connection map for the
     * given peptide ID.
     *
     * @return
     */
    public abstract Collection<Long> getAccIDsFromConnectionMap(Long pepId);


    /**
     * Puts the given connection from an accession to a peptide into the map.
     *
     * @return
     */
    public abstract void addAccessionPeptideConnection(Accession accession, Peptide peptide);


    /**
     * Erases the accessions to peptide map, can safely be called after
     * {@link #buildClusterList()} was called.
     *
     * @return
     */
    public abstract void clearConnectionMap();


    /**
     * Puts the given {@link AnalysisSoftware} into the softwareMap, if it is
     * not already in there. While doing so, set the software ID for internal
     * purposes.
     *
     * @return the ID of the software in the softwareMap
     */
    public final AnalysisSoftware putIntoSoftwareMap(AnalysisSoftware software) {
        // go through the list of software and look for an equal software
        for (Map.Entry<String, AnalysisSoftware> swIt : softwareMap.entrySet()) {
            if (MzIdentMLTools.paramsEqual(software.getSoftwareName(),
                    swIt.getValue().getSoftwareName())) {
                boolean equal;

                equal = PIATools.bothNullOrEqual(swIt.getValue().getName(),
                        software.getName());

                equal &= PIATools.bothNullOrEqual(swIt.getValue().getUri(),
                        software.getUri());

                equal &= PIATools.bothNullOrEqual(swIt.getValue().getVersion(),
                        software.getVersion());

                // TODO: maybe check for the contact as well... for now, assume if everything is equal, the contact does not matter

                equal &= PIATools.bothNullOrEqual(swIt.getValue().getCustomizations(),
                        software.getCustomizations());

                if (equal) {
                    // the software is already in the list, return it
                    return swIt.getValue();
                }
            }
        }

        String strID = PIAConstants.software_prefix + (softwareMap.size() + 1L);
        softwareMap.put(strID, software);
        software.setId(strID);
        return software;
    }



    /**
     * Puts the given {@link SearchDatabase} into the searchDatabasesMap, if it
     * is not already in it. While doing so, set the identification ID for
     * internal purposes.
     *
     * @return the ID of the spectrumIdentification in the spectrumIdentificationsMap
     */
    public final SearchDatabase putIntoSearchDatabasesMap(SearchDatabase database) {
        // go through the databases and check, if any equals the given database
        for (Map.Entry<String, SearchDatabase> dbIt : searchDatabasesMap.entrySet()) {

            if (dbIt.getValue().getLocation() != null && dbIt.getValue().getLocation().equals(database.getLocation())) {
                boolean equal;

                equal = PIATools.bothNullOrEqual(dbIt.getValue().getName(),
                        database.getName());

                equal &= PIATools.bothNullOrEqual(dbIt.getValue().getNumDatabaseSequences(),
                        database.getNumDatabaseSequences());

                equal &= PIATools.bothNullOrEqual(dbIt.getValue().getNumResidues(),
                        database.getNumResidues());

                equal &= PIATools.bothNullOrEqual(dbIt.getValue().getReleaseDate(),
                        database.getReleaseDate());

                equal &= PIATools.bothNullOrEqual(dbIt.getValue().getVersion(),
                        database.getVersion());

                equal &= PIATools.bothNullOrEqual(dbIt.getValue().getExternalFormatDocumentation(),
                        database.getExternalFormatDocumentation());

                equal &= MzIdentMLTools.fileFormatsEqualOrNull(dbIt.getValue().getFileFormat(),
                        database.getFileFormat());

                equal &= MzIdentMLTools.paramsEqual(dbIt.getValue().getDatabaseName(),
                        database.getDatabaseName());

                if (equal) {
                    List<CvParam> params1 = dbIt.getValue().getCvParam();
                    List<CvParam> params2 = database.getCvParam();

                    if (params1.size() == params2.size()) {
                        boolean failed = false;

                        for (CvParam param : params1) {
                            boolean found = false;

                            // look for this param in the other list...
                            for (CvParam checkParam : params2) {
                                if (MzIdentMLTools.cvParamsEqualOrNull(param,
                                        checkParam)) {
                                    found = true;
                                    break;
                                }
                            }

                            if (!found) {
                                failed = true;
                                break;
                            }
                        }

                        if (failed) {
                            equal = false;
                        }
                    } else {
                        // not the same size
                        equal = false;
                    }
                }

                if (equal) {
                    // the database is already in the list, return its ID
                    return dbIt.getValue();
                }
            }
        }

        String strID = PIAConstants.databases_prefix +
                (searchDatabasesMap.size() + 1L);
        searchDatabasesMap.put(strID, database);
        database.setId(strID);
        return database;
    }


    /**
     * Puts the given {@link SearchDatabase} into the searchDatabasesMap, if it
     * is not already in it. While doing so, set the identification ID for
     * internal purposes.
     *
     * @return the ID of the spectrumIdentification in the spectrumIdentificationsMap
     */
    public final SpectraData putIntoSpectraDataMap(SpectraData spectra) {
        // go through the databases and check, if any equals the given database
        for (Map.Entry<String, SpectraData> spectraIt : spectraDataMap.entrySet()) {
            if (spectraIt.getValue().getLocation().equals(spectra.getLocation())) {
                boolean equal;

                equal = PIATools.bothNullOrEqual(spectraIt.getValue().getName(),
                        spectra.getName());

                equal &= PIATools.bothNullOrEqual(
                        spectraIt.getValue().getExternalFormatDocumentation(),
                        spectra.getExternalFormatDocumentation());

                equal &= MzIdentMLTools.fileFormatsEqualOrNull(
                        spectraIt.getValue().getFileFormat(), spectra.getFileFormat());

                equal &= MzIdentMLTools.spectrumIDFormatEqualOrNull(
                        spectraIt.getValue().getSpectrumIDFormat(),
                        spectra.getSpectrumIDFormat());

                if (equal) {
                    return spectraIt.getValue();
                }
            }
        }

        String strID = PIAConstants.spectra_data_prefix +
                (spectraDataMap.size() + 1L);
        spectraDataMap.put(strID, spectra);
        this.spectraDataIDToOriginalIdMap.put(strID, spectra.getId());
        spectra.setId(strID);

        return spectra;
    }



    /**
     * Builds up the list of peptide accession maps. The list is clustered, i.e.
     * each entry in the list may be processed in parallel.
     * <p>
     * Before calling this method, some data should be read in by
     * {@link PIACompiler#getDataFromFile(String, String, String, String)}.
     */
    public final void buildClusterList() {
        LOGGER.info("start sorting clusters");

        Set<Long> peptidesDone = new HashSet<>(getNrPeptides());
        Set<Long> accessionsDone = new HashSet<>(getNrAccessions());
        clusteredPepAccMap = new ArrayList<>();


        // This accession is not yet clustered, so start a new cluster and
        // insert all the "connected" peptides and accessions
        getAllAccessionIDs().stream().filter(accID -> !accessionsDone.contains(accID)).forEach(accID -> {
            Map<Long, Collection<Long>> pepAccMapCluster = createCluster(accID, peptidesDone, accessionsDone);

            if (pepAccMapCluster != null) {
                clusteredPepAccMap.add(pepAccMapCluster);
            } else {
                LOGGER.error("cluster could not be created!");
            }

        });

        // the maps are no longer needed
        clearConnectionMap();

        LOGGER.info("clusters sorted: " + clusteredPepAccMap.size());
    }


    /**
     * Inserts the cluster of the given accession into the peptide accession
     * map cluster.<br/>
     * This method should only be called by {@link PIACompiler#buildClusterList()}.
     *
     * @param accessionID
     * @param peptidesDone
     * @param accessionsDone
     * @return
     */
    private final Map<Long, Collection<Long>> createCluster(Long accessionID,
            Set<Long> peptidesDone, Set<Long> accessionsDone) {
        Set<Long> clusterAccessions = new HashSet<>();
        Set<Long> clusterPeptides = new HashSet<>();

        // initialize the cluster's peptides with the peptides of the given accession
        int newPeptides = 0;
        int newAccessions = 1;  // for the given accession

        for (Long pepId : getPepIDsFromConnectionMap(accessionID)) {
            clusterPeptides.add(pepId);
            newPeptides++;
        }

        // repeat as long, as we get more accessions or peptides
        while ((newAccessions > 0) || (newPeptides > 0)) {
            newAccessions = 0;
            newPeptides = 0;

            // get accessions for peptides, which are new since the last loop
            for (Long pepId : clusterPeptides) {
                if (!peptidesDone.contains(pepId)) {
                    for (Long accId : getAccIDsFromConnectionMap(pepId)) {
                        if (clusterAccessions.add(accId)) {
                            newAccessions++;
                        }
                    }

                    peptidesDone.add(pepId);
                }
            }

            // get peptides for accessions, which are new since the last loop
            for (Long accId : clusterAccessions) {
                if (!accessionsDone.contains(accId)) {
                    for (Long pepId : getPepIDsFromConnectionMap(accId)) {
                        if (clusterPeptides.add(pepId)) {
                            newPeptides++;
                        }
                    }
                    accessionsDone.add(accId);
                }
            }
        }

        // now we have the whole cluster, so put it into the pepAccMapCluster
        Map<Long, Collection<Long>> pepAccMapCluster = new HashMap<>();
        for (Long pepId : clusterPeptides) {
            pepAccMapCluster.put(pepId, getAccIDsFromConnectionMap(pepId));
        }

        return pepAccMapCluster;
    }


    /**
     * Build up the intermediate structure.<br/>
     * Before this method is called, {@link PIACompiler#buildClusterList()}
     * must be called.
     */
    public final void buildIntermediateStructure() {
        int nrThreads;

        if (numThreads > 0) {
            nrThreads = numThreads;
        } else {
            nrThreads = Runtime.getRuntime().availableProcessors();
        }

        LOGGER.info("Using " + nrThreads + " threads.");

        List<CompilerWorkerThread> threads;


        if (clusteredPepAccMap == null) {
            LOGGER.error("the cluster map is not yet build!");
            return;
        }

        // initialize the groups map
        groups = new HashMap<>();

        // initialize the clusterIterator
        clusterIterator = clusteredPepAccMap.listIterator();
        buildProgress = 0L;
        clusterOffset = 0L;

        // start the threads
        threads = new ArrayList<>(nrThreads);
        for (int i = 0; i < nrThreads; i++) {
            CompilerWorkerThread thread = new CompilerWorkerThread(i+1, this);
            threads.add(thread);

            thread.start();
        }

        // wait for the threads to finish
        for (CompilerWorkerThread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                LOGGER.error("thread got interrupted!", e);
                Thread.currentThread().interrupt();
            }
        }
    }


    /**
     * Returns the next cluster in the cluster map (mapping from the peptide IDs
     * to the accession IDs)
     *
     * @return
     */
    public final synchronized Map<Long, Collection<Long>> getNextCluster() {
        synchronized (clusterIterator) {
            if (clusterIterator != null) {
                if (clusterIterator.hasNext()) {
                    return clusterIterator.next();
                } else {
                    return null;
                }
            } else {
                LOGGER.error("The cluster iterator is not yet initialized!");
                return null;
            }
        }
    }


    /**
     * Increase the number of build clusters.
     */
    public final synchronized void increaseBuildProgress() {
        buildProgress++;
    }


    /**
     * Merge the given groups into the groups map.
     *
     * @param subGroups
     */
    public final synchronized void mergeClustersIntoMap(Map<Long, Group> subGroups,
            long nrClusters) {
        synchronized (groups) {
            long groupOffset = groups.size();

            for (Group group : subGroups.values()) {
                group.setOffset(groupOffset);
                group.setTreeID(group.getTreeID()+clusterOffset);
                groups.put(group.getID(), group);
            }

            clusterOffset += nrClusters;
        }
    }


    /**
     * Setter for the name
     * @param name
     */
    public final void  setName(String name) {
        this.compilationName = name;
    }


    /**
     * Getter for the name.
     * @return
     */
    public final String getName() {
        return compilationName;
    }


    /**
     * Sets the number of used threads. If this is smaller than 1, all available
     * threads (Runtime.getRuntime().availableProcessors()) are used.
     *
     * @param threads
     */
    public final void setNrThreads(int threads) {
        numThreads = threads;
    }


    /**
     * Gets the number of used threads
     */
    public final int getNrThreads() {
        return numThreads;
    }


    /**
     * Write out the intermediate structure into an XML file.
     *
     * @param piaFile
     * @throws IOException
     */
    public final void writeOutXML(File piaFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(piaFile)) {
            LOGGER.info("Writing PIA XML file to " + piaFile.getAbsolutePath());
            writeOutXML(fos);
        }
    }


    /**
     * Write out the intermediate structure into an XML file.
     *
     * @param fileName
     * @throws IOException
     */
    public final void writeOutXML(String fileName) throws IOException {
        File piaFile = new File(fileName);
        writeOutXML(piaFile);
    }


    /**
     * Write out the intermediate structure into an XML file.
     *
     */
    public final void writeOutXML(OutputStream outputStream) {
        try (Writer out = new OutputStreamWriter(outputStream, encoding)) {
            LOGGER.info("Stream open, writing PIA XML");

            XMLOutputFactory xmlof = XMLOutputFactory.newInstance();
            XMLStreamWriter xmlOut = new IndentingXMLStreamWriter(xmlof.createXMLStreamWriter(out));

            // xml header
            xmlOut.writeStartDocument(encoding, "1.0");

            // the piaXML root element
            xmlOut.writeStartElement(prefixjPiaXML, "jPiaXML", nsjPiaXML);
            xmlOut.setPrefix(prefixjPiaXML, nsjPiaXML);

            xmlOut.writeAttribute("name", compilationName);
            xmlOut.writeAttribute("date", startDate.toString());

            xmlOut.writeNamespace(prefixMzIdentML, nsMzIdentML);
            xmlOut.writeNamespace(prefixjPiaXML, nsjPiaXML);

            // filesList
            writeOutJaxbFilesList(xmlOut);

            // inputs
            writeOutJaxbInputs(xmlOut);

            // analysisSoftwareList
            writeOutJaxbAnalysisSoftwareList(xmlOut);

            // spectraList
            writeOutJaxbSpectra(xmlOut);

            // accessionsList
            writeOutJaxbAccessions(xmlOut);

            // peptidesList
            writeOutJaxbPeptides(xmlOut);

            // groupsList
            writeOutJaxbGroups(xmlOut);

            xmlOut.writeEndElement(); // jPiaXML

            xmlOut.close();
        } catch (XMLStreamException e) {
            LOGGER.error("XMLStreamException while writing XML file", e);
        } catch (FactoryConfigurationError e) {
            LOGGER.error("FactoryConfigurationError while writing XML file", e);
        } catch (JAXBException e) {
            LOGGER.error("JAXBException while writing XML file", e);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("UnsupportedEncodingException while writing XML file", e);
        } catch (IOException e) {
            LOGGER.error("error writing the PIA XML file", e);
        }

        LOGGER.info("Writing of PIA XML file finished.");
    }


    /**
     * Creates a marshaller for PIA XML for the given class.
     *
     * @return
     * @throws JAXBException
     */
    private static Marshaller createMarshallerForPiaXML(Class<?> marshalClass) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(marshalClass);

        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_ENCODING, encoding);
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

        return m;
    }


    /**
     * Creates an formatted (indenting) jaxb fragment marshaller for the given
     * context, using the given jaxbElement and the given class for marshalling.
     *
     * @param xmlOut the writer
     * @param jaxbElement an jaxbElement
     * @param marshalClass the class for marshalling
     *
     * @throws JAXBException
     */
    private static void marshalToFormattedFragmentMarshaller(XMLStreamWriter xmlOut,
            Object jaxbElement, Class<?> marshalClass) throws JAXBException {
        Marshaller m = createMarshallerForPiaXML(marshalClass);
        m.marshal(jaxbElement, xmlOut);
    }


    /**
     * Creates an formatted (indenting) jaxb fragment marshaller for the given
     * context, using the given jaxbElement and the given class for marshalling.
     *
     * @param xmlOut the writer
     * @param object the object to be marshalled, will be casted to a
     * jaxbElement
     *
     * @throws JAXBException
     */
    private static <T> void marshalToFormattedFragmentMarshaller(
            XMLStreamWriter xmlOut, T object) throws JAXBException {
        QName aQName = ModelConstants.getQNameForClass(object.getClass());

        @SuppressWarnings("unchecked")
        Class<T> classCast = (Class<T>)object.getClass();
        JAXBElement<T> jaxbElement = new JAXBElement<>(aQName, classCast, object);

        marshalToFormattedFragmentMarshaller(xmlOut, jaxbElement, classCast);
    }


    /**
     * Writes out the filesList object to XML, using the given writer.
     *
     * @param xmlOut
     * @throws JAXBException
     */
    private void writeOutJaxbFilesList(XMLStreamWriter xmlOut) throws JAXBException {
        FilesListXML fileslistXML = new FilesListXML();
        for (Long fileID : getAllFileIDs()) {
            PIAInputFile file = getFile(fileID);
            PIAInputFileXML fileXML = new PIAInputFileXML();

            fileXML.setId(file.getID());
            fileXML.setName(file.getName());
            fileXML.setFileName(file.getFileName());
            fileXML.setFormat(file.getFormat());

            fileXML.setAnalysisCollection(file.getAnalysisCollection());
            fileXML.setAnalysisProtocolCollection(file.getAnalysisProtocolCollection());

            fileslistXML.getFiles().add(fileXML);
        }

        marshalToFormattedFragmentMarshaller(xmlOut, fileslistXML, FilesListXML.class);
    }


    /**
     * Writes out the Inputs object to XML, using the given writer.
     *
     * @param xmlOut
     * @throws JAXBException
     */
    private void writeOutJaxbInputs(XMLStreamWriter xmlOut) throws JAXBException {
        Inputs inputs = new Inputs();
        inputs.getSearchDatabase().addAll(searchDatabasesMap.values());
        inputs.getSpectraData().addAll(spectraDataMap.values());

        marshalToFormattedFragmentMarshaller(xmlOut, inputs);
    }


    /**
     * Writes out the AnalysisSoftwareList object to XML, using the given writer.
     *
     * @param xmlOut
     * @throws JAXBException
     */
    private void writeOutJaxbAnalysisSoftwareList(XMLStreamWriter xmlOut) throws JAXBException {
        AnalysisSoftwareList softwareList = new AnalysisSoftwareList();
        softwareList.getAnalysisSoftware().addAll(softwareMap.values());

        marshalToFormattedFragmentMarshaller(xmlOut, softwareList);
    }


    /**
     * Writes out the PSMs to XML, using the given writer.
     *
     * @param xmlOut
     * @throws JAXBException
     */
    private void writeOutJaxbSpectra(XMLStreamWriter xmlOut) throws XMLStreamException, JAXBException {
        xmlOut.writeStartElement("spectraList");

        Marshaller m = createMarshallerForPiaXML(SpectrumMatchXML.class);

        for (Long psmId : getAllPeptideSpectrumMatcheIDs()) {
            // marshall all spectra
            m.marshal(new SpectrumMatchXML(getPeptideSpectrumMatch(psmId)), xmlOut);
        }

        xmlOut.writeEndElement(); // spectraList
    }


    /**
     * Writes out the accessions to XML, using the given writer.
     *
     * @param xmlOut
     * @throws JAXBException
     */
    private void writeOutJaxbAccessions(XMLStreamWriter xmlOut) throws XMLStreamException, JAXBException {
        xmlOut.writeStartElement("accessionsList");

        Marshaller m = createMarshallerForPiaXML(AccessionXML.class);

        for (Long accId : getAllAccessionIDs()) {
            m.marshal(new AccessionXML(getAccession(accId)), xmlOut);
        }

        xmlOut.writeEndElement(); // accessionsList
    }


    /**
     * Writes out the peptides to XML, using the given writer.
     *
     * @param xmlOut
     * @throws JAXBException
     */
    private void writeOutJaxbPeptides(XMLStreamWriter xmlOut) throws XMLStreamException, JAXBException {
        xmlOut.writeStartElement("peptidesList");

        Marshaller m = createMarshallerForPiaXML(PeptideXML.class);

        for (Long pepId : getAllPeptideIDs()) {
            m.marshal(new PeptideXML(getPeptide(pepId)), xmlOut);
        }

        xmlOut.writeEndElement(); // peptidesList
    }


    /**
     * Writes out the groups to XML, using the given writer.
     *
     * @param xmlOut
     * @throws JAXBException
     */
    private void writeOutJaxbGroups(XMLStreamWriter xmlOut) throws XMLStreamException, JAXBException {
        xmlOut.writeStartElement("groupsList");

        Marshaller m = createMarshallerForPiaXML(GroupXML.class);

        for (Group group : groups.values()) {
            m.marshal(new GroupXML(group), xmlOut);
        }

        xmlOut.writeEndElement(); // groupsList
    }


    /**
     * Assures that all streams are closed and all temporary files are removed
     */
    public abstract void finish();


    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();

        Option outfileOpt = Option.builder("outfile")
                .required(true)
                .argName("outputFile")
                .hasArg()
                .desc("path to the created PIA XML file")
                .build();
        options.addOption(outfileOpt);

        Option nameOpt = Option.builder("name")
                .argName("name")
                .hasArg()
                .desc("name of the PIA compilation")
                .build();
        options.addOption(nameOpt);

        Option inputFileOpt = Option.builder("infile")
                .argName("inputFile")
                .hasArg()
                .desc( "input file with possible further information separated by semicolon. This option may be called "
                        + "multiple times. Any further information not given will be treated as null, the information "
                        + "is in this order:\n"
                        + "name of the input file (as shown in the PIA viewers, if not given will be set to the path "
                        + "of the input file), type of the file (usually guessed, but may also be explicitly given, "
                        + "possible values are "
                        + InputFileParserFactory.getAvailableTypeShorts()
                        + "), additional information file (very seldom used)")
                .build();
        options.addOption(inputFileOpt);

        Option disableUsageStatisticsOpt = Option.builder("disableUsageStatistics")
                .hasArg(false)
                .desc("set this option to disable the collection of usage statistics for quality control and"
                        + "funding purposes")
                .build();
        options.addOption(disableUsageStatisticsOpt);

        if (args.length < 1) {
            PIATools.printCommandLineHelp(PIACompiler.class.getSimpleName(),
                    options, HELP_DESCRIPTION);
            return;
        }

        String outFileName = null;
        String piaName;
        PIACompiler piaCompiler = new PIASimpleCompiler();

        // parse the command line arguments
        try {
            CommandLine line = parser.parse( options, args );

            PIAMatomoTracker.disableTracking(line.hasOption(disableUsageStatisticsOpt.getOpt()));

            PIAMatomoTracker.trackPIAEvent(PIAMatomoTracker.PIA_TRACKING_COMMAND_LINE_CATEGORY,
                    PIAMatomoTracker.PIA_TRACKING_COMPILER_NAME, PIAMatomoTracker.PIA_TRACKING_COMPILER_STARTED, null);
            boolean filesOk = false;
            if (line.hasOption(inputFileOpt.getOpt())) {
                filesOk = parseCommandLineInfiles(line.getOptionValues(inputFileOpt.getOpt()), piaCompiler);
            }
            if (!filesOk) {
                return;
            }

            piaCompiler.buildClusterList();
            piaCompiler.buildIntermediateStructure();

            piaName = line.getOptionValue(nameOpt.getOpt());
            if (piaName == null) {
                piaName = outFileName;
            }
            piaCompiler.setName(piaName);

            // now write out the file
            outFileName = line.getOptionValue(outfileOpt.getOpt());
            piaCompiler.writeOutXML(outFileName);
            piaCompiler.finish();
            PIAMatomoTracker.trackPIAEvent(PIAMatomoTracker.PIA_TRACKING_COMMAND_LINE_CATEGORY,
                    PIAMatomoTracker.PIA_TRACKING_COMPILER_NAME, PIAMatomoTracker.PIA_TRACKING_COMPILER_FINISHED, null);
        } catch (ParseException e) {
            LOGGER.error("error parsing the command line: " + e.getMessage());
            PIATools.printCommandLineHelp(PIACompiler.class.getSimpleName(),
                    options, HELP_DESCRIPTION);
            System.exit(-1);
        } catch (IOException e) {
            LOGGER.error("Error while writing PIA XML file.", e);
            PIAMatomoTracker.trackPIAEvent(PIAMatomoTracker.PIA_TRACKING_COMMAND_LINE_CATEGORY,
                    PIAMatomoTracker.PIA_TRACKING_COMPILER_NAME, PIAMatomoTracker.PIA_TRACKING_COMPILER_ERROR, null);
        }

    }


    /**
     * Parses the files given from the command line in the String array into the
     * given {@link PIACompiler}. The files may also contain the name and
     * additionalFile separated by a semicolon.
     *
     * @param inputFiles
     * @param piaCompiler
     * @return true, if all files were parsed correctly, otherwise false
     */
    private static boolean parseCommandLineInfiles(String[] inputFiles, PIACompiler piaCompiler) {
        for (String inputFile : inputFiles) {
            if (!parseCommandLineInfile(inputFile, piaCompiler)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Parses one file from the command line into the given {@link PIACompiler}.
     * The file string may also contain the name and additionalFile separated by
     * a semicolon.
     *
     * @param inputFile
     * @param piaCompiler
     * @return true, if the file was parsed correctly, otherwise false
     */
    private static boolean parseCommandLineInfile(String inputFile, PIACompiler piaCompiler) {
        String[] values = inputFile.split(";");
        String file = values[0];
        String name = values[0];
        String additionalInfoFile = null;
        String type = null;

        if (values.length > 1) {
            name = (values[1].trim().length() > 0) ? values[1].trim() : null;

            if (values.length > 2) {
                type = (values[2].length() > 0) ? values[2] : null;
            }
            if (values.length > 3) {
                additionalInfoFile = (values[3].length() > 0) ? values[3] : null;
            }
        } else {
            if (file.contains(File.separator)) {
                // take the filename-only as name, if none is given
                name = new File(file).getName();
            }
        }

        LOGGER.info("file: " + file +
                "\n\tname: " + name +
                "\n\ttype: " + type +
                "\n\tadditional info file: " + additionalInfoFile);

        return piaCompiler.getDataFromFile(name, file, additionalInfoFile, type);
    }

    /**
     * PIA generates internal autogenerated spectra IDs which may leads to loose the
     * relationship between original SpectraDataRef refered in assays(i.e MzIdentML).
     * Therefore, the relationship between original and the new Spectra IDs are kept
     * in a Map
     * @return Map of original and the new Spectra IDs
     */
    public Map<String, String> getSpectraDataIDToOriginalIdMap() {
        return spectraDataIDToOriginalIdMap;
    }
}
