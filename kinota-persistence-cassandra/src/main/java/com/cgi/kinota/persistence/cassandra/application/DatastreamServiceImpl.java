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

import com.cgi.kinota.commons.application.DatastreamService;
import com.cgi.kinota.commons.application.RelatedEntityManager;
import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.domain.ObservedProperty;
import com.cgi.kinota.commons.domain.Sensor;
import com.cgi.kinota.commons.domain.Thing;
import com.cgi.kinota.commons.domain.Datastream;
import com.cgi.kinota.commons.domain.support.ObservationType;
import com.cgi.kinota.commons.domain.support.UnitOfMeasurement;
import com.cgi.kinota.commons.persistence.DataRepository;
import com.cgi.kinota.commons.odata.ODataQuery;

import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.DatastreamRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.ObservedPropertyRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.SensorRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.ThingRepository;

import com.datastax.driver.core.utils.UUIDs;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.geojson.GeoJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.repository.MapId;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static com.cgi.kinota.commons.application.QueryableService.queryAll;
import static com.cgi.kinota.persistence.cassandra.application.util.JsonParseUtils.assertRelatedEntityExists;

import static org.springframework.data.cassandra.repository.support.BasicMapId.id;

/**
 * Created by bmiles on 2/28/17.
 */
@Service
public class DatastreamServiceImpl implements DatastreamService {

    private static final Logger logger = LoggerFactory.getLogger(DatastreamServiceImpl.class);

    @Autowired
    DatastreamRepository repo;

    @Autowired
    ThingRepository thingRepo;

    @Autowired
    SensorRepository sensorRepo;

    @Autowired
    ObservedPropertyRepository observedPropertyRepo;

    @Autowired
    RelatedEntityManager related;

    public Datastream findOne(UUID uuid) throws ApplicationException {
        MapId id = id().with("id", uuid);
        Datastream d = repo.findOne(id);
        if (d == null) {
            throw new ApplicationException(ApplicationErrorCode.E_NotFound,
                    "Datastream with UUID " + uuid.toString() + " not found.");
        }
        return d;
    }

    public Pair<Long, List<Datastream>> findAll(ODataQuery q) throws ApplicationException {
        return queryAll(q, (DataRepository) repo);
    }

    public Datastream overwrite(Datastream oldDs, Datastream newDs) throws ApplicationException {
        // TODO: Update to take JSON as input and allow the Datastream Thing, Sensor, or ObservedProperty to be updated
        if (StringUtils.isEmpty(newDs.getName()) || StringUtils.isEmpty(newDs.getDescription()) ||
                newDs.getUnitOfMeasurement() == null || newDs.getObservationType() == null) {
            String mesg = "Unable to overwrite " + oldDs + " with " + newDs
                    + " due to missing required fields.";
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    mesg);
        }
        oldDs.overwrite(newDs);
        return this.save(oldDs);
    }

    public Datastream update(Datastream d, UUID thingUUID, UUID sensorUUID, UUID observedPropertyUUID) {
        if (thingUUID != null) {
            assertRelatedEntityExists(thingUUID,
                    Thing.NAME, thingRepo);
            if (d.getThingId() != null && !thingUUID.equals(d.getThingId())) {
                // Disassociate with previous Thing
                related.disassociateThingWithDatastream(d.getId(), d.getThingId());
            }
            // Associate with new Thing
            d.setThingId(thingUUID);
            related.associateThingWithDatastream(d.getId(), thingUUID);
        }

        if (sensorUUID != null) {
            assertRelatedEntityExists(sensorUUID,
                    Sensor.NAME, sensorRepo);
            if (d.getSensorId() != null && !sensorUUID.equals(d.getSensorId())) {
                // Disassociate with previous Sensor
                related.disassociateSensorWithDatastream(d.getId(), d.getSensorId());
            }
            // Associate with new Sensor
            d.setSensorId(sensorUUID);
            related.associateSensorWithDatastream(d.getId(), sensorUUID);
        }

        if (observedPropertyUUID != null) {
            assertRelatedEntityExists(observedPropertyUUID,
                    ObservedProperty.NAME, observedPropertyRepo);
            if (d.getObservedPropertyId() != null && !observedPropertyUUID.equals(d.getObservedPropertyId())) {
                // Disassociate wth previous ObservedProperty
                related.disassociateObservedPropertyWithDatastream(d.getId(), d.getObservedPropertyId());
            }
            // Associate with new ObservedProperty
            d.setObservedPropertyId(observedPropertyUUID);
            related.associateObservedPropertyWithDatastream(d.getId(), observedPropertyUUID);
        }

        return this.save(d);
    }

    public Datastream save(Datastream d) throws ApplicationException {
        com.cgi.kinota.persistence.cassandra.domain.Datastream cassDatastream = null;
        try {
            cassDatastream = (com.cgi.kinota.persistence.cassandra.domain.Datastream) d;
        } catch (ClassCastException e) {
            String mesg = "Datastream is not a CassandraDatastream";
            throw new ApplicationException(ApplicationErrorCode.E_InternalError,
                    mesg);
        }
        return repo.save(cassDatastream);
    }

    public Datastream create(UUID thingId, UUID sensorId, UUID observedPropertyId,
                             String name, String description,
                             UnitOfMeasurement unitOfMeasurement,
                             URI observationType, GeoJsonObject observationArea) throws ApplicationException {
        // Validate ObservationType
        ObservationType ot = ObservationType.valueOfUri(observationType);

        assertRelatedEntityExists(thingId, Thing.NAME, thingRepo);
        assertRelatedEntityExists(sensorId, Sensor.NAME, sensorRepo);
        assertRelatedEntityExists(observedPropertyId, ObservedProperty.NAME, observedPropertyRepo);

        com.cgi.kinota.persistence.cassandra.domain.Datastream d = new com.cgi.kinota.persistence.cassandra.domain.Datastream(UUIDs.timeBased(),
                thingId, sensorId, observedPropertyId,
                name, description, unitOfMeasurement,
                observationType, observationArea,
                null, null, null, null);
        d = repo.save(d);
        related.associateThingWithDatastream(d.getId(), thingId);
        related.associateSensorWithDatastream(d.getId(), sensorId);
        related.associateObservedPropertyWithDatastream(d.getId(), observedPropertyId);
        return d;
    }

    public void delete(UUID uuid) throws ApplicationException {
        related.deleteDatastream(uuid);
    }
}
