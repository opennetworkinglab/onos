/*
 * Copyright 2016-present Open Networking Foundation
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

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides implementation of BGP route policy distribution capability tlv.
 */
public class RpdCapabilityTlv implements BgpValueType {

    private static final Logger log = LoggerFactory
            .getLogger(RpdCapabilityTlv.class);

    public static final byte TYPE = (byte) 129;
    public static final byte LENGTH = 4;
    private short afi = Constants.AFI_FLOWSPEC_RPD_VALUE;
    private byte sAfi = Constants.SAFI_FLOWSPEC_RPD_VALUE;

    private final byte sendReceive;

    /**
     * Creates instance of route policy distribution capability.
     * @param sendReceive value indicate wherether flow route is only for receive or send or both.
     */
    public RpdCapabilityTlv(byte sendReceive) {
        this.sendReceive = sendReceive;
    }

    /**
     * Creates instance of RpdCapabilityTlv.
     * @param sendReceive value indicate wherether flow route is only for receive or send or both.
     * @return object of RpdCapabilityTlv
     */
    public static RpdCapabilityTlv of(final byte sendReceive) {
        return new RpdCapabilityTlv(sendReceive);
    }

    /**
     * Returns value of send receive field of route policy distribution capability.
     * @return send receive value of route policy distribution capability
     */
    public byte sendReceive() {
        return sendReceive;
    }

    /**
     * Returns address family identifier value.
     * @return afi address family identifier value
     */
    public short getAfi() {
        return afi;
    }

    /**
     * Returns subsequent address family identifier value.
     * @return safi subsequent address family identifier value
     */
    public byte getSafi() {
        return sAfi;
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sendReceive);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RpdCapabilityTlv) {
            RpdCapabilityTlv other = (RpdCapabilityTlv) obj;
            return Objects.equals(sendReceive, other.sendReceive);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeByte(TYPE);
        cb.writeByte(LENGTH);
        cb.writeShort(afi);
        cb.writeByte(sAfi);
        cb.writeByte(sendReceive);
        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of RpdCapabilityTlv.
     * @param cb type of channel buffer
     * @return object of RpdCapabilityTlv
     */
    public static RpdCapabilityTlv read(ChannelBuffer cb) {
        short afi = cb.readShort();
        byte sAfi = cb.readByte();
        return RpdCapabilityTlv.of(cb.readByte());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("afi", afi)
                .add("safi", sAfi)
                .add("sendReceive", sendReceive).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
