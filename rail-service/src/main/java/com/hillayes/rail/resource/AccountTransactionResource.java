package com.hillayes.rail.resource;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.PaginatedTransactions;
import com.hillayes.onestop.api.TransactionSummaryResponse;
import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.service.AccountTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.UUID;
import java.util.function.Function;

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
                                    @QueryParam("account-id") UUID accountId) {
        Page<AccountTransaction> transactionsPage =
            accountTransactionService.getTransactions(ResourceUtils.getUserId(ctx), accountId, page, pageSize);

        Function<UriBuilder, UriBuilder> uriDecorator = uriBuilder -> {
            if (accountId != null) {
                uriBuilder.queryParam("account-id", accountId);
            }
            return uriBuilder;
        };

        PaginatedTransactions response = new PaginatedTransactions()
            .page(transactionsPage.getNumber())
            .pageSize(transactionsPage.getSize())
            .count(transactionsPage.getNumberOfElements())
            .total(transactionsPage.getTotalElements())
            .items(transactionsPage.getContent().stream().map(this::marshal).toList())
            .links(ResourceUtils.buildPageLinks(uriInfo, transactionsPage, uriDecorator));

        return Response.ok(response).build();
    }

    @GET
    @Path("/{transactionId}")
    public Response getTransaction(@Context SecurityContext ctx, @PathParam("transactionId") UUID transactionId) {
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
            .date(transaction.getBookingDateTime())
            .description(transaction.remittanceInformationStructured == null
                ? transaction.creditorName
                : transaction.remittanceInformationStructured);
    }
}
