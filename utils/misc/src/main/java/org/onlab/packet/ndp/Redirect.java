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
package org.onlab.packet.ndp;

import org.onlab.packet.BasePacket;
import org.onlab.packet.Deserializer;
import org.onlab.packet.IPacket;
import org.onlab.packet.Ip6Address;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Implements ICMPv6 Redirect packet format. (RFC 4861)
 */
public class Redirect extends BasePacket {
    public static final byte HEADER_LENGTH = 36; // bytes

    protected byte[] targetAddress = new byte[Ip6Address.BYTE_LENGTH];
    protected byte[] destinationAddress = new byte[Ip6Address.BYTE_LENGTH];

    private final NeighborDiscoveryOptions options =
        new NeighborDiscoveryOptions();

    /**
     * Gets target address.
     *
     * @return the target IPv6 address
     */
    public byte[] getTargetAddress() {
        return this.targetAddress;
    }

    /**
     * Sets target address.
     *
     * @param targetAddress the target IPv6 address to set
     * @return this
     */
    public Redirect setTargetAddress(final byte[] targetAddress) {
        this.targetAddress =
            Arrays.copyOfRange(targetAddress, 0, Ip6Address.BYTE_LENGTH);
        return this;
    }

    /**
     * Gets destination address.
     *
     * @return the destination IPv6 address
     */
    public byte[] getDestinationAddress() {
        return this.destinationAddress;
    }

    /**
     * Sets destination address.
     *
     * @param destinationAddress the destination IPv6 address to set
     * @return this
     */
    public Redirect setDestinationAddress(final byte[] destinationAddress) {
        this.destinationAddress =
            Arrays.copyOfRange(destinationAddress, 0, Ip6Address.BYTE_LENGTH);
        return this;
    }

    /**
     * Gets the Neighbor Discovery Protocol packet options.
     *
     * @return the Neighbor Discovery Protocol packet options
     */
    public List<NeighborDiscoveryOptions.Option> getOptions() {
        return this.options.options();
    }

    /**
     * Adds a Neighbor Discovery Protocol packet option.
     *
     * @param type the option type
     * @param data the option data
     * @return this
     */
    public Redirect addOption(final byte type, final byte[] data) {
        this.options.addOption(type, data);
        return this;
    }

    @Override
    public byte[] serialize() {
        byte[] optionsData = null;
        if (this.options.hasOptions()) {
            optionsData = this.options.serialize();
        }

        int optionsLength = 0;
        if (optionsData != null) {
            optionsLength = optionsData.length;
        }

        final byte[] data = new byte[HEADER_LENGTH + optionsLength];
        final ByteBuffer bb = ByteBuffer.wrap(data);

        bb.putInt(0);
        bb.put(this.targetAddress, 0, Ip6Address.BYTE_LENGTH);
        bb.put(this.destinationAddress, 0, Ip6Address.BYTE_LENGTH);
        if (optionsData != null) {
            bb.put(optionsData);
        }

        return data;
    }

    @Override
    public IPacket deserialize(byte[] data, int offset, int length) {
        final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

        bb.getInt();
        bb.get(this.targetAddress, 0, Ip6Address.BYTE_LENGTH);
        bb.get(this.destinationAddress, 0, Ip6Address.BYTE_LENGTH);

        this.options.deserialize(data, bb.position(),
                                 bb.limit() - bb.position());

        return this;
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
        ByteBuffer bb;
        bb = ByteBuffer.wrap(this.targetAddress);
        for (int i = 0; i < this.targetAddress.length / 4; i++) {
            result = prime * result + bb.getInt();
        }
        bb = ByteBuffer.wrap(this.destinationAddress);
        for (int i = 0; i < this.destinationAddress.length / 4; i++) {
            result = prime * result + bb.getInt();
        }
        result = prime * result + this.options.hashCode();
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
        if (!(obj instanceof Redirect)) {
            return false;
        }
        final Redirect other = (Redirect) obj;
        if (!Arrays.equals(this.targetAddress, other.targetAddress)) {
            return false;
        }
        if (!Arrays.equals(this.destinationAddress,
                           other.destinationAddress)) {
            return false;
        }
        if (!this.options.equals(other.options)) {
            return false;
        }
        return true;
    }

    /**
     * Deserializer function for redirect packets.
     *
     * @return deserializer function
     */
    public static Deserializer<Redirect> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, HEADER_LENGTH);

            Redirect redirect = new Redirect();

            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

            bb.getInt();

            bb.get(redirect.targetAddress, 0, Ip6Address.BYTE_LENGTH);
            bb.get(redirect.destinationAddress, 0, Ip6Address.BYTE_LENGTH);

            if (bb.limit() - bb.position() > 0) {
                NeighborDiscoveryOptions options = NeighborDiscoveryOptions.deserializer()
                        .deserialize(data, bb.position(), bb.limit() - bb.position());

                for (NeighborDiscoveryOptions.Option option : options.options()) {
                    redirect.addOption(option.type(), option.data());
                }
            }

            return redirect;
        };
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("targetAddress", Arrays.toString(targetAddress))
                .add("destinationAddress", Arrays.toString(destinationAddress))
                .toString();
    }
}
