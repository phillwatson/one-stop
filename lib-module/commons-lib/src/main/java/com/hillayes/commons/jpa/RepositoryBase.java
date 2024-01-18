package com.hillayes.commons.jpa;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Acts as a facade over the Panache Repository implementation to reflect a similar
 * interface to that provided by the JPA repository.
 *
 * @param <Entity> the type of entity to operate on
 * @param <Id> the type of the entity's identifier.
 */
public abstract class RepositoryBase<Entity, Id> implements PanacheRepositoryBase<Entity, Id> {
    public Optional<Entity> findFirst(String query, Object... parameters) {
        return find(query, parameters)
            .firstResultOptional();
    }

    public Optional<Entity> findFirst(String query, Map<String, Object> parameters) {
        return find(query, parameters)
            .firstResultOptional();
    }

    public Optional<Entity> findFirst(String query, OrderBy orderBy, Map<String, Object> parameters) {
        return find(query, toSort(orderBy), parameters)
            .firstResultOptional();
    }

    public Optional<Entity> lock(Id id) {
        return findByIdOptional(id, LockModeType.PESSIMISTIC_WRITE);
    }

    public List<Entity> lock(String query, int count) {
        return find(query)
            .withLock(LockModeType.PESSIMISTIC_WRITE)
            .page(0, count)
            .list();
    }

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

    @Override
    public List<Entity> listAll() {
        return findAll().list();
    }

    public List<Entity> listAll(String query, int page, int pageSize, OrderBy orderBy) {
        return find(query, toSort(orderBy))
            .page(page, pageSize)
            .list();
    }

    public List<Entity> listAll(String query, Object... parameters) {
        return find(query, parameters)
            .list();
    }

    public List<Entity> listAll(String query, Map<String,Object> parameters) {
        return find(query, parameters)
            .list();
    }

    public List<Entity> listAll(String query, OrderBy orderBy, Map<String, Object> parameters) {
        return find(query, toSort(orderBy), parameters)
            .list();
    }

    public Page<Entity> pageAll(OrderBy orderBy, int pageNumber, int pageSize) {
        return findByPage(findAll(toSort(orderBy)), pageNumber, pageSize);
    }

    public Page<Entity> pageAll(String query, int pageNumber, int pageSize, Object... parameters) {
        return findByPage(find(query, parameters), pageNumber, pageSize);
    }

    public Page<Entity> pageAll(String query, OrderBy orderBy, int pageNumber, int pageSize, Object... parameters) {
        return findByPage(find(query, toSort(orderBy), parameters), pageNumber, pageSize);
    }

    /**
     * Returns the identified page of results from the given query.
     * @param query the query to retrieve the full results.
     * @param pageNumber the, zero-based, page number of the requested page.
     * @param pageSize the max number of elements to be returned.
     * @return the requested page of results.
     */
    protected Page<Entity> findByPage(PanacheQuery<Entity> query, int pageNumber, int pageSize) {
        List<Entity> list = query
            .page(pageNumber, pageSize)
            .list();

        long count = (pageSize > list.size())
            ? ((long)pageSize * pageNumber) + list.size()
            : query.count();

        return new Page<>(list, count, pageNumber, pageSize);
    }

    /**
     * Translates the OrderBy to the panache Sort class.
     */
    private Sort toSort(OrderBy orderBy) {
        return orderBy.getColumns().stream()
            .map(col -> new Sort.Column(col.getName(), Sort.Direction.valueOf(col.getDirection().name())))
            .reduce(null,
                (sort, col) -> (sort == null)
                    ? Sort.by(col.getName(), col.getDirection())
                    : sort.and(col.getName(), col.getDirection()),
                (prev, next) -> next
            );
    }
}
