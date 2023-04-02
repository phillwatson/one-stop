package com.hillayes.rail.domain;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
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

    @Builder.Default
    @Column(name = "date_created", nullable = false)
    private Instant dateCreated = Instant.now();

    /**
     * Transaction identifier given by Nordigen
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "internal_transaction_id", nullable = false)
    public String internalTransactionId;

    /**
     * Unique transaction identifier given by financial institution.
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "transaction_id", nullable = true)
    public String transactionId;

    /**
     * The date when an entry is posted to an account on the financial institutions books.
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "booking_date", nullable = true)
    public LocalDate bookingDate;

    /**
     * The date and time when an entry is posted to an account on the financial institutions books.
     */
    @Column(name = "booking_datetime", nullable = true)
    public Instant bookingDateTime;

    /**
     * The Date at which assets become available to the account owner in case of a credit.
     */
    @Column(name = "value_date", nullable = true)
    public LocalDate valueDate;

    /**
     * The date and time at which assets become available to the account owner in case of a credit.
     */
    @Column(name = "value_datetime", nullable = true)
    public Instant valueDateTime;

    /**
     * The amount of the transaction as billed to the account.
     */
    @ToString.Include
    @Column(name = "transaction_amount", nullable = true)
    public double transactionAmount;

    /**
     * The currency of the transaction as billed to the account.
     */
    @Column(name = "transaction_currency", nullable = true)
    public String transactionCurrency;

    /**
     * Might be used by the financial institution to transport additional transaction related information
     */
    @Column(name = "additional_information", nullable = true)
    public String additionalInformation;

    /**
     * Is used if and only if the bookingStatus entry equals "information"
     */
    @Column(name = "additional_information_structured", nullable = true)
    public String additionalInformationStructured;

    /**
     * This is the balance after this transaction. Recommended balance type is interimBooked.
     */
    @Column(name = "balance_after_transaction", nullable = true)
    public String balanceAfterTransaction;

    /**
     * Bank transaction code as used by the financial institution and using the sub elements of
     * this structured code defined by ISO20022. For standing order reports the following codes
     * are applicable:
     * "PMNT-ICDT-STDO" for credit transfers,
     * "PMNT-IRCT-STDO" for instant credit transfers,
     * "PMNT-ICDT-XBST" for cross-border credit transfers,
     * "PMNT-IRCT-XBST" for cross-border real time credit transfers,
     * "PMNT-MCOP-OTHR" for specific standing orders which have a dynamical amount to move left
     *      funds e.g. on month end to a saving account
     */
    @Column(name = "balance_transaction_code", nullable = true)
    public String bankTransactionCode;

    /**
     * Identification of a Cheque.
     */
    @Column(name = "check_id", nullable = true)
    public String checkId;

    @Column(name = "creditor_iban", nullable = true)
    public String creditorIban;

    @Column(name = "creditor_agent", nullable = true)
    public String creditorAgent;

    /**
     * Identification of Creditors, e.g. a SEPA Creditor ID
     */
    @Column(name = "creditor_id", nullable = true)
    public String creditorId;

    /**
     * Name of the creditor if a "Debited" transaction
     */
    @Column(name = "creditor_name", nullable = true)
    public String creditorName;

    @Column(name = "currency_exchange", nullable = true)
    public String currencyExchange;

    @Column(name = "debtor_iban", nullable = true)
    public String debtorIban;

    @Column(name = "debtor_agent", nullable = true)
    public String debtorAgent;

    /**
     * Name of the debtor if a "Credited" transaction
     */
    @Column(name = "debtor_name", nullable = true)
    public String debtorName;

    /**
     * Unique end to end ID
     */
    @Column(name = "end_to_end_id", nullable = true)
    public String endToEndId;

    /**
     * Is the identification of the transaction as used for reference given by financial institution.
     */
    @Column(name = "entity_reference", nullable = true)
    public String entryReference;

    /**
     * Identification of Mandates, e.g. a SEPA Mandate ID
     */
    @Column(name = "mandate_id", nullable = true)
    public String mandateId;

    /**
     * Merchant category code as defined by card issuer
     */
    @Column(name = "merchant_category_code", nullable = true)
    public String merchantCategoryCode;

    /**
     * Proprietary bank transaction code as used within a community or within a financial institution
     */
    @Column(name = "proprietary_bank_transaction_code", nullable = true)
    public String proprietaryBankTransactionCode;

    @Column(name = "purpose_code", nullable = true)
    public String purposeCode;

    /**
     * Reference as contained in the structured remittance reference structure
     */
    @Column(name = "remittance_information_structured", nullable = true)
    public String remittanceInformationStructured;

    /**
     * Reference as contained in the structured remittance reference structure, as an array.
     */
    @Column(name = "remittance_information_structured_array", nullable = true)
    public String remittanceInformationStructuredArray;

    @Column(name = "remittance_information_unstructured", nullable = true)
    public String remittanceInformationUnstructured;

    @Column(name = "remittance_information_unstructured_array", nullable = true)
    public String remittanceInformationUnstructuredArray;

    @Column(name = "ultimate_creditor", nullable = true)
    public String ultimateCreditor;

    @Column(name = "ultimate_debtor", nullable = true)
    public String ultimateDebtor;
}
