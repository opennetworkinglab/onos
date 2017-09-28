/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onlab.packet.ipv6;

import org.onlab.packet.BasePacket;
import org.onlab.packet.Data;
import org.onlab.packet.Deserializer;
import org.onlab.packet.IPacket;
import org.onlab.packet.IPv6;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkHeaderLength;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Implements IPv6 routing extension header format. (RFC 2460)
 */
public class Routing extends BasePacket implements IExtensionHeader {
    public static final byte FIXED_HEADER_LENGTH = 4; // bytes
    public static final byte FIXED_ROUTING_DATA_LENGTH = 4; // bytes
    public static final byte LENGTH_UNIT = 8; // bytes per unit

    protected byte nextHeader;
    protected byte headerExtLength;
    protected byte routingType;
    protected byte segmentsLeft;
    protected byte[] routingData;

    @Override
    public byte getNextHeader() {
        return this.nextHeader;
    }

    @Override
    public Routing setNextHeader(final byte nextHeader) {
        this.nextHeader = nextHeader;
        return this;
    }

    /**
     * Gets the extension length of this header.
     *
     * @return header length
     */
    public byte getHeaderExtLength() {
        return this.headerExtLength;
    }

    /**
     * Sets the extension length of this header.
     *
     * @param headerExtLength the header length to set
     * @return this
     */
    public Routing setHeaderExtLength(final byte headerExtLength) {
        this.headerExtLength = headerExtLength;
        return this;
    }

    /**
     * Gets the routing type of this header.
     *
     * @return routing type
     */
    public byte getRoutingType() {
        return this.routingType;
    }

    /**
     * Sets the routing type of this header.
     *
     * @param routingType the routing type to set
     * @return this
     */
    public Routing setRoutingType(final byte routingType) {
        this.routingType = routingType;
        return this;
    }

    /**
     * Gets the number of remaining route segments of this header.
     *
     * @return number of remaining route segments
     */
    public byte getSegmentsLeft() {
        return this.segmentsLeft;
    }

    /**
     * Sets the number of remaining route segments of this header.
     *
     * @param segmentsLeft the number of remaining route segments to set
     * @return this
     */
    public Routing setSegmntsLeft(final byte segmentsLeft) {
        this.segmentsLeft = segmentsLeft;
        return this;
    }

    /**
     * Gets the routing data.
     *
     * @return the routing data
     */
    public byte[] getRoutingData() {
        return this.routingData;
    }

    /**
     * Sets the routing data.
     *
     * @param routingData the routing data to set
     * @return this
     */
    public Routing setRoutingData(final byte[] routingData) {
        this.routingData =
                Arrays.copyOfRange(routingData, 0, routingData.length);
        return this;
    }

    @Override
    public byte[] serialize() {
        byte[] payloadData = null;
        if (this.payload != null) {
            this.payload.setParent(this);
            payloadData = this.payload.serialize();
        }

        int headerLength = FIXED_HEADER_LENGTH + routingData.length;
        int payloadLength = 0;
        if (payloadData != null) {
            payloadLength = payloadData.length;
        }

        final byte[] data = new byte[headerLength + payloadLength];
        final ByteBuffer bb = ByteBuffer.wrap(data);

        bb.put(this.nextHeader);
        bb.put(this.headerExtLength);
        bb.put(this.routingType);
        bb.put(this.segmentsLeft);
        bb.put(this.routingData, 0, routingData.length);

        if (payloadData != null) {
            bb.put(payloadData);
        }

        if (this.parent != null && this.parent instanceof IExtensionHeader) {
            ((IExtensionHeader) this.parent).setNextHeader(IPv6.PROTOCOL_ROUTING);
        }
        return data;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 5807;
        int result = super.hashCode();
        result = prime * result + this.nextHeader;
        result = prime * result + this.headerExtLength;
        result = prime * result + this.routingType;
        result = prime * result + this.segmentsLeft;
        for (byte b : this.routingData) {
            result = prime * result + b;
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof Routing)) {
            return false;
        }
        final Routing other = (Routing) obj;
        if (this.nextHeader != other.nextHeader) {
            return false;
        }
        if (this.headerExtLength != other.headerExtLength) {
            return false;
        }
        if (this.routingType != other.routingType) {
            return false;
        }
        if (this.segmentsLeft != other.segmentsLeft) {
            return false;
        }
        if (!Arrays.equals(this.routingData, other.routingData)) {
            return false;
        }
        return true;
    }

    /**
     * Deserializer function for routing headers.
     *
     * @return deserializer function
     */
    public static Deserializer<Routing> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, FIXED_HEADER_LENGTH);

            Routing routing = new Routing();

            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            routing.nextHeader = bb.get();
            routing.headerExtLength = bb.get();
            routing.routingType = bb.get();
            routing.segmentsLeft = bb.get();
            int dataLength =
                    FIXED_ROUTING_DATA_LENGTH + LENGTH_UNIT * routing.headerExtLength;

            checkHeaderLength(bb.remaining(), dataLength);

            routing.routingData = new byte[dataLength];
            bb.get(routing.routingData, 0, dataLength);

            Deserializer<? extends IPacket> deserializer;
            if (IPv6.PROTOCOL_DESERIALIZER_MAP.containsKey(routing.nextHeader)) {
                deserializer = IPv6.PROTOCOL_DESERIALIZER_MAP.get(routing.nextHeader);
            } else {
                deserializer = new Data().deserializer();
            }
            routing.payload = deserializer.deserialize(data, bb.position(),
                                               bb.limit() - bb.position());
            routing.payload.setParent(routing);

            return routing;
        };
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("nextHeader", Byte.toString(nextHeader))
                .add("headerExtLength", Byte.toString(headerExtLength))
                .add("routingType", Byte.toString(routingType))
                .add("segmentsLeft", Byte.toString(segmentsLeft))
                .add("routingData", Arrays.toString(routingData))
                .toString();
    }
}
