package de.mpc.pia.modeller.peptide;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import de.mpc.pia.modeller.PIAModeller;
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
        private static final String ID_CONSIDER_MODS = "consider modifications";

        @Override
        public boolean execute(PeptideModeller peptideModeller, PIAModeller piaModeller, String[] params) {
            LOGGER.info(LOGGING_PREAMBEL + name());

            Boolean considerModifications = null;

            if ((params != null)
                    && (params.length > 0)
                    && (params[0] != null)) {
                considerModifications = "true".equals(params[0]) || "yes".equals(params[0]);
            }

            if (considerModifications == null) {
                // just giving the flag is considered as setting it true
                considerModifications = true;
            }

            peptideModeller.setConsiderModifications(considerModifications);
            return true;
        }

        @Override
        public String describe() {
            return "Sets whether modifications should be considered while"
                    + " inferring the peptides from the PSMs. Defaults to " +
                    PeptideModeller.CONSIDER_MODIFICATIONS_DEFAULT;
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            List<List<String>> params = new ArrayList<>();

            List<String> param = new ArrayList<>();
            param.add(ID_CONSIDER_MODS);
            param.add("no");
            param.add("yes");
            params.add(param);

            return params;
        }

        @Override
        public void executeXMLParameters(NODEType node, PeptideModeller peptideModeller, PIAModeller piaModeller) {
            String considerMods = null;

            for (Object item : node.getITEMOrITEMLISTOrNODE()) {
                if ((item instanceof ITEMType)
                        && ID_CONSIDER_MODS.equals(((ITEMType) item).getName())) {
                    considerMods = ((ITEMType) item).getValue();
                }
            }

            execute(peptideModeller, piaModeller, new String[] {considerMods});
        }
    },


    AddFilter {
        /** the identification string for the file ID */
        private static final String ID_FILE_ID= "file ID";

        /** the identification string for the filter NAME */
        private static final String ID_FILTERNAME= "filtername";

        /** the identification string for negate */
        private static final String ID_NEGATE= "negate";

        /** the identification string for the equation */
        private static final String ID_COMPARISON= "comparison";

        /** the identification string for the value */
        private static final String ID_VALUE= "value";

        @Override
        public boolean execute(PeptideModeller peptideModeller, PIAModeller piaModeller, String[] params) {
            LOGGER.info(LOGGING_PREAMBEL + name());

            boolean negate = false;

            if ((params.length >= 5)
                    && (params[4] != null)
                    && ("true".equals(params[4]) || "yes".equals(params[4]))) {
                negate = true;
            }

            if (params.length >= 4) {
                String fileID = params[0];
                String filtername = params[1];
                String comparison = params[2];
                String value = params[3];

                StringBuilder messageBuffer = new StringBuilder();

                AbstractFilter newFilter =
                        FilterFactory.newInstanceOf(
                                filtername,
                                FilterComparator.getFilterComparatorByCLI(
                                        comparison),
                                value,
                                negate,
                                messageBuffer);

                if (newFilter != null) {
                    peptideModeller.addFilter(Long.parseLong(fileID), newFilter);
                } else {
                    LOGGER.error("Filter " + filtername
                            + " could not be added: " + messageBuffer.toString());
                }
            } else {
                LOGGER.info("Too few parameters to execute " + name()
                        + ", ignoring the call");
            }

            return true;
        }

        @Override
        public String describe() {
            return "Adds a peptide level filter to a specified file. The file"
                    + " is given by the first parameter, which has to be the"
                    + " file ID. Filters are added by their NAME, an"
                    + " abbreviation for the camparison, the compared value and"
                    + " (optional), whether the comparison should be negated,"
                    + " e.g. \"" + name() + "=1,charge_filter,EQ,2,no\". \n"
                    + "Registered peptide filters are: " + RegisteredFilters.getPeptideFilterShortsForHelp();
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            List<List<String>> params = new ArrayList<>();

            List<String> param = new ArrayList<>();
            param.add(ID_FILE_ID);
            params.add(param);

            param = new ArrayList<>();
            param.add(ID_FILTERNAME);
            params.add(param);

            param = new ArrayList<>();
            param.add(ID_COMPARISON);
            for (FilterComparator comp : FilterComparator.values()) {
                param.add(comp.getCliShort());
            }
            params.add(param);

            param = new ArrayList<>();
            param.add(ID_VALUE);
            params.add(param);

            param = new ArrayList<>();
            param.add(ID_NEGATE);
            param.add("no");
            param.add("yes");
            params.add(param);

            return params;
        }

        @Override
        public void executeXMLParameters(NODEType node, PeptideModeller peptideModeller, PIAModeller piaModeller) {
            String fileID = null;
            String filtername = null;
            String comparison = null;
            String value = null;
            String negate = null;

            for (Object item : node.getITEMOrITEMLISTOrNODE()) {
                if (item instanceof ITEMType) {
                    if (ID_FILE_ID.equals(((ITEMType) item).getName())) {
                        fileID = ((ITEMType) item).getValue();
                    } else if (ID_FILTERNAME.equals(((ITEMType) item).getName())) {
                        filtername = ((ITEMType) item).getValue();
                    } else if (ID_COMPARISON.equals(((ITEMType) item).getName())) {
                        comparison = ((ITEMType) item).getValue();
                    } else if (ID_VALUE.equals(((ITEMType) item).getName())) {
                        value = ((ITEMType) item).getValue();
                    } else if (ID_NEGATE.equals(((ITEMType) item).getName())) {
                        negate = ((ITEMType) item).getValue();
                    }
                }
            }

            execute(peptideModeller, piaModeller,
                    new String[] {fileID, filtername, comparison, value, negate});
        }
    },


    Export {
        @Override
        public boolean execute(PeptideModeller peptideModeller, PIAModeller piaModeller, String[] params) {
            LOGGER.info(LOGGING_PREAMBEL + name());

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
                    oneAccessionPerLine = !((commandParams != null) &&
                            (commandParams.length > 0)) || "yes".equals(commandParams[0]) || "true".equals(commandParams[0]);
// only setting the flag is equivalent to true
                } else if ("exportPSMSets".equals(command)) {
                    exportPSMSets = !((commandParams != null) &&
                            (commandParams.length > 0)) || "yes".equals(commandParams[0]) || "true".equals(commandParams[0]);
// only setting the flag is equivalent to true
                } else if ("exportPSMs".equals(command)) {
                    exportPSMs = !((commandParams != null) && (commandParams.length > 0)) || "yes".equals(commandParams[0]) || "true".equals(commandParams[0]);
                    // only setting the flag is equivalent to true
                }
            }

            LOGGER.info("export parameters: " +
                    "filename: " + fileName +
                    ", fileID: " + fileID +
                    ", format: " + format +
                    ", oneAccessionPerLine: " + oneAccessionPerLine +
                    ", exportPSMSets: " + exportPSMSets +
                    ", exportPSMs:" + exportPSMs);

            Writer writer = null;
            try {
                writer = new FileWriter(fileName, false);

                if ((format == null)
                        || (format.trim().length() == 0)
                        || "csv".equalsIgnoreCase(format)) {
                    peptideModeller.exportCSV(writer, fileID, true,
                            oneAccessionPerLine, exportPSMSets, exportPSMs);
                }
            } catch (IOException e) {
                LOGGER.error("Cannot write to file " + fileName, e);
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        LOGGER.error("Cannot close file " + fileName, e);
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
                    "\nfileName: the report file NAME [report.peptide.csv]" +
                    "\noneAccessionPerLine: write one accession per line " +
                    "(useful for spectral counting), defaults to false" +
                    "\nexportPSMSets: defaults to false" +
                    "\nexportPSMs: defaults to false";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            // this is not executable via XML file
            return Collections.emptyList();
        }

        @Override
        public void executeXMLParameters(NODEType node, PeptideModeller peptideModeller, PIAModeller piaModeller) {
            // this is not executable via XML file
        }
    }
    ;


    /** logger for this enum */
    private static final Logger LOGGER = Logger.getLogger(PeptideExecuteCommands.class);

    /** informative preambel of logging */
    private static final String LOGGING_PREAMBEL = "execute CLI command ";

    /** the PREFIX for this level's execute commands */
    private static final String PREFIX = "Peptide";


    @Override
    public NODEType generateNode(String[] params) {
        if (params.length < 1) {
            return null;
        }

        String execution = params[0];
        if (execution.startsWith(PREFIX)) {
            execution = execution.substring(PREFIX.length());
        }
        if (!name().equals(execution)) {
            LOGGER.error(name() + " is the wrong execute command for " +
                    execution);
            return null;
        }

        NODEType node = new NODEType();
        node.setName(PREFIX + execution);
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
        return getPrefix();
    }


    /**
     * Static getter for the Prefix
     *
     * @return
     */
    public static String getPrefix() {
        return PREFIX;
    }


    /**
     * Gives a string representation of the valid commands for this enumeration
     * @return
     */
    public static String getValidCommandsString() {
        StringBuilder sb = new StringBuilder();

        for (PeptideExecuteCommands command : values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(command.name());
        }

        return sb.toString();
    }
}
