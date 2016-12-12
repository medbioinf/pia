package de.mpc.pia.intermediate.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.log4j.Logger;
import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;

import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;
import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;

/**
 * This class is used to read in one or several input files and compile them
 * into one PIA XML intermediate file.
 *
 * @author julian
 *
 */
public class PIACachedCompiler extends PIACompiler {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(PIASimpleCompiler.class);


    /** the cache manager */
    private PersistentCacheManager cacheManager;

    /** bidirectional map from accession IDs to strings with the actual accessions */
    private BidiMap<Long, String> accessionIDsToStrings;

    /** the accessions */
    private List<Accession> accessions;

    /** bidirectional map from peptide IDs to the peptide seqeunces */
    private BidiMap<Long, String> peptideIDsToSequences;

    /** the peptides */
    private List<Peptide> peptides;

    /** the psm IDs */
    private List<Long> spectraIDs;

    /** map of spectra, maps from the IDs to the PSMs */
    private Cache<Long, PeptideSpectrumMatch> spectra;

    /** maps from the accession IDs to the peptide IDs, used to calculate clusters*/
    private Map<Long, List<Long>> accPepMapIDs;

    /** maps from the peptide to the accessions, used to calculate the clusters */
    private Map<Long, List<Long>> pepAccMapIDs;


    /** temporary path for caches */
    private Path tmpPath;

    /** alias for the PSM cache */
    private static final String SPECTRUM_MATCH_CACHE_ALIAS = "psms-cache";



    private static long diskSpaceGB = 4;


    /**
     * Basic constructor
     */
    public PIACachedCompiler() {
        super();

        try {
            tmpPath = Files.createTempDirectory("pia_cache");
        } catch (IOException e) {
            LOGGER.error(e);
            throw new AssertionError(e);
        }

        accessionIDsToStrings = new DualHashBidiMap<>();
        accessions = new ArrayList<>();

        peptideIDsToSequences = new DualHashBidiMap<>();
        peptides = new ArrayList<>();

        spectraIDs = new ArrayList<>();
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(tmpPath.toString()))
                .withCache(SPECTRUM_MATCH_CACHE_ALIAS, CacheConfigurationBuilder.newCacheConfigurationBuilder(Long.class, PeptideSpectrumMatch.class,
                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                                .heap(100000, EntryUnit.ENTRIES)
                                .disk(diskSpaceGB, MemoryUnit.GB, false)
                                )
                    )
                .build(true);

        spectra = cacheManager.getCache(SPECTRUM_MATCH_CACHE_ALIAS, Long.class, PeptideSpectrumMatch.class);

        accPepMapIDs = new HashMap<>();
        pepAccMapIDs = new HashMap<>();
    }


    @Override
    public Accession getAccession(String acc) {
        Long accId = accessionIDsToStrings.getKey(acc);
        if (accId != null) {
            return accessions.get(accId.intValue() - 1);
        } else {
            return null;
        }
    }


    @Override
    public Accession getAccession(Long accId) {
        return accessions.get(accId.intValue() - 1);
    }


    @Override
    public Accession insertNewAccession(String accessionStr, String dbSequence) {
        Long accId = (long) (accessions.size() + 1);
        Accession acc = new Accession(accId, accessionStr, dbSequence);

        accessions.add(acc);
        accessionIDsToStrings.put(accId, accessionStr);

        return acc;
    }


    @Override
    public int getNrAccessions() {
        return accessions.size();
    }


    @Override
    public Set<Long> getAllAccessionIDs() {
        return accessionIDsToStrings.keySet();
    }


    @Override
    public Peptide getPeptide(String sequence) {
        Long pepId = peptideIDsToSequences.getKey(sequence);
        if (pepId != null) {
            return peptides.get(pepId.intValue() - 1);
        } else {
            return null;
        }
    }


    @Override
    public Peptide getPeptide(Long peptideID) {
        return peptides.get(peptideID.intValue() - 1);
    }


    @Override
    public Peptide insertNewPeptide(String sequence) {
        Long pepId = (long) (peptides.size() + 1);

        Peptide peptide = new Peptide(pepId, sequence);
        peptides.add(peptide);
        peptideIDsToSequences.put(pepId, sequence);

        return peptide;
    }


    @Override
    public int getNrPeptides() {
        return peptides.size();
    }


    @Override
    public Set<Long> getAllPeptideIDs() {
        return peptideIDsToSequences.keySet();
    }


    @Override
    public PeptideSpectrumMatch getPeptideSpectrumMatch(Long psmId) {
        return spectra.get(psmId);
    }


    @Override
    public PeptideSpectrumMatch createNewPeptideSpectrumMatch(int charge,
            double massToCharge, double deltaMass, Double rt, String sequence,
            int missed, String sourceID, String spectrumTitle,
            PIAInputFile file, SpectrumIdentification spectrumID) {
        Long id = (long) (spectraIDs.size() + 1);

        // the PSM is added later, as it might be changed
        return  new PeptideSpectrumMatch(id, charge,
                massToCharge, deltaMass, rt, sequence, missed, sourceID,
                spectrumTitle, file, spectrumID);
    }


    @Override
    public void insertCompletePeptideSpectrumMatch(PeptideSpectrumMatch psm) {
        Long id = psm.getID();
        if (!spectra.containsKey(id)) {
            spectraIDs.add(id);
            spectra.put(id, psm);
        } else {
            LOGGER.warn("PSM " + id + " already in the compiler, this might be invalid!");
        }
    }


    @Override
    public int getNrPeptideSpectrumMatches() {
        return spectraIDs.size();
    }


    @Override
    public List<Long> getAllPeptideSpectrumMatcheIDs() {
        return spectraIDs;
    }


    @Override
    public Set<Peptide> getPeptidesFromConnectionMap(String acc) {
        Long accId = accessionIDsToStrings.getKey(acc);

        if ((accId != null) && accPepMapIDs.containsKey(accId)) {
            Set<Peptide> pepSet = new HashSet<>();

            for (Long pepId : accPepMapIDs.get(accId)) {
                int pepIdx = pepId.intValue() - 1;
                pepSet.add(peptides.get(pepIdx));
            }

            return pepSet;
        }

        return null;
    }


    @Override
    public Set<Accession> getAccessionsFromConnectionMap(String pep) {
        Long pepId = peptideIDsToSequences.getKey(pep);

        if ((pepId != null) && pepAccMapIDs.containsKey(pepId)) {
            Set<Accession> accSet = new HashSet<>();

            for (Long accId : pepAccMapIDs.get(pepId)) {
                int accIdx = accId.intValue() - 1;
                accSet.add(accessions.get(accIdx));
            }

            return accSet;
        }

        return null;
    }


    @Override
    public List<Long> getPepIDsFromConnectionMap(Long accId) {
        return accPepMapIDs.get(accId);
    }


    @Override
    public List<Long> getAccIDsFromConnectionMap(Long pepId) {
        return pepAccMapIDs.get(pepId);
    }


    @Override
    public void addAccessionPeptideConnection(Accession accession, Peptide peptide) {
        Long pepId = peptide.getID();
        Long accId = accession.getID();

        if (!accPepMapIDs.containsKey(accId)) {
            if (accessionIDsToStrings.containsKey(accId) && peptideIDsToSequences.containsKey(pepId)) {
                accPepMapIDs.put(accId, new ArrayList<>());
            } else {
                // this was called erroneous, insert a null (which will provoke a NullPointerException)
                LOGGER.error("accession or peptide was not inserted into the compiler. "
                        + "acc: " + accessionIDsToStrings.containsKey(accId) + " (" + accId + ")"
                        + ", pep: " +  peptideIDsToSequences.containsKey(pepId) + " (" + pepId + ")");
                accPepMapIDs.put(accId, null);
            }
        }

        if (!pepAccMapIDs.containsKey(pepId)) {
            if (accessionIDsToStrings.containsKey(accId) && peptideIDsToSequences.containsKey(pepId)) {
                pepAccMapIDs.put(pepId, new ArrayList<>());
            } else {
                // this was called erroneous, insert a null (which will provoke a NullPointerException)
                LOGGER.error("accession or peptide was not inserted into the compiler. "
                        + "acc: " + accessionIDsToStrings.containsKey(accId) + " (" + accId + ")"
                        + ", pep: " +  peptideIDsToSequences.containsKey(pepId) + " (" + pepId + ")");
                pepAccMapIDs.put(pepId, null);
            }
        }

        List<Long> pepIds = accPepMapIDs.get(accId);
        if (!pepIds.contains(pepId)) {
            pepIds.add(pepId);
        }

        List<Long> accIds = pepAccMapIDs.get(pepId);
        if (!accIds.contains(accId)) {
            accIds.add(accId);
        }
    }


    @Override
    public void clearConnectionMap() {
        accPepMapIDs.clear();
        pepAccMapIDs.clear();
    }


    @Override
    public void finish() {
        if (cacheManager != null) {
            spectra.clear();
            cacheManager.close();
        }

        try {
            Files.deleteIfExists(tmpPath.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }
}
