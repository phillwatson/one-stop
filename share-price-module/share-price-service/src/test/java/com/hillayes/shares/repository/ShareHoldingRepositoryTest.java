package com.hillayes.shares.repository;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import lombok.RequiredArgsConstructor;

@QuarkusTest
@TestTransaction
@RequiredArgsConstructor
public class ShareHoldingRepositoryTest {
    private final ShareHoldingRepository shareHoldingRepository;
}
