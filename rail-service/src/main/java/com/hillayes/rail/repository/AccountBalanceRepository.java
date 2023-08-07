package com.hillayes.rail.repository;

import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.rail.domain.AccountBalance;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AccountBalanceRepository extends RepositoryBase<AccountBalance, UUID> {
    public List<AccountBalance> findByAccountIdAndReferenceDate(UUID accountId, LocalDate referenceDate) {
        return find("accountId = :accountId AND referenceDate = :referenceDate",
            Parameters
                .with("accountId", accountId)
                .and("referenceDate", referenceDate))
            .list();
    }

    public Optional<AccountBalance> findFirstByAccountIdOrderByReferenceDateDesc(UUID accountId) {
        return find("accountId = :accountId",
            Sort.by("referenceDate", Sort.Direction.Descending),
            Parameters.with("accountId", accountId))
            .firstResultOptional();
    }
}
