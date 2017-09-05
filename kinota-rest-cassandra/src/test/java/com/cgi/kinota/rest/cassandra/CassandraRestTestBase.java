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

package com.cgi.kinota.rest.cassandra;

import com.cgi.kinota.persistence.cassandra.CassandraTestBase;

import com.cgi.kinota.rest.cassandra.auth.jwt.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;

/**
 * Created by bmiles on 1/4/17.
 */
public abstract class CassandraRestTestBase extends CassandraTestBase {

    private static final Logger logger = LoggerFactory.getLogger(CassandraRestTestBase.class);

    protected static final String BASE_URL = "http://localhost:";
    protected static final String API_VERSION = "/device/api/v1.0/";

    @LocalServerPort
    protected int port;

    protected RestTemplate rest;
    protected String baseUrl;
    protected String apiRootUrl;

    protected static String generateBaseUrl(int port, String urlPathBase) {
        return BASE_URL + port + urlPathBase;
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public RestTemplateBuilder restTemplateBuilder() {
            return new RestTemplateBuilder().requestFactory(HttpComponentsClientHttpRequestFactory.class);
        }
    }

    public void init(String urlPathBase) {
        rest = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        apiRootUrl = BASE_URL + port + API_VERSION;
        baseUrl = generateBaseUrl(port, urlPathBase);
    }

    public static String getAuthToken(int port) {
        String authUrl = String.format(Utility.AUTH_URL_BASE, port);
        JsonObject request = Json.createObjectBuilder()
                .add("id", Utility.DEFAULT_ID)
                .add("key", Utility.DEFAULT_KEY)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(request.toString(), headers);
        RestTemplate rest = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        ResponseEntity<String> response = rest.exchange(authUrl, HttpMethod.POST, entity, String.class);


        JsonReader reader = Json.createReader(new StringReader(response.getBody()));
        JsonObject o = reader.readObject();
        return o.getString("token");
    }

    public static void addTokenToHeader(int port, HttpHeaders headers) {
        String token = getAuthToken(port);
        headers.add("Authorization", "Bearer " + token);
    }

    public static <T> HttpEntity<T> getAuthorizedEntity(int port, T type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addTokenToHeader(port, headers);
        return new HttpEntity<T>(null, headers);
    }
}
