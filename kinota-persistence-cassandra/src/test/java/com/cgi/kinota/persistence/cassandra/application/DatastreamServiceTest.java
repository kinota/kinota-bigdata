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

package com.cgi.kinota.persistence.cassandra.application;

import com.cgi.kinota.commons.application.DatastreamService;
import com.cgi.kinota.commons.application.LocationService;
import com.cgi.kinota.commons.application.ObservedPropertyService;
import com.cgi.kinota.commons.application.SensorService;
import com.cgi.kinota.commons.application.ThingService;
import com.cgi.kinota.commons.domain.Datastream;
import com.cgi.kinota.commons.domain.Location;
import com.cgi.kinota.commons.domain.ObservedProperty;
import com.cgi.kinota.commons.domain.Sensor;
import com.cgi.kinota.commons.domain.Thing;
import com.cgi.kinota.commons.domain.support.UnitOfMeasurement;

import com.cgi.kinota.persistence.cassandra.Application;
import com.cgi.kinota.persistence.cassandra.CassandraTestBase;
import com.cgi.kinota.persistence.cassandra.config.SpringDataCassandraConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static com.cgi.kinota.commons.domain.util.Serialization.stringToURI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by bmiles on 8/7/17.
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {Application.class, SpringDataCassandraConfig.class})
public class DatastreamServiceTest extends CassandraTestBase {

    private static final Logger logger = LoggerFactory.getLogger(DatastreamServiceTest.class);

    @Autowired
    private DatastreamService dsSvc;

    @Autowired
    private LocationService locationSvc;

    @Autowired
    private ThingService thingSvc;

    @Autowired
    private SensorService sensorSvc;

    @Autowired
    private ObservedPropertyService opSvc;

    public static Datastream createDatastream(DatastreamService dsSvc,
                                              String name,
                                              String description,
                                              String unitName,
                                              String unitSymbol,
                                              String unitDefinition,
                                              String observationTypeUri,
                                              Thing t,
                                              Sensor s,
                                              ObservedProperty op) {
        Datastream d = null;
        try {
            URI observationType = stringToURI(observationTypeUri);
            UnitOfMeasurement u = new UnitOfMeasurement();
            u.setName(unitName);
            u.setDefinition(stringToURI(unitDefinition));
            u.setSymbol(unitSymbol);
            d = dsSvc.create(t.getId(), s.getId(), op.getId(),
                    name, description, u, observationType, null);
        } catch (Exception e) {
            logger.error(e.getMessage());
            fail(e.getMessage());
        }
        return d;
    }

    @Test
    public void testCreateDatastream() throws Exception {
        Location l = LocationServiceTest.createLocation(locationSvc,
                "test location 1", "A location for testing.",
                -114.06,
                51.05);

        final Map<String, String> properties = new HashMap<String, String>();
        properties.put("foo", "bar");
        properties.put("baz", "qux");
        Thing t = ThingServiceTest.createThing(thingSvc,"test thing 1",
                "A thing for testing.",
                properties,
                l);

        Sensor s = SensorServiceTest.createSensor(sensorSvc,
                "TMP36",
                "TMP36 - Analog Temperature sensor",
                "application/pdf",
                "http://example.org/TMP35_36_37.pdf");

        ObservedProperty op = ObservedPropertyServiceTest.createObservedProperty(opSvc,
                "DewPoint Temperature",
                "http://dbpedia.org/page/Dew_point",
                ObservedPropertyServiceTest.description1);

        Datastream d = createDatastream(dsSvc,
                "oven temperature",
                "This is a datastream measuring the air temperature in an oven.",
                "degree Celsius",
                "C",
                "http://unitsofmeasure.org/ucum.html#para-30",
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                t,
                s,
                op);
        Datastream readDs = dsSvc.findOne(d.getId());
        assertEquals(d.getThingId(), readDs.getThingId());
        assertEquals(d.getSensorId(), readDs.getSensorId());
        assertEquals(d.getObservedPropertyId(), readDs.getObservedPropertyId());
        assertEquals(d.getName(), readDs.getName());
        assertEquals(d.getDescription(), readDs.getDescription());
        assertEquals(d.getUnitOfMeasurement(), readDs.getUnitOfMeasurement());
        assertEquals(d.getObservationType().toString(), readDs.getObservationType().toString());
    }

    // TODO: Test remaining CRUD operations
}
