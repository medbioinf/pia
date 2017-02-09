package de.mpc.pia.intermediate.compiler.parser.searchengines;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.Param;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationProtocol;
import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.parser.InputFileParserFactory;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.tools.CleavageAgent;
import de.mpc.pia.tools.MzIdentMLTools;
import de.mpc.pia.tools.OntologyConstants;
import de.mpc.pia.tools.unimod.jaxb.ModT;
import de.mpc.pia.tools.unimod.jaxb.SpecificityT;


/**
 * This class parses the data from a tide TXT file for a given
 * {@link PIACompiler}.<br/>
 *
 * @author julian
 *
 */
public class TideTXTFileParser {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(TideTXTFileParser.class);

    /** the separator in the TSV/CSV file */
    public static final String SEPARATOR_STRING = "\t";


    public static final String HEADER_CHARGE = "charge";
    public static final String HEADER_PRECURSOR_MZ = "spectrum precursor m/z";
    public static final String HEADER_SPECTRUM_NEUTRAL_MASS = "peptide mass";
    public static final String HEADER_PEPTIDE_MASS = "peptide mass";
    public static final String HEADER_SEQUENCE = "sequence";
    public static final String HEADER_CLEAVAGE_TYPE = "cleavage type";
    public static final String HEADER_SCAN = "scan";
    public static final String HEADER_XCORR = "xcorr score";
    public static final String HEADER_PROTEINID = "protein id";

    /** pattern to match and grep the accession */
    private static final Pattern patternAccessions = Pattern.compile("([^(]+)\\(\\d+\\)");


    /** the names of the columns */
    private static final List<String> colNames = Arrays.asList(
            "file", HEADER_SCAN,
            HEADER_CHARGE, HEADER_PRECURSOR_MZ,
            HEADER_SPECTRUM_NEUTRAL_MASS, HEADER_PEPTIDE_MASS, "delta_cn",
            HEADER_XCORR, "xcorr rank",
            "distinct matches/spectrum", HEADER_SEQUENCE,
            HEADER_CLEAVAGE_TYPE,
            HEADER_PROTEINID, "flanking aa");

    /**
     * We don't ever want to instantiate this class
     */
    private TideTXTFileParser() {
        throw new AssertionError();
    }


    /**
     * Parses the data from an tide TXT result file given by its name into the
     * given {@link PIACompiler}.
     *
     * @param name name if the file in the compilation
     * @param fileName name of the Tide TXT result file
     * @param compiler the PIACompiler
     */
    public static boolean getDataFromTideTXTFile(String name, String fileName,
            PIACompiler compiler) {
        int accNr = 0;
        int pepNr = 0;
        int specNr = 0;

        String line;
        int lineNr = 0;

        Map<String, Integer> columnMap = new HashMap<>(colNames.size());

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            if ((line = br.readLine()) != null) {
                lineNr++;

                // the first line contains the headers, create the mapping
                String[] headers = line.split(SEPARATOR_STRING);

                for (int idx = 0; idx < headers.length; idx++) {
                    if (colNames.contains(headers[idx])) {
                        columnMap.put(headers[idx], idx);
                    }
                }

                if (columnMap.get(HEADER_SEQUENCE) == null) {
                    LOGGER.error("the sequence header is missing");
                    br.close();
                    return false;
                } else if (columnMap.get(HEADER_PROTEINID) == null) {
                    LOGGER.error("the proteinid (accession) header is missing");
                    br.close();
                    return false;
                }
            }

            PIAInputFile file = compiler.insertNewFile(name, fileName, InputFileParserFactory.InputFileTypes.TIDE_TXT_INPUT.getFileSuffix());

            // create the analysis software and add it to the compiler
            AnalysisSoftware tide = new AnalysisSoftware();

            tide.setId("crux-tide");
            tide.setName("crux-tide");
            tide.setUri("http://cruxtoolkit.sourceforge.net/tide-search.html");

            tide = compiler.putIntoSoftwareMap(tide);


            // define the spectrumIdentificationProtocol
            SpectrumIdentificationProtocol spectrumIDProtocol =
                    new SpectrumIdentificationProtocol();

            spectrumIDProtocol.setId("tandemAnalysis");
            spectrumIDProtocol.setAnalysisSoftware(tide);

            Param param = new Param();
            param.setParam(MzIdentMLTools.createPSICvParam(OntologyConstants.MS_MS_SEARCH, null));
            spectrumIDProtocol.setSearchType(param);

            file.addSpectrumIdentificationProtocol(spectrumIDProtocol);


            // add the spectrum identification
            SpectrumIdentification spectrumID = new SpectrumIdentification();
            spectrumID.setId("tideIdentification");
            spectrumID.setSpectrumIdentificationList(null);
            spectrumID.setSpectrumIdentificationProtocol(spectrumIDProtocol);

            file.addSpectrumIdentification(spectrumID);


            // now parse the lines, each line is one PSM
            while ((line = br.readLine()) != null) {
                lineNr++;

                String[] columns = line.split(SEPARATOR_STRING);
                Peptide peptide;

                Integer charge;
                try {
                    charge = Integer.parseInt(columns[columnMap.get(HEADER_CHARGE)]);
                } catch (Exception ex) {
                    LOGGER.error("could not parse the chargestate in line " + lineNr, ex);
                    charge = 0;
                }

                Double precursorMZ;
                try {
                    precursorMZ = Double.parseDouble(columns[columnMap.get(HEADER_PRECURSOR_MZ)]);
                } catch (Exception ex) {
                    LOGGER.error("could not parse the precursor m/z in line " + lineNr, ex);
                    precursorMZ = Double.NaN;
                }

                // TODO: implement the delta mass, it is too imprecise to calculate from the given values
                // double deltaMass = Double.parseDouble(columns[columnMap.get(HEADER_PEPTIDE_MASS)])
                //     - Double.parseDouble(columns[columnMap.get(HEADER_SPECTRUM_NEUTRAL_MASS)]);
                double deltaMass = Double.NaN;

                String sequence = columns[columnMap.get(HEADER_SEQUENCE)];

                Map<Integer, Modification> modifications = new HashMap<>();
                if (sequence.contains("[")) {
                    sequence = extractModifications(sequence, modifications, compiler);
                }

                int missedCleavages = calculateMissed(sequence, columns[columnMap.get(HEADER_CLEAVAGE_TYPE)]);

                String sourceID = "index=" + columns[columnMap.get(HEADER_SCAN)];

                PeptideSpectrumMatch psm = compiler.createNewPeptideSpectrumMatch(
                        charge,
                        precursorMZ,
                        deltaMass,
                        null,
                        sequence,
                        missedCleavages,
                        sourceID,
                        null,
                        file,
                        spectrumID);

                // get the peptide or create it
                peptide = compiler.getPeptide(sequence);
                if (peptide == null) {
                    peptide = compiler.insertNewPeptide(sequence);
                    pepNr++;
                }

                // add the spectrum to the peptide
                peptide.addSpectrum(psm);

                // add the modifications
                for (Map.Entry<Integer, Modification> mod
                        : modifications.entrySet()) {
                    psm.addModification(mod.getKey(), mod.getValue());
                }


                // add the scores
                ScoreModel score;

                Double scoreValue;
                try {
                    scoreValue = Double.parseDouble(columns[columnMap.get(HEADER_XCORR)]);
                    score = new ScoreModel(scoreValue,
                            ScoreModelEnum.SEQUEST_XCORR);
                    psm.addScore(score);

                } catch (Exception e) {
                    LOGGER.error("could not parse the xcorr in line " + lineNr, e);
                }
                // TODO: add the other score( like value)s


                // add the protein/accession info

                String[] accessions = columns[columnMap.get(HEADER_PROTEINID)].split(",");
                for (String accession : accessions) {

                    Matcher matcher = patternAccessions.matcher(accession);

                    if (matcher.matches()) {
                        // add the Accession to the compiler (if it is not already there)
                        Accession acc = compiler.getAccession(matcher.group(1));
                        if (acc == null) {
                            // no sequence information in the file
                            acc = compiler.insertNewAccession(
                                    matcher.group(1), null);
                            accNr++;
                        }

                        acc.addFile(file.getID());

                        // now insert the connection between peptide and accession into the compiler
                        compiler.addAccessionPeptideConnection(acc, peptide);
                    } else {
                        LOGGER.error("could not get the accession in line" + lineNr);
                    }
                }

                // teh PSM is completed now
                compiler.insertCompletePeptideSpectrumMatch(psm);
                specNr++;
            }
        } catch (IOException e) {
            LOGGER.error("Error occurred while parsing the file " + fileName, e);
            return false;
        }

        LOGGER.info("inserted new: \n\t" +
                pepNr + " peptides\n\t" +
                specNr + " peptide spectrum matches\n\t" +
                accNr + " accessions");
        return true;
    }


    /**
     * extracts the modifications from the seqeunce string
     *
     * @param modificationsSequence the string with modifications
     * @param modifications the modifications map
     * @param compiler the PIACompiler
     * @return
     */
    private static String extractModifications(String modificationsSequence,
            Map<Integer, Modification> modifications, PIACompiler compiler) {
        if (modifications == null) {
            LOGGER.error("Modifications map not initialized!");
            return null;
        }

        StringBuilder sequence = new StringBuilder(modificationsSequence.length());

        int pos;
        while ( -1 < (pos = modificationsSequence.indexOf('['))) {
            sequence.append(modificationsSequence.substring(0, pos));
            modificationsSequence = modificationsSequence.substring(pos);

            int openBr = 0;
            StringBuilder modWeight = new StringBuilder();
            for (int p=1; p < modificationsSequence.length(); p++) {
                char c = modificationsSequence.charAt(p);
                if (c == '[') {
                    openBr++;
                } else if (c == ']') {
                    openBr--;
                    if (openBr < 0) {
                        break;
                    }
                }

                modWeight.append(c);
            }

            int loc = sequence.length();
            String residue = "" + sequence.charAt(loc-1);

            Double massShift;
            ModT unimod = null;
            try {
                massShift = Double.parseDouble(modWeight.toString());
                unimod = compiler.getUnimodParser().getModificationByMass(
                        massShift, residue);
            } catch (NumberFormatException e) {
                LOGGER.error("could not parse mass of modification: " + modWeight, e);
            }


            modificationsSequence =
                    modificationsSequence.substring(modWeight.length() + 2);

            if (unimod != null) {
                if (loc == 1) {
                    // check for N-terminal modifications
                    for (SpecificityT spec : unimod.getSpecificity()) {
                        if (spec.getSite().contains("N-term")) {
                            loc = 0;
                            residue = ".";
                        }
                    }
                } else if (modificationsSequence.length() == 0) {
                    // TODO: check for C-terminal modifications
                }

                Modification mod = new Modification(
                        residue.charAt(0),
                        unimod.getDelta().getMonoMass(),
                        unimod.getTitle(),
                        "UNIMOD:" + unimod.getRecordId());

                modifications.put(loc, mod);
            } else {
                LOGGER.error("Could not get information for " +
                        "modification " + modWeight + "@" + residue + " in " +
                        sequence);
            }
        }

        sequence.append(modificationsSequence);
        return sequence.toString();
    }


    /**
     * calculates the number of missed cleavages
     *
     * @param sequence the peptide seqeunce
     * @param cleavageType the cleavage type as stated in the tide TXT file output
     * @return
     */
    private static int calculateMissed(String sequence, String cleavageType) {
        int missed = 0;
        String type = cleavageType.trim();
        CleavageAgent enzyme = null;

        // TODO: add further cleavage agents
        if (type.equals("trypsin-full-digest")) {
            enzyme = CleavageAgent.TRYPSIN;
        }

        if (enzyme != null) {
            missed += sequence.split(enzyme.getSiteRegexp()).length - 1;
        } else {
            missed = -1;
        }

        return missed;
    }
}
