package com.hillayes.rail.resource;

import com.hillayes.auth.jwt.AuthUtils;
import com.hillayes.commons.Strings;
import com.hillayes.commons.jpa.Page;
import com.hillayes.onestop.api.*;
import com.hillayes.rail.audit.AuditReportTemplate;
import com.hillayes.rail.domain.AuditReportConfig;
import com.hillayes.rail.domain.AuditReportParameter;
import com.hillayes.rail.service.AuditReportService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Map;
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

    private AuditReportResponse marshal(AuditReportTemplate template) {
        return new AuditReportResponse()
            .id(template.getId())
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
            .reportId(config.getTemplateId())
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
            .templateId(request.getReportId())
            .reportSource(AuditReportConfig.ReportSource.valueOf(request.getSource().name()))
            .reportSourceId(request.getSourceId())
            .uncategorisedIncluded(request.getUncategorisedIncluded() != null && request.getUncategorisedIncluded())
            .build();

        // copy over the parameters
        request.getParameters().forEach(result::addParameter);

        return result;
    }
}
