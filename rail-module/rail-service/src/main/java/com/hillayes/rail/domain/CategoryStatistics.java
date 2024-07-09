package com.hillayes.rail.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@RegisterForReflection
public class CategoryStatistics {
    /**
     * The category name.
     */
    @EqualsAndHashCode.Include
    private String category;

    /**
     * The category id. This will be null for uncategorised transactions.
     */
    @EqualsAndHashCode.Include
    private UUID categoryId;

    /**
     * The number of transactions in this category.
     */
    private long count;

    /**
     * The total value of transactions in this category.
     */
    private BigDecimal total;
}
