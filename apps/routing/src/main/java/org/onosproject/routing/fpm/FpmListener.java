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

package org.onosproject.routing.fpm;

import org.onosproject.routing.fpm.protocol.FpmHeader;

import java.net.SocketAddress;

/**
 * Listener for events from the route source.
 */
public interface FpmListener {

    /**
     * Handles an FPM message.
     *
     * @param fpmMessage FPM message
     */
    void fpmMessage(FpmHeader fpmMessage);

    /**
     * Signifies that a new peer has attempted to initiate an FPM connection.
     *
     * @param address remote address of the peer
     * @return true if the connection should be admitted, otherwise false
     */
    boolean peerConnected(SocketAddress address);

    /**
     * Signifies that an FPM connection has been disconnected.
     *
     * @param address remote address of the peer
     */
    void peerDisconnected(SocketAddress address);
}
