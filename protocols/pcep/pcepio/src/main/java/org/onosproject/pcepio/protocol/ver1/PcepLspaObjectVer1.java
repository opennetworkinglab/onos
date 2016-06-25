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
import org.onosproject.pcepio.protocol.PcepLspaObject;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.onosproject.pcepio.types.PcepValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP label Object .
 */
public class PcepLspaObjectVer1 implements PcepLspaObject {

    /* LSPA Object Body Format

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                       Exclude-any                             |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                       Include-any                             |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                       Include-all                             |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |  Setup Prio   |  Holding Prio |     Flags   |L|   Reserved    |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                                                               |
    |                      Optional TLVs                            |
    |                                                               |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    protected static final Logger log = LoggerFactory.getLogger(PcepLspaObjectVer1.class);

    public static final byte LSPA_OBJ_TYPE = 1;
    public static final byte LSPA_OBJ_CLASS = 9;
    public static final byte LSPA_OBJECT_VERSION = 1;
    public static final short LSPA_OBJ_MINIMUM_LENGTH = 20;
    public static final int OBJECT_HEADER_LENGTH = 4;

    static final PcepObjectHeader DEFAULT_LSPA_OBJECT_HEADER = new PcepObjectHeader(LSPA_OBJ_CLASS, LSPA_OBJ_TYPE,
            PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED, LSPA_OBJ_MINIMUM_LENGTH);

    public static final int SETUP_PRIORITY_SHIFT_VALUE = 24;
    public static final int HOLD_PRIORITY_SHIFT_VALUE = 16;
    public static final int BFLAG_SHIFT_VALUE = 8;
    public static final int LFLAG_SET = 1;
    public static final int LFLAG_RESET = 0;
    private PcepObjectHeader lspaObjHeader;
    private int iExcludeAny;
    private int iIncludeAny;
    private int iIncludeAll;
    private byte cSetupPriority;
    private byte cHoldPriority;
    private boolean bLFlag;
    private LinkedList<PcepValueType> llOptionalTlv; //Optional TLV

    /**
     * Constructor to initialize member variables.
     *
     * @param lspaObjHeader lspa object header
     * @param bLFlag b l flag
     * @param iExcludeAny excludeAny value
     * @param iIncludeAny includeAny value
     * @param iIncludeAll includeAll value
     * @param cSetupPriority setup priority value
     * @param cHoldPriority hold priority value
     * @param llOptionalTlv list of optional tlv
     */
    public PcepLspaObjectVer1(PcepObjectHeader lspaObjHeader, boolean bLFlag, int iExcludeAny, int iIncludeAny,
            int iIncludeAll, byte cSetupPriority, byte cHoldPriority, LinkedList<PcepValueType> llOptionalTlv) {

        this.lspaObjHeader = lspaObjHeader;
        this.bLFlag = bLFlag;
        this.iExcludeAny = iExcludeAny;
        this.iIncludeAny = iIncludeAny;
        this.iIncludeAll = iIncludeAll;
        this.cSetupPriority = cSetupPriority;
        this.cHoldPriority = cHoldPriority;
        this.llOptionalTlv = llOptionalTlv;
    }

    /**
     * Sets Object Header.
     *
     * @param obj lspa object header
     */
    public void setLspaObjHeader(PcepObjectHeader obj) {
        this.lspaObjHeader = obj;
    }

    @Override
    public void setExcludeAny(int iExcludeAny) {
        this.iExcludeAny = iExcludeAny;
    }

    @Override
    public void setIncludeAny(int iIncludeAny) {
        this.iIncludeAny = iIncludeAny;
    }

    @Override
    public void setSetupPriority(byte cSetupPriority) {
        this.cSetupPriority = cSetupPriority;
    }

    @Override
    public void setHoldPriority(byte cHoldPriority) {
        this.cHoldPriority = cHoldPriority;
    }

    @Override
    public void setLFlag(boolean bLFlag) {
        this.bLFlag = bLFlag;
    }

    /**
     * Returns lspa Object Header.
     *
     * @return lspa Object Header
     */
    public PcepObjectHeader getLspaObjHeader() {
        return this.lspaObjHeader;
    }

    @Override
    public int getExcludeAny() {
        return this.iExcludeAny;
    }

    @Override
    public int getIncludeAny() {
        return this.iIncludeAny;
    }

    @Override
    public int getIncludeAll() {
        return this.iIncludeAll;
    }

    @Override
    public byte getSetupPriority() {
        return this.cSetupPriority;
    }

    @Override
    public byte getHoldPriority() {
        return this.cHoldPriority;
    }

    @Override
    public boolean getLFlag() {
        return this.bLFlag;
    }

    @Override
    public void setIncludeAll(int value) {
        this.iIncludeAll = value;

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
     * Reads channel buffer and returns object of PcepLspaObject.
     *
     * @param cb of type channel buffer.
     * @return object of PcepLspaObject
     * @throws PcepParseException while parsing lspa object from channel buffer
     */
    public static PcepLspaObject read(ChannelBuffer cb) throws PcepParseException {

        log.debug("LspaObject::read");
        PcepObjectHeader lspaObjHeader;
        int iExcludeAny;
        int iIncludeAny;
        int iIncludeAll;
        byte cSetupPriority;
        byte cHoldPriority;
        boolean bLFlag;
        byte flags;

        // Optional TLV
        LinkedList<PcepValueType> llOptionalTlv;

        lspaObjHeader = PcepObjectHeader.read(cb);

        //take only Lspa Object buffer.
        ChannelBuffer tempCb = cb.readBytes(lspaObjHeader.getObjLen() - OBJECT_HEADER_LENGTH);
        iExcludeAny = tempCb.readInt();
        iIncludeAny = tempCb.readInt();
        iIncludeAll = tempCb.readInt();
        cSetupPriority = tempCb.readByte();
        cHoldPriority = tempCb.readByte();
        flags = tempCb.readByte();
        tempCb.readByte();

        bLFlag = (flags & (byte) LFLAG_SET) == LFLAG_SET;

        llOptionalTlv = parseOptionalTlv(tempCb);

        return new PcepLspaObjectVer1(lspaObjHeader, bLFlag, iExcludeAny, iIncludeAny, iIncludeAll, cSetupPriority,
                cHoldPriority, llOptionalTlv);
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {

        //write Object header
        int objStartIndex = cb.writerIndex();

        int objLenIndex = lspaObjHeader.write(cb);

        if (objLenIndex <= 0) {
            throw new PcepParseException("Failed to write lspa object header. Index " + objLenIndex);
        }

        cb.writeInt(iExcludeAny);
        cb.writeInt(iIncludeAny);
        cb.writeInt(iIncludeAll);

        int iTemp = cSetupPriority << SETUP_PRIORITY_SHIFT_VALUE;
        iTemp = iTemp | (cHoldPriority << HOLD_PRIORITY_SHIFT_VALUE);
        byte bFlag;
        bFlag = (bLFlag) ? (byte) LFLAG_SET : LFLAG_RESET;
        iTemp = iTemp | (bFlag << BFLAG_SHIFT_VALUE);
        cb.writeInt(iTemp);

        // Add optional TLV
        if (!packOptionalTlv(cb)) {
            throw new PcepParseException("Faild to write lspa objects tlv to channel buffer");
        }

        short length = (short) (cb.writerIndex() - objStartIndex);

        lspaObjHeader.setObjLen(length); //will be helpful during print().

        //As per RFC the length of object should be multiples of 4
        short pad = (short) (length % 4);

        if (pad != 0) {
            pad = (short) (4 - pad);
            for (int i = 0; i < pad; i++) {
                cb.writeByte((byte) 0);
            }
            length = (short) (length + pad);
        }
        cb.setShort(objLenIndex, length);
        return cb.writerIndex();
    }

    /**
     * Parse list of optional tlvs.
     *
     * @param cb channel buffer
     * @return list of optional tlvs.
     * @throws PcepParseException when fails to parse optional tlv list.
     */
    public static LinkedList<PcepValueType> parseOptionalTlv(ChannelBuffer cb) throws PcepParseException {

        LinkedList<PcepValueType> llOutOptionalTlv = new LinkedList<>();

        return llOutOptionalTlv;
    }

    /**
     * Writes optional tlvs to channel buffer.
     *
     * @param cb channel buffer
     * @return true
     */
    protected boolean packOptionalTlv(ChannelBuffer cb) {
        int hTlvType;
        int hTlvLength;

        ListIterator<PcepValueType> listIterator = llOptionalTlv.listIterator();
        while (listIterator.hasNext()) {
            PcepValueType tlv = listIterator.next();
            if (tlv == null) {
                log.debug("Warning: tlv is null from OptionalTlv list");
                continue;
            }
            hTlvType = tlv.getType();
            hTlvLength = tlv.getLength();
            if (0 == hTlvLength) {
                log.debug("Warning: invalid length in tlv of OptionalTlv list");
                continue;
            }

            cb.writeShort(hTlvType);
            cb.writeShort(hTlvLength);

            switch (hTlvType) {
            //TODO: optional TLV for LSPA to be added

            default:
                log.debug("Warning: PcepLspaObject: unknown tlv");
            }

            // As per RFC the length of object should
            // be multiples of 4
            int pad = hTlvLength % 4;

            if (0 < pad) {
                pad = 4 - pad;
                if (pad <= cb.readableBytes()) {
                    cb.skipBytes(pad);
                }
            }
        }
        return true;
    }

    /**
     * Builder class for PCEP lspa object.
     */
    public static class Builder implements PcepLspaObject.Builder {
        private boolean bIsHeaderSet = false;

        private PcepObjectHeader lspaObjHeader;

        private boolean bLFlag;
        private int iExcludeAny;
        private boolean bIsExcludeAnySet = false;
        private int iIncludeAny;
        private boolean bIsIncludeAnySet = false;
        private int iIncludeAll;
        private boolean bIsIncludeAllSet = false;
        private byte cSetupPriority;
        private boolean bIsSetupPrioritySet = false;
        private byte cHoldPriority;
        private boolean bIsHoldPrioritySet = false;
        private LinkedList<PcepValueType> llOptionalTlv;

        private boolean bIsPFlagSet = false;
        private boolean bPFlag;

        private boolean bIsIFlagSet = false;
        private boolean bIFlag;

        @Override
        public PcepLspaObject build() throws PcepParseException {

            PcepObjectHeader lspaObjHeader = this.bIsHeaderSet ? this.lspaObjHeader : DEFAULT_LSPA_OBJECT_HEADER;

            if (!this.bIsExcludeAnySet) {
                throw new PcepParseException("ExcludeAny NOT Set while building PcepLspaObject.");
            }
            if (!this.bIsIncludeAnySet) {
                throw new PcepParseException("IncludeAny NOT Set while building PcepLspaObject.");
            }
            if (!this.bIsIncludeAllSet) {
                throw new PcepParseException("IncludeAll NOT Set while building PcepLspaObject.");
            }
            if (!this.bIsSetupPrioritySet) {
                throw new PcepParseException("Setup Priority NOT Set while building PcepLspaObject.");
            }
            if (!this.bIsHoldPrioritySet) {
                throw new PcepParseException("Hold Priority NOT Set while building PcepLspaObject.");
            }

            if (bIsPFlagSet) {
                lspaObjHeader.setPFlag(bPFlag);
            }

            if (bIsIFlagSet) {
                lspaObjHeader.setIFlag(bIFlag);
            }

            return new PcepLspaObjectVer1(lspaObjHeader, bLFlag, iExcludeAny, iIncludeAny, iIncludeAll, cSetupPriority,
                    cHoldPriority, llOptionalTlv);
        }

        @Override
        public PcepObjectHeader getLspaObjHeader() {
            return this.lspaObjHeader;
        }

        @Override
        public Builder setLspaObjHeader(PcepObjectHeader obj) {
            this.lspaObjHeader = obj;
            this.bIsHeaderSet = true;
            return this;
        }

        @Override
        public boolean getLFlag() {
            return this.bLFlag;
        }

        @Override
        public Builder setLFlag(boolean value) {
            this.bLFlag = value;
            return this;
        }

        @Override
        public int getExcludeAny() {
            return this.iExcludeAny;
        }

        @Override
        public Builder setExcludeAny(int value) {
            this.iExcludeAny = value;
            this.bIsExcludeAnySet = true;
            return this;
        }

        @Override
        public int getIncludeAny() {
            return this.iIncludeAny;
        }

        @Override
        public Builder setIncludeAny(int value) {
            this.iIncludeAny = value;
            this.bIsIncludeAnySet = true;
            return this;
        }

        @Override
        public int getIncludeAll() {
            return this.iIncludeAll;
        }

        @Override
        public Builder setIncludeAll(int value) {
            this.iIncludeAll = value;
            this.bIsIncludeAllSet = true;
            return this;
        }

        @Override
        public byte getSetupPriority() {
            return this.cSetupPriority;
        }

        @Override
        public Builder setSetupPriority(byte value) {
            this.cSetupPriority = value;
            this.bIsSetupPrioritySet = true;
            return this;
        }

        @Override
        public byte getHoldPriority() {
            return this.cHoldPriority;
        }

        @Override
        public Builder setHoldPriority(byte value) {
            this.cHoldPriority = value;
            this.bIsHoldPrioritySet = true;
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
                .add("LFlag", bLFlag)
                .add("SetupPriority", cSetupPriority)
                .add("HoldPriority", cHoldPriority)
                .add("IncludeAll", iIncludeAll)
                .add("IncludeAny", iIncludeAny)
                .add("ExcludeAny", iExcludeAny)
                .add("OptionalTlvList", llOptionalTlv)
                .toString();
    }
}
