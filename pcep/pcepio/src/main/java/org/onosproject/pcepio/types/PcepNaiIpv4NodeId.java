/*
 * Copyright 2015 Open Networking Laboratory
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

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepNai;

import com.google.common.base.MoreObjects;

/**
 * Provides Pcep Nai Ipv4 Node Id.
 */
public class PcepNaiIpv4NodeId implements PcepNai {

    public static final byte ST_TYPE = 0x01;

    private final int ipv4NodeId;

    /**
     * Constructor to initialize ipv4NodeId.
     *
     * @param value ipv4 node id
     */
    public PcepNaiIpv4NodeId(int value) {
        this.ipv4NodeId = value;
    }

    /**
     * Returns an object of PcepNaiIpv4NodeId.
     *
     * @param value ipv4 node id
     * @return object of PcepNaiIpv4NodeId
     */
    public static PcepNaiIpv4NodeId of(int value) {
        return new PcepNaiIpv4NodeId(value);
    }

    @Override
    public byte getType() {
        return ST_TYPE;
    }

    @Override
    public int write(ChannelBuffer bb) {
        int iLenStartIndex = bb.writerIndex();
        bb.writeInt(ipv4NodeId);
        return bb.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads from the channel buffer and returns object of PcepNAIIpv4NodeIdVer1.
     *
     * @param bb of channel buffer.
     * @return object of PcepNAIIpv4NodeIdVer1
     */
    public static PcepNaiIpv4NodeId read(ChannelBuffer bb) {
        return new PcepNaiIpv4NodeId(bb.readInt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipv4NodeId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PcepNaiIpv4NodeId) {
            PcepNaiIpv4NodeId other = (PcepNaiIpv4NodeId) obj;
            return Objects.equals(this.ipv4NodeId, other.ipv4NodeId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("IPv4NodeId", ipv4NodeId)
                .toString();
    }
}
