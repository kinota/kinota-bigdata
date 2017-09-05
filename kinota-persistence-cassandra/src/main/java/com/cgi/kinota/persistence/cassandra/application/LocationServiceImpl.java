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

package com.cgi.kinota.persistence.cassandra.application;

import com.cgi.kinota.commons.application.LocationService;
import com.cgi.kinota.commons.application.RelatedEntityManager;
import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.persistence.DataRepository;
import com.cgi.kinota.commons.odata.ODataQuery;
import com.cgi.kinota.commons.domain.Location;

import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.LocationRepository;

import com.datastax.driver.core.utils.UUIDs;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.geojson.GeoJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.repository.MapId;
import org.springframework.stereotype.Service;

import javax.activation.MimeType;
import java.util.List;
import java.util.UUID;

import static com.cgi.kinota.commons.application.QueryableService.queryAll;
import static org.springframework.data.cassandra.repository.support.BasicMapId.id;

/**
 * Created by bmiles on 1/19/17.
 */
@Service
public class LocationServiceImpl implements LocationService {

    private static final Logger logger = LoggerFactory.getLogger(LocationServiceImpl.class);

    @Autowired
    LocationRepository repo;

    @Autowired
    RelatedEntityManager related;

    public Location findOne(UUID uuid) throws ApplicationException {
        MapId id = id().with("id", uuid);
        Location l = repo.findOne(id);
        if (l == null) {
            throw new ApplicationException(ApplicationErrorCode.E_NotFound,
                    "Location with UUID " + uuid.toString() + " not found.");
        }
        return l;
    }

    public Pair<Long, List<Location>> findAll(ODataQuery q) throws ApplicationException {
        return queryAll(q, (DataRepository) repo);
    }

    public Location overwrite(Location oldLocation, Location newLocation) throws ApplicationException {
        com.cgi.kinota.persistence.cassandra.domain.Location oldCassLocation = null;
        try {
            oldCassLocation = (com.cgi.kinota.persistence.cassandra.domain.Location) oldLocation;
        } catch (ClassCastException e) {
            String mesg = "Location is not a CassandraLocation";
            throw new ApplicationException(ApplicationErrorCode.E_InternalError,
                    mesg);
        }
        if (StringUtils.isEmpty(newLocation.getName()) || StringUtils.isEmpty(newLocation.getDescription()) ||
                newLocation.getEncodingType() == null || newLocation.getLocation() == null) {
            String mesg = "Unable to overwrite " + oldLocation + " with " + newLocation
                    + " due to missing required fields.";
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    mesg);
        }
        oldCassLocation.overwrite(newLocation);
        return this.save(oldCassLocation);
    }

    public Location save(Location l) throws ApplicationException {
        com.cgi.kinota.persistence.cassandra.domain.Location cassLocation = null;
        try {
            cassLocation = (com.cgi.kinota.persistence.cassandra.domain.Location) l;
        } catch (ClassCastException e) {
            String mesg = "Location is not a CassandraLocation";
            throw new ApplicationException(ApplicationErrorCode.E_InternalError,
                    mesg);
        }
        return repo.save(cassLocation);
    }

    public Location create(String name, String description,
                           MimeType encodingType, GeoJsonObject location) {
        final com.cgi.kinota.persistence.cassandra.domain.Location l = new com.cgi.kinota.persistence.cassandra.domain.Location(UUIDs.timeBased(),
                name, description, encodingType, location);
        repo.save(l);
        return l;
    }

    public void delete(UUID uuid) throws ApplicationException {
        related.deleteLocation(uuid);
    }
}
