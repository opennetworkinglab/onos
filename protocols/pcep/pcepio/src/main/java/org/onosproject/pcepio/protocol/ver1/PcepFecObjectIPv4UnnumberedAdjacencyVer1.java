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
import org.onosproject.pcepio.protocol.PcepFecObjectIPv4UnnumberedAdjacency;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides Pcep Fec Object IPv4 Unnumbered Adjacency object.
 */
public class PcepFecObjectIPv4UnnumberedAdjacencyVer1 implements PcepFecObjectIPv4UnnumberedAdjacency {

    /*
     * ref : draft-zhao-pce-pcep-extension-for-pce-controller-01 , section : 7.5
     *
        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                       Local Node-ID                           |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                       Local Interface ID                      |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                       Remote Node-ID                          |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                       Remote Interface ID                     |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

             FEC Object-Type is 5, Unnumbered Adjacency with IPv4 NodeIDs
     */
    protected static final Logger log = LoggerFactory.getLogger(PcepFecObjectIPv4UnnumberedAdjacencyVer1.class);

    public static final byte FEC_OBJ_TYPE = 5;
    public static final byte FEC_OBJ_CLASS = (byte) 226;
    public static final byte FEC_OBJECT_VERSION = 1;
    public static final short FEC_OBJ_MINIMUM_LENGTH = 20;
    public static final int MINIMUM_COMMON_HEADER_LENGTH = 4;

    static final PcepObjectHeader DEFAULT_FEC_OBJECT_HEADER = new PcepObjectHeader(FEC_OBJ_CLASS, FEC_OBJ_TYPE,
            PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED, FEC_OBJ_MINIMUM_LENGTH);

    private PcepObjectHeader fecObjHeader;
    private int localNodeID;
    private int localInterfaceID;
    private int remoteNodeID;
    private int remoteInterfaceID;

    /**
     * Constructor to initialize parameter for PCEP fec object.
     *
     * @param fecObjHeader fec object header
     * @param localNodeID local node ID
     * @param localInterfaceID local interface ID
     * @param remoteNodeID remote node ID
     * @param remoteInterfaceID remote interface ID
     */
    public PcepFecObjectIPv4UnnumberedAdjacencyVer1(PcepObjectHeader fecObjHeader, int localNodeID,
            int localInterfaceID, int remoteNodeID, int remoteInterfaceID) {
        this.fecObjHeader = fecObjHeader;
        this.localNodeID = localNodeID;
        this.localInterfaceID = localInterfaceID;
        this.remoteNodeID = remoteNodeID;
        this.remoteInterfaceID = remoteInterfaceID;
    }

    /**
     * Sets Object Header.
     *
     * @param obj object header
     */
    public void setFecIpv4UnnumberedAdjacencyObjHeader(PcepObjectHeader obj) {
        this.fecObjHeader = obj;
    }

    @Override
    public void setLocalNodeID(int localNodeID) {
        this.localNodeID = localNodeID;
    }

    /**
     * Returns Object Header.
     *
     * @return fecObjHeader fec object header
     */
    public PcepObjectHeader getFecIpv4UnnumberedAdjacencyObjHeader() {
        return this.fecObjHeader;
    }

    @Override
    public int getLocalNodeID() {
        return this.localNodeID;
    }

    @Override
    public int getLocalInterfaceID() {
        return this.localInterfaceID;
    }

    @Override
    public void setLocalInterfaceID(int localInterfaceID) {
        this.localInterfaceID = localInterfaceID;
    }

    @Override
    public int getRemoteNodeID() {
        return this.remoteNodeID;
    }

    @Override
    public void setRemoteNodeID(int remoteNodeID) {
        this.remoteNodeID = remoteNodeID;
    }

    @Override
    public int getRemoteInterfaceID() {
        return this.remoteInterfaceID;
    }

    @Override
    public void setRemoteInterfaceID(int remoteInterfaceID) {
        this.remoteInterfaceID = remoteInterfaceID;
    }

    /**
     * Reads from channel buffer and returns object of PcepFecObjectIPv4UnnumberedAdjacency.
     *
     * @param cb of channel buffer
     * @return object of PcepFecObjectIPv4UnnumberedAdjacency
     * @throws PcepParseException when fails to read from channel buffer
     */
    public static PcepFecObjectIPv4UnnumberedAdjacency read(ChannelBuffer cb) throws PcepParseException {

        PcepObjectHeader fecObjHeader;
        int localNodeID;
        int localInterfaceID;
        int remoteNodeID;
        int remoteInterfaceID;

        fecObjHeader = PcepObjectHeader.read(cb);

        //take only FEC IPv4 Unnumbered Adjacency Object buffer.
        ChannelBuffer tempCb = cb.readBytes(fecObjHeader.getObjLen() - MINIMUM_COMMON_HEADER_LENGTH);
        localNodeID = tempCb.readInt();
        localInterfaceID = tempCb.readInt();
        remoteNodeID = tempCb.readInt();
        remoteInterfaceID = tempCb.readInt();

        return new PcepFecObjectIPv4UnnumberedAdjacencyVer1(fecObjHeader, localNodeID, localInterfaceID, remoteNodeID,
                remoteInterfaceID);
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {

        int objStartIndex = cb.writerIndex();

        //Write common header
        int objLenIndex = fecObjHeader.write(cb);
        cb.writeInt(localNodeID);
        cb.writeInt(localInterfaceID);
        cb.writeInt(remoteNodeID);
        cb.writeInt(remoteInterfaceID);

        //Now write FEC IPv4 Unnumbered Adjacency Object Length
        cb.setShort(objLenIndex, (short) (cb.writerIndex() - objStartIndex));

        return cb.writerIndex();
    }

    /**
     * Builder class for PCEP Fec object IPv4 unnumbered Adjacency.
     */
    public static class Builder implements PcepFecObjectIPv4UnnumberedAdjacency.Builder {
        private boolean bIsHeaderSet = false;
        private boolean bIsLocalNodeIDset = false;
        private boolean bIsLocalInterfaceIDset = false;
        private boolean bIsRemoteNodeIDset = false;
        private boolean bIsRemoteInterfaceIDset = false;

        private PcepObjectHeader fecObjHeader;
        private int localNodeID;
        private int localInterfaceID;
        private int remoteNodeID;
        private int remoteInterfaceID;

        private boolean bIsPFlagSet = false;
        private boolean bPFlag;

        private boolean bIsIFlagSet = false;
        private boolean bIFlag;

        @Override
        public PcepFecObjectIPv4UnnumberedAdjacency build() throws PcepParseException {
            PcepObjectHeader fecObjHeader = this.bIsHeaderSet ? this.fecObjHeader : DEFAULT_FEC_OBJECT_HEADER;

            if (!this.bIsLocalNodeIDset) {
                throw new PcepParseException(
                        " Local Node ID not set while building PcepFecObjectIPv4UnnumberedAdjacency object.");
            }
            if (!this.bIsLocalInterfaceIDset) {
                throw new PcepParseException(
                        " Local Interface ID not set while building PcepFecObjectIPv4UnnumberedAdjacency object.");
            }
            if (!this.bIsRemoteNodeIDset) {
                throw new PcepParseException(
                        " Remote Node ID not set while building PcepFecObjectIPv4UnnumberedAdjacency object.");
            }
            if (!this.bIsRemoteInterfaceIDset) {
                throw new PcepParseException(
                        " Remote Interface ID not set while building PcepFecObjectIPv4UnnumberedAdjacency object.");
            }
            if (bIsPFlagSet) {
                fecObjHeader.setPFlag(bPFlag);
            }
            if (bIsIFlagSet) {
                fecObjHeader.setIFlag(bIFlag);
            }
            return new PcepFecObjectIPv4UnnumberedAdjacencyVer1(fecObjHeader, this.localNodeID, this.localInterfaceID,
                    this.remoteNodeID, this.remoteInterfaceID);
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
        public PcepObjectHeader getFecIpv4UnnumberedAdjacencyObjHeader() {
            return this.fecObjHeader;
        }

        @Override
        public Builder setFecIpv4UnnumberedAdjacencyObjHeader(PcepObjectHeader obj) {
            this.fecObjHeader = obj;
            this.bIsHeaderSet = true;
            return this;
        }

        @Override
        public int getLocalNodeID() {
            return this.localNodeID;
        }

        @Override
        public Builder setLocalNodeID(int value) {
            this.localNodeID = value;
            this.bIsLocalNodeIDset = true;
            return this;
        }

        @Override
        public int getLocalInterfaceID() {
            return this.localInterfaceID;
        }

        @Override
        public Builder setLocalInterfaceID(int value) {
            this.localInterfaceID = value;
            this.bIsLocalInterfaceIDset = true;
            return this;
        }

        @Override
        public int getRemoteNodeID() {
            return this.remoteNodeID;
        }

        @Override
        public Builder setRemoteNodeID(int value) {
            this.remoteNodeID = value;
            this.bIsRemoteNodeIDset = true;
            return this;
        }

        @Override
        public int getRemoteInterfaceID() {
            return this.remoteInterfaceID;
        }

        @Override
        public Builder setRemoteInterfaceID(int value) {
            this.remoteInterfaceID = value;
            this.bIsRemoteInterfaceIDset = true;
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
                .add("LocalNodeID: ", localNodeID)
                .add("LocalInterfaceID: ", localInterfaceID).add("RemoteNodeID: ", remoteNodeID)
                .add("RemoteInterfaceID: ", remoteInterfaceID).toString();
    }
}
