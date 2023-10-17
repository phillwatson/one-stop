package com.hillayes.commons.jpa;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Encapsulates a subset of data within a collection. When the collection is
 * considered to be divided into ordered subsets of equal size, each Page is
 * an indexed subset of the collection; a window over the collection.
 *
 * The content of each page is the ordered subset of entries that fall within
 * the boundary of that page's index when the collection is divided into
 * subsets of the given page size.
 *
 * @param <T> the type of element contained within the Page.
 */
public class Page<T> {
    private final List<T> content;
    private final long totalCount;
    private final int pageIndex;
    private final int pageSize;

    public Page(List<T> content, long totalCount, int pageIndex, int pageSize) {
        this.content = content == null ? List.of() : content;
        this.totalCount = totalCount;
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
    }

    /**
     * Returns the page content as {@link List}.
     */
    public List<T> getContent() {
        return content;
    }

    /**
     * Returns the zero-based index of this page of elements.
     */
    public int getPageIndex() {
        return pageIndex;
    }

    /**
     * Returns the page size of elements in the original request. This may
     * be greater than the size of the page content.
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * The number of elements within this page. For the last page. this may be
     * smaller than the page size.
     */
    public int getContentSize() {
        return content.size();
    }

    /**
     * Determines whether the page contains any elements.
     */
    public boolean isEmpty() {
        return content.isEmpty();
    }

    /**
     * Returns the number of elements in the collection as a whole.
     */
    public long getTotalCount() {
        return totalCount;
    }

    /**
     * Returns the total number of pages within the collection when the page
     * size is as given in this Page.
     */
    public int getTotalPages() {
        return (pageSize > 0) ? (int) Math.ceil((double) totalCount / (double) pageSize) : 1;
    }

    /**
     * Returns the page's content as a stream.
     */
    public Stream<T> stream() {
        return content.stream();
    }

    /**
     * Applies the given action on each element of the page's content.
     */
    public void forEach(Consumer<? super T> action) {
        content.forEach(action);
    }

    /**
     * A factory method to create an empty Page.
     */
    public static <T> Page<T> empty() {
        return new Page<>(null, 0, 0, 0);
    }

    /**
     * A factory method to create a page of the given content. The page index is
     * assumed to be 0, the page size and total number of elements is assumed to
     * be the size of the given list.
     */
    public static <T> Page<T> of(List<T> content) {
        List<T> data = content == null ? List.of() : content;
        return new Page<>(data, data.size(), 0, data.size());
    }
}
