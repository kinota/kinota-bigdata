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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ODataQueryListener extends ODataBaseListener {
    private static final Logger logger = LoggerFactory.getLogger(ODataQueryListener.class);

    public ODataQuery query;

    public ODataQueryListener(ODataQuery f) {
	query = f;
    }

    public ODataQueryListener() {
	query = new ODataQuery();
    }

    @Override
    public void enterKvp(ODataParser.KvpContext ctx) {
        String key = ctx.KEY().getText();
        String value = ctx.VALUE().getText();

        switch (key) {
        case ODataQuery.TOP:
            query.top = Integer.valueOf(value);
            break;
        case ODataQuery.SKIP:
            query.skip = Integer.valueOf(value);
            break;
        case ODataQuery.RESULT_FORMAT:
            query.resultFormat = value;
            break;
        default:
            // We should never get here because the parser will fail for an unknown key
            logger.warn("Unknown key '" + key + "' encountered in OData query (this should be impossible).");
            break;
        }
    }
}