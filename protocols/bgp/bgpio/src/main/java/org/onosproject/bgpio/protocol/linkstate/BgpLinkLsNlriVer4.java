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
package org.onosproject.bgpio.protocol.linkstate;

import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpLinkLsNlri;
import org.onosproject.bgpio.protocol.NlriType;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4.ProtocolType;
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.RouteDistinguisher;
import org.onosproject.bgpio.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Implementation of Link LS NLRI.
 */
public class BgpLinkLsNlriVer4 implements BgpLinkLsNlri {

    /*
     * REFERENCE : draft-ietf-idr-ls-distribution-11
          0                   1                   2                   3
          0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
         +-+-+-+-+-+-+-+-+
         |  Protocol-ID  |
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         |                           Identifier                          |
         |                            (64 bits)                          |
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         //               Local Node Descriptors (variable)             //
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         //               Remote Node Descriptors (variable)            //
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         //                  Link Descriptors (variable)                //
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                          Figure : The Link NLRI format
     */
    private static final Logger log = LoggerFactory.getLogger(BgpLinkLsNlriVer4.class);
    public static final int LINK_NLRITYPE = 2;

    private BgpLinkLSIdentifier linkLSIdentifier;
    private byte protocolId;
    private long identifier;
    private RouteDistinguisher routeDistinguisher;
    private boolean isVpn;

    /**
     * Initialize fields.
     */
    public BgpLinkLsNlriVer4() {
        this.protocolId = 0;
        this.identifier = 0;
        this.linkLSIdentifier = null;
        this.routeDistinguisher = null;
        this.isVpn = false;
    }

    /**
     * Constructor to initialize parameters for BGP LinkLSNlri.
     *
     * @param protocolId protocol Id
     * @param identifier field in BGP LinkLSNlri
     * @param linkLSIdentifier link LS identifier
     * @param routeDistinguisher route distinguisher from message
     * @param isVpn vpn info availability in message
     */
    public BgpLinkLsNlriVer4(byte protocolId, long identifier, BgpLinkLSIdentifier linkLSIdentifier,
                             RouteDistinguisher routeDistinguisher, boolean isVpn) {
        this.protocolId = protocolId;
        this.identifier = identifier;
        this.linkLSIdentifier = linkLSIdentifier;
        this.routeDistinguisher = routeDistinguisher;
        this.isVpn = isVpn;
    }

    /**
     * Reads from channelBuffer and parses Link LS Nlri.
     *
     * @param cb ChannelBuffer
     * @param afi Address Family Identifier
     * @param safi Subsequent Address Family Identifier
     * @return object of this class
     * @throws BgpParseException while parsing Link LS NLRI
     */
    public static BgpLinkLsNlriVer4 read(ChannelBuffer cb, short afi, byte safi) throws BgpParseException {
        boolean isVpn = false;
        RouteDistinguisher routeDistinguisher = null;
        if ((afi == Constants.AFI_VALUE) && (safi == Constants.VPN_SAFI_VALUE)) {
            routeDistinguisher = new RouteDistinguisher();
            routeDistinguisher = RouteDistinguisher.read(cb);
            isVpn = true;
        } else {
            isVpn = false;
        }
        byte protocolId = cb.readByte();
        long identifier = cb.readLong();

        BgpLinkLSIdentifier linkLSIdentifier = new BgpLinkLSIdentifier();
        linkLSIdentifier = BgpLinkLSIdentifier.parseLinkIdendifier(cb, protocolId);
        return new BgpLinkLsNlriVer4(protocolId, identifier, linkLSIdentifier, routeDistinguisher, isVpn);
    }

    @Override
    public NlriType getNlriType() {
        return NlriType.LINK;
    }

    @Override
    public long getIdentifier() {
        return this.identifier;
    }

    /**
     * Set the link LS identifier.
     *
     * @param linkLSIdentifier link LS identifier to set
     */
    public void setLinkLSIdentifier(BgpLinkLSIdentifier linkLSIdentifier) {
        this.linkLSIdentifier = linkLSIdentifier;
    }

    @Override
    public ProtocolType getProtocolId() throws BgpParseException {
        switch (protocolId) {
        case Constants.ISIS_LEVELONE:
            return ProtocolType.ISIS_LEVEL_ONE;
        case Constants.ISIS_LEVELTWO:
            return ProtocolType.ISIS_LEVEL_TWO;
        case Constants.OSPFV2:
            return ProtocolType.OSPF_V2;
        case Constants.DIRECT:
            return ProtocolType.DIRECT;
        case Constants.STATIC_CONFIGURATION:
            return ProtocolType.STATIC_CONFIGURATION;
        case Constants.OSPFV3:
            return ProtocolType.OSPF_V3;
        default:
            throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR, (byte) 0, null);
        }
    }

    @Override
    public NodeDescriptors localNodeDescriptors() {
        return this.linkLSIdentifier.localNodeDescriptors();
    }

    @Override
    public NodeDescriptors remoteNodeDescriptors() {
        return this.linkLSIdentifier.remoteNodeDescriptors();
    }

    /**
     * Returns whether VPN is present or not.
     *
     * @return whether VPN is present or not
     */
    public boolean isVpnPresent() {
        return this.isVpn;
    }

    @Override
    public RouteDistinguisher getRouteDistinguisher() {
        return this.routeDistinguisher;
    }

    /**
     * Returns link identifier.
     *
     * @return link identifier
     */
    public BgpLinkLSIdentifier getLinkIdentifier() {
        return this.linkLSIdentifier;
    }

    @Override
    public List<BgpValueType> linkDescriptors() {
        return this.linkLSIdentifier.linkDescriptors();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("protocolId", protocolId)
                .add("identifier", identifier)
                .add("RouteDistinguisher ", routeDistinguisher)
                .add("linkLSIdentifier", linkLSIdentifier)
                .toString();
    }
}
