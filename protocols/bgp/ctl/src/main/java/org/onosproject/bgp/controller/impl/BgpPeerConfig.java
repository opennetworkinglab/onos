/*
 * Copyright 2015-present Open Networking Laboratory
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
import org.onosproject.bgp.controller.BgpConnectPeer;
import org.onosproject.bgp.controller.BgpPeerCfg;

/**
 * BGP Peer configuration information.
 */
public class BgpPeerConfig  implements BgpPeerCfg {
    private int asNumber;
    private short holdTime;
    private boolean isIBgp;
    private Ip4Address peerId = null;
    private State state;
    private boolean selfInitiated;
    private BgpConnectPeer connectPeer;

    /**
     * Constructor to initialize the values.
     */
    BgpPeerConfig() {
        state = State.IDLE;
        selfInitiated = false;
    }

    @Override
    public int getAsNumber() {
        return this.asNumber;
    }

    @Override
    public void setAsNumber(int asNumber) {
        this.asNumber = asNumber;
    }

    @Override
    public short getHoldtime() {
        return this.holdTime;
    }

    @Override
    public void setHoldtime(short holdTime) {
        this.holdTime = holdTime;
    }

    @Override
    public boolean getIsIBgp() {
        return this.isIBgp;
    }

    @Override
    public void setIsIBgp(boolean isIBgp) {
        this.isIBgp = isIBgp;
    }

    @Override
    public String getPeerRouterId() {
        if (this.peerId != null) {
            return this.peerId.toString();
        } else {
            return null;
        }
    }

    @Override
    public void setPeerRouterId(String peerId) {
        this.peerId = Ip4Address.valueOf(peerId);
    }

    @Override
    public void setPeerRouterId(String peerId, int asNumber) {
        this.peerId = Ip4Address.valueOf(peerId);
        this.asNumber = asNumber;
    }

    @Override
    public State getState() {
        return this.state;
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public boolean getSelfInnitConnection() {
        return this.selfInitiated;
    }

    @Override
    public void setSelfInnitConnection(boolean selfInit) {
        this.selfInitiated = selfInit;
    }

    @Override
    public BgpConnectPeer connectPeer() {
        return this.connectPeer;
    }

    @Override
    public void setConnectPeer(BgpConnectPeer connectPeer) {
        this.connectPeer = connectPeer;
    }
}
