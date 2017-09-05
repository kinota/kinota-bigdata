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

import com.cgi.kinota.persistence.cassandra.config.SpringDataCassandraConfig;
import com.cgi.kinota.persistence.cassandra.domain.support.DatastreamTemporalSummary;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

import static com.cgi.kinota.commons.Constants.TABLE_RELATED_OBSERVATION;

/**
 * Persistence repository for RelatedObservation objects, written against the Datastax driver instead of
 * Spring Data Cassandra.
 *
 * Created by bmiles on 6/27/17.
 */
@Repository
public class RelatedObservationNativeRepository {

    @Autowired
    SpringDataCassandraConfig config;

    public DatastreamTemporalSummary fetchMinMaxPhenomenonTime(UUID featureOfInterestId,
                                                               UUID datastreamId,
                                                               List<Integer> years) {
        DatastreamTemporalSummary summ = null;
        try {
            Session s = config.session().getObject();
            StringJoiner jn = new StringJoiner(",");
            years.stream().forEach(i -> jn.add(i.toString()));
            String yearsStr = jn.toString();
            ResultSet rs = s.execute("SELECT MIN(phenomenontime), MAX(phenomenontime), MIN(resulttime), MAX(resulttime) FROM " + TABLE_RELATED_OBSERVATION + " WHERE featureofinterestid = ? AND datastreamid = ? AND year IN (" + yearsStr + ")",
                    featureOfInterestId, datastreamId);
            Row r = rs.one();
            if (r != null) {
                summ = new DatastreamTemporalSummary();
                summ.setPhenomenonTimeBegin(r.getTimestamp(0));
                summ.setPhenomenonTimeEnd(r.getTimestamp(1));
                summ.setResultTimeBegin(r.getTimestamp(2));
                summ.setResultTimeEnd(r.getTimestamp(3));
            }
        } catch (ClassNotFoundException e) {
            throw new ApplicationException(ApplicationErrorCode.E_InternalError,
                    e.getMessage());
        }
        return summ;
    }

    public Long fetchRelatedObservationCount(UUID featureOfInterestId,
                                             UUID datastreamId,
                                             Integer year) {
        Long count = null;

        try {
            Session s = config.session().getObject();
            ResultSet rs = s.execute("SELECT COUNT(*) FROM relatedobservation WHERE featureofinterestid=? AND datastreamid=? AND year=?",
                    featureOfInterestId, datastreamId, year);
            Row r = rs.one();
            if (r != null) {
                count = r.getLong(0);
            }
        } catch (ClassNotFoundException e) {
            throw new ApplicationException(ApplicationErrorCode.E_InternalError,
                    e.getMessage());
        }

        return count;
    }
}
