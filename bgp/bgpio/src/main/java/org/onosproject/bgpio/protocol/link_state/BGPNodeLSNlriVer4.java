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
package org.onosproject.bgpio.protocol.link_state;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BGPParseException;
import org.onosproject.bgpio.protocol.BGPNodeLSNlri;
import org.onosproject.bgpio.protocol.NlriType;
import org.onosproject.bgpio.types.BGPErrorType;
import org.onosproject.bgpio.types.RouteDistinguisher;
import org.onosproject.bgpio.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Implementation of Node LS NLRI.
 */
public class BGPNodeLSNlriVer4 implements BGPNodeLSNlri {

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

    protected static final Logger log = LoggerFactory.getLogger(BGPNodeLSNlriVer4.class);

    public static final int NODE_NLRITYPE = 1;
    public static final int IDENTIFIER_LENGTH = 16;
    private long identifier;
    private byte protocolId;
    private BGPNodeLSIdentifier localNodeDescriptors;
    private RouteDistinguisher routeDistinguisher;
    private boolean isVpn;

    /**
     * Enum to provide PROTOCOLTYPE.
     */
    public enum PROTOCOLTYPE {
        ISIS_LevelOne(1), ISIS_LevelTwo(2), OSPFv2(3), Direct(4), Static_Configuration(5), OSPFv3(6);
        int value;

        /**
         * Assign val with the value as the protocol type.
         *
         * @param val protocol type
         */
        PROTOCOLTYPE(int val) {
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
    public BGPNodeLSNlriVer4() {
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
    BGPNodeLSNlriVer4(long identifier, byte protocolId, BGPNodeLSIdentifier localNodeDescriptors, boolean isVpn,
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
     * @throws BGPParseException while parsing node descriptors
     */
    public static BGPNodeLSNlriVer4 read(ChannelBuffer cb, short afi, byte safi) throws BGPParseException {
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

        // Parse Local Node Descriptors
        BGPNodeLSIdentifier localNodeDescriptors = new BGPNodeLSIdentifier();
        localNodeDescriptors = BGPNodeLSIdentifier.parseLocalNodeDescriptors(cb, protocolId);
        return new BGPNodeLSNlriVer4(identifier, protocolId, localNodeDescriptors, isVpn, routeDistinguisher);
    }

    @Override
    public NlriType getNlriType() {
        return NlriType.NODE;
    }

    @Override
    public BGPNodeLSIdentifier getLocalNodeDescriptors() {
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
    public void setNodeLSIdentifier(BGPNodeLSIdentifier localNodeDescriptors) {
        this.localNodeDescriptors = localNodeDescriptors;
    }

    @Override
    public PROTOCOLTYPE getProtocolId() throws BGPParseException {
        switch (protocolId) {
        case Constants.ISIS_LEVELONE:
            return PROTOCOLTYPE.ISIS_LevelOne;
        case Constants.ISIS_LEVELTWO:
            return PROTOCOLTYPE.ISIS_LevelTwo;
        case Constants.OSPFV2:
            return PROTOCOLTYPE.OSPFv2;
        case Constants.DIRECT:
            return PROTOCOLTYPE.Direct;
        case Constants.STATIC_CONFIGURATION:
            return PROTOCOLTYPE.Static_Configuration;
        case Constants.OSPFV3:
            return PROTOCOLTYPE.OSPFv3;
        default:
            throw new BGPParseException(BGPErrorType.UPDATE_MESSAGE_ERROR, (byte) 0, null);
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