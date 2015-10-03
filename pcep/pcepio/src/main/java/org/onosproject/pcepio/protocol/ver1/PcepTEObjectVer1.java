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

package org.onosproject.pcepio.protocol.ver1;

import java.util.LinkedList;
import java.util.ListIterator;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepTEObject;
import org.onosproject.pcepio.types.LocalTENodeDescriptorsTlv;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.onosproject.pcepio.types.PcepValueType;
import org.onosproject.pcepio.types.RemoteTENodeDescriptorsTlv;
import org.onosproject.pcepio.types.RoutingUniverseTlv;
import org.onosproject.pcepio.types.TELinkAttributesTlv;
import org.onosproject.pcepio.types.TELinkDescriptorsTlv;
import org.onosproject.pcepio.types.TENodeAttributesTlv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP TE Object.
 */
public class PcepTEObjectVer1 implements PcepTEObject {
    /*
     *
    reference: PCEP Extension for Transporting TE Data draft-dhodylee-pce-pcep-te-data-extn-02.
    TE Object-Class is [TBD6].

    Two Object-Type values are defined for the TE object:

    o  TE Node: TE Object-Type is 1.

    o  TE Link: TE Object-Type is 2.

    The format of the TE object body is as follows:

       0                   1                   2                   3
       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |  Protocol-ID  |          Flag                             |R|S|
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                          TE-ID                                |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      //                         TLVs                                //
      |                                                               |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    protected static final Logger log = LoggerFactory.getLogger(PcepTEObjectVer1.class);

    public static final byte TE_OBJ_TYPE_NODE_VALUE = 1;
    public static final byte TE_OBJ_TYPE_LINK_VALUE = 2;

    public static final byte TE_OBJ_CLASS = 101; //TBD6 in RFC.
    public static final byte TE_OBJECT_VERSION = 1;

    // TE_OBJ_MINIMUM_LENGTH = TEObjectHeaderLen(4)+ TEObjectLen(8)
    public static final short TE_OBJ_MINIMUM_LENGTH = 12;

    // Signaled ,all default values to be checked.
    public static final byte DEFAULT_PROTOCOL_ID = 1; //IS-IS Level 1
    public static final boolean DEFAULT_R_FLAG = false;
    public static final boolean DEFAULT_S_FLAG = false;
    public static final int DEFAULT_TE_ID = 0;

    public static final int OBJECT_HEADER_LENGTH = 4;
    public static final int RIGHT_SHIFT_ONE = 1;
    public static final int RIGHT_FIRST_FLAG = 0x1;
    public static final int FLAG_SET_R_FLAG = 0x2;
    public static final int FLAG_SET_S_FLAG = 0x1;
    public static final int MINIMUM_COMMON_HEADER_LENGTH = 4;
    public static final int MINIMUM_TLV_HEADER_LENGTH = 4;

    public static final PcepObjectHeader DEFAULT_TE_OBJECT_HEADER = new PcepObjectHeader(TE_OBJ_CLASS,
            TE_OBJ_TYPE_NODE_VALUE, PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED,
            TE_OBJ_MINIMUM_LENGTH);

    private PcepObjectHeader teObjHeader;
    private byte yProtocolId;
    // 2-flags
    private boolean bRFlag;
    private boolean bSFlag;
    private int iTEId;
    // Optional TLV
    private LinkedList<PcepValueType> llOptionalTlv;

    /**
     * Constructor to initialize variables.
     *
     * @param teObjHeader TE Object header
     * @param yProtocolId Protocol-ID
     * @param bRFlag R-flag
     * @param bSFlag S-flag
     * @param iTEId TE-ID
     * @param llOptionalTlv linked list of Optional TLV
     */
    public PcepTEObjectVer1(PcepObjectHeader teObjHeader, byte yProtocolId, boolean bRFlag, boolean bSFlag, int iTEId,
            LinkedList<PcepValueType> llOptionalTlv) {

        this.teObjHeader = teObjHeader;
        this.yProtocolId = yProtocolId;
        this.bRFlag = bRFlag;
        this.bSFlag = bSFlag;
        this.iTEId = iTEId;
        this.llOptionalTlv = llOptionalTlv;
    }

    @Override
    public PcepObjectHeader getTEObjHeader() {
        return this.teObjHeader;
    }

    @Override
    public void setTEObjHeader(PcepObjectHeader obj) {
        this.teObjHeader = obj;
    }

    @Override
    public byte getProtocolId() {
        return this.yProtocolId;
    }

    @Override
    public void setProtocolId(byte yProtId) {
        this.yProtocolId = yProtId;
    }

    @Override
    public boolean getRFlag() {
        return this.bRFlag;
    }

    @Override
    public void setRFlag(boolean bRFlag) {
        this.bRFlag = bRFlag;
    }

    @Override
    public boolean getSFlag() {
        return this.bSFlag;
    }

    @Override
    public void setSFlag(boolean bSFlag) {
        this.bSFlag = bSFlag;
    }

    @Override
    public int getTEId() {
        return this.iTEId;
    }

    @Override
    public void setTEId(int iTEId) {
        this.iTEId = iTEId;
    }

    @Override
    public LinkedList<PcepValueType> getOptionalTlv() {
        return this.llOptionalTlv;
    }

    @Override
    public void setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv) {
        this.llOptionalTlv = llOptionalTlv;
    }

    /**
     * Reads from the channel buffer and returns Object of PcepTEObject.
     *
     * @param cb of type channel buffer
     * @return Object of PcepTEObject
     * @throws PcepParseException if mandatory fields are missing
     */
    public static PcepTEObject read(ChannelBuffer cb) throws PcepParseException {
        log.debug("read");

        PcepObjectHeader teObjHeader;
        byte yProtocolId;
        // 2-flags
        boolean bRFlag;
        boolean bSFlag;
        int iTEId;
        LinkedList<PcepValueType> llOptionalTlv;

        teObjHeader = PcepObjectHeader.read(cb);

        //take only TEObject buffer.
        ChannelBuffer tempCb = cb.readBytes(teObjHeader.getObjLen() - OBJECT_HEADER_LENGTH);

        yProtocolId = tempCb.readByte();
        //ignore first two bytes of Flags
        tempCb.readShort();

        Integer iTemp = (int) tempCb.readByte(); //read 3rd byte Flag
        bSFlag = (iTemp & FLAG_SET_S_FLAG) == FLAG_SET_S_FLAG;
        bRFlag = (iTemp & FLAG_SET_R_FLAG) == FLAG_SET_R_FLAG;

        iTEId = tempCb.readInt();

        // parse optional TLV
        llOptionalTlv = parseOptionalTlv(tempCb);

        return new PcepTEObjectVer1(teObjHeader, yProtocolId, bRFlag, bSFlag, iTEId, llOptionalTlv);
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {

        //write Object header
        int objStartIndex = cb.writerIndex();
        int objLenIndex = teObjHeader.write(cb);

        if (objLenIndex <= 0) {
            throw new PcepParseException("ObjectLength Index is " + objLenIndex);
        }

        //write Protocol ID
        cb.writeByte(this.yProtocolId);

        //write Flag
        cb.writeShort(0);

        byte bTemp = 0;
        if (bSFlag) {
            bTemp = FLAG_SET_S_FLAG;
        }

        if (bRFlag) {
            bTemp = (byte) (bTemp | FLAG_SET_R_FLAG);
        }
        cb.writeByte(bTemp);

        //write TEId
        cb.writeInt(iTEId);

        // Add optional TLV
        packOptionalTlv(cb);

        //Update object length now
        int length = cb.writerIndex() - objStartIndex;

        //will be helpful during print().
        teObjHeader.setObjLen((short) length);

        cb.setShort(objLenIndex, (short) length);

        return cb.writerIndex();
    }

    /**
     * Returns Linked list of PCEP Value Type.
     *
     * @param cb of channel buffer
     * @return Linked list of PCEP Value Type
     * @throws PcepParseException if mandatory fields are missing
     */
    protected static LinkedList<PcepValueType> parseOptionalTlv(ChannelBuffer cb) throws PcepParseException {

        LinkedList<PcepValueType> llOutOptionalTlv;

        llOutOptionalTlv = new LinkedList<>();

        while (MINIMUM_TLV_HEADER_LENGTH <= cb.readableBytes()) {

            PcepValueType tlv;
            short hType = cb.readShort();
            short hLength = cb.readShort();
            long lValue = 0;

            switch (hType) {

            case RoutingUniverseTlv.TYPE:
                lValue = cb.readLong();
                tlv = new RoutingUniverseTlv(lValue);
                break;
            case LocalTENodeDescriptorsTlv.TYPE:
                tlv = LocalTENodeDescriptorsTlv.read(cb, hLength);
                break;
            case RemoteTENodeDescriptorsTlv.TYPE:
                tlv = RemoteTENodeDescriptorsTlv.read(cb, hLength);
                break;
            case TELinkDescriptorsTlv.TYPE:
                tlv = TELinkDescriptorsTlv.read(cb, hLength);
                break;
            case TENodeAttributesTlv.TYPE:
                tlv = TENodeAttributesTlv.read(cb, hLength);
                break;
            case TELinkAttributesTlv.TYPE:
                tlv = TELinkAttributesTlv.read(cb, hLength);
                break;
            default:
                throw new PcepParseException("Unsupported TLV type :" + hType);
            }

            // Check for the padding
            int pad = hLength % 4;
            if (0 < pad) {
                pad = 4 - pad;
                if (pad <= cb.readableBytes()) {
                    cb.skipBytes(pad);
                }
            }

            llOutOptionalTlv.add(tlv);
        }

        if (0 < cb.readableBytes()) {

            throw new PcepParseException("Optional Tlv parsing error. Extra bytes received.");
        }
        return llOutOptionalTlv;
    }

    /**
     * Returns the writer index.
     *
     * @param cb of type channel buffer
     * @return the writer index.
     */
    protected int packOptionalTlv(ChannelBuffer cb) {

        ListIterator<PcepValueType> listIterator = llOptionalTlv.listIterator();

        while (listIterator.hasNext()) {
            PcepValueType tlv = listIterator.next();

            if (tlv == null) {
                log.debug("TLV is null from OptionalTlv list");
                continue;
            }
            tlv.write(cb);

            // need to take care of padding
            int pad = tlv.getLength() % 4;

            if (0 != pad) {
                pad = 4 - pad;
                for (int i = 0; i < pad; ++i) {
                    cb.writeByte((byte) 0);
                }
            }
        }
        return cb.writerIndex();
    }

    /**
     * Builder class for PCEP te object.
     */
    public static class Builder implements PcepTEObject.Builder {
        private boolean bIsHeaderSet = false;
        private boolean bIsProtocolIdSet = false;
        private boolean bIsRFlagSet = false;
        private boolean bIsSFlagSet = false;
        private boolean bIsTEIdSet = false;

        private PcepObjectHeader teObjHeader;
        private byte yProtocolId;
        private boolean bRFlag;
        private boolean bSFlag;
        private int iTEId;
        private LinkedList<PcepValueType> llOptionalTlv = new LinkedList<>();

        private boolean bIsPFlagSet = false;
        private boolean bPFlag;

        private boolean bIsIFlagSet = false;
        private boolean bIFlag;

        @Override
        public PcepTEObject build() {
            PcepObjectHeader teObjHeader = this.bIsHeaderSet ? this.teObjHeader : DEFAULT_TE_OBJECT_HEADER;

            byte yProtocolId = this.bIsProtocolIdSet ? this.yProtocolId : DEFAULT_PROTOCOL_ID;
            boolean bRFlag = this.bIsRFlagSet ? this.bRFlag : DEFAULT_R_FLAG;
            boolean bSFlag = this.bIsSFlagSet ? this.bSFlag : DEFAULT_S_FLAG;
            int iTEId = this.bIsTEIdSet ? this.iTEId : DEFAULT_TE_ID;

            if (bIsPFlagSet) {
                teObjHeader.setPFlag(bPFlag);
            }

            if (bIsIFlagSet) {
                teObjHeader.setIFlag(bIFlag);
            }

            return new PcepTEObjectVer1(teObjHeader, yProtocolId, bRFlag, bSFlag, iTEId, llOptionalTlv);

        }

        @Override
        public PcepObjectHeader getTEObjHeader() {
            return this.teObjHeader;
        }

        @Override
        public Builder setTEObjHeader(PcepObjectHeader obj) {
            this.teObjHeader = obj;
            this.bIsHeaderSet = true;
            return this;
        }

        @Override
        public byte getProtocolId() {
            return this.yProtocolId;
        }

        @Override
        public Builder setProtocolId(byte yProtId) {
            this.yProtocolId = yProtId;
            this.bIsProtocolIdSet = true;
            return this;
        }

        @Override
        public boolean getRFlag() {
            return this.bRFlag;
        }

        @Override
        public Builder setRFlag(boolean bRFlag) {
            this.bRFlag = bRFlag;
            this.bIsRFlagSet = true;
            return this;
        }

        @Override
        public boolean getSFlag() {
            return this.bSFlag;
        }

        @Override
        public Builder setSFlag(boolean bSFlag) {
            this.bSFlag = bSFlag;
            this.bIsSFlagSet = true;
            return this;
        }

        @Override
        public int getTEId() {
            return this.iTEId;
        }

        @Override
        public Builder setTEId(int iTEId) {
            this.iTEId = iTEId;
            this.bIsTEIdSet = true;
            return this;
        }

        @Override
        public LinkedList<PcepValueType> getOptionalTlv() {
            return this.llOptionalTlv;
        }

        @Override
        public Builder setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv) {
            this.llOptionalTlv = llOptionalTlv;
            return this;
        }

        @Override
        public Builder setPFlag(boolean value) {
            this.bPFlag = value;
            this.bIsPFlagSet = true;
            return this;
        }

        @Override
        public Builder setIFlag(boolean value) {
            this.bIFlag = value;
            this.bIsIFlagSet = true;
            return this;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("ObjectHeader", teObjHeader)
                .add("ProtocolId", yProtocolId)
                .add("RFlag", (bRFlag) ? 1 : 0)
                .add("SFlag", (bSFlag) ? 1 : 0)
                .add("TeId", iTEId)
                .add("OptionalTlv", llOptionalTlv)
                .toString();
    }
}
