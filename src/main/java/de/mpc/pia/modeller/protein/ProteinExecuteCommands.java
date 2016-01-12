package de.mpc.pia.modeller.protein;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import de.mpc.pia.modeller.ProteinModeller;
import de.mpc.pia.modeller.execute.ExecuteModelCommands;
import de.mpc.pia.modeller.execute.xmlparams.ITEMType;
import de.mpc.pia.modeller.execute.xmlparams.NODEType;
import de.mpc.pia.modeller.execute.xmlparams.PossibleITEMType;
import de.mpc.pia.modeller.protein.inference.AbstractProteinInference;
import de.mpc.pia.modeller.protein.inference.ProteinInferenceFactory;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.protein.scoring.ProteinScoringFactory;
import de.mpc.pia.modeller.protein.scoring.ProteinScoringFactory.ScoringType;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;


/**
 * This enum holds the command line and XML interface executioners for the
 * protein modeller.
 *
 * @author julian
 *
 */
public enum ProteinExecuteCommands implements ExecuteModelCommands<ProteinModeller> {

    AddFilter {
        /** the identification string for the filter name */
        private static final String idFiltername= "filtername";

        /** the identification string for the equation */
        private static final String idComparison= "comparison";

        /** the identification string for the value */
        private static final String idValue= "value";

        /** the identification string for negate */
        private static final String idNegate= "negate";

        @Override
        public boolean execute(ProteinModeller modeller, String[] params) {
            logger.info("execute CLI command " + name());

            boolean negate = false;

            if (params.length >= 4) {
                if (params[3] != null &&
                        (params[3].equals("true") || params[3].equals("yes"))) {
                    negate = true;
                }
            }

            if (params.length >= 3) {
                String filtername = params[0];
                String comparison = params[1];
                String value = params[2];

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
                    modeller.addReportFilter(newFilter);
                    logger.info("Filter '" + newFilter.toString() +
                            "' added to report filters");
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
            return "Adds a protein level filter to a specified file. A " +
                    "filter is added by its name, an abbreviation for the " +
                    "comparison, the compared value and (optional), whether " +
                    "the comparison should be negated " +
                    "e.g. \"" + name() + "=charge_filter,EQ,2,no\". \n" +
                    "Registered protein filters are: " +
                    RegisteredFilters.getProteinFilterShortsForHelp();
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            List<List<String>> params = new ArrayList<List<String>>();

            List<String> param = new ArrayList<String>();
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
        public void executeXMLParameters(NODEType node, ProteinModeller model) {
            String filtername = null;
            String comparison = null;
            String value = null;
            String negate = null;

            for (Object item : node.getITEMOrITEMLISTOrNODE()) {
                if (item instanceof ITEMType) {
                    if (idFiltername.equals(((ITEMType) item).getName())) {
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

            execute(model,
                    new String[] {filtername, comparison, value, negate});
        }
    },


    AddInferenceFilter {
        /** the identification string for the filter name */
        private static final String idFiltername= "filtername";

        /** the identification string for the equation */
        private static final String idComparison= "comparison";

        /** the identification string for the value */
        private static final String idValue= "value";

        /** the identification string for negate */
        private static final String idNegate= "negate";

        @Override
        public boolean execute(ProteinModeller modeller, String[] params) {
            logger.info("execute CLI command " + name());

            boolean negate = false;

            if (params.length >= 4) {
                if (params[3] != null &&
                        (params[3].equals("true") || params[3].equals("yes"))) {
                    negate = true;
                }
            }

            if (params.length >= 3) {
                String filtername = params[0];
                String comparison = params[1];
                String value = params[2];

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
                    modeller.addInferenceFilter(newFilter);
                    logger.info("Filter '" + newFilter.toString() +
                            "' added to inference filters");
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
            return "Adds a filter used by the protein inference. A filter " +
                    "is added by its name, an abbreviation for the " +
                    "comparison, the compared value and (optional), whether " +
                    "the comparison should be negated" +
                    "e.g. \"" + name() + "=charge_filter,EQ,2,no\"";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            List<List<String>> params = new ArrayList<List<String>>();

            List<String> param = new ArrayList<String>();
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
        public void executeXMLParameters(NODEType node, ProteinModeller model) {
            String filtername = null;
            String comparison = null;
            String value = null;
            String negate = null;

            for (Object item : node.getITEMOrITEMLISTOrNODE()) {
                if (item instanceof ITEMType) {
                    if (idFiltername.equals(((ITEMType) item).getName())) {
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

            execute(model,
                    new String[] {filtername, comparison, value, negate});
        }
    },

    InfereProteins {
        /** the identification string for the inference method */
        private static final String idInferenceMethod = "inference";

        /** the identification string for the scoring method */
        private static final String idScoringMethod = "scoring";

        /** the identification string for the score used for the inference */
        private static final String idUsedScore = "used score";

        /** the identification string for the soectra used for the inference */
        private static final String idUsedSpectra = "used spectra";

        @Override
        public boolean execute(ProteinModeller modeller, String[] params) {
            logger.info("execute CLI command " + name());

            if (params.length >= 2) {
                String inferenceName = params[0];
                String scoringName = params[1];

                AbstractProteinInference inference =
                        ProteinInferenceFactory.createInstanceOf(inferenceName);

                if (inference != null) {
                    // add any filters
                    for (AbstractFilter filter : modeller.getInferenceFilters()) {
                        inference.addFilter(filter);
                    }

                    // set the scoring
                    AbstractScoring scoring;
                    scoring = ProteinScoringFactory.getNewInstanceByName(
                            scoringName, new HashMap<String, String>());
                    // set the scoring settings
                    if ((params.length > 2) && (params[2] != null)) {
                        for (String param : params[2].split(";")) {
                            String[] settingParams = param.split("=");
                            if (settingParams.length > 1) {
                                scoring.setSetting(settingParams[0],
                                        settingParams[1]);
                            }
                        }
                    }
                    inference.setScoring(scoring);

                    modeller.infereProteins(inference);
                } else {
                    logger.error("Could not create inference method with " +
                            "name: " + inferenceName);
                    return false;
                }
            }

            return true;
        }

        @Override
        public String describe() {
            return "Inferes the proteins with the given inference method. " +
                    "Any inference filters should be set before this call " +
                    "with calls of " + AddInferenceFilter + ". " +
                    "The scoring method is set with the second argument. " +
                    "The scoring settings can be given by athird argument " +
                    "containing setting=value[;setting=value]* (usual " +
                    "settings are used_score and used_spectra).";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            List<List<String>> params = new ArrayList<List<String>>();

            List<String> param = new ArrayList<String>();
            param.add(idInferenceMethod);
            param.add(ProteinInferenceFactory.ProteinInferenceMethod.
                    REPORT_SPECTRUM_EXTRACTOR.getShortName());
            param.add(ProteinInferenceFactory.ProteinInferenceMethod.
                    REPORT_ALL.getShortName());
            param.add(ProteinInferenceFactory.ProteinInferenceMethod.
                    REPORT_OCCAMS_RAZOR.getShortName());
            params.add(param);

            param = new ArrayList<String>();
            param.add(idScoringMethod);
            for (ScoringType type : ProteinScoringFactory.ScoringType.values()) {
                param.add(type.getShortName());
            }
            params.add(param);

            param = new ArrayList<String>();
            param.add(idUsedScore);
            params.add(param);

            param = new ArrayList<String>();
            param.add(idUsedSpectra);
            for (PSMForScoring psmScoring : PSMForScoring.values()) {
                param.add(psmScoring.getShortName());
            }
            params.add(param);

            return params;
        }

        @Override
        public void executeXMLParameters(NODEType node, ProteinModeller model) {
            String inferenceName = null;
            String scoringName = null;
            String usedScoreName = null;
            String usedSpectraName = null;

            for (Object item : node.getITEMOrITEMLISTOrNODE()) {
                if (item instanceof ITEMType) {
                    if (idInferenceMethod.equals(((ITEMType) item).getName())) {
                        inferenceName = ((ITEMType) item).getValue();
                    } else if (idScoringMethod.equals(((ITEMType) item).getName())) {
                        scoringName = ((ITEMType) item).getValue();
                    } else if (idUsedScore.equals(((ITEMType) item).getName())) {
                        usedScoreName = ((ITEMType) item).getValue();
                    } else if (idUsedSpectra.equals(((ITEMType) item).getName())) {
                        usedSpectraName = ((ITEMType) item).getValue();
                    }
                }
            }

            execute(model,
                    new String[] {inferenceName, scoringName,
                            "used_score=" + usedScoreName +
                            ";used_spectra=" + usedSpectraName});
        }
    },

    Export {
        @Override
        public boolean execute(ProteinModeller modeller, String[] params) {
            logger.info("execute CLI command " + name());

            String format = null;
            String fileName = null;
            boolean oneAccessionPerLine = false;
            boolean exportPeptides = false;
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
                } else if ("exportPeptides".equals(command)) {
                    if ((commandParams != null) &&
                            (commandParams.length > 0)) {
                        if (commandParams[0].equals("yes") ||
                                commandParams[0].equals("true")) {
                            exportPeptides = true;
                        } else {
                            exportPeptides = false;
                        }
                    } else {
                        // only setting the flag is equivalent to true
                        exportPeptides = true;
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

            if ((format == null) || (format.trim().length() == 0)) {
                format = "mzTab";
            }
            if (fileName == null) {
                fileName = "report-proteins." + format;
            }

            logger.info("export parameters: " +
                    "filename: " + fileName +
                    ", format: " + format +
                    ", exportPSMs:" + exportPSMs +
                    ", exportPSMSets: " + exportPSMSets +
                    ", exportPeptides: " + exportPeptides +
                    ", oneAccessionPerLine: " + oneAccessionPerLine);

            Writer writer = null;
            try {
                writer = new FileWriter(fileName, false);

                if (format.equalsIgnoreCase("mzTab")) {
                    modeller.exportMzTab(writer, true,
                            exportPSMs || exportPSMSets);
                } else if (format.equalsIgnoreCase("mzIdentML") ||
                        format.equalsIgnoreCase("mzid")) {
                    modeller.exportMzIdentML(writer, true);
                } else if (format.equalsIgnoreCase("csv")) {
                    modeller.exportCSV(writer, true, exportPeptides,
                            exportPSMSets, exportPSMs, oneAccessionPerLine);
                }
                writer.close();
            } catch (IOException e) {
                logger.error("Cannot write to file " + fileName, e);
            }

            return true;
        }

        @Override
        public String describe() {
            return "Exports the report. " +
                    "Additional parameters may be passed semicolon " +
                    "separated with the syntax param=arg[;arg2;...]." +
                    "valid parameters are:" +
                    "\nformat: csv [default], mzIdentML" +
                    "\nfileID: default 0 (overview)" +
                    "\nfileName: the report file name [report.peptide.csv]" +
                    "\noneAccessionPerLine: write one accession per line " +
                    "(useful for spectral counting), defaults to false" +
                    "\nexportPeptides: defaults to false" +
                    "\nexportPSMSets: defaults to false" +
                    "\nexportPSMs: defaults to false";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            // this is not executable via XML file
            return null;
        }

        @Override
        public void executeXMLParameters(NODEType node, ProteinModeller model) {
            // this is not executable via XML file
        }
    },

    CalculateFDR {
        /** the identification string for the decoy strategy */
        private static final String idDecoyStrategy = "decoy strategy";

        /** the identification string for the decoy pattern (if strategy == accessionpattern) */
        private static final String idDecoyPattern = "decoy pattern";

        @Override
        public boolean execute(ProteinModeller modeller, String[] params) {
            logger.info("execute CLI command " + name());

            if (params.length >= 1) {
                DecoyStrategy decoyStrategy = DecoyStrategy.getStrategyByString(params[0]);
                String decoyPattern = null;

                switch (decoyStrategy) {
                case ACCESSIONPATTERN:
                    if (params.length >= 2) {
                        decoyPattern = params[1];
                    } else {
                        logger.error("no decoy pattern given!");
                        return false;
                    }
                    break;

                case INHERIT:
                    break;

                default:
                    logger.error("invalid decoy strategy given: " + params[0]);
                    return false;
                }

                modeller.updateFDRData(decoyStrategy, decoyPattern, 0.01);
                modeller.updateDecoyStates();
                modeller.calculateFDR();

                return true;
            }

            logger.error("no parameters for FDR calculation given, needs 'decoy strategy [decoy pattern]'");
            return false;
        }

        @Override
        public String describe() {
            return "Calculates the FDR with the given parameters. The first "
                    + "parameter is the decoy strategy, either accessionpattern "
                    + "or inherit (cautious with this). The second is the "
                    + "decoy regular expression.";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            List<List<String>> params = new ArrayList<List<String>>();

            List<String> param = new ArrayList<String>();
            param.add(idDecoyStrategy);
            param.add(FDRData.DecoyStrategy.ACCESSIONPATTERN.toString());
            param.add(FDRData.DecoyStrategy.INHERIT.toString());
            params.add(param);

            param = new ArrayList<String>();
            param.add(idDecoyPattern);
            params.add(param);

            return params;
        }

        @Override
        public void executeXMLParameters(NODEType node, ProteinModeller model) {
            String decoyStrategy = null;
            String decoyPattern = null;

            for (Object item : node.getITEMOrITEMLISTOrNODE()) {
                if (item instanceof ITEMType) {
                    if (idDecoyStrategy.equals(((ITEMType) item).getName())) {
                        decoyStrategy = ((ITEMType) item).getValue();
                    } else if (idDecoyPattern.equals(((ITEMType) item).getName())) {
                        decoyPattern = ((ITEMType) item).getValue();
                    }
                }
            }

            execute(model,
                    new String[] {decoyStrategy, decoyPattern});
        }
    },
    ;


    /** logger for this enum */
    private static final Logger logger = Logger.getLogger(ProteinExecuteCommands.class);

    /** the prfix for this level's execute commands */
    public final static String prefix = "Protein";


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

        for (ProteinExecuteCommands command : values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(command.name());
        }

        return sb.toString();
    }
}