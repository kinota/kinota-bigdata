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
import com.cgi.kinota.commons.domain.FeatureOfInterest;
import com.cgi.kinota.commons.odata.ODataQuery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.geojson.GeoJsonObject;

import javax.activation.MimeType;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.UUID;

/**
 * Created by bmiles on 2/27/17.
 */
public interface FeatureOfInterestService extends QueryableService<FeatureOfInterest> {

    FeatureOfInterest findOne(UUID id) throws ApplicationException;

    Pair<Long, List<FeatureOfInterest>> findAll(ODataQuery q) throws ApplicationException;

    FeatureOfInterest overwrite(FeatureOfInterest oldFoi, FeatureOfInterest newFoi) throws ApplicationException;

    default FeatureOfInterest update(FeatureOfInterest foi, JsonObject json) throws ApplicationException {
        boolean dirty = false;
        // Read updated data from JSON object
        try {
            String name = json.getString("name");
            if (name != null) {
                foi.setName(name);
                dirty = true;
            }
        } catch (NullPointerException npe) {}

        try {
            String description = json.getString("description");
            if (description != null) {
                foi.setDescription(description);
                dirty = true;
            }
        } catch (NullPointerException npe) {}

        try {
            String encodingType = json.getString("encodingType");
            if (encodingType != null) {
                foi.setEncodingType(encodingType);
                dirty = true;
            }
        } catch (NullPointerException npe) {}

        try {
            JsonObject locationJson = json.getJsonObject(Constants.FEATURE_OF_INTEREST_LOCATION_JSON_ATTR);
            if (locationJson != null) {
                String locationStr = locationJson.toString();

                try {
                    GeoJsonObject location = new ObjectMapper().readValue(new StringReader(locationStr), GeoJsonObject.class);
                    foi.setLocation(location);
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
            foi = this.save(foi);
        }
        return foi;
    }

    FeatureOfInterest save(FeatureOfInterest foi) throws ApplicationException;

    FeatureOfInterest create(String name, String description,
                             MimeType encodingType, GeoJsonObject location);
}
