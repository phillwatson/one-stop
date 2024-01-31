package com.hillayes.nordigen.service;

import com.hillayes.nordigen.api.RequisitionApi;
import com.hillayes.nordigen.model.PaginatedList;
import com.hillayes.nordigen.model.Requisition;
import com.hillayes.nordigen.model.RequisitionRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class RequisitionService extends AbstractRailService {
    @Inject
    @RestClient
    RequisitionApi requisitionApi;

    public PaginatedList<Requisition> list(int limit,
                                           int offset) {
        log.debug("Listing requisitions [limit: {}, offset: {}]", limit, offset);
        return requisitionApi.list(limit, offset);
    }

    public Optional<Requisition> get(String id) {
        log.debug("Retrieving requisition [id: {}]", id);
        try {
            return Optional.ofNullable(requisitionApi.get(id));
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public Requisition create(RequisitionRequest requisition) {
        log.debug("Creating requisition [institutionId: {}]", requisition.getInstitutionId());
        Requisition result = requisitionApi.create(requisition);
        log.debug("Created requisition [institutionId: {}, id: {}]", requisition.getInstitutionId(), result.id);
        return result;
    }

    public Map<String, Object> delete(String id) {
        log.debug("Deleting requisition [id: {}]", id);
        try {
            return requisitionApi.delete(id);
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                return Map.of();
            }
            throw e;
        }
    }
}
