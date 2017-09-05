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

package com.cgi.kinota.persistence.cassandra;

import com.cgi.kinota.commons.config.PersistenceConfiguration;

import com.cgi.kinota.persistence.cassandra.domain.Datastream;
import com.cgi.kinota.persistence.cassandra.domain.DatastreamObservationFeatureOfInterestYear;
import com.cgi.kinota.persistence.cassandra.domain.FeatureOfInterest;
import com.cgi.kinota.persistence.cassandra.domain.FeatureOfInterestObservationDatastreamYear;
import com.cgi.kinota.persistence.cassandra.domain.HistoricalLocation;
import com.cgi.kinota.persistence.cassandra.domain.HistoricalLocationLocation;
import com.cgi.kinota.persistence.cassandra.domain.HistoricalLocationThing;
import com.cgi.kinota.persistence.cassandra.domain.Location;
import com.cgi.kinota.persistence.cassandra.domain.LocationHistoricalLocation;
import com.cgi.kinota.persistence.cassandra.domain.LocationThing;
import com.cgi.kinota.persistence.cassandra.domain.Observation;
import com.cgi.kinota.persistence.cassandra.domain.ObservedProperty;
import com.cgi.kinota.persistence.cassandra.domain.ObservedPropertyDatastream;
import com.cgi.kinota.persistence.cassandra.domain.RelatedObservation;
import com.cgi.kinota.persistence.cassandra.domain.Sensor;
import com.cgi.kinota.persistence.cassandra.domain.SensorDatastream;
import com.cgi.kinota.persistence.cassandra.domain.Thing;
import com.cgi.kinota.persistence.cassandra.domain.ThingDatastream;
import com.cgi.kinota.persistence.cassandra.domain.ThingHistoricalLocation;
import com.cgi.kinota.persistence.cassandra.domain.ThingLocation;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cassandra.core.cql.CqlIdentifier;
import org.springframework.context.annotation.Bean;
import org.springframework.data.cassandra.convert.CassandraConverter;
import org.springframework.data.cassandra.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.core.CassandraAdminOperations;
import org.springframework.data.cassandra.core.CassandraAdminTemplate;
import org.springframework.data.cassandra.mapping.BasicCassandraMappingContext;
import org.springframework.data.cassandra.mapping.CassandraMappingContext;

import java.util.Collections;
import java.util.TimeZone;

import static com.cgi.kinota.commons.Constants.*;
import static com.cgi.kinota.commons.Constants.MATERIALIZED_VIEW_FEATURE_OF_INTEREST_LOCATION;
import static com.cgi.kinota.commons.Constants.TABLE_FEATURE_OF_INTEREST_OBSERVATION_DS_YEAR;

public abstract class CassandraTestBase {

    private static final Logger logger = LoggerFactory.getLogger(CassandraTestBase.class);

    public static final String KEYSPACE_NAME = "cgist";
    public static final String KEYSPACE_CREATE = "CREATE KEYSPACE IF NOT EXISTS cgist WITH replication = {'class':'SimpleStrategy', 'replication_factor':3};";
    public static final String KEYSPACE_USE = "USE " + KEYSPACE_NAME;

    protected static CassandraAdminOperations admin;
    protected static Session session;

    @Autowired
    protected PersistenceConfiguration persistenceConfig;

    @Bean
    public CassandraMappingContext cassandraMapping() {
        return new BasicCassandraMappingContext();
    }

    @Bean
    public CassandraConverter cassandraConverter() {
        return new MappingCassandraConverter(cassandraMapping());
    }

    @Bean
    public CassandraAdminOperations cassandraTemplate() throws Exception {
        return new CassandraAdminTemplate(session,
                cassandraConverter());
    }

    @BeforeClass
    public static void startCassandraEmbedded() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        EmbeddedCassandraServerHelper.startEmbeddedCassandra();
        // User port 9142 so as to not connect to a "real" Cassandra node that might be running locally on port 9042.
        Cluster cluster = Cluster.builder()
                .addContactPoints("127.0.0.1").withPort(9142).build();
        session = cluster.connect();
        admin = new CassandraAdminTemplate(session, new MappingCassandraConverter());
        session.execute(KEYSPACE_CREATE);
        session.execute(KEYSPACE_USE);
        Thread.sleep(5000);
    }

    @AfterClass
    public static void stopCassandraEmbedded() {
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
    }

    @Before
    public void createTables() throws Exception {
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_THING), Thing.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_THING_DATASTREAM), ThingDatastream.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_SENSOR), Sensor.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_SENSOR_DATASTREAM), SensorDatastream.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_OBSERVED_PROPERTY), ObservedProperty.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_OBSERVED_PROPERTY_DATASTREAM), ObservedPropertyDatastream.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_LOCATION), Location.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_LOCATION_THING), LocationThing.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_THING_LOCATION), ThingLocation.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_HISTORICAL_LOCATION), HistoricalLocation.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_HISTORICAL_LOCATION_THING), HistoricalLocationThing.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_THING_HISTORICAL_LOCATION), ThingHistoricalLocation.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_HISTORICAL_LOCATION_LOCATION), HistoricalLocationLocation.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_LOCATION_HISTORICAL_LOCATION), LocationHistoricalLocation.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_DATASTREAM), Datastream.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_OBSERVATION), Observation.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_DATASTREAM_OBSERVATION_FOI_YEAR), DatastreamObservationFeatureOfInterestYear.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_FEATURE_OF_INTEREST), FeatureOfInterest.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_RELATED_OBSERVATION), RelatedObservation.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_FEATURE_OF_INTEREST_OBSERVATION_DS_YEAR), FeatureOfInterestObservationDatastreamYear.class, Collections.emptyMap());

        session.execute("CREATE MATERIALIZED VIEW IF NOT EXISTS " +
                MATERIALIZED_VIEW_FEATURE_OF_INTEREST_LOCATION + " " +
                "AS SELECT * FROM featureofinterest WHERE location IS NOT NULL PRIMARY KEY (location, id);");
    }

    @After
    public void dropTables() {
        session.execute("DROP MATERIALIZED VIEW " +
                MATERIALIZED_VIEW_FEATURE_OF_INTEREST_LOCATION + ";");

        admin.dropTable(CqlIdentifier.cqlId(TABLE_THING));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_THING_DATASTREAM));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_SENSOR));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_SENSOR_DATASTREAM));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_OBSERVED_PROPERTY));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_OBSERVED_PROPERTY_DATASTREAM));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_LOCATION));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_LOCATION_THING));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_THING_LOCATION));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_HISTORICAL_LOCATION));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_HISTORICAL_LOCATION_THING));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_THING_HISTORICAL_LOCATION));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_HISTORICAL_LOCATION_LOCATION));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_LOCATION_HISTORICAL_LOCATION));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_DATASTREAM));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_OBSERVATION));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_DATASTREAM_OBSERVATION_FOI_YEAR));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_FEATURE_OF_INTEREST));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_RELATED_OBSERVATION));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_FEATURE_OF_INTEREST_OBSERVATION_DS_YEAR));
    }

}