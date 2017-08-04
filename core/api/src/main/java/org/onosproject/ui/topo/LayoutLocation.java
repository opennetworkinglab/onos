/*
 * Copyright 2017-present Open Networking Foundation
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
 *
 */

package org.onosproject.ui.topo;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a "node location" on a UI layout.
 */
public final class LayoutLocation {
    private static final double ZERO_THRESHOLD = Double.MIN_VALUE * 2.0;
    private static final String COMMA = ",";
    private static final String TILDE = "~";
    private static final String EMPTY = "";

    private static final String E_BAD_COMPACT = "Badly formatted compact form: ";
    private static final String E_EMPTY_ID = "id must be non-empty";
    private static final String E_BAD_DOUBLE = "unparsable double";


    /**
     * Designates the type of location; either geographic or logical grid.
     */
    public enum Type {
        GEO, GRID;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    private final String id;
    private final double latOrY;
    private final double longOrX;
    private final Type locType;

    private LayoutLocation(String id, Type locType, double latOrY, double longOrX) {
        this.id = id;
        this.latOrY = latOrY;
        this.longOrX = longOrX;
        this.locType = locType;
    }

    private boolean doubleIsZero(double value) {
        return value >= -ZERO_THRESHOLD && value <= ZERO_THRESHOLD;
    }

    /**
     * Returns true if the coordinates indicate the origin (0, 0) of the
     * coordinate system; false otherwise.
     *
     * @return true if geo-coordinates are set; false otherwise
     */
    public boolean isOrigin() {
        return doubleIsZero(latOrY) && doubleIsZero(longOrX);
    }

    /**
     * Returns the identifier associated with this location.
     *
     * @return the identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns the location type (geo or grid), which indicates how the data
     * is to be interpreted.
     *
     * @return location type
     */
    public Type locType() {
        return locType;
    }

    /**
     * Returns the latitude (geo) or y-coord (grid) data value.
     *
     * @return geo latitude or grid y-coord
     */
    public double latOrY() {
        return latOrY;
    }

    /**
     * Returns the longitude (geo) or x-coord (grid) data value.
     *
     * @return geo longitude or grid x-coord
     */
    public double longOrX() {
        return longOrX;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("loc-type", locType)
                .add("lat/Y", latOrY)
                .add("long/X", longOrX)
                .toString();
    }

    /**
     * Produces a compact string representation of this instance.
     *
     * @return compact string rep
     */
    public String toCompactListString() {
        return id + COMMA + locType + COMMA + latOrY + COMMA + longOrX;
    }

    /**
     * Produces a layout location instance from a compact string representation.
     *
     * @param s the compact string
     * @return a corresponding instance
     */
    public static LayoutLocation fromCompactString(String s) {
        String[] tokens = s.split(COMMA);
        if (tokens.length != 4) {
            throw new IllegalArgumentException(E_BAD_COMPACT + s);
        }
        String id = tokens[0];
        String type = tokens[1];
        String latY = tokens[2];
        String longX = tokens[3];

        if (Strings.isNullOrEmpty(id)) {
            throw new IllegalArgumentException(E_BAD_COMPACT + E_EMPTY_ID);
        }

        double latOrY;
        double longOrX;
        try {
            latOrY = Double.parseDouble(latY);
            longOrX = Double.parseDouble(longX);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(E_BAD_COMPACT + E_BAD_DOUBLE);
        }

        return LayoutLocation.layoutLocation(id, type, latOrY, longOrX);
    }

    /**
     * Produces a compact encoding of a list of layout locations.
     *
     * @param locs array of layout location instances
     * @return string encoding
     */
    public static String toCompactListString(LayoutLocation... locs) {
        if (locs == null || locs.length == 0) {
            return EMPTY;
        }
        List<LayoutLocation> lls = Arrays.asList(locs);
        return toCompactListString(lls);
    }

    /**
     * Produces a compact encoding of a list of layout locations.
     *
     * @param locs list of layout location instances
     * @return string encoding
     */
    public static String toCompactListString(List<LayoutLocation> locs) {
        // note: locs may be empty
        if (locs == null || locs.isEmpty()) {
            return EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        for (LayoutLocation ll : locs) {
            sb.append(ll.toCompactListString()).append(TILDE);
        }
        final int len = sb.length();
        sb.replace(len - 1, len, "");
        return sb.toString();
    }

    /**
     * Returns a list of layout locations from a compact string representation.
     *
     * @param compactList string representation
     * @return corresponding list of layout locations
     */
    public static List<LayoutLocation> fromCompactListString(String compactList) {
        List<LayoutLocation> locs = new ArrayList<>();
        if (!Strings.isNullOrEmpty(compactList)) {
            String[] items = compactList.split(TILDE);
            for (String s : items) {
                locs.add(fromCompactString(s));
            }
        }
        return locs;
    }


    /**
     * Creates an instance of a layout location.
     *
     * @param id      an identifier for the item at this location
     * @param locType the location type
     * @param latOrY  geo latitude / grid y-coord
     * @param longOrX geo longitude / grid x-coord
     * @return layout location instance
     */
    public static LayoutLocation layoutLocation(String id, Type locType,
                                                double latOrY, double longOrX) {
        checkNotNull(id, "must supply an identifier");
        checkNotNull(locType, "must declare location type");
        return new LayoutLocation(id, locType, latOrY, longOrX);
    }

    /**
     * Creates an instance of a layout location.
     *
     * @param id      an identifier for the item at this location
     * @param locType the location type ("geo" or "grid")
     * @param latOrY  geo latitude / grid y-coord
     * @param longOrX geo longitude / grid x-coord
     * @return layout location instance
     * @throws IllegalArgumentException if the type is not "geo" or "grid"
     */
    public static LayoutLocation layoutLocation(String id, String locType,
                                                double latOrY, double longOrX) {
        Type t = Type.valueOf(locType.toUpperCase());
        return new LayoutLocation(id, t, latOrY, longOrX);
    }
}
