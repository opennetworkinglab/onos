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
 * Representation of an OSPF Database Description packet.
 * Database Description packets are OSPF packet type 2.
 * These packets are exchanged when an adjacency is being initialized.
 * They describe the contents of the link-state database.
 */
public class DdPacket extends OspfPacketHeader {

    /*
        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |   Version #   |       2       |         Packet length         |
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
       |         Interface MTU         |    Options    |0|0|0|0|0|I|M|MS
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                     DD sequence number                        |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                                                               |
       +-                                                             -+
       |                                                               |
       +-                      An LSA Header                          -+
       |                                                               |
       +-                                                             -+
       |                                                               |
       +-                                                             -+
       |                                                               |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                              ...                              |
     */
    private static final Logger log = LoggerFactory.getLogger(DdPacket.class);
    private int imtu;
    private int options;
    private int ims; // initialize , more but / master slave bit
    private int isMaster;
    private int isInitialize;
    private int isMore;
    private long sequenceNo;
    private boolean isOpaqueCapable;
    private List<LsaHeader> lsaHeaderList = new ArrayList<>();

    /**
     * Creates an instance of DD packet.
     */
    public DdPacket() {
    }

    /**
     * Creates an instance of DD packet.
     *
     * @param ospfHeader OSPF header instance
     */
    public DdPacket(OspfPacketHeader ospfHeader) {
        populateHeader(ospfHeader);
    }

    /**
     * Gets is opaque capable or not.
     *
     * @return true if opaque capable else false
     */
    public boolean isOpaqueCapable() {
        return isOpaqueCapable;
    }

    /**
     * Sets is opaque capable or not.
     *
     * @param isOpaqueCapable true or false
     */
    public void setIsOpaqueCapable(boolean isOpaqueCapable) {
        this.isOpaqueCapable = isOpaqueCapable;
    }

    /**
     * Gets IMS value.
     *
     * @return IMS bits as an int value
     */
    public int ims() {
        return ims;
    }

    /**
     * Sets IMS value.
     *
     * @param ims IMS value
     */
    public void setIms(int ims) {
        this.ims = ims;
    }

    /**
     * Gets master bit value.
     *
     * @return 1 if master else 0
     */
    public int isMaster() {
        return isMaster;
    }

    /**
     * Sets master value.
     *
     * @param isMaster 1 represents master
     */
    public void setIsMaster(int isMaster) {
        this.isMaster = isMaster;
    }

    /**
     * Gets Initialize bit value.
     *
     * @return 1 if initialize else 0
     */
    public int isInitialize() {
        return isInitialize;
    }

    /**
     * Sets initialize value.
     *
     * @param isInitialize 1 is initialize else 0
     */
    public void setIsInitialize(int isInitialize) {
        this.isInitialize = isInitialize;
    }

    /**
     * Gets is more bit set or not.
     *
     * @return 1 if more set else 0
     */
    public int isMore() {
        return isMore;
    }

    /**
     * Sets more bit value to 0 or 1.
     *
     * @param isMore 1 if more set else 0
     */
    public void setIsMore(int isMore) {
        this.isMore = isMore;
    }


    /**
     * Gets IMTU value.
     *
     * @return IMTU value
     */
    public int imtu() {
        return imtu;
    }

    /**
     * Sets IMTU value.
     *
     * @param imtu value
     */
    public void setImtu(int imtu) {
        this.imtu = imtu;
    }

    /**
     * Gets options value.
     *
     * @return options
     */
    public int options() {
        return options;
    }

    /**
     * Sets options value.
     *
     * @param options options value
     */
    public void setOptions(int options) {
        this.options = options;
    }

    /**
     * Gets sequence number.
     *
     * @return sequenceNo
     */
    public long sequenceNo() {
        return sequenceNo;
    }

    /**
     * Sets Sequence number.
     *
     * @param sequenceNo sequence number
     */
    public void setSequenceNo(long sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    /**
     * Gets LSA header list.
     *
     * @return LSA header
     */
    public List<LsaHeader> getLsaHeaderList() {
        return lsaHeaderList;
    }

    /**
     * Adds LSA header to header list.
     *
     * @param lsaHeader lsa header instance
     */
    public void addLsaHeader(LsaHeader lsaHeader) {

        if (!lsaHeaderList.contains(lsaHeader)) {
            lsaHeaderList.add(lsaHeader);
        }
    }

    @Override
    public OspfPacketType ospfMessageType() {
        return OspfPacketType.DD;
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) throws OspfParseException {

        try {
            this.setImtu(channelBuffer.readShort());

            int options = channelBuffer.readByte();
            String obit = Integer.toHexString(options);
            if (obit.length() == 1) {
                obit = "0" + obit;
            }
            String toBinary = Integer.toBinaryString(Integer.parseInt(new Character(obit.charAt(0)).toString()));
            if (toBinary.length() == 1) {
                toBinary = "000" + toBinary;
            } else if (toBinary.length() == 2) {
                toBinary = "00" + toBinary;
            } else if (toBinary.length() == 3) {
                toBinary = "0" + toBinary;
            }
            if (Integer.parseInt(new Character(toBinary.charAt(1)).toString()) == 1) {
                this.setIsOpaqueCapable(true);
            }
            this.setOptions(options);
            this.setIms(channelBuffer.readByte());
            //Convert the byte to ims bits
            String strIms = Integer.toBinaryString(this.ims());
            if (strIms.length() == 3) {
                this.setIsInitialize(Integer.parseInt(Character.toString(strIms.charAt(0))));
                this.setIsMore(Integer.parseInt(Character.toString(strIms.charAt(1))));
                this.setIsMaster(Integer.parseInt(Character.toString(strIms.charAt(2))));
            } else if (strIms.length() == 2) {
                this.setIsInitialize(0);
                this.setIsMore(Integer.parseInt(Character.toString(strIms.charAt(0))));
                this.setIsMaster(Integer.parseInt(Character.toString(strIms.charAt(1))));
            } else if (strIms.length() == 1) {
                this.setIsInitialize(0);
                this.setIsMore(0);
                this.setIsMaster(Integer.parseInt(Character.toString(strIms.charAt(0))));
            }
            this.setSequenceNo(channelBuffer.readInt());

            //add all the LSA Headers - header is of 20 bytes
            while (channelBuffer.readableBytes() >= OspfUtil.LSA_HEADER_LENGTH) {
                LsaHeader header = OspfUtil.readLsaHeader(channelBuffer.readBytes(OspfUtil.LSA_HEADER_LENGTH));
                //add the LSAHeader to DDPacket
                addLsaHeader(header);
            }

        } catch (Exception e) {
            log.debug("Error::DdPacket:: {}", e.getMessage());
            throw new OspfParseException(OspfErrorType.MESSAGE_HEADER_ERROR, OspfErrorType.BAD_MESSAGE_LENGTH);
        }
    }

    @Override
    public byte[] asBytes() {

        byte[] ddMessage = null;

        byte[] ddHeader = getDdHeaderAsByteArray();
        byte[] ddBody = getDdBodyAsByteArray();
        ddMessage = Bytes.concat(ddHeader, ddBody);

        return ddMessage;
    }

    /**
     * Gets DD Header as byte array.
     *
     * @return dd header as byte array.
     */
    public byte[] getDdHeaderAsByteArray() {
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
            log.debug("Error");

        }

        return Bytes.toArray(headerLst);
    }


    /**
     * Gets DD body as byte array.
     *
     * @return DD body
     */
    public byte[] getDdBodyAsByteArray() {
        List<Byte> bodyLst = new ArrayList<>();

        try {
            bodyLst.addAll(Bytes.asList(OspfUtil.convertToTwoBytes(this.imtu())));
            bodyLst.add((byte) this.options());

            StringBuilder sb = new StringBuilder();
            sb.append(this.isInitialize());
            sb.append(this.isMore());
            sb.append(this.isMaster());

            bodyLst.add((byte) Integer.parseInt(sb.toString(), 2));
            bodyLst.addAll(Bytes.asList(OspfUtil.convertToFourBytes(this.sequenceNo()))); // passing long value

            for (LsaHeader lsaHeader : lsaHeaderList) {
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
            log.debug("Error::getLsrBodyAsByteArray {}", e.getMessage());
            return Bytes.toArray(bodyLst);
        }

        return Bytes.toArray(bodyLst);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("imtu", imtu)
                .add("options", options)
                .add("ims", ims)
                .add("isMaster", isMaster)
                .add("isInitialize", isInitialize)
                .add("isMore", isMore)
                .add("sequenceNo", sequenceNo)
                .add("isOpaqueCapable", isOpaqueCapable)
                .add("lsaHeaderList", lsaHeaderList)
                .toString();
    }
}