package de.mpc.pia.tools;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;


/**
 * Some handy functions.
 * @author julian
 *
 */
public class PIATools {

    /**
     * We don't ever want to instantiate this class
     */
    private PIATools() {
        throw new AssertionError();
    }


    /**
     * Checks whether both objects are null or are equal.
     */
    public  static boolean bothNullOrEqual(Object x, Object y) {
        return ( x == null ? y == null : x.equals(y) );
    }


    /**
     * Round a double value and keeping (at max) the given number of decimal
     * places.
     *
     * @param value
     * @param dec
     * @return
     */
    public static double round(double value, int dec) {
        double factor = Math.pow(10, dec);
        return Math.round(value * factor) / factor;
    }


    /**
     * Calls the compareTo for the given objects, if none of them are null. If
     * one of them is null, null is ordered before the other. If both are null,
     * they are ccompared as equal.
     *
     * @param o1
     * @param o2
     * @return
     */
    public static <T extends Comparable<T>> int CompareProbableNulls(T o1, T o2) {
        if (o1 == null) {
            if (o2 == null) {
                return 0;
            } else {
                return -1;
            }
        } else {
            if (o2 == null) {
                return 1;
            } else {
                return o1.compareTo(o2);
            }
        }
    }


    /**
     * This method return the full path of specified subPath.
     */
    public static URL getFullPath(Class cs, String subPath)
            throws MalformedURLException {
        if (cs == null) {
            throw new IllegalArgumentException("Input class cannot be NULL");
        }

        URL fullPath = null;

        CodeSource src = cs.getProtectionDomain().getCodeSource();
        if (src != null) {
            if (subPath == null) {
                fullPath = src.getLocation();
            } else {
                fullPath = new URL(src.getLocation(), subPath);
            }
        }

        return fullPath;
    }


    /**
     * Print out the help, given the options
     * @param options
     */
    public static void printCommandLineHelp(String className, Options options, String header) {
        HelpFormatter formatter = new HelpFormatter();

        formatter.printHelp(className,
                header + "\nOptions:",
                options,
                "\nPIA - Protein Inference Algorithms, version " + PIAConstants.version +
                "\nCopyright (C) 2013-2015 Medizinisches Proteom-Center, " +
                "julian.uszkoreit@rub.de" +
                "\nThis is free software; see the source for copying conditions. " +
                "There is ABSOLUTELY NO warranty!",
                true);
    }


    /**
     * Returns the first complete occurance of the first peptide in the second,
     * with awareness of the special amino acid X.
     *
     * @param first
     * @param second
     * @return the position of the first sequence relative to the second (-1 for
     * no match)
     */
    public static int longestCommonPeptide(String first, String second) {
        if (first == null || second == null || first.length() == 0 || second.length() == 0) {
            return -1;
        }

        int maxLen = 0;
        int maxPos = -1;
        int fl = first.length();
        int sl = second.length();
        int[][] table = new int[fl+1][sl+1];

        for(int s=0; s <= sl; s++) {
            table[0][s] = 0;
        }
        for(int f=0; f <= fl; f++){
            table[f][0] = 0;
        }

        for (int i = 1; i <= fl; i++) {
            for (int j = 1; j <= sl; j++) {
                if ((first.charAt(i-1) == second.charAt(j-1)) ||
                        (first.charAt(i-1) == 'X') || (second.charAt(j-1) == 'X')){
                    if (i == 1 || j == 1) {
                        table[i][j] = 1;
                    } else {
                        table[i][j] = table[i - 1][j - 1] + 1;
                    }

                    if (table[i][j] > maxLen) {
                        maxLen = table[i][j];
                        maxPos = j - maxLen;
                    }
                }
            }
        }

        if (maxLen == fl) {
            return maxPos;
        } else {
            return -1;
        }
    }
}
