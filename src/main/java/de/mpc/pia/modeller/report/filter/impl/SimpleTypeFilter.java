package de.mpc.pia.modeller.report.filter.impl;

import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;

/**
 * Implementation of a filter, which filters by a simple value.
 *
 * @author julian
 *
 */
public class SimpleTypeFilter<T> extends AbstractFilter {

    /** the actual compared value */
    private T value;


    public SimpleTypeFilter(FilterComparator arg, RegisteredFilters filter,
            boolean negate, T value) {
        super(arg, filter, negate);
        this.value = value;
    }


    @Override
    public T getFilterValue() {
        return value;
    }
}
