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
import com.cgi.kinota.commons.application.HistoricalLocationService;
import com.cgi.kinota.commons.domain.HistoricalLocation;

import io.swagger.annotations.*;
import io.swagger.jaxrs.PATCH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.json.JsonObject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static com.cgi.kinota.commons.Constants.API_VERSION_PATH;

/**
 * Created by bmiles on 2/7/17.
 */
@Path(API_VERSION_PATH + "/HistoricalLocations({id})")
@Api(
        value = "API for CRUD of SensorThings HistoricalLocation objects.",
        consumes = MediaType.APPLICATION_JSON,
        produces = MediaType.APPLICATION_JSON
)
public class HistoricalLocationRUD extends BaseResource {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalLocationRUD.class);

    @Context
    UriInfo uriInfo;

    @Autowired
    HistoricalLocationService service;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Read an HistoricalLocation.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The HistoricalLocation bearing the specified ID."),
            @ApiResponse(code = 404, message = "A HistoricalLocation with this ID was not found."),
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response readHistoricalLocation(
            @ApiParam(value = "The ID of the HistoricalLocation to be read.", required = true) @PathParam("id") String id) {
        try {
            HistoricalLocation hl = service.findOne(id);
            return streamJsonResponse(Response.Status.OK,
                    g -> { hl.toJsonObject(g, this.getBaseUrlWithoutIdAsString(uriInfo));}
            );
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw ApplicationException.asApplicationException(e);
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Put an HistoricalLocation (replace all fields of the Location).")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "The HistoricalLocation was successfully updated."),
            @ApiResponse(code = 400, message = "Update failed, required HistoricalLocation fields were not specified."),
            @ApiResponse(code = 404, message = "A HistoricalLocation with this ID was not found."),
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response putHistoricalLocation(
            @ApiParam(value = "The ID of the HistoricalLocation to be updated.", required = true) @PathParam("id") String id,
            @ApiParam(value = "JSON representation of the Location to replace the extant HistoricalLocation.", required = true) HistoricalLocation data) {
        try {
            HistoricalLocation hl = service.findOne(id);
            service.overwrite(hl, data);
            return Response.status(204).build();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw ApplicationException.asApplicationException(e);
        }
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Patch an HistoricalLocation (update specified fields of the HistoricalLocation).")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "The HistoricalLocation was successfully updated."),
            @ApiResponse(code = 404, message = "A Location with this ID was not found."),
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response patchHistoricalLocation(
            @ApiParam(value = "The ID of the HistoricalLocation to be updated.", required = true) @PathParam("id") String id,
            @ApiParam(value = "JSON representation of HistoricalLocation attributes to be updated.") JsonObject json) {
        try {
            HistoricalLocation hl = service.findOne(id);
            service.update(hl, json);
            return Response.status(204).build();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw ApplicationException.asApplicationException(e);
        }
    }

    @DELETE
    @ApiOperation(value = "Delete an HistoricalLocation.")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "The HistoricalLocation was successfully deleted."),
            @ApiResponse(code = 404, message = "A HistoricalLocation with this ID was not found."),
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response deleteHistoricalLocation(
            @ApiParam(value = "The ID of the HistoricalLocation to be deleted.", required = true) @PathParam("id") String id) {
        try {
            service.delete(id);
            return Response.status(204).build();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw ApplicationException.asApplicationException(e);
        }
    }
}
