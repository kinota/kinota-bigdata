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
import com.cgi.kinota.commons.application.RelatedEntityManager;
import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.domain.FeatureOfInterest;
import com.cgi.kinota.commons.persistence.DataRepository;
import com.cgi.kinota.commons.odata.ODataQuery;

import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.FeatureOfInterestRepository;

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
 * Created by bmiles on 2/27/17.
 */
@Service
public class FeatureOfInterestServiceImpl implements FeatureOfInterestService {

    private static final Logger logger = LoggerFactory.getLogger(FeatureOfInterestServiceImpl.class);

    @Autowired
    FeatureOfInterestRepository repo;

    @Autowired
    RelatedEntityManager related;

    public FeatureOfInterest findOne(UUID uuid) throws ApplicationException {
        MapId id = id().with("id", uuid);
        FeatureOfInterest l = repo.findOne(id);
        if (l == null) {
            throw new ApplicationException(ApplicationErrorCode.E_NotFound,
                    "FeatureOfInterest with UUID " + uuid.toString() + " not found.");
        }
        return l;
    }

    public Pair<Long, List<FeatureOfInterest>> findAll(ODataQuery q) throws ApplicationException {
        return queryAll(q, (DataRepository) repo);
    }

    public FeatureOfInterest overwrite(FeatureOfInterest oldFoi, FeatureOfInterest newFoi) throws ApplicationException {
        if (StringUtils.isEmpty(newFoi.getName()) || StringUtils.isEmpty(newFoi.getDescription()) ||
                newFoi.getEncodingType() == null || newFoi.getLocation() == null) {
            String mesg = "Unable to overwrite " + oldFoi + " with " + newFoi
                    + " due to missing required fields.";
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    mesg);
        }
        oldFoi.overwrite(newFoi);
        return this.save(oldFoi);
    }

    public FeatureOfInterest save(FeatureOfInterest foi) throws ApplicationException {
        com.cgi.kinota.persistence.cassandra.domain.FeatureOfInterest cassFoi = null;
        try {
            cassFoi = (com.cgi.kinota.persistence.cassandra.domain.FeatureOfInterest) foi;
        } catch (ClassCastException e) {
            String mesg = "FeatureOfInterest is not a CassandraFeatureOfInterest";
            throw new ApplicationException(ApplicationErrorCode.E_InternalError,
                    mesg);
        }
        return repo.save(cassFoi);
    }

    public FeatureOfInterest create(String name, String description,
                           MimeType encodingType, GeoJsonObject location) {
        final com.cgi.kinota.persistence.cassandra.domain.FeatureOfInterest foi = new com.cgi.kinota.persistence.cassandra.domain.FeatureOfInterest(UUIDs.timeBased(),
                name, description, encodingType, location);
        repo.save(foi);
        return foi;
    }

    public void delete(UUID uuid) throws ApplicationException {
        related.deleteFeatureOfInterest(uuid);
    }
}
