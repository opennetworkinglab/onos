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
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.kubevirtnode.api.Constants.DEFAULT_CLUSTER_NAME;

/**
 * Representation of a KubeVirt node.
 */
public class DefaultKubevirtNode implements KubevirtNode {

    private static final String NOT_NULL_MSG = "Node % cannot be null";
    private static final String OVSDB = "ovsdb:";

    private final String clusterName;
    private final String hostname;
    private final Type type;
    private final DeviceId intgBridge;
    private final IpAddress managementIp;
    private final IpAddress dataIp;
    private final KubevirtNodeState state;
    private final Collection<KubevirtPhyInterface> phyIntfs;

    /**
     * A default constructor of kubevirt node.
     *
     * @param clusterName       clusterName
     * @param hostname          hostname
     * @param type              node type
     * @param intgBridge        integration bridge
     * @param managementIp      management IP address
     * @param dataIp            data IP address
     * @param state             node state
     * @param phyIntfs          physical interfaces
     */
    protected DefaultKubevirtNode(String clusterName, String hostname, Type type,
                                  DeviceId intgBridge, IpAddress managementIp,
                                  IpAddress dataIp, KubevirtNodeState state,
                                  Collection<KubevirtPhyInterface> phyIntfs) {
        this.clusterName = clusterName;
        this.hostname = hostname;
        this.type = type;
        this.intgBridge = intgBridge;
        this.managementIp = managementIp;
        this.dataIp = dataIp;
        this.state = state;
        this.phyIntfs = phyIntfs;
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
                .managementIp(managementIp)
                .dataIp(dataIp)
                .state(newState)
                .phyIntfs(phyIntfs)
                .build();
    }

    @Override
    public Collection<KubevirtPhyInterface> phyIntfs() {
        if (phyIntfs == null) {
            return new ArrayList<>();
        }

        return phyIntfs;
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
                .managementIp(node.managementIp())
                .dataIp(node.dataIp())
                .state(node.state());
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
                managementIp.equals(that.managementIp) &&
                dataIp.equals(that.dataIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterName, hostname, type, intgBridge,
                managementIp, dataIp);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("clusterName", clusterName)
                .add("hostname", hostname)
                .add("type", type)
                .add("intgBridge", intgBridge)
                .add("managementIp", managementIp)
                .add("dataIp", dataIp)
                .add("state", state)
                .toString();
    }

    public static final class Builder implements KubevirtNode.Builder {

        private String clusterName;
        private String hostname;
        private Type type;
        private DeviceId intgBridge;
        private IpAddress managementIp;
        private IpAddress dataIp;
        private KubevirtNodeState state;
        private Collection<KubevirtPhyInterface> phyIntfs;

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
                    managementIp,
                    dataIp,
                    state,
                    phyIntfs
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
    }
}
