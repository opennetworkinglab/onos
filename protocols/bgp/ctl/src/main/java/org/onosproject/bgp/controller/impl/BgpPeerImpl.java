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

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import org.jboss.netty.channel.Channel;
import org.onlab.packet.IpAddress;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.bgp.controller.BgpLocalRib;
import org.onosproject.bgp.controller.BgpPeer;
import org.onosproject.bgp.controller.BgpSessionInfo;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpFactories;
import org.onosproject.bgpio.protocol.BgpFactory;
import org.onosproject.bgpio.protocol.BgpLSNlri;
import org.onosproject.bgpio.protocol.BgpMessage;
import org.onosproject.bgpio.protocol.flowspec.BgpFlowSpecNlri;
import org.onosproject.bgpio.protocol.flowspec.BgpFlowSpecRouteKey;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLsNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpPrefixIPv4LSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.PathAttrNlriDetails;
import org.onosproject.bgpio.types.AsPath;
import org.onosproject.bgpio.types.As4Path;
import org.onosproject.bgpio.types.BgpExtendedCommunity;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.LocalPref;
import org.onosproject.bgpio.types.Med;
import org.onosproject.bgpio.types.MpReachNlri;
import org.onosproject.bgpio.types.MpUnReachNlri;
import org.onosproject.bgpio.types.MultiProtocolExtnCapabilityTlv;
import org.onosproject.bgpio.types.Origin;
import org.onosproject.bgpio.types.attr.WideCommunity;
import org.onosproject.bgpio.types.RpdCapabilityTlv;
import org.onosproject.bgpio.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.RejectedExecutionException;

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
    private BgpLocalRib bgplocalRib;
    private BgpLocalRib bgplocalRibVpn;
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
        this.bgplocalRib =  bgpController.bgpLocalRib();
        this.bgplocalRibVpn =  bgpController.bgpLocalRibVpn();
        this.adjRib = new AdjRibIn();
        this.vpnAdjRib = new VpnAdjRibIn();
    }

    /**
     * Check if peer support capability.
     *
     * @param type capability type
     * @param afi address family identifier
     * @param sAfi subsequent address family identifier
     * @return true if capability is supported, otherwise false
     */
    public final boolean isCapabilitySupported(short type, short afi, byte sAfi) {

        List<BgpValueType> capability = sessionInfo.remoteBgpCapability();
        ListIterator<BgpValueType> listIterator = capability.listIterator();

        while (listIterator.hasNext()) {
            BgpValueType tlv = listIterator.next();

            if (tlv.getType() == type) {
                if (tlv.getType() == MultiProtocolExtnCapabilityTlv.TYPE) {
                    MultiProtocolExtnCapabilityTlv temp = (MultiProtocolExtnCapabilityTlv) tlv;
                    if ((temp.getAfi() == afi) && (temp.getSafi() == sAfi)) {
                        log.debug("Multi prorotcol extension capabality TLV is true");
                        return true;

                    }
                } else if (tlv.getType() == RpdCapabilityTlv.TYPE) {
                    RpdCapabilityTlv temp = (RpdCapabilityTlv) tlv;
                    if ((temp.getAfi() == afi) && (temp.getSafi() == sAfi)) {
                        log.debug("RPD capabality TLV is true");
                        return true;
                    }
                }
            }
        }
        log.debug("IS capabality is not supported ");
        return false;
    }

    /**
     * Send flow specification update message to peer.
     *
     * @param operType operation type
     * @param routeKey flow rule key
     * @param flowSpec flow specification details
     * @param wideCommunity for route policy
     */
    public final void sendFlowSpecUpdateMessageToPeer(FlowSpecOperation operType, BgpFlowSpecRouteKey routeKey,
                                                      BgpFlowSpecNlri flowSpec, WideCommunity wideCommunity) {

        List<BgpValueType> attributesList = new LinkedList<>();
        byte sessionType = sessionInfo.isIbgpSession() ? (byte) 0 : (byte) 1;
        byte sAfi = Constants.SAFI_FLOWSPEC_VALUE;

        boolean isFsCapabilitySet = isCapabilitySupported(MultiProtocolExtnCapabilityTlv.TYPE,
                                                        Constants.AFI_FLOWSPEC_VALUE,
                                                        Constants.SAFI_FLOWSPEC_VALUE);

        boolean isVpnFsCapabilitySet = isCapabilitySupported(MultiProtocolExtnCapabilityTlv.TYPE,
                                                        Constants.AFI_FLOWSPEC_VALUE,
                                                        Constants.VPN_SAFI_FLOWSPEC_VALUE);

        boolean isRpdCapabilitySet = isCapabilitySupported(RpdCapabilityTlv.TYPE,
                                                        Constants.AFI_FLOWSPEC_RPD_VALUE,
                                                        Constants.SAFI_FLOWSPEC_RPD_VALUE);

        boolean isVpnRpdCapabilitySet = isCapabilitySupported(RpdCapabilityTlv.TYPE,
                                                        Constants.AFI_FLOWSPEC_RPD_VALUE,
                                                        Constants.VPN_SAFI_FLOWSPEC_RDP_VALUE);

        if ((!isFsCapabilitySet) && (!isVpnFsCapabilitySet) && (!isRpdCapabilitySet) && (!isVpnRpdCapabilitySet)) {
            log.debug("Peer do not support BGP flow spec capability", channel.getRemoteAddress());
            return;
        }

        if (isVpnFsCapabilitySet) {
            sAfi = Constants.VPN_SAFI_FLOWSPEC_VALUE;
        } else if (isVpnRpdCapabilitySet) {
            sAfi = Constants.VPN_SAFI_FLOWSPEC_RDP_VALUE;
        }
        attributesList.add(new Origin((byte) 0));

        if (sessionType != 0) {
            // EBGP
            if (!bgpController.getConfig().getLargeASCapability()) {
                List<Short> aspathSet = new ArrayList<>();
                List<Short> aspathSeq = new ArrayList<>();
                aspathSeq.add((short) bgpController.getConfig().getAsNumber());

                AsPath asPath = new AsPath(aspathSet, aspathSeq);
                attributesList.add(asPath);
            } else {
                List<Integer> aspathSet = new ArrayList<>();
                List<Integer> aspathSeq = new ArrayList<>();
                aspathSeq.add(bgpController.getConfig().getAsNumber());

                As4Path as4Path = new As4Path(aspathSet, aspathSeq);
                attributesList.add(as4Path);
            }
            attributesList.add(new Med(0));
        } else {
            attributesList.add(new AsPath());
            attributesList.add(new Med(0));
            attributesList.add(new LocalPref(100));
        }

        attributesList.add(new BgpExtendedCommunity(flowSpec.fsActionTlv()));
        if (wideCommunity != null) {
            attributesList.add(wideCommunity);
        }

        if (operType == FlowSpecOperation.ADD) {
            attributesList.add(new MpReachNlri(flowSpec, Constants.AFI_FLOWSPEC_VALUE, sAfi));
        } else if (operType == FlowSpecOperation.DELETE) {
            attributesList.add(new MpUnReachNlri(flowSpec, Constants.AFI_FLOWSPEC_VALUE, sAfi));
        }

        BgpMessage msg = Controller.getBgpMessageFactory4().updateMessageBuilder()
                                                           .setBgpPathAttributes(attributesList).build();

        log.debug("Sending flow spec update message to {}", channel.getRemoteAddress());
        channel.write(Collections.singletonList(msg));
    }

    @Override
    public void updateFlowSpec(FlowSpecOperation operType, BgpFlowSpecRouteKey routeKey,
                                     BgpFlowSpecNlri flowSpec, WideCommunity wideCommunity) {
        Preconditions.checkNotNull(operType, "flow specification operation type cannot be null");
        Preconditions.checkNotNull(routeKey, "flow specification prefix cannot be null");
        Preconditions.checkNotNull(flowSpec, "flow specification details cannot be null");
        Preconditions.checkNotNull(flowSpec.fsActionTlv(), "flow specification action cannot be null");

        sendFlowSpecUpdateMessageToPeer(operType, routeKey, flowSpec, wideCommunity);
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
                    bgplocalRib.add(sessionInfo(), nlriInfo, details);
                } else {
                    vpnAdjRib.addVpn(nlriInfo, details, ((BgpNodeLSNlriVer4) nlriInfo).getRouteDistinguisher());
                    bgplocalRibVpn.add(sessionInfo(), nlriInfo, details,
                                       ((BgpNodeLSNlriVer4) nlriInfo).getRouteDistinguisher());
                }
            } else if (nlriInfo instanceof BgpLinkLsNlriVer4) {
                PathAttrNlriDetails details = setPathAttrDetails(nlriInfo, pathAttr);
                if (!((BgpLinkLsNlriVer4) nlriInfo).isVpnPresent()) {
                    adjRib.add(nlriInfo, details);
                    bgplocalRib.add(sessionInfo(), nlriInfo, details);
                } else {
                    vpnAdjRib.addVpn(nlriInfo, details, ((BgpLinkLsNlriVer4) nlriInfo).getRouteDistinguisher());
                    bgplocalRibVpn.add(sessionInfo(), nlriInfo, details,
                                       ((BgpLinkLsNlriVer4) nlriInfo).getRouteDistinguisher());
                }
            } else if (nlriInfo instanceof BgpPrefixIPv4LSNlriVer4) {
                PathAttrNlriDetails details = setPathAttrDetails(nlriInfo, pathAttr);
                if (!((BgpPrefixIPv4LSNlriVer4) nlriInfo).isVpnPresent()) {
                    adjRib.add(nlriInfo, details);
                    bgplocalRib.add(sessionInfo(), nlriInfo, details);
                } else {
                    vpnAdjRib.addVpn(nlriInfo, details, ((BgpPrefixIPv4LSNlriVer4) nlriInfo).getRouteDistinguisher());
                    bgplocalRibVpn.add(sessionInfo(), nlriInfo, details,
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
     * @throws BgpParseException BGP parse exception
     */
    public void callRemove(BgpPeerImpl peerImpl, List<BgpLSNlri> nlri) throws BgpParseException {
        ListIterator<BgpLSNlri> listIterator = nlri.listIterator();
        while (listIterator.hasNext()) {
            BgpLSNlri nlriInfo = listIterator.next();
            if (nlriInfo instanceof BgpNodeLSNlriVer4) {
                if (!((BgpNodeLSNlriVer4) nlriInfo).isVpnPresent()) {
                    adjRib.remove(nlriInfo);
                    bgplocalRib.delete(nlriInfo);
                } else {
                    vpnAdjRib.removeVpn(nlriInfo, ((BgpNodeLSNlriVer4) nlriInfo).getRouteDistinguisher());
                    bgplocalRibVpn.delete(nlriInfo, ((BgpNodeLSNlriVer4) nlriInfo).getRouteDistinguisher());
                }
            } else if (nlriInfo instanceof BgpLinkLsNlriVer4) {
                if (!((BgpLinkLsNlriVer4) nlriInfo).isVpnPresent()) {
                    adjRib.remove(nlriInfo);
                    bgplocalRib.delete(nlriInfo);
                } else {
                    vpnAdjRib.removeVpn(nlriInfo, ((BgpLinkLsNlriVer4) nlriInfo).getRouteDistinguisher());
                    bgplocalRibVpn.delete(nlriInfo, ((BgpLinkLsNlriVer4) nlriInfo).getRouteDistinguisher());
                }
            } else if (nlriInfo instanceof BgpPrefixIPv4LSNlriVer4) {
                if (!((BgpPrefixIPv4LSNlriVer4) nlriInfo).isVpnPresent()) {
                    adjRib.remove(nlriInfo);
                    bgplocalRib.delete(nlriInfo);
                } else {
                    vpnAdjRib.removeVpn(nlriInfo, ((BgpPrefixIPv4LSNlriVer4) nlriInfo).getRouteDistinguisher());
                    bgplocalRibVpn.delete(nlriInfo, ((BgpPrefixIPv4LSNlriVer4) nlriInfo).getRouteDistinguisher());
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
     * @throws BgpParseException while updating local RIB
     */
    public void updateLocalRibOnPeerDisconnect() throws BgpParseException {
        BgpLocalRibImpl localRib = (BgpLocalRibImpl) bgplocalRib;
        BgpLocalRibImpl localRibVpn = (BgpLocalRibImpl) bgplocalRibVpn;

        localRib.localRibUpdate(adjacencyRib());
        localRibVpn.localRibUpdate(vpnAdjacencyRib());
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
    }

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
    }

    @Override
    public final Channel getChannel() {
        return this.channel;
    }

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
