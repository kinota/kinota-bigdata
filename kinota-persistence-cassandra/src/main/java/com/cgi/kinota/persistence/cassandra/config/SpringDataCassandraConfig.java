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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cassandra.core.keyspace.CreateKeyspaceSpecification;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.CassandraClusterFactoryBean;
import org.springframework.data.cassandra.config.CassandraSessionFactoryBean;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.config.java.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.convert.CassandraConverter;
import org.springframework.data.cassandra.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.mapping.BasicCassandraMappingContext;
import org.springframework.data.cassandra.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.mapping.SimpleUserTypeResolver;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.cgi.kinota.commons.Constants.MATERIALIZED_VIEW_FEATURE_OF_INTEREST_LOCATION;

/**
 * Created by bmiles on 12/28/16.
 */
@Configuration
@EnableCassandraRepositories(basePackages = "com.cgi.kinota.persistence.cassandra.infrastructure.persistence")
public class SpringDataCassandraConfig extends AbstractCassandraConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SpringDataCassandraConfig.class);

    @Override
    protected String getContactPoints() { return System.getenv().getOrDefault("CASS_CONTACT_POINTS", "127.0.0.1"); }

    @Override
    protected int getPort() { return Integer.valueOf(System.getenv().getOrDefault("CASS_PORT", "9042")); }

    @Override
    protected String getKeyspaceName() {
        return System.getenv().getOrDefault("CASS_KEYSPACE", "cgist");
    }

    @Bean
    @Override
    public CassandraClusterFactoryBean cluster() {
        String contactPoints = getContactPoints();
        logger.info("CASS_CONTACT_POINTS = " + getContactPoints());
        int port = getPort();
        logger.info("CASS_PORT = " + port);
        String keyspaceName = getKeyspaceName();
        logger.info("CASS_KEYSPACE = " + keyspaceName);
        int replicationFactor = Integer.valueOf(System.getenv().getOrDefault("CASS_REPLICATION_FACTOR", "1"));
        logger.info("CASS_REPLICATION_FACTOR = " + replicationFactor);

        CassandraClusterFactoryBean cluster =
                new CassandraClusterFactoryBean();
        cluster.setContactPoints(contactPoints);
        cluster.setPort(port);
        CreateKeyspaceSpecification keyspace = new CreateKeyspaceSpecification(keyspaceName)
                .ifNotExists()
                .withSimpleReplication(replicationFactor);
        cluster.setKeyspaceCreations(Arrays.asList(keyspace));
        return cluster;
    }

    @Bean
    public CassandraMappingContext mappingContext() {
        BasicCassandraMappingContext mappingContext =  new BasicCassandraMappingContext();
        mappingContext.setUserTypeResolver(new SimpleUserTypeResolver(cluster().getObject(), getKeyspaceName()));

        return mappingContext;
    }

    @Bean
    public CassandraConverter converter() {
        return new MappingCassandraConverter(mappingContext());
    }

    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.NONE;
    }

    @Override
    public String[] getEntityBasePackages() {
        return new String[] { "com.cgi.rap.cgist.samplenetwork.domain" };
    }

    @Override
    protected List<String> getStartupScripts() {
        List<String> scripts = new ArrayList<>();
        // Create materialized views
        scripts.add("CREATE MATERIALIZED VIEW IF NOT EXISTS " +
                MATERIALIZED_VIEW_FEATURE_OF_INTEREST_LOCATION + " " +
                "AS SELECT * FROM featureofinterest WHERE location IS NOT NULL PRIMARY KEY (location, id);");

        return scripts;
    }

    @Bean
    public CassandraSessionFactoryBean session() throws ClassNotFoundException {

        CassandraSessionFactoryBean session = new CassandraSessionFactoryBean();
        session.setCluster(cluster().getObject());
        session.setKeyspaceName(getKeyspaceName());
        session.setConverter(converter());
        session.setSchemaAction(getSchemaAction());

        return session;
    }
}
