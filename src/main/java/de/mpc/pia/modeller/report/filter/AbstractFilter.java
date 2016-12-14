package de.mpc.pia.modeller.report.filter;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.tools.unimod.UnimodParser;

/**
 * The abstract class of the filters, which are widely used in PIA. Most filters
 * should be instantiated by the {@link RegisteredFilters}, only scores and
 * TopIdentificationFilters have their own classes.
 *
 * @author julian
 *
 */
public abstract class AbstractFilter {

    /** the used comparator */
    private FilterComparator comparator;

    /** whether this filter is negated */
    private boolean negate;

    /** the represented filter */
    private RegisteredFilters filter;


    public AbstractFilter(FilterComparator arg, RegisteredFilters filter, boolean negate) {
        this.comparator = arg;
        this.filter = filter;
        this.negate = negate;
    }


    /**
     * Return the {@link RegisteredFilters} this filter is based on
     */
    public final RegisteredFilters getRegisteredFilter() {
        return filter;
    }


    /**
     * returns the machine readable name of the filter
     */
    public final String getShortName() {
        return filter.getShortName();
    }


    /**
     * returns the human readable long name of the filter
     */
    public final String getName() {
        return filter.getLongName();
    }


    /**
     * returns the name, which should be displayed at a filter list
     */
    public final String getFilteringListName() {
        return filter.getFilteringListName();
    }


    /**
     * returns the value, against which is filtered
     */
    public abstract Object getFilterValue();


    /**
     * returns the type of the filter, i.e. the kind of comparison
     */
    public final FilterType getFilterType() {
        return filter.getFilterType();
    }


    /**
     * returns, whether this filter is negating or not
     */
    public final boolean getFilterNegate() {
        return negate;
    }


    public final FilterComparator getFilterComparator() {
        return comparator;
    }


    /**
     * Returns the value of the Object o, which will be used for filtering. E.g.
     * it returns the actual numerical value for the "charge" of a PSM, if the
     * charge should be filtered.
     *
     * @param obj the object, containing the variable which should be filtered
     *
     * @return the value of the object's variable
     */
    public Object getObjectsValue(Object obj) {
        return filter.getObjectsValue(obj);
    }


    /**
     * Returns, whether the class of the given object is valid for an instance
     * of this filter.
     *
     * @param obj
     */
    public boolean supportsClass(Object obj) {
        return filter.supportsClass(obj);
    }


    /**
     * compares the given object and the filter value with the filter comparator
     * and thus returns, whether the object satisfies the filter.
     *
     * @param o
     */
    @SuppressWarnings("unchecked")
    public boolean satisfiesFilter(Object o, Long fileID) {
        Object objValue = getObjectsValue(o);

        if (filter.valueNeedsFileRefinement()) {
            objValue = filter.doFileRefinement(fileID, objValue);
        }

        if (objValue != null) {
            switch (getFilterType()) {
                case bool:
                    if (objValue instanceof Boolean) {
                        return satisfiesBooleanFilter((Boolean) objValue);
                    } else if (objValue instanceof Collection<?>) {
                        for (Object obj : (Collection<?>) objValue) {
                            // if any of the objects in the collection does not satisfy the filter or is not numerical, return false
                            if (obj instanceof Boolean) {
                                if (!satisfiesBooleanFilter((Boolean) obj)) {
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
                        return satisfiesNumericalFilter((Number) objValue);
                    } else if (objValue instanceof Collection<?>) {
                        for (Object obj : (Collection<?>) objValue) {
                            // if any of the objects in the collection does not satisfy the filter or is not numerical, return false
                            if (obj instanceof Number) {
                                if (!satisfiesNumericalFilter((Number) obj)) {
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
                        return satisfiesLiteralFilter((String) objValue);
                    } else if (objValue instanceof Collection<?>) {
                        for (Object obj : (Collection<?>) objValue) {
                            // if any of the objects in the collection does not satisfy the filter or is no String, return false
                            if (obj instanceof String) {
                                if (!satisfiesLiteralFilter((String) obj)) {
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
                    return objValue instanceof List<?> && satisfiesLiteralListFilter((List<String>) objValue);
// TODO: throw exception or something

                case modification:
                    return objValue instanceof List<?> && satisfiesModificationFilter((List<Modification>) objValue);
// TODO: throw exception or something
                default:
                    return false;
            }
        }

        return false;
    }


    /**
     * checks whether the given Boolean satisfies a boolean filter
     *
     * @param o Comparator Object
     */
    private boolean satisfiesBooleanFilter(Boolean o) {
        switch (getFilterComparator()) {
        case equal:
            return getFilterNegate() ^ getFilterValue().equals(o);

        default:
            return false;
        }
    }


    /**
     * checks whether the given Number satisfies a numerical filter
     *
     * @param o
     */
    private boolean satisfiesNumericalFilter(Number o) {
        boolean retVal;

        switch (getFilterComparator()) {
        case less:
            retVal = getFilterNegate() ^ (o.doubleValue() < ((Number)getFilterValue()).doubleValue());
            break;

        case less_equal:
            retVal = getFilterNegate() ^ (o.doubleValue() <= ((Number)getFilterValue()).doubleValue());
            break;

        case equal:
            retVal = getFilterNegate() ^ getFilterValue().equals(o);
            break;

        case greater_equal:
            retVal = getFilterNegate() ^ (o.doubleValue() >= ((Number)getFilterValue()).doubleValue());
            break;

        case greater:
            retVal = getFilterNegate() ^ (o.doubleValue() > ((Number)getFilterValue()).doubleValue());
            break;

        default:
            retVal =  false;
        }

        return retVal;
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

        default:
            return false;
        }
    }


    /**
     * checks whether the given String satisfies a literal list filter
     *
     * @param o
     * @return
     */
    private boolean satisfiesLiteralListFilter(List<String> o) {
        switch (getFilterComparator()) {
        case contains:
            // check, if the list contains the given string
            boolean contains = false;
            if (o != null) {
                for (String objStr : o) {
                    if (objStr.equals(getFilterValue())) {
                        contains = true;
                        break;
                    }
                }
            }
            return getFilterNegate() ^ contains;

        case contains_only:
            // check, if the list contains only the given string (maybe multiple times)
            boolean containsOnly = false;

            if ((o != null) && !o.isEmpty()
                    && o.get(0).equals(getFilterValue())) {
                // ok, the first one is our string
                containsOnly = true;
                // but are all the others?
                for (String objStr : o) {
                    if (!objStr.equals(getFilterValue())) {
                        containsOnly = false;
                        break;
                    }
                }
            }
            return getFilterNegate() ^ containsOnly;

        case regex:
            // check, if the list contains the given regex
            boolean contains_regex = false;
            Pattern regexP = Pattern.compile((String)getFilterValue());

            if (o != null) {
                for (String objStr : o) {
                    if (regexP.matcher(objStr).matches()) {
                        contains_regex = true;
                        break;
                    }
                }
            }
            return getFilterNegate() ^ contains_regex;

        case regex_only:
            // check, if the list contains only the given regex (maybe multiple times)
            boolean contains_only_regex = false;
            Pattern regexOnlyP = Pattern.compile((String)getFilterValue());

            if ((o != null) && (!o.isEmpty())
                    && regexOnlyP.matcher(o.get(0)).matches()) {
                // ok, the first one is our string
                contains_only_regex = true;
                // but are all the others?
                for (String objStr : o) {
                    if (!regexOnlyP.matcher(objStr).matches()) {
                        contains_only_regex = false;
                        break;
                    }
                }
            }
            return getFilterNegate() ^ contains_only_regex;

        default:
            return false;
        }
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
            if ((o != null) && (!o.isEmpty())) {
                has_any_modification = true;
            }
            return getFilterNegate() ^ has_any_modification;

        case has_description:
            // check, if the list of modifications has the given description
            boolean has_description = false;
            if (o != null) {
                for (Modification mod : o) {

                    if ((mod.getDescription() != null) &&
                            (mod.getDescription().equals(getFilterValue()))) {
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

                        if (Math.abs(mod.getMass() - mass) <= UnimodParser.UNIMOD_MASS_TOLERANCE) {
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

        default:
            return false;
        }
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
