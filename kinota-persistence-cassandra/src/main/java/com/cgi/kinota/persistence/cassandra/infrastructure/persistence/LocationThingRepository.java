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

package com.cgi.kinota.persistence.cassandra.infrastructure.persistence;

import com.cgi.kinota.persistence.cassandra.domain.LocationThing;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

import static com.cgi.kinota.commons.Constants.TABLE_LOCATION_THING;

/**
 * Created by bmiles on 2/1/17.
 */
public interface LocationThingRepository extends CassandraRepository<LocationThing> {
    @Query("SELECT * FROM " + TABLE_LOCATION_THING + " WHERE locationId = ?0")
    List<LocationThing> findAll(@Param("locationId") UUID locationId);

    @Query("DELETE FROM " + TABLE_LOCATION_THING + " WHERE locationId = ?0 AND thingId = ?1")
    void delete(@Param("locationId") UUID locationId,
                @Param("thingId") UUID thingId);
}
