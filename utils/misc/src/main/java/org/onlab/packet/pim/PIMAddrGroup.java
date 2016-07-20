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
import org.onlab.packet.IpPrefix;
import org.onlab.packet.PIM;


import java.nio.ByteBuffer;

import static org.onlab.packet.PacketUtils.checkInput;

public class PIMAddrGroup {
    private byte family;
    private byte encType;
    private byte reserved;
    private boolean bBit;
    private boolean zBit;
    private byte masklen;
    IpAddress addr;

    public static final int ENC_GROUP_IPV4_BYTE_LENGTH = 4 + Ip4Address.BYTE_LENGTH;
    public static final int ENC_GROUP_IPV6_BYTE_LENGTH = 4 + Ip6Address.BYTE_LENGTH;

    /**
     * PIM Encoded Group Address.
     */
    public PIMAddrGroup() {
        this.family = PIM.ADDRESS_FAMILY_IP4;
        this.encType = 0;
        this.reserved = 0;
        this.bBit = false;
        this.zBit = false;
    }

    /**
     * PIM Encoded Source Address.
     *
     * @param addr IPv4 or IPv6
     */
    public PIMAddrGroup(String addr) {
        this.setAddr(addr);
    }

    /**
     * PIM Encoded Group Address.
     *
     * @param gpfx PIM encoded group address.
     */
    public PIMAddrGroup(IpPrefix gpfx) {
        this.setAddr(gpfx);
    }

    /**
     * PIM encoded source address.
     *
     * @param addr IPv4 or IPv6
     */
    public void setAddr(String addr) {
        setAddr(IpPrefix.valueOf(addr));
    }

    /**
     * Set the encoded source address.
     *
     * @param pfx address prefix
     */
    public void setAddr(IpPrefix pfx) {
        this.addr = pfx.address();
        this.masklen = (byte) pfx.prefixLength();
        this.family = (byte) ((this.addr.isIp4()) ? PIM.ADDRESS_FAMILY_IP4 : PIM.ADDRESS_FAMILY_IP6);
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
     * Get the address of this encoded address.
     *
     * @return source address
     */
    public IpAddress getAddr() {
        return this.addr;
    }

    /**
     * Get the masklen of the group address.
     *
     * @return the masklen
     */
    public int getMasklen() {
        return this.masklen;
    }

    /**
     * Return the z bit for admin scoping. Only used for the Bootstrap router.
     *
     * @return true or false
     */
    public boolean getZBit() {
        return this.zBit;
    }

    /**
     * Return the bBit. Used to indicate this is a bidir
     *
     * @return return true or false.
     */
    public boolean getBBit() {
        return this.bBit;
    }

    /**
     * The size in bytes of a serialized address.
     *
     * @return the number of bytes when serialized
     */
    public int getByteSize() {
        int size = 4;
        size += addr.isIp4() ? 4 : 16;
        return size;
    }

    /**
     * Serialize this group address.
     *
     * @return the serialized address in a buffer
     */
    public byte[] serialize() {
        int len = getByteSize();

        final byte[] data = new byte[len];
        final ByteBuffer bb = ByteBuffer.wrap(data);

        bb.put(this.family);
        bb.put(this.encType);

        // Todo: technically we should be setting the B and Z bits, but we'll never use them.
        bb.put(reserved);

        bb.put(this.masklen);
        bb.put(this.addr.toOctets());
        return data;
    }

    /**
     * Deserialze from a ByteBuffer.
     *
     * @param bb the ByteBuffer
     * @return an encoded PIM group address
     * @throws DeserializationException if unable to deserialize the packet data
     */
    public PIMAddrGroup deserialize(ByteBuffer bb) throws DeserializationException {

        /*
         * We need to verify that we have enough buffer space.  First we'll assume that
         * we are decoding an IPv4 address.  After we read the first by (address family),
         * we'll determine if we actually need more buffer space for an IPv6 address.
         */
        checkInput(bb.array(), bb.position(), bb.limit() - bb.position(), ENC_GROUP_IPV4_BYTE_LENGTH);

        this.family = bb.get();
        if (family != PIM.ADDRESS_FAMILY_IP4 && family != PIM.ADDRESS_FAMILY_IP6) {
            throw new DeserializationException("Illegal IP version number: " + family + "\n");
        } else if (family == PIM.ADDRESS_FAMILY_IP6) {

            // Check for one less by since we have already read the first byte of the packet.
            checkInput(bb.array(), bb.position(), bb.limit() - bb.position(), ENC_GROUP_IPV6_BYTE_LENGTH - 1);
        }

        this.encType = bb.get();
        this.reserved = bb.get();
        if ((this.reserved & 0x80) != 0) {
            this.bBit = true;
        }
        if ((this.reserved & 0x01) != 0) {
            this.zBit = true;
        }
        // Remove the z and b bits from reserved
        this.reserved |= 0x7d;

        this.masklen = bb.get();
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
        result = prime * result + this.reserved;
        result = prime * result + this.masklen;
        result = prime * result + this.addr.hashCode();
        return result;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals()
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PIMAddrGroup)) {
            return false;
        }
        final PIMAddrGroup other = (PIMAddrGroup) obj;
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
