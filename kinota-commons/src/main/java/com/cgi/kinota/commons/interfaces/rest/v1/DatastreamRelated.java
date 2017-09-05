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
import com.cgi.kinota.commons.application.DatastreamService;
import com.cgi.kinota.commons.application.QueryableService;
import com.cgi.kinota.commons.application.RelatedEntityManager;
import com.cgi.kinota.commons.application.SensorService;
import com.cgi.kinota.commons.application.ThingService;
import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.application.paging.Paginator;
import com.cgi.kinota.commons.application.ObservedPropertyService;
import com.cgi.kinota.commons.domain.ObservedProperty;
import com.cgi.kinota.commons.domain.Datastream;
import com.cgi.kinota.commons.domain.Observation;
import com.cgi.kinota.commons.domain.Sensor;
import com.cgi.kinota.commons.domain.Thing;
import com.cgi.kinota.commons.odata.ODataQuery;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import java.util.UUID;

/**
 * Created by bmiles on 3/1/17.
 */
@Component
@Path(Constants.API_VERSION_PATH + "/Datastreams({id})/{navigationProperty}")
@Api(
        value = "API for CRUD of SensorThings Datastream objects.",
        consumes = MediaType.APPLICATION_JSON,
        produces = MediaType.APPLICATION_JSON
)
public class DatastreamRelated extends BaseResource {

    private static final Logger logger = LoggerFactory.getLogger(DatastreamRelated.class);

    @Context
    UriInfo uriInfo;

    @Autowired
    DatastreamService service;

    @Autowired
    ThingService thingService;

    @Autowired
    SensorService sensorService;

    @Autowired
    ObservedPropertyService observedPropertyService;

    @Autowired
    RelatedEntityManager related;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Read related entities associated with a Datastream.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The entity related to the Datastream bearing the specified ID was found."),
            @ApiResponse(code = 404, message = "A Datastream with this ID was not found."),
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response readRelated(
            @ApiParam(value = "The ID of the Datastream whose entity is to be read.", required = true) @PathParam("id") String id,
            @ApiParam(value = "The related entity to be read.", required = true) @PathParam("navigationProperty") String navigationProperty) {
        try {
            Datastream d = service.findOne(UUID.fromString(id));
            ODataQuery q = parseODataQuery();

            switch (navigationProperty) {
                case Thing.NAME:
                    Thing t = thingService.findOne(d.getThingId());
                    return streamArrayResponse(Response.Status.OK,
                            g -> { t.toJsonObject(g, this.getBaseUrlForEntity(uriInfo, Constants.API_VERSION, Thing.NAME_PLURAL)); });
                case Sensor.NAME:
                    Sensor s = sensorService.findOne(d.getSensorId());
                    return streamArrayResponse(Response.Status.OK,
                            g -> { s.toJsonObject(g, this.getBaseUrlForEntity(uriInfo, Constants.API_VERSION, Sensor.NAME_PLURAL)); });
                case ObservedProperty.NAME:
                    ObservedProperty op = observedPropertyService.findOne(d.getObservedPropertyId());
                    return streamArrayResponse(Response.Status.OK,
                            g -> { op.toJsonObject(g, this.getBaseUrlForEntity(uriInfo, Constants.API_VERSION, ObservedProperty.NAME_PLURAL)); });
                case Observation.NAME_PLURAL:
                    if (q.getResultFormat() == null) {
                        String requestUriBase = getRequestUriBase();
                        String entityUriBase = this.getBaseUrlForEntity(uriInfo, Constants.API_VERSION, Observation.NAME_PLURAL);
                        return streamJsonResponse(Response.Status.OK,
                                g -> { QueryableService.toJsonArrayRelated(related.fetchRelatedObservationsForDatastream(d.getId(), q), q,
                                        g, requestUriBase, entityUriBase, Paginator.DEFAULT_PAGINATOR);

                                });
                    } else {
                        // Read Observations using dataArray extension
                        if (Constants.RESULT_FORMAT_DATA_ARRAY.equals(q.getResultFormat())) {
                            String requestUriBase = getRequestUriBaseDataArray();
                            return streamArrayResponse(Response.Status.OK,
                                    g -> { Observation.toJsonDataArray(related.fetchRelatedObservationsForDatastream(d.getId(), q),
                                            q, g, requestUriBase, d.getId(), Paginator.DEFAULT_PAGINATOR); });
                        } else {
                            String mesg = "Unknown resultFormat '" + q.getResultFormat() + "'.";
                            logger.error(mesg);
                            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                                    mesg);
                        }
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
    @ApiOperation(value = "Get a list of selfLinks for related entities associated with a Datastream.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The entity related to the Datastream bearing the specified ID was found."),
            @ApiResponse(code = 404, message = "A Datastream with this ID was not found."),
            @ApiResponse(code = 500, message = "The server encountered an internal error. Please retry the request.")})
    public Response readRelatedRef(
            @ApiParam(value = "The ID of the Datastream whose entity is to be read.", required = true) @PathParam("id") String id,
            @ApiParam(value = "The related entity to be read.", required = true) @PathParam("navigationProperty") String navigationProperty) {
        try {
            Datastream d = service.findOne(UUID.fromString(id));
            ODataQuery q = parseODataQuery();
            String requestUriBase = getRequestUriBase();
            switch (navigationProperty) {
                case Thing.NAME:
                {
                    String entityUriBase = this.getBaseUrlForEntity(uriInfo, Constants.API_VERSION, Thing.NAME_PLURAL);
                    return streamJsonResponse(Response.Status.OK,
                            g -> { QueryableService.toJsonArrayRelatedRefSingleton(d.getThingId(), g, entityUriBase);});
                }
                case Sensor.NAME:
                {
                    String entityUriBase = this.getBaseUrlForEntity(uriInfo, Constants.API_VERSION, Sensor.NAME_PLURAL);
                    return streamJsonResponse(Response.Status.OK,
                            g -> { QueryableService.toJsonArrayRelatedRefSingleton(d.getSensorId(), g, entityUriBase);});
                }
                case ObservedProperty.NAME:
                {
                    String entityUriBase = this.getBaseUrlForEntity(uriInfo, Constants.API_VERSION, ObservedProperty.NAME_PLURAL);
                    return streamJsonResponse(Response.Status.OK,
                            g -> { QueryableService.toJsonArrayRelatedRefSingleton(d.getObservedPropertyId(), g, entityUriBase);});
                }
                case Observation.NAME_PLURAL:
                {
                    String entityUriBase = this.getBaseUrlForEntity(uriInfo, Constants.API_VERSION, Observation.NAME_PLURAL);
                    return streamJsonResponse(Response.Status.OK,
                            g -> { QueryableService.toJsonArrayRelatedRef(related.fetchRelatedObservationUuidsForDatastream(d.getId(), q), q, g, requestUriBase, entityUriBase,
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
