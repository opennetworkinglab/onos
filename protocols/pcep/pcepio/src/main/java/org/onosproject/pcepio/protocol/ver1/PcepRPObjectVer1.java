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
import org.onosproject.pcepio.protocol.PcepRPObject;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.onosproject.pcepio.types.PcepValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP RP object.
 */
public class PcepRPObjectVer1 implements PcepRPObject {

    /*
     *  RP Object.
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                          Flags                    |O|B|R| Pri |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                        Request-ID-number                      |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                                                               |
    //                      Optional TLVs                          //
    |                                                               |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    protected static final Logger log = LoggerFactory.getLogger(PcepRPObjectVer1.class);

    public static final byte RP_OBJ_TYPE = 1;
    public static final byte RP_OBJ_CLASS = 2;
    public static final byte RP_OBJECT_VERSION = 1;
    public static final short RP_OBJ_MINIMUM_LENGTH = 12;

    public static final int DEFAULT_REQUEST_ID_NUM = 0;
    //Signalled , all default values to be checked.
    public static final boolean DEFAULT_OFLAG = false;
    public static final boolean DEFAULT_BFLAG = false;
    public static final boolean DEFAULT_RFLAG = false;
    public static final byte DEFAULT_PRIFLAG = 0;
    public static final int OBJECT_HEADER_LENGTH = 4;
    public static final int OFLAG_SHIFT_VALUE = 5;
    public static final int BFLAG_SHIFT_VALUE = 4;
    public static final int RFLAG_SHIFT_VALUE = 3;
    public static final int OFLAG_TEMP_SHIFT_VALUE = 0x20;
    public static final int BFLAG_TEMP_SHIFT_VALUE = 0x10;
    public static final int RFLAG_TEMP_SHIFT_VALUE = 0x08;
    public static final int PRIFLAG_TEMP_SHIFT_VALUE = 0x07;
    public static final int BIT_SET = 1;
    public static final int BIT_RESET = 0;
    public static final int MINIMUM_COMMON_HEADER_LENGTH = 4;

    public static final PcepObjectHeader DEFAULT_RP_OBJECT_HEADER = new PcepObjectHeader(RP_OBJ_CLASS, RP_OBJ_TYPE,
            PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED, RP_OBJ_MINIMUM_LENGTH);

    private PcepObjectHeader rpObjHeader;
    private int iRequestIdNum;
    private boolean bOFlag;
    private boolean bBFlag;
    private boolean bRFlag;
    private byte yPriFlag; // 3bytes
    private LinkedList<PcepValueType> llOptionalTlv;

    /**
     * Constructor to initialize variables.
     *
     * @param rpObjHeader RP-OBJECT header
     * @param iRequestIdNum Request-ID-number
     * @param bOFlag O-flag
     * @param bBFlag B-flag
     * @param bRFlag R-flag
     * @param yPriFlag Pri-flag
     * @param llOptionalTlv linked list of Optional TLV
     */
    public PcepRPObjectVer1(PcepObjectHeader rpObjHeader, int iRequestIdNum, boolean bOFlag, boolean bBFlag,
            boolean bRFlag, byte yPriFlag, LinkedList<PcepValueType> llOptionalTlv) {
        this.rpObjHeader = rpObjHeader;
        this.iRequestIdNum = iRequestIdNum;
        this.bOFlag = bOFlag;
        this.bBFlag = bBFlag;
        this.bRFlag = bRFlag;
        this.yPriFlag = yPriFlag;
        this.llOptionalTlv = llOptionalTlv;
    }

    /**
     * Sets RP Object header.
     *
     * @param obj RP Object header
     */
    public void setRPObjHeader(PcepObjectHeader obj) {
        this.rpObjHeader = obj;
    }

    @Override
    public void setRequestIdNum(int iRequestIdNum) {
        this.iRequestIdNum = iRequestIdNum;
    }

    @Override
    public void setOFlag(boolean bOFlag) {
        this.bOFlag = bOFlag;
    }

    @Override
    public void setBFlag(boolean bBFlag) {
        this.bBFlag = bBFlag;
    }

    @Override
    public void setRFlag(boolean bRFlag) {
        this.bRFlag = bRFlag;
    }

    @Override
    public void setPriFlag(byte yPriFlag) {
        this.yPriFlag = yPriFlag;
    }

    /**
     * Returns RP Object header.
     *
     * @return rpObjHeader
     */
    public PcepObjectHeader getRPObjHeader() {
        return this.rpObjHeader;
    }

    @Override
    public int getRequestIdNum() {
        return this.iRequestIdNum;
    }

    @Override
    public boolean getOFlag() {
        return this.bOFlag;
    }

    @Override
    public boolean getBFlag() {
        return this.bBFlag;
    }

    @Override
    public boolean getRFlag() {
        return this.bRFlag;
    }

    @Override
    public byte getPriFlag() {
        return this.yPriFlag;
    }

    /**
     * Reads the channel buffer and returns the object of PcepRPObject.
     *
     * @param cb of type channel buffer
     * @return the object of PcepRPObject
     * @throws PcepParseException if mandatory fields are missing
     */
    public static PcepRPObject read(ChannelBuffer cb) throws PcepParseException {
        log.debug("read");
        PcepObjectHeader rpObjHeader;
        int iRequestIdNum;
        boolean bOFlag;
        boolean bBFlag;
        boolean bRFlag;
        byte yPriFlag; // 3bytes
        LinkedList<PcepValueType> llOptionalTlv = new LinkedList<>();

        rpObjHeader = PcepObjectHeader.read(cb);

        //take only LspObject buffer.
        ChannelBuffer tempCb = cb.readBytes(rpObjHeader.getObjLen() - OBJECT_HEADER_LENGTH);

        int iTemp = tempCb.readInt();
        yPriFlag = (byte) (iTemp & PRIFLAG_TEMP_SHIFT_VALUE);
        bOFlag = (iTemp & OFLAG_TEMP_SHIFT_VALUE) == OFLAG_TEMP_SHIFT_VALUE;
        bBFlag = (iTemp & BFLAG_TEMP_SHIFT_VALUE) == BFLAG_TEMP_SHIFT_VALUE;
        bRFlag = (iTemp & RFLAG_TEMP_SHIFT_VALUE) == RFLAG_TEMP_SHIFT_VALUE;

        iRequestIdNum = tempCb.readInt();

        // parse optional TLV
        llOptionalTlv = parseOptionalTlv(tempCb);

        return new PcepRPObjectVer1(rpObjHeader, iRequestIdNum, bOFlag, bBFlag, bRFlag, yPriFlag, llOptionalTlv);
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {

        //write Object header
        int objStartIndex = cb.writerIndex();

        int objLenIndex = rpObjHeader.write(cb);

        if (objLenIndex <= 0) {
            throw new PcepParseException("ObjectLength Index is " + objLenIndex);
        }
        int iTemp;
        iTemp = (yPriFlag);

        iTemp = (bOFlag) ? (iTemp | OFLAG_SHIFT_VALUE) : iTemp;
        iTemp = (bBFlag) ? (iTemp | BFLAG_SHIFT_VALUE) : iTemp;
        iTemp = (bRFlag) ? (iTemp | RFLAG_SHIFT_VALUE) : iTemp;

        cb.writeInt(iTemp);
        cb.writeInt(iRequestIdNum);

        // Add optional TLV
        packOptionalTlv(cb);

        //Update object length now
        int length = cb.writerIndex() - objStartIndex;

        //will be helpful during print().
        rpObjHeader.setObjLen((short) length);

        cb.setShort(objLenIndex, (short) length);
        return cb.writerIndex();
    }

    /**
     * Returns list of optional tlvs.
     *
     * @param cb of type channel buffer.
     * @return llOutOptionalTlv linked list of Optional TLV
     * @throws PcepParseException if mandatory fields are missing
     */
    protected static LinkedList<PcepValueType> parseOptionalTlv(ChannelBuffer cb) throws PcepParseException {

        LinkedList<PcepValueType> llOutOptionalTlv = new LinkedList<>();
        //Currently no optional TLvs, will be added based on requirements.
        return llOutOptionalTlv;
    }

    /**
     * Returns optional tlvs.
     *
     * @param cb of type channel buffer
     * @return llOptionalTlv linked list of Optional TLV
     */
    protected int packOptionalTlv(ChannelBuffer cb) {

        ListIterator<PcepValueType> listIterator = llOptionalTlv.listIterator();
        while (listIterator.hasNext()) {
            listIterator.next().write(cb);
        }

        return cb.writerIndex();
    }

    /**
     * Builder class for PCEP rp object.
     */
    public static class Builder implements PcepRPObject.Builder {

        private boolean bIsHeaderSet = false;
        private boolean bIsRequestIdNumSet = false;
        private boolean bIsOFlagSet = false;
        private boolean bIsRFlagset = false;
        private boolean bIsBFlagSet = false;
        private boolean bIsPriFlagSet = false;

        private PcepObjectHeader rpObjHeader;
        private int requestIdNum;
        private boolean bOFlag;
        private boolean bBFlag;
        private boolean bRFlag;
        private byte yPriFlag;
        private LinkedList<PcepValueType> llOptionalTlv = new LinkedList<>();

        private boolean bIsPFlagSet = false;
        private boolean bPFlag;

        private boolean bIsIFlagSet = false;
        private boolean bIFlag;

        @Override
        public PcepRPObject build() {
            PcepObjectHeader lspObjHeader = this.bIsHeaderSet ? this.rpObjHeader : DEFAULT_RP_OBJECT_HEADER;

            int requestIdNum = this.bIsRequestIdNumSet ? this.requestIdNum : DEFAULT_REQUEST_ID_NUM;
            boolean bOFlag = this.bIsOFlagSet ? this.bOFlag : DEFAULT_OFLAG;
            boolean bBFlag = this.bIsBFlagSet ? this.bBFlag : DEFAULT_BFLAG;
            boolean bRFlag = this.bIsRFlagset ? this.bRFlag : DEFAULT_RFLAG;
            byte yPriFlag = this.bIsPriFlagSet ? this.yPriFlag : DEFAULT_PRIFLAG;

            if (bIsPFlagSet) {
                lspObjHeader.setPFlag(bPFlag);
            }

            if (bIsIFlagSet) {
                lspObjHeader.setIFlag(bIFlag);
            }

            return new PcepRPObjectVer1(lspObjHeader, requestIdNum, bOFlag, bBFlag, bRFlag, yPriFlag, llOptionalTlv);
        }

        @Override
        public PcepObjectHeader getRPObjHeader() {
            return this.rpObjHeader;
        }

        @Override
        public Builder setRPObjHeader(PcepObjectHeader obj) {
            this.rpObjHeader = obj;
            this.bIsHeaderSet = true;
            return this;
        }

        @Override
        public int getRequestIdNum() {
            return this.requestIdNum;
        }

        @Override
        public Builder setRequestIdNum(int value) {
            this.requestIdNum = value;
            this.bIsRequestIdNumSet = true;
            return this;
        }

        @Override
        public Builder setOFlag(boolean value) {
            this.bOFlag = value;
            this.bIsOFlagSet = true;
            return this;
        }

        @Override
        public boolean getBFlag() {
            return this.bBFlag;
        }

        @Override
        public Builder setBFlag(boolean value) {
            this.bBFlag = value;
            this.bIsBFlagSet = true;
            return this;
        }

        @Override
        public boolean getRFlag() {
            return this.bRFlag;
        }

        @Override
        public Builder setRFlag(boolean value) {
            this.bRFlag = value;
            this.bIsRFlagset = true;
            return this;
        }

        @Override
        public byte getPriFlag() {
            return this.yPriFlag;
        }

        @Override
        public Builder setPriFlag(byte value) {
            this.yPriFlag = value;
            this.bIsPriFlagSet = true;
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

        @Override
        public boolean getOFlag() {
            return this.bOFlag;
        }

    }

    @Override
    public LinkedList<PcepValueType> getOptionalTlv() {
        return this.llOptionalTlv;
    }

    @Override
    public void setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv) {
        this.llOptionalTlv = llOptionalTlv;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("ObjectHeader", rpObjHeader)
                .add("OFlag", (bOFlag) ? 1 : 0)
                .add("BFlag", (bBFlag) ? 1 : 0)
                .add("RFlag", (bRFlag) ? 1 : 0)
                .add("PriFlag", yPriFlag)
                .add("RequestIdNumber", iRequestIdNum)
                .add("OptionalTlv", llOptionalTlv)
                .toString();
    }
}
