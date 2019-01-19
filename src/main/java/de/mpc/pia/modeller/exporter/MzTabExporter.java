package de.mpc.pia.modeller.exporter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.AccessionOccurrence;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.tools.CleavageAgent;
import de.mpc.pia.tools.MzIdentMLTools;
import de.mpc.pia.tools.OntologyConstants;
import de.mpc.pia.tools.PIAConstants;
import de.mpc.pia.tools.unimod.UnimodParser;
import de.mpc.pia.tools.unimod.jaxb.ModT;
import uk.ac.ebi.jmzidml.model.mzidml.AbstractParam;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisProtocolCollection;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.Enzyme;
import uk.ac.ebi.jmzidml.model.mzidml.InputSpectra;
import uk.ac.ebi.jmzidml.model.mzidml.ModificationParams;
import uk.ac.ebi.jmzidml.model.mzidml.Param;
import uk.ac.ebi.jmzidml.model.mzidml.ParamList;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabase;
import uk.ac.ebi.jmzidml.model.mzidml.SearchModification;
import uk.ac.ebi.jmzidml.model.mzidml.SpecificityRules;
import uk.ac.ebi.jmzidml.model.mzidml.SpectraData;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationProtocol;
import uk.ac.ebi.jmzidml.model.mzidml.UserParam;
import uk.ac.ebi.pride.jmztab.model.CVParam;
import uk.ac.ebi.pride.jmztab.model.Contact;
import uk.ac.ebi.pride.jmztab.model.FixedMod;
import uk.ac.ebi.pride.jmztab.model.Instrument;
import uk.ac.ebi.pride.jmztab.model.MZBoolean;
import uk.ac.ebi.pride.jmztab.model.MZTabColumnFactory;
import uk.ac.ebi.pride.jmztab.model.MZTabConstants;
import uk.ac.ebi.pride.jmztab.model.MZTabDescription;
import uk.ac.ebi.pride.jmztab.model.Metadata;
import uk.ac.ebi.pride.jmztab.model.Mod;
import uk.ac.ebi.pride.jmztab.model.MsRun;
import uk.ac.ebi.pride.jmztab.model.PSM;
import uk.ac.ebi.pride.jmztab.model.PSMColumn;
import uk.ac.ebi.pride.jmztab.model.Protein;
import uk.ac.ebi.pride.jmztab.model.ProteinColumn;
import uk.ac.ebi.pride.jmztab.model.Reliability;
import uk.ac.ebi.pride.jmztab.model.Sample;
import uk.ac.ebi.pride.jmztab.model.Section;
import uk.ac.ebi.pride.jmztab.model.SpectraRef;
import uk.ac.ebi.pride.jmztab.model.VariableMod;

/**
 * This exporter will enable to export the results to mzTab files.
 *
 * @author julianu
 */
public class MzTabExporter {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(MzTabExporter.class);


    /** the modeller, that should be exported */
    private PIAModeller piaModeller;

    /** the writer used to export the mzTab file */
    private BufferedWriter outWriter;

    /** used unimod parser */
    private UnimodParser unimodParser;

    /** the software param for PIA */
    private CVParam piaParam;

    /** mapping from the spectrumIdentification_ref to the msRuns */
    private Map<String, List<MsRun>> specIdRefToMsRuns;

    /** mapping from the score's short_name to the score's ID in the Metadata */
    private Map<String, Integer> psmScoreShortToId;

    /** the ID for the protein score */
    private Integer piaProteinScoreID;

    /** the mzTab {@link Metadata} for the exported file */
    private Metadata metadata;

    /** the fileID for the export */
    private Long exportFileID;


    /** caching the modifications by their accessions */
    private Map<String, ModT> accessionsToModifications;

    /** caching the modifications by their residues and masses */
    private Map<String, Map<Double, Set<ModT>>> resAndMassToModifications;

    /** mapping from the peptide sequence to the accessions and occurrences [pre, post, start, stop]*/
    private Map<String, Map<String, String[]>> peptideOccurrences;

    /** column parameter for FDR column */
    private CVParam fdrColumnParam;

    /** column parameter for FDR q-value column */
    private CVParam qvalueColumnParam;

    /** column parameter for nr peptides (of protein) column */
    private CVParam nrPeptidesColumnParam;

    /** column parameter for amino acid sequence column */
    private CVParam aminoAcidSequenceColumnParam;


    /**
     * Basic constructor to export the
     * @param modeller
     */
    public MzTabExporter(PIAModeller modeller) {
        this.piaModeller = modeller;
        this.unimodParser = null;

        accessionsToModifications = new HashMap<>();
        resAndMassToModifications = new HashMap<>();
        peptideOccurrences = new HashMap<>();
    }


    public boolean exportToMzTab(Long fileID, File exportFile,
            boolean proteinLevel, boolean peptideLevelStatistics,
            boolean filterExport) {
        return exportToMzTab(fileID, exportFile, proteinLevel, peptideLevelStatistics, filterExport, false);
    }


    public boolean exportToMzTab(Long fileID, File exportFile,
            boolean proteinLevel, boolean peptideLevelStatistics,
            boolean filterExport, boolean exportProteinSequences) {
        try {
            FileOutputStream fos;
            fos = new FileOutputStream(exportFile);
            return exportToMzTab(fileID, fos, proteinLevel, peptideLevelStatistics, filterExport, exportProteinSequences);
        } catch (IOException ex) {
            LOGGER.error("Error writing  mzTab to " + exportFile.getAbsolutePath(), ex);
            return false;
        }
    }


    public boolean exportToMzTab(Long fileID, String exportFileName,
            boolean proteinLevel, boolean peptideLevelStatistics,
            boolean filterExport) {
        File piaFile = new File(exportFileName);
        return exportToMzTab(fileID, piaFile, proteinLevel, peptideLevelStatistics, filterExport);
    }


    public boolean exportToMzTab(Long fileID, String exportFileName,
            boolean proteinLevel, boolean peptideLevelStatistics,
            boolean filterExport, boolean exportProteinSequences) {
        File piaFile = new File(exportFileName);
        return exportToMzTab(fileID, piaFile, proteinLevel, peptideLevelStatistics, filterExport, exportProteinSequences);
    }


    public boolean exportToMzTab(Long fileID, OutputStream exportStream,
            boolean proteinLevel, boolean peptideLevelStatistics, boolean filterExport) {
        return exportToMzTab(fileID, exportStream, proteinLevel, peptideLevelStatistics, filterExport, false);
    }


    public boolean exportToMzTab(Long fileID, OutputStream exportStream,
            boolean proteinLevel, boolean peptideLevelStatistics, boolean filterExport, boolean exportProteinSequences) {
        boolean exportOK;
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(exportStream));
            exportOK = exportToMzTab(fileID, writer, proteinLevel, peptideLevelStatistics, filterExport, exportProteinSequences);
            writer.close();
        } catch (IOException e) {
            LOGGER.error("Error while exporting to mzTab", e);
            exportOK = false;
        }
        return exportOK;
    }


    public boolean exportToMzTab(Long fileID, Writer exportWriter,
            boolean proteinLevel, boolean peptideLevelStatistics, boolean filterExport) {
        return exportToMzTab(fileID, exportWriter, proteinLevel, peptideLevelStatistics, filterExport, false);
    }


    /**
     * Exports the data of the modeller using the data of the fileID (only
     * relevant if not protein level) to the specified file. If protein level is
     * selected as well, also this will be exported (and accordingly the PSMs of
     * all merged files).
     *
     * @param proteinLevel export the protein level
     * @param peptideLevelStatistics export peptide level statistics in PSMs
     * @param filterExport whether the export should be filtered (on any level)
     * @return
     */
    public boolean exportToMzTab(Long fileID, Writer exportWriter,
            boolean proteinLevel, boolean peptideLevelStatistics, boolean filterExport,
            boolean exportProteinSequences) {
        boolean error = false;
        exportFileID = fileID;

        LOGGER.info("Start writing mzTab export"
                + "\n\tproteinLevel " + proteinLevel
                + "\n\tpepLevelStats " + peptideLevelStatistics
                + "\n\tfiltered " + filterExport
                + "\n\tproteinSequences " + exportProteinSequences
                );

        try (BufferedWriter writer = new BufferedWriter(exportWriter)) {
            outWriter = writer;

            unimodParser = new UnimodParser();

            piaParam = new CVParam(OntologyConstants.CV_PSI_MS_LABEL,
                    OntologyConstants.PIA.getPsiAccession(),
                    OntologyConstants.PIA.getPsiName(),
                    PIAConstants.version);

            // Setting version, mode, and type in MZTabDescription
            MZTabDescription tabDescription;

            if (proteinLevel) {
                // PIA cannot give a "Complete" protein level export, as the
                // protein scores of the used search engines is not known
                tabDescription = new MZTabDescription(
                        MZTabDescription.Mode.Summary,
                        MZTabDescription.Type.Identification);

                if (exportFileID != 0L) {
                    exportFileID = 0L;
                    LOGGER.warn("The exported file for protein level information is always 0, will be set automatically.");
                }

            } else {
                tabDescription = new MZTabDescription(
                        MZTabDescription.Mode.Complete,
                        MZTabDescription.Type.Identification);
            }

            specIdRefToMsRuns = new HashMap<>();
            psmScoreShortToId = new HashMap<>();
            metadata = createMetadataForMzTab(exportFileID, proteinLevel, filterExport,
                    tabDescription);

            // the PSMs, which will be in th export
            List<PSMReportItem> reportPSMs = new ArrayList<>();

            boolean exportReliabilitycolumn = false;

            List<ReportProtein> proteinList = null;
            Map<String, PSMReportItem> reportPSMsMap = null;

            if (proteinLevel) {
                // write the report proteins
                proteinList = piaModeller.getProteinModeller().getFilteredReportProteins(
                        filterExport ? piaModeller.getProteinModeller().getReportFilters() : null);

                if (proteinList == null) {
                    LOGGER.warn("No report protein list, probably inference was not run.");
                } else {
                    reportPSMsMap = new HashMap<>();

                    reportPSMs = new ArrayList<>(reportPSMsMap.values());
                    exportReliabilitycolumn = piaModeller.getPSMModeller().isCombinedFDRScoreCalculated();
                }
            } else {
                // get the report PSMs
                List<AbstractFilter> filters =
                        filterExport ? piaModeller.getPSMModeller().getFilters(exportFileID) : null;

                if (exportFileID > 0) {
                    reportPSMs.addAll(piaModeller.getPSMModeller().getFilteredReportPSMs(exportFileID, filters));
                    exportReliabilitycolumn = piaModeller.getPSMModeller().isFDRCalculated(exportFileID);
                } else {
                    reportPSMs.addAll(piaModeller.getPSMModeller().getFilteredReportPSMSets(filters));
                    exportReliabilitycolumn = piaModeller.getPSMModeller().isCombinedFDRScoreCalculated();
                }
            }

            checkPSMsForUnassignedModifications(reportPSMs);

            // write out the metadata header
            outWriter.append(metadata.toString());

            if (proteinList != null) {
                // write out the proteins
                outWriter.append(MZTabConstants.NEW_LINE);
                writeProteins(proteinList, reportPSMsMap, exportProteinSequences);
            }

            // write out the PSMs
            outWriter.append(MZTabConstants.NEW_LINE);
            writePSMs(reportPSMs, exportReliabilitycolumn, peptideLevelStatistics, filterExport);
        } catch (IOException ex) {
            LOGGER.error("Error exporting mzTab", ex);
            error = true;
        }

        outWriter = null;
        LOGGER.info("finished mzTab export " + (error ? "with" : "without") + " errors");
        return !error;
    }


    /**
     * This method creates the {@link Metadata} and fills it with the basic
     * information for an export.
     *
     * @param fileID the ID of the exported file (0 for overview)
     * @param proteinLevel whether the protein level should be exported
     * @param filterExport whether the export should be filtered (on any level)
     * @param tabDescription the prior generated {@link MZTabDescription}
     *
     * @return
     *
     * @throws MalformedURLException
     */
    private Metadata createMetadataForMzTab(Long fileID, boolean proteinLevel, boolean filterExport,
            MZTabDescription tabDescription)
                    throws MalformedURLException {
        Metadata mtd = new Metadata(tabDescription);
        mtd.setDescription("PIA export of " + piaModeller.getFileName());
        mtd.setTitle(mtd.getDescription());

        List<InputSpectra> inputSpectraList = new ArrayList<>();
        // all needed search modifications
        List<SearchModification> searchModifications = new ArrayList<>();
        // all needed analysis protocol collections (for the software in MTD)
        List<AnalysisProtocolCollection> analysisProtocols = new ArrayList<>();
        // maps from the spectrumIdentification ID to the spectraData ID
        Map<String, List<String>> spectrumIdentificationToSpectraData = new HashMap<>();

        if (fileID == 0) {
            addMetadataForOverview(mtd, inputSpectraList, searchModifications, analysisProtocols,
                    spectrumIdentificationToSpectraData);
        } else {
            addSpectraModsAndProtocolsForFileID(fileID, inputSpectraList, searchModifications, analysisProtocols,
                    spectrumIdentificationToSpectraData);
            addFilesScoresToMetadata(mtd, fileID);
        }

        // associate the spectraData (msRuns) to integer IDs
        Map<String, Integer> spectraDataID = new HashMap<>();
        // this inputSpectra is not yet in the list
        inputSpectraList.stream().filter(inputSpectra -> !spectraDataID.containsKey(inputSpectra.getSpectraDataRef())).forEach(inputSpectra -> {
            // this inputSpectra is not yet in the list
            Integer id = spectraDataID.size() + 1;
            spectraDataID.put(inputSpectra.getSpectraDataRef(), id);
        });

        // add msRuns and samples
        for (Map.Entry<String, Integer> spectraIt : spectraDataID.entrySet()) {
            SpectraData sd = piaModeller.getSpectraData().get(spectraIt.getKey());

            MsRun msRun = new MsRun(spectraIt.getValue());

            // there are sometimes errors in the URl encoding of the files...
            URL locationUrl;
            try {
                locationUrl = new URL(sd.getLocation());
            } catch (MalformedURLException ex) {
                locationUrl = new URL("file", "//", sd.getLocation());
            }
            msRun.setLocation(locationUrl);

            if ((sd.getFileFormat() != null) &&
                    (sd.getFileFormat().getCvParam() != null)) {
                msRun.setFormat(new CVParam(sd.getFileFormat().getCvParam().getCvRef(),
                        sd.getFileFormat().getCvParam().getAccession(),
                        sd.getFileFormat().getCvParam().getName(),
                        sd.getFileFormat().getCvParam().getValue()));
            }

            if ((sd.getSpectrumIDFormat() != null) &&
                    (sd.getSpectrumIDFormat().getCvParam() != null)) {
                msRun.setIdFormat(new CVParam(sd.getSpectrumIDFormat().getCvParam().getCvRef(),
                        sd.getSpectrumIDFormat().getCvParam().getAccession(),
                        sd.getSpectrumIDFormat().getCvParam().getName(),
                        sd.getSpectrumIDFormat().getCvParam().getValue()));
            }

            mtd.addMsRun(msRun);
        }

        // this mapping is needed to reference reported PSMs to the MsRuns
        specIdRefToMsRuns.clear();
        for (Map.Entry<String, List<String>> iter : spectrumIdentificationToSpectraData.entrySet()) {
            Set<MsRun> runSet = iter.getValue().stream().map(spectrumDataRef -> mtd.getMsRunMap().get(spectraDataID.get(spectrumDataRef))).collect(Collectors.toSet());

            specIdRefToMsRuns.put(iter.getKey(), new ArrayList<>(runSet));
        }

        // add modifications
        int nrVariableMods = 0;
        int nrFixedMods = 0;
        Set<String> fixedMods = new HashSet<>();
        Set<String> variableMods = new HashSet<>();
        for (SearchModification searchMod : searchModifications) {
            String modAccession = null;
            String modName = null;

            if ((searchMod.getCvParam() != null) &&
                    !searchMod.getCvParam().isEmpty()) {
                modAccession = searchMod.getCvParam().get(0).getAccession();
                modName = searchMod.getCvParam().get(0).getName();
            }

            if ((modAccession != null)
                    && (OntologyConstants.NO_FIXED_MODIFICATIONS_SEARCHED.getPsiAccession().equals(modAccession)
                            || OntologyConstants.NO_VARIABLE_MODIFICATIONS_SEARCHED.getPsiAccession().equals(modAccession))) {
                continue;
            }

            ModT uniMod = unimodParser.getModification(
                    modAccession,
                    modName,
                    (double)searchMod.getMassDelta(),
                    searchMod.getResidues());

            List<String> positions = new ArrayList<>();
            for (SpecificityRules rule : searchMod.getSpecificityRules()) {
                for (CvParam param : rule.getCvParam()) {
                    if (OntologyConstants.MODIFICATION_SPECIFICITY_PEP_N_TERM.getPsiAccession().equals(param.getAccession())) {
                        positions.add("Any N-term");
                    } else if (OntologyConstants.MODIFICATION_SPECIFICITY_PROTEIN_N_TERM.getPsiAccession().equals(param.getAccession())) {
                        positions.add("Protein N-term");
                    } else if (OntologyConstants.MODIFICATION_SPECIFICITY_PEP_C_TERM.getPsiAccession().equals(param.getAccession())) {
                        positions.add("Any C-term");
                    } else if (OntologyConstants.MODIFICATION_SPECIFICITY_PROTEIN_C_TERM.getPsiAccession().equals(param.getAccession())) {
                        positions.add("Protein C-term");
                    }
                }
            }
            if (positions.isEmpty()) {
                positions.add("Anywhere");
            }

            for (String site : searchMod.getResidues()) {
                CVParam cvParam;

                if (uniMod != null) {
                    cvParam = new CVParam(UnimodParser.getCv().getId(),
                            UnimodParser.getCv().getId() + ":" + uniMod.getRecordId(),
                            uniMod.getTitle(),
                            null);
                } else {
                    // build an "unknown modification"
                    cvParam = new CVParam(OntologyConstants.CV_PSI_MS_LABEL,
                            OntologyConstants.UNKNOWN_MODIFICATION.getPsiAccession(),
                            OntologyConstants.UNKNOWN_MODIFICATION.getPsiName(),
                            Float.toString(searchMod.getMassDelta()));
                }

                for (String position : positions) {
                    Mod mod;

                    if (searchMod.isFixedMod()) {
                        mod = new FixedMod(nrFixedMods+1);
                    } else {
                        mod = new VariableMod(nrVariableMods+1);
                    }

                    mod.setParam(cvParam);

                    if (!"Anywhere".equals(position)) {
                        if (position.contains("N-term")) {
                            if (".".equals(site)) {
                                site = "N-term";
                            } else {
                                site = "N-term " + site;
                            }
                        } else if (position.contains("C-term")) {
                            if (".".equals(site)) {
                                site = "C-term";
                            } else {
                                site = "C-term " + site;
                            }
                        }

                        mod.setPosition(position);
                    }
                    mod.setSite(site);

                    if (searchMod.isFixedMod()) {
                        if (!fixedMods.contains(position + site + cvParam.toString())) {
                            nrFixedMods++;
                            mtd.addFixedMod((FixedMod)mod);
                            fixedMods.add(position + site + cvParam.toString());
                        }
                    } else {
                        if (!variableMods.contains(position + site + cvParam.toString())) {
                            nrVariableMods++;
                            mtd.addVariableMod((VariableMod)mod);
                            variableMods.add(position + site + cvParam.toString());
                        }
                    }
                }
            }
        }

        if (nrFixedMods == 0) {
            FixedMod fixedMod = new FixedMod(1);
            fixedMod.setParam(new CVParam(OntologyConstants.CV_PSI_MS_LABEL,
                    OntologyConstants.NO_FIXED_MODIFICATIONS_SEARCHED.getPsiAccession(),
                    OntologyConstants.NO_FIXED_MODIFICATIONS_SEARCHED.getPsiName(),
                    null));
            mtd.addFixedMod(fixedMod);
        }
        if (nrVariableMods == 0) {
            VariableMod variableMod = new VariableMod(1);
            variableMod.setParam(new CVParam(OntologyConstants.CV_PSI_MS_LABEL,
                    OntologyConstants.NO_VARIABLE_MODIFICATIONS_SEARCHED.getPsiAccession(),
                    OntologyConstants.NO_VARIABLE_MODIFICATIONS_SEARCHED.getPsiName(),
                    null));
            mtd.addVariableMod(variableMod);
        }

        Map<String, AnalysisSoftware> softwareMap = new HashMap<>();
        Map<String, Integer> softwareToID = new HashMap<>();
        Set<String> enzymeNames = new HashSet<>();

        Integer softwareID;
        for (AnalysisProtocolCollection protocol : analysisProtocols) {
            SpectrumIdentificationProtocol specIdProtocol =
                    protocol.getSpectrumIdentificationProtocol().get(0);

            // adding/getting the software(s)
            if (softwareMap.containsKey(specIdProtocol.getAnalysisSoftwareRef())) {
                softwareID = softwareToID.get(specIdProtocol.getAnalysisSoftwareRef());
            } else {
                AnalysisSoftware software = piaModeller.getAnalysisSoftwares().get(
                        specIdProtocol.getAnalysisSoftwareRef());
                softwareID = softwareMap.size()+1;

                Param softwareName = software.getSoftwareName();
                if ((softwareName != null) && (softwareName.getCvParam() != null)) {
                    mtd.addSoftwareParam(softwareID,
                            new CVParam(OntologyConstants.CV_PSI_MS_LABEL,
                                    softwareName.getCvParam().getAccession(),
                                    softwareName.getCvParam().getName(),
                                    software.getVersion()));

                } else  {
                    mtd.addSoftwareParam(softwareID,
                            new CVParam(null,
                                    null,
                                    software.getName(),
                                    software.getVersion()));
                }

                softwareMap.put(specIdProtocol.getAnalysisSoftwareRef(), software);
                softwareToID.put(specIdProtocol.getAnalysisSoftwareRef(), softwareID);
            }


            // add tolerances
            if (specIdProtocol.getFragmentTolerance() != null) {
                for (CvParam param : specIdProtocol.getFragmentTolerance().getCvParam()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("fragment ");
                    sb.append(param.getName());
                    sb.append(" = ");
                    sb.append(param.getValue());
                    if (param.getUnitName() != null) {
                        sb.append(" ");
                        sb.append(param.getUnitName());
                    }

                    mtd.addSoftwareSetting(softwareID, sb.toString());
                }
            }
            if (specIdProtocol.getParentTolerance() != null) {
                for (CvParam param : specIdProtocol.getParentTolerance().getCvParam()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("parent ");
                    sb.append(param.getName());
                    sb.append(" = ");
                    sb.append(param.getValue());
                    if (param.getUnitName() != null) {
                        sb.append(" ");
                        sb.append(param.getUnitName());
                    }

                    mtd.addSoftwareSetting(softwareID, sb.toString());
                }
            }

            // add additional search params
            ParamList additionalSearchParams = preprocessAdditionalParams(
                    specIdProtocol.getAdditionalSearchParams(), mtd);

            if (additionalSearchParams != null) {
                for (CvParam param : additionalSearchParams.getCvParam()) {
                    if (param.getValue() != null) {
                        mtd.addSoftwareSetting(softwareID, param.getName() + " = " + param.getValue());
                    } else {
                        mtd.addSoftwareSetting(softwareID, param.getName());
                    }
                }
                for (UserParam param : additionalSearchParams.getUserParam()) {
                    if (param.getValue() != null) {
                        mtd.addSoftwareSetting(softwareID, param.getName() + " = " + param.getValue());
                    } else {
                        mtd.addSoftwareSetting(softwareID, param.getName());
                    }
                }
            }

            // add the enzyme/s (if not already added)
            if (specIdProtocol.getEnzymes() != null) {
                for (Enzyme enzyme : specIdProtocol.getEnzymes().getEnzyme()) {
                    List<AbstractParam> enzymeParams = new ArrayList<>();
                    if (enzyme.getEnzymeName() != null) {
                        enzymeParams.addAll(enzyme.getEnzymeName().getCvParam());
                        enzymeParams.addAll(enzyme.getEnzymeName().getUserParam());
                    } else if (enzyme.getSiteRegexp() != null) {
                        CleavageAgent agent = CleavageAgent.getBySiteRegexp(enzyme.getSiteRegexp());

                        if (agent != null) {
                            enzymeParams.add(MzIdentMLTools.createCvParam(
                                    agent.getAccession(),
                                    MzIdentMLTools.getCvPSIMS(),
                                    agent.getName(),
                                    null));
                        }
                    }

                    for (AbstractParam param : enzymeParams) {
                        if (!enzymeNames.contains(param.getName())) {

                            uk.ac.ebi.pride.jmztab.model.Param enzParam;

                            if (param instanceof CvParam) {
                                enzParam = new CVParam(
                                        ((CvParam) param).getCvRef(),
                                        ((CvParam) param).getAccession(),
                                        param.getName(),
                                        param.getValue());
                            } else {
                                enzParam = new uk.ac.ebi.pride.jmztab.model.UserParam(
                                        param.getName(),
                                        param.getValue());
                            }

                            mtd.addSampleProcessingParam(mtd.getSampleProcessingMap().size()+1,
                                    enzParam);
                            enzymeNames.add(param.getName());
                        }
                    }
                }
            }
        }

        // add PIA to the list of used softwares, with according settings
        int piaSoftwareNr = mtd.getSoftwareMap().size() + 1;
        mtd.addSoftwareParam(piaSoftwareNr, piaParam);

        if (proteinLevel) {
            mtd.addSoftwareSetting(piaSoftwareNr, "modifications are "
                    + (piaModeller.getConsiderModifications() ? "" : "NOT ")
                    + "taken into account for peptide distinction");

            // add the PIA protein score
            piaProteinScoreID = 1;
            mtd.addProteinSearchEngineScoreParam(piaProteinScoreID, new CVParam(OntologyConstants.CV_PSI_MS_LABEL,
                    OntologyConstants.PIA_PROTEIN_SCORE.getPsiAccession(),
                    OntologyConstants.PIA_PROTEIN_SCORE.getPsiName(),
                    null));
        } else {
            if (fileID == 0) {
                mtd.addSoftwareSetting(piaSoftwareNr, "mzTab export of PSMs for overview");
            } else {
                mtd.addSoftwareSetting(piaSoftwareNr, "mzTab export of PSMs for file " + fileID);
            }
        }

        // PSM (combined) FDRScore
        if ((proteinLevel || fileID == 0) &&
                piaModeller.getPSMModeller().isCombinedFDRScoreCalculated()) {
            mtd.addSoftwareSetting(piaSoftwareNr,
                    OntologyConstants.PSM_LEVEL_COMBINED_FDRSCORE.getPsiName()
                    + " was calculated");

            piaModeller.getPSMModeller().getFileFDRData().entrySet().stream().filter(fdrIt -> fdrIt.getKey() > 0).forEach(fdrIt -> mtd.addSoftwareSetting(piaSoftwareNr,
                    "base score for FDR calculation for file "
                            + fdrIt.getKey() + " = " + fdrIt.getValue().getScoreShortName()));
        } else if (!proteinLevel && (fileID > 0) && piaModeller.getPSMModeller().isFDRCalculated(fileID)) {
            mtd.addSoftwareSetting(piaSoftwareNr,
                    OntologyConstants.PSM_LEVEL_FDRSCORE.getPsiName() +
                    " was calculated for file " + fileID);

            mtd.addSoftwareSetting(piaSoftwareNr,
                    "base score for FDR calculation for file "
                    + fileID + " = "
                    + piaModeller.getPSMModeller().getFileFDRData().get(fileID).getScoreShortName());
        }

        if (filterExport) {
            List<AbstractFilter> filters;
            if (proteinLevel) {
                filters = piaModeller.getProteinModeller().getReportFilters();
            } else {
                filters = piaModeller.getPSMModeller().getFilters(fileID);
            }

            for (AbstractFilter filter : filters) {
                mtd.addSoftwareSetting(piaSoftwareNr, "applied filter " +
                        filter.toString());
            }
        }

        return mtd;
    }



    /**
     * Add the specific metaData for an export of the overview.
     *
     * @param mtd
     * @param inputSpectraList
     * @param searchModifications
     * @param analysisProtocols
     * @param spectrumIdentificationToSpectraData
     */
    private void addMetadataForOverview(Metadata mtd,
            List<InputSpectra> inputSpectraList, List<SearchModification> searchModifications,
            List<AnalysisProtocolCollection> analysisProtocols,
            Map<String, List<String>> spectrumIdentificationToSpectraData) {

        for (PIAInputFile file : piaModeller.getPSMModeller().getFiles().values()) {
            if (file.getID() > 0) {
                addSpectraModsAndProtocolsForFileID(file.getID(),
                        inputSpectraList, searchModifications, analysisProtocols,
                        spectrumIdentificationToSpectraData);
            }

            addFilesScoresToMetadata(mtd, file.getID());
        }
    }


    /**
     * Add the inputSpectra (with maps), modifications and analysisProtocols for the given file
     *
     * @param fileID
     * @param inputSpectraList
     * @param searchModifications
     * @param analysisProtocols
     * @param spectrumIdentificationToSpectraData
     */
    private void addSpectraModsAndProtocolsForFileID(Long fileID,
            List<InputSpectra> inputSpectraList, List<SearchModification> searchModifications,
            List<AnalysisProtocolCollection> analysisProtocols,
            Map<String, List<String>> spectrumIdentificationToSpectraData) {
        SpectrumIdentification specID = piaModeller.getFiles().get(fileID).
                getAnalysisCollection().getSpectrumIdentification().get(0);
        List<InputSpectra> inputSpectras = specID.getInputSpectra();

        if ((inputSpectras != null) && !inputSpectras.isEmpty()) {
            List<String> spectraDataRefs = inputSpectras.stream().map(InputSpectra::getSpectraDataRef).collect(Collectors.toList());
            spectrumIdentificationToSpectraData.put(specID.getId(), spectraDataRefs);

            inputSpectraList.addAll(inputSpectras);
        }

        ModificationParams modParams =
                piaModeller.getFiles().get(fileID).getAnalysisProtocolCollection().
                getSpectrumIdentificationProtocol().get(0).
                getModificationParams();
        if ((modParams != null) && (modParams.getSearchModification() != null)) {
            searchModifications.addAll(modParams.getSearchModification());
        }

        if (piaModeller.getFiles().get(fileID).getAnalysisProtocolCollection() != null) {
            analysisProtocols.add(piaModeller.getFiles().get(fileID).getAnalysisProtocolCollection());
        }


    }


    /**
     * Add the scores of the given file to the MetaData
     *
     * @param mtd
     * @param fileID
     */
    private void addFilesScoresToMetadata(Metadata mtd, Long fileID) {
        for (String scoreShort : piaModeller.getPSMModeller().getScoreShortNames(fileID)) {
            if (!psmScoreShortToId.containsKey(scoreShort)) {
                Integer scoreID = psmScoreShortToId.size() + 1;
                ScoreModelEnum scoreType =
                        ScoreModelEnum.getModelByDescription(scoreShort);

                uk.ac.ebi.pride.jmztab.model.Param scoreParam;

                if (scoreType.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
                    scoreParam = new uk.ac.ebi.pride.jmztab.model.UserParam(
                            piaModeller.getPSMModeller().getScoreName(scoreShort),
                            "");
                } else {
                    scoreParam = new CVParam(OntologyConstants.CV_PSI_MS_LABEL,
                            scoreType.getCvAccession(),
                            scoreType.getCvName(),
                            "");
                }

                mtd.addPsmSearchEngineScoreParam(scoreID, scoreParam);
                psmScoreShortToId.put(scoreShort, scoreID);
            }
        }
    }



    /**
     * Processed all additional search params and adds them to the software
     * settings, if the format for them is known.
     *
     * @param paramList
     * @param mtd
     * @return
     */
    private ParamList preprocessAdditionalParams(ParamList paramList, Metadata mtd) {
        if (paramList == null) {
            return new ParamList();
        }

        // all the unprocessed params
        ParamList unprocessedParams = new ParamList();

        Set<String> contactNames = mtd.getContactMap().values().stream().map(Contact::getName).collect(Collectors.toSet());
        Set<String> instrumentNames = mtd.getInstrumentMap().values().stream().map(instrument -> instrument.getName().getValue()).collect(Collectors.toSet());
        Set<String> sampleDescriptions = mtd.getSampleMap().values().stream().map(Sample::getDescription).collect(Collectors.toSet());

        ListIterator<CvParam> cvIterator = paramList.getCvParam().listIterator();
        while (cvIterator.hasNext()) {
            CvParam param = cvIterator.next();
            OntologyConstants ontologyEntry = OntologyConstants.getByAccession(param.getAccession());

            if (ontologyEntry != null) {
                switch (ontologyEntry) {
                case CONTACT_NAME:
                    // the first entry of a contact detail has to be the name
                    Contact contact = processContactData(param.getValue(),
                            cvIterator, contactNames.size()+1);
                    if (!contactNames.contains(contact.getName())) {
                        mtd.addContact(contact);
                        contactNames.add(contact.getName());
                    }
                    break;

                case INSTRUMENT_MODEL:
                    if (!instrumentNames.contains(param.getValue())) {
                        Instrument instrument = new Instrument(instrumentNames.size()+1);

                        instrument.setName(new uk.ac.ebi.pride.jmztab.model.UserParam(
                                "instrument model", param.getValue()));

                        mtd.addInstrument(instrument);
                        instrumentNames.add(param.getValue());
                    }
                    break;

                case SAMPLE_NAME:
                    // wenn "sample name" -> baue sample, mit enzyme und allen möglichen anderen beschreibungen (iTraq, alles was subsample im Namen hat...)
                    if (!sampleDescriptions.contains(param.getValue())) {
                        Sample sample = new Sample(sampleDescriptions.size()+1);
                        sample.setDescription(param.getValue());
                        mtd.addSample(sample);
                        sampleDescriptions.add(param.getValue());
                    }
                    break;

                default:
                    break;
                }
            }
        }

        /*
        // TODO: process userParams
        for (UserParam param : paramList.getUserParam()) {
        }
        */

        return unprocessedParams;
    }


    /**
     * Processes the subsequent cvParams in the additional search params, which
     * belong to a contact.
     *
     * @param name name of the contact, which is always the first of the contact
     * details in the list
     * @param cvIterator Iterator over the cvParams
     * @param id id of the created contact
     * @return
     */
    private Contact processContactData(String name, ListIterator<CvParam> cvIterator, int id) {
        // get the name again
        Contact contact = new Contact(id);
        contact.setName(name);

        while (cvIterator.hasNext()) {
            CvParam param = cvIterator.next();
            OntologyConstants ontologyEntry = OntologyConstants.getByAccession(param.getAccession());

            if (ontologyEntry != null) {
                switch (ontologyEntry) {
                case CONTACT_AFFILIATION:
                    contact.setAffiliation(param.getValue());
                    break;
                case CONTACT_EMAIL:
                    contact.setEmail(param.getValue());
                    break;

                case CONTACT_ATTRIBUTE:
                case CONTACT_ADDRESS:
                case CONTACT_URL:
                    break;

                default:
                    // end on any other element
                    cvIterator.previous();
                    return contact;
                }
            }
        }

        return contact;
    }


    /**
     * Writes out a PSM section for the list of PSMs (which can be either PSM
     * sets or single PSMs).
     *
     * @param report a List of {@link PSMReportItem}s containing the PSMs to be
     * reported
     * @param reliabilityCol whether the reliability column should be written
     * @param report peptide level statistics
     *
     * @throws IOException
     */
    private void writePSMs(List<PSMReportItem> report, boolean reliabilityCol,
            boolean peptideLevelStatistics, boolean filterExport)
            throws IOException {
        // initialize the columns
        MZTabColumnFactory columnFactory =
                MZTabColumnFactory.getInstance(Section.PSM_Header);

        columnFactory.addDefaultStableColumns();

        // add the score columns
        for (Integer scoreID : psmScoreShortToId.values()) {
            columnFactory.addSearchEngineScoreOptionalColumn(PSMColumn.SEARCH_ENGINE_SCORE, scoreID, null);
        }

        // add custom column for missed cleavages
        columnFactory.addOptionalColumn(PIAConstants.MZTAB_MISSED_CLEAVAGES_COLUMN_NAME, Integer.class);

        // add optional column for decoys
        CVParam decoyColumnParam =
                new CVParam(OntologyConstants.CV_PSI_MS_LABEL,
                        OntologyConstants.DECOY_PEPTIDE.getPsiAccession(),
                        OntologyConstants.DECOY_PEPTIDE.getPsiName(),
                        null);
        columnFactory.addOptionalColumn(decoyColumnParam, String.class);

        // maps from the peptide's stringID to the peptide
        Map<String, ReportPeptide> reportPeptides = null;

        String peptideIdColumnName = null;
        CVParam peptideQValueColumn = null;
        CVParam peptideFDRScoreColumn = null;
        if (peptideLevelStatistics) {
            // TODO add other params, if they are calculated
            peptideIdColumnName = "peptide_id";
            columnFactory.addOptionalColumn(peptideIdColumnName, String.class);

            peptideQValueColumn = createPeptideQValueColumnIfAppropriate(columnFactory);

            peptideFDRScoreColumn = createPeptideFDRSCoreColumnIfAppropriate(columnFactory);

            List<AbstractFilter> peptideFilters = null;
            if (filterExport) {
                peptideFilters = piaModeller.getPeptideModeller().getFilters(exportFileID);
            }
            List<ReportPeptide> repPeplist = piaModeller.getPeptideModeller().getFilteredReportPeptides(
                    exportFileID, peptideFilters);

            reportPeptides = new HashMap<>(repPeplist.size());
            for (ReportPeptide repPep : repPeplist) {
                reportPeptides.put(repPep.getStringID(), repPep);
            }
        }

        // if it is set, write the reliability column
        if (reliabilityCol) {
            // there seems to be a bug in the setting of the correct column for the reliability, therefore put it to the end
            columnFactory.addReliabilityOptionalColumn("9999");
        }

        outWriter.append(columnFactory.toString());
        outWriter.append(MZTabConstants.NEW_LINE);

        // cache the databaseRefs to an array with name and version
        Map<String, String[]> dbRefToDbNameAndVersion =
                new HashMap<>();

        // cache the softwareRefs to the Params
        Map<String, uk.ac.ebi.pride.jmztab.model.Param> softwareParams =
                new HashMap<>();

        // the ID of the currently processed PSM
        int mzTabPSMid = 0;

        // now write the PSMs
        int nrPSMsExport = report.size();
        int count = 0;
        for (PSMReportItem psmItem : report) {
            PSM mztabPsm = new PSM(columnFactory, metadata);

            mztabPsm.setSequence(psmItem.getSequence());

            List<ReportPSM> reportPSMs = new ArrayList<>();

            if (psmItem instanceof ReportPSM) {
                reportPSMs.add((ReportPSM) psmItem);

                mzTabPSMid = ((ReportPSM) psmItem).getId().intValue();
            } else if (psmItem instanceof ReportPSMSet) {
                reportPSMs.addAll(((ReportPSMSet) psmItem).getPSMs());

                // in PSM sets, the ID does NOT represent the ID from the PIA
                // file but is an incremental value
                mzTabPSMid++;
            }

            mztabPsm.setPSM_ID(mzTabPSMid);

            // collect the SpectrumIdRefs and softwareRefs from the ReportPSMs
            Set<String> softwareRefs = new HashSet<>();
            for (ReportPSM reportPSM : reportPSMs) {

                addSpecRefForPSM(mztabPsm, reportPSM.getSourceID(),
                        reportPSM.getSpectrum().getSpectrumIdentification().getId());

                softwareRefs.add(reportPSM.getFile().getAnalysisProtocolCollection().
                        getSpectrumIdentificationProtocol().get(0).getAnalysisSoftwareRef());
            }

            if (psmItem.getAccessions().size() > 1) {
                mztabPsm.setUnique(MZBoolean.False);
            } else {
                mztabPsm.setUnique(MZBoolean.True);
            }

            for (Map.Entry<Integer, Modification> modIt : psmItem.getModifications().entrySet()) {
                uk.ac.ebi.pride.jmztab.model.Modification mod;
                mod = getUnimodModification(modIt.getValue());

                mod.addPosition(modIt.getKey(), null);
                mztabPsm.addModification(mod);
            }

            if (psmItem.getRetentionTime() != null) {
                mztabPsm.setRetentionTime(
                        psmItem.getRetentionTime().toString());
            }

            mztabPsm.setCharge(psmItem.getCharge());
            mztabPsm.setExpMassToCharge(psmItem.getMassToCharge());
            mztabPsm.setCalcMassToCharge(
                    psmItem.getMassToCharge() - psmItem.getDeltaMass());

            // add the scores
            boolean calculatedPIAScore = false;
            Reliability reliability = null;
            for (Map.Entry<String, Integer> scoreIt : psmScoreShortToId.entrySet()) {
                Double scoreValue;

                if (psmItem instanceof ReportPSM) {
                    scoreValue = psmItem.getScore(scoreIt.getKey());
                } else {
                    // psmItem is a ReportPSMSet
                    scoreValue = ((ReportPSMSet) psmItem).getBestScore(scoreIt.getKey());
                    if (scoreValue.equals(Double.NaN)) {
                        scoreValue = psmItem.getScore(scoreIt.getKey());
                    }
                }
                if (scoreValue.equals(Double.NaN)) {
                    scoreValue = null;
                }
                mztabPsm.setSearchEngineScore(scoreIt.getValue(), scoreValue);

                ScoreModelEnum model = ScoreModelEnum.getModelByDescription(scoreIt.getKey());
                if (model.equals(ScoreModelEnum.PSM_LEVEL_FDR_SCORE)
                        || model.equals(ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE)) {
                    calculatedPIAScore = true;

                    if (reliabilityCol) {
                        if ((scoreValue != null) && (scoreValue <= 0.01)) {
                            reliability = Reliability.High;
                        } else if ((scoreValue != null) && (scoreValue <= 0.05)) {
                            reliability = Reliability.Medium;
                        } else {
                            reliability = Reliability.Poor;
                        }
                    }
                }
            }

            // add PIA, if a score was calculated by it
            if (calculatedPIAScore) {
                mztabPsm.addSearchEngineParam(piaParam);
            }

            // add the search engines (i.e. analysisSoftwares)
            for (String softwareRef : softwareRefs) {
                uk.ac.ebi.pride.jmztab.model.Param softwareParam =
                        softwareParams.get(softwareRef);

                if (softwareParam == null) {
                    AnalysisSoftware software = piaModeller.getAnalysisSoftwares().get(softwareRef);

                    Param softwareName = software.getSoftwareName();
                    if (softwareName != null) {
                        if (softwareName.getCvParam() != null) {
                            CvParam param = softwareName.getCvParam();

                            softwareParam = new CVParam(param.getCvRef(),
                                    param.getAccession(), param.getName(),
                                    software.getVersion());
                        } else if (softwareName.getUserParam() != null) {
                            UserParam param = softwareName.getUserParam();

                            softwareParam =
                                    new uk.ac.ebi.pride.jmztab.model.UserParam(
                                            param.getName(), software.getVersion());
                        }
                    }

                    softwareParams.put(softwareRef, softwareParam);
                }

                mztabPsm.addSearchEngineParam(softwareParam);
            }

            // if the (combined) FDRScore is calculated, give the reliability
            // 1: high reliability     (combined) FDRScore <= 0.01
            // 2: medium reliability   (combined) FDRScore <= 0.05
            // 3: poor reliability     (combined) FDRScore >  0.05
            if (reliability != null) {
                mztabPsm.setReliability(reliability);
            }


            mztabPsm.setOptionColumnValue(
                    PIAConstants.MZTAB_MISSED_CLEAVAGES_COLUMN_NAME,
                    psmItem.getMissedCleavages());

            mztabPsm.setOptionColumnValue(decoyColumnParam,
                    psmItem.getIsDecoy() ? "1" : "0");

            // one line and some special info per accession
            for (Accession accession : psmItem.getAccessions()) {
                mztabPsm.setAccession(accession.getAccession());

                // set the first available dbName and dbVersion
                for (String dbRef : accession.getSearchDatabaseRefs()) {
                    String[] nameAndVersion =
                            dbRefToDbNameAndVersion.get(dbRef);
                    // cache the name and version of databases
                    if (nameAndVersion == null) {
                        SearchDatabase sDB = piaModeller.getSearchDatabases().get(dbRef);

                        if (sDB.getDatabaseName() != null) {
                            nameAndVersion = new String[2];
                            if (sDB.getDatabaseName().getCvParam() != null) {
                                nameAndVersion[0] =
                                        sDB.getDatabaseName().getCvParam().getName();
                            } else if (sDB.getDatabaseName().getUserParam() != null) {
                                nameAndVersion[0] =
                                        sDB.getDatabaseName().getUserParam().getName();
                            }
                            nameAndVersion[1] = sDB.getVersion();

                        } else if (sDB.getName() != null) {
                            nameAndVersion = new String[2];
                            nameAndVersion[0] = sDB.getName();
                            nameAndVersion[1] = sDB.getVersion();
                        } else {
                            nameAndVersion = new String[1];
                            nameAndVersion[0] = null;
                        }

                        dbRefToDbNameAndVersion.put(dbRef, nameAndVersion);
                    }

                    if (nameAndVersion[0] != null) {
                        mztabPsm.setDatabase(nameAndVersion[0]);
                        mztabPsm.setDatabaseVersion(nameAndVersion[1]);
                    }
                }

                String[] occData = getPeptideOccurrences(psmItem.getPeptide(), accession.getAccession());
                if (occData != null) {
                    mztabPsm.setPre(occData[0]);
                    mztabPsm.setPost(occData[1]);
                    mztabPsm.setStart(occData[2]);
                    mztabPsm.setEnd(occData[3]);
                }

                if (peptideLevelStatistics) {
                    addPeptideLevelColumns(mztabPsm, psmItem, reportPeptides,
                            peptideIdColumnName, peptideQValueColumn, peptideFDRScoreColumn);
                }

                outWriter.append(mztabPsm.toString());
                outWriter.append(MZTabConstants.NEW_LINE);
            }

            count++;
            if (count % 10000 == 0) {
                LOGGER.debug("exported " + count + " / " + nrPSMsExport + " PSMs "
                        + "(" + (100.0 * count / nrPSMsExport) + "%)");
            }
        }
    }


    /**
     * Creates a column for "peptide level q-value" in the given
     * {@link MZTabColumnFactory}, if for the exported file the FDR was
     * calculated.
     *
     * @param columnFactory
     * @return the {@link CVParam} defining the added column or null, if it was
     * not created
     */
    private CVParam createPeptideQValueColumnIfAppropriate(MZTabColumnFactory columnFactory) {
        CVParam peptideQValueColumn = null;

        if (piaModeller.getPeptideModeller().isFDRCalculated(exportFileID)) {
            peptideQValueColumn = new CVParam(OntologyConstants.CV_PSI_MS_LABEL,
                    OntologyConstants.PEPTIDE_LEVEL_QVALUE.getPsiAccession(),
                    OntologyConstants.PEPTIDE_LEVEL_QVALUE.getPsiName(),
                    null);

            columnFactory.addOptionalColumn(peptideQValueColumn, Double.class);
        }

        return peptideQValueColumn;
    }


    /**
     * Creates a column for "peptide level FDR Score" in the given
     * {@link MZTabColumnFactory}, if for the exported file the FDR was
     * calculated.
     *
     * @param columnFactory
     * @return the {@link CVParam} defining the added column or null, if it was
     * not created
     */
    private CVParam createPeptideFDRSCoreColumnIfAppropriate(MZTabColumnFactory columnFactory) {
        CVParam peptideFDRScoreColumn = null;

        if (piaModeller.getPeptideModeller().isFDRCalculated(exportFileID)) {
            peptideFDRScoreColumn = new CVParam(OntologyConstants.CV_PSI_MS_LABEL,
                    OntologyConstants.PEPTIDE_LEVEL_FDRSCORE.getPsiAccession(),
                    OntologyConstants.PEPTIDE_LEVEL_FDRSCORE.getPsiName(),
                    null);

            columnFactory.addOptionalColumn(peptideFDRScoreColumn, Double.class);
        }

        return peptideFDRScoreColumn;
    }



    /**
     * Adds the peptide level columns to the mzTab's {@link PSM} row. Only adds
     * the values of the columns, which are actually calculated.
     *
     * @param mzTabPsm
     * @param psmItem
     * @param reportPeptides
     * @param peptideIdColumnName
     * @param peptideQValueColumn
     * @param peptideFDRScoreColumn
     */
    private void addPeptideLevelColumns(PSM mzTabPsm, PSMReportItem psmItem,
            Map<String, ReportPeptide> reportPeptides, String peptideIdColumnName,
            CVParam peptideQValueColumn, CVParam peptideFDRScoreColumn) {
        if ((peptideIdColumnName != null)
                || (peptideQValueColumn != null)
                || (peptideFDRScoreColumn != null)) {
            String peptideId = psmItem.getPeptideStringID(piaModeller.getConsiderModifications());
            ReportPeptide peptide = reportPeptides.get(peptideId);

            if ((peptide == null) || !peptide.getPSMs().contains(psmItem)) {
                // a PSM might not be connected to any peptide, if it was e.g. filtered out
                return;
            }

            if (peptideIdColumnName != null) {
                mzTabPsm.setOptionColumnValue(peptideIdColumnName, peptideId);
            }
            if (peptideQValueColumn != null) {
                mzTabPsm.setOptionColumnValue(peptideQValueColumn, peptide.getQValue());
            }
            if (peptideFDRScoreColumn != null) {
                mzTabPsm.setOptionColumnValue(peptideFDRScoreColumn, peptide.getFDRScore().getValue());
            }
        }
    }


    /**
     * Writes the protein section into an mzTab file
     *
     * @param report
     * @param reportPSMs
     * @throws IOException
     */
    private void writeProteins(List<ReportProtein> report,
            Map<String, PSMReportItem> reportPSMs, boolean exportSequences) throws IOException {
        // initialize the columns
        MZTabColumnFactory columnFactory = MZTabColumnFactory.getInstance(Section.Protein_Header);
        columnFactory.addDefaultStableColumns();
        columnFactory.addBestSearchEngineScoreOptionalColumn(ProteinColumn.BEST_SEARCH_ENGINE_SCORE, piaProteinScoreID);

        Map<String, Boolean> psmSetSettings = piaModeller.getPSMModeller().getPSMSetSettings();

        aminoAcidSequenceColumnParam = null;
        fdrColumnParam = null;
        qvalueColumnParam = null;

        // cache the msRuns
        Map<Integer, MsRun> msRunMap = new HashMap<>();
        for (List<MsRun> msRunList : specIdRefToMsRuns.values()) {
            for (MsRun msRun : msRunList) {
                msRunMap.put(msRun.getId(), msRun);
            }
        }

        // add msRun specific information, where possible
        for (MsRun msRun : msRunMap.values()) {
            columnFactory.addOptionalColumn(ProteinColumn.NUM_PSMS, msRun);
            columnFactory.addOptionalColumn(ProteinColumn.NUM_PEPTIDES_DISTINCT, msRun);
        }

        // add FDR value, if calculated

        if (piaModeller.getProteinModeller().getFDRData().getNrItems() != null) {
            columnFactory.addReliabilityOptionalColumn();

            fdrColumnParam = new CVParam(OntologyConstants.CV_PSI_MS_LABEL,
                    OntologyConstants.PROTEIN_LEVEL_LOCAL_FDR.getPsiAccession(),
                    OntologyConstants.PROTEIN_LEVEL_LOCAL_FDR.getPsiName(),
                    null);
            columnFactory.addOptionalColumn(fdrColumnParam, Double.class);

            qvalueColumnParam = new CVParam(OntologyConstants.CV_PSI_MS_LABEL,
                    OntologyConstants.PROTEIN_GROUP_LEVEL_Q_VALUE.getPsiAccession(),
                    OntologyConstants.PROTEIN_GROUP_LEVEL_Q_VALUE.getPsiName(),
                    null);
            columnFactory.addOptionalColumn(qvalueColumnParam, Double.class);
        }

        // add custom column for nr_peptides
        nrPeptidesColumnParam = new CVParam("MS", "MS:1001097", "distinct peptide sequences", null);
        columnFactory.addOptionalColumn(nrPeptidesColumnParam, Integer.class);

        // add custom column for nr_psms
        columnFactory.addOptionalColumn(PIAConstants.MZTAB_NR_PSMS_COLUMN_NAME, Integer.class);

        // add custom column for nr_spectra
        columnFactory.addOptionalColumn(PIAConstants.MZTAB_NR_SPECTRA_COLUMN_NAME, Integer.class);

        if (exportSequences) {
            aminoAcidSequenceColumnParam = new CVParam(OntologyConstants.CV_PSI_MS_LABEL,
                    OntologyConstants.AMINOACID_SEQUENCE.getPsiAccession(),
                    OntologyConstants.AMINOACID_SEQUENCE.getPsiName(),
                    null);
            columnFactory.addOptionalColumn(aminoAcidSequenceColumnParam, String.class);
        }

        outWriter.append(columnFactory.toString());
        outWriter.append(MZTabConstants.NEW_LINE);

        // cache the databaseRefs to an array with name and version
        Map<String, String[]> dbRefToDbNameAndVersion = new HashMap<>();

        for (ReportProtein reportProtein : report) {
            Protein mzTabProtein = createMzTabProtein(reportProtein, columnFactory, dbRefToDbNameAndVersion,
                    reportPSMs, psmSetSettings, msRunMap);

            outWriter.append(mzTabProtein.toString());
            outWriter.append(MZTabConstants.NEW_LINE);
        }
    }


    /**
     * Create a mzTab protein for the report
     *
     * @param reportProtein
     * @param proteinColumnFactory
     * @param dbRefToDbNameAndVersion
     * @param reportPSMs
     * @param psmSetSettings
     * @param msRunMap
     * @return
     */
    private Protein createMzTabProtein(ReportProtein reportProtein, MZTabColumnFactory proteinColumnFactory,
            Map<String, String[]> dbRefToDbNameAndVersion, Map<String, PSMReportItem> reportPSMs,
            Map<String, Boolean> psmSetSettings, Map<Integer, MsRun> msRunMap) {
        Protein mzTabProtein = new Protein(proteinColumnFactory);

        // TODO: better choice of representative
        Accession representative = reportProtein.getRepresentative();
        mzTabProtein.setAccession(representative.getAccession());

        // just take one description
        for (String desc : representative.getDescriptions().values()) {
            if (desc.trim().length() > 0) {
                mzTabProtein.setDescription(desc);
                break;
            }
        }

        // set the first available dbName and dbVersion of the representative
        for (String dbRef : representative.getSearchDatabaseRefs()) {
            String[] nameAndVersion = dbRefToDbNameAndVersion.get(dbRef);
            // cache the name and version of databases
            if (nameAndVersion == null) {
                SearchDatabase sDB = piaModeller.getSearchDatabases().get(dbRef);

                if (sDB.getDatabaseName() != null) {
                    nameAndVersion = new String[2];
                    if (sDB.getDatabaseName().getCvParam() != null) {
                        nameAndVersion[0] =
                                sDB.getDatabaseName().getCvParam().getName();
                    } else if (sDB.getDatabaseName().getUserParam() != null) {
                        nameAndVersion[0] =
                                sDB.getDatabaseName().getUserParam().getName();
                    }
                    nameAndVersion[1] = sDB.getVersion();

                } else if (sDB.getName() != null) {
                    nameAndVersion = new String[2];
                    nameAndVersion[0] = sDB.getName();
                    nameAndVersion[1] = sDB.getVersion();
                } else {
                    nameAndVersion = new String[1];
                    nameAndVersion[0] = null;
                }

                dbRefToDbNameAndVersion.put(dbRef, nameAndVersion);
            }

            if (nameAndVersion[0] != null) {
                mzTabProtein.setDatabase(nameAndVersion[0]);
                mzTabProtein.setDatabaseVersion(nameAndVersion[1]);
            }
        }

        Set<String> usedSoftwareRefs = new HashSet<>();
        Map<Integer, Integer> msRunIdToNumPSMs = new HashMap<>();
        Map<Integer, Set<String>> msRunIdToDistinctPeptides = new HashMap<>();

        // go through each peptide and PSM of the protein and collect information
        for (ReportPeptide reportPeptide : reportProtein.getPeptides()) {
            for (PSMReportItem reportItem : reportPeptide.getPSMs()) {
                if (reportItem instanceof ReportPSMSet) {

                    for (ReportPSM reportPSM
                            : ((ReportPSMSet) reportItem).getPSMs()) {
                        // get the used search engine for this PSM
                        usedSoftwareRefs.add(reportPSM.getFile().
                                getAnalysisProtocolCollection().
                                getSpectrumIdentificationProtocol().get(0).
                                getAnalysisSoftwareRef());

                        String specIdRef = reportPSM.getSpectrum().
                                getSpectrumIdentification().getId();

                        if (specIdRefToMsRuns.get(specIdRef) != null) {
                            // increase the numPSMs of the the msRun(s)
                            for (MsRun msRun : specIdRefToMsRuns.get(specIdRef)) {
                                Integer id = msRun.getId();

                                if (msRunIdToNumPSMs.containsKey(id)) {
                                    msRunIdToNumPSMs.put(id, msRunIdToNumPSMs.get(id) + 1);
                                } else {
                                    msRunIdToNumPSMs.put(id, 0);
                                }

                                Set<String> disctinctPeptides =
                                        msRunIdToDistinctPeptides.computeIfAbsent(msRun.getId(), k -> new HashSet<>(
                                                reportProtein.getNrPeptides()));
                                disctinctPeptides.add(reportPSM.getSequence());
                            }
                        }
                    }

                    // add the PSM set to the PSM map
                    String psmKey = reportItem.getIdentificationKey(psmSetSettings);
                    if (!reportPSMs.containsKey(psmKey)) {
                        reportPSMs.put(psmKey, reportItem);
                    }
                } else {
                    LOGGER.error(
                            "item in ReportPeptide should NOT be ReportPSM");
                }
            }
        }

        // add the search engines, which is always PIA
        mzTabProtein.addSearchEngineParam(piaParam);

        // set the PIA protein score
        mzTabProtein.setBestSearchEngineScore(piaProteinScoreID, reportProtein.getScore());

        // the protein score for each search engine's identification is not collected
        //mzTabProtein.addSearchEngineScoreParam(msRun, param)

        // get the protein FDR, if calculated, and set the reliability
        if (fdrColumnParam != null) {
            if (reportProtein.getQValue() <= 0.01) {
                mzTabProtein.setReliability(Reliability.High);
            } else if (reportProtein.getQValue() <= 0.05) {
                mzTabProtein.setReliability(Reliability.Medium);
            } else {
                mzTabProtein.setReliability(Reliability.Poor);
            }

            mzTabProtein.setOptionColumnValue(fdrColumnParam, reportProtein.getFDR());
            mzTabProtein.setOptionColumnValue(qvalueColumnParam, reportProtein.getQValue());
        }

        for (Map.Entry<Integer, MsRun> msRunIt : msRunMap.entrySet()) {
            Integer msRunID = msRunIt.getValue().getId();
            // set the num_psms_ms_run
            mzTabProtein.setNumPSMs(msRunIt.getValue(),
                    msRunIdToNumPSMs.getOrDefault(msRunID, 0));

            if (msRunIdToDistinctPeptides.containsKey(msRunID)) {
                mzTabProtein.setNumPeptidesDistinct(msRunIt.getValue(),
                        msRunIdToDistinctPeptides.get(msRunID).size());
            } else {
                mzTabProtein.setNumPeptidesDistinct(msRunIt.getValue(), 0);
            }
        }

        reportProtein.getAccessions().stream()
                .filter(ambiguityMember -> !ambiguityMember.equals(representative))
                .forEach(ambiguityMember -> mzTabProtein.addAmbiguityMembers(ambiguityMember.getAccession()));

        // TODO: perhaps add modifications with mzTabProtein.addModification(modification);

        Double coverage = reportProtein.getCoverage(representative.getAccession());
        if (!coverage.equals(Double.NaN)) {
            mzTabProtein.setProteinConverage(coverage);
        }

        mzTabProtein.setOptionColumnValue(nrPeptidesColumnParam,
                reportProtein.getNrPeptides());

        mzTabProtein.setOptionColumnValue(
                PIAConstants.MZTAB_NR_PSMS_COLUMN_NAME,
                reportProtein.getNrPSMs());

        mzTabProtein.setOptionColumnValue(
                PIAConstants.MZTAB_NR_SPECTRA_COLUMN_NAME,
                reportProtein.getNrSpectra());

        if (aminoAcidSequenceColumnParam != null) {
            mzTabProtein.setOptionColumnValue(aminoAcidSequenceColumnParam, representative.getDbSequence());
        }

        return mzTabProtein;
    }


    /**
     * Get the Unimod modification for the Modification. If this is not yet in
     * the caches, create it. If there is no UniMod modification for the
     * residue, mass and possible name, create a simple ChemMod modification.
     *
     * @param modification
     * @return
     */
    private uk.ac.ebi.pride.jmztab.model.Modification getUnimodModification(Modification modification) {
        uk.ac.ebi.pride.jmztab.model.Modification mod;

        ModT uniMod = getCachedModification(modification);

        if (uniMod == null) {
            // modification was not yet cached
            uniMod = createNewCachedModification(modification);
        }

        if (uniMod != null) {
            mod = new uk.ac.ebi.pride.jmztab.model.Modification(
                    Section.PSM,
                    uk.ac.ebi.pride.jmztab.model.Modification.Type.UNIMOD,
                    uniMod.getRecordId().toString());
        } else {
            // not found in UNIMOD, create a CHEMMOD mass-shift
            mod = new uk.ac.ebi.pride.jmztab.model.Modification(
                    Section.PSM,
                    uk.ac.ebi.pride.jmztab.model.Modification.Type.CHEMMOD,
                    modification.getMass().toString());
        }

        return mod;
    }


    /**
     * Check in the caches for a ModT type modification of the given
     * Modification and return it, if it was found.
     *
     * @param modification
     * @return
     */
    private ModT getCachedModification(Modification modification) {
        ModT uniMod = null;

        // look in accessions cached modifications
        String acc = modification.getAccession();
        if (acc != null) {
            if (acc.startsWith("UNIMOD:")) {
                acc = acc.substring(7);
            }
            uniMod = accessionsToModifications.get(acc);
        }

        if (uniMod == null) {
            // look in the res-mass-cache
            uniMod = getModInResAndMassCache(modification);
        }

        return uniMod;
    }


    /**
     * Look for the modification in the modifications cached by residue and mono
     * delta mass.
     *
     * @param modification
     * @return
     */
    private ModT getModInResAndMassCache(Modification modification) {
        String residue = modification.getResidue().toString();

        Set<ModT> possibleMods = new HashSet<>();
        Map<Double, Set<ModT>> massToMods = resAndMassToModifications.get(residue);
        if (massToMods != null) {
            massToMods.entrySet().stream().filter(massModsIt -> Math.abs(massModsIt.getKey() - modification.getMass()) <= UnimodParser.UNIMOD_MASS_TOLERANCE).forEach(massModsIt -> possibleMods.addAll(massModsIt.getValue()));
        }

        ModT uniMod = null;
        if (!possibleMods.isEmpty()) {
            String name = modification.getDescription();
            if ((name != null) && !name.trim().isEmpty()) {
                for (ModT mod : possibleMods) {
                    if (UnimodParser.isAnyName(name, mod)) {
                        uniMod = mod;
                        break;
                    }
                }
            } else {
                // no name is given, return any of the possible modifications
                uniMod = possibleMods.iterator().next();
            }
        }

        return uniMod;
    }


    /**
     * Create a ModT type modification and put it into the caches.
     *
     * @param modification
     * @return
     */
    private ModT createNewCachedModification(Modification modification) {
        ModT uniMod  = unimodParser.getModification(
                modification.getAccession(),
                modification.getDescription(),
                modification.getMass(),
                modification.getResidue().toString());

        // it can still be null, if the modification was not found in unimod
        if (uniMod != null) {
            accessionsToModifications.put(uniMod.getRecordId().toString(), uniMod);

            String residue = modification.getResidue().toString();
            Map<Double, Set<ModT>> massToMods = resAndMassToModifications.computeIfAbsent(residue, k -> new HashMap<>());
            Set<ModT> massMods = massToMods.computeIfAbsent(uniMod.getDelta().getMonoMass(), k -> new HashSet<>());
            massMods.add(uniMod);
        }

        return uniMod;
    }


    /**
     * Add the spectrum ID references for the PSM, given the source ID and the
     * specIdRef
     *
     * @param mztabPsm
     * @param sourceID
     */
    private void addSpecRefForPSM(PSM mztabPsm, String sourceID, String specIdRef) {
        if (sourceID == null) {
            return;
        }

        List<MsRun> runList = specIdRefToMsRuns.get(specIdRef);
        if ((runList != null) && (runList.size() == 1)) {
            // TODO: what, if there is more than one msRun per file?
            SpectraRef specRef = new SpectraRef(runList.get(0), sourceID);
            mztabPsm.addSpectraRef(specRef);
        }
    }


    /**
     * Get the peptide occurrences for the given peptide and the accessions.
     * This method uses a map as cache and fills it accordingly.
     *
     * @param accession
     * @return
     */
    private String[] getPeptideOccurrences(Peptide peptide, String accession) {
        String sequence = peptide.getSequence();
        Map<String, String[]> occurrences = peptideOccurrences.get(sequence);

        if (occurrences == null) {
            occurrences = new HashMap<>();
            peptideOccurrences.put(sequence, occurrences);

            // add the occurrences' data to the cache
            for (AccessionOccurrence occ : peptide.getAccessionOccurrences()) {
                String[] occData = new String[4];

                String dbSequence = occ.getAccession().getDbSequence();
                if (dbSequence != null) {
                    if (occ.getStart() > 1) {
                        occData[0] = dbSequence.substring(occ.getStart()-2, occ.getStart()-1);
                    } else {
                        occData[0] = "-";
                    }

                    if (occ.getEnd() < dbSequence.length()) {
                        occData[1] = dbSequence.substring(occ.getEnd(), occ.getEnd()+1);
                    } else {
                        occData[1] = "-";
                    }
                }

                occData[2] = occ.getStart().toString();
                occData[3] = occ.getEnd().toString();

                occurrences.put(occ.getAccession().getAccession(), occData);
                // TODO: multiple occurrences in the the same protein?
            }
        }

        return occurrences.get(accession);
    }


    /**
     * This function checks the given PSMs for modifications, which are not yet recorded for the header (i.e. not
     * in the SpectrumIdetificationProtocol.
     *
     * @param reportPSMs
     */
    private void checkPSMsForUnassignedModifications(List<PSMReportItem> reportPSMs) {
        // TODO: implement
        // this must still be able to edit the modifications, therefore the metadata must not be ready yet...
    }
}
