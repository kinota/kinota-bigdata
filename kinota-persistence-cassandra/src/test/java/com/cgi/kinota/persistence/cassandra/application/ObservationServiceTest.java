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

import com.cgi.kinota.commons.Utility;
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
import com.cgi.kinota.commons.domain.support.ObservationType;
import com.cgi.kinota.commons.odata.ODataQuery;

import com.cgi.kinota.persistence.cassandra.Application;
import com.cgi.kinota.persistence.cassandra.CassandraTestBase;
import com.cgi.kinota.persistence.cassandra.config.SpringDataCassandraConfig;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.cgi.kinota.commons.domain.util.Serialization.ISO8601DateTimeStringToUTCDate;
import static com.cgi.kinota.commons.domain.util.Serialization.ISO8601TimeIntervalStringToDates;
import static com.cgi.kinota.commons.domain.util.Serialization.stringToGeoJsonPolygon;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by bmiles on 8/7/17.
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {Application.class, SpringDataCassandraConfig.class})
public class ObservationServiceTest extends CassandraTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ObservationServiceTest.class);

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

    public static Observation createObservation(ObservationService obsSvc,
                                                String phenomenonTimeStr,
                                                String resultTimeStr,
                                                String validTimeStr,
                                                String result,
                                                String resultQuality,
                                                Map<String, String> parameters,
                                                Datastream d,
                                                FeatureOfInterest foi) {
        Observation o = null;
        try {
            UUID foiId = null;
            if (foi != null) {
                foiId = foi.getId();
            }

            Date phenomenonTime = null;
            Date phenomenonTimeEnd = null;
            try {
                phenomenonTime = ISO8601DateTimeStringToUTCDate(phenomenonTimeStr);
            } catch (IllegalArgumentException e) {
                // This might be a time interval
                try {
                    Pair<Date, Date> dates = ISO8601TimeIntervalStringToDates(phenomenonTimeStr);
                    phenomenonTime = dates.getLeft();
                    phenomenonTimeEnd = dates.getRight();
                } catch (ApplicationException ea) {
                    throw new ApplicationException(ApplicationErrorCode.E_JSON,
                            "Unable to parse phenomenonTime interval '" + phenomenonTimeStr + "'.");
                }
            }

            Date resultTime = null;
            DateTimeFormatter f = Utility.getISO8601Formatter();
            if (resultTimeStr != null) {
                DateTime dt = f.parseDateTime(resultTimeStr);
                resultTime = dt.toDate();
            }

            Date validTimeBegin = null;
            Date validTimeEnd = null;
            if (validTimeStr != null) {
                try {
                    Pair<Date, Date> dates = ISO8601TimeIntervalStringToDates(validTimeStr);
                    validTimeBegin = dates.getLeft();
                    validTimeEnd = dates.getRight();
                } catch (ApplicationException ea) {
                    throw new ApplicationException(ApplicationErrorCode.E_JSON,
                            "Unable to parse validTime '" + validTimeStr + "'.");
                }
            }

            Double resultMeasurement = null;
            Long resultCount = null;
            Boolean resultTruth = null;
            String resultString = null;
            ObservationType observationType = ObservationType.valueOfUri(d.getObservationType());
            // The type of the result depends on the observationType of the associated Datastream
            switch (observationType) {
                case OM_Measurement:
                    try {
                        resultMeasurement = Double.valueOf(result);
                    } catch (ClassCastException e) {
                        String mesg = "Expected 'result' to be of type double in Observation: " + result;
                        logger.error(mesg);
                        fail(mesg);
                    }
                    break;
                case OM_CountObservation:
                    try {
                        resultCount = Long.valueOf(result);
                    } catch (ClassCastException e) {
                        String mesg = "Expected 'result' to be of type integer in Observation: " + result;
                        logger.error(mesg);
                        fail(mesg);
                    }
                    break;
                case OM_TruthObservation:
                    try {
                        resultTruth = Boolean.valueOf(result);
                    } catch (ClassCastException e) {
                        String mesg = "Expected 'result' to be of type boolean in Observation: " + result;
                        logger.error(mesg);
                        fail(mesg);
                    }
                    break;
                case OM_CategoryObservation:
                    String tmpResult = result;
                    // Make sure result is a valid URI
                    try {
                        URI tmpUri = URI.create(tmpResult);
                        resultString = tmpResult;
                    } catch (IllegalArgumentException e) {
                        String mesg = "Expected 'result' to be of type URI in Observation: " + result;
                        logger.error(mesg);
                        fail(mesg);
                    }
                    break;
                case OM_Observation:
                    resultString = result;
                    break;
            }

            o = obsSvc.create(d, foiId,
                    phenomenonTime,
                    phenomenonTimeEnd, resultTime,
                    validTimeBegin, validTimeEnd,
                    resultQuality,
                    observationType,
                    resultString,
                    resultCount,
                    resultMeasurement,
                    resultTruth,
                    parameters);
        } catch (Exception e) {
            logger.error(e.getMessage());
            fail(e.getMessage());
        }

        return o;
    }

    @Test
    public void createDeleteDatastream() {
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

        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("foo", "bar");
        parameters.put("baz", "qux");
        Observation o = createObservation(obsSvc,
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

        // Delete the Datastream (including Observations)
        related.deleteDatastream(d.getId());
        // Make sure Observation was deleted
        boolean exceptionThrown = false;
        try {
            readObs = obsSvc.findOne(o.getId());
        } catch (ApplicationException ae) {
            if (ae.getErrorCode() == ApplicationErrorCode.E_NotFound) {
                exceptionThrown = true;
            }
        }
        assertTrue(exceptionThrown);
        // Make sure Datastream was deleted
        exceptionThrown = false;
        try {
            Datastream readDs = dsSvc.findOne(d.getId());
        } catch (ApplicationException ae) {
            if (ae.getErrorCode() == ApplicationErrorCode.E_NotFound) {
                exceptionThrown = true;
            }
        }
        assertTrue(exceptionThrown);

        // Make sure relationship between Datastream/FeatureOfInterest and the Observation was removed
        Pair<Long, Iterable<UUID>> dsObs = related.fetchRelatedObservationUuidsForDatastream(d.getId(),
                ODataQuery.defaultQuery());
        assertEquals(Long.valueOf(0l), dsObs.getLeft());

        Pair<Long, Iterable<UUID>> foiObs = related.fetchRelatedObservationUuidsForFeatureOfInterest(foi.getId(),
                ODataQuery.defaultQuery());
        assertEquals(Long.valueOf(0l), foiObs.getLeft());
    }

    @Test
    public void createDeleteDatastreamObservations() {
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

        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("foo", "bar");
        parameters.put("baz", "qux");
        Observation o = createObservation(obsSvc,
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
                foi);
        Observation readObs2 = obsSvc.findOne(o2.getId());
        assertEquals(o2.getId(), readObs2.getId());
        assertEquals(o2.getPhenomenonTime(), readObs2.getPhenomenonTime());
        assertEquals(o2.getResultMeasurement(), readObs2.getResultMeasurement());
        assertEquals(o2.getParameters().get("bar"), readObs2.getParameters().get("bar"));
        assertEquals(o2.getParameters().get("qux"), readObs2.getParameters().get("qux"));

        // Fetch Datastream for summaries before deletion
        Datastream d2 = dsSvc.findOne(d.getId());
        assertEquals(o.getPhenomenonTime(), d2.getPhenomenonTimeBegin());
        assertEquals(o2.getPhenomenonTime(), d2.getPhenomenonTimeEnd());
        assertEquals(stringToGeoJsonPolygon("{\"type\":\"Polygon\",\"coordinates\":[[[-114.0599,51.0501],[-114.0601,51.0501],[-114.0601,51.0499],[-114.0599,51.0499],[-114.0599,51.0501]]]}"),
                d2.getObservedArea());

        // Delete Observations in Datastream
        related.deleteObservationsForDatastream(d.getId(), true);
        // Make sure Observations were deleted
        boolean exceptionThrown = false;
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

        // Fetch Datastream for summaries after deletion of Observations
        Datastream d3 = dsSvc.findOne(d.getId());
        assertEquals(null, d3.getPhenomenonTimeBegin());
        assertEquals(null, d3.getPhenomenonTimeEnd());
        assertEquals(null, d3.getObservedArea());

        // Make sure relationship between Datastream/FeatureOfInterest and the Observation was removed
        Pair<Long, Iterable<UUID>> dsObs = related.fetchRelatedObservationUuidsForDatastream(d.getId(),
                ODataQuery.defaultQuery());
        assertEquals(Long.valueOf(0l), dsObs.getLeft());

        Pair<Long, Iterable<UUID>> foiObs = related.fetchRelatedObservationUuidsForFeatureOfInterest(foi.getId(),
                ODataQuery.defaultQuery());
        assertEquals(Long.valueOf(0l), foiObs.getLeft());
    }

    @Test
    public void createDeleteObservations() {
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
        Observation o = createObservation(obsSvc,
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

        // Fetch Datastream for summaries before deletion
        Datastream d2 = dsSvc.findOne(d.getId());
        assertEquals(o.getPhenomenonTime(), d2.getPhenomenonTimeBegin());
        assertEquals(o2.getPhenomenonTime(), d2.getPhenomenonTimeEnd());
        assertEquals(stringToGeoJsonPolygon("{\"type\":\"Polygon\",\"coordinates\":[[[-114.0599,53.05],[-116.06,53.05],[-116.06,51.0499],[-114.0599,51.0499],[-114.0599,53.05]]]}"),
                d2.getObservedArea());

        // Delete Observation
        related.deleteObservation(o.getId());
        // Make sure Observation was deleted
        boolean exceptionThrown = false;
        try {
            readObs = obsSvc.findOne(o.getId());
        } catch (ApplicationException ae) {
            if (ae.getErrorCode() == ApplicationErrorCode.E_NotFound) {
                exceptionThrown = true;
            }
        }
        assertTrue(exceptionThrown);

        // Fetch Datastream for summaries after deletion of first Observation
        Datastream d3 = dsSvc.findOne(d.getId());
        assertEquals(o2.getPhenomenonTime(), d3.getPhenomenonTimeBegin());
        assertEquals(o2.getPhenomenonTime(), d3.getPhenomenonTimeEnd());
        assertEquals(stringToGeoJsonPolygon("{\"type\":\"Polygon\",\"coordinates\":[[[-116.0599,53.0501],[-116.0601,53.0501],[-116.0601,53.0499],[-116.0599,53.0499],[-116.0599,53.0501]]]}"),
                d3.getObservedArea());

        // Delete second Observation
        related.deleteObservation(o2.getId());
        exceptionThrown = false;
        try {
            readObs = obsSvc.findOne(o2.getId());
        } catch (ApplicationException ae) {
            if (ae.getErrorCode() == ApplicationErrorCode.E_NotFound) {
                exceptionThrown = true;
            }
        }
        assertTrue(exceptionThrown);

        // Fetch Datastream for summaries after deletion of second Observation
        Datastream d4 = dsSvc.findOne(d.getId());
        assertEquals(null, d4.getPhenomenonTimeBegin());
        assertEquals(null, d4.getPhenomenonTimeEnd());
        assertEquals(null, d4.getObservedArea());

        // Make sure relationship between Datastream/FeatureOfInterest and the Observation was removed
        Pair<Long, Iterable<UUID>> dsObs = related.fetchRelatedObservationUuidsForDatastream(d.getId(),
                ODataQuery.defaultQuery());
        assertEquals(Long.valueOf(0l), dsObs.getLeft());

        Pair<Long, Iterable<UUID>> foiObs = related.fetchRelatedObservationUuidsForFeatureOfInterest(foi.getId(),
                ODataQuery.defaultQuery());
        assertEquals(Long.valueOf(0l), foiObs.getLeft());
    }

    // TODO: Test remaining CRUD operations
}
