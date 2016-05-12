package de.mpc.pia.modeller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.Group;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.intermediate.xmlhandler.PIAIntermediateJAXBHandler;
import de.mpc.pia.modeller.execute.xmlparams.CTDTool;
import de.mpc.pia.modeller.execute.xmlparams.NODEType;
import de.mpc.pia.modeller.execute.xmlparams.PARAMETERSType;
import de.mpc.pia.modeller.peptide.PeptideExecuteCommands;
import de.mpc.pia.modeller.protein.ProteinExecuteCommands;
import de.mpc.pia.modeller.psm.PSMExecuteCommands;
import de.mpc.pia.tools.PIAConstants;
import de.mpc.pia.tools.PIATools;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabase;
import uk.ac.ebi.jmzidml.model.mzidml.SpectraData;


/**
 * The main modeller class for PIA.
 *
 * @author julian
 *
 */
public class PIAModeller {

    /** the modeller for everything PSM related */
    private PSMModeller psmModeller;

    /** the modeller for everything peptide related */
    private PeptideModeller peptideModeller;

    /** the modeller for everything protein related */
    private ProteinModeller proteinModeller;


    /** name of the used file */
    private String fileName;

    /** handler for the intermediate file */
    private PIAIntermediateJAXBHandler intermediateHandler;


    /** logger for this class */
    private static final Logger logger = Logger.getLogger(PIAModeller.class);

    /** helper description */
    private static final String helpDescription =
            "PIAModeller... Use a high enough amount of memory (e.g. " +
                    "use the Java setting -Xmx8G).";


    /**
     * Very basic constructor.
     */
    public PIAModeller() {
        psmModeller = null;
        peptideModeller = null;
        proteinModeller = null;

        // TODO: make the default settings through a ini-file or something like that
        fileName = null;
        intermediateHandler = null;
    }


    /**
     * Basic constructor, creates a model for the given file.
     *
     * @param fileName
     * @throws JAXBException
     * @throws FileNotFoundException
     */
    public PIAModeller(String fileName)
            throws FileNotFoundException, JAXBException, XMLStreamException {
        this();

        if (fileName == null) {
            throw new IllegalArgumentException("No file name given.");
        }

        loadFileName(fileName, null);
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
     * Also initialises the model, if the fileName changed.
     *
     * @param filename
     * @param progress the first item in the array holds the current progress
     * @param notifier progress will be notified on this object
     *
     * @throws JAXBException
     * @throws FileNotFoundException
     *
     * @return true, if a new file was loaded
     */
    public boolean loadFileName(String filename, Long[] progress, Object notifier)
            throws FileNotFoundException, JAXBException, XMLStreamException {
        logger.info("start loading file " + filename);

        if (filename != null) {
            this.psmModeller = null;
            this.peptideModeller = null;
            this.proteinModeller = null;

            this.fileName = filename;
            this.intermediateHandler = null;

            if ((this.fileName != null) && !this.fileName.equals("")) {
                parseIntermediate(progress);
                if (notifier != null) {
                    synchronized (notifier) {
                        notifier.notifyAll();
                    }
                }
                return true;
            }
        }

        if (notifier != null) {
            synchronized (notifier) {
                notifier.notifyAll();
            }
        }
        return false;
    }


    /**
     * Setter for fileName.
     * Also initialises the model, if the fileName changed.
     *
     * @param filename
     * @throws JAXBException
     * @throws FileNotFoundException
     *
     * @return true, if a new file was loaded
     */
    public boolean loadFileName(String filename, Long[] progress)
            throws FileNotFoundException, JAXBException, XMLStreamException {
        return loadFileName(filename, progress, null);
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
        psmModeller.setCreatePSMSets(createPSMSets);
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
                    new HashMap<String, Set<Long>>(1);
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
     * The progress gets increased by 100.
     *
     * @param progress the first entry in this array holds the progress
     * @param notifier progress is notified on this object
     */
    private void parseIntermediate(Long[] progress, Object notifier)
            throws FileNotFoundException, JAXBException, XMLStreamException {
        logger.info("loadIntermediate started...");

        if (progress == null) {
            logger.warn("no progress array given, creating one. But no external" +
                    "supervision possible");
            progress = new Long[1];
        }

        progress[0] = 0L;
        if (notifier != null) {
            synchronized (notifier) {
                notifier.notifyAll();
            }
        }

        if (fileName == null) {
            logger.error("no file given!");
            return;
        }

        try {
            logger.info("Starting parse...");

            intermediateHandler = new PIAIntermediateJAXBHandler();
            intermediateHandler.parse(fileName, progress, notifier);

            logger.info(fileName + " successfully parsed.\n" +
                    "\t" + intermediateHandler.getFiles().size() + " files\n" +
                    "\t" + intermediateHandler.getGroups().size() + " groups\n" +
                    "\t" + intermediateHandler.getAccessions().size() + " accessions\n" +
                    "\t" + intermediateHandler.getPeptides().size() + " peptides\n" +
                    "\t" + intermediateHandler.getPSMs().size() + " peptide spectrum matches\n" +
                    "\t" + intermediateHandler.getNrTrees() + " trees");
        } catch (FileNotFoundException e) {
            throw e;
        } catch (JAXBException e) {
            throw e;
        } catch (XMLStreamException e) {
            throw e;
        } finally {
            logger.info("loadIntermediate done.");
        }

        if (intermediateHandler != null) {
            // TODO: maybe move this uniqueness setting the PIACompiler...
            logger.info("setting spectra uniquenesses");

            for (Map.Entry<Long, Group> grIt : intermediateHandler.getGroups().entrySet()) {
                if (grIt.getValue().getAllAccessions().size() == 1) {

                    if (grIt.getValue().getPeptides() != null) {
                        for (Map.Entry<String, Peptide> pepIt : grIt.getValue().getPeptides().entrySet()) {
                            for (PeptideSpectrumMatch spec : pepIt.getValue().getSpectra()) {
                                spec.setIsUnique(true);
                            }
                        }
                    }

                }
            }
            logger.info("spectra uniquenesses set.");

            // the PSMModeller needs no global settings
            psmModeller = new PSMModeller(intermediateHandler.getGroups(),
                    intermediateHandler.getFiles(),
                    intermediateHandler.getSearchDatabase(),
                    intermediateHandler.getSpectraData(),
                    intermediateHandler.getAnalysisSoftware(),
                    fileName,
                    intermediateHandler.getPSMSetSettingsWarnings(),
                    intermediateHandler.getPSMs().size());

            // the PeptideModeller takes the PSMModeller and considerModifications
            peptideModeller = new PeptideModeller(psmModeller);

            // initialise the ProteinModeller
            proteinModeller = new ProteinModeller(psmModeller,
                    peptideModeller,
                    getGroups(),
                    intermediateHandler.getSearchDatabase());

            progress[0] += 60;
            if (notifier != null) {
                synchronized (notifier) {
                    notifier.notifyAll();
                }
            }
        }
    }


    /**
     * Parses in the intermediate structure from the given file.<br/>
     * The progress gets increased by 100.
     */
    private void parseIntermediate(Long[] progress)
            throws FileNotFoundException, JAXBException, XMLStreamException {
        parseIntermediate(progress, null);
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
            return new HashMap<Long, PIAInputFile>(1);
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
            return new HashMap<String, SearchDatabase>(0);
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
            return new HashMap<String, AnalysisSoftware>(0);
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
            return new HashMap<String, SpectraData>(0);
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
            return new HashMap<Long, Group>(1);
        }
    }


    /**
     * Process a parameter pipeline file and executes the commands.<br/>
     * A parameter file is an XML file in the CTD schema (used also by OpenMS
     * and GenericKnimeNodes).
     *
     * @param paramFileName
     */
    public static void processPipelineFile(String paramFileName,
            PIAModeller model) {
        logger.info("starting parse parameter file " + paramFileName);

        try {
            JAXBContext context = JAXBContext.newInstance(CTDTool.class);
            Unmarshaller um = context.createUnmarshaller();
            CTDTool parametersXML =
                    (CTDTool)um.unmarshal(new FileReader(paramFileName));

            for (NODEType node : parametersXML.getPARAMETERS().getNODE()) {
                String nodeName = node.getName();

                logger.debug("parsing node " + nodeName);

                if (nodeName.startsWith(PSMExecuteCommands.prefix)) {
                    PSMExecuteCommands execute = PSMExecuteCommands.valueOf(
                            nodeName.substring(
                                    PSMExecuteCommands.prefix.length()));
                    if (execute != null) {
                        execute.executeXMLParameters(
                                node, model.getPSMModeller());
                    }
                } else if (nodeName.startsWith(PeptideExecuteCommands.prefix)) {
                    PeptideExecuteCommands execute = PeptideExecuteCommands.valueOf(
                            nodeName.substring(
                                    PeptideExecuteCommands.prefix.length()));
                    if (execute != null) {
                        execute.executeXMLParameters(
                                node, model.getPeptideModeller());
                    }
                } else if (nodeName.startsWith(ProteinExecuteCommands.prefix)) {
                    ProteinExecuteCommands execute = ProteinExecuteCommands.valueOf(
                            nodeName.substring(
                                    ProteinExecuteCommands.prefix.length()));
                    if (execute != null) {
                        execute.executeXMLParameters(
                                node, model.getProteinModeller());
                    }
                } else {
                    logger.error("Could not execute " + nodeName);
                }
            }

        } catch (JAXBException e) {
            logger.error("Error parsing the file " + paramFileName, e);
        } catch (FileNotFoundException e) {
            logger.error("Could not find the file " + paramFileName);
        }

        logger.info("finished parsing of parameter file " + paramFileName);
    }


    /**
     * This method initialises a new pipeline XML file with only the name
     * given. This file then can be filled by pipeline modeling procedures and
     * finally be executed by {@link #processPipelineFile(String)}.
     *
     * @param fileName
     */
    public static void initialisePipelineXML(String fileName, String name) {
        logger.info("initialising parameter file for " + name);

        CTDTool pipelineXML = new CTDTool();

        // set initialisation parameters
        pipelineXML.setName(name);
        pipelineXML.setVersion(PIAConstants.version);
        pipelineXML.setDescription("This file will contains a pipeline " +
                "execution for PIA");
        pipelineXML.setDocurl("http://www.medizinisches-proteom-center.de");
        pipelineXML.setPARAMETERS(new PARAMETERSType());

        // write them to file
        try {
            JAXBContext context = JAXBContext.newInstance(CTDTool.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.marshal(pipelineXML, new File(fileName));
        } catch (JAXBException e) {
            logger.error("Error while creating file:", e);
        }

        logger.info("initial parameter file written to " + fileName);
    }


    /**
     * Adds a new node for execution to the parameters of the given XML file and
     * writes it back to a file.
     *
     * @param fileName
     * @param params
     */
    public static void appendToPipelineXML(String fileName, String newFileName,
            String[] params) {
        try {
            // load the XML content
            JAXBContext context = JAXBContext.newInstance(CTDTool.class);
            Unmarshaller um = context.createUnmarshaller();
            CTDTool execution =
                    (CTDTool)um.unmarshal(new FileReader(fileName));

            // add the new node
            NODEType node = null;
            // the first param's prefix always specifies the level for execution
            if (params[0].startsWith(PSMExecuteCommands.prefix)) {
                PSMExecuteCommands execute = PSMExecuteCommands.valueOf(
                        params[0].substring(PSMExecuteCommands.prefix.length()));
                if (execute != null) {
                    node = execute.generateNode(params);
                }
            } else if (params[0].startsWith(PeptideExecuteCommands.prefix)) {
                PeptideExecuteCommands execute = PeptideExecuteCommands.valueOf(
                        params[0].substring(PeptideExecuteCommands.prefix.length()));
                if (execute != null) {
                    node = execute.generateNode(params);
                }
            } else if (params[0].startsWith(ProteinExecuteCommands.prefix)) {
                ProteinExecuteCommands execute = ProteinExecuteCommands.valueOf(
                        params[0].substring(ProteinExecuteCommands.prefix.length()));
                if (execute != null) {
                    node = execute.generateNode(params);
                }
            }

            if (node != null) {
                execution.getPARAMETERS().getNODE().add(node);
            }

            // write the new pipeline
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.marshal(execution, new File(newFileName));
        } catch (JAXBException e) {
            logger.error("Error parsing the file " + fileName, e);
        } catch (FileNotFoundException e) {
            logger.error("Could not find the file " + fileName);
        }
    }


    /**
     * For testing purposes only.
     * @param args
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) {
        CommandLineParser parser = new GnuParser();
        Options options = new Options();

        options.addOption(OptionBuilder
                .withArgName("inputFile")
                .hasArg()
                .withDescription( "path to the used PIA XML file" )
                .create("infile"));

        options.addOption(OptionBuilder
                .withArgName("command1[:command2...]")
                .hasArg()
                .withDescription( "commands to be executed on the PSM level, " +
                        "separated by colons. If params are given to the " +
                        "command, they follow an = and are separated by " +
                        "commata (e.g. command=param1,param2...)" +
                        "\nvalid commands are: " +
                        PSMExecuteCommands.getValidCommandsString() )
                .withLongOpt("psm")
                .create("S"));

        options.addOption(OptionBuilder
                .withArgName("command1[:command2...]")
                .hasArg()
                .withDescription( "commands to be executed on the peptide " +
                        "level, separated by colons. If params are given to " +
                        "the command, they follow an = and are separated by " +
                        "commata (e.g. command=param1,param2...)" +
                        "\nvalid commands are: " +
                        PeptideExecuteCommands.getValidCommandsString() )
                .withLongOpt("peptide")
                .create("E"));

        options.addOption(OptionBuilder
                .withArgName("command1[:command2...]")
                .hasArg()
                .withDescription( "commands to be executed on the protein " +
                        "level, separated by colons. If params are given to " +
                        "the command, they follow an = and are separated by " +
                        "commata (e.g. command=param1,param2...)" +
                        "\nvalid commands are: " +
                        ProteinExecuteCommands.getValidCommandsString() )
                .withLongOpt("protein")
                .create("R"));

        options.addOption(OptionBuilder
                .withArgName("filename")
                .hasArg()
                .withDescription( "path to the parameter file, which should " +
                        "be executed, created or extended")
                .create("paramFile"));

        options.addOption(OptionBuilder
                .withArgName("filename")
                .hasArg()
                .withDescription( "Path to the parameter file, which will " +
                        "contain the newly added execution. Only used in " +
                        "combination with append. If not given, the " +
                        "paramFile will be used." )
                .create("paramOutFile"));

        options.addOption(OptionBuilder
                .withDescription( "execute the parameter file given by " +
                        "paramFile (default)" )
                .create("execute"));

        options.addOption(OptionBuilder
                .withArgName("name")
                .hasArg()
                .withDescription( "Initialize the parameter file given by " +
                        "paramFile, giving the pipeline the specified name. " +
                        "This is mainly used to build a pipeline via KNIME " +
                        "and not intended to be called on the command line." )
                .create("init"));

        options.addOption(OptionBuilder
                .withDescription( "All free arguments together are appended " +
                        "as one command to the param file. The first " +
                        "argument specifies the command with prefix (e.g. " +
                        "psm_add_filter), all following arguments are passed " +
                        "to the execution of the command. This is mainly " +
                        "used to build a pipeline via KNIME and not intended " +
                        "to be called on the command line." )
                .create("append"));

        options.addOption(OptionBuilder
                .withArgName("outfile format [fileID spectralCount]")
                .withValueSeparator(' ')
                .hasArgs(4)
                .withDescription( "Exports on the psm level. Only used in " +
                        "combination with infile and paramFile, which should " +
                        "be executed before exporting." )
                .create("psmExport"));

        options.addOption(OptionBuilder
                .withArgName("outfile format [fileID exportPSMs exportPSMSets oneAccessionPerLine]")
                .withValueSeparator(' ')
                .hasArgs(6)
                .withDescription( "Exports on the peptide level. Only used " +
                        "in combination with infile and paramFile, which " +
                        "should be executed before exporting." )
                .create("peptideExport"));

        options.addOption(OptionBuilder
                .withArgName("outfile format [exportPSMs exportPSMSets exportPeptides oneAccessionPerLine]")
                .withValueSeparator(' ')
                .hasArgs(6)
                .withDescription( "Exports on the protein level. Only used " +
                        "in combination with infile and paramFile, which " +
                        "should be executed before exporting." )
                .create("proteinExport"));

        options.addOption(OptionBuilder
                .withArgName("outfile")
                .hasArgs(1)
                .withDescription( "Write out basic information and some QC of the file." )
                .create("writeInformation"));

        options.addOption(OptionBuilder
                .withArgName("outfile")
                .hasArgs(1)
                .withDescription("Calculate some more information for the writeInformation argument (yes/no)")
                .create("calculateInformation"));


        if (args.length > 0) {
            try {
                CommandLine line = parser.parse( options, args );

                if (line.hasOption("paramFile")) {
                    String paramFile = line.getOptionValue("paramFile");
                    String paramOutFile = paramFile;

                    if (line.hasOption("init")) {
                        initialisePipelineXML(line.getOptionValue("paramFile"),
                                line.getOptionValue("init"));
                    } else if (line.hasOption("append")) {
                        if (line.hasOption("paramOutFile")) {
                            paramOutFile = line.getOptionValue("paramOutFile");
                        }

                        appendToPipelineXML(paramFile, paramOutFile,
                                line.getArgs());
                    } else {
                        if (!line.hasOption("infile")) {
                            throw new ParseException("execution of paramFile " +
                                    "requires an infile");
                        }

                        PIAModeller model =
                                new PIAModeller(line.getOptionValue("infile"));

                        processPipelineFile(line.getOptionValue("paramFile"),
                                model);

                        if (line.hasOption("writeInformation")) {
                            boolean calculateInfo = false;
                            if (line.hasOption("calculateInformation") &&
                                    (line.getOptionValue("calculateInformation").equals("yes") ||
                                            line.getOptionValue("calculateInformation").equals("true"))) {
                                calculateInfo = true;
                            }

                            String fileName = line.getOptionValue("writeInformation");
                            if ((fileName != null) && (fileName.trim().length() > 0)) {
                                model.getPSMModeller().writePSMInformation(fileName, calculateInfo);
                                // TODO: write information from other layers...
                            }
                        }

                        if (line.hasOption("psmExport")) {
                            String[] params = line.getOptionValues("psmExport");
                            List<String> paramList = new ArrayList<String>();

                            if ((params.length > 0) &&
                                    (params[0].trim().length() > 0)) {
                                paramList.add("fileName=" + params[0]);

                                if (params.length > 1) {
                                    paramList.add("format=" + params[1]);

                                    if (params.length > 2) {
                                        paramList.add("fileID=" + params[2]);

                                        if (params.length > 3) {
                                            paramList.add("spectral_count=" +
                                                    params[3]);
                                        }
                                    }

                                    PSMExecuteCommands.Export.execute(
                                            model.getPSMModeller(),
                                            paramList.toArray(params));
                                }
                            }
                        }

                        if (line.hasOption("peptideExport")) {
                            String[] params = line.getOptionValues("peptideExport");
                            List<String> paramList = new ArrayList<String>();

                            if ((params.length > 0) &&
                                    (params[0].trim().length() > 0)) {
                                paramList.add("fileName=" + params[0]);

                                if (params.length > 1) {
                                    paramList.add("format=" + params[1]);

                                    if (params.length > 2) {
                                        paramList.add("fileID=" + params[2]);

                                        if (params.length > 3) {
                                            paramList.add("exportPSMs=" +
                                                    params[3]);

                                            if (params.length > 4) {
                                                paramList.add("exportPSMSets=" +
                                                        params[4]);

                                                if (params.length > 5) {
                                                    paramList.add("oneAccessionPerLine=" +
                                                            params[5]);
                                                }
                                            }
                                        }
                                    }

                                    PeptideExecuteCommands.Export.execute(
                                            model.getPeptideModeller(),
                                            paramList.toArray(params));
                                }
                            }
                        }

                        if (line.hasOption("proteinExport")) {
                            String[] params = line.getOptionValues("proteinExport");
                            List<String> paramList = new ArrayList<String>();

                            if ((params.length > 0) &&
                                    (params[0].trim().length() > 0)) {
                                paramList.add("fileName=" + params[0]);

                                if (params.length > 1) {
                                    paramList.add("format=" + params[1]);

                                    if (params.length > 2) {
                                        paramList.add("exportPSMs=" +
                                                params[2]);

                                        if (params.length > 3) {
                                            paramList.add("exportPSMSets=" +
                                                    params[3]);

                                            if (params.length > 4) {
                                                paramList.add("exportPeptides=" +
                                                        params[4]);

                                                if (params.length > 5) {
                                                    paramList.add("oneAccessionPerLine=" +
                                                            params[5]);
                                                }
                                            }
                                        }
                                    }

                                    ProteinExecuteCommands.Export.execute(
                                            model.getProteinModeller(),
                                            paramList.toArray(params));
                                }
                            }
                        }
                    }
                } else {
                    // commands are directly processed on the command line
                    if(line.hasOption("infile")) {
                        PIAModeller model =
                                new PIAModeller(line.getOptionValue("infile"));

                        if (line.hasOption("psm")) {
                            // perform the PSM commands
                            for (String command : line.getOptionValues("psm")) {
                                PSMModeller.processCLI(model.getPSMModeller(),
                                        command.split(":"));
                            }
                        }

                        if (line.hasOption("peptide")) {
                            // perform the PSM commands
                            for (String command : line.getOptionValues("peptide")) {
                                PeptideModeller.processCLI(
                                        model.getPeptideModeller(),
                                        command.split(":"));
                            }
                        }

                        if (line.hasOption("protein")) {
                            // perform the PSM commands
                            for (String command : line.getOptionValues("protein")) {
                                ProteinModeller.processCLI(
                                        model.getProteinModeller(),
                                        command.split(":"));
                            }
                        }
                    } else {
                        System.out.println("Nothing to be done, neither" +
                                "paramFile nor infile given.");
                    }
                }
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                PIATools.printCommandLineHelp(PIAModeller.class.getSimpleName(),
                        options, helpDescription);
                System.exit(-1);
            } catch (Exception e) {
                System.err.println("Unexpected exception: " + e.getMessage());
                e.printStackTrace();
                System.exit(-1);
            }
        } else {
            PIATools.printCommandLineHelp(PIAModeller.class.getSimpleName(),
                    options, helpDescription);
        }
    }
}
