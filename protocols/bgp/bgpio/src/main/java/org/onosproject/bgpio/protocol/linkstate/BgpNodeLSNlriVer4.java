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

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpNodeLSNlri;
import org.onosproject.bgpio.protocol.NlriType;
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.types.RouteDistinguisher;
import org.onosproject.bgpio.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Implementation of Node LS NLRI.
 */
public class BgpNodeLSNlriVer4 implements BgpNodeLSNlri {

    /*
     *REFERENCE : draft-ietf-idr-ls-distribution-11
          0                   1                   2                   3
          0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
         +-+-+-+-+-+-+-+-+
         |  Protocol-ID  |
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         |                           Identifier                          |
         |                            (64 bits)                          |
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         //                Local Node Descriptors (variable)            //
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                          Figure : The Node NLRI format
     */

    private static final Logger log = LoggerFactory.getLogger(BgpNodeLSNlriVer4.class);

    public static final int NODE_NLRITYPE = 1;
    public static final int IDENTIFIER_LENGTH = 16;
    private long identifier;
    private byte protocolId;
    private BgpNodeLSIdentifier localNodeDescriptors;
    private RouteDistinguisher routeDistinguisher;
    private boolean isVpn;

    /**
     * Enum to provide PROTOCOLTYPE.
     */
    public enum ProtocolType {
        ISIS_LEVEL_ONE(1), ISIS_LEVEL_TWO(2), OSPF_V2(3), DIRECT(4), STATIC_CONFIGURATION(5), OSPF_V3(6);
        int value;

        /**
         * Assign val with the value as the protocol type.
         *
         * @param val protocol type
         */
        ProtocolType(int val) {
            value = val;
        }

        /**
         * Returns value of protocol type.
         *
         * @return protocol type
         */
        public byte getType() {
            return (byte) value;
        }
    }

    /**
     * Reset fields.
     */
    public BgpNodeLSNlriVer4() {
        this.identifier = 0;
        this.protocolId = 0;
        this.localNodeDescriptors = null;
        this.routeDistinguisher = null;
        this.isVpn = false;
    }

    /**
     * Constructors to initialize its parameters.
     *
     * @param identifier of LinkState Nlri
     * @param protocolId of LinkState Nlri
     * @param localNodeDescriptors local node descriptors
     * @param isVpn true if VPN info is present
     * @param routeDistinguisher unique for each VPN
     */
    public BgpNodeLSNlriVer4(long identifier, byte protocolId, BgpNodeLSIdentifier localNodeDescriptors, boolean isVpn,
                      RouteDistinguisher routeDistinguisher) {
        this.identifier = identifier;
        this.protocolId = protocolId;
        this.localNodeDescriptors = localNodeDescriptors;
        this.routeDistinguisher = routeDistinguisher;
        this.isVpn = isVpn;
    }

    /**
     * Reads from channelBuffer and parses Node LS Nlri.
     *
     * @param cb ChannelBuffer
     * @param afi Address Family Identifier
     * @param safi Subsequent Address Family Identifier
     * @return object of this class
     * @throws BgpParseException while parsing node descriptors
     */
    public static BgpNodeLSNlriVer4 read(ChannelBuffer cb, short afi, byte safi) throws BgpParseException {
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

        log.debug("Parse local node descriptors");
        BgpNodeLSIdentifier localNodeDescriptors = new BgpNodeLSIdentifier();
        localNodeDescriptors = BgpNodeLSIdentifier.parseLocalNodeDescriptors(cb, protocolId);
        return new BgpNodeLSNlriVer4(identifier, protocolId, localNodeDescriptors, isVpn, routeDistinguisher);
    }

    @Override
    public NlriType getNlriType() {
        return NlriType.NODE;
    }

    @Override
    public BgpNodeLSIdentifier getLocalNodeDescriptors() {
        return this.localNodeDescriptors;
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

    @Override
    public long getIdentifier() {
        return this.identifier;
    }

    /**
     * Set the node LS identifier.
     *
     * @param localNodeDescriptors node LS identifier to set
     */
    public void setNodeLSIdentifier(BgpNodeLSIdentifier localNodeDescriptors) {
        this.localNodeDescriptors = localNodeDescriptors;
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
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("protocolId", protocolId)
                .add("identifier", identifier)
                .add("RouteDistinguisher ", routeDistinguisher)
                .add("localNodeDescriptors", localNodeDescriptors)
                .toString();
    }
}
