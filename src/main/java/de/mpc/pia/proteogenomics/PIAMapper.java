package de.mpc.pia.proteogenomics;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.PIASimpleCompiler;
import de.mpc.pia.intermediate.compiler.parser.InputFileParserFactory;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.peptide.PeptideExecuteCommands;
import de.mpc.pia.modeller.protein.ProteinExecuteCommands;
import de.mpc.pia.modeller.psm.PSMExecuteCommands;
import de.mpc.pia.tools.PIAConstants;
import de.mpc.pia.tools.PIATools;
import de.mpc.pia.tools.matomo.PIAMatomoTracker;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * This code is licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * ==Overview==
 * <p>

 This tool helps to map all the peptides from an import file into a fasta file and perform the protein inference. This tool can be used to map old identifications files into new versions of databases such as uniprot or ensembl.

 *
 * <p>
 * Created by ypriverol (ypriverol@gmail.com) on 24/05/2018.
 */
public class PIAMapper {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(PIAPogo.class);

    /**
     * The main method, which can be called from the command line.
     *
     * @param args
     */
    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();


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


        if (args.length > 0) {
            try {
                CommandLine line = parser.parse( options, args );

                PIAMatomoTracker.disableTracking(line.hasOption(disableUsageStatisticsOpt.getOpt()));

                if (line.hasOption(paramFileOpt.getOpt())) {
                    // commands processed from an XML file
                    PIAMatomoTracker.trackPIAEvent(PIAMatomoTracker.PIA_TRACKING_COMMAND_LINE_CATEGORY,
                            PIAMatomoTracker.PIA_TRACKING_MODELLER_NAME,
                            PIAMatomoTracker.PIA_TRACKING_MODELLER_XML_STARTED, null);
                    //parseParameterXMLFile(line);
                } else {
                    // commands are directly processed on the command line
                    PIAMatomoTracker.trackPIAEvent(PIAMatomoTracker.PIA_TRACKING_COMMAND_LINE_CATEGORY,
                            PIAMatomoTracker.PIA_TRACKING_MODELLER_NAME,
                            PIAMatomoTracker.PIA_TRACKING_MODELLER_CLI_STARTED, null);
                    //parseCommandsFromCommandLine(line);
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
}
