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

package com.cgi.kinota.commons.geo;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Test;

import static com.cgi.kinota.commons.geo.Utility.*;
import static org.junit.Assert.*;

/**
 * Created by bmiles on 5/2/17.
 */
public class UtilityTest {
    @Test
    public void testCreateInitialBoundingBox() {
        // Create initial bbox from a single point
        org.geojson.Point p = createGeoJsonPoint(-92.041213, 30.218805);
        org.geojson.Polygon bbox = createBoundingBoxPolygonFromPoint(p, null);
        assertNotNull(bbox);
        // Add a second point to the bbox
        org.geojson.Point p2 = createGeoJsonPoint(-92.046929, 30.227787);
        org.geojson.Polygon bboxNew = updateBoundingBox(bbox, p2);
        assertNotNull(bboxNew);
        assertNotEquals(bbox, bboxNew);

        // Test for topological consistency
        // Initial bbox contains first point, but not the second
        ReferencedEnvelope e = boundingBoxPolygonToEnvelope(bbox);
        assertTrue(e.covers(p.getCoordinates().getLongitude(),
                p.getCoordinates().getLatitude()));
        assertFalse(e.covers(p2.getCoordinates().getLongitude(),
                p2.getCoordinates().getLatitude()));
        // Second bbox contains both points
        ReferencedEnvelope e2 = boundingBoxPolygonToEnvelope(bboxNew);
        assertTrue(e2.covers(p.getCoordinates().getLongitude(),
                p.getCoordinates().getLatitude()));
        assertTrue(e2.covers(p2.getCoordinates().getLongitude(),
                p2.getCoordinates().getLatitude()));
    }
}
