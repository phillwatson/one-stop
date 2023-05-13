package com.hillayes.rail.simulator;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to store the stubs for an entity. Allowing the entities to be
 * returned in a "get-list" request and, also, for the stubs to be deleted when the
 * entity is deleted.
 * @param <T> the type of the entity
 */
class EntityStubs<T> {
    private final T entity;
    private final List<StubMapping> stubs = new ArrayList<>();

    public EntityStubs(T entity) {
        this.entity = entity;
    }

    /**
     * Adds a wiremock stub to the entity.
     * @param stub the stub to be added.
     * @return this object, to allow chaining.
     */
    public EntityStubs<T> add(StubMapping stub) {
        stubs.add(stub);
        return this;
    }

    /**
     * The entity for which the stubs are stored.
     */
    public T getEntity() {
        return entity;
    }

    /**
     * The wiremock stubs for the entity.
     */
    public List<StubMapping> getStubs() {
        return stubs;
    }
}
