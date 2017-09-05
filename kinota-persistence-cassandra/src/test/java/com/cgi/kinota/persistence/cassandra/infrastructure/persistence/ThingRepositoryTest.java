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
import com.cgi.kinota.persistence.cassandra.domain.Thing;

import com.datastax.driver.core.utils.UUIDs;

import org.junit.*;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by bmiles on 12/28/16.
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {Application.class, SpringDataCassandraConfig.class})
public class ThingRepositoryTest extends CassandraTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ThingRepositoryTest.class);

    @Autowired
    private ThingRepository thingRepository;

    @Test
    public void createThing() {
        final Thing t = new Thing(UUIDs.timeBased(),
                "test thing 1", "A thing for testing",
                Collections.emptyMap());
        thingRepository.save(t);
        final Iterable<Thing> things = thingRepository.findAll();
        Thing readThing = things.iterator().next();
        assertEquals(t.getId(), readThing.getId());
        assertEquals(t.getName(), readThing.getName());
        assertEquals(t.getDescription(), readThing.getDescription());
    }

    @Test
    public void createThingWithProperties() {
        final HashMap<String, String> properties = new HashMap<String, String>();
        properties.put("foo", "bar");
        properties.put("baz", "qux");
        final Thing t = new Thing(UUIDs.timeBased(),
                "test thing 1", "A thing for testing",
                properties);
        thingRepository.save(t);
        final Iterable<Thing> things = thingRepository.findAll();
        Thing readThing = things.iterator().next();
        Map<String, String> readProperties = readThing.getProperties();
        assertEquals(properties.get("foo"), readProperties.get("foo"));
        assertEquals(properties.get("baz"), readProperties.get("baz"));
    }

    @Test
    public void createThingWithPropertiesJson() {
        final String propertiesJson = "{\"foo\": \"bar\", \"baz\": \"qux\"}";
        final Thing t = new Thing(UUIDs.timeBased(),
                "test thing 1", "A thing for testing",
                propertiesJson);
        thingRepository.save(t);
        final Iterable<Thing> things = thingRepository.findAll();
        Thing readThing = things.iterator().next();
        Map<String, String> readProperties = readThing.getProperties();
        assertEquals("bar", readProperties.get("foo"));
        assertEquals("qux", readProperties.get("baz"));
    }

    // TODO: Test remaining CRUD operations
}
