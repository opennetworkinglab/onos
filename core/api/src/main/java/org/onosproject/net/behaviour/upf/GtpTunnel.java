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

import org.onlab.packet.Ip4Address;
import org.onlab.util.ImmutableByteSequence;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A structure representing a unidirectional GTP tunnel.
 */
public final class GtpTunnel {
    private final Ip4Address src;  // The source address of the unidirectional tunnel
    private final Ip4Address dst;  // The destination address of the unidirectional tunnel
    private final ImmutableByteSequence teid;  // Tunnel Endpoint Identifier
    private final short srcPort;  // Tunnel destination port, default 2152

    private GtpTunnel(Ip4Address src, Ip4Address dst, ImmutableByteSequence teid,
                      Short srcPort) {
        this.src = src;
        this.dst = dst;
        this.teid = teid;
        this.srcPort = srcPort;
    }

    public static GtpTunnelBuilder builder() {
        return new GtpTunnelBuilder();
    }

    @Override
    public String toString() {
        return String.format("GTP-Tunnel(%s -> %s, TEID:%s)",
                             src.toString(), dst.toString(), teid.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        GtpTunnel that = (GtpTunnel) obj;

        return (this.src.equals(that.src) &&
                this.dst.equals(that.dst) &&
                this.teid.equals(that.teid) &&
                (this.srcPort == that.srcPort));
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, dst, teid, srcPort);
    }

    /**
     * Get the source IP address of this unidirectional GTP tunnel.
     *
     * @return tunnel source IP
     */
    public Ip4Address src() {
        return this.src;
    }

    /**
     * Get the destination address of this unidirectional GTP tunnel.
     *
     * @return tunnel destination IP
     */
    public Ip4Address dst() {
        return this.dst;
    }

    /**
     * Get the ID of this unidirectional GTP tunnel.
     *
     * @return tunnel ID
     */
    public ImmutableByteSequence teid() {
        return this.teid;
    }


    /**
     * Get the source L4 port of this unidirectional GTP tunnel.
     *
     * @return tunnel source port
     */
    public Short srcPort() {
        return this.srcPort;
    }

    public static class GtpTunnelBuilder {
        private Ip4Address src;
        private Ip4Address dst;
        private ImmutableByteSequence teid;
        private short srcPort = 2152;  // Default value is equal to GTP tunnel dst port

        public GtpTunnelBuilder() {
            this.src = null;
            this.dst = null;
            this.teid = null;
        }

        /**
         * Set the source IP address of the unidirectional GTP tunnel.
         *
         * @param src GTP tunnel source IP
         * @return This builder object
         */
        public GtpTunnelBuilder setSrc(Ip4Address src) {
            this.src = src;
            return this;
        }

        /**
         * Set the destination IP address of the unidirectional GTP tunnel.
         *
         * @param dst GTP tunnel destination IP
         * @return This builder object
         */
        public GtpTunnelBuilder setDst(Ip4Address dst) {
            this.dst = dst;
            return this;
        }

        /**
         * Set the identifier of this unidirectional GTP tunnel.
         *
         * @param teid tunnel ID
         * @return This builder object
         */
        public GtpTunnelBuilder setTeid(ImmutableByteSequence teid) {
            this.teid = teid;
            return this;
        }

        /**
         * Set the identifier of this unidirectional GTP tunnel.
         *
         * @param teid tunnel ID
         * @return This builder object
         */
        public GtpTunnelBuilder setTeid(long teid) {
            this.teid = ImmutableByteSequence.copyFrom(teid);
            return this;
        }

        /**
         * Set the source port of this unidirectional GTP tunnel.
         *
         * @param srcPort tunnel source port
         * @return this builder object
         */
        public GtpTunnelBuilder setSrcPort(short srcPort) {
            this.srcPort = srcPort;
            return this;
        }

        public GtpTunnel build() {
            checkNotNull(src, "Tunnel source address cannot be null");
            checkNotNull(dst, "Tunnel destination address cannot be null");
            checkNotNull(teid, "Tunnel TEID cannot be null");
            return new GtpTunnel(this.src, this.dst, this.teid, srcPort);
        }
    }
}
