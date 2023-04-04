package com.hillayes.rail.service;

import com.hillayes.rail.model.PaginatedList;
import com.hillayes.rail.model.Requisition;
import com.hillayes.rail.model.RequisitionRequest;
import com.hillayes.rail.repository.RequisitionRepository;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class RequisitionService {
    @Inject
    @RestClient
    RequisitionRepository requisitionRepository;

    public PaginatedList<Requisition> list(int limit,
                                           int offset) {
        return requisitionRepository.list(limit, offset);
    }

    public Optional<Requisition> get(String id) {
        return requisitionRepository.get(id);
    }

    public Requisition create(RequisitionRequest requisition) {
        return requisitionRepository.create(requisition);
    }

    public Map<String, Object> delete(String id) {
        return requisitionRepository.delete(id);
    }
}
