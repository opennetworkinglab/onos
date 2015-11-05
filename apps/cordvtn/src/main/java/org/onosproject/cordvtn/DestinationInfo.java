/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.cordvtn;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.Port;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Contains destination information.
 */
public final class DestinationInfo {

    private final Port output;
    private final List<IpAddress> ip;
    private final MacAddress mac;
    private final IpAddress remoteIp;
    private final long tunnelId;

    /**
     * Creates a new destination information.
     *
     * @param output output port
     * @param ip destination ip address
     * @param mac destination mac address
     * @param remoteIp tunnel remote ip address
     * @param tunnelId segment id
     */
    public DestinationInfo(Port output, List<IpAddress> ip, MacAddress mac,
                           IpAddress remoteIp, long tunnelId) {
        this.output = checkNotNull(output);
        this.ip = ip;
        this.mac = mac;
        this.remoteIp = remoteIp;
        this.tunnelId = tunnelId;
    }

    /**
     * Returns output port.
     *
     * @return port
     */
    public Port output() {
        return output;
    }

    /**
     * Returns destination ip addresses.
     *
     * @return list of ip address
     */
    public List<IpAddress> ip() {
        return ip;
    }

    /**
     * Returns destination mac address.
     *
     * @return mac address
     */
    public MacAddress mac() {
        return mac;
    }

    /**
     * Returns tunnel remote ip address.
     *
     * @return ip address
     */
    public IpAddress remoteIp() {
        return remoteIp;
    }

    /**
     * Returns tunnel id.
     *
     * @return tunnel id
     */
    public long tunnelId() {
        return tunnelId;
    }

    /**
     * Returns a new destination info builder.
     *
     * @return destination info builder
     */
    public static DestinationInfo.Builder builder(Port output) {
        return new Builder(output);
    }

    /**
     * DestinationInfo builder class.
     */
    public static final class Builder {

        private final Port output;
        private List<IpAddress> ip;
        private MacAddress mac;
        private IpAddress remoteIp;
        private long tunnelId;

        /**
         * Creates a new destination information builder.
         *
         * @param output output port
         */
        public Builder(Port output) {
            this.output = checkNotNull(output, "Output port cannot be null");
        }

        /**
         * Sets the destination ip address.
         *
         * @param ip ip address
         * @return destination info builder
         */
        public Builder setIp(List<IpAddress> ip) {
            this.ip = checkNotNull(ip, "IP cannot be null");
            return this;
        }

        /**
         * Sets the destination mac address.
         *
         * @param mac mac address
         * @return destination info builder
         */
        public Builder setMac(MacAddress mac) {
            this.mac = checkNotNull(mac, "MAC address cannot be null");
            return this;
        }

        /**
         * Sets the tunnel remote ip address.
         *
         * @param remoteIp ip address
         * @return destination info builder
         */
        public Builder setRemoteIp(IpAddress remoteIp) {
            this.remoteIp = checkNotNull(remoteIp, "Remote IP address cannot be null");
            return this;
        }

        /**
         * Sets the tunnel id.
         *
         * @param tunnelId tunnel id
         * @return destination info builder
         */
        public Builder setTunnelId(long tunnelId) {
            this.tunnelId = checkNotNull(tunnelId, "Tunnel ID cannot be null");
            return this;
        }

        /**
         * Build a destination information.
         *
         * @return destination info object
         */
        public DestinationInfo build() {
            return new DestinationInfo(this);
        }
    }

    private DestinationInfo(Builder builder) {
        output = builder.output;
        ip = builder.ip;
        mac = builder.mac;
        remoteIp = builder.remoteIp;
        tunnelId = builder.tunnelId;
    }
}
