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
import com.cgi.kinota.commons.application.FeatureOfInterestService;
import com.cgi.kinota.commons.application.HistoricalLocationService;
import com.cgi.kinota.commons.application.LocationService;
import com.cgi.kinota.commons.application.ObservationService;
import com.cgi.kinota.commons.application.ObservedPropertyService;
import com.cgi.kinota.commons.application.RelatedEntityManager;
import com.cgi.kinota.commons.application.SensorService;
import com.cgi.kinota.commons.application.ThingService;
import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.application.paging.Paginator;
import com.cgi.kinota.commons.application.paging.PagingDescriptor;
import com.cgi.kinota.commons.domain.Datastream;
import com.cgi.kinota.commons.domain.FeatureOfInterest;
import com.cgi.kinota.commons.domain.HistoricalLocation;
import com.cgi.kinota.commons.domain.Location;
import com.cgi.kinota.commons.domain.Observation;
import com.cgi.kinota.commons.domain.ObservedProperty;
import com.cgi.kinota.commons.domain.Sensor;
import com.cgi.kinota.commons.domain.Thing;
import com.cgi.kinota.commons.odata.ODataQuery;

import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.ThingRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.DatastreamObservationFeatureOfInterestYearRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.DatastreamRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.FeatureOfInterestObservationDatastreamYearRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.FeatureOfInterestRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.HistoricalLocationLocationRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.HistoricalLocationRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.HistoricalLocationThingRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.LocationHistoricalLocationRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.LocationRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.LocationThingRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.ObservedPropertyDatastreamRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.ObservedPropertyRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.RelatedObservationNativeRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.RelatedObservationRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.SensorDatastreamRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.SensorRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.ThingDatastreamRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.ThingHistoricalLocationRepository;
import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.ThingLocationRepository;
import com.cgi.kinota.persistence.cassandra.application.support.HistoricalLocationServiceHelper;
import com.cgi.kinota.persistence.cassandra.domain.DatastreamObservationFeatureOfInterestYear;
import com.cgi.kinota.persistence.cassandra.domain.FeatureOfInterestObservationDatastreamYear;
import com.cgi.kinota.persistence.cassandra.domain.HistoricalLocationLocation;
import com.cgi.kinota.persistence.cassandra.domain.HistoricalLocationThing;
import com.cgi.kinota.persistence.cassandra.domain.LocationHistoricalLocation;
import com.cgi.kinota.persistence.cassandra.domain.LocationThing;
import com.cgi.kinota.persistence.cassandra.domain.ObservedPropertyDatastream;
import com.cgi.kinota.persistence.cassandra.domain.RelatedObservation;
import com.cgi.kinota.persistence.cassandra.domain.SensorDatastream;
import com.cgi.kinota.persistence.cassandra.domain.ThingDatastream;
import com.cgi.kinota.persistence.cassandra.domain.ThingHistoricalLocation;
import com.cgi.kinota.persistence.cassandra.domain.ThingLocation;
import com.cgi.kinota.persistence.cassandra.domain.support.DatastreamTemporalSummary;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.geojson.Feature;
import org.geojson.Point;
import org.geojson.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.cgi.kinota.commons.geo.Utility.createBoundingBoxPolygonFromPoint;
import static com.cgi.kinota.commons.geo.Utility.createBoundingBoxPolygonFromPoints;
import static com.cgi.kinota.commons.geo.Utility.updateBoundingBox;

import static org.springframework.data.cassandra.repository.support.BasicMapId.id;

/**
 * Created by bmiles on 2/1/17.
 */
@Service
public class RelatedEntityManagerImpl implements RelatedEntityManager {

    private static final Logger logger = LoggerFactory.getLogger(RelatedEntityManagerImpl.class);

    @Autowired
    ThingRepository thingRepo;

    @Autowired
    LocationRepository locRepo;

    @Autowired
    HistoricalLocationRepository histLocRepo;

    @Autowired
    SensorRepository sensorRepo;

    @Autowired
    ObservedPropertyRepository obsPropertyRepo;

    @Autowired
    DatastreamRepository dsRepo;

    @Autowired
    FeatureOfInterestRepository foiRepo;

    @Autowired
    LocationThingRepository locationThingRepo;

    @Autowired
    ThingLocationRepository thingLocationRepo;

    @Autowired
    HistoricalLocationThingRepository historicalLocationThingRepo;

    @Autowired
    ThingHistoricalLocationRepository thingHistoricalLocationRepo;

    @Autowired
    HistoricalLocationLocationRepository historicalLocationLocationRepo;

    @Autowired
    LocationHistoricalLocationRepository locationHistoricalLocationRepo;

    @Autowired
    ThingDatastreamRepository thingDatastreamRepo;

    @Autowired
    SensorDatastreamRepository sensorDatastreamRepo;

    @Autowired
    ObservedPropertyDatastreamRepository observedPropertyDatastreamRepo;

    @Autowired
    RelatedObservationRepository relatedObservationRepo;

    @Autowired
    RelatedObservationNativeRepository relatedObservationNativeRepo;

    @Autowired
    DatastreamObservationFeatureOfInterestYearRepository dsObsFoiYearRepo;

    @Autowired
    FeatureOfInterestObservationDatastreamYearRepository foiObsDsYearRepo;

    @Autowired
    DatastreamService dsSvc;

    @Autowired
    LocationService locSvc;

    @Autowired
    HistoricalLocationService histLocSvc;

    @Autowired
    ThingService thingSvc;

    @Autowired
    ObservedPropertyService obsPropertySvc;

    @Autowired
    SensorService sensorSvc;

    @Autowired
    FeatureOfInterestService foiSvc;

    @Autowired
    ObservationService obsSvc;


    public void associateLocationWithThing(UUID thingId, UUID locationId) {
        // TODO: Make atomic with custom BATCH query
        LocationThing lt = new LocationThing(locationId, thingId);
        ThingLocation tl = new ThingLocation(thingId, locationId);

        locationThingRepo.save(lt);
        thingLocationRepo.save(tl);

        HistoricalLocationServiceHelper.createHistoricalLocation(new Date(),
                thingId, locationId,
                histLocRepo, this);
    }

    public void disassociateLocationWithThing(UUID thingId, UUID locationId) throws ApplicationException {
        // TODO: Make atomic with custom BATCH query
        locationThingRepo.delete(locationId, thingId);
        thingLocationRepo.delete(thingId, locationId);
    }

    protected void disassociateLocationsFromThing(UUID thingId) throws ApplicationException {
        thingLocationRepo.findAll(thingId).stream()
                .forEach(tl -> thingLocationRepo.disassociateLocationsFromThing(tl.getThingId(), tl.getLocationId()));
    }

    protected void disassociateThingsFromLocation(UUID locationId) throws ApplicationException {
        locationThingRepo.findAll(locationId).stream()
                .forEach(lt -> disassociateLocationWithThing(lt.getThingId(), lt.getLocationId()));
    }

    public Set<UUID> fetchLocationUuidsForThing(UUID thingId) {
        Set<UUID> locationUuids = new HashSet<>();
        for (ThingLocation tl : thingLocationRepo.findAll(thingId)) {
            locationUuids.add(tl.getLocationId());
        }
        return locationUuids;
    }

    public Pair<Long, Iterable<UUID>> fetchLocationUuidsForThing(UUID thingId, ODataQuery q) {
        return fetchRelatedEntityUuidsForEntity(thingId, q, this::fetchLocationUuidsForThing);
    }

    public Pair<Long, List<Datastream>> fetchRelatedLocationsForThing(UUID thingId, ODataQuery q) {
        return fetchRelatedEntitiesForEntity(thingId, q, this::fetchLocationUuidsForThing, locSvc);
    }

    public Set<UUID> fetchThingUuidsForLocation(UUID locationId) {
        Set<UUID> thingUuids = new HashSet<>();
        for (LocationThing lt : locationThingRepo.findAll(locationId)) {
            thingUuids.add(lt.getThingId());
        };
        return thingUuids;
    }

    public Pair<Long, Iterable<UUID>> fetchThingUuidsForLocation(UUID locationId, ODataQuery q) {
        return fetchRelatedEntityUuidsForEntity(locationId, q, this::fetchThingUuidsForLocation);
    }

    public Pair<Long, List<Thing>> fetchRelatedThingsForLocation(UUID locationId, ODataQuery q) {
        return fetchRelatedEntitiesForEntity(locationId, q, this::fetchThingUuidsForLocation, thingSvc);
    }

    public void associateHistoricalLocationWithThing(UUID thingId, UUID historicalLocationId) {
        HistoricalLocationThing ht = new HistoricalLocationThing(historicalLocationId, thingId);
        ThingHistoricalLocation th = new ThingHistoricalLocation(thingId, historicalLocationId);

        historicalLocationThingRepo.save(ht);
        thingHistoricalLocationRepo.save(th);
    }

    public void disassociateHistoricalLocationWithThing(UUID thingId, UUID historicalLocationId) {
        historicalLocationThingRepo.delete(historicalLocationId, thingId);
        thingHistoricalLocationRepo.delete(thingId, historicalLocationId);
    }

    protected void disassociateHistoricalLocationsFromThing(UUID thingId) throws ApplicationException {
        thingHistoricalLocationRepo.findAll(thingId).stream()
                .forEach(thl -> thingHistoricalLocationRepo.disassociateHistoricalLocationsFromThing(thl.getThingId(),
                        thl.getHistoricalLocationId()));
    }

    protected void disassociateThingFromHistoricalLocation(UUID historicalLocationId) throws ApplicationException {
        historicalLocationThingRepo.findAll(historicalLocationId).stream()
                .forEach(hlt -> thingHistoricalLocationRepo.disassociateHistoricalLocationsFromThing(hlt.getThingId(),
                        hlt.getHistoricalLocationId()));
    }

    public Set<UUID> fetchHistoricalLocationUuidsForThing(UUID thingId) {
        Set<UUID> histLocationUuids = new HashSet<>();
        for (ThingHistoricalLocation thl : thingHistoricalLocationRepo.findAll(thingId)) {
            histLocationUuids.add(thl.getHistoricalLocationId());
        }
        return histLocationUuids;
    }

    public Pair<Long, Iterable<UUID>> fetchHistoricalLocationUuidsForThing(UUID thingId, ODataQuery q) {
        return fetchRelatedEntityUuidsForEntity(thingId, q, this::fetchHistoricalLocationUuidsForThing);
    }

    public Pair<Long, List<HistoricalLocation>> fetchRelatedHistoricalLocationsForThing(UUID thingId, ODataQuery q) {
        return fetchRelatedEntitiesForEntity(thingId, q, this::fetchHistoricalLocationUuidsForThing, histLocSvc);
    }


    public UUID fetchThingUuidForHistoricalLocation(UUID historicalLocationId) {
        UUID thingUuid = null;
        List<HistoricalLocationThing> hlt = historicalLocationThingRepo.findAll(historicalLocationId);
        if (hlt.size() > 0) {
            thingUuid = hlt.get(0).getThingId();
        }
        return thingUuid;
    }

    public void associateHistoricalLocationWithLocation(UUID locationId, UUID historicalLocationId) {
        HistoricalLocationLocation hl = new HistoricalLocationLocation(historicalLocationId, locationId);
        LocationHistoricalLocation lh = new LocationHistoricalLocation(locationId, historicalLocationId);

        historicalLocationLocationRepo.save(hl);
        locationHistoricalLocationRepo.save(lh);
    }

    protected void disassociateHistoricalLocationFromLocation(UUID historicalLocationId) throws ApplicationException {
        historicalLocationLocationRepo.findAll(historicalLocationId).stream()
                .forEach(hll -> historicalLocationLocationRepo.disassociateHistoricalLocationsFromLocation(hll.getHistoricalLocationId(),
                        hll.getLocationId()));
    }

    public Set<UUID> fetchHistoricalLocationUuidsForLocation(UUID locationId) {
        Set<UUID> histLocationUuids = new HashSet<>();
        for (LocationHistoricalLocation lhl : locationHistoricalLocationRepo.findAll(locationId)) {
            histLocationUuids.add(lhl.getHistoricalLocationId());
        }
        return histLocationUuids;
    }

    public Pair<Long, Iterable<UUID>> fetchHistoricalLocationUuidsForLocation(UUID locationId, ODataQuery q) {
        return fetchRelatedEntityUuidsForEntity(locationId, q, this::fetchHistoricalLocationUuidsForLocation);
    }

    public Pair<Long, List<HistoricalLocation>> fetchRelatedHistoricalLocationsForLocation(UUID locationId, ODataQuery q) {
        return fetchRelatedEntitiesForEntity(locationId, q, this::fetchHistoricalLocationUuidsForLocation, histLocSvc);
    }


    public Set<UUID> fetchLocationUuidsForHistoricalLocation(UUID historicalLocationId) {
        Set<UUID> locationUuids = new HashSet<>();
        for (HistoricalLocationLocation hll : historicalLocationLocationRepo.findAll(historicalLocationId)) {
            locationUuids.add(hll.getLocationId());
        }
        return locationUuids;
    }

    public Pair<Long, Iterable<UUID>> fetchLocationUuidsForHistoricalLocation(UUID historicalLocationId, ODataQuery q) {
        return fetchRelatedEntityUuidsForEntity(historicalLocationId, q, this::fetchLocationUuidsForHistoricalLocation);
    }

    public Pair<Long, List<Location>> fetchRelatedLocationsForHistoricalLocation(UUID historicalLocationId, ODataQuery q) {
        return fetchRelatedEntitiesForEntity(historicalLocationId, q, this::fetchLocationUuidsForHistoricalLocation, locSvc);
    }


    public void associateThingWithDatastream(UUID datastreamId, UUID thingId) {
        ThingDatastream td = new ThingDatastream(thingId, datastreamId);
        thingDatastreamRepo.save(td);
    }

    public void disassociateThingWithDatastream(UUID datastreamId, UUID thingId) {
        thingDatastreamRepo.delete(thingId, datastreamId);
    }

    protected void deleteDatastreamsForThing(UUID thingId) throws ApplicationException {
        thingDatastreamRepo.findAll(thingId).stream().forEach(thingDs -> deleteDatastream(thingDs.getDatastreamId()));
    }

    public Set<UUID> fetchDatastreamUuidsForThing(UUID thingId) {
        Set<UUID> datastreamUuids = new LinkedHashSet<>();
        for (ThingDatastream td : thingDatastreamRepo.findAll(thingId)) {
            datastreamUuids.add(td.getDatastreamId());
        }
        return datastreamUuids;
    }

    public Pair<Long, Iterable<UUID>> fetchDatastreamUuidsForThing(UUID thingId, ODataQuery q) {
        return fetchRelatedEntityUuidsForEntity(thingId, q, this::fetchDatastreamUuidsForThing);
    }

    public Pair<Long, List<Datastream>> fetchRelatedDatastreamsForThing(UUID thingId, ODataQuery q) {
        return fetchRelatedEntitiesForEntity(thingId, q, this::fetchDatastreamUuidsForThing, dsSvc);
    }


    public void associateSensorWithDatastream(UUID datastreamId, UUID sensorId) {
        SensorDatastream sd = new SensorDatastream(sensorId, datastreamId);
        sensorDatastreamRepo.save(sd);
    }

    public void disassociateSensorWithDatastream(UUID datastreamId, UUID sensorId) {
        sensorDatastreamRepo.delete(sensorId, datastreamId);
    }

    public Set<UUID> fetchDatastreamUuidsForSensor(UUID sensorId) {
        Set<UUID> datastreamUuids = new HashSet<>();
        for (SensorDatastream sd : sensorDatastreamRepo.findAll(sensorId)) {
            datastreamUuids.add(sd.getDatastreamId());
        }
        return datastreamUuids;
    }

    public Pair<Long, Iterable<UUID>> fetchDatastreamUuidsForSensor(UUID sensorId, ODataQuery q) {
        return fetchRelatedEntityUuidsForEntity(sensorId, q, this::fetchDatastreamUuidsForSensor);
    }

    public Pair<Long, List<Datastream>> fetchRelatedDatastreamsForSensor(UUID sensorId, ODataQuery q) {
        return fetchRelatedEntitiesForEntity(sensorId, q, this::fetchDatastreamUuidsForSensor, dsSvc);
    }


    public void associateObservedPropertyWithDatastream(UUID datastreamId, UUID observedPropertyId) {
        ObservedPropertyDatastream opd = new ObservedPropertyDatastream(observedPropertyId, datastreamId);
        observedPropertyDatastreamRepo.save(opd);
    }

    public void disassociateObservedPropertyWithDatastream(UUID datastreamId, UUID observedPropertyId) {
        observedPropertyDatastreamRepo.delete(observedPropertyId, datastreamId);
    }

    public Set<UUID> fetchDatastreamUuidsForObservedProperty(UUID observedPropertyId) {
        Set<UUID> datastreamUuids = new HashSet<>();
        for (ObservedPropertyDatastream opd : observedPropertyDatastreamRepo.findAll(observedPropertyId)) {
            datastreamUuids.add(opd.getDatastreamId());
        }
        return datastreamUuids;
    }

    public Pair<Long, Iterable<UUID>> fetchDatastreamUuidsForObservedProperty(UUID observedPropertyId, ODataQuery q) {
        return fetchRelatedEntityUuidsForEntity(observedPropertyId, q, this::fetchDatastreamUuidsForObservedProperty);
    }

    public Pair<Long, List<Datastream>> fetchRelatedDatastreamsForObservedProperty(UUID observedPropertyId, ODataQuery q) {
        return fetchRelatedEntitiesForEntity(observedPropertyId, q, this::fetchDatastreamUuidsForObservedProperty, dsSvc);
    }


    public void associateRelatedObservation(Observation o) {
        RelatedObservation relObs = new RelatedObservation(o.getDatastreamId(),
                o.getPhenomenonTime(), o.getPhenomenonTimeEnd(),
                o.getId(), o.getFeatureOfInterestId(), o.getResultTime(),
                o.getValidTimeBegin(), o.getValidTimeEnd(),
                o.getResultQuality(),
                o.getObservationType(),
                o.getResultString(),
                o.getResultCount(),
                o.getResultMeasurement(),
                o.getResultTruth(),
                o.getParameters());
        relatedObservationRepo.save(relObs);

        FeatureOfInterestObservationDatastreamYear foiObsYear = new FeatureOfInterestObservationDatastreamYear(o.getFeatureOfInterestId(),
                o.getDatastreamId(),
                o.getPhenomenonTimeYear());
        foiObsDsYearRepo.save(foiObsYear);

        DatastreamObservationFeatureOfInterestYear dsObsYear = new DatastreamObservationFeatureOfInterestYear(o.getDatastreamId(),
                o.getFeatureOfInterestId(),
                o.getPhenomenonTimeYear());
        dsObsFoiYearRepo.save(dsObsYear);

        // Update Datastream's phenomenonTimeBegin, phenomenonTimeEnd,
        //   resultTimeBegin, resultTimeEnd, and observedArea
        Datastream d = dsRepo.findOne(id().with("id", o.getDatastreamId()));
        FeatureOfInterest foi = foiRepo.findOne(id().with("id", o.getFeatureOfInterestId()));
        updateDatastreamSummaries(d, o, foi, dsRepo);
    }


    public void deleteObservationsForDatastream(UUID datastreamId, boolean updateDatastreamSummaries) throws ApplicationException {
        Pair<Set<UUID>, List<Integer>> partitionKeys = fetchRelatedObservationParitionKeysForDatastream(datastreamId);
        deleteObservationsForEntity(datastreamId,
                partitionKeys.getLeft(), partitionKeys.getRight(),
                false);

        if (updateDatastreamSummaries) {
            recreateDatastreamSummaries(datastreamId);
        }
    }

    public void deleteObservationsForFeatureOfInterest(UUID featureOfInterestId, boolean updateDatastreamSummaries) throws ApplicationException {
        Pair<Set<UUID>, List<Integer>> partitionKeys = fetchRelatedObservationParitionKeysForFeatureOfInterest(featureOfInterestId);
        deleteObservationsForEntity(featureOfInterestId,
                partitionKeys.getLeft(), partitionKeys.getRight(),
                true);

        if (updateDatastreamSummaries) {
            for (UUID datastreamId : partitionKeys.getLeft()) {
                recreateDatastreamSummaries(datastreamId);
            }
        }
    }

    private void deleteObservationsForEntity(UUID entityId,
                                             Set<UUID> otherEntityParitionKeyIds,
                                             List<Integer> partitionKeyYears,
                                             boolean entityIsFeatureOfInterest) {
        if (entityIsFeatureOfInterest) {
            // Entity is FeatureOfInterest
            for (UUID otherEntityParitionKeyId : otherEntityParitionKeyIds) {
                List<UUID> obsIds = relatedObservationRepo.findAll(entityId, otherEntityParitionKeyId, partitionKeyYears)
                        .stream()
                        .map(o -> o.getId())
                        .collect(Collectors.toList());
                relatedObservationRepo.deleteObservations(obsIds, entityId, otherEntityParitionKeyId, partitionKeyYears);
                dsObsFoiYearRepo.deleteFeatureOfInterestYearsForDatastream(otherEntityParitionKeyId, entityId);
            }
            foiObsDsYearRepo.deleteDatastreamYearsForFeatureOfInterest(entityId);
        } else {
            // Entity is Datastream
            DatastreamTemporalSummary summ = new DatastreamTemporalSummary();
            
            for (UUID otherEntityParitionKeyId : otherEntityParitionKeyIds) {
                List<UUID> obsIds = relatedObservationRepo.findAll(otherEntityParitionKeyId, entityId, partitionKeyYears)
                        .stream()
                        .map(o -> o.getId())
                        .collect(Collectors.toList());
                relatedObservationRepo.deleteObservations(obsIds, otherEntityParitionKeyId, entityId, partitionKeyYears);
            }
            foiObsDsYearRepo.deleteDatastreamYearsForFeaturesOfInterest(otherEntityParitionKeyIds, entityId);
            dsObsFoiYearRepo.deleteFeatureOfInterestYearsForDatastream(entityId);
        }
    }

    public Pair<Long, List<Observation>> fetchRelatedObservationsForFeatureOfInterest(UUID featureOfInterestId,
                                                                                             ODataQuery q) {
        Pair<Set<UUID>, List<Integer>> partitionKeys = fetchRelatedObservationParitionKeysForFeatureOfInterest(featureOfInterestId);
        return fetchRelatedObservationsForEntity(featureOfInterestId, partitionKeys.getLeft(), partitionKeys.getRight(),
                q, true);
    }

    private Pair<Set<UUID>, List<Integer>> fetchRelatedObservationParitionKeysForFeatureOfInterest(UUID featureOfInterestId) {
        List<FeatureOfInterestObservationDatastreamYear> rel = foiObsDsYearRepo.findDatastreamYearsForFeatureOfInterest(featureOfInterestId);
        Set<UUID> dsIds = new HashSet<>();
        List<Integer> years = new ArrayList<>();
        for (FeatureOfInterestObservationDatastreamYear r : rel) {
            dsIds.add(r.getDatastreamId());
            years.add(r.getYear());
        }
        return new ImmutablePair<>(dsIds, years);
    }

    public Pair<Long, List<Observation>> fetchRelatedObservationsForDatastream(UUID datastreamId, ODataQuery q) {
        Pair<Set<UUID>, List<Integer>> partitionKeys = fetchRelatedObservationParitionKeysForDatastream(datastreamId);
        return fetchRelatedObservationsForEntity(datastreamId, partitionKeys.getLeft(), partitionKeys.getRight(),
                q, false);
    }

    private void deleteFeatureOfInterestForDatastream(UUID featureOfInterestId, UUID datastreamId) {
        Set<UUID> dsIds = foiObsDsYearRepo.findDatastreamIdsForFeatureOfInterest(featureOfInterestId);
        if (dsIds.contains(datastreamId) && dsIds.size() == 1) {
            // Only delete the FeatureOfInterest if no other Datastreans reference it
            foiSvc.delete(featureOfInterestId);
        }
    }

    private Pair<Set<UUID>, List<Integer>> fetchRelatedObservationParitionKeysForDatastream(UUID datastreamId) {
        List<DatastreamObservationFeatureOfInterestYear> rel = dsObsFoiYearRepo.findFeatureOfInterestYearsForDatastream(datastreamId);

        Set<UUID> foiIds = new HashSet<>();
        List<Integer> years = new ArrayList<>();
        for (DatastreamObservationFeatureOfInterestYear r : rel) {
            foiIds.add(r.getFeatureOfInterestId());
            years.add(r.getYear());
        }
        return new ImmutablePair<>(foiIds, years);
    }

    private Pair<Long, List<Observation>> fetchRelatedObservationsForEntity(UUID entityId,
                                                                                   Set<UUID> otherEntityParitionKeyIds,
                                                                                   List<Integer> partitionKeyYears,
                                                                                   ODataQuery q,
                                                                                   boolean entityIsFeatureOfInterest) {
        // Handle paging
        PagingDescriptor pd = Paginator.extractPagingDescriptor(q);
        Integer top = pd.getTop();
        Integer skip = pd.getSkip();
        Integer limit = pd.getLimit();

        // Destination array for observations
        RelatedObservation[] obs = new RelatedObservation[top];

        // Bookkeeping variables
        boolean obsEmpty = true;
        long count = 0l;
        int currIdx = 0;
        int numPartitions = 0;
        boolean skipped = false;
        for (UUID otherEntityParitionKeyId : otherEntityParitionKeyIds) {

            // Count all observations for this partition
            if (entityIsFeatureOfInterest) {
                count += relatedObservationRepo.countAll(entityId, otherEntityParitionKeyId, partitionKeyYears);
            } else {
                count += relatedObservationRepo.countAll(otherEntityParitionKeyId, entityId, partitionKeyYears);
            }
            // No more results need to be fetched, but we need to know how many observations
            //   there are in subsequent partitions ...
            if (top <= 0) continue;

            for (Integer year : partitionKeyYears) {

                RelatedObservation[] tmp = null;
                if (entityIsFeatureOfInterest) {
                    tmp = relatedObservationRepo.findAll(entityId, otherEntityParitionKeyId, year, limit);
                } else {
                    tmp = relatedObservationRepo.findAll(otherEntityParitionKeyId, entityId, year, limit);
                }
                int tmpSize = tmp.length;

                int skipOverflow = skip - tmpSize;
                if (skipOverflow > 1) {
                    // There are not enough elements in tmp to satisfy skip length
                    //   count tmp length toward skip and move on to the next partition
                    skip = skipOverflow;
                    continue;
                }

                // TODO: Do additional client-side filtering here
                int filteredSize = tmpSize;

                // Copy to obs array
                int srcPos = 0;
                if (!skipped) {
                    srcPos = skip;
                    skip = 0;
                    skipped = true;
                }
                int amountToCopy = top;
                int overflow = (srcPos + amountToCopy) - filteredSize;
                if (overflow > 0) {
                    amountToCopy = top - overflow;
                }
                logger.debug("tmp.length: " + tmp.length + ", srcPos: " + srcPos +
                        ", obs.length: " + obs.length + ", currIdx: " + currIdx +
                        ", amountToCopy: " + amountToCopy);
                System.arraycopy(tmp, srcPos, obs, currIdx, amountToCopy);

                // Book keeping
                currIdx += amountToCopy;
                top -= amountToCopy;
                numPartitions++;
                obsEmpty = false;
            }
        }
        // Make sure observations are returned most recent phenomenonTime first.
        //   We only do this if we are reading across partitions, and thus
        //   need to collate.
        logger.debug("numPartitions: " + numPartitions);
        if (obsEmpty) {
            logger.debug("obs is null");
            obs = new RelatedObservation[0];
        } else {
            // Move to an array the exact size needed so there are no null values, which would complicate
            //   sorting and further handling down the line
            int finalObsLength = currIdx;
            RelatedObservation[] finalObs = new RelatedObservation[finalObsLength];
            System.arraycopy(obs, 0, finalObs, 0, finalObsLength);
            obs = finalObs;

            if (numPartitions > 1) {
                logger.debug("sorting array of length " + obs.length + "... ");
                Arrays.sort(obs, Observation::comparePhenomenonTimeDesc);
            }
        }
        logger.debug("returning, obs.length: " + obs.length + "...");
        return new ImmutablePair<>(count, Arrays.asList(obs));
    }

    private List<UUID> fetchedRelatedObservationUuidsForFeatureOfInterest(UUID featureOfInterestId) {
        List<UUID> ids = new ArrayList();
        Pair<Set<UUID>, List<Integer>> partitionKeys = fetchRelatedObservationParitionKeysForFeatureOfInterest(featureOfInterestId);
        for (UUID dataStreamId : partitionKeys.getLeft()) {
            ids.addAll(relatedObservationRepo.findAll(featureOfInterestId, dataStreamId, partitionKeys.getRight()).stream().map(o -> o.getId()).collect(Collectors.toList()));
        }

        return ids;
    }

    public Pair<Long, Iterable<UUID>> fetchRelatedObservationUuidsForFeatureOfInterest(UUID featureOfInterestId, ODataQuery q) {
        Pair<Long, List<Observation>> relatedObs = fetchRelatedObservationsForFeatureOfInterest(featureOfInterestId, q);
        return new ImmutablePair<>(relatedObs.getLeft(),
                relatedObs.getRight().stream().map(o -> o.getId()).collect(Collectors.toList()));
    }

    public Pair<Long, Iterable<UUID>> fetchRelatedObservationUuidsForDatastream(UUID datastreamId, ODataQuery q) {
        Pair<Long, List<Observation>> relatedObs = fetchRelatedObservationsForDatastream(datastreamId, q);
        return new ImmutablePair<>(relatedObs.getLeft(),
                relatedObs.getRight().stream().map(o -> o.getId()).collect(Collectors.toList()));
    }

    public static void reduceDatastreamTemporalSummary(DatastreamTemporalSummary summ,
                                                       DatastreamTemporalSummary newSumm) {
        // Phenomenon time begin
        if (summ.getPhenomenonTimeBegin() == null) {
            if (newSumm.getPhenomenonTimeBegin() != null) {
                summ.setPhenomenonTimeBegin(newSumm.getPhenomenonTimeBegin());
            }
        } else {
            if (newSumm.getPhenomenonTimeBegin() != null) {
                if (newSumm.getPhenomenonTimeBegin().before(summ.getPhenomenonTimeBegin())) {
                    summ.setPhenomenonTimeBegin(newSumm.getPhenomenonTimeBegin());
                }
            }
        }

        // Phenomenon time end
        if (summ.getPhenomenonTimeEnd() == null) {
            if (newSumm.getPhenomenonTimeEnd() != null) {
                summ.setPhenomenonTimeEnd(newSumm.getPhenomenonTimeEnd());
            }
        } else {
            if (newSumm.getPhenomenonTimeEnd() != null) {
                if (newSumm.getPhenomenonTimeEnd().after(summ.getPhenomenonTimeEnd())) {
                    summ.setPhenomenonTimeEnd(newSumm.getPhenomenonTimeEnd());
                }
            }
        }

        // Result time begin
        if (summ.getResultTimeBegin() == null) {
            if (newSumm.getResultTimeBegin() != null) {
                summ.setResultTimeBegin(newSumm.getResultTimeBegin());
            }
        } else {
            if (newSumm.getResultTimeBegin() != null) {
                if (newSumm.getResultTimeBegin().before(summ.getResultTimeBegin())) {
                    summ.setResultTimeBegin(newSumm.getResultTimeBegin());
                }
            }
        }

        // Result time end
        if (summ.getResultTimeEnd() == null) {
            if (newSumm.getResultTimeEnd() != null) {
                summ.setResultTimeEnd(newSumm.getResultTimeEnd());
            }
        } else {
            if (newSumm.getResultTimeEnd() != null) {
                if (newSumm.getResultTimeEnd().after(summ.getResultTimeEnd())) {
                    summ.setResultTimeEnd(newSumm.getResultTimeEnd());
                }
            }
        }
    }

    /**
     * Update Datastream temporal and spatial summaries when creating a new Observation
     * @param d
     * @param o
     * @param foi
     * @param dsRepo
     */
    public static void updateDatastreamSummaries(Datastream d, Observation o, FeatureOfInterest foi,
                                                 DatastreamRepository dsRepo) {
        com.cgi.kinota.persistence.cassandra.domain.Datastream cassDatastream = null;
        try {
            cassDatastream = (com.cgi.kinota.persistence.cassandra.domain.Datastream) d;
        } catch (ClassCastException e) {
            String mesg = "Datastream is not a CassandraDatastream";
            throw new ApplicationException(ApplicationErrorCode.E_InternalError,
                    mesg);
        }
        boolean dirty = false;
        // Update phenomenonTimeBegin
        Date dsPhenomTimeBegin = cassDatastream.getPhenomenonTimeBegin();
        Date obsPhenomTimeBegin = o.getPhenomenonTime();
        if (dsPhenomTimeBegin == null) {
            cassDatastream.setPhenomenonTimeBegin(obsPhenomTimeBegin);
            dirty = true;
        } else {
            if (dsPhenomTimeBegin.after(obsPhenomTimeBegin)) {
                cassDatastream.setPhenomenonTimeBegin(obsPhenomTimeBegin);
                dirty = true;
            }
        }
        // Update phenomenonTimeEnd
        Date dsPhenomTimeEnd = cassDatastream.getPhenomenonTimeEnd();
        Date obsPhenomTimeEnd = o.getPhenomenonTimeEnd();
        if (dsPhenomTimeEnd == null) {
            if (obsPhenomTimeEnd == null) {
                // This observation does not have a phenomenonTimeEnd, just using its phenomenonTime
                cassDatastream.setPhenomenonTimeEnd(obsPhenomTimeBegin);
                dirty = true;
            } else {
                cassDatastream.setPhenomenonTimeEnd(obsPhenomTimeEnd);
                dirty = true;
            }
        } else {
            Date obsPhenomTimeBasis = o.getPhenomenonTimeEnd();
            if (obsPhenomTimeEnd == null) {
                // This observation does not have a phenomenonTimeEnd, just using its phenomenonTime
                obsPhenomTimeBasis = o.getPhenomenonTime();
            }
            if (dsPhenomTimeEnd.before(obsPhenomTimeBasis)) {
                cassDatastream.setPhenomenonTimeEnd(obsPhenomTimeBasis);
                dirty = true;
            }
        }
        // Update Datastream's resultTimeBegin and resultTimeEnd
        Date obsResultTime = o.getResultTime();
        if (obsResultTime != null) {
            // Result time begin
            Date dsResultTimeBegin = cassDatastream.getResultTimeBegin();
            if (dsResultTimeBegin == null) {
                cassDatastream.setResultTimeBegin(obsResultTime);
                dirty = true;
            } else if (dsResultTimeBegin.after(obsResultTime)){
                cassDatastream.setResultTimeBegin(obsResultTime);
                dirty = true;
            }
            // Result time end
            Date dsResultTimeEnd = cassDatastream.getResultTimeEnd();
            if (dsResultTimeEnd == null) {
                cassDatastream.setResultTimeEnd(obsResultTime);
                dirty = true;
            } else if (dsResultTimeEnd.before(obsResultTime)) {
                cassDatastream.setResultTimeEnd(obsResultTime);
                dirty = true;
            }
        }

        // Update Datastream's observedArea
        // Get geometry from Observation's FeatureOfInterest
        //   assume FeatureOfInterest is a point for now.
        Point p = null;
        if (foi.getLocation() instanceof Feature) {
            Feature f = (Feature) foi.getLocation();
            if (f.getGeometry() instanceof Point) {
                p = (Point) f.getGeometry();
            }
        }

        if (p == null) {
            logger.error("Could not determine point of FeatureOfInterest: " + foi.toString());
        } else {
            Polygon observedArea = cassDatastream.getObservedArea();
            if (observedArea == null) {
                cassDatastream.setObservedArea(createBoundingBoxPolygonFromPoint(p, null));
                dirty = true;
            } else {
                cassDatastream.setObservedArea(updateBoundingBox(observedArea, p));
                dirty = true;
            }
        }

        if (dirty) {
            dsRepo.save(cassDatastream);
        }
    }

    /**
     * (re)Create Datastream summaries by looking up temporal ranges, and rebuilding the observedArea
     * based on all available FeaturesOfInterest.
     * @param datastreamId
     */
    public void recreateDatastreamSummaries(UUID datastreamId) {
        Datastream d = dsSvc.findOne(datastreamId);
        com.cgi.kinota.persistence.cassandra.domain.Datastream cassDatastream = null;
        try {
            cassDatastream = (com.cgi.kinota.persistence.cassandra.domain.Datastream) d;
        } catch (ClassCastException e) {
            String mesg = "Datastream is not a CassandraDatastream";
            throw new ApplicationException(ApplicationErrorCode.E_InternalError,
                    mesg);
        }
        DatastreamTemporalSummary summ = new DatastreamTemporalSummary();
        ArrayList<Point> points = new ArrayList<>();
        Pair<Set<UUID>, List<Integer>> partitionKeys = this.fetchRelatedObservationParitionKeysForDatastream(d.getId());

        for (UUID featureOfInterestId : partitionKeys.getLeft()) {
            FeatureOfInterest foi = foiSvc.findOne(featureOfInterestId);
            Feature f = (Feature) foi.getLocation();
            if (f.getGeometry() instanceof Point) {
                points.add((Point) f.getGeometry());
            }
            DatastreamTemporalSummary tmpSumm = relatedObservationNativeRepo.fetchMinMaxPhenomenonTime(featureOfInterestId,
                    cassDatastream.getId(), partitionKeys.getRight());
            reduceDatastreamTemporalSummary(summ, tmpSumm);
        }
        // Update temporal summaries
        cassDatastream.setPhenomenonTimeBegin(summ.getPhenomenonTimeBegin());
        cassDatastream.setPhenomenonTimeEnd(summ.getPhenomenonTimeEnd());
        cassDatastream.setResultTimeBegin(summ.getResultTimeBegin());
        cassDatastream.setResultTimeEnd(summ.getResultTimeEnd());
        // Update observed area
        cassDatastream.setObservedArea(createBoundingBoxPolygonFromPoints(points));

        dsRepo.save(cassDatastream);
    }

    public static void updateDatastreamTemporalSummaries(Datastream d, DatastreamTemporalSummary summ,
                                                         DatastreamRepository dsRepo) {
        com.cgi.kinota.persistence.cassandra.domain.Datastream cassDatastream = null;
        try {
            cassDatastream = (com.cgi.kinota.persistence.cassandra.domain.Datastream) d;
        } catch (ClassCastException e) {
            String mesg = "Datastream is not a CassandraDatastream";
            throw new ApplicationException(ApplicationErrorCode.E_InternalError,
                    mesg);
        }
        cassDatastream.setPhenomenonTimeBegin(summ.getPhenomenonTimeBegin());
        cassDatastream.setPhenomenonTimeEnd(summ.getPhenomenonTimeEnd());
        cassDatastream.setResultTimeBegin(summ.getResultTimeBegin());
        cassDatastream.setResultTimeEnd(summ.getResultTimeEnd());
        dsRepo.save(cassDatastream);
    }

    public void deleteDatastream(UUID datastreamId) throws ApplicationException {
        Datastream d = dsSvc.findOne(datastreamId);
        // TODO: Make the below steps atomic using a custom BATCH query
        // 1. Disassociate Datastream with Thing
        disassociateThingWithDatastream(datastreamId, d.getThingId());
        // 2. Disassociate Datastream with Sensor
        disassociateSensorWithDatastream(datastreamId, d.getSensorId());
        // 3. Disassociate Datastream with ObservedProperty
        disassociateObservedPropertyWithDatastream(datastreamId, d.getObservedPropertyId());
        // 4. Delete Datastream first to minimize chances of new Observations being created
        //   after we delete what are currently there.
        dsRepo.delete(datastreamId);
        // 5. Delete Observations in Datastream
        deleteObservationsForDatastream(datastreamId, false);
    }

    public void deleteObservation(UUID observationId) throws ApplicationException {
        Observation o = obsSvc.findOne(observationId);
        UUID foiId = o.getFeatureOfInterestId();
        UUID dsId = o.getDatastreamId();
        Integer phenoTimeYear = o.getPhenomenonTimeYear();
        // 1. Delete Observation and its RelatedObservation
        relatedObservationRepo.deleteObservation(observationId,
                foiId,
                dsId,
                phenoTimeYear,
                o.getPhenomenonTime());
        // 2. Update Observation relationship association tables (if this was the last
        //      observation in the partition
        Long count = relatedObservationNativeRepo.fetchRelatedObservationCount(foiId,
                dsId, phenoTimeYear);
        if (count == 0) {
            dsObsFoiYearRepo.deleteFeatureOfInterestYearForDatastream(dsId,
                    foiId, phenoTimeYear);
            foiObsDsYearRepo.deleteDatastreamYearForFeaturesOfInterest(foiId,
                    dsId, phenoTimeYear);
        }
        // 3. Update Datastream summaries
        recreateDatastreamSummaries(dsId);
    }

    public void deleteFeatureOfInterest(UUID featureOfInterestId) throws ApplicationException {
        FeatureOfInterest f = foiSvc.findOne(featureOfInterestId);
        // TODO: Make the below steps atomic using a custom BATCH query
        // Delete the FeatureOfInterest first to minimize chances of new Observations being created
        //   after we delete what are currently there.
        foiRepo.delete(featureOfInterestId);
        deleteObservationsForFeatureOfInterest(featureOfInterestId, true);
    }

    public void deleteObservedProperty(UUID observedPropertyId) throws ApplicationException {
        ObservedProperty op = obsPropertySvc.findOne(observedPropertyId);
        List<ObservedPropertyDatastream> opDsList = observedPropertyDatastreamRepo.findAll(observedPropertyId);
        // TODO: Make the below steps atomic using a custom BATCH query
        // 1. Delete Datastreams
        opDsList.stream().forEach(opDs -> deleteDatastream(opDs.getDatastreamId()));
        // 2. Disassociate ObservedProperty from Datastream
        observedPropertyDatastreamRepo.delete(observedPropertyId);
        // 3. Delete ObservedProperty
        obsPropertyRepo.delete(observedPropertyId);
    }

    public void deleteSensor(UUID sensorId) throws ApplicationException {
        Sensor s = sensorSvc.findOne(sensorId);
        List<SensorDatastream> senDsList = sensorDatastreamRepo.findAll(sensorId);
        // TODO: Make the below steps atomic using a custom BATCH query
        // 1. Delete Datastreams
        senDsList.stream().forEach(senDs -> deleteDatastream(senDs.getDatastreamId()));
        // 2. Disassociate Sensor from Datastream
        sensorDatastreamRepo.delete(sensorId);
        // 3. Delete Sensor
        sensorRepo.delete(sensorId);
    }

    public void deleteThing(UUID thingId) throws ApplicationException {
        Thing t = thingSvc.findOne(thingId);
        // TODO: Make the below steps atomic using a custom BATCH query
        // 1. Disassociate Thing with Location
        disassociateLocationsFromThing(thingId);
        // 2. Disassociate Thing with HistoricalLocation
        disassociateHistoricalLocationsFromThing(thingId);
        // 3. Delete Datastreams
        deleteDatastreamsForThing(thingId);
        // 4. Disassociate Thing from Datastream
        thingDatastreamRepo.delete(thingId);
        // 5. Delete Thing
        thingRepo.delete(thingId);
    }

    public void deleteHistoricalLocation(UUID historicalLocationId) throws ApplicationException {
        // 1. Disassociate HistoricalLocation with Thing
        disassociateThingFromHistoricalLocation(historicalLocationId);
        // 2. Disassociate HistoricalLocation with Location
        disassociateHistoricalLocationFromLocation(historicalLocationId);
        // 3. Delete HistoricalLocation
        histLocRepo.delete(historicalLocationId);
    }

    public void deleteLocation(UUID locationId) throws ApplicationException {
        // 1. Disassociate Location with Things
        disassociateThingsFromLocation(locationId);
        // 2. Delete HistoricalLocations associated with Location
        locationHistoricalLocationRepo.findAll(locationId).stream()
                .forEach(lhl -> deleteHistoricalLocation(lhl.getHistoricalLocationId()));
        // 3. Delete Location
        locRepo.delete(locationId);
    }
}
