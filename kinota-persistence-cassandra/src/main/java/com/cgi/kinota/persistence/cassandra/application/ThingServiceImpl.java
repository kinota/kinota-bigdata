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

import com.cgi.kinota.commons.application.RelatedEntityManager;
import com.cgi.kinota.commons.application.ThingService;
import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.domain.Thing;
import com.cgi.kinota.commons.domain.Location;
import com.cgi.kinota.commons.persistence.DataRepository;
import com.cgi.kinota.commons.odata.ODataQuery;

import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.LocationRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.ThingRepository;

import com.datastax.driver.core.utils.UUIDs;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.repository.MapId;
import org.springframework.stereotype.Service;

import static com.cgi.kinota.commons.application.QueryableService.queryAll;
import static com.cgi.kinota.persistence.cassandra.application.util.JsonParseUtils.assertRelatedEntityExists;
import static org.springframework.data.cassandra.repository.support.BasicMapId.id;

import java.util.*;

/**
 * Created by bmiles on 12/28/16.
 */
@Service
public class ThingServiceImpl implements ThingService {

    private static final Logger logger = LoggerFactory.getLogger(ThingServiceImpl.class);

    @Autowired
    ThingRepository repo;

    @Autowired
    LocationRepository locationRepo;

    @Autowired
    RelatedEntityManager related;

    public Thing findOne(UUID uuid) throws ApplicationException {
        MapId id = id().with("id", uuid);
        Thing t = repo.findOne(id);
        if (t == null) {
            throw new ApplicationException(ApplicationErrorCode.E_NotFound,
                    "Thing with UUID " + uuid.toString() + " not found.");
        }
        return t;
    }

    public Pair<Long, List<Thing>> findAll(ODataQuery q) throws ApplicationException {
        return queryAll(q, (DataRepository) repo);
    }

    public Thing overwrite(Thing oldThing, Thing newThing) throws ApplicationException {
        com.cgi.kinota.persistence.cassandra.domain.Thing oldCassThing = null;
        try {
            oldCassThing = (com.cgi.kinota.persistence.cassandra.domain.Thing) oldThing;
        } catch (ClassCastException e) {
            String mesg = "Thing is not a CassandraThing";
            throw new ApplicationException(ApplicationErrorCode.E_InternalError,
                    mesg);
        }
        // TODO: Update to take JSON as input and allow the Thing Location to be overwritten/unset
        if (StringUtils.isEmpty(newThing.getName()) || StringUtils.isEmpty(newThing.getDescription())) {
            String mesg = "Unable to overwrite " + oldThing + " with " + newThing
                    + " due to missing required fields.";
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    mesg);
        }
        oldCassThing.overwrite(newThing);
        return this.save(oldCassThing);
    }

    public Thing update(Thing t, UUID locationUUID) throws ApplicationException {
        // Related entities
        if (locationUUID != null) {
            // Make sure location exists
            Location location = locationRepo.findOne(id().with("id", locationUUID));
            if (location == null) {
                throw new ApplicationException(ApplicationErrorCode.E_NotFound,
                        "Unable to find Location with ID '" + locationUUID + "' when updating Thing named '" + t.getName() + "'.");
            }
            // Check to see if a Location with the same encoding type is associated
            //   with this Thing.  If so, un-associate that Location with this Thing
            //   (which must trigger the creation of an historical location), and then
            //   associate the new Location with the Thing.  If there are no Locations
            //   with this Location's encoding type, simply associate this Location
            //   with the Thing.
            Location locToReplace = null;
            for (UUID locU : related.fetchLocationUuidsForThing(t.getId())) {
                Location relatedLoc = locationRepo.findOne(id().with("id", locU));
                if (location.getEncodingType().match(relatedLoc.getEncodingType())) {
                    locToReplace = relatedLoc;
                    // There should only be one Location of each encoding type
                    //   associated with a Thing, so we can bail here.
                    break;
                }
            }
            if (locToReplace != null) {
                // Un-associate that Location with this Thing.
                related.disassociateLocationWithThing(t.getId(), locToReplace.getId());
            }
            // Associate this Location with the Thing.
            related.associateLocationWithThing(t.getId(), location.getId());
        }

        return this.save(t);
    }

    public Thing save(Thing t) throws ApplicationException {
        com.cgi.kinota.persistence.cassandra.domain.Thing cassThing = null;
        try {
            cassThing = (com.cgi.kinota.persistence.cassandra.domain.Thing) t;
        } catch (ClassCastException e) {
            String mesg = "Thing is not a CassandraThing";
            throw new ApplicationException(ApplicationErrorCode.E_InternalError,
                    mesg);
        }
        return repo.save(cassThing);
    }

    public Thing create(UUID locationUUID,
                        String name, String description,
                        Map<String, String> p) throws ApplicationException {
        final com.cgi.kinota.persistence.cassandra.domain.Thing t = new com.cgi.kinota.persistence.cassandra.domain.Thing(UUIDs.timeBased(), name, description, p);
        repo.save(t);
        if (locationUUID != null) {
            assertRelatedEntityExists(locationUUID,
                    Location.NAME, locationRepo);
            related.associateLocationWithThing(t.getId(), locationUUID);
        }
        return t;
    }

    public void delete(UUID uuid) throws ApplicationException {
        related.deleteThing(uuid);
    }
}
