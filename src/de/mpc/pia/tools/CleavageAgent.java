package de.mpc.pia.tools;

import java.util.Arrays;
import java.util.List;

/**
 * This enum holds information for different cleavage agents / enzymes
 * 
 * @author julian
 *
 */
public enum CleavageAgent {
	
	TRYPSIN {
		@Override
		public String getName() {
			return "Trypsin";
		}
		
		@Override
		public String getAccession() {
			return "MS:1001251";
		}
		
		@Override
		public String getSiteRegexp() {
			return "(?<=[KR])(?!P)";
		}
		
		@Override
		public List<String> getAlternativeNames() {
			return Arrays.asList(new String[]{
					getName(),
					"Trypsin (Full)"
			});
		}
	},
	
	ARGC {
		@Override
		public String getName() {
			return "Arg-C";
		}
		
		@Override
		public String getAccession() {
			return "MS:1001303";
		}
		
		@Override
		public String getSiteRegexp() {
			return "(?<=R)(?!P)";
		}
	},
	
	ASPN {
		@Override
		public String getName() {
			return "Asp-N";
		}
		
		@Override
		public String getAccession() {
			return "MS:1001304";
		}
		
		@Override
		public String getSiteRegexp() {
			return "(?=[BD])";
		}
	},
	
	ASPN_AMBIC {
		@Override
		public String getName() {
			return "Asp-N_ambic";
		}
		
		@Override
		public String getAccession() {
			return "MS:1001305";
		}
		
		@Override
		public String getSiteRegexp() {
			return "(?=[DE])";
		}
	},
	
	CHYMOTRYPSIN {
		@Override
		public String getName() {
			return "Chymotrypsin";
		}
		
		@Override
		public String getAccession() {
			return "MS:1001306";
		}
		
		@Override
		public String getSiteRegexp() {
			return "(?<=[FYWL])(?!P)";
		}
	},
	
	CNBR {
		@Override
		public String getName() {
			return "CNBr";
		}
		
		@Override
		public String getAccession() {
			return "MS:1001307";
		}
		
		@Override
		public String getSiteRegexp() {
			return "(?<=M)";
		}
	},
	
	FORMICACID {
		@Override
		public String getName() {
			return "Formic_acid";
		}
		
		@Override
		public String getAccession() {
			return "MS:1001308";
		}
		
		@Override
		public String getSiteRegexp() {
			return "((?<=D))|((?=D))";
		}
		
		@Override
		public List<String> getAlternativeNames() {
			return Arrays.asList(new String[]{
					getName(),
					"Formic Acid"
			});
		}
	},
	
	LYSC {
		@Override
		public String getName() {
			return "Lys-C";
		}
		
		@Override
		public String getAccession() {
			return "MS:1001309";
		}
		
		@Override
		public String getSiteRegexp() {
			return "(?<=K)(?!P)";
		}
	},
	
	LYSCP {
		@Override
		public String getName() {
			return "Lys-C/P";
		}
		
		@Override
		public String getAccession() {
			return "MS:1001310";
		}
		
		@Override
		public String getSiteRegexp() {
			return "(?<=K)";
		}
	},
	
	PEPSINA {
		@Override
		public String getName() {
			return "PepsinA";
		}
		
		@Override
		public String getAccession() {
			return "MS:1001311";
		}
		
		@Override
		public String getSiteRegexp() {
			return "(?<=[FL])";
		}
	},
	
	TRYPCHYMO {
		@Override
		public String getName() {
			return "TrypChymo";
		}
		
		@Override
		public String getAccession() {
			return "MS:1001312";
		}
		
		@Override
		public String getSiteRegexp() {
			return "(?<=[FYWLKR])(?!P)";
		}
	},
	
	TRYPSINP {
		@Override
		public String getName() {
			return "Trypsin/P";
		}
		
		@Override
		public String getAccession() {
			return "MS:1001313";
		}
		
		@Override
		public String getSiteRegexp() {
			return "(?<=[KR])";
		}
	},
	
	V8DE {
		@Override
		public String getName() {
			return "V8-DE";
		}
		
		@Override
		public String getAccession() {
			return "MS:1001314";
		}
		
		@Override
		public String getSiteRegexp() {
			return "(?<=[BDEZ])(?!P)";
		}
	},
	
	V8E {
		@Override
		public String getName() {
			return "V8-E";
		}
		
		@Override
		public String getAccession() {
			return "MS:1001315";
		}
		
		@Override
		public String getSiteRegexp() {
			return "(?<=[EZ])(?!P)";
		}
	},
	;
	
	
	/**
	 * Returns the name in the CV
	 * @return
	 */
	public abstract String getName();
	
	
	/**
	 * Returns the accession in the CV
	 * @return
	 */
	public abstract String getAccession();
	
	
	/**
	 * Returns the regular expression of the cleavage agent, i.e. where and how
	 * it cuts
	 * @return
	 */
	public abstract String getSiteRegexp();
	
	
	/**
	 * Returns a list of all alternative names of the enzyme, including the
	 * original name.
	 * @return
	 */
	public List<String> getAlternativeNames() {
		return Arrays.asList(new String[]{
				getName()
		});
	}
	
	
	/**
	 * Gets the {@link CleavageAgent} with the given case-insensitive name. If
	 * there is none, returns null.
	 * 
	 * @param name
	 * @return
	 */
	public static CleavageAgent getByName(String name) {
		for (CleavageAgent enzyme : values()) {
			for (String enzName : enzyme.getAlternativeNames()) {
				if (enzName.equalsIgnoreCase(name)) {
					return enzyme;
				}
			}
		}
		
		return null;
	}
}
