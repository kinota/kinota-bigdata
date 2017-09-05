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

package com.cgi.kinota.rest.cassandra;

import com.cgi.kinota.commons.domain.Location;

import org.junit.Before;

import java.util.regex.Pattern;

/**
 * Created by bmiles on 1/19/17.
 */
public abstract class CassandraLocationTestBase extends CassandraRestTestBase {

    public static final String URL_PATH_BASE = API_VERSION + Location.NAME_PLURAL;
    protected static final Pattern LOC_PATT = Pattern.compile(URL_PATH_BASE + "(.+)");

    @Before
    public void init() {
        super.init(URL_PATH_BASE);
    }
}
