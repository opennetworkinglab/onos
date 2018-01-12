/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.bgpio.protocol.evpn;

import com.google.common.base.MoreObjects;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpEvpnNlri;
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of Evpn NLRI.
 */
public class BgpEvpnNlriImpl implements BgpEvpnNlri {

    /*
     * REFERENCE : RFC 7432 BGP MPLS-Based Ethernet VPN
                 +-----------------------------------+
                 |    Route Type (1 octet)           |
                 +-----------------------------------+
                 |     Length (1 octet)              |
                 +-----------------------------------+
                 | Route Type specific (variable)    |
                 +-----------------------------------+

                Figure : The format of the EVPN NLRI
     */

    private static final Logger log = LoggerFactory
            .getLogger(BgpEvpnNlriImpl.class);

    // total length of route type and length
    public static final short TYPE_AND_LEN = 2;
    private byte routeType;
    private BgpEvpnNlriData routeTypeSpec;

    /**
     * Resets parameters.
     */
    public BgpEvpnNlriImpl() {
        this.routeType = Constants.BGP_EVPN_MAC_IP_ADVERTISEMENT;
        this.routeTypeSpec = null;

    }

    /**
     * Constructor to initialize parameters for BGP EvpnNlri.
     *
     * @param routeType       route Type
     * @param routeTypeSpefic route type specific
     */
    public BgpEvpnNlriImpl(byte routeType, BgpEvpnNlriData routeTypeSpefic) {
        this.routeType = routeType;
        this.routeTypeSpec = routeTypeSpefic;
    }

    /**
     * Reads from channelBuffer and parses Evpn Nlri.
     *
     * @param cb ChannelBuffer
     * @return object of BgpEvpnNlriVer4
     * @throws BgpParseException while parsing Bgp Evpn Nlri
     */
    public static BgpEvpnNlriImpl read(ChannelBuffer cb)
            throws BgpParseException {

        BgpEvpnNlriData routeNlri = null;

        if (cb.readableBytes() > 0) {
            ChannelBuffer tempBuf = cb.copy();
            byte type = cb.readByte();
            byte length = cb.readByte();
            if (cb.readableBytes() < length) {
                throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                            BgpErrorType.OPTIONAL_ATTRIBUTE_ERROR,
                                            tempBuf.readBytes(cb.readableBytes()
                                                                      + TYPE_AND_LEN));
            }
            ChannelBuffer tempCb = cb.readBytes(length);
            switch (type) {
                case BgpEvpnRouteType2Nlri.TYPE:
                    routeNlri = BgpEvpnRouteType2Nlri.read(tempCb);
                    break;
                default:
                    log.info("Discarding, EVPN route type {}", type);
                    throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                                BgpErrorType.MISSING_WELLKNOWN_ATTRIBUTE, null);
                    //        break;
            }
            return new BgpEvpnNlriImpl(type, routeNlri);
        } else {
            return new BgpEvpnNlriImpl();
        }

    }

    @Override
    public BgpEvpnNlriData getNlri() {
        return routeTypeSpec;
    }

    @Override
    public BgpEvpnRouteType getRouteType() {
        switch (routeType) {
            case Constants.BGP_EVPN_ETHERNET_AUTO_DISCOVERY:
                return BgpEvpnRouteType.ETHERNET_AUTO_DISCOVERY;
            case Constants.BGP_EVPN_MAC_IP_ADVERTISEMENT:
                return BgpEvpnRouteType.MAC_IP_ADVERTISEMENT;
            case Constants.BGP_EVPN_INCLUSIVE_MULTICASE_ETHERNET:
                return BgpEvpnRouteType.INCLUSIVE_MULTICASE_ETHERNET;
            case Constants.BGP_EVPN_ETHERNET_SEGMENT:
                return BgpEvpnRouteType.ETHERNET_SEGMENT;
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues()
                .add("routeType", routeType)
                .add("routeTypeSpefic ", routeTypeSpec).toString();
    }

    @Override
    public short getType() {
        return routeType;
    }


    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeByte(routeType);
        int iSpecStartIndex = cb.writerIndex();
        cb.writeByte(0);
        switch (routeType) {
            case BgpEvpnRouteType2Nlri.TYPE:
                ((BgpEvpnRouteType2Nlri) routeTypeSpec).write(cb);
                break;
            default:
                break;
        }
        cb.setByte(iSpecStartIndex,
                   (short) (cb.writerIndex() - iSpecStartIndex + 1));
        return cb.writerIndex() - iLenStartIndex;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }


}

