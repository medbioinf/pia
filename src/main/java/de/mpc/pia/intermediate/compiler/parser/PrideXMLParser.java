package de.mpc.pia.intermediate.compiler.parser;

import de.mpc.pia.intermediate.*;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.tools.MzIdentMLTools;
import de.mpc.pia.tools.PRIDETools;
import org.apache.log4j.Logger;
import uk.ac.ebi.jmzidml.model.mzidml.*;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.Param;
import uk.ac.ebi.pride.jaxb.model.*;
import uk.ac.ebi.pride.jaxb.xml.PrideXmlReader;
import java.io.File;
import java.util.*;

/**
 * This class read the PRIDE Xml files and map the structure into a
 * @author Yasset Perez-Riverol (ypriverol@gmail.com)
 * @date 08/02/2016
 */
public class PrideXMLParser {

    /** logger for this class */
    private static final Logger logger = Logger.getLogger(PrideXMLParser.class);

    private static final String PSI_ION_SELECTION_CHARGE_STATE = "PSI:1000041";
    private static final String ION_SELECTION_CHARGE_STATE = "MS:1000041";
    private static final String PSI_ION_SELECTION_MZ = "PSI:1000040";
    private static final String ION_SELECTION_MZ = "MS:1000744";

    /**
     * We don't ever want to instantiate this class
     */
    private PrideXMLParser() {
        throw new AssertionError();
    }

    /**
     * Parse the PRIDE Xml into a PIA structure, The concept of PSM is not provided in PRIDE Files
     * this is the main reason why we will considered peptides as PSMs here.
     * @param name
     * @param fileName
     * @param compiler
     * @return
     */
    public static boolean getDataFromPrideXMLFile(String name, String fileName,
                                                  PIACompiler compiler) {
        // Open the input mzIdentML file for parsing
        File prideFile = new File(fileName);

        int accNr = 0;
        int pepNr = 0;
        int specNr = 0;

        if (!prideFile.canRead()) {
            // TODO: better error / exception
            logger.error("could not read '" + fileName + "'.");
            return false;
        }

        PIAInputFile file = compiler.insertNewFile(name, fileName, InputFileParserFactory.InputFileTypes.PRIDEXML_INPUT.getFileSuffix());

        PrideXmlReader xmlParser = new PrideXmlReader(prideFile);

        // create the analysis software and add it to the compiler
        AnalysisSoftware analysisSoftware = parseSoftware(xmlParser.getDataProcessing());

        //Add the software to the compiler
        analysisSoftware = compiler.putIntoSoftwareMap(analysisSoftware);

        // define the spectrumIdentificationProtocol
        SpectrumIdentificationProtocol spectrumIDProtocol = new SpectrumIdentificationProtocol();

        spectrumIDProtocol.setId("tandemAnalysis");
        spectrumIDProtocol.setAnalysisSoftware(analysisSoftware);

        Param param = new Param();
        AbstractParam abstractParam = new CvParam();
        ((CvParam)abstractParam).setAccession("MS:1001083");
        ((CvParam)abstractParam).setCv(MzIdentMLTools.getCvPSIMS());
        abstractParam.setName("ms-ms search");
        param.setParam(abstractParam);
        spectrumIDProtocol.setSearchType(param);

        file.addSpectrumIdentificationProtocol(spectrumIDProtocol);


        // add the spectrum identification
        SpectrumIdentification spectrumID = new SpectrumIdentification();
        spectrumID.setId("prideIdentification");
        spectrumID.setSpectrumIdentificationList(null);
        spectrumID.setSpectrumIdentificationProtocol(spectrumIDProtocol);

        for(String identifier: xmlParser.getIdentIds()){

            Identification identification = xmlParser.getIdentById(identifier);

            List<Peptide> peptides = new ArrayList<Peptide>();

            for(PeptideItem peptideItem: identification.getPeptideItem()){

                Spectrum spectrum = peptideItem.getSpectrum();
                PeptideSpectrumMatch psm;
                Peptide peptide;

                Integer charge = getChargeFromDescription(spectrum.getSpectrumDesc());

                Double  precursorMZ     = getMzFromDescription(spectrum.getSpectrumDesc());
                if(precursorMZ == null) precursorMZ = Double.NaN;

                String sequence = peptideItem.getSequence();

                double deltaMass = Double.NaN;

                // missedCleavages can be computed here because we dont not the enzyme
                int missedCleavages = -1;

                Map<Integer, de.mpc.pia.intermediate.Modification> modifications = transformModifications(sequence, peptideItem.getModificationItem());

                String sourceID = "spectrum=" + peptideItem.getSpectrum().getId();

                psm = compiler.insertNewSpectrum(
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

                psm.setIsDecoy(PRIDETools.isDecoyHit(identification));

                // get the peptide or create it
                peptide = compiler.getPeptide(sequence);
                if (peptide == null) {
                    peptide = compiler.insertNewPeptide(sequence);
                    pepNr++;
                }

                // add the spectrum to the peptide
                peptide.addSpectrum(psm);

                // add the modifications
                for (Map.Entry<Integer, de.mpc.pia.intermediate.Modification> mod
                        : modifications.entrySet()) {
                    psm.addModification(mod.getKey(), mod.getValue());
                }

                // In PRIDE XML we dont have a way to compute this accuratelly.
                ScoreModel score;
                Double scoreValue = null;


                // add the protein/accession info

                // no sequence information in the file

                peptides.add(peptide);
                specNr++;

            }
            Accession acc = compiler.insertNewAccession(identification.getAccession(), null);
            accNr++;
            acc.addFile(file.getID());
            // now insert the peptide and the accession into the accession peptide map
            Set<Peptide> accsPeptides = compiler.getFromAccPepMap(acc.getAccession());

            if (accsPeptides == null) {
                accsPeptides = new HashSet<Peptide>();
                compiler.putIntoAccPepMap(acc.getAccession(), accsPeptides);
            }

            accsPeptides.addAll(peptides);

            // and also insert them into the peptide accession map
            for(Peptide peptide: peptides){
                Set<Accession> pepsAccessions = compiler.getFromPepAccMap(peptide.getSequence());

                if (pepsAccessions == null) {
                    pepsAccessions = new HashSet<Accession>();
                    compiler.putIntoPepAccMap(peptide.getSequence(), pepsAccessions);
                }
                pepsAccessions.add(acc);
            }
        }
        return true;
    }

    /**
     * Converting the modification Items to intermediate modifications
     * @param sequence
     * @param modificationItem
     * @return
     */
    private static Map<Integer, de.mpc.pia.intermediate.Modification> transformModifications(String sequence, List<ModificationItem> modificationItem) {
        if(modificationItem != null && !modificationItem.isEmpty()){
            Map<Integer, de.mpc.pia.intermediate.Modification> modificationMap = new HashMap<Integer, de.mpc.pia.intermediate.Modification>();
            for(ModificationItem mod: modificationItem){
                Double mass = null;
                if(mod.getModMonoDelta() != null && mod.getModMonoDelta().get(0) != null)
                    mass = new Double(mod.getModMonoDelta().get(0));
                else if(mod.getModAvgDelta() != null && mod.getModAvgDelta().get(0) != null)
                    mass = new Double(mod.getModAvgDelta().get(0));
                de.mpc.pia.intermediate.Modification modification =
                        new de.mpc.pia.intermediate.Modification(sequence.charAt(mod.getModLocation().intValue() -1),mass,mod.getModDatabase(),mod.getModAccession());
                modificationMap.put(mod.getModLocation().intValue(), modification);
            }
            return modificationMap;
        }
        return Collections.EMPTY_MAP;
    }


    private static Double getMzFromDescription(SpectrumDesc spectrumDesc) {
        Double mz = null;
        if(spectrumDesc != null){
            PrecursorList precursorList = spectrumDesc.getPrecursorList();
            if(precursorList != null && precursorList.getCount() > 0){
                for(Precursor precursor: precursorList.getPrecursor()){
                    mz = getPrecursorMz(precursor.getIonSelection());
                    if(mz != null)
                        return mz;
                }
            }
        }
        return mz;
    }


    public static Double getPrecursorMz(uk.ac.ebi.pride.jaxb.model.Param param) {

        Double mz = null;

        if (param != null && param.getCvParam() != null) {
            for(uk.ac.ebi.pride.jaxb.model.CvParam cvParam: param.getCvParam())
                if(cvParam.getAccession().equalsIgnoreCase(PSI_ION_SELECTION_MZ) ||
                        cvParam.getAccession().equalsIgnoreCase(ION_SELECTION_MZ))
                    return new Double(cvParam.getValue());
        }
        return mz;
    }

    /**
     * Retriving the charge for each peptide
     * @param spectrumDesc
     * @return
     */
    private static Integer getChargeFromDescription(SpectrumDesc spectrumDesc) {
        Integer charge = null;
        if(spectrumDesc != null){
            PrecursorList precursorList = spectrumDesc.getPrecursorList();
            if(precursorList != null && precursorList.getCount() > 0){
                for(Precursor precursor: precursorList.getPrecursor()){
                    charge = getPrecursorChargeParamGroup(precursor.getIonSelection());
                    if(charge != null)
                        return charge;
                }
            }
        }
        return charge;
    }

    /**
     * Convert of analysis software from DataProcessing in PRIDE to mzIdentML software represntation.
     * @param dataProcessing
     * @return
     */
    private static AnalysisSoftware parseSoftware(DataProcessing dataProcessing) {
        AnalysisSoftware software = new AnalysisSoftware();

        if(dataProcessing != null && dataProcessing.getSoftware() != null){

            uk.ac.ebi.pride.jaxb.model.Software rawSoftware = dataProcessing.getSoftware();

            software.setVersion(rawSoftware.getVersion());

            software.setName(rawSoftware.getName());

            software.setCustomizations(rawSoftware.getComments());

            software.setId(rawSoftware.getName());
        }
        return software;
    }

    /**
     * Transform protocol from pride xml to core data model. For now we will add the Steps in the
     * Data processing into CVparam in protein indentification.
     *
     * @param rawProt protocol from pride xml.
     * @return Protocol protocol in core data model format.
     */
    public static SpectrumIdentificationProtocol addProtocol(SpectrumIdentificationProtocol protocol, uk.ac.ebi.pride.jaxb.model.Protocol rawProt) {
        return protocol;
        //Todo we need to parse here the rawProtocol and been able to add them to the General Protocol.
    }

    /**
     * Get precursor charge from param group
     *
     * @param param param group
     * @return precursor charge
     */
    public static Integer getPrecursorChargeParamGroup(uk.ac.ebi.pride.jaxb.model.Param param) {

        if (param != null && param.getCvParam() != null) {
            for(uk.ac.ebi.pride.jaxb.model.CvParam cvParam: param.getCvParam())
                if(cvParam.getAccession().equalsIgnoreCase(PSI_ION_SELECTION_CHARGE_STATE) ||
                        cvParam.getAccession().equalsIgnoreCase(ION_SELECTION_CHARGE_STATE))
                    return new Integer(cvParam.getValue());

        }
        return null;
    }




}