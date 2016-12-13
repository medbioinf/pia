package de.mpc.pia.tools;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.biojava.nbio.ontology.Term;
import org.biojava.nbio.ontology.Triple;

import de.mpc.pia.tools.obo.OBOMapper;
import uk.ac.ebi.jmzidml.model.mzidml.AbstractParam;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.Cv;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.Enzyme;
import uk.ac.ebi.jmzidml.model.mzidml.Enzymes;
import uk.ac.ebi.jmzidml.model.mzidml.FileFormat;
import uk.ac.ebi.jmzidml.model.mzidml.Param;
import uk.ac.ebi.jmzidml.model.mzidml.ParamList;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIDFormat;
import uk.ac.ebi.jmzidml.model.mzidml.Tolerance;
import uk.ac.ebi.jmzidml.model.mzidml.UserParam;


/**
 * Some additional functions for handling mzIdentML files.
 *
 * @author julian
 *
 */
public class MzIdentMLTools {

    /** get the index from the title, which is the scan number there */
    public static final Pattern patternScanInTitle = Pattern.compile("^.*scan=(\\d+).*$");

    /**  the PSI-MS collected vocabulary as {@link Cv} variable */
    private static final Cv psiMS = new Cv();

    /**  the "Unit Ontology" collected vocabulary as {@link Cv} variable */
    private static final Cv unitOntology = new Cv();

    /** PIA as an {@link AnalysisSoftware} */
    private static final AnalysisSoftware piaAnalysisSoftware = new AnalysisSoftware();

    /**
     * static initialization
     */
    static {
        psiMS.setId("PSI-MS");
        psiMS.setFullName("PSI-MS");
        psiMS.setUri(OntologyConstants.PSI_MS_OBO_URL);
        psiMS.setVersion(OntologyConstants.PSI_MS_OBO_VERSION);

        unitOntology.setId("UO");
        unitOntology.setFullName("Unit Ontology");
        unitOntology.setUri("https://raw.githubusercontent.com/bio-ontology-research-group/unit-ontology/master/unit.obo");

        piaAnalysisSoftware.setName("PIA");
        piaAnalysisSoftware.setId("software_PIA");
        piaAnalysisSoftware.setVersion(PIAConstants.version);
        Param tempParam = new Param();
        tempParam.setParam(createPSICvParam(OntologyConstants.PIA, null));
        piaAnalysisSoftware.setSoftwareName(tempParam);
    }


    /**
     * We don't ever want to instantiate this class
     */
    private MzIdentMLTools() {
        throw new AssertionError();
    }


    /**
     * Compares the given Params for equality, i.e. all the Strings in it.
     * Returns also true, if both params are null.
     *
     * @param x
     * @param y
     * @return
     */
    public static boolean paramsEqual(Param x, Param y) {
        boolean ret;

        if ((x == null) && (y == null)) {
            // both are null
            ret = true;
        } else if ((x == null) || (y == null)) {
            // one is null
            ret = false;
        } else {
            // neither is null
            CvParam cv1 = x.getCvParam();
            CvParam cv2 = y.getCvParam();
            if ((cv1 != null) && (cv2 != null)) {
                // both are cvParams
                ret = cvParamsEqualOrNull(cv1, cv2);
            } else {
                UserParam up1 = x.getUserParam();
                UserParam up2 = y.getUserParam();

                ret = userParamsEqualOrNull(up1, up2);
            }
        }

        return ret;
    }


    /**
     * Checks for the given cvParams, whether both are null are equal.
     */
    public static boolean cvParamsEqualOrNull(CvParam x, CvParam y) {
        if ((x != null) && (y != null)) {
            // both are not null
            boolean equal = true;

            // required
            equal = x.getAccession().equals(y.getAccession());
            equal &= x.getCvRef().equals(y.getCvRef());
            equal &= x.getName().equals(y.getName());

            // optional
            equal &= PIATools.bothNullOrEqual(x.getUnitAccession(),
                    y.getUnitAccession());
            equal &= PIATools.bothNullOrEqual(x.getUnitCvRef(),
                    y.getUnitCvRef());
            equal &= PIATools.bothNullOrEqual(x.getUnitName(),
                    y.getUnitName());
            equal &= PIATools.bothNullOrEqual(x.getValue(),
                    y.getValue());

            return equal;
        } else {
            // both must be null then
            return (x == null) && (y == null);
        }
    }


    /**
     * Checks for the given userParams, whether both are null are equal.
     */
    public static boolean userParamsEqualOrNull(UserParam x, UserParam y) {
        if ((x != null) && (y != null)) {
            // both are not null
            boolean equal = true;

            // required
            equal = x.getName().equals(y.getName());

            // optional
            equal &= PIATools.bothNullOrEqual(x.getType(),
                    y.getType());
            equal &= PIATools.bothNullOrEqual(x.getUnitAccession(),
                    y.getUnitAccession());
            equal &= PIATools.bothNullOrEqual(x.getUnitCvRef(),
                    y.getUnitCvRef());
            equal &= PIATools.bothNullOrEqual(x.getUnitName(),
                    y.getUnitName());
            equal &= PIATools.bothNullOrEqual(x.getValue(),
                    y.getValue());


            return equal;
        } else {
            // both must be null then
            return (x == null) && (y == null);
        }
    }


    /**
     * Checks for the given fileFormats, whether both are null are equal.
     */
    public static boolean fileFormatsEqualOrNull(FileFormat x, FileFormat y) {
        if ((x != null) && (y != null)) {
            // both are not null
            return MzIdentMLTools.cvParamsEqualOrNull(x.getCvParam(),
                    y.getCvParam());
        } else {
            // both must be null to be equal
            return (x == null) && (y == null);
        }
    }


    /**
     * Checks for the given spectrumIDFormats, whether both are null are equal.
     */
    public static boolean spectrumIDFormatEqualOrNull(SpectrumIDFormat x,
            SpectrumIDFormat y) {
        if ((x != null) && (y != null)) {
            // both are not null
            return MzIdentMLTools.cvParamsEqualOrNull(x.getCvParam(),
                    y.getCvParam());
        } else {
            // both must be null to be equal
            return (x == null) && (y == null);
        }
    }


    /**
     * Adds the correct unit arguments to an {@link AbstractParam}, which is
     * given from a (parsed) string.
     * <p>
     * The abstractParam's value should be set before, as it is used for the
     * conversion from mmu to Dalton.
     *
     * @param unit
     * @param abstractParam
     * @return
     */
    public static boolean setUnitParameterFromString(String unit, AbstractParam abstractParam) {
        if ((unit == null) || (abstractParam == null)) {
            return false;
        }

        unit = unit.trim();

        if (unit.equalsIgnoreCase("Da") ||
                unit.equalsIgnoreCase("Dalton") ||
                unit.equalsIgnoreCase("amu") ||
                unit.equalsIgnoreCase("u") ||
                unit.equalsIgnoreCase("unified atomic mass unit")) {
            abstractParam.setUnitAccession("UO:0000221");
            abstractParam.setUnitCv(getUnitOntology());
            abstractParam.setUnitName("dalton");
            return true;
        } else if (unit.equalsIgnoreCase("ppm") ||
                unit.equalsIgnoreCase("parts per million")) {
            abstractParam.setUnitAccession("UO:0000169");
            abstractParam.setUnitCv(getUnitOntology());
            abstractParam.setUnitName("parts per million");
            return true;
        } else if (unit.equalsIgnoreCase("percent") ||
                unit.equalsIgnoreCase("%")) {
            abstractParam.setUnitAccession("UO:0000187");
            abstractParam.setUnitCv(getUnitOntology());
            abstractParam.setUnitName("percent");
            return true;
        } else if (unit.equalsIgnoreCase("mmu")) {
            // one mmu is 0.001 Da -> calculate it to dalton
            try {
                Double value = Double.parseDouble(abstractParam.getValue());
                value *= 0.001;
                abstractParam.setValue(value.toString());
                abstractParam.setUnitAccession("UO:0000221");
                abstractParam.setUnitCv(getUnitOntology());
                abstractParam.setUnitName("dalton");
            } catch (NumberFormatException e) {
                // could not
                abstractParam.setUnitName("mmu");
                return false;
            }
        }

        return false;
    }


    /**
     * Create an {@link Enzyme} corresponding to the given name
     *
     * @param enzymeString
     * @return
     */
    public static Enzyme getEnzymeFromName(String enzymeString) {
        if ((enzymeString == null) || (enzymeString.trim().length() == 0)) {
            return null;
        }

        CleavageAgent agent = CleavageAgent.getByName(enzymeString);
        Enzyme enzyme = new Enzyme();
        ParamList paramList = new ParamList();

        if (agent != null) {
            CvParam cvParam = new CvParam();
            cvParam.setAccession(agent.getAccession());
            cvParam.setCv(MzIdentMLTools.getCvPSIMS());
            cvParam.setName(agent.getName());

            enzyme.setSiteRegexp(agent.getSiteRegexp());

            paramList.getCvParam().add(cvParam);
            enzyme.setId("enzyme_" + cvParam.getAccession());
        } else {
            UserParam userParam = new UserParam();
            userParam.setName(enzymeString);

            paramList.getUserParam().add(userParam);
            enzyme.setId("enzyme_" + userParam.getName());
        }

        enzyme.setEnzymeName(paramList);
        return enzyme;
    }


    /**
     * Getter for the PSI-MS controlled vocabulary
     * @return
     */
    public static Cv getCvPSIMS() {
        return psiMS;
    }


    /**
     * Getter for the UnitOntology controlled vocabulary
     * @return
     */
    public static Cv getUnitOntology() {
        return unitOntology;
    }


    /**
     * Getter for PIA as an {@link AnalysisSoftware}
     * @return
     */
    public static AnalysisSoftware getPIAAnalysisSoftware() {
        return piaAnalysisSoftware;
    }


    /**
     * Shortcut function to create a {@link CvParam} without unit information.
     *
     * @return
     */
    public static CvParam createCvParam(String cvAccession, Cv cv, String name,
            String value) {
        return createCvParam(cvAccession, cv, name, value, null, null, null);
    }


    /**
     * Shortcut function to create a {@link CvParam} from OntologyConstant for
     * an accession inside the PSI ontology
     *
     * @return
     */
    public static CvParam createPSICvParam(OntologyConstants psiEntry, String value) {
        return createCvParam(psiEntry.getPsiAccession(),
                getCvPSIMS(),
                psiEntry.getPsiName(),
                value,
                null, null, null);
    }


    /**
     * Shortcut function to create a {@link CvParam}.
     *
     * @return
     */
    public static CvParam createCvParam(String cvAccession, Cv cv, String name,
            String value, String unitAccession, Cv unitCv, String unitName) {
        CvParam cvParam = new CvParam();

        if ((cvAccession != null) && !cvAccession.isEmpty()) {
            cvParam.setAccession(cvAccession);
        }

        if (cv != null) {
            cvParam.setCv(cv);
        }

        if ((name != null) && !name.isEmpty()) {
            cvParam.setName(name);
        }

        if ((value != null) && !value.isEmpty()) {
            cvParam.setValue(value);
        }

        if ((unitAccession != null) && !unitAccession.isEmpty()) {
            cvParam.setUnitAccession(unitAccession);
        }

        if (unitCv != null) {
            cvParam.setUnitCv(unitCv);
        }

        if ((unitName != null) && !unitName.isEmpty()) {
            cvParam.setUnitName(unitName);
        }

        return cvParam;
    }


    /**
     * Shortcut function to create a {@link UserParam} without unit information.
     *
     * @return
     */
    public static UserParam createUserParam(String name, String value, String type) {
        return createUserParam(name, value, type, null, null, null);
    }


    /**
     * Shortcut function to create a {@link UserParam}.
     *
     * @return
     */
    public static UserParam createUserParam(String name, String value, String type,
            String unitAccession, Cv unitCv, String unitName) {
        UserParam userParam = new UserParam();

        userParam.setType(value);

        if ((name != null) && !name.isEmpty()) {
            userParam.setName(name);
        }

        if ((value != null) && !value.isEmpty()) {
            userParam.setValue(value);
        }

        if ((type != null) && !type.isEmpty()) {
            userParam.setType(type);
        }

        if ((unitAccession != null) && !unitAccession.isEmpty()) {
            userParam.setUnitAccession(unitAccession);
        }

        if (unitCv != null) {
            userParam.setUnitCv(unitCv);
        }

        if ((unitName != null) && !unitName.isEmpty()) {
            userParam.setUnitName(unitName);
        }

        return userParam;
    }


    /**
     * Creates a search tolerance with the given toleranceValue as plus and
     * minus tolerance value and the given unit.
     *
     * @param toleranceValue
     * @param unit
     * @return
     */
    public static Tolerance createSearchTolerance(String toleranceValue, String unit) {
        if (toleranceValue == null) {
            return null;
        }

        Tolerance tolerance = new Tolerance();

        AbstractParam abstractParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.SEARCH_TOLERANCE_PLUS_VALUE,
                toleranceValue);
        MzIdentMLTools.setUnitParameterFromString(unit, abstractParam);
        tolerance.getCvParam().add((CvParam)abstractParam);

        abstractParam = MzIdentMLTools.createPSICvParam(
                OntologyConstants.SEARCH_TOLERANCE_MINUS_VALUE,
                toleranceValue);
        MzIdentMLTools.setUnitParameterFromString(unit, abstractParam);
        tolerance.getCvParam().add((CvParam)abstractParam);

        return tolerance;
    }


    /**
     *
     * @param sequence
     * @param enzymes
     * @param enzymesToRegexes
     * @return
     */
    public static int calculateMissedCleavages(String sequence, Enzymes enzymes,
            Map<String, String> enzymesToRegexes, OBOMapper oboMapper) {
        int missed = 0;
        if (enzymes != null) {
            for (Enzyme enzyme : enzymes.getEnzyme()) {
                String regExp = enzyme.getSiteRegexp();

                ParamList enzymeName = enzyme.getEnzymeName();
                if ((regExp == null) && (enzymeName != null)) {
                    // no siteRegexp given, but enzymeName
                    List<AbstractParam> paramList = enzymeName.getParamGroup();
                    if (!paramList.isEmpty()) {
                        if (paramList.get(0) instanceof CvParam) {
                            String oboID = ((CvParam)(paramList.get(0))).getAccession();
                            regExp = enzymesToRegexes.get(oboID);

                            if (regExp == null) {
                                // try to get the regular expression for this enzyme and put it into the map
                                Term oboTerm = oboMapper.getTerm(oboID);
                                if (oboTerm != null) {
                                    Set<Triple> tripleSet = oboMapper.getTriples(oboTerm, null, null);

                                    for (Triple triple : tripleSet) {
                                        if (triple.getPredicate().getName().equals(OBOMapper.obo_relationship) &&
                                                triple.getObject().getName().startsWith(OBOMapper.obo_has_regexp)) {
                                            String regExpID = triple.getObject().getName().substring(11).trim();
                                            Term regExpTerm = oboMapper.getTerm(regExpID);
                                            if (regExpTerm != null) {
                                                regExp = StringEscapeUtils.unescapeJava(regExpTerm.getDescription());
                                                enzymesToRegexes.put(oboID, regExp);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (regExp == null) {
                    // no regexp found -> set the missed cleavages to -1, because it is not calculable
                    return -1;
                }

                missed += sequence.split(regExp).length - 1;
            }
        }

        return missed;
    }
}
