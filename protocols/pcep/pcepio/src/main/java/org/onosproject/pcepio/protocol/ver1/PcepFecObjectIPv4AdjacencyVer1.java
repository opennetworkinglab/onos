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
import org.onosproject.pcepio.protocol.PcepFecObjectIPv4Adjacency;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP fec Object IPv4 Adjacency object.
 */
public class PcepFecObjectIPv4AdjacencyVer1 implements PcepFecObjectIPv4Adjacency {

    /*
     * ref : draft-zhao-pce-pcep-extension-for-pce-controller-01 , section : 7.5
     *
            0                   1                   2                   3
            0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           |                    Local IPv4 address                         |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           |                    Remote IPv4 address                        |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                      FEC Object-Type is 3 IPv4 Adjacency
     */
    protected static final Logger log = LoggerFactory.getLogger(PcepFecObjectIPv4AdjacencyVer1.class);

    public static final byte FEC_OBJ_TYPE = 3;
    public static final byte FEC_OBJ_CLASS = (byte) 226;
    public static final byte FEC_OBJECT_VERSION = 1;
    public static final short FEC_OBJ_MINIMUM_LENGTH = 12;
    public static final int MINIMUM_COMMON_HEADER_LENGTH = 4;

    public static final PcepObjectHeader DEFAULT_FEC_OBJECT_HEADER = new PcepObjectHeader(FEC_OBJ_CLASS, FEC_OBJ_TYPE,
            PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED, FEC_OBJ_MINIMUM_LENGTH);

    private PcepObjectHeader fecObjHeader;
    private int localIPv4Address;
    private int remoteIPv4Address;

    /**
     * Constructor to initialize parameters for PCEP fec object .
     *
     * @param fecObjHeader FEC Object header
     * @param localIPv4Address Local IPv4 Address
     * @param remoteIPv4Address Remote IPv4 Address
     */
    public PcepFecObjectIPv4AdjacencyVer1(PcepObjectHeader fecObjHeader, int localIPv4Address, int remoteIPv4Address) {
        this.fecObjHeader = fecObjHeader;
        this.localIPv4Address = localIPv4Address;
        this.remoteIPv4Address = remoteIPv4Address;
    }

    /**
     * Sets Object header.
     *
     * @param obj Pcep fec Object Header
     */
    public void setFecIpv4ObjHeader(PcepObjectHeader obj) {
        this.fecObjHeader = obj;
    }

    @Override
    public int getLocalIPv4Address() {
        return this.localIPv4Address;
    }

    @Override
    public void seLocalIPv4Address(int value) {
        this.localIPv4Address = value;
    }

    @Override
    public int getRemoteIPv4Address() {
        return this.remoteIPv4Address;
    }

    @Override
    public void seRemoteIPv4Address(int value) {
        this.remoteIPv4Address = value;
    }

    /**
     * Reads from channel buffer and Returns object of PcepFecObjectIPv4Adjacency.
     *
     * @param cb of channel buffer.
     * @return object of PcepFecObjectIPv4Adjacency
     * @throws PcepParseException when fails to read from channel buffer
     */
    public static PcepFecObjectIPv4Adjacency read(ChannelBuffer cb) throws PcepParseException {

        PcepObjectHeader fecObjHeader;
        int localIPv4Address;
        int remoteIPv4Address;

        fecObjHeader = PcepObjectHeader.read(cb);

        //take only FEC IPv4 Adjacency Object buffer.
        ChannelBuffer tempCb = cb.readBytes(fecObjHeader.getObjLen() - MINIMUM_COMMON_HEADER_LENGTH);
        localIPv4Address = tempCb.readInt();
        remoteIPv4Address = tempCb.readInt();

        return new PcepFecObjectIPv4AdjacencyVer1(fecObjHeader, localIPv4Address, remoteIPv4Address);
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {

        int objStartIndex = cb.writerIndex();

        //Write common header
        int objLenIndex = fecObjHeader.write(cb);
        cb.writeInt(localIPv4Address);
        cb.writeInt(remoteIPv4Address);

        //Now write FEC IPv4 Adjacency Object Length
        cb.setShort(objLenIndex, (short) (cb.writerIndex() - objStartIndex));
        return cb.writerIndex();
    }

    /**
     * Builder class for PCEP fec object IPv4 Adjacency.
     */
    public static class Builder implements PcepFecObjectIPv4Adjacency.Builder {
        private boolean bIsHeaderSet = false;
        private boolean bIsLocalIPv4Addressset = false;
        private boolean bIsRemoteIPv4Addressset = false;

        private PcepObjectHeader fecObjHeader;
        int localIPv4Address;
        int remoteIPv4Address;

        private boolean bIsPFlagSet = false;
        private boolean bPFlag;

        private boolean bIsIFlagSet = false;
        private boolean bIFlag;

        @Override
        public PcepFecObjectIPv4Adjacency build() throws PcepParseException {
            PcepObjectHeader fecObjHeader = this.bIsHeaderSet ? this.fecObjHeader : DEFAULT_FEC_OBJECT_HEADER;

            if (!this.bIsLocalIPv4Addressset) {
                throw new PcepParseException(
                        "Local IPv4 Address not set while building PcepFecObjectIPv4Adjacency object.");
            }

            if (!this.bIsRemoteIPv4Addressset) {
                throw new PcepParseException(
                        " Remote IPv4 Address not set while building PcepFecObjectIPv4Adjacency object.");
            }

            if (bIsPFlagSet) {
                fecObjHeader.setPFlag(bPFlag);
            }

            if (bIsIFlagSet) {
                fecObjHeader.setIFlag(bIFlag);
            }
            return new PcepFecObjectIPv4AdjacencyVer1(fecObjHeader, this.localIPv4Address, this.remoteIPv4Address);
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
        public PcepObjectHeader getFecIpv4AdjacencyObjHeader() {
            return this.fecObjHeader;
        }

        @Override
        public Builder setFecIpv4AdjacencyObjHeader(PcepObjectHeader obj) {
            this.fecObjHeader = obj;
            this.bIsHeaderSet = true;
            return this;
        }

        @Override
        public int getLocalIPv4Address() {
            return this.localIPv4Address;
        }

        @Override
        public Builder seLocalIPv4Address(int value) {
            this.localIPv4Address = value;
            this.bIsLocalIPv4Addressset = true;
            return this;
        }

        @Override
        public int getRemoteIPv4Address() {
            return this.remoteIPv4Address;
        }

        @Override
        public Builder seRemoteIPv4Address(int value) {
            this.remoteIPv4Address = value;
            this.bIsRemoteIPv4Addressset = true;
            return this;
        }

    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
    }

    @Override
    public int getType() {
        return FEC_OBJ_TYPE;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("fecObjHeader", fecObjHeader)
                .add("localIPv4Address", localIPv4Address)
                .add("remoteIPv4Address", remoteIPv4Address).toString();
    }
}
