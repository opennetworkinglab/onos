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
import org.onosproject.bgpio.protocol.BGPFactories;
import org.onosproject.bgpio.protocol.BGPMessage;
import org.onosproject.bgpio.protocol.BGPMessageReader;
import org.onosproject.bgpio.types.BGPErrorType;
import org.onosproject.bgpio.types.BGPHeader;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides BGP messages.
 */
public abstract class BGPMessageVer4 {

    protected static final Logger log = LoggerFactory.getLogger(BGPFactories.class);

    static final byte OPEN_MSG_TYPE = 0x1;
    static final byte KEEPALIVE_MSG_TYPE = 0x4;
    static final byte UPDATE_MSG_TYPE = 0x2;
    static final byte NOTIFICATION_MSG_TYPE = 0x3;
    static final int MINIMUM_COMMON_HEADER_LENGTH = 19;
    static final int HEADER_AND_MSG_LEN = 18;
    static final int MAXIMUM_PACKET_LENGTH = 4096;

    public static final BGPMessageVer4.Reader READER = new Reader();

    /**
     * Reader class for reading BGP messages from channel buffer.
     *
     */
    static class Reader implements BGPMessageReader<BGPMessage> {
        @Override
        public BGPMessage readFrom(ChannelBuffer cb, BGPHeader bgpHeader)
                throws BGPParseException {

            if (cb.readableBytes() < MINIMUM_COMMON_HEADER_LENGTH) {
                log.error("Packet should have minimum length.");
                Validation.validateLen(BGPErrorType.MESSAGE_HEADER_ERROR, BGPErrorType.BAD_MESSAGE_LENGTH,
                                       cb.readableBytes());
            }
            if (cb.readableBytes() > MAXIMUM_PACKET_LENGTH) {
                log.error("Packet length should not exceed {}.", MAXIMUM_PACKET_LENGTH);
                Validation.validateLen(BGPErrorType.MESSAGE_HEADER_ERROR, BGPErrorType.BAD_MESSAGE_LENGTH,
                                       cb.readableBytes());
            }
            try {
                // fixed value property version == 4
                byte[] marker = new byte[BGPHeader.MARKER_LENGTH];
                cb.readBytes(marker, 0, BGPHeader.MARKER_LENGTH);
                bgpHeader.setMarker(marker);
                for (int i = 0; i < BGPHeader.MARKER_LENGTH; i++) {
                    if (marker[i] != (byte) 0xff) {
                        throw new BGPParseException(BGPErrorType.MESSAGE_HEADER_ERROR,
                                                    BGPErrorType.CONNECTION_NOT_SYNCHRONIZED, null);
                    }
                }
                short length = cb.readShort();
                if (length != (cb.readableBytes() + HEADER_AND_MSG_LEN)) {
                    Validation.validateLen(BGPErrorType.MESSAGE_HEADER_ERROR, BGPErrorType.BAD_MESSAGE_LENGTH, length);
                }
                bgpHeader.setLength(length);
                byte type = cb.readByte();
                bgpHeader.setType(type);
                log.debug("Reading update message of type " + type);

                int len = length - MINIMUM_COMMON_HEADER_LENGTH;
                switch (type) {
                case OPEN_MSG_TYPE:
                    log.debug("OPEN MESSAGE is received");
                    return BGPOpenMsgVer4.READER.readFrom(cb.readBytes(len), bgpHeader);
                case KEEPALIVE_MSG_TYPE:
                    log.debug("KEEPALIVE MESSAGE is received");
                    return BGPKeepaliveMsgVer4.READER.readFrom(cb.readBytes(len), bgpHeader);
                case UPDATE_MSG_TYPE:
                    log.debug("UPDATE MESSAGE is received");
                    // TODO: Update message version 4
                case NOTIFICATION_MSG_TYPE:
                    log.debug("NOTIFICATION MESSAGE is received");
                    return BGPNotificationMsgVer4.READER.readFrom(cb.readBytes(len), bgpHeader);
                default:
                    Validation.validateType(BGPErrorType.MESSAGE_HEADER_ERROR, BGPErrorType.BAD_MESSAGE_TYPE, type);
                    return null;
                }
            } catch (IndexOutOfBoundsException e) {
                throw new BGPParseException(BGPErrorType.MESSAGE_HEADER_ERROR, BGPErrorType.BAD_MESSAGE_LENGTH, null);
            }
        }
    }
}