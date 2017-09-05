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

package com.cgi.kinota.commons;

/**
 * Created by bmiles on 1/4/17.
 */
public class Constants {
    public static final String CONFIG_ID = "cgi-sensorthings";
    public static final String API_VERSION = "v1.0";
    public static final String API_VERSION_PATH = "/" + API_VERSION;

    public static final String ANNO_IOT_ID = "@iot.id";
    public static final String ANNO_IOT_SELF_LINK = "@iot.selfLink";
    // WARNING: ANNO_IOT_SELF_LINKS may be a typo in the standard.  See sect. 9.2.7.
    //   (http://docs.opengeospatial.org/is/15-078r6/15-078r6.html#42)
    public static final String ANNO_IOT_SELF_LINKS = "@iot.selfLinks";
    public static final String ANNO_IOT_NAV_LINK = "@iot.navigationLink";
    public static final String ASSOC_LINK_ADDY = "/$ref";

    public static final String ANNO_COLLECTION_COUNT = "@iot.count";
    public static final String COLLECTION_ATTR = "value";
    // Set page size to allow two weeks of data, assuming one data point
    //   per second, to be fetched in single request.
    public static final Integer MAX_REQUEST_PAGE_SIZE = Integer.valueOf(System.getenv().getOrDefault("MAX_REQUEST_PAGE_SIZE", "20160"));

    public static final String ANNO_IOT_NEXT_LINK = "@iot.nextLink";

    public static final String RESULT_FORMAT_PARAM = "$resultFormat";
    public static final String RESULT_FORMAT_DATA_ARRAY = "dataArray";
    public static final String DATA_ARRAY_COMPONENTS_ATTR = "components";
    public static final String ANNO_DATA_ARRAY_COUNT = "dataArray@iot.count";
    public static final String DATA_ARRAY_ATTR = "dataArray";
    public static final String DATA_ARRAY_FEATURE_OF_INTEREST_ATTR = "FeatureOfInterest/id";
    public static final String DATA_ARRAY_CREATE_ERROR_INDICATOR = "error";

    public static final String FEATURE_OF_INTEREST_LOCATION_JSON_ATTR = "feature";

    public static final String TABLE_THING = "thing";
    public static final String TABLE_THING_LOCATION = "thinglocation";
    public static final String TABLE_THING_HISTORICAL_LOCATION = "thinghistoricallocation";
    public static final String TABLE_THING_DATASTREAM = "thingdatastream";

    public static final String TABLE_LOCATION = "location";
    public static final String TABLE_LOCATION_THING = "locationthing";
    public static final String TABLE_LOCATION_HISTORICAL_LOCATION = "locationhistoricallocation";

    public static final String TABLE_HISTORICAL_LOCATION = "historicallocation";
    public static final String TABLE_HISTORICAL_LOCATION_THING = "historicallocationthing";
    public static final String TABLE_HISTORICAL_LOCATION_LOCATION = "historicallocationlocation";

    public static final String TABLE_SENSOR = "sensor";
    public static final String TABLE_SENSOR_DATASTREAM = "sensordatastream";

    public static final String TABLE_OBSERVED_PROPERTY = "observedproperty";
    public static final String TABLE_OBSERVED_PROPERTY_DATASTREAM = "observedpropertydatastream";

    public static final String TABLE_FEATURE_OF_INTEREST = "featureofinterest";
    public static final String TABLE_FEATURE_OF_INTEREST_OBSERVATION_DS_YEAR = "featureofinterestobservationdsyear";
    public static final String MATERIALIZED_VIEW_FEATURE_OF_INTEREST_LOCATION = "mv_featureofinterestlocation";

    public static final String TABLE_DATASTREAM = "datastream";
    public static final String TABLE_DATASTREAM_OBSERVATION_FOI_YEAR = "datastreamobservationfoiyear";

    public static final String TABLE_OBSERVATION = "observation";
    public static final String TABLE_RELATED_OBSERVATION = "relatedobservation";

    public static final String CONTENT_TYPE_GEO_JSON = "application/vnd.geo+json";

    public static final String OBS_TYPE_OM_CATEGORY_OBSERVATION = "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_CategoryObservation";
    public static final String OBS_TYPE_OM_COUNT_OBSERVATION = "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_CountObservation";
    public static final String OBS_TYPE_OM_MEASUREMENT = "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement";
    public static final String OBS_TYPE_OM_OBSERVATION = "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Observation";
    public static final String OBS_TYPE_OM_TRUTH_OBSERVATION = "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_TruthObservation";
}
