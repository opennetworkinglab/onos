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

import java.util.LinkedList;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpLSNlri;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpPrefixIPv4LSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLsNlriVer4;
import org.onosproject.bgpio.util.Constants;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides Implementation of MpUnReach Nlri BGP Path Attribute.
 */
public class MpUnReachNlri implements BgpValueType {

    private static final Logger log = LoggerFactory.getLogger(MpUnReachNlri.class);
    public static final byte MPUNREACHNLRI_TYPE = 15;
    public static final byte LINK_NLRITYPE = 2;

    private boolean isMpUnReachNlri = false;
    private final short afi;
    private final byte safi;
    private final List<BgpLSNlri> mpUnReachNlri;
    private final int length;

    /**
     * Constructor to initialize parameters.
     *
     * @param mpUnReachNlri MpUnReach  Nlri attribute
     * @param afi address family identifier
     * @param safi subsequent address family identifier
     * @param length of MpUnReachNlri
     */
    public MpUnReachNlri(List<BgpLSNlri> mpUnReachNlri, short afi, byte safi,
                  int length) {
        this.mpUnReachNlri = mpUnReachNlri;
        this.isMpUnReachNlri = true;
        this.afi = afi;
        this.safi = safi;
        this.length = length;
    }

    /**
     * Reads from ChannelBuffer and parses MpUnReachNlri.
     *
     * @param cb ChannelBuffer
     * @return object of MpUnReachNlri
     * @throws BgpParseException while parsing MpUnReachNlri
     */
    public static MpUnReachNlri read(ChannelBuffer cb) throws BgpParseException {
        ChannelBuffer tempBuf = cb.copy();
        Validation parseFlags = Validation.parseAttributeHeader(cb);
        int len = parseFlags.isShort() ? parseFlags.getLength() + Constants.TYPE_AND_LEN_AS_SHORT
                                      : parseFlags.getLength() + Constants.TYPE_AND_LEN_AS_BYTE;
        ChannelBuffer data = tempBuf.readBytes(len);

        if (!parseFlags.getFirstBit() && parseFlags.getSecondBit()
                && parseFlags.getThirdBit()) {
            throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                        BgpErrorType.ATTRIBUTE_FLAGS_ERROR, data);
        }

        if (cb.readableBytes() < parseFlags.getLength()) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR, parseFlags.getLength());
        }

        LinkedList<BgpLSNlri> mpUnReachNlri = new LinkedList<>();
        BgpLSNlri bgpLSNlri = null;
        short afi = 0;
        byte safi = 0;
        ChannelBuffer tempCb = cb.readBytes(parseFlags.getLength());
        while (tempCb.readableBytes() > 0) {
            afi = tempCb.readShort();
            safi = tempCb.readByte();

            //Supporting only for AFI 16388 / SAFI 71
            if ((afi == Constants.AFI_VALUE) && (safi == Constants.SAFI_VALUE)
                    || (afi == Constants.AFI_VALUE) && (safi == Constants.VPN_SAFI_VALUE)) {
                while (tempCb.readableBytes() > 0) {
                    short nlriType = tempCb.readShort();
                    short totNlriLen = tempCb.readShort();
                    if (tempCb.readableBytes() < totNlriLen) {
                        Validation.validateLen(
                                BgpErrorType.UPDATE_MESSAGE_ERROR,
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
                        bgpLSNlri = BgpPrefixIPv4LSNlriVer4.read(tempBuf, afi,
                                                                 safi);
                        break;
                    default:
                        log.debug("nlriType not supported" + nlriType);
                    }
                    mpUnReachNlri.add(bgpLSNlri);
                }
            } else {
                //TODO: check with the values got from capability
                throw new BgpParseException("Not Supporting afi " + afi
                        + "safi " + safi);
            }
        }
        return new MpUnReachNlri(mpUnReachNlri, afi, safi,
                                 parseFlags.getLength());
    }

    @Override
    public short getType() {
        return MPUNREACHNLRI_TYPE;
    }

    /**
     * Returns SAFI.
     *
     * @return SAFI
     */
    public byte safi() {
        return this.safi;
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
     * Returns list of MpUnReach Nlri.
     *
     * @return list of MpUnReach Nlri
     */
    public List<BgpLSNlri> mpUnReachNlri() {
        return this.mpUnReachNlri;
    }

    /**
     * Returns whether MpReachNlri is present.
     *
     * @return whether MpReachNlri is present
     */
    public boolean isMpUnReachNlriSet() {
        return this.isMpUnReachNlri;
    }

    /**
     * Returns length of MpUnReach.
     *
     * @return length of MpUnReach
     */
    public int mpUnReachNlriLen() {
        return this.length;
    }

    @Override
    public int write(ChannelBuffer cb) {
        //Not to be Implemented as of now
        return 0;
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("mpReachNlri", mpUnReachNlri)
                .add("afi", afi)
                .add("safi", safi)
                .add("length", length)
                .toString();
    }
}