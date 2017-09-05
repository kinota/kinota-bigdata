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

package com.cgi.kinota.commons.application;

import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.application.paging.Paginator;
import com.cgi.kinota.commons.application.paging.PagingDescriptor;
import com.cgi.kinota.commons.domain.Entity;
import com.cgi.kinota.commons.domain.HistoricalLocation;
import com.cgi.kinota.commons.domain.Location;
import com.cgi.kinota.commons.domain.Datastream;
import com.cgi.kinota.commons.domain.Observation;
import com.cgi.kinota.commons.domain.Thing;
import com.cgi.kinota.commons.odata.ODataQuery;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by bmiles on 2/1/17.
 */
public interface RelatedEntityManager {

    // Things and Locations
    void associateLocationWithThing(UUID thingId, UUID locationId);
    void disassociateLocationWithThing(UUID thingId, UUID locationId) throws ApplicationException;

    Set<UUID> fetchLocationUuidsForThing(UUID thingId);
    Pair<Long, Iterable<UUID>> fetchLocationUuidsForThing(UUID thingId, ODataQuery q);
    Pair<Long, List<Datastream>> fetchRelatedLocationsForThing(UUID thingId, ODataQuery q);

    Set<UUID> fetchThingUuidsForLocation(UUID locationId);
    Pair<Long, Iterable<UUID>> fetchThingUuidsForLocation(UUID locationId, ODataQuery q);
    Pair<Long, List<Thing>> fetchRelatedThingsForLocation(UUID locationId, ODataQuery q);

    // Things and HistoricalLocations
    void associateHistoricalLocationWithThing(UUID thingId, UUID historicalLocationId);
    void disassociateHistoricalLocationWithThing(UUID thingId, UUID historicalLocationId);

    Set<UUID> fetchHistoricalLocationUuidsForThing(UUID thingId);
    Pair<Long, Iterable<UUID>> fetchHistoricalLocationUuidsForThing(UUID thingId, ODataQuery q);
    Pair<Long, List<HistoricalLocation>> fetchRelatedHistoricalLocationsForThing(UUID thingId, ODataQuery q);

    UUID fetchThingUuidForHistoricalLocation(UUID historicalLocationId);

    // Locations and HistoricalLocations
    void associateHistoricalLocationWithLocation(UUID locationId, UUID historicalLocationId);

    Set<UUID> fetchHistoricalLocationUuidsForLocation(UUID locationId);
    Pair<Long, Iterable<UUID>> fetchHistoricalLocationUuidsForLocation(UUID locationId, ODataQuery q);
    Pair<Long, List<HistoricalLocation>> fetchRelatedHistoricalLocationsForLocation(UUID locationId, ODataQuery q);

    Set<UUID> fetchLocationUuidsForHistoricalLocation(UUID historicalLocationId);
    Pair<Long, Iterable<UUID>> fetchLocationUuidsForHistoricalLocation(UUID historicalLocationId, ODataQuery q);
    Pair<Long, List<Location>> fetchRelatedLocationsForHistoricalLocation(UUID historicalLocationId, ODataQuery q);

    // Datastreams and Things
    void associateThingWithDatastream(UUID datastreamId, UUID thingId);
    void disassociateThingWithDatastream(UUID datastreamId, UUID thingId);

    Set<UUID> fetchDatastreamUuidsForThing(UUID thingId);
    Pair<Long, Iterable<UUID>> fetchDatastreamUuidsForThing(UUID thingId, ODataQuery q);
    Pair<Long, List<Datastream>> fetchRelatedDatastreamsForThing(UUID thingId, ODataQuery q);

    // Datastreams and Sensors
    void associateSensorWithDatastream(UUID datastreamId, UUID sensorId);
    void disassociateSensorWithDatastream(UUID datastreamId, UUID sensorId);

    Set<UUID> fetchDatastreamUuidsForSensor(UUID sensorId);
    Pair<Long, Iterable<UUID>> fetchDatastreamUuidsForSensor(UUID sensorId, ODataQuery q);
    Pair<Long, List<Datastream>> fetchRelatedDatastreamsForSensor(UUID sensorId, ODataQuery q);

    // Datastreams and ObservedProperties
    void associateObservedPropertyWithDatastream(UUID datastreamId, UUID observedPropertyId);
    void disassociateObservedPropertyWithDatastream(UUID datastreamId, UUID observedPropertyId);

    Set<UUID> fetchDatastreamUuidsForObservedProperty(UUID observedPropertyId);
    Pair<Long, Iterable<UUID>> fetchDatastreamUuidsForObservedProperty(UUID observedPropertyId, ODataQuery q);
    Pair<Long, List<Datastream>> fetchRelatedDatastreamsForObservedProperty(UUID observedPropertyId, ODataQuery q);

    // Observations related to Datastreams and FeaturesOfInterest
    void associateRelatedObservation(Observation observation);
    void deleteObservationsForDatastream(UUID datastreamId, boolean updateDatastreamSummaries) throws ApplicationException;
    void deleteObservationsForFeatureOfInterest(UUID featureOfInterestId, boolean updateDatastreamSummaries) throws ApplicationException;

    Pair<Long, List<Observation>> fetchRelatedObservationsForFeatureOfInterest(UUID featureOfInterestId,
                                                                               ODataQuery q);
    Pair<Long, Iterable<UUID>> fetchRelatedObservationUuidsForFeatureOfInterest(UUID featureOfInterestId, ODataQuery q);

    Pair<Long, List<Observation>> fetchRelatedObservationsForDatastream(UUID datastreamId, ODataQuery q);
    Pair<Long, Iterable<UUID>> fetchRelatedObservationUuidsForDatastream(UUID datastreamId, ODataQuery q);

    default Pair<Long, Iterable<UUID>> fetchRelatedEntityUuidsForEntity(UUID entityId, ODataQuery q,
                                                                        Function<UUID, Set<UUID>> fetchRelatedUuidsForEntity) {
        // Handle paging
        PagingDescriptor pd = Paginator.extractPagingDescriptor(q);
        Integer top = pd.getTop();
        Integer skip = pd.getSkip();

        Set<UUID> allRelatedUuids = fetchRelatedUuidsForEntity.apply(entityId);
        long count = (long) allRelatedUuids.size();

        List<UUID> uuids = allRelatedUuids.stream().skip(skip).limit(top).collect(Collectors.toList());
        return new ImmutablePair<>(count, uuids);
    }

    default <U extends Entity> Pair<Long, List<U>> fetchRelatedEntitiesForEntity(UUID entityId, ODataQuery q,
                                                                                 Function<UUID, Set<UUID>> fetchRelatedUuidsForEntity,
                                                                                 QueryableService svc) {
        // Handle paging
        PagingDescriptor pd = Paginator.extractPagingDescriptor(q);
        Integer top = pd.getTop();
        Integer skip = pd.getSkip();

        Set<UUID> allRelatedUuids = fetchRelatedUuidsForEntity.apply(entityId);
        long count = (long) allRelatedUuids.size();

        // For whatever reason this won't compile:
        //   List<U> relatedEntities = allRelatedUuids.stream().skip(skip).limit(top).map(svc::findOne).collect(Collectors.toList());
        //   (our use of generics must be incorrect) So we do this instead ...
        Object[] relatedEntities = allRelatedUuids.stream().skip(skip).limit(top).map(svc::findOne).toArray();
        List relatedEntitiesList = Arrays.asList(relatedEntities);
        return new ImmutablePair<>(count, relatedEntitiesList);
    }

    // Deletion functions that cascade across related entities.
    //   Note: These should be used rather than directly using the delete method
    //   from the respective entity repositories, even for entities that do not
    //   have integrity constraints for deletion (e.g. Observation and
    //   HistoricalLocation).
    void deleteThing(UUID thingId) throws ApplicationException;
    void deleteLocation(UUID locationId) throws ApplicationException;
    void deleteHistoricalLocation(UUID historicalLocationId) throws ApplicationException;
    void deleteSensor(UUID sensorId) throws ApplicationException;
    void deleteObservedProperty(UUID observedPropertyId) throws ApplicationException;
    void deleteDatastream(UUID datastreamId) throws ApplicationException;
    void deleteObservation(UUID observationId) throws ApplicationException;
    void deleteFeatureOfInterest(UUID featureOfInterestId) throws ApplicationException;
}
