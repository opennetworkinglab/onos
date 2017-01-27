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
import org.onlab.packet.DeserializationException;

import java.nio.ByteBuffer;

import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Netlink header.
 * <p>
 * Taken from struct nlmsghdr in linux/netlink.h
 * </p>
 */
public final class Netlink {

    public static final int NETLINK_HEADER_LENGTH = 16;

    private final long length;
    private final NetlinkMessageType type;
    private final int flags;
    private final long sequence;
    private final long processPortId;

    private final RtNetlink rtNetlink;

    /**
     * Class constructor.
     *
     * @param length message length
     * @param type type
     * @param flags flags
     * @param sequence sequence number
     * @param processPortId port ID
     * @param rtNetlink netlink routing message
     */
    private Netlink(long length, NetlinkMessageType type, int flags, long sequence,
                    long processPortId, RtNetlink rtNetlink) {
        this.length = length;
        this.type = type;
        this.flags = flags;
        this.sequence = sequence;
        this.processPortId = processPortId;
        this.rtNetlink = rtNetlink;
    }

    /**
     * Returns the message length.
     *
     * @return length
     */
    public long length() {
        return length;
    }

    /**
     * Returns the message type.
     *
     * @return message type
     */
    public NetlinkMessageType type() {
        return type;
    }

    /**
     * Returns the flags.
     *
     * @return flags
     */
    public int flags() {
        return flags;
    }

    /**
     * Returns the sequence number.
     *
     * @return sequence number
     */
    public long sequence() {
        return sequence;
    }

    /**
     * Returns the port ID.
     *
     * @return port ID
     */
    public long processPortId() {
        return processPortId;
    }

    /**
     * Returns the netlink routing message.
     *
     * @return netlink routing message
     */
    public RtNetlink rtNetlink() {
        return rtNetlink;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("length", length)
                .add("type", type)
                .add("flags", flags)
                .add("sequence", sequence)
                .add("processPortId", processPortId)
                .add("rtNetlink", rtNetlink)
                .toString();
    }

    /**
     * Decodes a netlink header from an input buffer.
     *
     * @param buffer input buffer
     * @param start starting position the netlink header
     * @param length length of the message
     * @return netlink header
     * @throws DeserializationException if a netlink header could not be
     * decoded from the input buffer
     */
    public static Netlink decode(byte[] buffer, int start, int length) throws
            DeserializationException {
        checkInput(buffer, start, length, NETLINK_HEADER_LENGTH);

        ByteBuffer bb = ByteBuffer.wrap(buffer, start, length);

        long messageLength = Integer.reverseBytes(bb.getInt());
        int type = Short.reverseBytes(bb.getShort());
        int flags = Short.reverseBytes(bb.getShort());
        long sequence = Integer.reverseBytes(bb.getInt());
        long processPortId = Integer.reverseBytes(bb.getInt());

        NetlinkMessageType messageType = NetlinkMessageType.get(type);
        if (messageType == null) {
            throw new DeserializationException(
                    "Unsupported Netlink message type: " + type);
        }

        // Netlink messages from Quagga's FPM protocol are always in the
        // netlink route family (family 0).
        RtNetlink rtNetlink = RtNetlink.decode(buffer, bb.position(),
                bb.limit() - bb.position());

        return new Netlink(messageLength,
                messageType,
                flags,
                sequence,
                processPortId,
                rtNetlink);
    }

}
