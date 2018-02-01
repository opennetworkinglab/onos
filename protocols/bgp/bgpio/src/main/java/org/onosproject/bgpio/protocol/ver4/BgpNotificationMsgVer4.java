/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onosproject.bgpio.protocol.BgpMessageReader;
import org.onosproject.bgpio.protocol.BgpMessageWriter;
import org.onosproject.bgpio.protocol.BgpNotificationMsg;
import org.onosproject.bgpio.protocol.BgpType;
import org.onosproject.bgpio.protocol.BgpVersion;
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.types.BgpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * A NOTIFICATION message is sent when an error condition is detected. The BGP connection is closed immediately after it
 * is sent.
 */
class BgpNotificationMsgVer4 implements BgpNotificationMsg {

    /*
          0                   1                   2                   3
          0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
          +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
          | Error code    | Error subcode |   Data (variable)             |
          +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
              REFERENCE : RFC 4271
    */

    private static final Logger log = LoggerFactory.getLogger(BgpNotificationMsgVer4.class);

    static final byte PACKET_VERSION = 4;
    //BGPHeader(19) + Error code(1) + Error subcode(1)
    static final int TOTAL_MESSAGE_MIN_LENGTH = 21;
    static final int PACKET_MINIMUM_LENGTH = 2;
    static final BgpType MSG_TYPE = BgpType.NOTIFICATION;
    static final byte DEFAULT_ERRORSUBCODE = 0;
    private static final byte[] MARKER = {(byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff };
    static final byte MESSAGE_TYPE = 3;
    static final BgpHeader DEFAULT_MESSAGE_HEADER = new BgpHeader(MARKER, BgpHeader.DEFAULT_HEADER_LENGTH,
                                                                  MESSAGE_TYPE);

    private byte errorCode;
    private byte errorSubCode;
    private byte[] data;
    private BgpHeader bgpHeader;
    public static final BgpNotificationMsgVer4.Reader READER = new Reader();

    /**
     * Initialize fields.
     */
    public BgpNotificationMsgVer4() {
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
    public BgpNotificationMsgVer4(BgpHeader bgpHeader, byte errorCode, byte errorSubCode, byte[] data) {
        this.bgpHeader = bgpHeader;
        this.data = data;
        this.errorCode = errorCode;
        this.errorSubCode = errorSubCode;
    }

    /**
     * Reader reads BGP Notification Message from the channel buffer.
     */
    static class Reader implements BgpMessageReader<BgpNotificationMsg> {
        @Override
        public BgpNotificationMsg readFrom(ChannelBuffer cb, BgpHeader bgpHeader) throws BgpParseException {
            byte errorCode;
            byte errorSubCode;
            if (cb.readableBytes() < PACKET_MINIMUM_LENGTH) {
                throw new BgpParseException("Not enough readable bytes");
            }
            errorCode = cb.readByte();
            errorSubCode = cb.readByte();
            //Message Length = 21 + Data Length
            int dataLength = bgpHeader.getLength() - TOTAL_MESSAGE_MIN_LENGTH;
            byte[] data = new byte[dataLength];
            cb.readBytes(data, 0, dataLength);
            return new BgpNotificationMsgVer4(bgpHeader, errorCode, errorSubCode, data);
        }
    }

    /**
     * Builder class for BGP notification message.
     */
    static class Builder implements BgpNotificationMsg.Builder {
        private byte errorCode;
        private byte errorSubCode;
        private byte[] data;
        private BgpHeader bgpHeader;
        private boolean isErrorCodeSet = false;
        private boolean isErrorSubCodeSet = false;
        private boolean isBgpHeaderSet = false;

        @Override
        public BgpNotificationMsg build() throws BgpParseException {
            BgpHeader bgpHeader = this.isBgpHeaderSet ? this.bgpHeader : DEFAULT_MESSAGE_HEADER;
            if (!this.isErrorCodeSet) {
                throw new BgpParseException("Error code must be present");
            }

            byte errorSubCode = this.isErrorSubCodeSet ? this.errorSubCode : DEFAULT_ERRORSUBCODE;
            return new BgpNotificationMsgVer4(bgpHeader, this.errorCode, errorSubCode, this.data);
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
            if (data != null) {
                this.data = data;
            }
            return this;
        }

        @Override
        public Builder setHeader(BgpHeader bgpMsgHeader) {
            this.bgpHeader = bgpMsgHeader;
            return this;
        }
    }

    @Override
    public BgpVersion getVersion() {
        return BgpVersion.BGP_4;
    }

    @Override
    public BgpType getType() {
        return BgpType.NOTIFICATION;
    }

    @Override
    public void writeTo(ChannelBuffer cb) throws BgpParseException {
        WRITER.write(cb, this);
    }

    static final Writer WRITER = new Writer();

    /**
     * Writer writes BGP notification message to channel buffer.
     */
    static class Writer implements BgpMessageWriter<BgpNotificationMsgVer4> {
        @Override
        public void write(ChannelBuffer cb, BgpNotificationMsgVer4 message) throws BgpParseException {
            int msgStartIndex = cb.writerIndex();
            int headerLenIndex = message.bgpHeader.write(cb);
            if (headerLenIndex <= 0) {
                throw new BgpParseException(BgpErrorType.MESSAGE_HEADER_ERROR, (byte) 0, null);
            }
            cb.writeByte(message.errorCode);
            cb.writeByte(message.errorSubCode);
            if (message.data != null) {
                cb.writeBytes(message.data);
            }

            log.debug("Update message length field in notification message");
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
    public BgpHeader getHeader() {
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
