package de.mpc.pia.modeller.report.filter;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.tools.PIAConstants;

public abstract class AbstractFilter {
	
	protected FilterComparator comparator;
	
	protected boolean negate;
	
	
	
	/**
	 * returns the machine readable name of the filter
	 * @return
	 */
	public abstract String getShortName();
	
	
	/**
	 * returns the human readable long name of the filter
	 * @return
	 */
	public abstract String getName();
	
	
	/**
	 * returns the human readable name, which includes the filtering explanation
	 * @return
	 */
	public abstract String getFilteringName();
	
	
	/**
	 * returns the value, against which is filtered
	 * @return
	 */
	public abstract Object getFilterValue();
	
	
	/**
	 * returns the type of the filter, i.e. the kind of comparison
	 * @return
	 */
	public abstract FilterType getFilterType();
	
	
	/**
	 * returns, whether this filter is negating or not
	 * @return
	 */
	public boolean getFilterNegate() {
		return negate;
	}
	
	
	/**
	 * getter for the filter's comparator
	 * @return
	 */
	public FilterComparator getFilterComparator() {
		return comparator;
	}
	
	
	/**
	 * returns the value of the Object o, which will be used for filtering.
	 * 
	 * @param o
	 * @return
	 */
	public abstract Object getObjectsValue(Object o);
	
	
	/**
	 * returns true, if the objects need file refinement (e.g. filtering the
	 * accessions).
	 * 
	 * @return
	 */
	public boolean valueNeedsFileRefinement() {
		return false;
	}
	
	
	/**
	 * performs the file refinement an the given object and returns the
	 * refined object.
	 * 
	 * @return
	 */
	public Object doFileRefinement(Long fileID, Object o) {
		return null;
	}
	
	
	/**
	 * Returns, whether a new inference on the level, where this filter is
	 * applied, is necessary. As the reports are for each file separately, only
	 * the file's inference must be performed, where this filter was added. If
	 * false is returned, the result will be valid simply by filering the
	 * current report list.
	 * 
	 * @param c one of the objects, the filter is performed on
	 * @return
	 */
	public boolean newInferenceFor(Class c) {
		return false;
	}
	
	
	/**
	 * Returns, whether the class of the given object is valid for an instance
	 * of this filter.
	 * 
	 * @param c
	 * @return
	 */
	public abstract boolean supportsClass(Object c);
	
	
	/**
	 * compares the given object and the filter value with the filter comparator
	 * and thus returns, whether the object satisfies the filter.
	 * 
	 * @param o
	 * @return
	 */
	public boolean satisfiesFilter(Object o, Long fileID) {
		Object objValue = getObjectsValue(o);
		
		if (valueNeedsFileRefinement()) {
			objValue = doFileRefinement(fileID, objValue);
		}
		
		if (objValue != null) {
			
			switch (getFilterType()) {
			case bool:
				if (objValue instanceof Boolean) {
					return satisfiesBooleanFilter((Boolean)objValue);
				} else if (objValue instanceof Collection<?>) {
					for (Object obj : (Collection<?>)objValue) {
						// if any of the objects in the collection does not satisfy the filter or is not numerical, return false 
						if (obj instanceof Boolean) {
							if (!satisfiesBooleanFilter((Boolean)obj)) {
								return false;
							}
						} else {
							return false;
						}
					}
					// all objects in collection satisfy the filter, return true
					return true;
				} else {
					// TODO: throw exception or something
					return false;
				}
			
			case numerical:
				if (objValue instanceof Number) {
					return satisfiesNumericalFilter((Number)objValue);
				} else if (objValue instanceof Collection<?>) {
					for (Object obj : (Collection<?>)objValue) {
						// if any of the objects in the collection does not satisfy the filter or is not numerical, return false 
						if (obj instanceof Number) {
							if (!satisfiesNumericalFilter((Number)obj)) {
								return false;
							}
						} else {
							return false;
						}
					}
					// all objects in collection satisfy the filter, return true
					return true;
				} else {
					// TODO: throw exception or something
					return false;
				}
				
			case literal:
				if (objValue instanceof String) {
					return satisfiesLiteralFilter((String)objValue);
				} else if(objValue instanceof Collection<?>) {
					for (Object obj : (Collection<?>)objValue) {
						// if any of the objects in the collection does not satisfy the filter or is no String, return false 
						if (obj instanceof String) {
							if (!satisfiesLiteralFilter((String)obj)) {
								return false;
							}
						} else {
							return false;
						}
					}
					// all objects in collection satisfy the filter, return true
					return true;
				} else {
					// TODO: throw exception or something
					return false;
				}
				
			case literal_list:
				if (objValue instanceof List<?>) {
					return satisfiesLiteralListFilter((List<String>)objValue);
				} else {
					// TODO: throw exception or something
					return false;
				}
				
			case modification:
				if (objValue instanceof List<?>) {
					return satisfiesModificationFilter((List<Modification>)objValue);
				} else {
					// TODO: throw exception or something
					return false;
				}
			}
			
		}
		
		return false;
	}
	
	
	/**
	 * checks whether the given Boolean satisfies a boolean filter
	 * 
	 * @param o
	 * @return
	 */
	private boolean satisfiesBooleanFilter(Boolean o) {
		switch (getFilterComparator()) {
		case equal:
			return getFilterNegate() ^ getFilterValue().equals(o);
			
		default:
			// TODO: throw exception or something
			break;
		}
		
		return false;
	}
	
	
	/**
	 * checks whether the given Number satisfies a numerical filter
	 * 
	 * @param o
	 * @return
	 */
	private boolean satisfiesNumericalFilter(Number o) {
		switch (getFilterComparator()) {
		case less:
			return getFilterNegate() ^ (o.doubleValue() < ((Number)getFilterValue()).doubleValue());
			
		case less_equal:
			return getFilterNegate() ^ (o.doubleValue() <= ((Number)getFilterValue()).doubleValue());
			
		case equal:
			return getFilterNegate() ^ getFilterValue().equals(o);
			
		case greater_equal:
			return getFilterNegate() ^ (o.doubleValue() >= ((Number)getFilterValue()).doubleValue());
			
		case greater:
			return getFilterNegate() ^ (o.doubleValue() > ((Number)getFilterValue()).doubleValue());
			
		default:
			// TODO: throw exception or something
			break;
		}
		
		return false;
	}
	
	
	/**
	 * checks whether the given String satisfies a literal filter
	 * 
	 * @param o
	 * @return
	 */
	private boolean satisfiesLiteralFilter(String o) {
		switch (getFilterComparator()) {
		case equal:
			return getFilterNegate() ^ (o.equals(getFilterValue()));
			
		case contains:
			return getFilterNegate() ^ (o.contains((String)getFilterValue()));
			
		case regex:
			Matcher m = Pattern.compile((String)getFilterValue()).matcher(o);
			return getFilterNegate() ^ m.matches();
		}
		
		
		return false;
	}
	
	
	/**
	 * checks whether the given String satisfies a literal list filter
	 * 
	 * @param o
	 * @return
	 */
	private boolean satisfiesLiteralListFilter(List<String> o) {
		switch (getFilterComparator()) {
		case contains: {
			// check, if the list contains the given string
			boolean contains = false;
			if (o != null) {
				for (String objStr : o) {
					if (objStr.equals((String)getFilterValue())) {
						contains = true;
						break;
					}
				}
			}
			return getFilterNegate() ^ contains;
		}
		
		case contains_only: {
			// check, if the list contains only the given string (maybe multiple times)
			boolean contains_only = false;
			
			if (o != null) {
				
				if (o.size() > 0) {
					if (o.get(0).equals((String)getFilterValue())) {
						// ok, the first one is our string
						contains_only = true;
						// but are all the others?
						for (String objStr : o) {
							if (!objStr.equals((String)getFilterValue())) {
								contains_only = false;
								break;
							}
						}
					}
				}
				
			}
			return getFilterNegate() ^ contains_only;
		}
			
		case regex: {
			// check, if the list contains the given regex
			boolean contains_regex = false;
			Pattern p = Pattern.compile((String)getFilterValue());
			
			if (o != null) {
				for (String objStr : o) {
					if (p.matcher(objStr).matches()) {
						contains_regex = true;
						break;
					}
				}
			}
			return getFilterNegate() ^ contains_regex;
		}
		
		case regex_only: {
			// check, if the list contains only the given regex (maybe multiple times)
			boolean contains_only_regex = false;
			Pattern p = Pattern.compile((String)getFilterValue());
			
			if (o != null) {
				if (o.size() > 0) {
					if (p.matcher(o.get(0)).matches()) {
						// ok, the first one is our string
						contains_only_regex = true;
						// but are all the others?
						for (String objStr : o) {
							if (!p.matcher(objStr).matches()) {
								contains_only_regex = false;
								break;
							}
						}
					}
				}
			}
			return getFilterNegate() ^ contains_only_regex;
			
		}
		}
		
		
		return false;
	}
	
	
	/**
	 * checks whether the given String satisfies a literal list filter
	 * 
	 * @param o
	 * @return
	 */
	private boolean satisfiesModificationFilter(List<Modification> o) {
		switch (getFilterComparator()) {
		case has_any_modification:
			boolean has_any_modification = false;
			if ((o != null) && (o.size() > 0)) {
				has_any_modification = true;
			}
			return getFilterNegate() ^ has_any_modification;
		
		case has_description:
			// check, if the list of modifications has the given description
			boolean has_description = false;
			if (o != null) {
				for (Modification mod : o) {
					
					if ((mod.getDescription() != null) &&
							(mod.getDescription().equals((String)getFilterValue()))) {
						has_description = true;
						break;
					}
					
				}
			}
			return getFilterNegate() ^ has_description;
		
		case has_mass:
			// check, if the list of modifications has the given mass
			boolean has_mass = false;
			if (o != null) {
				for (Modification mod : o) {
					try {
						Double mass = Double.parseDouble((String)getFilterValue());
						
						if (Math.abs(mod.getMass() - mass) <= PIAConstants.unimod_mass_tolerance) {
							has_mass = true;
							break;
						}
					} catch (NumberFormatException e) {
						// TODO: give the user feedback of wrong number format
						return false;
					}
				}
			}
			return getFilterNegate() ^ has_mass;
		
		case has_residue:
			// check, if the list of modifications has the given residue (but modification does not have to be on this residue)
			boolean has_residue = false;
			if (o != null) {
				for (Modification mod : o) {
					
					if ((mod.getResidue() != null) &&
							mod.getResidue().toString().startsWith((String)getFilterValue())) {
						has_residue = true;
						break;
					}
				}
			}
			return getFilterNegate() ^ has_residue;
		}
		
		return false;
	}
	
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder(getShortName());
		
		if (getFilterNegate()) {
			str.append(" not");
		}
		
		str.append(" ");
		str.append(getFilterComparator().toString());
		
		str.append(" ");
		str.append(getFilterValue());
		
		return str.toString();
	}
}
