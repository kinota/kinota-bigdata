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

package com.cgi.kinota.rest.cassandra.interfaces.rest;

import com.cgi.kinota.commons.interfaces.rest.ApplicationExceptionMapper;
import com.cgi.kinota.commons.interfaces.rest.ResponseFilters;
import com.cgi.kinota.commons.interfaces.rest.v1.*;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.json.stream.JsonGenerator;
import javax.ws.rs.ApplicationPath;

import static com.cgi.kinota.commons.Constants.API_VERSION_PATH;
import static com.cgi.kinota.commons.Constants.CONFIG_ID;
import static com.cgi.kinota.commons.Constants.MAX_REQUEST_PAGE_SIZE;

/**
 * @author dfladung
 */
@Component
@ApplicationPath(API_VERSION_PATH)
public class JerseyConfig extends ResourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(JerseyConfig.class);

    public JerseyConfig() {
        // initialize provider and register Joda provider
        JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
        JodaMapper jodaMapper = new JodaMapper();
        jodaMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        provider.setMapper(jodaMapper);
        register(provider);

        // JSON-P configuration
        property(JsonGenerator.PRETTY_PRINTING, true);

        // Register exception mappers
        register(ApplicationExceptionMapper.class);

        // using packages("...") causes issues when running as an executable WAR, so registering manually
        register(ResponseFilters.class);
        register(Root.class);
        register(ThingCF.class);
        register(ThingRUD.class);
        register(ThingRelated.class);
        register(LocationCF.class);
        register(LocationRUD.class);
        register(LocationRelated.class);
        register(HistoricalLocationCF.class);
        register(HistoricalLocationRUD.class);
        register(HistoricalLocationRelated.class);
        register(SensorCF.class);
        register(SensorRUD.class);
        register(SensorRelated.class);
        register(ObservedPropertyCF.class);
        register(ObservedPropertyRUD.class);
        register(ObservedPropertyRelated.class);
        register(FeatureOfInterestCF.class);
        register(FeatureOfInterestRUD.class);
        register(FeatureOfInterestRelated.class);
        register(DatastreamCF.class);
        register(DatastreamRUD.class);
        register(DatastreamRelated.class);
        register(ObservationCF.class);
        register(ObservationRUD.class);
        register(ObservationRelated.class);
        register(CreateObservations.class);

        // swagger initialization
        register(ApiListingResource.class);
        register(SwaggerSerializers.class);
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setConfigId(CONFIG_ID);
        beanConfig.setTitle("SensorThings API description.");
        beanConfig.setVersion(API_VERSION_PATH);
        beanConfig.setSchemes(new String[]{"https"});
        // Set a base path if desired.
        beanConfig.setBasePath("/device/api");
        beanConfig.setResourcePackage("com.cgi.rap.cgist.samplenetwork.interfaces.rest");
        beanConfig.setScan(true); // this is magic and an afront to everything I hold dear

        logger.info("MAX_REQUEST_PAGE_SIZE = " + MAX_REQUEST_PAGE_SIZE);
        logger.info("Initialized Jersey");
    }

}
