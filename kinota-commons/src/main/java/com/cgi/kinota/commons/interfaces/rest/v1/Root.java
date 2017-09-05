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

package com.cgi.kinota.commons.interfaces.rest.v1;

import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.Constants;
import com.cgi.kinota.commons.domain.Datastream;
import com.cgi.kinota.commons.domain.FeatureOfInterest;
import com.cgi.kinota.commons.domain.Location;
import com.cgi.kinota.commons.domain.ObservedProperty;
import com.cgi.kinota.commons.domain.Sensor;
import com.cgi.kinota.commons.domain.Thing;
import com.cgi.kinota.commons.domain.Observation;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;

/**
 * Created by bmiles on 3/24/17.
 */
@Component
@Path(Constants.API_VERSION_PATH)
@Api(
        value = "API for CRUD of SensorThings objects.",
        consumes = MediaType.APPLICATION_JSON,
        produces = MediaType.APPLICATION_JSON
)
public class Root extends BaseResource {

    private static final Logger logger = LoggerFactory.getLogger(Root.class);

    private static String[] ENTITY_NAMES = { Thing.NAME_PLURAL, Location.NAME_PLURAL,
            Datastream.NAME_PLURAL, Sensor.NAME_PLURAL, Observation.NAME_PLURAL,
            ObservedProperty.NAME_PLURAL, FeatureOfInterest.NAME_PLURAL };

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "API Root.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "JSON Document Describing SensorThings API endpoints."),
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response readEndpoints() {
        try {
            String urlBase = getBaseUrlWithTrailingSlash();
            return streamArrayResponse(Response.Status.OK,
                    g -> {
                        for (String entityName : ENTITY_NAMES) {
                            try {
                                g.writeStartObject();
                                g.writeStringField("name", entityName);
                                g.writeStringField("url", urlBase + entityName);
                                g.writeEndObject();
                            } catch (IOException e) {
                                String mesg = "Unable to write to JSON stream due to error: " +
                                        e.getMessage();
                                logger.error(mesg);
                                throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
                            }
                        }
                    });
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
