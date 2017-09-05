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

import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.application.ObservationService;
import com.cgi.kinota.commons.domain.Observation;

import io.swagger.annotations.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.json.JsonArray;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import static com.cgi.kinota.commons.Constants.API_VERSION;
import static com.cgi.kinota.commons.Constants.API_VERSION_PATH;


/**
 * Created by bmiles on 3/29/17.
 */
@Component
@Path(API_VERSION_PATH + "/CreateObservations")
@Api(
        value = "API for CRUD of SensorThings Observation objects.",
        consumes = MediaType.APPLICATION_JSON,
        produces = MediaType.APPLICATION_JSON
)
public class CreateObservations extends BaseResource {

    private static final Logger logger = LoggerFactory.getLogger(CreateObservations.class);

    @Context
    UriInfo uriInfo;

    @Autowired
    ObservationService service;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create Observations using SensorThings dataArray extension.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "The Observations were created."),
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response createObservations(
            @ApiParam(value = "JSON representation of the Observations (in SensorThings dataArray format) to be created.", required = true) JsonArray json) {
        try {
            String baseUrl = uriInfo.getBaseUri().toString() + API_VERSION + "/" + Observation.NAME_PLURAL;
            return streamBareArrayResponse(Response.Status.CREATED,
                    g -> {
                        service.createObservations(json, baseUrl).forEach(s -> responseWriteString(g, s));
                    });
        } catch (ApplicationException e) {
            logger.error("Error: " + e.toString() + ", when creating observations");
            throw e;
        }
    }
}
