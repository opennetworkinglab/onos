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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.IpPrefix;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpMessageReader;
import org.onosproject.bgpio.protocol.BgpMessageWriter;
import org.onosproject.bgpio.protocol.BgpType;
import org.onosproject.bgpio.protocol.BgpUpdateMsg;
import org.onosproject.bgpio.protocol.BgpVersion;
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.types.BgpHeader;
import org.onosproject.bgpio.types.MpReachNlri;
import org.onosproject.bgpio.types.MpUnReachNlri;
import org.onosproject.bgpio.util.Constants;
import org.onosproject.bgpio.util.Validation;

import org.onosproject.bgpio.types.BgpValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * BGP Update Message: UPDATE messages are used to transfer routing information
 * between BGP peers. The information in the UPDATE message is used by core to
 * construct a graph
 */
public class BgpUpdateMsgVer4 implements BgpUpdateMsg {

    /*      0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                                                               |
    +                                                               +
    |                                                               |
    +                                                               +
    |                           Marker                              |
    +                                                               +
    |                                                               |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |          Length               |      Type     |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |   Withdrawn Routes Length (2 octets)                |
    +-----------------------------------------------------+
    |   Withdrawn Routes (variable)                       |
    +-----------------------------------------------------+
    |   Total Path Attribute Length (2 octets)            |
    +-----------------------------------------------------+
    |   Path Attributes (variable)                        |
    +-----------------------------------------------------+
    |   Network Layer Reachability Information (variable) |
    +-----------------------------------------------------+
    REFERENCE : RFC 4271
    */

    private static final Logger log = LoggerFactory
            .getLogger(BgpUpdateMsgVer4.class);

    public static final byte PACKET_VERSION = 4;
    //Withdrawn Routes Length(2) + Total Path Attribute Length(2)
    public static final int PACKET_MINIMUM_LENGTH = 4;
    public static final int MARKER_LENGTH = 16;
    public static final int BYTE_IN_BITS = 8;
    public static final int MIN_LEN_AFTER_WITHDRW_ROUTES = 2;
    public static final int MINIMUM_COMMON_HEADER_LENGTH = 19;
    public static final BgpType MSG_TYPE = BgpType.UPDATE;
    private static byte[] marker = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                              (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                              (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                              (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
    public static final BgpHeader DEFAULT_UPDATE_HEADER = new BgpHeader(marker,
                                                                        (short) PACKET_MINIMUM_LENGTH, (byte) 0X02);
    public static final BgpUpdateMsgVer4.Reader READER = new Reader();

    private List<IpPrefix> withdrawnRoutes;
    private BgpPathAttributes bgpPathAttributes;
    private BgpHeader bgpHeader;
    private List<IpPrefix> nlri;

    /**
     * Constructor to initialize parameters for BGP Update message.
     *
     * @param bgpHeader in Update message
     * @param withdrawnRoutes withdrawn routes
     * @param bgpPathAttributes BGP Path attributes
     * @param nlri Network Layer Reachability Information
     */
    public BgpUpdateMsgVer4(BgpHeader bgpHeader, List<IpPrefix> withdrawnRoutes,
                     BgpPathAttributes bgpPathAttributes, List<IpPrefix> nlri) {
        this.bgpHeader = bgpHeader;
        this.withdrawnRoutes = withdrawnRoutes;
        this.bgpPathAttributes = bgpPathAttributes;
        this.nlri = nlri;
    }

    /**
     * Reader reads BGP Update Message from the channel buffer.
     */
    static class Reader implements BgpMessageReader<BgpUpdateMsg> {

        @Override
        public BgpUpdateMsg readFrom(ChannelBuffer cb, BgpHeader bgpHeader)
                throws BgpParseException {

            if (cb.readableBytes() != (bgpHeader.getLength() - MINIMUM_COMMON_HEADER_LENGTH)) {
                Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                        BgpErrorType.BAD_MESSAGE_LENGTH, bgpHeader.getLength());
            }

            LinkedList<IpPrefix> withDrwRoutes = new LinkedList<>();
            LinkedList<IpPrefix> nlri = new LinkedList<>();
            BgpPathAttributes bgpPathAttributes = new BgpPathAttributes();

            Short withDrwLen = cb.readShort();

            if (cb.readableBytes() < withDrwLen) {
                Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                        BgpErrorType.MALFORMED_ATTRIBUTE_LIST,
                        cb.readableBytes());
            }
            log.debug("Reading withdrawn routes length");
            ChannelBuffer tempCb = cb.readBytes(withDrwLen);
            if (withDrwLen != 0) {
                // Parsing WithdrawnRoutes
                withDrwRoutes = parseWithdrawnRoutes(tempCb);
                log.debug("Withdrawn routes parsed");
            }
            if (cb.readableBytes() < MIN_LEN_AFTER_WITHDRW_ROUTES) {
                log.debug("Bgp path attribute len field not present");
                throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR,
                        BgpErrorType.MALFORMED_ATTRIBUTE_LIST, null);
            }

            // Reading Total Path Attribute Length
            short totPathAttrLen = cb.readShort();
            int len = withDrwLen + totPathAttrLen + PACKET_MINIMUM_LENGTH;
            if (len > bgpHeader.getLength()) {
                throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR,
                        BgpErrorType.MALFORMED_ATTRIBUTE_LIST, null);
            }
            log.debug("Total path attribute length read");
            if (totPathAttrLen != 0) {
                // Parsing BGPPathAttributes
                if (cb.readableBytes() < totPathAttrLen) {
                    Validation
                            .validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                         BgpErrorType.MALFORMED_ATTRIBUTE_LIST,
                                         cb.readableBytes());
                }
                tempCb = cb.readBytes(totPathAttrLen);
                bgpPathAttributes = BgpPathAttributes.read(tempCb);
            }
            if (cb.readableBytes() > 0) {
                // Parsing NLRI
                nlri = parseNlri(cb);
            }
            return new BgpUpdateMsgVer4(bgpHeader, withDrwRoutes,
                    bgpPathAttributes, nlri);
        }
    }

    /**
     * Builder class for BGP update message.
     */
    static class Builder implements BgpUpdateMsg.Builder {
        BgpHeader bgpMsgHeader = null;
        BgpPathAttributes bgpPathAttributes;
        List<IpPrefix> withdrawnRoutes;
        List<IpPrefix> nlri;

        @Override
        public BgpUpdateMsg build() {
            BgpHeader bgpMsgHeader = DEFAULT_UPDATE_HEADER;

            return new BgpUpdateMsgVer4(bgpMsgHeader, withdrawnRoutes, bgpPathAttributes, nlri);
        }

        @Override
        public Builder setHeader(BgpHeader bgpMsgHeader) {
            this.bgpMsgHeader = bgpMsgHeader;
            return this;
        }

        @Override
        public Builder setBgpPathAttributes(List<BgpValueType> attributes) {
            this.bgpPathAttributes = new BgpPathAttributes(attributes);
            return this;
        }

    }

    public static final Writer WRITER = new Writer();

    /**
     * Writer class for writing BGP update message to channel buffer.
     */
    public static class Writer implements BgpMessageWriter<BgpUpdateMsgVer4> {

        @Override
        public void write(ChannelBuffer cb, BgpUpdateMsgVer4 message) throws BgpParseException {

            int startIndex = cb.writerIndex();
            short afi = 0;
            byte safi = 0;

            // write common header and get msg length index
            int msgLenIndex = message.bgpHeader.write(cb);

            if (msgLenIndex <= 0) {
                throw new BgpParseException("Unable to write message header.");
            }
            List<BgpValueType> pathAttr = message.bgpPathAttributes.pathAttributes();
            if (pathAttr != null) {
                Iterator<BgpValueType> listIterator = pathAttr.iterator();

                while (listIterator.hasNext()) {
                    BgpValueType attr = listIterator.next();
                    if (attr instanceof MpReachNlri) {
                        MpReachNlri mpReach = (MpReachNlri) attr;
                        afi = mpReach.afi();
                        safi = mpReach.safi();
                    } else if (attr instanceof MpUnReachNlri) {
                        MpUnReachNlri mpUnReach = (MpUnReachNlri) attr;
                        afi = mpUnReach.afi();
                        safi = mpUnReach.safi();
                    }
                }

                if ((afi == Constants.AFI_FLOWSPEC_VALUE)
                        || (afi == Constants.AFI_VALUE)) {
                    //unfeasible route length
                    cb.writeShort(0);
                }

                if ((afi == Constants.AFI_EVPN_VALUE)
                        && (safi == Constants.SAFI_EVPN_VALUE)) {
                    cb.writeShort(0);
                }

            }

            if (message.bgpPathAttributes != null) {
                message.bgpPathAttributes.write(cb);
            }

            // write UPDATE Object Length
            int length = cb.writerIndex() - startIndex;
            cb.setShort(msgLenIndex, (short) length);
            message.bgpHeader.setLength((short) length);
        }
    }

    /**
     * Parses NLRI from channel buffer.
     *
     * @param cb channelBuffer
     * @return list of IP Prefix
     * @throws BgpParseException while parsing NLRI
     */
    public static LinkedList<IpPrefix> parseNlri(ChannelBuffer cb)
            throws BgpParseException {
        LinkedList<IpPrefix> nlri = new LinkedList<>();
        while (cb.readableBytes() > 0) {
            int length = cb.readByte();
            IpPrefix ipPrefix;
            if (length == 0) {
                byte[] prefix = new byte[] {0};
                ipPrefix = Validation.bytesToPrefix(prefix, length);
                nlri.add(ipPrefix);
            } else {
                int len = length / BYTE_IN_BITS;
                int reminder = length % BYTE_IN_BITS;
                if (reminder > 0) {
                    len = len + 1;
                }
                if (cb.readableBytes() < len) {
                    Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                            BgpErrorType.MALFORMED_ATTRIBUTE_LIST,
                            cb.readableBytes());
                }
                byte[] prefix = new byte[len];
                cb.readBytes(prefix, 0, len);
                ipPrefix = Validation.bytesToPrefix(prefix, length);
                nlri.add(ipPrefix);
            }
        }
        return nlri;
    }

    /**
     * Parsing withdrawn routes from channel buffer.
     *
     * @param cb channelBuffer
     * @return list of IP prefix
     * @throws BgpParseException while parsing withdrawn routes
     */
    public static LinkedList<IpPrefix> parseWithdrawnRoutes(ChannelBuffer cb)
            throws BgpParseException {
        LinkedList<IpPrefix> withDrwRoutes = new LinkedList<>();
        while (cb.readableBytes() > 0) {
            int length = cb.readByte();
            IpPrefix ipPrefix;
            if (length == 0) {
                byte[] prefix = new byte[] {0};
                ipPrefix = Validation.bytesToPrefix(prefix, length);
                withDrwRoutes.add(ipPrefix);
            } else {
                int len = length / BYTE_IN_BITS;
                int reminder = length % BYTE_IN_BITS;
                if (reminder > 0) {
                    len = len + 1;
                }
                if (cb.readableBytes() < len) {
                    Validation
                            .validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                         BgpErrorType.MALFORMED_ATTRIBUTE_LIST,
                                         cb.readableBytes());
                }
                byte[] prefix = new byte[len];
                cb.readBytes(prefix, 0, len);
                ipPrefix = Validation.bytesToPrefix(prefix, length);
                withDrwRoutes.add(ipPrefix);
            }
        }
        return withDrwRoutes;
    }

    @Override
    public BgpVersion getVersion() {
        return BgpVersion.BGP_4;
    }

    @Override
    public BgpType getType() {
        return BgpType.UPDATE;
    }

    @Override
    public void writeTo(ChannelBuffer channelBuffer) {
        try {
            WRITER.write(channelBuffer, this);
        } catch (BgpParseException e) {
            log.debug("[writeTo] Error: " + e.toString());
        }
    }

    @Override
    public BgpPathAttributes bgpPathAttributes() {
        return this.bgpPathAttributes;
    }

    @Override
    public List<IpPrefix> withdrawnRoutes() {
        return withdrawnRoutes;
    }

    @Override
    public List<IpPrefix> nlri() {
        return nlri;
    }

    @Override
    public BgpHeader getHeader() {
        return this.bgpHeader;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("bgpHeader", bgpHeader)
                .add("withDrawnRoutes", withdrawnRoutes)
                .add("nlri", nlri)
                .add("bgpPathAttributes", bgpPathAttributes)
                .toString();
    }
}
