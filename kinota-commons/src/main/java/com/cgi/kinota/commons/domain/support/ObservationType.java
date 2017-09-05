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

package com.cgi.kinota.commons.domain.support;

import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;

import java.net.URI;

import static com.cgi.kinota.commons.Constants.*;

/**
 * Created by bmiles on 5/5/17.
 */
public enum ObservationType {

    OM_CategoryObservation,
    OM_CountObservation,
    OM_Measurement,
    OM_Observation,
    OM_TruthObservation;

    public static ObservationType valueOfUri(URI uri) throws ApplicationException {
        switch(uri.toString()) {
            case OBS_TYPE_OM_CATEGORY_OBSERVATION:
                return OM_CategoryObservation;
            case OBS_TYPE_OM_COUNT_OBSERVATION:
                return OM_CountObservation;
            case OBS_TYPE_OM_MEASUREMENT:
                return OM_Measurement;
            case OBS_TYPE_OM_OBSERVATION:
                return OM_Observation;
            case OBS_TYPE_OM_TRUTH_OBSERVATION:
                return OM_TruthObservation;
            default:
                String mesg = "Unknown ObservationType: " + uri;
                throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                        mesg);
        }
    }

    protected static ObservationType[] values = ObservationType.values();

    public static ObservationType valueOfOrdinal(int ordinal) throws ApplicationException {
        ObservationType t = null;
        try {
            t = values[ordinal];
        } catch(ArrayIndexOutOfBoundsException e) {
            String mesg = "";
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    mesg);
        }
        return t;
    }
}
