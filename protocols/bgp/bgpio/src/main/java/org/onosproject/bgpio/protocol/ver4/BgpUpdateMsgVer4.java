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
package org.onosproject.bgpio.protocol.ver4;

import java.util.LinkedList;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.IpPrefix;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpMessageReader;
import org.onosproject.bgpio.protocol.BgpType;
import org.onosproject.bgpio.protocol.BgpUpdateMsg;
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.types.BgpHeader;
import org.onosproject.bgpio.util.Validation;
import org.onosproject.bgpio.protocol.BgpVersion;
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

    protected static final Logger log = LoggerFactory
            .getLogger(BgpUpdateMsgVer4.class);

    public static final byte PACKET_VERSION = 4;
    //Withdrawn Routes Length(2) + Total Path Attribute Length(2)
    public static final int PACKET_MINIMUM_LENGTH = 4;
    public static final int BYTE_IN_BITS = 8;
    public static final int MIN_LEN_AFTER_WITHDRW_ROUTES = 2;
    public static final int MINIMUM_COMMON_HEADER_LENGTH = 19;
    public static final BgpType MSG_TYPE = BgpType.UPDATE;
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
            // Reading Withdrawn Routes Length
            Short withDrwLen = cb.readShort();

            if (cb.readableBytes() < withDrwLen) {
                Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                        BgpErrorType.MALFORMED_ATTRIBUTE_LIST,
                        cb.readableBytes());
            }
            ChannelBuffer tempCb = cb.readBytes(withDrwLen);
            if (withDrwLen != 0) {
                // Parsing WithdrawnRoutes
                withDrwRoutes = parseWithdrawnRoutes(tempCb);
            }
            if (cb.readableBytes() < MIN_LEN_AFTER_WITHDRW_ROUTES) {
                log.debug("Bgp Path Attribute len field not present");
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
    public void writeTo(ChannelBuffer channelBuffer) throws BgpParseException {
        //Not to be implemented as of now
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
