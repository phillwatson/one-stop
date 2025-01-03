package com.hillayes.rail.domain;

import com.hillayes.commons.MonetaryAmount;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "rails", name = "account_transaction")
@Getter
@Builder(builderClassName = "Builder")
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
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

    @lombok.Builder.Default
    @Column(name = "date_created", nullable = false)
    private Instant dateCreated = Instant.now();

    /**
     * Transaction identifier given by the rail service provider.
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "internal_transaction_id", nullable = false)
    private String internalTransactionId;

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
     * The amount of the transaction as billed to the account.
     */
    @ToString.Include
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "currency_code"))
    })
    private MonetaryAmount amount;

    /**
     * Might be used by the financial institution to transport additional transaction related information
     */
    @Column(name = "additional_information", nullable = true)
    private String additionalInformation;

    /**
     * Name of the creditor if a "Debited" transaction
     */
    @Column(name = "creditor_name", nullable = true)
    private String creditorName;

    /**
     * Is the identification of the transaction as used for reference given by financial institution.
     */
    @Column(name = "reference", nullable = true)
    private String reference;
}
