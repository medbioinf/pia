package de.mpc.pia;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.PIASimpleCompiler;
import de.mpc.pia.tools.matomo.PIAMatomoTracker;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Class to process PIA execution via command line interface (CLI) 
 * 
 * @author julianu
 *
 */
@Command(name = "pia", mixinStandardHelpOptions = true, 
	description = "PIA - Protein Inference Algorithms.")
public class PIACli implements Runnable{
	
    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(PIACli.class);

    
    @Option(names = { "-c", "--compile" },
    		description = "perform a compilation, otherwise perform analysis") 
    boolean compile = false;
    
    @Option(names = { "--processExample" },
    		description = "returns an example json for a PIA analysis") 
    boolean processExample = false;
    
    @Option(names = { "-o", "--outfile" },
    		description = "output file name (e.g. intermediate PIA file)",
    		required = true) 
    private String outfile;

    @Option(names = { "-n", "--name" },
    		description = "name of the compilation",
    		defaultValue = "PIA compilation") 
    private String name;
    
    @Option(names = { "--doNotTrack" },
    		description = "set this option to disable the collection of usage statistics for"
    				+ "quality control and funding purposes")
    private boolean doNotTrack;

    @Parameters(paramLabel = "<infile>",
            description = "input file(s): search results for the compilation, json for analysis."
            		+ "For the search results, possible further information can be passed, separated"
            		+ "by semicolon. The information is in this order:"
            + "\n\tname of the input file (if not given will be set to the path of the input file),"
            + "\n\ttype of the file (usually guessed, but may also be explicitly given, possible values are e.g. mzid, xtandem),"
            + "\n\tadditional information file (very seldom used)")
    private String[] infiles; 

    
    /**
     * Performs the actual work of the CLI call
     */
    @Override
    public void run() { 
    	if (compile) {
    		processCompile();
    	} else if (processExample) {
    		processExample();
    	}
    }
    

    /**
     * The main method, which can be called from the command line.
     *
     * @param args
     */
    public static void main(String[] args) {
    	int exitCode = new CommandLine(new PIACli()).execute(args); 
        System.exit(exitCode); 
    }

    
    /**
     * Processes the compilation called by the CLI
     */
    private void processCompile() {
    	
    	PIACompiler piaCompiler = new PIASimpleCompiler();
    	
        // parse the command line arguments
        try {
            PIAMatomoTracker.disableTracking(doNotTrack);

            PIAMatomoTracker.trackPIAEvent(
            		PIAMatomoTracker.PIA_TRACKING_COMMAND_LINE_CATEGORY,
                    PIAMatomoTracker.PIA_TRACKING_COMPILER_NAME,
                    PIAMatomoTracker.PIA_TRACKING_COMPILER_STARTED,
                    null);
            
            if (!parseCommandLineInfiles(piaCompiler)) {
            	return;
            }

            piaCompiler.buildClusterList();
            piaCompiler.buildIntermediateStructure();

            piaCompiler.setName(name);

            // now write out the file
            piaCompiler.writeOutXML(outfile);
            piaCompiler.finish();
            
            PIAMatomoTracker.trackPIAEvent(
            		PIAMatomoTracker.PIA_TRACKING_COMMAND_LINE_CATEGORY,
                    PIAMatomoTracker.PIA_TRACKING_COMPILER_NAME,
                    PIAMatomoTracker.PIA_TRACKING_COMPILER_FINISHED,
                    null);
        } catch (IOException e) {
            LOGGER.error("Error while writing PIA XML file.", e);
            
            PIAMatomoTracker.trackPIAEvent(
            		PIAMatomoTracker.PIA_TRACKING_COMMAND_LINE_CATEGORY,
                    PIAMatomoTracker.PIA_TRACKING_COMPILER_NAME,
                    PIAMatomoTracker.PIA_TRACKING_COMPILER_ERROR,
                    null);
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
    private boolean parseCommandLineInfiles(PIACompiler piaCompiler) {
    	boolean parsedOk = true;
    	
        for (String inputFile : infiles) {
            parsedOk |= parseCommandLineInfile(inputFile, piaCompiler);
        }

        return parsedOk;
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
    private boolean parseCommandLineInfile(String inputFile, PIACompiler piaCompiler) {
        String[] values = inputFile.split(";");
        String file = values[0];
        String compilationFileName = values[0];
        String additionalInfoFile = null;
        String type = null;

        if (values.length == 1) {
            if (file.contains(File.separator)) {
                // take the filename-only as name, if none is given
            	compilationFileName = new File(file).getName();
            }
        } else {
        	compilationFileName = !values[1].trim().isEmpty() ? values[1].trim() : null;
        	
            type = ((values.length > 2) && !values[2].trim().isEmpty()) ? values[2].trim() : null;
            additionalInfoFile = ((values.length > 3) && !values[3].trim().isEmpty()) ? values[3].trim() : null;
        }

        return piaCompiler.getDataFromFile(compilationFileName, file, additionalInfoFile, type);
    }
    
    
    /**
     * Just returns an example json for the PIA analysis
     */
    private void processExample() {
    	
    	
    	
    }

}
