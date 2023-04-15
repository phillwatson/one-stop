package com.hillayes.rail.resource;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.PageLinks;
import com.hillayes.onestop.api.PaginatedTransactions;
import com.hillayes.onestop.api.TransactionSummaryResponse;
import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.service.AccountTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.UUID;

@Path("/api/v1/rails/transactions")
@RolesAllowed("user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class AccountTransactionResource extends AbstractResource {
    private static final String PAGE_LINK = "%s?page=%d&page-size=%d";

    @Inject
    AccountTransactionService accountTransactionService;

    @GET
    public Response getTransactions(@Context SecurityContext ctx,
                                    @Context UriInfo uriInfo,
                                    @QueryParam("page") @DefaultValue("0") int page,
                                    @QueryParam("page-size") @DefaultValue("20") int pageSize,
                                    @QueryParam("account-id") UUID accountId) {
        Page<AccountTransaction> transactionsPage =
            accountTransactionService.getTransactions(getUserId(ctx), accountId, page, pageSize);

        PaginatedTransactions response = new PaginatedTransactions()
            .page(page)
            .pageSize(pageSize)
            .count(transactionsPage.getNumberOfElements())
            .total(transactionsPage.getTotalElements())
            .items(transactionsPage.getContent().stream().map(this::marshal).toList())
            .links(new PageLinks()
                .first(getPageLink(uriInfo.getPath(),0, pageSize, accountId))
                .last(getPageLink(uriInfo.getPath(),transactionsPage.getTotalPages() - 1, pageSize, accountId))
            );

        if (page > 0) {
            response.getLinks().previous(getPageLink(uriInfo.getPath(), page - 1, pageSize, accountId));
        }

        if (page < transactionsPage.getTotalPages() - 1) {
            response.getLinks().next(getPageLink(uriInfo.getPath(),page + 1, pageSize, accountId));
        }

        return Response.ok(response).build();
    }

    @GET
    @Path("/{transactionId}")
    public Response getTransaction(@Context SecurityContext ctx, @PathParam("transactionId") UUID transactionId) {
        AccountTransaction transaction = accountTransactionService.getTransaction(transactionId)
            .filter(t -> t.getUserId().equals(getUserId(ctx)))
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

    private String getPageLink(String path, int page, int pageSize, UUID accountId) {
        String result = String.format(PAGE_LINK, path, page, pageSize);
        if (accountId != null) {
            result += "&account-id=" + accountId;
        }
        return result;
    }
}
