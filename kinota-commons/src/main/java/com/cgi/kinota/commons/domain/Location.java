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
import com.cgi.kinota.commons.domain.util.Serialization;
import com.cgi.kinota.commons.application.exception.ApplicationException;

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

/**
 * Created by bmiles on 7/12/17.
 */
public class Location implements Entity<Location> {

    private static final Logger logger = LoggerFactory.getLogger(Location.class);

    public static final String NAME = "Location";
    public static final String NAME_PLURAL = "Locations";
    public static final String NAV_LINK = NAME_PLURAL + ANNO_IOT_NAV_LINK;

    public String getEntityName() { return NAME; }
    public String getEntityNamePlural() { return NAME_PLURAL; }

    @JsonProperty(ANNO_IOT_ID)
    protected UUID id;

    protected String name;

    protected String description;

    protected String encodingType;

    protected String location;

    public Location() {}

    public Location(UUID id, String name, String description,
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
            g.writeStringField(Thing.NAV_LINK_MANY, relLink + Thing.NAME_PLURAL);
            g.writeStringField(HistoricalLocation.NAV_LINK, relLink + HistoricalLocation.NAME_PLURAL);
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
            String locationStr = ",\"location\":" + this.location;
            g.writeRaw(locationStr);

            g.writeEndObject();
        } catch (IOException e) {
            String mesg = "Unable to write Location to JSON stream due to error: " +
                    e.getMessage();
            logger.error(mesg);
            throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
        }
    }

    /**
     *
     * @param other Location whose values are to overwrite our own values (ignoring the ID field)
     */
    public void overwrite(Location other) {
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
                ", location=" + location +
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

    public String getLocationAsString() {
        return this.location;
    }

    public void setLocation(GeoJsonObject location) {
        try {
            this.location = Serialization.geoJsonObjectToString(location);
        } catch (ApplicationException e) {
            logger.error(e.getMessage());
            this.location = null;
        }
    }
}
