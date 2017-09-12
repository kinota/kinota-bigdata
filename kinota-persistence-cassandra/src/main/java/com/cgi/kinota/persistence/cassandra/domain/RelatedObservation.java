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

import com.cgi.kinota.commons.Utility;
import com.cgi.kinota.commons.domain.Observation;
import com.cgi.kinota.commons.domain.support.ObservationType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cassandra.core.Ordering;
import org.springframework.cassandra.core.PrimaryKeyType;
import org.springframework.data.cassandra.mapping.Column;
import org.springframework.data.cassandra.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.mapping.Table;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static com.cgi.kinota.commons.Constants.*;

/**
 * Created by bmiles on 3/21/17.
 */
@Table(TABLE_RELATED_OBSERVATION)
public class RelatedObservation extends Observation {

    private static final Logger logger = LoggerFactory.getLogger(RelatedObservation.class);

    private Integer year;

    public RelatedObservation() {}

    public RelatedObservation(UUID datastreamId, Date phenomenonTime,
                              Date phenomenonTimeEnd,
                              UUID observationId, UUID featureOfInterestId,
                              Date resultTime, Date validTimeBegin, Date validTimeEnd,
                              String resultQuality,
                              ObservationType observationType,
                              String resultString,
                              Long resultCount,
                              Double resultMeasurement,
                              Boolean resultTruth,
                              Map<String, String> parameters) {
        super(observationId, featureOfInterestId, datastreamId, phenomenonTime, phenomenonTimeEnd, resultTime, validTimeBegin,
                validTimeEnd, resultQuality, observationType, resultString, resultCount, resultMeasurement,
                resultTruth, parameters);
        this.year = Utility.getYearForDate(phenomenonTime);
    }

    @Override
    @Column("observationid")
    public UUID getId() { return id; }

    @Override
    public void setId(UUID id) { this.id = id; }
    
    @PrimaryKeyColumn(name = "featureOfInterestId", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    public UUID getFeatureOfInterestId() { return featureOfInterestId; }

    @PrimaryKeyColumn(name = "datastreamId", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
    public UUID getDatastreamId() { return datastreamId; }

    @PrimaryKeyColumn(name = "year", ordinal = 2, type = PrimaryKeyType.PARTITIONED)
    public Integer getYear() { return year; }

    public void setYear(Integer year) { this.year = year; }

    @PrimaryKeyColumn(name = "phenomenonTime", ordinal = 3, type = PrimaryKeyType.CLUSTERED,
            ordering = Ordering.DESCENDING)
    public Date getPhenomenonTime() { return phenomenonTime; }
}
