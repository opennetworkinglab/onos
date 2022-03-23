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
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default implementation of kubernetes port.
 */
public final class DefaultKubevirtPort implements KubevirtPort {

    private static final String NOT_NULL_MSG = "Port % cannot be null";

    private final String vmName;
    private final String networkId;
    private final MacAddress macAddress;
    private final IpAddress ipAddress;
    private final DeviceId deviceId;
    private final PortNumber portNumber;
    private final Set<String> securityGroups;

    /**
     * Default constructor.
     *
     * @param vmName            VM name
     * @param networkId         network identifier
     * @param macAddress        MAC address
     * @param ipAddress         IP address
     * @param deviceId          device identifier
     * @param portNumber        port number
     * @param securityGroups    security groups
     */
    public DefaultKubevirtPort(String vmName, String networkId, MacAddress macAddress, IpAddress ipAddress,
                               DeviceId deviceId, PortNumber portNumber, Set<String> securityGroups) {
        this.vmName = vmName;
        this.networkId = networkId;
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.deviceId = deviceId;
        this.portNumber = portNumber;
        this.securityGroups = securityGroups;
    }

    @Override
    public String vmName() {
        return vmName;
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
    public DeviceId tenantDeviceId() {
        KubevirtNetworkService networkService =
                DefaultServiceDirectory.getService(KubevirtNetworkService.class);
        KubevirtNodeService nodeService =
                DefaultServiceDirectory.getService(KubevirtNodeService.class);
        KubevirtNetwork network = networkService.network(networkId);
        KubevirtNode node = nodeService.node(deviceId);

        if (network == null || node == null) {
            return null;
        } else {
            return network.tenantDeviceId(node.hostname());
        }
    }

    @Override
    public boolean isTenant() {
        KubevirtNetworkService networkService =
                DefaultServiceDirectory.getService(KubevirtNetworkService.class);
        KubevirtNetwork network = networkService.network(networkId);
        if (network == null) {
            return false;
        } else {
            return network.type() == KubevirtNetwork.Type.VXLAN ||
                    network.type() == KubevirtNetwork.Type.GRE ||
                    network.type() == KubevirtNetwork.Type.GENEVE ||
                    network.type() == KubevirtNetwork.Type.STT;
        }
    }

    @Override
    public PortNumber portNumber() {
        return portNumber;
    }

    @Override
    public KubevirtPort updateIpAddress(IpAddress updateIpAddress) {
        return new Builder()
                .vmName(vmName)
                .networkId(networkId)
                .macAddress(macAddress)
                .ipAddress(updateIpAddress)
                .deviceId(deviceId)
                .portNumber(portNumber)
                .securityGroups(securityGroups)
                .build();
    }

    @Override
    public KubevirtPort updatePortNumber(PortNumber updatedPortNumber) {
        return new Builder()
                .vmName(vmName)
                .networkId(networkId)
                .macAddress(macAddress)
                .ipAddress(ipAddress)
                .deviceId(deviceId)
                .portNumber(updatedPortNumber)
                .securityGroups(securityGroups)
                .build();
    }

    @Override
    public KubevirtPort updateDeviceId(DeviceId updatedDeviceId) {
        return new Builder()
                .vmName(vmName)
                .networkId(networkId)
                .macAddress(macAddress)
                .ipAddress(ipAddress)
                .deviceId(updatedDeviceId)
                .portNumber(portNumber)
                .securityGroups(securityGroups)
                .build();
    }

    @Override
    public Set<String> securityGroups() {
        if (securityGroups != null) {
            return ImmutableSet.copyOf(securityGroups);
        } else {
            return ImmutableSet.of();
        }
    }

    @Override
    public KubevirtPort updateSecurityGroups(Set<String> sgs) {
        return new Builder()
                .vmName(vmName)
                .networkId(networkId)
                .macAddress(macAddress)
                .ipAddress(ipAddress)
                .deviceId(deviceId)
                .portNumber(portNumber)
                .securityGroups(sgs)
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
        return vmName.equals(that.vmName) && networkId.equals(that.networkId) &&
                macAddress.equals(that.macAddress) && ipAddress.equals(that.ipAddress) &&
                deviceId.equals(that.deviceId) && portNumber.equals(that.portNumber) &&
                securityGroups.equals(that.securityGroups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vmName, networkId, macAddress, ipAddress, deviceId, portNumber, securityGroups);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("vmName", vmName)
                .add("networkId", networkId)
                .add("macAddress", macAddress)
                .add("ipAddress", ipAddress)
                .add("deviceId", deviceId)
                .add("portNumber", portNumber)
                .add("securityGroups", securityGroups)
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

        private String vmName;
        private String networkId;
        private MacAddress macAddress;
        private IpAddress ipAddress;
        private DeviceId deviceId;
        private PortNumber portNumber;
        private Set<String> securityGroups;

        // private constructor not intended to use from external
        private Builder() {
        }

        @Override
        public KubevirtPort build() {
            checkArgument(vmName != null, NOT_NULL_MSG, "vmName");
            checkArgument(networkId != null, NOT_NULL_MSG, "networkId");
            checkArgument(macAddress != null, NOT_NULL_MSG, "macAddress");

            return new DefaultKubevirtPort(vmName, networkId, macAddress,
                    ipAddress, deviceId, portNumber, securityGroups);
        }

        @Override
        public KubevirtPort.Builder vmName(String vmName) {
            this.vmName = vmName;
            return this;
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

        @Override
        public Builder securityGroups(Set<String> securityGroups) {
            this.securityGroups = securityGroups;
            return this;
        }
    }
}
