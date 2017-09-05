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

import com.cgi.kinota.commons.Constants;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by bmiles on 7/12/17.
 */
public class Thing implements Entity<Thing> {

    private static final Logger logger = LoggerFactory.getLogger(Thing.class);

    public static final String NAME = "Thing";
    public static final String NAME_PLURAL = "Things";
    public static final String NAV_LINK_MANY = Thing.NAME_PLURAL + Constants.ANNO_IOT_NAV_LINK;
    public static final String NAV_LINK_ONE = Thing.NAME + Constants.ANNO_IOT_NAV_LINK;

    public String getEntityName() { return NAME; }
    public String getEntityNamePlural() { return NAME_PLURAL; }

    @JsonProperty(Constants.ANNO_IOT_ID)
    protected UUID id;

    protected String name;

    protected String description;

    protected Map<String, String> properties;

    public Thing() {}

    public Thing(UUID id, String name, String description, Map<String, String> properties) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.properties = properties;
    }

    public Thing(UUID id, String name, String description, String propertiesJson) {
        this.id = id;
        this.name = name;
        this.description = description;

        // De-serialize JSON
        ObjectMapper mapper = new ObjectMapper();
        try {
            this.properties = mapper.readValue(propertiesJson,
                    new TypeReference<Map<String, String>>() {
                    });
        } catch (Exception e) {
            this.properties = null;
            logger.error("Error " + e.getLocalizedMessage() + " when trying to de-serialize JSON: " + propertiesJson);
        }
    }

    @Override
    public void addRelatedEntityLinks(JsonGenerator g) throws ApplicationException {
        String relLink = this.toRelLink();
        try {
            g.writeStringField(Location.NAV_LINK, relLink + Location.NAME_PLURAL);
            g.writeStringField(Datastream.NAV_LINK_MANY, relLink + Datastream.NAME_PLURAL);
            g.writeStringField(HistoricalLocation.NAV_LINK, relLink + HistoricalLocation.NAME_PLURAL);
        } catch (IOException e) {
            throw new ApplicationException(ApplicationErrorCode.E_IO,
                    e.getMessage());
        }
    }

    @Override
    public void toJsonObject(JsonGenerator g, String urlBase) throws ApplicationException {
        try {
            g.writeStartObject();
            this.generateJsonObjectBuilderWithSelfMetadata(g, urlBase);

            g.writeStringField("name", this.name);
            g.writeStringField("description", this.description);
            if (this.properties != null && this.properties.size() > 0) {
                g.writeObjectFieldStart("properties");
                for (Map.Entry<String, String> e : properties.entrySet()) {
                    g.writeStringField(e.getKey(), e.getValue());
                }
                g.writeEndObject();
            }

            g.writeEndObject();
        } catch (IOException e) {
            String mesg = "Unable to write Thing to JSON stream due to error: " +
                    e.getMessage();
            logger.error(mesg);
            throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
        }
    }

    /**
     * Overwrite values from one Thing with those of another.  Note: Does not change related
     * entities (e.g. Locations)
     * @param other Thing whose values are to overwrite our own values (ignoring the ID field)
     */
    public void overwrite(Thing other) {
        this.name = other.getName();
        this.description = other.getDescription();
        this.properties = new HashMap<String, String>();
        this.properties.putAll(other.getProperties());
    }

    @Override
    public String toString() {
        return this.getEntityName() + "{" +
                "id=" + id +
                ", name=" + name +
                ", description=" + description +
                ", properties=" + properties +
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

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
