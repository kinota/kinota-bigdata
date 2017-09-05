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

package com.cgi.kinota.rest.cassandra.infrastructure.security;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by dfladung on 3/28/17.
 */
public class ApiPatternMatcher implements RequestMatcher {

    RequestMatcher httpMethodMatcher;
    RequestMatcher pathMatcher;

    public ApiPatternMatcher(String path) {
        pathMatcher = new AntPathRequestMatcher(path);
        httpMethodMatcher = httpServletRequest -> BooleanUtils.isFalse(HttpMethod.GET.matches(httpServletRequest.getMethod()));
    }

    @Override
    public boolean matches(HttpServletRequest httpServletRequest) {
        return pathMatcher.matches(httpServletRequest) && httpMethodMatcher.matches(httpServletRequest);
    }
}
