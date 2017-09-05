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

package com.cgi.kinota.commons.domain;

import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.domain.util.Serialization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geojson.GeoJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import java.io.IOException;
import java.util.UUID;

import static com.cgi.kinota.commons.Constants.ANNO_IOT_ID;
import static com.cgi.kinota.commons.Constants.ANNO_IOT_NAV_LINK;
import static com.cgi.kinota.commons.Constants.FEATURE_OF_INTEREST_LOCATION_JSON_ATTR;

/**
 * Created by bmiles on 7/13/17.
 */
public class FeatureOfInterest implements Entity<FeatureOfInterest> {

    private static final Logger logger = LoggerFactory.getLogger(FeatureOfInterest.class);

    public static final String NAME = "FeatureOfInterest";
    public static final String NAME_PLURAL = "FeaturesOfInterest";
    public static final String NAV_LINK_MANY = NAME_PLURAL + ANNO_IOT_NAV_LINK;
    public static final String NAV_LINK = NAME + ANNO_IOT_NAV_LINK;

    public String getEntityName() { return NAME; }
    public String getEntityNamePlural() { return NAME_PLURAL; }

    @JsonProperty(ANNO_IOT_ID)
    protected UUID id;

    protected String name;

    protected String description;

    protected String encodingType;

    @JsonProperty(FEATURE_OF_INTEREST_LOCATION_JSON_ATTR)
    protected String location;

    public FeatureOfInterest() {}

    public FeatureOfInterest(UUID id, String name, String description,
                             MimeType encodingType, GeoJsonObject location) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.encodingType = encodingType.toString();
        try {
            this.location = new ObjectMapper().writeValueAsString(location);
        } catch (JsonProcessingException e) {
            logger.error("Unable to write GeoJSON object to string: " + location.toString());
            this.location = null;
        }
    }

    public void addRelatedEntityLinks(JsonGenerator g) throws ApplicationException {
        String relLink = this.toRelLink();
        try {
            g.writeStringField(Observation.NAV_LINK_MANY, relLink + Observation.NAME_PLURAL);
        } catch (IOException e) {
            throw new ApplicationException(ApplicationErrorCode.E_IO,
                    e.getMessage());
        }
    }

    public void toJsonObject(JsonGenerator g, String urlBase) throws ApplicationException {
        try {
            g.writeStartObject();
            this.generateJsonObjectBuilderWithSelfMetadata(g, urlBase);

            g.writeStringField("name", this.name);
            g.writeStringField("description", this.description);
            g.writeStringField("encodingType", this.encodingType);
            // Location is already in JSON format in the DB, so dump the raw value into the generator.
            String locationStr = ",\"" + FEATURE_OF_INTEREST_LOCATION_JSON_ATTR + "\":" + this.location;
            g.writeRaw(locationStr);

            g.writeEndObject();
        } catch (IOException e) {
            String mesg = "Unable to write FeatureOfInterest to JSON stream due to error: " +
                    e.getMessage();
            logger.error(mesg);
            throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
        }
    }

    /**
     *
     * @param other FeatureOfInterest whose values are to overwrite our own values (ignoring the ID field)
     */
    public void overwrite(FeatureOfInterest other) {
        this.name = other.name;
        this.description = other.description;
        this.encodingType = other.encodingType;
        this.location = other.location;
    }

    @Override
    public String toString() {
        return this.getEntityName() + "{" +
                "id=" + id +
                ", name=" + name +
                ", description=" + description +
                ", encodingType=" + encodingType +
                ", " + FEATURE_OF_INTEREST_LOCATION_JSON_ATTR + "=" + location +
                "}";
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MimeType getEncodingType() throws ApplicationException {
        return Serialization.stringToMimeType(this.encodingType);
    }

    public void setEncodingType(String encodingType) throws ApplicationException {
        MimeType t = Serialization.stringToMimeType(encodingType);
        this.encodingType = t == null ? null : t.toString();
    }

    public void setEncodingType(MimeType encodingType) {
        this.encodingType = encodingType == null ? null : encodingType.toString();
    }

    public GeoJsonObject getLocation() throws ApplicationException {
        return Serialization.stringToGeoJsonObject(this.location);
    }

    public void setLocation(GeoJsonObject location) {
        if (location == null) {
            this.location = null;
            return;
        }
        try {
            this.location = new ObjectMapper().writeValueAsString(location);
        } catch (JsonProcessingException e) {
            logger.error("Unable to write GeoJSON object to string: " + location.toString());
            this.location = null;
        }
    }
}
