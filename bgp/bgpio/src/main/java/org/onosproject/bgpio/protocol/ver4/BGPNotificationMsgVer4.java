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
import org.onosproject.bgpio.protocol.BGPMessageReader;
import org.onosproject.bgpio.protocol.BGPMessageWriter;
import org.onosproject.bgpio.protocol.BGPNotificationMsg;
import org.onosproject.bgpio.protocol.BGPType;
import org.onosproject.bgpio.protocol.BGPVersion;
import org.onosproject.bgpio.types.BGPErrorType;
import org.onosproject.bgpio.types.BGPHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * A NOTIFICATION message is sent when an error condition is detected. The BGP connection is closed immediately after it
 * is sent.
 */
class BGPNotificationMsgVer4 implements BGPNotificationMsg {

    /*
          0                   1                   2                   3
          0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
          +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
          | Error code    | Error subcode |   Data (variable)             |
          +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
              REFERENCE : RFC 4271
    */

    protected static final Logger log = LoggerFactory.getLogger(BGPNotificationMsgVer4.class);

    static final byte PACKET_VERSION = 4;
    //BGPHeader(19) + Error code(1) + Error subcode(1)
    static final int TOTAL_MESSAGE_MIN_LENGTH = 21;
    static final int PACKET_MINIMUM_LENGTH = 2;
    static final BGPType MSG_TYPE = BGPType.NOTIFICATION;
    static final byte DEFAULT_ERRORSUBCODE = 0;
    static final byte[] MARKER = {0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
            0x01, 0x01};
    static final byte MESSAGE_TYPE = 3;
    static final BGPHeader DEFAULT_MESSAGE_HEADER = new BGPHeader(MARKER, BGPHeader.DEFAULT_HEADER_LENGTH,
                                                                  MESSAGE_TYPE);

    private byte errorCode;
    private byte errorSubCode;
    private byte[] data;
    private BGPHeader bgpHeader;
    public static final BGPNotificationMsgVer4.Reader READER = new Reader();

    /**
     * Resets fields.
     */
    public BGPNotificationMsgVer4() {
        this.bgpHeader = null;
        this.data = null;
        this.errorCode = 0;
        this.errorSubCode = 0;
    }

    /**
     * Constructor to initialize parameters.
     *
     * @param bgpHeader BGP Header in notification message
     * @param errorCode error code
     * @param errorSubCode error subcode
     * @param data field
     */
    public BGPNotificationMsgVer4(BGPHeader bgpHeader, byte errorCode, byte errorSubCode, byte[] data) {
        this.bgpHeader = bgpHeader;
        this.data = data;
        this.errorCode = errorCode;
        this.errorSubCode = errorSubCode;
    }

    /**
     * Reader reads BGP Notification Message from the channel buffer.
     */
    static class Reader implements BGPMessageReader<BGPNotificationMsg> {
        @Override
        public BGPNotificationMsg readFrom(ChannelBuffer cb, BGPHeader bgpHeader) throws BGPParseException {
            byte errorCode;
            byte errorSubCode;
            if (cb.readableBytes() < PACKET_MINIMUM_LENGTH) {
                throw new BGPParseException("Not enough readable bytes");
            }
            errorCode = cb.readByte();
            errorSubCode = cb.readByte();
            //Message Length = 21 + Data Length
            int dataLength = bgpHeader.getLength() - TOTAL_MESSAGE_MIN_LENGTH;
            byte[] data = new byte[dataLength];
            cb.readBytes(data, 0, dataLength);
            return new BGPNotificationMsgVer4(bgpHeader, errorCode, errorSubCode, data);
        }
    }

    /**
     * Builder class for BGP notification message.
     */
    static class Builder implements BGPNotificationMsg.Builder {
        private byte errorCode;
        private byte errorSubCode;
        private byte[] data;
        private BGPHeader bgpHeader;
        private boolean isErrorCodeSet = false;
        private boolean isErrorSubCodeSet = false;
        private boolean isBGPHeaderSet = false;

        @Override
        public BGPNotificationMsg build() throws BGPParseException {
            BGPHeader bgpHeader = this.isBGPHeaderSet ? this.bgpHeader : DEFAULT_MESSAGE_HEADER;
            if (!this.isErrorCodeSet) {
                throw new BGPParseException("Error code must be present");
            }

            byte errorSubCode = this.isErrorSubCodeSet ? this.errorSubCode : DEFAULT_ERRORSUBCODE;
            return new BGPNotificationMsgVer4(bgpHeader, this.errorCode, errorSubCode, this.data);
        }

        @Override
        public Builder setErrorCode(byte errorCode) {
            this.errorCode = errorCode;
            this.isErrorCodeSet = true;
            return this;
        }

        @Override
        public Builder setErrorSubCode(byte errorSubCode) {
            this.errorSubCode = errorSubCode;
            this.isErrorSubCodeSet = true;
            return this;
        }

        @Override
        public Builder setData(byte[] data) {
            this.data = data;
            return this;
        }

        @Override
        public Builder setNotificationMsgHeader(BGPHeader header) {
            this.bgpHeader = header;
            this.isBGPHeaderSet = true;
            return this;
        }

        @Override
        public Builder setHeader(BGPHeader bgpMsgHeader) {
            this.bgpHeader = bgpMsgHeader;
            return this;
        }
    }

    @Override
    public BGPVersion getVersion() {
        return BGPVersion.BGP_4;
    }

    @Override
    public BGPType getType() {
        return BGPType.NOTIFICATION;
    }

    @Override
    public void writeTo(ChannelBuffer cb) throws BGPParseException {
        WRITER.write(cb, this);
    }

    static final Writer WRITER = new Writer();

    /**
     * Writer writes BGP notification message to channel buffer.
     */
    static class Writer implements BGPMessageWriter<BGPNotificationMsgVer4> {
        @Override
        public void write(ChannelBuffer cb, BGPNotificationMsgVer4 message) throws BGPParseException {
            int msgStartIndex = cb.writerIndex();
            int headerLenIndex = message.bgpHeader.write(cb);
            if (headerLenIndex <= 0) {
                throw new BGPParseException(BGPErrorType.MESSAGE_HEADER_ERROR, (byte) 0, null);
            }
            cb.writeByte(message.errorCode);
            cb.writeByte(message.errorSubCode);
            cb.writeBytes(message.data);

            //Update message length field in notification message
            int length = cb.writerIndex() - msgStartIndex;
            cb.setShort(headerLenIndex, (short) length);
            message.bgpHeader.setLength((short) length);
        }
    }

    @Override
    public byte getErrorCode() {
        return this.errorCode;
    }

    /**
     * Sets errorcode with specified errorcode.
     *
     * @param errorCode field
     */
    public void setErrorCode(byte errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public byte getErrorSubCode() {
        return this.errorSubCode;
    }

    /**
     * Sets error subcode with specified errorSubCode.
     *
     * @param errorSubCode field
     */
    public void setErrorSubCode(byte errorSubCode) {
        this.errorSubCode = errorSubCode;
    }

    @Override
    public byte[] getData() {
        return this.data;
    }

    /**
     * Sets error data with specified data.
     *
     * @param data field
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public BGPHeader getHeader() {
        return this.bgpHeader;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("bgpHeader", bgpHeader)
                .add("data", data)
                .add("errorCode", errorCode)
                .add("errorSubCode", errorSubCode)
                .toString();
    }
}