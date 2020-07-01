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

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default implementation of host to nodes mapping info.
 */
public final class DefaultHostNodesInfo implements HostNodesInfo {

    private static final String NOT_NULL_MSG = "HostNodesInfo % cannot be null";

    private final IpAddress hostIp;
    private final Set<String> nodes;

    private DefaultHostNodesInfo(IpAddress hostIp, Set<String> nodes) {
        this.hostIp = hostIp;
        this.nodes = ImmutableSet.copyOf(nodes);
    }

    @Override
    public IpAddress hostIp() {
        return hostIp;
    }

    @Override
    public Set<String> nodes() {
        return ImmutableSet.copyOf(nodes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultHostNodesInfo that = (DefaultHostNodesInfo) o;
        return Objects.equals(hostIp, that.hostIp) &&
                Objects.equals(nodes, that.nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostIp, nodes);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("hostIp", hostIp)
                .add("nodes", nodes)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return HostNodesInfo builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements HostNodesInfo.Builder {

        private IpAddress hostIp;
        private Set<String> nodes;

        @Override
        public HostNodesInfo build() {
            checkArgument(hostIp != null, NOT_NULL_MSG, "Host IP address");
            if (nodes == null) {
                nodes = ImmutableSet.of();
            }

            return new DefaultHostNodesInfo(hostIp, nodes);
        }

        @Override
        public HostNodesInfo.Builder hostIp(IpAddress hostIp) {
            this.hostIp = hostIp;
            return this;
        }

        @Override
        public HostNodesInfo.Builder nodes(Set<String> nodes) {
            this.nodes = nodes;
            return this;
        }
    }
}
