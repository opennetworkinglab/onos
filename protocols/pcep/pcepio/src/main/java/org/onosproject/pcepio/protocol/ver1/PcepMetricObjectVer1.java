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
import org.onosproject.pcepio.protocol.PcepMetricObject;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP metric object.
 */
public class PcepMetricObjectVer1 implements PcepMetricObject {

    /*
     METRIC Object Body Format.

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |          Reserved             |    Flags  |C|B|       T       |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                          metric-value                         |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    protected static final Logger log = LoggerFactory.getLogger(PcepMetricObjectVer1.class);

    public static final byte METRIC_OBJ_TYPE = 1;
    public static final byte METRIC_OBJ_CLASS = 6;
    public static final byte METRIC_OBJECT_VERSION = 1;
    public static final short METRIC_OBJ_MINIMUM_LENGTH = 12;
    public static final int OBJECT_HEADER_LENGTH = 4;
    public static final int IFLAG_SHIFT_VALUE = 9;
    public static final int BTYPE_SHIFT_VALUE = 8;
    public static final int CFLAG_SET = 1;
    public static final int CFLAG_RESET = 0;
    public static final int BFLAG_SET = 1;
    public static final int BFLAG_RESET = 0;
    public static final byte CFLAG_CHECK = 0x02;

    public static final byte IGP_METRIC = 0x01;
    public static final byte TE_METRIC = 0x02;
    public static final byte HOP_COUNT_METRIC = 0x03;

    static final PcepObjectHeader DEFAULT_METRIC_OBJECT_HEADER = new PcepObjectHeader(METRIC_OBJ_CLASS,
            METRIC_OBJ_TYPE, PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED,
            METRIC_OBJ_MINIMUM_LENGTH);

    private PcepObjectHeader metricObjHeader;
    private int iMetricVal;
    private byte yFlag; // 6-flags
    private boolean bCFlag;
    private boolean bBFlag;
    private byte bType;

    /**
     * Default constructor.
     */
    public PcepMetricObjectVer1() {
        this.metricObjHeader = null;
        this.iMetricVal = 0;
        this.yFlag = 0;
        this.bCFlag = false;
        this.bBFlag = false;
        this.bType = 0;

    }

    /**
     * Constructor to initialize all member variables.
     *
     * @param metricObjHeader metric object header
     * @param iMetricVal metric value
     * @param yFlag Y flag
     * @param bCFlag C flag
     * @param bBFlag B flag
     * @param bType Type value
     */
    public PcepMetricObjectVer1(PcepObjectHeader metricObjHeader, int iMetricVal, byte yFlag, boolean bCFlag,
            boolean bBFlag, byte bType) {

        this.metricObjHeader = metricObjHeader;
        this.iMetricVal = iMetricVal;
        this.yFlag = yFlag;
        this.bCFlag = bCFlag;
        this.bBFlag = bBFlag;
        this.bType = bType;

    }

    @Override
    public void setMetricVal(int value) {
        this.iMetricVal = value;

    }

    @Override
    public int getMetricVal() {
        return this.iMetricVal;
    }

    @Override
    public byte getYFlag() {
        return this.yFlag;
    }

    @Override
    public void setYFlag(byte value) {
        this.yFlag = value;
    }

    @Override
    public boolean getCFlag() {
        return this.bCFlag;
    }

    @Override
    public void setCFlag(boolean value) {
        this.bCFlag = value;
    }

    @Override
    public boolean getBFlag() {
        return this.bBFlag;
    }

    @Override
    public void setBFlag(boolean value) {
        this.bBFlag = value;
    }

    @Override
    public byte getBType() {
        return this.bType;
    }

    @Override
    public void setBType(byte value) {
        this.bType = value;
    }

    /**
     * Sets metric Object Header.
     *
     * @param obj metric object header
     */
    public void setMetricObjHeader(PcepObjectHeader obj) {
        this.metricObjHeader = obj;
    }

    /**
     * Returns metric Object Header.
     *
     * @return metricObjHeader
     */
    public PcepObjectHeader getMetricObjHeader() {
        return this.metricObjHeader;
    }

    /**
     * Reads from channel buffer and returns object of PcepMetricObject.
     *
     * @param cb of channel buffer.
     * @return object of PcepMetricObject
     * @throws PcepParseException when metric object is not present in channel buffer
     */
    public static PcepMetricObject read(ChannelBuffer cb) throws PcepParseException {

        log.debug("MetricObject::read");
        PcepObjectHeader metricObjHeader;
        int iMetricVal;
        byte yFlag; // 6-flags
        boolean bCFlag;
        boolean bBFlag;
        byte bType;

        metricObjHeader = PcepObjectHeader.read(cb);

        if (metricObjHeader.getObjClass() != METRIC_OBJ_CLASS) {
            throw new PcepParseException("This object is not a Metric Object. Object Class: "
                    + metricObjHeader.getObjClass());
        }

        //take only metric buffer.
        ChannelBuffer tempCb = cb.readBytes(metricObjHeader.getObjLen() - OBJECT_HEADER_LENGTH);

        tempCb.readShort();
        yFlag = tempCb.readByte();
        bType = tempCb.readByte();
        bCFlag = (yFlag & CFLAG_CHECK) == CFLAG_CHECK;
        bBFlag = (yFlag & BFLAG_SET) == BFLAG_SET;
        iMetricVal = tempCb.readInt();

        return new PcepMetricObjectVer1(metricObjHeader, iMetricVal, yFlag, bCFlag, bBFlag, bType);
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {
        //write Object header
        int objStartIndex = cb.writerIndex();

        int objLenIndex = metricObjHeader.write(cb);

        if (objLenIndex <= 0) {
            throw new PcepParseException("Error: ObjectLength is " + objLenIndex);
        }

        int iFlag = (bCFlag) ? CFLAG_SET : CFLAG_RESET;
        int iTemp = iFlag << IFLAG_SHIFT_VALUE;
        iFlag = (bBFlag) ? BFLAG_SET : BFLAG_RESET;
        iTemp = iTemp | (iFlag << BTYPE_SHIFT_VALUE);
        iTemp = iTemp | bType;
        cb.writeInt(iTemp);
        cb.writeInt(iMetricVal);

        short hLength = (short) (cb.writerIndex() - objStartIndex);
        cb.setShort(objLenIndex, hLength);
        //will be helpful during print().
        metricObjHeader.setObjLen(hLength);
        return hLength;
    }

    /**
     * Builder class for PCEP metric object.
     */
    public static class Builder implements PcepMetricObject.Builder {

        private boolean bIsHeaderSet = false;
        private PcepObjectHeader metricObjHeader;
        private int iMetricVal;
        private boolean bIsMetricValSet = false;
        private byte yFlag; // 6-flags
        private boolean bCFlag;
        private boolean bBFlag;
        private byte bType;
        private boolean bIsbTypeSet = false;

        private boolean bIsPFlagSet = false;
        private boolean bPFlag;

        private boolean bIsIFlagSet = false;
        private boolean bIFlag;

        @Override
        public PcepMetricObject build() throws PcepParseException {

            PcepObjectHeader metricObjHeader = this.bIsHeaderSet ? this.metricObjHeader : DEFAULT_METRIC_OBJECT_HEADER;

            if (!this.bIsMetricValSet) {
                throw new PcepParseException(" Metric Value NOT Set while building PcepMetricObject.");
            }
            if (!this.bIsbTypeSet) {
                throw new PcepParseException(" Type NOT Set while building PcepMetricObject.");
            }

            if (bIsPFlagSet) {
                metricObjHeader.setPFlag(bPFlag);
            }

            if (bIsIFlagSet) {
                metricObjHeader.setIFlag(bIFlag);
            }

            return new PcepMetricObjectVer1(metricObjHeader, iMetricVal, yFlag, bCFlag, bBFlag, bType);
        }

        @Override
        public PcepObjectHeader getMetricObjHeader() {
            return this.metricObjHeader;
        }

        @Override
        public Builder setMetricObjHeader(PcepObjectHeader obj) {
            this.metricObjHeader = obj;
            this.bIsHeaderSet = true;
            return this;
        }

        @Override
        public int getMetricVal() {
            return this.iMetricVal;
        }

        @Override
        public Builder setMetricVal(int value) {
            this.iMetricVal = value;
            this.bIsMetricValSet = true;
            return this;
        }

        @Override
        public byte getYFlag() {
            return this.yFlag;
        }

        @Override
        public Builder setYFlag(byte value) {
            this.yFlag = value;
            return this;
        }

        @Override
        public boolean getCFlag() {
            return this.bCFlag;
        }

        @Override
        public Builder setCFlag(boolean value) {
            this.bCFlag = value;
            return this;
        }

        @Override
        public boolean getBFlag() {
            return this.bBFlag;
        }

        @Override
        public Builder setBFlag(boolean value) {
            this.bBFlag = value;
            return this;
        }

        @Override
        public byte getBType() {
            return this.bType;
        }

        @Override
        public Builder setBType(byte value) {
            this.bType = value;
            this.bIsbTypeSet = true;
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
                .add("MetricValue", iMetricVal)
                .add("BFlag", bBFlag)
                .add("CFlag", bCFlag)
                .add("BType", bType)
                .toString();
    }
}
