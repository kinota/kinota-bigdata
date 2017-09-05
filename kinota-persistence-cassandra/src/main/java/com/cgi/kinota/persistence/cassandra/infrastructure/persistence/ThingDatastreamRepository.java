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

import com.cgi.kinota.persistence.cassandra.domain.ThingDatastream;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

import static com.cgi.kinota.commons.Constants.TABLE_THING_DATASTREAM;

/**
 * Created by bmiles on 2/28/17.
 */
public interface ThingDatastreamRepository extends CassandraRepository<ThingDatastream> {
    @Query("SELECT * FROM " + TABLE_THING_DATASTREAM + " WHERE thingId = ?0")
    List<ThingDatastream> findAll(@Param("thingId") UUID thingId);

    @Query("DELETE FROM " + TABLE_THING_DATASTREAM + " WHERE thingId = ?0 AND datastreamId = ?1")
    void delete(@Param("thingId") UUID thingId,
                @Param("datastreamId") UUID datastreamId);

    @Query("DELETE FROM " + TABLE_THING_DATASTREAM + " WHERE thingId = ?0")
    void delete(@Param("thingId") UUID thingId);
}
