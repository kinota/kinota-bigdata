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

import com.cgi.kinota.commons.Constants;
import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.domain.Datastream;
import com.cgi.kinota.commons.domain.ObservedProperty;
import com.cgi.kinota.commons.domain.Sensor;
import com.cgi.kinota.commons.domain.Thing;
import com.cgi.kinota.commons.domain.support.ObservationType;
import com.cgi.kinota.commons.domain.support.UnitOfMeasurement;
import com.cgi.kinota.commons.domain.util.Serialization;
import com.cgi.kinota.commons.odata.ODataQuery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.geojson.GeoJsonObject;

import javax.json.JsonObject;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Created by bmiles on 2/28/17.
 */
public interface DatastreamService extends QueryableService<Datastream> {

    Pair<Long, List<Datastream>> findAll(ODataQuery q) throws ApplicationException;

    Datastream overwrite(Datastream oldDs, Datastream newDs) throws ApplicationException;

    Datastream update(Datastream d, UUID thingUUID, UUID sensorUUID, UUID observedPropertyUUID);

    default Datastream update(Datastream d, JsonObject json) throws ApplicationException {
        boolean dirty = false;
        // Read updated data from JSON object
        try {
            String name = json.getString("name");
            if (name != null) {
                d.setName(name);
                dirty = true;
            }
        } catch (NullPointerException npe) {}

        try {
            String description = json.getString("description");
            if (description != null) {
                d.setDescription(description);
                dirty = true;
            }
        } catch (NullPointerException npe) {}

        UnitOfMeasurement u = null;
        try {
            JsonObject uJson = json.getJsonObject("unitOfMeasurement");
            u = new UnitOfMeasurement();
            try {
                u.setName(uJson.getString("name"));
            } catch (NullPointerException npe) {
                throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                        "Unable to read 'name' attribute from UnitOfMeasurement JSON: " + uJson.toString());
            }
            try {
                u.setSymbol(uJson.getString("symbol"));
            } catch (NullPointerException npe) {
                throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                        "Unable to read 'symbol' attribute from UnitOfMeasurement JSON: " + uJson.toString());
            }
            try {
                u.setDefinition(URI.create(uJson.getString("definition")));
            } catch (NullPointerException npe) {
                throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                        "Unable to read 'definition' attribute from UnitOfMeasurement JSON: " + uJson.toString());
            }
            d.setUnitOfMeasurement(u);
            dirty = true;
        } catch (NullPointerException npe) {
            // No unitOfMeasurement specified, ignore
        }

        try {
            // TODO: Do not allow transition from one observationType to an
            //   incompatible observationType (e.g. from OM_CategoryObservation (URI) to
            //   OM_Measurement (double).  Doing so may break retrieval of existing Observations.
            String observationType = json.getString("observationType");
            if (observationType != null) {
                d.setObservationType(URI.create(observationType));
                dirty = true;
            }
        } catch (NullPointerException npe) {
            // No observationType specified, ignore
        } catch (IllegalArgumentException iae) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Malformed 'observationType' URI found in Datastream: " + json.toString());
        }

        try {
            JsonObject observedAreaJson = json.getJsonObject("observedArea");
            if (observedAreaJson != null) {
                String observedAreaStr = observedAreaJson.toString();
                try {
                    GeoJsonObject observedArea = new ObjectMapper().readValue(new StringReader(observedAreaStr), GeoJsonObject.class);
                    d.setObservedArea(observedArea);
                    dirty = true;
                } catch (IOException ioe) {
                    String mesg = "Unable to read observedArea '" + observedAreaStr + "'";
                    throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                            mesg);
                }
            }
        } catch (NullPointerException npe) {
            // No observedArea specified, ignore
        }

        // Do not allow updating of phenomenonTime and resultTime as these are inferred
        //   from the Observations associated with the Datastream

        // Parse related entities
        UUID thingUUID = null;
        try {
            JsonObject t = json.getJsonObject("Thing");
            String id = t.getString(Constants.ANNO_IOT_ID);
            thingUUID = UUID.fromString(id);
            dirty = true;
        } catch (NullPointerException npe) {}

        UUID sensorUUID = null;
        try {
            JsonObject s = json.getJsonObject("Sensor");
            String id = s.getString(Constants.ANNO_IOT_ID);
            sensorUUID = UUID.fromString(id);
            dirty = true;
        } catch (NullPointerException npe) {}

        UUID observedPropertyUUID = null;
        try {
            JsonObject o = json.getJsonObject("ObservedProperty");
            String id = o.getString(Constants.ANNO_IOT_ID);
            observedPropertyUUID = UUID.fromString(id);
            dirty = true;
        } catch (NullPointerException npe) {}

        if (dirty) {
            d = update(d, thingUUID, sensorUUID, observedPropertyUUID);
        }
        return d;
    }

    Datastream save(Datastream d) throws ApplicationException;

    Datastream create(UUID thingId, UUID sensorId, UUID observedPropertyId,
                      String name, String description,
                      UnitOfMeasurement unitOfMeasurement,
                      URI observationType, GeoJsonObject observedArea) throws ApplicationException;

    default Datastream create(JsonObject json) throws ApplicationException {
        String name = null;
        try {
            name = json.getString("name");
        } catch (NullPointerException npe) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Required attribute 'name' not found in Datastream: " + json.toString());
        }
        String description = null;
        try {
            description = json.getString("description");
        } catch (NullPointerException npe) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Required attribute 'description' not found in Datastream: " + json.toString());
        }

        UnitOfMeasurement u = null;
        try {
            JsonObject uJson = json.getJsonObject("unitOfMeasurement");
            u = new UnitOfMeasurement();
            try {
                u.setName(uJson.getString("name"));
            } catch (NullPointerException npe) {
                throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                        "Unable to read 'name' attribute from UnitOfMeasurement when creating Datastream: " + json.toString());
            }
            try {
                u.setSymbol(uJson.getString("symbol"));
            } catch (NullPointerException npe) {
                throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                        "Unable to read 'symbol' attribute from UnitOfMeasurement when creating Datastream: " + json.toString());
            }
            try {
                u.setDefinition(URI.create(uJson.getString("definition")));
            } catch (NullPointerException npe) {
                throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                        "Unable to read 'definition' attribute from UnitOfMeasurement when creating Datastream: " + json.toString());
            }
        } catch (NullPointerException npe) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Required attribute 'unitOfMeasurement' not found when creating Datastream: " + json.toString());
        }

        URI observationType = null;
        try {
            String observationTypeStr = json.getString("observationType");
            if (observationTypeStr != null) {
                observationType = URI.create(observationTypeStr);
                // Validate ObservationType
                ObservationType ot = ObservationType.valueOfUri(observationType);
            }
        } catch (NullPointerException npe) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Required attribute 'observationType' not found when creating Datastream: " + json.toString());
        } catch (IllegalArgumentException iae) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Malformed 'observationType' URI found when creating Datastream: " + json.toString());
        }

        GeoJsonObject observedArea = null;
        try {
            JsonObject observedAreaJson = json.getJsonObject("observedArea");
            if (observedAreaJson != null) {
                String observedAreaStr = observedAreaJson.toString();
                try {
                    observedArea = new ObjectMapper().readValue(new StringReader(observedAreaStr), GeoJsonObject.class);
                } catch (IOException ioe) {
                    String mesg = "Unable to read observedArea '" + observedAreaStr + "' when creating Datastream: " + json.toString();
                    throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                            mesg);
                }
            }
        } catch (NullPointerException npe) {
            // No observedArea specified, ignore
        }

        // Parse related entities
        UUID thingId = Serialization.getRelatedEntityId(json, Thing.NAME);
        UUID sensorId = Serialization.getRelatedEntityId(json, Sensor.NAME);
        UUID observedPropertyId = Serialization.getRelatedEntityId(json, ObservedProperty.NAME);

        return create(thingId, sensorId, observedPropertyId,
                name, description, u,
                observationType, observedArea);

    }
}
