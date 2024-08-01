package com.hillayes.rail.domain;

import com.hillayes.commons.Strings;
import com.hillayes.exception.common.MissingParameterException;
import com.hillayes.rail.errors.CategoryAlreadyExistsException;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Category groups allow users to arrange their categories into groups. This can
 * be useful for organizing categories into logical groups, such as "Income" and
 * "Expenses".
 */
@Entity
@Table(name = "category_group")
@Getter
@Builder(builderClassName = "Builder")
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class CategoryGroup {
    @Id
    @GeneratedValue(generator = "uuid2")
    @Setter
    private UUID id;

    @Version
    @Column(name = "version")
    private long version;

    /**
     * The user to whom this category group belongs.
     */
    @EqualsAndHashCode.Include
    @Column(name = "user_id", nullable = false)
    private UUID userId;

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
     * The categories that belong to this group.
     */
    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @lombok.Builder.Default
    private Set<Category> categories = new HashSet<>();

    public Optional<Category> getCategory(UUID id) {
        return categories.stream()
            .filter(c -> c.getId().equals(id))
            .findAny();
    }

    public Optional<Category> getCategory(String name) {
        return categories.stream()
            .filter(c -> c.getName().equalsIgnoreCase(name))
            .findAny();
    }

    /**
     * Adds a category to this group; ensuring that the category is associated with
     * this group and that the category name is unique within this group.
     *
     * @param name The name of the category.
     * @param modifier A consumer that can be used to modify the category as it is added.
     * @return The new category.
     */
    public Category addCategory(String name,
                                Consumer<Category.Builder> modifier) {
        String newName = Strings.trimOrNull(name);
        if (newName == null) {
            throw new MissingParameterException("Category.name");
        }

        categories.stream()
            .filter(c -> c.getName().equalsIgnoreCase(newName))
            .findAny()
            .ifPresent(c -> {
                throw new CategoryAlreadyExistsException(c);
            });

        Category.Builder builder = Category.builder()
            .group(this)
            .name(name);

        if (modifier != null) {
            modifier.accept(builder);
        }

        Category category = builder.build();
        categories.add(category);
        return category;
    }

    public boolean removeCategory(Category category) {
        return categories.remove(category);
    }
}
