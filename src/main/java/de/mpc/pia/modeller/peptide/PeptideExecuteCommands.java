package de.mpc.pia.modeller.peptide;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import de.mpc.pia.modeller.PeptideModeller;
import de.mpc.pia.modeller.execute.ExecuteModelCommands;
import de.mpc.pia.modeller.execute.xmlparams.ITEMType;
import de.mpc.pia.modeller.execute.xmlparams.NODEType;
import de.mpc.pia.modeller.execute.xmlparams.PossibleITEMType;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;


/**
 * This enum holds the command line interface executioners for the peptide
 * modeller.
 *
 * @author julian
 *
 */
public enum PeptideExecuteCommands implements ExecuteModelCommands<PeptideModeller> {

    ConsiderModifications {
        /** the identification string for considering modifications */
        private static final String idConsiderMods = "consider modifications";

        @Override
        public boolean execute(PeptideModeller modeller, String[] params) {
            logger.info("execute CLI command " + name());

            Boolean considerModifications = null;

            if ((params != null) && (params.length > 0)) {
                if (params[0] != null) {
                    if (params[0].equals("true") || params[0].equals("yes")) {
                        considerModifications = true;
                    } else {
                        considerModifications = false;
                    }
                }
            }

            if (considerModifications == null) {
                // just giving the flag is considered as setting it true
                considerModifications = true;
            }

            modeller.setConsiderModifications(considerModifications);
            return true;
        }

        @Override
        public String describe() {
            return "Sets whether modifications should be considered while " +
                    "inferring the peptides from the PSMs. Defaults to " +
                    PeptideModeller.considerModificationsDefault;
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            List<List<String>> params = new ArrayList<List<String>>();

            List<String> param = new ArrayList<String>();
            param.add(idConsiderMods);
            param.add("no");
            param.add("yes");
            params.add(param);

            return params;
        }

        @Override
        public void executeXMLParameters(NODEType node, PeptideModeller model) {
            String considerMods = null;

            for (Object item : node.getITEMOrITEMLISTOrNODE()) {
                if (item instanceof ITEMType) {
                    if (idConsiderMods.equals(((ITEMType) item).getName())) {
                        considerMods = ((ITEMType) item).getValue();
                    }
                }
            }

            execute(model, new String[] {considerMods});
        }
    },


    AddFilter {
        /** the identification string for the file ID */
        private static final String idFileID= "file ID";

        /** the identification string for the filter name */
        private static final String idFiltername= "filtername";

        /** the identification string for negate */
        private static final String idNegate= "negate";

        /** the identification string for the equation */
        private static final String idComparison= "comparison";

        /** the identification string for the value */
        private static final String idValue= "value";

        @Override
        public boolean execute(PeptideModeller modeller, String[] params) {
            logger.info("execute CLI command " + name());

            boolean negate = false;

            if (params.length >= 5) {
                if (params[4] != null &&
                        (params[4].equals("true") || params[4].equals("yes"))) {
                    negate = true;
                }
            }

            if (params.length >= 4) {
                String fileID = params[0];
                String filtername = params[1];
                String comparison = params[2];
                String value = params[3];

                StringBuffer messageBuffer = new StringBuffer();

                AbstractFilter newFilter =
                        FilterFactory.newInstanceOf(
                                filtername,
                                FilterComparator.getFilterComparatorByCLI(
                                        comparison),
                                value,
                                negate,
                                messageBuffer);

                if (newFilter != null) {
                    modeller.addFilter(Long.parseLong(fileID), newFilter);
                } else {
                    logger.error("Filter " + filtername +
                            " could not be added: " + messageBuffer.toString());
                }
            } else {
                logger.info("Too few parameters to execute " + name() +
                        ", ignoring the call");
            }

            return true;
        }

        @Override
        public String describe() {
            return "Adds a peptide level filter to a specified file. The file " +
                    "is given by the first parameter, which has to be the file " +
                    "ID. Filters are added by their name, an abbreviation " +
                    "for the camparison, the compared value and (optional), " +
                    "whether the comparison should be negated " +
                    "e.g. \"" + name() + "=1,charge_filter,EQ,2,no\". \n" +
                    "Registered peptide filters are: " +
                    RegisteredFilters.getPeptideFilterShortsForHelp();
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            List<List<String>> params = new ArrayList<List<String>>();

            List<String> param = new ArrayList<String>();
            param.add(idFileID);
            params.add(param);

            param = new ArrayList<String>();
            param.add(idFiltername);
            params.add(param);

            param = new ArrayList<String>();
            param.add(idComparison);
            for (FilterComparator comp : FilterComparator.values()) {
                param.add(comp.getCliShort());
            }
            params.add(param);

            param = new ArrayList<String>();
            param.add(idValue);
            params.add(param);

            param = new ArrayList<String>();
            param.add(idNegate);
            param.add("no");
            param.add("yes");
            params.add(param);

            return params;
        }

        @Override
        public void executeXMLParameters(NODEType node, PeptideModeller model) {
            String fileID = null;
            String filtername = null;
            String comparison = null;
            String value = null;
            String negate = null;

            for (Object item : node.getITEMOrITEMLISTOrNODE()) {
                if (item instanceof ITEMType) {
                    if (idFileID.equals(((ITEMType) item).getName())) {
                        fileID = ((ITEMType) item).getValue();
                    } else if (idFiltername.equals(((ITEMType) item).getName())) {
                        filtername = ((ITEMType) item).getValue();
                    } else if (idComparison.equals(((ITEMType) item).getName())) {
                        comparison = ((ITEMType) item).getValue();
                    } else if (idValue.equals(((ITEMType) item).getName())) {
                        value = ((ITEMType) item).getValue();
                    } else if (idNegate.equals(((ITEMType) item).getName())) {
                        negate = ((ITEMType) item).getValue();
                    }
                }
            }

            execute(model, new String[] {fileID, filtername, comparison, value,
                    negate});
        }
    },


    Export {
        @Override
        public boolean execute(PeptideModeller modeller, String[] params) {
            logger.info("execute CLI command " + name());

            String format = "csv";
            Long fileID = 0L;
            String fileName = "report.peptide.csv";
            boolean oneAccessionPerLine = false;
            boolean exportPSMSets = false;
            boolean exportPSMs = false;

            Pattern pattern = Pattern.compile("^([^=]+)=(.*)");
            Matcher commandParamMatcher;

            for (String command : params) {
                String[] commandParams = null;
                commandParamMatcher = pattern.matcher(command);

                if (commandParamMatcher.matches()) {
                    command = commandParamMatcher.group(1);
                    commandParams = commandParamMatcher.group(2).split(";");
                }

                if ("format".equals(command)) {
                    if ((commandParams != null) &&
                            (commandParams.length > 0)) {
                        format = commandParams[0];
                    }
                } else if ("fileID".equals(command)) {
                    if ((commandParams != null) &&
                            (commandParams.length > 0)) {
                        fileID = Long.parseLong(commandParams[0]);
                    }
                } else if ("fileName".equals(command)) {
                    if ((commandParams != null) &&
                            (commandParams.length > 0)) {
                        fileName = commandParams[0];
                    }
                } else if ("oneAccessionPerLine".equals(command)) {
                    if ((commandParams != null) &&
                            (commandParams.length > 0)) {
                        if (commandParams[0].equals("yes") ||
                                commandParams[0].equals("true")) {
                            oneAccessionPerLine = true;
                        } else {
                            oneAccessionPerLine = false;
                        }
                    } else {
                        // only setting the flag is equivalent to true
                        oneAccessionPerLine = true;
                    }
                } else if ("exportPSMSets".equals(command)) {
                    if ((commandParams != null) &&
                            (commandParams.length > 0)) {
                        if (commandParams[0].equals("yes") ||
                                commandParams[0].equals("true")) {
                            exportPSMSets = true;
                        } else {
                            exportPSMSets = false;
                        }
                    } else {
                        // only setting the flag is equivalent to true
                        exportPSMSets = true;
                    }
                } else if ("exportPSMs".equals(command)) {
                    if ((commandParams != null) &&
                            (commandParams.length > 0)) {
                        if (commandParams[0].equals("yes") ||
                                commandParams[0].equals("true")) {
                            exportPSMs = true;
                        } else {
                            exportPSMs = false;
                        }
                    } else {
                        // only setting the flag is equivalent to true
                        exportPSMs = true;
                    }
                }
            }

            logger.info("export parameters: " +
                    "filename: " + fileName +
                    ", fileID: " + fileID +
                    ", format: " + format +
                    ", oneAccessionPerLine: " + oneAccessionPerLine +
                    ", exportPSMSets: " + exportPSMSets +
                    ", exportPSMs:" + exportPSMs);

            Writer writer = null;
            try {
                writer = new FileWriter(fileName, false);

                if ((format == null) ||
                        (format.trim().length() == 0) ||
                        (format.equalsIgnoreCase("csv"))) {
                    modeller.exportCSV(writer, fileID, true,
                            oneAccessionPerLine, exportPSMSets, exportPSMs);
                }
            } catch (IOException e) {
                logger.error("Cannot write to file " + fileName, e);
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        logger.error("Cannot close file " + fileName, e);
                    }
                }
            }

            return true;
        }

        @Override
        public String describe() {
            return "Exports the report. " +
                    "Additional parameters may be passed semicolon " +
                    "separated with the syntax param=arg[;arg2;...]." +
                    "valid parameters are:" +
                    "\nformat: csv [default]" +
                    "\nfileID: default 0 (overview)" +
                    "\nfileName: the report file name [report.peptide.csv]" +
                    "\noneAccessionPerLine: write one accession per line " +
                    "(useful for spectral counting), defaults to false" +
                    "\nexportPSMSets: defaults to false" +
                    "\nexportPSMs: defaults to false";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            // this is not executable via XML file
            return null;
        }

        @Override
        public void executeXMLParameters(NODEType node, PeptideModeller model) {
            // this is not executable via XML file
        }
    }
    ;


    /** logger for this enum */
    private static final Logger logger = Logger.getLogger(PeptideExecuteCommands.class);

    /** the prefix for this level's execute commands */
    public final static String prefix = "Peptide";


    @Override
    public NODEType generateNode(String params[]) {
        if (params.length < 1) {
            return null;
        }

        String execution = params[0];
        if (execution.startsWith(prefix)) {
            execution = execution.substring(prefix.length());
        }
        if (!name().equals(execution)) {
            logger.error(name() + " is the wrong execute command for " +
                    execution);
            return null;
        }

        NODEType node = new NODEType();
        node.setName(prefix + execution);
        node.setDescription(describe());

        int pos = 0;
        List<List<String>> neededParams = neededXMLParameters();
        for (List<String> paramList : neededParams) {
            ITEMType item = new ITEMType();
            item.setName(paramList.get(0));
            item.setType(PossibleITEMType.STRING);
            pos++;
            if (params.length > pos) {
                String value = params[pos];
                item.setValue(value);
            }
            node.getITEMOrITEMLISTOrNODE().add(item);
        }

        return node;
    }


    @Override
    public String prefix() {
        return prefix;
    }


    /**
     * Gives a string representation of the valid commands for this enumeration
     * @return
     */
    public static String getValidCommandsString() {
        StringBuffer sb = new StringBuffer();

        for (PeptideExecuteCommands command : values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(command.name());
        }

        return sb.toString();
    }
}
