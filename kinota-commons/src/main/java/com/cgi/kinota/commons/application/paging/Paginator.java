package com.cgi.kinota.commons.application.paging;

import com.cgi.kinota.commons.odata.ODataQuery;
import com.cgi.kinota.commons.application.exception.ApplicationException;

import com.fasterxml.jackson.core.JsonGenerator;

import static com.cgi.kinota.commons.Constants.MAX_REQUEST_PAGE_SIZE;

/**
 * Created by bmiles on 5/24/17.
 */
public interface Paginator {
    Paginator DEFAULT_PAGINATOR = new DefaultPaginator();

    void paginate(Long numEntities, ODataQuery q, JsonGenerator g, String urlBase) throws ApplicationException;

    static PagingDescriptor extractPagingDescriptor(ODataQuery q) {
        Integer top = q.getTop();
        if (top == null || top > MAX_REQUEST_PAGE_SIZE) {
            top = MAX_REQUEST_PAGE_SIZE;
        }

        Integer skip = q.getSkip();
        if (skip == null) {
            skip = 0;
        }

        Integer limit = null;
        if (skip >= top) {
            limit = skip + top;
        } else {
            limit = top;
        }

        return new PagingDescriptor(top, skip, limit);
    }

    static PagingDescriptor generateDefaultPagingDescriptor() {
        return new PagingDescriptor(MAX_REQUEST_PAGE_SIZE, 0, MAX_REQUEST_PAGE_SIZE);
    }
}
