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
package org.onosproject.incubator.net.virtual;

import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.provider.Provider;

/**
 * Entity capable of providing traffic isolation constructs for use in
 * implementation of virtual devices and virtual links.
 */
public interface VirtualNetworkProvider extends Provider {

    /**
     * Indicates whether or not the specified connect points on the underlying
     * network are traversable/reachable.
     *
     * @param src source connection point
     * @param dst destination connection point
     * @return true if the destination is reachable from the source
     */
    boolean isTraversable(ConnectPoint src, ConnectPoint dst);

    // TODO: Further enhance this interface to support the virtual intent programming across this boundary.

    /**
     * Creates a network tunnel for all traffic from the specified source
     * connection point to the indicated destination connection point.
     *
     * @param networkId virtual network identifier
     * @param src       source connection point
     * @param dst       destination connection point
     * @return new tunnel's id
     */
    TunnelId createTunnel(NetworkId networkId, ConnectPoint src, ConnectPoint dst);

    /**
     * Destroys the specified network tunnel.
     *
     * @param networkId virtual network identifier
     * @param tunnelId  tunnel identifier
     */
    void destroyTunnel(NetworkId networkId, TunnelId tunnelId);

}
