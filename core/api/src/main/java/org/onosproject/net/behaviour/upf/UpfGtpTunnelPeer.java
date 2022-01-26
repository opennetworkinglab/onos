/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.net.behaviour.upf;

import com.google.common.annotations.Beta;
import org.onlab.packet.Ip4Address;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A structure representing a UPF GTP tunnel peer.
 * The GTP Tunnel Peer is used by UPF to identify a second end of a GTP tunnel.
 * The source and destination tunnel IPv4 addresses, and source UDP port are set
 * based on the information from this structure.
 */
@Beta
public final class UpfGtpTunnelPeer implements UpfEntity {
    // Match keys
    private final byte tunPeerId;
    // Action parameters
    private final Ip4Address src;  // The source address of the unidirectional tunnel
    private final Ip4Address dst;  // The destination address of the unidirectional tunnel
    private final short srcPort;   // Tunnel source port, default 2152

    private UpfGtpTunnelPeer(byte tunPeerId, Ip4Address src, Ip4Address dst, short srcPort) {
        this.tunPeerId = tunPeerId;
        this.src = src;
        this.dst = dst;
        this.srcPort = srcPort;
    }

    public static UpfGtpTunnelPeer.Builder builder() {
        return new UpfGtpTunnelPeer.Builder();
    }

    @Override
    public String toString() {
        return String.format("UpfGtpTunnelPeer(tunn_peer_id=%s -> src=%s, dst=%s src_port=%s)",
                             tunPeerId, src.toString(), dst.toString(), srcPort);
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null) {
            return false;
        }

        if (getClass() != object.getClass()) {
            return false;
        }

        UpfGtpTunnelPeer that = (UpfGtpTunnelPeer) object;
        return this.tunPeerId == that.tunPeerId &&
                this.src.equals(that.src) &&
                this.dst.equals(that.dst) &&
                this.srcPort == that.srcPort;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tunPeerId, src, dst, srcPort);
    }

    /**
     * Get the ID of the UPF GTP tunnel peer.
     *
     * @return GTP tunnel peer ID
     */
    public byte tunPeerId() {
        return tunPeerId;
    }

    /**
     * Get the source IP address of this unidirectional UPF GTP tunnel.
     *
     * @return tunnel source IP
     */
    public Ip4Address src() {
        return this.src;
    }

    /**
     * Get the destination address of this unidirectional UPF GTP tunnel.
     *
     * @return tunnel destination IP
     */
    public Ip4Address dst() {
        return this.dst;
    }

    /**
     * Get the source L4 port of this unidirectional UPF GTP tunnel.
     *
     * @return tunnel source port
     */
    public short srcPort() {
        return this.srcPort;
    }

    @Override
    public UpfEntityType type() {
        return UpfEntityType.TUNNEL_PEER;
    }

    public static class Builder {
        private Byte tunPeerId = null;
        private Ip4Address src = null;
        private Ip4Address dst = null;
        private short srcPort = 2152;  // Default value is equal to GTP tunnel dst port

        public Builder() {

        }

        /**
         * Set the ID of the UPF GTP Tunnel peer.
         *
         * @param tunPeerId GTP tunnel peer ID
         * @return This builder object
         */
        public UpfGtpTunnelPeer.Builder withTunnelPeerId(byte tunPeerId) {
            this.tunPeerId = tunPeerId;
            return this;
        }

        /**
         * Set the source IP address of the unidirectional UPF GTP tunnel.
         *
         * @param src GTP tunnel source IP
         * @return This builder object
         */
        public UpfGtpTunnelPeer.Builder withSrcAddr(Ip4Address src) {
            this.src = src;
            return this;
        }

        /**
         * Set the destination IP address of the unidirectional UPF GTP tunnel.
         *
         * @param dst GTP tunnel destination IP
         * @return This builder object
         */
        public UpfGtpTunnelPeer.Builder withDstAddr(Ip4Address dst) {
            this.dst = dst;
            return this;
        }

        /**
         * Set the source port of this unidirectional UPF GTP tunnel.
         *
         * @param srcPort tunnel source port
         * @return this builder object
         */
        public UpfGtpTunnelPeer.Builder withSrcPort(short srcPort) {
            this.srcPort = srcPort;
            return this;
        }

        public UpfGtpTunnelPeer build() {
            checkArgument(tunPeerId != null, "Tunnel Peer ID must be provided");
            checkArgument(src != null, "Tunnel source address cannot be null");
            checkArgument(dst != null, "Tunnel destination address cannot be null");
            return new UpfGtpTunnelPeer(this.tunPeerId, this.src, this.dst, srcPort);
        }

    }

}
