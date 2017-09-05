package com.cgi.kinota.commons.application.paging;

/**
 * Created by bmiles on 5/31/17.
 */
public class PagingDescriptor {
    protected Integer top;
    protected Integer skip;
    protected Integer limit;

    public PagingDescriptor(Integer top, Integer skip, Integer limit) {
        this.top = top;
        this.skip = skip;
        this.limit = limit;
    }

    public Integer getTop() {
        return top;
    }

    public Integer getSkip() {
        return skip;
    }

    public Integer getLimit() {
        return limit;
    }
}
