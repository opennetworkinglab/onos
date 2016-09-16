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

import org.onosproject.ospf.controller.OspfMessage;
import org.onosproject.ospf.protocol.util.OspfParameters;
import org.onosproject.ospf.protocol.util.OspfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A message writer which writes an OSPF message to byte array.
 */
public class OspfMessageWriter {
    private static final Logger log = LoggerFactory.getLogger(OspfMessageWriter.class);

    /**
     * Writes OSPF message to byte array.
     *
     * @param ospfMessage    OSPF message
     * @param interfaceIndex interface index
     * @param interfaceState interface state
     * @return message as byte array
     */
    public byte[] getMessage(OspfMessage ospfMessage, int interfaceIndex, int interfaceState) {

        byte[] buf = null;
        switch (ospfMessage.ospfMessageType().value()) {
            case OspfParameters.HELLO:
            case OspfParameters.LSACK:
            case OspfParameters.DD:
            case OspfParameters.LSREQUEST:
            case OspfParameters.LSUPDATE:
                buf = writeMessageToBytes(ospfMessage, interfaceIndex, interfaceState);
                break;
            default:
                log.debug("Message Writer[Encoder] - Unknown Message to encode..!!!");
                break;
        }

        return buf;
    }

    /**
     * Writes an OSPF Message to byte array.
     *
     * @param ospfMessage    OSPF Message instance
     * @param interfaceState interface state
     * @return message as byte array
     */
    private byte[] writeMessageToBytes(OspfMessage ospfMessage, int interfaceIndex, int interfaceState) {
        byte[] ospfMessageAsByte = ospfMessage.asBytes();
        //Add the length and checksum in byte array at length position 2 & 3 and Checksum position
        ospfMessageAsByte = OspfUtil.addLengthAndCheckSum(ospfMessageAsByte, OspfUtil.OSPFPACKET_LENGTH_POS1,
                                                          OspfUtil.OSPFPACKET_LENGTH_POS2,
                                                          OspfUtil.OSPFPACKET_CHECKSUM_POS1,
                                                          OspfUtil.OSPFPACKET_CHECKSUM_POS2);
        //Add Interface State Info and destination IP as metadata
        if (interfaceState == OspfParameters.DR || interfaceState == OspfParameters.BDR) {
            ospfMessageAsByte = OspfUtil.addMetadata(interfaceIndex, ospfMessageAsByte, OspfUtil.JOIN_ALL_DROUTERS,
                                                     ospfMessage.destinationIp());
        } else {
            ospfMessageAsByte = OspfUtil.addMetadata(interfaceIndex, ospfMessageAsByte, OspfUtil.ONLY_ALL_SPF_ROUTERS,
                                                     ospfMessage.destinationIp());
        }

        return ospfMessageAsByte;
    }
}