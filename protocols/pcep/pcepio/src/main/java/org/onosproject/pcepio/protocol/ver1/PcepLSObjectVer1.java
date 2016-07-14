/*
 * Copyright 2016-present Open Networking Laboratory
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

import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepLSObject;
import org.onosproject.pcepio.types.LocalNodeDescriptorsTlv;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.onosproject.pcepio.types.PcepValueType;
import org.onosproject.pcepio.types.RemoteNodeDescriptorsTlv;
import org.onosproject.pcepio.types.RoutingUniverseTlv;
import org.onosproject.pcepio.types.LinkAttributesTlv;
import org.onosproject.pcepio.types.LinkDescriptorsTlv;
import org.onosproject.pcepio.types.NodeAttributesTlv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP LS (link-state) object.
 */
public class PcepLSObjectVer1 implements PcepLSObject {
    /*
     *
    reference: draft-dhodylee-pce-pcep-ls-01, section 9.2.

    Two Object-Type values are defined for the LS object:

    o  LS Node: LS Object-Type is 1.

    o  LS Link: LS Object-Type is 2.

    o  LS IPv4 Topology Prefix: LS Object-Type is 3.

    o  LS IPv6 Topology Prefix: LS Object-Type is 4.

    The format of the LS object body is as follows:

       0                   1                   2                   3
       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |  Protocol-ID  |          Flag                             |R|S|
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                          LS-ID                                |
      |                                                               |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      //                         TLVs                                //
      |                                                               |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    protected static final Logger log = LoggerFactory.getLogger(PcepLSObjectVer1.class);

    public static final byte LS_OBJ_TYPE_NODE_VALUE = 1;
    public static final byte LS_OBJ_TYPE_LINK_VALUE = 2;

    public static final byte LS_OBJ_CLASS = (byte) 224;
    public static final byte LS_OBJECT_VERSION = 1;

    // LS_OBJ_MINIMUM_LENGTH = LSObjectHeaderLen(4) + LSObjectLen(8)
    public static final short LS_OBJ_MINIMUM_LENGTH = 12;

    // Signaled, all default values to be checked.
    public static final byte DEFAULT_PROTOCOL_ID = 1; //IS-IS Level 1
    public static final boolean DEFAULT_R_FLAG = false;
    public static final boolean DEFAULT_S_FLAG = false;
    public static final int DEFAULT_LS_ID = 0;

    public static final int OBJECT_HEADER_LENGTH = 4;
    public static final int RIGHT_SHIFT_ONE = 1;
    public static final int RIGHT_FIRST_FLAG = 0x1;
    public static final int FLAG_SET_R_FLAG = 0x2;
    public static final int FLAG_SET_S_FLAG = 0x1;
    public static final int MINIMUM_COMMON_HEADER_LENGTH = 4;
    public static final int MINIMUM_TLV_HEADER_LENGTH = 4;

    public static final PcepObjectHeader DEFAULT_LS_OBJECT_HEADER = new PcepObjectHeader(LS_OBJ_CLASS,
            LS_OBJ_TYPE_NODE_VALUE, PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED,
            LS_OBJ_MINIMUM_LENGTH);

    private PcepObjectHeader lsObjHeader;
    private byte protocolId;
    // 2-flags
    private boolean removeFlag;
    private boolean syncFlag;
    private long lsId; //link-state identifier
    // Optional TLV
    private List<PcepValueType> optionalTlvList;

    /**
     * Constructor to initialize variables.
     *
     * @param lsObjHeader LS Object header
     * @param protocolId Protocol-ID
     * @param removeFlag R-flag
     * @param syncFlag S-flag
     * @param lsId LS-ID
     * @param optionalTlvList linked list of Optional TLV
     */
    public PcepLSObjectVer1(PcepObjectHeader lsObjHeader, byte protocolId, boolean removeFlag,
            boolean syncFlag, long lsId, List<PcepValueType> optionalTlvList) {

        this.lsObjHeader = lsObjHeader;
        this.protocolId = protocolId;
        this.removeFlag = removeFlag;
        this.syncFlag = syncFlag;
        this.lsId = lsId;
        this.optionalTlvList = optionalTlvList;
    }

    @Override
    public PcepObjectHeader getLSObjHeader() {
        return this.lsObjHeader;
    }

    @Override
    public void setLSObjHeader(PcepObjectHeader obj) {
        this.lsObjHeader = obj;
    }

    @Override
    public byte getProtocolId() {
        return this.protocolId;
    }

    @Override
    public void setProtocolId(byte protId) {
        this.protocolId = protId;
    }

    @Override
    public boolean getRemoveFlag() {
        return this.removeFlag;
    }

    @Override
    public void setRemoveFlag(boolean removeFlag) {
        this.removeFlag = removeFlag;
    }

    @Override
    public boolean getSyncFlag() {
        return this.syncFlag;
    }

    @Override
    public void setSyncFlag(boolean syncFlag) {
        this.syncFlag = syncFlag;
    }

    @Override
    public long getLSId() {
        return this.lsId;
    }

    @Override
    public void setLSId(long lsId) {
        this.lsId = lsId;
    }

    @Override
    public List<PcepValueType> getOptionalTlv() {
        return this.optionalTlvList;
    }

    @Override
    public void setOptionalTlv(List<PcepValueType> optionalTlvList) {
        this.optionalTlvList = optionalTlvList;
    }

    /**
     * Reads from the channel buffer and returns Object of PcepLSObject.
     *
     * @param cb of type channel buffer
     * @return Object of PcepLSObject
     * @throws PcepParseException if mandatory fields are missing
     */
    public static PcepLSObject read(ChannelBuffer cb) throws PcepParseException {
        log.debug("read");

        PcepObjectHeader lsObjHeader;
        byte protocolId;
        // 2-flags
        boolean removeFlag;
        boolean syncFlag;
        long lsId;
        List<PcepValueType> optionalTlvList;

        lsObjHeader = PcepObjectHeader.read(cb);

        //take only LSObject buffer.
        ChannelBuffer tempCb = cb.readBytes(lsObjHeader.getObjLen() - OBJECT_HEADER_LENGTH);

        protocolId = tempCb.readByte();
        //ignore first two bytes of Flags
        tempCb.readShort();

        Integer iTemp = (int) tempCb.readByte(); //read 3rd byte Flag
        syncFlag = (iTemp & FLAG_SET_S_FLAG) == FLAG_SET_S_FLAG;
        removeFlag = (iTemp & FLAG_SET_R_FLAG) == FLAG_SET_R_FLAG;

        lsId = tempCb.readLong();

        // parse optional TLV
        optionalTlvList = parseOptionalTlv(tempCb);

        return new PcepLSObjectVer1(lsObjHeader, protocolId, removeFlag, syncFlag, lsId, optionalTlvList);
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {

        //write Object header
        int objStartIndex = cb.writerIndex();
        int objLenIndex = lsObjHeader.write(cb);

        if (objLenIndex <= 0) {
            throw new PcepParseException("ObjectLength Index is " + objLenIndex);
        }

        //write Protocol ID
        cb.writeByte(this.protocolId);

        //write Flag
        cb.writeShort(0);

        byte bTemp = 0;
        if (syncFlag) {
            bTemp = FLAG_SET_S_FLAG;
        }

        if (removeFlag) {
            bTemp = (byte) (bTemp | FLAG_SET_R_FLAG);
        }
        cb.writeByte(bTemp);

        //write LSId
        cb.writeLong(lsId);

        // Add optional TLV
        packOptionalTlv(cb);

        //Update object length now
        int length = cb.writerIndex() - objStartIndex;

        //will be helpful during print().
        lsObjHeader.setObjLen((short) length);

        cb.setShort(objLenIndex, (short) length);

        return cb.writerIndex();
    }

    /**
     * Returns Linked list of PCEP Value Type.
     *
     * @param cb of channel buffer
     * @return Linked list of PCEP Value Type
     * @throws PcepParseException if mandatory fields are missing
     */
    protected static List<PcepValueType> parseOptionalTlv(ChannelBuffer cb) throws PcepParseException {

        List<PcepValueType> llOutOptionalTlv;

        llOutOptionalTlv = new LinkedList<>();

        while (MINIMUM_TLV_HEADER_LENGTH <= cb.readableBytes()) {

            PcepValueType tlv;
            short hType = cb.readShort();
            short hLength = cb.readShort();
            long lValue = 0;

            switch (hType) {

            case RoutingUniverseTlv.TYPE:
                lValue = cb.readLong();
                tlv = new RoutingUniverseTlv(lValue);
                break;
            case LocalNodeDescriptorsTlv.TYPE:
                tlv = LocalNodeDescriptorsTlv.read(cb, hLength);
                break;
            case RemoteNodeDescriptorsTlv.TYPE:
                tlv = RemoteNodeDescriptorsTlv.read(cb, hLength);
                break;
            case LinkDescriptorsTlv.TYPE:
                tlv = LinkDescriptorsTlv.read(cb, hLength);
                break;
            case NodeAttributesTlv.TYPE:
                tlv = NodeAttributesTlv.read(cb, hLength);
                break;
            case LinkAttributesTlv.TYPE:
                tlv = LinkAttributesTlv.read(cb, hLength);
                break;
            default:
                throw new PcepParseException("Unsupported TLV type :" + hType);
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

        if (0 < cb.readableBytes()) {

            throw new PcepParseException("Optional Tlv parsing error. Extra bytes received.");
        }
        return llOutOptionalTlv;
    }

    /**
     * Returns the writer index.
     *
     * @param cb of type channel buffer
     * @return the writer index.
     */
    protected int packOptionalTlv(ChannelBuffer cb) {

        ListIterator<PcepValueType> listIterator = optionalTlvList.listIterator();

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
        return cb.writerIndex();
    }

    /**
     * Builder class for PCEP LS (link-state) object.
     */
    public static class Builder implements PcepLSObject.Builder {
        private boolean isHeaderSet = false;
        private boolean isProtocolIdSet = false;
        private boolean isRemoveFlagSet = false;
        private boolean isSyncFlagSet = false;
        private boolean isLSIdSet = false;

        private PcepObjectHeader lsObjHeader;
        private byte protocolId;
        private boolean removeFlag;
        private boolean syncFlag;
        private long lsId;
        private List<PcepValueType> optionalTlvList = new LinkedList<>();

        private boolean isProcRuleFlagSet = false;
        private boolean procRuleFlag; //Processing rule flag

        private boolean isIgnoreFlagSet = false;
        private boolean ignoreFlag;

        @Override
        public PcepLSObject build() {
            PcepObjectHeader lsObjHeader = this.isHeaderSet ? this.lsObjHeader : DEFAULT_LS_OBJECT_HEADER;

            byte protocolId = this.isProtocolIdSet ? this.protocolId : DEFAULT_PROTOCOL_ID;
            boolean removeFlag = this.isRemoveFlagSet ? this.removeFlag : DEFAULT_R_FLAG;
            boolean syncFlag = this.isSyncFlagSet ? this.syncFlag : DEFAULT_S_FLAG;
            long lsId = this.isLSIdSet ? this.lsId : DEFAULT_LS_ID;

            if (isProcRuleFlagSet) {
                lsObjHeader.setPFlag(procRuleFlag);
            }

            if (isIgnoreFlagSet) {
                lsObjHeader.setIFlag(ignoreFlag);
            }

            return new PcepLSObjectVer1(lsObjHeader, protocolId, removeFlag, syncFlag, lsId, optionalTlvList);

        }

        @Override
        public PcepObjectHeader getLSObjHeader() {
            return this.lsObjHeader;
        }

        @Override
        public Builder setLSObjHeader(PcepObjectHeader obj) {
            this.lsObjHeader = obj;
            this.isHeaderSet = true;
            return this;
        }

        @Override
        public byte getProtocolId() {
            return this.protocolId;
        }

        @Override
        public Builder setProtocolId(byte protId) {
            this.protocolId = protId;
            this.isProtocolIdSet = true;
            return this;
        }

        @Override
        public boolean getRemoveFlag() {
            return this.removeFlag;
        }

        @Override
        public Builder setRemoveFlag(boolean removeFlag) {
            this.removeFlag = removeFlag;
            this.isRemoveFlagSet = true;
            return this;
        }

        @Override
        public boolean getSyncFlag() {
            return this.syncFlag;
        }

        @Override
        public Builder setSyncFlag(boolean syncFlag) {
            this.syncFlag = syncFlag;
            this.isSyncFlagSet = true;
            return this;
        }

        @Override
        public long getLSId() {
            return this.lsId;
        }

        @Override
        public Builder setLSId(long lsId) {
            this.lsId = lsId;
            this.isLSIdSet = true;
            return this;
        }

        @Override
        public List<PcepValueType> getOptionalTlv() {
            return this.optionalTlvList;
        }

        @Override
        public Builder setOptionalTlv(List<PcepValueType> optionalTlvList) {
            this.optionalTlvList = optionalTlvList;
            return this;
        }

        @Override
        public Builder setPFlag(boolean value) {
            this.procRuleFlag = value;
            this.isProcRuleFlagSet = true;
            return this;
        }

        @Override
        public Builder setIFlag(boolean value) {
            this.ignoreFlag = value;
            this.isIgnoreFlagSet = true;
            return this;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues()
                .add("ObjectHeader", lsObjHeader)
                .add("ProtocolId", protocolId)
                .add("RFlag", (removeFlag) ? 1 : 0)
                .add("SFlag", (syncFlag) ? 1 : 0)
                .add("LsId", lsId)
                .add("OptionalTlv", optionalTlvList).toString();
    }
}
