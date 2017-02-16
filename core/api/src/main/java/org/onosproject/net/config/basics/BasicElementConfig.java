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
package org.onosproject.net.config.basics;

/**
 * Basic configuration for network elements, e.g. devices, hosts. Such elements
 * can have a friendly name, geo-coordinates (or grid-coordinates),
 * logical rack coordinates and an owner entity.
 */
public abstract class BasicElementConfig<S> extends AllowedEntityConfig<S> {

    /**
     * Key for friendly name.
     */
    public static final String NAME = "name";

    /**
     * Key for UI type (glyph identifier).
     */
    public static final String UI_TYPE = "uiType";

    /**
     * Key for location type (geo or grid).
     */
    public static final String LOC_TYPE = "locType";

    /**
     * Key for latitude.
     */
    public static final String LATITUDE = "latitude";

    /**
     * Key for longitude.
     */
    public static final String LONGITUDE = "longitude";

    /**
     * Key for grid Y coordinate.
     */
    public static final String GRID_Y = "gridY";

    /**
     * Key for grid X coordinate.
     */
    public static final String GRID_X = "gridX";

    /**
     * Key for rack address.
     */
    protected static final String RACK_ADDRESS = "rackAddress";

    /**
     * Key for owner.
     */
    protected static final String OWNER = "owner";

    /**
     * Threshold for detecting double value is zero.
     */
    protected static final double ZERO_THRESHOLD = Double.MIN_VALUE * 2.0;

    private static final double DEFAULT_COORD = 0.0;
    private static final String LOC_TYPE_GEO = "geo";
    private static final String LOC_TYPE_GRID = "grid";

    /**
     * Returns friendly label for the element. If not set, returns the
     * element identifier.
     *
     * @return friendly label or element identifier itself if not set
     */
    public String name() {
        return get(NAME, subject.toString());
    }

    /**
     * Sets friendly label for the element.
     *
     * @param name new friendly label; null to clear
     * @return self
     */
    public BasicElementConfig name(String name) {
        return (BasicElementConfig) setOrClear(NAME, name);
    }

    /**
     * Returns the UI type (glyph image to be used) for the element in
     * the Topology View. If not set, null is returned.
     *
     * @return the UI type
     */
    public String uiType() {
        return get(UI_TYPE, null);
    }

    /**
     * Sets the UI type (glyph image to be used) for the element in
     * the Topology View. Setting this to null will indicate that the
     * default glyph image should be used for the element type.
     *
     * @param uiType the UI type; null for default
     * @return self
     */
    public BasicElementConfig uiType(String uiType) {
        return (BasicElementConfig) setOrClear(UI_TYPE, uiType);
    }

    /**
     * Returns the location type (geo or grid) for the element in
     * the Topology View. If not set, returns the default of "geo".
     *
     * @return location type (string)
     */
    public String locType() {
        return get(LOC_TYPE, LOC_TYPE_GEO);
    }

    /**
     * Sets the location type (geo or grid) for the element in
     * the Topology View. If null is passsed, it will default to "geo".
     *
     * @param locType the UI type; null for default
     * @return self
     */
    public BasicElementConfig locType(String locType) {
        String lt = LOC_TYPE_GRID.equals(locType) ? LOC_TYPE_GRID : LOC_TYPE_GEO;
        return (BasicElementConfig) setOrClear(LOC_TYPE, lt);
    }

    private boolean doubleIsZero(double value) {
        return value >= -ZERO_THRESHOLD && value <= ZERO_THRESHOLD;
    }

    /**
     * Returns true if the geographical coordinates (latitude and longitude)
     * are set on this element; false otherwise.
     *
     * @return true if geo-coordinates are set; false otherwise
     */
    public boolean geoCoordsSet() {
        return !doubleIsZero(latitude()) || !doubleIsZero(longitude());
    }

    /**
     * Returns element latitude.
     *
     * @return element latitude; 0.0 if (possibly) not set
     * @see #geoCoordsSet()
     */
    public double latitude() {
        return get(LATITUDE, DEFAULT_COORD);
    }

    /**
     * Sets the element latitude.
     *
     * @param latitude new latitude; null to clear
     * @return self
     */
    public BasicElementConfig latitude(Double latitude) {
        return (BasicElementConfig) setOrClear(LATITUDE, latitude);
    }

    /**
     * Returns element longitude.
     *
     * @return element longitude; 0 if (possibly) not set
     * @see #geoCoordsSet()
     */
    public double longitude() {
        return get(LONGITUDE, DEFAULT_COORD);
    }

    /**
     * Sets the element longitude.
     *
     * @param longitude new longitude; null to clear
     * @return self
     */
    public BasicElementConfig longitude(Double longitude) {
        return (BasicElementConfig) setOrClear(LONGITUDE, longitude);
    }

    /**
     * Returns true if the grid coordinates (gridY and gridX) are set on
     * this element; false otherwise.
     * <p>
     * It is assumed that elements will not be placed at {@code (0,0)}.
     * If you really need to position the element there, consider setting the
     * coordinates to something like {@code (0.000001, 0.000001)} instead.
     *
     * @return true if grid coordinates are set; false otherwise.
     */
    public boolean gridCoordsSet() {
        return !doubleIsZero(gridY()) || !doubleIsZero(gridX());
    }

    /**
     * Returns element grid y-coordinate.
     *
     * @return element y-coordinate
     */
    public double gridY() {
        return get(GRID_Y, DEFAULT_COORD);
    }

    /**
     * Sets the element grid y-coordinate.
     *
     * @param y new y-coordinate; null to clear
     * @return self
     */
    public BasicElementConfig gridY(Double y) {
        return (BasicElementConfig) setOrClear(GRID_Y, y);
    }

    /**
     * Returns element grid x-coordinate.
     *
     * @return element x-coordinate
     */
    public double gridX() {
        return get(GRID_X, DEFAULT_COORD);
    }

    /**
     * Sets the element grid x-coordinate.
     *
     * @param x new x-coordinate; null to clear
     * @return self
     */
    public BasicElementConfig gridX(Double x) {
        return (BasicElementConfig) setOrClear(GRID_X, x);
    }

    /**
     * Returns the element rack address.
     *
     * @return rack address; null if not set
     */
    public String rackAddress() {
        return get(RACK_ADDRESS, null);
    }

    /**
     * Sets element rack address.
     *
     * @param address new rack address; null to clear
     * @return self
     */
    public BasicElementConfig rackAddress(String address) {
        return (BasicElementConfig) setOrClear(RACK_ADDRESS, address);
    }

    /**
     * Returns owner of the element.
     *
     * @return owner or null if not set
     */
    public String owner() {
        return get(OWNER, null);
    }

    /**
     * Sets the owner of the element.
     *
     * @param owner new owner; null to clear
     * @return self
     */
    public BasicElementConfig owner(String owner) {
        return (BasicElementConfig) setOrClear(OWNER, owner);
    }

}
