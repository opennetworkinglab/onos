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
package org.onlab.packet;

import java.nio.ByteBuffer;

public class IGMPQuery extends IGMPGroup {

    // Bits and bytes after the group address
    private byte resv = 0;
    private boolean sbit = false;
    private byte qrv = 2;
    private byte qqic = 0x7d;

    /**
     * Create IGMP Query message.
     *
     * @param gaddr initiaze with a group address.
     * @param auxInfo auxillary info.
     */
    public IGMPQuery(IpAddress gaddr, int auxInfo) {
        super(gaddr, auxInfo);
    }

    /**
     * Create IGMP Query message.
     */
    public IGMPQuery() {
        super();
    }

    /**
     * Is the S flag set?  Telling adjacent routers to suppress normal timer updates.
     *
     * @return true if the flag is set, false otherwise
     */
    public boolean isSbit() {
        return sbit;
    }

    /**
     * Set the S flag.  Default is false.
     *
     * @param sbit true or false
     */
    public void setSbit(boolean sbit) {
        this.sbit = sbit;
    }

    /**
     * Get the Querier Robustness Variable.
     *
     * @return querier robustness value
     */
    public byte getQrv() {
        return qrv;
    }

    /**
     * Set the Querier Robustness Variable. Default is 2.
     *
     * @param qrv new querier robustness value
     */
    public void setQrv(byte qrv) {
        this.qrv = qrv;
    }

    /**
     * Get the reserved field.  Should be zero, but ignored regardless of it's value.
     *
     * @return the reserved field.
     */
    public byte getResv() {
        return resv;
    }

    /**
     * Set the reserved field.  Should be 0 and ignored by receivers.
     *
     * @param resv the reserved field.
     */
    public void setResv(byte resv) {
        this.resv = resv;
    }

    /**
     * Serialize this IGMPQuery.
     *
     * @param bb the ByteBuffer to write into, positioned at the next spot to be written to.
     * @return the serialized message
     */
    @Override
    public byte[] serialize(ByteBuffer bb) {

        bb.put(gaddr.toOctets());
        byte fld = (byte) (0x7 & qrv);
        bb.put(fld);
        bb.put(qqic);
        bb.putShort((short) sources.size());
        for (IpAddress ipaddr : sources) {
            bb.put(ipaddr.toOctets());
        }
        return bb.array();
    }

    /**
     * Deserialize the IGMP Query group structure.
     *
     * @param bb ByteBuffer pointing at the IGMP Query group address
     * @return the IGMP Group object
     * @throws DeserializationException on serializer error
     */
    public IGMPGroup deserialize(ByteBuffer bb) throws DeserializationException {

        gaddr = Ip4Address.valueOf(bb.getInt());
        byte fld = bb.get();

        // Just ignore the reserved bits
        resv = 0;
        this.sbit = ((fld & 0x8) == 0x8);
        qrv = (byte) (fld & 0x7);

        // QQIC field
        qqic = bb.get();

        // Get the number of sources.
        short nsrcs = bb.getShort();

        // Do a sanity check on the amount of space we have in our buffer.
        int lengthNeeded = (Ip4Address.BYTE_LENGTH * nsrcs);
        PacketUtils.checkHeaderLength(bb.remaining(), lengthNeeded);

        for (; nsrcs > 0; nsrcs--) {
            Ip4Address ipaddr = Ip4Address.valueOf(bb.getInt());
            this.sources.add(ipaddr);
        }
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals()
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IGMPQuery)) {
            return false;
        }
        IGMPQuery other = (IGMPQuery) obj;

        if (this.sbit != other.sbit) {
            return false;
        }
        if (this.qrv != other.qrv) {
            return false;
        }
        if (this.qqic != other.qqic) {
            return false;
        }
        if (this.sources.size() != other.sources.size()) {
            return false;
        }

        // TODO: make these tolerant of order
        if (!this.sources.equals(other.sources)) {
            return false;
        }

        return true;
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
        result = prime * result + this.gaddr.hashCode();
        result = prime * result + this.qqic;
        result = prime * result + this.qrv;
        result = prime * result + this.sources.hashCode();
        return result;
    }
}
