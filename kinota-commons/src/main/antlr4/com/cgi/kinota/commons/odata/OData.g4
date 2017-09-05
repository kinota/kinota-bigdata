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
/**
 *
 * Placeholder grammar for parsing OData 4.0 query subset supported by
 * OGC SensorThings API, Part I: Sensing.

 * References:
 * - http://docs.opengeospatial.org/is/15-078r6/15-078r6.html#19
 * - http://docs.oasis-open.org/odata/odata/v4.0/os/abnf/odata-abnf-construction-rules.txt
 */
grammar OData;

query : (kvp) ('&' kvp)* ;
kvp    : KEY '=' VALUE ;
KEY    : '$top'
       | '$skip'
       | '$resultFormat' ;
VALUE  : [a-zA-Z0-9]+;
