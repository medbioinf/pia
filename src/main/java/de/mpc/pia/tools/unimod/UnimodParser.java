package de.mpc.pia.tools.unimod;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;

import uk.ac.ebi.jmzidml.model.mzidml.Cv;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.Modification;

import de.mpc.pia.tools.unimod.jaxb.ModT;
import de.mpc.pia.tools.unimod.jaxb.SpecificityT;
import de.mpc.pia.tools.unimod.jaxb.UnimodT;


public class UnimodParser {

    /** the mass tolerance for finding a modification by mass in Unimod */
    public static Double unimod_mass_tolerance = 0.001;

    /** the path to the packaged unimod */
    private static String packagedUnimod =  "/de/mpc/pia/unimod.xml";

    /** the CV for unimod */
    private static Cv cvUnimod;

    // defining statics
    static {
        cvUnimod = new Cv();
        cvUnimod.setId("UNIMOD");
        cvUnimod.setFullName("UNIMOD");
        cvUnimod.setUri("http://www.unimod.org/obo/unimod.obo");
    }

    /** the modifications from the unimod */
    private List<ModT> modifications;


    /** logger for this class */
    private static final Logger logger = Logger.getLogger(UnimodParser.class);


    public UnimodParser() {
        this(true);
    }


    public UnimodParser(boolean useOnline) {
        InputStream inStream = null;

        if (useOnline) {
            try {
                inStream = new URL("http://www.unimod.org/xml/unimod.xml").openStream();
            } catch (IOException e) {
                logger.warn("could not use remote unimod.xml file, check internet connection");
            }
        }

        if (inStream == null) {
            try {
                inStream = this.getClass().getResourceAsStream(packagedUnimod);
            } catch (Exception e) {
                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (IOException ioex) {
                        logger.error(ioex);
                    }
                }
                logger.error("could not get unimod.xml file", e);
                throw new AssertionError(e);
            }
        }

        try {
            JAXBContext context = JAXBContext.newInstance(UnimodT.class.getPackage().getName());
            Unmarshaller um = context.createUnmarshaller();
            @SuppressWarnings("unchecked")
            JAXBElement<UnimodT> doc = (JAXBElement<UnimodT>)um.unmarshal(inStream);

            modifications = doc.getValue().getModifications().getMod();
        } catch (JAXBException e) {
            logger.error("could not parse unimod.xml file", e);
            throw new AssertionError(e);
        }
    }


    /**
     * Returns the Cv definition for unimod
     * @return
     */
    public static Cv getCv() {
        return cvUnimod;
    }


    /**
     * Creates a {@link Modification} from the given UNIMOD modification.
     * @param mod
     * @return
     */
    public Modification createModification(ModT uniMod, Integer position,
            List<String> residues) {
        Modification modification = new Modification();

        modification.setAvgMassDelta(uniMod.getDelta().getAvgeMass());
        if (position != null) {
            modification.setLocation(position);
        }
        modification.setMonoisotopicMassDelta(uniMod.getDelta().getMonoMass());
        modification.getResidues().addAll(residues);

        CvParam cvParam = new CvParam();
        cvParam.setAccession(cvUnimod.getId() + ":" + uniMod.getRecordId());
        cvParam.setCv(cvUnimod);
        cvParam.setName(uniMod.getTitle());
        modification.getCvParam().add(cvParam);
        return modification;
    }


    public Modification createModification(ModT uniMod, Integer position,
            String residue) {
        List<String> residueList = new ArrayList<String>(1);
        residueList.add(residue);
        return createModification(uniMod, position, residueList);
    }


    public ModT getModification(String accession, String name, Double massdelta,
            String residue) {
        List<String> residueList = new ArrayList<String>(1);
        residueList.add(residue);
        return getModification(accession, name, massdelta, residueList);
    }


    public ModT getModification(String accession, String name, Double massdelta,
            List<String> residues) {
        ModT mod = null;

        if ((accession != null)) {
            mod = getModificationByAccession(accession);
        }

        if ((mod == null) &&
                (name != null) && (massdelta != null) && (residues != null)) {
            mod = getModificationByNameAndMass(name, massdelta, residues);
        }

        if ((mod == null) && (massdelta != null) && (residues != null)) {
            mod = getModificationByMass(massdelta, residues);
        }

        return mod;
    }


    /**
     * Looks for a modification given the accession (UNIMOD:X)
     *
     * @param query
     * @param residue
     * @return
     */
    public ModT getModificationByAccession(String accession) {
        if (accession.startsWith("UNIMOD:")) {
            accession = accession.substring(7);
        }

        try {
            Long id = Long.parseLong(accession);
            for (ModT mod : modifications) {
                if (mod.getRecordId().equals(id)) {
                    return mod;
                }
            }
        } catch (NumberFormatException e) {
            logger.error("Could not parse accession: " + accession, e);
        }
        return null;
    }


    /**
     * Looks for a modification given the name and (some) residues.
     *
     * @param query
     * @param residues
     * @return
     */
    public ModT getModificationByName(String query, List<String> residues) {

        for (ModT mod : modifications) {
            boolean nameFound = false;

            if (mod.getTitle().equalsIgnoreCase(query) ||
                    mod.getFullName().equalsIgnoreCase(query)) {
                nameFound = true;
            } else {
                for (String altName : mod.getAltName()) {
                    if (altName.equalsIgnoreCase(query)) {
                        nameFound = true;
                        break;
                    }
                }
            }

            if (nameFound && checkResidues(mod, residues)) {
                return mod;
            }
        }

        return null;
    }


    /**
     * Looks for a modification given the name and a residue.
     *
     * @param query
     * @param residue
     * @return
     */
    public ModT getModificationByName(String query, String residue) {
        List<String> residueList = new ArrayList<String>(1);
        residueList.add(residue);
        return getModificationByName(query, residueList);
    }


    /**
     * Looks for a modification with the given name and mass shift on the
     * allowed residues.
     *
     * @param query
     * @param massdelta
     * @param residues
     * @return
     */
    public ModT getModificationByNameAndMass(String query, Double massdelta,
            List<String> residues) {

        for (ModT mod : modifications) {
            boolean nameFound = false;

            if (mod.getTitle().equalsIgnoreCase(query) ||
                    mod.getFullName().equalsIgnoreCase(query)) {
                nameFound = true;
            } else {
                for (String altName : mod.getAltName()) {
                    if (altName.equalsIgnoreCase(query)) {
                        nameFound = true;
                        break;
                    }
                }
            }

            if (nameFound &&
                    (Math.abs(mod.getDelta().getMonoMass() - massdelta) <= unimod_mass_tolerance) &&
                    checkResidues(mod, residues)) {
                return mod;
            }
        }

        return null;
    }


    /**
     * Looks for a modification given the mass shift and a residue.
     *
     * @param query
     * @param residue
     * @return
     */
    public ModT getModificationByMass(Double massdelta, String residue) {
        List<String> residueList = new ArrayList<String>(1);
        residueList.add(residue);
        return getModificationByMass(massdelta, residueList);
    }


    /**
     * Looks for a modification given the mass shift on the allowed residues.
     *
     * @param query
     * @param massdelta
     * @param residues
     * @return
     */
    public ModT getModificationByMass(Double massdelta, List<String> residues) {
        for (ModT mod : modifications) {
            if (Math.abs(mod.getDelta().getMonoMass() - massdelta) <= unimod_mass_tolerance) {
                if (checkResidues(mod, residues)) {
                    return mod;
                }
            }
        }

        return null;
    }


    /**
     * Checks for the given residues whether all of them are allowed for the
     * given modification.
     *
     * @param modification
     * @param residues
     * @return
     */
    private boolean checkResidues(ModT modification, List<String> residues) {
        Set<String> specificities =
                new HashSet<String>(modification.getSpecificity().size());
        for (SpecificityT spec : modification.getSpecificity()) {
            specificities.add(spec.getSite());

            // TODO: make this more sophisticated
            if (spec.getSite().equalsIgnoreCase("N-term") ||
                    spec.getSite().equalsIgnoreCase("C-term")) {
                return true;
            }
        }

        boolean residuesOK = true;
        for (String residue : residues) {
            if (!residue.equals(".")) {
                residuesOK &= specificities.contains(residue);
            }
        }

        return residuesOK;
    }
}
