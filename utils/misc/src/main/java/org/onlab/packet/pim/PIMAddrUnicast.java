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
package org.onlab.packet.pim;



import org.onlab.packet.DeserializationException;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.PIM;

import java.nio.ByteBuffer;

import static org.onlab.packet.PacketUtils.checkInput;

public class PIMAddrUnicast {
    private byte family;
    private byte encType;
    IpAddress addr;

    public static final int ENC_UNICAST_IPV4_BYTE_LENGTH = 2 + Ip4Address.BYTE_LENGTH;
    public static final int ENC_UNICAST_IPV6_BYTE_LENGTH = 2 + Ip6Address.BYTE_LENGTH;

    /**
     * PIM Encoded Source Address.
     */
    public PIMAddrUnicast() {
        this.family = PIM.ADDRESS_FAMILY_IP4;
        this.encType = 0;
    }

    /**
     * PIM Encoded Source Address.
     *
     * @param addr IPv4 or IPv6
     */
    public PIMAddrUnicast(String addr) {
        this.addr = IpAddress.valueOf(addr);
        if (this.addr.isIp4()) {
            this.family = PIM.ADDRESS_FAMILY_IP4;
        } else {
            this.family = PIM.ADDRESS_FAMILY_IP6;
        }
        this.encType = 0;
    }

    /**
     * PIM Encoded Source Address.
     *
     * @param addr IPv4 or IPv6
     */
    public void setAddr(IpAddress addr) {
        this.addr = addr;
        if (this.addr.isIp4()) {
            this.family = PIM.ADDRESS_FAMILY_IP4;
        } else {
            this.family = PIM.ADDRESS_FAMILY_IP6;
        }
    }

    /**
     * Get the address of this encoded address.
     *
     * @return source address
     */
    public IpAddress getAddr() {
        return this.addr;
    }

    /**
     * Get the IP family of this address: 4 or 6.
     *
     * @return the IP address family
     */
    public int getFamily() {
        return this.family;
    }

    /**
     * The size in bytes of a serialized address.
     *
     * @return the number of bytes when serialized
     */
    public int getByteSize() {
        int size = 2;
        if (addr != null) {
            size += addr.isIp4() ? 4 : 16;
        } else {
            size += 4;
        }
        return size;
    }

    public byte[] serialize() {
        int len = getByteSize();

        final byte[] data = new byte[len];
        final ByteBuffer bb = ByteBuffer.wrap(data);

        bb.put(family);
        bb.put(encType);
        bb.put(addr.toOctets());
        return data;
    }

    public PIMAddrUnicast deserialize(ByteBuffer bb) throws DeserializationException {

        // Assume IPv4 for check length until we read the encoded family.
        checkInput(bb.array(), bb.position(), bb.limit() - bb.position(), ENC_UNICAST_IPV4_BYTE_LENGTH);
        this.family = bb.get();

        // If we have IPv6 we need to ensure we have adequate buffer space.
        if (this.family != PIM.ADDRESS_FAMILY_IP4 && this.family != PIM.ADDRESS_FAMILY_IP6) {
            throw new DeserializationException("Invalid address family: " + this.family);
        } else if (this.family == PIM.ADDRESS_FAMILY_IP6) {
            // Subtract -1 from ENC_UNICAST_IPv6 BYTE_LENGTH because we read one byte for family previously.
            checkInput(bb.array(), bb.position(), bb.limit() - bb.position(), ENC_UNICAST_IPV6_BYTE_LENGTH - 1);
        }

        this.encType = bb.get();
        if (this.family == PIM.ADDRESS_FAMILY_IP4) {
            this.addr = IpAddress.valueOf(bb.getInt());
        } else if (this.family == PIM.ADDRESS_FAMILY_IP6) {
            this.addr = Ip6Address.valueOf(bb.array(), 2);
        }
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 2521;
        int result = super.hashCode();
        result = prime * result + this.family;
        result = prime * result + this.encType;
        result = prime * result + this.addr.hashCode();
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PIMAddrUnicast)) {
            return false;
        }
        final PIMAddrUnicast other = (PIMAddrUnicast) obj;
        if (this.family != other.family) {
            return false;
        }

        if (this.encType != other.encType) {
            return false;
        }

        if (!this.addr.equals(other.addr)) {
            return false;
        }
        return true;
    }
}
