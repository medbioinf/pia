package de.mpc.pia.intermediate.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.parser.searchengines.MascotDatFileParser;
import de.mpc.pia.intermediate.compiler.parser.searchengines.TandemFileParser;
import de.mpc.pia.intermediate.compiler.parser.searchengines.ThermoMSFFileParser;
import de.mpc.pia.intermediate.compiler.parser.searchengines.TideTXTFileParser;

public class InputFileParserFactory {

	/** logger for this class */
	private static final Logger logger = Logger.getLogger(InputFileParserFactory.class);
	
	public enum InputFileTypes {
		
		/**
		 * the input file is a FASTA database file
		 */
		FASTA_INPUT {
			@Override
			public String getFileSuffix() {
				return "fasta";
			}
			
			@Override
			public String getFileTypeName() {
				return "FASTA database";
			}
			
			@Override
			public String getFileTypeShort() {
				return "FASTA";
			}
			
			/**
			 * Parses the data from a FASTA database, assuming Trypsin, a
			 * minimal peptide length of 5, maximal peptide length of 50 and
			 * one missed cleavage.
			 */
			@Override
			public boolean parseFile(String name, String fileName,
					PIACompiler compiler, String additionalInfoFileName) {
				return FastaFileParser.getDataFromFastaFile(
						name,
						fileName,
						compiler,
						"(?<=[KR])(?!P)",
						5,
						50,
						1);
			}
		},
		
		/**
		 * the input file is a idXML file from OpenMS
		 */
		ID_XML_INPUT {
			@Override
			public String getFileSuffix() {
				return "idXML";
			}
			
			@Override
			public String getFileTypeName() {
				return "OpenMS IdXML";
			}
			
			@Override
			public String getFileTypeShort() {
				return "idxml";
			}
			
			@Override
			public boolean parseFile(String name, String fileName,
					PIACompiler compiler, String additionalInfoFileName) {
				return IdXMLFileParser.getDataFromIdXMLFile(name, fileName,
						compiler);
			}
		},

        PRIDEXML_INPUT{

            @Override
            public String getFileSuffix() {
                return "xml";
            }

            @Override
            public String getFileTypeName() {
                return "Pride XML";
            }

            @Override
            public String getFileTypeShort() {
                return "xml";
            }

            @Override
            public boolean parseFile(String name, String fileName,
                                     PIACompiler compiler, String additionalInfoFileName) {
                return PrideXMLParser.getDataFromPrideXMLFile(name, fileName,
                        compiler);
            }

        },
		/**
		 * the input file is a Mascot dat file
		 */
		MASCOT_DAT_INPUT {
			@Override
			public String getFileSuffix() {
				return "dat";
			}
			
			@Override
			public String getFileTypeName() {
				return "Mascot DAT";
			}
			
			@Override
			public String getFileTypeShort() {
				return "mascot";
			}
			
			@Override
			public boolean parseFile(String name,  String fileName,
					PIACompiler compiler, String additionalInfoFileName) {
				return MascotDatFileParser.getDataFromMascotDatFile(name,
						fileName, compiler);
			}
		},
		/**
		 * the input file is in the mzIdentML format
		 */
		MZIDENTML_INPUT {
			@Override
			public String getFileSuffix() {
				return "mzid";
			}
			
			@Override
			public String getFileTypeName() {
				return "mzIdentML";
			}
			
			@Override
			public String getFileTypeShort() {
				return "mzid";
			}
			
			@Override
			public boolean parseFile(String name, String fileName,
					PIACompiler compiler, String additionalInfoFileName) {
				return MzIdentMLFileParser.getDataFromMzIdentMLFile(name,
						fileName, compiler);
			}
		},
		/**
		 * the input file is in the X!Tandem XML format
		 */
		TANDEM_INPUT {
			@Override
			public String getFileSuffix() {
				return "xml";
			}
			
			@Override
			public String getFileTypeName() {
				return "X!Tandem XML";
			}
			
			@Override
			public String getFileTypeShort() {
				return "tandem";
			}
			
			@Override
			public boolean parseFile(String name, String fileName,
					PIACompiler compiler, String additionalInfoFileName) {
				return TandemFileParser.getDataFromTandemFile(name, fileName,
						compiler, additionalInfoFileName);
			}
		},
		/**
		 * the input file is a Thermo MSF File
		 */
		THERMO_MSF_INPUT {
			@Override
			public String getFileSuffix() {
				return "msf";
			}
			
			@Override
			public String getFileTypeName() {
				return "Thermo MSF";
			}
			
			@Override
			public String getFileTypeShort() {
				return "thermo";
			}
			
			@Override
			public boolean parseFile(String name, String fileName,
					PIACompiler compiler, String additionalInfoFileName) {
				return ThermoMSFFileParser.getDataFromThermoMSFFile(name,
						fileName, compiler);
			}
		},
		/**
		 * the input file is a Tide TXT file
		 */
		TIDE_TXT_INPUT {
			@Override
			public String getFileSuffix() {
				return "txt";
			}
			
			@Override
			public String getFileTypeName() {
				return "Tide TXT";
			}
			
			@Override
			public String getFileTypeShort() {
				return "tide";
			}
			
			@Override
			public boolean parseFile(String name, String fileName,
					PIACompiler compiler, String additionalInfoFileName) {
				return TideTXTFileParser.getDataFromTideTXTFile(name,
						fileName, compiler);
			}
		},
		;
		
		/**
		 * Get the file ending for this type.
		 * 
		 * @return
		 */
		public abstract String getFileSuffix();
		
		
		/**
		 * Get the name of the file type.
		 * 
		 * @return
		 */
		public abstract String getFileTypeName();
		
		
		/**
		 * Get the short name of the file type.
		 * 
		 * @return
		 */
		public abstract String getFileTypeShort();
		
		
		/**
		 * Parses the data from the file, assuming the given file type.
		 * 
		 * @param fileName
		 * @param compiler
		 */
		public abstract boolean parseFile(String name, String fileName,
				PIACompiler compiler, String additionalInfoFileName);
	}
	
	
	/**
	 * We don't ever want to instantiate this class
	 */
	private InputFileParserFactory() {
		throw new AssertionError();
	}
	
	
	/**
	 * Returns the {@link InputFileTypes} specified by the given fileSuffix or
	 * null.
	 */
	public static InputFileTypes getFileTypeBySuffix(String fileSuffix) {
		if (fileSuffix != null) {
			for (InputFileTypes type : InputFileTypes.values()) {
				if (type.getFileSuffix().equalsIgnoreCase(fileSuffix)) {
					return type;
				}
			}
		}
		
		return null;
	}
	
	
	/**
	 * Returns the {@link InputFileTypes} specified by the given shortName or
	 * null.
	 */
	public static InputFileTypes getFileTypeByShortName(String shortName) {
		for (InputFileTypes type : InputFileTypes.values()) {
			if (type.getFileTypeShort().equalsIgnoreCase(shortName)) {
				return type;
			}
		}
		
		return null;
	}
	
	
	/**
	 * Returns a List of the short names of the available {@link InputFileTypes}.
	 */
	public static List<String> getAvailableTypeShorts() {
		List<String> typeList = new ArrayList<String>();
		
		for (InputFileTypes type : InputFileTypes.values()) {
			typeList.add(type.getFileTypeShort());
		}
		
		return typeList;
	}
	
	
	/**
	 * Parses the file given by its name and puts the date into the given
	 * {@link PIACompiler}. The file type is guessed by the file ending.
	 * 
	 * @param fileName
	 * @param compiler
	 */
	public static boolean getDataFromFile(String name, String fileName,
			PIACompiler compiler, String additionalInfoFileName,
			String fileType) {
		
		if (fileType == null) {
			String fileSuffix = fileName.substring(fileName.lastIndexOf('.')+1);
			
			if (fileSuffix != null) {
				InputFileTypes type = getFileTypeBySuffix(fileSuffix);
				if (type != null) {
					logger.info("'" + fileName + "' seems to be a " +
							type.getFileTypeName()+" file");
					return type.parseFile(name, fileName, compiler, additionalInfoFileName);
				}
			}
			
			logger.error("File '" + fileName + "' could not be parsed, " +
					"fileType could not be guessed, please specify.");
		} else {
			InputFileTypes type = getFileTypeByShortName(fileType);
			if (type != null) {
				logger.info("'" + fileName + "' should be a " +
						type.getFileTypeName()+" file");
				return type.parseFile(name, fileName, compiler, additionalInfoFileName);
			}
			
			logger.error("File '" + fileName + "' could not be parsed, fileType '" +
					fileType + "' unknown.");
		}
		return false;
	}
	
}
