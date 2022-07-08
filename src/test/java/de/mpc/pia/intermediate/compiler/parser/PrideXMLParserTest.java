package de.mpc.pia.intermediate.compiler.parser;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.PIASimpleCompiler;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.pride.jaxb.xml.PrideXmlReader;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * @author Yasset Perez-Riverol (ypriverol@gmail.com)
 */
public class PrideXMLParserTest {

    private File prideXMLFile;

    private PrideXmlReader reader;

    private static final String piaIntermediateFileName = "PrideParser.pia.xml";

    @Before
    public void setUp() throws Exception {
        URI uri = (PrideXMLParserTest.class.getClassLoader().getResource("PRIDE_Example.xml")) != null ? (PrideXMLParserTest.class.getClassLoader().getResource("PRIDE_Example.xml")).toURI(): null;

        if(uri == null)
            throw new IOException("File not found");

        prideXMLFile = new File(uri);
        reader = new PrideXmlReader(uri.toURL());


    }

    @Test
    public void getDataFromPrideXMLFileTest() throws IOException {
        PIACompiler compiler = new PIASimpleCompiler();

        assertTrue(
        		compiler.getDataFromFile(prideXMLFile.getName(),
	                prideXMLFile.getAbsolutePath(),
	                null,
	                InputFileParserFactory.InputFileTypes.PRIDEXML_INPUT.getFileTypeShort())
        		);

        compiler.buildClusterList();

        compiler.buildIntermediateStructure();

        File piaIntermediateFile = File.createTempFile(piaIntermediateFileName, null);

        compiler.writeOutXML(piaIntermediateFile);
        compiler.finish();
    }

}