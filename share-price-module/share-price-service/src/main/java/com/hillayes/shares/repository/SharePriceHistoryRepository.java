package com.hillayes.shares.repository;

import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.domain.ShareIndexResolution;
import com.hillayes.shares.domain.SharePriceHistory;
import com.hillayes.shares.errors.DatabaseException;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
@Slf4j
public class SharePriceHistoryRepository extends RepositoryBase<SharePriceHistory, UUID> {
    private static final SharePriceHistorySqlMapper sqlMapper = new SharePriceHistorySqlMapper();

    // a pool of virtual threads on which batches can be persisted
    private static final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicInteger pendingBatchCount = new AtomicInteger();

    public boolean isBatchPending() {
        return pendingBatchCount.get() > 0;
    }

    public void saveBatch(Collection<SharePriceHistory> batch) {
        log.debug("Saving batch of share prices [size: {}]", batch.size());

        // split the whole into smaller batches
        Spliterator<SharePriceHistory> split = batch.spliterator();
        List<Spliterator<SharePriceHistory>> batches = new ArrayList<>();
        batches.add(split);
        while (split.estimateSize() > 50) {
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

    public void saveBatch(Spliterator<SharePriceHistory> batch) {
        int size = (int)batch.estimateSize();
        List<SharePriceHistory> asList = new ArrayList<>(size);
        batch.forEachRemaining(asList::add);

        pendingBatchCount.incrementAndGet();
        executorService.submit(() -> {
            try {
                _saveBatch(asList);
            } finally {
                pendingBatchCount.decrementAndGet();
            }
        });
    }

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    @ActivateRequestContext
    public void _saveBatch(Collection<SharePriceHistory> batch) {
        log.debug("Concurrently saving batch of share prices [size: {}]", batch.size());
        StringBuilder sql = new StringBuilder("INSERT INTO shares.share_price_history ")
            .append(sqlMapper.colNames)
            .append(" VALUES ");
        for (int i = 0; i < batch.size(); i++) {
            if (i > 0) sql.append(',');
            sql.append(sqlMapper.colPlaceholders);
        }
        sql.append(" ON CONFLICT DO NOTHING");

        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                AtomicInteger index = new AtomicInteger();
                batch.forEach(entry ->
                    sqlMapper.map(statement, index.getAndIncrement(), entry)
                );

                statement.execute();
            }
        } catch (DatabaseException e) {
            log.warn("Unexpected exception", e);
            throw e;
        } catch (SQLException e) {
            log.warn("Unexpected exception", e);
            throw new DatabaseException(e);
        }
    }

    public Optional<SharePriceHistory> getMostRecent(ShareIndex shareIndex) {
        return findAll(Sort.by("id.date"))
            .firstResultOptional();
    }

    public Page<SharePriceHistory> listPrices(ShareIndex shareIndex,
                                              ShareIndexResolution resolution,
                                              LocalDate fromDate,
                                              LocalDate toDate,
                                              int pageNumber, int pageSize) {
        return listPrices(shareIndex.getId(), resolution, fromDate, toDate, pageNumber, pageSize);
    }

    public Page<SharePriceHistory> listPrices(UUID shareIndexId,
                                              ShareIndexResolution resolution,
                                              LocalDate fromDate,
                                              LocalDate toDate,
                                              int pageNumber, int pageSize) {
        return findByPage(find("id.shareIndexId = :shareIndexId and id.resolution = :resolution and id.date >= :fromDate and id.date <= :toDate",
            Sort.by("id.shareIndexId").and("id.date"),
            Parameters
                .with("shareIndexId", shareIndexId)
                .and("resolution", resolution)
                .and("fromDate", fromDate)
                .and("toDate", toDate)), pageNumber, pageSize);
    }

    private Connection getConnection() {
        return getEntityManager()
            .unwrap(Session.class)
            .doReturningWork(connection -> connection);
    }
}
