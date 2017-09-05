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
import com.cgi.kinota.commons.Utility;
import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.domain.Datastream;
import com.cgi.kinota.commons.domain.FeatureOfInterest;
import com.cgi.kinota.commons.domain.Observation;
import com.cgi.kinota.commons.domain.support.ObservationType;
import com.cgi.kinota.commons.domain.util.Serialization;
import com.cgi.kinota.commons.odata.ODataQuery;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by bmiles on 3/20/17.
 */
public interface ObservationService extends QueryableService<Observation> {

    public static List<String> REQUIRED_DATA_ARRAY_COMPONENTS = Arrays.asList(new String[] {"phenomenonTime", "result"});
    public static List<String> SUPPORTED_DATA_ARRAY_COMPONENTS = Arrays.asList(new String[] {"phenomenonTime", "result", "parameters", Constants.DATA_ARRAY_FEATURE_OF_INTEREST_ATTR});

    Pair<Long, List<Observation>> findAll(ODataQuery q) throws ApplicationException;

    Observation overwrite(Observation oldObservation, Observation newObservation) throws ApplicationException;

    default Observation update(Observation o, JsonObject json) throws ApplicationException {
        // TODO: This method is incomplete as it does not properly handle updating
        //   related entities.  It should be safe to treat observations as immutable
        //   for now though.
        boolean dirty = false;
        // Read updated data from JSON data
        try {
            Date phenomenonTime = null;
            Date phenomenonTimeEnd = null;
            DateTimeFormatter f = Utility.getISO8601Formatter();
            String phenomenonTimeStr = json.getString("phenomenonTime");
            if (phenomenonTimeStr != null) {
                try {
                    DateTime dt = f.parseDateTime(phenomenonTimeStr);
                    phenomenonTime = dt.toDate();
                    o.setPhenomenonTime(phenomenonTime);
                    // TODO: Update phenomenonTime in RelatedObservation
                    dirty = true;
                } catch (IllegalArgumentException e) {
                    // This might be a time interval
                    try {
                        Pair<Date, Date> dates = Serialization.ISO8601TimeIntervalStringToDates(phenomenonTimeStr);
                        phenomenonTime = dates.getLeft();
                        phenomenonTimeEnd = dates.getRight();
                        o.setPhenomenonTime(phenomenonTime);
                        // TODO: Update phenomenonTime in RelatedObservation
                        o.setPhenomenonTimeEnd(phenomenonTimeEnd);
                        dirty = true;
                    } catch (ApplicationException ea) {
                        throw new ApplicationException(ApplicationErrorCode.E_JSON,
                                "Unable to parse phenomenonTime '" + phenomenonTimeStr + "'.");
                    }
                }
            }
        } catch (NullPointerException npe) {}

        try {
            Date resultTime = null;
            DateTimeFormatter f = Utility.getISO8601Formatter();
            String resultTimeStr = json.getString("resultTime");
            if (resultTimeStr != null) {
                try {
                    DateTime dt = f.parseDateTime(resultTimeStr);
                    resultTime = dt.toDate();
                    o.setResultTime(resultTime);
                    dirty = true;
                } catch (IllegalArgumentException e) {
                    throw new ApplicationException(ApplicationErrorCode.E_JSON,
                            "Unable to parse resultTime '" + resultTimeStr + "'.");
                }
            }
        } catch (NullPointerException npe) {}

        try {
            Date validTimeBegin = null;
            Date validTimeEnd = null;
            String validTimeStr = json.getString("validTime");
            if (validTimeStr != null) {
                try {
                    Pair<Date, Date> dates = Serialization.ISO8601TimeIntervalStringToDates(validTimeStr);
                    validTimeBegin = dates.getLeft();
                    validTimeEnd = dates.getRight();
                    o.setValidTimeBegin(validTimeBegin);
                    o.setValidTimeEnd(validTimeEnd);
                    dirty = true;
                } catch (ApplicationException ea) {
                    throw new ApplicationException(ApplicationErrorCode.E_JSON,
                            "Unable to parse valitTime '" + validTimeStr + "'.");
                }
            }
        } catch (NullPointerException npe) {}

        try {
            String resultQuality = json.getString("resultQuality");
            if (resultQuality != null) {
                o.setResultQuality(resultQuality);
                dirty = true;
            }
        } catch (NullPointerException npe) {}

        try {
            // The type of the result depends on the observationType of the associated Datastream
            switch (o.getObservationType()) {
                case OM_Measurement:
                    try {
                        o.setResultMeasurement(json.getJsonNumber("result").doubleValue());
                        dirty = true;
                    } catch (ClassCastException e) {
                        String mesg = "Expected 'result' to be of type double in Observation: " + json.toString();
                        throw new ApplicationException(ApplicationErrorCode.E_JSON,
                                mesg);
                    }
                    break;
                case OM_CountObservation:
                    try {
                        o.setResultCount(json.getJsonNumber("result").longValue());
                        dirty = true;
                    } catch (ClassCastException e) {
                        String mesg = "Expected 'result' to be of type integer in Observation: " + json.toString();
                        throw new ApplicationException(ApplicationErrorCode.E_JSON,
                                mesg);
                    }
                    break;
                case OM_TruthObservation:
                    try {
                        o.setResultTruth(json.getBoolean("result"));
                        dirty = true;
                    } catch (ClassCastException e) {
                        String mesg = "Expected 'result' to be of type boolean in Observation: " + json.toString();
                        throw new ApplicationException(ApplicationErrorCode.E_JSON,
                                mesg);
                    }
                    break;
                case OM_CategoryObservation:
                case OM_Observation:
                    o.setResultString(json.getString("result"));
                    dirty = true;
                    break;
            }
        } catch (NullPointerException npe) {}

        try {
            JsonArray parameters = json.getJsonArray("parameters");
            Map<String, String> op = new HashMap<String, String>(o.getParameters());
            for (int i = 0; i < parameters.size(); i++) {
                JsonObject obj = parameters.getJsonObject(i);
                if (obj.size() != 1) {
                    throw new ApplicationException(ApplicationErrorCode.E_JSON,
                            "Unable to update parameters '" + parameters.toString() +
                                    "' because parameter '" + obj.toString() +
                                    "' must only contain one parameter.");
                }
                for (String key : obj.keySet()) {
                    String value = obj.getString(key);
                    if ("".equals(value)) {
                        op.remove(key);
                    } else {
                        op.put(key, value);

                    }
                    dirty = true;
                }
            }
            if (dirty) {
                o.setParameters(op);
            }
        } catch (NullPointerException npe) {
            // No parameters, ignore
        } catch (ClassCastException cce) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Improperly formatted parameters for Observation.");
        }

        // TODO: Parse related entities

        if (dirty) {
            o = this.save(o);
        }
        return o;
    }

    Observation save(Observation o) throws ApplicationException;

    public Observation create(Datastream d, UUID featureOfInterestId,
                              Date phenomenonTime,
                              Date phenomenonTimeEnd, Date resultTime,
                              Date validTimeBegin, Date validTimeEnd,
                              String resultQuality,
                              ObservationType observationType,
                              String resultString,
                              Long resultCount,
                              Double resultMeasurement,
                              Boolean resultTruth,
                              Map<String, String> parameters) throws ApplicationException;

    default Observation create(Datastream d, JsonObject json) throws ApplicationException {
        Date phenomenonTime = null;
        Date phenomenonTimeEnd = null;
        try {
            String phenomenonTimeStr = json.getString("phenomenonTime");
            if (phenomenonTimeStr != null) {
                try {
                    phenomenonTime = Serialization.ISO8601DateTimeStringToUTCDate(phenomenonTimeStr);
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
        } catch (NullPointerException npe) {
            // A client may omit phenonmenonTime when POST new Observations, even
            // though phenonmenonTime is a mandatory property. When a SensorThings service
            // receives a POST Observations without phenonmenonTime, the service SHALL
            // assign the current server time to the value of the phenomenonTime.
            phenomenonTime = Utility.getCurrentTimeUTC();
        }

        Date resultTime = null;
        try {
            DateTimeFormatter f = Utility.getISO8601Formatter();
            String resultTimeStr = json.getString("resultTime");
            if (resultTimeStr != null) {
                try {
                    DateTime dt = f.parseDateTime(resultTimeStr);
                    resultTime = dt.toDate();
                } catch (IllegalArgumentException e) {
                    throw new ApplicationException(ApplicationErrorCode.E_JSON,
                            "Unable to parse resultTime '" + resultTimeStr + "'.");
                }
            }
        } catch (NullPointerException npe) {
            // Just allow resultTime to be null even though the spec says it is required:
            // Note: Many resource-constrained sensing devices do not have a clock.
            // As a result, a client may omit resultTime when POST new Observations,
            // even though resultTime is a mandatory property. When a SensorThings
            // service receives a POST Observations without resultTime, the service
            // SHALL assign a null value to the resultTime.
        }

        Date validTimeBegin = null;
        Date validTimeEnd = null;
        try {
            String validTimeStr = json.getString("validTime");
            if (validTimeStr != null) {
                try {
                    Pair<Date, Date> dates = Serialization.ISO8601TimeIntervalStringToDates(validTimeStr);
                    validTimeBegin = dates.getLeft();
                    validTimeEnd = dates.getRight();
                } catch (ApplicationException ea) {
                    throw new ApplicationException(ApplicationErrorCode.E_JSON,
                            "Unable to parse validTime '" + validTimeStr + "'.");
                }
            }
        } catch (NullPointerException npe) {}

        String resultQuality = null;
        try {
            resultQuality = json.getString("resultQuality");
        } catch (NullPointerException npe) {}

        Double resultMeasurement = null;
        Long resultCount = null;
        Boolean resultTruth = null;
        String resultString = null;
        ObservationType observationType = ObservationType.valueOfUri(d.getObservationType());
        try {
            // The type of the result depends on the observationType of the associated Datastream
            switch (observationType) {
                case OM_Measurement:
                    try {
                        resultMeasurement = json.getJsonNumber("result").doubleValue();
                    } catch (ClassCastException e) {
                        String mesg = "Expected 'result' to be of type double in Observation: " + json.toString();
                        throw new ApplicationException(ApplicationErrorCode.E_JSON,
                                mesg);
                    }
                    break;
                case OM_CountObservation:
                    try {
                        JsonNumber num = json.getJsonNumber("result");
                        if (!num.isIntegral()) {
                            throw new ClassCastException();
                        }
                        resultCount = num.longValue();
                    } catch (ClassCastException e) {
                        String mesg = "Expected 'result' to be of type integer in Observation: " + json.toString();
                        throw new ApplicationException(ApplicationErrorCode.E_JSON,
                                mesg);
                    }
                    break;
                case OM_TruthObservation:
                    try {
                        resultTruth = json.getBoolean("result");
                    } catch (ClassCastException e) {
                        String mesg = "Expected 'result' to be of type boolean in Observation: " + json.toString();
                        throw new ApplicationException(ApplicationErrorCode.E_JSON,
                                mesg);
                    }
                    break;
                case OM_CategoryObservation:
                    String tmpResult = json.getString("result");
                    // Make sure result is a valid URI
                    try {
                        URI tmpUri = URI.create(tmpResult);
                        resultString = tmpResult;
                    } catch (IllegalArgumentException e) {
                        String mesg = "Expected 'result' to be of type URI in Observation: " + json.toString();
                        throw new ApplicationException(ApplicationErrorCode.E_JSON,
                                mesg);
                    }
                    break;
                case OM_Observation:
                    resultString = json.getString("result");
                    break;
            }
        } catch (NullPointerException npe) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Required attribute 'result' not found in Observation.");
        }

        Map<String, String> p = new HashMap<>();
        try {
            JsonArray parameters = json.getJsonArray("parameters");
            for (int i = 0; i < parameters.size(); i++) {
                JsonObject obj = parameters.getJsonObject(i);
                if (obj.size() != 1) {
                    throw new ApplicationException(ApplicationErrorCode.E_JSON,
                            "Unable to create Object because parameter '" + obj.toString() +
                                    "' must only contain one parameter.");
                }
                for (String key : obj.keySet()) {
                    String value = obj.getString(key);
                    p.put(key, value);
                }
            }
        } catch (NullPointerException npe) {
            // No parameters, ignore
        } catch (ClassCastException cce) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Improperly formatted parameters for Observation.");
        }

        // Parse related entities
        UUID featureOfInterestUUID = Serialization.getRelatedEntityId(json, FeatureOfInterest.NAME);

        return create(d, featureOfInterestUUID,
                phenomenonTime,
                phenomenonTimeEnd, resultTime,
                validTimeBegin, validTimeEnd,
                resultQuality,
                observationType,
                resultString,
                resultCount,
                resultMeasurement,
                resultTruth,
                p);
    }

    Datastream getRelatedDatastream(JsonObject j) throws ApplicationException;

    UUID getReferencedFeatureOfInterestId(String uuid) throws ApplicationException;

    default Iterable<String> createObservations(JsonArray array,
                                                String urlBase) throws ApplicationException {
        int count = array.size();
        List<String> results = new ArrayList<String>(count);
        for (int i = 0 ; i < count; i++) {
            JsonObject o = array.getJsonObject(i);
            results.addAll(createObservationFromDataArrayElement(o, urlBase));
        }
        return results;
    }

    default List<String> createObservationFromDataArrayElement(JsonObject json,
                                                               String urlBase) throws ApplicationException {
        List<String> creationResults = new ArrayList<>();

        Datastream d = this.getRelatedDatastream(json);
        if (d == null) {
            String mesg = "Unknown datastream encountered in dataArray Observation '" +
                    json.toString() + "'.";
            throw new ApplicationException(ApplicationErrorCode.E_NotFound,
                    mesg);
        }

        JsonArray components = null;
        try {
            components = json.getJsonArray(Constants.DATA_ARRAY_COMPONENTS_ATTR);
        } catch (NullPointerException npe) {
            String mesg = "No components found in dataArray Observation '" + json.toString() + "'.";
            throw new ApplicationException(ApplicationErrorCode.E_JSON,
                    mesg);
        }

        Map<Integer, String> c = new HashMap<>();
        for (int i = 0; i < components.size(); i++) {
            c.put(i, components.getString(i));
        }
        Set<String> componentSet = new HashSet<>(c.values());

        // Make sure required components are declared
        if (!componentSet.containsAll(REQUIRED_DATA_ARRAY_COMPONENTS)) {
            String mesg = "Each dataArray Observation must have at least the following components: " +
                    REQUIRED_DATA_ARRAY_COMPONENTS.toString() + ". dataArray element was: " + json.toString();
            throw new ApplicationException(ApplicationErrorCode.E_JSON,
                    mesg);
        }

        // Make sure that only supported components have been declared
        Set<String> unsupportedComponents = new HashSet<>(componentSet);
        unsupportedComponents.removeAll(SUPPORTED_DATA_ARRAY_COMPONENTS);
        if (unsupportedComponents.size() > 0) {
            String mesg = "Each dataArray Observation must only have the following components: " +
                    SUPPORTED_DATA_ARRAY_COMPONENTS.toString() + ". dataArray element was: " + json.toString();
            throw new ApplicationException(ApplicationErrorCode.E_JSON,
                    mesg);
        }

        Integer count = null;
        try {
            count = json.getInt(Constants.ANNO_DATA_ARRAY_COUNT);
        } catch (NullPointerException npe) {
            String mesg = "No " + Constants.ANNO_DATA_ARRAY_COUNT + " attribute found in dataArray Observation '" + json.toString() + "'.";
            throw new ApplicationException(ApplicationErrorCode.E_JSON,
                    mesg);
        }

        JsonArray dataArray = null;
        try {
            dataArray = json.getJsonArray(Constants.DATA_ARRAY_ATTR);
        } catch (NullPointerException npe) {
            String mesg = "No " + Constants.DATA_ARRAY_ATTR + " found in dataArray Observation '" + json.toString() + "'.";
            throw new ApplicationException(ApplicationErrorCode.E_JSON,
                    mesg);
        }

        // Make sure number of dataArray elements matches count
        if (count != dataArray.size()) {
            String mesg = Constants.ANNO_DATA_ARRAY_COUNT + " was " + count + ", but data array contains "
                    + dataArray.size() + " elements.";
            throw new ApplicationException(ApplicationErrorCode.E_JSON,
                    mesg);
        }

        // Read components
        for (int i = 0; i < dataArray.size(); i++) {
            JsonArray currObs = dataArray.getJsonArray(i);
            String creationResult = null;

            try {
                Map<String, Object> observationComponents = new HashMap<>();
                for (int j = 0; j < currObs.size(); j++) {
                    String currComponent = c.get(j);
                    // First try to read the component as a JSON object (i.e. dictionary)
                    JsonObject obj = null;
                    try {
                        obj = currObs.getJsonObject(j);
                        Map<String, String> p = new HashMap<>();
                        for (String k : obj.keySet()) {
                            String v = obj.getString(k);
                            p.put(k, v);
                        }
                        observationComponents.put(currComponent, p);
                    } catch (ClassCastException e) {
                    }

                    // Next try to read the component as a boolean
                    if (obj == null) {
                        Boolean b = null;
                        try {
                            b = currObs.getBoolean(j);
                            observationComponents.put(currComponent, b);
                        } catch (ClassCastException e) {}

                        if (b == null) {
                            JsonNumber n = null;
                            try {
                                n = currObs.getJsonNumber(j);
                            } catch (ClassCastException e) {
                            }

                            if (n != null) {
                                // Try to interpret number as long
                                try {
                                    observationComponents.put(currComponent, Long.valueOf(n.longValueExact()));
                                } catch (ArithmeticException e) {
                                    // Interpret as a long
                                    observationComponents.put(currComponent, Double.valueOf(n.doubleValue()));
                                }
                            } else {
                                // Last case read as a string
                                observationComponents.put(currComponent, currObs.getString(j));
                            }
                        }
                    }
                }

                // Make sure required components were supplied for this observation
                if (!observationComponents.keySet().containsAll(REQUIRED_DATA_ARRAY_COMPONENTS)) {
                    String mesg = "Each dataArray Observation must have at least the following components: " +
                            REQUIRED_DATA_ARRAY_COMPONENTS.toString() + ". Observation was: " + currObs.toString();
                    throw new ApplicationException(ApplicationErrorCode.E_JSON,
                            mesg);
                }

                Date phenomenonTime = null;
                Date phenomenonTimeEnd = null;
                String phenomenonTimeStr = (String) observationComponents.get("phenomenonTime");
                try {
                    phenomenonTime = Serialization.ISO8601DateTimeStringToUTCDate(phenomenonTimeStr);
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

                // Read result
                ObservationType observationType = ObservationType.valueOfUri(d.getObservationType());
                Double resultMeasurement = null;
                Long resultCount = null;
                Boolean resultTruth = null;
                String resultString = null;
                Object resultObject = observationComponents.get("result");
                if (resultObject instanceof Double) {
                    if (observationType != ObservationType.OM_Measurement) {
                        throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                                "Result was of type double, but observationType is not OM_Measurement");
                    }
                    resultMeasurement = (Double) resultObject;
                } else if (resultObject instanceof Long) {
                    if (observationType == ObservationType.OM_Measurement) {
                        resultMeasurement = ((Long) resultObject).doubleValue();
                    } else if (observationType != ObservationType.OM_CountObservation) {
                        throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                                "Result was of type integer, but observationType is not OM_CountObservation");
                    }
                    resultCount = (Long) resultObject;
                } else if (resultObject instanceof Boolean) {
                    if (observationType != ObservationType.OM_TruthObservation) {
                        throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                                "Result was type boolean, but observationType is not OM_TruthObservation");
                    }
                    resultTruth = (Boolean) resultObject;
                } else if (resultObject instanceof String) {
                    if (observationType == ObservationType.OM_CategoryObservation) {
                        // Make sure result is a valid URI
                        try {
                            URI tmpUri = URI.create((String) resultObject);
                            resultString = tmpUri.toString();
                        } catch (IllegalArgumentException e) {
                            String mesg = "Expected 'result' to be of type URI in Observation: " + json.toString();
                            throw new ApplicationException(ApplicationErrorCode.E_JSON,
                                    mesg);
                        }
                    } else if (observationType == ObservationType.OM_Observation) {
                        resultString = (String) resultObject;
                    } else {
                        throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                                "Result was type string, but observationType is not OM_CategoryObservation or OM_Observation");
                    }
                } else {
                    String mesg = "Result is of unknown type '" + resultObject.getClass().getCanonicalName() +
                            "' in Observation: " + json.toString();
                    throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                            mesg);
                }

                // Get the remainder of items of the observation
                Set<String> allComp = observationComponents.keySet();
                Set<String> optionalComp = new HashSet<>(allComp);
                optionalComp.removeAll(REQUIRED_DATA_ARRAY_COMPONENTS);

                Map<String, String> parameters = null;
                UUID featureOfInterestUUID = null;
                for (String k : optionalComp) {
                    if ("parameters".equals(k)) {
                        parameters = (Map<String, String>) observationComponents.get(k);
                    } else if (Constants.DATA_ARRAY_FEATURE_OF_INTEREST_ATTR.equals(k)) {
                        featureOfInterestUUID = getReferencedFeatureOfInterestId((String) observationComponents.get(k));
                    }
                    // TODO: Handle other attributes of Observations
                }

                // Create the Observation and write its selfLink to the JsonGenerator
                Observation o = create(d, featureOfInterestUUID,
                        phenomenonTime,
                        phenomenonTimeEnd, null,
                        null, null,
                        null,
                        observationType,
                        resultString,
                        resultCount,
                        resultMeasurement,
                        resultTruth,
                        parameters);
                creationResult = o.generateSelfLinkUrl(urlBase).toString();
            } catch (ApplicationException e) {
                creationResult = Constants.DATA_ARRAY_CREATE_ERROR_INDICATOR;
            }
            if (creationResult == null) {
                creationResult = Constants.DATA_ARRAY_CREATE_ERROR_INDICATOR;
            }
            creationResults.add(creationResult);
        }

        return creationResults;
    }
}
