package com.hillayes.sim.nordigen;

import com.hillayes.nordigen.model.PaginatedList;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;

public abstract class AbstractEndpoint {
    public LocalDate localDateFromString(String value){
        try {
            if ((value != null) && (!value.isBlank())) {
                return LocalDate.parse(value);
            }
        } catch (DateTimeParseException e) {
        }
        return null;
    }

    /**
     * Returns a sublist of the given list, based on the "limit" and "offset".
     *
     * @param offset the offset into the list from which results will be taken.
     * @param limit the max number of results to be returned.
     * @param list the list from which the sub-list is to be extracted.
     * @param <T> the type of the list elements.
     * @return the extracted sub-list.
     */
    protected <T> PaginatedList<T> sublist(int offset, int limit, Collection<T> list) {
        PaginatedList<T> response = new PaginatedList<>();
        response.count = list.size();
        response.next = null;
        response.previous = null;
        response.results = ((limit == 0) || (offset >= list.size())) ? Collections.emptyList() :
            list.stream()
                .sorted()
                .skip(offset)
                .limit(limit)
                .toList();

        return response;
    }
}
