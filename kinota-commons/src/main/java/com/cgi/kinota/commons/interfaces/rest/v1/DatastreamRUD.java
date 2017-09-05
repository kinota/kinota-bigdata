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
import com.cgi.kinota.commons.application.DatastreamService;
import com.cgi.kinota.commons.domain.Datastream;

import io.swagger.annotations.*;
import io.swagger.jaxrs.PATCH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.json.JsonObject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static com.cgi.kinota.commons.Constants.API_VERSION_PATH;

/**
 * Created by bmiles on 3/1/17.
 */
@Component
@Path(API_VERSION_PATH + "/Datastreams({id})")
@Api(
        value = "API for CRUD of SensorThings Datastream objects.",
        consumes = MediaType.APPLICATION_JSON,
        produces = MediaType.APPLICATION_JSON
)
public class DatastreamRUD extends BaseResource {

    private static final Logger logger = LoggerFactory.getLogger(DatastreamRUD.class);

    @Context
    UriInfo uriInfo;

    @Autowired
    DatastreamService service;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Read a Datastream.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The Datastream bearing the specified ID."),
            @ApiResponse(code = 404, message = "A Datastream with this ID was not found."),
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response readDatastream(
            @ApiParam(value = "The ID of the Datastream to be read.", required = true) @PathParam("id") String id) {
        try {
            Datastream d = service.findOne(id);
            return streamJsonResponse(Response.Status.OK,
                    g -> { d.toJsonObject(g, this.getBaseUrlWithoutIdAsString(uriInfo));}
            );
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw ApplicationException.asApplicationException(e);
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Put a Datastream (replace all required fields of the Datastream).")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "The Datastream was successfully updated."),
            @ApiResponse(code = 400, message = "Update failed, required Datastream fields were not specified."),
            @ApiResponse(code = 404, message = "A Datastream with this ID was not found."),
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response putDatastream(
            @ApiParam(value = "The ID of the Datastream to be updated.", required = true) @PathParam("id") String id,
            @ApiParam(value = "JSON representation of the Datastream to replace the extant Datastream.", required = true) Datastream data) {
        try {
            Datastream d = service.findOne(id);
            service.overwrite(d, data);
            return Response.status(204).build();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw ApplicationException.asApplicationException(e);
        }
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Patch a Datastream (update specified fields of the Datastream).")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "The Datastream was successfully updated."),
            @ApiResponse(code = 404, message = "A Datastream with this ID was not found."),
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response patchDatastream(
            @ApiParam(value = "The ID of the Datastream to be updated.", required = true) @PathParam("id") String id,
            @ApiParam(value = "JSON representation of Datastream attributes to be updated.") JsonObject json) {
        try {
            Datastream d = service.findOne(id);
            service.update(d, json);
            return Response.status(204).build();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw ApplicationException.asApplicationException(e);
        }
    }

    @DELETE
    @ApiOperation(value = "Delete a Datastream.")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "The Datastream was successfully deleted."),
            @ApiResponse(code = 404, message = "A Datastream with this ID was not found."),
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response deleteDatastream(
            @ApiParam(value = "The ID of the Datastream to be deleted.", required = true) @PathParam("id") String id) {
        try {
            service.delete(id);
            return Response.status(204).build();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw ApplicationException.asApplicationException(e);
        }
    }
}
