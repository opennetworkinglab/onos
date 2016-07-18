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
import org.onosproject.ospf.exceptions.OspfErrorType;
import org.onosproject.ospf.exceptions.OspfParseException;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.OpaqueLsaHeader;
import org.onosproject.ospf.protocol.ospfpacket.OspfPacketHeader;
import org.onosproject.ospf.controller.OspfPacketType;
import org.onosproject.ospf.protocol.util.OspfParameters;
import org.onosproject.ospf.protocol.util.OspfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of an  OSPF Link State Acknowledgment Message.
 * Link State Acknowledgment Packets are OSPF packet type 5.
 * To make the flooding of LSAs reliable, flooded LSAs are explicitly
 * acknowledged. This acknowledgment is accomplished through the
 * sending and receiving of Link State Acknowledgment packets.
 * Multiple LSAs can be acknowledged in a single Link State Acknowledgment packet.
 */
public class LsAcknowledge extends OspfPacketHeader {
    /*
        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |   Version #   |       5       |         Packet length         |
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
       |                                                               |
       +-                                                             -+
       |                                                               |
       +-                         An LSA Header                       -+
       |                                                               |
       +-                                                             -+
       |                                                               |
       +-                                                             -+
       |                                                               |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                              ...                              |
 */
    private static final Logger log = LoggerFactory.getLogger(LsAcknowledge.class);
    private List<LsaHeader> linkStateHeaders = new ArrayList<>();

    /**
     * Creates an instance of Link State Acknowledgment instance.
     */
    public LsAcknowledge() {
    }

    /**
     * Creates an instance of Link State Acknowledgment instance.
     *
     * @param ospfHeader OSPF header instance.
     */
    public LsAcknowledge(OspfPacketHeader ospfHeader) {
        populateHeader(ospfHeader);
    }

    /**
     * Gets ls headers.
     *
     * @return ls headers
     */
    public List<LsaHeader> getLinkStateHeaders() {
        return linkStateHeaders;
    }

    /**
     * Adds link state header to list.
     *
     * @param lsaHeader LSA header
     */
    public void addLinkStateHeader(LsaHeader lsaHeader) {
        if (!linkStateHeaders.contains(lsaHeader)) {
            linkStateHeaders.add(lsaHeader);
        }
    }

    @Override
    public OspfPacketType ospfMessageType() {
        return OspfPacketType.LSAACK;
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) throws OspfParseException {
        try {
            //add all the LSA Headers - one header is of 20 bytes
            while (channelBuffer.readableBytes() >= OspfUtil.LSA_HEADER_LENGTH) {
                LsaHeader header = OspfUtil.readLsaHeader(channelBuffer);
                //add the LSAHeader to acknowledge
                addLinkStateHeader(header);
            }

        } catch (Exception e) {
            log.debug("Error::LsAckPacket:: {}", e.getMessage());
            throw new OspfParseException(OspfErrorType.MESSAGE_HEADER_ERROR, OspfErrorType.BAD_MESSAGE_LENGTH);
        }
    }

    @Override
    public byte[] asBytes() {
        byte[] lsAckMessage = null;

        byte[] lsAckHeader = getLsAckAsByteArray();
        byte[] lsAckBody = getLsAckBodyAsByteArray();
        lsAckMessage = Bytes.concat(lsAckHeader, lsAckBody);

        return lsAckMessage;
    }

    /**
     * Gets LSAcknowledge as byte array.
     *
     * @return byte array
     */
    public byte[] getLsAckAsByteArray() {
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
            log.debug("Error::LsAckPacket:: {}", e.getMessage());
            return Bytes.toArray(headerLst);
        }

        return Bytes.toArray(headerLst);
    }

    /**
     * Gets LsAck body as byte array.
     *
     * @return byte array
     */
    public byte[] getLsAckBodyAsByteArray() {
        List<Byte> bodyLst = new ArrayList<>();

        try {
            for (LsaHeader lsaHeader : linkStateHeaders) {
                if (lsaHeader.lsType() == OspfParameters.LINK_LOCAL_OPAQUE_LSA ||
                        lsaHeader.lsType() == OspfParameters.AREA_LOCAL_OPAQUE_LSA ||
                        lsaHeader.lsType() == OspfParameters.AS_OPAQUE_LSA) {
                    OpaqueLsaHeader header = (OpaqueLsaHeader) lsaHeader;
                    bodyLst.addAll(Bytes.asList(header.getOpaqueLsaHeaderAsByteArray()));
                } else {
                    bodyLst.addAll(Bytes.asList(lsaHeader.getLsaHeaderAsByteArray()));
                }
            }
        } catch (Exception e) {
            log.debug("Error::getLsAckBodyAsByteArray {}", e.getMessage());
            return Bytes.toArray(bodyLst);
        }

        return Bytes.toArray(bodyLst);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("linkStateHeaders", linkStateHeaders)
                .toString();
    }
}