/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.iptopology.api;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

/**
 * This class provides Prefix Identifier details.
 */
public class PrefixIdentifier {
    private final TopologyId topologyId;
    private final RouteType routeType;
    private final IpReachability ipReach;

    /**
     * Constructor to initialize its parameters.
     *
     * @param topologyId topology ID of prefix
     * @param routeType OSPF Route type of the prefix
     * @param ipReach IP address prefix reachability information
     */
    public PrefixIdentifier(TopologyId topologyId, RouteType routeType, IpReachability ipReach) {
        this.topologyId = topologyId;
        this.routeType = routeType;
        this.ipReach = ipReach;
    }

    /**
     * Provides topology ID of prefix.
     *
     * @return topology id
     */
    public TopologyId topologyId() {
        return this.topologyId;
    }

    /**
     * Provides IP address prefix reachability information.
     *
     * @return IP address prefix
     */
    public IpReachability ipReach() {
        return this.ipReach;
    }

    /**
     * Provides OSPF Route type of the prefix.
     *
     * @return Route type
     */
    public RouteType routeType() {
        return this.routeType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(topologyId, routeType, ipReach);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof PrefixIdentifier) {
            PrefixIdentifier other = (PrefixIdentifier) obj;
            return Objects.equals(topologyId, other.topologyId) && Objects.equals(routeType, other.routeType)
                   && Objects.equals(ipReach, other.ipReach);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .omitNullValues()
                .add("routeType", routeType)
                .add("ipReach", ipReach)
                .add("topologyId", topologyId)
                .toString();
    }
}