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

package com.cgi.kinota.commons.domain;

import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.domain.util.Serialization;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.odata.ODataQuery;

import com.fasterxml.jackson.core.JsonGenerator;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.UUID;

import static com.cgi.kinota.commons.Constants.*;

/**
 * Created by bmiles on 1/20/17.
 */
public interface Entity<T> {

    UUID getId();
    void setId(UUID id);
    String getEntityName();
    String getEntityNamePlural();

    <U extends T> void overwrite(U other);

    static String toSelfLink(String urlBase, String idStr) {
        // Generate absolute self link
        return urlBase + "(" + idStr + ")";
    }

    default String toRelLink() {
        return Serialization.generateRelLink(this.getEntityNamePlural(), this.getId().toString()) + "/";
    }

    default URI generateSelfLinkUrl(String urlBase) throws ApplicationException {
        String uriStr = toSelfLink(urlBase, this.getId().toString());
        try {
            return new URI(uriStr);
        } catch (URISyntaxException e) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Unable to generate selfLink URL from base URL " + urlBase);
        }
    }

    static String generateNextLinkUrl(String urlBase, long top, long skip) {
        String url = null;
        UriBuilder b = UriBuilder.fromUri(urlBase);
        b.queryParam(ODataQuery.TOP, top);
        b.queryParam(ODataQuery.SKIP, skip);
        try {
            url = URLDecoder.decode(b.build().toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {}
        return url;
    }

    default void generateJsonObjectBuilderWithSelfMetadata(JsonGenerator g, String urlBase) throws ApplicationException {
        // We will need the id as a String
        String idStr = this.getId().toString();
        String selfLink = toSelfLink(urlBase, idStr);

        try {
            // Self metadata
            g.writeStringField(ANNO_IOT_ID, idStr);
            g.writeStringField(ANNO_IOT_SELF_LINK, selfLink);
            // Related entities
            this.addRelatedEntityLinks(g);
        } catch (IOException e) {
            throw new ApplicationException(ApplicationErrorCode.E_IO,
                    e.getMessage());
        }
    }

    void addRelatedEntityLinks(JsonGenerator g) throws ApplicationException;

    void toJsonObject(JsonGenerator g, String urlBase) throws ApplicationException;

}
