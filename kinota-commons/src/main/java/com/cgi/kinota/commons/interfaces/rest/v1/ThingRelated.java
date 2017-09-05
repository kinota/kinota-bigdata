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

import com.cgi.kinota.commons.Constants;
import com.cgi.kinota.commons.application.QueryableService;
import com.cgi.kinota.commons.application.RelatedEntityManager;
import com.cgi.kinota.commons.application.ThingService;
import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.application.paging.Paginator;
import com.cgi.kinota.commons.domain.Datastream;
import com.cgi.kinota.commons.domain.HistoricalLocation;
import com.cgi.kinota.commons.domain.Location;
import com.cgi.kinota.commons.domain.Thing;
import com.cgi.kinota.commons.odata.ODataQuery;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Created by bmiles on 1/31/17.
 */
@Component
@Path(Constants.API_VERSION_PATH + "/Things({id})/{navigationProperty}")
@Api(
        value = "API for CRUD of SensorThings Thing objects.",
        consumes = MediaType.APPLICATION_JSON,
        produces = MediaType.APPLICATION_JSON
)
public class ThingRelated extends BaseResource {

    private static final Logger logger = LoggerFactory.getLogger(ThingRelated.class);

    @Context
    UriInfo uriInfo;

    @Autowired
    ThingService thingService;

    @Autowired
    RelatedEntityManager related;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Read related entities associated with a Thing.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The entity related to the Thing bearing the specified ID was found."),
            @ApiResponse(code = 404, message = "A Thing with this ID was not found."),
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response readRelated(
            @ApiParam(value = "The ID of the Thing whose entity is to be read.", required = true) @PathParam("id") String id,
            @ApiParam(value = "The related entity to be read.", required = true) @PathParam("navigationProperty") String navigationProperty) {
        try {
            Thing t = thingService.findOne(id);
            ODataQuery q = parseODataQuery();
            String requestUriBase = getRequestUriBase();
            switch (navigationProperty) {
                case Location.NAME_PLURAL:
                {
                    String entityUriBase = this.getBaseUrlForEntity(uriInfo, Constants.API_VERSION, Location.NAME_PLURAL);
                    return streamJsonResponse(Response.Status.OK,
                            g -> { QueryableService.toJsonArrayRelated(related.fetchRelatedLocationsForThing(t.getId(), q), q, g, requestUriBase, entityUriBase,
                                    Paginator.DEFAULT_PAGINATOR);});
                }
                case HistoricalLocation.NAME_PLURAL:
                {
                    String entityUriBase = this.getBaseUrlForEntity(uriInfo, Constants.API_VERSION, HistoricalLocation.NAME_PLURAL);
                    return streamJsonResponse(Response.Status.OK,
                            g -> { QueryableService.toJsonArrayRelated(related.fetchRelatedHistoricalLocationsForThing(t.getId(), q), q, g, requestUriBase, entityUriBase,
                                    Paginator.DEFAULT_PAGINATOR);});
                }
                case Datastream.NAME_PLURAL:
                {
                    String entityUriBase = this.getBaseUrlForEntity(uriInfo, Constants.API_VERSION, Datastream.NAME_PLURAL);
                    return streamJsonResponse(Response.Status.OK,
                            g -> { QueryableService.toJsonArrayRelated(related.fetchRelatedDatastreamsForThing(t.getId(), q), q, g, requestUriBase, entityUriBase,
                                    Paginator.DEFAULT_PAGINATOR);});
                }
                default:
                    // Unknown related entity
                    throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                            "Unknown navigation property '" + navigationProperty + "'.");
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw ApplicationException.asApplicationException(e);
        }
    }

    @GET
    @Path("$ref")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a list of selfLInks for related entities associated with a Thing.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The entity related to the Thing bearing the specified ID was found."),
            @ApiResponse(code = 404, message = "A Thing with this ID was not found."),
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response readRelatedRef(
            @ApiParam(value = "The ID of the Thing to be read.", required = true) @PathParam("id") String id,
            @ApiParam(value = "The related entities to be read.", required = true) @PathParam("navigationProperty") String navigationProperty) {
        try {
            Thing t = thingService.findOne(id);
            ODataQuery q = parseODataQuery();
            String requestUriBase = getRequestUriBase();
            switch (navigationProperty) {
                case Location.NAME_PLURAL:
                {
                    String entityUriBase = this.getBaseUrlForEntity(uriInfo, Constants.API_VERSION, Location.NAME_PLURAL);
                    return streamJsonResponse(Response.Status.OK,
                            g -> { QueryableService.toJsonArrayRelatedRef(related.fetchLocationUuidsForThing(t.getId(), q), q, g, requestUriBase, entityUriBase,
                                    Paginator.DEFAULT_PAGINATOR);});
                }
                case HistoricalLocation.NAME_PLURAL:
                {
                    String entityUriBase = this.getBaseUrlForEntity(uriInfo, Constants.API_VERSION, HistoricalLocation.NAME_PLURAL);
                    return streamJsonResponse(Response.Status.OK,
                            g -> { QueryableService.toJsonArrayRelatedRef(related.fetchHistoricalLocationUuidsForThing(t.getId(), q), q, g, requestUriBase, entityUriBase,
                                    Paginator.DEFAULT_PAGINATOR);});
                }
                case Datastream.NAME_PLURAL:
                {
                    String entityUriBase = this.getBaseUrlForEntity(uriInfo, Constants.API_VERSION, Datastream.NAME_PLURAL);
                    return streamJsonResponse(Response.Status.OK,
                            g -> { QueryableService.toJsonArrayRelatedRef(related.fetchDatastreamUuidsForThing(t.getId(), q), q, g, requestUriBase, entityUriBase,
                                    Paginator.DEFAULT_PAGINATOR);});
                }
                default:
                    // Unknown related entity
                    throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                            "Unknown navigation property '" + navigationProperty + "'.");
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw ApplicationException.asApplicationException(e);
        }
    }
}
