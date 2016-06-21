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

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepNai;

import com.google.common.base.MoreObjects;

/**
 * Provides Pcep Nai Ipv4 Adjacency.
 */
public class PcepNaiIpv4Adjacency implements PcepNai {

    public static final byte ST_TYPE = 0x03;
    private final int localIpv4Addr;
    private final int remoteIpv4Addr;

    /**
     * Constructor to initialize variables.
     *
     * @param localIpv4 local ipv4 address
     * @param remoteIpv4 remote ipv4 address
     */
    public PcepNaiIpv4Adjacency(int localIpv4, int remoteIpv4) {
        this.localIpv4Addr = localIpv4;
        this.remoteIpv4Addr = remoteIpv4;
    }

    /**
     * Returns Object of Pcep nai Ipv4 Adjacency.
     *
     * @param localIpv4Addr local ipv4 address
     * @param remoteIpv4Addr remote ipv4 address
     * @return Object of Pcep nai Ipv4 Adjacency
     */
    public static PcepNaiIpv4Adjacency of(int localIpv4Addr, int remoteIpv4Addr) {
        return new PcepNaiIpv4Adjacency(localIpv4Addr, remoteIpv4Addr);
    }

    @Override
    public byte getType() {
        return ST_TYPE;
    }

    public int getLocalIpv4Addr() {
        return localIpv4Addr;
    }

    public int getRemoteIpv4Addr() {
        return remoteIpv4Addr;
    }

    @Override
    public int write(ChannelBuffer bb) {
        int iLenStartIndex = bb.writerIndex();
        bb.writeInt(localIpv4Addr);
        bb.writeInt(remoteIpv4Addr);
        return bb.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of PcepNAIIpv4AdjacencyVer1.
     *
     * @param cb of channel buffer
     * @return object of PcepNAIIpv4Adjacency
     */
    public static PcepNaiIpv4Adjacency read(ChannelBuffer cb) {
        int localIpv4 = cb.readInt();
        int remoteIpv4 = cb.readInt();
        return new PcepNaiIpv4Adjacency(localIpv4, remoteIpv4);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localIpv4Addr, remoteIpv4Addr);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PcepNaiIpv4Adjacency) {
            PcepNaiIpv4Adjacency other = (PcepNaiIpv4Adjacency) obj;
            return Objects.equals(this.localIpv4Addr, other.localIpv4Addr)
                    && Objects.equals(this.remoteIpv4Addr, other.remoteIpv4Addr);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("localIPv4Address", localIpv4Addr)
                .add("remoteIPv4Address", remoteIpv4Addr)
                .toString();
    }
}
