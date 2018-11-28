/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
import org.onosproject.openstacknetworking.api.InstancePort;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of instance port.
 */
public final class DefaultInstancePort implements InstancePort {

    private static final String ANNOTATION_NETWORK_ID = "networkId";
    private static final String ANNOTATION_PORT_ID = "portId";
    private static final String ANNOTATION_CREATE_TIME = "createTime";

    private static final String NOT_NULL_MSG = "Instance Port % cannot be null";

    private final String networkId;
    private final String portId;
    private final MacAddress macAddress;
    private final IpAddress ipAddress;
    private final DeviceId deviceId;
    private final DeviceId oldDeviceId;
    private final PortNumber portNumber;
    private final PortNumber oldPortNumber;
    private final State state;

    // private constructor not intended for invoked from external
    private DefaultInstancePort(String networkId, String portId,
                                MacAddress macAddress, IpAddress ipAddress,
                                DeviceId deviceId, DeviceId oldDeviceId,
                                PortNumber portNumber, PortNumber oldPortNumber,
                                State state) {
        this.networkId = networkId;
        this.portId = portId;
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.deviceId = deviceId;
        this.oldDeviceId = oldDeviceId;
        this.portNumber = portNumber;
        this.oldPortNumber = oldPortNumber;
        this.state = state;
    }

    private DefaultInstancePort(Host host, State state,
                                DeviceId oldDeviceId, PortNumber oldPortNumber) {
        this.networkId = host.annotations().value(ANNOTATION_NETWORK_ID);
        this.portId = host.annotations().value(ANNOTATION_PORT_ID);
        this.macAddress = host.mac();

        this.ipAddress = host.ipAddresses().stream().findFirst().orElse(null);

        this.deviceId = host.location().deviceId();
        this.portNumber = host.location().port();
        this.state = state;
        this.oldDeviceId = oldDeviceId;
        this.oldPortNumber = oldPortNumber;
    }

    /**
     * A constructor fed by host and state.
     *
     * @param host  host object
     * @param state instance port state
     * @return instance port
     */
    public static DefaultInstancePort from(Host host, State state) {
        checkNotNull(host);
        checkArgument(!Strings.isNullOrEmpty(
                                host.annotations().value(ANNOTATION_NETWORK_ID)));
        checkArgument(!Strings.isNullOrEmpty(
                                host.annotations().value(ANNOTATION_PORT_ID)));
        checkArgument(!Strings.isNullOrEmpty(
                                host.annotations().value(ANNOTATION_CREATE_TIME)));

        return new DefaultInstancePort(host, state, null, null);
    }

    /**
     * A constructor fed by host, state, device ID and port number.
     *
     * @param host  host object
     * @param state instance port state
     * @param oldDeviceId device identifier
     * @param oldPortNumber port number
     * @return instance port
     */
    public static DefaultInstancePort from(Host host,
                                           State state,
                                           DeviceId oldDeviceId,
                                           PortNumber oldPortNumber) {
        checkNotNull(host);
        checkArgument(!Strings.isNullOrEmpty(
                host.annotations().value(ANNOTATION_NETWORK_ID)));
        checkArgument(!Strings.isNullOrEmpty(
                host.annotations().value(ANNOTATION_PORT_ID)));
        checkArgument(!Strings.isNullOrEmpty(
                host.annotations().value(ANNOTATION_CREATE_TIME)));

        return new DefaultInstancePort(host, state, oldDeviceId, oldPortNumber);
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
    public DeviceId oldDeviceId() {
        return oldDeviceId;
    }

    @Override
    public PortNumber portNumber() {
        return portNumber;
    }

    @Override
    public PortNumber oldPortNumber() {
        return oldPortNumber;
    }

    @Override
    public State state() {
        return state;
    }

    /**
     * Obtains an instance port builder.
     *
     * @return instance port builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public InstancePort updateState(State newState) {
        return new Builder()
                .networkId(networkId)
                .portId(portId)
                .macAddress(macAddress)
                .ipAddress(ipAddress)
                .deviceId(deviceId)
                .oldDeviceId(oldDeviceId)
                .portNumber(portNumber)
                .oldPortNumber(oldPortNumber)
                .state(newState)
                .build();
    }

    @Override
    public InstancePort updatePrevLocation(DeviceId oldDeviceId,
                                           PortNumber oldPortNumber) {
        return new Builder()
                .networkId(networkId)
                .portId(portId)
                .macAddress(macAddress)
                .ipAddress(ipAddress)
                .deviceId(deviceId)
                .oldDeviceId(oldDeviceId)
                .portNumber(portNumber)
                .oldPortNumber(oldPortNumber)
                .state(state)
                .build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("networkId", networkId)
                .add("portId", portId)
                .add("macAddress", macAddress)
                .add("ipAddress", ipAddress)
                .add("deviceId", deviceId)
                .add("oldDeviceId", oldDeviceId)
                .add("portNumber", portNumber)
                .add("oldPortNumber", oldPortNumber)
                .add("state", state)
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultInstancePort) {
            DefaultInstancePort that = (DefaultInstancePort) obj;
            return Objects.equals(networkId, that.networkId) &&
                    Objects.equals(portId, that.portId) &&
                    Objects.equals(macAddress, that.macAddress) &&
                    Objects.equals(ipAddress, that.ipAddress) &&
                    Objects.equals(deviceId, that.deviceId) &&
                    Objects.equals(oldDeviceId, that.oldDeviceId) &&
                    Objects.equals(portNumber, that.portNumber) &&
                    Objects.equals(oldPortNumber, that.oldPortNumber) &&
                    Objects.equals(state, that.state);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkId,
                portId,
                macAddress,
                ipAddress,
                deviceId,
                oldDeviceId,
                portNumber,
                oldPortNumber,
                state);
    }

    /**
     * A builder class for instance port.
     */
    public static final class Builder implements InstancePort.Builder {

        private String networkId;
        private String portId;
        private MacAddress macAddress;
        private IpAddress ipAddress;
        private DeviceId deviceId;
        private DeviceId oldDeviceId;
        private PortNumber portNumber;
        private PortNumber oldPortNumber;
        private State state;

        // private constructor not intended to use from external
        private Builder() {
        }

        @Override
        public InstancePort build() {

            checkArgument(networkId != null, NOT_NULL_MSG, "networkId");
            checkArgument(portId != null, NOT_NULL_MSG, "portId");
            checkArgument(macAddress != null, NOT_NULL_MSG, "macAddress");
            checkArgument(ipAddress != null, NOT_NULL_MSG, "ipAddress");
            checkArgument(deviceId != null, NOT_NULL_MSG, "deviceId");
            checkArgument(portNumber != null, NOT_NULL_MSG, "portNumber");
            checkArgument(state != null, NOT_NULL_MSG, "state");

            return new DefaultInstancePort(networkId,
                    portId,
                    macAddress,
                    ipAddress,
                    deviceId,
                    oldDeviceId,
                    portNumber,
                    oldPortNumber,
                    state);
        }

        @Override
        public InstancePort.Builder networkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        @Override
        public InstancePort.Builder portId(String portId) {
            this.portId = portId;
            return this;
        }

        @Override
        public InstancePort.Builder macAddress(MacAddress macAddress) {
            this.macAddress = macAddress;
            return this;
        }

        @Override
        public InstancePort.Builder ipAddress(IpAddress ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        @Override
        public InstancePort.Builder deviceId(DeviceId deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        @Override
        public InstancePort.Builder oldDeviceId(DeviceId oldDeviceId) {
            this.oldDeviceId = oldDeviceId;
            return this;
        }

        @Override
        public InstancePort.Builder portNumber(PortNumber portNumber) {
            this.portNumber = portNumber;
            return this;
        }

        @Override
        public InstancePort.Builder oldPortNumber(PortNumber oldPortNumber) {
            this.oldPortNumber = oldPortNumber;
            return this;
        }

        @Override
        public InstancePort.Builder state(State state) {
            this.state = state;
            return this;
        }
    }
}
