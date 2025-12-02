package com.hillayes.rail.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A category is a way to group a user's transactions within and across multiple
 * accounts. Each category has selectors that are used to match an account's
 * transactions to the category.
 */
@Entity
@Getter
@Builder(builderClassName = "Builder")
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Category {
    @Id
    @GeneratedValue(generator = "uuid2")
    @Setter
    private UUID id;

    @Version
    @Column(name = "version")
    private long version;

    /**
     * The category group to which this category is belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    private CategoryGroup group;

    /**
     * The name of the category. This is unique for each user.
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Setter
    @Column(nullable = false)
    private String name;

    /**
     * A description of the category.
     */
    @Setter
    private String description;

    /**
     * The colour that is used to represent the category in the UI.
     */
    @Setter
    private String colour;

    /**
     * The selectors that are used to match transactions to this category.
     * Each selector is associated with an account that belongs to the user
     * who owns the category. The same account can have multiple selectors
     * for the same category but the selection criteria, on which they match
     * transactions, must be different.
     */
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @OrderBy("infoContains, refContains, creditorContains")
    @lombok.Builder.Default
    private List<CategorySelector> selectors = new ArrayList<>();

    /**
     * Adds a selector to the category; ensuring that the selector is associated
     * with the category and account.
     *
     * @param accountId The account that the selector is associated with.
     * @param modifier A consumer that can be used to modify the selector as it is added.
     * @return The category, to allow method chaining.
     */
    public Category addSelector(UUID accountId,
                                Consumer<CategorySelector.Builder> modifier) {
        CategorySelector.Builder builder = CategorySelector.builder()
            .category(this)
            .accountId(accountId);

        if (modifier != null) {
            modifier.accept(builder);
        }

        CategorySelector selector = builder.build();
        selectors.add(selector);
        return this;
    }

    public boolean removeSelector(CategorySelector selector) {
        return selectors.remove(selector);
    }
}
