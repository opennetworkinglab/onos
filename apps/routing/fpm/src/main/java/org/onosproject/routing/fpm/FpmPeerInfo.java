/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.routing.fpm;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Information about an FPM peer.
 */
public class FpmPeerInfo {

    private final Collection<FpmConnectionInfo> connections;
    private final int routes;

    /**
     * Class constructor.
     *
     * @param connections connection information for the peer
     * @param routes number of routes the peer has sent to this node
     */
    public FpmPeerInfo(Collection<FpmConnectionInfo> connections, int routes) {
        this.connections = checkNotNull(connections);
        this.routes = routes;
    }

    /**
     * Returns connection information for the peer.
     *
     * @return collection of connection information
     */
    public Collection<FpmConnectionInfo> connections() {
        return connections;
    }

    /**
     * Returns number of routes sent to this node.
     *
     * @return number of routes
     */
    public int routes() {
        return routes;
    }
}
