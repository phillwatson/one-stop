package com.hillayes.integration.test.sim.rail;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.hillayes.rail.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

@Slf4j
public class AccountsEndpoint extends AbstractResponseTransformer {
    private final Map<String, AccountSummary> accounts = new HashMap<>();
    private final Map<String, AccountBalanceList> balances = new HashMap<>();
    private final Map<String, TransactionList> transactions = new HashMap<>();

    public void removeAccount(String accountId) {
        accounts.remove(accountId);
        balances.remove(accountId);
        transactions.remove(accountId);
    }

    public String acquireAccount(String institutionId) {
        AccountSummary account = AccountSummary.builder()
            .id(UUID.randomUUID().toString())
            .institutionId(institutionId)
            .iban(randomAlphanumeric(20))
            .ownerName(randomAlphanumeric(10))
            .status(AccountStatus.READY)
            .created(OffsetDateTime.now())
            .lastAccessed(OffsetDateTime.now())
            .build();

        accounts.put(account.id, account);
        return account.id;
    }

    public void register(WireMock wireMockClient) {
        accounts.clear();
        balances.clear();
        transactions.clear();

        // mock get account endpoint
        wireMockClient.register(get(urlPathMatching(NordigenSimulator.BASE_URI + "/api/v2/accounts/([^/]+)/([^/]+/)?$"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withTransformers(getName())
            )
        );
    }

    @Override
    public ResponseDefinition transform(Request request,
                                        ResponseDefinition responseDefinition,
                                        FileSource files,
                                        Parameters parameters) {
        String id = getIdFromPath(request.getUrl(), 4);
        AccountSummary account = accounts.get(id);
        if (account == null) {
            return notFound(request, responseDefinition);
        }

        String resourceName = getPathElement(request.getUrl(), 5).orElse(null);
        if (resourceName == null) {
            return getAccount(account, responseDefinition);
        } else if (resourceName.equalsIgnoreCase("balances")) {
            return getBalances(account, responseDefinition);
        } else if (resourceName.equalsIgnoreCase("details")) {
            return getDetails(account, responseDefinition);
        } else if (resourceName.equalsIgnoreCase("transactions")) {
            return getTransactions(account, request, responseDefinition);
        }

        return notFound(request, responseDefinition);
    }

    private ResponseDefinition getAccount(AccountSummary account,
                                          ResponseDefinition responseDefinition) {
        return ResponseDefinitionBuilder.like(responseDefinition)
            .withStatus(200)
            .withBody(toJson(account))
            .build();
    }

    private ResponseDefinition getBalances(AccountSummary account,
                                           ResponseDefinition responseDefinition) {
        String[] balanceTypes = {"expected", "interimAvailable"};

        AccountBalanceList result = balances.get(account.id);
        if (result == null) {
            result = new AccountBalanceList();
            result.balances = new ArrayList<>();

            LocalDate referenceDate = LocalDate.now();
            for (int i = 0; i < 4; i++) {
                for (String balanceType : balanceTypes) {
                    result.balances.add(Balance.builder()
                        .balanceAmount(randomAmount())
                        .balanceType(balanceType)
                        .referenceDate(referenceDate)
                        .build()
                    );
                }
                referenceDate = referenceDate.minusDays(1);
            }

            balances.put(account.id, result);
        }

        return ResponseDefinitionBuilder.like(responseDefinition)
            .withStatus(200)
            .withBody(toJson(result))
            .build();
    }

    private ResponseDefinition getDetails(AccountSummary account,
                                          ResponseDefinition responseDefinition) {
        // just some example properties of the account detail
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", account.id);
        detail.put("iban", account.iban);
        detail.put("ownerName", account.ownerName);
        detail.put("currency", "GBP");

        Map<String, Object> result = new HashMap<>();
        result.put("account", detail);

        return ResponseDefinitionBuilder.like(responseDefinition)
            .withStatus(200)
            .withBody(toJson(result))
            .build();
    }

    private ResponseDefinition getTransactions(AccountSummary account,
                                               Request request,
                                               ResponseDefinition responseDefinition) {
        LocalDate dateFrom = getQueryDate(request, "date_from").orElse(null);
        LocalDate dateTo = getQueryDate(request, "date_to").orElse(null);

        TransactionList transactionDetails = transactions.get(account.id);
        if (transactionDetails == null) {
            transactionDetails = TransactionList.builder()
                .booked(randomTransactions(dateFrom, dateTo))
                .pending(randomTransactions(LocalDate.now(), 3))
                .build();

            transactions.put(account.id, transactionDetails);
        }

        TransactionsResponse result = new TransactionsResponse();
        result.transactions = transactionDetails;

        return ResponseDefinitionBuilder.like(responseDefinition)
            .withStatus(200)
            .withBody(toJson(result))
            .build();
    }

    private List<TransactionDetail> randomTransactions(LocalDate from, LocalDate to) {
        List<TransactionDetail> result = new ArrayList<>();
        LocalDate date = LocalDate.now();
        for (int day = RandomUtils.nextInt(10, 30); day >= 0; --day) {
            result.addAll(randomTransactions(date, RandomUtils.nextInt(5, 10)));
            date = date.minusDays(1);
        }
        return result;
    }

    private List<TransactionDetail> randomTransactions(LocalDate date, int count) {
        int interval = 24 / count;
        List<TransactionDetail> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(randomTransaction(date, interval));
        }
        return result;
    }

    private TransactionDetail randomTransaction(LocalDate date, int hourOfDay) {
        return TransactionDetail.builder()
            .transactionId(UUID.randomUUID().toString())
            .internalTransactionId(UUID.randomUUID().toString())
            .transactionAmount(randomAmount())
            .bookingDate(date)
            .bookingDateTime(date.atTime(hourOfDay, 0).toInstant(ZoneOffset.UTC))
            .remittanceInformationUnstructured(randomAlphanumeric(30))
            .build();
    }

    private CurrencyAmount randomAmount() {
        return CurrencyAmount.builder()
            .amount(RandomUtils.nextFloat())
            .currency("GBP")
            .build();
    }
}
