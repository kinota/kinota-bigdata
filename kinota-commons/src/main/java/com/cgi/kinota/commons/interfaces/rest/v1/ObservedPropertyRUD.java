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
import com.cgi.kinota.commons.application.ObservedPropertyService;
import com.cgi.kinota.commons.Constants;
import com.cgi.kinota.commons.domain.ObservedProperty;

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

/**
 * Created by bmiles on 2/23/17.
 */
@Component
@Path(Constants.API_VERSION_PATH + "/ObservedProperties({id})")
@Api(
        value = "API for CRUD of SensorThings ObservedProperty objects.",
        consumes = MediaType.APPLICATION_JSON,
        produces = MediaType.APPLICATION_JSON
)
public class ObservedPropertyRUD extends BaseResource {

    private static final Logger logger = LoggerFactory.getLogger(ObservedPropertyRUD.class);

    @Context
    UriInfo uriInfo;

    @Autowired
    ObservedPropertyService service;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Read an ObservedProperty.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The ObservedProperty bearing the specified ID."),
            @ApiResponse(code = 404, message = "A ObservedProperty with this ID was not found."),
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response readObservedProperty(
            @ApiParam(value = "The ID of the ObservedProperty to be read.", required = true) @PathParam("id") String id) {
        try {
            ObservedProperty op = service.findOne(id);
            return streamJsonResponse(Response.Status.OK,
                    g -> { op.toJsonObject(g, this.getBaseUrlWithoutIdAsString(uriInfo)); }
            );
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw ApplicationException.asApplicationException(e);
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Put an ObservedProperty (replace all fields of the ObservedProperty).")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "The ObservedProperty was successfully updated."),
            @ApiResponse(code = 400, message = "Update failed, required ObservedProperty fields were not specified."),
            @ApiResponse(code = 404, message = "An ObservedProperty with this ID was not found."),
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response putObservedProperty(
            @ApiParam(value = "The ID of the ObservedProperty to be updated.", required = true) @PathParam("id") String id,
            @ApiParam(value = "JSON representation of the ObservedProperty to replace the extant ObservedProperty.", required = true) ObservedProperty data) {
        try {
            ObservedProperty op = service.findOne(id);
            service.overwrite(op, data);
            return Response.status(204).build();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw ApplicationException.asApplicationException(e);
        }
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Patch a ObservedProperty (update specified fields of the ObservedProperty).")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "The ObservedProperty was successfully updated."),
            @ApiResponse(code = 404, message = "A ObservedProperty with this ID was not found."),
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response patchObservedProperty(
            @ApiParam(value = "The ID of the ObservedProperty to be updated.", required = true) @PathParam("id") String id,
            @ApiParam(value = "JSON representation of ObservedProperty attributes to be updated.") JsonObject json) {
        try {
            ObservedProperty op = service.findOne(id);
            service.update(op, json);
            return Response.status(204).build();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw ApplicationException.asApplicationException(e);
        }
    }

    @DELETE
    @ApiOperation(value = "Delete an ObservedProperty.")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "The ObservedProperty was successfully deleted."),
            @ApiResponse(code = 404, message = "A ObservedProperty with this ID was not found."),
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response deleteSensor(
            @ApiParam(value = "The ID of the ObservedProperty to be deleted.", required = true) @PathParam("id") String id) {
        try {
            service.delete(id);
            return Response.status(204).build();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw ApplicationException.asApplicationException(e);
        }
    }
}
