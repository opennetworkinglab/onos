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
package org.onosproject.bgpio.protocol.ver4;

import java.util.LinkedList;
import java.util.ListIterator;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpMessageReader;
import org.onosproject.bgpio.protocol.BgpMessageWriter;
import org.onosproject.bgpio.protocol.BgpOpenMsg;
import org.onosproject.bgpio.protocol.BgpType;
import org.onosproject.bgpio.protocol.BgpVersion;
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.types.BgpHeader;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.FourOctetAsNumCapabilityTlv;
import org.onosproject.bgpio.types.MultiProtocolExtnCapabilityTlv;
import org.onosproject.bgpio.types.RouteRefreshCapabilityTlv;
import org.onosproject.bgpio.util.Validation;
import org.onosproject.bgpio.util.Constants;
import org.onosproject.bgpio.types.RpdCapabilityTlv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides BGP open message.
 */
public class BgpOpenMsgVer4 implements BgpOpenMsg {

    /*
       0                   1                   2                   3
       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+
       |    Version    |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |    My Autonomous System       |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |         Hold Time             |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                  BGP Identifier                             |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       | Opt Parm Len  |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |             Optional Parameters (variable)                  |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       OPEN Message Format
       REFERENCE : RFC 4271
    */

    private static final Logger log = LoggerFactory.getLogger(BgpOpenMsgVer4.class);

    public static final byte PACKET_VERSION = 4;
    public static final int OPEN_MSG_MINIMUM_LENGTH = 10;
    public static final int MSG_HEADER_LENGTH = 19;
    public static final int MARKER_LENGTH  = 16;
    public static final int DEFAULT_HOLD_TIME = 120;
    public static final short AS_TRANS = 23456;
    public static final int OPT_PARA_TYPE_CAPABILITY = 2;
    public static final BgpType MSG_TYPE = BgpType.OPEN;
    public static final short AFI = 16388;
    public static final byte SAFI = 71;
    public static final byte RES = 0;
    public static final int FOUR_OCTET_AS_NUM_CAPA_TYPE = 65;
    private static final byte[] MARKER = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
    public static final BgpHeader DEFAULT_OPEN_HEADER = new BgpHeader(MARKER,
        (short) OPEN_MSG_MINIMUM_LENGTH, (byte) 0X01);
    private BgpHeader bgpMsgHeader;
    private byte version;
    private long asNumber;
    private short holdTime;
    private int bgpId;
    private boolean isLargeAsCapabilitySet;
    private LinkedList<BgpValueType> capabilityTlv;

    public static final BgpOpenMsgVer4.Reader READER = new Reader();

    /**
     * reset variables.
     */
    public BgpOpenMsgVer4() {
        this.bgpMsgHeader = null;
        this.version = 0;
        this.holdTime = 0;
        this.asNumber = 0;
        this.bgpId = 0;
        this.capabilityTlv = null;
    }

    /**
     * Constructor to initialize all variables of BGP Open message.
     *
     * @param bgpMsgHeader BGP Header in open message
     * @param version BGP version in open message
     * @param holdTime hold time in open message
     * @param asNumber AS number in open message
     * @param bgpId BGP identifier in open message
     * @param capabilityTlv capabilities in open message
     */
    public BgpOpenMsgVer4(BgpHeader bgpMsgHeader, byte version, long asNumber, short holdTime,
             int bgpId, LinkedList<BgpValueType> capabilityTlv) {
        this.bgpMsgHeader = bgpMsgHeader;
        this.version = version;
        this.asNumber = asNumber;
        this.holdTime = holdTime;
        this.bgpId = bgpId;
        this.capabilityTlv = capabilityTlv;
    }

    @Override
    public BgpHeader getHeader() {
        return this.bgpMsgHeader;
    }

    @Override
    public BgpVersion getVersion() {
        return BgpVersion.BGP_4;
    }

    @Override
    public BgpType getType() {
        return MSG_TYPE;
    }

    @Override
    public short getHoldTime() {
        return this.holdTime;
    }

    @Override
    public long getAsNumber() {
        return this.asNumber;
    }

    @Override
    public int getBgpId() {
        return this.bgpId;
    }

    @Override
    public LinkedList<BgpValueType> getCapabilityTlv() {
        return this.capabilityTlv;
    }

    /**
     * Reader class for reading BGP open message from channel buffer.
     */
    public static class Reader implements BgpMessageReader<BgpOpenMsg> {

        @Override
        public BgpOpenMsg readFrom(ChannelBuffer cb, BgpHeader bgpHeader) throws BgpParseException {

            byte version;
            short holdTime;
            long asNumber;
            int bgpId;
            byte optParaLen = 0;
            byte optParaType;
            byte capParaLen = 0;
            LinkedList<BgpValueType> capabilityTlv = new LinkedList<>();

            if (cb.readableBytes() < OPEN_MSG_MINIMUM_LENGTH) {
                log.error("[readFrom] Invalid length: Packet size is less than the minimum length ");
                Validation.validateLen(BgpErrorType.OPEN_MESSAGE_ERROR, BgpErrorType.BAD_MESSAGE_LENGTH,
                        cb.readableBytes());
            }

            // Read version
            version = cb.readByte();
            if (version != PACKET_VERSION) {
                log.error("[readFrom] Invalid version: " + version);
                throw new BgpParseException(BgpErrorType.OPEN_MESSAGE_ERROR,
                        BgpErrorType.UNSUPPORTED_VERSION_NUMBER, null);
            }

            // Read AS number
            asNumber = cb.getUnsignedShort(cb.readerIndex());
            cb.readShort();
            log.debug("AS number read");

            // Read Hold timer
            holdTime = cb.readShort();
            log.debug("Holding time read");

            // Read BGP Identifier
            bgpId = cb.readInt();
            log.debug("BGP identifier read");

            // Read optional parameter length
            optParaLen = cb.readByte();
            log.debug("OPtional parameter length read");

            // Read Capabilities if optional parameter length is greater than 0
            if (optParaLen != 0) {
                while (cb.readableBytes() > 0) {
                    // Read optional parameter type
                    optParaType = cb.readByte();

                    // Read optional parameter length
                    capParaLen = cb.readByte();

                    if (cb.readableBytes() < capParaLen) {
                        throw new BgpParseException(BgpErrorType.OPEN_MESSAGE_ERROR, (byte) 0, null);
                    }

                    ChannelBuffer capaCb = cb.readBytes(capParaLen);

                    // Parse capabilities only if optional parameter type is 2
                    if ((optParaType == OPT_PARA_TYPE_CAPABILITY) && (capParaLen != 0)) {
                        //Observed that some routers send a list of capabilities, while others send a list
                        //of optional parameters. This takes care of both
                        LinkedList<BgpValueType> currentCapabilityTlv = parseCapabilityTlv(capaCb);
                        capabilityTlv.addAll(currentCapabilityTlv);
                    } else {
                        throw new BgpParseException(BgpErrorType.OPEN_MESSAGE_ERROR,
                                BgpErrorType.UNSUPPORTED_OPTIONAL_PARAMETER, null);
                    }
                }
            }
            return new BgpOpenMsgVer4(bgpHeader, version, asNumber, holdTime, bgpId, capabilityTlv);
        }
    }

    /**
     * Parsing capabilities.
     *
     * @param cb of type channel buffer
     * @return capabilityTlv of open message
     * @throws BgpParseException while parsing capabilities
     */
    protected static LinkedList<BgpValueType> parseCapabilityTlv(ChannelBuffer cb) throws BgpParseException {

        LinkedList<BgpValueType> capabilityTlv = new LinkedList<>();

        while (cb.readableBytes() > 0) {
            BgpValueType tlv;
            short type = cb.readByte();
            short length = cb.readByte();

            switch (type) {
            case FourOctetAsNumCapabilityTlv.TYPE:
                log.debug("FourOctetAsNumCapabilityTlv");
                if (FourOctetAsNumCapabilityTlv.LENGTH != length) {
                    throw new BgpParseException("Invalid length received for FourOctetAsNumCapabilityTlv.");
                }
                if (length > cb.readableBytes()) {
                    throw new BgpParseException("Four octet as num tlv length"
                            + " is more than readableBytes.");
                }
                int as4Num = cb.readInt();
                tlv = new FourOctetAsNumCapabilityTlv(as4Num);
                break;
            case RpdCapabilityTlv.TYPE:
                log.debug("RpdCapability");
                if (RpdCapabilityTlv.LENGTH != length) {
                    throw new BgpParseException("Invalid length received for RpdCapability.");
                }
                if (length > cb.readableBytes()) {
                    throw new BgpParseException("Four octet as num TLV length"
                            + " is more than readableBytes.");
                }
                short rpdAfi = cb.readShort();
                byte rpdAsafi = cb.readByte();
                byte sendReceive = cb.readByte();
                tlv = new RpdCapabilityTlv(sendReceive);
                break;

            case MultiProtocolExtnCapabilityTlv.TYPE:
                log.debug("MultiProtocolExtnCapabilityTlv");

                if (MultiProtocolExtnCapabilityTlv.LENGTH != length) {
                    throw new BgpParseException("Invalid length received for MultiProtocolExtnCapabilityTlv.");
                }

                if (length > cb.readableBytes()) {
                    throw new BgpParseException("BGP LS tlv length is more than readableBytes.");
                }
                short afi = cb.readShort();
                byte res = cb.readByte();
                byte safi = cb.readByte();
                tlv = new MultiProtocolExtnCapabilityTlv(afi, res, safi);

                break;
            case RouteRefreshCapabilityTlv.TYPE:
                log.debug("RouteRefreshCapabilityTlv");

                if (RouteRefreshCapabilityTlv.LENGTH != length) {
                    throw new BgpParseException("Invalid length received for RouteRefreshCapabilityTlv.");
                }

                tlv = new RouteRefreshCapabilityTlv(true);
                break;
            default:
                log.debug("Warning: Unsupported TLV: " + type);
                cb.skipBytes(length);
                continue;
            }
            capabilityTlv.add(tlv);
        }
        return capabilityTlv;
    }

    /**
     * Builder class for BGP open message.
     */
    static class Builder implements BgpOpenMsg.Builder {

        private boolean isHeaderSet = false;
        private BgpHeader bgpMsgHeader;
        private boolean isHoldTimeSet = false;
        private short holdTime;
        private boolean isAsNumSet = false;
        private short asNumber;
        private boolean isBgpIdSet = false;
        private int bgpId;
        private boolean isIpV4UnicastCapabilityTlvSet = true;
        private boolean isIpV6UnicastCapabilityTlvSet = false;
        private boolean isLargeAsCapabilityTlvSet = false;
        private boolean isLsCapabilityTlvSet = false;
        private boolean isFlowSpecCapabilityTlvSet = false;
        private boolean isVpnFlowSpecCapabilityTlvSet = false;
        private boolean isFlowSpecRpdCapabilityTlvSet = false;
        private boolean isEvpnCapabilityTlvSet = false;

        LinkedList<BgpValueType> capabilityTlv = new LinkedList<>();

        @Override
        public BgpOpenMsg build() throws BgpParseException {
            BgpHeader bgpMsgHeader = this.isHeaderSet ? this.bgpMsgHeader : DEFAULT_OPEN_HEADER;
            short holdTime = this.isHoldTimeSet ? this.holdTime : DEFAULT_HOLD_TIME;

            if (!this.isAsNumSet) {
                throw new BgpParseException("BGP AS number is not set (mandatory)");
            }

            if (!this.isBgpIdSet) {
                throw new BgpParseException("BGPID  is not set (mandatory)");
            }

            if (this.isIpV4UnicastCapabilityTlvSet) {
                BgpValueType tlv;
                tlv = new MultiProtocolExtnCapabilityTlv((short) Constants.AFI_IPV4_UNICAST, RES,
                                                         (byte) Constants.SAFI_UNICAST);
                this.capabilityTlv.add(tlv);
            }

            if (this.isIpV6UnicastCapabilityTlvSet) {
                BgpValueType tlv;
                tlv = new MultiProtocolExtnCapabilityTlv((short) Constants.AFI_IPV6_UNICAST, RES,
                                                         (byte) Constants.SAFI_UNICAST);
                this.capabilityTlv.add(tlv);
            }

            if (this.isLargeAsCapabilityTlvSet) {
                BgpValueType tlv;
                int value = this.asNumber;
                tlv = new FourOctetAsNumCapabilityTlv(value);
                this.capabilityTlv.add(tlv);
            }

            if (this.isLsCapabilityTlvSet) {
                BgpValueType tlv;
                tlv = new MultiProtocolExtnCapabilityTlv(AFI, RES, SAFI);
                this.capabilityTlv.add(tlv);
            }

            if (this.isFlowSpecCapabilityTlvSet) {
                BgpValueType tlv;
                tlv = new MultiProtocolExtnCapabilityTlv(Constants.AFI_FLOWSPEC_VALUE,
                                                         RES, Constants.SAFI_FLOWSPEC_VALUE);
                this.capabilityTlv.add(tlv);
            }

            if (this.isVpnFlowSpecCapabilityTlvSet) {
                BgpValueType tlv;
                tlv = new MultiProtocolExtnCapabilityTlv(Constants.AFI_FLOWSPEC_VALUE,
                                                         RES, Constants.VPN_SAFI_FLOWSPEC_VALUE);
                this.capabilityTlv.add(tlv);
            }

            if (this.isFlowSpecRpdCapabilityTlvSet) {
                BgpValueType tlv;
                tlv = new RpdCapabilityTlv(Constants.RPD_CAPABILITY_SEND_VALUE);
                this.capabilityTlv.add(tlv);
            }

            if (this.isEvpnCapabilityTlvSet) {
                BgpValueType tlv;
                tlv = new MultiProtocolExtnCapabilityTlv(Constants.AFI_EVPN_VALUE,
                        RES, Constants.SAFI_EVPN_VALUE);
                this.capabilityTlv.add(tlv);
            }

            return new BgpOpenMsgVer4(bgpMsgHeader, PACKET_VERSION, this.asNumber, holdTime, this.bgpId,
                       this.capabilityTlv);
        }

        @Override
        public Builder setHeader(BgpHeader bgpMsgHeader) {
            this.bgpMsgHeader = bgpMsgHeader;
            return this;
        }

        @Override
        public Builder setHoldTime(short holdTime) {
            this.holdTime = holdTime;
            this.isHoldTimeSet = true;
            return this;
        }

        @Override
        public Builder setAsNumber(short asNumber) {
            this.asNumber = asNumber;
            this.isAsNumSet = true;
            return this;
        }

        @Override
        public Builder setBgpId(int bgpId) {
            this.bgpId = bgpId;
            this.isBgpIdSet = true;
            return this;
        }

        @Override
        public Builder setCapabilityTlv(LinkedList<BgpValueType> capabilityTlv) {
            this.capabilityTlv = capabilityTlv;
            return this;
        }

        @Override
        public Builder setLargeAsCapabilityTlv(boolean isLargeAsCapabilitySet) {
            this.isLargeAsCapabilityTlvSet = isLargeAsCapabilitySet;
            return this;
        }

        @Override
        public Builder setLsCapabilityTlv(boolean isLsCapabilitySet) {
            this.isLsCapabilityTlvSet = isLsCapabilitySet;
            return this;
        }

        @Override
        public Builder setFlowSpecCapabilityTlv(boolean isFlowSpecCapabilitySet) {
            this.isFlowSpecCapabilityTlvSet = isFlowSpecCapabilitySet;
            return this;
        }

        @Override
        public Builder setVpnFlowSpecCapabilityTlv(boolean isVpnFlowSpecCapabilitySet) {
            this.isVpnFlowSpecCapabilityTlvSet = isVpnFlowSpecCapabilitySet;
            return this;
        }

        @Override
        public Builder setFlowSpecRpdCapabilityTlv(boolean isFlowSpecRpdCapabilityTlvSet) {
            this.isFlowSpecRpdCapabilityTlvSet = isFlowSpecRpdCapabilityTlvSet;
            return this;
        }

        @Override
        public Builder setEvpnCapabilityTlv(boolean isEvpnCapabilitySet) {
            this.isEvpnCapabilityTlvSet = isEvpnCapabilitySet;
            return this;
        }

        @Override
        public Builder setIpV4UnicastCapabilityTlvSet(boolean isIpV4UnicastCapabilityTlvSet) {
            this.isIpV4UnicastCapabilityTlvSet = isIpV4UnicastCapabilityTlvSet;
            return this;
        }

        @Override
        public Builder setIpV6UnicastCapabilityTlvSet(boolean isIpV6UnicastCapabilityTlvSet) {
            this.isIpV6UnicastCapabilityTlvSet = isIpV6UnicastCapabilityTlvSet;
            return this;
        }
    }

    @Override
    public void writeTo(ChannelBuffer cb) {
        try {
            WRITER.write(cb, this);
        } catch (BgpParseException e) {
            log.debug("[writeTo] Error: " + e.toString());
        }
    }

    public static final Writer WRITER = new Writer();

    /**
     * Writer class for writing BGP open message to channel buffer.
     */
    public static class Writer implements BgpMessageWriter<BgpOpenMsgVer4> {

        @Override
        public void write(ChannelBuffer cb, BgpOpenMsgVer4 message) throws BgpParseException {

            int optParaLen = 0;
            int as4num = 0;

            int startIndex = cb.writerIndex();

            // write common header and get msg length index
            int msgLenIndex = message.bgpMsgHeader.write(cb);

            if (msgLenIndex <= 0) {
                throw new BgpParseException("Unable to write message header.");
            }

            // write version in 1-octet
            cb.writeByte(message.version);

            // get as4num if LS Capability is set
            if (message.isLargeAsCapabilitySet) {
                LinkedList<BgpValueType> capabilityTlv = message
                        .getCapabilityTlv();
                ListIterator<BgpValueType> listIterator = capabilityTlv
                        .listIterator();

                while (listIterator.hasNext()) {
                    BgpValueType tlv = listIterator.next();
                    if (tlv.getType() == FOUR_OCTET_AS_NUM_CAPA_TYPE) {
                        as4num = ((FourOctetAsNumCapabilityTlv) tlv).getInt();
                        break;
                    }
                }
            }

            if ((message.isLargeAsCapabilitySet) && (as4num > 65535)) {
                // write As number as AS_TRANS
                cb.writeShort(AS_TRANS);
            } else {
                // write AS number in next 2-octet
                cb.writeShort((short) message.asNumber);
            }

            // write HoldTime in next 2-octet
            cb.writeShort(message.holdTime);

            // write BGP Identifier in next 4-octet
            cb.writeInt(message.bgpId);

            // store the index of Optional parameter length
            int optParaLenIndex = cb.writerIndex();

            // set optional parameter length as 0
            cb.writeByte(0);

            // Pack capability TLV
            optParaLen = message.packCapabilityTlv(cb, message);

            if (optParaLen != 0) {
                // Update optional parameter length
                cb.setByte(optParaLenIndex, (byte) (optParaLen + 2)); //+2 for optional parameter type.
            }

            // write OPEN Object Length
            int length = cb.writerIndex() - startIndex;
            cb.setShort(msgLenIndex, (short) length);
            message.bgpMsgHeader.setLength((short) length);
        }
    }

    /**
     * returns length of capability tlvs.
     *
     * @param cb of type channel buffer
     * @param message of type BGPOpenMsgVer4
     * @return capParaLen of open message
     */
    protected int packCapabilityTlv(ChannelBuffer cb, BgpOpenMsgVer4 message) {
        int startIndex = cb.writerIndex();
        int capParaLen = 0;
        int capParaLenIndex = 0;

        LinkedList<BgpValueType> capabilityTlv = message.capabilityTlv;
        ListIterator<BgpValueType> listIterator = capabilityTlv.listIterator();

        if (listIterator.hasNext()) {
            // Set optional parameter type as 2
            cb.writeByte(OPT_PARA_TYPE_CAPABILITY);

            // Store the index of capability parameter length and update length at the end
            capParaLenIndex = cb.writerIndex();

            // Set capability parameter length as 0
            cb.writeByte(0);

            // Update the startIndex to know the length of capability tlv
            startIndex = cb.writerIndex();
        }

        while (listIterator.hasNext()) {
            BgpValueType tlv = listIterator.next();
            if (tlv == null) {
                log.debug("Warning: TLV is null from CapabilityTlv list");
                continue;
            }
            tlv.write(cb);
        }

        capParaLen = cb.writerIndex() - startIndex;

        if (capParaLen != 0) {
            // Update capability parameter length
            cb.setByte(capParaLenIndex, (byte) capParaLen);
        }
        return capParaLen;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("bgpMsgHeader", bgpMsgHeader)
            .add("version", version)
            .add("holdTime", holdTime)
            .add("asNumber", asNumber)
            .add("bgpId", bgpId)
            .add("capabilityTlv", capabilityTlv)
            .toString();
    }
}
