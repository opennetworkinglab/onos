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
package org.onosproject.bgpio.types;

import java.net.InetAddress;
import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.Ip6Address;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.util.Validation;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

/**
 * Provides Implementation of IPv6AddressTlv.
 */
public class IPv6AddressTlv implements BgpValueType {
    private static final int LENGTH = 16;

    private final Ip6Address address;
    private short type;

    /**
     * Constructor to initialize parameters.
     *
     * @param address Ipv6 address of interface/neighbor
     * @param type address type
     */
    public IPv6AddressTlv(Ip6Address address, short type) {
        this.address = Preconditions.checkNotNull(address);
        this.type = type;
    }

    /**
     * Returns Ipv6 address of interface/neighbor.
     *
     * @return Ipv6 address of interface/neighbor
     */
    public Ip6Address address() {
        return address;
    }

    @Override
    public short getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IPv6AddressTlv) {
            IPv6AddressTlv other = (IPv6AddressTlv) obj;
            return Objects.equals(this.address, other.address) && Objects.equals(this.type, other.type);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeShort(type);
        cb.writeShort(LENGTH);
        cb.writeBytes(address.toOctets());
        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of IPv6AddressTlv.
     *
     * @param cb channelBuffer
     * @param type address type
     * @return object of IPv6AddressTlv
     * @throws BgpParseException while parsing IPv6AddressTlv
     */
    public static IPv6AddressTlv read(ChannelBuffer cb, short type) throws BgpParseException {
        InetAddress ipAddress = Validation.toInetAddress(LENGTH, cb);
        if (ipAddress.isMulticastAddress()) {
            throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR, (byte) 0, null);
        }
        Ip6Address address = Ip6Address.valueOf(ipAddress);
        return IPv6AddressTlv.of(address, type);
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param address Ipv6 interface/neighbor address
     * @param type says Ipv6 address of interface/neighbor tlv type
     * @return object of this class
     */
    public static IPv6AddressTlv of(final Ip6Address address, final short type) {
        return new IPv6AddressTlv(address, type);
    }

    @Override
    public int compareTo(Object o) {
        if (this.equals(o)) {
            return 0;
        }
        return ((Ip6Address) (this.address)).compareTo((Ip6Address) (((IPv6AddressTlv) o).address));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("type", type)
                .add("LENGTH", LENGTH)
                .add("address", address)
                .toString();
    }
}