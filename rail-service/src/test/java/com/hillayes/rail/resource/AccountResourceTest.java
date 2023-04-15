package com.hillayes.rail.resource;

import com.hillayes.onestop.api.AccountResponse;
import com.hillayes.onestop.api.PaginatedAccounts;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.model.Institution;
import com.hillayes.rail.service.AccountService;
import com.hillayes.rail.service.InstitutionService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.hillayes.rail.utils.TestData.mockAccount;
import static com.hillayes.rail.utils.TestData.mockInstitution;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        Map<String, Institution> banks = Map.of(
            accounts.get(0).getInstitutionId(), mockInstitution(),
            accounts.get(1).getInstitutionId(), mockInstitution(),
            accounts.get(2).getInstitutionId(), mockInstitution()
        );

        when(institutionService.get(any())).then(invocation -> {
            String institutionId = invocation.getArgument(0);
            return Optional.of(banks.get(institutionId));
        });

        // and: the account-service returns a paged list of the accounts
        PageRequest pageRequest = PageRequest.of(10, 20);
        Page<Account> pagedAccounts = new PageImpl<>(accounts, pageRequest, 310);
        when(accountService.getAccounts(any(UUID.class), anyInt(), anyInt()))
            .thenReturn(pagedAccounts);

        // when: client calls the endpoint
        PaginatedAccounts response = given()
            .request()
            .queryParam("page", pageRequest.getPageNumber())
            .queryParam("page-size", pageRequest.getPageSize())
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/accounts")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedAccounts.class);

        // then: the account-service is called with the authenticated user-id, and page parameters
        verify(accountService).getAccounts(userId, pageRequest.getPageNumber(), pageRequest.getPageSize());

        // and: the response corresponds to the paged list of accounts
        assertEquals(accounts.size(), response.getCount());
        assertNotNull(response.getItems());
        assertEquals(accounts.size(), response.getItems().size());
        assertEquals(pagedAccounts.getTotalElements(), response.getTotal());
        assertEquals(pageRequest.getPageNumber(), response.getPage());
        assertEquals(pageRequest.getPageSize(), response.getPageSize());

        // and: all page links are present
        assertEquals("/api/v1/rails/accounts?page=0&page-size=20", response.getLinks().getFirst());
        assertEquals("/api/v1/rails/accounts?page=11&page-size=20", response.getLinks().getNext());
        assertEquals("/api/v1/rails/accounts?page=9&page-size=20", response.getLinks().getPrevious());
        assertEquals("/api/v1/rails/accounts?page=15&page-size=20", response.getLinks().getLast());

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

            Institution institution = banks.get(account.getInstitutionId());
            assertNotNull(institution);
            assertEquals(institution.id, accountResponse.getBank().getId());
            assertEquals(institution.name, accountResponse.getBank().getName());
            assertEquals(institution.bic, accountResponse.getBank().getBic());
            assertEquals(institution.logo, accountResponse.getBank().getLogo());
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
        Map<String, Institution> banks = Map.of(
            accounts.get(0).getInstitutionId(), mockInstitution(),
            accounts.get(1).getInstitutionId(), mockInstitution(),
            accounts.get(2).getInstitutionId(), mockInstitution()
        );

        when(institutionService.get(any())).then(invocation -> {
            String institutionId = invocation.getArgument(0);
            return Optional.of(banks.get(institutionId));
        });

        // and: the account-service returns a paged list of the accounts
        PageRequest pageRequest = PageRequest.of(10, 20);
        Page<Account> pagedAccounts = new PageImpl<>(accounts, pageRequest, 310);
        when(accountService.getAccounts(any(UUID.class), anyInt(), anyInt()))
            .thenReturn(pagedAccounts);

        // when: client calls the endpoint
        int status = given()
            .request()
            .queryParam("page", pageRequest.getPageNumber())
            .queryParam("page-size", pageRequest.getPageSize())
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/accounts")
            .then()
            .extract().statusCode();

        // then: the request is forbidden
        assertEquals(403, status);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = { "admin", "user" })
    public void testGetAccounts_MultiRoleUser() {
        UUID userId = UUID.fromString(userIdStr);

        // given: a list of account belonging to the authenticated user
        List<Account> accounts = List.of(
            mockAccount(userId, UUID.randomUUID()),
            mockAccount(userId, UUID.randomUUID()),
            mockAccount(userId, UUID.randomUUID())
        );

        // and: a list of institutions linked to those accounts
        Map<String, Institution> banks = Map.of(
            accounts.get(0).getInstitutionId(), mockInstitution(),
            accounts.get(1).getInstitutionId(), mockInstitution(),
            accounts.get(2).getInstitutionId(), mockInstitution()
        );

        when(institutionService.get(any())).then(invocation -> {
            String institutionId = invocation.getArgument(0);
            return Optional.of(banks.get(institutionId));
        });

        // and: the account-service returns a paged list of the accounts
        PageRequest pageRequest = PageRequest.of(10, 20);
        Page<Account> pagedAccounts = new PageImpl<>(accounts, pageRequest, 310);
        when(accountService.getAccounts(any(UUID.class), anyInt(), anyInt()))
            .thenReturn(pagedAccounts);

        // when: client calls the endpoint
        int status = given()
            .request()
            .queryParam("page", pageRequest.getPageNumber())
            .queryParam("page-size", pageRequest.getPageSize())
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
        when(accountService.getAccount(account.getId())).thenReturn(Optional.of(account));

        // and: an institution linked to that account
        Institution institution = mockInstitution();
        when(institutionService.get(account.getInstitutionId())).thenReturn(Optional.of(institution));

        // when: client calls the endpoint
        AccountResponse accountResponse = given()
            .request()
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/accounts/" + account.getId())
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(AccountResponse.class);

        // then: the account is returned
        assertEquals(account.getId(), accountResponse.getId());
        assertEquals(account.getAccountName(), accountResponse.getName());
        assertEquals(account.getIban(), accountResponse.getIban());

        assertNotNull(institution);
        assertEquals(institution.id, accountResponse.getBank().getId());
        assertEquals(institution.name, accountResponse.getBank().getName());
        assertEquals(institution.bic, accountResponse.getBank().getBic());
        assertEquals(institution.logo, accountResponse.getBank().getLogo());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "admin")
    public void testGetAccountById_NotUserRole() {
        UUID userId = UUID.fromString(userIdStr);

        Account account = mockAccount(userId, UUID.randomUUID());
        when(accountService.getAccount(account.getId())).thenReturn(Optional.of(account));

        Institution bank = mockInstitution();
        when(institutionService.get(account.getInstitutionId())).thenReturn(Optional.of(bank));

        // when: the endpoint is called by a non-user role
        int status = given()
            .request()
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/accounts/" + account.getId())
            .then()
            .extract().statusCode();

        // then: the request is forbidden
        assertEquals(403, status);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = { "admin", "user" })
    public void testGetAccountById_MultipleRoleUser() {
        UUID userId = UUID.fromString(userIdStr);

        Account account = mockAccount(userId, UUID.randomUUID());
        when(accountService.getAccount(account.getId())).thenReturn(Optional.of(account));

        Institution bank = mockInstitution();
        when(institutionService.get(account.getInstitutionId())).thenReturn(Optional.of(bank));

        // when: the endpoint is called by a user with multiple roles
        int status = given()
            .request()
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/accounts/" + account.getId())
            .then()
            .extract().statusCode();

        // then: the request is successful
        assertEquals(200, status);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetAccountById_NotFound() {
        UUID userId = UUID.fromString(userIdStr);
        UUID accountId = UUID.randomUUID();
        when(accountService.getAccount(accountId)).thenReturn(Optional.empty());

        int status = given()
            .request()
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/accounts/" + accountId)
            .then()
            .extract().statusCode();

        // then: the account is not found
        assertEquals(404, status);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetAccountById_WrongUser() {
        UUID userId = UUID.fromString(userIdStr);

        // given: an account belonging to ANOTHER user
        Account account = mockAccount(UUID.randomUUID(), UUID.randomUUID());
        when(accountService.getAccount(account.getId())).thenReturn(Optional.of(account));

        // and: an institution linked to that account
        Institution institution = mockInstitution();
        when(institutionService.get(account.getInstitutionId())).thenReturn(Optional.of(institution));

        // when: client calls the endpoint
        int status = given()
            .request()
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/accounts/" + account.getId())
            .then()
            .extract().statusCode();

        // then: the account is not found
        assertEquals(404, status);
    }
}
