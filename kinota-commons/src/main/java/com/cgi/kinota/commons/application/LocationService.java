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
import com.cgi.kinota.commons.domain.Location;
import com.cgi.kinota.commons.odata.ODataQuery;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.tuple.Pair;
import org.geojson.GeoJsonObject;

import javax.activation.MimeType;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 * Created by bmiles on 1/19/17.
 */
public interface LocationService extends QueryableService<Location> {

    Pair<Long, List<Location>> findAll(ODataQuery q) throws ApplicationException;

    Location overwrite(Location oldLocation, Location newLocation) throws ApplicationException;

    default Location update(Location l, JsonObject json) throws ApplicationException {
        boolean dirty = false;
        // Read updated data from JSON object
        try {
            String name = json.getString("name");
            if (name != null) {
                l.setName(name);
                dirty = true;
            }
        } catch (NullPointerException npe) {}

        try {
            String description = json.getString("description");
            if (description != null) {
                l.setDescription(description);
                dirty = true;
            }
        } catch (NullPointerException npe) {}

        try {
            String encodingType = json.getString("encodingType");
            if (encodingType != null) {
                l.setEncodingType(encodingType);
                dirty = true;
            }
        } catch (NullPointerException npe) {}

        try {
            JsonObject locationJson = json.getJsonObject("location");
            if (locationJson != null) {
                String locationStr = locationJson.toString();

                try {
                    GeoJsonObject location = new ObjectMapper().readValue(new StringReader(locationStr), GeoJsonObject.class);
                    l.setLocation(location);
                    dirty = true;
                } catch (IOException ioe) {
                    String mesg = "Unable to read location '" + locationStr + "'";
                    throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                            mesg);
                }
            }
        } catch (NullPointerException npe) {
            // No location specified, ignore
        }

        if (dirty) {
            l = this.save(l);
        }
        return l;
    }

    Location save(Location location) throws ApplicationException;

    Location create(String name, String description,
                    MimeType encodingType, GeoJsonObject location);
}
