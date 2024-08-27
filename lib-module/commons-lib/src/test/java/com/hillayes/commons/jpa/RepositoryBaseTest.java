package com.hillayes.commons.jpa;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class RepositoryBaseTest {
    private final RepositoryBase<Object, Object> fixture = new RepositoryBase<>() { };

    @Test
    public void testPageZero() {
        PanacheQuery<Object> query = mockQuery(10, 20);

        Page<Object> page = fixture.findByPage(query, 0, 10);
        assertEquals(0, page.getPageIndex());
        assertEquals(10, page.getPageSize());
        assertEquals(2, page.getTotalPages());
        assertEquals(10, page.getContent().size());

        // total count is determined by SQL query
        assertEquals(20, page.getTotalCount());
        verify(query).count();
    }

    @Test
    public void testPageOne() {
        PanacheQuery<Object> query = mockQuery(10, 20);

        Page<Object> page = fixture.findByPage(query, 1, 10);
        assertEquals(1, page.getPageIndex());
        assertEquals(10, page.getPageSize());
        assertEquals(2, page.getTotalPages());
        assertEquals(10, page.getContent().size());

        // total count is determined by SQL query
        assertEquals(20, page.getTotalCount());
        verify(query).count();
    }

    @Test
    public void testPageZeroPartial() {
        PanacheQuery<Object> query = mockQuery(5, 20);

        Page<Object> page = fixture.findByPage(query, 0, 10);
        assertEquals(0, page.getPageIndex());
        assertEquals(10, page.getPageSize());
        assertEquals(1, page.getTotalPages());
        assertEquals(5, page.getContent().size());

        // total count is determined by calculation
        assertEquals(5, page.getTotalCount());
        verify(query, never()).count();
    }

    @Test
    public void testPageOnePartial() {
        PanacheQuery<Object> query = mockQuery(5, 20);

        Page<Object> page = fixture.findByPage(query, 1, 10);
        assertEquals(1, page.getPageIndex());
        assertEquals(10, page.getPageSize());
        assertEquals(2, page.getTotalPages());
        assertEquals(5, page.getContent().size());

        // total count is determined by calculation
        assertEquals(15, page.getTotalCount());
        verify(query, never()).count();
    }

    @Test
    public void testNoRowsPageZero() {
        PanacheQuery<Object> query = mockQuery(0, 0);

        Page<Object> page = fixture.findByPage(query, 0, 10);
        assertEquals(0, page.getPageIndex());
        assertEquals(10, page.getPageSize());
        assertEquals(0, page.getTotalPages());
        assertEquals(0, page.getContent().size());

        // total count is determined by calculation
        assertEquals(0, page.getTotalCount());
        verify(query, never()).count();
    }

    @Test
    public void testNoRowsPageOne() {
        PanacheQuery<Object> query = mockQuery(0, 0);

        Page<Object> page = fixture.findByPage(query, 1, 10);
        assertEquals(1, page.getPageIndex());
        assertEquals(10, page.getPageSize());
        assertEquals(0, page.getTotalPages());
        assertEquals(0, page.getContent().size());

        // total count is determined by SQL query
        assertEquals(0, page.getTotalCount());
        verify(query).count();
    }

    private PanacheQuery<Object> mockQuery(int size, long count) {
        PanacheQuery<Object> query = mock(PanacheQuery.class);
        when(query.page(anyInt(), anyInt())).thenReturn(query);
        when(query.count()).thenReturn(count);

        Object[] content = new Object[size];
        Arrays.fill(content, new Object());
        when(query.list()).thenReturn(List.of(content));

        return query;
    }
}
