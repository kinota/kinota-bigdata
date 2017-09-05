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

import com.cgi.kinota.persistence.cassandra.domain.RelatedObservation;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.cgi.kinota.commons.Constants.TABLE_OBSERVATION;
import static com.cgi.kinota.commons.Constants.TABLE_RELATED_OBSERVATION;

/**
 * Created by bmiles on 3/21/17.
 */
public interface RelatedObservationRepository extends CassandraRepository<RelatedObservation> {
    @Query("SELECT * FROM " + TABLE_RELATED_OBSERVATION + " WHERE featureOfInterestId = :featureOfInterestId AND datastreamId = :datastreamId AND year IN (:years) LIMIT :limit")
    RelatedObservation[] findAll(@Param("featureOfInterestId") UUID featureOfInterestId,
                                 @Param("datastreamId") UUID datastreamId,
                                 @Param("years") List<Integer> years,
                                 @Param("limit") Integer limit);

    @Query("SELECT COUNT(*) FROM " + TABLE_RELATED_OBSERVATION + " WHERE featureOfInterestId = :featureOfInterestId AND datastreamId = :datastreamId AND year IN (:years)")
    Long countAll(@Param("featureOfInterestId") UUID featureOfInterestId,
                  @Param("datastreamId") UUID datastreamId,
                  @Param("years") List<Integer> years);

    @Query("SELECT * FROM " + TABLE_RELATED_OBSERVATION + " WHERE featureOfInterestId = :featureOfInterestId AND datastreamId = :datastreamId AND year IN (:years)")
    List<RelatedObservation> findAll(@Param("featureOfInterestId") UUID featureOfInterestId,
                                     @Param("datastreamId") UUID datastreamId,
                                     @Param("years") List<Integer> years);

    @Query("SELECT * FROM " + TABLE_RELATED_OBSERVATION + " WHERE featureOfInterestId = :featureOfInterestId AND datastreamId = :datastreamId AND year = :year LIMIT :limit")
    RelatedObservation[] findAll(@Param("featureOfInterestId") UUID featureOfInterestId,
                                 @Param("datastreamId") UUID datastreamId,
                                 @Param("year") Integer year,
                                 @Param("limit") Integer limit);

    @Query("DELETE FROM " + TABLE_RELATED_OBSERVATION + " WHERE featureOfInterestId = ?0 AND datastreamId = ?1 AND year = ?2 AND phenomenonTime = ?3")
    void delete(@Param("featureOfInterestId") UUID featureOfInterestId,
                @Param("datastreamId") UUID datastreamId,
                @Param("year") Integer year,
                @Param("phenomenonTime") Date phenomenonTime);

    // Atomically delete observations and related observations
    @Query("BEGIN BATCH " +
            "DELETE FROM " + TABLE_OBSERVATION + " WHERE id IN (:observationIds); " +
            "DELETE FROM " + TABLE_RELATED_OBSERVATION + " WHERE featureOfInterestId = :featureOfInterestId AND datastreamId = :datastreamId AND year IN (:years); " +
            "APPLY BATCH")
    void deleteObservations(@Param("observationIds") List<UUID> observationIds,
                            @Param("featureOfInterestId") UUID featureOfInterestId,
                            @Param("datastreamId") UUID datastreamId,
                            @Param("years") List<Integer> years);

    // Atomically delete an observation and its related observation
    @Query("BEGIN BATCH " +
            "DELETE FROM " + TABLE_OBSERVATION + " WHERE id = :observationId; " +
            "DELETE FROM " + TABLE_RELATED_OBSERVATION + " WHERE featureOfInterestId = :featureOfInterestId AND datastreamId = :datastreamId AND year = :year AND phenomenontime = :phenomenonTime; " +
            "APPLY BATCH")
    void deleteObservation(@Param("observationId") UUID observationId,
                           @Param("featureOfInterestId") UUID featureOfInterestId,
                           @Param("datastreamId") UUID datastreamId,
                           @Param("year") Integer year,
                           @Param("phenomenonTime") Date phenomenonTime);
}
