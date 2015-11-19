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

package org.onosproject.pcepio.protocol.ver1;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepFactories;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.onosproject.pcepio.protocol.PcepMessageReader;
import org.onosproject.pcepio.types.PcepErrorDetailInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides PCEP messages.
 */
public abstract class PcepMessageVer1 {

    protected static final Logger log = LoggerFactory.getLogger(PcepFactories.class);

    // version: 1.0
    static final byte WIRE_VERSION = 1;
    static final int MINIMUM_LENGTH = 4;
    static final int PACKET_VERSION = 1;
    static final byte OPEN_MSG_TYPE = 0x1;
    static final byte KEEPALIVE_MSG_TYPE = 0x2;
    static final byte REPORT_MSG_TYPE = 0xa;
    static final byte TE_REPORT_MSG_TYPE = 0xe;
    static final byte UPDATE_MSG_TYPE = 0xb;
    static final byte INITIATE_MSG_TYPE = 0xc;
    static final byte CLOSE_MSG_TYPE = 0x7;
    static final byte ERROR_MSG_TYPE = 0x6;
    static final byte LABEL_UPDATE_MSG_TYPE = 0xD;
    static final byte LABEL_RANGE_RESV_MSG_TYPE = 0xF;
    public static final int SHIFT_FLAG = 5;
    static final int MINIMUM_COMMON_HEADER_LENGTH = 4;

    public static final PcepMessageVer1.Reader READER = new Reader();

    /**
     * Reader class for reading PCEP messages from channel buffer.
     */
    static class Reader implements PcepMessageReader<PcepMessage> {
        @Override
        public PcepMessage readFrom(ChannelBuffer cb) throws PcepParseException {

            if (cb.readableBytes() < MINIMUM_LENGTH) {
                throw new PcepParseException("Packet should have minimum length: " + MINIMUM_LENGTH);
            }

            try {
                int start = cb.readerIndex();
                // fixed value property version == 1
                byte version = cb.readByte();
                version = (byte) (version >> PcepMessageVer1.SHIFT_FLAG);
                if (version != (byte) PACKET_VERSION) {
                    throw new PcepParseException("Wrong version. Expected=PcepVersion.Message_1(1), got=" + version);
                }

                byte type = cb.readByte();
                short length = cb.readShort();
                cb.readerIndex(start);

                switch (type) {

                case OPEN_MSG_TYPE:
                    log.debug("OPEN MESSAGE is received");
                    // message type value 1 means it is open message
                    return PcepOpenMsgVer1.READER.readFrom(cb.readBytes(length));
                case KEEPALIVE_MSG_TYPE:
                    log.debug("KEEPALIVE MESSAGE is received");
                    // message type value 2 means it is Keepalive message
                    return PcepKeepaliveMsgVer1.READER.readFrom(cb.readBytes(length));
                case ERROR_MSG_TYPE:
                    log.debug("ERROR MESSAGE is received");
                    // message type value 6 means it is error message
                    return PcepErrorMsgVer1.READER.readFrom(cb.readBytes(length));
                case REPORT_MSG_TYPE:
                    log.debug("REPORT MESSAGE is received");
                    // message type value 10 means it is Report message
                    // return
                    return PcepReportMsgVer1.READER.readFrom(cb.readBytes(length));
                case UPDATE_MSG_TYPE:
                    log.debug("UPDATE MESSAGE is received");
                    //message type value 11 means it is Update message
                    return PcepUpdateMsgVer1.READER.readFrom(cb.readBytes(length));
                case INITIATE_MSG_TYPE:
                    log.debug("INITIATE MESSAGE is received");
                    //message type value 12 means it is PcInitiate message
                    return PcepInitiateMsgVer1.READER.readFrom(cb.readBytes(length));
                case CLOSE_MSG_TYPE:
                    log.debug("CLOSE MESSAGE is received");
                    // message type value 7 means it is Close message
                    return PcepCloseMsgVer1.READER.readFrom(cb.readBytes(length));
                case TE_REPORT_MSG_TYPE:
                    log.debug("TE REPORT MESSAGE is received");
                    // message type value 14 means it is TE REPORT message
                    // return
                    return PcepTEReportMsgVer1.READER.readFrom(cb.readBytes(length));
                case LABEL_UPDATE_MSG_TYPE:
                    log.debug("LABEL UPDATE MESSAGE is received");
                    // message type value 13 means it is LABEL UPDATE message
                    // return
                    return PcepLabelUpdateMsgVer1.READER.readFrom(cb.readBytes(length));
                case LABEL_RANGE_RESV_MSG_TYPE:
                    log.debug("LABEL RANGE RESERVE MESSAGE is received");
                    // message type value 15 means it is LABEL RANGE RESERVE message
                    // return
                    return PcepLabelRangeResvMsgVer1.READER.readFrom(cb.readBytes(length));
                default:
                    throw new PcepParseException("ERROR: UNKNOWN MESSAGE is received. Msg Type: " + type);
                }
            } catch (IndexOutOfBoundsException e) {
                throw new PcepParseException(PcepErrorDetailInfo.ERROR_TYPE_1, PcepErrorDetailInfo.ERROR_VALUE_1);
            }
        }
    }
}
