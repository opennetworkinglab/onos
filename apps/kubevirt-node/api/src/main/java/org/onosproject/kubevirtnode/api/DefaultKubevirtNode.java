/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.api;

import com.google.common.base.MoreObjects;
import org.apache.commons.lang.StringUtils;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.kubevirtnode.api.Constants.DEFAULT_CLUSTER_NAME;
import static org.onosproject.kubevirtnode.api.Constants.GENEVE;
import static org.onosproject.kubevirtnode.api.Constants.GRE;
import static org.onosproject.kubevirtnode.api.Constants.INTEGRATION_TO_PHYSICAL_PREFIX;
import static org.onosproject.kubevirtnode.api.Constants.STT;
import static org.onosproject.kubevirtnode.api.Constants.VXLAN;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;

/**
 * Representation of a KubeVirt node.
 */
public class DefaultKubevirtNode implements KubevirtNode {

    private static final String NOT_NULL_MSG = "Node % cannot be null";
    private static final String OVSDB = "ovsdb:";
    private static final int PORT_NAME_MAX_LENGTH = 15;

    private final String clusterName;
    private final String hostname;
    private final Type type;
    private final DeviceId intgBridge;
    private final DeviceId tunBridge;
    private final IpAddress managementIp;
    private final IpAddress dataIp;
    private final KubevirtNodeState state;
    private final Collection<KubevirtPhyInterface> phyIntfs;
    private final String gatewayBridgeName;

    /**
     * A default constructor of kubevirt node.
     *
     * @param clusterName       clusterName
     * @param hostname          hostname
     * @param type              node type
     * @param intgBridge        integration bridge
     * @param tunBridge         tunnel bridge
     * @param managementIp      management IP address
     * @param dataIp            data IP address
     * @param state             node state
     * @param phyIntfs          physical interfaces
     * @param gatewayBridgeName  gateway bridge name
     */
    protected DefaultKubevirtNode(String clusterName, String hostname, Type type,
                                  DeviceId intgBridge, DeviceId tunBridge,
                                  IpAddress managementIp, IpAddress dataIp,
                                  KubevirtNodeState state,
                                  Collection<KubevirtPhyInterface> phyIntfs,
                                  String gatewayBridgeName) {
        this.clusterName = clusterName;
        this.hostname = hostname;
        this.type = type;
        this.intgBridge = intgBridge;
        this.tunBridge = tunBridge;
        this.managementIp = managementIp;
        this.dataIp = dataIp;
        this.state = state;
        this.phyIntfs = phyIntfs;
        this.gatewayBridgeName = gatewayBridgeName;
    }

    @Override
    public String clusterName() {
        return clusterName;
    }

    @Override
    public String hostname() {
        return hostname;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public DeviceId ovsdb() {
        return DeviceId.deviceId(OVSDB + managementIp().toString());
    }

    @Override
    public DeviceId intgBridge() {
        return intgBridge;
    }

    @Override
    public DeviceId tunBridge() {
        return tunBridge;
    }

    @Override
    public IpAddress managementIp() {
        return managementIp;
    }

    @Override
    public IpAddress dataIp() {
        return dataIp;
    }

    @Override
    public KubevirtNodeState state() {
        return state;
    }

    @Override
    public KubevirtNode updateState(KubevirtNodeState newState) {
        return new Builder()
                .hostname(hostname)
                .clusterName(clusterName)
                .type(type)
                .intgBridge(intgBridge)
                .tunBridge(tunBridge)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .state(newState)
                .phyIntfs(phyIntfs)
                .gatewayBridgeName(gatewayBridgeName)
                .build();
    }

    @Override
    public KubevirtNode updateIntgBridge(DeviceId deviceId) {
        return new Builder()
                .hostname(hostname)
                .clusterName(clusterName)
                .type(type)
                .intgBridge(deviceId)
                .tunBridge(tunBridge)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .state(state)
                .phyIntfs(phyIntfs)
                .gatewayBridgeName(gatewayBridgeName)
                .build();
    }

    @Override
    public KubevirtNode updateTunBridge(DeviceId deviceId) {
        return new Builder()
                .hostname(hostname)
                .clusterName(clusterName)
                .type(type)
                .intgBridge(intgBridge)
                .tunBridge(deviceId)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .state(state)
                .phyIntfs(phyIntfs)
                .gatewayBridgeName(gatewayBridgeName)
                .build();
    }

    @Override
    public Collection<KubevirtPhyInterface> phyIntfs() {
        if (phyIntfs == null) {
            return new ArrayList<>();
        }

        return phyIntfs;
    }

    @Override
    public Set<PortNumber> physPatchPorts() {
        Set<PortNumber> portNumbers = new HashSet<>();
        for (KubevirtPhyInterface phyIntf : this.phyIntfs()) {
            String portName = structurePortName(
                    INTEGRATION_TO_PHYSICAL_PREFIX + phyIntf.network());
            PortNumber portNumber = patchPort(portName);
            if (portNumber != null) {
                portNumbers.add(portNumber);
            }
        }
        return portNumbers;
    }

    @Override
    public PortNumber vxlanPort() {
        return tunnelPort(VXLAN);
    }

    @Override
    public PortNumber grePort() {
        return tunnelPort(GRE);
    }

    @Override
    public PortNumber genevePort() {
        return tunnelPort(GENEVE);
    }

    @Override
    public PortNumber sttPort() {
        return tunnelPort(STT);
    }

    @Override
    public String gatewayBridgeName() {
        return gatewayBridgeName;
    }

    private PortNumber tunnelPort(String tunnelType) {
        if (dataIp == null) {
            return null;
        }
        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        Port port = deviceService.getPorts(tunBridge).stream()
                .filter(p -> p.isEnabled() &&
                        Objects.equals(p.annotations().value(PORT_NAME), tunnelType))
                .findAny().orElse(null);
        return port != null ? port.number() : null;
    }

    private PortNumber patchPort(String portName) {
        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        Port port = deviceService.getPorts(intgBridge).stream()
                .filter(p -> p.isEnabled() &&
                        Objects.equals(p.annotations().value(PORT_NAME), portName))
                .findAny().orElse(null);
        return port != null ? port.number() : null;
    }

    /**
     * Re-structures the OVS port name.
     * The length of OVS port name should be not large than 15.
     *
     * @param portName  original port name
     * @return re-structured OVS port name
     */
    private String structurePortName(String portName) {

        // The size of OVS port name should not be larger than 15
        if (portName.length() > PORT_NAME_MAX_LENGTH) {
            return StringUtils.substring(portName, 0, PORT_NAME_MAX_LENGTH);
        }

        return portName;
    }

    /**
     * Returns new builder instance.
     *
     * @return kubevirt node builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns new builder instance with the given node as a default value.
     *
     * @param node kubevirt node
     * @return kubevirt node builder
     */
    public static Builder from(KubevirtNode node) {
        return new Builder()
                .hostname(node.hostname())
                .clusterName(node.clusterName())
                .type(node.type())
                .intgBridge(node.intgBridge())
                .tunBridge(node.tunBridge())
                .managementIp(node.managementIp())
                .dataIp(node.dataIp())
                .state(node.state())
                .phyIntfs(node.phyIntfs())
                .gatewayBridgeName(node.gatewayBridgeName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultKubevirtNode that = (DefaultKubevirtNode) o;
        return clusterName.equals(that.clusterName) &&
                hostname.equals(that.hostname) &&
                type == that.type &&
                intgBridge.equals(that.intgBridge) &&
                tunBridge.equals(that.tunBridge) &&
                managementIp.equals(that.managementIp) &&
                dataIp.equals(that.dataIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterName, hostname, type, intgBridge, tunBridge,
                managementIp, dataIp);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("clusterName", clusterName)
                .add("hostname", hostname)
                .add("type", type)
                .add("intgBridge", intgBridge)
                .add("tunBridge", tunBridge)
                .add("managementIp", managementIp)
                .add("dataIp", dataIp)
                .add("state", state)
                .add("phyIntfs", phyIntfs)
                .add("gatewayBridgeName", gatewayBridgeName)
                .toString();
    }

    public static final class Builder implements KubevirtNode.Builder {

        private String clusterName;
        private String hostname;
        private Type type;
        private DeviceId intgBridge;
        private DeviceId tunBridge;
        private IpAddress managementIp;
        private IpAddress dataIp;
        private KubevirtNodeState state;
        private Collection<KubevirtPhyInterface> phyIntfs;
        private String gatewayBridgeName;

        // private constructor not intended to use from external
        private Builder() {
        }

        @Override
        public KubevirtNode build() {
            checkArgument(hostname != null, NOT_NULL_MSG, "hostname");
            checkArgument(type != null, NOT_NULL_MSG, "type");
            checkArgument(state != null, NOT_NULL_MSG, "state");
            checkArgument(managementIp != null, NOT_NULL_MSG, "management IP");

            if (StringUtils.isEmpty(clusterName)) {
                clusterName = DEFAULT_CLUSTER_NAME;
            }

            return new DefaultKubevirtNode(
                    clusterName,
                    hostname,
                    type,
                    intgBridge,
                    tunBridge,
                    managementIp,
                    dataIp,
                    state,
                    phyIntfs,
                    gatewayBridgeName
            );
        }

        @Override
        public Builder clusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        @Override
        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        @Override
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        @Override
        public Builder intgBridge(DeviceId deviceId) {
            this.intgBridge = deviceId;
            return this;
        }

        @Override
        public Builder tunBridge(DeviceId deviceId) {
            this.tunBridge = deviceId;
            return this;
        }

        @Override
        public Builder managementIp(IpAddress managementIp) {
            this.managementIp = managementIp;
            return this;
        }

        @Override
        public Builder dataIp(IpAddress dataIp) {
            this.dataIp = dataIp;
            return this;
        }

        @Override
        public Builder phyIntfs(Collection<KubevirtPhyInterface> phyIntfs) {
            this.phyIntfs = phyIntfs;
            return this;
        }

        @Override
        public Builder state(KubevirtNodeState state) {
            this.state = state;
            return this;
        }

        @Override
        public Builder gatewayBridgeName(String gatewayBridgeName) {
            this.gatewayBridgeName = gatewayBridgeName;
            return this;
        }
    }
}
