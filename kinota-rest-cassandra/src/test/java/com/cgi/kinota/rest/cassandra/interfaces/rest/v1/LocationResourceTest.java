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

import com.cgi.kinota.commons.domain.Location;

import com.cgi.kinota.rest.cassandra.Application;
import com.cgi.kinota.rest.cassandra.CassandraLocationTestBase;
import com.cgi.kinota.persistence.cassandra.config.SpringDataCassandraConfig;
import com.cgi.kinota.rest.cassandra.interfaces.rest.Utility;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.geojson.GeoJsonObject;
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

import javax.activation.MimeType;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;

import static com.cgi.kinota.commons.Constants.*;
import static org.junit.Assert.*;

/**
 * Created by bmiles on 1/20/17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {Application.class, SpringDataCassandraConfig.class})
public class LocationResourceTest extends CassandraLocationTestBase {

    private static final Logger logger = LoggerFactory.getLogger(LocationResourceTest.class);

    public static String createLocation(String baseUrl,
                                        int port,
                                        RestTemplate rest,
                                        String name,
                                        String description,
                                        Double lng,
                                        Double lat) {
        // Build request entity
        String requestStr = "{\"name\":\"" + name + "\"," +
                "\"description\":\"" + description + "\"," +
                "        \"encodingType\":\n" +
                "  \"application/vnd.geo+json\",\n" +
                "        \"location\": {\n" +
                "          \"type\":\n" +
                "  \"Feature\",\n" +
                "          \"geometry\": {\n" +
                "            \"type\":\n" +
                "  \"Point\",\n" +
                "           \n" +
                "  \"coordinates\": [" + lng + "," + lat + "]\n" +
                "          }\n" +
                "        }\n" +
                "      }";
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
        // (e.g. "http://localhost:55704/v1.0/Locations(b23e2d40-d381-11e6-ba14-6733c7a00eb7)")
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

    public static Location readLocation(String location, int port,
                                        RestTemplate rest) {
        ParameterizedTypeReference<Location> responseType = new ParameterizedTypeReference<Location>() {};
        ResponseEntity<Location> response = rest.exchange(location,
                HttpMethod.GET, getAuthorizedEntity(port, Location.class), responseType);
        return response.getBody();
    }

    @Test
    public void testCreateReadDeleteLocation() throws Exception {
        try {
            String location = createLocation(baseUrl, port, rest,
                    "CCIT",
                    "Calgary Centre for Innovative Technologies",
                    -92.041213, 30.218805);

            // TODO: Check for navigation links to related entities

            // Read newly created Location
            ParameterizedTypeReference<Location> locResponseType = new ParameterizedTypeReference<Location>() {
            };
            ResponseEntity<Location> locResponse = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Location.class), locResponseType);
            Location l = locResponse.getBody();
            assertEquals("CCIT", l.getName());
            assertEquals("Calgary Centre for Innovative Technologies",
                    l.getDescription());
            assertEquals(new MimeType("application/vnd.geo+json").toString(),
                    l.getEncodingType().toString());
            String locationStr = "{" +
                    "\"type\":\n" +
                    "  \"Feature\",\n" +
                    "          \"geometry\": {\n" +
                    "            \"type\":\n" +
                    "  \"Point\",\n" +
                    "           \n" +
                    "  \"coordinates\": [-92.041213,30.218805]\n" +
                    "          }\n" +
                    "        }\n";
            GeoJsonObject loc = new ObjectMapper().readValue(new StringReader(locationStr), GeoJsonObject.class);
            assertEquals(loc, l.getLocation());

            // Read newly created Location, this time examine the raw JSON that comes back,
            //   so that we can verify that the metadata are correct
            ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {
            };
            ResponseEntity<String> responseStr;
            responseStr = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            String s = responseStr.getBody();
            JsonReader r = Json.createReader(new StringReader(s));
            JsonObject o = r.readObject();
            assertEquals(o.getString(ANNO_IOT_ID), l.getId().toString());
            assertEquals(o.getString(ANNO_IOT_SELF_LINK), location);
            // TODO: Compare navigation links for Things and HistoricalLocations

            // Fetch all Locations
            String allLocationsUrl = apiRootUrl + "Locations";
            responseStr = rest.exchange(allLocationsUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            o = r.readObject();
            assertEquals(1, o.getInt(ANNO_COLLECTION_COUNT));
            JsonArray a = o.getJsonArray(COLLECTION_ATTR);
            JsonObject obj = a.getJsonObject(0);
            String entityUrl = obj.getString(ANNO_IOT_SELF_LINK);
            assertEquals(location, entityUrl);

            // Delete Location
            ParameterizedTypeReference<JsonObject> responseType = new ParameterizedTypeReference<JsonObject>() {
            };
            ResponseEntity<JsonObject> response = rest.exchange(location,
                    HttpMethod.DELETE, getAuthorizedEntity(port, JsonObject.class),
                    responseType);
            assertEquals(204, response.getStatusCodeValue());
            // Try to get deleted Location (this should fail with a 404)
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
    public void testCreateReadLocationPaging() throws Exception {
        assertEquals(Integer.valueOf(3), MAX_REQUEST_PAGE_SIZE);
        try {
            // Create first location
            String location = createLocation(baseUrl, port, rest,
                    "CCIT",
                    "Calgary Centre for Innovative Technologies",
                    -92.04, 30.21);

            // Create second location
            String location2 = createLocation(baseUrl, port, rest,
                    "CCIT 2",
                    "Calgary Centre for Innovative Technologies 2",
                    -94.04, 32.21);

            // Create first location
            String location3 = createLocation(baseUrl, port, rest,
                    "CCIT 3",
                    "Calgary Centre for Innovative Technologies 3",
                    -96.04, 34.21);

            // Create first location
            String location4 = createLocation(baseUrl, port, rest,
                    "CCIT 4",
                    "Calgary Centre for Innovative Technologies 4",
                    -98.04, 36.21);

            // Fetch all Locations (should be 4 across 2 pages)
            String allLocationsUrl = apiRootUrl + "Locations";
            ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {};
            ResponseEntity<String> responseStr = rest.exchange(allLocationsUrl,
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
    public void testReadUnknownLocation() throws Exception {
        try {
            String uuid = UUIDs.timeBased().toString();
            String url = baseUrl + "(" + uuid +")";
            ParameterizedTypeReference<Location> responseType = new ParameterizedTypeReference<Location>() {
            };
            ResponseEntity<Location> response = rest.exchange(url,
                    HttpMethod.GET, getAuthorizedEntity(port, Location.class),
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
    public void testReadMalformedIdLocation() throws Exception {
        try {
            String url = baseUrl + "(thisisnotauuid)";
            ParameterizedTypeReference<Location> responseType = new ParameterizedTypeReference<Location>() {
            };
            ResponseEntity<Location> response = rest.exchange(url,
                    HttpMethod.GET, getAuthorizedEntity(port, Location.class),
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
    public void testPatchLocation() throws Exception {
        try {
            String location = createLocation(baseUrl, port, rest,
                    "CCIT",
                    "Calgary Centre for Innovative Technologies",
                    -92.041213, 30.218805);

            // Read newly created Location
            ParameterizedTypeReference<Location> responseType = new ParameterizedTypeReference<Location>() {
            };
            ResponseEntity<Location> response = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Location.class), responseType);
            assertEquals(200, response.getStatusCodeValue());
            Location l = response.getBody();

            // Update the name of the Location
            String newLocationStr = "{" +
                    "\"type\":\n" +
                    "  \"Feature\",\n" +
                    "          \"geometry\": {\n" +
                    "            \"type\":\n" +
                    "  \"Point\",\n" +
                    "           \n" +
                    "  \"coordinates\": [42,42]\n" +
                    "          }\n" +
                    "        }\n";
            String updateStr = "{" +
                    "\"name\":\"test location 2\"," +
                    "\"location\":" + newLocationStr +
                    "}";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<>(updateStr, headers);
            ParameterizedTypeReference<JsonObject> responseTypeJson = new ParameterizedTypeReference<JsonObject>() {
            };
            response = rest.exchange(location,
                    HttpMethod.PATCH, entity,
                    responseType);
            assertEquals(204, response.getStatusCodeValue());

            // Get the updated Location
            response = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Location.class), responseType);
            assertEquals(200, response.getStatusCodeValue());
            Location l2 = response.getBody();
            assertEquals("test location 2", l2.getName());
            assertEquals("Calgary Centre for Innovative Technologies", l2.getDescription());
            assertEquals(new MimeType("application/vnd.geo+json").toString(),
                    l2.getEncodingType().toString());
            GeoJsonObject loc = new ObjectMapper().readValue(new StringReader(newLocationStr), GeoJsonObject.class);
            assertEquals(loc, l2.getLocation());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
        }
    }

    @Test
    public void testPatchUnknownLocation() throws Exception {
        try {
            String uuid = UUIDs.timeBased().toString();
            String url = baseUrl + "(" + uuid +")";
            // Build request entity
            String requestStr = "{\"name\":\"test location 2\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<>(requestStr, headers);
            ParameterizedTypeReference<Location> responseType = new ParameterizedTypeReference<Location>() {
            };
            ResponseEntity<Location> response = rest.exchange(url, HttpMethod.PATCH, entity, responseType);
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
    public void testPatchMalformedLocation() throws Exception {
        try {
            String url = baseUrl + "(thisisnotauuid)";
            // Build request entity
            String requestStr = "{\"name\":\"test location 2\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
            ParameterizedTypeReference<Location> responseType = new ParameterizedTypeReference<Location>() {
            };
            ResponseEntity<Location> response = rest.exchange(url, HttpMethod.PATCH, entity, responseType);
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
    public void testPutLocation() throws Exception {
        try {
            String location = createLocation(baseUrl, port, rest,
                    "CCIT",
                    "Calgary Centre for Innovative Technologies",
                    -92.041213, 30.218805);

            // Read newly created Location
            ParameterizedTypeReference<Location> responseType = new ParameterizedTypeReference<Location>() {
            };
            ResponseEntity<Location> response = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Location.class), responseType);
            assertEquals(200, response.getStatusCodeValue());
            Location l = response.getBody();

            // Update the Location
            String updateStr = "{\n" +
                    "        \"name\": \"CCIT 2\",\n" +
                    "        \"description\":\n" +
                    "  \"Calgary Centre for Innovative Technologies 2\",\n" +
                    "\n" +
                    "        \"encodingType\":\n" +
                    "  \"application/vnd.geo+json\",\n" +
                    "        \"location\": {\n" +
                    "          \"type\":\n" +
                    "  \"Feature\",\n" +
                    "          \"geometry\": {\n" +
                    "            \"type\":\n" +
                    "  \"Point\",\n" +
                    "           \n" +
                    "  \"coordinates\": [42,42]\n" +
                    "          }\n" +
                    "        }\n" +
                    "      }";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<String>(updateStr, headers);
            response = rest.exchange(location,
                    HttpMethod.PUT, entity,
                    responseType);
            assertEquals(204, response.getStatusCodeValue());

            // Get the updated Location
            response = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Location.class), responseType);
            Location l2 = response.getBody();
            assertEquals("CCIT 2", l2.getName());
            assertEquals("Calgary Centre for Innovative Technologies 2", l2.getDescription());
            assertEquals(new MimeType("application/vnd.geo+json").toString(),
                    l2.getEncodingType().toString());
            String locationStr = "{" +
                    "\"type\":\n" +
                    "  \"Feature\",\n" +
                    "          \"geometry\": {\n" +
                    "            \"type\":\n" +
                    "  \"Point\",\n" +
                    "           \n" +
                    "  \"coordinates\": [42,42]\n" +
                    "          }\n" +
                    "        }\n";
            GeoJsonObject loc = new ObjectMapper().readValue(new StringReader(locationStr), GeoJsonObject.class);
            assertEquals(loc, l2.getLocation());

            // Test incomplete request (should return a 400)
            try {
                String patchStr = "{\"name\":\"what's in a name?\"}";
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
    public void testPutUnknownLocation() throws Exception {
        try {
            String uuid = UUIDs.timeBased().toString();
            String url = baseUrl + "(" + uuid +")";
            // Build request entity
            String requestStr = "{\n" +
                    "        \"name\": \"CCIT\",\n" +
                    "        \"description\":\n" +
                    "  \"Calgary Centre for Innovative Technologies\",\n" +
                    "\n" +
                    "        \"encodingType\":\n" +
                    "  \"application/vnd.geo+json\",\n" +
                    "        \"location\": {\n" +
                    "          \"type\":\n" +
                    "  \"Feature\",\n" +
                    "          \"geometry\": {\n" +
                    "            \"type\":\n" +
                    "  \"Point\",\n" +
                    "           \n" +
                    "  \"coordinates\": [10,10]\n" +
                    "          }\n" +
                    "        }\n" +
                    "      }";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
            ParameterizedTypeReference<Location> responseType = new ParameterizedTypeReference<Location>() {
            };
            ResponseEntity<Location> response = rest.exchange(url, HttpMethod.PUT, entity, responseType);
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
    public void testPutMalformedLocation() throws Exception {
        try {
            String url = baseUrl + "(thisisnotauuid)";
            // Build request entity
            String requestStr = "{\n" +
                    "        \"name\": \"CCIT\",\n" +
                    "        \"description\":\n" +
                    "  \"Calgary Centre for Innovative Technologies\",\n" +
                    "\n" +
                    "        \"encodingType\":\n" +
                    "  \"application/vnd.geo+json\",\n" +
                    "        \"location\": {\n" +
                    "          \"type\":\n" +
                    "  \"Feature\",\n" +
                    "          \"geometry\": {\n" +
                    "            \"type\":\n" +
                    "  \"Point\",\n" +
                    "           \n" +
                    "  \"coordinates\": [10,10]\n" +
                    "          }\n" +
                    "        }\n" +
                    "      }";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
            ParameterizedTypeReference<Location> responseType = new ParameterizedTypeReference<Location>() {
            };
            ResponseEntity<Location> response = rest.exchange(url, HttpMethod.PUT, entity, responseType);
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
    public void testDeleteUnknownLocation() throws Exception {
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
    public void testDeleteMalformedIdLocation() throws Exception {
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
