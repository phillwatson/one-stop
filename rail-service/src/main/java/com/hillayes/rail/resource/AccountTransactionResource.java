package com.hillayes.rail.resource;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.PaginatedTransactions;
import com.hillayes.onestop.api.TransactionList;
import com.hillayes.onestop.api.TransactionSummaryResponse;
import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.service.AccountService;
import com.hillayes.rail.service.AccountTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/rails/transactions")
@RolesAllowed("user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class AccountTransactionResource {
    private final AccountTransactionService accountTransactionService;
    private final AccountService accountService;

    @GET
    public Response getTransactions(@Context SecurityContext ctx,
                                    @Context UriInfo uriInfo,
                                    @QueryParam("page") @DefaultValue("0") int page,
                                    @QueryParam("page-size") @DefaultValue("20") int pageSize,
                                    @QueryParam("account-id") UUID accountId) {
        UUID userId = ResourceUtils.getUserId(ctx);
        log.info("Listing transactions [userId: {}, accountId: {}, page: {}, pageSize: {}]",
            userId, accountId, page, pageSize);

        verifyAccountHolder(userId, accountId);

        Page<AccountTransaction> transactionsPage =
            accountTransactionService.getTransactions(userId, accountId, page, pageSize);

        PaginatedTransactions response = new PaginatedTransactions()
            .page(transactionsPage.getNumber())
            .pageSize(transactionsPage.getSize())
            .count(transactionsPage.getNumberOfElements())
            .total(transactionsPage.getTotalElements())
            .items(transactionsPage.getContent().stream().map(this::marshal).toList())
            .links(ResourceUtils.buildPageLinks(uriInfo, transactionsPage, uriBuilder -> {
                if (accountId != null) {
                    uriBuilder.queryParam("account-id", accountId);
                }
                return uriBuilder;
            }));

        log.debug("Listing account transactions [userId: {}, accountId: {}, page: {}, pageSize: {}, count: {}, total: {}]",
            userId, accountId, page, pageSize, response.getCount(), response.getTotal());
        return Response.ok(response).build();
    }

    @GET
    @Path("/dates")
    public Response getTransactionsForDateRange(@Context SecurityContext ctx,
                                                @QueryParam("account-id") UUID accountId,
                                                @QueryParam("from-date") LocalDate fromDate,
                                                @QueryParam("to-date") LocalDate toDate) {
        UUID userId = ResourceUtils.getUserId(ctx);
        log.info("Listing transaction [userId: {}, accountId: {}, from: {}, to: {}]",
            userId, accountId, fromDate, toDate);

        verifyAccountHolder(userId, accountId);

        List<AccountTransaction> transactions =
            accountTransactionService.getTransactions(userId, accountId, fromDate, toDate);

        TransactionList response = new TransactionList()
            .transactions(transactions.stream().map(this::marshal).toList());
        return Response.ok(response).build();
    }

    @GET
    @Path("/{transactionId}")
    public Response getTransaction(@Context SecurityContext ctx, @PathParam("transactionId") UUID transactionId) {
        UUID userId = ResourceUtils.getUserId(ctx);
        log.info("Getting account transaction [userId: {}, transactionId: {}]", userId, transactionId);

        AccountTransaction transaction = accountTransactionService.getTransaction(transactionId)
            .filter(t -> t.getUserId().equals(ResourceUtils.getUserId(ctx)))
            .orElseThrow(() -> new NotFoundException("Transaction", transactionId));
        return Response.ok(marshal(transaction)).build();
    }

    private TransactionSummaryResponse marshal(AccountTransaction transaction) {
        return new TransactionSummaryResponse()
            .id(transaction.getId())
            .accountId(transaction.getAccountId())
            .amount(transaction.getTransactionAmount())
            .currency(transaction.getTransactionCurrencyCode())
            .date(transaction.getBookingDateTime())
            .description(transaction.getRemittanceInformationUnstructured() == null
                ? transaction.getDebtorName()
                : transaction.getRemittanceInformationUnstructured());
    }

    /**
     * Verifies that the identified user owns the identified account. If the account
     * ID is null, no verification is performed.
     * @param userId the user attempting to access the account.
     * @param accountId tehe optional account identifier.
     */
    private void verifyAccountHolder(UUID userId, UUID accountId) {
        if (accountId != null) {
            // verify that the account belongs to the user
            accountService.getAccount(accountId)
                .filter(account -> userId.equals(account.getUserId()) )
                .orElseThrow(() -> new NotFoundException("Account", accountId));
        }
    }
}
