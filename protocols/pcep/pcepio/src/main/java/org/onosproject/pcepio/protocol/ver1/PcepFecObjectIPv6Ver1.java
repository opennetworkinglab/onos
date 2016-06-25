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
import org.onosproject.pcepio.protocol.PcepFecObjectIPv6;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides Pcep Fec Object IPv6 object.
 */
public class PcepFecObjectIPv6Ver1 implements PcepFecObjectIPv6 {

    /*
     * ref : draft-zhao-pce-pcep-extension-for-pce-controller-01 , section : 7.5
     *
        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                                                               |
       //                     IPv6 Node ID (16 bytes)                  //
       |                                                               |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                       FEC Object-Type is 2 IPv6 Node ID
     */
    protected static final Logger log = LoggerFactory.getLogger(PcepFecObjectIPv6Ver1.class);

    public static final byte FEC_OBJ_TYPE = 2;
    public static final byte FEC_OBJ_CLASS = (byte) 226;
    public static final byte FEC_OBJECT_VERSION = 1;
    public static final short FEC_OBJ_MINIMUM_LENGTH = 20;
    public static final int MINIMUM_COMMON_HEADER_LENGTH = 4;
    public static final int IPV6_ADDRESS_LENGTH = 16;

    static final PcepObjectHeader DEFAULT_FEC_OBJECT_HEADER = new PcepObjectHeader(FEC_OBJ_CLASS, FEC_OBJ_TYPE,
            PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED, FEC_OBJ_MINIMUM_LENGTH);

    private PcepObjectHeader fecObjHeader;
    private byte[] nodeID = new byte[IPV6_ADDRESS_LENGTH];

    /**
     * Constructor to initialize parameters for PCEP fec object.
     *
     * @param fecObjHeader Fec object header
     * @param nodeID node ID
     */
    public PcepFecObjectIPv6Ver1(PcepObjectHeader fecObjHeader, byte[] nodeID) {
        this.fecObjHeader = fecObjHeader;
        this.nodeID = nodeID;
    }

    /**
     * Sets the Object header.
     *
     * @param obj object header
     */
    public void setFecIpv6ObjHeader(PcepObjectHeader obj) {
        this.fecObjHeader = obj;
    }

    @Override
    public void setNodeID(byte[] nodeID) {
        this.nodeID = nodeID;
    }

    /**
     * Returns object header.
     *
     * @return fec Object Header
     */
    public PcepObjectHeader getFecIpv6ObjHeader() {
        return this.fecObjHeader;
    }

    @Override
    public byte[] getNodeID() {
        return this.nodeID;
    }

    /**
     * reads the channel buffer and returns object of PcepFecObjectIPv6.
     *
     * @param cb of channel buffer.
     * @return object of PcepFecObjectIPv6
     * @throws PcepParseException when fails to read from channel buffer
     */
    public static PcepFecObjectIPv6 read(ChannelBuffer cb) throws PcepParseException {

        PcepObjectHeader fecObjHeader;
        byte[] nodeID = new byte[IPV6_ADDRESS_LENGTH];
        fecObjHeader = PcepObjectHeader.read(cb);
        cb.readBytes(nodeID, 0, IPV6_ADDRESS_LENGTH);
        return new PcepFecObjectIPv6Ver1(fecObjHeader, nodeID);
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {

        int objStartIndex = cb.writerIndex();

        //write common header
        int objLenIndex = fecObjHeader.write(cb);
        cb.writeBytes(nodeID);

        //now write FEC IPv4 Object Length
        cb.setShort(objLenIndex, (short) (cb.writerIndex() - objStartIndex));
        return cb.writerIndex();
    }

    /**
     * Builder class for PCEP fec object IPv6.
     */
    public static class Builder implements PcepFecObjectIPv6.Builder {
        private boolean bIsHeaderSet = false;
        private boolean bIsNodeIdset = false;

        private PcepObjectHeader fecObjHeader;
        private byte[] nodeID = new byte[IPV6_ADDRESS_LENGTH];

        private boolean bIsPFlagSet = false;
        private boolean bPFlag;

        private boolean bIsIFlagSet = false;
        private boolean bIFlag;

        @Override
        public PcepFecObjectIPv6 build() throws PcepParseException {
            PcepObjectHeader fecObjHeader = this.bIsHeaderSet ? this.fecObjHeader : DEFAULT_FEC_OBJECT_HEADER;

            if (!this.bIsNodeIdset) {
                throw new PcepParseException(" NodeID not set while building PcepFecObjectIPv6 object.");
            }
            if (bIsPFlagSet) {
                fecObjHeader.setPFlag(bPFlag);
            }
            if (bIsIFlagSet) {
                fecObjHeader.setIFlag(bIFlag);
            }
            return new PcepFecObjectIPv6Ver1(fecObjHeader, this.nodeID);
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
        public PcepObjectHeader getFecIpv6ObjHeader() {
            return this.fecObjHeader;
        }

        @Override
        public Builder setFecIpv6ObjHeader(PcepObjectHeader obj) {
            this.fecObjHeader = obj;
            this.bIsHeaderSet = true;
            return this;
        }

        @Override
        public byte[] getNodeID() {
            return this.nodeID;
        }

        @Override
        public Builder setNodeID(byte[] value) {
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
                .add("NodeID: ", nodeID)
                .toString();
    }
}
