/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.openstacknode.impl;

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
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackNode;

import java.util.Objects;

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
    private final DeviceId routerBridge;
    private final IpAddress managementIp;
    private final IpAddress dataIp;
    private final String vlanIntf;
    private final NodeState state;

    protected DefaultOpenstackNode(String hostname,
                                 NodeType type,
                                 DeviceId intgBridge,
                                 DeviceId routerBridge,
                                 IpAddress managementIp,
                                 IpAddress dataIp,
                                 String vlanIntf,
                                 NodeState state) {
        this.hostname = hostname;
        this.type = type;
        this.intgBridge = intgBridge;
        this.routerBridge = routerBridge;
        this.managementIp = managementIp;
        this.dataIp = dataIp;
        this.vlanIntf = vlanIntf;
        this.state = state;
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
        return DeviceId.deviceId("ovsdb:" + managementIp().toString());
    }

    @Override
    public DeviceId intgBridge() {
        return intgBridge;
    }

    @Override
    public DeviceId routerBridge() {
        return routerBridge;
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
    public NodeState state() {
        return state;
    }

    @Override
    public GroupKey gatewayGroupKey(NetworkMode mode) {
        return new DefaultGroupKey(intgBridge.toString().concat(mode.name()).getBytes());
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
            if (Objects.equals(hostname, that.hostname) &&
                    Objects.equals(type, that.type) &&
                    Objects.equals(intgBridge, that.intgBridge) &&
                    Objects.equals(routerBridge, that.routerBridge) &&
                    Objects.equals(managementIp, that.managementIp) &&
                    Objects.equals(dataIp, that.dataIp) &&
                    Objects.equals(vlanIntf, that.vlanIntf)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname,
                type,
                intgBridge,
                routerBridge,
                managementIp,
                dataIp,
                vlanIntf);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("hostname", hostname)
                .add("type", type)
                .add("integrationBridge", intgBridge)
                .add("routerBridge", routerBridge)
                .add("managementIp", managementIp)
                .add("dataIp", dataIp)
                .add("vlanIntf", vlanIntf)
                .add("state", state)
                .toString();
    }

    @Override
    public OpenstackNode updateState(NodeState newState) {
        return new Builder()
                .type(type)
                .hostname(hostname)
                .intgBridge(intgBridge)
                .routerBridge(routerBridge)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .vlanIntf(vlanIntf)
                .state(newState)
                .build();
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
                .routerBridge(osNode.routerBridge())
                .managementIp(osNode.managementIp())
                .dataIp(osNode.dataIp())
                .vlanIntf(osNode.vlanIntf())
                .state(osNode.state());
    }

    public static final class Builder implements OpenstackNode.Builder {

        private String hostname;
        private NodeType type;
        private DeviceId intgBridge;
        private DeviceId routerBridge;
        private IpAddress managementIp;
        private IpAddress dataIp;
        private String vlanIntf;
        private NodeState state;

        private Builder() {
        }

        @Override
        public DefaultOpenstackNode build() {
            checkArgument(hostname != null, "Node hostname cannot be null");
            checkArgument(type != null, "Node type cannot be null");
            checkArgument(intgBridge != null, "Node integration bridge cannot be null");
            checkArgument(managementIp != null, "Node management IP cannot be null");
            checkArgument(state != null, "Node state cannot be null");

            if (type == NodeType.GATEWAY && routerBridge == null) {
                throw new IllegalArgumentException("Router bridge is required for gateway node");
            }
            if (dataIp == null && Strings.isNullOrEmpty(vlanIntf)) {
                throw new IllegalArgumentException("Either data IP or VLAN interface is required");
            }

            return new DefaultOpenstackNode(hostname,
                    type,
                    intgBridge,
                    routerBridge,
                    managementIp,
                    dataIp,
                    vlanIntf,
                    state);
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
        public Builder routerBridge(DeviceId routerBridge) {
            this.routerBridge = routerBridge;
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
        public Builder state(NodeState state) {
            this.state = state;
            return this;
        }
    }
}

