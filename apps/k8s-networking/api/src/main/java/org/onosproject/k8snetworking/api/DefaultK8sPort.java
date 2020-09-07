/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default implementation of kubernetes port.
 */
public final class DefaultK8sPort implements K8sPort {

    private static final String NOT_NULL_MSG = "Port % cannot be null";

    private final String networkId;
    private final String portId;
    private final MacAddress macAddress;
    private final IpAddress ipAddress;
    private final DeviceId deviceId;
    private final PortNumber portNumber;
    private final State state;

    // private constructor not intended for external invocation
    private DefaultK8sPort(String networkId, String portId, MacAddress macAddress,
                           IpAddress ipAddress, DeviceId deviceId,
                           PortNumber portNumber, State state) {
        this.networkId = networkId;
        this.portId = portId;
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.deviceId = deviceId;
        this.portNumber = portNumber;
        this.state = state;
    }

    @Override
    public String networkId() {
        return networkId;
    }

    @Override
    public String portId() {
        return portId;
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
    public State state() {
        return state;
    }

    @Override
    public K8sPort updateState(State newState) {
        return new Builder()
                .networkId(networkId)
                .portId(portId)
                .macAddress(macAddress)
                .ipAddress(ipAddress)
                .deviceId(deviceId)
                .portNumber(portNumber)
                .state(newState)
                .build();
    }

    @Override
    public K8sPort updatePortNumber(PortNumber portNumber) {
        return new Builder()
                .networkId(networkId)
                .portId(portId)
                .macAddress(macAddress)
                .ipAddress(ipAddress)
                .deviceId(deviceId)
                .portNumber(portNumber)
                .state(state)
                .build();
    }

    @Override
    public K8sPort updateDeviceId(DeviceId deviceId) {
        return new Builder()
                .networkId(networkId)
                .portId(portId)
                .macAddress(macAddress)
                .ipAddress(ipAddress)
                .deviceId(deviceId)
                .portNumber(portNumber)
                .state(state)
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
        DefaultK8sPort that = (DefaultK8sPort) o;
        return Objects.equal(networkId, that.networkId) &&
                Objects.equal(portId, that.portId) &&
                Objects.equal(macAddress, that.macAddress) &&
                Objects.equal(ipAddress, that.ipAddress) &&
                Objects.equal(deviceId, that.deviceId) &&
                Objects.equal(portNumber, that.portNumber) &&
                state == that.state;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(networkId, portId, macAddress, ipAddress,
                deviceId, portNumber, state);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("networkId", networkId)
                .add("portId", portId)
                .add("macAddress", macAddress)
                .add("ipAddress", ipAddress)
                .add("deviceId", deviceId)
                .add("portNumber", portNumber)
                .add("state", state)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return kubernetes port builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Default builder implementation.
     */
    public static final class Builder implements K8sPort.Builder {

        private String networkId;
        private String portId;
        private MacAddress macAddress;
        private IpAddress ipAddress;
        private DeviceId deviceId;
        private PortNumber portNumber;
        private State state;

        // private constructor not intended to use from external
        private Builder() {
        }

        @Override
        public K8sPort build() {
            checkArgument(networkId != null, NOT_NULL_MSG, "networkId");
            checkArgument(portId != null, NOT_NULL_MSG, "portId");
            checkArgument(macAddress != null, NOT_NULL_MSG, "macAddress");
            checkArgument(ipAddress != null, NOT_NULL_MSG, "ipAddress");

            return new DefaultK8sPort(networkId, portId, macAddress, ipAddress,
                    deviceId, portNumber, state);
        }

        @Override
        public Builder networkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        @Override
        public Builder portId(String portId) {
            this.portId = portId;
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

        @Override
        public Builder state(State state) {
            this.state = state;
            return this;
        }
    }
}
