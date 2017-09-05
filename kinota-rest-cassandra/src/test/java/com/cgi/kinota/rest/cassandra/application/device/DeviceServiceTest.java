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

package com.cgi.kinota.rest.cassandra.application.device;

import com.cgi.kinota.rest.cassandra.Application;
import com.cgi.kinota.rest.cassandra.CassandraRestTestBase;
import com.cgi.kinota.rest.cassandra.domain.device.Device;
import com.cgi.kinota.persistence.cassandra.config.SpringDataCassandraConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * Created by dfladung on 3/27/17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {Application.class, SpringDataCassandraConfig.class})
public class DeviceServiceTest extends CassandraRestTestBase {

    private static final Logger logger = LoggerFactory.getLogger(DeviceServiceTest.class);

    @Autowired
    DeviceService deviceService;

    @Test
    public void retrieveDevice() throws Exception {
        try {
            Device device = deviceService.retrieveDevice("2694b9e1-ce59-4fd5-b95e-aa1c780e8158");
            assertNotNull(device);
            assertEquals(device.getKey(), "ecacdce7-7374-408b-997b-5877bf9e37c3");
        } catch (Exception e){
            logger.error(e.getMessage(),e);
            fail(e.getMessage());
        }
    }

}