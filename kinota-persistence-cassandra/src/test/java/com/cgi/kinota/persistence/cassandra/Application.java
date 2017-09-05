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
import com.cgi.kinota.persistence.cassandra.config.CassandraConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.TimeZone;

/**
 * Created by bmiles on 8/7/17.
 */
@SpringBootApplication
public class Application extends SpringBootServletInitializer  {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Inject
    protected PersistenceConfiguration persistenceConfig;

    @PostConstruct
    void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        logger.info("Main...");
        new Application()
                .configure(new SpringApplicationBuilder(Application.class))
                .run(args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        logger.info("Configuring...");
        // Configure database
        // TODO: Figure out how to inject this @Autowired doesn't seem to work here
        persistenceConfig = new CassandraConfiguration();
        persistenceConfig.configure();
        persistenceConfig.createTablespace();
        persistenceConfig.useTablepace();
        persistenceConfig.createSchema();
        persistenceConfig.disconnect();

        return application.sources(Application.class);
    }

}
