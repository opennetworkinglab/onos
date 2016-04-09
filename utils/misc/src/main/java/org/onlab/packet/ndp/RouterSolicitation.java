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

import java.nio.ByteBuffer;
import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Implements ICMPv6 Router Solicitation packet format. (RFC 4861)
 */
public class RouterSolicitation extends BasePacket {
    public static final byte HEADER_LENGTH = 4; // bytes

    private final NeighborDiscoveryOptions options = new NeighborDiscoveryOptions();

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
    public RouterSolicitation addOption(final byte type, final byte[] data) {
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

        if (optionsData != null) {
            bb.put(optionsData);
        }

        return data;
    }

    @Override
    public IPacket deserialize(byte[] data, int offset, int length) {
        final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

        bb.getInt();

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
        if (!(obj instanceof RouterSolicitation)) {
            return false;
        }
        final RouterSolicitation other = (RouterSolicitation) obj;
        if (!this.options.equals(other.options)) {
            return false;
        }
        return true;
    }

    /**
     * Deserializer function for router solicitation packets.
     *
     * @return deserializer function
     */
    public static Deserializer<RouterSolicitation> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, HEADER_LENGTH);

            RouterSolicitation routerSolicitation = new RouterSolicitation();

            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

            bb.getInt();

            if (bb.limit() - bb.position() > 0) {
                NeighborDiscoveryOptions options = NeighborDiscoveryOptions.deserializer()
                        .deserialize(data, bb.position(), bb.limit() - bb.position());

                for (NeighborDiscoveryOptions.Option option : options.options()) {
                    routerSolicitation.addOption(option.type(), option.data());
                }
            }

            return routerSolicitation;
        };
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .toString();
        // TODO: need to handle options
    }
}
