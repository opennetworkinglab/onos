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
package org.onosproject.k8snode.api;

import com.google.common.base.MoreObjects;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.k8snode.api.Constants.GENEVE_TUNNEL;
import static org.onosproject.k8snode.api.Constants.GRE_TUNNEL;
import static org.onosproject.k8snode.api.Constants.VXLAN_TUNNEL;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;

/**
 * Representation of a kubernetes node.
 */
public class DefaultK8sNode implements K8sNode {

    private final String hostname;
    private final Type type;
    private final DeviceId intgBridge;
    private final IpAddress managementIp;
    private final IpAddress dataIp;
    private final K8sNodeState state;

    private static final String NOT_NULL_MSG = "Node % cannot be null";

    private static final String OVSDB = "ovsdb:";

    /**
     * A default constructor of kubernetes Node.
     *
     * @param hostname          hostname
     * @param type              node type
     * @param intgBridge        integration bridge
     * @param managementIp      management IP address
     * @param dataIp            data IP address
     * @param state             node state
     */
    protected DefaultK8sNode(String hostname, Type type, DeviceId intgBridge,
                             IpAddress managementIp, IpAddress dataIp, K8sNodeState state) {
        this.hostname = hostname;
        this.type = type;
        this.intgBridge = intgBridge;
        this.managementIp = managementIp;
        this.dataIp = dataIp;
        this.state = state;
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
    public IpAddress managementIp() {
        return managementIp;
    }

    @Override
    public IpAddress dataIp() {
        return dataIp;
    }

    @Override
    public K8sNodeState state() {
        return state;
    }

    @Override
    public K8sNode updateState(K8sNodeState newState) {
        return new Builder()
                .hostname(hostname)
                .type(type)
                .intgBridge(intgBridge)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .state(newState)
                .build();
    }

    @Override
    public PortNumber grePortNum() {
        return tunnelPortNum(GRE_TUNNEL);
    }

    @Override
    public PortNumber vxlanPortNum() {
        return tunnelPortNum(VXLAN_TUNNEL);
    }

    @Override
    public PortNumber genevePortNum() {
        return tunnelPortNum(GENEVE_TUNNEL);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DefaultK8sNode) {
            DefaultK8sNode that = (DefaultK8sNode) obj;

            return hostname.equals(that.hostname) &&
                    type == that.type &&
                    intgBridge.equals(that.intgBridge) &&
                    managementIp.equals(that.managementIp) &&
                    dataIp.equals(that.dataIp) &&
                    state == that.state;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, type, intgBridge, managementIp, dataIp, state);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("hostname", hostname)
                .add("type", type)
                .add("intgBridge", intgBridge)
                .add("managementIp", managementIp)
                .add("dataIp", dataIp)
                .add("state", state)
                .toString();
    }

    private PortNumber tunnelPortNum(String tunnelType) {
        if (dataIp == null) {
            return null;
        }
        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        Port port = deviceService.getPorts(intgBridge).stream()
                .filter(p -> p.isEnabled() &&
                        Objects.equals(p.annotations().value(PORT_NAME), tunnelType))
                .findAny().orElse(null);
        return port != null ? port.number() : null;
    }

    /**
     * Returns new builder instance.
     *
     * @return kubernetes node builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns new builder instance with the given node as a default value.
     *
     * @param node kubernetes node
     * @return kubernetes node builder
     */
    public static Builder from(K8sNode node) {
        return new Builder()
                .hostname(node.hostname())
                .type(node.type())
                .intgBridge(node.intgBridge())
                .managementIp(node.managementIp())
                .dataIp(node.dataIp())
                .state(node.state());
    }

    public static final class Builder implements K8sNode.Builder {

        private String hostname;
        private Type type;
        private DeviceId intgBridge;
        private IpAddress managementIp;
        private IpAddress dataIp;
        private K8sNodeState state;

        // private constructor not intended to use from external
        private Builder() {
        }

        @Override
        public K8sNode build() {
            checkArgument(hostname != null, NOT_NULL_MSG, "hostname");
            checkArgument(type != null, NOT_NULL_MSG, "type");
            checkArgument(state != null, NOT_NULL_MSG, "state");
            checkArgument(managementIp != null, NOT_NULL_MSG, "management IP");

            return new DefaultK8sNode(hostname,
                    type,
                    intgBridge,
                    managementIp,
                    dataIp,
                    state);
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
        public Builder state(K8sNodeState state) {
            this.state = state;
            return this;
        }
    }
}
