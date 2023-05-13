package com.hillayes.rail.simulator;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.util.ArrayList;
import java.util.List;

public class EntityStubs<T> {
    private final T entity;
    private final List<StubMapping> stubs = new ArrayList<>();

    public EntityStubs(T entity) {
        this.entity = entity;
    }

    public EntityStubs<T> add(StubMapping stub) {
        stubs.add(stub);
        return this;
    }

    public T getEntity() {
        return entity;
    }

    public List<StubMapping> getStubs() {
        return stubs;
    }
}
