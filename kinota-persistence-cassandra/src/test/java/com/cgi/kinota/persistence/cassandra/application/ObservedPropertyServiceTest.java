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
import com.cgi.kinota.commons.application.FeatureOfInterestService;
import com.cgi.kinota.commons.application.LocationService;
import com.cgi.kinota.commons.application.ObservationService;
import com.cgi.kinota.commons.application.ObservedPropertyService;
import com.cgi.kinota.commons.application.RelatedEntityManager;
import com.cgi.kinota.commons.application.SensorService;
import com.cgi.kinota.commons.application.ThingService;
import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.domain.Datastream;
import com.cgi.kinota.commons.domain.FeatureOfInterest;
import com.cgi.kinota.commons.domain.Location;
import com.cgi.kinota.commons.domain.Observation;
import com.cgi.kinota.commons.domain.ObservedProperty;
import com.cgi.kinota.commons.domain.Sensor;
import com.cgi.kinota.commons.domain.Thing;
import com.cgi.kinota.commons.odata.ODataQuery;

import com.cgi.kinota.persistence.cassandra.Application;
import com.cgi.kinota.persistence.cassandra.CassandraTestBase;
import com.cgi.kinota.persistence.cassandra.config.SpringDataCassandraConfig;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.cgi.kinota.commons.domain.util.Serialization.stringToURI;
import static org.assertj.core.api.Java6Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by bmiles on 8/7/17.
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {Application.class, SpringDataCassandraConfig.class})
public class ObservedPropertyServiceTest extends CassandraTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ObservedPropertyServiceTest.class);

    @Autowired
    private LocationService locationSvc;

    @Autowired
    private ThingService thingSvc;

    @Autowired
    private DatastreamService dsSvc;

    @Autowired
    private SensorService sensorSvc;

    @Autowired
    private ObservedPropertyService opSvc;

    @Autowired
    private FeatureOfInterestService foiSvc;

    @Autowired
    private ObservationService obsSvc;

    @Autowired
    private RelatedEntityManager related;

    static String description1 = "The dewpoint temperature is the temperature to which the air must be " +
            "cooled, at constant pressure, for dew to form. As the grass and other objects " +
            "near the ground cool to the dewpoint, some of the water vapor in the " +
            "atmosphere condenses into liquid water on the objects.";

    public static ObservedProperty createObservedProperty(ObservedPropertyService opSvc,
                                                          String name,
                                                          String definitionStr,
                                                          String description) {
        ObservedProperty op = null;
        try {
            URI definition = stringToURI(definitionStr);
            op = opSvc.create(name, definition, description);
        } catch (Exception e) {
            logger.error(e.getMessage());
            fail(e.getMessage());
        }
        return op;
    }

    @Test
    public void testCreateObservedProperty() throws Exception {
        ObservedProperty op = createObservedProperty(opSvc,
                "DewPoint Temperature",
                "http://dbpedia.org/page/Dew_point",
                description1);
        ObservedProperty readOp = opSvc.findOne(op.getId());
        assertEquals(op.getName(), readOp.getName());
        assertEquals(op.getDefinition(), readOp.getDefinition());
        assertEquals(op.getDescription(), readOp.getDescription());
    }

    @Test
    public void createDeleteObservedProperty() {
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

        Datastream d = DatastreamServiceTest.createDatastream(dsSvc,
                "oven temperature",
                "This is a datastream measuring the air temperature in an oven.",
                "degree Celsius",
                "C",
                "http://unitsofmeasure.org/ucum.html#para-30",
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                t,
                s,
                op);

        FeatureOfInterest foi = FeatureOfInterestServiceTest.createFeatureOfInterest(foiSvc,
                "test location 1", "A location for testing.",
                -114.06,
                51.05);

        FeatureOfInterest foi2 = FeatureOfInterestServiceTest.createFeatureOfInterest(foiSvc,
                "test location 2", "A location for testing 2.",
                -116.06,
                53.05);

        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("foo", "bar");
        parameters.put("baz", "qux");
        Observation o = ObservationServiceTest.createObservation(obsSvc,
                "2014-12-25T11:59:59.00+08:00",
                null,
                null,
                "42.23",
                null,
                parameters,
                d,
                foi);
        Observation readObs = obsSvc.findOne(o.getId());
        assertEquals(o.getId(), readObs.getId());
        assertEquals(o.getPhenomenonTime(), readObs.getPhenomenonTime());
        assertEquals(o.getResultMeasurement(), readObs.getResultMeasurement());
        assertEquals(o.getParameters().get("foo"), readObs.getParameters().get("foo"));
        assertEquals(o.getParameters().get("baz"), readObs.getParameters().get("baz"));

        // Create another Observation
        parameters.put("bar", "foo");
        parameters.put("qux", "baz");
        Observation o2 = ObservationServiceTest.createObservation(obsSvc,
                "2014-12-27T00:23:42.00+08:00",
                null,
                null,
                "23.42",
                null,
                parameters,
                d,
                foi2);
        Observation readObs2 = obsSvc.findOne(o2.getId());
        assertEquals(o2.getId(), readObs2.getId());
        assertEquals(o2.getPhenomenonTime(), readObs2.getPhenomenonTime());
        assertEquals(o2.getResultMeasurement(), readObs2.getResultMeasurement());
        assertEquals(o2.getParameters().get("bar"), readObs2.getParameters().get("bar"));
        assertEquals(o2.getParameters().get("qux"), readObs2.getParameters().get("qux"));

        // Delete ObservedProperty
        related.deleteObservedProperty(op.getId());
        // Make sure ObservedProperty was deleted
        boolean exceptionThrown = false;
        try {
            opSvc.findOne(op.getId());
        } catch (ApplicationException ae) {
            if (ae.getErrorCode() == ApplicationErrorCode.E_NotFound) {
                exceptionThrown = true;
            }
        }
        assertTrue(exceptionThrown);
        // Make sure Observations were deleted
        exceptionThrown = false;
        try {
            readObs = obsSvc.findOne(o.getId());
        } catch (ApplicationException ae) {
            if (ae.getErrorCode() == ApplicationErrorCode.E_NotFound) {
                exceptionThrown = true;
            }
        }
        assertTrue(exceptionThrown);
        exceptionThrown = false;
        try {
            readObs = obsSvc.findOne(o2.getId());
        } catch (ApplicationException ae) {
            if (ae.getErrorCode() == ApplicationErrorCode.E_NotFound) {
                exceptionThrown = true;
            }
        }
        assertTrue(exceptionThrown);

        // Make sure relationship between ObservedProperty/Datastream and Datastream/Thing were removed
        Pair<Long, Iterable<UUID>> dsThing = related.fetchDatastreamUuidsForThing(t.getId(),
                ODataQuery.defaultQuery());
        assertEquals(Long.valueOf(0l), dsThing.getLeft());

        Pair<Long, Iterable<UUID>> dsOp = related.fetchDatastreamUuidsForObservedProperty(op.getId(),
                ODataQuery.defaultQuery());
        assertEquals(Long.valueOf(0l), dsOp.getLeft());
    }

    // TODO: Test remaining CRUD operations
}
