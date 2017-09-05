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

import com.cgi.kinota.commons.application.exception.ApplicationErrorCode;
import com.cgi.kinota.commons.application.exception.ApplicationException;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;

import org.geojson.Point;
import org.geojson.Polygon;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;

import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 * Created by bmiles on 5/1/17.
 */
public class Utility {

    private static final Logger logger = LoggerFactory.getLogger(Utility.class);

    public static org.geojson.Point createGeoJsonPoint(double lng, double lat) throws ApplicationException {
        String jsonStr = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[" +
                lng + "," + lat + "]}}";
        return readGeoJsonPoint(jsonStr);
    }

    public static org.geojson.Point readGeoJsonPoint(String json) throws ApplicationException {
        org.geojson.Point p = null;
        try {
            org.geojson.Feature f = (org.geojson.Feature) new ObjectMapper().readValue(new StringReader(json),
                    GeoJsonObject.class);
            if (f.getGeometry() instanceof org.geojson.Point) {
                p = (org.geojson.Point) f.getGeometry();
            } else {
                String mesg = "GeoJSON object: '" + json + "' is not of type Point, so cannot be converted to a Point.";
                logger.error(mesg);
                throw new ApplicationException(ApplicationErrorCode.E_JSON, mesg);
            }
        } catch (IOException e) {
            String mesg = "Unable to create GeoJSON object: '" + json + "', due to error: " + e.getMessage();
            logger.error(mesg);
            throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
        }
        return p;
    }

    public static org.geojson.Polygon readGeoJsonPolygon(String json) throws ApplicationException {
        org.geojson.Polygon p = null;
        try {
            p = new ObjectMapper().readValue(new StringReader(json), org.geojson.Polygon.class);
        } catch (IOException e) {
            String mesg = "Unable to create GeoJSON Polygon: '" + json + "', due to error: " + e.getMessage();
            logger.error(mesg);
            throw new ApplicationException(ApplicationErrorCode.E_IO, mesg);
        }
        return p;
    }

    public static org.geojson.Polygon createBoundingBoxPolygonFromPoint(org.geojson.Point p, Double buffer) throws ApplicationException {
        if (buffer == null) buffer = 0.0001;
        LngLatAlt c = p.getCoordinates();
        double minLng = c.getLongitude() - buffer;
        double minLat = c.getLatitude() - buffer;
        double maxLng = c.getLongitude() + buffer;
        double maxLat = c.getLatitude() + buffer;
        return createBoundingBoxPolygonFromPoints(minLng, minLat, maxLng, maxLat);
    }

    public static org.geojson.Polygon createBoundingBoxPolygonFromPoints(List<Point> points) throws ApplicationException {
        Polygon p = null;

        if (points.size() == 1) {
            p = createBoundingBoxPolygonFromPoint(points.get(0), null);
        } else if (points.size() > 1) {
            double[] minMax = getMinMaxLngLat(points);
            p = createBoundingBoxPolygonFromPoints(minMax[0], minMax[1], minMax[2], minMax[3]);
        }

        return p;
    }

    public static org.geojson.Polygon createBoundingBoxPolygonFromPoints(double minLng, double minLat,
                                                                         double maxLng, double maxLat) throws ApplicationException {
        try {
            String bboxJsonStr = "{\"type\":\"Polygon\",\"coordinates\": [[" +
                    "[" + maxLng + "," + maxLat + "]," +
                    "[" + minLng + "," + maxLat + "]," +
                    "[" + minLng + "," + minLat + "]," +
                    "[" + maxLng + "," + minLat + "]," +
                    "[" + maxLng + "," + maxLat + "]" +
                    "]]}";
            return new ObjectMapper().readValue(new StringReader(bboxJsonStr), org.geojson.Polygon.class);
        } catch (IOException e) {
            String mesg = "Unable to create GeoJson Polygon from points " +
                    "{minLng:" + minLng + ",minLat:" + minLat +
                    "maxLng:" + maxLng + ",maxLat:" + maxLat + "} " +
                    " due to error: " + e.getMessage();
            logger.error(mesg);
            throw new ApplicationException(ApplicationErrorCode.E_JSON, mesg);
        }
    }

    public static ReferencedEnvelope boundingBoxPolygonToEnvelope(org.geojson.Polygon bbox) throws ApplicationException {
        ReferencedEnvelope env;
        CoordinateReferenceSystem crs = null;
        try {
            crs = CRS.decode("EPSG:4326");
        } catch (FactoryException e) {
            String mesg = "Unable to instantiate CRS EPSG:4326";
            logger.error(mesg);
            throw new ApplicationException(ApplicationErrorCode.E_InternalError,
                    mesg);
        }
        try {
            double[] minMax = getMinMaxLngLat(bbox);
            double minLng = minMax[0];
            double minLat = minMax[1];
            double maxLng = minMax[2];
            double maxLat = minMax[3];
            env = new ReferencedEnvelope(minLng, maxLng, minLat, maxLat, crs);
        } catch (MismatchedDimensionException e) {
            String mesg = "Unable to create envelope for bounding box polygon: " + bbox.toString();
            logger.error(mesg);
            throw new ApplicationException(ApplicationErrorCode.E_InternalError,
                    mesg);
        }
        return env;
    }

    public static org.geojson.Polygon updateBoundingBox(org.geojson.Polygon bbox,
                                                        org.geojson.Point point) throws ApplicationException {
        org.geojson.Point[] points = {point};
        return updateBoundingBox(bbox, points);
    }

    public static org.geojson.Polygon updateBoundingBox(org.geojson.Polygon bbox,
                                                        org.geojson.Point[] points) throws ApplicationException {
        // Convert GeoJson Polygon into a GeoTools ReferencedEnvelope
        ReferencedEnvelope env = boundingBoxPolygonToEnvelope(bbox);

        // Iterate over points, extending ReferencedEnvelope by each
        for (org.geojson.Point p : points) {
            LngLatAlt c = p.getCoordinates();
            env.include(c.getLongitude(), c.getLatitude());
        }

        // Convert ReferencedEnvelope back into a GeoJson Polygon
        org.geojson.Polygon bboxNew = createBoundingBoxPolygonFromPoints(env.getMinX(), env.getMinY(),
                env.getMaxX(), env.getMaxY());
        return bboxNew;
    }

    public static double[] getMinMaxLngLat(org.geojson.Polygon p) {
        double[] minMax = null;

        double minLng = 180.0;
        double maxLng = -180.0;
        double minLat = 90.0;
        double maxLat = -90.0;

        List<List<LngLatAlt>> coordsList = p.getCoordinates();
        if (coordsList.size() > 0) {
            List<LngLatAlt> coords = coordsList.get(0);
            if (coords.size() > 0) {
                double lng, lat;
                for (LngLatAlt c : coords) {
                    lng = c.getLongitude();
                    lat = c.getLatitude();
                    if (lng < minLng) minLng = lng;
                    if (lng > maxLng) maxLng = lng;
                    if (lat < minLat) minLat = lat;
                    if (lat > maxLat) maxLat = lat;
                }
                minMax = new double[4];
                minMax[0] = minLng; minMax[1] = minLat;
                minMax[2] = maxLng; minMax[3] = maxLat;
            }
        }

        return minMax;
    }

    public static double[] getMinMaxLngLat(List<Point> points) {
        double[] minMax = new double[4];

        double minLng = 180.0;
        double maxLng = -180.0;
        double minLat = 90.0;
        double maxLat = -90.0;

        for (Point p : points) {
            LngLatAlt c = p.getCoordinates();
            double lng = c.getLongitude();
            double lat = c.getLatitude();
            if (lng < minLng) minLng = lng;
            if (lng > maxLng) maxLng = lng;
            if (lat < minLat) minLat = lat;
            if (lat > maxLat) maxLat = lat;

            minMax[0] = minLng; minMax[1] = minLat;
            minMax[2] = maxLng; minMax[3] = maxLat;
        }

        return minMax;
    }

    public static org.geojson.Polygon wktPolygonToGeoJsonPolygon(String wkt) {
        // POLYGON ((10 10, 10 10, 10 10, 10 10, 10 10))
        org.geojson.Polygon poly = null;

        if (wkt.startsWith("POLYGON")) {
            int coordStart = wkt.indexOf('(') + 2;
            int coordStop = wkt.indexOf(')');
            if (coordStart != -1 && coordStop != -1 && coordStop > coordStart) {
                String jsonStr = "{\"type\":\"Polygon\",\"coordinates\":[[";
                String pointsStr = wkt.substring(coordStart, coordStop);
                String[] points = pointsStr.split(",");
                int num = points.length - 1;
                int i = 0;
                while (true) {
                    String p = points[i].trim();
                    String[] coord = p.split(" ");
                    jsonStr += "[" + coord[0] + "," + coord[1] + "]";
                    if (i == num) {
                        jsonStr += "]]";
                        break;
                    }
                    jsonStr += ",";
                    i++;
                }
                jsonStr += "}";
                try {
                    poly = new ObjectMapper().readValue(new StringReader(jsonStr), org.geojson.Polygon.class);
                } catch (IOException e) {
                    String mesg = "Unable to create GeoJson Polygon from WKT '" + wkt + "', error: " + e.getMessage();
                    logger.error(mesg);
                    throw new ApplicationException(ApplicationErrorCode.E_JSON, mesg);
                }
            }
        }

        return poly;
    }
}
