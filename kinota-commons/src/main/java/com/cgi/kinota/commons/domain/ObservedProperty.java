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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import static com.cgi.kinota.commons.Constants.ANNO_IOT_ID;
import static com.cgi.kinota.commons.Constants.ANNO_IOT_NAV_LINK;

/**
 * Created by bmiles on 7/13/17.
 */
public class ObservedProperty implements Entity<ObservedProperty> {

    private static final Logger logger = LoggerFactory.getLogger(ObservedProperty.class);

    public static final String NAME = "ObservedProperty";
    public static final String NAME_PLURAL = "ObservedProperties";
    public static final String NAV_LINK_MANY = NAME_PLURAL + ANNO_IOT_NAV_LINK;
    public static final String NAV_LINK_ONE = NAME + ANNO_IOT_NAV_LINK;

    public String getEntityName() { return NAME; }
    public String getEntityNamePlural() { return NAME_PLURAL; }

    @JsonProperty(ANNO_IOT_ID)
    protected UUID id;

    protected String name;

    protected String definition;

    protected String description;

    public ObservedProperty() {}

    public ObservedProperty(UUID id, String name, URI definition, String description) {
        this.id = id;
        this.name = name;
        this.definition = definition.toString();
        this.description = description;
    }

    public void addRelatedEntityLinks(JsonGenerator g) throws ApplicationException {
        String relLink = this.toRelLink();
        try {
            g.writeStringField(Datastream.NAV_LINK_MANY, relLink + Datastream.NAME_PLURAL);
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
            g.writeStringField("definition", this.definition);
            g.writeStringField("description", this.description);

            g.writeEndObject();
        } catch (IOException e) {
            String mesg = "Unable to write Thing to JSON stream due to error: " +
                    e.getMessage();
            logger.error(mesg);
            throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
        }
    }

    /**
     *
     * @param other ObservedProperty whose values are to overwrite our own values (ignoring the ID field)
     */
    public void overwrite(ObservedProperty other) {
        this.name = other.name;
        this.definition = other.definition;
        this.description = other.description;
    }

    @Override
    public String toString() {
        return this.getEntityName() + "{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", definition='" + definition + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    public UUID getId() { return id; }

    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public URI getDefinition() throws ApplicationException {
        return Serialization.stringToURI(definition);
    }

    public void setDefinition(String definition) throws ApplicationException {
        URI u = Serialization.stringToURI(definition);
        this.definition = u == null ? null : u.toString();
    }

    public void setDefinition(URI definition) {
        this.definition = definition == null ? null : definition.toString();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
