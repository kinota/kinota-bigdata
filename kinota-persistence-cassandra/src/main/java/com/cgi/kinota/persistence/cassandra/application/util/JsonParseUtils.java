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

package com.cgi.kinota.persistence.cassandra.application.util;

import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;

import org.springframework.data.cassandra.repository.CassandraRepository;

import javax.json.JsonObject;
import java.util.UUID;

import static com.cgi.kinota.commons.Constants.ANNO_IOT_ID;
import static org.springframework.data.cassandra.repository.support.BasicMapId.id;

/**
 * Created by bmiles on 2/7/17.
 */
public class JsonParseUtils {

    /**
     * Return UUID of entity referenced by ANNO_IOT_ID element.  Post condition: Entity exists
     * @param j JsonObject representing JSON serialization of referring entity
     * @param entityName Name of referee
     * @param repo CassandraRepository from which to look up the referree
     * @param <T> The type of entity
     * @return UUID of the referee
     * @throws ApplicationException if the referee could not be found
     */
    public static <T> UUID getRelatedEntityId(JsonObject j,
                                              String entityName,
                                              CassandraRepository<T> repo) throws ApplicationException {
        UUID uuid = null;
        try {
            JsonObject l = j.getJsonObject(entityName);
            if (l != null) {
                String id = l.getString(ANNO_IOT_ID);
                uuid = UUID.fromString(id);
                // Make sure entity exists
                T t = repo.findOne(id().with("id", uuid));
                if (t == null) {
                    throw new ApplicationException(ApplicationErrorCode.E_NotFound,
                            "Unable to find " + entityName + " with ID '" + id + "' when creating object '" + j.toString() + "'.");
                }
            }
        } catch (NullPointerException npe) {}

        return uuid;
    }

    public static <T> void assertRelatedEntityExists(UUID uuid,
                                                     String entityName,
                                                     CassandraRepository<T> repo) {
        // Make sure entity exists
        T t = repo.findOne(id().with("id", uuid));
        if (t == null) {
            throw new ApplicationException(ApplicationErrorCode.E_NotFound,
                    "Unable to find " + entityName + " with ID '" + uuid.toString() + "'.");
        }
    }

    /**
     * Return the entity referenced by ANNO_IOT_ID element.
     * @param j JsonObject representing JSON serialization of referring entity
     * @param entityName Name of referee
     * @param repo CassandraRepository from which to look up the referree
     * @param <T> The type of entity
     * @return T The related entity, or null if the entity does not exist.
     * @throws ApplicationException if the referee could not be found
     */
    public static <T> T getRelatedEntity(JsonObject j,
                                         String entityName,
                                         CassandraRepository<T> repo) throws ApplicationException {
        T entity = null;
        try {
            JsonObject l = j.getJsonObject(entityName);
            if (l != null) {
                String id = l.getString(ANNO_IOT_ID);
                UUID uuid = UUID.fromString(id);
                // Make sure entity exists
                entity = repo.findOne(id().with("id", uuid));
                if (entity == null) {
                    throw new ApplicationException(ApplicationErrorCode.E_NotFound,
                            "Unable to find " + entityName + " with ID '" + id + "' when creating object '" + j.toString() + "'.");
                }
            }
        } catch (NullPointerException npe) {}

        return entity;
    }

    /**
     * Return UUID of entity referenced by UUID.  Post condition: Entity exists
     * @param id String representing UUID of referenced entity
     * @param entityName Name of referee
     * @param repo CassandraRepository from which to look up the referree
     * @param <T> The type of entity
     * @return UUID of the referee
     * @throws ApplicationException if the referee could not be found
     */
    public static <T> UUID getReferencedEntityId(String id,
                                                 String entityName,
                                                 CassandraRepository<T> repo) throws ApplicationException {
        UUID uuid = null;
        try {
            uuid = UUID.fromString(id);
            // Make sure entity exists
            T t = repo.findOne(id().with("id", uuid));
            if (t == null) {
                throw new ApplicationException(ApplicationErrorCode.E_NotFound,
                        "Unable to find referenced " + entityName + " with ID '" + id + "'.");
            }
        } catch (NullPointerException npe) {}

        return uuid;
    }
}
