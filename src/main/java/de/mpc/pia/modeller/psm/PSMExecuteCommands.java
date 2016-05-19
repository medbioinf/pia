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

import de.mpc.pia.modeller.PSMModeller;
import de.mpc.pia.modeller.execute.ExecuteModelCommands;
import de.mpc.pia.modeller.execute.xmlparams.ITEMType;
import de.mpc.pia.modeller.execute.xmlparams.NODEType;
import de.mpc.pia.modeller.execute.xmlparams.PossibleITEMType;
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
        private static final String idScoreName = "score name";

        public boolean execute(PSMModeller modeller, String[] params) {
            logger.info("execute CLI command " + name());
            modeller.addPreferredFDRScore(params[0]);
            return true;
        }

        @Override
        public String describe() {
            return "Adds the given score name to the list of preferred " +
                    "scores for FDR calculation.";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            List<List<String>> params = new ArrayList<List<String>>();

            List<String> param = new ArrayList<String>();
            param.add(idScoreName);
            params.add(param);

            return params;
        }

        @Override
        public void executeXMLParameters(NODEType node, PSMModeller model) {
            String scoreName = null;

            for (Object item : node.getITEMOrITEMLISTOrNODE()) {
                if (item instanceof ITEMType) {
                    if (idScoreName.equals(((ITEMType) item).getName())) {
                        scoreName = ((ITEMType) item).getValue();
                        break;
                    }
                }
            }

            if (scoreName != null) {
                execute(model, new String[] {scoreName});
            }
        }
    },

    AddPreferredFDRScores {
        @Override
        public boolean execute(PSMModeller modeller, String[] params) {
            logger.info("execute CLI command " + name());
            ArrayList<String> list = new ArrayList<String>(params.length);
            Collections.addAll(list, params);
            modeller.addPreferredFDRScores(list);
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
            return null;
        }

        @Override
        public void executeXMLParameters(NODEType node, PSMModeller model) {
            // this is not executable via XML file
        }
    },

    SetAllTopidentificationsForFDR {
        /** the identification string for the number of top identifications */
        private static final String idNumberName = "number of top identifications";

        @Override
        public boolean execute(PSMModeller modeller, String[] params) {
            logger.info("execute CLI command " + name());
            Integer topIDs = Integer.parseInt(params[0]);

            modeller.setAllTopIdentifications(topIDs);
            return false;
        }

        @Override
        public String describe() {
            return "Sets the number of top identifications per spectrum " +
                    "used for all further FDR calculations, 0 meaning " +
                    "all identifications are used.";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            List<List<String>> params = new ArrayList<List<String>>();

            List<String> param = new ArrayList<String>();
            param.add(idNumberName);
            params.add(param);

            return params;
        }

        @Override
        public void executeXMLParameters(NODEType node, PSMModeller model) {
            String numberIdentifications = null;

            for (Object item : node.getITEMOrITEMLISTOrNODE()) {
                if (item instanceof ITEMType) {
                    if (idNumberName.equals(((ITEMType) item).getName())) {
                        numberIdentifications = ((ITEMType) item).getValue();
                        break;
                    }
                }
            }

            if (numberIdentifications != null) {
                execute(model, new String[] {numberIdentifications});
            }
        }
    },

    SetAllDecoyPattern {
        /** the identification string for the score name */
        private static final String idDecoyPattern = "decoy pattern";

        public boolean execute(PSMModeller modeller, String[] params) {
            logger.info("execute CLI command " + name());
            modeller.setAllDecoyPattern(params[0]);
            return true;
        }

        @Override
        public String describe() {
            return "Sets the regular expression used for decoy detection or " +
                    "if '" + FDRData.DecoyStrategy.SEARCHENGINE +
                    "' is given as pattern, assumes a decoy search directly " +
                    "performed by the search engine.";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            List<List<String>> params = new ArrayList<List<String>>();

            List<String> param = new ArrayList<String>();
            param.add(idDecoyPattern);
            params.add(param);

            return params;
        }

        @Override
        public void executeXMLParameters(NODEType node, PSMModeller model) {
            String pattern = null;

            for (Object item : node.getITEMOrITEMLISTOrNODE()) {
                if (item instanceof ITEMType) {
                    if (idDecoyPattern.equals(((ITEMType) item).getName())) {
                        pattern = ((ITEMType) item).getValue();
                        break;
                    }
                }
            }

            if (idDecoyPattern != null) {
                execute(model, new String[] {pattern});
            }
        }
    },

    CalculateAllFDR {
        @Override
        public boolean execute(PSMModeller modeller, String[] params) {
            logger.info("execute CLI command " + name());
            modeller.calculateAllFDR();
            return true;
        }

        @Override
        public String describe() {
            return "Calculates the FDR scores for all files.";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            // no parameters are needed
            return new ArrayList<List<String>>();
        }

        @Override
        public void executeXMLParameters(NODEType node, PSMModeller model) {
            execute(model, null);
        }
    },

    CalculateCombinedFDRScore {
        public boolean execute(PSMModeller modeller, String[] params) {
            logger.info("execute CLI command " + name());
            modeller.calculateCombinedFDRScore();
            return true;
        }

        @Override
        public String describe() {
            return "Calculates the combined FDR score. The FDR scores for " +
                    "the single files should be calculated before.";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            // no parameters are needed
            return new ArrayList<List<String>>();
        }

        @Override
        public void executeXMLParameters(NODEType node, PSMModeller model) {
            execute(model, null);
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
        public boolean execute(PSMModeller modeller, String[] params) {
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
            return "Adds a PSM level filter to a specified file. The file is " +
                    "given by the first parameter, which has to be the file " +
                    "id. Filters are added by their name, an abbreviation " +
                    "for the camparison, the compared value and (optional), " +
                    "whether the comparison should be negated " +
                    "e.g. \"" + name() + "=1,charge_filter,EQ,2,no\". \n" +
                    "Registered PSM filters are: " + RegisteredFilters.getPSMFilterShortsForHelp();
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
        public void executeXMLParameters(NODEType node, PSMModeller model) {
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

    CreatePSMSets {
        /** the identification string for create PSM sets */
        private static final String idCreatePSMSets = "create sets";

        @Override
        public boolean execute(PSMModeller modeller, String[] params) {
            logger.info("execute CLI command " + name());

            Boolean createSets = null;

            if ((params != null) && (params.length > 0)) {
                if (params[0] != null) {
                    if (params[0].equals("true") || params[0].equals("yes")) {
                        createSets = true;
                    } else {
                        createSets = false;
                    }
                }
            }

            if (createSets == null) {
                // just giving the flag is considered as setting it true
                createSets = true;
            }

            modeller.applyGeneralSettings(createSets);
            return true;
        }

        @Override
        public String describe() {
            return "Sets whether PSM sets should be built to combine search " +
                    "results from different search engines / runs.";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            List<List<String>> params = new ArrayList<List<String>>();

            List<String> param = new ArrayList<String>();
            param.add(idCreatePSMSets);
            param.add("no");
            param.add("yes");
            params.add(param);

            return params;
        }

        @Override
        public void executeXMLParameters(NODEType node, PSMModeller model) {
            String createSets = null;

            for (Object item : node.getITEMOrITEMLISTOrNODE()) {
                if (item instanceof ITEMType) {
                    if (idCreatePSMSets.equals(((ITEMType) item).getName())) {
                        createSets = ((ITEMType) item).getValue();
                    }
                }
            }

            execute(model, new String[] {createSets});
        }
    },

    Export {
        public boolean execute(PSMModeller modeller, String[] params) {
            logger.info("execute CLI command " + name());

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
                    if ((commandParams != null) &&
                            (commandParams.length > 0)) {

                        if (commandParams[0].equals("yes") ||
                                commandParams[0].equals("true")) {
                            spectralCount = true;
                        } else {
                            spectralCount = false;
                        }
                    } else {
                        // only setting the flag is equivalent to true
                        spectralCount = true;
                    }
                }
            }

            if ((format == null) || (format.trim().length() == 0)) {
                format = "mzid";
            }
            if (fileName == null) {
                fileName = "report-psms." + format;
            }

            logger.info("export parameters: " +
                    "filename: " + fileName +
                    ", fileID: " + fileID +
                    ", format: " + format +
                    ", spectral_count: " + spectralCount);

            Writer writer = null;
            try {
                writer = new FileWriter(fileName, false);

                if (format.equalsIgnoreCase("mzIdentML") ||
                        format.equalsIgnoreCase("mzid")) {
                    modeller.exportMzIdentML(writer, fileID, true);
                } else if (format.equalsIgnoreCase("mztab")) {
                    modeller.exportMzTab(writer, fileID, true);
                } else if (format.equalsIgnoreCase("csv")) {
                    modeller.exportCSV(writer, fileID,
                            spectralCount, true);
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
                    "\nformat: mzid [default], mztab, csv" +
                    "\nfileID: default 0 (overview)" +
                    "\nfileName: the report file name [report.mzid]" +
                    "\nspectral_count: defaults to no";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            // this is not executable via XML file
            return null;
        }

        @Override
        public void executeXMLParameters(NODEType node, PSMModeller model) {
            // this is not executable via XML file
        }
    },

    ;


    /** logger for this enum */
    private static final Logger logger = Logger.getLogger(PSMExecuteCommands.class);

    /** the prfix for this level's execute commands */
    public final static String prefix = "PSM";


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

        for (PSMExecuteCommands command : values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(command.name());
        }

        return sb.toString();
    }
}