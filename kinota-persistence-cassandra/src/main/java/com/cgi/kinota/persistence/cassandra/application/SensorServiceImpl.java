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
import com.cgi.kinota.commons.application.SensorService;
import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.domain.Sensor;
import com.cgi.kinota.commons.persistence.DataRepository;
import com.cgi.kinota.commons.odata.ODataQuery;

import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.SensorRepository;

import com.datastax.driver.core.utils.UUIDs;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.repository.MapId;
import org.springframework.stereotype.Service;

import javax.activation.MimeType;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import static com.cgi.kinota.commons.application.QueryableService.queryAll;
import static org.springframework.data.cassandra.repository.support.BasicMapId.id;

/**
 * Created by bmiles on 2/9/17.
 */
@Service
public class SensorServiceImpl implements SensorService {

    private static final Logger logger = LoggerFactory.getLogger(SensorServiceImpl.class);

    @Autowired
    SensorRepository repo;

    @Autowired
    RelatedEntityManager related;

    public Sensor findOne(UUID uuid) throws ApplicationException {
        MapId id = id().with("id", uuid);
        Sensor s = repo.findOne(id);
        if (s == null) {
            throw new ApplicationException(ApplicationErrorCode.E_NotFound,
                    "Sensor with UUID " + uuid.toString() + " not found.");
        }
        return s;
    }

    public Pair<Long, List<Sensor>> findAll(ODataQuery q) throws ApplicationException {
        return queryAll(q, (DataRepository) repo);
    }

    public Sensor overwrite(Sensor oldSensor, Sensor newSensor) throws ApplicationException {
        if (StringUtils.isEmpty(newSensor.getName()) || StringUtils.isEmpty(newSensor.getDescription()) ||
                newSensor.getEncodingType() == null || newSensor.getMetadata() == null) {
            String mesg = "Unable to overwrite " + oldSensor + " with " + newSensor
                    + " due to missing required fields.";
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    mesg);
        }
        oldSensor.overwrite(newSensor);
        return this.save(oldSensor);
    }

    public Sensor save(Sensor s) throws ApplicationException {
        com.cgi.kinota.persistence.cassandra.domain.Sensor cassSensor = null;
        try {
            cassSensor = (com.cgi.kinota.persistence.cassandra.domain.Sensor) s;
        } catch (ClassCastException e) {
            String mesg = "Sensor is not a CassandraSensor";
            throw new ApplicationException(ApplicationErrorCode.E_InternalError,
                    mesg);
        }
        return repo.save(cassSensor);
    }

    public Sensor create(String name, String description,
                         MimeType encodingType, URI metadata) {
        com.cgi.kinota.persistence.cassandra.domain.Sensor s = new com.cgi.kinota.persistence.cassandra.domain.Sensor(UUIDs.timeBased(),
                name, description, encodingType, metadata);
        return repo.save(s);
    }

    public void delete(UUID uuid) throws ApplicationException {
        related.deleteSensor(uuid);
    }
}
