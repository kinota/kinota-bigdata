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

import com.cgi.kinota.commons.domain.HistoricalLocation;
import com.cgi.kinota.commons.domain.Location;
import com.cgi.kinota.commons.domain.Thing;

import com.cgi.kinota.rest.cassandra.Application;
import com.cgi.kinota.rest.cassandra.interfaces.rest.Utility;
import com.cgi.kinota.rest.cassandra.CassandraThingTestBase;
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

import javax.activation.MimeType;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.cgi.kinota.commons.Constants.*;
import static com.cgi.kinota.commons.geo.Utility.createGeoJsonPoint;
import static org.junit.Assert.*;

/**
 * Created by bmiles on 1/4/17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {Application.class, SpringDataCassandraConfig.class})
public class ThingResourceTest extends CassandraThingTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ThingResourceTest.class);

    public static String createThing(String baseUrl,
                                     int port,
                                     RestTemplate rest,
                                     String name,
                                     String description,
                                     Map<String, String> properties,
                                     String locationUuid) {
        // Build request entity
        StringBuffer propertiesStrBuffer = new StringBuffer();
        Iterator<Map.Entry<String, String>> i = properties.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, String> e = i.next();
            propertiesStrBuffer.append("\"" + e.getKey() + "\":\"" + e.getValue() + "\"");
            if (i.hasNext()) {
                propertiesStrBuffer.append(",");
            }
        }
        String propertiesStr = propertiesStrBuffer.toString();
        String requestStr = "{";
        if (locationUuid != null) {
            requestStr += "\"Location\":{\"@iot.id\":\"" + locationUuid + "\"},";
        }
        requestStr += "\"name\":\"" + name + "\"," +
                "\"description\":\"" + description + "\"," +
                "\"properties\":" + "{" + propertiesStr + "}" +
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
        // (e.g. "http://localhost:55704/v1.0/Things(b23e2d40-d381-11e6-ba14-6733c7a00eb7)")
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

    public static void updateThingLocation(String location, String newLocationUuid,
                                           int port, RestTemplate rest) {
        ParameterizedTypeReference<JsonObject> responseType = new ParameterizedTypeReference<JsonObject>() {};
        String requestStr = "{\"Location\":{\"@iot.id\":\"" + newLocationUuid + "\"}" +
                "}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addTokenToHeader(port, headers);
        HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
        ResponseEntity<JsonObject> response = rest.exchange(location,
                HttpMethod.PATCH, entity,
                responseType);
        assertEquals(204, response.getStatusCodeValue());
    }

    public static Thing readThing(String location, int port,
                                           RestTemplate rest) {
        ParameterizedTypeReference<Thing> responseType = new ParameterizedTypeReference<Thing>() {};
        ResponseEntity<Thing> response = rest.exchange(location,
                HttpMethod.GET, getAuthorizedEntity(port, Thing.class), responseType);
        return response.getBody();
    }

    @Test
    public void testCreateReadDeleteThing() throws Exception {
        try {
            Map<String, String> properties = new LinkedHashMap<String, String>();
            properties.put("foo", "bar");
            properties.put("baz", "qux");
            String location = createThing(baseUrl, port, rest,
                    "test thing 1", "A thing for testing.", properties,
                    null);

            // TODO: Check for navigation links to related entities

            // Read newly created Thing
            ParameterizedTypeReference<Thing> thingResponseType = new ParameterizedTypeReference<Thing>() {};
            ResponseEntity<Thing> thingResponse = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Thing.class), thingResponseType);
            Thing t = thingResponse.getBody();
            assertEquals("test thing 1", t.getName());
            assertEquals("A thing for testing.", t.getDescription());
            Map<String, String> p = t.getProperties();
            assertEquals("bar", p.get("foo"));
            assertEquals("qux", p.get("baz"));

            // Read newly created Thing, this time examine the raw JSON that comes back,
            //   so that we can verify that the metadata are correct
            ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {};
            ResponseEntity<String> responseStr;
            responseStr = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            String s = responseStr.getBody();
            JsonReader r = Json.createReader(new StringReader(s));
            JsonObject o = r.readObject();
            assertEquals(o.getString(ANNO_IOT_ID), t.getId().toString());
            assertEquals(o.getString(ANNO_IOT_SELF_LINK), location);
            // TODO: Compare navigation links for Locations and Datastreams

            // Fetch all Things
            String icelandicParliamentUrl = apiRootUrl + "Things";
            responseStr = rest.exchange(icelandicParliamentUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            o = r.readObject();
            assertEquals(1, o.getInt(ANNO_COLLECTION_COUNT));
            JsonArray a = o.getJsonArray(COLLECTION_ATTR);
            JsonObject obj = a.getJsonObject(0);
            String entityUrl = obj.getString(ANNO_IOT_SELF_LINK);
            assertEquals(location, entityUrl);

            // Delete Thing
            ParameterizedTypeReference<JsonObject> responseType = new ParameterizedTypeReference<JsonObject>() {};
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
    public void testCreateReadThingPaging() throws Exception {
        assertEquals(Integer.valueOf(3), MAX_REQUEST_PAGE_SIZE);
        try {
            // Create first thing
            Map<String, String> properties = new LinkedHashMap<String, String>();
            properties.put("foo", "bar");
            properties.put("baz", "qux");
            String location = createThing(baseUrl, port, rest,
                    "test thing 1", "A thing for testing.", properties,
                    null);

            // Create second thing
            properties = new LinkedHashMap<String, String>();
            properties.put("foo1", "bar1");
            properties.put("baz1", "qux1");
            String location2 = createThing(baseUrl, port, rest,
                    "test thing 2", "A thing for testing. 2", properties,
                    null);

            // Create third thing
            properties = new LinkedHashMap<String, String>();
            properties.put("foo2", "bar2");
            properties.put("baz2", "qux2");
            String location3 = createThing(baseUrl, port, rest,
                    "test thing 3", "A thing for testing. 3", properties,
                    null);

            // Create fourth thing
            properties = new LinkedHashMap<String, String>();
            properties.put("foo3", "bar3");
            properties.put("baz3", "qux3");
            String location4 = createThing(baseUrl, port, rest,
                    "test thing 4", "A thing for testing. 4", properties,
                    null);

            // Fetch all Things (should be 4 across 2 pages)
            String icelandicParliamentUrl = apiRootUrl + "Things";
            ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {};
            ResponseEntity<String> responseStr = rest.exchange(icelandicParliamentUrl,
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
    public void testThingLocationChangeHistoricalLocationPaging() throws Exception {
        assertEquals(Integer.valueOf(3), MAX_REQUEST_PAGE_SIZE);
        try {
            // Create first Location
            org.geojson.Point p = createGeoJsonPoint(-92.041213, 30.218805);
            String location1Url = LocationResourceTest.createLocation(generateBaseUrl(port, LocationResourceTest.URL_PATH_BASE),
                    port,
                    rest,
                    "CCIT",
                    "Calgary Centre for Innovative Technologies",
                    p.getCoordinates().getLongitude(), p.getCoordinates().getLatitude());
            String location1Uuid = Utility.extractUuidForEntityUrl(location1Url);

            // Create second Location
            org.geojson.Point p2 = createGeoJsonPoint(-93.041213, 31.218805);
            String location2Url = LocationResourceTest.createLocation(generateBaseUrl(port, LocationResourceTest.URL_PATH_BASE),
                    port,
                    rest,
                    "CCIT 2",
                    "Calgary Centre for Innovative Technologies 2",
                    p2.getCoordinates().getLongitude(), p2.getCoordinates().getLatitude());
            String location2Uuid = Utility.extractUuidForEntityUrl(location2Url);

            // Create third Location
            org.geojson.Point p3 = createGeoJsonPoint(-94.041213, 32.218805);
            String location3Url = LocationResourceTest.createLocation(generateBaseUrl(port, LocationResourceTest.URL_PATH_BASE),
                    port,
                    rest,
                    "CCIT 3",
                    "Calgary Centre for Innovative Technologies 3",
                    p3.getCoordinates().getLongitude(), p3.getCoordinates().getLatitude());
            String location3Uuid = Utility.extractUuidForEntityUrl(location3Url);

            // Create fourth Location
            org.geojson.Point p4 = createGeoJsonPoint(-95.041213, 33.218805);
            String location4Url = LocationResourceTest.createLocation(generateBaseUrl(port, LocationResourceTest.URL_PATH_BASE),
                    port,
                    rest,
                    "CCIT 4",
                    "Calgary Centre for Innovative Technologies 4",
                    p4.getCoordinates().getLongitude(), p4.getCoordinates().getLatitude());
            String location4Uuid = Utility.extractUuidForEntityUrl(location4Url);

            // Create fourth Location
            org.geojson.Point p5 = createGeoJsonPoint(-96.041213, 34.218805);
            String location5Url = LocationResourceTest.createLocation(generateBaseUrl(port, LocationResourceTest.URL_PATH_BASE),
                    port,
                    rest,
                    "CCIT 5",
                    "Calgary Centre for Innovative Technologies 5",
                    p5.getCoordinates().getLongitude(), p5.getCoordinates().getLatitude());
            String location5Uuid = Utility.extractUuidForEntityUrl(location5Url);

            // Create a Thing with a location
            Map<String, String> properties = new LinkedHashMap<String, String>();
            properties.put("foo", "bar");
            properties.put("baz", "qux");
            String thingUrl = ThingResourceTest.createThing(generateBaseUrl(port, ThingResourceTest.URL_PATH_BASE), port, rest,
                    "test thing 1", "A thing for testing.", properties,
                    location1Uuid);
            assertNotNull(thingUrl);

            // Update Thing location a few times to create historical locations
            ThingResourceTest.updateThingLocation(thingUrl, location2Uuid, port, rest);
            ThingResourceTest.updateThingLocation(thingUrl, location3Uuid, port, rest);
            ThingResourceTest.updateThingLocation(thingUrl, location4Uuid, port, rest);
            ThingResourceTest.updateThingLocation(thingUrl, location5Uuid, port, rest);

            // Fetch all HistoricalLocations (should be 5 across 2 pages)
            String allHistoricalLocationsUrl = apiRootUrl + "HistoricalLocations";
            ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {};
            ResponseEntity<String> responseStr = rest.exchange(allHistoricalLocationsUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            String s = responseStr.getBody();
            JsonReader r = Json.createReader(new StringReader(s));
            JsonObject obj = r.readObject();
            assertEquals(5, obj.getInt(ANNO_COLLECTION_COUNT));

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
            assertEquals(5, obj.getInt(ANNO_COLLECTION_COUNT));
            a = obj.getJsonArray(COLLECTION_ATTR);
            assertEquals(Integer.valueOf(2), Integer.valueOf(a.size()));

            boolean exceptionThrown = false;
            try {
                nextLink = obj.getString(ANNO_IOT_NEXT_LINK);
            } catch (NullPointerException npe) {
                exceptionThrown = true;
            }
            assertTrue(exceptionThrown);

            // Fetch HistoricalLocations via Thing's link (should be 5 across 2 pages)
            String histLocRelUrl = thingUrl + "/" + HistoricalLocation.NAME_PLURAL;
            responseStr = rest.exchange(histLocRelUrl,
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
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            obj = r.readObject();
            a = obj.getJsonArray(COLLECTION_ATTR);
            assertEquals(Integer.valueOf(2), Integer.valueOf(a.size()));

            exceptionThrown = false;
            try {
                nextLink = obj.getString(ANNO_IOT_NEXT_LINK);
            } catch (NullPointerException npe) {
                exceptionThrown = true;
            }
            assertTrue(exceptionThrown);

            // Fetch HistoricalLocations $ref via Thing's link (should be 4 across 2 pages)
            String histLocRefUrl = thingUrl + "/" + HistoricalLocation.NAME_PLURAL + "/$ref";
            responseStr = rest.exchange(histLocRefUrl,
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
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            obj = r.readObject();
            a = obj.getJsonArray(COLLECTION_ATTR);
            assertEquals(Integer.valueOf(2), Integer.valueOf(a.size()));

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

    @Test
    public void testMultipleThingsSingleLocationPaging() throws Exception {
        assertEquals(Integer.valueOf(3), MAX_REQUEST_PAGE_SIZE);
        try {
            String locationBaseUrl = generateBaseUrl(port, LocationResourceTest.URL_PATH_BASE);
            String locationUrl = LocationResourceTest.createLocation(locationBaseUrl, port, rest,
                    "CCIT",
                    "Calgary Centre for Innovative Technologies",
                    -92.041213, 30.218805);
            String locationUuid = Utility.extractUuidForEntityUrl(locationUrl);

            // Create first Thing with location
            Map<String, String> properties = new LinkedHashMap<String, String>();
            properties.put("foo", "bar");
            properties.put("baz", "qux");
            String location = createThing(baseUrl, port, rest,
                    "test thing 1", "A thing for testing.", properties,
                    locationUuid);

            // Create second Thing with location
            properties = new LinkedHashMap<String, String>();
            properties.put("foo2", "bar2");
            properties.put("baz2", "qux2");
            String location2 = createThing(baseUrl, port, rest,
                    "test thing 2", "A thing for testing, 2.", properties,
                    locationUuid);

            // Create third Thing with location
            properties = new LinkedHashMap<String, String>();
            properties.put("foo3", "bar3");
            properties.put("baz3", "qux3");
            String location3 = createThing(baseUrl, port, rest,
                    "test thing 3", "A thing for testing, 3.", properties,
                    locationUuid);

            // Create fourth Thing with location
            properties = new LinkedHashMap<String, String>();
            properties.put("foo4", "bar4");
            properties.put("baz4", "qux4");
            String location4 = createThing(baseUrl, port, rest,
                    "test thing 4", "A thing for testing, 4.", properties,
                    locationUuid);

            // Fetch Things via Location's link (should be 4 across 2 pages)
            ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {};
            String histLocRelUrl = locationUrl + "/" + Thing.NAME_PLURAL;
            ResponseEntity<String> responseStr = rest.exchange(histLocRelUrl,
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
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            obj = r.readObject();
            a = obj.getJsonArray(COLLECTION_ATTR);
            assertEquals(Integer.valueOf(1), Integer.valueOf(a.size()));

            boolean exceptionThrown = false;
            try {
                nextLink = obj.getString(ANNO_IOT_NEXT_LINK);
            } catch (NullPointerException npe) {
                exceptionThrown = true;
            }
            assertTrue(exceptionThrown);

            // Fetch Things $ref via Location's link (should be 4 across 2 pages)
            String histLocRefUrl = locationUrl + "/" + Thing.NAME_PLURAL + "/$ref";
            responseStr = rest.exchange(histLocRefUrl,
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
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            obj = r.readObject();
            a = obj.getJsonArray(COLLECTION_ATTR);
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

    @Test
    public void testCreateResourceWithExistingLocation() throws Exception {
        try {
            String locationBaseUrl = generateBaseUrl(port, LocationResourceTest.URL_PATH_BASE);
            String origLocationUrl = LocationResourceTest.createLocation(locationBaseUrl, port, rest,
                    "CCIT",
                    "Calgary Centre for Innovative Technologies",
                    -92.041213, 30.218805);
            String locationUuid = Utility.extractUuidForEntityUrl(origLocationUrl);

            // Create Thing with location
            Map<String, String> properties = new LinkedHashMap<String, String>();
            properties.put("foo", "bar");
            properties.put("baz", "qux");
            String location = createThing(baseUrl, port, rest,
                    "test thing 1", "A thing for testing.", properties,
                    locationUuid);

            // Read newly created Thing
            ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {
            };
            ResponseEntity<String> responseStr = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            String s = responseStr.getBody();
            JsonReader r = Json.createReader(new StringReader(s));
            JsonObject o = r.readObject();
            assertEquals(o.getString(ANNO_IOT_SELF_LINK), location);
            String locationsListRel = o.getString(Location.NAV_LINK);
            assertNotNull(locationsListRel);
            // Fetch Location via the Thing's navigation link to it
            String locationsUrl = apiRootUrl + locationsListRel;
            responseStr = rest.exchange(locationsUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            o = r.readObject();
            JsonArray a = o.getJsonArray(COLLECTION_ATTR);
            assertTrue(a.size() == 1);
            JsonObject l = a.getJsonObject(0);
            String locationUrl = l.getString(ANNO_IOT_SELF_LINK);
            assertEquals(origLocationUrl, locationUrl);
            assertEquals("CCIT", l.getString("name"));
            assertEquals("Calgary Centre for Innovative Technologies",
                    l.getString("description"));
            assertEquals(new MimeType("application/vnd.geo+json").toString(),
                    l.getString("encodingType"));
            // Save for later so we can fetch the Thing by the Location Thing navLink
            String thingsListRel = l.getString(Thing.NAV_LINK_MANY);
            // Fetch location $ref
            String locationsRefUrl = locationsUrl + ASSOC_LINK_ADDY;
            responseStr = rest.exchange(locationsRefUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            o = r.readObject();
            a = o.getJsonArray(COLLECTION_ATTR);
            assertTrue(a.size() == 1);
            l = a.getJsonObject(0);
            String selfLink = l.getString(ANNO_IOT_SELF_LINKS);
            assertEquals(origLocationUrl, selfLink);

            // Fetch Thing via the Locations's navigation link to it
            String thingsUrl = apiRootUrl + thingsListRel;
            responseStr = rest.exchange(thingsUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            o = r.readObject();
            a = o.getJsonArray(COLLECTION_ATTR);
            assertTrue(a.size() == 1);
            JsonObject t = a.getJsonObject(0);
            String relatedThingUrl = t.getString(ANNO_IOT_SELF_LINK);
            assertEquals(location, relatedThingUrl);
            assertEquals("test thing 1", t.getString("name"));

            // Fetch location $ref
            String thingsRefUrl = thingsUrl + ASSOC_LINK_ADDY;
            responseStr = rest.exchange(thingsRefUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            o = r.readObject();
            a = o.getJsonArray(COLLECTION_ATTR);
            assertTrue(a.size() == 1);
            t = a.getJsonObject(0);
            selfLink = t.getString(ANNO_IOT_SELF_LINKS);
            assertEquals(location, selfLink);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
        }
    }

    @Test
    public void testUpdateThingWithExistingLocation() throws Exception {
        try {
            // Create location
            String locationBaseUrl = generateBaseUrl(port, LocationResourceTest.URL_PATH_BASE);
            String origLocationUrl = LocationResourceTest.createLocation(locationBaseUrl, port, rest,
                    "CCIT",
                    "Calgary Centre for Innovative Technologies",
                    -92.041213, 30.218805);
            String locationUuid = Utility.extractUuidForEntityUrl(origLocationUrl);

            // Create Thing with location
            Map<String, String> properties = new LinkedHashMap<String, String>();
            properties.put("foo", "bar");
            properties.put("baz", "qux");
            String location = createThing(baseUrl, port, rest,
                    "test thing 1", "A thing for testing.", properties,
                    locationUuid);

            // Read newly created Thing
            ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {
            };
            ResponseEntity<String> responseStr = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            String s = responseStr.getBody();
            JsonReader r = Json.createReader(new StringReader(s));
            JsonObject o = r.readObject();
            assertEquals(o.getString(ANNO_IOT_SELF_LINK), location);
            String locationsListRel = o.getString(Location.NAV_LINK);
            assertNotNull(locationsListRel);
            String hlListRel = o.getString(HistoricalLocation.NAV_LINK);
            assertNotNull(hlListRel);

            // Fetch HistoricalLocation via the Thing's navigation link to it
            String hlUrl = apiRootUrl + hlListRel;
            responseStr = rest.exchange(hlUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            o = r.readObject();
            JsonArray a = o.getJsonArray(COLLECTION_ATTR);
            assertTrue(a.size() == 1);
            JsonObject hl = a.getJsonObject(0);
            String hlLocListRel = hl.getString(Location.NAV_LINK);
            assertNotNull(hlLocListRel);

            // Fetch Location via the Thing's navigation link to it
            String locationsUrl = apiRootUrl + locationsListRel;
            responseStr = rest.exchange(locationsUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            o = r.readObject();
            a = o.getJsonArray(COLLECTION_ATTR);
            assertTrue(a.size() == 1);
            JsonObject l = a.getJsonObject(0);
            String locationUrl = l.getString(ANNO_IOT_SELF_LINK);
            assertEquals(origLocationUrl, locationUrl);

            // Create a new Location
            String newLocationUrl = LocationResourceTest.createLocation(locationBaseUrl, port, rest,
                    "CCIT 2",
                    "Another Calgary Centre for Innovative Technologies",
                    -92.046929, 30.227787);
            String newLocationUuid = Utility.extractUuidForEntityUrl(newLocationUrl);

            // Update Thing with new Location
            updateThingLocation(location, newLocationUuid, port, rest);

            // Fetch updated Location via the Thing's navigation link to it
            responseStr = rest.exchange(locationsUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            o = r.readObject();
            a = o.getJsonArray(COLLECTION_ATTR);
            assertTrue(a.size() == 1);
            l = a.getJsonObject(0);
            String updatedLocationUrl = l.getString(ANNO_IOT_SELF_LINK);
            assertEquals(newLocationUrl, updatedLocationUrl);

            // Check to see if locationUrl is associated with an
            //   HistoricalLocation of this Thing
            // Fetch HistoricalLocation via the Thing's navigation link to it
            hlUrl = apiRootUrl + hlListRel;
            responseStr = rest.exchange(hlUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            o = r.readObject();
            a = o.getJsonArray(COLLECTION_ATTR);
            assertTrue(a.size() == 2);

            boolean match = false;
            String hlLocUrl = null;
            for (int i = 0; i < a.size(); i++) {
                // For each HistoricalLocation, fetch associated Locations
                JsonObject tmpHl = a.getJsonObject(i);
                hlLocListRel = tmpHl.getString(Location.NAV_LINK);
                assertNotNull(hlLocListRel);
                hlLocUrl = apiRootUrl + hlLocListRel;
                responseStr = rest.exchange(hlLocUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
                String tmpS = responseStr.getBody();
                JsonReader tmpR = Json.createReader(new StringReader(tmpS));
                JsonObject tmpO = tmpR.readObject();
                JsonArray tmpA = tmpO.getJsonArray(COLLECTION_ATTR);
                assertTrue(tmpA.size() == 1);
                JsonObject tmpL = tmpA.getJsonObject(0);
                String entityUrl = tmpL.getString(ANNO_IOT_SELF_LINK);
                if (locationUrl.equals(entityUrl)) {
                    match = true;
                    break;
                }
            }
            assertTrue(match);

            // Fetch all HistoricalLocations
            assertNotNull(hlLocUrl);
            String expectedHlUrl = hlLocUrl.substring(0, hlLocUrl.lastIndexOf('/'));
            String allhistLocUrl = apiRootUrl + "HistoricalLocations";
            responseStr = rest.exchange(allhistLocUrl,
                    HttpMethod.GET, getAuthorizedEntity(port, String.class), responseTypeStr);
            s = responseStr.getBody();
            r = Json.createReader(new StringReader(s));
            o = r.readObject();
            assertEquals(2, o.getInt(ANNO_COLLECTION_COUNT));
            a = o.getJsonArray(COLLECTION_ATTR);
            assertEquals(2, a.size());

            match = false;
            for (int i = 0; i < a.size(); i++) {
                // Ensure new expected HistoricalLocation is among those in the HistoricalLocation list
                JsonObject tmpO = a.getJsonObject(i);
                String entityUrl = tmpO.getString(ANNO_IOT_SELF_LINK);
                if (expectedHlUrl.equals(entityUrl)) {
                    match = true;
                    break;
                }
            }
            assertTrue(match);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
        }
    }

    @Test
    public void testReadUnknownThing() throws Exception {
        try {
            String uuid = UUIDs.timeBased().toString();
            String url = baseUrl + "(" + uuid +")";
            ParameterizedTypeReference<Thing> responseType = new ParameterizedTypeReference<Thing>() {
            };
            ResponseEntity<Thing> response = rest.exchange(url,
                    HttpMethod.GET, getAuthorizedEntity(port, Thing.class),
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
    public void testReadMalformedIdThing() throws Exception {
        try {
            String url = baseUrl + "(thisisnotauuid)";
            ParameterizedTypeReference<Thing> responseType = new ParameterizedTypeReference<Thing>() {
            };
            ResponseEntity<Thing> response = rest.exchange(url,
                    HttpMethod.GET, getAuthorizedEntity(port, Thing.class),
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
    public void testPatchThing() throws Exception {
        try {
            Map<String, String> properties = new LinkedHashMap<String, String>();
            properties.put("foo", "bar");
            properties.put("baz", "qux");
            String location = createThing(baseUrl, port, rest,
                    "test thing 1", "A thing for testing.", properties,
                    null);

            // Read newly created Thing
            ParameterizedTypeReference<Thing> responseType = new ParameterizedTypeReference<Thing>() {
            };
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            ResponseEntity<Thing> response = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Thing.class), responseType);
            assertEquals(200, response.getStatusCodeValue());
            Thing t = response.getBody();

            // Update the name of the Thing
            String updateStr = "{\"name\":\"test thing 2\"}";
            HttpEntity<String> entity = new HttpEntity<String>(updateStr, headers);
            response = rest.exchange(location,
                    HttpMethod.PATCH, entity,
                    responseType);
            assertEquals(204, response.getStatusCodeValue());

            // Get the updated Thing
            response = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Thing.class), responseType);
            Thing t2 = response.getBody();
            assertEquals("test thing 2", t2.getName());
            assertEquals("A thing for testing.", t2.getDescription());
            Map<String, String> p = t2.getProperties();
            assertEquals("bar", p.get("foo"));
            assertEquals("qux", p.get("baz"));

            // Update the description of the Thing
            updateStr = "{\"description\":\"Another thing for testing.\"}";
            entity = new HttpEntity<String>(updateStr, headers);
            response = rest.exchange(location,
                    HttpMethod.PATCH, entity,
                    responseType);
            assertEquals(204, response.getStatusCodeValue());

            // Get the updated Thing
            response = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Thing.class), responseType);
            Thing t3 = response.getBody();
            assertEquals("test thing 2", t3.getName());
            assertEquals("Another thing for testing.", t3.getDescription());
            p = t3.getProperties();
            assertEquals("bar", p.get("foo"));
            assertEquals("qux", p.get("baz"));

            // Update the properties of the Thing
            updateStr = "{\"properties\":{\"baz\": \"\", \"foo\": \"qux\", \"blef\": \"baz\"}}";
            entity = new HttpEntity<String>(updateStr, headers);
            response = rest.exchange(location,
                    HttpMethod.PATCH, entity,
                    responseType);
            assertEquals(204, response.getStatusCodeValue());

            // Get the updated Thing
            response = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Thing.class), responseType);
            Thing t4 = response.getBody();
            assertEquals("test thing 2", t4.getName());
            assertEquals("Another thing for testing.", t4.getDescription());
            p = t4.getProperties();
            assertEquals(null, p.get("baz"));
            assertEquals("qux", p.get("foo"));
            assertEquals("baz", p.get("blef"));

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
        }
    }

    @Test
    public void testPatchUnknownThing() throws Exception {
        try {
            String uuid = UUIDs.timeBased().toString();
            String url = baseUrl + "(" + uuid +")";
            // Build request entity
            String requestStr = "{\"name\":\"test thing 2\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
            ParameterizedTypeReference<Thing> responseType = new ParameterizedTypeReference<Thing>() {
            };
            ResponseEntity<Thing> response = rest.exchange(url, HttpMethod.PATCH, entity, responseType);
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
    public void testPatchMalformedThing() throws Exception {
        try {
            String url = baseUrl + "(thisisnotauuid)";
            // Build request entity
            String requestStr = "{\"name\":\"test thing 2\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
            ParameterizedTypeReference<Thing> responseType = new ParameterizedTypeReference<Thing>() {
            };
            ResponseEntity<Thing> response = rest.exchange(url, HttpMethod.PATCH, entity, responseType);
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
    public void testPutThing() throws Exception {
        try {
            Map<String, String> properties = new LinkedHashMap<String, String>();
            properties.put("foo", "bar");
            properties.put("baz", "qux");
            String location = createThing(baseUrl, port, rest,
                    "test thing 1", "A thing for testing.", properties,
                    null);

            // Read newly created Thing
            ParameterizedTypeReference<Thing> responseType = new ParameterizedTypeReference<Thing>() {
            };
            HttpHeaders headers = new HttpHeaders();
            addTokenToHeader(port, headers);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Thing> response = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Thing.class), responseType);
            assertEquals(200, response.getStatusCodeValue());
            Thing t = response.getBody();

            // Update the Thing
            String updateStr = "{\"name\":\"test thing 2\"," +
                    "\"description\":\"Another thing for testing.\"," +
                    "\"properties\":" + "{\"foo\": \"qux\", \"blef\": \"baz\"}" +
                    "}";
            HttpEntity<String> entity = new HttpEntity<String>(updateStr, headers);
            response = rest.exchange(location,
                    HttpMethod.PUT, entity,
                    responseType);
            assertEquals(204, response.getStatusCodeValue());

            // Get the updated Thing
            response = rest.exchange(location,
                    HttpMethod.GET, getAuthorizedEntity(port, Thing.class), responseType);
            Thing t2 = response.getBody();
            assertEquals("test thing 2", t2.getName());
            assertEquals("Another thing for testing.", t2.getDescription());
            Map<String, String> p2 = t2.getProperties();
            assertEquals(null, p2.get("baz"));
            assertEquals("qux", p2.get("foo"));
            assertEquals("baz", p2.get("blef"));

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
    public void testPutUnknownThing() throws Exception {
        try {
            String uuid = UUIDs.timeBased().toString();
            String url = baseUrl + "(" + uuid +")";
            // Build request entity
            String requestStr = "{\"name\":\"test thing 1\"," +
                    "\"description\":\"A thing for testing.\"," +
                    "\"properties\":" + "{\"foo\": \"bar\", \"baz\": \"qux\"}" +
                    "}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
            ParameterizedTypeReference<Thing> responseType = new ParameterizedTypeReference<Thing>() {
            };
            ResponseEntity<Thing> response = rest.exchange(url, HttpMethod.PUT, entity, responseType);
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
            String requestStr = "{\"name\":\"test thing 1\"," +
                    "\"description\":\"A thing for testing.\"," +
                    "\"properties\":" + "{\"foo\": \"bar\", \"baz\": \"qux\"}" +
                    "}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addTokenToHeader(port, headers);
            HttpEntity<String> entity = new HttpEntity<String>(requestStr, headers);
            ParameterizedTypeReference<Thing> responseType = new ParameterizedTypeReference<Thing>() {
            };
            ResponseEntity<Thing> response = rest.exchange(url, HttpMethod.PUT, entity, responseType);
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
    public void testDeleteUnknownThing() throws Exception {
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
    public void testDeleteMalformedIdThing() throws Exception {
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
