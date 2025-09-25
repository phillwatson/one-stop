package com.hillayes.sim.nordigen;

import com.hillayes.nordigen.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.apache.commons.lang3.RandomStringUtils.insecure;

@ApplicationScoped
@Path(NordigenSimulator.BASE_URI + "/api/v2/accounts/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class AccountsEndpoint extends AbstractEndpoint {
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
            .iban(insecure().nextAlphanumeric(20))
            .ownerName(insecure().nextAlphanumeric(10))
            .status(AccountStatus.READY)
            .created(OffsetDateTime.now())
            .lastAccessed(OffsetDateTime.now())
            .build();

        accounts.put(account.id, account);
        return account.id;
    }

    public void reset() {
        accounts.clear();
        balances.clear();
        transactions.clear();
    }

    @GET
    @Path("{id}/")
    public Response getAccount(@PathParam("id") String accountId) {
        log.info("get account [id: {}]", accountId);
        AccountSummary account = accounts.get(accountId);
        if (account == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(account).build();
    }

    @GET
    @Path("{id}/details/")
    public Response getDetails(@PathParam("id") String accountId) {
        log.info("get account detail [id: {}]", accountId);
        AccountSummary account = accounts.get(accountId);
        if (account == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // just some example properties of the account detail
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", account.id);
        detail.put("iban", account.iban);
        detail.put("ownerName", account.ownerName);
        detail.put("currency", "GBP");

        Map<String, Object> result = new HashMap<>();
        result.put("account", detail);
        return Response.ok(result).build();
    }

    @GET
    @Path("{id}/balances/")
    public Response getBalances(@PathParam("id") String accountId) {
        log.info("get balances [id: {}]", accountId);
        AccountSummary account = accounts.get(accountId);
        if (account == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

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
        return Response.ok(result).build();
    }

    @GET
    @Path("{id}/transactions/")
    public Response getTransactions(@PathParam("id") String accountId,
                                    @QueryParam("date_from") String dateFromStr,
                                    @QueryParam("date_to") String dateToStr) {
        log.info("get transactions [id: {}, from: {}, to: {}]", accountId, dateFromStr, dateToStr);
        AccountSummary account = accounts.get(accountId);
        if (account == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        LocalDate dateFrom = localDateFromString(dateFromStr);
        LocalDate dateTo = localDateFromString(dateToStr);

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

        return Response.ok(result).build();
    }

    private List<TransactionDetail> randomTransactions(LocalDate from, LocalDate to) {
        List<TransactionDetail> result = new ArrayList<>();
        LocalDate date = LocalDate.now();
        for (int day = RandomUtils.insecure().randomInt(10, 30); day >= 0; --day) {
            result.addAll(randomTransactions(date, RandomUtils.insecure().randomInt(5, 10)));
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
            .remittanceInformationUnstructured(insecure().nextAlphanumeric(30))
            .build();
    }

    private CurrencyAmount randomAmount() {
        float amount = RandomUtils.insecure().randomFloat(10f, 100000f);
        if (RandomUtils.insecure().randomBoolean())
            amount = -amount;

        return CurrencyAmount.builder()
            .amount(amount)
            .currency("GBP")
            .build();
    }
}
