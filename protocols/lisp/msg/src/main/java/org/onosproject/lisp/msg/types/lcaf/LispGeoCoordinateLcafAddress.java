/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.lisp.msg.types.lcaf;

import io.netty.buffer.ByteBuf;
import org.onlab.util.ByteOperator;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispAddressReader;
import org.onosproject.lisp.msg.types.LispAddressWriter;
import org.onosproject.lisp.msg.types.LispAfiAddress;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Geo Coordinate type LCAF address class.
 * <p>
 * Geo Coordinate type is defined in draft-ietf-lisp-lcaf-22
 * https://tools.ietf.org/html/draft-ietf-lisp-lcaf-22#page-11
 *
 * <pre>
 * {@literal
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           AFI = 16387         |     Rsvd1     |     Flags     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   Type = 5    |     Rsvd2     |            Length             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |N|     Latitude Degrees        |    Minutes    |    Seconds    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |E|     Longitude Degrees       |    Minutes    |    Seconds    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                            Altitude                           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |              AFI = x          |         Address  ...          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * }</pre>
 */
public final class LispGeoCoordinateLcafAddress extends LispLcafAddress {

    private final boolean north;
    private final short latitudeDegree;
    private final byte latitudeMinute;
    private final byte latitudeSecond;
    private final boolean east;
    private final short longitudeDegree;
    private final byte longitudeMinute;
    private final byte longitudeSecond;
    private final int altitude;
    private final LispAfiAddress address;

    /**
     * Initializes geo coordinate type LCAF address.
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
     * @param address         AFI address
     */
    private LispGeoCoordinateLcafAddress(boolean north, short latitudeDegree,
                                         byte latitudeMinute, byte latitudeSecond,
                                         boolean east, short longitudeDegree,
                                         byte longitudeMinute, byte longitudeSecond,
                                         int altitude, LispAfiAddress address) {
        super(LispCanonicalAddressFormatEnum.GEO_COORDINATE);
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
     * Obtains AFI address.
     *
     * @return AFI address
     */
    public LispAfiAddress getAddress() {
        return address;
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

        if (obj instanceof LispGeoCoordinateLcafAddress) {
            final LispGeoCoordinateLcafAddress other =
                    (LispGeoCoordinateLcafAddress) obj;
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
        return toStringHelper(this)
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

    public static final class GeoCoordinateAddressBuilder
                        extends LcafAddressBuilder<GeoCoordinateAddressBuilder> {
        private boolean north;
        private short latitudeDegree;
        private byte latitudeMinute;
        private byte latitudeSecond;
        private boolean east;
        private short longitudeDegree;
        private byte longitudeMinute;
        private byte longitudeSecond;
        private int altitude;
        private LispAfiAddress address;

        /**
         * Sets north flag value.
         *
         * @param north north flag value
         * @return GeoCoordinateAddressBuilder object
         */
        public GeoCoordinateAddressBuilder withIsNorth(boolean north) {
            this.north = north;
            return this;
        }

        /**
         * Sets latitude degree.
         *
         * @param latitudeDegree latitude degree
         * @return GeoCoordinateAddressBuilder object
         */
        public GeoCoordinateAddressBuilder withLatitudeDegree(short latitudeDegree) {
            this.latitudeDegree = latitudeDegree;
            return this;
        }

        /**
         * Sets latitude minute.
         *
         * @param latitudeMinute latitude minute
         * @return GeoCoordinateAddressBuilder object
         */
        public GeoCoordinateAddressBuilder withLatitudeMinute(byte latitudeMinute) {
            this.latitudeMinute = latitudeMinute;
            return this;
        }

        /**
         * Sets latitude second.
         *
         * @param latitudeSecond latitude second
         * @return GeoCoordinateAddressBuilder object
         */
        public GeoCoordinateAddressBuilder withLatitudeSecond(byte latitudeSecond) {
            this.latitudeSecond = latitudeSecond;
            return this;
        }

        /**
         * Sets east flag value.
         *
         * @param east east flag
         * @return GeoCoordinateAddressBuilder object
         */
        public GeoCoordinateAddressBuilder withIsEast(boolean east) {
            this.east = east;
            return this;
        }

        /**
         * Sets longitude degree.
         *
         * @param longitudeDegree longitude degree
         * @return GeoCoordinateAddressBuilder object
         */
        public GeoCoordinateAddressBuilder withLongitudeDegree(short longitudeDegree) {
            this.longitudeDegree = longitudeDegree;
            return this;
        }

        /**
         * Sets longitude minute.
         *
         * @param longitudeMinute longitude minute
         * @return GeoCoordinateAddressBuilder object
         */
        public GeoCoordinateAddressBuilder withLongitudeMinute(byte longitudeMinute) {
            this.longitudeMinute = longitudeMinute;
            return this;
        }

        /**
         * Sets longitude second.
         *
         * @param longitudeSecond longitude second
         * @return GeoCoordinateAddressBuilder object
         */
        public GeoCoordinateAddressBuilder withLongitudeSecond(byte longitudeSecond) {
            this.longitudeSecond = longitudeSecond;
            return this;
        }

        /**
         * Sets altitude.
         *
         * @param altitude altitude
         * @return GeoCoordinateAddressBuilder object
         */
        public GeoCoordinateAddressBuilder withAltitude(int altitude) {
            this.altitude = altitude;
            return this;
        }

        /**
         * Sets AFI address.
         *
         * @param address AFI address
         * @return GeoCoordinateAddressBuilder object
         */
        public GeoCoordinateAddressBuilder withAddress(LispAfiAddress address) {
            this.address = address;
            return this;
        }

        /**
         * Builds LispGeoCoordinateLcafAddress instance.
         *
         * @return LispGeoCoordinateLcafAddress instance
         */
        @Override
        public LispGeoCoordinateLcafAddress build() {

            checkNotNull(address, "Must specify an AFI address");

            return new LispGeoCoordinateLcafAddress(north, latitudeDegree,
                        latitudeMinute, latitudeSecond, east, longitudeDegree,
                        longitudeMinute, longitudeSecond, altitude, address);
        }
    }

    /**
     * GeoCoordinate LCAF address reader class.
     */
    public static class GeoCoordinateLcafAddressReader
                    implements LispAddressReader<LispGeoCoordinateLcafAddress> {

        private static final int NORTH_INDEX = 7;
        private static final int EAST_INDEX = 7;
        private static final int FLAG_SHIFT = 8;

        @Override
        public LispGeoCoordinateLcafAddress readFrom(ByteBuf byteBuf)
                                    throws LispParseError, LispReaderException {

            LispLcafAddress.deserializeCommon(byteBuf);

            // north flag -> 1 bit
            byte flagWithLatitude = byteBuf.readByte();

            boolean north = ByteOperator.getBit(flagWithLatitude, NORTH_INDEX);

            // latitude degree -> 15 bits
            short latitudeFirst = flagWithLatitude;
            if (north) {
                latitudeFirst = (short) (flagWithLatitude & 0x7F);
            }
            short latitude = (short) ((latitudeFirst << FLAG_SHIFT) + byteBuf.readByte());

            // latitude minute -> 8 bits
            byte latitudeMinute = byteBuf.readByte();

            // latitude second -> 8 bits
            byte latitudeSecond = byteBuf.readByte();

            // east flag -> 1 bit
            byte flagWithLongitude = byteBuf.readByte();

            boolean east = ByteOperator.getBit(flagWithLongitude, EAST_INDEX);

            // longitude degree -> 15 bits
            short longitudeFirst = flagWithLongitude;
            if (east) {
                longitudeFirst = (short) (flagWithLongitude & 0x7F);
            }
            short longitude = (short) ((longitudeFirst << FLAG_SHIFT) + byteBuf.readByte());

            // longitude minute -> 8 bits
            byte longitudeMinute = byteBuf.readByte();

            // longitude second -> 8 bits
            byte longitudeSecond = byteBuf.readByte();

            // altitude -> 32 bits
            int altitude = byteBuf.readInt();

            LispAfiAddress address = new AfiAddressReader().readFrom(byteBuf);

            return new GeoCoordinateAddressBuilder()
                            .withIsNorth(north)
                            .withLatitudeDegree(latitude)
                            .withLatitudeMinute(latitudeMinute)
                            .withLatitudeSecond(latitudeSecond)
                            .withIsEast(east)
                            .withLongitudeDegree(longitude)
                            .withLongitudeMinute(longitudeMinute)
                            .withLongitudeSecond(longitudeSecond)
                            .withAltitude(altitude)
                            .withAddress(address)
                            .build();
        }
    }

    /**
     * GeoCoordinate LCAF address writer class.
     */
    public static class GeoCoordinateLcafAddressWriter
                    implements LispAddressWriter<LispGeoCoordinateLcafAddress> {

        private static final int NORTH_SHIFT_BIT = 15;
        private static final int EAST_SHIFT_BIT = 15;

        private static final int ENABLE_BIT = 1;
        private static final int DISABLE_BIT = 0;

        @Override
        public void writeTo(ByteBuf byteBuf, LispGeoCoordinateLcafAddress address)
                                                    throws LispWriterException {

            int lcafIndex = byteBuf.writerIndex();
            LispLcafAddress.serializeCommon(byteBuf, address);

            // north flag + latitude degree
            short north = DISABLE_BIT;
            if (address.isNorth()) {
                north = (short) (ENABLE_BIT << NORTH_SHIFT_BIT);
            }

            byteBuf.writeShort(north + address.latitudeDegree);

            // latitude minute
            byteBuf.writeByte(address.latitudeMinute);

            // latitude second
            byteBuf.writeByte(address.latitudeSecond);

            // east flag + longitude degree
            short east = DISABLE_BIT;
            if (address.isEast()) {
                east = (short) (ENABLE_BIT << EAST_SHIFT_BIT);
            }

            byteBuf.writeShort(east + address.longitudeDegree);

            // longitude minute
            byteBuf.writeByte(address.longitudeMinute);

            // longitude second
            byteBuf.writeByte(address.longitudeSecond);

            // altitude
            byteBuf.writeInt(address.altitude);

            // address
            AfiAddressWriter writer = new AfiAddressWriter();
            writer.writeTo(byteBuf, address.getAddress());

            LispLcafAddress.updateLength(lcafIndex, byteBuf);
        }
    }
}
