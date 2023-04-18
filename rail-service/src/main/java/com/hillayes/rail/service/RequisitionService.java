package com.hillayes.rail.service;

import com.hillayes.rail.model.PaginatedList;
import com.hillayes.rail.model.Requisition;
import com.hillayes.rail.model.RequisitionRequest;
import com.hillayes.rail.repository.RequisitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class RequisitionService extends AbstractRailService {
    @Inject
    @RestClient
    RequisitionRepository requisitionRepository;

    public PaginatedList<Requisition> list(int limit,
                                           int offset) {
        log.debug("Listing requisitions [limit: {}, offset: {}]", limit, offset);
        return requisitionRepository.list(limit, offset);
    }

    public Optional<Requisition> get(String id) {
        log.debug("Retrieving requisition [id: {}]", id);
        try {
            return Optional.ofNullable(requisitionRepository.get(id));
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public Requisition create(RequisitionRequest requisition) {
        log.debug("Creating requisition [institution: {}]", requisition.getInstitutionId());
        return requisitionRepository.create(requisition);
    }

    public Map<String, Object> delete(String id) {
        log.debug("Deleting requisition [id: {}]", id);
        try {
            return requisitionRepository.delete(id);
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                return Map.of();
            }
            throw e;
        }
    }
}