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

package com.cgi.kinota.commons;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Created by bmiles on 2/3/17.
 */
public class Utility {
    public static DateTimeFormatter getISO8601Formatter() {
        return ISODateTimeFormat.dateTime();
    }

    public static String getISO8601String(Date date) {
        DateTimeFormatter f = getISO8601Formatter();
        return f.print(new DateTime(date, DateTimeZone.UTC));
    }

    public static Integer getYearForDate(Date date) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTime(date);
        return Integer.valueOf(c.get(Calendar.YEAR));
    }

    public static Date getCurrentTimeUTC() {
        return new DateTime(DateTimeZone.UTC).toDate();
    }
}
