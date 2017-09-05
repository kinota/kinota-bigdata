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

import com.cgi.kinota.commons.Constants;
import com.cgi.kinota.commons.Utility;
import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 * Created by bmiles on 7/12/17.
 */
public class HistoricalLocation implements Entity<HistoricalLocation> {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalLocation.class);

    public static final String NAME = "HistoricalLocation";
    public static final String NAME_PLURAL = "HistoricalLocations";
    public static final String NAV_LINK = NAME_PLURAL + Constants.ANNO_IOT_NAV_LINK;

    public String getEntityName() { return NAME; }
    public String getEntityNamePlural() { return NAME_PLURAL; }

    @JsonProperty(Constants.ANNO_IOT_ID)
    protected UUID id;

    protected Date time;

    public HistoricalLocation() {}

    public HistoricalLocation(UUID id, Date time) {
        this.id = id;
        this.time = time;
    }

    public void addRelatedEntityLinks(JsonGenerator g) throws ApplicationException {
        String relLink = this.toRelLink();
        try {
            g.writeStringField(Location.NAV_LINK, relLink + Location.NAME_PLURAL);
            g.writeStringField(Thing.NAV_LINK_ONE, relLink + Thing.NAME);
        } catch (IOException e) {
            throw new ApplicationException(ApplicationErrorCode.E_IO,
                    e.getMessage());
        }
    }

    public void toJsonObject(JsonGenerator g, String urlBase) throws ApplicationException {
        try {
            g.writeStartObject();
            this.generateJsonObjectBuilderWithSelfMetadata(g, urlBase);

            g.writeStringField("time", Utility.getISO8601String(this.time));

            g.writeEndObject();
        } catch (IOException e) {
            String mesg = "Unable to write Historical Location to JSON stream due to error: " +
                    e.getMessage();
            logger.error(mesg);
            throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
        }
    }

    /**
     *
     * @param other HistoricalLocation whose values are to overwrite our own values (ignoring the ID field)
     */
    public void overwrite(HistoricalLocation other) {
        this.time = other.time;
    }

    @Override
    public String toString() {
        return this.getEntityName() + "{" +
                "id=" + id +
                ", time=" + Utility.getISO8601String(time) +
                "}";
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }
}
