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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cassandra.core.PrimaryKeyType;
import org.springframework.data.cassandra.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.mapping.Table;

import java.util.UUID;

import static com.cgi.kinota.commons.Constants.TABLE_THING_LOCATION;

/**
 * Internal entity used to track the Thing side of the Thing-Location many-to-many
 * relationship.  That is, this class tracks a Location associated with a Thing.
 *
 * Created by bmiles on 2/1/17.
 */
@Table(TABLE_THING_LOCATION)
public class ThingLocation {

    private static final Logger logger = LoggerFactory.getLogger(ThingLocation.class);

    @PrimaryKeyColumn(name = "thingId", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private UUID thingId;

    @PrimaryKeyColumn(name = "locationId", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private UUID locationId;

    public ThingLocation() {}

    public ThingLocation(UUID thingId, UUID locationId) {
        this.thingId = thingId;
        this.locationId = locationId;
    }

    public UUID getThingId() {
        return thingId;
    }

    public void setThingId(UUID thingId) {
        this.thingId = thingId;
    }

    public UUID getLocationId() {
        return locationId;
    }

    public void setLocationId(UUID locationId) {
        this.locationId = locationId;
    }
}
