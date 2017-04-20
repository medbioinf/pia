package de.mpc.pia.modeller.execute;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * class which holds some tools for command line and XML file parsing.
 *
 * @author julianu
 *
 */
public class CommandTools {

    /** pattern for one single command */
    private static final String COMMAND_PATTERN = "^([^=]+)=(.*)";


    private CommandTools() {
        // do not instantiate this class
    }


    /**
     * Parses the commands into a map, i.e. separates the key and values separated by a "=".
     *
     * @param commands
     * @return
     */
    public static Map<String, String> parseCommands(String[] commands) {
        Map<String, String> commandMap = new HashMap<>(commands.length);
        Pattern pattern = Pattern.compile(COMMAND_PATTERN);
        Matcher commandParamMatcher;

        for (String command : commands) {
            commandParamMatcher = pattern.matcher(command);

            if (commandParamMatcher.matches()) {
                commandMap.put(commandParamMatcher.group(1), commandParamMatcher.group(2));
            }
        }

        return commandMap;
    }



    /**
     * Return true, if the map contains the key and its value != null, and the value is either empty, yes or true.
     *
     * @param commandName
     * @param commandMap
     * @return
     */
    public static boolean checkYesNoCommand(String commandName, Map<String, String> commandMap) {
        String argument = commandMap.get(commandName);

        return (argument != null)
                && (argument.isEmpty() || "yes".equals(argument) || "true".equals(argument));
    }
}
