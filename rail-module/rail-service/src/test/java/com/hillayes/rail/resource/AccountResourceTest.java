package com.hillayes.rail.resource;

import com.hillayes.commons.jpa.Page;
import com.hillayes.onestop.api.*;
import com.hillayes.rail.api.domain.RailInstitution;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.AccountBalance;
import com.hillayes.rail.service.AccountService;
import com.hillayes.rail.service.InstitutionService;
import com.hillayes.rail.utils.TestApiData;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.hillayes.rail.utils.TestData.*;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class AccountResourceTest extends TestBase {
    @InjectMock
    AccountService accountService;

    @InjectMock
    InstitutionService institutionService;

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetAccounts() {
        UUID userId = UUID.fromString(userIdStr);

        // given: a list of account belonging to the authenticated user
        List<Account> accounts = List.of(
            mockAccount(userId, UUID.randomUUID()),
            mockAccount(userId, UUID.randomUUID()),
            mockAccount(userId, UUID.randomUUID())
        );

        // and: a list of institutions linked to those accounts
        Map<String, RailInstitution> banks = Map.of(
            accounts.get(0).getInstitutionId(), TestApiData.mockInstitution(),
            accounts.get(1).getInstitutionId(), TestApiData.mockInstitution(),
            accounts.get(2).getInstitutionId(), TestApiData.mockInstitution()
        );

        when(institutionService.get(any())).then(invocation -> {
            String institutionId = invocation.getArgument(0);
            return Optional.of(banks.get(institutionId));
        });

        // and: the account-service returns a paged list of the accounts
        Page<Account> pagedAccounts = new Page<>(accounts, 310, 10, 20);
        when(accountService.getAccounts(any(UUID.class), anyInt(), anyInt()))
            .thenReturn(pagedAccounts);

        // when: client calls the endpoint
        PaginatedAccounts response = given()
            .request()
            .queryParam("page", pagedAccounts.getPageIndex())
            .queryParam("page-size", pagedAccounts.getPageSize())
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/accounts")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedAccounts.class);

        // then: the account-service is called with the authenticated user-id, and page parameters
        verify(accountService).getAccounts(userId, pagedAccounts.getPageIndex(), pagedAccounts.getPageSize());

        // and: the response corresponds to the paged list of accounts
        assertEquals(accounts.size(), response.getCount());
        assertNotNull(response.getItems());
        assertEquals(accounts.size(), response.getItems().size());
        assertEquals(pagedAccounts.getTotalCount(), response.getTotal());
        assertEquals(pagedAccounts.getPageIndex(), response.getPage());
        assertEquals(pagedAccounts.getPageSize(), response.getPageSize());

        // and: all page links are present
        PageLinks links = response.getLinks();
        assertEquals("/api/v1/rails/accounts", links.getFirst().getPath());
        assertTrue(links.getFirst().getQuery().contains("page-size=20"));
        assertTrue(links.getFirst().getQuery().contains("page=0"));

        assertNotNull(links.getPrevious());
        assertEquals("/api/v1/rails/accounts", links.getPrevious().getPath());
        assertTrue(links.getPrevious().getQuery().contains("page-size=20"));
        assertTrue(links.getPrevious().getQuery().contains("page=9"));

        assertNotNull(links.getNext());
        assertEquals("/api/v1/rails/accounts", links.getNext().getPath());
        assertTrue(links.getNext().getQuery().contains("page-size=20"));
        assertTrue(links.getNext().getQuery().contains("page=11"));

        assertEquals("/api/v1/rails/accounts", links.getLast().getPath());
        assertTrue(links.getLast().getQuery().contains("page-size=20"));
        assertTrue(links.getLast().getQuery().contains("page=15"));

        // and: each account is found in the response
        accounts.forEach(account -> {
            var accountResponse = response.getItems().stream()
                .filter(a -> a.getId().equals(account.getId()))
                .findFirst()
                .orElse(null);
            assertNotNull(accountResponse);

            assertEquals(account.getId(), accountResponse.getId());
            assertEquals(account.getAccountName(), accountResponse.getName());
            assertEquals(account.getIban(), accountResponse.getIban());

            RailInstitution institution = banks.get(account.getInstitutionId());
            assertNotNull(institution);
            assertNotNull(accountResponse.getInstitution());
            assertEquals(institution.getId(), accountResponse.getInstitution().getId());
            assertEquals(institution.getName(), accountResponse.getInstitution().getName());
            assertEquals(institution.getBic(), accountResponse.getInstitution().getBic());
            assertEquals(institution.getLogo(), accountResponse.getInstitution().getLogo());
        });
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "admin")
    public void testGetAccounts_NotUserRole() {
        UUID userId = UUID.fromString(userIdStr);

        // given: a list of account belonging to the authenticated user
        List<Account> accounts = List.of(
            mockAccount(userId, UUID.randomUUID()),
            mockAccount(userId, UUID.randomUUID()),
            mockAccount(userId, UUID.randomUUID())
        );

        // and: a list of institutions linked to those accounts
        Map<String, RailInstitution> banks = Map.of(
            accounts.get(0).getInstitutionId(), TestApiData.mockInstitution(),
            accounts.get(1).getInstitutionId(), TestApiData.mockInstitution(),
            accounts.get(2).getInstitutionId(), TestApiData.mockInstitution()
        );

        when(institutionService.get(any())).then(invocation -> {
            String institutionId = invocation.getArgument(0);
            return Optional.of(banks.get(institutionId));
        });

        // and: the account-service returns a paged list of the accounts
        Page<Account> pagedAccounts = new Page<>(accounts, 310, 10, 20);
        when(accountService.getAccounts(any(UUID.class), anyInt(), anyInt()))
            .thenReturn(pagedAccounts);

        // when: client calls the endpoint
        int status = given()
            .request()
            .queryParam("page", pagedAccounts.getPageIndex())
            .queryParam("page-size", pagedAccounts.getPageSize())
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/accounts")
            .then()
            .extract().statusCode();

        // then: the request is forbidden
        assertEquals(403, status);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = {"admin", "user"})
    public void testGetAccounts_MultiRoleUser() {
        UUID userId = UUID.fromString(userIdStr);

        // given: a list of account belonging to the authenticated user
        List<Account> accounts = List.of(
            mockAccount(userId, UUID.randomUUID()),
            mockAccount(userId, UUID.randomUUID()),
            mockAccount(userId, UUID.randomUUID())
        );

        // and: a list of institutions linked to those accounts
        Map<String, RailInstitution> banks = Map.of(
            accounts.get(0).getInstitutionId(), TestApiData.mockInstitution(),
            accounts.get(1).getInstitutionId(), TestApiData.mockInstitution(),
            accounts.get(2).getInstitutionId(), TestApiData.mockInstitution()
        );

        when(institutionService.get(any())).then(invocation -> {
            String institutionId = invocation.getArgument(0);
            return Optional.of(banks.get(institutionId));
        });

        // and: the account-service returns a paged list of the accounts
        Page<Account> pagedAccounts = new Page<>(accounts, 310, 10, 20);
        when(accountService.getAccounts(any(UUID.class), anyInt(), anyInt()))
            .thenReturn(pagedAccounts);

        // when: client calls the endpoint
        int status = given()
            .request()
            .queryParam("page", pagedAccounts.getPageIndex())
            .queryParam("page-size", pagedAccounts.getPageSize())
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/accounts")
            .then()
            .extract().statusCode();

        // then: the request is successful
        assertEquals(200, status);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetAccountById() {
        UUID userId = UUID.fromString(userIdStr);

        // given: an account belonging to the authenticated user
        Account account = mockAccount(userId, UUID.randomUUID());
        when(accountService.getAccount(userId, account.getId())).thenReturn(Optional.of(account));

        // and: the account's recent balance records
        List<AccountBalance> balances = List.of(
            mockAccountBalance(account, (a) -> a.balanceType("expected")),
            mockAccountBalance(account, (a) -> a.balanceType("interimAvailable"))
        );
        when(accountService.getMostRecentBalance(account)).thenReturn(balances);

        // and: an institution linked to that account
        RailInstitution institution = TestApiData.mockInstitution();
        when(institutionService.get(account.getInstitutionId())).thenReturn(Optional.of(institution));

        // when: client calls the endpoint
        AccountResponse accountResponse = given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("accountId", account.getId())
            .get("/api/v1/rails/accounts/{accountId}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(AccountResponse.class);

        // then: the account is returned
        assertEquals(account.getId(), accountResponse.getId());
        assertEquals(account.getAccountName(), accountResponse.getName());
        assertEquals(account.getIban(), accountResponse.getIban());

        // and: the institution is included
        assertNotNull(accountResponse.getInstitution());
        assertEquals(institution.getId(), accountResponse.getInstitution().getId());
        assertEquals(institution.getName(), accountResponse.getInstitution().getName());
        assertEquals(institution.getBic(), accountResponse.getInstitution().getBic());
        assertEquals(institution.getLogo(), accountResponse.getInstitution().getLogo());

        // and: the recent balances are included
        assertNotNull(accountResponse.getBalance());
        assertEquals(balances.size(), accountResponse.getBalance().size());
        for (AccountBalance expected : balances) {
            AccountBalanceResponse actual = accountResponse.getBalance().stream()
                .filter(b -> b.getId().equals(expected.getId()))
                .findFirst().orElse(null);
            assertNotNull(actual);
            assertEquals(expected.getAmount().toDecimal(), actual.getAmount());
            assertEquals(expected.getReferenceDate(), actual.getReferenceDate());
            assertEquals(expected.getBalanceType(), actual.getType());
            assertEquals(expected.getDateCreated(), actual.getDateRecorded());
        }
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "admin")
    public void testGetAccountById_NotUserRole() {
        UUID userId = UUID.fromString(userIdStr);

        // given: an account belonging to the authenticated user
        Account account = mockAccount(userId, UUID.randomUUID());
        when(accountService.getAccount(userId, account.getId())).thenReturn(Optional.of(account));

        // and: an institution linked to that account
        RailInstitution bank = TestApiData.mockInstitution();
        when(institutionService.get(account.getInstitutionId())).thenReturn(Optional.of(bank));

        // when: the endpoint is called by a non-user role
        int status = given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("accountId", account.getId())
            .get("/api/v1/rails/accounts/{accountId}")
            .then()
            .extract().statusCode();

        // then: the request is forbidden
        assertEquals(403, status);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = {"admin", "user"})
    public void testGetAccountById_MultipleRoleUser() {
        UUID userId = UUID.fromString(userIdStr);

        // given: an account belonging to the authenticated user
        Account account = mockAccount(userId, UUID.randomUUID());
        when(accountService.getAccount(userId, account.getId())).thenReturn(Optional.of(account));

        // and: an institution linked to that account
        RailInstitution bank = TestApiData.mockInstitution();
        when(institutionService.get(account.getInstitutionId())).thenReturn(Optional.of(bank));

        // when: the endpoint is called by a user with multiple roles
        AccountResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("accountId", account.getId())
            .get("/api/v1/rails/accounts/{accountId}")
            .then()
            .statusCode(200)
            .extract().as(AccountResponse.class);

        // then: the identified account is returned
        assertEquals(account.getId(), response.getId());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetAccountById_NotFound() {
        UUID userId = UUID.fromString(userIdStr);

        // given: the an unknown account
        UUID accountId = UUID.randomUUID();
        when(accountService.getAccount(userId, accountId)).thenReturn(Optional.empty());

        ServiceErrorResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("accountId", accountId)
            .get("/api/v1/rails/accounts/{accountId}")
            .then()
            .statusCode(404)
            .contentType(JSON)
            .extract()
            .as(ServiceErrorResponse.class);

        // then: the account service was called
        verify(accountService).getAccount(userId, accountId);

        // and: the response contains the expected error message
        assertNotNull(response);

        // and: the response contains an error message
        assertNotFoundError(response, (contextAttributes) -> {
            assertEquals("Account", contextAttributes.get("entity-type"));
            assertEquals(accountId.toString(), contextAttributes.get("entity-id"));
        });
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetAccountById_WrongUser() {
        UUID userId = UUID.fromString(userIdStr);

        // given: an account belonging to ANOTHER user
        Account account = mockAccount(UUID.randomUUID(), UUID.randomUUID());
        when(accountService.getAccount(userId, account.getId())).thenReturn(Optional.empty());

        // and: an institution linked to that account
        RailInstitution institution = TestApiData.mockInstitution();
        when(institutionService.get(account.getInstitutionId())).thenReturn(Optional.of(institution));

        // when: client calls the endpoint
        int status = given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("accountId", account.getId())
            .get("/api/v1/rails/accounts/{accountId}")
            .then()
            .extract().statusCode();

        // then: the account is not found
        assertEquals(404, status);

        // and: the service was called with the calling user's ID
        verify(accountService).getAccount(userId, account.getId());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testDeleteAccount() {
        UUID userId = UUID.fromString(userIdStr);
        UUID accountId = UUID.randomUUID();

        // given: the account-service returns true when deleting the account
        when(accountService.deleteAccount(userId, accountId)).thenReturn(true);

        // when: client calls the endpoint
        int status = given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("accountId", accountId)
            .delete("/api/v1/rails/accounts/{accountId}")
            .then()
            .extract().statusCode();

        // then: the account-service is called with the authenticated user-id, and the account-id
        verify(accountService).deleteAccount(userId, accountId);

        // and: the request is successful
        assertEquals(204, status);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testDeleteAccount_NotFound() {
        UUID userId = UUID.fromString(userIdStr);
        UUID accountId = UUID.randomUUID();

        // given: the account-service returns false when deleting the account
        when(accountService.deleteAccount(userId, accountId)).thenReturn(false);

        // when: client calls the endpoint
        ServiceErrorResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("accountId", accountId)
            .delete("/api/v1/rails/accounts/{accountId}")
            .then()
            .statusCode(404)
            .contentType(JSON)
            .extract()
            .as(ServiceErrorResponse.class);

        // then: the account-service is called with the authenticated user-id, and the account-id
        verify(accountService).deleteAccount(userId, accountId);

        // and: the response contains the expected error message
        assertNotNull(response);

        // and: the response contains an error message
        assertNotFoundError(response, (contextAttributes) -> {
            assertEquals("Account", contextAttributes.get("entity-type"));
            assertEquals(accountId.toString(), contextAttributes.get("entity-id"));
        });
    }
}
