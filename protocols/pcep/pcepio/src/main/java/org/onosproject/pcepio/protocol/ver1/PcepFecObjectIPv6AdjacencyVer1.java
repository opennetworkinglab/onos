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
import org.onosproject.pcepio.protocol.PcepFecObjectIPv6Adjacency;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides Pcep Fec Object IPv6 Adjacency object.
 */
public class PcepFecObjectIPv6AdjacencyVer1 implements PcepFecObjectIPv6Adjacency {

    /*
     * ref : draft-zhao-pce-pcep-extension-for-pce-controller-01 , section : 7.5
     *
            0                   1                   2                   3
            0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           |                                                               |
           //              Local IPv6 address (16 bytes)                   //
           |                                                               |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           |                                                               |
           //              Remote IPv6 address (16 bytes)                  //
           |                                                               |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                      FEC Object-Type is 4 IPv6 Adjacency
     */
    protected static final Logger log = LoggerFactory.getLogger(PcepFecObjectIPv6AdjacencyVer1.class);

    public static final byte FEC_OBJ_TYPE = 4;
    public static final byte FEC_OBJ_CLASS = (byte) 226;
    public static final byte FEC_OBJECT_VERSION = 1;
    public static final short FEC_OBJ_MINIMUM_LENGTH = 36;
    public static final int MINIMUM_COMMON_HEADER_LENGTH = 4;
    public static final int IPV6_ADDRESS_LENGTH = 16;

    static final PcepObjectHeader DEFAULT_FEC_OBJECT_HEADER = new PcepObjectHeader(FEC_OBJ_CLASS, FEC_OBJ_TYPE,
            PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED, FEC_OBJ_MINIMUM_LENGTH);

    private PcepObjectHeader fecObjHeader;
    private byte[] localIPv6Address = new byte[IPV6_ADDRESS_LENGTH];
    private byte[] remoteIPv6Address = new byte[IPV6_ADDRESS_LENGTH];

    /**
     * Constructor to initialize parameters for PCEP fec object.
     *
     * @param fecObjHeader fec object header
     * @param localIPv6Address local IPv6 address
     * @param remoteIPv6Address remote IPv6 address
     */
    public PcepFecObjectIPv6AdjacencyVer1(PcepObjectHeader fecObjHeader, byte[] localIPv6Address,
            byte[] remoteIPv6Address) {
        this.fecObjHeader = fecObjHeader;
        this.localIPv6Address = localIPv6Address;
        this.remoteIPv6Address = remoteIPv6Address;
    }

    /**
     * Sets Object Header.
     *
     * @param obj object header
     */
    public void setFecIpv4ObjHeader(PcepObjectHeader obj) {
        this.fecObjHeader = obj;
    }

    @Override
    public byte[] getLocalIPv6Address() {
        return this.localIPv6Address;
    }

    @Override
    public void seLocalIPv6Address(byte[] value) {
        this.localIPv6Address = value;
    }

    @Override
    public byte[] getRemoteIPv6Address() {
        return this.remoteIPv6Address;
    }

    @Override
    public void seRemoteIPv6Address(byte[] value) {
        this.remoteIPv6Address = value;
    }

    /**
     * Reads channel buffer and Returns object of PcepFecObjectIPv6Adjacency.
     *
     * @param cb of channel buffer
     * @return object of PcepFecObjectIPv6Adjacency
     * @throws PcepParseException when fails tp read from channel buffer
     */
    public static PcepFecObjectIPv6Adjacency read(ChannelBuffer cb) throws PcepParseException {

        PcepObjectHeader fecObjHeader;
        byte[] localIPv6Address = new byte[IPV6_ADDRESS_LENGTH];
        byte[] remoteIPv6Address = new byte[IPV6_ADDRESS_LENGTH];
        fecObjHeader = PcepObjectHeader.read(cb);
        cb.readBytes(localIPv6Address, 0, IPV6_ADDRESS_LENGTH);
        cb.readBytes(remoteIPv6Address, 0, IPV6_ADDRESS_LENGTH);
        return new PcepFecObjectIPv6AdjacencyVer1(fecObjHeader, localIPv6Address, remoteIPv6Address);
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {

        int objStartIndex = cb.writerIndex();

        //write common header
        int objLenIndex = fecObjHeader.write(cb);
        cb.writeBytes(localIPv6Address);
        cb.writeBytes(remoteIPv6Address);
        //now write FEC IPv6 Adjacency Object Length
        cb.setShort(objLenIndex, (short) (cb.writerIndex() - objStartIndex));
        return cb.writerIndex();
    }

    /**
     * Builder class for PCEP fec object IPv6 Adjacency.
     */
    public static class Builder implements PcepFecObjectIPv6Adjacency.Builder {
        private boolean bIsHeaderSet = false;
        private boolean bIsLocalIPv6Addressset = false;
        private boolean bIsRemoteIPv6Addressset = false;

        private PcepObjectHeader fecObjHeader;
        byte[] localIPv6Address = new byte[IPV6_ADDRESS_LENGTH];
        byte[] remoteIPv6Address = new byte[IPV6_ADDRESS_LENGTH];

        private boolean bIsPFlagSet = false;
        private boolean bPFlag;

        private boolean bIsIFlagSet = false;
        private boolean bIFlag;

        @Override
        public PcepFecObjectIPv6Adjacency build() throws PcepParseException {
            PcepObjectHeader fecObjHeader = this.bIsHeaderSet ? this.fecObjHeader : DEFAULT_FEC_OBJECT_HEADER;

            if (!this.bIsLocalIPv6Addressset) {
                throw new PcepParseException(
                        "Local IPv6 Address not set while building PcepFecObjectIPv6Adjacency object.");
            }
            if (!this.bIsRemoteIPv6Addressset) {
                throw new PcepParseException(
                        "Remote IPv6 Address not set while building PcepFecObjectIPv6Adjacency object.");
            }
            if (bIsPFlagSet) {
                fecObjHeader.setPFlag(bPFlag);
            }
            if (bIsIFlagSet) {
                fecObjHeader.setIFlag(bIFlag);
            }
            return new PcepFecObjectIPv6AdjacencyVer1(fecObjHeader, this.localIPv6Address, this.remoteIPv6Address);
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
        public PcepObjectHeader getFecIpv6AdjacencyObjHeader() {
            return this.fecObjHeader;
        }

        @Override
        public Builder setFecIpv6AdjacencyObjHeader(PcepObjectHeader obj) {
            this.fecObjHeader = obj;
            this.bIsHeaderSet = true;
            return this;
        }

        @Override
        public byte[] getLocalIPv6Address() {
            return this.localIPv6Address;
        }

        @Override
        public Builder setLocalIPv6Address(byte[] value) {
            this.localIPv6Address = value;
            this.bIsLocalIPv6Addressset = true;
            return this;
        }

        @Override
        public byte[] getRemoteIPv6Address() {
            return this.remoteIPv6Address;
        }

        @Override
        public Builder setRemoteIPv6Address(byte[] value) {
            this.remoteIPv6Address = value;
            this.bIsRemoteIPv6Addressset = true;
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
                .add("localIPv6Address", localIPv6Address)
                .add("remoteIPv6Address: ", remoteIPv6Address)
                .toString();
    }
}
