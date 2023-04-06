package com.hillayes.rail.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.time.LocalDate;

@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionDetail {
    @EqualsAndHashCode.Include
    public String transactionId;
    @EqualsAndHashCode.Include
    public LocalDate bookingDate;
    public Instant bookingDateTime;
    public LocalDate valueDate;
    public Instant valueDateTime;
    public CurrencyAmount transactionAmount;
    public String additionalInformation;
    public String additionalInformationStructured;
    public String balanceAfterTransaction;
    public String bankTransactionCode;
    public String checkId;
    public AccountReference creditorAccount;
    public String creditorAgent;
    public String creditorId;
    public String creditorName;
    public String currencyExchange;
    public AccountReference debtorAccount;
    public String debtorAgent;
    public String debtorName;
    public String endToEndId;
    public String entryReference;
    @EqualsAndHashCode.Include
    public String internalTransactionId;
    public String mandateId;
    public String merchantCategoryCode;
    public String proprietaryBankTransactionCode;
    public String purposeCode;
    public String remittanceInformationStructured;
    public String remittanceInformationStructuredArray;
    public String remittanceInformationUnstructured;
    public String remittanceInformationUnstructuredArray;
    public String ultimateCreditor;
    public String ultimateDebtor;
}
