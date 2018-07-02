/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.openstacknode.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.GroupKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.net.AnnotationKeys.PORT_MAC;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.openstacknode.api.Constants.DEFAULT_TUNNEL;
import static org.onosproject.openstacknode.api.Constants.PATCH_INTG_BRIDGE;

/**
 * Representation of a openstack node.
 */
public class DefaultOpenstackNode implements OpenstackNode {

    private final String hostname;
    private final NodeType type;
    private final DeviceId intgBridge;
    private final IpAddress managementIp;
    private final IpAddress dataIp;
    private final String vlanIntf;
    private final String uplinkPort;
    private final NodeState state;
    private final Collection<OpenstackPhyInterface> phyIntfs;
    private final OpenstackAuth auth;
    private final String endPoint;

    private static final String NOT_NULL_MSG = "Node % cannot be null";

    private static final String OVSDB = "ovsdb:";

    /**
     * A default constructor of Openstack Node.
     *
     * @param hostname      hostname
     * @param type          node type
     * @param intgBridge    integration bridge
     * @param managementIp  management IP address
     * @param dataIp        data IP address
     * @param vlanIntf      VLAN interface
     * @param uplinkPort    uplink port name
     * @param state         node state
     * @param phyIntfs      physical interfaces
     * @param auth          keystone authentication info
     * @param endPoint      openstack endpoint URL
     */
    protected DefaultOpenstackNode(String hostname, NodeType type,
                                   DeviceId intgBridge,
                                   IpAddress managementIp,
                                   IpAddress dataIp,
                                   String vlanIntf,
                                   String uplinkPort,
                                   NodeState state,
                                   Collection<OpenstackPhyInterface> phyIntfs,
                                   OpenstackAuth auth,
                                   String endPoint) {
        this.hostname = hostname;
        this.type = type;
        this.intgBridge = intgBridge;
        this.managementIp = managementIp;
        this.dataIp = dataIp;
        this.vlanIntf = vlanIntf;
        this.uplinkPort = uplinkPort;
        this.state = state;
        this.phyIntfs = phyIntfs;
        this.auth = auth;
        this.endPoint = endPoint;
    }

    @Override
    public String hostname() {
        return hostname;
    }

    @Override
    public NodeType type() {
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
    public IpAddress managementIp() {
        return managementIp;
    }

    @Override
    public IpAddress dataIp() {
        return dataIp;
    }

    @Override
    public String vlanIntf() {
        return vlanIntf;
    }

    @Override
    public String uplinkPort() {
        return uplinkPort;
    }

    @Override
    public NodeState state() {
        return state;
    }

    @Override
    public GroupKey gatewayGroupKey(NetworkMode mode) {
        return new DefaultGroupKey(intgBridge.toString().concat(mode.name()).getBytes());
    }

    @Override
    public PortNumber uplinkPortNum() {
        if (uplinkPort == null) {
            return null;
        }

        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        Port port = deviceService.getPorts(intgBridge).stream()
                .filter(p -> p.isEnabled() &&
                        Objects.equals(p.annotations().value(PORT_NAME), uplinkPort))
                .findAny().orElse(null);

        return port != null ? port.number() : null;

    }
    @Override
    public PortNumber tunnelPortNum() {
        if (dataIp == null) {
            return null;
        }
        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        Port port = deviceService.getPorts(intgBridge).stream()
                .filter(p -> p.isEnabled() &&
                        Objects.equals(p.annotations().value(PORT_NAME), DEFAULT_TUNNEL))
                .findAny().orElse(null);
        return port != null ? port.number() : null;
    }

    @Override
    public PortNumber vlanPortNum() {
        if (vlanIntf == null) {
            return null;
        }
        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        Port port = deviceService.getPorts(intgBridge).stream()
                .filter(p -> p.isEnabled() &&
                        Objects.equals(p.annotations().value(PORT_NAME), vlanIntf))
                .findAny().orElse(null);
        return port != null ? port.number() : null;
    }

    @Override
    public PortNumber patchPortNum() {
        if (type == NodeType.COMPUTE) {
            return null;
        }
        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        Port port = deviceService.getPorts(intgBridge).stream()
                .filter(p -> p.isEnabled() &&
                        Objects.equals(p.annotations().value(PORT_NAME), PATCH_INTG_BRIDGE))
                .findAny().orElse(null);
        return port != null ? port.number() : null;
    }

    @Override
    public MacAddress vlanPortMac() {
        if (vlanIntf == null) {
            return null;
        }
        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        Port port = deviceService.getPorts(intgBridge).stream()
                .filter(p -> p.annotations().value(PORT_NAME).equals(vlanIntf))
                .findAny().orElse(null);
        return port != null ? MacAddress.valueOf(port.annotations().value(PORT_MAC)) : null;
    }

    @Override
    public GroupId gatewayGroupId(NetworkMode mode) {
        return new GroupId(intgBridge.toString().concat(mode.name()).hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DefaultOpenstackNode) {
            DefaultOpenstackNode that = (DefaultOpenstackNode) obj;
            return Objects.equals(hostname, that.hostname) &&
                    Objects.equals(type, that.type) &&
                    Objects.equals(intgBridge, that.intgBridge) &&
                    Objects.equals(managementIp, that.managementIp) &&
                    Objects.equals(dataIp, that.dataIp) &&
                    Objects.equals(uplinkPort, that.uplinkPort) &&
                    Objects.equals(vlanIntf, that.vlanIntf) &&
                    Objects.equals(phyIntfs, that.phyIntfs) &&
                    Objects.equals(auth, that.auth) &&
                    Objects.equals(endPoint, that.endPoint);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname,
                type,
                intgBridge,
                managementIp,
                dataIp,
                vlanIntf,
                uplinkPort,
                phyIntfs,
                auth,
                endPoint);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("hostname", hostname)
                .add("type", type)
                .add("integrationBridge", intgBridge)
                .add("managementIp", managementIp)
                .add("dataIp", dataIp)
                .add("vlanIntf", vlanIntf)
                .add("uplinkPort", uplinkPort)
                .add("state", state)
                .add("phyIntfs", phyIntfs)
                .add("auth", auth)
                .add("endpoint", endPoint)
                .toString();
    }

    @Override
    public OpenstackNode updateState(NodeState newState) {
        return new Builder()
                .type(type)
                .hostname(hostname)
                .intgBridge(intgBridge)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .vlanIntf(vlanIntf)
                .uplinkPort(uplinkPort)
                .state(newState)
                .phyIntfs(phyIntfs)
                .authentication(auth)
                .endPoint(endPoint)
                .build();
    }

    @Override
    public Collection<OpenstackPhyInterface> phyIntfs() {

        if (phyIntfs == null) {
            return new ArrayList<>();
        }

        return phyIntfs;
    }

    @Override
    public PortNumber phyIntfPortNum(String providerPhysnet) {
        Optional<OpenstackPhyInterface> openstackPhyInterface =
                phyIntfs.stream().filter(p -> p.network().equals(providerPhysnet)).findAny();

        if (openstackPhyInterface.isPresent()) {
            DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
            Port port = deviceService.getPorts(intgBridge).stream()
                    .filter(p -> p.isEnabled() &&
                            Objects.equals(p.annotations().value(PORT_NAME), openstackPhyInterface.get().intf()))
                    .findAny().orElse(null);

            return port != null ? port.number() : null;
        } else {
            return null;
        }

    }

    @Override
    public OpenstackAuth authentication() {
        return auth;
    }

    @Override
    public String endPoint() {
        return endPoint;
    }

    /**
     * Returns new builder instance.
     *
     * @return openstack node builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns new builder instance with the given node as a default value.
     *
     * @param osNode openstack node
     * @return openstack node builder
     */
    public static Builder from(OpenstackNode osNode) {
        return new Builder()
                .hostname(osNode.hostname())
                .type(osNode.type())
                .intgBridge(osNode.intgBridge())
                .managementIp(osNode.managementIp())
                .dataIp(osNode.dataIp())
                .vlanIntf(osNode.vlanIntf())
                .uplinkPort(osNode.uplinkPort())
                .state(osNode.state())
                .phyIntfs(osNode.phyIntfs())
                .authentication(osNode.authentication())
                .endPoint(osNode.endPoint());
    }

    /**
     * A builder class for openstack Node.
     */
    public static final class Builder implements OpenstackNode.Builder {

        private String hostname;
        private NodeType type;
        private DeviceId intgBridge;
        private IpAddress managementIp;
        private IpAddress dataIp;
        private String vlanIntf;
        private String uplinkPort;
        private NodeState state;
        private Collection<OpenstackPhyInterface> phyIntfs;
        private OpenstackAuth auth;
        private String endPoint;

        // private constructor not intended to use from external
        private Builder() {
        }

        @Override
        public DefaultOpenstackNode build() {
            checkArgument(hostname != null, NOT_NULL_MSG, "hostname");
            checkArgument(type != null, NOT_NULL_MSG, "type");
            checkArgument(state != null, NOT_NULL_MSG, "state");
            checkArgument(managementIp != null, NOT_NULL_MSG, "management IP");

            if (type != NodeType.CONTROLLER) {
                checkArgument(intgBridge != null, NOT_NULL_MSG, "integration bridge");

                if (dataIp == null && Strings.isNullOrEmpty(vlanIntf)) {
                    throw new IllegalArgumentException("Either data IP or VLAN interface is required");
                }
            } else {
                checkArgument(endPoint != null, NOT_NULL_MSG, "endpoint URL");
            }

            if (type == NodeType.GATEWAY && uplinkPort == null) {
                throw new IllegalArgumentException("Uplink port is required for gateway node");
            }

            return new DefaultOpenstackNode(hostname,
                    type,
                    intgBridge,
                    managementIp,
                    dataIp,
                    vlanIntf,
                    uplinkPort,
                    state,
                    phyIntfs,
                    auth,
                    endPoint);
        }

        @Override
        public Builder hostname(String hostname) {
            if (!Strings.isNullOrEmpty(hostname)) {
                this.hostname = hostname;
            }
            return this;
        }

        @Override
        public Builder type(NodeType type) {
            this.type = type;
            return this;
        }

        @Override
        public Builder intgBridge(DeviceId intgBridge) {
            this.intgBridge = intgBridge;
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
        public Builder vlanIntf(String vlanIntf) {
            this.vlanIntf = vlanIntf;
            return this;
        }

        @Override
        public Builder uplinkPort(String uplinkPort) {
            this.uplinkPort = uplinkPort;
            return this;
        }

        @Override
        public Builder state(NodeState state) {
            this.state = state;
            return this;
        }

        @Override
        public Builder phyIntfs(Collection<OpenstackPhyInterface> phyIntfs) {
            this.phyIntfs = phyIntfs;
            return this;
        }

        @Override
        public Builder authentication(OpenstackAuth auth) {
            this.auth = auth;
            return this;
        }

        @Override
        public Builder endPoint(String endPoint) {
            this.endPoint = endPoint;
            return this;
        }
    }
}

