/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.routeservice;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.cluster.NodeId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a route.
 */
public class Route {

    private static final String VERSION_MISMATCH =
            "Prefix and next hop must be in the same address family";

    private static final NodeId UNDEFINED = new NodeId("-");

    /**
     * Source of the route.
     */
    public enum Source {
        /**
         * Route came from the iBGP route source.
         */
        BGP,

        /**
         * Route came from the FPM route source.
         */
        FPM,

        /**
         * Route came from the RIP route source.
         */
        RIP,

        /**
         * Route can from the static route source.
         */
        STATIC,

        /**
         * Route can from the DHCP route source.
         */
        DHCP,

        /**
         * Route source was not defined.
         */
        UNDEFINED,

        /**
         * Route can from the DHCP-LQ route source.
         */
        DHCPLQ
    }

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
    public Route(Source source, IpPrefix prefix, IpAddress nextHop) {
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
    public Route(Source source, IpPrefix prefix, IpAddress nextHop, NodeId sourceNode) {
        checkNotNull(prefix);
        checkNotNull(nextHop);
        checkArgument(prefix.version().equals(nextHop.version()), VERSION_MISMATCH);

        this.source = checkNotNull(source);
        this.prefix = prefix;
        this.nextHop = nextHop;
        this.sourceNode = checkNotNull(sourceNode);
    }

    /**
     * Returns the route source.
     *
     * @return route source
     */
    public Source source() {
        return source;
    }

    /**
     * Returns the IP prefix of the route.
     *
     * @return IP prefix
     */
    public IpPrefix prefix() {
        return prefix;
    }

    /**
     * Returns the next hop IP address.
     *
     * @return next hop
     */
    public IpAddress nextHop() {
        return nextHop;
    }

    /**
     * Returns the ONOS node the route was sourced from.
     *
     * @return ONOS node ID
     */
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

        if (!(other instanceof Route)) {
            return false;
        }

        Route that = (Route) other;

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
}
