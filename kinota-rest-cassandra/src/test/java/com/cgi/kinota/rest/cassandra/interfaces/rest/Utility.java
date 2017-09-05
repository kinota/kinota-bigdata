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

package com.cgi.kinota.rest.cassandra.interfaces.rest;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bmiles on 1/12/17.
 */
public class Utility {
    public static Pair<Boolean, String> doesLocationPathMatch(ResponseEntity<?> response,
                                                       Pattern urlPath) {
        Boolean matches = false;
        String locationUrl = null;
        List<String> locations = response.getHeaders().get("Location");
        if (locations != null || locations.size() > 0) {
            locationUrl = locations.get(0);
            URI u = URI.create(locationUrl);
            String locationPath = u.getPath();
            Matcher m = urlPath.matcher(locationPath);
            matches = m.matches();
        }
        return new ImmutablePair<Boolean, String>(matches, locationUrl);
    }

    public static Pair<Boolean, String> extractEntityUuidFromLocationUrlPath(ResponseEntity<?> response,
                                                                             Pattern urlPath) {
        Boolean matches = false;
        String locationUuid = null;
        List<String> locations = response.getHeaders().get("Location");
        if (locations != null || locations.size() > 0) {
            String locationUrl = locations.get(0);
            URI u = URI.create(locationUrl);
            String locationPath = u.getPath();
            Matcher m = urlPath.matcher(locationPath);
            matches = m.matches();
            if (matches) {
                // Strip leading and trailing parens
                locationUuid = m.group(1);
                locationUuid = locationUuid.substring(1, locationUuid.length() - 1);
            }
        }
        return new ImmutablePair<Boolean, String>(matches, locationUuid);
    }

    public static String extractUuidForEntityUrl(String url) {
        String locationUuid = null;

        int idx = url.lastIndexOf('(');
        locationUuid = url.substring(idx + 1, url.length() - 1);

        return locationUuid;
    }
}
