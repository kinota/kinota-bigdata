package com.cgi.kinota.commons.application.paging;

import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.domain.Entity;
import com.cgi.kinota.commons.odata.ODataQuery;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

import static com.cgi.kinota.commons.Constants.ANNO_IOT_NEXT_LINK;
import static com.cgi.kinota.commons.Constants.MAX_REQUEST_PAGE_SIZE;

/**
 * Created by bmiles on 5/24/17.
 */
public class DefaultPaginator implements Paginator {
    public void paginate(Long numEntities, ODataQuery q, JsonGenerator g,
                         String urlBase) throws ApplicationException {
        try {
            if (numEntities > MAX_REQUEST_PAGE_SIZE) {
                Integer top = q.getTop();
                if (top == null || top > MAX_REQUEST_PAGE_SIZE) {
                    top = MAX_REQUEST_PAGE_SIZE;
                }
                Integer skip = q.getSkip();
                if (top != null) {
                    if (skip == null) {
                        skip = top;
                    } else {
                        skip = skip + top;
                    }
                } else if (skip != null) {
                    skip += MAX_REQUEST_PAGE_SIZE;
                } else {
                    skip = MAX_REQUEST_PAGE_SIZE;
                }
                if (skip < numEntities) {
                    // Only display next link if we are not in the last page of results
                    g.writeStringField(ANNO_IOT_NEXT_LINK, Entity.generateNextLinkUrl(urlBase, top, skip));
                }
            }
        } catch (IOException e) {
            String mesg = "Unable to paginate JSON stream due to error: " +
                    e.getMessage();
            throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
        }
    }
}
