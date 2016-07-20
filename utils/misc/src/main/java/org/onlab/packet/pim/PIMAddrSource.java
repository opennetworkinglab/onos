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

public class PIMAddrSource {
    private byte family;
    private byte encType;
    private byte reserved;
    private boolean sBit;
    private boolean wBit;
    private boolean rBit;
    private byte masklen;
    IpAddress addr;

    public static final int ENC_SOURCE_IPV4_BYTE_LENGTH = 4 + Ip4Address.BYTE_LENGTH;
    public static final int ENC_SOURCE_IPV6_BYTE_LENGTH = 4 + Ip6Address.BYTE_LENGTH;

    /**
     * PIM Encoded Source Address.
     *
     * @param addr IPv4 or IPv6
     */
    public PIMAddrSource(String addr) {
        this.init();
        this.setAddr(addr);
    }

    /**
     * PIM Encoded Source Address.
     *
     * @param spfx IPv4 or IPv6
     */
    public PIMAddrSource(IpPrefix spfx) {
        this.init();
        this.setAddr(spfx);
    }

    /**
     * PIM Encoded Group Address.
     */
    public PIMAddrSource() {
        this.init();
    }

    private void init() {
        this.family = PIM.ADDRESS_FAMILY_IP4;
        this.encType = 0;
        this.reserved = 0;
        this.sBit = true;
        this.wBit = false;
        this.rBit = false;
    }

    /**
     * PIM Encoded Source Address.
     *
     * @param addr IPv4 or IPv6
     */
    public void setAddr(String addr) {
        IpPrefix spfx = IpPrefix.valueOf(addr);
        setAddr(spfx);
    }

    /**
     * PIM Encoded Source Address.
     *
     * @param spfx IPv4 or IPv6 address prefix
     */
    public void setAddr(IpPrefix spfx) {
        this.addr = spfx.address();
        this.masklen = (byte) spfx.prefixLength();
        this.family = (byte) ((this.addr.isIp4()) ? PIM.ADDRESS_FAMILY_IP4 : PIM.ADDRESS_FAMILY_IP6);
    }

    /**
     * Get the IP family of this address: 4 or 6.
     *
     * @return the IP address family
     */
    public byte getFamily() {
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
     * Return the sparse bit.
     *
     * @return true or false
     */
    public boolean getSBit() {
        return this.sBit;
    }

    /**
     * Return the wBit, used in Join/Prune messages.
     *
     * @return return true or false.
     */
    public boolean getWBit() {
        return this.wBit;
    }

    /**
     * Return the rBit. Used by Rendezvous Point.
     *
     * @return the rBit.
     */
    public boolean getRBit() {
        return this.rBit;
    }

    /**
     * The size in bytes of a serialized address.
     *
     * @return the number of bytes when serialized
     */
    public int getByteSize() {
        int size = 4;
        size += addr.isIp4() ? PIM.ADDRESS_FAMILY_IP4 : PIM.ADDRESS_FAMILY_IP6;
        return size;
    }

    public byte[] serialize() {
        int len = addr.isIp4() ? ENC_SOURCE_IPV4_BYTE_LENGTH : ENC_SOURCE_IPV6_BYTE_LENGTH;

        final byte[] data = new byte[len];
        final ByteBuffer bb = ByteBuffer.wrap(data);

        bb.put(this.family);
        bb.put(this.encType);

        // Todo: technically we should be setting the B and Z bits, but we'll never use them.
        byte mask = 0x0;
        if (this.sBit) {
            this.reserved |= 0x4;
        }
        if (this.wBit) {
            this.reserved |= 0x2;
        }
        if (this.rBit) {
            this.reserved |= 0x1;
        }
        bb.put(reserved);

        bb.put(this.masklen);
        bb.put(this.addr.toOctets());
        return data;
    }

    public PIMAddrSource deserialize(byte[] data, int offset, int length) throws DeserializationException {
        final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
        return deserialize(bb);
    }

    public PIMAddrSource deserialize(ByteBuffer bb) throws DeserializationException {

        /*
         * We need to verify that we have enough buffer space.  First we'll assume that
         * we are decoding an IPv4 address.  After we read the first by (address family),
         * we'll determine if we actually need more buffer space for an IPv6 address.
         */
        checkInput(bb.array(), bb.position(), bb.limit() - bb.position(), ENC_SOURCE_IPV4_BYTE_LENGTH);

        this.family = bb.get();
        if (family != PIM.ADDRESS_FAMILY_IP4 && family != PIM.ADDRESS_FAMILY_IP6) {
            throw new DeserializationException("Illegal IP version number: " + family + "\n");
        } else if (family == PIM.ADDRESS_FAMILY_IP6) {

            // Check for one less by since we have already read the first byte of the packet.
            checkInput(bb.array(), bb.position(), bb.limit() - bb.position(), ENC_SOURCE_IPV6_BYTE_LENGTH - 1);
        }

        this.encType = bb.get();
        this.reserved = bb.get();
        if ((this.reserved & 0x01) != 0) {
            this.rBit = true;
        }
        if ((this.reserved & 0x02) != 0) {
            this.wBit = true;
        }
        if ((this.reserved & 0x4) != 0) {
            this.sBit = true;
        }

        // Remove the s, reserved
        this.reserved &= 0xf8;

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
     * @see java.lang.Object#hashCode()
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PIMAddrSource)) {
            return false;
        }
        final PIMAddrSource other = (PIMAddrSource) obj;
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
