/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onlab.util;

import com.google.common.base.MoreObjects;

/**
 * Geo location specified in terms of longitude and latitude.
 */
public class GeoLocation {

    public static final double EARTH_RADIUS_KM = 6378.1370D;

    private final double latitude;
    private final double longitude;

    /**
     * Creates a new location using the specified coordinates.
     *
     * @param latitude  latitude line
     * @param longitude longitude line
     */
    public GeoLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Returns the latitude of this location.
     *
     * @return latitude
     */
    public double latitude() {
        return latitude;
    }

    /**
     * Returns the longitude of this location.
     *
     * @return longitude
     */
    public double longitude() {
        return longitude;
    }

    /**
     * Returns the distance in kilometers, between this location and another.
     *
     * @param other other geo location
     * @return distance in kilometers
     */
    public double kilometersTo(GeoLocation other) {
        double hereLat = Math.toRadians(latitude);
        double hereLon = Math.toRadians(longitude);
        double thereLat = Math.toRadians(other.latitude);
        double thereLon = Math.toRadians(other.longitude);

        double cos = Math.sin(hereLat) * Math.sin(thereLat) +
                Math.cos(hereLat) * Math.cos(thereLat) * Math.cos(hereLon - thereLon);
        return Math.acos(cos) * EARTH_RADIUS_KM;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("latitude", latitude)
                .add("longitude", longitude)
                .toString();
    }

}
