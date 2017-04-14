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

package org.onosproject.routing.fpm.protocol;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.DeserializationException;

import java.nio.ByteBuffer;

import static org.onlab.packet.PacketUtils.checkInput;

/**
 * FPM header.
 */
public final class FpmHeader {
    public static final int FPM_HEADER_LENGTH = 4;

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
}
