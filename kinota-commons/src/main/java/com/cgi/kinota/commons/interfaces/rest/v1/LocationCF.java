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

import com.cgi.kinota.commons.application.LocationService;
import com.cgi.kinota.commons.application.QueryableService;
import com.cgi.kinota.commons.Constants;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.application.paging.Paginator;
import com.cgi.kinota.commons.domain.Location;
import com.cgi.kinota.commons.odata.ODataQuery;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by bmiles on 1/20/17.
 */
@Component
@Path(Constants.API_VERSION_PATH + "/Locations")
@Api(
        value = "API for CRUD of SensorThings Location objects.",
        consumes = MediaType.APPLICATION_JSON,
        produces = MediaType.APPLICATION_JSON
)
public class LocationCF extends BaseResource {

    private static final Logger logger = LoggerFactory.getLogger(LocationCF.class);

    @Autowired
    LocationService service;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a Location.")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response createLocation(
            @ApiParam(value = "JSON representation of the Location to be created.", required = true) Location data) {
        try {
            Location l = service.create(data.getName(), data.getDescription(),
                    data.getEncodingType(), data.getLocation());
            // Add selfLink to "Location" response header.
            return Response.created(l.generateSelfLinkUrl(getBaseUrl())).build();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw ApplicationException.asApplicationException(e);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Read Locations.")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response readLocations() {
        try {
            ODataQuery q = parseODataQuery();
            String baseUrl = this.getBaseUrlForEntity(uriInfo, Constants.API_VERSION, Location.NAME_PLURAL);
            return streamJsonResponse(Response.Status.OK,
                    g -> { QueryableService.toJsonArray(service.findAll(q), q, g, baseUrl,
                            Paginator.DEFAULT_PAGINATOR); });
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw ApplicationException.asApplicationException(e);
        }
    }
}
