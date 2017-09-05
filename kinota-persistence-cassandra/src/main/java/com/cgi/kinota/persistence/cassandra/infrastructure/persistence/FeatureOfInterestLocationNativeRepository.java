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

import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.domain.Location;

import com.cgi.kinota.persistence.cassandra.config.SpringDataCassandraConfig;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static com.cgi.kinota.commons.Constants.MATERIALIZED_VIEW_FEATURE_OF_INTEREST_LOCATION;

/**
 * Persistence repository for FeatureOfInterest objects, written against the Datastax driver instead of
 * Spring Data Cassandra.
 *
 * Created by bmiles on 4/28/17.
 */
@Repository
public class FeatureOfInterestLocationNativeRepository {

    @Autowired
    SpringDataCassandraConfig config;

    public UUID findFeatureOfInterestWithLocation(Location location) throws ApplicationException {
        UUID u = null;
        try {
            Session s = config.session().getObject();
            // It is probably safe to do this as only valid GeoJSON objects can be stored in Location.location,
            //   thus we should be safe from CQL injection attacks.  I would have used a parameterized
            //   statement, but that results in a NullPointerException somewhere in the Datastax driver.
            ResultSet rs = s.execute("SELECT id FROM " + MATERIALIZED_VIEW_FEATURE_OF_INTEREST_LOCATION + " WHERE location = '" + location.getLocationAsString() + "'");
            Row r = rs.one();
            if (r != null) {
                u = r.getUUID(0);
            }
        } catch (ClassNotFoundException e) {
            throw new ApplicationException(ApplicationErrorCode.E_InternalError,
                    e.getMessage());
        }
        return u;
    }
}
