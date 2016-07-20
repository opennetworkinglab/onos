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
import org.onosproject.pcepio.protocol.PcepFecObjectIPv4;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides Pcep Fec Object IPv4 object.
 */
public class PcepFecObjectIPv4Ver1 implements PcepFecObjectIPv4 {

    /*
     * ref : draft-zhao-pce-pcep-extension-for-pce-controller-01 , section : 7.5
     *
        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                        IPv4 Node ID                           |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                       FEC Object-Type is 1 IPv4 Node ID
     */
    protected static final Logger log = LoggerFactory.getLogger(PcepFecObjectIPv4Ver1.class);

    public static final byte FEC_OBJ_TYPE = 1;
    public static final byte FEC_OBJ_CLASS = (byte) 226;
    public static final byte FEC_OBJECT_VERSION = 1;
    public static final short FEC_OBJ_MINIMUM_LENGTH = 8;
    public static final int MINIMUM_COMMON_HEADER_LENGTH = 4;

    static final PcepObjectHeader DEFAULT_FEC_OBJECT_HEADER = new PcepObjectHeader(FEC_OBJ_CLASS, FEC_OBJ_TYPE,
            PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED, FEC_OBJ_MINIMUM_LENGTH);

    private PcepObjectHeader fecObjHeader;
    private int nodeID;

    /**
     * Constructor to initialize parameters for PCEP fec object.
     *
     * @param fecObjHeader fec object header
     * @param nodeID node id
     */
    public PcepFecObjectIPv4Ver1(PcepObjectHeader fecObjHeader, int nodeID) {
        this.fecObjHeader = fecObjHeader;
        this.nodeID = nodeID;
    }

    /**
     * Sets the Object Header.
     *
     * @param obj object header
     */
    public void setFecIpv4ObjHeader(PcepObjectHeader obj) {
        this.fecObjHeader = obj;
    }

    @Override
    public void setNodeID(int nodeID) {
        this.nodeID = nodeID;
    }

    /**
     * Returns Object Header.
     *
     * @return fecObjHeader fec object header
     */
    public PcepObjectHeader getFecIpv4ObjHeader() {
        return this.fecObjHeader;
    }

    @Override
    public int getNodeID() {
        return this.nodeID;
    }

    /**
     * Reads from channel buffer and returns object of PcepFecObjectIPv4.
     *
     * @param cb of channel buffer
     * @return object of PcepFecObjectIPv4
     * @throws PcepParseException when fails to read from channel buffer
     */
    public static PcepFecObjectIPv4 read(ChannelBuffer cb) throws PcepParseException {

        PcepObjectHeader fecObjHeader;
        int nodeID;
        fecObjHeader = PcepObjectHeader.read(cb);
        nodeID = cb.readInt();
        return new PcepFecObjectIPv4Ver1(fecObjHeader, nodeID);
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {

        int objStartIndex = cb.writerIndex();

        //write common header
        int objLenIndex = fecObjHeader.write(cb);
        cb.writeInt(nodeID);

        //now write FEC IPv4 Object Length
        cb.setShort(objLenIndex, (short) (cb.writerIndex() - objStartIndex));
        return cb.writerIndex();
    }

    /**
     * Builder class for PCEP fec pobject IPv4.
     */
    public static class Builder implements PcepFecObjectIPv4.Builder {
        private boolean bIsHeaderSet = false;
        private boolean bIsNodeIdset = false;

        private PcepObjectHeader fecObjHeader;
        private int nodeID;

        private boolean bIsPFlagSet = false;
        private boolean bPFlag;

        private boolean bIsIFlagSet = false;
        private boolean bIFlag;

        @Override
        public PcepFecObjectIPv4 build() throws PcepParseException {
            PcepObjectHeader fecObjHeader = this.bIsHeaderSet ? this.fecObjHeader : DEFAULT_FEC_OBJECT_HEADER;

            if (!this.bIsNodeIdset) {
                throw new PcepParseException("NodeID not set while building PcepFecObjectIPv4 object.");
            }
            if (bIsPFlagSet) {
                fecObjHeader.setPFlag(bPFlag);
            }
            if (bIsIFlagSet) {
                fecObjHeader.setIFlag(bIFlag);
            }
            return new PcepFecObjectIPv4Ver1(fecObjHeader, this.nodeID);
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
        public PcepObjectHeader getFecIpv4ObjHeader() {
            return this.fecObjHeader;
        }

        @Override
        public Builder setFecIpv4ObjHeader(PcepObjectHeader obj) {
            this.fecObjHeader = obj;
            this.bIsHeaderSet = true;
            return this;
        }

        @Override
        public int getNodeID() {
            return this.nodeID;
        }

        @Override
        public Builder setNodeID(int value) {
            this.nodeID = value;
            this.bIsNodeIdset = true;
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
                .add("nodeID: ", nodeID)
                .toString();
    }
}
