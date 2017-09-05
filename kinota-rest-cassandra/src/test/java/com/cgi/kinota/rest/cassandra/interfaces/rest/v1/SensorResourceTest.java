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

import com.cgi.kinota.commons.domain.Sensor;

import com.cgi.kinota.rest.cassandra.Application;
import com.cgi.kinota.rest.cassandra.CassandraSensorTestBase;
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

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;

import static com.cgi.kinota.commons.Constants.*;
import static org.junit.Assert.*;

/**
 * Created by bmiles on 2/23/17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {Application.class, SpringDataCassandraConfig.class})
public class SensorResourceTest extends CassandraSensorTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SensorResourceTest.class);

    public static String createSensor(String baseUrl,
                                      int port,
                                      RestTemplate rest,
                                      String name,
                                      String description,
                                      String encodingType,
                                      String metadata) {
        // Build request entity
        String requestStr = "{\"name\":\"" + name + "\"," +
                "\"description\":\"" + description + "\"," +
                "\"encodingType\":\"" + encodingType + "\"," +
                "\"metadata\":\"" + metadata + "\"" +
                "}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addTokenToHeader(port, headers);
        HttpEntity<String> entity = new HttpEntity<>(requestStr, headers);
        ParameterizedTypeReference<JsonObject> responseType = new ParameterizedTypeReference<JsonObject>() {
        };
        ResponseEntity<JsonObject> response = rest.exchange(baseUrl,
                HttpMethod.POST, entity,
                responseType);
        assertEquals(201, response.getStatusCodeValue());
        JsonObject o = response.getBody();

        // Check that the location response parameter is the self link of the created Location
        // (e.g. "http://localhost:55704/v1.0/Sensors(b23e2d40-d381-11e6-ba14-6733c7a00eb7)")
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

    @Test
    public void testCreateReadDeleteSensor() throws Exception {
        try {
            String location = createSensor(baseUrl, port, rest,
                    "TMP36",
                    "TMP36 - Analog Temperature sensor",
                    "application/pdf",
                    "http://example.org/TMP35_36_37.pdf");

            // TODO: Check for navigation links to related entities

            // Read newly created Sensor
            ParameterizedTypeReference<Sensor> sensorResponseType = new ParameterizedTypeReference<Sensor>() {
            };
            ResponseEntity<Sensor> sensorResponse = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Sensor.class), sensorResponseType);
            Sensor s = sensorResponse.getBody();
            assertEquals("TMP36", s.getName());
            assertEquals("TMP36 - Analog Temperature sensor", s.getDescription());
            assertEquals("application/pdf", s.getEncodingType().toString());
            assertEquals("http://example.org/TMP35_36_37.pdf", s.getMetadata().toString());

            // Read newly created Sensor, this time examine the raw JSON that comes back,
            //   so that we can verify that the metadata are correct
            ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {
            };
            ResponseEntity<String> responseStr;
            responseStr = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            String str = responseStr.getBody();
            JsonReader r = Json.createReader(new StringReader(str));
            JsonObject o = r.readObject();
            assertEquals(o.getString(ANNO_IOT_ID), s.getId().toString());
            assertEquals(o.getString(ANNO_IOT_SELF_LINK), location);
            // TODO: Compare navigation links for Datastreams

            // Fetch all Sensors
            String allSensorsUrl = apiRootUrl + "Sensors";
            responseStr = rest.exchange(allSensorsUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            str = responseStr.getBody();
            r = Json.createReader(new StringReader(str));
            o = r.readObject();
            assertEquals(1, o.getInt(ANNO_COLLECTION_COUNT));
            JsonArray a = o.getJsonArray(COLLECTION_ATTR);
            JsonObject obj = a.getJsonObject(0);
            String entityUrl = obj.getString(ANNO_IOT_SELF_LINK);
            assertEquals(location, entityUrl);

            // Delete Sensor
            ParameterizedTypeReference<JsonObject> responseType = new ParameterizedTypeReference<JsonObject>() {
            };
            ResponseEntity<JsonObject> response = rest.exchange(location,
                    HttpMethod.DELETE, getAuthorizedEntity(port, JsonObject.class),
                    responseType);
            assertEquals(204, response.getStatusCodeValue());
            // Try to get deleted Sensor (this should fail with a 404)
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
    public void testCreateReadSensorPaging() throws Exception {
        assertEquals(Integer.valueOf(3), MAX_REQUEST_PAGE_SIZE);
        try {
            // Create first sensor
            String location = createSensor(baseUrl, port, rest,
                    "TMP36",
                    "TMP36 - Analog Temperature sensor",
                    "application/pdf",
                    "http://example.org/TMP35_36_37.pdf");

            // Create first sensor
            String location2 = createSensor(baseUrl, port, rest,
                    "TMP36 2",
                    "TMP36 - Analog Temperature sensor 2",
                    "application/pdf",
                    "http://example.org/TMP35_36_37_2.pdf");

            // Create first sensor
            String location3 = createSensor(baseUrl, port, rest,
                    "TMP36 3",
                    "TMP36 - Analog Temperature sensor 3",
                    "application/pdf",
                    "http://example.org/TMP35_36_37_3.pdf");

            // Create first sensor
            String location4 = createSensor(baseUrl, port, rest,
                    "TMP36 4",
                    "TMP36 - Analog Temperature sensor 4",
                    "application/pdf",
                    "http://example.org/TMP35_36_37_4.pdf");

            // Fetch all Sensors (should be 4 across 2 pages)
            String allSensorsUrl = apiRootUrl + "Sensors";
            ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {};
            ResponseEntity<String> responseStr = rest.exchange(allSensorsUrl,
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

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
        }
    }

    @Test
    public void testPatchSensor() throws Exception {
        try {
            String location = createSensor(baseUrl, port, rest,
                    "TMP36",
                    "TMP36 - Analog Temperature sensor",
                    "application/pdf",
                    "http://example.org/TMP35_36_37.pdf");

            // Read newly created Sensor
            ParameterizedTypeReference<Sensor> sensorResponseType = new ParameterizedTypeReference<Sensor>() {
            };
            ResponseEntity<Sensor> sensorResponse = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Sensor.class), sensorResponseType);
            Sensor s = sensorResponse.getBody();

            // Update the name of the Sensor
            ParameterizedTypeReference<JsonObject> responseType = new ParameterizedTypeReference<JsonObject>() {
            };
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            String updateStr = "{\"name\":\"TMP42\"}";
            HttpEntity<String> entity = new HttpEntity<String>(updateStr, headers);
            ResponseEntity<JsonObject> response = rest.exchange(location,
                    HttpMethod.PATCH, entity,
                    responseType);
            assertEquals(204, response.getStatusCodeValue());

            // Get the updated Sensor
            sensorResponse = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Sensor.class), sensorResponseType);
            assertEquals(200, sensorResponse.getStatusCodeValue());
            Sensor s2 = sensorResponse.getBody();
            assertEquals("TMP42", s2.getName());
            assertEquals(s.getDescription(), s2.getDescription());
            assertEquals(s.getEncodingType().toString(), s2.getEncodingType().toString());
            assertEquals(s.getMetadata(), s2.getMetadata());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
        }
    }

    @Test
    public void testPatchUnknownSensor() throws Exception {
        try {
            String uuid = UUIDs.timeBased().toString();
            String url = baseUrl + "(" + uuid +")";
            // Build request entity
            String requestStr = "{\"name\":\"test sensor 2\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<>(requestStr, headers);
            ParameterizedTypeReference<Sensor> responseType = new ParameterizedTypeReference<Sensor>() {
            };
            ResponseEntity<Sensor> response = rest.exchange(url, HttpMethod.PATCH, entity, responseType);
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
    public void testPatchMalformedSensor() throws Exception {
        try {
            String url = baseUrl + "(thisisnotauuid)";
            // Build request entity
            String requestStr = "{\"name\":\"test sensor 2\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<>(requestStr, headers);
            ParameterizedTypeReference<Sensor> responseType = new ParameterizedTypeReference<Sensor>() {
            };
            ResponseEntity<Sensor> response = rest.exchange(url, HttpMethod.PATCH, entity, responseType);
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
    public void testPutSensor() throws Exception {
        try {
            String location = createSensor(baseUrl, port, rest,
                    "TMP36",
                    "TMP36 - Analog Temperature sensor",
                    "application/pdf",
                    "http://example.org/TMP35_36_37.pdf");

            // Read newly created Sensor
            ParameterizedTypeReference<Sensor> sensorResponseType = new ParameterizedTypeReference<Sensor>() {
            };
            ResponseEntity<Sensor> sensorResponse = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Sensor.class), sensorResponseType);
            Sensor s = sensorResponse.getBody();

            // Update the Sensor
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            ParameterizedTypeReference<JsonObject> responseType = new ParameterizedTypeReference<JsonObject>() {
            };

            String updateStr = "{\"name\":\"TMP42\"," +
                    "\"description\":\"TMP42 - Analog Temperature sensor\"," +
                    "\"encodingType\":\"image/png\"," +
                    "\"metadata\":\"http://example.org/TMP35_36_37.png\"" +
                    "}";
            HttpEntity<String> entity = new HttpEntity<>(updateStr, headers);
            ResponseEntity<JsonObject> response = rest.exchange(location,
                    HttpMethod.PUT, entity,
                    responseType);
            assertEquals(204, response.getStatusCodeValue());

            // Get the updated Sensor
            sensorResponse = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Sensor.class), sensorResponseType);
            Sensor s2 = sensorResponse.getBody();
            assertEquals("TMP42", s2.getName());
            assertEquals("TMP42 - Analog Temperature sensor", s2.getDescription());
            assertEquals("image/png", s2.getEncodingType().toString());
            assertEquals("http://example.org/TMP35_36_37.png", s2.getMetadata().toString());

            // Test incomplete request (should return a 400)
            try {
                String patchStr = "{\"name\":\"what's in a name?\"}";
                entity = new HttpEntity<>(patchStr, headers);
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
    public void testPutUnknownSensor() throws Exception {
        try {
            String uuid = UUIDs.timeBased().toString();
            String url = baseUrl + "(" + uuid +")";
            // Build request entity
            String requestStr = "{\"name\":\"TMP36\"," +
                    "\"description\":\"TMP36 - Analog Temperature sensor\"," +
                    "\"encodingType\":\"application/pdf\"," +
                    "\"metadata\":\"http://example.org/TMP35_36_37.pdf\"" +
                    "}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
            ParameterizedTypeReference<Sensor> responseType = new ParameterizedTypeReference<Sensor>() {
            };
            ResponseEntity<Sensor> response = rest.exchange(url, HttpMethod.PUT, entity, responseType);
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
    public void testPutMalformedSensor() throws Exception {
        try {
            String url = baseUrl + "(thisisnotauuid)";
            // Build request entity
            String requestStr = "{\"name\":\"TMP36\"," +
                    "\"description\":\"TMP36 - Analog Temperature sensor\"," +
                    "\"encodingType\":\"application/pdf\"," +
                    "\"metadata\":\"http://example.org/TMP35_36_37.pdf\"" +
                    "}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
            ParameterizedTypeReference<Sensor> responseType = new ParameterizedTypeReference<Sensor>() {
            };
            ResponseEntity<Sensor> response = rest.exchange(url, HttpMethod.PUT, entity, responseType);
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
    public void testDeleteUnknownSensor() throws Exception {
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
    public void testDeleteMalformedIdSensor() throws Exception {
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
}
