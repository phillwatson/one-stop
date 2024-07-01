package com.hillayes.rail.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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

    @EqualsAndHashCode.Include
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @lombok.Builder.Default
    private Set<Category> subcategories = new HashSet<>();

    @EqualsAndHashCode.Include
    @ToString.Include
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(nullable = false)
    private String name;

    private String description;

    private String colour;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @lombok.Builder.Default
    private Set<CategorySelector> selectors = new HashSet<>();

    public Category addSubcategory(Category subcategory) {
        if (subcategory == null) throw new IllegalArgumentException("Subcategory cannot be null");
        if (subcategory == this) throw new IllegalArgumentException("Subcategory cannot be the same as the parent");

        subcategory.parent = this;
        subcategory.userId = userId;
        subcategories.add(subcategory);
        return subcategory;
    }

    public boolean removeSubcategory(Category subcategory) {
        if (subcategories.remove(subcategory)) {
            subcategory.parent = null;
            return true;
        }
        return false;
    }

    public CategorySelector addSelector(Account account, String regex) {
        CategorySelector selector = CategorySelector.builder()
            .category(this)
            .account(account)
            .regex(regex)
            .build();

        selectors.add(selector);
        return selector;
    }

    public boolean removeSelector(CategorySelector selector) {
        return selectors.remove(selector);
    }
}
