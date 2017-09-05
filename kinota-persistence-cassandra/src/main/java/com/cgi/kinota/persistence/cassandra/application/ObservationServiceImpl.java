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

import com.cgi.kinota.commons.application.FeatureOfInterestService;
import com.cgi.kinota.commons.application.ObservationService;
import com.cgi.kinota.commons.application.RelatedEntityManager;
import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.domain.Datastream;
import com.cgi.kinota.commons.domain.FeatureOfInterest;
import com.cgi.kinota.commons.domain.Location;
import com.cgi.kinota.commons.domain.Observation;
import com.cgi.kinota.commons.domain.support.ObservationType;
import com.cgi.kinota.commons.persistence.DataRepository;
import com.cgi.kinota.commons.odata.ODataQuery;

import com.cgi.kinota.persistence.cassandra.application.util.JsonParseUtils;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.DatastreamRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.FeatureOfInterestLocationNativeRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.FeatureOfInterestRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.LocationRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.ObservationRepository;

import com.datastax.driver.core.utils.UUIDs;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.repository.MapId;
import org.springframework.stereotype.Service;

import javax.json.JsonObject;
import java.util.*;

import static com.cgi.kinota.commons.Constants.*;
import static org.springframework.data.cassandra.repository.support.BasicMapId.id;
import static com.cgi.kinota.commons.application.QueryableService.queryAll;

/**
 * Created by bmiles on 3/20/17.
 */
@Service
public class ObservationServiceImpl implements ObservationService {

    private static final Logger logger = LoggerFactory.getLogger(ObservationServiceImpl.class);

    @Autowired
    ObservationRepository repo;

    @Autowired
    FeatureOfInterestRepository featureOfInterestRepo;

    @Autowired
    DatastreamRepository datastreamRepo;

    @Autowired
    LocationRepository locationRepo;

    @Autowired
    FeatureOfInterestService featureOfInterestService;

    @Autowired
    FeatureOfInterestLocationNativeRepository featureOfInterestLocationRepo;

    @Autowired
    RelatedEntityManager related;

    public Observation findOne(UUID uuid) throws ApplicationException {
        MapId id = id().with("id", uuid);
        Observation o = repo.findOne(id);
        if (o == null) {
            throw new ApplicationException(ApplicationErrorCode.E_NotFound,
                    "Observation with UUID " + uuid.toString() + " not found.");
        }
        return o;
    }

    public Pair<Long, List<Observation>> findAll(ODataQuery q) throws ApplicationException {
        return queryAll(q, (DataRepository) repo);
    }

    public Observation overwrite(Observation oldObservation, Observation newObservation) throws ApplicationException {
        // TODO: Update to take JSON as input and allow the Observation FeatureOfInterest to be overwritten
        if (newObservation.getPhenomenonTime() == null || newObservation.getResultTime() == null ||
                (newObservation.getResultString() == null && newObservation.getResultCount() == null &&
                        newObservation.getResultMeasurement() == null && newObservation.getResultTruth() == null)) {
            String mesg = "Unable to overwrite " + oldObservation + " with " + newObservation
                    + " due to missing required fields.";
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    mesg);
        }
        oldObservation.overwrite(newObservation);
        return this.save(oldObservation);
    }

    public Observation save(Observation o) throws ApplicationException {
        com.cgi.kinota.persistence.cassandra.domain.Observation cassObservation = null;
        try {
            cassObservation = (com.cgi.kinota.persistence.cassandra.domain.Observation) o;
        } catch (ClassCastException e) {
            String mesg = "Observation is not a CassandraObservation";
            throw new ApplicationException(ApplicationErrorCode.E_InternalError,
                    mesg);
        }
        return repo.save(cassObservation);
    }

    protected Observation doCreate(Datastream d,
                                   com.cgi.kinota.persistence.cassandra.domain.Observation o) throws ApplicationException {

        if (o.getFeatureOfInterestId() == null) {
            o.setFeatureOfInterestId(findFeatureOfInterestForThingLocation(d));
        }

        if (o.getFeatureOfInterestId() == null) {
            throw new ApplicationException(ApplicationErrorCode.E_InternalError,
                    "No FeatureOfInterest specified for Observation, and no FeatureOfInterest could be found " +
                    "or created based on the Location of Thing with ID " + d.getThingId() + ".");
        }

        repo.save(o);

        // Related entities
        related.associateRelatedObservation(o);

        return o;
    }

    /**
     * Find the UUID of a FeatureOfInterest matching a Thing's Location.  If
     * such a FeatureOfInterest cannot be found, create it.
     *
     * Invariant: featureOfInterestUUID is not null
     *
     * From the SensorThings spec:
     * If the service detects that there is no link to a FeatureOfInterest
     * entity in the POST body message that creates an Observation entity, the
     * service SHALL either (1) create a FeatureOfInterest entity by using the
     * location property from the Location of the Thing entity when there is no
     * FeatureOfInterest whose location property is from the Location of the
     * Thing entity or (2) link to the FeatureOfInterest whose location property
     * is from the Location of the Thing entity.
     * @param d Datastream whose Thing will be used to infer the Location of the FeatureOfInterest.
     * @return UUID of the FeatureOfInterest
     * @throws ApplicationException If FeatureOfInterest cannot be found or created.
     */
    protected UUID findFeatureOfInterestForThingLocation(Datastream d) throws ApplicationException {
        UUID featureOfInterestUUID = null;

        // Find Thing's location that has content type GeoJSON and then
        // Look for feature of interest that has the same location as our Thing
        Location l = null;
        for (UUID locUuid : related.fetchLocationUuidsForThing(d.getThingId())) {
            l = locationRepo.findOne(id().with("id", locUuid));
            if (l != null && l.getEncodingType().toString().equals(CONTENT_TYPE_GEO_JSON)) {
                featureOfInterestUUID = featureOfInterestLocationRepo.findFeatureOfInterestWithLocation(l);
                break;
            }
            l = null;
        }

        if (featureOfInterestUUID == null) {
            // Did not find an existing feature of interest that matches Thing's location
            //   try to create one
            if (l == null) {
                throw new ApplicationException(ApplicationErrorCode.E_InternalError,
                        "Thing with UUID " + d.getThingId().toString() + " has no location, " +
                                " and there was no FeatureOfInterest specified for new Observation in" +
                                " Datastream with UUID " + d.getId().toString());
            }
            FeatureOfInterest foi = featureOfInterestService.create(l.getName(),
                    l.getDescription(), l.getEncodingType(), l.getLocation());
            featureOfInterestUUID = foi.getId();
        }

        return featureOfInterestUUID;
    }

    public Observation create(Datastream d, UUID featureOfInterestId,
                              Date phenomenonTime,
                              Date phenomenonTimeEnd, Date resultTime,
                              Date validTimeBegin, Date validTimeEnd,
                              String resultQuality,
                              ObservationType observationType,
                              String resultString,
                              Long resultCount,
                              Double resultMeasurement,
                              Boolean resultTruth,
                              Map<String, String> parameters) throws ApplicationException {
        com.cgi.kinota.persistence.cassandra.domain.Observation o = new com.cgi.kinota.persistence.cassandra.domain.Observation(UUIDs.timeBased(),
                featureOfInterestId, d.getId(), phenomenonTime,
                phenomenonTimeEnd, resultTime,
                validTimeBegin, validTimeEnd, resultQuality,
                observationType,
                resultString,
                resultCount,
                resultMeasurement,
                resultTruth,
                parameters);
        return doCreate(d, o);
    }

    public void delete(UUID uuid) throws ApplicationException {
        related.deleteObservation(uuid);
    }

    public Datastream getRelatedDatastream(JsonObject j) throws ApplicationException {
        return JsonParseUtils.getRelatedEntity(j, Datastream.NAME, datastreamRepo);
    }

    public UUID getReferencedFeatureOfInterestId(String uuid) throws ApplicationException {
        return JsonParseUtils.getReferencedEntityId(uuid, FeatureOfInterest.NAME, featureOfInterestRepo);
    }
}
