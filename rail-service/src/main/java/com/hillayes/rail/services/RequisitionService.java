package com.hillayes.rail.services;

import com.hillayes.rail.model.PaginatedList;
import com.hillayes.rail.model.Requisition;
import com.hillayes.rail.model.RequisitionRequest;
import com.hillayes.rail.repository.RequisitionRepository;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class RequisitionService {
    @Inject
    @RestClient
    RequisitionRepository requisitionRepository;

    public PaginatedList<Requisition> list(int limit,
                                           int offset) {
        return requisitionRepository.list(limit, offset);
    }

    public Requisition get(UUID id) {
        return requisitionRepository.get(id);
    }

    public Requisition create(RequisitionRequest requisition) {
        return requisitionRepository.create(requisition);
    }

    public Map<String, Object> delete(UUID id) {
        return requisitionRepository.delete(id);
    }
}
