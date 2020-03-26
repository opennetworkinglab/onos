/*
 * Copyright 2015-present Open Networking Foundation
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

import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onosproject.bgp.controller.BgpCfg;
import org.onosproject.bgp.controller.BgpConnectPeer;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.bgp.controller.BgpId;
import org.onosproject.bgp.controller.BgpPeer;
import org.onosproject.bgp.controller.BgpPeerCfg;
import org.onosproject.bgp.controller.impl.BgpControllerImpl.BgpPeerManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * Provides BGP configuration of this BGP speaker.
 */
public class BgpConfig implements BgpCfg {

    private static final Logger log = LoggerFactory.getLogger(BgpConfig.class);

    private static final short DEFAULT_HOLD_TIMER = 120;
    private static final short DEFAULT_CONN_RETRY_TIME = 120;
    private static final short DEFAULT_CONN_RETRY_COUNT = 5;
    private List<BgpConnectPeerImpl> peerList = new ArrayList();
    private State state = State.INIT;
    private int localAs;
    private int maxSession;
    private boolean lsCapability;
    private short holdTime;
    private boolean largeAs = false;
    private int maxConnRetryTime;
    private int maxConnRetryCount;
    private FlowSpec flowSpec = FlowSpec.NONE;
    private Ip4Address routerId = null;
    private TreeMap<String, BgpPeerCfg> bgpPeerTree = new TreeMap<>();
    private BgpConnectPeer connectPeer;
    private BgpPeerManagerImpl peerManager;
    private BgpController bgpController;
    private boolean rpdCapability;
    private boolean evpnCapability;

    //Set default connection type as IPv4
    private ConnectionType connectionType = ConnectionType.IPV4;

    private boolean isRouteRefreshEnabled = false;
    private long periodicTimer;
    private long warmupTimer;
    private long cooldownTimer;

    /*
     * Constructor to initialize the values.
     */
    public BgpConfig(BgpController bgpController) {
        this.bgpController = bgpController;
        this.peerManager = (BgpPeerManagerImpl) bgpController.peerManager();
        this.holdTime = DEFAULT_HOLD_TIMER;
        this.maxConnRetryTime = DEFAULT_CONN_RETRY_TIME;
        this.maxConnRetryCount = DEFAULT_CONN_RETRY_COUNT;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public int getAsNumber() {
        return this.localAs;
    }

    @Override
    public void setAsNumber(int localAs) {

        State localState = getState();
        this.localAs = localAs;

        /* Set configuration state */
        if (localState == State.IP_CONFIGURED) {
            setState(State.IP_AS_CONFIGURED);
        } else {
            setState(State.AS_CONFIGURED);
        }
    }

    @Override
    public int getMaxSession() {
        return this.maxSession;
    }

    @Override
    public void setMaxSession(int maxSession) {
        this.maxSession = maxSession;
    }

    @Override
    public boolean getLsCapability() {
        return this.lsCapability;
    }

    @Override
    public void setLsCapability(boolean lsCapability) {
        this.lsCapability = lsCapability;
    }

    @Override
    public FlowSpec flowSpecCapability() {
        return this.flowSpec;
    }

    @Override
    public void setFlowSpecCapability(FlowSpec flowSpec) {
        this.flowSpec = flowSpec;
    }

    @Override
    public boolean flowSpecRpdCapability() {
        return this.rpdCapability;
    }

    @Override
    public void setFlowSpecRpdCapability(boolean rpdCapability) {
        this.rpdCapability = rpdCapability;
    }

    @Override
    public boolean getEvpnCapability() {
        return this.evpnCapability;
    }

    @Override
    public void setEvpnCapability(boolean evpnCapability) {
        this.evpnCapability = evpnCapability;
    }

    @Override
    public ConnectionType connectionType() {
        return this.connectionType;
    }

    @Override
    public void setConnectionType(ConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    @Override
    public boolean isRouteRefreshEnabled() {
        return this.isRouteRefreshEnabled;
    }

    @Override
    public void setRouteRefreshEnabled(boolean isEnabled) {
        this.isRouteRefreshEnabled = isEnabled;
    }

    @Override
    public long getRouteRefreshPeriodicTimer() {
        return this.periodicTimer;
    }

    @Override
    public void setRouteRefreshPeriodicTimer(long periodicTimer) {
        this.periodicTimer = periodicTimer;
    }

    @Override
    public long getRouteRefreshWarmupTimer() {
        return this.warmupTimer;
    }

    @Override
    public void setRouteRefreshWarmupTimer(long warmupTimer) {
        this.warmupTimer = warmupTimer;
    }

    @Override
    public long getRouteRefreshCooldownTimer() {
        return this.cooldownTimer;
    }

    @Override
    public void setRouteRefreshCooldownTimer(long cooldownTimer) {
        this.cooldownTimer = cooldownTimer;
    }

    @Override
    public String getRouterId() {
        if (this.routerId != null) {
            return this.routerId.toString();
        } else {
            return null;
        }
    }

    @Override
    public void setRouterId(String routerId) {
        State localState = getState();
        this.routerId = Ip4Address.valueOf(routerId);

        /* Set configuration state */
        if (localState == State.AS_CONFIGURED) {
            setState(State.IP_AS_CONFIGURED);
        } else {
            setState(State.IP_CONFIGURED);
        }
    }

    @Override
    public boolean addPeer(String routerid, int remoteAs) {
        return addPeer(routerid, remoteAs, DEFAULT_HOLD_TIMER);
    }

    @Override
    public boolean addPeer(String routerid, short holdTime) {
        return addPeer(routerid, this.getAsNumber(), holdTime);
    }

    @Override
    public boolean addPeer(String routerid, int remoteAs, short holdTime) {
        BgpPeerConfig lspeer = new BgpPeerConfig();
        if (this.bgpPeerTree.get(routerid) == null) {

            lspeer.setPeerRouterId(routerid);
            lspeer.setAsNumber(remoteAs);
            lspeer.setHoldtime(holdTime);
            lspeer.setState(BgpPeerCfg.State.IDLE);
            lspeer.setSelfInnitConnection(false);

            if (this.getAsNumber() == remoteAs) {
                lspeer.setIsIBgp(true);
            } else {
                lspeer.setIsIBgp(false);
            }

            this.bgpPeerTree.put(routerid, lspeer);
            log.debug("Added successfully");
            return true;
        } else {
            log.debug("Already exists");
            return false;
        }
    }

    @Override
    public boolean connectPeer(String routerid) {
        BgpPeerCfg lspeer = this.bgpPeerTree.get(routerid);

        if (lspeer != null) {
            lspeer.setSelfInnitConnection(true);

            if (lspeer.connectPeer() == null) {
                connectPeer = new BgpConnectPeerImpl(bgpController, routerid, Controller.BGP_PORT_NUM);
                lspeer.setConnectPeer(connectPeer);
                connectPeer.connectPeer();
                peerList.add((BgpConnectPeerImpl) connectPeer);

            }
            return true;
        }

        return false;
    }

    @Override
    public boolean removePeer(String routerid) {
        BgpPeerCfg lspeer = this.bgpPeerTree.get(routerid);

        if (lspeer != null) {

            disconnectPeer(routerid);
            lspeer.setSelfInnitConnection(false);
            lspeer = this.bgpPeerTree.remove(routerid);
            log.debug("Deleted : " + routerid + " successfully");

            return true;
        } else {
            log.debug("Did not find : " + routerid);
            return false;
        }
    }

    @Override
    public boolean disconnectPeer(String routerid) {
        BgpPeerCfg lspeer = this.bgpPeerTree.get(routerid);

        if (lspeer != null) {

            BgpPeer disconnPeer = peerManager.getPeer(BgpId.bgpId(IpAddress.valueOf(routerid)));
            if (disconnPeer != null) {
                // TODO: send notification peer deconfigured
                disconnPeer.disconnectPeer();
            } else if (lspeer.connectPeer() != null) {
                lspeer.connectPeer().disconnectPeer();
            }
            lspeer.setState(BgpPeerCfg.State.IDLE);
            lspeer.setSelfInnitConnection(false);
            log.debug("Disconnected : " + routerid + " successfully");

            return true;
        } else {
            log.debug("Did not find : " + routerid);
            return false;
        }
    }

    @Override
    public void setPeerConnState(String routerid, BgpPeerCfg.State state) {
        BgpPeerCfg lspeer = this.bgpPeerTree.get(routerid);

        if (lspeer != null) {
            lspeer.setState(state);
            log.debug("Peer : " + routerid + " is not available");

            return;
        } else {
            log.debug("Did not find : " + routerid);
            return;
        }
    }

    @Override
    public BgpPeerCfg.State getPeerConnState(String routerid) {
        BgpPeerCfg lspeer = this.bgpPeerTree.get(routerid);

        if (lspeer != null) {
            return lspeer.getState();
        } else {
            return BgpPeerCfg.State.INVALID; //No instance
        }
    }

    @Override
    public boolean isPeerConnectable(String routerid) {
        BgpPeerCfg lspeer = this.bgpPeerTree.get(routerid);

        if ((lspeer != null) && lspeer.getState().equals(BgpPeerCfg.State.IDLE)) {
            return true;
        }

        return false;
    }

    @Override
    public TreeMap<String, BgpPeerCfg> getPeerTree() {
        return this.bgpPeerTree;
    }

    @Override
    public TreeMap<String, BgpPeerCfg> displayPeers() {
        if (this.bgpPeerTree.isEmpty()) {
            log.debug("There are no BGP peers");
        } else {
            Set<Entry<String, BgpPeerCfg>> set = this.bgpPeerTree.entrySet();
            Iterator<Entry<String, BgpPeerCfg>> list = set.iterator();
            BgpPeerCfg lspeer;

            while (list.hasNext()) {
                Entry<String, BgpPeerCfg> me = list.next();
                lspeer = me.getValue();
                log.debug("Peer neighbor IP :" + me.getKey());
                log.debug(", AS Number : " + lspeer.getAsNumber());
                log.debug(", Hold Timer : " + lspeer.getHoldtime());
                log.debug(", Is iBGP : " + lspeer.getIsIBgp());
            }
        }
        return null;
    }

    @Override
    public BgpPeerCfg displayPeers(String routerid) {

        if (this.bgpPeerTree.isEmpty()) {
            log.debug("There are no Bgp peers");
        } else {
            return this.bgpPeerTree.get(routerid);
        }
        return null;
    }

    @Override
    public void setHoldTime(short holdTime) {
        this.holdTime = holdTime;
    }

    @Override
    public short getHoldTime() {
        return this.holdTime;
    }

    @Override
    public boolean getLargeASCapability() {
        return this.largeAs;
    }

    @Override
    public void setLargeASCapability(boolean largeAs) {
        this.largeAs = largeAs;
    }

    @Override
    public boolean isPeerConfigured(String routerid) {
        BgpPeerCfg lspeer = this.bgpPeerTree.get(routerid);
        return (lspeer != null) ? true : false;
    }

    @Override
    public boolean isPeerConnected(String routerid) {
        // TODO: is peer connected
        return true;
    }

    @Override
    public int getMaxConnRetryCount() {
        return this.maxConnRetryCount;
    }

    @Override
    public void setMaxConnRetryCout(int retryCount) {
        this.maxConnRetryCount = retryCount;
    }

    @Override
    public int getMaxConnRetryTime() {
        return this.maxConnRetryTime;
    }

    @Override
    public void setMaxConnRetryTime(int retryTime) {
        this.maxConnRetryTime = retryTime;
    }
}
