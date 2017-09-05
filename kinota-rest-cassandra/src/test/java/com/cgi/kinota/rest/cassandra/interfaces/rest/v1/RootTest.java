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

import com.cgi.kinota.rest.cassandra.Application;
import com.cgi.kinota.rest.cassandra.CassandraRestTestBase;
import com.cgi.kinota.persistence.cassandra.config.SpringDataCassandraConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;

import static com.cgi.kinota.commons.Constants.COLLECTION_ATTR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by bmiles on 3/24/17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = { Application.class, SpringDataCassandraConfig.class })
public class RootTest extends CassandraRestTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RootTest.class);

    protected static final String BASE_URL = "http://localhost:";
    protected static final String URL_PATH_BASE = "/v1.0/";

    RestTemplate rest;
    String baseUrl;

    @Before
    public void init() {
        rest = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        baseUrl = generateBaseUrl(port);
    }

    @LocalServerPort
    int port;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public RestTemplateBuilder restTemplateBuilder() {
            return new RestTemplateBuilder().requestFactory(HttpComponentsClientHttpRequestFactory.class);
        }
    }

    public static String generateBaseUrl(int port) {
        return BASE_URL + port + API_VERSION;
    }

    @Test
    public void testApiRoot() throws Exception {
        ParameterizedTypeReference<String> responseTypeStr = new ParameterizedTypeReference<String>() {};
        ResponseEntity<String> responseStr;
        responseStr = rest.exchange(baseUrl,
                HttpMethod.GET, null, responseTypeStr);
        String s = responseStr.getBody();
        JsonReader r = Json.createReader(new StringReader(s));
        JsonObject o = r.readObject();
        JsonArray a = o.getJsonArray(COLLECTION_ATTR);
        assertTrue(a.size() == 7);

        JsonObject endPoint;
        // Things endpoint
        endPoint = a.getJsonObject(0);
        assertEquals("Things", endPoint.getString("name"));
        assertEquals(baseUrl + "Things", endPoint.getString("url"));
        // Locations endpoint
        endPoint = a.getJsonObject(1);
        assertEquals("Locations", endPoint.getString("name"));
        assertEquals(baseUrl + "Locations", endPoint.getString("url"));
        // Datastreams endpoint
        endPoint = a.getJsonObject(2);
        assertEquals("Datastreams", endPoint.getString("name"));
        assertEquals(baseUrl + "Datastreams", endPoint.getString("url"));
        // Sensors endpoint
        endPoint = a.getJsonObject(3);
        assertEquals("Sensors", endPoint.getString("name"));
        assertEquals(baseUrl + "Sensors", endPoint.getString("url"));
        // Observations endpoint
        endPoint = a.getJsonObject(4);
        assertEquals("Observations", endPoint.getString("name"));
        assertEquals(baseUrl + "Observations", endPoint.getString("url"));
        // ObservedProperties endpoint
        endPoint = a.getJsonObject(5);
        assertEquals("ObservedProperties", endPoint.getString("name"));
        assertEquals(baseUrl + "ObservedProperties", endPoint.getString("url"));
        // FeaturesOfInterest endpoint
        endPoint = a.getJsonObject(6);
        assertEquals("FeaturesOfInterest", endPoint.getString("name"));
        assertEquals(baseUrl + "FeaturesOfInterest", endPoint.getString("url"));
    }
}
