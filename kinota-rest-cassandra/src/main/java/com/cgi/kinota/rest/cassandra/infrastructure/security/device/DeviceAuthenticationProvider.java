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

package com.cgi.kinota.rest.cassandra.infrastructure.security.device;

import com.cgi.kinota.rest.cassandra.application.device.DeviceService;
import com.cgi.kinota.rest.cassandra.domain.device.Device;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;

/**
 * Created by dfladung on 3/27/17.
 */
@Component
public class DeviceAuthenticationProvider implements AuthenticationProvider {

    public static final List<SimpleGrantedAuthority> roles =
            Collections.singletonList(new SimpleGrantedAuthority("CGIST_DEVICE"));
    private final DeviceService deviceService;

    @Autowired
    public DeviceAuthenticationProvider(final DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.notNull(authentication, "No authentication data provided");
        String id = (String) authentication.getPrincipal();
        String key = (String) authentication.getCredentials();

        Device device = deviceService.retrieveDevice(id);
        if (device == null) {
            throw new UsernameNotFoundException("Device not found: " + id);
        }
        if (!StringUtils.equals(key, device.getKey())) {
            throw new BadCredentialsException("Authentication Failed. Device ID or Key not valid.");
        }
        User user = new User(id, key, roles);
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
