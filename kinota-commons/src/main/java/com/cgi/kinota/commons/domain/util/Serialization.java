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

package com.cgi.kinota.commons.domain.util;

import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.Utility;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.geojson.GeoJsonObject;
import org.geojson.Point;
import org.geojson.Polygon;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.UUID;

import static com.cgi.kinota.commons.Constants.ANNO_IOT_ID;

/**
 * Created by bmiles on 2/9/17.
 */
public class Serialization {

    private static final Logger logger = LoggerFactory.getLogger(Serialization.class);

    public static String generateRelLink(String entityNamePlural, String entityId) {
        return entityNamePlural + "(" + entityId + ")";
    }

    public static MimeType stringToMimeType(String string) throws ApplicationException {
        if (string == null) {
            return null;
        }
        try {
            return new MimeType(string);
        } catch (MimeTypeParseException e) {
            String mesg = string + " is not a valid/recognized MIME type.";
            logger.error(mesg);
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    mesg);
        }
    }

    public static URI stringToURI(String string) throws ApplicationException {
        URI uri = null;

        try {
            uri = new URI(string);
        } catch (NullPointerException npe) {
            // Do nothing, just return null
        } catch (URISyntaxException e) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Unable to parse URI '" + string + "'.");
        }

        return uri;
    }

    public static String datesToISO8601TimeIntervalString(Date begin, Date end) {
        DateTime b = new DateTime(begin, DateTimeZone.UTC);
        DateTime e = new DateTime(end, DateTimeZone.UTC);
        DateTimeFormatter f = Utility.getISO8601Formatter();
        return f.print(b) + "/" + f.print(e);
    }

    public static Pair<Date, Date> ISO8601TimeIntervalStringToDates(String timeIntervalString) throws ApplicationException {
        String[] dateStr = timeIntervalString.split("/");
        if (dateStr.length != 2) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid, "Unable to parse time interval '" + timeIntervalString + "'.");
        }
        Date begin = null;
        try {
            DateTime dt = new DateTime(dateStr[0]);
            begin = dt.toDate();
        } catch (IllegalArgumentException e) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid, "Unable to parse time interval '" + timeIntervalString + "'.");
        }
        Date end = null;
        try {
            DateTime dt = new DateTime(dateStr[1]);
            end = dt.toDate();
        } catch (IllegalArgumentException e) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid, "Unable to parse time interval '" + timeIntervalString + "'.");
        }
        return new ImmutablePair<>(begin, end);
    }

    public static Date ISO8601DateTimeStringToUTCDate(String timeStr) {
        DateTime dt = new DateTime(timeStr, DateTimeZone.UTC);
        return dt.toDate();
    }

    public static String geoJsonObjectToString(GeoJsonObject geo) throws ApplicationException {
        if (geo == null) { return null; }
        try {
            return new ObjectMapper().writeValueAsString(geo);
        } catch (JsonProcessingException e) {
            String mesg = "Unable to write GeoJSON object to string: " + geo.toString();
            throw new ApplicationException(ApplicationErrorCode.E_JSON,
                    mesg);
        }
    }

    public static GeoJsonObject stringToGeoJsonObject(String geo) throws ApplicationException {
        if (geo == null) { return null; }
        try {
            return new ObjectMapper().readValue(new StringReader(geo), GeoJsonObject.class);
        } catch (JsonParseException | JsonMappingException e) {
            String mesg = "Error parsing JSON: " + geo;
            throw new ApplicationException(ApplicationErrorCode.E_JSON,
                    mesg);
        } catch (IOException e){
            String mesg = "Error reading JSON: " + geo;
            throw new ApplicationException(ApplicationErrorCode.E_IO,
                    mesg);
        }
    }

    public static Point stringToGeoJsonPoint(String geo) throws ApplicationException {
        if (geo == null) { return null; }
        try {
            return new ObjectMapper().readValue(new StringReader(geo), Point.class);
        } catch (JsonParseException | JsonMappingException e) {
            String mesg = "Error parsing JSON: " + geo;
            throw new ApplicationException(ApplicationErrorCode.E_JSON,
                    mesg);
        } catch (IOException e){
            String mesg = "Error reading JSON: " + geo;
            throw new ApplicationException(ApplicationErrorCode.E_IO,
                    mesg);
        }
    }

    public static Polygon stringToGeoJsonPolygon(String geo) throws ApplicationException {
        if (geo == null) { return null; }
        try {
            return new ObjectMapper().readValue(new StringReader(geo), Polygon.class);
        } catch (JsonParseException | JsonMappingException e) {
            String mesg = "Error parsing JSON: " + geo;
            throw new ApplicationException(ApplicationErrorCode.E_JSON,
                    mesg);
        } catch (IOException e){
            String mesg = "Error reading JSON: " + geo;
            throw new ApplicationException(ApplicationErrorCode.E_IO,
                    mesg);
        }
    }

    public static UUID getRelatedEntityId(JsonObject json,
                                          String entityName) {
        UUID uuid = null;
        try {
            JsonObject l = json.getJsonObject(entityName);
            if (l != null) {
                String id = l.getString(ANNO_IOT_ID);
                uuid = UUID.fromString(id);
            }
        } catch (NullPointerException npe) {}

        return uuid;
    }
}
