package de.mpc.pia.modeller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
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
import de.mpc.pia.tools.matomo.PIAMatomoTracker;
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
    private static final Logger LOGGER = Logger.getLogger(PIAModeller.class);

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
        LOGGER.info("start loading file " + filename);

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

        LOGGER.info(fileName + " successfully parsed.\n" +
                '\t' + intermediateHandler.getFiles().size() + " files\n" +
                '\t' + intermediateHandler.getGroups().size() + " groups\n" +
                '\t' + intermediateHandler.getAccessions().size() + " accessions\n" +
                '\t' + intermediateHandler.getPeptides().size() + " peptides\n" +
                '\t' + intermediateHandler.getPSMs().size() + " peptide spectrum matches\n" +
                '\t' + intermediateHandler.getNrTrees() + " trees");

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
     * Process a parameter pipeline file and executes the commands.<br/>
     * A parameter file is an XML file in the CTD schema (used also by OpenMS
     * and GenericKnimeNodes).
     *
     * @param paramFileName
     */
    public static void processPipelineFile(String paramFileName, PIAModeller model) {
        LOGGER.info("starting parse parameter file " + paramFileName);

        try {
            JAXBContext context = JAXBContext.newInstance(CTDTool.class);
            Unmarshaller um = context.createUnmarshaller();
            CTDTool parametersXML =
                    (CTDTool)um.unmarshal(new FileReader(paramFileName));

            for (NODEType node : parametersXML.getPARAMETERS().getNODE()) {
                processNodeInPipelineFile(model, node);
            }
        } catch (JAXBException e) {
            LOGGER.error("Error parsing the file " + paramFileName, e);
        } catch (FileNotFoundException e) {
            LOGGER.error("Could not find the file " + paramFileName, e);
        }

        LOGGER.info("finished parsing of parameter file " + paramFileName);
    }


    /**
     * Processed a single node in a parameter pipeline file and executes the
     * command.
     *
     * @param model
     * @param node
     */
    private static void processNodeInPipelineFile(PIAModeller model, NODEType node) {
        String nodeName = node.getName();

        LOGGER.debug("parsing node " + nodeName);

        if (nodeName.startsWith(PSMExecuteCommands.getPrefix())) {
            PSMExecuteCommands execute = PSMExecuteCommands.valueOf(
                    nodeName.substring(PSMExecuteCommands.getPrefix().length()));
            execute.executeXMLParameters(node, model.getPSMModeller(), model);
        } else if (nodeName.startsWith(PeptideExecuteCommands.getPrefix())) {
            PeptideExecuteCommands execute = PeptideExecuteCommands.valueOf(
                    nodeName.substring(PeptideExecuteCommands.getPrefix().length()));
            execute.executeXMLParameters(node, model.getPeptideModeller(), model);
        } else if (nodeName.startsWith(ProteinExecuteCommands.getPrefix())) {
            ProteinExecuteCommands execute = ProteinExecuteCommands.valueOf(
                    nodeName.substring(ProteinExecuteCommands.getPrefix().length()));
            execute.executeXMLParameters(node, model.getProteinModeller(), model);
        } else {
            LOGGER.error("Could not execute " + nodeName);
        }
    }


    /**
     * This method initialises a new pipeline XML file with only the name
     * given. This file then can be filled by pipeline modeling procedures and
     * finally be executed by .
     *
     * @param fileName
     */
    public static void initialisePipelineXML(String fileName, String name) {
        LOGGER.info("initialising parameter file for " + name);

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
            LOGGER.error("Error while creating file:", e);
        }

        LOGGER.info("initial parameter file written to " + fileName);
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
            if (params[0].startsWith(PSMExecuteCommands.getPrefix())) {
                PSMExecuteCommands execute = PSMExecuteCommands.valueOf(
                        params[0].substring(PSMExecuteCommands.getPrefix().length()));
                node = execute.generateNode(params);
            } else if (params[0].startsWith(PeptideExecuteCommands.getPrefix())) {
                PeptideExecuteCommands execute = PeptideExecuteCommands.valueOf(
                        params[0].substring(PeptideExecuteCommands.getPrefix().length()));
                node = execute.generateNode(params);
            } else if (params[0].startsWith(ProteinExecuteCommands.getPrefix())) {
                ProteinExecuteCommands execute = ProteinExecuteCommands.valueOf(
                        params[0].substring(ProteinExecuteCommands.getPrefix().length()));
                node = execute.generateNode(params);
            }

            if (node != null) {
                execution.getPARAMETERS().getNODE().add(node);
            }

            // write the new pipeline
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.marshal(execution, new File(newFileName));
        } catch (JAXBException e) {
            LOGGER.error("Error parsing the file " + fileName, e);
        } catch (FileNotFoundException e) {
            LOGGER.error("Could not find the file " + fileName, e);
        }
    }



    /**
     * The main method, which can be called from the command line.
     *
     * @param args
     */
    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();

        Option inputFileOpt = Option.builder(PIAConstants.INPUT_FILE_OPTION)
                .argName("inputFile")
                .hasArg()
                .desc("path to the used PIA XML file")
                .build();
        options.addOption(inputFileOpt);

        Option psmOpt = Option.builder(PIAConstants.PSM_OPTION)
                .argName(PIAConstants.COLON_COMMAND_PARAMETERS)
                .hasArg()
                .desc("commands to be executed on the PSM level, "
                        + PIAConstants.COLON_COMMAND_PARAMETERS_EXPLANATION
                        + PSMExecuteCommands.getValidCommandsString())
                .longOpt("psm")
                .build();
        options.addOption(psmOpt);

        Option peptideOpt = Option.builder(PIAConstants.PEPTIDE_OPTION)
                .argName(PIAConstants.COLON_COMMAND_PARAMETERS)
                .hasArg()
                .desc("commands to be executed on the peptide level, "
                        + PIAConstants.COLON_COMMAND_PARAMETERS_EXPLANATION
                        + PeptideExecuteCommands.getValidCommandsString())
                .longOpt("peptide")
                .build();
        options.addOption(peptideOpt);

        Option proteinOpt = Option.builder(PIAConstants.PROTEIN_OPTION)
                .argName(PIAConstants.COLON_COMMAND_PARAMETERS)
                .hasArg()
                .desc("commands to be executed on the protein level, "
                        + PIAConstants.COLON_COMMAND_PARAMETERS_EXPLANATION
                        + ProteinExecuteCommands.getValidCommandsString())
                .longOpt("protein")
                .build();
        options.addOption(proteinOpt);

        Option paramFileOpt = Option.builder(PIAConstants.PARAM_FILE_OPTION)
                .argName("filename")
                .hasArg()
                .desc("path to the parameter file, which should be executed, created or extended")
                .build();
        options.addOption(paramFileOpt);

        Option paramOutFileOpt = Option.builder(PIAConstants.PARAM_OUT_FILE_OPTION)
                .argName("filename")
                .hasArg()
                .desc("Path to the parameter file, which will contain the newly added execution. Only used in "
                        + "combination with append. If not given, the paramFile will be used.")
                .build();
        options.addOption(paramOutFileOpt);

        Option executeOpt = Option.builder("execute")
                .desc("execute the parameter file given by paramFile (default)")
                .build();
        options.addOption(executeOpt);

        Option initOpt = Option.builder(PIAConstants.INIT_OPTION)
                .argName("name")
                .hasArg()
                .desc("Initialize the parameter file given by paramFile, giving the pipeline the specified name. This "
                        + "was mainly used to build a pipeline via KNIME and not intended to be called on the command "
                        + "line." )
                .build();
        options.addOption(initOpt);

        Option appendOpt = Option.builder(PIAConstants.APPEND_OPTION)
                .desc("All free arguments together are appended as one command to the param file. The first argument "
                        + "specifies the command with prefix (e.g. psm_add_filter), all following arguments are passed "
                        + "to the execution of the command. This is mainly used to build a pipeline via KNIME and not "
                        + "intended to be called on the command line." )
                .build();
        options.addOption(appendOpt);

        Option psmExportOpt = Option.builder(PIAConstants.PSM_EXPORT_OPTION)
                .argName("outfile format [fileID=ID] [spectralCount=true/false]")
                .valueSeparator(' ')
                .hasArg()
                .optionalArg(true)
                .numberOfArgs(4)
                .desc("Exports on the psm level. Only used in combination with infile and paramFile, which should be "
                        + "executed before exporting.")
                .build();
        options.addOption(psmExportOpt);

        Option peptideExportOpt = Option.builder(PIAConstants.PEPTIDE_EXPORT_OPTION)
                .argName("outfile format [fileID=ID] [exportPSMs=true/false] [exportPSMSets=true/false] "
                        + "[oneAccessionPerLine=true/false]")
                .valueSeparator(' ')
                .hasArg()
                .optionalArg(true)
                .numberOfArgs(6)
                .desc("Exports on the peptide level. Only used in combination with infile and paramFile, which should "
                        + "be executed before exporting." )
                .build();
        options.addOption(peptideExportOpt);

        Option proteinExportOpt = Option.builder(PIAConstants.PROTEIN_EXPORT_OPTION)
                .argName("outfile format [exportPSMs=true/false] [exportPSMSets=true/false] "
                        + "[exportPeptides=true/false] [oneAccessionPerLine=true/false] "
                        + "[exportProteinSequences=true/false]")
                .valueSeparator(' ')
                .hasArg()
                .optionalArg(true)
                .numberOfArgs(7)
                .desc( "Exports on the protein level. Only used in combination with infile and paramFile, which should "
                        + "be executed before exporting." )
                .build();
        options.addOption(proteinExportOpt);

        Option writeInfoOpt = Option.builder(PIAConstants.WRITE_INFORMATION_OPTION)
                .argName("outfile")
                .numberOfArgs(1)
                .desc("Write out basic information and some QC of the file.")
                .build();
        options.addOption(writeInfoOpt);

        Option calculateInfoOpt = Option.builder(PIAConstants.CALCULATE_INFORMATION_OPTION)
                .argName("outfile")
                .numberOfArgs(1)
                .desc("Calculate some more information for the writeInformation argument (yes/no)")
                .build();
        options.addOption(calculateInfoOpt);

        Option disableUsageStatisticsOpt = Option.builder("disableUsageStatistics")
                .hasArg(false)
                .desc("set this option to disable the collection of usage statistics for quality control and"
                        + "funding purposes")
                .build();
        options.addOption(disableUsageStatisticsOpt);


        if (args.length > 0) {
            try {
                CommandLine line = parser.parse( options, args );

                PIAMatomoTracker.disableTracking(line.hasOption(disableUsageStatisticsOpt.getOpt()));

                if (line.hasOption(paramFileOpt.getOpt())) {
                    // commands processed from an XML file
                    PIAMatomoTracker.trackPIAEvent(PIAMatomoTracker.PIA_TRACKING_COMMAND_LINE_CATEGORY,
                            PIAMatomoTracker.PIA_TRACKING_MODELLER_NAME,
                            PIAMatomoTracker.PIA_TRACKING_MODELLER_XML_STARTED, null);
                    parseParameterXMLFile(line);
                } else {
                    // commands are directly processed on the command line
                    PIAMatomoTracker.trackPIAEvent(PIAMatomoTracker.PIA_TRACKING_COMMAND_LINE_CATEGORY,
                            PIAMatomoTracker.PIA_TRACKING_MODELLER_NAME,
                            PIAMatomoTracker.PIA_TRACKING_MODELLER_CLI_STARTED, null);
                    parseCommandsFromCommandLine(line);
                }
                PIAMatomoTracker.trackPIAEvent(PIAMatomoTracker.PIA_TRACKING_COMMAND_LINE_CATEGORY,
                        PIAMatomoTracker.PIA_TRACKING_MODELLER_NAME,
                        PIAMatomoTracker.PIA_TRACKING_MODELLER_FINISHED, null);
            } catch (ParseException e) {
                LOGGER.error("Error parsing command line", e);
                PIATools.printCommandLineHelp(PIAModeller.class.getSimpleName(),
                        options, PIAConstants.HELP_DESCRIPTION);
                System.exit(-1);
            } catch (Exception e) {
                LOGGER.error("Error while executing " + PIAModeller.class.getSimpleName(), e);
                PIAMatomoTracker.trackPIAEvent(PIAMatomoTracker.PIA_TRACKING_COMMAND_LINE_CATEGORY,
                        PIAMatomoTracker.PIA_TRACKING_MODELLER_NAME,
                        PIAMatomoTracker.PIA_TRACKING_MODELLER_ERROR, null);
                System.exit(-1);
            }
        } else {
            PIATools.printCommandLineHelp(PIAModeller.class.getSimpleName(),
                    options, PIAConstants.HELP_DESCRIPTION);
        }
    }


    /**
     * Processed the commands from an XML parameters file
     *
     * @param line
     */
    private static void parseParameterXMLFile(CommandLine line) {
        String paramFile = line.getOptionValue(PIAConstants.PARAM_FILE_OPTION);

        if (line.hasOption(PIAConstants.INIT_OPTION)) {
            initialisePipelineXML(line.getOptionValue(PIAConstants.PARAM_FILE_OPTION),
                    line.getOptionValue(PIAConstants.INIT_OPTION));
        } else if (line.hasOption(PIAConstants.APPEND_OPTION)) {
            String paramOutFile = paramFile;
            if (line.hasOption(PIAConstants.PARAM_OUT_FILE_OPTION)) {
                paramOutFile = line.getOptionValue(PIAConstants.PARAM_OUT_FILE_OPTION);
            }

            appendToPipelineXML(paramFile, paramOutFile, line.getArgs());
        } else {
            // so the default EXECUTE_OPTION will be performed
            if (!line.hasOption(PIAConstants.INPUT_FILE_OPTION)) {
                LOGGER.error("execution of paramFile requires an infile");
            } else {
                processExecuteXMLFile(line.getOptionValue(PIAConstants.PARAM_FILE_OPTION), line);
            }
        }
    }


    /**
     * execute an parameter XML file from the command line
     *
     * @param line
     */
    private static void processExecuteXMLFile(String paramFileName, CommandLine line) {
        try {
            PIAModeller model = new PIAModeller(line.getOptionValue(PIAConstants.INPUT_FILE_OPTION));
            processPipelineFile(paramFileName, model);

            if (line.hasOption(PIAConstants.WRITE_INFORMATION_OPTION)) {
                processWriteInformation(line.hasOption(PIAConstants.CALCULATE_INFORMATION_OPTION) ? line.getOptionValue(PIAConstants.CALCULATE_INFORMATION_OPTION) : null,
                        line.getOptionValue(PIAConstants.WRITE_INFORMATION_OPTION), model);
            }

            if (line.hasOption(PIAConstants.PSM_EXPORT_OPTION)) {
                String[] params = line.getOptionValues(PIAConstants.PSM_EXPORT_OPTION);
                processPSMExport(params, model);
            }

            if (line.hasOption(PIAConstants.PEPTIDE_EXPORT_OPTION)) {
                String[] params = line.getOptionValues(PIAConstants.PEPTIDE_EXPORT_OPTION);
                processPeptideExport(params, model);
            }

            if (line.hasOption(PIAConstants.PROTEIN_EXPORT_OPTION)) {
                String[] params = line.getOptionValues(PIAConstants.PROTEIN_EXPORT_OPTION);
                processProteinExport(params, model);
            }
        } catch (Exception e) {
            LOGGER.error("Error while processing XML parameter file", e);
        }
    }


    /**
     * Write the PSM information to the given file
     *
     * @param calculateInformationOption if null, no information will be calculated, otherwise it may be "yes" or "no"
     * @param informationFileName filename, where the information will be saved
     * @param model
     */
    private static void processWriteInformation(String calculateInformationOption, String informationFileName,
            PIAModeller model) {
        boolean calculateInfo = false;
        if (("yes".equalsIgnoreCase(calculateInformationOption)
                || "true".equalsIgnoreCase(calculateInformationOption))) {
            calculateInfo = true;
        }

        if ((informationFileName != null) && !informationFileName.trim().isEmpty()) {
            model.getPSMModeller().writePSMInformation(informationFileName, calculateInfo);
            // TODO: write information from other layers...
        }
    }


    /**
     * Process PSM export from command line params.
     *
     * @param params the maximal four splitted params (fileName, format, fileID and spectralCount)
     * @param model
     */
    private static void processPSMExport(String[] params, PIAModeller model) {
        List<String> paramList = processExportOptions(params);

        if (paramList.size() >= 2) {
            PSMExecuteCommands.Export.execute(
                    model.getPSMModeller(),
                    model,
                    paramList.toArray(params));
        }
    }

    /**
     * Process peptide level export from the separated command line params.
     *
     * @param params
     * @param model
     */
    private static void processPeptideExport(String[] params, PIAModeller model) {
        List<String> paramList = processExportOptions(params);

        if (paramList.size() >= 2) {
            PeptideExecuteCommands.Export.execute(
                    model.getPeptideModeller(),
                    model,
                    paramList.toArray(params));
        }
    }


    /**
     * Process protein level export from the separated command line params.
     *
     * @param params
     * @param model
     */
    private static void processProteinExport(String[] params, PIAModeller model) {
        List<String> paramList = processExportOptions(params);

        if (paramList.size() >= 2) {
            ProteinExecuteCommands.Export.execute(
                    model.getProteinModeller(),
                    model,
                    paramList.toArray(params));
        }
    }


    /**
     * Processes the params for export. The first two are mandatory without key, these are the fileName and the format,
     * in this order. All others are added as they are.
     *
     * @param params
     * @return
     */
    private static List<String> processExportOptions(String[] params) {
        List<String> paramList = new ArrayList<>();

        if (params.length >= 2) {
            paramList.add(PIAConstants.FILE_NAME_PARAM + params[0]);
            paramList.add(PIAConstants.FORMAT_PARAM + params[1]);

            if (params.length > 2) {
                paramList.addAll(Arrays.asList(params).subList(2, params.length));
            }
        }

        return paramList;
    }


    /**
     * Parses the parameters directly from the command line, i.e. no paremeters
     * XMl file was given.
     *
     * @param line
     */
    private static void parseCommandsFromCommandLine(CommandLine line) {
        if (!line.hasOption(PIAConstants.INPUT_FILE_OPTION)) {
            LOGGER.warn("Nothing to be done, neither paramFile nor infile given.");
            return;
        }

        try {
            PIAModeller model = new PIAModeller(line.getOptionValue(PIAConstants.INPUT_FILE_OPTION));

            if (line.hasOption(PIAConstants.PSM_OPTION)) {
                // perform the PSM commands
                for (String command : line.getOptionValues(PIAConstants.PSM_OPTION)) {
                    PSMModeller.processCLI(
                            model.getPSMModeller(),
                            model,
                            command.split(":"));
                }
            }

            if (line.hasOption(PIAConstants.PEPTIDE_OPTION)) {
                // perform the PSM commands
                for (String command : line.getOptionValues(PIAConstants.PEPTIDE_OPTION)) {
                    PeptideModeller.processCLI(
                            model.getPeptideModeller(),
                            model,
                            command.split(":"));
                }
            }

            if (line.hasOption(PIAConstants.PROTEIN_OPTION)) {
                // perform the PSM commands
                for (String command : line.getOptionValues(PIAConstants.PROTEIN_OPTION)) {
                    ProteinModeller.processCLI(
                            model.getProteinModeller(),
                            model,
                            command.split(":"));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while processing parameters directly from command line", e);
        }
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
        LOGGER.info("Serializing data to " + file.getAbsolutePath());
        try (FileOutputStream fos = new FileOutputStream(file);
                GZIPOutputStream gzo = new GZIPOutputStream(fos);
                ObjectOutputStream oos = new ObjectOutputStream(gzo)) {
            oos.writeObject(piaModeller);
        } catch (IOException e) {
            LOGGER.error("Could not write whole PIA model to " + file.getAbsolutePath(), e);
            throw e;
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
        LOGGER.info("reading modeller from " + file.getAbsolutePath());

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
            LOGGER.error("Could not read PIA model from " + file.getAbsolutePath(), e);
            throw e;
        } catch (ClassNotFoundException e) {
            String msg = "Could not read PIA model from " + file.getAbsolutePath();
            LOGGER.error(msg, e);
            throw new IOException(msg, e);
        }

        return piaModeller;
    }
}
