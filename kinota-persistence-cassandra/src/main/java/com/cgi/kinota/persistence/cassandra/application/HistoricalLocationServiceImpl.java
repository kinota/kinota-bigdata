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

import com.cgi.kinota.commons.application.HistoricalLocationService;
import com.cgi.kinota.commons.application.RelatedEntityManager;
import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.domain.Thing;
import com.cgi.kinota.commons.persistence.DataRepository;
import com.cgi.kinota.commons.odata.ODataQuery;
import com.cgi.kinota.commons.domain.HistoricalLocation;
import com.cgi.kinota.commons.domain.Location;

import com.cgi.kinota.persistence.cassandra.application.support.HistoricalLocationServiceHelper;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.HistoricalLocationRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.LocationRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.ThingRepository;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.repository.MapId;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.cgi.kinota.commons.application.QueryableService.queryAll;
import static com.cgi.kinota.persistence.cassandra.application.util.JsonParseUtils.assertRelatedEntityExists;

import static org.springframework.data.cassandra.repository.support.BasicMapId.id;

/**
 * Created by bmiles on 2/3/17.
 */
@Service
public class HistoricalLocationServiceImpl implements HistoricalLocationService {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalLocationServiceImpl.class);

    @Autowired
    HistoricalLocationRepository repo;

    @Autowired
    ThingRepository thingRepo;

    @Autowired
    LocationRepository locationRepo;

    @Autowired
    RelatedEntityManager related;

    public HistoricalLocation findOne(UUID uuid) throws ApplicationException {
        MapId id = id().with("id", uuid);
        HistoricalLocation l = repo.findOne(id);
        if (l == null) {
            throw new ApplicationException(ApplicationErrorCode.E_NotFound,
                    "HistoricalLocation with UUID " + uuid.toString() + " not found.");
        }
        return l;
    }

    public Pair<Long, List<HistoricalLocation>> findAll(ODataQuery q) throws ApplicationException {
        return queryAll(q, (DataRepository) repo);
    }

    public HistoricalLocation overwrite(HistoricalLocation oldLocation, HistoricalLocation newLocation) throws ApplicationException {
        if (newLocation.getTime() == null) {
            String mesg = "Unable to overwrite " + oldLocation + " with " + newLocation
                    + " due to missing required fields.";
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    mesg);
        }
        oldLocation.overwrite(newLocation);
        return this.save(oldLocation);
    }

    public HistoricalLocation update(HistoricalLocation l,
                                     UUID thingUUID, UUID locationUUID) throws ApplicationException {
        if (thingUUID != null) {
            assertRelatedEntityExists(thingUUID,
                    Thing.NAME, thingRepo);
            // Disassociate with previous Thing
            UUID oldThingId = related.fetchThingUuidForHistoricalLocation(l.getId());
            if (oldThingId != null) {
                related.disassociateHistoricalLocationWithThing(oldThingId, l.getId());
            }
            // Associate with new Thing
        }   related.associateHistoricalLocationWithThing(thingUUID, l.getId());

        if (locationUUID != null) {
            assertRelatedEntityExists(locationUUID,
                    Location.NAME, locationRepo);
            // Associate with new Location
            related.associateHistoricalLocationWithLocation(locationUUID, l.getId());
        }

        return this.save(l);
    }

    public HistoricalLocation save(HistoricalLocation l) throws ApplicationException {
        com.cgi.kinota.persistence.cassandra.domain.HistoricalLocation cassLocation = null;
        try {
            cassLocation = (com.cgi.kinota.persistence.cassandra.domain.HistoricalLocation) l;
        } catch (ClassCastException e) {
            String mesg = "HistoricalLocation is not a CassandraHistoricalLocation";
            throw new ApplicationException(ApplicationErrorCode.E_InternalError,
                    mesg);
        }
        return repo.save(cassLocation);
    }

    public HistoricalLocation create(UUID thingUUID,
                                     UUID locationUUID,
                                     Date time) throws ApplicationException {
        if (thingUUID == null || locationUUID == null) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "HistoricalLocation did not specify a Thing and a Location.");
        }

        assertRelatedEntityExists(locationUUID,
                Thing.NAME, thingRepo);
        assertRelatedEntityExists(locationUUID,
                Location.NAME, locationRepo);
        return HistoricalLocationServiceHelper.createHistoricalLocation(time,
                thingUUID, locationUUID, repo, related);
    }

    public void delete(UUID uuid) throws ApplicationException {
        related.deleteHistoricalLocation(uuid);
    }
}
