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

import com.cgi.kinota.persistence.cassandra.domain.FeatureOfInterestObservationDatastreamYear;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.cgi.kinota.commons.Constants.TABLE_FEATURE_OF_INTEREST_OBSERVATION_DS_YEAR;

/**
 * Created by bmiles on 4/19/17.
 */
public interface FeatureOfInterestObservationDatastreamYearRepository extends CassandraRepository<FeatureOfInterestObservationDatastreamYear> {
    @Query("SELECT * FROM " + TABLE_FEATURE_OF_INTEREST_OBSERVATION_DS_YEAR + " WHERE featureOfInterestId = ?0")
    List<FeatureOfInterestObservationDatastreamYear> findDatastreamYearsForFeatureOfInterest(@Param("featureOfInterestId") UUID featureOfInterestId);

    @Query("DELETE FROM " + TABLE_FEATURE_OF_INTEREST_OBSERVATION_DS_YEAR + " WHERE featureOfInterestId = ?0")
    void deleteDatastreamYearsForFeatureOfInterest(@Param("featureOfInterestId") UUID featureOfInterestId);

    @Query("DELETE FROM " + TABLE_FEATURE_OF_INTEREST_OBSERVATION_DS_YEAR + " WHERE featureofinterestid IN (:featureOfInterestIds) AND datastreamId = :datastreamId")
    void deleteDatastreamYearsForFeaturesOfInterest(@Param("featureOfInterestIds") Set<UUID> featureOfInterestIds,
                                                    @Param("datastreamId") UUID datastreamId);

    @Query("DELETE FROM " + TABLE_FEATURE_OF_INTEREST_OBSERVATION_DS_YEAR + " WHERE featureofinterestid = :featureOfInterestId AND datastreamId = :datastreamId AND year = :year")
    void deleteDatastreamYearForFeaturesOfInterest(@Param("featureOfInterestId") UUID featureOfInterestId,
                                                   @Param("datastreamId") UUID datastreamId,
                                                   @Param("year") Integer year);

    @Query("SELECT datastreamid FROM " + TABLE_FEATURE_OF_INTEREST_OBSERVATION_DS_YEAR + " WHERE featureOfInterestId = ?0")
    Set<UUID> findDatastreamIdsForFeatureOfInterest(@Param("featureOfInterestId") UUID featureOfInterestId);
}
