package com.hillayes.sim.nordigen;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.hillayes.commons.json.MapperFactory;
import com.hillayes.rail.model.PaginatedList;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public abstract class AbstractResponseTransformer extends ResponseDefinitionTransformer {
    protected static final ObjectMapper jsonMapper = MapperFactory.defaultMapper();

    abstract public void register(WireMock wireMockClient);

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }

    protected static String toJson(Object object) {
        try {
            return jsonMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return jsonMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected Optional<String> getPathElement(String path, int index) {
        String[] elements = path.split("/");
        if ((index < 0) || (index >= elements.length)) {
            return Optional.empty();
        }

        return Optional.of(elements[index]);
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

    protected Optional<LocalDate> getQueryDate(Request request, String paramName) {
        return getQueryString(request, paramName)
            .map(QueryParameter::firstValue)
            .map(LocalDate::parse);
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

    protected ResponseDefinition errorResponse(ErrorResponse response,
                                               ResponseDefinition responseDefinition) {
        return ResponseDefinitionBuilder.like(responseDefinition)
            .withStatus(response.statusCode)
            .withBody(toJson(response))
            .build();
    }

    protected ResponseDefinition unsupportedMethod(Request request,
                                                   ResponseDefinition responseDefinition) {
        ErrorResponse error = new ErrorResponse(400, "Unsupported method",
            "This endpoint does not support the " + request.getMethod() + " method.");
        return errorResponse(error, responseDefinition);
    }

    protected ResponseDefinition notFound(Request request,
                                          ResponseDefinition responseDefinition) {
        ErrorResponse error = new ErrorResponse(404, "Not found", "Not found.");
        return errorResponse(error, responseDefinition);
    }

    @AllArgsConstructor
    public static class ErrorResponse {
        @JsonProperty("status_code")
        public int statusCode;

        @JsonProperty("summary")
        public String summary;

        @JsonProperty("detail")
        public String detail;

    }
}
