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
import org.onosproject.pcepio.protocol.PcepSrpObject;
import org.onosproject.pcepio.types.PathSetupTypeTlv;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.onosproject.pcepio.types.PcepValueType;
import org.onosproject.pcepio.types.SymbolicPathNameTlv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP SRP obejct.
 */
public class PcepSrpObjectVer1 implements PcepSrpObject {

    /*
     * ref : draft-ietf-pce-stateful-pce-10, section : 7.2
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    | Object-Class  |   OT  |Res|P|I|   Object Length (bytes)       |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                          Flags                            |S|R|
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                        SRP-ID-number                          |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                                                               |
    //                      Optional TLVs                          //
    |                                                               |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     */
    protected static final Logger log = LoggerFactory.getLogger(PcepSrpObjectVer1.class);

    public static final byte SRP_OBJ_TYPE = 1;
    public static final byte SRP_OBJ_CLASS = 33;
    public static final byte SRP_OBJECT_VERSION = 1;
    public static final short SRP_OBJ_MINIMUM_LENGTH = 12;
    public static final int MINIMUM_COMMON_HEADER_LENGTH = 4;
    public static final boolean FLAG_DEFAULT_VALUE = false;

    static final PcepObjectHeader DEFAULT_SRP_OBJECT_HEADER = new PcepObjectHeader(SRP_OBJ_CLASS, SRP_OBJ_TYPE,
            PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED, SRP_OBJ_MINIMUM_LENGTH);

    private PcepObjectHeader srpObjHeader;
    private static int flags;
    private boolean bRFlag;
    private boolean bSFlag;
    private int srpId;

    //Optional TLV
    private LinkedList<PcepValueType> llOptionalTlv;
    public static final byte BBIT_SET = 1;
    public static final byte BBIT_RESET = 0;

    /**
     * Constructor to initialize member variables.
     *
     * @param srpObjHeader srp object header
     * @param bRFlag R flag
     * @param bSFlag S (sync) flag
     * @param srpID srp Id
     * @param llOptionalTlv list of optional tlv
     */
    public PcepSrpObjectVer1(PcepObjectHeader srpObjHeader, boolean bRFlag, boolean bSFlag, int srpID,
            LinkedList<PcepValueType> llOptionalTlv) {

        this.srpObjHeader = srpObjHeader;
        this.bRFlag = bRFlag;
        this.bSFlag = bSFlag;
        this.srpId = srpID;
        this.llOptionalTlv = llOptionalTlv;
    }

    /**
     * sets the SRP object header.
     *
     * @param obj srp object header
     */
    public void setSrpObjHeader(PcepObjectHeader obj) {
        this.srpObjHeader = obj;
    }

    @Override
    public void setSrpID(int srpID) {
        this.srpId = srpID;
    }

    @Override
    public void setRFlag(boolean bRFlag) {
        this.bRFlag = bRFlag;
    }

    @Override
    public void setSFlag(boolean bSFlag) {
        this.bSFlag = bSFlag;
    }

    /**
     * Returns SRP object header.
     *
     * @return srpObjHeader
     */
    public PcepObjectHeader getSrpObjHeader() {
        return this.srpObjHeader;
    }

    @Override
    public int getSrpID() {
        return this.srpId;
    }

    @Override
    public boolean getRFlag() {
        return this.bRFlag;
    }

    @Override
    public boolean getSFlag() {
        return this.bSFlag;
    }

    @Override
    public void setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv) {
        this.llOptionalTlv = llOptionalTlv;

    }

    @Override
    public LinkedList<PcepValueType> getOptionalTlv() {
        return this.llOptionalTlv;
    }

    /**
     * Reads from channel buffer and returns instance of PCEP SRP object.
     *
     * @param cb of channel buffer.
     * @return PCEP SRP object
     * @throws PcepParseException when srp object is not received in channel buffer
     */
    public static PcepSrpObject read(ChannelBuffer cb) throws PcepParseException {

        log.debug("SrpObject::read");
        PcepObjectHeader srpObjHeader;
        boolean bRFlag;
        boolean bSFlag;

        int srpID;
        int flags;
        LinkedList<PcepValueType> llOptionalTlv = new LinkedList<>();

        srpObjHeader = PcepObjectHeader.read(cb);

        if (srpObjHeader.getObjClass() != SRP_OBJ_CLASS) {
            throw new PcepParseException("SRP object expected. But received " + srpObjHeader.getObjClass());
        }

        //take only SrpObject buffer.
        ChannelBuffer tempCb = cb.readBytes(srpObjHeader.getObjLen() - MINIMUM_COMMON_HEADER_LENGTH);
        flags = tempCb.readInt();
        bRFlag = 0 < (flags & 0x1);
        bSFlag = 0 < ((flags >> 1) & 0x1);
        srpID = tempCb.readInt();

        llOptionalTlv = parseOptionalTlv(tempCb);

        return new PcepSrpObjectVer1(srpObjHeader, bRFlag, bSFlag, srpID, llOptionalTlv);
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {

        int objStartIndex = cb.writerIndex();

        //write common header
        int objLenIndex = srpObjHeader.write(cb);

        //write Flags
        byte bFlag;

        bFlag = (bRFlag) ? BBIT_SET : BBIT_RESET;
        bFlag |= (((bSFlag) ? BBIT_SET : BBIT_RESET) << 1);

        cb.writeInt(bFlag);

        //write SrpId
        cb.writeInt(srpId);

        // Add optional TLV
        if (!packOptionalTlv(cb)) {
            throw new PcepParseException("Failed to write srp tlv to channel buffer.");
        }

        //now write SRP Object Length
        cb.setShort(objLenIndex, (short) (cb.writerIndex() - objStartIndex));

        return cb.writerIndex();
    }

    /**
     * Parse Optional TLvs from the channel buffer.
     *
     * @param cb of type channel buffer
     * @return list of optional tlvs
     * @throws PcepParseException when unsupported tlv is received in srp object
     */
    public static LinkedList<PcepValueType> parseOptionalTlv(ChannelBuffer cb) throws PcepParseException {

        LinkedList<PcepValueType> llOutOptionalTlv = new LinkedList<>();

        while (MINIMUM_COMMON_HEADER_LENGTH <= cb.readableBytes()) {

            PcepValueType tlv;
            short hType = cb.readShort();
            short hLength = cb.readShort();

            switch (hType) {
            case SymbolicPathNameTlv.TYPE:
                if (cb.readableBytes() < hLength) {
                    throw new PcepParseException("Length is not valid in SymbolicPathNameTlv");
                }
                tlv = SymbolicPathNameTlv.read(cb, hLength);
                break;
            case PathSetupTypeTlv.TYPE:
                if (cb.readableBytes() != PathSetupTypeTlv.LENGTH) {
                    throw new PcepParseException("Length is not valid in PathSetupTypeTlv");
                }
                tlv = PathSetupTypeTlv.of(cb.readInt());
                break;
            default:
                // Skip the unknown TLV.
                cb.skipBytes(hLength);
                tlv = null;
                log.info("Received unsupported TLV type :" + hType + " in SRP object.");
            }

            // Check for the padding
            int pad = hLength % 4;
            if (0 < pad) {
                pad = 4 - pad;
                if (pad <= cb.readableBytes()) {
                    cb.skipBytes(pad);
                }
            }

            if (tlv != null) {
                llOutOptionalTlv.add(tlv);
            }
        }

        return llOutOptionalTlv;
    }

    /**
     * Writes optional tlvs to channel buffer.
     *
     * @param cb of type channel buffer
     * @return true if writing optional tlv to channel buffer is success.
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
     * Builder class for PCEP srp Object.
     */
    public static class Builder implements PcepSrpObject.Builder {
        private boolean bIsHeaderSet = false;
        private boolean bIsSrpIdset = false;
        private boolean bIsRFlagSet = false;
        private boolean bIsSFlagSet = false;

        private PcepObjectHeader srpObjHeader;
        private int srpId;
        private boolean bRFlag;
        private boolean bSFlag;
        LinkedList<PcepValueType> llOptionalTlv = new LinkedList<>();

        private boolean bIsPFlagSet = false;
        private boolean bPFlag;

        private boolean bIsIFlagSet = false;
        private boolean bIFlag;

        @Override
        public PcepSrpObject build() throws PcepParseException {
            PcepObjectHeader srpObjHeader = this.bIsHeaderSet ? this.srpObjHeader : DEFAULT_SRP_OBJECT_HEADER;

            boolean bRFlag = this.bIsRFlagSet ? this.bRFlag : FLAG_DEFAULT_VALUE;
            boolean bSFlag = this.bIsSFlagSet ? this.bSFlag : FLAG_DEFAULT_VALUE;

            if (!this.bIsSrpIdset) {
                throw new PcepParseException("SrpID not set while building SRP Object.");
            }

            if (bIsPFlagSet) {
                srpObjHeader.setPFlag(bPFlag);
            }

            if (bIsIFlagSet) {
                srpObjHeader.setIFlag(bIFlag);
            }

            return new PcepSrpObjectVer1(srpObjHeader, bRFlag, bSFlag, this.srpId, this.llOptionalTlv);
        }

        @Override
        public PcepObjectHeader getSrpObjHeader() {
            return this.srpObjHeader;
        }

        @Override
        public Builder setSrpObjHeader(PcepObjectHeader obj) {
            this.srpObjHeader = obj;
            this.bIsHeaderSet = true;
            return this;
        }

        @Override
        public int getSrpID() {
            return this.srpId;
        }

        @Override
        public Builder setSrpID(int srpID) {
            this.srpId = srpID;
            this.bIsSrpIdset = true;
            return this;
        }

        @Override
        public boolean getRFlag() {
            return this.bRFlag;
        }

        @Override
        public Builder setRFlag(boolean bRFlag) {
            this.bRFlag = bRFlag;
            this.bIsRFlagSet = true;
            return this;
        }

        @Override
        public boolean getSFlag() {
            return this.bSFlag;
        }

        @Override
        public Builder setSFlag(boolean bSFlag) {
            this.bSFlag = bSFlag;
            this.bIsSFlagSet = true;
            return this;
        }

        @Override
        public Builder setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv) {
            this.llOptionalTlv = llOptionalTlv;
            return this;
        }

        @Override
        public LinkedList<PcepValueType> getOptionalTlv() {
            return this.llOptionalTlv;
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
                .add("RFlag", bRFlag)
                .add("SFlag", bSFlag)
                .add("SRPID", srpId)
                .add("OptionalTlvList", llOptionalTlv)
                .toString();
    }
}
