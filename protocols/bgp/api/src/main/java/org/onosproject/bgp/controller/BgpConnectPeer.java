/*
 * Copyright 2015-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.onosproject.bgp.controller;

/**
 * Abstraction of an BGP connect peer, initiate remote connection to BGP peer on configuration.
 */
public interface BgpConnectPeer {
    /**
     * Initiate bgp peer connection.
     */
    void connectPeer();

    /**
     * End bgp peer connection.
     */
    void disconnectPeer();

    /**
     * Returns the peer port.
     *
     * @return PeerPort
     */
    int getPeerPort();

    /**
     * Returns the connect retry counter.
     *
     * @return connectRetryCounter
     */
    int getConnectRetryCounter();


}
