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
import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepNai;

import com.google.common.base.MoreObjects;

/**
 * Provides Pcep Nai Ipv6 Adjacency.
 */
public class PcepNaiIpv6Adjacency implements PcepNai {

    public static final byte ST_TYPE = 0x04;
    public static final byte IPV6_LEN = 0x10;

    private final byte[] localIpv6Addr;
    private final byte[] remoteIpv6Addr;

    /**
     * Constructor to initialize local ipv6 and remote ipv6.
     *
     * @param localIpv6 local ipv6 address
     * @param remoteIpv6 remote ipv6 address
     */
    public PcepNaiIpv6Adjacency(byte[] localIpv6, byte[] remoteIpv6) {
        this.localIpv6Addr = localIpv6;
        this.remoteIpv6Addr = remoteIpv6;
    }

    @Override
    public byte getType() {
        return ST_TYPE;
    }

    @Override
    public int write(ChannelBuffer bb) {
        int iLenStartIndex = bb.writerIndex();
        bb.writeBytes(localIpv6Addr);
        bb.writeBytes(remoteIpv6Addr);
        return bb.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads from channel buffer and returns object of PcepNAIIpv6AdjacencyVer1.
     *
     * @param bb of type channel buffer
     * @return object of PcepNAIIpv6AdjacencyVer1
     */
    public static PcepNaiIpv6Adjacency read(ChannelBuffer bb) {
        byte[] localIpv6 = new byte[IPV6_LEN];
        bb.readBytes(localIpv6, 0, IPV6_LEN);
        byte[] remoteIpv6 = new byte[IPV6_LEN];
        bb.readBytes(remoteIpv6, 0, IPV6_LEN);
        return new PcepNaiIpv6Adjacency(localIpv6, remoteIpv6);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(localIpv6Addr), Arrays.hashCode(remoteIpv6Addr));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PcepNaiIpv6Adjacency) {
            PcepNaiIpv6Adjacency other = (PcepNaiIpv6Adjacency) obj;
            return Arrays.equals(this.localIpv6Addr, other.localIpv6Addr)
                    && Arrays.equals(this.remoteIpv6Addr, other.remoteIpv6Addr);
        }
        return false;
    }

    /**
     * Creates object of PcepNaiIpv6Adjacency with local ipv6 address and remote ipv6 address.
     *
     * @param localIpv6Addr local ipv6 address
     * @param remoteIpv6Addr remote ipv6 address
     * @return object of PcepNaiIpv6Adjacency
     */

    public static PcepNaiIpv6Adjacency of(final byte[] localIpv6Addr, final byte[] remoteIpv6Addr) {
        return new PcepNaiIpv6Adjacency(localIpv6Addr, remoteIpv6Addr);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("localIPV6Address", localIpv6Addr)
                .add("remoteIPV6Address", remoteIpv6Addr)
                .toString();
    }

}
