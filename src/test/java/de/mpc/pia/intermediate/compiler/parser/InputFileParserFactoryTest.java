package de.mpc.pia.intermediate.compiler.parser;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import de.mpc.pia.intermediate.compiler.PIACompilerTest;
import de.mpc.pia.intermediate.compiler.parser.searchengines.CometTSVFileParser;
import de.mpc.pia.intermediate.compiler.parser.searchengines.MascotDatFileParser;
import de.mpc.pia.intermediate.compiler.parser.searchengines.TandemFileParser;
import de.mpc.pia.intermediate.compiler.parser.searchengines.ThermoMSFFileParser;
import de.mpc.pia.intermediate.compiler.parser.searchengines.TideTXTFileParser;

public class InputFileParserFactoryTest {

    private File cRAPfastaFile;
    private File idXMLfile;
    private File mzTabFile;
    private File prideXMLFile;
    private File mascotDatFile;
    private File mzIdentMLFile;
    private File xTandemXMLFile;
    private File msfFile;
    private File tideTXTFile;
    private File cometTSVFile;

    @Before
    public void setUp() {
        cRAPfastaFile = new File(InputFileParserFactoryTest.class.getResource("/cRAP-contaminants-20120229.fasta").getPath());
        idXMLfile = new File(PIACompilerTest.class.getResource("/merge1-tandem-fdr_filtered-015.idXML").getPath());
        mzTabFile = new File(PIACompilerTest.class.getResource("/submission/JKGF-01-DTASelect-filter.pride.mztab").getPath());
        prideXMLFile = new File(PIACompilerTest.class.getResource("/PRIDE_Example.xml").getPath());
        mascotDatFile = new File(PIACompilerTest.class.getResource("/Set1_A1.mascot.dat").getPath());
        mzIdentMLFile = new File(PIACompilerTest.class.getResource("/55merge_mascot_full.mzid").getPath());
        xTandemXMLFile = new File(PIACompilerTest.class.getResource("/Set1_A1.tandem.xml").getPath());
        msfFile = new File(PIACompilerTest.class.getResource("/QExHF04458.msf").getPath());
        tideTXTFile = new File(PIACompilerTest.class.getResource("/tide-search-cut.txt").getPath());
        cometTSVFile = new File(PIACompilerTest.class.getResource("/comet-tsv-cut.txt").getPath());
    }


    @Test
    public void testFileTypeDetection() {
        // FASTA files
        assertTrue(FastaFileParser.checkFileType(cRAPfastaFile.getAbsolutePath()));
        assertFalse(FastaFileParser.checkFileType(idXMLfile.getAbsolutePath()));
        assertEquals(InputFileParserFactory.InputFileTypes.FASTA_INPUT,
                InputFileParserFactory.getFileTypeByContent(cRAPfastaFile.getAbsolutePath()));

        // idXML files
        assertTrue(IdXMLFileParser.checkFileType(idXMLfile.getAbsolutePath()));
        assertFalse(IdXMLFileParser.checkFileType(prideXMLFile.getAbsolutePath()));
        assertEquals(InputFileParserFactory.InputFileTypes.ID_XML_INPUT,
                InputFileParserFactory.getFileTypeByContent(idXMLfile.getAbsolutePath()));

        // mzTab files
        assertTrue(MzTabParser.checkFileType(mzTabFile.getAbsolutePath()));
        assertFalse(MzTabParser.checkFileType(cRAPfastaFile.getAbsolutePath()));
        assertEquals(InputFileParserFactory.InputFileTypes.MZTAB_INPUT,
                InputFileParserFactory.getFileTypeByContent(mzTabFile.getAbsolutePath()));

        // PRIDE XML files
        assertTrue(PrideXMLParser.checkFileType(prideXMLFile.getAbsolutePath()));
        assertFalse(PrideXMLParser.checkFileType(idXMLfile.getAbsolutePath()));
        assertEquals(InputFileParserFactory.InputFileTypes.PRIDEXML_INPUT,
                InputFileParserFactory.getFileTypeByContent(prideXMLFile.getAbsolutePath()));

        // Mascot DAT files
        assertTrue(MascotDatFileParser.checkFileType(mascotDatFile.getAbsolutePath()));
        assertFalse(MascotDatFileParser.checkFileType(mzTabFile.getAbsolutePath()));
        assertEquals(InputFileParserFactory.InputFileTypes.MASCOT_DAT_INPUT,
                InputFileParserFactory.getFileTypeByContent(mascotDatFile.getAbsolutePath()));

        // mzIdentML files
        assertTrue(MzIdentMLFileParser.checkFileType(mzIdentMLFile.getAbsolutePath()));
        assertFalse(MzIdentMLFileParser.checkFileType(idXMLfile.getAbsolutePath()));
        assertEquals(InputFileParserFactory.InputFileTypes.MZIDENTML_INPUT,
                InputFileParserFactory.getFileTypeByContent(mzIdentMLFile.getAbsolutePath()));

        // X!Tandem files
        assertTrue(TandemFileParser.checkFileType(xTandemXMLFile.getAbsolutePath()));
        assertFalse(TandemFileParser.checkFileType(idXMLfile.getAbsolutePath()));
        assertEquals(InputFileParserFactory.InputFileTypes.TANDEM_INPUT,
                InputFileParserFactory.getFileTypeByContent(xTandemXMLFile.getAbsolutePath()));

        // Thermo MSF files
        assertTrue(ThermoMSFFileParser.checkFileType(msfFile.getAbsolutePath()));
        assertFalse(ThermoMSFFileParser.checkFileType(mascotDatFile.getAbsolutePath()));
        assertEquals(InputFileParserFactory.InputFileTypes.THERMO_MSF_INPUT,
                InputFileParserFactory.getFileTypeByContent(msfFile.getAbsolutePath()));

        // Tide TXT files
        assertTrue(TideTXTFileParser.checkFileType(tideTXTFile.getAbsolutePath()));
        assertFalse(TideTXTFileParser.checkFileType(mzTabFile.getAbsolutePath()));
        assertEquals(InputFileParserFactory.InputFileTypes.TIDE_TXT_INPUT,
                InputFileParserFactory.getFileTypeByContent(tideTXTFile.getAbsolutePath()));
        
        assertTrue(CometTSVFileParser.checkFileType(cometTSVFile.getAbsolutePath()));
        assertFalse(CometTSVFileParser.checkFileType(mzTabFile.getAbsolutePath()));
        assertEquals(InputFileParserFactory.InputFileTypes.COMET_TSV_INPUT,
                InputFileParserFactory.getFileTypeByContent(cometTSVFile.getAbsolutePath()));
    }
}
