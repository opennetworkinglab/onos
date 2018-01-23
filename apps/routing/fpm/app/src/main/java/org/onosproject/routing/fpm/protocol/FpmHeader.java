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

package org.onosproject.routing.fpm.protocol;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.DeserializationException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import java.nio.ByteBuffer;

import static org.onlab.packet.PacketUtils.checkInput;

/**
 * FPM header.
 */
public final class FpmHeader {
    public static final int FPM_HEADER_LENGTH = 4;
    public static final int FPM_MESSAGE_MAX_LENGTH = 4096;

    public static final short FPM_VERSION_1 = 1;
    public static final short FPM_VERSION_ONOS_EXT = 32;

    private static final ImmutableSet<Short> SUPPORTED_VERSIONS =
            ImmutableSet.<Short>builder()
            .add(FPM_VERSION_1)
            .add(FPM_VERSION_ONOS_EXT)
            .build();

    public static final short FPM_TYPE_NETLINK = 1;
    public static final short FPM_TYPE_PROTOBUF = 2;
    public static final short FPM_TYPE_KEEPALIVE = 32;

    private static final String VERSION_NOT_SUPPORTED = "FPM version not supported: ";
    private static final String TYPE_NOT_SUPPORTED = "FPM type not supported: ";

    private final short version;
    private final short type;
    private final int length;

    private final Netlink netlink;

    /**
     * Class constructor.
     *
     * @param version version
     * @param type type
     * @param length length
     * @param netlink netlink header
     */
    private FpmHeader(short version, short type, int length, Netlink netlink) {
        this.version = version;
        this.type = type;
        this.length = length;
        this.netlink = netlink;
    }

    /**
     * Returns the protocol version.
     *
     * @return protocol version
     */
    public short version() {
        return version;
    }

    /**
     * Returns the type.
     *
     * @return type
     */
    public short type() {
        return type;
    }

    /**
     * Returns the message length.
     *
     * @return message length
     */
    public int length() {
        return length;
    }

    /**
     * Returns the netlink header.
     *
     * @return netlink header
     */
    public Netlink netlink() {
        return netlink;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("version", version)
                .add("type", type)
                .add("length", length)
                .add("netlink", netlink)
                .toString();
    }

    /**
     * Decodes an FPM header from an input buffer.
     *
     * @param buffer input buffer
     * @param start starting position the FPM header
     * @param length length of the message
     * @return FPM header
     * @throws DeserializationException if an FPM header could not be decoded
     * from the input buffer
     */
    public static FpmHeader decode(byte[] buffer, int start, int length) throws
            DeserializationException {
        checkInput(buffer, start, length, FPM_HEADER_LENGTH);

        ByteBuffer bb = ByteBuffer.wrap(buffer, start, length);

        short version = bb.get();
        if (!SUPPORTED_VERSIONS.contains(version)) {
            throw new DeserializationException(VERSION_NOT_SUPPORTED + version);
        }

        short type = bb.get();
        int messageLength = bb.getShort();

        if (type == FPM_TYPE_KEEPALIVE) {
            return new FpmHeader(version, type, messageLength, null);
        }

        if (type != FPM_TYPE_NETLINK) {
            throw new DeserializationException(TYPE_NOT_SUPPORTED + type);
        }

        Netlink netlink = Netlink.decode(buffer, bb.position(), bb.limit() - bb.position());

        return new FpmHeader(version, type, messageLength, netlink);
    }

    /**
     * Encode the FpmHeader contents into a ChannelBuffer.
     *
     * @return filled in ChannelBuffer
     */
   public ChannelBuffer encode() {

        ChannelBuffer cb = ChannelBuffers.buffer(FPM_MESSAGE_MAX_LENGTH);

        cb.writeByte(version);
        cb.writeByte(type);
        cb.writeShort(length);

        netlink.encode(cb);
        return cb;
    }

    /**
     * Returns a new FpmHeader builder.
     *
     * @return FpmHeader builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * FpmHeader Builder.
     */
    public static final class Builder {

        private short version = FPM_VERSION_1;
        private short type = FPM_TYPE_NETLINK;
        private int length = 0;
        private Netlink netlink = null;

        /**
        * Hide class constructor.
        */
        private Builder() {
        }

        /**
         * Sets version for the FpmHeader that will be built.
         *
         * @param version to use for built FpmHeader
         * @return this builder
         */
        public Builder version(short version) {
            this.version = version;
            return this;
        }

        /**
         * Sets type for the FpmHeader that will be built.
         *
         * @param type to use for built FpmHeader
         * @return this builder
         */
        public Builder type(short type) {
            this.type = type;
            return this;
        }

        /**
         * Sets length for the FpmHeader that will be built.
         *
         * @param length to use for built FpmHeader
         * @return this builder
         */
        public Builder length(int length) {
            this.length = length;
            return this;
        }

        /**
         * Sets netlink for the FpmHeader that will be built.
         *
         * @param netlink to use for built FpmHeader
         * @return this builder
         */
        public Builder netlink(Netlink netlink) {
            this.netlink = netlink;
            return this;
        }

        /**
         * Builds the FpmHeader.
         *
         * @return FpmHeader reference
         */
        public FpmHeader build() {
            return new FpmHeader(version, type, length, netlink);
        }
    }
}
