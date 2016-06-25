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
import org.onosproject.pcepio.protocol.PcepCloseMsg;
import org.onosproject.pcepio.protocol.PcepMessageReader;
import org.onosproject.pcepio.protocol.PcepMessageWriter;
import org.onosproject.pcepio.protocol.PcepType;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.onosproject.pcepio.types.PcepValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP Close Message.
 */
class PcepCloseMsgVer1 implements PcepCloseMsg {

    /*
     * RFC : 5440 , section : 6.8
     * <Close Message>           ::= <Common Header> <CLOSE>
     *
         0                   1                   2                   3
         0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        | Ver |  Flags  |  Message-Type |       Message-Length          |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        | Object-Class  |   OT  |Res|P|I|   Object Length (bytes)       |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |          Reserved             |      Flags    |    Reason     |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                                                               |
        //                         Optional TLVs                       //
        |                                                               |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    protected static final Logger log = LoggerFactory.getLogger(PcepCloseMsgVer1.class);

    // Pcep version: 1
    public static final byte PACKET_VERSION = 1;
    public static final int PACKET_MINIMUM_LENGTH = 12;
    public static final PcepType MSG_TYPE = PcepType.CLOSE;
    public static final byte CLOSE_OBJ_TYPE = 1;
    public static final byte CLOSE_OBJ_CLASS = 15;
    public static final byte CLOSE_OBJECT_VERSION = 1;
    public static final byte DEFAULT_REASON = 1; // Default reason to close
    public static final short CLOSE_OBJ_MINIMUM_LENGTH = 8;
    public static final int SHIFT_FLAG = 5;
    static final PcepObjectHeader DEFAULT_CLOSE_HEADER = new PcepObjectHeader(CLOSE_OBJ_CLASS, CLOSE_OBJ_TYPE,
            PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED, CLOSE_OBJ_MINIMUM_LENGTH);

    private final PcepObjectHeader closeObjHeader;
    private byte yReason;
    private LinkedList<PcepValueType> llOptionalTlv;

    public static final PcepCloseMsgVer1.Reader READER = new Reader();

    /**
     * Reader class for reading close message for channel buffer.
     */
    static class Reader implements PcepMessageReader<PcepCloseMsg> {
        PcepObjectHeader closeObjHeader;
        byte yReason;
        // Optional TLV
        private LinkedList<PcepValueType> llOptionalTlv;

        @Override
        public PcepCloseMsg readFrom(ChannelBuffer cb) throws PcepParseException {

            if (cb.readableBytes() < PACKET_MINIMUM_LENGTH) {
                throw new PcepParseException("Packet size is less than the minimum length.");
            }
            // fixed value property version == 1
            byte version = cb.readByte();
            version = (byte) (version >> SHIFT_FLAG);
            if (version != PACKET_VERSION) {
                throw new PcepParseException("Wrong version. Expected=PcepVersion.PCEP_1(1), got=" + version);
            }
            // fixed value property type == 7
            byte type = cb.readByte();
            if (type != MSG_TYPE.getType()) {
                throw new PcepParseException("Wrong type. Expected=PcepType.CLOSE(7), got=" + type);
            }
            short length = cb.readShort();
            if (length < PACKET_MINIMUM_LENGTH) {
                throw new PcepParseException("Wrong length. Expected to be >= " + PACKET_MINIMUM_LENGTH + ", was: "
                        + length);
            }
            closeObjHeader = PcepObjectHeader.read(cb);
            // Reserved
            cb.readShort();
            // Flags
            cb.readByte();
            // Reason
            yReason = cb.readByte();
            // parse optional TLV
            llOptionalTlv = parseOptionalTlv(cb);
            return new PcepCloseMsgVer1(closeObjHeader, yReason, llOptionalTlv);
        }
    }

    /**
     * Parse the list of Optional Tlvs.
     *
     * @param cb channel buffer
     * @return list of Optional Tlvs
     * @throws PcepParseException when fails to parse optional tlvs
     */
    public static LinkedList<PcepValueType> parseOptionalTlv(ChannelBuffer cb) throws PcepParseException {

        LinkedList<PcepValueType> llOptionalTlv = new LinkedList<>();
        /*
         rfc 5440:
         Optional TLVs may be included within the CLOSE object body. The
         specification of such TLVs is outside the scope of this document.
         */
        return llOptionalTlv;
    }

    /**
     * constructor to initialize PCEP close Message with all the parameters.
     *
     * @param closeObjHeader object header for close message
     * @param yReason reason for closing the channel
     * @param llOptionalTlv list of optional tlvs
     */
    PcepCloseMsgVer1(PcepObjectHeader closeObjHeader, byte yReason, LinkedList<PcepValueType> llOptionalTlv) {

        this.closeObjHeader = closeObjHeader;
        this.yReason = yReason;
        this.llOptionalTlv = llOptionalTlv;
    }

    /**
     * Builder class for PCEP close message.
     */
    static class Builder implements PcepCloseMsg.Builder {

        // PCEP Close message fields
        private boolean bIsHeaderSet = false;
        private PcepObjectHeader closeObjHeader;
        private boolean bIsReasonSet = false;
        private byte yReason;
        private LinkedList<PcepValueType> llOptionalTlv = new LinkedList<>();

        private boolean bIsPFlagSet = false;
        private boolean bPFlag;

        private boolean bIsIFlagSet = false;
        private boolean bIFlag;

        @Override
        public PcepVersion getVersion() {
            return PcepVersion.PCEP_1;
        }

        @Override
        public PcepType getType() {
            return PcepType.CLOSE;
        }

        @Override
        public PcepCloseMsg build() {

            PcepObjectHeader closeObjHeader = this.bIsHeaderSet ? this.closeObjHeader : DEFAULT_CLOSE_HEADER;
            byte yReason = this.bIsReasonSet ? this.yReason : DEFAULT_REASON;

            if (bIsPFlagSet) {
                closeObjHeader.setPFlag(bPFlag);
            }

            if (bIsIFlagSet) {
                closeObjHeader.setIFlag(bIFlag);
            }
            return new PcepCloseMsgVer1(closeObjHeader, yReason, this.llOptionalTlv);
        }

        @Override
        public PcepObjectHeader getCloseObjHeader() {
            return this.closeObjHeader;
        }

        @Override
        public Builder setCloseObjHeader(PcepObjectHeader obj) {
            this.closeObjHeader = obj;
            this.bIsHeaderSet = true;
            return this;
        }

        @Override
        public byte getReason() {
            return this.yReason;
        }

        @Override
        public Builder setReason(byte value) {
            this.yReason = value;
            this.bIsReasonSet = true;
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
    public void writeTo(ChannelBuffer cb) throws PcepParseException {
        WRITER.write(cb, this);
    }

    static final Writer WRITER = new Writer();

    /**
     * Writer class for writing close message to channel buffer.
     */
    static class Writer implements PcepMessageWriter<PcepCloseMsgVer1> {

        @Override
        public void write(ChannelBuffer cb, PcepCloseMsgVer1 message) throws PcepParseException {
            int startIndex = cb.writerIndex();
            // first 3 bits set to version
            cb.writeByte((byte) (PACKET_VERSION << SHIFT_FLAG));
            // message type
            cb.writeByte(MSG_TYPE.getType());
            // length is length of variable message, will be updated at the end
            // Store the position of message
            // length in buffer
            int msgLenIndex = cb.writerIndex();
            cb.writeShort((short) 0);
            int objStartIndex = cb.writerIndex();
            int objLenIndex = message.closeObjHeader.write(cb);
            if (objLenIndex <= 0) {
                throw new PcepParseException("Failed to write Close object header.");
            }
            // first 3 bits set to version
            cb.writeShort(0); // Reserved
            cb.writeByte(0); // Flags
            cb.writeByte(message.yReason);
            // Pack optional TLV
            packOptionalTlv(cb, message);
            int length = cb.writerIndex() - objStartIndex;
            cb.setShort(objLenIndex, (short) length);
            // will be helpful during print().
            message.closeObjHeader.setObjLen((short) length);
            // As per RFC the length of object should be
            // multiples of 4
            int pad = length % 4;
            if (pad != 0) {
                pad = 4 - pad;
                for (int i = 0; i < pad; i++) {
                    cb.writeByte((byte) 0);
                }
                length = length + pad;
            }
            // update message length field
            length = cb.writerIndex() - startIndex;
            cb.setShort(msgLenIndex, (short) length);
        }

        public void packOptionalTlv(ChannelBuffer cb, PcepCloseMsgVer1 message) {

            LinkedList<PcepValueType> llOptionalTlv = message.llOptionalTlv;
            ListIterator<PcepValueType> listIterator = llOptionalTlv.listIterator();
            while (listIterator.hasNext()) {
                listIterator.next().write(cb);
            }
        }
    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
    }

    @Override
    public PcepType getType() {
        return MSG_TYPE;
    }

    @Override
    public byte getReason() {
        return this.yReason;
    }

    @Override
    public void setReason(byte value) {
        this.yReason = value;
    }

    @Override
    public LinkedList<PcepValueType> getOptionalTlv() {
        return this.llOptionalTlv;
    }

    @Override
    public void setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv) {
        this.llOptionalTlv = llOptionalTlv;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("closeObjectHeader", closeObjHeader).add("Reason", yReason)
                .add("OptionalTlvlist", llOptionalTlv).toString();
    }
}
