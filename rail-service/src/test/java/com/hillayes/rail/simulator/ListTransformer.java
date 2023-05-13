package com.hillayes.rail.simulator;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.hillayes.rail.model.PaginatedList;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;

import static com.hillayes.rail.utils.TestData.toJson;

@Slf4j
class ListTransformer<T> extends ResponseDefinitionTransformer {
    private final String name;

    // a live list of stubs that can be modified by the simulator
    private Map<String, EntityStubs<T>> list;

    ListTransformer(String name, Map<String, EntityStubs<T>> list) {
        this.name = name;
        this.list = list;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }

    @Override
    public ResponseDefinition transform(Request request,
                                        ResponseDefinition responseDefinition,
                                        FileSource files,
                                        Parameters parameters) {
        return new ResponseDefinitionBuilder()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(toJson(sublist(request, list)))
            .build();
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
    private PaginatedList<T> sublist(Request request, Map<String, EntityStubs<T>> list) {
        QueryParameter offsetParam = request.queryParameter("offset");
        int offset = offsetParam.isPresent()
            ? Math.max(0, Integer.parseInt(offsetParam.firstValue())) : 0;

        QueryParameter limitParam = request.queryParameter("limit");
        int limit = limitParam.isPresent()
            ? Math.max(0, Integer.parseInt(limitParam.firstValue())) : Integer.MAX_VALUE;

        log.trace("Getting sublist [offset: {}, limit: {}, size: {}, url: {}]",
            offset, limit, list.size(), request.getUrl());

        PaginatedList<T> response = new PaginatedList<>();
        response.count = list.size();
        response.next = null;
        response.previous = null;
        response.results = ((limit == 0) || (offset >= list.size())) ? Collections.emptyList() :
            list.values().stream()
                .map(EntityStubs::getEntity)
                .sorted()
                .skip(offset)
                .limit(limit)
                .toList();

        return response;
    }
}

