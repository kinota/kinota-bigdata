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
import com.cgi.kinota.commons.domain.support.UnitOfMeasurement;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geojson.GeoJsonObject;
import org.geojson.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.cgi.kinota.commons.Constants.ANNO_IOT_ID;
import static com.cgi.kinota.commons.Constants.ANNO_IOT_NAV_LINK;

/**
 * Created by bmiles on 7/12/17.
 */
public class Datastream implements Entity<Datastream> {

    private static final Logger logger = LoggerFactory.getLogger(Datastream.class);

    public static final String NAME = "Datastream";
    public static final String NAME_PLURAL = "Datastreams";
    public static final String NAV_LINK_MANY = NAME_PLURAL + ANNO_IOT_NAV_LINK;
    public static final String NAV_LINK = NAME + ANNO_IOT_NAV_LINK;

    public String getEntityName() { return NAME; }
    public String getEntityNamePlural() { return NAME_PLURAL; }

    @JsonProperty(ANNO_IOT_ID)
    protected UUID id;

    protected UUID thingId;

    protected UUID sensorId;

    protected UUID observedPropertyId;

    protected String name;

    protected String description;

    protected Map<String, String> unitOfMeasurement;

    protected String observationType;

    protected String observedArea;

    protected Date phenomenonTimeBegin;

    protected Date phenomenonTimeEnd;

    protected Date resultTimeBegin;

    protected Date resultTimeEnd;

    public Datastream() {}

    public Datastream(UUID id, UUID thingId, UUID sensorId, UUID observedPropertyId,
                      String name, String description,
                      UnitOfMeasurement unitOfMeasurement,
                      URI observationType, GeoJsonObject observedArea,
                      Date phenomenonTimeBegin, Date phenomenonTimeEnd,
                      Date resultTimeBegin, Date resultTimeEnd) throws ApplicationException {
        this.id = id;
        this.thingId = thingId;
        this.sensorId = sensorId;
        this.observedPropertyId = observedPropertyId;

        this.name = name;
        this.description = description;
        this.setUnitOfMeasurement(unitOfMeasurement);
        this.setObservationType(observationType);
        if (observedArea != null) {
            try {
                this.observedArea = new ObjectMapper().writeValueAsString(observedArea);
            } catch (JsonProcessingException e) {
                logger.error("Unable to write GeoJSON object to string: " + observedArea.toString());
                this.observedArea = null;
            }
        }

        this.phenomenonTimeBegin = phenomenonTimeBegin;
        this.phenomenonTimeEnd = phenomenonTimeEnd;
        this.resultTimeBegin = resultTimeBegin;
        this.resultTimeEnd = resultTimeEnd;
    }

    public void addRelatedEntityLinks(JsonGenerator g) throws ApplicationException {
        String relLink = this.toRelLink();
        try {
            g.writeStringField(Thing.NAV_LINK_ONE, relLink + Thing.NAME);
            g.writeStringField(Sensor.NAV_LINK_ONE, relLink + Sensor.NAME);
            g.writeStringField(ObservedProperty.NAV_LINK_ONE, relLink + ObservedProperty.NAME);
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

            // Data
            g.writeStringField("name", this.name);
            g.writeStringField("description", this.description);

            if (this.unitOfMeasurement.size() > 0) {
                g.writeObjectFieldStart("unitOfMeasurement");
                for (Map.Entry<String, String> e : unitOfMeasurement.entrySet()) {
                    g.writeStringField(e.getKey(), e.getValue());
                }
                g.writeEndObject();
            }

            g.writeStringField("observationType", this.observationType);

            // observedArea is already in JSON format in the DB, so dump the raw value into the generator.
            String obsAreaStr = ",\"observedArea\":" + this.observedArea;
            g.writeRaw(obsAreaStr);

            if (this.phenomenonTimeBegin != null && this.phenomenonTimeEnd != null) {
                g.writeStringField("phenomenonTime", Serialization.datesToISO8601TimeIntervalString(this.phenomenonTimeBegin,
                        this.phenomenonTimeEnd));
            }

            if (this.resultTimeBegin != null && this.resultTimeEnd != null) {
                g.writeStringField("resultTime", Serialization.datesToISO8601TimeIntervalString(this.resultTimeBegin,
                        this.resultTimeEnd));
            }

            g.writeEndObject();
        } catch (IOException e) {
            String mesg = "Unable to write Observation to JSON stream due to error: " +
                    e.getMessage();
            logger.error(mesg);
            throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
        }
    }

    /**
     * Overwrite values from one thing with those of another.  Note: Does not update phenomenon
     * or result times, or related entities.
     * @param other Thing whose values are to overwrite our own values (ignoring the ID field)
     */
    public void overwrite(Datastream other) {
        this.name = other.name;
        this.description = other.description;
        this.setUnitOfMeasurement(other.getUnitOfMeasurement());
        this.observationType = other.observationType;
        this.observedArea = other.observedArea;
    }

    @Override
    public String toString() {
        return "Datastream{" +
                "id=" + id +
                ", thingId=" + thingId +
                ", sensorId=" + sensorId +
                ", observedPropertyId=" + observedPropertyId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", unitOfMeasurement=" + unitOfMeasurement +
                ", observationType='" + observationType + '\'' +
                ", observedArea='" + observedArea + '\'' +
                ", phenomenonTimeBegin=" + phenomenonTimeBegin +
                ", phenomenonTimeEnd=" + phenomenonTimeEnd +
                ", resultTimeBegin=" + resultTimeBegin +
                ", resultTimeEnd=" + resultTimeEnd +
                '}';
    }

    public UUID getId() {
        return this.id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getThingId() {
        return this.thingId;
    }

    public void setThingId(UUID thingId) {
        this.thingId = thingId;
    }

    public UUID getSensorId() {
        return sensorId;
    }

    public void setSensorId(UUID sensorId) {
        this.sensorId = sensorId;
    }

    public UUID getObservedPropertyId() {
        return observedPropertyId;
    }

    public void setObservedPropertyId(UUID observedPropertyId) {
        this.observedPropertyId = observedPropertyId;
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

    public UnitOfMeasurement getUnitOfMeasurement() {
        UnitOfMeasurement u = null;
        if (this.unitOfMeasurement != null) {
            u = new UnitOfMeasurement();
            u.setName(this.unitOfMeasurement.get("name"));
            u.setSymbol(this.unitOfMeasurement.get("symbol"));
            u.setDefinition(URI.create(this.unitOfMeasurement.get("definition")));
        }
        return u;
    }

    public void setUnitOfMeasurement(UnitOfMeasurement u) {
        this.unitOfMeasurement = new LinkedHashMap<>();
        this.unitOfMeasurement.put("name", u.getName());
        this.unitOfMeasurement.put("symbol", u.getSymbol());
        this.unitOfMeasurement.put("definition", u.getDefinition().toString());
    }

    public URI getObservationType() {
        return observationType == null ? null : URI.create(observationType);
    }

    public void setObservationType(URI observationType) {
        this.observationType = observationType == null ? null : observationType.toString();
    }

    public Polygon getObservedArea() throws ApplicationException {
        return Serialization.stringToGeoJsonPolygon(this.observedArea);
    }

    public void setObservedArea(GeoJsonObject observedArea) {
        try {
            this.observedArea = Serialization.geoJsonObjectToString(observedArea);
        } catch (ApplicationException e) {
            logger.error(e.getMessage());
            this.observedArea = null;
        }
    }

    public Date getPhenomenonTimeBegin() {
        return phenomenonTimeBegin;
    }

    public void setPhenomenonTimeBegin(Date phenomenonTimeBegin) {
        this.phenomenonTimeBegin = phenomenonTimeBegin;
    }

    public Date getPhenomenonTimeEnd() {
        return phenomenonTimeEnd;
    }

    public void setPhenomenonTimeEnd(Date phenomenonTimeEnd) {
        this.phenomenonTimeEnd = phenomenonTimeEnd;
    }

    public Date getResultTimeBegin() {
        return resultTimeBegin;
    }

    public void setResultTimeBegin(Date resultTimeBegin) {
        this.resultTimeBegin = resultTimeBegin;
    }

    public Date getResultTimeEnd() {
        return resultTimeEnd;
    }

    public void setResultTimeEnd(Date resultTimeEnd) {
        this.resultTimeEnd = resultTimeEnd;
    }
}
