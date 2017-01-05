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

package org.onosproject.pcepio.types;

import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepNai;

import com.google.common.base.MoreObjects;

/**
 * Provides Pcep Nai Ipv6 Node Id.
 */
public class PcepNaiIpv6NodeId implements PcepNai {

    public static final byte ST_TYPE = 0x02;
    public static final byte IPV6_LEN = 0x10;

    private final byte[] ipv6NodeId;

    /**
     * Constructor to initialize ipv6NodeId.
     *
     * @param value ipv6 node id
     */
    public PcepNaiIpv6NodeId(byte[] value) {
        this.ipv6NodeId = value;
    }

    /**
     * Return object of Pcep Nai Ipv6 Node ID.
     *
     * @param ipv6NodeId Ipv6 node ID.
     * @return object of Pcep Nai Ipv6 Node ID.
     */
    public static PcepNaiIpv6NodeId of(byte[] ipv6NodeId) {
        return new PcepNaiIpv6NodeId(ipv6NodeId);
    }

    @Override
    public byte getType() {
        return ST_TYPE;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeBytes(ipv6NodeId);
        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads from the channel buffer and returns object of PcepNAIIpv6NodeId.
     *
     * @param cb of type channel buffer.
     * @return object of PcepNAIIpv6NodeId
     */
    public static PcepNaiIpv6NodeId read(ChannelBuffer cb) {
        byte[] ipv6NodeId = new byte[IPV6_LEN];
        cb.readBytes(ipv6NodeId, 0, IPV6_LEN);
        return new PcepNaiIpv6NodeId(ipv6NodeId);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(ipv6NodeId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PcepNaiIpv6NodeId) {
            PcepNaiIpv6NodeId other = (PcepNaiIpv6NodeId) obj;
            return Arrays.equals(this.ipv6NodeId, other.ipv6NodeId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("IPV6NodeID", ipv6NodeId)
                .toString();
    }
}
