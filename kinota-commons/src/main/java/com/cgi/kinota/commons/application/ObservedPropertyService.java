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
import com.cgi.kinota.commons.domain.ObservedProperty;
import com.cgi.kinota.commons.domain.util.Serialization;
import com.cgi.kinota.commons.odata.ODataQuery;

import org.apache.commons.lang3.tuple.Pair;

import javax.json.JsonObject;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Created by bmiles on 2/23/17.
 */
public interface ObservedPropertyService extends QueryableService<ObservedProperty> {

    ObservedProperty findOne(UUID id) throws ApplicationException;

    Pair<Long, List<ObservedProperty>> findAll(ODataQuery q) throws ApplicationException;

    ObservedProperty overwrite(ObservedProperty oldOP, ObservedProperty newOP) throws ApplicationException;

    default ObservedProperty update(ObservedProperty op, JsonObject json) throws ApplicationException {
        boolean dirty = false;
        // Read updated data from JSON object
        try {
            String name = json.getString("name");
            if (name != null) {
                op.setName(name);
                dirty = true;
            }
        } catch (NullPointerException npe) {}

        try {
            String definition = json.getString("definition");
            if (definition != null) {
                op.setDefinition(definition);
                dirty = true;
            }
        } catch (NullPointerException npe) {}

        try {
            String description = json.getString("description");
            if (description != null) {
                op.setDescription(description);
                dirty = true;
            }
        } catch (NullPointerException npe) {}

        if (dirty) {
            op = this.save(op);
        }
        return op;
    }

    ObservedProperty save(ObservedProperty op) throws ApplicationException;

    ObservedProperty create(String name, URI definition, String description);

    default ObservedProperty create(JsonObject json) throws ApplicationException {
        String name = null;
        try {
            name = json.getString("name");
        } catch (NullPointerException npe) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Required attribute 'name' not found in Sensor.");
        }
        URI definition = null;
        try {
            definition = Serialization.stringToURI(json.getString("definition"));
        } catch (NullPointerException npe) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Required attribute 'definition' not found in Sensor.");
        }
        String description = null;
        try {
            description = json.getString("description");
        } catch (NullPointerException npe) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Required attribute 'description' not found in Sensor.");
        }

        // TODO: Parse related entities
        
        return create(name, definition, description);
    }
}
