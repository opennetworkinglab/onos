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
import org.onosproject.pcepio.protocol.PcepOpenObject;
import org.onosproject.pcepio.protocol.PcepType;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.onosproject.pcepio.types.GmplsCapabilityTlv;
import org.onosproject.pcepio.types.NodeAttributesTlv;
import org.onosproject.pcepio.types.PceccCapabilityTlv;
import org.onosproject.pcepio.types.PcepLabelDbVerTlv;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.onosproject.pcepio.types.PcepValueType;
import org.onosproject.pcepio.types.SrPceCapabilityTlv;
import org.onosproject.pcepio.types.StatefulLspDbVerTlv;
import org.onosproject.pcepio.types.StatefulPceCapabilityTlv;
import org.onosproject.pcepio.types.LsCapabilityTlv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP open object.
 */
public class PcepOpenObjectVer1 implements PcepOpenObject {

    /*
     message format.
      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    | Object-Class  |   OT  |Res|P|I|   Object Length (bytes)       |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    | Ver |   Flags |   Keepalive   |  DeadTimer    |      SID      |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                                                               |
    //                       Optional TLVs                         //
    |                                                               |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                     The OPEN Object format
     */
    protected static final Logger log = LoggerFactory.getLogger(PcepOpenObjectVer1.class);

    public static final PcepType MSG_TYPE = PcepType.OPEN;
    public static final byte OPEN_OBJECT_VERSION = 1;
    public static final byte OPEN_OBJ_TYPE = 1;
    public static final byte OPEN_OBJ_CLASS = 1;
    public static final byte DEFAULT_KEEPALIVE_TIME = 30;
    public static final byte DEFAULT_DEAD_TIME = 120;
    public static final short OPEN_OBJ_MINIMUM_LENGTH = 8;
    public static final int DEFAULT_GMPLS_CAPABILITY_TLV_IVALUE = 0X0;
    public static final int DEFAULT_STATEFUL_PCE_CAPABILITY_TLV_IVALUE = 0xf;
    public static final int DEFAULT_PCECC_CAPABILITY_TLV_IVALUE = 0x7;
    public static final int DEFAULT_PCEP_LABEL_DB_VER_TLV_IVALUE = 0X0;

    public static final PcepObjectHeader DEFAULT_OPEN_HEADER = new PcepObjectHeader(OPEN_OBJ_CLASS, OPEN_OBJ_TYPE,
            PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED, OPEN_OBJ_MINIMUM_LENGTH);

    private PcepObjectHeader openObjHeader;
    private byte keepAliveTime;
    private byte deadTime;
    private byte sessionId;
    private LinkedList<PcepValueType> llOptionalTlv;

    /**
     * Default constructor.
     */
    public PcepOpenObjectVer1() {
        this.openObjHeader = null;
        this.keepAliveTime = 0;
        this.deadTime = 0;
        this.sessionId = 0;
        this.llOptionalTlv = null;
    }

    /**
     * Constructor to initialize all member variables.
     *
     * @param openObjHeader Open Object Header
     * @param keepAliveTime Keepalive timer value
     * @param deadTime      Dead timer value
     * @param sessionID     session id
     * @param llOptionalTlv Optional TLV
     */
    public PcepOpenObjectVer1(PcepObjectHeader openObjHeader, byte keepAliveTime, byte deadTime, byte sessionID,
            LinkedList<PcepValueType> llOptionalTlv) {
        this.openObjHeader = openObjHeader;
        this.keepAliveTime = keepAliveTime;
        this.deadTime = deadTime;
        this.sessionId = sessionID;
        this.llOptionalTlv = llOptionalTlv;
    }

    @Override
    public PcepObjectHeader getOpenObjHeader() {
        return this.openObjHeader;
    }

    @Override
    public void setOpenObjHeader(PcepObjectHeader obj) {
        this.openObjHeader = obj;
    }

    @Override
    public byte getKeepAliveTime() {
        return this.keepAliveTime;
    }

    @Override
    public void setKeepAliveTime(byte value) {
        this.keepAliveTime = value;
    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
    }

    @Override
    public byte getDeadTime() {
        return this.deadTime;
    }

    @Override
    public void setDeadTime(byte value) {
        this.deadTime = value;
    }

    @Override
    public byte getSessionId() {
        return this.sessionId;
    }

    @Override
    public void setSessionId(byte value) {
        this.sessionId = value;
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
     * Reads from channel buffer and returns object of PcepOpenObject.
     *
     * @param cb of type channel buffer
     * @return object of PcepOpenObject
     * @throws PcepParseException if mandatory fields are missing
     */
    public static PcepOpenObject read(ChannelBuffer cb) throws PcepParseException {

        PcepObjectHeader openObjHeader;
        byte version;
        byte keepAliveTime;
        byte deadTime;
        byte sessionID;
        LinkedList<PcepValueType> llOptionalTlv;

        openObjHeader = PcepObjectHeader.read(cb);
        version = cb.readByte();
        version = (byte) (version >> PcepMessageVer1.SHIFT_FLAG);
        if (version != OPEN_OBJECT_VERSION) {
            throw new PcepParseException("Wrong version: Expected=PcepVersion.PCEP_1(1), got=" + version);
        }
        /* Keepalive */
        keepAliveTime = cb.readByte();

        /* DeadTimer */
        deadTime = cb.readByte();

        /* SID */
        sessionID = cb.readByte();

        // Optional TLV
        llOptionalTlv = parseOptionalTlv(cb);

        return new PcepOpenObjectVer1(openObjHeader, keepAliveTime, deadTime, sessionID, llOptionalTlv);
    }

    /**
     * Returns linkedlist of optional tlvs.
     *
     * @param cb of type channel buffer
     * @return llOptionalTlv Optional TLV
     * @throws PcepParseException if mandatory fields are missing
     */
    protected static LinkedList<PcepValueType> parseOptionalTlv(ChannelBuffer cb) throws PcepParseException {

        LinkedList<PcepValueType> llOptionalTlv;

        llOptionalTlv = new LinkedList<>();

        while (4 <= cb.readableBytes()) {
            PcepValueType tlv;
            short hType = cb.readShort();
            short hLength = cb.readShort();

            switch (hType) {
            case GmplsCapabilityTlv.TYPE:
                log.debug("GmplsCapabilityTlv");
                if (GmplsCapabilityTlv.LENGTH != hLength) {
                    throw new PcepParseException("Invalid length received for Gmpls_Capability_Tlv.");
                }
                int iValue = cb.readInt();
                tlv = new GmplsCapabilityTlv(iValue);
                break;
            case StatefulPceCapabilityTlv.TYPE:
                log.debug("StatefulPceCapabilityTlv");
                if (StatefulPceCapabilityTlv.LENGTH != hLength) {
                    throw new PcepParseException("Invalid length received for StatefulPceCapabilityTlv.");
                }
                tlv = StatefulPceCapabilityTlv.read(cb);
                break;
            case PceccCapabilityTlv.TYPE:
                log.debug("PceccCapabilityTlv");
                if (PceccCapabilityTlv.LENGTH != hLength) {
                    throw new PcepParseException("Invalid length for PceccCapabilityTlv.");
                }
                iValue = cb.readInt();
                tlv = new PceccCapabilityTlv(iValue);
                break;
            case StatefulLspDbVerTlv.TYPE:
                log.debug("StatefulLspDbVerTlv");
                if (StatefulLspDbVerTlv.LENGTH != hLength) {
                    throw new PcepParseException("Invalid length received for StatefulLspDbVerTlv.");
                }
                long lValue = cb.readLong();
                tlv = new StatefulLspDbVerTlv(lValue);
                break;
            case LsCapabilityTlv.TYPE:
                log.debug("LsCapabilityTlv");
                if (LsCapabilityTlv.LENGTH != hLength) {
                    throw new PcepParseException("Invalid length received for LsCapabilityTlv.");
                }
                iValue = cb.readInt();
                tlv = new LsCapabilityTlv(iValue);
                break;
            case PcepLabelDbVerTlv.TYPE:
                log.debug("PcepLabelDbVerTlv");
                if (PcepLabelDbVerTlv.LENGTH != hLength) {
                    throw new PcepParseException("Invalid length received for PcepLabelDbVerTlv.");
                }
                lValue = cb.readLong();
                tlv = new PcepLabelDbVerTlv(lValue);
                break;
            case NodeAttributesTlv.TYPE:
                log.debug("NodeAttributesTlv");
                if (cb.readableBytes() < hLength) {
                    throw new PcepParseException("Invalid length for NodeAttributesTlv.");
                }
                tlv = NodeAttributesTlv.read(cb.readBytes(hLength), hLength);
                break;
            case SrPceCapabilityTlv.TYPE:
                log.debug("SrPceCapabilityTlv");
                if (SrPceCapabilityTlv.LENGTH != hLength) {
                    throw new PcepParseException("Invalid length received for SrPceCapabilityTlv.");
                }
                tlv = SrPceCapabilityTlv.read(cb);
                break;
            default:
                log.debug("Unsupported TLV: " + hType);
                cb.skipBytes(hLength);
                tlv = null;
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
                llOptionalTlv.add(tlv);
            }
        }

        if (0 < cb.readableBytes()) {
            throw new PcepParseException("Optional Tlv parsing error. Extra bytes received.");
        }

        return llOptionalTlv;
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {

        int objStartIndex = cb.writerIndex();

        //write common header
        int objLenIndex = openObjHeader.write(cb);

        if (objLenIndex <= 0) {
            throw new PcepParseException("Unable to write Open object header.");
        }

        cb.writeByte((byte) (OPEN_OBJECT_VERSION << PcepMessageVer1.SHIFT_FLAG));
        cb.writeByte(this.keepAliveTime);
        cb.writeByte(this.deadTime);
        cb.writeByte(this.sessionId);

        //Pack optional TLV
        packOptionalTlv(cb);

        //now write OPEN Object Length
        int length = cb.writerIndex() - objStartIndex;
        cb.setShort(objLenIndex, (short) length);
        //will be helpful during print().
        this.openObjHeader.setObjLen((short) length);

        return length;
    }

    /**
     * Returns writer index.
     *
     * @param cb of type channel buffer.
     * @return writer index
     */
    protected int packOptionalTlv(ChannelBuffer cb) {
        int startIndex = cb.writerIndex();

        LinkedList<PcepValueType> llOptionalTlv = this.llOptionalTlv;
        ListIterator<PcepValueType> listIterator = llOptionalTlv.listIterator();
        while (listIterator.hasNext()) {
            PcepValueType tlv = listIterator.next();
            if (tlv == null) {
                log.debug("TLV is null from OptionalTlv list");
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
        return cb.writerIndex() - startIndex;
    }

    /**
     * Builder class for PCPE open object.
     */
    public static class Builder implements PcepOpenObject.Builder {
        // Pcep open message fields
        private boolean bIsHeaderSet = false;
        private PcepObjectHeader openObjHeader;
        private boolean bIsKeepAliveTimeSet = false;
        private byte keepAliveTime;
        private boolean bIsDeadTimeSet = false;
        private byte deadTime;
        private boolean bIsSessionIDSet = false;
        private byte sessionID;
        private boolean bIsOptionalTlvSet = false;
        private LinkedList<PcepValueType> llOptionalTlv = new LinkedList<>();

        private boolean bIsPFlagSet = false;
        private boolean bPFlag;

        private boolean bIsIFlagSet = false;
        private boolean bIFlag;

        @Override
        public PcepOpenObject build() throws PcepParseException {
            PcepObjectHeader openObjHeader = this.bIsHeaderSet ? this.openObjHeader : DEFAULT_OPEN_HEADER;
            byte keepAliveTime = this.bIsKeepAliveTimeSet ? this.keepAliveTime : DEFAULT_KEEPALIVE_TIME;
            byte deadTime = this.bIsDeadTimeSet ? this.deadTime : DEFAULT_DEAD_TIME;

            if (!this.bIsSessionIDSet) {
                throw new PcepParseException("SessionID is not set (mandatory)");
            }
            if (!this.bIsOptionalTlvSet) {
                //Add tlv to list
                //Add GmplsCapabilityTlv
                PcepValueType tlv;
                int iValue = DEFAULT_GMPLS_CAPABILITY_TLV_IVALUE;
                tlv = new GmplsCapabilityTlv(iValue);
                this.llOptionalTlv.add(tlv);

                //Add StatefulPceCapabilityTlv
                iValue = DEFAULT_STATEFUL_PCE_CAPABILITY_TLV_IVALUE;
                tlv = new StatefulPceCapabilityTlv(iValue);
                this.llOptionalTlv.add(tlv);

            }

            if (bIsPFlagSet) {
                openObjHeader.setPFlag(bPFlag);
            }

            if (bIsIFlagSet) {
                openObjHeader.setIFlag(bIFlag);
            }

            return new PcepOpenObjectVer1(openObjHeader, keepAliveTime, deadTime, this.sessionID, this.llOptionalTlv);
        }

        @Override
        public PcepObjectHeader getOpenObjHeader() {
            return this.openObjHeader;
        }

        @Override
        public Builder setOpenObjHeader(PcepObjectHeader obj) {
            this.openObjHeader = obj;
            this.bIsHeaderSet = true;
            return this;
        }

        @Override
        public byte getKeepAliveTime() {
            return this.keepAliveTime;
        }

        @Override
        public Builder setKeepAliveTime(byte value) {
            this.keepAliveTime = value;
            this.bIsKeepAliveTimeSet = true;
            return this;
        }

        @Override
        public byte getDeadTime() {
            return this.deadTime;
        }

        @Override
        public Builder setDeadTime(byte value) {
            this.deadTime = value;
            this.bIsDeadTimeSet = true;
            return this;
        }

        @Override
        public byte getSessionId() {
            return this.sessionID;
        }

        @Override
        public Builder setSessionId(byte value) {
            this.sessionID = value;
            this.bIsSessionIDSet = true;
            return this;
        }

        @Override
        public Builder setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv) {
            this.llOptionalTlv = llOptionalTlv;
            this.bIsOptionalTlvSet = true;
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
                .add("ObjectHeader", openObjHeader)
                .add("Keepalive", keepAliveTime)
                .add("DeadTimer", deadTime)
                .add("SessionId", sessionId)
                .add("OptionalTlv", llOptionalTlv)
                .toString();
    }
}
