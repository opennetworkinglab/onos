/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.openstacknetworking;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;

import java.util.Collection;
import java.util.Collections;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Contains OpenstackPort Information.
 */
public final class OpenstackPortInfo {
    private final Ip4Address hostIp;
    private final MacAddress hostMac;
    private final DeviceId deviceId;
    private final long vni;
    private final Ip4Address gatewayIP;
    private final String networkId;
    private final Collection<String> securityGroups;

    /**
     * Returns OpenstackPortInfo reference.
     *
     * @param hostIp host IP address
     * @param hostMac host MAC address
     * @param deviceId device ID
     * @param vni  tunnel ID
     * @param gatewayIP gateway IP address
     * @param networkId network identifier
     * @param securityGroups security group list
     */
    public OpenstackPortInfo(Ip4Address hostIp, MacAddress hostMac, DeviceId deviceId, long vni,
                             Ip4Address gatewayIP, String networkId, Collection<String> securityGroups) {
        this.hostIp = hostIp;
        this.hostMac = hostMac;
        this.deviceId = deviceId;
        this.vni = vni;
        this.gatewayIP = gatewayIP;
        this.networkId = networkId;
        this.securityGroups = securityGroups;
    }

    /**
     * Returns IP address of the port.
     *
     * @return IP address
     */
    public Ip4Address ip() {
        return hostIp;
    }

    /**
     * Returns MAC address of the port.
     *
     * @return MAC address
     */
    public MacAddress mac() {
        return hostMac;
    }

    /**
     * Returns device ID.
     *
     * @return device ID
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Returns tunnel ID.
     *
     * @return tunnel ID
     */
    public long vni() {
        return vni;
    }

    /**
     * Returns gateway IP address.
     *
     * @return gateway IP address
     */
    public Ip4Address gatewayIP() {
        return gatewayIP;
    }

    /**
     * Returns network ID.
     *
     * @return network ID
     */
    public String networkId() {
        return  networkId;
    }

    /**
     * Returns Security Group ID list.
     *
     * @return list of Security Group ID
     */
    public Collection<String> securityGroups() {
        return Collections.unmodifiableCollection(securityGroups);
    }

    /**
     * Returns the builder of the OpenstackPortInfo.
     *
     * @return OpenstackPortInfo builder reference
     */
    public static OpenstackPortInfo.Builder builder() {
        return new Builder();
    }

    /**
     * Represents the OpenstackPortInfo Builder.
     *
     */
    public static final class Builder {
        private Ip4Address hostIp;
        private MacAddress hostMac;
        private DeviceId deviceId;
        private long vni;
        private Ip4Address gatewayIP;
        private Collection<String> securityGroups;
        private String networkId;

        /**
         * Sets the IP address of the port.
         *
         * @param gatewayIP gateway IP
         * @return Builder reference
         */
        public Builder setGatewayIP(Ip4Address gatewayIP) {
            this.gatewayIP = checkNotNull(gatewayIP, "gatewayIP cannot be null");
            return this;
        }

        /**
         * Sets the network ID.
         *
         * @param networkId network id
         * @return Builder reference
         */
        public Builder setNetworkId(String networkId) {
            this.networkId = checkNotNull(networkId, "networkId cannot be null");
            return this;
        }

        /**
         * Sets the host IP address of the port.
         *
         * @param hostIp host IP address
         * @return Builder reference
         */
        public Builder setHostIp(Ip4Address hostIp) {
            this.hostIp = checkNotNull(hostIp, "hostIp cannot be null");
            return this;
        }

        /**
         * Sets the host MAC address of the port.
         *
         * @param hostMac host MAC address
         * @return Builder reference
         */
        public Builder setHostMac(MacAddress hostMac) {
            this.hostMac = checkNotNull(hostMac, "hostMac cannot be bull");
            return this;
        }

        /**
         * Sets the device ID.
         *
         * @param deviceId device ID
         * @return Builder reference
         */
        public Builder setDeviceId(DeviceId deviceId) {
            this.deviceId = checkNotNull(deviceId, "deviceId cannot be null");
            return this;
        }

        /**
         * Sets the tunnel ID.
         *
         * @param vni tunnel ID
         * @return Builder reference
         */
        public Builder setVni(long vni) {
            this.vni = checkNotNull(vni, "vni cannot be null");
            return this;
        }

        /**
         * Sets the security group ID list.
         *
         * @param securityGroups security group ID list
         * @return Builder reference
         */
        public Builder setSecurityGroups(Collection<String> securityGroups) {
            this.securityGroups = securityGroups;
            return this;
        }

        /**
         * Builds the OpenstackPortInfo reference.
         *
         * @return OpenstackPortInfo reference
         */
        public OpenstackPortInfo build() {
            return new OpenstackPortInfo(hostIp, hostMac, deviceId, vni, gatewayIP, networkId, securityGroups);
        }
    }
}
