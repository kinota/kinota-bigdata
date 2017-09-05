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

package com.cgi.kinota.persistence.cassandra.domain;

import org.springframework.cassandra.core.PrimaryKeyType;
import org.springframework.data.cassandra.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.mapping.Table;

import javax.activation.MimeType;
import java.net.URI;
import java.util.UUID;

import static com.cgi.kinota.commons.Constants.TABLE_SENSOR;

/**
 * Created by bmiles on 2/9/17.
 */
@Table(TABLE_SENSOR)
public class Sensor extends com.cgi.kinota.commons.domain.Sensor {

    public Sensor() {}

    public Sensor(UUID id, String name, String description,
                  MimeType encodingType, URI metadata) {
        super(id, name, description, encodingType, metadata);
    }

    @Override
    @PrimaryKeyColumn(name = "id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    public UUID getId() { return id; }
}
