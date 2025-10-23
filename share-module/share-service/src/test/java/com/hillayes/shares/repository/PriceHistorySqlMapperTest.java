package com.hillayes.shares.repository;

import com.hillayes.shares.domain.PriceHistory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.postgresql.util.PGobject;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class PriceHistorySqlMapperTest {
    private final PriceHistorySqlMapper fixture = new PriceHistorySqlMapper();

    @Test
    public void testNameOrderIsConsistentWithMapping() {
        String[] colNames = fixture.colNames.substring(1, fixture.colNames.length() - 1).split(",");
        Map<String, SqlEntityMapper.ColMapper<PriceHistory>> colMappings = fixture.colMappings;

        assertEquals(colNames.length, colMappings.size());

        AtomicInteger index = new AtomicInteger();
        colMappings.forEach((name, mapping) -> {
            assertEquals(colNames[index.getAndIncrement()], name);
        });
    }

    @Test
    public void testSetString() throws SQLException {
        // Given: a PreparedStatement
        PreparedStatement statement = mock();

        // When: a non-null value is passed
        String value = "any string";
        fixture.setString(statement, 1, value);

        // Then: the statement setString is called
        verify(statement).setString(1, value);

        // When: a null value is passed
        fixture.setString(statement, 1, null);

        // Then: a null value is set
        verify(statement).setNull(1, Types.VARCHAR);
    }

    @Test
    public void testSetLong() throws SQLException {
        // Given: a PreparedStatement
        PreparedStatement statement = mock();

        // When: a non-null value is passed
        Long value = Long.valueOf(200);
        fixture.setLong(statement, 1, value);

        // Then: the statement setLong is called
        verify(statement).setLong(1, value);

        // When: a null value is passed
        fixture.setLong(statement, 1, null);

        // Then: a null value is set
        verify(statement).setNull(1, Types.BIGINT);
    }

    @Test
    public void testSetDecimal() throws SQLException {
        // Given: a PreparedStatement
        PreparedStatement statement = mock();

        // When: a non-null value is passed
        BigDecimal value = BigDecimal.valueOf(200);
        fixture.setDecimal(statement, 1, value);

        // Then: the statement setBigDecimal is called
        verify(statement).setBigDecimal(1, value);

        // When: a null value is passed
        fixture.setDecimal(statement, 1, null);

        // Then: a null value is set
        verify(statement).setNull(1, Types.DECIMAL);
    }

    @Test
    public void testSetUUID() throws SQLException {
        // Given: a PreparedStatement
        PreparedStatement statement = mock();

        // When: a non-null value is passed
        UUID value = UUID.randomUUID();
        fixture.setUuid(statement, 1, value);

        // Then: the statement setObject is called
        verify(statement).setObject(1, value);

        // When: a null value is passed
        fixture.setUuid(statement, 1, null);

        // Then: a null value is set
        verify(statement).setNull(1, Types.OTHER);
    }

    @Test
    public void testSetDate() throws SQLException {
        // Given: a PreparedStatement
        PreparedStatement statement = mock();

        // When: a non-null value is passed
        LocalDate value = LocalDate.now();
        fixture.setDate(statement, 1, value);

        // Then: the statement setDate is called
        verify(statement).setDate(1, java.sql.Date.valueOf(value));

        // When: a null value is passed
        fixture.setDate(statement, 1, null);

        // Then: a null value is set
        verify(statement).setNull(1, java.sql.Types.DATE);
    }

    @Test
    public void testSetTimestamp() throws SQLException {
        // Given: a PreparedStatement
        PreparedStatement statement = mock();

        // When: a non-null value is passed
        Instant value = Instant.now();
        fixture.setTimestamp(statement, 1, value);

        // Then: the statement setTimestamp is called
        verify(statement).setTimestamp(1, Timestamp.from(value));

        // When: a null value is passed
        fixture.setTimestamp(statement, 1, null);

        // Then: a null value is set
        verify(statement).setNull(1, Types.TIMESTAMP_WITH_TIMEZONE);
    }

    @Test
    public void testSetJsonb() throws SQLException {
        // Given: a PreparedStatement
        PreparedStatement statement = mock();

        // When: a non-null value is passed
        String value = "{ x: \"abc\" }";
        fixture.setJsonb(statement, 1, value);

        // Then: the statement setTimestamp is called
        verify(statement).setObject(eq(1), any(PGobject.class));

        // When: a null value is passed
        fixture.setJsonb(statement, 1, null);

        // Then: a null value is set (using same method)
        verify(statement, times(2)).setObject(eq(1), any(PGobject.class));
    }
}
