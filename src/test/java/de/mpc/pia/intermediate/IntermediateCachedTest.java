package de.mpc.pia.intermediate;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Driver;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpc.pia.modeller.PIAModellerTest;

public class IntermediateCachedTest {

    /** logger for this class */
    private static final Logger LOGGER = Logger.getLogger(IntermediateCachedTest.class);

    public static File piaFile;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        piaFile = new File(PIAModellerTest.class.getResource("/55merge_mascot_tandem.pia.xml").getPath());
        //piaFile = new File("/mnt/data/uniNOBACKUP/PIA/manuscript_datasets/technical_paper/to_submit/03_iPRG2008/PIA/mascot.pia.xml");
    }


    @Before
    public void setUp() throws Exception {

    }


    @Test
    public void testIntermediateCached() {
        /*
        PIAIntermediateJAXBHandler intermediateHandler;
        intermediateHandler = new PIAIntermediateJAXBHandler();
        */

        Runtime runtime = Runtime.getRuntime();
        double mb = 1024*1024;
        final long startTime = System.nanoTime();
        final long endTime;

        /*
        try {
            intermediateHandler.parse(piaFile.getAbsolutePath(), null);
        } catch (FileNotFoundException e) {
            LOGGER.error(e);
        } catch (XMLStreamException e) {
            LOGGER.error(e);
        } catch (JAXBException e) {
            LOGGER.error(e);
        }

        endTime = System.nanoTime();
        LOGGER.info(piaFile + " successfully parsed.");
        LOGGER.info(intermediateHandler.getFiles().size() + " files");
        LOGGER.info(intermediateHandler.getSpectraData().size() + " spectra data inputs");
        LOGGER.info(intermediateHandler.getSearchDatabase().size() + " searchDBs");
        LOGGER.info(intermediateHandler.getAnalysisSoftware().size() + " softwares");
        LOGGER.info(intermediateHandler.getGroups().size() + " groups");
        LOGGER.info(intermediateHandler.getAccessions().size() + " accessions");
        LOGGER.info(intermediateHandler.getPeptides().size() + " peptides");
        LOGGER.info(intermediateHandler.getPSMs().size() + " peptide spectrum matches");
        LOGGER.info(intermediateHandler.getNrTrees() + " trees");
        */

        LOGGER.info("Total Memory: " + runtime.totalMemory() / mb + " MB");
        LOGGER.info("Used Memory: " + (runtime.totalMemory() - runtime.freeMemory()) / mb + " MB");
        LOGGER.info("Free Memory: " + runtime.freeMemory() / mb + " MB");
        LOGGER.info("Max Memory: " + runtime.maxMemory() / mb + " MB");
        //LOGGER.info("Execution time: " + ((endTime - startTime) / 1000000000.0));
    }



    @Test
    public void testCache() {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("preConfigured",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(Long.class, String.class, ResourcePoolsBuilder.heap(10)))
                .build();

        cacheManager.init();

        Cache<Long, String> preConfigured = cacheManager.getCache("preConfigured", Long.class, String.class);


        Cache<Long, String> myCache = cacheManager.createCache("myCache",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(Long.class, String.class, ResourcePoolsBuilder.heap(10)).build());

        myCache.put(1L, "da one!");
        String value = myCache.get(1L);



        cacheManager = CacheManagerBuilder.newCacheManagerBuilder().withCache("tieredCache",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(Long.class, String.class,
                    ResourcePoolsBuilder.newResourcePoolsBuilder()
                        .heap(10000, EntryUnit.ENTRIES)
                        .offheap(3, MemoryUnit.GB))
                    )
                .build(true);


        cacheManager.removeCache("preConfigured");
        cacheManager.close();
    }
}
