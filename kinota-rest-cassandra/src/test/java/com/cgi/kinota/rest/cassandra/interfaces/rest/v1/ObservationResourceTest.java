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
import com.cgi.kinota.commons.domain.Location;
import com.cgi.kinota.commons.domain.FeatureOfInterest;
import com.cgi.kinota.commons.domain.Observation;

import com.cgi.kinota.rest.cassandra.Application;
import com.cgi.kinota.rest.cassandra.CassandraObservationTestBase;
import com.cgi.kinota.persistence.cassandra.config.SpringDataCassandraConfig;

import com.cgi.kinota.rest.cassandra.interfaces.rest.Utility;

import org.apache.commons.lang3.tuple.Pair;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.*;

import static com.cgi.kinota.commons.Constants.*;
import static com.cgi.kinota.commons.geo.Utility.boundingBoxPolygonToEnvelope;
import static com.cgi.kinota.commons.geo.Utility.createGeoJsonPoint;
import static com.cgi.kinota.commons.geo.Utility.readGeoJsonPolygon;
import static org.junit.Assert.*;

/**
 * Created by bmiles on 3/21/17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = { Application.class, SpringDataCassandraConfig.class })
public class ObservationResourceTest extends CassandraObservationTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ObservationResourceTest.class);

    public static String createObservation(String baseUrl,
                                           int port,
                                           RestTemplate rest,
                                           String featureOfInterestUuid,
                                           String datastreamUuid,
                                           String phenomenonTime, String phenomenonTimeEnd,
                                           String resultTime,
                                           String validTimeBegin, String validTimeEnd,
                                           String result,
                                           Map<String, String> parameters) {
        // Build request entity
        String parametersStr = null;
        if (parameters.size() > 0) {
            StringBuffer parametersStrBuffer = new StringBuffer();
            Iterator<Map.Entry<String, String>> i = parameters.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<String, String> e = i.next();
                parametersStrBuffer.append("{\"" + e.getKey() + "\":\"" + e.getValue() + "\"}");
                if (i.hasNext()) {
                    parametersStrBuffer.append(",");
                }
            }
            parametersStr = parametersStrBuffer.toString();
        }

        String phenomenonTimeStr = phenomenonTime;
        if (phenomenonTimeEnd != null) {
            phenomenonTimeStr = phenomenonTime + "/" + phenomenonTimeEnd;
        }

        String requestStr = "{" +
                "\"Datastream\":{\"@iot.id\":\"" + datastreamUuid + "\"},";
        if (featureOfInterestUuid != null) {
            requestStr += "\"FeatureOfInterest\":{\"@iot.id\":\"" + featureOfInterestUuid + "\"},";
        }
        if (phenomenonTimeStr != null) {
            requestStr += "\"phenomenonTime\":\"" + phenomenonTimeStr + "\",";
        }
        if (resultTime != null) {
            requestStr += "\"resultTime\":\"" + resultTime + "\",";
        }
        if (validTimeBegin != null && validTimeEnd != null) {
            String validTimeStr = validTimeBegin + "/" + validTimeEnd;
            requestStr += "\"validTime\":\"" + validTimeStr + "\",";
        }
        if (parametersStr != null) {
            requestStr += "\"parameters\":" + "[" + parametersStr + "],";
        }
        // If Datastream does not have a observationType of OM_Measurement (double)
        //   or OM_CountObservation (integer) or OM_TruthObservation (boolean), then
        //   result must include beginning and ending quotes.
        requestStr += "\"result\":" + result;

        requestStr += "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addTokenToHeader(port, headers);
        HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
        ParameterizedTypeReference<JsonObject> responseType = new ParameterizedTypeReference<JsonObject>() {};
        ResponseEntity<JsonObject> response = rest.exchange(baseUrl,
                HttpMethod.POST, entity,
                responseType);
        assertEquals(201, response.getStatusCodeValue());
        JsonObject o = response.getBody();

        // Check that the location response parameter is the self link of the created Observation
        // (e.g. "http://localhost:55704/v1.0/Observations(b23e2d40-d381-11e6-ba14-6733c7a00eb7)")
        Pair<Boolean, String> matchesLocation = Utility.doesLocationPathMatch(response, LOC_PATT);
        assertTrue(matchesLocation.getLeft());
        String location =  matchesLocation.getRight();

        // Check that selfLink of response matches
        try {
            String selfLink = o.getString(ANNO_IOT_SELF_LINK);
            assertEquals(location, selfLink);
        } catch (NullPointerException npe) {}

        return location;
    }

    public static void createObservationFailure(String baseUrl,
                                                int port,
                                                RestTemplate rest,
                                                String featureOfInterestUuid,
                                                String datastreamUuid,
                                                String phenomenonTime, String phenomenonTimeEnd,
                                                String resultTime,
                                                String validTimeBegin, String validTimeEnd,
                                                String result,
                                                Map<String, String> parameters) {
        // Build request entity
        String parametersStr = null;
        if (parameters.size() > 0) {
            StringBuffer parametersStrBuffer = new StringBuffer();
            Iterator<Map.Entry<String, String>> i = parameters.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<String, String> e = i.next();
                parametersStrBuffer.append("{\"" + e.getKey() + "\":\"" + e.getValue() + "\"}");
                if (i.hasNext()) {
                    parametersStrBuffer.append(",");
                }
            }
            parametersStr = parametersStrBuffer.toString();
        }

        String phenomenonTimeStr = phenomenonTime;
        if (phenomenonTimeEnd != null) {
            phenomenonTimeStr = phenomenonTime + "/" + phenomenonTimeEnd;
        }

        String requestStr = "{" +
                "\"Datastream\":{\"@iot.id\":\"" + datastreamUuid + "\"},";
        if (featureOfInterestUuid != null) {
            requestStr += "\"FeatureOfInterest\":{\"@iot.id\":\"" + featureOfInterestUuid + "\"},";
        }
        if (phenomenonTimeStr != null) {
            requestStr += "\"phenomenonTime\":\"" + phenomenonTimeStr + "\",";
        }
        if (resultTime != null) {
            requestStr += "\"resultTime\":\"" + resultTime + "\",";
        }
        if (validTimeBegin != null && validTimeEnd != null) {
            String validTimeStr = validTimeBegin + "/" + validTimeEnd;
            requestStr += "\"validTime\":\"" + validTimeStr + "\",";
        }
        if (parametersStr != null) {
            requestStr += "\"parameters\":" + "[" + parametersStr + "],";
        }
        // If Datastream does not have a observationType of OM_Measurement (double)
        //   or OM_CountObservation (integer) or OM_TruthObservation (boolean), then
        //   result must include beginning and ending quotes.
        requestStr += "\"result\":" + result;

        requestStr += "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addTokenToHeader(port, headers);
        HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
        ParameterizedTypeReference<JsonObject> responseType = new ParameterizedTypeReference<JsonObject>() {};
        boolean exceptionThrown = false;
        try {
            ResponseEntity<JsonObject> response = rest.exchange(baseUrl,
                    HttpMethod.POST, entity,
                    responseType);
        } catch(HttpServerErrorException e) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);

    }

    @Test
    public void testCreateReadObservation() throws Exception {
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
                "-92.041213",
                "30.218805");
        String featureOfInterestUuid = Utility.extractUuidForEntityUrl(foiUrl);

        // Create first Observation
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("foo", "bar");
        parameters.put("baz", "qux");
        String location1 = createObservation(baseUrl, port, rest,
                featureOfInterestUuid,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "42.23",
                parameters);

        // Ensure that temporal and spatial summaries of datastream were updated
        JsonObject dsObj = DatastreamResourceTest.readDatastream(dsUrl, port, rest);
        assertEquals("2014-12-25T03:59:59.000Z/2014-12-25T03:59:59.000Z", dsObj.getString("phenomenonTime"));
        boolean threwException = false;
        try {
            dsObj.getString("resultTime");
        } catch (NullPointerException npe) {
            threwException = true;
        }
        assertTrue(threwException);
        threwException = false;
        JsonObject obsArea = dsObj.getJsonObject("observedArea");
        assertNotNull(obsArea);

        // Create second Observation
        parameters = new LinkedHashMap<String, String>();
        parameters.put("one", "1");
        parameters.put("two", "2");
        String location2 = createObservation(baseUrl, port, rest,
                featureOfInterestUuid,
                datastreamUuid,
                "2015-01-03T00:01:00.00+08:00", null,
                null,
                null, null,
                "23.42",
                parameters);

        // Ensure that temporal and spatial summaries of datastream were updated
        dsObj = DatastreamResourceTest.readDatastream(dsUrl, port, rest);
        assertEquals("2014-12-25T03:59:59.000Z/2015-01-02T16:01:00.000Z", dsObj.getString("phenomenonTime"));
        try {
            dsObj.getString("resultTime");
        } catch (NullPointerException npe) {
            threwException = true;
        }
        assertTrue(threwException);
        threwException = false;

        // Read newly created Observations
        // First
        ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {};
        ResponseEntity<String> responseStr = rest.exchange(location1,
                HttpMethod.GET, getAuthorizedEntity(port, Observation.class), responseTypeStr);
        String s = responseStr.getBody();
        JsonReader r = Json.createReader(new StringReader(s));
        JsonObject obj = r.readObject();
        assertEquals(42.23, obj.getJsonNumber("result").doubleValue(), 0.001);
        assertEquals("2014-12-25T03:59:59.000Z", obj.getString("phenomenonTime"));
        JsonArray params = obj.getJsonArray("parameters");
        JsonObject param = params.getJsonObject(0);
        assertEquals("qux", param.getString("baz"));
        param = params.getJsonObject(1);
        assertEquals("bar", param.getString("foo"));

        // Read second Observation
        responseStr = rest.exchange(location2,
                HttpMethod.GET, getAuthorizedEntity(port, Observation.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        assertEquals(23.42, obj.getJsonNumber("result").doubleValue(), 0.001);
        assertEquals("2015-01-02T16:01:00.000Z", obj.getString("phenomenonTime"));
        params = obj.getJsonArray("parameters");
        param = params.getJsonObject(0);
        assertEquals("1", param.getString("one"));
        param = params.getJsonObject(1);
        assertEquals("2", param.getString("two"));

        // Fetch all Observations
        String allObservationsUrl = apiRootUrl + "Observations";
        responseStr = rest.exchange(allObservationsUrl,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        assertEquals(2, obj.getInt(ANNO_COLLECTION_COUNT));

        // Read observations from FeatureOfInterest
        String obsRelUrl = foiUrl + "/" + Observation.NAME_PLURAL;
        responseStr = rest.exchange(obsRelUrl,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        JsonArray a = obj.getJsonArray("value");
        assertTrue(a.size() == 2);
        JsonObject obs = a.getJsonObject(0);
        assertEquals(location2, obs.getString(ANNO_IOT_SELF_LINK));
        assertEquals(23.42, obs.getJsonNumber("result").doubleValue(), 0.001);
        assertEquals("2015-01-02T16:01:00.000Z", obs.getString("phenomenonTime"));
        params = obs.getJsonArray("parameters");
        param = params.getJsonObject(0);
        assertEquals("1", param.getString("one"));
        param = params.getJsonObject(1);
        assertEquals("2", param.getString("two"));

        obs = a.getJsonObject(1);
        assertEquals(location1, obs.getString(ANNO_IOT_SELF_LINK));
        assertEquals(42.23, obs.getJsonNumber("result").doubleValue(), 0.001);
        assertEquals("2014-12-25T03:59:59.000Z", obs.getString("phenomenonTime"));
        params = obs.getJsonArray("parameters");
        param = params.getJsonObject(0);
        assertEquals("qux", param.getString("baz"));
        param = params.getJsonObject(1);
        assertEquals("bar", param.getString("foo"));

        // Read observations from Datastream
        String obsRelDatastreamUrl = dsUrl + "/" + Observation.NAME_PLURAL;
        responseStr = rest.exchange(obsRelDatastreamUrl,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        a = obj.getJsonArray("value");
        assertTrue(a.size() == 2);
        obs = a.getJsonObject(0);
        assertEquals(location2, obs.getString(ANNO_IOT_SELF_LINK));
        assertEquals(23.42, obs.getJsonNumber("result").doubleValue(), 0.001);
        assertEquals("2015-01-02T16:01:00.000Z", obs.getString("phenomenonTime"));
        params = obs.getJsonArray("parameters");
        param = params.getJsonObject(0);
        assertEquals("1", param.getString("one"));
        param = params.getJsonObject(1);
        assertEquals("2", param.getString("two"));
        String obs2Uuid = obs.getString(ANNO_IOT_ID);

        obs = a.getJsonObject(1);
        assertEquals(location1, obs.getString(ANNO_IOT_SELF_LINK));
        assertEquals(42.23, obs.getJsonNumber("result").doubleValue(), 0.001);
        assertEquals("2014-12-25T03:59:59.000Z", obs.getString("phenomenonTime"));
        params = obs.getJsonArray("parameters");
        param = params.getJsonObject(0);
        assertEquals("qux", param.getString("baz"));
        param = params.getJsonObject(1);
        assertEquals("bar", param.getString("foo"));
        String obs1Uuid = obs.getString(ANNO_IOT_ID);

        // Read observations $ref from Datastream
        String obsRefRelDatastreamUrl = dsUrl + "/" + Observation.NAME_PLURAL + "/$ref";
        responseStr = rest.exchange(obsRefRelDatastreamUrl,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        a = obj.getJsonArray("value");
        assertTrue(a.size() == 2);
        obs = a.getJsonObject(0);
        assertEquals(location2, obs.getString(ANNO_IOT_SELF_LINKS));
        obs = a.getJsonObject(1);
        assertEquals(location1, obs.getString(ANNO_IOT_SELF_LINKS));

        // Read FeatureOfInterest and Datastream from Observation's navigationLinks
        responseStr = rest.exchange(location1,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        String foiListRel = obj.getString(FeatureOfInterest.NAV_LINK);
        assertNotNull(foiListRel);
        String foiUrlFromNavlink = apiRootUrl + foiListRel;
        String dsListRel = obj.getString(Datastream.NAV_LINK);
        assertNotNull(dsListRel);
        String dsUrlFromNavlink = apiRootUrl + dsListRel;

        // FeatureOfInterest
        responseStr = rest.exchange(foiUrlFromNavlink,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        a = obj.getJsonArray(COLLECTION_ATTR);
        assertTrue(a.size() == 1);
        JsonObject l = a.getJsonObject(0);
        String foiUrlFromNavigatedTo = l.getString(ANNO_IOT_SELF_LINK);
        assertEquals(foiUrl, foiUrlFromNavigatedTo);
        // FeatureOfInterest$ref
        String foiUrlFromNavlinkRef = foiUrlFromNavlink + "/$ref";
        responseStr = rest.exchange(foiUrlFromNavlinkRef,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        a = obj.getJsonArray(COLLECTION_ATTR);
        assertTrue(a.size() == 1);
        l = a.getJsonObject(0);
        String foiUrlFromNavigatedTo2 = l.getString(ANNO_IOT_SELF_LINKS);
        assertEquals(foiUrl, foiUrlFromNavigatedTo2);

        // Datastream
        responseStr = rest.exchange(dsUrlFromNavlink,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        a = obj.getJsonArray(COLLECTION_ATTR);
        assertTrue(a.size() == 1);
        l = a.getJsonObject(0);
        String dsUrlFromNavigatedTo = l.getString(ANNO_IOT_SELF_LINK);
        assertEquals(dsUrl, dsUrlFromNavigatedTo);
        // Datastream/$ref
        String dsUrlFromNavlinkRef = dsUrlFromNavlink + "/$ref";
        responseStr = rest.exchange(dsUrlFromNavlinkRef,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        a = obj.getJsonArray(COLLECTION_ATTR);
        assertTrue(a.size() == 1);
        l = a.getJsonObject(0);
        String dsUrlFromNavigatedTo2 = l.getString(ANNO_IOT_SELF_LINKS);
        assertEquals(dsUrl, dsUrlFromNavigatedTo2);

    }

    @Test
    public void testCreateReadObservationPaging() throws Exception {
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
                "-92.041213",
                "30.218805");
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

        // Fetch all Observations (should be 4 across 2 pages)
        String allObservationsUrl = apiRootUrl + "Observations";
        ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {};
        ResponseEntity<String> responseStr = rest.exchange(allObservationsUrl,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        String s = responseStr.getBody();
        JsonReader r = Json.createReader(new StringReader(s));
        JsonObject obj = r.readObject();
        assertEquals(4, obj.getInt(ANNO_COLLECTION_COUNT));

        JsonArray a = obj.getJsonArray(COLLECTION_ATTR);
        assertEquals(Integer.valueOf(3), Integer.valueOf(a.size()));

        String nextLink = obj.getString(ANNO_IOT_NEXT_LINK);
        assertNotNull(nextLink);

        // Fetch nextLink
        responseStr = rest.exchange(nextLink,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        assertEquals(4, obj.getInt(ANNO_COLLECTION_COUNT));
        a = obj.getJsonArray(COLLECTION_ATTR);
        assertEquals(Integer.valueOf(1), Integer.valueOf(a.size()));

        boolean exceptionThrown = false;
        try {
            nextLink = obj.getString(ANNO_IOT_NEXT_LINK);
        } catch (NullPointerException npe) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);

        // Read observations from Datastream (should be 4 observations across 2 pages)
        String obsRelDatastreamUrl = dsUrl + "/" + Observation.NAME_PLURAL;
        responseStr = rest.exchange(obsRelDatastreamUrl,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        a = obj.getJsonArray("value");
        assertEquals(Integer.valueOf(3), Integer.valueOf(a.size()));

        nextLink = obj.getString(ANNO_IOT_NEXT_LINK);
        assertNotNull(nextLink);

        // Fetch nextLink
        responseStr = rest.exchange(nextLink,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        a = obj.getJsonArray("value");
        assertEquals(Integer.valueOf(1), Integer.valueOf(a.size()));

        exceptionThrown = false;
        try {
            nextLink = obj.getString(ANNO_IOT_NEXT_LINK);
        } catch (NullPointerException npe) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);

        // Read observations $ref from Datastream (should be 4 observations across 2 pages)
        String obsRefRelDatastreamUrl = dsUrl + "/" + Observation.NAME_PLURAL + "/$ref";
        responseStr = rest.exchange(obsRefRelDatastreamUrl,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        String s3 = responseStr.getBody();
        r = Json.createReader(new StringReader(s3));
        obj = r.readObject();
        a = obj.getJsonArray("value");
        assertEquals(Integer.valueOf(3), Integer.valueOf(a.size()));

        nextLink = obj.getString(ANNO_IOT_NEXT_LINK);
        assertNotNull(nextLink);

        // Fetch nextLink
        responseStr = rest.exchange(nextLink,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        a = obj.getJsonArray("value");
        assertEquals(Integer.valueOf(1), Integer.valueOf(a.size()));

        exceptionThrown = false;
        try {
            nextLink = obj.getString(ANNO_IOT_NEXT_LINK);
        } catch (NullPointerException npe) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
        exceptionThrown = false;

        // Read observations from FeatureOfInterest (should be 4 observations across 2 pages)
        String obsRelFeatureOfInterestUrl = foiUrl + "/" + Observation.NAME_PLURAL;
        responseStr = rest.exchange(obsRelFeatureOfInterestUrl,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        a = obj.getJsonArray("value");
        assertEquals(Integer.valueOf(3), Integer.valueOf(a.size()));

        nextLink = obj.getString(ANNO_IOT_NEXT_LINK);
        assertNotNull(nextLink);

        // Fetch nextLink
        responseStr = rest.exchange(nextLink,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        a = obj.getJsonArray("value");
        assertEquals(Integer.valueOf(1), Integer.valueOf(a.size()));

        exceptionThrown = false;
        try {
            nextLink = obj.getString(ANNO_IOT_NEXT_LINK);
        } catch (NullPointerException npe) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);

        // Read observations $ref from FeatureOfInterest (should be 4 observations across 2 pages)
        String obsRefFeatureOfInterestUrl = foiUrl + "/" + Observation.NAME_PLURAL + "/$ref";
        responseStr = rest.exchange(obsRefFeatureOfInterestUrl,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        String s4 = responseStr.getBody();
        r = Json.createReader(new StringReader(s4));
        obj = r.readObject();
        a = obj.getJsonArray("value");
        assertEquals(Integer.valueOf(3), Integer.valueOf(a.size()));

        nextLink = obj.getString(ANNO_IOT_NEXT_LINK);
        assertNotNull(nextLink);

        // Fetch nextLink
        responseStr = rest.exchange(nextLink,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        s4 = responseStr.getBody();
        r = Json.createReader(new StringReader(s4));
        obj = r.readObject();
        a = obj.getJsonArray("value");
        assertEquals(Integer.valueOf(1), Integer.valueOf(a.size()));

        exceptionThrown = false;
        try {
            nextLink = obj.getString(ANNO_IOT_NEXT_LINK);
        } catch (NullPointerException npe) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
        exceptionThrown = false;
    }

    @Test
    public void testCreateReadObservationNoPhenomenonTime() throws Exception {
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
        String location1 = createObservation(baseUrl, port, rest,
                featureOfInterestUuid,
                datastreamUuid,
                null, null,
                null,
                null, null,
                "42.23",
                parameters);

        // Ensure that temporal and spatial summaries of datastream were updated
        JsonObject dsObj = DatastreamResourceTest.readDatastream(dsUrl, port, rest);
        assertNotNull(dsObj.getString("phenomenonTime"));
        boolean threwException = false;
        try {
            dsObj.getString("resultTime");
        } catch (NullPointerException npe) {
            threwException = true;
        }
        assertTrue(threwException);
        threwException = false;
        JsonObject obsArea = dsObj.getJsonObject("observedArea");
        assertNotNull(obsArea);

        // Create second Observation
        parameters = new LinkedHashMap<String, String>();
        parameters.put("one", "1");
        parameters.put("two", "2");
        String location2 = createObservation(baseUrl, port, rest,
                featureOfInterestUuid,
                datastreamUuid,
                null, null,
                null,
                null, null,
                "23.42",
                parameters);

        // Ensure that temporal and spatial summaries of datastream were updated
        dsObj = DatastreamResourceTest.readDatastream(dsUrl, port, rest);
        assertNotNull(dsObj.getString("phenomenonTime"));
        try {
            dsObj.getString("resultTime");
        } catch (NullPointerException npe) {
            threwException = true;
        }
        assertTrue(threwException);
        threwException = false;

        // Read newly created Observations
        // First
        ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {
        };
        ResponseEntity<String> responseStr = rest.exchange(location1,
                HttpMethod.GET, getAuthorizedEntity(port, Observation.class), responseTypeStr);
        String s = responseStr.getBody();
        JsonReader r = Json.createReader(new StringReader(s));
        JsonObject obj = r.readObject();
        assertEquals(42.23, obj.getJsonNumber("result").doubleValue(), 0.001);
        assertNotNull(obj.getString("phenomenonTime"));
        JsonArray params = obj.getJsonArray("parameters");
        JsonObject param = params.getJsonObject(0);
        assertEquals("qux", param.getString("baz"));
        param = params.getJsonObject(1);
        assertEquals("bar", param.getString("foo"));

        // Read second Observation
        responseStr = rest.exchange(location2,
                HttpMethod.GET, getAuthorizedEntity(port, Observation.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        assertEquals(23.42, obj.getJsonNumber("result").doubleValue(), 0.001);
        assertNotNull(obj.getString("phenomenonTime"));
        params = obj.getJsonArray("parameters");
        param = params.getJsonObject(0);
        assertEquals("1", param.getString("one"));
        param = params.getJsonObject(1);
        assertEquals("2", param.getString("two"));
    }

    @Test
    public void testCreateReadObservationNoFeatureOfInterest() throws Exception {
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

        // Create first Observation
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("foo", "bar");
        parameters.put("baz", "qux");
        String location1 = createObservation(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "42.23",
                parameters);

        // Ensure that temporal and spatial summaries of datastream were updated
        JsonObject dsObj = DatastreamResourceTest.readDatastream(dsUrl, port, rest);
        assertEquals("2014-12-25T03:59:59.000Z/2014-12-25T03:59:59.000Z", dsObj.getString("phenomenonTime"));
        boolean threwException = false;
        try {
            dsObj.getString("resultTime");
        } catch (NullPointerException npe) {
            threwException = true;
        }
        assertTrue(threwException);
        threwException = false;
        JsonObject obsArea = dsObj.getJsonObject("observedArea");
        assertNotNull(obsArea);

        // Create second Observation
        parameters = new LinkedHashMap<String, String>();
        parameters.put("one", "1");
        parameters.put("two", "2");
        String location2 = createObservation(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2015-01-03T00:01:00.00+08:00", null,
                null,
                null, null,
                "23.42",
                parameters);

        // Ensure that temporal and spatial summaries of datastream were updated
        dsObj = DatastreamResourceTest.readDatastream(dsUrl, port, rest);
        assertEquals("2014-12-25T03:59:59.000Z/2015-01-02T16:01:00.000Z", dsObj.getString("phenomenonTime"));
        try {
            dsObj.getString("resultTime");
        } catch (NullPointerException npe) {
            threwException = true;
        }
        assertTrue(threwException);
        threwException = false;

        // Read newly created Observations
        // First
        ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {
        };
        ResponseEntity<String> responseStr = rest.exchange(location1,
                HttpMethod.GET, getAuthorizedEntity(port, Observation.class), responseTypeStr);
        String s = responseStr.getBody();
        JsonReader r = Json.createReader(new StringReader(s));
        JsonObject obj = r.readObject();
        assertEquals(42.23, obj.getJsonNumber("result").doubleValue(), 0.001);
        assertEquals("2014-12-25T03:59:59.000Z", obj.getString("phenomenonTime"));
        JsonArray params = obj.getJsonArray("parameters");
        JsonObject param = params.getJsonObject(0);
        assertEquals("qux", param.getString("baz"));
        param = params.getJsonObject(1);
        assertEquals("bar", param.getString("foo"));

        // Get FeatureOfInterest and compare to Thing's location
        String foiUrl1 = apiRootUrl + obj.getString(FeatureOfInterest.NAV_LINK);
        Location l = LocationResourceTest.readLocation(locationUrl, port, rest);
        FeatureOfInterest foi = FeatureOfInterestResourceTest.readRelatedFeatureOfInterest(foiUrl1,
                port, rest);
        assertEquals(l.getLocation(), foi.getLocation());

        // Read second Observation
        responseStr = rest.exchange(location2,
                HttpMethod.GET, getAuthorizedEntity(port, Observation.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        assertEquals(23.42, obj.getJsonNumber("result").doubleValue(), 0.001);
        assertEquals("2015-01-02T16:01:00.000Z", obj.getString("phenomenonTime"));
        params = obj.getJsonArray("parameters");
        param = params.getJsonObject(0);
        assertEquals("1", param.getString("one"));
        param = params.getJsonObject(1);
        assertEquals("2", param.getString("two"));

        // Get FeatureOfInterest and compare to Thing's location
        String foiUrl2 = apiRootUrl + obj.getString(FeatureOfInterest.NAV_LINK);
        foi = FeatureOfInterestResourceTest.readRelatedFeatureOfInterest(foiUrl2,
                port, rest);
        assertEquals(l.getLocation(), foi.getLocation());
    }

    @Test
    public void testCreateReadObservationNoFeatureOfInterestMobileSensors() throws Exception {
        // Initial location
        org.geojson.Point p = createGeoJsonPoint(-92.041213, 30.218805);
        // Second location
        org.geojson.Point p2 = createGeoJsonPoint(-92.046929, 30.227787);
        // Create a Location
        String locationUrl = LocationResourceTest.createLocation(generateBaseUrl(port, LocationResourceTest.URL_PATH_BASE),
                port,
                rest,
                "CCIT",
                "Calgary Centre for Innovative Technologies",
                p.getCoordinates().getLongitude(), p.getCoordinates().getLatitude());
        String locationUuid = Utility.extractUuidForEntityUrl(locationUrl);

        // Create a second Location
        String locationUrl2 = LocationResourceTest.createLocation(generateBaseUrl(port, LocationResourceTest.URL_PATH_BASE),
                port,
                rest,
                "CCIT2",
                "Calgary Centre for Innovative Technologies, 2",
                p2.getCoordinates().getLongitude(), p2.getCoordinates().getLatitude());
        String locationUuid2 = Utility.extractUuidForEntityUrl(locationUrl2);

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

        // Create first Observation
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("foo", "bar");
        parameters.put("baz", "qux");
        String location1 = createObservation(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "42.23",
                parameters);

        // Ensure that temporal and spatial summaries of datastream were updated
        JsonObject dsObj = DatastreamResourceTest.readDatastream(dsUrl, port, rest);
        assertEquals("2014-12-25T03:59:59.000Z/2014-12-25T03:59:59.000Z", dsObj.getString("phenomenonTime"));
        boolean threwException = false;
        try {
            dsObj.getString("resultTime");
        } catch (NullPointerException npe) {
            threwException = true;
        }
        assertTrue(threwException);
        threwException = false;
        JsonObject obsArea = dsObj.getJsonObject("observedArea");
        assertNotNull(obsArea);
        org.geojson.Polygon observedArea = readGeoJsonPolygon(obsArea.toString());
        ReferencedEnvelope e = boundingBoxPolygonToEnvelope(observedArea);
        assertTrue(e.covers(p.getCoordinates().getLongitude(),
                p.getCoordinates().getLatitude()));
        assertFalse(e.covers(p2.getCoordinates().getLongitude(),
                p2.getCoordinates().getLatitude()));

        // Create second Observation
        parameters = new LinkedHashMap<String, String>();
        parameters.put("one", "1");
        parameters.put("two", "2");
        String location2 = createObservation(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2015-01-03T00:01:00.00+08:00", null,
                null,
                null, null,
                "23.42",
                parameters);

        // Ensure that temporal summary of datastream were updated
        dsObj = DatastreamResourceTest.readDatastream(dsUrl, port, rest);
        assertEquals("2014-12-25T03:59:59.000Z/2015-01-02T16:01:00.000Z", dsObj.getString("phenomenonTime"));
        try {
            dsObj.getString("resultTime");
        } catch (NullPointerException npe) {
            threwException = true;
        }
        assertTrue(threwException);
        threwException = false;

        // Update Thing with new Location
        ThingResourceTest.updateThingLocation(thingUrl, locationUuid2, port, rest);

        // Create another Observation
        parameters = new LinkedHashMap<String, String>();
        parameters.put("four", "4");
        parameters.put("five", "5");
        String location3 = createObservation(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2015-01-04T00:03:02.00+08:00", null,
                null,
                null, null,
                "66.87",
                parameters);

        // Ensure that spatial summaries of datastream were updated
        dsObj = DatastreamResourceTest.readDatastream(dsUrl, port, rest);
        obsArea = dsObj.getJsonObject("observedArea");
        assertNotNull(obsArea);
        observedArea = readGeoJsonPolygon(obsArea.toString());
        e = boundingBoxPolygonToEnvelope(observedArea);
        assertTrue(e.covers(p.getCoordinates().getLongitude(),
                p.getCoordinates().getLatitude()));
        assertTrue(e.covers(p2.getCoordinates().getLongitude(),
                p2.getCoordinates().getLatitude()));
    }

    @Test
    public void testCreateReadObservationNoFeatureOfInterestOMMeasurement() throws Exception {
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

        // Create Observations with invalid results
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("foo", "bar");
        parameters.put("baz", "qux");
        createObservationFailure(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "true",
                parameters);
        createObservationFailure(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "\"an arbitrary string\"",
                parameters);

        // Create first Observation
        String location1 = createObservation(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "42.23",
                parameters);

        // Create second Observation
        parameters = new LinkedHashMap<String, String>();
        parameters.put("one", "1");
        parameters.put("two", "2");
        String location2 = createObservation(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2015-01-03T00:01:00.00+08:00", null,
                null,
                null, null,
                "23",
                parameters);

        // Read newly created Observations
        ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {};
        ResponseEntity<String> responseStr = rest.exchange(location1,
                HttpMethod.GET, getAuthorizedEntity(port, Observation.class), responseTypeStr);
        String s = responseStr.getBody();
        JsonReader r = Json.createReader(new StringReader(s));
        JsonObject obj = r.readObject();
        assertEquals(42.23, obj.getJsonNumber("result").doubleValue(), 0.001);
        assertEquals("2014-12-25T03:59:59.000Z", obj.getString("phenomenonTime"));
        JsonArray params = obj.getJsonArray("parameters");
        JsonObject param = params.getJsonObject(0);
        assertEquals("qux", param.getString("baz"));
        param = params.getJsonObject(1);
        assertEquals("bar", param.getString("foo"));

        responseStr = rest.exchange(location2,
                HttpMethod.GET, getAuthorizedEntity(port, Observation.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        assertEquals(23, obj.getJsonNumber("result").doubleValue(), 0.001);
        assertEquals("2015-01-02T16:01:00.000Z", obj.getString("phenomenonTime"));
        params = obj.getJsonArray("parameters");
        param = params.getJsonObject(0);
        assertEquals("1", param.getString("one"));
        param = params.getJsonObject(1);
        assertEquals("2", param.getString("two"));
    }

    @Test
    public void testCreateReadObservationNoFeatureOfInterestOMCountObservation() throws Exception {
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
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_CountObservation",
                thingUuid,
                sensorUuid,
                observedPropertyUuid);
        String datastreamUuid = Utility.extractUuidForEntityUrl(dsUrl);

        // Create Observations with invalid results
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("foo", "bar");
        parameters.put("baz", "qux");
        createObservationFailure(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "42.23",
                parameters);
        createObservationFailure(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "true",
                parameters);
        createObservationFailure(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "\"an arbitrary string\"",
                parameters);

        // Create Observation
        String location1 = createObservation(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "42",
                parameters);

        // Read newly created Observation
        ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {};
        ResponseEntity<String> responseStr = rest.exchange(location1,
                HttpMethod.GET, getAuthorizedEntity(port, Observation.class), responseTypeStr);
        String s = responseStr.getBody();
        JsonReader r = Json.createReader(new StringReader(s));
        JsonObject obj = r.readObject();
        assertEquals(42, obj.getJsonNumber("result").longValue());
        assertEquals("2014-12-25T03:59:59.000Z", obj.getString("phenomenonTime"));
        JsonArray params = obj.getJsonArray("parameters");
        JsonObject param = params.getJsonObject(0);
        assertEquals("qux", param.getString("baz"));
        param = params.getJsonObject(1);
        assertEquals("bar", param.getString("foo"));
    }

    @Test
    public void testCreateReadObservationNoFeatureOfInterestOMTruthObservation() throws Exception {
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
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_TruthObservation",
                thingUuid,
                sensorUuid,
                observedPropertyUuid);
        String datastreamUuid = Utility.extractUuidForEntityUrl(dsUrl);

        // Create Observations with invalid results
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("foo", "bar");
        parameters.put("baz", "qux");
        createObservationFailure(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "42.23",
                parameters);
        createObservationFailure(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "42",
                parameters);
        createObservationFailure(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "\"an arbitrary string\"",
                parameters);

        // Create Observation
        String location1 = createObservation(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "true",
                parameters);

        // Create second Observation
        parameters = new LinkedHashMap<String, String>();
        parameters.put("one", "1");
        parameters.put("two", "2");
        String location2 = createObservation(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2015-01-03T00:01:00.00+08:00", null,
                null,
                null, null,
                "false",
                parameters);

        // Read newly created Observation
        ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {};
        ResponseEntity<String> responseStr = rest.exchange(location1,
                HttpMethod.GET, getAuthorizedEntity(port, Observation.class), responseTypeStr);
        String s = responseStr.getBody();
        JsonReader r = Json.createReader(new StringReader(s));
        JsonObject obj = r.readObject();
        assertEquals(true, obj.getBoolean("result"));
        assertEquals("2014-12-25T03:59:59.000Z", obj.getString("phenomenonTime"));
        JsonArray params = obj.getJsonArray("parameters");
        JsonObject param = params.getJsonObject(0);
        assertEquals("qux", param.getString("baz"));
        param = params.getJsonObject(1);
        assertEquals("bar", param.getString("foo"));

        responseStr = rest.exchange(location2,
                HttpMethod.GET, getAuthorizedEntity(port, Observation.class), responseTypeStr);
        s = responseStr.getBody();
        r = Json.createReader(new StringReader(s));
        obj = r.readObject();
        assertEquals(false, obj.getBoolean("result"));
        assertEquals("2015-01-02T16:01:00.000Z", obj.getString("phenomenonTime"));
        params = obj.getJsonArray("parameters");
        param = params.getJsonObject(0);
        assertEquals("1", param.getString("one"));
        param = params.getJsonObject(1);
        assertEquals("2", param.getString("two"));
    }

    @Test
    public void testCreateReadObservationNoFeatureOfInterestOMObservation() throws Exception {
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
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Observation",
                thingUuid,
                sensorUuid,
                observedPropertyUuid);
        String datastreamUuid = Utility.extractUuidForEntityUrl(dsUrl);

        // Create Observations with invalid results
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("foo", "bar");
        parameters.put("baz", "qux");
        createObservationFailure(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "42.23",
                parameters);
        createObservationFailure(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "42",
                parameters);
        createObservationFailure(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "true",
                parameters);

        // Create Observation
        String location1 = createObservation(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "\"an arbitrary string\"",
                parameters);

        // Read newly created Observation
        ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {};
        ResponseEntity<String> responseStr = rest.exchange(location1,
                HttpMethod.GET, getAuthorizedEntity(port, Observation.class), responseTypeStr);
        String s = responseStr.getBody();
        JsonReader r = Json.createReader(new StringReader(s));
        JsonObject obj = r.readObject();
        assertEquals("an arbitrary string", obj.getString("result"));
        assertEquals("2014-12-25T03:59:59.000Z", obj.getString("phenomenonTime"));
        JsonArray params = obj.getJsonArray("parameters");
        JsonObject param = params.getJsonObject(0);
        assertEquals("qux", param.getString("baz"));
        param = params.getJsonObject(1);
        assertEquals("bar", param.getString("foo"));
    }

    @Test
    public void testCreateReadObservationNoFeatureOfInterestOMCategoryObservation() throws Exception {
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
                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_CategoryObservation",
                thingUuid,
                sensorUuid,
                observedPropertyUuid);
        String datastreamUuid = Utility.extractUuidForEntityUrl(dsUrl);

        // Create Observations with invalid results
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("foo", "bar");
        parameters.put("baz", "qux");
        createObservationFailure(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "42.23",
                parameters);
        createObservationFailure(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "42",
                parameters);
        createObservationFailure(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "true",
                parameters);
        createObservationFailure(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "\"an arbitrary string\"",
                parameters);

        // Create Observation
        String location1 = createObservation(baseUrl, port, rest,
                null,
                datastreamUuid,
                "2014-12-25T11:59:59.00+08:00", null,
                null,
                null, null,
                "\"http://some.uri/of/some/definition#section1.1\"",
                parameters);

        // Read newly created Observation
        ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {};
        ResponseEntity<String> responseStr = rest.exchange(location1,
                HttpMethod.GET, getAuthorizedEntity(port, Observation.class), responseTypeStr);
        String s = responseStr.getBody();
        JsonReader r = Json.createReader(new StringReader(s));
        JsonObject obj = r.readObject();
        assertEquals("http://some.uri/of/some/definition#section1.1", obj.getString("result"));
        assertEquals("2014-12-25T03:59:59.000Z", obj.getString("phenomenonTime"));
        JsonArray params = obj.getJsonArray("parameters");
        JsonObject param = params.getJsonObject(0);
        assertEquals("qux", param.getString("baz"));
        param = params.getJsonObject(1);
        assertEquals("bar", param.getString("foo"));
    }
}
