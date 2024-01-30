package com.hillayes.nordigen;

import com.hillayes.commons.MonetaryAmount;
import com.hillayes.commons.Strings;
import com.hillayes.nordigen.model.*;
import com.hillayes.nordigen.service.AccountService;
import com.hillayes.nordigen.service.AgreementService;
import com.hillayes.nordigen.service.InstitutionService;
import com.hillayes.nordigen.service.RequisitionService;
import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.AccountStatus;
import com.hillayes.rail.api.domain.Balance;
import com.hillayes.rail.api.domain.Institution;
import com.hillayes.rail.api.domain.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class NordigenRailAdaptor implements RailProviderApi {
    // The number of days for which account access will be agreed
    private final static int ACCESS_VALID_FOR_DAYS = 90;
    private final static List<String> CONSENT_SCOPES = List.of("balances", "details", "transactions");

    @Inject
    AccountService accountService;

    @Inject
    RequisitionService requisitionService;

    @Inject
    AgreementService agreementService;

    @Inject
    InstitutionService institutionService;

    @Override
    public Optional<Institution> getInstitution(String id) {
        log.debug("Getting institution [id: {}]", id);
        return institutionService.get(id)
            .map(institution -> Institution.builder()
                .id(institution.id)
                .name(institution.name)
                .bic(institution.bic)
                .logo(institution.logo)
                .countries(institution.countries)
                .transactionTotalDays(institution.transactionTotalDays)
                .paymentsEnabled(false)
                .build()
            );
    }

    @Override
    public List<Institution> listInstitutions(String countryCode,
                                              boolean paymentsEnabled) {
        log.debug("Listing institutions [countryCode: {}]", countryCode);
        return institutionService.list(countryCode, paymentsEnabled)
            .stream()
            .map(institution -> Institution.builder()
                .id(institution.id)
                .name(institution.name)
                .bic(institution.bic)
                .logo(institution.logo)
                .countries(institution.countries)
                .transactionTotalDays(institution.transactionTotalDays)
                .paymentsEnabled(paymentsEnabled)
                .build()
            )
            .toList();
    }

    @Override
    public Agreement register(Institution institution, URI callbackUri, String reference) {
        log.debug("Requesting agreement [reference: {}, institutionId: {}]", reference, institution.getId());
        EndUserAgreement agreement = agreementService.create(EndUserAgreementRequest.builder()
            .institutionId(institution.getId())
            .accessScope(CONSENT_SCOPES)
            .accessValidForDays(ACCESS_VALID_FOR_DAYS)
            .maxHistoricalDays(institution.getTransactionTotalDays())
            .build());

        // create requisition
        log.debug("Requesting requisition [reference: {}, institutionId: {}: agreementId: {}, callbackUri: {}]",
            reference, institution.getId(), agreement.id, callbackUri);

        Requisition requisition = requisitionService.create(RequisitionRequest.builder()
            .institutionId(agreement.institutionId)
            .agreement(agreement.id)
            .accountSelection(Boolean.FALSE)
            .userLanguage("EN")
            .redirectImmediate(Boolean.FALSE)
            .redirect(callbackUri.toString())
            // reference is returned in callback
            // - allows us to identify the consent record
            .reference(reference)
            .build());

        log.debug("Requisition created [reference: {}, institutionId: {}: agreementId: {}, requisitionId: {}]",
            reference, institution.getId(), agreement.id, requisition.id);

        return Agreement.builder()
            .id(requisition.id)
            .accountIds(requisition.accounts)
            .status(of(requisition.status))
            .dateCreated(requisition.created.toInstant())
            .dateGiven(agreement.accepted == null ? null : agreement.accepted.toInstant())
            .dateExpires(agreement.accepted == null ? null : agreement.accepted.plusDays(agreement.accessValidForDays).toInstant())
            .institutionId(agreement.institutionId)
            .maxHistory(agreement.maxHistoricalDays)
            .agreementLink(URI.create(requisition.link))
            .build();
    }

    @Override
    public Optional<Agreement> getAgreement(String id) {
        log.debug("Getting agreement [id: {}]", id);
        return requisitionService.get(id)
            .flatMap(requisition -> agreementService.get(requisition.agreement)
                .map(agreement -> Agreement.builder()
                    .id(requisition.id)
                    .accountIds(requisition.accounts)
                    .status(of(requisition.status))
                    .dateCreated(requisition.created.toInstant())
                    .dateGiven(agreement.accepted == null ? null : agreement.accepted.toInstant())
                    .dateExpires(agreement.accepted == null ? null : agreement.accepted.plusDays(agreement.accessValidForDays).toInstant())
                    .institutionId(agreement.institutionId)
                    .maxHistory(agreement.maxHistoricalDays)
                    .build()
                ));
    }

    @Override
    public boolean deleteAgreement(String id) {
        log.debug("Deleting agreement [id: {}]", id);
        requisitionService.delete(id);
        return true;
    }

    @Override
    public Optional<Account> getAccount(String id) {
        log.debug("Getting account [id: {}]", id);
        return accountService.get(id)
            .map(account -> Account.builder()
                .id(account.id)
                .iban(account.iban)
                .institutionId(account.institutionId)
                .name(account.ownerName)
                .ownerName(account.ownerName)
                .status(AccountStatus.valueOf(account.status.name()))
                .build()
            )
            .map(account -> accountService.details(id)
                .map(details -> (Map<String, String>) details.get("account"))
                .map(accountProperties -> {
                    account.setName(accountProperties.get("name"));
                    account.setAccountType(accountProperties.get("cashAccountType"));
                    account.setCurrency(currency(accountProperties.get("currency")));
                    return account;
                })
                .orElse(account)
            );
    }

    @Override
    public List<Balance> listBalances(String accountId, LocalDate dateFrom) {
        log.debug("Listing balances [accountId: {}, from: {}]", accountId, dateFrom);
        return accountService.balances(accountId)
            .map(balances -> balances.stream()
                .filter(balance -> balance.referenceDate.isAfter(dateFrom))
                .map(balance -> Balance.builder()
                    .type(balance.balanceType)
                    .dateTime(balance.referenceDate)
                    .amount(of(balance.balanceAmount))
                    .build()
                )
                .toList()
            )
            .orElse(List.of());
    }

    @Override
    public List<Transaction> listTransactions(String accountId, LocalDate dateFrom, LocalDate dateTo) {
        log.debug("Listing transactions [accountId: {}, from: {}, to: {}]", accountId, dateFrom, dateTo);
        return accountService.transactions(accountId, dateFrom, dateTo)
            .map(transactions -> transactions.booked.stream()
                .map(transaction -> Transaction.builder()
                    .id(Strings.valueOf(transaction.internalTransactionId, transaction.transactionId))
                    .originalTransactionId(Strings.valueOf(transaction.transactionId, transaction.entryReference))
                    .dateBooked(bestOf(transaction.bookingDate, transaction.bookingDateTime))
                    .dateValued(bestOf(transaction.valueDate, transaction.valueDateTime))
                    .amount(of(transaction.transactionAmount))
                    .reference(Strings.valueOf(transaction.entryReference, transaction.remittanceInformationUnstructured))
                    .description(Strings.toStringOrNull(transaction.additionalInformation))
                    .creditor(Strings.toStringOrNull(transaction.creditorName))
                    .build()
                )
                .toList()
            )
            .orElse(List.of());
    }

    private AgreementStatus of(RequisitionStatus status) {
        return switch (status) {
            case CR -> AgreementStatus.INITIATED; // CREATED Requisition has been successfully created
            case GC -> AgreementStatus.WAITING; // GIVING_CONSENT End-user is giving consent at GoCardless's consent screen
            case UA -> AgreementStatus.WAITING; // UNDERGOING_AUTHENTICATION End-user is redirected to the financial institution for authentication
            case RJ -> AgreementStatus.DENIED; // REJECTED Either SSN verification has failed or end-user has entered incorrect credentials
            case SA -> AgreementStatus.WAITING; // SELECTING_ACCOUNTS End-user is selecting accounts
            case GA -> AgreementStatus.WAITING; // GRANTING_ACCESS End-user is granting access to their account information
            case LN -> AgreementStatus.GIVEN; // LINKED Account has been successfully linked to requisition
            case SU -> AgreementStatus.SUSPENDED; // SUSPENDED Requisition is suspended due to numerous consecutive errors that happened while accessing its accounts
            case EX -> AgreementStatus.EXPIRED; // EXPIRED Access to accounts has expired as set in End User Agreement
            case ID -> AgreementStatus.WAITING;
            case ER -> AgreementStatus.WAITING;
        };
    }

    private MonetaryAmount of(com.hillayes.nordigen.model.CurrencyAmount amount) {
        return (amount == null)
            ? MonetaryAmount.ZERO
            : MonetaryAmount.of(amount.currency, amount.amount);
    }

    private Currency currency(String currencyCode) {
        return Strings.isBlank(currencyCode)
            ? Currency.getInstance("GBP")
            : Currency.getInstance(currencyCode);
    }

    /**
     * Takes the best of the given date and instant; preferring the instant if both are present.
     * If neither are present, returns null.
     *
     * @param date the date to use if instant is null.
     * @param instant the instant to use if not null.
     * @return the best of the given date and instant.
     */
    private Instant bestOf(LocalDate date, Instant instant) {
        return (instant != null)
            ? instant
            : (date == null) ? null : date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
