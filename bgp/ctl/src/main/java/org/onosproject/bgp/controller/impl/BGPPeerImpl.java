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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.netty.channel.Channel;
import org.onlab.packet.IpAddress;
import org.onosproject.bgp.controller.BGPId;
import org.onosproject.bgp.controller.BGPPacketStats;
import org.onosproject.bgp.controller.BGPPeer;
import org.onosproject.bgpio.protocol.BGPMessage;
import org.onosproject.bgpio.protocol.BGPVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * BGPPeerImpl implements BGPPeer, maintains peer information and store updates in RIB .
 */
public class BGPPeerImpl implements BGPPeer {

    protected final Logger log = LoggerFactory.getLogger(BGPPeerImpl.class);

    private static final String SHUTDOWN_MSG = "Worker has already been shutdown";

    private Channel channel;
    protected String channelId;
    private boolean connected;
    protected boolean isHandShakeComplete = false;
    public BGPSessionInfo sessionInfo;
    private BGPPacketStatsImpl pktStats;

    @Override
    public void init(BGPId bgpId, BGPVersion bgpVersion, BGPPacketStats pktStats) {
        this.sessionInfo.setRemoteBgpId(bgpId);
        this.sessionInfo.setRemoteBgpVersion(bgpVersion);
        this.pktStats = (BGPPacketStatsImpl) pktStats;
        this.sessionInfo = new BGPSessionInfo();
    }

    // ************************
    // Channel related
    // ************************

    @Override
    public final void disconnectPeer() {
        this.channel.close();
    }

    @Override
    public final void sendMessage(BGPMessage m) {
        log.debug("Sending message to {}", channel.getRemoteAddress());
        try {
            channel.write(Collections.singletonList(m));
            this.pktStats.addOutPacket();
        } catch (RejectedExecutionException e) {
            log.warn(e.getMessage());
            if (!e.getMessage().contains(SHUTDOWN_MSG)) {
                throw e;
            }
        }
    }

    @Override
    public final void sendMessage(List<BGPMessage> msgs) {
        try {
            channel.write(msgs);
            this.pktStats.addOutPacket(msgs.size());
        } catch (RejectedExecutionException e) {
            log.warn(e.getMessage());
            if (!e.getMessage().contains(SHUTDOWN_MSG)) {
                throw e;
            }
        }
    }

    @Override
    public final boolean isConnected() {
        return this.connected;
    }

    @Override
    public final void setConnected(boolean connected) {
        this.connected = connected;
    };

    @Override
    public final void setChannel(Channel channel) {
        this.channel = channel;
        final SocketAddress address = channel.getRemoteAddress();
        if (address instanceof InetSocketAddress) {
            final InetSocketAddress inetAddress = (InetSocketAddress) address;
            final IpAddress ipAddress = IpAddress.valueOf(inetAddress.getAddress());
            if (ipAddress.isIp4()) {
                channelId = ipAddress.toString() + ':' + inetAddress.getPort();
            } else {
                channelId = '[' + ipAddress.toString() + "]:" + inetAddress.getPort();
            }
        }
    };

    @Override
    public final Channel getChannel() {
        return this.channel;
    };

    @Override
    public String channelId() {
        return channelId;
    }

    // ************************
    // BGP Peer features related
    // ************************

    @Override
    public final BGPId getBGPId() {
        return this.sessionInfo.getRemoteBgpId();
    };

    @Override
    public final String getStringId() {
        return this.sessionInfo.getRemoteBgpId().toString();
    }

    @Override
    public final void setBgpPeerVersion(BGPVersion peerVersion) {
        this.sessionInfo.setRemoteBgpVersion(peerVersion);
    }

    @Override
    public void setBgpPeerASNum(short peerASNum) {
        this.sessionInfo.setRemoteBgpASNum(peerASNum);
    }

    @Override
    public void setBgpPeerHoldTime(short peerHoldTime) {
        this.sessionInfo.setRemoteBgpHoldTime(peerHoldTime);
    }

    @Override
    public void setBgpPeerIdentifier(int peerIdentifier) {
        this.sessionInfo.setRemoteBgpIdentifier(peerIdentifier);
    }

    @Override
    public int getBgpPeerIdentifier() {
        return this.sessionInfo.getRemoteBgpIdentifier();
    }

    @Override
    public int getNegotiatedHoldTime() {
        return this.sessionInfo.getNegotiatedholdTime();
    }

    @Override
    public void setNegotiatedHoldTime(short negotiatedHoldTime) {
        this.sessionInfo.setNegotiatedholdTime(negotiatedHoldTime);
    }

    @Override
    public boolean isHandshakeComplete() {
        return isHandShakeComplete;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues().add("channel", channelId())
                .add("bgpId", getBGPId()).toString();
    }
}
