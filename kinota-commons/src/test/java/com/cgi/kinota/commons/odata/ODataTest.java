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

import org.junit.Assert;
import org.junit.Test;

import static com.cgi.kinota.commons.odata.QueryParser.parseQuery;

/**
 * Created by bmiles on 5/19/17.
 */
public class ODataTest {

    @Test
    public void testODataParserTopSkipResultFormat() {
        // Valid queries
        ODataQuery filter = parseQuery("$top=42&$skip=23&$resultFormat=dataArray");
        Assert.assertEquals(Integer.valueOf(42), filter.top);
        Assert.assertEquals(Integer.valueOf(23), filter.skip);
        Assert.assertEquals("dataArray", filter.resultFormat);

        filter = parseQuery("$resultFormat=dataArray&$skip=23&$top=42");
        Assert.assertEquals(Integer.valueOf(42), filter.top);
        Assert.assertEquals(Integer.valueOf(23), filter.skip);
        Assert.assertEquals("dataArray", filter.resultFormat);

        filter = parseQuery("$skip=23&$resultFormat=dataArray");
        Assert.assertNull(filter.top);
        Assert.assertEquals(Integer.valueOf(23), filter.skip);
        Assert.assertEquals("dataArray", filter.resultFormat);

        filter = parseQuery("$top=42&$resultFormat=dataArray");
        Assert.assertEquals(Integer.valueOf(42), filter.top);
        Assert.assertNull(filter.skip);
        Assert.assertEquals("dataArray", filter.resultFormat);

        filter = parseQuery("$top=42&$skip=23");
        Assert.assertEquals(Integer.valueOf(42), filter.top);
        Assert.assertEquals(Integer.valueOf(23), filter.skip);
        Assert.assertNull(filter.resultFormat);

        // Invalid queries
        boolean exceptionThrown = false;
        try {
            filter = parseQuery("fwejlkfewkjlfewjlkfwe3243240e3=32089342kl");
        } catch (ODataQueryException e) {
            exceptionThrown = true;
        }
        Assert.assertTrue(exceptionThrown);
        exceptionThrown = false;

        try {
            filter = parseQuery("$foo=42&$skip=23&$resultFormat=dataArray");
        } catch (ODataQueryException e) {
            exceptionThrown = true;
        }
        Assert.assertTrue(exceptionThrown);
        exceptionThrown = false;
    }
}
