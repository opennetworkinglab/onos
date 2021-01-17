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
package org.onosproject.kubevirtnetworking.api;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default implementation of kubernetes port.
 */
public final class DefaultKubevirtPort implements KubevirtPort {

    private static final String NOT_NULL_MSG = "Port % cannot be null";

    private final String networkId;
    private final MacAddress macAddress;
    private final IpAddress ipAddress;
    private final DeviceId deviceId;
    private final PortNumber portNumber;

    /**
     * Default constructor.
     *
     * @param networkId         network identifier
     * @param macAddress        MAC address
     * @param ipAddress         IP address
     * @param deviceId          device identifier
     * @param portNumber        port number
     */
    public DefaultKubevirtPort(String networkId, MacAddress macAddress, IpAddress ipAddress,
                               DeviceId deviceId, PortNumber portNumber) {
        this.networkId = networkId;
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.deviceId = deviceId;
        this.portNumber = portNumber;
    }

    @Override
    public String networkId() {
        return networkId;
    }

    @Override
    public MacAddress macAddress() {
        return macAddress;
    }

    @Override
    public IpAddress ipAddress() {
        return ipAddress;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public PortNumber portNumber() {
        return portNumber;
    }

    @Override
    public KubevirtPort updateIpAddress(IpAddress updateIpAddress) {
        return new Builder()
                .networkId(networkId)
                .macAddress(macAddress)
                .ipAddress(updateIpAddress)
                .deviceId(deviceId)
                .portNumber(portNumber)
                .build();
    }

    @Override
    public KubevirtPort updatePortNumber(PortNumber updatedPortNumber) {
        return new Builder()
                .networkId(networkId)
                .macAddress(macAddress)
                .ipAddress(ipAddress)
                .deviceId(deviceId)
                .portNumber(updatedPortNumber)
                .build();
    }

    @Override
    public KubevirtPort updateDeviceId(DeviceId updatedDeviceId) {
        return new Builder()
                .networkId(networkId)
                .macAddress(macAddress)
                .ipAddress(ipAddress)
                .deviceId(updatedDeviceId)
                .portNumber(portNumber)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultKubevirtPort that = (DefaultKubevirtPort) o;
        return networkId.equals(that.networkId) && macAddress.equals(that.macAddress) &&
                ipAddress.equals(that.ipAddress) && deviceId.equals(that.deviceId) &&
                portNumber.equals(that.portNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkId, macAddress, ipAddress, deviceId, portNumber);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("networkId", networkId)
                .add("macAddress", macAddress)
                .add("ipAddress", ipAddress)
                .add("deviceId", deviceId)
                .add("portNumber", portNumber)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return kubevirt port builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Default builder implementation.
     */
    public static final class Builder implements KubevirtPort.Builder {

        private String networkId;
        private MacAddress macAddress;
        private IpAddress ipAddress;
        private DeviceId deviceId;
        private PortNumber portNumber;

        // private constructor not intended to use from external
        private Builder() {
        }

        @Override
        public KubevirtPort build() {
            checkArgument(networkId != null, NOT_NULL_MSG, "networkId");
            checkArgument(macAddress != null, NOT_NULL_MSG, "macAddress");

            return new DefaultKubevirtPort(networkId, macAddress, ipAddress,
                    deviceId, portNumber);
        }

        @Override
        public Builder networkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        @Override
        public Builder macAddress(MacAddress macAddress) {
            this.macAddress = macAddress;
            return this;
        }

        @Override
        public Builder ipAddress(IpAddress ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        @Override
        public Builder deviceId(DeviceId deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        @Override
        public Builder portNumber(PortNumber portNumber) {
            this.portNumber = portNumber;
            return this;
        }
    }
}
