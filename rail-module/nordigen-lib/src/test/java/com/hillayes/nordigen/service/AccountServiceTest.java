package com.hillayes.nordigen.service;

import com.hillayes.nordigen.api.AccountApi;
import com.hillayes.nordigen.model.*;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextFloat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AccountServiceTest {
    AccountApi accountApi;

    AccountService fixture;

    @BeforeEach
    public void beforeEach() {
        accountApi = mock();

        fixture = new AccountService();
        fixture.accountApi = accountApi;
    }

    @Test
    public void testGet_HappyPath() {
        // given: a rail-account
        AccountSummary accountSummary = AccountSummary.builder()
            .id(randomAlphanumeric(20))
            .status(AccountStatus.READY)
            .institutionId(randomAlphanumeric(20))
            .build();
        when(accountApi.get(accountSummary.id)).thenReturn(accountSummary);

        // when: the service is called
        Optional<AccountSummary> result = fixture.get(accountSummary.id);

        // then: the account is returned
        assertTrue(result.isPresent());

        // and: the account is correct
        assertEquals(accountSummary.id, result.get().id);
    }

    @Test
    public void testGet_NotFound() {
        // given: the identified rail-account does not exist
        String id = randomAlphanumeric(20);
        when(accountApi.get(id)).thenThrow(new WebApplicationException(Response.Status.NOT_FOUND));

        // when: the service is called
        Optional<AccountSummary> result = fixture.get(id);

        // then: NO account is returned
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGet_ServerError() {
        // given: the rail-account cannot be retrieved
        String id = randomAlphanumeric(20);
        when(accountApi.get(id)).thenThrow(new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR));

        // when: the service is called
        // then: the server error is passed back
        assertThrows(WebApplicationException.class, () ->
            fixture.get(id)
        );
    }

    @Test
    public void testBalances_HappyPath() {
        // given: a rail-account id
        String accountId = randomAlphanumeric(20);

        // and: the account has available balances
        AccountBalanceList balanceList = new AccountBalanceList();
        balanceList.balances = List.of(
            Balance.builder()
                .balanceType("expected")
                .balanceAmount(CurrencyAmount.builder().currency("GBP").amount(nextFloat()).build())
                .referenceDate(LocalDate.now())
                .build(),
            Balance.builder()
                .balanceType("resolved")
                .balanceAmount(CurrencyAmount.builder().currency("GBP").amount(nextFloat()).build())
                .referenceDate(LocalDate.now())
                .build()
        );
        when(accountApi.balances(accountId)).thenReturn(balanceList);

        // when: the service is called
        Optional<List<Balance>> result = fixture.balances(accountId);

        // then: the result is not empty
        assertTrue(result.isPresent());

        // and: the balances are as provided
        List<Balance> resultList = result.get();
        assertEquals(balanceList.balances.size(), result.get().size());
        balanceList.balances.forEach(expected -> {
            Balance actual = resultList.stream()
                .filter(b -> b.balanceType.equals(expected.balanceType))
                .findFirst().orElse(null);
            assertNotNull(actual);
            assertEquals(expected.balanceAmount, actual.balanceAmount);
        });
    }

    @Test
    public void testBalances_NotAvailable() {
        // given: a rail-account id
        String accountId = randomAlphanumeric(20);

        // and: the account has NO available balances
        when(accountApi.balances(accountId)).thenReturn(null);

        // when: the service is called
        Optional<List<Balance>> result = fixture.balances(accountId);

        // then: the result is not empty
        assertTrue(result.isPresent());

        // and: the list is empty
        assertTrue(result.get().isEmpty());
    }

    @Test
    public void testBalances_NotAvailable_Empty() {
        // given: a rail-account id
        String accountId = randomAlphanumeric(20);

        // and: the account has NO available balances
        AccountBalanceList balanceList = new AccountBalanceList();
        when(accountApi.balances(accountId)).thenReturn(balanceList);

        // when: the service is called
        Optional<List<Balance>> result = fixture.balances(accountId);

        // then: the result is not empty
        assertTrue(result.isPresent());

        // and: the list is empty
        assertTrue(result.get().isEmpty());
    }

    @Test
    public void testBalances_NotFound() {
        // given: an unknown rail-account id
        String accountId = randomAlphanumeric(20);

        // and: no balances are found
        when(accountApi.balances(accountId)).thenThrow(new WebApplicationException(Response.Status.NOT_FOUND));

        // when: the service is called
        Optional<List<Balance>> result = fixture.balances(accountId);

        // then: the result is empty
        assertTrue(result.isEmpty());
    }

    @Test
    public void testBalances_ServiceError() {
        // given: a rail-account id
        String accountId = randomAlphanumeric(20);

        // and: no balances are found
        when(accountApi.balances(accountId)).thenThrow(new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR));

        // when: the service is called
        // then: the server error is passed back
        assertThrows(WebApplicationException.class, () ->
            fixture.balances(accountId)
        );
    }

    @Test
    public void testDetails_HappyPath() {
        // given: a rail-account id
        String accountId = randomAlphanumeric(20);

        // and: the account details are available
        Map<String, Object> details = Map.of("key", "value");
        when(accountApi.details(accountId)).thenReturn(details);

        // when: the service is called
        Optional<Map<String, Object>> result = fixture.details(accountId);

        // then: the result is not empty
        assertTrue(result.isPresent());

        // and: the content is as expected
        assertEquals(details.size(), result.get().size());
        details.forEach((k, v) -> assertEquals(v, result.get().get(k)));
    }

    @Test
    public void testDetails_NotFound() {
        // given: an unknown rail-account id
        String accountId = randomAlphanumeric(20);

        // and: the account details are not found
        when(accountApi.details(accountId)).thenThrow(new WebApplicationException(Response.Status.NOT_FOUND));

        // when: the service is called
        Optional<Map<String, Object>> result = fixture.details(accountId);

        // then: the result is empty
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDetails_ServiceError() {
        // given: a rail-account id
        String accountId = randomAlphanumeric(20);

        // and: the account details are available
        when(accountApi.details(accountId)).thenThrow(new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR));

        // when: the service is called
        // then: the server error is passed back
        assertThrows(WebApplicationException.class, () ->
            fixture.details(accountId)
        );
    }

    @Test
    public void testTransactions_HappyPath() {
        // given: a rail-account id
        String accountId = randomAlphanumeric(20);

        // and: an available collection of transactions
        List<TransactionDetail> booked = List.of(
            TransactionDetail.builder().transactionId(randomAlphanumeric(20)).build(),
            TransactionDetail.builder().transactionId(randomAlphanumeric(20)).build()
        );
        List<TransactionDetail> pending = List.of(
            TransactionDetail.builder().transactionId(randomAlphanumeric(20)).build(),
            TransactionDetail.builder().transactionId(randomAlphanumeric(20)).build()
        );
        TransactionsResponse t = new TransactionsResponse();
        t.transactions = TransactionList.builder().booked(booked).pending(pending).build();
        when(accountApi.transactions(eq(accountId), any(), any())).thenReturn(t);

        // when: the service is called
        Optional<TransactionList> result = fixture
            .transactions(accountId, LocalDate.now().minusWeeks(1), LocalDate.now());

        // then: the result is not empty
        assertTrue(result.isPresent());

        // and: the content is as expected
        booked.forEach(expected -> {
            assertNotNull(result.get().booked.stream()
                .filter(trans -> trans.transactionId.equals(expected.transactionId))
                .findFirst().orElse(null));
        });
        pending.forEach(expected -> {
            assertNotNull(result.get().pending.stream()
                .filter(trans -> trans.transactionId.equals(expected.transactionId))
                .findFirst().orElse(null));
        });
    }

    @Test
    public void testTransactions_EmptyResponse() {
        // given: a rail-account id
        String accountId = randomAlphanumeric(20);

        // and: NO transactions are available
        when(accountApi.transactions(eq(accountId), any(), any())).thenReturn(new TransactionsResponse());

        // when: the service is called
        Optional<TransactionList> result = fixture
            .transactions(accountId, LocalDate.now().minusWeeks(1), LocalDate.now());

        // then: the result is not empty
        assertTrue(result.isPresent());

        // and: the content is as empty
        assertTrue(result.get().booked.isEmpty());
        assertTrue(result.get().pending.isEmpty());
    }

    @Test
    public void testTransactions_NullResponse() {
        // given: a rail-account id
        String accountId = randomAlphanumeric(20);

        // and: NO transactions are available
        when(accountApi.transactions(eq(accountId), any(), any())).thenReturn(null);

        // when: the service is called
        Optional<TransactionList> result = fixture
            .transactions(accountId, LocalDate.now().minusWeeks(1), LocalDate.now());

        // then: the result is not empty
        assertTrue(result.isPresent());

        // and: the content is as empty
        assertTrue(result.get().booked.isEmpty());
        assertTrue(result.get().pending.isEmpty());
    }

    @Test
    public void testTransactions_NotFound() {
        // given: a rail-account id
        String accountId = randomAlphanumeric(20);

        // and: NO transactions are not found
        when(accountApi.transactions(eq(accountId), any(), any()))
            .thenThrow(new WebApplicationException(Response.Status.NOT_FOUND));

        // when: the service is called
        Optional<TransactionList> result = fixture
            .transactions(accountId, LocalDate.now().minusWeeks(1), LocalDate.now());

        // then: the result is empty
        assertTrue(result.isEmpty());
    }

    @Test
    public void testTransactions_ServerError() {
        // given: a rail-account id
        String accountId = randomAlphanumeric(20);

        // and: NO transactions are not found
        when(accountApi.transactions(eq(accountId), any(), any()))
            .thenThrow(new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR));

        // when: the service is called
        // then: the server error is passed back
        assertThrows(WebApplicationException.class, () ->
            fixture.transactions(accountId, LocalDate.now().minusWeeks(1), LocalDate.now())
        );
    }
}
