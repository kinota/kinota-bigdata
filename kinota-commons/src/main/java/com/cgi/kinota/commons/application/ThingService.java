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
import com.cgi.kinota.commons.Constants;
import com.cgi.kinota.commons.domain.Location;
import com.cgi.kinota.commons.domain.util.Serialization;
import com.cgi.kinota.commons.domain.Thing;
import com.cgi.kinota.commons.odata.ODataQuery;

import org.apache.commons.lang3.tuple.Pair;

import javax.json.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by bmiles on 12/28/16.
 */
public interface ThingService extends QueryableService<Thing> {

    Thing findOne(UUID id) throws ApplicationException;

    Pair<Long, List<Thing>> findAll(ODataQuery q) throws ApplicationException;

    Thing overwrite(Thing oldThing, Thing newThing) throws ApplicationException;

    Thing update(Thing t, UUID locationUUID) throws ApplicationException;

    default Thing update(Thing t, JsonObject json) throws ApplicationException {
        boolean dirty = false;
        // Read updated data from JSON object
        try {
            String name = json.getString("name");
            if (name != null) {
                t.setName(name);
                dirty = true;
            }
        } catch (NullPointerException npe) {}

        try {
            String description = json.getString("description");
            if (description != null) {
                t.setDescription(description);
                dirty = true;
            }
        } catch (NullPointerException npe) {}

        try {
            JsonObject properties = json.getJsonObject("properties");

            Map<String, String> tp = new HashMap<String, String>(t.getProperties());
            for (String key : properties.keySet()) {
                String value = properties.getString(key);
                if ("".equals(value)) {
                    tp.remove(key);
                } else {
                    tp.put(key, value);

                }
                dirty = true;
            }
            if (dirty) {
                t.setProperties(tp);
            }
        } catch (NullPointerException npe) {
            // No properties, ignore
        } catch (ClassCastException cce) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Improperly formatted properties for Thing.");
        }

        UUID locationUUID = null;
        try {
            JsonObject l = json.getJsonObject("Location");
            String id = l.getString(Constants.ANNO_IOT_ID);
            locationUUID = UUID.fromString(id);
            dirty = true;
        } catch (NullPointerException npe) {}

        if (dirty) {
            t = update(t, locationUUID);
        }
        return t;
    }

    Thing save(Thing t) throws ApplicationException;

    Thing create(UUID locationUUID,
                 String name, String description,
                 Map<String, String> p) throws ApplicationException;

    default Thing create(JsonObject json) throws ApplicationException {
        String name = null;
        try {
            name = json.getString("name");
        } catch (NullPointerException npe) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Required attribute 'name' not found in Thing.");
        }
        String description = null;
        try {
            description = json.getString("description");
        } catch (NullPointerException npe) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Required attribute 'description' not found in Thing.");
        }
        Map<String, String> p = new HashMap<>();
        try {
            JsonObject properties = json.getJsonObject("properties");
            for (String key : properties.keySet()) {
                String value = properties.getString(key);
                p.put(key, value);
            }
        } catch (NullPointerException npe) {
            // No properties, ignore
        } catch (ClassCastException cce) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Improperly formatted properties for Thing.");
        }

        // Parse related entities
        UUID locationUUID = Serialization.getRelatedEntityId(json, Location.NAME);

        return create(locationUUID, name, description, p);
    }
}
