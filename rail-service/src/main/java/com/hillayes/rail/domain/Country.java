package com.hillayes.rail.domain;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(of="id")
@Builder
public class Country {
    private String id;
    private String name;
    private String flagUrl;
}
