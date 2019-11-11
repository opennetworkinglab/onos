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
import org.onosproject.bgpio.protocol.BgpPrefixLSNlri;
import org.onosproject.bgpio.protocol.NlriType;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4.ProtocolType;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.RouteDistinguisher;
import org.onosproject.bgpio.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Implementation of Prefix IPV4 LS NLRI.
 */
public class BgpPrefixIPv4LSNlriVer4 implements BgpPrefixLSNlri {

    /*
     * REFERENCE : draft-ietf-idr-ls-distribution-11
     *       0                   1                   2                   3
          0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
         +-+-+-+-+-+-+-+-+
         |  Protocol-ID  |
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         |                           Identifier                          |
         |                            (64 bits)                          |
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         //              Local Node Descriptor (variable)               //
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         //                Prefix Descriptors (variable)                //
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                Figure : The IPv4/IPv6 Topology Prefix NLRI format
     */

    private static final Logger log = LoggerFactory.getLogger(BgpPrefixIPv4LSNlriVer4.class);

    public static final int PREFIX_IPV4_NLRITYPE = 3;
    public static final int PREFIX_IPV6_NLRITYPE = 4;
    public static final int IDENTIFIER_LENGTH = 16;
    private long identifier;
    private byte protocolId;
    private RouteDistinguisher routeDistinguisher;
    private boolean isVpn;
    private BgpPrefixLSIdentifier bgpPrefixLSIdentifier;

    /**
     * Resets parameters.
     */
    public BgpPrefixIPv4LSNlriVer4() {
        this.identifier = 0;
        this.protocolId = 0;
        this.bgpPrefixLSIdentifier = null;
        this.routeDistinguisher = null;
        this.isVpn = false;
    }

    /**
     * Constructor to initialize parameters for BGP PrefixLSNlri.
     *
     * @param identifier field in BGP PrefixLSNlri
     * @param protocolId protocol Id
     * @param bgpPrefixLSIdentifier prefix LS Identifier
     * @param routeDistinguisher RouteDistinguisher
     * @param isVpn vpn availability in message
     */
    public BgpPrefixIPv4LSNlriVer4(long identifier, byte protocolId, BgpPrefixLSIdentifier bgpPrefixLSIdentifier,
                                   RouteDistinguisher routeDistinguisher, boolean isVpn) {
        this.identifier = identifier;
        this.protocolId = protocolId;
        this.bgpPrefixLSIdentifier = bgpPrefixLSIdentifier;
        this.routeDistinguisher = routeDistinguisher;
        this.isVpn = isVpn;
    }

    /**
     * Reads from channelBuffer and parses Prefix LS Nlri.
     *
     * @param cb ChannelBuffer
     * @param afi Address family identifier
     * @param safi Subsequent address family identifier
     * @return object of BGPPrefixIPv4LSNlriVer4
     * @throws BgpParseException while parsing Prefix LS Nlri
     */
    public static BgpPrefixIPv4LSNlriVer4 read(ChannelBuffer cb, short afi, byte safi) throws BgpParseException {

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

        BgpPrefixLSIdentifier bgpPrefixLSIdentifier = new BgpPrefixLSIdentifier();
        bgpPrefixLSIdentifier = BgpPrefixLSIdentifier.parsePrefixIdendifier(cb, protocolId);
        return new BgpPrefixIPv4LSNlriVer4(identifier, protocolId, bgpPrefixLSIdentifier, routeDistinguisher, isVpn);
    }

    @Override
    public NlriType getNlriType() {
        return NlriType.PREFIX_IPV4;
    }

    @Override
    public NodeDescriptors getLocalNodeDescriptors() {
        return this.bgpPrefixLSIdentifier.getLocalNodeDescriptors();
    }

    @Override
    public long getIdentifier() {
        return this.identifier;
    }

    /**
     * Set the prefix LS identifier.
     *
     * @param bgpPrefixLSIdentifier prefix identifier to set
     */
    public void setPrefixLSIdentifier(BgpPrefixLSIdentifier bgpPrefixLSIdentifier) {
        this.bgpPrefixLSIdentifier = bgpPrefixLSIdentifier;
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
            throw new BgpParseException("protocol id not valid");
        }
    }

    /**
     * Returns whether VPN is present or not.
     *
     * @return whether VPN is present or not
     */
    public boolean isVpnPresent() {
        return this.isVpn;
    }

    /**
     * Returns Prefix Identifier.
     *
     * @return Prefix Identifier
     */
    public BgpPrefixLSIdentifier getPrefixIdentifier() {
        return this.bgpPrefixLSIdentifier;
    }

    @Override
    public RouteDistinguisher getRouteDistinguisher() {
        return this.routeDistinguisher;
    }

    @Override
    public List<BgpValueType> getPrefixdescriptor() {
        return this.bgpPrefixLSIdentifier.getPrefixdescriptor();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("protocolId", protocolId)
                .add("identifier", identifier)
                .add("RouteDistinguisher ", routeDistinguisher)
                .add("bgpPrefixLSIdentifier", bgpPrefixLSIdentifier)
                .toString();
    }
}
