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
import org.onosproject.ospf.controller.OspfLsa;
import org.onosproject.ospf.exceptions.OspfErrorType;
import org.onosproject.ospf.exceptions.OspfParseException;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.OpaqueLsaHeader;
import org.onosproject.ospf.protocol.lsa.types.AsbrSummaryLsa;
import org.onosproject.ospf.protocol.lsa.types.ExternalLsa;
import org.onosproject.ospf.protocol.lsa.types.NetworkLsa;
import org.onosproject.ospf.protocol.lsa.types.OpaqueLsa10;
import org.onosproject.ospf.protocol.lsa.types.OpaqueLsa11;
import org.onosproject.ospf.protocol.lsa.types.OpaqueLsa9;
import org.onosproject.ospf.protocol.lsa.types.RouterLsa;
import org.onosproject.ospf.protocol.lsa.types.SummaryLsa;
import org.onosproject.ospf.protocol.ospfpacket.OspfPacketHeader;
import org.onosproject.ospf.controller.OspfPacketType;
import org.onosproject.ospf.protocol.util.OspfParameters;
import org.onosproject.ospf.protocol.util.OspfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Representation of an OSPF Link State Update packet.
 * Link State Update packets are OSPF packet type 4.  These packets
 * implement the flooding of LSAs.  Each Link State Update packet
 * carries a collection of LSAs one hop further from their origin.
 * Several LSAs may be included in a single packet.
 */
public class LsUpdate extends OspfPacketHeader {
    /*
        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |   Version #   |       4       |         Packet length         |
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
       |                            # LSAs                             |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                                                               |
       +-                                                            +-+
       |                             LSAs                              |
       +-                                                            +-+
       |                              ...                              |
     */
    private static final Logger log = LoggerFactory.getLogger(LsUpdate.class);
    private int numberOfLsa;
    private List<OspfLsa> lsaList = new LinkedList<>();

    /**
     * Creates an instance of Link State Update packet.
     */
    public LsUpdate() {
    }

    /**
     * Creates an instance of Link State Update packet.
     *
     * @param ospfHeader ospf header instance.
     */
    public LsUpdate(OspfPacketHeader ospfHeader) {
        populateHeader(ospfHeader);
    }

    /**
     * Gets the LSA list.
     *
     * @return list of LSA
     */
    public List getLsaList() {
        return lsaList;
    }

    /**
     * Adds the LSA to list.
     *
     * @param lsa LSA
     */
    public void addLsa(OspfLsa lsa) {
        if (!lsaList.contains(lsa)) {
            lsaList.add(lsa);
        }
    }

    /**
     * Gets the number of LSA.
     *
     * @return number of LSA
     */
    public int noLsa() {
        return numberOfLsa;
    }

    /**
     * Sets number of LSA.
     *
     * @param numberOfLsa number of LSA
     */
    public void setNumberOfLsa(int numberOfLsa) {
        this.numberOfLsa = numberOfLsa;
    }


    @Override
    public OspfPacketType ospfMessageType() {
        return OspfPacketType.LSUPDATE;
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) throws OspfParseException {
        try {
            //From header 4 bytes is number of lsa's
            this.setNumberOfLsa(channelBuffer.readInt());
            //get the remaining bytes represents Number of LSA's present. Add all the LSA's
            while (channelBuffer.readableBytes() > OspfUtil.LSA_HEADER_LENGTH) {

                LsaHeader header = OspfUtil.readLsaHeader(channelBuffer.readBytes(OspfUtil.LSA_HEADER_LENGTH));
                int lsaLength = header.lsPacketLen();
                int lsType = header.lsType();

                switch (lsType) {
                    case OspfParameters.LINK_LOCAL_OPAQUE_LSA:
                        OpaqueLsa9 opaqueLsa9 = new OpaqueLsa9((OpaqueLsaHeader) header);
                        opaqueLsa9.readFrom(channelBuffer.readBytes(lsaLength - OspfUtil.LSA_HEADER_LENGTH));
                        addLsa(opaqueLsa9);
                        break;
                    case OspfParameters.AREA_LOCAL_OPAQUE_LSA:
                        OpaqueLsa10 opaqueLsa10 = new OpaqueLsa10((OpaqueLsaHeader) header);
                        opaqueLsa10.readFrom(channelBuffer.readBytes(lsaLength - OspfUtil.LSA_HEADER_LENGTH));
                        addLsa(opaqueLsa10);
                        break;
                    case OspfParameters.AS_OPAQUE_LSA:
                        OpaqueLsa11 opaqueLsa11 = new OpaqueLsa11((OpaqueLsaHeader) header);
                        opaqueLsa11.readFrom(channelBuffer.readBytes(lsaLength - OspfUtil.LSA_HEADER_LENGTH));
                        addLsa(opaqueLsa11);
                        break;
                    case OspfParameters.ROUTER:
                        RouterLsa routerLsa = new RouterLsa(header);
                        routerLsa.readFrom(channelBuffer.readBytes(lsaLength - OspfUtil.LSA_HEADER_LENGTH));
                        addLsa(routerLsa);
                        break;
                    case OspfParameters.NETWORK:
                        NetworkLsa networkLsa = new NetworkLsa(header);
                        networkLsa.readFrom(channelBuffer.readBytes(lsaLength - OspfUtil.LSA_HEADER_LENGTH));
                        addLsa(networkLsa);
                        break;
                    case OspfParameters.ASBR_SUMMARY:
                        AsbrSummaryLsa asbrSummaryLsa = new AsbrSummaryLsa(header);
                        asbrSummaryLsa.readFrom(channelBuffer.readBytes(lsaLength - OspfUtil.LSA_HEADER_LENGTH));
                        addLsa(asbrSummaryLsa);
                        break;
                    case OspfParameters.SUMMARY:
                        SummaryLsa summaryLsa = new SummaryLsa(header);
                        summaryLsa.readFrom(channelBuffer.readBytes(lsaLength - OspfUtil.LSA_HEADER_LENGTH));
                        addLsa(summaryLsa);
                        break;
                    case OspfParameters.EXTERNAL_LSA:
                        ExternalLsa externalLsa = new ExternalLsa(header);
                        externalLsa.readFrom(channelBuffer.readBytes(lsaLength - OspfUtil.LSA_HEADER_LENGTH));
                        addLsa(externalLsa);
                        break;
                    default:
                        log.debug("LSUpdate::readLsUpdateBody::UnKnown LS Type: {}", lsType);
                        break;
                }
            }
        } catch (Exception e) {
            log.debug("Error::LsUpdate:: {}", e.getMessage());
            throw new OspfParseException(OspfErrorType.MESSAGE_HEADER_ERROR, OspfErrorType.BAD_MESSAGE_LENGTH);
        }
    }

    @Override
    public byte[] asBytes() {
        byte[] lsuMessage = null;

        byte[] ospfHeader = getLsuHeaderAsByteArray();
        byte[] lsuBody = getLsuBodyAsByteArray();
        lsuMessage = Bytes.concat(ospfHeader, lsuBody);

        return lsuMessage;
    }

    /**
     * Gets lsu header.
     *
     * @return lsu header as byte array
     */
    public byte[] getLsuHeaderAsByteArray() {
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
            log.debug("Error::LSUpdate::getLsuHeaderAsByteArray:: {}", e.getMessage());
            return Bytes.toArray(headerLst);
        }

        return Bytes.toArray(headerLst);
    }

    /**
     * Get lsu body as byte array.
     *
     * @return lsu body as byte array
     */
    public byte[] getLsuBodyAsByteArray() {
        List<Byte> bodyLst = new ArrayList<>();

        try {
            //add number of LSA's
            bodyLst.addAll(Bytes.asList(OspfUtil.convertToFourBytes(this.noLsa())));
            //for each type of LSA's from the list get lsa bytes
            for (OspfLsa ospfLsa : lsaList) {
                //Check the type of lsa and build bytes accordingly
                switch (ospfLsa.getOspfLsaType().value()) {
                    case OspfParameters.LINK_LOCAL_OPAQUE_LSA:
                        OpaqueLsa9 opaqueLsa9 = (OpaqueLsa9) ospfLsa;
                        bodyLst.addAll(Bytes.asList(opaqueLsa9.asBytes()));
                        break;
                    case OspfParameters.AREA_LOCAL_OPAQUE_LSA:
                        OpaqueLsa10 opaqueLsa10 = (OpaqueLsa10) ospfLsa;
                        bodyLst.addAll(Bytes.asList(opaqueLsa10.asBytes()));
                        break;
                    case OspfParameters.AS_OPAQUE_LSA:
                        OpaqueLsa11 opaqueLsa11 = (OpaqueLsa11) ospfLsa;
                        bodyLst.addAll(Bytes.asList(opaqueLsa11.asBytes()));
                        break;
                    case OspfParameters.ROUTER:
                        RouterLsa routerLsa = (RouterLsa) ospfLsa;
                        bodyLst.addAll(Bytes.asList(routerLsa.asBytes()));
                        break;
                    case OspfParameters.NETWORK:
                        NetworkLsa networkLsa = (NetworkLsa) ospfLsa;
                        bodyLst.addAll(Bytes.asList(networkLsa.asBytes()));
                        break;
                    case OspfParameters.ASBR_SUMMARY:
                        AsbrSummaryLsa asbrSummaryLsa = (AsbrSummaryLsa) ospfLsa;
                        bodyLst.addAll(Bytes.asList(asbrSummaryLsa.asBytes()));
                        break;
                    case OspfParameters.SUMMARY:
                        SummaryLsa summaryLsa = (SummaryLsa) ospfLsa;
                        bodyLst.addAll(Bytes.asList(summaryLsa.asBytes()));
                        break;
                    case OspfParameters.EXTERNAL_LSA:
                        ExternalLsa externalLsa = (ExternalLsa) ospfLsa;
                        bodyLst.addAll(Bytes.asList(externalLsa.asBytes()));
                        break;
                    default:
                        log.debug("LSUpdate::getLsuBodyAsByteArray::UnKnown ospfLsa: {}", ospfLsa);
                        break;
                }
            }

        } catch (Exception e) {
            log.debug("Error::getLsuBodyAsByteArray {}", e.getMessage());
            return Bytes.toArray(bodyLst);
        }

        return Bytes.toArray(bodyLst);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("noLsa", numberOfLsa)
                .add("lsaList", lsaList)
                .toString();
    }
}