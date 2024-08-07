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
     * The category group id.
     */
    @EqualsAndHashCode.Include
    private UUID groupId;

    /**
     * The category group name.
     */
    private String groupName;

    /**
     * The category name.
     */
    @EqualsAndHashCode.Include
    private String category;

    /**
     * The category id. This will be null for un-categorised transactions.
     */
    @EqualsAndHashCode.Include
    private UUID categoryId;

    /**
     * The category description. This will be null for un-categorised transactions.
     */
    private String description;

    /**
     * The category colour. This will be null for un-categorised transactions.
     */
    private String colour;

    /**
     * The number of transactions in this category.
     */
    private long count;

    /**
     * The total value of debit and credit transactions in this category.
     */
    private BigDecimal total;

    /**
     * The total credit value of transactions in this category.
     */
    private BigDecimal credit;

    /**
     * The total debit value of transactions in this category; shown as a positive
     * value.
     */
    private BigDecimal debit;
}
