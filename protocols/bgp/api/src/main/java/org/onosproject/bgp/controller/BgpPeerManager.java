/*
 * Copyright 2015-present Open Networking Laboratory
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
 * Responsible for keeping track of the current set BGPLS peers connected to the system.
 *
 */
public interface BgpPeerManager {

    /**
     * Add connected peer.
     *
     * @param bgpId BGP ID to add
     * @param bgpPeer BGp peer instance
     *
     * @return false if peer already exist, otherwise true
     */
    public boolean addConnectedPeer(BgpId bgpId, BgpPeer bgpPeer);

    /**
     * Validate wheather peer is connected.
     *
     * @param bgpId BGP ID to validate
     *
     * @return true if peer exist, otherwise false
     */
    public boolean isPeerConnected(BgpId bgpId);

    /**
     * Remove connected peer.
     *
     * @param bgpId BGP ID
     */
    public void removeConnectedPeer(BgpId bgpId);

    /**
     * Gets connected peer.
     *
     * @param bgpId BGP ID
     * @return BGPPeer the connected peer, otherwise null
     */
    public BgpPeer getPeer(BgpId bgpId);
}
