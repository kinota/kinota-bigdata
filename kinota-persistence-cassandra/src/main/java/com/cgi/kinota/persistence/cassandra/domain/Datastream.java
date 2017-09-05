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

import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.domain.support.UnitOfMeasurement;

import org.geojson.GeoJsonObject;
import org.springframework.cassandra.core.PrimaryKeyType;
import org.springframework.data.cassandra.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.mapping.Table;

import java.net.URI;
import java.util.*;

import static com.cgi.kinota.commons.Constants.*;

/**
 * Created by bmiles on 1/13/17.
 */
@Table(TABLE_DATASTREAM)
public class Datastream extends com.cgi.kinota.commons.domain.Datastream {

    public Datastream() {}

    public Datastream(UUID id, UUID thingId, UUID sensorId, UUID observedPropertyId,
                      String name, String description, UnitOfMeasurement unitOfMeasurement, URI observationType,
                      GeoJsonObject observedArea, Date phenomenonTimeBegin, Date phenomenonTimeEnd,
                      Date resultTimeBegin, Date resultTimeEnd) throws ApplicationException {
        super(id, thingId, sensorId, observedPropertyId, name, description, unitOfMeasurement, observationType, observedArea, phenomenonTimeBegin, phenomenonTimeEnd, resultTimeBegin, resultTimeEnd);
    }

    @Override
    @PrimaryKeyColumn(name = "id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    public UUID getId() {
        return this.id;
    }
}
