package com.hillayes.yapily;

import com.hillayes.commons.MonetaryAmount;
import com.hillayes.commons.Strings;
import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.*;
import com.hillayes.yapily.model.*;
import com.hillayes.yapily.service.AccountsService;
import com.hillayes.yapily.service.ConsentsService;
import com.hillayes.yapily.service.InstitutionsService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class YapilyRailProvider implements RailProviderApi {
    /**
     * The number of days for which transaction data can be obtained.
     * As Yapily doesn't provide this information, we default to this.
     */
    private static final int MAX_HISTORY = 89;

    private final ConsentsService consentsService;
    private final AccountsService accountsService;
    private final InstitutionsService institutionsService;

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

        ApiResponseOfAccountAuthorisationResponse response = accountsService.createAccountAuthorisation(request);
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
            .dateGiven(consent.getAuthorizedAt())
            .dateExpires(consent.getExpiresAt() == null ? consent.getReconfirmBy() : consent.getExpiresAt())
            .status(AgreementStatus.INITIATED)
            .maxHistory(MAX_HISTORY)
            .agreementLink(URI.create(consent.getAuthorisationUrl()))
            .build();
    }

    @Override
    public ConsentResponse parseConsentResponse(MultivaluedMap<String, String> queryParams) {
        // A typical Yapily consent callback request:
        // http://5.81.68.243/api/v1/rails/consents/response/YAPILY/
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
            .errorDescription(decode(queryParams.getFirst("error-description")))
            .build();
    }

    /**
     * Used to decode the error description in the consent response.
     * @param encoded the base-64 encoded string.
     * @return the decoded string, or null if the encoded string is null.
     */
    private String decode(String encoded) {
        return Strings.isBlank(encoded)
            ? null
            : new String(java.util.Base64.getDecoder().decode(encoded));
    }

    @Override
    public Optional<RailAgreement> getAgreement(String id) {
        log.debug("Getting agreement [id: {}]", id);
        return consentsService.getConsent(id)
            .map(consent -> {
                AgreementStatus agreementStatus = of(consent.getStatus());
                List<String> accountIds = (agreementStatus == AgreementStatus.GIVEN)
                    ? accountsService.getAccounts(consent.getConsentToken()).stream()
                    .map(Account::getId)
                    .toList()
                    : List.of();
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

        // TODO: deleting user will prevent user subsequent registrations
        //return consentsService.getConsent(id)
        //    .map(consent -> {
        //        // delete the consent
        //        consentsService.deleteConsent(id);
        //
        //        // if user has no more consents, delete user
        //        //  if (consentsService.listConsents(consent.getUserUuid()).isEmpty()) {
        //        //      usersService.deleteUser(consent.getUserUuid());
        //        //  }
        //        return true;
        //    })
        //    .orElse(false);

        // TODO: simply delete the consent
        return consentsService.deleteConsent(id);
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
                .balance(of(account))
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
                .amount(MonetaryAmount.of(transaction.getCurrency(), transaction.getAmount()))
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
            case AWAITING_AUTHORIZATION, UNKNOWN -> AgreementStatus.INITIATED;

            case AWAITING_FURTHER_AUTHORIZATION,
                AWAITING_DECOUPLED_PRE_AUTHORIZATION,
                AWAITING_PRE_AUTHORIZATION,
                AWAITING_DECOUPLED_AUTHORIZATION,
                AWAITING_SCA_METHOD,
                AWAITING_SCA_CODE -> AgreementStatus.WAITING;

            case AUTHORIZED, PRE_AUTHORIZED, CONSUMED -> AgreementStatus.GIVEN;
            case REJECTED, INVALID, FAILED -> AgreementStatus.DENIED;
            case EXPIRED, AWAITING_RE_AUTHORIZATION -> AgreementStatus.EXPIRED;
            case REVOKED -> AgreementStatus.CANCELLED;
        };
    }

    private RailAccountStatus accountStatus(AgreementStatus agreementStatus) {
        return switch (agreementStatus) {
            case GIVEN -> RailAccountStatus.READY;
            case WAITING, INITIATED -> RailAccountStatus.PROCESSING;
            case DENIED -> RailAccountStatus.ERROR;
            case SUSPENDED, CANCELLED -> RailAccountStatus.SUSPENDED;
            case EXPIRED -> RailAccountStatus.EXPIRED;
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

    private RailBalance of(Account account) {
        List<AccountBalance> balances = account.getAccountBalances();
        if (balances == null) {
            return RailBalance.builder()
                .type("UNKNOWN")
                .dateTime(Instant.now())
                .amount(MonetaryAmount.ZERO)
                .build();
        }

        return balances.stream()
            .filter(
                balance -> balance.getType() != null &&
                balance.getDateTime() != null &&
                balance.getBalanceAmount() != null &&
                balance.getBalanceAmount().getCurrency().equals(account.getCurrency())
            )
            .max((a, b) -> {
                assert b.getDateTime() != null;
                return b.getDateTime().compareTo(a.getDateTime());
            })
            .map(balance -> RailBalance.builder()
                .type(balance.getType().getValue())
                .dateTime(balance.getDateTime())
                .amount(of(balance.getBalanceAmount()))
                .build()
            ).orElseGet(() -> RailBalance.builder()
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
            .findFirst()
            .map(Media::getSource);
    }

    private String getIBAN(Account account) {
        if (account.getAccountIdentifications() == null) {
            return null;
        }

        return account.getAccountIdentifications().stream()
            .filter(identification -> AccountIdentificationType.IBAN.equals(identification.getType()))
            .findFirst()
            .map(AccountIdentification::getIdentification)
            .orElse(null);
    }
}
