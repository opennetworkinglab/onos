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
import java.util.ListIterator;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.netty.channel.Channel;
import org.onlab.packet.IpAddress;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.bgp.controller.BgpPeer;
import org.onosproject.bgp.controller.BgpSessionInfo;
import org.onosproject.bgp.controller.BgpLocalRib;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpFactories;
import org.onosproject.bgpio.protocol.BgpFactory;
import org.onosproject.bgpio.protocol.BgpLSNlri;
import org.onosproject.bgpio.protocol.BgpMessage;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpPrefixIPv4LSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLsNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.PathAttrNlriDetails;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.MpReachNlri;
import org.onosproject.bgpio.types.MpUnReachNlri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * BGPPeerImpl implements BGPPeer, maintains peer information and store updates in RIB .
 */
public class BgpPeerImpl implements BgpPeer {

    protected final Logger log = LoggerFactory.getLogger(BgpPeerImpl.class);

    private static final String SHUTDOWN_MSG = "Worker has already been shutdown";

    private BgpController bgpController;
    private Channel channel;
    protected String channelId;
    private boolean connected;
    protected boolean isHandShakeComplete = false;
    private BgpSessionInfo sessionInfo;
    private BgpPacketStatsImpl pktStats;
    private BgpLocalRib bgplocalRIB;
    private BgpLocalRib bgplocalRIBVpn;
    private AdjRibIn adjRib;
    private VpnAdjRibIn vpnAdjRib;

    /**
     * Return the adjacency RIB-IN.
     *
     * @return adjRib the adjacency RIB-IN
     */
    public AdjRibIn adjacencyRib() {
        return adjRib;
    }

    /**
     * Return the adjacency RIB-IN with VPN.
     *
     * @return vpnAdjRib the adjacency RIB-IN with VPN
     */
    public VpnAdjRibIn vpnAdjacencyRib() {
        return vpnAdjRib;
    }

    @Override
    public BgpSessionInfo sessionInfo() {
        return sessionInfo;
    }

    /**
     * Initialize peer.
     *
     *@param bgpController controller instance
     *@param sessionInfo bgp session info
     *@param pktStats packet statistics
     */
    public BgpPeerImpl(BgpController bgpController, BgpSessionInfo sessionInfo, BgpPacketStatsImpl pktStats) {
        this.bgpController = bgpController;
        this.sessionInfo = sessionInfo;
        this.pktStats = pktStats;
        this.bgplocalRIB =  bgpController.bgpLocalRib();
        this.bgplocalRIBVpn =  bgpController.bgpLocalRibVpn();
        this.adjRib = new AdjRibIn();
        this.vpnAdjRib = new VpnAdjRibIn();
    }


    @Override
    public void buildAdjRibIn(List<BgpValueType> pathAttr) throws BgpParseException {
        ListIterator<BgpValueType> iterator = pathAttr.listIterator();
        while (iterator.hasNext()) {
            BgpValueType attr = iterator.next();
            if (attr instanceof MpReachNlri) {
                List<BgpLSNlri> nlri = ((MpReachNlri) attr).mpReachNlri();
                callAdd(this, nlri, pathAttr);
            }
            if (attr instanceof MpUnReachNlri) {
                List<BgpLSNlri> nlri = ((MpUnReachNlri) attr).mpUnReachNlri();
                callRemove(this, nlri);
            }
        }
    }

    /**
     * Updates NLRI identifier node in a tree separately based on afi and safi.
     *
     * @param peerImpl BGP peer instance
     * @param nlri MpReachNlri path attribute
     * @param pathAttr list of BGP path attributes
     * @throws BgpParseException throws exception
     */
    public void callAdd(BgpPeerImpl peerImpl, List<BgpLSNlri> nlri, List<BgpValueType> pathAttr)
            throws BgpParseException {
        ListIterator<BgpLSNlri> listIterator = nlri.listIterator();
        while (listIterator.hasNext()) {
            BgpLSNlri nlriInfo = listIterator.next();
            if (nlriInfo instanceof BgpNodeLSNlriVer4) {
                PathAttrNlriDetails details = setPathAttrDetails(nlriInfo, pathAttr);
                if (!((BgpNodeLSNlriVer4) nlriInfo).isVpnPresent()) {
                    adjRib.add(nlriInfo, details);
                    bgplocalRIB.add(sessionInfo(), nlriInfo, details);
                } else {
                    vpnAdjRib.addVpn(nlriInfo, details, ((BgpNodeLSNlriVer4) nlriInfo).getRouteDistinguisher());
                    bgplocalRIBVpn.add(sessionInfo(), nlriInfo, details,
                                       ((BgpNodeLSNlriVer4) nlriInfo).getRouteDistinguisher());
                }
            } else if (nlriInfo instanceof BgpLinkLsNlriVer4) {
                PathAttrNlriDetails details = setPathAttrDetails(nlriInfo, pathAttr);
                if (!((BgpLinkLsNlriVer4) nlriInfo).isVpnPresent()) {
                    adjRib.add(nlriInfo, details);
                    bgplocalRIB.add(sessionInfo(), nlriInfo, details);
                } else {
                    vpnAdjRib.addVpn(nlriInfo, details, ((BgpLinkLsNlriVer4) nlriInfo).getRouteDistinguisher());
                    bgplocalRIBVpn.add(sessionInfo(), nlriInfo, details,
                                       ((BgpLinkLsNlriVer4) nlriInfo).getRouteDistinguisher());
                }
            } else if (nlriInfo instanceof BgpPrefixIPv4LSNlriVer4) {
                PathAttrNlriDetails details = setPathAttrDetails(nlriInfo, pathAttr);
                if (!((BgpPrefixIPv4LSNlriVer4) nlriInfo).isVpnPresent()) {
                    adjRib.add(nlriInfo, details);
                    bgplocalRIB.add(sessionInfo(), nlriInfo, details);
                } else {
                    vpnAdjRib.addVpn(nlriInfo, details, ((BgpPrefixIPv4LSNlriVer4) nlriInfo).getRouteDistinguisher());
                    bgplocalRIBVpn.add(sessionInfo(), nlriInfo, details,
                                       ((BgpPrefixIPv4LSNlriVer4) nlriInfo).getRouteDistinguisher());
                }
            }
        }
    }

    /**
     * Sets BGP path attribute and NLRI details.
     *
     * @param nlriInfo MpReachNlri path attribute
     * @param pathAttr list of BGP path attributes
     * @return details object of PathAttrNlriDetails
     * @throws BgpParseException throw exception
     */
    public PathAttrNlriDetails setPathAttrDetails(BgpLSNlri nlriInfo, List<BgpValueType> pathAttr)
            throws BgpParseException {
        PathAttrNlriDetails details = new PathAttrNlriDetails();
        details.setProtocolID(nlriInfo.getProtocolId());
        details.setIdentifier(nlriInfo.getIdentifier());
        details.setPathAttribute(pathAttr);
        return details;
    }

    /**
     * Removes NLRI identifier node in a tree separately based on afi and safi.
     *
     * @param peerImpl BGP peer instance
     * @param nlri NLRI information
     */
    public void callRemove(BgpPeerImpl peerImpl, List<BgpLSNlri> nlri) {
        ListIterator<BgpLSNlri> listIterator = nlri.listIterator();
        while (listIterator.hasNext()) {
            BgpLSNlri nlriInfo = listIterator.next();
            if (nlriInfo instanceof BgpNodeLSNlriVer4) {
                if (!((BgpNodeLSNlriVer4) nlriInfo).isVpnPresent()) {
                    adjRib.remove(nlriInfo);
                    bgplocalRIB.delete(nlriInfo);
                } else {
                    vpnAdjRib.removeVpn(nlriInfo, ((BgpNodeLSNlriVer4) nlriInfo).getRouteDistinguisher());
                    bgplocalRIBVpn.delete(nlriInfo, ((BgpNodeLSNlriVer4) nlriInfo).getRouteDistinguisher());
                }
            } else if (nlriInfo instanceof BgpLinkLsNlriVer4) {
                if (!((BgpLinkLsNlriVer4) nlriInfo).isVpnPresent()) {
                    adjRib.remove(nlriInfo);
                    bgplocalRIB.delete(nlriInfo);
                } else {
                    vpnAdjRib.removeVpn(nlriInfo, ((BgpLinkLsNlriVer4) nlriInfo).getRouteDistinguisher());
                    bgplocalRIBVpn.delete(nlriInfo, ((BgpLinkLsNlriVer4) nlriInfo).getRouteDistinguisher());
                }
            } else if (nlriInfo instanceof BgpPrefixIPv4LSNlriVer4) {
                if (!((BgpPrefixIPv4LSNlriVer4) nlriInfo).isVpnPresent()) {
                    adjRib.remove(nlriInfo);
                    bgplocalRIB.delete(nlriInfo);
                } else {
                    vpnAdjRib.removeVpn(nlriInfo, ((BgpPrefixIPv4LSNlriVer4) nlriInfo).getRouteDistinguisher());
                    bgplocalRIBVpn.delete(nlriInfo, ((BgpPrefixIPv4LSNlriVer4) nlriInfo).getRouteDistinguisher());
                }
            }
        }
    }

    /**
     * Return the adjacency RIB-IN.
     *
     * @return adjRib the adjacency RIB-IN
     */
    public AdjRibIn adjRib() {
        return adjRib;
    }

    /**
     * Return the adjacency RIB-IN with VPN.
     *
     * @return vpnAdjRib the adjacency RIB-IN with VPN
     */
    public VpnAdjRibIn vpnAdjRib() {
        return vpnAdjRib;
    }

    /**
     * Update localRIB on peer disconnect.
     *
     */
    public void updateLocalRIBOnPeerDisconnect() {
        BgpLocalRibImpl localRib = (BgpLocalRibImpl) bgplocalRIB;
        BgpLocalRibImpl localRibVpn = (BgpLocalRibImpl) bgplocalRIBVpn;

        localRib.localRIBUpdate(adjacencyRib());
        localRibVpn.localRIBUpdate(vpnAdjacencyRib());
    }

    // ************************
    // Channel related
    // ************************

    @Override
    public final void disconnectPeer() {
        this.channel.close();
    }

    @Override
    public final void sendMessage(BgpMessage m) {
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
    public final void sendMessage(List<BgpMessage> msgs) {
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

    @Override
    public BgpFactory factory() {
        return BgpFactories.getFactory(sessionInfo.remoteBgpVersion());
    }

    @Override
    public boolean isHandshakeComplete() {
        return isHandShakeComplete;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues()
                                       .add("channel", channelId())
                                       .add("BgpId", sessionInfo().remoteBgpId()).toString();
    }
}
