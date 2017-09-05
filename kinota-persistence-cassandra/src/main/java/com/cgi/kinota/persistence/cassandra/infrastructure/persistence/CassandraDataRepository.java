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

import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.domain.Entity;

import com.cgi.kinota.commons.persistence.DataRepository;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.MapId;
import org.springframework.data.repository.NoRepositoryBean;

import static org.springframework.data.cassandra.repository.support.BasicMapId.id;

import java.util.UUID;

/**
 * Created by bmiles on 5/24/17.
 */
@NoRepositoryBean
public interface CassandraDataRepository<U extends Entity> extends DataRepository<U>, CassandraRepository<U> {
    default void delete(UUID uuid) throws ApplicationException {
        MapId id = id().with("id", uuid);
        this.delete(id);
    }
    default Iterable<U> fetchAll() {
        return this.findAll();
    }
}
