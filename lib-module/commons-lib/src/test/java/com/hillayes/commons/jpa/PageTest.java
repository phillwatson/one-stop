package com.hillayes.commons.jpa;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PageTest {
    @Test
    public void testNullList() {
        Page<Object> page = Page.of(null);

        assertEquals(0, page.getTotalPages());
        assertEquals(0, page.getContentSize());
        assertNotNull(page.getContent());
        assertEquals(0, page.getContent().size());
        assertEquals(0, page.getPageIndex());
        assertEquals(0, page.getPageSize());
    }

    @Test
    public void testNullListWithIndex() {
        Page<Object> page = Page.of(null, 2, 10);

        assertEquals(0, page.getTotalPages());
        assertEquals(0, page.getContentSize());
        assertNotNull(page.getContent());
        assertEquals(0, page.getContent().size());
        assertEquals(2, page.getPageIndex());
        assertEquals(10, page.getPageSize());
    }

    @Test
    public void testEmptyList() {
        Page<Object> page = Page.of(List.of());

        assertEquals(0, page.getTotalPages());
        assertEquals(0, page.getContentSize());
        assertNotNull(page.getContent());
        assertEquals(0, page.getContent().size());
        assertEquals(0, page.getPageIndex());
        assertEquals(0, page.getPageSize());
    }

    @Test
    public void testEmptyListWithIndex() {
        Page<Object> page = Page.of(List.of(), 3, 30);

        assertEquals(0, page.getTotalPages());
        assertEquals(0, page.getContentSize());
        assertNotNull(page.getContent());
        assertEquals(0, page.getContent().size());
        assertEquals(3, page.getPageIndex());
        assertEquals(30, page.getPageSize());
    }

    @Test
    public void testEmpty() {
        Page<Object> page = Page.empty();

        assertEquals(0, page.getTotalPages());
        assertEquals(0, page.getContentSize());
        assertNotNull(page.getContent());
        assertEquals(0, page.getContent().size());
        assertEquals(0, page.getPageIndex());
        assertEquals(0, page.getPageSize());
    }

    @Test
    public void testEmptyWithIndex() {
        Page<Object> page = Page.empty(3, 30);

        assertEquals(0, page.getTotalPages());
        assertEquals(0, page.getContentSize());
        assertNotNull(page.getContent());
        assertEquals(0, page.getContent().size());
        assertEquals(3, page.getPageIndex());
        assertEquals(30, page.getPageSize());
    }
}
