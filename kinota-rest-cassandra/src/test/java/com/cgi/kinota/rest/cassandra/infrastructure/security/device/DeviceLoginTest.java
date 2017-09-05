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

package com.cgi.kinota.rest.cassandra.infrastructure.security.device;

import com.cgi.kinota.persistence.cassandra.config.SpringDataCassandraConfig;
import com.cgi.kinota.rest.cassandra.Application;
import com.cgi.kinota.rest.cassandra.CassandraRestTestBase;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;

import static com.cgi.kinota.rest.cassandra.auth.jwt.Utility.AUTH_URL_BASE;
import static com.cgi.kinota.rest.cassandra.auth.jwt.Utility.DEFAULT_ID;
import static com.cgi.kinota.rest.cassandra.auth.jwt.Utility.DEFAULT_KEY;
import static org.junit.Assert.*;

/**
 * Created by dfladung on 3/27/17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {Application.class, SpringDataCassandraConfig.class})
public class DeviceLoginTest extends CassandraRestTestBase {

    private static final Logger logger = LoggerFactory.getLogger(DeviceLoginTest.class);

    @LocalServerPort
    int port;

    @Test
    public void testLogin() {
        try {
            String authUrl = String.format(AUTH_URL_BASE, port);
            JsonObject request = Json.createObjectBuilder()
                    .add("id", DEFAULT_ID)
                    .add("key", DEFAULT_KEY)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(request.toString(), headers);
            RestTemplate rest = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
            ResponseEntity<String> response = rest.exchange(authUrl, HttpMethod.POST, entity, String.class);

            assertEquals(200, response.getStatusCodeValue());
            JsonReader reader = Json.createReader(new StringReader(response.getBody()));
            JsonObject o = reader.readObject();
            String token = o.getString("token");
            assertTrue(!StringUtils.isEmpty(token));
            logger.info(token);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
        }
    }

    @Test
    public void testInvalidLogin() {
        try {
            String authUrl = String.format(AUTH_URL_BASE, port);
            JsonObject request = Json.createObjectBuilder()
                    .add("id", DEFAULT_ID)
                    .add("key", "bogus")
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(request.toString(), headers);
            RestTemplate rest = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
            rest.exchange(authUrl, HttpMethod.POST, entity, String.class);

        } catch (HttpClientErrorException e) {
            assertEquals(401, e.getRawStatusCode());
            JsonReader reader = Json.createReader(new StringReader(e.getResponseBodyAsString()));
            JsonObject o = reader.readObject();
            String message = o.getString("message");
            assertEquals("Invalid username or password", message);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
        }
    }
}