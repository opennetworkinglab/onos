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
package org.onosproject.openstacknode;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.openstacknode.OpenstackNodeEvent.NodeState;
import org.onosproject.openstacknode.OpenstackNodeService.NodeType;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.openstacknode.OpenstackNodeEvent.NodeState.INIT;

/**
 * Representation of a compute/gateway node for OpenstackSwitching/Routing service.
 */
public final class OpenstackNode {

    private final String hostname;
    private final NodeType type;
    private final IpAddress managementIp;
    private final IpAddress dataIp;
    private final DeviceId integrationBridge;
    private final Optional<DeviceId> routerBridge;
    private final NodeState state;

    public static final Comparator<OpenstackNode> OPENSTACK_NODE_COMPARATOR =
            (node1, node2) -> node1.hostname().compareTo(node2.hostname());

    private OpenstackNode(String hostname,
                          NodeType type,
                          IpAddress managementIp,
                          IpAddress dataIp,
                          DeviceId integrationBridge,
                          Optional<DeviceId> routerBridge,
                          NodeState state) {
        this.hostname = hostname;
        this.type = type;
        this.managementIp = managementIp;
        this.dataIp = dataIp;
        this.integrationBridge = integrationBridge;
        this.routerBridge = routerBridge;
        this.state = state;
    }

    /**
     * Returns OpenStack node with new state.
     *
     * @param node openstack node
     * @param state openstack node init state
     * @return openstack node
     */
    public static OpenstackNode getUpdatedNode(OpenstackNode node, NodeState state) {
        return new OpenstackNode(node.hostname,
                node.type,
                node.managementIp,
                node.dataIp,
                node.integrationBridge,
                node.routerBridge,
                state);
    }

    /**
     * Returns hostname of the node.
     *
     * @return hostname
     */
    public String hostname() {
        return hostname;
    }

    /**
     * Returns the type of the node.
     *
     * @return node type
     */
    public NodeType type() {
        return type;
    }

    /**
     * Returns the management network IP address of the node.
     *
     * @return management network ip address
     */
    public IpAddress managementIp() {
        return managementIp;
    }

    /**
     * Returns the data network IP address of the node.
     *
     * @return data network ip address
     */
    public IpAddress dataIp() {
        return dataIp;
    }

    /**
     * Returns the integration bridge device ID.
     *
     * @return device id
     */
    public DeviceId intBridge() {
        return integrationBridge;
    }

    /**
     * Returns the router bridge device ID.
     * It returns valid value only if the node type is GATEWAY.
     *
     * @return device id; or empty device id
     */
    public Optional<DeviceId> routerBridge() {
        return routerBridge;
    }

    /**
     * Returns the init state of the node.
     *
     * @return init state
     */
    public NodeState state() {
        return state;
    }

    /**
     * Returns the device ID of the OVSDB session of the node.
     *
     * @return device id
     */
    public DeviceId ovsdbId() {
        return DeviceId.deviceId("ovsdb:" + managementIp.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof OpenstackNode) {
            OpenstackNode that = (OpenstackNode) obj;
            if (Objects.equals(hostname, that.hostname) &&
                    Objects.equals(type, that.type) &&
                    Objects.equals(managementIp, that.managementIp) &&
                    Objects.equals(dataIp, that.dataIp) &&
                    Objects.equals(integrationBridge, that.integrationBridge) &&
                    Objects.equals(routerBridge, that.routerBridge)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname,
                type,
                managementIp,
                dataIp,
                integrationBridge,
                routerBridge);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("hostname", hostname)
                .add("type", type)
                .add("managementIp", managementIp)
                .add("dataIp", dataIp)
                .add("integrationBridge", integrationBridge)
                .add("routerBridge", routerBridge)
                .add("state", state)
                .toString();
    }

    /**
     * Returns a new builder instance.
     *
     * @return openstack node builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of OpenStack node entities.
     */
    public static final class Builder {
        private String hostname;
        private NodeType type;
        private IpAddress managementIp;
        private IpAddress dataIp;
        private DeviceId integrationBridge;
        private Optional<DeviceId> routerBridge = Optional.empty();
        private NodeState state = INIT;

        private Builder() {
        }

        public OpenstackNode build() {
            checkArgument(!Strings.isNullOrEmpty(hostname));
            checkNotNull(type);
            checkNotNull(managementIp);
            checkNotNull(dataIp);
            checkNotNull(integrationBridge);
            checkNotNull(routerBridge);
            return new OpenstackNode(hostname,
                    type,
                    managementIp,
                    dataIp,
                    integrationBridge,
                    routerBridge,
                    state);
        }

        /**
         * Returns node builder with the hostname.
         *
         * @param hostname hostname
         * @return openstack node builder
         */
        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        /**
         * Returns node builder with the node type.
         *
         * @param type openstack node type
         * @return openstack node builder
         */
        public Builder type(NodeType type) {
            this.type = type;
            return this;
        }

        /**
         * Returns node builder with the management network IP address.
         *
         * @param managementIp management ip address
         * @return openstack node builder
         */
        public Builder managementIp(IpAddress managementIp) {
            this.managementIp = managementIp;
            return this;
        }

        /**
         * Returns node builder with the data network IP address.
         *
         * @param dataIp data network ip address
         * @return openstack node builder
         */
        public Builder dataIp(IpAddress dataIp) {
            this.dataIp = dataIp;
            return this;
        }

        /**
         * Returns node builder with the integration bridge ID.
         *
         * @param integrationBridge integration bridge device id
         * @return openstack node builder
         */
        public Builder integrationBridge(DeviceId integrationBridge) {
            this.integrationBridge = integrationBridge;
            return this;
        }

        /**
         * Returns node builder with the router bridge ID.
         *
         * @param routerBridge router bridge device ID
         * @return openstack node builder
         */
        public Builder routerBridge(DeviceId routerBridge) {
            this.routerBridge = Optional.ofNullable(routerBridge);
            return this;
        }

        /**
         * Returns node builder with the init state.
         *
         * @param state node init state
         * @return openstack node builder
         */
        public Builder state(NodeState state) {
            this.state = state;
            return this;
        }
    }
}

