package de.mpc.pia.modeller.exporter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.score.comparator.Rankable;

public class CSVExporter {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(CSVExporter.class);

    /** the value separator */
    public static final String SEPARATOR = ",";
    /** the value separator for lists in one CSV value */
    public static final String MULTIVALUE_SEPARATOR = ";";

    /** the newline string */
    public static final String NEWLINE = "\n";


    /** the modeller, that should be exported */
    private PIAModeller piaModeller;
    /** the writer used to export the mzTab file */
    private BufferedWriter outWriter;

    /** the export fileID */
    private Long exportFileID;

    /** whether the export shoudl be filtered */
    private boolean filterExport;

    /** whether the PSM level shoudl be exported */
    private boolean psmLevel;
    /** whether the peptide level shoudl be exported */
    private boolean peptideLevel;
    /** whether the protein level shoudl be exported */
    private boolean proteinLevel;

    /** are modifications considered for peptide inference */
    private boolean considerModifications;

    /** is FDR on protein level calculated, for exported file/view */
    private boolean proteinFDR;

    /** should PSM sets be exported */
    private boolean includePSMSets;

    /** mapping from the schoreShorts to the score names */
    private Map<String, String> scoreShortsToNames;

    // the column headers
    private static final String HEADER_COLS_PROTEIN = "\"COLS_PROTEIN\"";
    private static final String HEADER_COLS_PEPTIDE = "\"COLS_PEPTIDE\"";
    private static final String HEADER_COLS_PSMSET = "\"COLS_PSMSET\"";
    private static final String HEADER_COLS_PSM = "\"COLS_PSM\"";
    private static final String HEADER_PROTEINS = "\"Proteins\"";
    private static final String HEADER_SCORE = "\"Score\"";
    private static final String HEADER_COVERAGES = "\"Coverages\"";
    private static final String HEADER_NR_PEPTIDES = "\"nrPeptides\"";
    private static final String HEADER_NR_PSMS = "\"nrPSM\"";
    private static final String HEADER_NR_PSM_SETS = "\"nrPSMSets\"";
    private static final String HEADER_NR_SPECTRA = "\"nrSpectra\"";
    private static final String HEADER_NR_IDENTIFICATIONS = "\"nrIdentifications\"";
    private static final String HEADER_CLUSTER_ID = "\"ClusterID\"";
    private static final String HEADER_DESCRIPTION = "\"Description\"";
    private static final String HEADER_DECOY = "\"Decoy\"";
    private static final String HEADER_FDR_Q_VALUE = "\"FDR q-valu\"";
    private static final String HEADER_SEQUENCE = "\"Sequence\"";
    private static final String HEADER_ACCESSIONS = "\"Accessions\"";
    private static final String HEADER_MODIFICATIONS = "\"Modifications\"";
    private static final String HEADER_MISSED_CLEAVAGES = "\"Missed Cleavages\"";
    private static final String HEADER_CHARGE = "\"Charge\"";
    private static final String HEADER_MASS_TO_CHARGE = "\"m/z\"";
    private static final String HEADER_DELTA_MASS = "\"deltaMass\"";
    private static final String HEADER_DELTA_PPM = "\"deltaPPM\"";
    private static final String HEADER_RETENTION_TIME = "\"Retention time\"";
    private static final String HEADER_SOURCE_ID = "\"Source ID\"";
    private static final String HEADER_SPECTRUM_TITLE = "\"Spectrum title\"";
    private static final String HEADER_SCORES = "\"scores\"";
    private static final String HEADER_BEST_SCORES = "\"best scores\"";
    private static final String HEADER_SCORE_NAMES = "\"score names\"";
    private static final String HEADER_SCORE_SHORTS = "\"score shorts\"";


    /**
     * Basic constructor
     *
     * @param modeller
     */
    public CSVExporter(PIAModeller modeller) {
        this.piaModeller = modeller;
    }


    public boolean exportToCSV(Long fileID, File exportFile,
            boolean psmLevel, boolean peptideLevel, boolean proteinLevel,
            boolean filterExport) {
        boolean exportOK;

        try (FileOutputStream fos = new FileOutputStream(exportFile)) {
            exportOK = exportToCSV(fileID, fos, psmLevel, peptideLevel, proteinLevel, filterExport);
        } catch (IOException ex) {
            LOGGER.error("Error writing CSV to " + exportFile.getAbsolutePath(), ex);
            exportOK =  false;
        }

        return exportOK;
    }


    public boolean exportToCSV(Long fileID, String exportFileName,
            boolean psmLevel, boolean peptideLevel, boolean proteinLevel,
            boolean filterExport) {
        File piaFile = new File(exportFileName);
        return exportToCSV(fileID, piaFile, psmLevel, peptideLevel, proteinLevel, filterExport);
    }


    public boolean exportToCSV(Long fileID, OutputStream exportStream,
            boolean psmLevel, boolean peptideLevel, boolean proteinLevel,
            boolean filterExport) {
        boolean exportOK;

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(exportStream))) {
            exportOK = exportToCSV(fileID, writer, psmLevel, peptideLevel, proteinLevel, filterExport);
        } catch (IOException e) {
            LOGGER.error("Error while exporting to CSV", e);
            exportOK = false;
        }

        return exportOK;
    }


    /**
     * Exports the data of the modeller using the data of the fileID (only
     * relevant if not protein level) to the specified file. If protein level is
     * selected as well, also this will be exported (and accordingly the PSMs of
     * all merged files).
     *
     * @param filterExport whether the export should be filtered (on any level)
     * @return
     */
    public boolean exportToCSV(Long fileID, Writer exportWriter,
            boolean psmLevel, boolean peptideLevel, boolean proteinLevel,
            boolean filterExport) {
        boolean error = false;

        // if protein level is selected, the exportFileID is 0 (overview), otherwise teh given value
        exportFileID = proteinLevel ? 0L : fileID;

        this.filterExport = filterExport;

        this.psmLevel = psmLevel;
        this.peptideLevel= peptideLevel;
        this.proteinLevel = proteinLevel;

        this.considerModifications = piaModeller.getConsiderModifications();

        // check for calculated protein FDRs
        this.proteinFDR = piaModeller.getProteinModeller().getFDRData().getNrItems() != null;

        // if PSM sets are created and the overview or protein level is selected
        includePSMSets = piaModeller.getCreatePSMSets()
                && (fileID.equals(0L) || proteinLevel);

        scoreShortsToNames = piaModeller.getPSMModeller().getScoreShortsToScoreNames();

        LOGGER.info("start writing CSV file");
        try {
            outWriter = new BufferedWriter(exportWriter);

            writeHeader(outWriter);

            if (proteinLevel) {
                writeStartAtProteinLevel();
            } else if (peptideLevel) {
                writeStartAtPeptideLevel();
            } else if (includePSMSets) {
                writeStartAtPSMSetLevel();
            } else if (psmLevel) {
                writeStartAtPSMLevel();
            }

        } catch (Exception e) {
            LOGGER.error("Error writing the CSV file", e);
            error = true;
        } finally {
            try {
                outWriter.close();
                LOGGER.info("CSV export done.");
            } catch (Exception e) {
                LOGGER.error("Error writing the CSV file", e);
                error = true;
            }
        }

        return !error;
    }


    /**
     * Writes the CSV header
     *
     * @param writer
     * @throws IOException
     */
    private void writeHeader(Writer writer) throws IOException {

        StringBuilder headerSB = new StringBuilder();

        if (proteinLevel) {
            headerSB.append(HEADER_COLS_PROTEIN).append(SEPARATOR)
                    .append(HEADER_PROTEINS).append(SEPARATOR)
                    .append(HEADER_SCORE).append(SEPARATOR)
                    .append(HEADER_COVERAGES).append(SEPARATOR)
                    .append(HEADER_NR_PEPTIDES).append(SEPARATOR)
                    .append(HEADER_NR_PSMS).append(SEPARATOR)
                    .append(HEADER_NR_SPECTRA).append(SEPARATOR)
                    .append(HEADER_CLUSTER_ID).append(SEPARATOR)
                    .append(HEADER_DESCRIPTION);

            if (proteinFDR) {
                headerSB.append(SEPARATOR)
                        .append(HEADER_DECOY).append(SEPARATOR)
                        .append(HEADER_FDR_Q_VALUE);
            }

            headerSB.append(NEWLINE);
        }

        if (peptideLevel) {
            headerSB.append(HEADER_COLS_PEPTIDE).append(SEPARATOR)
                    .append(HEADER_SEQUENCE).append(SEPARATOR)
                    .append(HEADER_ACCESSIONS).append(SEPARATOR);

            if (considerModifications) {
                headerSB.append(HEADER_MODIFICATIONS).append(SEPARATOR);
            }

            headerSB.append(HEADER_NR_SPECTRA).append(SEPARATOR)
                    .append(HEADER_NR_PSM_SETS).append(SEPARATOR)
                    .append(HEADER_MISSED_CLEAVAGES).append(SEPARATOR)
                    .append(HEADER_BEST_SCORES).append(SEPARATOR)
                    .append(HEADER_SCORE_NAMES).append(SEPARATOR)
                    .append(HEADER_SCORE_SHORTS);

            headerSB.append(NEWLINE);
        }

        if (includePSMSets) {
            headerSB.append(HEADER_COLS_PSMSET).append(SEPARATOR)
                    .append(HEADER_SEQUENCE).append(SEPARATOR)
                    .append(HEADER_ACCESSIONS).append(SEPARATOR)
                    .append(HEADER_MODIFICATIONS).append(SEPARATOR)
                    .append(HEADER_DECOY).append(SEPARATOR)
                    .append(HEADER_CHARGE).append(SEPARATOR)
                    .append(HEADER_MASS_TO_CHARGE).append(SEPARATOR)
                    .append(HEADER_DELTA_MASS).append(SEPARATOR)
                    .append(HEADER_DELTA_PPM).append(SEPARATOR)
                    .append(HEADER_RETENTION_TIME).append(SEPARATOR)
                    .append(HEADER_MISSED_CLEAVAGES).append(SEPARATOR)
                    .append(HEADER_NR_IDENTIFICATIONS).append(SEPARATOR)
                    .append(HEADER_SCORES).append(SEPARATOR)
                    .append(HEADER_SCORE_NAMES).append(SEPARATOR)
                    .append(HEADER_SCORE_SHORTS);

            headerSB.append(NEWLINE);
        }

        if (psmLevel) {
            headerSB.append(HEADER_COLS_PSM).append(SEPARATOR)
                    .append(HEADER_SEQUENCE).append(SEPARATOR)
                    .append(HEADER_SEQUENCE).append(SEPARATOR)
                    .append(HEADER_ACCESSIONS).append(SEPARATOR)
                    .append(HEADER_MODIFICATIONS).append(SEPARATOR)
                    .append(HEADER_DECOY).append(SEPARATOR)
                    .append(HEADER_CHARGE).append(SEPARATOR)
                    .append(HEADER_MASS_TO_CHARGE).append(SEPARATOR)
                    .append(HEADER_DELTA_MASS).append(SEPARATOR)
                    .append(HEADER_DELTA_PPM).append(SEPARATOR)
                    .append(HEADER_RETENTION_TIME).append(SEPARATOR)
                    .append(HEADER_MISSED_CLEAVAGES).append(SEPARATOR)
                    .append(HEADER_SOURCE_ID).append(SEPARATOR)
                    .append(HEADER_SPECTRUM_TITLE).append(SEPARATOR)
                    .append(HEADER_SCORES).append(SEPARATOR)
                    .append(HEADER_SCORE_NAMES).append(SEPARATOR)
                    .append(HEADER_SCORE_SHORTS);

            headerSB.append(NEWLINE);
        }

        writer.append(headerSB);
    }


    /**
     * Writes out the data starting at the protein level
     * @throws IOException
     */
    private void writeStartAtProteinLevel() throws IOException {
        List<AbstractFilter> filters = filterExport ? piaModeller.getProteinModeller().getReportFilters() : null;
        List<ReportProtein> reportList = piaModeller.getProteinModeller().getFilteredReportProteins(filters);

        if (reportList == null) {
            LOGGER.warn("The report is empty, probably no inference run?");
            return;
        }

        for (ReportProtein protein : reportList) {
            outWriter.append("PROTEIN").append(SEPARATOR);

            appendQuoted(outWriter, createAccessionsString(protein.getAccessions()));
            outWriter.append(SEPARATOR);
            appendQuoted(outWriter, protein.getScore().toString());
            outWriter.append(SEPARATOR);
            appendQuoted(outWriter, createCoveragesString(protein));
            outWriter.append(SEPARATOR);
            appendQuoted(outWriter, protein.getNrPeptides().toString());
            outWriter.append(SEPARATOR);
            appendQuoted(outWriter, protein.getNrPSMs().toString());
            outWriter.append(SEPARATOR);
            appendQuoted(outWriter, protein.getNrSpectra().toString());
            outWriter.append(SEPARATOR);
            appendQuoted(outWriter, String.valueOf(protein.getAccessions().get(0).getGroup().getTreeID()));
            outWriter.append(SEPARATOR);
            appendQuoted(outWriter, createDescriptionsString(protein.getAccessions()));

            if (proteinFDR) {
                outWriter.append(SEPARATOR);
                appendQuoted(outWriter, String.valueOf(protein.getIsDecoy()));
                outWriter.append(SEPARATOR);
                appendQuoted(outWriter, String.valueOf(protein.getFDR()));
            }

            outWriter.append(NEWLINE);

            if (peptideLevel || includePSMSets || psmLevel) {
                for (ReportPeptide peptide : protein.getPeptides()) {
                    writePeptide(peptide);
                }
            }
        }
    }


    /**
     * Writes out the data starting at the peptide level
     *
     * @throws IOException
     */
    private void writeStartAtPeptideLevel() throws IOException {
        List<AbstractFilter> filters = filterExport ? piaModeller.getPeptideModeller().getFilters(exportFileID) : null;
        List<ReportPeptide> reportList = piaModeller.getPeptideModeller().getFilteredReportPeptides(exportFileID, filters);

        if (reportList == null) {
            LOGGER.warn("The report is empty, probably no peptides inferred?");
            return;
        }

        for (ReportPeptide peptide : reportList) {
            writePeptide(peptide);
        }
    }


    /**
     * Writes out the data starting at the PSM sets level
     *
     * @throws IOException
     */
    private void writeStartAtPSMSetLevel() throws IOException {
        List<AbstractFilter> filters = filterExport ? piaModeller.getPSMModeller().getFilters(0L) : null;
        List<ReportPSMSet> reportList = piaModeller.getPSMModeller().getFilteredReportPSMSets(filters);

        if (reportList == null) {
            LOGGER.warn("The report is empty, too many filters or no PSM sets created?");
            return;
        }

        for (ReportPSMSet psmSet : reportList) {
            writePSMSet(psmSet);
        }
    }


    /**
     * Writes out the data starting at the PSM level
     * @throws IOException
     */
    private void writeStartAtPSMLevel() throws IOException {
        List<AbstractFilter> filters = filterExport ? piaModeller.getPSMModeller().getFilters(exportFileID) : null;
        List<ReportPSM> reportList = piaModeller.getPSMModeller().getFilteredReportPSMs(exportFileID, filters);

        if (reportList == null) {
            LOGGER.warn("The report is empty, too many filters?");
            return;
        }

        for (ReportPSM psmSet : reportList) {
            writePSM(psmSet);
        }
    }


    /**
     * Writes the data of a single peptide (and the levels below)
     *
     * @throws IOException
     */
    private void writePeptide(ReportPeptide peptide) throws IOException {
        if (peptideLevel) {
            //the peptide level should be written
            outWriter.append("PEPTIDE").append(SEPARATOR);

            appendQuoted(outWriter, peptide.getSequence());
            outWriter.append(SEPARATOR);
            appendQuoted(outWriter, createAccessionsString(peptide.getAccessions()));
            outWriter.append(SEPARATOR);

            if (considerModifications) {
                appendQuoted(outWriter, peptide.getPSMs().get(0).getModificationsString());
                outWriter.append(SEPARATOR);
            }

            appendQuoted(outWriter, peptide.getNrSpectra().toString());
            outWriter.append(SEPARATOR);
            appendQuoted(outWriter, peptide.getNrPSMs().toString());
            outWriter.append(SEPARATOR);
            appendQuoted(outWriter, String.valueOf(peptide.getMissedCleavages()));
            outWriter.append(SEPARATOR);
            writeScores(peptide, piaModeller.getPeptideModeller().getScoreShortNames(exportFileID));

            outWriter.append(NEWLINE);
        }

        writePeptidesPSMorSet(peptide);
    }


    /**
     * Writes the PSM sets or just the PSMs of the given peptide
     *
     * @param peptide
     * @throws IOException
     */
    private void writePeptidesPSMorSet(ReportPeptide peptide) throws IOException {

        if (includePSMSets) {
            for (PSMReportItem psmSet : peptide.getPSMs()) {
                if (psmSet instanceof ReportPSMSet) {
                    writePSMSet((ReportPSMSet)psmSet);
                }
            }
        } else if (psmLevel) {
            List<ReportPSM> psmList = new ArrayList<>();
            for (PSMReportItem psm : peptide.getPSMs()) {
                if (psm instanceof ReportPSMSet) {
                    psmList.addAll(((ReportPSMSet) psm).getPSMs());
                } else if (psm instanceof ReportPSM) {
                    psmList.add((ReportPSM) psm);
                }
            }

            for (ReportPSM psm : psmList) {
                writePSM(psm);
            }
        }
    }


    /**
     * Writes the data of a single PSM set (and the levels below)
     *
     * @throws IOException
     */
    private void writePSMSet(ReportPSMSet psmSet) throws IOException {
        if (includePSMSets) {
            //psmSets
            writePSMorPSMSet(psmSet);
        }

        if (psmLevel) {
            for (ReportPSM psm : psmSet.getPSMs()) {
                writePSM(psm);
            }
        }
    }

    /**
     * Writes the data of a single PSM set (and the levels below)
     *
     * @throws IOException
     */
    private void writePSM(ReportPSM psm) throws IOException {
        if (psmLevel) {
            writePSMorPSMSet(psm);
        }
    }


    /**
     * Writes the data of a single PSM or PSMset
     *
     * @throws IOException
     */
    private void writePSMorPSMSet(PSMReportItem psm) throws IOException {
        boolean isSet;
        if (psm instanceof ReportPSMSet) {
            isSet = true;
            outWriter.append("PSMSET").append(SEPARATOR);
        } else {
            isSet = false;
            outWriter.append("PSM").append(SEPARATOR);
        }

        appendQuoted(outWriter, psm.getSequence());
        outWriter.append(SEPARATOR);
        appendQuoted(outWriter, createAccessionsString(psm.getAccessions()));
        outWriter.append(SEPARATOR);
        appendQuoted(outWriter, createAccessionsString(psm.getAccessions()));
        outWriter.append(SEPARATOR);
        appendQuoted(outWriter, psm.getModificationsString());
        outWriter.append(SEPARATOR);
        appendQuoted(outWriter, String.valueOf(psm.getIsDecoy()));
        outWriter.append(SEPARATOR);
        appendQuoted(outWriter, String.valueOf(psm.getCharge()));
        outWriter.append(SEPARATOR);
        appendQuoted(outWriter, String.valueOf(psm.getMassToCharge()));
        outWriter.append(SEPARATOR);
        appendQuoted(outWriter, String.valueOf(psm.getDeltaMass()));
        outWriter.append(SEPARATOR);
        appendQuoted(outWriter, String.valueOf(psm.getDeltaPPM()));
        outWriter.append(SEPARATOR);
        appendQuoted(outWriter, String.valueOf(psm.getRetentionTime()));
        outWriter.append(SEPARATOR);
        appendQuoted(outWriter, String.valueOf(psm.getMissedCleavages()));
        outWriter.append(SEPARATOR);

        if (isSet) {
            appendQuoted(outWriter, String.valueOf(((ReportPSMSet) psm).getPSMs().size()));
            outWriter.append(SEPARATOR);
        } else {
            appendQuoted(outWriter, psm.getSourceID());
            outWriter.append(SEPARATOR);
            appendQuoted(outWriter, psm.getSpectrumTitle());
            outWriter.append(SEPARATOR);
        }

        writeScores(psm, piaModeller.getPSMModeller().getScoreShortNames(exportFileID));

        outWriter.append(NEWLINE);
    }


    /**
     * Create a string representation of the given accessions
     * @return
     */
    private static String createAccessionsString(Collection<Accession> accessions) {
        StringBuilder accSB = new StringBuilder();
        for (Accession accession : accessions) {
            if (accSB.length() > 0) {
                accSB.append(MULTIVALUE_SEPARATOR);
            }
            accSB.append(accession.getAccession());
        }

        return accSB.toString();
    }


    /**
     * Create a string representation of the protein's accessions' coverages
     *
     * @return
     */
    private static String createCoveragesString(ReportProtein protein) {
        StringBuilder coverageSB = new StringBuilder();

        for (Accession accession : protein.getAccessions()) {
            if (coverageSB.length() > 0) {
                coverageSB.append(MULTIVALUE_SEPARATOR);
            }

            Double coverage = protein.getCoverage(accession.getAccession());
            if (coverage.equals(Double.NaN)) {
                coverageSB.append("NA");
            } else {
                coverageSB.append(coverage);
            }
        }

        return coverageSB.toString();
    }


    /**
     * Create a string representation of the protein's accessions' descriptions
     *
     * @return
     */
    private String createDescriptionsString(Collection<Accession> accessions) {
        StringBuilder accSB = new StringBuilder();
        for (Accession accession : accessions) {
            if (accSB.length() > 0) {
                accSB.append(MULTIVALUE_SEPARATOR);
            }
            accSB.append(accession.getDescription(exportFileID));
        }

        return accSB.toString();
    }

    /**
     * Appends the given String-value to the writer
     *
     * @param writer
     * @param value
     * @throws IOException
     */
    private static void appendQuoted(Writer writer, String value) throws IOException {
        writer.append('"').append(value).append('"');
    }

    private void writeScores(Rankable item, List<String> scoreShorts) throws IOException {
        StringBuilder scoresSB = new StringBuilder();
        StringBuilder scoreNamesSB = new StringBuilder();
        StringBuilder scoreShortsSB = new StringBuilder();

        for (String scoreShort : scoreShorts) {
            if (scoresSB.length() > 0) {
                scoresSB.append(MULTIVALUE_SEPARATOR);
                scoreNamesSB.append(MULTIVALUE_SEPARATOR);
                scoreShortsSB.append(MULTIVALUE_SEPARATOR);
            }

            scoresSB.append(item.getScore(scoreShort));
            scoreNamesSB.append(scoreShortsToNames.get(scoreShort));
            scoreShortsSB.append(scoreShort);
        }

        appendQuoted(outWriter, scoresSB.toString());
        outWriter.append(SEPARATOR);
        appendQuoted(outWriter, scoreNamesSB.toString());
        outWriter.append(SEPARATOR);
        appendQuoted(outWriter, scoreShortsSB.toString());
    }

}