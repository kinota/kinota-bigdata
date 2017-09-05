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

package com.cgi.kinota.persistence.cassandra.domain;

import com.cgi.kinota.commons.domain.support.ObservationType;

import org.springframework.cassandra.core.Ordering;
import org.springframework.cassandra.core.PrimaryKeyType;
import org.springframework.data.cassandra.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.mapping.Table;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static com.cgi.kinota.commons.Constants.*;

/**
 * Created by bmiles on 2/27/17.
 */
@Table(TABLE_OBSERVATION)
public class Observation extends com.cgi.kinota.commons.domain.Observation {

    public Observation() {}

    public Observation(UUID id, UUID featureOfInterestId, UUID datastreamId, Date phenomenonTime,
                       Date phenomenonTimeEnd, Date resultTime, Date validTimeBegin, Date validTimeEnd,
                       String resultQuality, ObservationType observationType, String resultString,
                       Long resultCount, Double resultMeasurement, Boolean resultTruth,
                       Map<String, String> parameters) {
        super(id, featureOfInterestId, datastreamId, phenomenonTime, phenomenonTimeEnd, resultTime, validTimeBegin,
                validTimeEnd, resultQuality, observationType, resultString, resultCount, resultMeasurement,
                resultTruth, parameters);
    }

    public Observation(UUID id, UUID featureOfInterestId, UUID datastreamId, Date phenomenonTime,
                       Date phenomenonTimeEnd, Date resultTime, Date validTimeBegin, Date validTimeEnd,
                       String resultQuality, ObservationType observationType, String resultString,
                       Long resultCount, Double resultMeasurement, Boolean resultTruth, String parametersJson) {
        super(id, featureOfInterestId, datastreamId, phenomenonTime, phenomenonTimeEnd, resultTime, validTimeBegin,
                validTimeEnd, resultQuality, observationType, resultString, resultCount, resultMeasurement,
                resultTruth, parametersJson);
    }

    @Override
    @PrimaryKeyColumn(name = "id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    public UUID getId() { return id; }

    @PrimaryKeyColumn(name = "featureOfInterestId", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
    public UUID getFeatureOfInterestId() { return featureOfInterestId; }

    @PrimaryKeyColumn(name = "datastreamId", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    public UUID getDatastreamId() { return datastreamId; }

    @PrimaryKeyColumn(name = "phenomenonTime", ordinal = 3, type = PrimaryKeyType.CLUSTERED,
            ordering = Ordering.DESCENDING)
    public Date getPhenomenonTime() { return phenomenonTime; }
}
