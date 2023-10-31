package com.hillayes.nordigen.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PaginatedList<T> {
    @JsonProperty("count")
    public Integer count;

    @JsonProperty("next")
    public String next;

    @JsonProperty("previous")
    public String previous;

    @JsonProperty("results")
    public List<T> results;
}
