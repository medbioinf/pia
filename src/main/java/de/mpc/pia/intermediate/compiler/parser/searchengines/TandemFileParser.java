package de.mpc.pia.intermediate.compiler.parser.searchengines;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

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
import de.mpc.pia.modeller.IdentificationKeySettings;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.tools.MzIdentMLTools;
import de.mpc.pia.tools.OntologyConstants;
import de.mpc.pia.tools.PIAConstants;
import de.mpc.pia.tools.PIATools;
import de.mpc.pia.tools.unimod.UnimodParser;
import de.proteinms.xtandemparser.parser.XTandemParser;
import de.proteinms.xtandemparser.xtandem.Domain;
import de.proteinms.xtandemparser.xtandem.InputParams;
import de.proteinms.xtandemparser.xtandem.PeptideMap;
import de.proteinms.xtandemparser.xtandem.Protein;
import de.proteinms.xtandemparser.xtandem.ProteinMap;
import de.proteinms.xtandemparser.xtandem.Spectrum;
import de.proteinms.xtandemparser.xtandem.XTandemFile;


/**
 * This class parses the data from a mzIdentML file for a given
 * {@link PIACompiler}.<br/>
 *
 * @author julian
 *
 */
public class TandemFileParser {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(TandemFileParser.class);


    /** this pattern matches a special case of RT, which occurs from mzML files */
    private static Pattern patternMzMLRT = Pattern.compile("^PT(\\d+(\\.\\d+))S$");


    /**
     * We don't ever want to instantiate this class
     */
    private TandemFileParser() {
        throw new AssertionError();
    }


    /**
     * Parses the data from an mzIdentML file given by its name into the given
     * {@link PIACompiler}.
     *
     * @param fileName name of the XTandem XML result file
     * @param compiler the PIACompiler
     * @param rtMapFileName maps from the spectrum ID to the retentionTime
     */
    public static boolean getDataFromTandemFile(String name, String fileName,
            PIACompiler compiler, String rtMapFileName) {
        int accNr = 0;
        int pepNr = 0;
        int specNr = 0;

        Map<Integer, Double> rtMap = new HashMap<>();

        if ((rtMapFileName != null) && (rtMapFileName.length() > 0)) {
            // additional RT info is given, parse the file
            try (FileInputStream rtStream = new FileInputStream(rtMapFileName)) {
                LOGGER.info("Parsing the file '" + rtMapFileName + '\''
                        + " for RT information.");

                DataInputStream in = new DataInputStream(rtStream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));

                String strLine;
                while ((strLine = br.readLine()) != null) {
                    String[] v = strLine.split("\t");
                    rtMap.put(Integer.parseInt(v[0]), Double.parseDouble(v[1]));
                }

                in.close();
            } catch (Exception e) {
                LOGGER.error("Error while parsing the RT info file " +
                        rtMapFileName + ", program will continue, " +
                        "but you won't have RT information", e);
                rtMap.clear();
            }
        }

        // TODO: test for multiple databases!
        Map<String, SearchDatabase> searchDatabaseMap = // maps from the "sequence source" to the SearchDatabse object
                new HashMap<>();
        FileFormat fileFormat;

        /* tandemParser.getPerformParamMap()
         * POINTMUT=0
         * TOTALPROTUSED=1032162
         * TOTALSPECUSED=261
         * TOTALSPECASS=145
         * TOTALUNIQUEASS=139
         * TOTALPEPUSED=18158050
         * SEQSRC1=/var/www/thegpm/fasta/uniprot_sprot_trembl/uniprot_sprot_decoy.fasta
         * INITMODELSPECTIME=0.221
         * LOADSEQMODELTIME=3.17
         * INITMODELTOTALTIME=57.65
         * INPUTMOD=0
         * POTC_TERM=0
         * ESTFP=1
         * UNANTICLEAV=0
         * PARTCLEAV=0
         * PROCVER=x! tandem TORNADO (2010.01.01.4)
         * PROCSTART=2012:01:24:15:18:04
         * REFINETIME=0.000
         * POTN_TERM=0
         * SEQSRCDESC1=no description
         * QUALVAL=25 14 16 15 17 15 12 14 5 13 8 4 4 4 6 4 3 2 2 3
         * INPUTSPEC=0
         */
        // only three sources are possible with the parser, take the file...
        File tandemFile = new File(fileName);
        if (!tandemFile.canRead()) {
            // TODO: better error / exception
            LOGGER.error("could not read '" + fileName + "'.");
            return false;
        }
        XTandemParser tandemParser;
        try {
            tandemParser = new XTandemParser(tandemFile);
        } catch (Exception e) {
            // TODO: better error / exception
            LOGGER.error("could not parse '" + fileName + "'.", e);
            return false;
        }
        for (Map.Entry<String, String> performParam : tandemParser.getPerformParamMap().entrySet()) {
            if (performParam.getKey().startsWith("SEQSRC") &&
                    !performParam.getKey().startsWith("SEQSRCDESC")) {
                // create the searchDatabase and add it to the compiler
                SearchDatabase searchDatabase = new SearchDatabase();

                int dbNr = Integer.parseInt(performParam.getKey().substring(6));

                // required
                searchDatabase.setId("tandemDB" + dbNr);
                searchDatabase.setLocation(performParam.getValue());

                // TODO: set searchDatabase.setName("") to tandemParser.getInputParamMap("TAXON")
                // <note type="input" label="protein, taxon">uniprot_decoy</note> (comma separated)

                // optional
                /*
                TODO: are these accessible with multiple databases
                searchDatabase.setNumDatabaseSequences();
                searchDatabase.setNumResidues();
                 */

                // fileformat
                fileFormat = new FileFormat();
                fileFormat.setCvParam(MzIdentMLTools.createPSICvParam(OntologyConstants.FASTA_FORMAT, null));
                searchDatabase.setFileFormat(fileFormat);

                // databaseName
                Param param = new Param();
                param.setParam(MzIdentMLTools.createUserParam(
                        "databaseName",
                        tandemParser.getPerformParamMap().get("SEQSRCDESC" + dbNr),
                        "string"));
                searchDatabase.setDatabaseName(param);

                searchDatabase = compiler.putIntoSearchDatabasesMap(searchDatabase);

                searchDatabaseMap.put(performParam.getValue(), searchDatabase);
            }
        }

        // now go on with the XTandemFile (easier access)
        XTandemFile xtandemFile;

        try {
            xtandemFile = new XTandemFile(fileName);
        } catch (Exception e) {
            // TODO: better error / exception
            LOGGER.error("could not parse '" + fileName + "'.", e);
            return false;
        }

        PIAInputFile file = compiler.insertNewFile(name, fileName,
                InputFileParserFactory.InputFileTypes.TANDEM_INPUT.getFileSuffix());

        // create the analysis software and add it to the compiler
        AnalysisSoftware tandem = new AnalysisSoftware();

        tandem.setId("tandem");
        tandem.setName("tandem");
        tandem.setUri("http://www.thegpm.org/TANDEM/index.html");
        String strParam = xtandemFile.getPerformParameters().getProcVersion();
        if (strParam != null) {
            tandem.setVersion(strParam);
        }

        Param tandemParam = new Param();
        tandemParam.setParam(MzIdentMLTools.createPSICvParam(OntologyConstants.XTANDEM, null));
        tandem.setSoftwareName(tandemParam);

        tandem = compiler.putIntoSoftwareMap(tandem);

        // TODO: add refinement (modifications and all other stuff

        /* tandemParser.getInputParamMap()
         * N_TERMCLEAVMASSCHANGE=+1.007825
         * SPECPARENTMASSERRORMINUS=0.15
         * SPECMINPEAKS=15
         * SCORINGMINIONCOUNT=4
         * SPECMAXPRECURSORCHANGE=4
         * REFINEUNANTICLEAV=yes
         * POINTMUTATIONS=no
         * POTMODMASS=15.994915@M,15.994915@W,0.984016@N,0.984016@Q
         * RESIDUEPOTMODMASS=15.994915@M
         * SPECPARENTMASSERRORPLUS=0.15
         * SPECMONOISOMASSERROR=0.3
         * C_TERMRESMODMASS=0.0
         * SPECTOTALPEAK=50
         * SCORING_BIONS=yes
         * SPECPARENTMASSISOERROR=yes
         * SPECTHREADS=1
         * HISTOEXIST=yes
         * OUTPUTSPECTRA=yes
         * C_TERMCLEAVMASSCHANGE=+17.002735
         * REFINE=no
         * SCORINGMISSCLEAV=1
         * SPECMONOISOMASSERRORUNITS=Daltons
         * MAXVALIDEXPECT=0.1
         * HISTOCOLWIDTH=30
         * SPECMINPRECURSORMZ=500.0
         * SPECTRUMPATH=/usr/tmp/CGItemp4088
         * CLEAVAGESITE=[RK]|{P}
         * N_TERMRESMODMASS=0.0
         * POTMODMASS_1=31.98983@M,31.98983@W
         * OUTPUTPERFORMANCE=yes
         * TAXONOMYINFOPATH=../tandem/taxonomy.xml
         * OUTPUTPROTEINS=yes
         * SPECMINFRAGMZ=150.0
         * OUTPUTPARAMS=yes
         * OUTPUTPATH=../gpm/archive/GPM77700000033.xml
         * SCORINGINCREV=no
         * OUTPUTRESULTS=valid
         * DEFAULTPARAMPATH=../tandem/methods/qstar.xml
         * SPECPARENTMASSERRORUNITS=Daltons
         * SPECDYNRANGE=100.0
         * SCORINGPLUGSCORING=yes
         * OUTPUTSEQUENCES=yes
         * REFINEMAXVALIDEXPECT=0.01
         * SCORING_YIONS=yes
         * SPECUSENOISECOMP=no
         * REFINESPECSYTNH=yes
         * TAXON=uniprot_decoy
         * OUTPUTSORTRESULTS=protein
         * POTMODSFULLREFINE=no
         * OUTPUTPATHHASH=no
         * OUTPUTSXSLPATH=/tandem/tandem-style.xsl
         * SPECFRAGMASSTYPE=monoisotopic
         */
        InputParams inputParams = xtandemFile.getInputParameters();

        // add the spectraData (input file)
        SpectraData spectraData = new SpectraData();

        spectraData.setId("tandemInputMGF");
        spectraData.setLocation(inputParams.getSpectrumPath());
        // TODO: for now write MGF, though it could be mzML as well
        fileFormat = new FileFormat();
        fileFormat.setCvParam(MzIdentMLTools.createPSICvParam(
                OntologyConstants.MASCOT_MGF_FORMAT, null));
        spectraData.setFileFormat(fileFormat);
        SpectrumIDFormat idFormat = new SpectrumIDFormat();
        idFormat.setCvParam(MzIdentMLTools.createPSICvParam(
                OntologyConstants.MULTIPLE_PEAK_LIST_NATIVEID_FORMAT, null));
        spectraData.setSpectrumIDFormat(idFormat);

        spectraData = compiler.putIntoSpectraDataMap(spectraData);


        // define the spectrumIdentificationProtocol
        SpectrumIdentificationProtocol spectrumIDProtocol =
                new SpectrumIdentificationProtocol();

        spectrumIDProtocol.setId("tandemAnalysis");
        spectrumIDProtocol.setAnalysisSoftware(tandem);

        Param searchTypeParam = new Param();
        searchTypeParam.setParam(MzIdentMLTools.createPSICvParam(OntologyConstants.MS_MS_SEARCH, null));
        spectrumIDProtocol.setSearchType(searchTypeParam);

        ParamList paramList = new ParamList();
        // there does not appear to be a way in Tandem of specifying parent mass is average
        paramList.getCvParam().add(
                MzIdentMLTools.createPSICvParam(OntologyConstants.PARENT_MASS_TYPE_MONO, null));

        boolean fragmentMonoisotopic = false;
        if (inputParams.getSpectrumFragMassType() != null) {
            CvParam fragMassType;
            if ("monoisotopic".equalsIgnoreCase(inputParams.getSpectrumFragMassType())) {
                fragMassType = MzIdentMLTools.createPSICvParam(
                        OntologyConstants.FRAGMENT_MASS_TYPE_MONO, null);
                fragmentMonoisotopic = true;
            } else {
                fragMassType = MzIdentMLTools.createPSICvParam(
                        OntologyConstants.FRAGMENT_MASS_TYPE_AVERAGE, null);
                fragmentMonoisotopic = false;
            }
            paramList.getCvParam().add(fragMassType);

        }

        spectrumIDProtocol.setAdditionalSearchParams(paramList);


        ModificationParams modParams = new ModificationParams();
        addSearchModifications(inputParams.getResidueModMass(), true, modParams);
        addSearchModifications(inputParams.getResiduePotModMass(), false, modParams);
        spectrumIDProtocol.setModificationParams(modParams);

        // TODO: add the modifications given by tandem's "quick acetyl" and "quick pyrolidone"

        Enzymes enzymes = new Enzymes();
        // TODO: add semi-cleavage behaviour
        /*
         * C_TERMCLEAVMASSCHANGE=+17.002735
         * N_TERMCLEAVMASSCHANGE=+1.007825
         */
        Enzyme enzyme = new Enzyme();

        enzyme.setId("enzyme");
        enzyme.setMissedCleavages(inputParams.getScoringMissCleavageSites());

        strParam = inputParams.getProteinCleavageSite();
        if (strParam != null) {
            enzyme.setSiteRegexp(strParam);

            String[] cleavages = strParam.split(",");

            if (cleavages.length > 1) {
                LOGGER.warn("Only one enzyme (cleavage site) implemented yet.");
            }

            if (cleavages.length > 0) {
                strParam = cleavages[0];


                String[] values = strParam.split("\\|");
                String pre = values[0].substring(1, values[0].length() - 1);
                if ("X".equalsIgnoreCase(pre)) {
                    // X stands for any, make it \S in PCRE for "not whitespace"
                    pre = "\\S";
                } else if (pre.length() > 1) {
                    pre = '[' + pre + ']';
                }
                if (values[0].startsWith("[") && values[0].endsWith("]")) {
                    pre = "(?<=" + pre + ')';
                } else if (values[0].startsWith("{") && values[0].endsWith("}")) {
                    pre = "(?<!" + pre + ')';
                }

                String post = values[1].substring(1, values[1].length() - 1);
                if ("X".equalsIgnoreCase(post)) {
                    // X stands for any, make it \S in PCRE for "not whitespace"
                    post = "\\S";
                } else if (post.length() > 1) {
                    post = '[' + post + ']';
                }
                if (values[1].startsWith("[") && values[1].endsWith("]")) {
                    post = "(?=" + post + ')';
                } else if (values[1].startsWith("{") && values[1].endsWith("}")) {
                    post = "(?!" + post + ')';
                }

                enzyme.setSiteRegexp(pre + post);
            } else {
                LOGGER.error("No cleavage site found!");
            }
        }

        enzymes.getEnzyme().add(enzyme);
        spectrumIDProtocol.setEnzymes(enzymes);


        Tolerance tolerance = new Tolerance();

        if (fragmentMonoisotopic) {
            double fragmentError;
            String units;

            fragmentError = inputParams.getSpectrumMonoIsoMassError();
            units = inputParams.getSpectrumMonoIsoMassErrorUnits();

            CvParam tolParam = MzIdentMLTools.createPSICvParam(
                    OntologyConstants.SEARCH_TOLERANCE_PLUS_VALUE,
                    String.valueOf(Double.toString(fragmentError)));
            MzIdentMLTools.setUnitParameterFromString(units, tolParam);
            tolerance.getCvParam().add(tolParam);

            tolParam = MzIdentMLTools.createPSICvParam(
                    OntologyConstants.SEARCH_TOLERANCE_MINUS_VALUE,
                    String.valueOf(Double.toString(fragmentError)));
            MzIdentMLTools.setUnitParameterFromString(units, tolParam);
            tolerance.getCvParam().add(tolParam);

            spectrumIDProtocol.setFragmentTolerance(tolerance);
        } else {
            // TODO: implement average fragment mass
        }

        tolerance = new Tolerance();

        CvParam tolParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.SEARCH_TOLERANCE_PLUS_VALUE,
                String.valueOf(inputParams.getSpectrumParentMonoIsoMassErrorPlus()));
        MzIdentMLTools.setUnitParameterFromString(
                inputParams.getSpectrumParentMonoIsoMassErrorUnits(), tolParam);
        tolerance.getCvParam().add(tolParam);

        tolParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.SEARCH_TOLERANCE_MINUS_VALUE,
                String.valueOf(inputParams.getSpectrumParentMonoIsoMassErrorMinus()));
        MzIdentMLTools.setUnitParameterFromString(
                inputParams.getSpectrumParentMonoIsoMassErrorUnits(), tolParam);
        tolerance.getCvParam().add(tolParam);

        spectrumIDProtocol.setParentTolerance(tolerance);

        /* TODO: tandem has the "output, maximum valid expectation value" and
         * "output, maximum valid protein expectation" set, this is a threshold...
        // no threshold set, take all PSMs from the file
        paramList = new ParamList();
        abstractParam = new CvParam();
        ((CvParam)abstractParam).setAccession("MS:1001494");
        ((CvParam)abstractParam).setCv(psiMS);
        abstractParam.setName("no threshold");
        paramList.getCvParam().add((CvParam)abstractParam);
        spectrumIDProtocol.setThreshold(paramList);
         */
        file.addSpectrumIdentificationProtocol(spectrumIDProtocol);


        // add the spectrum identification
        SpectrumIdentification spectrumID = new SpectrumIdentification();
        spectrumID.setId("tandemIdentification");
        spectrumID.setSpectrumIdentificationList(null);
        spectrumID.setSpectrumIdentificationProtocol(spectrumIDProtocol);

        InputSpectra inputSpectra = new InputSpectra();
        inputSpectra.setSpectraData(spectraData);
        spectrumID.getInputSpectra().add(inputSpectra);

        SearchDatabaseRef searchDBRef;
        for (SearchDatabase sDB : searchDatabaseMap.values()) {
            searchDBRef = new SearchDatabaseRef();
            searchDBRef.setSearchDatabase(sDB);
            spectrumID.getSearchDatabaseRef().add(searchDBRef);
        }

        file.addSpectrumIdentification(spectrumID);

        // TODO: add the heaps of other settings... they are almost all in the XML-files


        // to later check, whether the PSM is already there, we need the psmSetSettings map
        Map<String, Boolean> psmSetSettings = new HashMap<>();
        psmSetSettings.put(IdentificationKeySettings.SOURCE_ID.name(), true);
        psmSetSettings.put(IdentificationKeySettings.SEQUENCE.name(), true);
        psmSetSettings.put(IdentificationKeySettings.MODIFICATIONS.name(), true);
        psmSetSettings.put(IdentificationKeySettings.CHARGE.name(), true);

        // now go through the spectra
        Iterator<?> iter = xtandemFile.getSpectraIterator();
        PeptideMap pepMap = xtandemFile.getPeptideMap();
        ProteinMap protMap = xtandemFile.getProteinMap();

        while (iter.hasNext()) {
            Object nxt = iter.next();
            Spectrum spectrum;
            if (nxt instanceof Spectrum){
                spectrum = (Spectrum)nxt;
            } else {
                continue;
            }

            int charge = spectrum.getPrecursorCharge();
            double precursorMZ = (spectrum.getPrecursorMh() +
                    charge * PIAConstants.H_MASS.doubleValue() -
                    PIAConstants.H_MASS.doubleValue()) / charge;
            precursorMZ = PIATools.round(precursorMZ, 6);

            String sourceID = "index=" + (spectrum.getSpectrumId()-1);

            String spectrumTitle = xtandemFile.
                    getSupportData(spectrum.getSpectrumNumber()).
                    getFragIonSpectrumDescription();

            // check for scan number in the title, if it is there, take the title as sourceID
            Matcher matcher = MzIdentMLTools.patternScanInTitle.matcher(spectrumTitle);
            if (matcher.matches()) {
                sourceID = "index=" + matcher.group(1);
            }

            List<de.proteinms.xtandemparser.xtandem.Peptide> pepList =
                    pepMap.getAllPeptides(spectrum.getSpectrumNumber());


            String rtStr = spectrum.getPrecursorRetentionTime();
            Double rt = null;
            if ((rtStr != null) && (rtStr.trim().length() > 0)) {
                rtStr = rtStr.trim();

                try {
                    matcher = patternMzMLRT.matcher(rtStr);
                    if (matcher.matches()) {
                        rt = Double.parseDouble(matcher.group(1));
                        // the RT is also somehow wrong, fix this
                        rt = rt / 60.0;
                    } else {
                        // try to get the RT directly from tandem
                        rt = Double.parseDouble(rtStr);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error("Could not parse RT: ", e);
                    rt = null;
                }
            }

            if (rt == null) {
                // if the RT is still null, try the mapping from OpenMS-file
                rt = rtMap.get(spectrum.getSpectrumId()-1);
            }


            // we need a map, to store the PSMs of this spectrum
            Map<String, PeptideSpectrumMatch> keysToPSMs;
            keysToPSMs = new HashMap<>();

            for (de.proteinms.xtandemparser.xtandem.Peptide pep : pepList) {

                for (Domain domain : pep.getDomains()) {
                    // a domain is a PSM in a protein, therefore this may be already in the compiler
                    String sequence = domain.getDomainSequence();

                    // to check, whether the PSM is already there, the modifications
                    // are needed.
                    // note: there can not be a PSM with all equal except the scores!

                    // create the modifications
                    Map<Integer, Modification> modifications =
                            new HashMap<>();

                    // variable mods
                    modifications.putAll(createModifications(
                            xtandemFile.getModificationMap().getVariableModifications(domain.getDomainKey()),
                            sequence, domain.getDomainStart(), modParams));

                    // fixed mods
                    modifications.putAll(createModifications(
                            xtandemFile.getModificationMap().getFixedModifications(domain.getDomainKey()),
                            sequence, domain.getDomainStart(), modParams));

                    String psmKey = PeptideSpectrumMatch.getIdentificationKey(
                            psmSetSettings,
                            sequence,
                            PeptideSpectrumMatch.getModificationString(modifications),  // no different rounding in the same file, so this should be safe
                            charge,
                            null,
                            null,
                            sourceID,
                            null,
                            null);

                    Peptide peptide;
                    PeptideSpectrumMatch psm = keysToPSMs.get(psmKey);

                    if (psm == null) {
                        psm = compiler.createNewPeptideSpectrumMatch(
                                charge,
                                precursorMZ,
                                PIATools.round(spectrum.getPrecursorMh()-domain.getDomainMh(), 6),
                                rt,
                                sequence,
                                domain.getMissedCleavages(),
                                sourceID,
                                spectrumTitle,
                                file,
                                spectrumID,
                                null);
                        specNr++;
                        keysToPSMs.put(psmKey, psm);

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

                        score = new ScoreModel(domain.getDomainExpect(),
                                ScoreModelEnum.XTANDEM_EXPECT);
                        psm.addScore(score);

                        score = new ScoreModel(domain.getDomainHyperScore(),
                                ScoreModelEnum.XTANDEM_HYPERSCORE);
                        psm.addScore(score);

                        // the PSm is finished now
                        compiler.insertCompletePeptideSpectrumMatch(psm);
                    } else {
                        // if the PSM is already in the compiler, the peptide must be there as well
                        peptide = compiler.getPeptide(sequence);
                        if (peptide == null) {
                            LOGGER.error("The peptide " + sequence +
                                    " was not found in the compiler!");
                            continue;
                        }
                    }

                    // get the protein infos
                    Protein protein = protMap.getProtein(domain.getProteinKey());

                    FastaHeaderInfos fastaInfo =
                            FastaHeaderInfos.parseHeaderInfos(protein.getLabel());

                    if (fastaInfo == null) {
                        LOGGER.error("Could not parse '" +
                                protein.getLabel() + '\'');
                        continue;
                    }

                    // add the Accession to the compiler (if it is not already there)
                    Accession acc = compiler.getAccession(fastaInfo.getAccession());
                    if (acc == null) {
                        // sequence will be added later (without whitespaces)
                        acc = compiler.insertNewAccession(
                                fastaInfo.getAccession(), null);
                        accNr++;
                    }

                    acc.addFile(file.getID());

                    if ((fastaInfo.getDescription() != null) &&
                            (fastaInfo.getDescription().length() > 0)) {
                        acc.addDescription(file.getID(), fastaInfo.getDescription());
                    }


                    String proteinSequence = pep.getSequence();
                    if (proteinSequence != null) {
                        // remove whitespaces and breaks
                        proteinSequence = proteinSequence.replaceAll("\\s", "");

                        if (acc.getDbSequence() != null)  {
                            if (!proteinSequence.equals(acc.getDbSequence())) {
                                LOGGER.warn("Different DBSequences found for same Accession, this is not suported!\n" +
                                        "\t Accession: " + acc.getAccession() +
                                        "\t'" + proteinSequence + "'\n" +
                                        "\t'" + acc.getDbSequence() + '\'');
                            }
                        } else {
                            acc.setDbSequence(proteinSequence);
                        }
                    }

                    // add the searchDB to the accession
                    SearchDatabase sDB =
                            searchDatabaseMap.get(pep.getFastaFilePath());
                    if (sDB != null) {
                        acc.addSearchDatabaseRef(sDB.getId());
                    }

                    // add the accession occurrence to the peptide
                    peptide.addAccessionOccurrence(acc,
                            domain.getDomainStart(), domain.getDomainEnd());


                    // now insert the connection between peptide and accession into the compiler
                    compiler.addAccessionPeptideConnection(acc, peptide);
                }

            }
        }

        LOGGER.info("inserted new: \n\t" +
                pepNr + " peptides\n\t" +
                specNr + " peptide spectrum matches\n\t" +
                accNr + " accessions");
        return true;
    }


    /**
     * Adds the modifications in the tandem encoded strParam to the
     * {@link ModificationParams}.
     *
     * @param strParam modifications encoded as specified by the X!Tandem API
     * @param isFixed whether these are fixed or potential modifications
     * @param modParams the list of {@link SearchModification}s
     */
    private static void addSearchModifications(String strParam, boolean isFixed,
            ModificationParams modParams) {
        if (modParams == null) {
            LOGGER.error("modParams is nt initialised, cannot add any modifications!");
            return;
        }

        if (strParam != null) {
            // these are modifications (w/o refinement)
            String[] varMods = strParam.split(",");

            for(String varMod : varMods) {
                if (!"None".equalsIgnoreCase(varMod)) {

                    String[] values = varMod.split("@");

                    SearchModification searchMod = new SearchModification();
                    searchMod.setFixedMod(isFixed);
                    searchMod.setMassDelta(Float.parseFloat(values[0]));

                    if ("[".equals(values[1]) || "]".equals(values[1])) {
                        searchMod.getResidues().add(".");

                        OntologyConstants modConstant;
                        if ("[".equals(values[1])) {
                            modConstant = OntologyConstants.MODIFICATION_SPECIFICITY_PEP_N_TERM;
                        } else {
                            modConstant = OntologyConstants.MODIFICATION_SPECIFICITY_PEP_C_TERM;
                        }

                        CvParam specificity = MzIdentMLTools.createPSICvParam(modConstant, null);

                        SpecificityRules specRules = new SpecificityRules();
                        specRules.getCvParam().add(specificity);
                        searchMod.getSpecificityRules().add(specRules);
                    } else {
                        searchMod.getResidues().add(values[1]);
                    }

                    modParams.getSearchModification().add(searchMod);
                }
            }
        }
    }


    /**
     * Create a List of {@link Modification}s with the given data from the
     * tandem file
     *
     * @param mods
     * @param peptideSequence
     * @param domainStart
     * @param modParams these are the (user given) modification parameters for
     * the search, they are used for cross-checking against N- and C-terminal
     * modifications, as they are ambiguously encoded in the tandem XML file
     * @return
     */
    private static Map<Integer, Modification> createModifications(
            List<de.proteinms.xtandemparser.interfaces.Modification> mods,
            String peptideSequence, int domainStart,
            ModificationParams modParams) {

        Map<Integer, Modification> modifications =
                new HashMap<>(mods.size());

        for (de.proteinms.xtandemparser.interfaces.Modification mod : mods) {

            int loc = Integer.parseInt(mod.getLocation()) -
                    domainStart + 1;

            if ((loc < 0) || (loc > peptideSequence.length() + 1)) {
                LOGGER.error("weird location for modification: '" + mod.getLocation() + "' in " + peptideSequence + ", domainStart: " + domainStart);
            }

            if (loc == 1) {
                // this might be a N-terminal modification
                //  => check against suitable settings
                for (SearchModification searchMod
                        : modParams.getSearchModification()) {
                    if ((searchMod.getSpecificityRules() != null)
                            && !searchMod.getSpecificityRules().isEmpty()) {
                        for (SpecificityRules rule
                                : searchMod.getSpecificityRules()) {
                            for (CvParam cvParam : rule.getCvParam()) {
                                if (cvParam.getAccession().equals(OntologyConstants.MODIFICATION_SPECIFICITY_PEP_N_TERM.getPsiAccession())) {
                                    loc = 0;
                                    break;
                                }
                            }
                        }
                    }
                }

                // the quick acetyl and quick pyrolidone are also n-terminal
                if (Math.abs(mod.getMass() - 42.010565) < UnimodParser.UNIMOD_MASS_TOLERANCE) {
                    // acetylation
                    Modification modification = new Modification('.',
                            42.0105647,
                            "Acetyl",
                            "UNIMOD:1");
                    modifications.put(0, modification);
                    continue;
                } else if ((Math.abs(mod.getMass() + 18.010565) < UnimodParser.UNIMOD_MASS_TOLERANCE) ||
                        (Math.abs(mod.getMass() + 17.026549) < UnimodParser.UNIMOD_MASS_TOLERANCE)) {
                    // pyrolidone
                    loc = 0;
                }
            } else if (loc == peptideSequence.length()) {
                // this might be a C-terminal modification
                //  => check against suitable settings
                for (SearchModification searchMod
                        : modParams.getSearchModification()) {
                    if ((searchMod.getSpecificityRules() != null) &&
                            !searchMod.getSpecificityRules().isEmpty()) {
                        for (SpecificityRules rule
                                : searchMod.getSpecificityRules()) {
                            for (CvParam cvParam : rule.getCvParam()) {
                                if (cvParam.getAccession().equals(OntologyConstants.MODIFICATION_SPECIFICITY_PEP_C_TERM.getPsiAccession())) {
                                    loc = peptideSequence.length()+1;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            char residue;
            if ((loc == 0) || (loc > peptideSequence.length())) {
                residue = '.';
            } else {
                residue = peptideSequence.charAt(loc-1);
            }

            Modification modification = new Modification(residue,
                    mod.getMass(),
                    null,       // no description
                    null);      // no CV accession

            modifications.put(loc, modification);
        }

        return modifications;
    }


    /**
     * Checks, whether the given file looks like an X!Tandem XML file
     *
     * @param fileName
     * @return
     */
    public static boolean checkFileType(String fileName) {
        boolean isTandemFile = false;
        LOGGER.debug("checking whether this is an X!Tandem XML file: " + fileName);

        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            // read in the first 10, not empty lines
            List<String> lines = stream.filter(line -> !line.trim().isEmpty())
                    .limit(100)
                    .collect(Collectors.toList());

            // check, if first lines are ok
            int idx = 0;

            // optional declaration
            if (lines.get(idx).trim().matches("<\\?xml version=\"[0-9.]+\"( encoding=\"[^\"]+\"){0,1}\\?>")) {
                LOGGER.debug("file has the XML declaration line:" + lines.get(idx));
                idx++;
            }

            // optional stylesheet declaration
            if (lines.get(idx).trim().matches("<\\?xml-stylesheet.+\\?>")) {
                LOGGER.debug("file has the XML stylesheet line:" + lines.get(idx));
                idx++;
            }

            // now the bioml element must be next
            if (lines.get(idx).trim().matches("<bioml .+")) {
                isTandemFile = true;
                LOGGER.debug("file has the bioml element: " + lines.get(idx));
            }


            boolean groupFound = false;
            boolean proteinFound = false;

            for (String line : lines) {
                if (line.contains("<group ")) {
                    groupFound = true;
                } else if (line.contains("<protein ")) {
                    proteinFound = true;
                }
            }

            isTandemFile &= groupFound;
            isTandemFile &= proteinFound;
        } catch (Exception e) {
            LOGGER.debug("Could not check file " + fileName, e);
        }

        return isTandemFile;
    }
}
