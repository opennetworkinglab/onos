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
import org.onosproject.pcepio.protocol.PcepLabelRangeObject;
import org.onosproject.pcepio.types.PathSetupTypeTlv;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.onosproject.pcepio.types.PcepValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP label range object.
 */
public class PcepLabelRangeObjectVer1 implements PcepLabelRangeObject {

    /*
     * ref : draft-zhao-pce-pcep-extension-for-pce-controller-01, section : 7.2
            0                   1                   2                     3
            0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           | label type    | range size                                    |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           |                        label base                             |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           |                                                               |
           //                      Optional TLVs                           //
           |                                                               |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                               LABEL-RANGE Object
     */
    protected static final Logger log = LoggerFactory.getLogger(PcepLabelRangeObjectVer1.class);

    public static final byte LABEL_RANGE_OBJ_TYPE = 1;
    public static final byte LABEL_RANGE_OBJ_CLASS = 60; //to be defined
    public static final byte LABEL_RANGE_OBJECT_VERSION = 1;
    public static final short LABEL_RANGE_OBJ_MINIMUM_LENGTH = 12;
    public static final int MINIMUM_COMMON_HEADER_LENGTH = 4;
    //P flag and I flag must be set to 0
    static final PcepObjectHeader DEFAULT_LABELRANGE_OBJECT_HEADER = new PcepObjectHeader(LABEL_RANGE_OBJ_CLASS,
            LABEL_RANGE_OBJ_TYPE, PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED,
            LABEL_RANGE_OBJ_MINIMUM_LENGTH);

    private PcepObjectHeader labelRangeObjHeader;
    private byte labelType;
    private int rangeSize;
    private int labelBase;
    //Optional TLV
    private LinkedList<PcepValueType> llOptionalTlv;

    /**
     * Constructor to initialize parameters for PCEP label range object.
     *
     * @param labelRangeObjHeader label range object header
     * @param labelType label type
     * @param rangeSize range size
     * @param labelBase label base
     * @param llOptionalTlv list of optional tlvs
     */
    public PcepLabelRangeObjectVer1(PcepObjectHeader labelRangeObjHeader, byte labelType, int rangeSize, int labelBase,
            LinkedList<PcepValueType> llOptionalTlv) {
        this.labelRangeObjHeader = labelRangeObjHeader;
        this.labelType = labelType;
        this.rangeSize = rangeSize;
        this.llOptionalTlv = llOptionalTlv;
        this.labelBase = labelBase;
    }

    @Override
    public void setLabelRangeObjHeader(PcepObjectHeader obj) {
        this.labelRangeObjHeader = obj;
    }

    @Override
    public void setLabelType(byte labelType) {
        this.labelType = labelType;
    }

    @Override
    public void setRangeSize(int rangeSize) {
        this.rangeSize = rangeSize;
    }

    @Override
    public void setLabelBase(int labelBase) {
        this.labelBase = labelBase;
    }

    @Override
    public PcepObjectHeader getLabelRangeObjHeader() {
        return this.labelRangeObjHeader;
    }

    @Override
    public byte getLabelType() {
        return this.labelType;
    }

    @Override
    public int getRangeSize() {
        return this.rangeSize;
    }

    @Override
    public int getLabelBase() {
        return this.labelBase;
    }

    /**
     * Reads from the channel buffer and returns object of  PcepLabelRangeObject.
     *
     * @param cb of type channel buffer
     * @return object of  PcepLabelRangeObject
     * @throws PcepParseException when fails to read from channel buffer
     */
    public static PcepLabelRangeObject read(ChannelBuffer cb) throws PcepParseException {

        PcepObjectHeader labelRangeObjHeader;
        byte labelType;
        int rangeSize;
        int labelBase;

        LinkedList<PcepValueType> llOptionalTlv = new LinkedList<>();

        labelRangeObjHeader = PcepObjectHeader.read(cb);

        //take only LabelRangeObject buffer.
        ChannelBuffer tempCb = cb.readBytes(labelRangeObjHeader.getObjLen() - MINIMUM_COMMON_HEADER_LENGTH);
        int temp = 0;
        temp = tempCb.readInt();
        rangeSize = temp & 0x00FFFFFF;
        labelType = (byte) (temp >> 24);
        labelBase = tempCb.readInt();
        llOptionalTlv = parseOptionalTlv(tempCb);
        return new PcepLabelRangeObjectVer1(labelRangeObjHeader, labelType, rangeSize, labelBase, llOptionalTlv);
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {

        int objStartIndex = cb.writerIndex();

        //write common header
        int objLenIndex = labelRangeObjHeader.write(cb);
        int temp = 0;
        temp = labelType;
        temp = temp << 24;
        temp = temp | rangeSize;
        cb.writeInt(temp);

        // Add optional TLV
        if (!packOptionalTlv(cb)) {
            throw new PcepParseException("Error while writing Optional tlv.");
        }

        //now write LabelRange Object Length
        cb.setShort(objLenIndex, (short) (cb.writerIndex() - objStartIndex));
        return cb.writerIndex() - objStartIndex;
    }

    /**
     * Returns list of optional tlvs.
     *
     * @param cb of type channle buffer
     * @return list of optional tlvs
     * @throws PcepParseException whne fails to parse list of optional tlvs
     */
    public static LinkedList<PcepValueType> parseOptionalTlv(ChannelBuffer cb) throws PcepParseException {

        LinkedList<PcepValueType> llOutOptionalTlv = new LinkedList<>();

        while (MINIMUM_COMMON_HEADER_LENGTH <= cb.readableBytes()) {

            PcepValueType tlv;
            int iValue;
            short hType = cb.readShort();
            short hLength = cb.readShort();

            switch (hType) {

            case PathSetupTypeTlv.TYPE:
                iValue = cb.readInt();
                tlv = new PathSetupTypeTlv(iValue);
                break;

            default:
                throw new PcepParseException("Unsupported TLV in LabelRange Object.");
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
        return llOutOptionalTlv;
    }

    /**
     * Pack optional tlvs.
     *
     * @param cb of channel buffer
     * @return true
     */
    protected boolean packOptionalTlv(ChannelBuffer cb) {

        ListIterator<PcepValueType> listIterator = llOptionalTlv.listIterator();

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
        return true;
    }

    /**
     * Builder class for PCEP label range object.
     */
    public static class Builder implements PcepLabelRangeObject.Builder {
        private boolean bIsHeaderSet = false;
        private boolean bIsLabelType = false;
        private boolean bIsRangeSize = false;
        private boolean bIsLabelBase = false;

        byte labelType;
        int rangeSize;
        int labelBase;
        private boolean bIsPFlagSet = false;
        private boolean bPFlag;

        private boolean bIsIFlagSet = false;
        private boolean bIFlag;
        private PcepObjectHeader labelRangeObjHeader;

        LinkedList<PcepValueType> llOptionalTlv = new LinkedList<>();

        @Override
        public PcepLabelRangeObject build() throws PcepParseException {
            PcepObjectHeader labelRangeObjHeader = this.bIsHeaderSet ? this.labelRangeObjHeader
                    : DEFAULT_LABELRANGE_OBJECT_HEADER;

            if (!this.bIsLabelType) {
                throw new PcepParseException("LabelType NOT Set while building label range object.");
            }

            if (!this.bIsRangeSize) {
                throw new PcepParseException("RangeSize NOT Set while building label range object.");
            }

            if (!this.bIsLabelBase) {
                throw new PcepParseException("LabelBase NOT Set while building label range object.");
            }

            if (bIsPFlagSet) {
                labelRangeObjHeader.setPFlag(bPFlag);
            }

            if (bIsIFlagSet) {
                labelRangeObjHeader.setIFlag(bIFlag);
            }
            return new PcepLabelRangeObjectVer1(labelRangeObjHeader, this.labelType, this.rangeSize, this.labelBase,
                    this.llOptionalTlv);
        }

        @Override
        public PcepObjectHeader getLabelRangeObjHeader() {
            return this.labelRangeObjHeader;
        }

        @Override
        public Builder setLabelRangeObjHeader(PcepObjectHeader obj) {
            this.labelRangeObjHeader = obj;
            this.bIsHeaderSet = true;
            return this;
        }

        @Override
        public byte getLabelType() {
            return this.labelType;
        }

        @Override
        public Builder setLabelType(byte labelType) {
            this.labelType = labelType;
            this.bIsLabelType = true;
            return this;
        }

        @Override
        public int getRangeSize() {
            return this.rangeSize;
        }

        @Override
        public Builder setRangeSize(int rangeSize) {
            this.rangeSize = rangeSize;
            this.bIsRangeSize = true;
            return this;
        }

        @Override
        public int getLabelBase() {
            return this.labelBase;
        }

        @Override
        public Builder setLabelBase(int labelBase) {
            this.labelBase = labelBase;
            this.bIsLabelBase = true;
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
                .add("LabelType", labelType)
                .add("rangeSize", rangeSize)
                .add("labelBase", labelBase)
                .add("optionalTlvList", llOptionalTlv)
                .toString();
    }
}
