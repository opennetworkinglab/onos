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
package org.onosproject.provider.bgp.topology.impl;

import org.onosproject.bgp.controller.BgpCfg;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.bgp.controller.BgpId;
import org.onosproject.bgp.controller.BgpLinkListener;
import org.onosproject.bgp.controller.BgpLocalRib;
import org.onosproject.bgp.controller.BgpNodeListener;
import org.onosproject.bgp.controller.BgpPeer;
import org.onosproject.bgp.controller.BgpPeerManager;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpMessage;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapter implementation for BGP controller.
 */
public class BgpControllerAdapter implements BgpController {
    @Override
    public Iterable<BgpPeer> getPeers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BgpPeer getPeer(BgpId bgpId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void writeMsg(BgpId bgpId, BgpMessage msg) {
        // TODO Auto-generated method stub
    }

    @Override
    public void processBgpPacket(BgpId bgpId, BgpMessage msg) throws BgpParseException {
        // TODO Auto-generated method stub
    }

    @Override
    public void closeConnectedPeers() {
        // TODO Auto-generated method stub
    }

    @Override
    public BgpCfg getConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int connectedPeerCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public BgpLocalRib bgpLocalRibVpn() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BgpLocalRib bgpLocalRib() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BgpPeerManager peerManager() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<BgpId, BgpPeer> connectedPeers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<BgpNodeListener> listener() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<BgpLinkListener> linkListener() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void activeSessionExceptionAdd(String peerId, String exception) {
        return;
    }

    @Override
    public void closedSessionExceptionAdd(String peerId, String exception) {
        return;
    }

    @Override
    public Map<String, List<String>> activeSessionMap() {
        return null;
    }

    @Override
    public Map<String, List<String>> closedSessionMap() {
        return null;
    }

    @Override
    public void addListener(BgpNodeListener listener) {
        // TODO Auto-generated method stub
    }

    @Override
    public void removeListener(BgpNodeListener listener) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addLinkListener(BgpLinkListener listener) {
        // TODO Auto-generated method stub
    }

    @Override
    public void removeLinkListener(BgpLinkListener listener) {
        // TODO Auto-generated method stub
    }
}
