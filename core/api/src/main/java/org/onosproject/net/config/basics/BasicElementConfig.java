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
 * can have a friendly name, geo-coordinates, logical rack coordinates and
 * an owner entity.
 */
public abstract class BasicElementConfig<S> extends AllowedEntityConfig<S> {

    protected static final String NAME = "name";

    protected static final String LATITUDE = "latitude";
    protected static final String LONGITUDE = "longitude";

    protected static final String RACK_ADDRESS = "rackAddress";
    protected static final String OWNER = "owner";

    protected static final double ZERO_THRESHOLD = Double.MIN_VALUE * 2.0;
    private static final double DEFAULT_COORD = 0.0;

    /**
     * Returns friendly label for the element.
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

    private static boolean doubleIsZero(double value) {
        return value >= -ZERO_THRESHOLD && value <= ZERO_THRESHOLD;
    }

    /**
     * Returns true if the geographical coordinates (latitude and longitude)
     * are set on this element.
     *
     * @return true if geo-coordinates are set
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
