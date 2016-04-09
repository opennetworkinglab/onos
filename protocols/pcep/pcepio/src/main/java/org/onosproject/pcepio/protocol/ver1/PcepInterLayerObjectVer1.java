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

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepInterLayerObject;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP inter layer object.
 */
public class PcepInterLayerObjectVer1 implements PcepInterLayerObject {

    /*
     *      0                   1                   2                   3
         0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |    Reserved                                               |N|I|
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    protected static final Logger log = LoggerFactory.getLogger(PcepInterLayerObjectVer1.class);

    public static final byte INTER_LAYER_OBJ_TYPE = 1;
    public static final byte INTER_LAYER_OBJ_CLASS = 18;
    public static final byte INTER_LAYER_OBJECT_VERSION = 1;
    public static final short INTER_LAYER_OBJ_MINIMUM_LENGTH = 8;
    public static final boolean DEFAULT_IFLAG = false;
    public static final boolean DEFAULT_NFLAG = false;
    public static final int OBJECT_HEADER_LENGTH = 4;
    public static final int NFLAG_SHIFT_VALUE = 0x02;
    public static final int IFLAG_SHIFT_VALUE = 0x01;

    static final PcepObjectHeader DEFAULT_INTER_LAYER_OBJECT_HEADER = new PcepObjectHeader(INTER_LAYER_OBJ_CLASS,
            INTER_LAYER_OBJ_TYPE, PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED,
            INTER_LAYER_OBJ_MINIMUM_LENGTH);

    private PcepObjectHeader interLayerObjHeader;
    private boolean bNFlag;
    private boolean bIFlag;

    /**
     * Constructor to initialize all parameters for Pcep Inter Layer Object.
     *
     * @param interLayerObjHeader inter layer object header
     * @param bNFlag N flag
     * @param bIFlag I flag
     */
    public PcepInterLayerObjectVer1(PcepObjectHeader interLayerObjHeader, boolean bNFlag, boolean bIFlag) {

        this.interLayerObjHeader = interLayerObjHeader;
        this.bNFlag = bNFlag;
        this.bIFlag = bIFlag;
    }

    /**
     * Sets Object Header.
     *
     * @param obj object header
     */
    public void setInterLayerObjHeader(PcepObjectHeader obj) {
        this.interLayerObjHeader = obj;
    }

    @Override
    public void setbNFlag(boolean bNFlag) {
        this.bNFlag = bNFlag;
    }

    @Override
    public void setbIFlag(boolean bIFlag) {
        this.bIFlag = bIFlag;
    }

    /**
     * Returns object header.
     *
     * @return inter Layer Object Header
     */
    public PcepObjectHeader getInterLayerObjHeader() {
        return this.interLayerObjHeader;
    }

    @Override
    public boolean getbNFlag() {
        return this.bNFlag;
    }

    @Override
    public boolean getbIFlag() {
        return this.bIFlag;
    }

    /**
     * Reads channel buffer and returns object of PcepInterLayerObject.
     *
     * @param cb of type channel buffer
     * @return object of PcepInterLayerObject
     * @throws PcepParseException when fails to read from channel buffer
     */
    public static PcepInterLayerObject read(ChannelBuffer cb) throws PcepParseException {

        PcepObjectHeader interLayerObjHeader;
        boolean bNFlag;
        boolean bIFlag;

        interLayerObjHeader = PcepObjectHeader.read(cb);

        //take only InterLayerObject buffer.
        ChannelBuffer tempCb = cb.readBytes(interLayerObjHeader.getObjLen() - OBJECT_HEADER_LENGTH);

        int iTemp = tempCb.readInt();
        bIFlag = ((iTemp & (byte) IFLAG_SHIFT_VALUE) == IFLAG_SHIFT_VALUE);
        bNFlag = ((iTemp & (byte) NFLAG_SHIFT_VALUE) == NFLAG_SHIFT_VALUE);

        return new PcepInterLayerObjectVer1(interLayerObjHeader, bNFlag, bIFlag);
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {

        //write Object header
        int objStartIndex = cb.writerIndex();

        int objLenIndex = interLayerObjHeader.write(cb);

        if (objLenIndex <= 0) {
            throw new PcepParseException(" ObjectLength Index is " + objLenIndex);
        }

        int iTemp = 0;

        if (bIFlag) {
            iTemp = iTemp | (byte) IFLAG_SHIFT_VALUE;
        }
        if (bNFlag) {
            iTemp = iTemp | (byte) NFLAG_SHIFT_VALUE;
        }

        cb.writeInt(iTemp);

        //Update object length now
        int length = cb.writerIndex() - objStartIndex;
        //will be helpful during print().
        interLayerObjHeader.setObjLen((short) length);
        cb.setShort(objLenIndex, (short) length);

        objLenIndex = cb.writerIndex();
        return objLenIndex;
    }

    /**
     * Builder class for PCEP inter layer object.
     */
    public static class Builder implements PcepInterLayerObject.Builder {

        private boolean bIsHeaderSet = false;
        private boolean bIsNFlagset = false;
        private boolean bIsIFlagset = false;

        private PcepObjectHeader interLayerObjHeader;
        private boolean bNFlag;
        private boolean bIFlag;

        private boolean bIsPFlagSet = false;
        private boolean bPFalg;

        private boolean bIsIFlagSet = false;
        private boolean iFlag;

        @Override
        public PcepInterLayerObject build() {
            PcepObjectHeader interLayerObjHeader = this.bIsHeaderSet ? this.interLayerObjHeader
                    : DEFAULT_INTER_LAYER_OBJECT_HEADER;

            boolean bNFlag = this.bIsNFlagset ? this.bNFlag : DEFAULT_NFLAG;
            boolean bIFlag = this.bIsIFlagset ? this.bIFlag : DEFAULT_IFLAG;

            if (bIsPFlagSet) {
                interLayerObjHeader.setPFlag(bPFalg);
            }

            if (bIsIFlagSet) {
                interLayerObjHeader.setIFlag(iFlag);
            }
            return new PcepInterLayerObjectVer1(interLayerObjHeader, bNFlag, bIFlag);
        }

        @Override
        public PcepObjectHeader getInterLayerObjHeader() {
            return this.interLayerObjHeader;
        }

        @Override
        public Builder setInterLayerObjHeader(PcepObjectHeader obj) {
            this.interLayerObjHeader = obj;
            this.bIsHeaderSet = true;
            return this;
        }

        @Override
        public boolean getbNFlag() {
            return this.bNFlag;
        }

        @Override
        public Builder setbNFlag(boolean value) {
            this.bNFlag = value;
            this.bIsNFlagset = true;
            return this;
        }

        @Override
        public boolean getbIFlag() {
            return this.bIFlag;
        }

        @Override
        public Builder setbIFlag(boolean value) {
            this.bIFlag = value;
            this.bIsIFlagset = true;
            return this;
        }

        @Override
        public Builder setPFlag(boolean value) {
            this.bPFalg = value;
            this.bIsPFlagSet = true;
            return this;
        }

        @Override
        public Builder setIFlag(boolean value) {
            this.iFlag = value;
            this.bIsIFlagSet = true;
            return this;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("IFlag", bIFlag)
                .add("NFlag", bNFlag).toString();
    }
}
