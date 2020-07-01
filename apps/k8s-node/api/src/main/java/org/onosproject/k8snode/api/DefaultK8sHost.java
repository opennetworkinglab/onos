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
package org.onosproject.k8snode.api;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Representation of a kubernetes host.
 */
public class DefaultK8sHost implements K8sHost {

    private final IpAddress hostIp;
    private final Set<String> nodeNames;
    private final K8sHostState state;

    private static final String NOT_NULL_MSG = "Host % cannot be null";

    private static final String OVSDB = "ovsdb:";

    /**
     * A default constructor of kubernetes host.
     *
     * @param hostIp        host IP address
     * @param nodeNames     node names
     * @param state         host state
     */
    protected DefaultK8sHost(IpAddress hostIp, Set<String> nodeNames,
                             K8sHostState state) {
        this.hostIp = hostIp;
        this.nodeNames = nodeNames;
        this.state = state;
    }

    @Override
    public IpAddress hostIp() {
        return hostIp;
    }

    @Override
    public Set<String> nodeNames() {
        return ImmutableSet.copyOf(nodeNames);
    }

    @Override
    public K8sHostState state() {
        return state;
    }

    @Override
    public DeviceId ovsdb() {
        return DeviceId.deviceId(OVSDB + hostIp.toString());
    }

    @Override
    public K8sHost updateState(K8sHostState newState) {
        return new Builder()
                .hostIp(hostIp)
                .nodeNames(nodeNames)
                .state(newState)
                .build();
    }

    @Override
    public K8sHost updateNodeNames(Set<String> nodeNames) {
        return new Builder()
                .hostIp(hostIp)
                .nodeNames(nodeNames)
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
        DefaultK8sHost that = (DefaultK8sHost) o;
        return Objects.equals(hostIp, that.hostIp) &&
                Objects.equals(nodeNames, that.nodeNames) &&
                state == that.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostIp, nodeNames, state);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("hostIp", hostIp)
                .add("nodeNames", nodeNames)
                .add("state", state)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return kubernetes host builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements K8sHost.Builder {

        private IpAddress hostIp;
        private Set<String> nodeNames;
        private K8sHostState state;

        // private constructor not intended to use from external
        private Builder() {
        }

        @Override
        public K8sHost build() {
            checkArgument(hostIp != null, NOT_NULL_MSG, "hostIp");
            checkArgument(state != null, NOT_NULL_MSG, "state");

            if (nodeNames == null) {
                nodeNames = new HashSet<>();
            }

            return new DefaultK8sHost(hostIp, nodeNames, state);
        }

        @Override
        public Builder hostIp(IpAddress hostIp) {
            this.hostIp = hostIp;
            return this;
        }

        @Override
        public Builder nodeNames(Set<String> nodeNames) {
            this.nodeNames = nodeNames;
            return this;
        }

        @Override
        public Builder state(K8sHostState state) {
            this.state = state;
            return this;
        }
    }
}
