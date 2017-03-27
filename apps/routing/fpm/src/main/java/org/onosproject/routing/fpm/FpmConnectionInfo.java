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

import org.onosproject.cluster.NodeId;

/**
 * Information about an FPM connection.
 */
public class FpmConnectionInfo {

    private final NodeId connectedTo;
    private final long connectTime;
    private final FpmPeer peer;

    /**
     * Creates a new connection info.
     *
     * @param connectedTo ONOS node the FPM peer is connected to
     * @param peer FPM peer
     * @param connectTime time the connection was made
     */
    public FpmConnectionInfo(NodeId connectedTo, FpmPeer peer, long connectTime) {
        this.connectedTo = connectedTo;
        this.peer = peer;
        this.connectTime = connectTime;
    }

    /**
     * Returns the node the FPM peers is connected to.
     *
     * @return ONOS node
     */
    public NodeId connectedTo() {
        return connectedTo;
    }

    /**
     * Returns the FPM peer.
     *
     * @return FPM peer
     */
    public FpmPeer peer() {
        return peer;
    }

    /**
     * Returns the time the connection was made.
     *
     * @return connect time
     */
    public long connectTime() {
        return connectTime;
    }
}
