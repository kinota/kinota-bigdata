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

import com.cgi.kinota.commons.application.QueryableService;
import com.cgi.kinota.commons.application.ThingService;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.application.paging.Paginator;
import com.cgi.kinota.commons.domain.Thing;
import com.cgi.kinota.commons.odata.ODataQuery;
import com.cgi.kinota.commons.Constants;

import io.swagger.annotations.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.json.JsonObject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import static com.cgi.kinota.commons.Constants.*;

/**
 * Created by bmiles on 1/4/17.
 *
 * Handles API requests for Things whose URL ends with "/Things" (i.e. for creating Things
 *   and for requesting Things using filters)
 */
@Component
@Path(API_VERSION_PATH + "/Things")
@Api(
        value = "API for CRUD of SensorThings Thing objects.",
        consumes = MediaType.APPLICATION_JSON,
        produces = MediaType.APPLICATION_JSON
)
public class ThingCF extends BaseResource {

    private static final Logger logger = LoggerFactory.getLogger(ThingCF.class);

    @Autowired
    ThingService service;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a Thing.")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response createThing(
            @ApiParam(value = "JSON representation of the Thing to be created.", required = true) JsonObject json) {
        try {
            Thing t = service.create(json);
            // Add selfLink to "Location" response header.
            return Response.created(t.generateSelfLinkUrl(getBaseUrl())).build();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw ApplicationException.asApplicationException(e);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Read Things.")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response readThings() {
        try {
            ODataQuery q = parseODataQuery();
            String baseUrl = this.getBaseUrlForEntity(uriInfo, Constants.API_VERSION, Thing.NAME_PLURAL);
            return streamJsonResponse(Response.Status.OK,
                    g -> { QueryableService.toJsonArray(service.findAll(q), q, g, baseUrl,
                            Paginator.DEFAULT_PAGINATOR); });
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw ApplicationException.asApplicationException(e);
        }
    }
}
