/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.bgpio.protocol.ver4;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BGPParseException;
import org.onosproject.bgpio.protocol.BGPKeepaliveMsg;
import org.onosproject.bgpio.protocol.BGPMessageReader;
import org.onosproject.bgpio.protocol.BGPMessageWriter;
import org.onosproject.bgpio.types.BGPHeader;
import org.onosproject.bgpio.protocol.BGPType;
import org.onosproject.bgpio.protocol.BGPVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides BGP keep alive message.
 */
class BGPKeepaliveMsgVer4 implements BGPKeepaliveMsg {

    /*
    <Keepalive Message>::= <Common Header>
    A KEEPALIVE message consists of only the message header and has a
    length of 19 octets.

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                                                               |
    +                                                               +
    |                                                               |
    +                                                               +
    |                           Marker                              |
    +                                                               +
    |                                                               |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |          Length               |      Type     |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

    REFERENCE : RFC 4271
    */

    protected static final Logger log = LoggerFactory
            .getLogger(BGPKeepaliveMsgVer4.class);

    private BGPHeader bgpMsgHeader;
    public static final byte PACKET_VERSION = 4;
    public static final int PACKET_MINIMUM_LENGTH = 19;
    public static final int MARKER_LENGTH = 16;
    public static final BGPType MSG_TYPE = BGPType.KEEP_ALIVE;
    public static byte[] marker = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                              (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                              (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                              (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

    public static final BGPKeepaliveMsgVer4.Reader READER = new Reader();

    /**
     * Reader class for reading BGP keepalive message from channel buffer.
     */
    static class Reader implements BGPMessageReader<BGPKeepaliveMsg> {

        @Override
        public BGPKeepaliveMsg readFrom(ChannelBuffer cb, BGPHeader bgpHeader)
                throws BGPParseException {

            /* bgpHeader is not required in case of keepalive message and
            Header is already read and no other fields except header in keepalive message.*/
            return new BGPKeepaliveMsgVer4();
        }
    }

    /**
     * Default constructor.
     */
    BGPKeepaliveMsgVer4() {
    }

    /**
     * Builder class for BGP keepalive message.
     */
    static class Builder implements BGPKeepaliveMsg.Builder {
        BGPHeader bgpMsgHeader;

        @Override
        public BGPVersion getVersion() {
            return BGPVersion.BGP_4;
        }

        @Override
        public BGPType getType() {
            return BGPType.KEEP_ALIVE;
        }

        @Override
        public BGPHeader getHeader() {
            return this.bgpMsgHeader;
        }

        @Override
        public Builder setHeader(BGPHeader bgpMsgHeader) {
            this.bgpMsgHeader = bgpMsgHeader;
            return this;
        }

        @Override
        public BGPKeepaliveMsg build() {
            return new BGPKeepaliveMsgVer4();
        }
    }

    @Override
    public void writeTo(ChannelBuffer cb) {
        WRITER.write(cb, this);
    }

    static final Writer WRITER = new Writer();

    /**
     * Writer class for writing the BGP keepalive message to channel buffer.
     */
    static class Writer implements BGPMessageWriter<BGPKeepaliveMsgVer4> {

        @Override
        public void write(ChannelBuffer cb, BGPKeepaliveMsgVer4 message) {

            // write marker
            cb.writeBytes(marker, 0, MARKER_LENGTH);

            // write length of header
            cb.writeShort(PACKET_MINIMUM_LENGTH);

            // write the type of message
            cb.writeByte(MSG_TYPE.getType());
        }
    }

    @Override
    public BGPVersion getVersion() {
        return BGPVersion.BGP_4;
    }

    @Override
    public BGPType getType() {
        return MSG_TYPE;
    }

    @Override
    public BGPHeader getHeader() {
        return this.bgpMsgHeader;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).toString();
    }
}
