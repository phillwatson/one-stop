package com.hillayes.rail.simulator;

import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.http.Request;
import com.hillayes.rail.model.PaginatedList;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractResponseTransformer extends ResponseDefinitionTransformer {
    private final String name;
    private final boolean global;

    AbstractResponseTransformer(String name) {
        this(name, false);
    }

    AbstractResponseTransformer(String name, boolean global) {
        this.name = name;
        this.global = global;
    }

    @Override
    public String getName() {
        return name == null ? this.getClass().getSimpleName() : name;
    }

    @Override
    public boolean applyGlobally() {
        return global;
    }

    protected String getIdFromPath(String path, int index) {
        return path.split("/")[index];
    }

    protected Optional<QueryParameter> getQueryString(Request request, String paramName) {
        QueryParameter param = request.queryParameter(paramName);
        return param.isPresent() ? Optional.of(param) : Optional.empty();
    }

    protected Optional<Boolean> getQueryBoolean(Request request, String paramName) {
        return getQueryString(request, paramName)
            .map(QueryParameter::firstValue)
            .map(Boolean::parseBoolean);
    }

    /**
     * Examines the query parameters of the given request and, based on the "limit"
     * and "offset" parameters it finds, returns a sublist of the given list.
     *
     * @param request the request in which "limit" and "offset" parameters may be present.
     * @param list the list from which the sub-list is to be extracted.
     * @param <T> the type of the list elements.
     * @return the extracted sub-list.
     */
    protected <T> PaginatedList<T> sublist(Request request, Collection<T> list) {
        QueryParameter offsetParam = request.queryParameter("offset");
        int offset = offsetParam.isPresent()
            ? Math.max(0, Integer.parseInt(offsetParam.firstValue())) : 0;

        QueryParameter limitParam = request.queryParameter("limit");
        int limit = limitParam.isPresent()
            ? Math.max(0, Integer.parseInt(limitParam.firstValue())) : Integer.MAX_VALUE;

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
