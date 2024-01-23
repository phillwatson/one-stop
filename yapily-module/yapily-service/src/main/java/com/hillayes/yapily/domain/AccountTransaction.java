package com.hillayes.yapily.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_transaction")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class AccountTransaction {
    @Id
    @GeneratedValue(generator = "uuid2")
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Builder.Default
    @Column(name = "date_created", nullable = false)
    private Instant dateCreated = Instant.now();

    /**
     * Transaction identifier given by rail service
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "rail_transaction_id", nullable = false)
    private String railTransactionId;

    /**
     * Unique transaction identifier given by financial institution.
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "transaction_id", nullable = true)
    private String transactionId;

    /**
     * The date and time when an entry is posted to an account on the financial institutions books.
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "booking_datetime", nullable = false)
    private Instant bookingDateTime;

    /**
     * The date and time at which assets become available to the account owner in case of a credit.
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "value_datetime", nullable = true)
    private Instant valueDateTime;

    /**
     * The amount of the transaction as billed to the account. The value is given in the minor
     * units of the currency.
     */
    @ToString.Include
    @Column(name = "amount", nullable = false)
    private long amount;

    /**
     * The currency of the transaction as billed to the account.
     */
    @Column(name = "currency_code", nullable = false)
    private String currencyCode;

    /**
     * Is the identification of the transaction as used for reference given by financial institution.
     */
    @Column(name = "entry_reference", nullable = true)
    private String entryReference;
}
