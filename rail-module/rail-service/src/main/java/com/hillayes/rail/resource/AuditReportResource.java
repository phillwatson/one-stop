package com.hillayes.rail.resource;

import com.hillayes.auth.jwt.AuthUtils;
import com.hillayes.commons.Strings;
import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.*;
import com.hillayes.rail.audit.AuditReportTemplate;
import com.hillayes.rail.domain.*;
import com.hillayes.rail.service.AccountTransactionService;
import com.hillayes.rail.service.AuditReportService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/v1/rails/audit")
@RolesAllowed("user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class AuditReportResource {
    private final AuditReportService auditReportService;
    private final AccountTransactionService accountTransactionService;

    @GET
    @Path("/templates")
    public Response getAuditTemplates(@Context UriInfo uriInfo,
                                      @QueryParam("page") @DefaultValue("0") int page,
                                      @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        log.info("Getting audit report templates [page: {}, pageSize: {}]", page, pageSize);
        Page<AuditReportTemplate> templates = auditReportService.getAuditTemplates(page, pageSize);

        PaginatedAuditTemplates response = new PaginatedAuditTemplates()
            .page(templates.getPageIndex())
            .pageSize(templates.getPageSize())
            .count(templates.getContentSize())
            .total(templates.getTotalCount())
            .totalPages(templates.getTotalPages())
            .items(templates.getContent().stream().map(this::marshal).toList())
            .links(PaginationUtils.buildPageLinks(uriInfo, templates));

        if (log.isDebugEnabled()) {
            log.debug("Listing audit report templates [page: {}, pageSize: {}, count: {}, total: {}]",
                page, pageSize, response.getCount(), response.getTotal());
        }
        return Response.ok(response).build();
    }

    @GET
    @Path("/configs")
    public Response getAuditConfigs(@Context SecurityContext ctx,
                                    @Context UriInfo uriInfo,
                                    @QueryParam("page") @DefaultValue("0") int page,
                                    @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Getting audit report configs [userId: {}, page: {}, pageSize: {}]", userId, page, pageSize);
        Page<AuditReportConfig> reportConfigs = auditReportService.getAuditConfigs(userId, page, pageSize);

        PaginatedAuditConfigs response = new PaginatedAuditConfigs()
            .page(reportConfigs.getPageIndex())
            .pageSize(reportConfigs.getPageSize())
            .count(reportConfigs.getContentSize())
            .total(reportConfigs.getTotalCount())
            .totalPages(reportConfigs.getTotalPages())
            .items(reportConfigs.getContent().stream().map(this::marshal).toList())
            .links(PaginationUtils.buildPageLinks(uriInfo, reportConfigs));

        if (log.isDebugEnabled()) {
            log.debug("Listing audit report configs [userId: {}, page: {}, pageSize: {}, count: {}, total: {}]",
                userId, page, pageSize, response.getCount(), response.getTotal());
        }
        return Response.ok(response).build();
    }

    @POST
    @Path("/configs")
    public Response createAuditConfig(@Context SecurityContext ctx,
                                      @Context UriInfo uriInfo,
                                      AuditReportConfigRequest request) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Creating audit report config [userId: {}, name: {}]", userId, request.getName());

        AuditReportConfig config = auditReportService.createAuditConfig(userId, unmarshal(request));

        URI location = uriInfo.getBaseUriBuilder()
            .path(AuditReportResource.class)
            .path(AuditReportResource.class, "getAuditConfig")
            .buildFromMap(Map.of("configId", config.getId()));
        return Response.created(location)
            .entity(marshal(config)).build();
    }

    @GET
    @Path("/configs/{configId}")
    public Response getAuditConfig(@Context SecurityContext ctx,
                                   @PathParam("configId") UUID configId) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Getting audit report config [userId: {}, configId: {}]", userId, configId);
        AuditReportConfig config = auditReportService.getAuditConfig(userId, configId);
        return Response.ok(marshal(config)).build();
    }

    @PUT
    @Path("/configs/{configId}")
    public Response updateAuditConfig(@Context SecurityContext ctx,
                                      @PathParam("configId") UUID configId,
                                      AuditReportConfigRequest request) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Updating audit report config [userId: {}, configId: {}]", userId, configId);

        AuditReportConfig result = auditReportService.updateAuditConfig(userId, configId, unmarshal(request));
        return Response.ok(marshal(result)).build();
    }

    @DELETE
    @Path("/configs/{configId}")
    public Response deleteAuditConfig(@Context SecurityContext ctx,
                                      @PathParam("configId") UUID configId) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Deleting audit report config [userId: {}, configId: {}]", userId, configId);

        auditReportService.deleteAuditConfig(userId, configId);
        return Response.noContent().build();
    }

    @GET
    @Path("/configs/{configId}/issues")
    public Response getAuditIssues(@Context SecurityContext ctx,
                                   @Context UriInfo uriInfo,
                                   @PathParam("configId") UUID configId,
                                   @QueryParam("acknowledged") NullableParam acknowledgedParam,
                                   @QueryParam("page") @DefaultValue("0") int page,
                                   @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Getting audit issues [userId: {}, configId: {}, acknowledged: {}, page: {}, pageSize: {}]",
            userId, configId, acknowledgedParam, page, pageSize);

        Boolean acknowledged = (acknowledgedParam == null) ? null : acknowledgedParam.asBoolean();
        Page<AuditIssue> issues = auditReportService.getAuditIssues(userId, configId, acknowledged, page, pageSize);

        // find the transactions referenced by the issues - key on their IDs
        Set<UUID> transactionIds = issues.stream().map(AuditIssue::getTransactionId).collect(Collectors.toSet());
        Map<UUID, AccountTransaction> accountTransactions =
            accountTransactionService.listAll(transactionIds).stream()
                .collect(Collectors.toMap(AccountTransaction::getId, t -> t));

        PaginatedAuditIssues response = new PaginatedAuditIssues()
            .page(issues.getPageIndex())
            .pageSize(issues.getPageSize())
            .count(issues.getContentSize())
            .total(issues.getTotalCount())
            .totalPages(issues.getTotalPages())
            .items(issues.getContent().stream()
                .map(issue -> marshal(issue, accountTransactions.get(issue.getTransactionId())))
                .toList())
            .links(PaginationUtils.buildPageLinks(uriInfo, issues));

        if (log.isDebugEnabled()) {
            log.debug("Listing audit issues [userId: {}, configId: {}, acknowledged: {}, page: {}, pageSize: {}, count: {}, total: {}]",
                userId, configId, acknowledged, page, pageSize, response.getCount(), response.getTotal());
        }
        return Response.ok(response).build();
    }

    @GET
    @Path("/summaries")
    public Response getAuditIssueSummaries(@Context SecurityContext ctx) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Getting audit issue summaries [userId: {}]", userId);

        List<AuditIssueSummary> summaries = auditReportService.getIssueSummaries(userId);
        return Response.ok(summaries).build();
    }

    @GET
    @Path("/issues/{issueId}")
    public Response getAuditIssue(@Context SecurityContext ctx,
                                  @PathParam("issueId") UUID issueId) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Getting audit issue [userId: {}, issueId: {}]", userId, issueId);

        AuditIssue auditIssue = auditReportService.getAuditIssue(userId, issueId);
        return Response.ok(marshal(auditIssue)).build();
    }

    @PUT
    @Path("/issues/{issueId}")
    public Response updateAuditIssue(@Context SecurityContext ctx,
                                     @PathParam("issueId") UUID issueId,
                                     AuditIssueRequest request) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Updating audit issue [userId: {}, issueId: {}, acknowledged: {}]",
            userId, issueId, request.getAcknowledged());

        AuditIssue auditIssue = auditReportService.updateIssue(userId, issueId, request.getAcknowledged());
        return Response.ok(marshal(auditIssue)).build();
    }

    @DELETE
    @Path("/issues/{issueId}")
    public Response deleteAuditIssue(@Context SecurityContext ctx,
                                     @PathParam("issueId") UUID issueId) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Deleting audit issue [userId: {}, issueId: {}]", userId, issueId);

        auditReportService.deleteIssue(userId, issueId);
        return Response.noContent().build();
    }

    private AuditReportTemplateResponse marshal(AuditReportTemplate template) {
        return new AuditReportTemplateResponse()
            .name(template.getName())
            .description(template.getDescription())
            .parameters(template.getParameters().stream()
                .map(p -> new AuditReportParam()
                    .name(p.name())
                    .description(p.description())
                    .type(AuditReportParamType.fromValue(p.type().name()))
                    .defaultValue(Strings.toStringOrNull(p.defaultValue())))
                .toList());
    }

    private AuditReportConfigResponse marshal(AuditReportConfig config) {
        return new AuditReportConfigResponse()
            .id(config.getId())
            .version(config.getVersion())
            .disabled(config.isDisabled())
            .templateName(config.getTemplateName())
            .name(config.getName())
            .description(config.getDescription())
            .source(AuditReportSource.fromValue(config.getReportSource().name()))
            .sourceId(config.getReportSourceId())
            .uncategorisedIncluded(config.isUncategorisedIncluded())
            .parameters(config.getParameters().values().stream()
                .collect(Collectors.toMap(AuditReportParameter::getName, AuditReportParameter::getValue))
            );
    }

    private AuditReportConfig unmarshal(AuditReportConfigRequest request) {
        AuditReportConfig result = AuditReportConfig.builder()
            .disabled(request.getDisabled() != null && request.getDisabled())
            .name(request.getName())
            .description(request.getDescription())
            .templateName(request.getTemplateName())
            .reportSource(AuditReportConfig.ReportSource.valueOf(request.getSource().name()))
            .reportSourceId(request.getSourceId())
            .uncategorisedIncluded(request.getUncategorisedIncluded() != null && request.getUncategorisedIncluded())
            .build();

        // copy over the parameters
        request.getParameters().forEach(result::addParameter);

        return result;
    }

    private AuditIssueResponse marshal(AuditIssue issue) {
        AccountTransaction transaction = accountTransactionService.getTransaction(issue.getTransactionId())
            .orElseThrow(() -> new NotFoundException("AccountTransaction", issue.getTransactionId()));

        return marshal(issue, transaction);
    }

    private AuditIssueResponse marshal(AuditIssue issue, AccountTransaction transaction) {
        return new AuditIssueResponse()
            .id(transaction.getId())
            .issueId(issue.getId())
            .auditConfigId(issue.getReportConfigId())
            .acknowledged(issue.isAcknowledged())
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
