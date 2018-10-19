package de.mpc.pia.modeller.report.filter;

/**
 * This enum contains the comparator types used by the filters.
 * 
 * @author julian
 *
 */
public enum FilterComparator {
	less {
		@Override
		public String toString() {
			return "less";
		}
		
		@Override
		public String getLabel() {
			return "<";
		}
		
		@Override
		public String getCliShort() {
			return "LT";
		}
	},
	less_equal {
		@Override
		public String toString() {
			return "less_equal";
		}
		
		@Override
		public String getLabel() {
			return "<=";
		}
		
		@Override
		public String getCliShort() {
			return "LEQ";
		}
	},
	equal {
		@Override
		public String toString() {
			return "equal";
		}
		
		@Override
		public String getLabel() {
			return "=";
		}
		
		@Override
		public String getCliShort() {
			return "EQ";
		}
	},
	greater_equal {
		@Override
		public String toString() {
			return "greater_equal";
		}
		
		@Override
		public String getLabel() {
			return ">=";
		}
		
		@Override
		public String getCliShort() {
			return "GEQ";
		}
	},
	greater {
		@Override
		public String toString() {
			return "greater";
		}
		
		@Override
		public String getLabel() {
			return ">";
		}
		
		@Override
		public String getCliShort() {
			return "GT";
		}
	},
	
	contains {
		@Override
		public String toString() {
			return "contains";
		}
		
		@Override
		public String getLabel() {
			return "contains";
		}
		
		@Override
		public String getCliShort() {
			return "CON";
		}
	},
	contains_only {
		@Override
		public String toString() {
			return "contains_only";
		}
		
		@Override
		public String getLabel() {
			return "contains only";
		}
		
		@Override
		public String getCliShort() {
			return "COO";
		}
	},
	regex {
		@Override
		public String toString() {
			return "regex";
		}
		
		@Override
		public String getLabel() {
			return "regular expression";
		}
		
		@Override
		public String getCliShort() {
			return "REG";
		}
	},
	regex_only {
		@Override
		public String toString() {
			return "regex_only";
		}
		
		@Override
		public String getLabel() {
			return "regular expression only";
		}
		
		@Override
		public String getCliShort() {
			return "RXO";
		}
	},
	
	has_any_modification {
		@Override
		public String toString() {
			return "has_any_modification";
		}
		
		@Override
		public String getLabel() {
			return "has any modification";
		}
		
		@Override
		public String getCliShort() {
			return "HAM";
		}
	},
	has_mass {
		@Override
		public String toString() {
			return "has_mass";
		}
		
		@Override
		public String getLabel() {
			return "has mass";
		}
		
		@Override
		public String getCliShort() {
			return "MAS";
		}
	},
	has_description {
		@Override
		public String toString() {
			return "has_description";
		}
		
		@Override
		public String getLabel() {
			return "has description";
		}
		
		@Override
		public String getCliShort() {
			return "DEC";
		}
	},
	has_residue {
		@Override
		public String toString() {
			return "has_residue";
		}
		
		@Override
		public String getLabel() {
			return "has residue";
		}
		
		@Override
		public String getCliShort() {
			return "RES";
		}
	},
	is_in_all_search_engines {
		@Override
		public String toString() {
			return "is_identified_by_all_search_engines";
		}

		@Override
		public String getLabel() {
			return "Is identified by all search engines";
		}

		@Override
		public String getCliShort() {
			return "IAS";
		}
	},
	;
	
	
	/**
	 * returns the name used as the SelectItem's value
	 * @return
	 */
	public String getName() {
		return toString();
	}
	
	
	/**
	 * returns the label used for the SelectItems
	 * @return
	 */
	public String getLabel() {
		return null;
	}
	
	
	/**
	 * returns the short form for the command line
	 * @return
	 */
	public abstract String getCliShort();
	
	
	/**
	 * gets the FilterArgument given by the name, or null, if the name is not found.
	 * 
	 * @param name
	 * @return
	 */
	public static FilterComparator getFilterComparatorByName(String name) {
		if (name != null) {
			for (FilterComparator comp : values()) {
				if (name.equals(comp.getName())) {
					return comp;
				}
			}
		}
		return null;
	}
	
	
	/**
	 * gets the FilterArgument given by the CLI short, or null, if the short is
	 * not found.
	 * 
	 * @return
	 */
	public static FilterComparator getFilterComparatorByCLI(String cliShort) {
		if (cliShort != null) {
			for (FilterComparator comp : values()) {
				if (cliShort.equals(comp.getCliShort())) {
					return comp;
				}
			}
		}
		return null;
	}
	
	
	/**
	 * Builds a regex to extract the comparator from the command line
	 * @return
	 */
	public static String getComparatorRegexes() {
		StringBuilder comparatorRegex = new StringBuilder();
		// build the regex for the comparators
		for (FilterComparator fc : FilterComparator.values()) {
			if (comparatorRegex.length() > 0) {
				comparatorRegex.append("|");
			}
			comparatorRegex.append(fc.getCliShort());
		}
		
		return comparatorRegex.toString();
	}
}
