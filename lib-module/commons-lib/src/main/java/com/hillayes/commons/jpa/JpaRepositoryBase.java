package com.hillayes.commons.jpa;

import com.hillayes.commons.annotation.AnnotationUtils;
import jakarta.inject.Inject;
import jakarta.persistence.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A base class for JPA repositories that provides some common functionality.
 *
 * @param <Entity>
 * @param <Id>
 */
public abstract class JpaRepositoryBase<Entity, Id> {
    @Inject
    EntityManager entityManager;

    private final Class<Entity> clazz;
    private final String entityName;

    protected JpaRepositoryBase(Class<Entity> clazz) {
        this.clazz = clazz;

        jakarta.persistence.Entity entity = AnnotationUtils.getFirstAnnotation(clazz, jakarta.persistence.Entity.class)
            .orElseThrow(() -> new IllegalArgumentException("Class " + clazz.getName() + " is not a JPA entity"));
        entityName = entity.name().isBlank() ? clazz.getSimpleName() : entity.name();
    }

    public void flush() {
        entityManager.flush();
    }

    public Entity save(Entity entity) {
        entityManager.persist(entity);
        return entity;
    }

    public Entity saveAndFlush(Entity entity) {
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }

    public void saveAll(Iterable<Entity> entities) {
        entities.forEach(entityManager::persist);
    }

    public void delete(Entity entity) {
        entityManager.remove(entity);
    }

    public Entity findById(Id id) {
        return entityManager.find(clazz, id);
    }

    public Optional<Entity> findByIdOptional(Id id) {
        return Optional.ofNullable(findById(id));
    }

    public Optional<Entity> lock(Id id) {
        return Optional.ofNullable(entityManager.find(clazz, id, LockModeType.PESSIMISTIC_WRITE));
    }

    public List<Entity> listAll() {
        return entityManager.createQuery("select e from " + entityName + " e", clazz)
            .getResultList();
    }

    public List<Entity> listAll(String query, Object param) {
        TypedQuery<Entity> q = entityManager.createQuery(query, clazz)
            .setParameter(1, param);
        return q.getResultList();
    }

    public List<Entity> listAll(String query, Map<String, Object> params) {
        TypedQuery<Entity> query1 = entityManager.createQuery(query, clazz);
        params.forEach(query1::setParameter);
        return query1.getResultList();
    }

    /**
     * Returns the identified page of results from the given query.
     *
     * @param sql the query to retrieve the full results.
     * @param pageNumber the, zero-based, page number of the requested page.
     * @param pageSize the max number of elements to be returned.
     * @return the requested page of results.
     */
    public Page<Entity> findByPage(String sql, int pageNumber, int pageSize) {
        Query query = entityManager.createQuery(sql);
        List<Entity> list = query
            .setFirstResult(pageNumber * pageSize)
            .setMaxResults(pageSize)
            .getResultList();

        long count = (pageSize > list.size())
            ? ((long) pageSize * pageNumber) + list.size()
            : entityManager.createQuery("SELECT COUNT(*) FROM (" + sql + ")").getFirstResult();

        return new Page<>(list, count, pageNumber, pageSize);
    }

    public Page<Entity> pageAll(String sql, int pageNumber, int pageSize, Object param) {
        Query query = entityManager.createQuery(sql)
            .setParameter(1, param);

        List<Entity> list = query
            .setFirstResult(pageNumber * pageSize)
            .setMaxResults(pageSize)
            .getResultList();

        long count = (pageSize > list.size())
            ? ((long) pageSize * pageNumber) + list.size()
            : entityManager.createQuery("SELECT COUNT(*) FROM (" + sql + ")").getFirstResult();

        return new Page<>(list, count, pageNumber, pageSize);
    }
}
