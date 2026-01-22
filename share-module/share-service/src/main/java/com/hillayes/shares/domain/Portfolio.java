package com.hillayes.shares.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "portfolio")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder(builderClassName = "Builder")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class Portfolio {
    @Id
    @GeneratedValue(generator = "uuid2")
    @Setter
    private UUID id;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Setter
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "name", nullable = false)
    private String name;

    @lombok.Builder.Default
    @ToString.Include
    @Column(name = "date_created", nullable = false)
    private Instant dateCreated = Instant.now();

    @Formula("(select count(1) from shares.holding h where h.portfolio_id = id)")
    private int holdingCount;

    @OneToMany(mappedBy = "portfolio", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @lombok.Builder.Default
    private List<Holding> holdings = new ArrayList<>();

    /**
     * Adds the given share index to the holdings within this portfolio.
     * This ensures that the relationships are maintained correctly.
     *
     * @param shareIndex the share index for which a holding is to be added
     * @return the newly added holding
     */
    @Transient
    public Holding add(ShareIndex shareIndex) {
        Holding holding = Holding.builder()
            .portfolioId(this.getId())
            .portfolio(this)
            .shareIndexId(shareIndex.getId())
            .shareIndex(shareIndex)
            .build();

        holdings.add(holding);
        return holding;
    }

    @Transient
    public Optional<Holding> get(ShareIndex shareIndex) {
        return getHoldings().stream()
            .filter(holding -> holding.getShareIndex().equals(shareIndex))
            .findFirst();
    }

    /**
     * Removes the holding for the given share index from this portfolio, if
     * it is present.
     *
     * @param shareIndex the share index of the holding to be removed.
     * @return true if the holding was found and removed.
     */
    public boolean remove(ShareIndex shareIndex) {
        int count = getHoldings().size();
        getHoldings().stream()
            .filter(holding -> holding.getShareIndex().equals(shareIndex))
            .findFirst()
            .ifPresent(this::remove);
        return getHoldings().size() < count;
    }

    /**
     * Removes the given holding from this portfolio, if it is present.
     *
     * @param holding the holding instance to be removed.
     * @return true if the holding was found and removed.
     */
    public boolean remove(Holding holding) {
        return holdings.remove(holding);
    }
}
