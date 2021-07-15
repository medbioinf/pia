package de.mpc.pia.intermediate.compiler.parser.searchengines;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import com.compomics.mascotdatfile.util.interfaces.MascotDatfileInf;
import com.compomics.mascotdatfile.util.interfaces.QueryToPeptideMapInf;
import com.compomics.mascotdatfile.util.mascot.FixedModification;
import com.compomics.mascotdatfile.util.mascot.PeptideHit;
import com.compomics.mascotdatfile.util.mascot.ProteinHit;
import com.compomics.mascotdatfile.util.mascot.ProteinMap;
import com.compomics.mascotdatfile.util.mascot.Query;
import com.compomics.mascotdatfile.util.mascot.VariableModification;
import com.compomics.mascotdatfile.util.mascot.enumeration.MascotDatfileType;
import com.compomics.mascotdatfile.util.mascot.factory.MascotDatfileFactory;
import com.compomics.mascotdatfile.util.mascot.iterator.QueryEnumerator;

import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.Enzyme;
import uk.ac.ebi.jmzidml.model.mzidml.Enzymes;
import uk.ac.ebi.jmzidml.model.mzidml.FileFormat;
import uk.ac.ebi.jmzidml.model.mzidml.InputSpectra;
import uk.ac.ebi.jmzidml.model.mzidml.ModificationParams;
import uk.ac.ebi.jmzidml.model.mzidml.Param;
import uk.ac.ebi.jmzidml.model.mzidml.ParamList;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabase;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabaseRef;
import uk.ac.ebi.jmzidml.model.mzidml.SearchModification;
import uk.ac.ebi.jmzidml.model.mzidml.SpecificityRules;
import uk.ac.ebi.jmzidml.model.mzidml.SpectraData;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIDFormat;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationProtocol;
import uk.ac.ebi.jmzidml.model.mzidml.Tolerance;
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
import de.mpc.pia.tools.unimod.UnimodParser;
import de.mpc.pia.tools.unimod.jaxb.ModT;


/**
 * This class parses the data from a Mascot DAT file for a given
 * {@link PIACompiler}.<br/>
 *
 * @author julian
 *
 */
public class MascotDatFileParser {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(MascotDatFileParser.class);

    /**
     * We don't ever want to instantiate this class
     */
    private MascotDatFileParser() {
        throw new AssertionError();
    }


    /**
     * Parses the data from an mzIdentML file given by its name into the given
     * {@link PIACompiler}.
     *
     * @param fileName name of the mzTab file
     */
    public static boolean getDataFromMascotDatFile(String name, String fileName,
            PIACompiler compiler) {

        // need to parse through the file, as mascotdatfile (3.2.11) does not support
        //   - the "index" variable of the queries
        //   - the "fastafile"
        //   - no good information for enzyme
        Map<String, String> queryIndexMap = new HashMap<>();
        String fastaFile = null;

        String enzymeCleavage = null;
        String enzymeRestrict = null;

        try (BufferedReader rd = new BufferedReader(new FileReader(fileName))) {
            String line;

            boolean inQuery = false;
            boolean inEnzyme = false;
            String queryName = null;

            while ((line = rd.readLine()) != null) {
                if (!inQuery) {
                    if (line.startsWith("Content-Type: application/x-Mascot; NAME=\"query")) {
                        queryName = line.substring(42, line.length()-1);
                        inQuery = true;
                    } else if ((fastaFile == null) &&
                            line.startsWith("fastafile")) {
                        fastaFile = line.substring(10);
                    }
                } else if (inQuery &&
                        line.startsWith("index=")) {
                    queryIndexMap.put(queryName, line);
                    inQuery = false;
                }

                if (!inEnzyme) {
                    if (((enzymeCleavage == null) || (enzymeRestrict == null)) &&
                            line.startsWith("Content-Type: application/x-Mascot; NAME=\"enzyme\"")) {
                        inEnzyme = true;
                    }
                } else {
                    if (line.startsWith("Cleavage:")) {
                        enzymeCleavage = line.substring(9).trim();
                    } else if (line.startsWith("Restrict:")) {
                        enzymeRestrict = line.substring(9).trim();
                    } else if (line.startsWith("Content-Type:")) {
                        inEnzyme = false;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("could not read '" + fileName + "' for index parsing.", e);
            return false;
        }


        MascotDatfileInf mascotFile =
                MascotDatfileFactory.create(fileName, MascotDatfileType.MEMORY);

        if (mascotFile == null) {
            LOGGER.error("could not read '" + fileName + "'.");
            return false;
        }

        PIAInputFile file = compiler.insertNewFile(name, fileName,
                InputFileParserFactory.InputFileTypes.MASCOT_DAT_INPUT.getFileSuffix());

        // create the analysis software and add it to the compiler
        AnalysisSoftware mascot = new AnalysisSoftware();

        mascot.setId("mascot");
        mascot.setName("mascot");
        mascot.setUri("http://www.matrixscience.com/");
        mascot.setVersion(mascotFile.getHeaderSection().getVersion());

        Param param = new Param();
        param.setParam(MzIdentMLTools.createPSICvParam(OntologyConstants.MASCOT, null));
        mascot.setSoftwareName(param);

        mascot = compiler.putIntoSoftwareMap(mascot);


        // create the searchDatabase and add it to the compiler
        SearchDatabase searchDatabase = new SearchDatabase();

        // required
        searchDatabase.setId("mascotDB");
        searchDatabase.setLocation(fastaFile);
        // optional
        searchDatabase.setName(mascotFile.getParametersSection().getDatabase());
        searchDatabase.setNumDatabaseSequences(mascotFile.getHeaderSection().getSequences());
        searchDatabase.setNumResidues(mascotFile.getHeaderSection().getResidues());

        // fileformat
        FileFormat fileFormat = new FileFormat();
        fileFormat.setCvParam(MzIdentMLTools.createPSICvParam(OntologyConstants.FASTA_FORMAT, null));
        searchDatabase.setFileFormat(fileFormat);
        // databaseName
        param = new Param();
        param.setParam(MzIdentMLTools.createUserParam(mascotFile.getHeaderSection().getRelease(), null, "string"));
        searchDatabase.setDatabaseName(param);

        // add searchDB to the compiler
        searchDatabase = compiler.putIntoSearchDatabasesMap(searchDatabase);


        // add the spectraData (input file)
        SpectraData spectraData = null;
        if ((mascotFile.getParametersSection().getFile() != null) &&
                (mascotFile.getParametersSection().getFile().trim().length() > 0)) {
            spectraData = new SpectraData();

            spectraData.setId("mascotInput");
            spectraData.setLocation(mascotFile.getParametersSection().getFile());

            if ((mascotFile.getParametersSection().getFormat() != null) &&
                    "Mascot generic".equals(mascotFile.getParametersSection().getFormat())) {
                fileFormat = new FileFormat();

                fileFormat.setCvParam(MzIdentMLTools.createPSICvParam(
                        OntologyConstants.MASCOT_MGF_FORMAT, null));
                spectraData.setFileFormat(fileFormat);

                SpectrumIDFormat idFormat = new SpectrumIDFormat();
                idFormat.setCvParam(MzIdentMLTools.createPSICvParam(
                        OntologyConstants.MULTIPLE_PEAK_LIST_NATIVEID_FORMAT, null));
                spectraData.setSpectrumIDFormat(idFormat);
            }

            spectraData = compiler.putIntoSpectraDataMap(spectraData);
        } else {
            LOGGER.warn("The source file (MGF) was not recorded in the file!");
        }

        // define the spectrumIdentificationProtocol
        SpectrumIdentificationProtocol spectrumIDProtocol =
                new SpectrumIdentificationProtocol();

        spectrumIDProtocol.setId("mascotAnalysis");
        spectrumIDProtocol.setAnalysisSoftware(mascot);

        param = new Param();
        if ("MIS".equals(mascotFile.getParametersSection().getSearch())) {
            param.setParam(MzIdentMLTools.createPSICvParam(OntologyConstants.MS_MS_SEARCH, null));
        }
        // TODO: add error on PMF query (not usable for PIA)
        // TODO: and sequence query
        spectrumIDProtocol.setSearchType(param);

        ParamList paramList = new ParamList();
        paramList.getCvParam().add(MzIdentMLTools.createPSICvParam(
                OntologyConstants.MASCOT_INSTRUMENT,
                mascotFile.getParametersSection().getInstrument()));

        paramList.getUserParam().add(MzIdentMLTools.createUserParam("Mascot User Comment",
                        mascotFile.getParametersSection().getCom(), "string"));

        if ("Monoisotopic".equalsIgnoreCase(mascotFile.getParametersSection().getMass())) {
            paramList.getCvParam().add(
                    MzIdentMLTools.createPSICvParam(OntologyConstants.FRAGMENT_MASS_TYPE_MONO, null));
            paramList.getCvParam().add(
                    MzIdentMLTools.createPSICvParam(OntologyConstants.PARENT_MASS_TYPE_MONO, null));
        } else {
            paramList.getCvParam().add(
                    MzIdentMLTools.createPSICvParam(OntologyConstants.FRAGMENT_MASS_TYPE_AVERAGE, null));
            paramList.getCvParam().add(
                    MzIdentMLTools.createPSICvParam(OntologyConstants.PARENT_MASS_TYPE_AVERAGE, null));
        }

        spectrumIDProtocol.setAdditionalSearchParams(paramList);

        ModificationParams modParams = new ModificationParams();
        for (Object objMod : mascotFile.getModificationList().getVariableModifications()) {
            modParams.getSearchModification().add(createPSIModification((VariableModification)objMod, compiler.getUnimodParser()));
        }
        for (Object objMod : mascotFile.getModificationList().getFixedModifications()) {
            modParams.getSearchModification().add(createPSIModification((FixedModification)objMod, compiler.getUnimodParser()));
        }
        spectrumIDProtocol.setModificationParams(modParams);

        Enzymes enzymes = new Enzymes();
        spectrumIDProtocol.setEnzymes(enzymes);
        if (enzymeCleavage != null) {
            Enzyme enzyme = new Enzyme();

            enzyme.setId("enzyme");
            enzyme.setMissedCleavages(
                    Integer.parseInt(mascotFile.getParametersSection().getPFA()));

            StringBuilder regExp = new StringBuilder();
            if (enzymeRestrict == null) {
                regExp.append("(?=[");
                regExp.append(enzymeCleavage);
                regExp.append("])");
            } else {
                regExp.append("(?<=[");
                regExp.append(enzymeCleavage);
                regExp.append("])(?!");
                regExp.append(enzymeRestrict);
                regExp.append(')');
            }
            enzyme.setSiteRegexp(regExp.toString());

            enzymes.getEnzyme().add(enzyme);
        }

        Tolerance tolerance = new Tolerance();

        CvParam abstractParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.SEARCH_TOLERANCE_PLUS_VALUE,
                mascotFile.getParametersSection().getITOL());
        MzIdentMLTools.setUnitParameterFromString(
                mascotFile.getParametersSection().getITOLU(), abstractParam);
        tolerance.getCvParam().add(abstractParam);

        abstractParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.SEARCH_TOLERANCE_MINUS_VALUE,
                mascotFile.getParametersSection().getITOL());
        MzIdentMLTools.setUnitParameterFromString(
                mascotFile.getParametersSection().getITOLU(), abstractParam);
        tolerance.getCvParam().add(abstractParam);

        spectrumIDProtocol.setFragmentTolerance(tolerance);


        tolerance = new Tolerance();

        abstractParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.SEARCH_TOLERANCE_PLUS_VALUE,
                mascotFile.getParametersSection().getTOL());
        MzIdentMLTools.setUnitParameterFromString(
                mascotFile.getParametersSection().getTOLU(), abstractParam);
        tolerance.getCvParam().add(abstractParam);

        abstractParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.SEARCH_TOLERANCE_MINUS_VALUE,
                mascotFile.getParametersSection().getTOL());
        MzIdentMLTools.setUnitParameterFromString(
                mascotFile.getParametersSection().getTOLU(), abstractParam);
        tolerance.getCvParam().add(abstractParam);

        spectrumIDProtocol.setParentTolerance(tolerance);


        // no threshold set, take all PSMs from the dat file
        paramList = new ParamList();
        paramList.getCvParam().add(MzIdentMLTools.createPSICvParam(OntologyConstants.NO_THRESHOLD, null));
        spectrumIDProtocol.setThreshold(paramList);

        file.addSpectrumIdentificationProtocol(spectrumIDProtocol);


        // add the spectrum identification
        SpectrumIdentification spectrumID = new SpectrumIdentification();
        spectrumID.setId("mascotIdentification");
        spectrumID.setSpectrumIdentificationList(null);
        spectrumID.setSpectrumIdentificationProtocol(spectrumIDProtocol);

        if (spectraData != null) {
            InputSpectra inputSpectra = new InputSpectra();
            inputSpectra.setSpectraData(spectraData);
            spectrumID.getInputSpectra().add(inputSpectra);
        }

        SearchDatabaseRef searchDBRef = new SearchDatabaseRef();
        searchDBRef.setSearchDatabase(searchDatabase);
        spectrumID.getSearchDatabaseRef().add(searchDBRef);

        file.addSpectrumIdentification(spectrumID);


        // get the mappings
        QueryEnumerator queryEnumerator = mascotFile.getQueryEnumerator();
        QueryToPeptideMapInf queryToPeptideMap = mascotFile.getQueryToPeptideMap();
        QueryToPeptideMapInf decoyQueryToPeptideMap = mascotFile.getDecoyQueryToPeptideMap(false);
        ProteinMap proteinMap = mascotFile.getProteinMap();
        ProteinMap decoyProteinMap = mascotFile.getDecoyProteinMap();

        // one query is one spectrum, so go through the queries
        int nrQueries = mascotFile.getNumberOfQueries();
        int nrQueriesDone = 0;
        LOGGER.debug("queries in file: " + nrQueries);
        while (queryEnumerator.hasMoreElements()) {
            Query currQuery = queryEnumerator.nextElement();

            int charge;
            try {
                if (currQuery.getChargeString() == null) {
                    charge = 0;
                } else if (currQuery.getChargeString().contains("-")) {
                    charge = -Integer.parseInt(currQuery.getChargeString().replace("-", ""));
                } else {
                    // we assume, it is positively charged
                    charge = Integer.parseInt(currQuery.getChargeString().replace("+", ""));
                }
            } catch (NumberFormatException e) {
                charge = 0;
                LOGGER.warn("could not parse charge '" + currQuery.getChargeString() + "' for '" + currQuery.getTitle() + '\'');
            }

            double precursorMZ = currQuery.getPrecursorMZ();

            Double retentionTime;
            if (currQuery.getRetentionTimeInSeconds() != null) {
                retentionTime = Double.parseDouble(currQuery.getRetentionTimeInSeconds());
            } else {
                retentionTime = null;
            }

            String spectrumTitle = currQuery.getTitle();
            String index = queryIndexMap.get("query"+currQuery.getQueryNumber());

            // add the target identifications
            if (queryToPeptideMap != null) {
                List<PeptideHit> peptideHits =
                        queryToPeptideMap.getAllPeptideHits(currQuery.getQueryNumber());
                insertPeptideHitsIntoCompiler(compiler, peptideHits, proteinMap,
                        searchDatabase, charge, precursorMZ, retentionTime,
                        index, spectrumTitle, file, spectrumID, false);
            }

            // add the decoy identifications
            if (decoyQueryToPeptideMap != null) {
                List<PeptideHit> peptideHits =
                        decoyQueryToPeptideMap.getAllPeptideHits(currQuery.getQueryNumber());
                insertPeptideHitsIntoCompiler(compiler, peptideHits,
                        decoyProteinMap, searchDatabase, charge, precursorMZ,
                        retentionTime, index, spectrumTitle, file, spectrumID,
                        true);
            }

            nrQueriesDone++;
            if (nrQueriesDone % 10000 == 0) {
                LOGGER.debug("done " + nrQueriesDone + " / " + nrQueries
                        + String.format(" (%1$.4f%%)", 100.0 * nrQueriesDone / nrQueries));
            }
        }

        mascotFile.finish();
        return true;
    }


    private static int insertPeptideHitsIntoCompiler(PIACompiler compiler,
            List<PeptideHit> peptideHits, ProteinMap proteinMap,
            SearchDatabase searchDatabase, int charge, Double precursorMZ,
            Double retentionTime, String sourceId, String spectrumTitle,
            PIAInputFile file, SpectrumIdentification spectrumID,
            boolean isDecoy) {
        String sourceIdStr = sourceId;

        if (peptideHits == null) {
            return 0;
        }

        int nrPepHits = 0;

        Matcher matcher = MzIdentMLTools.patternScanInTitle.matcher(spectrumTitle);
        if (matcher.matches()) {
            sourceIdStr = "index=" + matcher.group(1);
        }

        // the peptideHits are the SpectrumPeptideMatches
        for (PeptideHit peptideHit : peptideHits) {
            PeptideSpectrumMatch psm;
            psm = compiler.createNewPeptideSpectrumMatch(
                    charge,
                    precursorMZ,
                    peptideHit.getDeltaMass(),
                    retentionTime,
                    peptideHit.getSequence(),
                    peptideHit.getMissedCleavages(),
                    sourceIdStr,
                    spectrumTitle,
                    file,
                    spectrumID,
                    null);

            psm.setIsDecoy(isDecoy);

            // get the peptide from the compiler or, if need be, add it
            Peptide peptide;
            peptide = compiler.getPeptide(peptideHit.getSequence());
            if (peptide == null) {
                peptide = compiler.insertNewPeptide(peptideHit.getSequence());
            }

            // add the spectrum to the peptide
            peptide.addSpectrum(psm);

            // go through the protein hits
            @SuppressWarnings("unchecked")
            List<ProteinHit> proteins = peptideHit.getProteinHits();
            for (ProteinHit proteinHit : proteins) {

                FastaHeaderInfos fastaInfo =
                        FastaHeaderInfos.parseHeaderInfos(proteinHit.getAccession());

                if (fastaInfo == null) {
                    fastaInfo = new FastaHeaderInfos(null,
                            proteinHit.getAccession(),
                            proteinMap.getProteinID(proteinHit.getAccession()).getDescription());
                } else {
                    // if there was a protein description different to the now parsed one, take the original from mascot
                    String proteinDescription = proteinMap.
                            getProteinID(proteinHit.getAccession()).
                            getDescription();
                    if ((proteinDescription != null) &&
                            (proteinDescription.trim().length() > 0) &&
                            !proteinDescription.equals(fastaInfo.getDescription())) {
                        fastaInfo = new FastaHeaderInfos(null,
                                fastaInfo.getAccession(),
                                proteinDescription);
                    }
                }

                // add the Accession to the compiler (if it is not already there)
                Accession acc = compiler.getAccession(fastaInfo.getAccession());
                if (acc == null) {
                    // unfortunately, the sequence is not stored in the dat file
                    acc = compiler.insertNewAccession(
                            fastaInfo.getAccession(), null);
                }

                acc.addFile(file.getID());

                if ((fastaInfo.getDescription() != null) &&
                        (fastaInfo.getDescription().length() > 0)) {
                    acc.addDescription(file.getID(),
                            fastaInfo.getDescription());
                }

                acc.addSearchDatabaseRef(searchDatabase.getId());

                // add the accession occurrence to the peptide
                peptide.addAccessionOccurrence(acc,
                        proteinHit.getStart(), proteinHit.getStop());

                // now insert the connection between peptide and accession into the compiler
                compiler.addAccessionPeptideConnection(acc, peptide);
            }

            // add the scores
            ScoreModel score;

            score = new ScoreModel(peptideHit.getIonsScore(),
                    ScoreModelEnum.MASCOT_SCORE);
            psm.addScore(score);

            score = new ScoreModel(peptideHit.getExpectancy(),
                    ScoreModelEnum.MASCOT_EXPECT);
            psm.addScore(score);

            // add the modifications
            com.compomics.mascotdatfile.util.interfaces.Modification[] mods = peptideHit.getModifications();
            for (int loc = 0; loc < mods.length; loc++) {
                if (mods[loc] != null) {
                    Modification modification;

                    Character residue;
                    if ((loc == 0) || (loc > psm.getSequence().length())) {
                        residue = '.';
                    } else {
                        residue = psm.getSequence().charAt(loc - 1);
                    }

                    modification = new Modification(
                            residue,
                            mods[loc].getMass(),
                            mods[loc].getType(),
                            null);

                    psm.addModification(loc, modification);
                }
            }

            compiler.insertCompletePeptideSpectrumMatch(psm);
            nrPepHits++;
        }

        return nrPepHits;
    }


    private static SearchModification createPSIModification(com.compomics.mascotdatfile.util.interfaces.Modification mod, UnimodParser uniModParser) {
        SearchModification searchMod = new SearchModification();

        searchMod.setFixedMod(!(mod instanceof VariableModification));

        if (mod.getLocation().contains("term") || mod.getLocation().contains("Term")) {

            OntologyConstants modConstant = null;
            if (mod.getLocation().startsWith("Protein N")) {
                modConstant = OntologyConstants.MODIFICATION_SPECIFICITY_PROTEIN_N_TERM;
            } else if (mod.getLocation().startsWith("Protein C")) {
                modConstant = OntologyConstants.MODIFICATION_SPECIFICITY_PROTEIN_C_TERM;
            } else if (mod.getLocation().startsWith("N")) {
                modConstant = OntologyConstants.MODIFICATION_SPECIFICITY_PEP_N_TERM;
            } else if (mod.getLocation().startsWith("C")) {
                modConstant = OntologyConstants.MODIFICATION_SPECIFICITY_PEP_C_TERM;
            }

            if (modConstant != null) {
                CvParam specificity = MzIdentMLTools.createPSICvParam(modConstant, null);

                SpecificityRules specRules = new SpecificityRules();
                specRules.getCvParam().add(specificity);
                searchMod.getSpecificityRules().add(specRules);

                String[] residues = mod.getLocation().split("erm");
                if (residues.length > 1) {
                    for (Character residue : residues[1].trim().toCharArray()) {
                        if (residue != ' ') {
                            searchMod.getResidues().add(residue.toString());
                        }
                    }
                } else {
                    searchMod.getResidues().add(".");
                }
            }
        } else {
            for (Character residue : mod.getLocation().toCharArray()) {
                searchMod.getResidues().add(residue.toString());
            }
        }
        searchMod.setMassDelta((float)mod.getMass());

        ModT unimod = uniModParser.getModificationByNameAndMass(
                mod.getType(),
                mod.getMass(),
                searchMod.getResidues());
        if (unimod != null) {
            CvParam cvParam = new CvParam();
            cvParam.setAccession("UNIMOD:" + unimod.getRecordId());
            cvParam.setCv(UnimodParser.getCv());
            cvParam.setName(unimod.getTitle());
            searchMod.getCvParam().add(cvParam);
        }

        return searchMod;
    }



    /**
     * Checks, whether the given file looks like a Mascot DAT file
     *
     * @param fileName
     * @return
     */
    public static boolean checkFileType(String fileName) {
        boolean isMascotFile = false;
        LOGGER.debug("checking whether this is a mascot DAT file: " + fileName);

        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            // read in the first 50, not empty lines
            List<String> lines = stream.filter(line -> !line.trim().isEmpty())
                    .limit(250)
                    .collect(Collectors.toList());

            // check, if the labels are found
            boolean foundMultipartMixed = false;
            boolean foundMascot = false;
            boolean foundTOL = false;
            boolean foundTOLU = false;
            boolean foundITOL = false;
            boolean foundITOLU = false;
            boolean foundFILE = false;
            boolean foundFORMAT = false;

            for (String line : lines) {
                if (line.contains("Content-Type: multipart/mixed;")) {
                    foundMultipartMixed = true;
                } else if (line.contains("Content-Type: application/x-Mascot;")) {
                    foundMascot = true;
                } else if (line.startsWith("TOL=")) {
                    foundTOL = true;
                } else if (line.startsWith("TOLU=")) {
                    foundTOLU = true;
                } else if (line.startsWith("ITOL=")) {
                    foundITOL = true;
                } else if (line.startsWith("ITOLU=")) {
                    foundITOLU = true;
                } else if (line.startsWith("FILE=")) {
                    foundFILE = true;
                } else if (line.startsWith("FORMAT=")) {
                    foundFORMAT = true;
                }
            }

            isMascotFile = foundMultipartMixed;
            isMascotFile &= foundMascot;
            isMascotFile &= foundTOL;
            isMascotFile &= foundTOLU;
            isMascotFile &= foundITOL;
            isMascotFile &= foundITOLU;
            isMascotFile &= foundFILE;
            isMascotFile &= foundFORMAT;
        } catch (Exception e) {
            LOGGER.debug("Could not check file " + fileName, e);
        }

        return isMascotFile;
    }
}
