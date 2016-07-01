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
import org.onosproject.pcepio.protocol.PcepBandwidthObject;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PcepBandwidthObject.
 */
public class PcepBandwidthObjectVer1 implements PcepBandwidthObject {

    /*
     *    RFC : 5440 , section : 7.7.
          0                   1                   2                   3
          0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
          +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
          |                         Bandwidth                             |
          +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                         The BANDWIDTH Object format
     */

    protected static final Logger log = LoggerFactory.getLogger(PcepBandwidthObjectVer1.class);
    /*
     *  Requested bandwidth: BANDWIDTH Object-Type is 1.
        Bandwidth of an existing TE LSP for which a re-optimization is
        requested. BANDWIDTH Object-Type is 2.
     */
    //Right now handling type 1
    public static final byte BANDWIDTH_OBJ_TYPE = 1;
    public static final byte BANDWIDTH_OBJ_CLASS = 5;
    public static final byte BANDWIDTH_OBJECT_VERSION = 1;
    public static final int NO_OF_BITS = 8;
    public static final short BANDWIDTH_OBJ_MINIMUM_LENGTH = 8;

    static final PcepObjectHeader DEFAULT_BANDWIDTH_OBJECT_HEADER = new PcepObjectHeader(BANDWIDTH_OBJ_CLASS,
            BANDWIDTH_OBJ_TYPE, PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED,
            BANDWIDTH_OBJ_MINIMUM_LENGTH);

    private PcepObjectHeader bandwidthObjHeader;
    private float iBandwidth;

    /**
     * Constructor to bandwidth object header and bandwidth.
     *
     * @param bandwidthObjHeader bandwidth object header
     * @param iBandwidth bandwidth value
     */
    public PcepBandwidthObjectVer1(PcepObjectHeader bandwidthObjHeader, float iBandwidth) {
        this.bandwidthObjHeader = bandwidthObjHeader;
        this.iBandwidth = iBandwidth;
    }

    /**
     * Constructor to initialize bandwidth.
     *
     * @param iBandwidth bandwidth value
     */
    public PcepBandwidthObjectVer1(float iBandwidth) {
        this.bandwidthObjHeader = DEFAULT_BANDWIDTH_OBJECT_HEADER;
        this.iBandwidth = iBandwidth;
    }

    /**
     * Returns Object Header.
     *
     * @return bandwidthObjHeader
     */
    public PcepObjectHeader getBandwidthObjHeader() {
        return this.bandwidthObjHeader;
    }

    /**
     * Sets Object Header.
     *
     * @param obj bandwidth object header
     */
    public void setBandwidthObjHeader(PcepObjectHeader obj) {
        this.bandwidthObjHeader = obj;
    }

    @Override
    public float getBandwidth() {
        return this.iBandwidth;
    }

    @Override
    public void setBandwidth(float iBandwidth) {
        this.iBandwidth = iBandwidth;
    }

    /**
     * Reads from channel buffer and returns object of PcepBandwidthObject.
     *
     * @param cb channel buffer to parse
     * @return object of PcepBandwidthObject
     * @throws PcepParseException while parsing channel buffer
     */
    public static PcepBandwidthObject read(ChannelBuffer cb) throws PcepParseException {

        PcepObjectHeader bandwidthObjHeader;
        float bandwidth;

        bandwidthObjHeader = PcepObjectHeader.read(cb);
        bandwidth = ieeeToFloatRead(cb.readInt()) * NO_OF_BITS;

        return new PcepBandwidthObjectVer1(bandwidthObjHeader, bandwidth);
    }

    /**
     * Parse the IEEE floating point notation and returns it in normal float.
     *
     * @param iVal IEEE floating point number
     * @return normal float
     */
    public static float ieeeToFloatRead(int iVal) {

        return Float.intBitsToFloat(iVal);
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {

        //write Object header
        int objStartIndex = cb.writerIndex();
        int objLenIndex = bandwidthObjHeader.write(cb);

        if (objLenIndex <= 0) {
            throw new PcepParseException("Failed to write bandwidth object header. Index " + objLenIndex);
        }

        //Convert to bytes per second
        float bwBytes = iBandwidth / 8.0f;
        //Bytes/sec to IEEE floating format
        int bandwidth = Float.floatToIntBits(bwBytes);

        cb.writeByte(bandwidth >>> 24);
        cb.writeByte(bandwidth >> 16 & 0xff);
        cb.writeByte(bandwidth >> 8 & 0xff);
        cb.writeByte(bandwidth & 0xff);

        short hLength = (short) (cb.writerIndex() - objStartIndex);
        cb.setShort(objLenIndex, hLength);
        //will be helpful during print().
        bandwidthObjHeader.setObjLen(hLength);

        return cb.writerIndex() - objStartIndex;
    }

    /**
     * builder class for PCEP bandwidth object.
     */
    public static class Builder implements PcepBandwidthObject.Builder {

        private PcepObjectHeader bandwidthObjHeader;
        private boolean bIsHeaderSet = false;

        private float iBandwidth;
        private boolean bIsBandwidthSet = false;

        private boolean bPFlag;
        private boolean bIsPFlagSet = false;

        private boolean bIFlag;
        private boolean bIsIFlagSet = false;

        @Override
        public PcepBandwidthObject build() throws PcepParseException {

            PcepObjectHeader bandwidthObjHeader = this.bIsHeaderSet ? this.bandwidthObjHeader
                    : DEFAULT_BANDWIDTH_OBJECT_HEADER;

            if (bIsPFlagSet) {
                bandwidthObjHeader.setPFlag(bPFlag);
            }

            if (bIsIFlagSet) {
                bandwidthObjHeader.setIFlag(bIFlag);
            }

            if (!this.bIsBandwidthSet) {
                throw new PcepParseException("bandwidth not Set while building Bandwidth Object.");
            }

            return new PcepBandwidthObjectVer1(bandwidthObjHeader, iBandwidth);
        }

        @Override
        public float getBandwidth() {
            return this.iBandwidth;
        }

        @Override
        public PcepObjectHeader getBandwidthObjHeader() {
            return this.bandwidthObjHeader;
        }

        @Override
        public Builder setBandwidthObjHeader(PcepObjectHeader obj) {
            this.bandwidthObjHeader = obj;
            return this;
        }

        @Override
        public Builder setBandwidth(float iBandwidth) {
            this.iBandwidth = iBandwidth;
            this.bIsBandwidthSet = true;
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
                .add("BandwidthObjectHeader", bandwidthObjHeader)
                .add("Bandwidth", iBandwidth).toString();
    }
}
