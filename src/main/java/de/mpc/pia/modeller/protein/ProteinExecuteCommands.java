package de.mpc.pia.modeller.protein;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.ProteinModeller;
import de.mpc.pia.modeller.execute.ExecuteModelCommands;
import de.mpc.pia.modeller.execute.xmlparams.ITEMType;
import de.mpc.pia.modeller.execute.xmlparams.NODEType;
import de.mpc.pia.modeller.execute.xmlparams.PossibleITEMType;
import de.mpc.pia.modeller.exporter.MzIdentMLExporter;
import de.mpc.pia.modeller.exporter.MzTabExporter;
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
        /** the identification string for the filter NAME */
        private static final String ID_FILTERNAME= "filtername";

        /** the identification string for the equation */
        private static final String ID_COMPARISON= "comparison";

        /** the identification string for the value */
        private static final String ID_VALUE= "value";

        /** the identification string for negate */
        private static final String ID_NEGATE= "negate";

        @Override
        public boolean execute(ProteinModeller proteinModeller, PIAModeller piaModeller, String[] params) {
            LOGGER.info(LOGGING_PREAMBEL + name());

            boolean negate = false;

            if ((params.length >= 4)
                    && (params[3] != null)
                    && ("true".equals(params[3]) || "yes".equals(params[3]))) {
                negate = true;
            }

            if (params.length >= 3) {
                String filtername = params[0];
                String comparison = params[1];
                String value = params[2];

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
                    proteinModeller.addReportFilter(newFilter);
                    LOGGER.info("Filter '" + newFilter.toString()
                            + "' added to report filters");
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
            return "Adds a protein level filter to a specified file. A " +
                    "filter is added by its NAME, an abbreviation for the " +
                    "comparison, the compared value and (optional), whether " +
                    "the comparison should be negated " +
                    "e.g. \"" + name() + "=charge_filter,EQ,2,no\". \n" +
                    "Registered protein filters are: " +
                    RegisteredFilters.getProteinFilterShortsForHelp();
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            List<List<String>> params = new ArrayList<>();

            List<String> param = new ArrayList<>();
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
        public void executeXMLParameters(NODEType node, ProteinModeller proteinModeller, PIAModeller piaModeller) {
            String filtername = null;
            String comparison = null;
            String value = null;
            String negate = null;

            for (Object item : node.getITEMOrITEMLISTOrNODE()) {
                if (item instanceof ITEMType) {
                    if (ID_FILTERNAME.equals(((ITEMType) item).getName())) {
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

            execute(proteinModeller, piaModeller,
                    new String[] {filtername, comparison, value, negate});
        }
    },


    AddInferenceFilter {
        /** the identification string for the filter NAME */
        private static final String ID_FILTERNAME = "filtername";

        /** the identification string for the equation */
        private static final String ID_COMPARISON = "comparison";

        /** the identification string for the value */
        private static final String ID_VALUE = "value";

        /** the identification string for negate */
        private static final String ID_NEGATE = "negate";

        @Override
        public boolean execute(ProteinModeller proteinModeller, PIAModeller piaModeller, String[] params) {
            LOGGER.info(LOGGING_PREAMBEL + name());

            boolean negate = false;

            if ((params.length >= 4)
                    && (params[3] != null)
                    && ("true".equals(params[3]) || "yes".equals(params[3]))) {
                negate = true;
            }

            if (params.length >= 3) {
                String filtername = params[0];
                String comparison = params[1];
                String value = params[2];

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
                    proteinModeller.addInferenceFilter(newFilter);
                    LOGGER.info("Filter '" + newFilter.toString()
                            + "' added to inference filters");
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
            return "Adds a filter used by the protein inference. A filter is"
                    + " added by its NAME, an abbreviation for the comparison,"
                    + " the compared value and (optional), whether the"
                    + " comparison should be negated e.g."
                    + " \"" + name() + "=charge_filter,EQ,2,no\"";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            List<List<String>> params = new ArrayList<>();

            List<String> param = new ArrayList<>();
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
        public void executeXMLParameters(NODEType node, ProteinModeller proteinModeller, PIAModeller piaModeller) {
            String filtername = null;
            String comparison = null;
            String value = null;
            String negate = null;

            for (Object item : node.getITEMOrITEMLISTOrNODE()) {
                if (item instanceof ITEMType) {
                    if (ID_FILTERNAME.equals(((ITEMType) item).getName())) {
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

            execute(proteinModeller, piaModeller,
                    new String[] {filtername, comparison, value, negate});
        }
    },

    InfereProteins {
        /** the identification string for the inference method */
        private static final String ID_INFERENCE_METHOD = "inference";

        /** the identification string for the scoring method */
        private static final String ID_SCORING_METHOD = "scoring";

        /** the identification string for the score used for the inference */
        private static final String ID_USED_SCORE = "used score";

        /** the identification string for the soectra used for the inference */
        private static final String ID_USED_SPECTRA = "used spectra";

        @Override
        public boolean execute(ProteinModeller proteinModeller, PIAModeller piaModeller, String[] params) {
            LOGGER.info(LOGGING_PREAMBEL + name());

            if (params.length >= 2) {
                String inferenceName = params[0];
                String scoringName = params[1];

                AbstractProteinInference inference =
                        ProteinInferenceFactory.createInstanceOf(inferenceName);

                if (inference != null) {
                    // add any filters
                    proteinModeller.getInferenceFilters().forEach(inference::addFilter);

                    // set the scoring
                    AbstractScoring scoring;
                    scoring = ProteinScoringFactory.getNewInstanceByName(
                            scoringName, new HashMap<>());
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

                    proteinModeller.infereProteins(inference);
                } else {
                    LOGGER.error("Could not create inference method with NAME: " + inferenceName);
                    return false;
                }
            }

            return true;
        }

        @Override
        public String describe() {
            return "Inferes the proteins with the given inference method. Any"
                    + " inference filters should be set before this call with"
                    + " calls of " + AddInferenceFilter + ". The scoring method"
                    + " is set with the second argument. The scoring settings"
                    + " can be given by athird argument containing"
                    + " setting=value[;setting=value]* (common settings are"
                    + " used_score and used_spectra).";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            List<List<String>> params = new ArrayList<>();

            List<String> param = new ArrayList<>();
            param.add(ID_INFERENCE_METHOD);
            param.add(ProteinInferenceFactory.ProteinInferenceMethod.REPORT_SPECTRUM_EXTRACTOR.getShortName());
            param.add(ProteinInferenceFactory.ProteinInferenceMethod.REPORT_ALL.getShortName());
            param.add(ProteinInferenceFactory.ProteinInferenceMethod.REPORT_OCCAMS_RAZOR.getShortName());
            params.add(param);

            param = new ArrayList<>();
            param.add(ID_SCORING_METHOD);
            for (ScoringType type : ProteinScoringFactory.ScoringType.values()) {
                param.add(type.getShortName());
            }
            params.add(param);

            param = new ArrayList<>();
            param.add(ID_USED_SCORE);
            params.add(param);

            param = new ArrayList<>();
            param.add(ID_USED_SPECTRA);
            for (PSMForScoring psmScoring : PSMForScoring.values()) {
                param.add(psmScoring.getShortName());
            }
            params.add(param);

            return params;
        }

        @Override
        public void executeXMLParameters(NODEType node, ProteinModeller proteinModeller, PIAModeller piaModeller) {
            String inferenceName = null;
            String scoringName = null;
            String usedScoreName = null;
            String usedSpectraName = null;

            for (Object item : node.getITEMOrITEMLISTOrNODE()) {
                if (item instanceof ITEMType) {
                    if (ID_INFERENCE_METHOD.equals(((ITEMType) item).getName())) {
                        inferenceName = ((ITEMType) item).getValue();
                    } else if (ID_SCORING_METHOD.equals(((ITEMType) item).getName())) {
                        scoringName = ((ITEMType) item).getValue();
                    } else if (ID_USED_SCORE.equals(((ITEMType) item).getName())) {
                        usedScoreName = ((ITEMType) item).getValue();
                    } else if (ID_USED_SPECTRA.equals(((ITEMType) item).getName())) {
                        usedSpectraName = ((ITEMType) item).getValue();
                    }
                }
            }

            execute(proteinModeller, piaModeller,
                    new String[] {inferenceName, scoringName,
                            "used_score=" + usedScoreName + ";used_spectra=" + usedSpectraName});
        }
    },

    Export {
        @Override
        public boolean execute(ProteinModeller proteinModeller, PIAModeller piaModeller, String[] params) {
            LOGGER.info(LOGGING_PREAMBEL + name());

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
                    oneAccessionPerLine = !((commandParams != null) &&
                            (commandParams.length > 0)) || "yes".equals(commandParams[0]) || "true".equals(commandParams[0]);
// only setting the flag is equivalent to true
                } else if ("exportPeptides".equals(command)) {
                    exportPeptides = !((commandParams != null) &&
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

            if ((format == null) || (format.trim().length() == 0)) {
                format = "mzTab";
            }
            if (fileName == null) {
                fileName = "report-proteins." + format;
            }

            LOGGER.info("export parameters: " +
                    "filename: " + fileName +
                    ", format: " + format +
                    ", exportPSMs:" + exportPSMs +
                    ", exportPSMSets: " + exportPSMSets +
                    ", exportPeptides: " + exportPeptides +
                    ", oneAccessionPerLine: " + oneAccessionPerLine);

            boolean exportOK = true;
            try {
                if ("mzTab".equalsIgnoreCase(format)) {
                    MzTabExporter exporter = new MzTabExporter(piaModeller);
                    exportOK = exporter.exportToMzTab(0L, fileName, true, exportPeptides, true);
                } else if ("mzIdenML".equalsIgnoreCase(format)
                        || "mzid".equalsIgnoreCase(format)) {
                    MzIdentMLExporter exporter = new MzIdentMLExporter(piaModeller);
                    exportOK = exporter.exportToMzIdentML(0L, fileName, true, true);
                } else if ("csv".equalsIgnoreCase(format)) {
                    Writer writer = new FileWriter(fileName, false);
                    proteinModeller.exportCSV(writer, true, exportPeptides,
                            exportPSMSets, exportPSMs, oneAccessionPerLine);
                    writer.close();
                }
            } catch (IOException e) {
                LOGGER.error("Cannot write to file " + fileName, e);
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
                    "\nformat: csv [default], mzIdentML" +
                    "\nfileID: default 0 (overview)" +
                    "\nfileName: the report file NAME [report.peptide.csv]" +
                    "\noneAccessionPerLine: write one accession per line " +
                    "(useful for spectral counting), defaults to false" +
                    "\nexportPeptides: defaults to false" +
                    "\nexportPSMSets: defaults to false" +
                    "\nexportPSMs: defaults to false";
        }

        @Override
        public List<List<String>> neededXMLParameters() {
            // this is not executable via XML file
            return Collections.emptyList();
        }

        @Override
        public void executeXMLParameters(NODEType node, ProteinModeller proteinModeller, PIAModeller piaModeller) {
            // this is not executable via XML file
        }
    },

    CalculateFDR {
        /** the identification string for the decoy strategy */
        private static final String ID_DECOY_STRATEGY = "decoy strategy";

        /** the identification string for the decoy pattern (if strategy == accessionpattern) */
        private static final String ID_DECOY_PATTERN = "decoy pattern";

        @Override
        public boolean execute(ProteinModeller proteinModeller, PIAModeller piaModeller, String[] params) {
            LOGGER.info(LOGGING_PREAMBEL + name());

            if (params.length >= 1) {
                DecoyStrategy decoyStrategy = DecoyStrategy.getStrategyByString(params[0]);
                String decoyPattern = null;

                switch (decoyStrategy) {
                case ACCESSIONPATTERN:
                    if (params.length >= 2) {
                        decoyPattern = params[1];
                    } else {
                        LOGGER.error("no decoy pattern given!");
                        return false;
                    }
                    break;

                case INHERIT:
                    break;

                default:
                    LOGGER.error("invalid decoy strategy given: " + params[0]);
                    return false;
                }

                proteinModeller.updateFDRData(decoyStrategy, decoyPattern, 0.01);
                proteinModeller.updateDecoyStates();
                proteinModeller.calculateFDR();

                return true;
            }

            LOGGER.error("no parameters for FDR calculation given, needs 'decoy strategy [decoy pattern]'");
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
            List<List<String>> params = new ArrayList<>();

            List<String> param = new ArrayList<>();
            param.add(ID_DECOY_STRATEGY);
            param.add(FDRData.DecoyStrategy.ACCESSIONPATTERN.toString());
            param.add(FDRData.DecoyStrategy.INHERIT.toString());
            params.add(param);

            param = new ArrayList<>();
            param.add(ID_DECOY_PATTERN);
            params.add(param);

            return params;
        }

        @Override
        public void executeXMLParameters(NODEType node, ProteinModeller proteinModeller, PIAModeller piaModeller) {
            String decoyStrategy = null;
            String decoyPattern = null;

            for (Object item : node.getITEMOrITEMLISTOrNODE()) {
                if (item instanceof ITEMType) {
                    if (ID_DECOY_STRATEGY.equals(((ITEMType) item).getName())) {
                        decoyStrategy = ((ITEMType) item).getValue();
                    } else if (ID_DECOY_PATTERN.equals(((ITEMType) item).getName())) {
                        decoyPattern = ((ITEMType) item).getValue();
                    }
                }
            }

            execute(proteinModeller, piaModeller,
                    new String[] {decoyStrategy, decoyPattern});
        }
    },
    ;


    /** logger for this enum */
    private static final Logger LOGGER = Logger.getLogger(ProteinExecuteCommands.class);

    /** informative preambel of logging */
    private static final String LOGGING_PREAMBEL = "execute CLI command ";

    /** the prfix for this level's execute commands */
    private static final String PREFIX = "Protein";


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

        for (ProteinExecuteCommands command : values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(command.name());
        }

        return sb.toString();
    }
}