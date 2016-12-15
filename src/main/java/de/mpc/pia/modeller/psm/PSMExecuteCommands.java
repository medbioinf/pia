package de.mpc.pia.modeller.psm;

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
import de.mpc.pia.modeller.PSMModeller;
import de.mpc.pia.modeller.execute.ExecuteModelCommands;
import de.mpc.pia.modeller.execute.xmlparams.ITEMType;
import de.mpc.pia.modeller.execute.xmlparams.NODEType;
import de.mpc.pia.modeller.execute.xmlparams.PossibleITEMType;
import de.mpc.pia.modeller.exporter.MzIdentMLExporter;
import de.mpc.pia.modeller.exporter.MzTabExporter;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;
import de.mpc.pia.modeller.score.FDRData;


/**
 * This enum holds the command line and XML interface executioners for the PSM
 * modeler.
 *
 * @author julian
 *
 */
public enum PSMExecuteCommands implements ExecuteModelCommands<PSMModeller> {
    AddPreferredFDRScore {
        /** the identification string for the score name */
        private static final String ID_SCORE_NAME = "score name";

        @Override
        public boolean execute(PSMModeller psmModeller, PIAModeller piaModeller, String[] params) {
            LOGGER.info(LOGGING_PREAMBEL + name());
            psmModeller.addPreferredFDRScore(params[0]);
            return true;
        }

        @Override
        public String describe() {
            return "Adds the given score name to the list of preferred scores for FDR calculation.";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            List<List<String>> params = new ArrayList<>();

            List<String> param = new ArrayList<>();
            param.add(ID_SCORE_NAME);
            params.add(param);

            return params;
        }

        @Override
        public void executeXMLParameters(NODEType node, PSMModeller psmModeller, PIAModeller piaModeller) {
            String scoreName = null;

            for (Object item : node.getITEMOrITEMLISTOrNODE()) {
                if ((item instanceof ITEMType)
                        && ID_SCORE_NAME.equals(((ITEMType) item).getName())) {
                    scoreName = ((ITEMType) item).getValue();
                    break;
                }
            }

            if (scoreName != null) {
                execute(psmModeller, piaModeller, new String[] {scoreName});
            }
        }
    },

    AddPreferredFDRScores {
        @Override
        public boolean execute(PSMModeller psmModeller, PIAModeller piaModeller, String[] params) {
            LOGGER.info(LOGGING_PREAMBEL + name());
            ArrayList<String> list = new ArrayList<>(params.length);
            Collections.addAll(list, params);
            psmModeller.addPreferredFDRScores(list);
            return true;
        }

        @Override
        public String describe() {
            return "Adds the params to the list of preferred scores for " +
                    "FDR calculation.";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            // this is not executable via XML file
            return Collections.emptyList();
        }

        @Override
        public void executeXMLParameters(NODEType node, PSMModeller psmModeller, PIAModeller piaModeller) {
            // this is not executable via XML file
        }
    },

    SetAllTopidentificationsForFDR {
        /** the identification string for the number of top identifications */
        private static final String ID_NUMBER_NAME = "number of top identifications";

        @Override
        public boolean execute(PSMModeller psmModeller, PIAModeller piaModeller, String[] params) {
            LOGGER.info(LOGGING_PREAMBEL + name());
            Integer topIDs = Integer.parseInt(params[0]);
            psmModeller.setAllTopIdentifications(topIDs);
            return false;
        }

        @Override
        public String describe() {
            return "Sets the number of top identifications per spectrum used"
                    + " for all further FDR calculations, 0 meaning all"
                    + " identifications are used.";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            List<List<String>> params = new ArrayList<>();

            List<String> param = new ArrayList<>();
            param.add(ID_NUMBER_NAME);
            params.add(param);

            return params;
        }

        @Override
        public void executeXMLParameters(NODEType node, PSMModeller psmModeller, PIAModeller piaModeller) {
            String numberIdentifications = null;

            for (Object item : node.getITEMOrITEMLISTOrNODE()) {
                if ((item instanceof ITEMType)
                        && ID_NUMBER_NAME.equals(((ITEMType) item).getName())) {
                    numberIdentifications = ((ITEMType) item).getValue();
                    break;
                }
            }

            if (numberIdentifications != null) {
                execute(psmModeller, piaModeller, new String[] {numberIdentifications});
            }
        }
    },

    SetAllDecoyPattern {
        /** the identification string for the score name */
        private static final String ID_DECOY_PATTERN = "decoy pattern";

        @Override
        public boolean execute(PSMModeller psmModeller, PIAModeller piaModeller, String[] params) {
            LOGGER.info(LOGGING_PREAMBEL + name());
            psmModeller.setAllDecoyPattern(params[0]);
            return true;
        }

        @Override
        public String describe() {
            return "Sets the regular expression used for decoy detection or if"
                    + " '" + FDRData.DecoyStrategy.SEARCHENGINE + "' is given"
                    + " as pattern, assumes a decoy search directly performed"
                    + " by the search engine.";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            List<List<String>> params = new ArrayList<>();

            List<String> param = new ArrayList<>();
            param.add(ID_DECOY_PATTERN);
            params.add(param);

            return params;
        }

        @Override
        public void executeXMLParameters(NODEType node, PSMModeller psmModeller, PIAModeller piaModeller) {
            String pattern = null;

            for (Object item : node.getITEMOrITEMLISTOrNODE()) {
                if ((item instanceof ITEMType) && ID_DECOY_PATTERN.equals(((ITEMType) item).getName())) {
                    pattern = ((ITEMType) item).getValue();
                    break;
                }
            }
            execute(psmModeller, piaModeller, new String[] {pattern});
        }
    },

    CalculateAllFDR {
        @Override
        public boolean execute(PSMModeller psmModeller, PIAModeller piaModeller, String[] params) {
            LOGGER.info(LOGGING_PREAMBEL + name());
            psmModeller.calculateAllFDR();
            return true;
        }

        @Override
        public String describe() {
            return "Calculates the FDR scores for all files.";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            // no parameters are needed
            return Collections.emptyList();
        }

        @Override
        public void executeXMLParameters(NODEType node, PSMModeller psmModeller, PIAModeller piaModeller) {
            execute(psmModeller, piaModeller, null);
        }
    },

    CalculateCombinedFDRScore {
        @Override
        public boolean execute(PSMModeller psmModeller, PIAModeller piaModeller, String[] params) {
            LOGGER.info(LOGGING_PREAMBEL + name());
            psmModeller.calculateCombinedFDRScore();
            return true;
        }

        @Override
        public String describe() {
            return "Calculates the combined FDR score. The FDR scores for the"
                    + " single files should be calculated before.";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            // no parameters are needed
            return Collections.emptyList();
        }

        @Override
        public void executeXMLParameters(NODEType node, PSMModeller psmModeller, PIAModeller piaModeller) {
            execute(psmModeller, piaModeller, null);
        }
    },

    AddFilter {
        /** the identification string for the file ID */
        private static final String ID_FILE_ID= "file ID";

        /** the identification string for the filter name */
        private static final String ID_FILTERNAME= "filtername";

        /** the identification string for negate */
        private static final String ID_NEGATE= "negate";

        /** the identification string for the equation */
        private static final String ID_COMPARISON= "comparison";

        /** the identification string for the value */
        private static final String ID_VALUE= "value";

        @Override
        public boolean execute(PSMModeller psmModeller, PIAModeller piaModeller, String[] params) {
            LOGGER.info(LOGGING_PREAMBEL + name());

            boolean negate = false;

            if ((params.length >= 5) && (params[4] != null)
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
                    psmModeller.addFilter(Long.parseLong(fileID), newFilter);
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
            return "Adds a PSM level filter to a specified file. The file is"
                    + " given by the first parameter, which has to be the file"
                    + " id. Filters are added by their name, an abbreviation"
                    + " for the camparison, the compared value and (optional),"
                    + " whether the comparison should be negated, e.g. "
                    + "\"" + name() + "=1,charge_filter,EQ,2,no\". \n"
                    + "Registered PSM filters are: " + RegisteredFilters.getPSMFilterShortsForHelp();
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
        public void executeXMLParameters(NODEType node, PSMModeller psmModeller, PIAModeller piaModeller) {
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

            execute(psmModeller, piaModeller,
                    new String[] {fileID, filtername, comparison, value, negate});
        }
    },

    CreatePSMSets {
        /** the identification string for create PSM sets */
        private static final String ID_CREATE_PSM_SETS = "create sets";

        @Override
        public boolean execute(PSMModeller psmModeller, PIAModeller piaModeller, String[] params) {
            LOGGER.info(LOGGING_PREAMBEL + name());

            Boolean createSets = null;

            if ((params != null) && (params.length > 0) && (params[0] != null)) {
                createSets = "true".equals(params[0]) || "yes".equals(params[0]);
            }

            if (createSets == null) {
                // just giving the flag is considered as setting it true
                createSets = true;
            }

            psmModeller.applyGeneralSettings(createSets);
            return true;
        }

        @Override
        public String describe() {
            return "Sets whether PSM sets should be built to combine search " +
                    "results from different search engines / runs.";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            List<List<String>> params = new ArrayList<>();

            List<String> param = new ArrayList<>();
            param.add(ID_CREATE_PSM_SETS);
            param.add("no");
            param.add("yes");
            params.add(param);

            return params;
        }

        @Override
        public void executeXMLParameters(NODEType node, PSMModeller psmModeller, PIAModeller piaModeller) {
            String createSets = null;

            for (Object item : node.getITEMOrITEMLISTOrNODE()) {
                if ((item instanceof ITEMType)
                        && ID_CREATE_PSM_SETS.equals(((ITEMType) item).getName())) {
                    createSets = ((ITEMType) item).getValue();
                }
            }

            execute(psmModeller, piaModeller, new String[] {createSets});
        }
    },

    Export {
        @Override
        public boolean execute(PSMModeller psmModeller, PIAModeller piaModeller, String[] params) {
            LOGGER.info(LOGGING_PREAMBEL + name());

            String format = null;
            Long fileID = 0L;
            String fileName = null;
            Boolean spectralCount = false;

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
                } else if ("spectral_count".equals(command)) {
                    spectralCount = !((commandParams != null) &&
                            (commandParams.length > 0)) || "yes".equals(commandParams[0]) || "true".equals(commandParams[0]);
// only setting the flag is equivalent to true
                }
            }

            if ((format == null) || (format.trim().length() == 0)) {
                format = "mzid";
            }
            if (fileName == null) {
                fileName = "report-psms." + format;
            }

            LOGGER.info("export parameters: " +
                    "filename: " + fileName +
                    ", fileID: " + fileID +
                    ", format: " + format +
                    ", spectral_count: " + spectralCount);

            boolean exportOK = true;

            try {
                if ("mzIdentML".equalsIgnoreCase(format) ||
                        "mzid".equalsIgnoreCase(format)) {
                    MzIdentMLExporter exporter = new MzIdentMLExporter(piaModeller);
                    exportOK = exporter.exportToMzIdentML(fileID, fileName, false, true);
                } else if ("mztab".equalsIgnoreCase(format)) {
                    MzTabExporter exporter = new MzTabExporter(piaModeller);
                    exportOK = exporter.exportToMzTab(fileID, fileName, false, false, true);
                } else if ("csv".equalsIgnoreCase(format)) {
                    Writer writer = new FileWriter(fileName, false);
                    psmModeller.exportCSV(writer, fileID, spectralCount, true);
                    writer.close();
                }
            } catch (IOException e) {
                LOGGER.error("Error writing to file " + fileName, e);
                exportOK = false;
            }

            return exportOK;
        }

        @Override
        public String describe() {
            return "Exports the report. " +
                    "Additional parameters may be passed semicolon " +
                    "separated with the syntax param=arg[;arg2;...]." +
                    "valid parameters are:" +
                    "\nformat: mzid [default], mztab, csv" +
                    "\nfileID: default 0 (overview)" +
                    "\nfileName: the report file name [report.mzid]" +
                    "\nspectral_count: defaults to no";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            // this is not executable via XML file
            return Collections.emptyList();
        }

        @Override
        public void executeXMLParameters(NODEType node, PSMModeller psmModeller, PIAModeller piaModeller) {
            // this is not executable via XML file
        }
    },

    ;


    /** logger for this enum */
    private static final Logger LOGGER = Logger.getLogger(PSMExecuteCommands.class);

    /** informative preambel of logging */
    private static final String LOGGING_PREAMBEL = "execute CLI command ";

    /** the prefix for this level's execute commands */
    private static final String PREFIX = "PSM";


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

        for (PSMExecuteCommands command : values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(command.name());
        }

        return sb.toString();
    }
}