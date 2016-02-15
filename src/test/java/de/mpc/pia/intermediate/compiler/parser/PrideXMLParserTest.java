package de.mpc.pia.intermediate.compiler.parser;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import org.apache.log4j.Logger;
import org.apache.xerces.util.URI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * @author Yasset Perez-Riverol (ypriverol@gmail.com)
 * @date 15/02/2016
 */
public class PrideXMLParserTest {

    /** logger for this class */
    private static final Logger logger = Logger.getLogger(PrideXMLParser.class);


    PIACompiler compiler;

    File prideXMLFile;

    @Before
    public void setUp() throws Exception {

        compiler = new PIACompiler();

        java.net.URI uri = PrideXMLParserTest.class.getClassLoader().getResource("PRIDE_Example.xml").toURI();

        prideXMLFile = new File(uri);

    }

    @Test
    public void getDataFromPrideXMLFileTest(){

        compiler.getDataFromFile(prideXMLFile.getName(),prideXMLFile.getAbsolutePath(), null,InputFileParserFactory.InputFileTypes.PRIDEXML_INPUT.getFileTypeShort());

        compiler.buildIntermediateStructure();

        compiler.buildClusterList();

    }

    @After
    public void tearDown() throws Exception {

    }
}