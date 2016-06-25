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
import org.onosproject.pcepio.protocol.PcepKeepaliveMsg;
import org.onosproject.pcepio.protocol.PcepMessageReader;
import org.onosproject.pcepio.protocol.PcepMessageWriter;
import org.onosproject.pcepio.protocol.PcepType;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP keep alive message.
 */
class PcepKeepaliveMsgVer1 implements PcepKeepaliveMsg {

    /*
    <Keepalive Message>::= <Common Header>

     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    | Ver |  Flags  |  Message-Type |       Message-Length          |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    protected static final Logger log = LoggerFactory.getLogger(PcepKeepaliveMsgVer1.class);
    // Pcep version: 1
    public static final byte PACKET_VERSION = 1;
    public static final int PACKET_MINIMUM_LENGTH = 4;
    public static final PcepType MSG_TYPE = PcepType.KEEP_ALIVE;

    public static final PcepKeepaliveMsgVer1.Reader READER = new Reader();

    /**
     * Reader class for reading PCEP keepalive message from channel buffer.
     */
    static class Reader implements PcepMessageReader<PcepKeepaliveMsg> {

        @Override
        public PcepKeepaliveMsg readFrom(ChannelBuffer cb) throws PcepParseException {

            if (cb.readableBytes() < PACKET_MINIMUM_LENGTH) {
                throw new PcepParseException("Packet size is less than the minimum required length.");
            }
            // fixed value property version == 1
            byte version = cb.readByte();
            version = (byte) (version >> PcepMessageVer1.SHIFT_FLAG);
            if (version != PACKET_VERSION) {
                throw new PcepParseException("Wrong version: Expected=PcepVersion.KEEP_ALIVE_1(2), got=" + version);
            }
            // fixed value property type == 2
            byte type = cb.readByte();
            if (type != MSG_TYPE.getType()) {
                throw new PcepParseException("Wrong type: Expected=PcepType.KEEP_ALIVE_1(2), got=" + type);
            }
            short length = cb.readShort();
            if (length < PACKET_MINIMUM_LENGTH) {
                throw new PcepParseException("Wrong length: Expected to be >= " + PACKET_MINIMUM_LENGTH + ", was: "
                        + length);
            }
            return new PcepKeepaliveMsgVer1();
        }
    }

    /**
     * Default constructor.
     */
    PcepKeepaliveMsgVer1() {
    }

    /**
     * Builder class for PCEP keepalive message.
     */
    static class Builder implements PcepKeepaliveMsg.Builder {
        @Override
        public PcepVersion getVersion() {
            return PcepVersion.PCEP_1;
        }

        @Override
        public PcepType getType() {
            return PcepType.KEEP_ALIVE;
        }

        @Override
        public PcepKeepaliveMsg build() {
            return new PcepKeepaliveMsgVer1();
        }
    }

    @Override
    public void writeTo(ChannelBuffer cb) {
        WRITER.write(cb, this);
    }

    static final Writer WRITER = new Writer();

    /**
     * Writer class for writing the PCEP keepalive message to channel buffer.
     */
    static class Writer implements PcepMessageWriter<PcepKeepaliveMsgVer1> {

        @Override
        public void write(ChannelBuffer cb, PcepKeepaliveMsgVer1 message) {
            int startIndex = cb.writerIndex();
            // first 3 bits set to version
            cb.writeByte((byte) (PACKET_VERSION << PcepMessageVer1.SHIFT_FLAG));
            // message type
            cb.writeByte(MSG_TYPE.getType());
            // length is length of variable message, will be updated at the end
            // Store the position of message
            // length in buffer
            int msgLenIndex = cb.writerIndex();
            cb.writeShort((short) 0);
            // update message length field
            int length = cb.writerIndex() - startIndex;
            cb.setShort(msgLenIndex, (short) length);
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
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).toString();
    }
}
