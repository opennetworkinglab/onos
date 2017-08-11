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
 */

package org.onosproject.drivers.lisp.extensions;

import com.google.common.collect.Maps;
import org.onosproject.mapping.addresses.ExtensionMappingAddress;
import org.onosproject.mapping.addresses.ExtensionMappingAddressType;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.net.flow.AbstractExtension;

import java.util.Map;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.mapping.addresses.ExtensionMappingAddressType
                            .ExtensionMappingAddressTypes.GEO_COORDINATE_ADDRESS;

/**
 * Implementation of LISP Geo Coordinate (GC) address.
 * If an ETR desires to send a Map-Reply describing the Geo Coordinates for each
 * locator in its locator-set, it can use the Geo Coordinate Type to convey
 * physical location information.
 */
public class LispGcAddress extends AbstractExtension
        implements ExtensionMappingAddress {

    private static final String NORTH = "north";
    private static final String LATITUDE_DEGREE = "latitudeDegree";
    private static final String LATITUDE_MINUTE = "latitudeMinute";
    private static final String LATITUDE_SECOND = "latitudeSecond";
    private static final String EAST = "east";
    private static final String LONGITUDE_DEGREE = "longitudeDegree";
    private static final String LONGITUDE_MINUTE = "longitudeMinute";
    private static final String LONGITUDE_SECOND = "longitudeSecond";
    private static final String ALTITUDE = "altitude";
    private static final String ADDRESS = "address";

    private boolean north;
    private short latitudeDegree;
    private byte latitudeMinute;
    private byte latitudeSecond;
    private boolean east;
    private short longitudeDegree;
    private byte longitudeMinute;
    private byte longitudeSecond;
    private int altitude;
    private MappingAddress address;

    /**
     * Default constructor.
     */
    public LispGcAddress() {
    }

    /**
     * Creates an instance with initialized parameters.
     *
     * @param north           north flag
     * @param latitudeDegree  latitude degree
     * @param latitudeMinute  latitude minute
     * @param latitudeSecond  latitude second
     * @param east            east flag
     * @param longitudeDegree longitude degree
     * @param longitudeMinute longitude minute
     * @param longitudeSecond longitude second
     * @param altitude        altitude
     * @param address         mapping address
     */
    private LispGcAddress(boolean north, short latitudeDegree, byte latitudeMinute,
                          byte latitudeSecond, boolean east, short longitudeDegree,
                          byte longitudeMinute, byte longitudeSecond, int altitude,
                          MappingAddress address) {
        this.north = north;
        this.latitudeDegree = latitudeDegree;
        this.latitudeMinute = latitudeMinute;
        this.latitudeSecond = latitudeSecond;
        this.east = east;
        this.longitudeDegree = longitudeDegree;
        this.longitudeMinute = longitudeMinute;
        this.longitudeSecond = longitudeSecond;
        this.altitude = altitude;
        this.address = address;
    }

    /**
     * Obtains north flag value.
     *
     * @return north flag value
     */
    public boolean isNorth() {
        return north;
    }

    /**
     * Obtains latitude degree.
     *
     * @return latitude degree
     */
    public short getLatitudeDegree() {
        return latitudeDegree;
    }

    /**
     * Obtains latitude minute.
     *
     * @return latitude minute
     */
    public byte getLatitudeMinute() {
        return latitudeMinute;
    }

    /**
     * Obtains latitude second.
     *
     * @return latitude second
     */
    public byte getLatitudeSecond() {
        return latitudeSecond;
    }

    /**
     * Obtains east flag value.
     *
     * @return east flag vlaue
     */
    public boolean isEast() {
        return east;
    }

    /**
     * Obtains longitude degree.
     *
     * @return longitude degree
     */
    public short getLongitudeDegree() {
        return longitudeDegree;
    }

    /**
     * Obtains longitude minute.
     *
     * @return longitude minute
     */
    public byte getLongitudeMinute() {
        return longitudeMinute;
    }

    /**
     * Obtains longitude second.
     *
     * @return longitude second
     */
    public byte getLongitudeSecond() {
        return longitudeSecond;
    }

    /**
     * Obtains altitude.
     *
     * @return altitude
     */
    public int getAltitude() {
        return altitude;
    }

    /**
     * Obtains mapping address.
     *
     * @return mapping address
     */
    public MappingAddress getAddress() {
        return address;
    }

    @Override
    public ExtensionMappingAddressType type() {
        return GEO_COORDINATE_ADDRESS.type();
    }

    @Override
    public byte[] serialize() {
        Map<String, Object> parameterMap = Maps.newHashMap();

        parameterMap.put(NORTH, north);
        parameterMap.put(LATITUDE_DEGREE, latitudeDegree);
        parameterMap.put(LATITUDE_MINUTE, latitudeMinute);
        parameterMap.put(LATITUDE_SECOND, latitudeSecond);
        parameterMap.put(EAST, east);
        parameterMap.put(LONGITUDE_DEGREE, longitudeDegree);
        parameterMap.put(LONGITUDE_MINUTE, longitudeMinute);
        parameterMap.put(LONGITUDE_SECOND, longitudeSecond);
        parameterMap.put(ALTITUDE, altitude);
        parameterMap.put(ADDRESS, address);

        return APP_KRYO.serialize(parameterMap);
    }

    @Override
    public void deserialize(byte[] data) {
        Map<String, Object> parameterMap = APP_KRYO.deserialize(data);

        this.north = (boolean) parameterMap.get(NORTH);
        this.latitudeDegree = (short) parameterMap.get(LATITUDE_DEGREE);
        this.latitudeMinute = (byte) parameterMap.get(LATITUDE_MINUTE);
        this.latitudeSecond = (byte) parameterMap.get(LATITUDE_SECOND);
        this.east = (boolean) parameterMap.get(EAST);
        this.longitudeDegree = (short) parameterMap.get(LONGITUDE_DEGREE);
        this.longitudeMinute = (byte) parameterMap.get(LONGITUDE_MINUTE);
        this.longitudeSecond = (byte) parameterMap.get(LONGITUDE_SECOND);
        this.altitude = (int) parameterMap.get(ALTITUDE);
        this.address = (MappingAddress) parameterMap.get(ADDRESS);
    }

    @Override
    public int hashCode() {
        return Objects.hash(north, latitudeDegree, latitudeMinute, latitudeSecond,
                east, longitudeDegree, longitudeMinute, longitudeSecond,
                altitude, address);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispGcAddress) {
            final LispGcAddress other = (LispGcAddress) obj;
            return Objects.equals(this.north, other.north) &&
                    Objects.equals(this.latitudeDegree, other.latitudeDegree) &&
                    Objects.equals(this.latitudeMinute, other.latitudeMinute) &&
                    Objects.equals(this.latitudeSecond, other.latitudeSecond) &&
                    Objects.equals(this.east, other.east) &&
                    Objects.equals(this.longitudeDegree, other.longitudeDegree) &&
                    Objects.equals(this.longitudeMinute, other.longitudeMinute) &&
                    Objects.equals(this.longitudeSecond, other.longitudeSecond) &&
                    Objects.equals(this.altitude, other.altitude) &&
                    Objects.equals(this.address, other.address);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(type().toString())
                .add("north", north)
                .add("latitude degree", latitudeDegree)
                .add("latitude minute", latitudeMinute)
                .add("latitude second", latitudeSecond)
                .add("east", east)
                .add("longitude degree", longitudeDegree)
                .add("longitude minute", longitudeMinute)
                .add("longitude second", longitudeSecond)
                .add("altitude", altitude)
                .add("address", address)
                .toString();
    }

    /**
     * A builder for building LispGcAddress.
     */
    public static final class Builder {
        private boolean north;
        private short latitudeDegree;
        private byte latitudeMinute;
        private byte latitudeSecond;
        private boolean east;
        private short longitudeDegree;
        private byte longitudeMinute;
        private byte longitudeSecond;
        private int altitude;
        private MappingAddress address;

        /**
         * Sets north flag value.
         *
         * @param north north flag value
         * @return Builder object
         */
        public Builder withIsNorth(boolean north) {
            this.north = north;
            return this;
        }

        /**
         * Sets latitude degree.
         *
         * @param latitudeDegree latitude degree
         * @return Builder object
         */
        public Builder withLatitudeDegree(short latitudeDegree) {
            this.latitudeDegree = latitudeDegree;
            return this;
        }

        /**
         * Sets latitude minute.
         *
         * @param latitudeMinute latitude minute
         * @return Builder object
         */
        public Builder withLatitudeMinute(byte latitudeMinute) {
            this.latitudeMinute = latitudeMinute;
            return this;
        }

        /**
         * Sets latitude second.
         *
         * @param latitudeSecond latitude second
         * @return Builder object
         */
        public Builder withLatitudeSecond(byte latitudeSecond) {
            this.latitudeSecond = latitudeSecond;
            return this;
        }

        /**
         * Sets east flag value.
         *
         * @param east east flag
         * @return Builder object
         */
        public Builder withIsEast(boolean east) {
            this.east = east;
            return this;
        }

        /**
         * Sets longitude degree.
         *
         * @param longitudeDegree longitude degree
         * @return Builder object
         */
        public Builder withLongitudeDegree(short longitudeDegree) {
            this.longitudeDegree = longitudeDegree;
            return this;
        }

        /**
         * Sets longitude minute.
         *
         * @param longitudeMinute longitude minute
         * @return Builder object
         */
        public Builder withLongitudeMinute(byte longitudeMinute) {
            this.longitudeMinute = longitudeMinute;
            return this;
        }

        /**
         * Sets longitude second.
         *
         * @param longitudeSecond longitude second
         * @return Builder object
         */
        public Builder withLongitudeSecond(byte longitudeSecond) {
            this.longitudeSecond = longitudeSecond;
            return this;
        }

        /**
         * Sets altitude.
         *
         * @param altitude altitude
         * @return Builder object
         */
        public Builder withAltitude(int altitude) {
            this.altitude = altitude;
            return this;
        }

        /**
         * Sets mapping address.
         *
         * @param address mapping address
         * @return Builder object
         */
        public Builder withAddress(MappingAddress address) {
            this.address = address;
            return this;
        }

        /**
         * Builds LispGcAddress instance.
         *
         * @return LispGcAddress instance
         */
        public LispGcAddress build() {

            return new LispGcAddress(north, latitudeDegree,
                    latitudeMinute, latitudeSecond, east, longitudeDegree,
                    longitudeMinute, longitudeSecond, altitude, address);
        }
    }
}
