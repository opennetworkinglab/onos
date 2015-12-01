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

package org.onosproject.bgpio.types;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.Ip4Address;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpLSNlri;
import org.onosproject.bgpio.protocol.linkstate.BgpPrefixIPv4LSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLsNlriVer4;
import org.onosproject.bgpio.util.Constants;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/*
 * Provides Implementation of MpReach Nlri BGP Path Attribute.
 */
public class MpReachNlri implements BgpValueType {

    private static final Logger log = LoggerFactory.getLogger(MpReachNlri.class);
    public static final byte MPREACHNLRI_TYPE = 14;
    public static final byte LINK_NLRITYPE = 2;

    private boolean isMpReachNlri = false;
    private final List<BgpLSNlri> mpReachNlri;
    private final int length;
    private final short afi;
    private final byte safi;
    private final Ip4Address ipNextHop;

    /**
     * Constructor to initialize parameters.
     *
     * @param mpReachNlri MpReach  Nlri attribute
     * @param afi address family identifier
     * @param safi subsequent address family identifier
     * @param ipNextHop nexthop IpAddress
     * @param length of MpReachNlri
     */
    public MpReachNlri(List<BgpLSNlri> mpReachNlri, short afi, byte safi, Ip4Address ipNextHop, int length) {
        this.mpReachNlri = mpReachNlri;
        this.isMpReachNlri = true;
        this.ipNextHop = ipNextHop;
        this.afi = afi;
        this.safi = safi;
        this.length = length;
    }

    /**
     * Returns whether MpReachNlri is present.
     *
     * @return whether MpReachNlri is present
     */
    public boolean isMpReachNlriSet() {
        return this.isMpReachNlri;
    }

    /**
     * Returns list of MpReach Nlri.
     *
     * @return list of MpReach Nlri
     */
    public List<BgpLSNlri> mpReachNlri() {
        return this.mpReachNlri;
    }

    /**
     * Returns length of MpReachNlri.
     *
     * @return length of MpReachNlri
     */
    public int mpReachNlriLen() {
        return this.length;
    }

    /**
     * Reads from ChannelBuffer and parses MpReachNlri.
     *
     * @param cb channelBuffer
     * @return object of MpReachNlri
     * @throws BgpParseException while parsing MpReachNlri
     */
    public static MpReachNlri read(ChannelBuffer cb) throws BgpParseException {
        ChannelBuffer tempBuf = cb.copy();
        Validation parseFlags = Validation.parseAttributeHeader(cb);
        int len = parseFlags.isShort() ? parseFlags.getLength() + Constants.TYPE_AND_LEN_AS_SHORT :
                  parseFlags.getLength() + Constants.TYPE_AND_LEN_AS_BYTE;
        ChannelBuffer data = tempBuf.readBytes(len);

        if (cb.readableBytes() < parseFlags.getLength()) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                    parseFlags.getLength());
        }
        if (!parseFlags.getFirstBit() && parseFlags.getSecondBit() && parseFlags.getThirdBit()) {
            throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_FLAGS_ERROR, data);
        }

        BgpLSNlri bgpLSNlri = null;
        List<BgpLSNlri> mpReachNlri = new LinkedList<>();
        ChannelBuffer tempCb = cb.readBytes(parseFlags.getLength());
        short afi = 0;
        byte safi = 0;
        Ip4Address ipNextHop = null;
        while (tempCb.readableBytes() > 0) {
            afi = tempCb.readShort();
            safi = tempCb.readByte();

            //Supporting for AFI 16388 / SAFI 71 and VPN AFI 16388 / SAFI 128
            if ((afi == Constants.AFI_VALUE) && (safi == Constants.SAFI_VALUE) || (afi == Constants.AFI_VALUE)
                                    && (safi == Constants.VPN_SAFI_VALUE)) {
                byte nextHopLen = tempCb.readByte();
                InetAddress ipAddress = Validation.toInetAddress(nextHopLen, tempCb);
                if (ipAddress.isMulticastAddress()) {
                    throw new BgpParseException("Multicast not supported");
                }
                ipNextHop = Ip4Address.valueOf(ipAddress);
                byte reserved = tempCb.readByte();

                while (tempCb.readableBytes() > 0) {
                    short nlriType = tempCb.readShort();
                    short totNlriLen = tempCb.readShort();
                    if (tempCb.readableBytes() < totNlriLen) {
                        Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                        BgpErrorType.ATTRIBUTE_LENGTH_ERROR, totNlriLen);
                    }
                    tempBuf = tempCb.readBytes(totNlriLen);
                    switch (nlriType) {
                    case BgpNodeLSNlriVer4.NODE_NLRITYPE:
                        bgpLSNlri = BgpNodeLSNlriVer4.read(tempBuf, afi, safi);
                        break;
                    case BgpLinkLsNlriVer4.LINK_NLRITYPE:
                        bgpLSNlri = BgpLinkLsNlriVer4.read(tempBuf, afi, safi);
                        break;
                    case BgpPrefixIPv4LSNlriVer4.PREFIX_IPV4_NLRITYPE:
                        bgpLSNlri = BgpPrefixIPv4LSNlriVer4.read(tempBuf, afi, safi);
                        break;
                    default:
                        log.debug("nlriType not supported" + nlriType);
                    }
                    mpReachNlri.add(bgpLSNlri);
                }
            } else {
                throw new BgpParseException("Not Supporting afi " + afi + "safi " + safi);
            }
        }
        return new MpReachNlri(mpReachNlri, afi, safi, ipNextHop, parseFlags.getLength());
    }

    @Override
    public short getType() {
        return MPREACHNLRI_TYPE;
    }

    /**
     * Returns AFI.
     *
     * @return AFI
     */
    public short afi() {
        return this.afi;
    }

    /**
     * Returns Nexthop IpAddress.
     *
     * @return Nexthop IpAddress
     */
    public Ip4Address nexthop4() {
        return this.ipNextHop;
    }

    /**
     * Returns SAFI.
     *
     * @return SAFI
     */
    public byte safi() {
        return this.safi;
    }

    @Override
    public int write(ChannelBuffer cb) {
        //Not to be Implemented as of now
        return 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("mpReachNlri", mpReachNlri)
                .add("afi", afi)
                .add("safi", safi)
                .add("ipNextHop", ipNextHop)
                .add("length", length)
                .toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}