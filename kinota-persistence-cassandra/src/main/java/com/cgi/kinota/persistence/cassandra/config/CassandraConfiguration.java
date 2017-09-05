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

package com.cgi.kinota.persistence.cassandra.config;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cassandra.core.cql.CqlIdentifier;
import org.springframework.data.cassandra.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.core.CassandraAdminOperations;
import org.springframework.data.cassandra.core.CassandraAdminTemplate;
import org.springframework.data.cassandra.mapping.BasicCassandraMappingContext;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

import static com.cgi.kinota.commons.Constants.*;


/**
 * Created by bmiles on 5/10/17.
 */
@Component
public class CassandraConfiguration implements PersistenceConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CassandraConfiguration.class);

    public static Cluster cluster = null;
    public static Session session = null;

    protected String contactPoints;
    protected Integer port;
    protected String keyspaceName;
    protected Integer replicationFactor;

    public void configure() {
        configure(Collections.EMPTY_MAP);
    }

    public void configure(Map<String, String> parameters) {
        if (contactPoints == null) {
            if (parameters.containsKey("CASS_CONTACT_POINTS")) {
               contactPoints = parameters.get("CASS_CONTACT_POINTS");
            } else {
                contactPoints = System.getenv().getOrDefault("CASS_CONTACT_POINTS", "127.0.0.1");
            }
        }
        logger.info("CASS_CONTACT_POINTS = " + contactPoints);

        if (port == null) {
            if (parameters.containsKey("CASS_PORT")) {
                port = Integer.valueOf(parameters.get("CASS_PORT"));
            } else {
                port = Integer.valueOf(System.getenv().getOrDefault("CASS_PORT", "9042"));
            }
        }
        logger.info("CASS_PORT = " + port);

        if (keyspaceName == null) {
            if (parameters.containsKey("CASS_KEYSPACE")) {
                keyspaceName = parameters.get("CASS_KEYSPACE");
            } else {
                keyspaceName = System.getenv().getOrDefault("CASS_KEYSPACE", "cgist");
            }
        }
        logger.info("CASS_KEYSPACE = " + keyspaceName);

        if (replicationFactor == null) {
            if (parameters.containsKey("CASS_REPLICATION_FACTOR")) {
                replicationFactor = Integer.valueOf(parameters.get("CASS_REPLICATION_FACTOR"));
            } else {
                replicationFactor = Integer.valueOf(System.getenv().getOrDefault("CASS_REPLICATION_FACTOR", "1"));
            }
        }
        logger.info("CASS_REPLICATION_FACTOR = " + replicationFactor);

        String contactPointsArray[] = contactPoints.split(",");
        cluster = Cluster.builder().addContactPoints(contactPointsArray).withPort(port).build();
        session = cluster.connect();
    }

    public void createTablespace() {
        // Create and use keyspace
        String createKeyspace = "CREATE KEYSPACE IF NOT EXISTS " + keyspaceName +
                " WITH replication = {'class':'SimpleStrategy', 'replication_factor':" + replicationFactor + "};";
        session.execute(createKeyspace);
    }

    public void useTablepace() {
        session.execute("USE " + keyspaceName);
    }

    public void createSchema() {
        CassandraAdminOperations admin = getCassandraAdminOperations();
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
                CqlIdentifier.cqlId(TABLE_DATASTREAM_OBSERVATION_FOI_YEAR), DatastreamObservationFeatureOfInterestYear.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_OBSERVATION), Observation.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_FEATURE_OF_INTEREST), FeatureOfInterest.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_RELATED_OBSERVATION), RelatedObservation.class, Collections.emptyMap());
        admin.createTable(true,
                CqlIdentifier.cqlId(TABLE_FEATURE_OF_INTEREST_OBSERVATION_DS_YEAR), FeatureOfInterestObservationDatastreamYear.class, Collections.emptyMap());

        // Materialized views
        session.execute("CREATE MATERIALIZED VIEW IF NOT EXISTS " +
                MATERIALIZED_VIEW_FEATURE_OF_INTEREST_LOCATION + " " +
                "AS SELECT * FROM featureofinterest WHERE location IS NOT NULL PRIMARY KEY (location, id);");
    }

    public void dropSchema() {
        CassandraAdminOperations admin = getCassandraAdminOperations();

        // Drop materialized views
        session.execute("DROP MATERIALIZED VIEW " +
                MATERIALIZED_VIEW_FEATURE_OF_INTEREST_LOCATION + ";");

        // Drop tables
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
        admin.dropTable(CqlIdentifier.cqlId(TABLE_DATASTREAM_OBSERVATION_FOI_YEAR));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_OBSERVATION));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_FEATURE_OF_INTEREST));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_RELATED_OBSERVATION));
        admin.dropTable(CqlIdentifier.cqlId(TABLE_FEATURE_OF_INTEREST_OBSERVATION_DS_YEAR));
    }

    public void disconnect() {
        session.close();
        cluster.close();
    }

    public CassandraAdminOperations getCassandraAdminOperations() {
        return new CassandraAdminTemplate(session,
                new MappingCassandraConverter(new BasicCassandraMappingContext()));
    }

    public String getContactPoints() {
        return contactPoints;
    }

    public void setContactPoints(String contactPoints) {
        this.contactPoints = contactPoints;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getKeyspaceName() {
        return keyspaceName;
    }

    public void setKeyspaceName(String keyspaceName) {
        this.keyspaceName = keyspaceName;
    }

    public int getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(int replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

}
