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

package org.onosproject.pcepio.protocol.ver1;

import java.util.LinkedList;
import java.util.ListIterator;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepLspObject;
import org.onosproject.pcepio.types.PcepErrorDetailInfo;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.onosproject.pcepio.types.PcepValueType;
import org.onosproject.pcepio.types.StatefulIPv4LspIdentifiersTlv;
import org.onosproject.pcepio.types.StatefulLspDbVerTlv;
import org.onosproject.pcepio.types.StatefulLspErrorCodeTlv;
import org.onosproject.pcepio.types.StatefulRsvpErrorSpecTlv;
import org.onosproject.pcepio.types.SymbolicPathNameTlv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP lsp object.
 */
public class PcepLspObjectVer1 implements PcepLspObject {

    /*
     message format.
     Reference : draft-ietf-pce-stateful-pce-11, section 7.3.
      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     | Object-Class  |   OT  |Res|P|I|   Object Length (bytes)       |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                PLSP-ID                |  Flag |C|    O|A|R|S|D|
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     //                        TLVs                                 //
     |                                                               |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                     The LSP Object format
     */
    protected static final Logger log = LoggerFactory.getLogger(PcepLspObjectVer1.class);

    public static final byte LSP_OBJ_TYPE = 1;
    public static final byte LSP_OBJ_CLASS = 32;
    public static final byte LSP_OBJECT_VERSION = 1;

    // LSP_OBJ_MINIMUM_LENGTH = CommonHeaderLen(4)+ LspObjectHeaderLen(4)+TlvAssumedMinLength(8)
    public static final short LSP_OBJ_MINIMUM_LENGTH = 16;

    public static final int DEFAULT_PLSPID = 0;
    public static final byte DEFAULT_OFLAG = 1;
    public static final boolean DEFAULT_AFLAG = false;
    public static final boolean DEFAULT_RFLAG = false;
    public static final boolean DEFAULT_SFLAG = false;
    public static final boolean DEFAULT_DFLAG = false;
    public static final boolean DEFAULT_CFLAG = false;
    public static final int OBJECT_HEADER_LENGTH = 4;
    public static final int PLSPID_SHIFT_VALUE = 12;
    public static final int CFLAG_SHIFT_VALUE = 7;
    public static final int OFLAG_SHIFT_VALUE = 4;
    public static final int AFLAG_SHIFT_VALUE = 3;
    public static final int RFLAG_SHIFT_VALUE = 2;
    public static final int SFLAG_SHIFT_VALUE = 1;
    public static final int PLSPID_TEMP_SHIFT_VALUE = 0xFFFFF000;
    public static final int CFLAG_TEMP_SHIFT_VALUE = 0x80;
    public static final int OFLAG_TEMP_SHIFT_VALUE = 0x70;
    public static final int AFLAG_TEMP_SHIFT_VALUE = 0x08;
    public static final int RFLAG_TEMP_SHIFT_VALUE = 0x04;
    public static final int SFLAG_TEMP_SHIFT_VALUE = 0x02;
    public static final int DFLAG_TEMP_SHIFT_VALUE = 0x01;
    public static final int BIT_SET = 1;
    public static final int BIT_RESET = 0;
    public static final int MINIMUM_COMMON_HEADER_LENGTH = 4;

    static final PcepObjectHeader DEFAULT_LSP_OBJECT_HEADER = new PcepObjectHeader(LSP_OBJ_CLASS, LSP_OBJ_TYPE,
            PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED, LSP_OBJ_MINIMUM_LENGTH);

    private PcepObjectHeader lspObjHeader;
    private int iPlspId;
    // 3-bits
    private byte yOFlag;
    private boolean bAFlag;
    private boolean bRFlag;
    private boolean bSFlag;
    private boolean bDFlag;
    private boolean bCFlag;

    // Optional TLV
    private LinkedList<PcepValueType> llOptionalTlv;

    /**
     * Constructor to initialize all the member variables.
     *
     * @param lspObjHeader lsp object header
     * @param iPlspId plsp id
     * @param yOFlag O flag
     * @param bAFlag A flag
     * @param bRFlag R flag
     * @param bSFlag S flag
     * @param bDFlag D flag
     * @param bCFlag C flag
     * @param llOptionalTlv list of optional tlv
     */
    public PcepLspObjectVer1(PcepObjectHeader lspObjHeader, int iPlspId, byte yOFlag, boolean bAFlag, boolean bRFlag,
            boolean bSFlag, boolean bDFlag, boolean bCFlag, LinkedList<PcepValueType> llOptionalTlv) {

        this.lspObjHeader = lspObjHeader;
        this.iPlspId = iPlspId;
        this.yOFlag = yOFlag;
        this.bAFlag = bAFlag;
        this.bRFlag = bRFlag;
        this.bSFlag = bSFlag;
        this.bDFlag = bDFlag;
        this.bCFlag = bCFlag;
        this.llOptionalTlv = llOptionalTlv;
    }

    /**
     * Sets lsp Object Header.
     *
     * @param obj lsp object header
     */
    public void setLspObjHeader(PcepObjectHeader obj) {
        this.lspObjHeader = obj;
    }

    @Override
    public void setPlspId(int iPlspId) {
        this.iPlspId = iPlspId;
    }

    @Override
    public void setCFlag(boolean bCFlag) {
        this.bCFlag = bCFlag;
    }

    @Override
    public void setOFlag(byte yOFlag) {
        this.yOFlag = yOFlag;
    }

    @Override
    public void setAFlag(boolean bAFlag) {
        this.bAFlag = bAFlag;
    }

    @Override
    public void setRFlag(boolean bRFlag) {
        this.bRFlag = bRFlag;
    }

    @Override
    public void setSFlag(boolean bSFlag) {
        this.bSFlag = bSFlag;
    }

    @Override
    public void setDFlag(boolean bDFlag) {
        this.bDFlag = bDFlag;
    }

    /**
     * Returns lsp object header.
     *
     * @return lspObjHeader
     */
    public PcepObjectHeader getLspObjHeader() {
        return this.lspObjHeader;
    }

    @Override
    public int getPlspId() {
        return this.iPlspId;
    }

    @Override
    public boolean getCFlag() {
        return this.bCFlag;
    }

    @Override
    public byte getOFlag() {
        return this.yOFlag;
    }

    @Override
    public boolean getAFlag() {
        return this.bAFlag;
    }

    @Override
    public boolean getRFlag() {
        return this.bRFlag;
    }

    @Override
    public boolean getSFlag() {
        return this.bSFlag;
    }

    @Override
    public boolean getDFlag() {
        return this.bDFlag;
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
     * Parse channel buffer and returns object of PcepLspObject.
     *
     * @param cb of type channel buffer
     * @return object of  PcepLspObject
     * @throws PcepParseException when lsp object is not present in channel buffer
     */
    public static PcepLspObject read(ChannelBuffer cb) throws PcepParseException {

        PcepObjectHeader lspObjHeader;
        int iPlspId;
        // 3-bits
        byte yOFlag;
        boolean bAFlag;
        boolean bRFlag;
        boolean bSFlag;
        boolean bDFlag;
        boolean bCFlag;

        // Optional TLV
        LinkedList<PcepValueType> llOptionalTlv = new LinkedList<>();

        lspObjHeader = PcepObjectHeader.read(cb);

        if (lspObjHeader.getObjClass() != PcepLspObjectVer1.LSP_OBJ_CLASS) {
            throw new PcepParseException(PcepErrorDetailInfo.ERROR_TYPE_6, PcepErrorDetailInfo.ERROR_VALUE_8);
        }
        //take only LspObject buffer.
        ChannelBuffer tempCb = cb.readBytes(lspObjHeader.getObjLen() - OBJECT_HEADER_LENGTH);

        Integer iTemp = tempCb.readInt();
        iPlspId = (iTemp & PLSPID_TEMP_SHIFT_VALUE) >> PLSPID_SHIFT_VALUE;
        bCFlag = ((iTemp & CFLAG_TEMP_SHIFT_VALUE) >> CFLAG_SHIFT_VALUE) > 0;
        Integer iX = (iTemp & OFLAG_TEMP_SHIFT_VALUE) >> OFLAG_SHIFT_VALUE;
        yOFlag = iX.byteValue();
        iX = (iTemp & AFLAG_TEMP_SHIFT_VALUE) >> AFLAG_SHIFT_VALUE;
        bAFlag = iX > 0;
        iX = (iTemp & RFLAG_TEMP_SHIFT_VALUE) >> RFLAG_SHIFT_VALUE;
        bRFlag = iX > 0;
        iX = (iTemp & SFLAG_TEMP_SHIFT_VALUE) >> SFLAG_SHIFT_VALUE;
        bSFlag = iX > 0;
        iX = iTemp & DFLAG_TEMP_SHIFT_VALUE;
        bDFlag = iX > 0;

        // parse optional TLV
        llOptionalTlv = parseOptionalTlv(tempCb);

        return new PcepLspObjectVer1(lspObjHeader, iPlspId, yOFlag, bAFlag, bRFlag, bSFlag, bDFlag, bCFlag,
                                     llOptionalTlv);
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {

        //write Object header
        int objStartIndex = cb.writerIndex();

        int objLenIndex = lspObjHeader.write(cb);

        if (objLenIndex <= 0) {
            throw new PcepParseException("Failed to write lsp object header. Index " + objLenIndex);
        }

        int iTemp = iPlspId << PLSPID_SHIFT_VALUE;

        iTemp = iTemp | (((bCFlag) ? BIT_SET : BIT_RESET) << CFLAG_SHIFT_VALUE);

        iTemp = iTemp | (yOFlag << OFLAG_SHIFT_VALUE);
        byte bFlag;
        iTemp = bAFlag ? (iTemp | AFLAG_TEMP_SHIFT_VALUE) : iTemp;

        bFlag = (bRFlag) ? (byte) BIT_SET : BIT_RESET;
        iTemp = iTemp | (bFlag << RFLAG_SHIFT_VALUE);
        bFlag = (bSFlag) ? (byte) BIT_SET : BIT_RESET;
        iTemp = iTemp | (bFlag << SFLAG_SHIFT_VALUE);
        bFlag = (bDFlag) ? (byte) BIT_SET : BIT_RESET;
        iTemp = iTemp | bFlag;
        cb.writeInt(iTemp);

        // Add optional TLV
        packOptionalTlv(cb);

        //Update object length now
        int length = cb.writerIndex() - objStartIndex;
        //will be helpful during print().
        lspObjHeader.setObjLen((short) length);
        // As per RFC the length of object should be
        // multiples of 4

        cb.setShort(objLenIndex, (short) length);

        return length;
    }

    /**
     * Returns Linked list of optional tlvs.
     *
     * @param cb of channel buffer.
     * @return list of optional tlvs
     * @throws PcepParseException when unsupported tlv is received
     */
    protected static LinkedList<PcepValueType> parseOptionalTlv(ChannelBuffer cb) throws PcepParseException {

        LinkedList<PcepValueType> llOutOptionalTlv;

        llOutOptionalTlv = new LinkedList<>();

        while (MINIMUM_COMMON_HEADER_LENGTH <= cb.readableBytes()) {

            PcepValueType tlv = null;
            short hType = cb.readShort();
            short hLength = cb.readShort();
            int iValue = 0;

            switch (hType) {

            case StatefulIPv4LspIdentifiersTlv.TYPE:
                tlv = StatefulIPv4LspIdentifiersTlv.read(cb);
                break;
            case StatefulLspErrorCodeTlv.TYPE:
                iValue = cb.readInt();
                tlv = new StatefulLspErrorCodeTlv(iValue);
                break;
            case StatefulRsvpErrorSpecTlv.TYPE:
                tlv = StatefulRsvpErrorSpecTlv.read(cb);
                break;
            case SymbolicPathNameTlv.TYPE:
                tlv = SymbolicPathNameTlv.read(cb, hLength);
                break;
            case StatefulLspDbVerTlv.TYPE:
                tlv = StatefulLspDbVerTlv.read(cb);
                break;
            default:
                // Skip the unknown TLV.
                cb.skipBytes(hLength);
                log.info("Received unsupported TLV type :" + hType + " in LSP object.");
            }
            // Check for the padding
            int pad = hLength % 4;
            if (0 < pad) {
                pad = 4 - pad;
                if (pad <= cb.readableBytes()) {
                    cb.skipBytes(pad);
                }
            }

            if (tlv != null) {
                llOutOptionalTlv.add(tlv);
            }
        }

        if (0 < cb.readableBytes()) {

            throw new PcepParseException("Optional Tlv parsing error. Extra bytes received.");
        }
        return llOutOptionalTlv;
    }

    /**
     * returns writer index.
     *
     * @param cb of type channel buffer
     * @return length of bytes written to channel buffer
     */
    protected int packOptionalTlv(ChannelBuffer cb) {

        ListIterator<PcepValueType> listIterator = llOptionalTlv.listIterator();
        int startIndex = cb.writerIndex();

        while (listIterator.hasNext()) {
            PcepValueType tlv = listIterator.next();

            if (tlv == null) {
                log.debug("tlv is null from OptionalTlv list");
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

        return cb.writerIndex() - startIndex;
    }

    /**
     * Builder class for PCEP lsp Object.
     */
    public static class Builder implements PcepLspObject.Builder {

        private boolean bIsHeaderSet = false;
        private boolean bIsPlspIdSet = false;
        private boolean bIsOFlagSet = false;
        private boolean bIsRFlagSet = false;
        private boolean bIsAFlagSet = false;
        private boolean bIsDFlagSet = false;
        private boolean bIsSFlagSet = false;
        private boolean bIsCFlagSet = false;

        private PcepObjectHeader lspObjHeader;
        private byte yOFlag;
        private boolean bAFlag;
        private boolean bDFlag;
        private boolean bSFlag;
        private boolean bRFlag;
        private boolean bCFlag;
        LinkedList<PcepValueType> llOptionalTlv = null;

        private int plspId;

        private boolean bIsPFlagSet = false;
        private boolean bPFlag;

        private boolean bIsIFlagSet = false;
        private boolean bIFlag;

        @Override
        public PcepLspObject build() {
            PcepObjectHeader lspObjHeader = this.bIsHeaderSet ? this.lspObjHeader : DEFAULT_LSP_OBJECT_HEADER;

            int plspId = this.bIsPlspIdSet ? this.plspId : DEFAULT_PLSPID;
            byte yOFlag = this.bIsOFlagSet ? this.yOFlag : DEFAULT_OFLAG;
            boolean bAFlag = this.bIsAFlagSet ? this.bAFlag : DEFAULT_AFLAG;
            boolean bRFlag = this.bIsRFlagSet ? this.bRFlag : DEFAULT_RFLAG;
            boolean bSFlag = this.bIsSFlagSet ? this.bSFlag : DEFAULT_SFLAG;
            boolean bDFlag = this.bIsDFlagSet ? this.bDFlag : DEFAULT_DFLAG;
            boolean bCFlag = this.bIsCFlagSet ? this.bCFlag : DEFAULT_CFLAG;

            if (bIsPFlagSet) {
                lspObjHeader.setPFlag(bPFlag);
            }

            if (bIsIFlagSet) {
                lspObjHeader.setIFlag(bIFlag);
            }

            return new PcepLspObjectVer1(lspObjHeader, plspId, yOFlag, bAFlag, bRFlag, bSFlag, bDFlag, bCFlag,
                                         llOptionalTlv);
        }

        @Override
        public PcepObjectHeader getLspObjHeader() {
            return this.lspObjHeader;
        }

        @Override
        public Builder setLspObjHeader(PcepObjectHeader obj) {
            this.lspObjHeader = obj;
            this.bIsHeaderSet = true;
            return this;
        }

        @Override
        public int getPlspId() {
            return this.plspId;
        }

        @Override
        public Builder setPlspId(int value) {
            this.plspId = value;
            this.bIsPlspIdSet = true;
            return this;
        }

        @Override
        public boolean getCFlag() {
            return this.bCFlag;
        }

        @Override
        public Builder setCFlag(boolean value) {
            this.bCFlag = value;
            this.bIsCFlagSet = true;
            return this;
        }

        @Override
        public byte getOFlag() {
            return this.yOFlag;
        }

        @Override
        public Builder setOFlag(byte value) {
            this.yOFlag = value;
            this.bIsOFlagSet = true;
            return this;
        }

        @Override
        public boolean getAFlag() {
            return this.bAFlag;
        }

        @Override
        public Builder setAFlag(boolean value) {
            this.bAFlag = value;
            this.bIsAFlagSet = true;
            return this;
        }

        @Override
        public boolean getRFlag() {
            return this.bRFlag;
        }

        @Override
        public Builder setRFlag(boolean value) {
            this.bRFlag = value;
            this.bIsRFlagSet = true;
            return this;
        }

        @Override
        public boolean getSFlag() {
            return this.bSFlag;
        }

        @Override
        public Builder setSFlag(boolean value) {
            this.bSFlag = value;
            this.bIsSFlagSet = true;
            return this;
        }

        @Override
        public boolean getDFlag() {
            return this.bDFlag;
        }

        @Override
        public Builder setDFlag(boolean value) {
            this.bDFlag = value;
            this.bIsDFlagSet = true;
            return this;
        }

        @Override
        public Builder setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv) {
            this.llOptionalTlv = llOptionalTlv;
            return this;
        }

        @Override
        public LinkedList<PcepValueType> getOptionalTlv() {
            return this.llOptionalTlv;
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
                .add("PlspIDValue", iPlspId)
                .add("CFlag", bCFlag)
                .add("OFlag", yOFlag)
                .add("AFlag", bAFlag)
                .add("RFlag", bRFlag)
                .add("SFlag", bSFlag)
                .add("DFlag", bDFlag)
                .add("OptionalTlvList", llOptionalTlv)
                .toString();
    }
}
