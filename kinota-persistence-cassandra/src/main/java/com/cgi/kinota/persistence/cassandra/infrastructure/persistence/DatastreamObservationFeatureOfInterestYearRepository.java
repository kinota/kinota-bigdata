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

import com.cgi.kinota.persistence.cassandra.domain.DatastreamObservationFeatureOfInterestYear;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

import static com.cgi.kinota.commons.Constants.TABLE_DATASTREAM_OBSERVATION_FOI_YEAR;

/**
 * Created by bmiles on 4/19/17.
 */
public interface DatastreamObservationFeatureOfInterestYearRepository extends CassandraRepository<DatastreamObservationFeatureOfInterestYear> {
    @Query("SELECT * FROM " + TABLE_DATASTREAM_OBSERVATION_FOI_YEAR + " WHERE datastreamId = ?0")
    List<DatastreamObservationFeatureOfInterestYear> findFeatureOfInterestYearsForDatastream(@Param("datastreamId") UUID datastreamId);

    @Query("DELETE FROM " + TABLE_DATASTREAM_OBSERVATION_FOI_YEAR + " WHERE datastreamId = ?0")
    void deleteFeatureOfInterestYearsForDatastream(@Param("datastreamId") UUID datastreamId);

    @Query("DELETE FROM " + TABLE_DATASTREAM_OBSERVATION_FOI_YEAR + " WHERE datastreamId = :datastreamId AND featureofinterestid = :featureOfInterestId")
    void deleteFeatureOfInterestYearsForDatastream(@Param("datastreamId") UUID datastreamId,
                                                   @Param("featureOfInterestId") UUID featureOfInterestId);

    @Query("DELETE FROM " + TABLE_DATASTREAM_OBSERVATION_FOI_YEAR + " WHERE datastreamId = :datastreamId AND featureofinterestid = :featureOfInterestId AND year = :year")
    void deleteFeatureOfInterestYearForDatastream(@Param("datastreamId") UUID datastreamId,
                                                  @Param("featureOfInterestId") UUID featureOfInterestId,
                                                  @Param("year") Integer year);
}
