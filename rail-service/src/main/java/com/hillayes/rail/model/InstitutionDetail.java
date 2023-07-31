package com.hillayes.rail.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class InstitutionDetail extends Institution {
    @JsonProperty("supported_features")
    public List<String> supportedFeatures;
}
