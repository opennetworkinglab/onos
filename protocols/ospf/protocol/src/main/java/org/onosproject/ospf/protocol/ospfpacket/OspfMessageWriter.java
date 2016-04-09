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
package org.onosproject.ospf.protocol.ospfpacket;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.onosproject.ospf.protocol.util.OspfParameters;
import org.onosproject.ospf.protocol.util.OspfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A message writer which writes an OspfMessage to ChannelBuffer.
 */
public class OspfMessageWriter {
    private static final Logger log = LoggerFactory.getLogger(OspfMessageWriter.class);

    /**
     * Writes OSPF message to ChannelBuffer.
     *
     * @param ospfMessage    OSPF message
     * @param interfaceState interface state
     * @param interfaceType  interface type
     * @return channelBuffer channel buffer instance
     * @throws Exception might throws exception while parsing message
     */
    public ChannelBuffer writeToBuffer(OspfMessage ospfMessage, int interfaceState,
                                       int interfaceType) throws Exception {

        ChannelBuffer buf = null;
        switch (ospfMessage.ospfMessageType().value()) {
            case OspfParameters.HELLO:
            case OspfParameters.LSACK:
                buf = writeMessageToBuffer(ospfMessage, interfaceState);
                break;
            case OspfParameters.DD:
            case OspfParameters.LSREQUEST:
            case OspfParameters.LSUPDATE:
                buf = writeMessageToBuffer(ospfMessage, interfaceState);
                break;
            default:
                log.debug("Message Writer[Encoder] - Unknown Message to encode..!!!");
                break;
        }

        return buf;
    }

    /**
     * Writes an OSPF Message to channel buffer.
     *
     * @param ospfMessage    OSPF Message instance
     * @param interfaceState interface state
     * @return channelBuffer instance
     */
    private ChannelBuffer writeMessageToBuffer(OspfMessage ospfMessage, int interfaceState) throws Exception {
        ChannelBuffer channelBuffer = null;
        byte[] ospfMessageAsByte = ospfMessage.asBytes();
        //Add the length and checksum in byte array at length position 2 & 3 and Checksum position
        ospfMessageAsByte = OspfUtil.addLengthAndCheckSum(ospfMessageAsByte, OspfUtil.OSPFPACKET_LENGTH_POS1,
                                                          OspfUtil.OSPFPACKET_LENGTH_POS2,
                                                          OspfUtil.OSPFPACKET_CHECKSUM_POS1,
                                                          OspfUtil.OSPFPACKET_CHECKSUM_POS2);
        //Add Interface State Info and destination IP as metadata
        if (interfaceState == OspfParameters.DR || interfaceState == OspfParameters.BDR) {
            ospfMessageAsByte = OspfUtil.addMetadata(ospfMessageAsByte, OspfUtil.JOIN_ALL_DROUTERS,
                                                     ospfMessage.destinationIp());
        } else {
            ospfMessageAsByte = OspfUtil.addMetadata(ospfMessageAsByte, OspfUtil.ONLY_ALL_SPF_ROUTERS,
                                                     ospfMessage.destinationIp());
        }

        channelBuffer = ChannelBuffers.buffer(ospfMessageAsByte.length);
        channelBuffer.writeBytes(ospfMessageAsByte);

        return channelBuffer;
    }
}