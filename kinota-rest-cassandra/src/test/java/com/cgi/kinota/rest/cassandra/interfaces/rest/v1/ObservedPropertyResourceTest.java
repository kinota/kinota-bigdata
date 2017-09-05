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

import com.cgi.kinota.commons.domain.ObservedProperty;

import com.cgi.kinota.rest.cassandra.Application;
import com.cgi.kinota.rest.cassandra.CassandraObservedPropertyTestBase;
import com.cgi.kinota.rest.cassandra.interfaces.rest.Utility;
import com.cgi.kinota.persistence.cassandra.config.SpringDataCassandraConfig;

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
public class ObservedPropertyResourceTest extends CassandraObservedPropertyTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ObservedPropertyResourceTest.class);

    static String description1 = "The dewpoint temperature is the temperature to which the air must be " +
            "cooled, at constant pressure, for dew to form. As the grass and other objects " +
            "near the ground cool to the dewpoint, some of the water vapor in the " +
            "atmosphere condenses into liquid water on the objects.";
    static String requestStr1 = "{\"name\":\"DewPoint Temperature\"," +
            "\"definition\":\"http://dbpedia.org/page/Dew_point\"," +
            "\"description\":" +
            "\"" + description1 + "\"" +
            "}";

    public static String createObservedProperty(String baseUrl,
                                                int port,
                                                RestTemplate rest,
                                                String name,
                                                String definition,
                                                String description) {
        // Build request entity
        String requestStr = "{\"name\":\"" + name + "\"," +
                "\"definition\":\"" + definition + "\"," +
                "\"description\":" +
                "\"" + description + "\"" +
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

        // Check that the location response parameter is the self link of the created ObservedProperty
        // (e.g. "http://localhost:55704/v1.0/ObservedProperty(b23e2d40-d381-11e6-ba14-6733c7a00eb7)")
        Pair<Boolean, String> matchesLocation = Utility.doesLocationPathMatch(response, LOC_PATT);
        assertTrue(matchesLocation.getLeft());
        String location = matchesLocation.getRight();

        // Check that selfLink of response matches
        try {
            String selfLink = o.getString(ANNO_IOT_SELF_LINK);
            assertEquals(location, selfLink);
        } catch (NullPointerException npe) {}

        return location;
    }

    @Test
    public void testCreateReadDeleteObservedProperty() throws Exception {
        try {
            String location = createObservedProperty(baseUrl, port, rest,
                    "DewPoint Temperature",
                    "http://dbpedia.org/page/Dew_point",
                    description1);

            // TODO: Check for navigation links to related entities

            // Read newly created ObservedProperty
            ParameterizedTypeReference<ObservedProperty> sensorResponseType = new ParameterizedTypeReference<ObservedProperty>() {
            };
            ResponseEntity<ObservedProperty> opResponse = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, ObservedProperty.class), sensorResponseType);
            ObservedProperty op = opResponse.getBody();
            assertEquals("DewPoint Temperature", op.getName());
            assertEquals("http://dbpedia.org/page/Dew_point", op.getDefinition().toString());
            assertEquals(description1, op.getDescription());

            // Read newly created ObservedProperty, this time examine the raw JSON that comes back,
            //   so that we can verify that the metadata are correct
            ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {
            };
            ResponseEntity<String> responseStr;
            responseStr = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            String str = responseStr.getBody();
            JsonReader r = Json.createReader(new StringReader(str));
            JsonObject o = r.readObject();
            assertEquals(o.getString(ANNO_IOT_ID), op.getId().toString());
            assertEquals(o.getString(ANNO_IOT_SELF_LINK), location);
            // TODO: Compare navigation links for Datastreams

            // Fetch all ObservedProperties
            String allObsPropUrl = apiRootUrl + "ObservedProperties";
            responseStr = rest.exchange(allObsPropUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            String s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            o = r.readObject();
            assertEquals(1, o.getInt(ANNO_COLLECTION_COUNT));
            JsonArray a = o.getJsonArray(COLLECTION_ATTR);
            JsonObject obj = a.getJsonObject(0);
            String entityUrl = obj.getString(ANNO_IOT_SELF_LINK);
            assertEquals(location, entityUrl);

            // Delete ObservedProperty
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
    public void testCreateReadObservedPropertyPaging() throws Exception {
        assertEquals(Integer.valueOf(3), MAX_REQUEST_PAGE_SIZE);
        try {
            // Create first observed property
            String location = createObservedProperty(baseUrl, port, rest,
                    "DewPoint Temperature",
                    "http://dbpedia.org/page/Dew_point",
                    description1);

            // Create two observed property
            String location2 = createObservedProperty(baseUrl, port, rest,
                    "DewPoint Temperature 2",
                    "http://dbpedia.org/page/Dew_point2",
                    description1);

            // Create third observed property
            String location3 = createObservedProperty(baseUrl, port, rest,
                    "DewPoint Temperature 3",
                    "http://dbpedia.org/page/Dew_point3",
                    description1);

            // Create fourth observed property
            String location4 = createObservedProperty(baseUrl, port, rest,
                    "DewPoint Temperature 4",
                    "http://dbpedia.org/page/Dew_point4",
                    description1);

            // Fetch all ObservedProperties (should be 4 across 2 pages)
            String allObsPropUrl = apiRootUrl + "ObservedProperties";
            ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {};
            ResponseEntity<String> responseStr = rest.exchange(allObsPropUrl,
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
    public void testPatchObservedProperty() throws Exception {
        try {
            String location = createObservedProperty(baseUrl, port, rest,
                    "DewPoint Temperature",
                    "http://dbpedia.org/page/Dew_point",
                    description1);

            // Read newly created ObservedProperty
            ParameterizedTypeReference<ObservedProperty> opResponseType = new ParameterizedTypeReference<ObservedProperty>() {
            };
            ResponseEntity<ObservedProperty> opResponse = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, ObservedProperty.class), opResponseType);
            ObservedProperty op = opResponse.getBody();

            // Update the name of the ObservedProperty
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            ParameterizedTypeReference<JsonObject> responseType = new ParameterizedTypeReference<JsonObject>() {
            };
            String updateStr = "{\"name\":\"DryBulb Temperature\"}";
            HttpEntity<String> entity = new HttpEntity<String>(updateStr, headers);
            ResponseEntity<JsonObject> response = rest.exchange(location,
                    HttpMethod.PATCH, entity,
                    responseType);
            assertEquals(204, response.getStatusCodeValue());

            // Get the updated ObservedProperty
            opResponse = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, ObservedProperty.class), opResponseType);
            assertEquals(200, opResponse.getStatusCodeValue());
            ObservedProperty op2 = opResponse.getBody();
            assertEquals("DryBulb Temperature", op2.getName());
            assertEquals(op.getDefinition().toString(), op2.getDefinition().toString());
            assertEquals(op.getDescription(), op2.getDescription());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
        }
    }

    @Test
    public void testPatchUnknownObservedProperty() throws Exception {
        try {
            String uuid = UUIDs.timeBased().toString();
            String url = baseUrl + "(" + uuid +")";
            // Build request entity
            String requestStr = "{\"name\":\"Some measurement\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<>(requestStr, headers);
            ParameterizedTypeReference<ObservedProperty> responseType = new ParameterizedTypeReference<ObservedProperty>() {
            };
            ResponseEntity<ObservedProperty> response = rest.exchange(url, HttpMethod.PATCH, entity, responseType);
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
    public void testPatchMalformedObservedProperty() throws Exception {
        try {
            String url = baseUrl + "(thisisnotauuid)";
            // Build request entity
            String requestStr = "{\"name\":\"Some measurement\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<>(requestStr, headers);
            ParameterizedTypeReference<ObservedProperty> responseType = new ParameterizedTypeReference<ObservedProperty>() {
            };
            ResponseEntity<ObservedProperty> response = rest.exchange(url, HttpMethod.PATCH, entity, responseType);
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
    public void testPutObservedProperty() throws Exception {
        try {
            String location = createObservedProperty(baseUrl, port, rest,
                    "DewPoint Temperature",
                    "http://dbpedia.org/page/Dew_point",
                    description1);

            // Read newly created ObservedProperty
            ParameterizedTypeReference<ObservedProperty> sensorResponseType = new ParameterizedTypeReference<ObservedProperty>() {
            };
            ResponseEntity<ObservedProperty> opResponse = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, ObservedProperty.class), sensorResponseType);
            ObservedProperty op = opResponse.getBody();

            // Update the ObservedProperty
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            ParameterizedTypeReference<JsonObject> responseType = new ParameterizedTypeReference<JsonObject>() {
            };

            String description2 = "The air temperature is the temperature of the air.";
            String updateStr = "{\"name\":\"Air Temperature\"," +
                    "\"definition\":\"http://dbpedia.org/page/Air_temperature\"," +
                    "\"description\":" +
                    "\"" + description2 + "\"" +
                    "}";
            HttpEntity<String> entity = new HttpEntity<String>(updateStr, headers);
            ResponseEntity<JsonObject> response = rest.exchange(location,
                    HttpMethod.PUT, entity,
                    responseType);
            assertEquals(204, response.getStatusCodeValue());

            // Get the updated ObservedProperty
            opResponse = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, ObservedProperty.class), sensorResponseType);
            ObservedProperty op2 = opResponse.getBody();
            assertEquals("Air Temperature", op2.getName());
            assertEquals("http://dbpedia.org/page/Air_temperature", op2.getDefinition().toString());
            assertEquals(description2, op2.getDescription());

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
    public void testPutUnknownObservedProperty() throws Exception {
        try {
            String uuid = UUIDs.timeBased().toString();
            String url = baseUrl + "(" + uuid +")";
            // Build request entity
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<String>(requestStr1, headers);
            ParameterizedTypeReference<ObservedProperty> responseType = new ParameterizedTypeReference<ObservedProperty>() {
            };
            ResponseEntity<ObservedProperty> response = rest.exchange(url, HttpMethod.PUT, entity, responseType);
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
    public void testPutMalformedObservedProperty() throws Exception {
        try {
            String url = baseUrl + "(thisisnotauuid)";
            // Build request entity
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<String>(requestStr1, headers);
            ParameterizedTypeReference<ObservedProperty> responseType = new ParameterizedTypeReference<ObservedProperty>() {
            };
            ResponseEntity<ObservedProperty> response = rest.exchange(url, HttpMethod.PUT, entity, responseType);
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
    public void testDeleteUnknownObservedProperty() throws Exception {
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
    public void testDeleteMalformedIdObservedProperty() throws Exception {
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
