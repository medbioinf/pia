package de.mpc.pia.intermediate.compiler.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.ebi.jmzidml.model.mzidml.DBSequence;

/**
 * This class contains the information in a fasta header.
 * 
 * @author julian
 *
 */
public class FastaHeaderInfos {
	
	/**
	 * The supported FASTA databases.
	 * 
	 * @author julian
	 *
	 */
	public enum Database {
		
		UNIPROT {
			@Override
			public String getName() {
				return "UniProt";
			}
			
			@Override
			public String matchRegex() {
				// TODO: use official regex!
				return "^>?(?:sp|tr)\\|[^|]+\\|.*$";
			}
			
			@Override
			public FastaHeaderInfos parseHeader(String header) {
				Matcher matcher;
				Pattern pattern = Pattern.compile("^>?(?:sp|tr)\\|([^|]+)\\|(.*)$");
				
				matcher = pattern.matcher(header);
				
				if (matcher.matches()) {
					return new FastaHeaderInfos(this, matcher.group(1),
							matcher.group(2).trim());
				}
				else {
					return null;
				}
			}
		},
		
		UNIPROT_SHORTENED {
			@Override
			public String getName() {
				return "UniProt_short";
			}
			
			@Override
			public String matchRegex() {
				return "^>?(?:(?:[OPQ][0-9][A-Z0-9]{3}[0-9])|(?:[A-NR-Z][0-9](?:[A-Z][A-Z0-9]{2}[0-9]){1,2}))\\|[^ ]+_[^ ]+.*$";
			}
			
			@Override
			public FastaHeaderInfos parseHeader(String header) {
				Matcher matcher;
				Pattern pattern = Pattern.compile("^>?((?:[OPQ][0-9][A-Z0-9]{3}[0-9])|(?:[A-NR-Z][0-9](?:[A-Z][A-Z0-9]{2}[0-9]){1,2}))\\|([^_]+_[^ ]+) .*$");
				
				matcher = pattern.matcher(header);
				
				if (matcher.matches()) {
					return new FastaHeaderInfos(this, matcher.group(1), matcher.group(2).trim());
				}
				else {
					return null;
				}
			}
		},
		
		GENBANK {
			@Override
			public String getName() {
				return "GenBank";
			}
			
			@Override
			public String matchRegex() {
				return "^>?gi\\|[0-9]+\\|gb\\|[^|]+\\|.*$";
			}
			
			@Override
			public FastaHeaderInfos parseHeader(String header) {
				Matcher matcher;
				Pattern pattern = Pattern.compile("^>?gi\\|[0-9]+\\|gb\\|([^|]+)\\|(.*)$");
				
				matcher = pattern.matcher(header);
				
				if (matcher.matches()) {
					return new FastaHeaderInfos(this, matcher.group(1),
							matcher.group(2).trim());
				}
				else {
					return null;
				}
			}
		},
		
		SGD {
			@Override
			public String getName() {
				return "Saccharomyces Genome Databank";
			}
			
			@Override
			public String matchRegex() {
				return "^>?[^ ]+ [^ ]+ SGDID.+$";
			}
			
			@Override
			public FastaHeaderInfos parseHeader(String header) {
				Matcher matcher;
				Pattern pattern = Pattern.compile("^>?([^ ]+) ([^ ]+ SGDID.+)$");
				
				matcher = pattern.matcher(header);
				
				if (matcher.matches()) {
					return new FastaHeaderInfos(this, matcher.group(1),
							matcher.group(2).trim());
				}
				else {
					return null;
				}
			}
		},
		;
		
		
		/**
		 * The human readable name of the database
		 */
		public abstract String getName();
		
		
		/**
		 * The start regex of the database
		 * @return
		 */
		public abstract String matchRegex();
		
		
		/**
		 * The regex, which parses the information from the header
		 * @return
		 */
		public abstract FastaHeaderInfos parseHeader(String header);
	}
	
	
	/** the database, this info comes from */
	private Database database;
	
	/** accession parsed for this header */
	private String accession;
	
	/** description in the header */
	private String description;
	
	
	public FastaHeaderInfos(Database database,
			String accession, String description) {
		this.database = database;
		this.accession = accession;
		this.description = description;
	}
	
	
	/**
	 * Constructor which parses the information from the given fastaHeader
	 */
	public static FastaHeaderInfos parseHeaderInfos(String fastaHeader) {
		for (Database db : Database.values()) {
			if (Pattern.matches(db.matchRegex(), fastaHeader)) {
				return db.parseHeader(fastaHeader);
			}
		}
		
		// remove the ">"
		if (fastaHeader.startsWith(">")) {
			fastaHeader = fastaHeader.substring(1);
		} else if (fastaHeader.startsWith("&gt;")) {
			fastaHeader = fastaHeader.substring(4);
		}
		
		
		// nothing parsable -> take everything until the first whitespace as accession
		String[] split = fastaHeader.split("\\s+", 2);
		if (split.length == 1) {
			return new FastaHeaderInfos(null, split[0], null);
		} else {
			return new FastaHeaderInfos(null, split[0], split[1]);
		}
	}
	
	
	/**
	 * Constructor which parses the information from a given DBSequence
	 */
	public static FastaHeaderInfos parseHeaderInfos(DBSequence dbSequence) {
		// TODO: check for cvParams containing the description...
		String accession = dbSequence.getAccession();
		
		return parseHeaderInfos(accession);
	}
	
	
	public Database getDatabase() {
		return database;
	}
	
	
	public String getAccession() {
		return accession;
	}
	
	
	public String getDescription() {
		return description;
	}
}
