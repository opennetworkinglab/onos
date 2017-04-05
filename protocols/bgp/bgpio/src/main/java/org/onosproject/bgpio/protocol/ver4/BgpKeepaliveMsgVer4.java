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
package org.onosproject.bgpio.protocol.ver4;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpKeepaliveMsg;
import org.onosproject.bgpio.protocol.BgpMessageReader;
import org.onosproject.bgpio.protocol.BgpMessageWriter;
import org.onosproject.bgpio.types.BgpHeader;
import org.onosproject.bgpio.protocol.BgpType;
import org.onosproject.bgpio.protocol.BgpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides BGP keep alive message.
 */
public class BgpKeepaliveMsgVer4 implements BgpKeepaliveMsg {

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
            .getLogger(BgpKeepaliveMsgVer4.class);

    private BgpHeader bgpMsgHeader;
    public static final byte PACKET_VERSION = 4;
    public static final int PACKET_MINIMUM_LENGTH = 19;
    public static final int MARKER_LENGTH = 16;
    public static final BgpType MSG_TYPE = BgpType.KEEP_ALIVE;
    static byte[] marker = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                              (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                              (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                              (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

    public static final BgpKeepaliveMsgVer4.Reader READER = new Reader();

    /**
     * Reader class for reading BGP keepalive message from channel buffer.
     */
    static class Reader implements BgpMessageReader<BgpKeepaliveMsg> {

        @Override
        public BgpKeepaliveMsg readFrom(ChannelBuffer cb, BgpHeader bgpHeader)
                throws BgpParseException {

            /* bgpHeader is not required in case of keepalive message and
            Header is already read and no other fields except header in keepalive message.*/
            return new BgpKeepaliveMsgVer4();
        }
    }

    /**
     * Default constructor.
     */
    public BgpKeepaliveMsgVer4() {
    }

    /**
     * Builder class for BGP keepalive message.
     */
    static class Builder implements BgpKeepaliveMsg.Builder {
        BgpHeader bgpMsgHeader;

        @Override
        public Builder setHeader(BgpHeader bgpMsgHeader) {
            this.bgpMsgHeader = bgpMsgHeader;
            return this;
        }

        @Override
        public BgpKeepaliveMsg build() {
            return new BgpKeepaliveMsgVer4();
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
    static class Writer implements BgpMessageWriter<BgpKeepaliveMsgVer4> {

        @Override
        public void write(ChannelBuffer cb, BgpKeepaliveMsgVer4 message) {

            log.debug("Write marker");
            cb.writeBytes(marker, 0, MARKER_LENGTH);

            log.debug("Write length of header");
            cb.writeShort(PACKET_MINIMUM_LENGTH);

            log.debug("Write the type of message");
            cb.writeByte(MSG_TYPE.getType());
        }
    }

    @Override
    public BgpVersion getVersion() {
        return BgpVersion.BGP_4;
    }

    @Override
    public BgpType getType() {
        return MSG_TYPE;
    }

    @Override
    public BgpHeader getHeader() {
        return this.bgpMsgHeader;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).toString();
    }
}
