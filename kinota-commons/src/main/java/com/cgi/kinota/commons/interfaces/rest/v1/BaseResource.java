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
import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.odata.ODataQuery;
import com.cgi.kinota.commons.odata.ODataQueryException;
import com.cgi.kinota.commons.odata.QueryParser;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * @author dfladung
 */
public abstract class BaseResource {

	private static final Logger logger = LoggerFactory.getLogger(BaseResource.class);

	@Context
	protected HttpHeaders headers;

	@Context
	SecurityContext securityContext;

    @Context
    UriInfo uriInfo;

	protected WebApplicationException translateException(ApplicationException e) {
		Response.ResponseBuilder response = Response.status(500);
		response.entity(String.format(
				"{\n\"code\" : \"%s\", \"message\" : \"%s\"\n}",
				e.getErrorCode(),
				e.getMessage()));
		return new WebApplicationException(response.build());
	}

    /**
     * Parse OData query from request URI.  Invariant: returned ODataQuery
     * is not null (even if request URI is).
     * @return ODataQuery
     * @throws ApplicationException
     */
	protected ODataQuery parseODataQuery() throws ApplicationException {
	    ODataQuery query = null;

	    String queryStr = uriInfo.getRequestUri().getQuery();
	    if (queryStr != null) {
            try {
                query = QueryParser.parseQuery(queryStr);
            } catch (ODataQueryException e) {
                throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                        e.getMessage());
            }
        }

        if (query == null) return new ODataQuery();
        return query;
    }

	protected String getBaseUrl() {
        String baseUrl = null;
	    if (securityContext.isSecure()) {
            baseUrl = uriInfo.getAbsolutePathBuilder().scheme("https").build().toString();
        } else {
            baseUrl = uriInfo.getAbsolutePathBuilder().build().toString();
        }
	    return baseUrl;
    }

    protected String getBaseUrlWithTrailingSlash() {
        String baseUrl = null;
        if (securityContext.isSecure()) {
            baseUrl = uriInfo.getAbsolutePathBuilder().scheme("https").build().toString();
        } else {
            baseUrl = uriInfo.getAbsolutePathBuilder().build().toString();
        }
        if (baseUrl.charAt(baseUrl.length()-1) != '/') {
            baseUrl += "/";
        }
        return baseUrl;
    }

    protected String getRequestUriBase() {
        return uriInfo.getRequestUriBuilder().replaceQuery("").toString();
    }

    protected String getRequestUriBaseDataArray() {
	    MultivaluedMap query = uriInfo.getQueryParameters();
	    String queryString = "";
	    if (query.containsKey(Constants.RESULT_FORMAT_PARAM)) {
            String resultFormat = query.getFirst(Constants.RESULT_FORMAT_PARAM).toString();
            queryString = Constants.RESULT_FORMAT_PARAM + "=" + resultFormat;
        }
        return uriInfo.getRequestUriBuilder().replaceQuery("").toString() + "?" + queryString;
    }

	/**
	 *
	 * @param selfUrlBase
	 * @return The base URL with the entity identifier stripped.
	 * E.g. if the full URL is http://localhost:55704/v1.0/Locations(b23e2d40-d381-11e6-ba14-6733c7a00eb7),
	 * return http://localhost:55704/v1.0/Locations
	 */
	protected String getBaseUrlWithoutIdAsString(UriInfo selfUrlBase) {
		UriBuilder ub = selfUrlBase.getAbsolutePathBuilder();
		if (securityContext.isSecure()) {
		    ub.scheme("https");
        }
		String currUriStr = ub.build().toString();
		int endIdx = currUriStr.indexOf('(');
		return currUriStr.substring(0, endIdx);
	}

	protected String getBaseUrlForEntity(UriInfo selfUrlBase, String apiVersionStr, String entity) {
	    UriBuilder ub = selfUrlBase.getBaseUriBuilder();
        if (securityContext.isSecure()) {
            ub.scheme("https");
        }
        String baseUriStr = ub.build().toString();
	    return baseUriStr + apiVersionStr + "/" + entity;
    }

    protected Response streamJsonResponse(Response.Status status, Consumer<JsonGenerator> c) throws ApplicationException {
        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                ObjectMapper o = new ObjectMapper();
                JsonFactory f = o.getFactory();
                JsonGenerator g = f.createGenerator(os, JsonEncoding.UTF8);

                c.accept(g);

                g.flush();
                g.close();
            }
        };

        return Response.status(status).entity(stream).type(MediaType.APPLICATION_JSON).build();
    }

    /**
     * Stream a JSON object.
     * @param status
     * @param c
     * @return
     * @throws ApplicationException
     */
    protected Response streamObjectResponse(Response.Status status, Consumer<JsonGenerator> c) throws ApplicationException {
        return streamJsonResponse(status,
                g -> {
                    try {
                        g.writeStartObject();
                        c.accept(g);
                        g.writeEndObject();
                    } catch (IOException e) {
                        String mesg = "Unable to write object to JSON stream due to error: " +
                                e.getMessage();
                        logger.error(mesg);
                        throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
                    }
                });
    }

    /**
     * Stream a JSON array enclosed in a JSON object.
     * @param status
     * @param c
     * @return
     * @throws ApplicationException
     */
    protected Response streamArrayResponse(Response.Status status, Consumer<JsonGenerator> c) throws ApplicationException {
        return streamObjectResponse(status,
                g -> {
                    try {
                        g.writeArrayFieldStart(Constants.COLLECTION_ATTR);
                        c.accept(g);
                        g.writeEndArray();
                    } catch (IOException e) {
                        String mesg = "Unable to write array to JSON stream due to error: " +
                                e.getMessage();
                        logger.error(mesg);
                        throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
                    }
                });
    }

    /**
     * Stream a JSON array without an enclosing JSON object.
     * @param status
     * @param c
     * @return
     * @throws ApplicationException
     */
    protected Response streamBareArrayResponse(Response.Status status, Consumer<JsonGenerator> c) throws ApplicationException {
        return streamJsonResponse(status,
                g -> {
                    try {
                        g.writeStartArray();
                        c.accept(g);
                        g.writeEndArray();
                    } catch (IOException e) {
                        String mesg = "Unable to write array to JSON stream due to error: " +
                                e.getMessage();
                        logger.error(mesg);
                        throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
                    }
                });
    }

    protected void responseWriteString(JsonGenerator g, String value) throws ApplicationException {
        try {
            g.writeString(value);
        } catch (IOException e) {
            String mesg = "Unable to write string '" + value + "' to JSON stream due to error: " +
                    e.getMessage();
            logger.error(mesg);
            throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
        }
    }
}
