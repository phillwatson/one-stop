package com.hillayes.rail.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * A category selector is a rule that is used to match transactions to a category.
 * Each category can have multiple selectors. Each of which is associated with an
 * account belonging to the user who owns the category.
 * <p>
 * Each selector has three optional fields that are used to match transactions:
 * infoContains, refContains and creditorContains. If a field is not specified, it
 * is ignored.
 * <p>
 * If specified, a transaction must contain the specified text in the corresponding
 * field to be matched. This results in an AND operation between the fields.
 * <p>
 * If the same account has multiple selectors for the same category, the transaction
 * must match at least one of the selectors to be matched to the category. Resulting
 * in an OR operation between the selectors.
 */
@Entity
@Table(schema = "rails", name = "category_selector")
@Getter
@Builder(builderClassName = "Builder")
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class CategorySelector {
    @Id
    @GeneratedValue(generator = "uuid2")
    @Setter
    private UUID id;

    /**
     * The category that this selector is associated with.
     */
    @ManyToOne
    @Setter
    private Category category;

    /**
     * The account that this selector is associated with. The account must belong
     * to the user to whom the category belongs.
     */
    @EqualsAndHashCode.Include
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    /**
     * If specified, the transaction must contain this text in the additionalInfo
     * field to be matched.
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Setter
    @Column(name = "info_contains")
    private String infoContains;

    /**
     * If specified, the transaction must contain this text in the reference field
     * to be matched.
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Setter
    @Column(name = "ref_contains")
    private String refContains;

    /**
     * If specified, the transaction must contain this text in the creditorName
     * field to be matched.
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Setter
    @Column(name = "creditor_contains")
    private String creditorContains;
}
