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

import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.persistence.DataRepository;
import com.cgi.kinota.commons.domain.Entity;
import com.cgi.kinota.commons.application.paging.Paginator;
import com.cgi.kinota.commons.application.paging.PagingDescriptor;
import com.cgi.kinota.commons.odata.ODataQuery;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.cgi.kinota.commons.Constants.*;

/**
 * Created by bmiles on 5/24/17.
 */
public interface QueryableService<U extends Entity> {

    default <U extends Entity> U findOne(String uuid) throws ApplicationException {
        try {
            UUID id = UUID.fromString(uuid);
            return this.findOne(id);
        } catch (IllegalArgumentException e) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Identifier '" + uuid + "' is not a valid UUID.");
        }
    }

    <U extends Entity> U findOne(UUID id) throws ApplicationException;

    default void delete(String uuid) throws ApplicationException {
        try {
            UUID id = UUID.fromString(uuid);
            this.delete(id);
        } catch (IllegalArgumentException e) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Identifier '" + uuid + "' is not a valid UUID.");
        }
    }

    void delete(UUID uuid) throws ApplicationException;

    /**
     * Query the repository using OData query semantics
     * @param q
     * @param repo
     * @param <U>
     * @return
     */
    static <U extends Entity> Pair<Long, List<U>> queryAll(ODataQuery q,
                                                           DataRepository<U> repo) {
        // Handle paging
        PagingDescriptor pd = Paginator.extractPagingDescriptor(q);
        Integer top = pd.getTop();
        Integer skip = pd.getSkip();

        Iterable<U> i = repo.fetchAll();
        Stream<U> s = StreamSupport.stream(i.spliterator(), false);

        // Get the total number of entities
        long count = repo.count();
        List<U> entities = s.skip(skip).limit(top).collect(Collectors.toList());
        return new ImmutablePair<>(count, entities);
    }

    static <U extends Entity> void toJsonArray(Pair<Long, List<U>> entities, ODataQuery q,
                                               JsonGenerator g, String urlBase,
                                               Paginator p) throws ApplicationException {
        try {
            g.writeStartObject();

            Long numEntities = entities.getLeft();
            g.writeNumberField(ANNO_COLLECTION_COUNT, numEntities);
            g.writeArrayFieldStart(COLLECTION_ATTR);
            entities.getRight().forEach(e -> e.toJsonObject(g, urlBase));
            g.writeEndArray();

            p.paginate(numEntities, q, g, urlBase);

            g.writeEndObject();
        } catch (IOException e) {
            String mesg = "Unable to write entity array to JSON stream due to error: " +
                    e.getMessage();
            throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
        }
    }

    static <U extends Entity> void toJsonArrayRelated(Pair<Long, List<U>> entities, ODataQuery q,
                                                      JsonGenerator g,
                                                      String requestUrlBase, String entityUrlBase,
                                                      Paginator p) throws ApplicationException {
        try {
            g.writeStartObject();

            Long numEntities = entities.getLeft();
            g.writeArrayFieldStart(COLLECTION_ATTR);
            entities.getRight().forEach(e -> e.toJsonObject(g, entityUrlBase));
            g.writeEndArray();

            p.paginate(numEntities, q, g, requestUrlBase);

            g.writeEndObject();

        } catch (IOException e) {
            String mesg = "Unable to write related entity array to JSON stream due to error: " +
                    e.getMessage();
            throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
        }
    }

    static <U extends Entity> void toJsonArrayRelatedRef(Pair<Long, Iterable<UUID>> uuids, ODataQuery q,
                                                         JsonGenerator g,
                                                         String requestUrlBase, String entityUrlBase,
                                                         Paginator p) throws ApplicationException {
        try {
            g.writeStartObject();

            Long numEntities = uuids.getLeft();
            g.writeArrayFieldStart(COLLECTION_ATTR);
            uuids.getRight().forEach(u -> responseWriteStringFieldObject(g, ANNO_IOT_SELF_LINKS, Entity.toSelfLink(entityUrlBase, u.toString())));
            g.writeEndArray();

            p.paginate(numEntities, q, g, requestUrlBase);

            g.writeEndObject();

        } catch (IOException e) {
            String mesg = "Unable to write related entity array to JSON stream due to error: " +
                    e.getMessage();
            throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
        }
    }

    static <U extends Entity> void toJsonArrayRelatedRefSingleton(UUID uuid,
                                                                  JsonGenerator g,
                                                                  String entityUrlBase) throws ApplicationException {
        try {
            g.writeStartObject();

            Long numEntities = 0l;
            g.writeArrayFieldStart(COLLECTION_ATTR);
            if (uuid != null) {
                responseWriteStringFieldObject(g, ANNO_IOT_SELF_LINKS, Entity.toSelfLink(entityUrlBase, uuid.toString()));
                numEntities = 1l;
            }
            g.writeEndArray();

            g.writeEndObject();

        } catch (IOException e) {
            String mesg = "Unable to write related entity array to JSON stream due to error: " +
                    e.getMessage();
            throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
        }
    }

    static void responseWriteStringFieldObject(JsonGenerator g, String key, String value) throws ApplicationException {
        try {
            g.writeStartObject();
            g.writeStringField(key, value);
            g.writeEndObject();
        } catch (IOException e) {
            String mesg = "Unable to write string field (key,value): '" + key + "'," + value +
                    "' in object to JSON stream due to error: " +
                    e.getMessage();
            throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
        }
    }
}
