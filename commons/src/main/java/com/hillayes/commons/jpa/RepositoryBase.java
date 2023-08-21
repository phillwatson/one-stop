package com.hillayes.commons.jpa;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

/**
 * Acts as a facade over the Panache Repository implementation to reflect a similar
 * interface to that provided by the JPA repository.
 *
 * @param <Entity> the type of entity to operate on
 * @param <Id> the type of the entity's identifier.
 */
@RegisterForReflection(targets = {
    // workaround: see https://github.com/quarkusio/quarkus/issues/34071
    UUID[].class,
})
public class RepositoryBase<Entity, Id> implements PanacheRepositoryBase<Entity, Id> {
    public Entity save(Entity entity) {
        persist(entity);
        return entity;
    }

    public Entity saveAndFlush(Entity entity) {
        persistAndFlush(entity);
        return entity;
    }

    public Iterable<Entity> saveAll(Iterable<Entity> entities) {
        persist(entities);
        return entities;
    }

    public List<Entity> listAll() {
        return findAll().list();
    }

    /**
     * Returns the identified page of results from the given query.
     * @param query the query to retrieve the full results.
     * @param pageNumber the, zero-based, page number of the requested page.
     * @param pageSize the max number of elements to be returned.
     * @return the requested page of results.
     */
    public Page<Entity> findByPage(PanacheQuery<Entity> query, int pageNumber, int pageSize) {
        List<Entity> list = query
            .page(pageNumber, pageSize)
            .list();

        long count = (pageSize > list.size())
            ? ((long)pageSize * pageNumber) + list.size()
            : query.count();

        return new PageImpl<>(list, PageRequest.of(pageNumber, pageSize), count);
    }
}
