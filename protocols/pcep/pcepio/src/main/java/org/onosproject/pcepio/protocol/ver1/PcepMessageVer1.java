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
import org.onosproject.pcepio.exceptions.PcepOutOfBoundMessageException;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepFactories;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.onosproject.pcepio.protocol.PcepMessageReader;
import org.onosproject.pcepio.protocol.PcepType;
import org.onosproject.pcepio.types.PcepErrorDetailInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides PCEP messages.
 */
public abstract class PcepMessageVer1 {

    protected static final Logger log = LoggerFactory.getLogger(PcepFactories.class);

    // version: 1.0
    public static final byte WIRE_VERSION = 1;
    public static final int MINIMUM_LENGTH = 4;
    public static final int PACKET_VERSION = 1;
    public static final int SHIFT_FLAG = 5;
    public static final int MINIMUM_COMMON_HEADER_LENGTH = 4;

    public static final PcepMessageVer1.Reader READER = new Reader();

    /**
     * Reader class for reading PCEP messages from channel buffer.
     */
    static class Reader implements PcepMessageReader<PcepMessage> {
        @Override
        public PcepMessage readFrom(ChannelBuffer cb) throws PcepParseException, PcepOutOfBoundMessageException {

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

                // Check the out-of-bound message.
                // If the message is out-of-bound then throw PcepOutOfBoundException.
                if ((length - MINIMUM_COMMON_HEADER_LENGTH) > cb.readableBytes()) {
                    throw new PcepOutOfBoundMessageException("Message is out-of-bound.");
                }

                if (type == (byte) PcepType.OPEN.getType()) {
                    log.debug("OPEN MESSAGE is received");
                    return PcepOpenMsgVer1.READER.readFrom(cb.readBytes(length));
                } else if (type == (byte) PcepType.KEEP_ALIVE.getType()) {
                    log.debug("KEEPALIVE MESSAGE is received");
                    return PcepKeepaliveMsgVer1.READER.readFrom(cb.readBytes(length));
                } else if (type == (byte) PcepType.ERROR.getType()) {
                    log.debug("ERROR MESSAGE is received");
                    return PcepErrorMsgVer1.READER.readFrom(cb.readBytes(length));
                } else if (type == (byte) PcepType.CLOSE.getType()) {
                    log.debug("CLOSE MESSAGE is received");
                    return PcepCloseMsgVer1.READER.readFrom(cb.readBytes(length));
                } else if (type == (byte) PcepType.REPORT.getType()) {
                    log.debug("REPORT MESSAGE is received");
                    return PcepReportMsgVer1.READER.readFrom(cb.readBytes(length));
                } else if (type == (byte) PcepType.UPDATE.getType()) {
                    log.debug("UPDATE MESSAGE is received");
                    return PcepUpdateMsgVer1.READER.readFrom(cb.readBytes(length));
                } else if (type == (byte) PcepType.INITIATE.getType()) {
                    log.debug("INITIATE MESSAGE is received");
                    return PcepInitiateMsgVer1.READER.readFrom(cb.readBytes(length));
                } else if (type == (byte) PcepType.LS_REPORT.getType()) {
                    log.debug("LS REPORT MESSAGE is received");
                    return PcepLSReportMsgVer1.READER.readFrom(cb.readBytes(length));
                } else if (type == (byte) PcepType.LABEL_RANGE_RESERV.getType()) {
                    log.debug("LABEL RANGE RESERVE MESSAGE is received");
                    return PcepLabelRangeResvMsgVer1.READER.readFrom(cb.readBytes(length));
                } else if (type == (byte) PcepType.LABEL_UPDATE.getType()) {
                    log.debug("LABEL UPDATE MESSAGE is received");
                    return PcepLabelUpdateMsgVer1.READER.readFrom(cb.readBytes(length));
                } else {
                    throw new PcepParseException("ERROR: UNKNOWN MESSAGE is received. Msg Type: " + type);
                }
            } catch (IndexOutOfBoundsException e) {
                throw new PcepParseException(PcepErrorDetailInfo.ERROR_TYPE_1, PcepErrorDetailInfo.ERROR_VALUE_1);
            }
        }
    }
}
