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

package org.onosproject.simplefabric.impl;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.cluster.NodeId;
import org.onosproject.simplefabric.api.FabricRoute;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a route.
 */
public final class DefaultFabricRoute implements FabricRoute {

    private static final String VERSION_MISMATCH =
            "Prefix and next hop must be in the same address family";

    private static final NodeId UNDEFINED = new NodeId("-");

    private final Source source;
    private final IpPrefix prefix;
    private final IpAddress nextHop;
    private final NodeId sourceNode;

    /**
     * Creates a route.
     *
     * @param source route source
     * @param prefix IP prefix
     * @param nextHop next hop IP address
     */
    private DefaultFabricRoute(Source source, IpPrefix prefix, IpAddress nextHop) {
        this(source, prefix, nextHop, UNDEFINED);
    }

    /**
     * Creates a route.
     *
     * @param source route source
     * @param prefix IP prefix
     * @param nextHop next hop IP address
     * @param sourceNode ONOS node the route was sourced from
     */
    private DefaultFabricRoute(Source source, IpPrefix prefix,
                              IpAddress nextHop, NodeId sourceNode) {
        this.source = checkNotNull(source);
        this.prefix = prefix;
        this.nextHop = nextHop;
        this.sourceNode = checkNotNull(sourceNode);
    }

    @Override
    public Source source() {
        return source;
    }

    @Override
    public IpPrefix prefix() {
        return prefix;
    }

    @Override
    public IpAddress nextHop() {
        return nextHop;
    }

    @Override
    public NodeId sourceNode() {
        return sourceNode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, nextHop);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof DefaultFabricRoute)) {
            return false;
        }

        DefaultFabricRoute that = (DefaultFabricRoute) other;

        return Objects.equals(this.prefix, that.prefix) &&
                Objects.equals(this.nextHop, that.nextHop);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("prefix", prefix)
                .add("nextHop", nextHop)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return fabric route builder
     */
    public static DefaultFabricRouteBuilder builder() {
        return new DefaultFabricRouteBuilder();
    }

    /**
     * A builder class for fabric route.
     */
    public static final class DefaultFabricRouteBuilder implements Builder {
        private Source source;
        private IpPrefix prefix;
        private IpAddress nextHop;
        private NodeId sourceNode;

        private DefaultFabricRouteBuilder() {
        }

        @Override
        public Builder source(Source source) {
            this.source = source;
            return this;
        }

        @Override
        public Builder prefix(IpPrefix prefix) {
            this.prefix = prefix;
            return this;
        }

        @Override
        public Builder nextHop(IpAddress nextHop) {
            this.nextHop = nextHop;
            return this;
        }

        @Override
        public Builder sourceNode(NodeId sourceNode) {
            this.sourceNode = sourceNode;
            return this;
        }

        @Override
        public FabricRoute build() {

            checkNotNull(prefix);
            checkNotNull(nextHop);
            checkArgument(prefix.version().equals(nextHop.version()), VERSION_MISMATCH);

            if (sourceNode != null) {
                return new DefaultFabricRoute(source, prefix, nextHop, sourceNode);
            } else {
                return new DefaultFabricRoute(source, prefix, nextHop);
            }
        }
    }
}
