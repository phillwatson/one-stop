package com.hillayes.shares.ft.repository;

import com.hillayes.shares.ft.domain.IsinIssueLookup;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.hibernate.internal.util.collections.CollectionHelper.listOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestTransaction
public class IsinIssueLookupRepositoryTest {
    private static final RandomStringUtils randomStrings = RandomStringUtils.insecure();

    @Inject
    private IsinIssueLookupRepository isinIssueLookupRepository;

    @Test
    public void testSave() {
        IsinIssueLookup entity = mockEntity();

        IsinIssueLookup saved = isinIssueLookupRepository.save(entity);

        assertEquals(entity, saved);
    }

    @Test
    public void testFindByIsin_success() {
        List<IsinIssueLookup> entities = listOf(
            mockEntity(),
            mockEntity(),
            mockEntity()
        );

        isinIssueLookupRepository.saveAll(entities);

        entities.forEach(expected -> {
            Optional<IsinIssueLookup> actual = isinIssueLookupRepository.findByIsin(expected.getIsin());

            assertTrue(actual.isPresent());
            assertEquals(expected, actual.get());
        });
    }

    @Test
    public void testFindByIsin_not_found() {
        List<IsinIssueLookup> entities = listOf(
            mockEntity(),
            mockEntity(),
            mockEntity()
        );

        isinIssueLookupRepository.saveAll(entities);

        Optional<IsinIssueLookup> actual = isinIssueLookupRepository.findByIsin(randomStrings.nextAlphanumeric(10));

        assertTrue(actual.isEmpty());
    }

    private IsinIssueLookup mockEntity() {
        return IsinIssueLookup.builder()
            .isin(randomStrings.nextAlphanumeric(10))
            .issueId(randomStrings.nextAlphanumeric(10))
            .build();
    }
}
