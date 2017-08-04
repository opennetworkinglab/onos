/*
 * Copyright 2016-present Open Networking Foundation
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
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispAddressReader;
import org.onosproject.lisp.msg.types.LispAddressWriter;
import org.onosproject.lisp.msg.types.LispAfiAddress;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Application data type LCAF address class.
 * <p>
 * Application data type is defined in draft-ietf-lisp-lcaf-22
 * https://tools.ietf.org/html/draft-ietf-lisp-lcaf-22#page-29
 *
 * <pre>
 * {@literal
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           AFI = 16387         |     Rsvd1     |     Flags     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   Type = 4    |     Rsvd2     |            Length             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |       IP TOS, IPv6 TC, or Flow Label          |    Protocol   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    Local Port (lower-range)   |    Local Port (upper-range)   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   Remote Port (lower-range)   |   Remote Port (upper-range)   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |              AFI = x          |         Address  ...          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * }</pre>
 */
public final class LispAppDataLcafAddress extends LispLcafAddress {

    private final byte protocol;
    private final int ipTos;
    private final short localPortLow;
    private final short localPortHigh;
    private final short remotePortLow;
    private final short remotePortHigh;
    private LispAfiAddress address;

    /**
     * Initializes application data type LCAF address.
     *
     * @param protocol       protocol number
     * @param ipTos          IP type of service
     * @param localPortLow   low-ranged local port number
     * @param localPortHigh  high-ranged local port number
     * @param remotePortLow  low-ranged remote port number
     * @param remotePortHigh high-ranged remote port number
     * @param address        address
     */
    private LispAppDataLcafAddress(byte protocol, int ipTos, short localPortLow,
                                   short localPortHigh, short remotePortLow,
                                   short remotePortHigh, LispAfiAddress address) {
        super(LispCanonicalAddressFormatEnum.APPLICATION_DATA);
        this.protocol = protocol;
        this.ipTos = ipTos;
        this.localPortLow = localPortLow;
        this.localPortHigh = localPortHigh;
        this.remotePortLow = remotePortLow;
        this.remotePortHigh = remotePortHigh;
        this.address = address;
    }

    /**
     * Initializes application data type LCAF address.
     *
     * @param reserved1      reserved1
     * @param reserved2      reserved2
     * @param flag           flag
     * @param length         length
     * @param protocol       protocol number
     * @param ipTos          IP type of service
     * @param localPortLow   low-ranged local port number
     * @param localPortHigh  high-ranged local port number
     * @param remotePortLow  low-ranged remote port number
     * @param remotePortHigh high-ranged remote port number
     * @param address        address
     */
    private LispAppDataLcafAddress(byte reserved1, byte reserved2, byte flag, short length,
                                   byte protocol, int ipTos, short localPortLow,
                                   short localPortHigh, short remotePortLow,
                                   short remotePortHigh, LispAfiAddress address) {
        super(LispCanonicalAddressFormatEnum.APPLICATION_DATA, reserved1, reserved2, flag, length);
        this.protocol = protocol;
        this.ipTos = ipTos;
        this.localPortLow = localPortLow;
        this.localPortHigh = localPortHigh;
        this.remotePortLow = remotePortLow;
        this.remotePortHigh = remotePortHigh;
        this.address = address;
    }

    /**
     * Obtains protocol number.
     *
     * @return protocol number
     */
    public byte getProtocol() {
        return protocol;
    }

    /**
     * Obtains IP type of service.
     *
     * @return IP type of service
     */
    public int getIpTos() {
        return ipTos;
    }

    /**
     * Obtains low-ranged local port number.
     *
     * @return low-ranged local port number
     */
    public short getLocalPortLow() {
        return localPortLow;
    }

    /**
     * Obtains high-ranged local port number.
     *
     * @return high-ranged local port number
     */
    public short getLocalPortHigh() {
        return localPortHigh;
    }

    /**
     * Obtains low-ranged remote port number.
     *
     * @return low-ranged remote port number
     */
    public short getRemotePortLow() {
        return remotePortLow;
    }

    /**
     * Obtains high-ranged remote port number.
     *
     * @return high-ranged remote port number
     */
    public short getRemotePortHigh() {
        return remotePortHigh;
    }

    /**
     * Obtains address.
     *
     * @return address
     */
    public LispAfiAddress getAddress() {
        return address;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, protocol, ipTos, localPortLow,
                localPortHigh, remotePortLow, remotePortHigh);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispAppDataLcafAddress) {
            final LispAppDataLcafAddress other = (LispAppDataLcafAddress) obj;
            return Objects.equals(this.address, other.address) &&
                    Objects.equals(this.protocol, other.protocol) &&
                    Objects.equals(this.ipTos, other.ipTos) &&
                    Objects.equals(this.localPortLow, other.localPortLow) &&
                    Objects.equals(this.localPortHigh, other.localPortHigh) &&
                    Objects.equals(this.remotePortLow, other.remotePortLow) &&
                    Objects.equals(this.remotePortHigh, other.remotePortHigh);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("address", address)
                .add("protocol", protocol)
                .add("ip type of service", ipTos)
                .add("low-ranged local port number", localPortLow)
                .add("high-ranged local port number", localPortHigh)
                .add("low-ranged remote port number", remotePortLow)
                .add("high-ranged remote port number", remotePortHigh)
                .toString();
    }

    public static final class AppDataAddressBuilder
            extends LcafAddressBuilder<AppDataAddressBuilder> {
        private byte protocol;
        private int ipTos;
        private short localPortLow;
        private short localPortHigh;
        private short remotePortLow;
        private short remotePortHigh;
        private LispAfiAddress address;

        /**
         * Sets protocol number.
         *
         * @param protocol protocol number
         * @return AppDataAddressBuilder object
         */
        public AppDataAddressBuilder withProtocol(byte protocol) {
            this.protocol = protocol;
            return this;
        }

        /**
         * Sets IP type of service.
         *
         * @param ipTos IP type of service
         * @return AppDataAddressBuilder object
         */
        public AppDataAddressBuilder withIpTos(int ipTos) {
            this.ipTos = ipTos;
            return this;
        }

        /**
         * Sets low-ranged local port number.
         *
         * @param localPortLow low-ranged local port number
         * @return AppDataAddressBuilder object
         */
        public AppDataAddressBuilder withLocalPortLow(short localPortLow) {
            this.localPortLow = localPortLow;
            return this;
        }

        /**
         * Sets high-ranged local port number.
         *
         * @param localPortHigh high-ranged local port number
         * @return AppDataAddressBuilder object
         */
        public AppDataAddressBuilder withLocalPortHigh(short localPortHigh) {
            this.localPortHigh = localPortHigh;
            return this;
        }

        /**
         * Sets low-ranged remote port number.
         *
         * @param remotePortLow low-ranged remote port number
         * @return AppDataAddressBuilder object
         */
        public AppDataAddressBuilder withRemotePortLow(short remotePortLow) {
            this.remotePortLow = remotePortLow;
            return this;
        }

        /**
         * Sets high-ranged remote port number.
         *
         * @param remotePortHigh high-ranged remote port number
         * @return AppDataAddressBuilder object
         */
        public AppDataAddressBuilder withRemotePortHigh(short remotePortHigh) {
            this.remotePortHigh = remotePortHigh;
            return this;
        }

        /**
         * Sets AFI address.
         *
         * @param address AFI address
         * @return AppDataAddressBuilder object
         */
        public AppDataAddressBuilder withAddress(LispAfiAddress address) {
            this.address = address;
            return this;
        }

        /**
         * Builds LispAppDataLcafAddress instance.
         *
         * @return LispAddDataLcafAddress instance
         */
        public LispAppDataLcafAddress build() {

            checkNotNull(address, "Must specify an address");

            return new LispAppDataLcafAddress(reserved1, reserved2, flag, length,
                    protocol, ipTos, localPortLow, localPortHigh, remotePortLow,
                    remotePortHigh, address);
        }
    }

    /**
     * Application data LCAF address reader class.
     */
    public static class AppDataLcafAddressReader
            implements LispAddressReader<LispAppDataLcafAddress> {

        @Override
        public LispAppDataLcafAddress readFrom(ByteBuf byteBuf) throws LispParseError, LispReaderException {

            LispLcafAddress.deserializeCommon(byteBuf);

            byte[] ipTosByte = new byte[3];
            byteBuf.readBytes(ipTosByte);

            byte protocol = (byte) byteBuf.readUnsignedByte();
            int ipTos = getPartialInt(ipTosByte);
            short localPortLow = (short) byteBuf.readUnsignedShort();
            short localPortHigh = (short) byteBuf.readUnsignedShort();
            short remotePortLow = (short) byteBuf.readUnsignedShort();
            short remotePortHigh = (short) byteBuf.readUnsignedShort();

            LispAfiAddress address = new LispAfiAddress.AfiAddressReader().readFrom(byteBuf);

            return new AppDataAddressBuilder()
                    .withProtocol(protocol)
                    .withIpTos(ipTos)
                    .withLocalPortLow(localPortLow)
                    .withLocalPortHigh(localPortHigh)
                    .withRemotePortLow(remotePortLow)
                    .withRemotePortHigh(remotePortHigh)
                    .withAddress(address)
                    .build();
        }

        /**
         * An utility function that obtains the partial int value from byte arrays.
         *
         * @param bytes an array of bytes
         * @return converted integer
         */
        public static int getPartialInt(byte[] bytes) {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.position(4 - bytes.length);
            buffer.put(bytes);
            buffer.position(0);
            return buffer.getInt();
        }
    }

    /**
     * Application data LCAF address writer class.
     */
    public static class AppDataLcafAddressWriter
            implements LispAddressWriter<LispAppDataLcafAddress> {

        @Override
        public void writeTo(ByteBuf byteBuf, LispAppDataLcafAddress address)
                throws LispWriterException {

            int lcafIndex = byteBuf.writerIndex();
            LispLcafAddress.serializeCommon(byteBuf, address);

            byte[] tos = getPartialByteArray(address.getIpTos());
            byteBuf.writeBytes(tos);
            byteBuf.writeByte(address.getProtocol());
            byteBuf.writeShort(address.getLocalPortLow());
            byteBuf.writeShort(address.getLocalPortHigh());
            byteBuf.writeShort(address.getRemotePortLow());
            byteBuf.writeShort(address.getRemotePortHigh());

            LispAfiAddress.AfiAddressWriter writer = new LispAfiAddress.AfiAddressWriter();
            writer.writeTo(byteBuf, address.getAddress());

            LispLcafAddress.updateLength(lcafIndex, byteBuf);
        }

        /**
         * An utility function that obtains byte array from partial int value.
         *
         * @param value integer value
         * @return an array of bytes
         */
        static byte[] getPartialByteArray(int value) {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            byte[] array = buffer.putInt(value).array();
            return Arrays.copyOfRange(array, 1, 4);
        }
    }
}
