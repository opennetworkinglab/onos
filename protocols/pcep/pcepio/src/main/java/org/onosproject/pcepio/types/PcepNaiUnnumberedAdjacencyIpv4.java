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
 * Provides Pcep Nai Unnumbered Adjacency Ipv4.
 */
public class PcepNaiUnnumberedAdjacencyIpv4 implements PcepNai {
    /**
     * draft-ietf-pce-segment-routing-03 section    5.3.2.
     */
    public static final byte ST_TYPE = 0x05;

    private final int localNodeId;
    private final int localInterfaceId;
    private final int remoteNodeId;
    private final int remoteInterfaceId;

    /**
     * Constructor to initialize all the member variables.
     *
     * @param localNodeId local node id
     * @param localInterfaceId local interface id
     * @param remoteNodeId remote node id
     * @param remoteInterfaceId remote interface id
     */
    public PcepNaiUnnumberedAdjacencyIpv4(int localNodeId, int localInterfaceId, int remoteNodeId,
            int remoteInterfaceId) {
        this.localNodeId = localNodeId;
        this.localInterfaceId = localInterfaceId;
        this.remoteNodeId = remoteNodeId;
        this.remoteInterfaceId = remoteInterfaceId;
    }

    /**
     * Returns PCEP Nai Unnumbered Adjacency Ipv4 object.
     *
     * @param localNodeId local node id
     * @param localInterfaceId local interface if
     * @param remoteNodeId remote node id
     * @param remoteInterfaceId remote interface id
     * @return PCEP Nai Unnumbered Adjacency Ipv4 object
     */
    public static PcepNaiUnnumberedAdjacencyIpv4 of(int localNodeId, int localInterfaceId, int remoteNodeId,
            int remoteInterfaceId) {
        return new PcepNaiUnnumberedAdjacencyIpv4(localNodeId, localInterfaceId, remoteNodeId, remoteInterfaceId);
    }

    @Override
    public byte getType() {
        return ST_TYPE;
    }

    @Override
    public int write(ChannelBuffer bb) {
        int iLenStartIndex = bb.writerIndex();
        bb.writeInt(localNodeId);
        bb.writeInt(localInterfaceId);
        bb.writeInt(remoteNodeId);
        bb.writeInt(remoteInterfaceId);
        return bb.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads from channel buffer and return object of PcepNAIUnnumberedAdjacencyIpv4.
     *
     * @param bb of type channel buffer
     * @return object of PcepNAIUnnumberedAdjacencyIpv4
     */
    public static PcepNaiUnnumberedAdjacencyIpv4 read(ChannelBuffer bb) {
        int localNodeId;
        int localInterfaceId;
        int remoteNodeId;
        int remoteInterfaceId;
        localNodeId = bb.readInt();
        localInterfaceId = bb.readInt();
        remoteNodeId = bb.readInt();
        remoteInterfaceId = bb.readInt();
        return new PcepNaiUnnumberedAdjacencyIpv4(localNodeId, localInterfaceId, remoteNodeId, remoteInterfaceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localNodeId, localInterfaceId, remoteNodeId, remoteInterfaceId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PcepNaiUnnumberedAdjacencyIpv4) {
            PcepNaiUnnumberedAdjacencyIpv4 other = (PcepNaiUnnumberedAdjacencyIpv4) obj;
            return Objects.equals(this.localNodeId, other.localNodeId)
                    && Objects.equals(this.localInterfaceId, other.localInterfaceId)
                    && Objects.equals(this.remoteNodeId, other.remoteNodeId)
                    && Objects.equals(this.remoteInterfaceId, other.remoteInterfaceId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("localNodeId", localNodeId)
                .add("localInterfaceId", localInterfaceId)
                .add("remoteNodeId", remoteNodeId)
                .add("remoteInterfaceId", remoteInterfaceId)
                .toString();
    }
}
