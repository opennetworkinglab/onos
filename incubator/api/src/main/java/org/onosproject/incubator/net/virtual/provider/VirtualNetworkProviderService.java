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
package org.onosproject.incubator.net.virtual.provider;

import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.provider.ProviderService;

import java.util.Set;

/**
 * Service through which virtual network providers can inject information into
 * the core.
 */
public interface VirtualNetworkProviderService extends ProviderService<VirtualNetworkProvider> {

    /**
     * Set of separate topology clusters expressed in terms of connect points which
     * belong to the same SCC of the underlying topology.
     *
     * @param clusters set of sets of mutually reachable connection points;
     *                 the outer sets are not mutually reachable
     */
    void topologyChanged(Set<Set<ConnectPoint>> clusters);

    // TBD: Is the above sufficient to determine health/viability of virtual entities based on
    // clustering (SCC) of the physical ones?

    /**
     * This method is used to notify the VirtualNetwork service that a tunnel is now ACTIVE.
     *
     * @param networkId network identifier
     * @param src       source connection point
     * @param dst       destination connection point
     * @param tunnelId  tunnel identifier
     */
    void tunnelUp(NetworkId networkId, ConnectPoint src, ConnectPoint dst, TunnelId tunnelId);

    /**
     * This method is used to notify the VirtualNetwork service that a tunnel is now
     * FAILED or INACTIVE.
     *
     * @param networkId network identifier
     * @param src       source connection point
     * @param dst       destination connection point
     * @param tunnelId  tunnel identifier
     */
    void tunnelDown(NetworkId networkId, ConnectPoint src, ConnectPoint dst, TunnelId tunnelId);

}
