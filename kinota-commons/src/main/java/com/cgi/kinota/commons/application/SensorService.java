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

package com.cgi.kinota.commons.application;

import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.domain.Sensor;
import com.cgi.kinota.commons.domain.util.Serialization;
import com.cgi.kinota.commons.odata.ODataQuery;

import org.apache.commons.lang3.tuple.Pair;

import javax.activation.MimeType;
import javax.json.JsonObject;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Created by bmiles on 2/9/17.
 */
public interface SensorService extends QueryableService<Sensor> {

    Sensor findOne(UUID id) throws ApplicationException;

    Pair<Long, List<Sensor>> findAll(ODataQuery q) throws ApplicationException;

    Sensor overwrite(Sensor oldSensor, Sensor newSensor) throws ApplicationException;

    default Sensor update(Sensor s, JsonObject json) throws ApplicationException {
        boolean dirty = false;
        // Read updated data from JSON object
        try {
            String name = json.getString("name");
            if (name != null) {
                s.setName(name);
                dirty = true;
            }
        } catch (NullPointerException npe) {}

        try {
            String description = json.getString("description");
            if (description != null) {
                s.setDescription(description);
                dirty = true;
            }
        } catch (NullPointerException npe) {}

        try {
            String encodingType = json.getString("encodingType");
            if (encodingType != null) {
                s.setEncodingType(encodingType);
                dirty = true;
            }
        } catch (NullPointerException npe) {}

        try {
            String metadata = json.getString("metadata");
            if (metadata != null) {
                s.setMetadata(metadata);
                dirty = true;
            }
        } catch (NullPointerException npe) {}

        if (dirty) {
            s = this.save(s);
        }
        return s;
    }

    Sensor save(Sensor sensor) throws ApplicationException;

    Sensor create(String name, String description,
                  MimeType encodingType, URI metadata);

    default Sensor create(JsonObject json) throws ApplicationException {
        String name = null;
        try {
            name = json.getString("name");
        } catch (NullPointerException npe) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Required attribute 'name' not found in Sensor.");
        }
        String description = null;
        try {
            description = json.getString("description");
        } catch (NullPointerException npe) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Required attribute 'description' not found in Sensor.");
        }
        MimeType encodingType = null;
        try {
            encodingType = Serialization.stringToMimeType(json.getString("encodingType"));
        } catch (NullPointerException npe) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Required attribute 'encodingType' not found in Sensor.");
        }
        URI metadata = null;
        try {
            metadata = Serialization.stringToURI(json.getString("metadata"));
        } catch (NullPointerException npe) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Required attribute 'metadata' not found in Sensor.");
        }

        // TODO: Parse related entities

        return create(name, description, encodingType, metadata);
    }
}
