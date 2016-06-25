/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.isis.io.isispacket;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.isis.controller.IsisMessage;
import org.onosproject.isis.exceptions.IsisErrorType;
import org.onosproject.isis.exceptions.IsisParseException;
import org.onosproject.isis.io.isispacket.pdu.Csnp;
import org.onosproject.isis.io.isispacket.pdu.L1L2HelloPdu;
import org.onosproject.isis.io.isispacket.pdu.LsPdu;
import org.onosproject.isis.io.isispacket.pdu.P2PHelloPdu;
import org.onosproject.isis.io.isispacket.pdu.Psnp;
import org.onosproject.isis.io.util.IsisConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents ISIS message reader.
 */
public class IsisMessageReader {

    protected static final Logger log = LoggerFactory.getLogger(IsisMessageReader.class);

    /**
     * Reads from ISIS packet from buffer.
     *
     * @param channelBuffer buffer
     * @return ISIS message
     * @throws Exception exception
     */
    public IsisMessage readFromBuffer(ChannelBuffer channelBuffer) throws Exception {

        int dataLength = channelBuffer.readableBytes();
        log.debug("IsisMessageReader::readFromBuffer Data length {}", dataLength);
        if (channelBuffer.readableBytes() < IsisConstants.PDU_LENGTH) {
            log.debug("Packet should have minimum length...");
            throw new IsisParseException(IsisErrorType.MESSAGE_HEADER_ERROR, IsisErrorType.BAD_MESSAGE_LENGTH);
        }
        IsisHeader isisHeader = getIsisHeader(channelBuffer);
        int totalLength = 0;
        IsisMessage isisMessage = null;
        switch (isisHeader.isisPduType()) {
            case L1HELLOPDU:
            case L2HELLOPDU:
                isisMessage = new L1L2HelloPdu(isisHeader);
                totalLength = channelBuffer.getShort(IsisConstants.PDULENGTHPOSITION);
                break;
            case P2PHELLOPDU:
                isisMessage = new P2PHelloPdu(isisHeader);
                totalLength = channelBuffer.getShort(IsisConstants.PDULENGTHPOSITION);
                break;
            case L1LSPDU:
            case L2LSPDU:
                isisMessage = new LsPdu(isisHeader);
                totalLength = channelBuffer.getShort(8);
                break;
            case L1CSNP:
            case L2CSNP:
                isisMessage = new Csnp(isisHeader);
                totalLength = channelBuffer.getShort(8);
                break;
            case L1PSNP:
            case L2PSNP:
                isisMessage = new Psnp(isisHeader);
                totalLength = channelBuffer.getShort(8);
                break;
            default:
                log.debug("Message Reader[Decoder] - Unknown PDU type..!!!");
                break;
        }

        if (isisMessage != null) {
            try {
                int bodyLength = totalLength - IsisConstants.COMMONHEADERLENGTH;
                isisMessage.readFrom(channelBuffer.readBytes(bodyLength));

            } catch (Exception e) {
                throw new IsisParseException(IsisErrorType.ISIS_MESSAGE_ERROR,
                                             IsisErrorType.BAD_MESSAGE);
            }

        }

        return isisMessage;
    }

    /**
     * Gets ISIS header.
     *
     * @param channelBuffer ISIS header
     * @return ISIS header
     * @throws Exception
     */
    private IsisHeader getIsisHeader(ChannelBuffer channelBuffer) throws Exception {

        IsisHeader isisHeader = new IsisHeader();
        isisHeader.setIrpDiscriminator(channelBuffer.readByte());
        isisHeader.setPduHeaderLength(channelBuffer.readByte());
        isisHeader.setVersion(channelBuffer.readByte());
        isisHeader.setIdLength(channelBuffer.readByte());
        isisHeader.setIsisPduType(channelBuffer.readByte());
        isisHeader.setVersion2(channelBuffer.readByte());
        isisHeader.setReserved(channelBuffer.readByte());
        isisHeader.setMaximumAreaAddresses(channelBuffer.readByte());

        return isisHeader;
    }
}