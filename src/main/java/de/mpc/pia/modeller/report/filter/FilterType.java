package de.mpc.pia.modeller.report.filter;

import java.util.ArrayList;
import java.util.List;

public enum FilterType {
	/**
	 * the boolean filter type compares only booleans
	 */
	bool {
		@Override
		public List<FilterComparator> getAvailableComparators() {
			List<FilterComparator> arguments = new ArrayList<>();
			
			arguments.add(FilterComparator.equal);
			
			return arguments;
		}
	},
	/**
	 * the numerical filter type compares numbers for filtering
	 */
	numerical {
		@Override
		public List<FilterComparator> getAvailableComparators() {
			List<FilterComparator> arguments = new ArrayList<>();
			
			arguments.add(FilterComparator.less);
			arguments.add(FilterComparator.less_equal);
			arguments.add(FilterComparator.equal);
			arguments.add(FilterComparator.greater_equal);
			arguments.add(FilterComparator.greater);
			
			return arguments;
		}

	},
	/**
	 * the literal filter type compares strings for filtering
	 */
	literal {
		@Override
		public List<FilterComparator> getAvailableComparators() {
			List<FilterComparator> arguments = new ArrayList<>();
			
			arguments.add(FilterComparator.equal);
			arguments.add(FilterComparator.contains);
			arguments.add(FilterComparator.regex);
			
			return arguments;
		}
	},
	/**
	 * the literal_list filter type compares a list of literals for filtering
	 */
	literal_list {
		@Override
		public List<FilterComparator> getAvailableComparators() {
			List<FilterComparator> arguments = new ArrayList<>();
			
			arguments.add(FilterComparator.contains);
			arguments.add(FilterComparator.contains_only);
			arguments.add(FilterComparator.regex);
			arguments.add(FilterComparator.regex_only);
			
			return arguments;
		}
		
	},
	/**
	 * the modification filter type has special arguments, only for modification
	 * filtering
	 */
	modification {
		@Override
		public List<FilterComparator> getAvailableComparators() {
			List<FilterComparator> arguments = new ArrayList<>();
			
			arguments.add(FilterComparator.has_any_modification);
			arguments.add(FilterComparator.has_description);
			arguments.add(FilterComparator.has_mass);
			arguments.add(FilterComparator.has_residue);
			
			return arguments;
		}
		
	},
	;
	
	/**
	 * Returns a {@link List} of the available {@link FilterComparator}s for
	 * this {@link FilterType}.
	 * @return
	 */
	public List<FilterComparator> getAvailableComparators() {
		return new ArrayList<>();
	}

}
