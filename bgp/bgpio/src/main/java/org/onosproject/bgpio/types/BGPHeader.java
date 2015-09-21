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
package org.onosproject.bgpio.types;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides BGP Message Header which is common for all the Messages.
 */

public class BGPHeader {

    /*      0                   1                   2                   3
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
    */

    protected static final Logger log = LoggerFactory.getLogger(BGPHeader.class);

    public static final int MARKER_LENGTH = 16;
    public static final short DEFAULT_HEADER_LENGTH = 19;

    private byte[] marker;
    private byte type;
    private short length;

    /**
     * Reset fields.
     */
    public BGPHeader() {
        this.marker = null;
        this.length = 0;
        this.type = 0;
    }

    /**
     * Constructors to initialize parameters.
     *
     * @param marker field in BGP header
     * @param length message length
     * @param type message type
     */
    public BGPHeader(byte[] marker, short length, byte type) {
        this.marker = marker;
        this.length = length;
        this.type = type;
    }

    /**
     * Sets marker field.
     *
     * @param value marker field
     */
    public void setMarker(byte[] value) {
        this.marker = value;
    }

    /**
     * Sets message type.
     *
     * @param value message type
     */
    public void setType(byte value) {
        this.type = value;
    }

    /**
     * Sets message length.
     *
     * @param value message length
     */
    public void setLength(short value) {
        this.length = value;
    }

    /**
     * Returns message length.
     *
     * @return message length
     */
    public short getLength() {
        return this.length;
    }

    /**
     * Returns message marker.
     *
     * @return message marker
     */
    public byte[] getMarker() {
        return this.marker;
    }

    /**
     * Returns message type.
     *
     * @return message type
     */
    public byte getType() {
        return this.type;
    }

    /**
     * Writes Byte stream of BGP header to channel buffer.
     *
     * @param cb ChannelBuffer
     * @return length index of message header
     */
    public int write(ChannelBuffer cb) {

        cb.writeBytes(getMarker(), 0, MARKER_LENGTH);

        int headerLenIndex = cb.writerIndex();
        cb.writeShort((short) 0);
        cb.writeByte(type);

        return headerLenIndex;
    }

    /**
     * Read from channel buffer and Returns BGP header.
     *
     * @param cb ChannelBuffer
     * @return object of BGPHeader
     */
    public static BGPHeader read(ChannelBuffer cb) {

        byte[] marker = new byte[MARKER_LENGTH];
        byte type;
        short length;
        cb.readBytes(marker, 0, MARKER_LENGTH);
        length = cb.readShort();
        type = cb.readByte();
        return new BGPHeader(marker, length, type);
    }
}