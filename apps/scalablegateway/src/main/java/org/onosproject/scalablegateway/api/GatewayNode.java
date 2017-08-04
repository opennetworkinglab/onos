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
package org.onosproject.scalablegateway.api;

import com.google.common.base.MoreObjects;
import org.onlab.packet.Ip4Address;
import org.onosproject.net.DeviceId;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents SONA GatewayNode information.
 */
public final class GatewayNode {
    private final DeviceId gatewayDeviceId;
    private final String uplinkIntf;
    private final Ip4Address dataIpAddress;

    private GatewayNode(DeviceId gatewayDeviceId, String uplinkIntf, Ip4Address dataIpAddress) {
        this.gatewayDeviceId = gatewayDeviceId;
        this.uplinkIntf = uplinkIntf;
        this.dataIpAddress = dataIpAddress;
    }

    /**
     * Returns the device id of gateway node.
     *
     * @return The device id of gateway node
     */
    public DeviceId getGatewayDeviceId() {
        return gatewayDeviceId;
    }

    /**
     * Returns the gateway`s interface name.
     *
     * @return The gateway`s interface name
     */
    public String getUplinkIntf() {
        return uplinkIntf;
    }

    /**
     * Returns the data ip address of gateway node.
     *
     * @return The data ip address of gateway node
     */
    public Ip4Address getDataIpAddress() {
        return dataIpAddress;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof GatewayNode) {
            GatewayNode that = (GatewayNode) obj;
            if (Objects.equals(gatewayDeviceId, that.gatewayDeviceId) &&
                    Objects.equals(uplinkIntf, that.uplinkIntf) &&
                    Objects.equals(dataIpAddress, that.dataIpAddress)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(gatewayDeviceId, uplinkIntf, dataIpAddress);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("gatewayDeviceId", gatewayDeviceId)
                .add("uplinkInterface", uplinkIntf)
                .add("dataIpAddress", dataIpAddress)
                .toString();
    }

    /**
     * Returns GatewayNode builder object.
     *
     * @return GatewayNode builder
     */
    public static GatewayNode.Builder builder() {
        return new Builder();
    }

    /**
     * GatewayNode Builder class.
     */
    public static final class Builder {

        private DeviceId gatewayDeviceId;
        private String uplinkIntf;
        private Ip4Address dataIpAddress;

        /**
         * Sets the device id of gateway node.
         *
         * @param deviceId The device id of gateway node
         * @return Builder object
         */
        public Builder gatewayDeviceId(DeviceId deviceId) {
            this.gatewayDeviceId = deviceId;
            return this;
        }

        /**
         * Sets the gateway`s uplink interface name.
         *
         * @param name The gateway`s interface name
         * @return Builder object
         */
        public Builder uplinkIntf(String name) {
            this.uplinkIntf = name;
            return this;
        }

        /**
         * Sets the ip address of gateway node for data plain.
         *
         * @param address The ip address of gateway node
         * @return Builder object
         */
        public Builder dataIpAddress(Ip4Address address) {
            this.dataIpAddress = address;
            return this;
        }

        /**
         * Builds a GatewayNode object.
         *
         * @return GatewayNode object
         */
        public GatewayNode build() {
            return new GatewayNode(checkNotNull(gatewayDeviceId), checkNotNull(uplinkIntf),
                                   checkNotNull(dataIpAddress));
        }
    }
}
