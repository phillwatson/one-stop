package com.hillayes.rail.resource;

import com.hillayes.auth.jwt.AuthUtils;
import com.hillayes.commons.MonetaryAmount;
import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.PaginatedTransactions;
import com.hillayes.onestop.api.PaginationUtils;
import com.hillayes.onestop.api.TransactionResponse;
import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.repository.TransactionFilter;
import com.hillayes.rail.service.AccountTransactionService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/v1/rails/transactions")
@RolesAllowed("user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class AccountTransactionResource {
    private final AccountTransactionService accountTransactionService;

    @GET
    public Response getTransactions(@Context SecurityContext ctx,
                                    @Context UriInfo uriInfo,
                                    @QueryParam("page") @DefaultValue("0") int page,
                                    @QueryParam("page-size") @DefaultValue("20") int pageSize,
                                    @QueryParam("account-id") UUID accountId,
                                    @QueryParam("from-date") LocalDate fromDate,
                                    @QueryParam("to-date") LocalDate toDate,
                                    @QueryParam("min-amount") Double minAmount,
                                    @QueryParam("max-amount") Double maxAmount,
                                    @QueryParam("reference") String refContaining,
                                    @QueryParam("info") String infoContaining,
                                    @QueryParam("creditor") String creditorContaining) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Listing transaction [userId: {}, accountId: {}, from: {}, to: {}]",
            userId, accountId, fromDate, toDate);

        TransactionFilter filter = TransactionFilter.builder()
            .userId(userId)
            .accountId(accountId)
            .minAmount(minAmount)
            .maxAmount(maxAmount)
            .reference(refContaining)
            .info(infoContaining)
            .creditor(creditorContaining)
            .build()
            .dateRange(fromDate, toDate);

        Page<AccountTransaction> transactionsPage =
            accountTransactionService.getTransactions(filter, page, pageSize);

        PaginatedTransactions response = new PaginatedTransactions()
            .page(transactionsPage.getPageIndex())
            .pageSize(transactionsPage.getPageSize())
            .count(transactionsPage.getContentSize())
            .total(transactionsPage.getTotalCount())
            .totalPages(transactionsPage.getTotalPages())
            .items(transactionsPage.getContent().stream().map(this::marshal).toList())
            .links(PaginationUtils.buildPageLinks(uriInfo, transactionsPage))
            .currencyTotals(
                accountTransactionService.getTransactionTotals(filter)
                    .stream()
                    .collect(Collectors.toMap(t -> t.getCurrency().getCurrencyCode(), MonetaryAmount::toDecimal))
            );

        log.debug("Listing account transactions [userId: {}, accountId: {}, page: {}, pageSize: {}, count: {}, total: {}]",
            userId, accountId, page, pageSize, response.getCount(), response.getTotal());
        return Response.ok(response).build();
    }

    @GET
    @Path("/{transactionId}")
    public Response getTransaction(@Context SecurityContext ctx, @PathParam("transactionId") UUID transactionId) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Getting account transaction [userId: {}, transactionId: {}]", userId, transactionId);

        AccountTransaction transaction = accountTransactionService.getTransaction(transactionId)
            .filter(t -> t.getUserId().equals(userId))
            .orElseThrow(() -> new NotFoundException("Transaction", transactionId));
        return Response.ok(marshal(transaction)).build();
    }

    private TransactionResponse marshal(AccountTransaction transaction) {
        return new TransactionResponse()
            .id(transaction.getId())
            .accountId(transaction.getAccountId())
            .transactionId(transaction.getTransactionId())
            .amount(transaction.getAmount().toDecimal())
            .currency(transaction.getAmount().getCurrencyCode())
            .bookingDateTime(transaction.getBookingDateTime())
            .valueDateTime(transaction.getValueDateTime())
            .reference(transaction.getReference())
            .additionalInformation(transaction.getAdditionalInformation())
            .creditorName(transaction.getCreditorName());
    }
}
