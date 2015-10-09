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

import static org.onlab.util.Tools.groupedThreads;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.bgp.controller.BGPCfg;
import org.onosproject.bgp.controller.BGPController;
import org.onosproject.bgp.controller.BGPId;
import org.onosproject.bgp.controller.BGPPacketStats;
import org.onosproject.bgp.controller.BGPPeer;
import org.onosproject.bgpio.protocol.BGPMessage;
import org.onosproject.bgpio.protocol.BGPVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
@Service
public class BGPControllerImpl implements BGPController {

    private static final Logger log = LoggerFactory.getLogger(BGPControllerImpl.class);

    private final ExecutorService executorMsgs = Executors.newFixedThreadPool(32,
                                                                              groupedThreads("onos/bgp",
                                                                                      "event-stats-%d"));

    private final ExecutorService executorBarrier = Executors.newFixedThreadPool(4,
                                                                                 groupedThreads("onos/bgp",
                                                                                                "event-barrier-%d"));
    protected ConcurrentHashMap<BGPId, BGPPeer> connectedPeers = new ConcurrentHashMap<BGPId, BGPPeer>();

    protected BGPPeerManager peerManager = new BGPPeerManager();
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
    public void writeMsg(BGPId bgpId, BGPMessage msg) {
        // TODO: Send message
    }

    @Override
    public void processBGPPacket(BGPId bgpId, BGPMessage msg) {

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
    public class BGPPeerManager {

        private final Logger log = LoggerFactory.getLogger(BGPPeerManager.class);
        private final Lock peerLock = new ReentrantLock();

        /**
         * Add a BGP peer that has just connected to the system.
         *
         * @param bgpId the id of bgp peer to add
         * @param bgpPeer the actual bgp peer object.
         * @return true if added, false otherwise.
         */
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

        /**
         * Checks if the activation for this bgp peer is valid.
         *
         * @param bgpId the id of bgp peer to check
         * @return true if valid, false otherwise
         */
        public boolean isPeerConnected(BGPId bgpId) {
            if (connectedPeers.get(bgpId) == null) {
                this.log.error("Trying to activate peer but is not in " + "connected peer: bgpIp {}. Aborting ..",
                               bgpId.toString());
                return false;
            }

            return true;
        }

        /**
         * Checks if the activation for this bgp peer is valid.
         *
         * @param routerid the routerid of bgp peer to check
         * @return true if valid, false otherwise
         */
        public boolean isPeerConnected(String routerid) {

            final BGPId bgpId;
            bgpId = BGPId.bgpId(IpAddress.valueOf(routerid));

            if (connectedPeers.get(bgpId) != null) {
                this.log.info("Peer connection exist ");
                return true;
            }
            this.log.info("Initiate connect request to " + "peer: bgpIp {}", bgpId.toString());

            return false;
        }

        /**
         * Clear all state in controller peer maps for a bgp peer that has
         * disconnected from the local controller.
         *
         * @param bgpId the id of bgp peer to remove.
         */
        public void removeConnectedPeer(BGPId bgpId) {
            connectedPeers.remove(bgpId);
        }

        /**
         * Clear all state in controller peer maps for a bgp peer that has
         * disconnected from the local controller.
         *
         * @param routerid the router id of bgp peer to remove.
         */
        public void removeConnectedPeer(String routerid) {
            final BGPId bgpId;

            bgpId = BGPId.bgpId(IpAddress.valueOf(routerid));

            connectedPeers.remove(bgpId);
        }

        /**
          * Gets bgp peer for connected peer map.
          *
          * @param routerid router id
          * @return peer if available, null otherwise
          */
        public BGPPeer getPeer(String routerid) {
            final BGPId bgpId;
            bgpId = BGPId.bgpId(IpAddress.valueOf(routerid));

            return connectedPeers.get(bgpId);
        }

        /**
          * Gets bgp peer instance.
          *
          * @param bgpId bgp identifier.
          * @param pv bgp version.
          * @param pktStats packet statistics.
          * @return BGPPeer peer instance.
          */
        public BGPPeer getBGPPeerInstance(BGPId bgpId, BGPVersion pv, BGPPacketStats pktStats) {
            BGPPeer bgpPeer = new BGPPeerImpl();
            bgpPeer.init(bgpId, pv, pktStats);
            return bgpPeer;
        }

    }

    /**
      * Gets controller instance.
      *
      * @return Controller instance.
      */
    public Controller getController() {
        return ctrl;
    }

    /**
      * Gets connected peers.
      *
      * @return connectedPeers from connected Peers Map.
      */
    public ConcurrentHashMap<BGPId, BGPPeer> getConnectedPeers() {
        return connectedPeers;
    }

    /**
      * Gets peer manager.
      *
      * @return peerManager.
      */
    public BGPPeerManager getPeerManager() {
        return peerManager;
    }

    @Override
    public BGPCfg getConfig() {
        return this.bgpconfig;
    }

    @Override
    public int getBGPConnNumber() {
        return connectedPeers.size();
    }
}