/*
 * Copyright 2017-present Open Networking Foundation
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

import org.onosproject.routing.fpm.protocol.FpmHeader;

/**
 * Listener for events from the route source.
 */
public interface FpmListener {

    /**
     * Handles an FPM message.
     *
     * @param peer FPM peer
     * @param fpmMessage FPM message
     */
    void fpmMessage(FpmPeer peer, FpmHeader fpmMessage);

    /**
     * Signifies that a new peer has attempted to initiate an FPM connection.
     *
     * @param peer FPM peer
     * @return true if the connection should be admitted, otherwise false
     */
    boolean peerConnected(FpmPeer peer);

    /**
     * Signifies that an FPM connection has been disconnected.
     *
     * @param peer FPM peer
     */
    void peerDisconnected(FpmPeer peer);
}
