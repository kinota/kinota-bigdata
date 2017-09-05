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

package com.cgi.kinota.persistence.cassandra.infrastructure.persistence;

import com.cgi.kinota.persistence.cassandra.Application;
import com.cgi.kinota.persistence.cassandra.CassandraTestBase;
import com.cgi.kinota.persistence.cassandra.config.SpringDataCassandraConfig;
import com.cgi.kinota.persistence.cassandra.domain.Location;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.geojson.GeoJsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.activation.MimeType;
import java.io.StringReader;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by bmiles on 1/19/17.
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {Application.class, SpringDataCassandraConfig.class})
public class LocationRepositoryTest extends CassandraTestBase {

    private static final Logger logger = LoggerFactory.getLogger(LocationRepositoryTest.class);

    @Autowired
    private LocationRepository locationRepository;

    @Test
    public void createLocation() throws Exception {
        MimeType t = new MimeType("application/vnd.geo+json");
        String locationStr = "{\n" +
                "      \"type\":\n" +
                "  \"Feature\",\n" +
                "      \"geometry\":{\n" +
                "        \"type\": \"Point\",\n" +
                "        \"coordinates\": [-114.06,51.05]\n" +
                "      }\n" +
                "    }";
        GeoJsonObject loc = new ObjectMapper().readValue(new StringReader(locationStr), GeoJsonObject.class);
        final Location l = new Location(UUIDs.timeBased(),
                "test location 1", "A location for testing.",
                t, loc);
        locationRepository.save(l);
        final Iterable<Location> locations = locationRepository.findAll();
        Location readLocation = locations.iterator().next();
        assertEquals(l.getId(), readLocation.getId());
        assertEquals(l.getName(), readLocation.getName());
        assertEquals(l.getDescription(), readLocation.getDescription());
        assertEquals(l.getEncodingType().toString(),
                readLocation.getEncodingType().toString());
        assertEquals(l.getLocation(), readLocation.getLocation());
    }

    // TODO: Test remaining CRUD operations
}
