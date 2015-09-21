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

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.onlab.packet.Ip4Address;
import org.onosproject.bgp.controller.BGPCfg;
import org.onosproject.bgp.controller.BGPPeerCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides BGP configuration of this BGP speaker.
 */
public class BGPConfig implements BGPCfg {

    protected static final Logger log = LoggerFactory.getLogger(BGPConfig.class);

    private static final short DEFAULT_HOLD_TIMER = 120;
    private static final short DEFAULT_CONN_RETRY_TIME = 120;
    private static final short DEFAULT_CONN_RETRY_COUNT = 5;

    private State state = State.INIT;
    private int localAs;
    private int maxSession;
    private boolean lsCapability;
    private short holdTime;
    private boolean largeAs = false;
    private int maxConnRetryTime;
    private int maxConnRetryCount;

    private Ip4Address routerId = null;
    private TreeMap<String, BGPPeerCfg> bgpPeerTree = new TreeMap<>();

    /**
     * Constructor to initialize the values.
     */
    public BGPConfig() {

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
        BGPPeerConfig lspeer = new BGPPeerConfig();
        if (this.bgpPeerTree.get(routerid) == null) {

            lspeer.setPeerRouterId(routerid);
            lspeer.setAsNumber(remoteAs);
            lspeer.setHoldtime(holdTime);
            lspeer.setState(BGPPeerCfg.State.IDLE);
            lspeer.setSelfInnitConnection(false);

            if (this.getAsNumber() == remoteAs) {
                lspeer.setIsIBgp(true);
            } else {
                lspeer.setIsIBgp(false);
            }

            this.bgpPeerTree.put(routerid, lspeer);
            log.debug("added successfully");
            return true;
        } else {
            log.debug("already exists");
            return false;
        }
    }

    @Override
    public boolean connectPeer(String routerid) {
        BGPPeerCfg lspeer = this.bgpPeerTree.get(routerid);

        if (lspeer != null) {
            lspeer.setSelfInnitConnection(true);
            // TODO: initiate peer connection
            return true;
        }

        return false;
    }

    @Override
    public boolean removePeer(String routerid) {
        BGPPeerCfg lspeer = this.bgpPeerTree.get(routerid);

        if (lspeer != null) {

            //TODO DISCONNECT PEER
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
        BGPPeerCfg lspeer = this.bgpPeerTree.get(routerid);

        if (lspeer != null) {

            //TODO DISCONNECT PEER
            lspeer.setState(BGPPeerCfg.State.IDLE);
            lspeer.setSelfInnitConnection(false);
            log.debug("Disconnected : " + routerid + " successfully");

            return true;
        } else {
            log.debug("Did not find : " + routerid);
            return false;
        }
    }

    @Override
    public void setPeerConnState(String routerid, BGPPeerCfg.State state) {
        BGPPeerCfg lspeer = this.bgpPeerTree.get(routerid);

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
    public BGPPeerCfg.State getPeerConnState(String routerid) {
        BGPPeerCfg lspeer = this.bgpPeerTree.get(routerid);

        if (lspeer != null) {
            return lspeer.getState();
        } else {
            return BGPPeerCfg.State.INVALID; //No instance
        }
    }

    @Override
    public boolean isPeerConnectable(String routerid) {
        BGPPeerCfg lspeer = this.bgpPeerTree.get(routerid);

        if ((lspeer != null) && lspeer.getState().equals(BGPPeerCfg.State.IDLE)) {
            return true;
        }

        return false;
    }

    @Override
    public TreeMap<String, BGPPeerCfg> getPeerTree() {
        return this.bgpPeerTree;
    }

    @Override
    public TreeMap<String, BGPPeerCfg> displayPeers() {
        if (this.bgpPeerTree.isEmpty()) {
            log.debug("There are no BGP peers");
        } else {
            Set<Entry<String, BGPPeerCfg>> set = this.bgpPeerTree.entrySet();
            Iterator<Entry<String, BGPPeerCfg>> list = set.iterator();
            BGPPeerCfg lspeer;

            while (list.hasNext()) {
                Entry<String, BGPPeerCfg> me = list.next();
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
    public BGPPeerCfg displayPeers(String routerid) {

        if (this.bgpPeerTree.isEmpty()) {
            log.debug("There are no BGP peers");
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
        BGPPeerCfg lspeer = this.bgpPeerTree.get(routerid);
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
