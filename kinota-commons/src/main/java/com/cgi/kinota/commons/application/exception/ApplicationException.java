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

package com.cgi.kinota.commons.application.exception;

import org.apache.commons.lang3.StringUtils;

/**
 * @author dfladung
 */
public class ApplicationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    ApplicationErrorCode errorCode;

    public ApplicationException(ApplicationErrorCode errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
    }

    public static ApplicationException asApplicationException(Exception e, ApplicationErrorCode code, String message) {
        if (e instanceof ApplicationException) {
            return ((ApplicationException) e);
        } else {
            ApplicationErrorCode aec = (code == null) ? ApplicationErrorCode.E_InternalError : code;
            String msg = (StringUtils.isEmpty(message)) ? "An internal error occurred." : message;
            return new ApplicationException(aec, msg);
        }
    }

    public static ApplicationException asApplicationException(Exception e) {
        return asApplicationException(e, null, null);
    }

    public ApplicationErrorCode getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "ApplicationException { " +
                "message: " + this.getMessage() +
                ", errorCode: " + this.errorCode.toString() +
                " }";
    }
}
