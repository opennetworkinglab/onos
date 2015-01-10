/*
 * Copyright 2014 Open Networking Laboratory
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
import org.onlab.packet.Data;
import org.onlab.packet.IPacket;
import org.onlab.packet.Ip6Address;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Implements ICMPv6 Neighbor Advertisement packet format. (RFC 4861)
 */
public class NeighborAdvertisement extends BasePacket {
    public static final byte HEADER_LENGTH = 20; // bytes

    protected byte routerFlag;
    protected byte solicitedFlag;
    protected byte overrideFlag;
    protected byte[] targetAddress = new byte[Ip6Address.BYTE_LENGTH];

    /**
     * Gets router flag.
     *
     * @return the router flag
     */
    public byte getRouterFlag() {
        return this.routerFlag;
    }

    /**
     * Sets router flag.
     *
     * @param routerFlag the router flag to set
     * @return this
     */
    public NeighborAdvertisement setRouterFlag(final byte routerFlag) {
        this.routerFlag = routerFlag;
        return this;
    }

    /**
     * Gets solicited flag.
     *
     * @return the solicited flag
     */
    public byte getSolicitedFlag() {
        return this.solicitedFlag;
    }

    /**
     * Sets solicited flag.
     *
     * @param solicitedFlag the solicited flag to set
     * @return this
     */
    public NeighborAdvertisement setSolicitedFlag(final byte solicitedFlag) {
        this.solicitedFlag = solicitedFlag;
        return this;
    }

    /**
     * Gets override flag.
     *
     * @return the override flag
     */
    public byte getOverrideFlag() {
        return this.overrideFlag;
    }

    /**
     * Sets override flag.
     *
     * @param overrideFlag the override flag to set
     * @return this
     */
    public NeighborAdvertisement setOverrideFlag(final byte overrideFlag) {
        this.overrideFlag = overrideFlag;
        return this;
    }

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
    public NeighborAdvertisement setTargetAddress(final byte[] targetAddress) {
        this.targetAddress = Arrays.copyOfRange(targetAddress, 0, Ip6Address.BYTE_LENGTH);
        return this;
    }

    @Override
    public byte[] serialize() {
        byte[] payloadData = null;
        if (this.payload != null) {
            this.payload.setParent(this);
            payloadData = this.payload.serialize();
        }

        int payloadLength = 0;
        if (payloadData != null) {
            payloadLength = payloadData.length;
        }

        final byte[] data = new byte[HEADER_LENGTH + payloadLength];
        final ByteBuffer bb = ByteBuffer.wrap(data);

        bb.putInt((this.routerFlag & 0x1) << 31 | (this.solicitedFlag & 0x1) << 30 | (this.overrideFlag & 0x1) << 29);
        bb.put(this.targetAddress, 0, Ip6Address.BYTE_LENGTH);
        if (payloadData != null) {
            bb.put(payloadData);
        }

        return data;
    }

    @Override
    public IPacket deserialize(byte[] data, int offset, int length) {
        final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
        int iscratch;

        iscratch = bb.getInt();
        this.routerFlag = (byte) (iscratch >> 31 & 0x1);
        this.solicitedFlag = (byte) (iscratch >> 30 & 0x1);
        this.overrideFlag = (byte) (iscratch >> 29 & 0x1);
        bb.get(this.targetAddress, 0, Ip6Address.BYTE_LENGTH);

        this.payload = new Data();
        this.payload = this.payload.deserialize(data, bb.position(), bb.limit()
                - bb.position());
        this.payload.setParent(this);

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
        result = prime * result + this.routerFlag;
        result = prime * result + this.solicitedFlag;
        result = prime * result + this.overrideFlag;
        bb = ByteBuffer.wrap(this.targetAddress);
        for (int i = 0; i < 4; i++) {
            result = prime * result + bb.getInt();
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
        if (!(obj instanceof NeighborAdvertisement)) {
            return false;
        }
        final NeighborAdvertisement other = (NeighborAdvertisement) obj;
        if (this.routerFlag != other.routerFlag) {
            return false;
        }
        if (this.solicitedFlag != other.solicitedFlag) {
            return false;
        }
        if (this.overrideFlag != other.overrideFlag) {
            return false;
        }
        if (!Arrays.equals(this.targetAddress, other.targetAddress)) {
            return false;
        }
        return true;
    }
}
