/**
 * Kinota (TM) Copyright (C) 2017 CGI Group Inc.
 *
 * Licensed under GNU Lesser General Public License v3.0 (LGPLv3);
 * you may not use this file except in compliance with the License.
 *
 * This software is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * v3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License v3.0 for more details.
 *
 * You can receive a copy of the GNU Lesser General Public License
 * from:
 *
 * https://www.gnu.org/licenses/lgpl-3.0.en.html
 *
 */

package com.cgi.kinota.commons.odata;

import com.cgi.kinota.commons.Constants;

public class ODataQuery {
    public static final String TOP = "$top";
    public static final String SKIP = "$skip";
    public static final String RESULT_FORMAT = "$resultFormat";

    public static ODataQuery defaultQuery() {
        ODataQuery q = new ODataQuery();
        q.top = Constants.MAX_REQUEST_PAGE_SIZE;
        q.skip = 0;
        q.resultFormat = null;
        return q;
    }

    protected Integer top;
    protected Integer skip;
    protected String resultFormat;

    public Integer getTop() {
        return top;
    }

    public Integer getSkip() {
        return skip;
    }

    public String getResultFormat() {
        return resultFormat;
    }
}
