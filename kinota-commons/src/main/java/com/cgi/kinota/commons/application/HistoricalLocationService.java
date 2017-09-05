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

import com.cgi.kinota.commons.Utility;
import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;
import com.cgi.kinota.commons.domain.util.Serialization;
import com.cgi.kinota.commons.domain.HistoricalLocation;
import com.cgi.kinota.commons.domain.Location;
import com.cgi.kinota.commons.domain.Thing;
import com.cgi.kinota.commons.odata.ODataQuery;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import javax.json.JsonObject;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by bmiles on 2/3/17.
 */
public interface HistoricalLocationService extends QueryableService<HistoricalLocation> {

    Pair<Long, List<HistoricalLocation>> findAll(ODataQuery q) throws ApplicationException;

    HistoricalLocation overwrite(HistoricalLocation oldLocation, HistoricalLocation newLocation) throws ApplicationException;

    HistoricalLocation update(HistoricalLocation l, UUID thingUUID, UUID locationUUID) throws ApplicationException;

    default HistoricalLocation update(HistoricalLocation l, JsonObject json) throws ApplicationException {
        boolean dirty = false;
        // Read updated data from JSON object
        try {
            DateTimeFormatter f = Utility.getISO8601Formatter();
            String timeStr = json.getString("time");
            if (timeStr != null) {
                try {
                    DateTime dt = f.parseDateTime(timeStr);
                    Date d = dt.toDate();
                    l.setTime(d);
                } catch (IllegalArgumentException e) {
                    String mesg = "Unable parse time string '" + timeStr + "'.";
                    throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                            mesg);
                }
                dirty = true;
            }
        } catch (NullPointerException npe) {}

        // Parse related entities
        UUID thingUUID = Serialization.getRelatedEntityId(json, Thing.NAME);
        UUID locationUUID = Serialization.getRelatedEntityId(json, Location.NAME);
        if (thingUUID != null || locationUUID != null) {
            dirty = true;
        }

        if (dirty) {
            l = update(l, thingUUID, locationUUID);
        }
        return l;
    }

    HistoricalLocation save(HistoricalLocation location) throws ApplicationException;

    public HistoricalLocation create(UUID thingUUID,
                                     UUID locationUUID,
                                     Date time) throws ApplicationException;

    default HistoricalLocation create(JsonObject json) throws ApplicationException {
        String timeStr = null;
        Date time = null;
        try {
            timeStr = json.getString("time");
            DateTimeFormatter f = Utility.getISO8601Formatter();
            DateTime dt = f.parseDateTime(timeStr);
            time = dt.toDate();
        } catch (NullPointerException npe) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Required attribute 'time' not found in HistoricalLocation.");
        } catch (IllegalArgumentException e) {
            throw new ApplicationException(ApplicationErrorCode.E_Invalid,
                    "Unable to parse attribute 'time' with value '" + timeStr + "' in HistoricalLocation.");
        }

        // Parse related entities
        UUID thingUUID = Serialization.getRelatedEntityId(json, Thing.NAME);
        UUID locationUUID = Serialization.getRelatedEntityId(json, Location.NAME);

        return create(thingUUID, locationUUID, time);
    }
}
