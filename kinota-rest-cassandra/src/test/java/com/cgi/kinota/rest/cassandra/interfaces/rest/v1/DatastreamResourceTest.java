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
import com.cgi.kinota.commons.domain.ObservedProperty;
import com.cgi.kinota.commons.domain.Sensor;
import com.cgi.kinota.commons.domain.Thing;
import com.cgi.kinota.commons.domain.support.UnitOfMeasurement;

import com.cgi.kinota.rest.cassandra.Application;
import com.cgi.kinota.rest.cassandra.CassandraDatastreamTestBase;
import com.cgi.kinota.persistence.cassandra.config.SpringDataCassandraConfig;
import com.cgi.kinota.rest.cassandra.interfaces.rest.Utility;

import com.datastax.driver.core.utils.UUIDs;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.json.*;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.cgi.kinota.commons.Constants.*;
import static org.junit.Assert.*;

/**
 * Created by bmiles on 3/2/17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {Application.class, SpringDataCassandraConfig.class})
public class DatastreamResourceTest extends CassandraDatastreamTestBase {

    private static final Logger logger = LoggerFactory.getLogger(DatastreamResourceTest.class);

    public static String createDatastream(String baseUrl,
                                          int port,
                                          RestTemplate rest,
                                          String name,
                                          String description,
                                          String unitName,
                                          String unitSymbol,
                                          String unitDefinition,
                                          String observationTypeUri,
                                          String thingUuid,
                                          String sensorUuid,
                                          String observedPropertyUuid) {
        // Build request entity
        String requestStr = "{" +
                "\"Thing\":{\"@iot.id\":\"" + thingUuid + "\"}," +
                "\"Sensor\":{\"@iot.id\":\"" + sensorUuid + "\"}," +
                "\"ObservedProperty\":{\"@iot.id\":\"" + observedPropertyUuid + "\"}," +
                "\"name\":\"" + name + "\"," +
                "\"description\":\"" + description + "\"," +
                "\"unitOfMeasurement\":{" +
                "\"name\":\"" + unitName + "\"," +
                "\"symbol\":\"" + unitSymbol + "\"," +
                "\"definition\":\"" + unitDefinition + "\"}," +
                "\"observationType\":\"" + observationTypeUri + "\"" +
                "}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addTokenToHeader(port, headers);
        HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
        ParameterizedTypeReference<JsonObject> responseType = new ParameterizedTypeReference<JsonObject>() {
        };
        ResponseEntity<JsonObject> response = rest.exchange(baseUrl,
                HttpMethod.POST, entity,
                responseType);
        assertEquals(201, response.getStatusCodeValue());
        JsonObject o = response.getBody();

        // Check that the location response parameter is the self link of the created Thing
        // (e.g. "http://localhost:55704/v1.0/Datastream(b23e2d40-d381-11e6-ba14-6733c7a00eb7)")
        Pair<Boolean, String> matchesLocation = Utility.doesLocationPathMatch(response, LOC_PATT);
        assertTrue(matchesLocation.getLeft());
        String location = matchesLocation.getRight();

        // Check that selfLink of response matches
        try {
            String selfLink = o.getString(ANNO_IOT_SELF_LINK);
            assertEquals(location, selfLink);
        } catch (NullPointerException npe) {
        }

        return location;
    }

    public static JsonObject readDatastream(String location, int port,
                                            RestTemplate rest) {
        ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {
        };
        ResponseEntity<String> responseStr = rest.exchange(location,
                HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
        String s = responseStr.getBody();
        JsonReader r = Json.createReader(new StringReader(s));
        return r.readObject();
    }

    @Test
    public void testCreateReadDeleteDatastream() throws Exception {
        try {
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

            String location = createDatastream(baseUrl, port, rest,
                    "oven temperature",
                    "This is a datastream measuring the air temperature in an oven.",
                    "degree Celsius",
                    "C", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                    "http://unitsofmeasure.org/ucum.html#para-30",
                    "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                    thingUuid,
                    sensorUuid,
                    observedPropertyUuid);

            // Read newly created Datastream
            JsonObject o = readDatastream(location, port, rest);
            assertEquals(o.getString(ANNO_IOT_SELF_LINK), location);
            assertEquals("oven temperature", o.getString("name"));
            assertEquals("This is a datastream measuring the air temperature in an oven.",
                    o.getString("description"));
            assertEquals("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                    o.getString("observationType"));
            JsonObject u = o.getJsonObject("unitOfMeasurement");
            assertNotNull(u);
            assertEquals("degree Celsius", u.getString("name"));
            assertEquals("http://unitsofmeasure.org/ucum.html#para-30",
                    u.getString("definition"));
            assertEquals("C", u.getString("symbol"));

            // Ensure temporal and spatial summaries are null to start
            boolean threwException = false;
            try {
                o.getString("phenomenonTime");
            } catch (NullPointerException npe) {
                threwException = true;
            }
            assertTrue(threwException);
            threwException = false;
            try {
                o.getString("resultTime");
            } catch (NullPointerException npe) {
                threwException = true;
            }
            assertTrue(threwException);
            threwException = false;
            try {
                JsonValue v = o.get("observedArea");
                assertEquals("null", v.toString());
            } catch (NullPointerException npe) {
                threwException = true;
            }
            assertFalse(threwException);

            // Fetch ObservedProperty via the Datastream's navigation link to it
            ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {};
            String opRel = o.getString(ObservedProperty.NAV_LINK_ONE);
            assertNotNull(opRel);
            String opNavUrl = apiRootUrl + opRel;

            ResponseEntity<String> responseStr = rest.exchange(opNavUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            String s = responseStr.getBody();
            JsonReader r = Json.createReader(new StringReader(s));
            JsonObject o2 = r.readObject();
            JsonArray a = o2.getJsonArray(COLLECTION_ATTR);
            assertTrue(a.size() == 1);
            JsonObject op = a.getJsonObject(0);
            String opRelUrl = op.getString(ANNO_IOT_SELF_LINK);
            assertEquals(opUrl, opRelUrl);
            assertEquals("DewPoint Temperature", op.getString("name"));
            assertEquals("http://dbpedia.org/page/Dew_point", op.getString("definition"));

            // Fetch ObservedProperty $ref
            String opRefUrl = opNavUrl + ASSOC_LINK_ADDY;
            responseStr = rest.exchange(opRefUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            JsonObject o3 = r.readObject();
            a = o3.getJsonArray(COLLECTION_ATTR);
            assertTrue(a.size() == 1);
            op = a.getJsonObject(0);
            String selfLink = op.getString(ANNO_IOT_SELF_LINKS);
            assertEquals(opUrl, selfLink);

            // Fetch Datastreams via ObservedProperty's relLink
            String datastreamUrl = opRelUrl + "/" + Datastream.NAME_PLURAL;
            responseStr = rest.exchange(datastreamUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            JsonObject o4 = r.readObject();
            a = o4.getJsonArray(COLLECTION_ATTR);
            assertTrue(a.size() == 1);
            op = a.getJsonObject(0);
            selfLink = op.getString(ANNO_IOT_SELF_LINK);
            assertEquals(location, selfLink);
            // Fetch Datastream $ref
            String datastreamRefUrl = datastreamUrl + ASSOC_LINK_ADDY;
            responseStr = rest.exchange(datastreamRefUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            op = r.readObject();
            a = op.getJsonArray(COLLECTION_ATTR);
            assertTrue(a.size() == 1);
            JsonObject d = a.getJsonObject(0);
            selfLink = d.getString(ANNO_IOT_SELF_LINKS);
            assertEquals(location, selfLink);

            // Fetch Sensor via the Datastream's navigation link to it
            responseTypeStr = new ParameterizedTypeReference<String>() {};
            String sRel = o.getString(Sensor.NAV_LINK_ONE);
            assertNotNull(sRel);
            String sNavUrl = apiRootUrl + sRel;

            responseStr = rest.exchange(sNavUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            o2 = r.readObject();
            a = o2.getJsonArray(COLLECTION_ATTR);
            assertTrue(a.size() == 1);
            JsonObject sen = a.getJsonObject(0);
            String sRelUrl = sen.getString(ANNO_IOT_SELF_LINK);
            assertEquals(sensorUrl, sRelUrl);
            assertEquals("TMP36", sen.getString("name"));
            assertEquals("TMP36 - Analog Temperature sensor", sen.getString("description"));
            assertEquals("application/pdf", sen.getString("encodingType"));
            assertEquals("http://example.org/TMP35_36_37.pdf", sen.getString("metadata"));

            // Fetch Sensor $ref
            String sRefUrl = sNavUrl + ASSOC_LINK_ADDY;
            responseStr = rest.exchange(sRefUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            o3 = r.readObject();
            a = o3.getJsonArray(COLLECTION_ATTR);
            assertTrue(a.size() == 1);
            op = a.getJsonObject(0);
            selfLink = op.getString(ANNO_IOT_SELF_LINKS);
            assertEquals(sensorUrl, selfLink);

            // Fetch Datastreams via Sensor's relLink
            datastreamUrl = sRelUrl + "/" + Datastream.NAME_PLURAL;
            responseStr = rest.exchange(datastreamUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            o4 = r.readObject();
            a = o4.getJsonArray(COLLECTION_ATTR);
            assertTrue(a.size() == 1);
            op = a.getJsonObject(0);
            selfLink = op.getString(ANNO_IOT_SELF_LINK);
            assertEquals(location, selfLink);
            // Fetch Datastream $ref
            datastreamRefUrl = datastreamUrl + ASSOC_LINK_ADDY;
            responseStr = rest.exchange(datastreamRefUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            op = r.readObject();
            a = op.getJsonArray(COLLECTION_ATTR);
            assertTrue(a.size() == 1);
            d = a.getJsonObject(0);
            selfLink = d.getString(ANNO_IOT_SELF_LINKS);
            assertEquals(location, selfLink);

            // Fetch Thing via the Datastream's navigation link to it
            responseTypeStr = new ParameterizedTypeReference<String>() {};
            String thingOneRel = o.getString(Thing.NAV_LINK_ONE);
            assertNotNull(thingOneRel);
            String thingNavUrl = apiRootUrl + thingOneRel;

            responseStr = rest.exchange(thingNavUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            o2 = r.readObject();
            a = o2.getJsonArray(COLLECTION_ATTR);
            assertTrue(a.size() == 1);
            JsonObject t = a.getJsonObject(0);
            String thingRelUrl = t.getString(ANNO_IOT_SELF_LINK);
            assertEquals(thingUrl, thingRelUrl);
            assertEquals("test thing 1", t.getString("name"));
            assertEquals("A thing for testing.", t.getString("description"));

            // Fetch Thing $ref
            String thingRefUrl = thingNavUrl + ASSOC_LINK_ADDY;
            responseStr = rest.exchange(thingRefUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            o3 = r.readObject();
            a = o3.getJsonArray(COLLECTION_ATTR);
            assertTrue(a.size() == 1);
            t = a.getJsonObject(0);
            selfLink = t.getString(ANNO_IOT_SELF_LINKS);
            assertEquals(thingUrl, selfLink);

            // Fetch Datastream via Thing's relLink
            datastreamUrl = thingRelUrl + "/" + Datastream.NAME_PLURAL;
            responseStr = rest.exchange(datastreamUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            o4 = r.readObject();
            a = o4.getJsonArray(COLLECTION_ATTR);
            assertTrue(a.size() == 1);
            t = a.getJsonObject(0);
            selfLink = t.getString(ANNO_IOT_SELF_LINK);
            assertEquals(location, selfLink);
            // Fetch Datastream $ref
            datastreamRefUrl = datastreamUrl + ASSOC_LINK_ADDY;
            responseStr = rest.exchange(datastreamRefUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            o = r.readObject();
            a = o.getJsonArray(COLLECTION_ATTR);
            assertTrue(a.size() == 1);
            d = a.getJsonObject(0);
            selfLink = d.getString(ANNO_IOT_SELF_LINKS);
            assertEquals(location, selfLink);

            // TODO: Read Sensor, and ObservedProperty via Datasteam's navigation links

            // Fetch all Datastreams
            String allDatastreamsUrl = apiRootUrl + "Datastreams";
            responseStr = rest.exchange(allDatastreamsUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            o = r.readObject();
            assertEquals(1, o.getInt(ANNO_COLLECTION_COUNT));
            a = o.getJsonArray(COLLECTION_ATTR);
            JsonObject obj = a.getJsonObject(0);
            String entityUrl = obj.getString(ANNO_IOT_SELF_LINK);
            assertEquals(location, entityUrl);

            // Delete Datastream
            ParameterizedTypeReference<JsonObject> responseType = new ParameterizedTypeReference<JsonObject>() {
            };
            ResponseEntity<JsonObject> response = rest.exchange(location,
                    HttpMethod.DELETE, getAuthorizedEntity(port, JsonObject.class),
                    responseType);
            assertEquals(204, response.getStatusCodeValue());
            // Try to get deleted Thing (this should fail with a 404)
            try {
                response = rest.exchange(location,
                        HttpMethod.GET, getAuthorizedEntity(port, JsonObject.class),
                        responseType);
            } catch (Exception e) {
                if (e instanceof HttpClientErrorException) {
                    HttpClientErrorException he = (HttpClientErrorException) e;
                    assertEquals(404, he.getRawStatusCode());
                } else {
                    logger.error(e.getMessage(), e);
                    fail(e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
        }
    }

    @Test
    public void testCreateDatastreamPaging() throws Exception {
        assertEquals(Integer.valueOf(3), MAX_REQUEST_PAGE_SIZE);
        try {
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

            // Create first Datastream
            String location = createDatastream(baseUrl, port, rest,
                    "oven temperature",
                    "This is a datastream measuring the air temperature in an oven.",
                    "degree Celsius",
                    "C", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                    "http://unitsofmeasure.org/ucum.html#para-30",
                    "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                    thingUuid,
                    sensorUuid,
                    observedPropertyUuid);

            // Create second Datastream
            String location2 = createDatastream(baseUrl, port, rest,
                    "oven temperature 2",
                    "This is a datastream measuring the air temperature in an oven. 2",
                    "degree Fahrenheit",
                    "F", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                    "http://unitsofmeasure.org/ucum.html#para-30",
                    "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                    thingUuid,
                    sensorUuid,
                    observedPropertyUuid);

            // Create third Datastream
            String location3 = createDatastream(baseUrl, port, rest,
                    "oven temperature 3",
                    "This is a datastream measuring the air temperature in an oven. 3",
                    "degree Kelvin",
                    "K", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                    "http://unitsofmeasure.org/ucum.html#para-30",
                    "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                    thingUuid,
                    sensorUuid,
                    observedPropertyUuid);

            // Create fourth Datastream
            String location4 = createDatastream(baseUrl, port, rest,
                    "oven temperature 4",
                    "This is a datastream measuring the air temperature in an oven. 4",
                    "degree Mibflamb",
                    "Mb", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                    "http://unitsofmeasure.org/ucum.html#para-30",
                    "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                    thingUuid,
                    sensorUuid,
                    observedPropertyUuid);

            // Fetch Datastreams via Thing's relLink (should be 4 across 2 pages)
            ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {};
            String datastreamUrl = thingUrl + "/" + Datastream.NAME_PLURAL;
            ResponseEntity<String> responseStr = rest.exchange(datastreamUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            String s = responseStr.getBody();
            JsonReader r = Json.createReader(new StringReader(s));
            JsonObject obj = r.readObject();
            JsonArray a = obj.getJsonArray(COLLECTION_ATTR);
            assertEquals(Integer.valueOf(3), Integer.valueOf(a.size()));

            String nextLink = obj.getString(ANNO_IOT_NEXT_LINK);
            assertNotNull(nextLink);

            // Fetch nextLink
            responseStr = rest.exchange(nextLink,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            String s2 = responseStr.getBody();
            r = Json.createReader(new StringReader(s2));
            obj = r.readObject();
            a = obj.getJsonArray("value");
            assertEquals(Integer.valueOf(1), Integer.valueOf(a.size()));

            boolean exceptionThrown = false;
            try {
                nextLink = obj.getString(ANNO_IOT_NEXT_LINK);
            } catch (NullPointerException npe) {
                exceptionThrown = true;
            }
            assertTrue(exceptionThrown);

            // Fetch Datastreams $ref via Thing's relLink (should be 4 across 2 pages)
            String datastreamRefUrl = thingUrl + "/" + Datastream.NAME_PLURAL + "/$ref";
            responseStr = rest.exchange(datastreamRefUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            String s3 = responseStr.getBody();
            r = Json.createReader(new StringReader(s3));
            obj = r.readObject();
            a = obj.getJsonArray(COLLECTION_ATTR);
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

            // Fetch Datastreams via Sensor's relLink (should be 4 across 2 pages)
            datastreamUrl = sensorUrl + "/" + Datastream.NAME_PLURAL;
            responseStr = rest.exchange(datastreamUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            obj = r.readObject();
            a = obj.getJsonArray(COLLECTION_ATTR);
            assertEquals(Integer.valueOf(3), Integer.valueOf(a.size()));

            nextLink = obj.getString(ANNO_IOT_NEXT_LINK);
            assertNotNull(nextLink);

            // Fetch nextLink
            responseStr = rest.exchange(nextLink,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s2 = responseStr.getBody();
            r = Json.createReader(new StringReader(s2));
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

            // Fetch Datastreams $ref via Sensor's relLink (should be 4 across 2 pages)
            datastreamRefUrl = sensorUrl + "/" + Datastream.NAME_PLURAL + "/$ref";
            responseStr = rest.exchange(datastreamRefUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s3 = responseStr.getBody();
            r = Json.createReader(new StringReader(s3));
            obj = r.readObject();
            a = obj.getJsonArray(COLLECTION_ATTR);
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

            // Fetch Datastreams via ObservedProperty's relLink (should be 4 across 2 pages)
            datastreamUrl = opUrl + "/" + Datastream.NAME_PLURAL;
            responseStr = rest.exchange(datastreamUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            obj = r.readObject();
            a = obj.getJsonArray(COLLECTION_ATTR);
            assertEquals(Integer.valueOf(3), Integer.valueOf(a.size()));

            nextLink = obj.getString(ANNO_IOT_NEXT_LINK);
            assertNotNull(nextLink);

            // Fetch nextLink
            responseStr = rest.exchange(nextLink,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s2 = responseStr.getBody();
            r = Json.createReader(new StringReader(s2));
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

            // Fetch Datastreams $ref via ObservedProperty's relLink (should be 4 across 2 pages)
            datastreamRefUrl = opUrl + "/" + Datastream.NAME_PLURAL + "/$ref";
            responseStr = rest.exchange(datastreamRefUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s3 = responseStr.getBody();
            r = Json.createReader(new StringReader(s3));
            obj = r.readObject();
            a = obj.getJsonArray(COLLECTION_ATTR);
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

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
        }
    }

    // TODO: Test creation of Datastream without Thing, Sensor, ObservedProperty (should fail)

    @Test
    public void testReadUnknownDatastream() throws Exception {
        try {
            String uuid = UUIDs.timeBased().toString();
            String url = baseUrl + "(" + uuid +")";
            ParameterizedTypeReference<Datastream> responseType = new ParameterizedTypeReference<Datastream>() {
            };
            ResponseEntity<Datastream> response = rest.exchange(url,
                    HttpMethod.GET, getAuthorizedEntity(port, Datastream.class),
                    responseType);
        } catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                HttpClientErrorException he = (HttpClientErrorException) e;
                assertEquals(404, he.getRawStatusCode());
            } else {
                logger.error(e.getMessage(), e);
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testReadMalformedIdDatastrean() throws Exception {
        try {
            String url = baseUrl + "(thisisnotauuid)";
            ParameterizedTypeReference<Datastream> responseType = new ParameterizedTypeReference<Datastream>() {
            };
            ResponseEntity<Datastream> response = rest.exchange(url,
                    HttpMethod.GET, getAuthorizedEntity(port, Datastream.class),
                    responseType);
        } catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                HttpClientErrorException he = (HttpClientErrorException) e;
                assertEquals(400, he.getRawStatusCode());
            } else {
                logger.error(e.getMessage(), e);
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testPatchDatastream() throws Exception {
        try {
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

            String location = createDatastream(baseUrl, port, rest,
                    "oven temperature",
                    "This is a datastream measuring the air temperature in an oven.",
                    "degree Celsius",
                    "C", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                    "http://unitsofmeasure.org/ucum.html#para-30",
                    "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                    thingUuid,
                    sensorUuid,
                    observedPropertyUuid);

            // Update the name of the Datastream
            ParameterizedTypeReference<Datastream> responseType = new ParameterizedTypeReference<Datastream>() {
            };
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            String updateStr = "{\"name\":\"refrigerator temperature\"}";
            HttpEntity<String> entity = new HttpEntity<String>(updateStr, headers);
            ResponseEntity<Datastream> response = rest.exchange(location,
                    HttpMethod.PATCH, entity,
                    responseType);
            assertEquals(204, response.getStatusCodeValue());

            // Get the updated Datastream
            response = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Datastream.class), responseType);
            Datastream d = response.getBody();
            assertEquals("refrigerator temperature", d.getName());
            assertEquals("This is a datastream measuring the air temperature in an oven.", d.getDescription());
            assertEquals("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement", d.getObservationType().toString());
            UnitOfMeasurement u = d.getUnitOfMeasurement();
            assertEquals("degree Celsius", u.getName());
            assertEquals("C", u.getSymbol());
            assertEquals("http://unitsofmeasure.org/ucum.html#para-30", u.getDefinition().toString());

            // Update the description of the Datastream
            updateStr = "{\"description\":\"This is a datastream measuring the air temperature in an refrigerator.\"}";
            entity = new HttpEntity<String>(updateStr, headers);
            response = rest.exchange(location,
                    HttpMethod.PATCH, entity,
                    responseType);
            assertEquals(204, response.getStatusCodeValue());

            // Get the updated Datastream
            response = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Datastream.class), responseType);
            Datastream d2 = response.getBody();
            assertEquals("refrigerator temperature", d2.getName());
            assertEquals("This is a datastream measuring the air temperature in an refrigerator.", d2.getDescription());
            assertEquals("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement", d2.getObservationType().toString());
            u = d2.getUnitOfMeasurement();
            assertEquals("degree Celsius", u.getName());
            assertEquals("C", u.getSymbol());
            assertEquals("http://unitsofmeasure.org/ucum.html#para-30", u.getDefinition().toString());

            // Update the observationTypeUri of the Datastream
            updateStr = "{\"observationType\":\"http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Observation\"}";
            entity = new HttpEntity<String>(updateStr, headers);
            response = rest.exchange(location,
                    HttpMethod.PATCH, entity,
                    responseType);
            assertEquals(204, response.getStatusCodeValue());

            // Get the updated Datastream
            response = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Datastream.class), responseType);
            Datastream d3 = response.getBody();
            assertEquals("refrigerator temperature", d3.getName());
            assertEquals("This is a datastream measuring the air temperature in an refrigerator.", d3.getDescription());
            assertEquals("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Observation", d3.getObservationType().toString());
            u = d3.getUnitOfMeasurement();
            assertEquals("degree Celsius", u.getName());
            assertEquals("C", u.getSymbol());
            assertEquals("http://unitsofmeasure.org/ucum.html#para-30", u.getDefinition().toString());

            // Update the unitOfMeasurement of the Datastream
            updateStr = "{\"unitOfMeasurement\":{" +
                    "\"name\":\"degree Fahrenheit\"," +
                    "\"symbol\":\"F\"," +
                    "\"definition\":\"http://unitsofmeasure.org/ucum.html#para-42\"}}";
            entity = new HttpEntity<String>(updateStr, headers);
            response = rest.exchange(location,
                    HttpMethod.PATCH, entity,
                    responseType);
            assertEquals(204, response.getStatusCodeValue());

            // Get the updated Datastream
            response = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Datastream.class), responseType);
            Datastream d4 = response.getBody();
            assertEquals("refrigerator temperature", d4.getName());
            assertEquals("This is a datastream measuring the air temperature in an refrigerator.", d4.getDescription());
            assertEquals("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Observation", d4.getObservationType().toString());
            u = d4.getUnitOfMeasurement();
            assertEquals("degree Fahrenheit", u.getName());
            assertEquals("F", u.getSymbol());
            assertEquals("http://unitsofmeasure.org/ucum.html#para-42", u.getDefinition().toString());

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
        }
    }

    @Test
    public void testPatchUnknownDatastream() throws Exception {
        try {
            String uuid = UUIDs.timeBased().toString();
            String url = baseUrl + "(" + uuid +")";
            // Build request entity
            String requestStr = "{\"name\":\"test datastream 2\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
            ParameterizedTypeReference<Datastream> responseType = new ParameterizedTypeReference<Datastream>() {
            };
            ResponseEntity<Datastream> response = rest.exchange(url, HttpMethod.PATCH, entity, responseType);
        } catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                HttpClientErrorException he = (HttpClientErrorException) e;
                assertEquals(404, he.getRawStatusCode());
            } else {
                logger.error(e.getMessage(), e);
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testPatchMalformedDatastream() throws Exception {
        try {
            String url = baseUrl + "(thisisnotauuid)";
            // Build request entity
            String requestStr = "{\"name\":\"test datasteam 2\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
            ParameterizedTypeReference<Datastream> responseType = new ParameterizedTypeReference<Datastream>() {
            };
            ResponseEntity<Datastream> response = rest.exchange(url, HttpMethod.PATCH, entity, responseType);
        } catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                HttpClientErrorException he = (HttpClientErrorException) e;
                assertEquals(400, he.getRawStatusCode());
            } else {
                logger.error(e.getMessage(), e);
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testPutDatastream() throws Exception {
        try {
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

            String location = createDatastream(baseUrl, port, rest,
                    "oven temperature",
                    "This is a datastream measuring the air temperature in an oven.",
                    "degree Celsius",
                    "C", // Spring Data Cassandra doesn't seem to handle Unicode/UTF-16 by default.
                    "http://unitsofmeasure.org/ucum.html#para-30",
                    "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                    thingUuid,
                    sensorUuid,
                    observedPropertyUuid);

            // Update the Datastream
            ParameterizedTypeReference<Datastream> responseType = new ParameterizedTypeReference<Datastream>() {
            };
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            String newName = "refrigerator temperature";
            String newDescription = "This is a datastream measuring the air temperature in an refrigerator.";
            String newUnitName = "degree Fahrenheit";
            String newUnitSymbol = "F";
            String newUnitDefinition = "http://unitsofmeasure.org/ucum.html#para-42";
            String newObservationType = "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Observation";
            String requestStr = "{" +
                    "\"name\":\"" + newName + "\"," +
                    "\"description\":\"" + newDescription + "\"," +
                    "\"unitOfMeasurement\":{" +
                    "\"name\":\"" + newUnitName + "\"," +
                    "\"symbol\":\"" + newUnitSymbol + "\"," +
                    "\"definition\":\"" + newUnitDefinition + "\"}," +
                    "\"observationType\":\"" + newObservationType + "\"" +
                    "}";
            HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
            ResponseEntity<Datastream> response = rest.exchange(location,
                    HttpMethod.PUT, entity,
                    responseType);
            assertEquals(204, response.getStatusCodeValue());

            // Get the updated Datastream
            response = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Datastream.class), responseType);
            Datastream d = response.getBody();
            assertEquals(newName, d.getName());
            assertEquals(newDescription, d.getDescription());
            assertEquals(newObservationType, d.getObservationType().toString());
            UnitOfMeasurement u = d.getUnitOfMeasurement();
            assertEquals(newUnitName, u.getName());
            assertEquals(newUnitSymbol, u.getSymbol());
            assertEquals(newUnitDefinition, u.getDefinition().toString());

            // Test incomplete request (should return a 400)
            try {
                String patchStr = "{\"name\":\"what's in a name\"}";
                entity = new HttpEntity<String>(patchStr, headers);
                response = rest.exchange(location,
                        HttpMethod.PUT, entity,
                        responseType);
            } catch (Exception e) {
                if (e instanceof HttpClientErrorException) {
                    HttpClientErrorException he = (HttpClientErrorException) e;
                    assertEquals(400, he.getRawStatusCode());
                } else {
                    logger.error(e.getMessage(), e);
                    fail(e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
        }
    }

    @Test
    public void testPutUnknownDatastream() throws Exception {
        try {
            String uuid = UUIDs.timeBased().toString();
            String url = baseUrl + "(" + uuid +")";
            // Build request entity
            String newName = "refrigerator temperature";
            String newDescription = "This is a datastream measuring the air temperature in an refrigerator.";
            String newUnitName = "degree Fahrenheit";
            String newUnitSymbol = "F";
            String newUnitDefinition = "http://unitsofmeasure.org/ucum.html#para-42";
            String newObservationType = "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Observation";
            String requestStr = "{" +
                    "\"name\":\"" + newName + "\"," +
                    "\"description\":\"" + newDescription + "\"," +
                    "\"unitOfMeasurement\":{" +
                    "\"name\":\"" + newUnitName + "\"," +
                    "\"symbol\":\"" + newUnitSymbol + "\"," +
                    "\"definition\":\"" + newUnitDefinition + "\"}," +
                    "\"observationType\":\"" + newObservationType + "\"" +
                    "}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
            ParameterizedTypeReference<Datastream> responseType = new ParameterizedTypeReference<Datastream>() {
            };
            ResponseEntity<Datastream> response = rest.exchange(url, HttpMethod.PUT, entity, responseType);
        } catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                HttpClientErrorException he = (HttpClientErrorException) e;
                assertEquals(404, he.getRawStatusCode());
            } else {
                logger.error(e.getMessage(), e);
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testPutMalformedThing() throws Exception {
        try {
            String url = baseUrl + "(thisisnotauuid)";
            // Build request entity
            String newName = "refrigerator temperature";
            String newDescription = "This is a datastream measuring the air temperature in an refrigerator.";
            String newUnitName = "degree Fahrenheit";
            String newUnitSymbol = "F";
            String newUnitDefinition = "http://unitsofmeasure.org/ucum.html#para-42";
            String newObservationType = "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Observation";
            String requestStr = "{" +
                    "\"name\":\"" + newName + "\"," +
                    "\"description\":\"" + newDescription + "\"," +
                    "\"unitOfMeasurement\":{" +
                    "\"name\":\"" + newUnitName + "\"," +
                    "\"symbol\":\"" + newUnitSymbol + "\"," +
                    "\"definition\":\"" + newUnitDefinition + "\"}," +
                    "\"observationType\":\"" + newObservationType + "\"" +
                    "}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
            ParameterizedTypeReference<Datastream> responseType = new ParameterizedTypeReference<Datastream>() {
            };
            ResponseEntity<Datastream> response = rest.exchange(url, HttpMethod.PUT, entity, responseType);
        } catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                HttpClientErrorException he = (HttpClientErrorException) e;
                assertEquals(400, he.getRawStatusCode());
            } else {
                logger.error(e.getMessage(), e);
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testDeleteUnknownDatastream() throws Exception {
        try {
            String uuid = UUIDs.timeBased().toString();
            String url = baseUrl + "(" + uuid +")";
            rest.exchange(url,
                    HttpMethod.DELETE, getAuthorizedEntity(port, Void.class),
                    Void.class);
        } catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                HttpClientErrorException he = (HttpClientErrorException) e;
                assertEquals(404, he.getRawStatusCode());
            } else {
                logger.error(e.getMessage(), e);
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testDeleteMalformedIdDatastream() throws Exception {
        try {
            String url = baseUrl + "(thisisnotauuid)";
            rest.exchange(url,
                    HttpMethod.DELETE, getAuthorizedEntity(port, Void.class),
                    Void.class);
        } catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                HttpClientErrorException he = (HttpClientErrorException) e;
                assertEquals(400, he.getRawStatusCode());
            } else {
                logger.error(e.getMessage(), e);
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testCreateDatastreamInvalidObservationType() throws Exception {
        try {
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

            // Build request entity
            String requestStr = "{" +
                    "\"Thing\":{\"@iot.id\":\"" + thingUuid + "\"}," +
                    "\"Sensor\":{\"@iot.id\":\"" + sensorUuid + "\"}," +
                    "\"ObservedProperty\":{\"@iot.id\":\"" + observedPropertyUuid + "\"}," +
                    "\"name\":\"" + "oven temperature" + "\"," +
                    "\"description\":\"" + "This is a datastream measuring the air temperature in an oven." + "\"," +
                    "\"unitOfMeasurement\":{" +
                    "\"name\":\"" + "degree Celsius" + "\"," +
                    "\"symbol\":\"" + "C" + "\"," +
                    "\"definition\":\"" + "http://unitsofmeasure.org/ucum.html#para-30" + "\"}," +
                    "\"observationType\":\"" + "http://not.a.valid/observation/type/uri" + "\"" +
                    "}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
            ParameterizedTypeReference<JsonObject> responseType = new ParameterizedTypeReference<JsonObject>() {
            };
            ResponseEntity<JsonObject> response = rest.exchange(baseUrl,
                    HttpMethod.POST, entity,
                    responseType);
        } catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                HttpClientErrorException he = (HttpClientErrorException) e;
                assertEquals(400, he.getRawStatusCode());
            } else {
                logger.error(e.getMessage(), e);
                fail(e.getMessage());
            }
        }
    }
}
