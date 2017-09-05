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

package com.cgi.kinota.rest.cassandra.interfaces.rest.v1;

import com.cgi.kinota.commons.domain.Datastream;
import com.cgi.kinota.commons.domain.Observation;

import com.cgi.kinota.rest.cassandra.Application;
import com.cgi.kinota.rest.cassandra.CassandraObservationTestBase;
import com.cgi.kinota.rest.cassandra.interfaces.rest.Utility;
import com.cgi.kinota.persistence.cassandra.config.SpringDataCassandraConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;

import static com.cgi.kinota.commons.Constants.*;
import static com.cgi.kinota.commons.Constants.ANNO_IOT_ID;
import static com.cgi.kinota.commons.Constants.ANNO_IOT_SELF_LINK;
import static org.junit.Assert.*;

/**
 * Created by bmiles on 4/5/17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = { Application.class, SpringDataCassandraConfig.class })
public class DataArrayExtensionTest extends CassandraObservationTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ObservationResourceTest.class);

    @Test
    public void testDataArrayRead() throws Exception {
        // Create a Location
        String locationUrl = LocationResourceTest.createLocation(generateBaseUrl(port, LocationResourceTest.URL_PATH_BASE),
                port,
                rest,
                "CCIT",
                "Calgary Centre for Innovative Technologies",
                -92.041213, 30.218805);
        String locationUuid = Utility.extractUuidForEntityUrl(locationUrl);

        // Create a Thing with a location
        String thingUrl = null;
        {
            Map<String, String> properties = new LinkedHashMap<String, String>();
            properties.put("foo", "bar");
            properties.put("baz", "qux");
            thingUrl = ThingResourceTest.createThing(generateBaseUrl(port, ThingResourceTest.URL_PATH_BASE), port, rest,
                    "test thing 1", "A thing for testing.", properties,
                    locationUuid);
            assertNotNull(thingUrl);
        }
        String thingUuid = Utility.extractUuidForEntityUrl(thingUrl);

        // Create a Sensor
        String sensorUrl = SensorResourceTest.createSensor(
                generateBaseUrl(port, SensorResourceTest.URL_PATH_BASE),
                port,
                rest,
                "TMP36",
                "TMP36 - Analog Temperature sensor",
                "application/pdf",
                "http://example.org/TMP35_36_37.pdf");
        String sensorUuid = Utility.extractUuidForEntityUrl(sensorUrl);

        // Create an ObservedProperty
        String opUrl = ObservedPropertyResourceTest.createObservedProperty(
                generateBaseUrl(port, ObservedPropertyResourceTest.URL_PATH_BASE),
                port,
                rest,
                "DewPoint Temperature",
                "http://dbpedia.org/page/Dew_point",
                ObservedPropertyResourceTest.description1);
        String observedPropertyUuid = Utility.extractUuidForEntityUrl(opUrl);

        // Create a Datastream
        String dsUrl = DatastreamResourceTest.createDatastream(generateBaseUrl(port, DatastreamResourceTest.URL_PATH_BASE), port, rest,
                "oven temperature",
                "This is a datastream measuring the air temperature in an oven.",
                "degree Celsius",
                "C", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                "http://unitsofmeasure.org/ucum.html#para-30",
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                thingUuid,
                sensorUuid,
                observedPropertyUuid);
        String datastreamUuid = Utility.extractUuidForEntityUrl(dsUrl);

        // Create a FeatureOfInterest
        String foiUrl = FeatureOfInterestResourceTest.createFeatureOfInterest(
                FeatureOfInterestResourceTest.generateBaseUrl(port, FeatureOfInterestResourceTest.URL_PATH_BASE), port, rest,
                "CCIT",
                "Calgary Centre for Innovative Technologies",
                "10",
                "10");
        String featureOfInterestUuid = Utility.extractUuidForEntityUrl(foiUrl);

        // Read observations from Datastream in dataArray format (should be zero observations)
        ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {};
        String obsRelDatastreamDataArrayUrl = dsUrl + "/" + Observation.NAME_PLURAL + "?$resultFormat=dataArray";
        ResponseEntity<String> responseStr = rest.exchange(obsRelDatastreamDataArrayUrl,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        String s = responseStr.getBody();
        JsonReader r = Json.createReader(new StringReader(s));
        JsonObject obj = r.readObject();
        JsonArray a = obj.getJsonArray("value");
        assertTrue(a.size() == 1);
        {
            JsonObject dataArrayResult = a.getJsonObject(0);
            String navLink = dataArrayResult.getString(Datastream.NAV_LINK);
            assertEquals(Datastream.NAME_PLURAL + "(" + datastreamUuid + ")", navLink);

            JsonArray c = dataArrayResult.getJsonArray(DATA_ARRAY_COMPONENTS_ATTR);
            assertTrue(c.size() == 4);
            assertEquals("id", c.getString(0));
            assertEquals("phenomenonTime", c.getString(1));
            assertEquals("resultTime", c.getString(2));
            assertEquals("result", c.getString(3));

            Integer count = dataArrayResult.getInt(ANNO_DATA_ARRAY_COUNT);
            assertEquals(Integer.valueOf(0), count);

            JsonArray dataArray = dataArrayResult.getJsonArray(DATA_ARRAY_ATTR);
            assertTrue(dataArray.size() == 0);
        }

        // Create first Observation
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("foo", "bar");
        parameters.put("baz", "qux");
        String location1 = ObservationResourceTest.createObservation(baseUrl, port, rest,
                featureOfInterestUuid,
                datastreamUuid,
                "2014-12-31T11:59:59.00+08:00", null,
                null,
                null, null,
                "42.23",
                parameters);

        // Create second Observation
        parameters = new LinkedHashMap<String, String>();
        parameters.put("one", "1");
        parameters.put("two", "2");
        String location2 = ObservationResourceTest.createObservation(baseUrl, port, rest,
                featureOfInterestUuid,
                datastreamUuid,
                "2015-01-01T00:01:00.00+08:00", null,
                null,
                null, null,
                "23.42",
                parameters);

        // Read observations from Datastream
        String obsRelDatastreamUrl = dsUrl + "/" + Observation.NAME_PLURAL;
        responseStr = rest.exchange(obsRelDatastreamUrl,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        a = obj.getJsonArray("value");
        assertTrue(a.size() == 2);
        JsonObject obs = a.getJsonObject(0);
        assertEquals(location2, obs.getString(ANNO_IOT_SELF_LINK));
        assertEquals(23.42, obs.getJsonNumber("result").doubleValue(), 0.001);
        assertEquals("2014-12-31T16:01:00.000Z", obs.getString("phenomenonTime"));
        JsonArray params = obs.getJsonArray("parameters");
        JsonObject param = params.getJsonObject(0);
        assertEquals("1", param.getString("one"));
        param = params.getJsonObject(1);
        assertEquals("2", param.getString("two"));
        String obs1Uuid = obs.getString(ANNO_IOT_ID);

        obs = a.getJsonObject(1);
        assertEquals(location1, obs.getString(ANNO_IOT_SELF_LINK));
        assertEquals(42.23, obs.getJsonNumber("result").doubleValue(), 0.001);
        assertEquals("2014-12-31T03:59:59.000Z", obs.getString("phenomenonTime"));
        params = obs.getJsonArray("parameters");
        param = params.getJsonObject(0);
        assertEquals("qux", param.getString("baz"));
        param = params.getJsonObject(1);
        assertEquals("bar", param.getString("foo"));
        String obs2Uuid = obs.getString(ANNO_IOT_ID);

        // Read observations from Datastream in dataArray format (should be 2 observations)
        responseTypeStr = new ParameterizedTypeReference<String>() {};
        obsRelDatastreamDataArrayUrl = dsUrl + "/" + Observation.NAME_PLURAL + "?$resultFormat=dataArray";
        responseStr = rest.exchange(obsRelDatastreamDataArrayUrl,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        a = obj.getJsonArray("value");
        assertTrue(a.size() == 1);
        {
            JsonObject dataArrayResult = a.getJsonObject(0);
            String navLink = dataArrayResult.getString(Datastream.NAV_LINK);
            assertEquals(Datastream.NAME_PLURAL + "(" + datastreamUuid + ")", navLink);

            JsonArray c = dataArrayResult.getJsonArray(DATA_ARRAY_COMPONENTS_ATTR);
            assertTrue(c.size() == 4);
            assertEquals("id", c.getString(0));
            assertEquals("phenomenonTime", c.getString(1));
            assertEquals("resultTime", c.getString(2));
            assertEquals("result", c.getString(3));

            Integer count = dataArrayResult.getInt(ANNO_DATA_ARRAY_COUNT);
            assertEquals(Integer.valueOf(2), count);

            JsonArray dataArray = dataArrayResult.getJsonArray(DATA_ARRAY_ATTR);
            assertTrue(dataArray.size() == 2);

            JsonArray o1 = dataArray.getJsonArray(0);
            // id
            assertEquals(obs1Uuid, o1.getString(0));
            // phenomenonTime
            assertEquals("2014-12-31T16:01:00.000Z", o1.getString(1));
            // resultTime
            assertEquals("null", o1.getString(2));
            // result
            assertEquals(23.42, o1.getJsonNumber(3).doubleValue(), 0.001);

            JsonArray o2 = dataArray.getJsonArray(1);
            // id
            assertEquals(obs2Uuid, o2.getString(0));
            // phenomenonTime
            assertEquals("2014-12-31T03:59:59.000Z", o2.getString(1));
            // resultTime
            assertEquals("null", o2.getString(2));
            // result
            assertEquals(42.23, o2.getJsonNumber(3).doubleValue(), 0.001);
        }
    }

    @Test
    public void testDataArrayReadPaging() throws Exception {
        assertEquals(Integer.valueOf(3), MAX_REQUEST_PAGE_SIZE);
        // Create a Location
        String locationUrl = LocationResourceTest.createLocation(generateBaseUrl(port, LocationResourceTest.URL_PATH_BASE),
                port,
                rest,
                "CCIT",
                "Calgary Centre for Innovative Technologies",
                -92.041213, 30.218805);
        String locationUuid = Utility.extractUuidForEntityUrl(locationUrl);

        // Create a Thing with a location
        String thingUrl = null;
        {
            Map<String, String> properties = new LinkedHashMap<String, String>();
            properties.put("foo", "bar");
            properties.put("baz", "qux");
            thingUrl = ThingResourceTest.createThing(generateBaseUrl(port, ThingResourceTest.URL_PATH_BASE), port, rest,
                    "test thing 1", "A thing for testing.", properties,
                    locationUuid);
            assertNotNull(thingUrl);
        }
        String thingUuid = Utility.extractUuidForEntityUrl(thingUrl);

        // Create a Sensor
        String sensorUrl = SensorResourceTest.createSensor(
                generateBaseUrl(port, SensorResourceTest.URL_PATH_BASE),
                port,
                rest,
                "TMP36",
                "TMP36 - Analog Temperature sensor",
                "application/pdf",
                "http://example.org/TMP35_36_37.pdf");
        String sensorUuid = Utility.extractUuidForEntityUrl(sensorUrl);

        // Create an ObservedProperty
        String opUrl = ObservedPropertyResourceTest.createObservedProperty(
                generateBaseUrl(port, ObservedPropertyResourceTest.URL_PATH_BASE),
                port,
                rest,
                "DewPoint Temperature",
                "http://dbpedia.org/page/Dew_point",
                ObservedPropertyResourceTest.description1);
        String observedPropertyUuid = Utility.extractUuidForEntityUrl(opUrl);

        // Create a Datastream
        String dsUrl = DatastreamResourceTest.createDatastream(generateBaseUrl(port, DatastreamResourceTest.URL_PATH_BASE), port, rest,
                "oven temperature",
                "This is a datastream measuring the air temperature in an oven.",
                "degree Celsius",
                "C", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                "http://unitsofmeasure.org/ucum.html#para-30",
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                thingUuid,
                sensorUuid,
                observedPropertyUuid);
        String datastreamUuid = Utility.extractUuidForEntityUrl(dsUrl);

        // Create a FeatureOfInterest
        String foiUrl = FeatureOfInterestResourceTest.createFeatureOfInterest(
                FeatureOfInterestResourceTest.generateBaseUrl(port, FeatureOfInterestResourceTest.URL_PATH_BASE), port, rest,
                "CCIT",
                "Calgary Centre for Innovative Technologies",
                "10",
                "10");
        String featureOfInterestUuid = Utility.extractUuidForEntityUrl(foiUrl);

        // Create first Observation
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("foo", "bar");
        parameters.put("baz", "qux");
        String location1 = ObservationResourceTest.createObservation(baseUrl, port, rest,
                featureOfInterestUuid,
                datastreamUuid,
                "2014-12-31T11:59:59.00+08:00", null,
                null,
                null, null,
                "42.23",
                parameters);

        // Create second Observation
        parameters = new LinkedHashMap<String, String>();
        parameters.put("one", "1");
        parameters.put("two", "2");
        String location2 = ObservationResourceTest.createObservation(baseUrl, port, rest,
                featureOfInterestUuid,
                datastreamUuid,
                "2015-01-01T00:01:00.00+08:00", null,
                null,
                null, null,
                "23.42",
                parameters);

        // Create third Observation
        parameters = new LinkedHashMap<String, String>();
        parameters.put("three", "3");
        parameters.put("four", "4");
        String location3 = ObservationResourceTest.createObservation(baseUrl, port, rest,
                featureOfInterestUuid,
                datastreamUuid,
                "2015-01-01T00:02:00.00+08:00", null,
                null,
                null, null,
                "33.52",
                parameters);

        // Create fourth Observation
        parameters = new LinkedHashMap<String, String>();
        parameters.put("five", "5");
        parameters.put("six", "6");
        String location4 = ObservationResourceTest.createObservation(baseUrl, port, rest,
                featureOfInterestUuid,
                datastreamUuid,
                "2015-01-01T00:03:00.00+08:00", null,
                null,
                null, null,
                "43.62",
                parameters);

        // Read observations from Datastream in dataArray format (should be 4 observations across 2 pages)
        ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {};
        String obsRelDatastreamDataArrayUrl = dsUrl + "/" + Observation.NAME_PLURAL + "?$resultFormat=dataArray";
        ResponseEntity<String> responseStr = rest.exchange(obsRelDatastreamDataArrayUrl,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        String s = responseStr.getBody();
        JsonReader r = Json.createReader(new StringReader(s));
        JsonObject obj = r.readObject();
        JsonArray a = obj.getJsonArray("value");
        assertTrue(a.size() == 1);

        JsonObject dataArrayResult = a.getJsonObject(0);
        String navLink = dataArrayResult.getString(Datastream.NAV_LINK);
        assertEquals(Datastream.NAME_PLURAL + "(" + datastreamUuid + ")", navLink);

        JsonArray c = dataArrayResult.getJsonArray(DATA_ARRAY_COMPONENTS_ATTR);
        assertTrue(c.size() == 4);
        assertEquals("id", c.getString(0));
        assertEquals("phenomenonTime", c.getString(1));
        assertEquals("resultTime", c.getString(2));
        assertEquals("result", c.getString(3));

        Integer count = dataArrayResult.getInt(ANNO_DATA_ARRAY_COUNT);
        assertEquals(Integer.valueOf(4), count);

        JsonArray dataArray = dataArrayResult.getJsonArray(DATA_ARRAY_ATTR);
        assertEquals(Integer.valueOf(3), Integer.valueOf(dataArray.size()));

        String nextLink = dataArrayResult.getString(ANNO_IOT_NEXT_LINK);
        assertNotNull(nextLink);

        // Fetch nextLink
        responseStr = rest.exchange(nextLink,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        String s2 = responseStr.getBody();
        r = Json.createReader(new StringReader(s2));
        obj = r.readObject();
        a = obj.getJsonArray("value");
        assertTrue(a.size() == 1);

        dataArrayResult = a.getJsonObject(0);
        Integer count2 = dataArrayResult.getInt(ANNO_DATA_ARRAY_COUNT);
        assertEquals(count, count2);

        dataArray = dataArrayResult.getJsonArray(DATA_ARRAY_ATTR);
        assertEquals(Integer.valueOf(1), Integer.valueOf(dataArray.size()));

        boolean exceptionThrown = false;
        try {
            nextLink = dataArrayResult.getString(ANNO_IOT_NEXT_LINK);
        } catch (NullPointerException npe) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
    }

    @Test
    public void testDataArrayCreate() throws Exception {
        // Create a Location
        String locationUrl = LocationResourceTest.createLocation(generateBaseUrl(port, LocationResourceTest.URL_PATH_BASE),
                port,
                rest,
                "CCIT",
                "Calgary Centre for Innovative Technologies",
                -92.041213, 30.218805);
        String locationUuid = Utility.extractUuidForEntityUrl(locationUrl);

        // Create a Thing with a location
        String thingUrl = null;
        {
            Map<String, String> properties = new LinkedHashMap<String, String>();
            properties.put("foo", "bar");
            properties.put("baz", "qux");
            thingUrl = ThingResourceTest.createThing(generateBaseUrl(port, ThingResourceTest.URL_PATH_BASE), port, rest,
                    "test thing 1", "A thing for testing.", properties,
                    locationUuid);
            assertNotNull(thingUrl);
        }
        String thingUuid = Utility.extractUuidForEntityUrl(thingUrl);

        // Create a Sensor
        String sensorUrl = SensorResourceTest.createSensor(
                generateBaseUrl(port, SensorResourceTest.URL_PATH_BASE),
                port,
                rest,
                "TMP36",
                "TMP36 - Analog Temperature sensor",
                "application/pdf",
                "http://example.org/TMP35_36_37.pdf");
        String sensorUuid = Utility.extractUuidForEntityUrl(sensorUrl);

        // Create a second Sensor
        String sensorUrl2 = SensorResourceTest.createSensor(
                generateBaseUrl(port, SensorResourceTest.URL_PATH_BASE),
                port,
                rest,
                "RH36",
                "RH36 - Analog Relative Humidity sensor",
                "application/pdf",
                "http://example.org/TMP35_36_37.pdf");
        String sensorUuid2 = Utility.extractUuidForEntityUrl(sensorUrl2);

        // Create an ObservedProperty
        String opUrl = ObservedPropertyResourceTest.createObservedProperty(
                generateBaseUrl(port, ObservedPropertyResourceTest.URL_PATH_BASE),
                port,
                rest,
                "Air Temperature",
                "http://dbpedia.org/page/Air_temp",
                ObservedPropertyResourceTest.description1);
        String observedPropertyUuid = Utility.extractUuidForEntityUrl(opUrl);

        // Create a second ObservedProperty
        String opUrl2 = ObservedPropertyResourceTest.createObservedProperty(
                generateBaseUrl(port, ObservedPropertyResourceTest.URL_PATH_BASE),
                port,
                rest,
                "Relative Humidity",
                "http://dbpedia.org/page/RH",
                ObservedPropertyResourceTest.description1);
        String observedPropertyUuid2 = Utility.extractUuidForEntityUrl(opUrl2);

        // Create a Datastream
        String dsUrl = DatastreamResourceTest.createDatastream(generateBaseUrl(port, DatastreamResourceTest.URL_PATH_BASE), port, rest,
                "oven temperature",
                "This is a datastream measuring the air temperature in an oven.",
                "degree Celsius",
                "C", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                "http://unitsofmeasure.org/ucum.html#para-30",
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                thingUuid,
                sensorUuid,
                observedPropertyUuid);
        String datastreamUuid = Utility.extractUuidForEntityUrl(dsUrl);

        // Create a second Datastream
        String dsUrl2 = DatastreamResourceTest.createDatastream(generateBaseUrl(port, DatastreamResourceTest.URL_PATH_BASE), port, rest,
                "oven RH",
                "This is a datastream measuring the relative humidity in an oven.",
                "percentage",
                "%", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                "http://unitsofmeasure.org/ucum.html#para-30",
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                thingUuid,
                sensorUuid,
                observedPropertyUuid);
        String datastreamUuid2 = Utility.extractUuidForEntityUrl(dsUrl2);

        // Create a FeatureOfInterest
        String foiUrl = FeatureOfInterestResourceTest.createFeatureOfInterest(
                FeatureOfInterestResourceTest.generateBaseUrl(port, FeatureOfInterestResourceTest.URL_PATH_BASE), port, rest,
                "CCIT",
                "Calgary Centre for Innovative Technologies",
                "10",
                "10");
        String featureOfInterestUuid = Utility.extractUuidForEntityUrl(foiUrl);

        // Observations
        String phenomTime1 = "2014-12-31T11:59:59.00+08:00";
        String phenomTime2 = "2015-01-01T00:01:00.00+08:00";
        String param1Key = "raw";
        String param2Key = "DN";

        String ds1Obs1 = "123.45";
        String ds1Obs1Param1Val = "12.12";
        String ds1Obs1Param2Val = "21.21";

        String ds1Obs2 = "543.21";
        String ds1Obs2Param1Val = "23.23";
        String ds1Obs2Param2Val = "32.32";

        String ds2Obs1 = "99.0";
        String ds2Obs1Param1Val = "34.34";
        String ds2Obs1Param2Val = "43.43";

        String ds2Obs2 = "42.5";
        String ds2Obs2Param1Val = "45.45";
        String ds2Obs2Param2Val = "54.54";

        // Construct dataArray JSON
        String requestStr = "[" +
                // First Datastream
                "{" +
                    "\"Datastream\": {\"@iot.id\": \"" + datastreamUuid + "\"}," +
                    "\"components\": [\"phenomenonTime\", \"result\", \"FeatureOfInterest/id\", \"parameters\"]," +
                    "\"dataArray@iot.count\": 2," +
                    "\"dataArray\":[" +
                        "[\"" + phenomTime1 + "\"," + ds1Obs1 + "," + "\"" + featureOfInterestUuid + "\"," +
                            "{" +
                                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                            "}" +
                        "]," +
                        "[\"" + phenomTime2 + "\"," + ds1Obs2 + "," + "\"" + featureOfInterestUuid + "\"," +
                            "{" +
                            "\"" + param1Key + "\":\"" + ds1Obs2Param1Val + "\"," +
                            "\"" + param2Key + "\":\"" + ds1Obs2Param2Val + "\"" +
                            "}" +
                        "]" +
                    "]" +
                "}," +
                // Second Datastream
                "{" +
                    "\"Datastream\": {\"@iot.id\": \"" + datastreamUuid2 + "\" }," +
                    "\"components\": [\"phenomenonTime\", \"result\", \"FeatureOfInterest/id\", \"parameters\"]," +
                    "\"dataArray@iot.count\": 2," +
                    "\"dataArray\":[" +
                        "[\"" + phenomTime1 + "\"," + ds2Obs1 + "," + "\"" + featureOfInterestUuid + "\"," +
                            "{" +
                            "\"" + param1Key + "\":\"" + ds2Obs1Param1Val + "\"," +
                            "\"" + param2Key + "\":\"" + ds2Obs1Param2Val + "\"" +
                            "}" +
                        "]," +
                        "[\"" + phenomTime2 + "\"," + ds2Obs2 + "," + "\"" + featureOfInterestUuid + "\"," +
                            "{" +
                            "\"" + param1Key + "\":\"" + ds2Obs2Param1Val + "\"," +
                            "\"" + param2Key + "\":\"" + ds2Obs2Param2Val + "\"" +
                            "}" +
                        "]" +
                    "]" +
                "}" +
                "]";

        String createObservationsUrl = apiRootUrl + "CreateObservations";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addTokenToHeader(port, headers);
        // Read as a string because Spring's web client doesn't know how to read a JSON Array response
        HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
        ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<String>() {};
        ResponseEntity<String> response = rest.exchange(createObservationsUrl,
                HttpMethod.POST, entity,
                responseType);
        assertEquals(201, response.getStatusCodeValue());

        // Read response and make sure no elements contain "error",
        //   and that URIs of created Observations match the
        //   expected URI format.
        String s = response.getBody();
        JsonReader r = Json.createReader(new StringReader(s));
        JsonArray a = r.readArray();
        r.close();
        for (int i = 0; i < a.size(); i++) {
            String creationResult = a.getString(i);
            assertNotEquals("error", creationResult);
            URI u = URI.create(creationResult);
            Matcher m = LOC_PATT.matcher(u.getPath());
            assertTrue(m.matches());
        }
    }

    @Test
    public void testDataArrayCreateNoFeatureOfInterest() throws Exception {
        // Create a Location
        String locationUrl = LocationResourceTest.createLocation(generateBaseUrl(port, LocationResourceTest.URL_PATH_BASE),
                port,
                rest,
                "CCIT",
                "Calgary Centre for Innovative Technologies",
                -92.041213, 30.218805);
        String locationUuid = Utility.extractUuidForEntityUrl(locationUrl);

        // Create a Thing with a location
        String thingUrl = null;
        {
            Map<String, String> properties = new LinkedHashMap<String, String>();
            properties.put("foo", "bar");
            properties.put("baz", "qux");
            thingUrl = ThingResourceTest.createThing(generateBaseUrl(port, ThingResourceTest.URL_PATH_BASE), port, rest,
                    "test thing 1", "A thing for testing.", properties,
                    locationUuid);
            assertNotNull(thingUrl);
        }
        String thingUuid = Utility.extractUuidForEntityUrl(thingUrl);

        // Create a Sensor
        String sensorUrl = SensorResourceTest.createSensor(
                generateBaseUrl(port, SensorResourceTest.URL_PATH_BASE),
                port,
                rest,
                "TMP36",
                "TMP36 - Analog Temperature sensor",
                "application/pdf",
                "http://example.org/TMP35_36_37.pdf");
        String sensorUuid = Utility.extractUuidForEntityUrl(sensorUrl);

        // Create a second Sensor
        String sensorUrl2 = SensorResourceTest.createSensor(
                generateBaseUrl(port, SensorResourceTest.URL_PATH_BASE),
                port,
                rest,
                "RH36",
                "RH36 - Analog Relative Humidity sensor",
                "application/pdf",
                "http://example.org/TMP35_36_37.pdf");
        String sensorUuid2 = Utility.extractUuidForEntityUrl(sensorUrl2);

        // Create an ObservedProperty
        String opUrl = ObservedPropertyResourceTest.createObservedProperty(
                generateBaseUrl(port, ObservedPropertyResourceTest.URL_PATH_BASE),
                port,
                rest,
                "Air Temperature",
                "http://dbpedia.org/page/Air_temp",
                ObservedPropertyResourceTest.description1);
        String observedPropertyUuid = Utility.extractUuidForEntityUrl(opUrl);

        // Create a second ObservedProperty
        String opUrl2 = ObservedPropertyResourceTest.createObservedProperty(
                generateBaseUrl(port, ObservedPropertyResourceTest.URL_PATH_BASE),
                port,
                rest,
                "Relative Humidity",
                "http://dbpedia.org/page/RH",
                ObservedPropertyResourceTest.description1);
        String observedPropertyUuid2 = Utility.extractUuidForEntityUrl(opUrl2);

        // Create a Datastream
        String dsUrl = DatastreamResourceTest.createDatastream(generateBaseUrl(port, DatastreamResourceTest.URL_PATH_BASE), port, rest,
                "oven temperature",
                "This is a datastream measuring the air temperature in an oven.",
                "degree Celsius",
                "C", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                "http://unitsofmeasure.org/ucum.html#para-30",
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                thingUuid,
                sensorUuid,
                observedPropertyUuid);
        String datastreamUuid = Utility.extractUuidForEntityUrl(dsUrl);

        // Create a second Datastream
        String dsUrl2 = DatastreamResourceTest.createDatastream(generateBaseUrl(port, DatastreamResourceTest.URL_PATH_BASE), port, rest,
                "oven RH",
                "This is a datastream measuring the relative humidity in an oven.",
                "percentage",
                "%", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                "http://unitsofmeasure.org/ucum.html#para-30",
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                thingUuid,
                sensorUuid,
                observedPropertyUuid);
        String datastreamUuid2 = Utility.extractUuidForEntityUrl(dsUrl2);

        // Observations
        String phenomTime1 = "2014-12-31T11:59:59.00+08:00";
        String phenomTime2 = "2015-01-01T00:01:00.00+08:00";
        String param1Key = "raw";
        String param2Key = "DN";

        String ds1Obs1 = "123.45";
        String ds1Obs1Param1Val = "12.12";
        String ds1Obs1Param2Val = "21.21";

        String ds1Obs2 = "543.21";
        String ds1Obs2Param1Val = "23.23";
        String ds1Obs2Param2Val = "32.32";

        String ds2Obs1 = "99.0";
        String ds2Obs1Param1Val = "34.34";
        String ds2Obs1Param2Val = "43.43";

        String ds2Obs2 = "42.5";
        String ds2Obs2Param1Val = "45.45";
        String ds2Obs2Param2Val = "54.54";

        // Construct dataArray JSON
        String requestStr = "[" +
                // First Datastream
                "{" +
                    "\"Datastream\": {\"@iot.id\": \"" + datastreamUuid + "\"}," +
                    "\"components\": [\"phenomenonTime\", \"result\", \"parameters\"]," +
                    "\"dataArray@iot.count\": 2," +
                    "\"dataArray\":[" +
                        "[\"" + phenomTime1 + "\"," + ds1Obs1 + "," +
                            "{" +
                            "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                            "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                            "}" +
                        "]," +
                        "[\"" + phenomTime2 + "\"," + ds1Obs2 + "," +
                            "{" +
                            "\"" + param1Key + "\":\"" + ds1Obs2Param1Val + "\"," +
                            "\"" + param2Key + "\":\"" + ds1Obs2Param2Val + "\"" +
                            "}" +
                        "]" +
                    "]" +
                "}," +
                // Second Datastream
                "{" +
                    "\"Datastream\": {\"@iot.id\": \"" + datastreamUuid2 + "\" }," +
                    "\"components\": [\"phenomenonTime\", \"result\", \"parameters\"]," +
                    "\"dataArray@iot.count\": 2," +
                    "\"dataArray\":[" +
                        "[\"" + phenomTime1 + "\"," + ds2Obs1 + "," +
                            "{" +
                            "\"" + param1Key + "\":\"" + ds2Obs1Param1Val + "\"," +
                            "\"" + param2Key + "\":\"" + ds2Obs1Param2Val + "\"" +
                            "}" +
                        "]," +
                        "[\"" + phenomTime2 + "\"," + ds2Obs2 + "," +
                            "{" +
                            "\"" + param1Key + "\":\"" + ds2Obs2Param1Val + "\"," +
                            "\"" + param2Key + "\":\"" + ds2Obs2Param2Val + "\"" +
                            "}" +
                        "]" +
                    "]" +
                "}" +
                "]";

        String createObservationsUrl = apiRootUrl + "CreateObservations";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addTokenToHeader(port, headers);
        // Read as a string because Spring's web client doesn't know how to read a JSON Array response
        HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
        ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<String>() {};
        ResponseEntity<String> response = rest.exchange(createObservationsUrl,
                HttpMethod.POST, entity,
                responseType);
        assertEquals(201, response.getStatusCodeValue());

        // Read response and make sure no elements contain "error",
        //   and that URIs of created Observations match the
        //   expected URI format.
        String s = response.getBody();
        JsonReader r = Json.createReader(new StringReader(s));
        JsonArray a = r.readArray();
        r.close();
        for (int i = 0; i < a.size(); i++) {
            String creationResult = a.getString(i);
            assertNotEquals("error", creationResult);
            URI u = URI.create(creationResult);
            Matcher m = LOC_PATT.matcher(u.getPath());
            assertTrue(m.matches());
        }
    }

    @Test
    public void testDataArrayCreateNoFeatureOfInterestOMMeasurement() throws Exception {
        // Create a Location
        String locationUrl = LocationResourceTest.createLocation(generateBaseUrl(port, LocationResourceTest.URL_PATH_BASE),
                port,
                rest,
                "CCIT",
                "Calgary Centre for Innovative Technologies",
                -92.041213, 30.218805);
        String locationUuid = Utility.extractUuidForEntityUrl(locationUrl);

        // Create a Thing with a location
        String thingUrl = null;
        {
            Map<String, String> properties = new LinkedHashMap<String, String>();
            properties.put("foo", "bar");
            properties.put("baz", "qux");
            thingUrl = ThingResourceTest.createThing(generateBaseUrl(port, ThingResourceTest.URL_PATH_BASE), port, rest,
                    "test thing 1", "A thing for testing.", properties,
                    locationUuid);
            assertNotNull(thingUrl);
        }
        String thingUuid = Utility.extractUuidForEntityUrl(thingUrl);

        // Create a Sensor
        String sensorUrl = SensorResourceTest.createSensor(
                generateBaseUrl(port, SensorResourceTest.URL_PATH_BASE),
                port,
                rest,
                "TMP36",
                "TMP36 - Analog Temperature sensor",
                "application/pdf",
                "http://example.org/TMP35_36_37.pdf");
        String sensorUuid = Utility.extractUuidForEntityUrl(sensorUrl);

        // Create a second Sensor
        String sensorUrl2 = SensorResourceTest.createSensor(
                generateBaseUrl(port, SensorResourceTest.URL_PATH_BASE),
                port,
                rest,
                "RH36",
                "RH36 - Analog Relative Humidity sensor",
                "application/pdf",
                "http://example.org/TMP35_36_37.pdf");
        String sensorUuid2 = Utility.extractUuidForEntityUrl(sensorUrl2);

        // Create an ObservedProperty
        String opUrl = ObservedPropertyResourceTest.createObservedProperty(
                generateBaseUrl(port, ObservedPropertyResourceTest.URL_PATH_BASE),
                port,
                rest,
                "Air Temperature",
                "http://dbpedia.org/page/Air_temp",
                ObservedPropertyResourceTest.description1);
        String observedPropertyUuid = Utility.extractUuidForEntityUrl(opUrl);

        // Create a second ObservedProperty
        String opUrl2 = ObservedPropertyResourceTest.createObservedProperty(
                generateBaseUrl(port, ObservedPropertyResourceTest.URL_PATH_BASE),
                port,
                rest,
                "Relative Humidity",
                "http://dbpedia.org/page/RH",
                ObservedPropertyResourceTest.description1);
        String observedPropertyUuid2 = Utility.extractUuidForEntityUrl(opUrl2);

        // Create a Datastream
        String dsUrl = DatastreamResourceTest.createDatastream(generateBaseUrl(port, DatastreamResourceTest.URL_PATH_BASE), port, rest,
                "oven temperature",
                "This is a datastream measuring the air temperature in an oven.",
                "degree Celsius",
                "C", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                "http://unitsofmeasure.org/ucum.html#para-30",
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                thingUuid,
                sensorUuid,
                observedPropertyUuid);
        String datastreamUuid = Utility.extractUuidForEntityUrl(dsUrl);

        // Create a second Datastream
        String dsUrl2 = DatastreamResourceTest.createDatastream(generateBaseUrl(port, DatastreamResourceTest.URL_PATH_BASE), port, rest,
                "oven RH",
                "This is a datastream measuring the relative humidity in an oven.",
                "percentage",
                "%", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                "http://unitsofmeasure.org/ucum.html#para-30",
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                thingUuid,
                sensorUuid,
                observedPropertyUuid);
        String datastreamUuid2 = Utility.extractUuidForEntityUrl(dsUrl2);

        // Observations
        String phenomTime1 = "2014-12-31T00:01:00.00+08:00";
        String phenomTime2 = "2015-01-01T00:01:00.00+08:00";
        String phenomTime3 = "2015-01-02T00:01:00.00+08:00";
        String phenomTime4 = "2015-01-03T00:01:00.00+08:00";
        String phenomTime5 = "2015-01-04T00:01:00.00+08:00";

        String param1Key = "raw";
        String param2Key = "DN";

        String ds1Obs1Param1Val = "12.12";
        String ds1Obs1Param2Val = "21.21";

        String ds1_OM_Measurement = "123.45";
        String ds1_OM_CountObservation = "543";
        String ds1_OM_TruthObservation = "true";
        String ds1_OM_Observation = "\"an arbitrary string\"";
        String ds1_OM_CategoryObservation = "\"http://some.uri/of/some/definition#section1.1\"";

        // Construct dataArray JSON
        String requestStr = "[" +
                // First Datastream
                "{" +
                "\"Datastream\": {\"@iot.id\": \"" + datastreamUuid + "\"}," +
                "\"components\": [\"phenomenonTime\", \"result\", \"parameters\"]," +
                "\"dataArray@iot.count\": 5," +
                "\"dataArray\":[" +
                "[\"" + phenomTime1 + "\"," + ds1_OM_Measurement + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]," +
                "[\"" + phenomTime2 + "\"," + ds1_OM_CountObservation + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]," +
                "[\"" + phenomTime3 + "\"," + ds1_OM_TruthObservation + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]," +
                "[\"" + phenomTime4 + "\"," + ds1_OM_Observation + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]," +
                "[\"" + phenomTime5 + "\"," + ds1_OM_CategoryObservation + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]" +
                "]" +
                "}" +
                "]";

        String createObservationsUrl = apiRootUrl + "CreateObservations";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addTokenToHeader(port, headers);
        // Read as a string because Spring's web client doesn't know how to read a JSON Array response
        HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
        ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<String>() {};
        ResponseEntity<String> response = rest.exchange(createObservationsUrl,
                HttpMethod.POST, entity,
                responseType);
        assertEquals(201, response.getStatusCodeValue());

        // Read response and make sure no elements contain "error",
        //   and that URIs of created Observations match the
        //   expected URI format.
        String s = response.getBody();
        JsonReader r = Json.createReader(new StringReader(s));
        JsonArray a = r.readArray();
        r.close();

        assertEquals(5, a.size());
        String creationResult = a.getString(0);
        assertNotEquals("error", creationResult);
        URI u = URI.create(creationResult);
        Matcher m = LOC_PATT.matcher(u.getPath());
        assertTrue(m.matches());

        creationResult = a.getString(1);
        assertNotEquals("error", creationResult);
        u = URI.create(creationResult);
        m = LOC_PATT.matcher(u.getPath());
        assertTrue(m.matches());

        creationResult = a.getString(2);
        assertEquals("error", creationResult);

        creationResult = a.getString(3);
        assertEquals("error", creationResult);

        creationResult = a.getString(4);
        assertEquals("error", creationResult);
    }

    @Test
    public void testDataArrayCreateNoFeatureOfInterestOMCountObservation() throws Exception {
        // Create a Location
        String locationUrl = LocationResourceTest.createLocation(generateBaseUrl(port, LocationResourceTest.URL_PATH_BASE),
                port,
                rest,
                "CCIT",
                "Calgary Centre for Innovative Technologies",
                -92.041213, 30.218805);
        String locationUuid = Utility.extractUuidForEntityUrl(locationUrl);

        // Create a Thing with a location
        String thingUrl = null;
        {
            Map<String, String> properties = new LinkedHashMap<String, String>();
            properties.put("foo", "bar");
            properties.put("baz", "qux");
            thingUrl = ThingResourceTest.createThing(generateBaseUrl(port, ThingResourceTest.URL_PATH_BASE), port, rest,
                    "test thing 1", "A thing for testing.", properties,
                    locationUuid);
            assertNotNull(thingUrl);
        }
        String thingUuid = Utility.extractUuidForEntityUrl(thingUrl);

        // Create a Sensor
        String sensorUrl = SensorResourceTest.createSensor(
                generateBaseUrl(port, SensorResourceTest.URL_PATH_BASE),
                port,
                rest,
                "TMP36",
                "TMP36 - Analog Temperature sensor",
                "application/pdf",
                "http://example.org/TMP35_36_37.pdf");
        String sensorUuid = Utility.extractUuidForEntityUrl(sensorUrl);

        // Create a second Sensor
        String sensorUrl2 = SensorResourceTest.createSensor(
                generateBaseUrl(port, SensorResourceTest.URL_PATH_BASE),
                port,
                rest,
                "RH36",
                "RH36 - Analog Relative Humidity sensor",
                "application/pdf",
                "http://example.org/TMP35_36_37.pdf");
        String sensorUuid2 = Utility.extractUuidForEntityUrl(sensorUrl2);

        // Create an ObservedProperty
        String opUrl = ObservedPropertyResourceTest.createObservedProperty(
                generateBaseUrl(port, ObservedPropertyResourceTest.URL_PATH_BASE),
                port,
                rest,
                "Air Temperature",
                "http://dbpedia.org/page/Air_temp",
                ObservedPropertyResourceTest.description1);
        String observedPropertyUuid = Utility.extractUuidForEntityUrl(opUrl);

        // Create a second ObservedProperty
        String opUrl2 = ObservedPropertyResourceTest.createObservedProperty(
                generateBaseUrl(port, ObservedPropertyResourceTest.URL_PATH_BASE),
                port,
                rest,
                "Relative Humidity",
                "http://dbpedia.org/page/RH",
                ObservedPropertyResourceTest.description1);
        String observedPropertyUuid2 = Utility.extractUuidForEntityUrl(opUrl2);

        // Create a Datastream
        String dsUrl = DatastreamResourceTest.createDatastream(generateBaseUrl(port, DatastreamResourceTest.URL_PATH_BASE), port, rest,
                "oven temperature",
                "This is a datastream measuring the air temperature in an oven.",
                "degree Celsius",
                "C", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                "http://unitsofmeasure.org/ucum.html#para-30",
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_CountObservation",
                thingUuid,
                sensorUuid,
                observedPropertyUuid);
        String datastreamUuid = Utility.extractUuidForEntityUrl(dsUrl);

        // Create a second Datastream
        String dsUrl2 = DatastreamResourceTest.createDatastream(generateBaseUrl(port, DatastreamResourceTest.URL_PATH_BASE), port, rest,
                "oven RH",
                "This is a datastream measuring the relative humidity in an oven.",
                "percentage",
                "%", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                "http://unitsofmeasure.org/ucum.html#para-30",
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                thingUuid,
                sensorUuid,
                observedPropertyUuid);
        String datastreamUuid2 = Utility.extractUuidForEntityUrl(dsUrl2);

        // Observations
        String phenomTime1 = "2014-12-31T00:01:00.00+08:00";
        String phenomTime2 = "2015-01-01T00:01:00.00+08:00";
        String phenomTime3 = "2015-01-02T00:01:00.00+08:00";
        String phenomTime4 = "2015-01-03T00:01:00.00+08:00";
        String phenomTime5 = "2015-01-04T00:01:00.00+08:00";

        String param1Key = "raw";
        String param2Key = "DN";

        String ds1Obs1Param1Val = "12.12";
        String ds1Obs1Param2Val = "21.21";

        String ds1_OM_Measurement = "123.45";
        String ds1_OM_CountObservation = "543";
        String ds1_OM_TruthObservation = "true";
        String ds1_OM_Observation = "\"an arbitrary string\"";
        String ds1_OM_CategoryObservation = "\"http://some.uri/of/some/definition#section1.1\"";

        // Construct dataArray JSON
        String requestStr = "[" +
                // First Datastream
                "{" +
                "\"Datastream\": {\"@iot.id\": \"" + datastreamUuid + "\"}," +
                "\"components\": [\"phenomenonTime\", \"result\", \"parameters\"]," +
                "\"dataArray@iot.count\": 5," +
                "\"dataArray\":[" +
                "[\"" + phenomTime1 + "\"," + ds1_OM_Measurement + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]," +
                "[\"" + phenomTime2 + "\"," + ds1_OM_CountObservation + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]," +
                "[\"" + phenomTime3 + "\"," + ds1_OM_TruthObservation + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]," +
                "[\"" + phenomTime4 + "\"," + ds1_OM_Observation + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]," +
                "[\"" + phenomTime5 + "\"," + ds1_OM_CategoryObservation + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]" +
                "]" +
                "}" +
                "]";

        String createObservationsUrl = apiRootUrl + "CreateObservations";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addTokenToHeader(port, headers);
        // Read as a string because Spring's web client doesn't know how to read a JSON Array response
        HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
        ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<String>() {};
        ResponseEntity<String> response = rest.exchange(createObservationsUrl,
                HttpMethod.POST, entity,
                responseType);
        assertEquals(201, response.getStatusCodeValue());

        // Read response and make sure no elements contain "error",
        //   and that URIs of created Observations match the
        //   expected URI format.
        String s = response.getBody();
        JsonReader r = Json.createReader(new StringReader(s));
        JsonArray a = r.readArray();
        r.close();

        assertEquals(5, a.size());
        String creationResult = a.getString(0);
        assertEquals("error", creationResult);

        creationResult = a.getString(1);
        assertNotEquals("error", creationResult);
        URI u = URI.create(creationResult);
        Matcher m = LOC_PATT.matcher(u.getPath());
        assertTrue(m.matches());

        creationResult = a.getString(2);
        assertEquals("error", creationResult);

        creationResult = a.getString(3);
        assertEquals("error", creationResult);

        creationResult = a.getString(4);
        assertEquals("error", creationResult);
    }

    @Test
    public void testDataArrayCreateNoFeatureOfInterestOMTruthObservation() throws Exception {
        // Create a Location
        String locationUrl = LocationResourceTest.createLocation(generateBaseUrl(port, LocationResourceTest.URL_PATH_BASE),
                port,
                rest,
                "CCIT",
                "Calgary Centre for Innovative Technologies",
                -92.041213, 30.218805);
        String locationUuid = Utility.extractUuidForEntityUrl(locationUrl);

        // Create a Thing with a location
        String thingUrl = null;
        {
            Map<String, String> properties = new LinkedHashMap<String, String>();
            properties.put("foo", "bar");
            properties.put("baz", "qux");
            thingUrl = ThingResourceTest.createThing(generateBaseUrl(port, ThingResourceTest.URL_PATH_BASE), port, rest,
                    "test thing 1", "A thing for testing.", properties,
                    locationUuid);
            assertNotNull(thingUrl);
        }
        String thingUuid = Utility.extractUuidForEntityUrl(thingUrl);

        // Create a Sensor
        String sensorUrl = SensorResourceTest.createSensor(
                generateBaseUrl(port, SensorResourceTest.URL_PATH_BASE),
                port,
                rest,
                "TMP36",
                "TMP36 - Analog Temperature sensor",
                "application/pdf",
                "http://example.org/TMP35_36_37.pdf");
        String sensorUuid = Utility.extractUuidForEntityUrl(sensorUrl);

        // Create a second Sensor
        String sensorUrl2 = SensorResourceTest.createSensor(
                generateBaseUrl(port, SensorResourceTest.URL_PATH_BASE),
                port,
                rest,
                "RH36",
                "RH36 - Analog Relative Humidity sensor",
                "application/pdf",
                "http://example.org/TMP35_36_37.pdf");
        String sensorUuid2 = Utility.extractUuidForEntityUrl(sensorUrl2);

        // Create an ObservedProperty
        String opUrl = ObservedPropertyResourceTest.createObservedProperty(
                generateBaseUrl(port, ObservedPropertyResourceTest.URL_PATH_BASE),
                port,
                rest,
                "Air Temperature",
                "http://dbpedia.org/page/Air_temp",
                ObservedPropertyResourceTest.description1);
        String observedPropertyUuid = Utility.extractUuidForEntityUrl(opUrl);

        // Create a second ObservedProperty
        String opUrl2 = ObservedPropertyResourceTest.createObservedProperty(
                generateBaseUrl(port, ObservedPropertyResourceTest.URL_PATH_BASE),
                port,
                rest,
                "Relative Humidity",
                "http://dbpedia.org/page/RH",
                ObservedPropertyResourceTest.description1);
        String observedPropertyUuid2 = Utility.extractUuidForEntityUrl(opUrl2);

        // Create a Datastream
        String dsUrl = DatastreamResourceTest.createDatastream(generateBaseUrl(port, DatastreamResourceTest.URL_PATH_BASE), port, rest,
                "oven temperature",
                "This is a datastream measuring the air temperature in an oven.",
                "degree Celsius",
                "C", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                "http://unitsofmeasure.org/ucum.html#para-30",
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_TruthObservation",
                thingUuid,
                sensorUuid,
                observedPropertyUuid);
        String datastreamUuid = Utility.extractUuidForEntityUrl(dsUrl);

        // Create a second Datastream
        String dsUrl2 = DatastreamResourceTest.createDatastream(generateBaseUrl(port, DatastreamResourceTest.URL_PATH_BASE), port, rest,
                "oven RH",
                "This is a datastream measuring the relative humidity in an oven.",
                "percentage",
                "%", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                "http://unitsofmeasure.org/ucum.html#para-30",
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                thingUuid,
                sensorUuid,
                observedPropertyUuid);
        String datastreamUuid2 = Utility.extractUuidForEntityUrl(dsUrl2);

        // Observations
        String phenomTime1 = "2014-12-31T00:01:00.00+08:00";
        String phenomTime2 = "2015-01-01T00:01:00.00+08:00";
        String phenomTime3 = "2015-01-02T00:01:00.00+08:00";
        String phenomTime4 = "2015-01-03T00:01:00.00+08:00";
        String phenomTime5 = "2015-01-04T00:01:00.00+08:00";

        String param1Key = "raw";
        String param2Key = "DN";

        String ds1Obs1Param1Val = "12.12";
        String ds1Obs1Param2Val = "21.21";

        String ds1_OM_Measurement = "123.45";
        String ds1_OM_CountObservation = "543";
        String ds1_OM_TruthObservation = "true";
        String ds1_OM_Observation = "\"an arbitrary string\"";
        String ds1_OM_CategoryObservation = "\"http://some.uri/of/some/definition#section1.1\"";

        // Construct dataArray JSON
        String requestStr = "[" +
                // First Datastream
                "{" +
                "\"Datastream\": {\"@iot.id\": \"" + datastreamUuid + "\"}," +
                "\"components\": [\"phenomenonTime\", \"result\", \"parameters\"]," +
                "\"dataArray@iot.count\": 5," +
                "\"dataArray\":[" +
                "[\"" + phenomTime1 + "\"," + ds1_OM_Measurement + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]," +
                "[\"" + phenomTime2 + "\"," + ds1_OM_CountObservation + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]," +
                "[\"" + phenomTime3 + "\"," + ds1_OM_TruthObservation + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]," +
                "[\"" + phenomTime4 + "\"," + ds1_OM_Observation + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]," +
                "[\"" + phenomTime5 + "\"," + ds1_OM_CategoryObservation + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]" +
                "]" +
                "}" +
                "]";

        String createObservationsUrl = apiRootUrl + "CreateObservations";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addTokenToHeader(port, headers);
        // Read as a string because Spring's web client doesn't know how to read a JSON Array response
        HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
        ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<String>() {};
        ResponseEntity<String> response = rest.exchange(createObservationsUrl,
                HttpMethod.POST, entity,
                responseType);
        assertEquals(201, response.getStatusCodeValue());

        // Read response
        String s = response.getBody();
        JsonReader r = Json.createReader(new StringReader(s));
        JsonArray a = r.readArray();
        r.close();

        assertEquals(5, a.size());
        String creationResult = a.getString(0);
        assertEquals("error", creationResult);

        creationResult = a.getString(1);
        assertEquals("error", creationResult);

        creationResult = a.getString(2);
        assertNotEquals("error", creationResult);
        URI u = URI.create(creationResult);
        Matcher m = LOC_PATT.matcher(u.getPath());
        assertTrue(m.matches());

        creationResult = a.getString(3);
        assertEquals("error", creationResult);

        creationResult = a.getString(4);
        assertEquals("error", creationResult);
    }

    @Test
    public void testDataArrayCreateNoFeatureOfInterestOMObservation() throws Exception {
        // Create a Location
        String locationUrl = LocationResourceTest.createLocation(generateBaseUrl(port, LocationResourceTest.URL_PATH_BASE),
                port,
                rest,
                "CCIT",
                "Calgary Centre for Innovative Technologies",
                -92.041213, 30.218805);
        String locationUuid = Utility.extractUuidForEntityUrl(locationUrl);

        // Create a Thing with a location
        String thingUrl = null;
        {
            Map<String, String> properties = new LinkedHashMap<String, String>();
            properties.put("foo", "bar");
            properties.put("baz", "qux");
            thingUrl = ThingResourceTest.createThing(generateBaseUrl(port, ThingResourceTest.URL_PATH_BASE), port, rest,
                    "test thing 1", "A thing for testing.", properties,
                    locationUuid);
            assertNotNull(thingUrl);
        }
        String thingUuid = Utility.extractUuidForEntityUrl(thingUrl);

        // Create a Sensor
        String sensorUrl = SensorResourceTest.createSensor(
                generateBaseUrl(port, SensorResourceTest.URL_PATH_BASE),
                port,
                rest,
                "TMP36",
                "TMP36 - Analog Temperature sensor",
                "application/pdf",
                "http://example.org/TMP35_36_37.pdf");
        String sensorUuid = Utility.extractUuidForEntityUrl(sensorUrl);

        // Create a second Sensor
        String sensorUrl2 = SensorResourceTest.createSensor(
                generateBaseUrl(port, SensorResourceTest.URL_PATH_BASE),
                port,
                rest,
                "RH36",
                "RH36 - Analog Relative Humidity sensor",
                "application/pdf",
                "http://example.org/TMP35_36_37.pdf");
        String sensorUuid2 = Utility.extractUuidForEntityUrl(sensorUrl2);

        // Create an ObservedProperty
        String opUrl = ObservedPropertyResourceTest.createObservedProperty(
                generateBaseUrl(port, ObservedPropertyResourceTest.URL_PATH_BASE),
                port,
                rest,
                "Air Temperature",
                "http://dbpedia.org/page/Air_temp",
                ObservedPropertyResourceTest.description1);
        String observedPropertyUuid = Utility.extractUuidForEntityUrl(opUrl);

        // Create a second ObservedProperty
        String opUrl2 = ObservedPropertyResourceTest.createObservedProperty(
                generateBaseUrl(port, ObservedPropertyResourceTest.URL_PATH_BASE),
                port,
                rest,
                "Relative Humidity",
                "http://dbpedia.org/page/RH",
                ObservedPropertyResourceTest.description1);
        String observedPropertyUuid2 = Utility.extractUuidForEntityUrl(opUrl2);

        // Create a Datastream
        String dsUrl = DatastreamResourceTest.createDatastream(generateBaseUrl(port, DatastreamResourceTest.URL_PATH_BASE), port, rest,
                "oven temperature",
                "This is a datastream measuring the air temperature in an oven.",
                "degree Celsius",
                "C", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                "http://unitsofmeasure.org/ucum.html#para-30",
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Observation",
                thingUuid,
                sensorUuid,
                observedPropertyUuid);
        String datastreamUuid = Utility.extractUuidForEntityUrl(dsUrl);

        // Create a second Datastream
        String dsUrl2 = DatastreamResourceTest.createDatastream(generateBaseUrl(port, DatastreamResourceTest.URL_PATH_BASE), port, rest,
                "oven RH",
                "This is a datastream measuring the relative humidity in an oven.",
                "percentage",
                "%", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                "http://unitsofmeasure.org/ucum.html#para-30",
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                thingUuid,
                sensorUuid,
                observedPropertyUuid);
        String datastreamUuid2 = Utility.extractUuidForEntityUrl(dsUrl2);

        // Observations
        String phenomTime1 = "2014-12-31T00:01:00.00+08:00";
        String phenomTime2 = "2015-01-01T00:01:00.00+08:00";
        String phenomTime3 = "2015-01-02T00:01:00.00+08:00";
        String phenomTime4 = "2015-01-03T00:01:00.00+08:00";
        String phenomTime5 = "2015-01-04T00:01:00.00+08:00";

        String param1Key = "raw";
        String param2Key = "DN";

        String ds1Obs1Param1Val = "12.12";
        String ds1Obs1Param2Val = "21.21";

        String ds1_OM_Measurement = "123.45";
        String ds1_OM_CountObservation = "543";
        String ds1_OM_TruthObservation = "true";
        String ds1_OM_Observation = "\"an arbitrary string\"";
        String ds1_OM_CategoryObservation = "\"http://some.uri/of/some/definition#section1.1\"";

        // Construct dataArray JSON
        String requestStr = "[" +
                // First Datastream
                "{" +
                "\"Datastream\": {\"@iot.id\": \"" + datastreamUuid + "\"}," +
                "\"components\": [\"phenomenonTime\", \"result\", \"parameters\"]," +
                "\"dataArray@iot.count\": 5," +
                "\"dataArray\":[" +
                "[\"" + phenomTime1 + "\"," + ds1_OM_Measurement + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]," +
                "[\"" + phenomTime2 + "\"," + ds1_OM_CountObservation + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]," +
                "[\"" + phenomTime3 + "\"," + ds1_OM_TruthObservation + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]," +
                "[\"" + phenomTime4 + "\"," + ds1_OM_Observation + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]," +
                "[\"" + phenomTime5 + "\"," + ds1_OM_CategoryObservation + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]" +
                "]" +
                "}" +
                "]";

        String createObservationsUrl = apiRootUrl + "CreateObservations";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addTokenToHeader(port, headers);
        // Read as a string because Spring's web client doesn't know how to read a JSON Array response
        HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
        ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<String>() {};
        ResponseEntity<String> response = rest.exchange(createObservationsUrl,
                HttpMethod.POST, entity,
                responseType);
        assertEquals(201, response.getStatusCodeValue());

        // Read response
        String s = response.getBody();
        JsonReader r = Json.createReader(new StringReader(s));
        JsonArray a = r.readArray();
        r.close();

        assertEquals(5, a.size());
        String creationResult = a.getString(0);
        assertEquals("error", creationResult);

        creationResult = a.getString(1);
        assertEquals("error", creationResult);

        creationResult = a.getString(2);
        assertEquals("error", creationResult);

        creationResult = a.getString(3);
        assertNotEquals("error", creationResult);
        URI u = URI.create(creationResult);
        Matcher m = LOC_PATT.matcher(u.getPath());
        assertTrue(m.matches());

        creationResult = a.getString(4);
        assertNotEquals("error", creationResult);
        u = URI.create(creationResult);
        m = LOC_PATT.matcher(u.getPath());
        assertTrue(m.matches());
    }

    @Test
    public void testDataArrayCreateNoFeatureOfInterestOMCategoryObservation() throws Exception {
        // Create a Location
        String locationUrl = LocationResourceTest.createLocation(generateBaseUrl(port, LocationResourceTest.URL_PATH_BASE),
                port,
                rest,
                "CCIT",
                "Calgary Centre for Innovative Technologies",
                -92.041213, 30.218805);
        String locationUuid = Utility.extractUuidForEntityUrl(locationUrl);

        // Create a Thing with a location
        String thingUrl = null;
        {
            Map<String, String> properties = new LinkedHashMap<String, String>();
            properties.put("foo", "bar");
            properties.put("baz", "qux");
            thingUrl = ThingResourceTest.createThing(generateBaseUrl(port, ThingResourceTest.URL_PATH_BASE), port, rest,
                    "test thing 1", "A thing for testing.", properties,
                    locationUuid);
            assertNotNull(thingUrl);
        }
        String thingUuid = Utility.extractUuidForEntityUrl(thingUrl);

        // Create a Sensor
        String sensorUrl = SensorResourceTest.createSensor(
                generateBaseUrl(port, SensorResourceTest.URL_PATH_BASE),
                port,
                rest,
                "TMP36",
                "TMP36 - Analog Temperature sensor",
                "application/pdf",
                "http://example.org/TMP35_36_37.pdf");
        String sensorUuid = Utility.extractUuidForEntityUrl(sensorUrl);

        // Create a second Sensor
        String sensorUrl2 = SensorResourceTest.createSensor(
                generateBaseUrl(port, SensorResourceTest.URL_PATH_BASE),
                port,
                rest,
                "RH36",
                "RH36 - Analog Relative Humidity sensor",
                "application/pdf",
                "http://example.org/TMP35_36_37.pdf");
        String sensorUuid2 = Utility.extractUuidForEntityUrl(sensorUrl2);

        // Create an ObservedProperty
        String opUrl = ObservedPropertyResourceTest.createObservedProperty(
                generateBaseUrl(port, ObservedPropertyResourceTest.URL_PATH_BASE),
                port,
                rest,
                "Air Temperature",
                "http://dbpedia.org/page/Air_temp",
                ObservedPropertyResourceTest.description1);
        String observedPropertyUuid = Utility.extractUuidForEntityUrl(opUrl);

        // Create a second ObservedProperty
        String opUrl2 = ObservedPropertyResourceTest.createObservedProperty(
                generateBaseUrl(port, ObservedPropertyResourceTest.URL_PATH_BASE),
                port,
                rest,
                "Relative Humidity",
                "http://dbpedia.org/page/RH",
                ObservedPropertyResourceTest.description1);
        String observedPropertyUuid2 = Utility.extractUuidForEntityUrl(opUrl2);

        // Create a Datastream
        String dsUrl = DatastreamResourceTest.createDatastream(generateBaseUrl(port, DatastreamResourceTest.URL_PATH_BASE), port, rest,
                "oven temperature",
                "This is a datastream measuring the air temperature in an oven.",
                "degree Celsius",
                "C", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                "http://unitsofmeasure.org/ucum.html#para-30",
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_CategoryObservation",
                thingUuid,
                sensorUuid,
                observedPropertyUuid);
        String datastreamUuid = Utility.extractUuidForEntityUrl(dsUrl);

        // Create a second Datastream
        String dsUrl2 = DatastreamResourceTest.createDatastream(generateBaseUrl(port, DatastreamResourceTest.URL_PATH_BASE), port, rest,
                "oven RH",
                "This is a datastream measuring the relative humidity in an oven.",
                "percentage",
                "%", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                "http://unitsofmeasure.org/ucum.html#para-30",
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                thingUuid,
                sensorUuid,
                observedPropertyUuid);
        String datastreamUuid2 = Utility.extractUuidForEntityUrl(dsUrl2);

        // Observations
        String phenomTime1 = "2014-12-31T00:01:00.00+08:00";
        String phenomTime2 = "2015-01-01T00:01:00.00+08:00";
        String phenomTime3 = "2015-01-02T00:01:00.00+08:00";
        String phenomTime4 = "2015-01-03T00:01:00.00+08:00";
        String phenomTime5 = "2015-01-04T00:01:00.00+08:00";

        String param1Key = "raw";
        String param2Key = "DN";

        String ds1Obs1Param1Val = "12.12";
        String ds1Obs1Param2Val = "21.21";

        String ds1_OM_Measurement = "123.45";
        String ds1_OM_CountObservation = "543";
        String ds1_OM_TruthObservation = "true";
        String ds1_OM_Observation = "\"an arbitrary string\"";
        String ds1_OM_CategoryObservation = "\"http://some.uri/of/some/definition#section1.1\"";

        // Construct dataArray JSON
        String requestStr = "[" +
                // First Datastream
                "{" +
                "\"Datastream\": {\"@iot.id\": \"" + datastreamUuid + "\"}," +
                "\"components\": [\"phenomenonTime\", \"result\", \"parameters\"]," +
                "\"dataArray@iot.count\": 5," +
                "\"dataArray\":[" +
                "[\"" + phenomTime1 + "\"," + ds1_OM_Measurement + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]," +
                "[\"" + phenomTime2 + "\"," + ds1_OM_CountObservation + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]," +
                "[\"" + phenomTime3 + "\"," + ds1_OM_TruthObservation + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]," +
                "[\"" + phenomTime4 + "\"," + ds1_OM_Observation + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]," +
                "[\"" + phenomTime5 + "\"," + ds1_OM_CategoryObservation + "," +
                "{" +
                "\"" + param1Key + "\":\"" + ds1Obs1Param1Val + "\"," +
                "\"" + param2Key + "\":\"" + ds1Obs1Param2Val + "\"" +
                "}" +
                "]" +
                "]" +
                "}" +
                "]";

        String createObservationsUrl = apiRootUrl + "CreateObservations";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addTokenToHeader(port, headers);
        // Read as a string because Spring's web client doesn't know how to read a JSON Array response
        HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
        ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<String>() {};
        ResponseEntity<String> response = rest.exchange(createObservationsUrl,
                HttpMethod.POST, entity,
                responseType);
        assertEquals(201, response.getStatusCodeValue());

        // Read response
        String s = response.getBody();
        JsonReader r = Json.createReader(new StringReader(s));
        JsonArray a = r.readArray();
        r.close();

        assertEquals(5, a.size());
        String creationResult = a.getString(0);
        assertEquals("error", creationResult);

        creationResult = a.getString(1);
        assertEquals("error", creationResult);

        creationResult = a.getString(2);
        assertEquals("error", creationResult);

        creationResult = a.getString(3);
        assertEquals("error", creationResult);

        creationResult = a.getString(4);
        assertNotEquals("error", creationResult);
        URI u = URI.create(creationResult);
        Matcher m = LOC_PATT.matcher(u.getPath());
        assertTrue(m.matches());
    }
}
