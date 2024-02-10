package com.hillayes.yapily;

import com.hillayes.commons.MonetaryAmount;
import com.hillayes.commons.Strings;
import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.*;
import com.hillayes.yapily.model.*;
import com.hillayes.yapily.service.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Slf4j
public class YapilyRailAdaptor implements RailProviderApi {
    /**
     * The number of days for which transaction data can be obtained.
     * As Yapily doesn't provide this information, we default to this.
     */
    private static final int MAX_HISTORY = 89;

    @Inject
    ConsentsService consentsService;

    @Inject
    AccountsService accountsService;

    @Inject
    InstitutionsService institutionsService;

    @Inject
    UsersService usersService;

    @Inject
    AuthorisationsService authorisationsService;

    @Override
    public RailProvider getProviderId() {
        return RailProvider.YAPILY;
    }

    @Override
    public Optional<RailInstitution> getInstitution(String id) {
        log.debug("Getting institution [id: {}]", id);

        return institutionsService.get(id)
            .map(institution -> RailInstitution.builder()
                .id(institution.getId())
                .name(institution.getName())
                .provider(RailProvider.YAPILY)
                .bic(null)
                .logo(getLogo(institution).orElse(null))
                .countries(
                    institution.getCountries() == null ? List.of() :
                        institution.getCountries().stream()
                            .map(Country::getCountryCode2)
                            .toList()
                )
                .transactionTotalDays(90)
                .paymentsEnabled(institutionsService.arePaymentsEnabled(institution))
                .build()
            );
    }

    @Override
    public List<RailInstitution> listInstitutions(String countryCode, boolean paymentsEnabled) {
        log.debug("Listing institutions [countryCode: {}, paymentsEnabled: {}]", countryCode, paymentsEnabled);
        return institutionsService.list(countryCode, paymentsEnabled).stream()
            .map(institution -> RailInstitution.builder()
                .id(institution.getId())
                .name(institution.getName())
                .provider(RailProvider.YAPILY)
                .bic(null)
                .logo(getLogo(institution).orElse(null))
                .countries(
                    institution.getCountries() == null ? List.of() :
                        institution.getCountries().stream()
                            .map(Country::getCountryCode2)
                            .toList()
                )
                .transactionTotalDays(90)
                .paymentsEnabled(paymentsEnabled)
                .build()
            ).toList();
    }

    @Override
    public RailAgreement register(UUID userId, RailInstitution institution, URI callbackUri, String reference) {
        log.debug("Requesting agreement [userId: {}, reference: {}, institutionId: {}]",
            userId, reference, institution.getId());

        AccountAuthorisationRequest request = new AccountAuthorisationRequest()
            .institutionId(institution.getId())
            .callback(callbackUri.toString())
            .applicationUserId(userId.toString())
            .addForwardParametersItem("ref:" + reference);

        ApiResponseOfAccountAuthorisationResponse response = authorisationsService.createAccountAuthorisation(request);
        AccountAuthorisationResponse consent = response.getData();
        if (consent == null) {
            throw new NullPointerException("No consent returned in authorisation response");
        }
        if (consent.getId() == null) {
            throw new NullPointerException("No consent ID returned in authorisation response");
        }
        if (consent.getAuthorisationUrl() == null) {
            throw new NullPointerException("No auth-link returned in authorisation response");
        }

        return RailAgreement.builder()
            .id(consent.getId().toString())
            .institutionId(institution.getId())
            .dateCreated(consent.getCreatedAt())
            .status(AgreementStatus.INITIATED)
            .maxHistory(MAX_HISTORY)
            .agreementLink(URI.create(consent.getAuthorisationUrl()))
            .build();
    }

    @Override
    public ConsentResponse parseConsentResponse(MultivaluedMap<String, String> queryParams) {
        // A typical Yapily consent callback request:
        // http://5.81.68.243/api/v1/rails/consents/response/YAPIL/
        // ?application-user-id=snoopy
        // &user-uuid=6e147c4c-b3f5-4d7b-b0b9-ceaa6ef1fb86
        // &institution=modelo-sandbox
        // &error=access_denied
        // &error-source=user
        // &error-description=VGhlIHVzZXIgY2FuY2VsbGVkIHRoZSB0cmFuc2FjdGlvbiBvciBmYWlsZWQgdG8gbG9naW4%3D
        // &ref%3A1234=
        return ConsentResponse.builder()
            .consentReference(queryParams.keySet().stream()
                .filter(key -> key.startsWith("ref:"))
                .findFirst()
                .map(ref -> ref.substring(4))
                .orElse(null))
            .errorCode(queryParams.getFirst("error"))
            .errorDescription(queryParams.getFirst("error-description"))
            .build();
    }

    @Override
    public Optional<RailAgreement> getAgreement(String id) {
        log.debug("Getting agreement [id: {}]", id);
        return consentsService.getConsent(id)
            .map(consent -> {
                List<String> accountIds = accountsService.getAccounts(consent.getConsentToken()).stream()
                    .map(Account::getId)
                    .toList();
                return RailAgreement.builder()
                    .id(id)
                    .authToken(consent.getConsentToken())
                    .accountIds(accountIds)
                    .status(of(consent.getStatus()))
                    .dateCreated(consent.getCreatedAt())
                    .dateGiven(consent.getAuthorizedAt())
                    .dateExpires(consent.getExpiresAt() == null ? consent.getReconfirmBy() : consent.getExpiresAt())
                    .institutionId(consent.getInstitutionId())
                    .maxHistory(MAX_HISTORY)
                    .build();
            });
    }

    @Override
    public boolean deleteAgreement(String id) {
        log.debug("Deleting agreement [id: {}]", id);
        return consentsService.getConsent(id)
            .map(consent -> {
                // delete the consent
                consentsService.deleteConsent(id);

                // if user has no more consents, delete user
                if (consentsService.listConsents(consent.getUserUuid()).isEmpty()) {
                    usersService.deleteUser(consent.getUserUuid());
                }
                return true;
            })
            .orElse(false);
    }

    @Override
    public Optional<RailAccount> getAccount(RailAgreement agreement, String accountId) {
        log.debug("Getting detailed account [agreementId: {}, accountId: {}]", agreement.getId(), accountId);
        return accountsService.getAccount(agreement.getAuthToken(), accountId)
            .map(account -> RailAccount.builder()
                .id(account.getId())
                .iban(getIBAN(account))
                .status(accountStatus(agreement.getStatus()))
                .institutionId(agreement.getInstitutionId())
                .name(getAccountName(account).orElse("Unknown"))
                .ownerName(getOwnerName(account).orElse("Unknown"))
                .currency(currency(account.getCurrency()))
                .accountType(account.getAccountType().getValue())

                .balance(of(account.getAccountBalances()))
                .build()
            );
    }

    public List<RailTransaction> listTransactions(RailAgreement agreement,
                                                  String accountId,
                                                  LocalDate dateFrom) {
        log.debug("Getting transactions [agreementId: {}, accountId: {}]", agreement.getId(), accountId);
        return accountsService.getAccountTransactions(agreement.getAuthToken(), accountId, dateFrom).stream()
            .filter(transaction -> transaction.getStatus() == TransactionStatusEnum.PENDING)
            .map(transaction -> RailTransaction.builder()
                .id(transaction.getId())
                .originalTransactionId(Strings.getOrDefault(transaction.getTransactionMutability(), transaction.getReference()))
                .dateBooked(transaction.getBookingDateTime())
                .dateValued(transaction.getValueDateTime())
                .amount(MonetaryAmount.of(transaction.getCurrency(), transaction.getAmount().doubleValue()))
                .description(transaction.getDescription())
                .reference(transaction.getReference())
                .creditor(transaction.getPayeeDetails() == null ? null : transaction.getPayeeDetails().getName())
                .build())
            .toList();
    }

    private AgreementStatus of(AuthorisationStatus consentStatus) {
        if (consentStatus == null)
            return AgreementStatus.INITIATED;

        return switch (consentStatus) {
            case AWAITING_AUTHORIZATION -> AgreementStatus.INITIATED;
            case AWAITING_FURTHER_AUTHORIZATION -> AgreementStatus.WAITING;
            case AWAITING_RE_AUTHORIZATION -> AgreementStatus.WAITING;
            case AWAITING_DECOUPLED_PRE_AUTHORIZATION -> AgreementStatus.WAITING;
            case AWAITING_PRE_AUTHORIZATION -> AgreementStatus.WAITING;
            case AWAITING_DECOUPLED_AUTHORIZATION -> AgreementStatus.WAITING;
            case AWAITING_SCA_METHOD -> AgreementStatus.WAITING;
            case AWAITING_SCA_CODE -> AgreementStatus.WAITING;

            case AUTHORIZED -> AgreementStatus.GIVEN;
            case CONSUMED -> AgreementStatus.GIVEN;
            case PRE_AUTHORIZED -> AgreementStatus.GIVEN;

            case REJECTED -> AgreementStatus.DENIED;
            case FAILED -> AgreementStatus.DENIED;
            case INVALID -> AgreementStatus.DENIED;

            case REVOKED -> AgreementStatus.CANCELLED;
            case EXPIRED -> AgreementStatus.EXPIRED;
            case UNKNOWN -> AgreementStatus.INITIATED;
        };
    }

    private AccountStatus accountStatus(AgreementStatus agreementStatus) {
        return switch (agreementStatus) {
            case GIVEN -> AccountStatus.READY;
            case WAITING, INITIATED -> AccountStatus.PROCESSING;
            case DENIED -> AccountStatus.ERROR;
            case SUSPENDED, CANCELLED -> AccountStatus.SUSPENDED;
            case EXPIRED -> AccountStatus.EXPIRED;
        };
    }

    private Currency currency(String currencyCode) {
        return Strings.isBlank(currencyCode)
            ? Currency.getInstance("GBP")
            : Currency.getInstance(currencyCode);
    }

    private Optional<String> getOwnerName(Account account) {
        if (account.getAccountNames() == null)
            return Optional.empty();
        return account.getAccountNames().stream()
            .map(AccountName::getName)
            .filter(Strings::isNotBlank)
            .findFirst();
    }

    private Optional<String> getAccountName(Account account) {
        return Optional.ofNullable(
            Strings.getOrDefault(
                account.getNickname(),
                getOwnerName(account).orElse(null)
            )
        );
    }

    private MonetaryAmount of(Amount amount) {
        return amount == null
            ? MonetaryAmount.ZERO
            : MonetaryAmount.of(amount.getCurrency(), amount.getAmount().doubleValue());
    }

    private RailBalance of(List<AccountBalance> balances) {
        if (balances == null) {
            return RailBalance.builder()
                .type("UNKNOWN")
                .dateTime(Instant.now())
                .amount(MonetaryAmount.ZERO)
                .build();
        }

        return balances.stream()
            .filter(balance -> balance.getType() != null)
            .filter(balance -> balance.getDateTime() != null)
            .max((a, b) -> {
                assert b.getDateTime() != null;
                return b.getDateTime().compareTo(a.getDateTime());
            })
            .map(balance -> RailBalance.builder()
                .type(balance.getType().getValue())
                .dateTime(balance.getDateTime())
                .amount(of(balance.getBalanceAmount()))
                .build()
            ).orElse(RailBalance.builder()
                .type("UNKNOWN")
                .dateTime(Instant.now())
                .amount(MonetaryAmount.ZERO)
                .build()
            );
    }

    private Optional<String> getLogo(Institution institution) {
        if (institution.getMedia() == null) {
            return Optional.empty();
        }
        return institution.getMedia().stream()
            .filter(media -> media.getSource() != null)
            .filter(media -> "ICON".equalsIgnoreCase(media.getType()))
            .map(Media::getSource)
            .findFirst();
    }

    private String getIBAN(Account account) {
        if (account.getAccountIdentifications() == null) {
            return null;
        }

        return account.getAccountIdentifications().stream()
            .filter(identification -> AccountIdentificationType.IBAN.equals(identification.getType()))
            .map(AccountIdentification::getIdentification)
            .findFirst()
            .orElse(null);
    }
}