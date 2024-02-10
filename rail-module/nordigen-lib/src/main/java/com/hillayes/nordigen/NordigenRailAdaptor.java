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
import com.hillayes.rail.api.domain.RailBalance;
import com.hillayes.rail.api.domain.RailInstitution;
import com.hillayes.rail.api.domain.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

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
    public RailProvider getProviderId() {
        return RailProvider.NORDIGEN;
    }

    @Override
    public Optional<RailInstitution> getInstitution(String id) {
        log.debug("Getting institution [id: {}]", id);
        return institutionService.get(id)
            .map(institution -> RailInstitution.builder()
                .id(institution.id)
                .provider(RailProvider.NORDIGEN)
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
    public List<RailInstitution> listInstitutions(String countryCode,
                                                  boolean paymentsEnabled) {
        log.debug("Listing institutions [countryCode: {}, paymentsEnabled: {}]", countryCode, paymentsEnabled);
        return institutionService.list(countryCode, paymentsEnabled)
            .stream()
            .map(institution -> RailInstitution.builder()
                .id(institution.id)
                .provider(RailProvider.NORDIGEN)
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
    public RailAgreement register(UUID userId,
                                  RailInstitution institution,
                                  URI callbackUri,
                                  String reference) {
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

        return RailAgreement.builder()
            .id(requisition.id)
            .institutionId(agreement.institutionId)
            .accountIds(requisition.accounts)
            .status(of(requisition.status))
            .dateCreated(requisition.created.toInstant())
            .dateGiven(agreement.accepted == null ? null : agreement.accepted.toInstant())
            .dateExpires(agreement.accepted == null ? null : agreement.accepted.plusDays(agreement.accessValidForDays).toInstant())
            .maxHistory(agreement.maxHistoricalDays)
            .agreementLink(URI.create(requisition.link))
            .build();
    }

    @Override
    public ConsentResponse parseConsentResponse(MultivaluedMap<String, String> queryParams) {
        // A typical Nordigen consent callback request:
        // http://5.81.68.243/api/v1/rails/consents/response/NORDIGEN
        // ?ref=cbaee100-3f1f-4d7c-9b3b-07244e6a019f
        // &error=UserCancelledSession
        // &details=User+cancelled+the+session.
        return ConsentResponse.builder()
            .consentReference(queryParams.getFirst("ref"))
            .errorCode(queryParams.getFirst("error"))
            .errorDescription(queryParams.getFirst("details"))
            .build();
    }

    @Override
    public Optional<RailAgreement> getAgreement(String id) {
        log.debug("Getting agreement [id: {}]", id);
        return requisitionService.get(id)
            .flatMap(requisition -> agreementService.get(requisition.agreement)
                .map(agreement -> RailAgreement.builder()
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
    public Optional<RailAccount> getAccount(RailAgreement agreement, String id) {
        log.debug("Getting account [id: {}]", id);
        return accountService.get(id)
            .map(account -> RailAccount.builder()
                .id(account.id)
                .iban(account.iban)
                .institutionId(account.institutionId)
                .name(account.ownerName)
                .ownerName(account.ownerName)
                .status(AccountStatus.valueOf(account.status.name()))
                .balance(getBalance(id).orElse(RailBalance.builder()
                        .type("")
                        .dateTime(Instant.now())
                        .amount(MonetaryAmount.ZERO)
                        .build()))
                .build()
            )
            .map(account -> accountService.details(id)
                .map(details -> (Map<String, String>) details.get("account"))
                .map(accountProperties -> account.toBuilder()
                    .name(accountProperties.getOrDefault("name", account.getName()))
                    .accountType(accountProperties.get("cashAccountType"))
                    .currency(currency(accountProperties.get("currency")))
                    .build()
                )
                .orElse(account)
            );
    }

    @Override
    public List<RailTransaction> listTransactions(RailAgreement agreement,
                                                  String accountId,
                                                  LocalDate dateFrom) {
        log.debug("Listing transactions [accountId: {}, from: {}]", accountId, dateFrom);
        return accountService.transactions(accountId, dateFrom, LocalDate.now())
            .map(transactions -> transactions.booked.stream()
                .map(transaction -> RailTransaction.builder()
                    .id(Strings.getOrDefault(transaction.internalTransactionId, transaction.transactionId))
                    .originalTransactionId(Strings.getOrDefault(transaction.transactionId, transaction.entryReference))
                    .dateBooked(bestOf(transaction.bookingDate, transaction.bookingDateTime))
                    .dateValued(bestOf(transaction.valueDate, transaction.valueDateTime))
                    .amount(of(transaction.transactionAmount))
                    .reference(Strings.getOrDefault(transaction.entryReference, transaction.remittanceInformationUnstructured))
                    .description(Strings.toStringOrNull(transaction.additionalInformation))
                    .creditor(Strings.toStringOrNull(transaction.creditorName))
                    .build()
                )
                .toList()
            )
            .orElse(List.of());
    }

    private Optional<RailBalance> getBalance(String accountId) {
        log.debug("Listing balances [accountId: {}]", accountId);
        return accountService.balances(accountId)
            .flatMap(balances -> balances.stream()
                .max((a, b) -> b.referenceDate.compareTo(a.referenceDate))
                .map(balance -> RailBalance.builder()
                    .type(balance.balanceType)
                    .dateTime(balance.referenceDate.atStartOfDay().toInstant(ZoneOffset.UTC))
                    .amount(of(balance.balanceAmount))
                    .build()
                )
            );
    }

    private AgreementStatus of(RequisitionStatus status) {
        return switch (status) {
            case CR -> AgreementStatus.INITIATED; // CREATED Requisition has been successfully created
            case GC -> AgreementStatus.WAITING; // GIVING_CONSENT End-user is giving consent at GoCardless's consent screen
            case UA -> AgreementStatus.WAITING; // UNDERGOING_AUTHENTICATION End-user is redirected to the financial institution for authentication
            case SA -> AgreementStatus.WAITING; // SELECTING_ACCOUNTS End-user is selecting accounts
            case GA -> AgreementStatus.WAITING; // GRANTING_ACCESS End-user is granting access to their account information
            case LN -> AgreementStatus.GIVEN; // LINKED Account has been successfully linked to requisition
            case RJ -> AgreementStatus.DENIED; // REJECTED Either SSN verification has failed or end-user has entered incorrect credentials
            case SU -> AgreementStatus.SUSPENDED; // SUSPENDED Requisition is suspended due to numerous consecutive errors that happened while accessing its accounts
            case EX -> AgreementStatus.EXPIRED; // EXPIRED Access to accounts has expired as set in End User Agreement
            case ID, ER -> AgreementStatus.WAITING;
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
