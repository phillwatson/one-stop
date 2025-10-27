package com.hillayes.shares.repository;

import com.hillayes.commons.correlation.Correlation;
import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.shares.domain.PriceHistory;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.domain.SharePriceResolution;
import com.hillayes.shares.errors.DatabaseException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
@Slf4j
public class PriceHistoryRepository extends RepositoryBase<PriceHistory, UUID> {
    /**
     * Defines the maximum size of the batches in which PriceHistory records are
     * inserted.
     */
    private int insertBatchSize;

    /**
     * Identifies the DB schema in which PriceHistory records are held.
     */
    private String dbSchema;

    // a utility method to map PriceHistory records to native SQL statements
    private final PriceHistorySqlMapper sqlMapper;

    public PriceHistoryRepository(
        @ConfigProperty(name = "one-stop.shares.share-price.insert-batch-size", defaultValue = "50")
        int insertBatchSize,
        @ConfigProperty(name ="quarkus.hibernate-orm.database.default-schema", defaultValue = "shares")
        String dbSchema,
        PriceHistorySqlMapper sqlMapper
    ) {
        this.insertBatchSize = insertBatchSize;
        this.dbSchema = dbSchema;
        this.sqlMapper = sqlMapper;
    }

    // a pool of virtual threads on which batches can be persisted
    private static final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicInteger pendingBatchCount = new AtomicInteger();

    public int getInsertBatchSize() {
        return insertBatchSize;
    }

    /**
     * Tests whether the insertion of any batches of PriceHistory records are
     * still in-flight.
     *
     * @return true if any batches are still in-flight, false otherwise.
     */
    public boolean isBatchPending() {
        return pendingBatchCount.get() > 0;
    }

    /**
     * Inserts the given batch of PriceHistory records. The batch will be split
     * into smaller batches (to larger than the configured size).
     *
     * Any duplicates in the given batch will be ignored.
     *
     * @param batch the collection of PriceHistory records to be inserted.
     */
    public void saveBatch(Collection<PriceHistory> batch) {
        log.debug("Saving batch of share prices [size: {}]", batch.size());
        if (batch.isEmpty()) {
            return;
        }

        // split the whole into smaller batches
        Spliterator<PriceHistory> split = batch.spliterator();
        List<Spliterator<PriceHistory>> batches = new ArrayList<>();
        batches.add(split);
        while (split.estimateSize() > insertBatchSize) {
            batches.addAll(batches.stream()
                .map(Spliterator::trySplit)
                .toList());
        }

        if (batches.size() > 1) {
            log.debug("Split batch into sub-batches [count: {}]", batches.size());
        }

        // save each batch individually
        batches.forEach(this::saveBatch);
    }

    /**
     * Inserts the given batch of PriceHistory records. The insert will be
     * performed on a virtual thread. This allows multiple threads to insert
     * small batches of records, providing better performance.
     *
     * Any duplicates in the given batch will be ignored.
     *
     * @param batch the collection of PriceHistory records to be inserted.
     */
    public void saveBatch(Spliterator<PriceHistory> batch) {
        int size = (int)batch.estimateSize();
        if (size == 0) {
            return;
        }

        // copy the record iteration to a new list
        // this avoids issues should the original collection be amended
        // it also allows the batch to be traversed multiple times
        List<PriceHistory> asList = new ArrayList<>(size);
        batch.forEachRemaining(asList::add);

        // queue a threaded task to insert the records
        String correlationId = Correlation.getCorrelationId().orElse(null);
        pendingBatchCount.incrementAndGet();
        executorService.submit(() -> Correlation.run(
            correlationId,
            () -> {
                try {
                    _saveBatch(asList);
                } catch (Exception e) {
                    PriceHistory.PrimaryKey from = asList.getFirst().getId();
                    PriceHistory.PrimaryKey to = asList.getLast().getId();
                    if (from.getDate().isAfter(to.getDate())) {
                        PriceHistory.PrimaryKey x = from;
                        from = to;
                        to = x;
                    }
                    log.warn("Unexpected exception saving prices [shareId: {}, from: {}, to: {}]",
                        from.getShareIndexId(), from.getDate(), to.getDate(), e);
                } finally {
                    pendingBatchCount.decrementAndGet();
                }
            }));
    }

    /**
     * Performs the insertion of the given collection of PriceHistory records.
     * The insertion is performed using a native SQL insert statement with the
     * clause "ON CONFLICT DO NOTHING". This allows multiple threads to attempt to
     * insert the same records without throwing a constraint exception. This is
     * only viable as the PriceHistory records carry historic and immutable
     * data.
     *
     * This method is only intended to be called from a virtual thread, initiated
     * from within this class. As such it should be considered private, but cannot
     * be marked as private as the annotations it carries will not take effect.
     *
     * @param batch the batch of records to be inserted.
     */
    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    @ActivateRequestContext
    public void _saveBatch(Collection<PriceHistory> batch) {
        log.debug("_saveBatch [size: {}]", batch.size());
        // construct a native SQL statement
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(dbSchema).append(".price_history ")
            .append(sqlMapper.colNames)
            .append(" VALUES ");

        // add col place-holders for each row to be inserted
        for (int i = 0; i < batch.size(); i++) {
            if (i > 0) sql.append(',');
            sql.append(sqlMapper.colPlaceholders);
        }

        // add ON CONFLICT clause to ignore duplicate (constraint) conflicts
        sql.append(" ON CONFLICT DO NOTHING");

        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                // map each record to the SQL statement place-holders
                AtomicInteger index = new AtomicInteger();
                batch.forEach(entry ->
                    sqlMapper.map(statement, index.getAndIncrement(), entry)
                );

                // perform the batch insert
                statement.execute();
            }
        } catch (DatabaseException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    /**
     * Returns the record of the most recent DAILY share price record for the given
     * ShareIndex.
     *
     * @param shareIndex the ShareIndex whose price history is to be searched.
     * @return the record of the most recent share price, or an empty result.
     */
    public Optional<PriceHistory> getMostRecent(ShareIndex shareIndex) {
        return findFirst("id.shareIndexId = :shareIndexId AND id.resolution = :resolution",
            OrderBy.by("id.date", OrderBy.Direction.Descending),
            Map.of(
                "shareIndexId", shareIndex.getId(),
                "resolution", SharePriceResolution.DAILY)
        );
    }

    /**
     * Returns a page of PriceHistory records, in ascending date order, for
     * the given ShareIndex. The records will include only those within the given
     * date range, and be of the given resolution.
     *
     * @param shareIndex the ShareIndex for which the prices are required.
     * @param resolution the resolution at which the prices are required.
     * @param fromDate the earliest date to be included in the page (inclusive).
     * @param toDate the latest date to be included in the page (exclusive).
     * @param pageNumber the (zero based) index of the page to be returned.
     * @param pageSize the size of the page, and the maximum number of records to be returned.
     * @return the page of records, or an empty page if no records are found.
     */
    public Page<PriceHistory> listPrices(ShareIndex shareIndex,
                                         SharePriceResolution resolution,
                                         LocalDate fromDate,
                                         LocalDate toDate,
                                         int pageNumber, int pageSize) {
        return listPrices(shareIndex.getId(), resolution, fromDate, toDate, pageNumber, pageSize);
    }

    /**
     * Returns a page of PriceHistory records, in ascending date order, for
     * the identified ShareIndex. The records will include only those within the given
     * date range, and be of the given resolution.
     *
     * @param shareIndexId the identity of the ShareIndex for which the prices are required.
     * @param resolution the resolution at which the prices are required.
     * @param fromDate the earliest date to be included in the page (inclusive).
     * @param toDate the latest date to be included in the page (exclusive).
     * @param pageNumber the (zero based) index of the page to be returned.
     * @param pageSize the size of the page, and the maximum number of records to be returned.
     * @return the page of records, or an empty page if no records are found.
     */
    public Page<PriceHistory> listPrices(UUID shareIndexId,
                                         SharePriceResolution resolution,
                                         LocalDate fromDate,
                                         LocalDate toDate,
                                         int pageNumber, int pageSize) {
        return pageAll("id.shareIndexId = :shareIndexId " +
                "and id.resolution = :resolution " +
                "and id.date >= :fromDate " +
                "and id.date < :toDate",
            pageNumber, pageSize,
            OrderBy.by("id.shareIndexId").and("id.date"),
            Map.of(
                "shareIndexId", shareIndexId,
                "resolution", resolution,
                "fromDate", fromDate,
                "toDate", toDate));
    }

    /**
     * Obtains a JDBC connection from the entity manager; allowing native access
     * to the database. Used only for inserting batches of share price records.
     */
    private Connection getConnection() {
        return getEntityManager()
            .unwrap(Session.class)
            .doReturningWork(connection -> connection);
    }
}
