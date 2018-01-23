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
import com.google.common.collect.ImmutableList;
import org.onlab.packet.DeserializationException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.jboss.netty.buffer.ChannelBuffer;

import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Netlink routing message (rtnetlink).
 * <p>
 * Taken from struct rtmsg in linux/rtnetlink.h
 * </p>
 */
public final class RtNetlink {

    public static final int RT_ADDRESS_FAMILY_INET = 2;
    public static final int RT_ADDRESS_FAMILY_INET6 = 10;
    public static final int RT_NETLINK_LENGTH = 12;

    private static final int MASK = 0xff;

    private final short addressFamily;
    private final int dstLength;
    private final int srcLength;
    private final short tos;
    private final short table;
    private final RtProtocol protocol;
    private final short scope;
    private final short type;
    private final long flags;

    private final List<RouteAttribute> attributes;

    /**
     * Class constructor.
     *
     * @param addressFamily address family
     * @param dstLength destination address length
     * @param srcLength source address length
     * @param tos type of service
     * @param table routing table
     * @param protocol protocol
     * @param scope scope
     * @param type type
     * @param flags flags
     * @param attributes list of attributes
     */
    private RtNetlink(short addressFamily,
                      int dstLength,
                      int srcLength,
                      short tos,
                      short table,
                      RtProtocol protocol,
                      short scope,
                      short type,
                      long flags,
                      List<RouteAttribute> attributes) {

        this.addressFamily = addressFamily;
        this.dstLength = dstLength;
        this.srcLength = srcLength;
        this.tos = tos;
        this.table = table;
        this.protocol = protocol;
        this.scope = scope;
        this.type = type;
        this.flags = flags;

        this.attributes = ImmutableList.copyOf(attributes);

    }

    /**
     * Returns the address family of the route.
     *
     * @return address family
     */
    public short addressFamily() {
        return addressFamily;
    }

    /**
     * Returns the destination address length.
     *
     * @return destination address length
     */
    public int dstLength() {
        return dstLength;
    }

    /**
     * Returns the source address length.
     *
     * @return source address length
     */
    public int srcLength() {
        return srcLength;
    }

    /**
     * Returns the type of service.
     *
     * @return type of service
     */
    public short tos() {
        return tos;
    }

    /**
     * Returns the routing table.
     *
     * @return routing table
     */
    public short table() {
        return table;
    }

    /**
     * Returns the protocol.
     *
     * @return protocol
     */
    public RtProtocol protocol() {
        return protocol;
    }

    /**
     * Returns the route scope.
     *
     * @return scope
     */
    public short scope() {
        return scope;
    }

    /**
     * Returns the route type.
     *
     * @return route type
     */
    public short type() {
        return type;
    }

    /**
     * Returns the route flags.
     *
     * @return route flags
     */
    public long flags() {
        return flags;
    }

    /**
     * Returns the list of route attributes.
     *
     * @return route attributes
     */
    public List<RouteAttribute> attributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("addressFamily", addressFamily)
                .add("dstLength", dstLength)
                .add("srcLength", srcLength)
                .add("tos", tos)
                .add("table", table)
                .add("protocol", protocol)
                .add("scope", scope)
                .add("type", type)
                .add("flags", flags)
                .add("attributes", attributes)
                .toString();
    }

    /**
     * Decodes an rtnetlink message from an input buffer.
     *
     * @param buffer input buffer
     * @param start starting position the rtnetlink message
     * @param length length of the message
     * @return rtnetlink message
     * @throws DeserializationException if an rtnetlink message could not be
     * decoded from the input buffer
     */
    public static RtNetlink decode(byte[] buffer, int start, int length)
            throws DeserializationException {
        checkInput(buffer, start, length, RT_NETLINK_LENGTH);

        ByteBuffer bb = ByteBuffer.wrap(buffer, start, length);

        short addressFamily = (short) (bb.get() & MASK);
        int dstLength = bb.get() & MASK;
        int srcLength = bb.get() & MASK;
        short tos = (short) (bb.get() & MASK);
        short table = (short) (bb.get() & MASK);
        short protocol = (short) (bb.get() & MASK);
        short scope = (short) (bb.get() & MASK);
        short type = (short) (bb.get() & MASK);
        long flags = Integer.reverseBytes(bb.getInt());
        List<RouteAttribute> attributes = new ArrayList<>();

        RtProtocol rtProtocol = RtProtocol.get(protocol);

        while (bb.hasRemaining()) {
            RouteAttribute attribute = RouteAttribute.decode(buffer, bb.position(),
                    bb.limit() - bb.position());
            attributes.add(attribute);
            bb.position(bb.position() + attribute.length());
        }

        return new RtNetlink(
                addressFamily,
                dstLength,
                srcLength,
                tos,
                table,
                rtProtocol,
                scope,
                type,
                flags,
                attributes);
    }


    /**
     * Encode the RtNetlink contents into the ChannelBuffer.
     *
     * @param cb channelbuffer to be filled in
     */
    public void encode(ChannelBuffer cb) {

        cb.writeByte(addressFamily);
        cb.writeByte(dstLength);
        cb.writeByte(srcLength);
        cb.writeByte(tos);
        cb.writeByte(table);
        cb.writeByte(protocol.value());
        cb.writeByte(scope);
        cb.writeByte(type);
        cb.writeInt(Integer.reverseBytes((int) flags));

        for (RouteAttribute attribute : attributes()) {
            attribute.encode(cb);
        }
    }

    /**
     * Returns a new RtNetlink builder.
     *
     * @return RtNetlink builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * RtNetlink Builder.
     */
    public static final class Builder {

        private short addressFamily = RT_ADDRESS_FAMILY_INET;
        private int dstLength = 0;
        private int srcLength = 0;
        private short tos = 0;
        private short table = 0;
        private RtProtocol protocol = RtProtocol.ZEBRA;
        private short scope = 0;
        private short type = FpmHeader.FPM_TYPE_NETLINK;
        private long flags = 0;
        private List<RouteAttribute> attributes = new ArrayList<>();

        /**
        * Hide class constructor.
        */
        private Builder() {
        }

        /**
         * Sets addressFamily for the RtNetlink that will be built.
         *
         * @param addressFamily to use for built RtNetlink
         * @return this builder
         */
        public Builder addressFamily(short addressFamily) {
            this.addressFamily = addressFamily;
            return this;
        }

        /**
         * Sets dstLength for the RtNetlink that will be built.
         *
         * @param dstLength to use for built RtNetlink
         * @return this builder
         */
        public Builder dstLength(int dstLength) {
            this.dstLength = dstLength;
            return this;
        }

        /**
         * Sets srcLength for the RtNetlink that will be built.
         *
         * @param srcLength to use for built RtNetlink
         * @return this builder
         */
        public Builder srcLength(int srcLength) {
            this.srcLength = srcLength;
            return this;
        }

        /**
         * Sets tos for the RtNetlink that will be built.
         *
         * @param tos to use for built RtNetlink
         * @return this builder
         */
        public Builder tos(short tos) {
            this.tos = tos;
            return this;
        }

        /**
         * Sets table for the RtNetlink that will be built.
         *
         * @param table to use for built RtNetlink
         * @return this builder
         */
        public Builder table(short table) {
            this.table = table;
            return this;
        }

        /**
         * Sets protocol for the RtNetlink that will be built.
         *
         * @param protocol to use for built RtNetlink
         * @return this builder
         */
        public Builder protocol(RtProtocol protocol) {
            this.protocol = protocol;
            return this;
        }

        /**
         * Sets scope for the RtNetlink that will be built.
         *
         * @param scope to use for built RtNetlink
         * @return this builder
         */
        public Builder scope(short scope) {
            this.scope = scope;
            return this;
        }

        /**
         * Sets type for the RtNetlink that will be built.
         *
         * @param type to use for built RtNetlink
         * @return this builder
         */
        public Builder type(short type) {
            this.type = type;
            return this;
        }

        /**
         * Sets flags for the RtNetlink that will be built.
         *
         * @param flags to use for built RtNetlink
         * @return this builder
         */
        public Builder flags(long flags) {
            this.flags = flags;
            return this;
        }

        /**
         * Adds attribute for the RtNetlink that will be built.
         *
         * @param ra to add to list of route attributes
         * @return this builder
         */
        public Builder routeAttribute(RouteAttribute ra) {
            attributes.add(ra);
            return this;
        }

        /**
         * Builds the RtNetlink.
         *
         * @return RtNetlink reference
         */
        public RtNetlink build() {
            return new RtNetlink(addressFamily,
                                 dstLength,
                                 srcLength,
                                 tos,
                                 table,
                                 protocol,
                                 scope,
                                 type,
                                 flags,
                                 attributes);
        }
    }
}
