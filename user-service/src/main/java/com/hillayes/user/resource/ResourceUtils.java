package com.hillayes.user.resource;

import com.hillayes.commons.jpa.Page;
import com.hillayes.onestop.api.PageLinks;

import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.util.UUID;
import java.util.function.Function;

public class ResourceUtils {
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
     * Builds page links for the given page, using the given uriInfo and uriDecorator.
     * The decorator is used to add any additional query parameters to the links.
     *
     * @param uriInfo the UriInfo for the request, used to obtain the base URI.
     * @param page the page of DB query results.
     * @param uriDecorator a function that adds additional data to the given UriBuilder,
     * and returns a UriBuilder (possibly the same as given).
     * @return a PageLinks object containing the links for the given page.
     */
    public static PageLinks buildPageLinks(UriInfo uriInfo, Page<?> page,
                                           Function<UriBuilder, UriBuilder> uriDecorator) {
        int pageNo = page.getPageIndex();
        int pageCount = page.getTotalPages();

        // construct UriBuilder and pass it to the decorator for any additional query params
        UriBuilder uriBuilder = uriDecorator.apply(uriInfo.getAbsolutePathBuilder())
            .queryParam("page-size", page.getPageSize());

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
