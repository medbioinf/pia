package de.mpc.pia.intermediate.compiler.parser.searchengines;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.parser.FastaHeaderInfos;
import de.mpc.pia.intermediate.compiler.parser.InputFileParserFactory;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.tools.MzIdentMLTools;
import de.mpc.pia.tools.OntologyConstants;
import de.mpc.pia.tools.PIATools;
import de.mpc.pia.tools.unimod.jaxb.ModT;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.Param;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationProtocol;

/*
 * This class parses the data from a Comet TSV file for a given {@link PIACompiler}.<br/>
 */
public class CometTSVFileParser {

	/** logger for this class */
	private static final Logger LOGGER = LogManager.getLogger();

	/** the separator in the TSV/CSV file */
	private static final String SEPARATOR_STRING = "\t";

	public static final String HEADER_SCAN = "scan";
	// num
	public static final String HEADER_CHARGE = "charge";
	public static final String HEADER_EXP_NEUTRAL_MASS = "exp_neutral_mass";
	public static final String HEADER_CALC_NEUTRAL_MASS = "calc_neutral_mass";
	public static final String HEADER_E_VALUE = "e-value";
	public static final String HEADER_XCORR = "xcorr";
	public static final String HEADER_DELTA_CN = "delta_cn";
	public static final String HEADER_SP_SCORE = "sp_score";
	// ions_matched
	// ions_total
	public static final String HEADER_PLAIN_PEPTIDE = "plain_peptide";
	// modified_peptide
	// prev_aa
	// next_aa
	public static final String HEADER_PROTEIN = "protein";
	// protein_count
	// modifications
	public static final String HEADER_MODIFICATIONS = "modifications";
	public static final String HEADER_RETENTION_TIME_SEC = "retention_time_sec";
	public static final String HEADER_SP_RANK = "sp_rank";
	// 


	/** the names of the columns */
	private static final List<String> colNames = Arrays.asList(
			HEADER_SCAN,
			HEADER_CHARGE,
			HEADER_EXP_NEUTRAL_MASS,
			HEADER_CALC_NEUTRAL_MASS,
			HEADER_E_VALUE,
			HEADER_XCORR,
			HEADER_DELTA_CN,
			HEADER_SP_SCORE,
			HEADER_PLAIN_PEPTIDE,
			HEADER_PROTEIN,
			HEADER_MODIFICATIONS,
			HEADER_RETENTION_TIME_SEC,
			HEADER_SP_RANK
			);

	/**
	 * We don't ever want to instantiate this class
	 */
	private CometTSVFileParser() {
		throw new AssertionError();
	}


	/**
	 * Parses the data from a Comet TSV result file given by its name into the
	 * given {@link PIACompiler}.
	 *
	 * @param name name if the file in the compilation
	 * @param fileName name of the Comet TSV result file
	 * @param compiler the PIACompiler
	 */
	public static boolean getDataFromCometTSVFile(String name, String fileName,
			PIACompiler compiler) {
		int accNr = 0;
		int pepNr = 0;
		int specNr = 0;

		String line;
		int lineNr = 0;

		Map<String, Integer> columnMap = Collections.emptyMap();
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {

			// go through the lines until a column header map is created
			while (columnMap.isEmpty() && ((line = br.readLine()) != null)) {
				columnMap = buildColumnMap(line);
				lineNr++;
			}

			PIAInputFile file = compiler.insertNewFile(name, fileName, InputFileParserFactory.InputFileTypes.COMET_TSV_INPUT.getFileSuffix());

			// create the analysis software and add it to the compiler
			AnalysisSoftware comet = new AnalysisSoftware();

			comet.setId("comet");
			comet.setName("comet");
			comet.setUri("https://uwpr.github.io/Comet/");

			comet = compiler.putIntoSoftwareMap(comet);

			// define the spectrumIdentificationProtocol
			SpectrumIdentificationProtocol spectrumIDProtocol =
					new SpectrumIdentificationProtocol();

			spectrumIDProtocol.setId("cometAnalysis");
			spectrumIDProtocol.setAnalysisSoftware(comet);

			Param param = new Param();
			param.setParam(MzIdentMLTools.createPSICvParam(OntologyConstants.MS_MS_SEARCH, null));
			spectrumIDProtocol.setSearchType(param);

			file.addSpectrumIdentificationProtocol(spectrumIDProtocol);


			// add the spectrum identification
			SpectrumIdentification spectrumID = new SpectrumIdentification();
			spectrumID.setId("cometIdentification");
			spectrumID.setSpectrumIdentificationList(null);
			spectrumID.setSpectrumIdentificationProtocol(spectrumIDProtocol);

			file.addSpectrumIdentification(spectrumID);

			// now parse the lines, each line is one PSM
			while ((line = br.readLine()) != null) {
				lineNr++;

				String[] columns = line.split(SEPARATOR_STRING);

				Integer charge = parseCharge(columns, columnMap, lineNr);
				Double precursorMZ = parsePrecursorMZ(columns, columnMap, lineNr);
				Double deltaMass = parseAndCalculateDeltaMass(columns, columnMap, lineNr, precursorMZ);
				String sequence = columns[columnMap.get(HEADER_PLAIN_PEPTIDE)];

				String modificationsString = columns[columnMap.get(HEADER_MODIFICATIONS)];
				Map<Integer, Modification> modifications = new HashMap<>();
                if (!"-".equals(modificationsString) &&
                		!extractModifications(modificationsString, sequence, modifications, compiler)) {
                	return false;
                }

				// Missed cleavages and cleavage enzyme are not given in the CSV file and hence cannot be calculated!
				//   these need to be added by parsing the parameter file!

				String sourceID = "index=" + columns[columnMap.get(HEADER_SCAN)];
				
				Double retentionTime = parseRetentionTime(columns, columnMap, lineNr);

				PeptideSpectrumMatch psm = compiler.createNewPeptideSpectrumMatch(
						charge,
						precursorMZ,
						deltaMass,
						retentionTime,
						sequence,
						-1,
						sourceID,
						null,
						file,
						spectrumID);

				// get the peptide or create it
				Peptide peptide = compiler.getPeptide(sequence);
				if (peptide == null) {
					peptide = compiler.insertNewPeptide(sequence);
					pepNr++;
				}

				// add the spectrum to the peptide
				peptide.addSpectrum(psm);

				// add the modifications
				for (Map.Entry<Integer, Modification> mod : modifications.entrySet()) {
					psm.addModification(mod.getKey(), mod.getValue());
				}


				// add the scores
				addScores(columns, columnMap, psm);

				// add the protein/accession info
				String[] accessions = columns[columnMap.get(HEADER_PROTEIN)].split(",");
				for (String accession : accessions) {
					FastaHeaderInfos fastaHeader = FastaHeaderInfos.parseHeaderInfos(accession);

					// add the Accession to the compiler (if it is not already there)
					Accession acc = compiler.getAccession(fastaHeader.getAccession());
					if (acc == null) {
						// no sequence information in the file
						acc = compiler.insertNewAccession(fastaHeader.getAccession(), null);
						accNr++;
					}

					acc.addFile(file.getID());

					// now insert the connection between peptide and accession into the compiler
					compiler.addAccessionPeptideConnection(acc, peptide);
				}

				// the PSM is completed now
				compiler.insertCompletePeptideSpectrumMatch(psm);
				specNr++;
			}
		} catch (IOException e) {
			LOGGER.error("Error occurred while parsing the file {}", fileName, e);
			return false;
		}

		LOGGER.info("""
				inserted new:
				\t{} peptides,
				\t{} peptide spectrum matches,
				\t{} accessions""", pepNr, specNr, accNr);

		return true;
	}

	
	/**
	 * Build the column header mapping from the respective line
	 * 
	 * @param line
	 * @return
	 */
	private static Map<String, Integer> buildColumnMap(String line) {
		if (line.trim().matches("CometVersion.+")) {
			// this is not the correct line yet
			return Collections.emptyMap();
		}

		Map<String, Integer> columnMap = HashMap.newHashMap(colNames.size());
		String[] headers = line.split(SEPARATOR_STRING);

		for (int idx = 0; idx < headers.length; idx++) {
			if (colNames.contains(headers[idx])) {
				columnMap.put(headers[idx], idx);
			}
		}

		if (columnMap.get(HEADER_PLAIN_PEPTIDE) == null) {
			LOGGER.warn("the sequence header is missing, trying next line");
			return Collections.emptyMap();
		} else if (columnMap.get(HEADER_PROTEIN) == null) {
			LOGGER.warn("the proteinid (accession) header is missing, trying next line");
			return Collections.emptyMap();
		}

		return columnMap;
	}


	/**
	 * Checks, whether the given file looks like a Comet CSV file
	 *
	 * @param fileName
	 * @return
	 */
	public static boolean checkFileType(String fileName) {
		boolean isCometCSVFile = false;
		LOGGER.debug("checking whether this is a Comet CSV file: {}", fileName);

		try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
			// read in the first 10, not empty lines
			List<String> lines = stream.filter(line -> !line.trim().isEmpty())
					.limit(10).
					toList();

			// check, if first lines are OK
			int idx = 0;

			// there may be an optional "CometVersion ..." line, skip this
			if (lines.get(idx).trim().matches("CometVersion.+")) {
				LOGGER.debug("file has the 'CometVersion' declaration line: {}", lines.get(idx));
				idx++;
			}

			// ... and a mandatory header line
			int countOK = 0;
			int countFalse = 0;
			boolean foundProtein = false;
			boolean foundSequence = false;

			for (String header : lines.get(idx).split(SEPARATOR_STRING)) {
				if (colNames.contains(header)) {
					countOK++;
				} else {
					countFalse++;
				}

				if (header.equals(HEADER_PROTEIN)) {
					foundProtein = true;
				} else if (header.equals(HEADER_PLAIN_PEPTIDE)) {
					foundSequence = true;
				}
			}

			isCometCSVFile = (countOK >= 2)
					&& foundProtein
					&& foundSequence;

			LOGGER.debug("ok: {}, false: {}, protein: {}, sequence: {}", countOK, countFalse, foundProtein, foundSequence);
		} catch (Exception e) {
			LOGGER.debug("Could not check file {}", fileName, e);
		}

		return isCometCSVFile;
	}


	/**
	 * Parses the charge state from the already separated line in the CSV given by columns
	 * 
	 * @param columns
	 * @param columnMap
	 * @param lineNr
	 * @return
	 */
	private static Integer parseCharge(String[] columns, Map<String, Integer> columnMap, int lineNr) {
		Integer charge;
		try {
			charge = Integer.parseInt(columns[columnMap.get(HEADER_CHARGE)]);
		} catch (Exception ex) {
			LOGGER.error("could not parse the chargestate in line {}", lineNr, ex);
			charge = 0;
		}

		return charge;
	}


	/**
	 * Parses the precursor m/z (exp_neutral_mass) state from the already separated line in the CSV given by columns
	 * 
	 * @param columns
	 * @param columnMap
	 * @param lineNr
	 * @return
	 */
	private static Double parsePrecursorMZ(String[] columns, Map<String, Integer> columnMap, int lineNr) {
		Double precursorMZ;

		try {
			precursorMZ = Double.parseDouble(columns[columnMap.get(HEADER_EXP_NEUTRAL_MASS)]);
		} catch (Exception ex) {
			LOGGER.error("could not parse the precursor m/z in line {}", lineNr, ex);
			precursorMZ = Double.NaN;
		}

		return precursorMZ;
	}


	private static Double parseAndCalculateDeltaMass(String[] columns, Map<String, Integer> columnMap, int lineNr, Double precursorMZ) {
		Double deltaMass = Double.NaN;
		try  {
			Double calcNeutralMass = Double.parseDouble(columns[columnMap.get(HEADER_CALC_NEUTRAL_MASS)]);
			if (Double.NaN != precursorMZ) {
				deltaMass = PIATools.round(precursorMZ-calcNeutralMass, 6);
			}
		} catch (Exception ex) {
			LOGGER.error("could not parse the calculated m/z in line {}", lineNr, ex);
			deltaMass = Double.NaN;
		}

		return deltaMass;
	}
	
	
	/**
	 * Parses the retention time in seconds
	 * 
	 * @param columns
	 * @param columnMap
	 * @param lineNr
	 * @return retention time in seconds
	 */
	private static Double parseRetentionTime(String[] columns, Map<String, Integer> columnMap, int lineNr) {
		Double retentionTime;

		try {
			retentionTime = Double.parseDouble(columns[columnMap.get(HEADER_RETENTION_TIME_SEC)]);
		} catch (Exception ex) {
			LOGGER.error("could not parse the retention time in line {}", lineNr, ex);
			retentionTime = Double.NaN;
		}

		return retentionTime;
	}
	
	
	/**
	 * extracts the modifications from the "modified_peptide" column
	 *
	 * @param modificationsSequence the string with modifications
	 * @param modifications         the modifications map
	 * @param compiler              the PIACompiler
	 * @return
	 */
	private static boolean extractModifications(String modificationsString, String pepSequence,
			Map<Integer, Modification> modifications, PIACompiler compiler) {
		if (modifications == null) {
			LOGGER.error("Modifications map not initialized!");
			return false;
		}

		String[] splitMods = modificationsString.split(",");

		boolean returnState = true;
		for (String modStr : splitMods) {
			try {
				String[] splitMod = modStr.split("_");
				int loc = Integer.parseInt(splitMod[0]);
				Double massShift = Double.parseDouble(splitMod[2]);
				Character residue = pepSequence.charAt(loc - 1);

				ModT unimod = null;
				unimod = compiler.getUnimodParser().getModificationByMass(massShift, residue.toString());
				String description = modStr;
				String accession = modStr;

				if (unimod != null) {
					massShift = unimod.getDelta().getMonoMass();
					description = unimod.getTitle();
					accession = "UNIMOD:" + unimod.getRecordId();
				}

				Modification mod = new Modification(residue, massShift, description, accession);
				modifications.put(loc, mod);
			} catch (NumberFormatException e) {
				LOGGER.error("could not parse modification: {}", modStr, e);
				returnState = false;
			}
		}

		return returnState;
	}
    
	
	/**
	 * Adds the scores to the PSM for the already separated line in the CSV given by columns
	 * 
	 * @param columns
	 * @param columnMap
	 * @param psm
	 */
	private static void addScores(String[] columns, Map<String, Integer> columnMap, PeptideSpectrumMatch psm) {
		if(columnMap.get(HEADER_E_VALUE) != null ){
			Double scoreValue = Double.parseDouble(columns[columnMap.get(HEADER_E_VALUE)]);
			ScoreModel score = new ScoreModel(scoreValue, ScoreModelEnum.COMET_EXPECTATION);
			psm.addScore(score);
		}
		
		if(columnMap.get(HEADER_XCORR) != null ){
			Double scoreValue = Double.parseDouble(columns[columnMap.get(HEADER_XCORR)]);
			ScoreModel score = new ScoreModel(scoreValue, ScoreModelEnum.COMET_XCORR);
			psm.addScore(score);
		}

		if(columnMap.get(HEADER_DELTA_CN) != null ){
			Double scoreValue = Double.parseDouble(columns[columnMap.get(HEADER_DELTA_CN)]);
			ScoreModel score = new ScoreModel(scoreValue, ScoreModelEnum.COMET_DELTA_CN);
			psm.addScore(score);
		}

		if(columnMap.get(HEADER_SP_SCORE) != null ){
			Double scoreValue = Double.parseDouble(columns[columnMap.get(HEADER_SP_SCORE)]);
			ScoreModel score = new ScoreModel(scoreValue, ScoreModelEnum.COMET_SPSCORE);
			psm.addScore(score);
		}

		if(columnMap.get(HEADER_SP_RANK) != null ){
			Double scoreValue = Double.parseDouble(columns[columnMap.get(HEADER_SP_RANK)]);
			ScoreModel score = new ScoreModel(scoreValue, ScoreModelEnum.COMET_SPRANK);
			psm.addScore(score);
		}
	}

}
