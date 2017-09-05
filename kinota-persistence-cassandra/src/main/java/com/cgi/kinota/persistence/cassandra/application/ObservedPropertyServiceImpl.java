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

import com.cgi.kinota.commons.application.ObservedPropertyService;
import com.cgi.kinota.commons.application.RelatedEntityManager;
import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.domain.ObservedProperty;
import com.cgi.kinota.commons.persistence.DataRepository;
import com.cgi.kinota.commons.odata.ODataQuery;

import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.ObservedPropertyRepository;

import com.datastax.driver.core.utils.UUIDs;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.repository.MapId;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static com.cgi.kinota.commons.application.QueryableService.queryAll;
import static org.springframework.data.cassandra.repository.support.BasicMapId.id;

/**
 * Created by bmiles on 2/23/17.
 */
@Service
public class ObservedPropertyServiceImpl implements ObservedPropertyService {

    private static final Logger logger = LoggerFactory.getLogger(ObservedProperty.class);

    @Autowired
    ObservedPropertyRepository repo;

    @Autowired
    RelatedEntityManager related;

    public ObservedProperty findOne(UUID uuid) throws ApplicationException {
        MapId id = id().with("id", uuid);
        ObservedProperty p = repo.findOne(id);
        if (p == null) {
            throw new ApplicationException(ApplicationErrorCode.E_NotFound,
                    "ObservedProperty with UUID " + uuid.toString() + " not found.");
        }
        return p;
    }

    public Pair<Long, List<ObservedProperty>> findAll(ODataQuery q) throws ApplicationException {
        return queryAll(q, (DataRepository) repo);
    }

    public ObservedProperty overwrite(ObservedProperty oldOP, ObservedProperty newOP) throws ApplicationException {
        if (StringUtils.isEmpty(newOP.getName()) || newOP.getDefinition() == null ||
                StringUtils.isEmpty(newOP.getDescription())) {
            String mesg = "Unable to overwrite " + oldOP + " with " + newOP
                    + " due to missing required fields.";
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    mesg);
        }
        oldOP.overwrite(newOP);
        return this.save(oldOP);
    }

    public ObservedProperty save(ObservedProperty op) throws ApplicationException {
        com.cgi.kinota.persistence.cassandra.domain.ObservedProperty cassOP = null;
        try {
            cassOP = (com.cgi.kinota.persistence.cassandra.domain.ObservedProperty) op;
        } catch (ClassCastException e) {
            String mesg = "ObservedProperty is not a CassandraObservedProperty";
            throw new ApplicationException(ApplicationErrorCode.E_InternalError,
                    mesg);
        }
        return repo.save(cassOP);
    }

    public ObservedProperty create(String name, URI definition, String description) {
        com.cgi.kinota.persistence.cassandra.domain.ObservedProperty op = new com.cgi.kinota.persistence.cassandra.domain.ObservedProperty(UUIDs.timeBased(),
                name, definition, description);
        return repo.save(op);
    }

    public void delete(UUID uuid) throws ApplicationException {
        related.deleteObservedProperty(uuid);
    }
}
