package com.hillayes.shares.repository;

import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.shares.domain.ShareDealing;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class DealingRepository extends RepositoryBase<ShareDealing, UUID> {
}
