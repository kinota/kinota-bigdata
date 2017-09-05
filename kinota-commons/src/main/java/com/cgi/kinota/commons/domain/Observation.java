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
import com.cgi.kinota.commons.application.paging.Paginator;
import com.cgi.kinota.commons.domain.support.ObservationType;
import com.cgi.kinota.commons.domain.util.Serialization;
import com.cgi.kinota.commons.odata.ODataQuery;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by bmiles on 7/13/17.
 */
public class Observation implements Entity<Observation> {

    private static final Logger logger = LoggerFactory.getLogger(Observation.class);

    public static final String NAME = "Observation";
    public static final String NAME_PLURAL = "Observations";
    public static final String NAV_LINK_MANY = NAME_PLURAL + Constants.ANNO_IOT_NAV_LINK;

    public String getEntityName() { return NAME; }
    public String getEntityNamePlural() { return NAME_PLURAL; }

    @JsonProperty(Constants.ANNO_IOT_ID)
    protected UUID id;

    protected UUID datastreamId;

    protected UUID featureOfInterestId;

    protected Date phenomenonTime;

    protected Date phenomenonTimeEnd;

    protected Date resultTime;

    protected Date validTimeBegin;

    protected Date validTimeEnd;

    protected String resultQuality;

    protected Byte observationType;

    // observationTypes: OM_CategoryObservation (URI) and OM_Observation (Any)
    protected String resultString;

    // observationTypes: OM_CountObservation (integer)
    protected Long resultCount;

    // observationTypes: OM_Measurement (double)
    protected Double resultMeasurement;

    // observationTypes: OM_TruthObservation (boolean)
    protected Boolean resultTruth;

    protected Map<String, String> parameters;

    public Observation() {}

    public Observation(UUID id, UUID featureOfInterestId,
                       UUID datastreamId, Date phenomenonTime,
                       Date phenomenonTimeEnd, Date resultTime,
                       Date validTimeBegin, Date validTimeEnd,
                       String resultQuality,
                       ObservationType observationType,
                       String resultString,
                       Long resultCount,
                       Double resultMeasurement,
                       Boolean resultTruth,
                       Map<String, String> parameters) {
        this.id = id;
        this.featureOfInterestId = featureOfInterestId;
        this.datastreamId = datastreamId;
        this.phenomenonTime = phenomenonTime;
        this.phenomenonTimeEnd = phenomenonTimeEnd;
        this.resultTime = resultTime;
        this.validTimeBegin = validTimeBegin;
        this.validTimeEnd = validTimeEnd;
        this.resultQuality = resultQuality;
        this.observationType = (byte) observationType.ordinal();
        this.resultString = resultString;
        this.resultCount = resultCount;
        this.resultMeasurement = resultMeasurement;
        this.resultTruth = resultTruth;
        this.parameters = parameters;
    }

    public Observation(UUID id, UUID featureOfInterestId,
                       UUID datastreamId, Date phenomenonTime,
                       Date phenomenonTimeEnd, Date resultTime,
                       Date validTimeBegin, Date validTimeEnd,
                       String resultQuality,
                       ObservationType observationType,
                       String resultString,
                       Long resultCount,
                       Double resultMeasurement,
                       Boolean resultTruth,
                       String parametersJson) {
        this.id = id;
        this.featureOfInterestId = featureOfInterestId;
        this.datastreamId = datastreamId;
        this.phenomenonTime = phenomenonTime;
        this.phenomenonTimeEnd = phenomenonTimeEnd;
        this.resultTime = resultTime;
        this.validTimeBegin = validTimeBegin;
        this.validTimeEnd = validTimeEnd;
        this.resultQuality = resultQuality;
        this.observationType = (byte) observationType.ordinal();
        this.resultString = resultString;
        this.resultCount = resultCount;
        this.resultMeasurement = resultMeasurement;
        this.resultTruth = resultTruth;

        // De-serialize JSON
        ObjectMapper mapper = new ObjectMapper();
        try {
            this.parameters = mapper.readValue(parametersJson,
                    new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            this.parameters = null;
            logger.error("Error " + e.getLocalizedMessage() + " when trying to de-serialize JSON: " + parametersJson);
        }
    }

    public void addRelatedEntityLinks(JsonGenerator g) throws ApplicationException {
        String relLink = this.toRelLink();
        try {
            g.writeStringField(FeatureOfInterest.NAV_LINK, relLink + FeatureOfInterest.NAME);
            g.writeStringField(Datastream.NAV_LINK, relLink + Datastream.NAME);
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
            g.writeStringField("phenomenonTime", getPhenomenonTimeStr());

            if (this.resultTime != null) {
                g.writeStringField("resultTime", Utility.getISO8601String(this.resultTime));
            }
            if (this.validTimeBegin != null && this.validTimeEnd != null) {
                g.writeStringField("validTime", Serialization.datesToISO8601TimeIntervalString(this.validTimeBegin,
                        this.validTimeEnd));
            }
            if (this.resultQuality != null) {
                g.writeStringField("resultQuality", this.resultQuality);
            }
            // The type of the result depends on the observationType of the associated Datastream
            ObservationType t = ObservationType.valueOfOrdinal(this.observationType);
            switch (t) {
                case OM_Measurement:
                    g.writeNumberField("result", this.resultMeasurement);
                    break;
                case OM_CountObservation:
                    g.writeNumberField("result", this.resultCount);
                    break;
                case OM_TruthObservation:
                    g.writeBooleanField("result", this.resultTruth);
                    break;
                case OM_CategoryObservation:
                case OM_Observation:
                    g.writeStringField("result", this.resultString);
                    break;
            }

            if (this.parameters != null && this.parameters.size() > 0) {
                g.writeArrayFieldStart("parameters");
                for (Map.Entry<String, String> e : parameters.entrySet()) {
                    g.writeStartObject();
                    g.writeStringField(e.getKey(), e.getValue());
                    g.writeEndObject();
                }
                g.writeEndArray();
            }

            g.writeEndObject();
        } catch (IOException e) {
            String mesg = "Unable to write related Observation to JSON stream due to error: " +
                    e.getMessage();
            logger.error(mesg);
            throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
        }
    }

    public static void toJsonDataArray(Pair<Long, List<Observation>> observations, ODataQuery q,
                                       JsonGenerator g,
                                       String requestUrlBase, UUID datastreamId,
                                       Paginator p) throws ApplicationException {
        try {
            g.writeStartObject();
            g.writeStringField(Datastream.NAV_LINK, Serialization.generateRelLink(Datastream.NAME_PLURAL, datastreamId.toString()));

            g.writeArrayFieldStart(Constants.DATA_ARRAY_COMPONENTS_ATTR);
            g.writeString("id");
            g.writeString("phenomenonTime");
            g.writeString("resultTime");
            g.writeString("result");
            g.writeEndArray();

            g.writeNumberField(Constants.ANNO_DATA_ARRAY_COUNT, observations.getLeft());

            g.writeArrayFieldStart(Constants.DATA_ARRAY_ATTR);
            observations.getRight().stream().forEach(o -> toJsonDataArrayElement(g, o));

            g.writeEndArray();

            p.paginate(observations.getLeft(), q, g, requestUrlBase);

            g.writeEndObject();
        } catch (IOException e) {
            String mesg = "Unable to write Observation dataArray to JSON stream due to error: " +
                    e.getMessage();
            logger.error(mesg);
            throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
        }
    }

    public static void toJsonDataArrayElement(JsonGenerator g, Observation o) throws ApplicationException {
        try {
            g.writeStartArray();
            g.writeString(o.getId().toString());
            String phenomenonTimeStr = o.getPhenomenonTime() == null ? "null" : Utility.getISO8601String(o.getPhenomenonTime());
            g.writeString(phenomenonTimeStr);
            String resultTimeStr = o.getResultTime() == null ? "null" : Utility.getISO8601String(o.getResultTime());
            g.writeString(resultTimeStr);
            // The type of the result depends on the observationType of the associated Datastream
            switch (o.getObservationType()) {
                case OM_Measurement:
                    g.writeNumber(o.getResultMeasurement());
                    break;
                case OM_CountObservation:
                    g.writeNumber(o.getResultCount());
                    break;
                case OM_TruthObservation:
                    g.writeBoolean(o.getResultTruth());
                    break;
                case OM_CategoryObservation:
                case OM_Observation:
                    g.writeString(o.getResultString());
                    break;
            }
            g.writeEndArray();
        } catch (IOException e) {
            String mesg = "Unable to write related Observation to JSON stream due to error: " +
                    e.getMessage();
            logger.error(mesg);
            throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
        }
    }

    /**
     * Overwrite values from one Observation with those of another.  Note: Does not change related
     * entities (e.g. Datastreams)
     * @param other Thing whose values are to overwrite our own values (ignoring the ID field)
     */
    public void overwrite(Observation other) {
        this.featureOfInterestId = other.featureOfInterestId;
        this.datastreamId = other.datastreamId;
        this.phenomenonTime = other.phenomenonTime;
        this.phenomenonTimeEnd = other.phenomenonTimeEnd;
        this.resultTime = other.resultTime;
        this.validTimeBegin = other.validTimeBegin;
        this.validTimeEnd = other.validTimeEnd;
        this.resultQuality = other.resultQuality;
        this.observationType = other.observationType;
        this.resultString = other.resultString;
        this.resultCount = other.resultCount;
        this.resultMeasurement = other.resultMeasurement;
        this.resultTruth = other.resultTruth;

        this.parameters = new HashMap<String, String>();
        this.parameters.putAll(other.getParameters());
    }

    @Override
    public String toString() {
        return "Observation{" +
                "id=" + id +
                ", featureOfInterestId=" + featureOfInterestId +
                ", datastreamId=" + datastreamId +
                ", phenomenonTime=" + phenomenonTime +
                ", phenomenonTimeEnd=" + phenomenonTimeEnd +
                ", resultTime=" + resultTime +
                ", validTimeBegin=" + validTimeBegin +
                ", validTimeEnd=" + validTimeEnd +
                ", resultQuality='" + resultQuality + '\'' +
                ", resultString='" + resultString + '\'' +
                ", resultCount=" + resultCount +
                ", resultMeasurement=" + resultMeasurement +
                ", resultTruth=" + resultTruth +
                ", parameters=" + parameters +
                '}';
    }

    public static int comparePhenomenonTimeDesc(Observation o1, Observation o2) {
        int result = o1.getPhenomenonTime().compareTo(o2.getPhenomenonTime());
        if (result != 0) {
            result = -1 * result;
        }
        return result;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getFeatureOfInterestId() {
        return featureOfInterestId;
    }

    public void setFeatureOfInterestId(UUID featureOfInterestId) {
        this.featureOfInterestId = featureOfInterestId;
    }

    public UUID getDatastreamId() {
        return datastreamId;
    }

    public void setDatastreamId(UUID datastreamId) {
        this.datastreamId = datastreamId;
    }

    public String getPhenomenonTimeStr() {
        if (this.phenomenonTimeEnd == null) {
            return Utility.getISO8601String(this.phenomenonTime);
        } else {
            return Serialization.datesToISO8601TimeIntervalString(this.phenomenonTime,
                    this.phenomenonTimeEnd);
        }
    }

    public Date getPhenomenonTime() {
        return phenomenonTime;
    }

    public void setPhenomenonTime(Date phenomenonTime) {
        this.phenomenonTime = phenomenonTime;
    }

    public Date getPhenomenonTimeEnd() {
        return phenomenonTimeEnd;
    }

    public void setPhenomenonTimeEnd(Date phenomenonTimeEnd) {
        this.phenomenonTimeEnd = phenomenonTimeEnd;
    }

    public void setPhenomenonTime(String phenomenonTimeStr) throws ApplicationException {
        try {
            this.phenomenonTime = Serialization.ISO8601DateTimeStringToUTCDate(phenomenonTimeStr);
        } catch (IllegalArgumentException e) {
            // This might be a time interval
            try {
                Pair<Date, Date> dates = Serialization.ISO8601TimeIntervalStringToDates(phenomenonTimeStr);
                phenomenonTime = dates.getLeft();
                phenomenonTimeEnd = dates.getRight();
            } catch (ApplicationException ea) {
                throw new ApplicationException(ApplicationErrorCode.E_JSON,
                        "Unable to parse phenomenonTime interval '" + phenomenonTimeStr + "'.");
            }
        }
    }

    public Date getResultTime() {
        return resultTime;
    }

    public void setResultTime(Date resultTime) {
        this.resultTime = resultTime;
    }

    public Date getValidTimeBegin() {
        return validTimeBegin;
    }

    public void setValidTimeBegin(Date validTimeBegin) {
        this.validTimeBegin = validTimeBegin;
    }

    public Date getValidTimeEnd() {
        return validTimeEnd;
    }

    public void setValidTimeEnd(Date validTimeEnd) {
        this.validTimeEnd = validTimeEnd;
    }

    public String getResultQuality() {
        return resultQuality;
    }

    public void setResultQuality(String resultQuality) {
        this.resultQuality = resultQuality;
    }

    public ObservationType getObservationType() {
        return ObservationType.valueOfOrdinal(observationType);
    }

    public void setObservationType(ObservationType observationType) {
        this.observationType = (byte) observationType.ordinal();
    }

    public String getResultString() { return resultString; }

    public void setResultString(String resultString) { this.resultString = resultString; }

    public Long getResultCount() { return resultCount; }

    public void setResultCount(Long resultCount) { this.resultCount = resultCount; }

    public Double getResultMeasurement() { return resultMeasurement; }

    public void setResultMeasurement(Double resultMeasurement) { this.resultMeasurement = resultMeasurement; }

    public Boolean getResultTruth() { return resultTruth; }

    public void setResultTruth(Boolean resultTruth) { this.resultTruth = resultTruth; }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public Integer getPhenomenonTimeYear() {
        return Utility.getYearForDate(phenomenonTime);
    }
}
