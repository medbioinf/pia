package de.mpc.pia.intermediate.compiler.parser;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.FileFormat;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabase;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabaseRef;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationProtocol;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.AccessionOccurrence;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.tools.MzIdentMLTools;

public class FastaFileParser {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(FastaFileParser.class);


    /**
     * We don't ever want to instantiate this class
     */
    private FastaFileParser() {
        throw new AssertionError();
    }


    /**
     * Parses the data from a FASTA file, the proteins are digested by the
     * enzymePattern allowing for up to missedCleavages misses.
     *
     * @return
     */
    public static boolean getDataFromFastaFile(String name, String fileName,
            PIACompiler compiler, String enzymePattern, int minPepLength,
            int maxPepLength, int missedCleavages) {
        if (missedCleavages < 0) {
            LOGGER.warn("You allowed for all possible missed cleavages, this "
                    + "may result in a massive file and take very long!");
        }

        try (FileInputStream fileStream = new FileInputStream(fileName)) {
            DataInputStream in = new DataInputStream(fileStream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            PIAInputFile inputFile =
                    compiler.insertNewFile(name, fileName, "FASTA");

            // add the searchDB (actually, this FASTA file)
            SearchDatabase searchDatabase = new SearchDatabase();
            searchDatabase.setId("fastaFile");
            searchDatabase.setLocation(fileName);

            FileFormat fileFormat = new FileFormat();
            CvParam abstractParam = new CvParam();
            abstractParam.setAccession("MS:1001348");
            abstractParam.setCv(MzIdentMLTools.getCvPSIMS());
            abstractParam.setName("FASTA format");
            fileFormat.setCvParam(abstractParam);
            searchDatabase.setFileFormat(fileFormat);

            searchDatabase = compiler.putIntoSearchDatabasesMap(searchDatabase);

            // add the spectrumIdentificationProtocol
            SpectrumIdentificationProtocol spectrumIDProtocol =
                    new SpectrumIdentificationProtocol();
            spectrumIDProtocol.setId("fastaParsing");
            // TODO: set this: spectrumIDProtocol.setAnalysisSoftware(PIA);
            inputFile.addSpectrumIdentificationProtocol(spectrumIDProtocol);

            // add the spectrum identification
            SpectrumIdentification spectrumID = new SpectrumIdentification();
            spectrumID.setId("fastaParsing");
            spectrumID.setSpectrumIdentificationList(null);
            spectrumID.setSpectrumIdentificationProtocol(spectrumIDProtocol);

            SearchDatabaseRef searchDBRef = new SearchDatabaseRef();
            searchDBRef.setSearchDatabase(searchDatabase);
            spectrumID.getSearchDatabaseRef().add(searchDBRef);

            inputFile.addSpectrumIdentification(spectrumID);


            String strLine;
            int spectrumOffset = 0;
            int accessions = 0;
            FastaHeaderInfos headerInfos = null;
            StringBuilder dbSequenceBuffer = new StringBuilder();

            while ((strLine = br.readLine()) != null) {
                if (strLine.startsWith(">")) {
                    if ((headerInfos != null) && (dbSequenceBuffer.length() > 0)) {
                        // a prior protein can be inserted and digested
                        spectrumOffset += digestProtein(headerInfos,
                                dbSequenceBuffer.toString(),
                                compiler,
                                inputFile,
                                spectrumID,
                                searchDatabase.getId(),
                                enzymePattern,
                                minPepLength,
                                maxPepLength,
                                missedCleavages,
                                spectrumOffset);
                        accessions++;

                        if (accessions % 100000 == 0) {
                            LOGGER.info(accessions + " accessions processed");
                        }
                    }

                    // start of a new protein
                    headerInfos = FastaHeaderInfos.parseHeaderInfos(strLine);
                    dbSequenceBuffer = new StringBuilder();
                } else {
                    // just reading in the protein sequence
                    dbSequenceBuffer.append(strLine.trim());
                }
            }

            if ((headerInfos != null) && (dbSequenceBuffer.length() > 0)) {
                // the last protein can be inserted and digested
                spectrumOffset += digestProtein(headerInfos,
                        dbSequenceBuffer.toString(),
                        compiler,
                        inputFile,
                        spectrumID,
                        searchDatabase.getId(),
                        enzymePattern,
                        minPepLength,
                        maxPepLength,
                        missedCleavages,
                        spectrumOffset);
            }

            in.close();
        } catch (Exception e) {
            LOGGER.error("Error while parsing the FASTA file", e);
        }

        return true;
    }


    /**
     * Inserts the peptides of the given protein into the compiler.
     *
     * @param fastaHeader
     * @param dbSequence
     * @param compiler
     */
    private static int digestProtein(FastaHeaderInfos fastaHeader,
            String dbSequence, PIACompiler compiler, PIAInputFile inputFile,
            SpectrumIdentification spectrumID, String searchDBRef,
            String enzymePattern, int minPepLength, int maxPepLength,
            int maxMissedCleavages, int spectrumCountOffset) {
        Accession accession;

        // first, look if the accession is already in the compilation (this should not be the case!)
        accession = compiler.getAccession(fastaHeader.getAccession());
        if (accession != null) {
            LOGGER.warn("Protein with accession " + accession.getAccession() +
                    " already in the compilation! Only keeping the sequence " +
                    "of the first accession.");
            return 0;
        }

        // put the new accession into the compiler
        accession = compiler.insertNewAccession(fastaHeader.getAccession(),
                dbSequence);

        accession.addFile(inputFile.getID());

        accession.addDescription(inputFile.getID(),
                fastaHeader.getDescription());

        accession.addSearchDatabaseRef(searchDBRef);

        // digest the protein
        String[] peptides = dbSequence.split(enzymePattern);

        // if the missedCleavages is below 0, allow for all possible missed cleavages
        int missedCleavages;
        if (maxMissedCleavages >= 0) {
            missedCleavages = maxMissedCleavages;
        } else {
            missedCleavages = peptides.length - 1;
        }

        // get the peptides
        int spectraCount = 1;
        for (int missed=0; missed <= missedCleavages; missed++) {

            int start = 0;
            for (int i = 0; i < peptides.length-missed; i++) {
                // build the sequence with misses
                StringBuilder sequence = new StringBuilder(peptides[i]);
                for (int miss=1; miss <= missed; miss++) {
                    sequence.append(peptides[i+miss]);
                }

                if ((sequence.length() >= minPepLength) &&
                        (sequence.length() <= maxPepLength)) {
                    addSequence(sequence.toString(),
                            accession,
                            start+1,
                            missed,
                            compiler,
                            inputFile,
                            spectrumID,
                            spectrumCountOffset + spectraCount);
                }

                start += peptides[i].length();
                spectraCount++;
            }

        }

        return spectraCount;
    }


    /**
     * Adds the sequence to the compiler.
     */
    private static void addSequence(String sequence, Accession accession,
            int start, int missed, PIACompiler compiler, PIAInputFile inputFile,
            SpectrumIdentification spectrumID, int spectrumCount) {
        Peptide peptide = compiler.getPeptide(sequence);

        if (peptide == null) {
            peptide = compiler.insertNewPeptide(sequence);

            // only add one PSM for one peptide-sequence
            // TODO: calculate the mass
            double massToCharge = sequence.length();

            String sourceID = "index=" + spectrumCount;

            PeptideSpectrumMatch psm = compiler.createNewPeptideSpectrumMatch(
                    2,                          // just a pseudo-charge
                    massToCharge,
                    0,
                    null,
                    sequence,
                    missed,
                    sourceID,
                    sequence,
                    inputFile,
                    spectrumID);

            peptide.addSpectrum(psm);

            // add the "FASTA Sequence Count" score
            ScoreModel score = new ScoreModel(1.0,
                    ScoreModelEnum.FASTA_SEQUENCE_COUNT);
            psm.addScore(score);

            // add the "FASTA Accession Count" score
            score = new ScoreModel(0.0,
                    ScoreModelEnum.FASTA_ACCESSION_COUNT);
            psm.addScore(score);

            compiler.insertCompletePeptideSpectrumMatch(psm);
        } else {
            // increase the "FASTA Sequence Count" score
            Optional<PeptideSpectrumMatch> psm = peptide.getSpectra().stream().findFirst();
            if (psm.isPresent()) {
                ScoreModel score = psm.get().getScore(ScoreModelEnum.FASTA_SEQUENCE_COUNT.getShortName());
                Double value = score.getValue();
                score.setValue(value + 1);
            }
        }

        boolean increaseAccessionCount = true;
        for (AccessionOccurrence occ : peptide.getAccessionOccurrences()) {
            // only count the accessions once for the "FASTA Accession Count"
            if (accession.getID().equals(occ.getAccession().getID())) {
                increaseAccessionCount = false;
                break;
            }
        }
        if (increaseAccessionCount) {
            // increase the "FASTA Accession Count" score
            Optional<PeptideSpectrumMatch> psm = peptide.getSpectra().stream().findFirst();
            if (psm.isPresent()) {
                ScoreModel score = psm.get().getScore(ScoreModelEnum.FASTA_ACCESSION_COUNT.getShortName());
                Double value = score.getValue();
                score.setValue(value + 1);
            }
        }

        peptide.addAccessionOccurrence(accession,
                start,
                start+sequence.length()-1);

        // now insert the connection between peptide and accession into the compiler
        compiler.addAccessionPeptideConnection(accession, peptide);
    }


    /**
     * Checks, whether the given file looks like a FASTA file
     *
     * @param fileName
     * @return
     */
    public static boolean checkFileType(String fileName) {
        boolean isFastaFile = false;
        LOGGER.debug("checking whether this is a FASTA file: " + fileName);

        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            // read in the first 100, not empty lines
            List<String> lines = stream.filter(line -> !line.trim().isEmpty())
                    .limit(100)
                    .collect(Collectors.toList());

            // check, if first line is a header

            int headerCount = 0;
            int sequenceCount = 0;
            int otherCount = 0;

            // file must start with a header
            isFastaFile = lines.get(0).startsWith(">");

            for (String line : lines) {
                if (line.startsWith(">")) {
                    headerCount++;
                } else if (line.replaceAll("[a-zA-Z]*", "").trim().isEmpty()) {
                    sequenceCount++;
                } else {
                    otherCount++;
                }
            }

            if ((otherCount > 0) || (sequenceCount < 1)) {
                // not a single sequence line and some "other" lines -> it's no FASTA file
                isFastaFile = false;
            }

            LOGGER.debug("headers: " + headerCount
                    + ", sequences: " + sequenceCount
                    + ", other: " + otherCount);
        } catch (Exception e) {
            LOGGER.debug("Could not check file " + fileName, e);
        }

        return isFastaFile;
    }
}
