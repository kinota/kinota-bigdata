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

package com.cgi.kinota.commons.interfaces.rest;

import com.cgi.kinota.commons.application.exception.ApplicationException;

import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.core.Response;

/**
 * Created by bmiles on 2/24/17.
 */
@Provider
public class ApplicationExceptionMapper implements ExceptionMapper<ApplicationException> {
    public Response toResponse(ApplicationException e) {
        switch (e.getErrorCode()) {
            case E_Invalid:
                return Response.status(400)
                        .entity(renderJsonError(400, e.getMessage())).build();
            case E_Authentication:
                return Response.status(401)
                        .entity(renderJsonError(401, e.getMessage())).build();
            case E_Authorization:
                return Response.status(403)
                        .entity(renderJsonError(403, e.getMessage())).build();
            case E_NotFound:
                return Response.status(404)
                        .entity(renderJsonError(404, e.getMessage())).build();
            default:
                return Response.status(500).build();
        }
    }

    protected String renderJsonError(int status, String message) {
        return "{\"error\":{\"code\":" + status
                + ",\"message\":\"" + message + "\"}}";
    }
}
