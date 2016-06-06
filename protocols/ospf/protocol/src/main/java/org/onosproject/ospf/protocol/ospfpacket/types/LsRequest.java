/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.ospf.protocol.ospfpacket.types;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.Bytes;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.exceptions.OspfParseException;
import org.onosproject.ospf.protocol.ospfpacket.OspfPacketHeader;
import org.onosproject.ospf.protocol.ospfpacket.subtype.LsRequestPacket;
import org.onosproject.ospf.controller.OspfPacketType;
import org.onosproject.ospf.protocol.util.OspfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Representation of an OSPF Link State Request packet.
 * Link State Request packets are OSPF packet type 3.  After exchanging
 * database description packets with a neighboring router, a router may
 * find that parts of its link-state database are out-of-date.  The
 * Link State Request packet is used to request the pieces of the
 * neighbor's database that are more up-to-date.
 */
public class LsRequest extends OspfPacketHeader {
    /*
        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |   Version #   |       3       |         Packet length         |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                          Router ID                            |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                           Area ID                             |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |           Checksum            |             AuType            |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                       Authentication                          |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                       Authentication                          |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                          LS type                              |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                       Link State ID                           |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                     Advertising Router                        |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                              ...                              |

       LsRequest Message Format
       REFERENCE : RFC 2328
     */
    private static final Logger log = LoggerFactory.getLogger(LsRequest.class);
    private List<LsRequestPacket> linkStateRequests = new ArrayList<>();

    /**
     * Creates an instance of link state request packet.
     */
    public LsRequest() {
    }

    /**
     * Creates an instance of link state request packet.
     *
     * @param ospfHeader OSPF header instance.
     */
    public LsRequest(OspfPacketHeader ospfHeader) {
        populateHeader(ospfHeader);
    }

    /**
     * Adds link state request.
     *
     * @param lsRequestPacket ls request packet instance
     */
    public void addLinkStateRequests(LsRequestPacket lsRequestPacket) {
        if (!linkStateRequests.contains(lsRequestPacket)) {
            linkStateRequests.add(lsRequestPacket);
        }
    }

    /**
     * Gets link state request packet instance.
     *
     * @return link state request packet instance
     */
    public List<LsRequestPacket> getLinkStateRequests() {
        return linkStateRequests;
    }

    @Override
    public OspfPacketType ospfMessageType() {
        return OspfPacketType.LSREQUEST;
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) throws OspfParseException {

        while (channelBuffer.readableBytes() >= OspfUtil.LSREQUEST_LENGTH) {
            LsRequestPacket lsRequestPacket = new LsRequestPacket();
            lsRequestPacket.setLsType(channelBuffer.readInt());
            byte[] tempByteArray = new byte[OspfUtil.FOUR_BYTES];
            channelBuffer.readBytes(tempByteArray, 0, OspfUtil.FOUR_BYTES);
            lsRequestPacket.setLinkStateId(Ip4Address.valueOf(tempByteArray).toString());
            tempByteArray = new byte[OspfUtil.FOUR_BYTES];
            channelBuffer.readBytes(tempByteArray, 0, OspfUtil.FOUR_BYTES);
            lsRequestPacket.setOwnRouterId(Ip4Address.valueOf(tempByteArray).toString());

            this.addLinkStateRequests(lsRequestPacket);
        }
    }

    @Override
    public byte[] asBytes() {
        byte[] lsrMessage = null;
        byte[] lsrHeader = getLsrHeaderAsByteArray();
        byte[] lsrBody = getLsrBodyAsByteArray();
        lsrMessage = Bytes.concat(lsrHeader, lsrBody);

        return lsrMessage;
    }

    /**
     * Gets LS request packet header as byte array.
     *
     * @return LS request packet header as byte array
     */
    public byte[] getLsrHeaderAsByteArray() {
        List<Byte> headerLst = new ArrayList<>();

        try {
            headerLst.add((byte) this.ospfVersion());
            headerLst.add((byte) this.ospfType());
            headerLst.addAll(Bytes.asList(OspfUtil.convertToTwoBytes(this.ospfPacLength())));
            headerLst.addAll(Bytes.asList(this.routerId().toOctets()));
            headerLst.addAll(Bytes.asList(this.areaId().toOctets()));
            headerLst.addAll(Bytes.asList(OspfUtil.convertToTwoBytes(this.checksum())));
            headerLst.addAll(Bytes.asList(OspfUtil.convertToTwoBytes(this.authType())));
            //Authentication is 0 always. Total 8 bytes consist of zero
            byte[] auth = new byte[OspfUtil.EIGHT_BYTES];
            headerLst.addAll(Bytes.asList(auth));
        } catch (Exception e) {
            log.debug("Error::getLsrBodyAsByteArray {}", e.getMessage());
            return Bytes.toArray(headerLst);
        }

        return Bytes.toArray(headerLst);
    }

    /**
     * Gets LS request packet body as byte array.
     *
     * @return LS request packet body as byte array
     */
    public byte[] getLsrBodyAsByteArray() {
        List<Byte> bodyLst = new ArrayList<>();

        try {
            for (LsRequestPacket lsrPacket : linkStateRequests) {
                bodyLst.addAll(Bytes.asList(OspfUtil.convertToFourBytes(lsrPacket.lsType())));
                bodyLst.addAll(Bytes.asList(InetAddress.getByName(lsrPacket.linkStateId()).getAddress()));
                bodyLst.addAll(Bytes.asList(InetAddress.getByName(lsrPacket.ownRouterId()).getAddress()));
            }
        } catch (Exception e) {
            log.debug("Error::getLsrBodyAsByteArray {}", e.getMessage());
            return Bytes.toArray(bodyLst);
        }

        return Bytes.toArray(bodyLst);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("linkStateRequests", linkStateRequests)
                .toString();
    }
}