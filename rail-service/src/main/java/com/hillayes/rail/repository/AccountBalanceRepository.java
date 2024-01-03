package com.hillayes.rail.repository;

import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.rail.domain.AccountBalance;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AccountBalanceRepository extends RepositoryBase<AccountBalance, UUID> {
    public List<AccountBalance> findByAccountIdAndReferenceDate(UUID accountId, LocalDate referenceDate) {
        return listAll("accountId = :accountId AND referenceDate = :referenceDate",
            Map.of(
                "accountId", accountId,
                "referenceDate", referenceDate)
        );
    }

    public Optional<AccountBalance> findFirstByAccountIdOrderByReferenceDateDesc(UUID accountId) {
        return findFirst("accountId = :accountId",
            OrderBy.descending("referenceDate"),
            Map.of("accountId", accountId));
    }
}
