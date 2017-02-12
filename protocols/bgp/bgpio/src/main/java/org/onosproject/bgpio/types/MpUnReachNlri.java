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

package org.onosproject.bgpio.types;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpLSNlri;
import org.onosproject.bgpio.protocol.flowspec.BgpFlowSpecNlri;
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
    public static final byte FLAGS = (byte) 0x90;
    private boolean isMpUnReachNlri = false;
    private final short afi;
    private final byte safi;
    private final List<BgpLSNlri> mpUnReachNlri;
    private final int length;
    private BgpFlowSpecNlri bgpFlowSpecNlri;

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

    public MpUnReachNlri(BgpFlowSpecNlri bgpFlowSpecNlri, short afi, byte safi) {
        this.mpUnReachNlri = null;
        this.isMpUnReachNlri = true;
        this.length = 0;
        this.bgpFlowSpecNlri = bgpFlowSpecNlri;
        this.afi = afi;
        this.safi = safi;
    }

    /**
     * Returns BGP flow specification info.
     *
     * @return BGP flow specification info
     */
    public BgpFlowSpecNlri bgpFlowSpecNlri() {
        return this.bgpFlowSpecNlri;
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
                        break;
                    }
                    mpUnReachNlri.add(bgpLSNlri);
                }
            } else if ((afi == Constants.AFI_FLOWSPEC_VALUE)
                                               && ((safi == Constants.SAFI_FLOWSPEC_VALUE)
                                               || (safi == Constants.VPN_SAFI_FLOWSPEC_VALUE))) {
                List<BgpValueType> flowSpecComponents = new LinkedList<>();
                RouteDistinguisher routeDistinguisher = null;
                if (tempCb.readableBytes() > 0) {
                    BgpValueType flowSpecComponent = null;

                    if (safi == Constants.VPN_SAFI_FLOWSPEC_VALUE) {
                        routeDistinguisher = new RouteDistinguisher();
                        routeDistinguisher = RouteDistinguisher.read(tempCb);
                    }
                    while (tempCb.readableBytes() > 0) {
                        short totNlriLen = tempCb.getUnsignedByte(tempCb.readerIndex());
                        if (totNlriLen >= BgpFlowSpecNlri.FLOW_SPEC_LEN) {
                           if (tempCb.readableBytes() < 2) {
                                Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                    BgpErrorType.ATTRIBUTE_LENGTH_ERROR, totNlriLen);
                            }
                            totNlriLen = tempCb.readShort();
                        } else {
                            totNlriLen = tempCb.readByte();
                        }
                        if (tempCb.readableBytes() < totNlriLen) {
                            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                    BgpErrorType.ATTRIBUTE_LENGTH_ERROR, totNlriLen);
                        }
                        tempBuf = tempCb.readBytes(totNlriLen);
                        while (tempBuf.readableBytes() > 0) {
                            short type = tempBuf.readByte();
                            switch (type) {
                            case Constants.BGP_FLOWSPEC_DST_PREFIX:
                                flowSpecComponent = BgpFsDestinationPrefix.read(tempBuf);
                                break;
                            case Constants.BGP_FLOWSPEC_SRC_PREFIX:
                                flowSpecComponent = BgpFsSourcePrefix.read(tempBuf);
                                break;
                            case Constants.BGP_FLOWSPEC_IP_PROTO:
                                flowSpecComponent = BgpFsIpProtocol.read(tempBuf);
                                break;
                            case Constants.BGP_FLOWSPEC_PORT:
                                flowSpecComponent = BgpFsPortNum.read(tempBuf);
                                break;
                            case Constants.BGP_FLOWSPEC_DST_PORT:
                                flowSpecComponent = BgpFsDestinationPortNum.read(tempBuf);
                                break;
                            case Constants.BGP_FLOWSPEC_SRC_PORT:
                                flowSpecComponent = BgpFsSourcePortNum.read(tempBuf);
                                break;
                            case Constants.BGP_FLOWSPEC_ICMP_TP:
                                flowSpecComponent = BgpFsIcmpType.read(tempBuf);
                                break;
                            case Constants.BGP_FLOWSPEC_ICMP_CD:
                                flowSpecComponent = BgpFsIcmpCode.read(tempBuf);
                                break;
                            case Constants.BGP_FLOWSPEC_TCP_FLAGS:
                                flowSpecComponent = BgpFsTcpFlags.read(tempBuf);
                                break;
                            case Constants.BGP_FLOWSPEC_PCK_LEN:
                                flowSpecComponent = BgpFsPacketLength.read(tempBuf);
                                break;
                            case Constants.BGP_FLOWSPEC_DSCP:
                                flowSpecComponent = BgpFsDscpValue.read(tempBuf);
                                break;
                            case Constants.BGP_FLOWSPEC_FRAGMENT:
                                flowSpecComponent = BgpFsFragment.read(tempBuf);
                                break;
                            default:
                                log.debug("flow spec type not supported" + type);
                                break;
                            }
                            flowSpecComponents.add(flowSpecComponent);
                        }
                    }
                }
                BgpFlowSpecNlri flowSpecDetails = new BgpFlowSpecNlri(flowSpecComponents);
                flowSpecDetails.setRouteDistinguiher(routeDistinguisher);
                return new MpUnReachNlri(flowSpecDetails, afi, safi);
            } else {
                //TODO: check with the values got from capability
                throw new BgpParseException("Not Supporting afi " + afi + "safi " + safi);
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
        int iLenStartIndex = cb.writerIndex();
        if ((afi == Constants.AFI_FLOWSPEC_VALUE) && ((safi == Constants.SAFI_FLOWSPEC_VALUE) ||
            (safi == Constants.VPN_SAFI_FLOWSPEC_VALUE))) {
            List<BgpValueType> flowSpec = bgpFlowSpecNlri.flowSpecComponents();
            ListIterator<BgpValueType> listIterator = flowSpec.listIterator();
            boolean isAllFlowTypesIdentical = true;

            cb.writeByte(FLAGS);
            cb.writeByte(MPUNREACHNLRI_TYPE);

            int mpUnReachIndx = cb.writerIndex();
            cb.writeShort(0);

            cb.writeShort(afi);
            cb.writeByte(safi);

            if (bgpFlowSpecNlri.routeDistinguisher() != null) {
                cb.writeLong(bgpFlowSpecNlri.routeDistinguisher().getRouteDistinguisher());
            }

            BgpValueType tlv1 = null;
            if (listIterator.hasNext()) {
                tlv1 = listIterator.next();
            }
            while (listIterator.hasNext()) {
                BgpValueType tlv = listIterator.next();
                if (tlv.getType() != tlv1.getType()) {
                    isAllFlowTypesIdentical = false;
                    break;
                }
            }

            if (isAllFlowTypesIdentical) {
                BgpFlowSpecNlri.updateBufferIdenticalFlowTypes(cb, bgpFlowSpecNlri());
            } else {
                BgpFlowSpecNlri.updateBufferNonIdenticalFlowTypes(cb, bgpFlowSpecNlri());
            }
            int fsNlriLen = cb.writerIndex() - mpUnReachIndx;
            cb.setShort(mpUnReachIndx, (short) (fsNlriLen - 2));
        }

        return cb.writerIndex() - iLenStartIndex;
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues()
                .add("mpReachNlri", mpUnReachNlri)
                .add("bgpFlowSpecNlri", bgpFlowSpecNlri)
                .add("afi", afi)
                .add("safi", safi)
                .add("length", length)
                .toString();
    }
}