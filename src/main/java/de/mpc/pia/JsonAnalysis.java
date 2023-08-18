package de.mpc.pia;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import de.mpc.pia.modeller.protein.inference.ProteinInferenceFactory.ProteinInferenceMethod;
import de.mpc.pia.modeller.protein.scoring.ProteinScoringFactory;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.ScoreModelEnum;


public class JsonAnalysis {

	// logger for this class
	private static final Logger LOGGER = LogManager.getLogger();
	
	// global settings
	private boolean considerModifications = false;
	private boolean createPSMsets = true;
	private boolean errorOnNoDecoys = true;
	private Long[] calculateFDRFileIDs;
	private boolean calculateAllFDR = true;
	private String decoyPattern;
	private int topIdentifications = 0;

	// PSM settings
	private long psmLevelFileID = 0L;
	private boolean calculateCombinedFDRScore = true;
	private String[] preferredFDRScores;
	private String[] psmFilters;
	private String psmExportFile;
	
	// peptide settings
	private boolean inferePeptides = true;
	private long peptideLevelFileID = 0L;
	private String[] peptideFilters;
	private boolean peptideExportWithPSMs = true;
	private String peptideExportFile;
	
	// protein settings
	private boolean infereProteins = true;
	private boolean calculateProteinFDR = true;
	private String inferenceMethod;
	private String[] inferenceFilters;
	private String scoringMethod;
	private String scoringBaseScore;
	private String scoringPSMs;
	private String[] proteinFilters;
	private String proteinExportFile;
	private boolean proteinExportWithPSMs = false;
	private boolean proteinExportWithPeptides = false;
	private boolean proteinExportWithProteinSequences = false;
	
	
	/**
	 * Reads the data from the provided JSON file
	 * 
	 * @param jsonFile
	 * @return
	 */
	public static JsonAnalysis readFromFile(File jsonFile) {
		Gson gson = new Gson();
		JsonAnalysis jsonAnalysis = null;
		
		try (Reader reader = Files.newBufferedReader(jsonFile.toPath())) {
			jsonAnalysis = gson.fromJson(reader, JsonAnalysis.class);
		} catch (IOException e) {
            LOGGER.error("Error while parsing JSON file {}, does it exist and is well formatted?",jsonFile, e);
		} catch (JsonParseException e) {
            LOGGER.error("Problem with the parsing of the JSON file", e);
            throw e;
		}
		
		return jsonAnalysis;
	}
	
	
	@Override
	public String toString() {
		GsonBuilder builder = new GsonBuilder();
		builder.setPrettyPrinting().serializeNulls();
		builder.setPrettyPrinting().disableHtmlEscaping();
		Gson gson = builder.create();
		
		return gson.toJson(this);
	}

	
	public void setToDefaults() {
		considerModifications = false;
		createPSMsets = true;
		errorOnNoDecoys = true;
		calculateFDRFileIDs =new Long[] {1L};
		calculateAllFDR = true;
		decoyPattern = "DECOY_.*";
		topIdentifications = 0;

		psmLevelFileID = 0;
		calculateCombinedFDRScore = true;
		preferredFDRScores = new String[] {};
		psmFilters = new String[] {"psm_score_filter_psm_combined_fdr_score <= 0.01"};
		psmExportFile = "/tmp/piaExport-PSMs.mzTab";
		
		inferePeptides = true;
		peptideLevelFileID = 0;
		peptideFilters = new String[] {"psm_score_filter_psm_combined_fdr_score <= 0.01"};
		peptideExportWithPSMs = true;
		peptideExportFile = "/tmp/piaExport-peptides.csv";
		
		// protein settings
		infereProteins = true;
		calculateProteinFDR = true;
		inferenceMethod = ProteinInferenceMethod.REPORT_OCCAMS_RAZOR.getShortName();
		inferenceFilters = new String[] {"psm_score_filter_psm_combined_fdr_score <= 0.01"};
		scoringMethod = ProteinScoringFactory.ScoringType.MULTIPLICATIVE_SCORING.getShortName();
		scoringBaseScore = ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName();
		scoringPSMs = "best";	//best or all
		proteinFilters = new String[] {"protein_q_value_filter <= 0.01"};
		proteinExportFile = "/tmp/piaExport-proteins.mzid";
		proteinExportWithPSMs = false;
		proteinExportWithPeptides = false;
		proteinExportWithProteinSequences = false;
	}


	public boolean isConsiderModifications() {
		return considerModifications;
	}


	public boolean isCreatePSMsets() {
		return createPSMsets;
	}


	public boolean isErrorOnNoDecoys() {
		return errorOnNoDecoys;
	}


	public long getPsmLevelFileID() {
		return psmLevelFileID;
	}
	
	
	public boolean isCalculateAllFDR() {
		return calculateAllFDR;
	}


	public boolean isCalculateCombinedFDRScore() {
		return calculateCombinedFDRScore;
	}


	public String getDecoyPattern() {
		if (decoyPattern == null) {
			decoyPattern = FDRData.DecoyStrategy.SEARCHENGINE.toString();
		}
		return decoyPattern;
	}


	public int getTopIdentifications() {
		return topIdentifications;
	}


	public String[] getPreferredFDRScores() {
		if (preferredFDRScores == null) {
			preferredFDRScores = new String[] {};
		}
		return preferredFDRScores;
	}


	public String[] getPsmFilters() {
		if (psmFilters == null) {
			psmFilters = new String[] {};
		}
		return psmFilters;
	}


	public String getPsmExportFile() {
		return psmExportFile;
	}


	public boolean isInferePeptides() {
		return inferePeptides;
	}


	public long getPeptideLevelFileID() {
		return peptideLevelFileID;
	}


	public String[] getPeptideFilters() {
		if (peptideFilters == null) {
			peptideFilters = new String[] {};
		}
		return peptideFilters;
	}


	public boolean isPeptideExportWithPSMs() {
		return peptideExportWithPSMs;
	}


	public String getPeptideExportFile() {
		return peptideExportFile;
	}


	public boolean isInfereProteins() {
		return infereProteins;
	}


	public String getInferenceMethod() {
		return inferenceMethod;
	}


	public String[] getInferenceFilters() {
		if (inferenceFilters == null) {
			inferenceFilters = new String[] {};
		}
		return inferenceFilters;
	}


	public String getScoringMethod() {
		return scoringMethod;
	}


	public String getScoringBaseScore() {
		return scoringBaseScore;
	}


	public String getScoringPSMs() {
		return scoringPSMs;
	}


	public String[] getProteinFilters() {
		if (proteinFilters == null) {
			proteinFilters = new String[] {};
		}
		return proteinFilters;
	}


	public String getProteinExportFile() {
		return proteinExportFile;
	}


	public boolean isProteinExportWithPSMs() {
		return proteinExportWithPSMs;
	}


	public boolean isProteinExportWithPeptides() {
		return proteinExportWithPeptides;
	}


	public boolean isProteinExportWithProteinSequences() {
		return proteinExportWithProteinSequences;
	}


	public Long[] getCalculateFDRFileIDs() {
		if (calculateFDRFileIDs == null) {
			calculateFDRFileIDs = new Long[] {};
		}
		return calculateFDRFileIDs;
	}


	public boolean isCalculateProteinFDR() {
		return calculateProteinFDR;
	}
}