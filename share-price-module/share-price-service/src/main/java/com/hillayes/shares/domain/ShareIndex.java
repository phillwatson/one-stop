package com.hillayes.shares.domain;

import com.hillayes.commons.jpa.CurrencyConverter;
import com.hillayes.shares.api.domain.ShareProvider;
import jakarta.persistence.*;
import lombok.*;

import java.util.Currency;
import java.util.UUID;

@Entity
@Table(name = "share_index")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder(builderClassName = "Builder")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ShareIndex {
    @Id
    @GeneratedValue(generator = "uuid2")
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "isin", nullable = false)
    private String isin;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "currency_code", nullable = false)
    @Convert(converter = CurrencyConverter.class)
    private Currency currency;

    @ToString.Include
    @Enumerated(EnumType.STRING)
    @Column(name ="provider", nullable = false)
    private ShareProvider provider;
}
