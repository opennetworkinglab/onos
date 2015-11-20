/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.bgp.controller.impl;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.bgp.controller.BGPCfg;
import org.onosproject.bgp.controller.BGPController;
import org.onosproject.bgp.controller.BGPId;
import org.onosproject.bgp.controller.BGPPeer;
import org.onosproject.bgp.controller.BgpLinkListener;
import org.onosproject.bgp.controller.BgpNodeListener;
import org.onosproject.bgp.controller.BgpPeerManager;
import org.onosproject.bgpio.exceptions.BGPParseException;
import org.onosproject.bgpio.protocol.BGPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
@Service
public class BGPControllerImpl implements BGPController {

    private static final Logger log = LoggerFactory.getLogger(BGPControllerImpl.class);

    protected ConcurrentHashMap<BGPId, BGPPeer> connectedPeers = new ConcurrentHashMap<BGPId, BGPPeer>();

    protected BGPPeerManagerImpl peerManager = new BGPPeerManagerImpl();

    protected Set<BgpNodeListener> bgpNodeListener = new CopyOnWriteArraySet<>();
    protected Set<BgpLinkListener> bgpLinkListener = new CopyOnWriteArraySet<>();

    final Controller ctrl = new Controller(this);

    private BGPConfig bgpconfig = new BGPConfig();

    @Activate
    public void activate() {
        this.ctrl.start();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        // Close all connected peers
        closeConnectedPeers();
        this.ctrl.stop();
        log.info("Stopped");
    }

    @Override
    public Iterable<BGPPeer> getPeers() {
        return this.connectedPeers.values();
    }

    @Override
    public BGPPeer getPeer(BGPId bgpId) {
        return this.connectedPeers.get(bgpId);
    }

    @Override
    public void addListener(BgpNodeListener listener) {
        this.bgpNodeListener.add(listener);
    }

    @Override
    public void removeListener(BgpNodeListener listener) {
        this.bgpNodeListener.remove(listener);
    }

    @Override
    public Set<BgpNodeListener> listener() {
        return bgpNodeListener;
    }

    @Override
    public void addLinkListener(BgpLinkListener listener) {
        this.bgpLinkListener.add(listener);
    }

    @Override
    public void removeLinkListener(BgpLinkListener listener) {
        this.bgpLinkListener.remove(listener);
    }

    @Override
    public Set<BgpLinkListener> linkListener() {
        return bgpLinkListener;
    }

    @Override
    public void writeMsg(BGPId bgpId, BGPMessage msg) {
        this.getPeer(bgpId).sendMessage(msg);
    }

    @Override
    public void processBGPPacket(BGPId bgpId, BGPMessage msg) throws BGPParseException {

        switch (msg.getType()) {
        case OPEN:
            // TODO: Process Open message
            break;
        case KEEP_ALIVE:
            // TODO: Process keepalive message
            break;
        case NOTIFICATION:
            // TODO: Process notificatoin message
            break;
        case UPDATE:
            // TODO: Process update message
            break;
        default:
            // TODO: Process other message
            break;
        }
    }

    @Override
    public void closeConnectedPeers() {
        BGPPeer bgpPeer;
        for (BGPId id : this.connectedPeers.keySet()) {
            bgpPeer = getPeer(id);
            bgpPeer.disconnectPeer();
        }
    }

    /**
     * Implementation of an BGP Peer which is responsible for keeping track of connected peers and the state in which
     * they are.
     */
    public class BGPPeerManagerImpl implements BgpPeerManager {

        private final Logger log = LoggerFactory.getLogger(BGPPeerManagerImpl.class);
        private final Lock peerLock = new ReentrantLock();

        @Override
        public boolean addConnectedPeer(BGPId bgpId, BGPPeer bgpPeer) {

            if (connectedPeers.get(bgpId) != null) {
                this.log.error("Trying to add connectedPeer but found previous " + "value for bgp ip: {}",
                               bgpId.toString());
                return false;
            } else {
                this.log.debug("Added Peer {}", bgpId.toString());
                connectedPeers.put(bgpId, bgpPeer);
                return true;
            }
        }

        @Override
        public boolean isPeerConnected(BGPId bgpId) {
            if (connectedPeers.get(bgpId) == null) {
                this.log.error("Is peer connected: bgpIp {}.", bgpId.toString());
                return false;
            }

            return true;
        }

        @Override
        public void removeConnectedPeer(BGPId bgpId) {
            connectedPeers.remove(bgpId);
        }

        @Override
        public BGPPeer getPeer(BGPId bgpId) {
            return connectedPeers.get(bgpId);
        }

        /**
         * Gets bgp peer instance.
         *
         * @param bgpController controller instance.
         * @param sessionInfo bgp session info.
         * @param pktStats packet statistics.
         * @return BGPPeer peer instance.
         */
        public BGPPeer getBGPPeerInstance(BGPController bgpController, BgpSessionInfoImpl sessionInfo,
                                          BGPPacketStatsImpl pktStats) {
            BGPPeer bgpPeer = new BGPPeerImpl(bgpController, sessionInfo, pktStats);
            return bgpPeer;
        }

    }

    @Override
    public ConcurrentHashMap<BGPId, BGPPeer> connectedPeers() {
        return connectedPeers;
    }

    @Override
    public BGPPeerManagerImpl peerManager() {
        return peerManager;
    }

    @Override
    public BGPCfg getConfig() {
        return this.bgpconfig;
    }

    @Override
    public int connectedPeerCount() {
        return connectedPeers.size();
    }
}
