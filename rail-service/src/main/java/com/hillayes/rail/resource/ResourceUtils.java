package com.hillayes.rail.resource;

import com.hillayes.onestop.api.PageLinks;
import org.springframework.data.domain.Page;

import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.UUID;
import java.util.function.Function;

public final class ResourceUtils {
    /**
     * Extracts any UUID of the calling user, from the given SecurityContext.
     */
    public static UUID getUserId(SecurityContext context) {
        return UUID.fromString(context.getUserPrincipal().getName());
    }

    /**
     * Builds page links for the given page, using the given uriInfo.
     *
     * @param uriInfo the UriInfo for the request, used to obtain the base URI.
     * @param page the page of DB query results.
     * @return a PageLinks object containing the links for the given page.
     */
    public static PageLinks buildPageLinks(UriInfo uriInfo, Page<?> page) {
        return buildPageLinks(uriInfo, page, Function.identity());
    }

    /**
     * Builds page links for a page of the given sizes, using the given uriInfo.
     *
     * @param uriInfo the UriInfo for the request, used to obtain the base URI.
     * @param pageNo the page number - zero-based.
     * @param pageSize the size of this page.
     * @param pageCount the total number of pages.
     * @return a PageLinks object containing the links for the given page.
     */
    public static PageLinks buildPageLinks(UriInfo uriInfo, int pageNo, int pageSize, int pageCount) {
        return buildPageLinks(uriInfo, pageNo, pageSize, pageCount, Function.identity());
    }

    /**
     * Builds page links for the given page, using the given uriInfo and uriDecorator.
     * The decorator is used to add any additional query parameters to the links.
     *
     * @param uriInfo the UriInfo for the request, used to obtain the base URI.
     * @param page the page of DB query results.
     * @param uriDecorator a function that adds additional data to the given UriBuilder,
     *     and returns a UriBuilder (possibly the same as given).
     * @return a PageLinks object containing the links for the given page.
     */
    public static PageLinks buildPageLinks(UriInfo uriInfo, Page<?> page,
                                           Function<UriBuilder, UriBuilder> uriDecorator) {
        return buildPageLinks(uriInfo, page.getNumber(), page.getSize(), page.getTotalPages(), uriDecorator);
    }

    /**
     * Builds page links for a page of the given sizes, using the given uriInfo and
     * uriDecorator.
     * The decorator is used to add any additional query parameters to the links.
     *
     * @param uriInfo the UriInfo for the request, used to obtain the base URI.
     * @param pageNo the page number - zero-based.
     * @param pageSize the size of this page.
     * @param pageCount the total number of pages.
     * @param uriDecorator a function that adds additional data to the given UriBuilder,
     *     and returns a UriBuilder (possibly the same as given).
     * @return a PageLinks object containing the links for the given page.
     */
    public static PageLinks buildPageLinks(UriInfo uriInfo, int pageNo, int pageSize, int pageCount,
                                           Function<UriBuilder, UriBuilder> uriDecorator) {
        // construct UriBuilder and pass it to the decorator for any additional query params
        UriBuilder uriBuilder = uriDecorator.apply(uriInfo.getAbsolutePathBuilder())
            .queryParam("page-size", pageSize);

        PageLinks result = new PageLinks()
            .first(uriBuilder.replaceQueryParam("page", 0).build())
            .last(uriBuilder.replaceQueryParam("page", pageCount - 1).build());

        if (pageNo > 0) {
            result.previous(uriBuilder.replaceQueryParam("page", pageNo - 1).build());
        }
        if (pageNo < pageCount - 1) {
            result.next(uriBuilder.replaceQueryParam("page", pageNo + 1).build());
        }

        return result;
    }
}
