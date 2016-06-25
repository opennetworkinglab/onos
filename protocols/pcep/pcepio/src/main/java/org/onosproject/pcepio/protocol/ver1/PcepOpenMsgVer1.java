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
import org.onosproject.pcepio.protocol.PcepMessageReader;
import org.onosproject.pcepio.protocol.PcepMessageWriter;
import org.onosproject.pcepio.protocol.PcepOpenMsg;
import org.onosproject.pcepio.protocol.PcepOpenObject;
import org.onosproject.pcepio.protocol.PcepType;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.onosproject.pcepio.types.PcepErrorDetailInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP open message.
 */
public class PcepOpenMsgVer1 implements PcepOpenMsg {

    /*
     * <Open Message>::= <Common Header> <OPEN>
     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    | Ver |  Flags  |  Message-Type |       Message-Length          |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    | Object-Class  |   OT  |Res|P|I|   Object Length (bytes)       |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    | Ver |   Flags |   Keepalive   |  DeadTimer    |      SID      |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                                                               |
    //                       Optional TLVs                         //
    |                                                               |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    protected static final Logger log = LoggerFactory.getLogger(PcepOpenMsgVer1.class);

    public static final byte PACKET_VERSION = 1;
    public static final int PACKET_MINIMUM_LENGTH = 12;
    public static final PcepType MSG_TYPE = PcepType.OPEN;
    private PcepOpenObject pcepOpenObj;

    public static final PcepOpenMsgVer1.Reader READER = new Reader();

    /**
     * Constructor to initialize PcepOpenObject.
     *
     * @param pcepOpenObj PCEP-OPEN-OBJECT
     */
    public PcepOpenMsgVer1(PcepOpenObject pcepOpenObj) {
        this.pcepOpenObj = pcepOpenObj;
    }

    @Override
    public PcepOpenObject getPcepOpenObject() {
        return this.pcepOpenObj;
    }

    @Override
    public void setPcepOpenObject(PcepOpenObject pcepOpenObj) {
        this.pcepOpenObj = pcepOpenObj;
    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
    }

    @Override
    public PcepType getType() {
        return MSG_TYPE;
    }

    /**
     * Reader class for reading PCEP open message from channel buffer.
     */
    public static class Reader implements PcepMessageReader<PcepOpenMsg> {

        @Override
        public PcepOpenMsg readFrom(ChannelBuffer cb) throws PcepParseException {

            if (cb.readableBytes() < PACKET_MINIMUM_LENGTH) {
                throw new PcepParseException("Packet size is less than the minimum length.");
            }

            byte version = cb.readByte();
            version = (byte) (version >> PcepMessageVer1.SHIFT_FLAG);
            if (version != PACKET_VERSION) {
                log.error("[readFrom] Invalid version: " + version);
                throw new PcepParseException(PcepErrorDetailInfo.ERROR_TYPE_1, PcepErrorDetailInfo.ERROR_VALUE_1);
            }
            // fixed value property type == 1
            byte type = cb.readByte();

            if (type != MSG_TYPE.getType()) {
                log.error("[readFrom] Unexpected type: " + type);
                throw new PcepParseException(PcepErrorDetailInfo.ERROR_TYPE_1, PcepErrorDetailInfo.ERROR_VALUE_1);
            }
            int length = cb.readShort();
            if (length < PACKET_MINIMUM_LENGTH) {
                throw new PcepParseException(
                        "Wrong length: Expected to be >= " + PACKET_MINIMUM_LENGTH + ", was: " + length);
            }
            return new PcepOpenMsgVer1(PcepOpenObjectVer1.read(cb));
        }
    }

    /**
     * Builder class for PCEP open message.
     */
    static class Builder implements PcepOpenMsg.Builder {

        private PcepOpenObject pcepOpenObj;

        @Override
        public PcepOpenMsg build() throws PcepParseException {
            if (!(pcepOpenObj instanceof PcepOpenObjectVer1)) {
                throw new NullPointerException("PcepOpenObject is null.");
            }
            return new PcepOpenMsgVer1(pcepOpenObj);
        }

        @Override
        public PcepVersion getVersion() {
            return PcepVersion.PCEP_1;
        }

        @Override
        public PcepType getType() {
            return PcepType.OPEN;
        }

        @Override
        public PcepOpenObject getPcepOpenObj() {
            return this.pcepOpenObj;
        }

        @Override
        public Builder setPcepOpenObj(PcepOpenObject obj) {
            this.pcepOpenObj = obj;
            return this;
        }
    }

    @Override
    public void writeTo(ChannelBuffer cb) throws PcepParseException {
        WRITER.write(cb, this);
    }

    public static final Writer WRITER = new Writer();

    /**
     * Writer class for writing PCEP opne message to channel buffer.
     */
    public static class Writer implements PcepMessageWriter<PcepOpenMsgVer1> {

        @Override
        public void write(ChannelBuffer cb, PcepOpenMsgVer1 message) throws PcepParseException {
            int startIndex = cb.writerIndex();
            // first 3 bits set to version
            cb.writeByte((byte) (PACKET_VERSION << PcepMessageVer1.SHIFT_FLAG));
            // message type
            cb.writeByte(MSG_TYPE.getType());
            // length is length of variable message, will be updated at the end
            // Store the position of message
            // length in buffer

            int msgLenIndex = cb.writerIndex();
            cb.writeShort(0);

            message.getPcepOpenObject().write(cb);

            // update message length field
            int iLength = cb.writerIndex() - startIndex;
            cb.setShort(msgLenIndex, (short) iLength);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("OpenObject", pcepOpenObj)
                .toString();
    }
}
